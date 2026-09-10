package io.github.r3neer.scalebrews.collision.internal;

/**
 * Orders server contact publications across a tracking stop/start. Sequence is
 * local to a tracking generation, so it cannot order two generations by
 * itself. This is deliberately packet-only: it never decides physical
 * support.
 */
public final class AnatomyContactOrdering {
    private AnatomyContactOrdering() {}

    public static boolean newer(AnatomyContactPayload candidate,AnatomyContactPayload previous) {
        if(candidate==null)throw new IllegalArgumentException("Candidate contact is required");
        if(previous==null)return true;
        if(!candidate.body().equals(previous.body()))throw new IllegalArgumentException("Cannot order different contact bodies");
        if(candidate.trackingGeneration()!=previous.trackingGeneration())
            return candidate.trackingGeneration()>previous.trackingGeneration();
        return candidate.sequence()>previous.sequence();
    }
}
