package io.github.r3neer.scalebrews.collision.internal;

import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** IMPLEMENTER state bookkeeping only; synthetic stored facts do not certify physical contact. */
public final class S43ContactIndexClientProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try(var world=context.worldBuilder().create()) {
            world.getServer().runOnServer(server->{
                var overworld=server.overworld();var nether=server.getLevel(Level.NETHER);
                require(nether!=null,"Fresh real world needs the second dimension for teardown bookkeeping");
                var first=EntityTypes.COW.create(overworld,EntitySpawnReason.COMMAND);
                var second=EntityTypes.COW.create(nether,EntitySpawnReason.COMMAND);
                var a=EntityTypes.PIG.create(overworld,EntitySpawnReason.COMMAND);
                var b=EntityTypes.PIG.create(nether,EntitySpawnReason.COMMAND);
                var c=EntityTypes.PIG.create(nether,EntitySpawnReason.COMMAND);
                require(first!=null && second!=null && a!=null && b!=null && c!=null,"Native state fixtures absent");
                // They are not inserted into entity lookup: reused IDs exercise identity maps
                // without corrupting a real world's native ID registry.
                second.setId(first.getId());b.setId(a.getId());
                require(first.equals(second) && a.equals(b),"Native equality-reuse precondition must be non-vacuous");
                try {
                    store(a,first,1);store(a,first,2);store(b,first,1);store(c,second,1);
                    require(exact(first,a,b) && exact(second,c),"Weak identity membership merges equal-ID endpoints or duplicates a revision");
                    store(a,second,3);
                    require(exact(first,b) && exact(second,a,c),"Replacement must unlink old support and link new one atomically");
                    long watermark=AnatomyContactState.contactSequence(a);
                    AnatomyContactState.clear(a);
                    require(exact(second,c) && AnatomyContactState.contact(a)==null
                        && AnatomyContactState.contactSequence(a)==watermark,"Clear must unlink membership and preserve the contact watermark");
                    // b already belongs to the destination level but retains the old support.
                    // The isolated fresh world's teardown must retire both endpoint directions.
                    AnatomyContactState.deactivate(overworld);
                    require(exact(first) && exact(second,c) && AnatomyContactState.contact(b)==null
                        && AnatomyContactState.contactSequence(b)>0,"Old-level teardown leaves stale support membership or erases destination watermark");
                    store(b,second,4);
                    require(exact(second,b,c),"Destination reattachment after old-level teardown must remain indexed");
                    AnatomyContactState.deactivate(nether);
                    require(exact(second) && AnatomyContactState.contact(b)==null && AnatomyContactState.contact(c)==null
                        && AnatomyContactState.contactSequence(b)==0,"Destination teardown must retire its own state and membership");
                    System.out.println("S43_CONTACT_INDEX_BOOKKEEPING_PASS equalIds=true replacement=true clear=true bothEndpointTeardown=true");
                } finally {AnatomyContactState.clear(a);AnatomyContactState.clear(b);AnatomyContactState.clear(c);}
            });
        }
    }
    private static void store(Entity body,LivingEntity support,long revision) {
        AnatomyContactState.setContact(body,support,"state-fixture",revision,new Vec3(0,1,0));
    }
    private static boolean exact(LivingEntity support,Entity... expected) {
        List<Entity> actual=AnatomyContactState.bodiesSupportedBy(support);
        return actual.size()==expected.length && java.util.Arrays.stream(expected)
            .allMatch(body->actual.stream().anyMatch(member->member==body));
    }
    private static void require(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
}
