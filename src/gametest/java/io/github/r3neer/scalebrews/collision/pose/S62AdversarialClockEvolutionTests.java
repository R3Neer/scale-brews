package io.github.r3neer.scalebrews.collision.pose;

import com.google.gson.Gson;
import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.physics.SupportTransport;
import io.github.r3neer.scalebrews.collision.runtime.TransportLedger;
import java.nio.file.Files;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Independent original-method differential; manually applied ledger inputs, not contact/renderer proof. */
public final class S62AdversarialClockEvolutionTests {
    private static void check(boolean b,String why){if(!b)throw new AssertionError("S62 ADV "+why);}
    private static void bits(float a,float b,String why){check(Float.floatToRawIntBits(a)==Float.floatToRawIntBits(b),why+" expected="+a+" actual="+b);}
    private static LivingPoseInputSource.Bound source(LivingEntity a){return LivingPoseInputSource.bind(a.getClass(),new LivingPoseInputSource.Program(1,a.getClass().getName(),List.of(new LivingPoseInputSource.Channel("phase",LivingPoseInputSource.Source.WALK_POSITION),new LivingPoseInputSource.Channel("amount",LivingPoseInputSource.Source.WALK_SPEED))));}
    private static LivingEntity cow(GameTestHelper h){return EntityTypes.COW.create(h.getLevel(),EntitySpawnReason.COMMAND);}
    @GameTest public void originalLawAcrossFramesAndScale(GameTestHelper helper)throws Exception{
        var rows=new ArrayList<Map<String,Object>>();int samples=0,reads=0;
        for(var type:List.of(EntityTypes.COW,EntityTypes.PIG))for(boolean baby:List.of(false,true))for(double scale:new double[]{.25,.5,1,2,4})for(var direction:Direction.values()){
            LivingEntity actor=type.create(helper.getLevel(),EntitySpawnReason.COMMAND),control=type.create(helper.getLevel(),EntitySpawnReason.COMMAND);
            ((AgeableMob)actor).setAge(baby?-1200:0);((AgeableMob)control).setAge(baby?-1200:0);
            actor.getAttribute(Attributes.SCALE).setBaseValue(scale);control.getAttribute(Attributes.SCALE).setBaseValue(scale);
            actor.setPos(8,8,8);control.setPos(8,8,8);actor.walkAnimation.update(.75f,.4f,2f);
            float originalPhase=actor.walkAnimation.position(1f),originalSpeed=actor.walkAnimation.speed(1f);
            var clock=new LivingLocomotionClock();var frame=new GravityFrame(direction);var bound=source(actor);
            clock.advance(actor,1000,frame);control.walkAnimation.stop();long sequence=0;
            for(int step=0;step<12;step++){
                double x=step%4==0?0:step%4==1?.0625:step%4==2?-.125:.5;
                double z=step%3==0?.125:0;
                var own=new Vec3(x,step%2==0?.25:0,z);var passive=new Vec3(.125,0,-.0625);
                var applied=frame.toWorld(passive);actor.setPos(actor.position().add(frame.toWorld(own)).add(applied));
                TransportLedger.record(actor,new SupportTransport(helper.getLevel().getGameTime(),++sequence,step,applied,applied));
                var old=control.position();control.xo=old.x;control.yo=old.y;control.zo=old.z;control.setPos(old.add(x,0,z));control.calculateEntityAnimation(false);
                var current=clock.advance(actor,1001+step,frame);check(current.available(),"original sequence unavailable");
                bits(control.walkAnimation.position(1f),current.position(),"native phase");bits(control.walkAnimation.speed(1f),current.speed(),"native speed");
                for(int q=0;q<25;q++){
                    check(clock.advance(actor,1001+step,frame)==current,"same tick advanced");var values=bound.sample(actor,current,1001+step).orElseThrow();
                    bits(current.position(),values.get("phase"),"frame phase projection");bits(current.speed(),values.get("amount"),"frame speed projection");reads++;
                }
                bits(originalPhase,actor.walkAnimation.position(1f),"entity presentation phase contaminated");bits(originalSpeed,actor.walkAnimation.speed(1f),"entity presentation speed contaminated");
                if(scale==.5&&direction==Direction.DOWN)rows.add(Map.of("actor",actor.getClass().getSimpleName(),"baby",baby,"step",step,"nativePhase",control.walkAnimation.position(1f),"candidatePhase",current.position(),"nativeSpeed",control.walkAnimation.speed(1f),"candidateSpeed",current.speed()));
                samples++;
            }
            actor.discard();control.discard();
        }
        var dir=FabricLoader.getInstance().getGameDir().resolve("s62-adversarial");Files.createDirectories(dir);Files.writeString(dir.resolve("clock-sequence.json"),new Gson().toJson(rows));
        System.out.println("S62_ADV_ORIGINAL_CLOCK_PASS samples="+samples+" repeatedSamples="+reads+" scalarBits="+(samples*2)+" sourceReadOnly=true manualLedger=true");helper.succeed();
    }
    @GameTest public void barriersIdentityAndThread(GameTestHelper helper)throws Exception{
        var actor=cow(helper);actor.setPos(4,4,4);var clock=new LivingLocomotionClock();clock.advance(actor,10,GravityFrame.VANILLA);actor.setPos(4.25,4,4);
        var frame=clock.advance(actor,11,GravityFrame.VANILLA);check(frame.speed()>0,"nonvacuous walking baseline");
        var bound=source(actor);check(bound.sample(actor,frame,11).isPresent(),"positive source");
        var threadResult=new ArrayList<Boolean>();var thread=new Thread(()->{threadResult.add(!frame.validFor(actor,11));threadResult.add(bound.sample(actor,frame,11).isEmpty());try{clock.advance(actor,12,GravityFrame.VANILLA);threadResult.add(false);}catch(IllegalArgumentException expected){threadResult.add(true);}});thread.start();thread.join();check(threadResult.equals(List.of(true,true,true)),"off-thread admitted");check(clock.current().orElseThrow()==frame,"off-thread changed owner");
        TransportLedger.invalidate(actor,false);check(bound.sample(actor,frame,11).isEmpty(),"retired ledger frame revived");var reset=clock.advance(actor,11,GravityFrame.VANILLA);check(reset!=frame&&reset.speed()==0&&reset.position()==0,"same-tick barrier not rebased");
        var other=cow(helper);check(bound.sample(other,reset,11).isEmpty(),"foreign actor accepted");try{clock.advance(other,12,GravityFrame.VANILLA);throw new AssertionError("S62 ADV foreign actor mutated clock");}catch(IllegalArgumentException expected){}
        try{clock.advance(actor,9,GravityFrame.VANILLA);throw new AssertionError("S62 ADV rewind admitted");}catch(IllegalArgumentException expected){}
        actor.setPos(5,4,4);var recovered=clock.advance(actor,12,GravityFrame.VANILLA);check(recovered.speed()>0,"barrier recovery failed");check(!reset.validFor(actor,11),"historical frame accepted");
        actor.setPos(10,4,4);var jump=clock.advance(actor,13,GravityFrame.VANILLA);check(jump.speed()==0,"jump debt");actor.setPos(10.125,4,4);var resumed=clock.advance(actor,14,GravityFrame.VANILLA);check(resumed.speed()>0,"jump recovery");
        var gap=clock.advance(actor,16,GravityFrame.VANILLA);check(gap.speed()==0,"gap debt");actor.discard();check(bound.sample(actor,gap,16).isEmpty(),"removed actor frame admitted");other.discard();helper.succeed();
    }
}
