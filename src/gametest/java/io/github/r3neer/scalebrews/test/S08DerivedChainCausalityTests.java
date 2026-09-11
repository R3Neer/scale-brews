package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyPoseHistory;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.internal.MaterialEventDispatcher;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S08 causal holdouts for DERIVED_CARRY ordering, ancestry and local chain budgets. */
public final class S08DerivedChainCausalityTests {
    @GameTest
    public void derivedCarryChainRunsBaseToLeafExactlyOnce(GameTestHelper h) {
        var interval=interval();
        var backend=new ChainBackend(interval,Map.of("A","B","B","C"));
        var dispatcher=new MaterialEventDispatcher<String>(4,8,16,key->key);

        var outcome=dispatcher.ingest("A",MaterialEventDispatcher.Source.ROOT_MUTATION,interval,backend);

        h.assertTrue(outcome.status()==MaterialEventDispatcher.Status.APPLIED_PREFIX,
            "Root contribution must remain applied when its certified child chain succeeds");
        h.assertTrue(backend.resolved.equals(List.of("A","B","C")),
            "Derived chain must drain base -> dependent -> leaf exactly once: "+backend.resolved);
        h.assertTrue(backend.counts.equals(Map.of("A",1,"B",1,"C",1)),
            "No support in A -> B -> C may be replayed: "+backend.counts);
        h.assertTrue(backend.sources.equals(List.of(
                MaterialEventDispatcher.Source.ROOT_MUTATION,
                MaterialEventDispatcher.Source.DERIVED_CARRY,
                MaterialEventDispatcher.Source.DERIVED_CARRY)),
            "Only the base event may be ROOT; dependents must retain DERIVED_CARRY provenance: "+backend.sources);
        h.assertTrue(backend.ancestries.get("A").equals(Set.of("A"))
                && backend.ancestries.get("B").equals(Set.of("A","B"))
                && backend.ancestries.get("C").equals(Set.of("A","B","C")),
            "Every derived event must extend, never replace, causal ancestry: "+backend.ancestries);
        h.assertTrue(backend.quarantines.isEmpty(),"Valid A -> B -> C must not quarantine any member: "+backend.quarantines);
        h.succeed();
    }

    @GameTest
    public void cycleDepthAndEventLimitsStopOnlyTheDerivedContinuation(GameTestHelper h) {
        var interval=interval();

        var cycleBackend=new ChainBackend(interval,Map.of("A","B","B","A"));
        var cycleDispatcher=new MaterialEventDispatcher<String>(4,8,16,key->key);
        cycleDispatcher.ingest("A",MaterialEventDispatcher.Source.ROOT_MUTATION,interval,cycleBackend);
        h.assertTrue(cycleBackend.resolved.equals(List.of("A","B")),
            "A -> B -> A cycle must stop before resolving A twice: "+cycleBackend.resolved);
        h.assertTrue(cycleBackend.quarantines.equals(List.of("A:CYCLE")),
            "Cycle rejection must be local and explicit: "+cycleBackend.quarantines);

        var depthBackend=new ChainBackend(interval,Map.of("A","B","B","C","C","D"));
        var depthDispatcher=new MaterialEventDispatcher<String>(4,3,16,key->key);
        depthDispatcher.ingest("A",MaterialEventDispatcher.Source.ROOT_MUTATION,interval,depthBackend);
        h.assertTrue(depthBackend.resolved.equals(List.of("A","B","C")),
            "Depth boundary must admit exactly the configured ancestry depth: "+depthBackend.resolved);
        h.assertTrue(depthBackend.quarantines.equals(List.of("D:DEPTH_LIMIT")),
            "The first child beyond max depth must be rejected locally: "+depthBackend.quarantines);

        var eventBackend=new ChainBackend(interval,Map.of("A","B","B","C"));
        var eventDispatcher=new MaterialEventDispatcher<String>(4,8,2,key->key);
        eventDispatcher.ingest("A",MaterialEventDispatcher.Source.ROOT_MUTATION,interval,eventBackend);
        h.assertTrue(eventBackend.resolved.equals(List.of("A","B")),
            "Event budget must not allow a hidden third derived resolution: "+eventBackend.resolved);
        h.assertTrue(eventBackend.quarantines.equals(List.of("C:QUEUE_LIMIT")),
            "The first derived event beyond the event budget must fail locally: "+eventBackend.quarantines);
        h.succeed();
    }

