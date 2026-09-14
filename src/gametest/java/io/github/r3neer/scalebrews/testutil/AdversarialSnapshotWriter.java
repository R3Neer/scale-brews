package io.github.r3neer.scalebrews.testutil;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Deterministic SVG diagnostics for adversarial material-physics GameTests. */
public final class AdversarialSnapshotWriter {
    private static final int WIDTH = 960;
    private static final int HEIGHT = 500;

    private AdversarialSnapshotWriter() {}

    public record LabeledBox(String label, AABB box) {
        public LabeledBox {
            if (label == null || label.isBlank() || box == null) throw new IllegalArgumentException("Invalid snapshot box");
        }
    }

    public static void curvedPathXZ(String fileName, String title, AABB body,
                                    List<Vec3> displacements, Vec3 chordEnd,
                                    List<LabeledBox> obstacles, String outcome) {
        if (body == null || displacements == null || displacements.size() < 2 || chordEnd == null || obstacles == null)
            throw new IllegalArgumentException("Invalid curved-path snapshot input");
        Vec3 origin = body.getCenter();
        double minX = body.minX - origin.x, maxX = body.maxX - origin.x;
        double minZ = body.minZ - origin.z, maxZ = body.maxZ - origin.z;
        for (Vec3 delta : displacements) {
            minX = Math.min(minX, body.minX + delta.x - origin.x);
            maxX = Math.max(maxX, body.maxX + delta.x - origin.x);
            minZ = Math.min(minZ, body.minZ + delta.z - origin.z);
            maxZ = Math.max(maxZ, body.maxZ + delta.z - origin.z);
        }
        for (LabeledBox obstacle : obstacles) {
            minX = Math.min(minX, obstacle.box.minX - origin.x);
            maxX = Math.max(maxX, obstacle.box.maxX - origin.x);
            minZ = Math.min(minZ, obstacle.box.minZ - origin.z);
            maxZ = Math.max(maxZ, obstacle.box.maxZ - origin.z);
        }
        minX -= .4; maxX += .4; minZ -= .4; maxZ += .4;
        Plot plot = new Plot(90, 70, 800, 350, minX, maxX, minZ, maxZ);

        StringBuilder svg = start(title, "XZ | certified arc vs endpoint chord", outcome);
        svg.append("<defs><marker id=\"arrow\" markerWidth=\"8\" markerHeight=\"8\" refX=\"7\" refY=\"4\" orient=\"auto\"><path d=\"M0,0 L8,4 L0,8 z\" fill=\"#555\"/></marker></defs>\n");
        for (LabeledBox obstacle : obstacles) drawBoxXZ(svg, plot, obstacle.box, origin, "#d62728", .18, 2.2, obstacle.label);

        StringBuilder points = new StringBuilder();
        for (int i = 0; i < displacements.size(); i++) {
            Vec3 delta = displacements.get(i);
            double cx = delta.x;
            double cz = delta.z;
            if (i > 0) points.append(' ');
            points.append(fmt(plot.x(cx))).append(',').append(fmt(plot.y(cz)));
            if (i % Math.max(1, (displacements.size() - 1) / 4) == 0 || i == displacements.size() - 1)
                drawBodyXZ(svg, plot, body, origin, delta, "#1f77b4", .08, 1.2);
        }
        svg.append("<polyline points=\"").append(points).append("\" fill=\"none\" stroke=\"#1f77b4\" stroke-width=\"4\"/>\n");
        svg.append(String.format(Locale.ROOT,
            "<line x1=\"%.3f\" y1=\"%.3f\" x2=\"%.3f\" y2=\"%.3f\" stroke=\"#555\" stroke-width=\"2\" stroke-dasharray=\"8 6\" marker-end=\"url(#arrow)\"/>\n",
            plot.x(0), plot.y(0), plot.x(chordEnd.x), plot.y(chordEnd.z)));
        svg.append(label(95, 445, "blue = certified material path/body samples; dashed = endpoint chord; red = intermediate obstacle"));
        svg.append("</svg>\n");
        write(fileName, svg.toString());
    }

