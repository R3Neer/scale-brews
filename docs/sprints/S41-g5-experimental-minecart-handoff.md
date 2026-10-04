# S41 / G5.8 — experimental minecart material and rail handoff

Status: closed locally after independent runtime holdouts and a complete zero-change source/evidence review.
Active role for this section: **IMPLEMENTER**. S40 closed locally at `4f23cf4c`.

## Scope and thesis — IMPLEMENTER

At the end of S41, a real cart constructed in a Minecraft 26.2 world with
`MINECART_IMPROVEMENTS` enabled uses one canonical living-material contact
only while off rails. A physical rail entry retires that relation before
Minecraft's experimental track physics takes over; a later physical rail exit
can acquire a fresh eligible support. A cart occupied by a local player must
not receive a duplicate material carry or a stale client interpolation after
that handoff. The cart remains server-authoritative while the local player
rides it; the client receives native experimental interpolation. Acceptance
requires the actual `NewMinecartBehavior` on real cart instances and native
movement/ticks; manually changing `onRails`
or replacing the behavior field cannot prove the thesis.

This is the remaining experimental part of FR-007/065 within G5 task 1.
FR-009/010/053/055-058/093 and NFR-007/008/033 constrain ownership,
eligibility, push and cost. S37 certified ordinary `OldMinecartBehavior`; it
does not certify this variant. G5 legacy deletion, broad nonparticipant
equivalence, G6-G9 and human pack QA remain separate. Human-only checks are
deferred to completion of the epic as requested.

## Current state and mainline inspection — IMPLEMENTER

Fresh `origin/main` was fetched at `b5e62535` before this plan. It has not
advanced since S40. Its wolf/tiny-mount gameplay, input and rendering changes
are independent and must be preserved in later integration. The working
`chatgpt-editing` tree is clean at `4f23cf4c`.

S37 prepared proof used real carts and rails in a normal feature world. The
installed 26.2 `AbstractMinecart` constructor chooses `NewMinecartBehavior`
only if `Level.enabledFeatures()` contains `MINECART_IMPROVEMENTS`;
`ServerLevel.enabledFeatures()` reads `WorldData.enabledFeatures()`. The new
behavior has a different track iteration and explicit client interpolation
steps. `BodyClassification.category` currently admits an `AbstractMinecart`
as `minecarts` only while `isOnRails()` is false; `AnatomyMovement` rechecks
eligibility after movement. Whether the experimental behavior updates rail
state early enough for this handoff, and whether its client interpolation
coexists with material receipts, are hypotheses for S41.

Fabric client GameTest `TestWorldBuilder.adjustSettings` can modify the real
`WorldCreationUiState` before world creation. The intended fixture expands
`WorldDataConfiguration.enabledFeatures` with the installed minecart flag and
then asserts the flag and behavior on both server and client. This route is
unproven until an actual world starts with the expected behavior. The fixture
must not use a post-construction behavior swap or merely a spoofed rail bit.

Installed `Entity.getControllingPassenger()` returns null unless a vehicle
overrides it; `VehicleEntity` and `AbstractMinecart` do not. Thus occupying a
cart does not make it locally authoritative. `NewMinecartBehavior.tick` takes
the client interpolation branch on a client level. This corrects the initial
I3 authority assumption identified by the independent ADVERSARY RED1.

## Provisional implementation plan — IMPLEMENTER

- [x] I1 Establish an isolated experimental client/integrated-server world
  with the actual feature flag and prepared exported canonical cow catalog.
  Assert `enabledFeatures()` and `NewMinecartBehavior` on real server and
  client carts; compare an otherwise equivalent ordinary-world control.
  If the proposed world-creation settings do not persist, revise this plan
  before implementing a replacement harness.
- [x] I2 In that world, use real off-rail cart motion/ticks to acquire a
  named canonical piece. Prove one ROOT translation/rotation carry with
  passenger and no `PlatformState`, then a physical rail entry that retires
  contact and leaves native track motion in charge. Exit physical rail and
  reacquire a fresh prepared support, with a new contact sequence and one
  later ROOT carry. Include an ineligible ratio/policy pair and a world-block
  obstruction so ordinary push and no-debt invariants remain visible.
- [x] I3 Exercise a server-authoritative cart occupied by the local player in
  the feature-enabled client/server world through off-rail material, rail
  entry and later exit. Assert native authority on both peers without forcing
  a controller, and observe actual client/server cart and rider positions,
  contact identity, experimental interpolation packets/steps, and transport
  sequence across ROOT and rail ticks. One server material carry must have
  one native client presentation; a stale lerp or duplicate visible move must
  fail. Compare an unoccupied cart and a remote observer replica without
  assuming that occupancy transfers physical authority.
