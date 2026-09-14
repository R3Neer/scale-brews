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
        var required = new LinkedHashSet<String>();
        var sixtyFour = new LinkedHashMap<String, Float>();
        for (int i = 0; i < 64; i++) {
            String name = "gate" + i;
            required.add(name);
            sixtyFour.put(name, 1f);
        }

        // Precondition: exactly 64 channels are representable, while the selector-added 65th is not.
        new PoseEngine.Inputs(0, 0, 0, 0, 0, true, sixtyFour);
        var sixtyFive = new LinkedHashMap<>(sixtyFour);
        sixtyFive.put("clock", 1f);
        boolean inputsReject65 = false;
        try {
            new PoseEngine.Inputs(0, 0, 0, 0, 0, true, sixtyFive);
        } catch (IllegalArgumentException expected) {
            inputsReject65 = true;
        }
        h.assertTrue(inputsReject65,
            "Precondition: PoseEngine.Inputs must reject a 65-channel live payload");

        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        var program = program();
        PoseEngine.Resources resources = id -> id.equals(PROGRAM) ? Optional.of(program) : Optional.empty();
        var parameters = Map.of(
            "program", PROGRAM.toString(),
            "clock", "channel:clock",
            "clock_scale", "1",
            "amplitude", "one");

        h.assertTrue(engine.bind(geometry(), parameters, required, resources).isEmpty(),
            "Binding must fail closed when clock/amplitude selectors expand the effective required-channel set beyond the 64-channel Inputs budget; accepting such a Bound creates an endpoint no valid live input can ever satisfy");
        h.succeed();
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
