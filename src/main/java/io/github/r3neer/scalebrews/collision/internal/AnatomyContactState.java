package io.github.r3neer.scalebrews.collision.internal;

import com.google.common.collect.MapMaker;
import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Sole owner of retained body contact state.
 *
 * <p>This class deliberately stores facts but does not decide physics: it never samples geometry,
 * evaluates gravity/eligibility, performs queries, moves entities, or writes transport receipts.
 * Physical validation belongs to its caller, which may write/read this state only after validating
 * the relevant invariants.</p>
 */
final class AnatomyContactState {
    record ContactEntry(LivingEntity support, String piece, long revision, Vec3 normal, long sequence) {}

    record Anchor(Vec3 local, Vec3 previous, ConvexBox materialBefore, Vec3 supportOrigin, Vec3 bodyOrigin,
                  GravityFrame bodyGravity, GravityFrame supportGravity) {
        Anchor {
            if (materialBefore == null) throw new IllegalArgumentException("Missing material provenance");
        }
    }

    private static <K, V> Map<K, V> entityMap() {
        return Collections.synchronizedMap(new MapMaker().weakKeys().<K, V>makeMap());
    }

    private static final Map<Entity, ContactEntry> CONTACTS = entityMap();
    private static final Map<Entity, Long> CONTACT_SEQUENCES = entityMap();
    private static final Map<Entity, Anchor> ANCHORS = entityMap();
    private static final Map<Entity, SurfaceContact> SURFACES = entityMap();
    private static final Map<Entity, Map<LivingEntity, Long>> SUSPENDED = entityMap();

    private AnatomyContactState() {}

    static synchronized ContactEntry contact(Entity body) {
        return CONTACTS.get(body);
    }

    static synchronized long contactSequence(Entity body) {
        return CONTACT_SEQUENCES.getOrDefault(body, 0L);
    }

    static synchronized ContactEntry setContact(Entity body, LivingEntity support, String piece, long revision, Vec3 normal) {
        var old = CONTACTS.get(body);
        boolean same = old != null && old.support() == support && old.revision() == revision && old.piece().equals(piece);
        long sequence = same ? old.sequence() : Math.incrementExact(CONTACT_SEQUENCES.getOrDefault(body, 0L));
        if (!same) CONTACT_SEQUENCES.put(body, sequence);
        var contact = new ContactEntry(support, piece, revision, normal, sequence);
        CONTACTS.put(body, contact);
        return contact;
    }

    /** Clears the retained material relation, but deliberately preserves its sequence watermark. */
    static synchronized void clear(Entity body) {
        CONTACTS.remove(body);
        ANCHORS.remove(body);
        SURFACES.remove(body);
    }

    static synchronized SurfaceContact surface(Entity body) {
        return SURFACES.get(body);
    }

    static synchronized void surface(Entity body, SurfaceContact surface) {
        if (surface == null) SURFACES.remove(body);
        else SURFACES.put(body, surface);
    }

    static synchronized Anchor anchor(Entity body) {
        return ANCHORS.get(body);
    }

    static synchronized void anchor(Entity body, Anchor anchor) {
        if (anchor == null) ANCHORS.remove(body);
        else ANCHORS.put(body, anchor);
    }

    static synchronized List<Entity> contactBodies() {
        return List.copyOf(CONTACTS.keySet());
    }

    static synchronized List<Entity> bodiesSupportedBy(LivingEntity support) {
        var bodies = new ArrayList<Entity>();
        for (var entry : CONTACTS.entrySet()) if (entry.getValue().support() == support) bodies.add(entry.getKey());
        return List.copyOf(bodies);
    }

    static synchronized Long suspensionGeneration(Entity body, LivingEntity support) {
        var suspended = SUSPENDED.get(body);
        return suspended == null ? null : suspended.get(support);
    }

    static synchronized void suspend(Entity body, LivingEntity support, long generation) {
        SUSPENDED.computeIfAbsent(body, ignored -> entityMap()).put(support, generation);
    }

    static synchronized void clearSuspension(Entity body, LivingEntity support) {
        var suspended = SUSPENDED.get(body);
        if (suspended == null) return;
        suspended.remove(support);
        if (suspended.isEmpty()) SUSPENDED.remove(body);
    }

    static synchronized void deactivate(Level level) {
        // Dimension changes may update body.level() before the old support relation is torn down.
        // Clear a retained relation when either endpoint still belongs to the deactivated level,
        // but preserve the sequence watermark of a body that has already transitioned elsewhere.
        for (var entry : new ArrayList<>(CONTACTS.entrySet())) {
            var body = entry.getKey();
            var support = entry.getValue().support();
            if (body.level() != level && support.level() != level) continue;
            CONTACTS.remove(body);
            ANCHORS.remove(body);
            SURFACES.remove(body);
        }
        CONTACT_SEQUENCES.keySet().removeIf(entity -> entity.level() == level);
        ANCHORS.keySet().removeIf(entity -> entity.level() == level);
        SURFACES.keySet().removeIf(entity -> entity.level() == level);

        for (var body : new ArrayList<>(SUSPENDED.keySet())) {
            var suspended = SUSPENDED.get(body);
            if (suspended == null) continue;
            if (body.level() == level) {
                SUSPENDED.remove(body);
                continue;
            }
            suspended.keySet().removeIf(support -> support.level() == level);
            if (suspended.isEmpty()) SUSPENDED.remove(body);
        }
    }
}
