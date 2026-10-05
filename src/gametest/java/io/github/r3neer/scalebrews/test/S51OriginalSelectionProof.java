package io.github.r3neer.scalebrews.test;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.collision.preparation.EnumAgeModelPreparation;
import io.github.r3neer.scalebrews.client.collision.preparation.ModelPartStructure;
import io.github.r3neer.scalebrews.collision.api.CollisionVariantSources;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.variant.ModelAndTexture;

/** IMPLEMENTER: original extraction and queued original model over every native enum/age pair. */
public final class S51OriginalSelectionProof implements FabricClientGameTest {
    private record State(PartPose current, PartPose initial, boolean visible, boolean skipDraw) {}
    private static void require(boolean valid, String reason) { if (!valid) throw new AssertionError(reason); }

    @Override @SuppressWarnings({"rawtypes", "unchecked"})
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(client -> {
                try {
                    var folder = FabricLoader.getInstance().getGameDir().resolve("s51-original-resources"); Files.createDirectories(folder);
                    for (Class<?> original : List.of(LivingEntityRenderer.class, net.minecraft.client.renderer.entity.EntityRenderer.class,
                        AdultAndBabyModelPair.class, ModelAndTexture.class, net.minecraft.world.entity.LivingEntity.class,
                        net.minecraft.world.item.ItemStack.class, net.minecraft.util.Mth.class)) {
                        String path = "/" + original.getName().replace('.', '/') + ".class";
                        try (var stream = original.getResourceAsStream(path)) { Files.write(folder.resolve(original.getSimpleName()+".class"), stream.readAllBytes()); }
                        System.out.println("S51_ORIGINAL_RESOURCE " + original.getName() + " origin=" + original.getProtectionDomain().getCodeSource());
                    }
                } catch (java.io.IOException failure) { throw new AssertionError(failure); }
                var recipes = new ArrayList<EnumAgeModelPreparation.Recipe>();
                int selected = 0;
                for (var type : List.of(EntityTypes.COW, EntityTypes.PIG, EntityTypes.CHICKEN)) {
                    var entity = (AgeableMob)type.create(client.level, EntitySpawnReason.COMMAND);
                    require(entity != null, "Native factory absent");
                    entity.setId(41000 + recipes.size());
                    entity.setPos(4 + recipes.size() * 3, 5, 4);
                    client.level.addEntity(entity);
                    LivingEntityRenderer renderer = (LivingEntityRenderer)client.getEntityRenderDispatcher().getRenderer(entity);
                    try {
                        var mapField = renderer.getClass().getDeclaredField("models"); mapField.setAccessible(true);
                        Map<?, ?> models = (Map<?, ?>)mapField.get(renderer);
                        var originalParts = snapshots(models);
                        var result = EnumAgeModelPreparation.inspect(renderer);
                        require(result.admitted(), "Native selector rejected: " + entity.getClass() + " / " + result.absence());
                        var recipe = result.recipe(); recipes.add(recipe);
                        require(recipe.exactActorClass().equals(entity.getClass().getName()), "Actor proof not exact");
                        require(recipe.rows().size() == models.size() * 2, "Incomplete enum/age rows");
                        require(recipe.equals(EnumAgeModelPreparation.inspect(renderer).recipe()), "Preparation not deterministic");
                        verify(originalParts);
                        try { recipe.rows().clear(); throw new AssertionError("Mutable recipe rows"); }
                        catch (UnsupportedOperationException expected) {}
                        var template = ((Holder<?>)entity.getClass().getMethod("getVariant").invoke(entity)).value();
                        var components = template.getClass().getRecordComponents();
                        Class<?>[] parameterTypes = java.util.Arrays.stream(components).map(java.lang.reflect.RecordComponent::getType).toArray(Class<?>[]::new);
                        Object[] arguments = new Object[components.length];
                        int projection = -1;
                        for (int i = 0; i < components.length; i++) {
                            arguments[i] = components[i].getAccessor().invoke(template);
                            if (components[i].getType() == ModelAndTexture.class) projection = i;
                        }
                        var texture = ((ModelAndTexture<?>)arguments[projection]).asset();
                        for (var entry : models.entrySet()) for (boolean baby : List.of(false, true)) {
                            arguments[projection] = new ModelAndTexture(entry.getKey(), texture);
                            Object variant = template.getClass().getConstructor(parameterTypes).newInstance(arguments);
                            entity.setComponent((DataComponentType)BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse(recipe.componentId())), Holder.direct(variant));
                            entity.setAge(baby ? -24000 : 0);
                            Object authoritativeVariant = ((Holder<?>)entity.getClass().getMethod("getVariant").invoke(entity)).value();
                            LivingEntityRenderState state = (LivingEntityRenderState)renderer.createRenderState();
                            renderer.extractRenderState(entity, state, 0.5f);
                            require(state.isBaby == entity.isBaby(), "Original extraction changed age");
                            // SynchedEntityData may retain an equal prior value; the authoritative
                            // getter, not the object passed to setVariant, is the extraction oracle.
                            require(state.getClass().getField("variant").get(state) == authoritativeVariant, "Original extraction changed component value");
                            var source = CollisionVariantSources.snapshot().get(recipe.sourceKey());
                            String value = ((StringRepresentable)entry.getKey()).getSerializedName();
                            require(value.equals(source.reader().value(entity)), "Source/extraction model mismatch");
                            String age = baby ? "baby" : "adult";
                            var row = recipe.rows().stream().filter(candidate -> candidate.age().equals(age)
                                && candidate.modelValue().equals(value)).findFirst().orElseThrow();
                            Model<?> expected = ((AdultAndBabyModelPair<?>)entry.getValue()).getModel(baby);
                            require(row.initialStructure().equals(ModelPartStructure.inspect(expected.root())), "Recipe points to wrong structure");
                            var collector = new SubmitNodeStorage();
                            renderer.submit(state, new PoseStack(), collector, new CameraRenderState());
                            var queued = new ArrayList<ModelFeatureRenderer.Submit<?>>();
                            collector.order(0).solid.sortInto((node, strict) -> {
                                if (node instanceof ModelFeatureRenderer.Submit<?> submit && submit.state() == state) queued.add(submit);
                            });
                            require(queued.stream().anyMatch(submit -> submit.model() == expected), "Original submit did not queue recipe model");
                            selected++;
                        }
                    } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
                    finally { entity.discard(); }
                }
                var other = EntityTypes.ZOMBIE_NAUTILUS.create(client.level, EntitySpawnReason.COMMAND);
                require(!EnumAgeModelPreparation.inspect(client.getEntityRenderDispatcher().getRenderer(other)).admitted(), "Other selector family admitted");
                other.discard();
                require(!EnumAgeModelPreparation.inspect(null).admitted(), "Absent renderer admitted");
                try {
                    var folder = FabricLoader.getInstance().getGameDir().resolve("s51-implementation"); Files.createDirectories(folder);
                    Files.writeString(folder.resolve("recipes.json"), new GsonBuilder().setPrettyPrinting().create().toJson(recipes));
                } catch (java.io.IOException failure) { throw new AssertionError(failure); }
                System.out.println("S51_IMPLEMENTER_PASS nativeRenderers=" + recipes.size() + " originalExtractSubmitSelections=" + selected);
            });
        }
    }

    private static Map<ModelPart, State> snapshots(Map<?, ?> models) {
        var result = new IdentityHashMap<ModelPart, State>();
        for (var value : models.values()) {
            var pair = (AdultAndBabyModelPair<?>)value;
            for (var model : List.of(pair.adultModel(), pair.babyModel())) for (var part : model.allParts())
                result.put(part, new State(part.storePose(), part.getInitialPose(), part.visible, part.skipDraw));
        }
        return result;
    }
    private static void verify(Map<ModelPart, State> original) {
        original.forEach((part, state) -> require(state.equals(new State(part.storePose(), part.getInitialPose(), part.visible, part.skipDraw)),
            "Preparation changed shared model pose/visibility"));
    }
}
