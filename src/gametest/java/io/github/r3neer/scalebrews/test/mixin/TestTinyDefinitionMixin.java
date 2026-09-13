package io.github.r3neer.scalebrews.test.mixin;

import io.github.r3neer.scalebrews.mount.TinyMounts;
import io.github.r3neer.scalebrews.mount.TinyMountDefinition;
import io.github.r3neer.scalebrews.mount.TinyMountInventory;
import io.github.r3neer.scalebrews.test.TinyDefinitionTestScope;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TinyMounts.class)
public class TestTinyDefinitionMixin {
    @Inject(method = "configuredDefinition", at = @At("HEAD"), cancellable = true)
    private static void definition(Entity entity, CallbackInfoReturnable<TinyMountDefinition> cir) {
        var value = TinyDefinitionTestScope.CURRENT.get();
        if (value != null && value.entity().equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())))
            cir.setReturnValue(value);
    }

    @Inject(method = "interact", at = @At("HEAD"))
    private static void traceInteraction(Mob mob, Player player, InteractionHand hand,
                                         CallbackInfoReturnable<InteractionResult> cir) {
        if (mob.getType() != EntityTypes.CHICKEN || !player.isSecondaryUseActive()) return;
        var definition = TinyMounts.definition(mob);
        System.out.println("TINY_DIRECT_TRACE side=" + (mob.level().isClientSide() ? "client" : "server")
                + " family=" + (definition == null ? "null" : definition.family())
                + " secondary=" + player.isSecondaryUseActive()
                + " isVehicle=" + mob.isVehicle()
                + " playerVehicle=" + (player.getVehicle() == mob)
                + " canOpen=" + TinyMountInventory.canOpen(player, mob)
                + " distance2=" + player.distanceToSqr(mob));
    }
}
