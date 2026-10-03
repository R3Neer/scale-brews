# S30 / G4.6a — causal original-model presentation

Active role: **IMPLEMENTER**. ADVERSARY owns its independent pre-implementation
model, holdouts, mutation checks and spatial/temporal snapshots. This document
records execution; requirements and architecture remain canonical elsewhere.

## Scope and initial state

The initial S30 proposal covered all FR-086..088 and is recorded below with
ADVERSARY's planning RED. That scope has been split. This sprint now covers
the first FR-086 dependency: the visible original support model consumes the
same accepted causal endpoint/interval as the material geometry, without
turning renderer animation into authority. A later S31 covers the body
residual/camera, FR-087..088 and the two revealed planning counterexamples.
S30 adds no client collision/carry, protocol field or G4.7 latency work.

S29/G4.5 is closed at `54f023eb`. `AnatomyClientNetworking.presentationContact`
already checks accepted contact, live support, binding and current endpoint, and
returns the exact evaluated geometry. `presentationInterval` is separately
available only for a server-certified material serial. The active renderer hook
stores one `PlatformVisuals.offset` in render state and applies it once in
`getRenderOffset`. For anatomy, that legacy offset is currently zero. The camera
hook delegates to `PlatformCamera`, which combines the legacy offset with Tiny
Mounts, smooths, bounds and block/near-plane clips it. Current `origin/main`
`b5e62535` adds `PlatformCamera.appliedOffset()` for first-person Tiny Mount
rendering. G4.6 must retain that contract when the lines are integrated.

Thesis: after S30, a compatible anatomical support's original visible model
is posed from the accepted `PresentationFrame` rather than a second local
animation sample, with model/identity mismatch failing closed and the legacy
`DISABLED` render route untouched. S30 alone does not close G4.6.

## Implementation plan — IMPLEMENTER

- [x] I1 Attach one accepted `PresentationFrame` and its catalog model identity
  to the support's render state during extraction. Re-check live support,
  level, epoch, revision, binding and model both before submission and at the
  delayed draw; no cached frame grants authority after a lifecycle barrier.
  `CURRENT_ENDPOINT` is the default;
  `CERTIFIED_INTERVAL` is used only when an exact server material serial is
  available to the consumer, never inferred from adjacency or partial tick.
- [x] I2 Add a client `collision.presentation` adapter for the original
  `ModelPart` family. During preparation, export the **actual visible model
  instance** with the same geometry-family extractor used to prepare the
  catalog model. Compare part paths, parent relations, neutral transforms,
  source poses, model transform and the IDs, owning part and local cuboid
  bounds of every catalog-retained physical piece; identical names/poses alone
  are insufficient. Reject missing, extra or changed retained pieces rather
  than drawing a causal pose over mismatched material. Cache only a successful
  renderer-model-object/catalog-epoch/revision/resource-generation association.
  Invalidate on model/variant replacement and client resource reload; failed
  comparison stays fail-closed for that generation. Inspect the live model in
  its baked neutral pose, restoring **all** mutated fields afterward, so a
  previous entity's `setupAnim` cannot influence the comparison. No model
  extraction or reflection runs per render frame.
- [x] I3 Apply `EvaluatedFrame` root/model transform to the pose submitted by
  `LivingEntityRenderer`, accounting for vanilla renderer root transform and
  camera-relative coordinates. Apply local-part transforms only at the actual
  model draw boundary after vanilla `setupAnim`, with an equivalent idempotent
  pose for immediate render layers. Recompose and verify every representable
  local matrix before mutating `ModelPart`; unsupported shear/technology fails
  closed. Save/restore the full shared-model fields (translation, rotation,
  nonunit scale, visibility and skipDraw) around each use, including
  exceptional exits. Check retained-part visibility at draw without a new
  extraction, then fail closed if a material piece is hidden. Layer/seat order
  and mainline Tiny Mount behavior stay intact.
- [x] I4 Verify ROOT-only and JOINT-only original-model transforms at fixed
  fractions with a deterministic geometric oracle and actual renderer
  snapshots. Assert no physical position/AABB/movement/gravity change, no
  duplicate pose application and invalidation on dimension/catalog/binding/
  teleport. Run server build and full client GameTests; hand off the unchanged
  candidate to ADVERSARY for independent mutation/performance review.

## Plan convergence review — IMPLEMENTER

The first broad plan failed the independent pre-implementation review: its
body/material calibration could erase voluntary walking after a late contact,
and resetting only a new residual owner would leave the camera's own smoothed
state alive after a short teleport. These are valid plan defects, not
production regressions; the exact counterexamples remain in the ADVERSARY
section below. They become mandatory S31 inputs. Splitting at the causal
support-render dependency avoids writing a residual before its physical
provenance and hard-reset propagation are specified.

The revised S30 plan still needs its own independent adversarial model and a
complete zero-change plan review before production edits. The original
ADVERSARY section below remains an immutable record of the rejected broad
proposal; it is not evidence of S30 renderer acceptance.

