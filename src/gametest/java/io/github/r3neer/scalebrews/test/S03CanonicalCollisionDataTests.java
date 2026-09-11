package io.github.r3neer.scalebrews.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionCodecs;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** G1/S03 versioned canonical data and legacy decoder acceptance. */
public final class S03CanonicalCollisionDataTests {
    @GameTest
    public void canonicalBindingRoundTripsWithoutLegacyPlaneSemantics(GameTestHelper h) {
        var binding = fixtureBinding();
        var json = CollisionCodecs.BINDING.encodeStart(JsonOps.INSTANCE, binding).getOrThrow();
        var object = json.getAsJsonObject();
        h.assertTrue(object.get("schema_version").getAsInt() == 1, "Binding schema version must be explicit");
        h.assertTrue(!object.has("surfaces") && !object.has("automatic_surfaces"), "Canonical binding must not reuse legacy plane keys");
        var decoded = CollisionCodecs.BINDING.parse(JsonOps.INSTANCE, json).getOrThrow();
        h.assertTrue(decoded.equals(binding), "Canonical binding codec must round-trip deterministically");
        object.addProperty("schema_version", 99);
        h.assertTrue(CollisionCodecs.BINDING.parse(JsonOps.INSTANCE, object).result().isEmpty(), "Unknown schema versions fail closed");
        h.succeed();
    }

    @GameTest
    public void canonicalCodecsRejectInjectedLegacyKeys(GameTestHelper h) {
        JsonObject binding = CollisionCodecs.BINDING.encodeStart(JsonOps.INSTANCE, fixtureBinding()).getOrThrow().getAsJsonObject();

        var withLegacySurfaces = binding.deepCopy();
        withLegacySurfaces.add("surfaces", new JsonArray());
        assertCodecError(h, CollisionCodecs.BINDING, withLegacySurfaces,
            "Canonical binding must reject the legacy surfaces key instead of silently ignoring it");

        var withLegacyAnatomy = binding.deepCopy();
        withLegacyAnatomy.add("anatomy", new JsonObject());
        assertCodecError(h, CollisionCodecs.BINDING, withLegacyAnatomy,
            "Canonical binding must reject the legacy anatomy key instead of silently creating dual-format data");

        JsonObject policy = CollisionCodecs.POLICY.encodeStart(JsonOps.INSTANCE, CollisionPolicy.DEFAULT).getOrThrow().getAsJsonObject();
        var withAutomaticSurfaces = policy.deepCopy();
        withAutomaticSurfaces.addProperty("automatic_surfaces", true);
        assertCodecError(h, CollisionCodecs.POLICY, withAutomaticSurfaces,
            "Canonical policy must reject legacy automatic_surfaces instead of silently reviving top-surface semantics");
        h.succeed();
    }

    @GameTest
    public void oversizedCanonicalCollectionsReturnCodecErrors(GameTestHelper h) {
        JsonObject base = CollisionCodecs.BINDING.encodeStart(JsonOps.INSTANCE, fixtureBinding()).getOrThrow().getAsJsonObject();

        var oversizedVariant = base.deepCopy();
        var variants = new JsonObject();
        for (int n = 0; n < 33; n++) variants.addProperty("v" + n, "value");
        oversizedVariant.add("variant", variants);
        assertCodecError(h, CollisionCodecs.BINDING, oversizedVariant, "33 variant selectors must be a codec error, not a constructor exception");

        var oversizedVariantValue = base.deepCopy();
        var longVariant = new JsonObject();
        longVariant.addProperty("kind", "x".repeat(257));
        oversizedVariantValue.add("variant", longVariant);
        assertCodecError(h, CollisionCodecs.BINDING, oversizedVariantValue, "Variant values longer than 256 characters must fail through DataResult");

        var oversizedChannels = base.deepCopy();
        var channels = new JsonArray();
        for (int n = 0; n < 65; n++) channels.add("channel" + n);
        oversizedChannels.getAsJsonObject("pose").add("channels", channels);
        assertCodecError(h, CollisionCodecs.BINDING, oversizedChannels, "65 pose channels must fail through DataResult");

        var oversizedStates = base.deepCopy();
        var states = new JsonArray();
        for (int n = 0; n < 65; n++) states.add("state" + n);
        oversizedStates.add("excluded_states", states);
        assertCodecError(h, CollisionCodecs.BINDING, oversizedStates, "65 excluded states must fail through DataResult");

        JsonObject policy = CollisionCodecs.POLICY.encodeStart(JsonOps.INSTANCE, CollisionPolicy.DEFAULT).getOrThrow().getAsJsonObject();
        var categories = new JsonObject();
        for (int n = 0; n < 257; n++) categories.add("category" + n, new JsonObject());
        var oversizedCategories = policy.deepCopy();
        oversizedCategories.add("categories", categories);
        assertCodecError(h, CollisionCodecs.POLICY, oversizedCategories, "257 category policies must fail through DataResult");

        var supports = new JsonObject();
        for (int n = 0; n < 4097; n++) supports.add("fixture:support_" + n, new JsonObject());
        var oversizedSupports = policy.deepCopy();
        oversizedSupports.add("supports", supports);
        assertCodecError(h, CollisionCodecs.POLICY, oversizedSupports, "4097 support policies must fail through DataResult");

        var modelIdFilter = new AnatomyFilter(.01, .01, .00001, Set.of("root/left:piece detail"), Set.of("gear#overlay"));
        var encodedFilter = CollisionCodecs.FILTER.encodeStart(JsonOps.INSTANCE, modelIdFilter).getOrThrow();
        h.assertTrue(CollisionCodecs.FILTER.parse(JsonOps.INSTANCE, encodedFilter).getOrThrow().equals(modelIdFilter),
            "Filter codec must preserve the ModelGeometry id domain rather than canonical-key syntax");
        h.succeed();
    }

