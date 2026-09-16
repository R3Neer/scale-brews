package io.github.r3neer.scalebrews.collision.internal;

import java.util.Map;
import java.util.ConcurrentModificationException;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Adversarial lifecycle holdout: runtime reset may reject incompatible recipients, but it must
 * not invalidate the player-list iterator it is currently using to perform that rejection.
 */
public final class S24AdversarialRuntimePlayerIterationTests {
    @GameTest
    public void resetMustTolerateMultipleRecipientsRejectedDuringCatalogBootstrap(GameTestHelper h) {
        var server=h.getLevel().getServer();
        var first=h.makeMockServerPlayerInLevel();
        var second=h.makeMockServerPlayerInLevel();
        h.assertTrue(server.getPlayerList().getPlayers().contains(first)
                && server.getPlayerList().getPlayers().contains(second),
            "Fixture requires two live recipients in the server player list before runtime bootstrap");

        try {
            AnatomyRuntime.startPrepared(server,Map.of(),Map.of());
        } catch(ConcurrentModificationException invalidIteration) {
            throw new AssertionError(
                "Runtime reset mutated the live player list while iterating recipients during catalog bootstrap",
                invalidIteration);
        } finally {
            AnatomyRuntime.stop(server);
        }
        h.succeed();
    }
}