Minecraft 26.2 source check tightened I3: `LivingEntityRenderer.submit` queues
`ModelFeatureRenderer.Submit` with a model/state reference and a copied
`PoseStack.Pose`; `ModelFeatureRenderer.render` calls `model.setupAnim` and
draws later. A ModelPart mutation performed and restored solely during submit
cannot reach the base model draw. Immediate layers also call `setupAnim` in
submit. The adapter therefore needs explicit consumption at both phases, with
absolute/idempotent part poses and bounded restoration. This is a plan correction
before code, not a production defect.
`ModelPart.storePose()` in the pinned source omits scale, visibility and
skipDraw, although `loadPose()` writes scale; restoration must capture those
fields separately, as `PlatformVisuals.sample` already does for visibility.

ADVERSARY's second plan RED showed that two original Cow `ModelPart` trees can
share every path, parent, neutral matrix and source pose while one retained
body cube differs by a full block. I2 now compares the actual live renderer's
retained cuboid geometry against the accepted catalog model, using the one
existing family extractor during preparation and explicit resource/model
generation invalidation. This rejects the revealed counterexample without
placing extraction in the draw loop. The updated plan returns to independent
review before implementation.

## Pre-implementation adversarial model — ADVERSARY

Active role: ADVERSARY. Independent architecture/source review on54f023eb; no
S30 production candidate exists, so these are planning counterexamples rather
than claims of executed runtime failures in an unimplemented feature.

The source of a drawable residual must be a live accepted contact and its exact
causal geometry, with an explicit frame of reference. Residual ownership differs
from physical transport ownership: local player, controlled vehicle and remote
observer may consume the same presentation while only the authorized actor can
predict/carry. Position, previous/interpolated position data, AABB, velocity,
gravity, contact/reference/receipt state must be identical before and after every
presentation consumer. Renderer animation is never a material input.

Lifecycle equivalence classes: same contact continuity versus fresh packet
sequence/heartbeat; changed material local point versus replaced contact/support;
body/support object or network ID replacement; epoch/revision/binding/tracking;
READY/BINDING/DISABLED; level/socket; body or support teleport, gravity and missing
geometry. An ordinary authoritative contact clear may fade. Missing authority,
material mismatch or discontinuity must not masquerade as ordinary clear and
must invalidate every residual consumer, including retained camera smoothing.
Identity of an accepted packet is not automatically the identity of a calibration
session. The plan must define the latter independently of render query order.

Temporal equivalence classes: root-only and joint-only material changes, stable
support with voluntary body motion, combined motion, already-applied physical
transport, body/contact/support packet order, fixed and changing partial fraction,
late heartbeat/local-point updates, contact transition while fading, repeated
render/camera reads and camera-first versus renderer-first order. Exact fractions
and geometry must be selected from server-issued endpoints/certificates; partial
fraction cannot authorize an inferred interval. Tests will distinguish a zero
residual caused by correctly applied physical movement from a vacuous missing
contact/unsupported body.

Spatial equivalence classes: scale and all gravity directions, rotated/sheared
retained geometry, convex surface point/normal and body relation in one world
frame; support chains; blocked center and near-plane corners, detached/panoramic
camera; Tiny Mounts/vanilla passengers excluded as independent anatomy contacts.
Mainline appliedOffset() must expose the final bounded/clipped camera result for
its first-person consumer, with no second anatomical correction layered onto it.

### Executed planning counterexamples / convergence blockers

Reusable mathematical probe: `python tools/s30_adversarial_plan_probe.py`.
Durable output: `../s30-adversarial-plan-counterexamples.json`. The probe returns
RED for two unresolved contracts; it is not a simulated S30 production oracle.

1. **PLAN FR-088, I3/I4:** the current PlatformCamera coordinator retains its
   own smoothed residual and resets only for changed camera entity/world or a
   physical jump whose squared distance exceeds16. With unchanged entity/world,
   a one-block teleport, old camera residual0.2, owner reset to0 and dt0.05, its
   existing exponential smoothing still returns0.12130613194 in unobstructed
   space. Therefore immediate zero in the new owner alone cannot satisfy the
   camera discontinuity contract. Plan convergence must specify how the same
   hard barrier retires the actual retained/applied camera residual (including
   gravity/rebind/short teleport), rather than testing only the owner's target.
2. **PLAN FR-086, I2:** a direct calibrated material-point minus interpolated-body
   comparison with an unchanged support anchor cancels voluntary body walking
   while its server local-point update is delayed. Baseline anchor/body0;
   body voluntarily moves0.2; anchor stays0: naive residual=-0.2 and rendered
   position returns0. Correct support-induced residual is0 and walking remains
   visible at0.2. The plan says to avoid this outcome but has not specified the
   observed causal relation separating voluntary motion, already-applied transport
   and packet-age/interpolation differences. Calibration at every new sequence
   would also conflate heartbeat/order progression with a new relation. I2 must
   state acquisition/update/heartbeat behavior and available validated inputs;
   it cannot assume that body position and contact packets describe the same time.

