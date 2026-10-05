package io.github.r3neer.scalebrews.collision.internal;

import com.mojang.serialization.JsonOps;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionCodecs;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.CowVariant;
import org.joml.Matrix4f;

/** Isolated IMPLEMENTER selection fixture; its cubes are not original Cow geometry. */
public final class S50ImplementerModelLifecycleTests {
    private static final Identifier TYPE = Identifier.parse("minecraft:cow");
    private static final Identifier NORMAL = Identifier.parse("scalebrews_test:s50_normal");
    private static final Identifier COLD = Identifier.parse("scalebrews_test:s50_cold");

    @GameTest
    public void modelChangesRetireMaterialWhileCosmeticChangesKeepTheSameBinding(GameTestHelper h) {
        var cow = h.spawn(EntityTypes.COW, 2, 20, 2);
        cow.setNoAi(true);cow.setNoGravity(true);
        cow.setComponent(DataComponents.COW_VARIANT, S50ImplementerModelComponentTests.variant(CowVariant.ModelType.NORMAL, "original"));
        var server = h.getLevel().getServer();
        try {
            AnatomyRuntime.startPrepared(server, Map.of(), Map.of());
            AnatomyRuntime.reload(server, resources(server.getResourceManager()));
            var first = frame(cow);
            h.assertTrue(first.identity().model().equals(NORMAL), "Original model enum selects the normal row");
            cow.setComponent(DataComponents.COW_VARIANT, S50ImplementerModelComponentTests.variant(CowVariant.ModelType.NORMAL, "cosmetic_only"));
            h.assertTrue(AnatomyMovement.queryFrame(cow).orElseThrow().identity().equals(first.identity()), "Cosmetic-only replacement preserves current material and its generation");
            cow.setComponent(DataComponents.COW_VARIANT, S50ImplementerModelComponentTests.variant(CowVariant.ModelType.COLD, "cold"));
            var descriptor = AnatomyBindingState.descriptor(cow);
            h.assertTrue(descriptor.model().equals(COLD) && descriptor.bindingGeneration() > first.identity().bindingGeneration(), "Structural replacement immediately selects B with fresh generation");
            h.assertTrue(AnatomyMovement.queryFrame(cow).isEmpty(), "Old A material is absent before B pose publication");
            var second = frame(cow);
            cow.setComponent(DataComponents.COW_VARIANT, S50ImplementerModelComponentTests.variant(CowVariant.ModelType.NORMAL, "return"));
            var third = frame(cow);
            h.assertTrue(third.identity().bindingIdentity().equals(first.identity().bindingIdentity()) && third.identity().bindingGeneration() > second.identity().bindingGeneration(), "Structural ABA preserves row identity but opens fresh authority");
        } finally {AnatomyRuntime.stop(server);cow.discard();}
        h.succeed();
    }

    private static GeometryProvider.QueryFrame frame(Cow cow) {
        AnatomyBindingState.provider(cow).tick(cow, cow.level().getGameTime());
        return AnatomyMovement.queryFrame(cow).orElseThrow();
    }
    private static CollisionBinding row(Identifier model, String value) {
        return new CollisionBinding(1, TYPE, Map.of(S50ImplementerModelComponentTests.key(DataComponents.COW_VARIANT), value),
            new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART, model, Map.of(), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(Identifier.parse("scalebrews:static"), Map.of(), Set.of()),
            BuiltInRootTransformProviders.ENTITY_ROOT, CollisionPolicy.Patch.EMPTY, Set.of());
    }
    private static ResourceManager resources(ResourceManager installed) {
        var pack = installed.listPacks().findFirst().orElseThrow();
        var geometry = new TreeMap<Identifier,Resource>(java.util.Comparator.comparing(Identifier::toString));
        var rows = new TreeMap<Identifier,Resource>(java.util.Comparator.comparing(Identifier::toString));
        for (var id : List.of(NORMAL, COLD)) {
            double r = id.equals(NORMAL) ? .5 : .25;
            var model = new ModelGeometry(2, TYPE.toString(), "26.2", List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(new Matrix4f()))),
                List.of(new ModelGeometry.Piece("fixture_body", "root", List.of(-r,0d,-r), List.of(r,1d,r), null)), ModelGeometry.values(new Matrix4f()));
            geometry.put(Identifier.fromNamespaceAndPath(id.getNamespace(), "scalebrews/entity_geometry/" + id.getPath() + ".json"),
                resource(pack, AnatomyCodecs.GEOMETRY.encodeStart(JsonOps.INSTANCE, model).getOrThrow().toString()));
            rows.put(Identifier.fromNamespaceAndPath(id.getNamespace(), "scalebrews/entity_collision/" + id.getPath() + ".json"),
                resource(pack, CollisionCodecs.BINDING.encodeStart(JsonOps.INSTANCE, row(id, id.equals(NORMAL) ? "normal" : "cold")).getOrThrow().toString()));
        }
        return (ResourceManager) java.lang.reflect.Proxy.newProxyInstance(ResourceManager.class.getClassLoader(), new Class<?>[]{ResourceManager.class}, (proxy, method, args) -> {
            if (method.getName().equals("listPacks")) return installed.listPacks();
            if (method.getName().equals("listResources")) return switch ((String) args[0]) {
                case "scalebrews/entity_geometry" -> geometry;
                case "scalebrews/entity_collision" -> rows;
                default -> Map.of();
            };
            throw new UnsupportedOperationException(method.toString());
        });
    }
    private static Resource resource(net.minecraft.server.packs.PackResources pack, String json) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return new Resource(pack, () -> new java.io.ByteArrayInputStream(bytes));
    }
}
