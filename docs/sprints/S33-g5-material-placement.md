# S33 / G5.1 — material-face placement

Status: S33/G5.1 closed after the S34/G4 repair, real native packet holdouts,
full prepared regression and independent zero-change cross-sprint review. The
logical local commit records this closure; later G5 work remains open.

## Scope and thesis — IMPLEMENTER

At the end of S33, an eligible boat/raft item or spawn egg can be placed on a
specific accepted anatomical face through the real interaction path, with no
legacy platform definition needed. A rejected placement leaves the entity world
and inventory unchanged; a success creates one body and one Scale anchor. Real
server GameTests must exercise six faces, survival/creative and each rejection
axis. The raycast must hit eligible material, not a guessed entity AABB.

Included: FR-069..071 and the placement-facing parts of FR-007, FR-053,
FR-089..093 and NFR-033..035. Existing G2 eligibility, G3 material snapshots,
G4 authoritative contact and vanilla interaction behavior remain invariants.
Special-body lifecycle, sneak/jump, fall/exhaustion and removal of the legacy
engine remain later G5 sprints; no publication is in scope.

## State reconstructed — IMPLEMENTER

`PlatformPlacement.interact` currently requires `Platforms.definition(support)`
before it tests `AnatomyApi.mode(player)`. An anatomy-only canonical support can
therefore never reach `anatomicalInteract`. The anatomical branch checks build
permissions, block/entity overlap and a material raycast, but it has no explicit
reach bound or selected-face footprint test. It calls `addFreshEntity` before
`attachAtContact`; a later failed attach discards the entity after it entered
the world. The legacy placement tests primarily assert `Platforms.state` and
`PlatformPhysics.carry`, so they do not establish the canonical contract.
The same outer guard checks the legacy policy's global `enabled` flag before
canonical category/support/profile overrides can resolve; canonical routing
must let the existing `Platforms.eligible` rule decide the effective policy.

`AnatomyMovement.raycast` already selects a real `ConvexBox` piece/face/local
point and exposes its material identity through the public API.
`AnatomyMovement.confirm` checks active state,
eligibility, current piece/revision and gravity-supported normal, then records
the anchor; it does not require the body to have been added to the level.
This makes pre-add confirmation plausible, but GameTests must verify the full
path and cleanup when `addFreshEntity` refuses a body. The public `RayHit`
does not expose the selected `ConvexBox`; a narrow backend predicate can use
`ConvexBox.coordinates` to validate the prospective body's support-facing
footprint against the current piece/revision without leaking mutable geometry.
The effective entity AABB after item components and owner gravity is the
physical shape to validate. Scale positions that AABB against the face; its
gravity owner, not Scale, owns any dimension rotation. A vanilla body under a
GameTest-only gravity read override does not magically acquire Gravity
Changer's rotated box, so the six-face fixture must check its actual AABB and
must not claim to prove external-mod box rotation.
Minecraft 26.2 exposes `Player.isWithinEntityInteractionRange` and
`entityInteractionRange`; the final server-side check must also bound the
specific clicked material point, not merely the target entity's large AABB.
Scale reads effective body gravity through the registered adapter. Side-face
success cases must provide a compatible chosen gravity through that owner;
placement itself must not install another gravity writer. The six-face real
interaction fixture uses a development-only, scoped candidate-created hook:
after basic construction, before placement or confirmation, the fixture calls
the existing `GravityFrames.overrideForTests(candidate, frame)` on that exact
body. The hook is absent from normal gameplay and must reset in `finally`.
One real `Player.interactOn` call per frame then checks the selected material
face and actual resulting AABB. A separate optional-Gravity-Changer run is
required before claiming that mod's rotated shape compatibility.
`Platforms.definition` reads a separate legacy registry and may synthesize
`automatic_top` when enabled. The canonical-only fixture must disable that
fallback and still obtain a valid anatomy binding/pose; success may not depend
on either a legacy registry entry or a generated top surface.
`anatomicalInteract` currently returns `SUCCESS` immediately on the client,
before asking whether a material face was hit. That can consume an ordinary
entity interaction locally even when the server would fall back to vanilla;
the routing plan must distinguish a locally known miss from a provisional
eligible hit, with the server retaining final authority.
The existing `target.getBoundingBox().inflate(.25)` click test runs before
READY dispatch. It is a legacy packet guard only: a selected material piece
may legitimately extend beyond that box. READY instead validates finite
packet-local intent, the authoritative material ray and player reach to the
actual face point; the vanilla AABB must never veto that selection.
`EntityType.create(ServerLevel, PostSpawnProcessor, ...)` runs
`Mob.finalizeSpawn` before the caller receives the candidate. A zombie can
change or insert a chicken there. Therefore using that overload for a
rejectable preflight cannot satisfy atomic failure, even if the candidate is
discarded. The transaction needs a body-local preparation path without
`finalizeSpawn` or arbitrary spawn callbacks before all rejectable checks.
Admission is subordinate to ownership: Scale must not reject a vanilla
same-species baby egg or a pair/material miss it does not manage merely
because the stack carries entity data unsafe for Scale's atomic placement.
Unsupported custom data/type means `PASS` to vanilla before Scale constructs
anything, except BINDING's existing fail-closed barrier. Only after a vetted
body proves material eligibility does a placement validation failure mean
`FAIL`.

Routing decision table (in order):

| Case | Scale result | Reason |
| --- | --- | --- |
| Non-placement item, excluded support or anatomy-free non-platform pair | `PASS` | Vanilla owns the interaction; no candidate is constructed. |
| BINDING with an otherwise placement-capable item/support | `FAIL` | Shared physics owns but cannot query material; no candidate is constructed. |
| READY same-species egg on ageable adult | `PASS` | Vanilla baby interaction has priority, regardless of stack data; no candidate is constructed. |
| READY input with unvetted type or nonempty custom `ENTITY_DATA` tag | `PASS` | Scale cannot safely construct a probe; vanilla retains its ordinary behavior. |
| READY vetted candidate whose body-specific material ray misses or whose category/ratio is ineligible | `PASS` | The safe candidate is discarded; no material placement was owned by Scale. |
| READY vetted eligible body and selected material, but reach/permissions/footprint/clearance/insertion fails | `FAIL` | Scale owns the attempted placement and leaves no side effect. |
| READY vetted eligible body and every check passes | `SUCCESS` | One insertion, anchor, consume/stat/event as appropriate. |

The body-specific material query needs a prospective body: the current
`AnatomyApi.raycast(Entity,...)` filters by that body's category, ratio and
active anatomy ownership. It must never substitute the player as observer.
The vetted basic factory therefore follows the early no-candidate PASS rows
and precedes the raycast. The candidate is side-effect-free relative to the
world by the I2 admission invariant; it is discarded on a material miss or
ineligible pair, returning `PASS`. Unsupported data returns `PASS` without
invoking a factory, so it cannot suppress a vanilla baby interaction or mutate
a nonparticipant as a failed Scale placement. Vanilla 26.2 spawn eggs carry
their entity type in `DataComponents.ENTITY_DATA` even with no custom payload;
the admission check accepts that required type-only component and rejects only
a nonempty custom tag or a mismatched type. The client uses the same vetted
body probe only to distinguish a known local miss from provisional success;
server validation remains authoritative. A temporarily unavailable client
catalog is unknown rather than a certified material miss.

## Provisional implementation plan — IMPLEMENTER

