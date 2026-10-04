package io.github.r3neer.scalebrews.test.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.r3neer.scalebrews.test.S32AdversarialNativeBundleProof;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.Mixin;

/** Inert outside the selected test's armed wire lane. Never ships with production. */
@Mixin(ClientPacketListener.class)
public abstract class S32AdversarialNativeBundleMixin {
    @WrapMethod(method="handleBundlePacket")
    private void test$bundle(ClientboundBundlePacket p,Operation<Void> original) {
        boolean active=S32AdversarialNativeBundleProof.begin(p),completed=false;
        try {original.call(p);completed=true;}
        finally {if(active)S32AdversarialNativeBundleProof.finish(completed);}
    }
    @WrapMethod(method="handleMoveEntity")
    private void test$move(ClientboundMoveEntityPacket p,Operation<Void> original) {
        S32AdversarialNativeBundleProof.before(p);original.call(p);S32AdversarialNativeBundleProof.after(p);
    }
    @WrapMethod(method="handleEntityPositionSync")
    private void test$sync(ClientboundEntityPositionSyncPacket p,Operation<Void> original) {
        S32AdversarialNativeBundleProof.before(p);original.call(p);S32AdversarialNativeBundleProof.after(p);
    }
}
