package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.runtime.TransportLedger;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;

/** Independent authority-frame admission oracle; explicit manual source, not original class proof. */
public final class S62AdversarialFrameAdmissionTests {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("S62 ADV " + message);
    }
    public static final class CallbackCow extends Cow {
        Runnable onName;
        CallbackCow(Level level) { super(EntityTypes.COW, level); }
        @Override public Component getCustomName() {
            Runnable callback=onName; onName=null;
            if(callback!=null)callback.run();
            return super.getCustomName();
        }
    }
    @GameTest public void retiredFrameCannotCommitDuringSourceRead(GameTestHelper helper) {
        var actor=new CallbackCow(helper.getLevel()); actor.setPos(5,5,5);
        var clock=new LivingLocomotionClock();
        var frame=clock.advance(actor,100,GravityFrame.VANILLA);
        var source=LivingPoseInputSource.bind(CallbackCow.class,new LivingPoseInputSource.Program(1,
            CallbackCow.class.getName(),List.of(
                new LivingPoseInputSource.Channel("age",LivingPoseInputSource.Source.AGE_IN_TICKS),
                new LivingPoseInputSource.Channel("name",LivingPoseInputSource.Source.IS_UPSIDE_DOWN))));
        check(frame.validFor(actor,100),"positive frame was invalid before callback");
        check(source.sample(actor,frame,100).isPresent(),"positive sample absent");
        actor.onName=()->TransportLedger.invalidate(actor,false);
        var after=source.sample(actor,frame,100);
        check(!frame.validFor(actor,100),"callback did not retire authority generation");
        check(after.isEmpty(),"retired frame admitted during source read; published="+after);
        helper.succeed();
    }
}
