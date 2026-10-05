# S44 — retire the legacy physical runtime

Status: CLOSED locally for G5 tasks7-9; later gates and final owner acceptance remain open.
Active role for this section: **IMPLEMENTER**.

## Scope and thesis — IMPLEMENTER

S43 closed G5 task6 in its own local commit `0b7c5bea`, with a verified clean
tree. G5 tasks7–9 are the next dependency-coherent frontier: all category,
placement, passive accounting, native interaction/push and client authority
equivalents are now verified. The remaining legacy engine cannot survive as a
second physics, interaction, networking or presentation owner.

Normative scope: FR-002/003/007/011/022/023/053/093, the already verified G5
category/placement/native accounting obligations, NFR-007..014/027/035 and the
canonical migration architecture. FR-023 permits an explicit warned one-sided
surface data decoder; it does not justify runtime automatic_top, AABB-derived
anatomy, height capture, parallel carry or old movement references. No new
requirements, balance changes, general-family coverage or consumer/pack acceptance
are added here. G6–G9 and human-only needs remain later gates.

## Current state and investigation — IMPLEMENTER

A fresh `git fetch origin` after the S43 commit still resolves independent main
to `b5e62535`, already an ancestor. There is no incoming main commit to reconcile.
Its unrelated gameplay, mounts, gravity, inventory, equipment, render, gait and
reach must remain preserved during removal.

The source inventory exposes two dependencies that must be transferred before
their old owner is removed. Canonical transport, obstruction clipping and live
move wrappers currently use PlatformPhysics.enter/exit for the native pair-query
scope. That scope is a current canonical requirement, not a reason to keep the
legacy solver. PlatformCamera composes canonical causal residuals and the
mainline TinyMountCamera, smooths/clips the result and exposes the actual applied
offset used by MountRenderFrame. Those behaviors must move intact to the client
presentation owner while the legacy model-sampling contribution disappears.

Platforms and several mixins bifurcate on shared ownership. The inactive
canonical mode can still generate automatic_top, retain PlatformState, resolve
legacy surface placement, carry and publish old platform packets. PlatformVisuals
can sample living renderer/model state for that old route. Canonical catalog
migration already converts released data to exactly one binding or explicit
one-sided plane record, so data compatibility and the physical runtime must be
audited separately. Historical tests contain both cancelled automatic-top
expectations and still-required gameplay assertions; names alone cannot decide
which tests to preserve or retire.

## Implementation plan — IMPLEMENTER

- [x] I1 Finish a source/data/mixin/test consumer inventory and classify each
  legacy reference as cancelled behavior, current canonical behavior to transfer,
  unrelated mainline behavior to preserve, or justified data-only compatibility.
  Record the retirement boundary and required replacement evidence before edits.
  Do not delete a current consumer merely because its helper has a legacy name.
- [x] I2 Transfer the scoped native moving-body context to the canonical
  integration owner, with one context truth and the same enter/exit identity and
  exception/nesting restoration. Route canonical pair blocker replacement,
  transport and native clipping through it, retaining exactly-once native push.
  Recertify this ownership extension before relying on it; it is not a solver,
  contact owner or new geometry provider.
- [x] I3 Remove the legacy solver, geometry/state/participant caches and body
  state accessor. Make the shared facade and entity/category mixins use only
  the existing canonical contact/carry/eligibility/native interaction routes.
  DISABLED remains native and BINDING remains fail-closed; absent/unresolved
  geometry never produces a fallback plane or collider. Retain policy/category
  semantics, supported chains, gravity ownership and all native category releases.
- [x] I4 Retire legacy platform payload registration/publication/queues and
  old outgoing/incoming movement reference resolution. Preserve the existing
  canonical receipt/reference/reconciliation, native player/vehicle baselines,
  unsupported-state native packet behavior and current disconnect/teleport/
  dimension/reload fences. A retained baseline helper must have one canonical
  responsibility and no hidden legacy carry side effect.
- [x] I5 Retire legacy visual carry/model-sampling and its adapters/caches.
  Transfer the actual camera offset, clipping, smoothing/reset, canonical local/
  remote causal residuals and TinyMountCamera integration to their presentation
  owners. Preserve current MountRenderFrame/FirstPerson behavior and shared
  renderer hooks; remove only accessors/resources with no remaining requirement
  or consumer. Recertify changed presentation ownership and deterministic visual
  state before accepting removal.
