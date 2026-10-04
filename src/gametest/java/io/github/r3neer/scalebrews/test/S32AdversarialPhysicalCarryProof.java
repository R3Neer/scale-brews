package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.*;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/** Independent R1 oracle: exact physical points and signed ROOT burst, with voluntary movement. */
public final class S32AdversarialPhysicalCarryProof implements FabricClientGameTest {
    private static final List<AnatomyObserverCarryPayload> WIRE=new CopyOnWriteArrayList<>();
    private static volatile boolean armed;
    public static void decodedBundle(net.minecraft.network.protocol.game.ClientboundBundlePacket bundle) {
        if(!armed)return;
        var packets=new ArrayList<net.minecraft.network.protocol.Packet<?>>();bundle.subPackets().forEach(packets::add);
        if(packets.size()!=2 || !(packets.get(1) instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom)
                || !(custom.payload() instanceof AnatomyObserverCarryPayload mark))return;
        var c=net.minecraft.client.Minecraft.getInstance();var body=c.level.getEntity(mark.bodyId());
        if(body==null || !body.getUUID().equals(mark.body()))throw new AssertionError("R2 wrong actual decoded body");
        var nativePacket=packets.getFirst();
        if(nativePacket instanceof net.minecraft.network.protocol.game.ClientboundMoveEntityPacket move) {
            if(move.getEntity(c.level)!=body || !move.hasPosition())throw new AssertionError("R2 native packet body mismatch");
        } else if(nativePacket instanceof net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket sync) {
            if(sync.id()!=body.getId())throw new AssertionError("R2 native sync body mismatch");
        } else throw new AssertionError("R2 bundle lacks its actual native position packet");
        same(body.getPositionCodec().getBase(),mark.nativeTarget(),"R2 actual decoded native target");
        WIRE.add(mark);System.out.println("S32 ADVERSARY PHYSICAL WIRE "+mark);
    }
    @Override public void runTest(ClientGameTestContext context) {
        WIRE.clear();armed=true;
        var geometry=context.computeOnClient(c->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var supportId=new AtomicInteger();var bodyId=new AtomicInteger();
        var expected=new AtomicReference<List<AnatomyObserverCarries.Carry>>();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(s->{
                var support=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);
                if(support==null)throw new AssertionError("Missing support");
                support.setNoAi(true);support.setNoGravity(true);support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
                support.setPos(2,3,2);s.overworld().addFreshEntity(support);supportId.set(support.getId());
                AnatomyRuntime.startPrepared(s,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s32_adversary",profile));
            });
            context.waitFor(c->c.level!=null && c.level.getEntity(supportId.get()) instanceof Cow support && AnatomyClientNetworking.presentationFrame(support).isPresent(),160);
            world.getServer().runOnServer(s->{
                var support=(Cow)s.overworld().getEntity(supportId.get());
                var top=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().values().stream().map(ConvexBox::bounds)
                    .max(Comparator.comparingDouble(b->b.maxY)).orElseThrow();
                var body=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);
                if(body==null)throw new AssertionError("Missing body");
                body.setNoAi(true);body.setNoGravity(true);body.getAttribute(Attributes.SCALE).setBaseValue(.35);body.refreshDimensions();
                body.setPos(top.getCenter().x,top.maxY+.3,top.getCenter().z);s.overworld().addFreshEntity(body);bodyId.set(body.getId());
                body.move(MoverType.SELF,new Vec3(0,-.6,0));
                if(AnatomyMovement.contact(body)==null || AnatomyMovement.contact(body).support()!=support)throw new AssertionError("Contact fixture failed");
            });
            context.waitFor(c->c.level.getEntity(bodyId.get()) instanceof Cow body && AnatomyClientNetworking.presentationFrame(body).isPresent(),160);
            world.getServer().runOnServer(s->{
                var support=(Cow)s.overworld().getEntity(supportId.get());var body=(Cow)s.overworld().getEntity(bodyId.get());
                AnatomyObserverCarries.invalidate(body);
                body.move(MoverType.SELF,new Vec3(.08,0,0));
                if(!AnatomyObserverCarries.snapshot(body).isEmpty())throw new AssertionError("Voluntary movement inferred as carry");
                int index=0;
                for(double amount:new double[]{.2,-.125,.075}) {
                    var surface=AnatomyMovement.surface(body);if(surface==null)throw new AssertionError("Voluntary move lost contact");
                    var before=AnatomyMovement.queryFrame(support).orElseThrow();
                    var bodyBefore=body.position();var velocity=body.getDeltaMovement();
                    var materialBefore=before.snapshot().pieces().get(surface.piece()).point(surface.localPoint());
                    support.move(MoverType.SELF,new Vec3(amount,0,0));
                    var after=AnatomyMovement.queryFrame(support).orElseThrow();
                    var materialAfter=after.snapshot().pieces().get(surface.piece()).point(surface.localPoint());
                    var records=AnatomyObserverCarries.snapshot(body);
                    if(records.size()!=++index)throw new AssertionError("R1 carry lost/duplicated: "+records);
                    var mark=records.getLast();
                    if(mark.beforeSupportFrameSerial()!=before.endpoint().frameSerial() || mark.supportFrameSerial()!=after.endpoint().frameSerial())
                        throw new AssertionError("R1 serial reconstructed from latest pose: "+mark);
                    same(mark.bodyBefore(),bodyBefore,"body before");same(mark.bodyAfter(),body.position(),"body after");
                    same(mark.appliedDelta(),new Vec3(amount,0,0),"passive signed delta");
                    same(mark.materialBefore(),materialBefore,"physical point before");same(mark.materialAfter(),materialAfter,"physical point after");
                    same(mark.localPoint(),surface.localPoint(),"local point");same(body.getDeltaMovement(),velocity,"voluntary velocity");
                    if(!mark.body().equals(body.getUUID()) || mark.bodyId()!=body.getId() || !mark.support().equals(support.getUUID())
                            || mark.supportId()!=support.getId() || !mark.piece().equals(surface.piece()) || mark.face()!=surface.face()
                            || mark.revision()!=surface.revision() || !mark.epoch().equals(AnatomyNetworking.epoch(s))
                            || mark.supportBindingGeneration()!=before.identity().bindingGeneration()
                            || !mark.dimension().equals(s.overworld().dimension().identifier())
                            || mark.tick()!=s.overworld().getGameTime()
                            || mark.transportSequence()!=AnatomyMovement.transport(body).sequence())
                        throw new AssertionError("R1 physical identity mismatch");
                    System.out.println("S32 ADVERSARY PHYSICAL "+mark);
                }
                expected.set(AnatomyObserverCarries.snapshot(body));
            });
            context.waitFor(c->WIRE.stream().filter(p->p.bodyId()==bodyId.get()).flatMap(p->p.carries().stream())
                .anyMatch(m->m.transportSequence()==expected.get().getLast().transportSequence()),160);
            var packets=WIRE.stream().filter(p->p.bodyId()==bodyId.get()).toList();
            for(var carry:expected.get()) {
                var marks=packets.stream().flatMap(p->p.carries().stream()).filter(m->m.transportSequence()==carry.transportSequence()).toList();
                if(marks.size()!=1)throw new AssertionError("R2 actual tracking omitted/duplicated physical carry "+carry.transportSequence());
                var m=marks.getFirst();
                if(m.materialSerial()!=carry.materialSerial() || m.materialSerial()<1
                        || m.beforeSupportFrameSerial()!=carry.beforeSupportFrameSerial() || m.supportFrameSerial()!=carry.supportFrameSerial()
                        || !m.piece().equals(carry.piece()) || m.face()!=carry.face())throw new AssertionError("R2 changed physical provenance");
                same(m.localPoint(),carry.localPoint(),"wire local point");same(m.bodyBefore(),carry.bodyBefore(),"wire before");
                same(m.bodyAfter(),carry.bodyAfter(),"wire after");same(m.appliedDelta(),carry.appliedDelta(),"wire passive");
            }
            context.waitTicks(5);
            context.waitFor(c->{var body=c.level.getEntity(bodyId.get());return body.getInterpolation()!=null
                && !body.getInterpolation().hasActiveInterpolation() && body.oldPosition().distanceToSqr(body.position())<1e-20;},80);
            context.runOnClient(c->{var body=c.level.getEntity(bodyId.get());
                for(float partial:new float[]{0f,.25f,.5f,.75f,1f}) {
                    var offset=io.github.r3neer.scalebrews.client.collision.presentation.CausalBodyResidual.rendererOffset(body,partial)
                        .add(io.github.r3neer.scalebrews.client.collision.presentation.CausalRemoteResidual.rendererOffset(body,partial));
                    System.out.println("S32 ADVERSARY QUIET native old="+body.oldPosition()+" current="+body.position()+" partial="+partial+" residual="+offset);
                    if(offset.lengthSqr()>1e-16)throw new AssertionError("R4: observer residual survives native incorporation and the following quiet baseline: partial="+partial+" residual="+offset);
                }
            });
            world.getServer().runOnServer(s->{var body=s.overworld().getEntity(bodyId.get());AnatomyMovement.clear(body);
                if(!AnatomyObserverCarries.snapshot(body).isEmpty() || expected.get().size()!=3)throw new AssertionError("R1 clear or immutable history failed");});
            System.out.println("S32 ADVERSARY PHYSICAL PASS exact signed burst, voluntary separation, sealed point, clear and production tracking bundle");
        } finally {armed=false;world.getServer().runOnServer(AnatomyRuntime::stop);world.close();}
    }
    private static void same(Vec3 actual,Vec3 expected,String name){if(actual.distanceToSqr(expected)>1e-10)throw new AssertionError("R1 "+name+": "+actual+" != "+expected);}
}
