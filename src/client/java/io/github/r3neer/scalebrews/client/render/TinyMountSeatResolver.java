package io.github.r3neer.scalebrews.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.client.mixin.ModelPartAccess;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolves a stable rest-pose surface once, then samples its complete animated chain each frame. */
public final class TinyMountSeatResolver {
    private static final Map<ModelPart, Map<String, Selection>> CACHE = new IdentityHashMap<>();
    private static final Set<String> WARNED = new HashSet<>();
    private TinyMountSeatResolver() {}

    public static synchronized void clearCaches() { CACHE.clear(); }

    public static SeatFrame resolve(ModelPart root, Matrix4fc outer, TinyMountVisualProfile profile,
                                    long serial, String diagnosticName) {
        String requested = profile.anchor().path() == null ? "" : profile.anchor().path();
        String variantKey = requested + '\0' + variantSignature(root);
        Selection selection;
        synchronized (TinyMountSeatResolver.class) {
            Map<String, Selection> variants = CACHE.computeIfAbsent(root, ignored -> new java.util.HashMap<>());
            selection = variants.get(variantKey);
            if (selection == null || !usable(selection)) {
                // Selection is model-space metadata, not a property of the first
                // entity/camera/GUI orientation that happens to use this model.
                selection = select(root, new Matrix4f().scale(-1, -1, 1), requested, diagnosticName);
                variants.put(variantKey, selection);
            }
        }
        Matrix4f currentPart = chain(selection.chain, false);
        Matrix4f referencePart = new Matrix4f(selection.referencePart);
        var anchor = profile.anchor();
        Vector3f local = new Vector3f(selection.right).mul(lerp(selection.rightMin, selection.rightMax, anchor.point().x))
                .add(new Vector3f(selection.up).mul(lerp(selection.upMin, selection.upMax, anchor.point().y)))
                .add(new Vector3f(selection.front).mul(lerp(selection.frontMin, selection.frontMax, anchor.point().z)));
        Matrix4f localSeat = new Matrix4f().identity()
                .setColumn(0, new Vector4f(selection.right, 0))
                .setColumn(1, new Vector4f(new Vector3f(selection.up).negate(), 0))
                .setColumn(2, new Vector4f(selection.front, 0))
                .setColumn(3, new Vector4f(local, 1))
                .translate(anchor.offset().x / 16F, -anchor.offset().y / 16F, anchor.offset().z / 16F)
                .rotateXYZ((float)Math.toRadians(anchor.rotation().x), (float)Math.toRadians(anchor.rotation().y),
                        (float)Math.toRadians(anchor.rotation().z));
        Matrix4f currentAbsolute = new Matrix4f(outer).mul(currentPart).mul(localSeat);
        Matrix4f referenceAbsolute = new Matrix4f(outer).mul(referencePart).mul(localSeat);
        Matrix4f relative = new Matrix4f(currentAbsolute).mul(new Matrix4f(referenceAbsolute).invert());
        Matrix4f delta = rigid(relative);
        float width = Math.max(1F / 16, selection.width * profile.saddle().width());
        float depth = Math.max(1F / 16, selection.depth * profile.saddle().length());
        Matrix4f saddle = new Matrix4f(currentPart).mul(localSeat);
        Vector3f up = currentAbsolute.transformDirection(new Vector3f(0, -1, 0)).normalize();
        float strapScale = Math.max(.15F, selection.height * 16F / 6F * .55F)
                * profile.saddle().strapLength();
        return new SeatFrame(selection.path, saddle, currentAbsolute.getTranslation(new Vector3f()), up, delta,
                width, depth, strapScale, profile.saddle().seatHeight(), serial);
    }

