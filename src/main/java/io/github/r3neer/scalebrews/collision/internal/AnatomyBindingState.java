package io.github.r3neer.scalebrews.collision.internal;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Sole owner of live provider/binding identity state. It has no physics or orchestration callbacks. */
final class AnatomyBindingState {
    record Snapshot(GeometryProvider provider,GeometryProvider.GeometryIdentityDescriptor descriptor,long generation) {
        Snapshot {
            Objects.requireNonNull(provider,"provider");
            if(generation<1)throw new IllegalArgumentException("Invalid binding generation");
        }
        boolean causal(){return descriptor!=null;}
    }

    private static final class Slot {
        long generation;
        GeometryProvider provider;
        GeometryProvider.GeometryIdentityDescriptor descriptor;
        Level level;
        long quarantinedGeneration;
        boolean capturing;
    }

    private static final Map<LivingEntity,Slot> SLOTS=Collections.synchronizedMap(
        new com.google.common.collect.MapMaker().weakKeys().<LivingEntity,Slot>makeMap());

    private AnatomyBindingState() {}

    static synchronized Snapshot rebind(LivingEntity support,GeometryProvider provider,
            GeometryProvider.GeometryIdentityDescriptor descriptor) {
        if(support==null || provider==null)throw new IllegalArgumentException("Missing geometry registration");
        var slot=SLOTS.computeIfAbsent(support,ignored->new Slot());
        slot.generation=Math.incrementExact(slot.generation);
        if(descriptor!=null && descriptor.bindingGeneration()==0)
            descriptor=new GeometryProvider.GeometryIdentityDescriptor(descriptor.epoch(),descriptor.revision(),
                descriptor.model(),descriptor.poseProvider(),slot.generation);
        slot.provider=provider;
        slot.descriptor=descriptor;
        slot.level=support.level();
        slot.quarantinedGeneration=0;
        return new Snapshot(provider,descriptor,slot.generation);
    }

    static synchronized GeometryProvider provider(LivingEntity support) {
        var slot=SLOTS.get(support);return slot==null?null:slot.provider;
    }

    static synchronized GeometryProvider.GeometryIdentityDescriptor descriptor(LivingEntity support) {
        var slot=SLOTS.get(support);return slot==null?null:slot.descriptor;
    }

    static synchronized boolean hasProvider(LivingEntity support) {return provider(support)!=null;}
    static synchronized boolean causal(LivingEntity support) {return descriptor(support)!=null;}

    static synchronized long generation(LivingEntity support) {
        var slot=SLOTS.get(support);return slot==null?0:slot.generation;
    }

    static synchronized Snapshot snapshot(LivingEntity support) {
        var slot=SLOTS.get(support);
        return slot==null || slot.provider==null?null:new Snapshot(slot.provider,slot.descriptor,slot.generation);
    }

    static synchronized boolean current(LivingEntity support,Snapshot snapshot) {
        if(snapshot==null)return false;
        var slot=SLOTS.get(support);
        return slot!=null && slot.provider==snapshot.provider()
            && Objects.equals(slot.descriptor,snapshot.descriptor()) && slot.generation==snapshot.generation;
    }

    static synchronized Snapshot beginCapture(LivingEntity support) {
        var slot=SLOTS.get(support);
        if(slot==null || slot.provider==null || slot.descriptor==null || slot.capturing
                || slot.quarantinedGeneration==slot.generation)return null;
        slot.capturing=true;
        return new Snapshot(slot.provider,slot.descriptor,slot.generation);
    }

    static synchronized void endCapture(LivingEntity support) {
        var slot=SLOTS.get(support);if(slot!=null)slot.capturing=false;
    }

    static synchronized boolean quarantined(LivingEntity support) {
        var slot=SLOTS.get(support);
        return slot!=null && slot.provider!=null && slot.quarantinedGeneration==slot.generation;
    }

    static synchronized void quarantineCurrent(LivingEntity support) {
        var slot=SLOTS.get(support);
        if(slot!=null && slot.provider!=null)slot.quarantinedGeneration=slot.generation;
    }

    static synchronized Map<LivingEntity,GeometryProvider> providersSnapshot() {
        var result=new IdentityHashMap<LivingEntity,GeometryProvider>();
        for(var entry:SLOTS.entrySet())if(entry.getValue().provider!=null)result.put(entry.getKey(),entry.getValue().provider);
        return result;
    }

    static synchronized void deactivate(Level level) {
        if(level==null)return;
        for(var slot:SLOTS.values())if(slot.level==level) {
            slot.provider=null;
            slot.descriptor=null;
            slot.level=null;
            slot.quarantinedGeneration=0;
            // Do not release an outer capture. Its finally owns the reentrancy guard even across lifecycle/rebind.
        }
    }
}
