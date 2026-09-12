package io.github.r3neer.scalebrews.collision.runtime;

import io.github.r3neer.scalebrews.collision.physics.SupportTransport;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Identity-local ledger for passive transport already applied to an entity.
 *
 * <p>This owns only cursor/history/lifecycle identity. Applying carry, clipping,
 * contacts, passengers and receipts remain outside this class.</p>
 */
public final class TransportLedger {
    static final int HISTORY_TICKS=40;
    static final int HISTORY_ENTRIES=64;

    private TransportLedger() {}

    // Entity/network IDs are reused across worlds. Guava weak keys use identity equivalence,
    // matching the entity lifetime semantics of the runtime that previously owned this state.
    private static <K,V> Map<K,V> entityMap() {
        return Collections.synchronizedMap(new com.google.common.collect.MapMaker().weakKeys().<K,V>makeMap());
    }

    private static final Map<Entity,SupportTransport> CURRENT=entityMap();
    private static final Map<Entity,History> HISTORY=entityMap();
    private static final Map<Entity,Long> GENERATIONS=entityMap();

    private static final class History {
        final ArrayDeque<SupportTransport> entries=new ArrayDeque<>();
    }

    /** Exact cursor result; a non-contiguous window never exposes a partial delta. */
    public record Window(boolean contiguous,long latestSequence,Vec3 appliedDelta) {
        public Window {
            if(latestSequence<0 || appliedDelta==null || !Double.isFinite(appliedDelta.lengthSqr()))
                throw new IllegalArgumentException("Invalid transport window");
        }
    }

    public static synchronized SupportTransport current(Entity body) {
        return CURRENT.get(Objects.requireNonNull(body,"Missing transport body"));
    }

    /** Physical lifecycle identity paired with the local transport cursor. */
    public static synchronized long generation(Entity body) {
        return GENERATIONS.getOrDefault(Objects.requireNonNull(body,"Missing transport body"),0L);
    }

    /**
     * Returns every applied contribution strictly after {@code consumedSequence} only when the
     * bounded history proves that no sequence is missing. A gap, rewind or pruned prefix is
     * fail-closed: {@code contiguous=false} and zero delta.
     */
    public static synchronized Window since(Entity body,long consumedSequence) {
        Objects.requireNonNull(body,"Missing transport body");
        if(consumedSequence<0)throw new IllegalArgumentException("Invalid consumed transport sequence");
        var current=CURRENT.get(body);
        long latest=current==null?0:current.sequence();
        var history=HISTORY.get(body);
        if(history!=null)prune(history,body.level().getGameTime());
        if(consumedSequence==latest)return new Window(true,latest,Vec3.ZERO);
        if(consumedSequence>latest)return new Window(false,latest,Vec3.ZERO);
        if(history==null || history.entries.isEmpty())return new Window(false,latest,Vec3.ZERO);
        long expected=consumedSequence+1;Vec3 applied=Vec3.ZERO;
        for(var transport:history.entries)if(transport.sequence()>consumedSequence) {
            if(transport.sequence()!=expected)return new Window(false,latest,Vec3.ZERO);
            applied=applied.add(transport.appliedDelta());expected++;
        }
        return expected==latest+1?new Window(true,latest,applied):new Window(false,latest,Vec3.ZERO);
    }

    /** Records one already-applied passive contribution without applying any physical movement. */
    public static synchronized void record(Entity body,SupportTransport transport) {
        Objects.requireNonNull(body,"Missing transport body");Objects.requireNonNull(transport,"Missing transport");
        CURRENT.put(body,transport);
        var history=HISTORY.computeIfAbsent(body,ignored->new History());
        var last=history.entries.peekLast();
        // Preserve the previous fail-closed semantics: a non-growing sequence invalidates the
        // remembered chain instead of making an older cursor appear contiguous.
        if(last!=null && transport.sequence()<=last.sequence())history.entries.clear();
        history.entries.addLast(transport);prune(history,body.level().getGameTime());
        while(history.entries.size()>HISTORY_ENTRIES)history.entries.removeFirst();
    }

    /**
     * Marks a physical lifecycle discontinuity. Applied history survives ordinary contact/support
     * release; teleport/removal-style invalidation may discard it explicitly.
     */
    public static synchronized void invalidate(Entity body,boolean discardTransport) {
        Objects.requireNonNull(body,"Missing transport body");
        GENERATIONS.merge(body,1L,Long::sum);
        if(discardTransport){CURRENT.remove(body);HISTORY.remove(body);}
    }

    /** Removes only ledger state belonging to one level lifecycle. */
    public static synchronized void deactivate(Level level) {
        Objects.requireNonNull(level,"Missing transport level");
        CURRENT.keySet().removeIf(e->e.level()==level);
        HISTORY.keySet().removeIf(e->e.level()==level);
        GENERATIONS.keySet().removeIf(e->e.level()==level);
    }

    private static void prune(History history,long tick) {
        while(history.entries.peekFirst()!=null && history.entries.peekFirst().tick()<tick-HISTORY_TICKS+1)
            history.entries.removeFirst();
    }
}
