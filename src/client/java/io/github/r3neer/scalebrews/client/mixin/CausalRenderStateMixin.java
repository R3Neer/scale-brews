package io.github.r3neer.scalebrews.client.mixin;

import io.github.r3neer.scalebrews.client.collision.presentation.CausalModelRender;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalPresentationState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class CausalRenderStateMixin implements CausalPresentationState {
    @Unique private CausalModelRender.Token scalebrews$causalToken;
    @Unique private boolean scalebrews$causalSubmitted;
    @Override public CausalModelRender.Token scalebrews$causalToken(){return scalebrews$causalToken;}
    @Override public void scalebrews$causalToken(CausalModelRender.Token token){scalebrews$causalToken=token;}
    @Override public boolean scalebrews$causalSubmitted(){return scalebrews$causalSubmitted;}
    @Override public void scalebrews$causalSubmitted(boolean submitted){scalebrews$causalSubmitted=submitted;}
}
