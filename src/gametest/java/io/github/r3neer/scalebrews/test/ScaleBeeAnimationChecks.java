package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.render.SaddleState;
import io.github.r3neer.scalebrews.client.render.RiderPoseState;
import io.github.r3neer.scalebrews.client.render.TinyMountSeatResolver;
import io.github.r3neer.scalebrews.client.render.TinyMountVisualProfile;
import io.github.r3neer.scalebrews.client.mixin.LivingRendererAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.animal.bee.AdultBeeModel;
import net.minecraft.client.renderer.entity.BeeRenderer;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.world.entity.animal.bee.Bee;

/** Renderer snapshots plus shared attachment-transform math with an actual synchronized passenger. */
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
        if ((((RiderPoseState)rider).scalebrews$vehicleId() == bee.getId()) != occupied)
            throw new AssertionError("Rider relationship metadata does not match passenger occupancy");
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
                        var outer = new org.joml.Matrix4f(frame.last().pose());
                        var seat = TinyMountSeatResolver.resolve(model.root(), outer, TinyMountVisualProfile.DEFAULT,
                                (long)time + 1, "test bee");
                        if (!Float.isFinite(seat.cameraPosition().x) || seat.width() <= 0 || seat.depth() <= 0)
                            throw new AssertionError("SeatFrame resolver produced invalid bee geometry");
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
