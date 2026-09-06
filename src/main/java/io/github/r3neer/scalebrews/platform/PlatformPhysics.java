package io.github.r3neer.scalebrews.platform;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import java.util.*;

/** Only called from movement, support and edge checks. Never extends global noCollision/pathfinding. */
public final class PlatformPhysics {
    private static final ThreadLocal<Entity> MOVING = new ThreadLocal<>();
    public static final double EPS = 1e-5;
    private PlatformPhysics() {}
    public static Entity enter(Entity body) { Entity old=MOVING.get(); MOVING.set(body); return old; }
    public static void exit(Entity old) { if (old==null) MOVING.remove(); else MOVING.set(old); }
    public record Contact(LivingEntity support, PlatformDefinition.Surface surface, double y, double t) {}
    public static Contact contact(Entity e, Vec3 delta) {
        if (!Platforms.ordinary(e) || Platforms.category(e)==null) return null;
        AABB box=e.getBoundingBox();
        Contact best=null;
        for (LivingEntity support : e.level().getEntitiesOfClass(LivingEntity.class, box.expandTowards(delta).inflate(Platforms.searchMargin(e.level())),
                s -> Platforms.eligible(e,s))) {
            var f=PlatformGeometry.frame(support);
            for (var surface: Platforms.definition(support).surfaces()) {
                double y=PlatformGeometry.height(f,surface), distance=y-box.minY;
                if (distance>EPS || delta.y>EPS || distance<delta.y-EPS) continue;
                double t=Math.abs(delta.y)<EPS?0:Math.clamp(distance/delta.y,0,1);
                if (!PlatformGeometry.overlaps(f,surface,box.move(delta.scale(t)))) continue;
                var current=new Contact(support,surface,y,t);
                if (best==null || t<best.t-EPS || Math.abs(t-best.t)<EPS
                    && (support==Platforms.state(e).support || support.getId()<best.support.getId())) best=current;
            }
        }
        return best;
    }
    public static boolean suppressPair(Entity body, Entity other) {
        if (MOVING.get()!=body || !(other instanceof LivingEntity support)) return false;
        if (Platforms.state(body).carrying && Platforms.state(body).support==support) return true;
        if (!Platforms.eligible(body,support)) return false;
        var f=PlatformGeometry.frame(support);
        for (var surface: Platforms.definition(support).surfaces())
            if (body.getBoundingBox().minY>=PlatformGeometry.height(f,surface)-EPS
                && PlatformGeometry.overlaps(f,surface,body.getBoundingBox().inflate(.1))) return true;
        return false;
    }
    public static Vec3 collide(Entity e, Vec3 requested, Vec3 vanilla) {
        if(!Platforms.simulates(e)) return vanilla;
        var state=Platforms.state(e);
        if (state.carrying) return vanilla;
        if (requested.y>EPS) { state.clear(); return vanilla; }
        Contact c=contact(e,vanilla);
        if (c==null && state.support!=null && Platforms.eligible(e,state.support)) {
            var frame=PlatformGeometry.frame(state.support);
            double maxStep=e instanceof LivingEntity living ? living.maxUpStep() : 0;
            for(var next:Platforms.definition(state.support).surfaces()) {
                double y=PlatformGeometry.height(frame,next), rise=y-e.getY();
                if(rise>EPS && rise<=maxStep && PlatformGeometry.overlaps(frame,next,e.getBoundingBox().move(vanilla.x,rise,vanilla.z))
                    && e.level().noCollision(e,e.getBoundingBox().move(vanilla.x,rise,vanilla.z))) {
                    c=new Contact(state.support,next,y,0); break;
                }
            }
        }
        if (c==null) return vanilla;
        state.support=c.support; state.surface=c.surface; state.frame=PlatformGeometry.frame(c.support);
        state.contact=state.frame.local(new Vec3(e.getX()+vanilla.x,c.y,e.getZ()+vanilla.z));
        state.landed=true;
        return new Vec3(vanilla.x,c.y-e.getBoundingBox().minY,vanilla.z);
    }
    public static void afterMove(Entity e) {
        if(!Platforms.simulates(e)) return;
        var s=Platforms.state(e);
        if (s.carrying || s.support==null) return;
        if (!Platforms.eligible(e,s.support)) { s.clear(); return; }
        var f=PlatformGeometry.frame(s.support);
        if (!PlatformGeometry.overlaps(f,s.surface,e.getBoundingBox())
                || Math.abs(e.getY()-PlatformGeometry.height(f,s.surface))>.025) { s.clear(); return; }
        s.contact=f.local(e.position()); s.frame=f;
        s.contact=new Vec3(s.contact.x,s.surface.y(),s.contact.z);
        e.setOnGround(true); e.verticalCollisionBelow=true;
    }
    public static boolean touching(Entity e) {
        var s=Platforms.state(e);
        return s.support!=null && Math.abs(e.getY()-PlatformGeometry.height(PlatformGeometry.frame(s.support),s.surface))<.025
            && PlatformGeometry.overlaps(PlatformGeometry.frame(s.support),s.surface,e.getBoundingBox());
    }
    public static void carry(Entity e) {
        if(!Platforms.simulates(e)) return;
        var s=Platforms.state(e);
        if (s.support==null || s.visiting) return;
        if (!Platforms.eligible(e,s.support)) { s.clear(); return; }
        s.visiting=true;
        try {
            carry(s.support);
            var now=PlatformGeometry.frame(s.support);
            Vec3 target=now.world(s.contact), delta=target.subtract(e.position());
            if (now.origin().distanceTo(s.frame.origin())>4 || delta.lengthSqr()>16) { s.clear(); return; }
            if (delta.lengthSqr()>1e-12) {
                s.carrying=true;
                Entity previous=enter(e);
                try {
                    Vec3 adapted=Platforms.adaptTransport(e,delta);
                    if(adapted==null || !Double.isFinite(adapted.lengthSqr()) || adapted.lengthSqr()>16) { s.clear(); return; }
                    Vec3 allowed=Entity.collideBoundingBox(e,adapted,e.getBoundingBox(),e.level(),
                        e.level().getEntityCollisions(e,e.getBoundingBox().expandTowards(delta)));
                    e.setPos(e.position().add(allowed));
                    if(e instanceof net.minecraft.server.level.ServerPlayer player)
                        ((PlatformConnection)player.connection).scalebrews$transportBaseline(e,allowed);
                    for(var passenger:e.getIndirectPassengers())
                        if(passenger instanceof net.minecraft.server.level.ServerPlayer player)
                            ((PlatformConnection)player.connection).scalebrews$transportBaseline(e,allowed);
                    s.transported=s.transported.add(allowed);
                    if (allowed.distanceToSqr(delta)>1e-6) { s.clear(); return; }
                } finally { s.carrying=false; exit(previous); }
            }
            s.frame=now;
            e.setOnGround(true); e.verticalCollisionBelow=true;
        } finally { s.visiting=false; }
    }
    /** A rising support may meet a falling/resting body; no horizontal side capture. */
    public static void movedSupport(LivingEntity support,PlatformGeometry.Frame before) {
        if(!Platforms.ordinary(support) || Platforms.definition(support)==null) return;
        var now=PlatformGeometry.frame(support);
        if(now.origin().distanceTo(before.origin())>4) return;
        AABB area=support.getBoundingBox().expandTowards(before.origin().subtract(now.origin())).inflate(Platforms.searchMargin(support.level()));
        for(Entity body:support.level().getEntities(support,area,e->Platforms.simulates(e) && Platforms.eligible(e,support))) {
            if(Platforms.supported(body) || body.getDeltaMovement().y>EPS) continue;
            for(var surface:Platforms.definition(support).surfaces()) {
                double oldY=PlatformGeometry.height(before,surface),newY=PlatformGeometry.height(now,surface),feet=body.getY();
                if(newY<=oldY || feet<oldY-EPS || feet>newY+EPS
                    || !PlatformGeometry.overlaps(now,surface,body.getBoundingBox())) continue;
                var state=Platforms.state(body);
                state.support=support; state.surface=surface; state.frame=before;
                var local=now.local(body.position());
                state.contact=new Vec3(local.x,surface.y(),local.z);
                carry(body);
                break;
            }
        }
    }
    public static Vec3 edge(Player e, Vec3 delta) {
        if (!Platforms.supported(e) || !e.isShiftKeyDown() || delta.y>EPS) return delta;
        var s=Platforms.state(e); var f=PlatformGeometry.frame(s.support);
        double x=delta.x,z=delta.z, step=Math.max(.005,.05*e.getScale());
        while ((Math.abs(x)>EPS || Math.abs(z)>EPS)
            && !hasEdgeSupport(e,e.getBoundingBox().move(x,0,z))
            && e.level().noCollision(e,e.getBoundingBox().move(x,-e.maxUpStep(),z))) {
            x=Math.abs(x)<=step?0:x-Math.copySign(step,x);
            z=Math.abs(z)<=step?0:z-Math.copySign(step,z);
        }
        return new Vec3(x,delta.y,z);
    }
    private static boolean hasEdgeSupport(Player body,AABB box) {
        var state=Platforms.state(body);
        var frame=PlatformGeometry.frame(state.support);
        for(var surface:Platforms.definition(state.support).surfaces()) {
            double gap=PlatformGeometry.height(frame,surface)-body.getY();
            if(Math.abs(gap)<=body.maxUpStep()+EPS && PlatformGeometry.overlaps(frame,surface,box)) return true;
        }
        return false;
    }
}
