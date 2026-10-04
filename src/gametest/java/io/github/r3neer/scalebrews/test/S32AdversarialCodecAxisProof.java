package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalBodyResidual;
import io.github.r3neer.scalebrews.collision.geometry.*;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.*;

/** Reproduction: a zero delta axis must use the actual native recipient codec baseline. */
public final class S32AdversarialCodecAxisProof implements FabricClientGameTest {
    private static final List<AnatomyObserverCarryPayload> WIRE=new CopyOnWriteArrayList<>();
    private static volatile boolean armed;
    private static volatile int watched;
    private static String lane(){return "CODEC_ZERO_AXIS";}
    public static int steps(Entity body) {
        var h=body.getInterpolation();if(h==null)return 0;
        try{var f=InterpolationHandler.class.getDeclaredField("interpolationData");f.setAccessible(true);var data=f.get(h);var n=data.getClass().getDeclaredField("steps");n.setAccessible(true);return n.getInt(data);}
        catch(ReflectiveOperationException e){throw new AssertionError("Native fixture",e);}
    }
    public static void afterBundle(ClientboundBundlePacket p){if(!armed)return;for(var sub:p.subPackets())if(sub instanceof ClientboundCustomPayloadPacket c && c.payload() instanceof AnatomyObserverCarryPayload mark && mark.bodyId()==watched)WIRE.add(mark);}
    @Override public void runTest(ClientGameTestContext context){
        WIRE.clear();armed=true;watched=-1;
        var geometry=context.computeOnClient(c->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var supportId=new AtomicInteger();var bodyId=new AtomicInteger();var world=context.worldBuilder().create();
        try{
            world.getServer().runOnServer(s->{var support=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);if(support==null)throw new AssertionError("No support");support.setNoAi(true);support.setNoGravity(true);support.setYRot(0);support.yBodyRot=0;support.yHeadRot=0;support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();support.setPos(2,3,2);s.overworld().addFreshEntity(support);supportId.set(support.getId());AnatomyRuntime.startPrepared(s,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s32_private",profile));});
            context.waitFor(c->c.level!=null && c.level.getEntity(supportId.get()) instanceof Cow support && AnatomyClientNetworking.presentationFrame(support).isPresent(),160);
            world.getServer().runOnServer(s->{var support=(Cow)s.overworld().getEntity(supportId.get());var top=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().values().stream().map(ConvexBox::bounds).max(Comparator.comparingDouble(b->b.maxY)).orElseThrow();var body=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);if(body==null)throw new AssertionError("No body");body.setNoAi(true);body.setNoGravity(true);body.getAttribute(Attributes.SCALE).setBaseValue(.35);body.refreshDimensions();body.setPos(top.getCenter().x,top.maxY+.3,top.getCenter().z);s.overworld().addFreshEntity(body);bodyId.set(body.getId());watched=body.getId();body.move(MoverType.SELF,new Vec3(0,-.6,0));if(AnatomyMovement.contact(body)==null)throw new AssertionError("Physical contact absent");});
            context.waitFor(c->c.level.getEntity(bodyId.get()) instanceof Cow body && AnatomyClientNetworking.presentationFrame(body).isPresent(),160);context.waitTicks(5);
                world.getServer().runOnServer(s->s.overworld().getEntity(supportId.get()).move(MoverType.SELF,new Vec3(0,0,.12345)));
                context.waitFor(c->WIRE.size()==1,160);awaitMaterial(context,bodyId.get(),WIRE.getFirst());context.waitTicks(1);
                sampleVector(context,bodyId.get(),WIRE.getFirst().carries().getLast().bodyAfter(),false);
                context.waitTicks(5);sampleVector(context,bodyId.get(),WIRE.getFirst().carries().getLast().bodyAfter(),true);
                world.getServer().runOnServer(s->s.overworld().getEntity(supportId.get()).move(MoverType.SELF,new Vec3(.25,0,0)));
                context.waitFor(c->WIRE.size()==2,160);awaitMaterial(context,bodyId.get(),WIRE.getLast());context.waitTicks(1);
                context.runOnClient(c->{var body=c.level.getEntity(bodyId.get());System.out.println("S32 ADVERSARY CODEC_ZERO_AXIS attested="+WIRE.getLast().nativeTarget()+" actual native codec="+body.getPositionCodec().getBase()+" contact/Q2 ready=true");});
                sampleVector(context,bodyId.get(),WIRE.getLast().carries().getLast().bodyAfter(),false);
        }finally{armed=false;world.getServer().runOnServer(AnatomyRuntime::stop);world.close();}
    }
    private static void awaitMaterial(ClientGameTestContext context,int id,AnatomyObserverCarryPayload packet){context.waitFor(c->{var body=c.level.getEntity(id);var support=(Cow)c.level.getEntity(packet.supportId());var contact=AnatomyClientNetworking.presentationContact(body,c.level.getGameTime()).orElse(null);return contact!=null && contact.bodyTrackingGeneration()==packet.bodyTrackingGeneration() && packet.carries().stream().allMatch(m->AnatomyClientNetworking.presentationInterval(support,m.materialSerial(),0).isPresent() && AnatomyClientNetworking.presentationInterval(support,m.materialSerial(),1).isPresent());},80);}
    private static void sampleVector(ClientGameTestContext context,int id,Vec3 target,boolean zero){context.runOnClient(c->{var body=c.level.getEntity(id);var pos=body.position();var old=body.oldPosition();var box=body.getBoundingBox();var velocity=body.getDeltaMovement();boolean positive=false;for(float f:new float[]{0,.25f,.5f,.75f,1}){var expected=zero?Vec3.ZERO:target.subtract(old.lerp(pos,f));var actual=CausalBodyResidual.rendererOffset(body,f).add(io.github.r3neer.scalebrews.client.collision.presentation.CausalRemoteResidual.rendererOffset(body,f));if(expected.lengthSqr()>1e-10)positive=true;System.out.println("S32 ADVERSARY VECTOR "+lane()+" f="+f+" expected="+expected+" actual="+actual+" steps="+steps(body));if(actual.distanceToSqr(expected)>1e-6)throw new AssertionError("R2/R4 "+lane()+" expected="+expected+" actual="+actual);if(CausalBodyResidual.cameraOffset(body,f).lengthSqr()>1e-20 || !actual.equals(CausalBodyResidual.rendererOffset(body,f).add(io.github.r3neer.scalebrews.client.collision.presentation.CausalRemoteResidual.rendererOffset(body,f))))throw new AssertionError("Vector query/camera ownership");}if(!zero && !positive)throw new AssertionError("Vector positive fixture already incorporated");if(!pos.equals(body.position()) || !old.equals(body.oldPosition()) || !box.equals(body.getBoundingBox()) || !velocity.equals(body.getDeltaMovement()))throw new AssertionError("Vector render query changed physics");});}
}
