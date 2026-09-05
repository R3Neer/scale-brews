package io.github.r3neer.scalebrews.physics;

import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.config.ScaleRules;
import io.github.r3neer.scalebrews.effect.ScaleEffects;
import io.github.r3neer.scalebrews.scale.ScaleSprintHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/** Rules shared by the small vanilla integration mixins. All mechanics target players. */
public final class ScalePhysics {
    public static final TagKey<Block> INSENSITIVE_PLATES = TagKey.create(Registries.BLOCK, ScaleBrews.id("insensitive_pressure_plates"));
    public static final TagKey<Block> RESISTED_SLOWDOWN = TagKey.create(Registries.BLOCK, ScaleBrews.id("growth_resisted_slowdown"));
    public static final TagKey<GameEvent> QUIET_MOVEMENT = TagKey.create(Registries.GAME_EVENT, ScaleBrews.id("shrinking_quiet_movement"));

    private ScalePhysics() {}

    public static int growth(Entity entity) {
        return entity instanceof Player p ? ScaleSprintHandler.level(p.getEffect(ScaleEffects.GROWTH)) : 0;
    }

    public static int shrinking(Entity entity) {
        return entity instanceof Player p ? ScaleSprintHandler.level(p.getEffect(ScaleEffects.SHRINKING)) : 0;
    }

    public static boolean weightless(Entity entity) { return shrinking(entity) == 3; }

    public static boolean triggersPlate(Entity entity, BlockState plate) {
        if (!ScaleRules.get(entity.level()).bypassesPlates()) return true;
        int level = shrinking(entity);
        return level < 3 && (level != 2 || !plate.is(INSENSITIVE_PLATES));
    }

    public static double vibrationRange(int shrinking, boolean sprinting) {
        return Math.clamp(1 - 0.25 * shrinking + (sprinting && shrinking > 0 ? 0.25 : 0), 0.25, 1);
    }

    public static float fallDamage(float vanillaDamage, int shrinking) {
        return vanillaDamage * (1 - 0.08F * Math.clamp(shrinking, 0, 3));
    }

    public static float blockSpeed(Entity entity, BlockState block, float vanillaFactor) {
        if (!ScaleRules.get(entity.level()).resistsTerrain()) return vanillaFactor;
        int level = growth(entity);
        if (level == 0 || !block.is(RESISTED_SLOWDOWN) || vanillaFactor >= 1) return vanillaFactor;
        double penalty = switch (level) { case 1 -> 0.60; case 2 -> 0.25; default -> 0; };
        return (float) (1 - (1 - vanillaFactor) * penalty);
    }

    public static Vec3 stuckSpeed(Entity entity, BlockState block, Vec3 vanilla) {
        if (!ScaleRules.get(entity.level()).resistsTerrain()) return vanilla;
        int level = growth(entity);
        if (level == 0 || !block.is(RESISTED_SLOWDOWN)) return vanilla;
        double remaining = 1 - level / 3.0;
        return new Vec3(relieve(vanilla.x, remaining), relieve(vanilla.y, remaining), relieve(vanilla.z, remaining));
    }

    private static double relieve(double factor, double remaining) {
        return factor >= 1 ? factor : 1 - (1 - factor) * remaining;
    }
}
