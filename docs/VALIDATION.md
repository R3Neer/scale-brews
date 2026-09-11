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

This remains the exact status of that historical snapshot; later green runs do not rewrite it.

### G0 post-restructure baseline — 2026-09-10

The exact G0 review/convergence commit `420c305e301a3082df119a14a7db81f1fd2423d1` was built by GitHub Actions run `34462591148` (run number 82) and completed **successfully**.

That commit includes the collision-documentation consolidation, removal of known unused lifecycle/root-dispatch scaffolding, relocation of the real public API to `collision.api`, relocation of client model extraction to `client.collision.preparation`, and compatibility shims for historical callers. The previously stale `MaterialEventDispatcher` GameTest constructors are therefore compiling in this baseline.

This run is the ordinary repository workflow (`./gradlew build`) on GitHub Actions. It establishes a compilable server/GameTest build baseline after structural cleanup. It does **not** prove the unfinished Q2 material pipeline, client-special proof lanes, dedicated latency/reconnect behavior, Clinging migration, VanillaPlus coverage, performance targets or final physical correctness. Those remain gated by the canonical plan.

### G0 final package reorganization — 2026-09-10

The package migration was verified in GitHub Actions run `34464719776`, job `102830447888`. The Action first built the pre-transform branch snapshot, applied the deterministic package migration, then executed a fresh `./gradlew clean build` **before** committing the transformed tree. The post-transform suite reported **137 required tests, 1 excluded, 137/137 passed**, and `BUILD SUCCESSFUL`.

The verified transformed tree was committed as `0ededb67464b47fb35170f8d9854cf62498fc626` (`refactor: organize entity collision packages`). The resulting source organization moves the replacement subsystem out of `platform.anatomy` into:

- `collision.api` for the public façade and API-adjacent DTOs;
- `collision.geometry` for pure geometry/data primitives;
- `collision.pose` for reusable pose providers/channels;
- `collision.physics` for the CCD/separation/temporal kernel;
- `collision.internal` for orchestration that still has real Q1/Q2 coupling and is not public API;
- `client.collision.preparation` and `client.collision.network` for client-only preparation and authoritative receive/cache responsibilities.

The migration also removed the old package shims and removed production-specific Alex/grizzly registration/eligibility code. The old 2.1.9 grizzly formula and guard survive only as `src/gametest/.../GrizzlyPose.java`, explicitly an H1 acceptance fixture. The temporary migration script and temporary self-modifying workflow do not survive in the transformed tree; the normal repository workflow was restored. A follow-up repository-only cleanup restored the Gradle wrapper's original non-executable Git mode after GitHub Actions' local `chmod` had accidentally staged it.

This evidence validates **compilation and the required server/GameTest suite after the structural refactor**. It does not establish that `collision.internal` is the final package boundary, nor does it prove G1's final API/data contract, the unfinished Q2 material pipeline, client-special proof lanes, dedicated latency/reconnect behavior, Clinging migration, VanillaPlus coverage, performance targets or final physical correctness.

## G1 public contract and canonical data — CLOSED 2026-09-11

Status: **G1 formally closed** after S01-S04, the independent adversarial campaign, repair of every observed implementation failure, a complete server rerun and a fresh real-client/integrated/dedicated proof. G2 has not been started.

### Candidate progression and evidence corrections

Early S01-S03/S04 workflow runs remained at the S00 baseline of **242 tests** because the new test classes compiled but were not registered as Fabric GameTest entrypoints. Those green runs are retained only as build/regression history and are not acceptance evidence for G1 assertions. `144470d769869426d8ee4be57912b9468a25e79e` registered S01-S04 and made the external fixture a real test-mod initializer.

The first registered run encountered one environment/download failure before compilation; the unchanged retry passed. Later review-driven fixes raised the suite through 256, 257, 258, 260 and **262** tests. Material corrections before the independent campaign included fail-closed filter decode, missing engine-reference rejection, exact ratio boundaries, legacy migration warnings, validation-before-ordering, structural selector identity, deterministic snapshots, fail-closed body adapters and removal of duplicate ratio semantics from `PlatformEligibility`.

The non-adversarial snapshot `b1d7c3b204412119d2c5ee708d3784f231a7db76` passed server run `34588346710`, job `103227629075`, with **262/262** and client proof run `34588346767`, job `103227629118`, with real client/integrated/dedicated success. That evidence was provisional pending the independent campaign.

