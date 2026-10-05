package io.github.r3neer.scalebrews.collision.internal;

import com.mojang.serialization.JsonOps;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.CollisionVariantSources;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionCodecs;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** IMPLEMENTER development probes. Independent acceptance remains owned by ADVERSARY. */
public final class S48ImplementerLiveVariantTests {
    private static final Identifier COW=Identifier.parse("minecraft:cow");
    private static final Identifier ADULT=Identifier.parse("scalebrews_test:s48_adult");
    private static final Identifier BABY=Identifier.parse("scalebrews_test:s48_baby");
    private static final String MODE="s48_impl_mode";
    private static final AtomicReference<String> VALUE=new AtomicReference<>("on");
    private static boolean registered;

    @GameTest
    public void realAgeChangeRetiresOldMaterialBeforeTheNextPoseTick(GameTestHelper h) {
        var cow=cow(h);var server=h.getLevel().getServer();
        try {
            start(h,List.of(row(Map.of("age","adult"),ADULT),row(Map.of("age","baby"),BABY)));
            var first=frame(cow);
            h.assertTrue(first.identity().model().equals(ADULT),"Live adult selects its exact variant without a default");
            cow.setBaby(true);
            var descriptor=AnatomyBindingState.descriptor(cow);
            h.assertTrue(descriptor!=null && descriptor.model().equals(BABY)
                && descriptor.bindingGeneration()>first.identity().bindingGeneration(),"Real synchronized age change installs a fresh baby registration immediately");
            h.assertTrue(AnatomyMovement.queryFrame(cow).isEmpty(),"Query cannot forge the new row's joint sample or return old adult material");
            var second=frame(cow);
            h.assertTrue(second.identity().bindingIdentity().equals(descriptor.bindingIdentity())
                && !second.identity().bindingIdentity().equals(first.identity().bindingIdentity()),"Baby material retains its exact accepted content token");
            cow.setBaby(false);
            var third=frame(cow);
            h.assertTrue(third.identity().model().equals(ADULT)
                && third.identity().bindingGeneration()>second.identity().bindingGeneration(),"A-B-A cannot reuse historical registration authority");
        } finally {AnatomyRuntime.stop(server);cow.discard();}
        h.succeed();
    }

    @GameTest
    public void unavailableSourceHasNoDefaultAndCanRecoverLocally(GameTestHelper h) {
        register();VALUE.set("on");var cow=cow(h);var server=h.getLevel().getServer();
        try {
            start(h,List.of(row(Map.of(),ADULT),row(Map.of(MODE,"on"),BABY)));
            var before=frame(cow);
            VALUE.set(null);AnatomyApi.variantChanged(cow);
            h.assertTrue(!AnatomyBindingState.hasProvider(cow) && AnatomyMovement.queryFrame(cow).isEmpty(),"Unknown source retires the active row instead of borrowing default");
            VALUE.set("outside-domain");AnatomyApi.variantChanged(cow);
            h.assertTrue(!AnatomyBindingState.hasProvider(cow),"Out-of-domain observation is unavailable");
            VALUE.set("on");AnatomyApi.variantChanged(cow);
            var recovered=frame(cow);
            h.assertTrue(recovered.identity().bindingIdentity().equals(before.identity().bindingIdentity())
                && recovered.identity().bindingGeneration()>before.identity().bindingGeneration(),"Loaded absent participant recovers the same row with fresh physical authority");
            VALUE.set("off");AnatomyApi.variantChanged(cow);var fallback=frame(cow);
            h.assertTrue(fallback.identity().model().equals(ADULT),"Known inputs may legitimately resolve the declared default");
            var slot=AnatomyBindingState.snapshot(cow);
            VALUE.set("idle");AnatomyApi.variantChanged(cow);
            h.assertTrue(AnatomyBindingState.current(cow,slot),"Different known inputs selecting the same row do not churn providers");
        } finally {AnatomyRuntime.stop(server);cow.discard();VALUE.set("on");}
        h.succeed();
    }

