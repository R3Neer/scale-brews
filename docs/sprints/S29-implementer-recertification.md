# S29 — implementer recertification of certified presentation intervals

Active role: **IMPLEMENTER**. ADVERSARY owns independent probes and holdouts.

## Scope and initial state

Continue G4.5, FR-086 and its causal/lifecycle/boundedness prerequisites
FR-037, FR-082, NFR-004, NFR-007, NFR-011, NFR-017 and NFR-018.
The candidate pulled at `a54031b9` already provides a server-issued interval
certificate, bounded exact endpoint retention and `presentationInterval`.
The canonical gate remains open; earlier capability-absence evidence describes
the historical baseline, not this candidate. G4.6 residual/camera is excluded.

The current `origin/main` (`b5e62535`) was inspected before planning. This work
does not require a merge or changes to mount/input/render/product mechanics.
S00 and G1-G3 are recorded closed; this is recertification of the existing S29
candidate, not a new foundation or feature sprint.

## Plan — IMPLEMENTER

- [x] I1 Reconstruct candidate ownership and run the existing build/server suite.
- [x] I2 Repair classified adversarial replay/TTL defect: expiration must remove
  material without removing lifecycle/serial rejection authority.
- [x] I3 Re-run the adversary's unchanged reproducible probe after repair.
- [x] I4 Repair any additional classified candidate defects in the same scope;
  review each repair against canonical requirements before editing.
- [x] I5 Verify build/server and affected real-client evidence; record executed
  results and remaining limitations without claiming G4.5 closed prematurely.
- [x] I6 Complete implementer diff review and independent adversarial handoff.

## Plan convergence and failure classification

Initial review: scope is one causal presentation primitive. Existing physics,
pose/catalog and observer ownership remain authoritative; no renderer-derived
authority or camera residual is introduced.

2026-10-02: ADVERSARY reproduced an expired certificate being accepted again by
`AnatomyCertifiedIntervalInbox` because pruning removed its entire support state.
Classification: **implementation bug**, NFR-011. Repair preserves a bounded
per-support lifecycle fence and expired-material watermark after pruning.
Newer binding/tracking lifecycle can replace that state; stale lifecycle cannot.
This fence is connection-owned and cleared by existing explicit session barriers.
Full-plan review after this classification produced no further scope changes.

ADVERSARY then reproduced two real same-tick root movements whose certificates
named an intermediate endpoint absent from the ordinary pose stream.
Classification: **implementation bug**, FR-086. The certificate sender must
publish both immutable endpoints from the accepted Q2 handle via the existing
pose schema, with the same recipient tracking generation, before its identity
certificate. A tick-current publication cannot substitute for either endpoint.
No new geometry/pose DTO or client authority is introduced. Plan re-review
against lifecycle, exact identity and wire ownership produced no scope change.

IMPLEMENTER development regression `S29ImplementerEndpointRetentionTests`
reproduced three additional retention defects: old endpoints survive an
unavailable/recovery barrier, a gravity round-trip, and their historical TTL
while the support continues publishing. Classification: **implementation bugs**,
FR-082 / NFR-011 / NFR-017. Plan extension within I4: retain receipt times alongside
exact packets, prune historical material after 100 client ticks, and cut retained
material at unavailable/gravity barriers. Current endpoint/order authority remains
independent and is not rewound by pruning. Review converged without new scope.

Final IMPLEMENTER receiver review found another ordering defect: a certificate
for a new binding/tracking window can arrive before its first endpoint, but the
old history's RESTART cleanup unconditionally dropped that staged certificate.
Classification: **implementation bug**, FR-080/FR-086 and S29 ordering acceptance.
The restart now retains only the inbox state matching the incoming exact lifecycle,
including its expiry/saturation fences; all old geometry/contact/cache state is
still discarded. Unavailable and connection barriers continue to discard material.
The development regression covers both preservation of an exact staged lifecycle
and rejection of a different lifecycle. Plan re-review produced no new scope.

ADVERSARY's real support-chain proof then showed a genuine 0.2-block child carry
with an advanced endpoint but no presentation certificate. Classification:
**implementation bug**, FR-086. ROOT/JOINT publication omitted DERIVED_CARRY.
The existing derived-interval provider certification seam must publish the same
certificate/endpoints after validating its material envelope, without creating
another physics event or applying transport again. Plan review converged.

ADVERSARY's structural work oracle reproduced 96 new joint evaluations after
warm-up while alternating the same immutable A→B interval and current C endpoint
32 times. Classification: **implementation bug**, NFR-009/HOT_PRESENTATION.
The existing two-sample physics cache is insufficient for retained presentation
material. Add a separate bounded presentation memo for at most 128 distinct joint
inputs per weak entity/evaluator, and separate current/interval drawable cache
slots so querying one kind cannot evict the other. Neither memo chooses authority;
exact live certificate/history checks remain mandatory before every cache hit.
The physics two-sample cache and authoritative sampling cadence remain unchanged.
Plan review against memory bounds, lifecycle and ownership converged.

