package io.github.r3neer.scalebrews.test;

import com.google.gson.GsonBuilder;
import io.github.r3neer.scalebrews.client.collision.preparation.OriginalResetPosePreparation;
import io.github.r3neer.scalebrews.client.mixin.ModelPartAccess;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.model.monster.slime.SmallSulfurCubeModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;

/** IMPLEMENTER: model-local numeric reset reference, no renderer or physical-pose authority. */
public final class S54OriginalResetProof implements FabricClientGameTest {
    private record Snapshot(PartPose current, PartPose initial, boolean visible, boolean skipDraw) {}
    private static void require(boolean valid, String why) { if (!valid) throw new AssertionError(why); }

    @Override @SuppressWarnings({"rawtypes", "unchecked"})
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(client -> {
                var recipes = new ArrayList<OriginalResetPosePreparation.Recipe>();
                int nativeCases = 0;
                for (EntityModel<?> model : List.of(new SlimeModel(SlimeModel.createInnerBodyLayer().bakeRoot()),
                    new SmallSulfurCubeModel(SmallSulfurCubeModel.createInnerBodyLayer().bakeRoot()))) {
                    var baseline = capture(model);
                    try {
                        var prepared = OriginalResetPosePreparation.inspect(model);
                        require(prepared.admitted(), "Original reset not certified: " + prepared.absence());
                        var recipe = prepared.recipe(); recipes.add(recipe);
                        require(recipe.operation().equals(OriginalResetPosePreparation.OPERATION), "Wrong numeric program");
                        require(recipe.parts().size() == model.allParts().size(), "Incomplete reset parts");
                        require(recipe.setupAnimOwner().equals(Model.class.getName()), "Wrong effective setup owner");
                        require(recipe.equals(OriginalResetPosePreparation.inspect(model).recipe()), "Unstable certificate");
                        verify(baseline);
                        try { recipe.parts().clear(); throw new AssertionError("Mutable parts"); }
                        catch (UnsupportedOperationException expected) {}
                        try { recipe.originalResources().clear(); throw new AssertionError("Mutable resources"); }
                        catch (UnsupportedOperationException expected) {}
                        var rows = new java.util.TreeMap<String, ModelPart>(); collect(model.root(), "$", rows);
                        for (var row : recipe.parts()) require(same(row.initial(), rows.get(row.path()).getInitialPose()), "Initial row mismatch");

                        for (int stateIndex = 0; stateIndex < 3; stateIndex++) {
                            for (var part : model.allParts()) {
                                part.loadPose(new PartPose(41, -13, 9, 0.7f, -0.4f, 1.2f, 2, 3, 4));
                                part.visible = false; part.skipDraw = true;
                            }
                            var perturbed = capture(model);
                            require(recipe.equals(OriginalResetPosePreparation.inspect(model).recipe()), "Current transforms or visibility contaminate recipe");
                            verify(perturbed);
                            var state = stateIndex == 0 ? null : new EntityRenderState();
                            if (state != null) state.ageInTicks = stateIndex * 700;
                            ((Model)model).setupAnim(state);
                            for (var part : model.allParts()) {
                                require(current(part).equals(part.getInitialPose()), "Native numeric reset mismatch");
                                require(!part.visible && part.skipDraw, "Native reset unexpectedly changes visibility");
                            }
                            nativeCases++;
                        }

                        // The cached reset list must be exactly the currently observed structural tree.
                        var children = ((ModelPartAccess)(Object)model.root()).scalebrews$children();
                        var first = children.entrySet().iterator().next();
                        var saved = first.getValue();
                        children.put(first.getKey(), new ModelPart(List.of(), new java.util.HashMap<>()));
                        try { require(!OriginalResetPosePreparation.inspect(model).admitted(), "Stale cached reset list admitted"); }
                        finally { children.put(first.getKey(), saved); }
                        require(recipe.equals(OriginalResetPosePreparation.inspect(model).recipe()), "Restored tree changed recipe");
                    } finally { restore(baseline); }
                    verify(baseline);
                }
                var pig = EntityTypes.PIG.create(client.level, EntitySpawnReason.COMMAND);
                require(pig != null, "Native animated witness missing");
                var renderer = (LivingEntityRenderer)client.getEntityRenderDispatcher().getRenderer(pig);
                var field = field(renderer.getClass(), "model");
                try {
                    field.setAccessible(true);
                    var result = OriginalResetPosePreparation.inspect((EntityModel<?>)field.get(renderer));
                    require(!result.admitted() && result.absence().equals("original_reset:animation_override"), "Animated model certified as reset-only");
                } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
                require(!OriginalResetPosePreparation.inspect(null).admitted(), "Null model admitted");
                try {
                    var output = FabricLoader.getInstance().getGameDir().resolve("s54-implementation"); Files.createDirectories(output);
                    Files.writeString(output.resolve("recipes.json"), new GsonBuilder().setPrettyPrinting().create().toJson(recipes));
                    var raw = output.resolve("original-resources"); Files.createDirectories(raw);
                    for (var recipe : recipes) for (String resource : recipe.originalResources().keySet()) {
                        try (var stream = Model.class.getResourceAsStream(resource)) {
                            require(stream != null, "Missing original resource"); Files.write(raw.resolve(resource.substring(1).replace('/', '_')), stream.readAllBytes());
                        }
                    }
                } catch (java.io.IOException failure) { throw new AssertionError(failure); }
                System.out.println("S54_IMPLEMENTER_PASS models=" + recipes.size() + " nativeResetCases=" + nativeCases);
            });
        }
    }

    private static java.lang.reflect.Field field(Class<?> type, String name) {
        for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
            try { return owner.getDeclaredField(name); } catch (NoSuchFieldException missing) { /* continue */ }
        }
        throw new AssertionError("Missing native field " + name);
    }
    private static void collect(ModelPart part, String path, Map<String, ModelPart> rows) {
        rows.put(path, part);
        new java.util.TreeMap<>(((ModelPartAccess)(Object)part).scalebrews$children())
            .forEach((key, child) -> collect(child, path + "/" + key.length() + ":" + key, rows));
    }
    private static boolean same(OriginalResetPosePreparation.Transform t, PartPose p) {
        return new PartPose(t.x(), t.y(), t.z(), t.xRot(), t.yRot(), t.zRot(), t.xScale(), t.yScale(), t.zScale()).equals(p);
    }
    private static PartPose current(ModelPart p) {
        return new PartPose(p.x, p.y, p.z, p.xRot, p.yRot, p.zRot, p.xScale, p.yScale, p.zScale);
    }
    private static Map<ModelPart, Snapshot> capture(Model<?> model) {
        var states = new IdentityHashMap<ModelPart, Snapshot>();
        for (var p : model.allParts()) states.put(p, new Snapshot(current(p), p.getInitialPose(), p.visible, p.skipDraw));
        return states;
    }
    private static void verify(Map<ModelPart, Snapshot> states) {
        states.forEach((p, s) -> require(current(p).equals(s.current()) && p.getInitialPose().equals(s.initial())
            && p.visible == s.visible() && p.skipDraw == s.skipDraw(), "Inspection mutated original model"));
    }
    private static void restore(Map<ModelPart, Snapshot> states) {
        states.forEach((p, s) -> { p.loadPose(s.current()); p.setInitialPose(s.initial()); p.visible = s.visible(); p.skipDraw = s.skipDraw(); });
    }
}
