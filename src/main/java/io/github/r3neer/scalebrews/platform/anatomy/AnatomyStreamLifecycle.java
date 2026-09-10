package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;

/**
 * Pure N3 ordering/lifecycle helper. It is intentionally not a payload,
 * networking receiver, Runtime state or catalog integration yet.
 *
 * <p>One instance belongs to one server-recipient or one client connection and
 * is thread-confined. Server publication checks the current tracked stream
 * before allocating a publication sequence. Client reception consumes the
 * sequence before semantic checks, so rejected content cannot be retried under
 * the same sequence. A gap clears bounded state and fails closed.</p>
 */
public final class AnatomyStreamLifecycle {
    public enum StartReason { INITIAL, RETRACK, REBIND, DISCONTINUITY, DIMENSION_ENTER, CATALOG_REPLACED }
    public enum RetireReason { STOP_TRACKING, UNLOAD, BINDING_REPLACED, DIMENSION_LEAVE, CATALOG_REPLACED, DISCONTINUITY }
    public enum FrameKind { ROOT_ONLY, JOINT_UPDATE, AVAILABLE_STATIC, UNAVAILABLE }
    public enum ReceiveResult { ACCEPTED, STALE_CONNECTION, STALE_SEQUENCE, GAP_FAIL_CLOSED, REJECTED_SEMANTICS, REJECTED_CLOCK, REJECTED_CAPACITY, FAILED_CONNECTION }

    /** Full server identity; local provider registration is deliberately absent. */
    public record StreamIdentity(UUID serverEpoch,Identifier dimension,UUID support,int entityId,long catalogRevision,
            Identifier model,Identifier provider,long bindingGeneration) {
        public StreamIdentity {
            if(serverEpoch==null || dimension==null || support==null || entityId<0 || catalogRevision<0 || model==null || provider==null || bindingGeneration<1)
                throw new IllegalArgumentException("Invalid authoritative anatomy stream identity");
        }
        private SupportKey key(){return new SupportKey(dimension,support,entityId);}
    }
    private record SupportKey(Identifier dimension,UUID support,int entityId) {}

    public sealed interface Publication permits StreamStart,Frame,Retire {
        UUID serverEpoch();
        long publicationSequence();
        StreamIdentity identity();
        long supportStreamLife();
    }
    public record StreamStart(UUID serverEpoch,long publicationSequence,StreamIdentity identity,long supportStreamLife,
            long firstFrameSerial,StartReason reason) implements Publication {
        public StreamStart {
            if(serverEpoch==null || identity==null || !serverEpoch.equals(identity.serverEpoch()) || publicationSequence<1 || supportStreamLife<1 || firstFrameSerial<1 || reason==null)
                throw new IllegalArgumentException("Invalid anatomy stream start");
        }
    }
    public record Frame(UUID serverEpoch,long publicationSequence,StreamIdentity identity,long supportStreamLife,long frameSerial,
            long authorityTick,long jointSampleTick,long rootFrameSequence,long rootFrameTick,FrameKind kind,boolean available) implements Publication {
        public Frame {
            if(serverEpoch==null || identity==null || !serverEpoch.equals(identity.serverEpoch()) || publicationSequence<1 || supportStreamLife<1
                    || frameSerial<1 || authorityTick<0 || jointSampleTick<0 || rootFrameSequence<0 || rootFrameTick<0 || kind==null)
                throw new IllegalArgumentException("Invalid anatomy stream frame");
            if(available && kind==FrameKind.UNAVAILABLE)throw new IllegalArgumentException("Unavailable kind cannot publish geometry");
            if(!available && kind!=FrameKind.UNAVAILABLE)throw new IllegalArgumentException("Unavailable frame requires unavailable kind");
        }
    }
    public record Retire(UUID serverEpoch,long publicationSequence,StreamIdentity identity,long supportStreamLife,
            long lastFrameSerial,RetireReason reason) implements Publication {
        public Retire {
            if(serverEpoch==null || identity==null || !serverEpoch.equals(identity.serverEpoch()) || publicationSequence<1 || supportStreamLife<1 || lastFrameSerial<0 || reason==null)
                throw new IllegalArgumentException("Invalid anatomy stream retirement");
        }
    }

