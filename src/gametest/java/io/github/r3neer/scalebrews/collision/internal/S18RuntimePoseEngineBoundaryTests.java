package io.github.r3neer.scalebrews.collision.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** S18 layer boundary: live pose authority/evaluation must use PoseEngine directly, not the legacy provider API. */
public final class S18RuntimePoseEngineBoundaryTests {
    private static final List<Class<?>> LIVE_RUNTIME = List.of(
        AuthorityPoseTracker.class,
        AnatomyPoseHistory.class,
        AnatomyPosePayload.class,
        ModelGeometryProvider.class
    );

    @GameTest
    public void livePoseRuntimeContainsNoLegacyPoseProviderReferences(GameTestHelper h) {
        for (var type : LIVE_RUNTIME) {
            String resource = type.getSimpleName() + ".class";
            try (var input = type.getResourceAsStream(resource)) {
                h.assertTrue(input != null, "Could not inspect live pose runtime class: " + type.getName());
                String pool = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
                h.assertTrue(!pool.contains("io/github/r3neer/scalebrews/collision/pose/PoseProvider"),
                    "Live pose runtime must depend on PoseEngine, not legacy PoseProvider: " + type.getName());
            } catch (IOException unreadable) {
                throw new AssertionError("Could not inspect live pose runtime bytecode: " + type.getName(), unreadable);
            }
        }
        h.succeed();
    }
}
