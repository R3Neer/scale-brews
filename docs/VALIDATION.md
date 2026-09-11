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

## G2 / S05 bounded material broadphase — CLOSED 2026-09-11

S05 attacked only the spatial-cost/fail-closed thesis, not the still-open continuous-causality requirements FR-049..051.

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
- preserves same-tick frame validation before index reuse;
- propagates exhausted/invalid spatial queries conservatively: `spaceClear=false`, `collide=Vec3.ZERO`, ray/sweep publish no material result;
- keeps the broadphase input as material `AABB` so later Q2 can replace endpoint bounds with certified temporal envelopes without replacing the kernel.

GitHub Actions run **`34602837677`**, job **`103274063124`**:

- **277 tests registered and executed**;
- **277/277 required GameTests passed**;
- `BUILD SUCCESSFUL in 1m 13s`;
- artifact **`10265820434`**;
- SHA-256 **`1573c97d70c3b254dcf6957b57c695c9004b0a37cd8acec1301b7e84cbec8f28`**.

The nine registered S05 holdouts cover exact/+1 query budgets, oversized entries, far-world amplification, identity-vs-equals, insertion-order determinism, candidate saturation, runtime clearance fail-closed, runtime movement fail-closed and quarantine/rebind recovery.

Final structural review on `541a333…` found no `overflow`, no `level.getAllEntities()` and no hot-query branch over all index entries. Existing temporal/spatial regression tests also remained green inside the 277/277 suite. S05 therefore closes G2 task 6 only; it does **not** claim continuous interval consumption, dispatcher integration, exactly-once causal events, full solver closure or final performance benchmark.

## Current acceptance gaps for entity collisions

The canonical open work is in `ENTITY_COLLISIONS_PLAN.md`. Major unproved areas now start at the remaining G2 scopes:

- live continuous material-event consumption for root/joint/carry;
- certified temporal envelopes and exactly-once causal interval processing;
- multiple same-tick material contributions and ancestry;
- full tangential retention/multicontact/sliding/separation recovery;
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