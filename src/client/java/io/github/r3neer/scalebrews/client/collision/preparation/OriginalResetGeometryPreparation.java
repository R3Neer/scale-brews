package io.github.r3neer.scalebrews.client.collision.preparation;

import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;

/**
 * PREPARATION bridge from an original reset certificate to detached initial local geometry.
 * Only ModelGeometry crosses to common/server consumers. Structural cubes and an identity
 * coordinate frame do not certify visibility, renderer root, material or a physical binding.
 */
public final class OriginalResetGeometryPreparation {
    public static final String TOOL_VERSION = "original-initial-reset-geometry-v1";
    private OriginalResetGeometryPreparation() {}

    public record Prepared(OriginalResetPosePreparation.Recipe program, ModelGeometry geometry) {}
    public record Result(Prepared prepared, String absence) {
        public boolean admitted() { return prepared != null; }
    }

    public static Result inspect(EntityModel<?> model) {
        if (!Minecraft.getInstance().isSameThread())
            throw new IllegalStateException("reset geometry preparation requires client thread");
        var certificate = OriginalResetPosePreparation.inspect(model);
        if (!certificate.admitted()) return new Result(null, certificate.absence());
        try {
            var recipe = certificate.recipe();
            var snapshot = ModelPartStructure.snapshot(model.root(), "original_reset/" + recipe.digest(), TOOL_VERSION);
            if (!recipe.initialStructure().equals(snapshot.fingerprint())) throw absent("structure_changed");
            verify(recipe, snapshot.geometry());
            return new Result(new Prepared(recipe, snapshot.geometry()), null);
        } catch (RuntimeException failure) {
            String reason = failure.getMessage();
            return new Result(null, reason == null ? "original_reset_geometry:unavailable"
                : reason.substring(0, Math.min(256, reason.length())));
        }
    }

    private record Expected(String parent, ModelGeometry.SourcePose initial) {}

    private static void verify(OriginalResetPosePreparation.Recipe recipe, ModelGeometry geometry) {
        Map<String, Expected> expected = new HashMap<>();
        for (var row : recipe.parts()) {
            String path = row.path();
            if (!path.startsWith("$")) throw absent("path");
            String id = "root", parent = null;
            int cursor = 1, depth = 0;
            while (cursor < path.length()) {
                if (++depth > ModelPartStructure.MAX_DEPTH || path.charAt(cursor++) != '/') throw absent("path");
                int start = cursor;
                while (cursor < path.length() && path.charAt(cursor) >= '0' && path.charAt(cursor) <= '9') cursor++;
                if (cursor == start || cursor - start > 3 || cursor == path.length() || path.charAt(cursor) != ':')
                    throw absent("path");
                int length = Integer.parseInt(path.substring(start, cursor++));
                if (length < 1 || length > 128 || length > path.length() - cursor) throw absent("path");
                parent = id;
                id += "/" + path.substring(cursor, cursor + length);
                cursor += length;
            }
            var t = row.initial();
            var pose = new ModelGeometry.SourcePose(t.x(), t.y(), t.z(), t.xRot(), t.yRot(), t.zRot(),
                t.xScale(), t.yScale(), t.zScale());
            if (expected.putIfAbsent(id, new Expected(parent, pose)) != null) throw absent("path_alias");
        }
        if (expected.size() != geometry.parts().size()) throw absent("part_membership");
        for (var part : geometry.parts()) {
            var row = expected.remove(part.id());
            if (row == null || !java.util.Objects.equals(row.parent(), part.parent()) || !row.initial().equals(part.sourcePose()))
                throw absent("initial_part_mismatch");
        }
        if (!expected.isEmpty()) throw absent("part_membership");
    }

    private static IllegalArgumentException absent(String reason) {
        return new IllegalArgumentException("original_reset_geometry:" + reason);
    }
}
