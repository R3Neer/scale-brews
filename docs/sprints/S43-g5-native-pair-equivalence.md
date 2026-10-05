# S43 — native equivalence at the material eligibility boundary

Status: CLOSED locally for G5 task6 after both complete zero-change reviews; its own closure commit identifies Sprint: S43.
Active role for this section: **IMPLEMENTER**.

## Scope and thesis — IMPLEMENTER

G5 task 6 must be verified before removing the legacy engine. A prepared model
alone cannot grant an anatomical wall to an ineligible pair. Eligible material
contact may replace the pair's solid blocker and carry its body, but native
push still belongs to Minecraft and must occur exactly once. Activation must
also leave nonparticipating entities' collision, pathfinding, combat and ordinary
interaction behavior unchanged.

Normative scope: FR-002..007, FR-009..012, FR-053/093 and NFR-001/004/033;
continuous cost review uses NFR-007..014/027. Canonical requirements and
architecture already define this behavior. This is a verification sprint;
production changes are authorized only if a classified defect returns the plan
to independent convergence. Legacy removal, Minecraft-family coverage, consumer/
pack migration and final human acceptance remain in their later gates.

## Initial state and investigation — IMPLEMENTER

S42 closed in the two-parent merge `c17818f7`; its work tree is clean. A fresh
fetch resolves current main to `b5e62535`, now an ancestor of the branch.
Build/server 483/483, the default 27-entrypoint client suite, prepared original
composition and actual experimental-cart controls were verified. S00's required
foundations remain closed; S43 neither replaces a foundation nor relies on an
unproven new primitive.

The live Entity.move wrapper scopes pair replacement through its moving-body
context; shared READY routes collision to AnatomyMovement, while BINDING must
return native collision without legacy fallback. The push hook delegates to
Minecraft under shared ownership. Eligibility uses actual width, canonical
policy, ordinary state and acyclic ancestry. Native solid collision and push
are different operations and need separate non-vacuous controls.

Historical G2 ratio/push fixtures and S35-S41 category proofs are candidates for
reuse, not the complete differential oracle. S43 must execute the public native
movement and query surfaces with real original-model preparation, rather than
only call the solver using a manually registered floor. The existing prepared
lane owns one runtime lifetime and sequences cases on distinct real level ticks;
its ordinary and client lanes remain separate.

## Implementation plan — IMPLEMENTER

- [x] I1 Add implementation-facing prepared regression scenarios for the
  native/material boundary, using the existing original cow export and real
  bodies. Cover player, ordinary mob, boat/raft, off-rail cart, item and falling
  block adapters. Record actual effective widths. At the ratio boundary, use a
  measured threshold and values immediately below/at/above it so float rounding
  cannot turn an equality case into a different class. Exercise enabled/disabled
  policy and a real effective-scale transition without changing attribute balance.
- [x] I2 For rejected pairs, compare real Entity.move results, native collision
  flags/velocity and scoped public clearance/raycast/contact results against an
  equivalent native control. Confirm prepared positive controls actually block
  or support an eligible body, and rejection yields no wall/anchor/carry. A
  control must preserve the same blocks, positions, dimensions, state and native
  inputs and must not accidentally acquire a legacy plane. Cover unavailable
  support/state and ordinary nonparticipating third entities without changing
  their native state merely to manufacture a zero response.
- [x] I3 Measure both directions of the actual native push operation before
  and during a genuine prepared material relation, including an actual eligible
  carry. Compare both entities' net impulse and assert a nonzero native response
  where Minecraft permits it. For native no-push categories, preserve the native
  zero rule and retain a nonzero living/vehicle control. Pair blocker suppression
  must neither erase nor duplicate push; unrelated third-pair push must match.
- [x] I4 Compare global native collision/pathfinding queries, native combat
  response and ordinary entity interaction for nonparticipants with an equivalent
  feature-off/native control. Preserve current mainline reach, gait, mounts,
  inventory and input behavior; avoid assuming that an anatomical query owns
  Minecraft's global interaction selection. Lifecycle exclusions must retire a
  prior material relation and leave native behavior in charge.
- [x] I5 Integrate scenarios into the existing prepared runtime lane with an
  isolated focal selector and no silent default exclusion. Add an explicit
  S43 client/server proof using the actual local player and native movement/push
  routes: a rejected prepared pair adds no wall, while an eligible relation
  actually acquires contact and applies carry. Compare local native responses
  with equivalent controls, require a nonzero push response where Minecraft
  permits it, preserve exactly-once behavior during contact/carry, and observe
  the actual server/client result rather than treating a server mock as client
  prediction. The existing general client suite alone cannot certify that
  authority branch. Keep the new proof enabled in the default client manifest.
  Run the focal proofs,
  full prepared composition, ordinary build/server and full relevant client
  regression. Classify every failure before changing fixture, production or plan.
  Audit cost/locality and preserve all budgets and shared ownership boundaries.
  Obtain independent holdouts, targeted mutations and deterministic snapshots
  where spatial state matters; reveal and execute them before acceptance.
