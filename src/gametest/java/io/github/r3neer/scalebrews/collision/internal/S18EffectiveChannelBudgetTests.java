package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData;
import io.github.r3neer.scalebrews.collision.pose.PoseProgram;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** Effective channel budget must remain constructible by the canonical PoseEngine.Inputs DTO. */
public final class S18EffectiveChannelBudgetTests {
    private static final Identifier ENGINE = Identifier.parse("scalebrews:mojang_keyframes");
    private static final Identifier PROGRAM = Identifier.parse("scalebrews_test:s18_channel_budget");
    private static final Identifier MODEL = Identifier.parse("proof:s18-channel-budget");

    @GameTest
    public void selectorChannelsCannotCreateAnUnrepresentableRequiredSet(GameTestHelper h) {
        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        PoseEngine.Resources resources = id -> id.equals(PROGRAM) ? Optional.of(program()) : Optional.empty();
        var parameters = parameters();

        var required64 = channels("gate", 64);
        var input64 = values(required64);
        new PoseEngine.Inputs(0, 0, 0, 0, 0, true, input64);

        var input65 = new LinkedHashMap<>(input64);
        input65.put("clock", 1f);
        boolean inputsReject65 = false;
        try {
            new PoseEngine.Inputs(0, 0, 0, 0, 0, true, input65);
        } catch (IllegalArgumentException expected) {
            inputsReject65 = true;
        }
        h.assertTrue(inputsReject65,
            "Precondition: PoseEngine.Inputs must accept exactly 64 channels and reject a 65-channel live payload");

        h.assertTrue(engine.bind(geometry(), parameters, required64, resources).isEmpty(),
            "Binding must fail closed when a selector expands 64 distinct declared channels to an effective set of 65");
        h.succeed();
    }

    @GameTest
    public void effectiveChannelBudgetAcceptsExactly64AndDeduplicatesSelectors(GameTestHelper h) {
        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        PoseEngine.Resources resources = id -> id.equals(PROGRAM) ? Optional.of(program()) : Optional.empty();
        var parameters = parameters();

        // 63 declared + one selector-added channel = exactly 64: this must remain bindable.
        var required63 = channels("gate", 63);
        var boundAdded = engine.bind(geometry(), parameters, required63, resources).orElse(null);
        h.assertTrue(boundAdded != null,
            "An effective required-channel set of exactly 64 must be accepted; the overflow fix must not reject the legal boundary");
        var valuesAdded = values(required63);
        valuesAdded.put("clock", 1f);
        new PoseEngine.Inputs(0, 0, 0, 0, 0, true, valuesAdded);
        h.assertTrue(boundAdded.evaluate(new PoseEngine.Inputs(0, 0, 0, 0, 0, true, valuesAdded)).isPresent(),
            "The exactly-64 binding must be satisfiable by a valid live Inputs payload");

        // 64 declared including the selector channel stays 64 after set deduplication.
        var requiredDedup = channels("gate", 63);
        requiredDedup.add("clock");
        var boundDedup = engine.bind(geometry(), parameters, requiredDedup, resources).orElse(null);
        h.assertTrue(boundDedup != null,
            "A selector channel already present in the declared set must be deduplicated, not counted twice");
        var valuesDedup = values(requiredDedup);
        new PoseEngine.Inputs(0, 0, 0, 0, 0, true, valuesDedup);
        h.assertTrue(boundDedup.evaluate(new PoseEngine.Inputs(0, 0, 0, 0, 0, true, valuesDedup)).isPresent(),
            "The deduplicated exactly-64 binding must remain live-representable and evaluable");
        h.succeed();
    }

    @GameTest
    public void catalogRejectsEffectiveChannelOverflowAtomically(GameTestHelper h) {
        var catalog = new WorldAnatomyCatalog();
        var epoch = UUID.randomUUID();
        var model = geometry();
        var program = program();

        // Publish one legal revision first so atomicity is observable by object identity and prepared-bundle reuse.
        var legalChannels = channels("gate", 63);
        var legal = binding(legalChannels);
        var accepted = catalog.replaceAtRevision(41, Map.of(MODEL.toString(), model), Map.of(PROGRAM.toString(), program), List.of(legal));
        var prepared = catalog.preparedPackets(epoch);

        var overflowing = binding(channels("gate", 64));
        boolean rejected = false;
        try {
            catalog.replaceAtRevision(42, Map.of(MODEL.toString(), model), Map.of(PROGRAM.toString(), program), List.of(overflowing));
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }

        h.assertTrue(rejected,
            "A candidate revision whose selector-added channel makes the effective live contract 65-wide must be rejected before publication");
        h.assertTrue(catalog.snapshot() == accepted,
            "Effective-channel overflow must retain the exact previously accepted catalog snapshot");
        h.assertTrue(catalog.preparedPackets(epoch) == prepared,
            "Effective-channel overflow must retain the exact prepared S15 bundle paired with the accepted snapshot");
        h.succeed();
    }

    private static CollisionBinding binding(java.util.Set<String> channels) {
        return new CollisionBinding(CollisionBinding.SCHEMA_VERSION, Identifier.parse("minecraft:cow"), Map.of(),
            new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART, MODEL, Map.of(), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(ENGINE, parameters(), channels),
            LegacyCollisionData.ENTITY_ROOT, CollisionPolicy.Patch.EMPTY, java.util.Set.of());
    }

    private static Map<String,String> parameters() {
        return Map.of(
            "program", PROGRAM.toString(),
            "clock", "channel:clock",
            "clock_scale", "1",
            "amplitude", "one");
    }

    private static LinkedHashSet<String> channels(String prefix, int count) {
        var result = new LinkedHashSet<String>();
        for (int i = 0; i < count; i++) result.add(prefix + i);
        return result;
    }

    private static LinkedHashMap<String, Float> values(Iterable<String> names) {
        var result = new LinkedHashMap<String, Float>();
        for (String name : names) result.put(name, 1f);
        return result;
    }

    private static ModelGeometry geometry() {
        var pose = new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1);
        return new ModelGeometry(2, MODEL.toString(), "26.2",
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(pose.matrix()), pose)),
            List.of(new ModelGeometry.Piece("piece", "root", List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)),
            ModelGeometry.values(new org.joml.Matrix4f()));
    }

    private static PoseProgram program() {
        var zero = new PoseProgram.Vector(0, 0, 0);
        var end = new PoseProgram.Vector(16, 0, 0);
        return new PoseProgram(PoseProgram.SCHEMA_VERSION, "proof:s18-channel-budget", "26.2", 1f, false, List.of(
            new PoseProgram.Track("root", PoseProgram.Target.TRANSLATION, List.of(
                new PoseProgram.Keyframe(0, zero, zero, PoseProgram.Interpolation.LINEAR),
                new PoseProgram.Keyframe(1, end, end, PoseProgram.Interpolation.LINEAR)))));
    }
}
