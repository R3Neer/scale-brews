package io.github.r3neer.scalebrews.collision.internal;

import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** Pure owner checks for the post-S25 bounded client receipt-token inbox. */
public final class S25ReceiptTokenInboxTests {
    @GameTest
    public void boundsGenerationAndLifecycleRemainLocal(GameTestHelper h) {
        var inbox=new AnatomyReceiptTokenInbox();long now=100;
        var bodies=new java.util.ArrayList<UUID>();
        for(int i=0;i<AnatomyReceiptTokenInbox.MAX_BODIES;i++) {
            UUID body=UUID.randomUUID();bodies.add(body);
            h.assertTrue(inbox.accept(token(body,UUID.randomUUID(),1,1,10+i,now),now),
                "Inbox must admit each body up to MAX_BODIES");
        }
        h.assertTrue(inbox.retainedBodies()==AnatomyReceiptTokenInbox.MAX_BODIES,
            "Inbox body bound count mismatch");
        h.assertTrue(!inbox.accept(token(UUID.randomUUID(),UUID.randomUUID(),1,1,99,now),now),
            "Inbox must fail closed when MAX_BODIES is reached");

        UUID body=bodies.getFirst(),support=UUID.randomUUID();
        inbox.discardBody(body);
        for(int sequence=1;sequence<=AnatomyReceiptTokenInbox.MAX_TOKENS_PER_BODY+3;sequence++)
            h.assertTrue(inbox.accept(token(body,support,1,sequence,100+sequence,now),now),
                "Existing body token stream must remain admissible under bounded prefix eviction");
        h.assertTrue(inbox.retainedTokens()<=AnatomyReceiptTokenInbox.MAX_BODIES*AnatomyReceiptTokenInbox.MAX_TOKENS_PER_BODY,
            "Inbox token count exceeded global bounded product");

        h.assertTrue(!inbox.accept(token(body,support,0+1,AnatomyReceiptTokenInbox.MAX_TOKENS_PER_BODY+3,999,now),now),
            "Duplicate receipt sequence must be rejected");
        h.assertTrue(inbox.accept(token(body,support,2,1,200,now),now),
            "New tracking generation must replace prior token life");
        h.assertTrue(!inbox.accept(token(body,support,1,2,201,now),now),
            "Retired tracking generation must not re-enter the inbox");

        UUID otherBody=UUID.randomUUID(),otherSupport=UUID.randomUUID();
        inbox.discardBody(bodies.get(1));
        h.assertTrue(inbox.accept(token(otherBody,otherSupport,1,1,300,now),now),
            "Fixture must admit a second lifecycle-local body after releasing capacity");
        inbox.discardSupport(support);
        h.assertTrue(inbox.consumeIncorporated(body,support,Long.MAX_VALUE,now).isEmpty(),
            "Support discard must remove only dependent token authority");
        h.assertTrue(inbox.consumeIncorporated(otherBody,otherSupport,Long.MAX_VALUE,now).isPresent(),
            "Support discard must not erase unrelated body/support tokens");
        h.succeed();
    }

    @GameTest
    public void ttlAndIncorporationSelectNewestEligibleServerToken(GameTestHelper h) {
        var inbox=new AnatomyReceiptTokenInbox();UUID body=UUID.randomUUID(),support=UUID.randomUUID();
        long now=500;
        h.assertTrue(inbox.accept(token(body,support,1,1,10,now-AnatomyTransportReceipts.HISTORY_TICKS),now-1),
            "Fixture stale token must be retainable before the pruning boundary");
        inbox.prune(now);
        h.assertTrue(inbox.retainedTokens()==0,
            "Token at HISTORY_TICKS age must be pruned");

        h.assertTrue(inbox.accept(token(body,support,1,1,10,now),now),"Token 1 rejected");
        h.assertTrue(inbox.accept(token(body,support,1,2,20,now),now),"Token 2 rejected");
        h.assertTrue(inbox.accept(token(body,support,1,3,30,now),now),"Token 3 rejected");

        var chosen=inbox.consumeIncorporated(body,support,25,now).orElseThrow();
        h.assertTrue(chosen.receiptSequence()==2,
            "Incorporation must choose the newest server token whose endpoint is already incorporated");
        var future=inbox.consumeIncorporated(body,support,25,now);
        h.assertTrue(future.isEmpty(),
            "Consumed prefix must not be reusable and future endpoint token must remain ineligible");
        var last=inbox.consumeIncorporated(body,support,30,now).orElseThrow();
        h.assertTrue(last.receiptSequence()==3 && inbox.retainedTokens()==0,
            "Later incorporation must expose only the remaining future token");
        h.succeed();
    }

    private static AnatomyTransportReceiptPayload token(UUID body,UUID support,long generation,long sequence,long frame,long tick) {
        return new AnatomyTransportReceiptPayload(UUID.randomUUID(),1,Identifier.parse("minecraft:overworld"),
            7,body,generation,support,frame,sequence,tick);
    }
}
