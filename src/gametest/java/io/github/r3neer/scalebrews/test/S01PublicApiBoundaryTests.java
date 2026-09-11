package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.AnatomyMode;
import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** G1/S01 acceptance tests for the public facade/backend dependency inversion. */
public final class S01PublicApiBoundaryTests {
    @GameTest
    public void backendContractIsNotPublicConsumerApi(GameTestHelper h) {
        try {
            var backend = Class.forName("io.github.r3neer.scalebrews.collision.api.AnatomyBackend");
            h.assertTrue(!Modifier.isPublic(backend.getModifiers()),
                "Scale-owned backend wiring must not remain public consumer API; consumers use AnatomyApi and registered SPIs");
        } catch (ClassNotFoundException movedOrInternalized) {
            // Also valid: the Scale-owned backend contract moved out of the public API package or disappeared entirely.
        }
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
