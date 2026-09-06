# Scale Brews

Growth and Shrinking potions for Minecraft **26.2 / Fabric / Java 25**. Install Fabric API and this mod on the client and server. Licensed under GPL-3.0-or-later.

Both effects have three potion levels. They change physical size, health, damage, reach and knockback; Growth also increases step height. Size transitions take one second. Health changes preserve the percentage of remaining health.

Walking and sprint balance (sprint multiplies the current walking speed):

| Effect | I | II | III |
| --- | ---: | ---: | ---: |
| Growth walking | 1.08x | 1.16x | 1.24x |
| Growth sprint | 1.20x | 1.12x | 1.05x |
| Shrinking walking | 0.92x | 0.84x | 0.76x |
| Shrinking sprint | 1.50x | 1.90x | 2.50x |

Normal sprint is 1.30x. Neither effect alone beats the sprint speed of Speed at the same level; Speed stacks multiplicatively with both. Shrinking increases jump strength by 2.5/5/7.5%; Growth does not change jump strength. Physical exhaustion from movement, jumping and attacks changes by +10/+20/+30% with Growth and -5/-10/-15% with Shrinking. Regeneration, Hunger, received damage and food nutrition are untouched.

When small in first person, the camera's near clipping plane and view-bobbing amplitude shrink with the actual size. This reduces close-corner clipping without changing collision, speed or FOV. Normal size, Growth and third person retain vanilla camera behavior.

Growth landings above three blocks push nearby entities with gust and ground particles, for players and mobs with compatible fall physics. Only level III above six blocks adds damage, capped at two hearts before defenses and decreasing with distance. The falling entity still takes its normal fall damage. Growth also resists soul sand and sweet berry bush slowdown for players.

Creeper explosion power scales with Growth (1.15/1.30/1.50x) and Shrinking (0.90/0.80/0.65x), affecting vanilla blast radius, damage and block destruction. Charged creepers use the same multipliers without an extra cap: Growth III reaches power 9.

Shrinking reduces fall damage, shortens the detection range of movement vibrations and enhances Swift Sneak at levels II/III. Level II stops activating stone/iron pressure plates; III stops activating all pressure plates and cannot trample farmland or turtle eggs. These additional physical mechanics affect players.

Both powers appear in a beacon's third tier. A full beacon can upgrade either to level II; level III remains potion-only. English and Spanish translations and original effect icons are included. Bottle models and potion tinting use Minecraft's normal resource system.

## Tiny mounts and configuration

Small enough riders can saddle and ride adult chickens (WASD, hold Space while falling to glide) and bees (look-directed flight with a Flower on a Stick). Their maximum rider/mount effective SCALE ratio is 0.53 by default: Shrinking II/III fits a normal mount, but shrinking the mount can make it too small. Craft the steering item from a fishing rod and any vanilla small flower. Both mounts have separate, hand-authored saddle layers.

Living mounts use configurable size ratios, including external scale modifiers and gradual transitions. Most default to 1.0, camels to 1.1, happy ghasts to 2.0; boats/minecarts are exempt. Growth II can ride a Growth II/III horse; Growth is no longer a blanket mounting ban. Native age, taming, saddle and passenger restrictions remain. Growth II/III also scares nearby villagers through vanilla panic, without reputation penalties or automatic golem hostility.

Tiny Mounts, individual mounts, villager fear, the complete Growth landing effect (knockback plus level-III damage) and environmental interactions can be enabled or disabled using server-owned JSON. The landing effect has one combined switch, not separate push/damage controls; ordinary knockback and self fall damage remain untouched. Everything defaults to enabled. See [configuration and mount definitions](docs/CONFIGURATION.md) and the ready-to-edit `examples/world-config` datapack.

## Brewing

1. Awkward potion + slime ball -> Growth. If `alexsmobs:elastic_tendon` is registered, that item replaces the slime ball.
2. Glowstone upgrades either family I -> II -> III.
3. Fermented spider eye converts Growth to Shrinking, retaining potency.
4. Vanilla gunpowder and dragon breath conversions provide splash and lingering forms.

Durations are 3 minutes, 90 seconds and 45 seconds respectively. Redstone extended variants are not implemented yet. Alex's Mobs is optional; its item lookup is checked at runtime.

## Development

Run `./gradlew build` (Windows: `.\gradlew.bat build`) for the JAR and server GameTests. Run `./gradlew runClientGameTest` for the automated beacon/resources/client synchronization check. The tests use disposable development worlds, separate from `run/saves`.

For OneDrive checkouts, generated output can be redirected with `-PscalebrewsBuildDir=C:/path/to/local/build`. Test run directories remain under `build/run`; automatic test-directory deletion is disabled to avoid OneDrive locking issues. The source checkout remains in place.

Recreate the original silhouette-and-arrow pixel-art PNGs with `java tools/GenerateArt.java`. The generated assets share the repository license and contain no third-party artwork. Vanilla bottle art, particles and sounds are reused by reference, not redistributed. See [art direction](docs/ART.md).

See [mechanics and integration details](docs/MECHANICS.md), [Git policy](CONTRIBUTING.md), [validation](docs/VALIDATION.md) and [remaining work](TODO.md). This is a development mod; full modpack/mount/collision playtesting and a public release are still pending.
