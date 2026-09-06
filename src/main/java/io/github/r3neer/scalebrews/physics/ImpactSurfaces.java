package io.github.r3neer.scalebrews.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded shortest paths over exposed collision tops; never generates missing chunks. */
public final class ImpactSurfaces {
    private static final double EPS = 1e-5;
    private record Surface(int x, int z, double y) {}
    private record Visit(Surface surface, double distance) {}
    private final ServerLevel level;
    private final Vec3 origin;
    private final double radius;
    private final Map<Long, List<Surface>> columns = new HashMap<>();
    private final Map<Surface, Double> reached = new HashMap<>();

    public ImpactSurfaces(ServerLevel level, Vec3 origin, double radius) {
        this.level=level;this.origin=origin;this.radius=radius;
        var start=column((int)Math.floor(origin.x),(int)Math.floor(origin.z)).stream()
            .filter(s->Math.abs(s.y-origin.y)<.2)
            .min(Comparator.comparingDouble(s->Math.abs(s.y-origin.y))).orElse(null);
        if(start==null)return;
        if(!clear(origin,new Vec3(start.x+.5,start.y,start.z+.5),Math.max(origin.y,start.y)+.05))return;
        var pending=new PriorityQueue<Visit>(Comparator.comparingDouble(Visit::distance));
        double initial=origin.distanceTo(new Vec3(start.x+.5,start.y,start.z+.5));
        reached.put(start,initial);pending.add(new Visit(start,initial));
        while(!pending.isEmpty()) {
            var visit=pending.remove();var from=visit.surface;
            if(visit.distance>reached.get(from)+EPS)continue;
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
                if(dx==0 && dz==0)continue;
                for(var to:column(from.x+dx,from.z+dz)) {
                    if(!step(from,to))continue;
                    // A diagonal must not cut across a missing surface or a tall corner.
                    if(dx!=0 && dz!=0 && (!bridge(from,to,from.x+dx,from.z)
                        || !bridge(from,to,from.x,from.z+dz)))continue;
                    double dy=to.y-from.y;
                    double distance=visit.distance+Math.sqrt(dx*dx+dz*dz+dy*dy);
                    if(distance>radius+EPS || distance>=reached.getOrDefault(to,Double.POSITIVE_INFINITY))continue;
                    reached.put(to,distance);pending.add(new Visit(to,distance));
                }
            }
        }
    }

    private boolean bridge(Surface from,Surface to,int x,int z) {
        return column(x,z).stream().anyMatch(s->step(from,s) && step(s,to));
    }
    private boolean step(Surface from,Surface to) {
        if(Math.abs(to.y-from.y)>1+EPS)return false;
        double above=Math.max(from.y,to.y)+.05;
        return clear(new Vec3(from.x+.5,from.y,from.z+.5),new Vec3(to.x+.5,to.y,to.z+.5),above);
    }
    private boolean clear(Vec3 from,Vec3 to,double y) {
        return level.clip(new ClipContext(new Vec3(from.x,y,from.z),
            new Vec3(to.x,y,to.z),ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty())).getType()==HitResult.Type.MISS;
    }
    private List<Surface> column(int x,int z) {
        if(Math.abs(x-Math.floor(origin.x))>Math.ceil(radius) || Math.abs(z-Math.floor(origin.z))>Math.ceil(radius))return List.of();
        long key=((long)x<<32) ^ (z & 0xffffffffL);
        return columns.computeIfAbsent(key,ignored->{
            var surfaces=new ArrayList<Surface>();
            for(int y=(int)Math.floor(origin.y-radius)-1;y<=Math.ceil(origin.y+radius);y++) {
                var pos=new BlockPos(x,y,z);
                if(!level.hasChunkAt(pos))continue;
                var shape=level.getBlockState(pos).getCollisionShape(level,pos);
                for(var box:shape.toAabbs()) {
                    if(box.minX>.5 || box.maxX<.5 || box.minZ>.5 || box.maxZ<.5)continue;
                    double top=y+box.maxY;
                    if(Math.abs(top-origin.y)>radius+EPS)continue;
                    // A thin layer of free space distinguishes an exposed top from buried blocks.
                    var clearance=new AABB(x+.49,top+EPS,z+.49,x+.51,top+.1,z+.51);
                    if(level.getBlockCollisions(null,clearance).iterator().hasNext())continue;
                    var surface=new Surface(x,z,top);
                    if(!surfaces.contains(surface))surfaces.add(surface);
                }
            }
            return List.copyOf(surfaces);
        });
    }

    /** Targets must stand on a reached surface; the wave does not cross air gaps. */
    public double distanceTo(LivingEntity target) {
        int x=(int)Math.floor(target.getX()),z=(int)Math.floor(target.getZ());
        double best=Double.POSITIVE_INFINITY;
        for(var surface:column(x,z)) {
            if(Math.abs(target.getY()-surface.y)>.2)continue;
            double distance=reached.getOrDefault(surface,Double.POSITIVE_INFINITY);
            if(!Double.isFinite(distance))continue;
            if(!clear(new Vec3(surface.x+.5,surface.y,surface.z+.5),target.position(),Math.max(surface.y,target.getY())+.05))continue;
            distance+=target.position().distanceTo(new Vec3(surface.x+.5,surface.y,surface.z+.5));
            if(x==(int)Math.floor(origin.x) && z==(int)Math.floor(origin.z) && Math.abs(surface.y-origin.y)<.2
                && clear(origin,target.position(),Math.max(origin.y,target.getY())+.05))
                distance=target.position().distanceTo(origin);
            best=Math.min(best,distance);
        }
        return best;
    }
}
