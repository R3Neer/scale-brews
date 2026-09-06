package io.github.r3neer.scalebrews.mixin;
import io.github.r3neer.scalebrews.loot.ScaleLoot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(Entity.class)
public abstract class ScaleHarvestDropMixin {
    @WrapMethod(method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;")
    private ItemEntity harvest(ServerLevel level, ItemStack stack, Vec3 offset, Operation<ItemEntity> original) {
        if ((Object)this instanceof LivingEntity living && ScaleLoot.isHarvesting(living) && ScaleLoot.eligible(living, stack)) {
            ItemEntity[] first = {null};
            ScaleLoot.emit(living, stack, drop -> { var spawned = original.call(level, drop, offset); if (first[0] == null) first[0] = spawned; });
            return first[0];
        }
        return original.call(level, stack, offset);
    }
}