    @GameTest
    public void missingRequiredSourceDisablesTheTypeIncludingItsDefault(GameTestHelper h) {
        var cow=cow(h);var server=h.getLevel().getServer();
        try {
            start(h,List.of(row(Map.of(),ADULT),row(Map.of("s48_impl_missing","x"),BABY)));
            h.assertTrue(!AnatomyBindingState.hasProvider(cow) && AnatomyMovement.queryFrame(cow).isEmpty(),"A default cannot hide an unobservable required variant axis");
        } finally {AnatomyRuntime.stop(server);cow.discard();}
        h.succeed();
    }

    @GameTest
    public void independentManualRegistrationSurvivesTraitChanges(GameTestHelper h) {
        var cow=cow(h);var server=h.getLevel().getServer();
        try {
            start(h,List.of(row(Map.of(),BABY),row(Map.of("age","adult"),ADULT)));
            GeometryProvider manual=entity->Optional.empty();
            AnatomyMovement.register(cow,manual);
            var slot=AnatomyBindingState.snapshot(cow);
            h.assertTrue(slot.binding().geometry().model().equals(ADULT),"Manual seam borrows the selected live row's policy instead of the different default");
            cow.setBaby(true);AnatomyRuntime.prepare(h.getLevel());
            h.assertTrue(AnatomyRuntime.catalogBinding(cow).orElseThrow().geometry().model().equals(BABY),"Independent provider ownership does not freeze the public live catalog observation");
            h.assertTrue(AnatomyBindingState.current(cow,slot) && AnatomyBindingState.provider(cow)==manual
                && !AnatomyRuntime.ownsSupportBinding(cow),"Runtime neither withdraws nor reclaims a manually owned registration");
        } finally {AnatomyRuntime.stop(server);cow.discard();}
        h.succeed();
    }

    @GameTest
    public void withdrawalFreezesThePreviousEndpointAndDetachesItsSlot(GameTestHelper h) throws ReflectiveOperationException {
        var cow=cow(h);var server=h.getLevel().getServer();
        try {
            start(h,List.of(row(Map.of(),ADULT)));var before=frame(cow);
            var slot=AnatomyBindingState.snapshot(cow);
            var withdrawal=AnatomyMovement.withdrawRegistration(cow,slot.provider(),slot.descriptor().bindingGeneration()).orElseThrow();
            h.assertTrue(withdrawal.identity().equals(before.identity())
                && withdrawal.endpoint().availability()==GeometryProvider.Availability.UNAVAILABLE
                && withdrawal.endpoint().frameSerial()==before.endpoint().frameSerial()+1
                && withdrawal.endpoint().root().equals(before.endpoint().root())
                && withdrawal.endpoint().sample().equals(before.endpoint().sample()),"Retirement freezes old content/root/sample and advances only its barrier serial");
            h.assertTrue(!AnatomyBindingState.hasProvider(cow) && AnatomyEndpointLedger.get(cow)==null
                && AnatomyMovement.registrationGeneration(cow)==slot.generation(),"Retirement releases executable state without laundering the local generation");
            var query=AnatomyMovement.class.getDeclaredMethod("currentSnapshot",net.minecraft.world.entity.LivingEntity.class,GeometryProvider.class);
            query.setAccessible(true);
            h.assertTrue(((Optional<?>)query.invoke(null,cow,slot.provider())).isEmpty(),"Captured retired provider cannot reenter fixture sample fallback after its descriptor was detached");
        } finally {AnatomyRuntime.stop(server);cow.discard();}
        h.succeed();
    }

