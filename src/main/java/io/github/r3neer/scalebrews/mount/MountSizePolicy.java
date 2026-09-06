package io.github.r3neer.scalebrews.mount;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.r3neer.scalebrews.ScaleBrews;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import java.util.Map;

/** A size gate only: never grants riding permissions or replaces native mount behavior. */
public record MountSizePolicy(double fallback, Map<Identifier, Double> ratios) {
    public static final Codec<Double> RATIO = Codec.doubleRange(0.0001, 1024);
    public static final Codec<MountSizePolicy> CODEC = RecordCodecBuilder.create(i -> i.group(
        RATIO.optionalFieldOf("default_max_rider_scale_ratio", 1.0).forGetter(MountSizePolicy::fallback),
        Codec.unboundedMap(Identifier.CODEC, RATIO).optionalFieldOf("mounts", Map.of()).forGetter(MountSizePolicy::ratios)
    ).apply(i, MountSizePolicy::new));
    public static final ResourceKey<Registry<MountSizePolicy>> REGISTRY = ResourceKey.createRegistryKey(ScaleBrews.id("mount_size_policy"));
    public static final ResourceKey<MountSizePolicy> DEFAULT = ResourceKey.create(REGISTRY, ScaleBrews.id("default"));
    private static final MountSizePolicy FALLBACK = new MountSizePolicy(1, Map.of());

    public static void initialize() { DynamicRegistries.registerSynced(REGISTRY, CODEC); }

    public static MountSizePolicy get(Level level) {
        return level.registryAccess().lookup(REGISTRY).flatMap(r -> r.get(DEFAULT)).map(h -> h.value()).orElse(FALLBACK);
    }

    public double limit(Entity mount) {
        var override = ratios.get(BuiltInRegistries.ENTITY_TYPE.getKey(mount.getType()));
        if (override != null) return override;
        // Size policy remains active even when the optional tiny-mount controls are disabled.
        var tiny = TinyMounts.configuredDefinition(mount);
        return tiny == null ? fallback : tiny.maxRiderScaleRatio();
    }

    public static boolean permits(LivingEntity rider, Entity mount) {
        if (!(mount instanceof LivingEntity living)) return true; // Boats/minecarts have no SCALE.
        // Keep attribute precision: getScale() narrows to float and can reject exact JSON boundaries.
        double riderScale = rider.getAttributeValue(Attributes.SCALE);
        double mountScale = living.getAttributeValue(Attributes.SCALE);
        return Double.isFinite(riderScale) && Double.isFinite(mountScale) && mountScale > 0
            && riderScale / mountScale <= get(mount.level()).limit(mount);
    }
}