- [x] I4 Repair only classified product defects, preserving Minecraft's
  experimental track implementation and current mainline gameplay. Audit
  HOT_MOVE_QUERY/HOT_TICK/HOT_NETWORK locality, geometry reuse, allocations
  and retention. Run prepared/ordinary build and full client where affected,
  then independent holdouts, mutation/fault checks and a complete zero-change
  review. Record executed evidence in `docs/VALIDATION.md`, update the G5
  canonical plan and create one logical local commit. No push or release.

## Acceptance boundaries — IMPLEMENTER

The experimental feature flag and behavior class must be observed at runtime,
not inferred from a setting request. Rail entry/exit must result from physical
blocks and native ticks. A success based only on server contact state cannot
certify client interpolation or the occupied cart. If enabling the feature
requires a separate native game world rather than the proposed GameTest
builder, the test plan must return to preimplementation convergence before
any product edit. Actual external-mod pack compatibility remains for epic QA.

## Plan revision after ADVERSARY RED1 — IMPLEMENTER

The first thesis/I3 incorrectly treated minecart occupancy as a client
authority transition. I checked the installed 26.2 Entity/VehicleEntity/
AbstractMinecart methods and accepted the adversary's RED1. The revised plan
keeps native server ownership and asks the client lane to measure experimental
interpolation and rider presentation under that ownership. No product or test
code changed. This revised plan requires another independent preimplementation
pass before implementation starts.

## ADVERSARY preimplementation model

Active role: **ADVERSARY**. Independent preimplementation pass at S40
4f23cf4c; origin/main b5e62535. No production or IMPLEMENTER tests changed.

Model: the feature-enabled world must select the real experimental behavior
before constructing the actual carts. An eligible off-rail body owns exactly
one material relation; a physical rail handoff retires that relation before
native track/interpolation consumes its motion. Returning off rail acquires a
new relation through real collision and can consume one later material interval.
Cart/rider position alone cannot establish ownership or absence of duplicate
carry. Inputs from an occupant, controlling-passenger authority and observer
interpolation must be distinguished. Physical eligibility remains local to the
pair; unmatched carts and other actors retain ordinary push and block behavior.

**Plan RED1 — impossible locally authoritative cart assumption.** The thesis
and I3 require a locally controlled occupied cart and contrast unoccupied/
server-owned behavior “where control changes authority”. Installed vanilla26.2
Minecart, AbstractMinecart and VehicleEntity do not override Entity's
getControllingPassenger, which returns null. Entity.isLocalClientAuthoritative
requires a non-null controlling passenger; therefore the client cart remains
non-authoritative even with the local player riding. NewMinecartBehavior.tick
unconditionally takes its client lerp branch whenever the level is not a
ServerLevel, then reads physical rail state; it does not simulate local cart
track motion based on rider authority. AnatomyMovement.predictsBody consequently
rejects client cart prediction, and rejects the mounted player's body because
its root vehicle is the cart. Merely occupying the cart does not flip either
physical authority to the client.

Operational counterexample: enable the flag, create Minecart normally, mount
the local player, and observe cart.getControllingPassenger()==null,
cart.isLocalInstanceAuthoritative()==false on the client and
AnatomyMovement.predictsBody(cart)==false. This valid native fixture cannot
satisfy a lane predicated on client-owned physical cart carry. A forced
controller/authority swap would fabricate the path, as a forced behavior swap
would. Revise I3/thesis to certify a local rider of a server-authoritative
experimental cart, alongside unoccupied/other-observer replicas. Explicitly
observe native ownership and the experimental interpolation packet/step path;
require one server material carry, native rider positioning, correct client
presentation and physical rail handoff without inventing a client authority
transition. This is a plan/ownership contradiction, not an executed product RED.
Verdict: **no funciona, arréglalo** until that boundary is reconciled.

Reproduction of the code evidence uses installed javap -p -c against
minecraft-merged-deobf-26.2.jar for Minecart, AbstractMinecart, VehicleEntity,
NewMinecartBehavior and Entity. Captured bytecode is in
../s41-adversary-private/vanilla-minecart.txt and vanilla-vehicle.txt:
Entity.getControllingPassenger returns null; isLocalInstanceAuthoritative uses
isLocalClientAuthoritative on client, whose null-controller branch returns false;
NewMinecartBehavior.tick dispatches to lerpClientPositionAndRotation on client.
Current Scale has no minecart controller/authority override. No Minecraft
runtime was executed during this plan pass; feature-world creation remains
unproven as I1 already states.

