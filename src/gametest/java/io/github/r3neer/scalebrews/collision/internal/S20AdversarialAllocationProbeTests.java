package io.github.r3neer.scalebrews.collision.internal;

import com.google.gson.Gson;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram.Condition;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram.Operation;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram.Scalar;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
        var allocations = allocationBean();
        if (allocations == null) {
            System.out.println("S20_ALLOC unsupported=true");
            h.succeed();
            return;
        }

        var one = bind(1);
        var sixteen = bind(16);
        var inputs = new PoseEngine.Inputs(1.75f, .65f, 21.25f, 17f, -9f, true);

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

    @GameTest
    public void measurePinnedGazelleAndGrizzlyPrograms(GameTestHelper h) {
        var allocations = allocationBean();
        if (allocations == null) {
            System.out.println("S20_ALLOC_REAL unsupported=true");
            h.succeed();
            return;
        }

        measurePinned(h, allocations, "gazelle");
        measurePinned(h, allocations, "grizzly_bear");
        h.succeed();
    }

    private static void measurePinned(GameTestHelper h, com.sun.management.ThreadMXBean allocations, String name) {
        var program = resource(name);
        var bound = bind(program, Identifier.parse("scalebrews_test:alloc_" + name));
        var ordinary = sample(program, null);
        var clip = sample(program, bestClip(program));

        consume(bound, ordinary.inputs(), 8_000);
        consume(bound, clip.inputs(), 8_000);
        double ordinaryBytes = bytesPerEvaluation(allocations, bound, ordinary.inputs(), 20_000);
        double clipBytes = bytesPerEvaluation(allocations, bound, clip.inputs(), 20_000);

        System.out.printf(java.util.Locale.ROOT,
            "S20_ALLOC_REAL model=%s operations=%d clips=%d referenced_bones=%d ordinary_bytes_per_eval=%.2f clip_animation=%d clip_tick=%d clip_union_bones=%d clip_bytes_per_eval=%.2f blackhole=%d%n",
            name, program.operations().size(), program.clips().size(), referencedBones(program).size(),
            ordinaryBytes, clip.animation(), clip.tick(), clip.unionBones(), clipBytes, BLACKHOLE);

        h.assertTrue(ordinaryBytes >= 0 && clipBytes >= 0,
            "Pinned-program allocation accounting must remain non-negative for " + name);
    }

    private static com.sun.management.ThreadMXBean allocationBean() {
        var bean = ManagementFactory.getThreadMXBean();
        if (!(bean instanceof com.sun.management.ThreadMXBean allocations)
                || !allocations.isThreadAllocatedMemorySupported()) return null;
        if (!allocations.isThreadAllocatedMemoryEnabled()) allocations.setThreadAllocatedMemoryEnabled(true);
        return allocations;
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
        return bind(geometry, program, PROGRAM);
    }

    private static PoseEngine.Bound bind(CitadelPoseProgram program, Identifier id) {
        var pose = new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1);
        var parts = referencedBones(program).stream()
            .map(bone -> new ModelGeometry.Part(bone, null, ModelGeometry.values(pose.matrix()), pose))
            .toList();
        if (parts.isEmpty()) throw new AssertionError("Pinned S20 program references no bones: " + program.source());
        var geometry = new ModelGeometry(2, program.source(), program.version(), parts,
            List.of(), ModelGeometry.values(new Matrix4f()));
        return bind(geometry, program, id);
    }

    private static PoseEngine.Bound bind(ModelGeometry geometry, CitadelPoseProgram program, Identifier id) {
        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        PoseEngine.Resources resources = new PoseEngine.Resources() {
            @Override public Optional<io.github.r3neer.scalebrews.collision.pose.PoseProgram> program(Identifier ignored) {
                return Optional.empty();
            }
            @Override public Optional<CitadelPoseProgram> citadelProgram(Identifier requested) {
                return id.equals(requested) ? Optional.of(program) : Optional.empty();
            }
        };
        return engine.bind(geometry, Map.of("program", id.toString()), program.requiredChannels(), resources).orElseThrow();
    }

    private static CitadelPoseProgram resource(String name) {
        String path = "data/alexsmobs/scalebrews/citadel_pose_programs/" + name + ".json";
        try (var input = S20AdversarialAllocationProbeTests.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new AssertionError("Missing pinned S20 allocation resource " + path);
            var raw = new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), CitadelPoseProgram.class);
            return CitadelPoseProgram.validatedCopy(raw);
        } catch (java.io.IOException failure) {
            throw new AssertionError("Cannot read pinned S20 allocation resource " + path, failure);
        }
    }

    private static Set<String> referencedBones(CitadelPoseProgram program) {
        var bones = new LinkedHashSet<String>();
        for (var clip : program.clips())
            for (var frame : clip.keyframes())
                for (var delta : frame.deltas()) bones.add(delta.bone());
        for (var operation : program.operations()) {
            if (operation.type() == CitadelPoseProgram.OperationType.FACE_TARGET) bones.addAll(operation.bones());
            else bones.add(operation.bone());
        }
        return Set.copyOf(bones);
    }

    private record ClipSample(PoseEngine.Inputs inputs, int animation, int tick, int unionBones) {}

    private static ClipSample sample(CitadelPoseProgram program, ClipChoice choice) {
        var channels = new LinkedHashMap<String, Float>();
        for (var channel : program.requiredChannels()) channels.put(channel, 0f);
        int animation = 0, tick = 0, union = 0;
        if (choice != null) {
            animation = choice.animation();
            tick = choice.tick();
            union = choice.unionBones();
            channels.put(CitadelPoseProgram.ANIMATION_CHANNEL, (float) animation);
            channels.put(CitadelPoseProgram.ANIMATION_TICK_CHANNEL, (float) tick);
            channels.put(CitadelPoseProgram.ANIMATION_PARTIAL_CHANNEL, .5f);
        }
        return new ClipSample(new PoseEngine.Inputs(2.35f, .8f, 17.25f, 21f, -11f, true, channels),
            animation, tick, union);
    }

    private record ClipChoice(int animation, int tick, int unionBones) {}

    private static ClipChoice bestClip(CitadelPoseProgram program) {
        ClipChoice best = null;
        for (var clip : program.clips()) {
            Set<String> previous = Set.of();
            int start = 0;
            for (var frame : clip.keyframes()) {
                var current = new LinkedHashSet<String>();
                for (var delta : frame.deltas()) current.add(delta.bone());
                var union = new LinkedHashSet<>(previous);
                union.addAll(current);
                if (best == null || union.size() > best.unionBones())
                    best = new ClipChoice(clip.animation(), start, union.size());
                if (!frame.stationary()) previous = Set.copyOf(current);
                start += frame.durationTicks();
            }
        }
        return best;
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
            if (!value.isEmpty()) {
                var matrix = value.values().iterator().next();
                checksum += Float.floatToRawIntBits(matrix.m00());
            }
        }
        BLACKHOLE ^= checksum;
    }
}
