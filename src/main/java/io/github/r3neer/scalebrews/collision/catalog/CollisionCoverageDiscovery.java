package io.github.r3neer.scalebrews.collision.catalog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * Tooling-only target discovery for collision coverage.
 *
 * <p>No method in this class is wired into a server tick or entity lifecycle path. A coverage job
 * invokes it once against the frozen built-in registry, then feeds the resulting ids into
 * {@link CollisionCoverageScanner}. Entity types are classified from their declared base class;
 * discovery never constructs/spawns an entity just to ask what it is.</p>
 */
public final class CollisionCoverageDiscovery {
    public static final int MAX_NAMESPACES = 256;
    public static final int MAX_INPUTS = 1024;

    private CollisionCoverageDiscovery() {}

    /** Exact identity of one reproducible target environment. */
    public record Target(Identifier id, String gameVersion, Set<String> namespaces, Map<String, String> inputs) {
        public Target {
            Objects.requireNonNull(id, "id");
            gameVersion = checkedValue(gameVersion, "game version");
            Objects.requireNonNull(namespaces, "namespaces");
            Objects.requireNonNull(inputs, "inputs");
            if (namespaces.isEmpty() || namespaces.size() > MAX_NAMESPACES)
                throw new IllegalArgumentException("Invalid coverage target namespaces");
            if (inputs.isEmpty() || inputs.size() > MAX_INPUTS)
                throw new IllegalArgumentException("Coverage target must identify its versioned inputs");

            var orderedNamespaces = new TreeSet<String>();
            for (var namespace : namespaces) {
                if (namespace == null || !namespace.matches("[a-z0-9_.-]{1,64}"))
                    throw new IllegalArgumentException("Invalid coverage namespace: " + namespace);
                orderedNamespaces.add(namespace);
            }
            namespaces = Collections.unmodifiableSet(orderedNamespaces);

            var orderedInputs = new TreeMap<String, String>();
            inputs.forEach((key, value) -> {
                var checkedKey = checkedValue(key, "input id");
                if (checkedKey.length() > 128) throw new IllegalArgumentException("Coverage input id too long");
                var checked = checkedValue(value, "input identity");
                if (checked.length() > 512) throw new IllegalArgumentException("Coverage input identity too long");
                if (orderedInputs.put(checkedKey, checked) != null)
                    throw new IllegalArgumentException("Duplicate coverage input id: " + checkedKey);
            });
            inputs = Collections.unmodifiableMap(orderedInputs);
        }

        String canonicalText() {
            var text = new StringBuilder("coverage-target-v1\n");
            text.append("id=").append(escape(id.toString())).append('\n');
            text.append("game=").append(escape(gameVersion)).append('\n');
            for (var namespace : namespaces) text.append("namespace=").append(escape(namespace)).append('\n');
            inputs.forEach((key, value) -> text.append("input=").append(escape(key)).append('|').append(escape(value)).append('\n'));
            return text.toString();
        }
    }

    /** Frozen automatic discovery result, sorted by registry id. */
    public record Discovery(Target target, List<Identifier> livingEntityTypes) {
        public Discovery {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(livingEntityTypes, "livingEntityTypes");
            var ordered = new TreeSet<Identifier>(Comparator.comparing(Identifier::toString));
            for (var id : livingEntityTypes) {
                if (id == null || !target.namespaces().contains(id.getNamespace()))
                    throw new IllegalArgumentException("Discovered entity is outside target namespaces: " + id);
                ordered.add(id);
            }
            livingEntityTypes = List.copyOf(ordered);
        }
    }

    /** Target metadata and coverage rows share one acceptance identity. */
    public record Artifact(Target target, CollisionCoverageScanner.Report coverage) {
        public Artifact {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(coverage, "coverage");
            for (var row : coverage.rows()) {
                if (!target.namespaces().contains(row.entity().getNamespace()))
                    throw new IllegalArgumentException("Coverage row is outside target namespaces: " + row.entity());
            }
        }

        public String canonicalText() {
            return target.canonicalText() + coverage.canonicalText();
        }

        public String digest() {
            try {
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonicalText().getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException("SHA-256 unavailable", impossible);
            }
        }

        public void requireResolved() {
            coverage.requireResolved();
        }
    }

    /** Enumerates every registered LivingEntity whose registry namespace belongs to the target. */
    public static Discovery discover(Target target) {
        Objects.requireNonNull(target, "target");
        List<Identifier> living = new ArrayList<>();
        for (var type : BuiltInRegistries.ENTITY_TYPE) {
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id == null || !target.namespaces().contains(id.getNamespace())) continue;
            if (LivingEntity.class.isAssignableFrom(type.getBaseClass())) living.add(id);
        }
        if (living.size() > CollisionCoverageScanner.MAX_TARGETS)
            throw new IllegalArgumentException("Coverage discovery target limit exceeded");
        return new Discovery(target, living);
    }

    /** One-shot tooling path from registry discovery to deterministic classification. */
    public static Artifact scan(Target target, CollisionBindingCatalog catalog,
                                Map<Identifier, CollisionCoverageScanner.ExceptionRule> exceptions) {
        var discovery = discover(target);
        return new Artifact(target, CollisionCoverageScanner.scan(discovery.livingEntityTypes(), catalog, exceptions));
    }

    private static String checkedValue(String value, String label) {
        if (value == null) throw new IllegalArgumentException("Missing coverage " + label);
        value = value.strip();
        if (value.isEmpty()) throw new IllegalArgumentException("Blank coverage " + label);
        return value;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n").replace("\r", "\\r");
    }
}
