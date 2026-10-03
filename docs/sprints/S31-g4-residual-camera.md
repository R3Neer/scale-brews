# S31 / G4.6b — causal body residual and camera

Active role: **IMPLEMENTER** for production and **ADVERSARY** for independent
verification. S31 local body/camera scope is closed after the final combined
adversarial pass and ordinary server/client suites. G4.6 remains open until the
remote observer route also converges.

## Source and boundary

FR-086..088 require a drawable body/camera residual from the same accepted
contact and support frame, once, without writing position, AABB, velocity or
gravity. `AnatomyClientNetworking.presentationContact` supplies the confirmed
surface and current causal geometry. `ConvexBox.point(localPoint)` maps the
contact's material-local point into world space, including joint rotation,
scale and shear. `AnatomyMovement.ClientTransportCursor` names the support frame
serial and local passive transport sequence; the matching `SupportTransport`
contains the **applied passive delta**, independently of voluntary body motion.
The S2C transport receipt names a support serial for the controlled actor but
does not contain a remote observer's vanilla-position incorporation marker.

The two S30 planning REDs are fixed inputs. A stationary support plus voluntary
body walk of0.2 must yield residual0, even if the contact localPoint update is
late. A one-block teleport must clear `PlatformCamera`'s retained smoothing,
not just reset a new residual owner; the existing >4-block distance guard does
not cover it. `PlatformClientTeleportMixin` handles the player packet path,
while support/body lifecycle, gravity, binding and receiver barriers must all
propagate a hard generation change to the camera coordinator.

## Candidate relation for locally controlled bodies

- [x] R1 Introduce one level-owned `client.collision.presentation` relation per
  locally authoritative body/controlled vehicle. Its key includes body and
  support UUID+network ID, dimension, catalog epoch/revision, support binding
  generation, tracking generation, gravity and piece/face. A contact sequence
  heartbeat with the same material localPoint does not create a new relation.
  Accepted packet/lifecycle/transport events advance the relation. An observed
  gravity, owner or interpolation hard barrier may retire it immediately; render
  and camera offset calculations never advance accepted material and read an
  immutable published draw sample.
- [x] R2 On acquisition, bind the confirmed contact identity to the exact
  accepted endpoint and start residual at zero. For a locally predicted body,
  never pair displacement evaluated at the last server-confirmed `localPoint`
  with a carry performed at a newer physical anchor. Extend the existing local
  transport cursor with the immutable physical surface point and piece/face
  used by that carry, captured inside `AnatomyMovement.carry` before its anchor
  is replaced. Stamp the physical anchor with its causal **before** frame
  serial at confirmation/afterMove and include it beside the carried **after**
  serial in the cursor. Multiple accepted endpoints may arrive before one
  body tick: retain the earliest exact before geometry and coalesce only a
  serial-continuous accepted chain to the latest endpoint. Match body,
  support, contact identity, both frame serials,
  transport sequence and the `SupportTransport.appliedDelta`. Evaluate both
  causal endpoint geometries at **that same carried point** and publish both
  the support-induced displacement and the applied passive delta in one
  immutable draw sample. Their difference is only the **physical** residual;
  it is not yet the render offset. If the endpoint arrives
  before the carry cursor, hold it pending without adding displacement at a
  stale confirmed point. A later confirmed contact point rebases the relation
  without changing an already computed carried-point sample; contact identity
  change is a barrier. Body position/interpolation is never subtracted, so
  voluntary walking on a stationary support cannot create a negative residual.
  Missing or mismatched point/frame/sequence provenance clears the target to
  zero and requires a fresh relation. Duplicate cursors and frame/contact
  heartbeats are idempotent. A certified render interval may change the target
  only when its exact material serial and physical-point domain are proven; it
  cannot be inferred from packet adjacency or an uncertified animation sample.
- [x] R3 Derive the draw offset at the caller's exact partial fraction from the
  immutable R2 sample. Minecraft 26.2's ordinary entity state interpolates
  `oldPosition` to current position; local `carry` changes only current. While
  the body's `oldPosition` and tick generation still match the pre-carry
  baseline, the vanilla draw has incorporated `partial * appliedDelta` from
  that carry. For S30's `CURRENT_ENDPOINT` support draw, the body offset is
  `supportInducedDelta - partial * appliedDelta`; for an exact certified
  support interval, substitute its material displacement at that fraction.
  On the next body tick, `oldPosition` incorporates the carry and the prior
  draw correction retires. If the old-position/tick marker changes in an
  unexpected way, fail closed and reset; do not guess incorporation from
  body displacement. Multiple exact carries within one interpolation baseline
  accumulate in sequence. Renderer and camera queries compute this pure
  per-fraction offset without advancing the relation, and apply it once in
  `EntityRenderer.getRenderOffset` / the camera coordinator. `PlatformVisuals`
  keeps its renderer-sampled legacy path only for `DISABLED`. The camera
  combines the same target once with Tiny Mounts before its existing bound
  and center/near-plane block clips. A single hard barrier generation retires both the relation and
  `PlatformCamera`'s `smoothed`/`applied` result even for a short teleport or
  unchanged camera entity/world. Preserve mainline `appliedOffset()` as the
  final clipped result consumed by first-person Tiny Mounts. Only a confirmed
  ordinary contact-clear event may fade the last bounded visual residual;
  absence of a query result is not classified as an ordinary clear.
- [x] R4 Prove ROOT-only and JOINT-only nonzero anchor displacement, exact
  passive carry cancellation at the physical carry point, stable-support
  voluntary walking, late localPoint/heartbeat update in both packet orders,
  camera-first/render-first and repeated queries, draw fractions 0/.25/.5/1
  with `CURRENT_ENDPOINT` and a fully applied carry, two accepted endpoints
  before one carry, missing/reordered intermediate endpoint, voluntary walking on
  top of carry, tick-baseline incorporation and multiple same-tick carries,
  local player and controlled vehicle, hard reset and ordinary clear. Compare
  physical-state hashes and actual renderer/camera consumers. Exercise
  nonzero bounded camera offset against center and near-plane obstacles and
  verify the final `appliedOffset()` mainline contract. Run server/client suites,
  latency-independent perf evidence and adversarial mutation/snapshot review.

The current receipt protocol does not tell a remote observer when vanilla
position interpolation has incorporated a particular server carry. S31 will
not infer this from body displacement, packet adjacency, contact tick or render
query order. Remote observers may consume the causal support model from S30,
but their body residual remains zero until a separately reviewed S32 provenance
contract is available. G4.6 cannot close on the local-only S31 result.

## Pre-implementation gates

IMPLEMENTER must inspect exact client transport/lifecycle paths and specify
how events are ordered when a contact, support endpoint and applied carry arrive
in either packet order. The pure transition model and rejected-case tests must
exist before production edits. ADVERSARY independently reviews the complete
plan, including the remote-observer boundary, and may reject it before S31
implementation. Owner visual/gameplay QA is recorded for the end of the epic;
no manual action is requested for this sprint.

### Source audit for the next plan pass — IMPLEMENTER

