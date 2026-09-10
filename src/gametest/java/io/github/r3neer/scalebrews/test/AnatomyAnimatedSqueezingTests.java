package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.platform.anatomy.AnatomyFilter;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyMovement;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyTransportReceipts;
import io.github.r3neer.scalebrews.platform.anatomy.ConservativeSweep;
import io.github.r3neer.scalebrews.platform.anatomy.ConvexBox;
import io.github.r3neer.scalebrews.platform.anatomy.GeometryProvider;
import io.github.r3neer.scalebrews.platform.anatomy.GravityFrame;
import io.github.r3neer.scalebrews.platform.anatomy.HierarchyMotion;
import io.github.r3neer.scalebrews.platform.anatomy.ModelGeometry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Physical regression for a locally animated anatomical limb squeezing a body. */
public class AnatomyAnimatedSqueezingTests {
    private static final long REVISION = 41;
    // It fills the one-block tunnel's height and width only after the animated
    // x-contact.  Therefore the stone floor, ceiling and side walls deny every
    // bounded SAT exit; the open control has no such block clip.
    private static final AABB RAM = new AABB(-.9, 0, -1, .9, 1, 1);
    private static final AABB JOINT_FLOOR = new AABB(-1, 2, -1, 1, 2.1, 1);

    /**
     * This deliberately calls the core collision seam rather than Entity.move: this GameTest
     * has no runtime session/mixin path, but still exercises its real block clipping callback.
     */
    @GameTest(rotation = Rotation.NONE) public void locallyAnimatedSqueezeSuspendsOnlyTrappedPairAndReacquires(GameTestHelper h) {
        var trappedSupport = h.spawn(EntityTypes.COW, 2, 20, 2);
        var controlSupport = h.spawn(EntityTypes.COW, 2, 20, 6);
        var floorSupport = h.spawn(EntityTypes.COW, 11, 20, 8);
        for (var support : List.of(trappedSupport, controlSupport, floorSupport)) {
            support.setNoAi(true);
            support.setNoGravity(true);
        }
        var trapped = smallPlayer(h);
        var control = smallPlayer(h);
        var grounded = smallPlayer(h);
        var wall = h.absolutePos(new BlockPos(7, 20, 2));
        // Float spawn coordinates are block corners.  Put the support/body on the
        // air cell's centre, rather than initially intersecting a z-side wall.
        trappedSupport.setPos(trappedSupport.getX(), wall.getY(), wall.getZ() + .5);
        var trappedProvider = new AnimatedProvider(ramMotion(trappedSupport, wall.getX() - .55, wall.getX() + .05));
        var controlProvider = new AnimatedProvider(ramMotion(controlSupport, wall.getX() - .55, wall.getX() + .05));
        var floor = ConvexBox.of(new AABB(-1, -.1, -1, 1, 0, 1), new Matrix4f()).move(floorSupport.position());

        // GameTestHelper block positions are template-relative.  The bodies use absolute world
        // positions, derived from the same helper, so the stone tunnel is a real collision cage.
        for (int x = 5; x <= 7; x++) for (int z = 1; z <= 3; z++) {
            h.setBlock(x, 19, z, Blocks.STONE);
            h.setBlock(x, 21, z, Blocks.STONE);
        }
        for (int x = 5; x <= 7; x++) {
            h.setBlock(x, 20, 1, Blocks.STONE);
            h.setBlock(x, 20, 3, Blocks.STONE);
        }
        h.setBlock(5, 20, 2, Blocks.STONE);
        h.setBlock(7, 20, 2, Blocks.STONE);

        AnatomyMovement.activate(h.getLevel());
        AnatomyMovement.register(trappedSupport, trappedProvider);
        AnatomyMovement.register(controlSupport, controlProvider);
        AnatomyMovement.register(floorSupport, support -> Optional.of(new GeometryProvider.Snapshot(
            REVISION, Map.of("floor", floor))));
        try {
            h.assertTrue(trappedProvider.motion.linearTranslation().equals(Vec3.ZERO)
                    && trappedProvider.motion.maxPointSpeed() > .25 && trappedProvider.motion.maxPointSpeed() < 4,
                "The squeezing path is local articulation, not a root translation");
            h.assertTrue(trappedProvider.motion.at().apply(0).bounds().maxX < wall.getX()
                    && trappedProvider.motion.at().apply(1).bounds().maxX > wall.getX(),
                "The animated piece enters the wall-side body by a small amount");

            AnatomyMovement.gravity(trapped, new GravityFrame(net.minecraft.core.Direction.WEST));
            double trappedHalfWidth = trapped.getBoundingBox().getXsize() / 2;
            trapped.setPos(wall.getX() - trappedHalfWidth - .001, wall.getY(), trappedSupport.getZ());
            float health = trapped.getHealth();
            double penetration = trappedProvider.motion.at().apply(1).bounds().maxX - trapped.getBoundingBox().minX;
            h.assertTrue(penetration > 0 && penetration < .2,
                "Local animation creates a small finite penetration against the real wall");
            h.assertTrue(trapped.getBoundingBox().maxX < wall.getX()
                    && h.getLevel().noCollision(trapped, trapped.getBoundingBox()),
                "The trapped body starts strictly in the air cell before the animated squeeze");

            // Control: identical hierarchy motion in open space resolves within the bounded
            // separation radius rather than entering the per-pair suspension state.
            double controlHalfWidth = control.getBoundingBox().getXsize() / 2;
            control.setPos(wall.getX() - controlHalfWidth - .001, wall.getY(), controlSupport.getZ());
            AnatomyMovement.gravity(control, new GravityFrame(net.minecraft.core.Direction.WEST));
            Vec3 free = AnatomyMovement.collide(control, Vec3.ZERO);
            control.setPos(control.position().add(free));
            h.assertTrue(!AnatomyMovement.suspended(control, controlSupport)
                    && !controlProvider.motion.at().apply(1).overlaps(control.getBoundingBox())
                    && free.length() > 1e-5 && free.length() <= 4,
                "Without the wall the same animated overlap obtains a bounded safe separation");

            // A separate material floor/body pair is acquired through collide + afterMove and
            // must remain a real support while the trapped pair is being suspended.
            grounded.setPos(floorSupport.getX(), floor.bounds().maxY + .5, floorSupport.getZ());
            Vec3 landing = AnatomyMovement.collide(grounded, new Vec3(0, -1, 0));
            grounded.setPos(grounded.position().add(landing));
            AnatomyMovement.afterMove(grounded);
            h.assertTrue(AnatomyMovement.supported(grounded) && AnatomyMovement.surface(grounded) != null,
                "Independent body acquires its own material support before the squeeze");

            h.assertBlockPresent(Blocks.STONE, 7, 20, 2);
            Vec3 wallClip = net.minecraft.world.entity.Entity.collideBoundingBox(trapped, new Vec3(.5, 0, 0),
                trapped.getBoundingBox(), h.getLevel(), h.getLevel().getEntityCollisions(trapped, trapped.getBoundingBox().expandTowards(.5, 0, 0)));
            h.assertTrue(wallClip.x < .01, "Template-relative stone wall blocks the intended world-side squeeze: clip="
                    + wallClip + ", box=" + trapped.getBoundingBox() + ", wall=" + wall + ", state="
                    + h.getLevel().getBlockState(wall) + ", noPhysics=" + trapped.noPhysics + ", rotation=" + h.getTestRotation());
            Vec3 blocked = AnatomyMovement.collide(trapped, Vec3.ZERO);
            trapped.setPos(trapped.position().add(blocked));
            long retiredSequence = AnatomyMovement.contactSequence(trapped);
            boolean trappedPair = AnatomyMovement.suspended(trapped, trappedSupport);
            boolean unrelatedPair = AnatomyMovement.suspended(trapped, floorSupport);
            h.assertTrue(trappedPair, "Animated endpoint remains overlapping after real block clip: blocked=" + blocked
                    + ", endpoint=" + trappedProvider.motion.at().apply(1).bounds() + ", body=" + trapped.getBoundingBox());
            h.assertTrue(!unrelatedPair, "Unrelated floor support must not enter the squeezed pair suspension set");
            h.assertTrue(blocked.length() < .01 && trapped.getHealth() == health
                    && AnatomyMovement.contact(trapped) == null && AnatomyMovement.surface(trapped) == null,
                "The blocked squeeze neither teleports/damages the body nor retains an old anchor");
            h.assertTrue(AnatomyMovement.supported(grounded)
                    && !AnatomyMovement.suspended(grounded, floorSupport)
                    && AnatomyMovement.contact(grounded).support() == floorSupport,
                "The unrelated supported pair remains physically supported during suspension");

            // Retreat uses another real local motion interval, still revision 41 and with the
            // root fixed.  It releases only after the endpoint no longer overlaps the body.
            trappedProvider.motion = ramMotion(trappedSupport, wall.getX() + .05, wall.getX() - .65);
            h.assertTrue(trappedProvider.snapshot(trappedSupport).revision() == REVISION
                    && trappedProvider.motion.linearTranslation().equals(Vec3.ZERO)
                    && trappedProvider.motion.maxPointSpeed() < 4,
                "Retreat publishes the same-revision local trajectory with a bounded per-tick motion");
            h.assertTrue(!AnatomyMovement.suspended(trapped, trappedSupport)
                    && !trappedProvider.motion.at().apply(1).overlaps(trapped.getBoundingBox()),
                "Pair leaves suspension only when the real animated endpoint has separated");

            // The old contact remains cleared. This legacy provider exposes only the current
            // endpoint, so the 1 mm gap is deliberately larger than CCD skin: a zero body
            // move must not invent an anchor. Q2 still needs a separate runtime test where a
            // newly published animated interval itself reacquires contact.
            Vec3 escape = AnatomyMovement.collide(trapped, new Vec3(-.45, 0, 0));
            trapped.setPos(trapped.position().add(escape));
            double contactMaxX = trapped.getBoundingBox().minX - .001;
            trappedProvider.motion = ramMotion(trappedSupport, wall.getX() - .65, contactMaxX);
            var endpoint = trappedProvider.motion.at().apply(1);
            double gap = trapped.getBoundingBox().minX - endpoint.bounds().maxX;
            h.assertTrue(gap > ConservativeSweep.SKIN && !endpoint.overlaps(trapped.getBoundingBox()),
                "Endpoint stays outside the body by more than CCD skin before own movement: gap=" + gap
                    + ", skin=" + ConservativeSweep.SKIN + ", endpoint=" + endpoint.bounds()
                    + ", body=" + trapped.getBoundingBox());
            Vec3 zero = AnatomyMovement.collide(trapped, Vec3.ZERO);
            trapped.setPos(trapped.position().add(zero));
            AnatomyMovement.afterMove(trapped);
            h.assertTrue(zero.lengthSqr() <= ConservativeSweep.SKIN * ConservativeSweep.SKIN
                    && !AnatomyMovement.supported(trapped) && AnatomyMovement.contact(trapped) == null
                    && AnatomyMovement.surface(trapped) == null && AnatomyMovement.contactSequence(trapped) == retiredSequence,
                "A separated endpoint and zero body movement must not magnetize a new contact: gap=" + gap
                    + ", zero=" + zero + ", contact=" + AnatomyMovement.contact(trapped));
            // A real WEST gravity-relative body move crosses the exposed +X material face.
            Vec3 reacquire = AnatomyMovement.collide(trapped, new Vec3(-.02, 0, 0));
            trapped.setPos(trapped.position().add(reacquire));
            AnatomyMovement.afterMove(trapped);
            h.assertTrue(AnatomyMovement.supported(trapped)
                    && AnatomyMovement.contact(trapped).support() == trappedSupport
                    && AnatomyMovement.contactSequence(trapped) > retiredSequence
                    && reacquire.x < -ConservativeSweep.SKIN,
                "A later real WEST body move acquires a new material contact instead of reviving the old anchor: escape="
                    + escape + ", reacquire=" + reacquire + ", endpoint=" + trappedProvider.motion.at().apply(1).bounds()
                    + ", body=" + trapped.getBoundingBox() + ", contact=" + AnatomyMovement.contact(trapped)
                    + ", surface=" + AnatomyMovement.surface(trapped) + ", retiredSequence=" + retiredSequence);
        } finally {
            AnatomyMovement.deactivate(h.getLevel());
            for (var entity : List.of(trappedSupport, controlSupport, floorSupport, trapped, control, grounded)) entity.discard();
        }
        h.succeed();
    }