- [x] I6 Complete both roles' zero-change review passes, record executed evidence
  in VALIDATION and update only the changed canonical gate status. G5 task 6 may
  close only if its native/material differential obligations are met; tasks 7-9
  remain separate. Create the local S43 closure commit and verify HEAD/tree before
  proceeding. Human-only work remains listed under G9 until the epic finishes.
- [x] I7 Repair the classified FR-011 stale-contact defect before final execution:
  retire a body's now-ineligible material contact on its actual dimensions change,
  and validate state/eligibility before the server-runtime delegation in carry.
  Validation must perform no movement, model preparation, unrelated-support scan
  or new physics owner. Keep the prepared effective-scale and support-baby RED
  assertions, repeat all affected prepared/local/ordinary regressions, and obtain
  independent review before accepting the repair. This is an implementation return
  within the existing frozen FR-011/053/093 scope, not a new requirement.
- [x] I8 Repair the independent symmetric FR-011 RED: support dimensions can
  make its retained dependents ineligible while its own body contact is absent.
  Add a derived weak-identity support-to-body membership index inside the sole
  AnatomyContactState owner. Maintain it on contact replacement, clear and level
  teardown; retain CONTACTS as the single source of contact truth. Replace the
  existing supported-by global contact scan with this local membership lookup.
  The dimensions hook must revalidate its own body contact and only this support's
  existing dependents, clearing ineligible relations/anchors/provenance with no
  movement or model preparation and preserving still-valid relations/native push.
  Require independent index lifecycle/identity consistency, body and support scale
  transitions, nonparticipant locality, applicable mutation and all affected
  regressions before acceptance. Revalidate the changed contact-state foundation
  before relying on the index; no new solver or alternate contact record is added.
- [x] I9 Repair the independent retirement-locality RED exposed by the new
  dimensions barrier. AnatomyTransportReceipts.invalidate currently scans every
  recipient history, including 128 unrelated histories when the retired Cow has
  no recipient receipts. Preserve the authoritative recipient-to-body histories
  and every receipt/claim/TTL/saturation/sequence fence, but add a derived
  body-UUID-to-recipient membership lookup so invalidation visits only historical
  recipients of that root. Current passengers alone are insufficient after a
  rider dismounts. Preserve UUID equality for body histories and weak identity
  for recipients; keep the reverse key alive while its authoritative history
  exists, without retaining entities/worlds or accumulating retired UUID buckets.
  Maintain membership on append, lazy expiration, explicit invalidation,
  disconnect, server clear and diagnostic pruning. No packet, physics owner,
  budget, replay acceptance or already-applied transport changes are permitted.
  Recertify the receipt-state extension as a foundation, repeat the unchanged
  128-recipient RED and physical boundaries, and require targeted locality and
  lifecycle sensitivity before final affected regressions and closure. Changes
  remain within FR-011 and NFR-007..014/027; no new product requirement is added.

The first independent review found RED1: I1-I4 and the existing client regression
could leave the actual local prediction authority untested. I5 now explicitly
requires a native local-player/client-server differential with rejected and
eligible pairs, real carry and a non-vacuous push control. The complete
IMPLEMENTER plan pass after that correction converged without further changes
and without modifying production or fixtures. The remaining engine-removal
tasks are deliberately later work; this
sprint establishes the differential behavior they must preserve. Independent
preimplementation convergence is required before I1 begins.

## Preimplementation model — ADVERSARY

Active role: **ADVERSARY**. Independent model starts from FR-002..013,
FR-053/093 and NFR-033, before consulting historical test cases. The boundary
has three independent observables: native solid collision/query selection,
native push on both bodies, and canonical material contact/carry. Prepared
geometry alone proves none of them. Each negative requires a non-vacuous native
control and each positive requires accepted READY binding and a real material
relation; solver registration alone cannot substitute for that ownership.

The model partitions actual width ratio below/equal/above the effective limit,
policy rejection dimensions, unavailable geometry, current effective-scale
changes and lifecycle exclusions. It distinguishes voluntary movement, native
impulse and applied material displacement, and preserves native category-specific
zero push without using those zero cases to prove the living/vehicle positive.
Both pair directions and unrelated third pairs must match the native control;
passengers and unsupported entities must remain outside material authority.
Global collision/pathfinding, combat and ordinary interaction must retain their
native selection and effect. DISABLED, BINDING and READY have distinct ownership;
BINDING cannot secretly recover through the old engine.

Performance surfaces under examination are HOT_MOVE_QUERY/HOT_TICK and, where
client evidence is consumed, HOT_NETWORK. Scaling variables are local candidate
pairs/pieces and current bodies/supports; unrelated supports must not introduce
per-query scans, extra model preparation or persistent pair histories. Fixture
preparation and differential bookkeeping are OFFLINE_TOOLING/PREPARATION. No
budget increase or whole-pack performance assertion follows from this sprint.

