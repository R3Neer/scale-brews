package io.github.r3neer.scalebrews.collision.internal;

import com.google.gson.Gson;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.api.spi.RootTransformProvider;
import io.github.r3neer.scalebrews.collision.catalog.CollisionBindingCatalog;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.migration.LegacyAnatomyCatalogMigration;
import io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram;
import io.github.r3neer.scalebrews.collision.pose.PoseProgram;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;

/** Publish canonical bindings and prepared geometry/programs together, only after every reference has validated. */
public final class WorldAnatomyCatalog {
    /** Executable bridge with geometry, joint evaluator, and root authority resolved for one accepted revision. */
    public record Binding(CollisionBinding selection, ModelGeometry model, PoseEngine.Bound poses,
                          RootTransformProvider root, Identifier legacyPoseProvider) {
        public Binding {
            Objects.requireNonNull(selection, "selection");
            Objects.requireNonNull(model, "model");
            Objects.requireNonNull(poses, "poses");
            Objects.requireNonNull(root, "root");
            Objects.requireNonNull(legacyPoseProvider, "legacyPoseProvider");
        }
    }

    /** Full canonical catalog plus revision-local pose programs and the subset executable by the S16 bridge. */
    public record Snapshot(long revision, Map<String, ModelGeometry> models, Map<String, PoseProgram> posePrograms,
                           Map<String, CitadelPoseProgram> citadelPosePrograms,
                           CollisionBindingCatalog catalog, Map<Identifier, Binding> bindings) {
        /** Source-compatible constructor for pre-S20 callers. */
        public Snapshot(long revision, Map<String, ModelGeometry> models, Map<String, PoseProgram> posePrograms,
                        CollisionBindingCatalog catalog, Map<Identifier, Binding> bindings) {
            this(revision, models, posePrograms, Map.of(), catalog, bindings);
        }
        public Snapshot {
            if (revision < 0 || catalog == null) throw new IllegalArgumentException("Invalid anatomical catalog snapshot");
            models = Map.copyOf(models);
            posePrograms = Map.copyOf(posePrograms);
            citadelPosePrograms = Map.copyOf(citadelPosePrograms);
            bindings = Map.copyOf(bindings);
        }
    }

    private record Accepted(Snapshot snapshot, AnatomyCatalogTransfer.PreparedBundle bundle) {}
    private volatile Accepted current = empty(0);

    public WorldAnatomyCatalog() {}
    public WorldAnatomyCatalog(long previousRevision) {
        if (previousRevision < 0) throw new IllegalArgumentException("Negative catalog revision");
        current = empty(previousRevision);
    }

    private static Accepted empty(long revision) {
        var catalog = new CollisionBindingCatalog(List.of());
        var snapshot = new Snapshot(revision, Map.of(), Map.of(), Map.of(), catalog, Map.of());
        return new Accepted(snapshot, AnatomyCatalogTransfer.prepareBundle(snapshot.models(), snapshot.posePrograms(),
            snapshot.citadelPosePrograms(), catalog.bindings()));
    }

    public Snapshot snapshot() { return current.snapshot(); }

    synchronized List<AnatomyCatalogPayload> preparedPackets(UUID epoch) {
        var accepted = current;
        return accepted.bundle().packets(epoch, accepted.snapshot().revision());
    }

    public Snapshot reload(net.minecraft.server.packs.resources.ResourceManager resources) {
        var models = read(resources, "scalebrews/entity_geometry", AnatomyCodecs.GEOMETRY);
        var programs = readPrograms(resources, "scalebrews/pose_programs");
        var citadelPrograms = readCitadelPrograms(resources, "scalebrews/citadel_pose_programs");
        List<CollisionBinding> bindings = new ArrayList<>(CollisionBindingCatalog.load(resources).bindings());
        bindings.addAll(LegacyAnatomyCatalogMigration.load(resources));
        return replace(models, programs, citadelPrograms, bindings);
    }

