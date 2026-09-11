package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyPoseHistory;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.internal.MaterialIntervalTracker;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** G2/S06 holdouts for material interval identity and replay fencing. */
public final class S06MaterialIntervalIdentityTests {
    private static final PoseProvider.Inputs INPUTS = new PoseProvider.Inputs(0,0,0,0,0,true);

    @GameTest public void sameTickAdvancesRemainDistinctAndReplayIsFenced(GameTestHelper h) {
        var tracker = new MaterialIntervalTracker();
        var id = identity(h, UUID.randomUUID(), 1, 1);
        var a = frame(id, 1, 40, 40, Vec3.ZERO, GravityFrame.VANILLA);
        var b = frame(id, 2, 40, 40, new Vec3(.1,0,0), GravityFrame.VANILLA);
        var c = frame(id, 3, 40, 40, new Vec3(.2,0,0), GravityFrame.VANILLA);
        var first = tracker.accept(a,b);
        var second = tracker.accept(b,c);
        h.assertTrue(first.outcome()==MaterialIntervalTracker.Outcome.ADVANCED && first.handle().materialSerial()==1,
            "First same-tick material advance must publish serial 1");
        h.assertTrue(second.outcome()==MaterialIntervalTracker.Outcome.ADVANCED && second.handle().materialSerial()==2,
            "Second same-tick material advance must publish a distinct serial");
        h.assertTrue(tracker.accept(b,c).outcome()==MaterialIntervalTracker.Outcome.REPLAY && tracker.materialSerial()==2,
            "Exact replay must not advance the material serial");
        h.succeed();
    }

    @GameTest public void gapsStaleFramesAndIdentityChangesFailClosed(GameTestHelper h) {
        var tracker = new MaterialIntervalTracker();
        var id = identity(h, UUID.randomUUID(), 2, 1);
        var a = frame(id,1,10,10,Vec3.ZERO,GravityFrame.VANILLA);
        var b = frame(id,2,11,11,new Vec3(.1,0,0),GravityFrame.VANILLA);
        var d = frame(id,4,12,12,new Vec3(.3,0,0),GravityFrame.VANILLA);
        h.assertTrue(tracker.accept(a,b).outcome()==MaterialIntervalTracker.Outcome.ADVANCED,"Contiguous pair must advance");
        h.assertTrue(tracker.accept(b,d).outcome()==MaterialIntervalTracker.Outcome.GAP_OR_STALE,
            "Skipped frame serial must be explicit gap, never invented history");
        h.assertTrue(tracker.accept(a,b).outcome()==MaterialIntervalTracker.Outcome.REPLAY,
            "Previously accepted exact pair remains replay-fenced");
        var other = identity(h, id.support(), 2, 2);
        var e = frame(other,1,12,12,new Vec3(.4,0,0),GravityFrame.VANILLA);
        h.assertTrue(tracker.accept(b,e).outcome()==MaterialIntervalTracker.Outcome.DISCONTINUITY && tracker.materialSerial()==0,
            "Binding identity change starts a new stream and resets its material serial");
        h.succeed();
    }

    @GameTest public void sameTickMotionSnapshotIsValidButRewindIsRejected(GameTestHelper h) {
        var box = ConvexBox.of(new AABB(0,0,0,1,1,1),new Matrix4f());
        var motion = new io.github.r3neer.scalebrews.collision.physics.ConservativeSweep.Motion(t->box,0);
        new GeometryProvider.MotionSnapshot(1,25,25,Vec3.ZERO,Vec3.ZERO,Map.of("body",motion));
        boolean rejected=false;
        try {new GeometryProvider.MotionSnapshot(1,26,25,Vec3.ZERO,Vec3.ZERO,Map.of("body",motion));}
        catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected,"MotionSnapshot must allow same-tick causality but reject time rewind");
        h.succeed();
    }

    @GameTest public void gravityChangeIsADiscontinuity(GameTestHelper h) {
        var tracker = new MaterialIntervalTracker();
        var id = identity(h,UUID.randomUUID(),3,1);
        var a = frame(id,1,5,5,Vec3.ZERO,GravityFrame.VANILLA);
        var b = frame(id,2,5,5,Vec3.ZERO,new GravityFrame(net.minecraft.core.Direction.EAST));
        h.assertTrue(tracker.accept(a,b).outcome()==MaterialIntervalTracker.Outcome.DISCONTINUITY,
            "Gravity lifecycle change must not become a continuous material interval");
        h.succeed();
    }

    private static GeometryProvider.GeometryIdentity identity(GameTestHelper h,UUID support,long revision,long generation) {
        return new GeometryProvider.GeometryIdentity(h.getLevel().dimension(),support,1,UUID.randomUUID(),revision,
            Identifier.parse("test:s06_model"),Identifier.parse("test:s06_pose"),generation,generation);
    }

    private static GeometryProvider.QueryFrame frame(GeometryProvider.GeometryIdentity id,long serial,long authority,long joint,Vec3 origin,GravityFrame gravity) {
        var root = new AnatomyMovement.RootFrame(serial,authority,origin,0,1,gravity);
        var sample = new AnatomyPoseHistory.Sample(INPUTS,origin,0,1,gravity);
        var endpoint = new GeometryProvider.CausalEndpoint(serial,authority,joint,root,sample,GeometryProvider.Availability.AVAILABLE);
        var snapshot = new GeometryProvider.Snapshot(id.revision(),Map.of("body",ConvexBox.of(new AABB(-.5,-.5,-.5,.5,.5,.5),new Matrix4f()).move(origin)));
        return new GeometryProvider.QueryFrame(id,endpoint,snapshot);
    }
}
