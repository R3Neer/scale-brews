package io.github.r3neer.scalebrews.collision.internal;

import com.google.gson.Gson;
import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.collision.api.CollisionAdapters;
import io.github.r3neer.scalebrews.collision.api.spi.PoseChannelAdapter;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * Optional compatibility bridge compiled from embedded data after every installed mod has initialized.
 * External classes are named only in data; reflection is confined to server-start preparation and the
 * resulting HOT_TICK adapter uses pre-bound MethodHandles.
 */
public final class OptionalPoseChannelAdapters {
    private static final String RESOURCE = "assets/scalebrews/citadel_pose_channel_adapters.json";
    private static boolean installed;
    private OptionalPoseChannelAdapters() {}

    private enum Kind { BOOLEAN_METHOD, FLOAT_METHOD, INT_METHOD, BOOLEAN_FIELD, FLOAT_FIELD, INT_FIELD }
    private record Accessor(Kind kind, String member) {}
    private record Token(String owner, String field) {}
    private record Animation(String valueMethod, String tickMethod, Map<String, Token> tokens) {}
    private record Descriptor(String mod, String version, String entity, String className,
                              Map<String, Accessor> channels, Animation animation) {}
    private record Document(int schema, List<Descriptor> adapters) {}
    private record Reader(String channel, Kind kind, MethodHandle handle) {}