- [x] I6 Keep only justified data migration under its decoder/catalog boundary.
  Retain FR-023's warned, deterministic explicit unilateral surface representation
  without creating sides/belly, live automatic_top or a parallel runtime. Preserve
  accepted canonical catalog/policy selection and transactional reload. Remove
  resources/options only used by the retired engine; obsolete keys cannot continue
  to own a second semantic path. Investigate any incompatibility before choosing
  to change schema or preparation rather than silently weakening migration.
- [x] I7 Classify historical fixtures individually. Preserve native gameplay and
  canonical physical assertions on real original prepared geometry; cancelled
  automatic-top/legacy-wire/model-sampling expectations may retire with reasons
  and identified canonical coverage. Update implementation-facing fixture seams
  for removed internals without replacing real movement with stored contact
  facts. ADVERSARY retains ownership of independent fixtures and infrastructure;
  unrevealed holdouts stay independent.
- [x] I8 Run focused and complete affected regression: ordinary build/server,
  full prepared original composition, default client, actual experimental carts,
  dedicated player/boat receipt/reference/reconciliation and applicable mount/
  renderer controls, including the actual pinned FirstPerson JAR and deterministic
  reference/candidate mount-camera captures for the changed presentation route.
  Require independent no-fallback/one-owner/lifecycle/native
  controls, targeted mutations, deterministic snapshots and locality/reuse
  evidence. Classify all failures and return to plan/code/fixture phase as needed.
  HOT_MOVE_QUERY/HOT_TICK/HOT_NETWORK/HOT_PRESENTATION preserve existing budgets;
  no reflection/model traversal/whole-world scans may leak into the new paths.
- [x] I9 Complete both full zero-change reviews, prove the structural absence of
  a second physical/interaction owner and surviving legacy runtime references,
  update executed VALIDATION and canonical G5 tasks7–9 only when their obligations
  pass, and create S44's own local closure commit. Verify HEAD/tree before G6.
  Keep global performance/coverage/consumer/pack/human acceptance in later gates.

The initial complete IMPLEMENTER plan review found no additional requirement or
scope change. Independent preimplementation convergence remains required before
production edits. Any newly required foundation remains UNPROVEN for its extended
scope until independently recertified within this sprint.

## Preimplementation model — ADVERSARY

Baseline: S43 closure HEAD0b7c5bea1d2ad4a171019fafb0ac285046f7f7e4,
clean tree observed; local origin/main b5e6253584b2233e5dd0039203c8a251bf10cdeb
is an ancestor. No production modifications or implementation plan have been
used to derive this model; historical tests have not been inspected for cases.
Authority: canonical requirements, architecture §8, G5 tasks7–9, current mainline
preservation rule, foundation audit §12 and continuous performance protocol.
This is a draft to place only in the forthcoming sprint's ADVERSARY section,
not a second roadmap or a parallel implementation plan.

The thesis is one retained physical/contact/network/presentation owner after
migration. Deletion by name is insufficient: retained wrappers/helpers/resources
must not implement another collision/carry/rebase/interaction path. Conversely,
mainline input, mounts, inventory, effective SCALE/gravity, reach, optional
compatibility and special-body semantics are not disposable legacy mechanics.

Independent invariants:

