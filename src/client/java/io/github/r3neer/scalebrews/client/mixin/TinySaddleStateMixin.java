package io.github.r3neer.scalebrews.client.mixin;

import io.github.r3neer.scalebrews.client.render.SaddleState;
import io.github.r3neer.scalebrews.client.render.RiderPoseState;
import org.joml.Matrix4fc;
import io.github.r3neer.scalebrews.mount.TinyMountDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class TinySaddleStateMixin implements SaddleState, RiderPoseState {
    @Unique private TinyMountDefinition.SaddleVisual scalebrews$visual;
    @Unique private Matrix4fc scalebrews$riderPose;
    public Matrix4fc scalebrews$riderPose() { return scalebrews$riderPose; }
    public void scalebrews$riderPose(Matrix4fc pose) { scalebrews$riderPose = pose; }
    public TinyMountDefinition.SaddleVisual scalebrews$saddle() { return scalebrews$visual; }
    public void scalebrews$saddle(TinyMountDefinition.SaddleVisual visual) { scalebrews$visual = visual; }
}
