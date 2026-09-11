package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.geometry.*;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import static io.github.r3neer.scalebrews.test.S00Fixtures.*;

/** A01/A03/A06/A08: malformed model and provider fault boundaries with observable recovery. */
public final class S00ModelTests {
    private static final AnatomyFilter ALL=new AnatomyFilter(0,0,0);
    private static PoseProvider.Inputs inputs(int tick){return new PoseProvider.Inputs(0,0,tick,0,0,true);}
    private static AnatomyPoseHistory.Sample sample(int tick,double x){return new AnatomyPoseHistory.Sample(inputs(tick),new Vec3(x,20,0),180,1,GravityFrame.VANILLA);}
    private static LivingEntity cow(GameTestHelper h){var cow=h.spawn(EntityTypes.COW,2,20,2);cow.setNoAi(true);cow.setNoGravity(true);return cow;}
    private static ModelGeometry.Part part(String id,String parent,Matrix4f transform){return new ModelGeometry.Part(id,parent,ModelGeometry.values(transform));}
    private static ModelGeometry.Piece piece(String id,String parent,String excluded){return new ModelGeometry.Piece(id,parent,List.of(0d,0d,0d),List.of(1d,1d,1d),excluded);}
    @GameTest public void catalogValidatesExportedRootBeforeAtomicSwap(GameTestHelper h) {
        var catalog=new GeometryCatalog();var prior=catalog.replace(Map.of("test:s00",model()),List.of("test:s00"));
        var bad=model().withModelTransform(new Matrix4f().translation(Float.MAX_VALUE,0,0));
        check(thrown(()->catalog.replace(Map.of("test:s00",bad),List.of("test:s00"))) instanceof RuntimeException,"Numerically invalid exported model root was published");
        check(catalog.snapshot()==prior,"Rejected model root replaced accepted snapshot");
        var next=catalog.replace(Map.of("test:s00",model()),List.of("test:s00"));check(next.revision()==prior.revision()+1,"Rejected candidate consumed catalog revision");h.succeed();
    }
    @GameTest public void hierarchyCyclesOrOrphansNeverReachEvaluation(GameTestHelper h) {
        var identity=new Matrix4f();
        for(var parts:List.of(List.of(part("root","root",identity)),List.of(part("a","b",identity),part("b","a",identity)),
                List.of(part("root",null,identity),part("root",null,identity)),List.of(part("root","missing",identity))))
            rejects(()->new ModelGeometry(2,"test:bad","1",parts,List.of(piece("piece","root",null)),ModelGeometry.values(identity)));
        rejects(()->new ModelGeometry(2,"test:bad","1",List.of(part("root",null,identity)),List.of(piece("piece","missing",null)),ModelGeometry.values(identity)));h.succeed();
    }
    @GameTest public void hierarchyAndPieceCapsHaveExplicitBoundaries(GameTestHelper h) {
        var parts=new ArrayList<ModelGeometry.Part>();
        for(int i=0;i<512;i++)parts.add(part("p"+i,i==0?null:"p"+(i-1),new Matrix4f().translation(.001f,0,0)));
        var valid=new ModelGeometry(2,"test:depth","1",parts,List.of(piece("piece","p511",null)),ModelGeometry.values(new Matrix4f()));
        check(Math.abs(valid.evaluate(new Matrix4f(),Map.of(),ALL).get("piece").bounds().minX-.512)<1e-5,"Deep hierarchy lost an ancestor");
        parts.add(part("p512","p511",new Matrix4f()));
        rejects(()->new ModelGeometry(2,"test:depth","1",parts,List.of(piece("piece","p511",null)),ModelGeometry.values(new Matrix4f())));
        var pieces=new ArrayList<ModelGeometry.Piece>();for(int i=0;i<4096;i++)pieces.add(piece("piece"+i,"root",null));
        new ModelGeometry(2,"test:count","1",model().parts(),pieces,model().modelTransform());pieces.add(piece("overflow","root",null));
        rejects(()->new ModelGeometry(2,"test:count","1",model().parts(),pieces,model().modelTransform()));h.succeed();
    }
    @GameTest public void degeneratePiecesCannotEnterTheModel(GameTestHelper h) {
        var parts=List.of(part("root",null,new Matrix4f()));
        rejects(()->new ModelGeometry(2,"test:flat","1",parts,List.of(new ModelGeometry.Piece("flat","root",List.of(0d,0d,0d),List.of(0d,1d,1d),null)),ModelGeometry.values(new Matrix4f())));
        rejects(()->new ModelGeometry(2,"test:underflow","1",parts,List.of(new ModelGeometry.Piece("tiny","root",List.of(0d,0d,0d),List.of(Double.MIN_VALUE,Double.MIN_VALUE,Double.MIN_VALUE),null)),ModelGeometry.values(new Matrix4f())));
        h.succeed();
    }
    @GameTest public void excludedParentDoesNotErasePhysicalChild(GameTestHelper h) {
        var geometry=new ModelGeometry(2,"test:filter","1",List.of(part("root",null,new Matrix4f()),part("child","root",new Matrix4f().translation(2,0,0))),
            List.of(piece("decoration","root","cosmetic"),piece("physical","child",null)),ModelGeometry.values(new Matrix4f()));
        var filter=new AnatomyFilter(0,0,0,Set.of("decoration"),Set.of("root"));
        var report=geometry.filterReport(filter);check(report.get("decoration").equals("cosmetic"),"Include override resurrected excluded decoration");
        var evaluated=geometry.evaluate(new Matrix4f(),Map.of(),filter);check(evaluated.keySet().equals(Set.of("physical")),"Excluded parent erased valid descendant geometry");
        check(evaluated.get("physical").bounds().minX==2,"Descendant lost excluded parent's transform");h.succeed();
    }
    @GameTest public void unknownJointKeysCannotSilentlyBecomeRestPose(GameTestHelper h) {
        rejects(()->model().transforms(Map.of("misspelled-root",new Matrix4f().translation(10,0,0))));h.succeed();
    }
    @GameTest public void hierarchyMotionUnknownJointCannotSilentlyBecomeRestPose(GameTestHelper h) {
        var m=model();var identity=new Matrix4f();
        rejects(()->new HierarchyMotion(m,Map.of("typo",new Matrix4f().translation(2,0,0)),Map.of(),identity,identity,Vec3.ZERO,Vec3.ZERO,ALL));
        rejects(()->new HierarchyMotion(m,Map.of(),Map.of("typo",new Matrix4f().translation(2,0,0)),identity,identity,Vec3.ZERO,Vec3.ZERO,ALL));
        h.succeed();
    }
    @GameTest public void malformedReplacementTransformsAreRejectedBeforeUse(GameTestHelper h) {
        for(var matrix:List.of(new Matrix4f().m00(Float.NaN),new Matrix4f().m03(.1f),new Matrix4f().scale(0)))
            rejects(()->model().transforms(Map.of("root",matrix)));
        var excludeAll=new AnatomyFilter(0,0,0,Set.of(),Set.of("piece"));
        rejects(()->model().evaluate(new Matrix4f().m00(Float.NaN),Map.of(),excludeAll));h.succeed();
    }
    @GameTest public void providerExceptionsFailClosedAndRecoverWithoutStaleGeometry(GameTestHelper h) {
        var entity=cow(h);var calls=new AtomicInteger();
        var provider=new ModelGeometryProvider(model(),(m,i)->{calls.incrementAndGet();if(i.age()==1)throw new IllegalArgumentException("unsupported pose primitive");return Optional.of(Map.of());},ALL,1);
        try {
            check(provider.sampleAt(entity,sample(0,0)).isPresent(),"Valid initial pose missing");
            for(int i=0;i<32;i++)check(provider.sampleAt(entity,sample(1,i*.01)).isEmpty(),"Invalid pose froze or invented geometry");
            check(calls.get()==2,"Rejected identical joint input was reevaluated per root query");
            check(provider.sampleAt(entity,sample(2,1)).isPresent(),"Valid pose did not recover after localized rejection");
        } finally {entity.discard();}h.succeed();
    }
    @GameTest public void providerNullResultIsUnavailableNotWorldFailure(GameTestHelper h) {
        var entity=cow(h);var provider=new ModelGeometryProvider(model(),(m,i)->null,ALL,1);
        try {check(provider.sampleAt(entity,sample(0,0)).isEmpty(),"Null pose published geometry");}finally{entity.discard();}h.succeed();
    }
    @GameTest public void providerUnknownJointDoesNotInventRestGeometry(GameTestHelper h) {
        var entity=cow(h);var provider=new ModelGeometryProvider(model(),(m,i)->Optional.of(Map.of("typo",new Matrix4f())),ALL,1);
        try {check(provider.sampleAt(entity,sample(0,0)).isEmpty(),"Unknown joint silently published a rest collider");}finally{entity.discard();}h.succeed();
    }
    @GameTest public void invalidProviderMatrixIsLocalizedUnavailable(GameTestHelper h) {
        var entity=cow(h);
        try {
            for(var matrix:List.of(new Matrix4f().m00(Float.NaN),new Matrix4f().m03(.1f),new Matrix4f().scale(0))) {
                var provider=new ModelGeometryProvider(model(),(m,i)->Optional.of(Map.of("root",matrix)),ALL,1);
                check(provider.sampleAt(entity,sample(0,0)).isEmpty(),"Invalid provider matrix became material");
            }
            var healthy=new ModelGeometryProvider(model(),(m,i)->Optional.of(Map.of()),ALL,1);
            check(healthy.sampleAt(entity,sample(0,0)).isPresent(),"Invalid provider disabled a healthy binding");
        }finally{entity.discard();}h.succeed();
    }
    @GameTest public void unsupportedAnimatedShearDoesNotInvalidateValidEndpoints(GameTestHelper h) {
        var entity=cow(h);var provider=new ModelGeometryProvider(model(),(m,i)->Optional.of(Map.of("root",new Matrix4f().m10(i.age()*.2f))),ALL,1);
        try {
            check(provider.sampleAt(entity,sample(0,0)).isPresent() && provider.sampleAt(entity,sample(1,0)).isPresent(),"Static affine endpoints are valid");
            check(provider.motionBetween(entity,sample(0,0),sample(1,0)).isEmpty(),"Unsupported animated shear received an invented trajectory");
            check(provider.sampleAt(entity,sample(1,0)).isPresent(),"Unsupported interval destroyed a valid current endpoint");
        }finally{entity.discard();}h.succeed();
    }
    @GameTest public void providerCopiesMutableMatricesAndBoundsJointCache(GameTestHelper h) {
        var entity=cow(h);var matrix=new Matrix4f();var provider=new ModelGeometryProvider(model(),(m,i)->Optional.of(Map.of("root",matrix)),ALL,1);
        try {
            var original=provider.sampleAt(entity,sample(0,0)).orElseThrow();matrix.translation(100,0,0);
            for(int i=0;i<32;i++) {
                var moved=provider.sampleAt(entity,sample(0,i*.1)).orElseThrow();
                near(moved.pieces().get("piece").vertices().getFirst(),original.pieces().get("piece").vertices().getFirst().add(i*.1,0,0),1e-8,"Mutable pose escaped its authority capture");
            }
            check(provider.jointEvaluations()==1,"Root changes reevaluated unchanged joints");
            for(int tick=1;tick<100;tick++)provider.sampleAt(entity,sample(tick,0));
            check(provider.cachedJointEndpoints(entity)==2,"Joint history grew beyond two endpoints");
        }finally{entity.discard();}h.succeed();
    }
    @GameTest public void providerCannotRewindAnAuthorityFrame(GameTestHelper h) {
        var entity=cow(h);var provider=new ModelGeometryProvider(model(),(m,i)->Optional.of(Map.of()),ALL,1);
        try {
            provider.pose(entity,inputs(10));provider.tick(entity,10);var prior=provider.authoritativeFrame(entity).orElseThrow();
            provider.pose(entity,inputs(9));try{provider.tick(entity,9);}catch(IllegalArgumentException rejected){}
            check(provider.authoritativeFrame(entity).orElseThrow().equals(prior),"Provider rewound an already published authority frame");
        }finally{entity.discard();}h.succeed();
    }
    @GameTest public void invalidProviderAndTrackerClocksDoNotCreateState(GameTestHelper h) {
        var entity=cow(h);var provider=new ModelGeometryProvider(model(),(m,i)->Optional.of(Map.of()),ALL,1);var tracker=new AuthorityPoseTracker();
        try {
            provider.pose(entity,inputs(0));rejects(()->provider.tick(entity,-1));
            check(provider.authoritativeFrame(entity).isEmpty(),"Invalid tick left authority state");
            rejects(()->tracker.tick(entity,-1,true));check(tracker.current(entity).isEmpty(),"Invalid tracker clock created state");
            rejects(()->new ModelGeometryProvider(model(),(m,i)->Optional.of(Map.of()),ALL,-1));
        }finally{entity.discard();}h.succeed();
    }
}
