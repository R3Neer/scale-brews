package io.github.r3neer.scalebrews.collision.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Structural red-before-green holdouts for the G2 spatial-index ownership split. */
public final class S13SpatialIndexOwnershipTests {
    private static final String STATE = "io.github.r3neer.scalebrews.collision.internal.AnatomySpatialIndex";
    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
        "SPATIAL", "CELL_SIZE", "MAX_INDEX_CELLS", "MAX_INDEX_CANDIDATES", "SUPPORT_ORDER");
    private static final Set<String> FORBIDDEN_NESTED = Set.of("SpatialIndex", "FrameStamp");
    private static final String[] FORBIDDEN_OWNER_DEPENDENCIES = {
        "io/github/r3neer/scalebrews/collision/internal/AnatomyMovement",
        "io/github/r3neer/scalebrews/collision/internal/GeometryProvider",
        "io/github/r3neer/scalebrews/collision/internal/AnatomyContactState",
        "io/github/r3neer/scalebrews/platform/Platforms"
    };

    @GameTest
    public void dedicatedSpatialIndexOwnerMustExist(GameTestHelper h) {
        h.assertTrue(classExists(STATE),
            "G2 task 1 requires a dedicated AnatomySpatialIndex owner before AnatomyMovement can shed spatial membership storage");
        h.succeed();
    }

    @GameTest
    public void anatomyMovementMustNotOwnSpatialIndexStateOrDeadFrameMetadata(GameTestHelper h) {
        var remainingFields = Arrays.stream(AnatomyMovement.class.getDeclaredFields())
            .map(java.lang.reflect.Field::getName)
            .filter(FORBIDDEN_FIELDS::contains)
            .collect(Collectors.toCollection(java.util.TreeSet::new));
        var remainingNested = Arrays.stream(AnatomyMovement.class.getDeclaredClasses())
            .map(Class::getSimpleName)
            .filter(FORBIDDEN_NESTED::contains)
            .collect(Collectors.toCollection(java.util.TreeSet::new));
        h.assertTrue(remainingFields.isEmpty() && remainingNested.isEmpty(),
            "AnatomyMovement still owns spatial state/dead frame metadata: fields="
                +remainingFields+" nested="+remainingNested);
        h.succeed();
    }

    @GameTest
    public void spatialOwnerMustRemainOneWayAndPhysicsBlind(GameTestHelper h) {
        h.assertTrue(classExists(STATE), "AnatomySpatialIndex must exist before dependency direction can be audited");
        try {
            var resource="/"+STATE.replace('.', '/')+".class";
            var stream=S13SpatialIndexOwnershipTests.class.getResourceAsStream(resource);
            h.assertTrue(stream!=null,"Cannot inspect AnatomySpatialIndex bytecode");
            var bytes=stream.readAllBytes();
            var constantPoolView=new String(bytes,StandardCharsets.ISO_8859_1);
            var leaked=Arrays.stream(FORBIDDEN_OWNER_DEPENDENCIES)
                .filter(constantPoolView::contains)
                .collect(Collectors.toCollection(java.util.TreeSet::new));
            h.assertTrue(leaked.isEmpty(),
                "AnatomySpatialIndex acquired forbidden causal/physics dependencies: "+leaked);
        } catch(IOException failure) {
            throw new AssertionError("Cannot inspect AnatomySpatialIndex bytecode",failure);
        }
        h.succeed();
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name,false,S13SpatialIndexOwnershipTests.class.getClassLoader());
            return true;
        } catch(ClassNotFoundException expected) {
            return false;
        }
    }
}
