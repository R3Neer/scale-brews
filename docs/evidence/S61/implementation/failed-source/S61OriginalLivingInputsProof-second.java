package io.github.r3neer.scalebrews.test;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.collision.preparation.OriginalFinitePosePreparation;
import io.github.r3neer.scalebrews.client.collision.preparation.OriginalFiniteResourcePreparation;
import io.github.r3neer.scalebrews.client.collision.preparation.OriginalLivingInputPreparation;
import io.github.r3neer.scalebrews.client.mixin.ModelPartAccess;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.*;
import java.nio.file.Files;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/** IMPLEMENTER: all expected scalars/matrices are captured from complete original methods. */
public final class S61OriginalLivingInputsProof implements FabricClientGameTest {
    private record Sample(String type, int index, LivingPoseInputSource.Program program, Map<String, Float> expected,
                          Map<String, List<Float>> matrices) {}
    private record Evidence(OriginalFiniteResourcePreparation.Prepared model, List<OriginalLivingInputPreparation.Recipe> recipes,
                            List<Sample> samples) {}
    private static void check(boolean ok, String why) { if (!ok) throw new AssertionError("S61 IMPL " + why); }
    private static List<OriginalLivingInputPreparation.Input> schema() {
        var result = new ArrayList<OriginalLivingInputPreparation.Input>();
        result.add(new OriginalLivingInputPreparation.Input("field.age", EntityRenderState.class.getName(), "ageInTicks", "F"));
        for (String name : List.of("walkAnimationPos", "walkAnimationSpeed", "xRot", "yRot", "bodyRot", "scale", "ageScale"))
            result.add(new OriginalLivingInputPreparation.Input("field." + name.toLowerCase(Locale.ROOT), LivingEntityRenderState.class.getName(), name, "F"));
        for (String name : List.of("isBaby", "isInWater", "isUpsideDown", "isFullyFrozen", "isAutoSpinAttack"))
            result.add(new OriginalLivingInputPreparation.Input("field." + name.toLowerCase(Locale.ROOT), LivingEntityRenderState.class.getName(), name, "Z"));
        return result;
    }
    @Override @SuppressWarnings({"rawtypes", "unchecked"})
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(client -> {
                var gson = new GsonBuilder().setPrettyPrinting().create();
                var recipes = new ArrayList<OriginalLivingInputPreparation.Recipe>(); var samples = new ArrayList<Sample>();
                var model = new CowModel(CowModel.createBodyLayer().bakeRoot());
                var finite = OriginalFinitePosePreparation.inspect(model); check(finite.admitted(), finite.absence());
                var exported = OriginalFiniteResourcePreparation.export(finite.prepared(), "s61_proof");
                var geometry = exported.numericPrograms().values().iterator().next().geometry();
                PoseEngine.Resources resources = new PoseEngine.Resources() {
                    @Override public Optional<PoseProgram> program(Identifier id) { return Optional.empty(); }
                    @Override public Optional<NumericPoseProgram> numericProgram(Identifier id) { return Optional.ofNullable(exported.numericPrograms().get(id.toString())); }
                    @Override public Optional<FinitePoseProgram> finiteProgram(Identifier id) { return id.toString().equals(exported.tableId()) ? Optional.of(exported.table()) : Optional.empty(); }
                };
                var required = new HashSet<String>(); exported.inputs().forEach(v -> required.add(v.channel()));
                var pose = new FinitePoseEngine().bind(geometry, Map.of("program", exported.tableId()), required, resources).orElseThrow();
                int scalarCount = 0, matrixCount = 0; boolean cancellation = false, minSpeed = false, frozen = false, spin = false, water = false;
                for (String type : List.of("minecraft:cow", "minecraft:pig", "minecraft:chicken", "minecraft:zombie_nautilus")) {
                    for (int index = 0; index < 8; index++) {
                        var objects = S61ActorInputFixtures.create(client.level, type, index);
                        var entity = (LivingEntity)objects.getFirst(); objects.forEach(client.level::addEntity);
                        try {
                            LivingEntityRenderer renderer = (LivingEntityRenderer)client.getEntityRenderDispatcher().getRenderer(entity);
                            var requested = schema();
                            for (var input : exported.inputs()) requested.add(new OriginalLivingInputPreparation.Input(input.channel(), input.owner(), input.name(), input.descriptor()));
                            var result = OriginalLivingInputPreparation.inspect(renderer, requested); check(result.admitted(), type + " " + result.absence());
                            var recipe = result.recipe();
                            check(recipe.equals(OriginalLivingInputPreparation.inspect(renderer, requested).recipe()), "recipe instability");
                            check(OriginalLivingInputPreparation.inspect(renderer, exported).admitted(), "export schema not connected");
                            if (index == 0) recipes.add(recipe);
                            var bound = LivingPoseInputSource.bind((Class)entity.getClass(), recipe.program());
                            float walkBefore = entity.walkAnimation.position(1f);
                            var actual = bound.sample(entity).orElseThrow();
                            check(Float.floatToRawIntBits(walkBefore) == Float.floatToRawIntBits(entity.walkAnimation.position(1f)), "reader advances clock");
                            LivingEntityRenderState state = (LivingEntityRenderState)renderer.createRenderState(); renderer.extractRenderState(entity, state, 1f);
                            var expected = new TreeMap<String, Float>();
                            try {
                                for (var input : recipe.inputs()) {
                                    var field = Class.forName(input.owner(), false, renderer.getClass().getClassLoader()).getDeclaredField(input.name());
                                    float value = input.descriptor().equals("Z") ? (field.getBoolean(state) ? 1f : 0f) : field.getFloat(state);
                                    expected.put(input.channel(), value);
                                    check(Float.floatToRawIntBits(value) == Float.floatToRawIntBits(actual.get(input.channel())),
                                        type + " sample" + index + " " + input + " expected=" + value + " actual=" + actual.get(input.channel()));
                                    scalarCount++;
                                }
                            } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
                            if (index == 2) cancellation |= state.xRot != entity.getXRot();
                            if (index == 0) minSpeed |= state.walkAnimationSpeed != entity.walkAnimation.speed();
                            frozen |= state.isFullyFrozen; spin |= state.isAutoSpinAttack; water |= state.isInWater;
                            check(bound.sample(null).isEmpty(), "null actor admitted");
                            try { actual.clear(); throw new AssertionError("Mutable source sample"); } catch (UnsupportedOperationException correct) {}
                            var matrices = new TreeMap<String, List<Float>>();
                            if (type.equals("minecraft:cow")) {
                                model.setupAnim(state); var original = new TreeMap<String, Matrix4f>(); collect(model.root(), "root", new PoseStack(), original);
                                var transformed = geometry.transforms(pose.evaluate(new PoseEngine.Inputs(0, 0, 0, 0, 0, true, actual)).orElseThrow());
                                for (var entry : original.entrySet()) {
                                    check(entry.getValue().equals(transformed.get(entry.getKey()), 3e-5f), "native matrix " + entry.getKey());
                                    matrices.put(entry.getKey(), ModelGeometry.values(entry.getValue())); matrixCount++;
                                }
                            }
                            samples.add(new Sample(type, index, recipe.program(), expected, matrices));
                            if (index == 0) {
                                var duplicate = new ArrayList<>(requested); duplicate.add(requested.getFirst());
                                check(!OriginalLivingInputPreparation.inspect(renderer, duplicate).admitted(), "duplicate schema admitted");
                                check(!OriginalLivingInputPreparation.inspect(renderer, List.of(new OriginalLivingInputPreparation.Input("bad", LivingEntityRenderState.class.getName(), "deathTime", "F"))).admitted(), "unsupported field admitted");
                                check(!OriginalLivingInputPreparation.inspect(renderer, List.of(new OriginalLivingInputPreparation.Input("bad", LivingEntityRenderState.class.getName(), "isBaby", "F"))).admitted(), "wrong field type admitted");
                                entity.xRotO = Float.NaN;
                                check(Float.isNaN(entity.getXRot(1f)), "fixture failed to inject nonfinite interpolated pitch");
                                check(bound.sample(entity).isEmpty(), "nonfinite required pitch published");
                                var walkOnly = LivingPoseInputSource.bind((Class)entity.getClass(), new LivingPoseInputSource.Program(1, entity.getClass().getName(),
                                    List.of(new LivingPoseInputSource.Channel("walk", LivingPoseInputSource.Source.WALK_POSITION))));
                                check(walkOnly.sample(entity).isPresent(), "unrequested pitch invalidates walk source");
                            }
                        } finally { S61ActorInputFixtures.destroy(objects); }
                    }
                }
                check(cancellation && minSpeed && frozen && spin && water, "native oracle state vacuity");
                try {
                    var out = FabricLoader.getInstance().getGameDir().resolve("s61-implementation"); Files.createDirectories(out);
                    Files.writeString(out.resolve("evidence.json"), gson.toJson(new Evidence(exported, recipes, samples)));
                    var raw = out.resolve("original-resources"); Files.createDirectories(raw);
                    for (var recipe : recipes) for (String resource : recipe.originalResources().keySet()) {
                        try (var input = getClass().getResourceAsStream(resource)) {
                            check(input != null, "original resource absent"); Files.write(raw.resolve(resource.substring(1).replace('/', '_')), input.readAllBytes());
                        }
                    }
                } catch (java.io.IOException failure) { throw new AssertionError(failure); }
                System.out.println("S61_IMPLEMENTER_NATIVE_PASS actors=" + recipes.size() + " samples=" + samples.size() + " bitExactScalars=" + scalarCount + " originalMatrices=" + matrixCount);
            });
        }
    }
    private static void collect(ModelPart part, String path, PoseStack stack, Map<String, Matrix4f> out) {
        stack.pushPose(); part.translateAndRotate(stack); out.put(path, new Matrix4f(stack.last().pose()));
        new TreeMap<>(((ModelPartAccess)(Object)part).scalebrews$children()).forEach((name, child) -> collect(child, path + "/" + name, stack, out)); stack.popPose();
    }
}
