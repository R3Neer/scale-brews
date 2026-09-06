package io.github.r3neer.scalebrews.client.mixin;

import io.github.r3neer.scalebrews.client.render.SaddleState;
import io.github.r3neer.scalebrews.mount.TinyMountDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class TinySaddleStateMixin implements SaddleState {
    @Unique private TinyMountDefinition.SaddleVisual scalebrews$visual;
    @Unique private boolean scalebrews$occupied;
    public boolean scalebrews$occupied() { return scalebrews$occupied; }
    public void scalebrews$occupied(boolean occupied) { scalebrews$occupied = occupied; }
    public TinyMountDefinition.SaddleVisual scalebrews$saddle() { return scalebrews$visual; }
    public void scalebrews$saddle(TinyMountDefinition.SaddleVisual visual) { scalebrews$visual = visual; }
}
