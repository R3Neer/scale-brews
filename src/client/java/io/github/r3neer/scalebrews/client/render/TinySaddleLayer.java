package io.github.r3neer.scalebrews.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import java.util.function.Supplier;

/** Separate equipment geometry; the mob's current variant and animation remain authoritative. */
public class TinySaddleLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    private final SaddleModel<S> bodyModel;
    private final SaddleModel<S> boneModel;
    private final SaddleModel<S> wolfModel;
    public TinySaddleLayer(RenderLayerParent<S, M> parent) {
        super(parent);
        bodyModel = new SaddleModel<>("body", this::getParentModel, false);
        wolfModel = new SaddleModel<>("body", this::getParentModel, true);
        boneModel = new SaddleModel<>("bone", this::getParentModel, false);
    }
    @Override public void submit(PoseStack poses, SubmitNodeCollector collector, int light, S state, float yaw, float pitch) {
        var visual = ((SaddleState)state).scalebrews$saddle();
        if (visual == null || state.isBaby || state.isInvisible) return;
        var model = state instanceof net.minecraft.client.renderer.entity.state.WolfRenderState ? wolfModel
                : visual.anchor().equals("body") ? bodyModel : boneModel;
        if (!getParentModel().root().hasChild(visual.anchor())) return;
        renderColoredCutoutModel(model, visual.texture(), poses, collector, light, state, -1, 1);
    }

    private static class SaddleModel<S extends LivingEntityRenderState> extends EntityModel<S> {
        private final String anchor;
        private final Supplier<? extends EntityModel<? super S>> parent;
        SaddleModel(String anchor, Supplier<? extends EntityModel<? super S>> parent, boolean wolf) {
            super(mesh(anchor, wolf).bakeRoot());
            this.anchor = anchor;
            this.parent = parent;
        }
        @Override public void setupAnim(S state) {
            super.setupAnim(state);
            var model = parent.get();
            // Submit collectors can render later: derive the pose from this snapshot, not a previous mob.
            model.setupAnim(state);
            var source = model.root().getChild(anchor);
            var target = root().getChild("saddle");
            target.loadPose(source.storePose());
            target.xScale = source.xScale;
            target.yScale = source.yScale;
            target.zScale = source.zScale;
        }
        private static LayerDefinition mesh(String anchor, boolean wolf) {
            var mesh = new MeshDefinition();
            var group = mesh.getRoot().addOrReplaceChild("saddle", CubeListBuilder.create(), PartPose.ZERO);
            // Body anchors (chicken) have a quarter-turn; orient the equipment back into the animal's frame.
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
