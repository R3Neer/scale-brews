package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.Map;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;

/** Common geometry source. Missing/unsupported poses return empty, never an entity AABB. */
public interface GeometryProvider {
    record Snapshot(long revision,Map<String,ConvexBox> pieces) {
        public Snapshot {pieces=Map.copyOf(pieces);}
    }
    Optional<Snapshot> sample(LivingEntity entity);
    record MotionSnapshot(long revision,long tick,Map<String,ConservativeSweep.Motion> pieces) {
        public MotionSnapshot {pieces=Map.copyOf(pieces);}
    }
    /** Default is an explicitly static current pose, not a whole-entity bounding box. */
    default Optional<MotionSnapshot> motion(LivingEntity entity) {
        return sample(entity).map(snapshot->{
            Map<String,ConservativeSweep.Motion> motions=new java.util.LinkedHashMap<>();
            snapshot.pieces().forEach((id,box)->motions.put(id,new ConservativeSweep.Motion(t->box,0)));
            return new MotionSnapshot(snapshot.revision(),entity.level().getGameTime(),motions);
        });
    }
    default void tick(LivingEntity entity,long tick) {}
}
