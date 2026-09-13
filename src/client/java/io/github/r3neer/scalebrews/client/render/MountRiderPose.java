package io.github.r3neer.scalebrews.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Shares the mount model's final rendered attachment transform with passengers.
 * The cache deliberately captures from the real render-layer phase, after vanilla
 * setupAnim and optional model animation mods such as EMF/Fresh Animations.
 */
public final class MountRiderPose {
    private record Snapshot(double x, double y, double z, Vec3 renderOffset, Matrix4f delta) {}
    private static final Map<Integer, Snapshot> SNAPSHOTS = new HashMap<>();
    private MountRiderPose() {}

    public static void capture(int entityId, LivingEntityRenderState state, Vec3 renderOffset,
                               Matrix4fc outer, ModelPart root, ModelPart anchor) {
        SNAPSHOTS.put(entityId, new Snapshot(state.x, state.y, state.z, renderOffset,
                rigid(delta(outer, root, anchor))));
    }

    public static void extract(LivingEntity entity, LivingEntityRenderState rider) {
        var output = (RiderPoseState) rider;
        output.scalebrews$riderPose(null);
        if (!(entity.getVehicle() instanceof LivingEntity vehicle)) return;
        var snapshot = SNAPSHOTS.get(vehicle.getId());
        if (snapshot == null) return;
        var riderOffset = rider.passengerOffset == null ? Vec3.ZERO : rider.passengerOffset;
        var relative = new Vec3(rider.x - snapshot.x, rider.y - snapshot.y, rider.z - snapshot.z)
                .add(riderOffset).subtract(snapshot.renderOffset);
        output.scalebrews$riderPose(atPassenger(snapshot.delta, relative));
    }

    /** Difference from the resting attachment frame to the already-animated frame. */
    public static Matrix4f delta(Matrix4fc outer, ModelPart root, ModelPart anchor) {
        var animatedStack = new PoseStack();
        animatedStack.mulPose(outer);
        root.translateAndRotate(animatedStack);
        anchor.translateAndRotate(animatedStack);
        var animated = new Matrix4f(animatedStack.last().pose());
        var reference = part(part(new Matrix4f(outer), root.getInitialPose()), anchor.getInitialPose());
        return animated.mul(reference.invert());
    }

    public static Matrix4f atPassenger(Matrix4fc delta, Vec3 relative) {
        float x = (float) relative.x, y = (float) relative.y, z = (float) relative.z;
        return new Matrix4f().translation(-x, -y, -z).mul(delta).translate(x, y, z);
    }

    private static Matrix4f rigid(Matrix4fc source) {
        var matrix = new Matrix4f(source);
        var translation = matrix.getTranslation(new Vector3f());
        var rotation = matrix.getUnnormalizedRotation(new Quaternionf()).normalize();
        return new Matrix4f().translation(translation).rotate(rotation);
    }

    private static Matrix4f part(Matrix4f matrix, PartPose pose) {
        return matrix.translate(pose.x() / 16, pose.y() / 16, pose.z() / 16)
                .rotateZYX(pose.zRot(), pose.yRot(), pose.xRot())
                .scale(pose.xScale(), pose.yScale(), pose.zScale());
    }
}
