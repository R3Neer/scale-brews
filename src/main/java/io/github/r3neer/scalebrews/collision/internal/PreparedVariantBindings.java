package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionVariantSources;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/** Prepared trait reads and per-participant input reuse; all selection remains catalog-owned. */
final class PreparedVariantBindings {
    static final int MAX_AXES = 32;
    enum Status { AVAILABLE, NO_MATCH, SOURCE_UNAVAILABLE, PLAN_UNAVAILABLE }
    private record Plan(WorldAnatomyCatalog.Snapshot snapshot, Identifier type, String[] keys,
                        CollisionVariantSources.Source[] sources, boolean available, String unavailableAxis) {}
    private final Map<Identifier, Plan> plans;

    PreparedVariantBindings(WorldAnatomyCatalog.Snapshot snapshot) {
        var registry = CollisionVariantSources.snapshot();
        var prepared = new HashMap<Identifier, Plan>();
        for (var entry : snapshot.catalog().snapshot().entrySet()) {
            var keys = new TreeSet<String>();
            boolean available = true;
            String unavailableAxis = null;
            outer: for (var row : entry.getValue()) for (var axis : row.variant().entrySet()) {
                keys.add(axis.getKey());
                var source = registry.get(axis.getKey());
                if (keys.size() > MAX_AXES || source == null || !source.values().contains(axis.getValue())) {
                    available = false;
                    unavailableAxis = axis.getKey();
                    break outer;
                }
            }
            var names = available ? keys.toArray(String[]::new) : new String[0];
            var sources = new CollisionVariantSources.Source[names.length];
            for (int i = 0; i < names.length; i++) sources[i] = registry.get(names[i]);
            prepared.put(entry.getKey(), new Plan(snapshot, entry.getKey(), names, sources, available, unavailableAxis));
        }
        plans = Collections.unmodifiableMap(prepared);
    }

    Cursor cursor(Identifier type) {
        var plan = plans.get(type);
        return plan == null ? null : new Cursor(plan);
    }

    static final class Cursor {
        private final Plan plan;
        private final String[] previous, scratch;
        private boolean known;
        private WorldAnatomyCatalog.Binding selected;
        private Status status;
        private String unavailableAxis;

        private Cursor(Plan plan) {
            this.plan = plan;
            previous = new String[plan.keys().length];
            scratch = new String[plan.keys().length];
            status = plan.available() ? Status.NO_MATCH : Status.PLAN_UNAVAILABLE;
            unavailableAxis = plan.unavailableAxis();
            if (plan.available() && previous.length == 0) {
                selected = enabled(plan.snapshot().resolve(plan.type(), Map.of()).orElse(null));
                known = true;
                status = selected == null ? Status.NO_MATCH : Status.AVAILABLE;
            }
        }

        /** Stable input path allocates no map and never repeats the canonical resolution. */
        WorldAnatomyCatalog.Binding read(LivingEntity entity) {
            if (!plan.available()) return null;
            boolean changed = !known;
            for (int i = 0; i < scratch.length; i++) {
                var source = plan.sources()[i];
                String value;
                try { value = source.reader().value(entity); }
                catch (RuntimeException unavailable) { value = null; }
                if (value == null || value.length() > CollisionVariantSources.MAX_VALUE_LENGTH || !source.values().contains(value)) {
                    known = false;
                    selected = null;
                    status = Status.SOURCE_UNAVAILABLE;
                    unavailableAxis = plan.keys()[i];
                    return null;
                }
                scratch[i] = value;
                if (!value.equals(previous[i])) changed = true;
            }
            if (changed) {
                var values = new TreeMap<String, String>();
                for (int i = 0; i < scratch.length; i++) {
                    previous[i] = scratch[i];
                    values.put(plan.keys()[i], scratch[i]);
                }
                selected = enabled(plan.snapshot().resolve(plan.type(), Collections.unmodifiableMap(values)).orElse(null));
                known = true;
                status = selected == null ? Status.NO_MATCH : Status.AVAILABLE;
            }
            return selected;
        }

        Status status() { return status; }
        WorldAnatomyCatalog.Binding selected() { return selected; }
        String unavailableAxis() { return unavailableAxis; }
        private static WorldAnatomyCatalog.Binding enabled(WorldAnatomyCatalog.Binding binding) {
            return binding != null && binding.selection().policy().enabled().orElse(true) ? binding : null;
        }
    }
}
