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
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.phys.Vec3;

/** Two genuine recipient windows; no contact, pose or generation injection. */
public final class S29AdversarialContactLiveReconnectProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(c->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var cowId=new AtomicInteger();var pigId=new AtomicInteger();var body=new AtomicReference<UUID>();
        var old=new AtomicReference<AnatomyContactPayload>();var serverGeneration=new AtomicLong();
        var chunk=new AtomicReference<net.minecraft.world.level.ChunkPos>();
        try(var server=context.worldBuilder().createServer()) {
            try {
                try(var connection=server.connect()) {
                    context.waitFor(c->c.level!=null && c.player!=null,260);
                    server.runOnServer(s->{var player=s.getPlayerList().getPlayers().getFirst();
                        var cow=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);
                        cow.setNoAi(true);cow.setNoGravity(true);cow.setPersistenceRequired();cow.setPos(player.getX()+2,player.getY(),player.getZ());
                        cow.setYRot(0);cow.yBodyRot=0;chunk.set(cow.chunkPosition());s.overworld().setChunkForced(chunk.get().x(),chunk.get().z(),true);
                        s.overworld().addFreshEntity(cow);cowId.set(cow.getId());
                        AnatomyRuntime.startPrepared(s,Map.of("minecraft:cow",geometry),Map.of("s29_adversary:contact",profile));
                    });
                    context.waitFor(c->c.level.getEntity(cowId.get()) instanceof Cow cow && AnatomyMovement.queryFrame(cow).isPresent(),260);
                    server.runOnServer(s->{var cow=(Cow)s.overworld().getEntity(cowId.get());var pig=EntityTypes.PIG.create(s.overworld(),EntitySpawnReason.COMMAND);
                        pig.setNoAi(true);pig.setNoGravity(true);pig.setPersistenceRequired();
                        pig.getAttribute(Attributes.SCALE).addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            Identifier.parse("s29_adversary:body_scale"),-.8,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));pig.refreshDimensions();
                        s.overworld().addFreshEntity(pig);pigId.set(pig.getId());body.set(pig.getUUID());land(cow,pig);
                    });
                    context.waitFor(c->c.level.getEntity(pigId.get()) instanceof Pig pig && AnatomyMovement.supported(pig)
                        && AnatomyClientNetworking.presentationContact(pig,c.level.getGameTime()).isPresent(),260);
                    context.runOnClient(c->{old.set(accepted(body.get()));S29AdversarialContactLifecycleProof.assertPassive((Pig)c.level.getEntity(pigId.get()),"live baseline");});
                }
                context.waitFor(c->c.level==null,260);
                server.runOnServer(s->{var cow=(Cow)s.overworld().getEntity(cowId.get());var pig=(Pig)s.overworld().getEntity(pigId.get());
                    AnatomyMovement.clear(pig);pig.setPos(cow.getX(),cow.getY()+.5,cow.getZ()+2);});
                try(var connection=server.connect()) {
                    context.waitFor(c->c.level!=null && c.level.getEntity(cowId.get()) instanceof Cow cow && c.level.getEntity(pigId.get()) instanceof Pig
                        && AnatomyMovement.queryFrame(cow).isPresent() && AnatomyClientNetworking.presentationFrame(cow).isPresent(),260);
                    server.runOnServer(s->{var pig=(Pig)s.overworld().getEntity(pigId.get());land((Cow)s.overworld().getEntity(cowId.get()),pig);
                        serverGeneration.set(AnatomyRuntime.trackingGeneration(s.getPlayerList().getPlayers().getFirst(),pig));});
                    context.waitFor(c->{var packet=accepted(body.get());return c.level.getEntity(pigId.get()) instanceof Pig pig && AnatomyMovement.supported(pig)
                        && AnatomyClientNetworking.presentationContact(pig,c.level.getGameTime()).isPresent() && packet!=null && packet.trackingGeneration()>old.get().trackingGeneration();},260);
                    context.runOnClient(c->{var packet=accepted(body.get());var previous=old.get();
                        if(!packet.epoch().equals(previous.epoch()) || packet.revision()!=previous.revision() || packet.trackingGeneration()!=serverGeneration.get())
                            throw new AssertionError("NFR-017/FR-080: genuine contact window did not preserve catalog and server authority");
                        S29AdversarialContactLifecycleProof.assertPassive((Pig)c.level.getEntity(pigId.get()),"live reconnect");
                        System.out.println("S29 CONTACT LIVE RECONNECT PASS server/client body generation="+previous.trackingGeneration()+" -> "+packet.trackingGeneration());
                    });
                }
            } finally {server.runOnServer(s->{AnatomyRuntime.stop(s);if(chunk.get()!=null)s.overworld().setChunkForced(chunk.get().x(),chunk.get().z(),false);});}
        }
    }
    private static void land(Cow cow,Pig pig){var box=AnatomyMovement.queryFrame(cow).orElseThrow().snapshot().pieces().get("root/body/cube_0").bounds();
        pig.setPos(box.getCenter().x,box.maxY+.1,box.getCenter().z);pig.move(MoverType.SELF,new Vec3(0,-.2,0));
        if(!AnatomyMovement.supported(pig))throw new AssertionError("Real solver did not establish contact");}
    @SuppressWarnings("unchecked") private static AnatomyContactPayload accepted(UUID body){return ((Map<UUID,AnatomyContactPayload>)S29AdversarialContactLifecycleProof.field("presentationContacts")).get(body);}
}