1. FR-001/002/003/004/022 and NFR-004/035: DISABLED gives native behavior,
BINDING contributes zero material queries/placement/contact/carry and never
falls back, READY uses only resolved certified geometry. Missing models, unknown
poses and recovery cannot invent a height plane or revive previous legacy state.
No client/server/wire/tick/render branch may select a second engine.
2. FR-023 and NFR-023: a surviving adapter is a deterministic data decoder,
with explicit migration warning and one canonical unilateral-plane meaning.
A surface migration cannot create volume/sides/underside, automatic_top or a
height/minY capture path. Unsupported/ambiguous/malformed declarations cannot
leave partial bindings or mixed revisions. Whether any adapter remains must be
justified by its actual input contract, not by retaining the whole platform DTO.
3. FR-007..013/053/093, NFR-033/034: policy/ratio/friction and effective dimensions
remain relational and shared. Eligible pairs retain native push exactly once;
rejected/excluded/unknown pairs and unrelated third entities retain native
collision, query, navigation, combat and interaction behavior without a material
wall. Dimensions changes retire local invalid dependents/provenance immediately.
4. FR-005/006/064..068/089..092: passengers/Tiny Mounts remain seats, not a second
contact. Boat/raft water priority and all hulls, carts' rail/off-rail handoff,
item age/pickup/merge/friction/despawn, falling-block water/hardening/placement/
timeout/impact, and vanilla Happy Ghast behavior survive through the canonical
owner or native delegation. Removing old flags must neither pause all vanilla
lifecycle nor create a second execution of a resumed event.
5. FR-061/062/069..071: passive deltas remain excluded from voluntary accounting,
fall damage remains native for real landing, and Growth landing uses a real
surface. Placement/raycast uses material identity, reach/policy/footprint/gravity,
clearance/permissions/inventory checks and one atomic body/anchor transaction.
Removing an interaction wrapper must preserve unrelated mainline interactions.
6. FR-072..076/094 and NFR-021/022/024/025: surviving public capabilities and
optional adapters preserve their ownership and version contracts. Core common
classes remain server-safe; optional mods/pack IDs do not become mandatory.
Removing a legacy helper does not authorize deleting unrelated methods or moving
client-only extraction/reflection into common physics. Consumer work beyond the
current gate remains separate, rather than claimed by a source rename.
7. FR-077..085 and NFR-015..018: the registry, sender/receiver and live baseline
paths contain only canonical authoritative transport. Old payloads/references
cannot write carry/contact/baselines; lifecycle barriers keep accepted history
and replay fences exact. Local owner/remote observer roles, historical recipient
retirement, claim-once, TTL/saturation and allow-flight=false remain valid.
8. FR-086..088: local and remote body/model/camera draw uses certified causal
material plus the native interpolation baseline exactly once. Removing legacy
visual state must neither remove required canonical residuals nor introduce a
second draw correction. Draw remains pure; barriers and deferred draw/fallback
continue to invalidate the same sample. Mainline mounts/attachments/FirstPerson
and consumer camera controls survive independently of collision ownership.
9. NFR-031/035/037..039: production classes, mixin configs, initializers,
resources, optional integrations and built JAR are audited for live reachability,
not only string absence. Historical fixtures are classified after this model:
obsolete legacy-engine expectations may retire, semantic oracles remain and
change only their setup if required. An empty selector, disabled tests or lowered
budget is not green. Server, prepared, real client, dedicated/RTT and affected
experimental contexts retain separate evidence and exact source/artifact hashes.
10. NFR-007..014/027: deletion must reduce/remove redundant runtime work without
replacing it with global contact/support/recipient scans or uncached evaluation.
Measure local candidate/dependent/historical-recipient scaling and preparation
reuse; review allocations, weak ownership, queue cleanup, packet fanout and
locking. Diagnose lifecycle/diagnostic cold work separately from hot move/tick,
network and presentation. No global TPS/heap/GC or manual-pack claim follows
from compilation or small functional lanes; final epic benchmark/human needs
remain G9 under the canonical gate.

Foundation status before candidate: canonical contact/receipt derived indexes
retain S43 TRUSTED evidence only for their existing scopes. Any owner, lifecycle,
adapter geometry/provenance, authority or hot-path extension returns to UNPROVEN
for its changed scope until independent recertification. Legacy physical routes
are removal candidates, not trusted substitutes. A temporary decoder must be
recertified as data-only and cannot bless unverified plane physics or pose frames.

Acceptance model: compare required semantics under equivalent native/material
inputs and require actual live positives. Source/bytecode audits and active
observer probes make absence of legacy dispatch measurable; targeted mutations
must demonstrate that a retained fallback/second carry/rebase or dropped native
semantic obligation is detected. Spatial/temporal projections or deterministic
captures complement machine assertions. Concrete holdout inputs stay private
until completed evidence or a reproducible defect is returned.

Independent full I1-I9 plan review — ADVERSARY: **PASS — funciona, continúa**.
The model above was persisted before reading the IMPLEMENTER plan or any
historical fixture as a source of cases. Subsequent production inventory confirms
that legacy-named wrappers also contain current canonical calls; I1/I2/I5
explicitly transfer these rather than treating their names as deletion proof.
The scoped moving-body context and actual combined camera offset are required
foundations for the new ownership boundary and remain UNPROVEN for that changed
scope until recertified. The plan requires exception/nesting restoration, actual
mainline mount/FirstPerson controls, source/runtime absence, native/material
positives, failure classification and exact regression evidence.

