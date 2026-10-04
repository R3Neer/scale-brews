package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Test-only observation at the real native jump method, before tick damping. */
public final class S40NativeJumpAudit {
    private S40NativeJumpAudit() {}
    private static volatile boolean enabled;
    private static final ConcurrentHashMap<UUID,Boolean> STARTS=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,Jump> JUMPS=new ConcurrentHashMap<>();
    public record Jump(boolean hadMaterialAtEntry,Vec3 impulse) {}
    public static void reset() {STARTS.clear();JUMPS.clear();enabled=true;}
    public static void stop() {enabled=false;STARTS.clear();JUMPS.clear();}
    public static Jump get(UUID id) {return JUMPS.get(id);}
    public static void start(LivingEntity body) {
        if(enabled && body instanceof Player && body.level().isClientSide())
            STARTS.put(body.getUUID(),AnatomyMovement.contact(body)!=null);
    }
    public static void end(LivingEntity body) {
        if(enabled && body instanceof Player && body.level().isClientSide())
            JUMPS.put(body.getUUID(),new Jump(Boolean.TRUE.equals(STARTS.remove(body.getUUID())),body.getDeltaMovement()));
    }
}
