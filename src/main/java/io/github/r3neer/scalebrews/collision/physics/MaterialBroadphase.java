package io.github.r3neer.scalebrews.collision.physics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.phys.AABB;

/**
 * Bounded spatial index over material bounds. The index owns no world/provider
 * lifecycle: callers supply material bounds and may replace one identity entry
 * when an explicit runtime mutation hook advances that support.
 */
public final class MaterialBroadphase<K> {
    public enum QueryStatus { COMPLETE, BUDGET_EXHAUSTED, INVALID_QUERY }
    public enum RejectionReason { ENTRY_BUDGET_EXHAUSTED, INVALID_BOUNDS }

    public record Entry<K>(K key, AABB bounds) {
        public Entry {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(bounds, "bounds");
        }
    }

    public record Rejected<K>(K key, RejectionReason reason, long requestedCells) {
        public Rejected {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(reason, "reason");
            if (requestedCells < 0) throw new IllegalArgumentException("Negative cell count");
        }
    }

    public record QueryResult<K>(QueryStatus status, List<K> candidates, long requestedCells,
            long cellsVisited, int candidatesVisited) {
        public QueryResult {
            Objects.requireNonNull(status, "status");
            candidates = List.copyOf(candidates);
            if (requestedCells < 0 || cellsVisited < 0 || candidatesVisited < 0)
                throw new IllegalArgumentException("Negative broadphase metric");
            if (status != QueryStatus.COMPLETE && !candidates.isEmpty())
                throw new IllegalArgumentException("Incomplete broadphase query cannot publish partial candidates");
        }

        public boolean complete() { return status == QueryStatus.COMPLETE; }
    }

    public record BuildResult<K>(MaterialBroadphase<K> index, List<Rejected<K>> rejected) {
        public BuildResult {
            Objects.requireNonNull(index, "index");
            rejected = List.copyOf(rejected);
        }
    }

    private record Cell(long x, long y, long z) {}
    private record Range(long minX, long maxX, long minY, long maxY, long minZ, long maxZ) {}

    private final double cellSize;
    private final long maxEntryCells;
    private final long maxQueryCells;
    private final int maxCandidates;
    private final Comparator<? super K> order;
    private final Map<Cell, List<K>> cells;
    private final Map<K, AABB> bounds;

    private MaterialBroadphase(double cellSize, long maxEntryCells, long maxQueryCells, int maxCandidates,
            Comparator<? super K> order, Map<Cell, List<K>> cells, Map<K, AABB> bounds) {
        this.cellSize = cellSize;
        this.maxEntryCells = maxEntryCells;
        this.maxQueryCells = maxQueryCells;
        this.maxCandidates = maxCandidates;
        this.order = order;
        this.cells = cells;
        this.bounds = bounds;
    }

    public static <K> BuildResult<K> build(Collection<Entry<K>> entries, Comparator<? super K> order,
            double cellSize, long maxEntryCells, long maxQueryCells, int maxCandidates) {
        Objects.requireNonNull(entries, "entries");
        Objects.requireNonNull(order, "order");
        if (!Double.isFinite(cellSize) || cellSize <= 0 || maxEntryCells < 1 || maxQueryCells < 1 || maxCandidates < 1)
            throw new IllegalArgumentException("Invalid broadphase budget");

        Map<Cell, List<K>> cells = new HashMap<>();
        Map<K, AABB> bounds = new IdentityHashMap<>();
        List<Rejected<K>> rejected = new ArrayList<>();

        for (var entry : entries) {
            Objects.requireNonNull(entry, "entry");
            var range = range(entry.bounds(), cellSize);
            if (range == null) {
                rejected.add(new Rejected<>(entry.key(), RejectionReason.INVALID_BOUNDS, 0));
                continue;
            }
            long count = boundedCellCount(range, maxEntryCells);
            if (count > maxEntryCells) {
                rejected.add(new Rejected<>(entry.key(), RejectionReason.ENTRY_BUDGET_EXHAUSTED, count));
                continue;
            }
            bounds.put(entry.key(), entry.bounds());
            forEachCell(range, cell -> cells.computeIfAbsent(cell, ignored -> new ArrayList<>()).add(entry.key()));
        }

        for (var bucket : cells.values()) bucket.sort(order);
        return new BuildResult<>(new MaterialBroadphase<>(cellSize, maxEntryCells, maxQueryCells, maxCandidates,
            order, cells, bounds), rejected);
    }

    /** Replace one identity entry without rebuilding or evaluating unrelated entries. */
    public synchronized Rejected<K> upsert(Entry<K> entry) {
        Objects.requireNonNull(entry, "entry");
        remove(entry.key());
        var range = range(entry.bounds(), cellSize);
        if (range == null)
            return new Rejected<>(entry.key(), RejectionReason.INVALID_BOUNDS, 0);
        long count = boundedCellCount(range, maxEntryCells);
        if (count > maxEntryCells)
            return new Rejected<>(entry.key(), RejectionReason.ENTRY_BUDGET_EXHAUSTED, count);
        bounds.put(entry.key(), entry.bounds());
        forEachCell(range, cell -> {
            var bucket = cells.computeIfAbsent(cell, ignored -> new ArrayList<>());
            bucket.add(entry.key());
            bucket.sort(order);
        });
        return null;
    }

