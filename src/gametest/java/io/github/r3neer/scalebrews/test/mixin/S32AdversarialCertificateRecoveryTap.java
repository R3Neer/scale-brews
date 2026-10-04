package io.github.r3neer.scalebrews.test.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.r3neer.scalebrews.test.S32AdversarialCertificateRecoveryProof;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import org.spongepowered.asm.mixin.Mixin;
/** Test fault at the receiver boundary; forwards the genuine native position once. */
@Mixin(ClientPacketListener.class)
public abstract class S32AdversarialCertificateRecoveryTap {
    @WrapMethod(method="handleBundlePacket")
    private void test$wire(ClientboundBundlePacket p,Operation<Void> original){
        var actual=S32AdversarialCertificateRecoveryProof.incoming(p);
        original.call(actual);S32AdversarialCertificateRecoveryProof.afterBundle(actual);
    }
}
