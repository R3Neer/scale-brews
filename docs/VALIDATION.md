# Validation record

This file is the sole source of truth for **executed evidence**. It records what was actually built, run or inspected, against which snapshot, and what that evidence does **not** prove.

## Tiny Mount SeatFrame candidate (after beta.7)

This branch replaces direct `body` / `bone` anchors and the previous-frame entity-ID cache with an autodetected, complete `SeatFrame`. Compile-time checks cover the minimal gameplay codec, legacy `saddle_visual` decoding, strict visual-profile values, nested/rest-scaled `ModelPart` resolution, explicit-path override, same-frame topological ordering and cache clearing. Release still requires the complete base client suite, the pinned EMF 3.3.5 + ETF 7.2 + Fresh Animations 1.10.5 lane, and the front/side/rear/top manual capture and two-minute traversal matrix for Bee, Chicken and Wolf. Do not treat compilation alone as aesthetic acceptance.

## Current prerelease: 0.1.0-beta.7 — 2026-09-14

Normative entity-collision behavior lives in [ENTITY_COLLISIONS_REQUIREMENTS](ENTITY_COLLISIONS_REQUIREMENTS.md), architecture/API/data ownership in [ENTITY_COLLISIONS](ENTITY_COLLISIONS.md), and implementation order/status in [ENTITY_COLLISIONS_PLAN](ENTITY_COLLISIONS_PLAN.md).

## Evidence rules

- Source/test existence is not passing evidence.
- Kernel/unit evidence does not substitute for real GameTest/client/dedicated/compatibility/latency/human QA when required.
- Evidence attaches to an exact snapshot or is labelled historical/isolated.
- Compatibility evidence is version-bounded.
- Packaging proves packaging only unless gameplay was separately exercised.
- A later red snapshot can reopen a previously green gate; history is retained as red-before-green evidence.

## 0.1.0-beta.8 release candidate — 2026-09-14

This release replaces direct `body` / `bone` anchors and the previous-frame entity-ID cache with an autodetected, complete `SeatFrame`. Checks cover the minimal gameplay codec, legacy `saddle_visual` decoding, strict visual-profile values, nested/rest-scaled `ModelPart` resolution, explicit-path override, same-frame topological ordering and cache clearing. The final source passed the complete base client suite and the pinned EMF 3.3.5 + ETF 7.2 + Fresh Animations 1.10.5 lane. Sixteen mounted, unmounted, movement and pounce snapshots for Bee, Chicken and Wolf were inspected directly; saddle orientation and rider contact remained coherent in those captures, and the native Horse stayed outside this renderer path. The planned two-minute-per-creature human-controlled play session and broad multiplayer latency check were not performed; publication remains explicitly prerelease-scoped.

## Previous prerelease: 0.1.0-beta.7 — 2026-09-14

- The guarded prerelease lane passes **154 required server GameTests**, the complete real-client/integrated-server GameTest suite, packaged-version/test-class checks, tag verification and a post-upload SHA-256 comparison of the regular JAR.
- Tiny Mount interaction is validated by `family`, including data-only Cow adversaries proving `direct` / `item_steered` behavior without species branches, dynamic equipment menus, Bee player-inventory UX, vanilla saddle shearing/drop behavior, Wolf owner protection and BODY-before-SADDLE shearing, plus the real dispenser equipment path.
- Animated saddle/rider attachment uses the final rendered mount transform. A separate reproducible optional-mod lane passed with the VanillaPlus-pinned **EMF 3.3.5 + ETF 7.2 + Fresh Animations 1.10.5** stack and verified an active EMF bee model plus a finite, non-identity rider attachment transform.
- Size presentation/reach coverage includes scale-dependent step pitch and gait timing plus the final reach contract: Growth block +20% per equivalent level, Growth entity +30%, and Shrinking block/entity -10%.
- Normal gameplay still uses the released upper-surface living-platform system. All-direction anatomical collisions and shared Clinging Reoriented physics remain unfinished and disabled in ordinary worlds. Full-pack human acceptance and broad multiplayer/mod-combination QA remain bounded separately.

## Historical validation records

The dated sections below are retained as evidence of earlier milestones. They may describe behavior or balances that were later superseded; current gameplay contracts live in the player/configuration/mechanics references and the beta.7 section above.

## 0.1.0-beta.6 candidate — 2026-09-11

- Candidate scope adds optional effective-gravity support for Scale-generated Tiny Mount movement: bee flight, chicken glide and wolf pounce/landing use the root mount's effective gravity frame rather than assuming world-down.
- Gravity Changer is the concrete provider recognized automatically. It is not a hard dependency; without a provider, the shared frame service resolves to vanilla `DOWN` and the existing Tiny Mount behavior is retained.
- Normal gameplay still uses the released upper-surface living-platform system. All-direction anatomical collisions and the shared Clinging Reoriented physics remain unfinished and disabled in ordinary worlds.
- Prerelease publication is gated by `.github/workflows/prerelease.yml`: the exact release tree must pass `git diff --check`, build, every required server GameTest, the real client GameTest suite, and packaged-version checks before the workflow may create a tag or GitHub prerelease.
- Full-pack human acceptance, broad optional-mod combinations and multiplayer latency/host-change coverage remain bounded separately; a passing prerelease lane does not certify every modpack.

## 0.1.0-beta.5 candidate — 2026-09-09

Snapshot `33b1f5c`:

- fresh isolated `build runClientGameTest --offline` passed **122 required server tests** and the integrated-client suite;
- GitHub Actions run `34373390000` passed;
- released beta.5 uses the legacy upper-surface Living Platforms engine; the all-direction replacement is not a released feature;
- installed regular JAR SHA-256 `5cbee58b3da9628f53f267b35ab3a3d16d922c4d2af6dfbf5b31a4d126711fc7`.

This does not certify the whole VP26 pack, every optional mod, or the in-development replacement collision system.

## Released gameplay regression history

Historical server/client suites cover Growth/Shrinking mechanics, movement/jump/fall/reach/health, brewing/beacons, Growth landing impacts, world interactions, villager fear, material loot/harvesting, Tiny Mount chicken/bee behavior and wolf saddle/trust/attack behavior. These runs are regression oracles for released gameplay, not acceptance of the replacement collision architecture.

### Released Living Platforms legacy engine

Historical evidence includes oriented top-surface collision, ratio boundaries, walls/jumping/sneak edge, support translation/yaw, chains, item/minecart/boat/falling-block/anvil semantics and no global `noCollision` rewrite.

A real-client dedicated proof completed **0 / 100 / 200 ms RTT** with `allow-flight=false`; a player transport soak reached **12,000 ticks**. Later bounded VP26 runs reached 106 passing tests after an e4mc permission compatibility issue was repaired. This evidence describes the temporary legacy engine that G5 must retire after feature-equivalent replacement.

## All-direction entity-collision historical evidence

GitHub Actions runs both the build/server suite and the real client GameTest under Xvfb on Ubuntu with Java 25 for every push and pull request. Screenshot interpretation remains a human/local task, but client assertions now run in CI and CI uploads logs/screenshots for inspection. Packaging checks ensure production JARs include sprites/languages/tags/mixins but exclude GameTest classes and test-only mixins. `git diff --check` checks patch whitespace.

Historical prototype work demonstrated:

- original Minecraft cow/player ModelPart extraction and a bounded Alex's Mobs grizzly slice;
- hierarchy/piece/transform/filter preservation checks and early pose equivalence comparisons;
- convex overlap/separation/raycast, six cardinal gravity frames, temporal envelopes and bounded overlap recovery;
- root translation/rotation/scale and selected joint interpolation fixtures;
- prototype catalog/protocol/epoch/revision/integrity paths and server-owned pose/contact publication;
- one integrated scenario with 60 real transport ticks on a moving cow;
- a bounded Clinging proof compiled against an earlier protocol and completed 22 server GameTests.

Those results do not prove final Q2, final lifecycle, prediction/reconciliation, complete vanilla/mod coverage or current consumer compatibility.

### Preserved historical red scenarios

Snapshot-021 retained two physical red cases that remain required regression targets: initial floor contact during combined support yaw/ascending motion, and contact reacquisition after animated squeezing.

## G0 structural baseline — 2026-09-10

Pre-restructure snapshot `5a8bf915d22d52cf4e79a68e2506d199bb6e848e`, run `34449689179`, failed at `compileGametestJava` because two dispatcher fixtures used a stale constructor after the event-budget parameter was added.

Post-cleanup commit `420c305e301a3082df119a14a7db81f1fd2423d1`, run `34462591148`, passed the ordinary repository workflow.

Package migration was verified in run `34464719776`, job `102830447888`: fresh `clean build`, **137/137 required tests passed**. Verified transformed commit `0ededb67464b47fb35170f8d9854cf62498fc626` moved the replacement system into `collision.api`, `geometry`, `pose`, `physics`, `internal`, plus client preparation/network packages, removed package shims and removed production grizzly-specific registration/eligibility.

G0 proves structural cleanup and compilation, not G1+ physical completion.

## S00 Foundation Audit — accepted 2026-09-11

Logical source tree `68244f2acbebccfd0dec607220726b9ed7e56e8a` was materialized/verified by transport commit `a5503f97a7320e416cd253357c6c8f3cb5b002e3`.

GitHub Actions run `34574727529`:

- `./gradlew build --rerun-tasks` → **242/242 required server GameTests passed**;
- real-client/integrated lane under Xvfb/llvmpipe passed;
- cow/player wide/player slim original-model exports and pose comparisons passed;
- **640 additional vanilla-family pose comparisons** passed;
- dedicated `allow-flight=false` completed **0 / 100 / 200 ms RTT, 120 ticks each**;
- `S00_CLIENT_RECEIPT_AUTHORITY PASS`;
- `S00_OBSERVER_AUTHORITY PASS`;
- evidence artifact `S00-34574727529`, SHA-256 `eeaf7d9b323a5f14c5cb3819385b216e15ff50f662c194ec8d3622d3ccdbd064`.

Directed mutation evidence: `s00-mutation-v2` 22/22, `s00-hierarchy-mutation` 1/1 and `s00-suspension-mutation` 2/2. **25/25 directed mutations killed**.

Red-before-green lifecycle run `34545247231` found exactly two required bugs: suspended-pair quarantine surviving explicit rebind and inheritance through support UUID reuse. Both were repaired before S00 closure.

S00 does not claim G1-G5 implementation.

## G1 public contract and canonical data — CLOSED 2026-09-11

Status: **G1 is closed after two adversarial red-before-green cycles.** G2 may begin only from this closure snapshot/evidence.

### Candidate progression and evidence correction

Early S01-S04 green runs remained at the S00 count because the new GameTest classes compiled but were not registered. `144470d769869426d8ee4be57912b9468a25e79e` fixed registration, so later runs execute the actual G1 assertions.

The non-adversarial candidate `b1d7c3b204412119d2c5ee708d3784f231a7db76` passed run `34588346710`, job `103227629075`, with **262/262**, plus provisional client proof `34588346767` / `103227629118`.

### First independent adversarial red-before-green

The adversarial campaign expanded the suite to 266 tests. Run `34592683287`, job `103241351742`, snapshot `19626950569110963cd47611b245234c650f2ca0`, failed exactly two required tests:

1. S01: `AnatomyBackend` was public under `collision.api`;
2. S03: canonical codecs accepted unknown/legacy fields because the underlying record codec ignored extras.

Both were implementation bugs. No test or normative requirement was weakened.

`3ba014dc2e19b6b700a707bd3177c55ac443328f` moved the backend contract to `collision.runtime` and made canonical codecs strict, including nested objects.

Historical green after that repair:

- run `34593762577`, job `103244714934` → **266/266**, `BUILD SUCCESSFUL`;
- artifact `10260227920` has SHA-256 **`67622ea0e2f440d6ac6f66abe860ffc6d8403f3649e129ff9c98b035a59e45f0`**;
- the previously recorded different digest was a documentation transcription error and is superseded by this raw-log value;
- client proof run `34593970131`, job `103245364717` passed original cow/player exports, pose/family comparisons, receipt/observer authority and dedicated 0/100/200 ms RTT.

### Second adversarial reopening: NFR-025 layer cycle

A later structural holdout checked NFR-025 literally. Although the backend type had moved out of the consumer API, `AnatomyApi` referenced `collision.runtime.AnatomyBackend` and that runtime contract referenced API DTOs, creating `collision.api ↔ collision.runtime`.

`94f2db96277f02c9e9d4903bdb02046b7878f4c2` added `S01PublicApiBoundaryTests.publicApiAndRuntimeDoNotFormALayerCycle` without changing production. Run `34595220211`, job `103249293688`, executed **267 tests**: 266 passed and exactly that new holdout failed.

`be41003bbceaecd27a36dc1d13e4bef0f8bde114` added S03 `constructorAcceptedBoundaryDataRoundTripsCanonically`. Run `34595505502`, job `103250215902`, executed **268 tests**: the S03 boundary property passed; the same S01 layer-cycle test was the only failure.

This is intentional red-before-green evidence. It reopened G1 rather than being hidden behind the earlier green.

### Final repair and server evidence

`a09c881a6a526bb735fe9e5f9f4a26d74325e615` (`fix(collision): break api runtime layer cycle`) changed the backend contract to use neutral runtime-owned records/enums and JDK/Minecraft types. `AnatomyApi` performs conversion to/from public DTOs at the façade; `collision.runtime.AnatomyBackend` no longer references `collision.api`.

GitHub Actions run **`34600301662`**, job **`103265686046`**:

- **268 tests registered and executed**;
- **268/268 required GameTests passed**;
- `BUILD SUCCESSFUL in 1m 14s`;
- artifact **`10264495705`**;
- artifact SHA-256 **`8676c2ddc0099ac0a34d647c426539b88b323fee53d9ccab552e34630f9534b4`**.

This run includes the adversarial S01 layer-cycle holdout and the S03 maximum-boundary round-trip holdout.

### Final real-client / integrated / dedicated evidence

A temporary workflow was added only to execute the special client lane. Trigger commit `5d725ae700c812f7864a22dc0459b9617f52e822` changed workflow configuration, not production/tests relative to `a09c881…`.

`g1-client-proof` run **`34600577409`**, job **`103266578934`** executed `xvfb-run -a ./gradlew runClientGameTest` and completed `BUILD SUCCESSFUL`.

Observed acceptance markers:

- `ANATOMY_EXPORT minecraft:cow vertices=240 pieces=10` and 80 animated pose comparisons;
- player wide/slim: 144 vertices / 6 pieces and 80 pose comparisons each;
- **640 additional vanilla-family comparisons passed**;
- `S00_CLIENT_RECEIPT_AUTHORITY PASS real client clear/record isolation and server invalidation`;
- `S00_OBSERVER_AUTHORITY PASS direct and transitive carry keep remote roots read-only`;
- dedicated `Platform dedicated soak passed RTT 0 ms (120 ticks)`;
- same for **100 ms** and **200 ms** RTT.

Hosted ALSA/narrator/X11/profile-service warnings were environmental and did not fail asserted lanes.

The temporary workflow was removed in `1c591578f2fac8406bd69fa81dd08000b19b5b7e`; production/tests therefore return to the ordinary workflow-only tree.

### Final G1 review

Reviewed after the server and client proofs:

- the adversarial tests remain intact;
- no `AnatomyBackend` exists in `collision.api`;
- `collision.runtime.AnatomyBackend` has no `collision.api` dependency;
- API conversion remains at the façade;
- canonical codecs reject unknown/legacy fields and exact-boundary objects round-trip;
- `CollisionBindingCatalog` validates registered geometry/pose/root ids and preserves deterministic/fail-closed selection;
- binding/policy remain independent of legacy surfaces/`automatic_top`;
- body policy/adapters share the canonical integration layer;
- external fixture registration is public-API + declarative JSON;
- G1 did not divide `AnatomyMovement`, integrate the live Q2 dispatcher, replace G3 lifecycle/catalog or remove the G5 legacy motor.

No further G1 implementation change was identified. **G1 is closed.**

## G2 start state — 2026-09-11

G2 begins only after the preceding closure. Before any G2 source change, the live tree still had the known Q2 debt deliberately deferred from S00/G1:

- `AnatomyMovement` owned provider registration, state/history, spatial broadphase, queries, contact, root tracking and carry in one class;
- `MaterialEventDispatcher` existed as a generic causal scheduler but was not integrated into live root/joint/carry hooks;
- `GeometryProvider.MotionIntervalHandle`/`MotionSnapshot` existed, while live movement still consumed causal endpoint state in several paths;
- `indexedSupports` fell back to adding **all** indexed support bounds when an oversized query exceeded `MAX_INDEX_CELLS`, and added the `overflow` list to every ordinary query;
- comments in the code explicitly deferred temporal spatial envelopes and certified support-interval consumption to Q2.

## G2 / S05 bounded material broadphase — historical closure 2026-09-11

S05 initially attacked only the spatial-cost/fail-closed thesis, not the still-open continuous-causality requirements FR-049..051.

Kernel commit `dbcf315a9ac00d77b43cf442bb2596030b1518c3` added `collision.physics.MaterialBroadphase<K>` with:

- identity-based entries/deduplication;
- caller-supplied deterministic ordering;
- explicit entry-cell, query-cell and candidate budgets;
- `COMPLETE`, `BUDGET_EXHAUSTED` and `INVALID_QUERY` query outcomes;
- entry rejection reasons;
- `requestedCells`, `cellsVisited` and `candidatesVisited` instrumentation;
- no global fallback and no partial candidate publication on exhaustion.

Live integration commit `541a333140543fb1df55e61b4aa78bb2f54f84d7` replaced the embedded `AnatomyMovement` spatial structure:

- removed the query fallback that scanned all indexed bounds;
- removed the global per-query `overflow` list;
- quarantines oversized support entries until explicit rebind;
- propagates exhausted/invalid spatial queries conservatively: `spaceClear=false`, `collide=Vec3.ZERO`, ray/sweep publish no material result;
- keeps the broadphase input as material `AABB` so later Q2 can replace endpoint bounds with certified temporal envelopes without replacing el kernel.

GitHub Actions run **`34602837677`**, job **`103274063124`**:

- **277 tests registered and executed**;
- **277/277 required GameTests passed**;
- `BUILD SUCCESSFUL in 1m 13s`;
- artifact **`10265820434`**;
- SHA-256 **`1573c97d70c3b254dcf6957b57c695c9004b0a37cd8acec1301b7e84cbec8f28`**.

This was a valid closure of the original kernel/fail-closed scope, but later adversarial live-wrapper evidence reopened S05 under NFR-008. The historical run remains evidence for the kernel and is not erased.

## G2 adversarial campaign — historical red-before-green evidence 2026-09-11

### S05 live-wrapper locality reopening

The reusable `MaterialBroadphase` kernel remains bounded, but the live wrapper around it exposed world-sized work in the movement/query path.

`9b34ec8f909f278082a75931d014867c596f0fef` repaired the steady-state case: a repeated local query in the same tick no longer re-samples every registered provider. Subsequent holdouts then moved the adversarial boundary to **legitimate mutation hooks**.

Run **`34626316654`**, job **`103352177773`**, snapshot `f06ec043ff5c5ea4936829d94e7114cf8eb1cd64`:

- **308 GameTests**;
- **306 passed / 2 failed**;
- steady-state locality passes;
- `S06SupportedMembershipCausalityTests` passes using `GeometryProvider.tick(...)`, including material geometry outside the support's vanilla AABB;
- `S07BudgetFailureLocalizationTests` passes;
- `S05LiveBroadphaseLocalityTests.firstLocalQueryAfterLegitimateRebindMustNotResampleFarWorld` fails with **128 far-provider samples** on the first local query after a supported local rebind.

Run **`34626763741`**, job **`103353628494`**, snapshot `834fd694619ad899f9d238143af9af9ac4980409`:

- **309 GameTests**;
- **306 passed / 3 failed**;
- post-rebind locality still fails with **128 far samples**;
- `firstLocalQueryAfterSupportedRootCommitMustNotResampleFarWorld` also fails with **128 far samples** after `captureRoot → setPos → commitRoot`, with counters reset only after the supported hook;
- the third failure is the older direct-`setPos` legacy fixture. It is not needed for acceptance because the supported root-commit holdout independently reproduces the defect.

A further holdout, `S05DirtyMutationLocalityTests.manyFarRebindsMustNotBeProcessedByUnrelatedLocalQuery`, performs 64 supported far-world rebinds, resets counters after those hooks, and then makes an unrelated local query. It is designed to reject a global dirty-list drained by the first query.

Current snapshot **`91d0be66ef21f4d50c91861084e2680844b65580`**, run **`34627841257`**, job **`103357151536`**:

- **311 GameTests**;
- **306 passed / 5 failed**;
- S05 failures include:
  - the historical direct-`setPos` stale-index fixture;
  - post-rebind locality: **128 far samples**;
  - post-root-commit locality: **128 far samples**;
  - many-far-dirty-rebind locality: **128 far samples**;
- the fifth failure is the independent S07 plan-conflict blocker below.

The contractual S05 blockers are therefore the supported rebind/root-commit/far-dirty cases. They permit eager maintenance inside mutation hooks by resetting counters afterwards; they constrain only the subsequent physical query path, matching NFR-008.

A discarded adversarial test that silently mutated an internal provider from `UNAVAILABLE` to `AVAILABLE` between queries without `tick`, rebind, packet binding or callback is **not** acceptance evidence and was removed. Production providers advance through known causal hooks; requiring discovery of an invisible mutation would imply global polling and conflict with NFR-008.

### S06 causal membership / identity

The earlier S06 red-before-green campaign found conflicting same-serial payloads, wrong-support interval certification, `Pending` support/handle mismatch, stale binding reuse and unbounded staging. Those were repaired.

`S06SupportedMembershipCausalityTests` is the valid membership holdout: availability changes through `GeometryProvider.tick(...)`, then the canonical runtime rebuild must expose the new material geometry. This test is green in the runs above. There is no active S06 adversarial blocker in the current snapshot.

### S07 contact/local-failure progression

The S07 campaign produced several red-before-green repairs before the later blockers:

- exact `t=1` contact with zero body displacement;
- contact provenance in a joint batch with a merely-near distractor support;
- starting-overlap failure must suspend only the bad body/support pair and preserve a bystander;
- same-binding reacquisition after the overlap disappears;
- numerical budget exhaustion without initial overlap must release/suspend an old retained contact instead of leaving uncertainty authoritative.

The provenance repair is green in run `34623887683`, job `103344184321`. The budget-uncertainty holdout is green in later runs, including the `91d0be66…` baseline.

### S07 historical blocker: mutually incompatible valid plans

`S07PlanConflictLocalizationTests.simultaneousValidPlansThatWouldOverlapMustReleaseOnlyAffectedPairs` creates two retained contacts on opposite material walls. Each candidate is first proven independently to have a complete bounded temporal plan. Those plans move the bodies toward one another and their planned final AABBs overlap. A third body has an unrelated valid contact on the same support.

The fixture originally hit floating-point/coordinate artifacts because GameTest worlds can have very large absolute coordinates. Snapshot `91d0be66ef21f4d50c91861084e2680844b65580` constructs the convexes in a small local frame and translates them in double precision, so every fixture precondition reaches the intended production branch.

In run **`34627841257`**, job **`103357151536`**:

- both individual temporal plans are `COMPLETE`;
- the planned simultaneous finals overlap;
- backend returns `QUARANTINED / BACKEND_EXHAUSTED`;
- no partial displacement is applied;
- the test then fails exactly because the two affected retained body/support relations remain authoritative instead of being released/suspended;
- the third bystander is retained as the locality control for the eventual fix.

This was a real **NFR-004 / FR-052** blocker in the `worsensBodyOverlap(...)` failure path, distinct from the already-repaired `plan()==null` budget case. It was subsequently repaired and is retained here as red-before-green history.

### Prepared client/server evidence during G2

The isolated prepared-anatomy lane is green after the causal/binding repairs:

- run **`34623128548`**, job **`103341673751`** passed original client geometry export plus the deterministic prepared pair-suspension proof;
- run **`34624096154`**, job **`103344876628`** also passed the client export and prepared server proof.

These special lanes do not override red ordinary holdouts on the snapshots where those reds existed.

### Historical G2 interpretation at snapshot `91d0be66…`

- S05 was **reopened** under NFR-008 for post-mutation live-query locality. The immutable/build-only kernel itself remained supported by its historical green evidence.
- S06 identity/causal membership had no active blocker.
- S07 was **reopened** under NFR-004/FR-052 for the valid-plan conflict path.
- G2 could not close while either S05 or S07 remained red.

This paragraph records the state of that historical snapshot only. Later repairs and closure evidence supersede those active-red conclusions; they are preserved rather than rewritten out of the red-before-green history.

## Current acceptance gaps for entity collisions

The canonical open work is in `ENTITY_COLLISIONS_PLAN.md`. G2 is closed. Major unproved areas now begin in later gates:

- G3 catalog/binding lifecycle, reload/reconnect, root-provider generalization, reusable family engines and final separation of internal endpoint/root types once that frontier is genuinely acyclic;
- G4 prediction/reconciliation for locally controlled actors and latency-sensitive multiplayer presentation;
- G5 special body categories, placement semantics and final removal of the legacy Living Platforms motor;
- G6 generic family coverage and `UNRESOLVED=0` for ordinary Minecraft 26.2 living entities;
- G7 final Clinging Reoriented migration;
- G8 version-pinned VanillaPlus compatibility;
- G9 normative performance benchmark and final client/dedicated/latency/soak/QA acceptance matrix.

The remaining `FRAME_SERIALS` + root history inside `AnatomyMovement` is **not** an untracked G2 gap: `GeometryProvider.CausalEndpoint` still contains `AnatomyMovement.RootFrame`, so extracting that block before G3 would require an inverse dependency or prematurely redesign root/lifecycle. The canonical plan records it under G3.

The gravity-authority reconciliation is also closed: S11 established `io.github.r3neer.scalebrews.integration.gravity.GravityFrames` as the single shared Scale body-gravity authority.

## Reproduction commands

Ordinary core build/server suite:

```powershell
.\gradlew.bat build
```

Real client/integrated-server suite:

```powershell
.\gradlew.bat runClientGameTest
```

Optional compatibility jars:

```powershell
.\gradlew.bat runGameTest runClientGameTest -PscalebrewsCompatMods=C:\path\to\test-mods
```

The preparation harness is `tools/PrepareAnatomyProof.ps1`. Pin every external input/version used as evidence. GitHub Actions ordinary workflow covers the Gradle build/server path; client/dedicated/compatibility lanes count only when their exact snapshot/command/result is recorded here.

## G2 / S07 rejected-batch contact validity reopening — 2026-09-11

The earlier plan-conflict locality defect is closed, but a later adversarial holdout reopened S07 under NFR-004.

Green evidence immediately before the reopening:

- source repair `29d4595f38f7ff3c44f9af7ebb98512cdd16f93d` localized body/body conflicts by responsible support;
- run `34633241588`, job `103374905847` passed **319/319 GameTests**;
- run `34633398671`, job `103375427902` passed **320/320 GameTests**, including the cross-tick S05 locality holdout;
- prepared proof run `34633591227`, job `103376062867` passed original-client geometry export plus the isolated prepared server runtime.

New red-before-green evidence:

- `S07RejectedBatchContactValidityTests.rejectedConflictMustStillReleaseRetainedFaceThatMovedAway` creates a simultaneous A/B joint batch;
- support A is the cause of an otherwise valid body/body plan conflict;
- the left body retains a valid contact on support B before the event;
- B's own certified interval moves that retained floor away by `0.4`, while the batch is rejected because of A;
- the fixture starts B at gap `0.01`, inside the `0.025` retention tolerance but outside the CCD skin, and models B as rigid translation, so it does not manufacture a `t=0` temporal contact;
- run **`34634310744`**, job **`103378374367`**, snapshot `a48f4e6e960e5b1adf4bd937e3275e838cdb7ad3`, executed **321 GameTests**: **320 passed / 1 failed**;
- the single failure is exactly `NFR-004 forbids preserving a retained B face that the same rejected batch certifies has moved away`;
- all fixture preconditions passed, the individual plans were `COMPLETE`, the batch was rejected as `BACKEND_EXHAUSTED`, and no partial body displacement was applied.

An earlier version of this holdout that started B at exact tangency entered `ITERATION_LIMIT` and is explicitly **not** production evidence. `a48f4e6e…` corrected the fixture before the valid red above.

This reopening does **not** reopen S05 or S06. It demonstrates a distinct S07 rule: after a rejected batch, causal attribution of the conflict and final material validity of retained contacts are separate checks. A repair must preserve the existing stable-B multi-support holdout while releasing a B contact whose own certified `after` frame no longer validates it.

## G2 / S07 rejected-batch contact validity repair and closure — 2026-09-11

Production repair `50dfac066248679397d463ca74e1b6cefb9f38a7` adds a body-local post-rejection retained-contact revalidation against the retained support event's certified `after` frame. It checks active identity and registration generation, revision consistency, piece, face, gap, eligibility and gravity support. It does not apply any rejected-batch displacement and does not call support-global `invalidateSupport(...)`.

Ordinary candidate evidence:

- run **`34635928336`**, job **`103383672978`**, snapshot `50dfac066248679397d463ca74e1b6cefb9f38a7` passed **321/321 required GameTests** and finished `BUILD SUCCESSFUL`; artifact **`10278451232`**;
- independent log inspection reconfirmed the exact checkout, all **321** registered tests, **321/321** pass marker and artifact SHA-256 **`746517246992ab17c054ff459bb97a13c3a300c1647ccaa3b5c480cb26eeaca4`**;
- the temporary prepared-harness trigger `faa6b1524db3f78fea2adc83fc53caf6239d49a0` changed workflow configuration only, not production/tests; its ordinary run **`34636187333`**, job **`103384539908`**, again passed **321/321 required GameTests** and produced artifact **`10278876352`**.

Prepared client/server evidence on the repaired candidate:

- run **`34636187384`**, job **`103384540056`** checked out `faa6b1524db3f78fea2adc83fc53caf6239d49a0` and exported original client geometry successfully with `BUILD SUCCESSFUL`;
- observed client markers include cow `240 vertices / 10 pieces`, player wide/slim `144 vertices / 6 pieces`, 80 animated comparisons for each primary model and **640 additional vanilla-family comparisons**;
- the generated catalog at `build/run/clientGameTest/anatomy-export` was then consumed by the isolated prepared server lane;
- the prepared server executed **2/2 required GameTests** and both passed; that phase also finished `BUILD SUCCESSFUL`.

The temporary prepared workflow was removed in `c7c297e26c3781e3270be126889f5d2f8a68930c`, restoring the ordinary workflow-only tree. `49158d377497b5a62aac2a20dc8dfd61006a07c8` was the stable post-cleanup tree inspected for the independent closure review; production/tests remained those of the repaired candidate.

### Independent adversarial zero-change review

