package io.github.r3neer.scalebrews.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.r3neer.scalebrews.ScaleBrews;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Client-only, reloadable visual metadata. It never participates in server gameplay or syncing. */
public record TinyMountVisualProfile(Anchor anchor, Saddle saddle) {
    public static final Identifier GENERIC_SADDLE = ScaleBrews.id("textures/entity/saddle/generic.png");
    public static final TinyMountVisualProfile DEFAULT = new TinyMountVisualProfile(Anchor.DEFAULT, Saddle.DEFAULT);
    private static final String DIRECTORY = "scalebrews/tiny_mount_visual";
    private static final Set<String> WARNED = new HashSet<>();
    private static volatile Map<Identifier, TinyMountVisualProfile> profiles = Map.of();
    private static volatile long generation;

    public static void initialize() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(ScaleBrews.id("tiny_mount_visual_profiles"),
                new SimpleReloadListener<Map<Identifier, TinyMountVisualProfile>>() {
            @Override protected Map<Identifier, TinyMountVisualProfile> prepare(PreparableReloadListener.SharedState state) {
                return load(state.resourceManager());
            }
            @Override protected void apply(Map<Identifier, TinyMountVisualProfile> loaded, PreparableReloadListener.SharedState state) {
                profiles = loaded;
                generation++;
                TinyMountSeatResolver.clearCaches();
                TinyMountCamera.clear();
            }
        });
    }

    public static TinyMountVisualProfile get(Identifier entity) {
        return profiles.getOrDefault(entity, DEFAULT);
    }

    public static TinyMountVisualProfile resolve(Identifier entity, io.github.r3neer.scalebrews.mount.TinyMountDefinition.SaddleVisual legacy) {
        TinyMountVisualProfile explicit = profiles.get(entity);
        if (explicit != null || legacy == null) return explicit == null ? DEFAULT : explicit;
        return new TinyMountVisualProfile(Anchor.DEFAULT,
                new Saddle(legacy.texture(), 1, 1, 1, 1));
    }

    public static long generation() { return generation; }

    static Map<Identifier, TinyMountVisualProfile> load(ResourceManager manager) {
        Map<Identifier, TinyMountVisualProfile> previous = profiles;
        Map<Identifier, TinyMountVisualProfile> next = new HashMap<>();
        manager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json")).forEach((resourceId, resource) -> {
            Identifier entityId;
            try {
                String prefix = DIRECTORY + "/";
                String path = resourceId.getPath();
                entityId = Identifier.fromNamespaceAndPath(resourceId.getNamespace(),
                        path.substring(prefix.length(), path.length() - ".json".length()));
            } catch (RuntimeException error) {
                warnOnce(resourceId.toString(), "Invalid Tiny Mount visual profile path " + resourceId, error);
                return;
            }
            try (Reader reader = resource.openAsReader()) {
                next.put(entityId, parse(JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (Exception error) {
                TinyMountVisualProfile lastGood = previous.get(entityId);
                if (lastGood != null) next.put(entityId, lastGood);
                warnOnce(resourceId + "@" + resource.sourcePackId(),
                        "Ignoring invalid Tiny Mount visual profile " + resourceId + " from " + resource.sourcePackId()
                                + (lastGood == null ? "" : "; keeping its last valid value"), error);
            }
        });
        return Map.copyOf(next);
    }

    public static TinyMountVisualProfile parse(JsonObject json) {
        rejectUnknown(json, Set.of("anchor", "saddle"), "profile");
        Anchor anchor = json.has("anchor") ? parseAnchor(object(json, "anchor")) : Anchor.DEFAULT;
        Saddle saddle = json.has("saddle") ? parseSaddle(object(json, "saddle")) : Saddle.DEFAULT;
        return new TinyMountVisualProfile(anchor, saddle);
    }

    private static Anchor parseAnchor(JsonObject json) {
        rejectUnknown(json, Set.of("path", "point", "offset", "rotation"), "anchor");
        String path = json.has("path") ? json.get("path").getAsString() : null;
        if (path != null) {
            path = path.trim();
            if (path.isEmpty() || path.startsWith("/") || path.endsWith("/") || path.contains("//") || path.contains(".."))
                throw new IllegalArgumentException("anchor.path must be a slash-separated ModelPart path");
        }
        Vector3f point = vector(json, "point", new Vector3f(.5F, 1F, .5F));
        if (point.x < 0 || point.x > 1 || point.y < 0 || point.y > 1 || point.z < 0 || point.z > 1)
            throw new IllegalArgumentException("anchor.point values must be within [0, 1]");
        Vector3f offset = vector(json, "offset", new Vector3f());
        Vector3f rotation = vector(json, "rotation", new Vector3f());
        if (Math.max(Math.abs(offset.x), Math.max(Math.abs(offset.y), Math.abs(offset.z))) > 128)
            throw new IllegalArgumentException("anchor.offset is limited to 128 model pixels");
        if (Math.max(Math.abs(rotation.x), Math.max(Math.abs(rotation.y), Math.abs(rotation.z))) > 3600)
            throw new IllegalArgumentException("anchor.rotation is limited to 3600 degrees");
        return new Anchor(path, point, offset, rotation);
    }

    private static Saddle parseSaddle(JsonObject json) {
        rejectUnknown(json, Set.of("texture", "width", "length", "strap_length", "seat_height"), "saddle");
        Identifier texture = json.has("texture") ? Identifier.parse(json.get("texture").getAsString()) : GENERIC_SADDLE;
        return new Saddle(texture, positive(json, "width", 1), positive(json, "length", 1),
                positive(json, "strap_length", 1), positive(json, "seat_height", 1));
    }

    private static JsonObject object(JsonObject owner, String name) {
        JsonElement element = owner.get(name);
        if (!element.isJsonObject()) throw new IllegalArgumentException(name + " must be an object");
        return element.getAsJsonObject();
    }

    private static Vector3f vector(JsonObject owner, String name, Vector3f fallback) {
        if (!owner.has(name)) return fallback;
        JsonElement element = owner.get(name);
        if (!element.isJsonArray()) throw new IllegalArgumentException(name + " must be an array of three finite numbers");
        JsonArray array = element.getAsJsonArray();
        if (array.size() != 3) throw new IllegalArgumentException(name + " must contain exactly three numbers");
        Vector3f value = new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
        if (!Float.isFinite(value.x) || !Float.isFinite(value.y) || !Float.isFinite(value.z))
            throw new IllegalArgumentException(name + " must contain finite numbers");
        return value;
    }

    private static float positive(JsonObject owner, String name, float fallback) {
        if (!owner.has(name)) return fallback;
        float value = owner.get(name).getAsFloat();
        if (!Float.isFinite(value) || value <= 0 || value > 16)
            throw new IllegalArgumentException(name + " must be finite and within (0, 16]");
        return value;
    }

    private static void rejectUnknown(JsonObject object, Set<String> accepted, String where) {
        for (String key : object.keySet()) if (!accepted.contains(key))
            throw new IllegalArgumentException("Unknown " + where + " field: " + key);
    }

    private static void warnOnce(String key, String message, Throwable error) {
        synchronized (WARNED) {
            if (WARNED.add(key)) ScaleBrews.LOGGER.warn(message + ": " + error.getMessage());
        }
    }

    public record Anchor(String path, Vector3f point, Vector3f offset, Vector3f rotation) {
        public static final Anchor DEFAULT = new Anchor(null, new Vector3f(.5F, 1F, .5F), new Vector3f(), new Vector3f());
    }

    public record Saddle(Identifier texture, float width, float length, float strapLength, float seatHeight) {
        public static final Saddle DEFAULT = new Saddle(GENERIC_SADDLE, 1, 1, 1, 1);
    }
}
