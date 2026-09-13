# Size-scaled gait presentation

This file is the canonical requirement source for size-dependent footstep presentation.

## Requirements

- Presentation follows the current physical `Attributes.SCALE`, including Scale Brews transitions, mixed effects and external scale modifiers. Active potion names are not an input.
- Vanilla step cadence remains owned by Minecraft movement distance. Scale Brews changes only the pitch of ordinary, combination and muffled step sounds emitted through the vanilla `Entity` step-sound path.
- Footstep pitch multiplier is `clamp(1 / sqrt(scale), 0.5, 2.0)`. Scale `1` is therefore byte-for-byte neutral at the argument level; larger bodies sound lower and smaller bodies higher.
- Vanilla walk-animation amplitude remains unchanged. Only `WalkAnimationState`'s time scale changes, using `clamp(1 / scale, 0.25, 4.0)`, so stride frequency tracks body-relative travel distance.
- Invalid or non-positive scale values fail neutral at multiplier `1` rather than contaminating sound or animation state.
- The hooks must delegate these rules to one shared helper. Mixins must not maintain a second copy of the curves.

## Integration boundary

The sound rule covers living entities that use Minecraft's vanilla `Entity` step-sound helpers, including normal, combination and muffled surfaces. The gait-clock rule covers the default `LivingEntity.updateWalkAnimation` path used by players and ordinary vanilla living entities. Species or external mods that replace that walk-animation method entirely retain ownership of their custom animation timing unless a compatibility adapter is added deliberately.
