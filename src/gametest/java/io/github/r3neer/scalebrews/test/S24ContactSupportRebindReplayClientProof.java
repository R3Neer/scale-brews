package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.AnatomyContactPayload;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyNetworking;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.internal.ModelGeometryProvider;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import io.github.r3neer.scalebrews.collision.pose.QuadrupedPose;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.phys.Vec3;

/**
 * G3.9 adversarial holdout: a contact created against support binding N but delayed in the network
 * must not become valid merely because the same support UUID/network id has already rebound to N+1.
 * A genuinely fresh contact certified by N+1 must still materialize afterwards.
 */
public final class S24ContactSupportRebindReplayClientProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla(
            "minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var cowId=new AtomicInteger(-1);var pigId=new AtomicInteger(-1);var pigUuid=new AtomicReference<UUID>();
        var oldFrame=new AtomicReference<GeometryProvider.PublishedFrame>();
        var oldContact=new AtomicReference<AnatomyContactPayload>();
        var world=context.worldBuilder().create();Throwable failure=null;
        try {
            world.getServer().runOnServer(server->boot(server,geometry,profile,cowId));
            context.waitFor(client->{
                if(client.level==null)return false;
                var entity=client.level.getEntity(cowId.get());
                return entity instanceof Cow cow && AnatomyClientNetworking.catalog().ready()
                    && AnatomyClientNetworking.presentationFrame(cow).isPresent();
            },160);

            // Build a real geometrically-valid contact while binding N is authoritative, but do not
            // deliver that contact to the client. It is the delayed packet under test.
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(cowId.get());
                if(cow==null)throw new AssertionError("Missing support before rebind replay fixture");
                var player=server.getPlayerList().getPlayers().getFirst();
                var frame=AnatomyMovement.publishedFrame(cow).orElseThrow(
                    ()->new AssertionError("Missing authoritative support frame before rebind"));
                oldFrame.set(frame);

                var pig=net.minecraft.world.entity.EntityTypes.PIG.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(pig==null)throw new AssertionError("Could not create support-rebind replay pig");
                pig.setNoAi(true);pig.setNoGravity(true);
                pig.getAttribute(Attributes.SCALE).setBaseValue(.2);pig.refreshDimensions();

                var evaluator=new ModelGeometryProvider(geometry,new QuadrupedPose(),AnatomyFilter.DEFAULT,AnatomyNetworking.revision(server));
                evaluator.pose(cow,new PoseProvider.Inputs(0,0,0,0,0,true));
                var box=evaluator.sample(cow).orElseThrow().pieces().get("root/body/cube_0");
                if(box==null)throw new AssertionError("Cow fixture lacks root/body/cube_0");
                int face=box.closestFace(new Vec3(0,1,0));
                var localPoint=box.facePoint(face,box.bounds().getCenter());
                var normal=box.faceNormal(face);
                pig.setPos(box.point(localPoint).add(normal.scale(.05)));
                server.overworld().addFreshEntity(pig);pigId.set(pig.getId());pigUuid.set(pig.getUUID());

                long bodyGeneration=AnatomyRuntime.trackingGeneration(player,pig);
                if(bodyGeneration<1)throw new AssertionError("Could not allocate body tracking generation for delayed contact");
                oldContact.set(new AnatomyContactPayload(
                    AnatomyNetworking.epoch(server),AnatomyNetworking.revision(server),server.overworld().dimension().identifier(),
                    pig.getId(),pig.getUUID(),bodyGeneration,10_000,server.overworld().getGameTime(),
                    cow.getId(),cow.getUUID(),frame.identity().bindingGeneration(),"root/body/cube_0",face,localPoint,normal));

                // Freeze server anatomy publication before any contact packet can be published naturally.
                // Vanilla entity spawn/connection remains alive so the client still receives the pig.
                AnatomyRuntime.stop(server);
            });

            context.waitFor(client->client.level!=null && client.level.getEntity(pigId.get()) instanceof Pig pig
                && pig.getUUID().equals(pigUuid.get()),120);
            context.runOnClient(client->{
                var pig=client.level.getEntity(pigId.get());
                if(AnatomyClientNetworking.presentationContact(pig,client.level.getGameTime()).isPresent())
                    throw new AssertionError("Delayed contact was materialized before the support rebind fixture ran");
            });

            long reboundBinding=oldFrame.get().identity().bindingGeneration()+1;
            var rebound=rebound(oldFrame.get(),oldFrame.get().endpoint().frameSerial()+1,reboundBinding);
            world.getServer().runOnServer(server->AnatomyNetworking.sendPose(
                server.getPlayerList().getPlayers().getFirst(),rebound,1));
            context.waitFor(client->{
                var cow=client.level==null?null:client.level.getEntity(cowId.get());
                return cow instanceof Cow living && AnatomyClientNetworking.presentationFrame(living)
                    .map(frame->frame.identity().bindingGeneration()==reboundBinding).orElse(false);
            },100);
            context.runOnClient(client->{
                var pig=client.level.getEntity(pigId.get());
                if(AnatomyClientNetworking.presentationContact(pig,client.level.getGameTime()).isPresent())
                    throw new AssertionError("Support rebind itself invented a contact before delayed packet arrival");
            });

            // The packet was valid when created, but belongs to binding N. Binding N+1 is now current.
            // A correct lifecycle fence must reject it rather than reinterpreting its local coordinates
            // against the new support authority.
            world.getServer().runOnServer(server->{
                long now=server.overworld().getGameTime();
                if(oldContact.get().tick()+100<now)
                    throw new AssertionError("Delayed contact aged beyond receiver TTL before rebind replay injection");
                ServerPlayNetworking.send(server.getPlayerList().getPlayers().getFirst(),oldContact.get());
            });
            context.waitTicks(5);
            context.runOnClient(client->{
                var pig=client.level.getEntity(pigId.get());
                if(AnatomyClientNetworking.presentationContact(pig,client.level.getGameTime()).isPresent())
                    throw new AssertionError("Pre-rebind contact became authoritative against support binding N+1");
            });

            // The fence must be selective, not a permanent quarantine. Publish the same physical
            // contact as a later body event, but certify it against support binding N+1. This packet
            // must become present, proving that rebind recovery still works after killing the stale N packet.
            world.getServer().runOnServer(server->{
                var old=oldContact.get();long now=server.overworld().getGameTime();
                var fresh=new AnatomyContactPayload(old.epoch(),old.revision(),old.dimension(),old.bodyId(),old.body(),
                    old.trackingGeneration(),old.sequence()+1,now,old.supportId(),old.support(),reboundBinding,
                    old.piece(),old.face(),old.localPoint(),old.normal());
                ServerPlayNetworking.send(server.getPlayerList().getPlayers().getFirst(),fresh);
            });
            context.waitFor(client->{
                if(client.level==null)return false;
                var pig=client.level.getEntity(pigId.get());
                return pig instanceof Pig && AnatomyClientNetworking.presentationContact(pig,client.level.getGameTime()).isPresent();
            },100);
            context.runOnClient(client->{
                var pig=client.level.getEntity(pigId.get());
                var contact=AnatomyClientNetworking.presentationContact(pig,client.level.getGameTime()).orElseThrow(
                    ()->new AssertionError("Fresh binding N+1 contact did not recover after stale N rejection"));
                var frame=AnatomyClientNetworking.presentationFrame(contact.support()).orElseThrow();
                if(frame.identity().bindingGeneration()!=reboundBinding)
                    throw new AssertionError("Recovered contact was not materialized against support binding N+1");
            });

            System.out.println("S24_CONTACT_REBIND_REPLAY PASS delayed N rejected and fresh N+1 contact accepted");
        } catch(Throwable error) {failure=error;throw error;}
        finally {
            try {world.getServer().runOnServer(AnatomyRuntime::stop);world.close();}
            catch(Throwable cleanup) {if(failure!=null)failure.addSuppressed(cleanup);else if(cleanup instanceof RuntimeException runtime)throw runtime;else throw new AssertionError("Contact rebind replay cleanup failed",cleanup);}
        }
    }

    private static void boot(MinecraftServer server,ModelGeometry geometry,PlatformDefinition profile,AtomicInteger cowId) {
        var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
        if(cow==null)throw new AssertionError("Could not create support-rebind replay cow");
        cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);server.overworld().addFreshEntity(cow);cowId.set(cow.getId());
        AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s24_contact_rebind_cow",profile));
    }

    private static GeometryProvider.PublishedFrame rebound(GeometryProvider.PublishedFrame prior,long serial,long bindingGeneration) {
        var old=prior.endpoint();var identity=prior.identity();var sample=old.sample();
        var root=new AnatomyMovement.RootFrame(old.root().sequence()+1,old.root().tick()+1,
            sample.origin(),sample.yaw(),sample.scale(),sample.gravity());
        var endpoint=new GeometryProvider.CausalEndpoint(serial,old.authorityTick()+1,old.jointSampleTick(),root,sample,GeometryProvider.Availability.AVAILABLE);
        var rebound=new GeometryProvider.GeometryIdentity(identity.dimension(),identity.support(),identity.entityId(),identity.epoch(),identity.revision(),
            identity.model(),identity.poseProvider(),bindingGeneration,identity.localRegistrationGeneration()+1);
        return new GeometryProvider.PublishedFrame(rebound,endpoint);
    }
}