Performance model: HOT_MOVE_QUERY/HOT_TICK for local material/contact and rail
checks, HOT_NETWORK/HOT_PRESENTATION if experimental interpolation integrates
with new evidence. Variables are local candidate pieces P/C, carts B, native
track substeps M, observers O and interpolation/history steps H. The world flag
belongs to fixture creation/preparation, not per-query behavior reconstruction.
Audit that native substeps cannot apply one material interval repeatedly, that
per-observer work does not duplicate geometry preparation, and that rail
retirement does not retain material/interpolation history indefinitely or clear
unrelated native track motion. Bound any new retention and preserve ordinary
carts' costs. No allocation/timing/FPS claim is made in this plan review.

Spatial/temporal evidence will compare deterministic material/rail/rider and
interpolation snapshots against equivalent native controls, beside contact,
receipt and position assertions. Unrevealed holdouts and mutation details stay
private. This pass does not close the experimental category, G5 or pack QA.

## ADVERSARY convergence after authority revision

Verdict: **funciona, continúa** for the revised preimplementation plan. RED1
is resolved at the plan boundary: the cart retains native server authority,
occupancy does not manufacture a controlling passenger, and I3 now requires
observing the actual experimental packet/step interpolation and rider on the
client. The unoccupied and remote-observer controls no longer assume an
ownership transition that vanilla does not provide.

I1 still requires a real feature-enabled world and the actual behavior on both
peers; I2/I3 require physical rail entry/exit, fresh contact and one server carry
with one client presentation. Those remain executable acceptance obligations,
not results of this review. The settings route is explicitly unproven and must
return to convergence if it fails. The complete revised plan introduces no new
contradiction against the installed authority/behavior paths inspected above.
No production or IMPLEMENTER test was changed and no runtime certification,
experimental-category closure, G5 closure or human pack QA is claimed. Private
holdouts remain separate until a candidate is delivered.

## IMPLEMENTER runtime candidate

The feature-enabled integrated world actually reports `MINECART_IMPROVEMENTS`
and constructs `NewMinecartBehavior` on the server cart and client replica.
The control world reports the flag absent and `OldMinecartBehavior` on both.
The local rider does not acquire physical client authority: the occupied cart
has no controlling passenger, and the client observes native experimental
interpolation steps. The prepared original cow's named `root/body/cube_0`
first supports the occupied off-rail cart with no legacy `PlatformState` owner.
One server ROOT translation advances transport once; a separate ROOT yaw
interval follows the same stored local point and advances it once again.

Physical rail blocks are built on the cart's actual lane. Native cart ticks
enter the rail, retire the canonical contact, and ignore a later ROOT movement
of the old cow. Further native ticks leave the track. A second prepared cow
then acquires a new contact, and its subsequent ROOT moves the cart exactly
0.2 blocks with one transport-sequence advance. The
client converges on the occupied cart and rider through both boundaries.
An unoccupied server cart on a separate physical track supplies a remote
client-observer control with native interpolation steps and no anatomical
contact. Ratio-rejected and profile-policy-disabled occupied carts retain
vanilla collision and push; a real block wall clips a different supported cart
and removing it creates no delayed carry. The policy-off case restarts the
prepared session with an explicitly disabled cow profile after the positive
case. No production source changed in this candidate.

The initial empty-cart fixture failed because experimental vanilla behavior
picked up the cow as a passenger, making that pair ineligible. Mounting the
local rider before material contact established the requested occupied-cart
case. An initial rail fixture followed the cow center rather than the moved
cart's actual lane; after aligning physical blocks to that lane, native entry
and exit passed. A subsequent ROOT-yaw test revision initially failed to
compile because its server lambda lacked a local `level`; this test-only error
was fixed before the executed yaw pass. These fixture failures do not prove a
product defect.

