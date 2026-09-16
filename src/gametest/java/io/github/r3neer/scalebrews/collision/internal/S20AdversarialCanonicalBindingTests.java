package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** S20 closeout holdouts for I5/I9: Citadel data must cross the catalog and become executable bindings. */
public final class S20AdversarialCanonicalBindingTests {
    private static final Identifier GAZELLE = Identifier.parse("alexsmobs:gazelle");
    private static final Identifier GRIZZLY = Identifier.parse("alexsmobs:grizzly_bear");
    private static final Identifier CITADEL = Identifier.parse("scalebrews:citadel_program");
    private static final String PROBE_CHANNEL = "probe";

    @GameTest
    public void representativeCitadelBindingsMustBecomeExecutableWithoutExternalClasses(GameTestHelper h) {
        var models = models();
        var programs = programs();
        var bindings = bindings();
        var snapshot = new WorldAnatomyCatalog().replace(models, Map.of(), programs, bindings);

        for (var entity : List.of(GAZELLE, GRIZZLY)) {
            h.assertTrue(snapshot.catalog().resolve(entity, Map.of()).isPresent(),
                "Representative S20 binding must survive canonical selection for " + entity);
            var executable = snapshot.bindings().get(entity);
            h.assertTrue(executable != null,
                "S20 I9: accepted Citadel binding must be prepared into the executable catalog for " + entity);
            h.assertTrue(executable.selection().pose().engine().equals(CITADEL),
                "Representative S20 binding must execute through the shared scalebrews:citadel_program engine");
            h.assertTrue(executable.selection().rootTransform().equals(LegacyCollisionData.ENTITY_ROOT),
                "Representative S20 binding must preserve the canonical selected root provider id for " + entity);
            h.assertTrue(executable.model().source().equals(entity.toString()),
                "Representative S20 executable binding must retain the selected model identity for " + entity);
            h.assertTrue(executable.root() != null,
                "Representative S20 binding must retain resolved root authority without external mod classes");

            var missingChannel = executable.poses().evaluate(new PoseEngine.Inputs(0, 0, 0, 0, 0, true));
            h.assertTrue(missingChannel.isEmpty(),
                "S20 I9: prepared canonical binding must retain its declared required channels for " + entity);
            var evaluated = executable.poses().evaluate(
                new PoseEngine.Inputs(0, 0, 0, 0, 0, true, Map.of(PROBE_CHANNEL, .25f)));
            h.assertTrue(evaluated.isPresent() && evaluated.orElseThrow().containsKey("root"),
                "S20 I9: prepared Citadel binding must execute its selected program and channel contract for " + entity);
        }
        h.succeed();
    }

    @GameTest
    public void citadelProgramsAndBindingsMustRoundTripAtomicallyInCatalogBundle(GameTestHelper h) {
        var models = models();
        var programs = programs();
        var bindings = bindings();
        var packets = AnatomyCatalogTransfer.encode(UUID.randomUUID(), 7, models, Map.of(), programs, bindings);
        var receiver = new AnatomyCatalogTransfer();
        boolean accepted = false;
        for (var packet : packets) accepted |= receiver.accept(packet);

        h.assertTrue(accepted && receiver.ready() && receiver.revision() == 7,
            "S20 I5: complete Citadel catalog bundle must become the accepted revision atomically");
        var snapshot = receiver.snapshot();
        h.assertTrue(snapshot.citadelPosePrograms().keySet().equals(programs.keySet()),
            "S20 I5: Citadel programs must survive catalog transfer with canonical ids");
        for (var entity : List.of(GAZELLE, GRIZZLY))
            h.assertTrue(snapshot.catalog().resolve(entity, Map.of()).isPresent(),
                "S20 I5: representative binding must survive catalog transfer for " + entity);
        h.succeed();
    }

    private static Map<String, ModelGeometry> models() {
        var result = new LinkedHashMap<String, ModelGeometry>();
        result.put(GAZELLE.toString(), model(GAZELLE));
        result.put(GRIZZLY.toString(), model(GRIZZLY));
        return Map.copyOf(result);
    }

    private static Map<String, CitadelPoseProgram> programs() {
        var result = new LinkedHashMap<String, CitadelPoseProgram>();
        result.put(GAZELLE.toString(), program(GAZELLE));
        result.put(GRIZZLY.toString(), program(GRIZZLY));
        return Map.copyOf(result);
    }

    private static CitadelPoseProgram program(Identifier entity) {
        var operation = new CitadelPoseProgram.Operation(
            CitadelPoseProgram.OperationType.ADD_ROTATION,
            "root", List.of(), CitadelPoseProgram.Condition.always(),
            CitadelPoseProgram.Scalar.channel(PROBE_CHANNEL), CitadelPoseProgram.Scalar.constant(0),
            CitadelPoseProgram.Scalar.constant(0), null, null, null,
            0, 0, 0, 0, 0, false, false);
        return new CitadelPoseProgram(1, entity.toString(), "2.1.9", List.of(), List.of(operation));
    }

    private static List<CollisionBinding> bindings() {
        return List.of(binding(GAZELLE), binding(GRIZZLY));
    }

    private static CollisionBinding binding(Identifier entity) {
        return new CollisionBinding(CollisionBinding.SCHEMA_VERSION, entity, Map.of(),
            new CollisionBinding.Geometry(BuiltInGeometryEngines.ADVANCED_MODEL_BOX, entity, Map.of(), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(CITADEL, Map.of("program", entity.toString()), Set.of(PROBE_CHANNEL)),
            LegacyCollisionData.ENTITY_ROOT, CollisionPolicy.Patch.EMPTY, Set.of());
    }

    private static ModelGeometry model(Identifier id) {
        var rest = new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1);
        return new ModelGeometry(1, id.toString(), "2.1.9",
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(rest.matrix()), rest)),
            List.of(new ModelGeometry.Piece("probe", "root", List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)),
            ModelGeometry.values(new Matrix4f()));
    }
}
