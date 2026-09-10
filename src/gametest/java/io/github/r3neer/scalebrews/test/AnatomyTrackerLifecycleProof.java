package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.platform.anatomy.*;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Isolated lifecycle proof: it owns the one active anatomy level across delayed
 * callbacks, so ordinary and prepared suites cannot deactivate it underneath a sample.
 */
public final class AnatomyTrackerLifecycleProof {
    @GameTest(maxTicks=80) public void consumesPassiveTransportAcrossSamples(GameTestHelper h) {
        var support=h.spawn(EntityTypes.COW,2,20,2);
        var body=h.spawn(EntityTypes.COW,2,23,2);
        var cleaned=new AtomicBoolean();
        Runnable cleanup=()->{
            if(cleaned.compareAndSet(false,true)) {
                AnatomyMovement.deactivate(h.getLevel());support.discard();body.discard();
            }
        };
        // 26.2 schedules this at timeoutTicks - 1. Every callback below also
        // invokes it on failure, so an expired asynchronous test cannot leave
        // the level's anatomy state active for a following test.
        h.runBeforeTestEnd(cleanup);
        try {
            support.setNoAi(true);support.setNoGravity(true);body.setNoAi(true);body.setNoGravity(true);
            support.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(3);support.refreshDimensions();
            body.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
            GeometryProvider provider=e->java.util.Optional.of(new GeometryProvider.Snapshot(1,java.util.Map.of("back",
                ConvexBox.of(new AABB(-2,0,-2,2,1,2),new Matrix4f()).move(e.position()))));
            AnatomyMovement.activate(h.getLevel());AnatomyMovement.register(support,provider);
            var tracker=new AuthorityPoseTracker();
            var box=provider.sample(support).orElseThrow().pieces().get("back");
            body.setPos(box.bounds().getCenter().x,box.bounds().maxY,box.bounds().getCenter().z);
            var surface=new SurfaceContact(support.getUUID(),1,"back",3,
                box.facePoint(3,body.getBoundingBox().getCenter()),box.faceNormal(3),h.getLevel().getGameTime());
            h.assertTrue(AnatomyMovement.confirm(body,support,surface),"Passive cow establishes a real anatomical material anchor");
            tracker.tick(body,h.getLevel().getGameTime(),true);
            support.setPos(support.position().add(.2,0,0));AnatomyMovement.carry(body);
            final int[] samples={0};final Runnable[] next=new Runnable[1];
            next[0]=()->h.runAfterDelay(1,()->{
                try {
                    body.setPos(body.position().add(.1,0,0));
                    var pose=tracker.tick(body,h.getLevel().getGameTime(),true);
                    samples[0]++;
                    if(samples[0]==1)h.assertTrue(Math.abs(pose.walkAmount()-.16f)<1e-6,
                        "Carry applied at END before this sample is removed from passive locomotion");
                    if(samples[0]<42){next[0].run();return;}
                    h.assertTrue(pose.walkAmount()>.01f,"Own tangent walk remains live after the 40-tick transport history expires");
                    AnatomyMovement.invalidateRoot(body);
                    h.assertTrue(AnatomyMovement.contact(body)==null && !AnatomyMovement.supported(body),
                        "Small root discontinuity clears the body's own material anchor");
                    body.setPos(body.position().add(.2,0,0));
                    h.runAfterDelay(1,()->{
                        try {
                            body.setPos(body.position().add(.1,0,0));
                            var afterTeleport=tracker.tick(body,h.getLevel().getGameTime(),true);
                            h.assertTrue(afterTeleport.walkAmount()==0,"Small teleport resets locomotion without a transport sequence change");
                            h.runAfterDelay(1,()->{
                                try {
                                    body.setPos(body.position().add(.1,0,0));
                                    var resumed=tracker.tick(body,h.getLevel().getGameTime(),true);
                                    h.assertTrue(resumed.walkAmount()>.01f,"Ordinary walking resumes after the explicit teleport reset");
                                    cleanup.run();h.succeed();
                                } catch(Throwable failure) {cleanup.run();throw failure;}
                            });
                        } catch(Throwable failure) {cleanup.run();throw failure;}
                    });
                } catch(Throwable failure) {cleanup.run();throw failure;}
            });
            next[0].run();
        } catch(Throwable failure) {cleanup.run();throw failure;}
    }
}
