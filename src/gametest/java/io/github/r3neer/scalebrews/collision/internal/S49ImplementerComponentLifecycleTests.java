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
import net.minecraft.world.entity.animal.fox.Fox;
import org.joml.Matrix4f;

/** Isolated IMPLEMENTER live probe. Synthetic geometry proves selection only, not original fox geometry. */
public final class S49ImplementerComponentLifecycleTests {
    private static final Identifier TYPE = Identifier.parse("minecraft:fox");
    private static final Identifier RED = Identifier.parse("scalebrews_test:s49_red");
    private static final Identifier SNOW = Identifier.parse("scalebrews_test:s49_snow");

    @GameTest
    public void nativeComponentChangeUsesTheExistingCausalVariantLifecycle(GameTestHelper h) {
        var fox = h.spawn(EntityTypes.FOX, 2, 20, 2);
        fox.setNoAi(true);fox.setNoGravity(true);fox.setComponent(DataComponents.FOX_VARIANT, Fox.Variant.RED);
        var server = h.getLevel().getServer();
        try {
            AnatomyRuntime.startPrepared(server, Map.of(), Map.of());
            AnatomyRuntime.reload(server, resources(server.getResourceManager()));
            var first = frame(fox);
            h.assertTrue(first.identity().model().equals(RED), "Original red component selects the exact declared red row");
            fox.setComponent(DataComponents.FOX_VARIANT, Fox.Variant.SNOW);
            var descriptor = AnatomyBindingState.descriptor(fox);
            h.assertTrue(descriptor != null && descriptor.model().equals(SNOW)
                && descriptor.bindingGeneration() > first.identity().bindingGeneration(), "Native synchronized component change retires A and installs B immediately");
            h.assertTrue(AnatomyMovement.queryFrame(fox).isEmpty(), "A query cannot reuse red material or advance snow joints");
            var second = frame(fox);
            h.assertTrue(!second.identity().bindingIdentity().equals(first.identity().bindingIdentity()), "The selected component row preserves its distinct content token");
            fox.setComponent(DataComponents.FOX_VARIANT, Fox.Variant.RED);
            var third = frame(fox);
            h.assertTrue(third.identity().bindingIdentity().equals(first.identity().bindingIdentity())
                && third.identity().bindingGeneration() > second.identity().bindingGeneration(), "Native component ABA opens fresh physical authority");
        } finally {AnatomyRuntime.stop(server);fox.discard();}
        h.succeed();
    }

    private static GeometryProvider.QueryFrame frame(Fox fox) {
        AnatomyBindingState.provider(fox).tick(fox, fox.level().getGameTime());
        return AnatomyMovement.queryFrame(fox).orElseThrow();
    }
    private static CollisionBinding row(Identifier model, Fox.Variant value) {
        return new CollisionBinding(1, TYPE, Map.of(S49ImplementerEnumPreparationTests.key(DataComponents.FOX_VARIANT), value.getSerializedName()),
            new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART, model, Map.of(), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(Identifier.parse("scalebrews:static"), Map.of(), Set.of()),
            BuiltInRootTransformProviders.ENTITY_ROOT, CollisionPolicy.Patch.EMPTY, Set.of());
    }
    private static ResourceManager resources(ResourceManager installed) {
        var pack = installed.listPacks().findFirst().orElseThrow();
        var geometry = new TreeMap<Identifier,Resource>(java.util.Comparator.comparing(Identifier::toString));
        var rows = new TreeMap<Identifier,Resource>(java.util.Comparator.comparing(Identifier::toString));
        for (var id : List.of(RED, SNOW)) {
            double r = id.equals(RED) ? .5 : .25;
            var model = new ModelGeometry(2, TYPE.toString(), "26.2", List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(new Matrix4f()))),
                List.of(new ModelGeometry.Piece("fixture_body", "root", List.of(-r,0d,-r), List.of(r,1d,r), null)), ModelGeometry.values(new Matrix4f()));
            geometry.put(Identifier.fromNamespaceAndPath(id.getNamespace(), "scalebrews/entity_geometry/" + id.getPath() + ".json"),
                resource(pack, AnatomyCodecs.GEOMETRY.encodeStart(JsonOps.INSTANCE, model).getOrThrow().toString()));
            rows.put(Identifier.fromNamespaceAndPath(id.getNamespace(), "scalebrews/entity_collision/" + id.getPath() + ".json"),
                resource(pack, CollisionCodecs.BINDING.encodeStart(JsonOps.INSTANCE, row(id, id.equals(RED) ? Fox.Variant.RED : Fox.Variant.SNOW)).getOrThrow().toString()));
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
