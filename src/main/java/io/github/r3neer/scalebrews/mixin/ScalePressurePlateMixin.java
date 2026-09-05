package io.github.r3neer.scalebrews.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.r3neer.scalebrews.physics.ScalePhysics;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BasePressurePlateBlock.class)
public abstract class ScalePressurePlateMixin {
    @ModifyExpressionValue(method = "getEntityCount", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private static List<? extends Entity> scalebrews$filterWeight(List<? extends Entity> original,
            Level level, AABB box, Class<? extends Entity> entityClass) {
        var plate = level.getBlockState(BlockPos.containing(box.getCenter()));
        return original.stream().filter(entity -> ScalePhysics.triggersPlate(entity, plate)).toList();
    }
}
