package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.integration.gravity.GravityFrame;
import io.github.r3neer.scalebrews.mount.TinyMountGravity;
import io.github.r3neer.scalebrews.mount.TinyMounts;
import io.github.r3neer.scalebrews.mount.WolfMount;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public class TinyMountGravityTests {
    private static final double EPS = 1.0E-6;

    @GameTest public void beeFlightUsesRootGravityForAllSixFrames(GameTestHelper h) {
        if (!TestGravityFrames.ensure()) { h.succeed(); return; }
        var rider = h.makeMockServerPlayerInLevel();
        rider.setGameMode(GameType.SURVIVAL);
        rider.getAttribute(Attributes.SCALE).setBaseValue(.5);
        var bee = h.spawn(EntityTypes.BEE, 2, 3, 2);
        bee.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        rider.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(io.github.r3neer.scalebrews.item.ScaleItems.FLOWER_ON_A_STICK));
        h.assertTrue(rider.startRiding(bee), "Bee uses ordinary passenger relation");
        h.assertTrue(TinyMounts.controller(bee) == rider, "Bee controller established");
        rider.setYRot(37);
        rider.setXRot(-23);
        var definition = TinyMounts.definition(bee);

        TestGravityFrames.set(bee, Direction.DOWN);
        Vec3 localBaseline = TinyMounts.flightVelocity(rider, definition);
        for (Direction direction : Direction.values()) {
            TestGravityFrames.set(bee, direction);
            Vec3 actual = TinyMounts.flightVelocity(rider, definition);
            Vec3 expected = new GravityFrame(direction).toWorld(localBaseline);
            near(h, actual, expected, "Bee generated velocity follows root frame " + direction);
        }
        rider.stopRiding();
        h.succeed();
    }

    @GameTest public void chickenGlideRuleDampsOnlyLocalDownwardVelocity(GameTestHelper h) {
        for (Direction direction : Direction.values()) {
            GravityFrame frame = new GravityFrame(direction);
            Vec3 falling = frame.toWorld(new Vec3(.25, -1, .4));
            near(h, TinyMountGravity.dampLocalFall(falling, frame, .6, true), falling,
                    "Released Space suppresses local fall damping " + direction);
            near(h, frame.toLocal(TinyMountGravity.dampLocalFall(falling, frame, .6, false)),
                    new Vec3(.25, -.6, .4), "Held Space damps only local vertical " + direction);

            Vec3 rising = frame.toWorld(new Vec3(.25, .2, .4));
            near(h, TinyMountGravity.dampLocalFall(rising, frame, .6, false), rising,
                    "Local upward movement is never damped " + direction);
        }
        h.succeed();
    }

    @GameTest public void wolfPounceAndLandingUseCurrentRootFrame(GameTestHelper h) {
        if (!TestGravityFrames.ensure()) { h.succeed(); return; }
        for (Direction direction : Direction.values()) {
            var rider = h.makeMockServerPlayerInLevel();
            rider.setGameMode(GameType.SURVIVAL);
            rider.getAttribute(Attributes.SCALE).setBaseValue(.76);
            var wolf = h.spawn(EntityTypes.WOLF, 2, 15, 2);
            wolf.tame(rider);
            wolf.setNoAi(true);
            wolf.setNoGravity(true);
            wolf.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
            h.assertTrue(rider.startRiding(wolf, true, false), "Wolf mounts for " + direction);
            h.assertTrue(TinyMounts.controller(wolf) == rider, "Wolf controller established " + direction);
            TestGravityFrames.set(wolf, direction);
            rider.setYRot(29);
            rider.setXRot(-17);

            Input jump = new Input(false, false, false, false, true, false, false);
            rider.setLastClientInput(jump);
            for (int i = 0; i < 10; i++) {
                wolf.setOnGround(true);
                WolfMount.tick(wolf);
            }
            rider.setLastClientInput(Input.EMPTY);
            wolf.setOnGround(true);
            WolfMount.tick(wolf);

            GravityFrame frame = new GravityFrame(direction);
            Vec3 expectedLocal = WolfMount.launchVelocity(29, -17, 1);
            near(h, frame.toLocal(wolf.getDeltaMovement()), expectedLocal,
                    "Pounce generated in current root frame " + direction);

            wolf.setDeltaMovement(frame.toWorld(new Vec3(.5, -.3, .25)));
            wolf.setOnGround(true);
            WolfMount.tick(wolf);
            near(h, frame.toLocal(wolf.getDeltaMovement()), new Vec3(0, -.3, 0),
                    "Landing clears only local tangential velocity " + direction);
            rider.stopRiding();
            wolf.discard();
        }
        h.succeed();
    }

    @GameTest public void frameChangeAffectsOnlySubsequentGeneratedPounce(GameTestHelper h) {
        if (!TestGravityFrames.ensure()) { h.succeed(); return; }
        var rider = h.makeMockServerPlayerInLevel();
        rider.setGameMode(GameType.SURVIVAL);
        rider.getAttribute(Attributes.SCALE).setBaseValue(.76);
        var wolf = h.spawn(EntityTypes.WOLF, 2, 20, 2);
        wolf.tame(rider);
        wolf.setNoAi(true);
        wolf.setNoGravity(true);
        wolf.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        h.assertTrue(rider.startRiding(wolf, true, false), "Wolf mounts");
        rider.setYRot(0);
        rider.setXRot(0);

        TestGravityFrames.set(wolf, Direction.DOWN);
        Vec3 existingMomentum = new Vec3(.31, .17, -.23);
        wolf.setDeltaMovement(existingMomentum);
        Input jump = new Input(false, false, false, false, true, false, false);
        rider.setLastClientInput(jump);
        for (int i = 0; i < 10; i++) {
            wolf.setOnGround(true);
            WolfMount.tick(wolf);
        }
        near(h, wolf.getDeltaMovement(), existingMomentum, "Charging does not rotate existing world momentum");

        TestGravityFrames.set(wolf, Direction.EAST);
        rider.setLastClientInput(Input.EMPTY);
        wolf.setOnGround(true);
        WolfMount.tick(wolf);
        Vec3 expected = new GravityFrame(Direction.EAST).toWorld(WolfMount.launchVelocity(0, 0, 1));
        near(h, wolf.getDeltaMovement(), expected, "Release uses the new frame once");
        rider.stopRiding();
        h.succeed();
    }

    private static void near(GameTestHelper h, Vec3 actual, Vec3 expected, String message) {
        h.assertTrue(actual.distanceToSqr(expected) <= EPS * EPS,
                message + ": " + actual + " != " + expected);
    }
}