The exact production API/algorithm remains IMPLEMENTER-owned. These blockers ask
for a converged observable contract; ADVERSARY does not prescribe a repair or
supply unrevealed holdout traces. No production modification is authorized by a
plan RED. More specific held cases will remain private until ready evidence or a
classified reproducible defect exists.

### Performance and acceptance model

HOT_PRESENTATION/HOT_NETWORK-adjacent invalidation are the affected routes;
catalog/model binding stays PREPARATION, and renderer extraction cannot leak into
anatomical queries. Scaling variables are live bodies/supports, model pieces,
query consumers/fractions, contact updates/heartbeats and lifecycle resets. One
bounded relation/owner and shared causal geometry should avoid per-query model
sampling, repeated interpolation preparation, cross-world scans and retained weak
entity keys. Measure work/allocation on nonzero residuals and changing fractions,
not only a cached zero. Camera clipping has its existing bounded center/corner
rays; reset/fade state and caches must not grow with packet or render frequency.

Post-implementation evidence must include pure transition/geometry assertions,
real client contact with nonzero residual, physical-state hashes, actual render
state/getRenderOffset and camera coordinator consumers, dedicated classloading,
fixed-view/time geometry/contact plus actual consumer snapshots, and directed
faults removing authority checks, duplicating application, cancelling walking,
keeping stale camera state or making evaluation query-order dependent. Mutation
compile errors/timeouts are not kills. The final review must produce no further
productive changes. G4.7 full latency/allow-flight workloads and G9 NFR-014 remain
separate canonical gates; they do not substitute this sprint's applicable costs.

Pre-implementation status: PLAN RED / not converged until the two observable
contracts above are resolved and independently reviewed. No S30 runtime, visual
acceptance or production correctness claim follows from this review.

## Revised S30 independent model — ADVERSARY

Active role: ADVERSARY. This model concerns only the revised G4.6a original
support renderer scope. The previous residual/camera counterexamples above are
historical S31 inputs, not blockers attributed to this narrower S30. No production
change or renderer acceptance is claimed by this pre-implementation review.

Authority is an accepted causal frame plus a compatible visible model. Matching
source names, part paths and neutral local transforms is not automatically
matching material geometry. Compatibility classes include retained cube geometry,
piece ownership/filtering, same-tree resource variants, model replacement within
one revision, and unsupported local matrices. Preparation may inspect original
model structure; repeated draw must consume a bounded prepared association without
reflection, extraction or a full catalog scan. A cached association cannot outlive
its renderer/model/resource or accepted catalog identity.

Consumption has two observable phases in pinned Minecraft 26.2: immediate layers
and queued base-model drawing. Both must consume the same absolute causal pose
after their corresponding vanilla animation setup. Public test families include
root-only and joint-only motion, nonunit scale, shared model interleaving,
exceptional exits, duplicate application, lifecycle barriers between extraction,
submission and delayed draw, and legacy/Tiny Mount ordering. Independent geometry
oracles and fixed-view renderer snapshots must distinguish an actual posed original
model from an unchanged local animation or vacuous failed compatibility lookup.
Physical state must remain identical across these presentation consumers.

The updated I1/I3 explicitly address delayed draw invalidation, vanilla setupAnim
overwriting submit-time part edits, idempotence and full shared-field restoration.
Local pinned bytecode inspection confirms these are necessary contracts:
SubmitNodeCollection retains a model/state reference plus a copied PoseStack pose,
and ModelFeatureRenderer prepares the model with setupAnim at later drawing.

Performance scope is HOT_PRESENTATION for extraction/submission/layers/draw and
PREPARATION for compatibility. Scaling axes are visible supports, part/piece count,
layers/consumers, changing fractions and renderer/catalog churn. Acceptance will
measure structural work on changing nonzero root/joint poses, bounded associations,
and restoration cost rather than accepting only a cached empty/zero path. G9's
full workload remains separate; unexplained repeated preparation is still an S30
blocker. Unexecuted concrete traces remain private.

### Revised plan review evidence

**PLAN RED — I2 / FR-086:** the proposed part-path/neutral-transform preparation
predicate does not discriminate different retained cube geometry. The executed
`S30AdversarialModelCompatibilityPlanProof` bakes two original Minecraft 26.2 Cow
trees, changes only one body cube's width by one block, then extracts both through
the existing ModelPart family. Source/version, every part ID/parent, neutral matrix
and SourcePose remain equal; retained cube bounds differ by exactly one block.
Such a resource/model variant passes the stated checks while the visible body
surface differs from the catalog material surface even at an identical causal
pose. I2 needs an explicit retained-geometry compatibility/fail-closed contract,
including invalidation when the visible model/resource changes. Source labels
and model tree identity alone cannot certify that contract.

