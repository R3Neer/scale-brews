# S32 / G4.6c — remote observer carry provenance

Active role: **IMPLEMENTER** for the plan and production, **ADVERSARY** for
independent attack and verification. S31 local prediction was committed at
`6c662f59`. This sprint closes the remaining G4.6 remote path after the
independent final R1-R6 review below.

## Thesis and boundary

At the end of S32, a remote observer can draw a server-carried body against the
same accepted material endpoint as its contact, with a visual-only residual
that is removed exactly as vanilla interpolation incorporates that particular
server carry. This must be demonstrated through the real server tracking packet
and client interpolation paths, without inferring carry from an arbitrary body
position delta, packet adjacency, or a render query. FR-086..088 apply. No
physical position, AABB, velocity or gravity mutation is permitted on the
observer. S31 remains the sole locally controlled prediction route.

S31's `AnatomyTransportReceiptPayload` is for the controlling player and names
only a support serial. `AnatomyTransportReceipts.record` currently builds
histories only for a player body or passenger. A remote tracker receives
contact/support poses but no proof that its next vanilla body interpolation
contains a particular passive carry. Its residual therefore remains zero.
The new provenance must be server-issued at the actual carry and correlated to
an actual vanilla position packet for the **same remote body and recipient**.
Vanilla 26.2 `ServerEntity.sendChanges` can omit a position packet below its
threshold and can send a relative move or `ClientboundEntityPositionSyncPacket`;
the separate teleport packet is another hard-discontinuity path.
`ClientPacketListener` decodes those into `Entity.moveOrInterpolateTo` or
`snapTo` according to native tracking, distance and local authority.
A contact packet's tick and a receipt's arrival order alone do not certify that
the native interpolation has incorporated a carry.

## IMPLEMENTER plan, revised after adversarial PLAN RED

- [x] R1 Capture immutable server observer-carry evidence at the same physical
  write as the S31 transport receipt: body/support identity, epoch/catalog and
  tracking/binding generations, piece/face, physical material localPoint,
  exact before/after support frame serials, transport sequence, applied passive
  delta and body before/after position. Bound history per body/tick/recipient,
  fail closed on saturation or identity change, and never reapply this delta.
  The certified dispatcher path currently carries only the after serial in
  `AnchoredTransportPlanner.Evidence`; extend that immutable evidence from its
  source `MotionIntervalHandle.before/after` instead of reconstructing a before
  serial from the latest pose at send time. The non-dispatch endpoint carry
  path likewise uses its stamped `AnatomyContactState.Anchor` before serial.
- [x] R2 Intercept the **actual** vanilla body position packet sent to each
  tracking recipient, including relative-move and position-sync routes. Bind a
  server-issued observer attestation to that packet's decoded position target
  and the precise carry or serial-continuous carry bundle it incorporates.
  An accepted packet target alone does not prove that an earlier carry remains
  as a separable component: voluntary movement could offset it exactly.
  Attest only a decomposition established by the server's physical move
  provenance, or mark the packet unproven and publish no visual residual.
  The inspected 26.2 `ChunkMap.TrackedEntity.sendToTrackingPlayers` loops over
  `ServerPlayerConnection.send(packet)`; a composable per-recipient operation
  wrapper can replace an eligible send by one certified bundle to that exact
  recipient while forwarding the original packet exactly once inside it. Do
  not substitute `PlayerLookup.tracking` for the real send set.
  For an attested send, the wrapper must bind a fresh recipient/body/stream
  nonce to the **packet occurrence**, not just its decoded target. The proposed
  transport is one native `ClientboundBundlePacket` containing that exact
  vanilla position packet followed by its attestation. A client wrapper around
  `ClientPacketListener.handleBundlePacket` first inspects the two subpackets
  and opens an explicit scope for that native packet **object identity** and
  attestation nonce; the native position handler records its real transition
  only inside that scope. The bundle wrapper commits after the original bundle
  handler returns and verifies the same packet object, decoded codec base/
  target, kind and connection/tracking generation. It does not depend on a
  Fabric custom-payload callback being scheduled before or after the native
  handler. A repeated target, changed rotation or ABA position is a different
  occurrence. A missing, reordered, nested, duplicated or malformed bundle
  fails closed. The bundle encoding, packet order and handler scope still
  require an actual two-client integration proof before this can be treated
  as a valid implementation contract; if 26.2/Fabric cannot preserve them,
  the plan must be revised again before production.
  The server must retain the decoded native codec base per recipient and
  advance it only after each real position send. `ServerEntity`'s exact base
  is insufficient after a prior relative packet rounded another axis; the
  attested target must equal that recipient's decoded target. A catalog
  rebind of the same live entity must not reset this vanilla codec base.
  If vanilla sends no position update, no attestation is emitted and the remote
  residual remains zero. A separate teleport is a hard barrier. A passenger
  rotation packet, non-position metadata,
  failed send or tracker that never received the packet cannot certify carry.
  Establish packet ordering in a real client test rather than assuming the
  custom packet follows the native packet on the client main thread.
- [x] R3 On the client, maintain one bounded remote relation per actually
  tracked body, keyed by connection/level, UUID+network ID, tracking generation,
  support identity/binding/catalog, gravity and contact piece/face. Accept only
  certified support endpoint chains and an attested native position **event**
  within the same explicit occurrence scope; target equality is a secondary
  consistency check, never identity. Reject replay,
  duplicate, missing predecessor, old entity-ID reuse and cross-dimension
  attestations. Cap both per-body history and the total number of tracked body
  slots; expire idle or stopped-tracking state. Hold bounded out-of-order
  endpoint/contact/attestation material without allowing a render query to
  advance authority; retire at expiry.
- [x] R4 Measure native remote interpolation progress for that exact attested
  update from the actual `InterpolationHandler` state/transitions, including
  client ticks and render partial fraction. Derive the support-induced material
  displacement at the physical carry point and subtract **only the passive
  carry fraction already incorporated by vanilla**. Voluntary motion may share
  the native position packet; never equate its total displacement to passive
  carry. Keep a decomposed outstanding passive vector across **every actual
  native interpolation restart**. At a restart, freeze the old sample at the
  native breakpoint, transfer its still-unincorporated passive component into
  the new interpolation basis and add only newly attested carry material.
  For the exact-grid A=.75, one of three native steps leaves A=.5 pending;
  a new B=.75 makes the restarted basis carry A_pending+B=1.25 from the
  actual current body position .25 to target1.5. Rotation-only changed-yaw
  restarts transfer the same A_pending with B=0; identical target+yaw+pitch
  is a native no-op and must **not** restart the visual schedule. The client
  observes actual `InterpolationHandler` start/step/cancel and snap transitions,
  including target adjustment, rather than counting incoming packets. A new
  position packet without a matching causal attestation must either transfer
  only a previously proven outstanding component when native behavior proves
  its continuity, or retire to zero; it cannot claim new carry. Hard teleport,
  native baseline replacement, stopped tracking, contact clear,
  binding/catalog/gravity change and missing provenance retire the relevant
  correction. Duplicate presentation queries remain pure. Remote camera never
  consumes another body's correction.
- [x] R5 Compose the remote offset once in the existing renderer hook, with
  local/remote ownership mutually exclusive and original-model dual-role
  behavior preserved. The actual draw baseline must be explicit: for a body
  whose own accepted original model uses `ENTITY_ROOT`, anchor that model's
  root to the **same native interpolated render-state origin** used by the
  ordinary entity renderer throughout the confirmed contact lifetime, then
  add exactly one body residual. Its accepted frame still supplies model
  transform and joint pose. At the one-of-three-step example, native origin
  .25 plus residual .5 draws .75; the accepted own model origin .75 must not
  be added again. This contact-lifetime root rule extends S31's local dual-role
  rule to an observed remote body and remains stable after residual retirement.
  If an accepted original-model selection uses a custom root provider whose
  relationship to the body's native root is not certified, suppress that
  body's residual instead of adding it to an independent causal origin.
  Delayed original-model draw and adapter fallback must retain the same
  once-only baseline choice; validate the queued token before claiming a
  causal model root. Keep fail-closed zero in every unproven state. No legacy
  `PlatformVisuals` path may provide the same offset for anatomy-owned bodies.
- [x] R6 Verify real tracking over two clients/one server, no-position-update,
  relative move and teleport, ROOT and JOINT, voluntary motion, multiple
  carries, a remote body with its own `ENTITY_ROOT` original-model binding at
  fractional and post-incorporation draws, custom-root suppression and delayed
  draw fallback, packet ordering, latency/reorder/replay, late tracking/reconnect,
  lifecycle barriers, query-order invariance and physical-state identity.
  Run dedicated `allow-flight=false` at 0/100/200 ms as applicable, full
  server/client suites, bounded work/heap probe, independent mutations and
  native-consumer snapshots. Record only executed evidence in `VALIDATION.md`.

## IMPLEMENTER production and closeout

R1 records the physical Q2 `materialSerial`, exact before/after support frame
serials, local material point, applied delta, transport sequence and body position
at the actual passive write. The per-body history and recipient cursor are bounded
and keyed by object identity, with tracking and physical-generation barriers.
At the global history cap, the server now reclaims only expired/removed entries
before refusing a new body, so long-lived but idle entities cannot permanently
occupy all 512 slots. This liveness refinement has compiled and awaits bounded
history/soak verification.
The adversary's real physical carry holdout passed three ROOT transports and the
initial production tracking bundle (`../s32-adversarial-physical-wire-second.log`).

R2/R3 currently bundle the exact vanilla position packet and observer payload
per recipient, then validate one client handler event inside that decoded bundle
scope. The payload names the existing Q2 certificate; the client evaluates its
accepted endpoints at the physical local point before it enables any correction.
The server also advances that recipient's carry cursor at an unbundled native
teleport so old, unsent carries cannot be attached to a later position packet.
R4 keeps passive carry separate from voluntary native displacement and transfers
its outstanding component across observed interpolation restarts. A quiet-baseline
bug found by the adversary was repaired and the unchanged holdout passed
(`../s32-adversarial-quiet-retirement-green.log`). A second independent holdout
found that an unavailable Q2 certificate remained pending after its native
interpolation had fully settled, poisoning the next valid carry. The implementer
now retires that pending chain only at a proven quiet native baseline; the
unchanged recovery holdout passed (`../s32-adversarial-certificate-recovery-green.log`).
The adversary also found that a same-tick gravity-frame change remained visible
until `endTick`; the implementer added an immediate effective-gravity identity
check to the otherwise read-only render sample. The unchanged holdout passed,
including DOWN→UP→DOWN in one callback without reviving the residual
(`../s32-adversarial-gravity-barrier-green.log`).
The client support-material discard event now retires remote relations at once,
matching its existing local-residual barrier; this additional lifecycle change
still needs the final unchanged client battery.
An R5 holdout found that a queued original-model draw retained a remote offset
after a same-tick gravity barrier. The submit token now records the remote
residual sampled at extraction and the delayed draw rechecks that exact value;
if authority changed, it discards the stale draw. The unchanged holdout passed
(`../s32-adversarial-deferred-remote-barrier-green.log`); positive draw and
adapter fallback are being rechecked independently. R2-R5 still need the
remaining independent holdouts, latency, render-root and performance evidence.
The implementer review additionally found an equal-vector ABA window between
queued and current remote residuals. The token now checks the exact certified
packet nonce as well as the offset; this refinement has compiled and needs the
repeated draw/regression battery.
The same review found that an adapter fallback returned through the vanilla
draw before checking that token. The delayed wrapper now verifies remote
authority before either causal or fallback draw and discards a stale queued
draw in both cases; its unchanged fallback oracle remains pending.
The ordinary client suite passed before this last R5 change and is being repeated.
No S32/G4.6 acceptance is claimed
by this status note.