- [x] I1 Route READY/canonical supports to material placement without requiring
  a legacy definition. Preserve PASS for vanilla interactions when no eligible
  material exists, including same-species baby eggs, and preserve the BINDING
  barrier. On the client, do not return provisional SUCCESS for a known
  material miss; the server makes the final placement decision. Do not let the
  legacy global `enabled` precheck override a canonical category/support/profile
  policy decision. Move the vanilla AABB click guard into the legacy branch;
  READY validates material identity, the packet point and exact face reach.
- [x] I2 Validate player reach, hand item availability and actual server
  permissions before creating/adding a body; use Minecraft 26.2 interaction
  distance semantics rather than a new hardcoded range. Resolve the type and
  reject disabled/empty stack data before construction. Route unsupported
  stack data or unvetted types to `PASS` before construction; keep same-species
  baby `PASS` even earlier. After those no-candidate exits, construct one vetted
  body-local probe and use that exact body's category/dimensions for material
  raycast and policy. Never use
  `EntityType.create(ServerLevel,PostSpawnProcessor,...)` for a rejectable
  attempt: it executes `finalizeSpawn`, whose side effects cannot be rolled
  back by discarding the prospective body. Prepare only a body-local candidate
  through the basic `EntityType.create(Level,SPAWN_ITEM_USE)` factory and a
  separately audited safe stack-component application; disallow arbitrary
  nonempty custom `ENTITY_DATA`/non-vetted factory or postprocessor hooks in
  this atomic route. The vanilla type-only egg component is valid input.
  Such unsupported input must leave Scale through `PASS` before construction.
  Prove vanilla factory
  and component operations for admitted types cannot insert/change unrelated
  entities. Recheck category, dimensions, eligibility and clearance after
  body-local configuration. Record the deliberate loss of random
  `finalizeSpawn` variants rather than silently claiming vanilla equivalence;
  if those variants are required, add a separate transactional initializer in
  a later sprint before widening the accepted inputs. A `PASS` is explicitly
  vanilla's own interaction, never reported as a rejected Scale placement;
  the atomic no-mutation guarantee applies to Scale's `FAIL` attempts.
- [x] I3 Compute body position/orientation from the chosen face and effective
  gravity. Verify the entire oriented support-facing footprint fits that
  selected `ConvexBox` face, including narrow/rotated/sheared pieces and
  near-edge placement. Resolve current geometry by material identity and
  revision inside the anatomy backend; expose only a validation predicate to
  placement. Project the four gravity-facing AABB corners along the effective
  gravity onto the selected material plane and require every projected point
  to lie inside that face, with bounded numerical tolerance. Retain the
  block/world-border/other-entity collision checks.
- [x] I4 Confirm one contact before world insertion where supported by the
  public API, then insert exactly once. On failure, clear transient contact and
  leave no entity, passenger, changed existing entity, item consumption, stat
  or placement event. On success, consume once in survival, never in creative,
  and publish one anchor. Verify that insertion callbacks and passenger state
  cannot invalidate a committed candidate; a failed `addFreshEntity` must
  clear the pre-add anchor and leave no observer-visible body.
- [x] I5 Add real server GameTests for each reject axis and all six faces,
  survival/creative, boat/raft and spawn egg, same-species baby fallback,
  canonical-only supports and non-eligible vanilla-pair behavior. Include
  lifecycle/rollback assertions rather than checking result codes alone. The
  scoped development fixture installs a frame on the exact newborn candidate
  before the actual `Player.interactOn` route reaches `attachAtContact`, and
  asserts actual AABB orientation/footprint rather than inferring it from a
  direction enum. Add a zombie-egg/chicken rollback regression for a rejected
  click, including existing-chicken state, new-chicken count and riding.
- [x] I6 Run relevant existing suites, independent adversarial battery,
  zero-change final review and local logical commit; record executed evidence.

The planned failure matrix records entity count/identity and riding state,
Scale contact,
inventory count, item-used stat and placement event before/after each attempt:
out-of-reach material point, stale or wrong face/revision, narrow or near-edge
footprint, intervening block, occupied volume, world border/height, denied
build/use, disabled item, invalid spawn type, and failed world insertion. The
  positive matrix covers each face with matching effective gravity, the two
boat/raft variants, an egg body, survival/creative consumption, and one real
`Player.interactOn` route. Separate PASS controls cover baby eggs and an
anatomy-free vanilla pair; BINDING rejects without falling through to legacy.

## IMPLEMENTER execution in progress

The current transaction prepares a vetted body without `finalizeSpawn`,
selects a body-specific material ray, checks the actual footprint and
clearance, attaches before insertion, then revalidates the live registered
body, contact, physical box, effective reach, permissions and hand stack after
`ENTITY_LOAD` and before consumption. The independent holdouts revealed
reentrancy, candidate retirement, SCALE/footprint mutation and effective reach
mutation during insertion; all four are classified as production defects.

Local pre-final runs: `../s33-ordinary-run-final.log` passed 463/463 required
GameTests after the final exceptional insertion cleanup; `../s33-client-run.log` completed
the ordinary client suite (`BUILD SUCCESSFUL`, before exceptional insertion
cleanup); `../s33-isolated-run-7.log` passed 2/2 after exceptional insertion
cleanup with the expanded six-face physical AABB, egg/raft, permission, reach,
block, occupied-volume, narrow-face and denied zombie-egg/chicken fixture.
`../s33-build-final.log` passed packaging/check and another 463/463 ordinary
GameTests on the same production source.
The full prepared suite reaches an
older S09 A9 interior-contact fixture and fails at extreme GameTest positions
with `ITERATION_LIMIT` after 256 evaluations. Repeating with S33 disabled
produced the same failure (`../s33-prepared-skip-run-2.log`), so it is tracked
separately as a high-coordinate numerical regression; no prepared-suite green
is claimed. A separate fresh GameTest world still placed its fixture at
X=-4.66 million/Z=-14.57 million and also failed A9, this time in the full
cow manifold after the chosen single piece resolved
(`../s33-prepared-fresh-run.log`). This rules out stale test-world contents as
the explanation; the S09 temporal numerical/iteration behavior needs its own
repair and adversarial gate.

## ADVERSARY model and verification

Reserved for the ADVERSARY after S32 closes and the implementation plan
converges. Holdouts and their source stay independent of IMPLEMENTER context
until a failure is revealed.


### ADVERSARY independent plan model / PLAN RED (2026-10-04)

Active role: **ADVERSARY**. Reviewed the draft against FR-069..071,
FR-053/093, NFR-004/015/017/033/034 and the canonical API/placement ownership.
Current baseline is S32 commit0ce27305. No production, IMPLEMENTER section or
existing tests modified; this is plan review, not implementation acceptance.

The independent model treats placement as a transaction with six authorities:
material selection, body-specific eligibility, effective gravity/physical shape,
player intent/permission/inventory, entity construction, and world insertion.
Before commit, observable world entities, riding relationships, physical/contact
state, inventory, stats, placement events and tracking packets must match the
baseline. After commit, exactly one intended body and one valid material anchor
exist, with consumption conditional on creative. PASS is an intentional vanilla
continuation, not a successful Scale placement or generic rejection. READY,
BINDING and anatomy-free paths have distinct routing obligations.

Attack classes: geometry outside a vanilla hitbox, holes/occluded pieces and
nearest-face ambiguity; ratio/category/state boundaries; all cardinal gravity
frames, unsupported normal and provider-controlled dimensions; oriented footprint
and edge/tolerance/degenerate cases; block/entity/border/height/permissions and
empty/disabled/malformed item inputs; construction callbacks and world insertion
failure/reentrancy; revision/pose/support replacement between selection and
commit; same-species baby behavior and unrelated vanilla interactions. Physical
pose, root and geometry must come from accepted material, with no render or
legacy fallback. Atomicity includes secondary entities and mutation of existing
entities, not merely the candidate body count.