This is a planning counterexample, not a production regression or a proposed
implementation algorithm. The fixture asserts the real-model premises and exits
successfully when the insufficiency is demonstrated. Run the existing adversarial
init selector with
`runClientGameTest -Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S30AdversarialModelCompatibilityPlanProof`
and the ADVERSARY build/run/cache directories. Executed lane:
`../s30-adversarial-model-compatibility-plan.log`, BUILD SUCCESSFUL in 1m 2s,
clean client shutdown. Durable geometry evidence:
`../s29-adversary-run/s30-model-compatibility-plan-counterexample.json` reports
identityMatches=true, partsAndNeutralPosesMatch=true, cubeGeometryMatches=false,
widthDeltaBlocks=1.0. No production file was modified. This revised plan remains
unconverged until that observable compatibility contract is independently
reviewed; the delayed-draw correction itself passes this plan review.

### Revised I2 convergence review — ADVERSARY

**PLAN GREEN / zero-change review (2026-10-03):** reviewed the complete updated
I1..I4 against the independent G4.6a model after the retained-geometry correction.
I2 now compares the actual renderer model instance's retained piece IDs, ownership
and bounds as well as hierarchy, neutral matrices, SourcePose and model transform;
the executed Cow counterexample has unequal retained bounds and therefore cannot
satisfy this corrected compatibility contract. Model/resource replacement and
catalog epoch/revision retirement are explicit, with unsuccessful preparation
remaining fail-closed rather than repeatedly entering the hot draw path.

The complete pass also covers endpoint/certificate selection, authority at delayed
draw, root coordinate conversion, post-setupAnim consumption for immediate and
queued rendering, representability, idempotence, full shared-field restoration,
legacy/Tiny Mount preservation, physical-state invariance, geometric/visual
oracles and applicable performance checks. It requires no further plan change.
The earlier PLAN RED remains the immutable evidence for the rejected predicate;
the mathematical residual/camera REDs remain S31 inputs. This is convergence of
the pre-implementation plan only: no S30 renderer runtime or visual acceptance,
production implementation, or G4.6 closure is claimed. Concrete unexecuted
holdouts remain private until the unchanged candidate is ready for testing.

### First production holdout — ADVERSARY

**PRODUCTION RED — FR-086 / I3 representable ModelPart pose:**
`S30AdversarialModelPoseProof` on the actual pinned Cow model rejects a valid
head TRS at deterministic iteration 344 (seed `0x530CA05A`). The frame's local
head matrix exactly equals the generated translation/rotation/nonuniform-positive-
scale matrix. Directly assigning its original translation, Euler rotation and
scale fields to that actual ModelPart and calling `translateAndRotate` reproduces
the matrix within the same 2e-4 tolerance. Nevertheless Prepared.apply returns
empty; diagnostic invocation isolates rejection to `root/head`, not visibility,
missing parts, incompatible geometry or an unrepresentable shear. Thus the
production draw can discard a drawable accepted causal model near this Euler
configuration. ADVERSARY has not repaired production.

Three independent client launches reproduced the rejection: initial log
`../s30-adversarial-model-pose.log` (1m27s), per-part diagnostic log
`../s30-adversarial-model-pose-diagnostic.log` (46s), and stronger original
ModelPart representability oracle
`../s30-adversarial-model-pose-original-oracle.log` (42s). All fail the explicit
assertion at iteration 344; compilation succeeds. Iterations 0..343 already verify
draw matrices and nested/exceptional full-field restoration, but the remaining
campaign cases, lifecycle renderer snapshots, mutation campaign, applicable cost
checks and global S30 acceptance are still pending. No green closure is claimed.