I16 reviewed the remaining candidate surfaces without changing production or tests:

- **candidate/batch exhaustion before `plan(...)`:** overflow occurs before a complete body/support relation set is available, so event/support quarantine is a conservative fail-closed result and is distinct from pair-local `BACKEND_EXHAUSTED` after complete capture;
- **reacquisition after final-invalid release:** A12 uses `AnatomyMovement.clear(body)` rather than persistent pair suspension, clearing stale contact/surface/anchor/receipt state while permitting later valid reacquisition;
- **multi-support order:** candidate bodies are canonicalized by UUID/id, contact piece keys are scoped by support UUID/material serial/piece and plan/contact selection uses ordered collections where order can matter; existing permutation holdouts remain green and no new event-order dependency was found in the separated causal-attribution/final-validity phases;
- **lifecycle during capture/resolve:** `MaterialIntervalRuntime.poll(...)` and `AnatomyRuntime.acceptsIntervalIdentity(...)` fence registration/binding/epoch/revision identity before preparation, while capture/resolve runs synchronously under the dispatcher's reentrancy gate on the world thread; no mid-resolution asynchronous lifecycle seam was found.

A possible anchor/carry concern was also inspected and deliberately not promoted into an S07 blocker: FR-056..060 and productive `DERIVED_CARRY` were outside that sprint's declared scope and were later closed by S08.

No further S07 implementation change was identified. **S07 is closed after the independent zero-change review.**

## G2 / S08 continuous anchor transport and derived chains — closure 2026-09-11

S08 closed the retained-contact carry and derived-chain slice of G2: material anchors follow certified continuous trajectories, obstruction is checked over the path, passive transport updates receipts/passengers once, and an actively bound transported support emits a causally parented `DERIVED_CARRY` rather than a second opportunistic root event.

### Ownership red-before-green

Prepared run **`34645201815`**, job `103414197619`, snapshot `0f4f4ec99b0e6168bf93d542bc953e6967710fca`, exported original client geometry successfully but failed the prepared server proof at `anatomicalRootTransportOncePrepared`: a manually registered support inside an otherwise prepared session lost its legacy fallback because the S08 fence treated session ownership as support ownership.

Production repair **`fc4ef518bbb8b357d8f3ee062d15caf4493e4f7c`** narrowed the internal `LivingEntity` ownership query to runtime-active bindings while preserving the public session-level meaning of `AnatomyRuntime.owns(Entity)`. Prepared run **`34645613492`** then passed both the manual fallback regression and the real prepared A→B→C chain.

A later experimental holdout demanding immediate legacy carry after direct `setPos` on an active runtime binding was withdrawn before closure: that binding can publish the mutation at its material cadence, and forcing an additional immediate fallback could violate FR-050/051 by double consumption. It is not acceptance evidence.

### A10 permutation / active bystander red-before-green

The final holdout runs the same prepared A→B→C chain under two registration orders while a tiny active cow lies inside A's conservative material envelope but outside A's material, outside the B→C setup envelope and on the side from which A moves away. It is therefore an eligible broadphase candidate but intentionally non-causal.

After correcting an earlier contaminated fixture placement, prepared run **`34647526137`**, snapshot `8d28f3f8d26ec9abcceb921295e29f5090387294`, reached all fixture preconditions and failed only when A moved: B stayed at `(0,0,0)` instead of receiving the expected `(+0.2,0,0)`. The live backend was classifying every active `LivingEntity` candidate without `plan.transport()` as `INVALID_DERIVATION`, even when its planned displacement was exactly zero. The stationary bystander therefore aborted the causal chain.

Production repair **`0a6914ba355021b0e0e8c29a32eee0a32cb0adbc`** keeps the invalid-derivation fence for **non-zero** active movement without certified transport, but lets a zero-displacement active bystander remain a harmless candidate. This preserves fail-closed provenance for actual derived motion without inventing a ROOT contribution for stationary geometry.

### Final S08 evidence

Both final lanes executed against the same production snapshot `0a6914ba355021b0e0e8c29a32eee0a32cb0adbc`:

- ordinary run **`34647905669`**, job `103422980830`: `build` completed successfully; artifact **`10282667053`**, SHA-256 **`99e91b2e8093ee65737dbdd00784f7d924590c8ac526ef04032f1d70dadc3c24`**;
- prepared adversarial run **`34647905593`**, job **`103422980690`**: original client geometry export succeeded, catalog location succeeded and the isolated prepared server proof completed successfully, including the A10 bystander-first/chain-first permutations.

The final source review found live consumers for the S08 ownership/planner/transport path: the narrow runtime ownership fence is consumed by legacy `carry`, `MaterialPhysicsRuntime` consumes `AnchoredTransportPlanner`, applied certified carry records through `recordCertifiedTransport(...)`, and an actively transported support produces `MaterialIntervalRuntime.deriveRoot(...)` plus `derivedInterval(...)`. No S08 production helper/state without a real consumer was identified.

At this S08 snapshot, **G2 remained open** for tangential retention, multicontact/sliding, recovery/wall-squeeze/intermediate-only contact and remaining budget observability. The S09 closure below supersedes that active-gap statement while preserving it as historical state.

## G2 / S09 multicontact, sliding and recovery — closure 2026-09-12

S09 closed the remaining physical Q2 slice owned by G2 tasks 5, 7 and 8: gravity-relative tangential retention/sliding, deterministic multi-contact response, bounded initial-separation recovery, pair-local wall-squeeze failure, strictly intermediate temporal contact and explicit budget boundaries. It did not close G2's architectural partition/ownership tasks 1 and 9 at that historical point.

### Recovery frontier red-before-green

The exact recovery-budget holdout initially could not calibrate a 128/129 boundary: nested separation paths generated thousands of numerically distinct `Vec3` offsets for the same physical candidate because raw floating-point sums were used as queue identity.

`dd2299c74a678fbc392bb780745b5ba7bb38d361` canonicalized recovery candidates on a `1e-8` grid before enqueue, 100× finer than the `1e-6` escape skin. Diagnostic evidence changed the pathological family from `>4096` explored states to a linear physical frontier: 120 slabs → 121 candidates, …, 127 → 128 and 128 → 129. `S09SeparationBudgetBoundaryTests` retains the permanent exact-boundary/live-clip assertions; the temporary calibration diagnostic was removed in `dbc2a9aabebe092b6dfd54c8b8b222a6490b76f8`.

### Prepared intermediate-contact red-before-green

The real prepared cow A9 holdout uses a 120° root-yaw interval with both endpoints clear and a tiny stationary body intersected only in the interior. Earlier temporal screening exhausted the contractual 256-evaluation budget even when direct CCD could complete inside that budget.

The repair sequence tightened rigid hierarchy speed bounds without weakening conservatism, added adversarial sampling that checks the bound over 1000 steps × 8 vertices, and made `TemporalResponse` probe cheap direct CCD queries before paying for broader certified screening. No response budget was raised. The cleaned production snapshot `05e8ad9c90bc4c9f09a5d47d49e929c8a080b628` preserves the physical A9 assertions without the exploratory diagnostic payload.

### Clean ordinary and prepared evidence

Ordinary workflow run **`34659541784`**, job **`103458981698`**, on the cleaned production snapshot completed successfully.

Prepared adversarial run **`34659541774`**, job **`103458981680`**:

- original client extraction succeeded;
- cow export: **240 vertices / 10 pieces**;
- player wide/slim: **144 vertices / 6 pieces** each;
- primary animated-pose comparisons and **640 additional vanilla-family comparisons** succeeded;
- the isolated prepared server suite executed **2/2 required GameTests** and both passed;
- the real A9 strictly-intermediate cow contact completed within the unchanged 256-evaluation response budget.

### Exact budget edge and final-review evidence

`80fc63386dc0ae5faf10ce28f4658aa2d24c5fec` hardened A11 so the own-move boundary is an exact N-1/N oracle: N-1 completes below budget and N exhausts exactly at 256, with live fail-closed zero displacement/contact/debt on exhaustion. Its ordinary CI run completed successfully.

A later final-review pass found no production defect but did find misleading executable documentation: the method formerly named `screeningCannotCostMoreThanTheClearSweepItReplaces` did not assert a strict cost ordering; it asserted the normative property that screening cannot turn a direct CLEAR fitting the same budget into exhaustion. `2aca10ef37c8fdbde85a26e9a4a5c0067189eb0e` renamed it to `screeningCannotExhaustWhenDirectClearFitsSameBudget` without changing its oracle or production. Run **`34659926225`** completed successfully.

No production code changed after the cleaned prepared proof `05e8ad9…` in this historical closure phase; subsequent changes were boundary-test hardening, test naming and documentation/CI trigger bookkeeping. Hot-path inspection found bounded local entity queries rather than world scans, and the prepared workflow was extended so future `HierarchyMotion` changes trigger the real-geometry lane.

## G2 architecture partition S10-S13 and integrated Q2 evidence — 2026-09-12

The physical Q2 work remained closed while four ownership extractions reduced `AnatomyMovement` without moving physical decisions into storage owners:

- **S10:** `collision.runtime.TransportLedger` owns passive transport current/history/generation/cursors; `AuthorityPoseTracker` consumes it directly.
- **S11:** `integration.gravity.GravityFrames` is the single shared Scale body-gravity authority; the former `AnatomyMovement.GRAVITY` and `collision.internal.GravityFrames` owners are gone.
- **S12:** `collision.internal.AnatomyContactState` owns retained contact, sequence watermark, anchor, `SurfaceContact` and pair suspension state. Cross-dimension cleanup is fenced by both body/support level without rewinding surviving sequence state.
- **S13:** `collision.internal.AnatomySpatialIndex` owns per-level/tick bounded membership and broadphase budgets. Dead `FrameStamp/frames` metadata was removed; same-tick mutation remains local, legacy rebuild samples each provider once, and stale callers cannot rebuild a newer index backwards.

S13 prepared validation exposed an independent late S09 defect: `TemporalResponse.validCorrection(q)` paid twice per static piece and could exhaust the unchanged 256 budget after a real prepared A9 contact. `de44c78c75bcc54ef423af783d35761014b49356` preserves route validation plus final `SKIN+ULP` clearance while using one budgeted material sample per piece. No response/separation budget or numeric skin was raised.

Final integrated evidence at that stage:

- production snapshot `de44c78c75bcc54ef423af783d35761014b49356`, ordinary run **`34695839944`**, job **`103559056801`**: **380/380 required GameTests passed**; artifact **`10299110544`**, SHA-256 **`58c33a23d8a8cbd9256736e56d55ffeee43b93cb7b02dcf5df2e8d78d1b7985b`**;
- the same production snapshot passed prepared run **`34695839860`**, job **`103559056605`**, including original client export and isolated prepared server **2/2**;
- test-only hardening `a1f1a568726d95664c6e8b4a144659c36be0e5cc` re-ran prepared A9 under several explicit world translations; run **`34696004864`**, job **`103559482598`**, remained **2/2** with cow `240 vertices / 10 pieces`, player wide/slim `144 vertices / 6 pieces` and **640** additional vanilla-family pose comparisons;
- ordinary run `34696004871`, job `103559482658`, on that test-hardened snapshot remained **380/380**; artifact `10298651622`, SHA-256 `2d9d4275bccba84139b0a9d95425197c019a36d6fb28a1d18e6ef0f298137131`.

At that historical point, this evidence closed S10-S13 and the then-known late S09 budget reopening. It did **not yet** close G2 tasks 1/9 because provider/binding/causal-endpoint/query/root ownership still required the S14 audit below.

## G2 / S14 binding-state ownership and final G2 closure — 2026-09-12

S14 audited the remaining provider/binding state and found a real separable owner plus one real production defect. It deliberately did **not** move endpoint/root/query state whose current type graph would create an inverse dependency or pre-empt G3.

### S14 red-before-green baseline

Commit `5019fd7f7116c0c79291556fe41a8d70f2c95941`, run **`34696527544`**, job **`103560853344`**:

- **386 GameTests executed**;
- **383 passed / 3 failed**;
- structural holdout failed because `AnatomyBindingState` did not yet exist and `AnatomyMovement` still owned provider/descriptor/generation/quarantine/capture state;
- dependency-direction holdout failed because the owner was not yet auditable;
- causal-rebind holdout failed with **`samples=3`**: `register(support, provider, descriptor)` first passed through the descriptorless fixture registration path and sampled the provider before installing causal identity.

All other required tests remained green, so this is a clean S14 baseline rather than a mixed regression.

### S14 production repair

`f8693c1d17ac0b73b95aeec74e5b2a57490fe9e1` migrated live binding ownership to `collision.internal.AnatomyBindingState`:

- weak-identity slot owns provider, optional descriptor, monotonic local generation, current-generation quarantine and reentrant capture guard;
- causal rebind installs provider + descriptor + generation atomically and increments local generation exactly once;
- `deactivate(level)` removes the active binding/quarantine without rewinding the generation watermark;
- an outer capture retains its guard through rebind/deactivate until its `finally`;
- stale capture fences compare provider + descriptor + local generation;
- `AnatomyMovement` no longer declares the five former binding-state owners;
- endpoint serials and root history did not change ownership or semantics.

### Final ordinary evidence

Run **`34708981487`**, job **`103594130965`**, snapshot `27a915aba32e0113f5e587c594187783498375a4`:

- **386/386 required GameTests passed**;
- `BUILD SUCCESSFUL`;
- artifact **`10302359158`**;
- SHA-256 **`c6f506d4cc33cb3d2071d4534eb61057114ab116d53626d4350e4776038ce363`**.

This ordinary tree contains the complete S14 owner/migration/holdouts.

### Final S09 reopening discovered during S14 and prepared evidence

A later prepared A9 run found another legitimate world translation where the first midpoint-positive causal cow piece needed more than the fixed 8-iteration cheap probe. The contact itself remained valid and the global response budget stayed **256**; the inefficiency was only the work schedule before establishing the first `earliest` contact.

Production commit **`d04874085b60ff4c9aaef7246928cf8379240484`** adds an adaptive first-contact probe:

- only a midpoint-positive candidate while no `earliest` exists may receive an enlarged local probe (80);
- after the first contact is known, later pieces use the normal 8-iteration probe again;
- all evaluations remain charged to the same global 256 budget;
- no piece is omitted and CCD, `q`, `SKIN`, `TIME_EPS`, horizon, simultaneity and live fail-closed behavior are unchanged.

Workflow **`34708981488`**, job **`103594131113`**, validated the exact patch before push:

1. checkout of `27a915a...`;
2. apply the exact `d048740...` patch in workspace;
3. full ordinary suite green;
4. original client geometry export green;
5. isolated prepared server suite **2/2 green**;
6. only then create/push `d048740...`.

This workflow validates the final production source containing both S14 and the final S09 hardening. The push made by `GITHUB_TOKEN` did not create a redundant second workflow run, so the pre-push workspace run is the exact special-lane evidence for `d048740...`.

### Final G2 ownership review

Post-green source review found no additional G2 production change:

- `TransportLedger` owns passive transport state;
- shared `GravityFrames` owns body gravity;
- `AnatomyContactState` owns retained contact/anchor/suspension state;
- `AnatomySpatialIndex` owns bounded spatial membership/budgets;
- `AnatomyBindingState` owns live provider/descriptor/generation/quarantine/capture state;
- `AnatomyMovement` remains the live query/orchestration boundary and retains `FRAME_SERIALS`, `RootFrame`/`RootHistory`, activation and sweep metrics.

The remaining endpoint/root block is not another separable G2 owner: `GeometryProvider.CausalEndpoint` directly contains `AnatomyMovement.RootFrame`. Moving `FRAME_SERIALS` without redesigning root would create an inverse dependency back to the orchestrator; moving/redesigning root now would pre-empt G3's `RootTransformProvider` and lifecycle work. Task 9 requires types to leave `collision.internal` only when actually decoupled, so leaving this block internal is the compliant result, not an unfinished extraction.

**G2 is closed.** Its evidence proves the bounded continuous-material Q2 pipeline and the ownership partition described by S05-S14. It does **not** prove G3 catalog/root lifecycle, G4 prediction/reconciliation, G5 removal of the legacy engine, G6 complete vanilla coverage, G7 Clinging migration, G8 VP26 compatibility or G9 final benchmark/acceptance.

## G3 / S15 prepared catalog bundle reuse — closure 2026-09-13

S15 cierra G3 tarea 2 / NFR-010. `WorldAnatomyCatalog` publica cada revisión como `Snapshot + AnatomyCatalogTransfer.PreparedBundle`; preparación de bytes/digest/fragments ocurre antes del swap, `AnatomyRuntime` usa `preparedPackets(epoch)` y `AnatomyNetworking.sendCatalog(...)` no recibe `models/profiles`. Repetir receptor con el mismo `epoch + revision` reutiliza la misma lista preparada; replacement inválido conserva el par anterior y cambio de revision/epoch rematerializa sólo headers/payloads desde fragments ya preparados.

Los ocho holdouts S15 se añadieron antes del camino productivo en `6b360fe9ec5c635cf3e9cdf89b848b543241f017`. La primera lane dedicada, run **`34746882338`**, job **`103696316689`**, ejecutó **9/9 required GameTests** verdes, pero el job terminó rojo porque la aserción de CI esperaba ocho casos totales y Fabric añade `minecraft:always_pass`; fue un fallo de la lane, no del producto. `c49087ec095160043db142dcc7774945dc2bfbdd` corrigió esa aserción.

Evidencia final exacta sobre **`e52766a71cf66c4157d31b8884d901b22d4de7a8`**: run **`34747160506`**, job **`103697083788`**, fase focal **8/8 S15** + `minecraft:always_pass`, seguida de suite ordinary **394/394** incluyendo los ocho holdouts. Artifact **`10313754760`**, SHA-256 **`5a3a20f13c7f24726cee3ed6af5baa3a1f3f63c4c6b5347c69c5063dfba42f57`**. Build run **`34747160495`** concluyó **success** sobre el mismo snapshot.

La revisión adversarial final recorrió owner, hot send, fixture encoder, invalid replacement, revision/epoch fences, bounds e inmutabilidad. No encontró una segunda ruta live de serialización/hash/fragmentation ni owner duplicado; desde `684142f32003a29f255b8ff204d39a7e0a968ed2` no hubo cambios adicionales de producción/tests S15. **La pasada final produjo cero cambios de producto; S15 queda cerrado y G3 permanece abierto para tareas 1 y 3-12.**


## G3 / S16 canonical catalog authority — CLOSED 2026-09-13 after adversarial reopening

Snapshot de evidencia `0b7a8940e783f3b8e08128f9d95fa04688376e79`; último cambio productivo propio `3a3503b1cba944933eaaf3cf010e8be981ed7d8d`.

S16 sustituyó la autoridad anatómica legacy por `CollisionBinding` canónico: `WorldAnatomyCatalog` conserva el catálogo completo y un bridge precomputado ejecutable acotado, `AnatomyCatalogTransfer` usa protocolo v4 con `{models, bindings}`, cliente y servidor validan identidad por `geometry.model + pose.engine`, y la policy activa acompaña al binding vivo. `PlatformDefinition` queda antes de la frontera mediante migración explícita; las superficies legacy no se promocionan a anatomía y una sesión anatómica no cae silenciosamente a Living Platforms cuando el binding es disabled, variant-only o todavía no ejecutable.

Red-before-green relevante: la primera migración dejó consumidores server/client en el schema de perfiles; después la retirada de la tabla temporal expuso autoridad dual de policy; un holdout posterior detectó fallback legacy dentro de sesión anatómica; y el último holdout prepared detectó que un provider manual podía perder la policy canónica. Las reparaciones culminaron en `3a3503b1...`; los helpers/workflows temporales se retiraron antes de la evidencia final.

GitHub Actions ordinary run **34752369790**, job **103710940047**: **398/398 required GameTests passed**, `BUILD SUCCESSFUL`; artifact **10315852333**, SHA-256 **`a1de8633e76ed2df2516b47fe549a83e4aff5eaf208765cca34088db4737f036`**.

Lane focal S16 run **34752369719**, job **103710939888**: **5/5 required S16 GameTests passed**, `BUILD SUCCESSFUL`; artifact **10316377128**, SHA-256 **`429aac1871a8fcb108766c12f0b7dba9eeeac06bf018408be181fbf7c5eba205`**.

La revisión final de catálogo, transfer, runtime, cliente, binding state, movimiento y frontera Living Platforms no produjo cambios de producción en aquella campaña. Esa evidencia constituyó el cierre provisional de G3 tarea 1, pero quedó invalidada por el holdout adversarial post-cierre descrito a continuación.


### Post-closure adversarial reopening — variant bridge validation

Commit de holdout **`e3e49ac2a80ea1729c20dfe75ef3a7095e1ea25e`**. La prueba `variantBridgeReferencesMustValidateBeforeAtomicPublication` construye un `CollisionBinding` del bridge precomputado con selector variant no vacío y `geometry.model=proof:missing_variant_model`, conserva referencias al snapshot y al bundle preparado aceptados, y exige que `WorldAnatomyCatalog.replace(...)` rechace el candidato antes de publicar nada.

GitHub Actions focal run **`34753107721`**, job **`103712855904`**: compilación main/client/GameTest correcta; servidor GameTest iniciado; **6 required S16 GameTests ejecutados, 5 verdes / 1 rojo**. El único fallo es el holdout nuevo: el candidato variant-only inválido es aceptado. La causa observada es que la validación del bridge consulta sólo `canonical.resolve(entity, Map.of())`, por lo que un selector no vacío queda fuera de la validación de referencias aunque forme parte del candidato canónico.

Esto reabre **FR-033/FR-036 y G3 tarea 1**. Que la selección runtime de variants quede fuera del scope de S16 no permite publicar datos declarativos con referencias inválidas. El candidato completo debe validarse antes del swap; tras el rechazo deben conservarse exactamente el snapshot y el `PreparedBundle` anteriores. La evidencia 398/398 + 5/5 previa sigue siendo histórica, pero ya no es suficiente para cierre porque no contenía este holdout.


### Repair and renewed closure after variant-validation reopening

Production commit **`5cff913349a4fc64921a81e0d8236e5aae235ac9`** validates the complete canonical candidate before publication. Every binding touching the legacy compatibility IDs must either form the full bridge or reject; every full bridge validates its model reference, legacy provider parameter/provider compatibility and filter before any default selector is resolved. Only after those fallible steps does S16 derive the currently executable `variant={}` subset. Variant-only bindings therefore remain canonical data without becoming accidental default execution.

The red holdout `variantBridgeReferencesMustValidateBeforeAtomicPublication` is green without relaxation, and invalid candidates preserve the exact previously accepted `Snapshot` and S15 `PreparedBundle` objects.

A subsequent adversarial-only commit **`d218adc84a0180e02dd5908114bacf010c4ee86e`** added two further cases: canonical plus migrated legacy bindings cannot share one selector by merge/file-order precedence, and a valid variant selector must survive protocol-v4 round-trip while remaining absent from the default executable map. The existing invalid-wire replacement/rejectPending fence also remained active. No production change followed this test expansion.

Renewed exact evidence on `d218adc...`:

- ordinary run **`34753580865`**, job **`103714077843`**: **401/401 required GameTests passed**, `BUILD SUCCESSFUL`; artifact **`10315699079`**, SHA-256 **`9cd3c32a251988793cd2df1bcb145b0003d2c4b8e149e65832e0473b6d8a004e`**;
- focal S16 run **`34753580858`**, job **`103714077812`**: **8/8 required S16 GameTests passed**, `BUILD SUCCESSFUL`; artifact **`10316528568`**, SHA-256 **`f4ca1104ee19347997d456e5719da9ea5fd107fd1941eb39e9b8c7d9e2bb5f7a`**.

Supplemental prepared evidence on production `5cff913...`: the first attempt of run **`34753380100`**, job `103713567025`, failed in the unrelated S09 prepared A9 temporal-response translation oracle after client export succeeded. Re-running the exact same job/SHA as **`103714031864`** completed the same export and prepared server proof successfully. The first failure is retained as a non-reproduced S09 incident and is not used to waive or replace any S16 oracle.

Final review after the repair found no further S16 production change. The later adversarial expansion was green on the existing repair, satisfying the zero-change final-pass criterion. **S16 and G3 task 1 are closed; this does not close G3 tasks 3–12.**

## G3 / S16 final post-reopening evidence — closure 2026-09-13

The S16 record above ended on the first fully green expansion, but one further adversarial client-style rejection case was executed before later G3 work. Commit **`eabd6ef31ee8f6ed4a7e4fffa873171339db508f`** added a full real-wire invalid-model candidate and proves that rejection preserves the exact accepted snapshot/revision and that `rejectPending()` recovers `READY` in the same epoch.

Final S16 evidence:

- ordinary run **`34753730671`**, job **`103714474314`**: **402/402 required GameTests passed**; artifact **`10316763087`**, SHA-256 **`5c2ee0b2f3673ec238ebced823cf32eee89efe2fa05cdc15c8295676ad1cf54a`**;
- focal run **`34753730753`**, job **`103714474664`**: **9/9 required S16 GameTests passed**; artifact **`10315833864`**, SHA-256 **`5401316de24eb5db5c9be7ab63c81da1b01cb8d87ec739c3779c88852f8e5564`**.

No S16 production change followed `5cff913349a4fc64921a81e0d8236e5aae235ac9`. This supersedes the 401/401 + 8/8 intermediate evidence as the final S16 closure snapshot while retaining that earlier evidence as history.

## G3 / S17 ModelPart GeometryEngine — closure 2026-09-13

S17 closed G3 task 3. `scalebrews:model_part` is a common/server-safe geometry engine fed by client-only ModelPart preparation. The final adversarial campaign, ending at test commit **`11824a121b669eeab7cd77704afad6bce4b0c254`**, covered classloading/constant-pool isolation, delegate ownership, failure isolation, structural exclusions and deterministic repeatability without requiring another production repair.

Final evidence:

- ordinary run **`34755126613`**, job **`103718098834`**: **407/407 required GameTests passed**; artifact **`10317615494`**, SHA-256 **`93ce1322a6186ed211bb55dbfb6ba17de5b9d47a1f02ccf9df29f5ce9091dd0c`**;
- focal run **`34755126630`**, common job **`103718098895`**: **6/6 required S17 GameTests passed**; artifact **`10317310881`**, SHA-256 **`3a1c2de697169e59256a75c4b78ea54d63820a1972d1ce70d0d2d99ed8598534`**;
- same focal run, client preparation job **`103718098982`**: success; artifact **`10317930031`**, SHA-256 **`501503b498e9cb3462b9ca7515839e4dd626ccc1dd15cd3f660830ca14c9b81a`**;
- same focal run, original-model job **`103718098983`**: success; artifact **`10316049789`**, SHA-256 **`22fc133027782e612b388a53197737ef4bfbb198679a06eb44da39e4e0181d9c`**.

Observed original-model evidence includes cow **240 vertices / 10 pieces**, player wide/slim **288 vertices / 12 pieces** under the newer extraction, 80 animated comparisons per primary family and **640 additional vanilla-family comparisons**. The final adversarial pass required no S17 production change.

## G3 / S18 vanilla PoseEngine + Mojang keyframe program — historical closure 2026-09-14

This was the first S18 closure and was later reopened by stronger original-source rest-transform oracles. At that historical point, vanilla procedural families had canonical `PoseEngine` ownership in `CollisionEngines.pose`; live authority/history/wire evaluation uses `PoseEngine.Inputs`; `PoseProviders` remains only as a read-only legacy adapter. `scalebrews:mojang_keyframes` evaluates revision-owned, common/server-safe `PoseProgram` data compiled from Mojang `AnimationDefinition` on the client/tooling side. Protocol v5 synchronizes **`{models, pose_programs, bindings}`** and preserves the S15 prepared-bundle and S16 atomic-rejection invariants.

The post-implementation adversarial campaign found real defects rather than expanding requirements:

- duplicate keyframe timestamps were accepted; production repair `1c2e4d5...` changed the ordering fence to reject equality;
- the evaluator collapsed Mojang discontinuity semantics at an exact keyframe timestamp; `49d828a12e9fec7d75c6b1d5ee1760ce6b7fbb7e` restored exact `preTarget` at the timestamp and `postTarget` immediately after/final clamp;
- revision and wire program identifiers were hardened to canonical form in `1fbf2d71341e69bce662bc989dca32eefc490894` and the final S18 production commit **`9ff92bb6f6bf93844516643e1b2fcac8ca3405a6`**.

Final focal evidence on run **`34819384389`**:

- common/dedicated job **`103897173738`**: **21/21 required S18 GameTests passed**, including semantic revision ownership, exact N/N+1 bounds, SPI SAM compatibility, required-channel/fail-closed behavior and common-runtime client-class isolation;
- client compiler/original-source job **`103897173936`**: success with `S18_ANIMATION_COMPILER_BOUNDARY PASS` and `S18_ANIMATION_DEFINITION PASS original Minecraft 26.2 parity, loop wrap and fail-closed compiler`; artifact **`10337647129`**, SHA-256 **`50df0d9d0050009b08afc1f4e85a65da6a811e6a89bdea8971e534b2d7ec8cce`**.

Final ordinary evidence after repairing the historical reload fixture:

- run **`34819573289`**, job **`103897783223`**: **407/407 required GameTests passed**, `BUILD SUCCESSFUL`; artifact **`10337702118`**, SHA-256 **`d12a251fd777d0e5bfd41c63585f99ffbdf9d4eca3a8eef7ba55da947444a741`**;
- normal build run **`34819573066`**, job **`103897782597`**: **407/407 required GameTests passed**; artifact **`10336964165`**, SHA-256 **`4f4699a8e336703bfe29b54dde8b3c3954d9c9cf44626184cef26d86b89015c8`**.

S16 regression was re-run on the exact final production commit `9ff92bb6f6bf93844516643e1b2fcac8ca3405a6`: run **`34818534247`**, job **`103894502904`**, **9/9 required S16 GameTests passed**, `BUILD SUCCESSFUL`; artifact **`10337526551`**, SHA-256 **`4a87841f15ee620f8f2ac6df56de0b4b17e1fb8632ec211f56d9944654bc9404`**.

The final second-read compare from `9ff92bb6f6bf93844516643e1b2fcac8ca3405a6` to checkpoint `22b1803784b7792163b881ae49254efaa2dd8c2c` changed only tests, documentation and CI; no `src/main` or `src/client` production file changed. Temporary helper workflows were removed before closure. Sprint documentation closed at `5dfec1b2f67e027bf7d91289f3aae37984bc4758`, and the canonical G3 plan marked task 4 closed in `509946f80ead5c0ce4d693ea2faa741f49ef9c43`.

**Historical conclusion only:** this closure was later superseded by the retroactive reopening recorded below; its evidence remains valid for the coverage it actually exercised.


## G3 / S18 retroactive reopening and final reclosure — 2026-09-14

