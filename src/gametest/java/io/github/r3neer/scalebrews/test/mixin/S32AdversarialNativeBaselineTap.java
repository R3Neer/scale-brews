package io.github.r3neer.scalebrews.test.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.r3neer.scalebrews.test.S32AdversarialNativeBaselineProof;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import org.spongepowered.asm.mixin.Mixin;
/** Read-only wire observation after the production native bundle handler. */
@Mixin(ClientPacketListener.class)
public abstract class S32AdversarialNativeBaselineTap {
    @WrapMethod(method="handleBundlePacket")
    private void test$wire(ClientboundBundlePacket p,Operation<Void> original){
        original.call(p);S32AdversarialNativeBaselineProof.afterBundle(p);
    }
}
