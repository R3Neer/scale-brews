package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.render.SaddleState;
import io.github.r3neer.scalebrews.client.render.RiderPoseState;
import io.github.r3neer.scalebrews.client.render.BeeRiderPose;
import io.github.r3neer.scalebrews.client.mixin.LivingRendererAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.animal.bee.AdultBeeModel;
import net.minecraft.client.renderer.entity.BeeRenderer;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.world.entity.animal.bee.Bee;

/** Real renderer snapshots and injected model animation, with an actual synchronized passenger. */
public final class ScaleBeeAnimationChecks {
    private ScaleBeeAnimationChecks() {}

    public static void run(Minecraft client, boolean occupied) {
        Bee bee = null;
        for (var entity : client.level.entitiesForRendering()) {
            if (entity instanceof Bee candidate) { bee = candidate; break; }
        }
        if (bee == null) throw new AssertionError("Bee fixture missing");
        var position = bee.position();
        var bounds = bee.getBoundingBox();
        var renderer = (BeeRenderer) client.getEntityRenderDispatcher().getRenderer(bee);
        var state = new BeeRenderState();
        renderer.extractRenderState(bee, state, 1);
        var rider = client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player, 1);
        if ((((RiderPoseState)rider).scalebrews$riderPose() != null) != occupied)
            throw new AssertionError("Rider animation snapshot does not match passenger occupancy");
        var definition = io.github.r3neer.scalebrews.mount.TinyMounts.definition(bee);
        if (definition == null) throw new AssertionError("Bee definition missing");
        if (!definition.saddleVisual().orElseThrow().equals(((SaddleState)state).scalebrews$saddle()))
            throw new AssertionError("Saddle snapshot missing");

        var model = new AdultBeeModel(AdultBeeModel.createBodyLayer().bakeRoot());
        var bone = model.root().getChild("bone");
        var rest = bone.getInitialPose();
        float lastWing = Float.NaN;
        boolean wingMoved = false;
        boolean bodyMoved = false;
        state.isOnGround = false;
        for (boolean angry : new boolean[]{false, true}) {
            state.isAngry = angry;
            state.rollAmount = angry ? .75F : 0;
            for (float time : new float[]{0, 4, 8, 12, 16, 24, 36}) {
                state.ageInTicks = time;
                model.setupAnim(state);
                for (float scale : new float[]{.52F, 1F, 2.92F}) {
                    for (float yaw : new float[]{0, 90, 180}) {
                        state.scale = scale;
                        state.bodyRot = yaw;
                        var frame = new PoseStack();
                        frame.scale(scale, scale, scale);
                        ((LivingRendererAccess)renderer).scalebrews$rotations(state, frame, yaw, scale);
                        frame.scale(-1, -1, 1);
                        ((LivingRendererAccess)renderer).scalebrews$scale(state, frame);
                        frame.translate(0, -1.501F, 0);
                        model.root().translateAndRotate(frame);
                        var base = new org.joml.Matrix4f(frame.last().pose());
                        bone.translateAndRotate(frame);
                        var animated = new org.joml.Matrix4f(frame.last().pose());
                        bone.loadPose(rest);
                        var reference = new PoseStack();
                        reference.mulPose(base);
                        bone.translateAndRotate(reference);
                        model.setupAnim(state);
                        var delta = BeeRiderPose.bodyDelta(renderer, state);
                        for (var point : new Vector3f[]{new Vector3f(), new Vector3f(.2F, -.3F, .1F)}) {
                            var resting = reference.last().pose().transformPosition(new Vector3f(point));
                            var expected = animated.transformPosition(new Vector3f(point));
                            var actual = delta.transformPosition(new Vector3f(resting));
                            if (actual.distance(expected) > .00001F)
                                throw new AssertionError("Rider did not follow the saddle body frame");
                            var offset = new net.minecraft.world.phys.Vec3(.2, .7, -.1);
                            var local = resting.sub(.2F, .7F, -.1F);
                            BeeRiderPose.atPassenger(delta, offset).transformPosition(local).add(.2F, .7F, -.1F);
                            if (local.distance(expected) > .00001F)
                                throw new AssertionError("Passenger-relative pivot is incorrect");
                        }
                    }
                }
                bodyMoved |= Math.abs(bone.y - rest.y()) > .01;
                float wing = bone.getChild("right_wing").zRot;
                wingMoved |= !Float.isNaN(lastWing) && Math.abs(wing - lastWing) > .01;
                lastWing = wing;
                if (Math.abs(wing + bone.getChild("left_wing").zRot) > .00001)
                    throw new AssertionError("Wing symmetry changed");
            }
        }
        if (!wingMoved || !bodyMoved)
            throw new AssertionError("Wing animation or bobbing was lost");
        state.isOnGround = true;
        state.rollAmount = 0;
        state.hasStinger = false;
        model.setupAnim(state);
        if (bone.getChild("body").getChild("stinger").visible)
            throw new AssertionError("Stinger state was overwritten");
        if (!bee.position().equals(position) || !bee.getBoundingBox().equals(bounds))
            throw new AssertionError("Model animation changed entity physics");
    }
}
