package io.github.r3neer.scalebrews.mount;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Transient commanded actions; equipment, ownership and autonomous attacks remain vanilla. */
public final class WolfMount {
    private static final Map<Wolf, State> STATES = new WeakHashMap<>();
    private static final ThreadLocal<Command> COMMAND = new ThreadLocal<>();
    private record Command(Wolf wolf, Player rider) {}
    private static final class State {
        Player rider;
        int held, cooldown, flight;
        boolean previousJump, hit;
        Vec3 previous;
    }
    private WolfMount() {}
    public static boolean enabled(Wolf wolf) {
        var d = TinyMounts.definition(wolf);
        return d != null && d.ability() == TinyMountDefinition.Ability.WOLF_POUNCE;
    }
    public static boolean permits(Wolf wolf, Player player) {
        return wolf.isTame() && wolf.getTarget() != player;
    }
    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, base, taken, blocked) -> {
            if (taken <= 0) return;
            if (source.getEntity() instanceof Player rider && rider.getVehicle() instanceof Wolf wolf)
                betray(wolf, rider, victim);
            var command = COMMAND.get();
            if (command != null && source.getEntity() == command.wolf)
                betray(command.wolf, command.rider, victim);
        });
    }
    private static void betray(Wolf wolf, Player rider, LivingEntity victim) {
        if (!wolf.isOwnedBy(victim) || wolf.isOwnedBy(rider) || rider.getVehicle() != wolf) return;
        rider.stopRiding();
        rider.resetFallDistance();
        STATES.remove(wolf);
        wolf.setOrderedToSit(false);
        wolf.setInSittingPose(false);
        wolf.getNavigation().stop();
        wolf.setLastHurtByMob(rider);
        wolf.setTarget(rider);
    }
    public static void tick(Wolf wolf) {
        if (wolf.level().isClientSide()) return;
        WolfTaming.tick(wolf);
        Player rider = TinyMounts.controller(wolf);
        if (rider == null || !enabled(wolf)) { STATES.remove(wolf); return; }
        State state = STATES.computeIfAbsent(wolf, ignored -> new State());
        if (state.rider != rider) { state = new State(); state.rider = rider; STATES.put(wolf, state); }
        if (state.cooldown > 0) state.cooldown--;
        if (state.flight > 0) {
            if (!state.hit && state.previous != null) {
                LivingEntity target = target(wolf, rider, state.previous, wolf.position());
                if (target != null) { state.hit = true; attack(wolf, rider, target);
                    if (TinyMounts.controller(wolf) != rider) return; }
            }
            state.previous = wolf.position();
            if (--state.flight == 0 || wolf.onGround()) {
                state.flight = 0;
                if (wolf.onGround()) wolf.setDeltaMovement(0, wolf.getDeltaMovement().y, 0);
            }
        }
        boolean jump = TinyMounts.input(rider).jump();
        if (jump && wolf.onGround() && state.cooldown == 0) state.held++;
        if (!jump && state.previousJump && state.held > 0 && state.cooldown == 0 && wolf.onGround()) {
            if (state.held <= 3) {
                var start = wolf.position();
                var end = start.add(rider.getLookAngle().scale(1.4 * wolf.getScale()));
                LivingEntity target = target(wolf, rider, start, end);
                if (target != null) attack(wolf, rider, target);
                state.cooldown = 20; // Vanilla melee goal attack interval.
            } else {
                double charge = Math.min(1, state.held * .1);
                Vec3 velocity = launchVelocity(rider.getYRot(), rider.getXRot(), Math.clamp(charge, 0, 1));
                wolf.setDeltaMovement(velocity);
                wolf.setOnGround(false);
                wolf.hurtMarked = true;
                state.flight = 30;
                state.previous = wolf.position();
                state.hit = false;
                state.cooldown = 20;
            }
        }
        if (!jump) state.held = 0;
        state.previousJump = jump;
    }
    public static Vec3 launchVelocity(float yawDegrees, float pitchDegrees, double charge) {
        double pitch = Math.toRadians(Math.clamp(pitchDegrees, -55, 55));
        double yaw = Math.toRadians(yawDegrees);
        double up = Math.clamp(.32 - Math.sin(pitch) * .65, -.16, .48) * (.55 + .45 * charge);
        // Match the vanilla airborne gravity/drag trajectory, then choose horizontal speed for the envelope.
        double y = 0, vertical = up, drag = 1, distancePerSpeed = 0;
        for (int tick = 0; tick < 40; tick++) {
            y += vertical;
            vertical = (vertical - .08) * .98;
            distancePerSpeed += drag;
            drag *= .91;
            if (y <= 0) break;
        }
        double horizontal = Math.min(.85, (1 + 4 * charge) * Math.cos(pitch) / distancePerSpeed);
        return new Vec3(-Math.sin(yaw) * horizontal, up, Math.cos(yaw) * horizontal);
    }
    private static LivingEntity target(Wolf wolf, Player rider, Vec3 from, Vec3 to) {
        double reach = .35 * wolf.getScale();
        var box = wolf.getBoundingBox().move(from.subtract(wolf.position())).expandTowards(to.subtract(from)).inflate(reach);
        Vec3 offset = new Vec3(0, wolf.getBbHeight() * .55, 0);
        Vec3 a = from.add(offset), b = to.add(offset);
        return wolf.level().getEntitiesOfClass(LivingEntity.class, box, candidate -> {
            if (candidate == wolf || candidate == rider || !wolf.canAttack(candidate)) return false;
            if (candidate instanceof Player player && !rider.canHarmPlayer(player)) return false;
            boolean owner = wolf.isOwnedBy(candidate) && !wolf.isOwnedBy(rider);
            if (!owner && (wolf.isAlliedTo(candidate) || !wolf.wantsToAttack(candidate, rider))) return false;
            if (!wolf.hasLineOfSight(candidate)) return false;
            var bounds = candidate.getBoundingBox().inflate(reach);
            return bounds.contains(a) || bounds.clip(a, b).isPresent();
        }).stream().min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(from))).orElse(null);
    }
    public static boolean attack(Wolf wolf, Player rider, LivingEntity target) {
        if (TinyMounts.controller(wolf) != rider || !(wolf.level() instanceof ServerLevel level)) return false;
        Command previous = COMMAND.get();
        COMMAND.set(new Command(wolf, rider));
        try { return wolf.doHurtTarget(level, target); }
        finally { if (previous == null) COMMAND.remove(); else COMMAND.set(previous); }
    }
}