`AnatomyContactPayload` carries the body's tracking generation and sequence,
the support binding generation, and the material local point. The client only
publishes a present contact after `bindPhysics` confirms it; a clear packet
removes it there. `PresentationFrame` publishes the accepted endpoint serial
and catalog/binding identity. S30 now also exposes the support's tracking
generation separately, after its adversarial alias proof showed that the same
endpoint serial can recur in a new tracking window. S31 must include that
value in its relation key; serial equality alone does not prove continuity.

The local carry observer fires immediately after `TransportLedger.record` in
`AnatomyMovement.carry`. Its cursor names the exact local transport sequence
and support frame serial. At that callback, `AnatomyMovement.transport(body)`
can provide the matching `appliedDelta`; the existing callback also stages the
movement reference, so residual bookkeeping must compose with that behavior.
The event transition must hold an endpoint pending until its matching local
carry outcome is known. The callback must supply the physical point used by
`carry`; a later confirmed point is not assumed to be the one that was carried.
An endpoint burst updates the pending after geometry without discarding the
original before geometry, provided exact serial provenance proves continuity;
the callback must name that original before serial and final after serial.
The transition evaluates endpoint displacement at the supplied physical point
and records the exact applied delta separately; it then handles a later
confirmed point as a rebase. Render/camera consumption must account for how
much of that delta vanilla has already interpolated into the body draw. The
client callback must capture the body's `oldPosition`, tick generation and
pre/post carry positions. If point, identity, sequence or vanilla interpolation
baseline provenance is unavailable, it clears the visual target instead of
guessing from body displacement. Queries from renderer and camera consume the
last published snapshot and never advance transitions.

The pure pre-implementation transition model in
`tools/s31_implementer_plan_model.py` executes both orders of the adversary's
voluntary-walk/rotated-endpoint case. It holds the endpoint pending until the
carry's own point B is known, yielding zero after full carry whether confirmed
B arrives before or after it. The same model checks duplicate cursor and
contact heartbeat, stationary-support voluntary walking, and mismatched frame
fail-closed. Its revised draw function also checks fractions 0/.25/.5/1,
voluntary movement on top of carry, next-tick baseline incorporation and two
same-tick carries. Executed with `python tools/s31_implementer_plan_model.py`:
PASS.
After the revealed endpoint-burst RED, the same model retains the original
before frame across exact accepted 1→2→3 edges and requires the physical
cursor's before serial 1 and final after serial 3. The unchanged
`tools/s31_adversarial_endpoint_burst_probe.py` now exits 0: both direct 1→3
and burst 1→2→3 give draw offset .1 at partial .5 after a fully applied .2
carry. A broken edge clears pending material. This remains plan evidence.
This is plan evidence, not a production test or S31 acceptance.

`acceptedCatalog`, `discardSupportMaterial`, level/connection reset, entity
unload, contact clear, transport lifecycle and player teleport are distinct
existing paths into the same proposed relation barrier. The current
`PlatformClientTeleportMixin` resets `PlatformCamera` for the player position
packet; S31 must additionally reset it for any causal hard barrier that leaves
the camera entity and world unchanged. `origin/main` contains the Tiny Mount
`appliedOffset()` consumer, which this branch must retain when integrated.

## Pre-implementation adversarial review — ADVERSARY

Active role: ADVERSARY. This is an independent review of the plan and current
transport/contact seams, not an executed test of unimplemented S31 production.
No production changes, compilation, canonical gate closure or S31 acceptance.

The independent model distinguishes server-confirmed presentation material
point from the physical local carry anchor. `AnatomyMovement.afterMove` updates
its physical surface and Anchor from the moved body, whereas the receiver's
`presentationContacts` changes only when a newly accepted contact is confirmed.
`carry` evaluates `piece.point(anchor.local())`, applies that passive displacement,
records SupportTransport and publishes the cursor. Consequently a fully known
body/support/frame/sequence cursor need not describe passive movement at the
material point currently held by the presentation relation.

**PLAN RED, R2/R4:** Start on a horizontal support face at confirmed material
point A=(.25,0,0). Voluntary motion updates the physical anchor to B=(.5,0,0),
but the server-confirmed localPoint packet is delayed. A new accepted endpoint
rotates that face by 90 degrees around its gravity axis. R2 first advances its
old presentation point A, adding (-.25,0,.25). Physical carry at B has already
applied (-.5,0,.5); body/support, endpoint serial, binding/tracking and the new
local transport sequence all match. Subtracting the exact applied delta leaves
(.25,0,-.25), even though the passive carry was fully applied. The subsequent
confirmed B packet rebases at the current frame and preserves that spurious
residual under R2. Receiving B before the endpoint instead yields zero for the
same physical motion. No body-position subtraction, shear, unsupported pose,
teleport, duplicate sequence or unknown frame is involved.

Reproduce with `C:/Python314/python.exe tools/s31_adversarial_plan_probe.py`:
exit 1 and durable JSON `../s31-adversarial-plan-red.json` show both packet orders
and the required zero. This probe implements the stated R2 addition/subtraction/
preserving-rebase rule, not a proposed production solution. Classification:
plan/provenance defect, not a bug in unimplemented S31 or a regression claim
against the physical carry subsystem. The exact material point/frame domain
used by passive carry remains a missing discriminator in the candidate relation.
The ordering pre-implementation gate is therefore not converged. A revised plan
must resolve this counterexample and provide its pure event-transition model
before production edits; more independent holdouts remain private until needed.

Performance model for the next plan review: relations must be bounded by live
locally controlled bodies/vehicles; accepted-event transitions need support-local
work and once-consumed transport sequences; render/camera reads must not mutate
relations, extract model geometry, scan other supports or rebuild histories.
Spatial acceptance will pair actual ROOT/JOINT/carry consumers with fixed-view
snapshots and physical-state checks. Remote-observer residual remains explicitly
zero pending the separate provenance contract; this boundary does not close G4.6.

### Revised physical-point relation: draw-incorporation RED — ADVERSARY

The revised point domain addresses the first revealed counterexample. Executed
`tools/s31_implementer_plan_model.py` passes its late/early confirmed-point,
full carry, duplicate, stable walking and frame-mismatch checks. Historical
`tools/s31_adversarial_plan_probe.py` remains the immutable rejected-rule probe;
its RED is not attributed to the revised rule.

The complete plan still fails at the local body draw boundary. Installed
Minecraft 26.2 `EntityRenderer.extractRenderState` computes state.x with
Mth.lerp(partial, body.xOld, body.getX()), likewise y/z. The actual carry route
writes body.setPos(current+allowed), and neither that route nor Entity.setPos /
setPosRaw updates xOld/yOld/zOld. S30 renders the support's CURRENT_ENDPOINT
by default. Fully applied physical carry is therefore not necessarily fully
incorporated into the ordinary body's drawn position at partial<1.

