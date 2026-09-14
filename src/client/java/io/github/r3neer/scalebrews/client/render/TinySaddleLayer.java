package io.github.r3neer.scalebrews.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Separate equipment geometry driven by the mount's already-final animated body pose. */
public class TinySaddleLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    private final SaddleModel<S> model;

    public TinySaddleLayer(RenderLayerParent<S, M> parent) {
        super(parent);
        model = new SaddleModel<>();
    }

    @Override
    public void submit(PoseStack poses, SubmitNodeCollector collector, int light, S state, float yaw, float pitch) {
        var bridge = (SaddleState) state;
        SeatFrame frame = bridge.scalebrews$seatFrame();
        if (!bridge.scalebrews$hasSaddle() || frame == null || state.isBaby || state.isInvisible) return;
        var profile = bridge.scalebrews$visualProfile().saddle();
        poses.pushPose();
        try {
            poses.mulPose(frame.saddleTransform());
            poses.scale(frame.width() * 16F / 6F, 1, frame.depth() * 16F / 6F);
            renderColoredCutoutModel(model, profile.texture(), poses, collector, light, state, -1, 1);
        } finally {
            poses.popPose();
        }
    }

    private static class SaddleModel<S extends LivingEntityRenderState> extends EntityModel<S> {
        SaddleModel() { super(mesh().bakeRoot()); }

        @Override
        public void setupAnim(S state) {
            super.setupAnim(state);
            SeatFrame frame = ((SaddleState)state).scalebrews$seatFrame();
            if (frame == null) return;
            var equipment = root().getChild("equipment");
            for (String name : new String[]{"pad", "seat", "front", "back"})
                equipment.getChild(name).yScale = frame.seatHeight();
            for (String suffix : new String[]{"-1", "1"}) {
                equipment.getChild("strap" + suffix).yScale = frame.strapLength();
                equipment.getChild("buckle" + suffix).y = 4F * (frame.strapLength() - 1F);
            }
        }

        private static LayerDefinition mesh() {
            var mesh = new MeshDefinition();
            var equipment = mesh.getRoot().addOrReplaceChild("equipment", CubeListBuilder.create(), net.minecraft.client.model.geom.PartPose.ZERO);
            equipment.addOrReplaceChild("pad", CubeListBuilder.create().texOffs(0, 8)
                    .addBox(-3, -.5F, -3, 6, .5F, 6), net.minecraft.client.model.geom.PartPose.ZERO);
            equipment.addOrReplaceChild("seat", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-2.5F, -1.25F, -2.5F, 5, 1, 5), net.minecraft.client.model.geom.PartPose.ZERO);
            equipment.addOrReplaceChild("front", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-2.5F, -1.75F, -2.5F, 5, .5F, 1), net.minecraft.client.model.geom.PartPose.ZERO);
            equipment.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-2.5F, -1.75F, 1.5F, 5, .5F, 1), net.minecraft.client.model.geom.PartPose.ZERO);
            for (int sign : new int[]{-1, 1}) {
                equipment.addOrReplaceChild("strap" + sign, CubeListBuilder.create().texOffs(32, 0)
                        .addBox(sign * 3F - .3F, 0, 1, .6F, 6, 1), net.minecraft.client.model.geom.PartPose.ZERO);
                equipment.addOrReplaceChild("buckle" + sign, CubeListBuilder.create().texOffs(40, 0)
                        .addBox(sign * 3.15F - .4F, 3, .5F, .8F, 2, 2), net.minecraft.client.model.geom.PartPose.ZERO);
            }
            return LayerDefinition.create(mesh, 64, 32);
        }
    }
}
