package io.github.r3neer.scalebrews.test;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.r3neer.scalebrews.test.mixin.TestCameraAccess;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.joml.Matrix4f;

/** Runs inside a real rendered client, exercising the injected methods rather than only the helper. */
public final class ScaleCameraChecks {
    private ScaleCameraChecks() {}

    private static void near(double actual, double expected, String message) {
        if (Math.abs(actual - expected) > 0.000001) throw new AssertionError(message + ": " + actual + " expected " + expected);
    }

    public static void run(Minecraft client) {
        var camera = client.gameRenderer.mainCamera();
        var access = (TestCameraAccess) camera;
        var scale = client.player.getAttribute(Attributes.SCALE);
        double base = scale.getBaseValue();
        var cameraType = client.options.getCameraType();
        var avatar = client.player.avatarState();
        avatar.updateBob(.1F);
        try {
            // Include intermediate blend values, restoration, Growth and very small external scales.
            for (double size : new double[]{1, .76, .64, .52, .40, .28, .0625, 1, 2}) {
                scale.setBaseValue(size);
                for (var type : CameraType.values()) {
                    client.options.setCameraType(type);
                    double factor = type.isFirstPerson() ? Math.clamp(client.player.getScale(), .05, 1) : 1;
                    var position = client.player.position();
                    var bounds = client.player.getBoundingBox();
                    camera.update(DeltaTracker.ONE);
                    var projection = access.test$projection();
                    near(projection.zNear(), .05 * factor, "Render near plane " + size + " " + type);
                    near(camera.getNearPlane(camera.getFov()).getPointOnPlane(0, 0).length(), .05 * factor, "Near-plane sampling agrees");
                    float cullFov = Math.max(camera.getFov(), client.options.fov().get());
                    var expected = new Matrix4f().perspective(cullFov * (float) (Math.PI / 180),
                            projection.width() / projection.height(), (float) (.05 * factor), projection.zFar(),
                            RenderSystem.getDevice().getDeviceInfo().isZZeroToOne());
                    if (!access.test$cullingProjection().equals(expected, .000001F)) throw new AssertionError("Culling near plane disagrees");
                    var state = new CameraRenderState();
                    camera.extractRenderState(state, 1);
                    float bob = avatar.getInterpolatedBob(1);
                    if (bob <= 0) throw new AssertionError("Bobbing fixture must be nonzero");
                    near(state.entityRenderState.bob, bob * factor, "Bobbing follows current scale");
                    if (!position.equals(client.player.position()) || !bounds.equals(client.player.getBoundingBox())) {
                        throw new AssertionError("Camera must not move player or alter collision");
                    }
                }
            }
            scale.setBaseValue(.28);
            client.options.setCameraType(CameraType.FIRST_PERSON);
            camera.enablePanoramicMode();
            camera.update(DeltaTracker.ONE);
            near(access.test$projection().zNear(), .05, "Panorama remains vanilla");
        } finally {
            camera.disablePanoramicMode();
            scale.setBaseValue(base);
            client.options.setCameraType(cameraType);
            avatar.resetBob();
            camera.update(DeltaTracker.ONE);
        }
    }
}