An R6 native-baseline holdout then found that `oldPosition` could be replaced
inside the same tick after an otherwise valid attested interpolation. The
remote presentation read checked the current position but retained a stale
passive offset against the new vanilla baseline. The client relation now
records both native positions at each accepted event/interpolation step;
the render read returns zero immediately on a changed `oldPosition`, and
`endTick` retires that relation. The unchanged public holdout passed in
`../s32-native-baseline-implementer-green.log`. Full ordinary and independent
adversarial repetition are pending; no global R6 closure is claimed.

A stronger R6 holdout restored both native position values after two
`setOldPosAndRot` calls in one callback; value comparison alone let the old
residual revive. The client now observes changed vanilla old-position writes
as monotonic events. A render sample requires the exact accepted event
generation as well as the native values, while the real interpolation step
accepts at most its expected one-step baseline update. The unchanged public
round-trip holdout passed in `../s32-native-baseline-roundtrip-implementer.log`.
Independent repetition of both baseline oracles passed in
`../s32-adversarial-native-baseline-roundtrip-green-second.log`, and the
post-change ordinary client battery passed in
`../s32-ordinary-client-s29-diagnostic.log` after one repeat of the known
S29 rejected-catalog intermittent assertion. The remaining R6 matrix and
final zero-change review are still open.

An R6 codec-axis holdout found that `ServerEntity`'s exact base cannot be used
to predict a later recipient's decoded native target after a relative packet
has quantized another axis. This was a production R2/R4 defect: exact packet
occurrence scope correctly rejected the mismatched payload, but also lost a
valid later passive X carry. The server cursor now retains the native decoded
base per connection/body, advances it after every actual position send, and
preserves it for a same-instance catalog rebind. An unprovable baseline fails
closed until an absolute sync. The unchanged public codec-axis oracle passed
in `../s32-codec-axis-implementer-green.log`; independent repetition and the
affected full battery are pending. The S33 placement document is preparatory
only; no G5 production work begins until S32 closes.

The adversary's unchanged codec-axis and two baseline oracles then passed
together in `../s32-codec-axis-adversary-green-retest.log`; its 13-case affected
receiver repetition passed in `../s32-adversary-private/codec-affected-retest.log`.
The post-fix `build runGameTest` passed 463/463 and the ordinary client suite
passed in `../s32-ordinary-client-after-codec.log`. Dedicated JOINT/latency,
mutation/performance and the final zero-change review remain open.

The expanded dedicated matrix then found an intermittent client-close crash:
presentation-contact removal during `ENTITY_UNLOAD` threw
`ConcurrentModificationException` after the mixed-voluntary positive checks.
The presentation-contact map now synchronizes its whole `removeIf` with its
other operations. `compileClientJava` passed; the adversary is repeating the
same dedicated disconnect and affected lifecycle oracles before this fix can
be accepted. Its separate bounded receiver-capacity/retention probe passed,
but neither that probe nor the compiler closes R6.

The unchanged mixed dedicated pair later closed cleanly in all three
post-repair repetitions; nine receiver and five public lifecycle lanes passed.
The ordinary client battery and 463/463 server GameTests also passed. A later
implementer review found two `CausalRemoteResidual` map-view copies without
their synchronized-map monitor. Both now take a detached key/value snapshot
under the monitor; compilation passed. Affected repetition on that exact
candidate passed the ordinary client suite. The remaining R6 fanout/network-
lifecycle matrix and final no-change review are still open.

Final closeout: the synchronized-snapshot candidate passed the complete client
suite, `build runGameTest` with 463/463 required server GameTests, 16-body/
two-recipient real dedicated traffic and observer teleport/dimension/reorder
fixtures. The adversary's independent R1-R6 review reconciled all prior
positive, negative, mutation, capacity, latency and native-consumer geometry
evidence with the current production manifest and found zero production drift
or unresolved RED. R1-R6 are complete within the measured limits recorded in
`VALIDATION.md`; the integrated NFR-014 benchmark and pack/manual/external-
model acceptance remain in G9 and the deferred epic-level QA ledger.

## Plan review questions before production

The 26.2 vanilla bytecode and current mod hooks must establish the exact
packet-send interception point and client native-interpolation observable.
The plan must be revised if a recipient-specific packet target cannot be
attested without inventing a second position channel. The adversarial model
should first attack packet omission, quantization, duplicated targets, native
interpolation restart and metadata arrival on either side of the vanilla
packet. Production begins only after the plan and adversarial model converge.

## ADVERSARY model and findings

Active role: **ADVERSARY**. Pre-implementation model constructed independently
from FR-086..088, NFR-007..014/NFR-027 and the remote-vanilla ownership boundary.
This is a plan review; no S32 production exists or was modified, and the model
is not yet a designed concrete production test battery.

### Native sources verified

Pinned 26.2 merged-deobf JAR SHA256:
9cc3373f015f79b7c48d406227edeb768262c144d2cc23884cb721f23e43332f.
Independent javap reads cover ServerEntity, its Synchronizer, ChunkMap.TrackedEntity,
ClientPacketListener, Entity and InterpolationHandler. ServerEntity.sendChanges
uses VecDeltaCodec relative Pos/PosRot or EntityPositionSync; actual recipient
fan-out is TrackedEntity.sendToTrackingPlayers/Filtered over seenBy and each
ServerPlayerConnection.send, with a distinct self path. Packet construction alone
cannot prove recipient delivery/incorporation. handleMoveEntity decodes against
that entity's position codec, updates its base and routes positional and rotation
branches separately. Rotation-only invokes Entity.moveOrInterpolateTo(float,float),
which retains InterpolationHandler.position() as the positional target.

PositionSync updates the codec and either calls moveOrInterpolateTo or snapTo:
native ticking-entity status and squared distance4096 distinguish those routes;
local authority is handled separately. A separate teleport is a distinct handler.
Entity.getInterpolation() is nullable; the generic method directly sets position
when no handler exists. InterpolationHandler defaults to3 steps. interpolateTo
returns without restarting only if an active target position AND yaw AND pitch
are equal; otherwise it resets steps to configured interpolationSteps and takes
current entity position as previousTickPosition. Each interpolate tick uses
1/remainingSteps then decreases the count. Native previous-position/rotation
adjustments may further change the target when collision checks allow them.
These facts support the amended position-sync/teleport distinction, but expose
the retirement and occurrence-correlation gaps below.

### Adversarial state and scaling model

Authority state must distinguish physical carry lineage, actual recipient native
packet occurrence, decoded target/base, accepted material-point domain, effective
native interpolation generation/progress and render incorporation. These are
separate identities. Equal positions do not imply equal updates, and a native
update does not imply a new carry. No observer physics write or camera consumption
is allowed; local prediction ownership remains exclusive. Queries cannot consume
pending authority, replay barriers or unproven fractional history.

Equivalence classes include no native positional send, relative Pos/PosRot,
position-sync interpolation, position-sync snap, separate teleport, absent/zero-
length interpolation handler, rotation-only updates, identical-target no-op,
changed-rotation same-target restart, consecutive positional restarts and native
external baseline/target edits. Identity classes include same-body generations,
new tracker versus existing tracker, replaced recipient connection, UUID/network
ID ABA, catalog/dimension/binding/gravity barriers and ownership round trips.
Material classes include ROOT/JOINT, quiet and moving endpoints, continuous
bundles, missing/interleaved/retired domains and changing contact point. Movement
classes separate actual passive applied delta from voluntary displacement in
the same packet, including cancellation and clipped carry.

Event permutations cover endpoint/contact/native packet/attestation before and
after one another, replay/duplicates/conflicts/expiry/saturation, late tracking,
reconnect, restart before a previous interpolation has completed and fraction
boundaries0/1 around ticks and incorporation. Metamorphic constraints include
query order/count invariance, no added correction under purely voluntary motion,
identical physical hashes around draw, zero remote camera contribution, and
preserving unincorporated passive components across legitimate native restarts.
Finite-value/domain limits and saturation must distinguish fail-closed outcomes.

Performance model: server capture is HOT_MOVE_QUERY/HOT_TICK; recipient fan-out
and decoding are HOT_NETWORK; remote sample reads are HOT_PRESENTATION; model
preparation remains PREPARATION. Scaling variables are tracked bodies B,
recipients O, carry rate M, material pieces P, history depth H, packet rate and
lifecycle churn. B*O actual network fan-out is inherent; pose/material evaluation
must remain shared rather than repeated per recipient, and each render lookup
must remain body-local with explicit TTL/caps on histories AND total body slots.
No target matching scan over unrelated bodies/recipients is acceptable. Snapshots,
mutations, counters and bounded work/heap evidence belong to later implementation
verification, not a claim made by this plan review.

### PLAN RED — R4 discards a still-unincorporated predecessor

Classification: plan causal-incorporation contract defect, not production.
R4 says a new native update retires the relevant correction; R2 attests the carry
or bundle incorporated by that particular packet. A legitimate new packet can
arrive before vanilla finishes the predecessor's passive component. Retirement
cannot discard that still-unincorporated component without breaking the thesis.

Reproducer with exact VecDeltaCodec-grid amounts: support ROOT carry A=3/4,
body starts0, native target3/4 with default3-step interpolation. After one tick
body/vanilla-at-partial1 is1/4 and A has1/2 still unincorporated. Server then
performs carry B=3/4, support material endpoint3/2, and sends its legitimate new
body target3/2. Native restarts from1/4. If the old correction is retired and the
new packet's B starts at progress0, the drawable body is1/4+3/4=1, not3/2:
1/2 of already certified carry is lost. A serial-continuous support chain alone
does not repair the client interpolation baseline. This counterexample contains
no voluntary movement, packet reordering, quantization error or missing proof.
The plan must specify how remaining passive predecessor contributions transfer
into the new native baseline/progress, or explicitly fail closed on an unproven
transition while still demonstrating ordinary consecutive-update functionality.

