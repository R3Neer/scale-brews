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

## First holdout tranche — SPI contract

The first independent holdout covers the SPI surface: DTO normalization/immutability/invalid-state rejection and registry ownership.

Evidence:

- `S21AdversarialRootTransformContractTests`;
- workflow `s21-adversarial-root-contract`;
- run `35013695077`: **SUCCESS**.

This tranche proves the DTO/registry contract only. It does not imply runtime convergence.

## Second holdout tranche — runtime causal integration

The first live root-authority integration was attacked through independent focused workflows rather than one aggregate red test. Current findings are intentionally separated so a partial production repair cannot hide another authority leak.

### A. Causal publication loses the custom root quaternion — RED

`S21AdversarialRootCausalityTests.transverseRootMustSurviveCausalPublication` samples a provider returning a transverse quaternion, verifies that `ModelGeometryProvider` captured it once, then asks `AnatomyMovement` for the causal query frame.

The published/query geometry differs from the reference geometry produced with the accepted root DTO. The production route reconstructs the world root through the legacy gravity+yaw path instead of consuming the sampled provider root.

Evidence:

- workflow `s21-adversarial-root-causality`;
- corrected-fixture run `35017750952`: **FAIL** on causal publication;
- root-only joint-reuse property in the same holdout passes.

An earlier red from this holdout was discarded because the adversarial fixture incorrectly supplied piece size where `ModelGeometry.Piece` expects min/max bounds. It is not counted as product evidence.

### B. Presentation discards transported root authority — RED

`evaluatePresentation(...)` is required to consume accepted causal authority. The holdout constructs an endpoint carrying a transverse `rootTransform` and compares presentation geometry to an explicit-root reference sample.

Evidence:

- workflow `s21-adversarial-root-causality`;
- run `35018354591`: **FAIL** on `presentationMustConsumeTransportedRootTransform`.

### C. Motion interval discards both transported endpoint roots — RED

The motion holdout builds two accepted endpoints with unchanged local joints and different root quaternions, then certifies a `MotionIntervalHandle`. The expected end geometry is the explicit after-root sample. The current interval route rebuilds legacy roots from the joint samples instead of consuming `before.rootTransform()` / `after.rootTransform()`.

Evidence:

- workflow `s21-adversarial-root-causality`;
- run `35018354591`: **FAIL** on `motionIntervalMustConsumeBothTransportedRootTransforms`.

The same run leaves the independent NFR-009 root-only joint-cache test green.

### D. Changing only `root_transform` can make a valid binding non-executable — RED

FR-031/FR-033 require geometry, pose and root authority to remain independently selectable. The binding holdout keeps geometry and pose unchanged and substitutes only a registered custom root provider.

The current catalog route rejects that candidate as an incomplete legacy compatibility binding rather than preparing an executable canonical binding.

Evidence:

- `S21AdversarialRootBindingTests`;
- workflow `s21-adversarial-root-binding`;
- run `35017973596`: **FAIL** on custom-root executable binding;
- the independent unknown-root atomic rejection property passes.

### E. Root authority is missing from the pose wire contract — RED / I4

The wire holdout protects both authority value and authority identity:

1. a custom quaternion must survive `PublishedFrame -> AnatomyPosePayload -> AnatomyFrameHistory -> CausalEndpoint`;
2. the payload must carry the canonical root-provider id.

The current v4 pose packet reconstructs root from legacy origin/yaw/scale/gravity and has no root-provider accessor/field.

Evidence:

- `S21AdversarialRootWireTests`;
- workflow `s21-adversarial-root-wire`;
- run `35018498220`: **FAIL**, with both independent properties red.

This is an expected implementation gate while I4 is open, but it must not be marked complete until the holdout is green.

### F. Root loss removes local geometry but does not publish remote invalidation — RED

FR-029 requires an unavailable indispensable authority to mark the endpoint `UNAVAILABLE`, so a previously valid collider cannot remain frozen. `PublishedFrame` is explicitly capable of transporting unavailable endpoints, while `QueryFrame` is only legal for AVAILABLE geometry.

The availability holdout first establishes a valid endpoint and then makes the root provider either return `Optional.empty()` or throw. Both cases correctly remove local authoritative/query geometry, but `publishedFrame()` becomes empty rather than advancing causal identity with an `UNAVAILABLE` endpoint. A remote consumer therefore receives no explicit clearing event from this path.

Evidence:

- `S21AdversarialRootAvailabilityTests`;
- workflow `s21-adversarial-root-availability`;
- run `35019382819`: **FAIL** in both empty-provider and throwing-provider cases at the missing `UNAVAILABLE` publication assertion.

The repair property is external, not prescriptive about DTO internals: after authority loss there must be no query collider and there must be a new causal publication that marks the endpoint unavailable without treating stale root data as valid authority.

### G. External root orientation is independent from physical GravityFrame — GREEN