    public static void multicontact(String fileName, String title, AABB body,
                                    List<LabeledBox> constraints, Vec3 requested,
                                    Vec3 moved, String outcome) {
        if (body == null || constraints == null || requested == null || moved == null)
            throw new IllegalArgumentException("Invalid multicontact snapshot input");
        Vec3 origin = body.getCenter();
        AABB end = body.move(moved);
        StringBuilder svg = start(title, "two projections | requested vs solver-allowed displacement", outcome);
        svg.append("<defs><marker id=\"reqArrow\" markerWidth=\"8\" markerHeight=\"8\" refX=\"7\" refY=\"4\" orient=\"auto\"><path d=\"M0,0 L8,4 L0,8 z\" fill=\"#888\"/></marker><marker id=\"moveArrow\" markerWidth=\"8\" markerHeight=\"8\" refX=\"7\" refY=\"4\" orient=\"auto\"><path d=\"M0,0 L8,4 L0,8 z\" fill=\"#1f77b4\"/></marker></defs>\n");

        Bounds xz = bounds(body, end, constraints, origin, true);
        Bounds xy = bounds(body, end, constraints, origin, false);
        Plot left = new Plot(55, 95, 405, 320, xz.minA, xz.maxA, xz.minB, xz.maxB);
        Plot right = new Plot(505, 95, 405, 320, xy.minA, xy.maxA, xy.minB, xy.maxB);
        svg.append(label(60, 88, "XZ projection"));
        svg.append(label(510, 88, "XY projection"));
        for (LabeledBox constraint : constraints) {
            drawBoxXZ(svg, left, constraint.box, origin, "#d62728", .12, 1.7, constraint.label);
            drawBoxXY(svg, right, constraint.box, origin, "#d62728", .12, 1.7, constraint.label);
        }
        drawBoxXZ(svg, left, body, origin, "#555", .08, 1.5, "body@t0");
        drawBoxXZ(svg, left, end, origin, "#1f77b4", .14, 2.2, "body@allowed");
        drawBoxXY(svg, right, body, origin, "#555", .08, 1.5, "body@t0");
        drawBoxXY(svg, right, end, origin, "#1f77b4", .14, 2.2, "body@allowed");
        drawArrow(svg, left, 0, 0, requested.x, requested.z, "#888", "reqArrow");
        drawArrow(svg, left, 0, 0, moved.x, moved.z, "#1f77b4", "moveArrow");
        drawArrow(svg, right, 0, 0, requested.x, requested.y, "#888", "reqArrow");
        drawArrow(svg, right, 0, 0, moved.x, moved.y, "#1f77b4", "moveArrow");
        svg.append(label(70, 455, "gray arrow = request; blue arrow/body = solver result; red = simultaneous material constraints"));
        svg.append("</svg>\n");
        write(fileName, svg.toString());
    }

    private static Bounds bounds(AABB body, AABB end, List<LabeledBox> boxes, Vec3 origin, boolean xz) {
        double minA = Double.POSITIVE_INFINITY, maxA = Double.NEGATIVE_INFINITY;
        double minB = Double.POSITIVE_INFINITY, maxB = Double.NEGATIVE_INFINITY;
        for (AABB box : concat(body, end, boxes)) {
            double a0 = (xz ? box.minX - origin.x : box.minX - origin.x);
            double a1 = (xz ? box.maxX - origin.x : box.maxX - origin.x);
            double b0 = xz ? box.minZ - origin.z : box.minY - origin.y;
            double b1 = xz ? box.maxZ - origin.z : box.maxY - origin.y;
            minA = Math.min(minA, a0); maxA = Math.max(maxA, a1);
            minB = Math.min(minB, b0); maxB = Math.max(maxB, b1);
        }
        double margin = .45;
        return new Bounds(minA - margin, maxA + margin, minB - margin, maxB + margin);
    }

    private static List<AABB> concat(AABB body, AABB end, List<LabeledBox> boxes) {
        var out = new java.util.ArrayList<AABB>();
        out.add(body); out.add(end); boxes.forEach(box -> out.add(box.box));
        return out;
    }

