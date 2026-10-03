package io.github.r3neer.scalebrews.collision.internal;

import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/** New recipients of the same server authority must not reuse old tracking windows. */
public final class S29ImplementerRecipientGenerationTests {
    @GameTest public void replacementRecipientAdvancesBeyondPriorContactAndPoseWindow(GameTestHelper h) throws Exception {
        var server=h.getLevel().getServer();
        AnatomyRuntime.stop(server);
        AnatomyRuntime.startPrepared(server,Map.of(),Map.of());
        try {
            var first=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);
            var replacement=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);
            h.assertTrue(first!=replacement,"Replacement fixture reused the same recipient object");
            UUID body=UUID.fromString("298f1001-c2cb-4e90-8e6b-f35b14819950");
            long old=acquire(server,first,body);
            long fresh=acquire(server,replacement,body);
            h.assertTrue(old>0 && fresh>old,
                "A persistent server reused a retired body tracking generation across recipient replacement: old="+old+" fresh="+fresh);
            h.assertTrue(acquire(server,replacement,body)==fresh,
                "Repeated observation of the active replacement window allocated another generation");
        } finally {AnatomyRuntime.stop(server);}
        h.succeed();
    }

    @SuppressWarnings("unchecked")
    private static long acquire(net.minecraft.server.MinecraftServer server,ServerPlayer recipient,UUID body) throws Exception {
        var field=AnatomyRuntime.class.getDeclaredField("STATES");field.setAccessible(true);
        var state=((Map<Object,Object>)field.get(null)).get(server);
        if(state==null)throw new AssertionError("Missing active runtime state");
        var method=AnatomyRuntime.class.getDeclaredMethod("generation",state.getClass(),ServerPlayer.class,UUID.class);
        method.setAccessible(true);
        return ((Long)method.invoke(null,state,recipient,body)).longValue();
    }
}
