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
}
