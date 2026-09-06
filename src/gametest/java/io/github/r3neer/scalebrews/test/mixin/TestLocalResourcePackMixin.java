package io.github.r3neer.scalebrews.test.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Accept only loopback test-server packs, using the same download/validation path as Yes. */
@Mixin(ClientCommonPacketListenerImpl.class)
public class TestLocalResourcePackMixin {
    @Inject(method = "handleResourcePackPush", at = @At("HEAD"), cancellable = true)
    private void scalebrews$localPack(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        var client = Minecraft.getInstance();
        if (!client.isSameThread()) return; // Vanilla reschedules the packet onto the client thread.
        try {
            var uri = java.net.URI.create(packet.url());
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) return;
            if (!("127.0.0.1".equals(uri.getHost()) || "localhost".equals(uri.getHost())
                    || "[::1]".equals(uri.getHost()))) return;
            client.getDownloadedPackSource().allowServerPacks();
            client.getDownloadedPackSource().pushPack(packet.id(), uri.toURL(), packet.hash());
            ci.cancel();
        } catch (java.net.MalformedURLException | IllegalArgumentException ignored) {
            // Leave invalid or non-local requests to vanilla validation/confirmation.
        }
    }
}