The historical S18 closure above was reopened after stronger client oracles proved that a local affine matrix is insufficient authority for Mojang animation semantics: distinct Euler representatives and signed/non-unit rest scales can be matrix-equivalent at rest yet diverge after `ModelPart.offsetRotation` / `offsetScale`. This was a real model-data gap, not a test-only disagreement.

The repair campaign converged on exact neutral source fields plus native sequential application. `ModelGeometry.SourcePose` now preserves local position/Euler/scale fields; the keyframe evaluator applies tracks in source order from those fields; keyframe interval selection, `preTarget/postTarget`, Catmull-Rom edge control points, negative Java remainder, duplicate-target accumulation and finite runtime vectors match the original source semantics. Bindings also preserve `applyWalk` clock/amplitude transforms explicitly and the final production repair **`3ff54a708e0c0ac93f81dc0f26d4b6be13e44897`** reproduces `AnimationState`'s age-clock integer-millisecond truncation rather than approximating it with continuous seconds.

### Exact final campaign

On production snapshot **`3ff54a708e0c0ac93f81dc0f26d4b6be13e44897`**, GitHub Actions reported **17 workflow runs, all successful**, and a filtered query for failures returned **0 runs**.

Focal run **`34876662857`** passed all six jobs:

- `common-authority` — job **`104085426886`**;
- `client-compiler-boundary` — job **`104085426627`**;
- `client-euler-representative` — job **`104085426367`**;
- `client-non-unit-rest-scale` — job **`104085426719`**;
- `client-signed-rest-scale` — job **`104085426792`**;
- `client-signed-scale-representative` — job **`104085426707`**.

The same production snapshot also passed the isolated `applyWalk`, apply-walk clock quantization, duplicate-target order, Catmull first/right boundary, first-keyframe exact/boundary, single-keyframe, negative-loop, effective-channel-budget, vanilla/equine/bee procedural parity, implementer-regression and S16 catalog lanes. Ordinary build run **`34876662817`** was green as well.

A later test-only commit, **`6a72c9f96bd4d19b1c1a02b0fa5a474cbb7fe884`**, permanently locks the age-clock double-truncation case. It changed no production and its ordinary build run **`34876732944`** completed successfully.

### Final zero-change review

The closing TM reread re-inspected the canonical registry/SAM, built-in procedural engines, legacy read-only adapter, `PoseProgram`, evaluator, exact `SourcePose` export, client compiler, revision-local catalog/program ownership, protocol-v5 bundle, authoritative tracker/history/payload, bound provider and runtime bridge. It identified **no further production change**.

The compare from the last production change `3ff54a7...` to pre-documentation checkpoint **`4afcc19dcdb9c683125e874ec1e3f16894e7ebec`** contains only the S18 regression test plus documentation/CI work; there is no later `src/main` or `src/client` S18 modification. The transitional S16 execution bridge remains intentionally outside this closure's replacement scope: it already binds the canonical `PoseEngine.Bound`; generic root/lifecycle removal belongs to later G3 tasks.

**Final conclusion:** S18 satisfies its 11 closure criteria and **G3 task 4 is closed again after the retroactive reopening**. G3 remains open for tasks 5–12, with task 5 / S19 as the next productive gate.

## G3 / S23 DisplayRig generic-SPI proof — adversarial closure 2026-09-16

S23 closes G3 task 7 / FR-018 without inventing a production DisplayRig target. The implementer proof registers a synthetic `GeometryEngine`, prepares common `ModelGeometry`, materializes ordinary `ConvexBox` geometry and exercises the existing raycast/overlap path. Its ordinary candidate evidence was run **`35103246319`**, job **`104817741616`**, with **425/425 required GameTests**; focal run **`35103246410`**, job **`104817742057`**, passed the isolated S23 proof.

The independent adversarial close strengthened the oracle rather than changing production:

- `ff074173e7a1b1e694f695fc52e4bbc8c4876d28` adds `S23AdversarialGeometryEngineOpacityTests`: two engines with different ids, model ids, parameters, `source` and `version` must materialize identical bounds and produce identical common raycast and `ConservativeSweep` results when their physical geometry is identical;
- `199bff049506692a2d104518b2588770a3c0612c` adds `tools/s23_spi_only_gate.py`, which rejects nominal DisplayRig/display-composite knowledge under production `src/main` while no real target exists;
- `a02e04f695944686f4cc1dcbf1619670c2e45cbb` adds the focal adversarial workflow and two directed mutation campaigns;
- `d76194273874b5ca9697cd6cb075e843fd648d82` makes that workflow watch the permanent GameTest registration;
- `2ffeb627bc9b397b417393eba6454c49a2a714e9` permanently registers the adversarial holdout in the ordinary suite.

Final adversarial run **`35104624130`** on `2ffeb627bc9b397b417393eba6454c49a2a714e9` passed all three jobs:

- **`104822510244`** `engine-opacity`: source gate clean and isolated physical holdout green;
- **`104823016011`** `metadata-branch-mutation-kill`: a compiling mutant that makes `ModelGeometry.evaluate(...)` translate only `source=synthetic_display_composite` is **KILLED** by the holdout;
- **`104823015908`** `source-boundary-mutation-kill`: an injected production `DisplayRigSpecialCase` is **KILLED** by the SPI-only source gate.

The earlier full campaign run **`35104004704`** on `a02e04f...` was also green and killed both directed mutants before permanent registration. No production defect was exposed, so the adversary made no `src/main` repair: the weakness was the strength of the original proof, not observed runtime behavior.

This evidence proves the current pre-target FR-018 boundary: after a geometry engine emits common geometry, physical behavior is family/metadata-opaque under the exercised raycast/sweep paths, and current production contains no DisplayRig/display-composite hardcode. It does **not** prove extraction or semantics of a future real DisplayRig target. The nominal source gate is intentionally temporary and must be refined or retired with new evidence when such a target actually lands, not bypassed with silent exceptions.

**S23 is independently closed and G3 task 7 is closed.**

## G2 / FR-053 late push reopening — final adversarial recertification 2026-09-18

The late FR-053 reopening separated geometric blocker ownership from vanilla pairwise `Entity.push`. The original READY holdout proved that cancelling push for a managed material pair erased the vanilla impulse. A later adversarial handoff holdout then exposed a second boundary: legacy `PlatformState.support` acquired before READY could survive until the next cleanup tick and continue cancelling push after shared physics had already taken ownership.

Final production repair: **`1437795ce18689282b4532fc3644bc4566cd1f58`** (`fix(g2): release legacy push guard on anatomy handoff`).

The adversarial oracle was hardened in **`5b4b516b0d460671d15ec6e53fe42011dff85a9d`** so the same production-acquired pair proves both sides of the ownership cut: before shared ownership, legacy support still suppresses pair push; immediately after READY handoff, without waiting for a cleanup tick, both caller directions reproduce the previously measured vanilla response exactly.

Final push evidence:

- `g2-vanilla-push-proof` run **`35327758929`**, job **`105544729000`**: success;
- ordinary build on the same SHA: **`35327758984`**: success;
- artifact **`10539368207`**, SHA-256 **`865ff29a05c58c7f4205f11713c005d5e000352abaf5eab4bbfd0417d4095c6b`**.

The no-wall lane was also hardened to trigger on `PlatformEntityMixin.java` changes in **`47db343dde7db545e48f39f548840956977ab9ad`**. Its recertification run **`35327844908`**, job **`105545001610`**, passed; build **`35327844879`** also passed. Artifact **`10539502957`**, SHA-256 **`4a4c8f7159217a2804d98da7fb749847bccddf914c153968cabc7297390ceb45`**.

The single-geometric-owner proof on the production fix itself remained green in run **`35205403082`**: normal job **`105149830359`** and `pair-suppression-mutant-must-die` job **`105149830787`** both succeeded. Normal artifact **`10489777286`**, SHA-256 **`4f4ed28a6f592f0075607a17dfa12d1923d3c4ea9492e5dd2af5be927ecf1077`**; mutant artifact **`10489462405`**, SHA-256 **`ec93434638b9b1c89348823f81abc6d3fb0af43552d3018a3969f1e2b2859a2f`**.

A compare from `1437795...` to `47db343...` contains only the adversarial GameTest hardening and CI trigger change; no later `src/main` or `src/client` change exists.

The contemporaneous S08 prepared/boat red lanes are not attributed to this repair: the boat lane already failed with the same assertion on earlier SHA `bdbd267d2862e55533609dfda9c229906e82bd1b` (run `35130296400`) before `1437795`. They remain separate evidence/debt and are not used to weaken or inflate the FR-053 result.

**Final conclusion:** FR-053/FR-093 are adversarially recertified: ineligible pairs gain no anatomical wall; eligible pairs retain one anatomical geometric blocker; the geometric-suppression mutant is killed; vanilla `Entity.push` is preserved exactly once; and legacy→READY ownership changes immediately without waiting for the next tick. **G2 task 10 and the global G2 gate are closed again.**

## G3 / S22 coverage provenance — final adversarial closure 2026-09-18

S22/G3.8 closes the deterministic coverage-classification kernel and its acceptance provenance boundary. The implementer repair **`50296a6792f523e57fcd8daa71ba6f598254a6e6`** made completeness and provenance independent: exact discovered membership is always checked, while non-empty resolved classification claims require a private scanner-issued provenance capability.

The final adversarial hardening **`942aa4fec0b372dd63de8f7916f91a54bd66d956`** adds the missing positive oracle. The same automatically discovered Minecraft target used by the forged-row negative cases is supplied with one valid canonical default binding per discovered id through `CollisionCoverageDiscovery.scan(...)`; every row must be `FULL` and the scanner-issued artifact must successfully pass `requireResolved()`. This prevents a blanket reject-all implementation from satisfying the negative provenance holdout.

Mutation-gate commit **`fd9ca047f3df0549f21672e94f6adaa706ace588`**, run **`35328441530`**:

- baseline provenance job **`105546923380`**: success;
- provenance-bypass mutant job **`105547301841`**: success as mutation harness. Disabling only the acceptance fence makes forged `FULL` coverage resolve, the GameTest fails at the intended FR-038/NFR-032/NFR-036 assertion, and the harness reports `KILLED`;
- provenance-issuance mutant job **`105547301819`**: success as mutation harness. Dropping only `SCANNER_PROVENANCE` from the canonical scan path leaves forged claims rejected but causes the positive control to fail with `Coverage artifact lacks canonical scanner provenance`; the harness reports `KILLED`.

Artifacts:

- baseline **`10539264816`**, SHA-256 **`959a68766aea46ee2895e0e503b53de48bd7f30a29eeebdbbca4b2f5686769ef`**;
- bypass mutant **`10540212060`**, SHA-256 **`3c93fa3e7db005f8f9bd19c8e33826f525c707fa94562ed07ea2f01de85e9d95`**;
- issuance mutant **`10539374622`**, SHA-256 **`94b4eb5b9605fb7d70ec817a253b5db09135887c63cc3a711a7a41a7bb058c4b`**.

Ordinary build run **`35328441560`** on the same snapshot is green.

The earlier independent S22 barriers remain in force: completeness has its own self-justifying-membership mutation kill; canonical digest framing is length-injective; mixed default/variant state gaps are deterministic; and the hot-path structural gate plus its runtime-reference mutant remained green through later production snapshot `1437795...` (residual run **`35205403247`**).

Final zero-change review compares `50296a6...` with the adversarial closeout lineage and finds no later change to `CollisionCoverageDiscovery.java` or `CollisionCoverageScanner.java`; post-repair S22 changes are tests, CI and documentation only.

**Conclusion:** S22 is independently closed and G3 task 8 is closed. The next open G3 work is S24/tasks 9-12.

## G3 / S24 tracking-generation read purity — adversarial RED 2026-09-18

S24 tasks 9-12 remain open. The independent lifecycle review repaired two stale mutation harnesses before classifying product behavior:

- dimension lifecycle mutation workflow run **`35329062829`**: baseline job **`105548909539`**, tracking-generation mutation job **`105548909156`**, and catalog-scope mutation job **`105548909537`** all succeeded. The tracking mutant now targets `TrackingGenerationLedger.release(...)`, the current owner of retirement semantics.
- reconnect lifecycle workflow run **`35329603429`**: baseline job **`105550626278`**, stale-history mutation job **`105550626526`**, and stale-catalog mutation job **`105550626580`** all succeeded after reanchoring cleanup mutations to the current split level/connection teardown.

A distinct production RED remains.

Holdout **`S24AdversarialTrackingReadPurityTests`**, test commit **`d63deca5c4d16445d9349cb5613abbe4b53d3954`**, isolated workflow commit **`4ec3eb69fc70fee760fba34716bbd261a4ede6af`**. The recipient is created with `makeMockServerPlayer(...)` and explicitly verified absent from `PlayerList`, so no `START_TRACKING` transition can authorize the recipient/body pair. A single call to the public observation façade `AnatomyRuntime.trackingGeneration(recipient, body)` must therefore report `TrackingGenerationLedger.UNAVAILABLE == 0`.

Run **`35329517023`**, job **`105550355468`**: failure at the intended causal assertion:

```text
A read of an untracked recipient/body pair must return UNAVAILABLE and must not acquire tracking authority; observed=1 on tick 0
```

Artifact **`10541105179`**, SHA-256 **`ccd06b1e6f39d3e66670f7315f1599b313531abfce3c2f52e495d6f20fc1bd99`**. Ordinary build of the same snapshot, run **`35329516917`**, succeeded.

The source cause is direct: `trackingGeneration(...)` calls the same helper used for authoritative acquisition, which performs `TrackingGenerationLedger.acquire(...)`. Production callers include transport receipts and one live pose-send overload. A read can therefore create the authority it claims merely to observe.

**Classification:** PRODUCT RED. G3.9 stays open pending an implementer repair that separates observation from acquisition and revalidates all callers without reviving nonexistent or retired tracking windows. Full handoff: `docs/sprints/S24-adversarial-tracking-read-revival-red.md`.

## G3 / S24 lifecycle, unavailable, packet order and ownership — final adversarial closure 2026-09-18

S24 closes G3 tasks 9-12 after one additional product reopening and several oracle/CI hardenings.

### G3.9 — lifecycle and tracking authority

The adversarial read-purity holdout proved that `AnatomyRuntime.trackingGeneration(recipient, body)` could create the authority it claimed to observe and returned synthetic generation `1` even without an active runtime. Product fixes:

- **`256ab44b434af4d7cfae2da37bda7dc873444e58`** — separate current observation from acquisition;
- **`99969ccb4e44b86c28f6898b62c4752781679949`** — explicitly bootstrap windows already justified by vanilla tracking when runtime starts/resets.

Run **`35333686648`**:
- baseline **`105563538619`** success;
- read-acquires mutant **`105563913730`** killed;
- no-runtime-default mutant **`105563913814`** killed.

Dimension and reconnect mutation campaigns were reanchored to the current implementation and are fully green: runs **`35329062829`** and **`35329603429`**. Pose/contact dimension replay, contact support-rebind, tracking-ledger bounds/reuse, reload and player-list iteration also remain green on the integrated S24 lineage.

### G3.10 — explicit UNAVAILABLE and recovery

`S24UnavailableRecoveryClientProof` was hardened so the unsupported SLEEPING phase must accept an explicit newer pose endpoint with `available=false` before TTL expiry; mere disappearance after timeout cannot satisfy the oracle. Recovery must then accept a strictly newer AVAILABLE serial.

Run **`35335621883`**:
- baseline **`105569661575`** success;
- recovery-freeze mutant **`105570042027`** killed;
- UNAVAILABLE-publication suppression mutant **`105570042132`** killed.

Baseline artifact **`10543506605`**, SHA-256 **`68a628ad45fa44a0fc94b248e69a2472b691d5e45a564e70cd735a7d7943a5db`**.

### G3.11 — real late tracking plus owner-level ordering

The integrated client proof demonstrates late START_TRACKING materializes current state directly. An initial mutation campaign exposed an oracle weakness: continuous fresh publication could hide transient stale rollback. The proof was hardened to freeze further server anatomy publication before stale injection, and the exact ordering owner was split into `S24AdversarialFrameOrderTests`.

Run **`35336440655`**:
- owner baseline **`105572251990`** success;
- real late-tracking client **`105572252178`** success;
- same-window ordering mutant **`105572742720`** compiled and was killed after removing frame-serial, authority-tick and joint-sample-tick fences.

Artifacts:
- client **`10543522907`**, SHA-256 **`6602a57e3985d4d8c252f932716704da3819a56507a1a5695618959664c787c0`**;
- owner baseline **`10543032562`**, SHA-256 **`6eb6acd6352d49f6caf71c1e0f89e9c63d01e3280ac6791936a35b3ff11ad2f5`**;
- owner mutant **`10542979062`**, SHA-256 **`3d9e53d9f64bc4b0c218ceb009b3e78c47bddba521cdfbb3933199acc250b8de`**.

The owner holdout is permanently registered in the ordinary GameTest suite.

### G3.12 — extracted root/endpoint ownership

Structural gate run **`35333447075`**:
- baseline **`105562783643`** success;
- duplicate endpoint owner mutant **`105562809519`** killed;
- duplicate root-history owner mutant **`105562809667`** killed.

This prevents `AnatomyMovement` from silently reacquiring persistent root/endpoint state now owned by `RootFrameLedger` and `AnatomyEndpointLedger`.

### Ordinary and zero-change

Receipt kernel tests that initially failed after purifying tracking observation were classified as TEST/EVIDENCE: they had relied on the old implicit-acquire getter as fixture setup. They were migrated to a GameTest-only, action-scoped authority seam; production remained pure.

Final ordinary run **`35336440635`**, job **`105572251864`**: **442/442 required GameTests passed**, `BUILD SUCCESSFUL`. Artifact **`10543418166`**, SHA-256 **`36ab2120935b3339753130f43a80e3771d785f4526b0a732e934c0d65eea5579`**.

Comparing final S24 production **`99969cc...`** to the adversarial closeout checkpoint **`8dcbb615...`** shows **zero later changes under `src/main` or `src/client`**; all subsequent changes are tests, CI, gates and documentation.

**Conclusion:** G3 tasks 9-12 are independently closed. Together with previously closed tasks 1-8, **G3 is fully closed**. The next gate is G4.

## G4 / S25 transport-reference baseline — adversarial RED 2026-09-18

G4 opens from a fully closed G3. The first gate is FR-084 authority for movement references/baselines backed by transports the server actually applied.

Current product already owns bounded server receipts in `AnatomyTransportReceipts`, but the C2S reference half is absent:

- `history(...)` explicitly describes itself as a snapshot for an “eventual metadata-only C2S reference validator”;
- the only movement C2S receiver is legacy `PlatformMovePayload`;
- `PlatformNetworking` exits from that receiver when `AnatomyApi.ownsSharedPhysics(body)`;
- `PlatformMovementReference.resolve(...)` returns the absolute movement and clears legacy pending reference under anatomy shared physics;
- `AnatomyPredictionBaselineProof` explicitly states that its N2 measurement has no anatomy C2S reference, rollback or replay.

Temporary presence gate commit `69e23855a437227f2f7cf4a090b72b90a37daaef`, workflow `s25-adversarial-reference-presence`:

- run **`35337131202`**, job **`105574448871`**: expected RED;
- exact gate result: `no non-legacy anatomy/collision C2S receiver is registered`.

The ordinary build of the same snapshot, run **`35337131192`**, job **`105574448591`**, passed **442/442 required GameTests** and ended `BUILD SUCCESSFUL`.

This is an **expected capability-absence RED**, not a regression. The temporary source presence gate is not acceptance evidence and must be replaced by behavioral holdouts once an anatomy reference surface exists.

Threat model and required happy/negative/mutation cases: `docs/sprints/S25-g4-reference-adversarial-model.md`.

## G4 / S25 reference authority baseline — 2026-09-18

G4.1 opens with an intentional capability RED: server-side `AnatomyTransportReceipts` exist and are bounded, but no non-legacy anatomy/collision C2S reference receiver is registered.

Presence evidence: workflow `s25-adversarial-reference-presence`, run **`35337131202`**, job **`105574448871`**, fails exactly with `no non-legacy anatomy/collision C2S receiver is registered`. Ordinary on that snapshot, run **`35337131192`**, job **`105574448591`**, passed **442/442 required GameTests**.

A separate structural authority gate remains green while capability is absent and will continue to constrain the first implementation. Run **`35383169968`**:

- baseline authority boundary job **`105723887331`**: success;
- legacy-borrow mutant **`105723934416`**: compiling mutant killed when collision code depends on `PlatformMovePayload`;
- rich-authority mutant **`105723934423`**: compiling mutant killed after registering the server-confirmed contact payload as serverbound authority.

Ordinary after the hardened gate, run **`35383169917`**, job **`105723885840`**, is green; artifact **`10562113085`**, SHA-256 **`156c1d83b7d1d2293af815b0e0c7d767ae34d0922e2d7037521e68833718c0d2`**.

**Interpretation:** G4.1 remains RED because the metadata-only C2S reference/validator is absent. The current evidence already rejects two invalid closure strategies: borrowing the legacy movement reference path or allowing client-uploaded geometry/contact/pose authority. Behavioral closure still requires exactly-once, TTL/saturation, lifecycle cross-product, controlled-body authority and no double-apply.

## G4 / S25 pre-implementation authority/store hardening — 2026-09-18

G4.1 remains intentionally RED because no anatomy/collision metadata-only C2S reference/validator exists yet. Before implementation, two independent foundations are now mutation-protected.

**Reference authority boundary.** Final run **`35384438639`**:

- baseline job **`105727903384`**: success;
- common/server legacy-borrow mutant **`105727968468`**: compiling mutant killed;
- client-side legacy-borrow mutant **`105727968484`**: compiling mutant killed;
- rich serverbound contact-authority mutant **`105727968513`**: compiling mutant killed.

The gate follows actual collision `serverboundPlay` payload registrations to their schemas, rather than rejecting unrelated S2C pose/contact types that happen to share a networking class. It also keeps `PlatformMovePayload` excluded on both server and client while anatomy owns shared physics.

Ordinary build on the same authority snapshot, run **`35384438584`**, job **`105727903091`**, is green; artifact **`10563561102`**, SHA-256 **`89cfb89533561e1dcdba5af812f2e1d2a4b540912eff88deee7b4c9cf435fe47`**.

**Receipt store authority.** Run **`35384233437`**:

- baseline **`105727247783`**: success;
- recipient-key mutant **`105727617910`**: compiling mutant killed;
- global-disconnect mutant **`105727617921`**: compiling mutant killed;
- body-key mutant **`105727617960`**: compiling mutant killed.

Artifact **`10563370910`**, SHA-256 **`dc523680cb24cd8036fc102ee56a5c6a4a68cd73f37586edc0f9d72410eced0e`**. The holdout also proves saturation is scoped to the exact recipient/body/tick authority key.

A receipt is historical provenance, not current movement authority: STOP_TRACKING may leave it until TTL while retiring the recipient tracking window. Any G4.1 validator must therefore check current lifecycle/tracking authority in addition to locating a receipt.

Controlled-vehicle acceptance must not collapse to “recipient has a receipt”: receipts are emitted to indirect ServerPlayer passengers, so a non-controlling passenger may legitimately own a server-issued boat receipt. The server must derive the allowed body/control relationship from the connection context.

**Interpretation:** the missing C2S path remains the sole S25 capability RED. When the first candidate lands, adversarial closure moves to behavioral exactly-once, TTL/saturation, lifecycle cross-product, controlled-body authority, fake-reference rejection and no-double-apply.


## G4 / S25 candidate implementer — metadata-only movement references — 2026-09-19

Rol de esta evidencia: **IMPLEMENTER**. Este bloque registra únicamente ejecuciones reales del candidato; no declara cierre adversarial de G4.1.

Candidato productivo: `b09c490a8405cf5042468e8ea1bd6d4538fad0cd`.

- `build` run **`35388973833`**, job **`105742610038`**: **success**.
- `s25-adversarial-reference-presence` run **`35388973889`**, job **`105742611717`**: **success**. La superficie C2S no-legacy existe.
- `s25-adversarial-reference-authority` run **`35388973830`**: **success** completo:
  - boundary `105742610290` verde;
  - rich-authority mutant `105742653710` muerto;
  - legacy-borrow mutant `105742653716` muerto;
  - client-legacy-borrow mutant `105742653816` muerto.
- `s25-adversarial-receipt-isolation` run **`35388973790`**: **success** completo:
  - baseline `105742609899` verde;
  - recipient-key mutant `105743014546` muerto;
  - disconnect-global mutant `105743014568` muerto;
  - body-key mutant `105743014743` muerto.
- `s24-adversarial-tracking-ledger` run **`35388973836`**: **success**.
- `s16-canonical-catalog-proof` run **`35388973771`**: **success**.
- `s22-adversarial-coverage-residuals` run **`35388973929`**: **success**.

Regresión de desarrollo implementer, commit `35224ed5261969854feb58379f2fcd34927d61c9`:

- `s25-implementer-reference-proof` run **`35444126698`**, job **`105900018851`**: **success**;
- artifact `S25-implementer-reference`, id **`10585450603`**;
- `build` del mismo snapshot run **`35444126655`**, job **`105900018815`**: **success**.

La regresión implementer comprueba que metadata de support/frame incorrecta no consume el receipt, el endpoint server-issued exacto lo resuelve, el receipt histórico permanece inmutable tras claim y el replay exacto es rechazado.

**Límite de evidencia:** G4.1 continúa **ABIERTO**. Falta la campaña adversarial conductual independiente A–G y los ocho mutation-kills definidos en `docs/sprints/S25-g4-reference-adversarial-model.md`. No se infiere todavía estabilidad 0/100/200 ms, drift/reconciliation ni cierre de G4.2+.

## G4 / S25 dedicated reference emission — adversarial RED 2026-09-19

The owner/kernel reference path is green and mutation-adequate. Run **`35446958412`** passes the baseline plus nine compiling directed mutants: exactly-once, tracking generation, lifecycle identity, expired receipt, saturated tick, non-controller, double-apply, fabricated-hit and per-tick rate budget. Baseline artifact **`10586355183`**, SHA-256 **`720be464edab24d7c6063996283f92c36c4d0cb7288aef35e4ad6723f8f4bd60`**.

The dedicated latency lane initially failed before gameplay because the exported original anatomy catalog was not forwarded to the isolated proof. Workflow-only fix **`d8ffe899fbbf4d3ee12f71190be8c36de440eb4c`** passes `-PscalebrewsAnatomyCatalog=<export>` into the second `runClientGameTest`, allowing the proof to reach real dedicated movement.

Run **`35447097292`**, job **`105907798139`**, exposes a product integration RED at player RTT=0. The outgoing movement mixin is reached and the client reports `ready=true`, local authority true, reference capability sendable, real confirmed support, and a local transport sequence advancing through 26/76/126/176. Nevertheless the client reference map remains unstaged and no `scalebrews:anatomy_move_reference_v1` packet appears before any of roughly 201 real player movement packets. The proof fails with `S25 player RTT 0 emitted no anatomy_move_reference_v1 packet despite real local carry`.

Causal localization: client carry already advances `TransportLedger` on the real `Entity.move -> PlatformEntityMixin -> PlatformPhysics.carry -> AnatomyMovement.carry` path. The current cursor capture runs later in `END_CLIENT_TICK` and only captures when a second carry changes the transport sequence during that callback. By then `before.sequence == after.sequence`, so the cursor is never staged.

**Classification:** PRODUCT RED in client carry-to-reference capture timing. Receiver authority, claim/resolve behavior, Netty capability, READY/local authority and the dedicated fixture are not the blocker. G4.1 remains open. Full handoff: `docs/sprints/S25-adversarial-latency-capture-red.md`.

## G4 / S25 receipt-frame identity — adversarial RED 2026-09-19

The previous client-capture blocker is repaired: real dedicated traffic now contains `scalebrews:anatomy_move_reference_v1` immediately before vanilla player movement packets, with READY/local authority/capability guards satisfied and no vanilla corrections.

A stricter acceptance oracle then required evidence that the server actually consumed at least one live receipt per phase. Run **`35448151599`**, job **`105910578723`**, artifact **`10585761418`**, SHA-256 **`89ff7254b5883e205e9006c78b686eb0b086e084a333b8f41ae22e3c7ad1c379`**, fails causally at player RTT=0 with **`consumedReferences=0`** despite real reference traffic.

Test-only wire tracing and server receipt metadata show a disjoint reference key:

- client C2S `supportFrameSerial`: **8, 10, 12, ... 400, 402, 404** — 199 references, all even;
- server receipt map in the live bounded history: **167→335, 168→337, ... 199→399, 200→401, 201→403** — all support-frame serials odd.

Support UUID matches; the exact support-frame serial never does. `AnatomyTransportReceipts.claim(...)` therefore cannot find a receipt and consumes nothing.

The mismatch is causal rather than networking noise. Server receipts bind to the endpoint of the material interval that actually produced carry. Client prediction can incorporate a later published endpoint for the same support/root state whose `frameSerial` advanced due another causal publication (for example a subsequent pose/joint sample) without another server carry. Exact endpoint serial is therefore too fine an identity for the transport incorporated by the client.

Owner/kernel evidence remains green with baseline plus **10 compiling mutation kills**, including rate budget and pending-reference TTL. Ordinary build on the diagnostic harness snapshot is also green.

**Classification:** PRODUCT RED in reference↔receipt causal identity. G4.1 remains open. Full handoff: `docs/sprints/S25-adversarial-receipt-frame-identity-red.md`.

## G4 / S25 owner-kernel final adversarial closure — 2026-09-19

The G4.1 owner/kernel path is now independently mutation-adequate, while the dedicated reference↔receipt identity integration remains RED.

Final owner run **`35451920466`**, baseline job **`105920453853`**: success. Fourteen directed mutants compiled and were killed:

- expired receipt **`105920630826`**;
- combined lifecycle identity **`105920630835`**;
- non-controller **`105920630836`**;
- saturated tick **`105920630837`**;
- tracking generation **`105920630848`**;
- rate budget **`105920630851`**;
- fabricated hit **`105920630853`**;
- exactly-once **`105920630869`**;
- pending TTL **`105920630890`**;
- adjacent-frame/fuzzy match **`105920630894`**;
- network-id fence **`105920630900`**;
- double-apply **`105920630901`**;
- dimension fence **`105920630904`**;
- epoch fence **`105920631014`**.

The split epoch/dimension/network-id mutants first compiled and survived run `35451440249`, proving an oracle gap rather than a production regression. `S25AdversarialReferenceLifecycleFenceTests` then isolated each historical receipt axis without modifying production; the final run kills all three. The adjacent-frame mutant weakens exact receipt lookup to accept `±1` support-frame serials and is also killed.

Baseline artifact **`10587071981`**, SHA-256 **`02cb9ef7807420d9c0a7fbe21eb0e8aade2e9a2619fcae6d401ef6cad60646f2`**. Adjacent-frame mutant artifact **`10587346709`**, SHA-256 **`544e0626aad36be89dbe64ba8fd2168ce416239cec256641830f4d78d394c0ed`**.

