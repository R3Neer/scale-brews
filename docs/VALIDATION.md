# Validation record

This file is the sole source of truth for **executed evidence**. It records what was actually built, run or inspected, against which snapshot, and what that evidence does **not** prove.

Normative entity-collision behavior lives in [ENTITY_COLLISIONS_REQUIREMENTS](ENTITY_COLLISIONS_REQUIREMENTS.md), architecture/API/data ownership in [ENTITY_COLLISIONS](ENTITY_COLLISIONS.md), and implementation order/status in [ENTITY_COLLISIONS_PLAN](ENTITY_COLLISIONS_PLAN.md).

## Evidence rules

- Source/test existence is not passing evidence.
- Kernel/unit evidence does not substitute for real GameTest/client/dedicated/compatibility/latency/human QA when required.
- Evidence attaches to an exact snapshot or is labelled historical/isolated.
- Compatibility evidence is version-bounded.
- Packaging proves packaging only unless gameplay was separately exercised.
- A later red snapshot can reopen a previously green gate; history is retained as red-before-green evidence.

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
- keeps the broadphase input as material `AABB` so later Q2 can replace endpoint bounds with certified temporal envelopes without replacing the kernel.

GitHub Actions run **`34602837677`**, job **`103274063124`**:

- **277 tests registered and executed**;
- **277/277 required GameTests passed**;
- `BUILD SUCCESSFUL in 1m 13s`;
- artifact **`10265820434`**;
- SHA-256 **`1573c97d70c3b254dcf6957b57c695c9004b0a37cd8acec1301b7e84cbec8f28`**.

This was a valid closure of the original kernel/fail-closed scope, but later adversarial live-wrapper evidence reopened S05 under NFR-008. The historical run remains evidence for the kernel and is not erased.

## G2 adversarial campaign — current red evidence 2026-09-11

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
- post-rebind locality still fails with **128 far-provider samples**;
- `firstLocalQueryAfterSupportedRootCommitMustNotResampleFarWorld` also fails with **128 far-provider samples** after `captureRoot → setPos → commitRoot`, with counters reset only after the supported hook;
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

The S07 campaign produced several red-before-green repairs before the current blocker:

- exact `t=1` contact with zero body displacement;
- contact provenance in a joint batch with a merely-near distractor support;
- starting-overlap failure must suspend only the bad body/support pair and preserve a bystander;
- same-binding reacquisition after the overlap disappears;
- numerical budget exhaustion without initial overlap must release/suspend an old retained contact instead of leaving uncertainty authoritative.

The provenance repair is green in run `34623887683`, job `103344184321`. The budget-uncertainty holdout is green in later runs, including the current `91d0be66…` baseline.

### S07 current blocker: mutually incompatible valid plans

`S07PlanConflictLocalizationTests.simultaneousValidPlansThatWouldOverlapMustReleaseOnlyAffectedPairs` creates two retained contacts on opposite material walls. Each candidate is first proven independently to have a complete bounded temporal plan. Those plans move the bodies toward one another and their planned final AABBs overlap. A third body has an unrelated valid contact on the same support.

The fixture originally hit floating-point/coordinate artifacts because GameTest worlds can have very large absolute coordinates. Snapshot `91d0be66ef21f4d50c91861084e2680844b65580` constructs the convexes in a small local frame and translates them in double precision, so every fixture precondition reaches the intended production branch.

In run **`34627841257`**, job **`103357151536`**:

- both individual temporal plans are `COMPLETE`;
- the planned simultaneous finals overlap;
- backend returns `QUARANTINED / BACKEND_EXHAUSTED`;
- no partial displacement is applied;
- the test then fails exactly because the two affected retained body/support relations remain authoritative instead of being released/suspended;
- the third bystander is retained as the locality control for the eventual fix.

This is a real **NFR-004 / FR-052** blocker in the `worsensBodyOverlap(...)` failure path, distinct from the already-repaired `plan()==null` budget case.

### Prepared client/server evidence during G2

The isolated prepared-anatomy lane is green after the causal/binding repairs:

- run **`34623128548`**, job **`103341673751`** passed original client geometry export plus the deterministic prepared pair-suspension proof;
- run **`34624096154`**, job **`103344876628`** also passed the client export and prepared server proof.

These special lanes do not override the red ordinary S05/S07 holdouts above.

### Current G2 interpretation

- S05 is **reopened** under NFR-008 for post-mutation live-query locality. The immutable/build-only kernel itself remains supported by its historical green evidence.
- S06 identity/causal membership has no active blocker.
- S07 is **reopened** under NFR-004/FR-052 for the valid-plan conflict path.
- G2 cannot close while either S05 or S07 is red.
- Before global G1/S04/G2 closure, `chatgpt-editing` must still be reconciled with `main@39824ddfeb708825e6aaf4abc5efb6bd0d9ac284` so `io.github.r3neer.scalebrews.integration.gravity.GravityFrames` is the single Scale body-gravity authority.

## Current acceptance gaps for entity collisions

The canonical open work is in `ENTITY_COLLISIONS_PLAN.md`. Major unproved areas include:

- closing the current S07 rejected-batch final-contact-validity red;
- remaining G2 tangential/multicontact/sliding/separation/chains work not yet claimed by the current sprint;
- final lifecycle/reload/reconnect architecture;
- prediction/reconciliation for locally controlled actors;
- generic family engines and zero-UNRESOLVED vanilla coverage;
- final Clinging migration;
- version-pinned VanillaPlus compatibility;
- normative performance benchmark and final latency/soak matrix.

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

The earlier plan-conflict locality defect is closed, but a later adversarial holdout reopens S07 under NFR-004.

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