Decoder acceptance is data-only unless a canonical provider explicitly resolves
the resulting primitive: a surviving record does not invent available physics.
The warned unilateral meaning must stay unique and transactional; incomplete
models remain empty and cannot trigger DISABLED/BINDING/READY fallback. Preserving
valid current binding/preparation/policy semantics is part of I6, independently
of removing height capture and visual model sampling. Historical setup changes
must preserve live semantic oracles and meaningful selectors; no retired physical
expectation becomes a replacement test merely by asserting an empty stored map.

No operational contradiction emerged from the complete plan pass. I1-I9 can
proceed within G5 tasks7–9; no additional product requirement or production edit
was introduced by ADVERSARY. This planning PASS is not runtime acceptance, does
not make changed foundations TRUSTED and does not close G5. Concrete holdout
inputs remain private until completed evidence or reproducible defects.

## Implementation and executed verification — IMPLEMENTER

I1 inventory and retirement boundary, before production edits:

| Consumer | Classification and treatment |
| --- | --- |
| PlatformPhysics, PlatformGeometry, PlatformState/Body, participant/margin/automatic caches | Cancelled upper-plane capture, step, carry and secondary contact owner. Remove, including Entity.push cancellation. |
| PlatformPhysics.enter/exit and canonical clipping callers | Current scoped native pair-query ownership. Transfer to MovingBodyContext, retaining identity/nesting/finally semantics and READY/predicting-only replacement. |
| Platforms category/ordinary/adaptTransport, eligibility, friction and tick pipeline | Retain canonical delegation and existing policy precedence; remove definition/AABB and legacy ancestry fallthrough. The broadphase keeps its fixed conservative margin, never data-derived anatomy. |
| PlatformConnection and server packet wrappers | Transfer the native player/lastVehicle baseline bridge and canonical reference resolution; remove old reference/carry branches. |
| PlatformClient/Payload/MovePayload/Networking/MovementReference | Remove registration, client pending queues, publication and old coordinate authority. |
| PlatformCamera, renderer/state hooks, MountRenderFrame and CausalBodyResidual | Transfer camera and render residual composition to canonical presentation names; preserve clipping/smoothing/appliedOffset and mainline TinyMountCamera/FirstPerson. |
| PlatformVisuals, PlatformVariantModels, AgeableModelAccess | Remove legacy model sampling, adapters and caches. ModelPartAccess remains canonical preparation; LivingRendererAccess remains required by independent mainline bee animation checks. |
| Placement/category/friction mixins and GrowthImpact | Keep canonical atomic placement, category lifecycle/native accounting and public support queries. Remove only legacy placement and ServerPlayer state-only clearing hooks; common lifecycle invalidation stays. |
| PlatformDefinition/Policy and LegacyCollisionData/catalog migration | Retain bounded data decode and canonical preparation bridges, with explicit warned unilateral records and no geometry for plane-only input. Remove shipped plane-only profiles and obsolete automatic_surfaces option. Preserve current anatomical prepared profiles, engine identifiers and transactional catalog semantics. |
| Historical manual core registration | These fixtures currently obtain eligibility through a legacy profile despite supplying actual convex geometry. Replace that setup with explicit test-owned canonical binding policy; do not add an implicit production fallback or turn DISABLED fixtures into prepared sessions. Keep original geometry/movement/assertions and causal descriptors. |

Historical fixture classification is by behavior:

* PlatformTests physical upper-plane cases (occupiedBoatOnGiantPlayer,
  sneakAndKnockback, offRailCartAndPassiveAccounting, closerSizeContactAndTransport,
  anvilImpactOnce, risingSurfaceAndWall, physicalOnlyAndLifecycle,
  stationaryItemsAndChains, fallingBlockRemainsEntity, movingSupportProof,
  boatAndPlayerProof, orientedFootprint) retire their cancelled engine setup.
  Their still-normative oracles remain required in S33/S35–S43 prepared/category,
  S00–S14 core and G2 native controls. packetReferencesAreScoped is obsolete wire
  behavior, replaced by S25/S28/S29/S42 canonical receipts/reference/lifecycle.
  decodedSwitches, widthBoundaryAndForeignScale and profileValidation retain
  data/policy/dimension assertions with explicit canonical setup.
* PlatformPlacementTests retains preservesVanillaBabyEggInteraction. Its other
  three upper-plane scenarios retire only after confirming the corresponding
  real prepared S33 atomic placement, permissions/creative inventory, oversized
  and obstruction controls remain selected and green.