First plan verdict: **RED1 — missing targeted live-client differential obligation**.
I1-I4 prescribe prepared bodies/native operations, and I5 runs the existing
client regressions, but no item requires an S43 client/server proof of this
boundary for the actual local predicted actor. `AnatomyMovement.simulates` and
`predicts` explicitly select different client authority from the server path.
A server-only prepared proof plus an unchanged general client suite can therefore
pass while the local actor acquires a rejected-pair wall or loses/duplicates
native push during an eligible material relation. I5 must explicitly require
an automated real client/server differential scenario, even with no production
diff: native local movement/push on a rejected pair and an accepted material
relation, matched native control, actual nonzero native response where permitted,
and observable applied carry with client/server consistency. Human-only pack
acceptance remains in G9; it is not the missing machine-verifiable lane.
Current independent verdict: **PASS — funciona, continúa**. Reviewed the complete
revised I1-I6, not only the added paragraph. I5 now requires the actual local
prediction authority, matched native controls, rejected-pair movement without
an anatomical wall, nonzero native push where permitted, accepted contact with
really applied carry, and observed server/client outcomes. Its default-manifest
obligation prevents a focal proof from silently disappearing in composition.
This resolves RED1 without moving machine-verifiable work to human G9 acceptance.
The remaining prepared/native query, third-pair, lifecycle and performance
obligations remain consistent with the independent model. The complete second
plan pass produced no further changes or operational contradiction. Production
and implementation-facing fixtures are unchanged; runtime acceptance remains
pending. Independent holdouts may now be designed privately from this model.

## Implementation and executed verification — IMPLEMENTER

Prepared server and actual local-client differentials are implemented as test-only
sources. The server selector is `scalebrewsS43Only`; the client selector is
`scalebrewsS43ClientOnly`, and the local proof is also in the default client
manifest. Production remains unchanged. Transactional fixture reload serializes
all supplied models/profiles and restores the original prepared composition.

Initial executions exposed fixture defects, not acceptance:

- `s43-prepared-first.log`: the native, pre-activation navigation control had
  not ticked to establish ground state. The actual stone block under the pig is
  now asserted before setting the synchronous fixture's ground state; the
  multi-node path requirement is unchanged and passed on the next execution.
- `s43-prepared-second.log`: the disconnected mock Player was absent from the
  world's entity queries, so the ROOT dispatcher could not find it. It was
  replaced in both differential branches with the native connected ServerPlayer
  fixture. This does not alter any production authority or query.
- `s43-prepared-third.log` / `fourth`: the embedded-channel ServerPlayer does
  not advertise the required Fabric protocol. Actual catalog activation correctly
  disconnects it, and live diagnostics confirm `UNLOADED_WITH_PLAYER` at the
  boundary assertion. The server differential now inserts the native mock Player
  into real world queries; it does not pretend the embedded connection is compatible.
  The distinct client proof uses a genuine connected ServerPlayer/LocalPlayer.
- `s43-local-first.log`: the local relation acquired real contact and observed
  its `.2` ROOT transport, but the original body face lies inside the native cow
  AABB. A native automatic push added `.025` before the observation, violating the
  fixture's assumption that net position consisted solely of carry. The matched
  native/accepted push controls and carry now use the highest original clear face
  beyond the native overlap region; the positive nonzero manual push, actual
  contact and exactly-once displacement assertions remain. Repetition pending.

These corrections are class 5 (test/fixture), under IMPLEMENTER ownership for
these development proofs. They add no requirement or production change. Fifth
prepared and second client executions are in progress. Full
regressions, independent holdouts and acceptance are still pending.

`s43-prepared-fifth.log` / `sixth` now pass all 28 ratio/policy movement and
accepted-contact push/carry comparisons with the world-inserted native Player.
The subsequent actual scale change is **class 1, production defect**, not fixture:
at effective ratio `1.777777974007075`, eligibility is false and position remains
unchanged, but the original revision-30 material Contact still exists after ROOT
drain. Metrics show zero admitted candidates for that body, so the dispatcher does
not retire the old relation. `carry` also delegates to the prepared server owner
before validating eligibility/state. FR-011 requires this transition without
ghost state. I7 specifies local invalidation and pre-delegation validation; the
complete IMPLEMENTER plan pass with I7 has no other changes. Independent repair
plan review is pending; production remains unchanged until convergence.

Independent repair-plan convergence was subsequently recorded below by ADVERSARY.
I7 implementation changes only `AnatomyMovement` and `PlatformEntityMixin`:
an actual dimensions callback validates the body's existing contact for every
Entity category, and carry validates eligibility/gravity before the server
delegation return. Valid delegated relations still return before any endpoint
movement. Validation reads one identity-keyed contact and resolves its current
pair policy/ordinary state; it does not sample geometry, traverse unrelated
supports, add an owner or increase budgets. The helper reads the stored internal
entry directly rather than allocating a wrapper for the dimensions hook. Invalid
relations use the existing clear path and retire contact/anchor/receipt/observer
state while retaining the already-applied segment needed for current-tick motion
accounting. Prepared/local repaired runs are in progress.

`s43-local-second.log` observes the actual clear-face carry and then detects an
unequal push input: material endpoints arrive before the native replica finishes
interpolating. The local proof now waits for the actual native support position
before comparing the same relative-state impulse and uses `.25`, exactly
representable by native position quantization. This is class 5 fixture timing;
the actual nonzero ROOT/impulse and final server/client equality remain required.