Performance model: HOT_MOVE_QUERY selection and clearance during interaction depend
on nearby material candidates/pieces and bounded footprint vertices; preparation
of bindings/geometry remains outside interaction. No global entity scan, full
model traversal, repeated catalog compilation or retained rejected-body/contact
history is justified by item use. PREPARATION concerns spawn initialization and
shape resolution; OFFLINE_TOOLING concerns fixtures. Later adversarial evidence
will distinguish semantic invariants, native interaction integration, rollback,
visual placement/shape and bounded work; this plan record reveals no future
holdout implementation.

**PLAN RED1 — construction is not a read-only preflight.** The draft's pre-add
transaction assumes the existing PlatformPlacement.create is safe before final
ray/footprint/collision/attach validation. Minecraft26.2
EntityType.create(ServerLevel,PostSpawnProcessor,BlockPos,EntitySpawnReason,...)
invokes Mob.finalizeSpawn before returning the candidate, then applies the stack
postprocessor. Current PlatformPlacement.create uses that overload with
SPAWN_ITEM_USE. The installed vanilla Zombie.finalizeSpawn has a baby/jockey
branch that either mutates an existing Chicken (setChickenJockey/startRiding) or
creates and inserts a Chicken via ServerLevelAccessor.addFreshEntity at bytecode
287..295. There is no SPAWN_ITEM_USE exclusion on that branch. A later invalid
material hit/footprint or denied insertion can discard the unadded zombie while
leaving the chicken/world or existing chicken state changed. FR-070's rejected
placement contract is therefore not guaranteed by I2/I4 or candidate-body cleanup.
Plan must explicitly resolve spawn initialization/stack processing as a
transactional boundary, including secondary mutations and initialization-dependent
eligibility/dimensions, before it is considered converged. Bytecode evidence is
captured privately in entitytype-placement-bytecode.txt and
zombie-placement-bytecode.txt; no runtime RED is claimed for unimplemented S33.

**PLAN RED2 — canonical material outside the vanilla AABB remains unspecified.**
Current interact rejects clicked points outside target.getBoundingBox().inflate(.25)
before READY routing. Removing Platforms.definition/global-enabled guards alone
still rejects a valid accepted convex piece located outside that unrelated box.
Counterexample: canonical support with a prepared translated/rotated piece whose
reachable face lies outside the inflated entity box, valid body ratio, permissions
and footprint. The material ray and selected convex are authoritative; a vanilla
box does not certify their extent. I1 must specify the fate of this remaining
precheck in the canonical path and the trust/reach validation replacing it.
This is a plan omission with a concrete route, not a request to change legacy
routing indiscriminately.

**Unresolved convergence obligation — six-face gravity/orientation.** The draft
itself leaves the chosen-gravity source and fixture unresolved. GravityFrames
reads a sole cardinal provider; default newborn bodies use DOWN. Its GameTest
per-instance override cannot be attached to a body the real interactOn creates
until a legitimate pre-confirm seam is defined. GravityFrame also states that
Gravity Changer owns physical box orientation; merely returning a side direction
to the reader does not orient a vanilla AABB. The converged plan needs a concrete
owner/read path and a verified shape rule for newborn boat/egg bodies before
claiming FR-071 six-face success. A test that overrides the body only after its
confirmation or bypasses Player.interactOn would not prove this contract. No
second production gravity writer is authorized by placement.

Verdict: **esto no funciona, arréglalo** at the PLAN phase. RED1 and RED2 plus the
explicit gravity/shape obligation prevent converged implementation acceptance.
The narrow sprint boundary is otherwise consistent: special-body lifecycle and
legacy engine removal remain later G5 work; no G5 global closure is requested.


### ADVERSARY revised-plan review / remaining routing PLAN RED (2026-10-04)

Active role: **ADVERSARY**. The revised draft resolves the previous two explicit
mechanisms: body-local basic construction replaces the finalizeSpawn overload,
with admission/audit of stack processing; READY no longer lets a vanilla AABB
veto accepted material. The scoped development candidate-created seam makes
six-face interactOn testing feasible before confirmation, while accurately
separating actual vanilla AABB validation from optional Gravity Changer rotation.
These are plan repairs, not yet demonstrated implementation/runtime results.

One new conflict remains between I1 and I2: I1 requires PASS for a non-eligible
pair/known material miss, including same-species baby eggs, while I2 rejects
unsupported ENTITY_DATA/non-vetted factories before construction and states that
an unsupported egg/item is FAIL and never falls through to vanilla. This makes
admission rejection precede the decision that Scale owns the interaction.

Concrete counterexample: READY canonical ordinary adult cow, same-species egg
whose normal newborn body does not qualify by the configured ratio/category,
and a vanilla-valid ENTITY_DATA component. This is a non-platform pair; vanilla
has its normal baby interaction. I2's unconditional unsupported-input FAIL
cancels Player.interactOn at the Scale HEAD hook, even though I1/NFR-033 require
vanilla continuation. The same conflict exists for a non-vetted egg type on a
known material miss. Rejecting unsafe construction is correct for a Scale-owned
placement attempt, but is not authorization to suppress unrelated vanilla use.
The installed EntityType.appendCustomEntityStackConfig explicitly accepts
DataComponents.ENTITY_DATA, so its presence alone does not make vanilla input
invalid; this is an intentional narrower Scale admission boundary.

The converged plan needs an explicit ownership/routing decision table that
orders the PASS/nonparticipation decision relative to construction admission:
prove Scale-owned eligible material before applying its atomic-route FAIL, or
provide an equivalent read-only distinction that preserves vanilla controls.
If eligibility cannot be established without unsafe construction, define the
conservative no-Scale outcome separately from a proven owned placement failure.
Do not execute finalizeSpawn merely to decide that distinction. Include the
unsupported-data/type plus non-platform/miss/baby cross-cases in acceptance.
No proposed production fix or future holdout source is disclosed here.

Verdict: **esto no funciona, arréglalo** at PLAN phase for this concrete routing
ambiguity/contradiction. Previous RED1/RED2 and the newborn-gravity feasibility
obligation are accounted for by the revised text. Production remains identical
to S32 commit0ce27305; only this ADVERSARY section was added.


### ADVERSARY ordered-routing review / material-query PLAN RED (2026-10-04)

Active role: **ADVERSARY**. The new ownership table resolves the previous
unsupported-input FAIL versus vanilla PASS conflict. Unsafe stack/type admission
now leaves Scale without constructing or finalizing a body, and same-species
baby interaction has explicit priority. Earlier transaction/AABB/gravity repairs
remain present. No production changes.

One feasibility/ordering conflict remains in the new pre-construction material
query. The draft requires a read-only material query before any factory call and
states that rows1-4 create no candidate, but the current public raycast takes an
actual observer Entity. AnatomyMovement.raycast first checks active(observer),
then candidates(observer,...); indexedSupports filters using
Platforms.eligible(observer,support). That cannot select placement material for
an as-yet-uncreated prospective boat/egg body. Reusing the player silently changes
its category, width ratio and ordinary-state predicates. Counterexample: player
not eligible for this canonical support while the smaller admitted egg/boat
body is eligible; player-scoped pre-query yields a false miss and routes PASS,
so the later legal body never reaches placement. The reverse ratio ordering
can also prematurely assert a placement candidate the final body cannot use.

