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

/** Frame-scoped seat exchange, with a separately bounded rotation-only interpolation history. */
public final class MountRenderFrame {
    private static final Map<Integer, SeatFrame> SEATS = new HashMap<>();
    private static final java.util.Set<EntityRenderState> WORLD_STATES =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private static long serial;
    private record Rotation(Quaternionf value, long nanos) {}
    private static final Map<Integer, Rotation> ROTATIONS = new HashMap<>();
    private static Object level;
    private MountRenderFrame() {}

    public static void begin(List<EntityRenderState> states) {
        SEATS.clear();
        WORLD_STATES.clear();
        WORLD_STATES.addAll(states);
        Object currentLevel = net.minecraft.client.Minecraft.getInstance().level;
        if (level != currentLevel) { ROTATIONS.clear(); level = currentLevel; }
        var ids = new java.util.HashSet<Integer>();
        for (var state : states) if (state instanceof MountPoseState mount && mount.scalebrews$tinyMount())
            ids.add(mount.scalebrews$entityId());
        ROTATIONS.keySet().retainAll(ids);
        serial++;
        List<EntityRenderState> ordered = stableTopological(states);
        states.clear();
        states.addAll(ordered);
    }

    public static void end() { SEATS.clear(); WORLD_STATES.clear(); }
    public static boolean contains(EntityRenderState state) { return WORLD_STATES.contains(state); }
    public static void clear() { end(); ROTATIONS.clear(); level = null; }
    public static long serial() { return serial; }
    public static void capture(int entityId, SeatFrame frame) {
        if (frame.serial() != serial || WORLD_STATES.isEmpty()) return;
        long now = System.nanoTime();
        Quaternionf target = frame.riderRotationDelta().getUnnormalizedRotation(new Quaternionf()).normalize();
        Rotation previous = ROTATIONS.get(entityId);
        Quaternionf rotation = previous == null ? target : interpolateRotation(previous.value, target,
                Math.clamp((now - previous.nanos) / 1E9, 0, .25));
        ROTATIONS.put(entityId, new Rotation(rotation, now));
        SEATS.put(entityId, new SeatFrame(frame.path(), frame.saddleTransform(), frame.cameraPosition(), frame.up(),
                new Matrix4f().rotate(rotation), frame.width(), frame.depth(), frame.strapLength(), frame.seatHeight(), frame.serial()));
    }

    public static Quaternionf interpolateRotation(Quaternionf previous, Quaternionf target, double seconds) {
        return new Quaternionf(previous).slerp(target, (float)(1 - Math.exp(-seconds / .06))).normalize();
    }

    public static Matrix4f riderTransform(RiderPoseState rider, EntityRenderState state, PoseStack poses) {
        if (!contains(state)) return null;
        var firstPerson = rider.scalebrews$firstPersonOffset();
        if (firstPerson != null && rider.scalebrews$vehicleId() != -1) {
            // The camera is translation-only and collision-clipped. Its body must
            // use that exact applied translation, not the unbounded seat target.
            // FirstPerson's sitting offset is expressed in unscaled world blocks.
            float scale = state instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState living
                    ? living.scale : 1;
            var displacement = io.github.r3neer.scalebrews.client.platform.PlatformCamera.appliedOffset()
                    .add(firstPerson.scale(scale - 1));
            Matrix4f current = new Matrix4f(poses.last().pose());
            return new Matrix4f(current).invert().translate((float)displacement.x, (float)displacement.y,
                    (float)displacement.z).mul(current);
        }
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
