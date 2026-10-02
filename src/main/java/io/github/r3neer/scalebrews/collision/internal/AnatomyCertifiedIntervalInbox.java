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
        boolean saturated;
        State(AnatomyMaterialIntervalPayload identity){this.identity=identity;}
    }
    private final Map<UUID,State> states=new HashMap<>();
    private boolean saturated;

    public boolean accept(AnatomyMaterialIntervalPayload payload,long clientTick) {
        if(payload==null || clientTick<0 || saturated)return false;
        prune(clientTick);
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
        if(state.saturated)return false;
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
        prune(clientTick);var state=states.get(support);
        if(state==null || state.saturated)return Optional.empty();
        for(var entry:state.entries)if(entry.payload().materialSerial()==materialSerial)return Optional.of(entry.payload());
        return Optional.empty();
    }

    /** All exact live certificates for presentation callers that enumerate explicit server serials. */
    public java.util.List<AnatomyMaterialIntervalPayload> certificates(UUID support,long clientTick) {
        if(support==null || clientTick<0 || saturated)return java.util.List.of();
        prune(clientTick);var state=states.get(support);
        if(state==null || state.saturated)return java.util.List.of();
        return state.entries.stream().map(Entry::payload).toList();
    }

    public void discardSupport(UUID support){if(support!=null)states.remove(support);}
    public void clear(){states.clear();saturated=false;}
    public boolean saturated(){return saturated;}
    public int supports(){return states.size();}
    public int entries(UUID support){var state=states.get(support);return state==null?0:state.entries.size();}

    public void prune(long clientTick) {
        if(clientTick<0 || saturated)return;
        states.entrySet().removeIf(entry->{
            var state=entry.getValue();
            if(state.saturated)return false;
            while(!state.entries.isEmpty() && state.entries.peekFirst().receivedAt()+TTL_CLIENT_TICKS<clientTick)
                state.entries.removeFirst();
            return state.entries.isEmpty();
        });
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