    /**
     * Server-thread confined publication allocator; no packet is sent here.
     * Runtime must validate the current world binding/tracking life before
     * invoking {@link #start}; a start is an authoritative transition, not a
     * callback that may revive an old support.
     */
    public static final class ServerGate {
        private final UUID serverEpoch;
        private final int maxActive;
        private final Map<SupportKey,ServerEntry> active=new LinkedHashMap<>();
        private Thread owner;
        private long nextPublication=1,nextLife=1,rejectedStale;
        private boolean closed;

        public ServerGate(UUID serverEpoch,int maxActive) {
            if(serverEpoch==null || maxActive<1)throw new IllegalArgumentException("Invalid server anatomy stream gate");
            this.serverEpoch=serverEpoch;this.maxActive=maxActive;
        }
        public Optional<StreamStart> start(StreamIdentity identity,long firstFrameSerial,StartReason reason) {
            confined();if(closed || !validStart(identity,firstFrameSerial,reason))return Optional.empty();var key=identity.key();
            if(active.containsKey(key) || active.size()>=maxActive)return Optional.empty();
            long life=nextLife,sequence=nextPublication;
            long followingLife=incrementOrClose(life,"support stream life"),followingPublication=incrementOrClose(sequence,"publication sequence");
            var start=new StreamStart(serverEpoch,sequence,identity,life,firstFrameSerial,reason);
            active.put(key,new ServerEntry(identity,life,firstFrameSerial-1,-1,-1,-1,-1));nextLife=followingLife;nextPublication=followingPublication;
            return Optional.of(start);
        }
        public Optional<Frame> frame(StreamIdentity identity,long life,long frameSerial,long authorityTick,long jointSampleTick,
                long rootFrameSequence,long rootFrameTick,FrameKind kind,boolean available) {
            confined();if(closed || !validFrameRequest(identity,life,frameSerial,authorityTick,jointSampleTick,rootFrameSequence,rootFrameTick,kind,available))return Optional.empty();var entry=active.get(identity.key());
            if(entry==null || entry.life!=life || !entry.identity.equals(identity)) {rejectStale();return Optional.empty();}
            if(!validFrame(entry,frameSerial,authorityTick,jointSampleTick,rootFrameSequence,rootFrameTick,kind,available))return Optional.empty();
            long sequence=nextPublication,followingPublication=incrementOrClose(sequence,"publication sequence");
            var frame=new Frame(serverEpoch,sequence,identity,life,frameSerial,authorityTick,jointSampleTick,rootFrameSequence,rootFrameTick,kind,available);
            entry.frameSerial=frameSerial;entry.authorityTick=authorityTick;entry.jointSampleTick=jointSampleTick;entry.rootFrameSequence=rootFrameSequence;entry.rootFrameTick=rootFrameTick;
            nextPublication=followingPublication;
            return Optional.of(frame);
        }
        public Optional<Retire> retire(StreamIdentity identity,long life,RetireReason reason) {
            confined();if(closed || !validRetire(identity,life,reason))return Optional.empty();var entry=active.get(identity.key());
            if(entry==null || entry.life!=life || !entry.identity.equals(identity)) {rejectStale();return Optional.empty();}
            long sequence=nextPublication,followingPublication=incrementOrClose(sequence,"publication sequence");
            var retire=new Retire(serverEpoch,sequence,identity,life,Math.max(0,entry.frameSerial),reason);
            active.remove(identity.key());nextPublication=followingPublication;return Optional.of(retire);
        }
        public long nextPublicationSequence(){confined();return nextPublication;}
        public long rejectedStaleCallbacks(){confined();return rejectedStale;}
        public int activeStreams(){confined();return active.size();}
        /** Closes this recipient/socket gate; subsequent stale callbacks allocate no sequence. */
        public void close(){confined();closed=true;active.clear();}
        public boolean closed(){confined();return closed;}
        private boolean validStart(StreamIdentity identity,long firstFrameSerial,StartReason reason){return matchesEpoch(identity) && firstFrameSerial>=1 && reason!=null;}
        private boolean validFrameRequest(StreamIdentity identity,long life,long frameSerial,long authorityTick,long jointSampleTick,long rootFrameSequence,long rootFrameTick,FrameKind kind,boolean available) {
            return matchesEpoch(identity) && life>=1 && frameSerial>=1 && authorityTick>=0 && jointSampleTick>=0 && rootFrameSequence>=0 && rootFrameTick>=0
                && kind!=null && (!available ? kind==FrameKind.UNAVAILABLE : kind!=FrameKind.UNAVAILABLE);
        }
        private boolean validRetire(StreamIdentity identity,long life,RetireReason reason){return matchesEpoch(identity) && life>=1 && reason!=null;}
        private boolean matchesEpoch(StreamIdentity identity){return identity!=null && serverEpoch.equals(identity.serverEpoch());}
        private long incrementOrClose(long value,String counter) {
            try{return Math.incrementExact(value);}catch(ArithmeticException exception) {closed=true;active.clear();throw new IllegalStateException("Anatomy stream "+counter+" overflow closed this recipient gate",exception);}
        }
        private void rejectStale(){rejectedStale=incrementOrClose(rejectedStale,"stale-callback counter");}
        private void confined(){if(owner==null)owner=Thread.currentThread();else if(owner!=Thread.currentThread())throw new IllegalStateException("Server anatomy stream gate escaped its owner thread");}
    }

