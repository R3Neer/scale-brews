# S20 — Adversarial closeout state

Status: **NOT CLOSED — I9 RED**.

This document records the independent adversarial state of S20 after the Citadel pose-program implementation reached real-model parity and the condition-runtime bound was repaired. It does not replace the canonical sprint checklist and deliberately does not mark S20 closed while the canonical executable-binding path is still failing.

## 1. Executive result

S20's reusable Citadel evaluator, optional authoritative-channel boundary, catalog transfer format, representative program data, real-model oracle and bounded hot-path program contract are materially present.

The independent closeout now classifies:

- I5 catalog/data transfer: **PASS**;
- I6 external channel adapter boundary: **PASS after adversarial bug/fix cycle**;
- I7 representative Grizzly/Gazelle program data: **PASS**;
- I8 original-model parity oracle: **PASS**;
- I9 canonical runtime integration without hard external dependency: **FAIL / OPEN**;
- I10 runtime/limits/authoring contract: **PASS after adversarial boundedness bug/fix cycle**.

S20 has one remaining production blocker: accepted canonical `scalebrews:citadel_program` bindings are still not materialized into `WorldAnatomyCatalog.Snapshot.bindings()`, and the runtime path still carries unconditional legacy pose-eligibility assumptions that a canonical binding must not inherit.

## 2. External channel transaction bug and repair

The adversarial pass found that `AuthorityPoseTracker.sampleExternalChannels` could previously publish an adapter's valid prefix and then return failure after a later non-finite value, ownership conflict, duplicate or budget overflow. That violated fail-closed semantics because partial authoritative truth escaped from a rejected adapter sample.

The implementer changed publication to stage the whole adapter output, validate it completely and call `putAll` only after success. The independent holdout now covers:

1. non-finite tail rollback;
2. late ownership conflict rollback;
3. duplicate channel inside one adapter rollback;
4. late 65th-channel budget overflow rollback.

Evidence: workflow `s20-adversarial-pose-channel-atomicity`, run `35012624020`, **SUCCESS**.

## 3. Real-model parity oracle

The S20 client oracle is independent of the declarative program formulas. It loads the pinned Alex's Mobs Continued `alexsmobs` 2.1.9 model implementation, invokes the original model `setupAnim`, obtains real `AdvancedModelBox` local transforms through its actual transform path and compares them against Scale's shared `scalebrews:citadel_program` result.

Coverage in the accepted run:

- Gazelle: 4 samples, including ordinary procedural state and clip/state coverage;
- Grizzly Bear: 4 samples, including ordinary procedural state and `ModelAnimator` clip/state coverage;
- both evaluate through the same shared Scale pose engine.

Evidence: workflow `s20-citadel-pose-real-proof`, run `35014694555`, **SUCCESS**.

## 4. Performance and boundedness evidence

The hot evaluator no longer reconstructs keyframe deltas on every sample: program binding compiles keyframe state up front and sample lookup uses binary search. Allocation and CPU scaling were measured independently rather than inferred from code shape.

Allocation evidence, run `35014967562`:

- synthetic 1 touched bone: **936.00 B/evaluation**;
- synthetic 16 touched bones: **6849.44 B/evaluation**;
- marginal slope: **394.23 B per additional touched bone**;
- pinned Gazelle: **3898.91 B ordinary / 4184.00 B clip**;
- pinned Grizzly Bear: **5172.53 B ordinary / 5448.00 B clip**.

Operation CPU scaling, run `35059646199`:

- 32 operations: **1151.84 ns/evaluation**;
- 128 operations: **2686.13 ns/evaluation**;
- 512 operations: **11633.27 ns/evaluation**;
- 2048 operations: **45989.92 ns/evaluation**;
- the 64× operation increase produces 39.928× measured CPU growth, with roughly 21–25 ns/operation after the small-workload fixed overhead.

Condition evaluation on the same run measured:

- 1 node: **289.66 ns/evaluation**;
- 33 nodes: **538.25 ns/evaluation**;
- 1057 nodes: **9232.50 ns/evaluation**;
- 33825 nodes: **295741.31 ns/evaluation**.