* MountPlaytestTests retains every unrelated equipment/wolf/trust/passenger test.
  automaticSurfaceNeedsNoSpeciesProfile is cancelled FR-022 fallback;
  normalPlayerOnGrowthThreeProfiles belongs to G6's actual geometry family gate;
  playerHeadRemainsAnatomical's manufactured head surface is cancelled FR-024.
  Actual exported player composition and no special head scale remain covered
  by prepared geometry and mainline mount/render tests.
* PlatformClientProof is the old ghast upper-plane/old-wire/visual-sampling proof.
  Dedicated canonical player/boat RTT0/100/200, S30–S32 draw/camera, S36/S41/S43
  real client physics and pinned FirstPerson controls replace its live oracles.
  Its optional soak property must not survive as an empty acceptance selector.
* All other PlatformPhysics/Camera/Connection references are current core,
  prepared, camera or baseline assertions. Only their owner/import/setup changes;
  ADVERSARY owns migration of independent fixtures. No assertion or tolerance
  is weakened because its previous helper is removed.

The complete IMPLEMENTER inventory pass adds no requirement or scope change to
I1–I9. The independent plan PASS permits implementation; affected ownership
foundations remain UNPROVEN until runtime/holdout recertification.

Candidate I2–I7: the old solver/state/geometry/caches and both platform payloads,
registration/publication/queues/reference resolution are deleted. MovingBodyContext
owns the single native query scope; NativeMovementBaselines preserves the exact
player/lastVehicle coordinate bridge. Entity/category/placement hooks now delegate
only to READY canonical physics or native behavior. No Entity.push cancellation
remains. BodyCameraOffset and BodyRenderOffset retain canonical residuals and
current mainline mount offset, smoothing/clipping/appliedOffset; the legacy living
model sampler and its exclusive accessors are deleted. Data DTOs and existing
canonical migration/preparation bridges remain, while the 18 shipped plane-only
profiles, automatic_surfaces option and obsolete platformSoak selector retire.

Production common/client compilation passed in 22s
(`../s44-production-compile-i1.log`); this is compilation only. The first fixture
compile found an IMPLEMENTER compound-var syntax error, repaired as explicit
Vec3. Subsequent compilation found 26 deleted Platforms.state references in
historical fixtures: these are infrastructure migration, not physical RED.
Their physical movement/contact/velocity/lifecycle assertions stay unchanged;
deleted secondary-state assertions now inspect actual retired class/accessor
absence through TestRetiredPlatformRuntime once, without a fictional empty state.
ADVERSARY owns the independent setup/import/gate adaptations. TestCoreBindings
preserves accepted catalog policy first, then existing explicit binding, then
test-owned raw-core policy which inherits global enabled. registerCore requires
an explicit matching entity binding and creates neither a causal descriptor nor
a READY session. Compilation did not certify the ownership extensions; the
following actual executions supply the affected runtime regressions.

Final IMPLEMENTER executions on the frozen production candidate:

- Ordinary build/server: 466/466, clean exit2m24 in
  `../s44-server-i3-full.log`; final build including server466/466, clean
  exit1m57 in `../s44-build-final.log`. The three I2 failures were infrastructure:
  two eligibility preconditions ran before explicit fixture binding, and one
  Happy Ghast assertion confused ordinary classification with material support.
  Their repaired, original physical/category oracles pass. Test count is
  483 minus19 cancelled old-engine assertions plus2 new real retirement controls.
- Original prepared composition: 2/2 with every scenario0–23, clean exit1m57
  (`../s44-prepared-i3-full.log`), including S43's28 native comparisons and14
  nonzero push controls. Placement, item, boat, ordinary cart, falling-block,
  gravity, chains and accounting assertions still execute on their original
  selected fixtures; S33's documented placement-specific fixture limit remains.
- Default client first passes31 in6m58; final default32 after ADVERSARY promotes
  the scope holdout passes in3m34, clean exit (`../s44-client-final.log`). Source
  and actually processed manifests agree exactly; S42's retired-connection fence,
  S43 real client pairs and current presentation controls remain selected.
- Actual experimental minecart flag/native behavior and rail exit/reacquisition,
  ratio/policy/native push and obstruction controls pass in1m41
  (`../s44-experimental-client.log`). Dedicated native player+boat movement at
  RTT0/100/200 passes with zero player/vehicle corrections, no trace drops and
  client/server convergence in2m39 (`../s44-receipt-reconciliation.log`).
