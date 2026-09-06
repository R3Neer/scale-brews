package io.github.r3neer.scalebrews.client.mixin;

import io.github.r3neer.scalebrews.platform.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class PlatformOutgoingMoveMixin {
    @Inject(method="send(Lnet/minecraft/network/protocol/Packet;)V",at=@At("HEAD"))
    private void scalebrews$reference(Packet<?> packet,CallbackInfo ci) {
        Minecraft client=Minecraft.getInstance();
        if(client.player==null || !client.isSameThread()) return;
        Entity body; Vec3 target;
        if(packet instanceof ServerboundMovePlayerPacket p && p.hasPosition()) {
            body=client.player; target=new Vec3(p.getX(body.getX()),p.getY(body.getY()),p.getZ(body.getZ()));
        } else if(packet instanceof ServerboundMoveVehiclePacket p) {
            body=client.player.getRootVehicle(); target=p.position();
        } else return;
        if(!Platforms.supported(body) || !ClientPlayNetworking.canSend(PlatformMovePayload.TYPE)) return;
        var state=Platforms.state(body);
        ClientPlayNetworking.send(new PlatformMovePayload(body.getId(),state.support.getId(),target,
            PlatformGeometry.frame(state.support).local(target)));
    }
}
