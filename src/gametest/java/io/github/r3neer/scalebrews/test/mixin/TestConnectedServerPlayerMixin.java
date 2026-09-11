package io.github.r3neer.scalebrews.test.mixin;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Minecraft 26.2's non-deprecated makeMockServerPlayer(GameType) creates a
 * ServerPlayer but deliberately leaves it outside PlayerList with no network
 * listener. Scale Brews' mount GameTests exercise ServerPlayer paths that send
 * teleport/chunk packets, so complete the mock with the same in-memory
 * connection infrastructure used by a real test login.
 *
 * <p>This mixin lives only in the GameTest source set and is never packaged in
 * the production mod JAR.</p>
 */
@Mixin(GameTestHelper.class)
public abstract class TestConnectedServerPlayerMixin {
    @Inject(method = "makeMockServerPlayer", at = @At("RETURN"))
    private void scalebrews$connectMockServerPlayer(
            GameType gameType,
            CallbackInfoReturnable<Player> cir) {
        if (!(cir.getReturnValue() instanceof ServerPlayer player) || player.connection != null) {
            return;
        }

        GameTestHelper helper = (GameTestHelper) (Object) this;
        var server = helper.getLevel().getServer();
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);

        server.getPlayerList().placeNewPlayer(
                connection,
                player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));

        helper.runBeforeTestEnd(() -> {
            if (server.getPlayerList().getPlayer(player.getUUID()) == player) {
                server.getPlayerList().remove(player);
            }
        });
    }
}
