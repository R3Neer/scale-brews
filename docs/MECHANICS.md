# Mechanics and integration

## Core effects

Brewing supports level-I extended variants `scalebrews:long_growth` and `scalebrews:long_shrinking` (9,600 ticks, amplifier 0). Redstone extends the base potions; fermented spider eye converts extended Growth to extended Shrinking. There are no redstone recipes for II/III or glowstone recipes for extended variants. Native container conversions preserve the registered potion, so brewing works before or after converting to splash/lingering. Lingering delivery retains its native 0.25 duration factor. Extended variants share the existing effect/translation names; no separate texture or localization key is needed.

`effect/ScaleEffects` registers the attribute effects; `potion/ScalePotions` and `brewing/ScaleBrewing` own potion types and brewing. The pre-existing balance is preserved: Growth scale targets are 1.96/2.92/3.88, Shrinking 0.76/0.52/0.28. These reflect the owner's current code, not the earlier provisional specification.

`ScaleTransition` replaces only this mod's direct SCALE modifiers with one transient, server-owned multiplier. Smoothstep interpolation takes 20 simulation ticks. A changed target starts from the current value; duration refreshes do not restart the animation. Removing an effect interpolates back. Other mods' scale modifiers multiply normally. Existing saved modifiers from the tutorial are migrated. On a fresh load, the transient transition starts again; it is not persisted as a separate animation state. Native SCALE synchronization updates client size, collision box and camera; other attributes such as reach change immediately. Head proportions are unchanged.

`ScaleEffects` adds a separate ADD_MULTIPLIED_TOTAL movement modifier of +8% per Growth level and -8% per Shrinking level. `ScaleSprintHandler` replaces only the sprint modifier amount: Growth 1.20/1.12/1.05 or Shrinking 1.50/1.90/2.50, relative to that modified walking speed (not multiplied again by vanilla 1.30). Speed's separate attribute modifier stacks multiplicatively. Shrinking also adds +2.5% JUMP_STRENGTH per level through its own ADD_MULTIPLIED_TOTAL modifier; other jump modifiers are retained on removal. This changes jump strength, not height by the same percentage. Growth wins for sprint if both effects coexist; their walking modifiers both apply. Tick reconciliation catches hidden-effect downgrades; event hooks handle additions/removals. No sprint state is cancelled. Mods replacing sprint itself may require compatibility; unrelated attribute modifiers are retained.

| Effect | I total sprint | II total sprint | III total sprint |
| --- | ---: | ---: | ---: |
| Growth | 1.296 | 1.2992 | 1.302 |
| Shrinking | 1.38 | 1.596 | 1.90 |
| Speed alone | 1.56 | 1.82 | 2.08 |

Totals are relative to unmodified walking, on ordinary ground while sprinting, without other effects. This keeps the requested pure-speed ordering, not a guarantee on every terrain: Growth has a separate terrain advantage. Speed adds its usual 20% per level on top of Growth/Shrinking.

`ScaleExhaustion` multiplies only physical costs: Growth 1.10/1.20/1.30 and Shrinking .95/.90/.85. If both coexist, their factors multiply. Level progression is immediate; only physical size uses the 20-tick blend. `ScaleMovementExhaustionMixin` targets ServerPlayer movement statistics (sprint, swimming and water movement) and both ground-jump branches. `ScaleAttackExhaustionMixin` targets Player attack and the 26.2 stabbing attack. Ordinary walking's zero exhaustion remains zero. The global `causeFoodExhaustion` / `FoodData` sink is untouched, so regeneration, Hunger, received damage, mining and unrelated sources retain vanilla costs. Food values and hunger/saturation are never directly edited by the mod. Custom attacks from mods bypassing vanilla call sites are not automatically covered.

`ScaleHealthMixin` wraps the actual attribute update lifecycle, preserving the health fraction on addition, upgrade, hidden downgrade and removal. This fixes the tutorial event handler, which missed some power changes. Each invocation owns its snapshot; no global entity map is retained.

`ScaleBeacon` extends the third-tier effect list and validation/persistence set after registries initialize. The vanilla GUI reads the same list, so its existing layout, packets, payment and level-II upgrade work without a custom screen. Existing effects are retained.

## Creepers and living-entity landings

