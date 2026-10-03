package io.github.r3neer.scalebrews.test;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.collision.preparation.ModelPartGeometryEngine;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalModelPartAdapter;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.HierarchyMotion;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Actual ModelPart signed-scale representability, independent of neutral determinant parity. */
public final class S30AdversarialSignedModelPoseProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(client->{
            var root=CowModel.createBodyLayer().bakeRoot();var model=new CowModel(root);
            var id=Identifier.parse("minecraft:cow");
            var geometry=ModelPartGeometryEngine.inspectVisible(id,root,new Matrix4f().scaling(-1,-1,1).translate(0,-1.501f,0));
            var prepared=CausalModelPartAdapter.prepare(model,id,geometry,AnatomyFilter.DEFAULT).orElseThrow();
            var head=root.getChild("head");var neutral=head.getInitialPose();
            for(int mask=0;mask<8;mask++) {
                float sx=(mask&1)==0?1.2f:-1.2f,sy=(mask&2)==0?.8f:-.8f,sz=(mask&4)==0?1.1f:-1.1f;
                var wanted=new Matrix4f().translation(neutral.x()/16,neutral.y()/16,neutral.z()/16).rotateZYX(.2f,-.7f,.45f).scale(sx,sy,sz);
                head.xRot=.45f;head.yRot=-.7f;head.zRot=.2f;head.xScale=sx;head.yScale=sy;head.zScale=sz;
                var direct=new PoseStack();head.translateAndRotate(direct);
                if(!direct.last().pose().equals(wanted,2e-4f))throw new AssertionError("Invalid signed-scale original model fixture");
                head.resetPose();
                var frame=HierarchyMotion.withRootTrs(geometry,Map.of("root/head",wanted),Map.of("root/head",wanted),
                    new Matrix4f(),new Matrix4f(),Vec3.ZERO,Vec3.ZERO,AnatomyFilter.DEFAULT).evaluate(1);
                var applied=prepared.apply(frame);
                if(applied.isEmpty()) {
                    System.out.println("S30 ADVERSARY signed-scale rejected mask="+mask+" originalModelPartOracle=PASS determinant="+wanted.determinant3x3()+" scale="+sx+","+sy+","+sz);
                    throw new AssertionError("Representable original ModelPart signed-scale pose rejected at mask "+mask);
                }
                try(var scope=applied.orElseThrow()) {
                    var actual=new PoseStack();head.translateAndRotate(actual);
                    if(!actual.last().pose().equals(wanted,2e-4f))throw new AssertionError("Signed-scale causal pose differs from original ModelPart");
                }
                if(head.xScale!=neutral.xScale() || head.yScale!=neutral.yScale() || head.zScale!=neutral.zScale())
                    throw new AssertionError("Signed-scale application failed restoration");
            }
            System.out.println("S30 ADVERSARY original ModelPart signed scales PASS all 8 determinant/sign combinations");
        });
    }
}
