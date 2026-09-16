package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.RootTransformProvider;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityTypes;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** S21 lifecycle holdout: reload/rebind must fence obsolete root authority and interval identity. */
public final class S21AdversarialRootRebindLifecycleTests {
    private static final Identifier ENTITY = Identifier.parse("minecraft:cow");
    private static final Identifier MODEL = Identifier.parse("scalebrews_test:s21_root_rebind_model");
    private static final Identifier ROOT_A = Identifier.parse("scalebrews_test:s21_root_rebind_a");
    private static final Identifier ROOT_B = Identifier.parse("scalebrews_test:s21_root_rebind_b");
    private static final AtomicInteger ROOT_A_CALLS = new AtomicInteger();
    private static final AtomicInteger ROOT_B_CALLS = new AtomicInteger();
    private static final RootTransformProvider PROVIDER_A = entity -> {
        ROOT_A_CALLS.incrementAndGet();
        return Optional.of(new RootTransformProvider.RootTransform(entity.position(),
            new Quaternionf().rotateZ((float)(Math.PI * .5)), entity.getScale()));
    };
    private static final RootTransformProvider PROVIDER_B = entity -> {
        ROOT_B_CALLS.incrementAndGet();
        return Optional.of(new RootTransformProvider.RootTransform(entity.position(),
            new Quaternionf().rotateX((float)(Math.PI * .5)), entity.getScale()));
    };

    @GameTest(maxTicks = 80)
    public void rebindMustReplaceRootAuthorityAndRejectObsoleteIntervals(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        registerRoot(ROOT_A, PROVIDER_A);
        registerRoot(ROOT_B, PROVIDER_B);
        ROOT_A_CALLS.set(0);
        ROOT_B_CALLS.set(0);

        AnatomyRuntime.stop(server);
        AnatomyRuntime.startPrepared(server, Map.of(), Map.of());
        var support = h.spawn(EntityTypes.COW, 2, 2, 2);
        support.setNoAi(true);
        support.setNoGravity(true);

        try {
            Object state = runtimeState(server);
            WorldAnatomyCatalog catalog = runtimeCatalog(state);

            var revisionA = catalog.replace(Map.of(MODEL.toString(), model()), List.of(binding(ROOT_A)));
            resetRuntime(server, state);
            var providerA = liveProvider(h, support, "revision A");
            providerA.tick(support, support.level().getGameTime());
            var firstA = AnatomyMovement.queryFrame(support).orElseThrow(
                () -> new AssertionError("S21 lifecycle fixture requires a published query frame for revision A"));

            h.assertTrue(firstA.identity().revision() == revisionA.revision()
                    && firstA.identity().rootProvider().equals(ROOT_A),
                "Revision A must publish the selected root-provider identity");
            h.assertTrue(sameOrientation(firstA.rootTransform().quaternion(),
                    new Quaternionf().rotateZ((float)(Math.PI * .5))),
                "Revision A must publish root A's transverse orientation");

            // Build a genuinely valid A-generation interval before rebinding. Changing only world-root
            // position must advance material serial while preserving the same live binding identity.
            support.setPos(support.getX() + .25, support.getY(), support.getZ());
            AnatomyMovement.spatialMutation(support);
            var secondA = AnatomyMovement.queryFrame(support).orElseThrow(
                () -> new AssertionError("S21 lifecycle fixture requires a second revision-A query frame"));
            h.assertTrue(secondA.identity().equals(firstA.identity()),
                "Root-only movement inside one binding must preserve geometry identity");
            h.assertTrue(secondA.endpoint().frameSerial() > firstA.endpoint().frameSerial(),
                "Root-only movement must advance the material endpoint before the rebind holdout can certify an interval");

            var oldHandle = new GeometryProvider.MotionIntervalHandle(firstA.identity(), 1, firstA, secondA);
            h.assertTrue(AnatomyRuntime.acceptsIntervalIdentity(support, oldHandle),
                "Precondition: the A-generation interval must be accepted before rebinding");
            int aCallsBeforeRebind = ROOT_A_CALLS.get();
            long oldBindingGeneration = secondA.identity().bindingGeneration();
            long oldRegistrationGeneration = secondA.identity().localRegistrationGeneration();

            var revisionB = catalog.replace(Map.of(MODEL.toString(), model()), List.of(binding(ROOT_B)));
            h.assertTrue(revisionB.revision() == revisionA.revision() + 1,
                "Synthetic reload fixture must advance the accepted catalog revision exactly once");
            resetRuntime(server, state);

            var providerB = liveProvider(h, support, "revision B");
            h.assertTrue(providerB != providerA,
                "A runtime rebind must replace the revision-A ModelGeometryProvider instance");
            providerB.tick(support, support.level().getGameTime());
            var rebound = AnatomyMovement.queryFrame(support).orElseThrow(
                () -> new AssertionError("S21 rebind must publish a query frame for revision B"));

            h.assertTrue(rebound.identity().revision() == revisionB.revision()
                    && rebound.identity().rootProvider().equals(ROOT_B),
                "Rebind must publish only revision B and root-provider B identity");
            h.assertTrue(rebound.identity().bindingGeneration() > oldBindingGeneration,
                "Reload/rebind must allocate a fresh monotonic runtime binding generation");
            h.assertTrue(rebound.identity().localRegistrationGeneration() > oldRegistrationGeneration,
                "Reload/rebind must advance the local registration generation for the same entity instance");
            h.assertTrue(sameOrientation(rebound.rootTransform().quaternion(),
                    new Quaternionf().rotateX((float)(Math.PI * .5))),
                "Revision B must publish root B's orientation, not retain root A");
            h.assertTrue(!sameOrientation(rebound.rootTransform().quaternion(), firstA.rootTransform().quaternion()),
                "The holdout roots must be materially distinct across the rebind");

            h.assertTrue(!AnatomyRuntime.acceptsIntervalIdentity(support, oldHandle),
                "An interval certified under root A must be rejected after the runtime rebinds to root B");
            h.assertTrue(AnatomyRuntime.interval(support, oldHandle).isEmpty(),
                "Obsolete A-generation interval data must not execute against revision B");
            h.assertTrue(ROOT_A_CALLS.get() == aCallsBeforeRebind,
                "Revision B must never resample the obsolete root-A provider");
            h.assertTrue(ROOT_B_CALLS.get() == 1,
                "One revision-B authoritative endpoint must sample root B exactly once; query reuse must not resample it");
        } finally {
            support.discard();
            AnatomyRuntime.stop(server);
        }
        h.succeed();
    }

