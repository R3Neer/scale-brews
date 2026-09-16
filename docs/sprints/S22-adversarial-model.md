# S22 adversarial model — coverage classification kernel

Status: **OPEN — canonical digest + acceptance completeness RED**.

> Role: adversarial verification only. This document does not authorize production changes.
>
> Sprint under test: `S22-coverage-classification-kernel.md`.

## 1. Threat model

S22 turns discovered entity ids plus canonical binding evidence into a reproducible acceptance artifact. The dangerous failures are therefore omissions, optimistic classification and non-injective artifact identity.

The adversarial pass treats these properties as mandatory:

1. **Total classification**
   - every discovered target id appears exactly once;
   - duplicate discovery entries cannot manufacture duplicate rows;
   - no binding/exclusion state can silently disappear from the classification.
2. **Fail-closed semantics**
   - missing evidence remains `UNRESOLVED`;
   - explicit `SAFE_PARTIAL` cannot manufacture coverage;
   - explicit `EXCLUDED` cannot hide canonical bindings;
   - known state gaps on any applicable binding cannot be promoted to `FULL` by a cleaner sibling binding.
3. **Determinism**
   - discovery order, map insertion order and binding insertion order do not change equivalent output;
   - reasons/evidence use stable ordering;
   - target version/input identity participates in the acceptance artifact.
4. **Injective canonical identity**
   - semantically distinct evidence must not serialize to the same canonical preimage;
   - delimiter characters allowed by canonical data must be escaped or length-framed;
   - digest equality may represent equivalent canonical evidence, never an ambiguity introduced before SHA-256.
5. **Discovery/acceptance coupling**
   - tooling discovers the intended `LivingEntity` target without spawning entities;
   - namespace scoping happens as part of discovery;
   - an acceptance artifact cannot claim `resolved` while omitting ids from the target's automatic discovery;
   - discovery/scanning remains outside runtime hot paths.

## 2. Current implementation status

The implementer kernel already covers default/variant/state classification, unresolved gating, explicit exclusions, deterministic ordering, target input identity and automatic registry discovery. Commit `b1b2681` aggregates excluded states across all applicable bindings so a clean default cannot hide a variant state gap. Commit `4bea96b` moved LivingEntity discovery to `DefaultAttributes.hasSupplier(type)`, and `74217f5` added implementer proof for that boundary.

Two independent adversarial blockers remain:

1. canonical binding evidence has an ambiguous pre-hash encoding;
2. the public acceptance `Artifact` can be directly assembled with an incomplete report and still satisfy `requireResolved()`.

## 3. Canonical digest ambiguity — RED / CURRENT BLOCKER

`CollisionBinding` permits arbitrary variant **values** up to the configured length. Variant keys are restricted, but values may legally contain `,` and `=`.

`CollisionCoverageScanner.BindingEvidence.canonical()` currently serializes each selector entry as:

`escape(key) + "=" + escape(value) + ","`

while `escape()` escapes backslash, `|`, newline and carriage return, but not `=` or `,`.

Therefore these two valid, semantically different selectors alias before hashing:

- `{a = "b,c=d"}`
- `{a = "b", c = "d"}`

Both produce the same selector fragment:

`a=b,c=d,`

With otherwise identical binding evidence they produce the same complete coverage canonical text and consequently the same SHA-256 digest. This is not a cryptographic collision; it is an ambiguous preimage encoding.

Independent holdout:

- `S22AdversarialCoverageDigestTests.distinctVariantSelectorsMustNotAliasCanonicalCoverageDigest`;
- test commit `7b75b86`, `test(s22): expose ambiguous coverage digest encoding`;
- workflow `s22-adversarial-coverage-digest`;
- workflow commit `246e226`, `ci(s22): run adversarial coverage digest holdout`;
- ordinary build on the same SHA: run `35098294841` **SUCCESS**;
- adversarial run `35098295265`: **FAIL**.

The failure is exact and occurs after both reports have retained distinct semantic selector evidence:

`NFR-028/NFR-036: distinct selector evidence must not alias in canonical coverage serialization; escape or length-frame variant key/value boundaries before hashing`

Repair property, intentionally non-prescriptive:

> The canonical coverage preimage must be injective over every value accepted by the canonical binding schema. Two semantically distinct binding-evidence records must not become byte-identical merely because allowed data contains serialization delimiters.

The adversarial test remains red until production satisfies that property. Mutation adequacy should be added only after the unchanged baseline becomes green.

## 4. Acceptance completeness bypass — RED / CURRENT BLOCKER

FR-038 requires the reproducible report to enumerate every automatically discovered target exactly once and forbids silently omitted rows. FR-039/FR-040 then use `UNRESOLVED=0` as an acceptance condition.

The canonical `CollisionCoverageDiscovery.scan(...)` path is complete: it discovers target ids first and classifies that set. However, `CollisionCoverageDiscovery.Artifact` is publicly constructible from an arbitrary `Report`, and its `requireResolved()` delegates only to `Report.requireResolved()`. A report with zero rows therefore has zero `UNRESOLVED` rows and passes the gate even when automatic discovery for the declared target contains many LivingEntity ids.

Independent holdout:

- `S22AdversarialCoverageCompletenessTests.manuallyAssembledArtifactCannotResolveWhileOmittingDiscoveredTargets`;
- test commit `569324e`, `test(s22): reject incomplete resolved coverage artifacts`;
- workflow `s22-adversarial-coverage-completeness`;
- workflow commit `0c6dffd`, `ci(s22): run coverage completeness holdout`;
- ordinary build on the same SHA: run `35098751232` **SUCCESS**;
- adversarial run `35098751358`: **FAIL**.

The fixture first executes real automatic discovery for the Minecraft 26.2 target and verifies that it contains `minecraft:cow` and multiple LivingEntity ids. It then directly constructs an empty `Artifact` and calls `requireResolved()`.

The exact failure is:

`FR-038/FR-039/FR-040: an acceptance artifact that silently omits discovered LivingEntity rows must not satisfy requireResolved(); completeness must be tied to automatic discovery`

Repair property, intentionally non-prescriptive:

> Any object or operation presented as the S22 acceptance gate must prove report completeness against the declared target's discovered identity set. Omitting rows must fail closed; an empty/incomplete report must never become equivalent to `UNRESOLVED=0` merely because no unresolved rows were supplied.

This does not require `Report` itself to perform registry discovery. The implementation may instead make complete artifacts constructible only through a discovery-backed path, carry/validate discovery identity in the artifact, or otherwise enforce the same property without moving discovery into runtime code.

The adversarial holdout remains red until production satisfies that property. Mutation adequacy should be added only after the unchanged baseline becomes green.

## 5. Next adversarial targets

After these two reds are repaired:

1. rerun both unchanged holdouts and add semantic mutation adequacy for each repair;
2. probe target/discovery identity for namespace and version-input ambiguity;
3. audit deterministic state-gap aggregation across mixed default + multiple variant bindings;
4. verify no low-level/public gate can bypass target membership by supplying duplicate, foreign or partial rows;
5. perform a final read-through proving discovery/scanning remains absent from runtime/hot-tick paths.

S21 remains independently open on its entity-removal lifecycle red; S22 progress does not waive that gate.
