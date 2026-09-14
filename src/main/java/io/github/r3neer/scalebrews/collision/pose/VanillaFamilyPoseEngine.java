package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Canonical server-safe Minecraft 26.2 pose formulae for reusable ordinary model families. */
public final class VanillaFamilyPoseEngine implements PoseEngine {
    public enum Family { CHICKEN, VILLAGER, IRON_GOLEM, GHAST, EQUINE, BEE }
    private static final float KEEP = Float.NaN;
    private static final Set<String> EQUINE_SOURCES = Set.of(
        "minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:skeleton_horse", "minecraft:zombie_horse");
    private final Family family;

    public VanillaFamilyPoseEngine(Family family) { this.family = Objects.requireNonNull(family); }

    @Override
    public Optional<Map<String, Matrix4f>> evaluate(ModelGeometry geometry, Inputs in, Map<String, String> parameters) {
        if (!parameters.isEmpty() || !in.ordinary() || !"26.2".equals(geometry.version()) || !supportsSource(geometry.source()))
            return Optional.empty();
        Map<String, Matrix4f> out = new LinkedHashMap<>();
        switch (family) {
            case CHICKEN -> chicken(geometry, in, out);
            case VILLAGER -> villager(geometry, in, out);
            case IRON_GOLEM -> golem(geometry, in, out);
            case GHAST -> ghast(geometry, in, out);
            case EQUINE -> equine(geometry, in, out);
            case BEE -> bee(geometry, in, out);
        }
        return complete(out) ? Optional.of(Collections.unmodifiableMap(out)) : Optional.empty();
    }

    private boolean supportsSource(String source) {
        return switch (family) {
            case CHICKEN -> "minecraft:chicken".equals(source);
            case VILLAGER -> "minecraft:villager".equals(source);
            case IRON_GOLEM -> "minecraft:iron_golem".equals(source);
            case GHAST -> "minecraft:ghast".equals(source);
            case EQUINE -> EQUINE_SOURCES.contains(source);
            case BEE -> "minecraft:bee".equals(source);
        };
    }

    private boolean complete(Map<String, Matrix4f> out) {
        return switch (family) {
            case CHICKEN -> has(out, "head", "right_leg", "left_leg", "right_wing", "left_wing");
            case VILLAGER -> has(out, "head", "right_leg", "left_leg");
            case IRON_GOLEM -> has(out, "head", "right_arm", "left_arm", "right_leg", "left_leg");
            case GHAST -> has(out, "tentacle0", "tentacle1", "tentacle2", "tentacle3", "tentacle4", "tentacle5", "tentacle6", "tentacle7", "tentacle8");
            case EQUINE -> has(out, "body", "head_parts", "left_hind_leg", "right_hind_leg", "left_front_leg", "right_front_leg", "tail");
            case BEE -> has(out, "bone", "right_wing", "left_wing", "front_legs", "middle_legs", "back_legs", "left_antenna", "right_antenna");
        };
    }

    private static boolean has(Map<String, Matrix4f> out, String... names) {
        for (String wanted : names) {
            boolean present = false;
            for (String id : out.keySet()) if (id.substring(id.lastIndexOf('/') + 1).equals(wanted)) { present = true; break; }
            if (!present) return false;
        }
        return true;
    }

    private static void chicken(ModelGeometry g, Inputs in, Map<String, Matrix4f> out) {
        if (!required(in, "flap", "flap_speed")) return;
        float a = Mth.cos(in.walkPhase() * .6662f) * 1.4f * in.walkAmount();
        float b = Mth.cos(in.walkPhase() * .6662f + (float)Math.PI) * 1.4f * in.walkAmount();
        float flap = (Mth.sin(in.channel("flap", 0)) + 1) * in.channel("flap_speed", 0);
        for (var p : g.parts()) switch (name(p)) {
            case "head" -> put(out, p, 0, 0, 0, in.headPitch() * Mth.DEG_TO_RAD, in.headYaw() * Mth.DEG_TO_RAD, KEEP);
            case "right_leg" -> put(out, p, 0, 0, 0, a, KEEP, KEEP);
            case "left_leg" -> put(out, p, 0, 0, 0, b, KEEP, KEEP);
            case "right_wing" -> put(out, p, 0, 0, 0, KEEP, KEEP, flap);
            case "left_wing" -> put(out, p, 0, 0, 0, KEEP, KEEP, -flap);
        }
    }

    private static void villager(ModelGeometry g, Inputs in, Map<String, Matrix4f> out) {
        if (!required(in, "unhappy")) return;
        float a = Mth.cos(in.walkPhase() * .6662f) * .7f * in.walkAmount();
        float b = Mth.cos(in.walkPhase() * .6662f + (float)Math.PI) * .7f * in.walkAmount();
        for (var p : g.parts()) switch (name(p)) {
            case "head" -> {
                boolean unhappy = in.flag("unhappy");
                put(out, p, 0, 0, 0, unhappy ? .4f : in.headPitch() * Mth.DEG_TO_RAD,
                    in.headYaw() * Mth.DEG_TO_RAD, unhappy ? .3f * Mth.sin(.45f * in.age()) : 0f);
            }
            case "right_leg" -> put(out, p, 0, 0, 0, a, 0f, KEEP);
            case "left_leg" -> put(out, p, 0, 0, 0, b, 0f, KEEP);
        }
    }