The plan needs an explicit body-independent material-intent probe (with bounded
current geometry and no player-as-body eligibility inference), or a precisely
ordered safe-vetted candidate phase before its body-specific material ray.
Current I3's backend footprint validation predicate alone does not provide that
selection capability. Reconcile the table's no-candidate rows with the following
paragraph permitting basic construction when actual dimensions are needed: a
post-candidate noneligible PASS is different from pre-construction admission.
Unsafe factories must remain excluded in either ordering. This is a concrete
plan gap, not a request to execute an unsafe factory or expose mutable geometry.

Verdict: **esto no funciona, arréglalo** at PLAN phase for this material-query
ownership ordering. The unsupported-data/PASS defect is resolved. Production
still equals commit0ce27305; only ADVERSARY record added.


### ADVERSARY converged-plan pass GREEN (2026-10-04)

Active role: **ADVERSARY**. A complete unchanged-production pass over the latest
I1-I6 and ordered routing table finds no remaining concrete plan contradiction.
Early vanilla-owned exits precede any candidate; unsafe types/data return PASS.
A vetted body-local candidate is now explicit before the material query, and the
same body's category, actual dimensions and ordinary state define eligibility.
Miss/noneligible cleanup returns PASS; owned validation failures return atomic
FAIL. The player is never substituted as prospective body. This resolves the
last material-query/no-candidate ordering RED without restoring unsafe spawn
finalization. The original AABB-veto and initialization-effect counterexamples
are excluded by the revised contract, and the six-face development seam occurs
before confirmation while preserving the sole gravity owner's responsibility.

The independent six-authority transaction model remains the ADVERSARY model.
Runtime obligations still require actual proof: admission audit of basic factory
and safe components, local known-miss versus unavailable client state, exact
reachable face and oriented footprint, pre-add anchor/insertion rollback,
secondary/riding/event/stat/inventory identity, six real interactOn faces,
vanilla baby and unsupported-input/nonparticipant controls, and bounded work.
These are required acceptance evidence for the future candidate, not findings
against unimplemented production. Optional external gravity compatibility remains
separate from the cardinal development fixture. No runtime, GameTest, visual or
S33/G5 completion claim is made by this plan acceptance.

Verdict: **funciona, continúa** — the current narrow S33 plan and independent
model converge. Production diff against S32 commit0ce27305 is empty throughout
this pass. ADVERSARY added only this section; IMPLEMENTER content remains intact.
Future holdouts stay private until a completed result or reproducible defect.


### ADVERSARY vanilla egg component correction / PLAN GREEN (2026-10-04)

Active role: **ADVERSARY**. The previous PLAN GREEN overlooked that vanilla26.2
SpawnEggItem.getType reads the required type from DataComponents.ENTITY_DATA.
A blanket component-presence veto would exclude ordinary eggs. IMPLEMENTER's
local bytecode finding is correct; independent javap inspection of the installed
26.2 SpawnEggItem confirms getType returns TypedEntityData.type (null without the
component). TypedEntityData exposes type and copyTagWithoutId, permitting the
required type-only/empty-payload distinction without spawn initialization.
Local evidence: ../s32-adversary-private/spawnegg-type-placement-bytecode.txt.

The revised plan admits the required vanilla type-only component and routes
nonempty custom tags, mismatched or unvetted types to vanilla PASS before
construction. This repairs the overbroad admission assumption while preserving
atomic candidate initialization, body-specific query and early baby/nonparticipant
behavior. No new concrete plan gap found in this pass. Runtime acceptance must
include an ordinary unmodified vanilla egg as positive control and separately
exercise empty payload, nonempty custom payload and resolved type identity; this
is not a claim that every custom egg is supported by Scale's atomic route.

Verdict: **funciona, continúa** for the corrected plan. ADVERSARY changed no
production; an IMPLEMENTER AnatomyMovement working-tree edit was already present
during this review and is not being certified by this plan-only result. No build,
runtime or G5 completion claim is made. IMPLEMENTER sections remain untouched.


### ADVERSARY real insertion reentrancy / PRODUCTION RED (2026-10-04)

Active role: **ADVERSARY**. Independent local server holdout
S33PrivateReentrantPlacementProof uses a prepared anatomy session, current accepted
registered convex material, real Player.interactOn and the real Fabric
ServerEntityEvents.ENTITY_LOAD callback fired during addFreshEntity. It does not
replace placement, invent an anchor, mutate production, change held inventory or
bypass the native interaction. A scoped test callback requests one nested use of
the same player/hand/item on a second nonoverlapping point of the same broad face.
The survival player starts with exactly one OAK_BOAT item; both points are in reach
and independently clear. The callback disarms before the nested insertion.

Actual result in ../s33-adversary-private/reentrant-insertion.log:4134:
outer SUCCESS, nested SUCCESS, inserted=2, remaining=0, anchors=2. Both intended
placements reached actual native insertion and Scale anchors; then the oracle
fails at lines4155/4162 on NFR-015/FR-070 one held item cannot create a second body
during the first commit. The run compiled normally, sentinel passed, required
holdout failed:1/2 tests, BUILD FAILED2m24s. This is production RED with positive
material/outer-commit controls, not a fixture startup/compiler/precondition failure.

Classification: reentrant commit/inventory amplification, NFR-015 and FR-070/071.
The production transaction has no active-interaction ownership guard or equivalent
inventory reservation. Its addFreshEntity callbacks run before stack.consume,
allowing the same original stack to commit another body and anchor. The fixture
uses the actual authorized server-world callback rather than the development
candidate-created seam. It deliberately exercises reentrancy; it is not claimed
that vanilla without any callback spontaneously repeats use.

Candidate source hashes at execution: PlatformPlacement.java
e6485dcaba61325fc8c2263944c7b290958958c6fa0ee2c84b35532cb1ec3e0d;
AnatomyMovement.java
4b249a4a2064bf83b6cd2eee1ae70403e8388f9066369469ca3e7d1cc5929188.
Source/fixture/init/log manifest: reentrant-red-evidence.json in the private
adversarial directory. Own build/cache/run: ../s33-adversary-build,
../s33-adversary-cache, ../s33-adversary-run. An initial compile-only launch was
interrupted before the game to configure the owned run directory; no assertion
result is attributed to it.

Minimal command after configuring JAVA_HOME to the installed Java25 runtime:
`./gradlew.bat runGameTest -PscalebrewsBuildDir=../s33-adversary-build
--project-cache-dir ../s33-adversary-cache
-I ../s33-adversary-private/server.init.gradle --console=plain`.
The init uses the existing isolated original-cow export under
../s29-adversary-run/anatomy-export. Holdout and init are test-only private files.

Verdict: **esto no funciona, arréglalo**. Return this concrete production RED to
IMPLEMENTER. S33 acceptance remains open; remaining adversarial holdouts are not
revealed by this handoff. No production or IMPLEMENTER test changes by ADVERSARY.


### ADVERSARY transaction fence repair repetition GREEN (2026-10-04)

Active role: **ADVERSARY**. Repeated the exact ENTITY_LOAD holdout without any
oracle/input/assertion change: source SHA256 remains
5ed9f14ee6d1f0a800ac54f8fd854dc312f726accd2e04962556936da01fb787.
Current PlatformPlacement identity-player fence encloses the entire anatomical
attempt and removes its active ownership in finally. Actual native insertion
now yields outer SUCCESS, nested FAIL, inserted=1, remaining=0, anchors=1.
../s33-adversary-private/reentrant-insertion-fixed-green-retest.log passes all2
required tests including sentinel, BUILD SUCCESSFUL1m24s and clean shutdown.
The first repair repetition stopped in compileGametestJava because an
IMPLEMENTER AnatomyGeometryTests diagnostic lacked its Platforms import; this
infrastructure error was returned and repaired by its owner. It is not claimed
as a fence/runtime regression. Its failed log remains preserved separately.

