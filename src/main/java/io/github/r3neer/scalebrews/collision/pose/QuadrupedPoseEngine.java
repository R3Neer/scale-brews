package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Canonical Minecraft ordinary quadruped pose engine; server-safe and renderer independent. */
public final class QuadrupedPoseEngine implements PoseEngine {
    private static final Set<String> SOURCES = Set.of(
        "minecraft:cow", "minecraft:pig", "minecraft:sheep", "minecraft:llama", "minecraft:trader_llama");

    @Override
    public Optional<Map<String, Matrix4f>> evaluate(ModelGeometry geometry, Inputs in, Map<String, String> parameters) {
        if (!parameters.isEmpty() || !in.ordinary() || !"26.2".equals(geometry.version())
                || !SOURCES.contains(geometry.source())) return Optional.empty();
        Map<String, Matrix4f> result = new LinkedHashMap<>();
        float phase = in.walkPhase() * .6662F;
        float a = Mth.cos(phase) * 1.4F * in.walkAmount();
        float b = Mth.cos(phase + (float)Math.PI) * 1.4F * in.walkAmount();
        for (var p : geometry.parts()) {
            String name = p.id().substring(p.id().lastIndexOf('/') + 1);
            float x, y;
            var source = p.sourcePose();
            switch (name) {
                case "head" -> { x = in.headPitch() * Mth.DEG_TO_RAD; y = in.headYaw() * Mth.DEG_TO_RAD; }
                case "right_hind_leg", "left_front_leg" -> { x = a; y = source == null ? 0f : source.yRot(); }
                case "left_hind_leg", "right_front_leg" -> { x = b; y = source == null ? 0f : source.yRot(); }
                default -> { continue; }
            }
            if (source != null) {
                result.put(p.id(), new Matrix4f().translation(source.x() / 16f, source.y() / 16f, source.z() / 16f)
                    .rotateZYX(source.zRot(), y, x).scale(source.xScale(), source.yScale(), source.zScale()));
            } else {
                Matrix4f rest = ModelGeometry.matrix(p.transform());
                Vector3f pos = rest.getTranslation(new Vector3f()), scale = rest.getScale(new Vector3f());
                result.put(p.id(), new Matrix4f().translation(pos).rotateZYX(0, y, x).scale(scale));
            }
        }
        return result.size() == 5 ? Optional.of(Collections.unmodifiableMap(result)) : Optional.empty();
    }
}
