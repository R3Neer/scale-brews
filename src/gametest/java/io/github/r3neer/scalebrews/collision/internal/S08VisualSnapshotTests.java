package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import io.github.r3neer.scalebrews.platform.PlatformPhysics;
import io.github.r3neer.scalebrews.testutil.AdversarialSnapshotWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Non-normative SVG backfill for S08's certified curved carry path. Physical asserts still decide pass/fail. */
public final class S08VisualSnapshotTests {
    private static final PoseProvider.Inputs INPUTS = new PoseProvider.Inputs(0, 0, 0, 0, 0, true);

    @GameTest
    public void curvedPathVsChordBlockSnapshot(GameTestHelper h) {
        run(h, false);
        h.succeed();
    }

    @GameTest
    public void curvedPathVsChordEntitySnapshot(GameTestHelper h) {
        run(h, true);
        h.succeed();
    }

    private static void run(GameTestHelper h, boolean entityObstacle) {
        var level = h.getLevel();
        var support = h.spawn(EntityTypes.COW, 2, 20, 2);
        support.setNoAi(true);
        support.setNoGravity(true);
        var body = h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.1);
        body.refreshDimensions();

        BlockPos blockRelative = new BlockPos(6, 40, 4);
        net.minecraft.world.entity.LivingEntity obstacleEntity = null;
        AABB obstacleBox;
        if (entityObstacle) {
            obstacleEntity = h.spawn(EntityTypes.SHULKER, 6, 40, 4);
            obstacleEntity.setNoAi(true);
            obstacleEntity.setNoGravity(true);
            Vec3 cell = h.absoluteVec(new Vec3(6, 40, 4));
            obstacleEntity.setPos(cell.x + .5, cell.y, cell.z + .5);
            obstacleBox = obstacleEntity.getBoundingBox();
        } else {
            h.setBlock(blockRelative, Blocks.STONE);
            Vec3 blockMin = h.absoluteVec(new Vec3(blockRelative.getX(), blockRelative.getY(), blockRelative.getZ()));
            obstacleBox = new AABB(blockMin.x, blockMin.y, blockMin.z, blockMin.x + 1, blockMin.y + 1, blockMin.z + 1);
        }