    @GameTest
    public void exhaustedPhysicalGenerationLeavesAbsenceWithoutRetryingInstallation(GameTestHelper h) throws ReflectiveOperationException {
        var cow=cow(h);var server=h.getLevel().getServer();
        try {
            start(h,List.of(row(Map.of("age","adult"),ADULT),row(Map.of("age","baby"),BABY)));
            frame(cow);
            var states=AnatomyRuntime.class.getDeclaredField("STATES");states.setAccessible(true);
            var state=((Map<?,?>)states.get(null)).get(server);
            var next=state.getClass().getDeclaredField("nextBindingGeneration");next.setAccessible(true);next.setLong(state,0);
            cow.setBaby(true);
            h.assertTrue(!AnatomyBindingState.hasProvider(cow),"Unrepresentable replacement authority retires A and leaves absence");
            for(int i=0;i<100;i++) {
                AnatomyRuntime.prepare(h.getLevel());
                h.assertTrue(AnatomyMovement.queryFrame(cow).isEmpty(),"Failed replacement cannot retain A or retry installation on every read");
            }
            h.assertTrue(next.getLong(state)==0,"Generation exhaustion is not silently repaired by reusing authority");
        } finally {AnatomyRuntime.stop(server);cow.discard();}
        h.succeed();
    }

    private static void register() {
        if(!registered) {CollisionVariantSources.register(MODE,Set.of("on","off","idle"),entity->VALUE.get());registered=true;}
    }
    private static Cow cow(GameTestHelper h) {
        var cow=h.spawn(EntityTypes.COW,2,20,2);cow.setNoAi(true);cow.setNoGravity(true);cow.setBaby(false);return cow;
    }
    private static GeometryProvider.QueryFrame frame(Cow cow) {
        AnatomyBindingState.provider(cow).tick(cow,cow.level().getGameTime());
        return AnatomyMovement.queryFrame(cow).orElseThrow();
    }
    private static void start(GameTestHelper h,List<CollisionBinding> bindings) {
        var server=h.getLevel().getServer();
        AnatomyRuntime.startPrepared(server,Map.of(),Map.of());
        AnatomyRuntime.reload(server,resources(server.getResourceManager(),bindings));
    }
    private static CollisionBinding row(Map<String,String> variant,Identifier model) {
        return new CollisionBinding(1,COW,variant,
            new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART,model,Map.of(),AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(Identifier.parse("scalebrews:static"),Map.of(),Set.of()),
            BuiltInRootTransformProviders.ENTITY_ROOT,CollisionPolicy.Patch.EMPTY,Set.of());
    }
    private static ModelGeometry model(Identifier id) {
        double radius=id.equals(ADULT)?.5:.25;
        return new ModelGeometry(2,COW.toString(),"26.2",List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("body","root",List.of(-radius,0d,-radius),List.of(radius,1d,radius),null)),ModelGeometry.values(new Matrix4f()));
    }
    private static ResourceManager resources(ResourceManager installed,List<CollisionBinding> bindings) {
        var pack=installed.listPacks().findFirst().orElseThrow();
        var geometry=new TreeMap<Identifier,Resource>(java.util.Comparator.comparing(Identifier::toString));
        for(var id:List.of(ADULT,BABY))geometry.put(Identifier.fromNamespaceAndPath(id.getNamespace(),"scalebrews/entity_geometry/"+id.getPath()+".json"),
            resource(pack,AnatomyCodecs.GEOMETRY.encodeStart(JsonOps.INSTANCE,model(id)).getOrThrow().toString()));
        var rows=new TreeMap<Identifier,Resource>(java.util.Comparator.comparing(Identifier::toString));
        for(int i=0;i<bindings.size();i++)rows.put(Identifier.parse("scalebrews_test:scalebrews/entity_collision/s48_"+i+".json"),
            resource(pack,CollisionCodecs.BINDING.encodeStart(JsonOps.INSTANCE,bindings.get(i)).getOrThrow().toString()));
        return (ResourceManager)java.lang.reflect.Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),new Class<?>[]{ResourceManager.class},(proxy,method,args)->{
            if(method.getName().equals("listPacks"))return installed.listPacks();
            if(method.getName().equals("listResources"))return switch((String)args[0]) {
                case "scalebrews/entity_geometry"->geometry;
                case "scalebrews/entity_collision"->rows;
                default->Map.of();
            };
            throw new UnsupportedOperationException(method.toString());
        });
    }
    private static Resource resource(net.minecraft.server.packs.PackResources pack,String json) {
        byte[] bytes=json.getBytes(StandardCharsets.UTF_8);
        return new Resource(pack,()->new java.io.ByteArrayInputStream(bytes));
    }
}