Rotation-only restart is a separate necessary member of that same boundary.
After A's first native tick at1/4, a Rot packet with changed yaw retains target3/4
but restarts3 interpolation steps through the actual vanilla route. The next
native position is5/12, leaving1/3 of A, even though Rot certifies no new carry.
Discarding the old correction loses that1/3; treating the restarted step count
as a fresh interpolation of the full A also attributes the wrong incorporated
fraction. Identical position AND rotation takes an early return instead, so a
packet-arrival counter cannot be assumed identical to a handler-start counter.

Reusable offline contract model `tools/s32_adversarial_plan_model.py` prints
these exact rational results; run `python tools/s32_adversarial_plan_model.py`.
Executed output: `../s32-adversarial-plan-counterexamples.json`. It models the
literal retirement clause plus independently verified native three-step laws;
it does not claim to run an absent S32 implementation.

### PLAN RED — R2/R3 target equality is not packet occurrence identity

Classification: plan attestation-correlation gap. R2 binds an attestation to a
decoded target and R3 accepts one whose target matches the observed native
update, but no shared recipient/body packet-occurrence contract is specified.
Distinct sends can have the same target: forced/no-motion position sends,
carry plus opposite voluntary displacement, quantization, ABA position return,
or changed-rotation/same-position updates. A delayed attestation for the older
actual send can match the newer target while its native predecessor/progress
belongs to another occurrence. Merely rejecting duplicate attestation IDs does
not correlate an independently observed native packet with that attestation.
Support/contact identities can remain identical throughout this ambiguity.

The plan must establish exact occurrence correlation (or a defined fail-closed
ambiguity policy), including position-codec base, native no-op versus restart,
late metadata and connection/tracking resets, without using decoded position or
arrival adjacency as the occurrence ID. No protocol implementation is prescribed
by this finding. The cited actual recipient send/handler routes demonstrate
where this proof must hold; no production is authorized yet.

Disposition: **PLAN RED**. Amend these contracts, then repeat the entire
independent plan pass. G4.6 and S32 remain open. The S31 closure is unaffected.

The subsequent IMPLEMENTER additions were read: the real recipient send wrapper
matches the independently inspected native fan-out, and rejecting unsupported
passive/voluntary decomposition is required. Neither addition specifies transfer
of a proven predecessor's remaining passive component across interpolation
restart, nor correlates two distinct equal-target packet occurrences. The
counterexample above uses pure passive motion and exact-grid positions, so the
new voluntary-cancellation guard does not remove that PLAN RED.

### Revised plan review: predecessor/occurrence contracts resolved, R5 PLAN RED

Active role: **ADVERSARY**. A new complete pass reads the revised R1-R6,
canonical ownership/requirements and the previously captured pinned26.2 native
routes. R4 now explicitly transfers the proven unincorporated predecessor at
actual handler restart, distinguishes changed-yaw Rot from no-op, and does not
mint carry for a rotation-only packet. This resolves the first plan model's
retirement counterexamples at the contract level. R2/R3 now identify the exact
packet object inside a nonce-bearing native bundle scope and treat target
matching as consistency only; this resolves target/occurrence ambiguity at the
contract level. Actual bundle encoding/two-client/native handler proof remains
an explicit pre-production verification dependency, not executed evidence.
The latest version supersedes the earlier begin/native/end proposal with
[native packet, attestation] inspected by the native bundle wrapper. Native
handleBundlePacket iterates subPackets and calls each Packet.handle(this) in
order after its same-thread check; the scoped-handler proposal is compatible
with that inspected structure. Per-body plus total-slot caps are now explicit.
No implementation or runtime success is inferred from these source checks.

Remaining classification: **PLAN R5 original-model incorporation contract gap**.
The remote actor may itself be a bound LivingEntity with an accepted original
model. That model's root can already draw the complete server-carried endpoint
while its ordinary entity position is still interpolating. The existing S31
root-origin exception applies only to isCurrentPredictionBody; it excludes a
remote actor. Merely adding the remote correction once in the renderer hook and
preserving existing dual-role behavior therefore does not prove once-only
incorporation in the actual final model draw.

Concrete pure-passive example, all amounts on the codec grid: start actor/root0,
server carry+.75, own accepted original-model origin+.75. The remote native
handler completes one of three steps, state.x=.25, so the vanilla-root body
residual would be+.5. Current CausalModelRender.applyRoot constructs desired
root from scope.base plus (acceptedOrigin-state.x). With remote rendererOffset
included once, base is.25+.5=.75; the accepted root substitution adds another
.75-.25=.5. Final queued original root is1.25 rather than.75. No repeated offset
call, packet ambiguity, voluntary motion or handler restart is involved: the
chosen consumer has already incorporated the passive translation before the
new correction. A plain body and an accepted CURRENT_ENDPOINT original model
therefore require explicitly distinguished draw-incorporation bases. Preserving
joint/model authority is a separate constraint from the root incorporation proof.

R5 must specify how the correction uses the actual consumer's incorporation
basis, including original-model root and delayed draw, instead of applying the
vanilla-body formula indiscriminately. It must also state the proven fallback
for incompatible/custom root providers and barriers. This finding does not
prescribe a production implementation; it demands a contract that excludes the
above duplicate incorporation. Include the corresponding original-model
remote actor in R6's native two-client acceptance frontier.

`python tools/s32_adversarial_plan_model.py` now also prints
remote_original_model_double_incorporation: native1/4, own accepted3/4,
residual1/2, existing model submit plus remote offset5/4, expected3/4.
Executed JSON retains the earlier historical contract examples as well; their
first two outputs are no longer allegations against the revised transfer and
occurrence clauses. This is a source-grounded plan counterexample, not a test
result for absent production. Only ADVERSARY docs/offline model changed.
Disposition remains **PLAN RED**, now solely on the root-consumer incorporation
contract; repeat the complete plan pass after its correction.

### Complete revised-plan pass GREEN — pre-implementation only

Active role: **ADVERSARY**. The entire revised R1-R6 plan was reviewed again
against the canonical FR-086..088 remote/local ownership boundary, boundedness
and cache requirements, the pinned26.2 send/handler/interpolation sources and
all earlier counterexamples. This pass requests **zero further plan or production
changes**. No S32 production has been implemented or validated by this result.

R1 now captures exact predecessor/successor material serials at immutable
MotionIntervalHandle evidence construction, with stamped Anchor provenance for
the separate endpoint route. It does not reconstruct history from a later send
pose. R2 uses the actual recipient connection send and a once-forwarded native
packet/attestation bundle with a scoped packet-object+nonce identity. R3 explicitly
bounds histories AND total body slots and rejects malformed/expired/foreign
material. R4 transfers proven remaining passive components at actual handler
restarts, distinguishes Rot changed-yaw from identical no-op and observes steps,
cancel/snap/target changes rather than inferring them from packet counts. R5 now
uses one shared native render origin for ENTITY_ROOT actor/model during accepted
contact, retaining accepted joints/model transform; incompatible independent
custom roots suppress unproven residual. Delayed submission/fallback must retain
that same incorporation choice. The earlier remote own-model duplication
counterexample is therefore excluded by the new contract. R6 includes the
relevant own-binding remote actor, post-incorporation draw and fallback routes.

Independent offline reference algebra covers720 combinations of signed/zero
first and second carry, old incorporation fraction, voluntary motion and new
progress fraction. Its reference draw is support carry total plus only the
native voluntary fraction; revised native basis+remaining passive correction
matches that reference, including the ENTITY_ROOT model basis. Executed output:
`../s32-adversarial-revised-plan-model.json` from
`python tools/s32_adversarial_plan_model.py`. Historical counterexample outputs
are explicitly labelled rejected clauses, not current-plan failures. The
IMPLEMENTER model was separately read and executed without edits and also passes
its carry-transfer, Rot/no-op, occurrence and own-model examples. Neither model
simulates encoding, actual interpolation callback timing or Minecraft game pixels.

The independent adversarial state/scaling model remains applicable in full.
Further runtime attacks must include packet processing before/after native body
oldPosition incorporation and render partials, not just physical breakpoint
fractions; exact physical carry point/domain changes; mixed/cancelled voluntary
motion; clipping/ordinary clear; handler absence/zero length; target adjustment;
query count/order invariance; local ownership round trips; and body/support/
connection/catalog/dimension/gravity barriers. Per-recipient/native occurrence
work must remain local, capped and separate from shared model preparation/pose
work. These are verification obligations for the future implementation, not new
holdouts designed against absent production and not alleged current defects.

Disposition: **PLAN GREEN — esto funciona, continúa**. This approves convergence
of the logical plan only. Its explicit real bundle encoding/handler-scope,
two-client/dedicated, native renderer/delayed draw, physical-state identity,
latency/bounded-work/heap, mutation and snapshot proofs remain required before
claiming runtime acceptance or closing S32/G4.6. The plan itself requires the
native bundle scope proof before relying on that implementation contract; a
failed spike must reopen planning. No runtime or final gate success is inferred
from this GREEN. Production and IMPLEMENTER model stayed untouched.

## ADVERSARY native occurrence spike — 2026-10-04

The native encoding/handler feasibility prerequisite is GREEN on pinned Minecraft
26.2. `S32AdversarialNativeBundleProof` first passed with an integrated server
(`../s32-adversarial-native-bundle-second.log`). The stronger paired proof then
used two Minecraft client processes with different native usernames/UUIDs,
connected by actual TCP to one dedicated server on port28232 with
allow-flight=false. Both final Gradle processes returned BUILD SUCCESSFUL and
closed cleanly: `../s32-adversary-pair-20261004-001658-592/host.log` (1m33s) and
`peer.log` (1m29s). Reproduce from this checkout with
`./tools/s32_adversarial_pair.ps1`; the launcher creates a fresh rendezvous and
separate host/peer build, run and project-cache directories.

Each real recipient observed five decoded occurrences with independent nonces:
relative Pos, changed-yaw Rot, identical-target Pos no-op, near position-sync
interpolation and far position-sync snap. The test-only bundle wrapper scopes
that first decoded Packet object identity, and observes exactly one real native
handler call before committing after the original bundle returns. Every observed
order was bundle-enter/native-enter/native-return/Fabric-payload/bundle-return.
Changed yaw restarts native steps; identical targets preserve existing steps;
near sync restarts from an in-flight position; far sync snaps position/oldPosition
without assuming the interpolation handler's previous target disappears. No
callback-order dependency is used for occurrence identity or commit.

Earlier reds were adversarial infrastructure only: nonexistent playS2C API
(correct API clientboundPlay), Loom argument provider overriding the selected
username, Fabric dedicated default max-players=1, and peer ending at a native
DisconnectedScreen instead of the runner-required title screen. Fixture/API
corrections and final clean rerun exclude these from production findings.

This proves the transport/handler mechanism, not S32 production acceptance:
packets/stamps are explicitly test-authored direct connection sends; no physical
carry attestation, recipient fanout production hook, latency/barrier acceptance,
remote renderer or performance conclusion follows. Test hooks are armed only
by selected test entrypoints and registered only through the adversarial init.
Production remained untouched. Independent private reference/holdout material
is prepared; runtime adapters await a delivered candidate. S32/G4.6 remain open.

