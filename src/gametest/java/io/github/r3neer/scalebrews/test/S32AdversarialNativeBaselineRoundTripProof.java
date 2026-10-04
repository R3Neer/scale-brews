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

/** Reproduction: native baseline round trip must not revive remote residual within the same tick. */
public final class S32AdversarialNativeBaselineRoundTripProof implements FabricClientGameTest {
    private static final List<AnatomyObserverCarryPayload> WIRE=new CopyOnWriteArrayList<>();
    private static volatile boolean armed;
    private static volatile int watched;
    private static String lane(){return "OLD_BASELINE";}
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
            world.getServer().runOnServer(s->{var support=s.overworld().getEntity(supportId.get());support.move(MoverType.SELF,new Vec3(.75,0,0));});
            context.waitFor(c->!WIRE.isEmpty(),160);
            double target=WIRE.getFirst().carries().getLast().bodyAfter().x;
            sample(context,bodyId.get(),target,false);
            context.waitTicks(1);
            sample(context,bodyId.get(),target,false);
            context.runOnClient(c->{var body=c.level.getEntity(bodyId.get());var previous=body.oldPosition();
                if(previous.equals(body.position()) || steps(body)<1)throw new AssertionError("Baseline replacement fixture needs active interpolation");
                var position=body.position();var bounds=body.getBoundingBox();var velocity=body.getDeltaMovement();int tick=body.tickCount;
                body.setOldPosAndRot();
                if(body.oldPosition().equals(previous) || tick!=body.tickCount || !position.equals(body.position()))throw new AssertionError("Baseline replacement fixture did not change oldPosition only");
                body.setPos(previous);body.setOldPosAndRot();body.setPos(position);
                if(!body.oldPosition().equals(previous) || !body.position().equals(position))throw new AssertionError("Baseline round-trip fixture did not restore value tuple");
                var offset=io.github.r3neer.scalebrews.client.collision.presentation.CausalRemoteResidual.rendererOffset(body,.5f);
                System.out.println("S32 ADVERSARY BASELINE_ROUND_TRIP old="+previous+" replaced="+body.oldPosition()+" activeSteps="+steps(body)+" offset="+offset);
                if(offset.lengthSqr()>1e-20)throw new AssertionError("R4 native baseline round trip revives remote residual");
                if(!bounds.equals(body.getBoundingBox()) || !velocity.equals(body.getDeltaMovement()))throw new AssertionError("Baseline read changed physics");
            });
        }finally{armed=false;world.getServer().runOnServer(AnatomyRuntime::stop);world.close();}
    }
    private static void sample(ClientGameTestContext context,int id,double target,boolean zero){context.runOnClient(c->{var body=c.level.getEntity(id);var position=body.position();var old=body.oldPosition();var box=body.getBoundingBox();var velocity=body.getDeltaMovement();for(float f:new float[]{0f,.25f,.5f,.75f,1f}){
        double expected=zero?0:target-(old.x+(position.x-old.x)*f);var got=CausalBodyResidual.rendererOffset(body,f).add(io.github.r3neer.scalebrews.client.collision.presentation.CausalRemoteResidual.rendererOffset(body,f));
        if(!lane().equals("CAPTURE") && (Math.abs(got.x-expected)>1e-3 || Math.abs(got.y)>1e-8 || Math.abs(got.z)>1e-8))throw new AssertionError("R3/R4 "+lane()+" tick="+body.tickCount+" steps="+steps(body)+" partial="+f+" expected="+expected+" got="+got+" native="+old+"->"+position);
        if(!CausalBodyResidual.rendererOffset(body,f).add(io.github.r3neer.scalebrews.client.collision.presentation.CausalRemoteResidual.rendererOffset(body,f)).equals(got) || CausalBodyResidual.cameraOffset(body,f).lengthSqr()>1e-20)throw new AssertionError("Remote query purity/camera ownership");
        System.out.println("S32 ADVERSARY NATIVE tick="+body.tickCount+" steps="+steps(body)+" partial="+f+" passiveReference="+expected+" observed="+got);
    }for(float bad:new float[]{Float.NaN,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY,-.01f,1.01f})
        if(io.github.r3neer.scalebrews.client.collision.presentation.CausalRemoteResidual.rendererOffset(body,bad).lengthSqr()>1e-20)throw new AssertionError("Invalid render fraction gained authority");
    if(!position.equals(body.position()) || !old.equals(body.oldPosition()) || !box.equals(body.getBoundingBox()) || !velocity.equals(body.getDeltaMovement()))throw new AssertionError("Visual read changed native physics");});}
}
