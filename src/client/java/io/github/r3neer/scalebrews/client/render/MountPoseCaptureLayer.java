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

/** Captures the final body frame after vanilla and optional model animation mods have run. */
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
        if (!snapshot.scalebrews$hasPassengers()) return;
        var root = getParentModel().root();
        String anchorName = snapshot.scalebrews$mountAnchor();
        if (anchorName == null || !root.hasChild(anchorName)) {
            anchorName = root.hasChild("body") ? "body" : root.hasChild("bone") ? "bone" : null;
        }
        if (anchorName == null) return;
        MountRiderPose.capture(snapshot.scalebrews$entityId(), state, renderer.getRenderOffset(state),
                new Matrix4f(poses.last().pose()), root, root.getChild(anchorName));
    }
}
