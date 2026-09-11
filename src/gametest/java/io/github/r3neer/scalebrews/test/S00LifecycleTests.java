package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.*;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import io.github.r3neer.scalebrews.platform.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import static io.github.r3neer.scalebrews.test.S00Fixtures.*;

/** A07/A08/A11/A13: real registry callbacks and the active anatomical support graph. */
public final class S00LifecycleTests {
    private static GeometryProvider.GeometryIdentityDescriptor descriptor(long generation) {
        return new GeometryProvider.GeometryIdentityDescriptor(EPOCH,1,MODEL,STATIC,generation);
    }
    private static class Provider implements GeometryProvider {
        final CausalEndpoint endpoint;
        final Snapshot snapshot;
        Runnable onEndpoint=()->{},onSample=()->{};
        int endpointCalls;
        Provider(LivingEntity entity,long serial,double shift) {
            var origin=entity.position().add(shift,0,0);var gravity=AnatomyMovement.gravity(entity);long tick=entity.level().getGameTime();
            var root=new AnatomyMovement.RootFrame(serial,tick,origin,0,1,gravity);
            var inputs=new PoseProvider.Inputs(0,0,0,0,0,true);
            endpoint=new CausalEndpoint(serial,tick,tick,root,new AnatomyPoseHistory.Sample(inputs,origin,0,1,gravity),Availability.AVAILABLE);
            double width=entity.getBbWidth();
            // Explicit analytic fixture, not a production fallback from entity bounds.
            snapshot=new Snapshot(1,Map.of("piece",box(new AABB(-width/2,0,-width/2,width/2,2,width/2)).move(origin)));
        }
        public Optional<CausalEndpoint> causalEndpoint(LivingEntity ignored) {
            endpointCalls++;var callback=onEndpoint;onEndpoint=()->{};callback.run();return Optional.of(endpoint);
        }
        public Optional<Snapshot> sample(LivingEntity ignored) {
            var callback=onSample;onSample=()->{};callback.run();return Optional.of(snapshot);
        }
    }
    private static LivingEntity cow(GameTestHelper h) {
        var entity=h.spawn(EntityTypes.COW,2,20,2);entity.setNoAi(true);entity.setNoGravity(true);return entity;
    }
    @GameTest public void rebindDuringCaptureCannotPublishRetiredGeometry(GameTestHelper h) {
        var entity=cow(h);AnatomyMovement.activate(h.getLevel());
        try {
            var old=new Provider(entity,1,0);var fresh=new Provider(entity,2,4);
            AnatomyMovement.register(entity,old,descriptor(1));
            old.onSample=()->AnatomyMovement.register(entity,fresh,descriptor(2));
            check(AnatomyMovement.queryFrame(entity).isEmpty(),"Retired provider published across a rebind inside its sample callback");
            var frame=AnatomyMovement.queryFrame(entity).orElseThrow();
            check(frame.identity().bindingGeneration()==2 && frame.snapshot().equals(fresh.snapshot),"Fresh binding was contaminated by its predecessor");
        }finally{AnatomyMovement.deactivate(h.getLevel());entity.discard();}h.succeed();
    }
    @GameTest public void invalidationInsideFirstCaptureIsACausalBarrier(GameTestHelper h) {
        var entity=cow(h);AnatomyMovement.activate(h.getLevel());
        try {
            var provider=new Provider(entity,1,0);AnatomyMovement.register(entity,provider,descriptor(1));
            provider.onSample=()->AnatomyMovement.invalidateRoot(entity);
            check(AnatomyMovement.queryFrame(entity).isEmpty(),"First sample was published after a root invalidation inside its callback");
            check(AnatomyMovement.queryFrame(entity).isEmpty(),"Rejected pre-barrier serial became available on replay");
            var fresh=new Provider(entity,2,0);AnatomyMovement.register(entity,fresh,descriptor(2));
            check(AnatomyMovement.queryFrame(entity).isPresent(),"Explicit new binding did not recover after first-capture invalidation");
        }finally{AnatomyMovement.deactivate(h.getLevel());entity.discard();}h.succeed();
    }
    @GameTest public void nestedProviderQueryCannotEvaluateASecondEndpoint(GameTestHelper h) {
        var entity=cow(h);AnatomyMovement.activate(h.getLevel());
        try {
            var provider=new Provider(entity,1,0);var nested=new ArrayList<Optional<GeometryProvider.QueryFrame>>();
            AnatomyMovement.register(entity,provider,descriptor(1));
            provider.onEndpoint=()->nested.add(AnatomyMovement.queryFrame(entity));
            AnatomyMovement.queryFrame(entity);
            check(nested.size()==1 && nested.getFirst().isEmpty() && provider.endpointCalls==1,"Reentrant provider query evaluated or published a second endpoint");
        }finally{AnatomyMovement.deactivate(h.getLevel());entity.discard();}h.succeed();
    }
    @GameTest public void validReadOnlyProviderQueriesRetainIdentity(GameTestHelper h) {
        var entity=cow(h);AnatomyMovement.activate(h.getLevel());
        try {
            var provider=new Provider(entity,1,0);AnatomyMovement.register(entity,provider,descriptor(1));
            var first=AnatomyMovement.queryFrame(entity).orElseThrow();
            for(int i=0;i<32;i++)check(AnatomyMovement.queryFrame(entity).orElseThrow().equals(first),"Read-only query changed material identity");
        }finally{AnatomyMovement.deactivate(h.getLevel());entity.discard();}h.succeed();
    }
    @GameTest public void instantaneousProviderCannotInventAStaticHistory(GameTestHelper h) {
        var entity=cow(h);
        try {
            check(h.getLevel().getGameTime()>0,"Motion history fixture needs a prior authority tick");
            GeometryProvider instantaneous=e->Optional.of(new GeometryProvider.Snapshot(1,Map.of("piece",box(UNIT))));
            check(instantaneous.sample(entity).isPresent() && instantaneous.motion(entity).isEmpty(),"A current snapshot silently certified a historical static interval");
        }finally{entity.discard();}h.succeed();
    }
    @GameTest public void anatomicalCycleIsRejectedBeforeMutatingValidContact(GameTestHelper h) {
        var a=cow(h);var b=cow(h);AnatomyMovement.activate(h.getLevel());
        try {
            b.setPos(a.position().add(a.getBbWidth(),0,0));
            AnatomyMovement.gravity(a,new GravityFrame(Direction.EAST));AnatomyMovement.gravity(b,new GravityFrame(Direction.WEST));
            var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.of(2d),List.of(),
                Optional.of(new AnatomyDefinition(MODEL,STATIC,AnatomyFilter.DEFAULT)));
            Platforms.anatomicalDefinitions(h.getLevel(),List.of(profile));
            AnatomyMovement.register(a,new Provider(a,1,0),descriptor(1));AnatomyMovement.register(b,new Provider(b,1,0),descriptor(1));
            long tick=h.getLevel().getGameTime();
            var onB=new SurfaceContact(b.getUUID(),1,"piece",0,new Vec3(0,.35,.5),new Vec3(-1,0,0),tick);
            check(AnatomyMovement.confirm(a,b,onB) && AnatomyMovement.supported(a),"Valid side contact fixture was not physically supported");
            var first=AnatomyMovement.contact(a);
            var onA=new SurfaceContact(a.getUUID(),1,"piece",1,new Vec3(1,.35,.5),new Vec3(1,0,0),tick);
            check(!AnatomyMovement.confirm(b,a,onA),"Anatomical support cycle bypassed the legacy-only ancestry check");
            check(AnatomyMovement.contact(b)==null && AnatomyMovement.contact(a).equals(first),"Cycle rejection mutated the valid contact");
        }finally{Platforms.clearAnatomicalDefinitions(h.getLevel());AnatomyMovement.deactivate(h.getLevel());a.discard();b.discard();}h.succeed();
    }

    @GameTest public void firstCaptureInvalidationExceptionCannotForgetBarrier(GameTestHelper h) {
        var entity=cow(h);AnatomyMovement.activate(h.getLevel());
        try {
            var provider=new Provider(entity,1,0);AnatomyMovement.register(entity,provider,descriptor(1));
            provider.onSample=()->{AnatomyMovement.invalidateRoot(entity);throw new IllegalStateException("fault after lifecycle barrier");};
            check(AnatomyMovement.queryFrame(entity).isEmpty(),"Invalidating provider failure published its first endpoint");
            check(AnatomyMovement.queryFrame(entity).isEmpty(),"Provider failure forgot the first-capture lifecycle barrier");
            AnatomyMovement.register(entity,new Provider(entity,2,0),descriptor(2));
            check(AnatomyMovement.queryFrame(entity).isPresent(),"Explicit rebind did not recover a quarantined first capture");
        }finally{AnatomyMovement.deactivate(h.getLevel());entity.discard();}h.succeed();
    }
    @GameTest public void rebindThenProviderFailureCannotQuarantineReplacement(GameTestHelper h) {
        var entity=cow(h);AnatomyMovement.activate(h.getLevel());
        try {
            var old=new Provider(entity,1,0);var fresh=new Provider(entity,2,4);AnatomyMovement.register(entity,old,descriptor(1));
            old.onEndpoint=()->{AnatomyMovement.register(entity,fresh,descriptor(2));throw new IllegalStateException("retired provider failed after rebind");};
            check(AnatomyMovement.queryFrame(entity).isEmpty(),"Retired failing capture published through its replacement");
            var current=AnatomyMovement.queryFrame(entity).orElseThrow();
            check(current.identity().bindingGeneration()==2 && current.snapshot().equals(fresh.snapshot),"Retired provider failure quarantined the replacement binding");
        }finally{AnatomyMovement.deactivate(h.getLevel());entity.discard();}h.succeed();
    }
    @GameTest public void deactivateReactivationCannotReuseLocalRegistrationIdentity(GameTestHelper h) {
        var entity=cow(h);AnatomyMovement.activate(h.getLevel());
        try {
            var provider=new Provider(entity,1,0);AnatomyMovement.register(entity,provider,descriptor(1));
            long before=AnatomyMovement.registrationGeneration(entity);
            provider.onSample=()->{AnatomyMovement.deactivate(h.getLevel());AnatomyMovement.activate(h.getLevel());AnatomyMovement.register(entity,provider,descriptor(1));};
            check(AnatomyMovement.queryFrame(entity).isEmpty(),"Capture crossed deactivate/reactivate with a reused local registration identity");
            check(AnatomyMovement.registrationGeneration(entity)>before,"Local registration generation rewound across level lifecycle");
            check(AnatomyMovement.queryFrame(entity).isPresent(),"Fresh local registration failed after lifecycle replacement");
        }finally{AnatomyMovement.deactivate(h.getLevel());entity.discard();}h.succeed();
    }
    private static void forceSuspendedForLifecycleFixture(Entity body,LivingEntity support) {
        // State setup only. Reachability through blocked separation is covered by the squeezing/recovery tests.
        try {
            var method=AnatomyMovement.class.getDeclaredMethod("suspend",Entity.class,LivingEntity.class);
            method.setAccessible(true);method.invoke(null,body,support);
        }catch(ReflectiveOperationException failure){throw new AssertionError("Could not seed pair quarantine fixture",failure);}
    }
    @GameTest public void rebindClearsSuspendedPairQuarantine(GameTestHelper h) {
        var support=cow(h);var body=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);AnatomyMovement.activate(h.getLevel());
        try {
            body.setPos(support.position());
            var enclosing=box(new AABB(-.35,-.35,-.35,.35,.35,.35)).move(support.position());
            GeometryProvider provider=e->Optional.of(new GeometryProvider.Snapshot(1,Map.of("piece",enclosing)));
            AnatomyMovement.register(support,provider);forceSuspendedForLifecycleFixture(body,support);
            check(AnatomyMovement.suspended(body,support),"Fixture did not seed an overlapping suspended pair");
            AnatomyMovement.register(support,provider);
            check(!AnatomyMovement.suspended(body,support),"Explicit support rebind retained quarantine from the retired binding");
        }finally{AnatomyMovement.deactivate(h.getLevel());support.discard();body.discard();}
        h.succeed();
    }
    @GameTest public void replacementSupportCannotInheritSuspendedPairByUuid(GameTestHelper h) {
        var support=cow(h);var body=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);AnatomyMovement.activate(h.getLevel());
        Mob replacement=null;
        try {
            body.setPos(support.position());
            var enclosing=box(new AABB(-.35,-.35,-.35,.35,.35,.35)).move(support.position());
            AnatomyMovement.register(support,e->Optional.of(new GeometryProvider.Snapshot(1,Map.of("piece",enclosing))));
            forceSuspendedForLifecycleFixture(body,support);
            check(AnatomyMovement.suspended(body,support),"Fixture did not seed original support quarantine");
            var uuid=support.getUUID();
            replacement=EntityTypes.COW.create(h.getLevel(),EntitySpawnReason.COMMAND);
            check(replacement!=null,"Could not construct replacement support");
            replacement.setUUID(uuid);replacement.setNoAi(true);replacement.setNoGravity(true);replacement.setPos(body.position());
            var replacementBox=box(new AABB(-.35,-.35,-.35,.35,.35,.35)).move(replacement.position());
            AnatomyMovement.register(replacement,e->Optional.of(new GeometryProvider.Snapshot(1,Map.of("piece",replacementBox))));
            check(!AnatomyMovement.suspended(body,replacement),"Replacement entity inherited suspended-pair state solely through reused UUID");
        }finally{AnatomyMovement.deactivate(h.getLevel());if(replacement!=null)replacement.discard();support.discard();body.discard();}
        h.succeed();
    }
    @GameTest public void defaultMotionAbsenceDoesNotSampleCurrentGeometry(GameTestHelper h) {
        var entity=cow(h);
        try {
            int[] samples={0};
            GeometryProvider instantaneous=e->{samples[0]++;return Optional.of(new GeometryProvider.Snapshot(1,Map.of("piece",box(UNIT))));};
            check(instantaneous.motion(entity).isEmpty() && samples[0]==0,"Absent historical motion evaluated current geometry as a side effect");
        }finally{entity.discard();}h.succeed();
    }
}
