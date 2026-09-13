package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.PoseProgram;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** S18 execution/lifecycle holdouts for revision-bound server-safe keyframe programs. */
public final class S18PoseProgramExecutionTests {
    private static final Identifier ENGINE = Identifier.parse("scalebrews:mojang_keyframes");
    private static final Identifier PROGRAM = Identifier.parse("scalebrews_test:s18_wave");

    @GameTest
    public void revisionLocalProgramBindingIsImmutableAndFailClosed(GameTestHelper h) {
        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        var geometry = geometry();
        var first = program(16);
        var second = program(32);
        PoseEngine.Resources firstResources = id -> id.equals(PROGRAM) ? Optional.of(first) : Optional.empty();
        PoseEngine.Resources secondResources = id -> id.equals(PROGRAM) ? Optional.of(second) : Optional.empty();
        var parameters = Map.of("program", PROGRAM.toString(), "clock", "channel:time", "clock_scale", "1", "amplitude", "channel:weight");

        var boundFirst = engine.bind(geometry, parameters, Set.of("gate"), firstResources).orElseThrow();
        var boundSecond = engine.bind(geometry, parameters, Set.of("gate"), secondResources).orElseThrow();
        var inputs = new PoseEngine.Inputs(0, 0, 0, 0, 0, true, Map.of("time", .5f, "weight", 1f, "gate", 1f));
        float firstX = translationX(boundFirst.evaluate(inputs).orElseThrow().get("root"));
        float secondX = translationX(boundSecond.evaluate(inputs).orElseThrow().get("root"));
        float firstAgain = translationX(boundFirst.evaluate(inputs).orElseThrow().get("root"));

        h.assertTrue(Math.abs(firstX - .5f) < 1e-6 && Math.abs(secondX - 1f) < 1e-6,
            "Two accepted revisions may bind the same program id to different immutable program content");
        h.assertTrue(Math.abs(firstAgain - firstX) < 1e-6,
            "A previously bound evaluator must not observe a later revision through a global program lookup");
        h.assertTrue(boundFirst.evaluate(new PoseEngine.Inputs(0,0,0,0,0,true,Map.of("time",.5f,"weight",1f))).isEmpty(),
            "Missing declared binding channels must make the endpoint unavailable, not substitute zero/default state");
        h.assertTrue(engine.bind(geometry, parameters, Set.of(), id -> Optional.empty()).isEmpty(),
            "Missing revision-local program must fail binding closed");
        h.assertTrue(engine.bind(geometry, Map.of("program",PROGRAM.toString(),"clock","static","amplitude","one","invented","x"), Set.of(), firstResources).isEmpty(),
            "Unknown keyframe-engine parameters must not create an accidental procedural language");
        h.succeed();
    }

    @GameTest
    public void unknownAndAmbiguousBonesFailBindingClosed(GameTestHelper h) {
        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        PoseEngine.Resources missingBoneResources = id -> id.equals(PROGRAM) ? Optional.of(programForBone("missing", 16)) : Optional.empty();
        var params = Map.of("program",PROGRAM.toString(),"clock","static","amplitude","one");
        h.assertTrue(engine.bind(geometry(), params, Set.of(), missingBoneResources).isEmpty(),
            "Unknown program bone must fail revision binding instead of silently dropping a track");

        var ambiguous = new ModelGeometry(1,"proof:ambiguous","26.2",
            List.of(
                new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f())),
                new ModelGeometry.Part("root/left", "root", ModelGeometry.values(new Matrix4f())),
                new ModelGeometry.Part("root/left/head", "root/left", ModelGeometry.values(new Matrix4f())),
                new ModelGeometry.Part("root/right", "root", ModelGeometry.values(new Matrix4f())),
                new ModelGeometry.Part("root/right/head", "root/right", ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("piece","root",List.of(0d,0d,0d),List.of(1d,1d,1d),null)));
        PoseEngine.Resources ambiguousResources = id -> id.equals(PROGRAM) ? Optional.of(programForBone("head",16)) : Optional.empty();
        h.assertTrue(engine.bind(ambiguous, params, Set.of(), ambiguousResources).isEmpty(),
            "Ambiguous basename resolution must fail closed instead of binding by incidental hierarchy order");
        h.succeed();
    }

    private static ModelGeometry geometry() {
        return new ModelGeometry(1,"proof:s18","26.2",
            List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("piece","root",List.of(0d,0d,0d),List.of(1d,1d,1d),null)));
    }

    private static PoseProgram program(float endPixels) { return programForBone("root", endPixels); }
    private static PoseProgram programForBone(String bone,float endPixels) {
        return new PoseProgram(PoseProgram.SCHEMA_VERSION,"proof:program","26.2",1f,false,List.of(
            new PoseProgram.Track(bone,PoseProgram.Target.TRANSLATION,List.of(
                new PoseProgram.Keyframe(0,new PoseProgram.Vector(0,0,0),new PoseProgram.Vector(0,0,0),PoseProgram.Interpolation.LINEAR),
                new PoseProgram.Keyframe(1,new PoseProgram.Vector(endPixels,0,0),new PoseProgram.Vector(endPixels,0,0),PoseProgram.Interpolation.LINEAR)))));
    }

    private static float translationX(Matrix4f matrix) {
        return matrix.getTranslation(new Vector3f()).x;
    }
}
