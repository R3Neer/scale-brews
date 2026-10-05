package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.catalog.CollisionBindingCatalog;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** IMPLEMENTER development probes for immutable preparation; no live variant or FULL claims. */
public final class S46ImplementerPreparedVariantTests {
    private static final Identifier ENTITY = Identifier.parse("minecraft:cow");
    private static final Identifier MODEL = Identifier.parse("scalebrews_test:s46_model");
    private static final Identifier STATIC = Identifier.parse("scalebrews:static");

    @GameTest
    public void allVariantsStayPreparedWithOneCanonicalSelector(GameTestHelper h) {
        var baseline = binding(Map.of());
        var baby = binding(Map.of("age", "baby"));
        var coldBaby = binding(Map.of("age", "baby", "climate", "cold"));
        var snapshot = new WorldAnatomyCatalog().replace(models(), List.of(coldBaby, baseline, baby));
        h.assertTrue(snapshot.prepared().size() == 3, "Every prepared canonical selection must survive acceptance");
        h.assertTrue(snapshot.bindings().size() == 1 && snapshot.bindings().get(ENTITY).selection().equals(baseline),
            "Compatibility projection remains default-only");
        var chosen = snapshot.resolve(ENTITY, Map.of("climate", "cold", "age", "baby", "extra", "ignored")).orElseThrow();
        h.assertTrue(chosen.selection().equals(coldBaby), "Canonical most-specific subset must select its exact executable row");
        h.assertTrue(snapshot.resolve(ENTITY, Map.of("age", "baby")).orElseThrow().selection().equals(baby), "Less-specific exact variant remains executable");
        h.assertTrue(snapshot.resolveIdentity(ENTITY, snapshot.bindingIdentity(chosen).orElseThrow()).orElseThrow() == chosen,
            "Content lookup must retrieve the same accepted prepared instance");
        h.succeed();
    }

    @GameTest
    public void ambiguityUnknownAndWrongEntityReturnAbsence(GameTestHelper h) {
        var snapshot = new WorldAnatomyCatalog().replace(models(), List.of(binding(Map.of("age", "baby")), binding(Map.of("climate", "cold"))));
        h.assertTrue(snapshot.bindings().isEmpty() && snapshot.resolve(ENTITY, Map.of()).isEmpty(), "Variant-only catalog cannot invent a default");
        h.assertTrue(snapshot.resolve(ENTITY, Map.of("age", "baby", "climate", "cold")).isEmpty(), "Equal-specificity ambiguity cannot choose by preparation order");
        h.assertTrue(snapshot.resolve(ENTITY, Map.of("age", "adult")).isEmpty(), "Unknown variant has no execution");
        var chosen = snapshot.resolve(ENTITY, Map.of("age", "baby")).orElseThrow();
        var identity = snapshot.bindingIdentity(chosen).orElseThrow();
        h.assertTrue(snapshot.resolveIdentity(Identifier.parse("minecraft:pig"), identity).isEmpty(), "Binding content ID cannot confer another entity's ownership");
        h.assertTrue(snapshot.resolveIdentity(ENTITY, Identifier.parse("scalebrews:binding/v1/unknown")).isEmpty(), "Unknown content ID cannot fall back");
        h.assertTrue(snapshot.resolve(null, Map.of()).isEmpty() && snapshot.resolve(ENTITY, null).isEmpty(), "Missing selector inputs fail closed");
        h.succeed();
    }

