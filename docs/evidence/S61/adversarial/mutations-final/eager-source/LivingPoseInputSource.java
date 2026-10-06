package io.github.r3neer.scalebrews.collision.pose;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Explicit read-only partial1 projection of the original 26.2 Living extractor.
 * Does not advance or certify the server walk clock, install a binding, or trust client data.
 */
public final class LivingPoseInputSource {
    public static final int FORMAT = 1, MAX_CHANNELS = 64;
    public static final String VERSION = "original-living-input-v1";
    private LivingPoseInputSource() {}

    public enum Source {
        AGE_IN_TICKS, WALK_POSITION, WALK_SPEED, X_ROT, Y_ROT, BODY_ROT, SCALE, AGE_SCALE,
        IS_BABY, IS_IN_WATER, IS_UPSIDE_DOWN, IS_FULLY_FROZEN, IS_AUTO_SPIN_ATTACK
    }

    public record Channel(String name, Source source) {
        public Channel {
            if (name == null || !name.matches("[a-z0-9_.-]{1,64}") || source == null)
                throw new IllegalArgumentException("Invalid Living input channel");
        }
    }

    /** Server-owned recipe data; a recipe is not itself an original-renderer proof. */
    public record Program(int format, String actor, List<Channel> channels) {
        public Program {
            if (format != FORMAT || actor == null || actor.isBlank() || actor.length() > 256
                || channels == null || channels.isEmpty() || channels.size() > MAX_CHANNELS)
                throw new IllegalArgumentException("Invalid Living input program");
            var names = new HashSet<String>();
            for (var channel : channels) {
                if (channel == null || !names.add(channel.name()))
                    throw new IllegalArgumentException("Duplicate/missing Living input channel");
            }
            channels = List.copyOf(channels);
        }
    }

    /** Cold boundary: revalidate nested records even when decoded without their constructors. */
    public static Program validatedCopy(Program raw) {
        if (raw == null || raw.channels() == null || raw.channels().isEmpty() || raw.channels().size() > MAX_CHANNELS)
            throw new IllegalArgumentException("Missing/oversized Living input program");
        var channels = new java.util.ArrayList<Channel>(raw.channels().size());
        for (var channel : raw.channels()) {
            if (channel == null) throw new IllegalArgumentException("Missing Living input channel");
            channels.add(new Channel(channel.name(), channel.source()));
        }
        return new Program(raw.format(), raw.actor(), channels);
    }

    /** PREPARATION only. The caller associates its trusted recipe with an exact common actor class. */
    public static Bound bind(Class<? extends LivingEntity> actor, Program raw) {
        var program = validatedCopy(raw);
        if (actor == null || !LivingEntity.class.isAssignableFrom(actor) || !actor.getName().equals(program.actor()))
            throw new IllegalArgumentException("Living input actor mismatch");
        String[] names = new String[program.channels().size()];
        Source[] sources = new Source[names.length];
        for (int i = 0; i < names.length; i++) {
            var channel = program.channels().get(i);
            names[i] = channel.name(); sources[i] = channel.source();
        }
        return new Bound(actor, names, sources);
    }

    public static final class Bound {
        private final Class<? extends LivingEntity> actor;
        private final String[] names;
        private final Source[] sources;
        private Bound(Class<? extends LivingEntity> actor, String[] names, Source[] sources) {
            this.actor = actor; this.names = names; this.sources = sources;
        }

        /** No retained entity/clock/renderer or partial publication on a required-input failure. */
        public Optional<Map<String, Float>> sample(LivingEntity entity) {
            if (entity == null || entity.getClass() != actor) return Optional.empty();
            try {
                var scratch = new Scratch(entity); for (Source s : Source.values()) if (!Float.isFinite(scratch.value(s))) return Optional.empty();
                var staged = new LinkedHashMap<String, Float>(names.length);
                for (int i = 0; i < names.length; i++) {
                    float value = scratch.value(sources[i]);
                    if (!Float.isFinite(value)) return Optional.empty();
                    staged.put(names[i], value);
                }
                return Optional.of(Collections.unmodifiableMap(staged));
            } catch (RuntimeException failure) {
                return Optional.empty();
            }
        }
    }

    /** Call-local lazy memo: duplicate projections and shared orientation read one source value. */
    private static final class Scratch {
        private static final int SOURCE_COUNT = Source.values().length;
        private final LivingEntity entity;
        private final float[] values = new float[SOURCE_COUNT];
        private int computed;
        private boolean walkingKnown, walking, headKnown;
        private float head;
        Scratch(LivingEntity entity) { this.entity = entity; }
        private float flag(boolean value) { return value ? 1f : 0f; }
        private boolean walking() {
            if (!walkingKnown) { walking = !entity.isPassenger() && entity.isAlive(); walkingKnown = true; }
            return walking;
        }
        private float head() {
            if (!headKnown) { head = Mth.rotLerp(1f, entity.yHeadRotO, entity.yHeadRot); headKnown = true; }
            return head;
        }
        float value(Source source) {
            int index = source.ordinal(), bit = 1 << index;
            if ((computed & bit) != 0) return values[index];
            float result = switch (source) {
                case AGE_IN_TICKS -> (float) entity.tickCount + 1f;
                case WALK_POSITION -> walking() ? entity.walkAnimation.position(1f) : 0f;
                case WALK_SPEED -> walking() ? entity.walkAnimation.speed(1f) : 0f;
                case X_ROT -> {
                    float pitch = entity.getXRot(1f);
                    yield value(Source.IS_UPSIDE_DOWN) == 1f ? pitch * -1f : pitch;
                }
                case Y_ROT -> {
                    float yaw = Mth.wrapDegrees(head() - value(Source.BODY_ROT));
                    yield value(Source.IS_UPSIDE_DOWN) == 1f ? yaw * -1f : yaw;
                }
                case BODY_ROT -> bodyRotation(entity, head());
                case SCALE -> entity.getScale();
                case AGE_SCALE -> entity.getAgeScale();
                case IS_BABY -> flag(entity.isBaby());
                case IS_IN_WATER -> flag(entity.isInWater());
                case IS_UPSIDE_DOWN -> {
                    var name = entity.getCustomName();
                    String text = name == null ? null : name.getString();
                    yield flag("Dinnerbone".equals(text) || "Grumm".equals(text));
                }
                case IS_FULLY_FROZEN -> flag(entity.isFullyFrozen());
                case IS_AUTO_SPIN_ATTACK -> flag(entity.isAutoSpinAttack());
            };
            values[index] = result; computed |= bit; return result;
        }
    }

    /** Original generic solveBodyRot order/float boundaries; no species formula or current-angle alias. */
    private static float bodyRotation(LivingEntity entity, float head) {
        if (entity.getVehicle() instanceof LivingEntity vehicle) {
            float body = Mth.rotLerp(1f, vehicle.yBodyRotO, vehicle.yBodyRot);
            float difference = Mth.clamp(Mth.wrapDegrees(head - body), -85f, 85f);
            body = head - difference;
            if (Math.abs(difference) > 50f) body += difference * .2f;
            return body;
        }
        return Mth.rotLerp(1f, entity.yBodyRotO, entity.yBodyRot);
    }
}
