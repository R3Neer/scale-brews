package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Common geometry source. Missing/unsupported poses return empty, never an entity AABB. */
public interface GeometryProvider {
    /** Runtime-supplied causal descriptor; never derive model id from ModelGeometry.source(). */
    record GeometryIdentityDescriptor(UUID epoch,long revision,Identifier model,Identifier poseProvider,long bindingGeneration) {
        /** Server-only compatibility constructor: registration allocates the authoritative generation. */
        public GeometryIdentityDescriptor(UUID epoch,long revision,Identifier model,Identifier poseProvider) {this(epoch,revision,model,poseProvider,0);}
        public GeometryIdentityDescriptor {if(epoch==null || revision<0 || model==null || poseProvider==null || bindingGeneration<0)throw new IllegalArgumentException("Invalid geometry descriptor");}
    }
    record GeometryIdentity(ResourceKey<Level> dimension,UUID support,int entityId,UUID epoch,long revision,
            Identifier model,Identifier poseProvider,long bindingGeneration,long localRegistrationGeneration) {
        public GeometryIdentity {if(dimension==null || support==null || entityId<0 || epoch==null || revision<0 || model==null || poseProvider==null || bindingGeneration<1 || localRegistrationGeneration<1)throw new IllegalArgumentException("Invalid geometry identity");}
    }
    /**
     * A sampled causal endpoint.  The provider (or a server-side adapter) owns the
     * tick and root serial: callers must never fill these fields from live client
     * entity state.  This is an instantaneous convex frame, not a sweep interval.
     */
    /** Pose/root is publishable even when this endpoint deliberately has no convex geometry. */
    enum Availability {AVAILABLE,UNAVAILABLE}
    record CausalEndpoint(long frameSerial,long authorityTick,long jointSampleTick,AnatomyMovement.RootFrame root,AnatomyPoseHistory.Sample sample,Availability availability) {
        public CausalEndpoint {
            if(frameSerial<1 || authorityTick<0 || jointSampleTick<0 || root==null || sample==null || availability==null
                    || jointSampleTick>authorityTick || root.tick()>authorityTick
                    || !root.origin().equals(sample.origin()) || Float.compare(root.yaw(),sample.yaw())!=0
                    || Float.compare(root.scale(),sample.scale())!=0 || !root.gravity().equals(sample.gravity()))
                throw new IllegalArgumentException("Invalid causal endpoint");
        }
    }
    /** Current immutable convex endpoint; it never implies a historical sweep. */
    record QueryFrame(GeometryIdentity identity,CausalEndpoint endpoint,Snapshot snapshot) {
        public QueryFrame {if(identity==null || endpoint==null || snapshot==null || endpoint.availability()!=Availability.AVAILABLE || snapshot.revision()!=identity.revision())throw new IllegalArgumentException("Invalid query frame");}
        public long authorityTick(){return endpoint.authorityTick();}
        public AnatomyMovement.RootFrame root(){return endpoint.root();}
        public AnatomyPoseHistory.Sample sample(){return endpoint.sample();}
    }
    /** Wire/publication handle; unlike QueryFrame it represents a valid unavailable pose too. */
    record PublishedFrame(GeometryIdentity identity,CausalEndpoint endpoint) {
        public PublishedFrame {if(identity==null || endpoint==null)throw new IllegalArgumentException("Invalid published frame");}
    }
    record MotionIntervalHandle(GeometryIdentity identity,long materialSerial,QueryFrame before,QueryFrame after) {
        /** A gravity change is a lifecycle discontinuity, never a continuous material interval. */
        public MotionIntervalHandle {if(materialSerial<1 || before==null || after==null || !before.identity().equals(identity) || !after.identity().equals(identity)
                || after.authorityTick()<before.authorityTick() || after.endpoint().frameSerial()<=before.endpoint().frameSerial()
                || after.endpoint().jointSampleTick()<before.endpoint().jointSampleTick() || !before.root().gravity().equals(after.root().gravity()))throw new IllegalArgumentException("Invalid motion handle");}
    }
    record Snapshot(long revision,Map<String,ConvexBox> pieces) {
        public Snapshot {if(revision<0)throw new IllegalArgumentException("Invalid snapshot revision");pieces=validatedPieces(pieces);}
    }
    private static <T> Map<String,T> validatedPieces(Map<String,T> pieces) {
        if(pieces==null || pieces.size()>4096 || pieces.entrySet().stream().anyMatch(e->e.getKey()==null
                || e.getKey().isBlank() || e.getKey().length()>256 || e.getValue()==null))
            throw new IllegalArgumentException("Invalid physical pieces");
        return Map.copyOf(pieces);
    }
    Optional<Snapshot> sample(LivingEntity entity);
    /**
     * Supplies a root/TRS/snapshot endpoint captured by the same authority sample.
     * Client adapters must implement this from accepted protocol data, never from
     * entity position, render animation, or local game time.
     */
    default Optional<CausalEndpoint> causalEndpoint(LivingEntity entity) {return Optional.empty();}
    /** Certified motion provenance. Same-tick material advances are valid; rewinds are not. */
    record MotionSnapshot(long revision,long fromTick,long toTick,Vec3 supportOriginFrom,Vec3 supportOriginTo,Map<String,ConservativeSweep.Motion> pieces) {
        public MotionSnapshot {
            if(revision<0 || fromTick<0 || toTick<fromTick || supportOriginFrom==null || supportOriginTo==null
                || !Double.isFinite(supportOriginFrom.lengthSqr()+supportOriginTo.lengthSqr()))throw new IllegalArgumentException("Invalid motion interval");
            pieces=validatedPieces(pieces);
        }
    }
    /**
     * Certify motion from an already accepted causal handle. Providers must derive
     * only from handle.before()/after(); no later live entity state may repair history.
     */
    default Optional<MotionSnapshot> interval(LivingEntity entity,MotionIntervalHandle handle) {return Optional.empty();}
    /**
     * Legacy tick->tick provenance seam. S06+ runtime paths prefer interval(entity, handle).
     */
    default Optional<MotionSnapshot> motion(LivingEntity entity) {return Optional.empty();}
    default void tick(LivingEntity entity,long tick) {}
}