    /** Client-thread confined bounded ledger; no networking callback is registered here. */
    public static final class ClientLedger {
        private final int maxActive;
        private final Map<SupportKey,ClientEntry> active=new LinkedHashMap<>();
        private Thread owner;
        private UUID serverEpoch;
        private long connectionGeneration,expectedSequence=1;
        private boolean failed;

        public ClientLedger(int maxActive) {if(maxActive<1)throw new IllegalArgumentException("Invalid client anatomy stream capacity");this.maxActive=maxActive;}
        public long beginConnection(UUID epoch) {
            confined();if(epoch==null)throw new IllegalArgumentException("Server epoch required");connectionGeneration=incrementOrFail(connectionGeneration,"connection generation");serverEpoch=epoch;expectedSequence=1;failed=false;active.clear();return connectionGeneration;
        }
        public long resetConnection(){confined();connectionGeneration=incrementOrFail(connectionGeneration,"connection generation");serverEpoch=null;expectedSequence=1;failed=false;active.clear();return connectionGeneration;}
        public ReceiveResult receive(long callbackConnectionGeneration,Publication publication) {
            confined();if(callbackConnectionGeneration!=connectionGeneration)return ReceiveResult.STALE_CONNECTION;
            if(failed)return ReceiveResult.FAILED_CONNECTION;
            if(publication.publicationSequence()<expectedSequence)return ReceiveResult.STALE_SEQUENCE;
            if(publication.publicationSequence()>expectedSequence) {failed=true;active.clear();return ReceiveResult.GAP_FAIL_CLOSED;}
            // Consume before content/identity checks. A malformed current packet
            // cannot be reintroduced under this publication sequence.
            expectedSequence=incrementOrFail(expectedSequence,"publication sequence");
            if(serverEpoch==null || !serverEpoch.equals(publication.serverEpoch()) || !publication.serverEpoch().equals(publication.identity().serverEpoch()))
                return ReceiveResult.REJECTED_SEMANTICS;
            return switch(publication) {
                case StreamStart start -> acceptStart(start);
                case Frame frame -> acceptFrame(frame);
                case Retire retire -> acceptRetire(retire);
            };
        }
        public int activeStreams(){confined();return active.size();}
        public long expectedSequence(){confined();return expectedSequence;}
        public boolean failed(){confined();return failed;}
        public long connectionGeneration(){confined();return connectionGeneration;}
        private ReceiveResult acceptStart(StreamStart start) {
            var key=start.identity().key();if(active.containsKey(key))return ReceiveResult.REJECTED_SEMANTICS;
            if(active.size()>=maxActive)return ReceiveResult.REJECTED_CAPACITY;
            active.put(key,new ClientEntry(start.identity(),start.supportStreamLife(),start.firstFrameSerial()-1,-1,-1,-1,-1));return ReceiveResult.ACCEPTED;
        }
        private ReceiveResult acceptFrame(Frame frame) {
            var entry=active.get(frame.identity().key());
            if(entry==null || entry.life!=frame.supportStreamLife() || !entry.identity.equals(frame.identity()))return ReceiveResult.REJECTED_SEMANTICS;
            if(!validFrame(entry,frame.frameSerial(),frame.authorityTick(),frame.jointSampleTick(),frame.rootFrameSequence(),frame.rootFrameTick(),frame.kind(),frame.available()))return ReceiveResult.REJECTED_CLOCK;
            entry.frameSerial=frame.frameSerial();entry.authorityTick=frame.authorityTick();entry.jointSampleTick=frame.jointSampleTick();entry.rootFrameSequence=frame.rootFrameSequence();entry.rootFrameTick=frame.rootFrameTick();return ReceiveResult.ACCEPTED;
        }
        private ReceiveResult acceptRetire(Retire retire) {
            var entry=active.get(retire.identity().key());
            if(entry==null || entry.life!=retire.supportStreamLife() || !entry.identity.equals(retire.identity()) || retire.lastFrameSerial()<entry.frameSerial)return ReceiveResult.REJECTED_SEMANTICS;
            active.remove(retire.identity().key());return ReceiveResult.ACCEPTED;
        }
        private void confined(){if(owner==null)owner=Thread.currentThread();else if(owner!=Thread.currentThread())throw new IllegalStateException("Client anatomy stream ledger escaped its owner thread");}
        private long incrementOrFail(long value,String counter) {
            try{return Math.incrementExact(value);}catch(ArithmeticException exception) {failed=true;active.clear();throw new IllegalStateException("Anatomy stream "+counter+" overflow failed this connection",exception);}
        }
    }

