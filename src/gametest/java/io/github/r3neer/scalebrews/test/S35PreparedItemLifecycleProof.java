package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.MaterialIntervalRuntime;
import io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime;
import io.github.r3neer.scalebrews.platform.Platforms;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.Vec3;

/** S35: real item movement and vanilla lifecycle inside the prepared anatomy runtime. */
final class S35PreparedItemLifecycleProof {
    private S35PreparedItemLifecycleProof() {}

    static void run(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        ItemEntity item=null,control=null,pickup=null,expiry=null;
        try {
            Platforms.tick(level);
            var frame=AnatomyMovement.queryFrame(support).orElseThrow();
            var back=frame.snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(back!=null,"S35 requires a real prepared cow body piece");
            var bounds=back.bounds();
            item=new ItemEntity(level,bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z,new ItemStack(Items.STONE));
            item.setPickUpDelay(3);
            h.assertTrue(level.addFreshEntity(item),"S35 inserts one ordinary vanilla item");
            control=new ItemEntity(level,bounds.getCenter().x+12,bounds.maxY+.8,bounds.getCenter().z,
                new ItemStack(Items.STONE));
            control.setPickUpDelay(3);
            h.assertTrue(level.addFreshEntity(control),"S35 inserts a distant ordinary item control");
            h.assertTrue(AnatomyApi.ready(item) && Platforms.eligible(item,support),"The prepared catalog admits the item/support pair");
            item.move(MoverType.SELF,new Vec3(0,-1.5,0));
            var contact=AnatomyMovement.contact(item);var surface=AnatomyMovement.surface(item);
            h.assertTrue(contact!=null && contact.support()==support && surface!=null
                    && contact.revision()==frame.snapshot().revision() && contact.piece().equals(surface.piece())
                    && surface.normal().y>.9 && Platforms.supported(item),
                "Real ItemEntity.move acquires an identified upward material contact: contact="+contact+" surface="+surface);
            h.assertTrue(TestRetiredPlatformRuntime.absent(),"Canonical item contact never enters PlatformState");
            int age=item.getAge();
            item.tick();control.tick();
            h.assertTrue(item.getAge()==age+1 && item.getAge()==control.getAge()
                    && item.hasPickUpDelay() && control.hasPickUpDelay() && Platforms.supported(item)
                    && !Platforms.supported(control),
                "ItemEntity.tick advances vanilla age/pickup delay like a nonparticipant while retaining material contact");

            var local=surface.localPoint();var beforePosition=item.position();
            var beforeMaterial=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(surface.piece());
            var capture=MaterialIntervalRuntime.captureRoot(support);
            var metricsBefore=MaterialPhysicsRuntime.metrics(level);
            support.setPos(support.position().add(.2,.1,.15));
            MaterialIntervalRuntime.commitRoot(support,capture);
            var afterMaterial=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(surface.piece());
            var expected=afterMaterial.point(local).subtract(beforeMaterial.point(local));
            MaterialPhysicsRuntime.drain(level);
            var metricsAfter=MaterialPhysicsRuntime.metrics(level);
            h.assertTrue(item.position().distanceToSqr(beforePosition.add(expected))<1e-8
                    && AnatomyMovement.contact(item)!=null,
                "One certified ROOT interval transports the item at its material point: expected="+expected
                    +" actual="+item.position().subtract(beforePosition)+" before="+metricsBefore+" after="+metricsAfter
                    +" contact="+AnatomyMovement.contact(item)+" supported="+Platforms.supported(item));
            var once=item.position();MaterialPhysicsRuntime.drain(level);
            h.assertTrue(item.position().equals(once),"A second drain cannot duplicate the item ROOT carry");
            var rotatedSurface=AnatomyMovement.surface(item);
            h.assertTrue(rotatedSurface!=null && Platforms.supported(item),
                "The translated item retains a usable material anchor for a later ROOT interval");
            var rotatedBefore=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(rotatedSurface.piece());
            var beforeYawPosition=item.position();
            var yawCapture=MaterialIntervalRuntime.captureRoot(support);
            support.yBodyRot=yawCapture.root().yaw()+10;
            MaterialIntervalRuntime.commitRoot(support,yawCapture);
            var rotatedAfter=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(rotatedSurface.piece());
            var yawDelta=rotatedAfter.point(rotatedSurface.localPoint()).subtract(rotatedBefore.point(rotatedSurface.localPoint()));
            MaterialPhysicsRuntime.drain(level);
            h.assertTrue(item.position().distanceToSqr(beforeYawPosition.add(yawDelta))<1e-8
                    && Platforms.supported(item),
                "A later nonidentity ROOT yaw transports the same item once at its retained local point: expected="
                    +yawDelta+" actual="+item.position().subtract(beforeYawPosition));
            item.tick();control.tick();
            h.assertTrue(item.getAge()==age+2 && item.getAge()==control.getAge()
                    && item.hasPickUpDelay()==control.hasPickUpDelay(),
                "The next real item tick preserves ordinary age and pickup-delay cadence after ROOT carry");
            h.assertTrue(item.onGround() && !control.onGround() && !item.isInWater() && !control.isInWater(),
                "Ground-friction comparison requires one dry material-ground item and one dry airborne control");
            float ordinaryFriction=level.getBlockState(item.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction();
            double materialFriction=Platforms.friction(item,ordinaryFriction);
            item.setDeltaMovement(new Vec3(.1,0,0));control.setDeltaMovement(new Vec3(.1,0,0));
            item.tick();control.tick();
            h.assertTrue(item.getAge()==age+3 && item.getAge()==control.getAge()
                    && Math.abs(item.getDeltaMovement().x-control.getDeltaMovement().x*materialFriction)<1e-5
                    && Platforms.supported(item),
                "Real dry ItemEntity.tick applies the canonical ground friction while preserving vanilla age: "
                    +"material="+item.getDeltaMovement()+" control="+control.getDeltaMovement()
                    +" friction="+materialFriction);

            var currentPiece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(surface.piece());
            var pickupBounds=currentPiece.bounds();
            pickup=new ItemEntity(level,pickupBounds.getCenter().x,pickupBounds.maxY+.8,
                pickupBounds.getCenter().z,new ItemStack(Items.STONE));
            h.assertTrue(level.addFreshEntity(pickup),"S35 inserts a second ordinary item for supported pickup");
            pickup.move(MoverType.SELF,new Vec3(0,-1.5,0));
            h.assertTrue(Platforms.supported(pickup),"Supported pickup item acquires a real material contact");
            var collector=h.makeMockPlayer(GameType.SURVIVAL);
            int beforeInventory=collector.getInventory().countItem(Items.STONE);
            pickup.playerTouch(collector);
            h.assertTrue(pickup.isRemoved() && AnatomyMovement.contact(pickup)==null
                    && collector.getInventory().countItem(Items.STONE)==beforeInventory+1,
                "A supported ordinary item is collected and retires its material contact through vanilla playerTouch");

            support.discard();
            h.assertTrue(AnatomyMovement.contact(item)==null && !Platforms.supported(item)
                    && TestRetiredPlatformRuntime.absent(),
                "Retiring the canonical support releases the item without a legacy owner");
            double releasedY=item.getY();
            item.setDeltaMovement(Vec3.ZERO);
            // Direct tick() calls do not advance Entity.tickCount; the server
            // dispatcher normally does. Model that clock for vanilla's quiet
            // onGround item movement cadence, which also uses the entity id.
            item.tickCount++;control.tickCount++;
            item.tick();control.tick();
            h.assertTrue(item.getDeltaMovement().y<-.039 && item.getAge()==control.getAge()
                    && AnatomyMovement.contact(item)==null,
                "The first real tick after release resumes native gravity and age without stale material contact");
            // Vanilla 26.2 defers move for a quiet onGround item except when
            // (tickCount + entity id) % 4 == 0. The old one-tick position
            // assertion depended on the randomly assigned GameTest entity id.
            for(int tick=0;tick<3 && item.getY()>=releasedY-.02;tick++) {
                item.tickCount++;control.tickCount++;
                item.tick();control.tick();
            }
            h.assertTrue(item.getY()<releasedY-.02 && item.getAge()==control.getAge()
                    && AnatomyMovement.contact(item)==null,
                "Released item falls within the native four-tick movement cadence: "
                    +"y="+item.getY()+" releasedY="+releasedY+" age="+item.getAge()
                    +" controlAge="+control.getAge()+" tickCount="+item.tickCount);
            item.setPickUpDelay(0);
            item.playerTouch(collector);
            h.assertTrue(item.isRemoved() && collector.getInventory().countItem(Items.STONE)==beforeInventory+2,
                "Released ordinary item enters a survival inventory through vanilla playerTouch");

            expiry=new ItemEntity(level,bounds.getCenter().x+24,bounds.maxY+.8,bounds.getCenter().z,
                new ItemStack(Items.STONE));
            h.assertTrue(level.addFreshEntity(expiry),"S35 inserts one ordinary near-expiry item");
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());
            expiry.saveWithoutId(saved);
            var tag=saved.buildResult();tag.putShort("Age",(short)5999);
            expiry.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag));
            h.assertTrue(expiry.getAge()==5999 && !expiry.isRemoved(),
                "Vanilla persistence seeds an ordinary item one tick before expiry");
            expiry.tick();
            h.assertTrue(expiry.getAge()==6000 && expiry.isRemoved(),
                "Released ordinary item retires through Minecraft's own age threshold");
        } finally {
            if(item!=null)item.discard();
            if(control!=null)control.discard();
            if(pickup!=null)pickup.discard();
            if(expiry!=null)expiry.discard();
            support.discard();
        }
    }
}
