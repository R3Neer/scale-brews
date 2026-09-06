package io.github.r3neer.scalebrews.platform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import java.util.Map;

public record PlatformPolicy(boolean enabled, double maxWidthRatio, Map<String, Boolean> bodies,
                             Map<Identifier, Boolean> supports, boolean automaticSurfaces) {
    public PlatformPolicy(boolean enabled, double maxWidthRatio, Map<String,Boolean> bodies, Map<Identifier,Boolean> supports) {
        this(enabled,maxWidthRatio,bodies,supports,true);
    }
    public static final PlatformPolicy DEFAULT = new PlatformPolicy(true, .85, Map.of(), Map.of(), true);
    public static final Codec<PlatformPolicy> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.BOOL.optionalFieldOf("enabled", true).forGetter(PlatformPolicy::enabled),
        PlatformDefinition.POSITIVE.optionalFieldOf("max_width_ratio", .85).forGetter(PlatformPolicy::maxWidthRatio),
        Codec.unboundedMap(Codec.STRING, Codec.BOOL).optionalFieldOf("bodies", Map.of()).forGetter(PlatformPolicy::bodies),
        Codec.unboundedMap(Identifier.CODEC, Codec.BOOL).optionalFieldOf("supports", Map.of()).forGetter(PlatformPolicy::supports),
        Codec.BOOL.optionalFieldOf("automatic_surfaces", true).forGetter(PlatformPolicy::automaticSurfaces)
    ).apply(i, PlatformPolicy::new));
    public PlatformPolicy { bodies = Map.copyOf(bodies); supports = Map.copyOf(supports); }
}
