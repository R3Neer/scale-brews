package io.github.r3neer.scalebrews.test;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/** Shared fixture construction; network completion stays in the GameTest-only mixin. */
public final class TestPlayers {
    private TestPlayers() {}

    /** Preserve the former connected fixture's CREATIVE gameMode() contract. */
    public static ServerPlayer connectedCreative(GameTestHelper helper) {
        return (ServerPlayer)helper.makeMockServerPlayer(GameType.CREATIVE);
    }
}
