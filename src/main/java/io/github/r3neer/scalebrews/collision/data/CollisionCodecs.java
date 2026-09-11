package io.github.r3neer.scalebrews.collision.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.resources.Identifier;

/** Canonical v1 codecs. Legacy platform JSON is decoded only by the dedicated migration decoder. */
public final class CollisionCodecs {
    private CollisionCodecs() {}

    private static final Codec<Integer> BINDING_SCHEMA = Codec.INT.validate(value -> value == CollisionBinding.SCHEMA_VERSION
        ? DataResult.success(value) : DataResult.error(() -> "Unsupported collision binding schema " + value));
    private static final Codec<Integer> POLICY_SCHEMA = Codec.INT.validate(value -> value == CollisionPolicy.SCHEMA_VERSION
        ? DataResult.success(value) : DataResult.error(() -> "Unsupported collision policy schema " + value));
    private static final Codec<Double> FINITE_NONNEGATIVE = Codec.DOUBLE.validate(value -> Double.isFinite(value) && value >= 0
        ? DataResult.success(value) : DataResult.error(() -> "Expected finite non-negative number"));
    private static final Codec<Double> FINITE = Codec.DOUBLE.validate(value -> Double.isFinite(value)
        ? DataResult.success(value) : DataResult.error(() -> "Expected finite number"));
    private static final Codec<Double> RATIO = Codec.DOUBLE.validate(value -> Double.isFinite(value) && value > 0 && value <= 1024
        ? DataResult.success(value) : DataResult.error(() -> "Invalid max width ratio"));
    private static final Codec<Double> FRICTION = Codec.DOUBLE.validate(value -> Double.isFinite(value) && value >= 0 && value <= 1
        ? DataResult.success(value) : DataResult.error(() -> "Invalid friction"));
    private static final Codec<String> KEY = Codec.STRING.validate(value -> value != null && value.matches("[a-z0-9_.-]{1,64}")
        ? DataResult.success(value) : DataResult.error(() -> "Invalid canonical key"));
    private static final Codec<String> VALUE = Codec.STRING.validate(value -> value != null && value.length() <= 1024
        ? DataResult.success(value) : DataResult.error(() -> "Engine parameter too long"));
    private static final Codec<String> PIECE_ID = Codec.STRING.validate(value -> value != null && !value.isBlank() && value.length() <= 256
        ? DataResult.success(value) : DataResult.error(() -> "Invalid anatomy piece/part id"));
    private static final Codec<Map<String, String>> STRING_MAP = Codec.unboundedMap(KEY, VALUE).validate(map -> map.size() <= 128
        ? DataResult.success(map) : DataResult.error(() -> "Too many engine parameters"));
    private static final Codec<Map<String, String>> VARIANT_MAP = STRING_MAP
        .validate(map -> map.size() <= 32 ? DataResult.success(map) : DataResult.error(() -> "Too many collision variant selectors"))
        .validate(map -> map.values().stream().allMatch(value -> value.length() <= 256)
            ? DataResult.success(map) : DataResult.error(() -> "Collision variant value too long"));
    private static final Codec<Set<String>> STRING_SET = canonicalSet(KEY);
    private static final Codec<Set<String>> PIECE_SET = canonicalSet(PIECE_ID);
    private static final Codec<Set<String>> POSE_CHANNELS = STRING_SET.validate(values -> values.size() <= 64
        ? DataResult.success(values) : DataResult.error(() -> "Too many pose channels"));
    private static final Codec<Set<String>> EXCLUDED_STATES = STRING_SET.validate(values -> values.size() <= 64
        ? DataResult.success(values) : DataResult.error(() -> "Too many excluded collision states"));

    private static Codec<Set<String>> canonicalSet(Codec<String> element) {
        return element.listOf().validate(values -> new java.util.HashSet<>(values).size() == values.size()
            ? DataResult.success(values) : DataResult.error(() -> "Duplicate canonical value"))
            .xmap(values -> Collections.unmodifiableSet(new TreeSet<>(values)), values -> values.stream().sorted().toList());
    }

    private record FilterData(double thickness, double aspect, double volume, Set<String> include, Set<String> exclude) {}
    private static final Codec<FilterData> FILTER_DATA = RecordCodecBuilder.create(i -> i.group(
        FINITE_NONNEGATIVE.optionalFieldOf("min_thickness", AnatomyFilter.DEFAULT.minThickness()).forGetter(FilterData::thickness),
        FINITE.optionalFieldOf("min_aspect", AnatomyFilter.DEFAULT.minAspect()).forGetter(FilterData::aspect),
        FINITE.optionalFieldOf("min_volume_ratio", AnatomyFilter.DEFAULT.minVolumeRatio()).forGetter(FilterData::volume),
        PIECE_SET.optionalFieldOf("include", Set.of()).forGetter(FilterData::include),
        PIECE_SET.optionalFieldOf("exclude", Set.of()).forGetter(FilterData::exclude)
    ).apply(i, FilterData::new));

