package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.pose.PoseProgram;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Pose programs are revision-local catalog data; CollisionEngines owns behavior only. */
public final class S18PoseProgramOwnershipTests {
    private static final String PROGRAM_ID = "scalebrews_test:s18_revision_local";
    private static final int MAX_PROGRAMS = 4096;

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
    public void trackAndKeyframeBoundsAcceptNAndRejectNPlusOne(GameTestHelper h) {
        var zero = new PoseProgram.Vector(0, 0, 0);
        var oneFrame = List.of(new PoseProgram.Keyframe(0, zero, zero, PoseProgram.Interpolation.LINEAR));

        var tracksAtLimit = new ArrayList<PoseProgram.Track>(PoseProgram.MAX_TRACKS);
        for (int i = 0; i < PoseProgram.MAX_TRACKS; i++)
            tracksAtLimit.add(new PoseProgram.Track("bone_" + i, PoseProgram.Target.TRANSLATION, oneFrame));
        new PoseProgram(PoseProgram.SCHEMA_VERSION, "proof:track_limit", "26.2", 1f, false, tracksAtLimit);

        var tracksOverLimit = new ArrayList<>(tracksAtLimit);
        tracksOverLimit.add(new PoseProgram.Track("overflow", PoseProgram.Target.TRANSLATION, oneFrame));
        boolean tracksRejected = false;
        try {
            new PoseProgram(PoseProgram.SCHEMA_VERSION, "proof:track_overflow", "26.2", 1f, false, tracksOverLimit);
        } catch (IllegalArgumentException expected) {
            tracksRejected = true;
        }
        h.assertTrue(tracksRejected, "PoseProgram must accept MAX_TRACKS and reject MAX_TRACKS+1");

        var framesAtLimit = frames(PoseProgram.MAX_KEYFRAMES, 3600f);
        new PoseProgram(PoseProgram.SCHEMA_VERSION, "proof:keyframe_limit", "26.2", 3600f, false, List.of(
            new PoseProgram.Track("root", PoseProgram.Target.TRANSLATION, framesAtLimit)));

        var framesOverLimit = frames(PoseProgram.MAX_KEYFRAMES + 1, 3600f);
        boolean framesRejected = false;
        try {
            new PoseProgram(PoseProgram.SCHEMA_VERSION, "proof:keyframe_overflow", "26.2", 3600f, false, List.of(
                new PoseProgram.Track("root", PoseProgram.Target.TRANSLATION, framesOverLimit)));
        } catch (IllegalArgumentException expected) {
            framesRejected = true;
        }
        h.assertTrue(framesRejected, "PoseProgram must accept MAX_KEYFRAMES and reject MAX_KEYFRAMES+1");
        h.succeed();
    }

    @GameTest
    public void programAndWireByteBoundsAcceptNAndRejectNPlusOneAtomically(GameTestHelper h) {
        var value = program(16);
        var programsAtLimit = new LinkedHashMap<String, PoseProgram>(MAX_PROGRAMS);
        for (int i = 0; i < MAX_PROGRAMS; i++) programsAtLimit.put("proof:p" + i, value);

        var catalog = new WorldAnatomyCatalog();
        var accepted = catalog.replaceAtRevision(31, Map.of(), programsAtLimit, List.of());
        h.assertTrue(accepted.posePrograms().size() == MAX_PROGRAMS,
            "World catalog must accept exactly the S18 pose-program count limit");
        var epoch = UUID.randomUUID();
        var prepared = catalog.preparedPackets(epoch);

        var programsOverLimit = new LinkedHashMap<>(programsAtLimit);
        programsOverLimit.put("proof:overflow", value);
        boolean programsRejected = false;
        try {
            catalog.replaceAtRevision(32, Map.of(), programsOverLimit, List.of());
        } catch (IllegalArgumentException expected) {
            programsRejected = true;
        }
        h.assertTrue(programsRejected, "World catalog must reject the pose-program count limit plus one");
        h.assertTrue(catalog.snapshot() == accepted && catalog.preparedPackets(epoch) == prepared,
            "Program-count overflow must preserve the exact accepted snapshot and prepared bundle");

        String digest = "0".repeat(64);
        int maxCount = AnatomyCatalogPayload.MAX_BYTES / AnatomyCatalogPayload.CHUNK;
        new AnatomyCatalogPayload(UUID.randomUUID(), AnatomyApi.PROTOCOL_VERSION, AnatomyApi.capabilities(), 1,
            0, maxCount, AnatomyCatalogPayload.MAX_BYTES, digest, new byte[AnatomyCatalogPayload.CHUNK]);

        boolean bytesRejected = false;
        int overflowBytes = AnatomyCatalogPayload.MAX_BYTES + 1;
        int overflowCount = (overflowBytes + AnatomyCatalogPayload.CHUNK - 1) / AnatomyCatalogPayload.CHUNK;
        try {
            new AnatomyCatalogPayload(UUID.randomUUID(), AnatomyApi.PROTOCOL_VERSION, AnatomyApi.capabilities(), 1,
                0, overflowCount, overflowBytes, digest, new byte[AnatomyCatalogPayload.CHUNK]);
        } catch (IllegalArgumentException expected) {
            bytesRejected = true;
        }
        h.assertTrue(bytesRejected, "Protocol-v5 catalog bytes must accept MAX_BYTES and reject MAX_BYTES+1");
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

    private static List<PoseProgram.Keyframe> frames(int count, float duration) {
        var zero = new PoseProgram.Vector(0, 0, 0);
        var result = new ArrayList<PoseProgram.Keyframe>(count);
        float denominator = count - 1f;
        for (int i = 0; i < count; i++) {
            float timestamp = denominator == 0 ? 0 : duration * i / denominator;
            result.add(new PoseProgram.Keyframe(timestamp, zero, zero, PoseProgram.Interpolation.LINEAR));
        }
        return result;
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
