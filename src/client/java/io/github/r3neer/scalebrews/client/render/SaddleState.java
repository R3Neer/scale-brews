package io.github.r3neer.scalebrews.client.render;

import io.github.r3neer.scalebrews.mount.TinyMountDefinition;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

public interface SaddleState {
    TinyMountDefinition.SaddleVisual scalebrews$saddle();
    void scalebrews$saddle(TinyMountDefinition.SaddleVisual visual);
    SaddlePose scalebrews$saddlePose();
    void scalebrews$saddlePose(SaddlePose pose);

    record PartSnapshot(PartPose pose, float xScale, float yScale, float zScale) {
        public static PartSnapshot capture(ModelPart part) {
            return new PartSnapshot(part.storePose(), part.xScale, part.yScale, part.zScale);
        }
        public void apply(ModelPart part) {
            part.loadPose(pose);
            part.xScale = xScale;
            part.yScale = yScale;
            part.zScale = zScale;
        }
    }
    record SaddlePose(PartSnapshot root, PartSnapshot anchor) {
        public static SaddlePose capture(ModelPart root, ModelPart anchor) {
            return new SaddlePose(PartSnapshot.capture(root), PartSnapshot.capture(anchor));
        }
    }
}
