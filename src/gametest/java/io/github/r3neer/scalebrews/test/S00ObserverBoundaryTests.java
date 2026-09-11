package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.*;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import io.github.r3neer.scalebrews.platform.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.*;
import static io.github.r3neer.scalebrews.test.S00Fixtures.*;

/** Real replica ownership with explicit geometric fixtures; not a packet/reconciliation proof. */
public final class S00ObserverBoundaryTests implements FabricClientGameTest {
    private static final Vec3 UP=new Vec3(0,1,0),POINT=new Vec3(.5,1,.5),DELTA=new Vec3(.125,0,0);
    private static final class Provider implements GeometryProvider {
        final LivingEntity support;
        final double height;
        Vec3 shift=Vec3.ZERO,previous;
        long serial;
        CausalEndpoint endpoint;
        Snapshot snapshot;
        Provider(LivingEntity support,double height){this.support=support;this.height=height;}
        private void update() {
            var origin=support.position().add(shift);
            if(origin.equals(previous))return;
            previous=origin;long tick=support.level().getGameTime();serial++;
            var root=new AnatomyMovement.RootFrame(serial,tick,origin,0,support.getScale(),GravityFrame.VANILLA);
            var inputs=new PoseProvider.Inputs(0,0,0,0,0,true);
            endpoint=new CausalEndpoint(serial,tick,tick,root,new AnatomyPoseHistory.Sample(inputs,origin,0,support.getScale(),GravityFrame.VANILLA),Availability.AVAILABLE);
            snapshot=new Snapshot(1,Map.of("piece",box(new AABB(-1,0,-1,1,height,1)).move(origin)));
        }
        public Optional<CausalEndpoint> causalEndpoint(LivingEntity ignored){update();return Optional.of(endpoint);}
        public Optional<Snapshot> sample(LivingEntity ignored){update();return Optional.of(snapshot);}
    }
    @Override public void runTest(ClientGameTestContext context) {
        var world=context.worldBuilder().create();var bodyId=new AtomicInteger();var baseId=new AtomicInteger();
        try {
            world.getServer().runOnServer(server->{
                var base=EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                var body=EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                check(base!=null && body!=null,"Could not create real observer entities");
                base.setNoAi(true);base.setNoGravity(true);base.getAttribute(Attributes.SCALE).setBaseValue(2);base.refreshDimensions();base.setPos(3,20,3);
                body.setNoAi(true);body.setNoGravity(true);body.setPos(3,23.5,3);
                server.overworld().addFreshEntity(base);server.overworld().addFreshEntity(body);baseId.set(base.getId());bodyId.set(body.getId());
            });
            context.waitFor(client->client.level!=null && client.player!=null
                && client.level.getEntity(bodyId.get()) instanceof LivingEntity
                && client.level.getEntity(baseId.get()) instanceof LivingEntity base && base.getScale()==2,100);
            context.runOnClient(client->{
                var level=client.level;var base=(LivingEntity)level.getEntity(baseId.get());var body=(LivingEntity)level.getEntity(bodyId.get());
                check(!AnatomyMovement.simulates(body) && AnatomyMovement.simulates(client.player),"Fixture does not distinguish owner from remote observer");
                var problems=new ArrayList<String>();var oldPlayer=client.player.position();var initialBody=body.position();
                AnatomyMovement.activate(level);
                Platforms.anatomicalDefinitions(level,List.of(new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.of(2.0),List.of(),
                    Optional.of(new AnatomyDefinition(MODEL,STATIC,AnatomyFilter.DEFAULT)))));
                try {
                    for(int scenario=0;scenario<2;scenario++) {
                        AnatomyMovement.clear(body);AnatomyMovement.clear(client.player);body.setPos(initialBody);
                        var baseProvider=new Provider(base,3.5);var bodyProvider=new Provider(body,2);
                        var descriptor=new GeometryProvider.GeometryIdentityDescriptor(EPOCH,1,MODEL,STATIC,scenario+1);
                        AnatomyMovement.register(base,baseProvider,descriptor);AnatomyMovement.register(body,bodyProvider,descriptor);
                        var surface=new SurfaceContact(base.getUUID(),1,"piece",3,POINT,UP,level.getGameTime());
                        check(Platforms.eligible(body,base),"Observer fixture is ineligible: body="+body.position()+" base="+base.position()+" ratio="+(body.getBbWidth()/base.getBbWidth()));
                        var diagnosticFrame=AnatomyMovement.queryFrame(base).orElseThrow(()->new AssertionError("Observer fixture has no current base frame"));
                        var diagnosticPiece=diagnosticFrame.snapshot().pieces().get("piece");
                        check(diagnosticPiece!=null,"Observer fixture base frame has no piece");
                        var diagnosticSeparation=diagnosticPiece.separation(body.getBoundingBox());
                        check(Math.abs(diagnosticSeparation.gap())<=.025 && GravityFrame.VANILLA.supports(diagnosticSeparation.normal()),
                            "Observer fixture is not physically on the face: body="+body.position()+" box="+body.getBoundingBox()+" base="+base.position()+" piece="+diagnosticPiece.bounds()+" gap="+diagnosticSeparation.gap()+" normal="+diagnosticSeparation.normal());
                        check(AnatomyMovement.confirm(body,base,surface),"Observer fixture contact confirmation failed after eligibility/geometry checks");
                        check(AnatomyMovement.supported(body),"Observer fixture lost support immediately after confirmation");
                        if(scenario==1) {
                            client.player.getAbilities().flying=false;client.player.setPos(body.position().add(0,2,0));
                            var top=new SurfaceContact(body.getUUID(),1,"piece",3,POINT,UP,level.getGameTime());
                            check(AnatomyMovement.confirm(client.player,body,top),"Local owner fixture could not attach to its remote support");
                        }
                        var before=body.position();baseProvider.shift=DELTA;
                        AnatomyMovement.carry(scenario==0?body:client.player);
                        if(!body.position().equals(before))problems.add(scenario==0?"Observer direct carry moved a server-owned root":"Owner carry recursively moved its server-owned support");
                    }
                }finally{
                    Platforms.clearAnatomicalDefinitions(level);AnatomyMovement.deactivate(level);client.player.setPos(oldPlayer);body.setPos(initialBody);
                }
                if(!problems.isEmpty())throw new AssertionError(String.join("; ",problems));
            });
            System.out.println("S00_OBSERVER_AUTHORITY PASS direct and transitive carry keep remote roots read-only");
        }finally{world.close();}
    }
}