### Independent adversarial red-before-green

The adversarial campaign expanded the suite to **266 tests**. GitHub Actions run `34592683287`, job `103241351742`, on exact snapshot `19626950569110963cd47611b245234c650f2ca0` failed exactly two required tests:

1. `S01PublicApiBoundaryTests.backendContractIsNotPublicConsumerApi`: `AnatomyBackend` was public under `collision.api`, leaking runtime wiring into the consumer API surface.
2. `S03CanonicalCollisionDataTests.canonicalCodecsRejectInjectedLegacyKeys`: canonical codecs accepted unknown/legacy fields because `RecordCodecBuilder` ignored unrecognized keys.

Both were classified as **implementation bugs**. No test was weakened or removed and no normative requirement was changed.

`3ba014dc2e19b6b700a707bd3177c55ac443328f` repaired both defects: the backend contract moved to `collision.runtime.AnatomyBackend` with the corresponding ServiceLoader descriptor, and canonical binding/policy codecs became strict about allowed fields, including nested canonical objects, while preserving fail-closed filter decoding.

### Final server/build evidence

GitHub Actions run `34593762577`, job `103244714934`, checked out `3ba014dc2e19b6b700a707bd3177c55ac443328f` and ran the ordinary `./gradlew build` path on Ubuntu / Microsoft Java 25.0.3.

- **266/266 required server GameTests passed**.
- `BUILD SUCCESSFUL`.
- The complete adversarial S01-S04 suite was present and executed.
- Build artifact `10260227920` has SHA-256 `67622ee3c4af2ddffb902854f6d6843371aa9b6768b4cd79c300c6e44c238049`.

The final server suite covers the public façade/backend boundary, backend non-public consumer contract, public engine/body registries, bounded/canonical DTOs, versioned schemas/capabilities, unknown-field rejection, legacy xor semantics, strict filter errors, engine/provider reference validation, exact ratio boundaries, structural selector identity, deterministic ordering, body-adapter fail-closed behavior and the external init-time fixture selected by JSON.

### Final real client / integrated / dedicated evidence

A temporary one-shot workflow was triggered by `23a1072ef451a14f59f707019415e1f4ac60dbab`. That commit changed only the workflow comment; production and GameTest sources are code-identical to the repaired G1 implementation.

- The ordinary build run `34593970120` passed again.
- `g1-client-proof` run `34593970131`, job `103245364717`, executed `xvfb-run -a ./gradlew runClientGameTest` and completed **success / BUILD SUCCESSFUL**.
- The real client/integrated harness exported original Minecraft cow and player wide/slim geometry.
- It completed 80 animated pose comparisons for cow, player wide and player slim plus **640 additional vanilla-family comparisons**.
- It printed `S00_CLIENT_RECEIPT_AUTHORITY PASS` and `S00_OBSERVER_AUTHORITY PASS`.
- The dedicated proof used `allow-flight=false` and completed **0 / 100 / 200 ms RTT, 120 ticks each**.

ALSA/narrator, X11 and remote profile/service warnings in the hosted runner were environmental noise and did not fail the asserted lanes. The temporary workflow was deleted after the proof; its removal does not change production or tests.

### Final review and scope limits

The final zero-change review after the adversarial repair confirmed:

- `collision.api` no longer contains the backend contract; runtime wiring lives outside the consumer API surface;
- canonical codecs reject legacy/unknown fields rather than silently ignoring them;
- `CollisionBindingCatalog` validates registered geometry/pose/root ids and has deterministic/fail-closed selection;
- binding/policy data remain independent of `PlatformDefinition.Surface` and `automatic_top`;
- legacy planes migrate explicitly and never become anatomical geometry;
- body categories, ratio/friction policy and adapters use the canonical integration layer;
- the external fixture registers via public API and is selected by declarative JSON;
- G1 did not divide or redesign `AnatomyMovement`, integrate the live Q2 material dispatcher, replace final G3 lifecycle/catalog behavior, or remove the G5 legacy motor.

G1 therefore proves the public/data/integration contract required by its gate. It does **not** prove G2's continuous material-event pipeline, G3's final prepared catalog/lifecycle/coverage engines, G4 prediction/reconciliation, G5 legacy-motor removal, final Clinging migration, VanillaPlus compatibility, normative performance benchmark or final release acceptance.

