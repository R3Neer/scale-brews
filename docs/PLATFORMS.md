# Living platforms

Living platforms let smaller physical bodies stand on, move across and travel with larger living entities. This is independent of Tiny Mounts: it needs no saddle and does not create a passenger relationship.

## Rules

- Right-click a profiled support with a boat/raft item (including chest variants) or spawn egg to place it on the nearest defined surface. Placement requires the full oriented footprint to fit, a valid width ratio, clear space and build permission. Failed placement consumes nothing; creative placement preserves the item. The new body immediately joins the ordinary support/transport system, without becoming a passenger.

- The transported body's current bounding-box width must be at most 60% of the support's current width. Both dimensions include actual scale changes, including other mods.
- Players, mobs, boats/rafts, off-rail minecarts, dropped items and falling blocks can be transported. Passengers follow their existing vehicle; they do not acquire independent support. Projectiles are excluded.
- Mobs do not plan paths over living platforms. Falling, ordinary movement, pushing and leads can bring them onto a surface.
- Only top surfaces are added. Ordinary side collisions remain, except for the specific support/body pair when they would prevent contact with an anatomical surface.
- Support and supported body do not repel one another while in contact. Other entities, attacks and leads retain their normal effects.
- Sneaking protects a player's edge. Jumping releases support. Transport adds no extra jump impulse.
- Boats treat support as land with the configured friction. Water retains priority.
- Falling blocks remain entities while supported, with their falling timeout paused. Water reactions and actual terrain landings retain vanilla behavior.
- Teleports/discontinuities greater than four blocks release support rather than dragging a body across the world.
- Fall damage is retained; passive supported descent does not accumulate a fall. Landing on a living platform does not emit Growth's ground shockwave.

Built-in profiles cover adult players, ghasts, bees, chickens, cows, sheep, pigs, villagers, horses (including undead variants), donkeys, mules, camels and both llamas. Unconfigured species, babies, sleeping/swimming/fall-flying supports, sitting camels and passengers cannot provide these surfaces. Happy Ghast's native platform and stationary behavior remain vanilla.

## World-owned configuration

The server synchronizes both registries to clients. Like the other dynamic Scale Brews registries, install these definitions as datapack resources and restart/reopen the world after changing them.

Override `data/scalebrews/scalebrews/platform_policy/default.json`:

```json
{
  "enabled": true,
  "max_width_ratio": 0.6,
  "bodies": {
    "players": true,
    "mobs": true,
    "boats": true,
    "minecarts": true,
    "items": true,
    "falling_blocks": true
  },
  "supports": {
    "minecraft:bee": false
  }
}
```

Missing category/species switches default to enabled, but a support still needs a profile. Disabling this feature does not disable Tiny Mounts or the native Happy Ghast platform.

Add a profile at `data/<your_namespace>/scalebrews/entity_platform/<name>.json`:

```json
{
  "entity": "example:pack_animal",
  "enabled": true,
  "friction": 0.6,
  "max_width_ratio": 0.6,
  "surfaces": [
    {
      "id": "back",
      "x": 0,
      "y": 1.25,
      "z": 0,
      "width": 0.75,
      "depth": 1.1,
      "visual": {
        "part": "body",
        "x": 0,
        "y": -0.4,
        "z": 0
      }
    }
  ]
}
```

Physical coordinates are blocks at adult scale 1, relative to the feet: +Y up, +Z forward, +X right. Surfaces rotate with body yaw. `width` and `depth` describe an oriented rectangle, not its axis-aligned envelope. Different surface heights allow heads, backs and humps. A body's actual step height determines whether it can walk up to another surface.

Visual coordinates are blocks in the selected model part's local frame (model pixels divided by 16). They identify the resting contact point. Part paths follow children from the model root, such as `body/head` or `bone/body`. The client samples the animated model and translates the supported body's rendering while keeping it upright. Physical collisions remain server-owned and do not depend on render animations.

Use the same resource path as a built-in profile to override it through datapack priority. Two distinct resources defining the same species are an error; do not add a second species definition to override the first. Coordinates must be finite, dimensions positive and surface ids unique.

## Extension interfaces

- `Platforms.registerAdapter(entityTypeId, PhysicalAdapter)`: category, eligibility and optional `transport` constraint for special movement. The returned displacement still passes through collision resolution. Special movement implementations need integration testing; JSON alone does not guarantee compatibility.
- `PlatformVisuals.registerAdapter(entityTypeId, Adapter)`: client-only visual adapter for custom renderers. It returns a world-axis translation relative to the physical contact.
- Standard living-entity models can use the JSON part path. A missing/unavailable model part logs one diagnostic and leaves the physical surface active without the animation correction.

Camera corrections use approximately 100 ms smoothing, preserve the horizon, and are limited to the smaller of 0.25 blocks and half the camera entity's height. The camera origin and four near-plane corners are checked for block obstruction. Existing small-player near-plane corrections remain active. Teleport corrections reset the visual adjustment immediately; ordinary loss of support fades it out. A crouching giant lowers the physical player-head surface as well.

The standard model is sampled once per support and render fraction, sharing its animated transforms across surfaces and transported bodies. Model-part poses and visibility are restored after sampling. Renderer-specific adapters must return finite world-space translations and must not mutate gameplay state.

Copyable examples: [policy](platform-policy.example.json) and [profile](platform-profile.example.json). Place them at the datapack paths described above, with a current Minecraft 26.2 datapack `pack.mcmeta`; the example species is deliberately a placeholder.

## Authority and lifecycle

Server contacts are authoritative. S2C snapshots contain body/support ids, a stable surface id, a local contact and a tick sequence; tracking start sends the current relationship too. Local players and controlled vehicles predict shared physics. Other clients retain vanilla position interpolation and apply only the presentation adjustment.

Movement references precede player/vehicle movement packets and express the same requested position in the confirmed support's frame. The server consumes each reference once, requires matching ids and absolute coordinates, and bounds rebasing to four blocks. Vanilla movement-speed and collision checks still run on the resolved request. Only confirmed, physically touching support satisfies the flight check. Passive carry updates movement baselines rather than charging it as walking distance.

Support links are transient. They are not passengers and are not saved. Passenger roots, cycles, invalid sizes, special poses, removal, teleports and blocked transport are handled by the common state/transport layer. Item resting optimizations cannot skip server carry. Falling-block placement is deferred without cancelling its normal movement, water reaction or impact-damage path.

## Validation

The implementation has dedicated-server and real-client tests; acceptance is recorded in [VALIDATION](VALIDATION.md) and TODO rather than inferred from compilation. The moving-support proof passed with players, mobs and an occupied boat. Player and occupied-boat network tests passed with flight disabled at 0, 100 and 200 ms additional round-trip latency. A 12,000-tick player soak passed; this is ten minutes total, split across the three latencies, not ten minutes at each latency.

The sixteen built-in profiles pass client model-path and finite-transform checks. These are not a substitute for human animation/contact review for every species and resource pack. The owner reports successful singleplayer testing. Beta.3 passes all 98 VP26 server tests; full client acceptance still encounters an `enchancement:empty` network-registry conflict, recorded in VALIDATION. No general modpack-compatibility claim follows from the bounded tests.

Run the short suite with `./gradlew runGameTest runClientGameTest`. Add `-PscalebrewsPlatformSoak=true` for the dedicated connection's ten-minute matrix (0, 100 and 200 ms additional round-trip latency).
