package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.render.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.joml.Matrix4f;

/** Observes real submitted frames, not a manually positioned substitute model. */
public final class MountFrameProbe {
    private static int target=-1, frames, firstPersonFrames;
    private static String mountKind;
    private static double maximumRiderTilt;
    private static String path;
    private static Matrix4f previous;
    public static void start(int id, String kind) {
        target=id; mountKind=kind; frames=0; firstPersonFrames=0; path=null; previous=null; maximumRiderTilt=0;
    }
    public static void observe(LivingEntityRenderState state) {
        if (target==-1 || !MountRenderFrame.contains(state)) return;
        if (state instanceof RiderPoseState rider && rider.scalebrews$vehicleId()==target
                && rider.scalebrews$firstPersonOffset()!=null) firstPersonFrames++;
        if (((MountPoseState)state).scalebrews$entityId()!=target) return;
        SeatFrame frame=((SaddleState)state).scalebrews$seatFrame();
        if (frame==null || !frame.saddleTransform().isFinite() || !frame.cameraPosition().isFinite())
            throw new AssertionError("Mounted entity lost its finite saddle frame");
        if (path!=null && !path.equals(frame.path())) throw new AssertionError("Stationary saddle changed model anchor");
        var riderRotation=frame.riderRotationDelta().getUnnormalizedRotation(new org.joml.Quaternionf()).normalize();
        maximumRiderTilt=Math.max(maximumRiderTilt, 2*Math.acos(Math.clamp(Math.abs(riderRotation.w),0,1)));
        if (previous!=null) {
            var before=previous.getUnnormalizedRotation(new org.joml.Quaternionf()).normalize();
            var after=frame.saddleTransform().getUnnormalizedRotation(new org.joml.Quaternionf()).normalize();
            double angle=2*Math.acos(Math.clamp(Math.abs(before.dot(after)),0,1));
            if (angle>1.05) throw new AssertionError("Stationary saddle jumped over 60 degrees between frames: "+angle);
        }
        previous=new Matrix4f(frame.saddleTransform()); path=frame.path(); frames++;
    }
    public static void finish(boolean expectFirstPerson) {
        target=-1;
        if(frames<3) throw new AssertionError("Too few submitted mount frames: "+frames);
        if(expectFirstPerson && firstPersonFrames<3) throw new AssertionError("FirstPerson extraction bridge was not exercised");
        String lower=path==null ? "" : path.toLowerCase(java.util.Locale.ROOT);
        if ("wolf".equals(mountKind) && (lower.contains("mane") || lower.contains("head") || lower.contains("tail") || lower.contains("leg")))
            throw new AssertionError("Wolf rider attached to accessory geometry: "+path);
        if ("chicken".equals(mountKind) && (lower.contains("head") || lower.contains("wing") || lower.contains("tail") || lower.contains("leg")))
            throw new AssertionError("Chicken rider attached to accessory geometry: "+path);
        if(maximumRiderTilt>Math.toRadians(45))
            throw new AssertionError("Stationary rider inherited an implausible model-basis tilt: "+Math.toDegrees(maximumRiderTilt)+" degrees at "+path);
        System.out.println("MOUNT_FRAME_PROBE frames="+frames+" firstPerson="+firstPersonFrames+" tilt="
                +Math.toDegrees(maximumRiderTilt)+" anchor="+path);
    }
}