    @GameTest
    public void unavailableSpecificSelectionNeverBorrowsDefaultExecution(GameTestHelper h) {
        var missing = changed(binding(Map.of("age", "baby")), ENTITY,
            new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART, Identifier.parse("scalebrews_test:s46_missing"), Map.of(), AnatomyFilter.DEFAULT),
            binding(Map.of()).pose(), BuiltInRootTransformProviders.ENTITY_ROOT, CollisionPolicy.Patch.EMPTY, Set.of());
        var snapshot = new WorldAnatomyCatalog().replace(models(), List.of(binding(Map.of()), missing));
        h.assertTrue(snapshot.catalog().resolve(ENTITY, Map.of("age", "baby")).orElseThrow().equals(missing), "Absent geometry does not erase the canonical selected row");
        h.assertTrue(snapshot.prepared().size() == 1 && snapshot.resolve(ENTITY, Map.of()).isPresent(), "Only available default was prepared");
        h.assertTrue(snapshot.resolve(ENTITY, Map.of("age", "baby")).isEmpty(), "Unavailable most-specific row cannot borrow default material");
        h.succeed();
    }

    @GameTest
    public void canonicalTransferReconstructsCompleteVariantIdentities(GameTestHelper h) {
        var variants = List.of(binding(Map.of("age", "baby")), binding(Map.of("climate", "cold")));
        var source = new WorldAnatomyCatalog().replace(models(), variants);
        var receiver = new AnatomyCatalogTransfer();
        for (var packet : AnatomyCatalogTransfer.encode(UUID.randomUUID(), 12, models(), variants)) receiver.accept(packet);
        var target = receiver.snapshot();
        h.assertTrue(receiver.ready() && target.revision() == 12 && target.prepared().size() == 2 && target.bindings().isEmpty(),
            "Wire reconstruction must preserve variant-only execution without creating defaults");
        for (var variant : variants) {
            var before = source.resolve(ENTITY, variant.variant()).orElseThrow();
            var after = target.resolve(ENTITY, variant.variant()).orElseThrow();
            h.assertTrue(source.bindingIdentity(before).equals(target.bindingIdentity(after)) && before.selection().equals(after.selection()),
                "Complete binding content identity must survive canonical transfer");
        }
        h.succeed();
    }

    @GameTest
    public void contentIdentityIncludesEveryCanonicalDecision(GameTestHelper h) {
        var base = binding(Map.of());
        var variants = new ArrayList<CollisionBinding>();
        variants.add(base);
        variants.add(binding(Map.of("age", "baby")));
        variants.add(changed(base, Identifier.parse("minecraft:pig"), base.geometry(), base.pose(), base.rootTransform(), base.policy(), base.excludedStates()));
        variants.add(changed(base, ENTITY, new CollisionBinding.Geometry(Identifier.parse("test:other_geometry"), MODEL, Map.of(), AnatomyFilter.DEFAULT), base.pose(), base.rootTransform(), base.policy(), base.excludedStates()));
        variants.add(changed(base, ENTITY, new CollisionBinding.Geometry(base.geometry().engine(), Identifier.parse("test:other_model"), Map.of(), AnatomyFilter.DEFAULT), base.pose(), base.rootTransform(), base.policy(), base.excludedStates()));
        variants.add(changed(base, ENTITY, new CollisionBinding.Geometry(base.geometry().engine(), MODEL, Map.of("alpha", "1"), AnatomyFilter.DEFAULT), base.pose(), base.rootTransform(), base.policy(), base.excludedStates()));
        variants.add(changed(base, ENTITY, new CollisionBinding.Geometry(base.geometry().engine(), MODEL, Map.of(), new AnatomyFilter(.1, .1, .1, Set.of("body"), Set.of())), base.pose(), base.rootTransform(), base.policy(), base.excludedStates()));
        variants.add(changed(base, ENTITY, base.geometry(), new CollisionBinding.Pose(Identifier.parse("test:other_pose"), Map.of(), Set.of()), base.rootTransform(), base.policy(), base.excludedStates()));
        variants.add(changed(base, ENTITY, base.geometry(), new CollisionBinding.Pose(STATIC, Map.of("alpha", "1"), Set.of()), base.rootTransform(), base.policy(), base.excludedStates()));
        variants.add(changed(base, ENTITY, base.geometry(), new CollisionBinding.Pose(STATIC, Map.of(), Set.of("attack")), base.rootTransform(), base.policy(), base.excludedStates()));
        variants.add(changed(base, ENTITY, base.geometry(), base.pose(), Identifier.parse("test:other_root"), base.policy(), base.excludedStates()));
        variants.add(changed(base, ENTITY, base.geometry(), base.pose(), base.rootTransform(), new CollisionPolicy.Patch(Optional.of(false), Optional.empty(), Optional.empty()), base.excludedStates()));
        variants.add(changed(base, ENTITY, base.geometry(), base.pose(), base.rootTransform(), new CollisionPolicy.Patch(Optional.empty(), Optional.of(.75), Optional.empty()), base.excludedStates()));
        variants.add(changed(base, ENTITY, base.geometry(), base.pose(), base.rootTransform(), new CollisionPolicy.Patch(Optional.empty(), Optional.empty(), Optional.of(.2)), base.excludedStates()));
        variants.add(changed(base, ENTITY, base.geometry(), base.pose(), base.rootTransform(), base.policy(), Set.of("sleeping")));
        var identities = new HashSet<Identifier>();
        for (var variant : variants) identities.add(CollisionBindingIdentity.of(variant));
        h.assertTrue(identities.size() == variants.size(), "Every canonical decision must affect identity even when model/pose/root triple stays equal");
        h.assertTrue(CollisionBindingIdentity.of(base).getPath().matches("binding/v1/[0-9a-f]{64}"), "Identity has a versioned content domain");
        h.succeed();
    }

    @GameTest
    public void equivalentMapAndSetPermutationsPreserveIdentity(GameTestHelper h) {
        var forward = new LinkedHashMap<String, String>(); forward.put("alpha", "1"); forward.put("beta", "2");
        var reverse = new LinkedHashMap<String, String>(); reverse.put("beta", "2"); reverse.put("alpha", "1");
        var base = binding(Map.of());
        var first = changed(base, ENTITY, new CollisionBinding.Geometry(base.geometry().engine(), MODEL, forward,
            new AnatomyFilter(.1, .1, .1, Set.of("body", "root"), Set.of())), new CollisionBinding.Pose(STATIC, forward, Set.of("attack", "flower")), base.rootTransform(), base.policy(), Set.of("sleeping", "swimming"));
        var second = changed(base, ENTITY, new CollisionBinding.Geometry(base.geometry().engine(), MODEL, reverse,
            new AnatomyFilter(.1, .1, .1, Set.of("root", "body"), Set.of())), new CollisionBinding.Pose(STATIC, reverse, Set.of("flower", "attack")), base.rootTransform(), base.policy(), Set.of("swimming", "sleeping"));
        h.assertTrue(CollisionBindingIdentity.of(first).equals(CollisionBindingIdentity.of(second)), "Equivalent canonical ordering cannot affect content identity");
        h.succeed();
    }

    @GameTest
    public void malformedTextAndInvalidNondefaultCandidateRetainExactAcceptedPair(GameTestHelper h) {
        var catalog = new WorldAnatomyCatalog(); catalog.replace(models(), List.of(binding(Map.of())));
        var accepted = catalog.snapshot(); var epoch = UUID.randomUUID(); var packets = catalog.preparedPackets(epoch);
        var baby = binding(Map.of("age", "baby"));
        var invalidFilter = changed(baby, ENTITY, new CollisionBinding.Geometry(baby.geometry().engine(), MODEL, Map.of(),
            new AnatomyFilter(0, 0, 0, Set.of("missing_part"), Set.of())), baby.pose(), baby.rootTransform(), baby.policy(), baby.excludedStates());
        var invalidText = changed(baby, ENTITY, new CollisionBinding.Geometry(baby.geometry().engine(), MODEL, Map.of("text", "\ud800"), AnatomyFilter.DEFAULT), baby.pose(), baby.rootTransform(), baby.policy(), baby.excludedStates());
        var invalidCatalogOnly = changed(baby, ENTITY, new CollisionBinding.Geometry(baby.geometry().engine(), Identifier.parse("test:missing"), Map.of("text", "\ud800"), AnatomyFilter.DEFAULT), baby.pose(), baby.rootTransform(), baby.policy(), baby.excludedStates());
        for (var invalid : List.of(invalidFilter, invalidText, invalidCatalogOnly)) {
            boolean rejected = false;
            try { catalog.replace(models(), List.of(binding(Map.of()), invalid)); } catch (IllegalArgumentException expected) { rejected = true; }
            h.assertTrue(rejected && catalog.snapshot() == accepted && catalog.preparedPackets(epoch) == packets,
                "Rejected nondefault preparation must retain exact snapshot and prepared bundle, including catalog-only malformed text");
        }
        h.succeed();
    }

    @GameTest
    public void preparedIndexOwnsItsEntriesAndCompatibilityProjection(GameTestHelper h) {
        var snapshot = new WorldAnatomyCatalog().replace(models(), List.of(binding(Map.of()), binding(Map.of("age", "baby"))));
        var baseline = snapshot.bindings().get(ENTITY);
        var supplied = new ArrayList<WorldAnatomyCatalog.Binding>(); supplied.add(baseline);
        var index = new PreparedBindingIndex(snapshot.catalog(), supplied); supplied.clear();
        h.assertTrue(index.size() == 1 && index.resolve(ENTITY, Map.of()).orElseThrow() == baseline, "Caller mutation must not alter the prepared index");
        var foreign = new WorldAnatomyCatalog.Binding(baseline.selection(), baseline.model(), baseline.poses(), baseline.root(), baseline.legacyPoseProvider());
        h.assertTrue(index.identity(foreign).isEmpty(), "Equal foreign Binding object cannot claim accepted prepared ownership");
        var compatible = new WorldAnatomyCatalog.Snapshot(snapshot.revision(), snapshot.models(), snapshot.posePrograms(), snapshot.catalog(), snapshot.bindings());
        h.assertTrue(compatible.prepared().size() == 1 && compatible.resolve(ENTITY, Map.of("age", "baby")).isEmpty(),
            "Old constructor preserves supplied defaults without inventing missing variants");
        boolean rejected = false;
        try { new PreparedBindingIndex(new CollisionBindingCatalog(List.of()), List.of(baseline)); } catch (IllegalArgumentException expected) { rejected = true; }
        h.assertTrue(rejected, "Foreign canonical selection cannot enter an accepted executable index");
        h.succeed();
    }

    @GameTest
    public void repeatedLookupNeverRebindsEngines(GameTestHelper h) {
        var counter = new AtomicInteger();
        var id = Identifier.parse("scalebrews_test:s46_counted_pose");
        CollisionEngines.registerPose(id, new PoseEngine() {
            @Override public Optional<Map<String, Matrix4f>> evaluate(ModelGeometry geometry, Inputs inputs, Map<String, String> parameters) { return Optional.of(Map.of()); }
            @Override public Optional<Bound> bind(ModelGeometry geometry, Map<String, String> parameters, Set<String> channels, Resources resources) {
                counter.incrementAndGet(); return PoseEngine.super.bind(geometry, parameters, channels, resources);
            }
        });
        var base = binding(Map.of("age", "baby"));
        var counted = changed(base, ENTITY, base.geometry(), new CollisionBinding.Pose(id, Map.of(), Set.of()), base.rootTransform(), base.policy(), base.excludedStates());
        var snapshot = new WorldAnatomyCatalog().replace(models(), List.of(counted));
        int prepared = counter.get(); var chosen = snapshot.resolve(ENTITY, counted.variant()).orElseThrow(); var identity = snapshot.bindingIdentity(chosen).orElseThrow();
        for (int i = 0; i < 2000; i++) {
            h.assertTrue(snapshot.resolve(ENTITY, counted.variant()).orElseThrow() == chosen && snapshot.resolveIdentity(ENTITY, identity).orElseThrow() == chosen,
                "Repeated lookups must retain the exact prepared binding");
            h.assertTrue(snapshot.bindingIdentity(chosen).orElseThrow().equals(identity), "Prepared ID lookup is stable");
        }
        h.assertTrue(prepared > 0 && counter.get() == prepared, "Prepared selection/ID lookups cannot recompile pose engines");
        h.succeed();
    }

    private static CollisionBinding binding(Map<String, String> variant) {
        return new CollisionBinding(1, ENTITY, variant, new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART, MODEL, Map.of(), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(STATIC, Map.of(), Set.of()), BuiltInRootTransformProviders.ENTITY_ROOT, CollisionPolicy.Patch.EMPTY, Set.of());
    }
    private static CollisionBinding changed(CollisionBinding base, Identifier entity, CollisionBinding.Geometry geometry, CollisionBinding.Pose pose,
                                           Identifier root, CollisionPolicy.Patch policy, Set<String> excluded) {
        return new CollisionBinding(base.schemaVersion(), entity, base.variant(), geometry, pose, root, policy, excluded);
    }
    private static Map<String, ModelGeometry> models() {
        var model = new ModelGeometry(1, ENTITY.toString(), "1", List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("body", "root", List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)), ModelGeometry.values(new Matrix4f()));
        return Map.of(MODEL.toString(), model);
    }
}
