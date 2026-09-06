package io.github.r3neer.scalebrews.mixin;
import io.github.r3neer.scalebrews.loot.ScaleLoot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(Player.class)
public abstract class ScaleHarvestPlayerMixin {
    @WrapMethod(method = "interactOn")
    private InteractionResult harvest(Entity entity, InteractionHand hand, Vec3 location, Operation<InteractionResult> original) {
        if (entity instanceof LivingEntity living && !entity.level().isClientSide())
            return ScaleLoot.harvesting(living, () -> original.call(entity, hand, location));
        return original.call(entity, hand, location);
    }
}
