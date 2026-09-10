package io.github.r3neer.scalebrews.collision.internal;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.*;

/** State guards are common/server code, not calls into animation/render classes. */
public final class AnatomyPoseEligibility {
    private AnatomyPoseEligibility() {}
    private static final Map<Identifier,java.util.function.Predicate<LivingEntity>> ADAPTERS=new HashMap<>();
    private static final Set<String> DIAGNOSTICS=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static void diagnose(String key) {
        if(DIAGNOSTICS.add(key))io.github.r3neer.scalebrews.ScaleBrews.LOGGER.warn("Anatomical pose unavailable: {}. No box/static fallback is used.",key);
    }
    public static synchronized void register(Identifier id,java.util.function.Predicate<LivingEntity> guard) {
        if(ADAPTERS.putIfAbsent(Objects.requireNonNull(id),Objects.requireNonNull(guard))!=null)throw new IllegalArgumentException("Duplicate pose guard "+id);
    }
    public static boolean supported(Identifier provider,LivingEntity entity) {
        if(!Platforms.ordinary(entity))return false;
        java.util.function.Predicate<LivingEntity> custom;
        synchronized(AnatomyPoseEligibility.class){custom=ADAPTERS.get(provider);}
        if(custom!=null)return custom.test(entity);
        return switch(provider.toString()) {
            case "scalebrews:static"->true;
            case "scalebrews:quadruped"->java.util.Set.of(EntityTypes.COW,EntityTypes.PIG,EntityTypes.SHEEP,EntityTypes.LLAMA,EntityTypes.TRADER_LLAMA).contains(entity.getType())
                && entity.getPose()==Pose.STANDING && (!(entity instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep) || sheep.getHeadEatPositionScale(1)==0);
            case "scalebrews:chicken"->entity.getType()==EntityTypes.CHICKEN && entity.getPose()==Pose.STANDING;
            case "scalebrews:villager"->entity.getType()==EntityTypes.VILLAGER && entity.getPose()==Pose.STANDING;
            case "scalebrews:iron_golem"->entity.getType()==EntityTypes.IRON_GOLEM && entity.getPose()==Pose.STANDING;
            case "scalebrews:ghast"->entity.getType()==EntityTypes.GHAST;
            case "scalebrews:feline"->entity.getType()==EntityTypes.CAT && entity.getPose()==Pose.STANDING;
            case "scalebrews:equine"->java.util.Set.of(EntityTypes.HORSE,EntityTypes.DONKEY,EntityTypes.MULE,EntityTypes.SKELETON_HORSE,EntityTypes.ZOMBIE_HORSE).contains(entity.getType())
                && entity instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse && horse.getEatAnim(1)==0 && horse.getStandAnim(1)==0 && horse.getMouthAnim(1)==0 && !horse.isInWater();
            case "scalebrews:bee"->entity.getType()==EntityTypes.BEE;
            case "scalebrews:player_walking"->entity instanceof net.minecraft.world.entity.player.Player && entity.getPose()==Pose.STANDING
                && !entity.isCrouching() && !entity.swinging && !entity.isUsingItem() && entity.getMainHandItem().isEmpty() && entity.getOffhandItem().isEmpty();
            default->{diagnose(provider.toString());yield false;}
        };
    }
}
