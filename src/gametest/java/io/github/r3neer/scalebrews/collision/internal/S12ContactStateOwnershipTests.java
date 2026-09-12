package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Structural and behavioral holdouts for the G2 contact-state ownership split. */
public final class S12ContactStateOwnershipTests {
    private static final String STATE = "io.github.r3neer.scalebrews.collision.internal.AnatomyContactState";
    private static final Set<String> FORBIDDEN = Set.of(
        "CONTACTS", "CONTACT_SEQUENCES", "ANCHORS", "SURFACES", "SUSPENDED");

    @GameTest
    public void dedicatedContactStateOwnerMustExist(GameTestHelper h) {
        h.assertTrue(classExists(STATE),
            "G2 task 1 requires a dedicated AnatomyContactState owner before AnatomyMovement can shed contact storage");
        h.succeed();
    }

    @GameTest
    public void anatomyMovementMustNotOwnContactStorage(GameTestHelper h) {
        var remaining = Arrays.stream(AnatomyMovement.class.getDeclaredFields())
            .map(java.lang.reflect.Field::getName)
            .filter(FORBIDDEN::contains)
            .collect(Collectors.toCollection(java.util.TreeSet::new));
        h.assertTrue(remaining.isEmpty(),
            "AnatomyMovement still owns contact-state fields: " + remaining);
        h.succeed();
    }

    @GameTest
    public void contactSequenceIsStableForSameIdentityAndSurvivesClear(GameTestHelper h) {
        Entity body = body(h); var support = support(h, 2); var other = support(h, 4);
        try {
            var first = AnatomyContactState.setContact(body, support, "body", 7, new Vec3(0, 1, 0));
            var same = AnatomyContactState.setContact(body, support, "body", 7, new Vec3(1, 0, 0));
            h.assertTrue(first.sequence() == 1 && same.sequence() == 1 && AnatomyContactState.contactSequence(body) == 1,
                "Reconfirming the same support/revision/piece must not advance the contact watermark");

            var changedPiece = AnatomyContactState.setContact(body, support, "head", 7, new Vec3(0, 1, 0));
            var changedSupport = AnatomyContactState.setContact(body, other, "head", 7, new Vec3(0, 1, 0));
            h.assertTrue(changedPiece.sequence() == 2 && changedSupport.sequence() == 3,
                "Each material identity change must advance the contact watermark exactly once");

            AnatomyContactState.clear(body);
            h.assertTrue(AnatomyContactState.contact(body) == null && AnatomyContactState.contactSequence(body) == 3,
                "Clearing current contact must preserve its monotonic sequence watermark");
        } finally {
            AnatomyContactState.clear(body); body.discard(); support.discard(); other.discard();
        }
        h.succeed();
    }

    @GameTest
    public void clearRemovesContactSurfaceAndAnchorTogether(GameTestHelper h) {
        Entity body = body(h); var support = support(h, 6);
        try {
            AnatomyContactState.setContact(body, support, "body", 11, new Vec3(0, 1, 0));
            AnatomyContactState.surface(body, new SurfaceContact(support.getUUID(), 11, "body", 3,
                new Vec3(.5, 1, .5), new Vec3(0, 1, 0), h.getLevel().getGameTime()));
            AnatomyContactState.anchor(body, new AnatomyContactState.Anchor(new Vec3(.5, 1, .5), Vec3.ZERO,
                unitBox(), support.position(), body.position(), GravityFrame.VANILLA, GravityFrame.VANILLA));

            AnatomyContactState.clear(body);
            h.assertTrue(AnatomyContactState.contact(body) == null
                    && AnatomyContactState.surface(body) == null
                    && AnatomyContactState.anchor(body) == null,
                "One contact-state clear must remove current contact, surface and retained anchor atomically");
            h.assertTrue(AnatomyContactState.contactSequence(body) == 1,
                "Clearing retained contact facts must not rewind their sequence watermark");
        } finally {
            AnatomyContactState.clear(body); body.discard(); support.discard();
        }
        h.succeed();
    }

