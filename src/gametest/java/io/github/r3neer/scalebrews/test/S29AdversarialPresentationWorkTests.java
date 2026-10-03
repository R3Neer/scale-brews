package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.collision.runtime.RootFrame;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** NFR-009: historical certified presentation must not churn the physical joint cache. */
public final class S29AdversarialPresentationWorkTests {
    @GameTest public void firstHistoricalPresentationReusesAlreadyEvaluatedPhysicalSamples(GameTestHelper h) {
        var model=new ModelGeometry(2,"test:s29_physical_reuse","1",List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("body","root",List.of(-.2d,0d,-.2d),List.of(.2d,.4d,.2d),null)),ModelGeometry.values(new Matrix4f()));
        var entity=h.makeMockPlayer(GameType.SURVIVAL);
        var provider=new ModelGeometryProvider(model,(geometry,inputs)->Optional.of(Map.of("root",new Matrix4f().rotateZ(inputs.walkAmount()))),AnatomyFilter.DEFAULT,1);
        var a=endpoint(1);var b=endpoint(2);var current=endpoint(3);
        try {
            provider.sampleAt(entity,a.sample(),a.rootTransform()).orElseThrow();
            provider.sampleAt(entity,b.sample(),b.rootTransform()).orElseThrow();
            provider.sampleAt(entity,current.sample(),current.rootTransform()).orElseThrow();
            long evaluated=provider.jointEvaluations();
            provider.evaluatePresentation(entity,a,b,.5).orElseThrow();
            provider.sampleAt(entity,current.sample(),current.rootTransform()).orElseThrow();
            long duplicate=provider.jointEvaluations()-evaluated;
            System.out.println("S29 FIRST HISTORICAL reuse physicalJointSamples duplicateEvaluations="+duplicate);
            h.assertTrue(duplicate==0,"NFR-009: first historical presentation recomputed already evaluated authority joints "+duplicate+" times");
        } finally {entity.discard();}
        h.succeed();
    }
    @GameTest public void currentAndHistoricalIntervalDoNotReevaluateSameJointSamples(GameTestHelper h) {
        var model=new ModelGeometry(2,"test:s29_pose_work","1",List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("body","root",List.of(-.2d,0d,-.2d),List.of(.2d,.4d,.2d),null)),ModelGeometry.values(new Matrix4f()));
        var entity=h.makeMockPlayer(GameType.SURVIVAL);
        var provider=new ModelGeometryProvider(model,(geometry,inputs)->Optional.of(Map.of("root",new Matrix4f().rotateZ(inputs.walkAmount()))),AnatomyFilter.DEFAULT,1);
        var a=endpoint(1);var b=endpoint(2);var current=endpoint(3);
        try {
            provider.evaluatePresentation(entity,a,b,.5).orElseThrow();provider.evaluatePresentation(entity,current).orElseThrow();
            long warm=provider.jointEvaluations();
            for(int i=0;i<32;i++) {
                provider.evaluatePresentation(entity,a,b,.5).orElseThrow();provider.evaluatePresentation(entity,current).orElseThrow();
            }
            long additional=provider.jointEvaluations()-warm;
            System.out.println("S29 HISTORICAL presentation repeatedSameEndpoints additionalJointEvaluations="+additional);
            h.assertTrue(additional==0,"NFR-009: alternating current endpoint and still-live certified interval re-evaluated identical joint samples "+additional+" times");
        } finally {entity.discard();}
        h.succeed();
    }
    private static GeometryProvider.CausalEndpoint endpoint(int index) {
        var inputs=new PoseEngine.Inputs(index*.2f,index*.1f,index,0,0,true);
        var sample=new AnatomyPoseHistory.Sample(inputs,new Vec3(index*.1,2,0),index*5,1);
        var root=new RootFrame(index,index,sample.origin(),sample.yaw(),sample.scale(),sample.gravity());
        return new GeometryProvider.CausalEndpoint(index,index,index,root,sample,GeometryProvider.Availability.AVAILABLE);
    }
}
