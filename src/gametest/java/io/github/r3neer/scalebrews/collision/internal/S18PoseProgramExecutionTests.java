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
    public void missingSourcePoseFailsKeyframeBindingClosed(GameTestHelper h) {
        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        var legacyGeometry = new ModelGeometry(1,"proof:s18-no-source-pose","26.2",
            List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("piece","root",List.of(0d,0d,0d),List.of(1d,1d,1d),null)));
        PoseEngine.Resources resources = id -> id.equals(PROGRAM) ? Optional.of(program(16)) : Optional.empty();
        var parameters = Map.of("program",PROGRAM.toString(),"clock","static","amplitude","one");
        h.assertTrue(engine.bind(legacyGeometry, parameters, Set.of(), resources).isEmpty(),
            "Mojang keyframes must fail closed when exact source pose metadata is unavailable");
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

    @GameTest
    public void validRuntimeAmplitudeMayExceedStoredVectorBound(GameTestHelper h) {
        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        PoseEngine.Resources resources = id -> id.equals(PROGRAM) ? Optional.of(program(40_000)) : Optional.empty();
        var parameters = Map.of("program", PROGRAM.toString(), "clock", "channel:time", "clock_scale", "1", "amplitude", "constant:2");
        var bound = engine.bind(geometry(), parameters, Set.of(), resources).orElseThrow();
        var inputs = new PoseEngine.Inputs(0, 0, 0, 0, 0, true, Map.of("time", 1f));

        var result = bound.evaluate(inputs);
        h.assertTrue(result.isPresent(),
            "A valid runtime amplitude must not be revalidated as stored PoseProgram data merely because the sampled vector exceeds the serialized vector bound");
        float x = translationX(result.orElseThrow().get("root"));
        h.assertTrue(Math.abs(x - 5_000f) < 1e-3f,
            "Mojang amplitude is applied after sampling: 40000 ModelPart pixels * 2 must remain a finite 5000-block local translation");
        h.succeed();
    }

    @GameTest
    public void validRuntimeTrackAccumulationMayExceedStoredVectorBound(GameTestHelper h) {
        var zero = new PoseProgram.Vector(0,0,0);
        var fortyThousand = new PoseProgram.Vector(40_000,0,0);
        var track = new PoseProgram.Track("root", PoseProgram.Target.TRANSLATION, List.of(
            new PoseProgram.Keyframe(0,zero,zero,PoseProgram.Interpolation.LINEAR),
            new PoseProgram.Keyframe(1,fortyThousand,fortyThousand,PoseProgram.Interpolation.LINEAR)));
        var accumulated = new PoseProgram(PoseProgram.SCHEMA_VERSION,"proof:runtime-accumulation","26.2",1f,false,
            List.of(track, track));

        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        PoseEngine.Resources resources = id -> id.equals(PROGRAM) ? Optional.of(accumulated) : Optional.empty();
        var parameters = Map.of("program",PROGRAM.toString(),"clock","channel:time","clock_scale","1","amplitude","one");
        var bound = engine.bind(geometry(), parameters, Set.of(), resources).orElseThrow();
        var inputs = new PoseEngine.Inputs(0,0,0,0,0,true,Map.of("time",1f));

        var result = bound.evaluate(inputs);
        h.assertTrue(result.isPresent(),
            "Two individually valid tracks may accumulate beyond the serialized per-vector bound; runtime accumulation must not reconstruct the bounded wire DTO");
        float x = translationX(result.orElseThrow().get("root"));
        h.assertTrue(Math.abs(x - 5_000f) < 1e-3f,
            "Mojang applies both POSITION channels additively: 40000 + 40000 pixels must produce a finite 5000-block local translation");
        h.succeed();
    }

    @GameTest
    public void explicitClockAndAmplitudeSelectorsHaveIndependentOracles(GameTestHelper h) {
        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        var geometry = geometry();
        var selectorProgram = twoSecondProgram(32);
        PoseEngine.Resources resources = id -> id.equals(PROGRAM) ? Optional.of(selectorProgram) : Optional.empty();
        var inputs = new PoseEngine.Inputs(2f, .25f, 20f, 0, 0, true, Map.of("time", .5f));

        var staticBound = engine.bind(geometry,
            Map.of("program",PROGRAM.toString(),"clock","static","amplitude","one"), Set.of(), resources).orElseThrow();
        var ageBound = engine.bind(geometry,
            Map.of("program",PROGRAM.toString(),"clock","age","clock_scale","1","amplitude","one"), Set.of(), resources).orElseThrow();
        var walkBound = engine.bind(geometry,
            Map.of("program",PROGRAM.toString(),"clock","walk_phase","clock_scale","0.5","amplitude","one"), Set.of(), resources).orElseThrow();
        var channelBound = engine.bind(geometry,
            Map.of("program",PROGRAM.toString(),"clock","channel:time","clock_scale","2","amplitude","one"), Set.of(), resources).orElseThrow();
        var walkAmplitudeBound = engine.bind(geometry,
            Map.of("program",PROGRAM.toString(),"clock","channel:time","clock_scale","2","amplitude","walk_amount"), Set.of(), resources).orElseThrow();

        h.assertTrue(Math.abs(translationX(staticBound.evaluate(inputs).orElseThrow().get("root"))) < 1e-6,
            "static clock must evaluate the program at t=0 regardless of live age/walk/channel inputs");
        h.assertTrue(Math.abs(translationX(ageBound.evaluate(inputs).orElseThrow().get("root")) - 1f) < 1e-6,
            "age clock must convert 20 authoritative ticks to 1 program second when clock_scale=1");
        h.assertTrue(Math.abs(translationX(walkBound.evaluate(inputs).orElseThrow().get("root")) - 1f) < 1e-6,
            "walk_phase clock must apply its explicit clock_scale exactly once: 2 * 0.5 = 1 program second");
        h.assertTrue(Math.abs(translationX(channelBound.evaluate(inputs).orElseThrow().get("root")) - 1f) < 1e-6,
            "channel clock must apply its explicit clock_scale exactly once: 0.5 * 2 = 1 program second");
        h.assertTrue(Math.abs(translationX(walkAmplitudeBound.evaluate(inputs).orElseThrow().get("root")) - .25f) < 1e-6,
            "walk_amount amplitude must scale the sampled target after clock evaluation: 1 block * 0.25 = 0.25 blocks");
        h.succeed();
    }

    private static ModelGeometry geometry() {
        var sourcePose = new ModelGeometry.SourcePose(0,0,0,0,0,0,1,1,1);
        return new ModelGeometry(1,"proof:s18","26.2",
            List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(sourcePose.matrix()),sourcePose)),
            List.of(new ModelGeometry.Piece("piece","root",List.of(0d,0d,0d),List.of(1d,1d,1d),null)));
    }

    private static PoseProgram program(float endPixels) { return programForBone("root", endPixels); }
    private static PoseProgram twoSecondProgram(float endPixels) {
        return new PoseProgram(PoseProgram.SCHEMA_VERSION,"proof:selector-program","26.2",2f,false,List.of(
            new PoseProgram.Track("root",PoseProgram.Target.TRANSLATION,List.of(
                new PoseProgram.Keyframe(0,new PoseProgram.Vector(0,0,0),new PoseProgram.Vector(0,0,0),PoseProgram.Interpolation.LINEAR),
                new PoseProgram.Keyframe(2,new PoseProgram.Vector(endPixels,0,0),new PoseProgram.Vector(endPixels,0,0),PoseProgram.Interpolation.LINEAR)))));
    }
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
