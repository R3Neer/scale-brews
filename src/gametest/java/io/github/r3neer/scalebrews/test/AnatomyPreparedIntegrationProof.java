package io.github.r3neer.scalebrews.test;

import com.google.gson.Gson;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.AnatomyMode;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/**
 * The only server GameTest entrypoint in the prepared-anatomy lane.
 *
 * Fabric 26.2 exposes no batch lifecycle annotation.  Keeping every real
 * Entity.move scenario in this one method gives the runtime one unambiguous
 * prepared lifetime, while the ordinary GameTest process never runs it beside
 * raw-core fixtures that intentionally activate/deactivate the same level.
 */
public final class AnatomyPreparedIntegrationProof {
    @GameTest(maxTicks=400)
    public void preparedCatalogRoutesAllRealEntityMoveScenarios(GameTestHelper h) throws IOException {
        var catalog=requiredCatalog();
        var cow=catalog.cow();
        var player=catalog.player();
        var cowProfile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse(cow.source()),Identifier.parse("scalebrews:static"),AnatomyFilter.DEFAULT)));
        var playerProfile=new PlatformDefinition(Identifier.parse("minecraft:player"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse(player.source()),Identifier.parse("scalebrews:player_walking"),AnatomyFilter.DEFAULT)));
        var models=Map.of(cow.source(),cow,player.source(),player);
        var profiles=Map.of("scalebrews:prepared_proof_cow",cowProfile,"scalebrews:prepared_proof_player",playerProfile);
        boolean runS43=Boolean.getBoolean("scalebrews.s43Only") || java.util.stream.Stream.of("s33Only","s35Only",
                "s36Only","s37Only","s38Only","s39Only","s40Only")
            .noneMatch(selector->Boolean.getBoolean("scalebrews."+selector));
        var nativeReference=runS43?S43PreparedNativePairProof.captureNative(h,cow):null;
        var session=AnatomyPreparedSession.start(h.getLevel().getServer(),models,profiles);
        try {
            var probe=h.spawn(net.minecraft.world.entity.EntityTypes.COW,0,20,0);
            try {
                h.assertTrue(AnatomyApi.mode(probe)==AnatomyMode.READY && AnatomyApi.ownsSharedPhysics(probe),
                    "Prepared catalog makes the shared mode ready for a body without requiring that body to be a support binding");
            } finally {probe.discard();}
            var scenarios=new java.util.ArrayList<Scenario>();
            if(Boolean.getBoolean("scalebrews.s43Only"))scenarios.add(()->S43PreparedNativePairProof.run(h,nativeReference,models,profiles));
            else if(Boolean.getBoolean("scalebrews.s35Only"))scenarios.add(()->S35PreparedItemLifecycleProof.run(h));
            else if(Boolean.getBoolean("scalebrews.s36Only"))scenarios.add(()->S36PreparedBoatProof.run(h));
            else if(Boolean.getBoolean("scalebrews.s37Only"))scenarios.add(()->S37PreparedMinecartProof.run(h));
            else if(Boolean.getBoolean("scalebrews.s38Only"))scenarios.add(()->S38PreparedFallingBlockProof.run(h));
            else if(Boolean.getBoolean("scalebrews.s39Only"))scenarios.add(()->S39PreparedLandingProof.run(h));
            else if(Boolean.getBoolean("scalebrews.s40Only"))scenarios.add(()->S40PreparedSneakJumpProof.run(h));
            else {
                if(runS43)scenarios.add(()->S43PreparedNativePairProof.run(h,nativeReference,models,profiles));
                if(!Boolean.getBoolean("scalebrews.s33Skip"))scenarios.add(()->S33PreparedPlacementProof.run(h));
                if(!Boolean.getBoolean("scalebrews.s33Only")) {
                    scenarios.add(()->S35PreparedItemLifecycleProof.run(h));
                    scenarios.add(()->S36PreparedBoatProof.run(h));
                    scenarios.add(()->S37PreparedMinecartProof.run(h));
                    scenarios.add(()->S38PreparedFallingBlockProof.run(h));
                    scenarios.add(()->S39PreparedLandingProof.run(h));
                    scenarios.add(()->S40PreparedSneakJumpProof.run(h));
                    scenarios.add(()->staleBindingIdentityCannotCertifyInterval(h));
                    scenarios.add(()->AnatomyGeometryTests.exportedCowRepeatedTransportAndIndependentMovementPrepared(h));
                    scenarios.add(()->AnatomyGeometryTests.actualEntityMovesWithAscendingAnatomyPrepared(h));
                    scenarios.add(()->S08PreparedLegacyEndpointCarryProof.run(h));
                    scenarios.add(()->AnatomyGeometryTests.exportedPlayerHeadCarriesBoatPrepared(h));
                    scenarios.add(()->AnatomyGeometryTests.exportedCowActualMovementPrepared(h));
                    scenarios.add(()->AnatomyGeometryTests.gravityChangesInvalidateMaterialTransportPrepared(h));
                    scenarios.add(()->AnatomyGeometryTests.actualMovementSixDirectionsPrepared(h));
                    scenarios.add(()->AnatomyGeometryTests.geometryBroadphaseFindsPieceOutsideSupportAabbPrepared(h));
                    scenarios.add(()->S07PreparedPairSuspensionProof.run(h));
                    scenarios.add(()->S08PreparedDerivedChainProof.run(h));
                    scenarios.add(()->S08PreparedPermutationProof.run(h));
                    scenarios.add(()->io.github.r3neer.scalebrews.collision.internal.S08PreparedStaticAnatomicalObstacleProof.run(h));
                    scenarios.add(()->S09PreparedIntervalContactProof.run(h));
                    scenarios.add(()->S09PreparedIntervalContactProof.nearEndpointWithoutTemporalContact(h));
                    scenarios.add(()->S09PreparedIntermediateContactProof.run(h));
                }
            }
            runNextScenario(h,session,List.copyOf(scenarios),0,h.getLevel().getGameTime());
        } catch(RuntimeException | Error failure) {
            session.close();
            throw failure;
        }
    }

    @FunctionalInterface
    private interface Scenario { void run() throws IOException; }

    /** Preserve one prepared lifetime, but give each scenario a fresh real level tick. */
    private static void runNextScenario(GameTestHelper h,AnatomyPreparedSession session,
            List<Scenario> scenarios,int index,long previousTick) {
        h.runAfterDelay(1,()->{
            try {
                long tick=h.getLevel().getGameTime();
                h.assertTrue(tick>previousTick,"Prepared scenarios require distinct real level ticks");
                if(index==scenarios.size()) {
                    session.close();
                    h.succeed();
                    return;
                }
                System.out.println("PREPARED_SCENARIO index="+index+" levelTick="+tick);
                scenarios.get(index).run();
                runNextScenario(h,session,scenarios,index+1,tick);
            } catch(IOException failure) {
                session.close();
                throw new java.io.UncheckedIOException(failure);
            } catch(RuntimeException | Error failure) {
                session.close();
                throw failure;
            }
        });
    }

    /**
     * Physical entity identity is necessary but not sufficient.  A stale interval
     * from another binding epoch/generation/model/pose/revision must never be
     * certified merely because it still names the same live entity instance.
     */
    private static void staleBindingIdentityCannotCertifyInterval(GameTestHelper h) {
        var support=h.spawn(net.minecraft.world.entity.EntityTypes.COW,1,20,1);
        try {
            AnatomyRuntime.prepare(h.getLevel());
            AnatomyMovement.tick(h.getLevel());
            var current=AnatomyMovement.queryFrame(support).orElseThrow();
            var live=current.identity();
            var staleIdentities=List.of(
                new GeometryProvider.GeometryIdentity(live.dimension(), live.support(), live.entityId(), live.epoch(), live.revision(), live.model(), live.poseProvider(), live.rootProvider(), Math.incrementExact(live.bindingGeneration()), live.localRegistrationGeneration(), live.bindingIdentity()),
                new GeometryProvider.GeometryIdentity(live.dimension(), live.support(), live.entityId(), live.epoch(), live.revision(), live.model(), live.poseProvider(), live.rootProvider(), live.bindingGeneration(), Math.incrementExact(live.localRegistrationGeneration()), live.bindingIdentity()),
                new GeometryProvider.GeometryIdentity(live.dimension(), live.support(), live.entityId(), UUID.randomUUID(), live.revision(), live.model(), live.poseProvider(), live.rootProvider(), live.bindingGeneration(), live.localRegistrationGeneration(), live.bindingIdentity()),
                new GeometryProvider.GeometryIdentity(live.dimension(), live.support(), live.entityId(), live.epoch(), live.revision(), Identifier.parse("test:stale_model"), live.poseProvider(), live.rootProvider(), live.bindingGeneration(), live.localRegistrationGeneration(), live.bindingIdentity()),
                new GeometryProvider.GeometryIdentity(live.dimension(), live.support(), live.entityId(), live.epoch(), live.revision(), live.model(), Identifier.parse("test:stale_pose"), live.rootProvider(), live.bindingGeneration(), live.localRegistrationGeneration(), live.bindingIdentity()),
                new GeometryProvider.GeometryIdentity(live.dimension(), live.support(), live.entityId(), live.epoch(), Math.incrementExact(live.revision()), live.model(), live.poseProvider(), live.rootProvider(), live.bindingGeneration(), live.localRegistrationGeneration(), live.bindingIdentity())
            );
            for(var stale:staleIdentities) {
                var snapshot=new GeometryProvider.Snapshot(stale.revision(),current.snapshot().pieces());
                var endpoint=current.endpoint();
                var before=new GeometryProvider.QueryFrame(stale,endpoint,snapshot);
                var afterEndpoint=new GeometryProvider.CausalEndpoint(Math.incrementExact(endpoint.frameSerial()),endpoint.authorityTick(),
                    endpoint.jointSampleTick(),endpoint.root(),endpoint.sample(),endpoint.availability());
                var after=new GeometryProvider.QueryFrame(stale,afterEndpoint,snapshot);
                var handle=new GeometryProvider.MotionIntervalHandle(stale,1,before,after);
                h.assertTrue(AnatomyRuntime.interval(support,handle).isEmpty(),
                    "Prepared runtime must reject stale binding identity axis: "+stale);
            }
        } finally {support.discard();}
    }

    private record ExportedCatalog(ModelGeometry cow,ModelGeometry player) {}

    /** Source is the isolated client export, never a user datapack or renderer. */
    private static ExportedCatalog requiredCatalog() throws IOException {
        var property=System.getProperty("scalebrews.anatomyCatalog");
        if(property==null || property.isBlank())
            throw new IllegalStateException("Prepared anatomy proof requires -PscalebrewsAnatomyCatalog from the isolated client export");
        var root=Path.of(property);
        var gson=new Gson();
        var cow=gson.fromJson(java.nio.file.Files.readString(root.resolve("minecraft_cow.json")),ModelGeometry.class);
        var player=gson.fromJson(java.nio.file.Files.readString(root.resolve("minecraft_player_wide.json")),ModelGeometry.class);
        if(cow==null || player==null || cow.format()!=2 || player.format()!=2
                || !"minecraft:cow".equals(cow.source()) || !"minecraft:player_wide".equals(player.source()))
            throw new IllegalArgumentException("Isolated export catalog does not contain the required original cow/player geometries");
        return new ExportedCatalog(cow,player);
    }
}
