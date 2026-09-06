package io.github.r3neer.scalebrews.test.mixin;
import net.fabricmc.fabric.impl.client.gametest.threading.ThreadingImpl;
import java.util.concurrent.Phaser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Register before publishing testThread: otherwise the render thread may wait before its peer registers. Test-only. */
@Mixin(value = ThreadingImpl.class, remap = false)
public abstract class TestClientStartupRaceMixin {
    @Inject(method = "runTestThread", at = @At(value = "NEW", target = "java/lang/Thread"))
    private static void registerBeforeStart(Runnable testRunner, CallbackInfo ci) { ThreadingImpl.PHASER.register(); }
    @Redirect(method = "lambda$runTestThread$0", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/Phaser;register()I"))
    private static int alreadyRegistered(Phaser phaser) { return phaser.getPhase(); }
}
