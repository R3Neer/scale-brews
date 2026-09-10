package io.github.r3neer.scalebrews.test;

import com.google.gson.Gson;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.AnatomyMode;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse(cow.source()),Identifier.parse("scalebrews:static"),AnatomyFilter.DEFAULT)));
        try(var session=AnatomyPreparedSession.start(h.getLevel().getServer(),Map.of(cow.source(),cow),Map.of("scalebrews:prepared_proof_cow",profile))) {
            var probe=h.spawn(net.minecraft.world.entity.EntityTypes.COW,0,20,0);
            try {
                h.assertTrue(AnatomyApi.mode(probe)==AnatomyMode.READY && AnatomyApi.ownsSharedPhysics(probe),
                    "Prepared catalog makes the shared mode ready for a body without requiring that body to be a support binding");
            } finally {probe.discard();}
            // The helpers retain their production route: actual Entity.move,
            // mixins, core queries and material contacts.  They do not start,
            // stop or otherwise simulate the session.
            AnatomyGeometryTests.exportedCowRepeatedTransportAndIndependentMovementPrepared(h);
            AnatomyGeometryTests.actualEntityMovesWithAscendingAnatomyPrepared(h);
            AnatomyGeometryTests.exportedPlayerHeadCarriesBoatPrepared(h);
            AnatomyGeometryTests.exportedCowActualMovementPrepared(h);
            AnatomyGeometryTests.gravityChangesInvalidateMaterialTransportPrepared(h);
            AnatomyGeometryTests.anatomicalRootTransportOncePrepared(h);
            AnatomyGeometryTests.actualMovementSixDirectionsPrepared(h);
            AnatomyGeometryTests.geometryBroadphaseFindsPieceOutsideSupportAabbPrepared(h);
            // This scenario deliberately fills a local block volume; keep it
            // last so its obstruction cannot contaminate another assertion.
            AnatomyGeometryTests.trappedPairSuspendsAndReacquiresIndependentlyPrepared(h);
        }
        h.succeed();
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