The third local focal execution completed cleanly (`s43-local-third.log`,
BUILD SUCCESSFUL 2m39, SHA256
`9be11140fc74cf136466399200750694073fd55257c3b8f28f765890bb36ac35`).
Rejected actual local Entity.move matches the pre-activation native result
`(5.850000005960464,0,0)` and all flags/velocity. Both callers and both bodies
have nonzero native impulses in client and server before activation and during
actual canonical contact/carry. A real ROOT moves the connected server actor and
local predicted actor `.25` exactly once; the proof observes the local transport
receipt, checks displacement, repeated drain and final actual client/server
positions after native replica interpolation. This is pre-I7 evidence, not final
acceptance; the affected local regression will repeat after the repair.

The I7 focal repair passes (`s43-prepared-repaired.log`, 1m42, comparisons28,
nonzeroPushControls14; `s43-local-repaired.log`, 2m53). Full prepared composition
also passes 24 scenarios, 2/2 tests, clean exit 1m13. These results are superseded
for final acceptance by the independently revealed support-side RED:
`s43-adversary-support-scale-first.log`, 1m06, actual support SCALE8→.5 with
body1 leaves the pair ineligible, position/transport unchanged, but Contact and
public `AnatomyApi.support(body)` still identify the old Cow. This is **class 1,
production defect**, within FR-011 and the existing frozen scope. I7's body-local
hook cannot retire contacts belonging to other bodies; calling carry artificially
would hide the missing native dimensions barrier. I8 adds a derived local index
to the existing sole contact owner, so revalidation can reach exactly the support's
dependents without a global contact/world scan. The complete IMPLEMENTER I1-I8
plan pass produced no other changes. Independent convergence is pending before
the production extension; ordinary/client regressions already running against I7
will be classified as intermediate evidence and repeated after I8.

After independent I8 convergence, implementation extends only the existing
contact-state owner and dimensions barrier. `SUPPORTED_BODIES` stores weak
identity membership with Boolean markers, never copies contact records. Contact
replacement unlinks the old support, clear removes empty buckets, and level
deactivation calls the same clear path for either retired endpoint. Supported-by
lookups no longer scan all CONTACTS. Dimensions validation visits its own body
and this support's indexed members; current pair validation and the established
clear/receipt path are reused. Cost is O(1 + local dependents + their existing
ancestry), with no unrelated scan/preparation and O(live membership) weak storage.

Foundation revalidation: the changed ContactState membership contract is
**UNPROVEN for this extended scope** under foundation audit §12 until independent
recertification. The added IMPLEMENTER bookkeeping proof exercises real native
equal-ID objects without putting duplicate IDs into native world lookup, same-
support revisions, A→B replacement, clear/watermarks, old-level retirement with
body already in the destination, preservation of unrelated destination members
and destination teardown. These stored-fact tests make no physical-contact claim;
actual scale/contact/carry proofs remain separate. Both proofs are included in
the S43 client selector and in the default manifest (now29 before any independent
promotion). Production, state bookkeeping and live regressions will all be
reviewed after execution; no new feature advances on this unproven foundation.

Independent I7 reconvergence — ADVERSARY: **PASS — funciona, continúa**.
The separate unchanged scale-transition oracle reproduced FR-011 after real
contact and exactly applied ROOT: actual widths become ineligible, no new carry
is applied, yet the material Contact remains alive. I7 addresses retirement
separately from motion accounting and restores state/eligibility validation
before the server owner return. Its retained support-baby assertion also covers
the other side of the relationship; a body's own dimension hook alone cannot
certify a support-side exclusion. Review requires actual current dimensions for
all body adapters, retirement without movement or fresh material preparation,
and preservation of valid relations and native push. Work must remain local to
the changed body/support and its existing dependents; no whole-world scan or
second transport owner is permitted. The full amended I1-I7 plan is consistent
with this model and produced no additional planning contradiction. Runtime
repair, unchanged RED repetition, locality controls and closure remain pending.
Independent I8 reconvergence — ADVERSARY: **PASS — funciona, continúa**.
The complete plan now addresses the symmetric support-side RED using the sole
contact-state owner. The membership map is a derived lookup, never an alternate
Contact/Anchor ledger; CONTACTS remains authoritative. Foundation revalidation
must check identity independently of UUID/network ID, same-contact updates,
replacement A to B, clear, stale membership and level teardown when the body
has already changed level. Weak support keys and weak body membership must
avoid keeping removed entities/worlds alive. A support mutation scales with
that support's existing dependent count D, not all retained contacts B; clearing
or replacing one body cannot traverse unrelated buckets. Reads must return a
stable local snapshot suitable for clearing members during iteration.
Still-valid dependents preserve contact sequence/provenance and native push;
ineligible dependents retire without model sampling or movement. No solver,
physics budget or public packet contract changes follow from this index.
I8 explicitly requires the modified foundation to be recertified before relying
on it. The amended I1-I8 pass yields no additional planning contradiction;
index implementation, independent foundation/locality proofs and unchanged
support-scale oracle remain required before acceptance.
Independent I9 reconvergence — ADVERSARY: **PASS — funciona, continúa**.
The full I1–I9 plan was reviewed against the classified retirement dependency,
existing receipt authority and the independent model. A current-passenger list
cannot retire histories belonging to a former rider; UUID equality for roots
and identity for recipient connections remain distinct obligations. A derived
membership lookup may select histories to remove, but cannot mint/consume receipts
or substitute for their authoritative state. Planned maintenance includes every
history removal path, including lazy pruning and diagnostics; saturation-only
histories and consumed receipts must retain their existing lifetime/fences.

