package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/** S20 I9 live-runtime holdout: canonical bindings must not inherit legacy pose-eligibility suppression. */
public final class S20AdversarialCanonicalRuntimeTests {
    private static final Identifier ARMOR_STAND = Identifier.parse("minecraft:armor_stand");
    private static final Identifier MODEL = Identifier.parse("scalebrews_test:s20_canonical_runtime");
    private static final Identifier CITADEL = Identifier.parse("scalebrews:citadel_program");

    @GameTest
    public void canonicalBindingMustPublishOrdinaryAuthorityThroughLiveRuntime(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        AnatomyRuntime.stop(server);
        AnatomyRuntime.startPrepared(server, Map.of(), Map.of());
        var support = h.spawn(EntityTypes.ARMOR_STAND, 2, 2, 2);
        try {
            Object state = runtimeState(server);
            WorldAnatomyCatalog catalog = runtimeCatalog(state);
            catalog.replace(Map.of(MODEL.toString(), model()), Map.of(),
                Map.of(MODEL.toString(), program()), List.of(binding()));

            // The stand may have loaded before the synthetic catalog was installed, so invoke the exact
            // private runtime binding boundary once. This is intentionally reflection-only test plumbing;
            // the provider, binding predicate, authority tracker and observation seam are production code.
            bindIfEligible(state, support);

            h.assertTrue(AnatomyRuntime.owns(support),
                "S20 I9: canonical armor-stand binding must enter AnatomyRuntime's active binding table");
            var selected = AnatomyMovement.canonicalBinding(support);
            h.assertTrue(selected != null && selected.pose().engine().equals(CITADEL),
                "S20 I9: live runtime must retain the canonical Citadel pose engine identity");
            var provider = AnatomyBindingState.provider(support);
            h.assertTrue(provider instanceof ModelGeometryProvider,
                "S20 I9: live canonical binding must materialize the real ModelGeometryProvider");

            long tick = support.level().getGameTime();
            ((ModelGeometryProvider) provider).tick(support, tick);
            var authority = AnatomyRuntime.authoritativePose(support).orElseThrow(
                () -> new AssertionError("S20 I9: live canonical binding must publish an authoritative pose sample"));
            h.assertTrue(authority.inputs().ordinary(),
                "S20 I9: canonical bindings must not be suppressed by legacy AnatomyPoseEligibility");
            h.assertTrue(AnatomyRuntime.catalogBinding(support).map(value -> value.pose().engine().equals(CITADEL)).orElse(false),
                "S20 I9: accepted live catalog selection must remain the canonical Citadel binding");
        } finally {
            support.discard();
            AnatomyRuntime.stop(server);
        }
        h.succeed();
    }

    @SuppressWarnings("unchecked")
    private static Object runtimeState(MinecraftServer server) {
        try {
            Field states = AnatomyRuntime.class.getDeclaredField("STATES");
            states.setAccessible(true);
            Object state = ((Map<MinecraftServer, ?>) states.get(null)).get(server);
            if (state == null) throw new AssertionError("S20 I9 fixture failed to start AnatomyRuntime");
            return state;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("S20 I9 could not reach the runtime state test boundary", failure);
        }
    }

    private static WorldAnatomyCatalog runtimeCatalog(Object state) {
        try {
            Field catalog = state.getClass().getDeclaredField("catalog");
            catalog.setAccessible(true);
            return (WorldAnatomyCatalog) catalog.get(state);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("S20 I9 could not reach the runtime catalog test boundary", failure);
        }
    }

    private static void bindIfEligible(Object state, LivingEntity support) {
        try {
            Method bind = AnatomyRuntime.class.getDeclaredMethod("bindIfEligible", state.getClass(), LivingEntity.class);
            bind.setAccessible(true);
            bind.invoke(null, state, support);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("S20 I9 could not invoke the live runtime binding boundary", failure);
        }
    }

    private static CollisionBinding binding() {
        return new CollisionBinding(CollisionBinding.SCHEMA_VERSION, ARMOR_STAND, Map.of(),
            new CollisionBinding.Geometry(BuiltInGeometryEngines.ADVANCED_MODEL_BOX, MODEL, Map.of(), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(CITADEL, Map.of("program", MODEL.toString()), Set.of()),
            LegacyCollisionData.ENTITY_ROOT, CollisionPolicy.Patch.EMPTY, Set.of());
    }

    private static ModelGeometry model() {
        var rest = new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1);
        return new ModelGeometry(1, MODEL.toString(), "2.1.9",
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(rest.matrix()), rest)),
            List.of(new ModelGeometry.Piece("probe", "root", List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)),
            ModelGeometry.values(new Matrix4f()));
    }

    private static CitadelPoseProgram program() {
        var operation = new CitadelPoseProgram.Operation(
            CitadelPoseProgram.OperationType.ADD_ROTATION,
            "root", List.of(), CitadelPoseProgram.Condition.always(),
            CitadelPoseProgram.Scalar.constant(.25f), CitadelPoseProgram.Scalar.constant(0),
            CitadelPoseProgram.Scalar.constant(0), null, null, null,
            0, 0, 0, 0, 0, false, false);
        return new CitadelPoseProgram(1, MODEL.toString(), "2.1.9", List.of(), List.of(operation));
    }
}
