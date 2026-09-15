package io.github.r3neer.scalebrews.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * World-owning harness for the pinned S20 oracle. The semantic proof remains in
 * {@link S20CitadelPoseClientProof}; this wrapper only supplies a real client world so the
 * external entity types can be constructed through vanilla EntityType#create.
 */
public final class S20CitadelPoseWorldClientProof implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();
            new S20CitadelPoseClientProof().runTest(context);
        }
    }
}
