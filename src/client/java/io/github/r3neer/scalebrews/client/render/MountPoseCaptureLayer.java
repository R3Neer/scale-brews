package io.github.r3neer.scalebrews.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/** Captures a complete same-frame SeatFrame after vanilla/EMF setupAnim has run. */
public final class MountPoseCaptureLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    private final LivingEntityRenderer<? extends LivingEntity, S, M> renderer;

    public MountPoseCaptureLayer(LivingEntityRenderer<? extends LivingEntity, S, M> renderer,
                                 RenderLayerParent<S, M> parent) {
        super(parent);
        this.renderer = renderer;
    }

    @Override
    public void submit(PoseStack poses, SubmitNodeCollector collector, int light, S state, float yaw, float pitch) {
        var snapshot = (MountPoseState) state;
        if (!snapshot.scalebrews$tinyMount()) return;
        var root = getParentModel().root();
        var saddle = (SaddleState)state;
        var outer = new Matrix4f(poses.last().pose());
        var adapter = TinyMountVisualAdapters.find(renderer);
        var frame = adapter == null
                ? TinyMountSeatResolver.resolve(root, outer, saddle.scalebrews$visualProfile(), MountRenderFrame.serial(),
                        String.valueOf(snapshot.scalebrews$mountType()))
                : adapter.resolve(renderer, state, root, outer, saddle.scalebrews$visualProfile(), MountRenderFrame.serial());
        if (frame == null) return;
        if (saddle.scalebrews$hasSaddle()) {
            var raised = new org.joml.Vector4f(0, -1.25F * frame.seatHeight() / 16F, 0, 1)
                    .mul(new Matrix4f(poses.last().pose()).mul(frame.saddleTransform()));
            frame = new SeatFrame(frame.path(), frame.saddleTransform(), new org.joml.Vector3f(raised.x, raised.y, raised.z), frame.up(),
                    frame.riderRotationDelta(), frame.width(), frame.depth(), frame.strapLength(), frame.seatHeight(), frame.serial());
        }
        saddle.scalebrews$seatFrame(frame);
        MountRenderFrame.capture(snapshot.scalebrews$entityId(), frame);
        TinyMountCamera.capture(snapshot.scalebrews$entityId(), frame);
    }
}
