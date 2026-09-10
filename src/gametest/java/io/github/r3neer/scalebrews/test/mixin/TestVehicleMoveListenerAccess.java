package io.github.r3neer.scalebrews.test.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test-only view of vanilla's vehicle-movement acceptance baseline. */
@Mixin(ServerGamePacketListenerImpl.class)
public interface TestVehicleMoveListenerAccess {
    @Accessor("lastVehicle") Entity test$lastVehicle();
    @Accessor("vehicleFirstGoodX") double test$vehicleFirstGoodX();
    @Accessor("vehicleFirstGoodY") double test$vehicleFirstGoodY();
    @Accessor("vehicleFirstGoodZ") double test$vehicleFirstGoodZ();
    @Accessor("vehicleLastGoodX") double test$vehicleLastGoodX();
    @Accessor("vehicleLastGoodY") double test$vehicleLastGoodY();
    @Accessor("vehicleLastGoodZ") double test$vehicleLastGoodZ();
}
