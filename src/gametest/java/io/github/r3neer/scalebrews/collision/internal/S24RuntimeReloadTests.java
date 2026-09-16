package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import org.joml.Matrix4f;

/** G3.9 live runtime reload barriers, above the catalog-only S15/S16 proofs. */
public final class S24RuntimeReloadTests {
    @GameTest
    public void rejectedResourceReloadPreservesExactAcceptedRuntimeAuthority(GameTestHelper h) {
        var server=h.getLevel().getServer();
        var cow=h.spawn(EntityTypes.COW,2,20,2);cow.setNoAi(true);cow.setNoGravity(true);
        var modelId="scalebrews_test:s24_reload_cow";
        var model=new ModelGeometry(2,modelId,"26.2",
            List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("body","root",List.of(-.5d,0d,-.5d),List.of(.5d,1d,.5d),null)),
            ModelGeometry.values(new Matrix4f()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse(modelId),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));

        try {
            AnatomyRuntime.startPrepared(server,Map.of(modelId,model),Map.of("scalebrews_test:s24_reload",profile));
            h.assertTrue(AnatomyRuntime.owns(cow),"Prepared baseline must own the live cow before reload failure injection");
            var before=AnatomyBindingState.snapshot(cow);
            h.assertTrue(before!=null && before.causal(),"Prepared baseline must install one causal provider/binding slot");
            var beforeFrame=AnatomyMovement.publishedFrame(cow).orElseThrow();
            long beforeRevision=AnatomyNetworking.revision(server);

            boolean rejected=false;
            try {AnatomyRuntime.reload(server);}
            catch(RuntimeException expected){rejected=true;}
            h.assertTrue(rejected,"The real GameTest ResourceManager must reject its incomplete canonical/legacy anatomy candidate");

            var after=AnatomyBindingState.snapshot(cow);
            h.assertTrue(after!=null && AnatomyBindingState.current(cow,before),
                "A candidate that fails while reading/validating resources must not replace the accepted provider, descriptor, binding, or local generation");
            h.assertTrue(after.provider()==before.provider() && after.descriptor().equals(before.descriptor())
                    && java.util.Objects.equals(after.binding(),before.binding()) && after.generation()==before.generation(),
                "Rejected live reload must preserve the exact accepted runtime slot, not reconstruct an equivalent-looking replacement");
            h.assertTrue(AnatomyNetworking.revision(server)==beforeRevision,
                "Rejected live reload must not advance the server publication revision");
            h.assertTrue(AnatomyRuntime.owns(cow),"Rejected live reload must leave the accepted support runtime-owned");

            var afterFrame=AnatomyMovement.publishedFrame(cow).orElseThrow();
            h.assertTrue(afterFrame.identity().revision()==beforeFrame.identity().revision()
                    && afterFrame.identity().bindingGeneration()==beforeFrame.identity().bindingGeneration()
                    && afterFrame.identity().support().equals(beforeFrame.identity().support())
                    && afterFrame.identity().entityId()==beforeFrame.identity().entityId(),
                "Rejected reload must not create a new causal publication identity for the still-live support");
        } finally {
            AnatomyRuntime.stop(server);cow.discard();
        }
        h.succeed();
    }
}
