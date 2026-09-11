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
            seed(after);
            return Result.of(Outcome.DISCONTINUITY);
        }
        if (identity == null) seed(before);
        else if (!identity.equals(before.identity())) {
            seed(after);
            return Result.of(Outcome.DISCONTINUITY);
        }
        if (last != null && last.before().equals(before) && last.after().equals(after))
            return Result.of(Outcome.REPLAY);
        long currentSerial = current == null ? -1 : current.endpoint().frameSerial();
        long beforeSerial = before.endpoint().frameSerial();
        long afterSerial = after.endpoint().frameSerial();
        if (beforeSerial != currentSerial)
            return Result.of(Outcome.GAP_OR_STALE);
        if (afterSerial == beforeSerial)
            return Result.of(Outcome.UNCHANGED);
        if (beforeSerial == Long.MAX_VALUE || afterSerial != beforeSerial + 1 || after.authorityTick() < before.authorityTick()
                || after.endpoint().jointSampleTick() < before.endpoint().jointSampleTick())
            return Result.of(Outcome.GAP_OR_STALE);
        long next = Math.incrementExact(materialSerial);
        var handle = new GeometryProvider.MotionIntervalHandle(identity, next, before, after);
        materialSerial = next;
        current = after;
        last = handle;
        return new Result(Outcome.ADVANCED, handle);
    }

    /** Seed a known-good available frame without claiming a continuous interval into it. */
    public void seed(GeometryProvider.QueryFrame next) {
        Objects.requireNonNull(next);
        boolean sameIdentity = identity != null && identity.equals(next.identity());
        identity = next.identity();
        current = next;
        last = null;
        if (!sameIdentity) materialSerial = 0;
    }

    /** Cut continuity at an unavailable/lifecycle barrier while preserving a same-binding serial fence. */
    public void cut(GeometryProvider.GeometryIdentity nextIdentity) {
        Objects.requireNonNull(nextIdentity);
        boolean sameIdentity = identity != null && identity.equals(nextIdentity);
        identity = nextIdentity;
        current = null;
        last = null;
        if (!sameIdentity) materialSerial = 0;
    }

    /** Barrier where the binding identity is not currently publishable; preserve the existing fence. */
    public void cut() { current=null;last=null; }

    public void clear() {
        identity = null;
        current = null;
        last = null;
        materialSerial = 0;
    }

    public long materialSerial() { return materialSerial; }
    public GeometryProvider.QueryFrame current() { return current; }
    public GeometryProvider.GeometryIdentity identity() { return identity; }
}
