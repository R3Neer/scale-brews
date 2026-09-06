package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.mount.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

public class WolfMountTests {
    @GameTest public void sharedWolfKeepsEquipmentAndOwner(GameTestHelper h) {
        var owner = h.makeMockPlayer(GameType.SURVIVAL);
        var rider = h.makeMockPlayer(GameType.SURVIVAL);
        var wolf = h.spawn(EntityTypes.WOLF, 2, 2, 2);
        wolf.tame(owner);
        wolf.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.WOLF_ARMOR));
        h.assertFalse(TinyMounts.eligible(rider, wolf), "Normal rider too large");
        rider.getAttribute(Attributes.SCALE).setBaseValue(.76);
        h.assertTrue(TinyMounts.eligible(rider, wolf), "External 0.76 scale permitted");
        rider.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        wolf.interact(rider, InteractionHand.MAIN_HAND, Vec3.ZERO);
        rider.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        rider.setShiftKeyDown(true);
        wolf.interact(rider, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(rider.getVehicle() == wolf && TinyMounts.controller(wolf) == rider, "Borrowed wolf directly controlled");
        h.assertFalse(wolf.isClientAuthoritative(), "Server owns mount movement");
        rider.stopRiding();
        h.assertTrue(wolf.isOwnedBy(owner) && wolf.getItemBySlot(EquipmentSlot.BODY).is(Items.WOLF_ARMOR)
            && wolf.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE), "Owner, armor and saddle preserved");
        rider.getAttribute(Attributes.SCALE).setBaseValue(1);
        wolf.getAttribute(Attributes.SCALE).setBaseValue(1.96);
        h.assertTrue(TinyMounts.eligible(rider, wolf), "Normal rider on grown wolf");
        wolf.setTame(false, false);
        h.assertTrue(TinyMounts.eligible(rider, wolf), "Wild wolf permits an uncontrolled taming attempt");
        h.succeed();
    }
    @GameTest public void damageBetrayalAndCommandScope(GameTestHelper h) {
        var owner = h.makeMockPlayer(GameType.SURVIVAL);
        var rider = h.makeMockPlayer(GameType.SURVIVAL);
        var wolf = h.spawn(EntityTypes.WOLF, 2, 2, 2);
        wolf.tame(owner);
        wolf.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        wolf.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.WOLF_ARMOR));
        rider.getAttribute(Attributes.SCALE).setBaseValue(.76);
        h.assertTrue(rider.startRiding(wolf), "Borrowed wolf mounts");
        owner.hurtServer(h.getLevel(), rider.damageSources().playerAttack(rider), 1);
        h.assertFalse(rider.isPassenger(), "Real rider damage immediately ejects");
        h.assertTrue(wolf.getTarget() == rider, "Vanilla AI targets betrayer");
        h.assertFalse(rider.startRiding(wolf), "Cannot remount active target");
        wolf.setTarget(null);
        owner.invulnerableTime = 0;
        h.assertTrue(rider.startRiding(wolf), "No permanent betrayal state");
        WolfMount.attack(wolf, rider, owner);
        h.assertFalse(rider.isPassenger(), "Commanded owner bite ejects");
        h.assertTrue(wolf.getTarget() == rider && wolf.isOwnedBy(owner), "True ownership persists");
        h.assertTrue(wolf.getItemBySlot(EquipmentSlot.BODY).is(Items.WOLF_ARMOR)
            && wolf.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE), "Betrayal preserves equipment");
        h.succeed();
    }

    @GameTest public void pounceEnvelope(GameTestHelper h) {
        var wolf = h.spawn(EntityTypes.WOLF, 2, 3, 2);
        wolf.setNoAi(true);
        var start = h.absoluteVec(new Vec3(2, 30, 2));
        for (float pitch : new float[]{0, -30, -55, 55}) {
            wolf.setPos(start);
            wolf.setOnGround(false);
            wolf.setDeltaMovement(WolfMount.launchVelocity(0, pitch, 1));
            double apex = 0;
            for (int i = 0; i < 30; i++) {
                wolf.travel(Vec3.ZERO);
                apex = Math.max(apex, wolf.getY() - start.y);
                if (wolf.getY() <= start.y) break;
            }
            double distance = wolf.position().subtract(start).horizontalDistance();
            System.out.println("WOLF_POUNCE pitch=" + pitch + " distance=" + distance + " apex=" + apex);
            h.assertTrue(distance <= 5.5 && apex <= 1.8, "Bounded pounce envelope");
            if (pitch == 0) h.assertTrue(distance >= 4.5, "Full forward pounce reaches approximately five blocks");
        }
        h.succeed();
    }

    @GameTest public void tapAndSinglePounceImpact(GameTestHelper h) {
        var rider=h.makeMockServerPlayerInLevel();rider.setGameMode(GameType.SURVIVAL);
        rider.getAttribute(Attributes.SCALE).setBaseValue(.76);
        var wolf=h.spawn(EntityTypes.WOLF,2,4,2);wolf.tame(rider);wolf.setNoAi(true);
        wolf.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));
        rider.startRiding(wolf);rider.setYRot(0);rider.setXRot(0);
        var cow=h.spawn(EntityTypes.COW,2,4,3);cow.setNoAi(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);
        var jump=new net.minecraft.world.entity.player.Input(false,false,false,false,true,false,false);
        rider.setLastClientInput(jump);wolf.setOnGround(true);WolfMount.tick(wolf);
        rider.setLastClientInput(net.minecraft.world.entity.player.Input.EMPTY);WolfMount.tick(wolf);
        h.assertTrue(cow.getHealth()<100,"Tap invokes the real wolf attack");
        h.assertTrue(wolf.swinging && wolf.isAggressive(),"Tap starts native swing and combat pose");
        float afterTap=cow.getHealth();
        for(int n=0;n<20;n++)WolfMount.tick(wolf);
        cow.invulnerableTime=0;
        rider.setLastClientInput(jump);
        for(int n=0;n<10;n++) {wolf.setOnGround(true);WolfMount.tick(wolf);}
        rider.setLastClientInput(net.minecraft.world.entity.player.Input.EMPTY);WolfMount.tick(wolf);
        wolf.setPos(wolf.position().add(0,0,.6));WolfMount.tick(wolf);
        h.assertTrue(cow.getHealth()<afterTap,"Pounce intersects and bites without second input");
        h.assertTrue(wolf.swinging,"Pounce bite emits native attack animation");
        float afterImpact=cow.getHealth();cow.invulnerableTime=0;
        wolf.setPos(wolf.position().add(0,0,.1));WolfMount.tick(wolf);
        h.assertTrue(cow.getHealth()==afterImpact,"Only one impact bite per pounce");
        rider.stopRiding();h.succeed();
    }

    @GameTest(maxTicks=60) public void missedBiteAnimatesAndPoseExpires(GameTestHelper h) {
        var rider=h.makeMockServerPlayerInLevel();rider.setGameMode(GameType.SURVIVAL);
        rider.getAttribute(Attributes.SCALE).setBaseValue(.76);
        var wolf=h.spawn(EntityTypes.WOLF,2,20,2);wolf.tame(rider);wolf.setNoAi(true);wolf.setNoGravity(true);
        wolf.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));rider.startRiding(wolf);
        rider.setLastClientInput(new net.minecraft.world.entity.player.Input(false,false,false,false,true,false,false));
        wolf.setOnGround(true);WolfMount.tick(wolf);
        rider.setLastClientInput(net.minecraft.world.entity.player.Input.EMPTY);WolfMount.tick(wolf);
        h.assertTrue(wolf.swinging && wolf.isAggressive(),"Empty bite starts the native animation");
        h.assertTrue(!wolf.isAngry() && wolf.getTarget()==null && wolf.isOwnedBy(rider),"Animation does not create anger or targets");
        rider.stopRiding();
        h.runAfterDelay(12,()->{
            h.assertFalse(wolf.swinging || wolf.isAggressive(),"Command pose expires even after dismount");
            var target=h.spawn(EntityTypes.COW,3,20,2);target.setNoAi(true);
            // Bypass vanilla's 60-tick boarding cooldown to isolate pose ownership.
            h.assertTrue(rider.startRiding(wolf,true,false),"Remount for independent pose check");
            wolf.setAggressive(true);
            h.assertTrue(WolfMount.attack(wolf,rider,target),"Second command executes a real attack");
            h.runAfterDelay(12,()->{
                h.assertTrue(wolf.isAggressive(),"Pre-existing native combat pose is preserved");
                rider.stopRiding();h.succeed();
            });
        });
    }
}