    /** Two real local-joint segments can contribute during one root-stationary tick. */
    @GameTest public void twoLocalJointContributionsKeepDistinctTransportProvenance(GameTestHelper h) {
        var support = h.spawn(EntityTypes.COW, 2, 20, 8);
        support.setNoAi(true);
        support.setNoGravity(true);
        var body = smallServerPlayer(h);
        var initialMaxX = support.getX() + JOINT_FLOOR.maxX;
        var provider = new AnimatedProvider(jointFloorMotion(support, initialMaxX, initialMaxX));
        AnatomyMovement.activate(h.getLevel());
        AnatomyMovement.register(support, provider);
        try {
            var floor = provider.motion.at().apply(1);
            body.setPos(support.getX(), floor.bounds().maxY + .4, support.getZ());
            Vec3 landing = AnatomyMovement.collide(body, new Vec3(0, -.8, 0));
            body.setPos(body.position().add(landing));
            AnatomyMovement.afterMove(body);
            h.assertTrue(AnatomyMovement.supported(body), "Body obtains a real material anchor before joint transport");
            Vec3 before = body.position();
            Vec3 rootOrigin = support.position();

            // The support root never moves.  Each endpoint comes from a different actual
            // child transform in HierarchyMotion, so carry consumes two joint segments.
            long duplicates = AnatomyTransportReceipts.metrics().duplicates();
            provider.motion = jointFloorMotion(support, initialMaxX, initialMaxX + .2);
            h.assertTrue(provider.motion.linearTranslation().equals(Vec3.ZERO), "First contribution is local joint motion");
            AnatomyMovement.carry(body);
            var first = AnatomyMovement.transport(body);
            provider.motion = jointFloorMotion(support, initialMaxX + .2, initialMaxX + .4);
            AnatomyMovement.carry(body);
            var second = AnatomyMovement.transport(body);
            var receipts = AnatomyTransportReceipts.history(body, body.getUUID());

            h.assertTrue(support.position().equals(rootOrigin)
                    && first != null && second != null && first.sequence() + 1 == second.sequence()
                    && first.rootFrameSequence() == second.rootFrameSequence()
                    && first.appliedDelta().distanceTo(new Vec3(.2, 0, 0)) < 1e-5
                    && second.appliedDelta().distanceTo(new Vec3(.2, 0, 0)) < 1e-5
                    && second.displacement().distanceTo(new Vec3(.4, 0, 0)) < 1e-5
                    && body.position().subtract(before).distanceTo(new Vec3(.4, 0, 0)) < 1e-5,
                "Two joint contributions retain separate sequences while the tick aggregate remains exact");
            h.assertTrue(receipts.size() == 2
                    && receipts.getFirst().transportSequence() + 1 == receipts.getLast().transportSequence()
                    && receipts.getFirst().contactSequence() == receipts.getLast().contactSequence()
                    && receipts.getFirst().rootFrameSequence() == receipts.getLast().rootFrameSequence()
                    && !receipts.getFirst().materialBefore().equals(receipts.getFirst().materialAfter())
                    && !receipts.getLast().materialBefore().equals(receipts.getLast().materialAfter())
                    && AnatomyTransportReceipts.metrics().duplicates() == duplicates,
                "Real post-baseline joint carries retain two distinct material receipts without synthetic record calls");
        } finally {
            AnatomyMovement.deactivate(h.getLevel());
            support.discard();
            body.discard();
        }
        h.succeed();
    }