The extended receipt foundation stays **UNPROVEN** until independent execution.
Recertification must cover retained historical recipients, same-UUID root values,
connection replacement identity, append/duplicate/saturation, expiry, disconnect,
server teardown, diagnostic pruning and unrelated history preservation. Reverse
membership must not disappear while the authoritative history survives, and must
not retain recipient entities/worlds or accumulate retired root keys afterward.
The exact representation is an IMPLEMENTER choice constrained by that contract;
weak references alone are not proof of a consistent lifecycle. Hot retirement
must visit only that root's historical recipients; cold teardown/diagnostics and
weak cleanup must be classified separately rather than silently charged to the
new dimensions hook. Existing packet, replay, receipt and physical boundaries
remain unchanged. The retained 128-history oracle and affected physical cases
remain required; its green result alone will not recertify the receipt owner.

No additional contradiction emerged from the complete plan review. I9 can be
implemented within the frozen requirements; this planning PASS does not mark
RED3 repaired or approve S43 closure. The I8 cost statement above is intermediate
and was invalidated by RED3 for downstream retirement; final complexity must
include the historical recipients actually visited after the repair.
## Adversarial verification — ADVERSARY

Active role: **ADVERSARY**. Independent cases derive from the model above;
IMPLEMENTER-owned S43 fixtures were not read to design them. Production remains
untouched by ADVERSARY. Private build/cache/run are isolated.

- `../s43-adversary-pair-first.log`: BUILD SUCCESSFUL 4m16, clean exit. Native
  push is nonzero for both real Cow bodies. Both caller directions match before
  activation and during actual READY contact. ROOT applies .1 exactly once,
  with identical native push afterward. Ratio1 rejected movement crosses the
  anatomy without wall/contact or public material ray hit; positive actual
  width ratio is .125. This server-boundary proof uses original geometry
  extracted by the client and does not certify local prediction.
- `../s43-adversary-scale-first.log`: independent production RED1, BUILD FAILED
  2m36, `Rejected scale transition retained material relation`. After contact
  and .1 carry, body SCALE changes 1 to12 against support8. Eligibility is false;
  ROOT .13 adds neither displacement nor a transport, but Contact stays alive.
  Oracle retained unchanged and returned to I7 before repair.
- `../s43-adversary-wall-mutant.log`: removing only the eligibility filter in
  an external main-source overlay produces the expected no-wall failure:
  rejected movement ends at x17.175 instead of x22. BUILD FAILED 2m12 after
  successful compilation. No repository production source was swapped.
- `../s43-adversary-scale-postfix.log`: unchanged body-scale RED passes against
  I7, BUILD SUCCESSFUL 2m35, clean exit. Actual ratio becomes1.4999999338;
  Contact retires, ROOT .13 creates neither movement nor a new transport.
  The prior .1 transport is distinguished from new carry debt.
- `../s43-adversary-support-scale-first.log`: **production RED2, FR-011**,
  BUILD FAILED 1m06 on AnatomyMovement SHA256
  `08b178163c3cdd99853fae87df416a8efc9651d68b0e314a8e7e425487ce343e`
  and PlatformEntityMixin
  `2f699f794858242351cf0470efc45d26443f4197373d73c548936fdf3fa6a9a4`.
  The real relation first applies .1; support SCALE then changes8 to.5 while
  body SCALE remains1 and both entities remain ordinary. Eligibility becomes
  false. ROOT .13 applies no new motion/transport, yet Contact revision1 and
  public `AnatomyApi.support(body)` still expose the now-rejected support.
  The fixture does not insert an artificial carry call before ROOT. This
  independently exposes missing dependent retirement on support dimensions;
  a body's own dimension callback cannot certify that symmetric boundary.
  Returned to IMPLEMENTER, oracle retained unchanged.

- `../s43-adversary-local-first.log`: BUILD SUCCESSFUL 1m03, clean exit.
  Actual LocalPlayer keybindings hold material contact, actual server ROOT
  carries .25, native tracked Cow interpolation settles and both peers converge.
  Both directions of native push give the same nonzero player response before
  and after the material carry. No IMPLEMENTER fixture was read to design it.
- `../s43-adversary-support-scale-postfix.log`: unchanged symmetric FR-011
  oracle passes I8, BUILD SUCCESSFUL 1m26, clean exit. Actual width ratio2;
  the now-ineligible support relation retires before ROOT, without new debt.
- `../s43-adversary-index-second.log`: BUILD SUCCESSFUL 2m08, clean exit.
  Distinct object identities sharing IDs/UUIDs remain distinct. Same-contact
  updates preserve sequence; replacement removes old membership; clear and
  both level teardowns remove membership. A body already in the next dimension
  retains its sequence watermark when its old support is torn down. The
  membership list remains a stable snapshot during clearing. 2,048 unrelated
  contacts and 1,000 local lookups visit zero global CONTACTS entries.
- `../s43-adversary-index-mutant.log`: expected runtime RED after successful
  compilation, BUILD FAILED 1m42. An external source overlay replaces only
  supported-by lookup with the old global scan. The unchanged instrumented
  oracle observes 2,050,000 global entries and rejects it.