    public static final Codec<AnatomyFilter> FILTER = FILTER_DATA.comapFlatMap(data -> {
        try {
            return DataResult.success(new AnatomyFilter(data.thickness(), data.aspect(), data.volume(), data.include(), data.exclude()));
        } catch (IllegalArgumentException invalid) {
            return DataResult.error(invalid::getMessage);
        }
    }, filter -> new FilterData(filter.minThickness(), filter.minAspect(), filter.minVolumeRatio(), filter.include(), filter.exclude()));

    public static final Codec<CollisionPolicy.Patch> POLICY_PATCH = RecordCodecBuilder.create(i -> i.group(
        Codec.BOOL.optionalFieldOf("enabled").forGetter(CollisionPolicy.Patch::enabled),
        RATIO.optionalFieldOf("max_width_ratio").forGetter(CollisionPolicy.Patch::maxWidthRatio),
        FRICTION.optionalFieldOf("friction").forGetter(CollisionPolicy.Patch::friction)
    ).apply(i, CollisionPolicy.Patch::new));

    private static final Codec<Map<String, CollisionPolicy.Patch>> CATEGORY_POLICIES = Codec.unboundedMap(KEY, POLICY_PATCH)
        .validate(map -> map.size() <= 256 ? DataResult.success(map) : DataResult.error(() -> "Too many collision category policies"));
    private static final Codec<Map<Identifier, CollisionPolicy.Patch>> SUPPORT_POLICIES = Codec.unboundedMap(Identifier.CODEC, POLICY_PATCH)
        .validate(map -> map.size() <= 4096 ? DataResult.success(map) : DataResult.error(() -> "Too many collision support policies"));

    public static final Codec<CollisionPolicy.Rule> POLICY_RULE = RecordCodecBuilder.create(i -> i.group(
        Codec.BOOL.fieldOf("enabled").forGetter(CollisionPolicy.Rule::enabled),
        RATIO.fieldOf("max_width_ratio").forGetter(CollisionPolicy.Rule::maxWidthRatio),
        FRICTION.fieldOf("friction").forGetter(CollisionPolicy.Rule::friction)
    ).apply(i, CollisionPolicy.Rule::new));

    public static final Codec<CollisionPolicy> POLICY = RecordCodecBuilder.create(i -> i.group(
        POLICY_SCHEMA.fieldOf("schema_version").forGetter(CollisionPolicy::schemaVersion),
        POLICY_RULE.fieldOf("defaults").forGetter(CollisionPolicy::defaults),
        CATEGORY_POLICIES.optionalFieldOf("categories", Map.of()).forGetter(CollisionPolicy::categories),
        SUPPORT_POLICIES.optionalFieldOf("supports", Map.of()).forGetter(CollisionPolicy::supports)
    ).apply(i, CollisionPolicy::new));

    public static final Codec<CollisionBinding.Geometry> GEOMETRY_SELECTION = RecordCodecBuilder.create(i -> i.group(
        Identifier.CODEC.fieldOf("engine").forGetter(CollisionBinding.Geometry::engine),
        Identifier.CODEC.fieldOf("model").forGetter(CollisionBinding.Geometry::model),
        STRING_MAP.optionalFieldOf("parameters", Map.of()).forGetter(CollisionBinding.Geometry::parameters),
        FILTER.optionalFieldOf("filter", AnatomyFilter.DEFAULT).forGetter(CollisionBinding.Geometry::filter)
    ).apply(i, CollisionBinding.Geometry::new));

    public static final Codec<CollisionBinding.Pose> POSE_SELECTION = RecordCodecBuilder.create(i -> i.group(
        Identifier.CODEC.fieldOf("engine").forGetter(CollisionBinding.Pose::engine),
        STRING_MAP.optionalFieldOf("parameters", Map.of()).forGetter(CollisionBinding.Pose::parameters),
        POSE_CHANNELS.optionalFieldOf("channels", Set.of()).forGetter(CollisionBinding.Pose::channels)
    ).apply(i, CollisionBinding.Pose::new));

    public static final Codec<CollisionBinding> BINDING = RecordCodecBuilder.create(i -> i.group(
        BINDING_SCHEMA.fieldOf("schema_version").forGetter(CollisionBinding::schemaVersion),
        Identifier.CODEC.fieldOf("entity").forGetter(CollisionBinding::entity),
        VARIANT_MAP.optionalFieldOf("variant", Map.of()).forGetter(CollisionBinding::variant),
        GEOMETRY_SELECTION.fieldOf("geometry").forGetter(CollisionBinding::geometry),
        POSE_SELECTION.fieldOf("pose").forGetter(CollisionBinding::pose),
        Identifier.CODEC.fieldOf("root_transform").forGetter(CollisionBinding::rootTransform),
        POLICY_PATCH.optionalFieldOf("policy", CollisionPolicy.Patch.EMPTY).forGetter(CollisionBinding::policy),
        EXCLUDED_STATES.optionalFieldOf("excluded_states", Set.of()).forGetter(CollisionBinding::excludedStates)
    ).apply(i, CollisionBinding::new));
}
