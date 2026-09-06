package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.effect.ScaleEffects;
import io.github.r3neer.scalebrews.mixin.BeaconEffectsAccessor;
import io.github.r3neer.scalebrews.scale.ScaleTransition;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

public class ScaleBrewsTests {
    @GameTest public void shrinkingThreeFitsHalfBlockGap(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);
        var topSlab=net.minecraft.world.level.block.Blocks.STONE_SLAB.defaultBlockState()
            .setValue(net.minecraft.world.level.block.SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.TOP);
        for(int x=1;x<=3;x++) {
            h.setBlock(x,1,2,net.minecraft.world.level.block.Blocks.STONE);
            h.setBlock(x,2,2,topSlab);
        }
        var inside=h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5,2,2.5));
        p.setPos(inside);
        h.assertFalse(h.getLevel().noCollision(p,p.getBoundingBox()),"Normal standing body cannot fit under top slab");
        p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING,200,2));TestScale.settle(p);
        near(h,p.getScale(),.274,"Shrinking III scale");
        near(h,p.getBbHeight(),.4932,"Standing collision height below half a block");
        h.assertTrue(h.getLevel().noCollision(p,p.getBoundingBox()),"Shrinking III fits actual half-block gap");
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(.5,2,2.5)));
        p.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(1,0,0));
        near(h,p.getX(),inside.x,"Actual horizontal movement enters gap");
        near(h,p.getY(),inside.y,"No stepping or crouching needed");
        h.succeed();
    }
    @GameTest
    public void shrinkingJumpStrength(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var attribute = p.getAttribute(Attributes.JUMP_STRENGTH);
        double original = attribute.getValue();
        var external = Identifier.fromNamespaceAndPath("test", "external_jump");
        attribute.addTransientModifier(new AttributeModifier(external, .2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        for (int level = 1; level <= 3; level++) {
            p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 200, level - 1)); TestScale.settle(p);
            near(h, attribute.getValue(), original * 1.2 * (1 + .025 * level), "Shrinking jump strength tier " + level);
            p.removeEffect(ScaleEffects.SHRINKING); TestScale.settle(p);
            near(h, attribute.getValue(), original * 1.2, "Removing Shrinking retains external jump modifier");
        }
        attribute.removeModifier(external);
        near(h, attribute.getValue(), original, "Vanilla jump restored");
        h.succeed();
    }

    private static void near(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < 0.00001, message + ": " + actual + " expected " + expected);
    }

    @GameTest
    public void sprintAndWalking(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        double walking = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double[] growth = {1.20, 1.12, 1.05};
        double[] shrinking = {1.50, 1.90, 2.50};
        for (int i = 0; i < 3; i++) {
            p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 200, i)); TestScale.settle(p);
            p.setSprinting(false);
            near(h, p.getAttributeValue(Attributes.MOVEMENT_SPEED), walking * (1 + .08 * (i+1)), "Growth walking");
            p.setSprinting(true);
            double growthSprint = walking * (1 + .08 * (i+1)) * growth[i];
            near(h, p.getAttributeValue(Attributes.MOVEMENT_SPEED), growthSprint, "Growth sprint");
            p.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, i)); TestScale.settle(p);
            near(h, p.getAttributeValue(Attributes.MOVEMENT_SPEED), growthSprint * (1 + .2 * (i+1)), "Growth stacks with Speed");
            p.removeEffect(MobEffects.SPEED); TestScale.settle(p);
            p.removeEffect(ScaleEffects.GROWTH); TestScale.settle(p);
            p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 200, i)); TestScale.settle(p);
            double shrinkSprint = walking * (1 - .08 * (i+1)) * shrinking[i];
            near(h, p.getAttributeValue(Attributes.MOVEMENT_SPEED), shrinkSprint, "Shrinking while already sprinting");
            p.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, i)); TestScale.settle(p);
            near(h, p.getAttributeValue(Attributes.MOVEMENT_SPEED), shrinkSprint * (1 + .2 * (i+1)), "Shrinking stacks with Speed");
            p.removeEffect(MobEffects.SPEED); TestScale.settle(p);
            double speedSprint = walking * 1.3 * (1 + .2 * (i+1));
            h.assertTrue(growthSprint < speedSprint && shrinkSprint < speedSprint, "Neither beats equivalent Speed sprint");
            p.setSprinting(false);
            near(h, p.getAttributeValue(Attributes.MOVEMENT_SPEED), walking * (1 - .08 * (i+1)), "Shrinking walking");
            p.removeEffect(ScaleEffects.SHRINKING); TestScale.settle(p);
        }
        p.setSprinting(true);
        near(h, p.getAttributeValue(Attributes.MOVEMENT_SPEED), walking * 1.3, "Restored vanilla sprint");
        h.succeed();
    }

    @GameTest
    public void beaconRules(GameTestHelper h) {
        for (var effect : java.util.List.of(ScaleEffects.GROWTH, ScaleEffects.SHRINKING)) {
            h.assertTrue(BeaconBlockEntity.BEACON_EFFECTS.get(2).contains(effect), "Third tier UI list");
            h.assertTrue(BeaconEffectsAccessor.scalebrews$validEffects().contains(effect), "Persistence allowlist");
            h.assertFalse(BeaconBlockEntity.validateEffects(effect, null, 2), "Two tiers cannot select it");
            h.assertTrue(BeaconBlockEntity.validateEffects(effect, null, 3), "Three tiers allow level I");
            h.assertTrue(BeaconBlockEntity.validateEffects(effect, effect, 4), "Four tiers allow level II");
            h.assertFalse(BeaconBlockEntity.validateEffects(effect, effect, 3), "No secondary at tier three");
        }
        h.succeed();
    }

    @GameTest
    public void scaleTransitionAndExternalModifier(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var scale = p.getAttribute(Attributes.SCALE);
        var external = Identifier.fromNamespaceAndPath("test", "external_scale");
        scale.addTransientModifier(new AttributeModifier(external, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        var transition = new ScaleTransition();
        p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 200, 0));
        near(h, p.getScale(), 1.5, "No instant scale change");
        double last = p.getScale();
        for (int i = 1; i <= 20; i++) {
            transition.tick(p);
            h.assertTrue(p.getScale() >= last, "Monotonic growth");
            if (i == 10) near(h, p.getScale(), 1.5 * 1.48, "Halfway scale");
            last = p.getScale();
        }
        near(h, p.getScale(), 1.5 * 1.96, "Final scale composes with other mods");
        p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 400, 0));
        transition.tick(p);
        near(h, p.getScale(), last, "Beacon renewal does not restart transition");
        p.removeEffect(ScaleEffects.GROWTH);
        for (int i = 0; i < 20; i++) transition.tick(p);
        near(h, p.getScale(), 1.5, "Removal restores other mod scale");
        h.assertTrue(scale.getModifier(ScaleTransition.MODIFIER) == null, "Transition modifier cleaned up");
        h.assertTrue(scale.getModifier(external) != null, "Other mod modifier preserved");
        h.succeed();
    }

    @GameTest
    public void healthAndHiddenEffect(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setHealth(15);
        p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 200, 0));
        near(h, p.getHealth() / p.getMaxHealth(), 0.75, "Health on growth");
        p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 2, 2));
        near(h, p.getHealth() / p.getMaxHealth(), 0.75, "Health on upgrade");
        for (int i = 0; i < 3; i++) p.tick();
        near(h, p.getHealth() / p.getMaxHealth(), 0.75, "Health on hidden-effect downgrade");
        p.removeEffect(ScaleEffects.GROWTH);
        TestScale.settle(p);
        near(h, p.getHealth(), 15, "No healing or damage after cycle");
        h.succeed();
    }
}
