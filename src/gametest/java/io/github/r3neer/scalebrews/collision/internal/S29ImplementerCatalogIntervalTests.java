package io.github.r3neer.scalebrews.collision.internal;

import com.mojang.serialization.JsonOps;
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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** IMPLEMENTER regressions above the live catalog transaction and interval provider seam. */
public final class S29ImplementerCatalogIntervalTests {
    /** Test-only bridge to the same transactional reload path exercised by server GameTests. */
    public static void reloadPrepared(net.minecraft.server.MinecraftServer server,
            Map<String,ModelGeometry> models,Map<String,PlatformDefinition> profiles) {
        AnatomyRuntime.reload(server,resources(server.getResourceManager(),new Fixture(models,profiles),false));
    }
    @GameTest
    public void acceptedReloadCannotCertifyPriorBindingInterval(GameTestHelper h) {
        var server=h.getLevel().getServer();var cow=h.spawn(EntityTypes.COW,2,20,2);
        cow.setNoAi(true);cow.setNoGravity(true);
        var before=fixture("scalebrews_test:s29_catalog_before");var after=fixture("scalebrews_test:s29_catalog_after");
        try {
            AnatomyRuntime.startPrepared(server,before.models(),before.profiles());
            var old=interval(cow);
            h.assertTrue(AnatomyRuntime.acceptsIntervalIdentity(cow,old) && AnatomyRuntime.interval(cow,old).isPresent(),
                "Observed live interval must be certifiable before reload");
            AnatomyRuntime.reload(server,resources(server.getResourceManager(),after,false));
            h.assertTrue(!AnatomyRuntime.acceptsIntervalIdentity(cow,old) && AnatomyRuntime.interval(cow,old).isEmpty(),
                "Accepted catalog revision must retire the old interval certification authority");
            var fresh=interval(cow);
            h.assertTrue(fresh.identity().revision()==old.identity().revision()+1
                    && fresh.identity().bindingGeneration()>old.identity().bindingGeneration(),
                "Fresh interval must belong to the committed revision and a new server binding");
            h.assertTrue(AnatomyRuntime.interval(cow,fresh).isPresent(),"The current binding must remain certifiable after reload");
        } finally {AnatomyRuntime.stop(server);cow.discard();}
        h.succeed();
    }

    @GameTest
    public void rejectedReloadPreservesExactIntervalAuthority(GameTestHelper h) {
        var server=h.getLevel().getServer();var cow=h.spawn(EntityTypes.COW,2,20,2);
        cow.setNoAi(true);cow.setNoGravity(true);var fixture=fixture("scalebrews_test:s29_catalog_stable");
        try {
            AnatomyRuntime.startPrepared(server,fixture.models(),fixture.profiles());
            var current=interval(cow);var slot=AnatomyBindingState.snapshot(cow);
            h.assertTrue(AnatomyRuntime.interval(cow,current).isPresent(),"Baseline authority must be usable");
            boolean rejected=false;
            try {AnatomyRuntime.reload(server,resources(server.getResourceManager(),fixture,true));}
            catch(RuntimeException expected){rejected=true;}
            h.assertTrue(rejected,"Malformed candidate must fail before committing a catalog revision");
            h.assertTrue(AnatomyBindingState.current(cow,slot) && AnatomyBindingState.snapshot(cow).provider()==slot.provider(),
                "Rejection must preserve the exact accepted provider slot");
            h.assertTrue(AnatomyRuntime.acceptsIntervalIdentity(cow,current) && AnatomyRuntime.interval(cow,current).isPresent(),
                "Rejected candidate must not invalidate the accepted interval authority");
        } finally {AnatomyRuntime.stop(server);cow.discard();}
        h.succeed();
    }

    /** A test handle over two genuine published query endpoints; this is provider-gate evidence, not wire evidence. */
    private static GeometryProvider.MotionIntervalHandle interval(Cow cow) {
        // This synchronous fixture starts/reloads and queries before the world's next pose cadence.
        AnatomyBindingState.provider(cow).tick(cow,cow.level().getGameTime());
        var a=AnatomyMovement.queryFrame(cow).orElseThrow();
        cow.move(MoverType.SELF,new Vec3(.125,0,0));
        var b=AnatomyMovement.queryFrame(cow).orElseThrow();
        return new GeometryProvider.MotionIntervalHandle(a.identity(),1,a,b);
    }
    private record Fixture(Map<String,ModelGeometry> models,Map<String,PlatformDefinition> profiles) {}
    private static Fixture fixture(String id) {
        var model=new ModelGeometry(2,id,"26.2",List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("body","root",List.of(-.5d,0d,-.5d),List.of(.5d,1d,.5d),null)),ModelGeometry.values(new Matrix4f()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse(id),Identifier.parse("scalebrews:static"),AnatomyFilter.DEFAULT)));
        return new Fixture(Map.of(id,model),Map.of("scalebrews_test:s29_catalog",profile));
    }
    private static ResourceManager resources(ResourceManager installed,Fixture fixture,boolean invalid) {
        var entry=fixture.models().entrySet().iterator().next();var id=Identifier.parse(entry.getKey());
        var pack=installed.listPacks().findFirst().orElseThrow();
        String geometry=invalid?"{":AnatomyCodecs.GEOMETRY.encodeStart(JsonOps.INSTANCE,entry.getValue()).getOrThrow().toString();
        String profile=PlatformDefinition.CODEC.encodeStart(JsonOps.INSTANCE,fixture.profiles().values().iterator().next()).getOrThrow().toString();
        return (ResourceManager)java.lang.reflect.Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),new Class<?>[]{ResourceManager.class},(proxy,method,args)->{
            if(method.getName().equals("listPacks"))return installed.listPacks();
            if(method.getName().equals("listResources")) {
                String directory=(String)args[0];
                if(directory.equals("scalebrews/entity_geometry"))return resource(pack,id.getNamespace()+":scalebrews/entity_geometry/"+id.getPath()+".json",geometry);
                if(directory.equals("scalebrews/entity_platform"))return resource(pack,"scalebrews_test:scalebrews/entity_platform/cow.json",profile);
                return Map.of();
            }
            throw new UnsupportedOperationException(method.toString());
        });
    }
    private static Map<Identifier,Resource> resource(net.minecraft.server.packs.PackResources pack,String id,String json) {
        byte[] bytes=json.getBytes(StandardCharsets.UTF_8);
        return Map.of(Identifier.parse(id),new Resource(pack,()->new java.io.ByteArrayInputStream(bytes)));
    }
}