    private static ModelGeometryProvider liveProvider(GameTestHelper h, net.minecraft.world.entity.LivingEntity support, String label) {
        var provider = AnatomyBindingState.provider(support);
        h.assertTrue(provider instanceof ModelGeometryProvider,
            "S21 lifecycle " + label + " must own a live ModelGeometryProvider");
        return (ModelGeometryProvider) provider;
    }

    private static void registerRoot(Identifier id, RootTransformProvider provider) {
        var existing = CollisionEngines.rootTransform(id);
        if (existing.isEmpty()) CollisionEngines.registerRootTransform(id, provider);
        else if (existing.orElseThrow() != provider)
            throw new AssertionError("S21 lifecycle root id already belongs to another provider: " + id);
    }

    private static CollisionBinding binding(Identifier root) {
        return new CollisionBinding(CollisionBinding.SCHEMA_VERSION, ENTITY, Map.of(),
            new CollisionBinding.Geometry(LegacyCollisionData.PRECOMPUTED_GEOMETRY,
                MODEL, Map.of(), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(LegacyCollisionData.LEGACY_POSE_PROVIDER,
                Map.of("provider", "scalebrews:static"), Set.of()),
            root, CollisionPolicy.Patch.EMPTY, Set.of());
    }

    private static ModelGeometry model() {
        return new ModelGeometry(1, MODEL.toString(), "1",
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("probe", "root",
                List.of(1d, .1d, .2d), List.of(1.5d, 1.2d, .6d), null)),
            ModelGeometry.values(new Matrix4f()));
    }

    private static boolean sameOrientation(Quaternionf a, Quaternionf b) {
        var left = new Quaternionf(a).normalize();
        var right = new Quaternionf(b).normalize();
        return Math.abs(left.dot(right)) > 0.99999f;
    }

    @SuppressWarnings("unchecked")
    private static Object runtimeState(MinecraftServer server) {
        try {
            Field states = AnatomyRuntime.class.getDeclaredField("STATES");
            states.setAccessible(true);
            Object state = ((Map<MinecraftServer, ?>) states.get(null)).get(server);
            if (state == null) throw new AssertionError("S21 lifecycle fixture failed to start AnatomyRuntime");
            return state;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("S21 lifecycle could not reach the runtime state test boundary", failure);
        }
    }

    private static WorldAnatomyCatalog runtimeCatalog(Object state) {
        try {
            Field catalog = state.getClass().getDeclaredField("catalog");
            catalog.setAccessible(true);
            return (WorldAnatomyCatalog) catalog.get(state);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("S21 lifecycle could not reach the runtime catalog test boundary", failure);
        }
    }

    private static void resetRuntime(MinecraftServer server, Object state) {
        try {
            Method reset = AnatomyRuntime.class.getDeclaredMethod("reset", MinecraftServer.class, state.getClass());
            reset.setAccessible(true);
            reset.invoke(null, server, state);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("S21 lifecycle could not invoke the production reload/reset boundary", failure);
        }
    }
}