    public static void initialize() {
        List<Descriptor> descriptors = load();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> install(descriptors));
    }

    private static List<Descriptor> load() {
        try (var input = OptionalPoseChannelAdapters.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("Missing optional pose-adapter descriptor " + RESOURCE);
            var doc = new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), Document.class);
            if (doc == null || doc.schema != 1 || doc.adapters == null || doc.adapters.size() > 128)
                throw new IllegalStateException("Invalid optional pose-adapter descriptor");
            var result = new ArrayList<Descriptor>(doc.adapters.size());
            for (var descriptor : doc.adapters) {
                Objects.requireNonNull(descriptor, "pose adapter descriptor");
                if (descriptor.mod == null || !descriptor.mod.matches("[a-z0-9_.-]{1,64}")
                        || descriptor.version == null || descriptor.version.isBlank() || descriptor.version.length() > 64
                        || descriptor.className == null || descriptor.className.isBlank() || descriptor.className.length() > 256
                        || descriptor.channels == null || descriptor.channels.size() > 64 || descriptor.animation == null)
                    throw new IllegalStateException("Malformed optional pose-adapter entry");
                Identifier.parse(descriptor.entity);
                var seen = new java.util.HashSet<String>();
                descriptor.channels.forEach((channel, accessor) -> {
                    if (!channel.matches("[a-z0-9_.-]{1,64}") || !seen.add(channel) || accessor == null
                            || accessor.kind == null || accessor.member == null || !accessor.member.matches("[A-Za-z_$][A-Za-z0-9_$]{0,127}"))
                        throw new IllegalStateException("Malformed optional pose channel " + channel);
                });
                validateAnimation(descriptor.animation);
                result.add(descriptor);
            }
            return List.copyOf(result);
        } catch (java.io.IOException | RuntimeException failure) {
            throw new IllegalStateException("Cannot load optional pose-adapter descriptor", failure);
        }
    }

    private static void validateAnimation(Animation animation) {
        if (animation.valueMethod == null || animation.tickMethod == null || animation.tokens == null
                || animation.tokens.isEmpty() || animation.tokens.size() > 128
                || !animation.valueMethod.matches("[A-Za-z_$][A-Za-z0-9_$]{0,127}")
                || !animation.tickMethod.matches("[A-Za-z_$][A-Za-z0-9_$]{0,127}"))
            throw new IllegalStateException("Malformed optional animation descriptor");
        boolean zero = false;
        for (var entry : animation.tokens.entrySet()) {
            final int token;
            try { token = Integer.parseInt(entry.getKey()); }
            catch (NumberFormatException invalid) { throw new IllegalStateException("Invalid animation token", invalid); }
            if (token < 0 || token > 65535 || entry.getValue() == null || entry.getValue().field == null
                    || !entry.getValue().field.matches("[A-Za-z_$][A-Za-z0-9_$]{0,127}"))
                throw new IllegalStateException("Malformed optional animation token " + entry.getKey());
            if (token == 0) zero = true;
        }
        if (!zero) throw new IllegalStateException("Optional animation descriptor must map the no-animation token 0");
    }

    private static synchronized void install(List<Descriptor> descriptors) {
        if (installed) return;
        installed = true;
        FabricLoader loader = FabricLoader.getInstance();
        for (var descriptor : descriptors) {
            var container = loader.getModContainer(descriptor.mod).orElse(null);
            if (container == null) continue;
            String actualVersion = container.getMetadata().getVersion().getFriendlyString();
            if (!descriptor.version.equals(actualVersion)) {
                ScaleBrews.LOGGER.warn("Skipping optional pose adapter {}: expected {} {}, found {}",
                    descriptor.entity, descriptor.mod, descriptor.version, actualVersion);
                continue;
            }
            Identifier entity = Identifier.parse(descriptor.entity);
            if (CollisionAdapters.poseChannels(entity).isPresent()) {
                ScaleBrews.LOGGER.info("External owner already supplied pose channels for {}; Scale adapter not installed", entity);
                continue;
            }
            try {
                CollisionAdapters.registerPoseChannels(entity, compile(descriptor));
                ScaleBrews.LOGGER.info("Installed data-driven optional pose channels for {} ({})", entity, descriptor.version);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
                // An optional compatibility mismatch disables this endpoint instead of making the base mod unloadable.
                ScaleBrews.LOGGER.warn("Optional pose adapter unavailable for {}: {}", entity, failure.toString());
            }
        }
    }

    private static PoseChannelAdapter compile(Descriptor descriptor) throws ReflectiveOperationException {
        ClassLoader loader = OptionalPoseChannelAdapters.class.getClassLoader();
        Class<?> entityClass = Class.forName(descriptor.className, false, loader);
        if (!LivingEntity.class.isAssignableFrom(entityClass))
            throw new IllegalArgumentException("Optional pose adapter class is not LivingEntity: " + descriptor.className);
        var lookup = MethodHandles.publicLookup();
        var readers = new ArrayList<Reader>(descriptor.channels.size());
        for (var entry : new java.util.TreeMap<>(descriptor.channels).entrySet()) {
            Accessor accessor = entry.getValue();
            MethodHandle handle = switch (accessor.kind) {
                case BOOLEAN_METHOD, FLOAT_METHOD, INT_METHOD -> lookup.unreflect(entityClass.getMethod(accessor.member));
                case BOOLEAN_FIELD, FLOAT_FIELD, INT_FIELD -> lookup.unreflectGetter(entityClass.getField(accessor.member));
            };
            readers.add(new Reader(entry.getKey(), accessor.kind, handle));
        }
        MethodHandle animationValue = lookup.unreflect(entityClass.getMethod(descriptor.animation.valueMethod));
        MethodHandle animationTick = lookup.unreflect(entityClass.getMethod(descriptor.animation.tickMethod));
        var tokens = new IdentityHashMap<Object, Integer>();
        for (var entry : descriptor.animation.tokens.entrySet()) {
            int token = Integer.parseInt(entry.getKey());
            Token source = entry.getValue();
            Class<?> owner = source.owner == null || source.owner.isBlank()
                ? entityClass : Class.forName(source.owner, false, loader);
            var field = owner.getField(source.field);
            if (!Modifier.isStatic(field.getModifiers())) throw new IllegalArgumentException("Animation token field is not static");
            Object identity = field.get(null);
            if (identity == null || tokens.put(identity, token) != null)
                throw new IllegalArgumentException("Duplicate/null animation identity for token " + token);
        }
        return new Compiled(entityClass, readers.toArray(Reader[]::new), animationValue, animationTick, tokens);
    }

    private static final class Compiled implements PoseChannelAdapter {
        private final Class<?> entityClass;
        private final Reader[] readers;
        private final MethodHandle animationValue, animationTick;
        private final IdentityHashMap<Object, Integer> animationTokens;

        private Compiled(Class<?> entityClass, Reader[] readers, MethodHandle animationValue,
                         MethodHandle animationTick, IdentityHashMap<Object, Integer> animationTokens) {
            this.entityClass = entityClass;
            this.readers = readers;
            this.animationValue = animationValue;
            this.animationTick = animationTick;
            this.animationTokens = animationTokens;
        }

        @Override
        public void sample(LivingEntity entity, ChannelSink sink) {
            if (!entityClass.isInstance(entity)) throw new IllegalArgumentException("Pose adapter/entity class mismatch");
            try {
                for (var reader : readers) {
                    Object raw = reader.handle.invoke(entity);
                    float value = switch (reader.kind) {
                        case BOOLEAN_METHOD, BOOLEAN_FIELD -> raw instanceof Boolean flag && flag ? 1f : 0f;
                        case FLOAT_METHOD, INT_METHOD, FLOAT_FIELD, INT_FIELD -> raw instanceof Number number
                            ? number.floatValue() : Float.NaN;
                    };
                    sink.put(reader.channel, value);
                }
                Object animation = animationValue.invoke(entity);
                Integer token = animationTokens.get(animation);
                if (token == null) throw new IllegalStateException("Unknown animation identity for optional pose adapter");
                Object rawTick = animationTick.invoke(entity);
                if (!(rawTick instanceof Number number) || number.intValue() < 0)
                    throw new IllegalStateException("Invalid animation tick for optional pose adapter");
                sink.put(CitadelPoseProgram.ANIMATION_CHANNEL, token.floatValue());
                sink.put(CitadelPoseProgram.ANIMATION_TICK_CHANNEL, number.floatValue());
            } catch (RuntimeException failure) {
                throw failure;
            } catch (Throwable failure) {
                throw new IllegalStateException("Optional pose adapter invocation failed", failure);
            }
        }
    }
}
