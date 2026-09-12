package io.github.r3neer.scalebrews.collision.internal;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Structural red-before-green holdouts for the G2 contact-state ownership split. */
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

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, S12ContactStateOwnershipTests.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException expected) {
            return false;
        }
    }
}
