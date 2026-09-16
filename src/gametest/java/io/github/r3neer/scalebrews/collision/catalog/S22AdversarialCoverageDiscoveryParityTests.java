package io.github.r3neer.scalebrews.collision.catalog;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/** Independent S22 holdout: attribute-backed discovery must equal the declared vanilla LivingEntity population. */
public final class S22AdversarialCoverageDiscoveryParityTests {
    @GameTest
    public void minecraftAttributeDiscoveryMatchesDeclaredLivingEntityTypes(GameTestHelper h) {
        var target = new CollisionCoverageDiscovery.Target(
            Identifier.parse("scalebrews_test:s22_vanilla_discovery_parity"),
            "26.2", Set.of("minecraft"), Map.of("minecraft", "26.2"));

        var discovered = new TreeSet<Identifier>(Comparator.comparing(Identifier::toString));
        discovered.addAll(CollisionCoverageDiscovery.discover(target).livingEntityTypes());

        var declaredLiving = new TreeSet<Identifier>(Comparator.comparing(Identifier::toString));
        for (var type : BuiltInRegistries.ENTITY_TYPE) {
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id != null && id.getNamespace().equals("minecraft")
                    && LivingEntity.class.isAssignableFrom(type.getBaseClass())) {
                declaredLiving.add(id);
            }
        }

        var missing = new TreeSet<Identifier>(Comparator.comparing(Identifier::toString));
        missing.addAll(declaredLiving);
        missing.removeAll(discovered);
        var unexpected = new TreeSet<Identifier>(Comparator.comparing(Identifier::toString));
        unexpected.addAll(discovered);
        unexpected.removeAll(declaredLiving);

        h.assertTrue(discovered.equals(declaredLiving),
            "FR-038/FR-039: attribute-backed Minecraft 26.2 discovery must equal the full declared LivingEntity population; "
                + "missing=" + missing + ", unexpected=" + unexpected);
        h.assertTrue(!discovered.isEmpty(), "Discovery parity fixture must not pass vacuously");
        h.succeed();
    }
}
