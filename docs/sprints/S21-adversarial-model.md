# S21 adversarial model — root transform authority

Status: **OPEN — entity-removal lifecycle RED**.

> Role: adversarial verification only. This document does not authorize production changes.
>
> Sprint under test: `S21-root-transform-provider.md`.

## 1. Threat model

S21 moves world-root authority out of the local-joint evaluator. The dangerous failures are therefore not merely wrong matrices; they are authority leaks, stale-root reuse, double sampling and accidental coupling between root changes and joint recomputation.

The adversarial pass treats these properties as mandatory:

1. **Root DTO integrity**
   - origin, quaternion and uniform scale are finite;
   - quaternion is safely normalizable and stored normalized;
   - caller-owned mutable quaternion state cannot mutate an accepted DTO later;
   - values returned for inspection cannot mutate the stored DTO;
   - zero/invalid quaternion and non-positive/non-finite scale fail closed.
2. **Registry ownership**
   - root providers have their own id namespace inside `CollisionEngines`;
   - duplicate root-provider ownership is rejected deterministically;
   - snapshots are immutable and deterministically ordered;
   - registering a root provider never creates or mutates a species binding.
3. **Sampling boundary**
   - a provider is sampled once per causal authoritative endpoint, not once per consumer/query;
   - `Optional.empty()`, thrown exceptions and invalid values make that endpoint unavailable;
   - failure never reuses a previous successful root as an implicit fallback;
   - client presentation/prediction consumes transported authority and never resamples an external root provider.
4. **Root/joint separation (NFR-009)**
   - root-only translation/rotation/scale changes may change geometry but must not increment local-joint evaluation count;
   - local pose changes may invalidate joints without changing the selected root authority contract;
   - root cache keys and joint cache keys remain distinct.
5. **Orientation semantics**
   - `q` and `-q` are equivalent orientations;
   - transverse external orientation is independent of physical gravity;
   - the built-in compatibility provider reproduces the pre-S21 root composition exactly;
   - no silent Euler reconstruction is introduced at an API boundary that promises quaternion authority.
6. **Lifecycle and discontinuity**
   - reload/rebind cannot retain a root sampled by an obsolete binding generation;
   - teleport/discontinuity does not interpolate through stale root authority;
   - entity removal and world/session teardown cannot keep causal root authority alive.

## 2. Current reconciled status

The early runtime tranche intentionally landed as independent red holdouts before the implementation converged. Those reds are retained below as historical evidence, but they are **not** current blockers anymore.

Current classification on the `chatgpt-editing` lineage:

- DTO/registry SPI contract: **PASS**;
- causal/query/presentation/motion consumption of transported root: **PASS**;
- canonical root-provider binding executability: **PASS**;
- pose/root wire transport and root-provider identity: **PASS**;
- unavailable root publication / stale-root clearing: **PASS**;
- gravity independence: **PASS**;
- exact `scalebrews:entity_root` compatibility: **PASS**, including mutation adequacy;
- same-tick root-only causal identity: **PASS**;
- reload/rebind root lifecycle: **PASS**, including mutation adequacy;
- external-root teleport discontinuity and recovery: **PASS**, including mutation adequacy;
- entity-removal lifecycle: **RED / current blocker**;
- session teardown: **not yet independently closed**.

The ordinary build is green on the removal-holdout SHA (`99364e4`, run `35088792862`). The removal red is therefore a behavioral product gate, not a compile or harness failure.

## 3. SPI contract — PASS

`S21AdversarialRootTransformContractTests` covers DTO normalization/immutability/invalid-state rejection and registry ownership.

Evidence:

- workflow `s21-adversarial-root-contract`;
- run `35013695077`: **SUCCESS**.

## 4. Historical runtime reds and their convergence

### A/B/C. Causal publication, presentation and motion interval — HISTORICAL RED → PASS

The first adversarial tranche proved that custom root quaternions were initially lost in causal/query geometry, presentation and motion intervals. Those paths reconstructed the legacy gravity+yaw root instead of consuming the accepted `rootTransform`.

Historical red evidence:

- `S21AdversarialRootCausalityTests`;
- corrected causal red run `35017750952`;
- presentation/motion red run `35018354591`.

After explicit-root runtime/casual publication changes, the unchanged causality lane is green:

- production lineage includes `3ac4d12` (`consume explicit root authority in runtime evaluation`) and `6b94ec5` (`publish explicit root causal authority`);
- run `35029174547`: **SUCCESS**.

NFR-009 remains independently protected: root-only changes reuse local joints rather than incrementing `jointEvaluations`.

### D. Canonical root-provider binding — HISTORICAL RED → PASS

The original binding holdout showed that changing only `root_transform` could make an otherwise valid canonical binding non-executable.

Historical red:

- `S21AdversarialRootBindingTests`;
- run `35017973596`: **FAIL**.

After catalog/runtime root ownership was decoupled from the legacy bridge pair, the same lane is green:

