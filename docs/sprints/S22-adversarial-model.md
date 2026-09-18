# S22 adversarial model — coverage classification kernel

Status: **CLOSED — provenance recertified with bidirectional mutation adequacy**.

> Role: adversarial verification only. This document does not authorize production changes.
>
> Sprint under test: `S22-coverage-classification-kernel.md`.

## 1. Threat model

S22 turns discovered entity ids plus canonical binding evidence into a reproducible acceptance artifact. The dangerous failures are omissions, optimistic classification, forged semantic rows, non-injective identity and accidental runtime coupling.

The adversarial pass treats these properties as mandatory:

1. **Total classification**
   - every discovered target id appears exactly once;
   - duplicate discovery entries cannot manufacture duplicate rows;
   - no binding/exclusion state can silently disappear from the classification.
2. **Fail-closed semantics and provenance**
   - missing evidence remains `UNRESOLVED`;
   - explicit `SAFE_PARTIAL` cannot manufacture coverage;
   - explicit `EXCLUDED` cannot hide canonical bindings;
   - known state gaps on any applicable binding cannot be promoted to `FULL` by a cleaner sibling binding;
   - exact discovery membership is necessary but not sufficient: an acceptance artifact may not trust a manually fabricated `FULL/SAFE_PARTIAL/EXCLUDED` classification that did not come from canonical scanner evidence.
3. **Determinism**
   - discovery order, map insertion order and binding insertion order do not change equivalent output;
   - reasons/evidence use stable ordering;
   - target version/input identity participates in the acceptance artifact.
4. **Injective canonical identity**
   - semantically distinct evidence must not serialize to the same canonical preimage;
   - every accepted variable-length field is unambiguously framed;
   - digest equality may represent equivalent canonical evidence, never an ambiguity introduced before SHA-256.
5. **Discovery/acceptance coupling**
   - tooling discovers the intended `LivingEntity` target without spawning entities;
   - namespace scoping happens as part of discovery;
   - an acceptance artifact cannot claim `resolved` while omitting ids from automatic discovery;
   - discovery/scanning remains outside runtime/hot paths.

## 2. Current reconciled status

Current classification on the `chatgpt-editing` lineage:

- scanner base semantics and deterministic row ordering: **PASS**;
- mixed default + multiple variant state-gap aggregation: **PASS**;
- automatic registry discovery without entity spawning: **PASS**;
- target/version/input identity: **PASS**;
- exact discovery membership in `Artifact`: **PASS + MUTATION ADEQUATE**;
- canonical digest framing: **PASS after repair**;
- no production runtime callers of discovery/scanner: **PASS + structural mutation kill**;
- acceptance classification provenance/authenticity: **PASS + bidirectional mutation adequate**.

S22 has no remaining blocker in this threat model. Membership, semantic provenance, canonical issuance, digest identity, residual classification and the runtime/hot-path boundary are independently exercised.

## 3. Canonical digest ambiguity — HISTORICAL RED → PASS

`CollisionBinding` permits arbitrary variant values, including delimiter characters. The original canonical preimage concatenated selector fields using delimiters, so semantically different selectors could alias before SHA-256.

Historical holdout:

- `S22AdversarialCoverageDigestTests.distinctVariantSelectorsMustNotAliasCanonicalCoverageDigest`;
- test commit `7b75b86`, `test(s22): expose ambiguous coverage digest encoding`;
- workflow `s22-adversarial-coverage-digest`;
- ordinary build `35098294841`: **SUCCESS**;
- adversarial run `35098295265`: **FAIL**.

Production repair **`8972f6243dbe8c574dbe4d98611f1d0bb11c0cf2`** replaced delimiter escaping with length framing (`collision-coverage-v2`) for variable-length fields and binding evidence. The unchanged digest holdout then passed; implementer evidence records run **`35101622368`** as **success**. No digest blocker remains.

## 4. Acceptance completeness — HISTORICAL RED → PASS + MUTATION ADEQUATE

The first canonical `scan(...)` path was complete, but `CollisionCoverageDiscovery.Artifact` could be assembled from a partial `Report` and `requireResolved()` only checked absence of `UNRESOLVED`. An empty report therefore looked resolved.

Historical red:

- `S22AdversarialCoverageCompletenessTests.manuallyAssembledArtifactCannotResolveWhileOmittingDiscoveredTargets`;
- test commit `569324e`;
- ordinary build `35098751232`: **SUCCESS**;
- adversarial run `35098751358`: **FAIL**.

Implementer repair **`4484fd7`** rediscovered the target in the `Artifact` constructor and requires exact ordered equality between discovered ids and report-row ids. The unchanged holdout passed in **`35099137468`**.