## Current acceptance gaps for entity collisions

The open work itself is not duplicated here; see [ENTITY_COLLISIONS_PLAN](ENTITY_COLLISIONS_PLAN.md). After formal G1 closure, the major unproved areas are:

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

## S00 Foundation Audit — accepted implementation snapshot — 2026-09-11

S00 acceptance was executed against the exact reconstructed source tree `68244f2acbebccfd0dec607220726b9ed7e56e8a`. GitHub Actions transport commit `a5503f97a7320e416cd253357c6c8f3cb5b002e3` materialized that tree and verified its identity before running Gradle. The transport commits are CI machinery only; the logical S00 tree removes that machinery.

### Final build/client/dedicated evidence

GitHub Actions run `34574727529` on Microsoft Java 25.0.3 completed successfully.

- The materializer printed `S00_SOURCE_TREE=68244f2acbebccfd0dec607220726b9ed7e56e8a` and validated every reviewed patch digest before execution.
- Gradle wrapper validation passed.
- `./gradlew build --rerun-tasks` completed with **242/242 required server GameTests** passing.
- The real-client/integrated lane completed successfully under Xvfb/llvmpipe. It exported original Minecraft cow and player wide/slim geometry, ran 80 animated comparisons for each of those three models, and ran **640 additional vanilla-family pose comparisons**.
- The same client lane completed the dedicated proof with `allow-flight=false` and **0 / 100 / 200 ms RTT, 120 ticks each**.
- The real client printed `S00_CLIENT_RECEIPT_AUTHORITY PASS` and `S00_OBSERVER_AUTHORITY PASS`.
- Evidence artifact `S00-34574727529` has SHA-256 digest `eeaf7d9b323a5f14c5cb3819385b216e15ff50f662c194ec8d3622d3ccdbd064`.

### Directed mutation evidence

The final mutation evidence uses exact candidate and mutated Git trees and requires the intended behavioral oracle to fail. A mutation compile error, transport error or unrelated assertion does not count as a kill.

- `s00-mutation-v2` run `34574727544`: **22/22 jobs passed**. The campaign covers dispatcher reentry/abort/orphan, SAT cutoff/subnormal, body-path certificates, invariant-plane budget, tiny rotation, receipt tick/surface, frame binding generation, model unknown-joint/degenerate geometry, thread ownership, endpoint serial reuse, first-capture quarantine, lifecycle registration watermark, authoritative catalog revision, client receipt authority, client carry authority and zero-thickness extraction.
- `s00-hierarchy-mutation` run `34574727548`: **1/1** directed unknown-joint mutation was killed by the exact hierarchy assertion.
- `s00-suspension-mutation` run `34574727576`: **2/2** lifecycle mutations passed their kill oracles, independently proving rebind-generation invalidation and support-instance identity.

Total final directed mutation result: **25/25 killed**, with no surviving mutation in the reviewed campaign.

### Red-before-green lifecycle evidence

Before the final suspended-pair repair, run `34545247231` materialized the hostile candidate and executed **242 server tests**. Exactly two required tests failed:

- `s00lifecycle_tests_rebind_clears_suspended_pair_quarantine` because explicit support rebind retained quarantine from the retired binding;
- `s00lifecycle_tests_replacement_support_cannot_inherit_suspended_pair_by_uuid` because a distinct support instance could inherit suspended-pair state through reused UUID.

The real-client/integrated/dedicated lane on that same red server candidate remained green. The failures were classified as lifecycle/identity implementation bugs before production was changed. The final tree binds suspended-pair quarantine to weak support-instance identity plus the local registration generation, and both corresponding directed mutations are killed.

Earlier attempts to construct the UUID-reuse holdout that failed to compile or failed to create the required suspended precondition are classified as **test defects** and are not evidence of a production failure.

### Limits of this evidence

S00 does **not** claim that G1–G5 are implemented. In particular, the public API/binding/codecs remain G1 work; `AnatomyMovement` decomposition, live `MaterialEventDispatcher` integration and removal of the oversized broadphase global fallback remain G2; canonical catalog/engine/runtime lifecycle and removal of world scans remain G3; final receipts/prediction/reconciliation/presentation remain G4; and the legacy `platform` physical motor remains scheduled for removal in G5.

The S00 execution log and component classifications are in `docs/sprints/S00-foundation-audit.md`. They are process records, not additional normative requirements.
