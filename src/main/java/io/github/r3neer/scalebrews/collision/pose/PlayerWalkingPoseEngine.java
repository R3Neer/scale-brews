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

/** Canonical standing, empty-handed Minecraft 26.2 player walk/head/breathing engine. */
public final class PlayerWalkingPoseEngine implements PoseEngine {
    @Override
    public Optional<Map<String, Matrix4f>> evaluate(ModelGeometry geometry, Inputs in, Map<String, String> parameters) {
        if (!parameters.isEmpty() || !in.ordinary() || !"26.2".equals(geometry.version())
                || !Set.of("minecraft:player_wide", "minecraft:player_slim").contains(geometry.source())) return Optional.empty();
        float phase = in.walkPhase() * .6662f;
        float a = Mth.cos(phase), b = Mth.cos(phase + (float)Math.PI);
        float bobX = Mth.sin(in.age() * .067f) * .05f, bobZ = Mth.cos(in.age() * .09f) * .05f + .05f;
        Map<String, Matrix4f> result = new LinkedHashMap<>();
        for (var p : geometry.parts()) {
            String name = p.id().substring(p.id().lastIndexOf('/') + 1);
            var source = p.sourcePose();
            float x, y = 0, z;
            switch (name) {
                case "head" -> {
                    x = in.headPitch() * Mth.DEG_TO_RAD;
                    y = in.headYaw() * Mth.DEG_TO_RAD;
                    z = source == null ? 0f : source.zRot();
                }
                case "right_arm" -> {
                    x = b * in.walkAmount() + bobX;
                    z = (source == null ? 0f : source.zRot()) + bobZ;
                }
                case "left_arm" -> {
                    x = a * in.walkAmount() - bobX;
                    z = (source == null ? 0f : source.zRot()) - bobZ;
                }
                case "right_leg" -> { x = a * 1.4f * in.walkAmount(); y = z = .005f; }
                case "left_leg" -> { x = b * 1.4f * in.walkAmount(); y = z = -.005f; }
                default -> { continue; }
            }
            if (source != null) {
                result.put(p.id(), new Matrix4f().translation(source.x() / 16f, source.y() / 16f, source.z() / 16f)
                    .rotateZYX(z, y, x).scale(source.xScale(), source.yScale(), source.zScale()));
            } else {
                var rest = ModelGeometry.matrix(p.transform());
                result.put(p.id(), new Matrix4f().translation(rest.getTranslation(new Vector3f()))
                    .rotateZYX(z, y, x).scale(rest.getScale(new Vector3f())));
            }
        }
        return result.size() == 5 ? Optional.of(Collections.unmodifiableMap(result)) : Optional.empty();
    }
}
