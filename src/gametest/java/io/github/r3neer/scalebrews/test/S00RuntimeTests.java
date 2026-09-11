package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import static io.github.r3neer.scalebrews.test.S00Fixtures.*;

/** A08/A09/A11: real registration and query/cache ownership, not a second lifecycle ledger. */
public final class S00RuntimeTests {
    private static final GeometryProvider.GeometryIdentityDescriptor DESCRIPTOR=new GeometryProvider.GeometryIdentityDescriptor(EPOCH,1,MODEL,STATIC,1);
    private static final class Provider implements GeometryProvider {
        CausalEndpoint endpoint;
        Snapshot snapshot;
        boolean broken;
        Provider(LivingEntity support){update(1,support.level().getGameTime(),support.position());}
        void update(long serial,long tick,Vec3 origin) {
            var inputs=new PoseProvider.Inputs(0,0,0,0,0,true);
            var root=new AnatomyMovement.RootFrame(serial,tick,origin,0,1,GravityFrame.VANILLA);
            endpoint=new CausalEndpoint(serial,tick,tick,root,new AnatomyPoseHistory.Sample(inputs,origin,0,1),Availability.AVAILABLE);
            snapshot=new Snapshot(1,Map.of("piece",box(UNIT).move(origin)));
        }
        public Optional<Snapshot> sample(LivingEntity ignored){return Optional.ofNullable(snapshot);}
        public Optional<CausalEndpoint> causalEndpoint(LivingEntity ignored){if(broken)throw new IllegalStateException("withdrawn provider resource");return Optional.of(endpoint);}
    }
    private static LivingEntity cow(GameTestHelper h){var cow=h.spawn(EntityTypes.COW,2,20,2);cow.setNoAi(true);cow.setNoGravity(true);return cow;}
    @GameTest public void changedEndpointCannotReuseMaterialSerial(GameTestHelper h) {
        var support=cow(h);var provider=new Provider(support);AnatomyMovement.activate(h.getLevel());
        try {
            AnatomyMovement.register(support,provider,DESCRIPTOR);check(AnatomyMovement.queryFrame(support).isPresent(),"Initial endpoint unavailable");
            provider.update(1,h.getLevel().getGameTime(),support.position().add(1,0,0));
            check(AnatomyMovement.queryFrame(support).isEmpty(),"Changed endpoint reused serial and left stale geometry available");
            provider.update(2,h.getLevel().getGameTime(),support.position().add(1,0,0));
            check(AnatomyMovement.queryFrame(support).orElseThrow().endpoint().frameSerial()==2,"Fresh serial did not recover after quarantine");
        }finally{AnatomyMovement.deactivate(h.getLevel());support.discard();}h.succeed();
    }
    @GameTest public void changedGeometryCannotReuseUnchangedEndpoint(GameTestHelper h) {
        var support=cow(h);var provider=new Provider(support);AnatomyMovement.activate(h.getLevel());
        try {
            AnatomyMovement.register(support,provider,DESCRIPTOR);var first=AnatomyMovement.queryFrame(support).orElseThrow();
            provider.snapshot=new GeometryProvider.Snapshot(1,Map.of("piece",box(UNIT).move(support.position().add(5,0,0))));
            check(AnatomyMovement.queryFrame(support).isEmpty(),"Conflicting geometry hid behind the same immutable endpoint");
            provider.snapshot=first.snapshot();check(AnatomyMovement.queryFrame(support).isEmpty(),"Replay of quarantined serial restored authority");
        }finally{AnatomyMovement.deactivate(h.getLevel());support.discard();}h.succeed();
    }
    @GameTest public void directProviderCannotRewindAuthorityWithHigherSerial(GameTestHelper h) {
        var support=cow(h);var provider=new Provider(support);AnatomyMovement.activate(h.getLevel());
        try {
            provider.update(1,10,support.position());AnatomyMovement.register(support,provider,DESCRIPTOR);
            AnatomyMovement.queryFrame(support).orElseThrow();provider.update(2,9,support.position());
            check(AnatomyMovement.queryFrame(support).isEmpty(),"Core accepted stale authority despite its new publication serial");
        }finally{AnatomyMovement.deactivate(h.getLevel());support.discard();}h.succeed();
    }
    @GameTest public void unavailableAndRebindNeverReusePriorGeometry(GameTestHelper h) {
        var support=cow(h);var provider=new Provider(support);AnatomyMovement.activate(h.getLevel());
        try {
            AnatomyMovement.register(support,provider,DESCRIPTOR);var first=AnatomyMovement.queryFrame(support).orElseThrow();
            var old=provider.endpoint;
            provider.endpoint=new GeometryProvider.CausalEndpoint(2,old.authorityTick(),old.jointSampleTick(),old.root(),old.sample(),GeometryProvider.Availability.UNAVAILABLE);
            provider.snapshot=null;check(AnatomyMovement.queryFrame(support).isEmpty(),"Unavailable retained a collider");
            check(AnatomyMovement.publishedFrame(support).orElseThrow().endpoint().availability()==GeometryProvider.Availability.UNAVAILABLE,"Unavailable state was not publishable");
            provider.update(3,h.getLevel().getGameTime(),support.position());check(AnatomyMovement.queryFrame(support).isPresent(),"Available endpoint failed to recover");
            AnatomyMovement.invalidateRoot(support);check(AnatomyMovement.queryFrame(support).isEmpty(),"Teleport barrier left a cached old endpoint");
            AnatomyMovement.register(support,provider,new GeometryProvider.GeometryIdentityDescriptor(EPOCH,1,MODEL,STATIC,2));
            var rebound=AnatomyMovement.queryFrame(support).orElseThrow();
            check(rebound.identity().bindingGeneration()==2 && !rebound.identity().equals(first.identity()),"Rebind reused stale causal identity");
        }finally{AnatomyMovement.deactivate(h.getLevel());support.discard();}h.succeed();
    }
    @GameTest public void providerFailureIsLocalAndDoesNotFreezeItsCollider(GameTestHelper h) {
        var first=cow(h);var second=cow(h);var broken=new Provider(first);var healthy=new Provider(second);AnatomyMovement.activate(h.getLevel());
        try {
            AnatomyMovement.register(first,broken,DESCRIPTOR);AnatomyMovement.register(second,healthy,DESCRIPTOR);
            AnatomyMovement.queryFrame(first).orElseThrow();broken.broken=true;
            check(AnatomyMovement.queryFrame(first).isEmpty(),"Failed provider remained available");
            check(AnatomyMovement.queryFrame(second).isPresent(),"One provider disabled an unrelated pair");
        }finally{AnatomyMovement.deactivate(h.getLevel());first.discard();second.discard();}h.succeed();
    }
    @GameTest public void spatialIndexPreservesObjectIdentityForReusedNetworkIds(GameTestHelper h) {
        var first=EntityTypes.COW.create(h.getLevel(),EntitySpawnReason.COMMAND);
        var second=EntityTypes.COW.create(h.getLevel(),EntitySpawnReason.COMMAND);
        check(first!=null && second!=null,"Could not construct identity fixture");
        first.setPos(2,20,2);second.setPos(7,20,2);second.setId(first.getId());
        check(first!=second && first.equals(second),"Fixture did not reproduce entity equality reuse");
        var body=h.makeMockPlayer(GameType.SURVIVAL);body.getAttribute(Attributes.SCALE).setBaseValue(.1);body.refreshDimensions();AnatomyMovement.activate(h.getLevel());
        try {
            AnatomyMovement.register(first,new Provider(first),DESCRIPTOR);AnatomyMovement.register(second,new Provider(second),DESCRIPTOR);
            check(!AnatomyMovement.spaceClear(body,new AABB(2.1,20.1,2.1,2.9,20.9,2.9)),"First same-id collider disappeared");
            check(!AnatomyMovement.spaceClear(body,new AABB(7.1,20.1,2.1,7.9,20.9,2.9)),"Second same-id collider disappeared");
        }finally{AnatomyMovement.deactivate(h.getLevel());first.discard();second.discard();body.discard();}h.succeed();
    }
    @GameTest public void serverRegistrationRejectsWrongThreadBeforeMutation(GameTestHelper h) throws InterruptedException {
        var support=cow(h);var provider=new Provider(support);AnatomyMovement.activate(h.getLevel());
        try {
            AnatomyMovement.register(support,provider,DESCRIPTOR);long generation=AnatomyMovement.registrationGeneration(support);
            var outcome=new AtomicReference<Throwable>();
            var thread=new Thread(()->{try{AnatomyMovement.register(support,provider,DESCRIPTOR);}catch(Throwable failure){outcome.set(failure);}},"S00-unauthorized-world-mutation");
            thread.start();thread.join(1000);
            check(!thread.isAlive(),"World-thread guard test did not terminate");
            check(outcome.get() instanceof IllegalStateException && AnatomyMovement.registrationGeneration(support)==generation,"Off-thread caller mutated server binding");
        }finally{AnatomyMovement.deactivate(h.getLevel());support.discard();}h.succeed();
    }
}
