package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** IMPLEMENTER probes for authoritative content transport, not live trait selection. */
public final class S47ImplementerBindingTransportTests {
    private static final Identifier ENTITY = Identifier.parse("minecraft:cow");
    private static final Identifier MODEL = Identifier.parse("scalebrews_test:s47_model");
    private static final Identifier POSE = Identifier.parse("scalebrews:static");
    private static final UUID EPOCH = UUID.randomUUID();
    private static final UUID SUPPORT = UUID.randomUUID();

    @GameTest
    public void completePreparedIdentitySurvivesTheRealWireCodec(GameTestHelper h) {
        var snapshot = catalog();
        var row = snapshot.resolve(ENTITY, Map.of("age", "baby")).orElseThrow();
        var id = snapshot.bindingIdentity(row).orElseThrow();
        var packet = packet(snapshot.revision(), 1, 1, 1).withBindingIdentity(id);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            AnatomyPosePayload.CODEC.encode(buffer, packet);
            var recovered = AnatomyPosePayload.CODEC.decode(buffer);
            h.assertTrue(packet.equals(recovered) && buffer.readableBytes() == 0, "v7 must round-trip the complete packet and its exact prepared content ID");
            h.assertTrue(snapshot.resolvePose(ENTITY, recovered).orElseThrow() == row, "Wire resolution must return its exact accepted member");
        } finally { buffer.release(); }
        h.succeed();
    }

    @GameTest
    public void localCompatibilityPacketsCannotEnterTheLiveWire(GameTestHelper h) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            boolean rejected = false;
            try { AnatomyPosePayload.CODEC.encode(buffer, packet(1, 1, 1, 1)); }
            catch (IllegalArgumentException expected) { rejected = true; }
            h.assertTrue(rejected && buffer.writerIndex() == 0, "Missing binding content cannot emit even a partial v7 frame");
            buffer.writeIdentifier(Identifier.parse("scalebrews:binding/v2/" + "0".repeat(64)));
            rejected = false;
            try { AnatomyPosePayload.CODEC.decode(buffer); }
            catch (IllegalArgumentException expected) { rejected = true; }
            h.assertTrue(rejected, "Unsupported binding token version must fail before decoding the body");
        } finally { buffer.release(); }
        h.succeed();
    }

    @GameTest
    public void variantOnlyRowsWithTheSameNominalProvidersStayDistinct(GameTestHelper h) {
        var snapshot = catalog();
        var baby = snapshot.resolve(ENTITY, Map.of("age", "baby")).orElseThrow();
        var adult = snapshot.resolve(ENTITY, Map.of("age", "adult")).orElseThrow();
        var babyId = snapshot.bindingIdentity(baby).orElseThrow();
        var adultId = snapshot.bindingIdentity(adult).orElseThrow();
        h.assertTrue(snapshot.bindings().isEmpty() && snapshot.prepared().size() == 2 && !babyId.equals(adultId), "Executable variants survive without a default and have distinct identities");
        h.assertTrue(!baby.selection().geometry().filter().equals(adult.selection().geometry().filter())
            && !baby.selection().policy().equals(adult.selection().policy()), "Same-model rows exercise distinct material filters and policy");
        h.assertTrue(snapshot.resolvePose(ENTITY, packet(snapshot.revision(), 1, 1, 1).withBindingIdentity(babyId)).orElseThrow() == baby,
            "Baby authority resolves baby policy/filter, not the same-model adult row");
        h.assertTrue(snapshot.resolvePose(ENTITY, packet(snapshot.revision(), 1, 2, 1).withBindingIdentity(adultId)).orElseThrow() == adult,
            "Adult authority resolves its exact prepared row");
        h.succeed();
    }

    @GameTest
    public void unknownWrongTypeAndNominalMismatchHaveNoFallback(GameTestHelper h) {
        var snapshot = catalog();
        var baby = snapshot.resolve(ENTITY, Map.of("age", "baby")).orElseThrow();
        var id = snapshot.bindingIdentity(baby).orElseThrow();
        var valid = packet(snapshot.revision(), 1, 1, 1).withBindingIdentity(id);
        h.assertTrue(snapshot.resolvePose(null, valid).orElseThrow() == baby, "Ahead-of-entity admission validates a known row without claiming its type");
        h.assertTrue(snapshot.resolvePose(Identifier.parse("minecraft:pig"), valid).isEmpty(), "A known token cannot confer the wrong entity type");
        h.assertTrue(snapshot.resolvePose(null, valid.withBindingIdentity(Identifier.parse("scalebrews:binding/v1/" + "f".repeat(64)))).isEmpty(), "Unknown well-formed content ID must return absence before entity admission");
        var wrong = new AnatomyPosePayload(valid.epoch(), valid.revision(), valid.dimension(), valid.entityId(), valid.entity(),
            Identifier.parse("scalebrews_test:wrong_model"), valid.provider(), valid.rootProvider(), valid.frameSerial(), valid.authorityTick(),
            valid.jointSampleTick(), valid.rootFrameSequence(), valid.rootFrameTick(), valid.bindingGeneration(), valid.trackingGeneration(),
            valid.available(), valid.inputs(), valid.origin(), valid.yaw(), valid.scale(), valid.gravity(), valid.rootTransform(), id);
        h.assertTrue(snapshot.resolvePose(null, wrong).isEmpty(), "A token cannot launder a nominally contradictory frame");
        h.assertTrue(snapshot.resolvePose(ENTITY, packet(snapshot.revision() + 1, 1, 1, 1).withBindingIdentity(id)).isEmpty(), "Content equality cannot cross the accepted revision");
        h.succeed();
    }

    @GameTest
    public void contentChangesRequireARealBindingRestartAndSurviveTombstones(GameTestHelper h) {
        var snapshot = catalog();
        var baby = snapshot.bindingIdentity(snapshot.resolve(ENTITY, Map.of("age", "baby")).orElseThrow()).orElseThrow();
        var adult = snapshot.bindingIdentity(snapshot.resolve(ENTITY, Map.of("age", "adult")).orElseThrow()).orElseThrow();
        var first = packet(snapshot.revision(), 1, 1, 1).withBindingIdentity(baby);
        var sameGen = packet(snapshot.revision(), 2, 1, 1).withBindingIdentity(adult);
        var retrack = packet(snapshot.revision(), 2, 1, 2).withBindingIdentity(adult);
        var rebind = packet(snapshot.revision(), 2, 2, 1).withBindingIdentity(adult);
        var history = new AnatomyFrameHistory(); history.accept(first);
        h.assertTrue(history.transition(sameGen) == AnatomyFrameHistory.LifecycleTransition.REJECT
            && history.transition(retrack) == AnatomyFrameHistory.LifecycleTransition.REJECT
            && history.transition(rebind) == AnatomyFrameHistory.LifecycleTransition.RESTART, "Only a new physical binding authorizes a different content token");
        var joints = new AnatomyPoseHistory(); joints.accept(first);
        boolean rejected = false;
        try { joints.accept(sameGen); } catch (IllegalArgumentException expected) { rejected = true; }
        h.assertTrue(rejected, "Joint history cannot interpolate different bindings with identical nominal providers");
        var fence = new TrackingReplayFence(); fence.retire(first);
        h.assertTrue(fence.rejects(sameGen) && fence.rejects(retrack) && !fence.rejects(rebind), "Compressed quiet-TTL fences retain complete content identity");
        fence.closeWindows();
        h.assertTrue(fence.rejects(rebind), "A new binding cannot reopen an explicitly retired tracking window");
        h.succeed();
    }

    @GameTest
    public void registrationGenerationAllocationPreservesExplicitRootAndContent(GameTestHelper h) {
        var support = h.spawn(EntityTypes.ARMOR_STAND, 2, 2, 2);
        try {
            var snapshot = catalog();
            var id = snapshot.bindingIdentity(snapshot.resolve(ENTITY, Map.of("age", "baby")).orElseThrow()).orElseThrow();
            var root = Identifier.parse("scalebrews_test:s47_custom_root");
            var descriptor = new GeometryProvider.GeometryIdentityDescriptor(EPOCH, snapshot.revision(), MODEL, POSE, root, 0, id);
            GeometryProvider provider = entity -> Optional.empty();
            var assigned = AnatomyBindingState.rebind(support, provider, descriptor).descriptor();
            h.assertTrue(assigned.bindingGeneration() > 0 && assigned.rootProvider().equals(root) && assigned.bindingIdentity() == id,
                "Allocating a generation must preserve the complete immutable descriptor without inferring root or content");
        } finally { support.discard(); }
        h.succeed();
    }

    private static WorldAnatomyCatalog.Snapshot catalog() {
        var model = new ModelGeometry(1, ENTITY.toString(), "1", List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("body", "root", List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)), ModelGeometry.values(new Matrix4f()));
        return new WorldAnatomyCatalog().replace(Map.of(MODEL.toString(), model), List.of(binding("adult"), binding("baby")));
    }
    private static CollisionBinding binding(String age) {
        var filter = age.equals("baby") ? AnatomyFilter.DEFAULT : new AnatomyFilter(.05, .03, .00002, Set.of("body"), Set.of());
        var policy = new CollisionPolicy.Patch(Optional.of(true), Optional.empty(), Optional.of(age.equals("baby") ? .2 : .8));
        return new CollisionBinding(1, ENTITY, Map.of("age", age), new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART, MODEL, Map.of(), filter),
            new CollisionBinding.Pose(POSE, Map.of(), Set.of()), BuiltInRootTransformProviders.ENTITY_ROOT, policy, Set.of());
    }
    private static AnatomyPosePayload packet(long revision, long serial, long binding, long tracking) {
        return new AnatomyPosePayload(EPOCH, revision, Identifier.parse("minecraft:overworld"), 1, SUPPORT, MODEL, POSE,
            serial, serial, serial, serial, serial, binding, tracking, true, new PoseEngine.Inputs(0, 0, 0, 0, 0, true), Vec3.ZERO, 0, 1, net.minecraft.core.Direction.DOWN);
    }
}
