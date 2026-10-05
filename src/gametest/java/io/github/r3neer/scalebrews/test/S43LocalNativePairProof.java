package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.ModelPartGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.AnatomyMode;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.*;
import java.util.concurrent.atomic.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** IMPLEMENTER: native differential on the actual connected local prediction owner. */
public final class S43LocalNativePairProof implements FabricClientGameTest {
    private record Move(Vec3 delta,Vec3 velocity,boolean horizontal,boolean vertical,boolean ground,boolean pair) {}
    private record Push(Vec3 body,Vec3 support) {}
    private record Pushes(Push supportCaller,Push bodyCaller) {}
    private static final Vec3 ROOT=new Vec3(.25,0,0);

    @Override public void runTest(ClientGameTestContext context) {
        var model=context.computeOnClient(client->ModelPartGeometryEngine.inspectVisible(
            Identifier.parse("minecraft:cow"),CowModel.createBodyLayer().bakeRoot(),
            new Matrix4f().scaling(-1,-1,1).translate(0,-1.501f,0)));
        var models=Map.of(model.source(),model);
        var cowId=new AtomicInteger(-1);var ratio=new AtomicReference<Double>();
        var bounds=new AtomicReference<AABB>();var top=new AtomicReference<AABB>();
        try(var world=context.worldBuilder().create()) {
            try {
                world.getServer().runOnServer(server->{
                    var level=server.overworld();var cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
                    require(cow!=null,"Original cow fixture absent");
                    cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,20,2);cow.yBodyRot=0;
                    cow.getAttribute(Attributes.SCALE).setBaseValue(6);cow.refreshDimensions();
                    require(level.addFreshEntity(cow),"Original cow fixture must enter the real world");cowId.set(cow.getId());
                    var player=server.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);
                    player.getAttribute(Attributes.SCALE).setBaseValue(.25);player.refreshDimensions();
                    ratio.set(player.getBbWidth()/(double)cow.getBbWidth());
                    var pieces=model.evaluate(new Matrix4f().rotationY((float)Math.PI).scale(cow.getScale())
                        .mul(ModelGeometry.matrix(model.modelTransform())),Map.of(),AnatomyFilter.DEFAULT);
                    bounds.set(pieces.get("root/body/cube_0").move(cow.position()).bounds());
                    top.set(pieces.values().stream().map(piece->piece.move(cow.position()).bounds())
                        .max(Comparator.comparingDouble(box->box.maxY)).orElseThrow());
                    require(top.get().maxY>cow.getBoundingBox().maxY+.01,
                        "Carry fixture needs an original clear top face beyond the native overlap region");
                });
                context.waitFor(client->client.player!=null && client.level!=null
                    && client.level.getEntity(cowId.get()) instanceof Cow cow && Math.abs(cow.getScale()-6)<1e-5
                    && Math.abs(client.player.getScale()-.25)<1e-5 && client.player.isNoGravity()
                    && !client.player.getAbilities().flying,180);
                var start=context.computeOnClient(client->start(client.player,bounds.get(),client.level.getEntity(cowId.get())));
                world.getServer().runOnServer(server->server.getPlayerList().getPlayers().getFirst().teleportTo(start.x,start.y,start.z));
                awaitPosition(context,start);
                var nativeMove=context.computeOnClient(client->{
                    require(AnatomyApi.mode(client.player)==AnatomyMode.DISABLED,"Native local control must precede activation");
                    return move(client.player,(Cow)client.level.getEntity(cowId.get()),bounds.get());
                });
                var roof=new Vec3(top.get().getCenter().x+.25,top.get().maxY,top.get().getCenter().z);
                world.getServer().runOnServer(server->server.getPlayerList().getPlayers().getFirst().teleportTo(roof.x,roof.y,roof.z));
                awaitPosition(context,roof);
                var nativePush=context.computeOnClient(client->pushes(client.player,(Cow)client.level.getEntity(cowId.get())));
                var nativeServerPush=world.getServer().computeOnServer(server->pushes(
                    server.getPlayerList().getPlayers().getFirst(),(Cow)server.overworld().getEntity(cowId.get())));
                require(nonzero(nativePush) && nonzero(nativeServerPush),"Both native client/server controls must produce an actual impulse");

                world.getServer().runOnServer(server->AnatomyRuntime.startPrepared(server,models,
                    Map.of("test:s43",profile(Math.nextDown(ratio.get())))));
                context.waitFor(client->AnatomyApi.ready(client.player)
                    && AnatomyMovement.queryFrame((Cow)client.level.getEntity(cowId.get())).isPresent(),180);
                world.getServer().runOnServer(server->server.getPlayerList().getPlayers().getFirst().teleportTo(start.x,start.y,start.z));
                awaitPosition(context,start);
                var rejected=context.computeOnClient(client->{
                    var cow=(Cow)client.level.getEntity(cowId.get());
                    require(!Platforms.eligible(client.player,cow),"Measured local pair must be rejected");
                    var result=move(client.player,cow,bounds.get());
                    require(AnatomyMovement.contact(client.player)==null && TestRetiredPlatformRuntime.absent(),
                        "Rejected local native move acquired a material/legacy contact");
                    var center=bounds.get().getCenter();
                    require(AnatomyApi.raycast(client.player,center.add(-5,0,0),center.add(5,0,0)).isEmpty()
                        && AnatomyApi.spaceClear(client.player,client.player.getBoundingBox().move(center.subtract(client.player.position()))),
                        "Rejected local pair added an anatomical query wall");
                    return result;
                });
                require(equal(nativeMove,rejected),"Rejected local native move changed: native="+nativeMove+" prepared="+rejected);

                long oldRevision=context.computeOnClient(client->AnatomyClientNetworking.catalog().revision());
                world.getServer().runOnServer(server->S43TestCatalog.reload(server,models,Map.of("test:s43",profile(Math.nextUp(ratio.get())))));
                context.waitFor(client->AnatomyClientNetworking.catalog().ready()
                    && AnatomyClientNetworking.catalog().revision()>oldRevision
                    && AnatomyMovement.queryFrame((Cow)client.level.getEntity(cowId.get())).isPresent(),180);
                world.getServer().runOnServer(server->server.getPlayerList().getPlayers().getFirst().teleportTo(start.x,start.y,start.z));
                awaitPosition(context,start);
                context.runOnClient(client->{
                    var cow=(Cow)client.level.getEntity(cowId.get());
                    require(Platforms.eligible(client.player,cow) && AnatomyMovement.predictsBody(client.player),
                        "Actual local prediction owner must be eligible");
                    var accepted=move(client.player,cow,bounds.get());
                    require(accepted.delta().x<nativeMove.delta().x-.05,"Prepared positive did not block actual local Entity.move");
                });
                var above=roof.add(0,.8,0);
                world.getServer().runOnServer(server->server.getPlayerList().getPlayers().getFirst().teleportTo(above.x,above.y,above.z));
                awaitPosition(context,above);
                context.runOnClient(client->{
                    client.player.setDeltaMovement(Vec3.ZERO);client.player.move(MoverType.SELF,new Vec3(0,-1.2,0));
                    require(AnatomyMovement.supported(client.player) && TestRetiredPlatformRuntime.absent(),
                        "Actual local Entity.move must acquire canonical-only contact");
                    compare(nativePush,pushes(client.player,(Cow)client.level.getEntity(cowId.get())),"local contact");
                });
                world.getServer().runOnServer(server->{
                    var player=server.getPlayerList().getPlayers().getFirst();
                    var cow=(Cow)server.overworld().getEntity(cowId.get());
                    // The native client position update establishes the same landing; this native
                    // downward move certifies the server's own material relation if it is still above.
                    player.setDeltaMovement(Vec3.ZERO);player.move(MoverType.SELF,new Vec3(0,-1.2,0));
                    require(AnatomyMovement.supported(player) && TestRetiredPlatformRuntime.absent(),
                        "Connected server actor did not acquire canonical-only contact");
                    compare(nativeServerPush,pushes(player,cow),"server contact");
                });
                context.waitFor(client->AnatomyMovement.supported(client.player),120);
                var before=context.computeOnClient(client->client.player.position());
                var serverAfter=world.getServer().computeOnServer(server->{
                    var player=server.getPlayerList().getPlayers().getFirst();var cow=(Cow)server.overworld().getEntity(cowId.get());
                    var prior=player.position();cow.move(MoverType.SELF,ROOT);
                    require(player.position().distanceToSqr(prior.add(ROOT))<1e-8 && AnatomyMovement.supported(player),
                        "Connected server actor did not receive exactly one real ROOT carry: "+prior+" -> "+player.position());
                    var once=player.position();MaterialPhysicsRuntime.drain(server.overworld());
                    require(player.position().equals(once),"Repeated server drain duplicated ROOT");
                    compare(nativeServerPush,pushes(player,cow),"server after carry");return player.position();
                });
                var applied=new AtomicReference<Vec3>();
                context.waitFor(client->{
                    var transport=AnatomyMovement.transport(client.player);
                    if(transport==null || transport.appliedDelta().distanceToSqr(ROOT)>1e-8)return false;
                    applied.set(transport.appliedDelta());return true;
                },120);
                context.runOnClient(client->{
                    require(AnatomyMovement.supported(client.player) && client.player.position().distanceToSqr(before.add(ROOT))<1e-6,
                        "Observed local ROOT receipt differs from actual local displacement: "+before+" -> "+client.player.position());
                });
                // Material endpoints can arrive before vanilla finishes replica interpolation.
                // Match the native control's relative coordinates before comparing its impulse.
                context.waitFor(client->client.level.getEntity(cowId.get()).position()
                    .distanceToSqr(new Vec3(2,20,2).add(ROOT))<1e-10,120);
                context.runOnClient(client->compare(nativePush,
                    pushes(client.player,(Cow)client.level.getEntity(cowId.get())),"local after carry"));
                context.waitTicks(4);
                context.runOnClient(client->require(client.player.position().distanceToSqr(serverAfter)<1e-6
                    && client.player.position().distanceToSqr(before.add(ROOT))<1e-6 && AnatomyMovement.supported(client.player),
                    "Actual client/server position must agree after one ROOT, without a second carry: server="+serverAfter
                        +" local="+client.player.position()));
                System.out.println("S43_LOCAL_NATIVE_PAIR_PASS rejectedMove="+rejected+" nativePush="+nativePush
                    +" serverNativePush="+nativeServerPush+" actualLocalCarry="+applied.get()+" serverAfter="+serverAfter);
            } finally {world.getServer().runOnServer(AnatomyRuntime::stop);}
        }
    }

    private static PlatformDefinition profile(double ratio) {
        return new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.of(ratio),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:static"),AnatomyFilter.DEFAULT)));
    }
    private static Vec3 start(Entity body,AABB bounds,Entity support) {
        return new Vec3(bounds.minX-body.getBbWidth()/2-.6,bounds.getCenter().y,support.getZ());
    }
    private static void awaitPosition(ClientGameTestContext context,Vec3 position) {
        context.waitFor(client->client.player!=null && client.player.position().distanceToSqr(position)<1e-8,160);
    }
    private static Move move(Entity body,Cow support,AABB bounds) {
        AnatomyApi.clearContact(body);body.setDeltaMovement(Vec3.ZERO);body.setOnGround(false);
        var before=body.position();body.move(MoverType.SELF,new Vec3(bounds.getXsize()+body.getBbWidth()+1.2,0,0));
        return new Move(body.position().subtract(before),body.getDeltaMovement(),body.horizontalCollision,
            body.verticalCollision,body.onGround(),body.canCollideWith(support));
    }
    private static Push push(Entity body,Cow support,boolean supportCaller) {
        body.setDeltaMovement(Vec3.ZERO);support.setDeltaMovement(Vec3.ZERO);
        if(supportCaller)support.push(body);else body.push(support);
        var result=new Push(body.getDeltaMovement(),support.getDeltaMovement());
        body.setDeltaMovement(Vec3.ZERO);support.setDeltaMovement(Vec3.ZERO);return result;
    }
    private static Pushes pushes(Entity body,Cow support) {return new Pushes(push(body,support,true),push(body,support,false));}
    private static boolean nonzero(Pushes value) {return value.supportCaller().body().lengthSqr()>1e-12
        && value.supportCaller().support().lengthSqr()>1e-12 && value.bodyCaller().body().lengthSqr()>1e-12
        && value.bodyCaller().support().lengthSqr()>1e-12;}
    private static void compare(Pushes nativeValue,Pushes actual,String phase) {
        require(nativeValue.supportCaller().body().distanceToSqr(actual.supportCaller().body())<1e-12
            && nativeValue.supportCaller().support().distanceToSqr(actual.supportCaller().support())<1e-12
            && nativeValue.bodyCaller().body().distanceToSqr(actual.bodyCaller().body())<1e-12
            && nativeValue.bodyCaller().support().distanceToSqr(actual.bodyCaller().support())<1e-12,
            "Native impulse changed at "+phase+": native="+nativeValue+" actual="+actual);
    }
    private static boolean equal(Move a,Move b) {return a.delta().distanceToSqr(b.delta())<1e-12
        && a.velocity().distanceToSqr(b.velocity())<1e-12 && a.horizontal()==b.horizontal()
        && a.vertical()==b.vertical() && a.ground()==b.ground() && a.pair()==b.pair();}
    private static void require(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
}
