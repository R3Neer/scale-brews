package io.github.r3neer.scalebrews.test;

import com.google.gson.GsonBuilder;
import io.github.r3neer.scalebrews.client.collision.preparation.ModelFamilyCensus;
import io.github.r3neer.scalebrews.client.collision.preparation.ModelPartStructure;
import io.github.r3neer.scalebrews.collision.catalog.CollisionBindingCatalog;
import io.github.r3neer.scalebrews.collision.catalog.CollisionCoverageDiscovery;
import io.github.r3neer.scalebrews.collision.catalog.CollisionCoverageScanner;
import io.github.r3neer.scalebrews.collision.internal.WorldAnatomyCatalog;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;

/** Original renderer census, not synthetic coverage or a FULL Minecraft catalog. */
public final class S45MinecraftCensusProof implements FabricClientGameTest {
    private record State(PartPose current, PartPose initial, boolean visible, boolean skipDraw) {}
    private static void require(boolean ok, String reason) { if (!ok) throw new AssertionError(reason); }

    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            var catalog = new AtomicReference<CollisionBindingCatalog>();
            var serverLiving = new TreeSet<String>();
            var nullFactories = new TreeSet<String>();
            world.getServer().runOnServer(server -> {
                catalog.set(new WorldAnatomyCatalog().reload(server.getResourceManager()).catalog());
                // Independent membership: actual server factories plus the actual connected player.
                // getBaseClass() is Entity.class in 26.2; repeating DefaultAttributes would not cross-check discovery.
                for (var type : BuiltInRegistries.ENTITY_TYPE) {
                    var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                    if (!id.getNamespace().equals("minecraft")) continue;
                    var entity = type.create(server.overworld(), EntitySpawnReason.COMMAND);
                    if (entity == null) nullFactories.add(id.toString());
                    else if (entity instanceof LivingEntity) serverLiving.add(id.toString());
                }
                for (var player : server.getPlayerList().getPlayers())
                    serverLiving.add(BuiltInRegistries.ENTITY_TYPE.getKey(player.getType()).toString());
                require(serverLiving.contains("minecraft:player"), "Independent player population absent");
            });
            context.runOnClient(client -> {
                var inputs = identity(client);
                var target = new CollisionCoverageDiscovery.Target(Identifier.parse("scalebrews:minecraft_26_2_census"),
                    SharedConstants.getCurrentVersion().name(), Set.of("minecraft"), inputs);
                var originals = snapshots(client);
                long start = System.nanoTime();
                var inventory = ModelFamilyCensus.inspect(client, target, catalog.get(), Map.of());
                long firstNanos = System.nanoTime() - start;
                var after = snapshots(client);
                require(originals.size() == after.size() && originals.entrySet().stream()
                    .allMatch(entry -> entry.getValue().equals(after.get(entry.getKey()))), "Census mutated original shared renderer models");
                var repeated = ModelFamilyCensus.inspect(client, target, catalog.get(), Map.of());
                require(inventory.canonicalText().equals(repeated.canonicalText()) && inventory.digest().equals(repeated.digest()),
                    "Repeated actual census changed identity");
                var discovered = inventory.rows().stream().map(row -> row.entity().toString()).toList();
                require(discovered.equals(List.copyOf(serverLiving)), "Independent server factory population differs: " + serverLiving + " / " + discovered);
                require(inventory.rows().stream().allMatch(row -> !row.renderers().isEmpty() && !row.slots().isEmpty()),
                    "Original Minecraft renderer/model missing from inventory");
                var player = row(inventory, "minecraft:player");
                require(player.renderers().size() == 2 && player.slots().stream().map(slot -> slot.structure().digest()).distinct().count() >= 2,
                    "Avatar structural alternatives omitted");
                require(row(inventory, "minecraft:mannequin").renderers().size() == 2, "Mannequin avatar dispatch omitted");
                require(row(inventory, "minecraft:cow").slots().size() >= 6, "Record-wrapped climate adult/baby model candidates omitted");
                require(row(inventory, "minecraft:pufferfish").slots().stream().map(slot -> slot.structure().digest()).distinct().count() >= 3,
                    "Pufferfish structural alternatives omitted");
                require(inventory.families().stream().anyMatch(family -> family.members().stream().map(ModelFamilyCensus.Member::entity).distinct().count() > 1),
                    "Reusable original family membership missing");
                require(inventory.rows().stream().allMatch(row -> row.gaps().contains("candidate_selection_pose_and_root_transform_unverified")),
                    "Candidate inventory falsely became verified physical selection");
                require(inventory.coverage().coverage().rows().stream().anyMatch(row -> row.status() == CollisionCoverageScanner.Status.UNRESOLVED),
                    "Unexpected resolved Minecraft base catalog");
                try { inventory.coverage().requireResolved(); throw new AssertionError("Unresolved coverage gate accepted"); }
                catch (IllegalStateException expected) { require(expected.getMessage().contains("unresolved"), "Wrong unresolved gate failure: " + expected); }
                var changedInputs = new TreeMap<>(inputs); changedInputs.put("selector", "different-exact-target-input");
                var changed = ModelFamilyCensus.inspect(client, new CollisionCoverageDiscovery.Target(target.id(), target.gameVersion(),
                    target.namespaces(), changedInputs), catalog.get(), Map.of());
                require(!inventory.digest().equals(changed.digest()), "Target input change did not invalidate census");
                structureBoundaries();
                var summary = new TreeMap<String, Object>();
                summary.put("tool", ModelFamilyCensus.TOOL_VERSION);
                summary.put("scope", "Original structure candidates; executable selectors/poses/root transforms remain unverified; G6 coverage gate RED");
                summary.put("target", target);
                summary.put("selectedResourcePackIds", client.getResourcePackRepository().getSelectedPacks().stream()
                    .map(pack -> pack.getId()).sorted().toList());
                summary.put("digest", inventory.digest());
                summary.put("coverageDigest", inventory.coverage().digest());
                summary.put("coverage", inventory.coverage().coverage());
                summary.put("rows", inventory.rows());
                summary.put("families", inventory.families());
                summary.put("cost", inventory.cost());
                summary.put("firstPreparationNanos", firstNanos);
                summary.put("originalMutablePartsChecked", originals.size());
                summary.put("independentServerLivingTypes", serverLiving);
                summary.put("serverNullFactories", nullFactories);
                save("minecraft-census.json", new GsonBuilder().setPrettyPrinting().create().toJson(summary));
                save("minecraft-census.canonical.txt", inventory.canonicalText());
                System.out.println("S45_CENSUS_PASS living=" + inventory.rows().size() + " families=" + inventory.families().size()
                    + " slots=" + inventory.cost().modelSlots() + " structures=" + inventory.cost().uniqueStructures()
                    + " originalParts=" + originals.size() + " preparationNanos=" + firstNanos + " digest=" + inventory.digest());
            });
        }
    }

    private static ModelFamilyCensus.Row row(ModelFamilyCensus.Inventory inventory, String entity) {
        return inventory.rows().stream().filter(row -> row.entity().toString().equals(entity)).findFirst().orElseThrow();
    }

    private static void structureBoundaries() {
        var children = new TreeMap<String, ModelPart>();
        var empty = new ModelPart(List.of(), children);
        var original = empty.storePose(); empty.visible = false; empty.skipDraw = true; empty.x = 31;
        var stable = ModelPartStructure.inspect(empty);
        require(stable.solidPieces() == 0 && stable.cubes() == 0, "Empty structural fixture manufactured material");
        require(!empty.visible && empty.skipDraw && empty.x == 31, "Fingerprint changed current source fields");
        empty.x = -20;
        require(stable.equals(ModelPartStructure.inspect(empty)), "Current animation fields altered initial structure identity");
        empty.setInitialPose(PartPose.offset(2, 0, 0));
        require(!stable.digest().equals(ModelPartStructure.inspect(empty).digest()), "Initial structure change did not invalidate identity");
        empty.setInitialPose(original);
        children.put("cycle", empty);
        reject(empty, "cycle_or_shared_child"); children.clear();
        children.put("\uD800", new ModelPart(List.of(), Map.of()));
        reject(empty, "malformed_unicode"); children.clear();
        ModelPart chain = new ModelPart(List.of(), Map.of());
        for (int i = 0; i <= ModelPartStructure.MAX_DEPTH; i++) chain = new ModelPart(List.of(), Map.of("n", chain));
        reject(chain, "depth_limit");
        empty.setInitialPose(new PartPose(Float.NaN, 0, 0, 0, 0, 0, 1, 1, 1));
        reject(empty, "non_finite");
    }
    private static void reject(ModelPart root, String reason) {
        try { ModelPartStructure.inspect(root); throw new AssertionError("Invalid structure accepted: " + reason); }
        catch (IllegalArgumentException expected) { require(expected.getMessage().contains(reason), "Wrong structure diagnostic " + expected); }
    }

    private static Map<String, String> identity(Minecraft client) {
        try {
            var inputs = new TreeMap<String, String>();
            inputs.put("selector", "all_registered_minecraft_living_types_original_dispatcher_models_initial_structure");
            inputs.put("tool", ModelFamilyCensus.TOOL_VERSION);
            for (var type : List.of(ModelFamilyCensus.class, ModelPartStructure.class, CollisionCoverageDiscovery.class,
                    CollisionCoverageScanner.class, S45MinecraftCensusProof.class,
                    Class.forName("io.github.r3neer.scalebrews.client.collision.preparation.ModelPartGeometryExtractor"))) {
                try (var stream = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
                    require(stream != null, "Missing loaded tool bytecode"); inputs.put("class:" + type.getSimpleName(), hash(stream.readAllBytes()));
                }
            }
            var source = Path.of(Minecraft.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            require(Files.isRegularFile(source), "Minecraft source JAR identity unavailable: " + source);
            inputs.put("minecraft-source-jar-sha256", hash(Files.readAllBytes(source)));
            for (var mod : FabricLoader.getInstance().getAllMods()) {
                var id = mod.getMetadata().getId();
                inputs.put("version:" + id, mod.getMetadata().getVersion().getFriendlyString());
                int i = 0;
                for (var path : mod.getOrigin().getPaths()) {
                    if (Files.isRegularFile(path)) inputs.put("jar:" + id + ":" + i++, hash(Files.readAllBytes(path)));
                    else if (Files.isDirectory(path)) inputs.put("directory:" + id + ":" + i++, directoryHash(path));
                    else throw new AssertionError("Unidentified source origin " + id + ": " + path);
                }
            }
            var packs = client.getResourcePackRepository().getSelectedPacks().stream()
                .map(pack -> pack.getId()).sorted().toList();
            inputs.put("resource-pack-id-manifest-sha256", hash(String.join("\n", packs).getBytes(StandardCharsets.UTF_8)));
            return inputs;
        } catch (Exception invalid) { throw new AssertionError("Exact census target identity failed", invalid); }
    }
    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    private static String directoryHash(Path directory) throws Exception {
        var text = new StringBuilder("source-directory-v1|");
        try (var stream = Files.walk(directory)) {
            for (var file : stream.filter(Files::isRegularFile).sorted().toList()) {
                String relative = directory.relativize(file).toString().replace('\\', '/');
                text.append(relative.length()).append(':').append(relative).append(hash(Files.readAllBytes(file)));
            }
        }
        return hash(text.toString().getBytes(StandardCharsets.UTF_8));
    }
    private static void save(String name, String text) {
        try {
            Path folder = FabricLoader.getInstance().getGameDir().resolve("s45-census"); Files.createDirectories(folder);
            Files.writeString(folder.resolve(name), text, StandardCharsets.UTF_8);
        } catch (Exception invalid) { throw new AssertionError(invalid); }
    }

    private static Map<ModelPart, State> snapshots(Minecraft client) {
        var states = new IdentityHashMap<ModelPart, State>();
        var seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        capture(client.getEntityRenderDispatcher(), states, seen, 0);
        return states;
    }
    private static void capture(Object owner, Map<ModelPart, State> states, Set<Object> seen, int depth) {
        if (owner == null || depth > 20 || !seen.add(owner)) return;
        if (owner instanceof Model<?> model) {
            for (var part : model.allParts()) states.put(part, new State(part.storePose(), part.getInitialPose(), part.visible, part.skipDraw));
        } else if (owner instanceof Map<?, ?> map) map.values().forEach(value -> capture(value, states, seen, depth + 1));
        else if (owner instanceof Iterable<?> iterable) iterable.forEach(value -> capture(value, states, seen, depth + 1));
        else if (owner instanceof Object[] array) for (var value : array) capture(value, states, seen, depth + 1);
        else if (owner == Minecraft.getInstance().getEntityRenderDispatcher() || owner instanceof EntityRenderer<?, ?>
                || owner instanceof RenderLayer<?, ?> || owner.getClass().isRecord()) {
            for (Class<?> type = owner.getClass(); type != null && type != Object.class; type = type.getSuperclass())
                for (var field : type.getDeclaredFields()) if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) try {
                    field.setAccessible(true); capture(field.get(owner), states, seen, depth + 1);
                } catch (ReflectiveOperationException invalid) { throw new AssertionError(invalid); }
        }
    }
}
