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
        try(var session=AnatomyPreparedSession.start(h.getLevel().getServer(),
                Map.of(cow.source(),cow,player.source(),player),
                Map.of("scalebrews:prepared_proof_cow",cowProfile,"scalebrews:prepared_proof_player",playerProfile))) {
            var probe=h.spawn(net.minecraft.world.entity.EntityTypes.COW,0,20,0);
            try {
                h.assertTrue(AnatomyApi.mode(probe)==AnatomyMode.READY && AnatomyApi.ownsSharedPhysics(probe),
                    "Prepared catalog makes the shared mode ready for a body without requiring that body to be a support binding");
            } finally {probe.discard();}
            staleBindingIdentityCannotCertifyInterval(h);
            // The helpers retain their production route: actual Entity.move,
            // mixins, core queries and material contacts.  They do not start,
            // stop or otherwise simulate the session.
            AnatomyGeometryTests.exportedCowRepeatedTransportAndIndependentMovementPrepared(h);
            AnatomyGeometryTests.actualEntityMovesWithAscendingAnatomyPrepared(h);
            // S08 I6 boundary proof. This support is registered manually inside an otherwise
            // prepared server session; it is not an AnatomyRuntime active binding and setPos
            // therefore creates no certified ROOT/JOINT interval for the dispatcher to own.
            // The legacy endpoint carry remains the required fallback until a material interval
            // actually exists. Session-level ownership must not suppress this relation.
            AnatomyGeometryTests.anatomicalRootTransportOncePrepared(h);
            AnatomyGeometryTests.exportedPlayerHeadCarriesBoatPrepared(h);
            AnatomyGeometryTests.exportedCowActualMovementPrepared(h);
            AnatomyGeometryTests.gravityChangesInvalidateMaterialTransportPrepared(h);
            AnatomyGeometryTests.actualMovementSixDirectionsPrepared(h);
            AnatomyGeometryTests.geometryBroadphaseFindsPieceOutsideSupportAabbPrepared(h);
            // Pair-local recovery must be proven geometrically, not via a block-wall fixture
            // whose absolute GameTest coordinates can accidentally leave an escape route.
            S07PreparedPairSuspensionProof.run(h);
            // S08 proves ROOT ownership, synchronous dispatcher drain and A -> B -> C derived
            // carry through the same prepared catalog/runtime. This complements, rather than
            // replaces, the I6 fallback-boundary proof above.
            S08PreparedDerivedChainProof.run(h);
            // A10 adversarial permutation: an active but non-causal cow sits inside the same
            // material envelope. Reversing whether it or B receives the earlier runtime binding
            // generation must not poison, reorder or add a DERIVED_CARRY to the real chain.
            S08PreparedPermutationProof.run(h);
            // FR-057 must also see a stationary anatomical support outside A's event batch.
            // Its vanilla pair is intentionally suppressed because the anatomy core owns it;
            // the replacement geometry therefore has to remain a continuous path obstacle.
            io.github.r3neer.scalebrews.collision.internal.S08PreparedStaticAnatomicalObstacleProof.run(h);
            // S09 A8: a body that never calls own-move must still be acquired by a real published
            // material interval. This distinguishes dispatcher contact establishment from an
            // own-movement path that merely happens to land on the new endpoint.
            S09PreparedIntervalContactProof.run(h);
            // S09 A9: both endpoint frames are clear. Only the certified interior trajectory
            // intersects the stationary body, so the event must affect it without retaining a
            // contact after the moving piece has withdrawn again.
            S09PreparedIntermediateContactProof.run(h);
        }
        h.succeed();
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
                new GeometryProvider.GeometryIdentity(live.dimension(),live.support(),live.entityId(),live.epoch(),live.revision(),
                    live.model(),live.poseProvider(),Math.incrementExact(live.bindingGeneration()),live.localRegistrationGeneration()),
                new GeometryProvider.GeometryIdentity(live.dimension(),live.support(),live.entityId(),live.epoch(),live.revision(),
                    live.model(),live.poseProvider(),live.bindingGeneration(),Math.incrementExact(live.localRegistrationGeneration())),
                new GeometryProvider.GeometryIdentity(live.dimension(),live.support(),live.entityId(),UUID.randomUUID(),live.revision(),
                    live.model(),live.poseProvider(),live.bindingGeneration(),live.localRegistrationGeneration()),
                new GeometryProvider.GeometryIdentity(live.dimension(),live.support(),live.entityId(),live.epoch(),live.revision(),
                    Identifier.parse("test:stale_model"),live.poseProvider(),live.bindingGeneration(),live.localRegistrationGeneration()),
                new GeometryProvider.GeometryIdentity(live.dimension(),live.support(),live.entityId(),live.epoch(),live.revision(),
                    live.model(),Identifier.parse("test:stale_pose"),live.bindingGeneration(),live.localRegistrationGeneration()),
                new GeometryProvider.GeometryIdentity(live.dimension(),live.support(),live.entityId(),live.epoch(),Math.incrementExact(live.revision()),
                    live.model(),live.poseProvider(),live.bindingGeneration(),live.localRegistrationGeneration())
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
