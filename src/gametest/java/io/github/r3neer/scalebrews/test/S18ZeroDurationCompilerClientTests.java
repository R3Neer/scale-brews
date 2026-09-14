package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.AnimationDefinitionCompiler;
import io.github.r3neer.scalebrews.collision.pose.PoseProgram;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/** Implementer regression: Mojang ships legitimate zero-length static AnimationDefinitions. */
public final class S18ZeroDurationCompilerClientTests implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var definition = AnimationDefinition.Builder.withLength(0.0F)
                .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.SCALE,
                    new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.2F, 1.2F, 1.2F), AnimationChannel.Interpolations.LINEAR)))
                .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)))
                .build();

            PoseProgram program = AnimationDefinitionCompiler.compile("minecraft:sniffer", "26.2", definition);
            check(program.durationSeconds() == 0.0F, "Zero-length Mojang static definitions must retain duration 0");
            check(!program.loop(), "Static zero-length fixture must retain non-looping semantics");
            check(program.tracks().size() == 2, "Compiler must retain every static timestamp-zero channel");
            check(program.tracks().stream().allMatch(track -> track.keyframes().size() == 1
                    && track.keyframes().getFirst().timestamp() == 0.0F),
                "Zero-length static program must retain timestamp-zero keyframes exactly");
            System.out.println("S18_ZERO_DURATION_COMPILER PASS duration=0 tracks=" + program.tracks().size());
        });
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
