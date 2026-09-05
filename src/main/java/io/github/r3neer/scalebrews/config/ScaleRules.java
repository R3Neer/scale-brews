package io.github.r3neer.scalebrews.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.r3neer.scalebrews.ScaleBrews;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import java.util.Map;

/** World-owned rules, sent to clients by Fabric's synced dynamic registry. */
public record ScaleRules(boolean tinyMounts, Map<Identifier, Boolean> mounts, boolean villagerFear,
        boolean growthImpact, boolean growthImpactKnockback, boolean growthImpactDamage,
        boolean environment, boolean farmland, boolean pressurePlates, boolean turtleEggs,
        boolean quietMovement, boolean terrainResistance) {
    public static final ScaleRules DEFAULT = new ScaleRules(true, Map.of(), true, true, true, true,
            true, true, true, true, true, true);
    public ScaleRules { mounts = Map.copyOf(mounts); }
    public static final Codec<ScaleRules> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.BOOL.optionalFieldOf("tiny_mounts", true).forGetter(ScaleRules::tinyMounts),
        Codec.unboundedMap(Identifier.CODEC, Codec.BOOL).optionalFieldOf("mounts", Map.of()).forGetter(ScaleRules::mounts),
        Codec.BOOL.optionalFieldOf("villager_fear", true).forGetter(ScaleRules::villagerFear),
        Codec.BOOL.optionalFieldOf("growth_landing_impact", true).forGetter(ScaleRules::growthImpact),
        Codec.BOOL.optionalFieldOf("growth_landing_knockback", true).forGetter(ScaleRules::growthImpactKnockback),
        Codec.BOOL.optionalFieldOf("growth_landing_damage", true).forGetter(ScaleRules::growthImpactDamage),
        Codec.BOOL.optionalFieldOf("environment_interactions", true).forGetter(ScaleRules::environment),
        Codec.BOOL.optionalFieldOf("farmland_protection", true).forGetter(ScaleRules::farmland),
        Codec.BOOL.optionalFieldOf("pressure_plate_bypass", true).forGetter(ScaleRules::pressurePlates),
        Codec.BOOL.optionalFieldOf("turtle_egg_protection", true).forGetter(ScaleRules::turtleEggs),
        Codec.BOOL.optionalFieldOf("quiet_movement", true).forGetter(ScaleRules::quietMovement),
        Codec.BOOL.optionalFieldOf("growth_terrain_resistance", true).forGetter(ScaleRules::terrainResistance)
    ).apply(i, ScaleRules::new));
    public static final ResourceKey<Registry<ScaleRules>> REGISTRY = ResourceKey.createRegistryKey(ScaleBrews.id("rules"));
    private static final ResourceKey<ScaleRules> ACTIVE = ResourceKey.create(REGISTRY, ScaleBrews.id("default"));

    public static void initialize() { DynamicRegistries.registerSynced(REGISTRY, CODEC); }
    public static ScaleRules get(Level level) {
        return level.registryAccess().lookup(REGISTRY).flatMap(r -> r.get(ACTIVE))
                .map(h -> h.value()).orElse(DEFAULT);
    }
    public boolean mountEnabled(Identifier entity) { return tinyMounts && mounts.getOrDefault(entity, true); }
    public boolean protectsFarmland() { return environment && farmland; }
    public boolean bypassesPlates() { return environment && pressurePlates; }
    public boolean protectsEggs() { return environment && turtleEggs; }
    public boolean quietensMovement() { return environment && quietMovement; }
    public boolean resistsTerrain() { return environment && terrainResistance; }
}
