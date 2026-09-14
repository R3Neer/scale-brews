package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.pose.PoseProgram;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Pose programs are revision-local catalog data; CollisionEngines owns behavior only. */
public final class S18PoseProgramOwnershipTests {
    private static final String PROGRAM_ID = "scalebrews_test:s18_revision_local";

    @GameTest
    public void acceptedCatalogSnapshotsOwnRevisionLocalPoseProgramsSemantically(GameTestHelper h) {
        var catalog = new WorldAnatomyCatalog();
        var first = program(16);
        var second = program(32);

        var firstSnapshot = catalog.replaceAtRevision(11, Map.of(), Map.of(PROGRAM_ID, first), List.of());
        var secondSnapshot = catalog.replaceAtRevision(12, Map.of(), Map.of(PROGRAM_ID, second), List.of());

        h.assertTrue(firstSnapshot.revision() == 11 && secondSnapshot.revision() == 12,
            "Accepted snapshots must retain their own revision identity");
        h.assertTrue(first.equals(firstSnapshot.posePrograms().get(PROGRAM_ID)),
            "The first accepted snapshot must retain its own pose-program content after a later revision is accepted");
        h.assertTrue(second.equals(secondSnapshot.posePrograms().get(PROGRAM_ID)),
            "The later accepted snapshot must own the replacement pose-program content");
        h.assertTrue(catalog.snapshot().equals(secondSnapshot),
            "The world catalog may advance to the new snapshot without mutating the previously accepted snapshot");

        boolean immutable = false;
        try {
            firstSnapshot.posePrograms().clear();
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        h.assertTrue(immutable,
            "Accepted revision-local pose-program data must be immutable once published");
        h.succeed();
    }

    @GameTest
    public void equivalentProgramIdsCanonicalizeAndAliasCollisionFailsAtomically(GameTestHelper h) {
        var value = program(16);
        var shorthand = AnatomyCatalogTransfer.serializedBundle(Map.of(), Map.of("s18_alias", value), List.of());
        var explicit = AnatomyCatalogTransfer.serializedBundle(Map.of(), Map.of("minecraft:s18_alias", value), List.of());
        h.assertTrue(Arrays.equals(shorthand, explicit),
            "Equivalent pose-program identifiers must serialize to the same canonical protocol-v5 bytes/hash input");

        var catalog = new WorldAnatomyCatalog();
        var accepted = catalog.replaceAtRevision(21, Map.of(), Map.of("s18_alias", value), List.of());
        h.assertTrue(value.equals(accepted.posePrograms().get("minecraft:s18_alias")) && !accepted.posePrograms().containsKey("s18_alias"),
            "Accepted revision data must own only the canonical Identifier string");
        var epoch = UUID.randomUUID();
        var prepared = catalog.preparedPackets(epoch);

        boolean rejected = false;
        try {
            catalog.replaceAtRevision(22, Map.of(), Map.of("s18_alias", value, "minecraft:s18_alias", value), List.of());
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        h.assertTrue(rejected,
            "Two textual aliases of the same canonical pose-program Identifier must be rejected instead of gaining map-order precedence");
        h.assertTrue(catalog.snapshot() == accepted,
            "Rejecting a canonical pose-program alias collision must retain the exact accepted snapshot object");
        h.assertTrue(catalog.preparedPackets(epoch) == prepared,
            "Rejecting a canonical pose-program alias collision must retain the exact prepared bundle");
        h.succeed();
    }

    @GameTest
    public void poseProgramRejectsDuplicateTimestampsWithinTrack(GameTestHelper h) {
        boolean rejected = false;
        try {
            var zero = new PoseProgram.Vector(0, 0, 0);
            new PoseProgram(PoseProgram.SCHEMA_VERSION, "proof:duplicate_timestamp", "26.2", 1f, false, List.of(
                new PoseProgram.Track("root", PoseProgram.Target.TRANSLATION, List.of(
                    new PoseProgram.Keyframe(.25f, zero, zero, PoseProgram.Interpolation.LINEAR),
                    new PoseProgram.Keyframe(.25f, zero, zero, PoseProgram.Interpolation.LINEAR)))));
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        h.assertTrue(rejected,
            "A single pose-program track must reject duplicate keyframe timestamps; discontinuities belong in preTarget/postTarget, not duplicate time entries");
        h.succeed();
    }

    @GameTest
    public void collisionEngineRegistryDoesNotBecomePoseProgramStorage(GameTestHelper h) {
        boolean publicProgramRegistryApi = Arrays.stream(CollisionEngines.class.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers()))
            .map(method -> method.getName().toLowerCase(java.util.Locale.ROOT))
            .anyMatch(name -> name.contains("program"));
        h.assertTrue(!publicProgramRegistryApi,
            "CollisionEngines registers reusable behavior; pose programs must remain revision-local catalog data");
        h.succeed();
    }

    private static PoseProgram program(float endPixels) {
        var zero = new PoseProgram.Vector(0, 0, 0);
        var end = new PoseProgram.Vector(endPixels, 0, 0);
        return new PoseProgram(PoseProgram.SCHEMA_VERSION, "proof:revision_local", "26.2", 1f, false, List.of(
            new PoseProgram.Track("root", PoseProgram.Target.TRANSLATION, List.of(
                new PoseProgram.Keyframe(0, zero, zero, PoseProgram.Interpolation.LINEAR),
                new PoseProgram.Keyframe(1, end, end, PoseProgram.Interpolation.LINEAR)))));
    }
}
