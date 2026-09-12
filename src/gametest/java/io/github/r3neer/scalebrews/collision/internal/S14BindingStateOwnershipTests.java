package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/** Structural and lifecycle holdouts for the G2 live-binding ownership split. */
public final class S14BindingStateOwnershipTests {
    private static final String OWNER="io.github.r3neer.scalebrews.collision.internal.AnatomyBindingState";
    private static final Set<String> FORBIDDEN_FIELDS=Set.of(
        "PROVIDERS","REGISTRATIONS","DESCRIPTORS","CAPTURING","QUARANTINED_REGISTRATIONS");
    private static final String[] FORBIDDEN_OWNER_DEPENDENCIES={
        "io/github/r3neer/scalebrews/collision/internal/AnatomyMovement",
        "io/github/r3neer/scalebrews/collision/internal/AnatomyContactState",
        "io/github/r3neer/scalebrews/collision/internal/AnatomySpatialIndex",
        "io/github/r3neer/scalebrews/collision/runtime/TransportLedger",
        "io/github/r3neer/scalebrews/platform/Platforms"
    };
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);

    @GameTest
    public void dedicatedBindingOwnerMustExistAndMovementMustShedBindingStorage(GameTestHelper h) {
        h.assertTrue(classExists(OWNER),
            "G2 task 1 requires a dedicated AnatomyBindingState owner before AnatomyMovement can shed live binding storage");
        var remaining=Arrays.stream(AnatomyMovement.class.getDeclaredFields())
            .map(java.lang.reflect.Field::getName).filter(FORBIDDEN_FIELDS::contains)
            .collect(Collectors.toCollection(java.util.TreeSet::new));
        h.assertTrue(remaining.isEmpty(),"AnatomyMovement still owns live binding fields: "+remaining);
        h.succeed();
    }

    @GameTest
    public void bindingOwnerMustRemainOneWayAndPhysicsBlind(GameTestHelper h) {
        h.assertTrue(classExists(OWNER),"AnatomyBindingState must exist before dependency direction can be audited");
        try {
            var stream=S14BindingStateOwnershipTests.class.getResourceAsStream("/"+OWNER.replace('.','/')+".class");
            h.assertTrue(stream!=null,"Cannot inspect AnatomyBindingState bytecode");
            var constantPool=new String(stream.readAllBytes(),StandardCharsets.ISO_8859_1);
            var leaked=Arrays.stream(FORBIDDEN_OWNER_DEPENDENCIES).filter(constantPool::contains)
                .collect(Collectors.toCollection(java.util.TreeSet::new));
            h.assertTrue(leaked.isEmpty(),"AnatomyBindingState acquired forbidden orchestration/physics dependencies: "+leaked);
        } catch(IOException failure) {
            throw new AssertionError("Cannot inspect AnatomyBindingState bytecode",failure);
        }
        h.succeed();
    }

    @GameTest
    public void causalRebindMustNotPassThroughDescriptorlessDoubleSample(GameTestHelper h) {
        var level=h.getLevel();
        var support=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
        h.assertTrue(support!=null,"S14 atomic rebind fixture requires a support");
        support.setNoAi(true);support.setNoGravity(true);support.setPos(4,20,4);level.addFreshEntity(support);
        var samples=new AtomicInteger();
        var provider=causalProvider(17,samples,false);
        AnatomyMovement.activate(level);
        try {
            // Make the spatial index current before registration. The legacy-first implementation then
            // samples once as descriptorless membership and once again for the causal published frame.
            AnatomySpatialIndex.rebuild(level,level.getGameTime(),List.of());
            AnatomyMovement.register(support,provider,descriptor(17,1));
            h.assertTrue(samples.get()==1,
                "Causal rebind must install provider+descriptor atomically and sample once, not transiently as a legacy binding; samples="+samples.get());
            h.assertTrue(AnatomyMovement.queryFrame(support).isPresent(),"Atomic causal rebind must publish a usable current frame");
        } finally {
            AnatomyMovement.deactivate(level);support.discard();
        }
        h.succeed();
    }

    @GameTest
    public void registrationGenerationSurvivesDeactivateAndRebind(GameTestHelper h) {
        var level=h.getLevel();
        var support=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
        h.assertTrue(support!=null,"S14 generation fixture requires a support");
        support.setNoAi(true);support.setNoGravity(true);support.setPos(6,20,6);level.addFreshEntity(support);
        var provider=fixtureProvider(3);
        AnatomyMovement.activate(level);
        try {
            AnatomyMovement.register(support,provider);long first=AnatomyMovement.registrationGeneration(support);
            AnatomyMovement.register(support,provider);long second=AnatomyMovement.registrationGeneration(support);
            h.assertTrue(second==first+1,"Every rebind must advance local registration exactly once: "+first+" -> "+second);
            AnatomyMovement.deactivate(level);AnatomyMovement.activate(level);
            AnatomyMovement.register(support,provider);long third=AnatomyMovement.registrationGeneration(support);
            h.assertTrue(third==second+1,
                "Level deactivate must clear the active binding without rewinding the same entity instance's generation: "+second+" -> "+third);
        } finally {
            AnatomyMovement.deactivate(level);support.discard();
        }
        h.succeed();
    }

    @GameTest
    public void quarantineIsScopedToCurrentRegistration(GameTestHelper h) {
        var level=h.getLevel();
        var support=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
        h.assertTrue(support!=null,"S14 quarantine fixture requires a support");
        support.setNoAi(true);support.setNoGravity(true);support.setPos(8,20,8);level.addFreshEntity(support);
        AnatomyMovement.activate(level);
        try {
            AnatomyMovement.register(support,causalProvider(21,new AtomicInteger(),true),descriptor(21,1));
            long rejected=AnatomyMovement.registrationGeneration(support);
            h.assertTrue(AnatomyMovement.queryFrame(support).isEmpty(),"Rejected first capture must quarantine that exact registration");
            AnatomyMovement.register(support,causalProvider(21,new AtomicInteger(),false),descriptor(21,1));
            long rebound=AnatomyMovement.registrationGeneration(support);
            h.assertTrue(rebound==rejected+1 && AnatomyMovement.queryFrame(support).isPresent(),
                "A new registration must clear the previous generation's quarantine without rewinding identity: "+rejected+" -> "+rebound);
        } finally {
            AnatomyMovement.deactivate(level);support.discard();
        }
        h.succeed();
    }

    @GameTest
    public void rebindDuringCaptureMustNotOpenNestedCapture(GameTestHelper h) {
        var level=h.getLevel();
        var support=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
        h.assertTrue(support!=null,"S14 reentrancy fixture requires a support");
        support.setNoAi(true);support.setNoGravity(true);support.setPos(10,20,10);level.addFreshEntity(support);
        var nestedBlocked=new AtomicBoolean(false);var triggered=new AtomicBoolean(false);
        var replacement=causalProvider(31,new AtomicInteger(),false);
        var replacementDescriptor=descriptor(31,1);
        GeometryProvider first=new GeometryProvider() {
            @Override public Optional<GeometryProvider.Snapshot> sample(LivingEntity entity) {return Optional.of(snapshot(entity,31));}
            @Override public Optional<GeometryProvider.CausalEndpoint> causalEndpoint(LivingEntity entity) {
                if(triggered.compareAndSet(false,true)) {
                    AnatomyMovement.register(entity,replacement,replacementDescriptor);
                    nestedBlocked.set(AnatomyMovement.queryFrame(entity).isEmpty());
                }
                return Optional.of(endpoint(entity,1));
            }
        };
        AnatomyMovement.activate(level);
        try {
            AnatomyMovement.register(support,first,descriptor(31,1));
            h.assertTrue(nestedBlocked.get(),"A rebind inside provider capture must not open a nested capture while the outer guard is owned");
            h.assertTrue(AnatomyMovement.queryFrame(support).isPresent(),
                "The replacement binding must become capturable after the outer capture releases its guard");
        } finally {
            AnatomyMovement.deactivate(level);support.discard();
        }
        h.succeed();
    }

    private static GeometryProvider fixtureProvider(long revision) {
        return entity->Optional.of(snapshot(entity,revision));
    }

    private static GeometryProvider causalProvider(long revision,AtomicInteger samples,boolean fail) {
        return new GeometryProvider() {
            @Override public Optional<GeometryProvider.Snapshot> sample(LivingEntity entity) {
                samples.incrementAndGet();return Optional.of(snapshot(entity,revision));
            }
            @Override public Optional<GeometryProvider.CausalEndpoint> causalEndpoint(LivingEntity entity) {
                if(fail)throw new IllegalStateException("intentional S14 capture rejection");
                return Optional.of(endpoint(entity,1));
            }
        };
    }

    private static GeometryProvider.Snapshot snapshot(LivingEntity entity,long revision) {
        var box=ConvexBox.of(new AABB(-.5,-.5,-.5,.5,.5,.5),new Matrix4f()).move(entity.position());
        return new GeometryProvider.Snapshot(revision,Map.of("body",box));
    }

    private static GeometryProvider.CausalEndpoint endpoint(LivingEntity entity,long serial) {
        var root=new AnatomyMovement.RootFrame(serial,entity.level().getGameTime(),entity.position(),entity.yBodyRot,entity.getScale(),AnatomyMovement.gravity(entity));
        var sample=new AnatomyPoseHistory.Sample(INPUTS,root.origin(),root.yaw(),root.scale(),root.gravity());
        return new GeometryProvider.CausalEndpoint(serial,entity.level().getGameTime(),entity.level().getGameTime(),root,sample,GeometryProvider.Availability.AVAILABLE);
    }

    private static GeometryProvider.GeometryIdentityDescriptor descriptor(long revision,long bindingGeneration) {
        return new GeometryProvider.GeometryIdentityDescriptor(UUID.randomUUID(),revision,
            Identifier.parse("test:s14_binding"),Identifier.parse("test:s14_pose"),bindingGeneration);
    }

    private static boolean classExists(String name) {
        try {Class.forName(name,false,S14BindingStateOwnershipTests.class.getClassLoader());return true;}
        catch(ClassNotFoundException expected){return false;}
    }
}