    private static void golem(ModelGeometry g, Inputs in, Map<String, Matrix4f> out) {
        if (!required(in, "attack", "flower")) return;
        float wave = Mth.triangleWave(in.walkPhase(), 13), speed = in.walkAmount();
        float attack = in.channel("attack", 0), flower = in.channel("flower", 0), rightArm, leftArm;
        if (attack > 0) rightArm = leftArm = -2 + 1.5f * Mth.triangleWave(attack, 10);
        else if (flower > 0) { rightArm = -.8f + .025f * Mth.triangleWave(flower, 70); leftArm = 0; }
        else { rightArm = (-.2f + 1.5f * wave) * speed; leftArm = (-.2f - 1.5f * wave) * speed; }
        for (var p : g.parts()) switch (name(p)) {
            case "head" -> put(out, p, 0, 0, 0, in.headPitch() * Mth.DEG_TO_RAD, in.headYaw() * Mth.DEG_TO_RAD, KEEP);
            case "right_arm" -> put(out, p, 0, 0, 0, rightArm, KEEP, KEEP);
            case "left_arm" -> put(out, p, 0, 0, 0, leftArm, KEEP, KEEP);
            case "right_leg" -> put(out, p, 0, 0, 0, -1.5f * wave * speed, 0f, KEEP);
            case "left_leg" -> put(out, p, 0, 0, 0, 1.5f * wave * speed, 0f, KEEP);
        }
    }

    private static void ghast(ModelGeometry g, Inputs in, Map<String, Matrix4f> out) {
        for (var p : g.parts()) if (name(p).startsWith("tentacle")) {
            try {
                int index = Integer.parseInt(name(p).substring("tentacle".length()));
                put(out, p, 0, 0, 0, .2f * Mth.sin(in.age() * .3f + index) + .4f, KEEP, KEEP);
            } catch (NumberFormatException ignored) {}
        }
    }

    private static void equine(ModelGeometry g, Inputs in, Map<String, Matrix4f> out) {
        if (!required(in, "eat", "stand", "mouth", "tail", "water")) return;
        float animationSpeed = in.walkAmount();
        float animationPos = in.walkPhase();
        float clampedYRot = Mth.clamp(in.headYaw(), -20f, 20f);
        float headRotXRad = in.headPitch() * Mth.DEG_TO_RAD;
        if (animationSpeed > .2f) headRotXRad += Mth.cos(animationPos * .8f) * .15f * animationSpeed;

        float eating = in.channel("eat", 0);
        float standing = in.channel("stand", 0);
        float iStanding = 1f - standing;
        float feeding = in.channel("mouth", 0);
        float waterMultiplier = in.flag("water") ? .2f : 1f;
        float legAnim1 = Mth.cos(waterMultiplier * animationPos * .6662f + (float)Math.PI);
        float legXRotAnim = legAnim1 * .8f * animationSpeed;
        float baseHeadAngle = (1f - Math.max(standing, eating))
            * ((float)Math.PI / 6f + headRotXRad + feeding * Mth.sin(in.age()) * .05f);
        float headX = standing * ((float)Math.PI / 12f + headRotXRad)
            + eating * (2.1816616f + Mth.sin(in.age()) * .05f) + baseHeadAngle;
        float headY = standing * clampedYRot * Mth.DEG_TO_RAD
            + (1f - Math.max(standing, eating)) * clampedYRot * Mth.DEG_TO_RAD;
        float headDy = Mth.lerp(eating, Mth.lerp(standing, 0f, -8f), 7f);
        float standAngle = (float)Math.PI / 12f * standing;
        float bobValue = Mth.cos(in.age() * .6f + (float)Math.PI);
        float rightFrontX = (-(float)Math.PI / 3f + bobValue) * standing + legXRotAnim * iStanding;
        float leftFrontX = (-(float)Math.PI / 3f - bobValue) * standing - legXRotAnim * iStanding;
        float leftHindX = standAngle - legAnim1 * .5f * animationSpeed * iStanding;
        float rightHindX = standAngle + legAnim1 * .5f * animationSpeed * iStanding;
        float ageScale = in.channel("age_scale", 1f);

        for (var p : g.parts()) {
            var source = p.sourcePose();
            switch (name(p)) {
                case "body" -> {
                    float restX = source == null ? 0f : source.xRot();
                    put(out, p, 0, 0, 0, standing * (float)(-Math.PI / 4) + iStanding * restX, KEEP, KEEP);
                }
                case "head_parts" -> {
                    float sourceZ = source == null ? -12f : source.z();
                    float desiredZ = Mth.lerp(standing, sourceZ, -4f);
                    put(out, p, 0, headDy, desiredZ - sourceZ, headX, headY, KEEP);
                }
                case "left_hind_leg" -> put(out, p, 0, 0, 0, leftHindX, KEEP, KEEP);
                case "right_hind_leg" -> put(out, p, 0, 0, 0, rightHindX, KEEP, KEEP);
                case "left_front_leg" -> put(out, p, 0, -12f * standing, 4f * standing, rightFrontX, KEEP, KEEP);
                case "right_front_leg" -> put(out, p, 0, -12f * standing, 4f * standing, leftFrontX, KEEP, KEEP);
                case "tail" -> put(out, p, 0, animationSpeed * ageScale, animationSpeed * 2f * ageScale,
                    (float)Math.PI / 6f + animationSpeed * .75f,
                    in.flag("tail") ? Mth.cos(in.age() * .7f) : 0f, KEEP);
            }
        }
    }

