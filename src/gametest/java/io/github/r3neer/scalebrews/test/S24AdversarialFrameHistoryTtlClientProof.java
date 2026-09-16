package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.internal.AnatomyFrameHistory;
import io.github.r3neer.scalebrews.collision.internal.AnatomyPosePayload;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** NFR-011 adversarial proof: expired client frame histories must actually leave bounded retention. */
public final class S24AdversarialFrameHistoryTtlClientProof implements FabricClientGameTest {
    private static final int FIXTURE_COUNT = 3;

    @Override public void runTest(ClientGameTestContext context) {
        context.waitFor(client -> client.level != null && client.player != null, 160);
        Set<UUID> fixtureIds = context.computeOnClient(client -> seedExpiredHistories(client.level.dimension().identifier()));

        context.waitFor(client -> {
            try {
                return staleFrames().containsAll(fixtureIds);
            } catch (ReflectiveOperationException error) {
                throw new AssertionError("Could not inspect S24 stale-frame state", error);
            }
        }, 20);

        context.runOnClient(client -> {
            try {
                var retained = frames();
                for (var id : fixtureIds) {
                    if (retained.containsKey(id)) {
                        throw new AssertionError(
                            "NFR-011: an expired AnatomyFrameHistory remained retained after its TTL for " + id);
                    }
                }
            } catch (ReflectiveOperationException error) {
                throw new AssertionError("Could not inspect S24 frame-history retention", error);
            } finally {
                cleanupFixture(fixtureIds);
            }
        });
    }

    private static Set<UUID> seedExpiredHistories(Identifier dimension) {
        try {
            cleanupFixture(Set.copyOf(frames().keySet()));
            long now = clientTick();
            var ids = new LinkedHashSet<UUID>();
            for (int i = 0; i < FIXTURE_COUNT; i++) {
                var id = new UUID(0x534234L, i + 1L);
                var history = new AnatomyFrameHistory();
                var packet = new AnatomyPosePayload(
                    new UUID(0x534234L, 0x54544cL),
                    1,
                    dimension,
                    1_500_000 + i,
                    id,
                    Identifier.parse("minecraft:cow"),
                    Identifier.parse("scalebrews:static"),
                    1,
                    new PoseEngine.Inputs(0, 0, 0, 0, 0, true, Map.of()),
                    Vec3.ZERO,
                    0,
                    1);
                if (!history.accept(packet)) throw new AssertionError("Could not seed expired S24 frame-history fixture");
                frames().put(id, history);
                receivedAt().put(id, now - 101);
                ids.add(id);
            }
            return Set.copyOf(ids);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Could not seed S24 frame-history TTL fixture", error);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, AnatomyFrameHistory> frames() throws ReflectiveOperationException {
        Field field = AnatomyClientNetworking.class.getDeclaredField("frames");
        field.setAccessible(true);
        return (Map<UUID, AnatomyFrameHistory>) field.get(null);
    }

    @SuppressWarnings("unchecked")
    private static Set<UUID> staleFrames() throws ReflectiveOperationException {
        Field field = AnatomyClientNetworking.class.getDeclaredField("staleFrames");
        field.setAccessible(true);
        return (Set<UUID>) field.get(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, Long> receivedAt() throws ReflectiveOperationException {
        Field field = AnatomyClientNetworking.class.getDeclaredField("receivedAt");
        field.setAccessible(true);
        return (Map<UUID, Long>) field.get(null);
    }

    private static long clientTick() throws ReflectiveOperationException {
        Field field = AnatomyClientNetworking.class.getDeclaredField("clientTick");
        field.setAccessible(true);
        return field.getLong(null);
    }

    private static void cleanupFixture(Set<UUID> ids) {
        try {
            frames().keySet().removeAll(ids);
            staleFrames().removeAll(ids);
            receivedAt().keySet().removeAll(ids);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Could not clean S24 frame-history TTL fixture", error);
        }
    }
}