Mutation adequacy in workflow lineage `80a946e` replaces automatic expected membership with the report's own row ids, recreating a self-justifying report. Run **`35099430830`**: baseline **SUCCESS**, mutation-kill **SUCCESS**.

Completeness is therefore closed. That repair is intentionally distinct from the provenance blocker below.

## 5. Forged classification provenance — HISTORICAL RED → PASS + MUTATION ADEQUATE

Exact membership does not authenticate the semantic content of the rows.

`CollisionCoverageScanner.Row` and `Report` are publicly constructible. `Artifact` currently re-discovers the target and checks only this equality:

`coverage.rows().map(Row::entity) == discover(target).livingEntityTypes()`.

A caller can therefore:

1. discover the correct target ids;
2. create one manual row for every id;
3. mark every row as any resolved status;
4. attach locally plausible but invented evidence where that status normally carries it;
5. construct an `Artifact` whose membership exactly matches discovery and whose rows look structurally valid;
6. call `requireResolved()` successfully because there are no `UNRESOLVED` rows.

This bypass does not omit anything and does not depend on empty evidence. It forges the classification itself while satisfying the completeness repair and status-local shape checks.

Independent holdout progression:

- original method `completeMembershipCannotForgeFullCoverageWithoutCanonicalEvidence`;
- initial test commit **`1d11409f4a61f013727ef922c9e4ae4b01d4d534`**;
- workflow commit **`e3e67f24dc1523053bf63b47878faa298ed88b2f`**;
- initial ordinary build: run **`35106752431`**, job **`104829855709`**: **SUCCESS**;
- initial adversarial run **`35106752630`**, job **`104829857809`**: **FAIL** only in the forged-provenance holdout;
- hardened holdout commit **`fe613bbf630276753789b80c6edc1e5acf6bf547`** replaces empty evidence with non-empty plausible forged `BindingEvidence` for every discovered `FULL` row;
- hardened ordinary build run **`35107336639`**, job **`104831843804`**: **SUCCESS**;
- hardened adversarial run **`35107336649`**, job **`104831844562`**: **FAIL** at the unchanged semantic gate;
- all-status hardening commit **`be10d79beeb198d9e506829c394ba57687327599`** renames the holdout to `completeMembershipCannotForgeAnyResolvedCoverageClassification` and exercises `FULL`, `SAFE_PARTIAL` and `EXCLUDED` separately, with plausible forged binding/state-gap evidence where appropriate;
- all-status ordinary build run **`35107950060`**, job **`104833930824`**: **SUCCESS**;
- all-status adversarial run **`35107949713`**, job **`104833929221`**: **FAIL** in the provenance holdout.

The failure property is:

> FR-038 / NFR-032 / NFR-036: an acceptance artifact must not satisfy the resolved gate merely because its row ids and local row shape match expectations. `FULL`, `SAFE_PARTIAL` and `EXCLUDED` acceptance claims must be bound to canonical scanner/catalog evidence rather than arbitrary caller-authored rows.

The adversary does not prescribe the implementation. A repair may bind artifacts to canonical scanner output, make trusted construction non-forgeable, or use another fail-closed provenance mechanism, but it must preserve deterministic reproducibility and the already-green completeness gate.

The all-status holdout is green on the repaired production path. The final adversarial pass adds both a forged-acceptance mutant and a canonical-issuance mutant; both are killed, so this property is now mutation adequate in both directions.

## 6. Mixed default/variant state gaps — PASS

The implementer already covered one clean default plus one variant gap. The final adversarial residual adds multiple applicable variants with overlapping excluded-state sets, changes binding insertion order and requires exact deterministic union.

`S22AdversarialCoverageResidualTests.multipleVariantStateGapsDowngradeDefaultWithoutOrderOrDuplicateNoise` asserts:

- a clean default cannot hide any state gap from sibling variants;
- union is deduplicated and lexically ordered (`angry,grazing,sleeping`);
- every applicable binding remains present in evidence;
- canonical text and digest are invariant under binding insertion order.

Evidence:

- test commit **`bcb6119db5662ffe955d988793255e2fe5e87431`**;
- residual workflow run **`35106870440`**, job **`104830262947`**: **SUCCESS**.

No production change was needed.

## 7. Runtime / HOT_TICK boundary — PASS + MUTATION ADEQUATE

S22 discovery and classification are tooling/acceptance mechanisms. They must not become runtime work merely because the implementation classes live under `src/main`.

`tools/s22_coverage_hotpath_gate.py` scans production Java and rejects references to `CollisionCoverageDiscovery` or `CollisionCoverageScanner` outside their two tooling implementation files.

