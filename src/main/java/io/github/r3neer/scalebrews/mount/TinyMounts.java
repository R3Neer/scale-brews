package io.github.r3neer.scalebrews.mount;

import io.github.r3neer.scalebrews.ScaleBrews;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import java.util.function.Function;

public final class TinyMounts {
    public static final ResourceKey<Registry<TinyMountDefinition>> REGISTRY = ResourceKey.createRegistryKey(ScaleBrews.id("tiny_mount"));
    /** Installed only by the client entrypoint. Server movement/tests use vanilla input packets. */
    public static Function<Player, Input> clientInput = player -> Input.EMPTY;
    private static final ClassValue<Boolean> GENERIC_MOB = new ClassValue<>() {
        protected Boolean computeValue(Class<?> type) {
            try {
                // Never replace an entity class's native controlling-passenger implementation.
                return type.getMethod("getControllingPassenger").getDeclaringClass() == Mob.class;
            } catch (NoSuchMethodException e) { return false; }
        }
    };
    private TinyMounts() {}

    public static void initialize() {
        MountSizePolicy.initialize();
        WolfMount.initialize();
        DynamicRegistries.registerSynced(REGISTRY, TinyMountDefinition.CODEC);
        ServerMobEffectEvents.AFTER_ADD.register((effect, entity, context) -> { if (entity instanceof Player p) enforceRider(p); });
        ServerMobEffectEvents.AFTER_REMOVE.register((effect, entity, context) -> { if (entity instanceof Player p) enforceRider(p); });
    }

    public static TinyMountDefinition definition(Entity entity) {
        var definition = configuredDefinition(entity);
        return definition != null && definition.enabled()
                && io.github.r3neer.scalebrews.config.ScaleRules.get(entity.level()).mountEnabled(definition.entity()) ? definition : null;
    }

    static TinyMountDefinition configuredDefinition(Entity entity) {
        if (!(entity instanceof Mob) || !GENERIC_MOB.get(entity.getClass())) return null;
        var registry = entity.registryAccess().lookup(REGISTRY);
        if (registry.isEmpty()) return null;
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return registry.get().listElements().map(h -> h.value())
                .filter(d -> d.entity().equals(id)).findFirst().orElse(null);
    }

    public static boolean eligible(Player player, Entity mount) {
        return !player.isSpectator() && MountSizePolicy.permits(player, mount)
            && (!(mount instanceof net.minecraft.world.entity.animal.wolf.Wolf wolf) || WolfMount.permits(wolf, player));
    }

    public static Player rider(Entity entity) {
        return entity.getFirstPassenger() instanceof Player p ? p : null;
    }

    public static Player controller(Mob mob) {
        var definition = definition(mob);
        var player = rider(mob);
        if (definition == null || player == null || !eligible(player, mob) || !mob.isAlive()
                || (definition.saddle() && !mob.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE))) return null;
        if (definition.control() == TinyMountDefinition.Control.ITEM_STEERED) {
            if (!TinyMountTemptation.matches(definition, player.getMainHandItem())
                    && !TinyMountTemptation.matches(definition, player.getOffhandItem())) return null;
        }
        return player;
    }

    public static Input input(Player player) {
        return player instanceof ServerPlayer p ? p.getLastClientInput() : clientInput.apply(player);
    }

    public static InteractionResult interact(Mob mob, Player player, InteractionHand hand) {
        var definition = definition(mob);
        if (definition == null || !mob.isAlive()) return InteractionResult.PASS;
        var held = player.getItemInHand(hand);
        boolean saddling = held.is(Items.SADDLE);
        boolean mounting = held.isEmpty() || definition.steeringItem()
                .map(id -> held.is(BuiltInRegistries.ITEM.getValue(id))).orElse(false);
        if (!saddling && !mounting) return InteractionResult.PASS; // Preserve feeding, breeding, naming, leads, etc.
        if (!eligible(player, mob)) {
            if (!mob.level().isClientSide()) {
                if (player instanceof ServerPlayer serverPlayer)
                    serverPlayer.sendSystemMessage(Component.translatable("message.scalebrews.too_large_to_ride"), true);
                mob.level().playSound(null, mob.blockPosition(), SoundEvents.VILLAGER_NO, net.minecraft.sounds.SoundSource.PLAYERS, .25F, 1.2F);
            }
            return InteractionResult.FAIL;
        }
        if (mob.isBaby() || player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (saddling) {
            if (!definition.saddle() || !mob.getItemBySlot(EquipmentSlot.SADDLE).isEmpty()) return InteractionResult.PASS;
            if (!mob.level().isClientSide()) {
                mob.setItemSlot(EquipmentSlot.SADDLE, held.copyWithCount(1));
                mob.setGuaranteedDrop(EquipmentSlot.SADDLE);
                mob.setPersistenceRequired();
                held.consume(1, player);
                mob.playSound(SoundEvents.PIG_SADDLE.value(), .5F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        if (mob.isVehicle() || (definition.saddle() && !mob.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE))) return InteractionResult.PASS;
        if (!mob.level().isClientSide()) {
            if (!player.startRiding(mob)) return InteractionResult.FAIL;
            if (mob instanceof net.minecraft.world.entity.animal.wolf.Wolf wolf) {
                wolf.setOrderedToSit(false); wolf.setInSittingPose(false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean mayMount(Player player, Entity vehicle) {
        if (!MountSizePolicy.permits(player, vehicle)) return false;
        var definition = definition(vehicle);
        return definition == null || (eligible(player, vehicle) && !vehicle.isVehicle()
                && (!(vehicle instanceof Mob mob) || !mob.isBaby())
                && (!definition.saddle() || ((LivingEntity) vehicle).getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE)));
    }

    public static void enforceRider(LivingEntity player) {
        if (player.level().isClientSide() || player.getVehicle() == null) return;
        var vehicle = player.getVehicle();
        var definition = definition(vehicle);
        if (!MountSizePolicy.permits(player, vehicle)
                || (player instanceof Player p && definition != null && (!eligible(p, vehicle)
                || (definition.saddle() && !((LivingEntity) vehicle).getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE))))) {
            player.stopRiding(); // Native LivingEntity dismount placement, not raw passenger removal.
            player.resetFallDistance();
        }
    }

    public static Vec3 groundInput(Player player) {
        Input input = input(player);
        float sideways = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
        float forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
        return new Vec3(sideways * .5, 0, forward < 0 ? forward * .5 : forward);
    }

    public static Vec3 flightVelocity(Player player, TinyMountDefinition definition) {
        double pitch = Math.toRadians(Math.clamp(player.getXRot(), -definition.maxPitch(), definition.maxPitch()));
        double yaw = Math.toRadians(player.getYRot());
        double speed = definition.speed();
        return new Vec3(-Math.sin(yaw) * Math.cos(pitch) * speed,
                Math.clamp(-Math.sin(pitch) * speed, -definition.maxVerticalSpeed(), definition.maxVerticalSpeed()),
                Math.cos(yaw) * Math.cos(pitch) * speed);
    }
}
