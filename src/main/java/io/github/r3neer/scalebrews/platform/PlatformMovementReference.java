package io.github.r3neer.scalebrews.platform;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class PlatformMovementReference {
    private PlatformMovementReference() {}
    public static Vec3 resolve(Entity body,Vec3 absolute) {
        var state=Platforms.state(body);
        var reference=state.pendingReference;
        state.pendingReference=null;
        if(reference==null || reference.body()!=body.getId() || !Platforms.supported(body) || !reference.finite()
            || reference.support()!=state.support.getId() || !reference.absolute().equals(absolute)) return absolute;
        // Only rebase a nearby packet onto a support the server already independently confirmed.
        var frame=PlatformGeometry.frame(state.support);
        Vec3 target=frame.world(reference.local());
        if(target.distanceTo(absolute)>4 || target.distanceTo(body.position())>4) return absolute;
        return target;
    }
}