    @GameTest
    public void constructorAcceptedBoundaryDataRoundTripsCanonically(GameTestHelper h) {
        var variant = new HashMap<String, String>();
        for (int n = 0; n < 32; n++) variant.put(n == 0 ? "v".repeat(64) : "v" + n, "x".repeat(256));

        var geometryParameters = new HashMap<String, String>();
        var poseParameters = new HashMap<String, String>();
        for (int n = 0; n < 128; n++) {
            String key = n == 0 ? "g".repeat(64) : "g" + n;
            geometryParameters.put(key, "g".repeat(1024));
            poseParameters.put(n == 0 ? "p".repeat(64) : "p" + n, "p".repeat(1024));
        }

        var channels = new HashSet<String>();
        var excludedStates = new HashSet<String>();
        for (int n = 0; n < 64; n++) {
            channels.add(n == 0 ? "c".repeat(64) : "channel" + n);
            excludedStates.add(n == 0 ? "s".repeat(64) : "state" + n);
        }

        var include = new HashSet<String>();
        var exclude = new HashSet<String>();
        include.add("i".repeat(256));
        exclude.add("e".repeat(256));
        for (int n = 1; n < 2048; n++) {
            include.add("include/part:" + n);
            exclude.add("exclude/part:" + n);
        }
        var filter = new AnatomyFilter(.01, .5, .5, include, exclude);

        var binding = new CollisionBinding(CollisionBinding.SCHEMA_VERSION, Identifier.parse("fixture:boundary"), variant,
            new CollisionBinding.Geometry(Identifier.parse("fixture:geometry"), Identifier.parse("fixture:model"), geometryParameters, filter),
            new CollisionBinding.Pose(Identifier.parse("fixture:pose"), poseParameters, channels),
            Identifier.parse("fixture:root"), CollisionPolicy.Patch.EMPTY, excludedStates);
        var bindingJson = CollisionCodecs.BINDING.encodeStart(JsonOps.INSTANCE, binding).getOrThrow();
        var decodedBinding = CollisionCodecs.BINDING.parse(JsonOps.INSTANCE, bindingJson).getOrThrow();
        h.assertTrue(decodedBinding.equals(binding),
            "Every constructor-accepted canonical binding at its declared size/length boundaries must codec round-trip");

        var categories = new HashMap<String, CollisionPolicy.Patch>();
        for (int n = 0; n < 256; n++) categories.put(n == 0 ? "k".repeat(64) : "category" + n, CollisionPolicy.Patch.EMPTY);
        var supports = new HashMap<Identifier, CollisionPolicy.Patch>();
        for (int n = 0; n < 4096; n++) supports.put(Identifier.parse("fixture:support_" + n), CollisionPolicy.Patch.EMPTY);
        var policy = new CollisionPolicy(CollisionPolicy.SCHEMA_VERSION, new CollisionPolicy.Rule(true, 1024, 1), categories, supports);
        var policyJson = CollisionCodecs.POLICY.encodeStart(JsonOps.INSTANCE, policy).getOrThrow();
        var decodedPolicy = CollisionCodecs.POLICY.parse(JsonOps.INSTANCE, policyJson).getOrThrow();
        h.assertTrue(decodedPolicy.equals(policy),
            "Every constructor-accepted canonical policy at its declared collection boundaries must codec round-trip");
        h.succeed();
    }