**PLAN RED, R2/R3/R4:** a locally controlled body with ordinary vanilla draw
position and no independent causal model root receives pure support ROOT +.2x,
with no voluntary movement or contact change. Endpoint/carry point/frame/sequence
provenance is complete; passive carry applies exactly +.2x. The revised model
publishes residual .2-.2=0. At partial=.5, the vanilla body is drawn at +.1x while
the accepted support endpoint/material is drawn at +.2x. R3 adds the zero target,
leaving .1 block of visible slip. Fractions0/.25/.5 fail, fraction1 alone passes.
A purely physical carry cancellation proof cannot establish the draw/camera
incorporation marker, even for a local actor. This is independent of the first
late-localPoint case and requires no packet mismatch or uncertified interval.

Reproduce `C:/Python314/python.exe tools/s31_adversarial_render_fraction_probe.py`:
it imports and executes the actual candidate pure Relation and then applies
the installed vanilla draw-position equation; exit1. Durable numeric evidence:
`../s31-adversarial-render-fraction-red.json`. Actual pinned bytecode inspection
is preserved in `../s31-entity-renderer-bytecode.txt` and
`../s31-entity-bytecode.txt`; the relevant methods were inspected directly from
`C:/Users/Usuario/.gradle/caches/fabric-loom/26.2/minecraft-merged.jar`.
Javap printed those method bodies but returned an ancillary archive-close access
warning; it is not a claimed compile/runtime failure. The algebra probe is plan
review evidence, not a native gameplay acceptance or unimplemented-production
bug. No production changed and no Minecraft build was run for this review.

Classification: plan draw-incorporation gap at R2/R3, observable under R4's
nonzero ROOT/fraction consumer criterion. The endpoint/transport event model
must converge with actual renderer/camera consumption before S31 production.
Full G4.6 remains open; remote-observer provenance is still deferred explicitly.

### Revised draw incorporation: endpoint-burst RED — ADVERSARY

The revised pure draw function resolves the single fully applied carry at
fractions0/.25/.5/1; the candidate's script passes, and an independent positive
control with accepted frame1->3, +.2 applied carry and partial.5 returns +.1.
The two older probes remain historical rejected-rule evidence, not assertions
against that corrected draw function.

**PLAN RED, R2/R3:** two accepted support endpoints arrive before one local body
carry. The relation/physical anchor starts at frame1. Frame2 moves the support
+.1x, then frame3 moves it a further +.1x; neither arrival is a lifecycle barrier.
Local physics has not yet consumed frame2 and carries its original anchor to
current accepted frame3 by +.2x in one sequence. This is possible in the current
seams: receiver endpoint acceptance and body carry are separate events, and the
physical Anchor retains its previous material point until carry/afterMove.
The identity, physical point, final frame serial and new local sequence are
known; there is no missing packet, duplicate transport or unknown material.

The actual candidate Relation.endpoint discards its pending pair when another
endpoint arrives, also discarding the new endpoint. Carry(3,1,point,+.2) therefore
has no matching pending geometry and clears the draw sample. At partial.5,
vanilla body x=.1 plus draw offset0 leaves .1 slip against support x=.2. Omitting
the intermediate frame and directly accepting the same final frame before the
same carry produces the required +.1 offset. Event batching alone changes the
observable result despite identical final support/body state and exact carry.
The revised plan's singular pending endpoint and matching-after-serial cursor
have not specified/proven the physical carry's before-frame domain across this
normal accepted burst. Generated missing provenance must not be mistaken for
an external discontinuity in this case.

Reproduce `C:/Python314/python.exe tools/s31_adversarial_endpoint_burst_probe.py`:
exit1, including the positive direct-acceptance control, actual candidate Relation
calls and both numeric results. Durable evidence:
`../s31-adversarial-endpoint-burst-red.json`. Classification: pre-implementation
plan/event model defect, not a native gameplay or S31-production failure. No
production was edited or compiled. A complete zero-change plan pass is still
pending; G4.6 remains open.

### Revised burst provenance: zero-change plan review — ADVERSARY

The unchanged endpoint-burst holdout now exits0: direct and two-endpoint burst
both publish +.1 draw correction at partial.5 after +.2 fully applied carry.
The matching before-frame domain is explicit in the revised plan/cursor;
the pending pair keeps its earliest exact before geometry and latest accepted
after geometry only across a verified shared endpoint. The old three RED
records/probes remain immutable evidence of their rejected rules.

Executed the current IMPLEMENTER pure model (PASS) and independent
`tools/s31_adversarial_plan_campaign.py` (exit0), with durable result
`../s31-adversarial-plan-campaign-green.json`: seed1329, 512 affine point-domain
cases, 8192 draw-fraction assertions plus rejected-edge checks. The campaign
compares direct/burst acceptance and early/late contact point orders, preserves
voluntary movement in the drawn position, checks duplicate cursors and query
immutability, and rejects wrong before-frame/baseline provenance. No production,
IMPLEMENTER model or oracle was edited to obtain GREEN. The repaired burst
proof uses the same test calls and numeric oracle as its original RED.

Complete independent review of the revised S31 dependency scope now requests
zero plan changes. Classification: **PLAN GREEN**, not implementation, native
renderer/camera/gameplay acceptance or G4.6 closure. The implementation must
still execute R4 against actual controlled body/vehicle and camera consumers,
including nonzero ROOT/JOINT fractions, physical-state invariants, ordinary fade
versus immediate generation reset, obstacles and mainline appliedOffset().
Prior pin-source render interpolation and physical carry audits remain applicable;
future production must supply the exact stamps and immutable draw sample promised
by the converged plan rather than assume the pure model's default fixture stamps.

The revised storage shape is bounded: one live local relation and one coalesced
before/after pair, scalar transport/baseline watermarks and immutable draw values.
Known duplicates/queries do not add histories. Planned render/camera reads are
constant-space pure arithmetic; model extraction/reflection stays preparation,
and event evaluation must remain local to the selected support/material point.
The production performance audit, mutation checks, snapshots and required suites
remain required after implementation. Remote body offsets stay zero until S32's
separate provenance contract; S31 cannot close all FR-086..088 or full G4.6.

## Production adversarial review — ADVERSARY

### Exact duplicate carry retires a valid draw sample

Classification: production S31/R2 idempotence defect. Executed client-native
`S31AdversarialDuplicateCarryProof` fails in
`../s31-adversarial-duplicate-carry.log`, BUILD FAILED40s, with
`Exact duplicate carry erased the nonzero draw sample`. The first callback
publishes x=.10000000000000009 at partial.5; repeating the same exact cursor,
body and ledger result immediately returns zero.

This test isolates the production transition, rather than claiming end-to-end
contact acquisition: it obtains before/after PresentationFrames through real
network acceptance, uses a live local body and actual TransportLedger, and seeds
a matching production relation via test-only reflection. The real production
carried/rendererOffset methods are then exercised without a test-side residual
implementation. Its nonzero positive control and physical position/AABB/velocity
invariants pass; no render query, packet mismatch or tick-baseline change occurs
between the first and duplicate callback. The accepted root moves +.2x, so its
partial.5 draw correction is positively +.1x before the duplicate.