## ADVERSARY physical origin and initial production wire — 2026-10-04

The independent `S32AdversarialPhysicalCarryProof` first passed the R1 physical
origin alone in `../s32-adversarial-physical-carry.log` (59s, clean shutdown).
It acquires a real small Cow/body contact on an independently prepared large
Cow/support via Entity.move, performs a voluntary move, then three actual ROOT
moves with signed passive deltas+.2/-.125/+.075 in one server callback. The
oracle captures the current body position and sealed physical material point
before each move, independently evaluates the material point after, and checks
ledger identity, binding, dimension, epoch, revision, piece/face/localPoint,
transport sequence, tick, actual body before/after, passive delta and unchanged
velocity. Voluntary movement does not create observer evidence. The exact
support chain was4->5->6->7; each carry appeared once and clear retired history
while the prior copied snapshot remained immutable.

The strengthened same oracle then observed the actual production
AnatomyObserverPacketMixin through a test-only read-only wrapper after the real
client native bundle handler. Final result:
`../s32-adversarial-physical-wire-second.log`, BUILD SUCCESSFUL1m9s with clean
shutdown. The real tracking send supplied one RELATIVE_POS native packet plus
one observer payload for the carried Cow. Its decoded target exactly matched
the body's native codec base. The payload retained the three physical carries,
transport sequences1/2/3, exact support chain4->5/5->6/6->7, Q2 materialSerials
4/5/6, signed deltas and each sealed localPoint/body before/body after. No record
was omitted or duplicated between ledger and real encoded wire bundle. This
uses the production tracking hook; it does not forge its attestation or manually
send a position packet. The observed native target was quantized relative to
the physical final position, and the oracle compared the actual decoded codec
base to the attested target rather than demanding unquantized equality.

An intermediate build red after materialSerial addition was classified
PRODUCTION COMPILATION: the endpoint call retained eight arguments against the
new nine-argument record method. Evidence `../s32-adversarial-physical-wire.log`.
IMPLEMENTER repaired it with a zero-materialSerial overload; the final rerun
above excludes that failure. ADVERSARY did not modify production.

Reproduce with runClientGameTest, selector
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S32AdversarialPhysicalCarryProof`,
`-I tools/s29_adversarial.init.gradle`, and distinct scalebrewsBuildDir,
s29AdversarialRunDir and --project-cache-dir as documented in the native spike.
This is GREEN for the exercised physical origin and initial production wire.
It is not remote residual acceptance: observer state/native interruption,
barriers/latency, two-recipient production attestations, draw/fallback, snapshots,
mutation resistance and scaling/heap evidence remain open. No S32/G4.6 closure.

## ADVERSARY R4 RED — stale correction after native incorporation (2026-10-04)

Classification: **PRODUCTION PRESENTATION / R4, FR-086..088**. The initial
receiver's independent native restart/no-op oracle found a positive residual
remaining after vanilla had completed interpolation and incorporated the next
quiet oldPosition baseline. The finding was reduced to the public
`S32AdversarialPhysicalCarryProof`, without rotation or replay.

Executed public reproduction `../s32-adversarial-quiet-retirement.log`:
BUILD FAILED1m9s on the final quiet-baseline assertion. All preceding R1 physical
and R2 wire assertions passed. The three real signed ROOT carries sum to+.15.
After five further client ticks, the fixture requires the actual native handler
inactive and body.oldPosition equal to body.position. The actual production
renderer offset at partial0 nevertheless remains (.050000000000000114,0,0),
where the independent native render baseline requires zero. This redraws an
already incorporated carry for partials below1. It is not a fixture timeout or
an unapplied packet: the actual tracking bundle was decoded and all three marks
had already matched the physical ledger.

Minimal command: runClientGameTest with
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S32AdversarialPhysicalCarryProof`
and `-I tools/s29_adversarial.init.gradle`, using distinct build/run/cache dirs.
Compiled receiver/interpolation class hashes are recorded separately in
`../s32-adversarial-quiet-retirement-class-hashes.json`. ADVERSARY modified only
tests/tooling/docs and returned the defect; no production repair or gate closure.
Other independently prepared holdouts remain private and unclaimed.

## ADVERSARY receiver follow-up RED — unavailable Q2 poisons recovery (2026-10-04)

The unchanged public quiet-baseline oracle passed the IMPLEMENTER repair:
`../s32-adversarial-quiet-retirement-green.log`, BUILD SUCCESSFUL54s, clean
shutdown, all five partials zero. The subsequent independent receiver campaign
found a distinct production R3/R4 recovery failure and repeated it after the
bounded pending queue was added. It was reduced to the public
`S32AdversarialCertificateRecoveryProof` and test-only receiver-boundary tap.

Reproduction: a real ROOT+.75 produces a native position+attestation bundle.
The fault changes only that first mark's materialSerial to an absent value;
the genuine native packet still executes exactly once. Its correction stays
zero as required. After four native ticks the body has incorporated that move
and has a quiet baseline. A second actual ROOT+.75 generates a fresh production
tracking bundle, nonce2/transportSequence2, with a valid Q2 material interval
and confirmed contact. The fixture independently requires both its Q2 interval
materializable and the contact present before reading any offset. Despite that
new independently valid evidence, the actual renderer-hook sum remains zero.

Executed public evidence `../s32-adversarial-certificate-recovery-red.log`:
BUILD FAILED1m11s; native tick15, steps3, oldPosition==position at x3.875;
new native target4.625; materialSerial17/support frames17->18; physical
appliedDelta+.75. At partial0, the independent remaining passive reference is
+.75 but observed offset is zero. The earlier missing certificate suppresses
the later valid sample. The same defect was independently repeated against
the current pending-queue version before publishing this reduced reproduction.
Classification: **PRODUCTION RECEIVER RECOVERY / R3/R4, FR-086..088**.

Minimal command: runClientGameTest with
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S32AdversarialCertificateRecoveryProof`
and `-I tools/s29_adversarial.init.gradle`, distinct build/run/cache dirs.
The test fault is opt-in through that selector; it is absent from production.
Tested receiver/scope class hashes: `../s32-adversarial-certificate-recovery-class-hashes.json`.
No production edits, permission-review failure, or gate closure. Other prepared
holdouts remain private and unclaimed until executed on the repaired candidate.

## ADVERSARY Q2 recovery repair — public regression GREEN (2026-10-04)

The unchanged `S32AdversarialCertificateRecoveryProof` now passes:
`../s32-adversarial-certificate-recovery-green.log`, BUILD SUCCESSFUL53s and
clean client/server shutdown. The first absent Q2 remains fail closed, then the
next genuine tracking bundle with independently materializable Q2/contact
produces the expected passive correction at all five render partials. This
closes that reduced RED only. The extended receiver/barrier campaign is still
being executed; no S32/G4.6 global closure or production edits are implied.

## ADVERSARY receiver campaign — executed scope (2026-10-04)

On the Q2-recovery repair, five fresh-world cases reached their independent
PASS assertions in `../s32-adversary-private/receiver-suite-green.log`:
changed-yaw Rot restart, identical Rot no-op, in-flight passive carry restart,
old nonce replay after native incorporation, absent Q2 rejection and valid Q2
recovery. Rot restart/no-op share one case. The run then stopped on a SNAP
fixture precondition that incorrectly observed the initial spawn sync as the
explicit far sync; this run is not claimed BUILD SUCCESSFUL.

After fixing only that fixture precondition, the two remaining cases passed
in `../s32-adversary-private/scope-snap.log`, BUILD SUCCESSFUL2m12s and clean
shutdown: actual far native position-sync snap retires the correction, and
reversing attestation/native order fails closed despite a matching target.
All cases queried five partials, repeated reads, invalid fractions and remote
camera ownership, with position/oldPosition/AABB/velocity identity checks.
These are numerical physical/native/render-hook checks, not visual snapshots
or deferred model submissions. Global R5/R6, latency and scaling gates remain
open. The independently prepared gravity barrier case is being reduced after
its runtime RED; unexecuted cases remain private.
## ADVERSARY gravity barrier RED — same-tick residual remains visible (2026-10-04)

Classification: **PRODUCTION PRESENTATION AUTHORITY / R3/R4, FR-086..088**.
The private gravity barrier failure was reduced to the public
`S32AdversarialGravityBarrierProof` plus a read-only native bundle tap.
Public execution `../s32-adversarial-gravity-barrier-red.log` is BUILD FAILED5m40s
at `Gravity barrier leaves remote residual visible in same tick`.

One genuine server ROOT+.75 carries the remote cow through the production
tracking bundle. Before the barrier, native tick9/steps3 has passive reference
+.75 and the actual renderer-hook sum equals (.75,0,0) at all five partials;
query purity, invalid fractions and remote camera ownership pass. In a single
subsequent client callback the development gravity-provider seam changes the
body's effective frame from DOWN to UP and `GravityFrames.observe(body)`
publishes the changed observation. Without any intervening native/client tick,
`CausalRemoteResidual.rendererOffset(body,.5)` is still nonzero. The contract
requires immediate retirement on this gravity barrier. The oracle also checks
no same-tick round-trip revival, but that second assertion is not reached on
this red and is not claimed independently demonstrated.

Minimal reproduction: runClientGameTest with
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S32AdversarialGravityBarrierProof`
and `-I tools/s29_adversarial.init.gradle`, separate build/run/cache dirs.
The test uses real physical contact/carry/native dispatch; no attestation is
forged and the network tap only observes after the original handler returns.
Gravity overrides are cleared in finally. Compiled receiver/scope/interpolation
hashes: `../s32-adversarial-gravity-barrier-class-hashes.json`.
No production edits, permission-review failure or S32/G4.6 closure.

## ADVERSARY gravity repair — unchanged public regression GREEN (2026-10-04)

`S32AdversarialGravityBarrierProof` passes without changing its RED oracle:
`../s32-adversarial-gravity-barrier-green.log`, BUILD SUCCESSFUL2m7s, clean
shutdown. The genuine passive residual is positive before the barrier, zero
immediately after the changed gravity observation, and stays zero after the
same callback restores the prior gravity frame. This proves the previously
unreached same-tick round-trip assertion as well as the reduced RED repair.
The affected receiver suite and independent support/burst extensions are now
being repeated; this result alone does not close S32/G4.6.

## ADVERSARY R3/R4 affected campaign — GREEN on gravity repair (2026-10-04)

