# S20 — Adversarial closeout state

Status: **NOT CLOSED — I9 RED / I10 BOUNDEDNESS RED**.

This document records the independent adversarial state of S20 after the Citadel pose-program implementation reached real-model parity. It does not replace the canonical sprint checklist and deliberately does not mark S20 closed while its canonical runtime binding path and bounded-condition contract are still failing.

## 1. Executive result

S20's reusable Citadel evaluator, optional authoritative-channel boundary, catalog transfer format, representative program data and real-model oracle are materially present. The adversarial closeout currently has two blockers:

1. an accepted canonical `scalebrews:citadel_program` binding is validated and transferred but is not materialized into `WorldAnatomyCatalog.Snapshot.bindings()`, so `AnatomyRuntime` cannot execute it through the canonical runtime path;
2. compound conditions have per-node cardinality and depth limits but no **program-wide** node budget, leaving HOT_TICK work without a practical schema bound.

The independent closeout therefore classifies:

- I5 catalog/data transfer: **PASS**;
- I6 external channel adapter boundary: **PASS after adversarial bug/fix cycle**;
- I7 representative Grizzly/Gazelle program data: **PASS**;
- I8 original-model parity oracle: **PASS**;
- I9 canonical runtime integration without hard external dependency: **FAIL / OPEN**;
- I10 runtime/limits/authoring contract: **FAIL / OPEN on boundedness**. Authoring guidance exists, but its stated bounds are incomplete until a program-wide condition-node budget exists and is tested.

S20 must remain open until both blockers are green and the final sprint checklist is reconciled.

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

The focused workflow isolates the oracle from unrelated GameTests/mixins and verifies the locked external inputs before execution.

Coverage in the accepted run:

- Gazelle: 4 samples, including ordinary procedural state and clip/state coverage;
- Grizzly Bear: 4 samples, including ordinary procedural state and `ModelAnimator` clip/state coverage;
- both evaluate through the same shared Scale pose engine.

Evidence: workflow `s20-citadel-pose-real-proof`, run `35014694555`, **SUCCESS**.

## 4. Performance evidence

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

This is consistent with approximately linear operation scaling and shows no superlinear evaluator regression. NFR-009 separately constrains joint evaluation to one causal pose sample rather than repeated work for root-only changes or multiple consumers.

Condition evaluation is different in policy, not complexity class. The same run measured valid full-traversal trees at:

- 1 node: **289.66 ns/evaluation**;
- 33 nodes: **538.25 ns/evaluation**;
- 1057 nodes: **9232.50 ns/evaluation**;
- 33825 nodes: **295741.31 ns/evaluation**.

The large-tree slope is about **8.74 ns/node**, again approximately linear. The blocker is that `CitadelPoseProgram` currently accepts the 33825-node tree and exposes no global condition-node contract. Linear work without a practical upper bound is still unbounded hot-path policy.

Detailed evidence lives in `S20-adversarial-performance-model.md`.

## 5. I5 — catalog transfer

The accepted S20 protocol bundle carries the Citadel program map beside geometry, vanilla pose programs and canonical bindings. The receive side validates Citadel programs before committing a complete revision.

The closeout holdout `citadelProgramsAndBindingsMustRoundTripAtomicallyInCatalogBundle` encodes and accepts a bundle containing representative Gazelle and Grizzly Citadel programs/bindings. In the isolated run only the separate I9 executable-binding test failed, so the transfer/selection round-trip passed.

## 6. I6 — optional external adapter boundary

External model/entity state is sampled through neutral `PoseChannelAdapter` registrations. Optional Alex/Citadel-specific discovery is outside the hot evaluator and does not introduce a required external runtime dependency into the ordinary Scale JAR. Reflection and `MethodHandle` discovery happen during adapter installation/preparation. HOT_TICK only invokes already-bound handles and publishes validated scalar channels. Rejected samples fail closed transactionally after the atomicity repair above.

A second source-level adversarial read on 2026-09-16 found no parsing, resource lookup or reflection discovery inside `CitadelPoseProgramEvaluator.evaluate(...)` or the bound Citadel engine path. This finding is **provisional until the I9/I10 production fixes land** and must be repeated before closure.

## 7. I7/I8 — representative pair

Pinned program resources exist for:

- `alexsmobs:gazelle`;
- `alexsmobs:grizzly_bear`.

They are used by the real-model oracle, which proves the shared evaluator against the original family implementation rather than against another copy of Scale's formulas.

## 8. I9 — canonical runtime integration blocker

Independent holdout:

