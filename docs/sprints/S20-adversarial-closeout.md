# S20 — Adversarial closeout state

Status: **NOT CLOSED — I9 RED**.

This document records the independent adversarial state of S20 after the Citadel pose-program implementation reached real-model parity. It does not replace the canonical sprint checklist and deliberately does not mark S20 closed while its canonical runtime binding path is still failing.

## 1. Executive result

S20's reusable Citadel evaluator, optional authoritative-channel boundary, catalog transfer format, representative program data and real-model oracle are materially present. The sprint still has one functional closeout blocker: an accepted canonical `scalebrews:citadel_program` binding is validated and transferred but is not materialized into `WorldAnatomyCatalog.Snapshot.bindings()`, so `AnatomyRuntime` cannot execute it through the canonical runtime path.

The independent closeout therefore classifies:

- I5 catalog/data transfer: **PASS**;
- I6 external channel adapter boundary: **PASS after adversarial bug/fix cycle**;
- I7 representative Grizzly/Gazelle program data: **PASS**;
- I8 original-model parity oracle: **PASS**;
- I9 canonical runtime integration without hard external dependency: **FAIL / OPEN**;
- I10 runtime/limits/authoring documentation: **PASS**, with this closeout and `S20-citadel-runtime-authoring-guide.md`.

S20 must remain open until the I9 holdout is green on the canonical implementation and the final sprint checklist is reconciled.

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

The hot evaluator no longer reconstructs keyframe deltas on every sample: program binding compiles keyframe state up front and sample lookup uses binary search. The remaining allocation cost is real and was measured rather than described qualitatively.

Measured CI/JVM allocation figures:

- synthetic 1 touched bone: approximately 936 B/evaluation;
- synthetic 16 touched bones: approximately 6.85 KiB/evaluation;
- marginal slope: approximately 395 B per additional touched bone;
- pinned Gazelle: approximately 3.90 KiB ordinary / 4.18 KiB clip sample;
- pinned Grizzly Bear: approximately 5.17 KiB ordinary / 5.45 KiB clip sample.

Observed scaling is approximately linear. Allocation remains material technical debt worth monitoring, but the measurements do not show a superlinear/explosive evaluator and NFR-009 separately constrains joint evaluation to one causal pose sample rather than repeated work for root-only changes.

## 5. I5 — catalog transfer

The accepted S20 protocol bundle carries the Citadel program map beside geometry, vanilla pose programs and canonical bindings. The receive side validates Citadel programs before committing a complete revision.

The closeout holdout `citadelProgramsAndBindingsMustRoundTripAtomicallyInCatalogBundle` encodes and accepts a bundle containing representative Gazelle and Grizzly Citadel programs/bindings. In the isolated run only the separate I9 executable-binding test failed, so the transfer/selection round-trip passed.

## 6. I6 — optional external adapter boundary

External model/entity state is sampled through neutral `PoseChannelAdapter` registrations. Optional Alex/Citadel-specific discovery is outside the hot evaluator and does not introduce a required external runtime dependency into the ordinary Scale JAR. Reflection/method-handle discovery is preparation work; rejected samples fail closed transactionally after the atomicity repair above.

## 7. I7/I8 — representative pair

Pinned program resources exist for:

- `alexsmobs:gazelle`;
- `alexsmobs:grizzly_bear`.

They are used by the real-model oracle, which proves the shared evaluator against the original family implementation rather than against another copy of Scale's formulas.

## 8. I9 — canonical runtime integration blocker

Independent holdout:

- test: `S20AdversarialCanonicalBindingTests.representativeCitadelBindingsMustBecomeExecutableWithoutExternalClasses`;
- workflow: `s20-adversarial-canonical-binding`;
- run: `35018925533`;
- result: **FAIL**.

The candidate revision accepts canonical Citadel bindings and canonical selection resolves them, but the executable runtime map is built only from the legacy compatibility bridge. The holdout fails at the first representative entity with:

`S20 I9: accepted Citadel binding must be prepared into the executable catalog for alexsmobs:gazelle`

This is not an oracle/harness/compile failure. The same run compiled, started the dedicated GameTest server and failed only the I9 runtime-executability property.

Required repair property, without prescribing implementation:

> Any accepted canonical binding whose geometry, pose engine/program, declared channels and root provider all validate must be materialized into the executable accepted revision. Executability must not be restricted to the legacy compatibility bridge.

After the implementer changes that path, the existing I9 workflow must turn green without weakening the test.

## 9. No hard Alex/Citadel dependency

The normal production dependency metadata continues to require Minecraft/Fabric rather than Alex's Mobs or Citadel. Family-specific proof dependencies are CI/test inputs, while common-side runtime structures are neutral Scale DTOs/engines. The I9 blocker is therefore catalog/runtime preparation, not a hard-dependency failure.

## 10. I10 — authoring and ownership guidance

`S20-citadel-runtime-authoring-guide.md` documents:

- ownership split between geometry, local-joint program and root authority;
- HOT_TICK restrictions;
- fail-closed behavior;
- exact schema/program bounds;
- external channel adapter role;
- performance measurements;
- reproducible steps for adding another species covered by the same family technology;
- requirement for original-model parity and canonical runtime integration.

The guide explicitly refuses to treat real-model evaluator parity as proof of canonical binding executability. That distinction is the reason the I9 blocker was found.

## 11. Closeout gate

Do not mark S20 `CLOSED` until all of the following are true in the same branch lineage:

1. `s20-adversarial-canonical-binding` is green;
2. the real-model oracle remains green;
3. the pose-channel transaction holdout remains green;
4. build is green;
5. the canonical S20 checklist/evidence is updated to reflect the actual implementation;
6. no S21 change regresses the S20 executable binding path.
