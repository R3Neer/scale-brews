package io.github.r3neer.scalebrews.platform.anatomy;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.*;

/** State guards are common/server code, not calls into animation/render classes. */
public final class AnatomyPoseEligibility {
    private AnatomyPoseEligibility() {}
    private static final Map<Identifier,java.util.function.Predicate<LivingEntity>> ADAPTERS=new HashMap<>();
    private static final Set<String> DIAGNOSTICS=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final ClassValue<java.util.function.Predicate<LivingEntity>> GRIZZLY=new ClassValue<>() {
        protected java.util.function.Predicate<LivingEntity> computeValue(Class<?> type) {
            try {
                var sitting=type.getMethod("isSitting");var standing=type.getMethod("isStanding");var eating=type.getMethod("isEating");
                var held=type.getMethod("isEatingHeldItem");var freddy=type.getMethod("isFreddy");var animation=type.getMethod("getAnimation");
                Object none=type.getField("NO_ANIMATION").get(null);
                var progress=List.of(type.getField("standProgress"),type.getField("sitProgress"),type.getField("prevStandProgress"),type.getField("prevSitProgress"));
                return entity->{
                    try {
                        // 2.1.9 leaves currentAnimation null until its first animation update;
                        // the original-model reference fixture also verifies this ordinary state.
                        Object current=animation.invoke(entity);
                        if((boolean)sitting.invoke(entity) || (boolean)standing.invoke(entity) || (boolean)eating.invoke(entity)
                            || (boolean)held.invoke(entity) || (boolean)freddy.invoke(entity) || current!=null && current!=none)return false;
                        for(var field:progress)if(field.getFloat(entity)!=0)return false;
                        return true;
                    }catch(ReflectiveOperationException invalid){diagnose(type.getName());return false;}
                };
            }catch(ReflectiveOperationException invalid){diagnose(type.getName());return entity->false;}
        }
    };
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
            case "scalebrews:quadruped"->entity.getType()==EntityTypes.COW && entity.getPose()==Pose.STANDING;
            case "scalebrews:player_walking"->entity instanceof net.minecraft.world.entity.player.Player && entity.getPose()==Pose.STANDING
                && !entity.isCrouching() && !entity.swinging && !entity.isUsingItem() && entity.getMainHandItem().isEmpty() && entity.getOffhandItem().isEmpty();
            case "scalebrews:grizzly"->entity.getClass().getName().equals("com.github.alexthe666.alexsmobs.entity.EntityGrizzlyBear")
                && net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("alexsmobs").map(mod->mod.getMetadata().getVersion().getFriendlyString().equals("2.1.9")).orElse(false)
                && GRIZZLY.get(entity.getClass()).test(entity);
            default->{diagnose(provider.toString());yield false;}
        };
    }
}
