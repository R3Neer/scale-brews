package io.github.r3neer.scalebrews.collision.pose;

import com.google.gson.GsonBuilder;
import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.internal.AuthorityPoseTracker;
import io.github.r3neer.scalebrews.collision.physics.SupportTransport;
import io.github.r3neer.scalebrews.collision.runtime.TransportLedger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** IMPLEMENTER: native transformed helper oracle, never copied expected walk formulas. */
public final class S62LocomotionClockTests {
    private static void check(boolean ok, String why) { if (!ok) throw new AssertionError("S62 IMPL " + why); }
    private static void exact(float actual, float expected, String why) {
        check(Float.floatToRawIntBits(actual) == Float.floatToRawIntBits(expected), why + " actual=" + actual + " expected=" + expected);
    }
    private static void rejects(Runnable op) {
        try { op.run(); throw new AssertionError("S62 IMPL illegal clock admitted"); } catch (IllegalArgumentException expected) {}
    }
    private static LivingEntity actor(GameTestHelper h, EntityType<? extends LivingEntity> type) {
        var entity = type.create(h.getLevel(), EntitySpawnReason.COMMAND);
        entity.setPos(8, 24, 8); entity.setNoGravity(true); return entity;
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static LivingPoseInputSource.Bound source(LivingEntity actor) {
        var channels = List.of(new LivingPoseInputSource.Channel("walk_position", LivingPoseInputSource.Source.WALK_POSITION),
            new LivingPoseInputSource.Channel("walk_speed", LivingPoseInputSource.Source.WALK_SPEED),
            new LivingPoseInputSource.Channel("age", LivingPoseInputSource.Source.AGE_IN_TICKS),
            new LivingPoseInputSource.Channel("scale", LivingPoseInputSource.Source.SCALE),
            new LivingPoseInputSource.Channel("age_scale", LivingPoseInputSource.Source.AGE_SCALE),
            new LivingPoseInputSource.Channel("baby", LivingPoseInputSource.Source.IS_BABY));
        return LivingPoseInputSource.bind((Class)actor.getClass(), new LivingPoseInputSource.Program(1, actor.getClass().getName(), channels));
    }
    private static void record(LivingEntity actor, long sequence, Vec3 applied) {
        TransportLedger.record(actor, new SupportTransport(actor.level().getGameTime(), sequence, 0, applied, applied));
    }
    @GameTest public void causalClockMatchesNativeTransformedBaseLivingLaw(GameTestHelper h) throws Exception {
        var rows = new ArrayList<Map<String, Object>>(); int comparisons = 0, repeated = 0;
        for (var type : List.of(EntityTypes.COW, EntityTypes.PIG)) for (boolean baby : List.of(false, true))
            for (double scale : List.of(.125, .5, 1d, 2d, 8d)) {
                var actor = actor(h, type); var reference = actor(h, type);
                try {
                    ((AgeableMob)actor).setBaby(baby); ((AgeableMob)reference).setBaby(baby);
                    actor.getAttribute(Attributes.SCALE).setBaseValue(scale); reference.getAttribute(Attributes.SCALE).setBaseValue(scale);
                    actor.walkAnimation.setSpeed(Float.NaN); // Presentation state must neither drive nor be rewritten by authority.
                    var clock = new LivingLocomotionClock(); var source = source(actor); var oracle = source(reference);
                    long sequence = 0;
                    for (int step = 0; step < 32; step++) {
                        Vec3 own = step == 0 || step % 7 == 0 ? Vec3.ZERO : new Vec3((step % 5 + 1) / 32d, 0, .0625);
                        Vec3 carry = step > 0 && step % 3 == 0 ? new Vec3(.375, .5, 0) : Vec3.ZERO;
                        actor.setPos(actor.position().add(own).add(carry));
                        reference.xo = reference.getX(); reference.yo = reference.getY(); reference.zo = reference.getZ();
                        reference.setPos(reference.position().add(own));
                        actor.tickCount = reference.tickCount = 100 + step;
                        if (carry != Vec3.ZERO) {
                            record(actor, ++sequence, new Vec3(.25, .25, 0));
                            record(actor, ++sequence, new Vec3(.125, .25, 0));
                        }
                        if (step == 0) reference.walkAnimation.stop();
                        else reference.calculateEntityAnimation(false); // Executes original helper with real ScaleGait mixin.
                        var frame = clock.advance(actor, 1000 + step, GravityFrame.VANILLA);
                        var actual = source.sample(actor, frame, 1000 + step).orElseThrow();
                        var expected = oracle.sample(reference).orElseThrow();
                        for (var entry : expected.entrySet()) { exact(actual.get(entry.getKey()), entry.getValue(), type + " step=" + step + " " + entry.getKey()); comparisons++; }
                        for (int query = 0; query < 25; query++) {
                            check(clock.advance(actor, 1000 + step, GravityFrame.VANILLA) == frame, "duplicate tick advances authority");
                            check(source.sample(actor, frame, 1000 + step).orElseThrow().equals(actual), "query changes source"); repeated++;
                        }
                        check(Float.isNaN(actor.walkAnimation.speed()), "authority rewrote presentation walk");
                        rows.add(Map.of("actor", actor.getClass().getName(), "baby", baby, "scale", scale, "step", step,
                            "own", List.of(own.x, own.y, own.z), "carry", List.of(carry.x, carry.y, carry.z),
                            "expected", expected, "actual", actual));
                    }
                } finally { TransportLedger.invalidate(actor, true); actor.discard(); reference.discard(); }
            }
        var output = FabricLoader.getInstance().getGameDir().resolve("s62-implementation/evidence.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("role", "IMPLEMENTER", "sprint", "S62",
            "oracle", "native transformed Living.calculateEntityAnimation(false), own movement only", "samples", rows,
            "bitExactScalars", comparisons, "repeatedReads", repeated)) + "\n");
        System.out.println("S62_IMPLEMENTER_NATIVE_PASS samples=" + rows.size() + " bitExactScalars=" + comparisons + " repeatedReads=" + repeated);
        h.succeed();
    }

    @GameTest public void frameLifecycleAndTangentialMovementAreExplicit(GameTestHelper h) throws Exception {
        var actor = actor(h, EntityTypes.COW); var foreign = actor(h, EntityTypes.COW);
        var reference = actor(h, EntityTypes.COW);
        try {
            var clock = new LivingLocomotionClock(); var source = source(actor);
            check(clock.current().isEmpty(), "read creates clock"); rejects(() -> clock.advance(actor, -1, GravityFrame.VANILLA));
            var initial = clock.advance(actor, 1, GravityFrame.VANILLA); actor.setPos(actor.position().add(.125, 0, 0));
            var moved = clock.advance(actor, 2, GravityFrame.VANILLA);
            check(moved.position() > 0 && !initial.validFor(actor, 1), "old frame did not retire");
            check(source.sample(actor, moved, 1).isEmpty() && source.sample(foreign, moved, 2).isEmpty(), "tick/identity admission");
            rejects(() -> clock.advance(foreign, 3, GravityFrame.VANILLA)); rejects(() -> clock.advance(actor, 1, GravityFrame.VANILLA));
            check(clock.current().orElseThrow() == moved, "rejection mutated state");
            TransportLedger.invalidate(actor, false);
            check(source.sample(actor, moved, 2).isEmpty(), "barrier cannot retire a source immediately");
            var barrier = clock.advance(actor, 2, GravityFrame.VANILLA); exact(barrier.position(), 0, "same-tick barrier reset");
            var gap = clock.advance(actor, 4, GravityFrame.VANILLA); exact(gap.speed(), 0, "gap reset");
            var west = new GravityFrame(Direction.WEST); clock.advance(actor, 4, west);
            actor.setPos(actor.position().add(.5, .125, .0625));
            reference.xo = reference.getX(); reference.yo = reference.getY(); reference.zo = reference.getZ();
            reference.setPos(reference.position().add(0, .125, .0625)); reference.calculateEntityAnimation(true);
            var lateral = clock.advance(actor, 5, west);
            exact(lateral.position(), reference.walkAnimation.position(1f), "gravity tangent native phase");
            exact(lateral.speed(), reference.walkAnimation.speed(1f), "gravity tangent native speed");
            actor.setHealth(0); exact(clock.advance(actor, 6, west).position(), 0, "dead reset"); actor.setHealth(10);
            actor.startRiding(foreign, true, true); exact(clock.advance(actor, 7, west).speed(), 0, "passenger reset"); actor.stopRiding();
            var position = LivingEntity.class.getSuperclass().getDeclaredField("position"); position.setAccessible(true);
            var finite = actor.position(); position.set(actor, new Vec3(Double.NaN, 24, 8));
            var invalid = clock.advance(actor, 8, west); check(!invalid.available() && source.sample(actor, invalid, 8).isEmpty(), "nonfinite availability");
            position.set(actor, finite); var recovery = clock.advance(actor, 9, west); exact(recovery.speed(), 0, "fresh finite baseline");
            actor.discard(); check(source.sample(actor, recovery, 9).isEmpty(), "removed frame source survives");
            System.out.println("S62_IMPLEMENTER_LIFECYCLE_PASS identity rewind sameTick generation gap lateral dead passenger nonfinite recovery removed");
        } finally { TransportLedger.invalidate(actor, true); actor.discard(); foreign.discard(); reference.discard(); }
        h.succeed();
    }

    @GameTest public void trackerUsesOneClockAndSameTickOrdinaryToggle(GameTestHelper h) {
        var actor = actor(h, EntityTypes.COW);
        try {
            actor.getAttribute(Attributes.SCALE).setBaseValue(2);
            var tracker = new AuthorityPoseTracker(); check(tracker.locomotion(actor).isEmpty(), "tracker query created clock");
            tracker.tick(actor, 10, true); actor.setPos(actor.position().add(.125, 0, 0));
            var inputs = tracker.tick(actor, 11, true); var frame = tracker.locomotion(actor).orElseThrow();
            exact(inputs.walkPhase(), frame.position(), "tracker split position"); exact(inputs.walkAmount(), frame.speed(), "tracker split speed");
            var source = source(actor); var sampled = source.sample(actor, frame, 11).orElseThrow();
            exact(sampled.get("walk_position"), inputs.walkPhase(), "reader split clock");
            var disabled = tracker.tick(actor, 11, false); check(!disabled.ordinary() && tracker.locomotion(actor).orElseThrow() == frame, "ordinary toggle advances clock");
            var enabled = tracker.tick(actor, 11, true); check(enabled.ordinary() && tracker.locomotion(actor).orElseThrow() == frame, "same tick recovery advances clock");
            TransportLedger.invalidate(actor, false); var reset = tracker.tick(actor, 11, true);
            exact(reset.walkPhase(), 0, "tracker did not observe same-tick physical barrier");
            check(source.sample(actor, frame, 11).isEmpty(), "old tracker frame accepted after barrier");
            System.out.println("S62_IMPLEMENTER_TRACKER_PASS singleOwner sharedProjection ordinaryToggle sameTickBarrier");
        } finally { TransportLedger.invalidate(actor, true); actor.discard(); }
        h.succeed();
    }
}