Reproduce the existing isolated client lane with
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S31AdversarialDuplicateCarryProof`
and `-I tools/s29_adversarial.init.gradle`, JAVA_HOME ms-25.0.3, own build/run/cache.
Production was not edited. The source groups an already-consumed sequence with
missing pending geometry into its hard reset branch; the converged plan/model
instead require exact duplicate cursors to be idempotent. This RED prevents
S31 acceptance. Native ROOT/JOINT acquisition, camera clipping/reset, visual,
controlled vehicle and production performance review remain pending; an ordinary
suite's GREEN does not replace those independent criteria.


The duplicate repair passes the unchanged native holdout in
`../s31-adversarial-duplicate-carry-repeat.log` (BUILD SUCCESSFUL28s):
before/after remain .10000000000000009. This repeat compiled the IMPLEMENTER
repair; it is not a second RED on the original source. Other S31 acceptance
criteria remain open; no gate closure follows from this isolated transition.


### Unexpected interpolation baseline leaves camera smoothing alive

Classification: production S31/R3 immediate hard reset defect. The actual native
camera consumer in `S31AdversarialCameraBaselineBarrierProof` receives a positively
nonzero accepted carry correction: renderer x=.1 and camera x=.039346934028736694.
The fixture then calls the real body.setOldPosAndRot inside the same unchanged
body tick, replacing the old-position marker unexpectedly without changing body
position, world or camera entity. The marker mismatch is explicitly asserted.
RendererOffset correctly returns zero, but PlatformCamera.offset retains
x=.03931134116164233 from its old smoothing. No ordinary contact-clear packet
or normal next-tick incorporation is involved. The converged R3 requires an
unexpected marker to fail closed and reset the retained camera result.

`../s31-adversarial-camera-baseline-barrier.log` exits BUILD FAILED42s with
`Camera retained smoothing across an unexpected same-tick interpolation baseline
barrier`. Reproduce the ordinary isolated ADVERSARY selector with
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S31AdversarialCameraBaselineBarrierProof`.
The scope is a production-consumer transition fixture with real network-accepted
frames, ledger, live body and actual main camera; matching relation acquisition
is seeded by test-only reflection as in the duplicate-carry proof. It is not an
end-to-end local contact acquisition claim. No production code changed.

The initial fixture used getMainCamera(), which does not exist in pinned26.2;
corrected to the verified mainCamera() before classification. An earlier normal
next-tick incorporation probe also observed residual smoothing but is not claimed
as a production defect: that scenario mixes ordinary continuous target smoothing
with draw retirement. Its temporary source was removed after durable observation,
and the final immutable handed-off oracle instead isolates an unexpected same-tick
baseline change with R3's explicit immediate-reset requirement. Numeric native
camera output supplies stronger signal for this temporal barrier than a static
screenshot alone. Final snapshot/clipping/native acquisition/performance/mutation
coverage remains open and S31/G4.6 is not accepted.

### Camera barrier repair GREEN; quiet endpoint before ROOT RED

The unchanged duplicate and unexpected-camera-baseline proofs pass together in
`../s31-adversarial-duplicate-camera-repaired.log`, BUILD SUCCESSFUL50s with clean
exit. Duplicate .1 is preserved, and actual camera smoothing .039346934028736694
becomes zero across the asserted unexpected same-tick baseline replacement.
This is isolated-consumer GREEN, not S31-wide acceptance.

The next wire-to-physics producer/consumer holdout fails production R2/R3:
`S31AdversarialLocalContactRootProof` accepts a valid test contact envelope through
actual networking and receiver/physics confirmation for the live local player,
placed on the accepted Cow material. No private relation is injected. It wraps
the installed production observer only to capture its result after delegation.
This does not claim server collision discovery of the test-authored contact.
Actual native carry applies and records the passive displacement.

An accepted endpoint first advances the serial without moving the material;
three client ticks follow. A subsequent ROOT endpoint moves +.2x. The physical
callback's exact beforeSerial6/afterSerial7 and appliedDelta
(.20000000000000018,0,0) provide a nonzero full-carry positive control, but its
production rendererOffset at partial.5 is zero in that same callback after
production residual bookkeeping. Expected +.1 closes vanilla's draw gap.
`../s31-adversarial-local-root.log` exits BUILD FAILED33s with
`Wire-confirmed local ROOT carry lost its nonzero draw residual after a quiet endpoint`.
The scalar zero result is not caused by waiting until the body baseline retires:
it is captured synchronously inside the real carry observer.

The quiet endpoint advances the physical anchor's serial without emitting a
nonzero carry; the visual relation can retain an earlier pending-before serial.
The next genuine moving cursor then mismatches and retires the relation. A normal
quiet accepted frame is not a hard lifecycle barrier. Reproduce the existing
isolated client selector with
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S31AdversarialLocalContactRootProof`.
No production was edited. S31's remaining native JOINT/vehicle/clipping/reset,
spatial snapshots and production cost/mutation criteria remain open.


### Quiet-frame repair and native JOINT renderer GREEN

Active role: **ADVERSARY**. The unchanged local ROOT proof repeats the original
quiet-frame RED in `../s31-adversarial-local-root-repeat.log` (45s). After the
IMPLEMENTER quiet-cursor repair, three unchanged holdouts pass together in
`../s31-adversarial-quiet-repair-battery.log`, BUILD SUCCESSFUL51s, clean exit:
ROOT actual carry before6/after7 applies +.2x and publishes +.1x at partial.5;
exact duplicate preserves the draw sample; unexpected same-tick baseline retires
actual camera smoothing immediately.

`S31AdversarialLocalContactJointProof` independently confirms a valid test-authored
wire contact on the live head material, actual client receiver/physics acquisition,
a quiet endpoint followed by head pitch +10 degrees, and actual passive carry.
ROOT origin is unchanged. The native EntityRenderer extractRenderState and
getRenderOffset consumer returns half the actual JOINT carry at partial.5;
vanilla state position plus that correction equals the current physical position.
No private relation is injected and no test-side residual formula substitutes
for the production consumer. `../s31-adversarial-local-joint.log` is GREEN30s,
with actual carry (-.001191563904285431,-.03635716438293457,.0405459851026535)
and actual renderer offset its half.

After the IMPLEMENTER immutable DrawSample change, all four unchanged holdouts
pass together in `../s31-adversarial-immutable-draw-battery.log`, GREEN35s.
These prove local ROOT/JOINT and the tested consumer transitions. They do not
close controlled vehicle, clipping, remaining lifecycle axes, deterministic
spatial snapshots or production performance/mutation review. G4.6b/S31 and full
G4.6 remain unaccepted. ADVERSARY changed only test infrastructure and this record.


### Controlled vehicle, camera clipping and HOT_PRESENTATION GREEN

Active role: **ADVERSARY**. `S31AdversarialControlledVehicleProof` passes in
`../s31-adversarial-controlled-vehicle.log`, BUILD SUCCESSFUL29s. A normally
mounted oak boat is the live locally authoritative prediction owner; it acquires
a valid test-authored contact via actual receiver confirmation and carries +.2x
through the real physics callback. Its renderer residual is +.1 at partial.5,
its rider's cameraOffset receives the same value, and its rider avatar's
rendererOffset stays zero. The support Cow is scaled10 to admit the boat's
body bounds. No relation injection is used in this vehicle proof. This tests
the camera target ownership; camera smoothing/clipping are exercised separately.

`S31AdversarialCameraClipProof` passes in `../s31-adversarial-camera-clip.log`,
BUILD SUCCESSFUL29s. The isolated accepted-frame/real-ledger fixture has a positive
native main-camera result .039346934028736694x. A deterministic stone collider
1cm ahead of its center ray cuts that value to exact zero under the existing
2cm safety margin. The final appliedOffset equals the clipped result, and
physical position/AABB/velocity are unchanged. Camera positioning and relation
acquisition are explicitly test-only fixture setup; this is not a claim of
native camera/body placement acquisition.

`S31AdversarialResidualWorkProof` passes in `../s31-adversarial-residual-work.log`,
BUILD SUCCESSFUL28s. After32768 warmup calls,32 batches of4096 active production
rendererOffset queries give p95=106.5185546875ns/query, p99=130.6640625ns/query,
and p95 allocated120bytes/query under this JDK/runtime. Relation and immutable
draw sample object identities, physical position/AABB/velocity are preserved.
This is isolated local HOT_PRESENTATION evidence, not a whole-scene G9 latency
budget. The path does bounded local gravity reads, one baseline Vec3 and offset
arithmetic; no model evaluation, catalog scan or reflection occurs inside the
measured production query. Shared synchronized gravity access remains a bounded
local dependency; this test does not measure competing-thread contention.

### Gravity barrier round-trip revives retired draw material RED

Classification: production S31/R1-R3 hard-barrier lifecycle defect.
`S31AdversarialGravityRoundTripProof` fails in
`../s31-adversarial-gravity-round-trip.log`, BUILD FAILED47s. Its network-accepted
ROOT frame moves+.2x and the real ledger/carry production consumer publishes+.1x
at partial.5. Effective body gravity changes DOWN→EAST within the same body tick;
rendererOffset correctly returns zero. Changing EAST→DOWN immediately revives
that exact preceding+.10000000000000009 draw sample, without a new endpoint,
carry, confirmed contact, world change or interpolation marker change. Assertion:
`Retired pre-gravity draw sample revived after same-tick gravity round trip`.

This isolated transition uses test-only relation acquisition as the duplicate
proof and the existing effective-gravity development seam; it does not claim
an installed GravityChanger packet integration. The defect is no-revival after
an observed hard gravity barrier, rather than an incorrect gravity-direction
projection. The holdout calls only the renderer during the barrier; acceptance
must not depend on the camera happening to query and retire the old relation.
Reproduce with the existing init and isolated selector
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S31AdversarialGravityRoundTripProof`.
Production was not edited. S31 remains RED; snapshots, remaining lifecycle axes
and mutation coverage are still open. The JOINT fixture now contains a reusable
CSV capture/fraction sweep, but no captured snapshot is claimed before its
updated lane executes.


