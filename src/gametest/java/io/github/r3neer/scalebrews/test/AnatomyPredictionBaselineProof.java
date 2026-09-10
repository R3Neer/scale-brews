package io.github.r3neer.scalebrews.test;

import com.google.gson.Gson;
import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.internal.AnatomyTransportReceipts;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * N2 measurement only: real dedicated networking with an original exported
 * catalog. It deliberately has no anatomy C2S reference, rollback or replay.
 * Register it in a separate client-test selector after the Q1 freeze.
 */
public final class AnatomyPredictionBaselineProof implements FabricClientGameTest {
    private static final int PHASE_TICKS=200;
    private static final int[] RTT_MILLIS={0,100,200};

    @Override
    public void runTest(ClientGameTestContext context) {
        try {runMeasured(context);}
        catch(Throwable failure) {throw new AssertionError("N2 baseline dedicated proof failed; inspect the emitted phase evidence",failure);}
    }

    private static void runMeasured(ClientGameTestContext context) throws Throwable {
        var catalog=loadOriginalCatalog();
        var profiles=Map.of("scalebrews:n2_cow",cowProfile(catalog.cow()));
        var properties=new java.util.Properties();properties.setProperty("allow-flight","false");
        try(var world=context.worldBuilder().createServer(properties);var connection=world.connect()) {
            Throwable failure=null;
            var latency=new AtomicReference<PlatformTestLatency>();
            try {
                // Install at RTT 0 before boot/setup: a timeout while waiting
                // for catalog, teleport or contact still has channel evidence.
                latency.set(context.computeOnClient(PlatformTestLatency::install));
                var fixture=new Fixture();
                world.runOnServer(server->boot(server,catalog.models(),profiles,fixture));
                awaitReady(context,fixture);
                // Setup uses a real server teleport at RTT 0. Direct
                // ServerPlayer#setPos would leave the client at a different
                // position and poison the first measured baseline.
                world.runCommand("gamemode survival @a");
                var playerTarget=world.computeOnServer(server->preparePlayerTeleport(server,fixture));
                world.runCommand("tp @a[limit=1] "+commandCoordinate(playerTarget.x)+" "+commandCoordinate(playerTarget.y)+" "+commandCoordinate(playerTarget.z));
                context.waitFor(client->client.player!=null && client.player.position().distanceToSqr(playerTarget)<.0025,100);
                world.runOnServer(server->confirmTeleportedPlayer(server,fixture));
                awaitPlayerContact(context,fixture);

                runRootPhases(context,world,fixture,latency.get());

                world.runOnServer(server->confirmBoatAndObserver(server,fixture));
                awaitBoatContact(context,fixture);
                runBoatPhases(context,world,fixture,latency.get());
                assertRemoteObserverNeverCarries(context,fixture);
            } catch(Throwable error) {
                var handler=latency.get();
                if(handler!=null)ScaleBrews.LOGGER.error("N2 setup or measurement failure; channel evidence before cleanup={}",handler.baseline(),error);
                failure=error;throw error;
            } finally {
                Throwable cleanupFailure=null;
                try {context.runOnClient(client->{
                    client.options.keyUp.setDown(false);client.options.keyDown.setDown(false);
                    var handler=latency.get();if(handler!=null)handler.close(client);
                });}
                catch(Throwable cleanup) {cleanupFailure=cleanup;}
                try {world.runOnServer(AnatomyRuntime::stop);}
                catch(Throwable cleanup) {
                    if(cleanupFailure==null)cleanupFailure=cleanup;else cleanupFailure.addSuppressed(cleanup);
                }
                if(cleanupFailure!=null) {if(failure!=null)failure.addSuppressed(cleanupFailure);else throw cleanupFailure;}
            }
        }
    }

    private static void runRootPhases(ClientGameTestContext context,TestDedicatedServerContext world,Fixture fixture,PlatformTestLatency latency) throws Exception {
        for(int rtt:RTT_MILLIS)runPhase(context,world,fixture,latency,rtt,false);
    }

    private static void runBoatPhases(ClientGameTestContext context,TestDedicatedServerContext world,Fixture fixture,PlatformTestLatency latency) throws Exception {
        for(int rtt:RTT_MILLIS)runPhase(context,world,fixture,latency,rtt,true);
    }