    private static Selection select(ModelPart root, Matrix4fc outer, String requested, String diagnosticName) {
        List<Node> nodes = new ArrayList<>();
        collect(root, "root", List.of(root), true, nodes);
        if (!requested.isEmpty()) {
            String normalized = requested.equals("root") || requested.startsWith("root/") ? requested : "root/" + requested;
            Node explicit = nodes.stream().filter(n -> n.path.equals(normalized)).findFirst().orElse(null);
            if (explicit != null) {
                List<Face> candidates = new ArrayList<>();
                for (Node node : nodes)
                    if (node.path.equals(normalized) || node.path.startsWith(normalized + "/"))
                        collectFaces(node, outer, candidates);
                Selection found = candidates.stream().max(Comparator.comparingDouble(f -> f.area))
                        .map(Face::selection).orElse(null);
                if (found != null) return found;
            }
            warnOnce(diagnosticName + ":missing:" + requested,
                    "Tiny Mount visual path '" + requested + "' is absent or has no usable top face for "
                            + diagnosticName + "; using autodetection");
        }
        List<Face> faces = new ArrayList<>();
        for (Node node : nodes) collectFaces(node, outer, faces);
        if (!faces.isEmpty()) {
            float largest = faces.stream().map(f -> f.area).max(Float::compare).orElse(0F);
            faces.removeIf(f -> f.area < largest * .05F);
            Bounds global = bounds(faces);
            for (Face face : faces) face.score = score(face, global, largest);
            Face best = faces.stream().max(Comparator.comparingDouble((Face f) -> f.score)
                    .thenComparing(f -> reverseLexicographic(f.node.path))).orElseThrow();
            return best.selection();
        }
        warnOnce(diagnosticName + ":fallback", "No usable ModelPart geometry for " + diagnosticName
                + "; using the model origin as a static Tiny Mount seat");
        return new Selection("root", List.of(root), chain(List.of(root), false),
                new Vector3f(1, 0, 0), new Vector3f(0, -1, 0),
                new Vector3f(0, 0, 1), -.25F, .25F, 0, .5F, -.25F, .25F, .5F, .5F, .5F);
    }

