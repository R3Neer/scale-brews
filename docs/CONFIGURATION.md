# World configuration and tiny mounts

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
  "growth_landing_knockback": true,
  "growth_landing_damage": true,
  "environment_interactions": true,
  "farmland_protection": true,
  "pressure_plate_bypass": true,
  "turtle_egg_protection": true,
  "quiet_movement": true,
  "growth_terrain_resistance": true
}
```

- `tiny_mounts: false` disables Scale Brews' additional mounting interactions and manual control. `mounts` disables particular entity IDs. Both must allow a mount, and its definition must also have `enabled: true` (the default).
- Growth's prohibition on riding living entities is **independent** of Tiny Mounts. Boats and minecarts remain allowed.
- `villager_fear` controls the local Growth II/III threat sensor, not vanilla hostile mobs or reputation.
- `growth_landing_impact` disables the entire landing shockwave, including particles/sound. Its two subordinate options independently disable radial push and radial damage; disabling both leaves cosmetic impact feedback. They do not change the player's received fall damage or melee knockback.
- `environment_interactions` is the master switch for the five following options. Disabling an option removes this mod's intervention; it does not force vanilla to trigger a plate or destroy a crop if vanilla itself would not do so.
- Missing keys retain enabled defaults. Invalid field types or invalid mount definitions produce a data-loading error; fix the file rather than replacing the world. Use JSON booleans, not quoted strings.

## Mount definitions

Defaults live at `data/scalebrews/scalebrews/tiny_mount/chicken.json` and `bee.json`. Override the same path to change a built-in definition. For another entity, add a new file under `data/<namespace>/scalebrews/tiny_mount/<name>.json`; use just one definition per entity.

```json
{
  "entity": "minecraft:bee",
  "enabled": true,
  "minimum_shrinking": 2,
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

`minimum_shrinking` accepts 2 or 3. Control types are `direct` (WASD) and `item_steered` (requires the named item in either hand). Movement types are `ground` and `flying_look_direction`. Speed accepts 0.01–1; the default 0.18 is a vanilla-style movement speed for ground movement and blocks/tick for controlled flight. Maximum pitch accepts 0–75 degrees, maximum vertical speed 0.01–0.5 blocks/tick. Start conservatively when tuning flight.

Abilities are `none` and `chicken_glide`. The latter uses the chicken's own fall damping while airborne Space is held; release removes that damping while ridden. It does not add ordinary jumps or change vanilla chicken fall-damage immunity. This ability's integration is specific to chickens; generic controls and movement can be reused by other entities without Java changes.

Only adult chicken and bee are included initially. Babies are excluded. No taming, stamina, dash, jump-charge HUD or multi-passenger mode is added. Native controller overrides on pigs, horses, striders and other entity classes are not replaced. Mods that inject a controller into the common `Mob` class, or completely replace movement/rendering, need separate compatibility testing.

## Saddle resources

A definition that requires a saddle must supply `saddle_visual`; `item_steered` must supply `steering_item`. The client renders separate saddle geometry only while a vanilla saddle occupies the equipment slot. Its texture is a resource identifier pointing to a PNG, not a filesystem path or URL. Textures are **not** transferred by the datapack: distribute custom PNGs in a resource pack to clients (or use the bundled textures).

The reusable anchors currently supported are `body` (chicken-style quarter-turned torso) and `bone` (bee-style body animation group). Both use the supplied 64×32 UV layout. The base model must expose the selected root child. A new body shape may need an additional model adapter; changing an identifier alone cannot fit arbitrary anatomy. A missing anchor skips the saddle layer rather than crashing the renderer. Chicken variants and bee anger/nectar states keep their vanilla base textures.

`tools/GenerateArt.java` hand-paints the leather, binding, stitches, buckles and flower overlay. Run it to reproduce the committed PNGs. The Flower on a Stick model references vanilla fishing-rod art; its flower is original. Recipe: fishing rod + `#minecraft:small_flowers`; there is one output item and no retained flower metadata.

## Implementation boundaries

`ScaleRules` and `TinyMountDefinition` are synced codecs/registries. `TinyMounts` owns eligibility, equipment interaction and input strategies. Small mixins connect entity mounting, mob interaction/controller, living-entity travel and chicken glide. Vanilla player-input and vehicle synchronization are reused. No permanent `NoAI` or gravity flag is set; removing the steering item releases manual control.

Mounted bees cannot enter a hive. The external `BeehiveBlockEntity.addOccupant` route dismounts players using native placement and clears accumulated fall distance before storage. Dismounting in midair does not teleport a player to the ground or grant permanent fall immunity.

Villagers detect Growth II/III players within eight blocks through their nearest-visible-threat sensor and use vanilla panic/flee/golem-summoning behavior. There are no gossip, reputation, attacker-memory or global hostility writes. Vanilla visibility/targetability filtering still applies.

The final direct-player attack knockback is multiplied by Growth 1.10/1.20/1.30 or Shrinking 0.90/0.80/0.70. Sprint/enchantment contributions are preserved. Projectiles, thorns and the separate landing-wave damage source are excluded. Vanilla arrow/trident damage does not read the shooter's scale-modified attack attribute; custom projectiles that read it need a mod-specific audit.
