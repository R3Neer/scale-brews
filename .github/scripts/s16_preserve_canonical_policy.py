from pathlib import Path


def replace(path, old, new, expected=1):
    p = Path(path)
    text = p.read_text()
    found = text.count(old)
    if found != expected:
        raise SystemExit(f"{path}: expected {expected} occurrence(s), found {found}: {old}")
    p.write_text(text.replace(old, new))


runtime = 'src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyRuntime.java'
replace(runtime,
'''import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.integration.CollisionRules;
import io.github.r3neer.scalebrews.collision.migration.LegacyAnatomyCatalogMigration;
import io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData;''',
'''import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.migration.LegacyAnatomyCatalogMigration;''')
replace(runtime,
'import io.github.r3neer.scalebrews.platform.Platforms;\n',
'')
replace(runtime,
'''    /** True only for a support selected by the accepted canonical catalog and executable bridge. */
    public static boolean hasBinding(LivingEntity support) { return support!=null && owns(support); }

    /** Canonical policy authority for an active anatomical support. No PlatformDefinition participates here. */
    public static boolean eligible(Entity body,LivingEntity support) {
        if(body==null || support==null || body==support || body.level()!=support.level()
                || !Platforms.ordinary(body) || !Platforms.ordinary(support))return false;
        var server=support.level().getServer();var state=server==null?null:STATES.get(server);
        var active=state==null?null:state.entities.get(support);if(active==null)return false;
        var category=Platforms.category(body);if(category==null)return false;
        double ratio=body.getBbWidth()/(double)support.getBbWidth();
        if(!Double.isFinite(ratio) || ratio<=0)return false;
        var rule=policy(body,support,active,category);
        if(!rule.enabled() || ratio>rule.maxWidthRatio())return false;
        Entity ancestor=support;Set<Entity> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        while(ancestor!=null) {
            if(ancestor==body || !seen.add(ancestor))return false;
            var anatomical=AnatomyMovement.contact(ancestor);
            ancestor=anatomical==null?Platforms.state(ancestor).support:anatomical.support();
        }
        return true;
    }

    /** Canonical material coefficient for the currently active support; missing authority preserves vanilla input. */
    public static double friction(Entity body,LivingEntity support,double original) {
        if(body==null || support==null || body.level()!=support.level())return original;
        var server=support.level().getServer();var state=server==null?null:STATES.get(server);
        var active=state==null?null:state.entities.get(support);if(active==null)return original;
        var category=Platforms.category(body);if(category==null)return original;
        return policy(body,support,active,category).friction();
    }

    private static CollisionPolicy.Rule policy(Entity body,LivingEntity support,Active active,String category) {
        var global=LegacyCollisionData.policy(Platforms.policy(body.level()));
        return CollisionRules.resolve(global,active.binding().selection().policy(),category,active.binding().selection().entity());
    }
''',
'''    /**
     * Canonical default selection for this entity type in the accepted server catalog.
     * This is intentionally independent of provider executability: a manual/core fixture may
     * borrow policy authority without becoming an AnatomyRuntime causal binding.
     */
    public static Optional<CollisionBinding> catalogBinding(LivingEntity support) {
        if(support==null)return Optional.empty();
        var server=support.level().getServer();
        var state=server==null?null:STATES.get(server);
        if(state==null)return Optional.empty();
        return state.catalog.snapshot().catalog().resolve(BuiltInRegistries.ENTITY_TYPE.getKey(support.getType()),Map.of());
    }
''')

movement = 'src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyMovement.java'
replace(movement,
'''    /** Fixture-only overload. Production runtime uses the causal-descriptor overload. */
    public static synchronized void register(LivingEntity support,GeometryProvider provider){
        if(support==null || provider==null)throw new IllegalArgumentException("Missing geometry registration");
        requireServerThread(support.level());
        AnatomyBindingState.rebind(support,provider,null);
        FRAME_SERIALS.remove(support);ROOTS.remove(support);clearSupportContacts(support);removeSpatialEntry(support);
        refreshSpatialEntry(support);
    }
''',
'''    /**
     * Fixture/local-core overload. Inside an accepted server session it borrows the canonical
     * policy selection but remains non-causal: no descriptor, no certified motion interval.
     */
    public static synchronized void register(LivingEntity support,GeometryProvider provider){
        if(support==null || provider==null)throw new IllegalArgumentException("Missing geometry registration");
        requireServerThread(support.level());
        var binding=AnatomyRuntime.catalogBinding(support).orElse(AnatomyBindingState.binding(support));
        AnatomyBindingState.rebind(support,provider,null,binding);
        FRAME_SERIALS.remove(support);ROOTS.remove(support);clearSupportContacts(support);removeSpatialEntry(support);
        refreshSpatialEntry(support);
    }
''')
