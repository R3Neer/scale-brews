package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.ModelPartGeometryEngine;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalModelPartAdapter;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.HierarchyMotion;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Tests a real original-model family with changed material and reversible joint poses. */
public final class S30CausalModelAdapterTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(client->{
            var modelId=Identifier.parse("minecraft:cow");
            var material=ModelPartGeometryEngine.inspectVisible(modelId,CowModel.createBodyLayer().bakeRoot(),
                new Matrix4f().scaling(-1,-1,1).translate(0,-1.501f,0));
            var visibleRoot=CowModel.createBodyLayer().bakeRoot();
            var visible=new CowModel(visibleRoot);
            var head=visibleRoot.getChild("head");
            head.xRot=.23f;head.yScale=1.15f;head.visible=false;head.skipDraw=true;
            var saved=head.storePose();
            var prepared=CausalModelPartAdapter.prepare(visible,modelId,material,AnatomyFilter.DEFAULT).orElseThrow();
            if(!head.storePose().equals(saved) || head.yScale!=1.15f || head.visible || !head.skipDraw)
                throw new AssertionError("Neutral preparation leaked into the shared renderer model");
            head.visible=true;head.skipDraw=false;
            var headRest=material.parts().stream().filter(p->p.id().equals("root/head")).findFirst().orElseThrow();
            var animated=new Matrix4f(ModelGeometry.matrix(headRest.transform())).rotateY(.37f).scale(1.2f,.8f,1.1f);
            var motion=HierarchyMotion.withRootTrs(material,Map.of(),Map.of("root/head",animated),
                new Matrix4f(),new Matrix4f().translate(2,0,0),Vec3.ZERO,Vec3.ZERO,AnatomyFilter.DEFAULT);
            var frame=motion.evaluate(.5);
            var before=head.storePose();float beforeScale=head.yScale;
            try(var applied=prepared.apply(frame).orElseThrow()) {
                var actual=new Matrix4f().translationRotateScale(
                    new org.joml.Vector3f(head.x/16,head.y/16,head.z/16),
                    new org.joml.Quaternionf().rotationZYX(head.zRot,head.yRot,head.xRot),
                    new org.joml.Vector3f(head.xScale,head.yScale,head.zScale));
                if(!actual.equals(frame.localPartTransforms().get("root/head").matrix(),2e-4f))
                    throw new AssertionError("The real Cow head did not consume the evaluated joint matrix");
            }
            if(!head.storePose().equals(before) || head.yScale!=beforeScale || !head.visible || head.skipDraw)
                throw new AssertionError("Original ModelPart pose was not restored after draw");
            try {
                var alteredRoot=CowModel.createBodyLayer().bakeRoot();
                Field field=ModelPart.class.getDeclaredField("cubes");field.setAccessible(true);
                var body=alteredRoot.getChild("body");
                @SuppressWarnings("unchecked") var cubes=new ArrayList<ModelPart.Cube>((java.util.List<ModelPart.Cube>)field.get(body));
                var cube=cubes.getFirst();
                cubes.set(0,new ModelPart.Cube(0,0,cube.minX,cube.minY,cube.minZ,
                    cube.maxX-cube.minX+16,cube.maxY-cube.minY,cube.maxZ-cube.minZ,
                    0,0,0,false,64,64,EnumSet.allOf(Direction.class)));
                field.set(body,java.util.List.copyOf(cubes));
                if(CausalModelPartAdapter.prepare(new CowModel(alteredRoot),modelId,material,AnatomyFilter.DEFAULT).isPresent())
                    throw new AssertionError("Altered retained body geometry was incorrectly accepted");
            } catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
        });
    }
}
