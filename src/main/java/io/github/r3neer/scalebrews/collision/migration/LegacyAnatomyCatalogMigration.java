package io.github.r3neer.scalebrews.collision.migration;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Explicit boundary from released Living Platforms data into canonical anatomy.
 * One-sided legacy surfaces are intentionally discarded from the anatomy result.
 */
public final class LegacyAnatomyCatalogMigration {
    private LegacyAnatomyCatalogMigration() {}
    private static final String DIRECTORY = "scalebrews/entity_platform";
    private static final long MAX_BYTES = 16L * 1024 * 1024;

    public static List<CollisionBinding> bindings(Collection<PlatformDefinition> profiles) {
        if (profiles == null || profiles.size() > 4096) throw new IllegalArgumentException("Invalid legacy anatomical profile set");
        var ordered = profiles.stream().sorted(Comparator.comparing(profile -> profile.entity().toString())).toList();
        List<CollisionBinding> result = new ArrayList<>();
        for (var profile : ordered) {
            var decoded = LegacyCollisionData.decode(profile);
            decoded.binding().ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    public static List<CollisionBinding> bindings(Map<String, PlatformDefinition> profiles) {
        if (profiles == null) throw new IllegalArgumentException("Missing legacy anatomical profiles");
        return bindings(profiles.values());
    }

    public static List<CollisionBinding> load(ResourceManager resources) {
        var files = resources.listResources(DIRECTORY, id -> id.getPath().endsWith(".json"));
        if (files.size() > 4096) throw new IllegalArgumentException("Too many legacy anatomical profiles");
        long[] total = {0};
        List<PlatformDefinition> profiles = new ArrayList<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(Object::toString))).forEach(entry -> {
            try (var input = entry.getValue().open()) {
                byte[] bytes = readBounded(input, MAX_BYTES - total[0]);
                total[0] += bytes.length;
                var json = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
                profiles.add(PlatformDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
            } catch (IOException | RuntimeException invalid) {
                throw new IllegalArgumentException("Invalid legacy anatomical resource " + entry.getKey(), invalid);
            }
        });
        return bindings(profiles);
    }

    public static List<CollisionBinding> bindings(RegistryAccess registries) {
        List<PlatformDefinition> profiles = new ArrayList<>();
        registries.lookup(Platforms.DEFINITIONS).ifPresent(registry -> registry.listElements().forEach(holder -> profiles.add(holder.value())));
        return bindings(profiles);
    }

    /** Legacy registry seam for precomputed geometry only; canonical binding authority stays outside Platforms. */
    public static Map<String, ModelGeometry> models(RegistryAccess registries) {
        Map<String, ModelGeometry> models = new TreeMap<>();
        registries.lookup(Platforms.GEOMETRIES).ifPresent(registry -> registry.listElements().forEach(holder ->
            models.put(holder.key().identifier().toString(), holder.value())));
        return models;
    }

    private static byte[] readBounded(InputStream input, long remaining) throws IOException {
        if (remaining < 0) throw new IllegalArgumentException("Legacy anatomical resources exceed size limit");
        var output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            total += count;
            if (total > remaining) throw new IllegalArgumentException("Legacy anatomical resources exceed size limit");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }
}
