package io.github.r3neer.scalebrews.client.mixin;
import io.github.r3neer.scalebrews.client.platform.PlatformRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
@Mixin(EntityRenderState.class)
public abstract class PlatformRenderStateMixin implements PlatformRenderState {
    @Unique private Vec3 scalebrews$offset=Vec3.ZERO;
    public Vec3 scalebrews$platformOffset() { return scalebrews$offset; }
    public void scalebrews$platformOffset(Vec3 value) { scalebrews$offset=value; }
}
