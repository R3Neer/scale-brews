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