Ordinary run **`35451920485`**, job **`105920454100`**: **446/446 required GameTests passed**, `BUILD SUCCESSFUL`; artifact **`10586857060`**, SHA-256 **`66dfe0bc240c04c4e516e1466e864d57f2a166d957b802609bd7a79bb1704a39`**.

This does **not** close G4.1. Dedicated run `35448151599` remains the current product RED: player RTT=0 emits 199 ordered anatomy references, receives no vanilla corrections, but consumes zero receipts because client `supportFrameSerial` values and server receipt frame serials have no intersection. The current evidence explicitly rejects fuzzy/nearest-frame repair. A root-frame sequence alone is also insufficient because existing joint-transport coverage demonstrates distinct transports under one root sequence.

**Current interpretation:** owner/kernel closed; product integration remains RED at the transport identity boundary. G4.1 stays open and G4.2 must not start.

## G4 / S25 owner identity hardening — 2026-09-19

The owner/kernel evidence was extended after the first 14-mutant closure to constrain the dedicated reference-identity repair itself.

Run **`35452742190`**, baseline job **`105922624414`**, is green with the full behavior/lifecycle suite. Fifteen directed mutants compile and are killed. The new fifteenth mutant is **`ambiguous-exact-match`**, job **`105922827839`**: it removes only the fail-closed rule for multiple receipts sharing one exact reference identity. The holdout proves that such ambiguity must reject without consuming either receipt.

Artifact baseline **`10587775660`**, SHA-256 **`3ebc64a0b8cdb6c19550d9638770719fd9bbccea744a392c951696921fa092ba`**. Ambiguous-identity mutant artifact **`10587292882`**, SHA-256 **`b668ee8891b6a818c53c4962953b29a19d878e60cd733ab2176bafd24ed6ba76`**.

The same baseline also includes an explicit coarse-identity holdout: two distinct server-issued receipts can share support UUID, game tick, contact sequence and root-frame sequence while retaining different transport sequences and remaining independently claimable. Those clocks therefore cannot replace `supportFrameSerial` as a unique key by themselves.

Ordinary run **`35452742176`**, job **`105922622233`**, passed **446/446 required GameTests**; artifact **`10586933280`**, SHA-256 **`869ade328e5d491fd393bbfe484ee5b813b4afb944b12a392549af5b4392e53d`**.

Reference-authority schema hardening also rejects client-local transport authority. Run **`35452660678`** is fully green: baseline **`105922405151`**, common/server legacy borrow **`105922422964`**, rich physical-authority serverbound schema **`105922422974`**, client legacy borrow **`105922423031`**, and client-local sequence schema **`105922423094`**. The final mutant adds and serializes a `localTransportSequence` field to the registered C2S reference schema; it compiles and is killed.

These results do **not** repair or close the dedicated integration RED. Current product evidence still shows reference packets ordered correctly before vanilla movement with no vanilla corrections, but `consumedReferences=0` because the client names a later endpoint serial while the server receipt names the material-interval endpoint that produced carry. Fuzzy/nearest matching, ambiguous selection, client-local sequence authority and coarse root/contact/tick identities are all explicitly excluded by the adversarial evidence.

If the wire identity semantics change from `supportFrameSerial`, the `anatomy_move_reference_v1` meaning must not be silently reinterpreted; protocol semantics must remain explicitly versioned/compatible.

**Current interpretation:** G4.1 remains PRODUCT RED only at reference↔receipt transport identity. Owner/kernel and schema authority are closed for the known threat model. G4.2 remains blocked.


## G4 / S25 V2 server-issued receipt identity — implementer repair + adversarial recertification — 2026-09-25

Rol productivo de la reparación: **IMPLEMENTER**. G4.1 permanece abierto por el dedicated boat setup descrito al final; no se declara cierre global.

### Reparación de identidad

El RED par/impar de `supportFrameSerial` no se reparó con fuzzy matching. Se introdujo un protocolo explícitamente versionado:

- S2C `AnatomyTransportReceiptPayload` (`scalebrews:anatomy_transport_receipt_v1`) publica metadata de un receipt que el servidor **ya aplicó**: epoch/revision/dimension, body id+UUID, tracking generation, support UUID, support frame serial, `receiptSequence` server-issued y tick;
- C2S `AnatomyMoveReferenceV2Payload` (`scalebrews:anatomy_move_reference_v2`) devuelve sólo `{vehicle, receiptSequence}`;
- el cliente mantiene `localTransportSequence` únicamente como freshness guard local; no cruza wire;
- el servidor deriva player/controlled vehicle desde la conexión y `AnatomyTransportReceipts.claim(recipient, body, receiptSequence)` exige coincidencia exacta del receipt server-side más todos los fences de lifecycle;
- v1 permanece registrado temporalmente como compatibilidad explícita; el cliente productivo sólo emite v2;
- publicación S2C es best-effort: un receipt autoritativo se almacena aunque el `ServerPlayer` de fixture no tenga listener de red.

El cliente sólo prepara un token recibido cuando su carry local ha incorporado un endpoint del mismo support con `supportFrameSerial >= token.supportFrameSerial`. Esa comparación decide **incorporación local**, no autoridad: el claim servidor continúa siendo igualdad exacta por `receiptSequence`.

### Evidencia ejecutada

- Authority/schema sobre v2: run **`36129090393`**, completamente verde:
  - boundary `108051936098`;
  - legacy borrow `108052771564`;
  - client-local-sequence schema mutant `108052771650`;
  - rich authority mutant `108052771673`;
  - client legacy borrow `108052771849`.
- Receipt publication sin network listener: run **`36129522915`**, job `108053315959`: **success**; artifact `10861341561`.
- Ordinary sobre oracle v2: run **`36129628428`**, job **`108053654651`**: **449/449 required GameTests**, `BUILD SUCCESSFUL`; artifact `10861276661`.
- Owner-v2 final: run **`36130245780`**: **success** completo:
  - baseline `108055602568`;
  - tracking mutant `108056028215` muerto;
  - replay mutant `108056028216` muerto;
  - fabricated-sequence mutant `108056028268` muerto.
  - artifact baseline `10861697283`.

El primer fabricated-sequence mutant sobrevivió porque el fake token se probaba después de consumir el único receipt; el exactly-once fence ocultaba la corrupción. El adversario emitió un receipt fresco no consumido antes del fake token en commit `15141734e1da4d4ae78fc2ad48d18ef7ce7088e9`; la recertificación anterior mata el mutante sin cambio productivo.

### Dedicated 0/100/200 ms

El protocolo v2 elimina el blocker causal original: las references wire se observan inmediatamente antes del movement vanilla y el servidor consume receipts reales.

Run `36130077183` con trace ampliada a 2048 conserva evidencia completa:

- player RTT=0: `consumedReferences=35`, corrections `0/0`, dropped `0`;
- player RTT=100: `consumedReferences=24`, corrections `0/0`, dropped `0`;
- player RTT=200: `consumedReferences=22`, corrections `0/0`, dropped `0`;
- controlled boat RTT=0: `consumedReferences=35`, dropped `0`, pero aparecen 2 `ClientboundMoveVehiclePacket` al comienzo de la fase.

Las dos correcciones boat RTT=0 ocurren **antes de la primera reference v2**. El trace muestra primero una corrección de setup, después un `ServerboundMoveVehiclePacket` transitorio con target `(0,-0.12,0)`, otra corrección, y sólo entonces comienza la cadena estable `anatomy_move_reference_v2 -> ServerboundMoveVehiclePacket`. No se observan correcciones posteriores dentro de la secuencia estable capturada.

**Clasificación vigente:** el RED productivo de identity reference↔receipt queda reparado y adversarialmente recertificado a nivel owner. El dedicated restante está contaminado por el handshake de montaje previo a la primera reference; no autoriza modificar reconciliación productiva todavía. El adversario debe cerrar una barrera de setup/movement-baseline de vehículo y repetir boat 0/100/200 ms. G4.1 y G4 siguen abiertos; G4.2 permanece bloqueado.

### S25 dedicated final — semantic boat-baseline barrier — 2026-09-27

Rol de esta anotación: **IMPLEMENTER handoff**. No declara por sí sola cierre adversarial de G4.1.

El RED de las dos correcciones iniciales del controlled boat quedó clasificado como tráfico del handshake vanilla de montaje, previo a la fase S25. El harness adversarial incorporó `VehicleMoveAudit` y sólo abre la medición cuando el listener servidor ha aceptado al menos dos `ServerboundMoveVehiclePacket` del boat exacto con `lastVehicle` correcto y `vehicleFirst/LastGood` convergidos respecto a la posición real.

Dedicated final `s25-adversarial-reference-latency` run **`36318320536`**, job **`108617148797`**: **success**. Evidencia observada:

- player RTT 0/100/200 ms: referencias v2 reales, receipts consumidos, posiciones cliente/servidor coincidentes y `0/0` correcciones player/vehicle;
- controlled boat RTT 0/100/200 ms: referencias v2 reales, receipts consumidos, posiciones cliente/servidor coincidentes y `0/0` correcciones player/vehicle;
- ordering wire preservado: `anatomy_move_reference_v2` inmediatamente antes de `ServerboundMovePlayerPacket`/`ServerboundMoveVehiclePacket` correspondiente;
- trace sin drops en las fases medidas;
- marker final: `S25_REFERENCE_LATENCY PASS player+boat RTT 0/100/200ms anatomy reference ordering and zero vanilla corrections`.

Artifact **`10931765875`** (`S25-reference-latency`). Build del mismo HEAD: run **`36318320523`**, job **`108617148838`** — **success**.

El fallo vecino `assertRemoteObserverNeverCarries` se retiró únicamente del modo `S25_REFERENCE_ACCEPTANCE`: corresponde a G4.3 (observer path), no al scope G4.1 de receipt/reference authority. La aserción permanece en el proof general.

**Estado:** no queda RED técnico conocido dentro de G4.1. El control vuelve al ADVERSARY para la pasada final independiente y el cierre formal del sprint antes de abrir G4.2.

## G4 / S25 final adversarial closure — 2026-09-27

S25/G4.1 is independently closed.

Final product for the receipt-token identity path: **`2aeefa00278a58315a338967901c84e8b7fa08bd`**. Comparison from that snapshot through final adversarial checkpoint `c30b411ec8f2c6ad75ea70a11a7c5db664a349f2` contains **zero later changes under `src/main` or `src/client`**.

The canonical C2S path is V2: the server publishes `AnatomyTransportReceiptPayload` and the client returns only `AnatomyMoveReferenceV2Payload(vehicle, receiptSequence)`. `localTransportSequence` remains a client-local freshness guard and never becomes server authority.

Owner-V2 run **`36130245780`**:

- baseline **`108055602568`** success;
- tracking-generation mutant **`108056028215`** killed;
- replay/exactly-once mutant **`108056028216`** killed;
- fabricated-sequence mutant **`108056028268`** killed.

Artifact **`10861697283`**, SHA-256 **`ffdd30fcb0897462cb60a88faaa9bfaf4ea2c37cac605a57e8047258b3637661`**.

Authority/schema run **`36129090393`** remains green including the client-local-sequence authority mutant. Receipt publication run **`36129522915`**, job **`108053315959`**, is green; artifact **`10861341561`**, SHA-256 **`15f1dd5899eb9197761308f489124a73cba6b271476f5ee0ecb4a7d795df63ba`**.

Wire semantics are frozen by run **`36318246051`**: V1, V2 and S2C receipt-token schemas all pass baseline and three compiling silent-schema mutants are killed.

The final dedicated lane uses a test-only `VehicleMoveAudit` boundary to exclude only the vanilla boat mount handshake. Measurement begins after at least two accepted packets from the exact boat with `lastVehicle`, `vehicleFirstGood` and `vehicleLastGood` stabilized; an inbound/outbound FIFO fence then drains all setup traffic before metrics reset.

Run **`36318320536`**, job **`108617148797`**: **success**. Final marker:

```text
S25_REFERENCE_LATENCY PASS player+boat RTT 0/100/200ms anatomy reference ordering and zero vanilla corrections
```

Consumed references / vanilla corrections:

- player 0 ms: **35 / 0**;
- player 100 ms: **25 / 0**;
- player 200 ms: **22 / 0**;
- controlled boat 0 ms: **35 / 0**;
- controlled boat 100 ms: **16 / 0**;
- controlled boat 200 ms: **24 / 0**.

All six phases retain confirmed material support, matching client/server final positions, ordered V2 reference → vanilla movement traffic and no trace drops. Artifact **`10931765875`**, SHA-256 **`19c5d69f752e44c49a86723037651b8d460679dccdb3706cca991bd58ee2eac3`**.

Ordinary on the same final acceptance tree, run **`36318320523`**, job **`108617148838`**, passed **449/449 required GameTests** and `BUILD SUCCESSFUL`; artifact **`10931995176`**, SHA-256 **`2d3e46f2c32023e1431471276f5076a53abfb0d7ca97ac15b720e88a61f2bc15`**.

The later remote-observer assertion failure observed while developing the boat setup boundary is explicitly **not** part of the G4.1 verdict. It conflates remote entity existence, presentation-contact availability and physical carry state and therefore requires isolated G4.2/G4.3 evidence before product classification.

**Conclusion:** G4 task 1 is closed. The next canonical gate is **G4.2: prediction only for the local player / actually controlled vehicle**.

## G4 / S25 post-close receipt-token lifecycle recertification — 2026-09-27

S25 reopened once after its first formal closure because the newly introduced bounded client `AnatomyReceiptTokenInbox` created a distinct lifecycle requirement.

Red-before-green:

- holdout `S25AdversarialReceiptTokenTeleportTests`;
- run **`36319103378`**, job **`108619353709`**: failure after a same-dimension player teleport;
- the production transport generation advanced, but an old client token `receiptSequence=1` survived and caused the fresh post-teleport server token with restarted sequence 1 to be rejected;
- artifact **`10932225690`**, SHA-256 **`3e244e982b1380e86405bad264921993da4a83970236d0d2088cd2bbb161e601`**.

An intermediate repair that only published lifecycle invalidation from `AnatomyMovement` remained red until client networking consumed that signal. Final product line:

- `263071814...`: publish client transport lifecycle invalidation;
- `25803838...`: discard staged references + receipt tokens for the invalidated body;
- `78e8ec6a943d28c5dbfd863db19ad001289c977f`: apply the same barrier to non-`LivingEntity` bodies such as boats.

The unchanged player expectation passed on run **`36319203434`**, job **`108619642199`**, artifact **`10931767348`**, SHA-256 **`875205bdea004c2e76c53e84f7bfefe8f736fb829dfb9421568fbd4e0c4655b2`**.

Final player + non-Living boat lifecycle run **`36319615155`**:

- baseline **`108620791027`** success;
- missing client lifecycle-observer mutant **`108621043309`** compiled and was killed;
- LivingEntity-only lifecycle mutant **`108621043326`** compiled and was killed.

Artifact **`10932565150`**, SHA-256 **`667ec16cf155ce34837e9658d0682816cdbff20161c99bca06268716230670ba`**.

The new inbox owner is independently mutation-protected by run **`36319228108`**:

- baseline **`108619706302`**;
- tracking-generation mutant **`108620469701`**;
- MAX_BODIES mutant **`108620469745`**;
- TTL mutant **`108620469747`**;

all success as baseline/mutation harnesses. Artifact **`10932290857`**, SHA-256 **`604a08a766169b61e6f39dff7a9830ff930319d515f24cef60afd0d3048f47d3`**.

Dedicated reference latency remains green after connecting the lifecycle barrier: run **`36319203501`**, job **`108619642390`**, artifact **`10932006286`**, SHA-256 **`4dd79b9baa4079c6644945426579d8a540fad283f119a65b2e34867e91ebcf0f`**.

Ordinary final run **`36319615112`**, job **`108620790741`** passed **451/451 required GameTests** and `BUILD SUCCESSFUL`; artifact **`10931537412`**, SHA-256 **`78484b2d36e888f61dd53aa39a8df259b601e8d68990cdc9a4bb80452926d491`**.

Final zero-change review from product `78e8ec6a...` through adversarial checkpoint `2fb7d763...` finds no later changes under `src/main` or `src/client`.

**Conclusion:** the localized S25 reopening is closed again. G4.1 is closed; G4.2 is the next canonical gate.


## G4 / S26 local prediction authority — final closure — 2026-09-29

G4.2 closes FR-079: full client-side anatomy prediction belongs to exactly one live local root actor, while remote observers/supports remain read-only.

Product fixes:

- `df3d9875842085169e7c879a958b1188414713aa` — introduces `AnatomyMovement.predictsBody(...)` so a mounted local player no longer shares full prediction with its controlled root vehicle.
- `2f73966390c020f77c904e2601cb5a8dfba06dbf` — derives a single `predictionOwner` from current control and discards staged references/receipt tokens across mount, dismount or replacement handoff.
- `ad97efb3927b2289ea60fcf5bb2037e5ee1fc7f5` — routes the `PlatformPhysics` anatomy bridge through root prediction authority rather than broad local-replica authority.

Final adversarial recertification: workflow `s26-adversarial-prediction-authority`, run **`36400355085`**:

- control handoff baseline **`108856467859`** — success;
- remote observer baseline **`108856468387`** — success;
- simulate-all mutant **`108856986314`** — killed;
- tick-loop guard mutant **`108856986340`** — killed;
- reference local-authority mutant **`108856986378`** — killed;
- bridge root-owner mutant **`108856986431`** — killed;
- stale metadata handoff mutant **`108857082946`** — killed;
- duplicate root actor mutant **`108857082947`** — killed;
- stale vehicle authority mutant **`108857082996`** — killed.

Artifacts: `S26-control-handoff` id **`10960180780`** and `S26-prediction-authority` id **`10959951365`**.

Build run **`36400355089`** is success. The positive dedicated player + controlled-boat matrix was rerun after bridge changes in `s25-adversarial-reference-latency` run **`36400806938`**, also success.

Zero-change criterion: after the last product fix `ad97efb...`, subsequent S26 changes were test/CI-only before the final green recertification.

**Conclusion:** G4.2 is closed. G4.3 (observer path without local carry) is the next open gate; this evidence does not claim observer presentation completeness, reconciliation/drift closure, or residual camera/visual closure.

## G4 / S27 observer path — final adversarial closure 2026-09-29

S27/G4.3 closes the remote-observer presentation path without introducing any new production state owner.

Final workflow `s27-adversarial-observer-path`, run **`36558367708`**:

- baseline job **`109372922579`**: success;
- local-only presentation mutant **`109373610515`**: compiling mutant killed;
- contact-publication suppression mutant **`109373610565`**: compiling mutant killed.

Artifact **`11028732300`**, SHA-256 **`af7b0bd38ebf03f67aaca3f530e3c53435d8cb7b9488d29e991aae85f254e898`**.

The real-client proof accumulates server transports before tracking, late-tracks the remote body/support, reconstructs current `presentationFrame + presentationContact`, and simultaneously asserts `AnatomyMovement.simulates(remote)==false`, `predictsBody(remote)==false`, and no client `TransportLedger`. After another confirmed server transport, vanilla position and presentation advance without local carry. STOP_TRACKING retires the body generation; further untracked server transport is accumulated; retrack creates a fresh generation and materializes only current confirmed state, again with no local transport replay.

Ordinary build on the same snapshot, run **`36558367581`**, job **`109372922475`**, is green; artifact **`11029555862`**, SHA-256 **`56674825ebb18db0cc24e3e9be04e322355447c07061e282c9ab76520a7aa9ed`**.

Compare `c39014a5374f6cb2348ee7c726ebb8c239dc613c → 7652be3b75ab264eb374f2eb428fb0c3544ee475` contains no `src/main` or `src/client` changes. S27 therefore closes under the no-change-first hypothesis.

**Conclusion:** G4 task 3 is closed. The next open gate is G4.4: reconciliation without double-apply or drift.


## G4 / S27 observer path — post-closure friction reopening — 2026-10-02

The historical S27 late-observer closure (`36558367708`) remains valid for bootstrap/retrack and no-local-transport evidence, but it no longer closes the whole observer-physics boundary.

Post-closure IMPLEMENTER review found that `PlatformLivingFrictionMixin` always calls `Platforms.friction(...)` from `LivingEntity.travelInAir`, while the anatomy branch of `Platforms.friction(...)` could consume a server-confirmed remote contact without checking `AnatomyMovement.predictsBody(...)`. That permits a non-owner replica to receive anatomy material friction during local movement integration even though carry/collide/transport remain fenced.

Product fix:

- `f661772e85a1ba916ad7a483123871f6dae70c7f` — remote anatomy replicas keep the caller-provided vanilla friction unless they are the current prediction body.
- `248a50ebae43d94aa0182f01fb4c96432a29096e` — real-client observer regression requires a remote body with confirmed anatomy contact to preserve vanilla friction 0.91 despite canonical policy friction 0.6.

**Status:** S27/G4.3 is REOPENED pending build + observer regression, an adversarial friction-owner mutant, repetition of the S27 late-observer baseline, and a subsequent zero-change pass. G4.4 remains blocked.

## G4 / S27 observer friction repair — implementer recertification — 2026-10-02

Product fix `f661772e85a1ba916ad7a483123871f6dae70c7f` keeps anatomy observer replicas on caller-provided vanilla friction unless they are the current `predictsBody(...)` owner. Regression `248a50ebae43d94aa0182f01fb4c96432a29096e` exercises a remote real-client body with confirmed anatomy contact and canonical friction 0.6 while requiring `Platforms.friction(remote, 0.91) == 0.91`.

Executed evidence:

- build run **`36986056890`** — success;
- `s26-adversarial-prediction-authority` run **`36986056911`** — success, including remote observer/control handoff baselines and all previous ownership mutants;
- `s27-implementer-observer-recertification` run **`36986505955`**, job **`110772649688`** — success;
- artifact **`11218260030`** (`S27-implementer-observer-recertification`).

The S27 late-observer proof therefore remains green after the friction fix: current presentation/contact bootstraps and retracks correctly and no local `TransportLedger` is created.

**Limit:** S27/G4.3 remains reopened until the ADVERSARY mutation-gates the new friction owner check and performs a final zero-change pass. This evidence is implementer recertification, not adversarial closure.

## G4 / S27 observer path — friction ownership final recertification — 2026-10-02

After the post-closure friction leak repair, S27 was adversarially recertified on workflow `s27-adversarial-observer-path`, run **`36988858727`**:

- late observer baseline **`110780085421`** — success;
- friction owner baseline **`110780085735`** — success;
- contact publication mutant **`110780581376`** — killed;
- presentation local-only mutant **`110780581563`** — killed;
- friction owner mutant **`110780612218`** — killed.

Artifact `S27-late-observer` id **`11218582566`**. Build run **`36988858759`** is success.

The friction-owner mutant specifically weakens/removes the rule that remote anatomy replicas keep vanilla friction unless they are `predictsBody(...)`; the real-client observer oracle kills that corruption while late tracking/retrack presentation remains green.

**Conclusion:** S27/G4.3 is closed again. The observer path may expose confirmed read-only support/presentation state but cannot gain local carry, transport/reference state, or anatomy friction integration. G4.4/S28 reconciliation is the next open gate.

## G4 / S28 reconciliation — final adversarial closure 2026-10-02

S28/G4.4 closes reconciliation without any production change.

Final workflow `s28-adversarial-reconciliation`, run **`36987926148`**:

- dedicated reconciliation baseline **`110777096130`** — success;
- exact vanilla baseline owner **`110777096206`** — success;
- reference double-apply mutant **`110778159564`** — compiling mutant killed;
- vanilla baseline double mutant **`110778159616`** — compiling mutant killed.

The owner-level oracle reads all four vanilla acceptance baselines directly: player `firstGood/lastGood` and vehicle `vehicleFirstGood/vehicleLastGood`. One confirmed carry advances each exactly once by the same transport delta. The dedicated mutation then proves that adding the current transport delta again on top of `TransportLedger.since(receiptSequence)` is observable and rejected under real player + controlled-boat 0/100/200 ms traffic.

Artifacts:

- reconciliation baseline **`11218208110`**, SHA-256 **`ef4715b7512b6d8e5e9d264bc399e2582e335bad62852c20ad2b37ea65b1d8f8`**;
- baseline owner **`11218297367`**, SHA-256 **`773e12f1b30b8df92bce6dc2941b8415c1f150f50e77463d9b0839bca24e4c8f`**.

Ordinary run **`36987926156`**, job **`110777096416`**, is green; artifact **`11217304592`**, SHA-256 **`a35fc8cb56598745b5e18f724cb867313d6d52314cff3be09f15152381c2458e`**.

Compare `94a0a9b3834ae4a65c4685729e08c9bf0501fa29 → 95c1c5064b5f5fab82276f5f568e92046bd6a362` contains no `src/main` or `src/client` changes.

**Conclusion:** G4 task 4 is closed. The next open gate is G4.5: presentation `CURRENT_ENDPOINT` to a certified interval only where Q2 requires interval semantics.

## G4 / S29 certified presentation interval — capability RED 2026-10-02

G4.5 opens after S28/G4.4 closure. The current client presentation path exposes only exact `CURRENT_ENDPOINT` frames. `CERTIFIED_INTERVAL` is reserved in the enum but has no product construction path, and no server-issued material-interval certificate crosses the network.

Temporary presence gate commit `475990739c51553788208f1e6e61e7d011ebe983`, workflow `s29-adversarial-certified-interval-presence`:

- run **`37006651350`**, job **`110836393829`** — expected RED;
- exact diagnostics:
  - no product path constructs `PresentationKind.CERTIFIED_INTERVAL`;
  - `PresentationFrame` rejects every non-`CURRENT_ENDPOINT` frame;
  - no server-issued material-interval payload/schema is present.

Ordinary on the same snapshot, run **`37006651353`**, job **`110836393729`**, is green; artifact **`11225893009`**, SHA-256 **`9720cf460e5da548694425c395a45a662a98447ed000b198b3623832da2a8725`**.

The core server already owns `GeometryProvider.MotionIntervalHandle(identity, materialSerial, before, after)`, but client `AnatomyFrameHistory` retains only the current endpoint. Two ordered pose frames are therefore not sufficient evidence of a certified material interval. S29 requires an explicit server-issued interval identity plus bounded client materialization/lifecycle handling; it must not infer Q2 from frame adjacency or renderer/local animation.

**Classification:** expected capability-absence RED, not a regression. G4 task 5 remains open pending an implementer candidate and subsequent behavioral/mutation closure.

## G4 / S29 repair checkpoint — IMPLEMENTER — 2026-10-02

Authorized branch: `chatgpt-editing`, pulled fast-forward to
`a54031b9cd2bd68bf63823860099665882e46a67`. The tested checkpoint consists of that
baseline plus the local production patch `../s29-product.patch` (relative to the
checkout, outside the repository); SHA-256
`a8a5450803a0b0081a9070013ceabab7ac3e5dfeedb295b2d336853648b75f41`.
No `main` gameplay code was reverted or merged as part of this checkpoint.

Executed final checks:

| Check | Actual result | Log SHA-256 |
| --- | --- | --- |
| `gradlew.bat build --offline --console=plain` | BUILD SUCCESSFUL; **458/458 required server GameTests** | `d5fec586ba90113efb1c3d212e6b9590bf4561dd123e8faf5c107fbce92c9983` |
| Real client, five affected proof classes | BUILD SUCCESSFUL, 47s; all five completed | `5226ed85ef4e23ac5efdf86ee464eea783bb53bebbe930864de1d4588c4de724` |
| Adversarial first-historical + repeated-historical work lane | BUILD SUCCESSFUL; **3/3 including sentinel**; repeated joint evaluations after warm-up = **0** | `dea332805e101e0c5cc4d214e58a11a4acc9559960b8bd1db0e5e609eba0307c` |
| Inbox campaign + eight scratch mutants | baseline PASS; expiry/bound/global-query/binding/tracking/epoch/dimension/nearest mutants compiled and were killed | `36406d51d165701d9ee882be1a4e3de88742b5e5e84fa42dc400cc9374de7ad9` |

Logs are retained beside the checkout at `../s29-checkpoint-build.log`,
`../s29-checkpoint-client.log`, `../s29-presentation-work-shared-green.log` and
`../s29-checkpoint-inbox.log`. Environment: Minecraft 26.2, Fabric Loader 0.19.5,
Fabric API 0.159.0+26.2, Loom 1.17.20, Microsoft OpenJDK 25.0.3, Windows local.

The real-client battery was selected only in generated test metadata by
`../s29-client.init.gradle`; it contains `AnatomyPresentationFrameTests`,
`S29AdversarialLiveEndpointProof`, `S29AdversarialUnavailableReplayProof`,
`S29AdversarialDerivedPresentationProof` and `S29AdversarialCertificateIdentityProof`.
Command: `gradlew.bat runClientGameTest --offline --console=plain --init-script ../s29-client.init.gradle`.
The reusable isolated selector for individual proofs is
`tools/s29_adversarial.init.gradle`; complete reproduction and independent
findings are in `sprints/S29-adversarial-recertification.md`.

Observed final client outcomes: both genuine same-tick root intervals materialize;
pre-unavailable certificate replay remains non-authoritative; a genuinely carried
child support receives/materializes its derived interval; certificate placement
before/between/after endpoints converges; fixed fractions 0/.25/.5/.75/1 match the
root trajectory; seven mismatched identity axes fail closed. No certificate still
means CURRENT_ENDPOINT rather than inferred interval authority.

The implementer and adversary completed separate final zero-change passes for
this **repair batch**. **G4.5 remains open.** This is not full frontend mutation,
dedicated latency/reconnect, contact/camera, visual/animated alignment, allocation/GC,
soak, NFR-014 benchmark or full-pack acceptance evidence. No release, push or
installation was performed.

## G4 / S29 joint-only integration — IMPLEMENTER — 2026-10-02

On the same production checkpoint above, executed:

```powershell
.\gradlew.bat runClientGameTest --offline --console=plain -I tools/s29_adversarial.init.gradle `
  '-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S29ImplementerJointPresentationProof' `
  '-PscalebrewsBuildDir=C:/Users/Usuario/Documents/Codex/2026-10-02/vas-x20/work/s29-implementer-joint-build'
```

BUILD SUCCESSFUL in **3m53s**, exit 0 and clean world teardown. A live server cow
head-pitch update generated material serial **2**. Client materialization at fixed
fractions 0/1 changed original head geometry, retained equal authoritative root
DTOs and advanced the joint-sample clock. No synthetic certificate send or renderer
animation served as authority. This development integration proof supplements the
independent evidence; it does not close the remaining G4.5 matrix. No production
changes were needed. Environment is unchanged from the preceding checkpoint.

Log: `../s29-implementer-joint.log`, SHA-256
`902b45b59c2a686cabc2b4cff5b8a1928f1c92beebe4cecd43d72c1d98d79237`.