The current focal experimental run including the policy-off case and exact
reacquisition carry passed in `../s41-experimental-exact-reacquire.log`
(SHA-256 `E7D52C40E6527E20442D2F97C6788426E9300FAF3366024F4A676EC83414E079`).
The preceding policy-control candidate passed in
`../s41-experimental-policy-control.log` (SHA-256
`0F3AECD84B865D13C423D96A624C5D6B0EAC04A4D70A18CAF43D9C1B72BBB0AE`).
The preceding ROOT-yaw candidate passed in
`../s41-experimental-root-yaw-fixed.log` (SHA-256
`F892E1D78A2E1B6B5FAABAC2A648705CDC5EA9F969655E811BDB4A906CF37C91`).
The ordinary-world feature/behavior control passed on the final test source in
`../s41-ordinary-final-tree.log` (SHA-256
`89325AAE46536D40E6E93F784CC213DA902E4105B8B8852D10B2F4C00DF18F67`).
`build runGameTest` passed **466/466** ordinary server tests in
`../s41-ordinary-full.log` (SHA-256
`C5222CBBF556A9EEE562E58B0DA09A6851D7F08420B2324C57B24768C2AD992C`).
The prepared suite passed **2/2** (23 scenarios) in
`../s41-prepared-full.log` (SHA-256
`E771FED48F128850240EF09E4D89CFE4E28883690B51CA4C4D3C85E950F9AC23`).
The full general client suite passed in `../s41-client-full.log` (SHA-256
`DBE8568AA58868C3DE2490BF0DEE82DEB9AFFE61914C0792059644A37A259089`).
The independent ADVERSARY executions and complete unchanged-source review
below close this S41 boundary; whole G5 and the epic remain open.

To reproduce the focal run from this checkout, set
`JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true` and invoke
`runClientGameTest` with `-PscalebrewsS41ExperimentalClientOnly=true` and
`-PscalebrewsAnatomyCatalog=../s36-impl-run/anatomy-export`. For the ordinary
control, additionally set `SCALEBREWS_S41_WORLD_VARIANT=ordinary`; omit it for
the feature-enabled run. The recorded runs used Gradle's offline mode,
`-I ../s41-impl.init.gradle`, `--project-cache-dir ../s41-impl-cache`, and
`-PscalebrewsBuildDir=../s41-impl-build` for isolation from other probes.

## IMPLEMENTER source review and cost boundary

The complete source review after adding the policy-off control and exact
reacquisition assertion produced no further source changes. The focal test
file has SHA-256
`2485FD711E619F1984CDCDCDD59B3C95052001F64241A195423B932DD2BA73ED`;
`build.gradle` has SHA-256
`E5D9AB337A385A3A15A03F62A0E81A4622253CDF50021B74E82773F97B586664`.
The Gradle selector changes only the GameTest manifest. There is no S41
`src/main` or `src/client` production diff, new hot loop, runtime cache,
reflection, serialization or persistent state. The new work is
**OFFLINE_TOOLING**; world feature selection and original catalog loading occur
in fixture preparation. Existing production HOT_MOVE_QUERY/HOT_TICK and
native experimental substep/interpolation costs remain subject to the
independent review and the later integrated G9 benchmark. The exact experimental
and ordinary feature-world proofs both passed with this unchanged source.

## ADVERSARY candidate review — executed independently

Verdict: **funciona** for the S41 experimental-minecart boundary. Production
remains identical to S40: 370 source/resource hashes from the prior adversarial
manifest match, and git diff reports no main/client production change. The
adversary changed no production or IMPLEMENTER test. All lanes below used
separate build/cache/run directories and actual native feature-enabled worlds.

- `../s41-adversary-private/original-aligned.log`: GREEN, 1m36s. Two actual
  experimental carts acquired the original exported cow body through native
  falling ticks. One carried the local player; the other was unoccupied. Both
  remained non-authoritative on the client, with null controlling passenger.
  Four genuine ROOT translations included a vertical interval; immediate
  server deltas and transport advances were exact, and both settled native
  replicas matched all four positions. The native rider kept the same seat
  offset. Gravity/voluntary velocity was neutralized after real acquisition
  for this isolated passive-presentation comparison; this lane does not prove
  that carts with ordinary push/momentum have zero interpolation lag.
- `handoff-unblocked.log`: GREEN, 1m12s. Independent prepared cube geometry,
  a native pig passenger and real world ticks exercised material-to-rail and
  rail-to-new-material transitions. No behavior swap, spoofed rail bit or
  repeated cart.tick calls inside one server tick were used. Entry at
  (5.600000000000003,31.1,2.5) retired contact; a later old-support ROOT could
  not alter position/receipt. Physical exit occurred at x17.36773607314576.
  The subsequent support was inserted only after exit, acquired contact
  sequence 2, and consumed exactly one later ROOT (.07,0,.04). Client state
  also left the actual rail. Its later position includes legitimate native
  push/movement; it is not compared to a historical settled endpoint.
- `rotation-first.log`: GREEN, 1m08s. On an actual experimental cart with
  native material acquisition, a captured ROOT yaw interval of 45 degrees
  matched an independent rigid-rotation formula at radius .7. The candidate
  delta differed from the analytic value by about 6e-8 blocks. The native pig
  seat refreshed once, transport sequence advanced once, and a second drain
  did not replay it. This directly exercises the real capture/commit/drain
  seam using actual entity state, not fabricated handles. It is server yaw
  evidence, not a client yaw-framebuffer claim.
