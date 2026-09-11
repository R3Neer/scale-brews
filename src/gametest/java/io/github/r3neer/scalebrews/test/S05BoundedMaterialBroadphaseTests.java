package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.physics.MaterialBroadphase;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** G2/S05 holdouts for local, explicitly bounded material broadphase work. */
public final class S05BoundedMaterialBroadphaseTests {
    private static final Comparator<Key> ORDER = Comparator.comparingInt(Key::id);

    @GameTest
    public void exactQueryBudgetPassesAndPlusOneExhausts(GameTestHelper h) {
        var built = build(List.of(entry(1, box(0, 0, 0, .9, .9, .9))), 64, 8, 32);
        var exact = built.index().query(box(0, 0, 0, 1.9, 1.9, 1.9));
        h.assertTrue(exact.status() == MaterialBroadphase.QueryStatus.COMPLETE,
            "A query exactly at the cell budget must remain valid");
        h.assertTrue(exact.requestedCells() == 8 && exact.cellsVisited() == 8,
            "Exact budget query must visit exactly its eight local cells");

        var over = built.index().query(box(0, 0, 0, 2.1, 1.9, 1.9));
        h.assertTrue(over.status() == MaterialBroadphase.QueryStatus.BUDGET_EXHAUSTED,
            "A query one spatial span beyond budget must fail explicitly");
        h.assertTrue(over.candidates().isEmpty() && over.cellsVisited() == 0,
            "Budget exhaustion must never publish a partial/global fallback candidate set");
        h.succeed();
    }

    @GameTest
    public void oversizedEntryIsRejectedInsteadOfBecomingGlobalOverflow(GameTestHelper h) {
        var small = entry(1, box(0, 0, 0, .9, .9, .9));
        var huge = entry(2, box(0, 0, 0, 2.1, 1.9, 1.9));
        var built = build(List.of(small, huge), 8, 8, 32);
        h.assertTrue(built.rejected().size() == 1 && built.rejected().getFirst().key() == huge.key(),
            "Entry budget overflow must reject only the pathological material entry");
        h.assertTrue(built.rejected().getFirst().reason() == MaterialBroadphase.RejectionReason.ENTRY_BUDGET_EXHAUSTED,
            "Oversized material bounds need an explicit rejection reason");
        var query = built.index().query(box(0, 0, 0, .9, .9, .9));
        h.assertTrue(query.complete() && query.candidates().equals(List.of(small.key())),
            "Rejected support must not be injected into every ordinary query");
        h.succeed();
    }

    @GameTest
    public void farWorldEntriesDoNotAmplifyLocalQueryWork(GameTestHelper h) {
        var local = entry(0, box(0, 0, 0, .9, .9, .9));
        var entries = new ArrayList<MaterialBroadphase.Entry<Key>>();
        entries.add(local);
        for (int n = 1; n <= 256; n++) {
            double x = 1000 + n * 4.0;
            entries.add(entry(n, box(x, 0, 0, x + .9, .9, .9)));
        }
        var built = build(entries, 8, 8, 32);
        var query = built.index().query(box(0, 0, 0, .9, .9, .9));
        h.assertTrue(query.complete() && query.candidates().equals(List.of(local.key())),
            "Local query must return only the local material entry");
        h.assertTrue(query.cellsVisited() == 1 && query.candidatesVisited() == 1,
            "Adding far-world supports must not increase local query work");
        h.succeed();
    }

    @GameTest
    public void identityKeysSurviveEqualsAlias(GameTestHelper h) {
        var first = entry(1, box(0, 0, 0, .9, .9, .9));
        var second = entry(2, box(0, 0, 0, .9, .9, .9));
        h.assertTrue(first.key() != second.key() && first.key().equals(second.key()),
            "Fixture must model two distinct identities that alias under equals");
        var query = build(List.of(first, second), 8, 8, 32).index().query(box(0, 0, 0, .9, .9, .9));
        h.assertTrue(query.complete() && query.candidates().size() == 2,
            "Broadphase deduplication must be identity-based, not equals/network-id based");
        h.assertTrue(query.candidates().get(0) == first.key() && query.candidates().get(1) == second.key(),
            "Identity-distinct candidates must retain deterministic comparator order");
        h.succeed();
    }

