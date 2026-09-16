# S21 adversarial model — root transform authority

Status: **CLOSED — INDEPENDENT ADVERSARIAL ACCEPTANCE COMPLETE**.

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

## 2. Final reconciled status

The early runtime tranche intentionally landed as independent red holdouts before the implementation converged. Those reds are retained below as historical evidence. After the final removal repair and mutation proof, no S21 blocker remains.

Final classification on the `chatgpt-editing` lineage:

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
- session teardown: **PASS**, including mutation adequacy;
- entity-removal lifecycle: **PASS after production repair + independent mutation kill**;
- final PREPARATION/HOT_TICK and root/joint separation reread: **PASS / zero production changes**.

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

After explicit-root runtime/causal publication changes, the unchanged causality lane is green:

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

## 5. Reload/rebind lifecycle — PASS + MUTATION ADEQUATE

`S21AdversarialRootRebindLifecycleTests` exercises the real runtime reset/rebind boundary with two accepted catalog revisions. It proves fresh provider/revision/binding/local generations, immediate rejection of old intervals and no resample of obsolete provider A after the rebind.

Evidence:

- test commit `ee9ba09`, `test(s21): fence root authority across runtime rebind`;
- initial baseline run `35087298324`: **SUCCESS**;
- first mutation run `35087616928`: **DISCARDED** because the CI harness failed before semantic execution;
- corrected workflow lineage `943926b`;
- run `35087828779`: baseline **SUCCESS**, mutation-kill **SUCCESS**.

The semantic mutant preserves obsolete `state.entities` across reset and is killed because revision B no longer owns a fresh live provider.

## 6. External-root teleport discontinuity — PASS + MUTATION ADEQUATE

`S21AdversarialExternalRootTeleportTests` uses a mutable custom root provider and a real `Entity.teleportTo(...)`. The fixture teleports only **0.5 blocks**, below the legacy large-jump heuristic, so the result cannot pass accidentally through that fallback.

Evidence:

- test commit `5a39cb5`, `test(s21): fence external root across teleport discontinuity`;
- workflow `s21-adversarial-external-root-teleport`;
- run `35088530406`: baseline **SUCCESS**, mutation-kill **SUCCESS**.

The mutation removes only `MaterialIntervalRuntime.invalidate(living)` from the teleport hook while preserving local root invalidation. It is killed when a pre-teleport material interval crosses the discontinuity.

## 7. Entity removal lifecycle — HISTORICAL RED → PASS + MUTATION ADEQUATE

`S21AdversarialRootRemovalLifecycleTests` first established a canonical runtime binding with an external root provider and proved a live certified interval was executable. It then queued another valid interval and removed the support through real `Entity.discard()`.

Historical red evidence:

- test commit `cb95998`, `test(s21): reject certified root intervals after removal`;
- ordinary build on SHA `99364e4`, run `35088792862`: **SUCCESS**;
- holdout run `35088792923`: **FAIL** exactly because a handle certified while the support was alive remained accepted immediately after removal.

The invariant was intentionally implementation-independent:

> Once a support has entered removal lifecycle, any previously certified material/root interval must immediately cease to be runtime-authoritative. Rejection must not depend on waiting for the next steady-tick `prepare()` sweep, and it must not resample root authority or resurrect queued material work.

Production repair **`c481194bb6156906768c12c499fc6ad8650a6595`** adds the minimal `entity.isRemoved()` rejection to `AnatomyRuntime.acceptsIntervalIdentity(...)`. Implementer evidence after the repair:

- ordinary build `35102351879`, job `104814662103`: **424/424 required GameTests**, `BUILD SUCCESSFUL`;
- removal holdout `35102351750`, job `104814662473`: **2/2**, `BUILD SUCCESSFUL`;
- rebind lane `35102351833`, job `104814661968`: **SUCCESS**;
- session teardown lane `35102351804`, job `104814662087`: **SUCCESS**.

The independent adversary then added mutation adequacy in **`0c99e2f1a4d158dd47b3d5b9f092dce1fa644986`**. The ephemeral mutant removes only the new `entity.isRemoved()` term, must compile, and reruns the unchanged removal holdout.

Run **`35105998877`**:

- job **`104827240871`** baseline: **SUCCESS**;
- job **`104827781422`** mutation kill: **SUCCESS**; the mutant compiles and the holdout kills it.

Ordinary workflow run **`35105998939`**, job **`104827241696`**, on the same head is **SUCCESS**.

The removal blocker is therefore closed with causal evidence rather than by trusting the repair shape.

## 8. Session teardown — PASS + MUTATION ADEQUATE

`S21AdversarialRootSessionTeardownTests` exercises the real `AnatomyRuntime.stop(server)` boundary after establishing live causal root, certified interval and queued material work.

The final hardened lane proves three independent teardown responsibilities with ephemeral semantic mutants:

1. **retain-state** keeps the server `State` instead of removing it;
2. **retain-pending** skips only `MaterialIntervalRuntime.clear(server)`;
3. **keep-movement-active** skips the per-level `AnatomyMovement.deactivate(level)` call.

Evidence:

- test commit `2d87418`, `test(s21): retire root authority on runtime stop`;
- initial baseline run `35089135904`: **SUCCESS**;
- final hardened workflow lineage `61575d1`;
- run `35097601316`: baseline **SUCCESS** and all three mutation-kill jobs **SUCCESS**.

Intermediate harness failures that never reached a semantic mutant remain discarded as non-product evidence.

## 9. Final PREPARATION/HOT_TICK and root/joint reread

The required second read was repeated after the final product repair.

Observed boundary:

- `WorldAnatomyCatalog.replaceValidated(...)` validates root-provider ids while accepting a revision;
- `prepareCanonical(...)` and `prepareBridge(...)` resolve the selected `RootTransformProvider` and place it in immutable executable `Binding` data before runtime;
- `AnatomyRuntime.bindIfEligible(...)` constructs `ModelGeometryProvider` from that already-resolved provider; no root-provider registry lookup, JSON parsing, resource walk or reflection is introduced in HOT_TICK;
- `RootTransformProvider.RootTransform` is common/server-safe, validates finite origin/scale/quaternion, normalizes the quaternion and canonicalizes `q/-q` sign equivalence at the DTO boundary;
- `ModelGeometryProvider.refreshRoot(...)` refreshes root authority using the already accepted local pose sample and never invokes local joint evaluation;
- `ModelGeometryProvider.joints(...)` keys the joint endpoint cache solely by `PoseEngine.Inputs`; root is deliberately outside that key;
- `sampleAt(...)` composes accepted root authority only after retrieving cached/evaluated local joints;
- the removal repair is an identity fence before interval resolution and adds no new preparation/hot-tick ownership or work.

No further S21 product or test change was required by this reread.

## 10. Final properties

The adversarial campaign has independently established that:

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
- real teleport fences transported external-root continuity and then recovers the tracker;
- entity removal immediately invalidates already-held certified intervals without resampling root authority;
- full runtime stop retires session identity, pending material intervals and local movement authority, and old handles stay dead after restart.

## 11. Gate result

Every S21 blocker in the original gate rule is now green, including the final removal lifecycle property and mutation adequacy. The final PREPARATION/HOT_TICK reread found no new product defect and required zero production changes.

**S21 is adversarially converged and CLOSED. G3 task 6 may be marked complete.**
