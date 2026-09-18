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
    public void observingWithoutRuntimeCannotInventDefaultTrackingAuthority(GameTestHelper h) {
        var server=h.getLevel().getServer();
        AnatomyRuntime.stop(server);
        var recipient=(net.minecraft.server.level.ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);
        var body=h.spawn(EntityTypes.PIG,3,2,2);

        h.assertTrue(!server.getPlayerList().getPlayers().contains(recipient),
            "No-runtime fixture recipient must remain outside PlayerList");

        long observed=AnatomyRuntime.trackingGeneration(recipient,body);
        h.assertTrue(observed==TrackingGenerationLedger.UNAVAILABLE,
            "Without an active anatomy runtime there is no recipient/body tracking authority to observe; observed="+observed);
        h.succeed();
    }

    @GameTest
    @SuppressWarnings("unchecked")
    public void observingActivePairReturnsExistingGenerationWithoutReplacingIt(GameTestHelper h) throws Exception {
        var server=h.getLevel().getServer();
        AnatomyRuntime.startPrepared(server,Map.of(),Map.of());
        try {
            var recipient=(net.minecraft.server.level.ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);
            var body=h.spawn(EntityTypes.PIG,4,2,2);

            var statesField=AnatomyRuntime.class.getDeclaredField("STATES");
            statesField.setAccessible(true);
            var states=(java.util.Map<Object,Object>)statesField.get(null);
            var state=states.get(server);
            h.assertTrue(state!=null,"Positive control requires an active runtime state");

            var acquire=java.util.Arrays.stream(AnatomyRuntime.class.getDeclaredMethods())
                .filter(method->method.getName().equals("generation") && method.getParameterCount()==3)
                .findFirst().orElseThrow();
            acquire.setAccessible(true);
            long acquired=((Long)acquire.invoke(null,state,recipient,body.getUUID())).longValue();
            h.assertTrue(acquired>0,"Positive control must install one legitimate active tracking generation");

            long observed=AnatomyRuntime.trackingGeneration(recipient,body);
            h.assertTrue(observed==acquired,
                "Observing an active pair must return the existing generation exactly, not zero or a replacement; acquired="
                    +acquired+" observed="+observed);
        } finally {
            AnatomyRuntime.stop(server);
        }
        h.succeed();
    }

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
