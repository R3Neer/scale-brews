package io.github.r3neer.scalebrews.platform;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared geometry without renderer or pathfinding dependencies. */
public final class PlatformGeometry {
    private PlatformGeometry() {}
    public record Frame(Vec3 origin, double yaw, double scale) {
        public Vec3 world(Vec3 v) {
            double c = Math.cos(yaw), s = Math.sin(yaw);
            return origin.add((v.x*c-v.z*s)*scale, v.y*scale, (v.x*s+v.z*c)*scale);
        }
        public Vec3 local(Vec3 world) {
            Vec3 v = world.subtract(origin).scale(1/scale);
            double c = Math.cos(yaw), s = Math.sin(yaw);
            return new Vec3(v.x*c+v.z*s, v.y, -v.x*s+v.z*c);
        }
    }
    public static Frame frame(LivingEntity e) {
        Vec3 origin=e.position();
        if(e instanceof net.minecraft.world.entity.player.Player && e.getPose()==net.minecraft.world.entity.Pose.CROUCHING)
            origin=origin.add(0,e.getBbHeight()-e.getDimensions(net.minecraft.world.entity.Pose.STANDING).height(),0);
        return new Frame(origin, Math.toRadians(e.yBodyRot), e.getScale());
    }
    public static double height(Frame f, PlatformDefinition.Surface p) { return f.origin.y+p.y()*f.scale; }
    /** Exact separating-axis test: horizontal AABB versus oriented rectangle. */
    public static boolean overlaps(Frame f, PlatformDefinition.Surface p, AABB b) {
        Vec3 center = f.world(new Vec3(p.x(), p.y(), p.z()));
        double dx=(b.minX+b.maxX)*.5-center.x, dz=(b.minZ+b.maxZ)*.5-center.z;
        double bx=b.getXsize()*.5, bz=b.getZsize()*.5, px=p.width()*f.scale*.5, pz=p.depth()*f.scale*.5;
        double c=Math.cos(f.yaw), s=Math.sin(f.yaw), ac=Math.abs(c), as=Math.abs(s);
        return Math.abs(dx)<bx+px*ac+pz*as-1e-7 && Math.abs(dz)<bz+px*as+pz*ac-1e-7
            && Math.abs(dx*c+dz*s)<px+bx*ac+bz*as-1e-7
            && Math.abs(-dx*s+dz*c)<pz+bx*as+bz*ac-1e-7;
    }
}
