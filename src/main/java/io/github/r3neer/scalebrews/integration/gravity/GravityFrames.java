package io.github.r3neer.scalebrews.integration.gravity;

import com.google.common.collect.MapMaker;
import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Single effective-gravity authority shared by Scale Brews subsystems.
 *
 * <p>Scale reads gravity but never owns gravity policy. Without an installed provider,
 * every entity uses vanilla DOWN. Different owners cannot silently replace one another.
 * The per-entity override is deliberately a development/GameTest seam so tests can exercise
 * all cardinal frames without creating a second production authority.</p>
 */
public final class GravityFrames {
    private static final String GRAVITY_CHANGER_MOD_ID = "gravity_changer";
    private static final String GRAVITY_CHANGER_OWNER = "gravity_changer";
    private static final Map<Entity, GravityFrame> TEST_OVERRIDES = Collections.synchronizedMap(
        new MapMaker().weakKeys().<Entity, GravityFrame>makeMap());
    private static volatile Provider provider;

    private record Provider(String owner, Function<Entity, Direction> resolver) {}

    private GravityFrames() {}

    public static void initialize() {
        if (!FabricLoader.getInstance().isModLoaded(GRAVITY_CHANGER_MOD_ID)) return;
        try {
            Class<?> util = Class.forName("com.moigferdsrte.gravitychanger.util.GravityDirectionUtil", false,
                GravityFrames.class.getClassLoader());
            MethodHandle getter = MethodHandles.publicLookup().findStatic(util, "getGravityDirection",
                MethodType.methodType(Direction.class, Entity.class));
            install(GRAVITY_CHANGER_OWNER, entity -> invokeDirection(getter, entity));
            ScaleBrews.LOGGER.info("Using Gravity Changer as Scale Brews effective-gravity provider");
        } catch (ReflectiveOperationException | LinkageError e) {
            ScaleBrews.LOGGER.error("Gravity Changer is present but its gravity API could not be linked; Scale gravity-aware mechanics will fall back to DOWN", e);
        }
    }

    private static Direction invokeDirection(MethodHandle getter, Entity entity) {
        try {
            return Objects.requireNonNull((Direction)getter.invoke(entity), "gravity provider returned null");
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException("Gravity provider invocation failed", t);
        }
    }

    public static synchronized void install(String owner, Function<Entity, Direction> resolver) {
        if (owner == null || owner.isBlank()) throw new IllegalArgumentException("Missing gravity provider owner");
        Objects.requireNonNull(resolver, "resolver");
        Provider current = provider;
        if (current != null) {
            if (current.owner().equals(owner)) return;
            throw new IllegalStateException("Gravity provider already installed by " + current.owner());
        }
        provider = new Provider(owner, resolver);
    }

    public static Direction direction(Entity entity) {
        return frame(entity).down();
    }

    public static GravityFrame frame(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        GravityFrame override = TEST_OVERRIDES.get(entity);
        if (override != null) return override;
        Provider current = provider;
        if (current == null) return GravityFrame.VANILLA;
        Direction direction = Objects.requireNonNull(current.resolver().apply(entity), "gravity provider returned null");
        return direction == Direction.DOWN ? GravityFrame.VANILLA : new GravityFrame(direction);
    }

    public static String owner() {
        Provider current = provider;
        return current == null ? null : current.owner();
    }

    /** Development/GameTest seam only; production integrations install one global provider instead. */
    public static void overrideForTests(Entity entity, GravityFrame frame) {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment())
            throw new IllegalStateException("Per-entity gravity overrides are available only in development/GameTest");
        Objects.requireNonNull(entity, "entity");
        TEST_OVERRIDES.put(entity, Objects.requireNonNull(frame, "frame"));
    }

    /** Clears exactly one development/GameTest override without changing the global provider. */
    public static void clearOverrideForTests(Entity entity) {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment())
            throw new IllegalStateException("Per-entity gravity overrides are available only in development/GameTest");
        TEST_OVERRIDES.remove(Objects.requireNonNull(entity, "entity"));
    }

    /** Lifecycle cleanup for the development seam; never resets or replaces the global provider. */
    public static void clearTestOverrides(Level level) {
        if (level == null) return;
        synchronized (TEST_OVERRIDES) {
            TEST_OVERRIDES.keySet().removeIf(entity -> entity.level() == level);
        }
    }
}
