# Validation record

This file is the sole source of truth for **executed evidence**. It records what was actually built, run or inspected, against which snapshot, and what that evidence does **not** prove.

Normative entity-collision behavior lives in [ENTITY_COLLISIONS_REQUIREMENTS](ENTITY_COLLISIONS_REQUIREMENTS.md), architecture/API/data ownership in [ENTITY_COLLISIONS](ENTITY_COLLISIONS.md), and implementation order/status in [ENTITY_COLLISIONS_PLAN](ENTITY_COLLISIONS_PLAN.md). Historical details removed from this file remain recoverable through Git; old planning/implementation diaries are not evidence sources anymore.

## Evidence rules

- A source file, implementation class or test method existing is **not** evidence that the behavior passes.
- A unit/kernel test does not substitute for a real GameTest, client, dedicated server, compatibility target, latency run or human visual review when the requirement calls for one.
- Evidence is attached to an exact source snapshot/commit or explicitly labelled historical/isolated.
- Compatibility evidence is version-bounded. Passing with one external-mod version does not certify future versions or a whole modpack.
- A generated/archive/install result proves packaging only unless gameplay was separately exercised.
- Current branch status is stated explicitly at the top of the relevant development section; historical green snapshots do not make a later red snapshot green.

## 0.1.0-beta.5 candidate — 2026-09-09

Snapshot/commit: `33b1f5c` for the published beta.5 candidate.

- Fresh isolated `build runClientGameTest --offline` passed with all **122 required server tests** and the complete integrated-client suite; the client exited cleanly at 17:49:26.
- GitHub Actions run `34373390000` passed for the candidate commit.
- Two HUD-free README screenshots were captured in a disposable vanilla-background world and visually reviewed. They demonstrate size and saddle equipment, not the in-development all-direction collision system.
- The regular beta.5 JAR was downloaded from the prerelease, its SHA-256 matched the locally validated artifact, and it was installed in the existing VanillaPlus-26.2 Modrinth profile with beta.4 backed up outside `mods`. Installed regular SHA-256: `5cbee58b3da9628f53f267b35ab3a3d16d922c4d2af6dfbf5b31a4d126711fc7`.
- Normal beta.5 gameplay uses the released upper-surface Living Platforms engine. The all-direction entity-collision prototype is dormant and is **not** a released feature.

Not proved by this milestone: human acceptance of the exact JAR in the whole VP26 pack, a fresh real dedicated/multiplayer session for beta.5, full optional-mod matrix, Modrinth public publication, or completion of the replacement entity-collision system.

## Released gameplay regression history

These entries summarize the strongest retained evidence for existing non-collision Scale Brews features. Earlier intermediate counts and superseded behavioral experiments remain available in Git history rather than being repeated as competing current truth.

### Core scale, movement, brewing and world mechanics

Across the beta progression on Minecraft 26.2 / Java 25 / Fabric, server and real-client suites exercised:

- Growth/Shrinking I–III, 20-tick effective-scale transitions, mixed effects and external SCALE modifiers;
- walking/sprint/jump/exhaustion/fall/reach/health behavior and size-aware camera checks;
- beacon powers and potion brewing, including extended/splash/lingering variants;
- Growth landing impacts, terrain propagation, creeper explosions, pressure plates, farmland/turtle eggs, vibration range and terrain resistance;
- relative villager fear using effective size;
- entity-scoped `scale^1.6` material loot and harvesting integration;
- packaging rules that exclude GameTest/test-mixin classes from production JARs.

The beta.5 122-test run above is the latest clean aggregate evidence for the released core. It does not turn optional compatibility branches into universal guarantees.

### Tiny mounts and wolves

Historical real server/client tests exercised:

- chicken/bee Tiny Mount definitions, relative size ratios, native saddle state and controller selection;
- chicken held-Space glide and bee look-directed flight;
- mounted-bee renderer state, natural bob/roll and rider/saddle presentation without moving physical bounds;
- unmounted steering-item attraction while retaining native food/breeding semantics;
- wolf saddle/armor coexistence, Crouch+Use mounting, direct controls, commanded bite/pounce and animation signalling;
- persistent probabilistic wild-wolf trust/taming with save/load and different riders.

The wolf attack-animation path was also run with EMF 3.3.5 / ETF 7.2 and Fresh Animations 1.10.5; that establishes the observed integration for those versions, not arbitrary resource packs.