- test: `S20AdversarialCanonicalBindingTests.representativeCitadelBindingsMustBecomeExecutableWithoutExternalClasses`;
- workflow: `s20-adversarial-canonical-binding`;
- hardened failing run: **35060148157**, SHA `35e1e03cc41533384a31b887a979b1ed397e510a`;
- general build on the same SHA: **SUCCESS**;
- result: **FAIL**, exactly at the missing executable Gazelle binding.

The candidate revision accepts canonical Citadel bindings and canonical selection resolves them, but the executable runtime map is built only from the legacy compatibility bridge. The holdout fails before its stronger execution assertions because `snapshot.bindings().get(alexsmobs:gazelle)` is absent.

The holdout now protects more than map presence. Once materialization exists it additionally requires that the prepared binding:

- preserves the selected model identity;
- preserves the selected root-provider id and resolved root authority;
- uses the shared `scalebrews:citadel_program` engine;
- retains `parameters.program` and the declared custom-channel contract;
- rejects evaluation when the required `probe` channel is absent;
- successfully evaluates the neutral bound program when that channel is present.

This is not an oracle/harness/compile failure. The isolated run compiled, started the dedicated GameTest server and failed only the I9 runtime-executability property; the separate catalog bundle round-trip test survives.

Required repair property, without prescribing implementation:

> Any accepted canonical binding whose geometry, pose engine/program, declared channels and root provider all validate must be materialized into the executable accepted revision. Executability must not be restricted to the legacy compatibility bridge, and preparation must preserve the exact selected parameters/channels/root rather than rebuild a legacy-shaped approximation.

After the implementer changes that path, the existing I9 workflow must turn green without weakening the test.

## 9. No hard Alex/Citadel dependency

The normal production dependency metadata continues to require Minecraft/Fabric rather than Alex's Mobs or Citadel. Family-specific proof dependencies are CI/test inputs, while common-side runtime structures are neutral Scale DTOs/engines. The I9 blocker is therefore catalog/runtime preparation, not a hard-dependency failure.

## 10. I10 — authoring guidance exists, program-wide condition bound does not

`S20-citadel-runtime-authoring-guide.md` already documents:

- ownership split between geometry, local-joint program and root authority;
- HOT_TICK restrictions;
- fail-closed behavior;
- current schema/program bounds;
- external channel adapter role;
- performance measurements;
- reproducible steps for adding another species covered by the same family technology;
- requirement for original-model parity and canonical runtime integration.

The adversarial CPU probe found that the currently documented condition limits are incomplete as a bounded-runtime contract. `ALL`/`ANY` are limited to 32 children per node and depth 16, but no global node count is enforced across the program. A valid 33825-node tree already costs about 0.296 ms/evaluation on the CI JVM, and the schema permits substantially more structure.

Independent boundedness holdout:

- test: `S20AdversarialConditionBudgetTests.publishedGlobalConditionBudgetMustAcceptLimitAndRejectLimitPlusOne`;
- workflow: `s20-adversarial-condition-budget`;
- baseline failing run: **35060266298**, SHA `2394538bc8c7767365ace2fe9dd53bb6c4fb05a1`;
- failure: no explicit immutable integer condition-node budget is declared on `CitadelPoseProgram` (`found []`).

The holdout derives its boundary from production instead of hard-coding the intended final limit. Its reflection is visibility-neutral: the bound may remain implementation-private. It requires:

1. exactly one explicit `static final int` condition-node budget;
2. a positive limit below the measured 33825-node pathological fixture;
3. exactly `limit` nodes accepted;
4. `limit + 1` nodes in one tree rejected fail-closed;
5. **program-wide aggregation**: two operations that are each individually within the limit but whose combined condition nodes equal `limit + 1` must also be rejected.

The aggregate case prevents a cosmetic per-operation cap from leaving the whole program's HOT_TICK work effectively unbounded.

I10 therefore remains open until production defines and enforces that explicit total program condition-node budget and the hardened holdout passes.

## 11. Closeout gate

Do not mark S20 `CLOSED` until all of the following are true in the same branch lineage:

1. `s20-adversarial-canonical-binding` is green;
2. the program-wide condition-node bound exists and the hardened boundary/aggregation holdout is green;
3. the real-model oracle remains green;
4. the pose-channel transaction holdout remains green;
5. operation CPU scaling remains classified as non-superlinear and allocation evidence remains accepted;
6. build is green;
7. the PREPARATION/HOT_TICK second read is repeated after the production fixes and remains clean;
8. the canonical S20 checklist/evidence is updated to reflect the actual implementation;
9. no S21 change regresses the S20 executable binding path.
