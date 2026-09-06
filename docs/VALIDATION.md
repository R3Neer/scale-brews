# Validation record

## Automated checks

Validated on Windows with Microsoft OpenJDK 25.0.3, Minecraft 26.2, Fabric Loader 0.19.5 and Fabric API 0.159.0+26.2.

2026-09-05: full build passed with all 14 required server tests; the client GameTest passed, including camera projection/bobbing checks. The close-corner screenshot was inspected: both stone faces remain opaque. English/Spanish key sets match and the production JAR excludes test classes.

`./gradlew build --offline -PscalebrewsBuildDir=<local-output>` compiles/packages the mod and runs server GameTests. The initial suite contained 13 mod tests plus Fabric's default test (expanded below). Tests cover:

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

## Effective-scale mounts, explosions and general landings (2026-09-06)

At this milestone the suite contained 34 mod tests plus Fabric's default test. All 35 required server tests passed both with the base Fabric setup and with Combatify 1.4.0-26.2 / Alex's Mobs Continued 2.1.9. The real client/integrated-server GameTest also passed in both configurations, including mounting/input, synced data, camera and beacon checks. Added coverage includes:

- Effective rider/mount SCALE matrices, external modifiers, transition-time dismounts, exact inclusive JSON boundaries, configurable fallback/overrides and legacy tiny-definition migration. Native saddle/age/passenger restrictions and boat/minecart exemptions remain covered.
- Actual normal/charged creeper explosions at every level: observed final vanilla radius, increasing/decreasing damage, distant blast reach and block destruction. Actual splash/lingering effect delivery also reaches the same explosion hook.
- Player and mob landing hooks, a natural gravity-driven mob fall, all Growth tiers, bounded wave damage, particles, native landing game events and retained self fall damage. Flying/no-fall negatives do not trigger a wave.
- The single combined landing switch disables both push and damage plus wave feedback. Legacy separate false switches migrate to combined-off; encoding omits old keys. Ordinary damage recoil and self fall damage remain active with the wave disabled.
- Optional integration tests exercise Combatify weapon-dependent reach (hand, sword, axe, trident), scaled attack knockback, Alex's actual tendon brewing ingredient and a Growth grizzly-bear landing. These optional branches run only when their mods are present, not in ordinary CI.
- Small sprinting player collision against perpendicular block walls and elytra eligibility at every Growth/Shrinking tier. These are bounded regression fixtures, not an exhaustive collision/flight simulation.

Use `-PscalebrewsCompatMods=<directory-with-jars>` with `runGameTest runClientGameTest` to add optional mods to the test runtime only. The isolated check used Atlas Core 1.1.3-26.2, Defaulted 1.3.8, CodxLib 1.5.1 and Cloth Config 26.2.155 alongside the two mods. It does not edit the user's modpack or bundle these dependencies in the production JAR. Cloth Config is needed by Atlas Core's client UI.

Combatify's alternate knockback wrapper bypassed the original HEAD injection. The shared hook now wraps outside Combatify's priority-1400 method (priority 1500), passing the scaled input into its physics without replacing them. Runtime tests verify the resulting behavior, including the landing-source exclusion. Combatify's configured regeneration timing is compared against an unscaled control rather than imposing vanilla's tick schedule.

The optional-mod run logs upstream/default-configuration warnings for Combatify optional weapon recipes/enchantment tags and an Alex's loot-table context. Successful Scale Brews tests do not certify those unrelated data files or the complete modpack.

## Redstone and mounted-bee animation (2026-09-06)

`build runClientGameTest --offline` passed with the base Fabric setup: **36 required server tests** and the real client/integrated-server suite. The optional-mod matrix recorded above was not repeated for this change.

