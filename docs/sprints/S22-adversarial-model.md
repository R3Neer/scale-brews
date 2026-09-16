# S22 adversarial model — coverage classification kernel

Status: **OPEN — canonical coverage digest RED**.

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

The implementer kernel covers default/variant/state classification, unresolved gating, explicit exclusions, deterministic ordering, target input identity and automatic registry discovery. Commit `b1b2681` aggregates excluded states across all applicable bindings so a clean default cannot hide a variant state gap. Commit `4bea96b` moved LivingEntity discovery to `DefaultAttributes.hasSupplier(type)`, and `74217f5` added implementer proof for that boundary.

The former acceptance-completeness bypass is now closed independently and mutation-protected. The remaining confirmed S22 blocker is the ambiguous canonical pre-hash encoding described below.

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

## 4. Acceptance completeness — HISTORICAL RED → PASS + MUTATION ADEQUATE

FR-038 requires the reproducible report to enumerate every automatically discovered target exactly once and forbids silently omitted rows. FR-039/FR-040 then use `UNRESOLVED=0` as an acceptance condition.

The initial canonical `scan(...)` path was complete, but `CollisionCoverageDiscovery.Artifact` could be directly assembled from an arbitrary `Report`, and its gate only checked for `UNRESOLVED` rows. An empty report therefore passed despite omitting the target's discovered population.

Historical red evidence:

- `S22AdversarialCoverageCompletenessTests.manuallyAssembledArtifactCannotResolveWhileOmittingDiscoveredTargets`;
- test commit `569324e`, `test(s22): reject incomplete resolved coverage artifacts`;
- initial workflow commit `0c6dffd`, `ci(s22): run coverage completeness holdout`;
- ordinary build on that SHA: run `35098751232` **SUCCESS**;
- adversarial run `35098751358`: **FAIL** exactly at:

`FR-038/FR-039/FR-040: an acceptance artifact that silently omits discovered LivingEntity rows must not satisfy requireResolved(); completeness must be tied to automatic discovery`

The implementer repair `4484fd7` (`fix(s22): bind artifact completeness to discovery`) makes the `Artifact` constructor rediscover the declared target and require exact ordered equality between discovered ids and report-row ids. The unchanged adversarial holdout then passed:

- run `35099137468`: **SUCCESS**.

Mutation adequacy was added in workflow lineage `80a946e`. The semantic mutant replaces discovery-backed expected membership with the report's own row ids, recreating a self-justifying incomplete artifact while preserving the public API. It compiles and is killed by the same unchanged holdout:

- run `35099430830`: baseline **SUCCESS**, mutation-kill **SUCCESS**.

This closes the acceptance-completeness bypass independently. It is no longer an S22 blocker.

## 5. Discarded discovery-parity experiment — TEST ORACLE INVALID

An attempted exhaustive cross-check compared attribute-backed discovery against `LivingEntity.class.isAssignableFrom(type.getBaseClass())`. Run `35099359378` was red, but the reference set was empty while attribute-backed discovery contained the expected vanilla living population. In Minecraft 26.2, `EntityType.getBaseClass()` is therefore not a valid static oracle for LivingEntity membership in this context.

No production claim is made from that red. The test and workflow were removed in commits `a44992b` and `fae7089` so a known-invalid oracle does not remain as fake debt.

The current discovery evidence remains the production criterion plus its implementer smoke/boundary tests until an actually independent, non-instantiating oracle is available.

## 6. Next adversarial targets

The remaining S22 sequence is deliberately narrow:

1. keep `s22-adversarial-coverage-digest` red and unchanged while the implementer repairs canonical framing;
2. rerun that same digest holdout after the fix and add a semantic mutant that restores ambiguous framing;
3. audit deterministic state-gap aggregation across mixed default + multiple variant bindings without duplicating existing implementer cases;
4. verify no other low-level/public acceptance path can bypass target membership by supplying foreign or partial rows;
5. perform a final read-through proving discovery/scanning remains absent from runtime/hot-tick paths.

S21 remains independently open on its entity-removal lifecycle red; S22 progress does not waive that gate.

## 7. Gate rule

S22 must not be considered adversarially converged while:

- semantically distinct accepted binding evidence can alias in canonical serialization or digest identity;
- an acceptance artifact can omit discovered target ids and still satisfy the resolved gate;
- a clean binding can hide a known state gap from another applicable binding;
- discovery or coverage scanning leaks into runtime/hot-tick code;
- target namespace/version/input identity is not represented deterministically in the acceptance artifact.
