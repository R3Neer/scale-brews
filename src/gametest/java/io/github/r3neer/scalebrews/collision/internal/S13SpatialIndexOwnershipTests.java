package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.physics.MaterialBroadphase;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Structural and behavioral holdouts for the G2 spatial-index ownership split. */
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

    @GameTest
    public void spatialOwnerRejectsOversizedMembershipAndQueryWithoutFallback(GameTestHelper h) {
        var level=h.getLevel();long tick=level.getGameTime();
        var support=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
        h.assertTrue(support!=null,"S13 fixture requires a creatable living support");
        try {
            AnatomySpatialIndex.rebuild(level,tick,List.of());
            var rejected=AnatomySpatialIndex.upsertIfCurrent(support,
                new AABB(-1000,-1000,-1000,1000,1000,1000));
            h.assertTrue(rejected!=null && rejected.reason()==MaterialBroadphase.RejectionReason.ENTRY_BUDGET_EXHAUSTED,
                "Oversized same-tick membership must return an explicit bounded rejection");

            var local=AnatomySpatialIndex.queryIfCurrent(level,tick,new AABB(-2,-2,-2,2,2,2));
            h.assertTrue(local!=null && local.complete() && local.candidates().isEmpty(),
                "Rejected membership must not leak into a later local query");

            var oversized=AnatomySpatialIndex.queryIfCurrent(level,tick,
                new AABB(-10000,-10000,-10000,10000,10000,10000));
            h.assertTrue(oversized!=null && !oversized.complete() && oversized.candidates().isEmpty(),
                "Oversized query must fail bounded instead of returning a fallback candidate scan");
        } finally {
            AnatomySpatialIndex.deactivate(level);support.discard();
        }
        h.succeed();
    }

    @GameTest
    public void spatialOwnerLifecycleIsLevelLocalAndTickFenced(GameTestHelper h) {
        var firstLevel=h.getLevel();
        var secondLevel=firstLevel.getServer().getLevel(Level.NETHER);
        h.assertTrue(secondLevel!=null && secondLevel!=firstLevel,
            "S13 lifecycle fixture requires a second loaded server level");
        var first=EntityTypes.COW.create(firstLevel,EntitySpawnReason.COMMAND);
        var second=EntityTypes.COW.create(secondLevel,EntitySpawnReason.COMMAND);
        h.assertTrue(first!=null && second!=null,"S13 lifecycle fixture requires creatable supports");
        long firstTick=firstLevel.getGameTime(),secondTick=secondLevel.getGameTime();
        var firstBox=new AABB(0,0,0,1,1,1);var secondBox=new AABB(20,0,0,21,1,1);
        try {
            AnatomySpatialIndex.rebuild(firstLevel,firstTick,
                List.of(new MaterialBroadphase.Entry<>(first,firstBox)));
            AnatomySpatialIndex.rebuild(secondLevel,secondTick,
                List.of(new MaterialBroadphase.Entry<>(second,secondBox)));

            var firstQuery=AnatomySpatialIndex.queryIfCurrent(firstLevel,firstTick,firstBox);
            var secondQuery=AnatomySpatialIndex.queryIfCurrent(secondLevel,secondTick,secondBox);
            h.assertTrue(firstQuery!=null && firstQuery.candidates().equals(List.of(first))
                    && secondQuery!=null && secondQuery.candidates().equals(List.of(second)),
                "Each level must expose only the membership installed for that level");
            h.assertTrue(AnatomySpatialIndex.queryIfCurrent(firstLevel,firstTick+1,firstBox)==null,
                "A previous-tick index must never be served as current membership");

            AnatomySpatialIndex.deactivate(firstLevel);
            h.assertTrue(AnatomySpatialIndex.queryIfCurrent(firstLevel,firstTick,firstBox)==null,
                "Deactivating a level must remove only that level's spatial index");
            var survivor=AnatomySpatialIndex.queryIfCurrent(secondLevel,secondTick,secondBox);
            h.assertTrue(survivor!=null && survivor.candidates().equals(List.of(second)),
                "Deactivating one level must preserve an independent level index");
        } finally {
            AnatomySpatialIndex.deactivate(firstLevel);AnatomySpatialIndex.deactivate(secondLevel);
            first.discard();second.discard();
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
