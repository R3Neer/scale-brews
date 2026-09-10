package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.platform.anatomy.AnatomyMovement;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyPoseHistory;
import io.github.r3neer.scalebrews.platform.anatomy.ConvexBox;
import io.github.r3neer.scalebrews.platform.anatomy.GeometryProvider;
import io.github.r3neer.scalebrews.platform.anatomy.GravityFrame;
import io.github.r3neer.scalebrews.platform.anatomy.MaterialEventDispatcher;
import io.github.r3neer.scalebrews.platform.anatomy.PoseProvider;
import io.github.r3neer.scalebrews.platform.anatomy.RootEventDispatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Q2 helper coverage: no mixin, Level entity query, receipt, or network path is enabled here. */
public class AnatomyMaterialEventDispatcherTests {
    private static AnatomyMovement.RootFrame root(long sequence,long tick,double x) {
        return new AnatomyMovement.RootFrame(sequence,tick,new Vec3(x,0,0),0,1,GravityFrame.VANILLA);
    }
    private static MaterialEventDispatcher.MaterialInterval interval(GameTestHelper h,long materialSerial) {
        var identity=new GeometryProvider.GeometryIdentity(Level.OVERWORLD,UUID.randomUUID(),1,UUID.randomUUID(),1,
            Identifier.parse("test:dispatcher"),Identifier.parse("test:static"),1,1);
        var inputs=new PoseProvider.Inputs(0,0,0,0,0,true);
        var beforeRoot=root(0,0,0);var afterRoot=root(1,1,.2);
        var beforeSample=new AnatomyPoseHistory.Sample(inputs,beforeRoot.origin(),beforeRoot.yaw(),beforeRoot.scale(),beforeRoot.gravity());
        var afterSample=new AnatomyPoseHistory.Sample(inputs,afterRoot.origin(),afterRoot.yaw(),afterRoot.scale(),afterRoot.gravity());
        var snapshot=new GeometryProvider.Snapshot(1,Map.of("piece",ConvexBox.of(new AABB(0,0,0,1,1,1),new Matrix4f())));
        var before=new GeometryProvider.QueryFrame(identity,new GeometryProvider.CausalEndpoint(1,0,0,beforeRoot,beforeSample,GeometryProvider.Availability.AVAILABLE),snapshot);
        var after=new GeometryProvider.QueryFrame(identity,new GeometryProvider.CausalEndpoint(2,1,0,afterRoot,afterSample,GeometryProvider.Availability.AVAILABLE),snapshot);
        return new MaterialEventDispatcher.MaterialInterval(new GeometryProvider.MotionIntervalHandle(identity,materialSerial,before,after),new AABB(-1,-1,-1,2,2,2));
    }
    private static final class FakeBackend implements MaterialEventDispatcher.Backend<String> {
        List<MaterialEventDispatcher.Candidate<String>> next=List.of();
        final List<String> calls=new ArrayList<>(),quarantines=new ArrayList<>();
        MaterialEventDispatcher<String> dispatcher;
        MaterialEventDispatcher.MaterialInterval interval;
        boolean overflow,reenter,cycle,startingOutside;
        @Override public MaterialEventDispatcher.Candidates<String> capture(MaterialEventDispatcher.Event<String> event,int maximumBodies) {
            calls.add("capture:"+event.support());return new MaterialEventDispatcher.Candidates<>(next,overflow);
        }
        @Override public MaterialEventDispatcher.Candidates<String> captureJointBatch(List<MaterialEventDispatcher.Event<String>> events,int maximumBodies) {
            calls.add("batch-capture:"+events.stream().map(MaterialEventDispatcher.Event::support).toList());return new MaterialEventDispatcher.Candidates<>(next,overflow);
        }
        @Override public MaterialEventDispatcher.Resolution<String> resolve(MaterialEventDispatcher.Event<String> event,List<MaterialEventDispatcher.Candidate<String>> candidates) {
            calls.add("resolve:"+event.support()+":"+candidates.stream().map(MaterialEventDispatcher.Candidate::body).toList());
            if(reenter) {
                reenter=false;
                var nested=dispatcher.ingest("nested",MaterialEventDispatcher.Source.ROOT_MUTATION,interval,this);
                calls.add("nested:"+nested.status()+":"+nested.reason());
            }
            if(startingOutside)return new MaterialEventDispatcher.Resolution<>(MaterialEventDispatcher.Outcome.released(0,MaterialEventDispatcher.Reason.STARTING_OUTSIDE),List.of());
            if(cycle)return new MaterialEventDispatcher.Resolution<>(MaterialEventDispatcher.Outcome.applied(.5),List.of(new MaterialEventDispatcher.DerivedCarry<>(event.id(),event.support(),interval)));
            if(event.support().equals("parent"))return new MaterialEventDispatcher.Resolution<>(MaterialEventDispatcher.Outcome.applied(.5),List.of(new MaterialEventDispatcher.DerivedCarry<>(event.id(),"child",interval)));
            return new MaterialEventDispatcher.Resolution<>(MaterialEventDispatcher.Outcome.applied(.5),List.of());
        }
        @Override public MaterialEventDispatcher.BatchResolution<String> resolveJointBatch(List<MaterialEventDispatcher.Event<String>> events,List<MaterialEventDispatcher.Candidate<String>> candidates) {
            calls.add("batch-resolve:"+events.stream().map(MaterialEventDispatcher.Event::support).toList()+":"+candidates.stream().map(MaterialEventDispatcher.Candidate::body).toList());
            return new MaterialEventDispatcher.BatchResolution<>(events.stream().map(ignored->MaterialEventDispatcher.Outcome.applied(.5)).toList(),List.of());
        }
        @Override public void quarantine(MaterialEventDispatcher.Event<String> event,MaterialEventDispatcher.Reason reason) {quarantines.add(event.support()+":"+reason);}
    }
    @GameTest public void rootMutationScopesAndCausalBodyEvents(GameTestHelper h) {
        var roots=new RootEventDispatcher();
        var outer=roots.begin("support",RootEventDispatcher.Source.ENTITY_MOVE,root(0,0,0));
        var nested=roots.begin("support",RootEventDispatcher.Source.DIRECT_SET_POS,root(0,0,0));
        h.assertTrue(roots.finish(nested,root(1,1,.2)).isEmpty(),"Nested setPos cannot manufacture a second material root segment");
        var mutation=roots.finish(outer,root(1,1,.2)).orElseThrow();
        h.assertTrue(mutation.source()==RootEventDispatcher.Source.ENTITY_MOVE && mutation.before().sequence()==0 && mutation.after().sequence()==1 && !roots.scoped(),"Only the completed outer Entity.move publishes its exact before/after root frames");
        var carryScope=roots.begin("derived",RootEventDispatcher.Source.DISPATCH_APPLY,root(0,0,0));
        h.assertTrue(roots.finish(carryScope,root(1,1,.2)).isEmpty(),"Dispatcher setPos stays scope-silent; only its confirmed parent may enqueue DERIVED_CARRY");

        var interval=interval(h,1);var backend=new FakeBackend();var dispatcher=new MaterialEventDispatcher<String>(2,3,key->key);
        backend.dispatcher=dispatcher;backend.interval=interval;
        // Timeline A: support E1 drains now; a body entering later cannot cause an E1 replay.
        h.assertTrue(dispatcher.ingest("support-a",MaterialEventDispatcher.Source.ROOT_MUTATION,interval,backend).equals(MaterialEventDispatcher.Outcome.applied(.5)),"First root event resolves synchronously");
        backend.next=List.of(new MaterialEventDispatcher.Candidate<>("late-body",new AABB(0,0,0,1,1,1)));
        h.assertTrue(backend.calls.stream().noneMatch(call->call.contains("late-body")),"A later body entry cannot be swept against an already drained interval");
        // Timeline B: body position is captured before the following support event is resolved.
        h.assertTrue(dispatcher.ingest("support-b",MaterialEventDispatcher.Source.ROOT_MUTATION,interval,backend).status()==MaterialEventDispatcher.Status.APPLIED_PREFIX,"Second root event applies a certified prefix");
        h.assertTrue(backend.calls.stream().anyMatch(call->call.equals("resolve:support-b:[late-body]")),"A support event after the body move captures that body once at its current AABB");
        // Timeline C: root events preserve actual ingestion order, not support identifiers.
        dispatcher.ingest("support-z",MaterialEventDispatcher.Source.ROOT_MUTATION,interval,backend);
        dispatcher.ingest("support-a",MaterialEventDispatcher.Source.ROOT_MUTATION,interval,backend);
        h.assertTrue(backend.calls.indexOf("resolve:support-z:[late-body]")<backend.calls.lastIndexOf("resolve:support-a:[late-body]"),"Interleaved support events retain real ingestion order rather than entity-id ordering");
        h.succeed();
    }
    @GameTest public void jointBatchesGateReentrancyAndQuarantineWholeEvents(GameTestHelper h) {
        var interval=interval(h,2);var backend=new FakeBackend();var dispatcher=new MaterialEventDispatcher<String>(2,2,key->key);
        backend.dispatcher=dispatcher;backend.interval=interval;backend.next=List.of(new MaterialEventDispatcher.Candidate<>("quiet",new AABB(0,0,0,1,1,1)));
        var batch=dispatcher.ingestJointBatch(List.of(new MaterialEventDispatcher.JointInput<>("joint-z",interval),new MaterialEventDispatcher.JointInput<>("joint-a",interval)),backend);
        h.assertTrue(batch.size()==2 && batch.stream().allMatch(outcome->outcome.status()==MaterialEventDispatcher.Status.APPLIED_PREFIX)
                && backend.calls.contains("batch-resolve:[joint-z, joint-a]:[quiet]"),"One 20 Hz batch captures all before/after inputs and resolves its simultaneous supports jointly without map ordering");
        backend.reenter=true;dispatcher.ingest("outer",MaterialEventDispatcher.Source.ROOT_MUTATION,interval,backend);
        h.assertTrue(backend.calls.stream().anyMatch(call->call.equals("nested:QUARANTINED:GATE_VIOLATION"))
                && backend.quarantines.contains("nested:GATE_VIOLATION")
                && backend.calls.stream().noneMatch(call->call.equals("resolve:nested:[quiet]")),"Unexpected reentrant gameplay query is quarantined immediately and never deferred past the drain");
        dispatcher.ingest("parent",MaterialEventDispatcher.Source.ROOT_MUTATION,interval,backend);
        h.assertTrue(backend.calls.indexOf("resolve:parent:[quiet]")<backend.calls.indexOf("resolve:child:[quiet]"),"A confirmed carry enqueues a derived support event only after its parent");
        backend.cycle=true;dispatcher.ingest("cycle",MaterialEventDispatcher.Source.ROOT_MUTATION,interval,backend);
        h.assertTrue(backend.quarantines.contains("cycle:CYCLE"),"A carry ancestry cycle quarantines instead of recursively resweeping a support");
        backend.cycle=false;backend.overflow=true;
        var overflow=dispatcher.ingest("overflow",MaterialEventDispatcher.Source.ROOT_MUTATION,interval,backend);
        h.assertTrue(overflow.status()==MaterialEventDispatcher.Status.QUARANTINED && overflow.reason()==MaterialEventDispatcher.Reason.CANDIDATE_LIMIT
                && backend.quarantines.contains("overflow:CANDIDATE_LIMIT"),"Candidate overflow quarantines the whole material event rather than applying a partial prefix");
        backend.overflow=false;backend.startingOutside=true;
        var outside=dispatcher.ingest("border",MaterialEventDispatcher.Source.ROOT_MUTATION,interval,backend);
        h.assertTrue(outside.status()==MaterialEventDispatcher.Status.RELEASED && outside.reason()==MaterialEventDispatcher.Reason.STARTING_OUTSIDE,"World-border starting-outside is an explicit obstacle result, never a synthetic clear or envelope rejection");
        h.succeed();
    }
}