Reproduction uses `runClientGameTest` with
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S30AdversarialModelPoseProof`,
`-I tools/s29_adversarial.init.gradle` and separate ADVERSARY build/run/cache
directories. The reusable fixture remains unchanged for IMPLEMENTER repair
revalidation.

### First repair revalidation — ADVERSARY

The unchanged `S30AdversarialModelPoseProof` passes after IMPLEMENTER's matrix
decomposition repair: `../s30-adversarial-model-pose-repaired.log`, BUILD SUCCESSFUL
in 1m9s, clean client shutdown. All 512 deterministic positive-scale TRS cases now
match the original ModelPart oracle, including iteration 344. Nested scopes,
exceptional exits and all saved fields restore exactly; an unrepresentable shear
is rejected without shared-state mutation. This supersedes that production RED
while preserving its original evidence. Global S30 acceptance remains pending.

The independent actual-draw harness initial attempts were classified as fixture
errors, not production failures: one queried before available tracking material;
the second used the preparation inspection version `visible` as a server catalog
model, whereas the quadruped evaluator requires the pinned `26.2` version. The
fixture now exports the actual Cow with that version and waits for registration
before the live draw/lifecycle test. No production repair arose from these attempts.

### Actual draw, barriers, mutations and costs — ADVERSARY

`S30AdversarialRendererDrawProof` exercises the actual LivingEntityRenderer
submission and ModelFeatureRenderer.prepareGroup/prepareModel path with its
installed mixins; a test-only StagedVertexBuffer captures the original emitted
vertices. JOINT authority is a real server-published 37-degree head pose; the local
render state deliberately requests a different head/limb animation. The actual
deferred draw emits 240 vertices containing all 80 retained causal material
corners. A separate nonzero accepted ROOT delta is checked against the exact
copied submit matrix while physical body position remains unchanged. A-B-A
dimension transition, fresh tracking/rebind, accepted catalog revision and binding
replacement, short support teleport and UNAVAILABLE all retire the old queued
draw; fresh compatible authority draws positively again.

The final native draw/lifecycle plus 512-matrix lane passes:
`../s30-adversarial-renderer-lifecycle-final.log`, BUILD SUCCESSFUL in 55s,
clean world/client shutdown. Profile snapshots fix observer at (2,3,-2), support
at (2,3,2) with body yaw 90 degrees, and server head pitch 0 then 37; the ROOT
snapshot adds accepted (+0.31,+0.17,-0.23) without physically moving that support.
The original PNGs were inspected: head rotation is clearly visible in profile,
with stable body/observer; the root snapshot changes visible position. Durable
copies are `../s30-adversarial-final-joint-before.png`,
`../s30-adversarial-final-joint-after.png` and
`../s30-adversarial-final-root-after.png`. These actual screenshots supplement the
exact vertex/matrix assertions. The initial rear-facing captures were superseded
because the body occluded most head motion; no acceptance was based on that view.

Reusable mutation generator: `python tools/s30_adversarial_render_mutants.py NAME`.
It writes copied sources outside the repo; the existing adversarial init script
replaces only that source in the test compilation. `restore-missing` is killed by
nested full-field restoration; `delayed-local-missing` by missing actual causal
vertices; `retired-draw` by emitted vertices after UNAVAILABLE. Initial `root-double`
survived because its tested root correction was identity: classified as an oracle
gap, not a production defect. The nonzero ROOT case kills it with "Original model
root was not applied exactly once" in
`../s30-adversarial-mutant-root-double-nonzero.log` (48s). Mutation invoker initially
had a PowerShell argument error (Cannot convert empty path); that configuration
failure was corrected and never counted as a kill. Other compiled assertion logs
are `../s30-adversarial-mutant-{restore-missing,delayed-local-missing,retired-draw}.log`.

`S30AdversarialPresentationCostProof` measures only applying/restoring original
model poses, with already evaluated changing nonzero root/joint fractions; it
excludes geometry evaluation, vertex emission and GPU drawing. For 64 Cow models
with seven parts, each consumer performs representability plus immediate and
deferred apply/restore. Warmed 160 samples in
`../s30-adversarial-renderer-and-cost.log` give:

| Consumers per support | Allocated bytes/batch | p95 ms | p99 ms |
| --- | ---: | ---: | ---: |
| 1 | 582198 | 0.3919 | 0.4214 |
| 16 | 9199616 | 8.0454 | 14.0054 |

This is an explicit linear part/consumer cost, not a G9 integrated benchmark or
a claim that the physical NFR-014 budget was measured here. Preparation remains
one renderer-model/catalog/resource association; steady consumption has no family
extraction/reflection or global entity scan. Shared restoration itself is O(parts)
per use; conversion and Saved/PartPose maps account for measured allocation. The
prepared association cap is 512 and reload clears it. The higher 16-consumer case
is a documented stress cost rather than silently treating one consumer as the
entire acceptance workload. Production hashes remained identical to
`../s30-adversarial-candidate-hashes.json` through these lanes.

The independent changed-retained-cube production holdout has both a compatible
positive and an incompatible negative. It passes in
`../s30-adversarial-compatibility-baseline.log` (45s). Its directed matcher mutation
and further representable-scale boundary review are still in execution; final
zero-change production acceptance has not yet been claimed.

### Signed-scale representability boundary — ADVERSARY

**PRODUCTION RED — FR-086 / I3:** the adapter rejects a drawable reflected local
TRS solely because its determinant sign differs from the baked neutral pose.
`S30AdversarialSignedModelPoseProof` verifies the actual original Cow head's
translateAndRotate against a local matrix with ZYX angles (0.2,-0.7,0.45) and
signed scale (-1.2,0.8,1.1). The original ModelPart oracle passes within 2e-4;
the determinant is finite/nonzero (-1.056). The physical HierarchyMotion uses
identical before/after local matrices, so this case has no shear, interpolation
or zero-scale crossing. The positive-scale control (mask 0) passes, but native
Prepared.apply returns empty at mask 1. The remaining parity/sign cases have not
yet executed after that assertion. This is not an unsupported model technology:
it uses the same already-compatible original ModelPart instance.

The native source lane after removing the prior test mutation fails exactly this
assertion in `../s30-adversarial-signed-model-pose.log`, BUILD FAILED in 38s,
compilation successful; source hashes still match
`../s30-adversarial-candidate-hashes.json`. Reproduce with `runClientGameTest`
and `-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S30AdversarialSignedModelPoseProof`
plus the adversarial init script and separate outputs. ADVERSARY has not changed
production. S30 remains RED until this representable local-matrix case is repaired
and the full affected acceptance pass is repeated.

The geometry compatibility directed mutant also compiles and is killed by
"Altered retained material was accepted by production adapter" in
`../s30-adversarial-mutant-geometry-mismatch.log` (41s). Together with the four
earlier families, five semantic mutation families are detected; configuration
errors and the original identity-root survivor were never counted as kills.

### Signed-scale repair and final native battery — ADVERSARY

The unchanged signed-scale holdout now passes all eight sign masks after the
IMPLEMENTER repair. The unchanged 512-TRS/gimbal/restoration/shear holdout and
actual compatibility positive/negative also pass. The actual deferred renderer
again covers all 80 material corners with 240 emitted vertices, nonzero ROOT
exactly once, and retirement/recovery across dimension, tracking/rebind, accepted
catalog/binding, short teleport and UNAVAILABLE. The complete five-class battery
including cost is green in
`../s30-adversarial-final-repair-client-retry.log`, BUILD SUCCESSFUL in 1m37s,
clean world/client shutdown. Production remained byte-identical to
`../s30-adversarial-final-candidate-hashes.json` during this pass.

The earlier attempt `../s30-adversarial-final-repair-client.log` passed the
functional assertions but deadlocked at Fabric's integrated-world close before
the cost class could execute. The preserved thread dump
`../s30-adversarial-final-repair-teardown-threads.txt` shows Render thread waiting
in IntegratedServer.halt/Minecraft.disconnect and Server/Test threads waiting on
Fabric ThreadingImpl Phaser; no collision presentation call is on those waiting
stacks. Classified as test-infrastructure shutdown synchronization, not a claimed
production defect or a green build. Only the verified ADVERSARY client process
was terminated after capture. The fixture now yields two ticks after runtime stop
before world.close; the complete retry exits cleanly. This does not claim a general
fix to Fabric's intermittent teardown problem. A separate automatic approval
timeout before the compatibility launch was retried successfully and contributes
no test result or mutation kill.

The full-battery cost sample after the signed repair reports 619201 bytes/batch,
p95/p99 1.5821/10.2874 ms at 64 supports/one consumer, and 9715712 bytes/batch,
p95/p99 9.806/20.6658 ms at 16 consumers. This timing variation is being checked
with an isolated native run; the corresponding adapter mutations are being
regenerated from the repaired source and repeated before the final zero-change
verdict. No S30 or full G4.6 closure is asserted by this intermediate record.

### Final independent costs, mutations and tracking RED — ADVERSARY

The repaired-source isolated native cost run
`../s30-adversarial-final-isolated-cost.log` exits BUILD SUCCESSFUL in 40s:
64 original Cow models / seven parts / one consumer use 619008 bytes per batch,
p95/p99 0.611/0.7062 ms; sixteen consumers use 9150464 bytes,
p95/p99 7.4851/9.9222 ms. The benchmark covers the adapter's representability,
immediate application/restoration and delayed application/restoration. It excludes
geometry evaluation, GPU cost and integrated frame budgets; it establishes the
bounded linear consumer cost, not G9 acceptance. Current-endpoint presentation
queries share the receiver's immutable cached frame for the same endpoint key,
so extraction/root/layer/draw revalidation does not redo geometry evaluation for
every boundary. Prepared model geometry remains outside those hot conversions.

Six semantic mutants have been killed by assertions. The three adapter mutants
were regenerated from the latest repaired source: final logs
`../s30-adversarial-final-mutant-restore-missing.log`,
`../s30-adversarial-final-mutant-geometry-mismatch.log` and
`../s30-adversarial-final-mutant-signed-parity.log` fail nested restoration,
altered material compatibility and signed pose acceptance respectively. The
unchanged renderer source's delayed-local-missing, retired-draw and nonzero
root-double kills remain applicable. None of the configuration failures or the
earlier identity-correction root-double survivor counts as a kill.

A further independent lifecycle holdout blocks the zero-change verdict:
`S30AdversarialTrackingAliasProof` receives the same available authoritative
endpoint through real networking with a newer recipient tracking generation.
The fixture intentionally publishes a trusted test envelope, freezes runtime
publication, and varies only that lifecycle axis; it does not claim a live
stop/start or reconnect integration run. The receiver accepts generation 1 -> 2
while retaining the same live client entity, presentation identity and endpoint
frame serial (5 in the recorded run). The previously queued original-model submit
still emits vertices. `../s30-adversarial-tracking-alias-envelope.log` fails in
28s, repeated unchanged in 25s in
`../s30-adversarial-tracking-alias-envelope-repeat.log`, with
`Queued original model drew across fresh tracking generation with reused
identity/frame serial`. Classification: production retirement failure at the
tracking barrier, not incompatible geometry, Euler decomposition or a build issue.
Reproduce via the existing init selector with
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S30AdversarialTrackingAliasProof`
and isolated build/run/cache directories. Production was not edited.