- Actual pinned FirstPerson2.7.2/26.2 runs on immutable S43 HEAD0b7c5bea and
  candidate pass in4m21/3m33. All three actual submitted-frame probes have finite
  anchors and at least71 FirstPerson frames. Each lane captures31 screenshots.
  IMPLEMENTER inspected the two durable comparison sheets and62 source hashes
  in `docs/evidence/S44/firstperson-comparison.json`: body/held-item framing,
  mount seating, bee player inventory, chicken/wolf equipment and native horse
  remain consistent. Inputs, angles and tick waits are fixed, but seed/particles/
  render partial are not frozen; these are actual bounded visual regression
  comparisons, not pixel equality. ADVERSARY's independent fixed-partial camera
  snapshot supplies the deterministic semantic projection for the changed route.
- Final artifact SHA256
  `9c258b33e5a147360e91a2ce66f2273ee99a7b5ad676931bc77d7c69431b56bd`:
  1,146,458bytes/720 entries, no retired runtime classes, plane-profile files,
  leaked test classes or unresolved configured mixins. Source inventory358 files/
  242 Java sources has no executable retired-type reference and every configured
  mixin resolves. An empty ZIP directory is metadata, not a shipped profile.
  Exact production hashes,370 GameTest source classes, manifests and log hashes
  are in `docs/evidence/S44/implementation-verification.json`.
- Deprecation audits retain all diagnostics: final build12 and final client4
  intentional test-only adapters; no production diagnostic or warning cap.
  Default `git diff --check` passes. Seven ownership/protocol source gates pass;
  the eighth S25 authority gate still reads removed files and was returned to
  ADVERSARY as infrastructure, with its metadata authority obligations intact.

IMPLEMENTER performance/reachability review: MovingBodyContext is O(1) HOT_MOVE_QUERY
thread identity with exact restoration and no contact/geometry cache. The facade
removes global participant/margin bookkeeping and delegates to the same bounded
canonical index/contact chain. Its existing per-query policy conversion and
chain-set allocation are unchanged from S43, not a new cache or second truth.
NativeMovementBaselines is HOT_NETWORK O(1), updating the existing player or
lastVehicle coordinates exactly once. Camera retains the same five block rays,
bounded smoothing/clamp and mainline mount composition; the model-sampling loop
and legacy receiver queues retire. Decoder reflection/model traversal remains
PREPARATION, not physics runtime. Surviving Platform* names identify pure policy,
data, canonical placement/category hooks or current lifecycle/mount hooks; names
alone are not evidence of a second engine. The eight independent replacement/
locality controls and fixed-partial presentation sensitivity remain ADVERSARY
acceptance, not IMPLEMENTER assertions about unrevealed holdouts.

## Adversarial verification — ADVERSARY

The execution record below supersedes the initial preparation status. S43's
evidence is reusable only within its stated scope; affected paths were rerun.

Preparation — ADVERSARY: private live absence/lifecycle controls and isolated
client selector are prepared for the compilable candidate. Their concrete inputs
remain private and no runtime PASS is claimed. An offline inventory was executed
against immutable S43 HEAD0b7c5bea: 389 production/resource files, 255 Java
sources and all configured production mixins resolved to sources. The inventory
records exact source hashes, legacy references, ownership bifurcations and cache/
context declarations for candidate comparison; it is evidence of the baseline,
not proof of retirement or live reachability. Production and IMPLEMENTER fixtures
were not modified by this preparation.

Executed preparation/candidate checks — ADVERSARY:

- Immutable baseline inventory: S43 HEAD0b7c5bea, 389 production/resource files,
  255 Java sources, no unresolved production mixin. Initial S44 inventory: 358
  files, 242 Java sources, no references to the ten inventoried retired runtime
  class names and no unresolved production mixin. This structural observation is
  not yet full reachability/artifact acceptance.
- Historical native context/baseline/camera fixtures use the new owner names;
  their assertions and inputs remain unchanged. Hand-authored core registrations
  use TestCoreBindings: accepted catalog selection first, existing binding next,
  explicit test binding only when neither exists; policy enabled inherits global
  precedence. The raw provider/descriptor remains original, and this seam does
  not create a prepared runtime/session or fabricate geometry. The cancelled
  G2 pre-READY legacy push-suppression expectation is replaced by live DISABLED
  native movement and both nonzero push controls, preserving its READY checks.
