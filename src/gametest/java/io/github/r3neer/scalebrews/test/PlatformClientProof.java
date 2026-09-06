package io.github.r3neer.scalebrews.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import io.github.r3neer.scalebrews.platform.Platforms;

/** Real local-player movement packets, unlike mock-player server-only tests. */
public class PlatformClientProof implements FabricClientGameTest {
    public void runTest(ClientGameTestContext context) {
        var properties=new java.util.Properties();
        properties.setProperty("allow-flight","false");
        try(var server=context.worldBuilder().createServer(properties);var connection=server.connect()) {
            server.runCommand("kill @e[tag=platform_boat]");
            server.runCommand("kill @e[tag=platform_proof]");
            server.runCommand("summon minecraft:ghast 0 20 0 {Tags:[\"platform_proof\"],NoAI:1b,NoGravity:1b,Silent:1b}");
            server.runCommand("gamemode survival @a");
            server.runCommand("tp @a 0 25 0");
            context.waitTicks(40);
            context.runOnClient(client -> {
                client.options.keyUp.setDown(false); client.options.keyDown.setDown(false);
                client.options.keyLeft.setDown(false); client.options.keyRight.setDown(false);
                client.options.keyJump.setDown(false);
            });
            context.runOnClient(client->{
                if(!Platforms.supported(client.player))
                    throw new AssertionError("Client player did not acquire support: "+client.player.position());
                verifyVisualProfiles(client);
            });
            var latency=context.computeOnClient(PlatformTestLatency::install);
            for(int delay:new int[]{0,100,200}) {
                latency.latency(delay);
                int count=Boolean.getBoolean("scalebrews.platformSoak")?400:12;
                for(int i=0;i<count;i++) {
                    final double dx=.04*Math.cos(i*.1),dy=.01*Math.cos(i*.07),dz=.04*Math.sin(i*.1);
                    server.runOnServer(s->{
                        for(var e:s.overworld().getAllEntities()) if(e.entityTags().contains("platform_proof"))
                            e.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(dx,dy,dz));
                    });
                    context.waitTicks(10);
                    context.runOnClient(client->{
                        if(client.player==null || !Platforms.supported(client.player))
                            throw new AssertionError("Lost support at RTT "+delay);
                    });
                }
                io.github.r3neer.scalebrews.ScaleBrews.LOGGER.info("Platform dedicated soak passed RTT {} ms ({} ticks)",delay,count*10);
                if(latency.corrections()>1) throw new AssertionError("Repeated movement corrections: "+latency.corrections());
            }
            latency.latency(0);
            for(int i=0;i<40;i++) {
                server.runCommand("execute as @e[tag=platform_proof] at @s run tp @s ~0.04 ~0.01 ~0.02");
                context.waitTicks(2);
                final int step=i;
                if(i%5==0) context.runOnClient(client->{
                    var state=Platforms.state(client.player);
                    io.github.r3neer.scalebrews.ScaleBrews.LOGGER.info("Platform proof step {} player {} support {}",
                        step,client.player.position(),state.support==null?null:state.support.position());
                });
            }
            context.waitTicks(20);
            context.runOnClient(client->{
                var s=Platforms.state(client.player);
                if(s.support==null || !Platforms.supported(client.player))
                    throw new AssertionError("Player lost moving support: "+client.player.position());
                double gap=client.player.getY()-s.support.getY()-4;
                if(Math.abs(gap)>.1) throw new AssertionError("Vertical drift: "+gap);
                if(client.player.getVehicle()!=null) throw new AssertionError("Platform unexpectedly mounts player");
            });
            context.takeScreenshot("platform-transport-proof");
            server.runOnServer(s->{
                var player=s.getPlayerList().getPlayers().getFirst();
                TestWorldgenValidation.assertCompleted();
                if(!Platforms.supported(player)) throw new AssertionError("Dedicated server lost support");
            });
            server.runCommand("summon minecraft:oak_boat 0 30 0 {Tags:[\"platform_boat\"]}");
            server.runCommand("execute at @e[tag=platform_proof,limit=1] run tp @e[tag=platform_boat] ~ ~5 ~");
            server.runCommand("ride @a[limit=1] mount @e[tag=platform_boat,limit=1]");
            server.runOnServer(s->{
                if(!s.getPlayerList().getPlayers().getFirst().isPassenger()) throw new AssertionError("Boat fixture did not mount player");
            });
            context.waitTicks(60);
            for(int delay:new int[]{0,100,200}) {
                latency.latency(delay);
                for(int i=0;i<20;i++) {
                    server.runOnServer(s->{
                        for(var e:s.overworld().getAllEntities()) if(e.entityTags().contains("platform_proof"))
                            e.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(.02,.01,.02));
                    });
                    context.waitTicks(5);
                    context.runOnClient(client->{
                        var root=client.player.getRootVehicle();
                        if(root==client.player || !Platforms.supported(root) || Platforms.supported(client.player))
                            throw new AssertionError("Occupied boat lost unique root support at RTT "+delay+" root="+root+" pos="+root.position()+" state="+Platforms.state(root).support+" passenger="+client.player.isPassenger());
                    });
                }
            }
            latency.latency(0);
            var boatBefore=context.computeOnClient(client->Platforms.state(client.player.getRootVehicle()).contact);
            context.runOnClient(client->client.options.keyUp.setDown(true));
            context.waitTicks(8);
            context.runOnClient(client->client.options.keyUp.setDown(false));
            context.waitTicks(8);
            context.runOnClient(client->{
                var root=client.player.getRootVehicle();
                if(!Platforms.supported(root) || Platforms.state(root).contact.distanceTo(boatBefore)<.01)
                    throw new AssertionError("Boat controls must retain independent movement on support");
            });
            context.runOnClient(client->{
                client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                client.player.setXRot(20);
            });
            context.waitTicks(5);
            context.takeScreenshot("platform-occupied-boat-proof");
            context.runOnClient(client->client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        }
    }
    private static void verifyVisualProfiles(net.minecraft.client.Minecraft client) {
        var registry=client.level.registryAccess().lookupOrThrow(Platforms.DEFINITIONS);
        int fixtureId=900000;
        for(var entry:registry.listElements().toList()) {
            var profile=entry.value();
            var type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(profile.entity());
            var support=type==net.minecraft.world.entity.EntityTypes.PLAYER?client.player:
                (net.minecraft.world.entity.LivingEntity)type.create(client.level,net.minecraft.world.entity.EntitySpawnReason.LOAD);
            if(support!=client.player) support.setId(fixtureId++);
            var body=new net.minecraft.world.entity.item.ItemEntity(client.level,0,0,0,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
            for(var surface:profile.surfaces()) {
                var state=Platforms.state(body);
                state.support=support;state.surface=surface;
                state.frame=io.github.r3neer.scalebrews.platform.PlatformGeometry.frame(support);
                state.contact=new net.minecraft.world.phys.Vec3(surface.x(),surface.y(),surface.z());
                body.setPos(state.frame.world(state.contact));
                var offset=io.github.r3neer.scalebrews.client.platform.PlatformVisuals.offset(body,.5f);
                if(!Double.isFinite(offset.lengthSqr()) || offset.length()>2*support.getScale())
                    throw new AssertionError("Invalid visual offset for "+profile.entity()+"/"+surface.id()+": "+offset);
                if(support instanceof net.minecraft.world.entity.animal.bee.Bee) {
                    var position=body.position();var bounds=body.getBoundingBox();
                    support.tickCount+=10;
                    var cached=io.github.r3neer.scalebrews.client.platform.PlatformVisuals.offset(body,.5f);
                    if(!cached.equals(offset)) throw new AssertionError("Same-frame visual sampling was not shared");
                    var animated=io.github.r3neer.scalebrews.client.platform.PlatformVisuals.offset(body,.75f);
                    if(animated.distanceTo(offset)<.001) throw new AssertionError("Bee bobbing did not move visual contact");
                    if(!body.position().equals(position) || !body.getBoundingBox().equals(bounds))
                        throw new AssertionError("Visual animation changed physics");
                }
            }
        }
        if(io.github.r3neer.scalebrews.client.platform.PlatformVisuals.missingVisualCount()!=0)
            throw new AssertionError("Built-in visual profile has an unresolved model part");
    }
}