    private static void bee(ModelGeometry g, Inputs in, Map<String, Matrix4f> out) {
        if (!required(in, "on_ground", "angry", "roll")) return;
        boolean ground = in.flag("on_ground"), angry = in.flag("angry");
        float age = in.age();
        float wave = Mth.cos(age * .18f);
        float wing = Mth.cos(age * 2.1f) * (float)Math.PI * .15f;

        float boneX = KEEP, boneDy = 0f;
        float rightWingX = KEEP, rightWingY = KEEP, rightWingZ = KEEP;
        float leftWingX = KEEP, leftWingY = KEEP, leftWingZ = KEEP;
        float frontLegX = KEEP, middleLegX = KEEP, backLegX = KEEP;
        float antennaX = KEEP;
        if (!ground) {
            rightWingY = 0f;
            rightWingZ = wing;
            leftWingX = sourceRotation(g, "right_wing", 0);
            leftWingY = 0f;
            leftWingZ = -wing;
            frontLegX = middleLegX = backLegX = (float)Math.PI / 4f;
        }
        if (!angry && !ground) {
            boneX = .1f + wave * (float)Math.PI * .025f;
            boneDy = -wave * .9f;
            frontLegX = -wave * (float)Math.PI * .1f + (float)Math.PI / 8f;
            backLegX = -wave * (float)Math.PI * .05f + (float)Math.PI / 4f;
            antennaX = wave * (float)Math.PI * .03f;
        }
        float roll = in.channel("roll", 0);
        if (roll > 0) {
            float current = Float.isNaN(boneX) ? sourceRotation(g, "bone", 0) : boneX;
            boneX = Mth.rotLerpRad(roll, current, 3.0915928f);
        }

        for (var p : g.parts()) switch (name(p)) {
            case "bone" -> put(out, p, 0, boneDy, 0, boneX, KEEP, KEEP);
            case "right_wing" -> put(out, p, 0, 0, 0, rightWingX, rightWingY, rightWingZ);
            case "left_wing" -> put(out, p, 0, 0, 0, leftWingX, leftWingY, leftWingZ);
            case "front_legs" -> put(out, p, 0, 0, 0, frontLegX, KEEP, KEEP);
            case "middle_legs" -> put(out, p, 0, 0, 0, middleLegX, KEEP, KEEP);
            case "back_legs" -> put(out, p, 0, 0, 0, backLegX, KEEP, KEEP);
            case "left_antenna", "right_antenna" -> put(out, p, 0, 0, 0, antennaX, KEEP, KEEP);
        }
    }

    private static float sourceRotation(ModelGeometry geometry, String partName, int axis) {
        for (var part : geometry.parts()) if (name(part).equals(partName) && part.sourcePose() != null) {
            return switch (axis) {
                case 0 -> part.sourcePose().xRot();
                case 1 -> part.sourcePose().yRot();
                case 2 -> part.sourcePose().zRot();
                default -> throw new IllegalArgumentException("Invalid rotation axis");
            };
        }
        return 0f;
    }

    private static boolean required(Inputs in, String... names) {
        for (String name : names) if (!in.channels().containsKey(name)) return false;
        return true;
    }

    private static String name(ModelGeometry.Part p) { return p.id().substring(p.id().lastIndexOf('/') + 1); }
    private static void put(Map<String, Matrix4f> out, ModelGeometry.Part p, float dx, float dy, float dz, float x, float y, float z) {
        var source = p.sourcePose();
        if (source != null) {
            float rx = Float.isNaN(x) ? source.xRot() : x;
            float ry = Float.isNaN(y) ? source.yRot() : y;
            float rz = Float.isNaN(z) ? source.zRot() : z;
            out.put(p.id(), new Matrix4f().translation(
                    source.x() / 16f + dx / 16f, source.y() / 16f + dy / 16f, source.z() / 16f + dz / 16f)
                .rotateZYX(rz, ry, rx).scale(source.xScale(), source.yScale(), source.zScale()));
            return;
        }
        Matrix4f rest = ModelGeometry.matrix(p.transform());
        Vector3f translation = rest.getTranslation(new Vector3f()), scale = rest.getScale(new Vector3f());
        float rx = Float.isNaN(x) ? 0f : x;
        float ry = Float.isNaN(y) ? 0f : y;
        float rz = Float.isNaN(z) ? 0f : z;
        out.put(p.id(), new Matrix4f().translation(translation.x + dx / 16, translation.y + dy / 16, translation.z + dz / 16)
            .rotateZYX(rz, ry, rx).scale(scale));
    }
}