- S26 authority source gate passes after mapping its four decisions to the new
  context/move hooks. Five independent shadow-source mutations removing a
  decision's authority fence are rejected; no production source was mutated.
- Independent live DISABLED absence and scope lifecycle controls passed in a
  real client/integrated server with clean shutdown: ../s44-adversary-absence-scope-third.log, BUILD SUCCESSFUL4m15, SHA256
  af10db73f03d9dd066424114db40ab02efa352e69aa2e28e47a7edbb48fb3057.
  The former body's fall remained exactly requested and support motion added no
  carry. Scope checks cover exact nesting, exceptional restoration, thread
  isolation and final retirement. Physical replacement/locality and presentation
  recertification are still pending, so changed foundations are not closed.
- Two earlier runs failed before runtime in historical/IMPLEMENTER fixture
  compilation; they are infrastructure RED, not physical evidence. The two
  server preconditions reported by IMPLEMENTER (bounded candidates/passengers)
  now install their same raw snapshots before querying eligibility; their
  numerical budgets and candidate oracles remain intact, reexecution pending.


Final independent candidate verification — ADVERSARY:

- Retained physical/lifecycle/locality battery: eight unchanged private S43
  oracles passed, clean real-client shutdown, ../s44-adversary-retained-first.log
  (2m52, SHA256 3f82e677a4b88f9fc8c9eb04a6b1eb5b9017d8bc7206796b823e102f079976f0).
  Both native push callers remain nonzero and equal to controls; real carry is
  applied once; body/support scale retirement, equal-UUID receipt histories,
  replacement, claims, saturation, TTL and teardown remain intact. A thousand
  revalidations add zero geometry/joint evaluations; 2048 unrelated contacts and
  128 unrelated receipt histories add zero foreign visits. These counts establish
  local work, not a live 128-client network/GC benchmark.
- Presentation battery: all 20 public S31 adversarial proofs plus one unchanged
  near-plane oracle with a diagnostic snapshot passed (21 entrypoints),
  ../s44-adversary-camera-first.log (5m02, SHA256
  bff57fbacf68c2f6205c8e1f17ee13628212d725fc76120391ff755e6730e7b5).
  This includes gravity/ownership/lifecycle barriers, late contact, ROOT/JOINT,
  partial draw, dual-role actor, exact duplicate and baseline changes.
- Ordinary server: 466/466 passed in ../s44-adversary-server-first.log (4m18,
  SHA256 dba6c89e8e13a1a121a9d83ef85e461820852d05d5c080304f0766cf02928d38).
  The candidate/passenger raw-core setup fixes preserve their exact snapshots,
  numerical budgets and assertions. The subsequent snapshot-lambda cleanup
  captures the same immutable value; IMPLEMENTER's final 466/466 also passes it.
- Scope sensitivity: isolated mutant replacing ThreadLocal with a global slot
  fails the unchanged worker identity assertion in
  ../s44-adversary-mutant-scope-first.log (expected RED). Near-plane sensitivity:
  isolated center-ray-only camera mutant fails the unchanged safety assertion in
  ../s44-adversary-mutant-camera-first.log (expected RED). Neither mutation touched
  production. Promoted S44AdversarialScopeProof passes independently in
  ../s44-adversary-public-scope-first.log (1m13, SHA256
  52d8a1d4b75e3d77df149f53a383f286da7e84a1db879eeaef3474f68f5ef7a4)
  and is the 32nd default client entrypoint.
- S25's last infrastructure failure was stale reads of removed legacy source
  files, not a physical RED. Its replacement checks actual absence across both
  production sides, exactly one native-packet emitter and registered mixin,
  current-player/thread/position/ownership fences; the C2S metadata/schema/
  receiver audit remains intact. `python tools/s25_reference_authority_gate.py`
  passes. `python tools/s44_reference_authority_sensitivity.py` reproduces a
  baseline PASS and eleven expected failures on isolated source copies, including
  qualified/unqualified duplicate emitters, missing emitter, legacy resurrection,
  ownership/thread/position fences, physical fields/types and registration/mixin
  removal. Evidence: docs/evidence/S44/reference-authority.json. This is a static
  source gate, complemented by the real packet/runtime suites, not a Java parser.
