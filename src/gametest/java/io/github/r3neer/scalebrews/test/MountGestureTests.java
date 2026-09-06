package io.github.r3neer.scalebrews.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public class MountGestureTests {
    @GameTest public void anyItemMountsAndSameWolfCanBeRemounted(GameTestHelper h) {
        var player=h.makeMockServerPlayerInLevel();
        player.getAttribute(Attributes.SCALE).setBaseValue(.52);
        var wolf=h.spawn(EntityTypes.WOLF,2,2,2);wolf.tame(player);wolf.setNoAi(true);
        for(boolean saddled:new boolean[]{false,true}) {
            wolf.setItemSlot(EquipmentSlot.SADDLE,saddled?new ItemStack(Items.SADDLE):ItemStack.EMPTY);
            for(var item:java.util.List.of(Items.STONE,Items.BEEF,Items.BONE,Items.SADDLE,Items.LEAD,Items.WOLF_ARMOR,Items.NAME_TAG,Items.WOLF_SPAWN_EGG)) {
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item,2));
                player.setShiftKeyDown(true);
                player.interactOn(wolf,InteractionHand.MAIN_HAND,Vec3.ZERO);
                h.assertTrue(player.getVehicle()==wolf,"Shift mounts with "+item+" saddled="+saddled);
                player.rideTick();
                h.assertTrue(player.getVehicle()==wolf,"Held mounting Shift must not eject a repeated rider");
                h.assertTrue(player.getMainHandItem().getCount()==2 && !wolf.isInLove(),"Mount gesture cannot use the item");
                h.assertTrue(wolf.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE)==saddled,"Mount gesture does not saddle");
                player.setShiftKeyDown(false);player.rideTick();
                player.setShiftKeyDown(true);player.rideTick();
                h.assertFalse(player.isPassenger(),"Fresh Shift press still dismounts");
            }
        }
        player.setShiftKeyDown(false);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BEEF));
        player.interactOn(wolf,InteractionHand.MAIN_HAND,Vec3.ZERO);
        h.assertTrue(wolf.isInLove(),"Ordinary feeding retains vanilla breeding");
        h.succeed();
    }

    @GameTest public void configuredCatUsesSameGesture(GameTestHelper h) {
        try(var scope=new TinyDefinitionTestScope("{\"entity\":\"minecraft:cat\",\"saddle\":false,\"control\":\"direct\",\"movement\":\"ground\",\"speed\":0.3}")) {
            var player=h.makeMockPlayer(GameType.SURVIVAL);
            player.getAttribute(Attributes.SCALE).setBaseValue(.28);
            var cat=h.spawn(EntityTypes.CAT,2,2,2);cat.tame(player);cat.setNoAi(true);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COD,2));
            for(int attempt=0;attempt<2;attempt++) {
                player.setShiftKeyDown(true);player.interactOn(cat,InteractionHand.MAIN_HAND,Vec3.ZERO);player.rideTick();
                h.assertTrue(player.getVehicle()==cat && !cat.isInLove(),"Configured tameable mounts instead of feeding");
                player.setShiftKeyDown(false);player.rideTick();
                player.setShiftKeyDown(true);player.rideTick();
                h.assertFalse(player.isPassenger(),"Configured tameable dismounts on new Shift");
            }
            h.assertTrue(player.getMainHandItem().getCount()==2,"Held food preserved");
        }
        h.succeed();
    }
}
