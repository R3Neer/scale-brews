package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** S20 PERF-004 holdout: compound conditions need one explicit practical program-wide node budget. */
public final class S20AdversarialConditionBudgetTests {
    private static final int MAX_SCHEMA_DEPTH = 16;
    private static final int MEASURED_PATHOLOGICAL_NODES = 33_825;

    @GameTest
    public void publishedGlobalConditionBudgetMustAcceptLimitAndRejectLimitPlusOne(GameTestHelper h) {
        int limit = publishedConditionNodeLimit();
        h.assertTrue(limit > 0,
            "S20 PERF-004: the published global condition-node limit must be positive");
        h.assertTrue(limit < MEASURED_PATHOLOGICAL_NODES,
            "S20 PERF-004: the global condition-node limit must exclude the measured 33825-node ~0.296ms/eval fixture; limit=" + limit);

        var atLimit = programWithOperationConditionNodes(limit);
        h.assertTrue(atLimit != null,
            "S20 PERF-004: a program exactly at the published condition-node budget must remain valid");

        boolean singleTreeRejected = false;
        try {
            programWithOperationConditionNodes(Math.addExact(limit, 1));
        } catch (IllegalArgumentException expected) {
            singleTreeRejected = true;
        }
        h.assertTrue(singleTreeRejected,
            "S20 PERF-004: limit + 1 nodes in one condition tree must fail closed during DTO validation; limit=" + limit);

        int left = Math.max(1, limit / 2);
        int right = Math.addExact(limit, 1) - left;
        h.assertTrue(left <= limit && right <= limit,
            "Adversarial aggregate fixture must keep every individual operation within the published limit");
        boolean aggregateRejected = false;
        try {
            programWithOperationConditionNodes(left, right);
        } catch (IllegalArgumentException expected) {
            aggregateRejected = true;
        }
        h.assertTrue(aggregateRejected,
            "S20 PERF-004: the budget must apply across the whole program, not independently per operation; "
                + left + "+" + right + "=" + (left + right) + " nodes for limit=" + limit);
        h.succeed();
    }

    private static int publishedConditionNodeLimit() {
        var candidates = Arrays.stream(CitadelPoseProgram.class.getDeclaredFields())
            .filter(field -> field.getType() == int.class)
            .filter(field -> Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()))
            .filter(field -> {
                String name = field.getName().toUpperCase(java.util.Locale.ROOT);
                return name.contains("CONDITION") && name.contains("NODE");
            })
            .toList();
        if (candidates.size() != 1) {
            throw new AssertionError("S20 PERF-004 requires exactly one explicit static final int condition-node budget on CitadelPoseProgram; found "
                + candidates.stream().map(java.lang.reflect.Field::getName).toList());
        }
        try {
            var field = candidates.getFirst();
            if (!field.trySetAccessible())
                throw new AssertionError("S20 condition-node budget exists but cannot be read by the isolated adversarial proof: " + field.getName());
            return field.getInt(null);
        } catch (IllegalAccessException impossible) {
            throw new AssertionError("S20 condition-node budget must be readable by the isolated adversarial proof", impossible);
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
