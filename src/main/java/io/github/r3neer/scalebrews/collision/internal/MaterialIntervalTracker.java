package io.github.r3neer.scalebrews.collision.internal;

import java.util.Objects;

/**
 * Pure causal fence for material intervals. It owns no entities or geometry and
 * advances only when a caller presents a contiguous before -> after frame pair.
 */
public final class MaterialIntervalTracker {
    public enum Outcome { ADVANCED, UNCHANGED, REPLAY, GAP_OR_STALE, DISCONTINUITY }

    public record Result(Outcome outcome, GeometryProvider.MotionIntervalHandle handle) {
        public Result {
            Objects.requireNonNull(outcome);
            if (outcome == Outcome.ADVANCED && handle == null)
                throw new IllegalArgumentException("Advanced interval requires a handle");
            if (outcome != Outcome.ADVANCED && handle != null)
                throw new IllegalArgumentException("Only advanced intervals publish a handle");
        }
        public static Result of(Outcome outcome) { return new Result(outcome, null); }
    }

    private GeometryProvider.GeometryIdentity identity;
    private GeometryProvider.QueryFrame current;
    private GeometryProvider.MotionIntervalHandle last;
    private long materialSerial;

    public Result accept(GeometryProvider.QueryFrame before, GeometryProvider.QueryFrame after) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        if (!before.identity().equals(after.identity()) || !before.root().gravity().equals(after.root().gravity())) {
            reset(after);
            return Result.of(Outcome.DISCONTINUITY);
        }
        if (identity == null) {
            identity = before.identity();
            current = before;
        } else if (!identity.equals(before.identity())) {
            reset(after);
            return Result.of(Outcome.DISCONTINUITY);
        }
        if (last != null && last.before().equals(before) && last.after().equals(after))
            return Result.of(Outcome.REPLAY);
        long currentSerial = current.endpoint().frameSerial();
        long beforeSerial = before.endpoint().frameSerial();
        long afterSerial = after.endpoint().frameSerial();
        if (beforeSerial != currentSerial)
            return Result.of(Outcome.GAP_OR_STALE);
        if (afterSerial == beforeSerial)
            return Result.of(Outcome.UNCHANGED);
        if (afterSerial != beforeSerial + 1 || after.authorityTick() < before.authorityTick()
                || after.endpoint().jointSampleTick() < before.endpoint().jointSampleTick())
            return Result.of(Outcome.GAP_OR_STALE);
        long next = Math.incrementExact(materialSerial);
        var handle = new GeometryProvider.MotionIntervalHandle(identity, next, before, after);
        materialSerial = next;
        current = after;
        last = handle;
        return new Result(Outcome.ADVANCED, handle);
    }

    public void barrier(GeometryProvider.QueryFrame next) { reset(Objects.requireNonNull(next)); }

    public void clear() {
        identity = null;
        current = null;
        last = null;
        materialSerial = 0;
    }

    public long materialSerial() { return materialSerial; }
    public GeometryProvider.QueryFrame current() { return current; }

    private void reset(GeometryProvider.QueryFrame next) {
        boolean sameIdentity = identity != null && identity.equals(next.identity());
        identity = next.identity();
        current = next;
        last = null;
        if (!sameIdentity) materialSerial = 0;
    }
}
