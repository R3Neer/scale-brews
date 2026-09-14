package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/**
 * Permanent original-source differential oracle for S18's canonical procedural vanilla pose engines.
 * Uses real Minecraft 26.2 models and canonical model source ids, so strict source gates remain exercised.
 */
public final class S18VanillaProceduralSemanticClientTests implements FabricClientGameTest {
    private static final AnatomyFilter ALL_MATERIAL = new AnatomyFilter(0, 0, 0);
    private static final double VERTEX_EPS = 3e-5;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            for (int tick = 0; tick < 80; tick++) {
                float walk = tick * .37f;
                float speed = (tick % 20) / 20f;
                float age = tick;
                float yaw = tick % 80 - 40;
                float pitch = tick % 50 - 25;

                cow(tick, walk, speed, age, yaw, pitch);
                chicken(tick, walk, speed, age, yaw, pitch);
                llama(tick, walk, speed, age, yaw, pitch);
                villager(tick, walk, speed, age, yaw, pitch);
                golem(tick, walk, speed, age, yaw, pitch);
                ghast(tick, walk, speed, age, yaw, pitch);
                feline(tick, walk, speed, age, yaw, pitch);
                equine(tick, walk, speed, age, yaw, pitch);
                bee(tick, walk, speed, age, yaw, pitch);
                player(tick, walk, speed, age, yaw, pitch);
            }
            System.out.println("S18_VANILLA_PROCEDURAL PASS 800 original-source Minecraft 26.2 pose comparisons");
        });
    }

    private static void cow(int tick, float walk, float speed, float age, float yaw, float pitch) {
        var root = net.minecraft.client.model.animal.cow.CowModel.createBodyLayer().bakeRoot();
        var model = new net.minecraft.client.model.animal.cow.CowModel(root);
        var state = new net.minecraft.client.renderer.entity.state.LivingEntityRenderState();
        common(state, walk, speed, age, yaw, pitch);
        check("cow " + tick, "minecraft:cow", "scalebrews:quadruped", root, model, state,
            new PoseEngine.Inputs(walk, speed, age, yaw, pitch, true), Set.of());
    }

    private static void chicken(int tick, float walk, float speed, float age, float yaw, float pitch) {
        var root = net.minecraft.client.model.animal.chicken.AdultChickenModel.createBodyLayer().bakeRoot();
        var model = new net.minecraft.client.model.animal.chicken.AdultChickenModel(root);
        var state = new net.minecraft.client.renderer.entity.state.ChickenRenderState();
        common(state, walk, speed, age, yaw, pitch);
        state.flap = tick * .41f;
        state.flapSpeed = (tick % 10) / 10f;
        check("chicken " + tick, "minecraft:chicken", "scalebrews:chicken", root, model, state,
            new PoseEngine.Inputs(walk, speed, age, yaw, pitch, true,
                Map.of("flap", state.flap, "flap_speed", state.flapSpeed)), Set.of());
    }

    private static void llama(int tick, float walk, float speed, float age, float yaw, float pitch) {
        var root = net.minecraft.client.model.animal.llama.LlamaModel.createBodyLayer(CubeDeformation.NONE).bakeRoot();
        var model = new net.minecraft.client.model.animal.llama.LlamaModel(root);
        var state = new net.minecraft.client.renderer.entity.state.LlamaRenderState();
        common(state, walk, speed, age, yaw, pitch);
        check("llama " + tick, "minecraft:llama", "scalebrews:quadruped", root, model, state,
            new PoseEngine.Inputs(walk, speed, age, yaw, pitch, true), Set.of("right_chest", "left_chest"));
    }

    private static void villager(int tick, float walk, float speed, float age, float yaw, float pitch) {
        var root = LayerDefinition.create(net.minecraft.client.model.npc.VillagerModel.createBodyModel(), 64, 64).bakeRoot();
        var model = new net.minecraft.client.model.npc.VillagerModel(root);
        var state = new net.minecraft.client.renderer.entity.state.VillagerRenderState();
        common(state, walk, speed, age, yaw, pitch);
        state.isUnhappy = tick % 3 == 0;
        check("villager " + tick, "minecraft:villager", "scalebrews:villager", root, model, state,
            new PoseEngine.Inputs(walk, speed, age, yaw, pitch, true,
                Map.of("unhappy", state.isUnhappy ? 1f : 0f)), Set.of("hat", "hat_rim"));
    }

    private static void golem(int tick, float walk, float speed, float age, float yaw, float pitch) {
        var root = net.minecraft.client.model.animal.golem.IronGolemModel.createBodyLayer().bakeRoot();
        var model = new net.minecraft.client.model.animal.golem.IronGolemModel(root);
        var state = new net.minecraft.client.renderer.entity.state.IronGolemRenderState();
        common(state, walk, speed, age, yaw, pitch);
        state.attackTicksRemaining = tick % 4 == 0 ? tick % 10 : 0;
        state.offerFlowerTick = tick % 4 == 1 ? tick % 70 : 0;
        check("iron_golem " + tick, "minecraft:iron_golem", "scalebrews:iron_golem", root, model, state,
            new PoseEngine.Inputs(walk, speed, age, yaw, pitch, true,
                Map.of("attack", state.attackTicksRemaining, "flower", (float)state.offerFlowerTick)), Set.of());
    }

    private static void ghast(int tick, float walk, float speed, float age, float yaw, float pitch) {
        var root = net.minecraft.client.model.monster.ghast.GhastModel.createBodyLayer().bakeRoot();
        var model = new net.minecraft.client.model.monster.ghast.GhastModel(root);
        var state = new net.minecraft.client.renderer.entity.state.GhastRenderState();
        common(state, walk, speed, age, yaw, pitch);
        check("ghast " + tick, "minecraft:ghast", "scalebrews:ghast", root, model, state,
            new PoseEngine.Inputs(walk, speed, age, yaw, pitch, true), Set.of());
    }

    private static void feline(int tick, float walk, float speed, float age, float yaw, float pitch) {
        var root = LayerDefinition.create(net.minecraft.client.model.animal.feline.AdultFelineModel.createBodyMesh(CubeDeformation.NONE), 64, 32).bakeRoot();
        var model = new net.minecraft.client.model.animal.feline.AdultFelineModel(root);
        var state = new net.minecraft.client.renderer.entity.state.FelineRenderState();
        common(state, walk, speed, age, yaw, pitch);
        state.ageScale = 1f;
        int mode = tick % 6;
        state.isCrouching = mode == 1;
        state.isSprinting = mode == 2;
        state.isSitting = mode == 3;
        state.lieDownAmount = mode == 4 ? .65f : 0f;
        state.lieDownAmountTail = mode == 4 ? .4f : 0f;
        state.relaxStateOneAmount = mode == 5 ? .55f : 0f;
        check("feline " + tick, "minecraft:cat", "scalebrews:feline", root, model, state,
            new PoseEngine.Inputs(walk, speed, age, yaw, pitch, true, Map.of(
                "crouching", state.isCrouching ? 1f : 0f,
                "sprinting", state.isSprinting ? 1f : 0f,
                "sitting", state.isSitting ? 1f : 0f,
                "lie", state.lieDownAmount,
                "lie_tail", state.lieDownAmountTail,
                "relax", state.relaxStateOneAmount,
                "age_scale", state.ageScale)), Set.of());
    }

    private static void equine(int tick, float walk, float speed, float age, float yaw, float pitch) {
        var root = LayerDefinition.create(net.minecraft.client.model.animal.equine.AbstractEquineModel.createBodyMesh(CubeDeformation.NONE), 64, 64).bakeRoot();
        var model = new net.minecraft.client.model.animal.equine.HorseModel(root);
        var state = new net.minecraft.client.renderer.entity.state.EquineRenderState();
        common(state, walk, speed, age, yaw, pitch);
        state.ageScale = 1f;
        int mode = tick % 5;
        state.eatAnimation = mode == 1 ? .65f : 0f;
        state.standAnimation = mode == 2 ? .7f : 0f;
        state.feedingAnimation = mode == 3 ? .45f : 0f;
        state.isInWater = mode == 4;
        state.animateTail = tick % 2 == 0;
        check("equine " + tick, "minecraft:horse", "scalebrews:equine", root, model, state,
            new PoseEngine.Inputs(walk, speed, age, yaw, pitch, true, Map.of(
                "eat", state.eatAnimation,
                "stand", state.standAnimation,
                "mouth", state.feedingAnimation,
                "tail", state.animateTail ? 1f : 0f,
                "water", state.isInWater ? 1f : 0f,
                "age_scale", state.ageScale)),
            Set.of("left_bit", "right_bit", "left_rein", "right_rein", "head_saddle", "mouth_saddle_wrap"));
    }

    private static void bee(int tick, float walk, float speed, float age, float yaw, float pitch) {
        var root = net.minecraft.client.model.animal.bee.AdultBeeModel.createBodyLayer().bakeRoot();
        var model = new net.minecraft.client.model.animal.bee.AdultBeeModel(root);
        var state = new net.minecraft.client.renderer.entity.state.BeeRenderState();
        common(state, walk, speed, age, yaw, pitch);
        state.isOnGround = tick % 4 == 0;
        state.isAngry = tick % 4 == 1;
        state.rollAmount = (tick % 5) / 5f;
        state.hasStinger = false;
        check("bee " + tick, "minecraft:bee", "scalebrews:bee", root, model, state,
            new PoseEngine.Inputs(walk, speed, age, yaw, pitch, true, Map.of(
                "on_ground", state.isOnGround ? 1f : 0f,
                "angry", state.isAngry ? 1f : 0f,
                "roll", state.rollAmount)), Set.of("stinger"));
    }

    private static void player(int tick, float walk, float speed, float age, float yaw, float pitch) {
        var root = LayerDefinition.create(net.minecraft.client.model.player.PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64).bakeRoot();
        var model = new net.minecraft.client.model.player.PlayerModel(root, false);
        var state = new net.minecraft.client.renderer.entity.state.AvatarRenderState();
        common(state, walk, speed, age, yaw, pitch);
        state.speedValue = 1f;
        check("player " + tick, "minecraft:player_wide", "scalebrews:player_walking", root, model, state,
            new PoseEngine.Inputs(walk, speed, age, yaw, pitch, true), Set.of());
    }

    private static void common(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,
                               float walk, float speed, float age, float yaw, float pitch) {
        state.walkAnimationPos = walk;
        state.walkAnimationSpeed = speed;
        state.ageInTicks = age;
        state.yRot = yaw;
        state.xRot = pitch;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void check(String label, String source, String engineText, ModelPart root,
                              net.minecraft.client.model.EntityModel model, Object state,
                              PoseEngine.Inputs inputs, Set<String> excluded) {
        var geometry = GeometryExtractor.vanilla(source, "26.2", root, excluded);
        model.setupAnim(state);
        var engine = CollisionEngines.pose(Identifier.parse(engineText)).orElseThrow();
        var replacements = engine.evaluate(geometry, inputs, Map.of())
            .orElseThrow(() -> new AssertionError("Canonical engine rejected original-source fixture: " + label));
        var predicted = geometry.evaluate(new Matrix4f(), replacements, ALL_MATERIAL);
        var actual = GeometryExtractor.vanilla(source, "26.2", root, excluded)
            .evaluate(new Matrix4f(), Map.of(), ALL_MATERIAL);
        compare(predicted, actual, label);
    }

    private static void compare(Map<String, ConvexBox> predicted, Map<String, ConvexBox> actual, String label) {
        if (!predicted.keySet().equals(actual.keySet()))
            throw new AssertionError("Animated piece mismatch: " + label + " predicted=" + predicted.keySet() + " actual=" + actual.keySet());
        for (String key : actual.keySet()) for (int i = 0; i < 8; i++) {
            double d2 = predicted.get(key).vertices().get(i).distanceToSqr(actual.get(key).vertices().get(i));
            if (d2 > VERTEX_EPS * VERTEX_EPS) throw new AssertionError("Canonical PoseEngine differs from original Minecraft 26.2: "
                + label + " piece=" + key + " vertex=" + i + " d2=" + d2);
        }
    }
}