    @GameTest
    public void suspensionGenerationUsesObjectIdentityAndCleansLocally(GameTestHelper h) {
        Entity body = body(h); var first = support(h, 8); var second = support(h, 10);
        try {
            second.setId(first.getId());
            h.assertTrue(first != second && first.equals(second),
                "Fixture must reproduce vanilla network-id equality across distinct support objects");
            AnatomyContactState.suspend(body, first, 5);
            AnatomyContactState.suspend(body, second, 9);
            h.assertTrue(Long.valueOf(5).equals(AnatomyContactState.suspensionGeneration(body, first))
                    && Long.valueOf(9).equals(AnatomyContactState.suspensionGeneration(body, second)),
                "Suspension generations must be keyed by support object identity, not reusable network id");
            AnatomyContactState.clearSuspension(body, first);
            h.assertTrue(AnatomyContactState.suspensionGeneration(body, first) == null
                    && Long.valueOf(9).equals(AnatomyContactState.suspensionGeneration(body, second)),
                "Clearing one support suspension must not erase another identity-local suspension");
        } finally {
            AnatomyContactState.clearSuspension(body, first);
            AnatomyContactState.clearSuspension(body, second);
            body.discard(); first.discard(); second.discard();
        }
        h.succeed();
    }

    @GameTest
    public void suspensionExpiresOnRebindAndWhenOverlapEnds(GameTestHelper h) {
        Entity body = body(h); var support = support(h, 12);
        body.setPos(0, 2, 0);
        var snapshot = new GeometryProvider.Snapshot(1, Map.of("body", box(body.getBoundingBox())));
        GeometryProvider provider = ignored -> Optional.of(snapshot);
        try {
            AnatomyMovement.register(support, provider);
            long firstGeneration = AnatomyMovement.registrationGeneration(support);
            AnatomyContactState.suspend(body, support, firstGeneration);
            h.assertTrue(AnatomyMovement.suspended(body, support),
                "A current-generation suspension must remain while the certified shape still overlaps the body");

            AnatomyMovement.register(support, provider);
            long reboundGeneration = AnatomyMovement.registrationGeneration(support);
            h.assertTrue(reboundGeneration > firstGeneration,
                "The fixture requires a real provider rebind with a newer local generation");
            h.assertTrue(!AnatomyMovement.suspended(body, support)
                    && AnatomyContactState.suspensionGeneration(body, support) == null,
                "A suspension from the previous binding generation must not quarantine the rebound support");

            AnatomyContactState.suspend(body, support, reboundGeneration);
            body.setPos(20, 2, 0);
            h.assertTrue(!AnatomyMovement.suspended(body, support)
                    && AnatomyContactState.suspensionGeneration(body, support) == null,
                "A current suspension must release locally once the overlap that justified it is gone");
        } finally {
            AnatomyContactState.clear(body);
            AnatomyContactState.clearSuspension(body, support);
            body.discard(); support.discard();
        }
        h.succeed();
    }

    @GameTest
    public void levelCleanupClearsTargetAndStaleCrossLevelRelationsOnly(GameTestHelper h) {
        var targetLevel = h.getLevel();
        var otherLevel = targetLevel.getServer().getLevel(Level.NETHER);
        h.assertTrue(otherLevel != null && otherLevel != targetLevel,
            "S12 lifecycle fixture requires a second loaded server level");

        Entity targetBody = body(h);
        var targetSupport = support(h, 14);
        Entity survivorBody = body(h, otherLevel);
        var survivorSupport = support(h, otherLevel, 16);
        Entity transitionedBody = body(h, otherLevel);
        var oldSupport = support(h, 18);
        try {
            AnatomyMovement.register(targetSupport,
                ignored -> Optional.of(new GeometryProvider.Snapshot(1, Map.of("body", unitBox()))));
            long registration = AnatomyMovement.registrationGeneration(targetSupport);

            primeState(targetBody, targetSupport, 21, 31);
            primeState(survivorBody, survivorSupport, 22, 32);
            // Models a body whose level has already changed while a stale relation still names an old-level support.
            primeState(transitionedBody, oldSupport, 23, 33);

            AnatomyContactState.deactivate(targetLevel);

            h.assertTrue(AnatomyContactState.contact(targetBody) == null
                    && AnatomyContactState.surface(targetBody) == null
                    && AnatomyContactState.anchor(targetBody) == null
                    && AnatomyContactState.suspensionGeneration(targetBody, targetSupport) == null,
                "Deactivating a level must clear all retained contact facts owned by bodies in that level");
            h.assertTrue(AnatomyContactState.contact(transitionedBody) == null
                    && AnatomyContactState.surface(transitionedBody) == null
                    && AnatomyContactState.anchor(transitionedBody) == null
                    && AnatomyContactState.suspensionGeneration(transitionedBody, oldSupport) == null,
                "Level cleanup must also remove a stale relation whose body has already transitioned but whose support belongs to the old level");
            h.assertTrue(AnatomyContactState.contact(survivorBody) != null
                    && AnatomyContactState.surface(survivorBody) != null
                    && AnatomyContactState.anchor(survivorBody) != null
                    && Long.valueOf(32).equals(AnatomyContactState.suspensionGeneration(survivorBody, survivorSupport)),
                "Level cleanup must not erase independent contact state from another level");
            h.assertTrue(AnatomyMovement.registrationGeneration(targetSupport) == registration,
                "Contact-state lifecycle cleanup must not rewind the support registration generation");
        } finally {
            AnatomyContactState.clear(targetBody);
            AnatomyContactState.clear(survivorBody);
            AnatomyContactState.clear(transitionedBody);
            AnatomyContactState.clearSuspension(targetBody, targetSupport);
            AnatomyContactState.clearSuspension(survivorBody, survivorSupport);
            AnatomyContactState.clearSuspension(transitionedBody, oldSupport);
            targetBody.discard(); targetSupport.discard(); survivorBody.discard(); survivorSupport.discard();
            transitionedBody.discard(); oldSupport.discard();
        }
        h.succeed();
    }