## G4 / S29 replay-fence locality — IMPLEMENTER — 2026-10-02

Receiver review found the new certificate path called a global tombstone scan in
`TrackingReplayFence.retiredGeneration(UUID)`. Grouped the existing canonical
tombstones by UUID/dimension; total 4096-scope cap, sticky saturation and exact
identity/order rules remain intact. A development regression covers dimensional
maxima, exact removal, repeated removal and scope accounting.

Tested baseline remains `a54031b9` plus local product patch
`../s29-local-fence-product.patch`, SHA-256
`edefdcde3c286cebb93bb0fe28409435915ad085514015bbc1045563a95a31a0`.
Environment matches the preceding checkpoint. Executed:

| Check | Result | Log SHA-256 |
| --- | --- | --- |
| Ordinary `gradlew.bat build --offline --console=plain` | BUILD SUCCESSFUL 3m02s, **459/459 required server GameTests** | `0c30a0377d7b88e27a81775259605d6e0d74514fd0e1e657b754086c4b1f9319` |
| Real-client six-class affected battery | BUILD SUCCESSFUL 2m24s, all six classes, clean teardown | `7e4c3748c98f282b431742241d7af71b17f2568c37e731f6554cc66a852cfb01` |
| Unchanged independent `python tools/s29_adversarial_replay_fence_probe.py` | PASS: three own-dimension visits with 4096 scopes; zero for unknown UUID; budget reuse/replay/saturation pass | `dcc5ca6498d28e2f5f3a2892954418c49c74d2ee6d5157581655aa51939a9875` |

Logs: `../s29-local-fence-build.log`, `../s29-local-fence-client.log` and
`../s29-implementer-replay-fence.log`. Client command uses the preceding joint
lane's init/build directory with comma-separated selectors for
`AnatomyPresentationFrameTests`, `S29AdversarialLiveEndpointProof`,
`S29AdversarialUnavailableReplayProof`, `S29AdversarialDerivedPresentationProof`,
`S29AdversarialCertificateIdentityProof` and `S29ImplementerJointPresentationProof`.
The scratch locality probe compiles the real fence with observational loop counts
and Identifier/payload DTO stubs; it does not substitute for wire evidence.
`git diff --check` passed. Full G4.5 closure is still pending independent frontend
mutation/remaining acceptance evidence. No push, release or installation.

## G4 / S29 receiver identity/bounds/mutation frontier — ADVERSARY — 2026-10-02

Independent evidence on the unchanged local product patch
`edefdcde3c286cebb93bb0fe28409435915ad085514015bbc1045563a95a31a0`:

| Executed check | Result | Log SHA-256 |
| --- | --- | --- |
| LifecycleOrdering + WireBounds + CertificateIdentity + gravity-roundtrip real-client baseline | BUILD SUCCESSFUL **3m10s**, four classes and clean teardown | `340135a1090954fd258f726e50eebb17efe0a1aa7cc97117dd53a34cd162468b` |
| Real-client two-support baseline | BUILD SUCCESSFUL **2m38s**, valid neighbor cannot supply missing target authority; equal serials remain partitioned | `666a9fca9c782c47a4e42571c1f0861881c671b7bcf39d9d31a4345374d9b259` |
| Compiled frontend mutations | **9 killed by specific runtime assertions**, covering the eight minimum S29 families with separate epoch/dimension rows | Individual hashes in the verified inventory below |

Mutation rows: inference, binding, tracking, epoch, dimension, nearest,
material-barrier, bound and cross-support. Sources are copied outside the checkout
and substituted only in generated compilation inputs. No production source file
is rewritten. Compile failures/timeouts/unrelated errors do not count as kills.
The initial PowerShell stderr harness interruption and the initially unrecognized
earlier valid nearest assertion are classified in the adversary's execution
record; neither was misreported as a completed campaign row.

Reproduction: `powershell -NoProfile -ExecutionPolicy Bypass -File tools/s29_adversarial_frontend_campaign.ps1`.
Use `-Mutations binding` (or another named row) to isolate one mutant. Original
positive baseline selectors and exact assertions are recorded in
`sprints/S29-adversarial-recertification.md`. The default UNAVAILABLE oracle and
its separate gravity-roundtrip mode both have positive evidence; the latter was
selected for the material-barrier mutation.

Logs: `../s29-lifecycle-frontend-baseline-green.log`,
`../s29-cross-support-green.log` and `../s29-front-<mutation>.log`.
IMPLEMENTER verified all **12** hashes in `../s29-frontend-evidence-hashes.json`
(two baselines, nine mutants and locality probe), inventory SHA-256
`1b4eaefb159fb41c0f50df72d7aa62522689ff598b0dad1320eeac74eda1307e`.
The recorded frontend source hashes match the working tree and the complete local
production patch hash remains unchanged. Original compilation was restored by
omitting mutation properties, and ADVERSARY recorded BUILD SUCCESSFUL in 20s.

Independent final receiver-frontier review requested zero production changes.
This completes minimum frontend mutation-family sensitivity, not G4.5 closure:
dedicated/reconnect/catalog/dimension transitions, longer heap/GC/fan-out,
applicable visual/contact snapshots and complete acceptance review remain open.
`presentationContact` still consumes CURRENT_ENDPOINT; certified-contact selection
is not falsely claimed by these tests. G4.6 has not started.

### Final combined original-source client regression

After adding the seven adversarial proof registrations to ordinary client
metadata, IMPLEMENTER executed all **nine** S29 proof classes together on the
unchanged product patch above: the prior six plus LifecycleOrderingProof,
WireBoundsProof and CrossSupportProof. Same isolated selector and implementer
joint build directory; no mutation properties, default UNAVAILABLE mode.
BUILD SUCCESSFUL in **2m59s**, exit 0 and clean client/server teardown.

Observed early-certificate continuity across binding-only/tracking-only/both
restart axes; missing/stale before endpoint fail-closed; positive 128-certificate
boundary; 129th certificate sticky saturation followed by newer-tracking recovery;
foreign valid authority cannot supply missing target authority; equal material
serials remain UUID-partitioned. Prior ROOT, DERIVED_CARRY, JOINT and identity
proofs all completed in that same process. The separate independent gravity-mode
baseline above covers the additional roundtrip barrier.

Log: `../s29-receiver-final-client.log`, SHA-256
`004bd7034cb5fb86a3348761acb5ed1ed66f233edafffaec3b5c425eea6e5bfe`.
Product patch SHA-256 remained
`edefdcde3c286cebb93bb0fe28409435915ad085514015bbc1045563a95a31a0`.
Ordinary server result remains 459/459 on this production source; later changes
are client test registrations/tooling/documentation. Manifest registration has no
duplicates and `git diff --check` passed. Both roles' final receiver-frontier
reviews requested zero new production changes; complete G4.5 remains open.

## G4 / S29 catalog rollback and reconnect — IMPLEMENTER — 2026-10-02

Catalog provider-seam regressions use production reload and live published query
frames: accepted reload retires old interval authority; rejected reload retains
the exact accepted provider and interval authority. These are provider-seam
proofs, not independent wire certification evidence. Initial syntax/access and
pre-first-pose fixture failures were classified before corrections.

Real-client `S29ImplementerRejectedCatalogClientProof` reproduced NFR-016:
BINDING deactivated physics, rejected candidate restored READY/presentation, but
the accepted support remained unregistered. Clearing only the registration cache
at deactivation repaired recovery; accepted revision, presentation and physical
queries recovered. RED log `../s29-catalog-rollback-red.log`, SHA-256
`85adf3a62cb65dab61deef2cc799caa2f51b7a6b98a33069910ba38387b9f48a`;
GREEN log `../s29-catalog-rollback-green.log`, BUILD SUCCESSFUL **3m35s**, SHA-256
`3ace898e7fd943964bc03fd1fa89593f3c2423fdd58f156ef8c4dcdf5de329e4`.
Ordinary build at this intermediate source passed **461/461**, 5m18s,
`../s29-catalog-final-build.log`, SHA-256
`b65e98e1764dd7a54f09df0d2dac214b02a0d5d5145f77f66f964bfd34e47069`.

The subsequently revealed dedicated reconnect failure (NFR-017/FR-080) required
strict retirement of old tracking windows. Bounded fences now survive socket
replacement only for the same accepted epoch/revision. Geometry, catalog,
certificates, contacts and receipts are still discarded; quiet TTL recovery
remains distinct from explicit lifecycle retirement. An explicit barrier also
closes quiet tombstones whose full histories had already expired. Closing windows
is a lifecycle-only bounded pass over at most 4096 scopes, not a packet/query hot
loop. Original locality probe passed again: target query visits three of 4096
scopes, unrelated UUID visits none; budget reuse and saturation remain intact.
Log `../s29-reconnect-locality.log`, SHA-256
`dcc5ca6498d28e2f5f3a2892954418c49c74d2ee6d5157581655aa51939a9875`.

Ordinary build after reconnect repair passed **462/462 required server tests**,
BUILD SUCCESSFUL **7m58s**, `../s29-reconnect-final-build.log`, SHA-256
`140657ca00212cab6a63b621d8bf19ac61438e1fc8ce1b49417d7acf724c3102`.
The unchanged revealed dedicated early-reconnect property also passed in
IMPLEMENTER's isolated build/cache/run directories, BUILD SUCCESSFUL **11m22s**,
`../s29-implementer-early-reconnect-green.log`. Independent reconnect recovery,
combined client checks and source/evidence freeze follow. This entry does not
close S29/G4.5 or claim whole-matrix visual/performance/contact acceptance.

IMPLEMENTER's combined **ten-class** original-source client battery passed,
BUILD SUCCESSFUL **4m57s**, exit 0 and clean teardown. It includes the prior nine
receiver proofs plus rejected-catalog physical recovery. Log
`../s29-catalog-reconnect-final-client.log`, SHA-256
`36f311c680e121bb12e89fe59048f2f94386970f4d027d831d416e5c7fc9dc56`.
The dedicated early-replay log's SHA-256 is
`722069f8928a6deb705ba652b0da84e7f15a4c992bf0003fc1efb733eb58b82b`.
Inventory `../s29-catalog-reconnect-evidence.json` records seven source hashes,
eleven executed logs and complete production patch SHA-256
`970077a86cd75802ce50eb95e27e6d085b28cc4004083e2b4c3428f1ee222c36`.
That text patch includes a post-run Javadoc correction to saturation ownership;
executable production code did not change after the successful checks.
Independent catalog/reconnect frontier review completed as recorded below.

### Independent frontier acceptance — 2026-10-03

ADVERSARY independently passed catalog rollback with honest digest/invalid JSON
and normal persistent dedicated reconnect in one lane, BUILD SUCCESSFUL 2m32s,
clean teardown. Log `../s29-adversarial-catalog-normal-reconnect.log`, SHA-256
`568972ed8157843f66f5dd29f0376957af191de3c1d098a2c858ac2c304d2801`.
Its separate original early-replay lane passed `authoritative=false` and closed
cleanly, log `../s29-adversarial-early-reconnect-green.log`, SHA-256
`2abc0a73aea9ac833a95d07d7c67d0bbcd5687d30ec2bb2e6cce497a9ccca333`.
The latter spans a host-clock discontinuity; printed 5h4m46s is not performance
evidence. Expanded independent strict-window/locality probe also passed, log
`../s29-adversarial-replay-closed-window-green.log`, SHA-256
`dd1c08993c72940d9071953c38407b13ee689da05621d85c3ffc9b4d79fbffc7`.

IMPLEMENTER then reproduced the revealed catalog proof without modifying it,
BUILD SUCCESSFUL 1m49s, clean teardown. Log
`../s29-implementer-adversarial-catalog.log`, SHA-256
`14c75c5ffb1406ed44e02780e8730613b143a0ec66187f7d756ab98de7bc00fb`.
The prior adversarial run reached green assertions but blocked during Fabric's
integrated-server cleanup. It remains infrastructure evidence, not a completed
successful build. ADVERSARY's two-tick fixture cleanup repair was repeated cleanly
by both roles; production was unchanged. Its thread dump and inconclusive log are
preserved separately in the adversarial sprint record.

IMPLEMENTER verified the independent client/fence source inventory against the
frozen production and all eleven evidence hashes. Both roles' final complete
review of this frontier requested zero production changes. This closes the tested
catalog rejection and persistent reconnect defect frontier, not S29/G4.5: full
accepted-catalog client/dimension/permutation matrix, applicable visual/contact
snapshots, sustained heap/GC/fan-out and whole-sprint review remain open. No G4.6
work or publication occurred.

## G4 / S29 accepted catalog live-client development proof — IMPLEMENTER — 2026-10-03

`S29ImplementerAcceptedCatalogClientProof` used a real prepared cow, a genuine
server-issued ROOT certificate, a valid `AnatomyRuntime.reload` from test resources,
and a second real ROOT move. The same client process rejected the old revision's
certificate, recovered current presentation plus physical query on the committed
revision, and materialized a fresh exact certificate with higher binding generation.
BUILD SUCCESSFUL **1m42s**, exit 0 and clean teardown. Log
`../s29-accepted-client-third.log`, SHA-256
`64a6b5557bebe846dd7b0643f6c58afd4ae58be3dbc1cdfe8c5695837a03af50`.

The subsequent ordinary build passed **462/462 required server GameTests**,
BUILD SUCCESSFUL **1m51s**. Log `../s29-accepted-ordinary-build.log`, SHA-256
`91ddb0406d8703cc72106e4abb8f4d121922dfaafaac039d64b1213ec15a5329`.
The first client attempt failed before reload because its fixture asserted physical
registration before the next client tick. A second attempt was blocked at compile
by a concurrently authored adversarial fixture's package access, which ADVERSARY
corrected in test code. Neither is classified as a production failure or a
successful acceptance run. All seven production source hashes remained equal to
`../s29-catalog-reconnect-evidence.json`; independent dimension/catalog review
and the complete G4.5 acceptance matrix remain open.

### Independent accepted replacement and dimension transitions — 2026-10-03

ADVERSARY's real-client fixture accepted a replacement model/binding, rejected a
captured older catalog bundle and interval, and materialized a new certified ROOT
interval. It then completed an active Overworld-Nether-Overworld round trip with
fresh tracking, followed by an early replay round trip inside TTL with publication
stopped before fresh pose. The three PASS markers appeared with clean teardown,
BUILD SUCCESSFUL **1m6s**: `../s29-adversarial-catalog-dimension-scoped.log`,
SHA-256 `9f35fd6daadd14eacc49d4c1409df246e1ae51fb0cba103effc1d69861a28bc0`.
IMPLEMENTER reviewed the revealed fixture and independently repeated the same
three assertions with clean teardown, BUILD SUCCESSFUL **1m12s**:
`../s29-implementer-catalog-dimension.log`, SHA-256
`8d6886230120b0774b46ae0281b6271c8f200b499911316a74d2b15982381634`.
The adversarial source-hash inventory matches current client networking, runtime
and replay fence files. Earlier fixture diagnostics (invalid model source, physical
ready ordering and reused numeric material serial) were classified and corrected
only in test code, as recorded in its sprint execution record. No production
source changed in this slice. G4.5 remains open for broader connection/contact
permutations, visual/contact snapshots, sustained heap/GC/fan-out and final review.

## G4 / S29 certified-inbox virtual-clock soak — IMPLEMENTER — 2026-10-03

The isolated `S29ImplementerIntervalSoak` GameTest ran a 12,000-tick **virtual**
receiver clock over 16 independent inboxes and 64 supports, publishing one
independently allocated certificate per support/recipient every four virtual
ticks. It accepted 3,073,024 exact
certificates, retained at most 26 live entries per support, and kept all 64
support identities per inbox without saturation. Settled heap after explicit GC
at ticks 6,000/12,000 was 237,681,888 / 237,685,832 bytes; six GC cycles
occurred across the run. Both selected GameTests passed and the server shut down
cleanly: `../s29-implementer-interval-soak.log`, SHA-256
`737d13062927261a446ab6054cf5bdbdd96dee7b96da1a901c951b994dae3d0c`.
This is bounded retention evidence for the S29 receiver. The synchronous
virtual-clock workload does not measure real server TPS, packet fan-out, bodies,
rendering or NFR-014's baseline-relative p95/p99 budget; those remain separate
acceptance work. No production code changed for this probe.

## G4 / S29 contact replay over persistent-server reconnect — 2026-10-03

ADVERSARY's real-client `S29AdversarialContactLifecycleProof` captured a genuine
confirmed passive pig-on-cow contact, disconnected from a persistent dedicated
server, cleared its server contact, and replayed the old packet after the same
catalog and fresh support tracking had arrived. The original candidate was
RED: `presentationContact` returned authority for that old packet while
`physicalSupported=false` (`../s29-adversarial-contact-reconnect-dimensions.log`).
This was an NFR-017/FR-080 client lifecycle defect: disconnect and same-catalog
reacceptance erased contact ordering watermarks. IMPLEMENTER now preserves only
the bounded watermark across socket replacement for the same accepted
`(epoch,revision)` and clears it for a different catalog identity. The exact
holdout passed unchanged: `../s29-implementer-contact-fix.log`, SHA-256
`42fbd8e23ab571c0ac574e38f2ad345a7fc6e594a5c734f299305bfa1cbfbfa1`,
BUILD SUCCESSFUL 1m42s, clean shutdown. Old replay returned
`authoritative=false physicalSupported=false`; a new generation's contact
then materialized. Independent retest and affected-suite closure are pending.

The expanded ordinary client run stopped in the older
`S00ClientBoundaryTests` before reaching the newly registered S29 proofs:
`Fixture did not record the actual applied delta`. The same S00 fixture failed
alone (`../s29-s00-isolated-baseline.log`, SHA-256
`d742f4f9bcd2993aff566b27b347db167494f1c625b8e7cce3abc6c755ab0e0f`).
It attempted to record a server receipt without first opening the recipient's
self-tracking window; that read became pure in S24. The test-only fixture now
starts an empty prepared runtime and verifies the explicit window before its
receipt assertion. The unchanged isolation assertions passed alone:
`../s29-s00-isolated-repaired.log`, SHA-256
`be516463f75b7b5ae7705eb6312f224408e9464aeb792daa1f2ce48d5913b65b`,
BUILD SUCCESSFUL 1m28s. This is a fixture repair, not a production receipt
change; the expanded whole-client suite still needs rerun.

ADVERSARY then varied only the captured old contact's sequence and reproduced
another RED after the first fix: an unseen later sequence from the retired
body tracking generation revived presentation (`../s29-adversarial-contact-delayed-window.log`,
SHA-256 `af75dbf9deb9e33f14d078622df6ce5572fbc1a4cc1598da7bcf42cc0dbd0b49`).
The client now retires the **entire** observed body tracking generation at
every level/socket barrier, retaining only a scalar in its bounded contact
entry. The same delayed-window holdout passed with old replay absent and a
fresh-generation positive control present: `../s29-implementer-contact-delayed-fix.log`,
SHA-256 `f1fa1ec5ab8501d9f7bd859c6ce3356a09c99fb129298de8bda794f524cd8bdf`,
BUILD SUCCESSFUL 1m32s.

A development GameTest also proved that two recipient ledgers on one server
could reuse `old=1 fresh=1` (`../s29-recipient-generation-red.log`, SHA-256
`f15bf873c05c1d5c9536dba328959ac0624ca35a50304e82d61bb85fbe422a94`).
Server tracking windows now allocate from one monotonic scalar across
recipients for that server, without retaining retired UUIDs. The same test
passed 2/2: `../s29-recipient-generation-repaired.log`, SHA-256
`687e51f545cbd7cee7e01b3f9b3b7b10757baa46512b864f86535ba09ef4404b`,
BUILD SUCCESSFUL 39s. This closes the proved generator reuse, pending
independent retest and the full affected suites.

The ordinary build after strict contact-window retirement, shared server
tracking generation and the S00 fixture repair passed **462/462** required
server GameTests and `build`, with clean shutdown:
`../s29-ordinary-after-contact.log`, SHA-256
`a5e178fc3fc82c2f6a00e686161c78ce80b2bde34b5a2917d424648084d0f23a`,
BUILD SUCCESSFUL 1m29s. The recipient-generation regression was registered in
the ordinary manifest after this run and therefore requires the final ordinary
rerun.

The expanded ordinary client battery passed through all registered client
GameTests, including the repaired S00 boundary, accepted/rejected catalog,
A-B-A dimension, interval identity/order/bounds and cross-support proofs:
`../s29-default-client-after-contact.log`, SHA-256
`fc73fc5e012bc96dda25c8e932b4f8d50f29b153d63760c756897950d450722c`,
BUILD SUCCESSFUL 4m25s with clean integrated-server shutdown. The Mojang
profile-key HTTP 401 messages in the offline test client did not fail the run.
This battery predates final live-contact adversarial confirmation and does not
by itself close G4.5.

The final ordinary build, now including the recipient-generation regression
in the default server manifest, passed **463/463** required GameTests and
`build`: `../s29-ordinary-final-candidate.log`, SHA-256
`99b8f2ea0c211b59b313fcd28f251e3081e0697a70261bbca603f75b6d14d79b`,
BUILD SUCCESSFUL 1m44s, clean test-server shutdown.

ADVERSARY repeated the exact and unseen-later-sequence contact replays over
real dedicated reconnect and integrated A-B-A dimension transitions on the
current repair. `../s29-adversarial-contact-final-delayed.log`, SHA-256
`59c0fd949b558efd1db9abc79a49e2c6e861b118dc6eedb3f0608b6159fc17ff`,
passed in 1m25s with clean shutdown. A separate no-injection holdout obtained
both contacts from the real solver and compared actual server/client body
generations across two sockets of the same persistent server: **3 -> 6**;
catalog identity stayed equal, the observer remained passive and the new
contact became drawable and physically supported. Its log is
`../s29-adversarial-contact-final-exact-live-port.log`, SHA-256
`8fb9accd00f8296449c131fa20584fafbaa8a797e5b0e414b73de6c85e17cdf27`,
BUILD SUCCESSFUL 1m28s with clean shutdown. The first attempted live run was
blocked by concurrent test servers using port 25565 and is not product evidence.

The independent final-candidate client repetition selected 13 ADVERSARY proof
classes and passed: `../s29-adversarial-final-candidate-client.log`, SHA-256
`d7a34216f5af622502d43a44d7aa67724b15afee3e7ec4699c38ef74f61b6f51`,
BUILD SUCCESSFUL 2m25s. The final presentation-work server holdouts passed
3/3, with no duplicate first-joint evaluation and no additional joint
evaluation when alternating current/historical endpoints:
`../s29-adversarial-final-presentation-work.log`, SHA-256
`93a240320aea339605776eab191a17e9ea0cfabe9b81830bfde5ff253ec64afa`,
BUILD SUCCESSFUL 38s. The independent source-level inbox campaign killed
eight targeted mutants on the final candidate:
`../s29-adversarial-final-inbox-mutants.log`, SHA-256
`9e7c4d9e70fceef89976c54d209af14ac65aab8a6986664868ad254115a94154`.
These are causal/order/bounds and structural-work results; the global sustained
network/tick benchmark and real-renderer acceptance are separate.

## G4 / S29 changing-fraction presentation preparation — 2026-10-03

ADVERSARY's real-client batch proof built 64 tracked anatomical supports and
queried the same certified endpoint pair at changing fractions. A disposable
observational copy of `HierarchyMotion` counted 64,000 invariant motion
preparations in 1,000 one-consumer batches on the preceding candidate:
`../s29-adversarial-presentation-preparation-counter-retry.log`, BUILD FAILED
at the explicit HOT_PRESENTATION assertion. The associated uninstrumented
batch measured about 6.77 MB allocated and p95 5.84 ms per 64-support
one-consumer batch (`../s29-adversarial-presentation-batch-repeat.log`). This
is a repeated-preparation defect, not a measured violation of G9/NFR-014.

IMPLEMENTER cached one prepared motion per live entity and exact endpoint
pair, leaving fraction-specific geometry evaluation and all receiver authority
checks in place. The unchanged instrumented holdout then passed:
`../s29-implementer-motion-preparation-fix.log`, SHA-256
`662f677a2765fad9cd2b7a87976ea067f5539b5fcde17d841680d2e178f09ad9`,
BUILD SUCCESSFUL 1m40s with clean shutdown. It recorded **zero** repeated
motion preparations and zero duplicate joint evaluations under 64 supports;
one-consumer changing-fraction allocation was 2,274,153 bytes/batch and p95
3.105 ms, while 16-consumer batching was 3,095,552 bytes/batch and p95
3.189 ms. Both are synthetic render-query batches under a frozen server clock,
not real-tick p95/p99/TPS or the full NFR-014 workload. The new material
evaluation still allocates for each distinct fraction; this measurement does
not erase that remaining cost. Default suites and independent retest follow.

The ordinary server build after this cache repair passed **463/463** required
GameTests and `build`, with clean shutdown: `../s29-ordinary-after-motion-cache.log`,
SHA-256 `08df1529668945aa502594a5cc6066d3d0bbaf1e3ac8cf80491a253b61128738`,
BUILD SUCCESSFUL 1m21s. The full default client suite and independent
adversarial repetition were then run on the same source candidate.

The expanded default client battery after the repair passed:
`../s29-default-client-after-motion-cache.log`, SHA-256
`81014b605840b1e5bf37277ee2457a728525bfb4d5264f9b9a8b48d048e00715`,
BUILD SUCCESSFUL 4m46s, clean integrated-server shutdown. The independent
instrumented presentation repetition passed with zero repeated preparation
and zero duplicate joints:
`../s29-adversarial-presentation-preparation-repaired.log`, SHA-256
`8df997b797b04911024d96562991297aeca82b6f0b7b146376fc755524d8444a`,
BUILD SUCCESSFUL 1m45s. ADVERSARY's separate full client repetition passed:
`../s29-adversarial-preparation-repair-full-client.log`, SHA-256
`63e0a97e067ab5f33925add86e49dc080c0aaeaa569711aae3a2ff4e2541c206`,
BUILD SUCCESSFUL 2m40s. The independent barrier-specific run passed:
`../s29-adversarial-preparation-repair-barriers.log`, SHA-256
`e054b3242d8c3b07065de786d8018bda0b14f26dc90dfc1348ef083189cf0620`,
BUILD SUCCESSFUL 1m32s with clean exit. Three affected server presentation-work
holdouts passed:
`../s29-adversarial-preparation-repair-server-work.log`, SHA-256
`54d99a40b9f3f5790a568cb908c45951823cec400ebc64199bdf256d8aa5635e`,
BUILD SUCCESSFUL 39s with clean exit. ADVERSARY compared the candidate's nine
productive hashes with its pre-run snapshot, checked the source diff, and gave
a GREEN whole-sprint S29/G4.5 zero-change verdict. Its matrix and the applicable
ROOT/JOINT/contact visual inspection are recorded in
`docs/sprints/S29-adversarial-recertification.md`. These results close S29's
certified primitive; they do not claim the G9 real-tick NFR-014 benchmark or
the G4.6 renderer/residual/camera consumer.

## S30 causal original-model renderer — post-RED IMPLEMENTER verification (2026-10-03)

The first independent S30 pose holdout rejected one valid near-gimbal Cow
ModelPart TRS; its original-model oracle proved the accepted matrix drawable.
After the production Euler-extraction repair, the unchanged 512-case holdout
passed in both lanes, including nested/exceptional full-field restoration and
shear rejection: IMPLEMENTER `../s30-fix-pose-proof.log` (SHA-256
`17e679cbe8fdd658b00d1578a1108f11798ab849287cec3e3e20690cc8cf4c29`,
BUILD SUCCESSFUL 1m12s); independent ADVERSARY
`../s30-adversarial-model-pose-repaired.log` (SHA-256
`0dc108e23f970cadcd798b4169fc07b6340707e261652b2aabd63e49495c33cc`,
BUILD SUCCESSFUL 1m9s). The targeted renderer/adapter client proof passed in
`../s30-implementer-client-after-fix.log` (SHA-256
`cd5aa07c3192f2d0403fe85cecc7cfb9d3c2b3793a327001bad69521c3a56cbf`).
The server battery passed **463/463** required GameTests in
`../s30-server-after-fix.log` (SHA-256
`5340ccd6ec0a94580684782ef55d5f7d27471e3c003efa693a762c7e7ae1436c`).

The first full ordinary client run after the repair failed in the pre-existing
`S29ImplementerAcceptedCatalogClientProof` retained-certificate assertion:
`../s30-ordinary-after-fix.log` (SHA-256
`b9056b74d4fdce82c8aa2f485cfaa6f56776c0115ab87692023b692d277f47ff`,
BUILD FAILED 3m31s). The exact proof passed alone in `../s30-s29-isolated.log`
(SHA-256 `90b22208d0972d7a43bc9d35ea0aa93eadc23aa8053b48f00365998d0fbf045d`,
BUILD SUCCESSFUL 1m41s). The full ordinary suite then passed, including that
proof, in `../s30-ordinary-after-fix-retry.log` (SHA-256
`215c50f9b311e6b2ab72a26b70b06bedb128fc9c11e8af94cc9c6a57b0e2fce2`,
BUILD SUCCESSFUL 3m55s). The earlier failure is an intermittent observation,
not a repaired or explained S29 bug. A separate ordered run of the adjacent
rejected- and accepted-catalog proofs also passed:
`../s30-s29-pair.log` (SHA-256
`3c301865d0249c2db015c7c66ea04c0240623eb22bf2127938264db7df7b590a`,
BUILD SUCCESSFUL 2m1s). S30 still needs independent renderer,
snapshot, performance and zero-change review before closure.

The subsequent S30 reflected-scale RED was repaired by trying determinant-
compatible local scale sign assignments before accepting a recomposed
`ModelPart` TRS. The unchanged original-model signed-scale holdout passed all
eight masks in `../s30-signed-fix.log` (SHA-256
`4da3732dcbcdf89648774f016ff9236cd7e9483fc023102a4de3bc9f068bebdd`,
BUILD SUCCESSFUL 1m9s). A combined run of that holdout and the earlier 512-pose
near-gimbal/restoration/shear holdout passed in
`../s30-pose-combined-after-signed-fix.log` (SHA-256
`48d358abddae2b0ee9714f3712a6c958e229d5bfabafcd1e0149902c1a04afcd`,
BUILD SUCCESSFUL 1m21s). Independent revalidation and final full suites on
this second repair are pending. The implementer-directed client renderer suite
passed in `../s30-implementer-client-after-signed-fix.log` (SHA-256
`15bdfeacefd21224de1f70c225c46016d925c6565b5bb410983c4b4a1cb8d2c8`,
BUILD SUCCESSFUL 1m21s), and the server suite passed **463/463** in
`../s30-server-after-signed-fix.log` (SHA-256
`90bc6ff523b006fd2d21fa53795242637e1f82a316395bda6e40c9ded3fd85ec`,
BUILD SUCCESSFUL 1m40s). The final ordinary client suite on this source passed,
including `S29ImplementerAcceptedCatalogClientProof`, in
`../s30-ordinary-after-signed-fix.log` (SHA-256
`2d4b9315af3a6f19c4f381c70528a9f5264aded06f14b90e38648c28723bc730`,
BUILD SUCCESSFUL 3m55s). Independent zero-change review remains pending.

