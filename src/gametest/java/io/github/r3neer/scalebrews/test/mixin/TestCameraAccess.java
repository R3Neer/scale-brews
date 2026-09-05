package io.github.r3neer.scalebrews.test.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.Projection;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface TestCameraAccess {
    @Accessor("projection") Projection test$projection();
    @Invoker("createProjectionMatrixForCulling") Matrix4f test$cullingProjection();
}