The final repeated receiver suite completed all ten selected fresh-world
scenarios in `../s32-adversary-private/receiver-final.log`, BUILD SUCCESSFUL3m12s
and clean client/server shutdown. Executed selection: ROT_NOOP,
PASSIVE_RESTART, REPLAY, BAD_Q2, BAD_Q2_RECOVERY, SNAP, REORDERED_SCOPE,
GRAVITY_BARRIER, GRAVITY_SUPPORT, GRAVITY_BURST. The last three prove immediate
body/support gravity retirement and no revival after an observed body gravity
round trip without a renderer query between the changes. The suite invokes
the real native handlers and production carry/bundle path in every fresh world;
its Q2/scope faults are receiver-boundary test fixtures only.

All positive root samples matched the independent passive/native reference at
five partials. Repeated reads, invalid fractions, zero remote camera offset,
and unchanged physical position/oldPosition/AABB/velocity passed. The unchanged
public gravity regression separately passed as recorded above. Compiled
receiver/scope/interpolation/client-packet hashes are in
`../s32-adversarial-receiver-final-class-hashes.json`; their source timestamps
were checked after the final run and precede the tested compilation.

Reproduction of the completed extended suite uses the existing init selector,
private source flag and the ten-case selection above, with separate
s32-adversary-private build/run/cache directories. This is GREEN for the
executed initial R3/R4 frontier and both returned recovery/gravity REDs.
Two-recipient production residuals, required latency/lifecycle matrices,
R5 draw/fallback/model snapshots, scaling/heap and mutation evidence remain
open for S32/G4.6. No global closure, production edits or permission-review
failure is claimed. Unexecuted holdouts remain private.
## ADVERSARY R5 original-model draw — positive executed frontier (2026-10-04)

The independent remote dual-role fixture uses genuine physical ROOT+.75,
production recipient bundle and native interpolation on a remote Cow whose own
accepted original model uses ENTITY_ROOT. The actual living renderer extracts
its state and render offset, submits through SubmitNodeStorage, then the actual
ModelFeatureRenderer deferred consumer produces vertices. Vanilla joint inputs
are deliberately replaced before submit so accepted joint restoration is an
independent necessary condition. No production methods are replaced.

Positive run `../s32-adversary-private/draw-positive-second.log`:
BUILD SUCCESSFUL2m44s and clean shutdown. Across native ticks9..13/steps3..0,
all five partials match the independent queued root matrix and every retained
material corner appears in the actual delayed draw. The native X interpolates
3.125->3.875 and the residual decreases .75->0; the queued model root remains
3.875 exactly once, including the quiet post-incorporation tick. Physical
position/oldPosition/AABB/velocity are unchanged by the render reads/draws.

Deterministic native-consumer geometry snapshot: durable CSV
`../s32-adversary-private/remote-draw-positive.csv` (8000 rows), metadata
`../s32-adversary-private/remote-draw-snapshot.json`, and visually inspected
contact sheet `../s32-adversary-private/remote-draw-contact-sheet.png`.
The fixed projection overlays actual delayed-consumer vertices with independent
accepted-material corners shifted to the corrected native root for all25
samples; black vertices coincide with orange reference rings. CSV SHA256:
0acda32eae8cd823fe0e5443552a030986a398b24f8b36f016ebc987a96668a8.
PNG SHA256:c45a0e146d3007ae02775b42040bcc19efb6ae9b328675d391d6df195cef0b7c.
This is a geometry comparison from the native model consumer, not a full-game
pixel capture or a two-observer/latency acceptance claim.

The first private compile failure was test infrastructure: the generator cut
at an inner finally. Only that fixture reduction was repaired before the
executed positive run. A later independent submit/draw authority case is being
reduced after its runtime failure. R5/R6 remain open; no production edits.
## ADVERSARY R5 RED — deferred draw retains retired remote carry (2026-10-04)

Classification: **PRODUCTION DEFERRED PRESENTATION / R5, FR-086..088**.
Public reduced reproduction: `S32AdversarialDeferredRemoteBarrierProof` and its
read-only bundle observation tap. Execution evidence
`../s32-adversarial-deferred-remote-barrier-red.log`: BUILD FAILED2m27s on
`R5 queued remote draw retains retired passive carry after observed gravity barrier`.

A genuine ROOT+.75 and production bundle establish a positive remote residual.
The actual renderer submits the body's own ENTITY_ROOT model and its actual
ModelFeatureRenderer delayed vertices first pass the independent matrix/corner
reference. Between submit and another draw of that queued submit, the body
changes effective gravity DOWN->UP and publishes the changed observation.
The repaired current renderer sample immediately returns (0,0,0), checked
before the delayed draw. The queued draw nevertheless retains the old+.75:
actual native rootX=.875, old queued rootX=1.625, actual minimum vertexX=
1.493749976158142, zero-residual reference minimumX=.7437500059604645.
The retired passive difference is .7499999701976776. Both calls exercise the
real ModelFeatureRenderer; no empty or synthetic vertex buffer substitutes
for the positive control. The public oracle permits either dropping the stale
draw or recomposing it at the zero-residual native root, so it does not require
a particular repair strategy.

Minimal command: runClientGameTest with
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S32AdversarialDeferredRemoteBarrierProof`
and `-I tools/s29_adversarial.init.gradle`, distinct build/run/cache directories.
The first private delayed-barrier failure was reduced and independently repeated
by this public command. Tested class hashes:
`../s32-adversarial-deferred-remote-barrier-class-hashes.json`.
No production edits or permission-review failure. R5/R6 and G4.6 remain open;
other unexecuted holdouts remain private.

## ADVERSARY deferred causal draw repair — public regression GREEN (2026-10-04)

`S32AdversarialDeferredRemoteBarrierProof` passes without changing its reduced
RED oracle: `../s32-adversarial-deferred-remote-barrier-green.log`, BUILD
SUCCESSFUL1m55s and clean shutdown. Positive native/causal submit and actual
deferred vertices still pass; the queued causal draw no longer retains+.75
after the same-callback gravity barrier retires the current remote correction.
The independent full positive draw regression and actual adapter fallback
branch are being exercised next. This is acceptance of the reduced causal-draw
repair only; no R5/R6 or G4.6 closure is claimed.

## ADVERSARY R5 draw/fallback and bounded histories — GREEN frontier (2026-10-04)

The independent full draw and fallback repetition completed in
`../s32-adversary-private/draw-fallback-fifth.log`: BUILD SUCCESSFUL4m44s,
clean shutdown. The genuine ROOT carry and remote own ENTITY_ROOT model passed
all25 native-tick/partial combinations again, including actual deferred vertices,
causal joints despite poisoned vanilla joint inputs and unchanged physical state.
The previously recorded contact sheet remains the durable geometric comparison.

The fallback fixture exercises the actual 26.2 CowRenderer variant-model map:
temporarily replacing the selected AdultAndBabyModelPair with an original CowModel
whose body cube is widened makes the causal adapter reject the model. The actual
vanilla ModelFeatureRenderer then produces a nonempty positive fallback draw.
After a same-callback observed gravity barrier, the queued fallback no longer
retains the retired remote correction. The oracle allows either discarding the
stale draw or rebasing it without the old offset. Renderer model/map and gravity
overrides are restored in finally. Earlier fallback attempts failed fixture
preconditions because CowRenderer selects its own variant model at submit, and
one attempt inspected the wrong renderer superclass; they are test infrastructure
failures, not additional production findings. A transient compile during concurrent
production editing was superseded by this completed latest-source run.

Independent server storage-kernel probes completed:
`../s32-adversary-private/history-limits.log`, BUILD SUCCESSFUL2m38s, and
`../s32-adversary-private/history-dense.log`, BUILD SUCCESSFUL2m30s, both with
clean shutdown. They prove per-body64 acceptance/65 saturation, distinct Entity
objects with equal network IDs retaining separate UUID histories, local
invalidation, immutable retained snapshots, global512 acceptance/513 rejection,
and recovery of an expired full table. The dense probe strongly retains all512
body objects and fills each to64: peak retained entries32768, then one newly
recorded body/entry after actual server-clock expiry. The expiry wait observes
at least40 ticks; it does not claim an exact inclusive-tick boundary measurement.

These probes use a real integrated server, support binding and clock, but their
carry inputs are constructor-valid synthetic storage inputs. They establish the
retention bound and reclamation behavior; they do not substitute for physical
Q2 provenance, recipient dispatch, an allocation profile or steady TPS evidence.
Reproduction uses the private-source init selector with S32PrivateHistoryProof;
`-Ps32HistoryDense=true` selects the combined maximum. Draw/fallback uses the
S32PrivateDrawProof,S32PrivateFallbackProof selector and DRAW_POSITIVE case.
All lanes use separate adversarial build/run/cache directories.

Compiled production hashes for the completed draw/history frontier are preserved
in `../s32-adversarial-draw-history-green-class-hashes.json`. No production code,
IMPLEMENTER evidence or VALIDATION was edited by this review. No outstanding
defect was found in this executed frontier. S32/G4.6 remains open: the complete
custom-root/lifecycle matrix, two-recipient production residuals, dedicated
latency and voluntary-motion matrix, broader mutation and allocation/performance
evidence still require their own executions. Unexecuted holdouts remain private.

## ADVERSARY R6 RED — native oldPosition baseline replacement (2026-10-04)

Classification: **PRODUCTION PRESENTATION / R4-R6, FR-088**. An unexpected
replacement of the native interpolation baseline within the same tick leaves
the previously computed remote residual visible, despite R4 requiring immediate
retirement on native baseline replacement.

Private discovery `../s32-adversary-private/old-baseline.log`: BUILD FAILED2m45s.
Independent public reduction `S32AdversarialNativeBaselineProof` with its read-only
bundle tap repeats the same defect: `../s32-adversarial-native-baseline-red.log`,
BUILD FAILED2m39s at `R4 unexpected native oldPosition replacement retains remote residual`.
Both runs first establish genuine physical ROOT+.75, production tracking bundle,
accepted Q2/contact and positive residual equality at all five partials. After
one actual client interpolation tick, two steps remain. In one subsequent client
callback, the native `body.setOldPosAndRot()` replaces oldPosition.x=.875 with
the current position.x=1.125 without changing tickCount, current position, AABB
or velocity. The immediate renderer sample at partial=.5 remains (.625,0,0)
instead of failing closed to zero. No tick or packet intervenes between that
baseline replacement and the read. The network tap does not alter packets or
native dispatch. This is a deliberate native-baseline fault boundary; it does
not claim that ordinary interpolation ticks should retire a valid residual.

Minimal reproduction: runClientGameTest with
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S32AdversarialNativeBaselineProof`
and `-I tools/s29_adversarial.init.gradle`, separate build/run/cache directories.
Tested compiled receiver/scope/interpolation/fixture hashes are preserved in
`../s32-adversarial-native-baseline-class-hashes.json`.
No production edits. This blocks a GREEN R6 verdict; the unexecuted dedicated,
two-recipient, latency and remaining global acceptance matrix is still open.

## ADVERSARY native baseline repair — unchanged regression GREEN (2026-10-04)