The later S30 tracking-window alias RED was repaired by including the accepted
support tracking generation in the `PresentationFrame` cache key and queued
original-model draw token. The unchanged real-network holdout passes: an old
submit draws no vertices after generation 1→2 even when entity, selection
identity and frame serial are reused; a fresh submit draws normally.
`../s30-tracking-fix.log` (SHA-256
`be041557cebb1159c03805dbcbf11c2119ef96e404370fd9f9db2162b098462d`,
BUILD SUCCESSFUL 45s). Independent repetition and final suites on this
candidate are recorded below.

The independent six-class battery passed on the repaired source in
`../s30-adversarial-tracking-repair-complete.log` (SHA-256
`65be6481a84184fcceaf5bc459215dca44652722c87d4a6f1e54860a3bc0e72a`,
BUILD SUCCESSFUL 1m2s). The separate native receiver/interval route passed in
`../s30-adversarial-final-native-receiver.log` (SHA-256
`f9bea58ad981b0144c6a42a8f2cc79148a94a58dc2d86cafdda04cd4b4be0444`,
BUILD SUCCESSFUL 37s). ADVERSARY killed seven semantic mutants, inspected
fixed ROOT/JOINT captures, and reported a complete zero-change review. Its
isolated native cost probe measured 64 supports / 1 consumer at 632,762 bytes
per batch, p95 .4815 ms, p99 .624 ms; 16 consumers at 9,682,944 bytes,
p95 2.9194 ms, p99 5.8002 ms. These are synthetic batches, not G9 real-tick/TPS
acceptance.

IMPLEMENTER's targeted client proof passed in
`../s30-implementer-client-after-tracking-fix.log` (SHA-256
`2f15f79f8216863dfc839c93e336ccf4f3093083161be0bd6777efa848ec82fa`,
BUILD SUCCESSFUL 55s). `../s30-server-after-tracking-fix.log` (SHA-256
`969236ee38c8e925b2c3af996ef2bd128ed7dbf15b83d96548a00a6fdd9ba25f`,
BUILD SUCCESSFUL 1m3s) passed **463/463** required GameTests and `build`.
The first full ordinary client pass failed in the pre-existing S29 rejected
catalog physical-registration proof, `../s30-ordinary-after-tracking-fix.log`
(SHA-256 `fbec9b0a7ceca70706fc8d31dc1601bbdce5597ce41aadf14a440f19531c83bd`,
BUILD FAILED 2m48s). The second passed that proof, then failed in the prior
S29 accepted-catalog certificate assertion,
`../s30-ordinary-after-tracking-fix-retry.log` (SHA-256
`f321a676c10e12163d856e859ee1e28f8bee8f7ecc13d507ed3aefed572c9f3f`,
BUILD FAILED 2m37s). An assertion-only diagnostic was then added to that S29
proof; its oracle and all production remained unchanged. The third complete
ordinary client pass succeeded, including both S29 catalog proofs:
`../s30-ordinary-diagnostic.log` (SHA-256
`192d5049ae51e4de15349430101db3f20bddd247d7978675793be565d5597bbd`,
BUILD SUCCESSFUL 3m2s). This supports S30/G4.6a closure but does not identify
or repair the S29 intermittent cause. Full G4.6 remains open.

### S31 / G4.6b local body and camera residual (2026-10-03)

The final combined independent lane passed **20 S31 and 6 S30 client proofs**
on unchanged production sources in `../s31-adversarial-final-combined.log`
(SHA-256 `39d9500bc35a5a68f23d3b9e884c371c8f60385a47e40319959bd2584555bc26`,
BUILD SUCCESSFUL 2m9s). It includes both native dual-role Pig cases: fractional
carry and the following tick after vanilla incorporation. The directed mutant
that limited root alignment to an active fractional sample was rejected by the
unchanged next-tick oracle in `../s31-adversarial-mutant-local-root.log`
(SHA-256 `29e2ae1cf66d9a62e35ee8f2c47b3a54f3146d85959307f1fcb96a5a7046805a`,
expected BUILD FAILED 28s); the mutation copy was removed. Adversarial geometry
snapshots and source hashes are recorded in `docs/sprints/S31-g4-residual-camera.md`.
The local work probe measured 131,072 active queries at p95 ≈381.3 ns,
p99 ≈391.2 ns and 120 bytes/query; this is not the G9 whole-scene budget.

The final implementer targeted client proof passed in
`../s31-implementer-postincorporation.log` (SHA-256
`7dc9d0b221e03bda4f39c4c664038cb7e04262880dcbfa9b02a7d1a5c96d5f9a`,
BUILD SUCCESSFUL 1m3s), and the pure plan model passed. `build runGameTest`
passed **463/463** required server GameTests in
`../s31-server-postincorporation.log` (SHA-256
`9849a236ec9f861ee82c30015d8dbd3c55d9af4f0e1bf1f0056be7f6df193198`,
BUILD SUCCESSFUL 1m). The complete ordinary client suite passed in
`../s31-ordinary-client-postincorporation.log` (SHA-256
`1d1719574118c4bce03b5e6ab24564b0cf3a15eb333468df69e0b4120e58bb66`,
BUILD SUCCESSFUL 3m). S31 closes only the locally predicted body/camera route;
remote observer incorporation provenance remains S32 and G4.6 remains open.

### S32 / G4.6c remote observer provenance — active verification (2026-10-04)

On the S32 candidate after the recipient teleport-cursor barrier, `build
runGameTest --offline --console=plain` passed **463/463** required server
GameTests in `../s32-server-after-teleport.log` (SHA-256
`f9dcdd4928dcea90a5154d675a97575e52dedd97f9ea63f1e47d98018c16c71c`,
BUILD SUCCESSFUL 5m8s). The complete ordinary client GameTest suite after
the gravity and support-material barriers passed in
`../s32-ordinary-client-after-barriers.log` (SHA-256
`530a34f0e61d3399e4f9416700094bc9a73fdc9452de6069d36d731bb9f2b8a7`,
BUILD SUCCESSFUL 5m14s), including existing 0/100/200 ms dedicated soaks.
After the bounded history reclamation, a second `build runGameTest --offline
--console=plain` passed **463/463** in `../s32-server-after-history-reclaim.log`
(SHA-256 `a7720db57419c68998dc687108370da32705a53f952fa894da9f41fb5f5de3e1`,
BUILD SUCCESSFUL 4m3s).
These regressions do not by themselves close S32/G4.6; the independent R5/R6
and final zero-change review remain pending.

After the delayed-render token and fallback checks, the first complete
ordinary client run failed at the pre-existing S29 rejected-catalog physical
registration assertion: `../s32-ordinary-client-final-candidate.log` (SHA-256
`07cf816d9bb6a17deed06d47f3fef22afa73e73b2935c3e1c28cd6fbbaa1909d`,
BUILD FAILED 4m49s). The exact unchanged S29 proof passed alone in
`../s32-s29-isolated.log` (SHA-256
`9334a3ffda541d8597dea497eb9f131b7d81f0e1ccbd5210514538bc325de20c`,
BUILD SUCCESSFUL 2m1s). The full ordinary client suite then passed on the
unchanged production candidate, including that S29 proof, in
`../s32-ordinary-client-repeat.log` (SHA-256
`58737623b5c6fb4bd9c738bb5ea6aebfe82035767deb37ba2d1aeb605d687b2a`,
BUILD SUCCESSFUL 4m23s). This is another intermittent S29 observation;
its cause remains unexplained. The final S32 adversarial R6 and zero-change
review remain open.

The adversary's public native-baseline holdout found a real R4/R6 presentation
failure: after a valid ROOT carry and native interpolation, a same-tick
`Entity.setOldPosAndRot()` replacement left a stale remote offset. RED
`../s32-adversarial-native-baseline-red.log` (SHA-256
`8a338ae4f73a0d764c9d48ecfe4462d531492437ac488241718f39701e83367a`,
BUILD FAILED 2m39s). The implementer recorded the native `oldPosition` with
each accepted observation/step, gated the pure render read on it and retired
the relation at the next tick boundary. The unchanged public proof passed in
`../s32-native-baseline-implementer-green.log` (SHA-256
`877090b9e1c60ec6206903f25f7796f51b0562f83a4804757e38dae44043f041`,
BUILD SUCCESSFUL 2m10s), preserving the prior fractional positive samples.
Independent repetition and the full R6 matrix remain pending.

The complete ordinary client suite on the native-baseline repair passed,
including the S29 rejected-catalog proof and existing dedicated latency
soaks: `../s32-ordinary-client-after-baseline.log` (SHA-256
`911bfdb73aad592fd58ce893287b2b4fef7d4d6b0094a984075d08797d46208a`,
BUILD SUCCESSFUL 5m45s). This does not replace the independent R6 matrix.
ADVERSARY reran the unchanged public baseline holdout in its independent lane:
`../s32-adversarial-native-baseline-green.log` (SHA-256
`596d132ec0c2c9a843fbdb49781d4119f6199f104ce7e0f882eba5a54cb2d03e`,
BUILD SUCCESSFUL 5m25s with clean shutdown). This confirms the reduced repair,
while the remaining R6 matrix stays open.

A second, stronger R6 holdout changed native `oldPosition`, then restored the
exact `(position, oldPosition)` values with no intervening query or tick.
The value-only guard let the stale residual revive. RED
`../s32-adversarial-native-baseline-roundtrip-red.log` (SHA-256
`af21e58f7f73b1400b9d690d37a3aaba7e3e0eff89898142c57461add62a76e7`,
BUILD FAILED 2m17s). The implementer added an event generation around changed
vanilla `setOldPosAndRot` writes and verified the unchanged public oracle:
`../s32-native-baseline-roundtrip-implementer.log` (SHA-256
`46d17aa45eeadf311688e9793861e1687c36c2185eb4ecfd77b6d773c109f4d1`,
BUILD SUCCESSFUL 2m4s; two active native steps and zero offset after the
round trip). ADVERSARY independently reran both unchanged baseline oracles in
`../s32-adversarial-native-baseline-roundtrip-green-second.log` (SHA-256
`9de4e435c20971898016e6a719ffdae07838fb28542964f7651e0866ea7e8f91`,
BUILD SUCCESSFUL 5m46s with clean shutdown). Its separate voluntary
cancel/return case also passed with no attestation and zero residual despite
active native interpolation (`../s32-adversary-private/voluntary-cancel-return.log`,
SHA-256 `1df0a33940b02711492ee7779ccd9fa4113fa0c6d47803a100b16bc18629f20d`,
BUILD SUCCESSFUL 5m56s). The remaining R6 matrix stays open.

The first complete ordinary client suite after the baseline-event hook again
failed at the pre-existing S29 rejected-catalog physical-registration
assertion: `../s32-ordinary-client-after-baseline-stamp.log` (SHA-256
`6728cb05550af0439c7e05d4d79b264c416086fcb2c61d491f22066a62e627aa`,
BUILD FAILED 4m55s). An assertion-only diagnostic was added to that S29 proof,
without changing its oracle or production. The next complete ordinary suite
passed, including S29 and dedicated soaks:
`../s32-ordinary-client-s29-diagnostic.log` (SHA-256
`413815f7181d953f1dd619ea3dd5e67311459621713a99bb2a22fd2204db4d5c`,
BUILD SUCCESSFUL 6m48s). The diagnostic therefore did not capture a failing
state; the S29 intermittent cause remains open and is not attributed to the
S32 baseline hook.

ADVERSARY's independent affected-root run passed the actual +3X custom-root
catalog and all 11 receiver scenarios: `../s32-adversary-private/custom-root-affected.log`
(SHA-256 `e64f7f09b397fd17a6d7f0110683f15abefd37ef8ffc1c2d9768b3a14753790a`,
BUILD SUCCESSFUL 2m43s). The affected-root gate suppresses remote residual
while the accepted custom root owns the visual offset. The JOINT and latency
portions of the dedicated matrix and final R6 review remain open.

The independent dedicated `allow-flight=false` production-pair ROOT fixture
passed on two real recipients with clean shutdown:
`../s32-adversary-private/production-pair-20261004-034432-196/host.log`
(SHA-256 `7d8316adfe077f19e2a2bd395ad546a7af0c466c0694571f39a00359674edfe1`,
BUILD SUCCESSFUL 55s) and its `peer.log` (SHA-256
`04ec588190040657b3fc64c15d4d7ffaacab508c0e539139ef04b10d4db23763`,
BUILD SUCCESSFUL 53s). A late receiver omitted already incorporated sequence 1;
sequences 2 and 3 matched five render fractions on both receivers. The
reconnected peer received only sequence 4 with a new tracking generation and
nonce 1, and no spawn residual. This proves the executed ROOT pair/reconnect
fixture; JOINT, latency and the rest of R6 remain under review. An earlier
fixture attempt sampled before accepted material was ready and observed a
transient zero; it did not establish a production defect.

ADVERSARY found a production R2/R4 mismatch after a real Z+.12345 carry had
settled and a later X+.25 carry arrived with accepted contact and Q2 material.
The server had attested Z=5.24845 from `ServerEntity`'s exact base, while the
client's native codec decoded Z=5.24853515625; the strict event-scope check
retired the valid X residual. Public unchanged oracle
`S32AdversarialCodecAxisProof` was RED in
`../s32-codec-axis-adversary-red.log` (SHA-256
`61c9bc40fa3b5bd80c03abadae5fc36ad4b26277e3d7844a8b08c9e8152b8317`,
BUILD FAILED 1m36s). The implementer now tracks each recipient's actual
decoded native codec base across position sends, preserving it across catalog
rebinds of the same live entity. The same public oracle passed unchanged in
`../s32-codec-axis-implementer-green.log` (SHA-256
`8a7e2e8a91a9534aa9955183e5c9e7eab7a7feb831f34ed7383454a91bd67de0`,
BUILD SUCCESSFUL 2m54s): the attested and actual codec Z both read
5.24853515625 and the new X residual was positive. Independent repetition
and the affected full battery remain pending.

The complete post-codec-change server build and GameTests passed 463/463:
`../s32-server-after-codec.log` (SHA-256
`66974c588de7b7452c4c1c0693cb0bde9ad87b903ce1015db9dbb062d2eb0fe5`,
BUILD SUCCESSFUL 6m26s). This includes compilation of the new cursor path;
the ordinary client battery and independent R6 repetitions remain pending.

ADVERSARY independently reran the unchanged public codec-axis and both native
baseline oracles together: `../s32-codec-axis-adversary-green-retest.log`
(SHA-256 `7543703f1e52f5ab528b9caa699965a2fff6245d097683c72216cb42909805de`,
BUILD SUCCESSFUL 2m24s, clean shutdown). Its 13-case affected receiver battery
also passed in `../s32-adversary-private/codec-affected-retest.log` (SHA-256
`f4332eec6f7cead378974cede3c077df1e232c04669b6a064c1c8cab70af228d`,
BUILD SUCCESSFUL 7m4s): codec, voluntary cancel/return, custom root,
rotation/no-op/restart, replay, bad Q2 and recovery, snap, scope reorder and
body/support/burst gravity. JOINT dedicated and latency cases still remain.

The complete ordinary client GameTest run after the codec fix passed in
`../s32-ordinary-client-after-codec.log` (SHA-256
`8d758e72af68ff5666e517a3f7d1afd300826b2338a1470099e9860a3a6a9647`,
BUILD SUCCESSFUL 4m31s), including the S29 rejected-catalog proof and the
existing dedicated latency soaks. This is the latest ordinary client-source
regression, separate from the remaining S32 adversarial R6 dedicated matrix.

ADVERSARY's dedicated two-recipient ROOT/JOINT fixture passed with
`allow-flight=false`, a zero-delay host and a peer behind a FIFO proxy with
nominal 50 ms each-way delay (100 ms RTT):
`../s32-adversary-private/production-pair-20261004-044927-957/host.log`
(SHA-256 `0553c6bf28c0a3981ca5d47bd4755c1f7519c38780036813c8314b9b32768e0c`,
BUILD SUCCESSFUL 2m20s) and `peer.log` (SHA-256
`8b51899a8b89a662ad3e0903daa32b8bd1eb64ae72c37f7d6d679a87c966ff91`,
BUILD SUCCESSFUL 2m19s), both clean shutdowns. The late peer excluded ROOT
sequence 1; shared ROOT +.75/-.25 and a genuine one-degree JOINT material
carry reached both recipients. The JOINT physical applied delta was
(-.019805312156677246, 0, .0194627046585083). Five partial fractions,
repeated queries, unchanged physical state and camera ownership passed;
quiet incorporation returned zero. Reconnect excluded old carries and advanced
tracking generation 8 to 13 with a fresh nonce 1. Proxy report SHA-256
`e41cf1a4151070d0cc810b72be946015f90880848089fccb34511a4b27bae9a9`;
native-consumer snapshot SHA-256
`e08148978ab26374a5785900b43dab06a8025fb648cf5a90bbc69c3f48d5586a`;
contact-sheet PNG SHA-256
`f9921e5b51552ee48ee7912c99693a70f649349fc295a92c461d24452ca2d77b`.
The proxy reports socket close errors during shutdown, while both client
proofs complete. This is an injected delay, not a measured packet RTT; 200 ms
and remaining R6 acceptance are pending.

The same dedicated ROOT/JOINT, late-tracking and reconnect fixture passed at
nominal 0/100/200 ms RTT with `allow-flight=false` and clean host/peer
shutdown. The 0 ms host/peer logs are in
`../s32-adversary-private/production-pair-20261004-045627-393/` (SHA-256
`461f8cb3ab602337712dccc01345be2da0a9663992b139c260b4f1ef5c85e483` /
`527282f5a718fd7c26a2c3ef39fa28ba0b2cac4a5e74ca38a1bc396eb1b06638`,
BUILD SUCCESSFUL 2m27s/2m26s). The 200 ms logs are in
`../s32-adversary-private/production-pair-20261004-045312-506/` (SHA-256
`db12f51c6baee4e3bfc762aecf6086d8f72dec6ec61ac94f96d83d14cbcb7d5c` /
`852f91182ecc177bed9f71ae9c42ba17d5ff0271f59e8ce99807b6ee5bd96318`,
BUILD SUCCESSFUL 1m53s/1m52s). The 100 ms record above is the middle profile.
The 200 ms proxy counted two connections, 250369 bytes and at most 10 queued
chunks. Delays are injected socket delays, not measured packet RTT or a claim
about arbitrary loss.

ADVERSARY's final independent ordinary build passed 463/463 in
`../s32-adversary-final-ordinary-build.log` (SHA-256
`5ccb35e97443c1c03b54c8888d7892d0bfb1e2f820d0c6ec38b30090d49c5eff`,
BUILD SUCCESSFUL 3m21s); its complete ordinary client run passed in
`../s32-adversary-final-ordinary-client.log` (SHA-256
`9c1243c00384aa53ea7447f0fbea3bde26164795be4b8d6f5eead6eeb83b1a62`,
BUILD SUCCESSFUL 4m2s, clean shutdown). Positive actual deferred model draw
and fallback passed in `../s32-adversary-private/final-draw-fallback-positive.log`
(SHA-256 `9ea3b0d9ee08d92cf3ad4cfad9709ffaeff1d0122bb1209fd19415e6d14f7df3`,
BUILD SUCCESSFUL 1m45s), with 8000 actual/reference native-consumer CSV rows
(SHA-256 `398a12ec19db2bb89cdc8ec63c3bb92d5e5453c949d050d4b04511e1dc010c13`)
and contact-sheet PNG (SHA-256
`09b24b6658caab73abcd88c57732d68d00153cefc4ef0505b8dc8ed5811c1e4b`).
The dense history oracle passed separately in
`../s32-adversary-private/final-history-dense-isolated.log` (SHA-256
`07fb618065ebec526791d342b779130f8545360b940b4772137f0beefd5d11bd`,
BUILD SUCCESSFUL 1m34s): 64/65 per body, 512/513 global, 32768 combined,
identity ABA, immutable snapshots and TTL recovery to one retained record.

Two private source-copy mutations died at semantic assertions after positive
controls: replacing recipient codec base with the raw server base in
`../s32-adversary-private/mutation-raw-codec.log` (SHA-256
`b9a9805398d72195cc86bbe15df4ade2d72a99c8a4670ff25b03d9c0b37ef52b`,
BUILD FAILED 3m25s), and omitting the immediate gravity barrier in
`../s32-adversary-private/mutation-gravity-guard.log` (SHA-256
`e963d4b5530cd4d8d49e39757c6e75639b8370f9fe153494a45b62a1e5296d8f`,
BUILD FAILED 2m43s). Nominal production was unchanged. A bounded work/heap
smoke probe passed in `../s32-adversary-private/remote-work-heap.log` (SHA-256
`d10145a15b93953dd52cdb97e41edef608f8e5c3977e00487b9c4d98fb3a6c7e`,
BUILD SUCCESSFUL 2m17s): 8192 same-tick query pairs, zero repeated motion
preparations and 6849608 thread-allocated bytes, with identical physics and
query results. This is not a long retained-heap soak or an FPS/TPS benchmark.
R6 remains open for fanout/queue/watermark stress and an expanded mixed
dedicated lifecycle matrix.

Final test-tooling review removed a machine-specific JDK path from
`tools/s32_adversarial_pair.ps1` and a duplicate/misplaced OpenGL hook from
the adversarial Gradle init script. The affected public native-bundle selector
passed with explicit OpenGL in `../s32-adversary-private/tool-native-selector.log`
(SHA-256 `9bb284a6430300fd216fa12bccff0e75ba3f9be8d08547db083053294da5662c`,
BUILD SUCCESSFUL 1m29s, clean shutdown). Production was unchanged.

The independent receiver-capacity repetition passed in
`../s32-adversary-private/receiver-capacity-retention.log` (SHA-256
`ed04cfd9ef239fe1109f776670ca608a348bb5f9f308f28c8cc71f5967f6ad86`,
BUILD SUCCESSFUL 2m35s). It covered 8/9 pending packets per body, 16 carry
marks, 512/513 global pending packets, 4096/4097 replay watermarks, 40
full-capacity drain cycles and explicit unload. After four GC passes across
client ticks, 6699 observed owned objects had zero live weak references;
heap used fell from 398029328 to 277234960 bytes. These are bounded storage
and retention checks with synthetic receiver inputs, not a network soak or
FPS/TPS measurement. Evidence index SHA-256
`8a364ac368cf4778d2e7353780d3ee9476095d5ba97f1a811d00efb476133bc7`.

The first mixed-voluntary dedicated pair passed its positive contact, ROOT,
JOINT, interpolation and reconnect assertions, then crashed during host
disconnect. `../s32-adversary-private/production-pair-20261004-053110-237/host.log`
(SHA-256 `1eaaee788dc2975841e49256181da55f9e92d01f249dc28160a42d9267b25eb2`)
records `ConcurrentModificationException` from
`AnatomyClientNetworking.discardSupportMaterial` while removing presentation
contacts in `ENTITY_UNLOAD`. A second unchanged pair closed cleanly. The
implementer changed that map to a synchronized map so the whole `removeIf`
and its other operations use one lock; `compileClientJava` passed in
`../s32-contact-cleanup-compile.log` (SHA-256
`bae589ee5cef6ebb5e7538c5a58bd8664440adaef4fe4d3f130f1de124a43a9a`,
BUILD SUCCESSFUL 15s). The independent dedicated/lifecycle repetition is
pending, so R6 remains open.

The complete ordinary client suite after that change passed in
`../s32-client-after-close-fix.log` (SHA-256
`84e7eb7703737921aa46e201316f3150a68e7037c005a7ebca03fc5de686603d`,
BUILD SUCCESSFUL 7m10s), including its dedicated 0/100/200 ms soaks. This
does not reproduce the intermittent mixed-pair disconnect by itself.

ADVERSARY then repeated its original, unchanged mixed dedicated pair three
times after the synchronized-map change. Both host and peer passed every
positive ROOT/JOINT, voluntary-motion, late-tracking and reconnect assertion
and closed cleanly in `../s32-adversary-private/production-pair-20261004-054252-659/`
(host/peer SHA-256 `7c46f67b17a3b214e78b4b3563fd5c93aa15898a6fa55c813eeb4261cca6ddc0` /
`507dac7dcb138c21b3f7fa2ac982c2460a091c5351ff1c749b58d0a742bad14a`),
`production-pair-20261004-054549-634/`
(`b688b2b5e1e372629c1aeb41bbe78b0f911922c2d6ccc472894eea06715b7c31` /
`c54dab79b02cc4ee4abd60fbc781e9d1406991f3c77e766d9915f78d14381e1a`),
and `production-pair-20261004-054951-895/`
(`0d27057754881f5441eb8c62071f17baaae306e9e968f5c8db6ce6e46e78701e` /
`0e1e1a471650460f9fc2c9bbb605c2e9feede9777e034924ee757132991ed277`).
The previously observed disconnect exception did not recur in these 3/3 runs;
this cannot prove all thread schedules safe. Nine unchanged receiver lifecycle
lanes passed in `../s32-adversary-private/contact-sync-receiver-lifecycle.log`
(SHA-256 `52ce538f2d83ebff51cccc44b29745550e93a79a7ed7d296d8aae6653f385484`,
BUILD SUCCESSFUL 4m43s); five public catalog/dimension/unavailable/lifecycle/
cross-support oracles passed in `contact-sync-catalog-lifecycle.log` (SHA-256
`c7c084934f47949e02fcefbd9f29145aaa74b1492d97fd4a82b62bbb78e380cd`,
BUILD SUCCESSFUL 2m45s). Evidence index SHA-256
`8a77db2065a0b2d92393b0f2a2d504ae5e90a30487d9a1e6ee37d408429b5a25`.
R6 remains open for its wider multi-body, traffic-retention and dedicated
teleport/dimension/reorder matrix.

The post-repair `build runGameTest` passed 463/463 server GameTests in
`../s32-build-after-close-fix.log` (SHA-256
`cc67b8aa9366109b238f133b7efaa0f408e773c4650362bebe4bbfa00d96a4b`,
BUILD SUCCESSFUL 2m32s). `git diff --check` reported no whitespace errors.

Subsequent implementer review found two unguarded iterations over
`CausalRemoteResidual.RELATIONS.entrySet()` despite that map being wrapped by
`Collections.synchronizedMap`. Both support retirement and end-tick now copy
immutable key/value entries while holding the map monitor, then process the
snapshot outside it. `compileClientJava` passed in
`../s32-relation-snapshot-compile.log` (SHA-256
`ca43ae4cf26e311efadae3c0786c2e14f29866b1d0fa74aa7edae6682fa7ac1b`,
BUILD SUCCESSFUL 52s); candidate source SHA-256
`cd65cc6cadec2801e690b6ea198010f40491538d009a93ca0fdc0c6715e077a5`.
The dedicated/fanout and ordinary client evidence above predates this last
change; its affected regression pass is pending.

The complete ordinary client suite on the synchronized relation-snapshot
candidate passed in `../s32-client-after-snapshot-fix.log` (SHA-256
`79cbebf6d1ff08d71e3ac492428c75fe57d64b90924e705b43a03596a5bed9da`,
BUILD SUCCESSFUL 4m15s), including its dedicated 0/100/200 ms soaks. The
independent expanded R6 matrix and final no-change review remain pending.

The matching `build runGameTest` passed all 463 required server GameTests in
`../s32-build-after-snapshot-fix.log` (SHA-256
`e98b846d7c2477d9f9041c71af8f97cfbf27bb364a0df832b26f66a66d3d1af3`,
BUILD SUCCESSFUL 2m19s).

The independent S32 fanout fixture passed on the exact synchronized-snapshot
class hash `5e43aa5dea0a88bb9737d2a34b39903ece8d6870234a16d8a4cbbd083c49e80b`
at both clients. Sixteen supported bodies, sixteen supports, two recipients
and 128 physical ROOT rounds produced 4096 actual attested native packet
occurrences. Five partial samples per round, unchanged physical state,
bounded relation/watermark storage, zero pending packets at checkpoints and
zero owned receiver storage after explicit retirement passed. In roughly one
minute of ordinary dedicated traffic, four post-GC heap samples were
331708840/331597712/331592760/331597136 bytes at the host and
241012304/241109688/241120544/241166064 at the peer. Logs:
`../s32-adversary-private/production-fanout-20261004-061646-966/host.log`
(SHA-256 `286661179a1327e89d5a90299f061326b069dcc99c40088afb38a7dbb2bf5ee5`,
BUILD SUCCESSFUL 3m9s) and `peer.log` (SHA-256
`b4bbf213e5e59ec1dcb50464928e85c8bdc69551704ef8fa0d310419825e41bf`,
BUILD SUCCESSFUL 3m8s), both clean shutdown. This is bounded real-traffic
retention evidence, not an arbitrary-duration heap soak or FPS/TPS benchmark.

The dedicated observer-transition fixture passed on the same class: actual
observer teleport out/back and Nether/Overworld round-trip retired old body
relations, increased tracking generations 8→12→16, restarted nonce 1, and
recovered positive ROOT correction. Two carries in one native occurrence,
an immediate entity-teleport visual barrier, reversed bundle parts and a
stale tracking attestation all passed their fail-closed/recovery assertions.
The separate entity-teleport packet was injected at an aligned native codec
base; it was not a naturally emitted Cow teleport. Logs:
`../s32-adversary-private/production-transitions-20261004-063225-246/host.log`
(SHA-256 `b62fa3a104bfba40b20ca9c84e7fd0f4c736348ee50b5b7184219787bd6314ab`,
BUILD SUCCESSFUL 2m23s) and `peer.log` (SHA-256
`520f1461c037af7a094bcb9d2fd1d2244bc2af4d73acf9b09616db06de6ec87e`,
BUILD SUCCESSFUL 2m21s), both clean shutdown. Evidence index SHA-256
`da83e4a76028a74643042827019f221464595a2dda652be25307a04dc316882e`.
The final unchanged-production review of G4 was still pending at this point.

The final independent S32 R1–R6/G4.6 review compared the complete current
production-source manifest with the fanout/transition candidate and found
zero added, removed or changed production files. It verified the current
ordinary logs above by SHA-256, reconciled the earlier positive/negative,
mutation, capacity, work and native-consumer visual evidence, and found no
unresolved classified RED or missing evidence within S32's scope. The
native-consumer contact sheet was re-inspected across 25 tick/partial samples
and 8000 vertices; it is geometry capture rather than a framebuffer screenshot.
Review index `../s32-adversary-private/s32-final-zero-change-review.json`
(SHA-256 `0c34369e716357b71770828093fc10e3b5faac0d9d0b3ecb8ddfb95a3b9d619b`).
S32/G4.6 is closed within the documented test limits. Integrated NFR-014
benchmarking and human/pack/external-model acceptance remain in G9.

