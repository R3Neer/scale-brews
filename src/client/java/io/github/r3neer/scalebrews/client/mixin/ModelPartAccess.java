package io.github.r3neer.scalebrews.client.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public interface ModelPartAccess {
    @Accessor("children") Map<String, ModelPart> scalebrews$children();
    @Accessor("cubes") List<ModelPart.Cube> scalebrews$cubes();
}
