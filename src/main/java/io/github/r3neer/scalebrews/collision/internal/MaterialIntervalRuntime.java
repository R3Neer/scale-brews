package io.github.r3neer.scalebrews.collision.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Runtime owner for per-support material interval fences and the pending handle queue.
 * It deliberately does not resolve physics; S07 consumes the already-fenced handles.
 */
public final class MaterialIntervalRuntime {
    private MaterialIntervalRuntime() {}
    public enum Source {ROOT,JOINT}
    private static final Map<LivingEntity,MaterialIntervalTracker> TRACKERS=
        Collections.synchronizedMap(new com.google.common.collect.MapMaker().weakKeys().<LivingEntity,MaterialIntervalTracker>makeMap());
    /**
     * Insertion order is causal for root mutations. Joint observations are appended by
     * AnatomyRuntime.publish in its explicit UUID/entity-id canonical order, because
     * that cadence is simultaneous rather than causally ordered.
     */
    private static final Map<Level,List<Pending>> PENDING=Collections.synchronizedMap(new WeakHashMap<>());

    public record Pending(LivingEntity support,Source source,GeometryProvider.MotionIntervalHandle handle) {
        public Pending {if(support==null || source==null || handle==null)throw new IllegalArgumentException("Invalid pending material interval");}
    }
    public record RootCapture(AnatomyMovement.RootFrame root,GeometryProvider.QueryFrame before) {
        public RootCapture {if(root==null)throw new IllegalArgumentException("Missing root capture");}
    }

    public static RootCapture captureRoot(LivingEntity support) {
        return new RootCapture(AnatomyMovement.captureRoot(support),AnatomyMovement.queryFrame(support).orElse(null));
    }

    public static void commitRoot(LivingEntity support,RootCapture capture) {
        if(support==null || capture==null)return;
        AnatomyMovement.observeRoot(support,capture.root());
        accept(support,Source.ROOT,capture.before(),AnatomyMovement.queryFrame(support).orElse(null));
    }

    /** Observe one post-tick current frame. Repeated reads are no-ops; a joint advance publishes one handle. */
    public static void observe(LivingEntity support) {
        if(support==null)return;
        var after=AnatomyMovement.queryFrame(support).orElse(null);
        var tracker=TRACKERS.computeIfAbsent(support,ignored->new MaterialIntervalTracker());
        var before=tracker.current();
        if(after==null) {
            AnatomyMovement.publishedFrame(support).ifPresentOrElse(frame->tracker.cut(frame.identity()),tracker::cut);
            return;
        }
        if(before==null) {tracker.seed(after);return;}
        accept(support,Source.JOINT,before,after);
    }

    private static void accept(LivingEntity support,Source source,GeometryProvider.QueryFrame before,GeometryProvider.QueryFrame after) {
        var tracker=TRACKERS.computeIfAbsent(support,ignored->new MaterialIntervalTracker());
        if(after==null) {
            AnatomyMovement.publishedFrame(support).ifPresentOrElse(frame->tracker.cut(frame.identity()),tracker::cut);
            return;
        }
        if(before==null) {tracker.seed(after);return;}
        if(tracker.current()==null)tracker.seed(before);
        var currentBefore=tracker.current();
        var result=tracker.accept(before,after);
        if(result.outcome()==MaterialIntervalTracker.Outcome.ADVANCED) {
            PENDING.computeIfAbsent(support.level(),ignored->new ArrayList<>()).add(new Pending(support,source,result.handle()));
        } else if(result.outcome()==MaterialIntervalTracker.Outcome.GAP_OR_STALE && currentBefore!=null
                && currentBefore.identity().equals(after.identity())
                && after.endpoint().frameSerial()>currentBefore.endpoint().frameSerial()) {
            // Explicitly resynchronise after a detected gap without publishing fabricated history.
            tracker.seed(after);
        }
    }

    /** Lifecycle barrier for teleports/removal/unavailable transitions. */
    public static void invalidate(LivingEntity support) {
        var tracker=TRACKERS.get(support);
        if(tracker==null)return;
        AnatomyMovement.publishedFrame(support).ifPresentOrElse(frame->tracker.cut(frame.identity()),tracker::cut);
        PENDING.computeIfPresent(support.level(),(level,list)->{list.removeIf(p->p.support()==support);return list.isEmpty()?null:list;});
    }

    /** One-shot queue seam for S07. Root order is causal; joint subgroups are already canonical. */
    public static List<Pending> poll(Level level) {
        var list=PENDING.remove(level);
        return list==null || list.isEmpty()?List.of():List.copyOf(list);
    }

    public static void clear(Level level) {
        TRACKERS.keySet().removeIf(entity->entity.level()==level);
        PENDING.remove(level);
    }

    public static void clear(MinecraftServer server) {
        if(server==null)return;
        for(var level:server.getAllLevels())clear(level);
    }
}
