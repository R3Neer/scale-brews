package io.github.r3neer.scalebrews.collision.internal;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Bounded client inbox for server-issued material-interval certificates.
 * It never infers adjacency or chooses a nearest interval: consumers name one
 * exact material serial inside one exact support lifecycle.
 */
public final class AnatomyCertifiedIntervalInbox {
    public static final int MAX_SUPPORTS=4096;
    public static final int MAX_PER_SUPPORT=128;
    public static final long TTL_CLIENT_TICKS=100;

    private record Entry(AnatomyMaterialIntervalPayload payload,long receivedAt) {}
    private static final class State {
        AnatomyMaterialIntervalPayload identity;
        final ArrayDeque<Entry> entries=new ArrayDeque<>();
        /** Expiration drops material, never the authority to reject its replay. */
        long expiredThrough;
        boolean saturated;
        State(AnatomyMaterialIntervalPayload identity){this.identity=identity;}
    }
    private final Map<UUID,State> states=new HashMap<>();
    private boolean saturated;

    public boolean accept(AnatomyMaterialIntervalPayload payload,long clientTick) {
        if(payload==null || clientTick<0 || saturated)return false;
        var state=states.get(payload.support());
        if(state==null) {
            if(states.size()>=MAX_SUPPORTS){saturated=true;states.clear();return false;}
            state=new State(payload);states.put(payload.support(),state);
        } else {
            int lifecycle=compareLifecycle(state.identity,payload);
            if(lifecycle<0)return false;
            if(lifecycle>0) {
                state=new State(payload);states.put(payload.support(),state);
            } else if(!stableIdentity(state.identity,payload))return false;
        }
        prune(state,clientTick);
        if(state.saturated)return false;
        if(payload.materialSerial()<=state.expiredThrough)return false;
        for(var entry:state.entries)if(entry.payload().materialSerial()==payload.materialSerial()) {
            return entry.payload().equals(payload); // exact duplicate is idempotent; conflicting serial is rejected.
        }
        if(state.entries.size()>=MAX_PER_SUPPORT) {
            state.entries.clear();state.saturated=true;return false;
        }
        state.entries.addLast(new Entry(payload,clientTick));state.identity=payload;
        return true;
    }

    /** Exact lookup only. There is intentionally no latest/nearest fallback. */
    public Optional<AnatomyMaterialIntervalPayload> exact(UUID support,long materialSerial,long clientTick) {
        if(support==null || materialSerial<1 || clientTick<0 || saturated)return Optional.empty();
        var state=states.get(support);
        if(state==null || state.saturated)return Optional.empty();
        prune(state,clientTick);
        for(var entry:state.entries)if(entry.payload().materialSerial()==materialSerial)return Optional.of(entry.payload());
        return Optional.empty();
    }

    /** All exact live certificates for presentation callers that enumerate explicit server serials. */
    public java.util.List<AnatomyMaterialIntervalPayload> certificates(UUID support,long clientTick) {
        if(support==null || clientTick<0 || saturated)return java.util.List.of();
        var state=states.get(support);
        if(state==null || state.saturated)return java.util.List.of();
        prune(state,clientTick);
        return state.entries.stream().map(Entry::payload).toList();
    }

    /** A certificate may precede the first pose of a new tracking/binding window. */
    public void retainLifecycle(UUID support,int supportId,long bindingGeneration,long trackingGeneration) {
        var state=states.get(support);
        if(state!=null && (state.identity.supportId()!=supportId
                || state.identity.bindingGeneration()!=bindingGeneration
                || state.identity.trackingGeneration()!=trackingGeneration))states.remove(support);
    }
    public void discardSupport(UUID support){if(support!=null)states.remove(support);}
    public void clear(){states.clear();saturated=false;}
    public boolean saturated(){return saturated;}
    public int supports(){return states.size();}
    public int entries(UUID support){var state=states.get(support);return state==null?0:state.entries.size();}

    public void prune(long clientTick) {
        if(clientTick<0 || saturated)return;
        for(var state:states.values())prune(state,clientTick);
    }
    private static void prune(State state,long clientTick) {
        if(state.saturated)return;
        while(!state.entries.isEmpty() && clientTick-state.entries.peekFirst().receivedAt()>TTL_CLIENT_TICKS) {
            var expired=state.entries.removeFirst();
            state.expiredThrough=Math.max(state.expiredThrough,expired.payload().materialSerial());
        }
    }

    /**
     * -1 stale, 0 same lifecycle, +1 strictly newer lifecycle.
     * Epoch/revision/dimension are connection/catalog barriers and may not change inside this inbox.
     */
    private static int compareLifecycle(AnatomyMaterialIntervalPayload current,AnatomyMaterialIntervalPayload next) {
        if(!current.epoch().equals(next.epoch()) || current.revision()!=next.revision()
                || !current.dimension().equals(next.dimension()) || !current.support().equals(next.support()))return -1;
        if(next.bindingGeneration()<current.bindingGeneration() || next.trackingGeneration()<current.trackingGeneration())return -1;
        if(next.bindingGeneration()==current.bindingGeneration() && next.trackingGeneration()==current.trackingGeneration())return 0;
        // Pure retrack retains the exact support/network identity. A binding advance may replace it.
        if(next.bindingGeneration()==current.bindingGeneration() && next.supportId()!=current.supportId())return -1;
        return 1;
    }
    private static boolean stableIdentity(AnatomyMaterialIntervalPayload a,AnatomyMaterialIntervalPayload b) {
        return a.epoch().equals(b.epoch()) && a.revision()==b.revision() && a.dimension().equals(b.dimension())
            && a.supportId()==b.supportId() && a.support().equals(b.support())
            && a.bindingGeneration()==b.bindingGeneration() && a.trackingGeneration()==b.trackingGeneration();
    }
}
