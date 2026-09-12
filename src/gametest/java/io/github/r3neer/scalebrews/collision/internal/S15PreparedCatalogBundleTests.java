package io.github.r3neer.scalebrews.collision.internal;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** Red-before-green and post-green holdouts for G3/NFR-010 prepared catalog publication. */
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

    @GameTest
    public void sameAcceptedRevisionReusesExactPreparedPacketList(GameTestHelper h) {
        var catalog=new WorldAnatomyCatalog();
        var epoch=UUID.randomUUID();
        var first=catalog.preparedPackets(epoch);
        var second=catalog.preparedPackets(epoch);
        h.assertTrue(first==second,
            "NFR-010 requires repeated recipients of one epoch/revision to reuse the same prepared packet list");
        h.assertTrue(!first.isEmpty() && first.getFirst()==second.getFirst(),
            "Prepared payload objects themselves must be reused rather than rematerialized per recipient");
        h.succeed();
    }

    @GameTest
    public void failedReplacementPreservesAcceptedSnapshotAndBundle(GameTestHelper h) {
        var catalog=new WorldAnatomyCatalog();
        var epoch=UUID.randomUUID();
        var accepted=catalog.snapshot();
        var packets=catalog.preparedPackets(epoch);
        boolean rejected=false;
        try {catalog.replace(null,Map.of());}
        catch(NullPointerException | IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected,"Invalid catalog replacement fixture must fail before publication");
        h.assertTrue(catalog.snapshot()==accepted,
            "Invalid replacement must retain the exact previously accepted snapshot object");
        h.assertTrue(catalog.preparedPackets(epoch)==packets,
            "Invalid replacement must retain the exact prepared bundle paired with the accepted snapshot");
        h.succeed();
    }

    @GameTest
    public void newRevisionGetsDistinctPreparedPackets(GameTestHelper h) {
        var catalog=new WorldAnatomyCatalog();
        var epoch=UUID.randomUUID();
        var before=catalog.snapshot();
        var oldPackets=catalog.preparedPackets(epoch);
        var after=catalog.replace(Map.of(),Map.of());
        var newPackets=catalog.preparedPackets(epoch);
        h.assertTrue(after.revision()==before.revision()+1,
            "Accepted replacement must advance exactly one catalog revision");
        h.assertTrue(newPackets!=oldPackets,
            "A new accepted revision must replace the prepared packet list rather than reuse stale packets");
        h.assertTrue(newPackets.stream().allMatch(packet->packet.revision()==after.revision() && packet.epoch().equals(epoch)),
            "Every packet in a new prepared bundle must carry the new revision and requested epoch");
        h.succeed();
    }

    @GameTest
    public void epochSwitchCannotReusePacketsFromAnotherServerEpoch(GameTestHelper h) {
        var catalog=new WorldAnatomyCatalog();
        var firstEpoch=UUID.randomUUID();var secondEpoch=UUID.randomUUID();
        var first=catalog.preparedPackets(firstEpoch);
        var second=catalog.preparedPackets(secondEpoch);
        h.assertTrue(second!=first && second.stream().allMatch(packet->packet.epoch().equals(secondEpoch)),
            "Changing server epoch must rematerialize headers instead of reusing packets from the previous epoch");
        var firstAgain=catalog.preparedPackets(firstEpoch);
        h.assertTrue(firstAgain!=second && firstAgain.stream().allMatch(packet->packet.epoch().equals(firstEpoch)),
            "Epoch cache switching must never relabel or serve packets belonging to another epoch");
        var copy=firstAgain.getFirst().fragment();byte original=copy[0];copy[0]^=1;
        h.assertTrue(firstAgain.getFirst().fragment()[0]==original,
            "Reusing a prepared payload must not expose mutable shared fragment storage");
        h.succeed();
    }
}
