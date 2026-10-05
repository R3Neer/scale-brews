package io.github.r3neer.scalebrews.mount;

import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.integration.gravity.GravityFrames;
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
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import java.util.function.Function;

public final class TinyMounts {
    public static final ResourceKey<Registry<TinyMountDefinition>> REGISTRY = ResourceKey.createRegistryKey(ScaleBrews.id("tiny_mount"));
    public static Function<Player, Input> clientInput = player -> Input.EMPTY;
    private static final ClassValue<Boolean> GENERIC_MOB = new ClassValue<>() {
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("getControllingPassenger").getDeclaringClass() == Mob.class;
            } catch (NoSuchMethodException e) { return false; }
        }
    };
    private TinyMounts() {}

    public static void initialize() {
        MountSizePolicy.initialize();
        TinyMountInventory.initialize();
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

    public static boolean familyTamed(Mob mob, TinyMountDefinition definition) {
        if (!definition.family().requiresTameForControl()) return true;
        return mob instanceof TamableAnimal tameable && tameable.isTame();
    }

    /** Saddle/body equipment follows the vanilla horse rule: adults only, and tameable families must be tamed first. */
    public static boolean equipmentAvailable(Mob mob, TinyMountDefinition definition) {
        return definition != null && mob.isAlive() && !mob.isBaby() && familyTamed(mob, definition);
    }

    public static boolean hasMountInventory(Mob mob) {
        var definition = definition(mob);
        return definition != null && definition.family().hasInventory() && equipmentAvailable(mob, definition);
    }

    public static boolean matchesBodyEquipment(TinyMountDefinition definition, ItemStack stack) {
        return definition != null && definition.bodyEquipment()
                .flatMap(body -> BuiltInRegistries.ITEM.getOptional(body.item()))
                .map(stack::is).orElse(false);
    }

    /** BODY equipment on tameable mounts is owner-managed; saddle borrowing remains intentionally independent. */
    public static boolean mayManageBodyEquipment(Mob mob, Player player, TinyMountDefinition definition) {
        if (definition == null || definition.bodyEquipment().isEmpty()) return false;
        if (definition.family() != TinyMountDefinition.Family.TAMEABLE_DIRECT) return true;
        return mob instanceof TamableAnimal tameable && tameable.isOwnedBy(player);
    }

    public static boolean eligible(Player player, Entity mount) {
        if (player.isSpectator() || !MountSizePolicy.permits(player, mount)) return false;
        var definition = definition(mount);
        if (definition != null && definition.family() == TinyMountDefinition.Family.TAMEABLE_DIRECT
                && !(mount instanceof TamableAnimal)) return false;
        return !(mount instanceof Wolf wolf) || !wolf.isTame() || WolfMount.permits(wolf, player);
    }

    public static Player rider(Entity entity) { return entity.getFirstPassenger() instanceof Player p ? p : null; }

    public static Player controller(Mob mob) {
        var definition = definition(mob);
        var player = rider(mob);
        if (definition == null || player == null || !eligible(player, mob) || !mob.isAlive()
                || !mob.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE)) return null;
        if (definition.family().requiresTameForControl() && !familyTamed(mob, definition)) return null;
        if (definition.family().usesSteeringItem()
                && !TinyMountTemptation.matches(definition, player.getMainHandItem())
                && !TinyMountTemptation.matches(definition, player.getOffhandItem())) return null;
        return player;
    }

    public static Input input(Player player) {
        return player instanceof ServerPlayer p ? p.getLastClientInput() : clientInput.apply(player);
    }

    public static InteractionResult interact(Mob mob, Player player, InteractionHand hand) {
        var definition = definition(mob);
        if (definition == null || !mob.isAlive() || player.isSpectator()) return InteractionResult.PASS;
        var held = player.getItemInHand(hand);
        var family = definition.family();
        boolean holdingSaddle = held.is(Items.SADDLE);
        boolean alreadySaddled = mob.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE);

        boolean tamedWolf = mob instanceof Wolf wolf && wolf.isTame();
        // Tamed wolves use the ordinary mount inventory gesture. Wild ride-taming
        // and other configured tameables keep their established gesture.
        if ((family == TinyMountDefinition.Family.DIRECT || tamedWolf) && player.isSecondaryUseActive()
                && TinyMountInventory.canOpen(player, mob)) {
            if (!mob.level().isClientSide() && player instanceof ServerPlayer serverPlayer)
                TinyMountInventory.open(serverPlayer, mob);
            return InteractionResult.SUCCESS;
        }

        boolean tameableMountGesture = family == TinyMountDefinition.Family.TAMEABLE_DIRECT
                && !tamedWolf && player.isSecondaryUseActive();

        // The retained wild/other-tameable gesture must not consume held equipment.
        if (holdingSaddle && !alreadySaddled && !tameableMountGesture) {
            if (mob.isBaby()) return reject(mob, player, "mount_too_young");
            if (!equipmentAvailable(mob, definition)) {
                if (mob instanceof Wolf) return reject(mob, player, "wolf_not_tamed");
                return InteractionResult.PASS;
            }
            if (!mob.level().isClientSide()) {
                mob.setItemSlot(EquipmentSlot.SADDLE, held.copyWithCount(1));
                mob.setGuaranteedDrop(EquipmentSlot.SADDLE);
                mob.setPersistenceRequired();
                held.consume(1, player);
                var sound = family == TinyMountDefinition.Family.ITEM_STEERED
                        ? SoundEvents.PIG_SADDLE.value() : SoundEvents.HORSE_SADDLE.value();
                mob.playSound(sound, .5F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        // Optional BODY equipment is declared by data, but still respects the item's native entity/slot rules.
        if (!tameableMountGesture && matchesBodyEquipment(definition, held)
                && mob.getItemBySlot(EquipmentSlot.BODY).isEmpty()) {
            if (!equipmentAvailable(mob, definition) || !mayManageBodyEquipment(mob, player, definition)
                    || !mob.isEquippableInSlot(held, EquipmentSlot.BODY)) return InteractionResult.PASS;
            if (!mob.level().isClientSide()) {
                mob.setItemSlot(EquipmentSlot.BODY, held.copyWithCount(1));
                mob.setGuaranteedDrop(EquipmentSlot.BODY);
                mob.setPersistenceRequired();
                held.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }

        boolean mounting = switch (family) {
            case DIRECT -> !player.isSecondaryUseActive()
                    && (held.isEmpty() || (holdingSaddle && alreadySaddled));
            case TAMEABLE_DIRECT -> tamedWolf
                    ? !player.isSecondaryUseActive() && alreadySaddled && (held.isEmpty() || holdingSaddle)
                    : tameableMountGesture;
            case ITEM_STEERED -> !player.isSecondaryUseActive() && alreadySaddled
                    && (held.isEmpty() || holdingSaddle || TinyMountTemptation.matches(definition, held));
        };
        if (!mounting) return InteractionResult.PASS;
        if (mob.isBaby()) return reject(mob, player, "mount_too_young");
        if (!MountSizePolicy.permits(player, mob)) return reject(mob, player, "too_large_to_ride");
        if (!eligible(player, mob)) return reject(mob, player, "mount_hostile");
        if (mob.isVehicle()) return reject(mob, player, "mount_occupied");
        if (!mob.level().isClientSide()) {
            boolean shift = player.isShiftKeyDown();
            boolean mounted;
            try { player.setShiftKeyDown(false); mounted = player.startRiding(mob); }
            finally { player.setShiftKeyDown(shift); }
            if (!mounted) return InteractionResult.FAIL;
            if (mob instanceof Wolf wolf) {
                wolf.setOrderedToSit(false); wolf.setInSittingPose(false);
                if (!wolf.isTame()) { wolf.stopBeingAngry(); wolf.setTarget(null); wolf.setLastHurtByMob(null); }
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean mayMount(Player player, Entity vehicle) {
        if (!MountSizePolicy.permits(player, vehicle)) return false;
        var definition = definition(vehicle);
        if (definition == null) return true;
        if (!eligible(player, vehicle) || vehicle.isVehicle() || !(vehicle instanceof Mob mob) || mob.isBaby()) return false;
        return !definition.family().requiresSaddleToMount()
                || mob.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE);
    }

    public static void enforceRider(LivingEntity player) {
        if (player.level().isClientSide() || player.getVehicle() == null) return;
        var vehicle = player.getVehicle();
        var definition = definition(vehicle);
        boolean missingRequiredSaddle = definition != null
                && definition.family().requiresSaddleToMount()
                && vehicle instanceof Mob mob
                && !mob.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE);
        if (!MountSizePolicy.permits(player, vehicle) || missingRequiredSaddle
                || (player instanceof Player p && definition != null && !eligible(p, vehicle))) {
            player.stopRiding();
            player.resetFallDistance();
        }
    }

    public static Vec3 groundInput(Player player) {
        Input input = input(player);
        float sideways = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
        float forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
        return new Vec3(sideways * .5, 0, forward < 0 ? forward * .5 : forward);
    }

    private static InteractionResult reject(Mob mob, Player player, String reason) {
        if (!mob.level().isClientSide() && player instanceof ServerPlayer serverPlayer)
            serverPlayer.sendSystemMessage(Component.translatable("message.scalebrews." + reason), true);
        return InteractionResult.FAIL;
    }

    public static Vec3 flightVelocity(Player player, TinyMountDefinition definition) {
        double pitch = Math.toRadians(Math.clamp(player.getXRot(), -definition.maxPitch(), definition.maxPitch()));
        double yaw = Math.toRadians(player.getYRot());
        double speed = definition.speed();
        Vec3 local = new Vec3(-Math.sin(yaw) * Math.cos(pitch) * speed,
                Math.clamp(-Math.sin(pitch) * speed, -definition.maxVerticalSpeed(), definition.maxVerticalSpeed()),
                Math.cos(yaw) * Math.cos(pitch) * speed);
        return GravityFrames.frame(player.getRootVehicle()).toWorld(local);
    }
}
