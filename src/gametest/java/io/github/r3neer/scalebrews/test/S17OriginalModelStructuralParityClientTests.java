package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.ModelPartGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.BuiltInGeometryEngines;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.animal.chicken.AdultChickenModel;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Retroactive S17 adversarial oracle. The reference side walks Minecraft's
 * original ModelPart tree directly and never calls GeometryExtractor, so a
 * systematic omission in the production extractor cannot be shared by both
 * sides of the comparison.
 */
public final class S17OriginalModelStructuralParityClientTests implements FabricClientGameTest {
    private static final double EPS2 = 1e-12;
    private static final AnatomyFilter ALL_MATERIAL = new AnatomyFilter(0, 0, 0);

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            verify("cow", Identifier.parse("scalebrews_test:s17_reference_cow"),
                () -> CowModel.createBodyLayer().bakeRoot());
            verify("chicken", Identifier.parse("scalebrews_test:s17_reference_chicken"),
                () -> AdultChickenModel.createBodyLayer().bakeRoot());
            verify("player_wide", Identifier.parse("scalebrews_test:s17_reference_player_wide"),
                () -> LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64).bakeRoot());
            System.out.println("S17_ORIGINAL_STRUCTURAL_PARITY PASS bidirectional original-tree coverage + deterministic SVG overlays");
        });
    }

    private static void verify(String label, Identifier source, Supplier<ModelPart> roots) {
        ModelPartGeometryEngine.registerSource(source,
            new ModelPartGeometryEngine.Source("26.2", roots, S17OriginalModelStructuralParityClientTests::defaultTransform));
        var engine = CollisionEngines.geometry(BuiltInGeometryEngines.MODEL_PART).orElseThrow();
        ModelGeometry geometry = engine.prepare(new GeometryEngine.Request(source)).orElseThrow();

        Map<String, List<Vec3>> expected = referencePieces(roots.get());
        Map<String, ConvexBox> actual = geometry.evaluate(new Matrix4f(), Map.of(), ALL_MATERIAL);
        writeSnapshot(label, expected, actual);

        if (!actual.keySet().equals(expected.keySet())) {
            var missing = new java.util.LinkedHashSet<>(expected.keySet());
            missing.removeAll(actual.keySet());
            var extra = new java.util.LinkedHashSet<>(actual.keySet());
            extra.removeAll(expected.keySet());
            throw new AssertionError("Original ModelPart structural piece mismatch for " + label
                + " missing=" + missing + " extra=" + extra);
        }

        for (var entry : expected.entrySet()) {
            List<Vec3> reference = entry.getValue();
            List<Vec3> prepared = actual.get(entry.getKey()).vertices();
            if (reference.size() != prepared.size())
                throw new AssertionError("Vertex cardinality mismatch for " + label + " " + entry.getKey()
                    + ": original=" + reference.size() + " prepared=" + prepared.size());
            for (Vec3 vertex : reference) if (!contains(prepared, vertex))
                throw new AssertionError("Prepared geometry misses original vertex for " + label + " " + entry.getKey() + ": " + vertex);
            for (Vec3 vertex : prepared) if (!contains(reference, vertex))
                throw new AssertionError("Prepared geometry invents vertex for " + label + " " + entry.getKey() + ": " + vertex);
        }
    }

    private static Map<String, List<Vec3>> referencePieces(ModelPart root) {
        Map<String, List<Vec3>> result = new LinkedHashMap<>();
        root.visit(new com.mojang.blaze3d.vertex.PoseStack(), (pose, path, index, cube) -> {
            var local = new ArrayList<Vec3>();
            var transformed = new ArrayList<Vec3>();
            for (var polygon : cube.polygons) for (var vertex : polygon.vertices()) {
                addUnique(local, new Vec3(vertex.worldX(), vertex.worldY(), vertex.worldZ()));
                Vector3f p = pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f());
                addUnique(transformed, new Vec3(p.x, p.y, p.z));
            }
            if (!volumetric(local)) return; // legitimate renderer-only plane
            String id = "root" + path + "/cube_" + index;
            if (result.putIfAbsent(id, List.copyOf(transformed)) != null)
                throw new AssertionError("Duplicate original ModelPart piece id: " + id);
        });
        return result;
    }

    private static boolean volumetric(List<Vec3> vertices) {
        if (vertices.isEmpty()) return false;
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3 v : vertices) {
            minX = Math.min(minX, v.x); maxX = Math.max(maxX, v.x);
            minY = Math.min(minY, v.y); maxY = Math.max(maxY, v.y);
            minZ = Math.min(minZ, v.z); maxZ = Math.max(maxZ, v.z);
        }
        return maxX > minX && maxY > minY && maxZ > minZ;
    }

    private static void addUnique(List<Vec3> vertices, Vec3 candidate) {
        if (!contains(vertices, candidate)) vertices.add(candidate);
    }

    private static boolean contains(List<Vec3> vertices, Vec3 target) {
        return vertices.stream().anyMatch(vertex -> vertex.distanceToSqr(target) <= EPS2);
    }

    private static Matrix4f defaultTransform() {
        return new Matrix4f().scaling(-1, -1, 1).translate(0, -1.501f, 0);
    }

    /** Deterministic orthographic two-view snapshot: reference dots + prepared crosses. */
    private static void writeSnapshot(String label, Map<String, List<Vec3>> expected, Map<String, ConvexBox> actual) {
        try {
            List<Vec3> reference = expected.values().stream().flatMap(List::stream).toList();
            List<Vec3> prepared = actual.values().stream().flatMap(box -> box.vertices().stream()).toList();
            Path dir = FabricLoader.getInstance().getGameDir().resolve("s17-retro-snapshots");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(label + ".svg"), svg(label, reference, prepared));
        } catch (java.io.IOException failure) {
            throw new AssertionError("Could not write S17 structural snapshot for " + label, failure);
        }
    }

    private static String svg(String label, List<Vec3> reference, List<Vec3> prepared) {
        StringBuilder out = new StringBuilder();
        out.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"900\" height=\"430\" viewBox=\"0 0 900 430\">\n")
            .append("<rect width=\"900\" height=\"430\" fill=\"white\"/>\n")
            .append("<text x=\"20\" y=\"24\" font-family=\"monospace\" font-size=\"16\">S17 ")
            .append(escape(label)).append(" | original dots / prepared crosses | Minecraft 26.2</text>\n");
        drawPanel(out, reference, prepared, 10, 35, 430, 380, 0, 1, "XY");
        drawPanel(out, reference, prepared, 460, 35, 430, 380, 0, 2, "XZ");
        out.append("</svg>\n");
        return out.toString();
    }

    private static void drawPanel(StringBuilder out, List<Vec3> reference, List<Vec3> prepared,
                                  double x0, double y0, double width, double height, int axisA, int axisB, String title) {
        List<Vec3> all = new ArrayList<>(reference); all.addAll(prepared);
        double minA = Double.POSITIVE_INFINITY, maxA = Double.NEGATIVE_INFINITY;
        double minB = Double.POSITIVE_INFINITY, maxB = Double.NEGATIVE_INFINITY;
        for (Vec3 v : all) {
            double a = axis(v, axisA), b = axis(v, axisB);
            minA = Math.min(minA, a); maxA = Math.max(maxA, a);
            minB = Math.min(minB, b); maxB = Math.max(maxB, b);
        }
        if (all.isEmpty()) { minA = minB = -1; maxA = maxB = 1; }
        double spanA = Math.max(1e-6, maxA - minA), spanB = Math.max(1e-6, maxB - minB);
        double scale = Math.min((width - 40) / spanA, (height - 50) / spanB);
        double cx = x0 + width / 2, cy = y0 + height / 2;
        double midA = (minA + maxA) / 2, midB = (minB + maxB) / 2;
        out.append(String.format(Locale.ROOT, "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"none\" stroke=\"#aaa\"/>\n", x0, y0, width, height));
        out.append(String.format(Locale.ROOT, "<text x=\"%.1f\" y=\"%.1f\" font-family=\"monospace\" font-size=\"13\">%s</text>\n", x0 + 8, y0 + 18, title));
        for (Vec3 v : reference) {
            double x = cx + (axis(v, axisA) - midA) * scale;
            double y = cy - (axis(v, axisB) - midB) * scale;
            out.append(String.format(Locale.ROOT, "<circle cx=\"%.3f\" cy=\"%.3f\" r=\"2.2\" fill=\"#1687d9\" fill-opacity=\"0.70\"/>\n", x, y));
        }
        for (Vec3 v : prepared) {
            double x = cx + (axis(v, axisA) - midA) * scale;
            double y = cy - (axis(v, axisB) - midB) * scale;
            out.append(String.format(Locale.ROOT, "<path d=\"M %.3f %.3f l 5 5 M %.3f %.3f l -5 5\" stroke=\"#d43f3a\" stroke-width=\"1.2\"/>\n", x - 2.5, y - 2.5, x + 2.5, y - 2.5));
        }
    }

    private static double axis(Vec3 v, int axis) {
        return axis == 0 ? v.x : axis == 1 ? v.y : v.z;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
