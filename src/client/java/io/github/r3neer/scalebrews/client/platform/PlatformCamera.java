package io.github.r3neer.scalebrews.client.platform;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.concurrent.TimeUnit;

public final class PlatformCamera {
    private static Entity previous;
    private static Object world;
    private static long nanos;
    private static Vec3 smoothed=Vec3.ZERO, lastPhysical=Vec3.ZERO;
    private PlatformCamera() {}
    public static void reset() { previous=null;world=null;nanos=0;smoothed=Vec3.ZERO;lastPhysical=Vec3.ZERO; }
    public static Vec3 offset(Camera camera,float partial) {
        Entity e=camera.entity();
        if(e==null || camera.isDetached() || camera.isPanoramicMode()) {
            previous=null; smoothed=Vec3.ZERO; return Vec3.ZERO;
        }
        long now=System.nanoTime();
        if(previous!=e || world!=e.level() || e.position().distanceToSqr(lastPhysical)>16) smoothed=Vec3.ZERO;
        double dt=nanos==0?0.05:Math.clamp((now-nanos)/1e9,0,.1);
        previous=e; world=e.level(); nanos=now; lastPhysical=e.position();
        Vec3 desired=PlatformVisuals.offset(e,partial);
        double max=Math.min(.25,.5*e.getBbHeight());
        if(desired.length()>max) desired=desired.normalize().scale(max);
        smoothed=smoothed.lerp(desired,1-Math.exp(-dt/.1));
        Vec3 from=camera.position();
        var plane=camera.getNearPlane(net.minecraft.client.Minecraft.getInstance().options.fov().get());
        double fraction=1;
        for(var point:java.util.List.of(Vec3.ZERO,plane.getTopLeft(),plane.getTopRight(),plane.getBottomLeft(),plane.getBottomRight())) {
            Vec3 start=from.add(point);
            var hit=e.level().clip(new ClipContext(start,start.add(smoothed),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,e));
            if(hit.getType()!=HitResult.Type.MISS && smoothed.length()>1e-8)
                fraction=Math.min(fraction,Math.max(0,(hit.getLocation().distanceTo(start)-.02)/smoothed.length()));
        }
        return smoothed.scale(fraction);
    }
}