S32AdversarialNativeBaselineProof passes without changing its RED oracle:
`../s32-adversarial-native-baseline-green.log`, BUILD SUCCESSFUL5m25s and clean
shutdown. Both positive five-partial controls still pass. The exact reduced
baseline replacement oldPosition.x=.875->1.125 with two active native steps
now returns (0,0,0) immediately in the same callback. This accepts that reduced
repair only; the next independent discontinuity and broader R6 matrix remain
under review. No production edits or S32/G4.6 closure.

## ADVERSARY R6 RED — same-tick native baseline round trip (2026-10-04)

Classification: **PRODUCTION PRESENTATION / R4-R6, FR-088**. The reduced
baseline comparison repair passes its original regression, but an independent
round trip restores the old value tuple and revives its remote relation.
Private discovery `../s32-adversary-private/baseline-aba.log`: BUILD FAILED5m42s.
Public independent reduction S32AdversarialNativeBaselineRoundTripProof with a
read-only native bundle tap: `../s32-adversarial-native-baseline-roundtrip-red.log`,
BUILD FAILED2m17s on `R4 native baseline round trip revives remote residual`.

The genuine ROOT+.75/control sequence and all five partials first pass. After
one native step, two remain. In one callback with no renderer query or tick
between transitions, setOldPosAndRot first replaces oldPosition with current
position. Then setPos(previousOld), setOldPosAndRot, setPos(originalCurrent)
restore both current position and oldPosition exactly. AABB and velocity are
also restored/unchanged. Those deliberate native baseline fault operations
do not dispatch a new packet or carry. The immediate renderer query returns
the old residual (.625,0,0) instead of zero after the discontinuity. This
checks retirement history, not ordinary native interpolation progress; merely
matching the previous position values cannot certify continuity after a reset.

Minimal reproduction: runClientGameTest with
`-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S32AdversarialNativeBaselineRoundTripProof`
and `-I tools/s29_adversarial.init.gradle`, own build/run/cache outputs.
Compiled receiver/interpolation/fixture hashes:
`../s32-adversarial-native-baseline-roundtrip-class-hashes.json`.
The original public baseline oracle remains unchanged and GREEN. No production
edits; R6/global S32 acceptance remains blocked by this independent RED.

## ADVERSARY baseline event repair and R6 intermediate frontier — GREEN (2026-10-04)

Both unchanged public baseline oracles pass together in
`../s32-adversarial-native-baseline-roundtrip-green-second.log`, BUILD SUCCESSFUL
5m46s, clean shutdown. Simple replacement and round trip return zero with two
native interpolation steps still active, while their positive controls pass.
The preceding grouped attempt failed only because the adversarial init selector
installed the round-trip tap but omitted the simple-baseline tap. The first
oracle passed; the second timed out awaiting its unobserved bundle. The selector
was repaired to install every selected tap and the full repetition above passed.
No production fix was made by the adversary.

Independent carry-cancellation/voluntary-return frontier:
`../s32-adversary-private/voluntary-cancel-return.log`, BUILD SUCCESSFUL5m56s,
clean shutdown. A genuine physical ROOT+.75 is cancelled by a voluntary-.75
before the observer receives an attestation. After five client ticks, offset
is zero. A later voluntary+.75 returns to the former carry's target without
another transport sequence. It receives no carry attestation and keeps zero
at all five partials, although native interpolation is active with three steps.
This proves the executed no-attestation cancellation/return case; absence of
an attestation alone is not asserted as an exhaustive native-packet omission
instrumentation result.

The real accepted-catalog custom-root case and all ten previously executed
receiver cases pass together in `../s32-adversary-private/custom-root-affected.log`,
BUILD SUCCESSFUL2m43s, clean shutdown. CUSTOM_ROOT registers an independent root
provider translated +3X, reloads it through canonical binding resources and
checks its actual accepted root-provider identity on support and body. Genuine
physical ROOT+.75 still produces the production carry bundle, but the body's
uncertified native/custom-root relationship suppresses its residual at every
partial throughout incorporation. Affected repetition includes ROT_NOOP,
PASSIVE_RESTART, REPLAY, BAD_Q2, BAD_Q2_RECOVERY, SNAP, REORDERED_SCOPE and the
three body/support/burst gravity cases. Physical-state/query/camera controls
remain unchanged. Compiled intermediate production hashes are preserved in
`../s32-adversarial-r6-intermediate-green-hashes.json`.

These are completed frontiers, not global R6 acceptance. The independent
dedicated two-recipient production carry/late-tracking/reconnect probe is now
being prepared/executed; no result is claimed for it here. Latency, JOINT,
full-suite, mutation and work/heap acceptance still require their own evidence.


### ADVERSARY R6 native codec zero-axis RED (2026-10-04)

Independent dedicated ROOT frontier is GREEN in
`../s32-adversary-private/production-pair-20261004-034432-196/{host,peer}.log`,
BUILD SUCCESSFUL55s/53s, clean shutdown. A real dedicated server on owned
port28432 (allow-flight=false) delivers four actual physical ROOT carries to
two native client processes; late join receives the incorporated spawn baseline
without old residual, subsequent shared carries match the native reference,
and reconnect advances the peer tracking generation8->13 with nonce1.
This is ROOT evidence only. A first draft sampled on wire receipt without
independently checking material readiness and failed; that draft does not
classify a production defect. Follow-up readiness controls were strengthened.
The separate proposed JOINT draft changed root yaw as well as head yaw and
therefore did not prove pure JOINT. Its root-lock replacement timed out before
receiving a carry; it remains unproven test infrastructure, not a production RED.

Classified production RED: R2 attestation does not describe the real recipient
native decoded target after a prior non-grid relative position update. Minimal
public reproduction: `S32AdversarialCodecAxisProof` and read-only bundle tap,
selected with the existing adversarial init. First actual support carry
Z+.12345 passes all five partials and incorporates to zero. After five quiet
ticks, an actual X+.25 carry arrives with accepted contact and both Q2 endpoint
queries ready. The attestation target is(1.125,9.254000186920166,5.24845), while
the native codec target is(1.125,9.254000186920166,5.24853515625). The Z delta of
the second native relative packet is zero. Vanilla preserves its actual base
on that axis; the server reconstruction uses ServerEntity.getPositionBase(),
which retains the raw coordinate. The scoped target comparison rejects the
occurrence. At partial0 and two remaining native steps, required residual
is(.25,0,-.00008515625); actual residual is zero. This loss is .25 in X,
not merely a tolerated quantization rounding discrepancy.

Private discovery: `../s32-adversary-private/codec-zero-axis.log`, FAILED1m58s.
Reduced public repetition: `../s32-codec-axis-adversary-red.log`, FAILED1m36s,
same values and positive/quiet controls. Test never injects a position packet,
attestation or physical carry; production tracking creates both bundles.
Renderer queries preserve position, oldPosition, bounds, velocity and remote
camera ownership. No production edits by ADVERSARY.

Reproduce with JAVA_HOME=ms-25.0.3:
`./gradlew.bat runClientGameTest -Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S32AdversarialCodecAxisProof -PscalebrewsBuildDir=../s29-adversary-build -Ps29AdversarialRunDir=../s29-adversary-run --project-cache-dir ../s29-adversary-cache -I tools/s29_adversarial.init.gradle --console=plain`.
Compiled class/log/artifact hashes: `../s32-codec-axis-adversary-evidence.json`.
Numeric native-consumer snapshot: `../s32-adversary-private/codec-axis-snapshot.json`
and `codec-axis-consumer-snapshot.png`; this is a deterministic comparison of
actual logged residual queries, not an in-game screenshot or vertex capture.
R6/S32/G4.6 remain open; latency, pure JOINT, final full-suite and work/heap
acceptance remain incomplete in addition to this blocking production defect.


### ADVERSARY recipient codec repair and affected battery GREEN (2026-10-04)

Original public CodecAxis, NativeBaseline and NativeBaselineRoundTrip oracles
pass together without changing assertions: `../s32-codec-axis-adversary-green-retest.log`,
BUILD SUCCESSFUL2m24s, clean shutdown. Off-grid Z then X now retains both
attested/native target consistency and the required passive visual component.
Independent receiver battery passes13 fresh-world cases in
`../s32-adversary-private/codec-affected-retest.log`, BUILD SUCCESSFUL7m4s:
CODEC_ZERO_AXIS, VOLUNTARY_CANCEL_RETURN, CUSTOM_ROOT, ROT_NOOP,
PASSIVE_RESTART, REPLAY, BAD_Q2, BAD_Q2_RECOVERY, SNAP, REORDERED_SCOPE,
GRAVITY_BARRIER, GRAVITY_SUPPORT and GRAVITY_BURST.

The concurrent dedicated draft did not prove JOINT: it failed before that
stage waiting for material readiness on the late peer. Startup also stalled
in native GLFW Vulkan availability detection, independently captured in
`../s32-adversary-private/slow-host-startup-thread.txt`. No production defect
is classified from that draft. Test-only diagnostic readiness logging and
explicit native26.2 `--graphicsBackend opengl` are used for its next repetition.
Global R6 and G4.6 remain open; no production edits by ADVERSARY.


### ADVERSARY dedicated ROOT/JOINT and nominal100ms RTT GREEN (2026-10-04)

`../s32-adversary-private/production-pair-20261004-044927-957/{host,peer}.log`,
BUILD SUCCESSFUL2m20s/2m19s, clean shutdown. A real dedicated server at owned
port28432 has allow-flight=false and two distinct native client processes.
The late peer's TCP stream traverses a localhost FIFO proxy at28433 with
nominal50ms delay in each direction (100ms RTT); host is the zero-delay control.
Proxy preserves native bytes, uses bounded64-chunk queues, counts two actual
connections and237931 bytes with maximum21 chunks. Shutdown reports Win10053
connection-close errors; both client lifecycle proofs complete cleanly.

Initial ROOT+.25 precedes peer tracking; shared ROOT+.75/-.25 and a genuine
vanilla head-yaw JOINT1 degree reach both recipients. Accepted Q2 endpoints
prove identical root origin/yaw and a changed material point, with physical
JOINT appliedDelta=(-.019805312156677246,0,.0194627046585083). Every partial
0/.25/.5/.75/1 matches independent bodyAfter minus native old/current root
within1e-3, with repeated-query/physics/camera controls. Quiet incorporation
returns zero; reconnect excludes old carry and advances generation8->13 with
fresh nonce1 for subsequent ROOT+.25. No forged certificate or position packet.

Earlier yaw15-degree variants (body scale.35 and.1) reached changed geometry
but lost physical contact and produced no JOINT transport. These drafts do
not prove a remote residual defect; they remain outside this proven1-degree
accepted-carry frontier. The small-body scale.1/one-degree variant guarantees
a footprint smaller than the horn face and an actual applied JOINT transport.
Nominal100ms denotes injected socket delay, not a claimed packet RTT profiler.
The200ms repetition is still pending; global R6/S32/G4.6 remain open.

