package io.github.r3neer.scalebrews.test;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Test-only structural performance probe for full-level entity enumeration owned by Scale Brews. */
public final class CollisionPerformanceProbe {
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final AtomicInteger CALLS = new AtomicInteger();
    private static final AtomicLong VISITS = new AtomicLong();
    private static volatile boolean enabled;

    private CollisionPerformanceProbe() {}

    public record Snapshot(int globalEnumerations, long entitiesVisited) {}

    public static void begin() {
        CALLS.set(0);
        VISITS.set(0);
        enabled = true;
    }

    public static Snapshot end() {
        enabled = false;
        return new Snapshot(CALLS.get(), VISITS.get());
    }

    public static boolean active() {
        return enabled;
    }

    /** Only attribute ServerLevel#getAllEntities calls whose caller belongs to collision orchestration. */
    public static boolean collisionCaller() {
        if (!enabled) return false;
        return WALKER.walk(frames -> frames.anyMatch(frame -> {
            Class<?> owner = frame.getDeclaringClass();
            String name = owner.getName();
            return name.equals("io.github.r3neer.scalebrews.platform.Platforms")
                || name.equals("io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime");
        }));
    }

    public static <T> Iterable<T> wrapFullEnumeration(Iterable<T> original) {
        Objects.requireNonNull(original, "original");
        CALLS.incrementAndGet();
        return () -> {
            Iterator<T> delegate = original.iterator();
            return new Iterator<>() {
                @Override public boolean hasNext() { return delegate.hasNext(); }
                @Override public T next() {
                    T value = delegate.next();
                    VISITS.incrementAndGet();
                    return value;
                }
                @Override public void remove() { delegate.remove(); }
                @Override public void forEachRemaining(java.util.function.Consumer<? super T> action) {
                    delegate.forEachRemaining(value -> {
                        VISITS.incrementAndGet();
                        action.accept(value);
                    });
                }
            };
        };
    }
}
