package io.github.r3neer.scalebrews.collision.internal;

import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** IMPLEMENTER regression for NFR-011 retention of the G4.1 client receipt-token cache. */
public final class S25ImplementerTokenRetentionTests {
    @GameTest
    public void receiptTokenInboxIsGloballyBoundedAndTtlPruned(GameTestHelper h) {
        var inbox=new AnatomyReceiptTokenInbox();
        var support=UUID.randomUUID();
        long now=100;

        for(int index=0;index<AnatomyReceiptTokenInbox.MAX_BODIES;index++)
            h.assertTrue(inbox.accept(token(UUID.randomUUID(),support,index+1,index+1,1,now),now),
                "Token body within global cap must be retained");
        h.assertTrue(inbox.retainedBodies()==AnatomyReceiptTokenInbox.MAX_BODIES,
            "Receipt-token body cache must stop at its documented global cap");

        var overflowBody=UUID.randomUUID();
        h.assertTrue(!inbox.accept(token(overflowBody,support,1,1,1,now),now),
            "New body beyond global token cap must fail closed");
        h.assertTrue(inbox.retainedBodies()==AnatomyReceiptTokenInbox.MAX_BODIES,
            "Rejected body must not enlarge the cache");

        long expiredAt=now+AnatomyTransportReceipts.HISTORY_TICKS;
        inbox.prune(expiredAt);
        h.assertTrue(inbox.retainedBodies()==0 && inbox.retainedTokens()==0,
            "Inactive body queues must disappear at receipt TTL even without another carry");
        h.assertTrue(!inbox.accept(token(UUID.randomUUID(),support,1,1,1,now),expiredAt),
            "A token already outside receipt TTL must not be re-admitted");

        var body=UUID.randomUUID();
        long fresh=expiredAt+1;
        for(int sequence=1;sequence<=AnatomyReceiptTokenInbox.MAX_TOKENS_PER_BODY+10;sequence++)
            h.assertTrue(inbox.accept(token(body,support,sequence,sequence,2,fresh),fresh),
                "Per-body overflow may evict old metadata but must stay bounded");
        h.assertTrue(inbox.retainedBodies()==1
                && inbox.retainedTokens()==AnatomyReceiptTokenInbox.MAX_TOKENS_PER_BODY,
            "Per-body token history must retain only its constant cap");

        var chosen=inbox.consumeIncorporated(body,support,
            AnatomyReceiptTokenInbox.MAX_TOKENS_PER_BODY+10,fresh).orElse(null);
        h.assertTrue(chosen!=null && chosen.receiptSequence()==AnatomyReceiptTokenInbox.MAX_TOKENS_PER_BODY+10,
            "Newest incorporated server token must be selected exactly");
        h.assertTrue(inbox.retainedBodies()==0 && inbox.retainedTokens()==0,
            "Consuming through newest token must retire older body-local metadata");

        var lifecycleBody=UUID.randomUUID();
        h.assertTrue(inbox.accept(token(lifecycleBody,support,1,1,3,fresh),fresh),
            "Fresh tracking generation token must be accepted");
        h.assertTrue(!inbox.accept(token(lifecycleBody,support,2,2,2,fresh),fresh),
            "Older tracking generation must not re-enter a live body queue");
        h.assertTrue(inbox.accept(token(lifecycleBody,support,3,3,4,fresh),fresh),
            "Newer tracking generation must replace prior body-local token history");
        h.assertTrue(inbox.retainedTokens()==1,
            "Tracking restart must discard prior-generation token metadata");

        inbox.discardSupport(support);
        h.assertTrue(inbox.retainedBodies()==0,
            "Support lifecycle teardown must release dependent token queues");

        var unloadBody=UUID.randomUUID();
        h.assertTrue(inbox.accept(token(unloadBody,UUID.randomUUID(),1,1,1,fresh),fresh),
            "Body teardown fixture must retain one token first");
        inbox.discardBody(unloadBody);
        h.assertTrue(inbox.retainedBodies()==0,
            "Body unload/removal must release non-Living controlled-vehicle token state");

        h.succeed();
    }

    private static AnatomyTransportReceiptPayload token(UUID body,UUID support,long sequence,long frame,
            long trackingGeneration,long tick) {
        return new AnatomyTransportReceiptPayload(UUID.fromString("00000000-0000-0000-0000-000000000025"),
            1,Identifier.parse("minecraft:overworld"),1,body,trackingGeneration,support,frame,sequence,tick);
    }
}
