package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyNetworking;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import org.joml.Matrix4f;

/** A new tracking window must not alias an old queued render token with reused local serials. */
public final class S30AdversarialTrackingAliasProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of())
            .withModelTransform(new Matrix4f().scaling(-1,-1,1).translate(0,-1.501f,0)));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var saved=new java.util.concurrent.atomic.AtomicReference<io.github.r3neer.scalebrews.collision.internal.GeometryProvider.PublishedFrame>(); var id=new AtomicInteger();var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(server->{
                var cow=EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("No tracking alias fixture Cow");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);server.overworld().addFreshEntity(cow);id.set(cow.getId());
                server.getPlayerList().getPlayers().getFirst().teleportTo(2,3,-2);
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s30_alias",profile));
            });
            context.waitTicks(5);
            world.getServer().runOnServer(server->{var cow=(Cow)server.overworld().getEntity(id.get());var player=server.getPlayerList().getPlayers().getFirst();
                saved.set(AnatomyMovement.publishedFrame(cow).orElseThrow());long generation=AnatomyRuntime.trackingGeneration(player,cow);AnatomyRuntime.stop(server);AnatomyNetworking.sendPose(player,saved.get(),generation);});
            context.waitFor(client->client.level!=null && client.level.getEntity(id.get()) instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow).isPresent(),120);
            var oldFrame=context.computeOnClient(client->AnatomyClientNetworking.presentationFrame((Cow)client.level.getEntity(id.get())).orElseThrow());
            var oldEntity=context.computeOnClient(client->(Cow)client.level.getEntity(id.get()));
            long oldTracking=context.computeOnClient(client->AnatomyClientNetworking.pose(oldEntity.getUUID()).current().trackingGeneration());
            var queued=context.computeOnClient(client->{var submit=S30AdversarialRendererDrawProof.submit(client,oldEntity);
                S30AdversarialRendererDrawProof.assertPositive(submit,"before tracking alias barrier");return submit;});
            world.getServer().runOnServer(server->AnatomyNetworking.sendPose(server.getPlayerList().getPlayers().getFirst(),saved.get(),AnatomyRuntime.nextTrackingGeneration(oldTracking)));
            context.waitFor(client->client.level.getEntity(id.get()) instanceof Cow cow && AnatomyClientNetworking.pose(cow.getUUID())!=null
                && AnatomyClientNetworking.pose(cow.getUUID()).current().trackingGeneration()>oldTracking && AnatomyClientNetworking.presentationFrame(cow).isPresent(),120);
            context.runOnClient(client->{
                var fresh=(Cow)client.level.getEntity(id.get());var frame=AnatomyClientNetworking.presentationFrame(fresh).orElseThrow();
                long tracking=AnatomyClientNetworking.pose(fresh.getUUID()).current().trackingGeneration();
                if(fresh!=oldEntity || oldEntity.isRemoved() || !frame.identity().equals(oldFrame.identity())
                    || frame.after().frameSerial()!=oldFrame.after().frameSerial())
                    throw new AssertionError("Tracking alias fixture premises: sameEntity="+(fresh==oldEntity)+" removed="+oldEntity.isRemoved()+" oldIdentity="+oldFrame.identity()+" newIdentity="+frame.identity()+" oldSerial="+oldFrame.after().frameSerial()+" newSerial="+frame.after().frameSerial()+" deltaSquared="+frame.evaluated().origin().distanceToSqr(oldFrame.evaluated().origin()));
                System.out.println("S30 ADVERSARY tracking alias oldGeneration="+oldTracking+" newGeneration="+tracking
                    +" sameIdentity=true sameFrameSerial="+frame.after().frameSerial()+" sameLiveClientEntity=true sameAcceptedEndpoint=true");
                S30AdversarialRendererDrawProof.assertNoVertices(queued,"fresh tracking generation with reused identity/frame serial");
                S30AdversarialRendererDrawProof.assertPositive(S30AdversarialRendererDrawProof.submit(client,fresh),"fresh tracking alias window");
            });
        } finally {world.getServer().runOnServer(AnatomyRuntime::stop);context.waitTicks(2);world.close();}
    }
}
