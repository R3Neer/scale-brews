package io.github.r3neer.scalebrews.test.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(LivingEntity.class)
public interface TestMountAccess {
    @Invoker("getRiddenInput") Vec3 test$input(Player p, Vec3 input);
    @Invoker("jumpFromGround") void test$jump();
    @Invoker("getJumpPower") float test$jumpPower();
}