Durable numeric native-consumer contact sheet and samples:
`../s32-adversary-private/joint-latency100-native-contact-sheet.png` and
`joint-latency100-native-snapshot.json`. These project actual queried residuals
against independent native-root references; no Minecraft pixel/vertex capture
is claimed. Artifact/log hashes: `joint-latency100-evidence.json`.


### ADVERSARY completed codec/JOINT/latency frontier handoff GREEN (2026-10-04)

The unchanged public codec/baseline oracles and13 receiver cases above stay
GREEN. Full ordinary build passes463/463 required server tests in
`../s32-adversary-final-ordinary-build.log`, BUILD SUCCESSFUL3m21s.
The complete ordinary client entrypoint list from the canonical test manifest
passes with clean shutdown in `../s32-adversary-final-ordinary-client.log`,
BUILD SUCCESSFUL4m2s. Explicit native26.2 OpenGL avoids the earlier availability
probe delay; this is test infrastructure, not a production renderer change.

The same dedicated tiny Cow/head-yaw1-degree fixture passes ROOT/JOINT,
late tracking and reconnect at all nominal socket profiles, allow-flight=false:

| Nominal RTT | Evidence directory | Host / peer | Result |
| --- | --- | --- | --- |
| 0ms | `../s32-adversary-private/production-pair-20261004-045627-393` | 2m27s / 2m26s | clean GREEN |
| 100ms | `../s32-adversary-private/production-pair-20261004-044927-957` | 2m20s / 2m19s | clean GREEN |
| 200ms | `../s32-adversary-private/production-pair-20261004-045312-506` | 1m53s / 1m52s | clean GREEN |

Two recipients use actual native packet/bundle dispatch and Q2/contact readiness,
with root origin/yaw equality independently checked for the JOINT carry. Native
partial0/.25/.5/.75/1, quiet incorporation and physical/query/camera invariants
pass. The100/200ms peer traverses the owned28433 FIFO proxy (50/100ms each way),
while the host remains the zero-delay control. The200ms report records two
connections,250369 bytes and maximum10 queued chunks; explicit reconnect uses
new generation8->13/nonce1 and carries no old residual. These are nominal injected
socket delays, not claims of exact measured RTT, jitter or arbitrary loss.
Reproduce with `../s32-adversary-private/production_pair.ps1 -DelayMs 0`,50 or100;
port28432 and own host/peer build/run/cache directories must be free. Completed
fixture sources/tooling and per-profile evidence hashes are in that private
adversarial directory; no unrevealed future cases are supplied in this handoff.

R5 draw/fallback repetition with positive assertions enabled passes:
`../s32-adversary-private/final-draw-fallback-positive.log`, BUILD SUCCESSFUL1m45s.
Actual deferred model vertices match accepted material at25 tick/partial samples;
original native-root correction occurs once, reaches zero after incorporation,
and stale fallback draw obeys the observed gravity barrier. Final durable CSV
and visually inspected contact sheet: `final-remote-draw-positive.csv` and
`final-remote-draw-contact-sheet.png`, with8000 actual/reference rows, ticks9..13,
fractions0/.25/.5/.75/1. This is native-consumer geometry capture, not a framebuffer
screenshot. The combined initial draw/fallback/history attempt failed only the
history fixture's explicit empty-global-storage precondition after other worlds.
It does not classify a production defect. Its initial draw lane was CAPTURE;
the subsequent DRAW_POSITIVE repetition above enables every scalar assertion.

The original dense-history oracle passes separately without changed assertions:
`../s32-adversary-private/final-history-dense-isolated.log`, BUILD SUCCESSFUL1m34s.
Identity/ID ABA, immutable snapshots,64/65 per-body and512/513 global limits,
combined32768 retained records and TTL recovery to1 retained record pass.
It exercises storage with constructor-valid synthetic inputs and real server/
binding/clock; it does not certify their physical/wire provenance or claim a
long network soak.

Independent private source-copy mutations are killed by their exact oracles:
- Raw ServerEntity codec base instead of recipient native base:
  `mutation-raw-codec.log`, FAILED3m25s at the original CODEC_ZERO_AXIS .25X loss.
- Skip the immediate renderer gravity-generation guard:
  `mutation-gravity-guard.log`, FAILED2m43s at gravity round-trip revival.
Both failures occur at semantic assertions after positive controls; neither is
a compiler/mixin failure. Nominal production was never altered by ADVERSARY.

Bounded work/heap smoke probe `S32PrivateRemoteWorkProof` passes actual remote
ROOT carry and original deferred draws with an observational HierarchyMotion
constructor counter in a private source copy. `remote-work-heap.log`, BUILD
SUCCESSFUL2m17s:8192 same-tick remote-offset/presentationFrame query pairs,
0 repeated motion preparations,6849608 thread-allocated bytes,35351100ns measured
loop, heap-used300067552->306359008 bytes. Identical results, position, oldPosition,
bounds and velocity survive all reads. The allocation guard is2KiB/query pair.
This measurement includes both public query APIs, uses no timing assertion,
and is not a render FPS/TPS benchmark or a long-running retained-heap claim.
Structural review: recipient native-base lookup remains one body-local map
lookup; HOT_NETWORK history/packet work is capped64 records/16 marks and client
pending work8 packets per body/512 total (128 marks per body maximum). Hot
presentation query does not rebuild motion invariants. Client endTick and support
retirement remain intentionally proportional to active residual relations.

Final source/ordinary-class fingerprints: `final-production-hashes.json`.
Source comparison after all executed tests shows zero production drift.
Logs/artifacts: `final-round-evidence.json`; per-profile hashes accompany latency
records. ADVERSARY changed only its test/tooling/evidence surfaces and this record;
IMPLEMENTER documentation and VALIDATION remain untouched.

Verdict for this requested frontier: **GREEN — funciona, continúa**. No new
classified production defect remains in this repetition. This does not close
R6/S32/G4.6 globally: the measured work/heap smoke and logical maxima do not
substitute for broader multi-body/fanout stress, client pending/watermark capacity
and retained-heap soak, or an expanded mixed-voluntary/teleport/dimension/reorder
matrix through the real dedicated connection. Earlier15-degree rotation drafts
remain physical-contact precondition failures, not positive JOINT coverage.
The one-degree accepted JOINT, nominal0/100/200ms, full ordinary suites and R5
native-consumer snapshots are the completed evidence of this handoff.

### ADVERSARY receiver capacity and mixed dedicated frontier (2026-10-04)

Cold receiver-kernel holdout `S32PrivateReceiverCapacityProof` passes unchanged
production: `../s32-adversary-private/receiver-capacity.log`, BUILD SUCCESSFUL
1m27s; extended explicit-unload retention repetition
`receiver-capacity-retention.log`, BUILD SUCCESSFUL2m35s and clean shutdown.
Constructor-valid synthetic NativeEvent/payload inputs exercise actual public
receiver storage, with a real client level and tracked support. This is storage
and lifecycle evidence, not native handler/wire provenance for those inputs.

Independent assertions cover equal network-ID/different-object ABA,8/9 pending
packets per body,16 carry marks each,512/513 global pending packets,
4096/4097 replay watermark bodies, rejection replay, release/recovery after
retirement and explicit unload, plus40 full-capacity drain cycles. Uncertified
inputs always produce zero drawable residual. All map/count assertions return
to zero after explicit unload.6699 observed owned Cow objects leave zero live
WeakReferences after four explicit GC passes separated by client ticks; measured
heap-used398029328 before cleanup ->277234960 after. This is a bounded object
retention/churn probe, not an FPS/TPS result or an arbitrary-duration heap soak.
Own fixture remains outside the repository under `../s32-adversary-private/java`;
selector/init/build/run/cache are the established private lane.

Dedicated two-client mixed-voluntary repetition checks the passive.75 carry
alone, against native interpolation steps/fraction, after a genuine.03125 walk
that retains contact. Five fractions match.75,.6875,.625,.5625,.5 at the first
sample; ROOT/JOINT, quiet incorporation, late tracking and reconnect also pass.
However, the first run has an intermittent **RED production lifecycle crash**
when its host closes the native dedicated connection:
`../s32-adversary-private/production-pair-20261004-053110-237/host.log:314-340`,
ConcurrentModificationException in `AnatomyClientNetworking.discardSupportMaterial`
line155, presentationContacts removal from ENTITY_UNLOAD -> retireFrameHistory ->
ClientPacketListener.clearLevel -> Minecraft.disconnect. All positive semantic
checks completed before the crash. Peer finishes green. An unchanged repetition
`production-pair-20261004-053551-773` passes both roles (2m13/2m11). Observed
frequency1/2, exact cause not claimed. Reproduce with the established private
`production_pair.ps1 -DelayMs 0`; this run uses nominal production, no source copy
or mutation. The reproducible route/log was returned to IMPLEMENTER; R6 cannot
close with the observed lifecycle crash unresolved.

Own reusable tools corrected: pair launcher accepts configured JAVA_HOME/PATH;
removed its absolute JDK path. Removed duplicated OpenGL doFirst from generated
resources; only JavaExec owns the launch hook. Affected public selector
S32AdversarialNativeBundleProof passes with s32AdversarialOpenGL=true:
`tool-native-selector.log`, BUILD SUCCESSFUL1m29s and clean shutdown. Prior
executed evidence is preserved. Production and IMPLEMENTER/VALIDATION sections
are not modified by this record. Multi-body/fanout, expanded native dedicated
teleport/dimension/reorder and longer retained-heap network soak remain open.

### ADVERSARY contact retirement synchronization revalidation GREEN (2026-10-04)

IMPLEMENTER replaced presentationContacts with Collections.synchronizedMap.
The original dedicated mixed fixture and all receiver/public assertions remain
unchanged. Its source SHA256 still equals the pre-repair RED oracle:
fe4aeeed0bcf479f9c100a0dedb3907c64178e8d5535f42c461a02805f4eaf37.
Three sequential zero-delay repetitions of production_pair.ps1 -DelayMs0 pass
both actual recipient clients, with clean native connection/server shutdown:

| Owned evidence directory | Host / peer | Result |
| --- | --- | --- |
| production-pair-20261004-054252-659 | 2m31s / 2m30s | GREEN |
| production-pair-20261004-054549-634 | 3m59s / 3m57s | GREEN |
| production-pair-20261004-054951-895 | 3m5s / 3m3s | GREEN |

All paths are beneath ../s32-adversary-private. ROOT/passive-only correction after
voluntary motion, JOINT with stable support root, late tracking, reconnect with
body generation8->13, nonce restart and five partial samples pass; allow-flight
remains false. The originally observed presentationContacts removal CME does
not recur in these3/3 post-repair repetitions. This is bounded empirical
revalidation of that RED route, not a proof against every thread schedule.

