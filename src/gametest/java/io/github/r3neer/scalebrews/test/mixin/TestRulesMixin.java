package io.github.r3neer.scalebrews.test.mixin;
import io.github.r3neer.scalebrews.config.ScaleRules;
import io.github.r3neer.scalebrews.test.RuleTestScope;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ScaleRules.class)
public class TestRulesMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void test$rules(Level level, CallbackInfoReturnable<ScaleRules> cir) {
        var override = RuleTestScope.CURRENT.get();
        if (override != null) cir.setReturnValue(override);
    }
}
