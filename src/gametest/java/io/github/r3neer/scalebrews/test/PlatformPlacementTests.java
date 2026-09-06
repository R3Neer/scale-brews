package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.platform.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class PlatformPlacementTests {
    @GameTest public void preservesVanillaBabyEggInteraction(GameTestHelper h) {
        var cow=h.spawnWithNoFreeWill(EntityTypes.COW,1,3,1);
        var player=h.makeMockPlayer(GameType.SURVIVAL); player.setPos(cow.position().add(-2,0,0));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COW_SPAWN_EGG,2));
        h.assertTrue(PlatformPlacement.interact(player,cow,InteractionHand.MAIN_HAND,new Vec3(0,.8,0))
                ==InteractionResult.PASS,"Same-species egg retains vanilla interaction when adult cannot fit");
        h.assertTrue(player.getMainHandItem().getCount()==2,"Fallback does not consume egg");
        cow.discard(); h.succeed();
    }
    @GameTest public void giantPlayerAndCreativePlacement(GameTestHelper h) {
        var support=h.makeMockPlayer(GameType.SURVIVAL);
        support.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(5);
        support.refreshDimensions(); support.setPos(h.absoluteVec(new Vec3(1,20,1)));
        h.getLevel().addFreshEntity(support);
        var player=h.makeMockPlayer(GameType.CREATIVE); player.setPos(support.position().add(-4,8,0));
        GameType.CREATIVE.updatePlayerAbilities(player.getAbilities());
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.OAK_CHEST_BOAT));
        h.assertTrue(player.interactOn(support,InteractionHand.MAIN_HAND,new Vec3(0,9,0))==InteractionResult.SUCCESS,"Boat on giant player head");
        h.assertTrue(player.getMainHandItem().getCount()==1,"Creative inventory preserved");
        var bodies=h.getLevel().getEntities(support,support.getBoundingBox().inflate(3),e->Platforms.state(e).support==support);
        h.assertTrue(bodies.size()==1,"One chest boat on giant head");
        bodies.forEach(net.minecraft.world.entity.Entity::discard);
        player.getAbilities().mayBuild=false;
        h.assertTrue(player.interactOn(support,InteractionHand.MAIN_HAND,new Vec3(0,9,0))==InteractionResult.FAIL,"Build restrictions retained");
        support.discard(); h.succeed();
    }
    @GameTest public void placesAndTransportsBodies(GameTestHelper h) {
        var support=h.spawnWithNoFreeWill(EntityTypes.GHAST,1,20,1); support.setNoGravity(true);
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(support.position().add(-4,3,0));
        for(var item:new Item[]{Items.OAK_BOAT,Items.BAMBOO_CHEST_RAFT,Items.PIG_SPAWN_EGG}) {
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item,2));
            var result=player.interactOn(support,InteractionHand.MAIN_HAND,new Vec3(0,4,0));
            h.assertTrue(result==InteractionResult.SUCCESS,"Place "+item);
            h.assertTrue(player.getMainHandItem().getCount()==1,"Consume exactly once");
            var bodies=h.getLevel().getEntities(support,support.getBoundingBox().inflate(5),e->Platforms.state(e).support==support);
            h.assertTrue(bodies.size()==1,"Exactly one supported entity");
            var body=bodies.getFirst(); var before=body.position();
            support.setPos(support.position().add(1,0,0)); PlatformPhysics.carry(body);
            h.assertTrue(body.position().distanceTo(before.add(1,0,0))<.001,"Placed entity is transported immediately");
            body.discard(); player.setPos(support.position().add(-4,3,0));
        }
        support.discard(); h.succeed();
    }

    @GameTest public void rejectsBlockedAndOversizedBodies(GameTestHelper h) {
        var support=h.spawnWithNoFreeWill(EntityTypes.GHAST,1,20,1); support.setNoGravity(true);
        var player=h.makeMockPlayer(GameType.SURVIVAL); player.setPos(support.position().add(-4,3,0));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GHAST_SPAWN_EGG,2));
        h.assertTrue(player.interactOn(support,InteractionHand.MAIN_HAND,new Vec3(0,4,0))==InteractionResult.FAIL,"Reject width ratio");
        h.assertTrue(player.getMainHandItem().getCount()==2,"Failed placement costs nothing");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.OAK_BOAT,2));
        var obstruction=support.blockPosition().above(4);
        h.getLevel().setBlockAndUpdate(obstruction,Blocks.STONE.defaultBlockState());
        h.assertTrue(player.interactOn(support,InteractionHand.MAIN_HAND,new Vec3(0,4,0))==InteractionResult.FAIL,"Reject obstructed surface");
        h.assertTrue(player.getMainHandItem().getCount()==2,"Obstruction costs nothing");
        h.getLevel().setBlockAndUpdate(obstruction,Blocks.AIR.defaultBlockState());
        support.discard(); h.succeed();
    }
}
