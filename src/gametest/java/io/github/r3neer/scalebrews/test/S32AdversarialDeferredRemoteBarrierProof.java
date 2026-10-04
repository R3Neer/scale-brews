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

/** Public reproduction: a queued remote original-model draw cannot retain retired passive carry. */
public final class S32AdversarialDeferredRemoteBarrierProof implements FabricClientGameTest {
    private static final List<AnatomyObserverCarryPayload> WIRE=new CopyOnWriteArrayList<>();
    private static volatile boolean armed;
    private static volatile int watched;
    private static String lane(){return "DRAW_GRAVITY";}
    public static int steps(Entity body) {
        var h=body.getInterpolation();if(h==null)return 0;
        try{var f=InterpolationHandler.class.getDeclaredField("interpolationData");f.setAccessible(true);var data=f.get(h);var n=data.getClass().getDeclaredField("steps");n.setAccessible(true);return n.getInt(data);}
        catch(ReflectiveOperationException e){throw new AssertionError("Native fixture",e);}
    }
    public static void afterBundle(ClientboundBundlePacket p){if(!armed)return;for(var sub:p.subPackets())if(sub instanceof ClientboundCustomPayloadPacket c && c.payload() instanceof AnatomyObserverCarryPayload mark && mark.bodyId()==watched)WIRE.add(mark);}
    @Override public void runTest(ClientGameTestContext context){
        WIRE.clear();armed=true;watched=-1;
        var geometry=context.computeOnClient(c->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of()).withModelTransform(new org.joml.Matrix4f().scaling(-1,-1,1).translate(0,-1.501f,0)));
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
            context.runOnClient(c->draw(c,bodyId.get(),target,true));
            System.out.println("S32 ADVERSARY DRAW "+lane()+" PASS");
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

