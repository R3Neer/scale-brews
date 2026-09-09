package io.github.r3neer.scalebrews.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Reproducible, disposable vanilla-background screenshots; no production saves. */
public final class ReadmeCapture implements FabricClientGameTest {
    public void runTest(ClientGameTestContext context) {
        boolean hud=context.computeOnClient(c->c.gui.hud.isHidden());
        try(var world=context.worldBuilder().create()) {
            var server=world.getServer();
            server.runCommand("time set day");server.runCommand("weather clear");
            server.runCommand("fill -30 -61 -30 30 -61 30 minecraft:grass_block");
            server.runCommand("gamemode spectator @a");
            server.runCommand("summon minecraft:villager 4 -60 4 {NoAI:1b,Silent:1b,Tags:[readme_growth]}");
            server.runCommand("summon minecraft:villager -2 -60 4 {NoAI:1b,Silent:1b,Tags:[readme_normal]}");
            server.runCommand("summon minecraft:villager -5 -60 4 {NoAI:1b,Silent:1b,Tags:[readme_shrink]}");
            server.runCommand("effect give @e[tag=readme_growth] scalebrews:growth 300 2 true");
            server.runCommand("effect give @e[tag=readme_shrink] scalebrews:shrinking 300 2 true");
            server.runCommand("tp @a 0.0 -57.8 17.0 180 0");
            context.runOnClient(c->{if(!c.gui.hud.isHidden())c.gui.hud.toggle();});
            context.waitTicks(50);world.getConnection().waitForChunksRender();
            context.takeScreenshot("readme-size-comparison");
            server.runOnServer(s->{for(var e:s.overworld().getAllEntities())if(e instanceof net.minecraft.world.entity.npc.villager.Villager)e.discard();});
            server.runCommand("summon minecraft:bee -2.0 -59.2 3.0 {NoAI:1b,NoGravity:1b,Silent:1b,Tags:[readme_mount]}");
            server.runCommand("summon minecraft:chicken 0.0 -60 3.0 {NoAI:1b,Silent:1b,Tags:[readme_mount]}");
            server.runCommand("summon minecraft:wolf 2.0 -60 3.0 {NoAI:1b,Silent:1b,Tags:[readme_mount]}");
            server.runOnServer(s->{
                for(var e:s.overworld().getAllEntities())if(e instanceof net.minecraft.world.entity.LivingEntity living && e.entityTags().contains("readme_mount")) {
                    if(e instanceof net.minecraft.world.entity.animal.wolf.Wolf wolf)wolf.tame(s.getPlayerList().getPlayers().getFirst());
                    living.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE));
                }
            });
            server.runCommand("tp @a 0.0 -59.0 8.0 180 17");context.waitTicks(60);
            context.takeScreenshot("readme-tiny-mounts");
        }finally{context.runOnClient(c->{if(c.gui.hud.isHidden()!=hud)c.gui.hud.toggle();});}
    }
}