- Actual world brewing recipes extend both level-I families to 9,600 ticks, preserve the container, and convert extended Growth to extended Shrinking. Tests cover normal/splash/lingering inputs, both upgrade orders, gunpowder/dragon-breath conversion, unchanged II/III upgrades, rejected redstone on II/III and rejected glowstone/repeated redstone on extended variants. Native lingering quarter-duration is checked (2,400 ticks).
- Client registry checks verify both extended entries and their shared translated names. No new language keys or art assets are required.
- The real bee renderer extracts occupied/unoccupied snapshots from the synchronized test mount. Its injected model is sampled over seven animation times, calm/angry and rolling states, before mounting, while steering, after removing the steering item and after dismounting. Mounted body height/pitch remain stable; wings keep moving symmetrically; free bobbing returns; stinger visibility and entity position/bounds remain unchanged. The occupied saddle snapshot remains present. A third-person mounted screenshot was inspected.

## Animated bee riders and steering-item attraction (2026-09-06)

The base Fabric `build runClientGameTest --offline` passed with **39 required server tests**. A subsequent client run also passed after adding the real unmounted-following fixture. Optional Combatify/Alex's Mobs integration was not rerun for this change.

- Vanilla pig and strider native temptation predicates accept their respective steering items in either hand, with no saddle or passenger. These tests use the live 26.2 goals and item tags, not hard-coded substitutes.
- Bee native temptation accepts Flower on a Stick without Shrinking or a saddle, retains real flower attraction/breeding, and respects master/per-mob switches, NoAI and attack targets. The stick is not breeding food.
- Decoded test-only JSON changes a chicken from direct to item-steered control, verifies the configured item, and rejects disabled/missing-item definitions and empty hands. A snow golem receives exactly one fallback goal through its actual server-AI hook, without requiring a native temptation attribute or goal. These are bounded generic-mob tests, not a claim about every modded brain/navigation implementation.
- The real client places an unsaddled, unmounted bee about six blocks from an unshrunk player holding the flower stick in the offhand. After 65 ticks of normal AI/navigation it must approach within 3.5 horizontal blocks. No movement or teleport is injected during that interval.
- Animated rider snapshots are checked before mounting, during flight, after removing the steering item and after dismounting. Across seven animation times, calm/angry rolling states, three mount scales and three yaws, transformed reference points must agree with the actual animated bee body frame. Passenger-relative pivots, native wing symmetry, bobbing, stinger state and unchanged entity position/bounds are also checked. Real third-person screenshots accompany the matrix checks; first-person rendering is exercised in the existing mounted-flight fixture.
- Packaged JSON parses, English/Spanish keys match, and the production JAR contains neither test classes nor the superseded fixed-body mixin. No new translation keys or textures were needed.

The earlier fixed-body validation above is historical and is superseded by this animated-rider implementation. Broader visual/modpack and multiplayer-latency QA remain subject to the limits below.

## First public beta candidate and VP26 packaging (2026-09-06)

The versioned `0.1.0-beta.1` candidate pins Loom 1.17.20 and requires Fabric API 0.159.0+26.2 or newer. Its clean local `build runClientGameTest --offline` completed successfully: all **39 required server tests** and the real client/integrated-server suite passed. The inspected production JAR reports the beta version and dependency constraint and contains no GameTest classes.

That exact JAR was added as a local, both-sides `gameplay` mod to VanillaPlus 26.2. Packwiz/ModpackTools generated a 93-mod MRPack successfully (29 client-only, one host-only, 63 both, zero unknown) and a fresh comparison reported no differences. All four previously unclassified mods were assigned after checking their actual project IDs: Better Archeology and its Dungeons & Taverns compatibility mod to world generation; Resourceful Config and StrawberryLib to libraries. Scale Brews was assigned to gameplay.

The real Modrinth test instance was synchronized with all 92 client-applicable artifacts declared by the pack. Filename and provider SHA-512/local SHA-256 comparison reported zero missing, extra or mismatched JARs. This establishes packaging and installed-artifact consistency; the owner's interactive gameplay session and a dedicated-server/client session remain pending and are intentionally not claimed here.

## Living platforms (2026-09-06, beta.2)

The expanded base server suite passed all **54 required tests**. The platform tests cover oriented rectangles (including rejection of enclosing-AABB corners), actual-width boundaries and external scale, decoded policy switches, ascending contact, walls, jumping, sneak/knockback separation, yaw/translation, transient lifecycle, mixed support chains, stationary items, off-rail cart release, passive descent, and occupied boats on a giant player's head (including crouching). Sand remains an entity after 700 ticks; an anvil damages its support once per landing. Global `noCollision` remains unchanged.