The discarded first fixture attempted a real runtime stop/start while requiring
an unchanged revision; its premise failed correctly because revision advanced
1 -> 2. That failed premise is not a production defect. The initial nonexistent
pose-history accessor was corrected to current().trackingGeneration() before
runtime testing. Existing five-class native green evidence and deterministic
visual captures remain valid for their scopes; S30/G4.6a does not close while this
independent tracking retirement defect remains. Full G4.6 and S31 are not closed.

### Tracking repair: independent zero-change review — ADVERSARY

The unchanged tracking-envelope holdout now rejects the retired queued submit
and positively draws a fresh one. The six-class native battery
`../s30-adversarial-tracking-repair-complete.log` passes in 1m2s with clean
integrated-world/client exit. It includes that holdout, actual deferred
ROOT/JOINT drawing, dimension A-B-A, catalog/binding replacement, short teleport,
UNAVAILABLE, 512 representable TRS cases, eight signed scale masks, nested and
exceptional restoration, invalid shear and actual cuboid mismatch rejection.
No test oracle was changed to accept the repair.

The new production tracking scalar is retained in immutable current/certified
frames, both presentation-cache keys and render tokens, and checked before
immediate/delayed consumption. It adds a local scalar equality with no new scan,
reflection, unbounded cache or geometry work at the draw boundary. Mutant
`tracking-alias` removes only that equality in a test-only compile replacement;
`../s30-adversarial-mutant-tracking-alias.log` fails semantically in 34s with the
same retired-draw assertion. Combined with the six prior semantic kills, the
campaign has seven kills; old runs are attributed to their exact source versions.
The original-source compilation was restored and native receiver
`AnatomyPresentationFrameTests` plus the tracking holdout pass in 37s in
`../s30-adversarial-final-native-receiver.log`, including certified-interval and
no-inferred-interval behavior. Generated mutation source copies were removed
only from the verified dedicated directory after evidence capture; the reusable
seven-family generator and immutable tests remain.

