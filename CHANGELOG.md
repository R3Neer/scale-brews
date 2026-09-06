# Changelog

Notable user-facing changes to Scale Brews are documented here.

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