    private static String reverseLexicographic(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) result.append((char)(Character.MAX_VALUE - value.charAt(i)));
        return result.toString();
    }

    private static Selection bestInNode(Node node, Matrix4fc outer) {
        List<Face> faces = new ArrayList<>();
        collectFaces(node, outer, faces);
        Face face = faces.stream().max(Comparator.comparingDouble(f -> f.area)).orElse(null);
        return face == null ? null : face.selection();
    }

    private static void collect(ModelPart part, String path, List<ModelPart> chain, boolean parentVisible, List<Node> output) {
        boolean visible = parentVisible && part.visible;
        output.add(new Node(path, List.copyOf(chain), part, visible));
        ((ModelPartAccess)(Object)part).scalebrews$children().entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                    List<ModelPart> childChain = new ArrayList<>(chain);
                    childChain.add(entry.getValue());
                    collect(entry.getValue(), path + "/" + entry.getKey(), childChain, visible, output);
                });
    }

    private static void collectFaces(Node node, Matrix4fc outer, List<Face> output) {
        if (!node.visible || node.part.skipDraw) return;
        Matrix4f posed = chain(node.chain, true);
        Matrix4f renderedPose = new Matrix4f(outer).mul(posed);
        Matrix3f normalTransform = new Matrix3f(renderedPose).invert().transpose();
        for (ModelPart.Cube cube : ((ModelPartAccess)(Object)node.part).scalebrews$cubes()) {
            Vector3f cubeMin = new Vector3f(cube.minX, cube.minY, cube.minZ).div(16);
            Vector3f cubeMax = new Vector3f(cube.maxX, cube.maxY, cube.maxZ).div(16);
            for (ModelPart.Polygon polygon : cube.polygons) {
                Vector3f normal = normalTransform.transform(new Vector3f(polygon.normal())).normalize();
                if (normal.y < .75F) continue;
                Vector3f min = new Vector3f(Float.POSITIVE_INFINITY), max = new Vector3f(Float.NEGATIVE_INFINITY);
                List<Vector3f> transformed = new ArrayList<>(polygon.vertices().length);
                for (ModelPart.Vertex vertex : polygon.vertices()) {
                    Vector3f local = new Vector3f(vertex.worldX(), vertex.worldY(), vertex.worldZ());
                    transformed.add(renderedPose.transformPosition(new Vector3f(local)));
                    min.min(local); max.max(local);
                }
                float area = polygonArea(transformed);
                if (Float.isFinite(area) && area > 1E-6F)
                    output.add(new Face(node, cubeMin, cubeMax, new Vector3f(polygon.normal()).normalize(),
                            renderedPose, posed, center(transformed), area));
            }
        }
    }

    private static boolean usable(Selection selection) {
        if (selection.chain.isEmpty()) return false;
        for (ModelPart part : selection.chain) if (!part.visible) return false;
        ModelPart selected = selection.chain.getLast();
        return !selected.skipDraw && !((ModelPartAccess)(Object)selected).scalebrews$cubes().isEmpty();
    }

    /** Includes replacement parts/cubes, but never walks polygon data or animated poses. */
    private static int variantSignature(ModelPart part) {
        int hash = 31 * System.identityHashCode(part) + Boolean.hashCode(part.visible);
        hash = 31 * hash + Boolean.hashCode(part.skipDraw);
        var access = (ModelPartAccess)(Object)part;
        hash = 31 * hash + access.scalebrews$cubes().size();
        for (var cube : access.scalebrews$cubes()) hash = 31 * hash + System.identityHashCode(cube);
        for (var entry : access.scalebrews$children().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            hash = 31 * hash + entry.getKey().hashCode();
            hash = 31 * hash + variantSignature(entry.getValue());
        }
        return hash;
    }

    private static float polygonArea(List<Vector3f> vertices) {
        if (vertices.size() < 3) return 0;
        Vector3f origin = vertices.getFirst();
        float area = 0;
        for (int i = 1; i + 1 < vertices.size(); i++)
            area += new Vector3f(vertices.get(i)).sub(origin).cross(new Vector3f(vertices.get(i + 1)).sub(origin)).length() * .5F;
        return area;
    }

    private static Vector3f center(List<Vector3f> points) {
        Vector3f center = new Vector3f();
        for (Vector3f point : points) center.add(point);
        return center.div(points.size());
    }

    private static Bounds bounds(List<Face> faces) {
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY), max = new Vector3f(Float.NEGATIVE_INFINITY);
        for (Face face : faces) { min.min(face.center); max.max(face.center); }
        return new Bounds(min, max);
    }

    private static double score(Face face, Bounds bounds, float largest) {
        float spanX = Math.max(.001F, bounds.max.x - bounds.min.x);
        float spanY = Math.max(.001F, bounds.max.y - bounds.min.y);
        float spanZ = Math.max(.001F, bounds.max.z - bounds.min.z);
        float centerX = (bounds.min.x + bounds.max.x) * .5F;
        float centerZ = (bounds.min.z + bounds.max.z) * .5F;
        float lateral = 1 - Math.min(1, Math.abs(face.center.x - centerX) / (spanX * .5F));
        float longitudinal = 1 - Math.min(1, Math.abs(face.center.z - centerZ) / (spanZ * .5F));
        float upper = Math.clamp((face.center.y - bounds.min.y) / spanY, 0, 1);
        return face.area / largest * 6 + lateral * 1.5 + longitudinal * 1.5 + upper;
    }

    private static Matrix4f chain(List<ModelPart> parts, boolean rest) {
        PoseStack stack = new PoseStack();
        for (ModelPart part : parts) apply(stack, rest ? part.getInitialPose() : part.storePose(), part, rest);
        return new Matrix4f(stack.last().pose());
    }

    private static void apply(PoseStack stack, PartPose pose, ModelPart part, boolean rest) {
        stack.translate(pose.x() / 16, pose.y() / 16, pose.z() / 16);
        if (pose.xRot() != 0 || pose.yRot() != 0 || pose.zRot() != 0)
            stack.mulPose(new Quaternionf().rotationZYX(pose.zRot(), pose.yRot(), pose.xRot()));
        float x = rest ? pose.xScale() : part.xScale;
        float y = rest ? pose.yScale() : part.yScale;
        float z = rest ? pose.zScale() : part.zScale;
        if (x != 1 || y != 1 || z != 1) stack.scale(x, y, z);
    }

    private static Matrix4f rigid(Matrix4fc source) {
        Vector3f translation = source.getTranslation(new Vector3f());
        Quaternionf rotation = source.getUnnormalizedRotation(new Quaternionf()).normalize();
        return new Matrix4f().translation(translation).rotate(rotation);
    }

    private static float lerp(float min, float max, float value) { return min + (max - min) * value; }
    private static void warnOnce(String key, String message) {
        synchronized (WARNED) { if (WARNED.add(key)) ScaleBrews.LOGGER.warn(message); }
    }

    private record Node(String path, List<ModelPart> chain, ModelPart part, boolean visible) {}
    private record Selection(String path, List<ModelPart> chain, Matrix4f referencePart,
                             Vector3f right, Vector3f up, Vector3f front,
                             float rightMin, float rightMax, float upMin, float upMax,
                             float frontMin, float frontMax, float width, float depth, float height) {}
    private record Bounds(Vector3f min, Vector3f max) {}
    private static final class Face {
        final Node node; final Vector3f cubeMin, cubeMax, localNormal, renderedRight, center;
        final Matrix4f referencePart;
        final float area; double score;
        Face(Node node, Vector3f cubeMin, Vector3f cubeMax, Vector3f localNormal,
             Matrix4fc renderedPose, Matrix4fc referencePart, Vector3f center, float area) {
            this.node = node; this.cubeMin = cubeMin; this.cubeMax = cubeMax;
            this.localNormal = localNormal; this.renderedRight = right(localNormal, renderedPose);
            this.referencePart = new Matrix4f(referencePart);
            this.center = center; this.area = area;
        }
        Selection selection() {
            Vector3f up = new Vector3f(localNormal);
            Vector3f down = new Vector3f(up).negate();
            Vector3f front = new Vector3f(renderedRight).cross(down).normalize();
            float[] r = projection(cubeMin, cubeMax, renderedRight);
            float[] u = projection(cubeMin, cubeMax, up);
            float[] f = projection(cubeMin, cubeMax, front);
            return new Selection(node.path, node.chain, referencePart, renderedRight, up, front,
                    r[0], r[1], u[0], u[1], f[0], f[1],
                    Math.max(1F / 16, r[1] - r[0]), Math.max(1F / 16, f[1] - f[0]),
                    Math.max(1F / 16, u[1] - u[0]));
        }

        private static Vector3f right(Vector3f normal, Matrix4fc renderedRest) {
            Vector3f best = null;
            float bestX = -1;
            for (Vector3f axis : new Vector3f[]{new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1)}) {
                if (Math.abs(axis.dot(normal)) > .5F) continue;
                Vector3f rendered = renderedRest.transformDirection(new Vector3f(axis)).normalize();
                float x = Math.abs(rendered.x);
                if (x > bestX) { bestX = x; best = rendered.x < 0 ? axis.negate() : axis; }
            }
            return best == null ? new Vector3f(1, 0, 0) : best;
        }

        private static float[] projection(Vector3f min, Vector3f max, Vector3f axis) {
            float low = Float.POSITIVE_INFINITY, high = Float.NEGATIVE_INFINITY;
            for (float x : new float[]{min.x, max.x}) for (float y : new float[]{min.y, max.y})
                for (float z : new float[]{min.z, max.z}) {
                    float value = axis.dot(x, y, z);
                    low = Math.min(low, value); high = Math.max(high, value);
                }
            return new float[]{low, high};
        }
    }
}