- `../s43-adversary-cost-first.log`: BUILD SUCCESSFUL 2m05, clean exit.
  A warm local broadphase visits at most three candidates despite16 far supports.
  1,000 dimensions revalidations plus negative clearance/ray queries add zero
  endpoint/joint evaluations, movement or valid-contact mutation.
- `../s43-adversary-push-mutant.log`: expected runtime RED after successful
  compilation, BUILD FAILED 1m16. The external mixin overlay cancels native push
  during material contact. The unchanged oracle rejects zero impulses versus
  native nonzero responses on both real bodies. Production files are untouched.
- Deterministic spatial projection: `../s43-adversary-private/spatial-snapshot.png`
  and JSON use actual AABBs captured in the local-input proof. Top/side views
  compare initial and held player against the identical static material box;
  reviewed visually for partial overlap and unchanged contact height. This is
  a geometry projection, not a Minecraft renderer capture.

The first index launch failed due solely to the private mixin package claiming
production package classes; moving the mixin package resolved infrastructure.
The first I8 physical invocation also found an obsolete private cost-harness
call after the public revalidation API changed; only that harness call was
adapted. Neither failure changes the retained body/support physical oracles.
The derived index's identity/lifecycle/local lookup is independently recertified;
retirement's downstream receipt invalidation is independently **REWORK**, not
covered by the index's green local lookup. Final zero-change closure is blocked
by the following concrete production RED3. Human-only needs remain G9.

**RED3 — local dimensions retirement scans unrelated receipt recipients.**
`../s43-adversary-receipt-cost-first.log`, BUILD FAILED 1m08 after successful
compilation. A separate copy of the unchanged support-scale physical oracle adds
128 strongly retained local ServerPlayer fixtures. Their histories are genuinely
populated by `AnatomyTransportReceipts.record` using the existing action-scoped
GameTest tracking seam, with one coherent receipt each for their own UUID;
history size1 is asserted before measurement. No player has a receipt for the
Cow body being retired. This probes receipt-store bookkeeping, not a claim of
128 live network clients or 128 physically observed carries. No production owner
or source is replaced. A test-only WrapOperation counts actual iterator.next
visits inside invalidate, active only during the support dimensions transition.

Support SCALE8 to.5 correctly clears the body's real material relation, but
`refreshDimensions -> revalidateDimensions -> clear -> AnatomyTransportReceipts.invalidate`
visits every global recipient history. Assertion:
`Local dimension retirement visited unrelated receipt recipients: 128 for a Cow with zero recipient receipts`.
The downstream global scan predates S43; the new retirement route inherits it
once per cleared dependent, so local dependent count alone does not describe
its cost. Classification: production HOT_TICK locality / continuous performance
protocol, not a physics failure and not evidence that a TPS budget was exceeded.

Reproduction: JDK25, `./gradlew runClientGameTest --init-script
../s43-adversary-private/receipt-cost.init.gradle
'-PscalebrewsBuildDir=../s43-adversary-build' --project-cache-dir
../s43-adversary-cache --console=plain` (arguments form one shell command).
Candidate SHA256 remains: AnatomyContactState
`87a74dd276907323524d5174c73442614f15f57cf231b62e6ee4be2c04109478`,
AnatomyMovement
`cb17a8f3f6fd535188bf95d8ba7da312235c8fff6c58a532d51b5f82eea713e6`,
PlatformEntityMixin
`fe6eaa1ae5d425665599c700b1c8b7aaec36dea587e8b0dfd8888b1a38b0ab98`.
Oracle and infrastructure retained independently for repair revalidation.
Returned to IMPLEMENTER: **no funciona, arréglalo**. S43 remains open.
### Final I9 revalidation and independent zero-change pass — ADVERSARY

The `dd878050` executions are intermediate only: unchanged RED3 passed in
`../s43-adversary-receipt-cost-postfix.log` (1m41), independent receipt lifecycle
passed in `../s43-adversary-receipt-foundation-first.log` (1m27), and the first
complete eight-case batch passed in `../s43-adversary-final-private.log` (1m25).
Its membership-retirement mutation failed as expected in disconnect (1m39).
IMPLEMENTER withdrew that representation for unbounded WeakHashMap queue work;
none of those greens certify final hot cleanup.

Final candidate AnatomyTransportReceipts SHA256:
`f98300e23cb6b35e5b29578fd24989446f3f49b61079e486b0eaf64d2572f7d1`.
The three I8 hashes above remain unchanged. On this candidate:

- `../s43-adversary-public-final-first.log`: all three promoted regression
  entrypoints pass together, BUILD SUCCESSFUL 1m15, clean exit, SHA256
  `e53b88f14ecd9625d9c903c86f34ba7befe00a866c27b5b41dcbb9e81c5422c5`.
  Support retirement remains a real-contact/ROOT proof; the 128 unrelated
  histories remain genuinely recorded; local retirement visits zero foreign
  receipt entries and applies no new carry. The stored-fact receipt proof covers
  equal UUID values, historical recipients with no current riders, replacement
  connections sharing ID/UUID, duplicate append, once-only claims, saturation,
  TTL, diagnostic pruning, disconnect and server clear. Its private append seam
  isolates bookkeeping and is not a claim of physical origin.