- `empty-native-retirement.log`: GREEN, 56s. Removing a native passenger from
  an experimentally constructed cart allowed vanilla pickup of the overlapping
  prepared support. When that cow became a vanilla passenger, the canonical
  cart contact retired. There was no support cycle or second anatomical anchor.
- `public-final-candidate.log`: GREEN, 1m35s. Independently reran the current
  IMPLEMENTER oracle including real original catalog, occupied rail handoff,
  native interpolation steps (9 and observer 4), exact fresh carry, ratio,
  policy and wall/no-debt controls. `public-ordinary-control.log`: GREEN,
  1m06s, actual default world and OldMinecartBehavior on both peers.

The root oracle was calibrated with an isolated production-source overlay:
`mutation.init.gradle` compiles a private copy of PlatformEntityMixin that
omits only MaterialIntervalRuntime.commitRoot. `root-commit-mutation.log`
produced the expected RED at phase 0: actual cart x2 instead of expected x2.17.
The repository file and the normal build were untouched. This distinguishes
real event delivery from an oracle that could pass without the ROOT mutation.

Deterministic coordinate snapshots `carry-contact-sheet.png` and
`yaw-contact-sheet.png` were generated from executed logs and inspected.
Their JSON metadata and Python generators preserve frame/time, projection,
inputs and reference/candidate values. The carry projection covers settled
replicas after 50 client ticks; the yaw projection covers the captured server
interval. They supplement assertions and do not replace client draw or human
pack QA. Hashes and exact local sources/commands are retained in
`../s41-adversary-private/evidence.json`.

Failure classification matters. The initial API-signature error and the
unquoted PowerShell build property were adversarial infrastructure errors.
An initial cube fixture expected pickup to be suppressed, but native pickup
making the support a passenger must retire contact under FR-005/013; its oracle
was corrected accordingly. Comparisons against old/current server endpoints
while native push continued were invalid zero-lag expectations: FR-053
requires that push to survive. The isolated comparison instead aligned the
cart with the support's horizontal center. Initial rail fixtures placed a
stone face at the exact integer material edge and placed the second cow's
vanilla AABB over the still-owned rail; those were physical fixture obstacles,
not a product RED. Lowering the track and inserting the next support after
actual rail exit preserved native blockage and produced the real-clock pass.
The IMPLEMENTER compile error reported at its line159 was repaired by that
role before subsequent executions. No correctly classified product RED remains.

Cost review: S41 adds only OFFLINE_TOOLING/test selection and fixtures. It
introduces no production branch, reflection, cache, lock, allocation or history.
The existing contact/material/native-network paths remain HOT_MOVE_QUERY,
HOT_TICK, HOT_NETWORK and HOT_PRESENTATION; their local pieces P/C, bodies B,
native track substeps M, recipients O and histories H remain the scaling
variables. The full native-clock lane avoids artificially accumulating many
substeps in one tick, and uses existing budgets unchanged. Prior measured
geometry reuse/bounds remain applicable to the identical production tree.
This pass makes no new whole-system heap, latency, TPS or FPS claim. G5 broad
nonparticipant/legacy work, G9, external-mod compatibility and human epic QA
remain outside this S41 verdict. Final source/evidence review produced no
production changes; git diff --check passed.

## IMPLEMENTER local closure

I verified the independent evidence inventory against the final public-test
source hash and inspected both deterministic contact sheets. The native-clock
handoff holdout supplies the physical world-tick evidence that the public
development probe's grouped cart ticks alone could not establish. Its original
geometry, occupancy, yaw, native-pickup and mutation lanes all have the limits
stated above; none is treated as human pack or client framebuffer acceptance.
The evidence manifest SHA-256 is
`316A7307BCAF2E6E9CA6CBEC57C92F8D755900658A911258A411EF6BCAFFEFA9`.

Both complete source reviews ended without further changes. The final focal
and ordinary controls passed on the same test source, alongside the ordinary
466/466, prepared 2/2 and full-client results already recorded. Only test and
documentation files are included in this logical local commit. S41/G5.8 is
closed for the actual experimental FR-007/065 boundary. G5 category and
water/rail/despawn/hardening/placement/anvil tasks now have their combined
S35-S41 evidence; broad nonparticipant equivalence and legacy retirement are
next. G6-G9 and human-only epic QA remain open. No push or release.
