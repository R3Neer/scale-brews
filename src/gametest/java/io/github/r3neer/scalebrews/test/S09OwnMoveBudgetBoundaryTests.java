package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.collision.physics.TemporalResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S09 A11: live own-move budget exhaustion is observable and never exports a partial safe prefix. */
public final class S09OwnMoveBudgetBoundaryTests {
    @GameTest
    public void queryBudgetExhaustionCannotBecomePartialOwnMovement(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockServerPlayerInLevel();body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        body.setPos(support.position().add(0,2,0));
        var captured=body.getBoundingBox();
        var origin=body.position();
        var requested=new Vec3(4,-4,0);

        // The floor is hit first and leaves a non-zero safe prefix. The wall would be hit later
        // after sliding. Repeated off-path decoys consume the same real query budget without
        // changing the physical answer. Find the first count where the kernel exhausts only after
        // having made physical progress, rather than hard-coding its internal evaluation cost.
        int calibrated=-1;TemporalResponse.Result kernel=null;Map<String,ConvexBox> geometry=null;
        for(int decoys=1;decoys<=220;decoys++) {
            var candidate=geometry(captured,origin,decoys);
            var motions=motions(candidate);
            var result=TemporalResponse.resolve(captured,requested,motions,32,256);
            if(result.status()==TemporalResponse.Status.ITERATION_LIMIT && result.displacement().lengthSqr()>1e-8) {
                calibrated=decoys;kernel=result;geometry=candidate;break;
            }
        }
        h.assertTrue(calibrated>0 && kernel!=null && geometry!=null,
            "A11 fixture must dynamically find a budget boundary reached after a non-zero certified prefix");
        h.assertTrue(kernel.evaluations()==256 && kernel.displacement().lengthSqr()>1e-8,
            "A11 kernel control must consume the exact 256 budget after physical progress: "+kernel);

        final Map<String,ConvexBox> snapshotPieces=geometry;
        AnatomyMovement.activate(level);
        AnatomyMovement.register(support,ignored->Optional.of(new GeometryProvider.Snapshot(509,snapshotPieces)));
        try {
            h.assertTrue(io.github.r3neer.scalebrews.platform.Platforms.eligible(body,support),
                "A11 support must be a real live candidate");
            var beforeMetrics=AnatomyMovement.sweepMetrics(level);
            var beforePos=body.position();
            Vec3 allowed=AnatomyMovement.collide(body,requested);
            var afterMetrics=AnatomyMovement.sweepMetrics(level);

            h.assertTrue(afterMetrics.exhausted()>beforeMetrics.exhausted(),
                "A11 live wrapper must expose the same query-budget exhaustion in metrics: before="+beforeMetrics+" after="+afterMetrics);
            h.assertTrue(body.position().equals(beforePos),
                "AnatomyMovement.collide is query-only and must not mutate the entity while reporting exhaustion");
            h.assertTrue(allowed.lengthSqr()<1e-20,
                "S09 A11 forbids exporting the kernel's safe prefix as partial own movement after budget exhaustion; kernel="
                    +kernel+" liveAllowed="+allowed+" decoys="+calibrated);
        } finally {
            AnatomyMovement.clear(body);
            AnatomyMovement.deactivate(level);
            support.discard();body.discard();
        }
        h.succeed();
    }

    private static Map<String,ConvexBox> geometry(AABB body,Vec3 origin,int decoys) {
        var pieces=new LinkedHashMap<String,ConvexBox>();
        double minX=body.minX-origin.x,maxX=body.maxX-origin.x;
        double minY=body.minY-origin.y,maxY=body.maxY-origin.y;
        double minZ=body.minZ-origin.z,maxZ=body.maxZ-origin.z;
        // First contact at roughly t=.25, wide enough that the post-contact path can slide +X.
        pieces.put("00_floor",local(new AABB(minX-2,minY-1.10,minZ-2,maxX+6,minY-1.0,maxZ+2),origin));
        // Later X constraint, forcing at least one post-floor response event if budget remains.
        pieces.put("01_wall",local(new AABB(maxX+2.0,minY-4,minZ-2,maxX+2.1,maxY+2,maxZ+2),origin));
        // These are deliberately off the z=0 path but within the local query volume. Duplicate
        // geometry under distinct material piece ids is legal and makes budget cost monotonic.
        var decoy=local(new AABB(minX-1,minY-1,maxZ+2.0,maxX+4,maxY+1,maxZ+2.1),origin);
        for(int i=0;i<decoys;i++)pieces.put(String.format(java.util.Locale.ROOT,"10_decoy_%03d",i),decoy);
        return pieces;
    }

    private static Map<String,ConservativeSweep.Motion> motions(Map<String,ConvexBox> pieces) {
        var motions=new LinkedHashMap<String,ConservativeSweep.Motion>();
        for(var entry:pieces.entrySet())motions.put(entry.getKey(),new ConservativeSweep.Motion(t->entry.getValue(),0));
        return motions;
    }

    private static ConvexBox local(AABB local,Vec3 origin) {
        return ConvexBox.of(local,new Matrix4f()).move(origin);
    }
}
