package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.testutil.AdversarialSnapshotWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Non-normative SVG backfill for S09 simultaneous-contact response. */
public final class S09VisualSnapshotTests {
    @GameTest
    public void tripleContactManifoldSnapshot(GameTestHelper h) {
        var level = h.getLevel();
        var floorSupport = h.spawn(EntityTypes.COW, 2, 20, 2);
        var xSupport = h.spawn(EntityTypes.COW, 4, 20, 2);
        var zSupport = h.spawn(EntityTypes.COW, 6, 20, 2);
        for (var support : List.of(floorSupport, xSupport, zSupport)) {
            support.setNoAi(true);
            support.setNoGravity(true);
            support.getAttribute(Attributes.SCALE).setBaseValue(4);
            support.refreshDimensions();
        }
        var body = h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.2);
        body.refreshDimensions();
        body.setPos(h.absoluteVec(new Vec3(9, 22, 6)));
        var box = body.getBoundingBox();
        double gap = .2;

        double floorWidth = box.getXsize() + 4, floorDepth = box.getZsize() + 4;
        var floor = ConvexBox.of(new AABB(0, 0, 0, floorWidth, .2, floorDepth), new Matrix4f())
            .move(new Vec3(box.minX - 2, box.minY - gap - .2, box.minZ - 2));
        double wallHeight = box.getYsize() + 4;
        var xWall = ConvexBox.of(new AABB(0, 0, 0, .2, wallHeight, box.getZsize() + 4), new Matrix4f())
            .move(new Vec3(box.maxX + gap, box.minY - 2, box.minZ - 2));
        var zWall = ConvexBox.of(new AABB(0, 0, 0, box.getXsize() + 4, wallHeight, .2), new Matrix4f())
            .move(new Vec3(box.minX - 2, box.minY - 2, box.maxZ + gap));

        GeometryProvider floorProvider = ignored -> Optional.of(new GeometryProvider.Snapshot(921, Map.of("floor", floor)));
        GeometryProvider xProvider = ignored -> Optional.of(new GeometryProvider.Snapshot(922, Map.of("x_wall", xWall)));
        GeometryProvider zProvider = ignored -> Optional.of(new GeometryProvider.Snapshot(923, Map.of("z_wall", zWall)));
        AnatomyMovement.activate(level);
        AnatomyMovement.register(zSupport, zProvider);
        AnatomyMovement.register(floorSupport, floorProvider);
        AnatomyMovement.register(xSupport, xProvider);
        try {
            h.assertTrue(io.github.r3neer.scalebrews.platform.Platforms.eligible(body, floorSupport)
                    && io.github.r3neer.scalebrews.platform.Platforms.eligible(body, xSupport)
                    && io.github.r3neer.scalebrews.platform.Platforms.eligible(body, zSupport),
                "Visual S09 fixture requires all three supports in the live candidate set");
            var request = new Vec3(.4, -.4, .4);
            Vec3 moved = AnatomyMovement.collide(body, request);

            AdversarialSnapshotWriter.multicontact("s09-triple-contact.svg", "S09 triple simultaneous contact",
                box,
                List.of(
                    new AdversarialSnapshotWriter.LabeledBox("floor (+Y normal)", floor.bounds()),
                    new AdversarialSnapshotWriter.LabeledBox("x wall (-X normal)", xWall.bounds()),
                    new AdversarialSnapshotWriter.LabeledBox("z wall (-Z normal)", zWall.bounds())),
                request, moved, "allowed=" + moved);

            body.setPos(body.position().add(moved));
            AnatomyMovement.afterMove(body);
            h.assertTrue(Math.abs(moved.x - gap) < 2e-4 && Math.abs(moved.y + gap) < 2e-4 && Math.abs(moved.z - gap) < 2e-4,
                "S09 visual backfill must exercise the real equal-time three-contact response: request=" + request + " moved=" + moved);
            h.assertTrue(!floor.overlaps(body.getBoundingBox()) && !xWall.overlaps(body.getBoundingBox())
                    && !zWall.overlaps(body.getBoundingBox()),
                "S09 visual backfill must finish non-penetrating against all three simultaneous constraints");
            var contact = AnatomyMovement.contact(body);
            h.assertTrue(contact != null && contact.support() == floorSupport && contact.piece().equals("floor"),
                "S09 visual backfill must retain the canonical supporting floor after the manifold response: " + contact);
        } finally {
            AnatomyMovement.clear(body);
            AnatomyMovement.deactivate(level);
            floorSupport.discard();
            xSupport.discard();
            zSupport.discard();
            body.discard();
        }
        h.succeed();
    }
}
