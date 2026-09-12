package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.physics.MaterialBroadphase;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Sole owner of the per-level bounded material membership index.
 *
 * <p>The owner stores only already-validated spatial envelopes. It never samples geometry,
 * decides causal identity, filters eligibility/suspension, or applies rejection policy.</p>
 */
final class AnatomySpatialIndex {
    private static final double CELL_SIZE=4;
    private static final long MAX_INDEX_CELLS=4096;
    private static final int MAX_INDEX_CANDIDATES=4096;
    private static final Comparator<LivingEntity> SUPPORT_ORDER=
        Comparator.comparing((LivingEntity entity)->entity.getUUID()).thenComparingInt(Entity::getId);

    private record Index(long tick,MaterialBroadphase<LivingEntity> broadphase) {}
    private static final Map<Level,Index> INDEXES=Collections.synchronizedMap(new WeakHashMap<>());

    private AnatomySpatialIndex() {}

    static synchronized boolean current(Level level,long tick) {
        var index=INDEXES.get(level);
        return index!=null && index.tick()==tick;
    }

    static synchronized List<MaterialBroadphase.Rejected<LivingEntity>> rebuild(Level level,long tick,
            List<MaterialBroadphase.Entry<LivingEntity>> entries) {
        if(level==null || tick<0 || entries==null)throw new IllegalArgumentException("Invalid spatial rebuild");
        var built=MaterialBroadphase.build(entries,SUPPORT_ORDER,CELL_SIZE,
            MAX_INDEX_CELLS,MAX_INDEX_CELLS,MAX_INDEX_CANDIDATES);
        INDEXES.put(level,new Index(tick,built.index()));
        return built.rejected();
    }

    /** Same-tick local maintenance only. A stale/missing index is rebuilt by the causal caller. */
    static synchronized MaterialBroadphase.Rejected<LivingEntity> upsertIfCurrent(LivingEntity support,AABB envelope) {
        if(support==null || envelope==null)throw new IllegalArgumentException("Invalid spatial upsert");
        var level=support.level();var index=INDEXES.get(level);
        if(index==null || index.tick()!=level.getGameTime())return null;
        return index.broadphase().upsert(new MaterialBroadphase.Entry<>(support,envelope));
    }

    /** Same-tick local maintenance only. */
    static synchronized void removeIfCurrent(LivingEntity support) {
        if(support==null)return;
        var level=support.level();var index=INDEXES.get(level);
        if(index==null || index.tick()!=level.getGameTime())return;
        index.broadphase().remove(support);
    }

    /** Returns null only when the caller must rebuild the current tick first. */
    static synchronized MaterialBroadphase.QueryResult<LivingEntity> queryIfCurrent(Level level,long tick,AABB query) {
        if(level==null)throw new IllegalArgumentException("Missing spatial level");
        var index=INDEXES.get(level);
        return index==null || index.tick()!=tick?null:index.broadphase().query(query);
    }

    static synchronized void deactivate(Level level) {
        if(level!=null)INDEXES.remove(level);
    }
}
