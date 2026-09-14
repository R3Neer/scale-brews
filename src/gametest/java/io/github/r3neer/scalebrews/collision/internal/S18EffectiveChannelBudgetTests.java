package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.PoseProgram;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** Effective channel budget must remain constructible by the canonical PoseEngine.Inputs DTO. */
public final class S18EffectiveChannelBudgetTests {
    private static final Identifier ENGINE = Identifier.parse("scalebrews:mojang_keyframes");
    private static final Identifier PROGRAM = Identifier.parse("scalebrews_test:s18_channel_budget");

    @GameTest
    public void selectorChannelsCannotCreateAnUnrepresentableRequiredSet(GameTestHelper h) {
        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        PoseEngine.Resources resources = id -> id.equals(PROGRAM) ? Optional.of(program()) : Optional.empty();
        var parameters = Map.of(
            "program", PROGRAM.toString(),
            "clock", "channel:clock",
            "clock_scale", "1",
            "amplitude", "one");

        var required64 = channels("gate", 64);
        var input64 = values(required64);
        new PoseEngine.Inputs(0, 0, 0, 0, 0, true, input64);

        var input65 = new LinkedHashMap<>(input64);
        input65.put("clock", 1f);
        boolean inputsReject65 = false;
        try {
            new PoseEngine.Inputs(0, 0, 0, 0, 0, true, input65);
        } catch (IllegalArgumentException expected) {
            inputsReject65 = true;
        }
        h.assertTrue(inputsReject65,
            "Precondition: PoseEngine.Inputs must accept exactly 64 channels and reject a 65-channel live payload");

        h.assertTrue(engine.bind(geometry(), parameters, required64, resources).isEmpty(),
            "Binding must fail closed when a selector expands 64 distinct declared channels to an effective set of 65");
        h.succeed();
    }

    @GameTest
    public void effectiveChannelBudgetAcceptsExactly64AndDeduplicatesSelectors(GameTestHelper h) {
        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        PoseEngine.Resources resources = id -> id.equals(PROGRAM) ? Optional.of(program()) : Optional.empty();
        var parameters = Map.of(
            "program", PROGRAM.toString(),
            "clock", "channel:clock",
            "clock_scale", "1",
            "amplitude", "one");

        // 63 declared + one selector-added channel = exactly 64: this must remain bindable.
        var required63 = channels("gate", 63);
        var boundAdded = engine.bind(geometry(), parameters, required63, resources).orElse(null);
        h.assertTrue(boundAdded != null,
            "An effective required-channel set of exactly 64 must be accepted; the overflow fix must not reject the legal boundary");
        var valuesAdded = values(required63);
        valuesAdded.put("clock", 1f);
        new PoseEngine.Inputs(0, 0, 0, 0, 0, true, valuesAdded);
        h.assertTrue(boundAdded.evaluate(new PoseEngine.Inputs(0, 0, 0, 0, 0, true, valuesAdded)).isPresent(),
            "The exactly-64 binding must be satisfiable by a valid live Inputs payload");

        // 64 declared including the selector channel stays 64 after set deduplication.
        var requiredDedup = channels("gate", 63);
        requiredDedup.add("clock");
        var boundDedup = engine.bind(geometry(), parameters, requiredDedup, resources).orElse(null);
        h.assertTrue(boundDedup != null,
            "A selector channel already present in the declared set must be deduplicated, not counted twice");
        var valuesDedup = values(requiredDedup);
        new PoseEngine.Inputs(0, 0, 0, 0, 0, true, valuesDedup);
        h.assertTrue(boundDedup.evaluate(new PoseEngine.Inputs(0, 0, 0, 0, 0, true, valuesDedup)).isPresent(),
            "The deduplicated exactly-64 binding must remain live-representable and evaluable");
        h.succeed();
    }

    private static LinkedHashSet<String> channels(String prefix, int count) {
        var result = new LinkedHashSet<String>();
        for (int i = 0; i < count; i++) result.add(prefix + i);
        return result;
    }

    private static LinkedHashMap<String, Float> values(Iterable<String> names) {
        var result = new LinkedHashMap<String, Float>();
        for (String name : names) result.put(name, 1f);
        return result;
    }

    private static ModelGeometry geometry() {
        var pose = new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1);
        return new ModelGeometry(2, "proof:s18-channel-budget", "26.2",
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(pose.matrix()), pose)),
            List.of(new ModelGeometry.Piece("piece", "root", List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)),
            ModelGeometry.values(new org.joml.Matrix4f()));
    }

    private static PoseProgram program() {
        var zero = new PoseProgram.Vector(0, 0, 0);
        var end = new PoseProgram.Vector(16, 0, 0);
        return new PoseProgram(PoseProgram.SCHEMA_VERSION, "proof:s18-channel-budget", "26.2", 1f, false, List.of(
            new PoseProgram.Track("root", PoseProgram.Target.TRANSLATION, List.of(
                new PoseProgram.Keyframe(0, zero, zero, PoseProgram.Interpolation.LINEAR),
                new PoseProgram.Keyframe(1, end, end, PoseProgram.Interpolation.LINEAR)))));
    }
}
