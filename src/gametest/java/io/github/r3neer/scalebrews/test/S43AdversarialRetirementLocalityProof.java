package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.geometry.*;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.platform.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.*;
import java.util.*;

public final class S43AdversarialRetirementLocalityProof implements FabricClientGameTest {
 static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static Cow cow(ServerLevel level,double scale,Vec3 pos){var e=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);require(e!=null,"Fixture cow absent");e.setNoAi(true);e.setNoGravity(true);e.getAttribute(Attributes.SCALE).setBaseValue(scale);e.refreshDimensions();e.setYRot(0);e.yBodyRot=0;e.yHeadRot=0;e.setPos(pos);level.addFreshEntity(e);return e;}
 static Vec3[] impulse(Entity a,Entity b,boolean reverse){a.setDeltaMovement(Vec3.ZERO);b.setDeltaMovement(Vec3.ZERO);if(reverse)b.push(a);else a.push(b);return new Vec3[]{a.getDeltaMovement(),b.getDeltaMovement()};}
 static void equal(Vec3[] a,Vec3[] b,String label){for(int i=0;i<a.length;i++)require(a[i].distanceToSqr(b[i])<1e-18,label+" actor="+i+" expected="+Arrays.toString(a)+" actual="+Arrays.toString(b));}
 @Override public void runTest(ClientGameTestContext c){
  var model=c.computeOnClient(mc->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of()));
  var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),Optional.of(new AnatomyDefinition(Identifier.parse(model.source()),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
  var world=c.worldBuilder().create();var server=world.getServer();int[] ids=new int[4];Vec3[][] baseline=new Vec3[4][];
  try {
   server.runOnServer(s->{var l=s.overworld();l.setChunkForced(0,0,true);
    var support=cow(l,8,new Vec3(2,30,2));var body=cow(l,1,new Vec3(2.2,48,2.1));
    var rejectSupport=cow(l,1,new Vec3(20,30,2));var rejectBody=cow(l,1,new Vec3(20.2,30,2.1));
    ids[0]=support.getId();ids[1]=body.getId();ids[2]=rejectSupport.getId();ids[3]=rejectBody.getId();
    baseline[0]=impulse(support,body,false);baseline[1]=impulse(support,body,true);
    baseline[2]=impulse(rejectSupport,rejectBody,false);baseline[3]=impulse(rejectSupport,rejectBody,true);
    require(baseline[0][0].lengthSqr()>0 && baseline[0][1].lengthSqr()>0,"Native control is vacuous");
    for(int id:ids)l.getEntity(id).setDeltaMovement(Vec3.ZERO);
    AnatomyRuntime.startPrepared(s,Map.of(model.source(),model),Map.of("test:s43_private",profile));
   });
   c.waitTicks(6);
   server.runOnServer(s->{var l=s.overworld();var support=(Cow)l.getEntity(ids[0]);var body=(Cow)l.getEntity(ids[1]);var rejected=(Cow)l.getEntity(ids[3]);var rejectedSupport=(Cow)l.getEntity(ids[2]);
    require(AnatomyApi.ready(support),"READY binding missing");require(!Platforms.eligible(rejected,rejectedSupport),"Ratio negative actually eligible");
    rejectedSupport.setPos(20,30,2);rejected.setPos(20.2,30,2.1);
    equal(baseline[2],impulse(rejectedSupport,rejected,false),"Rejected forward native push");equal(baseline[3],impulse(rejectedSupport,rejected,true),"Rejected reverse native push");
    rejected.setDeltaMovement(Vec3.ZERO);rejected.setPos(18,30.4,2);var before=rejected.position();rejected.move(MoverType.SELF,new Vec3(4,0,0));
    require(rejected.position().distanceToSqr(before.add(4,0,0))<1e-16,"Rejected pair acquired anatomical wall: "+rejected.position());require(AnatomyMovement.contact(rejected)==null,"Rejected pair acquired contact");
    require(AnatomyApi.raycast(rejected,new Vec3(18,30.7,2),new Vec3(22,30.7,2)).isEmpty(),"Rejected pair acquired scoped ray target");
    var shape=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get("root/body/cube_0");body.setDeltaMovement(Vec3.ZERO);body.setPos(support.getX()+.2,shape.bounds().maxY+.02,support.getZ()+.1);body.move(MoverType.SELF,new Vec3(0,-.1,0));
    require(AnatomyMovement.contact(body)!=null,"Positive failed to acquire real material relation");
    equal(baseline[0],impulse(support,body,false),"Eligible forward native push");equal(baseline[1],impulse(support,body,true),"Eligible reverse native push");
    body.setDeltaMovement(Vec3.ZERO);support.setDeltaMovement(Vec3.ZERO);var carryBefore=body.position();support.move(MoverType.SELF,new Vec3(.1,0,0));MaterialPhysicsRuntime.drain(l);
    require(body.position().distanceToSqr(carryBefore.add(.1,0,0))<1e-14,"Material carry missing or duplicated: before="+carryBefore+" actual="+body.position());
    equal(baseline[0],impulse(support,body,false),"Post-carry native push");
    body.setDeltaMovement(Vec3.ZERO);support.setDeltaMovement(Vec3.ZERO);
    var oldTransport=AnatomyMovement.transport(body);
    var recipientRefs=new ArrayList<net.minecraft.server.level.ServerPlayer>();
    for(int r=0;r<128;r++) {
     var recipient=new net.minecraft.server.level.ServerPlayer(s,l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"cost-"+r),net.minecraft.server.level.ClientInformation.createDefault());recipient.setPos(100+r,30,100);recipientRefs.add(recipient);
     long tick=l.getGameTime(),revision=AnatomyNetworking.revision(s);var normal=new Vec3(0,1,0);
     var rc=new AnatomyMovement.Contact(support,"piece",revision,normal,1);
     var surface=new io.github.r3neer.scalebrews.collision.api.SurfaceContact(support.getUUID(),revision,"piece",3,new Vec3(.5,1,.5),normal,tick);
     var root=new io.github.r3neer.scalebrews.collision.runtime.RootFrame(1,tick,support.position(),0,1,io.github.r3neer.scalebrews.collision.api.GravityFrame.VANILLA);
     var material=ConvexBox.of(new AABB(-1,0,-1,1,1,1),new org.joml.Matrix4f()).move(support.position());var delta=new Vec3(.125,0,0);recipient.setPos(recipient.position().add(delta));
     var transport=new io.github.r3neer.scalebrews.collision.physics.SupportTransport(tick,1,root.sequence(),delta,delta);
     S24TrackingAuthorityTestSeam.run(recipient,recipient,1,()->AnatomyTransportReceipts.record(recipient,rc,surface,root,101,transport,material,material.move(delta)));
     require(AnatomyTransportReceipts.history(recipient,recipient.getUUID()).size()==1,"Unrelated receipt was not actually recorded");
    }
    S43RetirementVisits.foreign.set(0);S43RetirementVisits.active=true;
    try {support.getAttribute(Attributes.SCALE).setBaseValue(.5);support.refreshDimensions();}finally{S43RetirementVisits.active=false;}
    require(AnatomyMovement.contact(body)==null,"Retirement did not run");
    require(S43RetirementVisits.foreign.get()==0,"Local dimension retirement visited unrelated receipt recipients: "+S43RetirementVisits.foreign.get()+" for a Cow with zero recipient receipts");
    require(!Platforms.eligible(body,support),"Effective scale transition failed to reject actual widths");
    var afterScale=body.position();support.move(MoverType.SELF,new Vec3(.13,0,0));MaterialPhysicsRuntime.drain(l);
    var afterTransport=AnatomyMovement.transport(body);
    require(body.position().distanceToSqr(afterScale)<1e-16,"Rejected scale transition still carried body: before="+afterScale+" actual="+body.position()+" contact="+AnatomyMovement.contact(body));
    require(AnatomyMovement.contact(body)==null,"Support-scale rejection retained material relation: support="+AnatomyApi.support(body)+" contact="+AnatomyMovement.contact(body));
    require(afterTransport==null || oldTransport!=null && afterTransport.sequence()==oldTransport.sequence(),"Rejected scale generated a new carry receipt");
    System.out.println("S43_ADVERSARY_SCALE_RETIRE_PASS ratio="+(body.getBbWidth()/(double)support.getBbWidth()));
    System.out.println("S43_ADVERSARY_PAIR_PASS ratio="+(body.getBbWidth()/(double)support.getBbWidth())+" negativeRatio="+(rejected.getBbWidth()/(double)rejectedSupport.getBbWidth())+" transport="+AnatomyMovement.transport(body));
   });
  }finally {server.runOnServer(AnatomyRuntime::stop);world.close();}
 }
}