    @GameTest
    public void contactStateHasNoReverseDependencyOnMovementOrchestrator(GameTestHelper h) {
        boolean reverse = Arrays.stream(AnatomyContactState.class.getDeclaredFields())
            .map(java.lang.reflect.Field::getGenericType)
            .anyMatch(type -> type.getTypeName().contains(AnatomyMovement.class.getName()));
        reverse |= Arrays.stream(AnatomyContactState.class.getDeclaredMethods())
            .anyMatch(method -> method.getGenericReturnType().getTypeName().contains(AnatomyMovement.class.getName())
                || Arrays.stream(method.getGenericParameterTypes())
                    .anyMatch(type -> type.getTypeName().contains(AnatomyMovement.class.getName())));
        for (Class<?> nested : AnatomyContactState.class.getDeclaredClasses()) {
            reverse |= Arrays.stream(nested.getDeclaredFields())
                .map(java.lang.reflect.Field::getGenericType)
                .anyMatch(type -> type.getTypeName().contains(AnatomyMovement.class.getName()));
        }
        h.assertTrue(!reverse,
            "AnatomyContactState must store validated facts without a type/callback dependency back to AnatomyMovement");
        h.succeed();
    }

    private static void primeState(Entity body, net.minecraft.world.entity.LivingEntity support, long revision, long suspension) {
        AnatomyContactState.setContact(body, support, "body", revision, new Vec3(0, 1, 0));
        AnatomyContactState.surface(body, new SurfaceContact(support.getUUID(), revision, "body", 3,
            new Vec3(.5, 1, .5), new Vec3(0, 1, 0), body.level().getGameTime()));
        AnatomyContactState.anchor(body, new AnatomyContactState.Anchor(new Vec3(.5, 1, .5), Vec3.ZERO,
            unitBox(), support.position(), body.position(), GravityFrame.VANILLA, GravityFrame.VANILLA));
        AnatomyContactState.suspend(body, support, suspension);
    }

    private static Entity body(GameTestHelper h) {
        return body(h, h.getLevel());
    }

    private static Entity body(GameTestHelper h, Level level) {
        var body = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
        h.assertTrue(body != null, "S12 fixture requires a creatable body entity");
        return body;
    }

    private static net.minecraft.world.entity.LivingEntity support(GameTestHelper h, int x) {
        return support(h, h.getLevel(), x);
    }

    private static net.minecraft.world.entity.LivingEntity support(GameTestHelper h, Level level, int x) {
        var support = EntityTypes.COW.create(level, EntitySpawnReason.COMMAND);
        h.assertTrue(support != null, "S12 fixture requires a creatable support entity");
        support.setPos(x, 2, 2);
        return support;
    }

    private static ConvexBox box(AABB bounds) {
        return new ConvexBox(List.of(
            new Vec3(bounds.minX,bounds.minY,bounds.minZ), new Vec3(bounds.maxX,bounds.minY,bounds.minZ),
            new Vec3(bounds.minX,bounds.maxY,bounds.minZ), new Vec3(bounds.maxX,bounds.maxY,bounds.minZ),
            new Vec3(bounds.minX,bounds.minY,bounds.maxZ), new Vec3(bounds.maxX,bounds.minY,bounds.maxZ),
            new Vec3(bounds.minX,bounds.maxY,bounds.maxZ), new Vec3(bounds.maxX,bounds.maxY,bounds.maxZ)));
    }

    private static ConvexBox unitBox() {
        return new ConvexBox(List.of(
            new Vec3(0,0,0), new Vec3(1,0,0), new Vec3(0,1,0), new Vec3(1,1,0),
            new Vec3(0,0,1), new Vec3(1,0,1), new Vec3(0,1,1), new Vec3(1,1,1)));
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, S12ContactStateOwnershipTests.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException expected) {
            return false;
        }
    }
}
