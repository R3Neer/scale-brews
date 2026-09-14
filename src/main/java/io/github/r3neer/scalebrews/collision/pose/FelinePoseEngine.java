package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Exact common-side Minecraft 26.2 cat pose evaluator for adult and baby model families. */
public final class FelinePoseEngine implements PoseEngine {
    private static final String[] REQUIRED = {
        "crouching", "sprinting", "sitting", "lie", "lie_tail", "relax", "age_scale"
    };

    @Override
    public Optional<Map<String, Matrix4f>> evaluate(ModelGeometry geometry, Inputs in, Map<String, String> parameters) {
        if (!parameters.isEmpty() || !in.ordinary() || !"26.2".equals(geometry.version())
                || !"minecraft:cat".equals(geometry.source()) || !required(in)) return Optional.empty();

        var states = new LinkedHashMap<String, State>();
        for (var part : geometry.parts()) {
            String name = shortName(part);
            if (!isFelinePart(name)) continue;
            if (part.sourcePose() == null) return Optional.empty();
            states.put(name, new State(part));
        }
        if (!states.keySet().containsAll(java.util.Set.of("head", "body", "tail1", "tail2",
                "left_hind_leg", "right_hind_leg", "left_front_leg", "right_front_leg"))) return Optional.empty();

        float ageScale = in.channel("age_scale", 1f);
        if (!Float.isFinite(ageScale) || ageScale <= 0f) return Optional.empty();
        boolean baby = ageScale < .999f;
        boolean crouching = in.flag("crouching");
        boolean sprinting = in.flag("sprinting");
        boolean sitting = in.flag("sitting");
        float lie = in.channel("lie", 0f);
        float lieTail = in.channel("lie_tail", 0f);
        float relax = in.channel("relax", 0f);
        if (!unit(lie) || !unit(lieTail) || !unit(relax)) return Optional.empty();

        State head = states.get("head"), body = states.get("body"), tail1 = states.get("tail1"), tail2 = states.get("tail2");
        State leftHind = states.get("left_hind_leg"), rightHind = states.get("right_hind_leg");
        State leftFront = states.get("left_front_leg"), rightFront = states.get("right_front_leg");

        if (crouching) {
            body.y += ageScale;
            head.y += 2f * ageScale;
            tail1.y += ageScale;
            tail2.y -= 4f * ageScale;
            tail2.z += 2f * ageScale;
            tail1.xRot = (float)Math.PI / 2f;
            tail2.xRot = (float)Math.PI / 2f;
        } else if (sprinting) {
            tail2.y = tail1.y;
            tail2.z += 2f * ageScale;
            tail1.xRot = (float)Math.PI / 2f;
            tail2.xRot = (float)Math.PI / 2f;
        }

        head.xRot = in.headPitch() * Mth.DEG_TO_RAD;
        head.yRot = in.headYaw() * Mth.DEG_TO_RAD;

        if (!sitting) {
            if (!baby) body.xRot = (float)Math.PI / 2f;
            float speed = in.walkAmount(), pos = in.walkPhase();
            if (sprinting) {
                leftHind.xRot = Mth.cos(pos * .6662f) * speed;
                rightHind.xRot = Mth.cos(pos * .6662f + .3f) * speed;
                leftFront.xRot = Mth.cos(pos * .6662f + (float)Math.PI + .3f) * speed;
                rightFront.xRot = Mth.cos(pos * .6662f + (float)Math.PI) * speed;
                tail2.xRot = 1.7278761f + (float)Math.PI / 10f * Mth.cos(pos) * speed;
            } else {
                leftHind.xRot = Mth.cos(pos * .6662f) * speed;
                rightHind.xRot = Mth.cos(pos * .6662f + (float)Math.PI) * speed;
                leftFront.xRot = rightHind.xRot;
                rightFront.xRot = leftHind.xRot;
                tail2.xRot = 1.7278761f + (crouching ? .47123894f : (float)Math.PI / 4f) * Mth.cos(pos) * speed;
            }
        } else if (baby) {
            body.xRot -= .43633232f;
            body.y += 1f;
            head.z += .75f;
            tail1.xRot += .5454154f;
            tail1.y += 4f;
            tail1.z -= .9f;
            leftHind.z -= .9f;
            rightHind.z -= .9f;
        } else {
            body.xRot = (float)Math.PI / 4f;
            body.y -= 4f * ageScale;
            body.z += 5f * ageScale;
            head.y -= 3.3f * ageScale;
            head.z += ageScale;
            tail1.y += 8f * ageScale;
            tail1.z -= 2f * ageScale;
            tail2.y += 2f * ageScale;
            tail2.z -= .8f * ageScale;
            tail1.xRot = 1.7278761f;
            tail2.xRot = 2.670354f;
            leftFront.xRot = rightFront.xRot = -(float)Math.PI / 20f;
            leftFront.y += 2f * ageScale;
            leftFront.z -= 2f * ageScale;
            rightFront.y += 2f * ageScale;
            rightFront.z -= 2f * ageScale;
            leftHind.xRot = rightHind.xRot = -(float)Math.PI / 2f;
            leftHind.y += 3f * ageScale;
            leftHind.z -= 4f * ageScale;
            rightHind.y += 3f * ageScale;
            rightHind.z -= 4f * ageScale;
        }

        if (lie > 0f) {
            if (baby) applyBabyLie(body, head, tail1, leftHind, rightHind, leftFront, rightFront, lie, lieTail);
            else applyAdultLie(head, tail1, tail2, leftHind, rightHind, leftFront, rightFront, ageScale, lie, lieTail);
        }
        if (relax > 0f) head.xRot = Mth.rotLerp(relax, head.xRot, -.58177644f);

        var out = new LinkedHashMap<String, Matrix4f>();
        for (State state : states.values()) out.put(state.part.id(), state.matrix());
        return Optional.of(Collections.unmodifiableMap(out));
    }

