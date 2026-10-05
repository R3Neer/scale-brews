package io.github.r3neer.scalebrews.client.collision.preparation;

import io.github.r3neer.scalebrews.collision.catalog.CollisionBindingCatalog;
import io.github.r3neer.scalebrews.collision.catalog.CollisionCoverageDiscovery;
import io.github.r3neer.scalebrews.collision.catalog.CollisionCoverageScanner;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;

/**
 * Explicit client PREPARATION census of original renderer-held model candidates.
 * No initializer, renderer, network or physics hot path calls this tooling.
 * Slots/structural fingerprints are not executable selectors, pose proofs or coverage authority.
 */
public final class ModelFamilyCensus {
    public static final String TOOL_VERSION = "model-family-census-v1";
    public static final int MAX_FIELDS = 512, MAX_SLOTS = 256, MAX_CONTAINER = 512, MAX_DEPTH = 12,
        MAX_ENTRIES = 4096, MAX_STRUCTURES = 4096;
    private static final Comparator<Slot> SLOT_ORDER = Comparator.comparing(Slot::path);

    private ModelFamilyCensus() {}

    public record Slot(String path, String modelClass, String animationFamily,
                       ModelPartStructure.Fingerprint structure, String failure) {}
    public record Row(Identifier entity, List<String> renderers, List<Slot> slots, List<String> gaps) {
        public Row {
            renderers = renderers.stream().sorted().distinct().toList();
            slots = slots.stream().sorted(SLOT_ORDER).toList();
            gaps = gaps.stream().sorted().distinct().toList();
        }
    }
    public record Member(Identifier entity, String slot, String structure) {}
    public record Family(String technology, String modelClass, String animationFamily, List<Member> members) {
        public Family { members = List.copyOf(members); }
    }
    public record Cost(int renderers, int fields, int containerEntries, int modelSlots, int uniqueStructures,
                       int parts, int cubes) {}

    public static final class Inventory {
        private final CollisionCoverageDiscovery.Artifact coverage;
        private final List<Row> rows;
        private final List<Family> families;
        private final Cost cost;

        private Inventory(CollisionCoverageDiscovery.Artifact coverage, List<Row> rows, List<Family> families, Cost cost) {
            this.coverage = coverage;
            this.rows = List.copyOf(rows);
            this.families = List.copyOf(families);
            this.cost = cost;
            if (!coverage.coverage().rows().stream().map(CollisionCoverageScanner.Row::entity).toList()
                    .equals(rows.stream().map(Row::entity).toList()))
                throw new IllegalArgumentException("Census does not match canonical discovered population");
        }
        public CollisionCoverageDiscovery.Artifact coverage() { return coverage; }
        public List<Row> rows() { return rows; }
        public List<Family> families() { return families; }
        public Cost cost() { return cost; }
        public String canonicalText() {
            var text = new StringBuilder(TOOL_VERSION + "|");
            ModelPartStructure.frame(text, coverage.canonicalText());
            for (var row : rows) {
                text.append("row|"); ModelPartStructure.frame(text, row.entity().toString());
                text.append("renderers:").append(row.renderers().size()).append('|');
                row.renderers().forEach(value -> ModelPartStructure.frame(text, value));
                text.append("slots:").append(row.slots().size()).append('|');
                for (var slot : row.slots()) {
                    ModelPartStructure.frame(text, slot.path());
                    ModelPartStructure.frame(text, slot.modelClass());
                    ModelPartStructure.frame(text, slot.animationFamily());
                    ModelPartStructure.frame(text, slot.failure());
                    if (slot.structure() == null) text.append("no_structure|");
                    else {
                        text.append("structure|"); ModelPartStructure.frame(text, slot.structure().digest());
                        text.append(slot.structure().parts()).append('|').append(slot.structure().cubes()).append('|')
                            .append(slot.structure().solidPieces()).append('|');
                    }
                }
                text.append("gaps:").append(row.gaps().size()).append('|');
                row.gaps().forEach(value -> ModelPartStructure.frame(text, value));
            }
            return text.toString();
        }
        public String digest() { return ModelPartStructure.hash(canonicalText()); }
    }

