package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;

/** Real local controller: native boat input must coexist with prepared material ownership. */
public final class S36LocalBoatControlProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla("minecraft:cow","26.2",
            CowModel.createBodyLayer().bakeRoot(),Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),
                Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var cowId=new AtomicInteger();var boatId=new AtomicInteger();var bankStart=new AtomicInteger();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                var cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("No prepared cow");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);
                cow.getAttribute(Attributes.SCALE).setBaseValue(8);cow.refreshDimensions();
                level.addFreshEntity(cow);cowId.set(cow.getId());
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("s36:boat",profile));
                Platforms.tick(level);
                var frame=AnatomyMovement.queryFrame(cow).orElseThrow();
                var piece=frame.snapshot().pieces().get("root/body/cube_0");
                if(piece==null)throw new AssertionError("No prepared body piece");
                var bounds=piece.bounds();
                System.out.println("S36 prepared body bounds="+bounds);
                var boat=EntityTypes.OAK_BOAT.create(level,EntitySpawnReason.COMMAND);
                if(boat==null)throw new AssertionError("No boat");
                boat.setPos(bounds.getCenter().x,bounds.maxY,bounds.getCenter().z);
                boat.setYRot(-90);
                level.addFreshEntity(boat);boatId.set(boat.getId());
                boat.move(MoverType.SELF,new Vec3(0,-.1,0));
                if(!Platforms.supported(boat) || Platforms.state(boat).support!=null)
                    throw new AssertionError("Boat did not acquire canonical-only material contact");
                var player=server.getPlayerList().getPlayers().getFirst();
                player.teleportTo(boat.getX(),boat.getY(),boat.getZ());
                if(!player.startRiding(boat,true,true))throw new AssertionError("Local controller mount failed");
            });
            context.waitFor(client->client.level!=null && client.level.getEntity(cowId.get()) instanceof Cow
                && client.player!=null && client.player.getRootVehicle().getId()==boatId.get()
                && client.player.getRootVehicle().isLocalInstanceAuthoritative(),180);
            context.waitTicks(8);
            context.runOnClient(client->{
                var boat=client.player.getRootVehicle();
                var cow=(Cow)client.level.getEntity(cowId.get());
                if(!Platforms.supported(boat))throw new AssertionError("Client hull lacks material contact after local mount: "
                    +"boat="+boat.position()+" bottom="+boat.getBoundingBox().minY
                    +" contact="+AnatomyMovement.contact(boat)
                    +" faceTop="+AnatomyMovement.queryFrame(cow).orElseThrow().snapshot()
                        .pieces().get("root/body/cube_0").bounds().maxY
                    +" mode="+io.github.r3neer.scalebrews.collision.api.AnatomyApi.mode(boat));
            });
            var before=context.computeOnClient(client->{
                var boat=client.player.getRootVehicle();
                if(Platforms.supported(client.player) || boat.getId()!=boatId.get())
                    throw new AssertionError("Passenger acquired a second material owner");
                return boat.position();
            });
            context.runOnClient(client->client.options.keyUp.setDown(true));
            context.waitTicks(12);
            context.runOnClient(client->client.options.keyUp.setDown(false));
            context.waitTicks(5);
            var after=context.computeOnClient(client->{
                var boat=client.player.getRootVehicle();
                if(boat.getId()!=boatId.get() || !boat.isLocalInstanceAuthoritative()
                        || !Platforms.supported(boat) || Platforms.supported(client.player))
                    throw new AssertionError("Controlled hull lost unique canonical support");
                return boat.position();
            });
            if(after.subtract(before).horizontalDistanceSqr()<.01)
                throw new AssertionError("Native local key input did not row the canonical hull: "+before+" -> "+after);
            System.out.println("S36 dry native rowing "+before+" -> "+after);
            world.getServer().runOnServer(server->{
                var boat=(AbstractBoat)server.overworld().getEntity(boatId.get());
                if(boat==null || boat.getFirstPassenger()!=server.getPlayerList().getPlayers().getFirst()
                        || AnatomyMovement.contact(boat)==null || !Platforms.supported(boat))
                    throw new AssertionError("Server lost accepted hull/contact/passenger after real client rowing: "
                        +(boat==null?"missing":boat.position()+" status="+nativeStatus(boat)
                            +" contact="+AnatomyMovement.contact(boat)+" supported="+Platforms.supported(boat)
                            +" rider="+boat.getFirstPassenger()));
            });
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                var cow=(Cow)level.getEntity(cowId.get());
                var boat=(AbstractBoat)level.getEntity(boatId.get());
                var rider=server.getPlayerList().getPlayers().getFirst();
                var preCarry=boat.position();
                cow.move(MoverType.SELF,new Vec3(.2,0,0));
                if(Math.abs(boat.getX()-preCarry.x-.2)>1e-5 || !Platforms.supported(boat)
                        || boat.getFirstPassenger()!=rider || rider.getVehicle()!=boat)
                    throw new AssertionError("Occupied hull did not own one certified ROOT carry");
            });
            context.waitTicks(4);
            context.runOnClient(client->{
                var boat=client.player.getRootVehicle();
                if(boat.getId()!=boatId.get() || boat.getX()<after.x+.1 || !Platforms.supported(boat)
                        || Platforms.supported(client.player))
                    throw new AssertionError("Client material owner changed after occupied ROOT carry");
            });
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                var boat=(AbstractBoat)level.getEntity(boatId.get());
                int startX=(int)Math.floor(boat.getX()+2), y=(int)Math.floor(boat.getY())-1;
                bankStart.set(startX+12);
                int centerZ=(int)Math.floor(boat.getZ());
                System.out.println("S36 water startX="+startX+" y="+y+" hull="+boat.position()+" yaw="+boat.getYRot());
                for(int x=startX;x<startX+12;x++)
                    for(int z=centerZ-3;z<=centerZ+3;z++) {
                        level.setBlockAndUpdate(new BlockPos(x,y-1,z),Blocks.STONE.defaultBlockState());
                        level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.WATER.defaultBlockState());
                    }
                for(int x=startX+12;x<startX+30;x++)
                    for(int z=centerZ-3;z<=centerZ+3;z++)
                        level.setBlockAndUpdate(new BlockPos(x,y-1,z),Blocks.STONE.defaultBlockState());
                if(boat.isInWater())
                    throw new AssertionError("Water fixture moved or immersed dry hull before input");
            });
            context.runOnClient(client->client.options.keyUp.setDown(true));
            try {
                context.waitFor(client->{
                    if(client.player==null || !(client.level.getEntity(boatId.get()) instanceof AbstractBoat boat))return false;
                    return client.player.getRootVehicle()==boat && nativeWater(boat)
                        && AnatomyMovement.contact(boat)==null && !Platforms.supported(boat);
                },180);
            } catch(AssertionError timeout) {
                var diagnostic=context.computeOnClient(client->{
                    var boat=(AbstractBoat)client.level.getEntity(boatId.get());
                    if(boat==null)return "boat missing, rider root="+client.player.getRootVehicle();
                    return "boat="+boat.position()+" riderRoot="+client.player.getRootVehicle().getId()
                        +" yaw="+boat.getYRot()+" velocity="+boat.getDeltaMovement()
                        +" box="+boat.getBoundingBox()+" hCollision="+boat.horizontalCollision
                        +" fluid="+boat.isInWater()+" status="+nativeStatus((AbstractBoat)boat)
                        +" contact="+AnatomyMovement.contact(boat)
                        +" supported="+Platforms.supported(boat)+" legacy="+Platforms.state(boat).support
                        +" mode="+io.github.r3neer.scalebrews.collision.api.AnatomyApi.mode(boat)
                        +" block="+client.level.getBlockState(BlockPos.containing(boat.position()))
                        +" below="+client.level.getBlockState(BlockPos.containing(boat.getX(),boat.getY()-1,boat.getZ()));
                });
                throw new AssertionError("Native water entry unresolved: "+diagnostic,timeout);
            }
            context.runOnClient(client->client.options.keyUp.setDown(false));
            context.runOnClient(client->{
                var boat=client.player.getRootVehicle();
                if(AnatomyMovement.contact(boat)!=null || Platforms.supported(client.player))
                    throw new AssertionError("Water entry retained stale material/passenger owner");
            });
            context.waitTicks(4);
            world.getServer().runOnServer(server->{
                var boat=(AbstractBoat)server.overworld().getEntity(boatId.get());
                if(boat==null || !nativeWater(boat) || AnatomyMovement.contact(boat)!=null
                        || boat.getFirstPassenger()!=server.getPlayerList().getPlayers().getFirst())
                    throw new AssertionError("Native server water state did not retire material hull contact: "
                        +(boat==null?"missing":boat.position()+" status="+nativeStatus(boat)+" contact="+AnatomyMovement.contact(boat)));
            });
            context.runOnClient(client->client.options.keyUp.setDown(true));
            try {
                context.waitFor(client->{
                    if(client.player==null || !(client.level.getEntity(boatId.get()) instanceof AbstractBoat boat))return false;
                    return client.player.getRootVehicle()==boat && boat.getX()>bankStart.get()+.2
                        && "ON_LAND".equals(nativeStatus(boat)) && !nativeWater(boat)
                        && AnatomyMovement.contact(boat)==null;
                },240);
            } catch(AssertionError timeout) {
                var diagnostic=context.computeOnClient(client->{
                    var boat=(AbstractBoat)client.level.getEntity(boatId.get());
                    return boat==null?"missing":"boat="+boat.position()+" status="+nativeStatus(boat)
                        +" fluid="+boat.isInWater()+" rider="+client.player.getRootVehicle().getId()
                        +" bank="+bankStart.get()+" contact="+AnatomyMovement.contact(boat)
                        +" hCollision="+boat.horizontalCollision+" velocity="+boat.getDeltaMovement();
                });
                throw new AssertionError("Native water-to-bank exit unresolved: "+diagnostic,timeout);
            }
            context.runOnClient(client->client.options.keyUp.setDown(false));
            world.getServer().runOnServer(server->{
                var boat=(AbstractBoat)server.overworld().getEntity(boatId.get());
                if(boat==null || nativeWater(boat) || AnatomyMovement.contact(boat)!=null
                        || boat.getFirstPassenger()!=server.getPlayerList().getPlayers().getFirst())
                    throw new AssertionError("Boat failed native water-to-dry exit with one hull/passenger owner");
            });
        } finally {
            context.runOnClient(client->client.options.keyUp.setDown(false));
            world.getServer().runOnServer(AnatomyRuntime::stop);
            context.waitTicks(2);world.close();
        }
    }
    private static String nativeStatus(AbstractBoat boat) {
        try {
            var field=AbstractBoat.class.getDeclaredField("status");
            field.setAccessible(true);
            return String.valueOf(field.get(boat));
        } catch(ReflectiveOperationException failure) {
            throw new AssertionError("Cannot read native boat status in test fixture",failure);
        }
    }
    private static boolean nativeWater(AbstractBoat boat) {
        return nativeStatus(boat).contains("WATER");
    }
}
