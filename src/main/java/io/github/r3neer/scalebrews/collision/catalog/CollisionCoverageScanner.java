package io.github.r3neer.scalebrews.collision.catalog;

import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.resources.Identifier;

/**
 * Deterministic coverage classifier for one declared target set.
 *
 * <p>This class deliberately does not discover entity types itself. Discovery is a tooling concern:
 * callers hand it the complete set produced by the target scanner, and this kernel guarantees that
 * every discovered id receives exactly one row. G6 can therefore replace/extend registry discovery
 * without creating a second coverage authority.</p>
 */
public final class CollisionCoverageScanner {
    public static final int MAX_TARGETS = 16_384;

    private CollisionCoverageScanner() {}

    public enum Status {
        FULL,
        SAFE_PARTIAL,
        EXCLUDED,
        UNRESOLVED
    }

    /**
     * Explicit exception metadata. FULL is never asserted manually: it is derived from canonical
     * binding coverage. UNRESOLVED is likewise an outcome rather than a declaration.
     */
    public record ExceptionRule(Status status, String reason) {
        public ExceptionRule {
            if (status != Status.SAFE_PARTIAL && status != Status.EXCLUDED)
                throw new IllegalArgumentException("Coverage exceptions may only declare SAFE_PARTIAL or EXCLUDED");
            reason = checkedReason(reason);
        }
    }

    /** Stable subset of canonical binding data that explains why a row received its status. */
    public record BindingEvidence(Map<String, String> variant, Identifier geometryEngine, Identifier poseEngine,
                                  Identifier rootTransform, Set<String> excludedStates) {
        public BindingEvidence {
            Objects.requireNonNull(variant, "variant");
            Objects.requireNonNull(geometryEngine, "geometryEngine");
            Objects.requireNonNull(poseEngine, "poseEngine");
            Objects.requireNonNull(rootTransform, "rootTransform");
            Objects.requireNonNull(excludedStates, "excludedStates");
            variant = Collections.unmodifiableMap(new TreeMap<>(variant));
            excludedStates = Collections.unmodifiableSet(new TreeSet<>(excludedStates));
        }

        static BindingEvidence from(CollisionBinding binding) {
            return new BindingEvidence(binding.variant(), binding.geometry().engine(), binding.pose().engine(),
                binding.rootTransform(), binding.excludedStates());
        }

        /**
         * Canonical evidence preimage. Every variable-length value is length-framed rather than
         * delimiter-escaped so every string accepted by the binding schema has an injective encoding.
         */
        String canonical() {
            var text = new StringBuilder("binding-evidence-v2|");
            text.append("variants:").append(variant.size()).append('|');
            variant.forEach((key, value) -> {
                appendFrame(text, key);
                appendFrame(text, value);
            });
            text.append("geometry:");
            appendFrame(text, geometryEngine.toString());
            text.append("pose:");
            appendFrame(text, poseEngine.toString());
            text.append("root:");
            appendFrame(text, rootTransform.toString());
            text.append("excluded:").append(excludedStates.size()).append('|');
            excludedStates.forEach(state -> appendFrame(text, state));
            return text.toString();
        }
    }

    public record Row(Identifier entity, Status status, String reason, List<BindingEvidence> bindings) {
        public Row {
            Objects.requireNonNull(entity, "entity");
            Objects.requireNonNull(status, "status");
            reason = checkedReason(reason);
            Objects.requireNonNull(bindings, "bindings");
            var ordered = new ArrayList<>(bindings);
            ordered.sort(Comparator.comparing(BindingEvidence::canonical));
            bindings = List.copyOf(ordered);
        }
    }

    /** Immutable deterministic report suitable for CI gating and reproducible hashing. */
    public record Report(List<Row> rows) {
        public Report {
            Objects.requireNonNull(rows, "rows");
            var ordered = new ArrayList<>(rows);
            ordered.sort(Comparator.comparing(row -> row.entity().toString()));
            Identifier previous = null;
            for (var row : ordered) {
                if (previous != null && previous.equals(row.entity()))
                    throw new IllegalArgumentException("Duplicate coverage row for " + row.entity());
                previous = row.entity();
            }
            rows = List.copyOf(ordered);
        }

        public long count(Status status) {
            Objects.requireNonNull(status, "status");
            return rows.stream().filter(row -> row.status() == status).count();
        }

        public Map<Status, Long> counts() {
            var counts = new EnumMap<Status, Long>(Status.class);
            for (var status : Status.values()) counts.put(status, count(status));
            return Collections.unmodifiableMap(counts);
        }

        public boolean resolved() {
            return count(Status.UNRESOLVED) == 0;
        }

        /** NFR-032 gate: a declared target with any unresolved row is not acceptable. */
        public void requireResolved() {
            var unresolved = rows.stream().filter(row -> row.status() == Status.UNRESOLVED).map(Row::entity).toList();
            if (!unresolved.isEmpty())
                throw new IllegalStateException("Coverage report contains unresolved entity types: " + unresolved);
        }