`ScaleExplosions` / `ScaleCreeperExplosionMixin` multiply the actual vanilla creeper explosion power after the charged multiplier: Growth I/II/III x1.15/x1.30/x1.50; Shrinking x0.90/x0.80/x0.65. Normal/charged base power 3/6 therefore becomes 4.5/9 with Growth III and 1.95/3.9 with Shrinking III. There is no extra cap or second explosion. Vanilla radius, damage, block destruction, mob-griefing rules, lingering cloud and death processing remain in their normal pipeline. Custom vanilla explosion-radius data is multiplied rather than replaced.

This balance explicitly follows the effect level, not an invented conversion from SCALE. Both effect factors multiply if they coexist; only physical size blends. Direct, splash and lingering application all feed the same active-effect lookup.

`GrowthImpact` applies to compatible LivingEntity, not just players. `checkFallDamage` supplies actual accumulated fall distance plus the last downward movement before vanilla resets it. A qualifying landing above 3 blocks produces the existing level-dependent wave; only Growth III above 6 blocks deals damage, bounded at 4 HP before radial falloff. Ordinary jumps and flying mobs with no accumulated fall do not qualify. No explosions or block destruction are added; self fall damage and landing game events remain untouched. Gust/terrain particles and landing sound also apply to mobs. Ally checks remain; player-vs-player restrictions apply only when the wave source is a player, so disabling PvP does not immunize players from mob waves.

The general hook supports modded living entities that retain vanilla-compatible fall processing; it does not add physics to flying/multipart/special entities that bypass it. There is no Ender Dragon adapter or species whitelist. Unrelated environmental and combat mechanics remain player-scoped. See CONFIGURATION.md for the combined landing-impact switch and the effective-SCALE mounting policy.

## Player physics

### Small-player camera

Client-only `ScaleCamera` / `ScaleCameraMixin` use `clamp(actualScale, .05, 1)` for a player viewed in first person. This follows both Shrinking and its removal blend, and composes with external scale attributes. Detached third-person and panoramic cameras return factor 1. Normal-size and larger players remain unchanged.

Both `Camera.update`'s world projection and `createProjectionMatrixForCulling`'s geometry culling use the same near-plane factor. Vanilla .05 becomes .038/.026/.014 blocks at Shrinking I/II/III, bounded to .0025 at extreme scales. `getNearPlane` already reads the projection, so its sampling follows the change. The separate held-item projection is not altered.

The interpolated bob amplitude is scaled as the camera render snapshot is extracted, keeping world and held-item animation consistent. This reduces translational and angular bob amplitude, not frequency. The bobbing preference, FOV, hurt tilt, entity position, eye height and collision remain untouched. These are clipping/bobbing mitigations, not a new collision solver for the camera; arbitrary extreme FOVs or camera/shader mods can need further integration.

| Mechanic | Integration | Behavior |
| --- | --- | --- |
| Landing impact | `GrowthImpact` + `GrowthImpactMixin` | Hook actual ground contact before vanilla resets fall distance; never cancel fall processing. Trigger only above 3 blocks. |
| Fall protection | `ScaleFallDamageMixin` | Multiply final magic/protection-adjusted fall damage by .92/.84/.76; no integer rounding or additive enchantment percentages. |
| Movement vibrations | `ScaleVibrationMixin` | Reject only tagged movement events outside each listener's reduced radius; retain vanilla crouching, occlusion and reception rules. |
| Swift Sneak | `ScaleSneaking` | Server-synchronized additive SNEAKING_SPEED bonus, only with the enchantment on leggings and Shrinking II/III. Removed when conditions end. |
| Pressure plates | `ScalePressurePlateMixin` | Filter the shared vanilla candidate list, keeping other entities and vanilla weighted counts. |
| Farmland | `ScaleFarmlandMixin` | Skip only trampling conversion for Shrinking III. Fall damage remains. |
| Turtle eggs | `ScaleTurtleEggMixin` | Disallow crushing by Shrinking III in the shared predicate used by walking and landing. |
| Terrain | `ScaleTerrainMixin` | Adjust tagged blocks' speed factors and stuck multipliers; retain vanilla damage and enchantment behavior. |

Impact radius is `min(6, 1.5 + tier*0.65 + min(20, fall-3)*0.08)`. Knockback strength is `min(2, (0.2 + tier*0.2)*(1 + min(20, fall-3)*0.06))`, multiplied by linear radial falloff and the target's vanilla knockback resistance. Targets are near landing height, within the horizontal radius and unobstructed by solid blocks. Allies and PvP-disabled players are excluded. No blocks are broken, no fire or explosions are created.