The large-tree slope is about **8.74 ns/node**, approximately linear. The original problem was policy boundedness: depth/fan-out limits still admitted huge hot-path trees.

The implementer repaired this in `b3e2c47` by imposing `MAX_CONDITION_NODES = 4096` **across the entire program**, summed over all operations during program validation. The adversarial holdout is behavior-only: it discovers the accepted boundary, proves `boundary + 1` fails closed and proves that two individually valid operation trees whose aggregate exceeds the boundary are also rejected.

Evidence: workflow `s20-adversarial-condition-budget`, run `35070996485`, **SUCCESS** without weakening the holdout.

NFR-009 remains covered in `ModelGeometryProvider`: root-only changes and `motionBetween(...)` reuse cached joints, while a genuinely distinct authority input increments joint evaluation exactly once.

Detailed cost evidence lives in `S20-adversarial-performance-model.md`.

## 5. I5 — catalog transfer

The accepted S20 protocol bundle carries the Citadel program map beside geometry, vanilla pose programs and canonical bindings. The receive side validates Citadel programs before committing a complete revision.

The closeout holdout `citadelProgramsAndBindingsMustRoundTripAtomicallyInCatalogBundle` encodes and accepts a bundle containing representative Gazelle and Grizzly Citadel programs/bindings. The separate I9 executable-binding property remains the failing part.

## 6. I6 — optional external adapter boundary

External model/entity state is sampled through neutral `PoseChannelAdapter` registrations. Optional Alex/Citadel-specific discovery is outside the hot evaluator and does not introduce a required external runtime dependency into the ordinary Scale JAR. Reflection and `MethodHandle` discovery happen during adapter installation/preparation. HOT_TICK only invokes already-bound handles and publishes validated scalar channels. Rejected samples fail closed transactionally after the atomicity repair above.

A source-level adversarial read found no parsing, resource lookup or reflection discovery inside `CitadelPoseProgramEvaluator.evaluate(...)` or the bound Citadel engine path. This must be repeated once I9 changes production integration, because a clean evaluator does not excuse moving resource/preparation work into the runtime binding path.

## 7. I7/I8 — representative pair

Pinned program resources exist for:

- `alexsmobs:gazelle`;
- `alexsmobs:grizzly_bear`.

They are used by the real-model oracle, which proves the shared evaluator against the original family implementation rather than against another copy of Scale's formulas.

## 8. I9 — sole remaining production blocker

Independent holdout:

- test: `S20AdversarialCanonicalBindingTests.representativeCitadelBindingsMustBecomeExecutableWithoutExternalClasses`;
- workflow: `s20-adversarial-canonical-binding`;
- hardened failing run: **35060148157**, SHA `35e1e03cc41533384a31b887a979b1ed397e510a`;
- general build on the same SHA: **SUCCESS**;
- result: **FAIL**, exactly at the missing executable Gazelle binding.

The accepted revision preserves canonical Citadel bindings and selection can resolve them, but the executable runtime map is still built only from the legacy compatibility bridge. The holdout fails before its stronger execution assertions because `snapshot.bindings().get(alexsmobs:gazelle)` is absent.

The holdout protects more than map presence. Once materialization exists it additionally requires that the prepared binding:

- preserves the selected model identity;
- preserves the selected root-provider id and resolved root authority;
- uses the shared `scalebrews:citadel_program` engine;
- retains `parameters.program` and the declared custom-channel contract;
- rejects evaluation when the required `probe` channel is absent;
- successfully evaluates the neutral bound program when that channel is present.

A second I9 boundary must be repaired at the same time. `AnatomyRuntime.bindIfEligible(...)` currently creates the server-driven provider with:

`AnatomyPoseEligibility.supported(binding.legacyPoseProvider(), entity)`.