Updated fixed-view snapshots were inspected and preserved outside generated
run output as `../s30-adversarial-tracking-repair-joint-before.png`,
`../s30-adversarial-tracking-repair-joint-after.png` and
`../s30-adversarial-tracking-repair-root-after.png`. They visibly show the original
Cow head pose changing and the accepted root moving the original support while
the camera/HUD remain fixed. The actual emitted-vertex and physical-state
assertions remain the acceptance oracles; screenshots supplement them.
The native batch cost now reports 632762 bytes / p95 .4815 ms / p99 .624 ms
at 64 supports / one consumer, and 9682944 bytes / p95 2.9194 ms / p99 5.8002 ms
at sixteen consumers. Earlier isolated distributions remain recorded; these are
adapter-only costs, not an integrated frame-budget or G9 claim.

Final inspected presentation source hashes are preserved in
`../s30-adversarial-tracking-repair-hashes.json`; they remained unchanged through
mutation and final native revalidation. ADVERSARY made no production changes.
Complete independent review of the converged S30 original-model renderer scope
now returns zero changes. S30/G4.6a is adversarially GREEN, subject to the mandatory
ordinary suites on this exact candidate. IMPLEMENTER's server log
`../s30-server-after-tracking-fix.log` is already verified 463/463; ordinary client
completion remains IMPLEMENTER-owned. Full G4.6 is still open for S31's body
residual/camera requirements; this verdict does not implement or accept S31.

## Verification and closure

IMPLEMENTER candidate is present in `client.collision.presentation` and four
client mixins. Render-state extraction captures only an accepted endpoint token;
submission rechecks it against the live support and current catalog, replaces the
vanilla root matrix with the accepted root/model transform, and queues the
original model. Immediate layers apply the absolute local pose after vanilla
`setupAnim` and restore the shared fields on scope exit. `ModelFeatureRenderer`
rechecks the token after its own delayed `setupAnim` and applies/restores the
same local pose around actual buffer drawing; a retired queued submit does not
draw. Format-2 `ModelPart` and migrated precomputed-family bindings use the same
live-model compatibility check, while unsupported formats retain vanilla render.

The adapter inspects one baked-neutral live renderer object per catalog/resource
generation, comparing hierarchy and every cuboid's ID, part ownership and local
bounds (a stricter predicate than retained-only comparison). The inspection
restores animation translation, rotations, scales, visibility and skipDraw. A
prepared association includes renderer-model identity and epoch/revision/model/
filter/resource generation; negative results remain fail-closed for that
generation. At consumption, retained material and its ancestors must be visible,
and every local affine matrix must recompose as a representable `ModelPart` pose.