- `63101f5`, `fix(s21): decouple root authority from legacy bridge pair`;
- run `35059084625`: **SUCCESS**.

Unknown root-provider ids continue to reject candidate catalog revisions atomically.

### E. Root wire authority — HISTORICAL RED → PASS

The initial wire holdout found that v4 carried neither the exact transported root DTO nor canonical root-provider identity.

Historical red:

- `S21AdversarialRootWireTests`;
- run `35018498220`: **FAIL**.

The implementation evolved the wire explicitly and reconstructs causal endpoints from transported authority:

- `ff28bbe` introduced pose wire v5;
- `162b85f` published root identity and transform;
- `1da9a69` reconstructed causal endpoints from transported root;
- run `35029307410`: **SUCCESS**.

Client acceptance also matches model + pose engine + root-provider identity before consuming transported material authority; it does not resample an external provider from presentation state.

### F. Root authority loss — HISTORICAL RED → PASS

The first availability holdout showed that local geometry disappeared after `Optional.empty()`/provider failure but no `UNAVAILABLE` endpoint was published, allowing remote stale truth to survive.

Historical red:

- `S21AdversarialRootAvailabilityTests`;
- run `35019382819`: **FAIL**.

The repaired publication path now advances causal authority with an unavailable endpoint:

- run `35029174647`: **SUCCESS**.

### G. Gravity independence — PASS

FR-032 was exercised with different physical `GravityFrame` values while an external provider supplied the same transverse quaternion/origin/scale. Physical gravity remained independent from material root orientation.

Evidence:

- `S21AdversarialRootGravityIndependenceTests`;
- corrected run `35019917699`: **SUCCESS**;
- later integrated run `35029765053`: **SUCCESS**.

The earlier compile-only red caused by a `double` fixture literal is discarded and is not product evidence.

### H. Built-in entity-root compatibility — PASS + MUTATION ADEQUATE

The compatibility matrix compares the pre-S21 composition with `scalebrews:entity_root` over all six gravity directions, five representative yaws and three scales, using asymmetric geometry and all eight transformed vertices. Root-only matrix changes must also preserve joint-cache reuse.

Evidence:

- `S21AdversarialEntityRootCompatibilityTests`;
- run `35020120798`: **SUCCESS**;
- later integrated run `35029764969`: **SUCCESS**.

The first mutation-workflow attempt (`35020708281`) is discarded because its textual mutator failed before compiling a mutant. The hardened regex-based lane later produced a clean baseline/compile/kill cycle:

- hardened workflow lineage `05b0ed6`;
- run `35021062884`: **SUCCESS**.

### I. Same-tick external root mutation — HISTORICAL RED → PASS

The original same-tick holdout proved that changing only an external root quaternion could remain causally invisible even after `spatialMutation()`.

Historical red:

- `S21AdversarialRootSameTickTests`;
- run `35020488727`: **FAIL**.

The repaired path advances material identity without reevaluating local joints:

- `8c8b384`, `fix(s21): preserve same-tick entity root compatibility`;
- run `35029765070`: **SUCCESS**.

The same lineage leaves causality, availability, compatibility and gravity-independence lanes green.

## 5. Reload/rebind lifecycle — PASS + MUTATION ADEQUATE

`S21AdversarialRootRebindLifecycleTests` exercises the real runtime reset/rebind boundary with two accepted catalog revisions:

1. revision A owns root provider A and publishes a transverse A root;
2. a genuine A-generation motion interval is certified and accepted;
3. the catalog advances to revision B selecting root provider B;
4. the production runtime `reset()` rebinds the same entity;
5. the new provider, revision, binding generation, local registration generation and root-provider identity must all advance to B;
6. the old A interval must become unexecutable immediately;
7. provider A must never be resampled after the rebind.

Baseline evidence:

- test commit `ee9ba09`, `test(s21): fence root authority across runtime rebind`;
- workflow `s21-adversarial-root-rebind-lifecycle`;
- initial baseline run `35087298324`: **SUCCESS**.

Mutation adequacy deliberately removes `state.entities.clear()` from the ephemeral CI copy of `AnatomyRuntime.reset()`, preserving the obsolete `Active` across reload. The mutant compiles and the holdout fails because revision B does not own a fresh live provider.

- first mutation run `35087616928`: **DISCARDED** because the CI harness attempted to `tee` its diff before creating `build/`; no semantic mutation claim is made from it;
- corrected workflow lineage `943926b`;
- run `35087828779`: baseline **SUCCESS**, mutation-kill **SUCCESS**;
- mutant failure: `S21 lifecycle revision B must own a live ModelGeometryProvider`.

This closes the rebind/generation threat independently from generic S14 ownership tests.

## 6. External-root teleport discontinuity — PASS + MUTATION ADEQUATE

Legacy teleport tests already exercised `RootFrame`, but did not prove discontinuity for a transported external quaternion. `S21AdversarialExternalRootTeleportTests` therefore uses a mutable custom root provider and the real `Entity.teleportTo(...)` lifecycle.