Additional independent S33PrivateFenceLifecycleProof passes known material-miss
PASS, permission-denied FAIL and an exceptional candidate callback followed by
real successful placement on the same player. Inventory/entities stay unchanged
in those failure paths; that success consumes one of two items and creates one
Scale anchor. After retirement, a second independent successful use consumes the
remaining item and creates its own valid anchor. This demonstrates transaction
ownership release after normal PASS/FAIL, exception and success through the
actual interactOn route, without inspecting or rewriting the fence container.
../s33-adversary-private/fence-lifecycle.log: all2 required tests pass,
BUILD SUCCESSFUL1m16s and clean shutdown. The exception is injected only through
the scoped development candidate seam; the reentrant insertion oracle remains
the real Fabric callback route. No production edit or test rewrite by ADVERSARY.

Candidate PlatformPlacement.java SHA256
de6c05dc42def39a7ede360a6f85c1b62981e7b8d3459ceeb5e1fc8cb79d745b.
Manifest of candidate sources/oracles/logs: fence-green-evidence.json under
../s33-adversary-private. Original RED init is archived byte-for-byte as
server-red-original.init.gradle (a1af14bb1ecc6381dcaa9626b1b98808d35fd13640c54804707f942c8766ff2d).
The current private init only adds a selectable proof property for the additional
lifecycle lane; the original reentrancy selector remains its default. Reproduce
with the earlier command, adding
-Ps33PrivateProof=io.github.r3neer.scalebrews.test.S33PrivateFenceLifecycleProof
for that additional lane. Build/run/cache stay owned and isolated.

Verdict: **funciona, continúa** for the reported reentrant insertion defect and
its tested fence-release regressions. No remaining concrete bypass was found in
this repair review. This is not full S33/G5 acceptance: the independent remaining
placement matrix, client/native interaction and applicable visual/work evidence
still need their own results. IMPLEMENTER sections and tests remain untouched.


### ADVERSARY insertion-retirement transaction / PRODUCTION RED (2026-10-04)

Active role: **ADVERSARY**. The expanded IMPLEMENTER six-face/egg/raft fixture is
not substituted for independent atomicity. New local server holdout
S33PrivateInsertionRetirementProof first proves a genuine successful interactOn
placement, one native inserted body, one Scale anchor and one survival consumption.
It retires that control body, restores one held OAK_BOAT and then injects discard
of the next candidate from the real Fabric ServerEntityEvents.ENTITY_LOAD callback
inside native world insertion. No inventory write occurs during that callback.
No placement primitive, contact, insertion result or production code is replaced.
This directly exercises I4's requirement to handle candidate invalidation during
insertion, rather than relying only on addFreshEntity's Boolean return value.

Actual second attempt: SUCCESS, removed=true, remaining=0, contact=Optional.empty.
../s33-adversary-private/insertion-retirement.log:189 records that state; the
semantic FR-070/071 assertion fails at210/217 after its positive control.
Sentinel passes;1/2 required tests fail, BUILD FAILED1m14s and server stops.
Classification: production transaction commits consumption/success after an
insertion callback has already invalidated the candidate's lifecycle. The
successful-placement invariant has zero surviving body and zero anchor.
The injection is a legitimate server-thread entity-load removal, not a claim
that an unmodified vanilla listener spontaneously deletes every placement.
The scope is invalidation before interactOn returns; later ordinary gameplay
removal is not classified as failed placement by this oracle.

Candidate PlatformPlacement.java remains the fence-repaired SHA256
de6c05dc42def39a7ede360a6f85c1b62981e7b8d3459ceeb5e1fc8cb79d745b.
Independent proof/init/log/source hashes are in
../s33-adversary-private/insertion-retirement-red-evidence.json.
Reproduce with the established Java25 environment and isolated directories:
`./gradlew.bat runGameTest
-Ps33PrivateProof=io.github.r3neer.scalebrews.test.S33PrivateInsertionRetirementProof
-PscalebrewsBuildDir=../s33-adversary-build
--project-cache-dir ../s33-adversary-cache
-I ../s33-adversary-private/server.init.gradle --console=plain`.
The init selects this one holdout plus the existing sentinel and supplies the
isolated original-model catalog. Remaining holdouts/matrix stay private; no
client/visual/full-S33 acceptance is claimed by this failed frontier.

Verdict: **esto no funciona, arréglalo**, concrete FR-070/071/I4 production RED.
Return to IMPLEMENTER; ADVERSARY has modified only its private fixture/evidence
and this section, never production or IMPLEMENTER tests/sections.


### ADVERSARY insertion retirement repair + fence regression GREEN (2026-10-04)

Active role: **ADVERSARY**. IMPLEMENTER checks live registration/identity and
supported Scale anchor after synchronous native insertion, before consumption,
stat and placement event. Repeated all three existing independent oracles without
changing their source, inputs or assertions. Their fingerprints match prior RED/
GREEN evidence; PlatformPlacement SHA256 remains
50460fdc101b1a10075046d3db55651f33ffe0966ec7f29aeae3c3d7942f7b65 and
AnatomyMovement remains4b249a4a2064bf83b6cd2eee1ae70403e8388f9066369469ca3e7d1cc5929188
throughout this batch. No production or IMPLEMENTER test edit by ADVERSARY.

| Own log beneath ../s33-adversary-private | Required tests / elapsed |
| --- | --- |
| insertion-retirement-fixed.log |2/2, BUILD SUCCESSFUL1m29s |
| post-insertion-fence-reentrant.log |2/2, BUILD SUCCESSFUL1m14s |
| post-insertion-fence-lifecycle.log |2/2, BUILD SUCCESSFUL1m59s |

All server runs close cleanly. The insertion-retired candidate now returns FAIL,
removed=true, remaining=1 and contact=Optional.empty after its unchanged real
positive control. Reentrant ENTITY_LOAD still gives outer SUCCESS, nested FAIL,
inserted=1 and anchors=1 with one survival consumption. Known miss PASS,
permission FAIL and injected exception still preserve world/item baseline and
allow subsequent real successful placements by that same player; independent
success also releases the fence for the next use. Manifest:
../s33-adversary-private/post-insertion-green-evidence.json. The established own
selector command reproduces each proof with its class property and isolated
build/run/cache; no new selection or oracle semantics were added in this batch.

Verdict: **funciona, continúa** for the insertion-retirement RED and requested
fence regressions. Their unchanged repetitions reveal no remaining concrete
failure. Full S33 matrix/client/visual/work acceptance remains open; this result
is not promoted to complete G5 or compatibility with arbitrary external callbacks.

### ADVERSARY effective insertion SCALE footprint RED (2026-10-04)

Active role: **ADVERSARY**. Independent actual prepared-session, Player.interactOn
and Fabric ENTITY_LOAD oracle `S33PrivateInsertionScaleProof` establishes a real
positive control with a vanilla type-only PIG_SPAWN_EGG. Support SCALE4; accepted
convex face width1/depth2; initial pig width.9 fits. On the second insertion,
synchronous ENTITY_LOAD initializes pig SCALE1.5 and refreshes dimensions.
The effective width1.3499999 remains eligible (ratio.5625 <=.6) and its retained
anchor remains supported, but `placementFits` rejects the complete footprint.
The callback records that precondition before production can retire the body.
No inventory mutation, nested placement or production seam is used for this fault.

