package io.github.r3neer.scalebrews.client.mixin;

import io.github.r3neer.scalebrews.client.render.MountPoseState;
import io.github.r3neer.scalebrews.client.render.SaddleState;
import io.github.r3neer.scalebrews.client.render.RiderPoseState;
import io.github.r3neer.scalebrews.mount.TinyMountDefinition;
import io.github.r3neer.scalebrews.client.render.SeatFrame;
import io.github.r3neer.scalebrews.client.render.TinyMountVisualProfile;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class TinySaddleStateMixin implements SaddleState, RiderPoseState, MountPoseState {
    @Unique private TinyMountDefinition.SaddleVisual scalebrews$visual;
    @Unique private TinyMountVisualProfile scalebrews$visualProfile;
    @Unique private SeatFrame scalebrews$seatFrame;
    @Unique private boolean scalebrews$hasSaddle;
    @Unique private int scalebrews$entityId;
    @Unique private int scalebrews$vehicleId = -1;
    @Unique private boolean scalebrews$tinyMount;
    @Unique private Identifier scalebrews$mountType;

    public int scalebrews$vehicleId() { return scalebrews$vehicleId; }
    public void scalebrews$vehicleId(int id) { scalebrews$vehicleId = id; }
    public TinyMountDefinition.SaddleVisual scalebrews$saddle() { return scalebrews$visual; }
    public void scalebrews$saddle(TinyMountDefinition.SaddleVisual visual) { scalebrews$visual = visual; }
    public boolean scalebrews$hasSaddle() { return scalebrews$hasSaddle; }
    public void scalebrews$hasSaddle(boolean value) { scalebrews$hasSaddle = value; }
    public TinyMountVisualProfile scalebrews$visualProfile() { return scalebrews$visualProfile; }
    public void scalebrews$visualProfile(TinyMountVisualProfile profile) { scalebrews$visualProfile = profile; }
    public SeatFrame scalebrews$seatFrame() { return scalebrews$seatFrame; }
    public void scalebrews$seatFrame(SeatFrame frame) { scalebrews$seatFrame = frame; }
    public int scalebrews$entityId() { return scalebrews$entityId; }
    public void scalebrews$entityId(int id) { scalebrews$entityId = id; }
    public Identifier scalebrews$mountType() { return scalebrews$mountType; }
    public void scalebrews$mountType(Identifier type) { scalebrews$mountType = type; }
    public boolean scalebrews$tinyMount() { return scalebrews$tinyMount; }
    public void scalebrews$tinyMount(boolean value) { scalebrews$tinyMount = value; }
}
