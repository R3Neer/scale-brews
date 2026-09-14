package io.github.r3neer.scalebrews.mount;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import java.util.Optional;

/** Data-driven Tiny Mount contract. Family owns interaction/control grammar; movement and abilities stay orthogonal. */
public record TinyMountDefinition(Identifier entity, double maxRiderScaleRatio,
        Family family, Movement movement, Optional<Identifier> steeringItem,
        float speed, float maxPitch, float maxVerticalSpeed, Ability ability, boolean enabled,
        Optional<SaddleVisual> saddleVisual, Optional<BodyEquipment> bodyEquipment) {
    public static final Codec<TinyMountDefinition> CODEC = RecordCodecBuilder.<TinyMountDefinition>create(i -> i.group(
        Identifier.CODEC.fieldOf("entity").forGetter(TinyMountDefinition::entity),
        // Legacy files without a ratio adopt the current Tiny Mount size policy.
        MountSizePolicy.RATIO.optionalFieldOf("max_rider_scale_ratio", 0.53).forGetter(TinyMountDefinition::maxRiderScaleRatio),
        StringRepresentable.fromEnum(Family::values).optionalFieldOf("family", Family.DIRECT).forGetter(TinyMountDefinition::family),
        StringRepresentable.fromEnum(Movement::values).optionalFieldOf("movement", Movement.GROUND).forGetter(TinyMountDefinition::movement),
        Identifier.CODEC.optionalFieldOf("steering_item").forGetter(TinyMountDefinition::steeringItem),
        Codec.floatRange(.01F, 1F).optionalFieldOf("speed", .18F).forGetter(TinyMountDefinition::speed),
        Codec.floatRange(0F, 75F).optionalFieldOf("max_pitch", 60F).forGetter(TinyMountDefinition::maxPitch),
        Codec.floatRange(.01F, .5F).optionalFieldOf("max_vertical_speed", .15F).forGetter(TinyMountDefinition::maxVerticalSpeed),
        StringRepresentable.fromEnum(Ability::values).optionalFieldOf("ability", Ability.NONE).forGetter(TinyMountDefinition::ability),
        Codec.BOOL.optionalFieldOf("enabled", true).forGetter(TinyMountDefinition::enabled),
        SaddleVisual.CODEC.optionalFieldOf("saddle_visual").forGetter(TinyMountDefinition::saddleVisual),
        BodyEquipment.CODEC.optionalFieldOf("body_equipment").forGetter(TinyMountDefinition::bodyEquipment)
    ).apply(i, TinyMountDefinition::new)).validate(d -> {
        if (d.family().usesSteeringItem() != d.steeringItem().isPresent())
            return com.mojang.serialization.DataResult.error(() -> d.family().usesSteeringItem()
                    ? "item_steered requires steering_item"
                    : "steering_item is only valid for item_steered mounts");
        if (d.family() == Family.ITEM_STEERED && d.bodyEquipment().isPresent())
            return com.mojang.serialization.DataResult.error(() -> "item_steered mounts use saddle-only equipment and have no mount inventory");
        return com.mojang.serialization.DataResult.success(d);
    });

    /** Saddle inventory/interaction remains family-driven; its visual is now an optional client hint. */
    public boolean saddle() { return true; }
    /** Java-source compatibility only. Control is derived from family and is no longer independently configurable. */
    public Control control() { return family == Family.ITEM_STEERED ? Control.ITEM_STEERED : Control.DIRECT; }

    public record SaddleVisual(Identifier texture, String anchor) {
        public static final Codec<SaddleVisual> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("texture").forGetter(SaddleVisual::texture),
            Codec.STRING.optionalFieldOf("anchor", "").forGetter(SaddleVisual::anchor)
        ).apply(i, SaddleVisual::new));
    }

    /** Optional native BODY equipment exposed by inventory-bearing families. */
    public record BodyEquipment(Identifier item, Identifier slotIcon) {
        public static final Codec<BodyEquipment> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("item").forGetter(BodyEquipment::item),
            Identifier.CODEC.fieldOf("slot_icon").forGetter(BodyEquipment::slotIcon)
        ).apply(i, BodyEquipment::new));
    }

    public enum Family implements StringRepresentable {
        DIRECT,
        TAMEABLE_DIRECT,
        ITEM_STEERED;

        public boolean hasInventory() { return this != ITEM_STEERED; }
        public boolean requiresTameForControl() { return this == TAMEABLE_DIRECT; }
        public boolean requiresSaddleToMount() { return this == ITEM_STEERED; }
        public boolean usesSteeringItem() { return this == ITEM_STEERED; }
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    public enum Control { DIRECT, ITEM_STEERED }

    public enum Movement implements StringRepresentable {
        GROUND, FLYING_LOOK_DIRECTION;
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public enum Ability implements StringRepresentable {
        NONE, CHICKEN_GLIDE, WOLF_POUNCE;
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
}