- `../s43-adversary-receipt-mutant-final.log`: expected runtime RED after
  successful compilation, BUILD FAILED 2m, SHA256
  `047b008c35c3f94f3cb91f6abb7af489fbb1d81ceee8f2b8f052e7f1ea3ce09b`.
  Removing only derived-member retirement in an external final-source overlay
  leaves disconnect membership wrong. The same lifecycle oracle detects it.
- `../s43-adversary-final-current.log`: all eight original independent holdouts
  pass, BUILD SUCCESSFUL 2m06, clean exit, SHA256
  `5e86d7f27c81178411971e173b85319505b8196a40545f70285314adc45f2cda`.
  Includes native pair push/no-wall, both scale directions, native local-player
  input plus server/client carry/push, 1,000 warm read/revalidation iterations
  with zero extra endpoint/joint evaluations, 2,048 unrelated contact histories
  with zero global lookup visits, receipt lifecycle and the unchanged RED3.
- Installed runClientGameTest classpath uses Guava33.6.0-jre, JAR SHA256
  `dc573e1fca4fd5454f4a5fd3d7da2df03002876a4175bafc14a95980dd7713b3`.
  Independent javap of Segment's key/value queue drains confirms the limit16;
  `../s43-adversary-mapmaker-segment.txt` preserves the actual bytecode. Reverse
  UUID keys use value equality; History strongly owns its shared weak-value
  bucket, and that bucket retains only weak recipient identity plus UUID data.
  No entity/world reference is added through the reverse values. Explicit
  removals and diagnostic expiration unlink empty buckets. Cold server teardown
  and metrics remain global; hot invalidation is scoped to historical recipients.
- The final native-input spatial sample reproduces the first sample's three
  AABBs within1e-10. Durable projection and metadata are in
  `docs/evidence/S43/native-contact-projection.png` and JSON. Actual top/side
  geometry shows partial foot overlap and preserved height; the immutable AABB
  projection has no RNG/render/world-seed dependency. This supplements machine
  assertions; it is not a renderer screenshot or full-world visual acceptance.
- `git diff --check` passes. The public build log passes the uncapped deprecation
  guard with21 reported intentional test-only diagnostics. Three adversarial
  entrypoints and their inactive-by-default observer/counter are confined to
  GameTest; default manifest includes them. No production source was edited by
  ADVERSARY. Production changes remain exactly the four planned I7-I9 files;
  full diff review finds no separate owner, packet/replay/claim change, geometry
  preparation or unrelated scan in the new hot paths.

Foundation verdict for the extended scopes: AnatomyContactState's derived
membership and AnatomyTransportReceipts' derived recipient membership are
**TRUSTED after independent recertification**. This is structural locality,
lifecycle and work reuse evidence, not a global TPS/p95/heap/GC-soak measurement.
The final complete review of I1-I9, production and promoted infrastructure
requires zero additional source changes. Independent S43 verdict:
**funciona, continúa**. G5 task6 may close after all required IMPLEMENTER final
regressions finish cleanly; its experimental final was still running at this
handoff. Legacy removal, later gates and final human needs remain separate/G9.
## Failures, final review and closure

Closed by the final review recorded below. Earlier pending/RED paragraphs are
the chronological execution record and do not supersede that final verdict.

IMPLEMENTER I8 execution: original prepared focal passes 28 ratio/policy
comparisons and 14 nonzero native push controls; the full prepared lane passes
24 scenarios, 2/2 required tests. The actual local-player differential and the
separate contact-index bookkeeping proof both pass. Ordinary build/server passes
483/483 required tests, clean exit 1m53. The three production paths are unchanged
since these executions. The uncapped deprecation guard passes the focal
compilation (273 intentional test-only adapter diagnostics) and ordinary build
(incremental, zero diagnostics). This is executed candidate evidence;
independent foundation recertification, mutations, snapshots, final default
client composition and both closure reviews still govern acceptance.

Independent I8 RED3: `../s43-adversary-receipt-cost-first.log`, BUILD FAILED
1m08, actual Cow contact followed by support SCALE8→.5. Contact retirement is
correct, but the existing clear dependency scans 128 actually populated unrelated
recipient histories despite this Cow having zero receipt recipients. This is
class 1 production/cost in HOT_TICK, not a physical or TPS claim. The contact
index itself passed independent lifecycle/identity/locality checks and targeted
mutations; the complete retirement path did not. I9 extends the receipt owner's
derived lookup only. The complete amended IMPLEMENTER I1–I9 plan has been reviewed
with no additional changes; independent reconvergence is required before code.
The I8 default-client run already dispatched remains intermediate evidence.