The real-client dedicated-server proof passed the short 0/100/200 ms additional-RTT matrix with `allow-flight=false` for both a player and an occupied boat. The player completed a separate **12,000-tick soak** (4,000 at each RTT), ten minutes of simulated transport in total, without disconnection or support loss. The machine logged scheduling lag near the end; this is not a server-performance benchmark. Later short tests also count player correction packets to reject repeated corrections. All sixteen built-in model-part paths resolve and produce finite translations in the client fixture. Third-party resource-pack/model animation combinations still need human QA.

The VP26 integration run used the installed JAR set in an isolated development runtime. It did **not** produce a clean full-pack result:

- With the full installed set, e4mc 6.2.1 calls absent `CommandSourceStack.hasPermission(int)` while sending commands to connecting players. This blocks a full dedicated-connection acceptance claim.
- Excluding e4mc **only in the diagnostic copy**, and copying the installed pack configuration, gave **88/89 passing server tests** (including additional mods' tests). Every platform test passed. The remaining existing exhaustion regression compares regeneration food usage against a matched baseline and observed 20 versus 19. No production hunger behavior or external mod was changed to suppress that result.
- The full client-mod diagnostic copy (minus e4mc) stopped during development bootstrap with unreferenced `wilderwild:*` biome holders. It did not reach the client platform test. This result is specific to that development launch; it is not proof of the same failure in the production launcher.

The final base `build runClientGameTest` passed after the last movement-reference authorization change. Client checks include independent occupied-boat controls, shared same-frame visual sampling, actual bee bobbing without modifying physics, and the existing Tiny Mounts/camera/beacon/scale tests. The third-person occupied-boat-on-ghast screenshot was visually inspected. The implementation commit `721dcc0` also passed GitHub's Linux build/server CI.

The beta.2 production JAR contains 33 parseable JSON files, all 16 platform profiles, and no test classes/resources. Its SHA-256 is `6127d50caab8476093a4333141b90b145f0c98518a516f23036511abbc5b81cd`. That exact artifact was installed in the VP26 source pack and the `VanillaPlus-26.2 (1)` Modrinth instance. Both previous beta JARs were moved into a separate backup directory, not deleted. The local gameplay category was retained.

`VanillaPlus-26.2-20260906-152523.mrpack` exported successfully with 93 mods; its embedded Scale Brews JAR has the same hash and no beta.1 duplicate. A fresh ModpackTools comparison reports no differences. Metadata verification remained incomplete (125 requirements not verified), so the export is a packaging result, not a compatibility certificate.

Other original VP26 mods and configurations were retained. These failures, broad visual QA, water/rail/lead combinations and late-join observer permutations remain explicitly open in TODO. Neither a successful archive export nor passing base Fabric tests establishes full modpack compatibility.

## Effective size and item placement — beta.3 (2026-09-06)

Physical SCALE now drives derived attributes, sprint, exhaustion, recoil, environmental thresholds, villager fear, creeper power and landing waves. The server suite covers all nine Growth/Shrinking pairs, external cancellation to normal size, fractional transition attributes/health, and inclusive impact thresholds at 1.96 and 3.88. Pure-potion endpoints remain unchanged; external extremes clamp derived strength at tier III.

Placement tests exercise a giant player's head, boats/chest boats/rafts and spawn eggs, immediate transport, footprint/obstruction rejection, permissions and creative/survival inventory behavior. Matching spawn eggs retain vanilla baby-spawning fallback where adult platform placement cannot fit.

The full installed VP26 mod set is retained in the isolated development runtime, including e4mc. All **98 required server tests passed**, including the final baby-spawning fallback regression (63 base tests plus 35 from Additional Additions). The e4mc optional production shim replaces its obsolete dedicated-server permission call with the 26.2 permission API, preserving the integrated-server branch. A dedicated permission regression exercises both allowed and denied command sources.

The regeneration discrepancy came from Combatify's random food-consumption choice, not increased regeneration exhaustion: the previous test compared different random draws. Both subjects now receive the same random sequence for 100 food ticks, with health, food, saturation and exhaustion compared after every tick. No production hunger/regen logic was changed.

Development-only client harness adaptations are not packaged in the mod:

- With Wilder Wild loaded, defer early vanilla command validation until a test server has loaded its real datapack registries; execute the same validation against those registries and assert that it completed. No biome registrations are removed or fabricated.
- Defer animated-atlas uploads only while the vanilla Globals uniform buffer has not yet been initialized. Runtime render validation remains enabled.
- Accept resource-pack requests from loopback test servers automatically, retaining vanilla download/hash validation. Non-local URLs still require normal confirmation.
- Allow up to 120 seconds for the dedicated test server to bootstrap with Wilder Wild, instead of Fabric's hard-coded ten seconds. Movement assertions, latency simulations and collision/speed checks are unchanged.

These adaptations address development bootstrapping and unattended test UI; they do not change the user's production configs or claim that all third-party log warnings are fixed. Wilder Wild's deferred validation completed against real registries at 17:27:49. The client accepted/downloaded the local Polymer pack and advanced to registry synchronization, but failed at 17:27:59 on duplicate `ResourceKey[minecraft:enchantment / enchancement:empty]`. The stack goes through `NetworkRegistryLoadTask` and `MappedRegistry.register`; duplicate-key validation was not weakened. This newly exposed full-pack connection failure remains open, and is not evidence of a Scale Brews platform failure. Its exact cross-mod cause still needs isolation.

The development renderer also reports unregistered virtual entity renderers for `enchancement:frozen_player` and `universal_graves:xp`, triggering its strict resource self-test fallback. This is another reason not to call the full-pack client run clean. Neither affected external JAR was changed. Only the isolated test e4mc configs now disable public hosting (`hostEnabled=false`) for subsequent local diagnostic runs; production sharing settings remain untouched.

### Final beta.3 base and artifact checks

`build runClientGameTest --offline` completed successfully at 17:33:41 with the base Fabric setup: all **63 server tests** and both real-client entrypoints passed. The client run includes dedicated player/occupied-boat transport at 0/100/200 ms added RTT with flight disabled, model-part checks, and the existing integrated-server scale, camera, beacon and Tiny Mounts regressions. This turn did not repeat the earlier ten-minute soak or replace the owner's manual playtesting.

The beta.3 JAR has 33 valid JSON files, all 16 platform profiles, and no test classes/resources. SHA-256: `36a19afff102aef2eccd8b46cd01a78b1397266c7ab01dacf1b773ece4a8eceb`. Matching copies are installed in the VP26 source pack and the `VanillaPlus-26.2 (1)` Modrinth instance. Both beta.2 JARs were moved to recoverable backups outside `mods`; the gameplay category was retained. These artifact checks do not imply that the blocked full-pack dedicated-client connection passed.

The export `VanillaPlus-26.2-20260906-173602.mrpack` completed with 93 mods; its sole embedded Scale Brews JAR is beta.3 and matches the hash above. ModpackTools reports no differences from that build. Its dependency-metadata verification still lists 125 unverified requirements: packaging success is not a full compatibility certificate. The base occupied-boat proof screenshot was visually inspected; this does not validate the blocked VP26 client.

### Remaining general limits

- These are integration tests in development worlds, not a complete modpack playthrough or a performance benchmark.
- Beyond the bounded tests above, full modpack playthroughs, dedicated multiplayer latency, unusual wall/ally/PvP impact edge cases and every Swift Sneak/Soul Speed equipment combination still need broader playtesting. Passing the tested versions/configuration is not equivalent to testing every external mod or future version.
- The 26.2 mock connected-player helper used by several tests is deprecated; it remains functional. Damage tests use a plain survival mock to avoid the connected mock's client-loading immunity.
- OneDrive can lock generated output. The optional local build-directory property avoids most build-output contention while leaving the repository in its intended location. Test-run directory auto-deletion is disabled; no user worlds or source directories are removed.
- There are no public release/tag claims. A successful build is not a claim that every gameplay interaction is release-ready.

## Scale materials and wolf mounts � 2026-09-06

Implemented on top of `e094257`, retaining the concurrent effective-size/platform changes.

- Integrated Scale Brews build: 71 required server GameTests passed without optional mods.
- Selected installed-mod suite: 71 required GameTests passed with Alex's Mobs 2.1.9, Animal Weights 1.1.0, Friends & Foes 4.0.27, Wilder Wild 4.2.11, Deeper Dark 4.4.1 and Stormie's Spiders 3.3.0 plus required libraries. This is a selected compatibility environment, not the entire VP26 pack.
- Real Murmur loot and snapping-turtle player shearing were exercised. Elastic Tendon and Spiked Scute scale; Lobster Tail is excluded. Looting III and probabilistic shrinking/growth were checked.
- The separate Animal Weights bridge passed its build and 4 required GameTests, including extra-roll production composition, scale-one occupancy, tiny neighbours and compact/giant enclosures.
- Complete client suite passed, including wolf direct input/pounce on a dedicated server with `allow-flight=false`, existing platform latency checks and existing integrated-client checks.
- Measured full forward pounce: 5.00000049 blocks. At pitch -30 degrees: 4.33012761 horizontal, 1.59467629 vertical apex.
- Client screenshots reviewed for simultaneous saddle/Wolf Armor and the reused charge HUD. Saddle art comes from the existing Java generator; no generative image assets.
- Test-only workaround registers Fabric's test phaser before publishing its worker thread; thread dumps showed a startup race in client-gametest 6.0.1. An integrated-server teardown also blocked once; final wolf proof uses a dedicated server. These changes are absent from production JARs.
- JARs were inspected for metadata, 81 material JSON definitions and absence of test classes.

Human two-player acceptance remains pending for borrowed-wolf combat, the full PvP/team/latency matrix and all gameplay cases in the original request. Automated real-client/server checks are not human playtesting. Production instances, pack exports, releases and tags were not updated by this change.

## VP26 Enchancement registry correction — 2026-09-06

The earlier `enchancement:empty` duplicate is resolved in the separate VanillaPlus-Enchancement-Compat project, version 1.0.1. Enchancement 26.2-r4 was replacing legitimate pending enchantment references with its missing-entry fallback during asynchronous registry loading. The narrow compatibility guard preserves ordinary pending-holder semantics while the enchantment registry is unfrozen; original fallback behavior resumes after freeze. Duplicate-key validation and all production mods/configuration remain intact.

- Corrected minimal negative control fails without the guard; compat build and all 3 minimal server tests pass with it, including fallback preservation after freeze.
- Full VP26 run used an isolated Scale Brews e19aed3 checkout to avoid concurrent build-output changes: 106 required server tests passed. Later platform-width changes in c39dd98 are not part of this run.
- Real clients joined dedicated servers at 18:27:08 and 18:28:11 after registry synchronization. Platform checks passed at RTT 0/100/200 ms, 120 ticks per setting, followed by integrated-client checks through bee dismount. This is not the ten-minute soak.
- The task was interrupted during final integrated-world teardown after 18:29:51. Thread dumps show the render thread waiting in IntegratedServer.halt/executeBlocking while the server and test threads await Fabric client-gametest phasers. The registry blocker is fixed, but a clean exit of the entire client suite is not claimed. Third-party resource warnings and broader manual QA remain outside this result.
- Installed only `vanillaplus-enchancement-compat-1.0.1.jar` in VP26 and Modrinth, SHA-256 `3858f2ee42f5382a6bb49fd4ac3194fd9eaef91d8225e9a2556dceafe4dbab4a`. Both old 1.0.0 JARs were moved to recoverable task-output backups outside mods; gameplay category preserved. No Scale Brews artifact was replaced by this fix.
- VP26 export `VanillaPlus-26.2-20260906-183420.mrpack` contains exactly one compat JAR with the same hash; `modpack diff --project vp26` reports no differences from that build. The existing 125 unverified dependency requirements remain packaging-metadata limitations, not new runtime failures. Compat source/tests/docs are committed locally as `4fb26f0`; that separate repository has no remote configured, so no compat push or release is claimed.