That first memo repair was insufficient: ADVERSARY's first-historical-use
holdout showed two extra joint evaluations after physics had already sampled
A/B/C. Classification remains NFR-009 implementation bug. The converged repair
therefore shares one bounded 128-input memo between physical sampling and
presentation; the existing current/previous two-slot cache is a fast view into
the same evaluated values. Presentation no longer owns a competing joint memo.
Every model evaluator has fixed revision/engine/filter ownership, entity keys
are weak, and the memo remains bounded; it supplies transforms only after the
caller has obtained an authoritative sample. Plan re-review converged.

## Implementation and evidence

Baseline `gradlew.bat build --offline --console=plain`: **BUILD SUCCESSFUL**, all
**452 required server GameTests passed** on the pulled candidate. This does not
demonstrate the newly added interval primitive or close G4.5.

After I2, unchanged `python tools/s29_adversarial_inbox_probe.py` passed:
expired replay returned `accepted=false, authoritative=false`. ADVERSARY
independently re-ran the same probe and confirmed that result.

Existing real-client `AnatomyPresentationFrameTests` failed at line 70 waiting
for a fixture root update after stopping the server anatomy runtime. This was
handed to ADVERSARY for test/evidence classification; no production change was
made merely to accommodate that fixture.

## Review / closure

The repair batch is verified: final ordinary build passed **458/458 required
server GameTests**, including the four development retention regressions and
both adversarial joint-reuse regressions. The final real-client run passed all
five affected proof classes (endpoint/no-inference, two real root events,
unavailable/replay, derived-carry and certificate identity/order/fractions).
The unchanged inbox campaign passed and killed all eight directed scratch mutants.

IMPLEMENTER's final pass reviewed the complete production diff against each
classified defect, shared authority, fail-closed paths, weak/bounded memo state,
cache-hit fencing, common/dedicated safety, scheduling and mainline preservation;
it requested no further change. ADVERSARY's independent final batch pass likewise
requested no further production change; see `S29-adversarial-recertification.md`.
Exact executed logs/hashes and reproduction are in `../VALIDATION.md`.

No full sprint/gate closure is claimed: frontend mutation adequacy, the remaining
contact/lifecycle/latency matrix, visual evidence and longer memory/fan-out
acceptance remain open in the canonical G4.5 gate. G4.6 has not started. This
checkpoint has not been pushed or released.

## Continuation — joint-only integration evidence

Active role remains IMPLEMENTER. The next coherent frontier is FR-086's joint-only
publication: a real authoritative head articulation must produce a certified
interval with unchanged root, distinct authority pose inputs and changed head
geometry. The proof uses the live server tick and existing wire/frontend; it does
not manually publish a certificate or run a renderer animation as authority.

- [x] J1 Add and execute a development client integration proof of that frontier.
- [x] J2 Classify any failures before changing production; hand the candidate to
  the independent adversary for the remaining canonical S29 evidence.
- [x] J3 Record executed evidence and review the affected scope without advancing G4.6.

Plan review: this extends evidence on the existing primitive, does not alter its
architecture or scheduling, and does not prescribe unrevealed adversarial cases.
One complete plan pass produced no further change.

J1 executed in a distinct build/run directory. The live client received material
serial 2; fixed fractions 0/1 changed original head pieces with equal root DTOs and
an advancing joint-sample clock. BUILD SUCCESSFUL in 3m53s, including clean world
teardown. No production defect was observed by this development proof. Registered
the proof in ordinary client metadata and handed it to ADVERSARY without exposing
or prescribing its independent holdouts. Canonical evidence is in VALIDATION.

### Locality extension after IMPLEMENTER receiver review

Classification: HOT_NETWORK implementation cost defect. Every new certificate
receiver calls `TrackingReplayFence.retiredGeneration(UUID)`, whose current loop
visits all retired supports (up to 4096), even when none belongs to that UUID.
The inbox's local lookup repair therefore left a global scan at its frontend.
The observable structural cost is one global tombstone visit per retained scope
per certificate; increasing unrelated retired supports increases this work.

Plan: group the existing canonical tombstones by UUID and dimension, preserving
all exact identity/order logic, the 4096 total-scope cap and sticky saturation.
Generation lookup examines only the named UUID's dimension scopes; no secondary
authority index or weaker lifecycle gate is introduced. Add a development
regression for cross-dimension max/removal, then run the ordinary server suite and
affected client evidence before independent handoff. Reviewed capacity accounting,
accept/removal, cross-dimension barriers and fail-closed behavior: no further plan
change after the complete second pass.

- [x] L1 Localize retired-generation queries without changing replay authority.
- [x] L2 Verify scope accounting, dimensional maxima/removal and affected runtime.
- [x] L3 Independent adversarial review and evidence registration.

