package io.github.r3neer.scalebrews.collision.migration;

import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * One-way decoder for the released PlatformDefinition shape. It never turns a
 * legacy top plane into anatomical geometry and never creates a fallback engine.
 */
public final class LegacyCollisionData {
    private LegacyCollisionData() {}

    public static final Identifier PRECOMPUTED_GEOMETRY = Identifier.parse("scalebrews:precomputed_geometry");
    public static final Identifier LEGACY_POSE_PROVIDER = Identifier.parse("scalebrews:legacy_pose_provider");
    public static final Identifier ENTITY_ROOT = Identifier.parse("scalebrews:entity_root");

    public record Visual(String part, double x, double y, double z) {}
    public record Plane(String id, double x, double y, double z, double width, double depth, Optional<Visual> visual) {}
    public record LegacyPlanes(Identifier entity, boolean enabled, double friction, Optional<Double> maxWidthRatio, List<Plane> planes) {
        public LegacyPlanes { planes = List.copyOf(planes); }
    }
    public record Decoded(Optional<CollisionBinding> binding, Optional<LegacyPlanes> legacyPlanes) {
        public Decoded {
            if (binding == null || legacyPlanes == null || binding.isPresent() == legacyPlanes.isPresent())
                throw new IllegalArgumentException("Legacy collision data must decode to exactly one representation");
        }
    }

    public static Decoded decode(PlatformDefinition source) {
        if (source == null) throw new IllegalArgumentException("Missing legacy platform definition");
        boolean anatomy = source.anatomy().isPresent();
        boolean planes = !source.surfaces().isEmpty();
        if (anatomy == planes) throw new IllegalArgumentException("Legacy definition must contain anatomy xor planes");
        if (anatomy) {
            var old = source.anatomy().orElseThrow();
            var patch = new CollisionPolicy.Patch(Optional.of(source.enabled()), source.maxRatio(), Optional.of(source.friction()));
            var binding = new CollisionBinding(CollisionBinding.SCHEMA_VERSION, source.entity(), Map.of(),
                new CollisionBinding.Geometry(PRECOMPUTED_GEOMETRY, old.model(), Map.of(), old.filter()),
                new CollisionBinding.Pose(LEGACY_POSE_PROVIDER, Map.of("provider", old.poses().toString()), java.util.Set.of()),
                ENTITY_ROOT, patch, java.util.Set.of());
            return new Decoded(Optional.of(binding), Optional.empty());
        }
        var decoded = source.surfaces().stream().map(surface -> new Plane(surface.id(), surface.x(), surface.y(), surface.z(),
            surface.width(), surface.depth(), surface.visual().map(visual -> new Visual(visual.part(), visual.x(), visual.y(), visual.z())))).toList();
        return new Decoded(Optional.empty(), Optional.of(new LegacyPlanes(source.entity(), source.enabled(), source.friction(), source.maxRatio(), decoded)));
    }
}
