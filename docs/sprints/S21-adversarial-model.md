# S21 adversarial model — root transform authority

> Role: adversarial verification only. This document does not authorize production changes.
>
> Sprint under test: `S21-root-transform-provider.md`.

## Threat model

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
   - entity removal and world/session teardown cannot keep a causal root endpoint alive.

## First holdout tranche

The first independent holdout covers the already-implemented SPI surface only: DTO normalization/immutability/invalid-state rejection and registry ownership. It intentionally does not pretend that runtime integration exists yet.

A second holdout tranche is reserved for the first commit that wires root authority into `ModelGeometryProvider`/`AnatomyRuntime`. That tranche will attack sampling count, stale fallback, root-only cache invalidation, gravity independence and replay/lifecycle identity.

## Gate rule

S21 must not be considered adversarially converged while:

- S20 remains materially open or its required real-model oracle cannot execute;
- the ordinary build is red for S20/S21 harness defects;
- runtime root integration is absent;
- any root failure path can publish or reuse stale causal truth;
- a root-only change forces local-joint reevaluation without a demonstrated reason.
