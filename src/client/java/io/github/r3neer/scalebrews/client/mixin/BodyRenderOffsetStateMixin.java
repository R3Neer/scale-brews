package io.github.r3neer.scalebrews.client.mixin;
import io.github.r3neer.scalebrews.client.collision.presentation.BodyRenderOffset;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
@Mixin(EntityRenderState.class)
public abstract class BodyRenderOffsetStateMixin implements BodyRenderOffset {
    @Unique private Vec3 scalebrews$offset=Vec3.ZERO;
    public Vec3 scalebrews$bodyOffset() { return scalebrews$offset; }
    public void scalebrews$bodyOffset(Vec3 value) { scalebrews$offset=value; }
}
