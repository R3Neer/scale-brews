package io.github.r3neer.scalebrews.test.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.r3neer.scalebrews.test.S32AdversarialPhysicalCarryProof;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import org.spongepowered.asm.mixin.Mixin;
/** Read-only observation after the actual vanilla bundle handler; selected test lane only. */
@Mixin(ClientPacketListener.class)
public abstract class S32AdversarialPhysicalBundleMixin {
    @WrapMethod(method="handleBundlePacket")
    private void test$physicalBundle(ClientboundBundlePacket p,Operation<Void> original){original.call(p);S32AdversarialPhysicalCarryProof.decodedBundle(p);}
}
