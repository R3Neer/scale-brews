package io.github.r3neer.scalebrews.platform;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class PlatformState {
    public LivingEntity support;
    public PlatformDefinition.Surface surface;
    public PlatformGeometry.Frame frame;
    public Vec3 contact = Vec3.ZERO;
    public Vec3 transported = Vec3.ZERO;
    public long sequence;
    public boolean carrying, visiting, landed;
    public boolean published;
    public PlatformMovePayload pendingReference;
    public void clear() { support=null; surface=null; frame=null; contact=Vec3.ZERO; landed=false; pendingReference=null; }
}
