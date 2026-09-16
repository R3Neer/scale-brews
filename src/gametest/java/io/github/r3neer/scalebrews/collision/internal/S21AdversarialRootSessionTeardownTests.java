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

/** S21 lifecycle holdout: stopping the runtime must retire every root-authority endpoint immediately. */
public final class S21AdversarialRootSessionTeardownTests {
    private static final Identifier ENTITY = Identifier.parse("minecraft:cow");
    private static final Identifier MODEL = Identifier.parse("scalebrews_test:s21_root_session_teardown_model");
    private static final Identifier ROOT = Identifier.parse("scalebrews_test:s21_root_session_teardown");
    private static final AtomicInteger ROOT_CALLS = new AtomicInteger();
    private static final RootTransformProvider PROVIDER = entity -> {
        ROOT_CALLS.incrementAndGet();
        return java.util.Optional.of(new RootTransformProvider.RootTransform(entity.position(),
            new Quaternionf().rotateY((float)(Math.PI * .37)), entity.getScale()));
    };

    @GameTest(maxTicks = 80)
    public void stopMustInvalidateCertifiedRootHandlesAndPendingIntervals(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        registerRoot();
        ROOT_CALLS.set(0);
        AnatomyRuntime.stop(server);
        AnatomyRuntime.startPrepared(server, Map.of(), Map.of());
        var support = h.spawn(EntityTypes.COW, 3, 2, 3);
        support.setNoAi(true);
        support.setNoGravity(true);
        boolean stopped = false;

        try {
            Object state = runtimeState(server);
            WorldAnatomyCatalog catalog = runtimeCatalog(state);
            catalog.replace(Map.of(MODEL.toString(), model()), List.of(binding()));
            resetRuntime(server, state);

            var provider = AnatomyBindingState.provider(support);
            h.assertTrue(provider instanceof ModelGeometryProvider,
                "S21 teardown fixture requires a canonical runtime ModelGeometryProvider");
            ((ModelGeometryProvider) provider).tick(support, support.level().getGameTime());

            var first = AnatomyMovement.queryFrame(support).orElseThrow(
                () -> new AssertionError("S21 teardown fixture requires an initial causal root endpoint"));
            MaterialIntervalRuntime.observe(support);
            h.assertTrue(MaterialIntervalRuntime.poll(h.getLevel()).isEmpty(),
                "Initial teardown observation must only seed material continuity");

            support.setPos(support.getX() + .20, support.getY(), support.getZ());
            AnatomyMovement.spatialMutation(support);
            var second = AnatomyMovement.queryFrame(support).orElseThrow();
            MaterialIntervalRuntime.observe(support);
            var certified = MaterialIntervalRuntime.poll(h.getLevel());
            h.assertTrue(certified.size() == 1,
                "Teardown fixture must certify one live interval before stopping the runtime");
            var oldHandle = certified.getFirst().handle();
            h.assertTrue(oldHandle.before().equals(first) && oldHandle.after().equals(second),
                "Certified teardown interval must describe the first contiguous root movement");
            h.assertTrue(AnatomyRuntime.acceptsIntervalIdentity(support, oldHandle),
                "Precondition: live runtime must accept the certified root interval before stop");
            h.assertTrue(AnatomyRuntime.interval(support, oldHandle).isPresent(),
                "Precondition: live runtime must execute the certified root interval before stop");

            // Queue fresh work so stop() must prove both identity retirement and pending-work cleanup.
            support.setPos(support.getX() + .20, support.getY(), support.getZ());
            AnatomyMovement.spatialMutation(support);
            var third = AnatomyMovement.queryFrame(support).orElseThrow();
            h.assertTrue(third.endpoint().frameSerial() == second.endpoint().frameSerial() + 1,
                "Teardown fixture requires a second contiguous endpoint before stop");
            MaterialIntervalRuntime.observe(support);
            int callsBeforeStop = ROOT_CALLS.get();

            AnatomyRuntime.stop(server);
            stopped = true;

            h.assertTrue(AnatomyMovement.queryFrame(support).isEmpty(),
                "Runtime stop must expose no causal collision frame for a formerly active support");
            h.assertTrue(MaterialIntervalRuntime.poll(h.getLevel()).isEmpty(),
                "Runtime stop must clear already queued material intervals");
            h.assertTrue(!AnatomyRuntime.acceptsIntervalIdentity(support, oldHandle),
                "A handle certified by a stopped runtime must be rejected immediately");
            h.assertTrue(AnatomyRuntime.interval(support, oldHandle).isEmpty(),
                "A stopped runtime must not execute previously certified root intervals");
            h.assertTrue(ROOT_CALLS.get() == callsBeforeStop,
                "Teardown rejection must not resample external root authority");

            // Restarting the session must not make the old handle authoritative again merely because
            // the same entity object is still alive in the level.
            AnatomyRuntime.startPrepared(server, Map.of(), Map.of());
            h.assertTrue(!AnatomyRuntime.acceptsIntervalIdentity(support, oldHandle),
                "A new runtime session must not resurrect a handle certified by the stopped session");
        } finally {
            support.discard();
            if (!stopped) AnatomyRuntime.stop(server);
            else AnatomyRuntime.stop(server);
        }
        h.succeed();
    }

    private static void registerRoot() {
        var existing = CollisionEngines.rootTransform(ROOT);
        if (existing.isEmpty()) CollisionEngines.registerRootTransform(ROOT, PROVIDER);
        else if (existing.orElseThrow() != PROVIDER)
            throw new AssertionError("S21 teardown root id already belongs to another provider: " + ROOT);
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
                List.of(.15d, .2d, .3d), List.of(1.2d, 1.4d, .8d), null)),
            ModelGeometry.values(new Matrix4f()));
    }

    @SuppressWarnings("unchecked")
    private static Object runtimeState(MinecraftServer server) {
        try {
            Field states = AnatomyRuntime.class.getDeclaredField("STATES");
            states.setAccessible(true);
            Object state = ((Map<MinecraftServer, ?>) states.get(null)).get(server);
            if (state == null) throw new AssertionError("S21 teardown fixture failed to start AnatomyRuntime");
            return state;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("S21 teardown could not reach the runtime state test boundary", failure);
        }
    }

    private static WorldAnatomyCatalog runtimeCatalog(Object state) {
        try {
            Field catalog = state.getClass().getDeclaredField("catalog");
            catalog.setAccessible(true);
            return (WorldAnatomyCatalog) catalog.get(state);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("S21 teardown could not reach the runtime catalog test boundary", failure);
        }
    }

    private static void resetRuntime(MinecraftServer server, Object state) {
        try {
            Method reset = AnatomyRuntime.class.getDeclaredMethod("reset", MinecraftServer.class, state.getClass());
            reset.setAccessible(true);
            reset.invoke(null, server, state);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("S21 teardown could not invoke the production reload/reset boundary", failure);
        }
    }
}