    @GameTest
    public void policyPrecedenceIsGlobalCategorySupportThenProfile(GameTestHelper h) {
        var cow = Identifier.parse("minecraft:cow");
        var policy = new CollisionPolicy(1, new CollisionPolicy.Rule(true, .85, .6),
            Map.of("players", new CollisionPolicy.Patch(Optional.empty(), Optional.of(.8), Optional.empty())),
            Map.of(cow, new CollisionPolicy.Patch(Optional.empty(), Optional.empty(), Optional.of(.4))));
        var profile = new CollisionPolicy.Patch(Optional.empty(), Optional.of(.7), Optional.of(.5));
        var resolved = policy.resolve("players", cow, profile);
        h.assertTrue(resolved.enabled() && Math.abs(resolved.maxWidthRatio() - .7) < 1e-9 && Math.abs(resolved.friction() - .5) < 1e-9,
            "Profile must override support/category while inheriting unspecified values");
        var disabled = policy.resolve("players", cow, new CollisionPolicy.Patch(Optional.of(false), Optional.empty(), Optional.empty()));
        h.assertTrue(!disabled.enabled() && Math.abs(disabled.maxWidthRatio() - .8) < 1e-9 && Math.abs(disabled.friction() - .4) < 1e-9,
            "Policy layers must resolve independently and in documented order");
        h.succeed();
    }

    @GameTest
    public void policyRejectsNullSupportEntriesBeforeCanonicalOrdering(GameTestHelper h) {
        var invalid = new HashMap<Identifier, CollisionPolicy.Patch>();
        invalid.put(null, CollisionPolicy.Patch.EMPTY);
        boolean rejected = false;
        try { new CollisionPolicy(1, CollisionPolicy.DEFAULT.defaults(), Map.of(), invalid); }
        catch (IllegalArgumentException expected) { rejected = true; }
        h.assertTrue(rejected, "Invalid support keys fail explicitly before deterministic sorting");
        h.succeed();
    }

    @GameTest
    public void legacyDecoderSeparatesAnatomyFromOneSidedPlanes(GameTestHelper h) {
        var entity = Identifier.parse("minecraft:cow");
        var anatomyDefinition = new AnatomyDefinition(Identifier.parse("minecraft:cow"), Identifier.parse("scalebrews:quadruped"), AnatomyFilter.DEFAULT);
        var anatomy = new PlatformDefinition(entity, true, .6, Optional.of(.75), List.of(), Optional.of(anatomyDefinition));
        var decodedAnatomy = LegacyCollisionData.decode(anatomy);
        h.assertTrue(decodedAnatomy.binding().isPresent() && decodedAnatomy.legacyPlanes().isEmpty(),
            "Legacy anatomy migrates to canonical binding only");
        h.assertTrue(decodedAnatomy.binding().orElseThrow().geometry().engine().equals(LegacyCollisionData.PRECOMPUTED_GEOMETRY),
            "Migration selects a named compatibility engine instead of inferring a model technology");

        var surface = new PlatformDefinition.Surface("back", 0, 1.25, 0, .75, 1.0, Optional.empty());
        var plane = new PlatformDefinition(entity, true, .6, Optional.empty(), List.of(surface), Optional.empty());
        var decodedPlane = LegacyCollisionData.decode(plane);
        h.assertTrue(decodedPlane.binding().isEmpty() && decodedPlane.legacyPlanes().isPresent(),
            "Legacy plane data must stay an explicit one-sided plane migration record, never anatomy");
        h.assertTrue(decodedPlane.legacyPlanes().orElseThrow().planes().getFirst().id().equals("back"),
            "Legacy plane decoder preserves explicit authored surfaces");

        boolean neitherRejected = false;
        try { LegacyCollisionData.decode(new PlatformDefinition(entity, true, .6, Optional.empty(), List.of(), Optional.empty())); }
        catch (IllegalArgumentException expected) { neitherRejected = true; }
        h.assertTrue(neitherRejected, "Legacy migration rejects a profile with neither anatomy nor explicit planes");

        boolean bothRejected = false;
        try { LegacyCollisionData.decode(new PlatformDefinition(entity, true, .6, Optional.of(.75), List.of(surface), Optional.of(anatomyDefinition))); }
        catch (IllegalArgumentException expected) { bothRejected = true; }
        h.assertTrue(bothRejected, "Legacy migration rejects anatomy and one-sided planes being assigned dual semantics in one profile");
        h.succeed();
    }

    private static CollisionBinding fixtureBinding() {
        return new CollisionBinding(1, Identifier.parse("minecraft:cow"), Map.of("coat", "brown"),
            new CollisionBinding.Geometry(Identifier.parse("fixture:model_part"), Identifier.parse("minecraft:cow"), Map.of("family", "cow"), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(Identifier.parse("fixture:keyframes"), Map.of(), Set.of("walk", "head_yaw")),
            Identifier.parse("fixture:entity_root"), CollisionPolicy.Patch.EMPTY, Set.of("sleeping"));
    }

    private static <T> void assertCodecError(GameTestHelper h, Codec<T> codec, JsonElement json, String message) {
        final boolean rejected;
        try {
            rejected = codec.parse(JsonOps.INSTANCE, json).result().isEmpty();
        } catch (RuntimeException escaped) {
            throw new AssertionError(message + "; exception escaped codec: " + escaped, escaped);
        }
        h.assertTrue(rejected, message);
    }
}
