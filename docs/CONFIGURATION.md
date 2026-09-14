# World configuration and tiny mounts

Since beta.3, Growth/Shrinking levels in the rules below mean the equivalent **effective size**, not a required potion icon. Mixed effects and external SCALE modifiers can enable or disable the same rules. See [Mechanics](MECHANICS.md#effective-size-beta3) for interpolation and exact thresholds. Configuration switches still govern the same modules.

Rules are JSON data owned by the world/server, not per-player client preferences. Fabric synchronizes both rules and mount definitions to connecting clients. All features default to enabled. Close and reopen the world (restart a dedicated server) after changing these dynamic registries; `/reload` alone is not supported for them.

## Installation

Copy `examples/world-config` into `<world>/datapacks/scalebrews-config`, edit its JSON, then reopen the world. Its `pack.mcmeta` targets Minecraft 26.2. Do not edit the mod JAR. On servers only the administrator needs to install this datapack. Existing items and stored saddles are not deleted when features are disabled.

The active rules file is `data/scalebrews/scalebrews/rules/default.json`. The repeated `scalebrews` is intentional: entry namespace, then registry namespace.

```json
{
  "tiny_mounts": true,
  "mounts": { "minecraft:chicken": true, "minecraft:bee": true, "minecraft:wolf": true },
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

- `tiny_mounts: false` disables Scale Brews' additional mounting interactions, manual control, steering-item attraction, saddle layer, animated rider integration, bee-hive overrides and Flower on a Stick crafting. The item stays registered for existing inventories. `mounts` disables particular entity IDs. Both must allow a mount, and its definition must also have `enabled: true` (the default).
- The living-mount size-ratio policy is **independent** of Tiny Mounts. Boats and minecarts are exempt. Growth is no longer an unconditional riding prohibition.
- `villager_fear` controls the additional local threat sensor for visible living entities at least two equivalent scale levels larger than the villager, within eight blocks. It does not disable vanilla hostile threats or change reputation.
- `growth_landing_impact` is the **single switch for the combined Growth landing effect**: radial knockback at I/II/III, the bounded additional damage at III, and its gust/terrain/sound feedback. Setting it to `false` disables the whole effect for players and compatible mobs. It does not change ordinary damage recoil, melee knockback, the falling entity's own fall damage or native landing events/particles. There are no separate push/damage switches.
- Migration: obsolete `growth_landing_knockback` / `growth_landing_damage` keys are accepted only for old files; if either is false, the combined effect is disabled. Remove both old keys and use only `growth_landing_impact` to configure new worlds or re-enable the effect. Encoding/network synchronization emits only the combined switch.
- `environment_interactions` is the master switch for the five following options. Disabling an option removes this mod's intervention; it does not force vanilla to trigger a plate or destroy a crop if vanilla itself would not do so.
- Missing keys retain enabled defaults. Invalid field types or invalid mount definitions produce a data-loading error; fix the file rather than replacing the world. Use JSON booleans, not quoted strings.

## Mount definitions

Defaults live at `data/scalebrews/scalebrews/tiny_mount/chicken.json`, `bee.json` and `wolf.json`. Override the same path to change a built-in definition. For another entity, add a new file under `data/<namespace>/scalebrews/tiny_mount/<name>.json`; use just one definition per entity.

```json
{
  "entity": "minecraft:bee",
  "enabled": true,
  "family": "item_steered",
  "max_rider_scale_ratio": 0.53,
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

### Mount families

`family` selects the vanilla-style interaction grammar. It is deliberately not a collection of independent booleans: choosing a family establishes its saddle, passenger, control and inventory invariants.

- `direct`: no taming gate. An eligible adult can carry a passenger without a saddle, but the vanilla saddle is required for manual control. Control is direct. The mount has the generic equipment menu. Chicken uses this family; the closest vanilla interaction pattern is the always-tamed camel.
- `tameable_direct`: the entity must be a `TamableAnimal`. It may carry a passive passenger before taming or saddling, allowing horse-style or entity-specific taming routes. Taming is required before Tiny Mount equipment becomes available; once tamed, the generic equipment menu is available even while unsaddled, so the saddle can be installed or removed there. The vanilla saddle is required for **manual control**, not for opening that menu. Ownership restrictions remain entity-specific; Wolf uses this family and keeps its riding-based taming route plus owner-protected BODY equipment.
- `item_steered`: Pig/Strider-style. A saddle is required even to mount. The configured `steering_item` is then required for manual control. This family has no mount inventory, so E opens the player's normal inventory while riding. Bee uses this family.

Every family uses the vanilla saddle in `EquipmentSlot.SADDLE`; `saddle` and `control` are no longer JSON settings. Old definitions using those keys must be migrated by adding `family` and removing the obsolete fields. This prevents contradictory states such as an item-steered mount that can be ridden unsaddled.

Saddle removal is not configurable. Minecraft 26.2 marks the vanilla saddle itself as shearable, so shears remove it whenever the entity's native equipment permissions allow that player to shear. The same principle applies to BODY equipment: shearability comes from the equipped item's `Equippable` component, not from Tiny Mount JSON. Vanilla equipment-slot ordering is preserved, so an entity wearing shearable BODY equipment and a saddle loses BODY first and SADDLE on the next shear. Wolf's vanilla owner-only shearing permission remains authoritative.

Inventory-bearing families may optionally expose native BODY equipment:

```json
"body_equipment": {
  "item": "minecraft:wolf_armor",
  "slot_icon": "scalebrews:container/slot/wolf_armor"
}
```

`body_equipment` is invalid for `item_steered`. The configured item must still be natively equippable into that entity's BODY slot; Scale Brews does not bypass the item's allowed-entity or equipment-slot rules. On `tameable_direct`, BODY management is owner-protected. Saddle borrowing is kept independent of BODY ownership, matching the Tiny Wolf gameplay contract.

`max_rider_scale_ratio` compares the rider's effective SCALE divided by the mount's effective SCALE. Defaults to 0.53 if omitted. Legacy `minimum_shrinking` fields are ignored: old files adopt the new ratio after their family schema is migrated. Replace that field explicitly when migrating; a former level-III-only configuration no longer imposes that level restriction.

Movement is independent of family. Types are `ground` and `flying_look_direction`. Speed accepts 0.01–1; 0.18 is a vanilla-style movement speed for ground movement and blocks/tick for controlled flight. Maximum pitch accepts 0–75 degrees, maximum vertical speed 0.01–0.5 blocks/tick. Start conservatively when tuning flight. Abilities are also independent of family; built-ins currently include `none`, `chicken_glide` and `wolf_pounce`.

## Living-mount size policy

Override `data/scalebrews/scalebrews/mount_size_policy/default.json` (included in the example configuration pack). `mounts` maps entity identifiers to maximum rider/mount SCALE ratios; `default_max_rider_scale_ratio` is the fallback for species without a rule. Ratios accept 0.0001–1024. This gate never grants permission to ride and never replaces native age, taming, saddle, state or passenger-capacity rules.

Precedence: an explicit general `mounts` ratio, then a tiny-mount definition's ratio, then the configurable fallback (1.0). Size gates remain active even if tiny-mount controls are disabled. To change the default tiny ratio, edit its definition or explicitly override its entity in the general policy. These registries synchronize to clients and require a world/server restart after editing.

Initial general values: horse/donkey/mule/skeleton horse/zombie horse/pig/strider/llama/trader llama 1.0; camel 1.1; happy ghast 2.0. Chicken and bee definitions supply 0.53; wolf supplies 0.76. Boat/minecart vehicles bypass this policy entirely. SCALE is the attribute multiplier, not a comparison of base model dimensions or baby proportions; native age checks are still separate.

Effective values include external modifiers and the current blend step. Existing riders are checked each server tick and safely dismounted if the ratio becomes too large. Adding/removing an effect alone does not dismount before the physical size changes. At settled default sizes, Growth II can ride a Growth II/III horse but not Growth I; Growth I cannot ride a normal horse. Shrinking II can ride a normal bee/chicken but not a Shrinking I mount. Shrinking III can ride a Shrinking I mount, but not a Shrinking II mount (0.274/0.516 exceeds 0.53).

`chicken_glide` uses the chicken's own fall damping while airborne Space is held; release removes that damping while ridden. It does not add ordinary jumps or change vanilla chicken fall-damage immunity. `wolf_pounce` remains the wolf-specific charged leap/attack package. Family, movement and ability are deliberately separate so another entity can reuse an interaction family without inheriting unrelated physics.

Only adult mounts are eligible for Tiny Mount equipment/riding rules. Native controller overrides on pigs, horses, striders and other entity classes are not replaced. Mods that inject a controller into the common `Mob` class, or completely replace movement/rendering, need separate compatibility testing.

## Saddle resources

Every Tiny Mount family requires `saddle_visual`; `item_steered` additionally requires `steering_item`. The client renders separate saddle geometry only while a vanilla saddle occupies the equipment slot. Its texture is a resource identifier pointing to a PNG, not a filesystem path or URL. Textures are **not** transferred by the datapack: distribute custom PNGs in a resource pack to clients (or use the bundled textures).

The reusable anchors currently supported are `body` (chicken-style quarter-turned torso) and `bone` (bee-style body animation group). Both use the supplied 64×32 UV layout. The base model must expose the selected root child. A new body shape may need an additional model adapter; changing an identifier alone cannot fit arbitrary anatomy. A missing anchor skips the saddle layer rather than crashing the renderer. Chicken variants and bee anger/nectar states keep their vanilla base textures.

Supported living Tiny Mounts share the mount model's final rendered attachment transform with both the saddle and passenger, after vanilla `setupAnim` and optional model animation have run. This keeps the saddle and rider on one animation source instead of reconstructing species-specific poses. Bees retain natural bobbing and rolling while ridden; the same path covers chicken, wolf and vanilla horse body animation. The pinned optional-mod proof passed with EMF 3.3.5, ETF 7.2 and Fresh Animations 1.10.5. Custom renderers that replace the expected model/root structure still need separate compatibility testing.

### Steering-item attraction

Every enabled `item_steered` definition also lets the unmounted mob follow a nearby player holding its `steering_item` in either hand. Neither a saddle nor Shrinking is required for attraction; the normal age/size/equipment checks still apply to mounting. `direct` and `tameable_direct` definitions do not gain this behavior. Missing item IDs do not match an empty hand.

Existing vanilla temptation goals retain their food predicates, speed, range and priority. A mob without one gets a MOVE/LOOK goal at priority 3, using navigation for pathfinding mobs and move control otherwise; its default range is 10 blocks when `TEMPT_RANGE` is absent. Modded brain-driven movement may need its own adapter. The added attraction stops while riding/carrying a passenger, with NoAI, or while targeting an enemy; it does not turn the item into food or change breeding. Configuration changes require restarting the world, as with the other synced definitions.

`tools/GenerateArt.java` hand-paints the leather, binding, stitches, buckles and flower overlay. Run it to reproduce the committed PNGs. The Flower on a Stick model references vanilla fishing-rod art; its flower is original. Recipe: fishing rod + `#minecraft:small_flowers`; there is one output item and no retained flower metadata.

## Implementation boundaries

`ScaleRules`, `MountSizePolicy` and `TinyMountDefinition` are synced codecs/registries. `MountSizePolicy` owns the shared size gate; `TinyMounts` owns the family grammar, optional tiny eligibility, equipment interaction and input strategies. Client/server inventory hooks consult the synced family dynamically; target entity classes do not need to implement a Tiny Mount-specific inventory interface. Vanilla player-input, equipment/shearing semantics and vehicle synchronization are reused. No permanent `NoAI` or gravity flag is set; removing the steering item releases manual control.

Mounted bees cannot enter a hive. The external `BeehiveBlockEntity.addOccupant` route dismounts players using native placement and clears accumulated fall distance before storage. Dismounting in midair does not teleport a player to the ground or grant permanent fall immunity.

Villagers detect Growth II/III players within eight blocks through their nearest-visible-threat sensor and use vanilla panic/flee/golem-summoning behavior. There are no gossip, reputation, attacker-memory or global hostility writes. Vanilla visibility/targetability filtering still applies.

The final direct-player attack knockback is multiplied by Growth 1.10/1.20/1.30 or Shrinking 0.90/0.80/0.70. Sprint/enchantment contributions are preserved. Projectiles, thorns and the separate landing-wave damage source are excluded. Vanilla arrow/trident damage does not read the shooter's scale-modified attack attribute; custom projectiles that read it need a mod-specific audit.

## Material loot datapacks

See [Scale loot](SCALE_LOOT.md) for reloadable entity/item rules, item tags, overrides and optional compatibility. Wolf uses the tiny-mount JSON registry with `family: "tameable_direct"`, BODY wolf armor and `max_rider_scale_ratio: 0.76`.
