package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** S20 PERF-004 holdout: compound conditions need one practical program-wide node budget. */
public final class S20AdversarialConditionBudgetTests {
    private static final int MAX_SCHEMA_DEPTH = 16;
    private static final int MEASURED_PATHOLOGICAL_NODES = 33_825;

    @GameTest
    public void programWideConditionBudgetMustRejectPathologicalAndBoundaryPlusOne(GameTestHelper h) {
        h.assertTrue(acceptsProgram(1),
            "S20 PERF-004 fixture sanity: a one-node condition program must remain valid");
        h.assertFalse(acceptsProgram(MEASURED_PATHOLOGICAL_NODES),
            "S20 PERF-004: the measured 33825-node ~0.296ms/eval fixture must be rejected by a practical global budget");

        int low = 1;
        int high = MEASURED_PATHOLOGICAL_NODES - 1;
        while (low < high) {
            int mid = low + (high - low + 1) / 2;
            if (acceptsProgram(mid)) low = mid;
            else high = mid - 1;
        }
        int limit = low;

        h.assertTrue(acceptsProgram(limit),
            "S20 PERF-004: the discovered condition-node boundary must accept its own limit=" + limit);
        h.assertFalse(acceptsProgram(Math.addExact(limit, 1)),
            "S20 PERF-004: limit + 1 condition nodes must fail closed during DTO validation; limit=" + limit);

        int left = Math.max(1, limit / 2);
        int right = Math.addExact(limit, 1) - left;
        h.assertTrue(left <= limit && right <= limit,
            "Adversarial aggregate fixture must keep each individual operation inside the discovered boundary");
        h.assertFalse(acceptsProgram(left, right),
            "S20 PERF-004: the condition budget must apply across the whole program, not independently per operation; "
                + left + "+" + right + "=" + (left + right) + " nodes for limit=" + limit);

        System.out.println("S20_CONDITION_BUDGET discovered_program_wide_limit=" + limit
            + " pathological_rejected=" + MEASURED_PATHOLOGICAL_NODES);
        h.succeed();
    }

    private static boolean acceptsProgram(int... nodesPerOperation) {
        try {
            programWithOperationConditionNodes(nodesPerOperation);
            return true;
        } catch (IllegalArgumentException rejected) {
            return false;
        }
    }

    private static CitadelPoseProgram programWithOperationConditionNodes(int... nodesPerOperation) {
        var operations = new ArrayList<CitadelPoseProgram.Operation>(nodesPerOperation.length);
        for (int nodes : nodesPerOperation) operations.add(operationWithConditionNodes(nodes));
        return new CitadelPoseProgram(CitadelPoseProgram.SCHEMA_VERSION,
            "scalebrews_test:condition_budget", "1", List.of(), operations);
    }

    private static CitadelPoseProgram.Operation operationWithConditionNodes(int nodes) {
        return new CitadelPoseProgram.Operation(
            CitadelPoseProgram.OperationType.ADD_ROTATION,
            "root", List.of(), exactTree(nodes, MAX_SCHEMA_DEPTH),
            CitadelPoseProgram.Scalar.constant(.01f), CitadelPoseProgram.Scalar.constant(0),
            CitadelPoseProgram.Scalar.constant(0), null, null, null,
            0, 0, 0, 0, 0, false, false);
    }

    /** Build exactly {@code nodes} condition nodes while respecting the existing <=32 children / depth<=16 schema. */
    private static CitadelPoseProgram.Condition exactTree(int nodes, int remainingDepth) {
        if (nodes <= 0) throw new IllegalArgumentException("Condition node count must be positive");
        if (nodes == 1) return new CitadelPoseProgram.Condition(
            CitadelPoseProgram.ConditionType.FLAG, "probe", 0, List.of());
        if (remainingDepth <= 0 || nodes > capacity(remainingDepth))
            throw new IllegalArgumentException("Requested condition tree cannot fit the existing schema depth: " + nodes);

        int remaining = nodes - 1;
        long childCapacity = capacity(remainingDepth - 1);
        int children = (int) Math.max(1, Math.min(32,
            (remaining + childCapacity - 1L) / childCapacity));
        if (remaining < children) children = remaining;

        var terms = new ArrayList<CitadelPoseProgram.Condition>(children);
        for (int index = 0; index < children; index++) {
            int siblingsAfter = children - index - 1;
            int childNodes = (int) Math.min(childCapacity, (long) remaining - siblingsAfter);
            terms.add(exactTree(childNodes, remainingDepth - 1));
            remaining -= childNodes;
        }
        if (remaining != 0) throw new AssertionError("Condition fixture failed to allocate exact node count");
        return new CitadelPoseProgram.Condition(CitadelPoseProgram.ConditionType.ALL, null, 0, terms);
    }

    private static long capacity(int remainingDepth) {
        long capacity = 1;
        for (int depth = 0; depth < remainingDepth; depth++) {
            if (capacity > (Integer.MAX_VALUE - 1L) / 32L) return Integer.MAX_VALUE;
            capacity = 1 + 32L * capacity;
        }
        return Math.min(capacity, Integer.MAX_VALUE);
    }
}
