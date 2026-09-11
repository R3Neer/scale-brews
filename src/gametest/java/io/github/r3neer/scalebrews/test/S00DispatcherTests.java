package io.github.r3neer.scalebrews.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import io.github.r3neer.scalebrews.collision.internal.MaterialEventDispatcher;
import io.github.r3neer.scalebrews.collision.internal.MaterialEventDispatcher.*;
import net.minecraft.world.phys.AABB;
import java.util.*;
import java.util.function.*;

/** S00 fault-injection checks of the real scheduler with real interval DTOs. */
public final class S00DispatcherTests {
    private static final AABB BOX=new AABB(0,0,0,1,1,1);
    private static final MaterialInterval INTERVAL=S00Fixtures.interval();
    private static final class Backend implements MaterialEventDispatcher.Backend<String> {
        final List<String> captured=new ArrayList<>(), resolved=new ArrayList<>(), quarantined=new ArrayList<>();
        final List<List<String>> batches=new ArrayList<>();
        Consumer<Event<String>> captureHook=e->{};
        BiConsumer<Event<String>,Reason> quarantineHook=(e,r)->{};
        Function<Event<String>,Resolution<String>> resolution=e->new Resolution<>(Outcome.applied(.5),List.of());
        List<Candidate<String>> candidates=List.of(new Candidate<>("body",BOX));
        boolean overflow;
        public Candidates<String> capture(Event<String> e,int maximumBodies) {
            captured.add(e.support());captureHook.accept(e);return new Candidates<>(candidates,overflow);
        }
        public Candidates<String> captureJointBatch(List<Event<String>> events,int maximumBodies) {
            batches.add(events.stream().map(Event::support).toList());
            for(var e:events)captureHook.accept(e);
            return new Candidates<>(candidates,overflow);
        }
        public Resolution<String> resolve(Event<String> e,List<Candidate<String>> candidates) {
            resolved.add(e.support());return resolution.apply(e);
        }
        public BatchResolution<String> resolveJointBatch(List<Event<String>> events,List<Candidate<String>> candidates) {
            List<Outcome> outcomes=new ArrayList<>();List<DerivedCarry<String>> derived=new ArrayList<>();
            for(var e:events){var r=resolve(e,candidates);outcomes.add(r.outcome());derived.addAll(r.derived());}
            return new BatchResolution<>(outcomes,derived);
        }
        public void quarantine(Event<String> e,Reason reason) {
            quarantined.add(e.support()+":"+reason);quarantineHook.accept(e,reason);
        }
    }
    private static MaterialEventDispatcher<String> dispatcher(){return new MaterialEventDispatcher<>(3,5,32,s->s);}
    private static Outcome ingest(MaterialEventDispatcher<String> d,String support,Backend b) {
        return d.ingest(support,Source.ROOT_MUTATION,INTERVAL,b);
    }
    private static List<DerivedCarry<String>> children(Event<String> parent,String... names) {
        return Arrays.stream(names).map(n->new DerivedCarry<>(parent.id(),n,INTERVAL)).toList();
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static Throwable thrown(Runnable body) {
        try{body.run();}catch(Throwable failure){return failure;}
        throw new AssertionError("Expected the backend's failure to propagate");
    }
    private static Backend failingTree(RuntimeException primary,RuntimeException cleanup) {
        var b=new Backend();
        b.resolution=e->new Resolution<>(Outcome.applied(.5),e.support().equals("parent")?children(e,"fail","cleanup","remaining"):List.of());
        b.captureHook=e->{if(e.support().equals("fail"))throw primary;};
        b.quarantineHook=(e,r)->{if(e.support().equals("cleanup"))throw cleanup;};
        return b;
    }
    @GameTest public void abortNoHiddenWork(GameTestHelper h) {
        var d=dispatcher();var b=failingTree(new IllegalArgumentException("capture failed"),new IllegalStateException("cleanup failed"));
        thrown(()->ingest(d,"parent",b));
        var unrelated=new Backend();ingest(d,"unrelated",unrelated);
        check(unrelated.resolved.equals(List.of("unrelated")),"Old work reached a DIFFERENT backend: "+unrelated.resolved);
        h.succeed();
    }
    @GameTest public void abortPrimaryCause(GameTestHelper h) {
        var d=dispatcher();var primary=new IllegalArgumentException("capture failed");var cleanup=new IllegalStateException("cleanup failed");
        var b=failingTree(primary,cleanup);var failure=thrown(()->ingest(d,"parent",b));
        check(failure==primary,"Cleanup replaced the original failure: "+failure);
        check(failure.getSuppressed().length==1 && failure.getSuppressed()[0].getCause()==cleanup,"Cleanup failure must remain inspectable as suppressed evidence");
        h.succeed();
    }
    @GameTest public void abortNotifiesAllPending(GameTestHelper h) {
        var d=dispatcher();var b=failingTree(new IllegalArgumentException("capture failed"),new IllegalStateException("cleanup failed"));
        thrown(()->ingest(d,"parent",b));
        check(b.quarantined.equals(List.of("cleanup:BACKEND_FAILURE","remaining:BACKEND_FAILURE")),"Cleanup stopped at first callback failure: "+b.quarantined);
        h.succeed();
    }
    @GameTest public void callbackSingleReentryOutsideDrain(GameTestHelper h) {
        var d=new MaterialEventDispatcher<String>(3,5,1,s->s);var b=new Backend();var nested=new Backend();var outcomes=new ArrayList<Outcome>();
        b.quarantineHook=(e,r)->outcomes.add(ingest(d,"nested",nested));
        d.ingestJointBatch(List.of(new JointInput<>("a",INTERVAL),new JointInput<>("b",INTERVAL)),b);
        check(nested.resolved.isEmpty(),"Quarantine admitted nested material resolution: "+nested.resolved);
        check(outcomes.size()==2 && outcomes.stream().allMatch(o->o.status()==Status.QUARANTINED && o.reason()==Reason.GATE_VIOLATION),"Every nested caller must see an explicit rejection");
        check(nested.quarantined.isEmpty(),"A nested rejection recursively notified another backend");
        h.succeed();
    }
    @GameTest public void callbackJointReentryOutsideDrain(GameTestHelper h) {
        var d=new MaterialEventDispatcher<String>(3,5,1,s->s);var b=new Backend();var nested=new Backend();var outcomes=new ArrayList<Outcome>();
        b.quarantineHook=(e,r)->outcomes.addAll(d.ingestJointBatch(List.of(new JointInput<>("nested",INTERVAL)),nested));
        d.ingestJointBatch(List.of(new JointInput<>("a",INTERVAL),new JointInput<>("b",INTERVAL)),b);
        check(nested.resolved.isEmpty() && nested.batches.isEmpty(),"Quarantine admitted nested joint work: "+nested.resolved);
        check(outcomes.stream().allMatch(o->o.reason()==Reason.GATE_VIOLATION),"Nested joint must be gated");
        h.succeed();
    }
    @GameTest public void callbackNoRecursiveNotification(GameTestHelper h) {
        var d=dispatcher();var b=new Backend();var once=new boolean[1];var results=new ArrayList<Outcome>();
        b.resolution=e->{if(e.support().equals("outer"))results.add(ingest(d,"nested",b));return new Resolution<>(Outcome.applied(.5),List.of());};
        b.quarantineHook=(e,r)->{if(!once[0]){once[0]=true;results.add(ingest(d,"callback-nested",b));}};
        ingest(d,"outer",b);
        check(b.resolved.equals(List.of("outer")),"Reentrant work resolved");
        check(b.quarantined.size()==1,"Quarantine recursively called itself: "+b.quarantined);
        check(results.size()==2 && results.stream().allMatch(o->o.reason()==Reason.GATE_VIOLATION),"Both rejected operations need explicit outcomes");
        h.succeed();
    }
    @GameTest public void abortFatalFailureDoesNotLeakQueue(GameTestHelper h) {
        var d=dispatcher();var b=new Backend();var fatal=new AssertionError("simulated fatal capture");
        b.resolution=e->new Resolution<>(Outcome.applied(.5),e.support().equals("parent")?children(e,"fatal","remaining"):List.of());
        b.captureHook=e->{if(e.support().equals("fatal"))throw fatal;};
        check(thrown(()->ingest(d,"parent",b))==fatal,"Fatal failure must not be swallowed");
        var next=new Backend();ingest(d,"unrelated",next);
        check(next.resolved.equals(List.of("unrelated")),"A fatal abort left delayed work: "+next.resolved);
        h.succeed();
    }
    @GameTest public void regressionIngestionAndDerivedOrder(GameTestHelper h) {
        var d=dispatcher();var b=new Backend();b.resolution=e->new Resolution<>(Outcome.applied(.5),e.support().equals("p")?children(e,"c"):List.of());
        ingest(d,"z",b);ingest(d,"p",b);ingest(d,"a",b);
        check(b.resolved.equals(List.of("z","p","c","a")),"Successful causal order changed: "+b.resolved);
        h.succeed();
    }
    @GameTest public void regressionSimultaneousGroup(GameTestHelper h) {
        var d=dispatcher();var b=new Backend();
        var outcomes=d.ingestJointBatch(List.of(new JointInput<>("z",INTERVAL),new JointInput<>("a",INTERVAL)),b);
        check(b.batches.equals(List.of(List.of("z","a"))) && outcomes.size()==2 && outcomes.stream().allMatch(o->o.equals(Outcome.applied(.5))),"Joint group was split or reordered");
        h.succeed();
    }
    @GameTest public void regressionCandidateBudgetWholeEvent(GameTestHelper h) {
        var d=new MaterialEventDispatcher<String>(1,3,8,s->s);var b=new Backend();b.candidates=List.of(new Candidate<>("a",BOX),new Candidate<>("b",BOX));
        var o=ingest(d,"support",b);check(o.reason()==Reason.CANDIDATE_LIMIT && b.resolved.isEmpty(),"Partial candidate prefix was admitted");
        h.succeed();
    }
    @GameTest public void regressionCyclePreservesAppliedPrefix(GameTestHelper h) {
        var d=dispatcher();var b=new Backend();b.resolution=e->new Resolution<>(Outcome.applied(.25),children(e,e.support()));
        var o=ingest(d,"a",b);check(o.status()==Status.APPLIED_PREFIX && o.safeFraction()==.25 && o.reason()==Reason.CYCLE && b.resolved.equals(List.of("a")),"Cycle changed truthful applied prefix or resolved twice");
        h.succeed();
    }
    @GameTest public void regressionDepthAndEventBudgets(GameTestHelper h) {
        for(int budget=1;budget<=5;budget++){
            var d=new MaterialEventDispatcher<String>(3,budget,32,s->s);var b=new Backend();
            b.resolution=e->new Resolution<>(Outcome.applied(.5),children(e,e.support()+"x"));
            ingest(d,"a",b);check(b.resolved.size()==budget && b.quarantined.stream().anyMatch(s->s.endsWith(":DEPTH_LIMIT")),"Depth budget not enforced at "+budget);
            var q=new MaterialEventDispatcher<String>(3,32,budget,s->s);var c=new Backend();c.resolution=b.resolution;
            ingest(q,"a",c);check(c.resolved.size()==budget && c.quarantined.stream().anyMatch(s->s.endsWith(":QUEUE_LIMIT")),"Event budget not enforced at "+budget);
        }
        h.succeed();
    }
    @GameTest public void regressionInvalidDerivationNoChild(GameTestHelper h) {
        var d=dispatcher();var b=new Backend();b.resolution=e->new Resolution<>(Outcome.applied(.25),List.of(new DerivedCarry<>(new EventId(999,0),"child",INTERVAL)));
        var o=ingest(d,"a",b);check(o.safeFraction()==.25 && o.reason()==Reason.INVALID_DERIVATION && b.resolved.equals(List.of("a")),"Invalid derivation was admitted or prefix lost");
        h.succeed();
    }
    @GameTest public void jointBatchInvalidParentMustMarkReturnedOutcomes(GameTestHelper h) {
        var d=dispatcher();var b=new Backend();
        b.resolution=e->new Resolution<>(Outcome.applied(.25),e.support().equals("a")?children(e,"valid-before-orphan"):
            List.of(new DerivedCarry<>(new EventId(999,0),"orphan",INTERVAL)));
        var outcomes=d.ingestJointBatch(List.of(new JointInput<>("a",INTERVAL),new JointInput<>("b",INTERVAL)),b);
        check(outcomes.size()==2 && outcomes.stream().allMatch(o->o.status()==Status.APPLIED_PREFIX && o.safeFraction()==.25 && o.reason()==Reason.INVALID_DERIVATION),
            "Joint invalid derivation notification disagreed with returned applied-prefix outcomes: "+outcomes);
        check(b.resolved.equals(List.of("a","b")),"A derived carry escaped a malformed simultaneous result: "+b.resolved);
        h.succeed();
    }
    @GameTest public void regressionReleasedParentNoChild(GameTestHelper h) {
        var d=dispatcher();var b=new Backend();b.resolution=e->new Resolution<>(Outcome.released(.25,Reason.STARTING_OUTSIDE),children(e,"child"));
        var o=ingest(d,"a",b);check(o.status()==Status.RELEASED && o.safeFraction()==.25 && b.resolved.equals(List.of("a")),"A released parent admitted derived work");
        h.succeed();
    }
    @GameTest public void propertyCleanupFailureSubsetsAndReentry(GameTestHelper h) {
        int scenarios=0;
        for(int failureMask=0;failureMask<8;failureMask++)for(boolean joint:List.of(false,true))for(boolean reenter:List.of(false,true)) {
            final int mask=failureMask;var d=dispatcher();var b=new Backend();var nested=new Backend();
            var primary=new IllegalArgumentException("capture failed before mutation");
            b.resolution=e->new Resolution<>(Outcome.applied(.5),e.support().equals("parent")?children(e,"fail","q0","q1","q2"):List.of());
            b.captureHook=e->{if(e.support().equals("fail"))throw primary;};
            b.quarantineHook=(e,r)->{
                if(reenter) {
                    check(ingest(d,"nested",nested).reason()==Reason.GATE_VIOLATION,"Single reentry during abort must fail closed");
                    check(d.ingestJointBatch(List.of(new JointInput<>("nested-joint",INTERVAL)),nested).getFirst().reason()==Reason.GATE_VIOLATION,"Joint reentry during abort must fail closed");
                }
                int index=Integer.parseInt(e.support().substring(1));
                if((mask&(1<<index))!=0)throw new IllegalStateException("cleanup "+index);
            };
            var failure=thrown(()->{
                if(joint)d.ingestJointBatch(List.of(new JointInput<>("parent",INTERVAL)),b);
                else ingest(d,"parent",b);
            });
            check(failure==primary && failure.getSuppressed().length==Integer.bitCount(mask),"Primary/suppressed evidence mismatch for mask "+mask);
            check(b.quarantined.equals(List.of("q0:BACKEND_FAILURE","q1:BACKEND_FAILURE","q2:BACKEND_FAILURE")),"Pending notification loss at mask "+mask);
            check(nested.resolved.isEmpty() && nested.quarantined.isEmpty(),"Reentrant callback executed or recursively notified work");
            var next=new Backend();ingest(d,"unrelated",next);
            check(next.resolved.equals(List.of("unrelated")),"Failure mask leaked work into next call");scenarios++;
        }
        check(scenarios==32,"All cleanup subsets and ingestion modes must execute");
        h.succeed();
    }
    @GameTest public void holdoutH04ScopedChainCallbackFailure(GameTestHelper h) {
        var d=dispatcher();var b=new Backend();var nested=new Backend();var primary=new IllegalArgumentException("C capture failed");
        b.resolution=e->{
            if(e.support().equals("A")) {
                check(ingest(d,"reentrant-A",nested).reason()==Reason.GATE_VIOLATION,"Application callback admitted reentry");
                return new Resolution<>(Outcome.applied(.5),children(e,"B"));
            }
            return new Resolution<>(Outcome.applied(.5),e.support().equals("B")?children(e,"C","pending"):List.of());
        };
        b.captureHook=e->{if(e.support().equals("C"))throw primary;};
        b.quarantineHook=(e,r)->{throw new IllegalStateException("quarantine failed");};
        check(thrown(()->ingest(d,"A",b))==primary,"Chain lost its primary failure");
        check(b.resolved.equals(List.of("A","B")) && nested.resolved.isEmpty(),"Unexpected application in failed chain");
        var next=new Backend();ingest(d,"external",next);
        check(next.resolved.equals(List.of("external")),"Chain contaminated an external pair");
        // No assertion of exact-once replay of A's handle: the live causal ledger
        // is absent from this isolated harness. Full H04 remains OPEN.
        h.succeed();
    }
    @GameTest public void callbackFailureRestoresAdmissionGate(GameTestHelper h) {
        var d=new MaterialEventDispatcher<String>(3,5,1,s->s);var b=new Backend();
        b.quarantineHook=(e,r)->{throw new IllegalStateException("quarantine failed outside drain");};
        thrown(()->d.ingestJointBatch(List.of(new JointInput<>("a",INTERVAL),new JointInput<>("b",INTERVAL)),b));
        var next=new Backend();var outcome=ingest(d,"unrelated",next);
        check(outcome.status()==Status.APPLIED_PREFIX && next.resolved.equals(List.of("unrelated")),"Quarantine flag remained stuck after callback failure");
        h.succeed();
    }
    @GameTest public void regressionRepeatedIndependentCalls(GameTestHelper h) {
        var d=dispatcher();var b=new Backend();
        for(int i=0;i<256;i++)check(ingest(d,"e"+i,b).equals(Outcome.applied(.5)),"Unexpected independent call outcome");
        check(b.resolved.size()==256 && new HashSet<>(b.resolved).size()==256,"Unexpected hidden extra resolution");
        h.succeed();
    }
    @GameTest public void regressionEmptyBatchNoWork(GameTestHelper h) {
        var d=dispatcher();var b=new Backend();check(d.ingestJointBatch(List.of(),b).isEmpty() && b.batches.isEmpty(),"Empty batch caused backend work");
        h.succeed();
    }
}
