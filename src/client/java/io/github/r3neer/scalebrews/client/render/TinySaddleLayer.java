package io.github.r3neer.scalebrews.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Separate equipment geometry driven by the mount's already-final animated body pose. */
public class TinySaddleLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    private final SaddleModel<S> bodyModel;
    private final SaddleModel<S> boneModel;
    private final SaddleModel<S> wolfModel;

    public TinySaddleLayer(RenderLayerParent<S, M> parent) {
        super(parent);
        bodyModel = new SaddleModel<>("body", false);
        wolfModel = new SaddleModel<>("body", true);
        boneModel = new SaddleModel<>("bone", false);
    }

    @Override
    public void submit(PoseStack poses, SubmitNodeCollector collector, int light, S state, float yaw, float pitch) {
        var bridge = (SaddleState) state;
        var visual = bridge.scalebrews$saddle();
        if (visual == null || state.isBaby || state.isInvisible) return;
        var root = getParentModel().root();
        if (!root.hasChild(visual.anchor())) return;
        bridge.scalebrews$saddlePose(SaddleState.SaddlePose.capture(root, root.getChild(visual.anchor())));
        var model = state instanceof net.minecraft.client.renderer.entity.state.WolfRenderState ? wolfModel
                : visual.anchor().equals("body") ? bodyModel : boneModel;
        renderColoredCutoutModel(model, visual.texture(), poses, collector, light, state, -1, 1);
    }

    private static class SaddleModel<S extends LivingEntityRenderState> extends EntityModel<S> {
        SaddleModel(String anchor, boolean wolf) {
            super(mesh(anchor, wolf).bakeRoot());
        }

        @Override
        public void setupAnim(S state) {
            super.setupAnim(state);
            var snapshot = ((SaddleState) state).scalebrews$saddlePose();
            if (snapshot == null) return;
            snapshot.root().apply(root());
            snapshot.anchor().apply(root().getChild("saddle"));
        }

        private static LayerDefinition mesh(String anchor, boolean wolf) {
            var mesh = new MeshDefinition();
            var group = mesh.getRoot().addOrReplaceChild("saddle", CubeListBuilder.create(), PartPose.ZERO);
            var pose = anchor.equals("body") ? PartPose.rotation(-(float)Math.PI / 2, 0, 0) : PartPose.ZERO;
            var equipment = group.addOrReplaceChild("equipment", CubeListBuilder.create(), pose);
            float top = wolf ? -3.7F : anchor.equals("body") ? -3.15F : -4.15F;
            equipment.addOrReplaceChild("pad", CubeListBuilder.create().texOffs(0, 8)
                    .addBox(-3, top - .5F, -1.5F, 6, .5F, 6), PartPose.ZERO);
            equipment.addOrReplaceChild("seat", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-2.5F, top - 1.25F, -1, 5, 1, 5), PartPose.ZERO);
            equipment.addOrReplaceChild("front", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-2.5F, top - 1.75F, -1, 5, .5F, 1), PartPose.ZERO);
            equipment.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-2.5F, top - 1.75F, 3, 5, .5F, 1), PartPose.ZERO);
            float side = wolf ? 3.35F : anchor.equals("body") ? 3.05F : 3.55F;
            for (int sign : new int[]{-1, 1}) {
                equipment.addOrReplaceChild("strap" + sign, CubeListBuilder.create().texOffs(32, 0)
                        .addBox(sign * side - .3F, top, 2, .6F, 6, 1), PartPose.ZERO);
                equipment.addOrReplaceChild("buckle" + sign, CubeListBuilder.create().texOffs(40, 0)
                        .addBox(sign * (side + .15F) - .4F, top + 3, 1.5F, .8F, 2, 2), PartPose.ZERO);
            }
            return LayerDefinition.create(mesh, 64, 32);
        }
    }
}
