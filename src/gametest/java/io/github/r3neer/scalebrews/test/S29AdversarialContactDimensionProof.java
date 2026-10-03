package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.*;
import java.util.concurrent.atomic.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Same-connection contact retirement with exact and unseen old-window replay variants. */
public final class S29AdversarialContactDimensionProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){
        var geometry=context.computeOnClient(c->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var cowId=new AtomicInteger();var pigId=new AtomicInteger();var packet=new AtomicReference<AnatomyContactPayload>();
        var frame=new AtomicReference<GeometryProvider.PublishedFrame>();var tracking=new AtomicLong();
        var world=context.worldBuilder().create();
        try{
            world.getServer().runOnServer(s->{var cow=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);if(cow==null)throw new AssertionError("Cow absent");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPersistenceRequired();cow.setPos(2,3,2);cow.setYRot(0);cow.yBodyRot=0;
                s.overworld().setChunkForced(0,0,true);s.overworld().addFreshEntity(cow);cowId.set(cow.getId());
                AnatomyRuntime.startPrepared(s,Map.of("minecraft:cow",geometry),Map.of("s29_adversary:dimension_contact",profile));
            });
            context.waitFor(c->c.level.getEntity(cowId.get()) instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow).isPresent()
                && AnatomyMovement.queryFrame(cow).isPresent(),260);
            world.getServer().runOnServer(s->{var cow=(Cow)s.overworld().getEntity(cowId.get());
                var b=AnatomyMovement.queryFrame(cow).orElseThrow().snapshot().pieces().get("root/body/cube_0").bounds();
                var pig=EntityTypes.PIG.create(s.overworld(),EntitySpawnReason.COMMAND);if(pig==null)throw new AssertionError("Pig absent");
                pig.setNoAi(true);pig.setNoGravity(true);pig.setPersistenceRequired();pig.getAttribute(Attributes.SCALE).addPermanentModifier(
                    new AttributeModifier(Identifier.parse("s29_adversary:body_scale"),-.8,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));pig.refreshDimensions();
                pig.setPos(b.getCenter().x,b.maxY+.1,b.getCenter().z);s.overworld().addFreshEntity(pig);pigId.set(pig.getId());pig.move(MoverType.SELF,new Vec3(0,-.2,0));
                if(!AnatomyMovement.supported(pig))throw new AssertionError("Real contact acquisition failed");
            });
            context.waitFor(c->{var pig=c.level.getEntity(pigId.get());return pig instanceof Pig && AnatomyMovement.supported(pig)
                && AnatomyClientNetworking.presentationContact(pig,c.level.getGameTime()).isPresent();},260);
            context.runOnClient(c->{var pig=(Pig)c.level.getEntity(pigId.get());
                @SuppressWarnings("unchecked") var contacts=(Map<UUID,AnatomyContactPayload>)S29AdversarialContactLifecycleProof.field("presentationContacts");
                packet.set(contacts.get(pig.getUUID()));S29AdversarialContactLifecycleProof.assertPassive(pig,"dimension baseline");
                S29AdversarialContactLifecycleProof.snapshot(c,(Cow)c.level.getEntity(cowId.get()),pig,packet.get(),"dim-baseline");
            });
            world.getServer().runOnServer(s->{var cow=(Cow)s.overworld().getEntity(cowId.get());var pig=(Pig)s.overworld().getEntity(pigId.get());
                frame.set(AnatomyMovement.publishedFrame(cow).orElseThrow());tracking.set(AnatomyRuntime.trackingGeneration(s.getPlayerList().getPlayers().getFirst(),cow));
                s.tickRateManager().setFrozen(true);AnatomyRuntime.stop(s);AnatomyMovement.clear(pig);pig.setPos(2,3.5,4);
                teleport(s,Level.NETHER,new Vec3(0,80,0));
            });
            context.waitFor(c->c.level!=null && c.level.dimension()==Level.NETHER,260);context.waitTicks(2);
            context.runOnClient(c->{if(!AnatomyClientNetworking.catalog().ready())throw new AssertionError("Dimension switch discarded catalog");});
            world.getServer().runOnServer(s->teleport(s,Level.OVERWORLD,new Vec3(2,4,2)));
            context.waitFor(c->c.level!=null && c.level.dimension()==Level.OVERWORLD && c.level.getEntity(cowId.get()) instanceof Cow
                && c.level.getEntity(pigId.get()) instanceof Pig,260);
            world.getServer().runOnServer(s->{if(s.overworld().getGameTime()-packet.get().tick()>100)throw new AssertionError("Inconclusive: old contact outside TTL");
                AnatomyNetworking.sendPose(s.getPlayerList().getPlayers().getFirst(),frame.get(),AnatomyRuntime.nextTrackingGeneration(tracking.get()));});
            context.waitFor(c->c.level.getEntity(cowId.get()) instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow).isPresent()
                && AnatomyMovement.queryFrame(cow).isPresent(),260);
            context.runOnClient(c->{var pig=(Pig)c.level.getEntity(pigId.get());var cow=(Cow)c.level.getEntity(cowId.get());pig.refreshDimensions();
                if(!io.github.r3neer.scalebrews.platform.Platforms.eligible(pig,cow))throw new AssertionError("Inconclusive: returning body ineligible");
                if(AnatomyClientNetworking.presentationContact(pig,c.level.getGameTime()).isPresent())throw new AssertionError("Contact survived before replay");
                S29AdversarialContactLifecycleProof.snapshot(c,cow,pig,packet.get(),"dim-before-replay");
            });
            var old=packet.get();var replay=Boolean.getBoolean("scalebrews.s29ContactDelayedSequence")?new AnatomyContactPayload(
                old.epoch(),old.revision(),old.dimension(),old.bodyId(),old.body(),old.trackingGeneration(),old.sequence()+1,old.tick(),
                old.supportId(),old.support(),old.supportBindingGeneration(),old.piece(),old.face(),old.localPoint(),old.normal()):old;
            world.getServer().runOnServer(s->ServerPlayNetworking.send(s.getPlayerList().getPlayers().getFirst(),replay));context.waitTicks(3);
            context.runOnClient(c->{var pig=(Pig)c.level.getEntity(pigId.get());boolean revived=AnatomyClientNetworking.presentationContact(pig,c.level.getGameTime()).isPresent();
                S29AdversarialContactLifecycleProof.snapshot(c,(Cow)c.level.getEntity(cowId.get()),pig,old,"dim-after-replay");
                System.out.println("S29 dimension contact replay authoritative="+revived+" delayedSequence="+Boolean.getBoolean("scalebrews.s29ContactDelayedSequence"));
                if(revived || AnatomyMovement.supported(pig))throw new AssertionError("NFR-017/FR-080: pre-dimension contact revived under fresh support window");
            });
            world.getServer().runOnServer(s->ServerPlayNetworking.send(s.getPlayerList().getPlayers().getFirst(),new AnatomyContactPayload(
                old.epoch(),old.revision(),old.dimension(),old.bodyId(),old.body(),AnatomyRuntime.nextTrackingGeneration(old.trackingGeneration()),old.sequence()+1,old.tick(),
                old.supportId(),old.support(),old.supportBindingGeneration(),old.piece(),old.face(),old.localPoint(),old.normal())));
            context.waitFor(c->AnatomyClientNetworking.presentationContact(c.level.getEntity(pigId.get()),c.level.getGameTime()).isPresent(),100);
            System.out.println("S29 CONTACT DIMENSION PASS retired contact rejected, fresh contact control materialized");
        }finally{world.getServer().runOnServer(s->{s.tickRateManager().setFrozen(false);AnatomyRuntime.stop(s);s.overworld().setChunkForced(0,0,false);});context.waitTicks(2);world.close();}
    }
    private static void teleport(net.minecraft.server.MinecraftServer s,net.minecraft.resources.ResourceKey<Level> key,Vec3 pos){
        if(s.getPlayerList().getPlayers().getFirst().teleport(new TeleportTransition(s.getLevel(key),pos,Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING))==null)
            throw new AssertionError("Teleport failed");
    }
}
