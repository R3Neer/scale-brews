package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.catalog.CollisionBindingCatalog;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.migration.LegacyAnatomyCatalogMigration;
import io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;

/** Publish canonical bindings and prepared geometry together, only after every reference has validated. */
public final class WorldAnatomyCatalog {
    /** Executable S16 bridge for the precomputed legacy backend; authority lives in {@code selection}. */
    public record Binding(CollisionBinding selection, ModelGeometry model, PoseEngine poses, Identifier legacyPoseProvider) {
        public Binding {
            Objects.requireNonNull(selection, "selection");
            Objects.requireNonNull(model, "model");
            Objects.requireNonNull(poses, "poses");
            Objects.requireNonNull(legacyPoseProvider, "legacyPoseProvider");
        }
    }

    /** Full canonical catalog plus the subset currently executable by the S16 bridge. */
    public record Snapshot(long revision, Map<String, ModelGeometry> models, CollisionBindingCatalog catalog,
                           Map<Identifier, Binding> bindings) {
        public Snapshot {
            if (revision < 0 || catalog == null) throw new IllegalArgumentException("Invalid anatomical catalog snapshot");
            models = Map.copyOf(models);
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
        var snapshot = new Snapshot(revision, Map.of(), catalog, Map.of());
        return new Accepted(snapshot, AnatomyCatalogTransfer.prepareBundle(snapshot.models(), catalog.bindings()));
    }

    public Snapshot snapshot() { return current.snapshot(); }

    synchronized List<AnatomyCatalogPayload> preparedPackets(UUID epoch) {
        var accepted = current;
        return accepted.bundle().packets(epoch, accepted.snapshot().revision());
    }

    public Snapshot reload(net.minecraft.server.packs.resources.ResourceManager resources) {
        var models = read(resources, "scalebrews/entity_geometry", AnatomyCodecs.GEOMETRY);
        List<CollisionBinding> bindings = new ArrayList<>(CollisionBindingCatalog.load(resources).bindings());
        bindings.addAll(LegacyAnatomyCatalogMigration.load(resources));
        return replace(models, bindings);
    }

    private static <T> Map<String, T> read(net.minecraft.server.packs.resources.ResourceManager resources, String directory,
                                           com.mojang.serialization.Codec<T> codec) {
        var files = resources.listResources(directory, id -> id.getPath().endsWith(".json"));
        if (files.size() > 4096) throw new IllegalArgumentException("Too many resources under " + directory);
        Map<String, T> result = new TreeMap<>();
        long total = 0;
        for (var entry : files.entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                var text = new StringBuilder();
                char[] buffer = new char[8192];
                int count;
                while ((count = reader.read(buffer)) != -1) {
                    total += count;
                    if (total > AnatomyCatalogPayload.MAX_BYTES) throw new IllegalArgumentException("Catalog resource limit exceeded");
                    text.append(buffer, 0, count);
                }
                var file = entry.getKey();
                var path = file.getPath();
                String id = Identifier.fromNamespaceAndPath(file.getNamespace(), path.substring(directory.length() + 1, path.length() - 5)).toString();
                result.put(id, codec.parse(com.mojang.serialization.JsonOps.INSTANCE,
                    com.google.gson.JsonParser.parseString(text.toString())).getOrThrow());
            } catch (java.io.IOException | RuntimeException invalid) {
                throw new IllegalArgumentException("Invalid anatomical resource " + entry.getKey(), invalid);
            }
        }
        return result;
    }

    public Snapshot reload(RegistryAccess registries) {
        return replace(LegacyAnatomyCatalogMigration.models(registries), LegacyAnatomyCatalogMigration.bindings(registries));
    }

    public synchronized Snapshot replace(Map<String, ModelGeometry> models, Collection<CollisionBinding> bindings) {
        return replaceValidated(Math.incrementExact(current.snapshot().revision()), models, bindings);
    }

    synchronized Snapshot replaceAtRevision(long revision, Map<String, ModelGeometry> models, Collection<CollisionBinding> bindings) {
        if (revision < 0) throw new IllegalArgumentException("Negative catalog revision");
        return replaceValidated(revision, models, bindings);
    }

    private Snapshot replaceValidated(long revision, Map<String, ModelGeometry> models, Collection<CollisionBinding> bindings) {
        Objects.requireNonNull(models, "models");
        Objects.requireNonNull(bindings, "bindings");
        var canonical = new CollisionBindingCatalog(bindings);

        List<String> references = new ArrayList<>();
        for (var candidate : canonical.bindings()) {
            boolean bridge = compatibilityBridge(candidate);
            if (touchesCompatibilityBridge(candidate) && !bridge)
                throw new IllegalArgumentException("Incomplete legacy compatibility binding for " + selector(candidate));
            if (bridge) references.add(candidate.geometry().model().toString());
        }

        var validated = new GeometryCatalog().replace(models, references);
        Map<CollisionBinding, Binding> preparedBridge = new HashMap<>();
        for (var candidate : canonical.bindings()) {
            if (!compatibilityBridge(candidate)) continue;
            preparedBridge.put(candidate, prepareBridge(candidate, validated.models()));
        }

        Map<Identifier, Binding> executable = new HashMap<>();
        for (var entity : canonical.snapshot().keySet()) {
            var selected = canonical.resolve(entity, Map.of()).orElse(null);
            if (selected == null) continue;
            var prepared = preparedBridge.get(selected);
            if (prepared != null) executable.put(entity, prepared);
        }

        var bundle = AnatomyCatalogTransfer.prepareBundle(validated.models(), canonical.bindings());
        var next = new Snapshot(revision, validated.models(), canonical, executable);
        current = new Accepted(next, bundle);
        return next;
    }

    private static Binding prepareBridge(CollisionBinding selection, Map<String, ModelGeometry> models) {
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
        var provider = CollisionEngines.pose(providerId)
            .orElseThrow(() -> new IllegalArgumentException("Missing pose engine " + providerId + " for " + selector(selection)));
        if (provider.evaluate(model, new PoseEngine.Inputs(0, 0, 0, 0, 0, true), Map.of()).isEmpty())
            throw new IllegalArgumentException("Pose engine does not support model/version for " + selector(selection));
        validateFilter(selection, model);
        return new Binding(selection, model, provider, providerId);
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
        return binding.geometry().engine().equals(LegacyCollisionData.PRECOMPUTED_GEOMETRY)
            || binding.pose().engine().equals(LegacyCollisionData.LEGACY_POSE_PROVIDER)
            || binding.rootTransform().equals(LegacyCollisionData.ENTITY_ROOT);
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
