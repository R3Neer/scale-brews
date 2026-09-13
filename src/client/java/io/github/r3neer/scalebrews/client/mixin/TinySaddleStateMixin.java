package io.github.r3neer.scalebrews.client.mixin;

import io.github.r3neer.scalebrews.client.render.MountPoseState;
import io.github.r3neer.scalebrews.client.render.SaddleState;
import io.github.r3neer.scalebrews.client.render.RiderPoseState;
import org.joml.Matrix4fc;
import io.github.r3neer.scalebrews.mount.TinyMountDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class TinySaddleStateMixin implements SaddleState, RiderPoseState, MountPoseState {
    @Unique private TinyMountDefinition.SaddleVisual scalebrews$visual;
    @Unique private SaddleState.SaddlePose scalebrews$saddlePose;
    @Unique private Matrix4fc scalebrews$riderPose;
    @Unique private int scalebrews$entityId;
    @Unique private boolean scalebrews$hasPassengers;
    @Unique private String scalebrews$mountAnchor;

    public Matrix4fc scalebrews$riderPose() { return scalebrews$riderPose; }
    public void scalebrews$riderPose(Matrix4fc pose) { scalebrews$riderPose = pose; }
    public TinyMountDefinition.SaddleVisual scalebrews$saddle() { return scalebrews$visual; }
    public void scalebrews$saddle(TinyMountDefinition.SaddleVisual visual) { scalebrews$visual = visual; }
    public SaddleState.SaddlePose scalebrews$saddlePose() { return scalebrews$saddlePose; }
    public void scalebrews$saddlePose(SaddleState.SaddlePose pose) { scalebrews$saddlePose = pose; }
    public int scalebrews$entityId() { return scalebrews$entityId; }
    public void scalebrews$entityId(int id) { scalebrews$entityId = id; }
    public boolean scalebrews$hasPassengers() { return scalebrews$hasPassengers; }
    public void scalebrews$hasPassengers(boolean value) { scalebrews$hasPassengers = value; }
    public String scalebrews$mountAnchor() { return scalebrews$mountAnchor; }
    public void scalebrews$mountAnchor(String anchor) { scalebrews$mountAnchor = anchor; }
}
