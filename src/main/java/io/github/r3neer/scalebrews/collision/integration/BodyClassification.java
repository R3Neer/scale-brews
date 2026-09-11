package io.github.r3neer.scalebrews.collision.integration;

import io.github.r3neer.scalebrews.collision.api.CollisionAdapters;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

/** Stable body classification/state boundary shared by legacy and replacement collision paths. */
public final class BodyClassification {
    private BodyClassification() {}

    public static String category(Entity entity) {
        if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) return null;
        var adapter = CollisionAdapters.body(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())).orElse(null);
        if (adapter != null) return adapter.permits(entity) ? adapter.category() : null;
        if (entity instanceof Player) return "players";
        if (entity instanceof Mob) return "mobs";
        if (entity instanceof AbstractBoat) return "boats";
        if (entity instanceof AbstractMinecart cart) return cart.isOnRails() ? null : "minecarts";
        if (entity instanceof ItemEntity) return "items";
        if (entity instanceof FallingBlockEntity) return "falling_blocks";
        return null;
    }

    public static boolean ordinary(Entity entity) {
        if (!entity.isAlive() || entity.noPhysics || entity.isSpectator() || entity.isPassenger()) return false;
        if (entity instanceof LivingEntity living && (living.isSleeping() || living.isFallFlying() || living.isSwimming() || living.isBaby())) return false;
        if (entity instanceof Player player && player.getAbilities().flying) return false;
        if (entity instanceof Camel camel && camel.isCamelSitting()) return false;
        if (entity instanceof net.minecraft.world.entity.animal.feline.Cat cat && (cat.isInSittingPose() || cat.isLying())) return false;
        return true;
    }

    public static Vec3 adaptTransport(Entity body, Vec3 requested) {
        var adapter = CollisionAdapters.body(BuiltInRegistries.ENTITY_TYPE.getKey(body.getType())).orElse(null);
        return adapter == null ? requested : adapter.transport(body, requested);
    }
}
