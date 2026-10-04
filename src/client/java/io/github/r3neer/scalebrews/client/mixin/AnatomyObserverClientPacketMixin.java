package io.github.r3neer.scalebrews.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.r3neer.scalebrews.client.collision.network.AnatomyObserverBundleScope;
import io.github.r3neer.scalebrews.client.collision.network.AnatomyObserverBundleScope.NativeEvent;
import io.github.r3neer.scalebrews.client.collision.network.AnatomyObserverBundleScope.NativeState;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalRemoteResidual;
import io.github.r3neer.scalebrews.collision.internal.AnatomyObserverCarryPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

/** Observes vanilla handling inside the decoded bundle; never moves an entity itself. */
@Mixin(ClientPacketListener.class)
public abstract class AnatomyObserverClientPacketMixin {
    @WrapMethod(method="handleBundlePacket")
    private void scalebrews$bundle(ClientboundBundlePacket packet,Operation<Void> original) {
        var scope=AnatomyObserverBundleScope.open((ClientPacketListener)(Object)this,packet);
        boolean completed=false;
        try {original.call(packet);completed=true;}
        finally {AnatomyObserverBundleScope.close(scope,completed);}
    }

    @WrapMethod(method="handleMoveEntity")
    private void scalebrews$move(ClientboundMoveEntityPacket packet,Operation<Void> original) {
        var level=Minecraft.getInstance().level;
        Entity body=level==null?null:packet.getEntity(level);
        NativeState before=NativeState.capture(body);
        Vec3 target=body!=null && packet.hasPosition()
            ?body.getPositionCodec().decode(packet.getXa(),packet.getYa(),packet.getZa()):null;
        original.call(packet);
        if(body!=null)AnatomyObserverBundleScope.handled(new NativeEvent((ClientPacketListener)(Object)this,
            body,packet,packet.hasPosition()
                ?packet.hasRotation()?AnatomyObserverCarryPayload.Kind.RELATIVE_POS_ROT
                    :AnatomyObserverCarryPayload.Kind.RELATIVE_POS:null,
            target,before,NativeState.capture(body)));
    }

    @WrapMethod(method="handleEntityPositionSync")
    private void scalebrews$sync(ClientboundEntityPositionSyncPacket packet,Operation<Void> original) {
        var level=Minecraft.getInstance().level;
        Entity body=level==null?null:level.getEntity(packet.id());
        NativeState before=NativeState.capture(body);
        original.call(packet);
        if(body!=null)AnatomyObserverBundleScope.handled(new NativeEvent((ClientPacketListener)(Object)this,
            body,packet,AnatomyObserverCarryPayload.Kind.POSITION_SYNC,packet.values().position(),
            before,NativeState.capture(body)));
    }

    @WrapMethod(method="handleTeleportEntity")
    private void scalebrews$teleport(ClientboundTeleportEntityPacket packet,Operation<Void> original) {
        var level=Minecraft.getInstance().level;
        Entity body=level==null?null:level.getEntity(packet.id());
        original.call(packet);
        if(body!=null)CausalRemoteResidual.retire(body);
    }
}
