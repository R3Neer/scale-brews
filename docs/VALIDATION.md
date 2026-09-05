# Validation record

## Automated checks

Validated on Windows with Microsoft OpenJDK 25.0.3, Minecraft 26.2, Fabric Loader 0.19.5 and Fabric API 0.159.0+26.2.

2026-09-05: full build passed with all 14 required server tests; the client GameTest passed, including camera projection/bobbing checks. The close-corner screenshot was inspected: both stone faces remain opaque. English/Spanish key sets match and the production JAR excludes test classes.

`./gradlew build --offline -PscalebrewsBuildDir=<local-output>` compiles/packages the mod and runs server GameTests. The suite contains 13 mod tests plus Fabric's default test. Tests cover:

- All walking/sprint levels, removal, changes while already sprinting, Speed stacking and relative balance.
- Shrinking jump-strength bonuses at all tiers, removal and composition with another jump modifier.
- Beacon third-tier access, level-II upgrade and persistence allowlist.
- Monotonic 20-tick physical scale interpolation, halfway/final values, renewal, removal and another scale modifier.
- Health fraction across upgrades, hidden-effect downgrades and removal.
- All Shrinking fall-reduction tiers with and without Feather Falling IV.
- Physical exhaustion at all tiers for sprint, jumping, sprint-jumping, swimming and attacks; unchanged direct exhaustion, damage, Hunger and natural regeneration.
- Stone/wood/gold/iron pressure plates and activation by other entities.
- Farmland protection without cancelling fall damage, and turtle-egg destruction eligibility.
- Soul sand/berry progression and unchanged cobweb/honey penalties.
- Swift Sneak III's enchanted/unenchanting/removal lifecycle.
- Movement vibrations inside/outside each adjusted range, sprint differences, audible block breaking and silent crouching.
- Real landing hook, no ordinary-jump wave, outward knockback, capped radial damage and retained self fall damage.

`./gradlew runClientGameTest --offline -PscalebrewsBuildDir=<local-output>` launches a real client/integrated server, checks icon/translation resources, opens the extended beacon screen, captures screenshots and verifies server-to-client Shrinking III scale synchronization. Screenshots are saved under `build/run/clientGameTest/screenshots` for visual review.

`ScaleCameraChecks` exercises the injected camera methods across all three perspectives, normal and large sizes, each Shrinking tier, intermediate blend values and an external scale of .0625. It checks render/culling near-plane agreement, near-plane geometric sampling, nonzero bob amplitude, unchanged player position/collision and panoramic exclusion. A disposable stone-corner fixture additionally captures a close-up at Shrinking III with FOV 90 and bobbing enabled. This is not an exhaustive test of every FOV, modded camera, block shape or movement trajectory.

GitHub Actions runs the build/server suite on Ubuntu with Java 25. The client visual check is local, not part of headless CI. Packaging checks ensure production JARs include sprites/languages/tags/mixins but exclude GameTest classes and test-only mixins. `git diff --check` checks patch whitespace.

## Tiny mounts and configurable mechanics (2026-09-06)

The expanded suite has 21 mod tests plus Fabric's default test. The 22-test server run and the real client/integrated-server run passed during implementation. Added coverage includes:

- Synced registry JSONs: exactly chicken and bee initially, enabled defaults and independently decoded module/individual switches.
- Disabled rules exercise the real hooks using a test-only, scoped server-thread rule override (not packaged in the mod). This is not a live `/reload` or a separate-server configuration-switch test.
- Wrong-size saddle rejection without consumption, one saddle consumed on success, controller selection, vanilla-packet WASD input, ordinary jump suppression, Shrinking removal and Growth dismount/forced-mount denial; minecarts preserved and native pigs excluded.
- Bee mounting without a steering item, bounded yaw/pitch flight vector, item removal retaining rider, hive-entry suppression and external hive storage dismount/reset.
- Jump Boost comparison and stacking; final direct-attack knockback at several input strengths; actual arrow/trident hits at normal size and all effect tiers without damage scaling.
- Recipe lookup and crafting with small flowers; two-block flowers rejected; identical item components regardless of flower.
- Client-side JSON/equipment synchronization, saddle texture availability, screenshots before/after saddle removal, actual keyboard-driven chicken movement, held-Space slow fall versus faster fall after release, look-driven bee ascent/forward flight, and relinquishing control when the flower is removed.

The client tests exposed and fixed a passenger-packet race: mounting restrictions must be server-authoritative, because the client can receive passenger state before equipment/effect updates. Original camera/beacon/scale-sync checks remain in the same run. The saddle and held-item screenshots were visually inspected.

## Limits and follow-up

- These are integration tests in development worlds, not a complete modpack playthrough or a performance benchmark.
- Combatify, Alex's Mobs Continued, mounts, elytra, tight spaces, dedicated multiplayer latency, wall/ally/PvP impact edge cases and every Swift Sneak/Soul Speed equipment combination still need broader playtesting. The relevant vanilla call paths were inspected, but that is not equivalent to testing every external mod.
- The 26.2 mock connected-player helper used by two tests is deprecated; it remains functional. Damage tests use a plain survival mock to avoid the connected mock's client-loading immunity.
- OneDrive can lock generated output. The optional local build-directory property avoids most build-output contention while leaving the repository in its intended location. Test-run directory auto-deletion is disabled; no user worlds or source directories are removed.
- There are no public release/tag claims. A successful build is not a claim that every gameplay interaction is release-ready.