Only Growth III above 6 blocks deals damage: `min(4, (fall-6)*0.4) * (1-distance/radius)`. The direct player-attack damage source preserves normal target defenses; it does not invoke weapon attacks or mace smash logic. Visuals use SMALL_GUST / GUST_EMITTER_SMALL / GUST_EMITTER_LARGE plus block particles. A low-pitched iron golem step supplies the heavy landing sound.

Shrinking III + Feather Falling IV takes `0.76 * 0.52 = 0.3952` of original fall damage (60.48% reduction). Existing vanilla protections/caps remain. Existing vanilla immunity mechanisms remain possible; Shrinking itself does not add immunity.

| Shrinking tier | Walking vibration range | Sprinting vibration range |
| --- | ---: | ---: |
| I | 75% | 100% |
| II | 50% | 75% |
| III | 25% | 50% |

These percentages apply separately to each sensor/warden listener's radius. Crouching follows vanilla behavior. Block breaking, placing, containers, food and projectiles are not filtered.

| Swift Sneak | Normal / Shrinking I | Shrinking II | Shrinking III |
| --- | ---: | ---: | ---: |
| None | 30% | 30% | 30% |
| I | 45% | 50% | 55% |
| II | 60% | 70% | 80% |
| III | 75% | 85% | 100% |

The native sneaking-speed attribute caps at 1.0. Bonuses coexist with vanilla equipment modifiers. Percentages are relative to current walking speed, including the newly requested Shrinking walking penalty; Shrinking III + Swift Sneak III reaches its own walking speed (.76 of unmodified walking), not unmodified player speed.

Shrinking II ignores stone, polished blackstone and heavy weighted (iron) plates. Wooden and light weighted (gold) plates remain active. III ignores all plates using the shared base implementation. Farmland already has a vanilla volume threshold, so sufficiently small entities can naturally fail to trample at other levels; this mod adds an explicit guarantee only for III and does not override vanilla's threshold.

Soul sand retains 60/25/0% of its base penalty, giving speed factors .64/.85/1.0 from vanilla .4. Soul Speed's movement bonus and MOVEMENT_EFFICIENCY processing remain outside this hook. Berry stuck multipliers retain 2/3, 1/3, 0 of their penalty. At III the stuck call is skipped entirely: a multiplier of 1 would still clear velocity in vanilla. Bush damage remains unchanged.

## Mounted-bee rendering

`BeeRiderPose` samples the actual adult bee model at the same interpolated time as its rider. It computes the transform from the resting body frame to the animated `bone` frame, including mount scale, yaw and renderer rotations, then expresses it relative to the passenger. `AnimatedRiderRendererMixin` applies that immutable snapshot around the complete living-entity render submission, so rider, armor and held items follow the saddle's translation and tilt. It does not duplicate vanilla bobbing formulas or freeze the bee. Selecting the adult model explicitly avoids reusing a renderer's last submitted baby model. Snapshots clear on dismount; entity positions, collision boxes, camera and first-person hands remain untouched. The saddle layer still copies the animated body pose for deferred rendering.

## Steering-item attraction

26.2's `Pig.registerGoals` explicitly installs a `TemptGoal` for `CARROT_ON_A_STICK`; `Strider.registerGoals` uses `STRIDER_TEMPT_ITEMS`, which includes its fungus stick. `TemptGoal.shouldFollow` checks both hands. Scale Brews extends that predicate with the enabled definition's `steering_item`, preserving native foods and goal scheduling. `TinyMountGoalsMixin` lazily installs a fallback on the first server AI step only when the configured mob lacks a native temptation goal, after subclass goals and synced registries are available. `TinyMountTemptation` shares item matching with mounted steering and honors module/entity/definition switches. Native controllers (such as pigs and striders) remain owned by Minecraft.

## Datapack extension points

- `#scalebrews:insensitive_pressure_plates`: plates ignored at Shrinking II.
- `#scalebrews:growth_resisted_slowdown`: blocks whose native speed factor or stuck multiplier Growth can resist; defaults to soul sand and sweet berry bush.
- `#scalebrews:shrinking_quiet_movement`: movement events with reduced detection; defaults to step, hit_ground and swim.

Tag JSON lives in `data/scalebrews/tags/block` and `tags/game_event`. Datapacks can extend these with `replace: false`. A modded plate/terrain that implements unrelated custom mechanics may need its own integration. Cobweb, honey, powder snow, water and Slowness remain unchanged by default.
