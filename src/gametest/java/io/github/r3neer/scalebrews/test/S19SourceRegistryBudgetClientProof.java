package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.AdvancedModelBoxGeometryEngine;
import java.lang.reflect.Field;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Adversarial S19 N/N+1 and duplicate-registration budget proof. */
public final class S19SourceRegistryBudgetClientProof implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            int limit = registryLimit();
            int baseline = AdvancedModelBoxGeometryEngine.sources().size();
            if (limit <= 0 || baseline >= limit)
                throw new AssertionError("S19 source registry cannot establish isolated N/N+1 boundary: baseline="
                    + baseline + " limit=" + limit);

            var descriptor = new AdvancedModelBoxGeometryEngine.Source(Object::new, Matrix4f::new);
            Identifier sentinel = Identifier.parse("test:s19_budget_sentinel");
            if (AdvancedModelBoxGeometryEngine.sources().containsKey(sentinel))
                throw new AssertionError("S19 budget sentinel unexpectedly pre-registered");

            AdvancedModelBoxGeometryEngine.registerSource(sentinel, descriptor);
            int afterSentinel = AdvancedModelBoxGeometryEngine.sources().size();
            if (afterSentinel != baseline + 1)
                throw new AssertionError("S19 first source registration changed size incorrectly: baseline="
                    + baseline + " after=" + afterSentinel);

            try {
                AdvancedModelBoxGeometryEngine.registerSource(sentinel, descriptor);
                throw new AssertionError("S19 duplicate source registration was accepted");
            } catch (IllegalArgumentException expected) {
                if (expected.getMessage() == null || !expected.getMessage().contains("Duplicate AdvancedModelBox geometry source"))
                    throw new AssertionError("S19 duplicate registration failed for the wrong reason: " + expected, expected);
            }
            if (AdvancedModelBoxGeometryEngine.sources().size() != afterSentinel)
                throw new AssertionError("S19 rejected duplicate source consumed registry budget");

            int index = 0;
            while (AdvancedModelBoxGeometryEngine.sources().size() < limit) {
                Identifier id = Identifier.parse("test:s19_budget_" + index++);
                if (id.equals(sentinel) || AdvancedModelBoxGeometryEngine.sources().containsKey(id)) continue;
                AdvancedModelBoxGeometryEngine.registerSource(id, descriptor);
            }
            if (AdvancedModelBoxGeometryEngine.sources().size() != limit)
                throw new AssertionError("S19 source registry did not land exactly on limit " + limit);

            Identifier overflow = Identifier.parse("test:s19_budget_overflow");
            try {
                AdvancedModelBoxGeometryEngine.registerSource(overflow, descriptor);
                throw new AssertionError("S19 source registry accepted N+1 entry beyond limit " + limit);
            } catch (IllegalArgumentException expected) {
                if (expected.getMessage() == null || !expected.getMessage().contains("Too many AdvancedModelBox geometry sources"))
                    throw new AssertionError("S19 N+1 source failed for the wrong reason: " + expected, expected);
            }
            if (AdvancedModelBoxGeometryEngine.sources().size() != limit)
                throw new AssertionError("S19 rejected N+1 source mutated registry size");
            if (AdvancedModelBoxGeometryEngine.sources().containsKey(overflow))
                throw new AssertionError("S19 rejected N+1 source leaked into public registry view");

            System.out.println("S19_SOURCE_BUDGET PASS baseline=" + baseline + " limit=" + limit
                + " duplicate_no_cost=true overflow_no_cost=true");
        });
    }

    private static int registryLimit() {
        try {
            Field field = AdvancedModelBoxGeometryEngine.class.getDeclaredField("MAX_SOURCES");
            field.setAccessible(true);
            return field.getInt(null);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot inspect S19 source registry limit for boundary proof", failure);
        }
    }
}
