package io.github.r3neer.scalebrews.collision.internal;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Bounded client-side cache of server-issued transport receipt tokens.
 *
 * <p>Tokens are capability metadata only. This inbox decides retention and when a server token
 * has become locally incorporable; the server remains the sole authority that can claim it.</p>
 */
public final class AnatomyReceiptTokenInbox {
    public static final int MAX_BODIES=16;
    public static final int MAX_TOKENS_PER_BODY=128;

    private final Map<UUID,ArrayDeque<AnatomyTransportReceiptPayload>> tokens=new HashMap<>();

    public boolean accept(AnatomyTransportReceiptPayload packet,long now) {
        if(packet==null || now<0)throw new IllegalArgumentException("Invalid receipt token input");
        if(expired(packet,now))return false;
        prune(now);
        var queue=tokens.get(packet.body());
        if(queue==null) {
            if(tokens.size()>=MAX_BODIES)return false;
            queue=new ArrayDeque<>();
            tokens.put(packet.body(),queue);
        }
        long generation=queue.isEmpty()?0:queue.peekLast().trackingGeneration();
        if(packet.trackingGeneration()<generation)return false;
        if(packet.trackingGeneration()>generation)queue.clear();
        for(var known:queue)if(known.receiptSequence()==packet.receiptSequence())return false;
        if(queue.size()>=MAX_TOKENS_PER_BODY)queue.removeFirst();
        queue.addLast(packet);
        return true;
    }

    /**
     * Consumes the newest server token whose support endpoint has already been incorporated locally.
     * The frame comparison is only a client-side readiness test; receiptSequence remains server authority.
     */
    public Optional<AnatomyTransportReceiptPayload> consumeIncorporated(UUID body,UUID support,long supportFrameSerial,long now) {
        if(body==null || support==null || supportFrameSerial<1 || now<0)
            throw new IllegalArgumentException("Invalid incorporated receipt query");
        prune(now);
        var queue=tokens.get(body);if(queue==null)return Optional.empty();
        AnatomyTransportReceiptPayload chosen=null;
        for(var token:queue) {
            if(!token.support().equals(support) || token.supportFrameSerial()>supportFrameSerial)continue;
            if(chosen==null || token.receiptSequence()>chosen.receiptSequence())chosen=token;
        }
        if(chosen==null)return Optional.empty();
        long consumedThrough=chosen.receiptSequence();
        queue.removeIf(token->token.receiptSequence()<=consumedThrough);
        if(queue.isEmpty())tokens.remove(body);
        return Optional.of(chosen);
    }

    /** Explicit body lifecycle barrier, including non-Living controlled vehicles. */
    public void discardBody(UUID body) {
        if(body!=null)tokens.remove(body);
    }

    /** Support unload/unavailability barrier; removes only tokens that depend on that support. */
    public void discardSupport(UUID support) {
        if(support==null)return;
        var iterator=tokens.entrySet().iterator();
        while(iterator.hasNext()) {
            var queue=iterator.next().getValue();
            queue.removeIf(token->support.equals(token.support()));
            if(queue.isEmpty())iterator.remove();
        }
    }

    /** World-tick TTL pruning; stale bodies disappear even if they remain loaded and never carry again. */
    public void prune(long now) {
        if(now<0)throw new IllegalArgumentException("Invalid receipt-token clock");
        var iterator=tokens.entrySet().iterator();
        while(iterator.hasNext()) {
            var queue=iterator.next().getValue();
            while(!queue.isEmpty() && expired(queue.peekFirst(),now))queue.removeFirst();
            if(queue.isEmpty())iterator.remove();
        }
    }

    public void clear(){tokens.clear();}

    int retainedBodies(){return tokens.size();}
    int retainedTokens(){int count=0;for(var queue:tokens.values())count+=queue.size();return count;}

    private static boolean expired(AnatomyTransportReceiptPayload token,long now) {
        return token.tick()<now && now-token.tick()>=AnatomyTransportReceipts.HISTORY_TICKS;
    }
}