        try {
            double halfWidth = body.getBoundingBox().getXsize() * .5;
            double initialGap = ConservativeSweep.SKIN * .5;
            Vec3 start = new Vec3(obstacleBox.getCenter().x, obstacleBox.minY, obstacleBox.minZ - halfWidth - initialGap);
            body.setPos(start);
            AABB captured = body.getBoundingBox();
            Vec3 pivot = new Vec3(start.x - 2, start.y, start.z);
            double floorHalf = .01;
            var floorLocal = ConvexBox.of(new AABB(0, 0, 0, floorHalf * 2, .20, floorHalf * 2), new Matrix4f());
            var floorBefore = floorLocal.move(new Vec3(start.x - floorHalf, captured.minY - .20, start.z - floorHalf));
            var floorAfter = rotateY(floorBefore, pivot, Math.PI);
            double maxRadius = floorBefore.vertices().stream().mapToDouble(v -> Math.hypot(v.x - pivot.x, v.z - pivot.z)).max().orElseThrow();
            var floorMotion = new ConservativeSweep.Motion(t -> rotateY(floorBefore, pivot, Math.PI * t), maxRadius * Math.PI, Vec3.ZERO);
            long revision = entityObstacle ? 812 : 811;

            AnatomyMovement.activate(level);
            AnatomyMovement.register(support, ignored -> java.util.Optional.of(new GeometryProvider.Snapshot(revision, Map.of("floor", floorBefore))));
            try {
                var separation = floorBefore.separation(captured);
                int face = floorBefore.closestFace(separation.normal());
                Vec3 normal = floorBefore.faceNormal(face);
                h.assertTrue(GravityFrame.VANILLA.supports(normal), "Visual S08 fixture must begin on a supporting floor");
                Vec3 local = floorBefore.facePoint(face, captured.getCenter());
                h.assertTrue(AnatomyMovement.confirm(body, support,
                    new SurfaceContact(support.getUUID(), revision, "floor", face, local, normal, level.getGameTime())),
                    "Visual S08 fixture must establish the retained anchor");

                Vec3 endpoint = new Vec3(-4, 0, 0);
                Vec3 chordAllowed = clip(level, body, captured, endpoint);
                h.assertTrue(chordAllowed.distanceToSqr(endpoint) < 1e-10,
                    "Visual S08 control requires a clear straight endpoint chord: " + chordAllowed);
                double midT = .05, angle = Math.PI * midT;
                Vec3 midDisplacement = new Vec3(2 * Math.cos(angle) - 2, 0, 2 * Math.sin(angle));
                h.assertTrue(obstacleBox.intersects(captured.move(midDisplacement)),
                    "Visual S08 control requires an obstacle only on the certified arc interior");
                h.assertTrue(!obstacleBox.intersects(captured.move(endpoint)),
                    "Visual S08 endpoint must remain obstacle-clear");

                var event = event(level, support, pivot, floorBefore, floorAfter, floorMotion, revision,
                    entityObstacle ? "visual_entity" : "visual_block");
                var handle = event.interval().handle();
                var motion = new GeometryProvider.MotionSnapshot(revision, level.getGameTime(), level.getGameTime(), pivot, pivot,
                    Map.of("floor", floorMotion));
                var result = AnchoredTransportPlanner.plan(level, body, captured, List.of(event), Map.of(handle, motion),
                    (box, requested) -> clip(level, body, box, requested));

                var samples = new ArrayList<Vec3>();
                for (int i = 0; i <= 20; i++) {
                    double t = i / 20d;
                    samples.add(new Vec3(2 * Math.cos(Math.PI * t) - 2, 0, 2 * Math.sin(Math.PI * t)));
                }
                AdversarialSnapshotWriter.curvedPathXZ(
                    entityObstacle ? "s08-curved-entity.svg" : "s08-curved-block.svg",
                    entityObstacle ? "S08 curved carry vs entity" : "S08 curved carry vs block",
                    captured, samples, endpoint,
                    List.of(new AdversarialSnapshotWriter.LabeledBox(entityObstacle ? "solid entity" : "stone block", obstacleBox)),
                    "planner=" + result.status());

                h.assertTrue(result.status() == AnchoredTransportPlanner.Status.RELEASE,
                    "S08 visual backfill must exercise the real FR-057 release path, got " + result.status());
            } finally {
                AnatomyMovement.clear(body);
                AnatomyMovement.deactivate(level);
            }
        } finally {
            support.discard();
            body.discard();
            if (obstacleEntity != null) obstacleEntity.discard();
            if (!entityObstacle) h.setBlock(blockRelative, Blocks.AIR);
        }
    }

    private static MaterialEventDispatcher.Event<net.minecraft.world.entity.Entity> event(
            net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.LivingEntity support, Vec3 pivot,
            ConvexBox floorBefore, ConvexBox floorAfter, ConservativeSweep.Motion floorMotion, long revision, String suffix) {
        long tick = level.getGameTime();
        long registration = AnatomyMovement.registrationGeneration(support);
        var identity = new GeometryProvider.GeometryIdentity(level.dimension(), support.getUUID(), support.getId(), UUID.randomUUID(), revision,
            Identifier.parse("test:s08_" + suffix), Identifier.parse("test:s08_" + suffix + "_pose"), 1, registration);
        var beforeRoot = new AnatomyMovement.RootFrame(1, tick, pivot, 0, 1, GravityFrame.VANILLA);
        var afterRoot = new AnatomyMovement.RootFrame(2, tick, pivot, 180, 1, GravityFrame.VANILLA);
        var beforeSample = new AnatomyPoseHistory.Sample(INPUTS, pivot, 0, 1, GravityFrame.VANILLA);
        var afterSample = new AnatomyPoseHistory.Sample(INPUTS, pivot, 180, 1, GravityFrame.VANILLA);
        var before = new GeometryProvider.QueryFrame(identity,
            new GeometryProvider.CausalEndpoint(1, tick, tick, beforeRoot, beforeSample, GeometryProvider.Availability.AVAILABLE),
            new GeometryProvider.Snapshot(revision, Map.of("floor", floorBefore)));
        var after = new GeometryProvider.QueryFrame(identity,
            new GeometryProvider.CausalEndpoint(2, tick, tick, afterRoot, afterSample, GeometryProvider.Availability.AVAILABLE),
            new GeometryProvider.Snapshot(revision, Map.of("floor", floorAfter)));
        var handle = new GeometryProvider.MotionIntervalHandle(identity, 1, before, after);
        double maxRadius = floorBefore.vertices().stream().mapToDouble(v -> Math.hypot(v.x - pivot.x, v.z - pivot.z)).max().orElseThrow();
        var envelope = floorBefore.bounds().inflate(maxRadius * 2 + 1);
        var interval = new MaterialEventDispatcher.MaterialInterval(handle, envelope);
        return new MaterialEventDispatcher.Event<>(new MaterialEventDispatcher.EventId(1, 0), support,
            MaterialEventDispatcher.Source.ROOT_MUTATION, interval, Set.of(support.getUUID()));
    }

    private static Vec3 clip(net.minecraft.server.level.ServerLevel level, Entity body, AABB box, Vec3 requested) {
        Entity entered = PlatformPhysics.enter(body);
        try {
            return Entity.collideBoundingBox(body, requested, box, level, level.getEntityCollisions(body, box.expandTowards(requested)));
        } finally {
            PlatformPhysics.exit(entered);
        }
    }

    private static ConvexBox rotateY(ConvexBox box, Vec3 pivot, double radians) {
        double sin = Math.sin(radians), cos = Math.cos(radians);
        return new ConvexBox(box.vertices().stream().map(v -> {
            double x = v.x - pivot.x, z = v.z - pivot.z;
            return new Vec3(pivot.x + x * cos - z * sin, v.y, pivot.z + x * sin + z * cos);
        }).toList());
    }
}
