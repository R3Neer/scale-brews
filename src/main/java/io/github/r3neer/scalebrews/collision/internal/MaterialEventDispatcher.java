package io.github.r3neer.scalebrews.collision.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.world.phys.AABB;

/**
 * Causal, synchronous material-event scheduler.  It owns no entity hooks, world
 * query, block collection, receipt, or protocol state: those arrive through the
 * narrow backend ports and are exercised with a fake backend first.
 */
public final class MaterialEventDispatcher<E> {
    public enum Source {ROOT_MUTATION,JOINT_BATCH,DERIVED_CARRY}
    public enum Status {APPLIED_PREFIX,RELEASED,QUARANTINED}
    public enum Reason {NONE,CANDIDATE_LIMIT,ENVELOPE_LIMIT,QUEUE_LIMIT,GATE_VIOLATION,CYCLE,DEPTH_LIMIT,BACKEND_EXHAUSTED,STARTING_OUTSIDE,BACKEND_FAILURE,INVALID_DERIVATION}
    public record EventId(long sequence,int simultaneousMember) {
        public EventId {if(sequence<1 || simultaneousMember<0)throw new IllegalArgumentException("Invalid material event identity");}
    }
    /** Exact geometry interval and a finite conservative envelope, not an endpoint chord. */
    public record MaterialInterval(GeometryProvider.MotionIntervalHandle handle,AABB envelope) {
        public MaterialInterval {
            if(handle==null || !finite(envelope))throw new IllegalArgumentException("Invalid material interval");
        }
    }
    public record Event<E>(EventId id,E support,Source source,MaterialInterval interval,Set<Object> ancestry) {
        public Event {
            if(id==null || support==null || source==null || interval==null || ancestry==null)throw new IllegalArgumentException("Invalid material event");
            ancestry=Set.copyOf(ancestry);
        }
    }
    /** Identity and AABB are captured synchronously before backend resolution mutates anything. */
    public record Candidate<E>(E body,AABB bounds) {
        public Candidate {if(body==null || !finite(bounds))throw new IllegalArgumentException("Invalid material candidate");}
    }
    public record Candidates<E>(List<Candidate<E>> bodies,boolean overflow) {
        public Candidates {bodies=List.copyOf(bodies);}
    }
    /** Applied prefixes are the only outcomes eligible for a future receipt. */
    public record Outcome(Status status,double safeFraction,Reason reason) {
        public Outcome {
            if(status==null || reason==null || !Double.isFinite(safeFraction) || safeFraction<0 || safeFraction>1)throw new IllegalArgumentException("Invalid material outcome");
            if(status==Status.QUARANTINED && reason==Reason.NONE)throw new IllegalArgumentException("Quarantine needs a reason");
            if(status==Status.QUARANTINED && safeFraction!=0)throw new IllegalArgumentException("Quarantine cannot claim an applied prefix");
        }
        public static Outcome applied(double safeFraction){return new Outcome(Status.APPLIED_PREFIX,safeFraction,Reason.NONE);}
        /** A prefix was actually applied, but a later derivative or publication was quarantined. */
        public static Outcome applied(double safeFraction,Reason reason){return new Outcome(Status.APPLIED_PREFIX,safeFraction,reason);}
        public static Outcome released(double safeFraction,Reason reason){return new Outcome(Status.RELEASED,safeFraction,reason);}
        public static Outcome quarantined(Reason reason){return new Outcome(Status.QUARANTINED,0,reason);}
    }
    /** Backend may create this only after its named parent's carry setPos has completed. */
    public record DerivedCarry<E>(EventId parent,E support,MaterialInterval interval) {
        public DerivedCarry {if(parent==null || support==null || interval==null)throw new IllegalArgumentException("Invalid derived carry");}
    }
    /** Simultaneous provider-tick input; list order is registration capture order, never a causal order. */
    public record JointInput<E>(E support,MaterialInterval interval) {
        public JointInput {if(support==null || interval==null)throw new IllegalArgumentException("Invalid joint input");}
    }
    public record Resolution<E>(Outcome outcome,List<DerivedCarry<E>> derived) {
        public Resolution {if(outcome==null || derived==null)throw new IllegalArgumentException("Invalid material resolution");derived=List.copyOf(derived);}
    }
    public record BatchResolution<E>(List<Outcome> outcomes,List<DerivedCarry<E>> derived) {
        public BatchResolution {outcomes=List.copyOf(outcomes);derived=List.copyOf(derived);}
    }
    /** World adapters implement spatial capture and certified path resolution; no partial candidate prefix is legal. */
    public interface Backend<E> {
        Candidates<E> capture(Event<E> event,int maximumBodies);
        Candidates<E> captureJointBatch(List<Event<E>> events,int maximumBodies);
        Resolution<E> resolve(Event<E> event,List<Candidate<E>> candidates);
        BatchResolution<E> resolveJointBatch(List<Event<E>> events,List<Candidate<E>> candidates);
        void quarantine(Event<E> event,Reason reason);
    }
    private sealed interface Pending<E> permits One,Joint {}
    private record One<E>(Event<E> event) implements Pending<E> {}
    private record Joint<E>(List<Event<E>> events) implements Pending<E> {private Joint {events=List.copyOf(events);}}