Nine independent unchanged receiver lifecycle lanes pass with clean shutdown:
OLD_BASELINE, BASELINE_ABA, REPLAY, BAD_Q2_RECOVERY, SNAP, GRAVITY_BARRIER,
GRAVITY_SUPPORT, GRAVITY_BURST and REORDERED_SCOPE.
contact-sync-receiver-lifecycle.log: BUILD SUCCESSFUL4m43s. Five unchanged public
lifecycle oracles pass together: S29AdversarialCatalogRollbackProof,
S29AdversarialCatalogDimensionProof, S29AdversarialUnavailableReplayProof,
S29AdversarialLifecycleOrderingProof and S29AdversarialCrossSupportProof.
contact-sync-catalog-lifecycle.log: BUILD SUCCESSFUL2m45s and clean shutdown.
The deliberate invalid catalog logs its expected rejection, then old certified
material and physical authority recover. Active and early A-B-A dimension
round-trips, revision replacement, early-certificate lifecycle retention and
cross-support isolation all assert GREEN.

Candidate AnatomyClientNetworking.java SHA256 remained unchanged throughout
these executions: fb41b4b0f52386087601564593a226f5a5430b4a57853eda288844a61fc52aa8.
Evidence hashes and source/fixture checks: contact-sync-frontier-evidence.json.
No production edit or oracle relaxation by ADVERSARY; no implementation-facing
record or VALIDATION changed. No unresolved RED remains in this requested
repair repetition. Verdict **GREEN — funciona, continúa**, limited to the
observed contact-retirement crash and the tested lifecycle cases. Earlier
capacity/retention evidence stays separate; full ordinary recertification of
the new candidate, broader multi-body/fanout/network heap soak and the expanded
S32-native dedicated teleport/dimension/reorder matrix remain required before
any global R6/S32/G4.6 closure. The S29 dimension proof is not silently promoted
to S32 remote-residual dedicated transition evidence.


### ADVERSARY dedicated fanout, retention and observer transitions GREEN (2026-10-04)

Active role: **ADVERSARY**. The requested functional frontier uses local Minecraft
26.2 fixtures, actual dedicated server and two recipient clients, with
allow-flight=false and owned build/run/cache directories. No production changes.
Both source and all four compiled receiver classes identify the post-snapshot
candidate: CausalRemoteResidual.java SHA256
cd65cc6cadec2801e690b6ea198010f40491538d009a93ca0fdc0c6715e077a5;
compiled class SHA256
5e43aa5dea0a88bb9737d2a34b39903ece8d6870234a16d8a4cbbd083c49e80b.
AnatomyClientNetworking remains the contact-sync candidate
fb41b4b0f52386087601564593a226f5a5430b4a57853eda288844a61fc52aa8.
Full source comparison with the earlier pre-sync manifest has exactly these two
expected changed files. Manifest/log/fixture hashes are preserved in
../s32-adversary-private/fanout-observer-transitions-evidence.json.

Private S32PrivateProductionFanoutProof passes 16 simultaneous supported bodies
on 16 supports, two real recipient connections and 128 physical ROOT rounds:
2048 actual certified native packet occurrences per recipient, 4096 total.
Every round asserts server applied carry and body displacement, packet/body/
sequence identity, no duplication, positive receiver correction at five partial
fractions, query repeat invariance, zero camera ownership and physical position,
oldPosition, bounds and velocity identity. Receiver relations/watermarks remain
bounded by16; no pending packet remains at sampled checkpoints. Explicit
connection retirement returns the owned receiver storage to zero.

Four cold explicit-GC heap observations, rounds32/64/96/128, are:

| Recipient | Heap-used bytes |
| --- | --- |
| Host | 331708840 / 331597712 / 331592760 / 331597136 |
| Peer | 241012304 / 241109688 / 241120544 / 241166064 |

Host quiet checkpoints have zero active relations; peer active checkpoints have
16; both retain16 watermarks and zero pending packets. The fixture stores only
16 last payloads plus16 counters. This is normal dedicated traffic over roughly
one minute of actual rounds, with bounded post-GC retained heap, not an
arbitrary-duration soak or FPS/TPS/latency claim. Earlier capacity/churn kernel
results remain separate evidence for maxima. Logs beneath
../s32-adversary-private/production-fanout-20261004-061646-966:
host.log BUILD SUCCESSFUL3m9s, peer.log BUILD SUCCESSFUL3m8s, clean shutdown.
Own command: ../s32-adversary-private/production_fanout.ps1 -DelayMs0.

Private S32PrivateProductionTransitionsProof passes on the same compiled
candidate, port28442, with actual ServerPlayer teleport to a distant Overworld
position and return, followed by Nether/Overworld dimension round-trip on the
same socket. The observed body unloads, its old object returns zero residual,
and return creates a new object and strictly higher tracking generation.
Tracking generations8->12->16 and nonce1 on both fresh baselines are asserted;
old carries do not replay on spawn, new ROOT carries remain positive at all five
fractions. The host continues to receive physical carry while the observer is
absent. Two physical carries in one native occurrence are checked on both
recipients, followed by a separate native entity-teleport barrier that returns
zero immediately inside the handler and then recovers fresh positive material.
The separate entity-teleport packet is injected through the actual tracking
sender at an already aligned codec base; it is not claimed to be a naturally
emitted Cow teleport packet.

Fault checks in that same dedicated session reverse the two decoded bundle
parts before the real handler (fail-closed zero, then positive recovery). A
constructor-valid stale pre-barrier attestation, with current native target but
old tracking identity, crosses the actual recipient socket beside a real
rotation packet; it stays zero at five fractions after both returns. This is an
explicit stale/cross-recipient fault input, not an unmodified packet replay.
Logs beneath ../s32-adversary-private/production-transitions-20261004-063225-246:
host.log BUILD SUCCESSFUL2m23s, peer.log BUILD SUCCESSFUL2m21s, clean shutdown.
Own command: ../s32-adversary-private/production_transitions.ps1 -DelayMs0.

Fixture classification: earlier fanout drafts lacked stable physical contact
for five bodies during late peer startup; their failed preconditions are not
production RED or positive coverage. Test-only yaw stabilization makes the
ROOT scene deterministic without changing its transport or residual assertions.
An earlier transition draft manually sent an absolute body teleport after an
unsent movement without updating vanilla ServerEntity's relative codec base.
That invalid native stream caused a timeout; correcting the fixture to send the
barrier at the already tracked position restores native alignment, with the
same immediate-zero and fresh-positive assertions. The subsequent distant
observer teleport uses the actual ServerPlayer transition path. A sandboxed
launch failed before Minecraft due to JDK java.security read access; the
unsandboxed authorized repetition executed normally. No automatic permission
review rejection occurred.

Verdict: **GREEN — funciona, continúa**, for the requested simultaneous-body,
normal dedicated retained-memory and observer teleport/dimension frontier.
No classified production RED remains in these executions. This record does not
silently close R6/S32/G4.6: final ordinary suites on the post-snapshot candidate
and the complete gate review still need their own current evidence. Earlier
0/100/200ms and native-consumer visual evidence retain their stated versions and
limits; no latency proxy or new visual capture was used in this session.


### ADVERSARY final R1-R6 no-change review GREEN (2026-10-04)

Active role: **ADVERSARY**. Independent final pass re-read current R1-R6,
FR-086..088, canonical G4.6 boundary, sprint workflow and adversarial performance
protocol. Reviewed the current server carry capture, recipient-specific native
send/codec cursor, payload bounds, exact bundle scope, native handlers, actual
interpolation/baseline hooks, client relation/Q2/contact/gravity lifecycle and
once-only/deferred original-model root composition. No new production defect or
unexplained hot-path regression found. No production, IMPLEMENTER section or
VALIDATION edit; only this adversarial execution record and evidence index.

| Criterion | Completed independent evidence reconciled in this pass |
| --- | --- |
| R1 | Genuine physical ROOT/JOINT carry, exact Evidence/Anchor serial provenance, immutable ledger, identity reuse, 64/65 and512/513 limits and TTL recovery. |
| R2 | Actual recipient-specific native bundle/handler, native codec rounding and zero-axis regression, absent/zero position, voluntary cancellation/carry separation, multiple physical carries and late-recipient baseline. |
| R3 | Q2 acceptance/recovery, replay/ABA/object identity, bounded pending/watermarks and explicit retirement, catalog/gravity/contact barriers, dedicated tracking/reconnect and observer teleport/dimension. |
| R4 | Five native tick/partial samples, passive restart transfer, rotation restart versus no-op, snap/teleport zero, oldPosition ABA barrier and pure renderer queries. |
| R5 | ENTITY_ROOT dual-role native root plus one residual, post-incorporation zero, causal joints, custom-root suppression, actual deferred draw and stale fallback/nonce barriers. |
| R6 | Two real clients/dedicated allow-flight=false, ROOT/JOINT/mixed voluntary, nominal0/100/200ms, reorder/replay, lifecycle,16-body fanout/real traffic retention, dense limits, work counter, semantic mutation kills and native-consumer visual comparison. |

Independently verified current ordinary logs against their recorded SHA256:
../s32-client-after-snapshot-fix.log
79cbebf6d1ff08d71e3ac492428c75fe57d64b90924e705b43a03596a5bed9da,
BUILD SUCCESSFUL4m15s with clean completion; and
../s32-build-after-snapshot-fix.log
e98b846d7c2477d9f9041c71af8f97cfbf27bb364a0df832b26f66a66d3d1af3,
BUILD SUCCESSFUL2m19s, all463 required GameTests passed. These are inspected
IMPLEMENTER executions, not newly claimed ADVERSARY reruns. Independent
adversarial executions are the already completed unchanged oracles documented
above. All hashes in final-round-evidence.json still match, including positive
controls, draw CSV/contact sheet, both semantic mutation failures and work probe.
The native-consumer contact sheet was visually inspected again: black actual
vertices remain centered in orange reference rings across all25 tick/partials,
including final incorporation. This remains geometry capture, not a framebuffer
screenshot or resource-pack/model compatibility certification.

Current full production-source manifest equals the fanout/transition candidate
manifest exactly: zero added, removed or changed production files during this
final pass. CausalRemoteResidual source remains cd65cc6cadec2801e690b6ea198010f40491538d009a93ca0fdc0c6715e077a5.
Its four dedicated client class fingerprints agree. New durable final index:
../s32-adversary-private/s32-final-zero-change-review.json.

Verdict: **GREEN — funciona, continúa**. The complete requested S32/R1-R6 pass
produces zero production changes and no unresolved classified RED. Independent
review now supports closing S32 and G4.6, together with the previously closed
S30 causal renderer and S31 local residual/camera. Checkbox/canonical gate
updates remain IMPLEMENTER-owned. This acceptance does not claim all thread
schedules, arbitrary-duration heap soak, FPS/TPS or the integrated normative
NFR-014 benchmark (G9), nor the unrelated full-pack/manual external-model QA.
The earlier open-frontier notes are historical boundaries; fanout/retention,
observer transitions, current ordinary suites and final review are now accounted
for by the subsequent entries rather than silently omitted.