    private static void applyAdultLie(State head, State tail1, State tail2, State leftHind, State rightHind,
                                      State leftFront, State rightFront, float ageScale, float lie, float lieTail) {
        head.zRot = Mth.rotLerp(lie, head.zRot, -1.2707963f);
        head.yRot = Mth.rotLerp(lie, head.yRot, 1.2707963f);
        leftFront.xRot = -1.2707963f;
        rightFront.xRot = -.47079635f;
        rightFront.zRot = -.2f;
        rightFront.x += ageScale;
        leftHind.xRot = -.4f;
        rightHind.xRot = .5f;
        rightHind.zRot = -.5f;
        rightHind.x += .8f * ageScale;
        rightHind.y += 2f * ageScale;
        tail1.xRot = Mth.rotLerp(lieTail, tail1.xRot, .8f);
        tail2.xRot = Mth.rotLerp(lieTail, tail2.xRot, -.4f);
    }

    private static void applyBabyLie(State body, State head, State tail1, State leftHind, State rightHind,
                                     State leftFront, State rightFront, float lie, float lieTail) {
        body.x += 1f;
        head.xRot = Mth.rotLerp(lie, head.xRot, (float)Math.PI / 18f);
        head.zRot = Mth.rotLerp(lie, head.zRot, -(float)Math.PI * 5f / 12f);
        head.x += 1f;
        head.y += .75f;
        head.z -= .5f;
        rightFront.xRot = -(float)Math.PI / 4f;
        rightFront.x += 3.5f;
        rightFront.y -= .5f;
        leftFront.xRot = -(float)Math.PI / 2f;
        leftFront.x += 1f;
        leftFront.y -= 1f;
        leftFront.z -= 2f;
        rightHind.xRot = (float)Math.PI * 2f / 9f;
        rightHind.yRot = (float)Math.PI / 9f;
        rightHind.zRot = -(float)Math.PI / 9f;
        rightHind.x += 2.5f;
        rightHind.y -= .25f;
        rightHind.z += .5f;
        leftHind.x += 1f;
        leftHind.z -= 1f;
        tail1.xRot += Mth.rotLerp(lieTail, tail1.xRot, -(float)Math.PI / 6f);
        tail1.yRot += Mth.rotLerp(lieTail, tail1.yRot, 0f);
        tail1.zRot += Mth.rotLerp(lieTail, tail1.zRot, -(float)Math.PI / 18f);
        tail1.x += 1f;
        tail1.y += .5f;
        tail1.z -= .25f;
    }

    private static boolean required(Inputs in) {
        for (String name : REQUIRED) if (!in.channels().containsKey(name)) return false;
        return true;
    }

    private static boolean unit(float value) { return Float.isFinite(value) && value >= 0f && value <= 1f; }

    private static boolean isFelinePart(String name) {
        return switch (name) {
            case "head", "body", "tail1", "tail2", "left_hind_leg", "right_hind_leg", "left_front_leg", "right_front_leg" -> true;
            default -> false;
        };
    }

    private static String shortName(ModelGeometry.Part part) {
        return part.id().substring(part.id().lastIndexOf('/') + 1);
    }

    private static final class State {
        final ModelGeometry.Part part;
        final float xScale, yScale, zScale;
        float x, y, z, xRot, yRot, zRot;

        State(ModelGeometry.Part part) {
            this.part = part;
            var source = part.sourcePose();
            this.x = source.x(); this.y = source.y(); this.z = source.z();
            this.xRot = source.xRot(); this.yRot = source.yRot(); this.zRot = source.zRot();
            this.xScale = source.xScale(); this.yScale = source.yScale(); this.zScale = source.zScale();
        }

        Matrix4f matrix() {
            return new Matrix4f().translation(x / 16f, y / 16f, z / 16f)
                .rotateZYX(zRot, yRot, xRot).scale(xScale, yScale, zScale);
        }
    }
}
