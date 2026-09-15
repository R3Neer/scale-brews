package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.render.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.joml.Matrix4f;

/** Observes real submitted frames, not a manually positioned substitute model. */
public final class MountFrameProbe {
    private static int target=-1, frames, firstPersonFrames;
    private static String path;
    private static Matrix4f previous;
    public static void start(int id) { target=id; frames=0; firstPersonFrames=0; path=null; previous=null; }
    public static void observe(LivingEntityRenderState state) {
        if (target==-1 || !MountRenderFrame.contains(state)) return;
        if (state instanceof RiderPoseState rider && rider.scalebrews$vehicleId()==target
                && rider.scalebrews$firstPersonOffset()!=null) firstPersonFrames++;
        if (((MountPoseState)state).scalebrews$entityId()!=target) return;
        SeatFrame frame=((SaddleState)state).scalebrews$seatFrame();
        if (frame==null || !frame.saddleTransform().isFinite() || !frame.cameraPosition().isFinite())
            throw new AssertionError("Mounted entity lost its finite saddle frame");
        if (path!=null && !path.equals(frame.path())) throw new AssertionError("Stationary saddle changed model anchor");
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
        System.out.println("MOUNT_FRAME_PROBE frames="+frames+" firstPerson="+firstPersonFrames+" anchor="+path);
    }
}