### Gravity generation repair, consumer battery and deterministic snapshot GREEN

Active role: **ADVERSARY**. The unchanged gravity round-trip holdout plus the
seven prior consumer proofs pass together on the observation-generation repair
in `../s31-adversarial-gravity-repair-battery.log`, BUILD SUCCESSFUL55s. The
restored-gravity result remains exact zero. Repeated HOT_PRESENTATION measurement
with generation observations gives p95=285.9130859375ns/query,
p99=309.1064453125ns/query and120bytes/query. The added local observation/map
reads explain the increase from the preceding106.52ns measurement; immutable
sample/relation identity and physical state still remain unchanged. No claim of
whole-scene latency or contended gravity-provider cost follows from this lane.

The JOINT fixture's capture inputs are now explicitly fixed to walk phase0,
walk amount0, age0, head yaw0, head pitch0 followed by pitch10; this is fixture
normalization, not a weakened oracle. `../s31-adversarial-joint-fixed-snapshot.log`
is GREEN30s. At fractions0/.25/.5/.75/1, actual EntityRenderer state+offset
matches the endpoint physical position. Its actual physical AABB and those
native positions are captured in the own-run CSV. Reusable
`tools/s31_adversarial_snapshot.py` renders the deterministic Y/Z bounds sheet
`../s31-adversarial-joint-snapshot.png` with adjacent JSON provenance and CSV
hash. ADVERSARY viewed the image: uncorrected orange bounds converge gradually;
corrected green bounds coincide with the endpoint bounds across all fractions.
The sheet is a geometric native-consumer capture, not a Minecraft pixel capture.
Reproduce the isolated JOINT lane, then run the snapshot tool against
`../s29-adversary-run/s31-adversarial-joint-snapshot.csv`.

Reusable `tools/s31_adversarial_mutants.py` creates test-only compile replacement
copies outside production. The partial mutation replaces partial*applied with
full applied: unchanged native JOINT oracle kills it semantically in
`../s31-adversarial-mutant-partial.log`, BUILD FAILED29s, actual renderer mismatch.
The duplicate mutation removes exact-cursor idempotence: unchanged duplicate
oracle kills it semantically in `../s31-adversarial-mutant-duplicate.log`,
BUILD FAILED43s, nonzero sample erased. No production file was edited. Quiet
and gravity copies have been generated but not yet executed; no result claimed.
Further native owner/lifecycle transition coverage remains open; S31 is not
closed by this GREEN subset.


### Native vehicle authority loss retains old draw sample RED

Active role: **ADVERSARY**. Classification: production S31/R1-R3 owner-barrier
defect. `S31AdversarialControlBarrierProof` is the native controlled-boat/contact/
carry path with no injected relation. The installed production observer executes
first; the oracle sees actual applied+.2x, renderer+.1x and positively nonzero
actual camera smoothing. It then invokes the real local player's stopRiding
inside the same tick. The rootVehicle is now the player, the boat's controlling
passenger is no longer that player, and the boat's isLocalInstanceAuthoritative
returns false. Its production rendererOffset nevertheless retains
+.10000000000000009 from the old draw sample.