Production nevertheless returns SUCCESS and consumes the survival egg (remaining0).
Classification: **production FR-070/FR-071 transaction-footprint defect**, not
fixture compilation or compatibility speculation. The post-insertion liveness
repair is insufficient when effective SCALE changes while the same body and
anchor remain valid. Full footprint is checked only before synchronous insertion.
The positive control passes; the unchanged semantic fault repeats twice. Final
oracle permits body retirement by a repair and demands FAIL with item retained.
`insertion-scale-footprint-final.log` is RED, BUILD FAILED1m29s,1/2 required tests;
sentinel passes and server closes cleanly. Initial run also RED1m37s.

Own manifest: `../s33-adversary-private/insertion-scale-red-evidence.json` pins
production, final oracle/init and log SHA256. PlatformPlacement remains
50460fdc101b1a10075046d3db55651f33ffe0966ec7f29aeae3c3d7942f7b65.
No production or IMPLEMENTER test/section edit. Repeat the established isolated
runGameTest command with `-Ps33PrivateProof=io.github.r3neer.scalebrews.test.S33PrivateInsertionScaleProof`.
Verdict: **esto no funciona, arréglalo**. Remaining matrix/client/cost criteria
stay open; this concrete RED prevents S33 closure.

### ADVERSARY insertion SCALE repair GREEN; current reach RED (2026-10-04)

Active role: **ADVERSARY**. Repeated `S33PrivateInsertionScaleProof` unchanged:
SHA256 f2a5f9b7149d4b02b2d63b589b2cdf8b8244d643468b761069ef04e1cd422e96.
`../s33-adversary-private/insertion-scale-fixed.log` GREEN2/2,1m43s, clean
shutdown: FAIL, remaining1, rejected body/contact retired. The prior SCALE RED
is repaired. Production PlatformPlacement SHA256 throughout the new batch:
adedc2df4aee21e043bbe40ff26d2a9d0ba8118689088461e4610375fee929e2;
AnatomyMovement remains4b249a4a2064bf83b6cd2eee1ae70403e8388f9066369469ca3e7d1cc5929188.

New independent `S33PrivateInsertionReachProof` uses the same prepared session,
actual player interaction and vanilla OAK_BOAT positive control. The second
actual synchronous ENTITY_LOAD callback only changes the player's vanilla
ENTITY_INTERACTION_RANGE attribute to.1. It does not move player/body or mutate
inventory. The chosen material point remains beyond that new effective range.
The candidate commits SUCCESS with currentReach=.1, removed=false, remaining0
and an accepted support anchor. Classification: **production FR-070/071 stale
reach at transaction commit**. The final distance check reuses `reach` captured
before insertion instead of the current authoritative range. This defeats the
new stated revalidation even though all physical body checks remain valid.

`../s33-adversary-private/insertion-reach.log:218` records the concrete result;
assertion RED, BUILD FAILED2m33s,1/2, sentinel GREEN, clean server shutdown.
Oracle expects FAIL, retained item and no rejected candidate anchor; the actual
positive control succeeds first. Manifest `insertion-reach-red-evidence.json`
pins source/oracles/init/logs. Reproduce established isolated runGameTest command
with `-Ps33PrivateProof=io.github.r3neer.scalebrews.test.S33PrivateInsertionReachProof`.
No production or IMPLEMENTER test/section changes. Verdict: **esto no funciona,
arréglalo**; remaining matrix/client/cost remains open.

### ADVERSARY current reach repair GREEN; insertion exception RED (2026-10-04)

Active role: **ADVERSARY**. Existing S33PrivateInsertionReachProof repeated
unchanged: GREEN2/2,1m12s, clean shutdown; FAIL/currentReach.1/removedtrue/item1/
contactempty in `../s33-adversary-private/insertion-reach-fixed.log:302`. This
confirms the reach repair; later commit checks added by IMPLEMENTER are not
claimed covered by a run compiled before those additions.

New S33PrivateInsertionExceptionProof compiled the latest source with those
checks: PlatformPlacement SHA256
40a0364960344e5b5ddfd03ca72319284a5250e83c5a90cdf43d6f0d78769db2
unchanged through the run. Independent actual prepared-session and vanilla
OAK_BOAT insertion positive control succeeds. On the second synchronous
ENTITY_LOAD, a one-shot scoped callback throws IllegalStateException with a
unique marker. The oracle catches that exact marker outside Player.interactOn;
it neither discards nor mutates inventory in the callback. Production propagates
the exception without cleaning its already-inserted candidate: removed=false,
registered=true, remaining1, accepted support anchor. Classification:
**production FR-070/071 transactional cleanup defect**, not failed fixture or
ordinary later despawn. The world has a live anchored body despite no committed
consumption; the owned insertion path lacks exceptional rollback.

Evidence `../s33-adversary-private/insertion-exception.log:1640`, assertionRED,
BUILD FAILED2m17s,1/2, sentinelGREEN and clean shutdown. The oracle finally
cleans its captured body after observing the fault; that fixture cleanup does
not hide the failed production invariant. Manifest:
`../s33-adversary-private/insertion-exception-red-evidence.json`. Repeat isolated
runGameTest with `-Ps33PrivateProof=io.github.r3neer.scalebrews.test.S33PrivateInsertionExceptionProof`
and the established own init/build/cache paths. No production, IMPLEMENTER
sections or tests changed. Verdict: **esto no funciona, arréglalo**. Remaining
matrix/client/visual/cost stays open.

### ADVERSARY insertion repair and expanded matrix GREEN (2026-10-04)

Active role: **ADVERSARY**. Candidate PlatformPlacement SHA256
 a9d0fba5b6cd26d8499553259eb33b50f70bf001b74c37fbe64dc76a9049cff0
and AnatomyMovement4b249a4a2064bf83b6cd2eee1ae70403e8388f9066369469ca3e7d1cc5929188
remain stable throughout this completed batch. No production or IMPLEMENTER test edits.

| Own log under ../s33-adversary-private | Result |
| --- | --- |
| insertion-exception-fixed.log |2/2 GREEN1m20s; exact exception marker preserved, removedtrue/registeredfalse/item1/contactempty |
| final-S33PrivateInsertionRetirementProof.log |2/2 GREEN1m12s |
| final-S33PrivateReentrantPlacementProof.log |2/2 GREEN1m08s |
| final-S33PrivateFenceLifecycleProof.log |2/2 GREEN2m30s |
| final-S33PrivateInsertionScaleProof.log |2/2 GREEN2m13s |
| final-S33PrivateInsertionReachProof.log |2/2 GREEN1m51s |
| placement-matrix.log |2/2 GREEN1m25s; edge, reach, block, live entity, border reject with item/world baseline; before/after positives succeed |
| shear-egg.log |2/2 GREEN1m19s; real sheared face and full footprint; baby/custom tag early PASS with zero candidate creations; actual ineligible ratio PASS |
| six-face-snapshot.log |2/2 GREEN1m42s; all six actual AABBs, gravity/contact/footprint/consume verified |
| gap-nearest-creative-retest.log |2/2 GREEN1m23s; material gap PASS, nearest z_near wins over lexical a_far, actual creative chest raft one anchor/no consume |
| placement-client-retest.log |Real client GREEN1m43s; known miss PASS, hit provisional, baby PASS, unknown snapshot provisional, no client consume; clean shutdown |
| placement-work.log |2/2 GREEN1m20s;512 rejected actual interactions,512 candidates,512 provider calls, zero registered rejected probes/anchors; final positive succeeds |

The five existing regressions above were repeated without oracle changes after
all repairs. The body footprint and reach faults remain repaired. Exception
cleanup now removes the owned body/anchor and propagates the original marker.
Two own infrastructure failures were classified and fixed without production:
S32 selector auto-added a missing private receiver mixin; own minimal client
selector avoids it. Mock CREATIVE did not apply abilities (confirmed installed
26.2 GameTestHelper$1 bytecode); explicit GameType.updatePlayerAbilities restores
the intended positive precondition before gap/nearest/creative tests. Failed
logs remain retained and are not credited as production failures.

