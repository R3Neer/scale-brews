package io.github.r3neer.scalebrews.physics;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class GrowthImpact {
    private GrowthImpact() {}

    public static double radius(int level, double distance) {
        return Math.min(6, 1.5 + level * 0.65 + Math.min(20, Math.max(0, distance - 3)) * 0.08);
    }

    public static double strength(int level, double distance) {
        return Math.min(2, (0.2 + level * 0.2) * (1 + Math.min(20, Math.max(0, distance - 3)) * 0.06));
    }

    public static float damage(int level, double distance, double radialFraction) {
        if (level < 3 || distance <= 6) return 0;
        return (float) (Math.min(4, (distance - 6) * 0.4) * Math.clamp(1 - radialFraction, 0, 1));
    }

    public static void land(Player player, double fallDistance, BlockState ground) {
        int tier = ScalePhysics.growth(player);
        if (tier == 0 || fallDistance <= 3 || !Double.isFinite(fallDistance) || ground.isAir()
                || player.isSpectator() || player.getAbilities().flying || player.isPassenger() || player.isInWater()
                || !(player.level() instanceof ServerLevel level)) return;
        double radius = radius(tier, fallDistance);
        double power = strength(tier, fallDistance);
        Vec3 origin = player.position().add(0, 0.2, 0);
        AABB area = new AABB(origin.x - radius, origin.y - 1, origin.z - radius,
                origin.x + radius, origin.y + 2, origin.z + radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isSpectator() && !player.isAlliedTo(e))) {
            if (target instanceof Player other && !player.canHarmPlayer(other)) continue;
            if (target instanceof Player && !level.isPvpAllowed()) continue;
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance >= radius) continue;
            Vec3 destination = target.position().add(0, 0.2, 0);
            if (level.clip(new ClipContext(origin, destination, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) continue;
            float damage = damage(tier, fallDistance, distance / radius);
            if (damage > 0) target.hurtServer(level, level.damageSources().playerAttack(player), damage);
            if (distance < 0.0001) { dx = 1; dz = 0; }
            target.knockback(power * (1 - distance / radius), -dx, -dz,
                    level.damageSources().playerAttack(player), damage, true);
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
        level.playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_STEP, SoundSource.PLAYERS,
                0.7F + 0.25F * tier, 0.9F - 0.1F * tier);
    }
}