`../s31-adversarial-control-barrier.log` exits BUILD FAILED31s with
`Retired controlled vehicle retained prior draw correction after dismount`.
Reproduce the isolated selector
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S31AdversarialControlBarrierProof`
using the existing init and ADVERSARY outputs. The camera-after-dismount oracle
is present but was not reached because the body-renderer assertion fails first;
no camera-after-dismount result is claimed yet. An END-tick authority refresh
cannot replace the required immediate hard barrier between event and draw.
No production changes were made. S31/G4.6b remains RED.


### Remaining mutation controls killed

Active role: **ADVERSARY**. The quiet mutation is killed by the unchanged native
local ROOT quiet→moving oracle in `../s31-adversarial-mutant-quiet.log`,
BUILD FAILED26s, nonzero draw residual lost. The gravity generation omission
is killed by the unchanged gravity round-trip oracle in
`../s31-adversarial-mutant-gravity.log`, BUILD FAILED33s, pre-gravity sample
revived. All four compile-replacement mutants now have semantic kills, not
compilation failures. Their CausalBodyResidual source snapshot SHA256 was
be978af66ba57dfa30e3e242bf81f8b2d76bf87bcc1bdca435fda5e92647aac6;
this predates the subsequent owner-barrier repair. The tests/oracles were not
weakened to produce kills. Reusable generator remains under tools; generated
replacement source copies are temporary and removed after evidence capture.


### Owner query barrier GREEN; control round-trip no-revival RED

Active role: **ADVERSARY**. The unchanged simple ControlBarrierProof executes
successfully first in `../s31-adversarial-control-repair-roundtrip.log`: a real
dismount immediately returns zero for the former vehicle renderer and actual
camera smoothing; the sample before this barrier is positively+.1x.

The next independent native `S31AdversarialControlRoundTripProof` extends that
same real controlled-boat/contact/carry path. It invokes stopRiding, observes
renderer zero and lost local authority, then invokes the actual local player's
startRiding on the same vehicle. The rootVehicle and local authority return to
the boat inside the same unchanged body tick; no new contact, endpoint or carry
is accepted. The renderer then revives the pre-barrier+.10000000000000009 draw
sample. No camera query occurs between loss and reacquisition: the owner barrier
must not depend on a different consumer happening to retire shared history.

Classification: production S31/R1-R3 no-revival after observed owner discontinuity.
The combined lane exits BUILD FAILED41s with
`Retired vehicle draw sample revived after same-tick control round trip`.
Reproduce the isolated selector
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S31AdversarialControlRoundTripProof`.
No relation was injected in either control proof; no production was edited.
This RED prevents acceptance despite the preceding GREEN snapshot and four
semantic mutation kills. Remaining full lifecycle/late-contact/voluntary-move
native matrix and final zero-change whole-candidate review remain pending.


### Owner-generation repair and native R4 matrix GREEN subset

Active role: **ADVERSARY**. Both immutable owner holdouts plus eight earlier
consumer proofs pass together in `../s31-adversarial-owner-generation-battery.log`,
BUILD SUCCESSFUL57s with clean exit. Same-tick observed loss/reacquisition now
leaves exact zero. HOT_PRESENTATION repeats at p95=251.318359375ns/query,
p99=258.935546875ns/query and120bytes/query with owner/gravity observations.

`../s31-adversarial-voluntary-heartbeat-second.log`, GREEN33s, proves native
voluntary movement+.025x on top of actual JOINT carry after the carry completes;
actual renderer fractions0/.25/.5/.75/1 retain ordinary motion interpolation and
cancel only the actual passive delta. The first fixture moved inside the still-
open carry observer and reentered physics; the second oracle omitted ordinary
physical separation between callback and checkpoint. These are classified
fixture/oracle errors, not production defects. The final checkpoint executes
at END_CLIENT_TICK after production physics, and its expected draw retains all
non-passive baseline displacement. Physical position/AABB/velocity/gravity/old
position remain unchanged by queries.

The same lane proves both heartbeat localPoint packet orders after native
voluntary+.025z walking on stationary head material: stable-support renderer
residual stays zero. Heartbeat before or after the moving JOINT endpoint gives
native applied(0,-.0406983693,.0401836554) and half that value in the renderer.
No relation is injected in these acquisition/movement proofs.

`S31AdversarialEndpointBurstProof` uses actual ClientboundBundlePacket decoding
to deliver two exact ROOT endpoints before one carry. Complete order produces
before5/after7, applied+.2x and residual+.1x; omitted/reversed intermediate gives
that same nonzero physical control and renderer zero. The latter proves
fail-closed behavior rather than inference from newest geometry. GREEN34s in
`../s31-adversarial-endpoint-burst-second.log`. The initial fixture compile error
was Java generic invariance at the bundle packet list; fixed only in the test.
Pinned26.2 constructor signatures were verified from the installed merged JAR.

`S31AdversarialSameBaselineCarryProof` passes in
`../s31-adversarial-same-baseline.log`, GREEN54s: two separate exact accepted
frame transitions and real ledger segments+.2 and+.1 within one immutable body
baseline accumulate+.15 at partial.5; repeating either exact cursor preserves
its sample. Relation/pending acquisition and actual segment application are
explicitly seeded test setup as in DuplicateCarryProof; this is isolated
production-consumer evidence, not a native two-dispatch carry integration claim.

`S31AdversarialNearPlaneClipProof` passes GREEN28s in
`../s31-adversarial-near-plane-clip.log`. A native camera ray through the center
misses the deterministic wall while an actual near-plane corner hits. An
analytic leading-corner bound limits the allowed correction; native camera
cuts its positive .039346934028736694x target to0, and appliedOffset is final0.
This separately exercises near-plane clipping from the preceding center-ray
clip proof. Camera positioning/relation acquisition remain isolated test setup.

`S31AdversarialLifecycleProof` passes GREEN46s in
`../s31-adversarial-lifecycle.log`. For each of seven native contact/carry cases,
post-carry draw+.1 and actual camera+.039346934028736694 are positively checked
before a barrier within the unchanged interpolation baseline. Actual core
support-root invalidation, body lifecycle invalidation, unavailable-support
invalidation, model-reload presentation callback, accepted-contact-clear callback,
body gravity and support gravity all leave renderer/camera/appliedOffset0,
without writing position/AABB/velocity. Contact is acquired through receiver and
physical core; barrier callbacks are invoked directly, not claimed as packet-
transport integration for every lifecycle axis. Ordinary clear immediately
retires in this candidate; that is an allowed policy, not a universal no-fade
requirement. Gravity mutation uses the existing development authority seam.

`S31AdversarialTickIncorporationProof` passes GREEN31s in
`../s31-adversarial-tick-incorporation.log`: actual positive carry/camera then
one real client tick retires renderer, camera and appliedOffset to0. Camera-
first/render-first causal target reads agree and actual camera consumption does
not change the draw material. Physical snapshots bracket each query, not the
ordinary tick's legitimate physics. Source review also verifies the existing
connection/dimension/catalog retirement routes clearLevelMaterial→reset, pose
UNAVAILABLE and support lifecycle routes retireSupport, body discontinuity and
entity unload callbacks retireBody, player-position packet handler reset, and
model reload callback reset. These combined callback/native/source results do
not imply new remote-observer provenance or close all G4.6.


### Authority event burst without intervening query RED

Active role: **ADVERSARY**. Classification: production S31/R1-R3 authority
barrier depends on query history. `S31AdversarialControlBurstProof` acquires
actual controlled-boat contact and carry, positively checks+.1 renderer and
native camera, then performs actual stopRiding/startRiding within one body tick.
The intermediate rootVehicle and controlling-passenger loss are asserted; local
vehicle authority is false. No presentation or prediction observation is called
between these two events. Once the same real authority returns, rendererOffset
revives+.10000000000000009 without any new endpoint/carry/contact. A hard control
handoff must retire historical material independently of consumers querying the
intermediate state; the earlier observed-owner round-trip GREEN is insufficient.

