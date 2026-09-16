package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram.Condition;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram.ConditionType;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram.Operation;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram.Scalar;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/**
 * Non-gating CPU probe for S20 PERF-003/PERF-004.
 *
 * <p>Absolute CI timings are deliberately not used as a gate. The probe records normalized
 * current-thread CPU cost after warm-up so the adversarial closeout can classify scaling shape
 * and decide an explicit structural condition-node budget from evidence rather than folklore.</p>
 */
public final class S20AdversarialCpuScalingProbeTests {
    private static final Identifier ENGINE = Identifier.parse("scalebrews:citadel_program");
    private static final Identifier PROGRAM = Identifier.parse("scalebrews_test:s20_cpu_probe");
    private static final int[] OPERATION_COUNTS = {32, 64, 128, 256, 512, 1024, 2048};
    private static final int[] CONDITION_DEPTHS = {0, 1, 2, 3};
    private static volatile long BLACKHOLE;

    @GameTest
    public void measureOperationCpuScaling(GameTestHelper h) {
        var cpu = cpuBean();
        if (cpu == null) {
            System.out.println("S20_CPU unsupported=true");
            h.succeed();
            return;
        }

        double first = -1;
        double last = -1;
        for (int operations : OPERATION_COUNTS) {
            var bound = bind(operationProgram(operations));
            var inputs = new PoseEngine.Inputs(1.75f, .65f, 21.25f, 17f, -9f, true);
            consume(bound, inputs, Math.max(1_000, 500_000 / operations));
            int iterations = Math.max(1_500, 4_000_000 / operations);
            double ns = cpuNsPerEvaluation(cpu, bound, inputs, iterations);
            if (first < 0) first = ns;
            last = ns;
            System.out.printf(java.util.Locale.ROOT,
                "S20_CPU operations=%d iterations=%d ns_per_eval=%.2f ns_per_operation=%.4f blackhole=%d%n",
                operations, iterations, ns, ns / operations, BLACKHOLE);
            h.assertTrue(Double.isFinite(ns) && ns >= 0,
                "S20 operation CPU accounting must return a finite non-negative measurement");
        }
        System.out.printf(java.util.Locale.ROOT,
            "S20_CPU_SUMMARY operations_first=%d operations_last=%d first_ns=%.2f last_ns=%.2f growth=%.3f%n",
            OPERATION_COUNTS[0], OPERATION_COUNTS[OPERATION_COUNTS.length - 1], first, last,
            first == 0 ? Double.POSITIVE_INFINITY : last / first);
        h.succeed();
    }

    @GameTest
    public void measureConditionTreeCpuScaling(GameTestHelper h) {
        var cpu = cpuBean();
        if (cpu == null) {
            System.out.println("S20_CPU_CONDITION unsupported=true");
            h.succeed();
            return;
        }

        boolean explicitGlobalBudget = false;
        try {
            CitadelPoseProgram.class.getField("MAX_CONDITION_NODES");
            explicitGlobalBudget = true;
        } catch (NoSuchFieldException ignored) {
            // This probe reports the missing policy; the final S20 gate decides whether it blocks closure.
        }
        System.out.println("S20_CONDITION_BOUND explicit_global_node_limit=" + explicitGlobalBudget);

        for (int depth : CONDITION_DEPTHS) {
            Condition condition = conditionTree(depth);
            int nodes = conditionNodes(condition);
            var bound = bind(conditionProgram(condition));
            var inputs = new PoseEngine.Inputs(1.75f, .65f, 21.25f, 17f, -9f, true, Map.of("gate", 1f));
            consume(bound, inputs, Math.max(100, 100_000 / nodes));
            int iterations = Math.max(200, 2_000_000 / nodes);
            double ns = cpuNsPerEvaluation(cpu, bound, inputs, iterations);
            System.out.printf(java.util.Locale.ROOT,
                "S20_CPU_CONDITION depth=%d nodes=%d iterations=%d ns_per_eval=%.2f ns_per_node=%.4f blackhole=%d%n",
                depth, nodes, iterations, ns, ns / nodes, BLACKHOLE);
            h.assertTrue(Double.isFinite(ns) && ns >= 0,
                "S20 condition CPU accounting must return a finite non-negative measurement");
        }
        h.succeed();
    }

