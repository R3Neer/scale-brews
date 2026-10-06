package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.runtime.TransportLedger;
import io.github.r3neer.scalebrews.scale.ScaleGait;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Authority-thread-owned base Living walk law, using only completed own movement.
 * One instance belongs to one participant/actor; sampling never advances this clock.
 * Does not certify foreign walk overrides or mutate the entity's presentation clock.
 */
public final class LivingLocomotionClock {
    private final WalkAnimationState walk = new WalkAnimationState();
    private WeakReference<LivingEntity> actor;
    private WeakReference<Level> level;
    private Vec3 previous;
    private GravityFrame gravity;
    private long tick = Long.MIN_VALUE, consumedSequence, generation;
    private Frame current;

    /** Read-only, revocable authority frame; no public/data constructor. */
    public static final class Frame {
        private final LivingLocomotionClock owner;
        private final WeakReference<LivingEntity> actor;
        private final WeakReference<Level> level;
        private final long tick, generation;
        private final float position, speed;
        private final boolean available;

        private Frame(LivingLocomotionClock owner, long tick,
                      long generation, float position, float speed, boolean available) {
            this.owner = owner; this.actor = owner.actor; this.level = owner.level;
            this.tick = tick; this.generation = generation;
            this.position = position; this.speed = speed; this.available = available;
        }

        public long tick() { return tick; }
        public float position() { return position; }
        public float speed() { return speed; }
        public boolean available() { return available; }

        /** One existing global-ledger monitor acquisition per validation, never per channel. */
        public boolean validFor(LivingEntity entity, long authorityTick) {
            return available && entity != null && actor.get() == entity && !entity.isRemoved()
                && level.get() == entity.level() && owner.current == this && tick == authorityTick
                && TransportLedger.generation(entity) == generation;
        }
    }

    /** No clock mutation, no lookup of history, and no retained strong actor/Level reference. */
    public Optional<Frame> current() { return Optional.ofNullable(current); }

    public Frame advance(LivingEntity entity, long authorityTick, GravityFrame effectiveGravity) {
        if (entity == null || authorityTick < 0 || entity.level().isClientSide() || entity.isRemoved())
            throw new IllegalArgumentException("Invalid server Living clock actor/tick");
        Objects.requireNonNull(effectiveGravity, "Missing clock gravity");
        if (actor != null && actor.get() != entity)
            throw new IllegalArgumentException("Living clock cannot change actor identity");
        if (authorityTick < tick) throw new IllegalArgumentException("Living clock cannot rewind");

        long nextGeneration = TransportLedger.generation(entity);
        boolean barrier = level == null || level.get() != entity.level()
            || !effectiveGravity.equals(gravity) || nextGeneration != generation;
        if (authorityTick == tick && !barrier) return current;

        Vec3 position = entity.position();
        var transport = TransportLedger.since(entity, consumedSequence);
        boolean valid = finite(position);
        boolean reset = !valid || barrier || previous == null || authorityTick != tick + 1
            || position.distanceToSqr(previous) > 16 || !transport.contiguous();
        if (!valid || reset || !entity.isAlive() || entity.isPassenger()) {
            walk.stop();
        } else {
            Vec3 ownDelta = position.subtract(previous).subtract(transport.appliedDelta());
            double distance = effectiveGravity.tangent(ownDelta).length();
            float timeScale = ScaleGait.walkTimeScale(entity, entity.isBaby() ? 3f : 1f);
            valid = Double.isFinite(distance) && Float.isFinite(timeScale);
            if (valid) walk.update(Math.min((float) distance * 4f, 1f), .4f, timeScale);
            else walk.stop();
        }
        float walkPosition = walk.position(1f), walkSpeed = walk.speed(1f);
        valid &= Float.isFinite(walkPosition) && Float.isFinite(walkSpeed);
        if (!valid) { walk.stop(); walkPosition = walkSpeed = 0f; }

        if (actor == null) actor = new WeakReference<>(entity);
        if (level == null || level.get() != entity.level()) level = new WeakReference<>(entity.level());
        previous = valid ? position : null; gravity = effectiveGravity; tick = authorityTick;
        generation = nextGeneration; consumedSequence = transport.latestSequence();
        current = new Frame(this, tick, generation, walkPosition, walkSpeed, valid);
        return current;
    }

    private static boolean finite(Vec3 position) {
        return position != null && Double.isFinite(position.x) && Double.isFinite(position.y)
            && Double.isFinite(position.z);
    }
}