Visual: six-face-snapshot.json contains actual captured material/body vertices,
contact point/normal and gravity. render_six_faces.py generates fixed isometric
six-face-contact-sheet.png; inspected all six views and corrected header spacing.
This spatial comparison supplements machine contact and footprint assertions.
No external Gravity Changer rotated AABB compatibility claim: the fixture uses
its effective test-read frame with actual vanilla AABB. Metadata and artifact
fingerprints are in final-functional-green-evidence.json.

Cost classification: interaction preparation/validation is user-action work;
raycast delegates to existing bounded spatial discovery. placementFits validates
four gravity-facing vertices, no catalog recompilation/reflection/world scan in
that helper. Commit adds another local footprint/collider/entity/ray check and
one stack copy, not a per-tick placement loop. Actual512 rejects allocated
8,830,024 thread bytes in383,721,100ns, one provider call per attempt. This is a
structural/cold-action sample, not normative FPS/TPS or heap-soak acceptance.
Existing canonical policy resolution remains authoritative; ratio checks exercise
that call path. Parent ordinary463 server tests and its implementation fixture
are separate evidence, not claimed as own executed results here.

This completed batch contains no new production defect. A real native packet
codec/dispatch holdout is still being classified; global S33 closure is not
claimed until that independent frontier resolves. G5 later lifecycle/retirement
and G9 normative benchmarks remain outside this scoped S33 batch.

### ADVERSARY native interaction codec/dispatch RED (2026-10-04)

Active role: **ADVERSARY**. S33PrivatePlacementNetworkProof runs real client and
integrated server, accepted cow geometry/profile, ordinary type-only PIG egg,
actual server player in survival. Player is teleported outside the prospective
body footprint, frozen and synchronized before sending. It selects the accepted
root/body/cube_0 material top face. The exact authoritative Player.interactOn
control succeeds with one supported pig and one consumption, then fixture
retires that control pig and restores one normal egg. It sends the SAME exact
relative material location in the actual native ServerboundInteractPacket via
client connection. No receive-handler invocation or synthetic endpoint is used.

Native 26.2 STREAM_CODEC round-trip changes relative selection
(0.07894736842105265,5.504000186920166,0) to
(0.07910639077092174,5.504120124519318,0). Y rounds above the selected face by
~0.00011994. Packet dispatch leaves zero pigs and one retained egg; the exact
nonserialized point still raycasts the original accepted face after20 ticks.
The body/face/actor inputs are stable, READY/mayBuild true and the actual positive
control proves footprint, clearance, reach, permissions and insertion together.
Classification: **production FR-069/FR-070 native interaction quantization
failure**. The finite ray ends at the rounded wire location above material;
normal client face intent can be lost instead of reaching anatomical placement.

Evidence ../s33-adversary-private/placement-native-packet-isolated.log:
229 exact positive SUCCESS/pigs1/consumed;230 real codec values;232 authoritative
postpacket pigs0/item1 plus exact retryHit at original face. Required acceptance
predicate times out, BUILD FAILED1m37s, clean world shutdown. Source hashes
remain PlatformPlacement a9d0fba5b6cd26d8499553259eb33b50f70bf001b74c37fbe64dc76a9049cff0
and AnatomyMovement4b249a4a2064bf83b6cd2eee1ae70403e8388f9066369469ca3e7d1cc5929188.
Own manifest native-packet-red-evidence.json pins source, selector and logs.
Repro: runClientGameTest with
`-Ps33PrivateClient=io.github.r3neer.scalebrews.test.S33PrivatePlacementNetworkProof`,
`-PscalebrewsBuildDir=../s33-adversary-client-build`,
`--project-cache-dir ../s33-adversary-client-cache`,
`-I ../s33-adversary-private/client.init.gradle`. Existing JDK25 setup applies.

Initial native fixture had not proven placer clearance; it was classified as
UNPROVEN and strengthened with displaced actor plus exact real positive control
before this RED. It was never claimed as a production defect on timeout alone.
All completed matrix/regression/visual/work GREEN evidence in the preceding
section remains scoped to its exercised routes; it cannot substitute for native
codec/dispatch. No production or IMPLEMENTER tests/sections changed. Verdict:
**esto no funciona, arréglalo**; S33 global closure remains blocked by this real
client interaction frontier.

### IMPLEMENTER codec repair pending independent rerun (2026-10-04)

The 26.2 `Vec3.LP_STREAM_CODEC` delegates to `LpVec3`. Bytecode confirms it
rounds each target-relative component onto a 32766-step grid scaled by the
largest absolute component's ceiling. The decoded packet point can move just
outside the clicked face. `PlatformPlacement` now keeps the exact material ray
first; only a miss causes a short continuation. The continuation is bounded by
the codec's per-axis quantization error (including a one-scale-step upper bound
when the original value straddles an integer), and a recovered hit must lie
inside that error box around the decoded endpoint. Existing exact hits keep
their original result; a materially farther miss cannot become placement.

The prepared fixture also tests a +0.00012 outward miss as recoverable and a
+0.001 miss as outside the codec budget. Compilation passed. The real native
packet proof and full matrix are being rerun independently; this paragraph is
not a GREEN claim or S33 closure.

The first repair passed the native ordinary-angle packet but failed an
independent near-parallel native packet: a 0.00012 normal-coordinate rounding
would require extending the eye ray much farther tangentially than the codec
cell. That failed holdout is a production RED, not a fixture waiver. The
second repair searches the bounded packet uncertainty cell along its four
opposite-corner lines and considers only faces that the eye ray can see.
Conflicting visible face identities fail closed; exact ray hits remain first.
An IMPLEMENTER grazing regression now exercises the same geometry class.
Independent rerun and final review are pending.

### ADVERSARY codec extension repair: original GREEN, grazing RED (2026-10-04)

Active role: **ADVERSARY**. PlatformPlacement SHA256
30289bc6dfd4eea6e10fac103015fe635b15d74a6b7959ba7afc623c10320472
stable throughout; AnatomyMovement unchanged4b249a4a2064bf83b6cd2eee1ae70403e8388f9066369469ca3e7d1cc5929188.
Repeated original S33PrivatePlacementNetworkProof without source/input changes:
`native-codec-fixed.log` GREEN2m19s, actual packet body1/anchor1/consume1, clean
shutdown. Existing S33PrivatePlacementMatrixProof unchanged:
`native-codec-rejection-matrix.log` GREEN2/2,2m58s; edge/reach/block/entity/border
reject and before/after positives succeed. The original native RED is repaired.

New independent S33PrivateGrazingPacketProof selects the same actual accepted
cow body top and normal egg after60 ticks (beyond20-tick Scale transition).
Actor eye is about.01 above the face and2.05 horizontally away, outside the
candidate footprint. Prerequisite log confirms top face5, placementFits=true,
distance2.0500243908438596<3. Actual direct authority control succeeds and creates
one supported pig/consumes once. The same native packet location then round-trips
relativeY5.504000186920166 to5.504120124519318. It still leaves pigs0/item1.
After20 ticks the exact original point still raycasts the same material face;
actor eyeY remains15.514000186920166 and faceY15.504000186920166, horizontal
distance2.9502 remains within reach3 (actor horizontal position drifted farther,
which preserves clearance/reach and is explicitly retained in the evidence).