The fixture deliberately teleports only **0.5 blocks**, below the automatic `RootHistory` large-jump threshold, so the result cannot pass accidentally through the legacy >4-block discontinuity heuristic.

The holdout requires:

- pre-teleport material history is seeded;
- teleport publishes the new external quaternion and advances endpoint serial without reevaluating joints;
- no interval crosses pre-teleport → post-teleport authority;
- the tracker then recovers, and the next contiguous external-root-only change produces exactly one interval starting at the post-teleport seed.

Evidence:

- test commit `5a39cb5`, `test(s21): fence external root across teleport discontinuity`;
- workflow `s21-adversarial-external-root-teleport`;
- run `35088530406`: baseline **SUCCESS**, mutation-kill **SUCCESS**.

The mutation removes only `MaterialIntervalRuntime.invalidate(living)` from the ephemeral `teleportTo(DDD)V` hook while preserving `AnatomyMovement.invalidateRoot(living)`. The mutant compiles and fails exactly at:

`A teleport discontinuity must never publish a material interval from the pre-teleport external root`

This proves that the explicit material-interval fence is semantically necessary rather than redundant decoration.

## 7. Entity removal lifecycle — RED / CURRENT BLOCKER

`S21AdversarialRootRemovalLifecycleTests` exercises a canonical runtime binding with an external root provider and first proves that a live certified interval is accepted and executable. It then queues another valid interval and removes the support through real `Entity.discard()`, exercising the production `remove` mixin hook.

Several properties already behave correctly after removal:

- `AnatomyMovement.queryFrame(support)` is empty;
- the queued `MaterialIntervalRuntime` entry is cleared by the remove hook;
- the lifecycle check does not resample external root authority.

However, the canonical runtime still accepts an already-held interval handle immediately after removal, before the next `AnatomyRuntime.prepare()` prunes the dead entry from `State.entities`.

Evidence:

- test commit `cb95998`, `test(s21): reject certified root intervals after removal`;
- workflow `s21-adversarial-root-removal-lifecycle`;
- SHA `99364e4` ordinary build run `35088792862`: **SUCCESS**;
- holdout run `35088792923`: **FAIL** exactly at:

`A handle certified while the support was alive must be rejected immediately after removal`

The static boundary matches the dynamic red: `GeometryIdentity.matches(...)` checks dimension/UUID/entity id, while `AnatomyRuntime.acceptsIntervalIdentity(...)` validates runtime/catalog generations and selected ids but does not currently reject an entity that is already removed. `State.entities` is only pruned in `prepare()`.

Repair property, intentionally non-prescriptive:

> Once a support has entered removal lifecycle, any previously certified material/root interval must immediately cease to be runtime-authoritative. Rejection must not depend on waiting for the next steady-tick `prepare()` sweep, and it must not resample root authority or resurrect queued material work.

The adversarial test must remain red until production satisfies that property. The adversary does not patch the production gate.

## 8. Properties currently green

The current adversarial pass has independently established that:

- DTO/registry normalization, immutability, invalid-state rejection and ownership are green;
- root-only orientation changes reuse already evaluated local joints;
- quaternion sign canonicalization prevents `q` / `-q` false identity churn;
- unknown root-provider ids reject candidate revisions atomically;
- provider exceptions/empty values fail closed and publish unavailable authority rather than stale geometry;
- external root orientation remains independent from physical gravity;
- built-in entity-root composition matches the previous gravity+yaw+scale semantics across the compatibility matrix;
- root-provider identity survives catalog binding, server causal identity, wire transport and client binding matching;
- same-tick root-only mutation advances causal identity without joint recomputation;
- runtime rebind fences obsolete provider/root generations;
- real teleport fences transported external-root continuity and then recovers the tracker.

These green properties do not compensate for the entity-removal red.

## 9. Next adversarial targets

The immediate sequence is now deliberately narrow:

1. keep `s21-adversarial-root-removal-lifecycle` red and unchanged while the implementer repairs production;
2. rerun that same holdout after the fix and add mutation adequacy only after baseline becomes green;
3. independently prove full `AnatomyRuntime.stop()` / session teardown cannot leave accepted root handles or pending intervals alive;
4. reconcile the canonical S21 checklist only after lifecycle/removal and teardown are green;
5. perform a final read-through for PREPARATION/HOT_TICK and root/joint cache separation after the last production repair.

## 10. Gate rule

S21 must not be considered adversarially converged while:

- the removal holdout remains red;
- session teardown lacks independent lifecycle proof;
- any root failure/removal path can publish, execute or reuse stale causal truth;
- a root-only change forces local-joint reevaluation without a demonstrated reason;
- any causal/presentation/motion/wire path reconstructs legacy gravity+yaw authority after a custom root has already been accepted;
- canonical bindings cannot swap root authority independently of geometry and pose;
- same-tick external root-only mutations can remain causally invisible;
- reload/rebind or teleport can preserve a stale external-root interval across a lifecycle boundary.