    @GameTest
    public void incrementalUpsertMovesOnlyTargetIdentityAndDoesNotDuplicate(GameTestHelper h) {
        var first = entry(1, box(0, 0, 0, .9, .9, .9));
        var alias = entry(2, box(0, 0, 0, .9, .9, .9));
        h.assertTrue(first.key() != alias.key() && first.key().equals(alias.key()),
            "Fixture must use identity-distinct keys that alias under equals");
        var index = build(List.of(first, alias), 8, 8, 32).index();

        var movedBounds = box(4, 0, 0, 4.9, .9, .9);
        var rejected = index.upsert(new MaterialBroadphase.Entry<>(first.key(), movedBounds));
        h.assertTrue(rejected == null, "Bounded incremental move must be accepted");
        var oldCell = index.query(first.bounds());
        var newCell = index.query(movedBounds);
        h.assertTrue(oldCell.complete() && oldCell.candidates().size() == 1 && oldCell.candidates().getFirst() == alias.key(),
            "Moving one identity must not remove or move an equals-alias entry");
        h.assertTrue(newCell.complete() && newCell.candidates().size() == 1 && newCell.candidates().getFirst() == first.key(),
            "Moved identity must appear only at its new material bounds");

        rejected = index.upsert(first);
        h.assertTrue(rejected == null, "Moving the same identity back must remain bounded");
        var restored = index.query(first.bounds());
        h.assertTrue(restored.complete() && restored.candidates().size() == 2
                && restored.candidates().get(0) == first.key() && restored.candidates().get(1) == alias.key(),
            "Repeated upsert must neither duplicate the key nor change deterministic candidate order");
        h.assertTrue(index.size() == 2, "Repeated upsert must preserve exact identity cardinality");
        h.succeed();
    }

    @GameTest
    public void rejectedIncrementalUpsertCannotLeaveStalePreviousCollider(GameTestHelper h) {
        var original = entry(1, box(0, 0, 0, .9, .9, .9));
        var index = build(List.of(original), 8, 8, 32).index();
        var huge = new MaterialBroadphase.Entry<>(original.key(), box(0, 0, 0, 2.1, 1.9, 1.9));

        var rejected = index.upsert(huge);
        h.assertTrue(rejected != null && rejected.key() == original.key()
                && rejected.reason() == MaterialBroadphase.RejectionReason.ENTRY_BUDGET_EXHAUSTED,
            "Oversized replacement must return an explicit rejection for the same identity");
        var staleRegion = index.query(original.bounds());
        h.assertTrue(staleRegion.complete() && staleRegion.candidates().isEmpty(),
            "Fail-closed incremental rejection must remove the previous collider instead of leaving stale geometry");
        h.assertTrue(index.size() == 0 && index.bucketCount() == 0,
            "Rejected replacement must leave no residual identity or occupied bucket");
        h.succeed();
    }

    @GameTest
    public void insertionOrderCannotChangeCandidateOrder(GameTestHelper h) {
        var first = entry(1, box(0, 0, 0, .9, .9, .9));
        var second = entry(2, box(0, 0, 0, .9, .9, .9));
        var forward = build(List.of(first, second), 8, 8, 32).index().query(box(0, 0, 0, .9, .9, .9));
        var reversed = build(List.of(second, first), 8, 8, 32).index().query(box(0, 0, 0, .9, .9, .9));
        h.assertTrue(ids(forward).equals(List.of(1, 2)) && ids(forward).equals(ids(reversed)),
            "Candidate order must be independent of build/hash insertion order");
        h.succeed();
    }

    @GameTest
    public void candidateBudgetExhaustionPublishesNoPartialSet(GameTestHelper h) {
        var entries = List.of(
            entry(1, box(0, 0, 0, .9, .9, .9)),
            entry(2, box(0, 0, 0, .9, .9, .9)),
            entry(3, box(0, 0, 0, .9, .9, .9)));
        var query = build(entries, 8, 8, 2).index().query(box(0, 0, 0, .9, .9, .9));
        h.assertTrue(query.status() == MaterialBroadphase.QueryStatus.BUDGET_EXHAUSTED,
            "Candidate saturation needs the same explicit fail-closed outcome as cell saturation");
        h.assertTrue(query.candidates().isEmpty() && query.candidatesVisited() == 3,
            "A saturated query must not leak a misleading partial candidate list");
        h.succeed();
    }

