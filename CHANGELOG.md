# Changelog

## Unreleased - half-block Shrinking III clearance

- Adjust Shrinking scale reduction from 24% to 24.2% per level: 0.758 / 0.516 / 0.274. Standing level-III players are approximately 0.4932 blocks tall and can move through a half-block gap. Preserve other pure-tier attribute values and mounting eligibility between levels.

## Unreleased - probabilistic wolf taming

- Replace the fixed third-ride tame with horse-style random rolls against persistent wolf trust. Failed full rides add 5 trust and failed accepted bones add 10; preserve vanilla direct bone taming and wild dismount hostility. Document the exact behavior in README.

## Unreleased - wolf attack animation

- Commanded wolf bites emit native swing progress and a transient combat pose for resource packs, including missed taps and pounce hits. Advance the wolf swing clock on server and client without creating anger or AI targets.

## Unreleased - mount gesture and terrain impact fixes

- Crouch + Use mounts configured tameables with any held item, without feeding, equipping or consuming it; ordinary Use retains those actions.
- Reset mounting Crouch protection on dismount so the same animal can be ridden repeatedly.
- Landing waves follow reachable collision surfaces with one-block ascent/descent, path-length range and falloff, and obstacle/gap checks.


## [0.1.0-beta.4] - Unreleased

- Crouch + Use with a spare saddle now mounts an already-saddled tameable instead of falling through to its sit interaction. Respect configured controls, preserve equipment/inventory, and exercise real client input in the wolf regression test.

- Rebalance Growth entity reach to +50% / +100% / +150%, following effective size so ordinary melee can reach small targets beside giant feet. Preserve block reach and Shrinking penalties; cover actual weapon range, mixed effects and external sizes with regression tests.

- Automatic physical support for compatible living species without per-model profiles; optional anatomical profiles remain authoritative, especially the animated player-head contact. Add cat/iron golem refinements and correct villager head height.
- Separate saddle equipment, riding and steering. Any player may saddle an adult tamed wolf; unsaddled Tiny Mounts permit passive riding. Accurate equipment/size/hostility messages in English and Spanish.
- Shift + right-click mounts configured tameables, preserving ordinary sit/feed interactions and requiring Shift release before dismounting.
- Wild wolf riding provides an alternative taming route with ejection and dismount retaliation; bones and existing owners remain intact.
- Material loot now uses actual scale raised to 1.6 instead of squared, retaining unbiased rounding, Looting and extra-roll composition.

## Unreleased � scale materials and wolf mounts

- Living platforms now accept bodies up to 85% of their support width (previously 60%), allowing closer-sized entities to stand and walk on them.
- Entity-scoped reloadable material drops based on actual scale (initially squared, superseded by beta.4's exponent 1.6), including installed-mod compatibility and intentional farm production.
- Shared tamed-wolf tiny mount with independent saddle/armor, direct controls and charged pounce.
- Separate Animal Weights bridge for physical-volume space and crowding.


Notable user-facing changes to Scale Brews are documented here.

## [0.1.0-beta.3] - Unreleased

- All scale-derived attributes and physical mechanics now use effective SCALE, including mixed Growth/Shrinking effects and external attribute scaling. Existing pure-potion values remain the balance anchors; continuous mechanics follow the physical blend.
- Growth landing waves require actual scale >= 1.96; landing damage additionally requires scale >= 3.88. Having a Growth icon alone no longer grants giant abilities.
- Right-click a profiled living support with a boat/raft item or spawn egg to place a supported entity when its entire footprint fits and space is clear. Inventory, creative mode and build permissions are respected.
- Retain mutable beacon extension lists so FrozenLib/Wilder Wild can register their powers after Scale Brews.
- Compatibility shim for e4mc 6.2.1's removed dedicated-server permission call; integrated-server sharing behavior is unchanged.
- Control Combatify CTS regeneration randomness in regression tests and validate development commands against loaded VP26 datapack registries.

## [0.1.0-beta.2] - Unreleased

- Living-platform subsystem: physical bodies can stand on larger living supports without becoming passengers; mob pathfinding is unchanged.
- Synced platform policies and anatomical surface profiles, support transport, vehicle/item/falling-block integration and client animation/camera corrections.
- Sixteen adult support profiles, extensible datapack policy/geometry, client visual and physical adapters, and player/vehicle movement references for delayed connections.
- Existing Tiny Mounts, effects, brewing, translations and Happy Ghast behavior are retained. No new artwork or translation keys are required.
- Dedicated-server/client coverage includes flight-disabled latency tests and prolonged player transport. Full VP26 acceptance is not claimed; external integration failures remain documented.

## [0.1.0-beta.1] - Unreleased

First public beta candidate for Minecraft 26.2 on Fabric.

### Added

- Growth and Shrinking potions with three strength levels, eight-minute level-I variants, splash and lingering forms.
- Gradual physical scaling, proportional health transitions, melee damage/knockback, reach, movement, jump, exhaustion, fall and vibration behavior.
- Beacon support for levels I and II.
- Size-aware world interactions: configurable Growth landing impacts, creeper explosions, villager fear, terrain resistance and Shrinking environmental protections.
- Configurable effective-scale mounting limits for living mounts.
- Tiny chicken and bee mounts, original saddle artwork, Flower on a Stick steering and JSON definitions for adding or disabling mounts.
- Natural mounted-bee bobbing with the rendered rider attached to the animated saddle.
- Steering-item attraction for enabled `item_steered` definitions while unmounted, preserving native food and breeding behavior.
- English and Spanish translations, server-owned JSON configuration and synchronized data registries.

### Compatibility

- Vanilla arrow and trident damage deliberately bypasses scale-based melee damage changes.
- Targeted runtime coverage for Combatify 1.4.0-26.2 and Alex's Mobs Continued 2.1.9.
- Client and server both require Fabric API 0.159.0+26.2 or newer for Minecraft 26.2.

### Known limits

- Custom projectile, camera, movement, renderer, brain-driven mob or multipart-entity implementations may require dedicated compatibility.
- Automated and isolated compatibility checks do not replace a full modpack playtest or multiplayer latency testing.