## S33 / G5.1 material-face placement — closure evidence (2026-10-04)

On the uncommitted `chatgpt-editing` candidate, `../s33-build-final.log`
(SHA-256 `c4c62dc0079f7c941ac7d1c36e7ce4b4eaf05d39dd84cd5e7fafa9929a8d3822`)
completed `build` with **463/463** required ordinary GameTests and
`BUILD SUCCESSFUL`. A separate final ordinary `runGameTest` also passed
463/463 (`../s33-ordinary-run-final.log`, SHA-256
`6653ed6ff93ca4be7bce1b154e2585b5c2c92bcb2eab2a08ee35ada543d533de`).
The S33-only prepared server run passed 2/2 with real `Player.interactOn`
six-face boat placement, physical AABB/owner gravity assertions, egg and raft
successes, same-species vanilla PASS, and permission, reach, block, occupied
volume, narrow-face and zombie-egg/chicken rejection controls
(`../s33-isolated-run-7.log`, SHA-256
`90cda86fbf7b59db3b94488aefbb02ab88ef3fdde758b73da82b13dde6985ad0`).
The ordinary client `runClientGameTest` completed successfully in
`../s33-client-run.log` (SHA-256
`0d9467c86fc8e5d92480975b2a9953b955b3e6fc66612f47afd981cb75eb995f`);
that run preceded the server-side exceptional-insertion cleanup.

The **full prepared suite is red in the older S09 A9 temporal response** at
large world coordinates. Disabling S33's fixture did not remove the A9 failure
(`../s33-prepared-skip-run-2.log`, SHA-256
`aa8ea35b33546ebb4c7be88e52d3f78065ba5e79a3a2532633a844a32ae73e79`).
A new GameTest world also placed the test at large coordinates and failed A9
in the full cow manifold after the selected causal piece completed
(`../s33-prepared-fresh-run.log`, SHA-256
`f329da71f5ba457b1d04b9efa953d7c5f57ef729e8c507225655173fa8f058b4`).
Neither red is a S33 placement assertion; they remain a separate temporal
numeric/work-budget reopening. No full-prepared green or G5 close is claimed.
The independent S33 holdout battery, final unchanged-source review and local
commit are still pending at this record point.

Minecraft 26.2's native `ServerboundInteractPacket` uses `Vec3.LP_STREAM_CODEC`;
independent client/server proof found an accepted face point rounding just
outside material, leaving the ordinary pig egg untouched. The production
raycast now retries only an exact miss within the `LpVec3` quantization bound.
After that repair, `../s33-packet-isolated.log` (SHA-256
`f5c8d0c0f3d75664c51dbd9fa86bf626f7b58268c43525fc2187be3d1960cad6`)
passed the ordinary **463/463** GameTests, BUILD SUCCESSFUL. The prepared S33
selector with the exported catalog passed **2/2** (`../s33-packet-prepared.log`,
SHA-256 `86ec82478b0155e4b066558efab9aebf06936f19bb3bbf03f5896570f6da8d90`),
including a +0.00012 outward endpoint accepted and a +0.001 miss rejected
without insertion/consumption. These are IMPLEMENTER runs; the native packet
rerun remains an independent open check at this point.

The first short-ray continuation failed a second independent native packet
proof at a near-parallel viewing angle: the exact control placed, while the
quantized packet retained the egg and placed no pig. This was a production
RED (`../s33-adversary-private/native-codec-grazing-stable.log`), not a
completed S33 gate. The revised implementation searches only the codec error
cell and checks eye visibility plus one unambiguous material face. The
IMPLEMENTER prepared selector then passed **2/2**, including normal and
grazing precision recoveries and a farther miss rejection
(`../s33-cell-prepared.log`, SHA-256
`6342a909c6a2041c2a2ab787ba7444b99a84c0e89321e27f30672e1c0f577cdc`,
BUILD SUCCESSFUL). The adversary's unchanged native grazing packet rerun is
still pending at this point.

The final precision-cell candidate passed a full local `build` with **463/463**
ordinary GameTests (`../s33-cell-build.log`, SHA-256
`cb25bf0b55184478f18046910556956fe269e6873f6c806c962e2066f0b7b745`,
BUILD SUCCESSFUL). Independent unchanged native oracles also passed: ordinary
packet `../s33-adversary-private/cell-S33PrivatePlacementNetworkProof.log`
(SHA-256 `2abf0a53f724f221231bc1dcf03473943b182da7a6197474fc886b3c0e9a0515`),
grazing packet `../s33-adversary-private/cell-S33PrivateGrazingPacketProof.log`
(SHA-256 `65f1e54eaca9b7f0fb7d9125cb99cac72804fb9a4c66e9569808c4111f49a289`),
and border/edge/reach/block/entity rejection matrix
`../s33-adversary-private/cell-rejections.log` (SHA-256
`a371d9f2c74e0091def1c334a3ed4d25893944ebf6454557862351df3d3ee675`,
2/2). Both native positives inserted exactly one supported body/anchor and
consumed one survival egg; all three processes shut down cleanly. The ordinary
client rerun and final zero-change S33 review are pending at this record point.

The ordinary client rerun also completed `BUILD SUCCESSFUL` with clean
integrated-world shutdown (`../s33-cell-client.log`, SHA-256
`30c98dc80f5eb906a76ed6cad194cbb6abf300a4e021ea64647dc3d1a8d90294`).
It exercised the existing client battery on the final precision-cell source;
the two native packet positives above remain the direct client/server
placement evidence. Final S33 zero-change review is still in progress.

S33/G5.1's final integration blocker was the older S09 A9 temporal RED,
repaired in S34. On the combined source, the prepared suite passed **2/2**
(`../s34-axis-prepared.log`) and ordinary build passed **465/465**
(`../s34-axis-build.log`); the corrected-source client passed as recorded in
the S34 section below. The independent final cross-sprint review found no
new placement defect and zero production changes, with manifest
`../s34-adversary-private/final-cross-sprint-evidence.json` (SHA-256
`bdee2f672c28fc5071fa52c095cc6f4fe3bf272edff9a0b77d3a20e4566dd8fe`).
S33's native packet and rejection oracles remained fingerprint-identical to
their earlier GREEN runs; they were not relabelled as newly rerun. The
independent verdict is GREEN for S33 placement and its required integrated
regression. Later G5 special-body/legacy and G9 acceptance remain open.

## S34 / G4 A9 large-coordinate temporal reopening — candidate evidence (2026-10-04)

The required prepared A9 proof was RED before S34 on both a run omitting S33
and a fresh GameTest world, as recorded above. Temporary opt-in traces over
the same certified cow motion showed selected direct CCD work varying from
432 evaluations near origin to 87 after a common large translation in one
sample, while the full 10-piece manifold exhausted 256. Other samples had a
selected piece within budget but exhausted after contact when later clear
pieces joined. Logs and classifications are recorded in the S34 sprint file;
none of these RED runs is counted as a pass.

The candidate computes `ConvexBox` SAT projections relative to one nearby
body-center origin. Its diagnostic prepared run completed the full manifold
in 170/171/171 evaluations at origin and two previously failing translations
(`../s34-relative-prepared.log`, SHA-256
`d69923e000b1e69aa74b1f76eb55489e657289be586c8e1e3d79337ee7bc0ce5`,
BUILD SUCCESSFUL). After removing instrumentation, the full prepared suite
with S33 enabled passed **2/2** and checked six translated real cow manifolds
before aggregation (`../s34-full-prepared-candidate.log`, SHA-256
`6d320c4b15fc5a846986b215cff1354ecb8d5c47b3dae0e7e1a2556c7bd43917`,
BUILD SUCCESSFUL). The existing 32-event/256-evaluation budget and skin were
unchanged. Ordinary/client and independent verification remain pending.

The same candidate then passed local `build` with **463/463** ordinary
GameTests (`../s34-relative-build.log`, SHA-256
`f21d7da266da66d857cdbcbb0f0bfd38caea99a5560d8799174feec43a70efe0`,
BUILD SUCCESSFUL). Client and adversarial verification remain in progress.

The ordinary `runClientGameTest` also completed `BUILD SUCCESSFUL` on the
relative-SAT candidate (`../s34-relative-client.log`, SHA-256
`33ae1539daeeee00bb396a96c6a758343440d85400b3e0d762b74a5c2994e397`).
This is client regression evidence, not a substitute for the independent
prepared-catalog and native packet proofs.

The adversary's independent S34 run invalidated closure of this candidate:
`../s34-adversary-private/candidate-sat-prepared.log` (SHA-256
`8697c522b02deb48b11090c009c4e034cc46004202d0536f2d1deefc3925ae19`)
passed its SAT and translation holdouts, but the required real prepared A9
GameTest exhausted the 256-evaluation budget for **all six** translated cow
manifolds, including the near-origin case. The near-origin input reached two
`root/head/cube_0` contacts at t≈0.2839344 before exhaustion; some large-world
translations exhausted before any contact. The process reported one required
failure, `BUILD FAILED`, and shut down cleanly. This RED is a G4/FR-048–049
acceptance blocker, and therefore S33 remains unclosed. The prior green logs
remain valid only for their sampled GameTest inputs.

The revised S34 candidate retains the original coordinate ulp when a convex
box is translated, uses the same bounded numeric band for CCD and temporal
screens, and charges a post-contact clear certificate before an inconclusive
CCD probe. The base `SKIN`, `SKIN/4` numeric cap, 32-event and 256-evaluation
limits are unchanged; the ulp factor increased from 8 to 13 and is subject to
independent tangent/clear review. With a six-level chronological fallback,
the cleaned prepared suite passed **2/2** on two fixed real cow yaw paths and
all six complete-manifold translations per path: `../s34-final-prepared-depth6.log`,
SHA-256 `133dc6ddab5c2ff8af8f4afd9858fcc89830287dacd98c045b61a6c3efa41198`,
`BUILD SUCCESSFUL`. The same source passed the ordinary `build` with
**463/463** required GameTests: `../s34-final-build-depth6.log`, SHA-256
`50ab58e75f43eebb76ef24df33a4d06b96a2230e4e9651c45b3d654f5d7ea153`.
The ordinary `runClientGameTest --offline` also completed `BUILD SUCCESSFUL`
on this candidate (`../s34-final-client.log`, SHA-256
`562ca9b6f3b781265aebc1a8fb9595072a4609b292c808ce64c2ee754e003bbb`).
The historic depth-eight fallback remained RED on one near-origin replay
(`../s34-final-prepared.log`, SHA-256
`e980289767e7657e5568fc2fb745a38623fd76857b71d6805255e08fc9f1bcdc`)
and is not counted as success. Exact code hashes and work analysis are in the
S34 sprint file. Independent adversarial results remain pending;
the candidate is not yet G4 or S33 closure.

The earlier adversarial S34 GREEN was pinned to a candidate with only two
fixed A9 yaws. Restoring the original live yaw exposed another true 256-query
exhaustion at 0.5 degrees near the vanilla border. A tighter root-only
rotation-axis speed bound repaired that work cost, but the adversary's private
nonidentity-root fixture exposed an unsafe coordinate-frame error in its first
revision: declared speed 0.117179 versus observed unit-interval vertex secant
5.657297; an actual interior hit was reported CLEAR. That RED is preserved in
`../s34-adversary-private/rigid-root-bound.log` (SHA-256
`4dc27efc1344f91b3cdf79b4d2fed79823c9bfe4e306fb2e7713c18f94547266`).
The revised bound expresses the relative rotation axis in the initial root's
local frame before measuring fixed downstream vertices. Its first private
reruns passed: original holdout in `rigid-root-fixed-campaign.log` (SHA-256
`1c3f94d05ef0936693e2ffa45a384a7cb2af19c6afe686125f5eb2478f5a7661`)
and expanded holdout in `rigid-root-expanded.log` (SHA-256
`0669f9c97ed4707255c0907d82829f58991b92bd61cd5636564613b84f920b22`),
each `BUILD SUCCESSFUL`. These are bounded independent checks, not yet the
final adversarial S34 verdict.

On the corrected source, the full prepared suite passed **2/2**, including
four real cow yaw cases and six complete-manifold translations per case:
`../s34-axis-prepared.log` (SHA-256
`43ad1a00aaf68617220210ff990a3c2fde08b6521b65616c25b87b000f5dd01a`,
`BUILD SUCCESSFUL`). The ordinary `build` passed **465/465** required GameTests,
including the permanent nonidentity-root tunneling and vertical-offset bound
regressions: `../s34-axis-build.log` (SHA-256
`c82b2888ba7bfecdaa224be347885756283f5593e3d4b968a8b1d6afc3088318`,
`BUILD SUCCESSFUL`). Production source SHA-256: `HierarchyMotion.java`
`b26d21335a35c298fbfdfb2f491fddde9af8e6bd65bb1b1e563c8dffc38cc454`,
`ConvexBox.java` `591caca1f655d8d3fb785d9721758f880bf12491b4b8a461c94ffc6d82c05beb`,
`ConservativeSweep.java` `86cf8d9e148032aa7888f6870761bd35bac0a492efe4daa4cb4646c701ab20ca`,
`TemporalResponse.java` `b93da6b1404aeeb27b6877e893e1ab87686c303fa249bfe252ac9cb438cdc12e`.
The corrected-source ordinary `runClientGameTest --offline` also completed
`BUILD SUCCESSFUL` with clean integrated-world shutdown in
`../s34-axis-client.log` (SHA-256
`0cca144cba9bb0624a8c009e6199c1a6ecfb64823c3a6759a4194ba5ef87639f`).
This uses the same `HierarchyMotion.java` hash above. The adversary's
`rigid-root-fixed-campaign.log` subsequently passed **6/6** across its
unchanged ROOT, SAT, translation, window, prepared and sentinel selectors;
`rigid-root-expanded.log` passed **2/2** with 48 nonidentity-root TRS cases
and 12,288 observed vertex secants. Its affected-surface zero-change verdict
is GREEN in the S34 sprint record. The independent final cross-sprint S34/S33
review found zero new production defects or evidence gaps on the same source
hashes and verified the final ordinary, prepared and client logs above. Its
manifest is `../s34-adversary-private/final-cross-sprint-evidence.json`
(SHA-256 `bdee2f672c28fc5071fa52c095cc6f4fe3bf272edff9a0b77d3a20e4566dd8fe`).
The historical S33/A9 and unsafe ROOT REDs remain classified in their sprint
records; S34 repair and S33 placement meet their tested closure criteria.

S35/G5.2 item slice closed locally: the real prepared
`ItemEntity` proof passed **2/2** including material contact, separate ROOT
translation/yaw carry, canonical dry-ground friction, vanilla age/pickup delay,
supported and released pickup, post-release gravity/fall and near-expiry
despawn. `../s35-release-prepared.log` is `BUILD SUCCESSFUL`, SHA-256
`df679cfd0e091424cbd925728e811fbc6d73ff0739b595d5b62dbcf79c8b3165`.
The same candidate's ordinary `build` passed **465/465**,
`../s35-final-ordinary-build.log`, SHA-256
`84072e55de24ac020029dc7f547f08999b841034a9a4a372ca5d81f6b745c859`.
Proof SHA-256:
`7467b5ccef2d8ce6c13207ef0be4f849c8831ec6dfe99b31bd50a8c4783484de`.
This adds test wiring and evidence only; S35 has no production change. Six
independent ADVERSARY real-server selectors then passed **2/2 each** for
lifecycle/ROOT, friction/merge/release, policy/ratio, water versus a vanilla
control, wall obstruction and an actual `ItemEntity` 256-evaluation budget
exhaustion. Their final manifest is
`../s35-adversary-private/final-evidence.json`, SHA-256
`9e2f8d3d425b9596e3f17d53b3e85d1af348a1f4e835b82229fc5db86e4a20eb`.
The independent final review found no production diff or further candidate
change. This closes only FR-007/066 for items; boats, rails, falling blocks,
legacy retirement and G9 acceptance remain open.

S36/G5.3 boat/raft slice closed locally after the final independent
zero-change review. The IMPLEMENTER's actual prepared server proof covers
ordinary boat, chest boat, bamboo raft and chest raft, canonical-only material
contact/friction, retained ROOT translation and yaw, ratio-rejected native
interaction/push, wall obstruction without replay and support release.
`../s36-prepared-push.log` passed **465/465**, SHA-256
`919375c726a496679df489664e96fb9d34a7b9b1b171f5611f0e084e9eaec1c2`.
The real local-controller client/server GameTest rows an occupied oak boat on
the prepared support, takes one ROOT carry, then enters source water, retires
material contact and exits onto dry stone with native `ON_LAND` status.
`../s36-client-final-candidate.log` passed, SHA-256
`04e6ca71348f393a22c61237dc8c019c77285de5f49a6fc6c8a12658da8afb23`.
The same production candidate passed ordinary **465/465** in
`../s36-final-ordinary-build.log`, SHA-256
`6d9fdf12c077571fd20fff28e9d3233e842e333dd41c39c01ac2de0c0bd5f43e`,
and the full client suite in `../s36-final-full-client.log`, SHA-256
`0d12e0f70c81945c0eaf67340a5908f239ecda62321e68593d495bbe2d6e8eab`.

ADVERSARY RED2 exposed an integer-loop overflow in the first vehicle-packet
fluid probe at a finite out-of-world coordinate. The bounded replacement was
retested by the unchanged original oracle **2/2 GREEN**. Independent server
oracles for the four hulls/native seats and contents, overlapping water/contact
retirement, and packet projection/controls also passed **2/2 each**. The
ADVERSARY reran the public real-client proof unchanged in its own build and
found no further defect in the final zero-change review. Its manifest is
`../s36-adversary-private/final-evidence.json`, SHA-256
`92de7d2e4c551e1500056f817d35657eb9241b747323793f09ce29f3b61f97f8`.
Final production SHA-256:
`AnatomyMovement.java` `ea50e54ffdb8ea0226dc82e3fb4ba246c81ba4a2b3fabc8724fa4449a1f1e433`;
`PlatformConnectionMixin.java` `12adf0c1661ebb0288ce323d4347018623d13a7bd59282c5c2dd8a024c441f6c`.
The HOT_NETWORK projection inspects at most 64 local fluid cells; the
independent 10,000-query check observed zero extra geometry/joint evaluations.
This evidence closes FR-007/064 for boats/rafts only. Minecarts, falling
blocks, legacy removal, global G9 benchmark and human pack/gameplay QA remain
open.

S37/G5.4 ordinary off-rail minecart slice closed locally with no production
change. The prepared real-server proof covers ordinary, chest and furnace
minecarts, exact canonical contact without `PlatformState`, native off-track
ticks/chest contents, one occupied ROOT translation and yaw, ratio-rejected
native push, block-wall clipping without replay, real rail entry/travel/exit,
and fresh contact on a second prepared support. The fixture reports installed
26.2 `OldMinecartBehavior`; it does not certify experimental
`MINECART_IMPROVEMENTS`. `../s37-prepared-candidate.log` passed **2/2**,
SHA-256 `36596c421453e66c0c6d8e484fddcb7832c7e6fad85da211d728cc106bbed53c`.
The ordinary `build` passed **465/465** in `../s37-ordinary-candidate.log`,
SHA-256 `b7925714aeb6f88901762d86fb41f4be1c6e3736e945cf807090b0c9c56f56cb`.
Public proof source SHA-256:
`f47f22af7abe8ec98d7f06fcda085d2921938685833a2487c2498ee3637ef377`.

ADVERSARY independent real-server boundary and native-rejection oracles passed
**2/2 each**. They add an occupied fixed-yaw cart with exact relative-seat
and container checks, stationary actual rail detection after support motion,
fresh reacquisition, unchanged nearby nonparticipant, and ratio/disabled-policy
native clipping/push controls. The public prepared selector was independently
repeated unchanged, **2/2**. Thirty thousand current-support queries across
three variants added zero geometry/joint evaluations; each yaw captured one
ROOT. A deterministic point/seat comparison was inspected. Final independent
review found no further concrete defect or candidate change. Manifest:
`../s37-adversary-private/final-evidence.json`, SHA-256
`ad37cf7f8802377d68d2411eea6361e9af51b4843275e71136fa1ec81071a158`.
No client runtime path changed, so no new client execution is claimed here.
This closes only the standard minecart behavior slice; experimental movement,
falling blocks, legacy removal, global G9 benchmark and human QA remain open.

S38/G5.5 falling-block candidate: the prepared real-server proof covers sand,
concrete powder and anvil entities with canonical-only material contact, ROOT
translation/yaw, wall and ratio rejection, 650 held ticks, native 600→601
timeout/drop, post-release placement, source/flowing-water hardening and
initial/later native anvil impacts without passive repeat. The only production
change suspends the installed 26.2 current-cell water predicate while eligible
material support holds, then restores vanilla behavior upon retirement.
Production mixin SHA-256:
`04f372826979c01d0e83ff29f6177634463e7b3bb6a219d2c7fc5abe6a186b0c`;
public S38 proof SHA-256:
`2b0d2f92ea97439631078ffac92872a3046a78fb79d90edc29045d03eb6f6803`.
The final targeted selector passed **2/2** in `../s38-targeted-after-fixtures.log`
(SHA-256 `814ced506400728874cba3cfb4a5ea7d061ed96e9e0be6104f4dc81d49bd9703`).
The complete prepared sequence passed **2/2** with all 21 scenarios on
distinct consecutive level ticks in `../s38-prepared-scheduled-third.log`
(SHA-256 `08fbc81962a1463b30fd7d9557ef97f153bb19ec2588321efbd880301544132d`).
Final ordinary `build` passed **465/465** in `../s38-build-final.log`
(SHA-256 `b432dd538c07b3215d48a0e81d77670846bcd66284ac8c3cb6d08ce0597e0346`),
and the full real-client suite passed in `../s38-client-final.log`
(SHA-256 `0d0dec9b67db802c89f18c5fc2e39967d88f1bc64e2c03e2de8f4b39aaef686e`).

ADVERSARY independently passed three **2/2** real-server holdouts for actual
anvil damage attempts, current-cell source/flowing-water retirement and native
age/drop branches; its own full-client rerun passed after one preserved S29
reload-predicate timeout on an earlier run. Its unchanged water holdout killed
the pre-fix mixin mutation. The final independent manifest is
`../s38-adversary-private/final-evidence.json`, SHA-256
`410256f9ac076df7aebea8439b443551b41aaec620a6fd168557777429a46413`.
An earlier RED in the full prepared composition came from one-tick scenario
scheduling and cross-tick fixture metrics; the accepted test-only scheduler
runs every scenario on its own real tick in one prepared session. S35 item
cadence, S36 obstruction distance and S36/S37 chest cleanup were also repaired
as fixtures without changing production budgets or physics. ADVERSARY's final
complete-tree zero-change review repeated the integrated prepared sequence
**2/2**, all 21 scenarios on distinct ticks, in
`../s38-adversary-private/zero-review-composite.log` (SHA-256
`abf6a23558bec89fb2d3e6f94bb77063be65454657f8808aad436aa0ff5a0a35`);
manifest `../s38-adversary-private/zero-review-evidence.json` (SHA-256
`a570f057461828862fcf602b5a335aaf6dddc1cdbb7274bb504913d79e386262`).
S38 is closed locally. This closes neither experimental minecart
behavior nor whole G5, legacy retirement, integrated G9 or human pack QA.

S39/G5.6 passive accounting and landing candidate: the real dedicated-client
proof runs player and controlled-boat RTT 0/100/200 ms phases, then keeps a
client-controlled player idle through horizontal, rising and descending
certified cow ROOT intervals. The native server movement handler receives
accepted packets while WALK/SPRINT/SWIM/FALL stats, exhaustion and fall
distance stay unchanged; a following voluntary input pulse through the same
handler increases WALK. Two earlier N2 attempts failed the pre-existing 0.8
relative-excursion bound before reaching S39 because the first player contact
arrived before ordinary post-teleport settling. Waiting 40 real ticks before
measurement and reconfirming contact preserved the same bound; the complete
N2+S39 run passed in `../s39-client-accounting-settled-first.log`, SHA-256
`52fb8804c3e88d2d7248fe9a9a1016cbfc21017e2c4b90d351ba32670abc8822`.

The prepared landing proof uses an actual Growth III pig fall onto canonical
living material with a non-AIR native ground argument: vanilla self damage
occurs, but Growth terrain impact does not. After release, another actual fall
onto stone produces both native self damage and exactly one Growth impact.
The post-fix integrated prepared suite passed **2/2**, all 22 scenarios on
distinct ticks, in `../s39-prepared-full-postfix.log`, SHA-256
`7e6dcedb82baa5bafb2290ccf3e560f547c189e36ff3e047dd9c527385b27a08`.

An independent synchronized-wall client/server holdout exposed a production
RED: the server rejected an obstructed ROOT carry, while client prediction
accepted a partial block clip and the native packet charged 43 cm of FLY
without voluntary input. `AnatomyMovement.carry` now rejects a clipped delta
before changing position or publishing transport. Its SHA-256 is
`d2467b72accf48002cb2e9a273de8e544b0319035e7b2db78719ca9379790928`.
The unchanged independent obstruction holdout passed after the fix, with zero
client/server movement and zero FLY; log
`../s39-adversary-private/packet-obstruction-postfix-repeat.log`, SHA-256
`a787368261244d6d6d658950e71f244059c2e8f0bfb71b658a5acc69158fd6f9`.
A public ordinary GameTest compares a free carry with a real wall's partial
clip and repeated rejected carry. The final ordinary build passed **466/466**
in `../s39-build-postfix.log`, SHA-256
`962f36274233d3e5290c816482730728943f60229a33adfc30a713f0cc325b89`.
The complete general client suite passed in `../s39-client-full-postfix.log`,
SHA-256 `1b10d352dc73ebfee89ee4a8fdc0fa0e855679fd3db2c33ae8c1f6d51cd1480b`.
The independent final review kept its original packet and landing oracles
unchanged. The packet obstruction proof passed with zero server/client carry
and zero FLY; `../s39-adversary-private/landing-postfix.log` passed **2/2**
(SHA-256 `138ec31fea7a3ca01d6e2e90817cb1c782d3ea3647bf6b3b2801c1c858148976`).
Its auxiliary release/locality lane passed in
`../s39-adversary-private/release-locality-separated.log` (SHA-256
`7cae1cf01518788a8ba7c91fc87149fb74a2ac079f694f52cf28043cc08b130c`):
10,000 quiet carries added no geometry/joint evaluations or displacement,
10,000 post-release queries recreated no transport, and a later native own
movement counted FLY +31 without passive debt. The prior Growth guard-removal
mutation remained RED. Final independent inventory
`../s39-adversary-private/review-evidence.json` has SHA-256
`1b1af34edf0d8ebbf56cdb511de0cd75f1ee66c3d1ce2743152a93662ef2ad4f`.
No production or IMPLEMENTER tests changed in that review; `git diff --check`
passed. S39/G5.6 is closed locally for FR-061/062. Whole G5, G9 benchmarks
and final human pack QA remain open.

S40/G5.7 IMPLEMENTER candidate: a prepared native `Player.move` regression
first proved that `jumpFromGround` left its old material contact live before
the next own movement; after the native jump-release hook, the focal test
passed. The edge helper now reads one accepted support snapshot and caps work
at 128 coarse plus eight refinement sweeps over local pieces. Ordinary build
passed **466/466** in `../s40-build-candidate-final.log` (SHA-256
`e83d82da73bd232c2f5b8a7810e50e2f7141d558e9759eb6b007163fe46ed6e1`).
The complete prepared sequence passed **2/2**, all 23 scenarios including
`Player.move` edge tests in down/up/north test gravity frames, in
`../s40-build-and-prepared-candidate.log` (SHA-256
`b5fc4f00475d92d0389323555c5ac42a959ac88500e6bf5b7b349bfccbfdcda5`).
Separate original-catalog dedicated-client runs used actual local sneak/jump
keys and native packets. Sneak retained the measured convex boundary, then
left it on release, with both peers' contact and transport converging:
`../s40-client-sneak-close.log` (SHA-256
`8ef01ea60dca00c22b9bab2de3020680c5c49bb968788e7e7da65628c386ee17`).
After native jump packets retired the server relation, a later ROOT produced
no carry or extra transport; the exact local jump impulse matched the same
native input on stone, `(0, 0.4482024610042572, 0)`:
`../s40-client-jump-final.log` (SHA-256
`240612306851705714d559ac176fd831644bba34f1360c5bc9789a834bef457d`).
The full general client suite passed in
`../s40-client-full-candidate-final.log` (SHA-256
`1d5dd3b46eb7273addee637a8f30420548209277c8b4a7c380c11daa2192467f`).
These runs do not close S40: independent holdouts, ownership controls and a
complete zero-change review are pending. Whole G5/G9 and final human pack QA
remain open.

S40/G5.7 final independent ADVERSARY pass: native real-input and genuine
post-landing reacquisition passed in `../s40-adversary-private/native-input-snapshot.log`
and `../s40-adversary-private/native-reacquire-grounded.log`; the immediate
jump-to-ROOT span passed and removing the production jump-return hook in an
isolated overlay made its oracle RED as expected. Six prepared gravity frames
passed through native `Player.move`. A work probe measured 128,700 local-piece
sweeps across 100 long edge queries, below its 137,000 bound, with no additional
geometry or joint preparation and no physical-state mutation. The measured
footprint was also inspected as a deterministic contact sheet. Exact logs,
snapshot parameters/hashes and fixture-failure classifications are recorded
in `../s40-adversary-private/evidence.json` and the ADVERSARY section of
`docs/sprints/S40-g5-sneak-jump-boundary.md`. The adversary reported no
remaining S40 product RED and made no production or IMPLEMENTER-test changes.

On the final unchanged tree, `build runGameTest` passed **466/466** ordinary
server tests in `../s40-zero-ordinary-final.log` (SHA-256
`57fd67101417be230e35a6c45661f86a1f33e167cfd2678f72818b4d83a30c7b`),
and the prepared composition passed **2/2** with 23 scenarios in
`../s40-zero-build-prepared-final.log` (SHA-256
`633aafbd6c785cc27236ed4c34c72b07071baafb56c446a3049ee4e5cab0ae5c`).
The final native-input sneak and jump invocations added explicit client/server
position-convergence assertions and passed in
`../s40-client-sneak-assert-final.log` (SHA-256
`9e68f1456d13225c554f86754f73bcd7a6a9b3bb3d4710133779e7773c436d86`)
and `../s40-client-jump-assert-final.log` (SHA-256
`f34a751e71f7c86f0703a36a8e6018c04899e374cfdc952858f584f2267898db`).
The full client suite passed in `../s40-zero-client-rerun.log` (SHA-256
`cbb7245c0595f4382c102a47d11947f6d69bb46f3861c5e08fe83368e5a11f20`).
An earlier final-client invocation timed out at an S29 catalog-certificate
wait; the complete suite had passed before it and passed afterward on the
unchanged source. Its cause was not established, so that invocation is not
counted as acceptance evidence. An initial prepared invocation used the wrong
Gradle selector; the corrected isolated prepared run above passed. `git diff
--check` passed. S40/G5.7 is closed locally for FR-054/063; whole G5, G9,
external-mod gameplay and human pack QA remain open.