- Frozen artifact: ordinary diagnostic JAR SHA256
  9c258b33e5a147360e91a2ce66f2273ee99a7b5ad676931bc77d7c69431b56bd,
  1,146,458 bytes, 720 entries, all 72 production mixins resolve. Fourteen retired
  class families, private test classes and GameTest entrypoints are absent. This
  is artifact/classpath inspection, not a published/remapped release assertion.
- Zero-change source inventory remains exactly
  32e3b0bfdd6ac731279cce25cb07c09e433a6297cae8b2c8ef3017adcecf584f:
  358 production/resource files and 242 Java sources; compared with immutable S43,
  13 existing Java files changed, eight owners added and 21 Java files removed.
  Comparison normalizes only line endings; unaffected mainline consumers remain
  identical. New helpers carry exact canonical context/baseline/camera state;
  remaining Platform names denote policy/data/category/placement/lifecycle hooks.
  No legacy automatic plane, parallel carry, coordinate receiver or fallback
  solver remains reachable in the inspected source and JAR.
- Costs/foundations: MovingBodyContext is bounded O(1) ThreadLocal scope with
  exact nested/exception retirement. NativeMovementBaselines updates existing
  native player/vehicle coordinates once, with no new truth/cache. Camera keeps
  the existing five rays and bounded smoothing/clip; removed legacy sampling and
  automatic participant scans do not survive. Changed owner extensions are
  TRUSTED within the physical, thread, presentation and locality controls above;
  no global TPS/FPS/heap bound is inferred. Existing contact/receipt foundations
  retain their unchanged owner and acceptance scope.
- Deterministic visual evidence: docs/evidence/S44/camera-near-plane.png and JSON
  record actual fixed-partial .5 runtime corners/rays/offset. The independent
  exponential open offset is .039346934028736694; the leading native-block gap
  .00820228596155887 is below the .02 safety margin, requiring and observing zero
  additional offset, with physical position unchanged. PNG SHA256
  11e2416a13e77204e1b9c79fe3712679bf08bfda777d1418b0c404f3e998aa6b.
  This measured projection is not a screenshot or a frozen whole-world pixel
  comparison. Both actual FirstPerson/mount comparison sheets were also inspected:
  bee/chicken/wolf first-person poses, third-person front/top, mount menus and
  animated horse show no regression within inspected views. Their partial/time
  is not frozen, so particle/animation variation is not pixel equivalence.
- IMPLEMENTER's final logs were independently read: ../s44-build-final.log
  466/466 (1m57), ../s44-client-final.log default32 (3m34),
  ../s44-prepared-i3-full.log prepared 2/2, ../s44-experimental-client.log real
  experimental handoff, ../s44-receipt-reconciliation.log player/boat RTT
  0/100/200 with zero corrections and convergent positions. Actual FirstPerson
  2.7.2 reference/candidate and paired capture hashes remain separately attributed
  to IMPLEMENTER execution, not independent ADVERSARY runtime executions.

Final zero-change review — ADVERSARY: funciona, continúa. I1–I9 converge and the
changed foundations are recertified within S44's scope; no unresolved production
RED remains. The stale S25 gate was infrastructure and is now executable and
sensitivity-tested. Production was never edited by ADVERSARY. No more fixture
promotion is pending. Human/full-pack acceptance and global NFR-014 stress,
heap/GC and frame-time percentiles remain G9; this verdict does not advance them
or G6. IMPLEMENTER owns the closure commit and final canonical status updates.

## Final review and closure

Both complete review passes require zero additional production or test changes.
IMPLEMENTER independently checked the frozen358 source hashes against the executed
candidate, exact manifest equality, retained current-mainline consumers, artifact
absence, all eight source gates and the original physical/native regressions.
ADVERSARY's final acceptance and changed-owner TRUSTED scope are recorded above.
The last S25 source-file failure was corrected as infrastructure and its eleven
shadow mutations are sensitive; the final gate campaign is green. The only
subsequent correction repaired the adversarial documentation block's CP1252 bytes
to UTF-8 without altering evidence; all modified text files decode as UTF-8.

G5 tasks7-9 close through S44's own local closure commit (`Sprint: S44`), including
implementation, preserved/migrated tests, independent scope/gates and necessary
evidence/docs. No later sprint begins before HEAD/tree verification. G6 coverage,
G7 consumer, G8 pack, integrated NFR-014 stress and human acceptance stay open;
real owner needs remain deferred to the whole epic's final G9 matrix.