Ordinary build on L1 passed **459/459 required server GameTests**, including the
new cross-dimension/accounting regression. ADVERSARY's unchanged scratch-source
locality probe also passed independently and when repeated by IMPLEMENTER: with
4096 total scopes, a target with three dimensions visits exactly three tombstones,
and an unknown target visits none. Removal, budget reuse and saturation stayed
conservative. The affected real-client battery passed all six classes together,
BUILD SUCCESSFUL in 2m24s with clean teardown. Final IMPLEMENTER replay-fence diff
review checked grouped ownership, total-scope accounting, repeated removal,
identity/order/epoch barriers and saturation preservation, with zero changes.
ADVERSARY's independent review/mutation campaign remains responsible for L3 and
the full G4.5 acceptance frontier.

### Final receiver-frontier handoff

ADVERSARY completed independent lifecycle/restart, gravity roundtrip, wire-cap and
two-support proofs and killed nine compiled frontend mutants spanning all eight
minimum S29 families. Its final receiver-frontier review requested zero production
changes. IMPLEMENTER verified all 12 recorded evidence hashes and frozen frontend
source hashes, reviewed mutation substitution/reproduction/classification, then
registered the seven adversarial client classes in ordinary client metadata.

The final combined nine-class original-source client battery passed, BUILD
SUCCESSFUL in **2m59s**, exit 0 and clean teardown. Default UNAVAILABLE was selected
for that battery; independent gravity-mode evidence remains separately recorded.
The complete product patch hash remained unchanged. Final implementer review of
the production diff, proof registrations, evidence, ownership and canonical scope
requested zero further changes. VALIDATION contains the exact tested hashes/logs.

This continuation closes J1-J3 and L1-L3 and the tested receiver frontier, not the
whole S29 milestone. Dedicated/reconnect/catalog/dimension transition coverage,
longer heap/GC/fan-out, applicable visual/contact snapshots and the final whole-
matrix acceptance pass remain open in G4.5. No G4.6 implementation or publication
occurred; changes remain local and the complete sprint has not been committed.

## Continuation — catalog transaction and interval authority

Active role remains IMPLEMENTER. `origin/main` is still locally recorded at
`b5e62535`; its unrelated hotfix changes remain outside this scope. The next
coherent frontier is NFR-016/017 and FR-082/086: an accepted live catalog reload
retires old interval authority, while rejection preserves the exact accepted
provider and interval authority. Existing S24 tests cover binding slots but do not
exercise the interval certification seam. Development proofs will use live
published endpoint pairs and the production reload path with test resources;
ADVERSARY independently owns full receiver/connection transitions.

- [x] C1 Add server regressions for accepted/rejected reload interval authority.
- [x] C2 Execute the regressions and ordinary server suite; classify failures
  before any production repair and re-review the plan when necessary.
- [x] C3 Independent handoff/review and canonical executed evidence.

Plan review covered transaction commit order, identity generations, common safety,
no new wire/schema, unchanged scheduling and test-fixture cleanup. A complete
second pass produced no plan change. No production change is assumed necessary.

C1 fixture compilation initially rejected a compound `var` declaration; corrected
as a development-fixture syntax issue, not a production RED. A second invocation
was interrupted by a concurrently authored adversarial fixture's package access
error; returned to ADVERSARY for infrastructure repair without editing its source.

Receiver review adds a development observable within C2: after BINDING deactivates
client physics, a rejected fragmented candidate must restore physical queries from
the still-accepted catalog. This tests rollback recovery rather than only the
transfer object's accepted revision. No production change precedes reproduction.

The first executed server fixture requested its query frame synchronously before
the first authoritative pose tick after start/reload and failed at its initial
`orElseThrow`. Classification: fixture setup, not a catalog defect. The fixture
now invokes the accepted provider's ordinary authority tick before observing the
live endpoints. It does not replace pose channels or forge snapshot geometry.

The real-client rollback regression reached its intended RED: physical baseline
present, first fragment enters BINDING and pauses queries, bad digest rejects the
candidate, mode returns READY and presentation remains available, but the support
is still physically unregistered. Classification: production bug, NFR-016 and
FR-082/086 recovery. `bindPhysics` deactivates the core during BINDING without
invalidating its `providers` registration cache, so subsequent READY skips every
previously registered support. Plan correction: clear that registration-only cache
at the same deactivation boundary, retaining the accepted catalog, endpoint history
and evaluator authority; ordinary READY rebinding then recovers physical queries.
Re-reviewed mode gates, contact heartbeat recovery, local generations, catalog
commit cleanup and level/session barriers; a complete pass produced no further
plan changes. No renderer or protocol change is introduced.

The corrected production passed the unchanged rollback property in a real client:
same accepted revision, presentation and query frame recovered, BUILD SUCCESSFUL
in 3m35s with clean teardown. The test's additional explicit revision assertion
confirms the invalid candidate was rejected rather than silently replacing data.
Added the regression to ordinary client metadata and returned the green candidate
to ADVERSARY. Full ordinary/server and affected-client checks follow before C3.

