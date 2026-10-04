package io.github.r3neer.scalebrews.test;

import com.google.gson.Gson;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime;
import io.github.r3neer.scalebrews.collision.internal.MaterialIntervalRuntime;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** IMPLEMENTER probe for the actual 26.2 feature-enabled experimental minecart world. */
public final class S41ExperimentalMinecartClientProof implements FabricClientGameTest {
    private static final boolean EXPERIMENTAL=!"ordinary".equals(System.getenv("SCALEBREWS_S41_WORLD_VARIANT"));
    @Override public void runTest(ClientGameTestContext context) {
        var originalCow=originalCow();
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),
                Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var cartId=new AtomicInteger(-1);var cowId=new AtomicInteger(-1);var secondCowId=new AtomicInteger(-1);
        var observerCartId=new AtomicInteger(-1);
        var carried=new AtomicReference<Vec3>();var railStart=new AtomicReference<Vec3>();
        var reacquired=new AtomicReference<Vec3>();
        var observerStart=new AtomicReference<Vec3>();var observerTrack=new ArrayList<BlockPos>();
        try(var world=context.worldBuilder().adjustSettings(state->{
            if(!EXPERIMENTAL)return;
            var old=state.getSettings();
            var config=old.dataConfiguration().expandFeatures(FeatureFlagSet.of(FeatureFlags.MINECART_IMPROVEMENTS));
            state.setSettings(new WorldCreationContext(old.options(),old.datapackDimensions(),
                old.selectedDimensions(),old.worldgenRegistries(),old.dataPackResources(),
                config,old.initialWorldCreationOptions()));
        }).create()) {
          try {
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                if(level.enabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS)!=EXPERIMENTAL)
                    throw new AssertionError("Minecart feature flag differs from requested real server world variant");
                var player=server.getPlayerList().getPlayers().getFirst();
                var cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("Prepared cow factory failed");
                cow.setNoAi(true);cow.setNoGravity(true);
                cow.setPos(player.getX()+2,player.getY()+4,player.getZ());
                cow.getAttribute(Attributes.SCALE).setBaseValue(4);cow.refreshDimensions();
                if(!level.addFreshEntity(cow))throw new AssertionError("Prepared cow spawn rejected");
                cowId.set(cow.getId());
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",originalCow),
                    Map.of("scalebrews:s41_minecart",profile));
                Platforms.tick(level);
                var piece=AnatomyMovement.queryFrame(cow).orElseThrow().snapshot().pieces().get("root/body/cube_0");
                if(piece==null)throw new AssertionError("Original prepared cow lacks named body piece");
                var bounds=piece.bounds();
                var cart=EntityTypes.MINECART.create(level,EntitySpawnReason.COMMAND);
                if(cart==null)throw new AssertionError("Minecart factory failed");
                cart.setPos(bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z);
                if(EXPERIMENTAL ? !(cart.getBehavior() instanceof NewMinecartBehavior)
                        : !(cart.getBehavior() instanceof OldMinecartBehavior))
                    throw new AssertionError("Server minecart selected wrong native behavior: "
                        +cart.getBehavior().getClass().getName());
                if(!level.addFreshEntity(cart))throw new AssertionError("Server minecart spawn rejected");
                cartId.set(cart.getId());
                player.teleportTo(cart.getX(),cart.getY(),cart.getZ());
                if(!player.startRiding(cart,true,true))throw new AssertionError("Local rider mount failed");
                cart.move(MoverType.SELF,new Vec3(0,-1.5,0));
                var binding=AnatomyMovement.canonicalBinding(cow);
                var category=Platforms.category(cart);
                var rule=binding==null?null:io.github.r3neer.scalebrews.collision.integration.CollisionRules.resolve(
                    io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData.policy(Platforms.policy(level)),
                    binding.policy(),category,binding.entity());
                if(!Platforms.supported(cart) || cart.isOnRails() || AnatomyMovement.contact(cart)==null
                        || Platforms.state(cart).support!=null || cow.isPassenger())
                    throw new AssertionError("Experimental cart did not acquire canonical-only off-rail contact: "
                        +cart.position()+" contact="+AnatomyMovement.contact(cart)
                        +" supported="+Platforms.supported(cart)+" onRails="+cart.isOnRails()
                        +" legacy="+Platforms.state(cart).support
                        +" gap="+piece.separation(cart.getBoundingBox()).gap()
                        +" normal="+piece.separation(cart.getBoundingBox()).normal()
                        +" eligible="+Platforms.eligible(cart,cow)
                        +" ordinary="+Platforms.ordinary(cart)+"/"+Platforms.ordinary(cow)
                        +" cowState="+cow.isAlive()+"/"+cow.noPhysics+"/"+cow.isSpectator()
                        +"/"+cow.isPassenger()+"/"+cow.isSleeping()+"/"+cow.isFallFlying()
                        +"/"+cow.isSwimming()+"/"+cow.isBaby()
                        +" category="+category+" ratio="+(cart.getBbWidth()/cow.getBbWidth())
                        +" rule="+rule
                        +" suspended="+AnatomyMovement.suspended(cart,cow)
                        +" gravitySupports="+AnatomyMovement.gravity(cart).supports(piece.separation(cart.getBoundingBox()).normal())
                        +" bounds="+bounds+" box="+cart.getBoundingBox()
                        +" mode="+io.github.r3neer.scalebrews.collision.api.AnatomyApi.mode(cart));
                cart.setDeltaMovement(Vec3.ZERO);
            });
            context.waitFor(client->client.level!=null
                && client.level.enabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS)==EXPERIMENTAL
                && client.level.getEntity(cartId.get()) instanceof AbstractMinecart cart
                && (EXPERIMENTAL ? cart.getBehavior() instanceof NewMinecartBehavior
                    : cart.getBehavior() instanceof OldMinecartBehavior)
                && client.level.getEntity(cowId.get()) instanceof Cow
                && client.player!=null && client.player.getVehicle()==cart,160);
            context.runOnClient(client->{
                var cart=(AbstractMinecart)client.level.getEntity(cartId.get());
                if(cart==null || cart.isLocalInstanceAuthoritative() || cart.getControllingPassenger()!=null)
                    throw new AssertionError("Occupied native cart must be visible and server-authoritative");
            });
            if(!EXPERIMENTAL) {
                System.out.println("S41_ORDINARY_WORLD_CONTROL PASS default feature flag and OldMinecartBehavior on both peers");
                return;
            }
            context.waitTicks(6);
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                var cow=(Cow)level.getEntity(cowId.get());
                var cart=(AbstractMinecart)level.getEntity(cartId.get());
                if(cow==null || cart==null || !Platforms.supported(cart))
                    throw new AssertionError("Experimental cart lost material before ROOT");
                var before=cart.position();
                var prior=AnatomyMovement.transport(cart);
                long sequence=prior==null?0:prior.sequence();
                cow.move(MoverType.SELF,new Vec3(.2,0,.1));
                if(cart.position().distanceToSqr(before.add(.2,0,.1))>1e-6)
                    throw new AssertionError("Experimental cart did not receive exactly one ROOT carry: "
                        +before+" -> "+cart.position());
                var now=AnatomyMovement.transport(cart);
                if(now==null || now.sequence()!=sequence+1)
                    throw new AssertionError("Experimental cart transport sequence did not advance once");
                var surface=AnatomyMovement.surface(cart);
                var shapeBefore=AnatomyMovement.queryFrame(cow).orElseThrow().snapshot().pieces().get(surface.piece());
                var beforeYaw=cart.position();
                var capture=MaterialIntervalRuntime.captureRoot(cow);
                cow.yBodyRot=capture.root().yaw()+10;
                MaterialIntervalRuntime.commitRoot(cow,capture);
                var shapeAfter=AnatomyMovement.queryFrame(cow).orElseThrow().snapshot().pieces().get(surface.piece());
                var yawDelta=shapeAfter.point(surface.localPoint()).subtract(shapeBefore.point(surface.localPoint()));
                MaterialPhysicsRuntime.drain(level);
                var yawTransport=AnatomyMovement.transport(cart);
                if(cart.position().distanceToSqr(beforeYaw.add(yawDelta))>1e-8
                        || yawTransport==null || yawTransport.sequence()!=now.sequence()+1
                        || cart.getFirstPassenger()!=server.getPlayerList().getPlayers().getFirst())
                    throw new AssertionError("Experimental occupied cart did not follow one ROOT yaw interval: "
                        +beforeYaw+" -> "+cart.position()+" expected="+yawDelta);
                carried.set(cart.position());
            });
            context.waitFor(client->client.level!=null
                && client.level.getEntity(cartId.get()) instanceof AbstractMinecart cart
                && cart.position().distanceToSqr(carried.get())<.04
                && client.player!=null && client.player.getVehicle()==cart,100);
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                var cow=(Cow)level.getEntity(cowId.get());
                var cart=(AbstractMinecart)level.getEntity(cartId.get());
                int railX=(int)Math.floor(cart.getX()+.4),railY=(int)Math.floor(cart.getY());
                int railZ=(int)Math.floor(cart.getZ());
                for(int x=railX;x<railX+5;x++) {
                    var floor=new BlockPos(x,railY-1,railZ);
                    var rail=new BlockPos(x,railY,railZ);
                    if(!level.getBlockState(floor).isAir() || !level.getBlockState(rail).isAir())
                        throw new AssertionError("Experimental rail fixture must start in air: "+rail);
                    level.setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(rail,Blocks.RAIL.defaultBlockState());
                }
                var before=cart.position();
                cart.setDeltaMovement(new Vec3(.42,0,0));
                boolean entered=false;
                for(int tick=0;tick<32;tick++) {
                    cart.tick();
                    if(cart.isOnRails()) {entered=true;break;}
                }
                if(!entered || cart.getX()<=before.x+.2)
                    throw new AssertionError("Experimental native cart did not enter physical rail: "
                        +before+" -> "+cart.position()+" velocity="+cart.getDeltaMovement());
                if(AnatomyMovement.contact(cart)!=null || Platforms.supported(cart))
                    throw new AssertionError("Experimental rail entry retained material ownership");
                railStart.set(cart.position());
                var prior=AnatomyMovement.transport(cart);
                long sequence=prior==null?0:prior.sequence();
                cow.move(MoverType.SELF,new Vec3(.2,0,0));
                var after=AnatomyMovement.transport(cart);
                if(cart.position().distanceToSqr(railStart.get())>1e-8
                        || (after==null?0:after.sequence())!=sequence)
                    throw new AssertionError("Rail cart consumed a stale material ROOT after handoff");
                System.out.println("S41_RAIL_ENTRY server="+cart.position()+" from="+before
                    +" behavior="+cart.getBehavior().getClass().getSimpleName());
            });
            var observedLerpSteps=new AtomicInteger();
            context.waitFor(client->{
                if(client.level==null || !(client.level.getEntity(cartId.get()) instanceof AbstractMinecart cart))return false;
                if(cart.getBehavior() instanceof NewMinecartBehavior behavior)
                    observedLerpSteps.accumulateAndGet(behavior.lerpSteps.size()+behavior.currentLerpSteps.size(),Math::max);
                return cart.isOnRails() && cart.getX()>carried.get().x+.2
                    && client.player!=null && client.player.getVehicle()==cart;
            },100);
            if(observedLerpSteps.get()==0)
                throw new AssertionError("Experimental client never consumed native minecart interpolation steps");
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                var cart=(AbstractMinecart)level.getEntity(cartId.get());
                var first=(Cow)level.getEntity(cowId.get());
                if(cart==null || first==null || !cart.isOnRails())
                    throw new AssertionError("Experimental rail exit requires current physical track state");
                var rail=cart.getCurrentBlockPosOrRailBelow();
                int railX=rail.getX(),railZ=rail.getZ();
                var railPosition=cart.position();
                boolean left=false;
                for(int tick=0;tick<80;tick++) {
                    cart.tick();
                    if(!cart.isOnRails()) {left=true;break;}
                }
                if(!left || cart.getX()<railPosition.x+.3)
                    throw new AssertionError("Experimental cart did not leave physical rail: "
                        +railPosition+" -> "+cart.position()+" velocity="+cart.getDeltaMovement());
                if(AnatomyMovement.contact(cart)!=null)
                    throw new AssertionError("Cart revived old support immediately after rail exit");
                var second=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
                if(second==null)throw new AssertionError("Second prepared cow factory failed");
                second.setNoAi(true);second.setNoGravity(true);
                second.setPos(railX+6,first.getY()-.44,railZ+.5);
                second.getAttribute(Attributes.SCALE).setBaseValue(4);second.refreshDimensions();
                if(!level.addFreshEntity(second))throw new AssertionError("Second prepared cow spawn rejected");
                secondCowId.set(second.getId());
                Platforms.tick(level);
                if(AnatomyMovement.queryFrame(second).isEmpty())
                    throw new AssertionError("Second support lacks prepared canonical geometry");
                boolean attached=false;
                for(int tick=0;tick<24;tick++) {
                    var contact=AnatomyMovement.contact(cart);
                    if(contact!=null && contact.support()==second && Platforms.supported(cart)) {
                        attached=true;break;
                    }
                    cart.tick();
                }
                if(!attached)
                    throw new AssertionError("Experimental cart failed fresh off-rail contact: "
                        +cart.position()+" contact="+AnatomyMovement.contact(cart));
                var before=cart.position();
                var prior=AnatomyMovement.transport(cart);
                long sequence=prior==null?0:prior.sequence();
                second.move(MoverType.SELF,new Vec3(.2,0,0));
                var now=AnatomyMovement.transport(cart);
                if(cart.position().distanceToSqr(before.add(.2,0,0))>1e-6
                        || now==null || now.sequence()!=sequence+1
                        || AnatomyMovement.contact(cart)==null || AnatomyMovement.contact(cart).support()!=second)
                    throw new AssertionError("Fresh support failed exactly one later ROOT carry: "
                        +before+" -> "+cart.position());
                cart.setDeltaMovement(Vec3.ZERO);
                reacquired.set(cart.position());
                System.out.println("S41_RAIL_EXIT server="+cart.position()+" old="+railPosition
                    +" contact="+AnatomyMovement.contact(cart)+" transport="+now.sequence());
            });
            context.waitFor(client->client.level!=null
                && client.level.getEntity(secondCowId.get()) instanceof Cow
                && client.level.getEntity(cartId.get()) instanceof AbstractMinecart cart
                && cart.position().distanceToSqr(reacquired.get())<.09
                && client.player!=null && client.player.getVehicle()==cart,120);
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                var player=server.getPlayerList().getPlayers().getFirst();
                int trackX=(int)Math.floor(player.getX()+12),trackY=(int)Math.floor(player.getY()+5);
                int trackZ=(int)Math.floor(player.getZ()+8);
                for(int x=trackX;x<trackX+10;x++) {
                    var floor=new BlockPos(x,trackY-1,trackZ);
                    var rail=new BlockPos(x,trackY,trackZ);
                    if(!level.getBlockState(floor).isAir() || !level.getBlockState(rail).isAir())
                        throw new AssertionError("Observer track must start in air");
                    level.setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(rail,Blocks.RAIL.defaultBlockState());
                    observerTrack.add(floor);observerTrack.add(rail);
                }
                var cart=EntityTypes.MINECART.create(level,EntitySpawnReason.COMMAND);
                if(cart==null || !(cart.getBehavior() instanceof NewMinecartBehavior))
                    throw new AssertionError("Unoccupied observer control needs experimental cart");
                cart.setPos(trackX+.5,trackY+.1,trackZ+.5);
                if(!level.addFreshEntity(cart))throw new AssertionError("Observer cart spawn rejected");
                observerCartId.set(cart.getId());observerStart.set(cart.position());
                cart.setDeltaMovement(new Vec3(.55,0,0));
                for(int tick=0;tick<3;tick++)cart.tick();
                if(!cart.isOnRails() || cart.getFirstPassenger()!=null || cart.getX()<observerStart.get().x+.2
                        || AnatomyMovement.contact(cart)!=null)
                    throw new AssertionError("Unoccupied experimental cart lost native track control");
            });
            var observerSteps=new AtomicInteger();
            context.waitFor(client->{
                if(client.level==null || !(client.level.getEntity(observerCartId.get()) instanceof AbstractMinecart cart))return false;
                if(cart.getBehavior() instanceof NewMinecartBehavior behavior)
                    observerSteps.accumulateAndGet(behavior.lerpSteps.size()+behavior.currentLerpSteps.size(),Math::max);
                return !cart.isLocalInstanceAuthoritative() && cart.getFirstPassenger()==null
                    && AnatomyMovement.contact(cart)==null && cart.getX()>observerStart.get().x+.1;
            },100);
            if(observerSteps.get()==0)
                throw new AssertionError("Unoccupied observer cart lacked native interpolation steps");
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                var cart=level.getEntity(observerCartId.get());if(cart!=null)cart.discard();
                for(var block:observerTrack)level.setBlockAndUpdate(block,Blocks.AIR.defaultBlockState());
                observerTrack.clear();
            });
            world.getServer().runOnServer(server->{
                ratioControl(server.overworld());
                blockedCarryControl(server.overworld());
                AnatomyRuntime.stop(server);
                var disabled=new PlatformDefinition(Identifier.parse("minecraft:cow"),false,.6,
                    Optional.empty(),List.of(),Optional.of(new AnatomyDefinition(
                        Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),
                        AnatomyFilter.DEFAULT)));
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",originalCow),
                    Map.of("scalebrews:s41_policy_off",disabled));
                policyControl(server.overworld());
            });
            System.out.println("S41_EXPERIMENTAL_MATERIAL PASS real flag/behavior, occupied ROOT, physical rail release/reacquisition and native lerp steps="+observedLerpSteps.get()+"/observer="+observerSteps.get());
          } finally {
            world.getServer().runOnServer(AnatomyRuntime::stop);
          }
        }
    }

    private static void ratioControl(ServerLevel level) {
        var player=level.getServer().getPlayerList().getPlayers().getFirst();
        var cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
        if(cow==null)throw new AssertionError("Ratio control support absent");
        cow.setNoAi(true);cow.setNoGravity(true);
        cow.setPos(player.getX()+15,player.getY()+6,player.getZ()+5);
        cow.getAttribute(Attributes.SCALE).setBaseValue(.5);cow.refreshDimensions();
        if(!level.addFreshEntity(cow))throw new AssertionError("Ratio control support not inserted");
        AbstractMinecart cart=null;net.minecraft.world.entity.animal.pig.Pig rider=null;
        try {
            Platforms.tick(level);
            var piece=AnatomyMovement.queryFrame(cow).orElseThrow().snapshot().pieces().get("root/body/cube_0");
            if(piece==null)throw new AssertionError("Ratio control support lacks prepared body");
            var bounds=piece.bounds();
            cart=EntityTypes.MINECART.create(level,EntitySpawnReason.COMMAND);
            if(cart==null || !(cart.getBehavior() instanceof NewMinecartBehavior))
                throw new AssertionError("Ratio control needs real experimental cart");
            cart.setPos(bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z);
            if(!level.addFreshEntity(cart))throw new AssertionError("Ratio control cart not inserted");
            rider=EntityTypes.PIG.create(level,EntitySpawnReason.COMMAND);
            if(rider==null)throw new AssertionError("Ratio control rider absent");
            rider.setNoAi(true);rider.setPos(cart.position());
            if(!level.addFreshEntity(rider) || !rider.startRiding(cart,true,true))
                throw new AssertionError("Ratio control rider did not prevent support pickup");
            if(Platforms.eligible(cart,cow))
                throw new AssertionError("Narrow prepared cow wrongly accepts experimental cart");
            cart.move(MoverType.SELF,new Vec3(0,-1.5,0));
            if(AnatomyMovement.contact(cart)!=null || Platforms.state(cart).support!=null || cow.isPassenger())
                throw new AssertionError("Ineligible experimental cart acquired a material owner");
            cart.setPos(cow.getX()+.4,cow.getY(),cow.getZ());
            cart.setDeltaMovement(Vec3.ZERO);cow.setDeltaMovement(Vec3.ZERO);
            if(!cart.canCollideWith(cow))
                throw new AssertionError("Ineligible cart lost vanilla collision predicate");
            cart.push(cow);
            if(cart.getDeltaMovement().horizontalDistanceSqr()<=1e-8
                    && cow.getDeltaMovement().horizontalDistanceSqr()<=1e-8)
                throw new AssertionError("Ineligible cart lost vanilla push");
            System.out.println("S41_RATIO_CONTROL PASS rejected experimental cart keeps native collision and push");
        } finally {
            if(rider!=null)rider.discard();
            if(cart!=null)cart.discard();
            cow.discard();
        }
    }

    private static void blockedCarryControl(ServerLevel level) {
        var player=level.getServer().getPlayerList().getPlayers().getFirst();
        var cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
        if(cow==null)throw new AssertionError("Blocked control support absent");
        cow.setNoAi(true);cow.setNoGravity(true);
        cow.setPos(player.getX()+25,player.getY()+10,player.getZ()+10);
        cow.getAttribute(Attributes.SCALE).setBaseValue(4);cow.refreshDimensions();
        if(!level.addFreshEntity(cow))throw new AssertionError("Blocked support not inserted");
        AbstractMinecart cart=null;net.minecraft.world.entity.animal.pig.Pig rider=null;
        var wall=new ArrayList<BlockPos>();
        try {
            Platforms.tick(level);
            var piece=AnatomyMovement.queryFrame(cow).orElseThrow().snapshot().pieces().get("root/body/cube_0");
            if(piece==null)throw new AssertionError("Blocked control lacks prepared body");
            var bounds=piece.bounds();
            cart=EntityTypes.MINECART.create(level,EntitySpawnReason.COMMAND);
            if(cart==null || !(cart.getBehavior() instanceof NewMinecartBehavior))
                throw new AssertionError("Blocked control needs real experimental cart");
            cart.setPos(bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z);
            if(!level.addFreshEntity(cart))throw new AssertionError("Blocked cart not inserted");
            rider=EntityTypes.PIG.create(level,EntitySpawnReason.COMMAND);
            if(rider==null)throw new AssertionError("Blocked control rider absent");
            rider.setNoAi(true);rider.setPos(cart.position());
            if(!level.addFreshEntity(rider) || !rider.startRiding(cart,true,true))
                throw new AssertionError("Blocked control rider did not prevent support pickup");
            cart.move(MoverType.SELF,new Vec3(0,-1.5,0));
            if(!Platforms.supported(cart) || Platforms.state(cart).support!=null)
                throw new AssertionError("Blocked control did not acquire canonical contact");
            int wallX=(int)Math.ceil(cart.getBoundingBox().maxX+.05);
            int wallY=(int)Math.floor(cart.getBoundingBox().minY);
            for(int z=(int)Math.floor(cart.getBoundingBox().minZ);z<=(int)Math.floor(cart.getBoundingBox().maxZ);z++) {
                var block=new BlockPos(wallX,wallY,z);
                if(!level.getBlockState(block).isAir())
                    throw new AssertionError("Blocked control wall starts inside another block: "+block);
                wall.add(block);level.setBlockAndUpdate(block,Blocks.STONE.defaultBlockState());
            }
            var before=cart.position();var rootBefore=cow.position();
            cow.move(MoverType.SELF,new Vec3(1.2,0,0));
            if(cow.getX()<rootBefore.x+.2 || cart.getBoundingBox().maxX>wallX+1e-6
                    || cart.getX()>=before.x+1.2-1e-4)
                throw new AssertionError("Experimental material ROOT crossed a real wall: "
                    +"root="+rootBefore+"->"+cow.position()+" cart="+before+"->"+cart.position()
                    +" wallX="+wallX);
            var stopped=cart.position();
            for(var block:wall)level.setBlockAndUpdate(block,Blocks.AIR.defaultBlockState());
            wall.clear();MaterialPhysicsRuntime.drain(level);
            if(cart.position().distanceToSqr(stopped)>1e-12)
                throw new AssertionError("Rejected experimental carry replayed after wall removal");
            System.out.println("S41_BLOCKED_CONTROL PASS real wall clips ROOT without delayed carry debt");
        } finally {
            for(var block:wall)level.setBlockAndUpdate(block,Blocks.AIR.defaultBlockState());
            if(rider!=null)rider.discard();
            if(cart!=null)cart.discard();
            cow.discard();
        }
    }

    private static void policyControl(ServerLevel level) {
        var player=level.getServer().getPlayerList().getPlayers().getFirst();
        var cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
        if(cow==null)throw new AssertionError("Disabled-policy support absent");
        cow.setNoAi(true);cow.setNoGravity(true);
        cow.setPos(player.getX()+35,player.getY()+10,player.getZ()+10);
        cow.getAttribute(Attributes.SCALE).setBaseValue(4);cow.refreshDimensions();
        if(!level.addFreshEntity(cow))throw new AssertionError("Disabled-policy support not inserted");
        AbstractMinecart cart=null;net.minecraft.world.entity.animal.pig.Pig rider=null;
        try {
            cart=EntityTypes.MINECART.create(level,EntitySpawnReason.COMMAND);
            if(cart==null || !(cart.getBehavior() instanceof NewMinecartBehavior))
                throw new AssertionError("Disabled-policy control needs real experimental cart");
            cart.setPos(cow.getX(),cow.getBoundingBox().maxY+.8,cow.getZ());
            if(!level.addFreshEntity(cart))throw new AssertionError("Disabled-policy cart not inserted");
            rider=EntityTypes.PIG.create(level,EntitySpawnReason.COMMAND);
            if(rider==null)throw new AssertionError("Disabled-policy rider absent");
            rider.setNoAi(true);rider.setPos(cart.position());
            if(!level.addFreshEntity(rider) || !rider.startRiding(cart,true,true))
                throw new AssertionError("Disabled-policy rider did not prevent support pickup");
            if(Platforms.eligible(cart,cow))
                throw new AssertionError("Disabled-policy cow wrongly accepts experimental cart");
            cart.move(MoverType.SELF,new Vec3(0,-1.5,0));
            if(AnatomyMovement.contact(cart)!=null || Platforms.state(cart).support!=null)
                throw new AssertionError("Disabled-policy cart acquired material owner");
            cart.setPos(cow.getX()+.4,cow.getY(),cow.getZ());
            cart.setDeltaMovement(Vec3.ZERO);cow.setDeltaMovement(Vec3.ZERO);
            if(!cart.canCollideWith(cow))
                throw new AssertionError("Disabled-policy cart lost vanilla collision predicate");
            cart.push(cow);
            if(cart.getDeltaMovement().horizontalDistanceSqr()<=1e-8
                    && cow.getDeltaMovement().horizontalDistanceSqr()<=1e-8)
                throw new AssertionError("Disabled-policy cart lost vanilla push");
            System.out.println("S41_POLICY_CONTROL PASS disabled prepared cow retains native collision and push");
        } finally {
            if(rider!=null)rider.discard();
            if(cart!=null)cart.discard();
            cow.discard();
        }
    }

    private static ModelGeometry originalCow() {
        var value=System.getProperty("scalebrews.anatomyCatalog");
        if(value==null || value.isBlank())throw new IllegalStateException("S41 needs original isolated catalog");
        try {
            var cow=new Gson().fromJson(Files.readString(Path.of(value).resolve("minecraft_cow.json")),ModelGeometry.class);
            if(cow==null || cow.format()!=2 || !cow.source().equals("minecraft:cow"))
                throw new IllegalArgumentException("S41 needs format-2 original cow export");
            return cow;
        } catch(IOException error) {throw new IllegalStateException("S41 cannot read original cow export",error);}
    }
}
