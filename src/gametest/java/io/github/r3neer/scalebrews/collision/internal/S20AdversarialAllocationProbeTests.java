package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram.Condition;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram.Operation;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram.Scalar;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/**
 * Non-gating adversarial allocation probe for S20 PERF-001.
 *
 * The probe intentionally reports bytes/evaluation instead of inventing a budget before a baseline exists.
 * A later gate may pin an accepted budget once the real-model workload is characterized.
 */
public final class S20AdversarialAllocationProbeTests {
    private static final Identifier ENGINE = Identifier.parse("scalebrews:citadel_program");
    private static final Identifier PROGRAM = Identifier.parse("scalebrews_test:s20_alloc_probe");
    private static volatile long BLACKHOLE;

    @GameTest
    public void measureAllocationSlopeAcrossTouchedBones(GameTestHelper h) {
        var bean = ManagementFactory.getThreadMXBean();
        if (!(bean instanceof com.sun.management.ThreadMXBean allocations)
                || !allocations.isThreadAllocatedMemorySupported()) {
            System.out.println("S20_ALLOC unsupported=true");
            h.succeed();
            return;
        }
        if (!allocations.isThreadAllocatedMemoryEnabled()) allocations.setThreadAllocatedMemoryEnabled(true);

        var one = bind(1);
        var sixteen = bind(16);
        var inputs = new PoseEngine.Inputs(1.75f, .65f, 21.25f, 17f, -9f, true);

        // Warm enough for the evaluator and JOML paths to be compiled before accounting begins.
        consume(one, inputs, 12_000);
        consume(sixteen, inputs, 12_000);

        double oneBytes = bytesPerEvaluation(allocations, one, inputs, 30_000);
        double sixteenBytes = bytesPerEvaluation(allocations, sixteen, inputs, 30_000);
        double slope = (sixteenBytes - oneBytes) / 15.0;

        System.out.printf(java.util.Locale.ROOT,
            "S20_ALLOC one_bone_bytes_per_eval=%.2f sixteen_bones_bytes_per_eval=%.2f marginal_bytes_per_extra_bone=%.2f blackhole=%d%n",
            oneBytes, sixteenBytes, slope, BLACKHOLE);

        h.assertTrue(oneBytes >= 0 && sixteenBytes >= 0,
            "HotSpot allocation accounting must return non-negative S20 measurements");
        h.assertTrue(sixteenBytes >= oneBytes,
            "A 16-bone evaluator workload unexpectedly allocates less than the one-bone baseline");
        h.succeed();
    }

    private static PoseEngine.Bound bind(int touchedBones) {
        var parts = new ArrayList<ModelGeometry.Part>();
        var root = new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1);
        parts.add(new ModelGeometry.Part("root", null, ModelGeometry.values(root.matrix()), root));

        var operations = new ArrayList<Operation>();
        for (int i = 0; i < touchedBones; i++) {
            String bone = "root/bone_" + i;
            var pose = new ModelGeometry.SourcePose(i * .125f, 8 + i * .25f, 0,
                .01f * i, -.015f * i, .02f * i, 1, 1, 1);
            parts.add(new ModelGeometry.Part(bone, "root", ModelGeometry.values(pose.matrix()), pose));
            operations.add(new Operation(CitadelPoseProgram.OperationType.ADD_ROTATION,
                bone, List.of(), Condition.always(), Scalar.constant(.1f), Scalar.constant(-.05f),
                Scalar.constant(.025f), null, null, null,
                0, 0, 0, 0, 0, false, false));
        }

        var geometry = new ModelGeometry(2, "scalebrews_test:alloc_probe", "1", parts,
            List.of(), ModelGeometry.values(new Matrix4f()));
        var program = new CitadelPoseProgram(1, geometry.source(), geometry.version(), List.of(), operations);
        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        PoseEngine.Resources resources = new PoseEngine.Resources() {
            @Override public Optional<io.github.r3neer.scalebrews.collision.pose.PoseProgram> program(Identifier id) {
                return Optional.empty();
            }
            @Override public Optional<CitadelPoseProgram> citadelProgram(Identifier id) {
                return PROGRAM.equals(id) ? Optional.of(program) : Optional.empty();
            }
        };
        return engine.bind(geometry, Map.of("program", PROGRAM.toString()), Set.of(), resources).orElseThrow();
    }

    private static double bytesPerEvaluation(com.sun.management.ThreadMXBean bean, PoseEngine.Bound bound,
                                             PoseEngine.Inputs inputs, int iterations) {
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        consume(bound, inputs, iterations);
        long after = bean.getThreadAllocatedBytes(thread);
        return (after - before) / (double) iterations;
    }

    private static void consume(PoseEngine.Bound bound, PoseEngine.Inputs inputs, int iterations) {
        long checksum = 0;
        for (int i = 0; i < iterations; i++) {
            var value = bound.evaluate(inputs).orElseThrow();
            checksum += value.size();
            var matrix = value.values().iterator().next();
            checksum += Float.floatToRawIntBits(matrix.m00());
        }
        BLACKHOLE ^= checksum;
    }
}
