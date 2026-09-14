package io.github.r3neer.scalebrews.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Strictly frame-scoped attachment exchange. No entity-id entry survives submitEntities. */
public final class MountRenderFrame {
    private static final Map<Integer, SeatFrame> SEATS = new HashMap<>();
    private static long serial;
    private MountRenderFrame() {}

    public static void begin(List<EntityRenderState> states) {
        SEATS.clear();
        serial++;
        List<EntityRenderState> ordered = stableTopological(states);
        states.clear();
        states.addAll(ordered);
    }

    public static void end() { SEATS.clear(); }
    public static long serial() { return serial; }
    public static void capture(int entityId, SeatFrame frame) { if (frame.serial() == serial) SEATS.put(entityId, frame); }

    public static Matrix4f riderTransform(RiderPoseState rider, EntityRenderState state, PoseStack poses) {
        SeatFrame frame = SEATS.get(rider.scalebrews$vehicleId());
        if (frame == null || frame.serial() != serial) return null;
        Matrix4f current = new Matrix4f(poses.last().pose());
        Vector3f riderOrigin = current.getTranslation(new Vector3f());
        Vector3f target = new Vector3f(frame.cameraPosition()).sub(new Vector3f(frame.up())
                .mul(state.boundingBoxHeight * .4F));
        Quaternionf rotation = frame.riderRotationDelta().getUnnormalizedRotation(new Quaternionf()).normalize();
        // PoseStack composes on the right. Build the conjugated local transform whose
        // world-space effect is: move the rider origin to the seat, then rotate about
        // that shared point. Applying a camera/world-space delta directly here makes
        // model-dependent yaw/pitch (notably EMF/FA) rotate the translation itself.
        Matrix4f worldCorrection = new Matrix4f().translate(target).rotate(rotation).translate(riderOrigin.negate());
        return new Matrix4f(current).invert().mul(worldCorrection).mul(current);
    }

    public static List<EntityRenderState> stableTopological(List<EntityRenderState> source) {
        List<EntityRenderState> result = new ArrayList<>(source.size());
        Map<Integer, EntityRenderState> mounts = new HashMap<>();
        for (EntityRenderState state : source) if (state instanceof MountPoseState mount && mount.scalebrews$tinyMount())
            mounts.put(mount.scalebrews$entityId(), state);
        for (EntityRenderState state : source) {
            if (state instanceof RiderPoseState rider) {
                EntityRenderState mount = mounts.get(rider.scalebrews$vehicleId());
                if (mount != null && !result.contains(mount)) result.add(mount);
            }
            if (!result.contains(state)) result.add(state);
        }
        return result;
    }
}
