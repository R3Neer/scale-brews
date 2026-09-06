package io.github.r3neer.scalebrews.mixin;

import net.minecraft.world.item.BoatItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BoatItem.class)
public interface PlatformBoatItemAccessor {
    @Accessor("entityType") EntityType<? extends AbstractBoat> scalebrews$type();
}
