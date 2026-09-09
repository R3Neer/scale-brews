# Scale Brews — player guide

[← Back to the discovery-first README](../README.md)

This is the spoiler-rich reference: exact recipes, numbers and interactions follow.
Start with the README if you would rather discover them in play.

## Contents

- [Installation](#installation)
- [Brewing](#brew-your-first-potions)
- [Size and movement](#bigger-or-smaller)
- [World interactions and beacons](#size-changes-the-world)
- [Platforms and mounts](#living-platforms-and-tiny-mounts)
- [Material drops](#size-dependent-materials)
- [World configuration](#world-configuration)
- [Compatibility and validation](#compatibility-and-validation)
- [Development](#development)
- [License and artwork](#license-and-artwork)

## Overview

Growth and Shrinking potions for **Minecraft 26.2 on Fabric**. Change your physical size, trade strength for agility, ride chickens, bees and tamed wolves, and bring size-based mechanics to the world around you.

Three potion levels, gradual size transitions, beacon powers, size-dependent material drops, English and Spanish translations, and server-owned JSON configuration are included. This is beta software: see [Validation](VALIDATION.md) for tested behavior and remaining playtesting limits.

![Three villagers at Shrinking III, normal size and Growth III, from left to right](images/size-comparison.png)

*Shrinking III → normal → Growth III. Same species, same ground level, different possibilities.*

Living platforms let smaller entities and vehicles use larger creatures as moving surfaces, without mounting them or adding mob navigation routes. Compatible living species have automatic upper support surfaces; optional profiles refine the contact, including the animated player head. These are **not full-body mesh collisions**. See [Living platforms](PLATFORMS.md).

> **In development, not enabled in normal gameplay:** a replacement system for all-direction anatomical collisions and shared Clinging Reoriented physics. Its source and tests are included in this repository, but its implementation and multiplayer validation are unfinished. See [development status](ANATOMY_IMPLEMENTATION.md); do not treat it as a feature of the current beta.

## Installation

Requires **Java 25**, **Fabric Loader 0.19.5 or newer**, and **Fabric API 0.159.0+26.2 or newer** for Minecraft 26.2.

1. Download a regular mod JAR from [GitHub releases](https://github.com/R3Neer/scale-brews/releases), obtain one from a successful [GitHub Actions build](https://github.com/R3Neer/scale-brews/actions/workflows/build.yml), or [build it yourself](#development).
2. Put the mod JAR and Fabric API in your instance's `mods` directory. Use the regular JAR, not the `-sources.jar`.
3. For multiplayer, install the mod and Fabric API on **both the server and every client**.

Combatify and Alex's Mobs Continued are optional, not required dependencies. Their tested versions and the exact limits of that compatibility coverage are recorded in [Validation](VALIDATION.md).

## Brew your first potions

Start with an Awkward Potion in a brewing stand.

| Input | Ingredient | Result |
| --- | --- | --- |
| Awkward Potion | Slime Ball* | Growth I |
| Growth I / II | Glowstone Dust | Growth II / III |
| Growth I / Shrinking I | Redstone Dust | Extended level I (8 minutes) |
| Extended Growth I | Fermented Spider Eye | Extended Shrinking I |
| Growth, any level | Fermented Spider Eye | Shrinking at the same level |
| Shrinking I / II | Glowstone Dust | Shrinking II / III |
| Either family | Gunpowder, then Dragon's Breath | Splash, then lingering form |

*If `alexsmobs:elastic_tendon` is registered, **Elastic Tendon replaces Slime Ball** as the starting ingredient.

Base durations are **3 minutes / 90 seconds / 45 seconds** for levels I / II / III. Redstone extends level I to **8 minutes**. Extended potions cannot accept glowstone; levels II/III cannot accept redstone. Splash and lingering conversions retain the extended potion, with vanilla delivery rules (a lingering effect has one quarter of the base duration).

## Bigger or smaller?

**Growth** makes you larger, tougher and stronger, with more health, interaction reach and step height. Walking feels like longer strides, while sprinting adds progressively less acceleration. Physical activity costs more exhaustion.

Growth's entity reach increases by 50% / 100% / 150% at its pure-potion sizes: 4.5 / 6 / 7.5 blocks from a survival baseline of 3. This keeps small targets beside a giant's feet reachable from eye height. Block reach retains +20% / +40% / +60%; Shrinking's reach penalties are unchanged. Entity reach follows effective size throughout blending and mixed effects, continuing the same linear growth curve beyond Growth III for external scales (subject to vanilla attribute limits). Other reach modifiers still compose multiplicatively; explicit item attack-range components may use their own ranges.

**Shrinking** trades health, damage and reach for a smaller body, explosive sprinting, slightly stronger jumps and lower physical exhaustion. It also reduces fall damage and movement-vibration detection range, with Swift Sneak synergy at levels II and III.

| Default effect | I | II | III |
| --- | ---: | ---: | ---: |
| Growth size | 1.96× | 2.92× | 3.88× |
| Shrinking size | 0.758× | 0.516× | 0.274× |
| Growth walking speed | 1.08× | 1.16× | 1.24× |
| Growth sprint multiplier | 1.20× | 1.12× | 1.05× |
| Shrinking walking speed | 0.92× | 0.84× | 0.76× |
| Shrinking sprint multiplier | 1.50× | 1.90× | 2.50× |

Size and walking values are relative to normal, without other modifiers. Sprint multiplies the **current walking speed**, replacing vanilla's 1.30× sprint factor. On ordinary ground, neither effect alone beats Speed of the same level while sprinting; Speed still stacks with both.

Shrinking reduces scale by 24.2% per level. At level III, a standing player's collision height is approximately **0.4932 blocks**, fitting through a half-block-high gap once the size transition completes.

Physical size blends over **20 ticks** when effects begin, change or end. All scale-derived attributes follow the current effective size, including mixed potions and external scale modifiers; health changes preserve your remaining-health percentage. The tables describe pure-potion endpoints. At normal size there are no giant or tiny bonuses, even with potion icons active. Vanilla arrow and trident damage is not scaled by the shooter's size.

The small-player first-person camera uses size-aware near clipping and reduced view bobbing to mitigate seeing through close block corners. This does not replace collision physics or change your FOV.

See [Mechanics](MECHANICS.md) for exact attribute, exhaustion, fall and stealth behavior.

## Size changes the world

- **Landing impacts:** At an actual size of at least 1.96× (Growth I), falls above three blocks push nearby entities, with gusts, ground particles and sound. At least 3.88× (Growth III) and a fall above six blocks are required for radial damage, capped at two hearts before defenses and reduced with distance. Potion icons alone do not qualify. Players and mobs with compatible fall physics can trigger the wave; their own fall damage remains intact. The wave follows connected terrain surfaces, climbing or descending at most one block per step. Its range and falloff use the traveled path length, so stairs and short detours work while gaps and larger ledges block it. The wave does not destroy blocks.
- **Creepers:** Growth multiplies actual explosion power by 1.15 / 1.30 / 1.50; Shrinking by 0.90 / 0.80 / 0.65. Vanilla blast radius, damage and block destruction follow that power. Charged creepers receive the same multipliers without an extra cap.
- **Quiet footsteps and protected ground:** Shrinking II players bypass stone, polished-blackstone and iron pressure plates. III bypasses all standard pressure plates and cannot trample farmland or turtle eggs.
- **Terrain and villagers:** Growth players resist soul sand and sweet berry bush slowdown without removing berry damage. Villagers fear visible living entities at least two equivalent scale levels larger than themselves within eight blocks: Shrinking I → Growth I, normal → Growth II, or Shrinking II → normal. Effective SCALE determines the comparison, including transitions and external modifiers; equal scales do not trigger size fear. Vanilla hostile threats remain unchanged, without adding reputation penalties or automatic golem hostility.
- **Beacons:** Both effects become primary-power choices with a three-layer pyramid. A full pyramid can upgrade either to level II; III remains potion-only.

## Living platforms and tiny mounts

### Stand on larger creatures

Physical support and riding are different systems. A supported body keeps its own movement and does not become a passenger. By default its **physical width must be at most 85% of the support's width**; both widths include their actual scale. Per-species policies can override this ratio.

Players, compatible mobs, boats and rafts (including chest variants), off-rail minecarts, dropped items and falling blocks can use these upper surfaces. Mobs may land there incidentally, but do not plan paths over other mobs. Happy Ghast behavior remains vanilla. Special poses and incompatible entities have limitations described in [Living platforms](PLATFORMS.md).

Use a boat item or spawn egg on an eligible living surface to place its entity when there is enough room and support. This respects placement permissions and consumes the item normally outside Creative mode.

### Ride and steer

Equip an adult chicken, bee or tamed wolf with a saddle; equipping does not require being small. Riding does require the appropriate size ratio.

For wolves and other configured tameables, hold **Crouch + Use** (your configured controls, normally Shift + right-click) with any item or an empty hand. This gesture mounts without using the held item, including food and saddles. Release Crouch before pressing it again to dismount; you can then mount the same animal again. Use without Crouch retains feeding, sitting and saddle equipment.

| Mount | Controls |
| --- | --- |
| Wolf | WASD movement; tap Space to bite, or hold and release for a pounce. Wolf Armor and saddle coexist. |
| Chicken | WASD movement; hold Space while falling to glide. No ordinary jump. |
| Bee | Hold a Flower on a Stick and look where you want to fly. Remove the item to release manual control. |

Craft **Flower on a Stick** from a fishing rod and any vanilla small flower. Chicken and bee saddles have separate hand-authored visual layers; wolves can wear their saddle and Wolf Armor together. Each tiny mount carries one rider. Mounted bees stay out of hives.

![A bee, chicken and tamed wolf wearing their saddles](images/tiny-mounts.png)

*The three default tiny mounts, shown with saddles. Equipping a saddle does not require Shrinking; riding depends on relative size.*

Bees keep their natural bobbing and rolling while ridden: the rendered rider follows the saddle, while first-person camera and collision physics stay stable.

A tamed wolf can be shared by compatible riders without changing its owner. Its default maximum rider/mount scale ratio is **0.76**.

Wild adult wolves can be tamed by riding, with **random success based on accumulated trust**, similar to horse temper; there is no fixed three-ride guarantee. Each complete three-second ride rolls against the wolf's trust (0–100). Failure adds 5 trust and ejects you; an accepted bone that does not tame it adds 10 trust. Bones retain their vanilla 1-in-3 chance to tame directly.

Trust belongs to the wolf and survives changing riders, unloading and restarting the world. Partial rides add nothing. With no prior trust the first ride fails; at 100 trust the next complete ride succeeds. Wild dismounts still provoke the wolf, and angry wolves reject bones. Unsaddled riding grants neither steering nor pounce, even after taming.

An unmounted bee also follows a Flower on a Stick in either hand, without needing a saddle or Shrinking. This mirrors native pig/strider steering-item attraction. The same behavior applies to enabled JSON `item_steered` tiny mounts using their configured `steering_item`; native foods and breeding are unchanged.

Riding eligibility uses the rider's **effective SCALE divided by the mount's**, including gradual transitions and other attribute modifiers. Chicken and bee default to a maximum ratio of **0.53**: Shrinking II/III fits a normal mount, but shrinking the mount can make it too small.

Other living mounts have configurable limits: most default to 1.0, camels to 1.1 and happy ghasts to 2.0. Growth II can ride a Growth II/III horse. Native age, taming, saddle and passenger rules still apply; this policy never grants new riding permissions. Boats and minecarts are exempt.

## Size-dependent materials

Eligible material drops scale approximately as **actual size raised to 1.6**. Growing a creature before harvesting it is intentional; shrinking it can yield fewer materials or none. Fractional amounts use probabilistic rounding rather than always rounding down, and Looting applies normally before scaling.

Only materials selected by entity-scoped rules are affected: this is **not a multiplier for every drop**. Equipment and XP are excluded. Death loot and supported harvesting/shearing interactions share the rules without applying the multiplier twice. Datapacks can choose item IDs or tags for each species; bundled optional-mod rules remain inert when those species are absent. See [material loot configuration and audited compatibility](SCALE_LOOT.md).

## World configuration

Copy the [example datapack](../examples/world-config) into your world's `datapacks` directory and edit its JSON. Gameplay registry settings belong to the server/world and synchronize to clients. **Restart the server or reopen the world** after changing these registries. Material-loot definitions are a separate resource system and **do support `/reload`**.

The released optional mechanics below default to enabled (this does not enable the anatomical prototype). Configure:

- Tiny Mounts as a whole, individual mount types, controls and saddle visuals.
- Living-mount size ratios, independently of the Tiny Mounts switch.
- Living-platform categories, support species, friction and physical-width ratios.
- Entity-scoped material drops through the separate reloadable loot definitions.
- Villager fear.
- The combined Growth landing impact.
- Environmental interactions, with individual controls for farmland, pressure plates, turtle eggs, quiet movement and terrain resistance.

**`growth_landing_impact: false` disables the entire landing wave:** radial knockback, additional level-III damage and its visual/sound feedback. It does not disable ordinary melee knockback, damage recoil or the falling entity's own fall damage.

See [Configuration](CONFIGURATION.md) for paths, complete JSON examples, extension points and migration from older settings.

## Compatibility and validation

The current suite includes server GameTests plus a real client/integrated-server test covering camera, beacons, resources, synchronization, tiny-mount input, animated bee riders and unmounted steering-item attraction.

Both run with the base Fabric setup and were also tested with **Combatify 1.4.0-26.2** and **Alex's Mobs Continued 2.1.9**, including their required dependencies. Targeted checks cover weapon-dependent reach, attack knockback, tendon brewing, modded-mob landings, small-player corner collision and elytra eligibility.

These are bounded integration tests, **not a guarantee for every modpack, configuration or future version**. Custom projectile, camera, movement or multipart-entity implementations may need additional integration. See [Validation](VALIDATION.md) for coverage and known limitations.

## Development

Use the included Gradle wrapper; a separate Gradle installation is not needed.

```powershell
# Build the JAR and run server GameTests
.\gradlew.bat build

# Run the real client/integrated-server GameTest
.\gradlew.bat runClientGameTest

# Launch a development client
.\gradlew.bat runClient
```

On Linux/macOS, use `./gradlew` instead. JARs are written to `build/libs` by default.

For a checkout in OneDrive, redirect generated output with `-PscalebrewsBuildDir=C:/path/to/local/build`. Test directories remain under `build/run`; tests use development worlds rather than your normal saves. Optional compatibility JARs can be supplied with `-PscalebrewsCompatMods=C:/path/to/test-mods`; they are not bundled in the production mod.

See [Changelog](../CHANGELOG.md), [Contributing and Git policy](../CONTRIBUTING.md), [Mechanics](MECHANICS.md) and [Work tracking](../TODO.md).

## License and artwork

Licensed under [GPL-3.0-or-later](../LICENSE).

Effect icons, saddle textures and the flower overlay are hand-authored pixel art, reproducible with `java tools/GenerateArt.java`; no image-generation models are used. Generated assets share the repository license. Minecraft textures, particles and sounds are reused by reference rather than redistributed. See [Art direction](ART.md).

README screenshots are actual Minecraft gameplay captures, not generated illustrations; underlying Minecraft artwork remains the property of its respective owners. Reproduce these disposable scenes with `./gradlew runClientGameTest -PscalebrewsReadmeCapture=true`.

Developer references: [material loot datapacks](SCALE_LOOT.md) and [wolf mount implementation](WOLF_MOUNT.md).