`../s31-adversarial-control-burst.log` fails28s; an unchanged semantic oracle
repeats in `../s31-adversarial-control-burst-repeat.log`, FAILED44s with
`Pre-handoff vehicle draw sample survived unobserved same-tick control burst`.
The repeat cleans fixture logging only: the first version printed a literal zero
placeholder for the intentionally omitted query; it was never a measured
intermediate residual. The repeat logs only the actual lost authority and actual
restored residual. Neither trial injects a relation or modifies production.
Reproduce with the ordinary isolated selector
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S31AdversarialControlBurstProof`.

Previous subset GREEN results remain valid at their recorded candidate points.
The current candidate is RED on this further authority frontier. Final combined
zero-change candidate review is still pending; full G4.6 additionally remains
open for remote body provenance/S32. Generated mutation copies were removed;
reusable generator, snapshot renderer and holdouts remain owned by ADVERSARY.

### Native authority event repair: combined independent GREEN

Active role: **ADVERSARY**. The unchanged semantic no-query burst holdout now
passes together with all eighteen existing S31 adversarial classes on the native
riding-event candidate. `../s31-adversarial-event-owner-battery.log` reports
BUILD SUCCESSFUL in 1m45s and clean client shutdown. This includes exact duplicate,
marker/gravity/control barriers, real ROOT and JOINT local carry, locally
controlled boat and rider camera, voluntary displacement, before/after heartbeat,
complete/missing/reversed endpoint bundles, same-baseline callback accumulation,
seven lifecycle callbacks, next-tick incorporation, center and near-plane native
camera rays, and the bounded-query work probe. No production changes were made
by ADVERSARY. The new event hooks were reviewed at native startRiding RETURN and
removeVehicle HEAD; both reach the existing relation reset policy.

The measured current work probe ran131072 active queries after warmup: p95
500.854ns/query, p99 500.952ns/query,120 allocated bytes/query. These are local
microbenchmark measurements, not a whole-scene latency or contention budget.
The observation checks stay local and bounded; gravity observation locking is
not a multithread contention proof. Previously captured four mutation kills and
the deterministic JOINT contact sheet retain their stated earlier-candidate
provenance. The contact sheet is a geometric consumer snapshot, not a Minecraft
pixel screenshot. Native integration fixtures use test-authored valid contact
packets; they do not independently establish server collision acquisition.

A further dual-role LivingEntity fixture (controlled Pig with its own accepted
original-model support binding) is still being stabilized. Preliminary runs
lost native control before the tested carry; they are fixture-precondition
failures, not a classified production defect or a GREEN composition claim.
S31 closure has not yet been dictated on that frontier. Full G4.6 additionally
retains the separately planned remote-body provenance boundary.

### Dual-role original-model/body composition RED

Active role: **ADVERSARY**. Classification: production S31/R3-R4 composition
failure at the actual original LivingEntity renderer, not a missing carry or
remote observer case. `S31AdversarialDualRolePigProof` starts a real mounted
locally authoritative Pig with its own accepted causal model binding, acquires
a wire-confirmed contact on a Cow, and receives a quiet endpoint followed by
ROOT+.2x. The installed real transport observer delegates production before
capturing the actual consumer. At partial.5 the physical Pig is x2.2, its own
accepted support-model origin is x2.0, and CausalBodyResidual supplies+.1x.
The actual LivingEntityRenderer submit queues the original Pig model at
x2.1000001 instead of the carried physical x2.2. A locally controlled actor that
also owns an accepted original-model binding therefore retains a half-carry
slip; the controlled-boat GREEN could not exercise this composition.

The first stabilized capture failed in `../s31-adversarial-dual-role-pig-alive.log`
(27s). An independent X-only oracle excludes incidental steering in Z and
repeats the same production mismatch in
`../s31-adversarial-dual-role-pig-repeat.log` (26s):
`queued=2.1000001 physical=2.2`. Positive controls include native Pig ownership,
nonzero renderer residual, accepted model submission and exact X origins. The
capture revision additionally checks actual TransportLedger.appliedDelta.x=.2
and records actual accepted/physical/vanilla/residual/queued X in a CSV before
the failing assertion. No relation injection or production modification occurs.

Earlier preparation timeouts are classified as fixture precondition failures:
the initial Pig stopped being saddled/controlling and did not reach the tested
carry. The stabilized fixture uses an adult, invulnerable Pig and an explicitly
valid equippable saddle. Those changes protect the required positive native
control; they do not replace the original renderer, accepted frame or physics
carry oracle. The assertion compares only ROOT X, leaving native steering and
ordinary voluntary Z movement outside this isolated comparison.

Reproduce with `tools/s29_adversarial.init.gradle` and the existing isolated
build/run/cache directories, selector
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S31AdversarialDualRolePigProof`.
The eighteen-class battery remains GREEN at its recorded scope, but S31 is RED
on this additional compositional frontier. Final zero-change closure is pending
an implementation repair and unchanged semantic holdout revalidation.

The final capture lane `../s31-adversarial-dual-role-pig-capture.log` repeats
FAILED26s with the actual applied carry positive control intact. Its CSV records
partial.5, accepted_x2.0, physical_x2.2, vanilla_x2.1, residual_x.1 and
queued_x2.1000001. Reusable
`tools/s31_adversarial_dual_role_snapshot.py` renders that captured comparison
at `../s31-adversarial-dual-role-pig-snapshot.png`, with source SHA256 and scope
in its JSON sidecar. ADVERSARY visually inspected the deterministic comparison;
the original-model queued root coincides with vanilla's half-carry instead of
the physical endpoint. This is geometric native-consumer evidence, not a game
pixel screenshot. The single-axis comparison deliberately excludes incidental
native steering in Z. Production remained untouched throughout all repeats.

### Dual-role ROOT repair: nineteen-class independent GREEN

Active role: **ADVERSARY**. The original Pig composition holdout passes without
changing its semantic oracle alongside the preceding eighteen S31 classes:
`../s31-adversarial-dual-role-repaired-battery.log`, BUILD SUCCESSFUL1m47s,
clean exit. The actual original-model root is now x2.2, matching physical x2.2;
vanilla interpolation remains x2.1 and the bounded residual remains+.1. Native
authority/contact and actual applied carry+.2 remain positive. The repaired
geometry comparison at `../s31-adversarial-dual-role-pig-repaired.png` and JSON
was generated with the reusable snapshot tool and visually inspected. The
previous RED PNG/JSON remain separately retained. The native-capture CSV is a
latest-run output; individual JSON sidecars pin its captured values and hash.

Current work probe131072 queries: p95≈526.8ns/query, p99≈547.9ns/query,
120bytes/query. These remain bounded local microbenchmark results, without a
whole-scene or lock-contention claim. Production review confirms the additional
root-origin policy is limited to entity_root and a valid active local transport
sample, retaining the accepted joint pose/model adapter. Its effect after the
next native body tick is under additional independent review; S31 closure has
not yet been dictated. ADVERSARY changed no production.

### Dual-role next-tick incorporation RED

Active role: **ADVERSARY**. Classification: production S31/R3-R4 root composition
regression after natural baseline incorporation. The extended independent
`S31AdversarialDualRoleIncorporationProof` first requires the now-GREEN native
Pig ROOT+.2 carry and original-model x2.2 draw at partial.5. It then waits one
actual client tick, requires the same native local owner and positive accepted
original model, and verifies the residual retired to0. Vanilla's body baseline
has now incorporated the carry: state.x2.2 and physical.x2.2. The actual queued
original model nevertheless returns to its older accepted root x2.0.

