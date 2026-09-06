package io.github.r3neer.scalebrews.physics;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GrowthImpact {
    private GrowthImpact() {}

    public static double radius(double level, double distance) {
        return Math.min(6, 1.5 + level * 0.65 + Math.min(20, Math.max(0, distance - 3)) * 0.08);
    }

    public static double strength(double level, double distance) {
        return Math.min(2, (0.2 + level * 0.2) * (1 + Math.min(20, Math.max(0, distance - 3)) * 0.06));
    }

    public static float damage(int level, double distance, double radialFraction) {
        if (level < 3 || distance <= 6) return 0;
        return (float) (Math.min(4, (distance - 6) * 0.4) * Math.clamp(1 - radialFraction, 0, 1));
    }

    public static void land(LivingEntity entity, double fallDistance, BlockState ground) {
        if (io.github.r3neer.scalebrews.platform.Platforms.supported(entity)) return;
        var rules = io.github.r3neer.scalebrews.config.ScaleRules.get(entity.level());
        if (!rules.growthImpact()) return;
        int tier = io.github.r3neer.scalebrews.scale.ScaleSize.tier(
                io.github.r3neer.scalebrews.scale.ScaleSize.growth(entity));
        if (tier == 0 || fallDistance <= 3 || !Double.isFinite(fallDistance) || ground.isAir()
                || entity.isSpectator() || (entity instanceof Player p && p.getAbilities().flying)
                || entity.isPassenger() || entity.isInWater()
                || !(entity.level() instanceof ServerLevel level)) return;
        double magnitude = io.github.r3neer.scalebrews.scale.ScaleSize.growth(entity);
        double radius = radius(magnitude, fallDistance);
        double power = strength(magnitude, fallDistance);
        var source = new net.minecraft.world.damagesource.DamageSource(
                level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(ScaleCombat.LANDING), entity);
        Vec3 origin = entity.position().add(0, 0.2, 0);
        var surfaces = new ImpactSurfaces(level, entity.position(), radius);
        AABB area = new AABB(origin.x - radius, entity.getY() - radius - .2, origin.z - radius,
                origin.x + radius, entity.getY() + radius + .2, origin.z + radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != entity && e.isAlive() && !e.isSpectator() && !entity.isAlliedTo(e))) {
            if (entity instanceof Player p && target instanceof Player other
                    && (!p.canHarmPlayer(other) || !level.isPvpAllowed())) continue;
            double dx = target.getX() - entity.getX();
            double dz = target.getZ() - entity.getZ();
            double distance = surfaces.distanceTo(target);
            if (distance >= radius) continue;
            float damage = damage(tier, fallDistance, distance / radius);
            if (damage > 0) target.hurtServer(level, source, damage);
            if (distance < 0.0001) { dx = 1; dz = 0; }
            target.knockback(power * (1 - distance / radius), -dx, -dz,
                    source, damage, true);
            target.hurtMarked = true;
        }
        var gust = switch (tier) {
            case 1 -> ParticleTypes.SMALL_GUST;
            case 2 -> ParticleTypes.GUST_EMITTER_SMALL;
            default -> ParticleTypes.GUST_EMITTER_LARGE;
        };
        level.sendParticles(gust, origin.x, origin.y, origin.z, 1, 0, 0, 0, 0);
        for (int i = 0; i < 12 * tier; i++) {
            double angle = i * Math.PI * 2 / (12 * tier);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    origin.x + Math.cos(angle) * radius * 0.6, origin.y, origin.z + Math.sin(angle) * radius * 0.6,
                    2, 0.12, 0.1, 0.12, 0.03);
        }
        level.playSound(null, entity.blockPosition(), SoundEvents.IRON_GOLEM_STEP, entity.getSoundSource(),
                0.7F + 0.25F * tier, 0.9F - 0.1F * tier);
    }
}
