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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityTypes;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** S21 lifecycle holdout: removal must immediately retire causal root interval authority. */
public final class S21AdversarialRootRemovalLifecycleTests {
    private static final Identifier ENTITY = Identifier.parse("minecraft:cow");
    private static final Identifier MODEL = Identifier.parse("scalebrews_test:s21_root_removal_model");
    private static final Identifier ROOT = Identifier.parse("scalebrews_test:s21_root_removal");
    private static final AtomicInteger ROOT_CALLS = new AtomicInteger();
    private static final RootTransformProvider PROVIDER = entity -> {
        ROOT_CALLS.incrementAndGet();
        return java.util.Optional.of(new RootTransformProvider.RootTransform(entity.position(),
            new Quaternionf().rotateZ((float)(Math.PI * .25)), entity.getScale()));
    };

    @GameTest(maxTicks = 80)
    public void removedSupportMustRejectPreviouslyCertifiedIntervalImmediately(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        registerRoot();
        ROOT_CALLS.set(0);
        AnatomyRuntime.stop(server);
        AnatomyRuntime.startPrepared(server, Map.of(), Map.of());
        var support = h.spawn(EntityTypes.COW, 3, 2, 3);
        support.setNoAi(true);
        support.setNoGravity(true);

        try {
            Object state = runtimeState(server);
            WorldAnatomyCatalog catalog = runtimeCatalog(state);
            catalog.replace(Map.of(MODEL.toString(), model()), List.of(binding()));
            resetRuntime(server, state);

            var provider = AnatomyBindingState.provider(support);
            h.assertTrue(provider instanceof ModelGeometryProvider,
                "S21 removal fixture requires a canonical runtime ModelGeometryProvider");
            var modelProvider = (ModelGeometryProvider) provider;
            modelProvider.tick(support, support.level().getGameTime());

            var first = AnatomyMovement.queryFrame(support).orElseThrow(
                () -> new AssertionError("S21 removal fixture requires an initial causal root endpoint"));
            MaterialIntervalRuntime.observe(support);
            h.assertTrue(MaterialIntervalRuntime.poll(h.getLevel()).isEmpty(),
                "Initial removal fixture observation must only seed interval continuity");

            support.setPos(support.getX() + .20, support.getY(), support.getZ());
            AnatomyMovement.spatialMutation(support);
            var second = AnatomyMovement.queryFrame(support).orElseThrow();
            MaterialIntervalRuntime.observe(support);
            var certified = MaterialIntervalRuntime.poll(h.getLevel());
            h.assertTrue(certified.size() == 1,
                "Removal fixture must certify one live interval before probing lifecycle invalidation");
            var oldHandle = certified.getFirst().handle();
            h.assertTrue(oldHandle.before().equals(first) && oldHandle.after().equals(second),
                "Certified pre-removal interval must describe the first contiguous root movement");
            h.assertTrue(AnatomyRuntime.acceptsIntervalIdentity(support, oldHandle),
                "Precondition: canonical runtime must accept the live interval identity before removal");
            h.assertTrue(AnatomyRuntime.interval(support, oldHandle).isPresent(),
                "Precondition: canonical provider must execute the certified interval before removal");

            // Leave one more valid interval pending. The real remove hook must delete it rather than
            // letting queued material work outlive the support.
            support.setPos(support.getX() + .20, support.getY(), support.getZ());
            AnatomyMovement.spatialMutation(support);
            var third = AnatomyMovement.queryFrame(support).orElseThrow();
            h.assertTrue(third.endpoint().frameSerial() == second.endpoint().frameSerial() + 1,
                "Removal fixture requires a second contiguous endpoint before discard");
            MaterialIntervalRuntime.observe(support);
            int callsBeforeRemoval = ROOT_CALLS.get();

            support.discard();
            h.assertTrue(support.isRemoved(), "Removal fixture must exercise the real Entity.remove/discard lifecycle");
            h.assertTrue(AnatomyMovement.queryFrame(support).isEmpty(),
                "Removed support must expose no live collision query frame");
            h.assertTrue(MaterialIntervalRuntime.poll(h.getLevel()).isEmpty(),
                "Entity removal must clear already queued material intervals for that support");
            h.assertTrue(!AnatomyRuntime.acceptsIntervalIdentity(support, oldHandle),
                "A handle certified while the support was alive must be rejected immediately after removal");
            h.assertTrue(AnatomyRuntime.interval(support, oldHandle).isEmpty(),
                "Removed support must not execute a previously certified root interval");
            h.assertTrue(ROOT_CALLS.get() == callsBeforeRemoval,
                "Lifecycle rejection after removal must not resample external root authority");
        } finally {
            if (!support.isRemoved()) support.discard();
            AnatomyRuntime.stop(server);
        }
        h.succeed();
    }

    private static void registerRoot() {
        var existing = CollisionEngines.rootTransform(ROOT);
        if (existing.isEmpty()) CollisionEngines.registerRootTransform(ROOT, PROVIDER);
        else if (existing.orElseThrow() != PROVIDER)
            throw new AssertionError("S21 removal root id already belongs to another provider: " + ROOT);
    }

    private static CollisionBinding binding() {
        return new CollisionBinding(CollisionBinding.SCHEMA_VERSION, ENTITY, Map.of(),
            new CollisionBinding.Geometry(LegacyCollisionData.PRECOMPUTED_GEOMETRY,
                MODEL, Map.of(), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(LegacyCollisionData.LEGACY_POSE_PROVIDER,
                Map.of("provider", "scalebrews:static"), Set.of()),
            ROOT, CollisionPolicy.Patch.EMPTY, Set.of());
    }

    private static ModelGeometry model() {
        return new ModelGeometry(1, MODEL.toString(), "1",
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("probe", "root",
                List.of(.25d, .1d, .4d), List.of(1.1d, 1.3d, .9d), null)),
            ModelGeometry.values(new Matrix4f()));
    }

    @SuppressWarnings("unchecked")
    private static Object runtimeState(MinecraftServer server) {
        try {
            Field states = AnatomyRuntime.class.getDeclaredField("STATES");
            states.setAccessible(true);
            Object state = ((Map<MinecraftServer, ?>) states.get(null)).get(server);
            if (state == null) throw new AssertionError("S21 removal fixture failed to start AnatomyRuntime");
            return state;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("S21 removal could not reach the runtime state test boundary", failure);
        }
    }

    private static WorldAnatomyCatalog runtimeCatalog(Object state) {
        try {
            Field catalog = state.getClass().getDeclaredField("catalog");
            catalog.setAccessible(true);
            return (WorldAnatomyCatalog) catalog.get(state);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("S21 removal could not reach the runtime catalog test boundary", failure);
        }
    }

    private static void resetRuntime(MinecraftServer server, Object state) {
        try {
            Method reset = AnatomyRuntime.class.getDeclaredMethod("reset", MinecraftServer.class, state.getClass());
            reset.setAccessible(true);
            reset.invoke(null, server, state);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("S21 removal could not invoke the production reload/reset boundary", failure);
        }
    }
}
