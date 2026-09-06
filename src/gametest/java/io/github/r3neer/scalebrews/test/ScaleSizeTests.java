package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.effect.ScaleEffects;
import io.github.r3neer.scalebrews.scale.*;
import io.github.r3neer.scalebrews.physics.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class ScaleSizeTests {
    @GameTest public void allMixedPairsAndNormalCancellation(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);
        double walking=p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        for(int growth=1;growth<=3;growth++) for(int shrink=1;shrink<=3;shrink++) {
            p.removeAllEffects(); p.getAttribute(Attributes.SCALE).setBaseValue(1);
            p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH,1200,growth-1));
            p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING,1200,shrink-1)); TestScale.settle(p);
            double size=(1+.96*growth)*(1-.242*shrink);
            double g=Math.max(0,(size-1)/.96),s=Math.max(0,(1-size)/.242);
            near(h,p.getMaxHealth(),20+6*g-4*s,"Mixed health "+growth+"/"+shrink);
            near(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED),walking*(1+.08*g-.08*s),"Mixed walk");
            p.getAttribute(Attributes.SCALE).setBaseValue(1/size); ScaleSize.tick(p);
            near(h,p.getMaxHealth(),20,"Normal cancellation removes health changes");
            near(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED),walking,"Normal cancellation removes movement changes");
            near(h,ScaleExhaustion.physical(p,1),1,"Normal cancellation removes physical exhaustion changes");
            h.assertTrue(ScalePhysics.growth(p)==0 && ScalePhysics.shrinking(p)==0,"Normal cancellation removes discrete abilities");
        }
        h.succeed();
    }
    @GameTest public void e4mcDedicatedPermissions(GameTestHelper h) throws ReflectiveOperationException {
        if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("e4mc")) {
            var requirement=Class.forName("link.e4mc.E4mcClient").getDeclaredMethod("lambda$registerCommands$0",net.minecraft.commands.CommandSourceStack.class);
            requirement.setAccessible(true);
            var source=h.getLevel().getServer().createCommandSourceStack();
            h.assertTrue((boolean)requirement.invoke(null,source),"Dedicated owner may administer e4mc");
            h.assertFalse((boolean)requirement.invoke(null,source.withPermission(net.minecraft.server.permissions.PermissionSet.NO_PERMISSIONS)),"Ordinary players cannot administer e4mc");
        }
        h.succeed();
    }
    @GameTest public void mixedEffectsUsePhysicalSize(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);
        double walking=p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH,1200,2));
        p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING,1200,2));
        TestScale.settle(p);
        double size=3.88*.274;
        double growth=(size-1)/.96;
        near(h,p.getAttributeValue(Attributes.SCALE),size,"Mixed actual scale");
        near(h,ScaleSize.growth(p),growth,"Only residual growth applies");
        near(h,ScaleSize.shrinking(p),0,"No shrinking penalties on a larger body");
        near(h,p.getMaxHealth(),20+6*growth,"Health follows residual size");
        near(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED),walking*(1+.08*growth),"Walk follows residual size");
        near(h,ScaleExhaustion.physical(p,1),1+.1*growth,"Exhaustion follows residual size");
        h.assertTrue(ScalePhysics.growth(p)==0 && ScalePhysics.shrinking(p)==0,"No discrete giant/tiny abilities near normal");
        p.removeEffect(ScaleEffects.GROWTH);
        p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH,1200,0));
        TestScale.settle(p);
        h.assertTrue(ScaleSize.signedLevel(p)<0 && p.getMaxHealth()<20,"Growth potion cannot confer giant benefits while physically small");
        h.succeed();
    }

    @GameTest public void externalSizeAndImpactThresholds(GameTestHelper h) {
        ImpactSurfaceTests.flatFloor(h,19);
        var p=h.makeMockPlayer(GameType.SURVIVAL);
        p.setPos(h.absoluteVec(new Vec3(1,20,1)));
        var target=h.spawnWithNoFreeWill(EntityTypes.PIG,2.5F,20,1);
        for(double size:new double[]{.28,1,1.959,1.96,2.92,3.879,3.88}) {
            p.getAttribute(Attributes.SCALE).setBaseValue(size);
            ScaleSize.tick(p);
            target.setDeltaMovement(Vec3.ZERO); target.setHealth(target.getMaxHealth()); target.invulnerableTime=0;
            GrowthImpact.land(p,10,Blocks.STONE.defaultBlockState());
            h.assertTrue((target.getDeltaMovement().lengthSqr()>0)==(size>=1.96),"Impact threshold at actual size "+size);
            h.assertTrue((target.getHealth()<target.getMaxHealth())==(size>=3.88),"Damage threshold at actual size "+size);
        }
        p.getAttribute(Attributes.SCALE).setBaseValue(1); ScaleSize.tick(p);
        near(h,p.getMaxHealth(),20,"External size returning to normal removes bonuses");
        target.discard(); h.succeed();
    }

    @GameTest public void derivedAttributesFollowBlend(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL); p.setHealth(10);
        var transition=new ScaleTransition();
        p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH,1200,0));
        near(h,p.getMaxHealth(),20,"No instantaneous giant attribute bonus");
        for(int i=1;i<=20;i++) {
            transition.tick(p); ScaleSize.tick(p);
            near(h,p.getMaxHealth(),20+6*ScaleSize.growth(p),"Attribute matches this blend tick");
            near(h,p.getHealth()/p.getMaxHealth(),.5,"Health fraction survives gradual resizing");
        }
        h.succeed();
    }
    private static void near(GameTestHelper h,double actual,double expected,String message) {
        h.assertTrue(Math.abs(actual-expected)<1e-4,message+": "+actual+" != "+expected);
    }
}
