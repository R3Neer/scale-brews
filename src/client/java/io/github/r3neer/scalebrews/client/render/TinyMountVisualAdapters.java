package io.github.r3neer.scalebrews.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.joml.Matrix4fc;

import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit escape hatch for renderers whose attachment cannot be described by ModelPart JSON. */
public final class TinyMountVisualAdapters {
    private static final Map<Class<?>, Adapter> ADAPTERS = new LinkedHashMap<>();
    private TinyMountVisualAdapters() {}

    public static synchronized void register(Class<? extends EntityRenderer<?, ?>> rendererType, Adapter adapter) {
        if (ADAPTERS.putIfAbsent(rendererType, adapter) != null)
            throw new IllegalArgumentException("A Tiny Mount visual adapter is already registered for " + rendererType.getName());
    }

    static synchronized Adapter find(Object renderer) {
        return ADAPTERS.entrySet().stream().filter(entry -> entry.getKey().isInstance(renderer))
                .map(Map.Entry::getValue).findFirst().orElse(null);
    }

    @FunctionalInterface
    public interface Adapter {
        SeatFrame resolve(EntityRenderer<?, ?> renderer, LivingEntityRenderState state, ModelPart root,
                          Matrix4fc outer, TinyMountVisualProfile profile, long serial);
    }
}
