package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.AnatomyBackend;
import io.github.r3neer.scalebrews.collision.api.AnatomyMode;
import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import java.lang.reflect.Modifier;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** G1/S01 acceptance tests for the public facade/backend dependency inversion. */
public final class S01PublicApiBoundaryTests {
    @GameTest
    public void backendContractIsNotPublicConsumerApi(GameTestHelper h) {
        h.assertTrue(!Modifier.isPublic(AnatomyBackend.class.getModifiers()),
            "Scale-owned backend wiring must not become public consumer API; consumers use AnatomyApi and registered SPIs");
        h.succeed();
    }

    @GameTest
    public void unavailableBackendFailsClosed(GameTestHelper h) {
        var backend = AnatomyBackend.unavailable();
        h.assertTrue(backend.mode(null) == AnatomyMode.DISABLED, "Absent backend must report DISABLED");
        h.assertTrue(!backend.ownsSharedPhysics(null) && !backend.ready(null) && !backend.supported(null),
            "Absent backend must not claim ownership, readiness or support");
        h.assertTrue(backend.support(null).isEmpty() && backend.raycast(null, null, null).isEmpty(),
            "Absent backend must not invent material state");
        h.assertTrue(backend.gravity(null).equals(GravityFrame.VANILLA),
            "Absent backend uses the inert vanilla gravity frame");
        boolean rejected = false;
        try { backend.installGravityAdapter("fixture", entity -> net.minecraft.core.Direction.DOWN); }
        catch (IllegalStateException expected) { rejected = true; }
        h.assertTrue(rejected, "Absent backend must not pretend an external gravity owner was installed");
        h.succeed();
    }

    @GameTest
    public void publicFacadeMissingArgumentsFailClosed(GameTestHelper h) {
        h.assertTrue(AnatomyApi.mode(null) == AnatomyMode.DISABLED,
            "Missing entity must resolve to the disabled public mode");
        h.assertTrue(!AnatomyApi.ownsSharedPhysics(null) && !AnatomyApi.ready(null) && !AnatomyApi.supported(null),
            "Missing entity cannot claim collision ownership/readiness/support");
        h.assertTrue(AnatomyApi.support(null).isEmpty(), "Missing entity cannot expose a support");
        h.assertTrue(!AnatomyApi.spaceClear(null, null), "Missing entity/box cannot claim anatomical clearance");
        h.assertTrue(AnatomyApi.raycast(null, null, null).isEmpty(), "Missing ray inputs cannot manufacture a material hit");
        h.assertTrue(!AnatomyApi.attachAtContact(null, null), "Missing body/contact cannot create a physical anchor");
        h.assertTrue(AnatomyApi.gravity(null).equals(GravityFrame.VANILLA), "Missing entity uses the inert vanilla gravity frame");
        AnatomyApi.clearContact(null);
        h.succeed();
    }

    @GameTest
    public void runtimeProvidesExactlyOneScaleOwnedBackend(GameTestHelper h) {
        var providers = ServiceLoader.load(AnatomyBackend.class, AnatomyApi.class.getClassLoader()).stream().toList();
        h.assertTrue(providers.size() == 1, "Runtime must expose exactly one collision backend service");
        h.assertTrue(providers.getFirst().type().getName().equals(
            "io.github.r3neer.scalebrews.collision.internal.ScaleAnatomyBackend"),
            "The sole backend must be Scale-owned");
        h.succeed();
    }

    @GameTest
    public void publicFacadeSignaturesDoNotExposeInternalTypes(GameTestHelper h) {
        var declared = Stream.concat(
            Stream.of(AnatomyApi.class.getDeclaredFields()).map(field -> field.getType()),
            Stream.of(AnatomyApi.class.getDeclaredMethods()).flatMap(method ->
                Stream.concat(Stream.of(method.getReturnType()), Stream.of(method.getParameterTypes()))));
        var leaked = declared.map(Class::getName).filter(name -> name.contains(".collision.internal.")).toList();
        h.assertTrue(leaked.isEmpty(), "Public facade must not expose collision.internal types: " + leaked);
        h.succeed();
    }
}