The corrected ordinary build passed 461/461 required server tests. ADVERSARY then
revealed a dedicated reconnect failure, NFR-017/FR-080: captured old material
becomes drawable before any new tracking window when the persistent server keeps
its epoch/revision. Classification: production lifecycle barrier. Plan correction:
retire live tracking windows strictly on disconnect/explicit level barriers,
preserve only bounded replay tombstones across reconnect to the same accepted
epoch/revision, and clear them when accepted catalog identity changes. Quiet TTL
retirement must retain its existing forward-recovery semantics. Catalog, geometry,
contacts, certificates and receipt tokens still clear at disconnect. Review of
ordering, repeated JOIN/DISCONNECT, budget saturation and fresh generation
recovery produced no further plan changes. Test the unchanged revealed dedicated
property and ordinary suite before independent handoff.

The reconnect repair passed 462/462 required server tests, the unchanged revealed
dedicated early-replay proof (old material authoritative=false), and the combined
ten-class original-source client battery, all with exit 0 and clean teardown.
Locality/capacity remains 3/4096 visited scopes for the selected support. A final
Javadoc-only correction describes catalog-identity ownership of saturation; no
executable change followed these runs. `../s29-catalog-reconnect-evidence.json`
records the new product patch, all seven production sources and seven executed
logs. Product patch SHA-256 is
`970077a86cd75802ce50eb95e27e6d085b28cc4004083e2b4c3428f1ee222c36`.
Independent catalog and normal reconnect acceptance still gate C3. Review of the
production diff, explicit/quiet barriers, same/different catalog identity, strict
window promotion, scope locality and canonical documentation found no further
production change. S29/G4.5 and G4.6 status are unchanged.

### Catalog/reconnect frontier close — 2026-10-03

ADVERSARY independently passed honest-digest/invalid-JSON rollback together with
normal dedicated reconnect, then the unchanged early-replay property separately.
Its final complete frontier review requested zero production changes. IMPLEMENTER
read the revealed fixtures, expanded strict-window probe and final review, matched
both independently recorded source hashes, and reproduced the adversarial catalog
proof separately: BUILD SUCCESSFUL 1m49s, clean teardown. The initial adversarial
cleanup blockage remains classified as fixture infrastructure, not successful
Gradle evidence; its fixed repetition closed cleanly. The early replay log includes
a host-clock discontinuity, so its printed long duration is not performance data.

The evidence inventory now contains eleven executed logs and unchanged product
hashes. Final IMPLEMENTER review of production, test cleanup, classification,
evidence and remaining acceptance boundaries requested zero changes. C1-C3 close
only this tested catalog rejection/provider-reload and persistent reconnect
frontier. Full accepted-catalog client/dimension/permutation matrix, applicable
visual/contact snapshots, sustained heap/GC/fan-out and whole-sprint acceptance
remain open in G4.5. No G4.6 work, commit, push, release or installation occurred.

## Continuation — accepted catalog on a live client

Active role: IMPLEMENTER. This is a new dependency-coherent slice of the still-open
G4.5/S29 acceptance gate, not a G4.6 feature. It covers NFR-016/017 and
FR-080/082/086 for a successfully committed catalog revision: the client must
lose the prior revision's certified interval, receive the fresh binding/endpoint,
and materialize only a new server-certified interval. The server provider seam and
rejected client candidate were proven above; the missing observable is the live
accepted client transaction. No protocol/schema or renderer change is planned.

- [x] A1 Add a development real-client proof driven by `AnatomyRuntime.reload` with
  valid test resources and a real prepared cow, obtaining certified ROOT material
  before and after the commit.
- [x] A2 Classify any failure before a production repair; require old-material
  rejection and fresh physical/presentation authority in the same client process.
- [x] A3 Run the affected server/client suites and hand the frozen candidate to
  ADVERSARY for its independent acceptance and dimension model.

Plan review: the test-only resource proxy will call the existing transactional
reload path and reuse the prepared model/profile inputs. Before the new ROOT move,
it checks that the old certificate has gone and the fresh current endpoint/physical
query is usable. It then checks the new certificate's revision and binding,
avoiding an accidental pass through a reused material serial. Lifecycle cleanup
is fixture-owned. This is receiver/transaction evidence, not visual acceptance or
a replacement for independent adversarial permutations. A second pass over
catalog commit order, recipient generation, server/client event threads, identity
axes, resource IDs and test teardown produced no plan change.

The first real-client execution reached an initial certified interval but asserted
physical provider registration synchronously in the same early client phase.
It failed before the reload at `Initial physical provider absent`. Classification:
fixture ordering, since client binding is completed on a later tick. The proof now
waits for the baseline physical query before initiating reload. No production code
changed, and this run is not accepted-red evidence for the catalog transaction.