    @GameTest
    public void runtimeClearanceFailsClosedWhenBroadphaseBudgetExhausts(GameTestHelper h) {
        var body = h.makeMockServerPlayerInLevel();
        AnatomyMovement.activate(h.getLevel());
        try {
            var pathological = new AABB(-1000, -1000, -1000, 1000, 1000, 1000);
            h.assertTrue(!AnatomyMovement.spaceClear(body, pathological),
                "Clearance must fail closed when its material broadphase budget is exhausted");
        } finally {
            AnatomyMovement.deactivate(h.getLevel());
            body.discard();
        }
        h.succeed();
    }

    @GameTest
    public void runtimeMovementBlocksWhenBroadphaseBudgetExhausts(GameTestHelper h) {
        var body = h.makeMockServerPlayerInLevel();
        AnatomyMovement.activate(h.getLevel());
        try {
            var requested = new Vec3(100000, 0, 0);
            h.assertTrue(AnatomyMovement.collide(body, requested).equals(Vec3.ZERO),
                "Physical movement must not interpret broadphase exhaustion as an empty world");
        } finally {
            AnatomyMovement.deactivate(h.getLevel());
            body.discard();
        }
        h.succeed();
    }

    @GameTest
    public void oversizedRuntimeSupportQuarantinesUntilExplicitRebind(GameTestHelper h) {
        var support = h.spawn(net.minecraft.world.entity.EntityTypes.COW, 2, 20, 2);
        var body = h.makeMockServerPlayerInLevel();
        support.setNoAi(true);
        support.setNoGravity(true);
        GeometryProvider huge = entity -> java.util.Optional.of(new GeometryProvider.Snapshot(1, Map.of("body",
            ConvexBox.of(new AABB(-100, -100, -100, 100, 100, 100), new Matrix4f()).move(entity.position()))));
        GeometryProvider small = entity -> java.util.Optional.of(new GeometryProvider.Snapshot(2, Map.of("body",
            ConvexBox.of(new AABB(-.5, -.5, -.5, .5, .5, .5), new Matrix4f()).move(entity.position()))));
        AnatomyMovement.activate(h.getLevel());
        AnatomyMovement.register(support, huge);
        try {
            AnatomyMovement.spaceClear(body, body.getBoundingBox()); // materializes the index and rejects the huge entry
            h.assertTrue(!AnatomyMovement.replacesPair(body, support),
                "A support rejected by the entry budget must remain locally quarantined");
            AnatomyMovement.register(support, small);
            h.assertTrue(AnatomyMovement.replacesPair(body, support),
                "Explicit rebind with bounded material must clear the local quarantine");
        } finally {
            AnatomyMovement.deactivate(h.getLevel());
            support.discard();
            body.discard();
        }
        h.succeed();
    }

    private static MaterialBroadphase.BuildResult<Key> build(List<MaterialBroadphase.Entry<Key>> entries,
            long entryCells, long queryCells, int candidates) {
        return MaterialBroadphase.build(entries, ORDER, 1.0, entryCells, queryCells, candidates);
    }

    private static MaterialBroadphase.Entry<Key> entry(int id, AABB bounds) {
        return new MaterialBroadphase.Entry<>(new Key(id), bounds);
    }

    private static AABB box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static List<Integer> ids(MaterialBroadphase.QueryResult<Key> result) {
        return result.candidates().stream().map(Key::id).toList();
    }

    /** Deliberately aliases all keys under equals/hashCode to exercise identity semantics. */
    private static final class Key {
        private final int id;
        private Key(int id) { this.id = id; }
        int id() { return id; }
        @Override public boolean equals(Object other) { return other instanceof Key; }
        @Override public int hashCode() { return 1; }
    }
}