Evidence:

- gate commit **`1eab17b3668ce017e1e3ea964bcf78e1fcde8cf2`**;
- workflow **`s22-adversarial-coverage-residuals`**;
- run **`35106870440`**, job **`104830262565`**: baseline source boundary **SUCCESS**;
- same run, job **`104830316050`**: **SUCCESS** after compiling an ephemeral mutant that injects a production reference to `CollisionCoverageScanner` into `AnatomyRuntime`; the source gate kills the mutant.

This is a structural boundary proof, not a performance benchmark. It establishes absence of production callers of the coverage tooling in the current tree.

## 8. Discarded discovery-parity experiment — TEST ORACLE INVALID

An attempted exhaustive cross-check compared attribute-backed discovery against `LivingEntity.class.isAssignableFrom(type.getBaseClass())`. Run `35099359378` was red, but the reference set was empty while attribute-backed discovery contained the expected vanilla living population. In Minecraft 26.2, `EntityType.getBaseClass()` is not a valid static oracle for LivingEntity membership here.

No production claim is made from that red. The test/workflow were removed in commits `a44992b` and `fae7089`.

## 9. Final adversarial sequence — COMPLETED

The planned sequence is complete:

1. the unchanged all-status forged-classification holdout is green on the implementer repair;
2. the adversarial oracle now also contains a canonical positive control, preventing blanket rejection from masquerading as provenance enforcement;
3. a semantic mutant that disables only the provenance acceptance fence is killed by the forged-classification assertions;
4. an opposite mutant that drops only canonical scanner provenance issuance is killed by the positive control;
5. completeness remains mutation-protected, digest and residual semantics remain green from the repaired lineage, and the residual/hot-path lane continued green through later production work;
6. the final zero-change compare from `50296a6...` to the adversarial closeout contains no modification to `CollisionCoverageDiscovery` or `CollisionCoverageScanner`.

S21 is independently closed. G3.8 can now close.

## 10. Gate rule

S22 is adversarially converged under this model: caller-authored resolved classifications fail closed, scanner-issued canonical classifications remain usable, and directed mutations of either side are killed. Completeness, digest framing and hot-path isolation retain their independent oracles.

## 11. Final closeout evidence — 2026-09-18

Adversarial hardening commit **`942aa4fec0b372dd63de8f7916f91a54bd66d956`** adds a positive control to `S22AdversarialCoverageProvenanceTests`: the exact automatically discovered Minecraft target is supplied with one valid canonical default binding per id through `CollisionCoverageDiscovery.scan(...)`, every row must be `FULL`, and the scanner-issued artifact must pass `requireResolved()`. The existing forged `FULL`, `SAFE_PARTIAL` and `EXCLUDED` negative controls remain unchanged in meaning.

Mutation-gate commit **`fd9ca047f3df0549f21672e94f6adaa706ace588`** adds two directed mutants to the same isolated workflow.

Run **`35328441530`**:

- baseline `forged-coverage-provenance`, job **`105546923380`**: **success**;
- `provenance-bypass-mutant-must-die`, job **`105547301841`**: **success as mutation harness**. The mutant disables only the provenance acceptance condition; the GameTest dies on the forged `FULL` claim and the harness reports `KILLED`;
- `provenance-issuance-mutant-must-die`, job **`105547301819`**: **success as mutation harness**. The mutant removes only `SCANNER_PROVENANCE` issuance from the canonical scan path; the positive control dies with `Coverage artifact lacks canonical scanner provenance` and the harness reports `KILLED`.

Artifacts:

- baseline **`10539264816`**, SHA-256 **`959a68766aea46ee2895e0e503b53de48bd7f30a29eeebdbbca4b2f5686769ef`**;
- bypass mutant **`10540212060`**, SHA-256 **`3c93fa3e7db005f8f9bd19c8e33826f525c707fa94562ed07ea2f01de85e9d95`**;
- issuance mutant **`10539374622`**, SHA-256 **`94b4eb5b9605fb7d70ec817a253b5db09135887c63cc3a711a7a41a7bb058c4b`**.

Ordinary build on the same mutation-gate snapshot, run **`35328441560`**, is **success**.

Zero-change review: comparing the final product repair **`50296a6792f523e57fcd8daa71ba6f598254a6e6`** with `fd9ca047...` shows no later change to `CollisionCoverageDiscovery.java` or `CollisionCoverageScanner.java`; S22 changes after the repair are adversarial tests/CI/docs only. The residual/hot-path workflow also remained green through later production snapshot `1437795...` (run **`35205403247`**).

**Adversarial conclusion:** S22 is closed and G3 task 8 may be marked complete.