The second attempt did not reach its oracle: ADVERSARY's concurrently authored
test fixture referenced package-private `AnatomyRuntime.ownsSupportBinding` from
another package, making all GameTests fail compilation. It repaired only its own
diagnostic; no production visibility was widened. The third attempt passed the
unchanged accepted-catalog assertions in a real client, BUILD SUCCESSFUL in
1m42s and clean teardown: old certificate/revision absent, new presentation and
physical frame share the committed revision, and a real ROOT move materializes a
fresh certificate with a higher binding generation and exact endpoints. Production
source has not changed in this continuation. The ordinary build passed 462/462
required server tests; independent dimension/catalog review remains part of A3.

Performance classification for A1/A2: the only new executable code is in the
GameTest source set (`OFFLINE_TOOLING`); its resource proxy and reflection run in
the test harness. No production HOT_TICK, HOT_MOVE_QUERY, HOT_NETWORK or
HOT_PRESENTATION path changed, and this development proof makes no new performance
claim. S29's sustained heap/GC/fan-out evidence remains an open G4.5 obligation.

ADVERSARY's independent accepted-replacement and active/early A-B-A dimension
proof passed on the frozen product, BUILD SUCCESSFUL 1m6s with clean teardown.
It rejects the old catalog bundle and exact prior-revision material, then proves
fresh ROOT progress; its early replay remains within TTL and runs before any fresh
pose can hide a stale interval. IMPLEMENTER reviewed the revealed fixture and
repeated the same three PASS assertions independently, BUILD SUCCESSFUL 1m12s and
clean teardown. Source hashes match across both roles and `git diff --check` passes.
Both roles' complete review of this accepted-catalog/dimension slice requested
zero production changes. A1-A3 close only that tested slice. Connection/contact
permutations, visual/contact snapshots, sustained heap/GC/fan-out and whole-S29
zero-change review remain open; G4.6 has not started.

## Continuation — bounded receiver retention under virtual fan-out

Active role: IMPLEMENTER. `S29ImplementerIntervalSoak` is an isolated test-only
receiver probe, not the integrated NFR-014 benchmark. It runs a 12,000-tick
virtual clock over 16 independent certificate inboxes and 64 support identities,
with one independently allocated server-issued-shaped certificate per support
and recipient every four ticks. It checks
the per-support retained-entry cap at both half and full duration and compares
settled heap after equally long windows. This exercises NFR-011's S29 certificate
retention with the same observer/support cardinality as the later normative
workload, while excluding wire delivery, physics, real-time tick pacing and body
contacts. The test stays outside ordinary GameTest registration so diagnostic GC
does not enter routine validation.

Execution: `../s29-implementer-interval-soak.log`, SHA-256
`737d13062927261a446ab6054cf5bdbdd96dee7b96da1a901c951b994dae3d0c`,
BUILD SUCCESSFUL in 35s, both selected GameTests passed with clean dedicated
shutdown. The receiver accepted **3,073,024** certificates. After explicit GC,
heap at the half/full snapshots was 237,681,888 / 237,685,832 bytes (delta
**3,944 bytes**); six GC cycles were observed across the run. Every live
inbox retained 64 support identities and at most 26 certificate entries per
support, without saturation. The 3.9-second GameTest workload is synchronous
virtual time; its `Can't keep up` log is not a measured 20-TPS failure or a
20-TPS success. This is bounded-memory evidence only. Integrated packet fan-out,
real bodies, allocation rates, real-tick GC and the baseline-relative p95/p99
NFR-014 workload remain for G9; G4.5 still needs contact/visual and final
whole-sprint adversarial closure.

## Continuation — confirmed contact across socket replacement

Active role: IMPLEMENTER. The independent real-client holdout
`S29AdversarialContactLifecycleProof` is RED on the frozen product: a genuine
passive pig-on-cow contact is confirmed, the client disconnects from a persistent
dedicated server, the server contact is cleared while publication is stopped,
and the same server catalog plus a fresh support tracking pose are received on
reconnect. Replaying the captured pre-disconnect contact inside its 100-tick TTL
sets `presentationContact` again while `physicalSupported=false`.

Classification: **production lifecycle bug** under NFR-017/FR-080, not a bad
fixture. The contact inbox intentionally retains accepted ordering watermarks
across same-connection level changes, but both `reset()` and `acceptedCatalog()`
unconditionally erase them. The pose replay fence already scopes retired
windows to the accepted `(epoch, revision)` catalog identity; contact ordering
needs the same boundary. This repair touches no schema, physics solver or
renderer path.

- [x] R1 Retain contact watermarks, dropping pending material, across socket
  replacement until the accepted catalog identity is known.
- [x] R2 Preserve watermarks for the same accepted epoch/revision; clear them
  when a different catalog identity is accepted.
- [x] R3 Re-run the unchanged adversarial reconnect proof and the affected
  client/server suites; review bounds and old/new authority progress.
- [x] R4 Retire the entire prior body tracking generation at each client level/
  socket barrier, including unseen higher contact sequences in that generation.