IMPLEMENTER client evidence: `../s30-implementer-client.log` is green for
`S30CausalModelAdapterTests` and `S30CausalRendererSubmitTests`. The first uses
two baked Cow trees, proves changed retained body cuboid rejection and reversible
nonunit joint pose. The second starts an actual prepared server, receives an
available causal packet through the client, changes only its accepted root, and
checks the original model matrix copied into Minecraft's `SubmitNodeCollection`,
the delayed leg pose and restoration, the shared `PoseStack`, and physical
position/AABB/velocity invariants. A subsequent real `unavailable` packet
retires that queued render state and the delayed draw callback is not invoked.
Fixed-camera screenshots from the same run:
`build/run/clientGameTest/screenshots/0000_s30-original-cow-before.png` and
`0001_s30-causal-original-cow.png`. The model moves by the injected accepted
root delta; the observer stands on a fixed stone platform, so camera fall does
not explain the change. `git diff --check` passes. The server suite
`../s30-implementer-server.log` passes **463/463** required tests in 51s;
the final ordinary client suite `../s30-ordinary-client.log` passes in 4m1s.
Independent adversarial evidence remains pending. G4.6 remains open.

**IMPLEMENTER repair after the first production RED:** The original
`Quaternionf.getEulerAnglesZYX` extraction rejected a valid original-Cow
head pose near the ZYX gimbal. The adapter now extracts Euler angles directly
from the normalized matrix columns, checks the exact `ModelPart` TRS
recomposition, and retains a singular-case/quaternion fallback. No shared
model field is written unless every local transform is representable. The
unchanged adversarial 512-pose oracle passes in both the IMPLEMENTER lane
(`../s30-fix-pose-proof.log`) and independent ADVERSARY lane
(`../s30-adversarial-model-pose-repaired.log`). The implementer-directed
client suite also passes (`../s30-implementer-client-after-fix.log`), as do
all **463/463** server GameTests (`../s30-server-after-fix.log`).

The first post-repair ordinary client launch failed in the earlier S29
`S29ImplementerAcceptedCatalogClientProof` assertion about a retained
prior-revision certificate (`../s30-ordinary-after-fix.log`). That S29 proof
passes alone (`../s30-s29-isolated.log`), and a new full ordinary client run
passes, including that exact proof (`../s30-ordinary-after-fix-retry.log`).
This is recorded as an intermittent full-suite observation, not as a claimed
fix to S29. The S30 independent renderer/lifecycle/performance review remains
open; no sprint closure follows from these green runs alone.

**IMPLEMENTER repair after the reflected-pose RED:** A local `ModelPart`
matrix with an odd number of negative scale axes can still be a valid TRS
even when the baked neutral pose has positive scales. The adapter now tries
signed scale assignments in order of distance from the neutral signs, checks
determinant parity, and accepts only an exact matrix recomposition. The
unchanged signed-scale original-model holdout passes all eight sign masks in
`../s30-signed-fix.log`; combined with the original 512-pose holdout, both
pass in `../s30-pose-combined-after-signed-fix.log`. ADVERSARY revalidation
and a complete zero-change review remain required before S30 can close. The
implementer-directed client suite passes in
`../s30-implementer-client-after-signed-fix.log`, the server suite passes
**463/463** in `../s30-server-after-signed-fix.log`, and the full ordinary
client suite passes in `../s30-ordinary-after-signed-fix.log`.

**IMPLEMENTER repair after the tracking-window alias RED:** The server can
accept a fresh support tracking generation while reusing the same live entity,
catalog/binding identity and endpoint frame serial. `PresentationFrame` now
publishes that accepted generation separately; both its result-cache key and
the queued renderer token include it. A draw submitted under the earlier
generation therefore fails revalidation, even when the endpoint serial is
identical. The unchanged network/actual-draw holdout passes in
`../s30-tracking-fix.log`. Independent repetition and final suites on this
candidate are recorded below.

**S30/G4.6a closure — IMPLEMENTER:** ADVERSARY's unchanged six-class renderer,
geometry, pose, tracking and cost battery passed in
`../s30-adversarial-tracking-repair-complete.log`; its native receiver/interval
check passed in `../s30-adversarial-final-native-receiver.log`. Seven semantic
mutants were detected. The independent visual captures and complete
zero-change review are in the ADVERSARY section above. IMPLEMENTER's targeted
client test passed in `../s30-implementer-client-after-tracking-fix.log`, and
`../s30-server-after-tracking-fix.log` passed all **463/463** server GameTests
and `build`. The first two ordinary client runs failed in different earlier
S29 catalog proofs (`../s30-ordinary-after-tracking-fix.log` and its `-retry`
log); the first proof passed in the second run. An assertion-only diagnostic
was added to the second S29 proof without changing its oracle or production.
The third full ordinary client run passed both S29 proofs and all other client
tests in `../s30-ordinary-diagnostic.log`, BUILD SUCCESSFUL 3m2s. This records
the intermittent S29 observations without claiming their cause is fixed.
`git diff --check` and staged diff checks passed. S30's original-model
presentation is closed; S31 body residual/camera and S32 observer provenance
remain open, so full G4.6 and the entity-collision epic are not closed.



\n