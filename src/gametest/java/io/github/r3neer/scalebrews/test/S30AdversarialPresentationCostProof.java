package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.ModelPartGeometryEngine;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalModelPartAdapter;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.HierarchyMotion;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Isolated measured cost of nonzero original-model pose consumers; excludes geometry evaluation/GPU. */
public final class S30AdversarialPresentationCostProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(client->{
            var id=Identifier.parse("minecraft:cow");
            var geometry=ModelPartGeometryEngine.inspectVisible(id,CowModel.createBodyLayer().bakeRoot(),new Matrix4f().scaling(-1,-1,1).translate(0,-1.501f,0));
            var adapters=new CausalModelPartAdapter.Prepared[64];
            for(int i=0;i<adapters.length;i++)adapters[i]=CausalModelPartAdapter.prepare(new CowModel(CowModel.createBodyLayer().bakeRoot()),id,geometry,AnatomyFilter.DEFAULT).orElseThrow();
            var initial=geometry.parts().stream().filter(p->p.id().equals("root/head")).findFirst().orElseThrow();
            var motion=HierarchyMotion.withRootTrs(geometry,Map.of("root/head",ModelGeometry.matrix(initial.transform())),
                Map.of("root/head",ModelGeometry.matrix(initial.transform()).rotateY(.71f).scale(1.13f,.89f,1.07f)),
                new Matrix4f(),new Matrix4f().translate(.3f,.2f,-.1f),Vec3.ZERO,Vec3.ZERO,AnatomyFilter.DEFAULT);
            var frames=new HierarchyMotion.EvaluatedFrame[32];
            for(int i=0;i<frames.length;i++)frames[i]=motion.evaluate((i+1)/33.0);
            var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
            bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().threadId();
            for(int consumers:new int[]{1,16}) {
                for(int warm=0;warm<120;warm++)batch(adapters,frames[warm%32],consumers);
                int samples=160;var times=new long[samples];long allocated=0;
                for(int i=0;i<samples;i++) {
                    long bytes=bean.getThreadAllocatedBytes(thread),start=System.nanoTime();
                    batch(adapters,frames[i%32],consumers);
                    times[i]=System.nanoTime()-start;allocated+=bean.getThreadAllocatedBytes(thread)-bytes;
                }
                Arrays.sort(times);
                System.out.println("S30 ADVERSARY pose cost supports=64 consumers="+consumers+" parts="+geometry.parts().size()
                    +" bytesPerBatch="+(allocated/samples)+" p95Ms="+(times[(int)(samples*.95)]/1e6)+" p99Ms="+(times[(int)(samples*.99)]/1e6));
            }
        });
    }
    private static void batch(CausalModelPartAdapter.Prepared[] adapters,HierarchyMotion.EvaluatedFrame frame,int consumers) {
        for(var adapter:adapters)for(int consumer=0;consumer<consumers;consumer++) {
            if(!adapter.representable(frame))throw new AssertionError("Nonzero cost fixture rejected");
            try(var immediate=adapter.apply(frame).orElseThrow()) {}
            try(var delayed=adapter.apply(frame).orElseThrow()) {}
        }
    }
}
