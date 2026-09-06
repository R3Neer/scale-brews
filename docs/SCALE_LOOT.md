# Scale-dependent materials

Scale Brews multiplies eligible final loot quantities by `minecraft:scale ^ 1.6` (actual physical scale). Looting runs normally first. Fractional quantities use unbiased probabilistic rounding and can yield zero. Large results split into legal stacks with the original components. Applying Growth immediately before harvesting or killing is intentional.

## Datapacks

Place JSON at `data/<namespace>/scalebrews/scale_loot/<path>.json`:

```json
{
  "entity": "minecraft:creeper",
  "drops": ["minecraft:gunpowder"]
}
```

Each definition always has an entity context. Several files for one entity contribute a union; matching twice never applies the multiplier twice. Use the same resource path in a higher-priority pack to replace a bundled definition; `"enabled": false` disables that file. `drops` supports item IDs and item tags such as `"#minecraft:wool"`. `/reload` rebuilds the server's rules. Invalid JSON and unknown IDs in active compatibility definitions are reported once during loading. Missing optional entities are inert.

An optional `entity_tag` restricts a rule to an entity's command/NBT tag, for datapack mobs represented by vanilla entity types. For example, Deeper Dark's Shockwave is a `minecraft:pig` tagged `deeper_dark.shockwave`, not a registered `deeper_dark:shockwave` entity.

The death-loot consumer runs after loot-table processing, including Looting and Animal Weights' independent extra rolls. Equipment drops are outside this path. Body-material drops produced during player interaction or dispenser shearing use the same rules and a scoped guard prevents double processing. This also covers the installed Alex's Mobs snapping turtle's sheared Spiked Scute. Arbitrary items emitted elsewhere, XP, snow trails and equipment are not multiplied.

## Audited compatibility

The bundled definitions were audited against these installed Fabric 26.2 JARs:

- Alex's Mobs Continued `2.1.9-fabric+26.2`: includes Murmur Elastic Tendon, hides/fur/meat, feathers, mosquito sacs, Warped Muscle, wing fragments, distributed slime/jelly and mineral body materials. Cooked meat counterparts are included. Lobster Tail, singular horns, eyes, arms, claws, clothing and equipment remain excluded.
- Friends & Foes `4.0.27+mc26.2`: Moobloom beef/leather, Tuff Golem tuff and Wildfire Crown Fragment. This version has no registered Friends & Foes Copper Golem; Minecraft 26.2's vanilla Copper Golem copper ingot rule is included. Crab Claw is excluded.
- Wilder Wild `4.2.11-mc26.2`: Ostrich/Penguin feathers, Moobloom beef/leather, Jellyfish nematocysts by colour, Scorched string and Zombie Ostrich flesh. Crab Claw and Scorched Eye are excluded.
- Deeper Dark `4.4.1`: tagged Shockwave soul sand. Its catalyst is not a distributed material.
- Stormie's Spiders `3.3.0`: no separate loot tables or species-specific Java integration; vanilla Spider/Cave Spider string rules apply to their vanilla entity types.

Animal Weights remains optional. The separate `scalebrews_animalweights_compat` mod adjusts only its space/crowding calculation. Scale Brews does not depend on Animal Weights and does not change beacon targets.