`AnatomyPoseEligibility` owns legacy procedural-provider guards and defaults unknown provider IDs to unsupported. `ModelGeometryProvider.tick(...)` feeds that predicate into `AuthorityPoseTracker.tick(...)`, where it contributes to the `ordinary`/supported-pose state. Consequently, merely inserting canonical Citadel bindings into `Snapshot.bindings()` is insufficient if canonical runtime execution still requires a nominal legacy pose-provider identity.

Required repair properties, without prescribing implementation:

> Any accepted canonical binding whose geometry, pose engine/program, declared channels and root provider all validate must be materialized into the executable accepted revision. Executability must not be restricted to the legacy compatibility bridge, and preparation must preserve the exact selected parameters/channels/model/root rather than rebuild a legacy-shaped approximation.

> Canonical bindings must not depend on a nominal legacy pose provider for runtime eligibility. Legacy state guards may remain on the compatibility bridge; canonical/data-backed bindings must derive availability from canonical inputs, declared channels/adapters and the bound engine's fail-closed contract.

After the implementer changes that path, the existing I9 workflow must turn green **without weakening the test**. The adversary must then add/execute a live-runtime holdout proving that a canonical bound sample is not suppressed by legacy eligibility before I9 is closed.

## 9. No hard Alex/Citadel dependency

The normal production dependency metadata continues to require Minecraft/Fabric rather than Alex's Mobs or Citadel. Family-specific proof dependencies are CI/test inputs, while common-side runtime structures are neutral Scale DTOs/engines. The I9 blocker is catalog/runtime preparation and lifecycle ownership, not a hard-dependency failure.

## 10. I10 — PASS after boundedness repair

`S20-citadel-runtime-authoring-guide.md` now documents the complete accepted schema limits, including the **4096 condition-node total per program**. The adversarial condition-budget workflow verifies the contract by behavior rather than by inspecting the implementation.

I10 evidence:

- initial pathological measurement: run `35059646199`;
- behavior-only red before repair: run `35068925788`, where the 33825-node program was still accepted;
- production fix: `b3e2c47`, `fix(s20): bound program-wide condition nodes`;
- unchanged behavior-only green after repair: run `35070996485`, **SUCCESS**;
- updated authoring guidance and performance model describe the accepted 4096-node program-wide bound.

I10 is therefore **PASS**. It is no longer a reason to hold S20 open.

## 11. Prepared historical regression lane

During closeout the cross-generation `s08-prepared-adversarial-proof` lane was found to have stale test assumptions and incomplete path triggers. The adversarial cleanup is test/CI-only and does not change production semantics:

- family pose export now uses canonical source IDs instead of obsolete `proof:*` identities;
- Feline/Equine fixtures now supply the complete canonical authoritative channel set from their render states;
- the legacy endpoint-carry fallback proof now derives expected motion from the exact `SurfaceContact.localPoint()` published at acquisition rather than assuming an entity origin is the material anchor;
- workflow paths now track the actual fixtures it executes;
- workflow run evidence is now retained as JUnit/console/log artifacts even on failure.

The client export portion is already green through cow, 640 additional vanilla-family comparisons and player wide/slim. The prepared server batch still has a later red under investigation. This historical-lane cleanup does not create a new S20 production blocker, but the final branch should not silently carry an unexplained cross-generation regression.

## 12. Closeout gate

Do not mark S20 `CLOSED` until all of the following are true in the same branch lineage:

1. `s20-adversarial-canonical-binding` is green;
2. a live canonical runtime sample is proven not to depend on legacy pose eligibility;
3. `s20-adversarial-condition-budget` remains green;
4. the real-model oracle remains green;
5. the pose-channel transaction holdout remains green;
6. operation CPU scaling remains classified as non-superlinear and allocation evidence remains accepted;
7. build is green;
8. the PREPARATION/HOT_TICK adversarial read is repeated after the I9 production fix and remains clean;
9. the canonical S20 checklist/evidence is reconciled with the actual implementation;
10. the prepared cross-generation regression lane is either green or any remaining red is explicitly classified with independent evidence;
11. no S21 change regresses the S20 executable binding path.
