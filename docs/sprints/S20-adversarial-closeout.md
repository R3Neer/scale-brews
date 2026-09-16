# S20 — Adversarial closeout state

Status: **CLOSED — INDEPENDENT ADVERSARIAL ACCEPTANCE COMPLETE**.

This document records the independent adversarial closeout of S20 after the Citadel pose-program implementation, the adversarial bug/fix cycles, the canonical runtime repair and the final PREPARATION/HOT_TICK reread. It complements the canonical sprint checklist in `S20-citadel-pose-program-engine.md`; both documents are now reconciled.

## 1. Executive result

S20's reusable Citadel evaluator, optional authoritative-channel boundary, catalog transfer format, representative program data, real-model oracle, executable canonical binding path and bounded hot-path program contract are accepted on this branch lineage.

Final classification:

- I5 catalog/data transfer: **PASS**;
- I6 external channel adapter boundary: **PASS after adversarial bug/fix cycle**;
- I7 representative Grizzly/Gazelle program data: **PASS**;
- I8 original-model parity oracle: **PASS**;
- I9 canonical runtime integration without hard external dependency: **PASS after production repair + live-runtime mutation proof**;
- I10 runtime/limits/authoring contract: **PASS after adversarial boundedness bug/fix cycle**.

No unresolved S20 production blocker remains. The historical S08 cross-generation lane is still separate proof debt, but it is explicitly classified below with executable evidence and predates the S20 I9 repair.

## 2. External channel transaction bug and repair

The adversarial pass found that `AuthorityPoseTracker.sampleExternalChannels` could previously publish an adapter's valid prefix and then return failure after a later non-finite value, ownership conflict, duplicate or budget overflow. That violated fail-closed semantics because partial authoritative truth escaped from a rejected adapter sample.

The implementer changed publication to stage the whole adapter output, validate it completely and call `putAll` only after success. The independent holdout covers:

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

The hot evaluator does not reconstruct keyframe deltas on every sample: program binding compiles keyframe state up front and sample lookup uses binary search. Allocation and CPU scaling were measured independently rather than inferred from code shape.

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

The closeout holdout `citadelProgramsAndBindingsMustRoundTripAtomicallyInCatalogBundle` encodes and accepts a bundle containing representative Gazelle and Grizzly Citadel programs/bindings.

The historical S18 proof briefly remained pinned to protocol v5 after the accepted bundle advanced to v6. That was a stale proof contract rather than a production regression. Test-only commit `5849637` advanced the proof expectation, and workflow `s18-pose-engine-proof`, run `35079429984`, is **SUCCESS**.

## 6. I6 — optional external adapter boundary and HOT_TICK reread

External model/entity state is sampled through neutral `PoseChannelAdapter` registrations. Optional Alex/Citadel-specific discovery is outside the hot evaluator and does not introduce a required external runtime dependency into the ordinary Scale JAR. Rejected samples fail closed transactionally after the atomicity repair above.

The required PREPARATION/HOT_TICK adversarial read was repeated **after** the I9 production fix. Result: **clean**.

The reread traced the relevant boundary as follows:

- `WorldAnatomyCatalog.replaceValidated(...)` and `prepareCanonical(...)` validate and resolve model, engine, parameters, declared channels and root provider while accepting/preparing the revision;
- `CitadelPoseEngine.bind(...)` resolves the revision-local program and validates the required channels;
- `CitadelPoseProgramEvaluator.bind(...)` validates source/version/bones and precompiles immutable evaluator state, including clips and rest-pose data;
- `AnatomyRuntime.bindIfEligible(...)` consumes the already-materialized accepted binding;
- `ModelGeometryProvider.tick(...)`, the bound evaluator and `AuthorityPoseTracker.sampleExternalChannels(...)` consume prepared engines/providers/adapters and scalar channel values.

No JSON parsing, filesystem/resource lookup, pose-engine binding, root-provider resolution or reflection discovery was found in the S20 **HOT_TICK** path. Reflection/`MethodHandle` discovery for optional adapters remains installation/preparation work.

## 7. I7/I8 — representative pair

Pinned program resources exist for:

- `alexsmobs:gazelle`;
- `alexsmobs:grizzly_bear`.

They are used by the real-model oracle, which proves the shared evaluator against the original family implementation rather than against another copy of Scale's formulas.

## 8. I9 — PASS after canonical materialization and live-runtime mutation proof

The adversarial I9 sequence deliberately preserved the original red before accepting the repair.

Historical failing evidence:

- test: `S20AdversarialCanonicalBindingTests.representativeCitadelBindingsMustBecomeExecutableWithoutExternalClasses`;
- workflow: `s20-adversarial-canonical-binding`;
- failing run: `35060148157`, SHA `35e1e03cc41533384a31b887a979b1ed397e510a`;
- failure: the accepted canonical binding was absent from executable `Snapshot.bindings()`.

Production repair:

- commit `4554cc0`, `fix(s20): materialize canonical pose bindings`;
- `WorldAnatomyCatalog.prepareCanonical(...)` now materializes the accepted canonical binding using the exact selected model, pose engine, parameters, declared channels and root provider;
- canonical bindings carry `legacyPoseProvider = null`;
- `Binding.supportsAuthorityPose(...)` applies `AnatomyPoseEligibility` only when a legacy compatibility provider actually exists.

The original canonical-binding holdout then turned green without weakening its execution assertions: workflow `s20-adversarial-canonical-binding`, run `35073158624`, **SUCCESS**.

