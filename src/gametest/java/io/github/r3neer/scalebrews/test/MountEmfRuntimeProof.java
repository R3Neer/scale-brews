package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.render.RiderPoseState;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.animal.bee.Bee;
import org.joml.Vector3f;

/**
 * Opt-in proof for the exact EMF/ETF/Fresh Animations versions pinned by VanillaPlus.
 * The normal base suite deliberately no-ops this class; the diagnostic workflow enables it.
 */
public final class MountEmfRuntimeProof implements FabricClientGameTest {
    private static final String ENV = "SCALEBREWS_EMF_PROOF";

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.parseBoolean(System.getenv(ENV))) return;

        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();

            context.runOnClient(client -> {
                var loader = FabricLoader.getInstance();
                if (!loader.isModLoaded("entity_model_features"))
                    throw new AssertionError("EMF is not loaded in the runtime proof");
                if (!loader.isModLoaded("entity_texture_features"))
                    throw new AssertionError("ETF is not loaded in the runtime proof");

                var selected = client.getResourcePackRepository().getSelectedIds();
                if (selected.stream().noneMatch(id -> id.contains("FreshAnimations_v1.10.5.zip")))
                    throw new AssertionError("Fresh Animations 1.10.5 is not selected: " + selected);
            });

            var server = world.getServer();
            server.runCommand("gamemode survival @a");
            server.runCommand("fill -5 -61 -5 5 -61 5 minecraft:stone");
            server.runCommand("tp @a 0 -60 0 0 0");
            server.runCommand("effect give @a scalebrews:shrinking 120 1 true");
            server.runCommand("summon minecraft:bee 0 -60 2 {Tags:[emf_mount_bee],NoAI:1b}");
            server.runCommand("item replace entity @e[tag=emf_mount_bee,limit=1] saddle with minecraft:saddle");
            server.runCommand("ride @a[limit=1] mount @e[tag=emf_mount_bee,limit=1]");
            context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            context.waitTicks(20);

            context.runOnClient(client -> {
                Bee bee = null;
                for (var entity : client.level.entitiesForRendering()) {
                    if (entity instanceof Bee candidate) {
                        bee = candidate;
                        break;
                    }
                }
                if (bee == null) throw new AssertionError("EMF bee fixture is missing on the client");

                var renderer = client.getEntityRenderDispatcher().getRenderer(bee);
                if (!(renderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer))
                    throw new AssertionError("Bee renderer is not a LivingEntityRenderer under EMF");

                Object model = livingRenderer.getModel();
                try {
                    Class<?> emfModel = Class.forName("traben.entity_model_features.models.IEMFModel");
                    if (!emfModel.isInstance(model))
                        throw new AssertionError("Bee model does not expose EMF's IEMFModel runtime interface");
                    boolean isEmf = (boolean) emfModel.getMethod("emf$isEMFModel").invoke(model);
                    Object emfRoot = emfModel.getMethod("emf$getEMFRootModel").invoke(model);
                    if (!isEmf || emfRoot == null)
                        throw new AssertionError("Fresh Animations did not produce an active EMF bee model");
                    if (!emfRoot.getClass().getName().contains("EMFModelPartRoot"))
                        throw new AssertionError("Unexpected EMF root class: " + emfRoot.getClass().getName());
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError("Could not interrogate the loaded EMF model", e);
                }

                var riderState = client.getEntityRenderDispatcher().getRenderer(client.player)
                        .createRenderState(client.player, 1.0F);
                var pose = ((RiderPoseState) riderState).scalebrews$riderPose();
                if (pose == null)
                    throw new AssertionError("Mounted rider did not receive the final EMF model attachment pose");
                var transformed = pose.transformPosition(new Vector3f(0, 0, 1));
                if (!Float.isFinite(transformed.x) || !Float.isFinite(transformed.y) || !Float.isFinite(transformed.z))
                    throw new AssertionError("EMF rider attachment produced a non-finite transform");
                if (transformed.distance(new Vector3f(0, 0, 1)) <= .0001F)
                    throw new AssertionError("Final EMF rider attachment pose remained identity");
            });

            context.takeScreenshot("scale-brews-emf-fresh-animations-bee-rider");
        }
    }
}
