package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.internal.AuthorityPoseTracker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;

/** Independent callback/failure publication test, not original renderer proof. */
public final class S62AdversarialPublicationTests {
    static final class ProbeCow extends Cow {
        Runnable onAlive,onPitch;
        ProbeCow(Level level){super(EntityTypes.COW,level);}
        @Override public boolean isAlive(){var op=onAlive;onAlive=null;if(op!=null)op.run();return super.isAlive();}
        @Override public float getXRot(){var op=onPitch;onPitch=null;if(op!=null)op.run();return super.getXRot();}
    }
    static void check(boolean b,String why){if(!b)throw new AssertionError("S62 ADV "+why);}
    @GameTest public void advanceCallbackCannotReadOrReenter(GameTestHelper helper){
        var a=new ProbeCow(helper.getLevel());a.setPos(4,4,4);var clock=new LivingLocomotionClock();var old=clock.advance(a,1,GravityFrame.VANILLA);
        a.setPos(4.125,4,4);var observed=new boolean[2];a.onAlive=()->{
            observed[0]=!old.validFor(a,1);
            try{clock.advance(a,2,GravityFrame.VANILLA);}catch(IllegalStateException expected){observed[1]=true;}
        };
        var current=clock.advance(a,2,GravityFrame.VANILLA);check(observed[0]&&observed[1],"provisional frame/reentrant advance admitted");check(current.validFor(a,2)&&current.speed()>0,"normal outer advance failed");a.discard();helper.succeed();
    }
    @GameTest public void getterFailureRetiresPublicState(GameTestHelper helper){
        for(boolean error:new boolean[]{false,true}){
            var a=new ProbeCow(helper.getLevel());a.setPos(4,4,4);var tracker=new AuthorityPoseTracker();var previous=tracker.tick(a,100,true);check(previous.ordinary(),"positive tracker baseline");
            a.setPos(4.125,4,4);a.onPitch=()->{check(tracker.current(a).isEmpty()&&tracker.locomotion(a).isEmpty(),"provisional tracker state exposed");if(error)throw new AssertionError("injected pitch error");throw new IllegalStateException("injected pitch runtime");};
            boolean failed=false;try{tracker.tick(a,101,true);}catch(RuntimeException expected){check(!error,"wrong failure category");failed=true;}catch(AssertionError expected){check(error&&expected.getMessage().equals("injected pitch error"),"wrong failure witness");failed=true;}
            check(failed,"failure injection did not fire");check(tracker.current(a).isEmpty(),"getter "+(error?"Error":"RuntimeException")+" leaves stale public inputs");check(tracker.locomotion(a).isEmpty(),"getter failure leaves public clock frame");
            check(tracker.tick(a,101,true).ordinary(),"same-tick recovery after failed getter unavailable");a.discard();
        }
        helper.succeed();
    }
}
