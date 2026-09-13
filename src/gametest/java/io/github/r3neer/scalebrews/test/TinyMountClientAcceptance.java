package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.client.TinyMountScreen;
import io.github.r3neer.scalebrews.client.render.RiderPoseState;
import io.github.r3neer.scalebrews.mount.TinyMountMenu;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import org.joml.Vector3f;

/** Manual-facing acceptance: E opens the correct tiny-mount UI and native horse body motion carries its rider. */
public final class TinyMountClientAcceptance implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            var server = world.getServer();
            server.runCommand("gamemode survival @a");
            server.runCommand("fill -5 -61 -5 10 -61 10 minecraft:stone");
            server.runCommand("tp @a 0 -60 0 0 0");

            server.runCommand("summon minecraft:chicken 0 -60 2 {Tags:[menu_chicken],NoAI:1b}");
            server.runCommand("item replace entity @e[tag=menu_chicken,limit=1] saddle with minecraft:saddle");
            server.runCommand("effect give @a scalebrews:shrinking 120 1 true");
            context.waitTicks(30);
            server.runCommand("ride @a[limit=1] mount @e[tag=menu_chicken,limit=1]");
            context.waitTicks(10);
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitForScreen(TinyMountScreen.class);
            context.runOnClient(client -> {
                if (!(client.player.containerMenu instanceof TinyMountMenu menu))
                    throw new AssertionError("Chicken E did not open TinyMountMenu");
                if (menu.hasWolfArmor() || menu.getSlot(1).isActive())
                    throw new AssertionError("Chicken menu exposed a graphical armor slot");
            });
            context.takeScreenshot("scale-brews-chicken-equipment-menu");
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitForScreen(null);
            server.runCommand("ride @a[limit=1] dismount");

            server.runCommand("summon minecraft:wolf 3 -60 2 {Tags:[menu_wolf],Tame:1b,NoAI:1b}");
            server.runCommand("item replace entity @e[tag=menu_wolf,limit=1] saddle with minecraft:saddle");
            server.runCommand("ride @a[limit=1] mount @e[tag=menu_wolf,limit=1]");
            context.waitTicks(10);
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitForScreen(TinyMountScreen.class);
            context.runOnClient(client -> {
                if (!(client.player.containerMenu instanceof TinyMountMenu menu) || !menu.hasWolfArmor()
                        || !menu.getSlot(1).isActive())
                    throw new AssertionError("Wolf menu did not expose its armor slot");
                if (client.getResourceManager().getResource(ScaleBrews.id(
                        "textures/gui/sprites/container/slot/wolf_armor.png")).isEmpty())
                    throw new AssertionError("Wolf armor slot sprite is missing");
            });
            context.takeScreenshot("scale-brews-wolf-equipment-menu");
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitForScreen(null);
            server.runCommand("ride @a[limit=1] dismount");

            server.runCommand("effect clear @a");
            context.waitTicks(30);
            server.runCommand("summon minecraft:horse 6 -60 2 {Tags:[animation_horse],Tame:1b,NoAI:1b}");
            server.runCommand("item replace entity @e[tag=animation_horse,limit=1] saddle with minecraft:saddle");
            server.runCommand("ride @a[limit=1] mount @e[tag=animation_horse,limit=1]");
            context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
            context.waitTicks(12);
            context.runOnClient(client -> assertHorseRiderPose(client, false));

            // This acceptance targets rendering, not the horse-jump input state machine. Force the
            // native rearing flag on the client so the vanilla model deterministically animates body.
            context.runOnClient(client -> {
                if (!(client.player.getVehicle() instanceof net.minecraft.world.entity.animal.equine.Horse horse))
                    throw new AssertionError("Horse acceptance fixture lost its rider before animation");
                horse.setStanding(20);
            });
            context.waitTicks(6);
            context.runOnClient(client -> assertHorseRiderPose(client, true));
            context.takeScreenshot("scale-brews-horse-animated-rider");
        }
    }

    private static void assertHorseRiderPose(net.minecraft.client.Minecraft client, boolean requireMotion) {
        if (!(client.player.getVehicle() instanceof net.minecraft.world.entity.animal.equine.Horse horse))
            throw new AssertionError("Horse acceptance fixture lost its rider");
        var state = client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player, 1);
        var pose = ((RiderPoseState)state).scalebrews$riderPose();
        if (pose == null) throw new AssertionError("Native horse did not publish a rider attachment pose");
        if (requireMotion) {
            float stand = horse.getStandAnim(1);
            if (stand <= .02F) throw new AssertionError("Forced native horse standing pose did not animate");
            var moved = pose.transformPosition(new Vector3f(0, 0, 1));
            if (moved.distance(new Vector3f(0, 0, 1)) <= .001F)
                throw new AssertionError("Animated horse body did not move the rider attachment");
        }
    }
}