- [x] R5 Ensure the server's replacement recipient receives a strictly newer
  tracking generation within the same server authority lifetime; prove both
  anti-replay and fresh progress without retaining retired UUIDs.

Plan review: `AnatomyContactOrdering` already compares tracking generation
before sequence, and `AnatomyContactInbox` retains at most 4,096 body
watermarks with a 100-world-tick prune. A new tracking generation can therefore
advance after reconnect; the old packet cannot. Carrying only watermarks,
not pending/presentation/physical state, keeps the reset fail-closed. Repeated
JOIN/DISCONNECT and same-catalog bundle acceptance remain idempotent; a new
server epoch or accepted revision clears the previous authority. This is the
smallest shared lifecycle change consistent with existing dimension behavior.

IMPLEMENTER changed only the client reset/catalog boundary: `reset()` now lets
the existing cross-level barrier discard pending contact material while keeping
accepted watermarks; `acceptedCatalog()` clears those watermarks only when
`(epoch,revision)` differs from the prior accepted identity. The original RED
holdout passed unchanged in an independent build/run directory:
`../s29-implementer-contact-fix.log`, SHA-256
`42fbd8e23ab571c0ac574e38f2ad345a7fc6e594a5c734f299305bfa1cbfbfa1`,
BUILD SUCCESSFUL in 1m42s with clean dedicated shutdown. It reported
`authoritative=false physicalSupported=false` for the exact old replay, then
materialized the positive-control contact under a fresh body generation.
ADVERSARY has been asked to retest the repair independently. R3 remains open
pending the affected suites and whole-sprint review.

The independent retest found the important remaining permutation: an unseen
later `sequence` from the **retired** body tracking generation still passed the
ordinary accepted watermark and revived presentation after reconnect. Its
`-Ps29ContactDelayedSequence=true` lane is RED on the first repair
(`../s29-adversarial-contact-delayed-window.log`). R4 therefore adds a strict
closed-window scalar per bounded body entry; consuming only the last observed
sequence is insufficient. Implementation review also found that the current
server allocates tracking generations from one new ledger per recipient. A
replacement connection can repeat the same generation numbers, which would
make any correct retired-window fence reject legitimate fresh authority. R5
shares one monotonic scalar across recipient ledgers for the server runtime
while keeping each active-UUID map bounded and connection-owned. No extra
per-UUID tombstone map is needed. This plan supersedes the earlier claim that
watermark preservation alone closes the whole reconnect frontier.

R4 stores the last accepted contact and a retired-generation scalar in each
existing bounded body entry. The level/socket barrier drops pending contacts
and retires the complete generation; accepting a later sequence from it now
fails before any material state changes. The unchanged delayed-window holdout
passed in `../s29-implementer-contact-delayed-fix.log`, SHA-256
`f1fa1ec5ab8501d9f7bd859c6ce3356a09c99fb129298de8bda794f524cd8bdf`,
BUILD SUCCESSFUL 1m32s, clean dedicated shutdown. The earlier RED is
`../s29-adversarial-contact-delayed-window.log`, SHA-256
`af75dbf9deb9e33f14d078622df6ce5572fbc1a4cc1598da7bcf42cc0dbd0b49`.

R5 was independently observable before editing: two new recipient ledgers on
the same live server allocated `old=1 fresh=1`, so a closed-window client
could reject a genuinely new connection. The development GameTest RED is
`../s29-recipient-generation-red.log`, SHA-256
`f15bf873c05c1d5c9536dba328959ac0624ca35a50304e82d61bb85fbe422a94`.
`TrackingGenerationLedger` now accepts one shared monotonic scalar from
`AnatomyRuntime` for all recipient ledgers; the scalar survives runtime
stop/start for the same server object in a weak-key map. Each recipient still
owns only its bounded active-UUID map and observation remains pure. The same
GameTest passed `2/2` in `../s29-recipient-generation-repaired.log`, SHA-256
`687e51f545cbd7cee7e01b3f9b3b7b10757baa46512b864f86535ba09ef4404b`,
BUILD SUCCESSFUL 39s. No retired body UUID map was introduced.

Performance review: contact packet acceptance remains one hash lookup plus a
constant-size watermark update; server generation allocation occurs only at a
tracking transition, with one scalar increment. `retireWindows()` scans at
most 4,096 known body entries only on level/socket/catalog barriers, never on
HOT_TICK, HOT_MOVE_QUERY or HOT_PRESENTATION. A weak server-key scalar map adds
one small entry per live server, and no per-reconnect history. The affected
batteries, independent delayed-window retest and live fresh-progress review
are recorded below; whole-sprint G4.5 closure remains separate work.

The expanded default client battery subsequently passed, including S00 and
the accepted/rejected catalog and A-B-A dimension proofs:
`../s29-default-client-after-contact.log`, SHA-256
`fc73fc5e012bc96dda25c8e932b4f8d50f29b153d63760c756897950d450722c`,
BUILD SUCCESSFUL 4m25s with clean shutdown. After registering the new
recipient-generation test, the ordinary server build passed **463/463**:
`../s29-ordinary-final-candidate.log`, SHA-256
`99b8f2ea0c211b59b313fcd28f251e3081e0697a70261bbca603f75b6d14d79b`,
BUILD SUCCESSFUL 1m44s.