S41/G5.8 IMPLEMENTER candidate uses a genuine 26.2 feature-enabled client/
integrated-server world and real `NewMinecartBehavior` on both peers. The local
rider remains on a server-authoritative cart. An original prepared cow carries
that occupied cart through one ROOT translation and one ROOT yaw interval,
each with one transport-sequence advance; physical rail entry retires contact,
native experimental interpolation reaches the client, physical rail exit
permits a fresh cow contact and later ROOT carry. A separate unoccupied cart
retains native track physics and observer interpolation. Ratio-rejected and
profile-policy-disabled pairs retain vanilla push, while a real-wall control
shows no delayed carry debt in the same feature world. The fresh contact's
later ROOT moves the cart exactly 0.2 blocks and advances transport once. The
current focal log `../s41-experimental-exact-reacquire.log` has SHA-256
`E7D52C40E6527E20442D2F97C6788426E9300FAF3366024F4A676EC83414E079`.
The ordinary feature-off/`OldMinecartBehavior` client/server control passed
on the final test source in `../s41-ordinary-final-tree.log` (SHA-256
`89325AAE46536D40E6E93F784CC213DA902E4105B8B8852D10B2F4C00DF18F67`).
Ordinary `build runGameTest` passed **466/466** in `../s41-ordinary-full.log`
(SHA-256 `C5222CBBF556A9EEE562E58B0DA09A6851D7F08420B2324C57B24768C2AD992C`);
the prepared suite passed **2/2** with 23 scenarios in
`../s41-prepared-full.log` (SHA-256
`E771FED48F128850240EF09E4D89CFE4E28883690B51CA4C4D3C85E950F9AC23`);
and the full general client suite passed in `../s41-client-full.log`
(SHA-256 `DBE8568AA58868C3DE2490BF0DEE82DEB9AFFE61914C0792059644A37A259089`).
No product source changed for this candidate.

S41/G5.8 final independent ADVERSARY pass: `original-aligned.log` proves four
exact ROOT translations including vertical carry for occupied and empty real
experimental carts, settled native replicas and unchanged rider seat;
`handoff-unblocked.log` uses real world ticks for physical rail entry/exit,
old-support retirement, fresh contact sequence 2 and one later ROOT. The
independent 45-degree `rotation-first.log` matches an analytic rigid-rotation
reference within about 6e-8 blocks, refreshes the native seat once and rejects
a repeated drain. `empty-native-retirement.log` proves that vanilla pickup of
the prepared cow as a passenger retires the material relation. An isolated
overlay omitting only the ROOT commit makes the unchanged root oracle RED
at its first interval. The final public candidate and ordinary controls were
also independently rerun GREEN. No production or IMPLEMENTER test was changed
by the adversary; all 370 production source/resource hashes match S40.

The executed snapshots, private test sources, commands and log digests are
recorded in `../s41-adversary-private/evidence.json` (SHA-256
`316A7307BCAF2E6E9CA6CBEC57C92F8D755900658A911258A411EF6BCAFFEFA9`).
Both coordinate contact sheets were generated from runtime evidence and
inspected; they do not claim client framebuffer or human gameplay acceptance.
Invalid zero-lag comparisons while native push continued and initial fixture
obstacles were classified and corrected without weakening native physics.
Full source/evidence review ended with no further changes. `git diff --check`
passed. S41/G5.8 and G5 category/lifecycle tasks 1-2 close locally; broad
nonparticipant equivalence, legacy retirement, G6-G9 and final human pack QA
remain open.

S42 IMPLEMENTER reconciles `main@b5e6253584b2233e5dd0039203c8a251bf10cdeb`
with the S41 canonical branch. The pre-I5 parent audit verifies all 69 changed
mainline product/resource/tool/workflow paths, one gravity frame/provider and
the exact union of both parents' test entrypoints (102 server, 25 client, no
duplicates). Of 112 canonical collision production files, 111 are unchanged
after line-ending normalization; GravityFrame adds the equivalent cardinal
coordinate/vertical conveniences. Current mainline mount families, inventory,
body equipment, gestures, gait, reach, render membership and optional-mod data
remain. `../s42-current-review.json` has SHA-256
`bbad102c0f53afdee7cb00c45ff874f524182c7cc5fad30daf2dfeb73423da84`.

Pre-I5 affected-source ordinary `build runGameTest` passes **483/483**, exits
cleanly in 3m23s, and the complete client suite exits cleanly in 5m27s. Logs:
`../s42-ordinary-final-tree.log` (SHA-256
`d52fad5b02807c5d71d20c6b3691ddbffe68979390021e4194eb7bede3866e0a`)
and `../s42-client-final-candidate.log` (SHA-256
`045c38b1d2ffd0327e2dff41815e21ce6a8b91d8417757d43f4b8fce6cd86a11`).
The prepared original-model suite passes **2/2**, 23 scenarios, in 2m08s:
`../s42-prepared-final-catalog-candidate.log` (SHA-256
`b214769104fec7d08896cc308cc0048ad99d8700606ac0ebba67369228b91dab`).
The real feature-enabled experimental minecart/native interpolation/control
proof passes in 2m22s: `../s42-experimental-client-final-candidate.log`
(SHA-256 `d6a2a6348741b987bed8e27e8e6c09a51700b6602692809d32deefef4ba8e71f`).
Prepared/experimental invocations explicitly use the isolated original client
export `-PscalebrewsAnatomyCatalog=../s36-impl-run/anatomy-export`; tests were
neither skipped nor weakened when that required argument was initially omitted.

Mainline's deprecation guard is retained with a finite, exact test-only
collision-adapter diagnostic allowlist and full compiler reporting. The real
full compile reports **387** expected adapter diagnostics and passes the guard
(`../s42-ci-real-final-candidate.log`, SHA-256
`d1b2ee9f4a274fc650d14ad54536483e3d9450b5b80e49037eb2521faf6ab463`).
The six IMPLEMENTER guard controls reject production, native, external,
unparsed and compiler-cap-omitted warnings while accepting the named fixture
adapter (`../s42-ci-guard-probes.json`, SHA-256
`2901e39e1488c4fe5f0d8ca31f65f68226acc68e6441f51e3a6f2d0dacfc83bd`).
The pre-I5 cache-reused server compilation reports zero
new diagnostics; it does not replace that earlier full-compile audit. Deprecated
Minecraft player helpers were migrated to the mainline connected-player test
seam. ADVERSARY preserves S25's intentionally disconnected receipt oracle by
constructing a genuine player without a listener, instead of expecting the new
connection mixin to keep a helper-created player disconnected.

The optional FirstPerson bridge prepares typed MethodHandles during explicit
client initialization without initializing external classes early; extraction
has four constant-cost samples and no reflective metadata/invocation discovery.
The real pinned FirstPerson 2.7.2/26.2 JAR was verified against its actual public
API and SHA-512
`a1822273015aeb291bdc6ceeb2787e2734a9bde1f20abbea613b1088ad34fed84a9a37953efc842650ad597d557df868d4b5142502fcb648121a25fa83a3350b`.
TinyMountClientAcceptance with that isolated JAR passes and exits cleanly in
3m55s, recording 124/130/113 bee/chicken/wolf first-person frames:
`../s42-firstperson-client-candidate.log` (SHA-256
`5403fa8eb403273684b32607ce6f85a759e35d894691c46aa03aee6334767140`).
Thirty captures from each lane are preserved in
`../s42-firstperson-visual/firstperson/` and `../s42-firstperson-visual/base/`
with names, capture timestamps and hashes in `capture-hashes.json`. The same
fixed camera/pitch/menu commands generated both lanes; IMPLEMENTER inspected
the chicken first-person pair, wolf equipment menu and bee mounted front.
FirstPerson adds the expected player-body render while preserving the mount
view. These are runtime visual observations, not final human pack acceptance.

The initial ordinary client invocation failed S29's old-catalog/material replay
oracle. It is preserved as RED; subsequent complete-suite green and isolated/
prefix/stress greens on unchanged production do not establish its chronology.
An independent real-event holdout then deterministically reproduced a production
NFR-017 defect: a callback from the previous connection executes on its Netty
thread after a new connection has accepted revision 2. The unqualified reset
clears the live catalog, after which a revision-1 replay on the unchanged new
connection is accepted. `../s42-adversary-retired-first.log` is RED and its
sources/log are fixed by `../s42-adversary-private/retired-evidence.json`.
This reproduces the failure class; it does not retrospectively prove the
uninstrumented log's exact event order. I5 returned to planning and converged
independently before production repair. The repaired transitions execute on the
client executor and check the exact current/owning listener identity at execution.
The genuine current disconnect retains its cleanup path. Post-I5 ordinary
build/server **483/483** and all 25 client entrypoints exit cleanly:
`../s42-i5-ordinary-full.log` (3m39s, SHA-256
`fe8b7975a49e143f401a3e65e5cea1dadd40665cdcefaa2b2c12b0481584c2db`)
and `../s42-i5-client-full.log` (6m04s, SHA-256
`022bf554a41546c1e1d6b561c16ccb4268d51618a649133316c5998c4e81787d`).
The repaired ledger `../s42-i5-current-review.json` (SHA-256
`d7eaedc258db106e93a29bcbbc57bfab955f5fcbc5f0b80be2f19d92244d6aba`)
identifies exactly two changed canonical production files: GravityFrame and
AnatomyClientNetworking. The beta.9 production JAR has SHA-256
`b2dafaa5e2a1f9785a6959e60506c4ef91707d9bf833ee5bce3b02bfd0ad8b26`,
correct version metadata and zero leaked GameTest classes/nested classes
from all 354 fixture-source paths at that stage. Independent positive controls,
targeted mutation and final review were still pending; earlier logs describe
pre-I5 source.
Other failures are
classified separately: unquoted/omitted Gradle arguments, the connected-player
fixture conflict, deprecation-gate integration and Gradle's rejection of a
combined server/client task graph under a temporary shared run directory.
Corrected server/client invocations are separate and clean. Main still resolves
to the pinned merge parent after a fresh fetch. That candidate record alone did
not certify S42 closure, broad G5 equivalence, legacy retirement or G6-G9.

S42 final closure verification (2026-10-05) is now green on the durable lifecycle
regression tree. `../s42-final-server.log` passes build and **483/483** tests,
clean exit 2m31s, SHA-256
`9a5a79b5926060a70e7f7bffe593e1f6359d0ca16fadf0ac480696b8c0b8ced3`.
The exact deprecation guard accepts all 147 uncapped incremental test-adapter
diagnostics, with no production/native warning present; the earlier full audit
remains separately recorded. `../s42-final-client.log` passes the complete
**27-entrypoint** default manifest and exits cleanly in 5m53s, SHA-256
`c896863a68cdfb58a2b27d217c1cfbd47ae75d5fd0c72bc71a3d20273642e231`.
The processed manifest exactly matches source, including both new regressions.
Actual retired-callback output preserves ready revision 2; the actual current
callback subsequently clears owner/catalog. S29's original assertions remain.
The final experimental minecart proof passes native behavior, ROOT, rail
release/reacquisition, interpolation and ratio/policy/obstruction controls in
1m08s: `../s42-final-experimental-client.log`, SHA-256
`f5ed37afd333fc7f7bb32c52a2d25c3475c981800e39215a8ed613dd4c0b1c54`.

Independent I5 evidence fixes the unmutated GREEN, the identity-guard mutant's
semantic RED (`accepted=2 actual=1`), reusable regression sources, exact-handler
gate and positive current-disconnect control in
`../s42-adversary-private/i5-final-evidence.json`, SHA-256
`960566fecbed6f15a10e2d5f5e15776d6b4f6b8e37570ae9a4746879fc118bbe`.
Its complete independent source/infrastructure review required no more changes.
The exact-listener gate's inherited-field linkage fault and earlier positive
fixture cleanup faults remain classified as test infrastructure, without
weakening the catalog or genuine-disconnect expectations. The reusable tests
and two client-only mixins are retained; temporary runners/overlays stay outside
the repository and generated Python cache files were removed.

The final parent/source ledger `../s42-final-source-review.json`, SHA-256
`a211ed8be38146e27d7bba162caaf17dd79dbd220f0b3b1166fad16f28c2b8e0`,
checks all 69 main-changed paths, both parents' test subsets, 102 server and
27 client entries without duplication. Exactly 110/112 canonical production
files remain unchanged after line-ending normalization; GravityFrame and I5
networking are the only changed ones. `../s42-final-evidence.json`, SHA-256
`c57a03fd862125c504dc387ea9ed9b6cdf11a702bd3b12599d0ad5f4c5ee1c7d`,
records the logs, processed manifest and beta.9 JAR digest
`b2dafaa5e2a1f9785a6959e60506c4ef91707d9bf833ee5bce3b02bfd0ad8b26`:
metadata is correct and none of all **358** GameTest source classes/nested
classes across any package leaks into production. The revealed seat contact
sheet/reference metadata was also inspected by IMPLEMENTER. Final source,
ownership, cost and evidence review and `git diff --check` require no further
product or fixture edits. Main still resolves to the pinned merge parent.
S42 closes through its own local `Sprint: S42` merge commit. Broad G5 equivalence,
legacy retirement and G6-G9 remain open; human QA stays at the end of the epic.

S43 verification in progress (2026-10-05, IMPLEMENTER): the converged native
pair differential exposed FR-011 ghost contact after an actual effective-scale
change. At ratio `1.777777974007075`, ROOT drain correctly applied no movement,
but kept the old material relation (`../s43-prepared-sixth.log`, SHA-256
`bba23a9fc82224a4ca335ba4f4ecd5bea985b5054215c69f3e6307ad8c121e1b`).
ADVERSARY independently reproduced the defect before the repair. The I7 repair
locally retires ineligible contacts on actual dimensions changes and validates
state/gravity before server carry delegation; it adds no physics owner or model
preparation. The unchanged scope remains in
`docs/sprints/S43-g5-native-pair-equivalence.md`.

The repaired prepared focal passes 2/2 required tests and 28 actual seven-category
ratio/policy comparisons, with 14 nonzero push controls, global native
collision/pathfinding/combat/milking controls, rejected-pair no-carry and genuine
accepted ROOT carry, effective-scale retirement and support-baby release:
`../s43-prepared-repaired.log`, BUILD SUCCESSFUL 1m42, SHA-256
`501d1ea5d334b84ed85b9200daa678b569d6024d7516580ec5777223df39c1d5`.
Full prepared composition also passes all 24 scenarios (indices0–23), 2/2 tests
and clean exit 1m13: `../s43-prepared-full.log`, SHA-256
`6694f077120d641baea285ae93ec205fdf3f5552af862f4fafb1eeb7dbc81171`.

The actual local-client/connected-server focal passes after repair and cleanly
exits in 2m53: `../s43-local-repaired.log`, SHA-256
`38a90687e1bc3072057bdc450a47e1b7c3f5e139bd2ef40148d800666fadf656`.
Rejected local Entity.move equals the native control in displacement, flags and
velocity. Both actual push callers and both bodies retain nonzero native impulse
during real accepted contact/carry. Server and predicted local actor receive
one `.25` ROOT; the local transport is observed, repeated drain is inert, and
actual client/server positions agree after native support interpolation. The
clear-face control avoids native automatic overlap pushes without disabling
native push. The proof is in the default client manifest; full default client,
ordinary build/server, adversarial holdouts/mutations/snapshots/cost and final
zero-change review remain pending. This evidence does not yet close S43 or G5.

S43 I8 candidate (IMPLEMENTER): independent support-scale RED2 reopened final
acceptance after the I7 greens. The derived weak-identity membership index now
revalidates only the changed support's retained dependents; CONTACTS remains
the sole contact truth. This modified foundation requires recertification within
S43 before another sprint may rely on it. Actual I8 executions:

- Prepared focal: 28 native/material comparisons, 14 nonzero push controls,
  2/2 tests, clean exit 2m07; `../s43-prepared-i8-focal.log`, SHA-256
  `52a768127053f509693d29d5891ab7764a0524ba42ab3672ef4bcf6e9f6f8f50`.
- Full original prepared composition: 24 scenarios, 2/2 tests, clean exit
  1m01; `../s43-prepared-i8-full.log`, SHA-256
  `e1a9460e9f3503522f7a9d03727be8efc560f2d55af4fa6ef95df53cacd1a938`.
- Actual local-player/client-server differential and isolated index bookkeeping:
  both pass, clean exit 2m50; `../s43-local-i8-focal.log`, SHA-256
  `06311dfd962c24a400302f7a5aefa4f011e84cfff72112a52459985ad92e9390`.
- Ordinary build/server: 483/483 tests, clean exit 1m53;
  `../s43-server-i8-full.log`, SHA-256
  `e4201bc6f542ad33865c863398a7e7619555b4e468dcb71c926654e2d153fb35`.

The deprecation guard passes the uncapped focal compilation (273 intentional
test-only adapter diagnostics) and the ordinary incremental build (zero).
These greens do not certify the still-pending independent final review or
default client composition. No S43/G5 closure is claimed yet.

S43 I9 candidate: independent RED3 shows that downstream receipt invalidation
visits 128 unrelated recipient histories while retiring a Cow with no receipts
(`../s43-adversary-receipt-cost-first.log`, failed1m08). I9 adds a derived
historical-recipient lookup to the existing receipt owner. The first candidate
`dd878050` passed functional/regression probes but its WeakHashMap could drain
an unbounded stale-reference queue during a hot access. IMPLEMENTER static review
replaced it within the same converged contract with MapMaker weak values;
authoritative History owns the corresponding bucket, preserving UUID value
equality and weak recipient identity with bounded weak-reference maintenance.
No packet/claim/TTL/saturation or movement contract changed. Final receipt source
SHA-256 is `f98300e23cb6b35e5b29578fd24989446f3f49b61079e486b0eaf64d2572f7d1`.

Executed final-candidate IMPLEMENTER evidence:

- Build/server 483/483, clean exit3m01; `../s43-server-i9b-full.log`, SHA-256
  `fc665d17b765d33958e01bf56e5c51cade1dcad5fd2ce799bd2d02fc9637db61`.
- Prepared composition 24 distinct-tick scenarios, 2/2 tests, 28 pair comparisons
  and14 nonzero native push controls, clean exit1m19;
  `../s43-prepared-i9b-full.log`, SHA-256
  `335c137daeaa9c7bc121041d85b20713a9144f7a0680e87c87a4608f8966801b`.
- Real dedicated player/boat RTT0/100/200ms receipts/references and reconciliation,
  zero vanilla corrections and client/server convergence, clean exit4m04;
  `../s43-receipt-i9b-reconciliation.log`, SHA-256
  `59ef88504fcc97ffed5b3cab31665958c91774b5882d4592b5f3ba8703f0e3a1`.

The final build's deprecation audit passes 21 intentional test-only adapter
diagnostics. Independent foundation/mutation/snapshot acceptance, final default
client32, experimental-cart recheck and closure reviews remain pending.

Final IMPLEMENTER execution subsequently completed on the same four production
hashes: full default client32, clean exit4m39 (`../s43-client-i9b-full.log`,
SHA-256 `d82d3fd51c1a074c9c2f58f07c795d7e7b6cfcc754f3d23b807a60192f850739`),
including both public scale-retirement cases and the receipt-state foundation,
and real experimental NewMinecartBehavior, clean exit59s
(`../s43-experimental-i9b.log`, SHA-256
`ff3bf4e0c69ba08097a7567c0a84d778d2c3ed963a4e37db7015821052d281a4`).
The default manifest was archived as executed before selecting the experimental
proof, then restored in generated resources without source changes.
`../s43-final-source-review.json` SHA-256
`9103eef2c084887c2ef3274b2ded46c9181825beab0855b376ec8440baff9898` verifies
exactly four production changes from S42 and all other gameplay/client paths
unchanged. `../s43-final-evidence.json` SHA-256
`9ea22db764377246e1021a0b4d0fc966461486e9b41b989ba1b0ba67c9baf0ee`
records the exact logs,24 native distinct-tick scenarios, default32 manifest and
production JAR. Beta9 JAR SHA-256
`883cba595594adffe87be37365ec763bb0b8d1e23bb1ce1dee005831cfae0f8d`
has correct metadata and no test-class/nested-class leak across all367 GameTest
source classes. IMPLEMENTER's complete final pass needs zero production changes;
independent final acceptance is still pending. This is not whole G5/G9 closure.

S43 final independent acceptance: `../s43-adversary-final-current.log` passes
all eight independent holdouts on the same final source, clean exit2m06,
SHA-256 `5e86d7f27c81178411971e173b85319505b8196a40545f70285314adc45f2cda`.
It includes real native push/no-wall, body/support scale retirement, actual local
input/carry, 1,000 warm revalidations with zero added geometry/joint evaluations,
2,048 unrelated contacts with zero global query visits, receipt lifecycle and
the unchanged128-unrelated-recipient RED. The three reusable public regressions
also pass together, clean exit1m15, `../s43-adversary-public-final-first.log`,
SHA-256 `e53b88f14ecd9625d9c903c86f34ba7befe00a866c27b5b41dcbb9e81c5422c5`.
The final external membership mutation compiles and fails at the expected
disconnect assertion, `../s43-adversary-receipt-mutant-final.log`, failed2m,
SHA-256 `047b008c35c3f94f3cb91f6abb7af489fbb1d81ceee8f2b8f052e7f1ea3ce09b`.
Earlier eligibility, push and contact-index mutations are recorded in the sprint.

Both derived-membership foundations are independently TRUSTED for the extended
scope. The actual native-input AABB top/side projection and reproducible metadata
are `docs/evidence/S43/native-contact-projection.png` (SHA-256
`e0cc57ee915b68a82687dd1ee8edbe5b1964dd60dd9d6127ccb5631dd246af30`) and
JSON (SHA-256 `93f63d775d9e8a81ed87006aa388647d74d3f400b6d3b09cadde7867e6073ff7`).
The final sample agrees with its native-input baseline within1e-10; IMPLEMENTER
viewed the projection. This supplements spatial assertions and is not a renderer
screenshot, global TPS/heap/GC soak or full-pack visual acceptance. Both complete
reviews require zero additional source changes. S43/G5 task6 closes locally
through its own `Sprint: S43` commit; legacy retirement, later gates and final
human acceptance remain open. No publication/install authorization follows.


S44 implementation verification (2026-10-05, IMPLEMENTER): G5 tasks7–9 retirement candidate.
Exact358 production/resource hashes,242 Java sources,370 GameTest source classes
and the final32 actually processed client entrypoints are recorded in
`docs/evidence/S44/implementation-verification.json`. Only executed results:

| Run | Scope/result | SHA256 log |
| --- | --- | --- |
| `../s44-server-i3-full.log` | 466/466 server; BUILD SUCCESSFUL in 2m 24s, clean exit | `0650e7fb0c7b4746a8cc334bc31b60c6305628f661b910914a0aa322bf29ebf5` |
| `../s44-prepared-i3-full.log` | 2/2 prepared with all24 original scenarios; BUILD SUCCESSFUL in 1m 57s, clean exit | `3ab7c1a4dc54946d02450f7644e9908fe237b077510e9f13b7f3ee537b93a1d7` |
| `../s44-client-i2-full.log` | 31 default client entries before final cosmetic hook rename; BUILD SUCCESSFUL in 6m 58s, clean exit | `c16ac1a5856077cf89a17487d3a6f8805b3724469cb372974c41c11d55a7d5d5` |
| `../s44-client-final.log` | 32 final default client entries including independent scope; BUILD SUCCESSFUL in 3m 34s, clean exit | `5c37265ff2cd99f9793370b838daf42cea65483f6f486d2f2b504c4da21d7b95` |
| `../s44-build-final.log` | final build and466/466 server; BUILD SUCCESSFUL in 1m 57s, clean exit | `7e7390ccb3e8042105914854c273e15e7046aaa2fa4291c4aab0ee2157be2c8b` |
| `../s44-experimental-client.log` | actual experimental cart flag/rail/native controls; BUILD SUCCESSFUL in 1m 41s, clean exit | `c63e7866a8aac66454843f63044841e07c2111bbef92088e7c3faab09b8c3390` |
| `../s44-receipt-reconciliation.log` | dedicated player+boat RTT0/100/200, zero corrections/drops and convergence; BUILD SUCCESSFUL in 2m 39s, clean exit | `fa1b100375fe3c4638231164fc44b44a6929ab0f462625c5e12b613dae21b9bf` |
| `../s44-firstperson-baseline.log` | immutable S43 HEAD0b7c5bea and actual FirstPerson2.7.2; BUILD SUCCESSFUL in 4m 21s, clean exit | `b57bb282f1b321aac6bc7ba742a6a6d9d812dd8af9e94d80c49a06c524e2c4a8` |
| `../s44-firstperson-candidate.log` | candidate and same pinned FirstPerson2.7.2; BUILD SUCCESSFUL in 3m 33s, clean exit | `85b013ee91b08877c99e9e0169ec7b0a5551ec131b5784e267f5e0de33241a3a` |

Final development beta.9 JAR SHA256 `9c258b33e5a147360e91a2ce66f2273ee99a7b5ad676931bc77d7c69431b56bd`:
1,146,458bytes/720 ZIP entries, no retired runtime classes, shipped plane-profile
files, leaked tests or unresolved mixins. Source inventory has zero executable
references to the21 retired types and no unresolved configured mixin. An empty
entity_platform ZIP directory is metadata. No publication/install occurred.

Server483→466 is19 cancelled old-engine assertions plus2 actual native/decoder
controls. Each removal and retained canonical oracle is classified in the S44
sprint record. Earlier compile/setup failures (compound-var, deleted state
references, two eligibility-before-binding preconditions, Happy Ghast ordinary
versus support classification) were repaired and reexecuted; no physical RED was
hidden. Final deprecation audits:12 build/4 client test-only adapter diagnostics,
no production diagnostic/cap. Default diff check passes.

Actual FirstPerson reference/candidate captures:31 each,62 hashes and two sheets
in `docs/evidence/S44/firstperson-comparison.json` and the adjacent PNGs. IMPLEMENTER
inspected rider/body/held-item framing, bee player inventory, chicken/wolf
equipment, mount front/top seating and native horse: no observed regression.
Scripted views/tick waits match; seed, particles and render partial are not
frozen. This is bounded actual Minecraft visual comparison, not pixel equality
or final human QA; ADVERSARY supplies a fixed-partial near-plane semantic snapshot.

Seven ownership/protocol gates pass; S25 authority reads removed files and was
returned to ADVERSARY as infrastructure RED. S44 remains open until gate
adaptation/sensitivity, independent final review and both zero-change passes
close. G6 coverage, G7 real consumer, G8 pack, NFR-014 benchmark and human acceptance
are not certified here.

S44 final closure (IMPLEMENTER): all eight ownership/protocol gates pass on the
frozen source. The stale S25 gate is repaired; the independent reusable sensitivity
campaign records baseline PASS and11 expected REDs in
`docs/evidence/S44/reference-authority.json`. ADVERSARY independently verifies
466/466 server, eight physical/lifecycle/locality holdouts,21 presentation entries,
DISABLED absence/scope, scope/camera mutations and fixed-partial near-plane
snapshot. Its final zero-change review and TRUSTED scope are in the S44 record.
Both agents inspected the actual mount/FirstPerson comparison sheets with their
stated timing limits. Production hashes still match the executed final candidate;
all modified text decodes as UTF-8 after a documentation-only encoding repair.
Both full review passes require no further source changes. G5 tasks7-9 close in
their own `Sprint: S44` local commit; G6-G9 remain open. Human stress cases from the
retired platform path move from TODO to G9's existing acceptance matrix.


## S45 - complete original Minecraft census (closed)

Active role IMPLEMENTER. Explicit client PREPARATION now inventories all93
Minecraft26.2 LivingEntity types through the canonical discovery/scanner, with
original renderer/model/record/layer and wide/slim avatar alternatives. Actual
server factories plus the actual connected player independently give the same93
types; no factory null except player. Candidate inventory:95renderer alternatives,
561semantic slots,467identity-cached model inspections,120candidate groups,
4499parts/3217cubes;4798shared mutable parts preserved before/after. This includes
current installed Scale layers; cosmetic candidates and9zero-material slots do
not authorize physics. Executable selectors/poses/root transforms remain gaps.

Executed corrected census PASS1m59s after identity corrections (stable enum names,
framed map keys, malformed Unicode rejection); identical repeat, changed exact
input, structure invalidation, empty/cycle/depth/nonfinite checks all PASS.
Actual GameTest catalog has92UNRESOLVED+1armor_standSAFE_PARTIAL from its existing
external variant fixture,0FULL; requireResolved correctly rejects the target.
The target pins actual game/loaded-mod versions, JAR/directory/tool hashes and
selected resource-pack identity; first preparation354938400ns with parallel
lanes is preparation evidence, not a physics budget or isolated benchmark.

Final build PASS2m35s with466/466 server GameTests. Default client suite32entries
PASS5m4s before the last tooling identity corrections; none of those proofs
invokes the census. The corrected tooling itself subsequently passes the original
census and final build. Source ownership is unchanged for runtime/solver/network.
Eight existing ownership/protocol gates PASS; presence-only gates are labelled.
Evidence: docs/evidence/S45/minecraft-census.json, its canonical preimage,
implementation-verification.json and ownership-gates.json, with360production
file hashes and exact executed log hashes. JAR SHA256
3418a682f7b3c3f19edb0881d63ef35c0a84ca9e66f93c78c5857c2d3580af4a
(1174236bytes/732entries) has current preparation owners and no tests/retired
platform classes. Reproduce the focal census with runClientGameTest
-PscalebrewsS45CensusOnly=true; combine no other proof selector.

Independent final acceptance PASS: four promoted Census/SlotIdentity/Identity/
Bounds client proofs with clean shutdown57s. Separate original observation523
models/4798parts and factory population93; exact limits, thread fence, Unicode,
unchanged semantic-slot oracle and initial identity checks PASS. Prior alias RED
is an implementation defect repaired by framing; isolated original-loadPose
ownership mutation gives expected RED2m29. No production fix or weakened oracle
comes from ADVERSARY. Reusable selector tools/s45_adversarial.init.gradle and
adversarial-verification.json preserve exact commands/hashes/classification.

Both complete zero-change reviews and all360 frozen productive hashes PASS;
existing production files are unchanged and two preparation owners are added.
Default manifest32 was restored after opt-in census. G6 task1 closes in S45's
own local closure commit (Sprint:S45), verified before the next sprint; task2
and complete physical coverage, G7-G9 and owner acceptance remain open.
