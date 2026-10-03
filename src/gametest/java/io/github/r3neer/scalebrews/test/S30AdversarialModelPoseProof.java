package io.github.r3neer.scalebrews.test;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.collision.preparation.ModelPartGeometryEngine;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalModelPartAdapter;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.HierarchyMotion;
import java.util.Map;
import java.util.Random;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Original-model matrix and nested/exceptional shared-state restoration holdout. */
public final class S30AdversarialModelPoseProof implements FabricClientGameTest {
    private record Fields(float x,float y,float z,float rx,float ry,float rz,float sx,float sy,float sz,boolean visible,boolean skip) {
        static Fields read(ModelPart p){return new Fields(p.x,p.y,p.z,p.xRot,p.yRot,p.zRot,p.xScale,p.yScale,p.zScale,p.visible,p.skipDraw);}
        void restore(ModelPart p){p.x=x;p.y=y;p.z=z;p.xRot=rx;p.yRot=ry;p.zRot=rz;p.xScale=sx;p.yScale=sy;p.zScale=sz;p.visible=visible;p.skipDraw=skip;}
    }
    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var root = CowModel.createBodyLayer().bakeRoot();
            var model = new CowModel(root);
            var id = Identifier.parse("minecraft:cow");
            var geometry = ModelPartGeometryEngine.inspectVisible(id, root, new Matrix4f().scaling(-1,-1,1).translate(0,-1.501f,0));
            var head = root.getChild("head");
            head.xScale=1.13f;head.yScale=.91f;head.zScale=1.17f;head.xRot=.29f;
            var baseline = root.getAllParts().stream().map(Fields::read).toList();
            var prepared = CausalModelPartAdapter.prepare(model,id,geometry,AnatomyFilter.DEFAULT).orElseThrow();
            if(!baseline.equals(root.getAllParts().stream().map(Fields::read).toList()))
                throw new AssertionError("Preparation changed shared fields");
            var random = new Random(0x530CA05AL);
            for(int i=0;i<512;i++) {
                var position=new Vector3f(random.nextFloat()*2-1,random.nextFloat()*2-1,random.nextFloat()*2-1);
                float zAngle=random.nextFloat()*6-3,yAngle=random.nextFloat()*6-3,xAngle=random.nextFloat()*6-3;
                var scale=new Vector3f(.2f+random.nextFloat()*2,.2f+random.nextFloat()*2,.2f+random.nextFloat()*2);
                var wanted = new Matrix4f().translationRotateScale(position,new Quaternionf().rotationZYX(zAngle,yAngle,xAngle),scale);
                // Independent original-ModelPart oracle proves this matrix is drawable before asking the adapter.
                var prior=Fields.read(head);
                head.x=position.x*16;head.y=position.y*16;head.z=position.z*16;
                head.xRot=xAngle;head.yRot=yAngle;head.zRot=zAngle;
                head.xScale=scale.x;head.yScale=scale.y;head.zScale=scale.z;
                requireMatrix(head,wanted,i);
                prior.restore(head);
                var frame=frame(geometry,wanted);
                var accepted=prepared.apply(frame);
                if(accepted.isEmpty()) {
                    System.out.println("S30 rejected iteration="+i+" originalModelPartOracle=PASS position="+position+" xyzAngles="+xAngle+","+yAngle+","+zAngle+" scale="+scale+" wanted="+wanted+" evaluated="+frame.localPartTransforms().get("root/head").matrix());
                    try {
                        var method=CausalModelPartAdapter.class.getDeclaredMethod("pose",Matrix4f.class,net.minecraft.client.model.geom.PartPose.class);
                        method.setAccessible(true);
                        for(var part:geometry.parts()) {
                            var visiblePart=part.parent()==null?root:root.getChild(part.id().substring(part.id().lastIndexOf('/')+1));
                            if(((java.util.Optional<?>)method.invoke(null,frame.localPartTransforms().get(part.id()).matrix(),visiblePart.getInitialPose())).isEmpty())
                                System.out.println("S30 rejected local part="+part.id()+" matrix="+frame.localPartTransforms().get(part.id()).matrix());
                        }
                    } catch(ReflectiveOperationException diagnosticFailure){throw new AssertionError(diagnosticFailure);}
                    throw new AssertionError("Representable TRS was rejected at iteration "+i);
                }
                try(var outer=accepted.orElseThrow()) {
                    requireMatrix(head,wanted,i);
                    var outerFields=root.getAllParts().stream().map(Fields::read).toList();
                    try(var inner=prepared.apply(frame(geometry,new Matrix4f().translate(.4f,.2f,-.6f))).orElseThrow()) {
                        requireMatrix(head,new Matrix4f().translate(.4f,.2f,-.6f),i);
                    }
                    if(!outerFields.equals(root.getAllParts().stream().map(Fields::read).toList()))
                        throw new AssertionError("Nested application failed to restore outer pose");
                    try(var exceptional=prepared.apply(frame).orElseThrow()) {throw new DeliberateExit();}
                    catch(DeliberateExit expected) {}
                    if(!outerFields.equals(root.getAllParts().stream().map(Fields::read).toList()))
                        throw new AssertionError("Exceptional application failed to restore outer pose");
                }
                if(!baseline.equals(root.getAllParts().stream().map(Fields::read).toList()))
                    throw new AssertionError("Application leaked shared fields at iteration "+i);
            }
            var sheared=new Matrix4f().m10(.35f);
            if(prepared.apply(frame(geometry,sheared)).isPresent())throw new AssertionError("Unrepresentable shear was accepted");
            if(!baseline.equals(root.getAllParts().stream().map(Fields::read).toList()))
                throw new AssertionError("Rejected shear changed shared fields");
            System.out.println("S30 ADVERSARY model pose: 512 TRS, nested scopes, exception restoration and rejected shear PASS");
        });
    }
    private static HierarchyMotion.EvaluatedFrame frame(ModelGeometry geometry,Matrix4f pose) {
        return HierarchyMotion.withRootTrs(geometry,Map.of("root/head",pose),Map.of("root/head",pose),
            new Matrix4f(),new Matrix4f(),Vec3.ZERO,Vec3.ZERO,AnatomyFilter.DEFAULT).evaluate(1);
    }
    private static void requireMatrix(ModelPart part,Matrix4f expected,int iteration) {
        var pose=new PoseStack();part.translateAndRotate(pose);
        if(!pose.last().pose().equals(expected,2e-4f))throw new AssertionError("Original model matrix differs at iteration "+iteration);
    }
    private static final class DeliberateExit extends RuntimeException {}
}