ADVERSARY repeated the delayed-sequence rejection on this repair across a
real dedicated reconnect and A-B-A dimension boundary:
`../s29-adversarial-contact-final-delayed.log`, SHA-256
`59c0fd949b558efd1db9abc79a49e2c6e861b118dc6eedb3f0608b6159fc17ff`,
BUILD SUCCESSFUL 1m25s. Its additional live reconnect oracle injected no
pose, contact or generation: the solver established contact before and after
the same-server socket replacement, and actual server/client body generation
advanced **3 -> 6** with the catalog unchanged and observer read-only:
`../s29-adversarial-contact-final-exact-live-port.log`, SHA-256
`8fb9accd00f8296449c131fa20584fafbaa8a797e5b0e414b73de6c85e17cdf27`,
BUILD SUCCESSFUL 1m28s. Both shut down cleanly. The adversarial full-client
repeat subsequently passed (see its independent record); whole-sprint visual,
performance and zero-change review remain before S29 closure.

IMPLEMENTER's second static performance pass classifies accepted-interval
publication as HOT_NETWORK (`O` observers per accepted material interval),
certificate acceptance as HOT_NETWORK, and exact presentation as
HOT_PRESENTATION. Publication reuses the two provider-owned endpoints and
builds two ordinary pose payloads plus one small certificate per compatible
recipient; it does not compile or hash a catalog per recipient. The cost still
scales with the number of accepted intervals and observers, so no real-tick
throughput claim follows from this review. On the client, certificate lookup
and expiration scan at most 128 entries for one support; periodic pruning
visits at most 4,096 support states, bounded by the receiver cap. Endpoint
history retains at most 128 packets/support for 100 client ticks, and the
additional joint-transform memo retains at most 128 input samples per live
entity behind weak entity keys. Presentation caches one current endpoint and
one interval/fraction per support; repeated identical queries reuse those
results. Contact barriers scan at most 4,096 bodies and run only at level,
socket or catalog transitions. The virtual receiver soak confirms bounded
certificate entries and settled heap under `64 × 16` fan-out; integrated
packet allocation, time distributions and the normative NFR-014 workload
remain G9 evidence. This inspection found no new unbounded production state
or preparation/reflection in the physics/query hot loop.

## Whole-sprint IMPLEMENTER review candidate

The reviewed S29 scope is the server-issued certified presentation primitive,
not its later renderer/camera residual consumer. A real renderer screenshot
cannot yet distinguish an interval at fraction 0, 0.5 and 1 because G4.6 has
not installed that consumer. The applicable S29 visual check is the fixed
support-relative geometry/contact projection recorded by ADVERSARY; it shows
the real confirmed baseline anchor and the absence of any reanimated contact
for equal separated geometry after dimension/reconnect replay. Actual-renderer
alignment and camera acceptance remain explicit G4.6/G9 work, rather than an
untested S29 claim.

The applicable S29 performance question is whether the new certificate,
endpoint and presentation state grows without limit or repeats joint work per
observer/query. The 12,000-virtual-tick 64-support/16-receiver soak, cap/TTL
assertions, two independent heap/GC runs, killed bound/lookup mutants, and
zero duplicate joint evaluation under repeated/alternating presentation
queries address that question. They do not simulate 16 real network clients or
establish p95/p99/TPS, which the canonical plan explicitly places in G9's
integrated NFR-014 benchmark. Publication still emits two poses and one
certificate per accepted interval/recipient, an `O(intervals × observers)`
cost to measure under that later workload. No timing claim is inferred from
the virtual soak. This boundary and the complete behavioral matrix await the
independent final adversarial decision before G4.5 is marked closed.

## Reopened HOT_PRESENTATION preparation — IMPLEMENTER plan

ADVERSARY's newly revealed 64-support real-client holdout makes the static
review insufficient. When only the requested fraction changes, the same
certified before/after endpoints cause `ModelGeometryProvider` to construct a
new `HierarchyMotion` for every query. Its temporary counter records 64,000
preparations in 1,000 one-consumer batches; the unchanged RED log is
`../s29-adversarial-presentation-preparation-counter-retry.log`. In the
observational run, changing-fraction one-consumer batches allocate about
6.77 MB and have p95 5.84 ms, versus about 60 KB and p95 0.235 ms for a fixed
fraction. This is repeated invariant preparation and churn in HOT_PRESENTATION,
not proof of an NFR-014 tick-budget failure. The existing one-frame cache is
fraction-specific and therefore cannot own this prepared interval.

- [x] P1 Retain one prepared `HierarchyMotion` per live entity in the bound
  `ModelGeometryProvider`, keyed by the complete pair of causal endpoints,
  independent of fraction. Keep the already-bounded current/interval evaluated
  frame caches and receiver certificate validation as the authority gate.
