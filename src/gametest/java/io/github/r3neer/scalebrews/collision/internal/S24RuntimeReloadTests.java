package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityTypes;
import org.joml.Matrix4f;

/** G3.9 live runtime reload barriers, above the catalog-only S15/S16 proofs. */
public final class S24RuntimeReloadTests {
    @GameTest
    public void acceptedRealResourceReloadRetiresPreviousRuntimeAuthority(GameTestHelper h) {
        var server=h.getLevel().getServer();var cow=h.spawn(EntityTypes.COW,2,20,2);cow.setNoAi(true);cow.setNoGravity(true);
        var fixture=fixture();
        try {
            AnatomyRuntime.startPrepared(server,fixture.models(),fixture.profiles());
            var before=AnatomyBindingState.snapshot(cow);
            h.assertTrue(before!=null && before.causal() && AnatomyRuntime.owns(cow),
                "Prepared baseline must install one live causal cow binding before the real resource reload");
            long beforeRevision=AnatomyNetworking.revision(server);var uuid=cow.getUUID();int entityId=cow.getId();

            AnatomyRuntime.reload(server); // Production overload: exact installed server ResourceManager.

            h.assertTrue(AnatomyNetworking.revision(server)==beforeRevision+1,
                "Accepted real resource reload must publish exactly the next catalog revision");
            h.assertTrue(cow.isAlive() && cow.getUUID().equals(uuid) && cow.getId()==entityId,
                "Catalog reload must not replace the live Minecraft entity instance/network identity");
            h.assertTrue(!AnatomyBindingState.current(cow,before) && AnatomyBindingState.snapshot(cow)==null,
                "Accepted revision without a default cow anatomy selection must retire the old provider/descriptor/binding slot");
            h.assertTrue(!AnatomyRuntime.owns(cow) && AnatomyMovement.publishedFrame(cow).isEmpty(),
                "Retired pre-reload authority must not remain publishable after the accepted catalog discontinuity");
            h.assertTrue(AnatomyRuntime.catalogBinding(cow).isEmpty(),
                "The installed GameTest catalog intentionally has no default cow selection; reload must not fabricate one");
        } finally {AnatomyRuntime.stop(server);cow.discard();}
        h.succeed();
    }

    @GameTest
    public void rejectedCandidateReloadPreservesExactAcceptedRuntimeAuthority(GameTestHelper h) {
        var server=h.getLevel().getServer();var cow=h.spawn(EntityTypes.COW,2,20,2);cow.setNoAi(true);cow.setNoGravity(true);
        var fixture=fixture();
        try {
            AnatomyRuntime.startPrepared(server,fixture.models(),fixture.profiles());
            var before=AnatomyBindingState.snapshot(cow);
            h.assertTrue(before!=null && before.causal() && AnatomyRuntime.owns(cow),
                "Prepared baseline must install one live causal cow binding before failure injection");
            long beforeRevision=AnatomyNetworking.revision(server);

            boolean rejected=false;
            try {AnatomyRuntime.reload(server,invalidResourceManager(server.getResourceManager()));}
            catch(RuntimeException expected){rejected=true;}
            h.assertTrue(rejected,"Malformed candidate resources must be rejected by the shared runtime reload transaction");

            var after=AnatomyBindingState.snapshot(cow);
            h.assertTrue(after!=null && AnatomyBindingState.current(cow,before),
                "A candidate rejected before catalog commit must not replace the accepted provider, descriptor, binding, or local generation");
            h.assertTrue(after.provider()==before.provider() && after.descriptor().equals(before.descriptor())
                    && java.util.Objects.equals(after.binding(),before.binding()) && after.generation()==before.generation(),
                "Rejected runtime reload must preserve the exact accepted slot, not reconstruct an equivalent-looking replacement");
            h.assertTrue(AnatomyNetworking.revision(server)==beforeRevision,
                "Rejected candidate must not advance the server publication revision");
            h.assertTrue(AnatomyRuntime.owns(cow),"Rejected candidate must leave the accepted support runtime-owned");
        } finally {AnatomyRuntime.stop(server);cow.discard();}
        h.succeed();
    }

    private record Fixture(Map<String,ModelGeometry> models,Map<String,PlatformDefinition> profiles) {}
    private static Fixture fixture() {
        var modelId="scalebrews_test:s24_reload_cow";
        var model=new ModelGeometry(2,modelId,"26.2",
            List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("body","root",List.of(-.5d,0d,-.5d),List.of(.5d,1d,.5d),null)),
            ModelGeometry.values(new Matrix4f()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse(modelId),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        return new Fixture(Map.of(modelId,model),Map.of("scalebrews_test:s24_reload",profile));
    }

    private static ResourceManager invalidResourceManager(ResourceManager installed) {
        var pack=installed.listPacks().findFirst().orElseThrow();
        return (ResourceManager)java.lang.reflect.Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),new Class<?>[]{ResourceManager.class},(proxy,method,args)->{
            if(method.getName().equals("listResources")) {
                String directory=(String)args[0];
                if(directory.equals("scalebrews/entity_geometry")) {
                    byte[] malformed="{".getBytes(StandardCharsets.UTF_8);
                    return Map.of(Identifier.parse("scalebrews_test:scalebrews/entity_geometry/broken.json"),
                        new Resource(pack,()->new java.io.ByteArrayInputStream(malformed)));
                }
                return Map.of();
            }
            if(method.getName().equals("listPacks"))return installed.listPacks();
            throw new UnsupportedOperationException(method.toString());
        });
    }
}
