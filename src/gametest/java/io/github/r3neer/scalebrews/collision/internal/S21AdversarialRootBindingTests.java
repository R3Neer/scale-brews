package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.RootTransformProvider;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** S21 holdout: changing only root authority must not make an otherwise executable binding inert. */
public final class S21AdversarialRootBindingTests {
    private static final Identifier ENTITY = Identifier.parse("minecraft:cow");
    private static final Identifier CUSTOM_ROOT = Identifier.parse("scalebrews_test:s21_transverse_catalog_root");
    private static final RootTransformProvider CUSTOM_PROVIDER = entity -> java.util.Optional.of(
        new RootTransformProvider.RootTransform(entity.position(),
            new Quaternionf().rotateZ((float)(Math.PI * .5)), entity.getScale()));

    @GameTest
    public void sameGeometryAndPoseRemainExecutableWhenOnlyRootProviderChanges(GameTestHelper h) {
        if (CollisionEngines.rootTransform(CUSTOM_ROOT).isEmpty())
            CollisionEngines.registerRootTransform(CUSTOM_ROOT, CUSTOM_PROVIDER);

        var model = fixtureModel();
        var geometry = new CollisionBinding.Geometry(
            LegacyCollisionData.PRECOMPUTED_GEOMETRY,
            Identifier.parse(model.source()), Map.of(), AnatomyFilter.DEFAULT);
        var pose = new CollisionBinding.Pose(
            LegacyCollisionData.LEGACY_POSE_PROVIDER,
            Map.of("provider", "scalebrews:static"), Set.of());
        var swappedRoot = new CollisionBinding(
            CollisionBinding.SCHEMA_VERSION, ENTITY, Map.of(), geometry, pose,
            CUSTOM_ROOT, CollisionPolicy.Patch.EMPTY, Set.of());

        var catalog = new WorldAnatomyCatalog();
        var snapshot = catalog.replace(Map.of(model.source(), model), List.of(swappedRoot));
        var executable = snapshot.bindings().get(ENTITY);

        h.assertTrue(executable != null,
            "FR-031/FR-033: changing only root_transform must preserve executable binding preparation");
        h.assertTrue(executable.selection().geometry().equals(geometry)
                && executable.selection().pose().equals(pose),
            "Root substitution must not rewrite geometry or pose authority");
        h.assertTrue(executable.selection().rootTransform().equals(CUSTOM_ROOT),
            "Prepared binding must retain the selected custom root provider id");
        h.assertTrue(executable.root() == CollisionEngines.rootTransform(CUSTOM_ROOT).orElseThrow(),
            "Prepared binding must retain the registry-resolved custom RootTransformProvider instance");
        h.succeed();
    }

    @GameTest
    public void missingRootProviderRejectsCandidateAtomically(GameTestHelper h) {
        var model = fixtureModel();
        var baseline = new CollisionBinding(
            CollisionBinding.SCHEMA_VERSION, ENTITY, Map.of(),
            new CollisionBinding.Geometry(LegacyCollisionData.PRECOMPUTED_GEOMETRY,
                Identifier.parse(model.source()), Map.of(), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(LegacyCollisionData.LEGACY_POSE_PROVIDER,
                Map.of("provider", "scalebrews:static"), Set.of()),
            LegacyCollisionData.ENTITY_ROOT, CollisionPolicy.Patch.EMPTY, Set.of());
        var catalog = new WorldAnatomyCatalog();
        var accepted = catalog.replace(Map.of(model.source(), model), List.of(baseline));
        var acceptedBinding = accepted.bindings().get(ENTITY);

        var invalid = new CollisionBinding(
            CollisionBinding.SCHEMA_VERSION, ENTITY, Map.of(), baseline.geometry(), baseline.pose(),
            Identifier.parse("scalebrews_test:missing_s21_root"), CollisionPolicy.Patch.EMPTY, Set.of());
        boolean rejected = false;
        try {
            catalog.replace(Map.of(model.source(), model), List.of(invalid));
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }

        h.assertTrue(rejected, "Unknown root provider must reject the whole candidate revision");
        h.assertTrue(catalog.snapshot() == accepted,
            "Rejected root-provider revision must retain the exact previously accepted catalog snapshot");
        h.assertTrue(catalog.snapshot().bindings().get(ENTITY) == acceptedBinding,
            "Rejected root-provider revision must retain the exact prepared binding object");
        h.succeed();
    }

    private static ModelGeometry fixtureModel() {
        return new ModelGeometry(1, "scalebrews_test:s21_root_binding_model", "1",
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("body", "root",
                List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)),
            ModelGeometry.values(new Matrix4f()));
    }
}
