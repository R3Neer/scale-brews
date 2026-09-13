package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.client.TinyMountScreen;
import io.github.r3neer.scalebrews.client.render.RiderPoseState;
import io.github.r3neer.scalebrews.mount.TinyMountMenu;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Vector3f;

/** Manual-facing acceptance for tiny-mount inventory UX plus native horse body motion. */
public final class TinyMountClientAcceptance implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            var server = world.getServer();
            server.runCommand("gamemode survival @a");
            server.runCommand("fill -5 -61 -5 10 -61 10 minecraft:stone");
            server.runCommand("tp @a 0 -60 0 0 0");
            server.runCommand("effect give @a scalebrews:shrinking 120 1 true");
            context.waitTicks(30);

            // Bees mirror vanilla item-steered mounts such as pigs/striders: no mount inventory.
            server.runCommand("summon minecraft:bee -3 -60 2 {Tags:[menu_bee],NoAI:1b}");
            server.runCommand("item replace entity @e[tag=menu_bee,limit=1] saddle with minecraft:saddle");
            server.runCommand("ride @a[limit=1] mount @e[tag=menu_bee,limit=1]");
            context.waitTicks(10);
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitForScreen(InventoryScreen.class);
            context.runOnClient(client -> {
                var screen = client.gui.screen();
                if (!(screen instanceof InventoryScreen) || screen instanceof TinyMountScreen)
                    throw new AssertionError("Item-steered bee E must open normal player inventory, like pig/strider");
            });
            context.takeScreenshot("scale-brews-bee-player-inventory");
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitForScreen(null);
            server.runCommand("ride @a[limit=1] dismount");

            // Vanilla saddles are shearable. Exercise the real client->server entity interaction so
            // the server handles it with a genuine ServerPlayer, exactly as normal gameplay does.
            server.runCommand("tp @a -3 -60 0 0 0");
            server.runCommand("item replace entity @a weapon.mainhand with minecraft:shears");
            context.waitTicks(5);
            context.runOnClient(client -> {
                net.minecraft.world.entity.animal.bee.Bee bee = null;
                for (var entity : client.level.entitiesForRendering()) {
                    if (entity instanceof net.minecraft.world.entity.animal.bee.Bee candidate) {
                        bee = candidate;
                        break;
                    }
                }
                if (bee == null || !bee.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE))
                    throw new AssertionError("Bee shearing fixture is missing its saddle");
                if (!client.player.getMainHandItem().is(Items.SHEARS))
                    throw new AssertionError("Bee shearing fixture did not synchronize shears");
                var result = client.gameMode.interact(client.player, bee, new EntityHitResult(bee), InteractionHand.MAIN_HAND);
                if (!result.consumesAction())
                    throw new AssertionError("Shears did not consume the bee equipment interaction");
            });
            context.waitTicks(8);
            context.runOnClient(client -> {
                net.minecraft.world.entity.animal.bee.Bee bee = null;
                boolean saddleDrop = false;
                for (var entity : client.level.entitiesForRendering()) {
                    if (entity instanceof net.minecraft.world.entity.animal.bee.Bee candidate)
                        bee = candidate;
                    if (entity instanceof net.minecraft.world.entity.item.ItemEntity item
                            && item.getItem().is(Items.SADDLE) && item.position().distanceToSqr(-3, -60, 2) < 16)
                        saddleDrop = true;
                }
                if (bee == null || !bee.getItemBySlot(EquipmentSlot.SADDLE).isEmpty())
                    throw new AssertionError("Shears did not remove the bee saddle");
                if (!saddleDrop)
                    throw new AssertionError("Shearing the bee did not drop its saddle like pig/strider");
            });
            server.runCommand("item replace entity @a weapon.mainhand with minecraft:air");
            server.runCommand("tp @a 0 -60 0 0 0");

            server.runCommand("summon minecraft:chicken 0 -60 2 {Tags:[menu_chicken],NoAI:1b}");
            server.runCommand("item replace entity @e[tag=menu_chicken,limit=1] saddle with minecraft:saddle");
            server.runCommand("ride @a[limit=1] mount @e[tag=menu_chicken,limit=1]");
            context.waitTicks(10);
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitForScreen(TinyMountScreen.class);
            context.runOnClient(client -> {
                if (!(client.player.containerMenu instanceof TinyMountMenu menu))
                    throw new AssertionError("Chicken E did not open TinyMountMenu");
                if (menu.hasBodyEquipment() || menu.getSlot(1).isActive())
                    throw new AssertionError("Chicken menu exposed a graphical BODY slot");
            });
            context.takeScreenshot("scale-brews-chicken-equipment-menu");
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitForScreen(null);
            server.runCommand("ride @a[limit=1] dismount");
            // Native dismount placement is free to choose a safe point. Pin this networking test
            // inside the server's entity-interaction reach so it tests inventory grammar, not dismount geometry.
            server.runCommand("tp @a 0 -60 1 0 0");
            context.waitTicks(5);

            // DIRECT mirrors the vanilla Camel interaction grammar: secondary-use from outside opens
            // the same equipment menu instead of attempting to mount.
            context.runOnClient(client -> {
                net.minecraft.world.entity.animal.chicken.Chicken chicken = null;
                for (var entity : client.level.entitiesForRendering()) {
                    if (entity instanceof net.minecraft.world.entity.animal.chicken.Chicken candidate) {
                        chicken = candidate;
                        break;
                    }
                }
                if (chicken == null) throw new AssertionError("External chicken inventory fixture is missing");
                if (client.player.distanceToSqr(chicken) > 6.25)
                    throw new AssertionError("External chicken inventory fixture is outside intended interaction range");
                client.player.setShiftKeyDown(true);
                try {
                    var result = client.gameMode.interact(client.player, chicken,
                            new EntityHitResult(chicken), InteractionHand.MAIN_HAND);
                    if (!result.consumesAction())
                        throw new AssertionError("Secondary-use did not consume direct-family inventory interaction");
                } finally {
                    client.player.setShiftKeyDown(false);
                }
            });
            context.waitForScreen(TinyMountScreen.class);
            context.runOnClient(client -> {
                if (client.player.isPassenger())
                    throw new AssertionError("External direct-family inventory interaction mounted the player");
                if (!(client.player.containerMenu instanceof TinyMountMenu menu)
                        || !(menu.mount() instanceof net.minecraft.world.entity.animal.chicken.Chicken))
                    throw new AssertionError("External direct-family interaction did not open the chicken menu");
            });
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitForScreen(null);

            server.runCommand("summon minecraft:wolf 3 -60 2 {Tags:[menu_wolf],NoAI:1b}");
            // 26.2 derives tame state from Owner when loading entity data. Copy the test player's
            // UUID so this is a genuinely owned/tamed wolf rather than an ownerless fake Tame flag.
            server.runCommand("data modify entity @e[tag=menu_wolf,limit=1] Owner set from entity @a[limit=1] UUID");
            server.runCommand("item replace entity @e[tag=menu_wolf,limit=1] saddle with minecraft:saddle");
            server.runCommand("ride @a[limit=1] mount @e[tag=menu_wolf,limit=1]");
            context.waitTicks(10);
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitForScreen(TinyMountScreen.class);
            context.runOnClient(client -> {
                if (!(client.player.containerMenu instanceof TinyMountMenu menu) || !menu.hasBodyEquipment()
                        || !menu.getSlot(0).isActive() || !menu.getSlot(1).isActive())
                    throw new AssertionError("Wolf menu did not expose saddle plus configured BODY slots");
                if (!menu.getSlot(0).getItem().is(Items.SADDLE))
                    throw new AssertionError("Wolf menu did not reflect its native saddle equipment");
                if (!(menu.mount() instanceof net.minecraft.world.entity.animal.wolf.Wolf wolf)
                        || !wolf.isTame() || !wolf.isOwnedBy(client.player))
                    throw new AssertionError("Wolf acceptance fixture is not genuinely owned by the rider");
                if (client.getResourceManager().getResource(ScaleBrews.id(
                        "textures/gui/sprites/container/slot/wolf_armor.png")).isEmpty())
                    throw new AssertionError("Configured wolf BODY slot sprite is missing");
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
