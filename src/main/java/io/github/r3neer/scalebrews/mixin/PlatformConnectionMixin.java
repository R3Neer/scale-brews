package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.platform.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlatformConnectionMixin implements PlatformConnection {
    @Shadow public ServerPlayer player;
    @Shadow private Entity lastVehicle;
    @Shadow private double firstGoodX,firstGoodY,firstGoodZ,lastGoodX,lastGoodY,lastGoodZ;
    @Shadow private double vehicleFirstGoodX,vehicleFirstGoodY,vehicleFirstGoodZ,vehicleLastGoodX,vehicleLastGoodY,vehicleLastGoodZ;
    @WrapMethod(method="handleMovePlayer")
    private void scalebrews$beforePlayerPacket(ServerboundMovePlayerPacket packet,Operation<Void> original) {
        if(player.level().getServer().isSameThread()) {
            PlatformPhysics.carry(player);
            if(packet.hasPosition()) {
                Vec3 raw=new Vec3(packet.getX(player.getX()),packet.getY(player.getY()),packet.getZ(player.getZ()));
                Vec3 position=PlatformMovementReference.resolve(player,raw);
                if(!position.equals(raw)) packet=new ServerboundMovePlayerPacket.PosRot(position,
                    packet.getYRot(player.getYRot()),packet.getXRot(player.getXRot()),packet.isOnGround(),packet.horizontalCollision());
            }
        }
        original.call(packet);
    }
    @WrapMethod(method="handleMoveVehicle")
    private void scalebrews$beforeVehiclePacket(ServerboundMoveVehiclePacket packet,Operation<Void> original) {
        if(player.level().getServer().isSameThread()) {
            Entity body=player.getRootVehicle();
            PlatformPhysics.carry(body);
            Vec3 target=PlatformMovementReference.resolve(body,packet.position());
            if(!target.equals(packet.position())) packet=new ServerboundMoveVehiclePacket(target,packet.yRot(),packet.xRot(),packet.onGround());
        }
        original.call(packet);
    }
    public void scalebrews$transportBaseline(Entity body,Vec3 d) {
        if(body==player) {
            firstGoodX+=d.x; firstGoodY+=d.y; firstGoodZ+=d.z;
            lastGoodX+=d.x; lastGoodY+=d.y; lastGoodZ+=d.z;
        }
        if(body==lastVehicle) {
            vehicleFirstGoodX+=d.x; vehicleFirstGoodY+=d.y; vehicleFirstGoodZ+=d.z;
            vehicleLastGoodX+=d.x; vehicleLastGoodY+=d.y; vehicleLastGoodZ+=d.z;
        }
    }
    @Inject(method="noBlocksAround",at=@At("HEAD"),cancellable=true)
    private void scalebrews$realSupport(Entity e,CallbackInfoReturnable<Boolean> cir) {
        if(Platforms.supported(e) && PlatformPhysics.touching(e)) cir.setReturnValue(false);
    }
}
