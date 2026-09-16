package io.github.r3neer.scalebrews.test;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.render.MountPoseState;
import io.github.r3neer.scalebrews.client.render.MountRenderFrame;
import io.github.r3neer.scalebrews.client.render.RiderPoseState;
import io.github.r3neer.scalebrews.client.render.SeatFrame;
import io.github.r3neer.scalebrews.client.render.TinyMountSeatResolver;
import io.github.r3neer.scalebrews.client.render.TinyMountVisualProfile;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;

/** Pure client proofs for JSON validation, nested surface selection and same-frame lifetime. */
public final class TinyMountVisualTests {
    private TinyMountVisualTests() {}

    public static void run() {
        profilesAreValidated();
        nestedAnimatedPartOwnsTheSeat();
        frameStateIsOrderedAndNeverStale();
        selectionDoesNotDependOnFirstRender();
        var target=new Quaternionf().rotationY(2);
        var smoothed=MountRenderFrame.interpolateRotation(new Quaternionf(),target,1.0/60);
        if (!(smoothed.angle()>0 && smoothed.angle()<target.angle()))
            throw new AssertionError("Rider rotation did not interpolate");
    }

    private static void profilesAreValidated() {
        var profile = TinyMountVisualProfile.parse(JsonParser.parseString("""
                {"anchor":{"path":"root/body/body_rotation","point":[0.5,1,0.5],"offset":[1,2,3],"rotation":[4,5,6]},
                 "saddle":{"texture":"minecraft:textures/entity/pig/pig.png","width":1.2,"length":0.8,"strap_length":1.1,"seat_height":0.9}}
                """).getAsJsonObject());
        if (!"root/body/body_rotation".equals(profile.anchor().path()) || Math.abs(profile.saddle().width() - 1.2F) > .0001F)
            throw new AssertionError("Complete visual profile did not preserve its values");
        var defaults = TinyMountVisualProfile.parse(JsonParser.parseString("{}").getAsJsonObject());
        if (defaults.anchor().path() != null || defaults.saddle().texture() == null)
            throw new AssertionError("Empty visual profile did not receive safe defaults");
        assertInvalid("{\"anchor\":{\"point\":[2,0,0]}}");
        assertInvalid("{\"saddle\":{\"width\":0}}");
        assertInvalid("{\"anchor\":{\"path\":\"root//body\"}}");
        assertInvalid("{\"unknown\":true}");
    }