    /** Remove one identity entry from only the cells occupied by its previous bounds. */
    public synchronized void remove(K key) {
        var previous = bounds.remove(key);
        if (previous == null) return;
        var range = range(previous, cellSize);
        if (range == null) return;
        forEachCell(range, cell -> {
            var bucket = cells.get(cell);
            if (bucket == null) return;
            bucket.removeIf(candidate -> candidate == key);
            if (bucket.isEmpty()) cells.remove(cell);
        });
    }

    public synchronized QueryResult<K> query(AABB query) {
        var range = range(query, cellSize);
        if (range == null) return new QueryResult<>(QueryStatus.INVALID_QUERY, List.of(), 0, 0, 0);
        long requested = boundedCellCount(range, maxQueryCells);
        if (requested > maxQueryCells)
            return new QueryResult<>(QueryStatus.BUDGET_EXHAUSTED, List.of(), requested, 0, 0);

        var visitedKeys = Collections.newSetFromMap(new IdentityHashMap<K, Boolean>());
        var matches = new ArrayList<K>();
        long[] visitedCells = {0};
        int[] candidatesVisited = {0};
        boolean[] exhausted = {false};
        forEachCell(range, cell -> {
            if (exhausted[0]) return;
            visitedCells[0]++;
            for (var key : cells.getOrDefault(cell, List.of())) {
                if (!visitedKeys.add(key)) continue;
                candidatesVisited[0]++;
                if (candidatesVisited[0] > maxCandidates) {
                    exhausted[0] = true;
                    return;
                }
                var bound = bounds.get(key);
                if (bound != null && bound.intersects(query)) matches.add(key);
            }
        });
        if (exhausted[0])
            return new QueryResult<>(QueryStatus.BUDGET_EXHAUSTED, List.of(), requested, visitedCells[0], candidatesVisited[0]);
        matches.sort(order);
        return new QueryResult<>(QueryStatus.COMPLETE, matches, requested, visitedCells[0], candidatesVisited[0]);
    }

    public synchronized int size() { return bounds.size(); }
    public synchronized int bucketCount() { return cells.size(); }
    public synchronized AABB bounds(K key) { return bounds.get(key); }

    private static Range range(AABB box, double cellSize) {
        if (box == null || !finite(box.minX) || !finite(box.minY) || !finite(box.minZ)
                || !finite(box.maxX) || !finite(box.maxY) || !finite(box.maxZ)
                || box.maxX < box.minX || box.maxY < box.minY || box.maxZ < box.minZ)
            return null;
        double minX = Math.floor(box.minX / cellSize), maxX = Math.floor(box.maxX / cellSize);
        double minY = Math.floor(box.minY / cellSize), maxY = Math.floor(box.maxY / cellSize);
        double minZ = Math.floor(box.minZ / cellSize), maxZ = Math.floor(box.maxZ / cellSize);
        if (!representable(minX) || !representable(maxX) || !representable(minY) || !representable(maxY)
                || !representable(minZ) || !representable(maxZ)) return null;
        return new Range((long) minX, (long) maxX, (long) minY, (long) maxY, (long) minZ, (long) maxZ);
    }

    private static boolean finite(double value) { return Double.isFinite(value); }
    private static boolean representable(double cell) {
        return Double.isFinite(cell) && cell > Long.MIN_VALUE + 1d && cell < Long.MAX_VALUE - 1d;
    }

    /** Returns at most budget+1, so pathological spans cannot overflow a long. */
    private static long boundedCellCount(Range range, long budget) {
        long x = span(range.minX(), range.maxX(), budget);
        if (x > budget) return budget + 1;
        long y = span(range.minY(), range.maxY(), budget);
        if (y > budget || x > budget / y) return budget + 1;
        long xy = x * y;
        long z = span(range.minZ(), range.maxZ(), budget);
        if (z > budget || xy > budget / z) return budget + 1;
        return xy * z;
    }

    private static long span(long min, long max, long budget) {
        if (max < min) return budget + 1;
        long diff;
        try { diff = Math.subtractExact(max, min); }
        catch (ArithmeticException overflow) { return budget + 1; }
        if (diff >= budget) return budget + 1;
        return diff + 1;
    }

    private static void forEachCell(Range range, java.util.function.Consumer<Cell> consumer) {
        for (long x = range.minX(); x <= range.maxX(); x++)
            for (long y = range.minY(); y <= range.maxY(); y++)
                for (long z = range.minZ(); z <= range.maxZ(); z++) consumer.accept(new Cell(x, y, z));
    }
}
