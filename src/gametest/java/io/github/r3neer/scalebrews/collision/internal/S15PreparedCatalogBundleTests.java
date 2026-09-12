package io.github.r3neer.scalebrews.collision.internal;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** Red-before-green holdouts for G3/NFR-010 prepared catalog publication. */
public final class S15PreparedCatalogBundleTests {
    @GameTest
    public void catalogTransferMustOwnAReusablePreparedBundle(GameTestHelper h) {
        boolean exists=Arrays.stream(AnatomyCatalogTransfer.class.getDeclaredClasses())
            .anyMatch(type->type.getSimpleName().equals("PreparedBundle"));
        h.assertTrue(exists,
            "NFR-010 requires one reusable prepared catalog bundle instead of per-recipient serialization");
        h.succeed();
    }

    @GameTest
    public void acceptedWorldCatalogMustExposePreparedPackets(GameTestHelper h) {
        boolean seam=Arrays.stream(WorldAnatomyCatalog.class.getDeclaredMethods())
            .anyMatch(method->method.getName().equals("preparedPackets")
                && Arrays.equals(method.getParameterTypes(),new Class<?>[]{UUID.class})
                && java.util.List.class.isAssignableFrom(method.getReturnType()));
        h.assertTrue(seam,
            "An accepted catalog revision must own reusable packets before any recipient asks for them");
        h.succeed();
    }

    @GameTest
    public void recipientSendMustNotAcceptRawCatalogModelsOrProfiles(GameTestHelper h) {
        var raw=Arrays.stream(AnatomyNetworking.class.getDeclaredMethods())
            .filter(method->method.getName().equals("sendCatalog"))
            .filter(method->method.getParameterCount()>0 && method.getParameterTypes()[0]==ServerPlayer.class)
            .filter(method->Arrays.stream(method.getParameterTypes()).anyMatch(Map.class::isAssignableFrom))
            .map(Method::toString)
            .sorted()
            .toList();
        h.assertTrue(raw.isEmpty(),
            "Recipient-time catalog send still accepts raw models/profiles and can reserialize per player: "+raw);
        h.succeed();
    }

    @GameTest
    public void acceptedRevisionMustHaveAnExplicitServerPublicationFence(GameTestHelper h) {
        boolean seam=Arrays.stream(AnatomyNetworking.class.getDeclaredMethods())
            .anyMatch(method->method.getName().equals("acceptCatalogRevision")
                && method.getParameterCount()==2
                && method.getParameterTypes()[0]==net.minecraft.server.MinecraftServer.class
                && method.getParameterTypes()[1]==long.class);
        h.assertTrue(seam,
            "Catalog revision authority must advance when a server accepts a revision, not when the first recipient happens to join");
        h.succeed();
    }
}
