package io.github.r3neer.scalebrews.collision.runtime;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Bounded rigid-root provenance independent of collision/contact policy.
 * The ledger owns sequence allocation and retention only; callers decide what a discontinuity means physically.
 */
public final class RootFrameLedger {
    private RootFrameLedger() {}
    private static final class History {final ArrayDeque<RootFrame> frames=new ArrayDeque<>();}
    private static final Map<LivingEntity,History> HISTORIES=Collections.synchronizedMap(
        new com.google.common.collect.MapMaker().weakKeys().<LivingEntity,History>makeMap());

    public record Observation(RootFrame frame,boolean discontinuity) {
        public Observation {
            if(frame==null)throw new IllegalArgumentException("Missing root frame observation");
        }
    }

    public static synchronized Observation observe(LivingEntity support,long tick,Vec3 origin,float yaw,float scale,GravityFrame gravity) {
        return observe(support,tick,origin,yaw,scale,gravity,null);
    }

    /**
     * Records the current root against the canonical ledger tail. A stale/missing explicit before frame
     * never invents an alternate segment; it falls back to the same canonical tail used by ordinary observation.
     */
    public static synchronized Observation observe(LivingEntity support,long tick,Vec3 origin,float yaw,float scale,
                                                   GravityFrame gravity,RootFrame before) {
        if(support==null)throw new IllegalArgumentException("Missing root support");
        var history=HISTORIES.computeIfAbsent(support,ignored->new History());
        var last=history.frames.peekLast();
        // Preserve the former capture/observe contract: an explicit before only certifies that the
        // callback belongs to the canonical tail; stale callbacks still observe against that tail.
        if(before!=null && last!=null && (before.sequence()!=last.sequence() || !sameRoot(before,last)))before=null;
        if(last==null) {
            var initial=new RootFrame(0,tick,origin,yaw,scale,gravity);
            history.frames.add(initial);
            return new Observation(initial,false);
        }
        var current=new RootFrame(Math.incrementExact(last.sequence()),tick,origin,yaw,scale,gravity);
        if(sameRoot(last,current))return new Observation(last,false);
        boolean discontinuity=last.origin().distanceToSqr(current.origin())>16 || !last.gravity().equals(current.gravity());
        if(discontinuity)history.frames.clear();
        history.frames.add(current);
        trim(history,current.tick());
        return new Observation(current,discontinuity);
    }

    public static synchronized void clear(LivingEntity support) {
        if(support!=null)HISTORIES.remove(support);
    }

    public static synchronized void deactivate(Level level) {
        if(level!=null)HISTORIES.keySet().removeIf(entity->entity.level()==level);
    }

    static int retainedFrames(LivingEntity support) {
        var history=HISTORIES.get(support);
        return history==null?0:history.frames.size();
    }

    private static boolean sameRoot(RootFrame a,RootFrame b) {
        return a.origin().equals(b.origin()) && Float.compare(a.yaw(),b.yaw())==0
            && Float.compare(a.scale(),b.scale())==0 && a.gravity().equals(b.gravity());
    }

    private static void trim(History history,long tick) {
        while(history.frames.size()>64 || history.frames.peekFirst()!=null && history.frames.peekFirst().tick()<tick-20)
            history.frames.removeFirst();
    }
}