FR-032 was exercised with two supports carrying different physical gravity frames (`DOWN` and `EAST`) while the external provider supplied the same transverse quaternion, world origin and scale. The authoritative samples preserved the different physical `GravityFrame` values but accepted the same root quaternion and produced identical explicit-root geometry.

Evidence:

- `S21AdversarialRootGravityIndependenceTests`;
- workflow `s21-adversarial-root-gravity-independence`;
- corrected run `35019917699`: **SUCCESS**.

An earlier red was discarded because the adversarial test passed a Java `double` literal to a `float` constructor parameter and therefore never compiled.

### H. Built-in `scalebrews:entity_root` is exactly compatible with pre-S21 root composition — GREEN

I8 is protected by a matrix holdout comparing the old internal root composition with the new built-in provider over:

- all six cardinal gravity directions;
- five representative yaws (`-179`, `-42.5`, `0`, `137.25`, `180` degrees);
- three scales (`0.5`, `1`, `2`).

The fixture uses asymmetric convex geometry, a non-trivial local rest transform and a non-trivial model transform. Every combination compares all eight transformed vertices, not just an AABB center. The same 90-case sweep also requires `jointEvaluations` to remain constant after the first evaluation because only root inputs change.

Evidence:

- `S21AdversarialEntityRootCompatibilityTests`;
- workflow `s21-adversarial-entity-root-compatibility`;
- run `35020120798`: **SUCCESS**;
- ordinary build on the same test lineage, run `35020120724`: **SUCCESS**.

Mutation adequacy is tracked separately. The first mutation-workflow attempt (`35020708281`) is **not** semantic evidence: all three baselines were green, but the textual mutator failed to find its targets before any mutant was compiled. The workflow was subsequently hardened to regex-based single-target transformations and must produce a clean baseline/compile/kill cycle before mutation adequacy is claimed.

### I. Same-tick external root-only mutation does not advance causal identity — RED

The common motion contract explicitly allows same-tick material advances, and the production mutation seams (`Entity.move`, `setPos`, `refreshDimensions`) route through root capture/commit or `spatialMutation()`. FR-030 therefore requires a root-only external change to receive new causal identity without forcing another local-joint evaluation.

The holdout establishes a valid endpoint, changes only the custom provider quaternion within the same game tick, calls `AnatomyMovement.spatialMutation(support)` and then checks serial, authority tick, joint sample tick, collider, root DTO and joint-evaluation count.

Evidence:

- `S21AdversarialRootSameTickTests`;
- workflow `s21-adversarial-root-same-tick`;
- run `35020488727`: **FAIL** at `FR-030: root-only same-tick mutation must advance causal/material frame identity` after normal compile/server startup.

The static path agrees with the dynamic failure: `spatialMutation()` currently observes only the legacy `RootFrame` (position/yaw/scale/gravity), while endpoint deduplication does not include the accepted external `rootTransform`. A provider-only quaternion change can therefore remain the same causal endpoint even though material geometry should change.

## Properties currently green

The current adversarial pass has established these positive properties independently from the red paths:

- DTO/registry normalization, immutability, invalid-state rejection and ownership are green;
- root-only orientation changes can reuse already evaluated local joints in `ModelGeometryProvider`;
- quaternion sign canonicalization prevents `q` / `-q` from creating false identity/cache churn;
- unknown root-provider ids reject candidate catalog revisions atomically rather than replacing the accepted snapshot;
- a root provider throwing during direct sampling is caught by `ModelGeometryProvider` and does not itself resurrect direct geometry;
- external root orientation remains independent from physical `GravityFrame` at the provider/materialization layer;
- built-in `scalebrews:entity_root` matches the old gravity+yaw+scale root across the 90-case compatibility matrix without joint reevaluation;
- the root-provider registry is synchronized, append-only per id and rejects duplicate ownership, so an accepted id cannot be silently swapped to another provider implementation later in the same process.

These green properties do not compensate for the red causal/wire/catalog paths above.

## Next adversarial targets

After production changes land, the existing red holdouts must be rerun before adding more surface. The next independent targets are:

1. clean mutation-kill evidence for the I8 compatibility matrix;
2. reload/rebind changes root-provider identity/generation without retaining an obsolete sampled root;
3. teleport/discontinuity never interpolates across stale root authority;
4. client accepted snapshot/binding matching includes root-provider identity and consumes only transported root DTOs;
5. entity removal/session teardown leaves no live causal root endpoint.

## Gate rule

S21 must not be considered adversarially converged while:

- S20 remains materially open or its required real-model oracle cannot execute;
- the ordinary build is red for S20/S21 harness defects;
- runtime root integration is absent;
- any root failure path can publish or reuse stale causal truth;
- a root-only change forces local-joint reevaluation without a demonstrated reason;
- any causal/presentation/motion/wire path reconstructs a legacy gravity+yaw root after a custom provider root has already been accepted;
- canonical bindings cannot swap root authority independently of geometry and pose;
- same-tick external root-only mutations can remain causally invisible.