That catalog-level green was intentionally not treated as sufficient. The adversary added `S20AdversarialCanonicalRuntimeTests` plus workflow `s20-adversarial-canonical-runtime`, which starts the real `AnatomyRuntime`, installs a synthetic canonical binding, crosses `bindIfEligible(...)`, observes the real `ModelGeometryProvider` and requires an authoritative ordinary pose sample.

The same workflow contains a mutation kill that changes the canonical eligibility boundary so a legacy provider becomes mandatory. Baseline must stay green and the mutant must fail. This proves causality rather than merely observing a convenient green path.

Post-integration evidence: workflow `s20-adversarial-canonical-runtime`, run `35079377725`, **SUCCESS**, including mutation kill, after the later runtime ownership change at `6aa8a4a`.

I9 therefore proves both required properties:

1. an accepted canonical Citadel binding becomes executable with its exact accepted model/root/engine/parameters/channels;
2. canonical runtime pose availability does **not** depend on nominal legacy pose eligibility.

The adversarial agent did not patch production logic to obtain this result.

## 9. No hard Alex/Citadel dependency

The normal production dependency metadata continues to require Minecraft/Fabric rather than Alex's Mobs or Citadel. Family-specific proof dependencies are CI/test inputs, while common-side runtime structures are neutral Scale DTOs/engines. S20's canonical runtime path is therefore data-backed without converting Alex/Citadel into a hard runtime dependency.

## 10. I10 — PASS after boundedness repair

`S20-citadel-runtime-authoring-guide.md` documents the complete accepted schema limits, including the **4096 condition-node total per program**. The adversarial condition-budget workflow verifies the contract by behavior rather than by inspecting the implementation.

I10 evidence:

- initial pathological measurement: run `35059646199`;
- behavior-only red before repair: run `35068925788`, where the 33825-node program was still accepted;
- production fix: `b3e2c47`, `fix(s20): bound program-wide condition nodes`;
- unchanged behavior-only green after repair: run `35070996485`, **SUCCESS**;
- updated authoring guidance and performance model describe the accepted 4096-node program-wide bound.

I10 is **PASS**.

## 11. S08 historical cross-generation lane — classified, not an S20 blocker

The cross-generation `s08-prepared-adversarial-proof` lane remains separate historical proof debt, but it no longer constitutes an unexplained S20 red.

Chronology matters:

- first known red in the current lineage is run `35059084565`, run number 100, at commit `63101f5` (`fix(s21): decouple root authority from legacy bridge pair`), timestamped before the S20 I9 repair `4554cc0`;
- that run failed earlier in `AnatomyExportProof.checkPose()` through `Optional.orElseThrow`, before the later occupied-boat carry assertion was even reached;
- subsequent test/fixture cleanup exposed a different stale expectation in the prepared occupied-boat proof.

Independent diagnostics then isolated the current behavior rather than inferring it from the final boat position:

- eligibility passes;
- `replacesPair` passes;
- the vanilla entity-collision query no longer contributes the giant support AABB;
- the boat acquires a real anatomical `SurfaceContact`;
- with the unfiltered exported player geometry, that contact is `root/head/hat/cube_0`, and the boat height matches the outer `hat` geometry rather than the vanilla entity box.

Evidence: `s08-implementer-boat-contact-diagnostic`, run `35081094150`, **SUCCESS**.

This classifies the later red as a fixture/physical-filter specificity issue: an exported player contains a visible second skin layer (`hat`, and analogous jacket/sleeve/pants parts), so an assertion hard-coded to `root/head/cube_0` is only valid when the physical anatomy filter explicitly excludes those cosmetic parts. Test-only commit `22a8658` codifies that FR-020 boundary without changing production semantics.

Therefore the S08 lane is **historical cross-generation proof debt, independently classified and pre-existing before the S20 I9 fix**. It is not evidence that S20 fell back to a giant vanilla AABB and it is not an S20 canonical-pose regression.

## 12. Closeout gate — SATISFIED

All S20 closeout conditions are satisfied in the same branch lineage:

1. `s20-adversarial-canonical-binding`: run `35073158624`, **SUCCESS**;
2. live canonical runtime independent of legacy pose eligibility: `s20-adversarial-canonical-runtime`, run `35079377725`, **SUCCESS**, with mutation kill;
3. `s20-adversarial-condition-budget`: run `35070996485`, **SUCCESS**;
4. real-model oracle: run `35014694555`, **SUCCESS**;
5. pose-channel transaction holdout: run `35012624020`, **SUCCESS**;
6. CPU scaling remains non-superlinear and allocation evidence remains accepted: runs `35059646199` and `35014967562`;
7. build after the relevant runtime/test integration: run `35081094083`, **SUCCESS**;
8. PREPARATION/HOT_TICK adversarial reread repeated after `4554cc0`: **clean**, as recorded in section 6;
9. canonical S20 checklist/evidence reconciled in `S20-citadel-pose-program-engine.md`;
10. remaining S08 red explicitly classified with independent executable evidence, as recorded in section 11;
11. later runtime ownership work does not regress the S20 executable binding path: post-change live-runtime run `35079377725` remains **SUCCESS**.

S20 is therefore **CLOSED** on this branch lineage. Further S08 cleanup belongs to its historical/cross-generation proof work rather than reopening S20 unless new evidence demonstrates an actual S20 production regression.
