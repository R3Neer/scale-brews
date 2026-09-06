package io.github.r3neer.scalebrews.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.mixin.AgeableModelAccess;
import io.github.r3neer.scalebrews.client.mixin.LivingRendererAccess;
import io.github.r3neer.scalebrews.mount.TinyMounts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.BeeRenderer;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** Samples the same body pose used by the bee and its saddle; no duplicated bobbing formula. */
public final class BeeRiderPose {
    private BeeRiderPose() {}

    public static void extract(LivingEntity entity, LivingEntityRenderState rider, float partialTick) {
        var output = (RiderPoseState) rider;
        output.scalebrews$riderPose(null);
        if (!(entity instanceof Player) || !(entity.getVehicle() instanceof Bee bee)
                || bee.isBaby() || !bee.isAlive() || TinyMounts.definition(bee) == null) return;
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(bee);
        if (!(renderer instanceof BeeRenderer beeRenderer)) return;
        var mount = new BeeRenderState();
        beeRenderer.extractRenderState(bee, mount, partialTick);
        var originOffset = beeRenderer.getRenderOffset(mount);
        var riderOffset = rider.passengerOffset == null ? Vec3.ZERO : rider.passengerOffset;
        // Subtract doubles before converting to floats, including at far-away world coordinates.
        Vec3 relative = new Vec3(rider.x - mount.x, rider.y - mount.y, rider.z - mount.z)
                .add(riderOffset).subtract(originOffset);
        output.scalebrews$riderPose(atPassenger(bodyDelta(beeRenderer, mount), relative));
    }

    @SuppressWarnings("unchecked")
    public static Matrix4f bodyDelta(BeeRenderer renderer, BeeRenderState state) {
        // getModel() may still refer to the last submitted baby: explicitly select the adult model.
        var model = (EntityModel<BeeRenderState>) ((AgeableModelAccess)renderer).scalebrews$adultModel();
        model.setupAnim(state);
        var root = model.root();
        var bone = root.getChild("bone");
        var frame = new PoseStack();
        frame.scale(state.scale, state.scale, state.scale);
        var access = (LivingRendererAccess) renderer;
        access.scalebrews$rotations(state, frame, state.bodyRot, state.scale);
        frame.scale(-1, -1, 1);
        access.scalebrews$scale(state, frame);
        frame.translate(0, -1.501F, 0);
        var reference = part(part(new Matrix4f(frame.last().pose()), root.getInitialPose()), bone.getInitialPose());
        root.translateAndRotate(frame);
        bone.translateAndRotate(frame);
        // Move every point from the resting body frame to the animated body frame.
        return new Matrix4f(frame.last().pose()).mul(reference.invert());
    }

    public static Matrix4f atPassenger(Matrix4fc delta, Vec3 relative) {
        float x = (float)relative.x, y = (float)relative.y, z = (float)relative.z;
        return new Matrix4f().translation(-x, -y, -z).mul(delta).translate(x, y, z);
    }

    private static Matrix4f part(Matrix4f matrix, PartPose pose) {
        return matrix.translate(pose.x() / 16, pose.y() / 16, pose.z() / 16)
                .rotateZYX(pose.zRot(), pose.yRot(), pose.xRot()).scale(pose.xScale(), pose.yScale(), pose.zScale());
    }
}
