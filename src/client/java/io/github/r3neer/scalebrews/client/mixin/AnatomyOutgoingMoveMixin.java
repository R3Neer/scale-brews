package io.github.r3neer.scalebrews.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class AnatomyOutgoingMoveMixin {
    @Inject(method="send(Lnet/minecraft/network/protocol/Packet;)V",at=@At("HEAD"))
    private void scalebrews$reference(Packet<?> packet,CallbackInfo ci) {
        Minecraft client=Minecraft.getInstance();
        if(client.player==null || !client.isSameThread()) return;
        Entity body;
        if(packet instanceof ServerboundMovePlayerPacket p && p.hasPosition()) {
            body=client.player;
        } else if(packet instanceof ServerboundMoveVehiclePacket p) {
            body=client.player.getRootVehicle();
        } else return;
        // Anatomy owns its own metadata-only receipt cursor. BINDING remains
        // fail-closed and never falls through to the legacy coordinate reference.
        if(io.github.r3neer.scalebrews.collision.api.AnatomyApi.ownsSharedPhysics(body))
            io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking.sendMovementReference(body);
    }
}
