package io.github.r3neer.scalebrews.platform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import java.util.List;
import java.util.Optional;

/** Blocks in an adult's unscaled body frame: +x right, +z forward, +y up. */
public record PlatformDefinition(Identifier entity, boolean enabled, double friction,
                                 Optional<Double> maxRatio, List<Surface> surfaces) {
    public static final Codec<Double> FINITE = Codec.DOUBLE.validate(v -> Double.isFinite(v)
        ? DataResult.success(v) : DataResult.error(() -> "Expected finite coordinate"));
    public static final Codec<Double> POSITIVE = FINITE.validate(v -> v > 0 && v <= 1024
        ? DataResult.success(v) : DataResult.error(() -> "Expected value in (0, 1024]"));
    private static final Codec<String> SURFACE_ID=Codec.STRING.validate(v->v.matches("[a-z0-9_.-]{1,128}")
        ? DataResult.success(v) : DataResult.error(()->"Surface id must be 1-128 lowercase letters, digits, dot, dash or underscore"));
    public static final Codec<PlatformDefinition> CODEC = RecordCodecBuilder.<PlatformDefinition>create(i -> i.group(
        Identifier.CODEC.fieldOf("entity").forGetter(PlatformDefinition::entity),
        Codec.BOOL.optionalFieldOf("enabled", true).forGetter(PlatformDefinition::enabled),
        FINITE.validate(v->v>=.01 && v<=1?DataResult.success(v):DataResult.error(()->"Friction must be between 0.01 and 1"))
            .optionalFieldOf("friction", .6).forGetter(PlatformDefinition::friction),
        POSITIVE.optionalFieldOf("max_width_ratio").forGetter(PlatformDefinition::maxRatio),
        Surface.CODEC.listOf().fieldOf("surfaces").forGetter(PlatformDefinition::surfaces)
    ).apply(i, PlatformDefinition::new)).validate(d -> !d.surfaces.isEmpty()
        && d.surfaces.stream().map(Surface::id).distinct().count() == d.surfaces.size()
        ? DataResult.success(d) : DataResult.error(() -> "Surfaces must be nonempty with unique ids"));
    public PlatformDefinition { surfaces = List.copyOf(surfaces); }

    public record Visual(String part, double x, double y, double z) {
        public static final Codec<Visual> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("part").forGetter(Visual::part),
            FINITE.optionalFieldOf("x", 0d).forGetter(Visual::x),
            FINITE.optionalFieldOf("y", 0d).forGetter(Visual::y),
            FINITE.optionalFieldOf("z", 0d).forGetter(Visual::z)
        ).apply(i, Visual::new));
    }
    public record Surface(String id, double x, double y, double z, double width, double depth,
                          Optional<Visual> visual) {
        public static final Codec<Surface> CODEC = RecordCodecBuilder.create(i -> i.group(
            SURFACE_ID.fieldOf("id").forGetter(Surface::id),
            FINITE.optionalFieldOf("x", 0d).forGetter(Surface::x),
            FINITE.fieldOf("y").forGetter(Surface::y),
            FINITE.optionalFieldOf("z", 0d).forGetter(Surface::z),
            POSITIVE.fieldOf("width").forGetter(Surface::width),
            POSITIVE.fieldOf("depth").forGetter(Surface::depth),
            Visual.CODEC.optionalFieldOf("visual").forGetter(Surface::visual)
        ).apply(i, Surface::new));
    }
}
