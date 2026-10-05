package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.catalog.CollisionBindingCatalog;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;

/** Revision-owned executable lookup; declarative selection remains exclusively catalog-owned. */
public final class PreparedBindingIndex {
    private final CollisionBindingCatalog catalog;
    private final Map<CollisionBinding, WorldAnatomyCatalog.Binding> selections;
    private final Map<WorldAnatomyCatalog.Binding, Identifier> identities;
    private final Map<Identifier, WorldAnatomyCatalog.Binding> byIdentity;

    /** PREPARATION: accepts only prepared bindings belonging to this exact canonical catalog. */
    public PreparedBindingIndex(CollisionBindingCatalog catalog, Collection<WorldAnatomyCatalog.Binding> prepared) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(prepared, "prepared");
        if (prepared.size() > 4096) throw new IllegalArgumentException("Too many prepared collision bindings");
        var canonical = new HashMap<CollisionBinding, CollisionBinding>();
        var canonicalIds = new IdentityHashMap<CollisionBinding, Identifier>();
        for (var selection : catalog.bindings()) {
            canonical.put(selection, selection);
            // Catalog-only rows cannot bypass complete text/representation validation.
            // This does not make them executable and these temporary IDs are not retained.
            canonicalIds.put(selection, CollisionBindingIdentity.of(selection));
        }
        var selected = new IdentityHashMap<CollisionBinding, WorldAnatomyCatalog.Binding>();
        var ids = new IdentityHashMap<WorldAnatomyCatalog.Binding, Identifier>();
        var indexed = new TreeMap<Identifier, WorldAnatomyCatalog.Binding>(Comparator.comparing(Identifier::toString));
        for (var binding : prepared) {
            if (binding == null) throw new IllegalArgumentException("Null prepared collision binding");
            var owned = canonical.get(binding.selection());
            if (owned == null) throw new IllegalArgumentException("Prepared binding is outside canonical catalog");
            if (selected.put(owned, binding) != null) throw new IllegalArgumentException("Duplicate prepared binding selection");
            var id = canonicalIds.get(owned);
            if (indexed.putIfAbsent(id, binding) != null) throw new IllegalArgumentException("Conflicting prepared binding identity");
            ids.put(binding, id);
        }
        selections = Collections.unmodifiableMap(selected);
        identities = Collections.unmodifiableMap(ids);
        byIdentity = Collections.unmodifiableMap(new java.util.LinkedHashMap<>(indexed));
    }

    boolean owns(CollisionBindingCatalog canonical) { return catalog == canonical; }
    public int size() { return selections.size(); }

    /** No compilation or record hashing. An unavailable most-specific row never falls back. */
    public Optional<WorldAnatomyCatalog.Binding> resolve(Identifier entity, Map<String, String> variant) {
        return catalog.resolve(entity, variant).map(selections::get);
    }

    /** Exact catalog member and entity ownership; does not infer current entity-state selection. */
    public Optional<WorldAnatomyCatalog.Binding> resolveIdentity(Identifier entity, Identifier identity) {
        if (entity == null || identity == null) return Optional.empty();
        var binding = byIdentity.get(identity);
        return binding != null && entity.equals(binding.selection().entity()) ? Optional.of(binding) : Optional.empty();
    }

    /** Member lookup before a network entity exists; does not confer entity-type ownership. */
    public Optional<WorldAnatomyCatalog.Binding> resolveIdentity(Identifier identity) {
        return Optional.ofNullable(byIdentity.get(identity));
    }

    /** Only the owned prepared Binding instance has an ID here; foreign equal objects confer no ownership. */
    public Optional<Identifier> identity(WorldAnatomyCatalog.Binding binding) {
        return Optional.ofNullable(identities.get(binding));
    }
}