    /** Caller supplies the actual pinned environment identity and canonical binding catalog. */
    public static Inventory inspect(Minecraft client, CollisionCoverageDiscovery.Target target,
                                    CollisionBindingCatalog catalog,
                                    Map<Identifier, CollisionCoverageScanner.ExceptionRule> exceptions) {
        if (client == null || !client.isSameThread()) throw new IllegalStateException("Census requires the client thread");
        var coverage = CollisionCoverageDiscovery.scan(target, catalog, exceptions);
        var dispatcher = client.getEntityRenderDispatcher();
        var regular = mapField(dispatcher, "renderers");
        var player = mapField(dispatcher, "playerRenderers");
        var mannequin = mapField(dispatcher, "mannequinRenderers");
        var pass = new Pass();
        var rows = new ArrayList<Row>();
        for (var row : coverage.coverage().rows()) {
            var type = BuiltInRegistries.ENTITY_TYPE.getValue(row.entity());
            var inspector = new RendererInspection(pass);
            // These two adapters mirror 26.2's structural avatar dispatch, not a species pose list.
            if (type == EntityTypes.PLAYER) inspector.avatar(player, "dispatcher.playerRenderers");
            else if (type == EntityTypes.MANNEQUIN) inspector.avatar(mannequin, "dispatcher.mannequinRenderers");
            else inspector.renderer(regular.get(type), "dispatcher.renderers");
            rows.add(new Row(row.entity(), inspector.renderers, inspector.slots, inspector.gaps));
        }
        var grouped = new TreeMap<String, List<Member>>();
        var descriptors = new TreeMap<String, Slot>();
        for (var row : rows) for (var slot : row.slots()) {
            if (slot.structure() == null) continue;
            String key = slot.modelClass() + "|" + slot.animationFamily();
            descriptors.putIfAbsent(key, slot);
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>())
                .add(new Member(row.entity(), slot.path(), slot.structure().digest()));
        }
        var families = new ArrayList<Family>();
        grouped.forEach((key, members) -> {
            var slot = descriptors.get(key);
            families.add(new Family("ModelPart", slot.modelClass(), slot.animationFamily(), members));
        });
        return new Inventory(coverage, rows, families, pass.cost());
    }

    /** Bounded one-renderer surface also useful for external technology preparation probes. */
    public static Row inspectRenderer(Identifier entity, EntityRenderer<?, ?> renderer) {
        if (entity == null) throw new IllegalArgumentException("Missing census entity");
        var inspector = new RendererInspection(new Pass());
        inspector.renderer(renderer, "renderer");
        return new Row(entity, inspector.renderers, inspector.slots, inspector.gaps);
    }

    private static final class Pass {
        private final Map<Model<?>, ModelPartStructure.Fingerprint> structures = new IdentityHashMap<>();
        private final Map<Model<?>, String> failures = new IdentityHashMap<>();
        private int renderers, fields, entries, slots, parts, cubes;
        private Cost cost() { return new Cost(renderers, fields, entries, slots, structures.size(), parts, cubes); }
        private ModelPartStructure.Fingerprint structure(Model<?> model) {
            if (failures.containsKey(model)) throw new IllegalArgumentException(failures.get(model));
            var prior = structures.get(model);
            if (prior != null) return prior;
            if (structures.size() + failures.size() >= MAX_STRUCTURES)
                throw new IllegalArgumentException("census:structure_limit");
            try {
                var next = ModelPartStructure.inspect(model.root());
                structures.put(model, next); parts += next.parts(); cubes += next.cubes();
                return next;
            } catch (IllegalArgumentException invalid) {
                failures.put(model, reason(invalid)); throw invalid;
            }
        }
    }

    private static final class RendererInspection {
        private final Pass pass;
        private final List<String> renderers = new ArrayList<>();
        private final List<Slot> slots = new ArrayList<>();
        private final List<String> gaps = new ArrayList<>(List.of("candidate_selection_pose_and_root_transform_unverified",
            "field_inventory_does_not_prove_dynamic_or_composite_completeness"));
        private final Set<Object> active = Collections.newSetFromMap(new IdentityHashMap<>());
        private int fields, entries;
        private RendererInspection(Pass pass) { this.pass = pass; }
        private void avatar(Map<?, ?> map, String path) {
            if (map.isEmpty()) gaps.add(path + ":missing_avatar_variants");
            walkMap(map, path, 0, true);
        }
        private void renderer(Object value, String path) {
            if (!(value instanceof EntityRenderer<?, ?> renderer)) { gaps.add(path + ":missing_renderer"); return; }
            pass.renderers++; renderers.add(path + "=" + renderer.getClass().getName());
            fields(renderer, path, 0);
            if (slots.isEmpty()) gaps.add(path + ":no_inspectable_model");
        }
        private void walk(Object value, String path, int depth) {
            if (value == null) return;
            if (depth > MAX_DEPTH) { gaps.add(path + ":container_depth_limit"); return; }
            if (value instanceof Model<?> model) { model(model, path); return; }
            if (!active.add(value)) { gaps.add(path + ":container_cycle"); return; }
            try {
                if (value instanceof Map<?, ?> map) walkMap(map, path, depth, false);
                else if (value instanceof List<?> list) {
                    if (list.size() > MAX_CONTAINER) { gaps.add(path + ":container_limit"); return; }
                    for (int i = 0; i < list.size(); i++) {
                        if (!entry(path)) return;
                        walk(list.get(i), path + "[" + i + "]", depth + 1);
                    }
                } else if (value instanceof Object[] array) {
                    if (array.length > MAX_CONTAINER) { gaps.add(path + ":container_limit"); return; }
                    for (int i = 0; i < array.length; i++) {
                        if (!entry(path)) return;
                        walk(array[i], path + "[" + i + "]", depth + 1);
                    }
                } else if (value instanceof RenderLayer<?, ?> || value.getClass().isRecord()) fields(value, path, depth);
                // Renderer links on layers point to their parent, not an independently owned model slot.
                else if (value instanceof Iterable<?>) gaps.add(path + ":unsupported_unordered_container");
            } finally { active.remove(value); }
        }
        private boolean entry(String path) {
            if (entries >= MAX_ENTRIES) { gaps.add(path + ":entry_limit"); return false; }
            entries++; pass.entries++; return true;
        }
        private void walkMap(Map<?, ?> map, String path, int depth, boolean avatars) {
            if (map.size() > MAX_CONTAINER) { gaps.add(path + ":container_limit"); return; }
            var ordered = new TreeMap<String, Object>();
            for (var entry : map.entrySet()) {
                var key = entry.getKey();
                if (!(key instanceof String || key instanceof Identifier || key instanceof Enum<?> || key instanceof Integer)) {
                    gaps.add(path + ":unsupported_map_key"); return;
                }
                // Enum.toString may be overridden/mutable; names and declaring classes are stable slot identity.
                String name = key instanceof Enum<?> variant
                    ? variant.getDeclaringClass().getName() + ":" + variant.name()
                    : key.getClass().getName() + ":" + key;
                if (name.length() > 256) { gaps.add(path + ":map_key_limit"); return; }
                if (ordered.putIfAbsent(name, entry.getValue()) != null) { gaps.add(path + ":ambiguous_map_key"); return; }
            }
            for (var item : ordered.entrySet()) {
                if (!entry(path)) return;
                String slot = path + "[" + item.getKey().length() + ":" + item.getKey() + "]";
                if (avatars) renderer(item.getValue(), slot);
                else walk(item.getValue(), slot, depth + 1);
            }
        }
        private void fields(Object owner, String path, int depth) {
            var declared = new ArrayList<Field>();
            for (Class<?> type = owner.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
                for (var field : type.getDeclaredFields()) if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) declared.add(field);
                if (declared.size() > MAX_FIELDS - fields) { gaps.add(path + ":field_limit"); return; }
            }
            declared.sort(Comparator.comparing(field -> field.getDeclaringClass().getName() + "." + field.getName()));
            for (var field : declared) {
                fields++; pass.fields++;
                String slot = path + "/" + field.getDeclaringClass().getName() + "." + field.getName();
                try {
                    if (!field.trySetAccessible()) { gaps.add(slot + ":inaccessible_field"); continue; }
                    walk(field.get(owner), slot, depth + 1);
                } catch (IllegalAccessException | RuntimeException invalid) { gaps.add(slot + ":inspection_failed:" + reason(invalid)); }
            }
        }
        private void model(Model<?> model, String path) {
            if (slots.size() >= MAX_SLOTS) { gaps.add(path + ":slot_limit"); return; }
            pass.slots++;
            var family = animationFamily(model.getClass());
            try {
                var structure = pass.structure(model);
                slots.add(new Slot(path, model.getClass().getName(), family, structure, ""));
                if (structure.solidPieces() == 0) gaps.add(path + ":no_material_volume");
            } catch (IllegalArgumentException invalid) {
                slots.add(new Slot(path, model.getClass().getName(), family, null, reason(invalid)));
                gaps.add(path + ":invalid_structure:" + reason(invalid));
            }
        }
    }

    private static String animationFamily(Class<?> type) {
        for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
            var signatures = new TreeSet<String>();
            for (var method : owner.getDeclaredMethods()) if (method.getName().equals("setupAnim") && !method.isBridge()
                    && !method.isSynthetic() && !Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 1)
                signatures.add(owner.getName() + "(" + method.getParameterTypes()[0].getName() + ")");
            if (!signatures.isEmpty()) return String.join(";", signatures);
        }
        return "missing_setupAnim";
    }

    private static Map<?, ?> mapField(Object owner, String name) {
        try {
            var field = owner.getClass().getDeclaredField(name);
            if (!field.trySetAccessible() || !(field.get(owner) instanceof Map<?, ?> map))
                throw new IllegalArgumentException("census:missing_dispatcher_map:" + name);
            return map;
        } catch (ReflectiveOperationException invalid) { throw new IllegalArgumentException("census:missing_dispatcher_map:" + name, invalid); }
    }

    private static String reason(Throwable invalid) {
        var message = invalid.getMessage();
        if (message == null) return invalid.getClass().getSimpleName();
        return message.length() <= 256 ? message : message.substring(0, 256);
    }
}
