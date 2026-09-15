package io.github.r3neer.scalebrews.client.render;

/** Relationship metadata extracted without consulting any previous rendered frame. */
public interface RiderPoseState {
    int scalebrews$vehicleId();
    void scalebrews$vehicleId(int id);
    net.minecraft.world.phys.Vec3 scalebrews$firstPersonOffset();
    void scalebrews$firstPersonOffset(net.minecraft.world.phys.Vec3 offset);
}
