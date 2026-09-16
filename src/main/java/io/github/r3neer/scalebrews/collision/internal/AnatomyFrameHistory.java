package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;

import java.util.Optional;

/**
 * Ordered authoritative root/joint endpoints. This is deliberately separate
 * from {@link AnatomyPoseHistory}: a new root endpoint may reuse a prior joint
 * sample, and must not cause a second pose-provider evaluation.
 */
public final class AnatomyFrameHistory {
    /** Receiver action for one already session-filtered pose packet. */
    public enum LifecycleTransition { CONTINUE, RESTART, REJECT }

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
    /**
     * Classifies the two independent lifecycle axes before the receiver mutates local material.
     * Binding generation identifies the server binding; tracking generation identifies this
     * recipient's visibility window. Neither axis may roll back, and an explicitly retired
     * tracking window cannot be revived merely by advancing the binding generation.
     */
    public LifecycleTransition transition(AnatomyPosePayload next) {
        if(next==null)throw new IllegalArgumentException("Missing causal frame");
        if(next.trackingGeneration()<=retiredTrackingGeneration)return LifecycleTransition.REJECT;
        if(current==null)return LifecycleTransition.CONTINUE;
        if(!current.epoch().equals(next.epoch()) || current.revision()!=next.revision() || !current.dimension().equals(next.dimension())
                || !current.entity().equals(next.entity()))return LifecycleTransition.REJECT;
        if(next.bindingGeneration()<current.bindingGeneration() || next.trackingGeneration()<current.trackingGeneration())
            return LifecycleTransition.REJECT;

        boolean bindingChanged=next.bindingGeneration()!=current.bindingGeneration();
        boolean trackingChanged=next.trackingGeneration()!=current.trackingGeneration();
        if(!bindingChanged && !trackingChanged) {
            return stableBindingIdentity(next)?LifecycleTransition.CONTINUE:LifecycleTransition.REJECT;
        }
        // A pure retrack is the same physical server binding. Network id/model/provider/root
        // therefore cannot change under tracking generation alone; replacement/rebind must
        // advance binding generation as well.
        if(!bindingChanged && !stableBindingIdentity(next))return LifecycleTransition.REJECT;
        return LifecycleTransition.RESTART;
    }
    private boolean stableBindingIdentity(AnatomyPosePayload next) {
        return current.entityId()==next.entityId() && current.model().equals(next.model())
            && current.provider().equals(next.provider()) && current.rootProvider().equals(next.rootProvider());
    }
    public boolean accept(AnatomyPosePayload next) {
        var lifecycle=transition(next);
        if(lifecycle==LifecycleTransition.REJECT)return false;
        if(lifecycle==LifecycleTransition.RESTART)
            throw new IllegalArgumentException("Causal frame lifecycle changed without receiver restart");
        if(current!=null) {
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
