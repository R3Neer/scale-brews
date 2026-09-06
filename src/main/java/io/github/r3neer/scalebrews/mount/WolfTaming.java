package io.github.r3neer.scalebrews.mount;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;
import java.util.Map;
import java.util.WeakHashMap;

/** Horse-style persistent trust; only the current ride timer is transient. */
public final class WolfTaming {
    public static final int MAX_TRUST = 100;
    public interface Trust {
        int scalebrews$getTrust();
        void scalebrews$setTrust(int trust);
    }
    private static final Map<Wolf, Progress> PROGRESS = new WeakHashMap<>();
    private static final class Progress { UUID player; int ticks; }
    private WolfTaming() {}
    public static int trust(Wolf wolf) { return ((Trust)wolf).scalebrews$getTrust(); }
    public static void setTrust(Wolf wolf, int trust) { ((Trust)wolf).scalebrews$setTrust(Math.clamp(trust, 0, MAX_TRUST)); }
    /** Called only after vanilla has accepted a bone and completed its normal taming roll. */
    public static void boneAccepted(Wolf wolf) {
        if (wolf.level().isClientSide() || !WolfMount.enabled(wolf)) return;
        setTrust(wolf, wolf.isTame() ? 0 : trust(wolf) + 10);
    }
    public static void tick(Wolf wolf) {
        if (wolf.level().isClientSide() || !WolfMount.enabled(wolf)) return;
        if (wolf.isTame() || !wolf.isAlive()) { PROGRESS.remove(wolf); return; }
        var rider = TinyMounts.rider(wolf);
        if (rider == null) return;
        var progress = PROGRESS.computeIfAbsent(wolf, ignored -> new Progress());
        if (!rider.getUUID().equals(progress.player)) {
            progress.player = rider.getUUID(); progress.ticks = 0;
        }
        // The wolf keeps its own movement. An angry remount must not attack its passenger.
        if (wolf.getTarget() == rider) wolf.setTarget(null);
        if (++progress.ticks < 60) return;
        progress.ticks = 0;
        // As with horses, roll before raising trust; an unfed first ride always fails.
        if (wolf.getRandom().nextInt(MAX_TRUST) < trust(wolf)) {
            wolf.tame(rider);
            wolf.stopBeingAngry();
            wolf.setTarget(null);
            wolf.setLastHurtByMob(null);
            wolf.getNavigation().stop();
            wolf.level().broadcastEntityEvent(wolf, (byte)7);
            setTrust(wolf, 0);
            PROGRESS.remove(wolf);
        } else {
            setTrust(wolf, trust(wolf) + 5);
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
