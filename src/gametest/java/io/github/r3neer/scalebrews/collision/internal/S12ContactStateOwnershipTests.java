package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
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

    private static Entity body(GameTestHelper h) {
        var body = EntityTypes.ARMOR_STAND.create(h.getLevel(), EntitySpawnReason.COMMAND);
        h.assertTrue(body != null, "S12 fixture requires a creatable body entity");
        return body;
    }

    private static net.minecraft.world.entity.LivingEntity support(GameTestHelper h, int x) {
        var support = EntityTypes.COW.create(h.getLevel(), EntitySpawnReason.COMMAND);
        h.assertTrue(support != null, "S12 fixture requires a creatable support entity");
        support.setPos(h.absolutePos(new net.minecraft.core.BlockPos(x, 2, 2)).getCenter());
        return support;
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
