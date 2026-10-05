package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.MaterialIntervalRuntime;
import io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real off-rail cart movement on the prepared canonical support. */
final class S37PreparedMinecartProof {
    private S37PreparedMinecartProof() {}

    static void run(GameTestHelper h) {
        List<EntityType<? extends AbstractMinecart>> variants=List.of(
            EntityTypes.MINECART,EntityTypes.CHEST_MINECART,EntityTypes.FURNACE_MINECART);
        for(var variant:variants)runOffrail(h,variant);
        runRailHandoff(h);
        runOversizeControl(h);
        runBlockedCarry(h);
    }

    private static void runOffrail(GameTestHelper h,EntityType<? extends AbstractMinecart> variant) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        AbstractMinecart cart=null;
        try {
            Platforms.tick(level);
            var frame=AnatomyMovement.queryFrame(support).orElseThrow();
            var piece=frame.snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(piece!=null,"Prepared cow body required for minecart proof");
            var bounds=piece.bounds();
            cart=variant.create(level,EntitySpawnReason.COMMAND);
            h.assertTrue(cart!=null,"Real minecart variant must spawn: "+variant);
            h.assertTrue(cart.getType()==variant,
                "Prepared material selection preserves native minecart variant identity");
            if(cart instanceof AbstractMinecartContainer chest)
                chest.setItem(0,new ItemStack(Items.STONE,3));
            cart.setPos(bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z);
            h.assertTrue(level.addFreshEntity(cart),"Real minecart variant inserted: "+variant);
            h.assertTrue(AnatomyApi.ready(cart) && Platforms.eligible(cart,support),
                "Prepared policy admits off-rail minecart: "+variant);
            cart.move(MoverType.SELF,new Vec3(0,-1.5,0));
            var contact=AnatomyMovement.contact(cart);
            h.assertTrue(contact!=null && contact.support()==support
                    && contact.revision()==frame.snapshot().revision()
                    && !cart.isOnRails() && Platforms.supported(cart)
                    && TestRetiredPlatformRuntime.absent(),
                "Real off-rail cart acquires canonical-only material contact: "+variant+" contact="+contact);
            cart.tick();
            h.assertTrue(!cart.isOnRails() && Platforms.supported(cart)
                    && AnatomyMovement.contact(cart)!=null,
                "Native off-rail cart tick retains canonical material contact: "+variant);
            if(cart instanceof AbstractMinecartContainer chest)
                h.assertTrue(chest.getItem(0).is(Items.STONE) && chest.getItem(0).getCount()==3,
                    "Vanilla minecart container inventory survives canonical material tick");
            if(variant==EntityTypes.MINECART) {
                var rider=h.spawn(EntityTypes.PIG,2,27,2);
                rider.setNoAi(true);
                h.assertTrue(rider.startRiding(cart,true,true),"Vanilla cart accepts a native passenger");
                h.assertTrue(!Platforms.supported(rider) && AnatomyMovement.contact(rider)==null,
                    "Cart passenger is not a second material root");
                var before=cart.position();
                var retained=AnatomyMovement.surface(cart);
                var materialBefore=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(retained.piece());
                var metricsBefore=MaterialPhysicsRuntime.metrics(level);
                support.move(MoverType.SELF,new Vec3(.2,.1,0));
                var materialAfter=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(retained.piece());
                var expected=materialAfter.point(retained.localPoint()).subtract(materialBefore.point(retained.localPoint()));
                var metricsAfter=MaterialPhysicsRuntime.metrics(level);
                long admittedBefore=metricsBefore.tick()==level.getGameTime()?metricsBefore.admitted():0;
                h.assertTrue(cart.position().distanceToSqr(before.add(expected))<1e-8
                        && cart.getFirstPassenger()==rider && rider.getVehicle()==cart
                        && metricsAfter.admitted()==admittedBefore+1,
                    "One ROOT interval carries the occupied cart root once: before="+before
                        +" after="+cart.position()+" expected="+expected
                        +" rider="+(cart.getFirstPassenger()==rider && rider.getVehicle()==cart)
                        +" metricsBefore="+metricsBefore+" metricsAfter="+metricsAfter
                        +" contact="+AnatomyMovement.contact(cart));
                var once=cart.position();MaterialPhysicsRuntime.drain(level);
                h.assertTrue(cart.position().equals(once),"Second material drain cannot replay cart carry");
                var yawSurface=AnatomyMovement.surface(cart);
                var yawBefore=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(yawSurface.piece());
                var cartBeforeYaw=cart.position();
                var capture=MaterialIntervalRuntime.captureRoot(support);
                support.yBodyRot=capture.root().yaw()+10;
                MaterialIntervalRuntime.commitRoot(support,capture);
                var yawAfter=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(yawSurface.piece());
                var yawDelta=yawAfter.point(yawSurface.localPoint()).subtract(yawBefore.point(yawSurface.localPoint()));
                MaterialPhysicsRuntime.drain(level);
                h.assertTrue(cart.position().distanceToSqr(cartBeforeYaw.add(yawDelta))<1e-8
                        && cart.getFirstPassenger()==rider && !Platforms.supported(rider),
                    "Retained local point carries one occupied cart through ROOT yaw");
                rider.discard();
            }
        } finally {
            if(cart instanceof AbstractMinecartContainer chest)chest.setItem(0,ItemStack.EMPTY);
            if(cart!=null)cart.discard();
            support.discard();
        }
    }

    private static void runRailHandoff(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        AbstractMinecart cart=null;Cow second=null;var placed=new ArrayList<BlockPos>();
        try {
            Platforms.tick(level);
            var piece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(piece!=null,"Rail handoff needs prepared body geometry");
            var bounds=piece.bounds();
            cart=EntityTypes.MINECART.create(level,EntitySpawnReason.COMMAND);
            h.assertTrue(cart!=null,"Rail handoff creates a real minecart");
            cart.setPos(bounds.maxX-.35,bounds.maxY+.8,bounds.getCenter().z);
            h.assertTrue(level.addFreshEntity(cart),"Rail handoff inserts cart");
            cart.move(MoverType.SELF,new Vec3(0,-1.5,0));
            h.assertTrue(Platforms.supported(cart) && !cart.isOnRails(),
                "Rail handoff begins with off-rail canonical material contact");
            int railX=(int)Math.floor(bounds.maxX),railY=(int)Math.floor(bounds.maxY);
            int railZ=(int)Math.floor(bounds.getCenter().z);
            for(int x=railX;x<railX+5;x++) {
                var floor=new BlockPos(x,railY-1,railZ);
                var rail=new BlockPos(x,railY,railZ);
                h.assertTrue(level.getBlockState(floor).isAir() && level.getBlockState(rail).isAir(),
                    "Native rail fixture starts in air");
                level.setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(rail,Blocks.RAIL.defaultBlockState());
                placed.add(rail);placed.add(floor);
            }
            System.out.println("S37 rail fixture bounds="+bounds+" railX="+railX+" railY="+railY
                +" cart="+cart.position()+" behavior="+cart.getBehavior().getClass().getSimpleName());
            var start=cart.position();
            cart.setDeltaMovement(new Vec3(.42,0,0));
            boolean entered=false;
            for(int tick=0;tick<32;tick++) {
                cart.tick();
                if(cart.isOnRails()){entered=true;break;}
            }
            h.assertTrue(entered && cart.getX()>start.x+.2,
                "Real native cart tick enters physical rail from prepared material: "+cart.position());
            h.assertTrue(AnatomyMovement.contact(cart)==null && !Platforms.supported(cart),
                "Real rail entry retires canonical material contact");
            var onRail=cart.position();
            support.move(MoverType.SELF,new Vec3(.2,0,0));
            h.assertTrue(cart.position().equals(onRail) && AnatomyMovement.contact(cart)==null,
                "On-rail cart ignores later material ROOT carry");
            for(int tick=0;tick<5;tick++)cart.tick();
            h.assertTrue(cart.isOnRails() && cart.getX()>onRail.x+.05,
                "Native rail travel continues after anatomical contact retirement");
            boolean leftRail=false;
            for(int tick=0;tick<80;tick++) {
                cart.tick();
                if(!cart.isOnRails()){leftRail=true;break;}
            }
            h.assertTrue(leftRail,"Real cart must leave the last physical rail block: "
                +"start="+onRail+" current="+cart.position()+" velocity="+cart.getDeltaMovement()
                +" railX="+railX+" onRails="+cart.isOnRails());
            second=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
            h.assertTrue(second!=null,"Rail exit creates second prepared support");
            second.setNoAi(true);second.setNoGravity(true);
            second.setPos(railX+6,support.getY()-.44,bounds.getCenter().z);
            second.getAttribute(Attributes.SCALE).setBaseValue(4);second.refreshDimensions();
            h.assertTrue(level.addFreshEntity(second),"Rail exit inserts second prepared support");
            Platforms.tick(level);
            h.assertTrue(AnatomyMovement.queryFrame(second).isPresent(),
                "Rail exit support has current prepared geometry");
            boolean reacquired=false;
            for(int tick=0;tick<20;tick++) {
                var fresh=AnatomyMovement.contact(cart);
                if(fresh!=null && fresh.support()==second && Platforms.supported(cart)) {
                    reacquired=true;break;
                }
                cart.tick();
            }
            h.assertTrue(reacquired,
                "Off-rail cart reacquires a fresh prepared material contact after rail exit: "
                    +cart.position()+" old="+onRail+" contact="+AnatomyMovement.contact(cart));
            var freshPosition=cart.position();
            second.move(MoverType.SELF,new Vec3(.2,0,0));
            h.assertTrue(cart.getX()>freshPosition.x+.1 && !cart.isOnRails(),
                "Second prepared support carries freshly reacquired off-rail cart");
        } finally {
            for(var pos:placed)level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            if(cart!=null)cart.discard();
            if(second!=null)second.discard();
            support.discard();
        }
    }

    private static void runOversizeControl(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(.5);support.refreshDimensions();
        AbstractMinecart cart=null;
        try {
            Platforms.tick(level);
            var piece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(piece!=null,"Rejected cart fixture needs prepared cow geometry");
            var bounds=piece.bounds();
            cart=EntityTypes.MINECART.create(level,EntitySpawnReason.COMMAND);
            h.assertTrue(cart!=null,"Rejected cart fixture creates real minecart");
            cart.setPos(bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z);
            h.assertTrue(level.addFreshEntity(cart),"Rejected cart fixture inserts real minecart");
            h.assertTrue(AnatomyApi.ready(cart) && !Platforms.eligible(cart,support),
                "Narrow prepared support rejects minecart by effective width ratio");
            cart.move(MoverType.SELF,new Vec3(0,-1.5,0));
            h.assertTrue(AnatomyMovement.contact(cart)==null && TestRetiredPlatformRuntime.absent(),
                "Ratio-rejected minecart has no canonical or legacy material owner");
            cart.setPos(support.getX()+.4,support.getY(),support.getZ());
            cart.setDeltaMovement(Vec3.ZERO);
            h.assertTrue(cart.canCollideWith(support),
                "Ratio-rejected minecart/cow pair retains native collision predicate");
            cart.push(support);
            h.assertTrue(cart.getDeltaMovement().horizontalDistanceSqr()>1e-8
                    || support.getDeltaMovement().horizontalDistanceSqr()>1e-8,
                "One native push still acts on ratio-rejected minecart/cow pair");
        } finally {
            if(cart!=null)cart.discard();
            support.discard();
        }
    }

    private static void runBlockedCarry(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        AbstractMinecart cart=null;var wall=new ArrayList<BlockPos>();
        try {
            Platforms.tick(level);
            var piece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(piece!=null,"Blocked cart fixture needs prepared body");
            var bounds=piece.bounds();
            cart=EntityTypes.MINECART.create(level,EntitySpawnReason.COMMAND);
            h.assertTrue(cart!=null,"Blocked cart fixture creates real minecart");
            cart.setPos(bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z);
            h.assertTrue(level.addFreshEntity(cart),"Blocked cart fixture inserts minecart");
            cart.move(MoverType.SELF,new Vec3(0,-1.5,0));
            h.assertTrue(Platforms.supported(cart),"Blocked cart fixture acquires canonical contact");
            int x=(int)Math.ceil(cart.getBoundingBox().maxX+.05);
            int y=(int)Math.floor(cart.getBoundingBox().minY);
            for(int z=(int)Math.floor(cart.getBoundingBox().minZ);z<=(int)Math.floor(cart.getBoundingBox().maxZ);z++) {
                var pos=new BlockPos(x,y,z);wall.add(pos);
                h.assertTrue(level.getBlockState(pos).isAir(),"Blocked cart wall starts in air");
                level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
            }
            var before=cart.position();
            support.move(MoverType.SELF,new Vec3(1.2,0,0));
            h.assertTrue(cart.getBoundingBox().maxX<=x+1e-6 && cart.getX()<before.x+1.2-1e-4,
                "Material ROOT cannot carry minecart through a real block wall: "
                    +"before="+before+" after="+cart.position()+" box="+cart.getBoundingBox()
                    +" wallX="+x+" wallY="+y+" contact="+AnatomyMovement.contact(cart));
            var stopped=cart.position();
            for(var pos:wall)level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            wall.clear();MaterialPhysicsRuntime.drain(level);
            h.assertTrue(cart.position().equals(stopped),
                "Removing wall cannot replay rejected minecart carry");
            support.discard();
            h.assertTrue(AnatomyMovement.contact(cart)==null && !Platforms.supported(cart),
                "Support retirement releases obstructed minecart without legacy fallback");
        } finally {
            for(var pos:wall)level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            if(cart!=null)cart.discard();
            support.discard();
        }
    }
}