- [x] P2 Reuse that motion for every fraction while both endpoint values match;
  reprepare on either endpoint change and never infer a certificate from a
  cache hit. Do not cache an evaluated fraction as if it were motion authority.
- [x] P3 Run the unchanged instrumented adversarial RED holdout and affected
  ordinary client/server batteries; compare allocations/work and review memory
  bounds and lifecycle invalidation before returning to ADVERSARY.

Plan review: `GeometryProvider.CausalEndpoint` is a value record containing
serial, authority clocks, root DTO/transform, joint sample and availability.
The provider itself is bound to one model/filter/revision. Its existing weak
entity-key caches can own one last prepared pair without adding a per-serial
history. Exact equality of both endpoint values covers root-only, joint-only,
gravity and lifecycle changes; the client still verifies the live certificate
and exact packet identities before reaching this evaluator. One motion/entity
is bounded, avoids cross-support scans and cannot keep a removed entity alive
through a weak key. This is the converged local repair; it does not alter Q2
physics motion construction or packet protocol.

The implementation adds one weak-entity-key `PresentationMotion` value to the
bound evaluator. A complete `CausalEndpoint` pair identifies prepared motion;
fraction changes reuse it, while any endpoint-value change rebuilds it. The
value carries no entity reference and contains at most one motion/entity.
Negative preparation results are cached for that exact immutable pair, so an
unsupported pose cannot trigger repeated compilation. Receiver certificate,
TTL, identity and endpoint checks still precede every evaluator call.

The unchanged observational-counter holdout now passes:
`../s29-implementer-motion-preparation-fix.log`, SHA-256
`662f677a2765fad9cd2b7a87976ea067f5539b5fcde17d841680d2e178f09ad9`,
BUILD SUCCESSFUL 1m40s, clean integrated-server shutdown. In 1,000 measured
batches of 64 real supports with varying fractions, it reports **zero**
compiled motion preparations and zero duplicate joint evaluations. The
one-consumer candidate allocation fell to 2,274,153 bytes/batch with p95
3,105 microseconds; its earlier uncached run allocated about 6.77 MB/batch
with p95 5,840 microseconds. The 16-consumer changing-fraction run reports
3,095,552 bytes/batch and p95 3,189 microseconds, with the same one prepared
motion/support shared across consumers. These are real-client synthetic render
query measurements under a held server clock, not NFR-014 tick measurements.
The remaining per-new-fraction geometry evaluation cost is material but no
longer repeats invariant motion preparation. Ordinary/full-client regression
and independent adversarial retest remain P3 work.

The post-repair ordinary build passed **463/463** required GameTests:
`../s29-ordinary-after-motion-cache.log`, SHA-256
`08df1529668945aa502594a5cc6066d3d0bbaf1e3ac8cf80491a253b61128738`,
BUILD SUCCESSFUL 1m21s. The expanded default client battery also passed:
`../s29-default-client-after-motion-cache.log`, SHA-256
`81014b605840b1e5bf37277ee2457a728525bfb4d5264f9b9a8b48d048e00715`,
BUILD SUCCESSFUL 4m46s with clean integrated-server shutdown. ADVERSARY's
separate prepared-motion repetition passed unchanged with zero post-warm-up
preparations and zero duplicate joint evaluations:
`../s29-adversarial-presentation-preparation-repaired.log`, SHA-256
`8df997b797b04911024d96562991297aeca82b6f0b7b146376fc755524d8444a`,
BUILD SUCCESSFUL 1m45s. Its full client repetition also passed:
`../s29-adversarial-preparation-repair-full-client.log`, SHA-256
`63e0a97e067ab5f33925add86e49dc080c0aaeaa569711aae3a2ff4e2541c206`,
BUILD SUCCESSFUL 2m40s. The remaining barrier lane passed in 1m32s
(`../s29-adversarial-preparation-repair-barriers.log`, SHA-256
`e054b3242d8c3b07065de786d8018bda0b14f26dc90dfc1348ef083189cf0620`),
and the three affected server work holdouts passed in 39s
(`../s29-adversarial-preparation-repair-server-work.log`, SHA-256
`54d99a40b9f3f5790a568cb908c45951823cec400ebc64199bdf256d8aa5635e`).

## Final IMPLEMENTER review and S29 closure

The complete current diff was compared with the S29 scope, ownership,
certificate/endpoint identity, replay and lifecycle rules, default server and
client suites, and the independent adversarial matrix. No further productive
change or evidence gap was found in the S29 boundary. ADVERSARY's final
whole-sprint review is GREEN with unchanged productive hashes and zero changes;
its detailed matrix is in `S29-adversarial-recertification.md`. This closes
G4.5/S29. The 12,000-real-tick normative NFR-014 benchmark remains G9 work,
and actual renderer/residual/camera integration remains G4.6 work.
