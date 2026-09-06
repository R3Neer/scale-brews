package io.github.r3neer.scalebrews.mount;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;
import java.util.Map;
import java.util.WeakHashMap;

/** Server-only, transient alternative to bones. No changes to vanilla ownership/equipment storage. */
public final class WolfTaming {
    private static final Map<Wolf, Progress> PROGRESS = new WeakHashMap<>();
    private static final class Progress { UUID player; int ticks, attempts; }
    private WolfTaming() {}
    public static void tick(Wolf wolf) {
        if (wolf.level().isClientSide() || !WolfMount.enabled(wolf)) return;
        if (wolf.isTame() || !wolf.isAlive()) { PROGRESS.remove(wolf); return; }
        var rider = TinyMounts.rider(wolf);
        if (rider == null) return;
        var progress = PROGRESS.computeIfAbsent(wolf, ignored -> new Progress());
        if (!rider.getUUID().equals(progress.player)) {
            progress.player = rider.getUUID(); progress.attempts = 0; progress.ticks = 0;
        }
        // The wolf keeps its own movement. An angry remount must not attack its passenger.
        if (wolf.getTarget() == rider) wolf.setTarget(null);
        if (++progress.ticks < 60) return;
        progress.ticks = 0;
        if (++progress.attempts >= 3) {
            wolf.tame(rider);
            wolf.stopBeingAngry();
            wolf.setTarget(null);
            wolf.setLastHurtByMob(null);
            wolf.getNavigation().stop();
            wolf.level().broadcastEntityEvent(wolf, (byte)7);
            PROGRESS.remove(wolf);
        } else {
            wolf.level().broadcastEntityEvent(wolf, (byte)6);
            rider.stopRiding();
        }
    }
    public static void dismounted(Wolf wolf, Entity passenger) {
        if (wolf.level().isClientSide() || wolf.isTame() || !wolf.isAlive() || !WolfMount.enabled(wolf)
                || !(passenger instanceof Player player) || !player.isAlive() || player.level() != wolf.level()) return;
        var progress = PROGRESS.get(wolf);
        if (progress != null) progress.ticks = 0;
        wolf.setLastHurtByMob(player);
        wolf.setTarget(player);
        wolf.startPersistentAngerTimer();
        wolf.setPersistentAngerTarget(net.minecraft.world.entity.EntityReference.of(player));
    }
}
