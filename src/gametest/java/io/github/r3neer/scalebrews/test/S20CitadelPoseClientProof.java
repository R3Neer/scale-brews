package io.github.r3neer.scalebrews.test;

import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.collision.preparation.AdvancedModelBoxGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.BuiltInGeometryEngines;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/**
 * S20 independent real-model acceptance: the data programs are compared against the exact
 * Alex's Mobs 2.1.9 model instances loaded from the pinned proof JAR, not against copied formulas.
 */
public final class S20CitadelPoseClientProof implements FabricClientGameTest {
    private static final Identifier ENGINE = Identifier.parse("scalebrews:citadel_program");
    private static final Identifier GRIZZLY_ID = Identifier.parse("alexsmobs:grizzly_bear");
    private static final Identifier GAZELLE_ID = Identifier.parse("alexsmobs:gazelle");
    private static final String GRIZZLY_MODEL = "com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear";
    private static final String GAZELLE_MODEL = "com.github.alexthe666.alexsmobs.client.model.ModelGazelle";
    private static final String ADVANCED_BOX = "com.github.alexthe666.alexsmobs.citadel.client.model.AdvancedModelBox";
    private static final String BASIC_PART = "com.github.alexthe666.alexsmobs.citadel.client.model.basic.BasicModelPart";
    private static final String ANIMATION = "com.github.alexthe666.alexsmobs.citadel.animation.Animation";
    private static final String ANIMATED_ENTITY = "com.github.alexthe666.alexsmobs.citadel.animation.IAnimatedEntity";
    private static final float MATRIX_EPS = 4e-5f;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FabricLoader.getInstance().isModLoaded("alexsmobs"))
                throw new AssertionError("S20 pinned real-model proof requires the locked Alex's Mobs input");
            proveGazelle(client);
            proveGrizzly(client);
        });
    }

    private static void proveGazelle(Minecraft client) {
        var program = resource("gazelle");
        var prepared = prepare(GAZELLE_ID, GAZELLE_MODEL, false);
        var entity = entity(client, GAZELLE_ID);
        var model = fresh(GAZELLE_MODEL, false);
        var bound = bind(prepared, GAZELLE_ID, program);

        sample("gazelle/ordinary", model, entity, prepared, bound,
            new Sample(1.3f, .6f, 7.4f, 20, -10, channels(program, Map.of("alexsmobs.running", 0f))),
            ignored -> invoke(entity, "setRunning", new Class[]{boolean.class}, false));
        sample("gazelle/running", model, entity, prepared, bound,
            new Sample(2.2f, .75f, 11.2f, -15, 8, channels(program, Map.of("alexsmobs.running", 1f))),
            ignored -> invoke(entity, "setRunning", new Class[]{boolean.class}, true));
        sampleAnimation("gazelle/tail_clip", model, entity, prepared, bound, program,
            "ANIMATION_FLICK_TAIL", 1, 1, Map.of("alexsmobs.running", 0f));
        sampleAnimation("gazelle/eat_clip", model, entity, prepared, bound, program,
            "ANIMATION_EAT_GRASS", 3, 6, Map.of("alexsmobs.running", 0f));
        System.out.println("S20_CITADEL_REAL gazelle samples=4 sameEngine=" + ENGINE);
    }

    private static void proveGrizzly(Minecraft client) {
        var program = resource("grizzly_bear");
        var prepared = prepare(GRIZZLY_ID, GRIZZLY_MODEL, true);
        var entity = entity(client, GRIZZLY_ID);
        var model = fresh(GRIZZLY_MODEL, true);
        var bound = bind(prepared, GRIZZLY_ID, program);

        Consumer<LivingEntity> ordinary = value -> grizzlyState(value, 0, 0, false);
        sample("grizzly/ordinary", model, entity, prepared, bound,
            new Sample(1.7f, .55f, 8.3f, 18, -6, grizzlyChannels(program, 0, 0, false, false)), ordinary);
        sample("grizzly/stand", model, entity, prepared, bound,
            new Sample(.9f, .35f, 4.5f, -25, 12, grizzlyChannels(program, 10, 0, false, false)),
            value -> grizzlyState(value, 10, 0, false));
        sample("grizzly/freddy_stand", model, entity, prepared, bound,
            new Sample(2.1f, .8f, 12.6f, 30, 5, grizzlyChannels(program, 10, 0, true, false)),
            value -> grizzlyState(value, 10, 0, true));
        sampleAnimation("grizzly/maul_clip", model, entity, prepared, bound, program,
            "ANIMATION_MAUL", 1, 2, Map.of(
                "alexsmobs.stand_progress", 0f,
                "alexsmobs.sit_progress", 0f,
                "alexsmobs.freddy", 0f,
                "alexsmobs.eating", 0f));
        System.out.println("S20_CITADEL_REAL grizzly samples=4 sameEngine=" + ENGINE);
    }

    private static void sampleAnimation(String label, Object model, LivingEntity entity, ModelGeometry geometry,
                                        PoseEngine.Bound bound, CitadelPoseProgram program,
                                        String animationField, int token, int tick, Map<String, Float> custom) {
        clearAnimation(entity);
        if (label.startsWith("gazelle/")) invoke(entity, "setRunning", new Class[]{boolean.class}, false);
        else grizzlyState(entity, 0, 0, false);
        setAnimation(entity, animationField, tick);
        float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var channels = new LinkedHashMap<>(custom);
        channels.put(CitadelPoseProgram.ANIMATION_CHANNEL, (float) token);
        channels.put(CitadelPoseProgram.ANIMATION_TICK_CHANNEL, (float) tick);
        channels.put(CitadelPoseProgram.ANIMATION_PARTIAL_CHANNEL, partial);
        var sample = new Sample(1.25f, .5f, 9.75f, 11, -7, Map.copyOf(channels));
        compare(label, model, entity, geometry, bound, sample);
        clearAnimation(entity);
    }

    private static void sample(String label, Object model, LivingEntity entity, ModelGeometry geometry,
                               PoseEngine.Bound bound, Sample sample, Consumer<LivingEntity> configure) {
        clearAnimation(entity);
        configure.accept(entity);
        compare(label, model, entity, geometry, bound, sample);
    }

    private static void compare(String label, Object model, LivingEntity entity, ModelGeometry geometry,
                                PoseEngine.Bound bound, Sample sample) {
        setupAnim(model, entity, sample);
        var expected = bound.evaluate(new PoseEngine.Inputs(sample.walkPhase, sample.walkAmount, sample.age,
            sample.headYaw, sample.headPitch, true, sample.channels)).orElseThrow(
                () -> new AssertionError("S20 engine rejected pinned real-model sample " + label));
        var actualParts = sourceParts(model);
        int compared = 0;
        for (var part : geometry.parts()) {
            if (part.sourcePose() == null || part.id().startsWith("$")) continue;
            Object source = actualParts.get(part.id());
            if (source == null) throw new AssertionError("S20 real model is missing extracted source part " + label + " / " + part.id());
            Matrix4f actual = localMatrix(source);
            Matrix4f predicted = expected.getOrDefault(part.id(), part.sourcePose().matrix());
            assertMatrixNear(predicted, actual, label + " / " + part.id());
            compared++;
        }
        if (compared == 0) throw new AssertionError("S20 real-model proof compared no source parts for " + label);
    }

    private static PoseEngine.Bound bind(ModelGeometry geometry, Identifier model, CitadelPoseProgram program) {
        if (!geometry.source().equals(model.toString()) || !geometry.version().equals("2.1.9"))
            throw new AssertionError("S20 geometry/program version mismatch for " + model);
        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        Identifier programId = Identifier.fromNamespaceAndPath(model.getNamespace(), model.getPath());
        PoseEngine.Resources resources = new PoseEngine.Resources() {
            @Override public Optional<io.github.r3neer.scalebrews.collision.pose.PoseProgram> program(Identifier id) {
                return Optional.empty();
            }
            @Override public Optional<CitadelPoseProgram> citadelProgram(Identifier id) {
                return programId.equals(id) ? Optional.of(program) : Optional.empty();
            }
        };
        return engine.bind(geometry, Map.of("program", programId.toString()), program.requiredChannels(), resources)
            .orElseThrow(() -> new AssertionError("S20 Citadel engine cannot bind pinned program " + programId));
    }

    private static ModelGeometry prepare(Identifier id, String modelClass, boolean adultGrizzly) {
        if (!AdvancedModelBoxGeometryEngine.sources().containsKey(id)) {
            AdvancedModelBoxGeometryEngine.registerSource(id, new AdvancedModelBoxGeometryEngine.Source(
                () -> fresh(modelClass, adultGrizzly), () -> sourceModelTransform(id)));
        }
        return CollisionEngines.geometry(BuiltInGeometryEngines.ADVANCED_MODEL_BOX).orElseThrow()
            .prepare(new GeometryEngine.Request(id)).orElseThrow(
                () -> new AssertionError("S20 cannot prepare pinned AdvancedModelBox geometry " + id));
    }

    private static CitadelPoseProgram resource(String name) {
        String path = "data/alexsmobs/scalebrews/citadel_pose_programs/" + name + ".json";
        try (var input = S20CitadelPoseClientProof.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new AssertionError("Missing S20 Citadel program resource " + path);
            var raw = new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), CitadelPoseProgram.class);
            var program = CitadelPoseProgram.validatedCopy(raw);
            if (!program.version().equals("2.1.9")) throw new AssertionError("Unpinned S20 program version " + path);
            return program;
        } catch (java.io.IOException failure) {
            throw new AssertionError("Cannot read S20 Citadel program " + path, failure);
        }
    }

    private static LivingEntity entity(Minecraft client, Identifier id) {
        var type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (type == null) throw new AssertionError("Pinned external entity type is absent: " + id);
        var value = type.create(client.level, EntitySpawnReason.LOAD);
        if (!(value instanceof LivingEntity living)) throw new AssertionError("Pinned external type is not living: " + id);
        return living;
    }

    private static Object fresh(String className, boolean adultGrizzly) {
        try {
            Object model = Class.forName(className).getConstructor().newInstance();
            if (adultGrizzly) {
                try { model.getClass().getField("young").setBoolean(model, false); }
                catch (NoSuchFieldException ignored) {}
            }
            return model;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot instantiate pinned Alex model " + className, failure);
        }
    }

    private static Matrix4f sourceModelTransform(Identifier id) {
        Matrix4f transform = new Matrix4f().scaling(-1f, -1f, 1f);
        if (id.equals(GAZELLE_ID)) transform.scale(.8f, .8f, .8f);
        return transform.translate(0f, -1.501f, 0f);
    }

    private static void setupAnim(Object model, LivingEntity entity, Sample sample) {
        try {
            Method target = null;
            for (Method method : model.getClass().getMethods()) {
                if (method.getName().equals("setupAnim") && method.getParameterCount() == 6
                        && method.getParameterTypes()[0].isAssignableFrom(entity.getClass())) {
                    target = method;
                    break;
                }
            }
            if (target == null) throw new NoSuchMethodException("setupAnim");
            target.invoke(model, entity, sample.walkPhase, sample.walkAmount, sample.age, sample.headYaw, sample.headPitch);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot invoke pinned Alex setupAnim for " + model.getClass().getName(), failure);
        }
    }

    private static Map<String, Object> sourceParts(Object model) {
        try {
            Class<?> advanced = Class.forName(ADVANCED_BOX, false, model.getClass().getClassLoader());
            Class<?> basic = Class.forName(BASIC_PART, false, model.getClass().getClassLoader());
            Field name = field(advanced, "boxName");
            Field children = field(advanced, "childModels");
            Method parts = model.getClass().getMethod("parts");
            var result = new LinkedHashMap<String, Object>();
            Object roots = parts.invoke(model);
            if (!(roots instanceof Iterable<?> iterable)) throw new AssertionError("Pinned Alex parts() is not iterable");
            for (Object root : iterable) collect(root, null, advanced, basic, name, children, result);
            return Map.copyOf(result);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot traverse pinned Alex model hierarchy", failure);
        }
    }

    private static void collect(Object part, String parent, Class<?> advanced, Class<?> basic,
                                Field name, Field children, Map<String, Object> result) throws IllegalAccessException {
        if (!advanced.isInstance(part) || !basic.isInstance(part)) throw new AssertionError("Unexpected pinned model part type");
        String local = (String) name.get(part);
        String id = parent == null ? local : parent + "/" + local;
        if (result.put(id, part) != null) throw new AssertionError("Duplicate pinned source part id " + id);
        Object raw = children.get(part);
        if (!(raw instanceof Iterable<?> iterable)) throw new AssertionError("Pinned childModels is not iterable");
        for (Object child : iterable) collect(child, id, advanced, basic, name, children, result);
    }

    private static Matrix4f localMatrix(Object box) {
        try {
            var stack = new PoseStack();
            box.getClass().getMethod("translateAndRotate", PoseStack.class).invoke(box, stack);
            return new Matrix4f(stack.last().pose());
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot read pinned AdvancedModelBox local transform", failure);
        }
    }

    private static void setAnimation(LivingEntity entity, String fieldName, int tick) {
        try {
            Class<?> animation = Class.forName(ANIMATION, false, entity.getClass().getClassLoader());
            Object value = entity.getClass().getField(fieldName).get(null);
            entity.getClass().getMethod("setAnimation", animation).invoke(entity, value);
            entity.getClass().getMethod("setAnimationTick", int.class).invoke(entity, tick);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot configure pinned animation " + fieldName, failure);
        }
    }

    private static void clearAnimation(LivingEntity entity) {
        try {
            ClassLoader loader = entity.getClass().getClassLoader();
            Class<?> animation = Class.forName(ANIMATION, false, loader);
            Object none = Class.forName(ANIMATED_ENTITY, false, loader).getField("NO_ANIMATION").get(null);
            entity.getClass().getMethod("setAnimation", animation).invoke(entity, none);
            entity.getClass().getMethod("setAnimationTick", int.class).invoke(entity, 0);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot reset pinned animation", failure);
        }
    }

    private static void grizzlyState(LivingEntity entity, float stand, float sit, boolean freddy) {
        setFloatField(entity, "prevStandProgress", stand);
        setFloatField(entity, "standProgress", stand);
        setFloatField(entity, "prevSitProgress", sit);
        setFloatField(entity, "sitProgress", sit);
        invoke(entity, "setAprilFoolsFlag", new Class[]{int.class}, freddy ? 2 : 0);
    }

    private static void setFloatField(Object target, String name, float value) {
        try { target.getClass().getField(name).setFloat(target, value); }
        catch (ReflectiveOperationException failure) { throw new AssertionError("Cannot set pinned field " + name, failure); }
    }

    private static void invoke(Object target, String name, Class<?>[] types, Object... values) {
        try { target.getClass().getMethod(name, types).invoke(target, values); }
        catch (ReflectiveOperationException failure) { throw new AssertionError("Cannot invoke pinned method " + name, failure); }
    }

    private static Map<String, Float> channels(CitadelPoseProgram program, Map<String, Float> custom) {
        var result = new LinkedHashMap<String, Float>();
        result.put(CitadelPoseProgram.ANIMATION_CHANNEL, 0f);
        result.put(CitadelPoseProgram.ANIMATION_TICK_CHANNEL, 0f);
        result.putAll(custom);
        if (!result.keySet().containsAll(program.requiredChannels()))
            throw new AssertionError("S20 proof fixture omitted required program channel: " + program.requiredChannels());
        return Map.copyOf(result);
    }

    private static Map<String, Float> grizzlyChannels(CitadelPoseProgram program, float stand, float sit,
                                                       boolean freddy, boolean eating) {
        return channels(program, Map.of(
            "alexsmobs.stand_progress", stand,
            "alexsmobs.sit_progress", sit,
            "alexsmobs.freddy", freddy ? 1f : 0f,
            "alexsmobs.eating", eating ? 1f : 0f));
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Class<?> cursor = owner;
        while (cursor != null) {
            try {
                Field value = cursor.getDeclaredField(name);
                value.setAccessible(true);
                return value;
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        throw new NoSuchFieldException(owner.getName() + "." + name);
    }

    private static void assertMatrixNear(Matrix4f expected, Matrix4f actual, String label) {
        float[] a = expected.get(new float[16]), b = actual.get(new float[16]);
        for (int i = 0; i < 16; i++) {
            if (Math.abs(a[i] - b[i]) > MATRIX_EPS)
                throw new AssertionError("S20 pinned model/program mismatch " + label + " matrix[" + i + "] expected="
                    + a[i] + " actual=" + b[i]);
        }
    }

    private record Sample(float walkPhase, float walkAmount, float age, float headYaw, float headPitch,
                          Map<String, Float> channels) {}
}