    private static void runPhase(ClientGameTestContext context,TestDedicatedServerContext world,Fixture fixture,
            PlatformTestLatency latency,int rtt,boolean boat) throws Exception {
        try {runMeasuredPhase(context,world,fixture,latency,rtt,boat);}
        catch(Throwable failure) {
            // A timeout at a FIFO fence or a lost support must still leave the
            // channel evidence in the dedicated log before test cleanup runs.
            ScaleBrews.LOGGER.error("N2 pre-assertion failure phase={} RTT={}ms baseline={}",boat?"boat":"player",rtt,latency.baseline(),failure);
            if(failure instanceof Exception checked)throw checked;
            if(failure instanceof Error error)throw error;
            throw new RuntimeException(failure);
        }
    }

    private static void runMeasuredPhase(ClientGameTestContext context,TestDedicatedServerContext world,Fixture fixture,
            PlatformTestLatency latency,int rtt,boolean boat) throws Exception {
        // Close setup at RTT 0 through a FIFO fence. At 100/200 ms the runtime
        // publishes poses continuously, so waiting for an empty channel would
        // be a false requirement and can starve forever.
        latency.latency(0);
        var setupFence=latency.fence();
        context.waitFor(client->latency.passed(setupFence),40);
        latency.resetBaseline();
        latency.latency(rtt);
        world.runOnServer(server->AnatomyTransportReceipts.invalidate(boat?fixture.boat.get():server.getPlayerList().getPlayers().getFirst()));
        var startRelative=world.computeOnServer(server->{Entity body=boat?fixture.boat.get():server.getPlayerList().getPlayers().getFirst();return body.position().subtract(fixture.cow.get().position());});
        var maximumIndependent=new AtomicReference<>(0d);
        var previousSupport=new AtomicReference<SupportCursor>();
        var pulseEvidence=new ArrayList<PulseEvidence>();
        PulseStart activePulse=null;
        for(int tick=0;tick<PHASE_TICKS;tick++) {
            final int step=tick;
            // Four two-tick, alternating pulses have no intentional net walk
            // direction over the 200-tick phase.  A phase that leaves the
            // material is a fixture failure, not evidence about prediction.
            if(step%50==0) {
                boolean forward=((step/50)&1)==0;
                activePulse=new PulseStart(step,forward,latency.baseline());
                context.runOnClient(client->{client.options.keyUp.setDown(forward);client.options.keyDown.setDown(!forward);});
            }
            if(step%50==2)context.runOnClient(client->{client.options.keyUp.setDown(false);client.options.keyDown.setDown(false);});
            world.runOnServer(server->{
                var support=fixture.cow.get();
                // A physical rigid-root move; the runtime END tick is solely
                // responsible for any resulting carry/receipt.
                support.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(.003,0,(step&1)==0?.0015:-.0015));
                Entity body=boat?fixture.boat.get():server.getPlayerList().getPlayers().getFirst();
                maximumIndependent.updateAndGet(previous->Math.max(previous,body.position().subtract(support.position()).distanceTo(startRelative)));
            });
            context.waitTicks(1);
            if(step%50==3 && activePulse!=null) {
                var after=latency.baseline();
                var pulse=new PulseEvidence(activePulse.step(),activePulse.forward(),after.playerMoves()-activePulse.before().playerMoves(),
                    after.vehicleMoves()-activePulse.before().vehicleMoves(),activePulse.before().trace().size(),after.trace().size());
                pulseEvidence.add(pulse);ScaleBrews.LOGGER.info("N2 input pulse kind={} RTT={}ms {}",boat?"boat":"player",rtt,pulse);activePulse=null;
            }
            if(step%50==25)sampleSupport(context,world,fixture,boat,rtt,step,previousSupport);
        }
        context.runOnClient(client->{client.options.keyUp.setDown(false);client.options.keyDown.setDown(false);});
        // Give the final vanilla movement intent a full measured round trip
        // plus server-tick margin before placing the receive/send FIFO fence.
        // This is not a synthetic ACK and does not replay any local input.
        context.waitTicks(Math.max(4,rtt/25+4));
        var closingFence=latency.fence();
        latency.latency(0);
        context.waitFor(client->latency.passed(closingFence),Math.max(40,rtt/25+20));
        var baseline=latency.baseline();
        var evidence=new AtomicReference<PhaseEvidence>();
        world.runOnServer(server->{
            var player=server.getPlayerList().getPlayers().getFirst();Entity body=boat?fixture.boat.get():player;
            var receipts=AnatomyTransportReceipts.history(player,body.getUUID());
            evidence.set(new PhaseEvidence(server.overworld().getGameTime(),player.getUUID(),body.getUUID(),body.getId(),
                fixture.cow.get().getUUID(),AnatomyMovement.contactSequence(body),AnatomyMovement.transport(body)==null?0:AnatomyMovement.transport(body).sequence(),
                body.position(),body.position().subtract(fixture.cow.get().position()),startRelative,maximumIndependent.get(),List.copyOf(pulseEvidence),receipts,receipts.isEmpty()?null:receipts.getLast()));
        });
        var clientEvidence=context.computeOnClient(client->{
            var player=client.player;
            Entity body=player==null?null:boat?player.getRootVehicle():player;
            return body==null?new ClientEvidence(Vec3.ZERO,false):new ClientEvidence(body.position(),AnatomyMovement.supported(body));
        });
        emitPhaseEvidence(boat?"boat":"player",rtt,baseline,evidence.get(),clientEvidence);
        assertPhase(boat?"boat":"player",rtt,baseline,evidence.get(),clientEvidence);
    }

    private static void emitPhaseEvidence(String kind,int rtt,PlatformTestLatency.Baseline baseline,PhaseEvidence evidence,ClientEvidence client) {
        ScaleBrews.LOGGER.info("N2 evidence {} RTT={}ms tick={} player={} body={}#{} support={} contactSeq={} transportSeq={} serverPos={} clientPos={} clientSupported={} relative(start={}, end={}, independent={}) receipt={} packets(player={}, vehicle={}) corrections(player={}, vehicle={}) pending(in={}, out={}) dropped={}",
            kind,rtt,evidence.serverTick(),evidence.player(),evidence.body(),evidence.bodyId(),evidence.support(),evidence.contactSequence(),evidence.transportSequence(),evidence.position(),
            client.position(),client.supported(),evidence.startRelative(),evidence.endRelative(),evidence.maximumIndependent(),evidence.lastReceipt(),
            baseline.playerMoves(),baseline.vehicleMoves(),baseline.playerCorrections(),baseline.vehicleCorrections(),baseline.pendingInbound(),baseline.pendingOutbound(),baseline.droppedTraceEvents());
        ScaleBrews.LOGGER.info("N2 trace {} RTT={}ms relevant={}/{} dropped={} packetTotals={} pulses={}",kind,rtt,baseline.trace().size(),baseline.peakTraceEntries(),
            baseline.droppedTraceEvents(),baseline.packetTotals(),evidence.pulses());
    }

    /**
     * Check the actual material contact away from a deliberately short input
     * pulse.  This is fixture health evidence, not a claim that client-side
     * prediction has reconciled anything: it records the material/contact
     * evidence without assigning a missing support to a fixture or protocol.
     */
    private static void sampleSupport(ClientGameTestContext context,TestDedicatedServerContext world,Fixture fixture,
            boolean boat,int rtt,int step,AtomicReference<SupportCursor> previous) throws Exception {
        var serverSample=world.computeOnServer(server->{
            Entity body=boat?fixture.boat.get():server.getPlayerList().getPlayers().getFirst();
            return serverSupportSample(body,fixture.cow.get());
        });
        var clientSample=context.computeOnClient(client->{
            var player=client.player;
            Entity body=player==null?null:boat?player.getRootVehicle():player;
            var support=client.level==null?null:client.level.getEntity(fixture.cow.get().getId());
            return body==null || support==null
                ?new ClientSupportSample(Vec3.ZERO,false)
                :new ClientSupportSample(body.position().subtract(support.position()),AnatomyMovement.supported(body));
        });
        var prior=previous.getAndSet(serverSample.cursor());
        ScaleBrews.LOGGER.info("N2 support sample kind={} RTT={}ms step={} server(tick={}, supported={}, relative={}, planeMargin={}, edgeMargin={}, cursor={}, previous={}) client(supported={}, relative={})",
            boat?"boat":"player",rtt,step,serverSample.tick(),serverSample.supported(),serverSample.relative(),serverSample.planeMargin(),serverSample.edgeMargin(),serverSample.cursor(),prior,
            clientSample.supported(),clientSample.relative());
        if(!serverSample.supported())
            throw new IllegalStateException("N2 material support is absent at "+(boat?"boat":"player")+" RTT "+rtt+" step "+step+"; inspect packet/receipt evidence before assigning cause. current="+serverSample+" previous="+prior);
        // The signed separation is evaluated against the exact confirmed
        // surface and body extent.  A small tolerance covers ordinary
        // collision epsilon, never an arbitrary relocation allowance.
        if(!Double.isFinite(serverSample.planeMargin()) || !Double.isFinite(serverSample.edgeMargin()) || Math.abs(serverSample.planeMargin())>.03 || serverSample.edgeMargin()<-.01)
            throw new IllegalStateException("N2 material contact margin is invalid at "+(boat?"boat":"player")+" RTT "+rtt+" step "+step+"; inspect packet/receipt evidence before assigning cause. current="+serverSample+" previous="+prior);
        if(!clientSample.supported())
            throw new IllegalStateException("N2 client lost confirmed support before prediction assertions at "+(boat?"boat":"player")+" RTT "+rtt+" step "+step+": "+clientSample);
    }

    private static ServerSupportSample serverSupportSample(Entity body,Cow support) {
        var surface=AnatomyMovement.surface(body);
        var frame=AnatomyMovement.queryFrame(support).orElse(null);
        if(!AnatomyMovement.supported(body) || surface==null || frame==null)return new ServerSupportSample(support.level().getGameTime(),false,body.position().subtract(support.position()),Double.NaN,Double.NaN,null);
        var piece=frame.snapshot().pieces().get(surface.piece());
        if(piece==null)return new ServerSupportSample(support.level().getGameTime(),false,body.position().subtract(support.position()),Double.NaN,Double.NaN,null);
        var normal=piece.faceNormal(surface.face());
        var point=piece.point(surface.localPoint());
        var box=body.getBoundingBox();
        double extent=(box.getXsize()*Math.abs(normal.x)+box.getYsize()*Math.abs(normal.y)+box.getZsize()*Math.abs(normal.z))*.5;
        double planeMargin=box.getCenter().subtract(point).dot(normal)-extent;
        return new ServerSupportSample(support.level().getGameTime(),true,body.position().subtract(support.position()),planeMargin,
            tangentialMargin(piece,surface),new SupportCursor(surface.piece(),surface.face(),surface.localPoint(),AnatomyMovement.contactSequence(body),surface.tick()));
    }

    /** Exact shortest distance from the local face point to either tangent edge of its affine parallelogram. */
    private static double tangentialMargin(ConvexBox piece,SurfaceContact surface) {
        int normalAxis=surface.face()/2,first=(normalAxis+1)%3,second=(normalAxis+2)%3;
        Vec3 edgeFirst=edge(piece,first),edgeSecond=edge(piece,second);var local=surface.localPoint();
        double firstCoordinate=coordinate(local,first),secondCoordinate=coordinate(local,second);
        double toFirstEdge=Math.min(firstCoordinate,1-firstCoordinate)*perpendicularLength(edgeFirst,edgeSecond);
        double toSecondEdge=Math.min(secondCoordinate,1-secondCoordinate)*perpendicularLength(edgeSecond,edgeFirst);
        return Math.min(toFirstEdge,toSecondEdge);
    }

    private static Vec3 edge(ConvexBox piece,int axis) {
        var origin=piece.vertices().getFirst();
        return switch(axis) {case 0->piece.vertices().get(1).subtract(origin);case 1->piece.vertices().get(2).subtract(origin);default->piece.vertices().get(4).subtract(origin);};
    }
    private static double coordinate(Vec3 value,int axis) {return axis==0?value.x:axis==1?value.y:value.z;}
    private static double perpendicularLength(Vec3 edge,Vec3 other) {
        double otherLength=other.lengthSqr();return otherLength<=1e-20?Double.NaN:Math.sqrt(Math.max(0,edge.lengthSqr()-Math.pow(edge.dot(other),2)/otherLength));
    }

    private static void assertPhase(String kind,int rtt,PlatformTestLatency.Baseline baseline,PhaseEvidence evidence,ClientEvidence client) {
        if(baseline.droppedTraceEvents()!=0)
            throw new AssertionError("N2 "+kind+" RTT "+rtt+" trace incomplete: "+baseline);
        if((kind.equals("player") && baseline.playerMoves()==0) || (kind.equals("boat") && baseline.vehicleMoves()==0))
            throw new AssertionError("N2 "+kind+" RTT "+rtt+" emitted no real controlled movement packet: "+baseline);
        if(baseline.playerCorrections()!=0 || baseline.vehicleCorrections()!=0)
            throw new AssertionError("N2 "+kind+" RTT "+rtt+" received vanilla correction(s), not a prediction success: "+baseline+" evidence="+evidence);
        if(evidence.receipts().isEmpty())throw new AssertionError("N2 "+kind+" RTT "+rtt+" had no post-baseline transport receipts: "+evidence);
        double independent=evidence.endRelative().distanceTo(evidence.startRelative());
        if(evidence.maximumIndependent()>.8)
            throw new AssertionError("N2 relative excursion exceeded the bounded measurement margin at "+kind+" RTT "+rtt+": max="+evidence.maximumIndependent()+"; inspect samples/receipts before assigning cause. evidence="+evidence);
        if(!client.supported())throw new AssertionError("N2 "+kind+" lost confirmed anatomy support at RTT "+rtt+" clientPosition="+client.position());
        var serials=new HashSet<Long>();for(var receipt:evidence.receipts())if(!serials.add(receipt.transportSequence()))
            throw new AssertionError("N2 "+kind+" RTT "+rtt+" duplicated transport sequence "+receipt.transportSequence()+": "+evidence);
    }

    private static void boot(MinecraftServer server,Map<String,ModelGeometry> models,Map<String,io.github.r3neer.scalebrews.platform.PlatformDefinition> profiles,Fixture fixture) {
        var level=server.overworld();var cow=net.minecraft.world.entity.EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
        if(cow==null)throw new IllegalStateException("N2 could not create cow support");
        cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(0,30,0);scale(cow,4);level.addFreshEntity(cow);fixture.cow.set(cow);
        AnatomyRuntime.startPrepared(server,models,profiles);
    }

    private static Vec3 preparePlayerTeleport(MinecraftServer server,Fixture fixture) {
        var player=server.getPlayerList().getPlayers().getFirst();scale(player,.35);
        var candidate=findTopFace(player,AnatomyMovement.queryFrame(fixture.cow.get()).orElseThrow().snapshot());
        var target=player.position().add(candidate.body().getCenter().subtract(player.getBoundingBox().getCenter()));
        fixture.player.set(player);fixture.playerPlacement.set(new Placement(candidate,target));return target;
    }

    private static void confirmTeleportedPlayer(MinecraftServer server,Fixture fixture) {
        var player=fixture.player.get();var placement=fixture.playerPlacement.get();
        if(player==null || placement==null || player.position().distanceToSqr(placement.target())>.0025)
            throw new IllegalStateException("N2 player did not arrive at the authoritative setup teleport before contact confirmation");
        confirmAt(player,fixture.cow.get(),placement.candidate());
    }

    private static void confirmBoatAndObserver(MinecraftServer server,Fixture fixture) {
        var level=server.overworld();var player=fixture.player.get();
        var boat=net.minecraft.world.entity.EntityTypes.OAK_BOAT.create(level,EntitySpawnReason.COMMAND);
        if(boat==null)throw new IllegalStateException("N2 could not create oak boat");level.addFreshEntity(boat);confirmOnTop(boat,fixture.cow.get());
        player.stopRiding();if(!player.startRiding(boat,true,true))throw new IllegalStateException("N2 player could not mount controlled boat");fixture.boat.set(boat);
        var observer=net.minecraft.world.entity.EntityTypes.PIG.create(level,EntitySpawnReason.COMMAND);
        if(observer==null)throw new IllegalStateException("N2 could not create observer pig");observer.setNoAi(true);observer.setNoGravity(true);scale(observer,.25);level.addFreshEntity(observer);confirmOnTop(observer,fixture.cow.get());fixture.observer.set(observer);
    }

    private static void confirmOnTop(Entity body,Cow support) {
        var frame=AnatomyMovement.queryFrame(support).orElseThrow(()->new IllegalStateException("N2 has no prepared original-cow QueryFrame"));
        var candidate=findTopFace(body,frame.snapshot());
        body.setPos(body.position().add(candidate.body().getCenter().subtract(body.getBoundingBox().getCenter())));
        confirmAt(body,support,candidate);
    }

    private static void confirmAt(Entity body,Cow support,FaceCandidate candidate) {
        var frame=AnatomyMovement.queryFrame(support).orElseThrow(()->new IllegalStateException("N2 lost original-cow QueryFrame before contact confirmation"));
        var surface=new SurfaceContact(support.getUUID(),frame.identity().revision(),candidate.piece(),candidate.face(),candidate.local(),candidate.normal(),support.level().getGameTime());
        if(!AnatomyMovement.confirm(body,support,surface) || !AnatomyMovement.supported(body))
            throw new IllegalStateException("N2 could not confirm real exported-cow material support for "+body.getType());
    }

    private static FaceCandidate findTopFace(Entity body,io.github.r3neer.scalebrews.collision.internal.GeometryProvider.Snapshot snapshot) {
        var before=body.getBoundingBox();var center=before.getCenter();var candidates=new ArrayList<FaceCandidate>();
        for(var entry:snapshot.pieces().entrySet())for(int face=0;face<6;face++) {
            ConvexBox piece=entry.getValue();var normal=piece.faceNormal(face);if(normal.y<=.99)continue;
            int axis=face/2;double[] localCoordinates={.5,.5,.5};localCoordinates[axis]=face%2;
            var local=new Vec3(localCoordinates[0],localCoordinates[1],localCoordinates[2]);var point=piece.point(local);
            double extent=(before.getXsize()*Math.abs(normal.x)+before.getYsize()*Math.abs(normal.y)+before.getZsize()*Math.abs(normal.z))*.5;
            var box=before.move(point.add(normal.scale(extent+.002)).subtract(center));
            if(body.level().noCollision(body,box) && snapshot.pieces().values().stream().noneMatch(other->other.overlaps(box)))
                candidates.add(new FaceCandidate(entry.getKey(),face,local,normal,box,faceArea(piece,axis)));
        }
        var selected=candidates.stream().sorted(java.util.Comparator.comparingDouble(FaceCandidate::usableArea).reversed()
            .thenComparing(FaceCandidate::piece).thenComparingInt(FaceCandidate::face))
            .findFirst()
            .orElseThrow(()->new IllegalStateException("N2 original cow has no clear upper material face for "+body.getType()));
        ScaleBrews.LOGGER.info("N2 selected deterministic material face body={} piece={} face={} usableArea={} local={}",
            body.getType(),selected.piece(),selected.face(),selected.usableArea(),selected.local());
        return selected;
    }

    private static double faceArea(ConvexBox piece,int normalAxis) {
        return edge(piece,(normalAxis+1)%3).cross(edge(piece,(normalAxis+2)%3)).length();
    }

    private static void awaitReady(ClientGameTestContext context,Fixture fixture) {
        context.waitFor(client->{
            var cow=client.level==null?null:client.level.getEntity(fixture.cow.get().getId());
            return cow instanceof LivingEntity living && AnatomyClientNetworking.catalog().ready() && AnatomyClientNetworking.pose(living.getUUID())!=null
                && AnatomyClientNetworking.geometry(living,client.level.getGameTime()).isPresent();
        },200);
    }

    private static void awaitPlayerContact(ClientGameTestContext context,Fixture fixture) {
        context.waitFor(client->client.player!=null && AnatomyClientNetworking.presentationContact(client.player,client.level.getGameTime()).isPresent(),200);
    }

    private static void awaitBoatContact(ClientGameTestContext context,Fixture fixture) {
        context.waitFor(client->{var root=client.player==null?null:client.player.getRootVehicle();return root!=null && root!=client.player
            && AnatomyClientNetworking.presentationContact(root,client.level.getGameTime()).isPresent();},200);
    }

    private static void assertRemoteObserverNeverCarries(ClientGameTestContext context,Fixture fixture) {
        context.runOnClient(client->{
            var observer=client.level.getEntity(fixture.observer.get().getId());
            if(!(observer instanceof LivingEntity living) || !AnatomyClientNetworking.presentationContact(living,client.level.getGameTime()).isPresent()
                    || AnatomyMovement.transport(living)!=null)
                throw new AssertionError("N2 remote observer body was not passive client-side");
        });
    }

    private static void scale(LivingEntity entity,double value) {
        var attribute=entity.getAttribute(Attributes.SCALE);if(attribute==null)throw new IllegalStateException("N2 needs SCALE attribute for physically clear fixture setup");
        attribute.setBaseValue(value);entity.refreshDimensions();
    }

    /** Brigadier accepts a decimal literal, not the scientific notation produced by Double#toString. */
    private static String commandCoordinate(double value) {
        if(!Double.isFinite(value))throw new IllegalArgumentException("N2 setup teleport coordinate is not finite: "+value);
        return BigDecimal.valueOf(value).toPlainString();
    }

    private static PlatformDefinition cowProfile(ModelGeometry cow) {
        return new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse(cow.source()),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
    }

    private static Catalog loadOriginalCatalog() throws IOException {
        var value=System.getProperty("scalebrews.anatomyCatalog");if(value==null || value.isBlank())throw new IllegalStateException("N2 needs the immutable isolated original anatomy export");
        var root=Path.of(value);var gson=new Gson();var cow=gson.fromJson(Files.readString(root.resolve("minecraft_cow.json")),ModelGeometry.class);
        var player=gson.fromJson(Files.readString(root.resolve("minecraft_player_wide.json")),ModelGeometry.class);
        if(cow==null || player==null || cow.format()!=2 || player.format()!=2 || !cow.source().equals("minecraft:cow") || !player.source().equals("minecraft:player_wide"))
            throw new IllegalArgumentException("N2 requires immutable format-2 original cow/player exports");
        return new Catalog(cow,Map.of(cow.source(),cow,player.source(),player));
    }

    private record Catalog(ModelGeometry cow,Map<String,ModelGeometry> models) {}
    private record FaceCandidate(String piece,int face,Vec3 local,Vec3 normal,AABB body,double usableArea) {}
    private record Placement(FaceCandidate candidate,Vec3 target) {}
    private record ClientEvidence(Vec3 position,boolean supported) {}
    private record ClientSupportSample(Vec3 relative,boolean supported) {}
    private record SupportCursor(String piece,int face,Vec3 local,long contactSequence,long surfaceTick) {}
    private record ServerSupportSample(long tick,boolean supported,Vec3 relative,double planeMargin,double edgeMargin,SupportCursor cursor) {}
    private record PulseStart(int step,boolean forward,PlatformTestLatency.Baseline before) {}
    private record PulseEvidence(int step,boolean forward,long playerMoveDelta,long vehicleMoveDelta,int traceBefore,int traceAfter) {}
    private record PhaseEvidence(long serverTick,java.util.UUID player,java.util.UUID body,int bodyId,java.util.UUID support,long contactSequence,long transportSequence,Vec3 position,Vec3 endRelative,Vec3 startRelative,double maximumIndependent,List<PulseEvidence> pulses,List<AnatomyTransportReceipts.Receipt> receipts,AnatomyTransportReceipts.Receipt lastReceipt) {}
    private static final class Fixture {
        final AtomicReference<Cow> cow=new AtomicReference<>();
        final AtomicReference<ServerPlayer> player=new AtomicReference<>();
        final AtomicReference<Placement> playerPlacement=new AtomicReference<>();
        final AtomicReference<Boat> boat=new AtomicReference<>();
        final AtomicReference<Pig> observer=new AtomicReference<>();
    }
}