    private static boolean validFrame(Entry entry,long frameSerial,long authorityTick,long jointSampleTick,long rootFrameSequence,long rootFrameTick,FrameKind kind,boolean available) {
        if(frameSerial<=entry.frameSerial || authorityTick<entry.authorityTick || rootFrameSequence<entry.rootFrameSequence || rootFrameTick<entry.rootFrameTick)return false;
        return switch(kind) {
            case ROOT_ONLY -> available && jointSampleTick==entry.jointSampleTick && rootFrameSequence>entry.rootFrameSequence;
            case JOINT_UPDATE -> available && jointSampleTick>entry.jointSampleTick;
            case AVAILABLE_STATIC -> available && jointSampleTick>=entry.jointSampleTick;
            case UNAVAILABLE -> !available && jointSampleTick>=entry.jointSampleTick;
        };
    }
    private abstract static class Entry {
        final StreamIdentity identity;final long life;
        long frameSerial,authorityTick,jointSampleTick,rootFrameSequence,rootFrameTick;
        Entry(StreamIdentity identity,long life,long frameSerial,long authorityTick,long jointSampleTick,long rootFrameSequence,long rootFrameTick) {
            this.identity=identity;this.life=life;this.frameSerial=frameSerial;this.authorityTick=authorityTick;this.jointSampleTick=jointSampleTick;this.rootFrameSequence=rootFrameSequence;this.rootFrameTick=rootFrameTick;
        }
    }
    private static final class ServerEntry extends Entry {ServerEntry(StreamIdentity i,long l,long f,long a,long j,long r,long rt){super(i,l,f,a,j,r,rt);}}
    private static final class ClientEntry extends Entry {ClientEntry(StreamIdentity i,long l,long f,long a,long j,long r,long rt){super(i,l,f,a,j,r,rt);}}
}
