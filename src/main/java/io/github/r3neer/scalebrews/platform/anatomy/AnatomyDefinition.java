package io.github.r3neer.scalebrews.platform.anatomy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/** World-selected original or alternative catalog model, independently of the host's renderer. */
public record AnatomyDefinition(Identifier model,Identifier poses,AnatomyFilter filter) {
    public static final Codec<AnatomyDefinition> CODEC=RecordCodecBuilder.create(i->i.group(
        Identifier.CODEC.fieldOf("model").forGetter(AnatomyDefinition::model),
        Identifier.CODEC.fieldOf("pose_provider").forGetter(AnatomyDefinition::poses),
        AnatomyCodecs.FILTER.optionalFieldOf("filter",AnatomyFilter.DEFAULT).forGetter(AnatomyDefinition::filter)
    ).apply(i,AnatomyDefinition::new));
    public AnatomyDefinition {java.util.Objects.requireNonNull(model);java.util.Objects.requireNonNull(poses);java.util.Objects.requireNonNull(filter);}
}