## Released Living Platforms evidence — beta.2 and later regressions

This section records the **legacy upper-surface engine** that remains active in released beta gameplay while it is being replaced. Its configuration is documented only in [CONFIGURATION](CONFIGURATION.md#released-living-platform-configuration-legacy-during-collision-migration). Its architecture is not the target architecture of the replacement system.

### Base physics and categories

The beta.2 expansion passed **54 required server tests**. Fixtures covered:

- oriented rectangular upper surfaces, including rejection of corners that exist only in the enclosing AABB;
- exact width-ratio boundaries and external SCALE;
- ascending contact, walls, jumping, sneaking edge protection and separation under knockback;
- support translation/yaw, transient lifecycle and support chains;
- stationary items, off-rail minecart release and passive-descent accounting;
- occupied boats on a giant player's head, including crouching support;
- falling sand remaining an entity after extended support and an anvil damaging its support once per real landing;
- no global `noCollision` rewrite.

### Network/latency

A real-client dedicated-server proof passed short **0 / 100 / 200 ms added RTT** runs with `allow-flight=false` for both a player and an occupied boat. The player also completed a **12,000-tick** transport soak, 4,000 ticks per RTT, ten simulated minutes total, without disconnection or support loss. Scheduling lag near the end was logged; this run was not a performance benchmark.

Later short fixtures counted correction packets to reject repeated correction loops and exercised shared same-frame visual sampling and occupied-boat controls.

### VP26 bounded integration

The beta.2 artifact was installed into the VP26 source pack and Modrinth instance with matching hashes and no duplicate Scale Brews JAR. An MRPack exported successfully. That establishes packaging consistency, not complete modpack compatibility.

The first diagnostic full-pack dedicated run exposed e4mc 6.2.1 calling a removed permission overload. After the dedicated compatibility shim work, later full-pack server evidence reached **106 passing tests**, real clients received registries and joined dedicated servers, and bounded 0/100/200 ms platform checks completed. The separate full VP26 integrated-client harness still had a teardown/deadlock limitation after gameplay assertions reached their end. Human broad renderer/camera/water/rail/lead/late-observer QA remains open.

The released platform engine is intentionally temporary during the new collision migration. Its old success is regression evidence for behavior that must be preserved, not justification for retaining two physical engines in the final architecture.

## All-direction entity-collision development evidence

This section consolidates the useful evidence formerly scattered through the `ANATOMY_*` development diary. It is historical/experimental unless a row explicitly identifies a current candidate. The replacement subsystem is governed by the canonical requirements and plan, not by these old proof names.

### Original geometry and pose vertical slice

Historical preparation runs demonstrated:

- extraction of original Minecraft `ModelPart` geometry for cow and player wide/slim and original Alex's Mobs Continued 2.1.9 grizzly geometry based on `AdvancedModelBox`;
- preservation checks for hierarchy, separate boxes/pieces, transforms and filtering decisions;
- **320 ordinary animated pose comparisons** against original model behavior for the early cow/player/grizzly slice;
- a dedicated-server lane capable of loading exported common geometry without loading Alex/client renderer classes.

These results prove the bounded vertical slice that was run. They do not prove all vanilla entities, all Alex's Mobs species, arbitrary render frameworks, special poses or the later generic engine architecture.

### Geometry kernel, gravity and temporal proofs

Historical kernel/GameTest evidence covered:

- convex piece overlap/separation/raycast operations and six cardinal gravity frames;
- top, underside, lateral and gap fixtures;
- hierarchy composition and conservative temporal envelopes;
- bounded overlap recovery with pair-local suspension rather than global collision disabling;
- root translation/rotation/scale and joint interpolation proofs on selected fixtures;
- deterministic multi-step motion proofs, including runs described as 200 steps in the prototype. Those steps were mathematical/test iterations, not automatically 200 real server ticks.

A synthetic or hand-reproduced pose formula is acceptable for a solver fixture but does **not** substitute for original-model equivalence evidence when the requirement is about extraction or model compatibility.

### Runtime/catalog/network vertical slice

Historical prototype runs demonstrated pieces of the server-owned pipeline:

- geometry/profile catalog validation and protocol-v2 transfer with epoch/revision/integrity checks;
- server-owned pose publication and client rejection of stale/wrong identity data;
- identity separation for client/server entity instances with the same network ID;
- ordered material contact publication including piece/face/local point and late-tracking state;
- unsupported-pose removal rather than indefinitely retaining old geometry;
- one integrated-server scenario that observed **60 real transport ticks** for a body on a moving original-model cow.

This does not prove the final Q2 continuous material-event pipeline, local-player prediction/reconciliation or all lifecycle adversaries.

### Host/session independence proof

The historical Host019 proof ran two isolated dedicated hosts/epochs against one immutable original-model export, rejected an old epoch and showed no carried/contact residue crossing host identity. It reused the body UUID but **not** the network ID. It therefore did not close same-network-ID reuse, SharedWorld, arbitrary resource-pack geometry or the final prediction/reconciliation requirements.

### Clinging integration proof

A bounded Clinging Reoriented source snapshot was compiled against an early protocol-2 Scale snapshot and completed **22 server GameTests**. A proof-only bridge exercised Gravity Changer, six-direction managed contact, anatomical clearance and no duplicate legacy carry in its fixture.

That evidence does not certify the current consumer against the final API, client behavior, all Space/charge/Elytra/camera transitions, latency, VP26 or release delivery. The canonical plan requires repeating integration after the public collision API/data contract stabilizes.

### Snapshot-021 red cases

One later recorded prototype run reached **134 tests on its selected lane** but retained two runtime failures:

1. initial floor contact in a fixture combining support yaw/ascending motion;
2. contact reacquisition after animated squeezing.

Those failures must not be confused with later compilation errors in dispatcher tests. They are preserved because they describe physical scenarios that must remain in the replacement regression suite even if the old fixture code is reorganized.

### Pre-restructure `chatgpt-editing` build status — 2026-09-10

The exact pre-restructure commit `5a8bf915d22d52cf4e79a68e2506d199bb6e848e` was built by GitHub Actions run `34449689179`, job `102782357848`.

- `compileJava` and `compileClientJava` completed.
- The workflow failed at `:compileGametestJava` before GameTests executed.
- Two `AnatomyMaterialEventDispatcherTests` call sites still used an older three-argument constructor after `MaterialEventDispatcher` gained a `maximumEvents` budget. The available constructor requires `(maximumBodies, maximumDepth, maximumEvents, identity)`.
- Warnings present in the job were not the build-breaking cause.

This is the latest recorded executable status **before the documentation/code cleanup now being performed**. Historical green anatomy snapshots do not override this red exact-snapshot result. A later build belongs in a new dated/current subsection after it actually runs.

## Current acceptance gaps for entity collisions

The open work itself is not duplicated here; see [ENTITY_COLLISIONS_PLAN](ENTITY_COLLISIONS_PLAN.md). For interpreting existing evidence, the major unproved areas at the start of that plan are:

- one final data/API model independent of legacy `PlatformDefinition.Surface`;
- complete Q2 continuous material-event consumption in live movement hooks;
- exactly-once prediction/reconciliation for locally controlled players/vehicles;
- transactional lifecycle/reload/reconnect coverage on the final architecture;
- generic family engines and zero-UNRESOLVED vanilla coverage;
- final Clinging consumer migration;
- separate version-pinned VanillaPlus compatibility coverage;
- the normative performance benchmark and final latency/soak matrix on the replacement engine.

This list exists only to scope what the preceding evidence does **not** prove. Task ordering and completion state remain canonical in the plan.

## Reproduction commands

Ordinary core build/server suite:

```powershell
.\gradlew.bat build
```

Real client/integrated-server suite:

```powershell
.\gradlew.bat runClientGameTest
```

Optional compatibility JARs for supported test tasks can be supplied with:

```powershell
.\gradlew.bat runGameTest runClientGameTest -PscalebrewsCompatMods=C:\path\to\test-mods
```

The collision preparation harness is `tools/PrepareAnatomyProof.ps1`. Its historical A/B/C lanes are test tooling, not separate product architectures. Use only the current flags declared by the script and pin every external JAR/input used by an evidence run.

GitHub Actions performs the ordinary Gradle build/server path on Ubuntu/Java 25. Special client, dedicated latency, compatibility and preparation lanes are only evidence when their exact command/snapshot/result has been recorded here.