    private final int maximumBodies,maximumDepth,maximumEvents;
    private final Function<E,Object> identity;
    private final Deque<Pending<E>> queue=new ArrayDeque<>();
    private boolean draining;
    private int admittedEvents;
    private long sequence;

    public MaterialEventDispatcher(int maximumBodies,int maximumDepth,int maximumEvents,Function<E,Object> identity) {
        if(maximumBodies<1 || maximumDepth<1 || maximumEvents<1 || identity==null)throw new IllegalArgumentException("Invalid material dispatcher limits");
        this.maximumBodies=maximumBodies;this.maximumDepth=maximumDepth;this.maximumEvents=maximumEvents;this.identity=identity;
    }
    public Outcome ingest(E support,Source source,MaterialInterval interval,Backend<E> backend) {
        return submit(newEvent(support,source,interval,Set.of(),0),backend);
    }
    /** One provider cadence is an explicitly simultaneous group, never an IdentityHashMap ordering. */
    public List<Outcome> ingestJointBatch(List<JointInput<E>> changed,Backend<E> backend) {
        Objects.requireNonNull(changed);Objects.requireNonNull(backend);
        if(changed.isEmpty())return List.of();
        long batch=nextSequence();var events=new ArrayList<Event<E>>();
        for(int index=0;index<changed.size();index++) {
            var event=changed.get(index);Object key=key(event.support());events.add(new Event<>(new EventId(batch,index),event.support(),Source.JOINT_BATCH,event.interval(),Set.of(key)));
        }
        if(draining) {
            var outcomes=new ArrayList<Outcome>();for(var event:events)outcomes.add(quarantine(event,Reason.GATE_VIOLATION,backend));return List.copyOf(outcomes);
        }
        if(events.size()>maximumEvents) {
            var outcomes=new ArrayList<Outcome>();for(var event:events)outcomes.add(quarantine(event,Reason.QUEUE_LIMIT,backend));return List.copyOf(outcomes);
        }
        queue.addLast(new Joint<>(events));return drainUntil(events,backend);
    }
    private Outcome submit(Event<E> event,Backend<E> backend) {
        Objects.requireNonNull(backend);
        if(draining)return quarantine(event,Reason.GATE_VIOLATION,backend);
        queue.addLast(new One<>(event));return drainUntil(List.of(event),backend).getFirst();
    }
    private List<Outcome> drainUntil(List<Event<E>> requested,Backend<E> backend) {
        var results=new java.util.HashMap<EventId,Outcome>();draining=true;admittedEvents=countQueuedEvents();
        try {
            while(!queue.isEmpty()) {
                var pending=queue.removeFirst();
                if(pending instanceof One<?> raw) {
                    @SuppressWarnings("unchecked") var one=(One<E>)raw;
                    resolveOne(one.event(),backend,results);
                } else {
                    @SuppressWarnings("unchecked") var joint=(Joint<E>)pending;
                    resolveJoint(joint.events(),backend,results);
                }
            }
        } catch(RuntimeException failure) {
            abortQueued(backend);throw failure;
        } finally {draining=false;admittedEvents=0;}
        var outcomes=new ArrayList<Outcome>();for(var event:requested)outcomes.add(results.getOrDefault(event.id(),Outcome.quarantined(Reason.BACKEND_FAILURE)));return List.copyOf(outcomes);
    }
    private void resolveOne(Event<E> event,Backend<E> backend,java.util.Map<EventId,Outcome> results) {
        try {
            var captured=backend.capture(event,maximumBodies);
            if(captured==null || captured.overflow() || captured.bodies().size()>maximumBodies) {results.put(event.id(),quarantine(event,Reason.CANDIDATE_LIMIT,backend));return;}
            var resolution=backend.resolve(event,captured.bodies());
            if(resolution==null) {results.put(event.id(),quarantine(event,Reason.BACKEND_FAILURE,backend));return;}
            results.put(event.id(),enqueueDerived(event,resolution,backend));
        } catch(BackendCaptureFailure failure) {results.put(event.id(),quarantine(event,Reason.BACKEND_FAILURE,backend));}
    }
    private void resolveJoint(List<Event<E>> events,Backend<E> backend,java.util.Map<EventId,Outcome> results) {
        try {
            var captured=backend.captureJointBatch(events,maximumBodies);
            if(captured==null || captured.overflow() || captured.bodies().size()>maximumBodies) {for(var event:events)results.put(event.id(),quarantine(event,Reason.CANDIDATE_LIMIT,backend));return;}
            var resolution=backend.resolveJointBatch(events,captured.bodies());
            if(resolution==null || resolution.outcomes().size()!=events.size()) {for(var event:events)results.put(event.id(),quarantine(event,Reason.BACKEND_FAILURE,backend));return;}
            for(int i=0;i<events.size();i++)results.put(events.get(i).id(),resolution.outcomes().get(i));
            for(var carry:resolution.derived()) {
                var parent=events.stream().filter(event->event.id().equals(carry.parent())).findFirst().orElse(null);
                if(parent==null) {for(var event:events)quarantine(event,Reason.INVALID_DERIVATION,backend);}
                else results.put(parent.id(),enqueueDerived(parent,new Resolution<>(results.get(parent.id()),List.of(carry)),backend));
            }
        } catch(BackendCaptureFailure failure) {for(var event:events)results.put(event.id(),quarantine(event,Reason.BACKEND_FAILURE,backend));}
    }
    private Outcome enqueueDerived(Event<E> parent,Resolution<E> resolution,Backend<E> backend) {
        if(resolution.outcome().status()!=Status.APPLIED_PREFIX) {
            for(var carry:resolution.derived())quarantineChild(parent,carry,Reason.INVALID_DERIVATION,backend);
            return resolution.outcome();
        }
        Reason issue=resolution.outcome().reason();
        for(var carry:resolution.derived()) {
            if(!carry.parent().equals(parent.id())) {quarantineChild(parent,carry,Reason.INVALID_DERIVATION,backend);issue=Reason.INVALID_DERIVATION;continue;}
            var ancestry=new HashSet<>(parent.ancestry());Object key=key(carry.support());
            if(ancestry.contains(key)) {quarantineChild(parent,carry,Reason.CYCLE,backend);issue=Reason.CYCLE;continue;}
            if(ancestry.size()>=maximumDepth) {quarantineChild(parent,carry,Reason.DEPTH_LIMIT,backend);issue=Reason.DEPTH_LIMIT;continue;}
            if(admittedEvents>=maximumEvents) {quarantineChild(parent,carry,Reason.QUEUE_LIMIT,backend);issue=Reason.QUEUE_LIMIT;continue;}
            ancestry.add(key);queue.addLast(new One<>(newEvent(carry.support(),Source.DERIVED_CARRY,carry.interval(),ancestry,0)));admittedEvents++;
        }
        return issue==Reason.NONE?resolution.outcome():Outcome.applied(resolution.outcome().safeFraction(),issue);
    }
    private Event<E> newEvent(E support,Source source,MaterialInterval interval,Set<Object> ancestry,int member) {
        Objects.requireNonNull(support);Objects.requireNonNull(source);Objects.requireNonNull(interval);
        Object key=key(support);var chain=ancestry.isEmpty()?Set.of(key):ancestry;
        return new Event<>(new EventId(nextSequence(),member),support,source,interval,chain);
    }
    private Outcome quarantine(Event<E> event,Reason reason,Backend<E> backend) {
        var outcome=Outcome.quarantined(reason);
        try {backend.quarantine(event,reason);} catch(RuntimeException failure) {throw new IllegalStateException("Material quarantine callback failed for "+event.id(),failure);}
        return outcome;
    }
    private void quarantineChild(Event<E> parent,DerivedCarry<E> carry,Reason reason,Backend<E> backend) {
        var child=new Event<>(new EventId(nextSequence(),0),carry.support(),Source.DERIVED_CARRY,carry.interval(),parent.ancestry());
        quarantine(child,reason,backend);
    }
    private int countQueuedEvents() {int total=0;for(var pending:queue)total+=pending instanceof One<?>?1:((Joint<?>)pending).events().size();return total;}
    private void abortQueued(Backend<E> backend) {
        while(!queue.isEmpty()) {
            var pending=queue.removeFirst();
            if(pending instanceof One<?> raw) {
                @SuppressWarnings("unchecked") var one=(One<E>)raw;quarantine(one.event(),Reason.BACKEND_FAILURE,backend);
            } else {
                @SuppressWarnings("unchecked") var joint=(Joint<E>)pending;for(var event:joint.events())quarantine(event,Reason.BACKEND_FAILURE,backend);
            }
        }
    }
    /** Capture is read-only. Resolve must return an Outcome even after applying a prefix; it must not throw post-mutation. */
    private static final class BackendCaptureFailure extends RuntimeException {private BackendCaptureFailure(RuntimeException cause){super(cause);}}
    private long nextSequence(){return sequence=Math.incrementExact(sequence);}
    private Object key(E entity){return Objects.requireNonNull(identity.apply(entity));}
    private static boolean finite(AABB box) {
        return box!=null && Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
            && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ)
            && box.minX<=box.maxX && box.minY<=box.maxY && box.minZ<=box.maxZ;
    }
}