After independent I9 convergence, IMPLEMENTER extends only the existing receipt
owner's derived lookup. HISTORIES remains the authoritative weak-identity
recipient-to-body store. The inverse uses UUID value equality and weak recipient
identity, so a body's historic riders are found even after dismounting. Each
authoritative History keeps the shared weak inverse bucket alive; the inverse
uses UUID value keys, weak bucket values and weak recipient identity, avoiding
premature bucket collection while another recipient history exists and retention
after all owners expire. Append creates membership; prune-and-find, metrics pruning, invalidation,
disconnect and server clear retire it. Receipt content, v1/v2 claims, causal
serials, saturation, TTL and packet contracts are unchanged. Local invalidation
is O(R_body), including O(1) for zero receipt recipients. Dimensions validation
is O(1 + D_local + existing ancestry + sum R_retired_body), with no unrelated
contact/recipient scan or geometry preparation. Diagnostic metrics and server
teardown retain their existing global scope. This modified receipt foundation
remains UNPROVEN until independent lifecycle/locality recertification passes.

IMPLEMENTER static-cost correction within I9: the first `dd878050` candidate
used WeakHashMap, whose installed JDK implementation expunges the entire stale
reference queue on access. That is unsuitable for a bounded hot lookup. The
revised representation reuses installed Guava MapMaker with weak values and an
authoritative History reference to the bucket. UUID keys still use value
equality; collection removes the key/value entry without retaining entities or
histories. Installed Guava33.6.0 exposes DRAIN_MAX16 (bounded queue maintenance).
This changes no planned contract or physical behavior. Executions already
dispatched against `dd878050` are intermediate and will repeat as applicable.

Final IMPLEMENTER candidate `f98300e2`: build/server passes 483/483 (3m01,
`../s43-server-i9b-full.log`); full prepared original composition passes 24
scenarios on distinct native ticks, 2/2 tests, 28 seven-category comparisons and
14 nonzero native push controls (1m19, `../s43-prepared-i9b-full.log`). The
dedicated player/boat receipt/reference and reconciliation proof passes actual
RTT0/100/200ms, zero vanilla corrections and client/server convergence (4m04,
`../s43-receipt-i9b-reconciliation.log`). The uncapped deprecation guard accepts
21 intentional test-only diagnostics in the final build and no production/native
diagnostic. Full default client now32 includes all three promoted independent
regressions, with the same five S43 entries in the focal selector. Its execution,
experimental-cart recheck, source/artifact evidence and independent final
foundation/mutation/snapshot review remain pending. The full IMPLEMENTER code
review found no further production change needed across I1–I9; this is not yet
the final closure pass.

Final IMPLEMENTER execution completed: default client32 exits cleanly in4m39
(`../s43-client-i9b-full.log`, SHA256
`d82d3fd51c1a074c9c2f58f07c795d7e7b6cfcc754f3d23b807a60192f850739`).
Both independent physical retirement controls, the128-recipient locality control,
receipt lifecycle foundation, actual local-player differential and contact-index
bookkeeping execute alongside the prior client regression. The exact default
manifest was archived before the final experimental selector, SHA256
`ac76578f8526286ff69e1d6b8fa8be270bd8245a60b562f5fa582402abe08dd8`.
The experimental real NewMinecartBehavior control also passes (59s,
`../s43-experimental-i9b.log`, SHA256
`ff3bf4e0c69ba08097a7567c0a84d778d2c3ed963a4e37db7015821052d281a4`)
with occupied ROOT, physical rail release/reacquisition, native interpolation,
ratio/policy-off and obstruction controls. Generated resources were restored
to the unchanged default manifest afterward; this did not alter source or rerun
physics.

`../s43-final-source-review.json` (SHA256
`9103eef2c084887c2ef3274b2ded46c9181825beab0855b376ec8440baff9898`)
confirms exactly four production paths changed from S42 and every other main/
client production path unchanged. `../s43-final-evidence.json` (SHA256
`9ea22db764377246e1021a0b4d0fc966461486e9b41b989ba1b0ba67c9baf0ee`)
collects executed logs, distinct native scenario ticks, manifest and artifact.
Production beta9 JAR SHA256
`883cba595594adffe87be37365ec763bb0b8d1e23bb1ce1dee005831cfae0f8d`
has correct metadata and no class/nested-class leak against all367 GameTest
source classes. Installed MapMaker's bounded16-entry value-reference drain is
captured in `../s43-impl-mapmaker-bytecode.txt` (SHA256
`aeb9c6e5d6a1f9ebb3fb3d3b0703aa2d03048ec1775b14634f21143fa9eddf77`).
The complete IMPLEMENTER final pass over I1–I9, cost/ownership, preserved gameplay,
fixtures and executed regressions needs zero further production changes.
Independent final foundation/sensitivity/snapshot review remains required;
S43 acceptance, G5 task6 and the closure commit are still pending that verdict.

Final IMPLEMENTER closure pass: the independent final verdict above recertifies
both extended foundations and requires zero changes. All required final
IMPLEMENTER regressions have completed on the same four production hashes,
including the experimental lane that was still pending at the adversarial
handoff. IMPLEMENTER viewed the stored top/side projection and its immutable
actual-AABB metadata: preserved support height and partial foot overlap agree
with the native-input assertions. It is a geometry projection, not renderer or
whole-pack acceptance. The final source/artifact ledger, runtime mutant failure
and both full reviews leave no source/evidence blocker for this sprint.
S43 and G5 task6 close locally through this sprint's own closure commit.
G5 tasks7–9, G6–G9, publication and final human QA remain open. Verify HEAD and
the clean working tree before advancing to the next sprint.