    private static <T> Map<String, T> read(net.minecraft.server.packs.resources.ResourceManager resources, String directory,
                                           com.mojang.serialization.Codec<T> codec) {
        var files = resources.listResources(directory, id -> id.getPath().endsWith(".json"));
        if (files.size() > 4096) throw new IllegalArgumentException("Too many resources under " + directory);
        Map<String, T> result = new TreeMap<>();
        long total = 0;
        for (var entry : files.entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                var text = readBounded(reader, total);
                total += text.length();
                var file = entry.getKey();
                var path = file.getPath();
                String id = Identifier.fromNamespaceAndPath(file.getNamespace(), path.substring(directory.length() + 1, path.length() - 5)).toString();
                result.put(id, codec.parse(com.mojang.serialization.JsonOps.INSTANCE,
                    com.google.gson.JsonParser.parseString(text)).getOrThrow());
            } catch (java.io.IOException | RuntimeException invalid) {
                throw new IllegalArgumentException("Invalid anatomical resource " + entry.getKey(), invalid);
            }
        }
        return result;
    }

    private static Map<String, PoseProgram> readPrograms(net.minecraft.server.packs.resources.ResourceManager resources, String directory) {
        var files = resources.listResources(directory, id -> id.getPath().endsWith(".json"));
        if (files.size() > 4096) throw new IllegalArgumentException("Too many resources under " + directory);
        Map<String, PoseProgram> result = new TreeMap<>();
        long total = 0;
        for (var entry : files.entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                var text = readBounded(reader, total);
                total += text.length();
                var file = entry.getKey();
                var path = file.getPath();
                String id = Identifier.fromNamespaceAndPath(file.getNamespace(), path.substring(directory.length() + 1, path.length() - 5)).toString();
                var raw = new Gson().fromJson(text, PoseProgram.class);
                if (result.put(id, PoseProgram.validatedCopy(raw)) != null) throw new IllegalArgumentException("Duplicate pose program " + id);
            } catch (java.io.IOException | RuntimeException invalid) {
                throw new IllegalArgumentException("Invalid pose-program resource " + entry.getKey(), invalid);
            }
        }
        return result;
    }

    private static Map<String, CitadelPoseProgram> readCitadelPrograms(net.minecraft.server.packs.resources.ResourceManager resources,
                                                                        String directory) {
        var files = resources.listResources(directory, id -> id.getPath().endsWith(".json"));
        if (files.size() > 4096) throw new IllegalArgumentException("Too many resources under " + directory);
        Map<String, CitadelPoseProgram> result = new TreeMap<>();
        long total = 0;
        for (var entry : files.entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                var text = readBounded(reader, total);
                total += text.length();
                var file = entry.getKey();
                var path = file.getPath();
                String id = Identifier.fromNamespaceAndPath(file.getNamespace(), path.substring(directory.length() + 1, path.length() - 5)).toString();
                var raw = new Gson().fromJson(text, CitadelPoseProgram.class);
                if (result.put(id, CitadelPoseProgram.validatedCopy(raw)) != null)
                    throw new IllegalArgumentException("Duplicate Citadel pose program " + id);
            } catch (java.io.IOException | RuntimeException invalid) {
                throw new IllegalArgumentException("Invalid Citadel pose-program resource " + entry.getKey(), invalid);
            }
        }
        return result;
    }

    private static String readBounded(java.io.Reader reader, long previous) throws java.io.IOException {
        var text = new StringBuilder();
        char[] buffer = new char[8192];
        int count;
        long total = previous;
        while ((count = reader.read(buffer)) != -1) {
            total += count;
            if (total > AnatomyCatalogPayload.MAX_BYTES) throw new IllegalArgumentException("Catalog resource limit exceeded");
            text.append(buffer, 0, count);
        }
        return text.toString();
    }

    public Snapshot reload(RegistryAccess registries) {
        return replace(LegacyAnatomyCatalogMigration.models(registries), Map.of(), Map.of(), LegacyAnatomyCatalogMigration.bindings(registries));
    }

    public synchronized Snapshot replace(Map<String, ModelGeometry> models, Collection<CollisionBinding> bindings) {
        return replace(models, Map.of(), Map.of(), bindings);
    }

    public synchronized Snapshot replace(Map<String, ModelGeometry> models, Map<String, PoseProgram> programs,
                                         Collection<CollisionBinding> bindings) {
        return replace(models, programs, Map.of(), bindings);
    }

    public synchronized Snapshot replace(Map<String, ModelGeometry> models, Map<String, PoseProgram> programs,
                                         Map<String, CitadelPoseProgram> citadelPrograms,
                                         Collection<CollisionBinding> bindings) {
        return replaceValidated(Math.incrementExact(current.snapshot().revision()), models, programs, citadelPrograms, bindings);
    }

    synchronized Snapshot replaceAtRevision(long revision, Map<String, ModelGeometry> models, Collection<CollisionBinding> bindings) {
        return replaceAtRevision(revision, models, Map.of(), Map.of(), bindings);
    }

    synchronized Snapshot replaceAtRevision(long revision, Map<String, ModelGeometry> models, Map<String, PoseProgram> programs,
                                             Collection<CollisionBinding> bindings) {
        return replaceAtRevision(revision, models, programs, Map.of(), bindings);
    }

    synchronized Snapshot replaceAtRevision(long revision, Map<String, ModelGeometry> models, Map<String, PoseProgram> programs,
                                             Map<String, CitadelPoseProgram> citadelPrograms,
                                             Collection<CollisionBinding> bindings) {
        if (revision < 0) throw new IllegalArgumentException("Negative catalog revision");
        return replaceValidated(revision, models, programs, citadelPrograms, bindings);
    }

    private Snapshot replaceValidated(long revision, Map<String, ModelGeometry> models, Map<String, PoseProgram> programs,
                                      Map<String, CitadelPoseProgram> citadelPrograms,
                                      Collection<CollisionBinding> bindings) {
        Objects.requireNonNull(models, "models");
        Objects.requireNonNull(programs, "programs");
        Objects.requireNonNull(citadelPrograms, "citadelPrograms");
        Objects.requireNonNull(bindings, "bindings");
        var canonical = new CollisionBindingCatalog(bindings);
        Map<String, PoseProgram> validatedPrograms = validatePrograms(programs);
        Map<String, CitadelPoseProgram> validatedCitadelPrograms = validateCitadelPrograms(citadelPrograms);
        var resources = resources(validatedPrograms, validatedCitadelPrograms);

        List<String> references = new ArrayList<>();
        for (var candidate : canonical.bindings()) {
            boolean bridge = compatibilityBridge(candidate);
            if (touchesCompatibilityBridge(candidate) && !bridge)
                throw new IllegalArgumentException("Incomplete legacy compatibility binding for " + selector(candidate));
            if (bridge) references.add(candidate.geometry().model().toString());
        }

        var validated = new GeometryCatalog().replace(models, references);
        validateCanonicalPoseBindings(canonical.bindings(), validated.models(), resources);
        validateRootBindings(canonical.bindings());
        Map<CollisionBinding, Binding> preparedBridge = new HashMap<>();
        for (var candidate : canonical.bindings()) {
            if (!compatibilityBridge(candidate)) continue;
            preparedBridge.put(candidate, prepareBridge(candidate, validated.models(), resources));
        }

        Map<Identifier, Binding> executable = new HashMap<>();
        for (var entity : canonical.snapshot().keySet()) {
            var selected = canonical.resolve(entity, Map.of()).orElse(null);
            if (selected == null) continue;
            var prepared = preparedBridge.get(selected);
            if (prepared != null) executable.put(entity, prepared);
        }

        var bundle = AnatomyCatalogTransfer.prepareBundle(validated.models(), validatedPrograms, validatedCitadelPrograms,
            canonical.bindings());
        var next = new Snapshot(revision, validated.models(), validatedPrograms, validatedCitadelPrograms, canonical, executable);
        current = new Accepted(next, bundle);
        return next;
    }

    private static Map<String, PoseProgram> validatePrograms(Map<String, PoseProgram> programs) {
        if (programs.size() > 4096) throw new IllegalArgumentException("Too many pose programs");
        Map<String, PoseProgram> result = new TreeMap<>();
        programs.forEach((id, raw) -> {
            final String canonicalId;
            try { canonicalId = Identifier.parse(id).toString(); }
            catch (RuntimeException invalid) { throw new IllegalArgumentException("Invalid pose-program id " + id, invalid); }
            var program = PoseProgram.validatedCopy(raw);
            if (result.putIfAbsent(canonicalId, program) != null)
                throw new IllegalArgumentException("Duplicate canonical pose-program id " + canonicalId);
        });
        return Map.copyOf(result);
    }

    private static Map<String, CitadelPoseProgram> validateCitadelPrograms(Map<String, CitadelPoseProgram> programs) {
        if (programs.size() > 4096) throw new IllegalArgumentException("Too many Citadel pose programs");
        Map<String, CitadelPoseProgram> result = new TreeMap<>();
        programs.forEach((id, raw) -> {
            final String canonicalId;
            try { canonicalId = Identifier.parse(id).toString(); }
            catch (RuntimeException invalid) { throw new IllegalArgumentException("Invalid Citadel pose-program id " + id, invalid); }
            var program = CitadelPoseProgram.validatedCopy(raw);
            if (result.putIfAbsent(canonicalId, program) != null)
                throw new IllegalArgumentException("Duplicate canonical Citadel pose-program id " + canonicalId);
        });
        return Map.copyOf(result);
    }

    private static PoseEngine.Resources resources(Map<String, PoseProgram> programs,
                                                  Map<String, CitadelPoseProgram> citadelPrograms) {
        return new PoseEngine.Resources() {
            @Override public Optional<PoseProgram> program(Identifier id) {
                return Optional.ofNullable(programs.get(id.toString()));
            }
            @Override public Optional<CitadelPoseProgram> citadelProgram(Identifier id) {
                return Optional.ofNullable(citadelPrograms.get(id.toString()));
            }
        };
    }

    private static void validateCanonicalPoseBindings(Collection<CollisionBinding> bindings, Map<String, ModelGeometry> models,
                                                      PoseEngine.Resources resources) {
        Identifier mojang = Identifier.parse("scalebrews:mojang_keyframes");
        Identifier citadel = Identifier.parse("scalebrews:citadel_program");
        for (var candidate : bindings) {
            if (compatibilityBridge(candidate)) continue;
            var model = models.get(candidate.geometry().model().toString());
            var engine = CollisionEngines.pose(candidate.pose().engine()).orElse(null);
            boolean dataBacked = candidate.pose().engine().equals(mojang) || candidate.pose().engine().equals(citadel);
            if (engine == null) {
                if (model != null || dataBacked)
                    throw new IllegalArgumentException("Missing pose engine " + candidate.pose().engine() + " for " + selector(candidate));
                continue;
            }
            if (model == null) {
                if (dataBacked) throw new IllegalArgumentException("Missing geometry for data-backed pose binding " + selector(candidate));
                continue;
            }
            if (engine.bind(model, candidate.pose().parameters(), candidate.pose().channels(), resources).isEmpty())
                throw new IllegalArgumentException("Pose engine cannot bind accepted revision for " + selector(candidate));
        }
    }

    private static void validateRootBindings(Collection<CollisionBinding> bindings) {
        for (var candidate : bindings)
            if (CollisionEngines.rootTransform(candidate.rootTransform()).isEmpty())
                throw new IllegalArgumentException("Missing root transform provider " + candidate.rootTransform() + " for " + selector(candidate));
    }

    private static Binding prepareBridge(CollisionBinding selection, Map<String, ModelGeometry> models, PoseEngine.Resources resources) {
        String modelId = selection.geometry().model().toString();
        var model = models.get(modelId);
        if (model == null) throw new IllegalArgumentException("Missing geometry " + modelId + " for " + selector(selection));
        String providerText = selection.pose().parameters().get("provider");
        if (providerText == null)
            throw new IllegalArgumentException("Missing legacy pose provider parameter for " + selector(selection));
        final Identifier providerId;
        try { providerId = Identifier.parse(providerText); }
        catch (RuntimeException invalid) {
            throw new IllegalArgumentException("Invalid legacy pose provider " + providerText + " for " + selector(selection), invalid);
        }
        var engine = CollisionEngines.pose(providerId)
            .orElseThrow(() -> new IllegalArgumentException("Missing pose engine " + providerId + " for " + selector(selection)));
        var bound = engine.bind(model, Map.of(), selection.pose().channels(), resources)
            .orElseThrow(() -> new IllegalArgumentException("Pose engine cannot bind model/version for " + selector(selection)));
        var root = CollisionEngines.rootTransform(selection.rootTransform())
            .orElseThrow(() -> new IllegalArgumentException("Missing root transform provider " + selection.rootTransform() + " for " + selector(selection)));
        validateFilter(selection, model);
        return new Binding(selection, model, bound, root, providerId);
    }

    private static String selector(CollisionBinding binding) {
        return binding.entity() + (binding.variant().isEmpty() ? "" : " variant " + binding.variant());
    }

    private static boolean compatibilityBridge(CollisionBinding binding) {
        return binding.geometry().engine().equals(LegacyCollisionData.PRECOMPUTED_GEOMETRY)
            && binding.pose().engine().equals(LegacyCollisionData.LEGACY_POSE_PROVIDER)
            && binding.rootTransform().equals(LegacyCollisionData.ENTITY_ROOT);
    }

    private static boolean touchesCompatibilityBridge(CollisionBinding binding) {
        // G3.3/G3.4 replace geometry and pose before G3.6 replaces the root provider. ENTITY_ROOT
        // therefore remains a legitimate transitional root for otherwise canonical bindings; only
        // the legacy geometry/pose pair is inseparable and must fail closed when partially selected.
        return binding.geometry().engine().equals(LegacyCollisionData.PRECOMPUTED_GEOMETRY)
            || binding.pose().engine().equals(LegacyCollisionData.LEGACY_POSE_PROVIDER);
    }

    private static void validateFilter(CollisionBinding binding, ModelGeometry model) {
        Set<String> ids = new HashSet<>();
        model.parts().forEach(part -> ids.add(part.id()));
        model.pieces().forEach(piece -> ids.add(piece.id()));
        var filter = binding.geometry().filter();
        for (String selected : java.util.stream.Stream.concat(filter.include().stream(), filter.exclude().stream()).toList())
            if (!ids.contains(selected)) throw new IllegalArgumentException("Missing selected piece/part " + selected + " for " + selector(binding));
    }
}
