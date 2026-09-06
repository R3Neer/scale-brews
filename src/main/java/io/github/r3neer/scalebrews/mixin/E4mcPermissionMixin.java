package io.github.r3neer.scalebrews.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permissions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** e4mc 6.2.1's dedicated branch still invokes the removed pre-26.1 permission API. */
@Pseudo
@Mixin(targets="link.e4mc.E4mcClient",remap=false)
public abstract class E4mcPermissionMixin {
    @Inject(method="lambda$registerCommands$0",at=@At("HEAD"),cancellable=true,require=0)
    private static void scalebrews$dedicatedPermission(CommandSourceStack source, CallbackInfoReturnable<Boolean> cir) {
        if(source.getServer()!=null && source.getServer().isDedicatedServer())
            cir.setReturnValue(source.permissions().hasPermission(Permissions.COMMANDS_OWNER));
    }
}
