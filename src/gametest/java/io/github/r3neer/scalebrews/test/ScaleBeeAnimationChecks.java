package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.render.SaddleState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.animal.bee.AdultBeeModel;
import net.minecraft.client.renderer.entity.BeeRenderer;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.world.entity.animal.bee.Bee;

/** Real renderer snapshots and injected model animation, with an actual synchronized passenger. */
public final class ScaleBeeAnimationChecks {
    private ScaleBeeAnimationChecks() {}

    public static void run(Minecraft client, boolean occupied) {
        Bee bee = null;
        for (var entity : client.level.entitiesForRendering()) {
            if (entity instanceof Bee candidate) { bee = candidate; break; }
        }
        if (bee == null) throw new AssertionError("Bee fixture missing");
        var position = bee.position();
        var bounds = bee.getBoundingBox();
        var renderer = (BeeRenderer) client.getEntityRenderDispatcher().getRenderer(bee);
        var state = new BeeRenderState();
        renderer.extractRenderState(bee, state, 1);
        if (((SaddleState)state).scalebrews$occupied() != occupied)
            throw new AssertionError("Renderer did not extract passenger occupancy");
        var definition = io.github.r3neer.scalebrews.mount.TinyMounts.definition(bee);
        if (definition == null) throw new AssertionError("Bee definition missing");
        if (!definition.saddleVisual().orElseThrow().equals(((SaddleState)state).scalebrews$saddle()))
            throw new AssertionError("Saddle snapshot missing");

        var model = new AdultBeeModel(AdultBeeModel.createBodyLayer().bakeRoot());
        var bone = model.root().getChild("bone");
        var rest = bone.getInitialPose();
        float lastWing = Float.NaN;
        boolean wingMoved = false;
        boolean bodyMoved = false;
        state.isOnGround = false;
        for (boolean angry : new boolean[]{false, true}) {
            state.isAngry = angry;
            state.rollAmount = angry ? .75F : 0;
            for (float time : new float[]{0, 4, 8, 12, 16, 24, 36}) {
                state.ageInTicks = time;
                model.setupAnim(state);
                if (occupied && (Math.abs(bone.y - rest.y()) > .00001 || Math.abs(bone.xRot - rest.xRot()) > .00001))
                    throw new AssertionError("Mounted bee body moved away from passenger anchor");
                bodyMoved |= Math.abs(bone.y - rest.y()) > .01;
                float wing = bone.getChild("right_wing").zRot;
                wingMoved |= !Float.isNaN(lastWing) && Math.abs(wing - lastWing) > .01;
                lastWing = wing;
                if (Math.abs(wing + bone.getChild("left_wing").zRot) > .00001)
                    throw new AssertionError("Wing symmetry changed");
            }
        }
        if (!wingMoved || (!occupied && !bodyMoved))
            throw new AssertionError("Wing animation or unmounted bobbing was lost");
        state.isOnGround = true;
        state.rollAmount = 0;
        state.hasStinger = false;
        model.setupAnim(state);
        if (bone.getChild("body").getChild("stinger").visible)
            throw new AssertionError("Stinger state was overwritten");
        if (!bee.position().equals(position) || !bee.getBoundingBox().equals(bounds))
            throw new AssertionError("Model animation changed entity physics");
    }
}