Classification: **production FR-069/070 near-grazing native quantization failure**.
The new fallback prolongs the quantized ray by sqrt(3)*axisError; at this shallow
angle that is insufficient to reach the unchanged selected plane even though
the actual point perturbation is within the codec bound. Exact interaction is
valid; transport round-trip alone still loses the intended face.
`native-codec-grazing-stable.log:231-234` supplies prerequisite, exact success,
codec values and authoritative nonplacement. Required acceptance predicate times
out, BUILD FAILED1m28s and clean world shutdown. Manifest:
`../s33-adversary-private/native-grazing-red-evidence.json`.
Repro same own client/init/build/cache command with
`-Ps33PrivateClient=io.github.r3neer.scalebrews.test.S33PrivateGrazingPacketProof`.

Earlier angular fixture runs failed their own prerequisite because initial Scale
transition advanced the face above the near-plane eye and selected an incompatible
side. They remain classified UNPROVEN fixture, never production RED; waiting60
ticks yielded the stable validated control above. No production or IMPLEMENTER
test/section changes. Verdict: **no funciona, arréglalo**. Previous original
packet/rejection GREEN results are retained, not promoted to global S33 closure.

### ADVERSARY precision-cell repair GREEN (2026-10-04)

Active role: **ADVERSARY**. IMPLEMENTER changed only the precision fallback:
search actual eligible face inside the LpVec3 uncertainty cell, require visibility
and reject differing face identities. Candidate PlatformPlacement SHA256
821c4ca1068d6ecb3923c30ed7afa95f8503eba422c8ee2c0fb3de12b44de953
and AnatomyMovement4b249a4a2064bf83b6cd2eee1ae70403e8388f9066369469ca3e7d1cc5929188
remain stable. Repeated both native oracles and associated rejection matrix
without any source/input/assertion changes from their prior evidence.

| Own log under ../s33-adversary-private | Result |
| --- | --- |
| cell-S33PrivatePlacementNetworkProof.log |Real client/server GREEN3m25s; exact positive and codec-roundtrip packet body1/anchor1/consume1 |
| cell-S33PrivateGrazingPacketProof.log |Real client/server GREEN1m36s; stable near-parallel positive and same native codec perturbation now body1/anchor1/consume1 |
| cell-rejections.log |2/2 GREEN3m45s; edge/reach/block/entity/border retain rejection/world/item baseline and before/after real positives |

All three processes close cleanly. The same codec round-trip coordinates are
retained, not replaced with a synthetic inside-material endpoint. The previous
near-grazing RED is repaired and the requested associated rejects remain intact.
Manifest `../s33-adversary-private/native-cell-green-evidence.json` pins candidate,
unchanged oracles/selectors and logs. Established client selector reproduces
either proof; established server selector reproduces PlacementMatrixProof.
No production or IMPLEMENTER tests/sections changed by ADVERSARY.

Cost review of this delta: one exact ray, then at most8 cell diagonals and up to
2 visibility rays per candidate, maximum25 core raycasts per fallback interaction.
Constant loop bounds reuse existing bounded spatial/material query and do not
introduce catalog recompilation or a new tick-loop placement task. This increases
MISS/quantized-hit action work versus the old ray extension; that explicit bounded
cost is explained and not credited as normative FPS/TPS or heap-soak proof.
Placement full footprint/reach/permission/clearance/commit guards still apply
after cell selection. No external Gravity Changer rotated-shape claim is added.

Verdict for both codec defects and requested rejection regressions: **funciona**.
This completed review pass proposes zero production changes. Global G5 later
lifecycle/legacy retirement and G9 normative performance acceptance remain their
separate gates; this scoped evidence does not advance those gates.

### ADVERSARY final zero-change review (2026-10-04)

Active role: ADVERSARY. Reviewed final S33 production, canonical scope, test
changes, acceptance evidence and workflow closure rules without changing any
production or IMPLEMENTER test/section. PlatformPlacement remains SHA256
821c4ca1068d6ecb3923c30ed7afa95f8503eba422c8ee2c0fb3de12b44de953;
AnatomyMovement remains
4b249a4a2064bf83b6cd2eee1ae70403e8388f9066369469ca3e7d1cc5929188.
All ten file fingerprints in native-cell-green-evidence.json match current
files. Both native packet proofs and associated rejection matrix are GREEN on
this candidate, unchanged. Earlier insertion/fence/scale/reach/exception,
six-face, shear, nearest/gap, creative/egg/client and bounded-work evidence is
retained with its execution-version limits; no older lane is silently labelled
as rerun after the precision-cell change. No new placement production defect
was found. The occupied-player-head fixture correction preserves its head
contact/height/carry assertions, admits the boat under the actual .6 ratio
policy, and deliberately excludes the cosmetic hat shell.

Final integration closure still has a concrete RED: required prepared proof
AnatomyPreparedIntegrationProof, older S09 A9, exhausts 256 evaluations at valid
large world coordinates. The skipped-S33 log has a required test failure at
lines5388-5389 and BUILD FAILED at5417; fresh-world failure is at215/225-226
and BUILD FAILED254. Verified SHA256 respectively
 aa8ea35b33546ebb4c7be88e52d3f78065ba5e79a3a2532633a844a32ae73e79
and
 f329da71f5ba457b1d04b9efa953d7c5f57ef729e8c507225655173fa8f058b4.
Classification: existing G4 numerical/work acceptance failure and closure
blocker, not a new S33 placement defect. Omitting S33's fixture does not prove
all S33 source changes irrelevant, but the failing assertion and unchanged
solver surface establish the separate affected frontier already recorded in
S34. No prepared-suite GREEN has been demonstrated by isolating S33.

Reproduction lane: runGameTest with scalebrewsAnatomyPreparedSuite=true,
scalebrewsAnatomyCatalog=../s29-adversary-run/anatomy-export and
scalebrewsS33Skip=true, using a separate build/cache/run directory and the
existing prepared server selector. Preserve the failing large-coordinate world
from the two logs for exact replay; a fresh randomized world need not choose
the same translation. The precise positions and result prefixes are in those
logs and the S34 draft. No production change proposed outside IMPLEMENTER.

Verdict for placement function: GREEN, zero new production fixes. Verdict for
unconditional S33 integration closure: **no funciona, arréglalo** until the
required prepared regression is green or the human explicitly authorizes a
scope/closure exception. Workflow section11 says "no se declara cerrado con
tests rojos o revisión pendiente"; section12 requires relevant regressions and
canonical consistency. A future draft cannot convert executed required RED
into closure evidence. This does not authorize ADVERSARY to implement S34,
raise budgets, suppress A9 or change any oracle. G5-wide/G9 and external rotated
Gravity Changer shape compatibility remain unclaimed.

### ADVERSARY final S33 cross-sprint closure review GREEN (2026-10-04)

The blocking required S09 A9 RED has been repaired and independently retested
through S34, including its late nonidentity-root certificate RED. The complete
S34 ADVERSARY final cross-sprint record verifies current production/oracle
hashes, own GREEN6/6 plus expanded48-case campaign, final ordinary465/465,
prepared2/2 and clean real client regression. S33 native packet, transaction,
six-face, policy/spatial and snapshot/work evidence remains scoped accurately;
its source, oracle, selector and log fingerprints have not changed. No new
placement production defect or affected regression/evidence gap found in the
final review. No production or IMPLEMENTER test/section changes made.

Verdict: **funciona**; S33 testing and zero-change review converge. IMPLEMENTER
still owns final canonical status and its separate logical commit. No later
G5 lifecycle/legacy migration, normative G9 or external rotated-box acceptance
is claimed by this closure review. The historical earlier S33 closure RED was
valid when A9 was unresolved; it is superseded by this repaired-candidate GREEN,
not silently erased.