        /** Canonical length-framed text used only as stable digest input, not as a user-facing file format. */
        public String canonicalText() {
            var text = new StringBuilder("collision-coverage-v2|");
            text.append("rows:").append(rows.size()).append('|');
            for (var row : rows) {
                var rowText = new StringBuilder("row-v2|");
                appendFrame(rowText, row.entity().toString());
                appendFrame(rowText, row.status().name());
                appendFrame(rowText, row.reason());
                rowText.append("bindings:").append(row.bindings().size()).append('|');
                for (var binding : row.bindings()) appendFrame(rowText, binding.canonical());
                appendFrame(text, rowText.toString());
            }
            return text.toString();
        }

        public String digest() {
            try {
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonicalText().getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException("SHA-256 unavailable", impossible);
            }
        }
    }

    /**
     * Classifies exactly the discovered target ids against the canonical binding catalog.
     *
     * <ul>
     *   <li>default binding + no known excluded states in any applicable binding => FULL;</li>
     *   <li>known excluded states, or variant-only coverage => SAFE_PARTIAL;</li>
     *   <li>explicit technical exclusion without a canonical binding => EXCLUDED;</li>
     *   <li>otherwise => UNRESOLVED.</li>
     * </ul>
     *
     * Explicit SAFE_PARTIAL may downgrade an otherwise FULL row when a known limitation is not yet
     * representable by {@link CollisionBinding#excludedStates()}. It may never manufacture coverage
     * for an entity that has no canonical binding.
     */
    public static Report scan(Collection<Identifier> discoveredTargets, CollisionBindingCatalog catalog,
                              Map<Identifier, ExceptionRule> exceptions) {
        Objects.requireNonNull(discoveredTargets, "discoveredTargets");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(exceptions, "exceptions");
        if (discoveredTargets.size() > MAX_TARGETS) throw new IllegalArgumentException("Coverage target limit exceeded");

        var targets = new TreeSet<Identifier>(Comparator.comparing(Identifier::toString));
        for (var target : discoveredTargets) {
            if (target == null) throw new IllegalArgumentException("Null coverage target");
            targets.add(target);
        }
        if (targets.size() > MAX_TARGETS) throw new IllegalArgumentException("Coverage target limit exceeded");

        for (var entry : exceptions.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) throw new IllegalArgumentException("Invalid coverage exception");
            if (!targets.contains(entry.getKey()))
                throw new IllegalArgumentException("Coverage exception is outside the discovered target set: " + entry.getKey());
        }

        var byEntity = catalog.snapshot();
        List<Row> rows = new ArrayList<>(targets.size());
        for (var entity : targets) {
            var bindings = byEntity.getOrDefault(entity, List.of());
            var evidence = bindings.stream().map(BindingEvidence::from).toList();
            var exception = exceptions.get(entity);

            if (exception != null && exception.status() == Status.EXCLUDED) {
                if (!bindings.isEmpty())
                    throw new IllegalArgumentException("EXCLUDED coverage target still has canonical bindings: " + entity);
                rows.add(new Row(entity, Status.EXCLUDED, exception.reason(), evidence));
                continue;
            }

            if (bindings.isEmpty()) {
                if (exception != null)
                    throw new IllegalArgumentException("SAFE_PARTIAL coverage requires at least one canonical binding: " + entity);
                rows.add(new Row(entity, Status.UNRESOLVED, "no canonical binding or explicit technical exclusion", evidence));
                continue;
            }

            if (exception != null) {
                rows.add(new Row(entity, Status.SAFE_PARTIAL, exception.reason(), evidence));
                continue;
            }

            var defaultBinding = bindings.stream().filter(binding -> binding.variant().isEmpty()).findFirst();
            if (defaultBinding.isEmpty()) {
                rows.add(new Row(entity, Status.SAFE_PARTIAL,
                    "canonical coverage exists only for explicit variants", evidence));
                continue;
            }

            var excludedStates = new TreeSet<String>();
            bindings.forEach(binding -> excludedStates.addAll(binding.excludedStates()));
            if (!excludedStates.isEmpty()) {
                rows.add(new Row(entity, Status.SAFE_PARTIAL,
                    "canonical bindings exclude states: " + String.join(",", excludedStates), evidence));
            } else {
                rows.add(new Row(entity, Status.FULL, "default canonical binding covers ordinary states", evidence));
            }
        }
        return new Report(rows);
    }

    private static String checkedReason(String reason) {
        if (reason == null) throw new IllegalArgumentException("Coverage reason is required");
        reason = reason.strip();
        if (reason.isEmpty() || reason.length() > 512) throw new IllegalArgumentException("Invalid coverage reason");
        return reason;
    }

    private static void appendFrame(StringBuilder target, String value) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(value, "value");
        target.append(value.length()).append(':').append(value);
    }
}
