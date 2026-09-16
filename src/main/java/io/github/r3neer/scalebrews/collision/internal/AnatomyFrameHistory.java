package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;

import java.util.Optional;

/**
 * Ordered authoritative root/joint endpoints. This is deliberately separate
 * from {@link AnatomyPoseHistory}: a new root endpoint may reuse a prior joint
 * sample, and must not cause a second pose-provider evaluation.
 */
public final class AnatomyFrameHistory {
    private AnatomyPosePayload current;
    /** Highest recipient tracking generation retired by an explicit tracking discontinuity. */
    private long retiredTrackingGeneration;

    public AnatomyPosePayload current(){return current;}
    public void clear(){current=null;retiredTrackingGeneration=0;}
    /**
     * Seals the currently accepted recipient tracking generation without discarding its ordering
     * watermark. Late packets from the retired tracking window must not resurrect material after
     * unload; only a strictly newer server-owned tracking generation can start a fresh history.
     */
    public void retireCurrentTrackingGeneration() {
        if(current!=null)retiredTrackingGeneration=Math.max(retiredTrackingGeneration,current.trackingGeneration());
    }
    public boolean accept(AnatomyPosePayload next) {
        if(next==null)throw new IllegalArgumentException("Missing causal frame");
        if(next.trackingGeneration()<=retiredTrackingGeneration)return false;
        if(current!=null) {
            if(!current.epoch().equals(next.epoch()) || current.revision()!=next.revision() || !current.dimension().equals(next.dimension())
                    || current.entityId()!=next.entityId() || !current.entity().equals(next.entity()) || !current.model().equals(next.model())
                    || !current.provider().equals(next.provider()) || !current.rootProvider().equals(next.rootProvider())
                    || current.bindingGeneration()!=next.bindingGeneration() || current.trackingGeneration()!=next.trackingGeneration())
                throw new IllegalArgumentException("Causal frame identity changed without rebind/retrack");
            if(next.frameSerial()<=current.frameSerial() || next.authorityTick()<current.authorityTick()
                    || next.jointSampleTick()<current.jointSampleTick())return false;
        }
        current=next;
        return true;
    }
    public AnatomyPoseHistory.Sample sample() {
        if(current==null)throw new IllegalStateException("No causal frame");
        return new AnatomyPoseHistory.Sample(current.inputs(),current.origin(),current.yaw(),current.scale(),new GravityFrame(current.gravity()));
    }
    public AnatomyMovement.RootFrame root() {
        if(current==null)throw new IllegalStateException("No causal frame");
        return new AnatomyMovement.RootFrame(current.rootFrameSequence(),current.rootFrameTick(),current.origin(),current.yaw(),current.scale(),new GravityFrame(current.gravity()));
    }
    public Optional<GeometryProvider.CausalEndpoint> endpoint() {
        if(current==null)return Optional.empty();
        return Optional.of(new GeometryProvider.CausalEndpoint(current.frameSerial(),current.authorityTick(),current.jointSampleTick(),root(),current.rootTransform(),sample(),
            current.available()?GeometryProvider.Availability.AVAILABLE:GeometryProvider.Availability.UNAVAILABLE));
    }
}
