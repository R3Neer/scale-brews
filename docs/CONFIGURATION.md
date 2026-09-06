# World configuration and tiny mounts

Since beta.3, Growth/Shrinking levels in the rules below mean the equivalent **effective size**, not a required potion icon. Mixed effects and external SCALE modifiers can enable or disable the same rules. See [Mechanics](MECHANICS.md#effective-size-beta3) for interpolation and exact thresholds. Configuration switches still govern the same modules.

Rules are JSON data owned by the world/server, not per-player client preferences. Fabric synchronizes both rules and mount definitions to connecting clients. All features default to enabled. Close and reopen the world (restart a dedicated server) after changing these dynamic registries; `/reload` alone is not supported for them.

## Installation

Copy `examples/world-config` into `<world>/datapacks/scalebrews-config`, edit its JSON, then reopen the world. Its `pack.mcmeta` targets Minecraft 26.2. Do not edit the mod JAR. On servers only the administrator needs to install this datapack. Existing items and stored saddles are not deleted when features are disabled.

The active rules file is `data/scalebrews/scalebrews/rules/default.json`. The repeated `scalebrews` is intentional: entry namespace, then registry namespace.

```json
{
  "tiny_mounts": true,
  "mounts": { "minecraft:chicken": true, "minecraft:bee": true },
  "villager_fear": true,
  "growth_landing_impact": true,
  "environment_interactions": true,
  "farmland_protection": true,
  "pressure_plate_bypass": true,
  "turtle_egg_protection": true,
  "quiet_movement": true,
  "growth_terrain_resistance": true
}
```

- `tiny_mounts: false` disables Scale Brews' additional mounting interactions, manual control, steering-item attraction, saddle layer, animated bee rider, bee-hive overrides and Flower on a Stick crafting. The item stays registered for existing inventories. `mounts` disables particular entity IDs. Both must allow a mount, and its definition must also have `enabled: true` (the default).
- The living-mount size-ratio policy is **independent** of Tiny Mounts. Boats and minecarts are exempt. Growth is no longer an unconditional riding prohibition.
- `villager_fear` controls the local Growth II/III threat sensor, not vanilla hostile mobs or reputation.
- `growth_landing_impact` is the **single switch for the combined Growth landing effect**: radial knockback at I/II/III, the bounded additional damage at III, and its gust/terrain/sound feedback. Setting it to `false` disables the whole effect for players and compatible mobs. It does not change ordinary damage recoil, melee knockback, the falling entity's own fall damage or native landing events/particles. There are no separate push/damage switches.
- Migration: obsolete `growth_landing_knockback` / `growth_landing_damage` keys are accepted only for old files; if either is false, the combined effect is disabled. Remove both old keys and use only `growth_landing_impact` to configure new worlds or re-enable the effect. Encoding/network synchronization emits only the combined switch.
- `environment_interactions` is the master switch for the five following options. Disabling an option removes this mod's intervention; it does not force vanilla to trigger a plate or destroy a crop if vanilla itself would not do so.
- Missing keys retain enabled defaults. Invalid field types or invalid mount definitions produce a data-loading error; fix the file rather than replacing the world. Use JSON booleans, not quoted strings.

## Mount definitions

Defaults live at `data/scalebrews/scalebrews/tiny_mount/chicken.json` and `bee.json`. Override the same path to change a built-in definition. For another entity, add a new file under `data/<namespace>/scalebrews/tiny_mount/<name>.json`; use just one definition per entity.

```json
{
  "entity": "minecraft:bee",
  "enabled": true,
  "max_rider_scale_ratio": 0.53,
  "saddle": true,
  "control": "item_steered",
  "movement": "flying_look_direction",
  "steering_item": "scalebrews:flower_on_a_stick",
  "speed": 0.18,
  "max_pitch": 60,
  "max_vertical_speed": 0.15,
  "ability": "none",
  "saddle_visual": {
    "texture": "scalebrews:textures/entity/saddle/bee.png",
    "anchor": "bone"
  }
}
```

`max_rider_scale_ratio` compares the rider's effective SCALE divided by the mount's effective SCALE. Defaults to 0.53 if omitted. Legacy `minimum_shrinking` fields are ignored: old files adopt the new ratio without losing saddle visuals or controls. Replace that field explicitly when migrating; a former level-III-only configuration no longer imposes that level restriction. Control types are `direct` (WASD) and `item_steered` (requires the named item in either hand). Movement types are `ground` and `flying_look_direction`. Speed accepts 0.01–1; the default 0.18 is a vanilla-style movement speed for ground movement and blocks/tick for controlled flight. Maximum pitch accepts 0–75 degrees, maximum vertical speed 0.01–0.5 blocks/tick. Start conservatively when tuning flight.

## Living-mount size policy

Override `data/scalebrews/scalebrews/mount_size_policy/default.json` (included in the example configuration pack). `mounts` maps entity identifiers to maximum rider/mount SCALE ratios; `default_max_rider_scale_ratio` is the fallback for species without a rule. Ratios accept 0.0001–1024. This gate never grants permission to ride and never replaces native age, taming, saddle, state or passenger-capacity rules.

Precedence: an explicit general `mounts` ratio, then a tiny-mount definition's ratio, then the configurable fallback (1.0). Size gates remain active even if tiny-mount controls are disabled. To change the default tiny ratio, edit its definition or explicitly override its entity in the general policy. These registries synchronize to clients and require a world/server restart after editing.

Initial general values: horse/donkey/mule/skeleton horse/zombie horse/pig/strider/llama/trader llama 1.0; camel 1.1; happy ghast 2.0. Chicken and bee definitions supply 0.53. Boat/minecart vehicles bypass this policy entirely. SCALE is the attribute multiplier, not a comparison of base model dimensions or baby proportions; native age checks are still separate.

Effective values include external modifiers and the current blend step. Existing riders are checked each server tick and safely dismounted if the ratio becomes too large. Adding/removing an effect alone does not dismount before the physical size changes. At settled default sizes, Growth II can ride a Growth II/III horse but not Growth I; Growth I cannot ride a normal horse. Shrinking II can ride a normal bee/chicken but not a Shrinking I mount. Shrinking III can ride a Shrinking I mount, but not a Shrinking II mount (0.274/0.516 exceeds 0.53).

Abilities are `none` and `chicken_glide`. The latter uses the chicken's own fall damping while airborne Space is held; release removes that damping while ridden. It does not add ordinary jumps or change vanilla chicken fall-damage immunity. This ability's integration is specific to chickens; generic controls and movement can be reused by other entities without Java changes.

Only adult chicken and bee are included initially. Babies are excluded. No taming, stamina, dash, jump-charge HUD or multi-passenger mode is added. Native controller overrides on pigs, horses, striders and other entity classes are not replaced. Mods that inject a controller into the common `Mob` class, or completely replace movement/rendering, need separate compatibility testing.

## Saddle resources

A definition that requires a saddle must supply `saddle_visual`; `item_steered` must supply `steering_item`. The client renders separate saddle geometry only while a vanilla saddle occupies the equipment slot. Its texture is a resource identifier pointing to a PNG, not a filesystem path or URL. Textures are **not** transferred by the datapack: distribute custom PNGs in a resource pack to clients (or use the bundled textures).

The reusable anchors currently supported are `body` (chicken-style quarter-turned torso) and `bone` (bee-style body animation group). Both use the supplied 64×32 UV layout. The base model must expose the selected root child. A new body shape may need an additional model adapter; changing an identifier alone cannot fit arbitrary anatomy. A missing anchor skips the saddle layer rather than crashing the renderer. Chicken variants and bee anger/nectar states keep their vanilla base textures.

Enabled tiny-mount bees retain natural body bobbing and rolling while carrying a player. The rendered rider follows the animated saddle, including its tilt, without moving the physical entity, collision box or first-person camera. This does not require a new texture or JSON field and remains active without the steering item. Custom renderers that replace the bee model need separate compatibility testing.

### Steering-item attraction

Every enabled `item_steered` definition also lets the unmounted mob follow a nearby player holding its `steering_item` in either hand. Neither a saddle nor Shrinking is required for attraction; the normal age/size/equipment checks still apply to mounting. `direct` definitions do not gain this behavior. Missing item IDs do not match an empty hand.

Existing vanilla temptation goals retain their food predicates, speed, range and priority. A mob without one gets a MOVE/LOOK goal at priority 3, using navigation for pathfinding mobs and move control otherwise; its default range is 10 blocks when `TEMPT_RANGE` is absent. Modded brain-driven movement may need its own adapter. The added attraction stops while riding/carrying a passenger, with NoAI, or while targeting an enemy; it does not turn the item into food or change breeding. Configuration changes require restarting the world, as with the other synced definitions.

`tools/GenerateArt.java` hand-paints the leather, binding, stitches, buckles and flower overlay. Run it to reproduce the committed PNGs. The Flower on a Stick model references vanilla fishing-rod art; its flower is original. Recipe: fishing rod + `#minecraft:small_flowers`; there is one output item and no retained flower metadata.

## Implementation boundaries

`ScaleRules`, `MountSizePolicy` and `TinyMountDefinition` are synced codecs/registries. `MountSizePolicy` owns the shared size gate; `TinyMounts` owns optional tiny eligibility, equipment interaction and input strategies. Small mixins connect entity mounting, mob interaction/controller, living-entity travel and chicken glide. Vanilla player-input and vehicle synchronization are reused. No permanent `NoAI` or gravity flag is set; removing the steering item releases manual control.

Mounted bees cannot enter a hive. The external `BeehiveBlockEntity.addOccupant` route dismounts players using native placement and clears accumulated fall distance before storage. Dismounting in midair does not teleport a player to the ground or grant permanent fall immunity.

Villagers detect Growth II/III players within eight blocks through their nearest-visible-threat sensor and use vanilla panic/flee/golem-summoning behavior. There are no gossip, reputation, attacker-memory or global hostility writes. Vanilla visibility/targetability filtering still applies.

The final direct-player attack knockback is multiplied by Growth 1.10/1.20/1.30 or Shrinking 0.90/0.80/0.70. Sprint/enchantment contributions are preserved. Projectiles, thorns and the separate landing-wave damage source are excluded. Vanilla arrow/trident damage does not read the shooter's scale-modified attack attribute; custom projectiles that read it need a mod-specific audit.

## Material loot datapacks

See [Scale loot](SCALE_LOOT.md) for reloadable entity/item rules, item tags, overrides and optional compatibility. Wolf uses the existing tiny-mount JSON registry and `max_rider_scale_ratio: 0.76`.
