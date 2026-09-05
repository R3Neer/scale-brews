package io.github.r3neer.scalebrews.test.mixin;
import net.minecraft.world.entity.ai.sensing.VillagerHostilesSensor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(VillagerHostilesSensor.class)
public interface TestVillagerSensorAccess {
    @Invoker("isMatchingEntity") boolean test$matches(ServerLevel level, LivingEntity body, LivingEntity candidate);
}
