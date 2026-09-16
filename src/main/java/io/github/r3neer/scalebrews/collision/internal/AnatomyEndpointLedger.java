package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.spi.RootTransformProvider;
import io.github.r3neer.scalebrews.collision.runtime.RootFrame;
import java.util.Collections;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Weak-identity owner for accepted material endpoint serials.
 *
 * <p>This class owns state and lifecycle retention only. Acceptance, quarantine, contact invalidation,
 * spatial publication and all other physical policy remain in {@link AnatomyMovement}.</p>
 */
final class AnatomyEndpointLedger {
    private AnatomyEndpointLedger() {}

    record Stamp(long jointSampleTick,RootFrame root,RootTransformProvider.RootTransform rootTransform,
                 AnatomyPoseHistory.Sample sample,long revision,GeometryProvider.Availability availability) {}

    record Entry(Stamp stamp,GeometryProvider.CausalEndpoint endpoint,GeometryProvider.Snapshot snapshot,boolean invalidated) {
        Entry {
            if(endpoint==null)throw new IllegalArgumentException("Missing endpoint serial");
        }
        Entry invalidatedCopy(){return invalidated?this:new Entry(stamp,endpoint,snapshot,true);}
    }

    private static final Map<LivingEntity,Entry> ENTRIES=Collections.synchronizedMap(
        new com.google.common.collect.MapMaker().weakKeys().<LivingEntity,Entry>makeMap());

    static Entry get(LivingEntity support){return support==null?null:ENTRIES.get(support);}

    static void put(LivingEntity support,Entry entry) {
        if(support==null || entry==null)throw new IllegalArgumentException("Missing endpoint ledger state");
        ENTRIES.put(support,entry);
    }

    static void clear(LivingEntity support) {
        if(support!=null)ENTRIES.remove(support);
    }

    static void invalidate(LivingEntity support) {
        if(support!=null)ENTRIES.computeIfPresent(support,(ignored,old)->old.invalidatedCopy());
    }

    static void deactivate(Level level) {
        if(level!=null)ENTRIES.keySet().removeIf(entity->entity.level()==level);
    }
}