    static final class Capture extends net.minecraft.client.renderer.StagedVertexBuffer {
        final List<Vec3> vertices=new ArrayList<>();
        Capture(){super(()->"S32 adversarial actual draw",65536);}
        @Override public com.mojang.blaze3d.vertex.VertexConsumer getVertexBuilder(Draw draw){
            return (com.mojang.blaze3d.vertex.VertexConsumer)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{com.mojang.blaze3d.vertex.VertexConsumer.class},(proxy,method,args)->{
                if(method.getName().equals("addVertex") && args.length>=3 && args[0] instanceof Number)vertices.add(new Vec3(((Number)args[0]).doubleValue(),((Number)args[1]).doubleValue(),((Number)args[2]).doubleValue()));
                return method.getReturnType()==com.mojang.blaze3d.vertex.VertexConsumer.class?proxy:null;
            });
        }
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void draw(net.minecraft.client.Minecraft c,int id,double target,boolean barrier){
        var body=(Cow)c.level.getEntity(id);var frame=AnatomyClientNetworking.presentationFrame(body).orElseThrow();
        if(AnatomyClientNetworking.presentationContact(body,c.level.getGameTime()).isEmpty())throw new AssertionError("Draw fixture has no confirmed contact");
        var pos=body.position();var old=body.oldPosition();var box=body.getBoundingBox();var velocity=body.getDeltaMovement();
        var raw=(net.minecraft.client.renderer.entity.LivingEntityRenderer)c.getEntityRenderDispatcher().getRenderer(body);
        for(float f:new float[]{0,.25f,.5f,.75f,1}){
            var state=(net.minecraft.client.renderer.entity.state.LivingEntityRenderState)raw.createRenderState();raw.extractRenderState(body,state,f);
            var offset=raw.getRenderOffset(state);
            double expectedResidual=target-(old.x+(pos.x-old.x)*f);
            if(Math.abs(offset.x-expectedResidual)>1e-3 || Math.abs(state.x-(old.x+(pos.x-old.x)*f))>1e-8)throw new AssertionError("Actual renderer native root/once-only offset mismatch");
            state.xRot=-61;state.yRot=54;state.walkAnimationPos=2.7f;state.walkAnimationSpeed=1;
            var pose=new com.mojang.blaze3d.vertex.PoseStack();pose.translate(state.x+offset.x,state.y+offset.y,state.z+offset.z);
            var storage=new net.minecraft.client.renderer.SubmitNodeStorage();raw.submit(state,pose,storage,new net.minecraft.client.renderer.state.level.CameraRenderState());
            if(!((io.github.r3neer.scalebrews.client.collision.presentation.CausalPresentationState)state).scalebrews$causalSubmitted())throw new AssertionError("Remote original model did not submit causal pose");
            var nodes=new ArrayList<net.minecraft.client.renderer.feature.submit.SubmitNode>();storage.order(0).solid.sortInto((node,strict)->nodes.add(node));
            var submit=nodes.stream().filter(n->n instanceof net.minecraft.client.renderer.feature.ModelFeatureRenderer.Submit<?> e && e.state()==state).map(n->(net.minecraft.client.renderer.feature.ModelFeatureRenderer.Submit<?>)n).findFirst().orElseThrow();
            var root=new Vec3(target,state.y+offset.y,state.z+offset.z);
            var expected=new org.joml.Matrix4f().translate((float)root.x,(float)root.y,(float)root.z).mul(frame.evaluated().rootTrs().matrix()).mul(frame.evaluated().modelTransform().matrix());
            if(!submit.pose().pose().equals(expected,2e-4f))throw new AssertionError("R5 remote queued root double-applies accepted own origin or loses passive carry: native="+state.x+" residual="+offset+" accepted="+frame.evaluated().origin()+" queued="+submit.pose().pose());
            try(var capture=new Capture()){
                new net.minecraft.client.renderer.feature.ModelFeatureRenderer().prepareGroup(new net.minecraft.client.renderer.feature.FeatureFrameContext(null,null,null,null,null,null,null,capture),List.of(submit),false);
                if(capture.vertices.isEmpty())throw new AssertionError("Vacuous delayed remote model draw");
                var delta=root.subtract(frame.evaluated().origin());
                for(var piece:frame.evaluated().pieces().values())for(var corner:piece.vertices())if(capture.vertices.stream().noneMatch(v->v.distanceToSqr(corner.add(delta))<1e-8))throw new AssertionError("R5 actual remote deferred vertices lose accepted joint/root material corner "+corner.add(delta));
                snapshot(body.tickCount,f,state.x,frame.evaluated().origin().x,offset.x,submit.pose().pose().m30(),capture.vertices,frame,delta);
            }
            if(barrier && lane().equals("DRAW_GRAVITY") && f==.5f){
                var prior=io.github.r3neer.scalebrews.integration.gravity.GravityFrames.observe(body);
                try{
                    io.github.r3neer.scalebrews.integration.gravity.GravityFrames.overrideForTests(body,new io.github.r3neer.scalebrews.collision.api.GravityFrame(net.minecraft.core.Direction.UP));
                    io.github.r3neer.scalebrews.integration.gravity.GravityFrames.observe(body);
                    if(io.github.r3neer.scalebrews.client.collision.presentation.CausalRemoteResidual.rendererOffset(body,f).lengthSqr()>1e-20)throw new AssertionError("R3 repaired gravity control failed");
                    try(var stale=new Capture()){
                        new net.minecraft.client.renderer.feature.ModelFeatureRenderer().prepareGroup(new net.minecraft.client.renderer.feature.FeatureFrameContext(null,null,null,null,null,null,null,stale),List.of(submit),false);
                        if(!stale.vertices.isEmpty()){
                            double expectedMin=frame.evaluated().pieces().values().stream().flatMap(p->p.vertices().stream()).mapToDouble(v->v.x).min().orElseThrow()+state.x-frame.evaluated().origin().x;
                            double observedMin=stale.vertices.stream().mapToDouble(v->v.x).min().orElseThrow();
                            System.out.println("S32 ADVERSARY delayed barrier nativeRoot="+state.x+" retiredCurrentOffset="+io.github.r3neer.scalebrews.client.collision.presentation.CausalRemoteResidual.rendererOffset(body,f)+" oldQueuedRoot="+submit.pose().pose().m30()+" observedMinX="+observedMin+" zeroResidualReferenceMinX="+expectedMin);
                            if(Math.abs(expectedMin-observedMin)>1e-4)throw new AssertionError("R5 queued remote draw retains retired passive carry after observed gravity barrier: staleX="+(observedMin-expectedMin));
                        }
                    }
                }finally{io.github.r3neer.scalebrews.integration.gravity.GravityFrames.overrideForTests(body,prior.frame());io.github.r3neer.scalebrews.integration.gravity.GravityFrames.observe(body);io.github.r3neer.scalebrews.integration.gravity.GravityFrames.clearOverrideForTests(body);}
                return;
            }
        }
        if(!pos.equals(body.position()) || !old.equals(body.oldPosition()) || !box.equals(body.getBoundingBox()) || !velocity.equals(body.getDeltaMovement()))throw new AssertionError("Actual model draw changed native physics");
        System.out.println("S32 ADVERSARY DRAW tick="+body.tickCount+" steps="+steps(body)+" exact root+joints at five partials");
    }
    private static void snapshot(int tick,float partial,double nativeX,double acceptedX,double residualX,double queuedX,List<Vec3> actual,AnatomyClientNetworking.PresentationFrame frame,Vec3 delta){
        try{
            var p=java.nio.file.Path.of("s32-adversarial-remote-draw.csv");
            if(!java.nio.file.Files.exists(p))java.nio.file.Files.writeString(p,"tick,partial,kind,x,y,z,native_x,accepted_x,residual_x,queued_x\n");
            var out=new StringBuilder();for(var v:actual)out.append(tick+","+partial+",actual,"+v.x+","+v.y+","+v.z+","+nativeX+","+acceptedX+","+residualX+","+queuedX+"\n");
            for(var piece:frame.evaluated().pieces().values())for(var corner:piece.vertices()){var v=corner.add(delta);out.append(tick+","+partial+",reference,"+v.x+","+v.y+","+v.z+","+nativeX+","+acceptedX+","+residualX+","+queuedX+"\n");}
            java.nio.file.Files.writeString(p,out.toString(),java.nio.file.StandardOpenOption.APPEND);
        }catch(java.io.IOException e){throw new AssertionError("Snapshot fixture",e);}
    }
}
