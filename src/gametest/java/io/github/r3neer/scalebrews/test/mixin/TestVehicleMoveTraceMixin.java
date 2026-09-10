package io.github.r3neer.scalebrews.test.mixin;

import io.github.r3neer.scalebrews.test.VehicleMoveAudit;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures raw vehicle input around vanilla acceptance; never ships with the mod. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class TestVehicleMoveTraceMixin {
    /*
     * Do not observe at HEAD: vanilla schedules an off-thread packet before
     * its body runs. This injection resumes only after PacketUtils has put the
     * callback on the authoritative server thread, immediately before the
     * movement acceptance/baseline checks.
     */
    @Inject(method="handleMoveVehicle",at=@At(value="INVOKE",
            target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift=At.Shift.AFTER))
    private void test$beforeVehicleMove(ServerboundMoveVehiclePacket packet,CallbackInfo ci) {
        VehicleMoveAudit.before((ServerGamePacketListenerImpl)(Object)this,packet);
    }

    @Inject(method="handleMoveVehicle",at=@At("RETURN"))
    private void test$afterVehicleMove(ServerboundMoveVehiclePacket packet,CallbackInfo ci) {
        VehicleMoveAudit.after((ServerGamePacketListenerImpl)(Object)this,packet);
    }
}
