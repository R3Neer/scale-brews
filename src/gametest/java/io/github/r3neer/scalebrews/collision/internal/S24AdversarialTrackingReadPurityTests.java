package io.github.r3neer.scalebrews.collision.internal;

import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameType;

/**
 * Adversarial S24 holdout: observing recipient tracking state must never create authority.
 *
 * <p>The recipient deliberately stays outside PlayerList, so no vanilla START_TRACKING event can
 * have allocated a window for the body. A method documented as returning the current generation
 * must therefore return UNAVAILABLE/0 rather than acquiring a new window as a side effect.</p>
 */
public final class S24AdversarialTrackingReadPurityTests {
    @GameTest
    public void observingUntrackedPairCannotMintTrackingAuthority(GameTestHelper h) {
        var server=h.getLevel().getServer();
        AnatomyRuntime.startPrepared(server,Map.of(),Map.of());
        try {
            var recipient=(net.minecraft.server.level.ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);
            var body=h.spawn(EntityTypes.PIG,2,2,2);

            h.assertTrue(!server.getPlayerList().getPlayers().contains(recipient),
                "Fixture recipient must remain outside PlayerList so no START_TRACKING event can authorize the pair");

            long observed=AnatomyRuntime.trackingGeneration(recipient,body);
            h.assertTrue(observed==TrackingGenerationLedger.UNAVAILABLE,
                "A read of an untracked recipient/body pair must return UNAVAILABLE and must not acquire tracking authority; observed="+observed);
        } finally {
            AnatomyRuntime.stop(server);
        }
        h.succeed();
    }
}
