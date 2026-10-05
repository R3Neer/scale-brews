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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Real vanilla falling-block lifecycle on a prepared canonical material support. */
final class S38PreparedFallingBlockProof {
    private S38PreparedFallingBlockProof() {}

    static void run(GameTestHelper h) {
        for(var state:List.of(Blocks.SAND.defaultBlockState(),Blocks.CONCRETE_POWDER.white().defaultBlockState(),
                Blocks.ANVIL.defaultBlockState()))runSupportedTick(h,state);
        runSandPlacement(h);
        runSandTimeout(h);
        runConcreteWater(h,false);
        runConcreteWater(h,true);
        runAnvilImpact(h);
        runRootCarry(h);
        runBlockedCarry(h);
        runRatioRejection(h);
    }

    private static void runSupportedTick(GameTestHelper h,BlockState state) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        FallingBlockEntity block=null;
        try {
            Platforms.tick(level);
            var frame=AnatomyMovement.queryFrame(support).orElseThrow();
            var piece=frame.snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(piece!=null,"Prepared cow body required for falling block");
            var bounds=piece.bounds();
            var source=BlockPos.containing(bounds.getCenter().x,bounds.maxY+5,bounds.getCenter().z);
            h.assertTrue(level.getBlockState(source).isAir(),"Falling-block source starts in air");
            block=FallingBlockEntity.fall(level,source,state);
            h.assertTrue(block!=null && block.getBlockState().equals(state),
                "Real falling entity retains native source block state");
            block.setPos(bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z);
            h.assertTrue(AnatomyApi.ready(block) && Platforms.eligible(block,support),
                "Prepared policy admits falling block");
            block.move(MoverType.SELF,new Vec3(0,-1.5,0));
            var contact=AnatomyMovement.contact(block);
            h.assertTrue(contact!=null && contact.support()==support
                    && contact.revision()==frame.snapshot().revision()
                    && Platforms.supported(block) && TestRetiredPlatformRuntime.absent(),
                "Falling block acquires canonical-only material contact: "+state);
            for(int tick=0;tick<8;tick++)block.tick();
            h.assertTrue(!block.isRemoved() && block.getBlockState().equals(state)
                    && Platforms.supported(block) && block.time<=2,
                "Native supported ticks keep block entity and pause finite timeout: "
                    +state+" time="+block.time+" removed="+block.isRemoved());
        } finally {
            if(block!=null)block.discard();
            support.discard();
        }
    }

    private static void runSandPlacement(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        FallingBlockEntity sand=null;var floor=new ArrayList<BlockPos>();
        BlockPos placed=null;
        try {
            Platforms.tick(level);
            var piece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(piece!=null,"Sand lifecycle needs prepared body");
            var bounds=piece.bounds();
            int x=(int)Math.floor(bounds.getCenter().x),z=(int)Math.floor(bounds.getCenter().z);
            int floorY=(int)Math.floor(bounds.maxY)-4;
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
                var pos=new BlockPos(x+dx,floorY,z+dz);
                h.assertTrue(level.getBlockState(pos).isAir(),"Sand floor starts in air");
                level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
                floor.add(pos);
            }
            placed=new BlockPos(x,floorY+1,z);
            h.assertTrue(level.getBlockState(placed).isAir(),"Sand destination starts empty");
            sand=FallingBlockEntity.fall(level,new BlockPos(x,floorY+10,z),Blocks.SAND.defaultBlockState());
            sand.setPos(bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z);
            sand.move(MoverType.SELF,new Vec3(0,-1.5,0));
            h.assertTrue(Platforms.supported(sand),"Sand lifecycle begins on canonical material");
            for(int tick=0;tick<650;tick++)sand.tick();
            h.assertTrue(!sand.isRemoved() && Platforms.supported(sand) && sand.time<=2
                    && level.getBlockState(placed).isAir(),
                "Sand remains entity after 650 native ticks on material without placement/timeout");
            support.discard();
            h.assertTrue(AnatomyMovement.contact(sand)==null,"Support release retires held sand contact");
            for(int tick=0;tick<120 && !sand.isRemoved();tick++)sand.tick();
            h.assertTrue(sand.isRemoved() && level.getBlockState(placed).is(Blocks.SAND),
                "Released sand executes one native placement on stone floor: "
                    +"removed="+sand.isRemoved()+" block="+level.getBlockState(placed));
        } finally {
            if(sand!=null)sand.discard();
            if(placed!=null)level.setBlockAndUpdate(placed,Blocks.AIR.defaultBlockState());
            for(var pos:floor)level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            support.discard();
        }
    }

    private static void runSandTimeout(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        FallingBlockEntity sand=null;
        try {
            Platforms.tick(level);
            var piece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(piece!=null,"Sand timeout needs prepared body");
            var bounds=piece.bounds();
            sand=FallingBlockEntity.fall(level,BlockPos.containing(bounds.getCenter().x,bounds.maxY+5,
                bounds.getCenter().z),Blocks.SAND.defaultBlockState());
            sand.setPos(bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z);
            sand.move(MoverType.SELF,new Vec3(0,-1.5,0));
            h.assertTrue(Platforms.supported(sand),"Sand timeout begins with canonical material contact");
            sand.time=600;
            sand.tick();
            h.assertTrue(!sand.isRemoved() && sand.time==600 && Platforms.supported(sand),
                "Held sand cannot take vanilla 600-tick timeout while supported");
            var dropArea=sand.getBoundingBox().inflate(3);
            h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,dropArea,
                item->item.getItem().is(Items.SAND)).isEmpty(),
                "Timeout fixture begins without a sand drop");
            support.discard();
            sand.tick();
            var drops=level.getEntitiesOfClass(ItemEntity.class,dropArea,
                item->item.getItem().is(Items.SAND));
            h.assertTrue(sand.isRemoved() && sand.time==601 && drops.size()==1
                    && drops.getFirst().getItem().getCount()==1,
                "Released sand resumes the native 600-tick timeout and drops exactly once: "
                    +"time="+sand.time+" removed="+sand.isRemoved()+" drops="+drops.size());
            drops.getFirst().discard();
        } finally {
            if(sand!=null)sand.discard();
            support.discard();
        }
    }

    private static void runConcreteWater(GameTestHelper h,boolean flowing) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        FallingBlockEntity powder=null;BlockPos water=null,floor=null;
        try {
            Platforms.tick(level);
            var piece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(piece!=null,"Concrete water fixture needs prepared body");
            var bounds=piece.bounds();
            powder=FallingBlockEntity.fall(level,BlockPos.containing(bounds.getCenter().x,bounds.maxY+5,
                bounds.getCenter().z),Blocks.CONCRETE_POWDER.white().defaultBlockState());
            powder.setPos(bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z);
            powder.move(MoverType.SELF,new Vec3(0,-1.5,0));
            h.assertTrue(Platforms.supported(powder),"Concrete powder starts on canonical material");
            water=BlockPos.containing(powder.position());floor=water.below();
            h.assertTrue(level.getBlockState(water).isAir() && level.getBlockState(floor).isAir(),
                "Concrete water fixture begins in air");
            level.setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(water,Blocks.WATER.defaultBlockState()
                .setValue(LiquidBlock.LEVEL,flowing?4:0));
            h.assertTrue(!level.getFluidState(water).isEmpty()
                    && level.getFluidState(water).isSource()!=flowing,
                "Concrete has real "+(flowing?"flowing":"source")+" water");
            powder.tick();
            h.assertTrue(!powder.isRemoved() && Platforms.supported(powder)
                    && level.getBlockState(water).is(Blocks.WATER),
                "Supported concrete powder stays an entity over water until release: "
                    +"removed="+powder.isRemoved()+" block="+level.getBlockState(water)
                    +" contact="+AnatomyMovement.contact(powder));
            support.discard();
            for(int tick=0;tick<20 && !powder.isRemoved();tick++)powder.tick();
            h.assertTrue(powder.isRemoved() && level.getBlockState(water).is(Blocks.CONCRETE.white()),
                "Released powder resumes native "+(flowing?"flowing":"source")
                    +" water hardening and places white concrete once: "
                    +"removed="+powder.isRemoved()+" block="+level.getBlockState(water));
        } finally {
            if(powder!=null)powder.discard();
            if(water!=null)level.setBlockAndUpdate(water,Blocks.AIR.defaultBlockState());
            if(floor!=null)level.setBlockAndUpdate(floor,Blocks.AIR.defaultBlockState());
            support.discard();
        }
    }

    private static void runAnvilImpact(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        support.setHealth(10);
        FallingBlockEntity anvil=null;Pig target=null;BlockPos floor=null,landed=null;
        try {
            Platforms.tick(level);
            var piece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(piece!=null,"Anvil impact needs prepared cow body");
            var bounds=piece.bounds();
            anvil=FallingBlockEntity.fall(level,BlockPos.containing(bounds.getCenter().x,bounds.maxY+7,
                bounds.getCenter().z),Blocks.ANVIL.defaultBlockState());
            anvil.setHurtsEntities(1,3);
            anvil.setPos(bounds.getCenter().x,bounds.maxY+4,bounds.getCenter().z);
            anvil.fallDistance=4;
            anvil.move(MoverType.SELF,new Vec3(0,-5,0));
            h.assertTrue(Platforms.supported(anvil) && !anvil.isRemoved()
                    && support.getHealth()<10,
                "Real canonical anvil landing damages its material support once: "
                    +"health="+support.getHealth()+" contact="+AnatomyMovement.contact(anvil));
            float impactHealth=support.getHealth();
            var before=anvil.position();
            support.invulnerableTime=0;
            support.move(MoverType.SELF,new Vec3(.2,0,0));
            h.assertTrue(anvil.getX()>before.x+.19 && support.getHealth()==impactHealth,
                "Passive material ROOT carries anvil without a second impact");
            support.invulnerableTime=0;
            anvil.tick();
            h.assertTrue(!anvil.isRemoved() && Platforms.supported(anvil)
                    && support.getHealth()==impactHealth,
                "Native supported anvil tick does not repeat impact after invulnerability reset");
            int floorY=(int)Math.floor(bounds.maxY)-4;
            floor=BlockPos.containing(bounds.getCenter().x,floorY,bounds.getCenter().z);
            landed=floor.above();
            h.assertTrue(level.getBlockState(floor).isAir() && level.getBlockState(landed).isAir(),
                "Second anvil impact floor begins empty");
            level.setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
            target=EntityTypes.PIG.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            h.assertTrue(target!=null,"Second anvil impact creates real living target");
            target.setNoAi(true);target.setNoGravity(true);
            target.setPos(bounds.getCenter().x,floorY+1,bounds.getCenter().z);
            target.setHealth(10);
            h.assertTrue(level.addFreshEntity(target),"Second anvil impact inserts target");
            support.discard();
            h.assertTrue(AnatomyMovement.contact(anvil)==null,
                "Anvil release retires old material contact before native free fall");
            for(int tick=0;tick<120 && !anvil.isRemoved();tick++)anvil.tick();
            h.assertTrue(anvil.isRemoved() && target.getHealth()<10,
                "Released anvil has one later genuine native landing impact: "
                    +"targetHealth="+target.getHealth()+" removed="+anvil.isRemoved()
                    +" block="+level.getBlockState(landed));
        } finally {
            if(anvil!=null)anvil.discard();
            if(target!=null)target.discard();
            if(landed!=null)level.setBlockAndUpdate(landed,Blocks.AIR.defaultBlockState());
            if(floor!=null)level.setBlockAndUpdate(floor,Blocks.AIR.defaultBlockState());
            support.discard();
        }
    }

    private static void runRootCarry(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        FallingBlockEntity sand=null;
        try {
            Platforms.tick(level);
            var piece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(piece!=null,"Sand ROOT fixture needs prepared body");
            var bounds=piece.bounds();
            sand=FallingBlockEntity.fall(level,BlockPos.containing(bounds.getCenter().x,bounds.maxY+5,
                bounds.getCenter().z),Blocks.SAND.defaultBlockState());
            sand.setPos(bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z);
            sand.move(MoverType.SELF,new Vec3(0,-1.5,0));
            h.assertTrue(Platforms.supported(sand) && TestRetiredPlatformRuntime.absent(),
                "Real sand ROOT fixture acquires canonical-only contact");
            var surface=AnatomyMovement.surface(sand);
            var beforePiece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(surface.piece());
            var before=sand.position();
            support.move(MoverType.SELF,new Vec3(.2,.1,0));
            var afterPiece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(surface.piece());
            var expected=afterPiece.point(surface.localPoint()).subtract(beforePiece.point(surface.localPoint()));
            h.assertTrue(sand.position().distanceToSqr(before.add(expected))<1e-8
                    && Platforms.supported(sand),
                "One material ROOT translation carries held sand at its retained point");
            var once=sand.position();MaterialPhysicsRuntime.drain(level);
            h.assertTrue(sand.position().equals(once),"Repeated drain cannot duplicate sand ROOT carry");
            surface=AnatomyMovement.surface(sand);
            beforePiece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(surface.piece());
            before=sand.position();
            var metricsBeforeYaw=MaterialPhysicsRuntime.metrics(level);
            var capture=MaterialIntervalRuntime.captureRoot(support);
            support.yBodyRot=capture.root().yaw()+10;
            MaterialIntervalRuntime.commitRoot(support,capture);
            afterPiece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(surface.piece());
            expected=afterPiece.point(surface.localPoint()).subtract(beforePiece.point(surface.localPoint()));
            MaterialPhysicsRuntime.drain(level);
            h.assertTrue(sand.position().distanceToSqr(before.add(expected))<1e-8
                    && Platforms.supported(sand),
                "Retained material point carries held sand through one ROOT yaw: before="+before
                    +" after="+sand.position()+" expected="+expected
                    +" contact="+AnatomyMovement.contact(sand)
                    +" metricsBefore="+metricsBeforeYaw
                    +" metricsAfter="+MaterialPhysicsRuntime.metrics(level));
        } finally {
            if(sand!=null)sand.discard();
            support.discard();
        }
    }

    private static void runBlockedCarry(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        FallingBlockEntity sand=null;var wall=new ArrayList<BlockPos>();
        try {
            Platforms.tick(level);
            var piece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(piece!=null,"Blocked sand fixture needs prepared body");
            var bounds=piece.bounds();
            sand=FallingBlockEntity.fall(level,BlockPos.containing(bounds.getCenter().x,bounds.maxY+5,
                bounds.getCenter().z),Blocks.SAND.defaultBlockState());
            sand.setPos(bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z);
            sand.move(MoverType.SELF,new Vec3(0,-1.5,0));
            h.assertTrue(Platforms.supported(sand),"Blocked sand fixture acquires canonical contact");
            int x=(int)Math.ceil(sand.getBoundingBox().maxX+.05);
            int y=(int)Math.floor(sand.getBoundingBox().minY);
            for(int z=(int)Math.floor(sand.getBoundingBox().minZ);
                    z<=(int)Math.floor(sand.getBoundingBox().maxZ);z++) {
                var pos=new BlockPos(x,y,z);
                h.assertTrue(level.getBlockState(pos).isAir(),"Blocked sand wall starts in air");
                level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
                wall.add(pos);
            }
            var before=sand.position();
            double gap=x-sand.getBoundingBox().maxX;
            double supportBeforeX=support.getX();
            support.move(MoverType.SELF,new Vec3(1.2,0,0));
            h.assertTrue(support.getX()-supportBeforeX>gap+.05,
                "Sand obstruction fixture crosses the actual body-to-wall gap");
            h.assertTrue(sand.getBoundingBox().maxX<=x+1e-6 && sand.getX()<before.x+1.2-1e-4,
                "Material ROOT cannot carry held sand through a real block wall");
            var stopped=sand.position();
            for(var pos:wall)level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            wall.clear();MaterialPhysicsRuntime.drain(level);
            h.assertTrue(sand.position().equals(stopped),
                "Removing obstruction cannot replay rejected sand displacement");
            support.discard();
            h.assertTrue(AnatomyMovement.contact(sand)==null && !Platforms.supported(sand),
                "Support retirement releases obstructed sand without legacy fallback");
        } finally {
            for(var pos:wall)level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            if(sand!=null)sand.discard();
            support.discard();
        }
    }

    private static void runRatioRejection(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(.5);support.refreshDimensions();
        FallingBlockEntity sand=null;
        try {
            Platforms.tick(level);
            var piece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(piece!=null,"Rejected sand fixture needs prepared body");
            var bounds=piece.bounds();
            sand=FallingBlockEntity.fall(level,BlockPos.containing(bounds.getCenter().x,bounds.maxY+5,
                bounds.getCenter().z),Blocks.SAND.defaultBlockState());
            sand.setPos(bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z);
            h.assertTrue(AnatomyApi.ready(sand) && !Platforms.eligible(sand,support),
                "Narrow prepared support rejects real sand by effective width ratio");
            sand.move(MoverType.SELF,new Vec3(0,-1.5,0));
            h.assertTrue(AnatomyMovement.contact(sand)==null && TestRetiredPlatformRuntime.absent(),
                "Ratio-rejected sand retains its native path without a material owner");
            var before=sand.position();
            support.move(MoverType.SELF,new Vec3(.2,0,0));
            h.assertTrue(sand.position().equals(before),
                "Rejected support ROOT cannot carry a nonmaterial falling block");
            int age=sand.time;
            sand.tick();
            h.assertTrue(sand.time==age+1 && !sand.isRemoved()
                    && AnatomyMovement.contact(sand)==null,
                "Ratio-rejected sand continues its own native age and free-fall path");
        } finally {
            if(sand!=null)sand.discard();
            support.discard();
        }
    }
}