    private static void nestedAnimatedPartOwnsTheSeat() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().addBox(-2, -4, -2, 4, 4, 4), PartPose.ZERO);
        var body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        body.addOrReplaceChild("body_rotation", CubeListBuilder.create().addBox(-4, -6, -5, 8, 6, 10), PartPose.ZERO);
        var baked = LayerDefinition.create(mesh, 16, 16).bakeRoot();
        var outer = new Matrix4f().scale(-1, -1, 1);
        var first = TinyMountSeatResolver.resolve(baked, outer, TinyMountVisualProfile.DEFAULT, 1, "synthetic nested mount");
        if (!"root/body/body_rotation".equals(first.path()))
            throw new AssertionError("Autodetection selected " + first.path() + " instead of the large nested torso");
        baked.getChild("body").getChild("body_rotation").xRot = .5F;
        var second = TinyMountSeatResolver.resolve(baked, outer, TinyMountVisualProfile.DEFAULT, 2, "synthetic nested mount");
        if (first.cameraPosition().distance(second.cameraPosition()) < .01F)
            throw new AssertionError("SeatFrame did not capture the animated nested chain");
        var explicit = TinyMountVisualProfile.parse(JsonParser.parseString("{\"anchor\":{\"path\":\"root/head\"}}").getAsJsonObject());
        if (!"root/head".equals(TinyMountSeatResolver.resolve(baked, outer, explicit, 3, "synthetic nested mount").path()))
            throw new AssertionError("Explicit visual path did not override autodetection");
        var bottomPoint = TinyMountVisualProfile.parse(JsonParser.parseString(
                "{\"anchor\":{\"path\":\"root/body/body_rotation\",\"point\":[0.5,0,0.5]}}" ).getAsJsonObject());
        var topPoint = TinyMountVisualProfile.parse(JsonParser.parseString(
                "{\"anchor\":{\"path\":\"root/body/body_rotation\",\"point\":[0.5,1,0.5]}}" ).getAsJsonObject());
        if (TinyMountSeatResolver.resolve(baked, outer, bottomPoint, 4, "synthetic nested mount").cameraPosition()
                .distance(TinyMountSeatResolver.resolve(baked, outer, topPoint, 5, "synthetic nested mount").cameraPosition()) < .2F)
            throw new AssertionError("Normalized anchor point Y did not span the selected cube volume");
        var torso = baked.getChild("body").getChild("body_rotation");
        torso.visible = false;
        if (!"root/head".equals(TinyMountSeatResolver.resolve(baked, outer, TinyMountVisualProfile.DEFAULT, 6,
                "synthetic hidden variant").path()))
            throw new AssertionError("Hidden selected torso was retained across a model variant change");
        torso.visible = true;
        if (!"root/body/body_rotation".equals(TinyMountSeatResolver.resolve(baked, outer, TinyMountVisualProfile.DEFAULT, 7,
                "synthetic restored variant").path()))
            throw new AssertionError("Restored torso did not receive its own cached model variant selection");
    }

    private static void frameStateIsOrderedAndNeverStale() {
        var mount = new LivingEntityRenderState();
        var rider = new LivingEntityRenderState();
        ((MountPoseState)mount).scalebrews$entityId(41);
        ((MountPoseState)mount).scalebrews$tinyMount(true);
        ((MountPoseState)rider).scalebrews$entityId(42);
        ((RiderPoseState)rider).scalebrews$vehicleId(41);
        var input = new ArrayList<net.minecraft.client.renderer.entity.state.EntityRenderState>();
        input.add(rider); input.add(mount);
        var ordered = MountRenderFrame.stableTopological(input);
        if (ordered.get(0) != mount || ordered.get(1) != rider)
            throw new AssertionError("Tiny Mount was not submitted before its rider");
        MountRenderFrame.begin(input);
        var preview = new LivingEntityRenderState();
        ((RiderPoseState)preview).scalebrews$vehicleId(41);
        long serial = MountRenderFrame.serial();
        MountRenderFrame.capture(41, new SeatFrame("root/body", new Matrix4f(), new Vector3f(1, 2, 3), new Vector3f(0, 1, 0),
                new Matrix4f(), 1, 1, 1, 1, serial));
        if (MountRenderFrame.riderTransform((RiderPoseState)rider, rider, new PoseStack()) == null)
            throw new AssertionError("Same-frame rider did not receive its captured SeatFrame");
        if (MountRenderFrame.riderTransform((RiderPoseState)preview, preview, new PoseStack()) != null)
            throw new AssertionError("Inventory preview consumed a world attachment");
        rider.boundingBoxHeight = 2;
        var rotated = new PoseStack();
        rotated.translate(7, 4, -2);
        rotated.mulPose(new Quaternionf().rotationY(.8F));
        Matrix4f correction = MountRenderFrame.riderTransform((RiderPoseState)rider, rider, rotated);
        Vector3f correctedOrigin = new Matrix4f(rotated.last().pose()).mul(correction).getTranslation(new Vector3f());
        if (correctedOrigin.distance(new Vector3f(1, 1.2F, 3)) > 1E-4F)
            throw new AssertionError("Rotated rider stack moved to " + correctedOrigin + " instead of the world-space seat");
        io.github.r3neer.scalebrews.client.platform.PlatformCamera.reset();
        ((RiderPoseState)rider).scalebrews$firstPersonOffset(new net.minecraft.world.phys.Vec3(.2,0,0));
        rider.scale=.5F;
        var firstPersonCorrection=MountRenderFrame.riderTransform((RiderPoseState)rider,rider,rotated);
        var firstPersonOrigin=new Matrix4f(rotated.last().pose()).mul(firstPersonCorrection).getTranslation(new Vector3f());
        if(firstPersonOrigin.distance(new Vector3f(6.9F,4,-2))>1E-4F)
            throw new AssertionError("FirstPerson lost its scaled extraction origin or consumed the unbounded seat target");
        MountRenderFrame.end();
        if (MountRenderFrame.riderTransform((RiderPoseState)rider, rider, new PoseStack()) != null)
            throw new AssertionError("SeatFrame survived the end of its render frame");
    }

    private static void selectionDoesNotDependOnFirstRender() {
        var mesh = new MeshDefinition();
        var group = mesh.getRoot().addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.ZERO);
        var body = group.addOrReplaceChild("body", CubeListBuilder.create().addBox(-3,-4,-5,6,8,10), PartPose.ZERO);
        body.addOrReplaceChild("mane_shake", CubeListBuilder.create().addBox(-8,-8,-8,16,16,16), PartPose.ZERO);
        group.addOrReplaceChild("head2", CubeListBuilder.create().addBox(-8,-8,-8,16,16,16), PartPose.ZERO);
        var baked = LayerDefinition.create(mesh, 32,32).bakeRoot();
        var profile = TinyMountVisualProfile.parse(JsonParser.parseString("{\"anchor\":{\"path\":\"bone\"}}").getAsJsonObject());
        var outer = new Matrix4f().scale(-1,-1,1);
        var first = TinyMountSeatResolver.resolve(baked,outer,profile,1,"group rest");
        if (!first.path().equals("root/bone/body")) throw new AssertionError("Group anchor must resolve descendants");
        baked.getChild("bone").xRot=1.2F;
        TinyMountSeatResolver.clearCaches();
        var second=TinyMountSeatResolver.resolve(baked,new Matrix4f().rotateY(2).mul(outer),profile,2,"rotated first render");
        if (!first.path().equals(second.path()) || Math.abs(first.width()-second.width())>1E-5
                || Math.abs(first.depth()-second.depth())>1E-5)
            throw new AssertionError("First render orientation changed saddle geometry");
        float angle=second.riderRotationDelta().getUnnormalizedRotation(new Quaternionf()).angle();
        if (angle<1) throw new AssertionError("Large animation was abruptly rebased instead of retained");
        var replacement=LayerDefinition.create(mesh,32,32).bakeRoot().getChild("bone");
        ((io.github.r3neer.scalebrews.client.mixin.ModelPartAccess)(Object)baked).scalebrews$children().put("bone",replacement);
        var replaced=TinyMountSeatResolver.resolve(baked,outer,profile,3,"same-shape replacement");
        if (replaced.cameraPosition().distance(first.cameraPosition())>1E-5)
            throw new AssertionError("Cached chain retained a replaced model variant");
    }

    private static void assertInvalid(String json) {
        try {
            TinyMountVisualProfile.parse(JsonParser.parseString(json).getAsJsonObject());
            throw new AssertionError("Invalid visual profile was accepted: " + json);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