`../s31-adversarial-dual-role-incorporation.log` fails28s:
`physical=2.2 vanilla=2.2 accepted=2.0 queued=2.0`, followed by
`Dual-role original model revived pre-carry accepted root after native tick incorporation`.
The newly allowed vanilla root preservation expires with the fractional draw
sample, while native local authority and the incorporated physical carry remain.
The former holdout's initial interpolation fraction passes; no original oracle
was rewritten. This is not a remote actor, absent accepted pose, missing carry,
incidental Z steering or a body ownership loss. ADVERSARY modified no production.
Reproduce with the same isolated init/build/run/cache command, replacing the
selector by
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S31AdversarialDualRoleIncorporationProof`.
The nineteen-class prior GREEN remains scope-qualified; S31 is RED on this
post-incorporation consumer boundary and is not closed.

The unchanged semantic next-tick oracle repeats in
`../s31-adversarial-dual-role-incorporation-repeat.log`, FAILED26s. The only
fixture addition is a capture before the assertion: partial.5, accepted_x2.0,
physical_x2.2, vanilla_x2.2, residual_x0, queued_x2.0. Reusable snapshot tool
renders its deterministic geometric comparison at
`../s31-adversarial-dual-role-pig-incorporated-red.png` with source/value/hash
JSON sidecar. ADVERSARY visually inspected it: queued original root returns
fully to the old endpoint while physical/vanilla remain coincident at the
carried position. This is actual native consumer geometry, not game pixels.

### Post-incorporation repair: final independent combined GREEN

Active role: **ADVERSARY**. Both dual-role semantic holdouts pass unchanged
against the contact-lifetime root candidate: physical/vanilla/queued X2.2 after
the next tick, residual0, accepted older rootX2.0, and native owner/contact
remain positive. Twenty S31 classes pass together in
`../s31-adversarial-contact-root-battery.log`, BUILD SUCCESSFUL1m54s. Six relevant
S30 original-model/compatibility/pose/signed-pose/tracking/cost classes pass in
`../s31-adversarial-original-model-regression.log`, BUILD SUCCESSFUL40s. The
rejected historical compatibility PLAN oracle is intentionally excluded; its
accepted current compatibility proof is included.

A directed compile-replacement mutation limits vanilla root preservation to
an active fractional transport sample instead of accepted contact lifetime.
The unchanged incorporation holdout kills it:
`../s31-adversarial-mutant-local-root.log`, FAILED28s, queuedX2.0 against
vanilla/physicalX2.2 after tick incorporation. The production file was not edited;
its SHA256 remained
014b7f60c46225e130cbcd41e5cf43c858972c9783345ff62ad43ec6cf01368f.
The generated mutant copy was removed from the verified own mutation directory.
The reusable mutation generator remains; preceding four distinct mutation kills
keep their stated earlier-candidate provenance.

After removing that copy, a single final combined lane executes all twenty S31
and all six current S30 holdouts against real production sources:
`../s31-adversarial-final-combined.log`, BUILD SUCCESSFUL2m9s and clean shutdown.
The final work measurement131072 queries is p95≈381.3ns/query,
p99≈391.2ns/query,120bytes/query. Independent lane variation demonstrates why
these local timings are not a whole-scene budget. Snapshot
`../s31-adversarial-dual-role-pig-incorporated-green.png` plus JSON was visually
inspected and kept separate from RED: vanilla, physical and original-model
submit coincide atX2.2 while the fractional residual is0. Captures represent
actual native-consumer geometry, not game pixels. Source hashes for eleven
changed/new production surfaces and exact lane identities are pinned in
`../s31-adversarial-final-sourcehashes.json`.

Final source review classifies carry/cursor and receiver event incorporation
as HOT_TICK/HOT_MOVE_QUERY/HOT_NETWORK, draw reads and local-root selection as
HOT_PRESENTATION. The relation is one unique local owner with a constant number
of frame/sample references; endpoint ingestion does not build an unbounded
queue. Consumers use local identity/observation checks, no scans over unrelated
supports, model extraction, preparation or reflection. The accepted-contact root
policy is restricted to entity_root and the current local prediction body;
accepted model/joint geometry remains authoritative. Its additional contact
lookup touches only that body/support and cached endpoint material, with bounded
record/Optional/Vec3 allocation, not a new per-scene evaluation. Gravity weak
observation keys do not retain entity values; owner observations are refreshed
unconditionally each client tick, including level loss. Query microbenchmarks do
not claim multi-thread lock contention coverage. The original-model preparation
and signed/delayed draw regressions stay GREEN. The superseded active-transport
boolean helper is bounded and no longer governs the productive root policy.

This final independent pass requires zero further productive changes and finds
no remaining functional S31/R1-R4 blocker within the local actor scope. Ordinary
server evidence separately inspected in the IMPLEMENTER lane reports463/463,
`../s31-server-postincorporation.log`, BUILD SUCCESSFUL1m. The IMPLEMENTER ordinary
client lane is still executing at this evidence checkpoint; it is not claimed
GREEN here. Formal R4/sprint registration, canonical status and logical commit
remain owned by IMPLEMENTER after its required ordinary client lane succeeds.
Full G4.6 is not closed: remote body provenance remains the explicit S32 boundary.
The evidence does not silently claim server contact discovery, an installed
external gravity packet route, native two-dispatch same-tick carry, all network
lifecycle axes or game-pixel QA beyond their individually documented scopes.

### IMPLEMENTER final closure

The dual-role repair keeps an accepted `entity_root` model on the vanilla
interpolation origin while its entity is the locally predicted body with a
confirmed contact. The accepted frame continues to supply the model transform
and joint pose. This rule lasts through natural incorporation of a physical
carry on the next tick; the body's fractional residual then retires to zero.
The unchanged Pig holdouts and six S30 model regressions pass in the adversary's
final 26-class combined run, with a directed mutant killed and no further
production edit requested. The local-only S31 acceptance claim is bounded by
the detailed adversarial limitations above.

On the final production tree, `python tools/s31_implementer_plan_model.py`
passed; targeted `S31ImplementerResidualClientProof` passed in
`../s31-implementer-postincorporation.log` (SHA-256
`7dc9d0b221e03bda4f39c4c664038cb7e04262880dcbfa9b02a7d1a5c96d5f9a`,
BUILD SUCCESSFUL1m3s). `build runGameTest` passed all **463/463** required
server GameTests in `../s31-server-postincorporation.log` (SHA-256
`9849a236ec9f861ee82c30015d8dbd3c55d9af4f0e1bf1f0056be7f6df193198`,
BUILD SUCCESSFUL1m). The complete ordinary `runClientGameTest` passed in
`../s31-ordinary-client-postincorporation.log` (SHA-256
`1d1719574118c4bce03b5e6ab24564b0cf3a15eb333468df69e0b4120e58bb66`,
BUILD SUCCESSFUL3m). The final production source hashes in the adversary's
combined lane remained unchanged; this closure changes documentation only.
S32 must establish remote observer incorporation provenance before G4.6 closes.
