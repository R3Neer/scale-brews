package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.physics.GrowthImpact;
import io.github.r3neer.scalebrews.physics.ImpactSurfaces;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class ImpactSurfaceTests {
    static void flatFloor(GameTestHelper h,int y) {
        for(int x=0;x<=4;x++)for(int z=0;z<=3;z++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);
    }
    @GameTest public void waveClimbsThreeStepsAndDescends(GameTestHelper h) {
        for(int x=2;x<=5;x++)h.setBlock(new BlockPos(x,20+x-2,2),Blocks.STONE);
        var source=h.makeMockPlayer(GameType.SURVIVAL);
        source.getAttribute(Attributes.SCALE).setBaseValue(3.88);
        source.setPos(h.absoluteVec(new Vec3(2.5,21,2.5)));
        var target=h.spawnWithNoFreeWill(EntityTypes.PIG,5.5F,24,2.5F);
        GrowthImpact.land(source,20,Blocks.STONE.defaultBlockState());
        h.assertTrue(target.getDeltaMovement().x>0 && target.getHealth()<target.getMaxHealth(),"Three successive one-block rises transmit knockback and damage");
        var downhill=new ImpactSurfaces(h.getLevel(),target.position(),5);
        h.assertTrue(downhill.distanceTo(source)<5,"Same staircase transmits downhill");
        target.setDeltaMovement(Vec3.ZERO);target.setHealth(target.getMaxHealth());target.invulnerableTime=0;
        source.getAttribute(Attributes.SCALE).setBaseValue(2.92);
        GrowthImpact.land(source,23,Blocks.STONE.defaultBlockState());
        h.assertTrue(target.getDeltaMovement().x>0 && target.getHealth()==target.getMaxHealth(),"Growth II pushes up stairs without damage");
        h.succeed();
    }

    @GameTest public void cliffGapAndAirborneBodiesBlockWave(GameTestHelper h) {
        h.setBlock(new BlockPos(2,20,2),Blocks.STONE);
        h.setBlock(new BlockPos(3,22,2),Blocks.STONE);
        h.setBlock(new BlockPos(4,20,2),Blocks.STONE);
        var origin=h.absoluteVec(new Vec3(2.5,21,2.5));
        var target=h.spawnWithNoFreeWill(EntityTypes.PIG,3.5F,23,2.5F);
        h.assertTrue(Double.isInfinite(new ImpactSurfaces(h.getLevel(),origin,5).distanceTo(target)),"Two-block cliff blocks wave");
        h.setBlock(new BlockPos(3,22,2),Blocks.AIR);
        target.setPos(h.absoluteVec(new Vec3(4.5,21,2.5)));
        h.assertTrue(Double.isInfinite(new ImpactSurfaces(h.getLevel(),origin,5).distanceTo(target)),"Gap blocks wave");
        h.setBlock(new BlockPos(3,20,2),Blocks.STONE);
        target.setPos(target.position().add(0,1,0));
        h.assertTrue(Double.isInfinite(new ImpactSurfaces(h.getLevel(),origin,5).distanceTo(target)),"Airborne target has no reached supporting surface");
        h.succeed();
    }

    @GameTest public void detourConsumesRangeAndSlabCountsItsRealHeight(GameTestHelper h) {
        for(int[] cell:new int[][]{{2,2},{2,3},{2,4},{3,4},{4,4},{4,3},{4,2}})
            h.setBlock(new BlockPos(cell[0],20,cell[1]),Blocks.STONE);
        var origin=h.absoluteVec(new Vec3(2.5,21,2.5));
        var target=h.spawnWithNoFreeWill(EntityTypes.PIG,4.5F,21,2.5F);
        h.assertTrue(Double.isInfinite(new ImpactSurfaces(h.getLevel(),origin,4).distanceTo(target)),"Nearby target outside path budget is excluded");
        h.assertTrue(new ImpactSurfaces(h.getLevel(),origin,7).distanceTo(target)==6,"Wave can turn corners within its path budget");
        h.setBlock(new BlockPos(3,21,2),Blocks.STONE_SLAB);
        target.setPos(h.absoluteVec(new Vec3(3.5,21.5,2.5)));
        double distance=new ImpactSurfaces(h.getLevel(),origin,3).distanceTo(target);
        h.assertTrue(Math.abs(distance-Math.sqrt(1.25))<.001,"Slab surface uses half-block height");
        h.succeed();
    }

    @GameTest public void closedDoorBlocksSurfaceConnection(GameTestHelper h) {
        h.setBlock(new BlockPos(2,20,2),Blocks.STONE);
        h.setBlock(new BlockPos(2,20,3),Blocks.STONE);
        var door=Blocks.OAK_DOOR.defaultBlockState()
            .setValue(net.minecraft.world.level.block.DoorBlock.FACING,net.minecraft.core.Direction.SOUTH);
        var lower=h.absolutePos(new BlockPos(2,21,3));
        h.getLevel().setBlock(lower,door,2);
        h.getLevel().setBlock(lower.above(),door.setValue(net.minecraft.world.level.block.DoorBlock.HALF,
            net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),2);
        var origin=h.absoluteVec(new Vec3(2.5,21,2.5));
        var target=h.spawnWithNoFreeWill(EntityTypes.PIG,2.5F,21,3.5F);
        h.assertTrue(Double.isInfinite(new ImpactSurfaces(h.getLevel(),origin,4).distanceTo(target)),"Thin closed wall cannot be crossed between clear floor centers");
        h.getLevel().setBlock(lower,Blocks.AIR.defaultBlockState(),2);
        h.getLevel().setBlock(lower.above(),Blocks.AIR.defaultBlockState(),2);
        h.assertTrue(new ImpactSurfaces(h.getLevel(),origin,4).distanceTo(target)==1,"Removing obstruction reconnects the surface");
        h.succeed();
    }
}
