package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.mount.*;
import io.github.r3neer.scalebrews.platform.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public class MountPlaytestTests {
    @GameTest public void automaticSurfaceNeedsNoSpeciesProfile(GameTestHelper h) {
        var support=h.spawn(EntityTypes.ZOMBIE,2,20,2); support.setNoAi(true); support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(3.88); support.refreshDimensions();
        h.assertTrue(Platforms.definition(support).surfaces().getFirst().id().equals("automatic_top"),"Unlisted mob gets physical fallback");
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(support.position().add(0,support.getBbHeight()+.3,0));
        player.move(MoverType.SELF,new Vec3(0,-.6,0));
        h.assertTrue(Platforms.state(player).support==support,"Normal player lands on unlisted giant");
        var before=player.position(); support.setPos(support.position().add(.2,.1,.1)); PlatformPhysics.carry(player);
        h.assertTrue(player.position().distanceTo(before.add(.2,.1,.1))<.001,"Automatic surface carries player");
        support.getAttribute(Attributes.SCALE).setBaseValue(4.2); support.refreshDimensions(); PlatformPhysics.carry(player);
        h.assertTrue(Platforms.supported(player),"External scale change keeps automatic support");
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("alexsmobs")) {
            var type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(
                net.minecraft.resources.Identifier.parse("alexsmobs:lobster"));
            var modded=(Mob)h.spawn(type,6,20,2); modded.setNoAi(true); modded.setNoGravity(true);
            modded.getAttribute(Attributes.SCALE).setBaseValue(3.88); modded.refreshDimensions();
            h.assertTrue(Platforms.definition(modded).surfaces().getFirst().id().equals("automatic_top"),"Alex's Mobs needs no species profile");
            var rider=h.makeMockPlayer(GameType.SURVIVAL);
            rider.setPos(modded.position().add(0,modded.getBbHeight()+.3,0));
            rider.move(MoverType.SELF,new Vec3(0,-.6,0));
            h.assertTrue(Platforms.state(rider).support==modded,"Actual contact on unprofiled Alex's Mobs lobster");
        }
        h.succeed();
    }
    @GameTest public void normalPlayerOnGrowthThreeProfiles(GameTestHelper h) {
        for (var type : java.util.List.of(EntityTypes.IRON_GOLEM, EntityTypes.CAT, EntityTypes.VILLAGER, EntityTypes.CHICKEN, EntityTypes.COW)) {
            var support = (Mob)h.spawn(type, 2, 20, 2);
            support.setNoAi(true); support.setNoGravity(true);
            support.getAttribute(Attributes.SCALE).setBaseValue(3.88); support.refreshDimensions();
            Platforms.noteSupport(support);
            var body = h.makeMockPlayer(GameType.SURVIVAL);
            h.assertTrue(Platforms.eligible(body, support), "Normal player eligible on Growth III " + type);
            var surface = Platforms.definition(support).surfaces().getFirst();
            var top = PlatformGeometry.frame(support).world(new Vec3(surface.x(), surface.y(), surface.z()));
            body.setPos(top.add(0, .4, 0));
            body.move(MoverType.SELF, new Vec3(0, -.8, 0));
            h.assertTrue(Platforms.state(body).support == support, "Actual downward movement lands on " + type);
            var start = body.position();
            support.setPos(support.position().add(.2, .1, .2)); PlatformPhysics.carry(body);
            h.assertTrue(body.position().distanceTo(start.add(.2,.1,.2)) < .001, "Support transports player " + type);
            body.discard(); support.discard();
        }
        h.succeed();
    }
    @GameTest public void equipmentIndependentOfSizeAndOwner(GameTestHelper h) {
        var wolf = h.spawn(EntityTypes.WOLF, 2, 2, 2);
        var owner = h.makeMockPlayer(GameType.SURVIVAL);
        var guest = h.makeMockPlayer(GameType.SURVIVAL);
        guest.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SADDLE, 2));
        wolf.interact(guest, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(wolf.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(), "Wild wolf rejects saddle");
        h.assertTrue(guest.getMainHandItem().getCount() == 2, "Rejected saddle not consumed");
        wolf.tame(owner);
        wolf.interact(guest, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(wolf.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE), "Normal-size non-owner may saddle");
        h.assertTrue(guest.getMainHandItem().getCount() == 1 && wolf.isOwnedBy(owner), "One saddle consumed, owner retained");
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        wolf.setOrderedToSit(false);
        wolf.interact(owner, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(wolf.isOrderedToSit() && !owner.isPassenger(), "Normal click retains sit command despite rider size");
        guest.getAttribute(Attributes.SCALE).setBaseValue(.52);
        guest.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); guest.setShiftKeyDown(true);
        wolf.interact(guest, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(guest.getVehicle() == wolf && !wolf.isOrderedToSit(), "Shift click mounts borrowed wolf");
        wolf.setItemSlot(EquipmentSlot.SADDLE, ItemStack.EMPTY); TinyMounts.enforceRider(guest);
        h.assertTrue(guest.getVehicle() == wolf && TinyMounts.controller(wolf) == null, "Removing saddle retains passive passenger");
        h.succeed();
    }
    @GameTest public void wildWolfRidingTamesAndRetaliates(GameTestHelper h) {
        var wolf = h.spawn(EntityTypes.WOLF, 2, 2, 2); wolf.setNoAi(true);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.getAttribute(Attributes.SCALE).setBaseValue(.52); player.setShiftKeyDown(true);
        for (int attempt = 0; attempt < 3; attempt++) {
            ((io.github.r3neer.scalebrews.test.mixin.TestEntityAccess)player).test$setBoardingCooldown(0);
            wolf.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertTrue(player.getVehicle() == wolf && TinyMounts.controller(wolf) == null, "Wild riding never grants control");
            for (int tick = 0; tick < 60; tick++) WolfTaming.tick(wolf);
            if (attempt < 2) h.assertTrue(!player.isPassenger() && wolf.getTarget() == player && !wolf.isTame(), "Wild wolf ejects and targets rider");
        }
        h.assertTrue(wolf.isOwnedBy(player) && player.getVehicle() == wolf && wolf.getTarget() == null, "Third sustained attempt tames without saddle");
        player.stopRiding(); h.assertTrue(wolf.getTarget() == null, "Tamed dismount is peaceful");
        var wild = h.spawn(EntityTypes.WOLF, 4, 2, 2);
        player.startRiding(wild,true,true); player.stopRiding();
        h.assertTrue(wild.getTarget() == player, "Voluntary early dismount also provokes wild wolf");
        h.succeed();
    }
    @GameTest public void unsaddledChickenAndBeeRetainOwnControl(GameTestHelper h) {
        for (var type : java.util.List.of(EntityTypes.CHICKEN, EntityTypes.BEE)) {
            var mob = (Mob)h.spawn(type, 2, 2, 2);
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            player.getAttribute(Attributes.SCALE).setBaseValue(.28);
            mob.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertTrue(player.getVehicle() == mob && TinyMounts.controller(mob) == null, "Unsaddled passive mount " + type);
            TinyMounts.enforceRider(player);
            h.assertTrue(player.getVehicle() == mob, "Passive rider retained");
            player.stopRiding(); mob.discard();
        }
        h.succeed();
    }
    @GameTest public void playerHeadRemainsAnatomical(GameTestHelper h) {
        var giant=h.makeMockPlayer(GameType.SURVIVAL);
        giant.getAttribute(Attributes.SCALE).setBaseValue(3.88); giant.refreshDimensions();
        giant.setPos(h.absoluteVec(new Vec3(2,20,2))); giant.setNoGravity(true); h.getLevel().addFreshEntity(giant);
        var head=Platforms.definition(giant).surfaces().getFirst();
        h.assertTrue(head.id().equals("head") && head.visual().isPresent(),"Player retains model-specific animated head profile");
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var top=PlatformGeometry.frame(giant).world(new Vec3(0,head.y(),0));
        player.setPos(top.add(0,.3,0)); player.move(MoverType.SELF,new Vec3(0,-.6,0));
        h.assertTrue(Platforms.state(player).support==giant,"Normal player lands on giant player head");
        var before=player.position();giant.setPos(giant.position().add(.2,0,.2)); PlatformPhysics.carry(player);
        h.assertTrue(player.position().distanceTo(before.add(.2,0,.2))<.001,"Player carries player");
        double standing=player.getY();giant.setPose(Pose.CROUCHING);giant.refreshDimensions();PlatformPhysics.carry(player);
        h.assertTrue(player.getY()<standing-.5 && Platforms.supported(player),"Crouching giant lowers contact without losing rider");
        h.succeed();
    }
}