    private static Player smallPlayer(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);
        player.refreshDimensions();
        return player;
    }

    private static ServerPlayer smallServerPlayer(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);
        player.refreshDimensions();
        return player;
    }

    private static ConservativeSweep.Motion ramMotion(LivingEntity support, double fromMaxX, double toMaxX) {
        var hierarchy = new HierarchyMotion(ramModel(),
            Map.of("ram", new Matrix4f().translation((float) (fromMaxX - RAM.maxX - support.getX()), 0, 0)),
            Map.of("ram", new Matrix4f().translation((float) (toMaxX - RAM.maxX - support.getX()), 0, 0)),
            new Matrix4f(), new Matrix4f(), support.position(), support.position(), AnatomyFilter.DEFAULT);
        return hierarchy.pieces().get("ram");
    }

    private static ConservativeSweep.Motion jointFloorMotion(LivingEntity support, double fromMaxX, double toMaxX) {
        var hierarchy = new HierarchyMotion(jointFloorModel(),
            Map.of("ram", new Matrix4f().translation((float) (fromMaxX - JOINT_FLOOR.maxX - support.getX()), 0, 0)),
            Map.of("ram", new Matrix4f().translation((float) (toMaxX - JOINT_FLOOR.maxX - support.getX()), 0, 0)),
            new Matrix4f(), new Matrix4f(), support.position(), support.position(), AnatomyFilter.DEFAULT);
        return hierarchy.pieces().get("ram");
    }

    private static ModelGeometry ramModel() {
        return new ModelGeometry(2, "test:animated_squeeze", "1", List.of(
            new ModelGeometry.Part("root", null, ModelGeometry.values(new Matrix4f())),
            new ModelGeometry.Part("ram", "root", ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("ram", "ram", List.of(RAM.minX, RAM.minY, RAM.minZ),
                List.of(RAM.maxX, RAM.maxY, RAM.maxZ), null)), ModelGeometry.values(new Matrix4f()));
    }

    private static ModelGeometry jointFloorModel() {
        return new ModelGeometry(2, "test:joint_floor", "1", List.of(
            new ModelGeometry.Part("root", null, ModelGeometry.values(new Matrix4f())),
            new ModelGeometry.Part("ram", "root", ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("ram", "ram", List.of(JOINT_FLOOR.minX, JOINT_FLOOR.minY, JOINT_FLOOR.minZ),
                List.of(JOINT_FLOOR.maxX, JOINT_FLOOR.maxY, JOINT_FLOOR.maxZ), null)), ModelGeometry.values(new Matrix4f()));
    }

    private static final class AnimatedProvider implements GeometryProvider {
        private ConservativeSweep.Motion motion;
        private AnimatedProvider(ConservativeSweep.Motion motion) { this.motion = motion; }
        @Override public Optional<Snapshot> sample(LivingEntity support) {
            return Optional.of(new Snapshot(REVISION, Map.of("ram", motion.at().apply(1))));
        }
        @Override public Optional<MotionSnapshot> motion(LivingEntity support) {
            long to = support.level().getGameTime();
            return Optional.of(new MotionSnapshot(REVISION, to - 1, to, support.position(), support.position(), Map.of("ram", motion)));
        }
        private MotionSnapshot snapshot(LivingEntity support) { return motion(support).orElseThrow(); }
    }
}
