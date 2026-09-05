package io.github.r3neer.scalebrews.mount;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import java.util.Optional;

public record TinyMountDefinition(Identifier entity, int minimumShrinking, boolean saddle,
        Control control, Movement movement, Optional<Identifier> steeringItem,
        float speed, float maxPitch, float maxVerticalSpeed, Ability ability, boolean enabled,
        Optional<SaddleVisual> saddleVisual) {
    public static final Codec<TinyMountDefinition> CODEC = RecordCodecBuilder.<TinyMountDefinition>create(i -> i.group(
        Identifier.CODEC.fieldOf("entity").forGetter(TinyMountDefinition::entity),
        Codec.intRange(2, 3).fieldOf("minimum_shrinking").forGetter(TinyMountDefinition::minimumShrinking),
        Codec.BOOL.optionalFieldOf("saddle", true).forGetter(TinyMountDefinition::saddle),
        StringRepresentable.fromEnum(Control::values).fieldOf("control").forGetter(TinyMountDefinition::control),
        StringRepresentable.fromEnum(Movement::values).fieldOf("movement").forGetter(TinyMountDefinition::movement),
        Identifier.CODEC.optionalFieldOf("steering_item").forGetter(TinyMountDefinition::steeringItem),
        Codec.floatRange(.01F, 1F).fieldOf("speed").forGetter(TinyMountDefinition::speed),
        Codec.floatRange(0F, 75F).optionalFieldOf("max_pitch", 60F).forGetter(TinyMountDefinition::maxPitch),
        Codec.floatRange(.01F, .5F).optionalFieldOf("max_vertical_speed", .15F).forGetter(TinyMountDefinition::maxVerticalSpeed),
        StringRepresentable.fromEnum(Ability::values).optionalFieldOf("ability", Ability.NONE).forGetter(TinyMountDefinition::ability),
        Codec.BOOL.optionalFieldOf("enabled", true).forGetter(TinyMountDefinition::enabled),
        SaddleVisual.CODEC.optionalFieldOf("saddle_visual").forGetter(TinyMountDefinition::saddleVisual)
    ).apply(i, TinyMountDefinition::new)).validate(d -> {
        if (d.saddle() && d.saddleVisual().isEmpty())
            return com.mojang.serialization.DataResult.error(() -> "Saddled mounts require saddle_visual (texture and anchor)");
        if (d.control() == Control.ITEM_STEERED && d.steeringItem().isEmpty())
            return com.mojang.serialization.DataResult.error(() -> "item_steered requires steering_item");
        return com.mojang.serialization.DataResult.success(d);
    });

    public record SaddleVisual(Identifier texture, String anchor) {
        public static final Codec<SaddleVisual> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("texture").forGetter(SaddleVisual::texture),
            Codec.STRING.validate(s -> s.equals("body") || s.equals("bone")
                ? com.mojang.serialization.DataResult.success(s)
                : com.mojang.serialization.DataResult.error(() -> "Saddle anchor must be body or bone"))
                .fieldOf("anchor").forGetter(SaddleVisual::anchor)
        ).apply(i, SaddleVisual::new));
    }

    public enum Control implements StringRepresentable {
        DIRECT, ITEM_STEERED;
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public enum Movement implements StringRepresentable {
        GROUND, FLYING_LOOK_DIRECTION;
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public enum Ability implements StringRepresentable {
        NONE, CHICKEN_GLIDE;
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
}
