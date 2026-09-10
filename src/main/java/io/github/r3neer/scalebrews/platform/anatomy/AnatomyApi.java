package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * @deprecated Public collision API moved to
 * {@link io.github.r3neer.scalebrews.collision.api.AnatomyApi}. This class is a
 * source-compatibility shim while internal callers and historical proof fixtures migrate.
 */
@Deprecated(forRemoval=true)
public final class AnatomyApi {
    private AnatomyApi() {}
    public static final int PROTOCOL_VERSION=io.github.r3neer.scalebrews.collision.api.AnatomyApi.PROTOCOL_VERSION;

    public enum Capability {
        CONTACTS,
        CLEARANCE,
        RAYCAST,
        GRAVITY_FRAME,
        ROOT_TRANSPORT,
        SERVER_AUTHORITY
    }

    public static long capabilities(){return io.github.r3neer.scalebrews.collision.api.AnatomyApi.capabilities();}
    public static long mask(Capability... requested) {
        var mapped=new io.github.r3neer.scalebrews.collision.api.AnatomyApi.Capability[requested.length];
        for(int i=0;i<requested.length;i++)mapped[i]=io.github.r3neer.scalebrews.collision.api.AnatomyApi.Capability.valueOf(requested[i].name());
        return io.github.r3neer.scalebrews.collision.api.AnatomyApi.mask(mapped);
    }
    public static boolean compatible(int peerVersion,long requiredCapabilities) {
        return io.github.r3neer.scalebrews.collision.api.AnatomyApi.compatible(peerVersion,requiredCapabilities);
    }
    /** @deprecated use {@link #ownsSharedPhysics(Entity)}. */
    @Deprecated(forRemoval=true)
    public static boolean active(Entity entity){return ownsSharedPhysics(entity);}
    public static AnatomyMode mode(Entity entity){return io.github.r3neer.scalebrews.collision.api.AnatomyApi.mode(entity);}
    public static boolean ownsSharedPhysics(Entity entity){return io.github.r3neer.scalebrews.collision.api.AnatomyApi.ownsSharedPhysics(entity);}
    public static boolean ready(Entity entity){return io.github.r3neer.scalebrews.collision.api.AnatomyApi.ready(entity);}
    /** @deprecated use {@link #ready(Entity)}. */
    @Deprecated(forRemoval=true)
    public static boolean usesSharedPhysics(Entity entity){return ready(entity);}
    public static void clearContact(Entity entity){io.github.r3neer.scalebrews.collision.api.AnatomyApi.clearContact(entity);}
    public static boolean supported(Entity entity){return io.github.r3neer.scalebrews.collision.api.AnatomyApi.supported(entity);}
    public static Optional<LivingEntity> support(Entity entity){return io.github.r3neer.scalebrews.collision.api.AnatomyApi.support(entity);}
    public static boolean spaceClear(Entity entity,AABB box){return io.github.r3neer.scalebrews.collision.api.AnatomyApi.spaceClear(entity,box);}
    public static Optional<RayHit> raycast(Entity entity,Vec3 start,Vec3 end) {
        return io.github.r3neer.scalebrews.collision.api.AnatomyApi.raycast(entity,start,end)
            .map(hit->new RayHit(hit.support(),hit.contact(),hit.position(),hit.fraction()));
    }
    public static boolean attachAtContact(Entity body,RayHit hit) {
        return io.github.r3neer.scalebrews.collision.api.AnatomyApi.attachAtContact(body,
            new io.github.r3neer.scalebrews.collision.api.AnatomyApi.RayHit(hit.support(),hit.contact(),hit.position(),hit.fraction()));
    }
    public static GravityFrame gravity(Entity entity){return io.github.r3neer.scalebrews.collision.api.AnatomyApi.gravity(entity);}
    public static void installGravityAdapter(String owner,Function<Entity,Direction> resolver) {
        io.github.r3neer.scalebrews.collision.api.AnatomyApi.installGravityAdapter(owner,resolver);
    }
    public record RayHit(LivingEntity support,SurfaceContact contact,Vec3 position,double fraction) {
        public RayHit {
            if(support==null || contact==null || !support.getUUID().equals(contact.support()) || position==null || !Double.isFinite(position.lengthSqr()) || !Double.isFinite(fraction) || fraction<0 || fraction>1)
                throw new IllegalArgumentException("Invalid anatomical ray hit");
        }
    }
}