    private static java.lang.management.ThreadMXBean cpuBean() {
        var bean = ManagementFactory.getThreadMXBean();
        if (!bean.isCurrentThreadCpuTimeSupported()) return null;
        if (!bean.isThreadCpuTimeEnabled()) bean.setThreadCpuTimeEnabled(true);
        return bean;
    }

    private static CitadelPoseProgram operationProgram(int count) {
        var operations = new ArrayList<Operation>(count);
        for (int i = 0; i < count; i++) {
            operations.add(rotation(Condition.always()));
        }
        return new CitadelPoseProgram(CitadelPoseProgram.SCHEMA_VERSION,
            "scalebrews_test:cpu_probe", "1", List.of(), operations);
    }

    private static CitadelPoseProgram conditionProgram(Condition condition) {
        return new CitadelPoseProgram(CitadelPoseProgram.SCHEMA_VERSION,
            "scalebrews_test:cpu_probe", "1", List.of(), List.of(rotation(condition)));
    }

    private static Operation rotation(Condition condition) {
        return new Operation(CitadelPoseProgram.OperationType.ADD_ROTATION,
            "root/bone", List.of(), condition,
            Scalar.constant(.0001f), Scalar.constant(0), Scalar.constant(0),
            null, null, null, 0, 0, 0, 0, 0, false, false);
    }

    /** depth 0 = one leaf; depth N = ALL with 32 children at depth N-1. */
    private static Condition conditionTree(int depth) {
        if (depth == 0) return new Condition(ConditionType.FLAG, "gate", 0, List.of());
        var terms = new ArrayList<Condition>(32);
        for (int i = 0; i < 32; i++) terms.add(conditionTree(depth - 1));
        return new Condition(ConditionType.ALL, null, 0, terms);
    }

    private static int conditionNodes(Condition condition) {
        int count = 1;
        for (var term : condition.terms()) count = Math.addExact(count, conditionNodes(term));
        return count;
    }

    private static PoseEngine.Bound bind(CitadelPoseProgram program) {
        var root = new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1);
        var bone = new ModelGeometry.SourcePose(0, 8, 0, 0, 0, 0, 1, 1, 1);
        var geometry = new ModelGeometry(2, program.source(), program.version(), List.of(
            new ModelGeometry.Part("root", null, ModelGeometry.values(root.matrix()), root),
            new ModelGeometry.Part("root/bone", "root", ModelGeometry.values(bone.matrix()), bone)
        ), List.of(), ModelGeometry.values(new Matrix4f()));

        PoseEngine.Resources resources = new PoseEngine.Resources() {
            @Override public Optional<io.github.r3neer.scalebrews.collision.pose.PoseProgram> program(Identifier ignored) {
                return Optional.empty();
            }
            @Override public Optional<CitadelPoseProgram> citadelProgram(Identifier requested) {
                return PROGRAM.equals(requested) ? Optional.of(program) : Optional.empty();
            }
        };
        return CollisionEngines.pose(ENGINE).orElseThrow()
            .bind(geometry, Map.of("program", PROGRAM.toString()), program.requiredChannels(), resources)
            .orElseThrow();
    }

    private static double cpuNsPerEvaluation(java.lang.management.ThreadMXBean bean, PoseEngine.Bound bound,
                                             PoseEngine.Inputs inputs, int iterations) {
        long before = bean.getCurrentThreadCpuTime();
        consume(bound, inputs, iterations);
        long after = bean.getCurrentThreadCpuTime();
        return (after - before) / (double) iterations;
    }

    private static void consume(PoseEngine.Bound bound, PoseEngine.Inputs inputs, int iterations) {
        long checksum = 0;
        for (int i = 0; i < iterations; i++) {
            var value = bound.evaluate(inputs).orElseThrow();
            checksum += value.size();
            if (!value.isEmpty()) checksum += Float.floatToRawIntBits(value.values().iterator().next().m00());
        }
        BLACKHOLE ^= checksum;
    }
}