    private static final class ChainBackend implements MaterialEventDispatcher.Backend<String> {
        private final MaterialEventDispatcher.MaterialInterval interval;
        private final Map<String,String> next;
        final List<String> resolved=new ArrayList<>();
        final List<MaterialEventDispatcher.Source> sources=new ArrayList<>();
        final Map<String,Set<Object>> ancestries=new LinkedHashMap<>();
        final Map<String,Integer> counts=new LinkedHashMap<>();
        final List<String> quarantines=new ArrayList<>();

        ChainBackend(MaterialEventDispatcher.MaterialInterval interval,Map<String,String> next) {
            this.interval=interval;this.next=Map.copyOf(next);
        }
        @Override public MaterialEventDispatcher.Candidates<String> capture(MaterialEventDispatcher.Event<String> event,int maximumBodies) {
            return new MaterialEventDispatcher.Candidates<>(List.of(),false);
        }
        @Override public MaterialEventDispatcher.Candidates<String> captureJointBatch(List<MaterialEventDispatcher.Event<String>> events,int maximumBodies) {
            return new MaterialEventDispatcher.Candidates<>(List.of(),false);
        }
        @Override public MaterialEventDispatcher.Resolution<String> resolve(MaterialEventDispatcher.Event<String> event,List<MaterialEventDispatcher.Candidate<String>> candidates) {
            resolved.add(event.support());sources.add(event.source());ancestries.put(event.support(),event.ancestry());
            counts.merge(event.support(),1,Integer::sum);
            var child=next.get(event.support());
            var derived=child==null?List.<MaterialEventDispatcher.DerivedCarry<String>>of()
                :List.of(new MaterialEventDispatcher.DerivedCarry<>(event.id(),child,interval));
            return new MaterialEventDispatcher.Resolution<>(MaterialEventDispatcher.Outcome.applied(1),derived);
        }
        @Override public MaterialEventDispatcher.BatchResolution<String> resolveJointBatch(List<MaterialEventDispatcher.Event<String>> events,List<MaterialEventDispatcher.Candidate<String>> candidates) {
            throw new AssertionError("Chain fixture does not use simultaneous joint batches");
        }
        @Override public void quarantine(MaterialEventDispatcher.Event<String> event,MaterialEventDispatcher.Reason reason) {
            quarantines.add(event.support()+":"+reason);
        }
    }

    private static MaterialEventDispatcher.MaterialInterval interval() {
        var identity=new GeometryProvider.GeometryIdentity(Level.OVERWORLD,UUID.randomUUID(),1,UUID.randomUUID(),312,
            Identifier.parse("test:s08_chain"),Identifier.parse("test:s08_chain_pose"),1,1);
        var inputs=new PoseProvider.Inputs(0,0,0,0,0,true);
        var beforeRoot=new AnatomyMovement.RootFrame(1,1,Vec3.ZERO,0,1,GravityFrame.VANILLA);
        var afterRoot=new AnatomyMovement.RootFrame(2,1,new Vec3(.1,0,0),0,1,GravityFrame.VANILLA);
        var beforeSample=new AnatomyPoseHistory.Sample(inputs,beforeRoot.origin(),0,1,GravityFrame.VANILLA);
        var afterSample=new AnatomyPoseHistory.Sample(inputs,afterRoot.origin(),0,1,GravityFrame.VANILLA);
        var box=ConvexBox.of(new AABB(0,0,0,1,1,1),new Matrix4f());
        var snapshot=new GeometryProvider.Snapshot(312,Map.of("piece",box));
        var before=new GeometryProvider.QueryFrame(identity,
            new GeometryProvider.CausalEndpoint(1,1,1,beforeRoot,beforeSample,GeometryProvider.Availability.AVAILABLE),snapshot);
        var after=new GeometryProvider.QueryFrame(identity,
            new GeometryProvider.CausalEndpoint(2,1,1,afterRoot,afterSample,GeometryProvider.Availability.AVAILABLE),snapshot);
        var handle=new GeometryProvider.MotionIntervalHandle(identity,1,before,after);
        return new MaterialEventDispatcher.MaterialInterval(handle,new AABB(-1,-1,-1,2,2,2));
    }
}
