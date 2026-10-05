package io.github.r3neer.scalebrews.test;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.collision.preparation.ModelPartStructure;
import io.github.r3neer.scalebrews.client.collision.preparation.OriginalAgeStateModelPreparation;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;

/** IMPLEMENTER: original state selector; entity extraction deliberately remains uncertified. */
public final class S52AgeStateSelectionProof implements FabricClientGameTest {
    private record State(PartPose current, PartPose initial, boolean visible, boolean skipDraw) {}
    private static void require(boolean valid, String reason) { if (!valid) throw new AssertionError(reason); }

    @Override @SuppressWarnings({"rawtypes", "unchecked"})
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(client -> {
                var recipes = new ArrayList<OriginalAgeStateModelPreparation.Recipe>();
                int selections = 0;
                for (var type : List.of(EntityTypes.WOLF, EntityTypes.POLAR_BEAR, EntityTypes.BEE, EntityTypes.GOAT, EntityTypes.TURTLE, EntityTypes.OCELOT)) {
                    var entity = (AgeableMob)type.create(client.level, EntitySpawnReason.COMMAND);
                    require(entity != null, "Missing native factory");
                    entity.setId(42000 + recipes.size()); entity.setPos(4 + recipes.size() * 3, 5, 4); client.level.addEntity(entity);
                    LivingEntityRenderer renderer = (LivingEntityRenderer)client.getEntityRenderDispatcher().getRenderer(entity);
                    try {
                        var adult = AgeableMobRenderer.class.getDeclaredField("adultModel"); adult.setAccessible(true);
                        var baby = AgeableMobRenderer.class.getDeclaredField("babyModel"); baby.setAccessible(true);
                        Model<?> adultModel = (Model<?>)adult.get(renderer), babyModel = (Model<?>)baby.get(renderer);
                        var before = snapshots(List.of(adultModel, babyModel));
                        var result = OriginalAgeStateModelPreparation.inspect(renderer);
                        require(result.admitted(), "Native state selector rejected " + entity.getClass() + ": " + result.absence());
                        var recipe = result.recipe(); recipes.add(recipe);
                        require(recipe.extractionAuthority().equals("UNPROVEN"), "Entity extraction accidentally certified");
                        require(recipe.rows().size() == 2 && recipe.uniqueModels() == (adultModel == babyModel ? 1 : 2), "Incomplete state predicate domain");
                        require(recipe.equals(OriginalAgeStateModelPreparation.inspect(renderer).recipe()), "Nondeterministic recipe");
                        verify(before);
                        try { recipe.rows().clear(); throw new AssertionError("Mutable rows"); } catch (UnsupportedOperationException expected) {}
                        for (boolean nativeBaby : List.of(false, true)) {
                            entity.setAge(nativeBaby ? -24000 : 0);
                            LivingEntityRenderState state = (LivingEntityRenderState)renderer.createRenderState();
                            renderer.extractRenderState(entity, state, 0);
                            require(state.isBaby == nativeBaby, "Native witness age extraction differs");
                            verifySelection(renderer, state, nativeBaby ? babyModel : adultModel, recipe); selections++;
                            // Proves exactly the state predicate, even when it disagrees with the entity.
                            state.isBaby = !nativeBaby;
                            verifySelection(renderer, state, state.isBaby ? babyModel : adultModel, recipe); selections++;
                        }
                    } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
                    finally { entity.discard(); }
                }
                var other = EntityTypes.COW.create(client.level, EntitySpawnReason.COMMAND);
                require(!OriginalAgeStateModelPreparation.inspect(client.getEntityRenderDispatcher().getRenderer(other)).admitted(), "Enum-map selector admitted as inherited age family");
                other.discard();
                require(!OriginalAgeStateModelPreparation.inspect(null).admitted(), "Absent renderer admitted");
                try {
                    var folder = FabricLoader.getInstance().getGameDir().resolve("s52-implementation"); Files.createDirectories(folder);
                    Files.writeString(folder.resolve("recipes.json"), new GsonBuilder().setPrettyPrinting().create().toJson(recipes));
                } catch (java.io.IOException failure) { throw new AssertionError(failure); }
                System.out.println("S52_IMPLEMENTER_PASS nativeRenderers=" + recipes.size() + " originalAndPerturbedStateSelections=" + selections);
            });
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void verifySelection(LivingEntityRenderer renderer, LivingEntityRenderState state, Model<?> expected,
                                        OriginalAgeStateModelPreparation.Recipe recipe) {
        var row = recipe.rows().stream().filter(value -> value.statePredicate() == state.isBaby).findFirst().orElseThrow();
        require(row.initialStructure().equals(ModelPartStructure.inspect(expected.root())), "Wrong structure for state predicate");
        var collector = new SubmitNodeStorage(); renderer.submit(state, new PoseStack(), collector, new CameraRenderState());
        var queued = new ArrayList<ModelFeatureRenderer.Submit<?>>();
        collector.order(0).solid.sortInto((node, strict) -> {
            if (node instanceof ModelFeatureRenderer.Submit<?> submit && submit.state() == state) queued.add(submit);
        });
        require(queued.stream().anyMatch(submit -> submit.model() == expected), "Original submission did not queue selected model");
    }

    private static Map<ModelPart, State> snapshots(List<Model<?>> models) {
        var states = new IdentityHashMap<ModelPart, State>();
        for (var model : models) model.root().getAllParts().forEach(part -> states.put(part,
            new State(part.storePose(), part.getInitialPose(), part.visible, part.skipDraw)));
        return states;
    }
    private static void verify(Map<ModelPart, State> states) {
        states.forEach((part, state) -> require(state.equals(new State(part.storePose(), part.getInitialPose(), part.visible, part.skipDraw)), "Preparation mutated shared model"));
    }
}
