package io.github.r3neer.scalebrews.collision.internal;

import com.google.gson.JsonParser;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import io.github.r3neer.scalebrews.collision.pose.PoseProviders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Red-first S18 holdouts for canonical pose-engine and keyframe-program authority. */
public final class S18PoseEngineAuthorityTests {
    private static final List<String> VANILLA_ENGINES = List.of(
        "scalebrews:player_walking",
        "scalebrews:quadruped",
        "scalebrews:chicken",
        "scalebrews:villager",
        "scalebrews:iron_golem",
        "scalebrews:ghast",
        "scalebrews:feline",
        "scalebrews:equine",
        "scalebrews:bee",
        "scalebrews:static"
    );

    @GameTest
    public void vanillaPoseFamiliesAreCanonicalPoseEngines(GameTestHelper h) {
        for (String text : VANILLA_ENGINES) {
            var id = Identifier.parse(text);
            h.assertTrue(CollisionEngines.pose(id).isPresent(),
                "Reusable vanilla pose family must be owned by CollisionEngines.pose: " + id);
        }
        h.succeed();
    }

    @GameTest
    public void liveAuthorityUsesPoseEngineInputsInsteadOfLegacyProviderInputs(GameTestHelper h) {
        var inputComponent = java.util.Arrays.stream(AnatomyPosePayload.class.getRecordComponents())
            .filter(component -> component.getName().equals("inputs"))
            .findFirst().orElseThrow();
        h.assertTrue(inputComponent.getType() == PoseEngine.Inputs.class,
            "Wire/live pose authority must converge on PoseEngine.Inputs instead of PoseProvider.Inputs");

        var sampleInput = java.util.Arrays.stream(AnatomyPoseHistory.Sample.class.getRecordComponents())
            .filter(component -> component.getName().equals("inputs"))
            .findFirst().orElseThrow();
        h.assertTrue(sampleInput.getType() == PoseEngine.Inputs.class,
            "Pose history must retain the same canonical PoseEngine.Inputs DTO as the wire/runtime authority");
        h.succeed();
    }

    @GameTest
    public void legacyPoseProvidersExposeOnlyCompatibilityLookup(GameTestHelper h) {
        boolean publicRegister = java.util.Arrays.stream(PoseProviders.class.getDeclaredMethods())
            .anyMatch(method -> method.getName().equals("register") && Modifier.isPublic(method.getModifiers()));
        h.assertTrue(!publicRegister,
            "PoseProviders must not retain a public second registry once CollisionEngines owns pose behavior");

        boolean independentProviderMap = java.util.Arrays.stream(PoseProviders.class.getDeclaredFields())
            .anyMatch(field -> java.util.Map.class.isAssignableFrom(field.getType()) && field.getName().toLowerCase().contains("provider"));
        h.assertTrue(!independentProviderMap,
            "PoseProviders compatibility adapter must not retain an independent provider-behavior map");
        h.succeed();
    }

    @GameTest
    public void legacyQuadrupedAdapterMatchesCanonicalEngineExactly(GameTestHelper h) {
        var id = Identifier.parse("scalebrews:quadruped");
        var engine = CollisionEngines.pose(id).orElse(null);
        h.assertTrue(engine != null, "Quadruped must exist as a canonical PoseEngine before legacy parity can be checked");
        var provider = PoseProviders.find(id).orElse(null);
        h.assertTrue(provider != null, "S16 legacy bridge still needs a compatibility lookup for quadruped during migration");

        var geometry = quadrupedGeometry();
        var canonicalInputs = new PoseEngine.Inputs(1.25f, .7f, 42f, 17f, -8f, true, Map.of());
        var legacyInputs = new PoseProvider.Inputs(1.25f, .7f, 42f, 17f, -8f, true, Map.of());
        var canonical = engine.evaluate(geometry, canonicalInputs, Map.of()).orElse(null);
        var legacy = provider.evaluate(geometry, legacyInputs).orElse(null);
        h.assertTrue(canonical != null && legacy != null && sameMatrices(canonical, legacy),
            "Legacy compatibility lookup must delegate to the canonical engine, not own a divergent quadruped formula");
        h.succeed();
    }

    @GameTest
    public void keyframeEngineIsCommonAndContainsNoClientReferences(GameTestHelper h) {
        var id = Identifier.parse("scalebrews:mojang_keyframes");
        var engine = CollisionEngines.pose(id).orElse(null);
        h.assertTrue(engine != null, "General Mojang keyframe PoseEngine must be registered in common/dedicated");
        var type = engine.getClass();
        String resource = type.getSimpleName() + ".class";
        try (var input = type.getResourceAsStream(resource)) {
            h.assertTrue(input != null, "Could not inspect keyframe PoseEngine bytecode");
            String constantPool = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
            h.assertTrue(!constantPool.contains("net/minecraft/client/"),
                "Common keyframe engine bytecode must not reference net.minecraft.client");
            h.assertTrue(!constantPool.contains("AnimationDefinition"),
                "Common keyframe engine must evaluate an exported neutral program, not AnimationDefinition itself");
            h.assertTrue(!constantPool.contains("com/mojang/blaze3d/"),
                "Common keyframe engine bytecode must not reference renderer classes");
        } catch (IOException unreadable) {
            throw new AssertionError("Could not inspect keyframe PoseEngine bytecode", unreadable);
        }
        h.succeed();
    }

    @GameTest
    public void protocolV5CarriesPoseProgramsInsideTheAtomicBundle(GameTestHelper h) {
        h.assertTrue(AnatomyApi.PROTOCOL_VERSION == 5,
            "Adding authoritative pose programs to the synchronized revision is an incompatible wire change and must own protocol v5");
        h.assertTrue(!AnatomyApi.compatible(4, 0), "A v5 endpoint must not advertise protocol-v4 compatibility");

        var packets = AnatomyCatalogTransfer.encode(UUID.randomUUID(), 1, Map.of(), List.of());
        var complete = new ByteArrayOutputStream();
        for (var packet : packets) complete.writeBytes(packet.fragment());
        var json = JsonParser.parseString(complete.toString(StandardCharsets.UTF_8)).getAsJsonObject();
        h.assertTrue(json.has("pose_programs"),
            "Protocol-v5 authoritative bundle must carry pose_programs atomically with models and bindings");
        h.assertTrue(json.has("models") && json.has("bindings"),
            "Protocol-v5 pose-program extension must preserve the canonical model/binding bundle");
        h.succeed();
    }

    private static ModelGeometry quadrupedGeometry() {
        var identity = ModelGeometry.values(new Matrix4f());
        return new ModelGeometry(1, "minecraft:cow", "26.2",
            List.of(
                new ModelGeometry.Part("root", null, identity),
                new ModelGeometry.Part("root/head", "root", identity),
                new ModelGeometry.Part("root/right_hind_leg", "root", identity),
                new ModelGeometry.Part("root/left_hind_leg", "root", identity),
                new ModelGeometry.Part("root/right_front_leg", "root", identity),
                new ModelGeometry.Part("root/left_front_leg", "root", identity)
            ),
            List.of(new ModelGeometry.Piece("body", "root", List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)),
            identity);
    }

    private static boolean sameMatrices(Map<String, Matrix4f> left, Map<String, Matrix4f> right) {
        if (!left.keySet().equals(right.keySet())) return false;
        for (var id : left.keySet()) if (!left.get(id).equals(right.get(id))) return false;
        return true;
    }
}