    private static void drawBodyXZ(StringBuilder svg, Plot plot, AABB body, Vec3 origin, Vec3 delta,
                                   String color, double opacity, double width) {
        AABB moved = body.move(delta);
        drawBoxXZ(svg, plot, moved, origin, color, opacity, width, null);
    }

    private static void drawBoxXZ(StringBuilder svg, Plot plot, AABB box, Vec3 origin,
                                  String color, double opacity, double width, String text) {
        drawRect(svg, plot, box.minX - origin.x, box.maxX - origin.x, box.minZ - origin.z, box.maxZ - origin.z,
            color, opacity, width, text);
    }

    private static void drawBoxXY(StringBuilder svg, Plot plot, AABB box, Vec3 origin,
                                  String color, double opacity, double width, String text) {
        drawRect(svg, plot, box.minX - origin.x, box.maxX - origin.x, box.minY - origin.y, box.maxY - origin.y,
            color, opacity, width, text);
    }

    private static void drawRect(StringBuilder svg, Plot plot, double minA, double maxA, double minB, double maxB,
                                 String color, double opacity, double width, String text) {
        double x = plot.x(minA), x2 = plot.x(maxA), y = plot.y(maxB), y2 = plot.y(minB);
        svg.append(String.format(Locale.ROOT,
            "<rect x=\"%.3f\" y=\"%.3f\" width=\"%.3f\" height=\"%.3f\" fill=\"%s\" fill-opacity=\"%.3f\" stroke=\"%s\" stroke-width=\"%.2f\"/>\n",
            Math.min(x, x2), Math.min(y, y2), Math.abs(x2 - x), Math.abs(y2 - y), color, opacity, color, width));
        if (text != null) svg.append(label(Math.min(x, x2) + 4, Math.min(y, y2) + 14, text));
    }

    private static void drawArrow(StringBuilder svg, Plot plot, double a0, double b0, double a1, double b1,
                                  String color, String marker) {
        svg.append(String.format(Locale.ROOT,
            "<line x1=\"%.3f\" y1=\"%.3f\" x2=\"%.3f\" y2=\"%.3f\" stroke=\"%s\" stroke-width=\"3\" marker-end=\"url(#%s)\"/>\n",
            plot.x(a0), plot.y(b0), plot.x(a1), plot.y(b1), color, marker));
    }

    private static StringBuilder start(String title, String subtitle, String outcome) {
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(WIDTH).append("\" height=\"").append(HEIGHT)
            .append("\" viewBox=\"0 0 ").append(WIDTH).append(' ').append(HEIGHT).append("\">\n")
            .append("<rect width=\"960\" height=\"500\" fill=\"white\"/>\n")
            .append(label(18, 25, title)).append(label(18, 47, subtitle));
        if (outcome != null && !outcome.isBlank()) svg.append(label(640, 25, outcome));
        return svg;
    }

    private static String label(double x, double y, String text) {
        return String.format(Locale.ROOT,
            "<text x=\"%.3f\" y=\"%.3f\" font-family=\"monospace\" font-size=\"13\" fill=\"#222\">%s</text>\n",
            x, y, escape(text));
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static void write(String fileName, String svg) {
        try {
            var dir = FabricLoader.getInstance().getGameDir().resolve("adversarial-snapshots");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(fileName), svg);
        } catch (IOException failure) {
            throw new AssertionError("Could not write adversarial snapshot " + fileName, failure);
        }
    }

    private record Bounds(double minA, double maxA, double minB, double maxB) {}

    private record Plot(double x, double y, double width, double height,
                        double minA, double maxA, double minB, double maxB) {
        private double sx() { return width / Math.max(1e-6, maxA - minA); }
        private double sy() { return height / Math.max(1e-6, maxB - minB); }
        private double scale() { return Math.min(sx(), sy()); }
        private double x(double a) { return x + (a - minA) * scale(); }
        private double y(double b) { return y + height - (b - minB) * scale(); }
    }
}
