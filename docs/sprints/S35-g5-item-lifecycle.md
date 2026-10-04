# S35 / G5.2 — item bodies and vanilla lifecycle

Status: S35 item slice closed locally after independent ADVERSARY review and
a final zero-change production pass. S33/G5.1 and the blocking S34/G4
reopening closed locally at `03efc6bd` and `80741805`. No S35 production
change was required.

## Scope and thesis — IMPLEMENTER

At the end of S35, an ordinary `ItemEntity` can acquire and retain a material
contact on a canonical prepared support, follow that support through the one
anatomical transport owner, and still age, become pickable and despawn through
Minecraft's own lifecycle. Releasing the material contact restores ordinary
item physics immediately. Real server tests must exercise the entity's move
and tick paths, not just `Platforms.supported` or a synthetic contact.

This is the item-body slice of FR-007 and FR-066, with FR-009/010/053/055-058
and NFR-001/004/007/008/033 as neighboring invariants. It does not migrate
boats/rafts, off-rail minecarts or falling blocks, nor authorize deletion of
legacy physics needed by those categories. Player pickup authority stays with
Minecraft. No new generic item age or pickup implementation belongs in Scale.

## State reconstructed — IMPLEMENTER

`BodyClassification.category` already admits `ItemEntity` as `items` while
ordinary state excludes dead, no-physics, spectator and passenger bodies.
`Platforms.eligible` resolves the canonical binding/policy before legacy and
checks ratio/cycles. `AnatomyMovement` owns current material contacts and
transport; `Platforms.supported` reads it for READY bodies. The existing
`PlatformItemMixin` substitutes only the block friction read in `ItemEntity.tick`
through `Platforms.friction`. That helper resolves canonical friction when a
current supported item has a material support, and leaves non-predicting remote
anatomy bodies on vanilla friction. These routes suggest reuse, but current
`PlatformTests` item carry assertions use `PlatformState` and
`PlatformPhysics.carry`, so they do not prove FR-066 on a canonical-only support.

The S33 prepared fixture shows how to register a canonical support and use
actual `Entity.move` within the single prepared server session without a legacy
surface. S09's physical transport owner, contact identity, fail-closed budget
and large-coordinate fixes remain required. Installed Minecraft 26.2
`ItemEntity.tick`, pickup and lifespan APIs were checked in the installed
26.2 bytecode. Its public `getAge`, `setPickUpDelay`, `hasPickUpDelay` and
`playerTouch` support direct lifecycle assertions. A near-expiry item can be
prepared through the vanilla save/load path with `Age=5999`, then exercised
by its actual tick; `makeFakeItem` also sets age 5999 but disables pickup and
is therefore not the ordinary positive case. No guessed private fields or
test-only lifecycle hook may stand in for actual vanilla behavior. The current local `main` is
`aeca64e1`; inspection before this plan found no newer local main commit than
the S33/S34 baseline, and unrelated mainline behavior remains protected.

## Provisional implementation plan — IMPLEMENTER

- [x] I1 Establish baseline on a canonical-only prepared support: actual
  `ItemEntity.move` acquires a `SurfaceContact` with piece/revision/normal and
  `PlatformState` does not own that item. Check policy off/ratio failure as
  no anatomical wall with vanilla pair behavior. Keep the fixture's geometry,
  world tick and cleanup deterministic.
- [x] I2 Verify one support ROOT/material change carries the resting item once
  with a valid anchor; unrelated movement, obstructions and budget exhaustion
  must not turn into teleport or duplicate carry. Diagnose whether generic
  anatomy already supplies this; repair only the missing owner boundary.
- [x] I3 Prove `ItemEntity.tick` continues Minecraft age and pickup-delay
  progression during contact and ordinary pickup/despawn after release. Test
  supported→released and nearby nonparticipant controls, plus water/ground
  friction behavior where the item tick actually consults friction. Preserve
  vanilla lifecycle callbacks and never freeze the entity wholesale.
- [x] I4 Run targeted prepared, ordinary, client if a shared path changes,
  independent adversarial tests, performance/locality review and a final
  zero-change pass. Record exact code and log hashes, update canonical G5
  status and create one separate local logical commit. Do not push or publish.

## Implementer candidate and evidence

No S35 production code changed. The existing canonical body classifier,
`Entity.move`/material dispatcher, `PlatformItemMixin` friction substitution
and vanilla `ItemEntity.tick` already supply the required positive route.
`S35PreparedItemLifecycleProof` now exercises a real item and a distant ordinary
control inside the prepared cow session. It asserts actual `Entity.move`
contact identity, no `PlatformState` ownership, one translation ROOT carry,
a separate yaw ROOT carry, no duplicate drain, age/pickup-delay parity, dry
ground friction, supported and released vanilla pickup, post-release gravity
and falling on its sampled idle cadence, and vanilla near-expiry despawn
prepared through save/load. The first
diagnostic RED combined `setPos` and yaw under one ROOT capture, producing a
two-serial fixture gap; separating those real mutations made each interval
causally valid. That RED was a test-setup error, not a production defect.

The final full prepared run `../s35-release-prepared.log` passed **2/2** and
`BUILD SUCCESSFUL` (SHA-256
`df679cfd0e091424cbd925728e811fbc6d73ff0739b595d5b62dbcf79c8b3165`).
The same code's ordinary `build` passed **465/465** and `BUILD SUCCESSFUL` in
`../s35-final-ordinary-build.log` (SHA-256
`84072e55de24ac020029dc7f547f08999b841034a9a4a372ca5d81f6b745c859`).
Proof source SHA-256 is
`7467b5ccef2d8ce6c13207ef0be4f849c8831ec6dfe99b31bd50a8c4783484de`.
No client runtime path changed, so a client rerun was not required. Independent
policy/ratio no-wall, water, obstruction/exhaustion, merge and
performance/locality checks are recorded in the ADVERSARY section below.

## ADVERSARY independent model and verification

Active role: ADVERSARY. S34/S33 commits80741805/03efc6bd were independently
checked against the final tested source hashes and separate logical scope;
only this S35 preimplementation document is untracked. Production and S33/S34
oracles remain intact. Local main is aeca64e1; unrelated product behavior is
protected. No S35 runtime success is claimed by this plan review.

### Requirement-derived model before candidate

FR-007/066 require an actual item body using the single accepted material core,
not a legacy PlatformState carry demonstration. Establish the prepared session,
canonical-only support, admitted items policy/effective ratio and exact material
contact identity through real ItemEntity.move. A valid contact must capture
current support/piece/revision/local point/normal/time authority; guessing the
support bounding box is insufficient. A policy-off/ratio/ordinary-state control
must preserve vanilla pair behavior rather than replace it with an anatomy wall.

Resting transport must occur once at the authoritative ROOT interval, even if
vanilla's item tick skips a stationary move that tick. Compare physical delta
with the same retained local material point, including nonidentity support root
orientation; record physical authority separately from presentation. An explicit
ItemEntity.tick after material transport may not add that carry again. Collision
obstruction, exhausted work, unavailable support and contact release may not
leave teleport, persistent velocity debt or a stale anchor. Reacquisition must
use a fresh accepted material sample, not restore the retired contact.

Item lifecycle is Minecraft-owned. Compare supported/released items and a
matched ordinary nonparticipant at the same actual world ticks, with explicit
merge/pickup proximity controls so callbacks do not silently delete a fixture.
Age/pickup-delay progression during contact, pickup/despawn after release are
the exact FR-066 acceptance contract. Ordinary supported pickup/removal/stack
merge and empty-stack retirement supplement the no-freeze/no-stale-anchor
invariant; they do not create a new Scale lifecycle owner. Preserve inventory
count, vanilla pickup target/delay, stack components and conservation across
merge/removal. Never use creative infinite pickup or lifetime settings as the
positive ordinary lifecycle case. Cover special infinite settings as negative
controls if candidate code touches lifecycle; do not reinterpret vanilla's
sentinels as ordinary counters.

Real 26.2 bytecode was inspected locally. ItemEntity.tick decrements finite
positive pickupDelay except32767, advances age except-32768, performs server
merge checks and discards server-side at age>=6000; empty stack discards before
the normal tick. Public getAge, setPickUpDelay, hasPickUpDelay, playerTouch,
setTarget and vanilla lifetime methods are available. setExtendedLifetime sets
age=-6000, so it cannot stand in for an imminent-despawn seed. Use full real ticks
or vanilla persistence to prepare a near-expiry state, then invoke actual tick;
no private age setter, fake counter progression or test-only lifecycle callback
is the acceptance proof. playerTouch checks the real pickup/target/inventory
conditions. Bytecode capture: ../s35-item-bytecode.txt (read-only inspection).

Ground friction comes from the actual branch of ItemEntity.tick that reads
Block.getFriction; PlatformItemMixin substitutes that read only. Compare current
canonical friction with baseline/matched control when that branch executes.
Water/lava/no-ground movement must retain their own vanilla branches; do not
force a ground flag to fabricate fluid proof. Non-predicting remote anatomy
replicas must retain vanilla integration/friction as Platforms.friction already
specifies. No new rendering, generic item pickup, age, lifespan or fluid-physics
implementation is authorized by S35.

### Private oracles and evidence levels

Own inputs/fixtures remain private until a defect or completed evidence handoff.
Preparation matrix covers real canonical contact, rest/tick cadence, single
ROOT transport, obstruction/retirement, lifecycle and nearby vanilla controls,
including removal/merge while an anchor exists. Start from current vanilla/API
semantics rather than rewriting legacy tests as an expected result. Invalid
fixture geometry/age/proximity is UNPROVEN infrastructure, never a product RED.
Require body/stack/inventory/contact state before and after transitions, not
only supported/result flags. Retain S33/S34 oracles unchanged.

If production repairs are needed, mutation adequacy will challenge tick freezing,
carry twice across dispatcher/tick, stale release/UUID/merge contact reuse,
policy/ratio bypass, false CLEAR/exhaustion and friction leaking to nonparticipants.
Do not weaken a lifecycle prerequisite to pass a candidate. Existing generic
core reuse is a hypothesis until the actual item runtime baseline proves it.

HOT_TICK/HOT_MOVE_QUERY surfaces scale with item bodies B, nearby merge/pickup
neighbors N, support pieces P and interval samples M. Audit pair-local work,
receiver independence if touched, allocation/caches and removal cleanup. Normal
vanilla merge work is a matched control, not charged as a newly invented Scale
algorithm. No new global item scan, per-item catalog preparation or reflection
may enter gameplay. Static/ROOT/release deterministic geometry/contact snapshots
will complement numeric identity/transport/lifecycle assertions where they add
signal. Runtime timing is not normative G9 FPS/TPS or general heap-soak evidence.

Plan verdict: **funciona, continúa**. I1-I4 converge with canonical item scope and
actual26.2 APIs; no concrete contradiction found. The accepted plan does not
certify item behavior before the real server baseline and independent oracles
execute, nor close later boats/rails/falling-block/G5/G9 work.

### ADVERSARY final executed pass — S35 item slice

Verdict: **funciona, continúa**. Production stayed identical to committed
`03efc6bd799431a00e2a6ffd4f46cf3e3d2cada2`: `git diff -- src/main src/client`
is empty. Final IMPLEMENTER proof source remains
`7467b5ccef2d8ce6c13207ef0be4f849c8831ec6dfe99b31bd50a8c4783484de`.
No production or IMPLEMENTER test was edited by ADVERSARY.

Six isolated, sequential private selectors ran in real Minecraft server
GameTests, each **2/2 GREEN** including its sentinel. Own build/cache/run dirs:
`../s35-adversary-build`, `../s35-adversary-cache`, `../s35-adversary-run`.
Private infrastructure is under `../s35-adversary-private`; each entrypoint is
selected by `-Ps35PrivateProof=io.github.r3neer.scalebrews.test.<class>` with
`-I ../s35-adversary-private/server.init.gradle` and the own build/cache flags.

| Own oracle | Actual evidence | Log |
|---|---|---|
| `S35PrivateItemLifecycleProof` | Matched real world ticks age5/delay; resting ROOT `(0.25,0.125,0)` once; next tick cannot repeat; supported pickup inventory4; persisted vanilla age5999 expires on real ticks; removed contacts absent; ordinary pickup inventory5 | `../s35-adversary-private/final-lifecycle.log`, 45s |
| `S35PrivateItemBoundaryProof` | Actual grounded item tick applies .25 material friction (`vx=.009800000190734863` from .04); no-physics move remains vanilla; actual neighbor merge3+5 conserves8; retired merge contact absent; support removal, age/gravity, eventual fall within vanilla cadence and released pickup8 | `../s35-adversary-private/final-boundary.log`, 47s |
| `S35PrivateItemPolicyProof` | Positive exact trajectory first contacts material; same trajectory with ratio rejection or disabled binding preserves full vanilla displacement, no material/legacy wall | `../s35-adversary-private/final-policy.log`, 70s |
| `S35PrivateItemWaterProof` | Actual water state after accepted contact, no forced ground flags; participant and matched nonparticipant velocities exactly equal `(0.03765152379100346,0.0009702000743107886,0)`; age/delay preserved | `../s35-adversary-private/final-water.log`, 27s |
| `S35PrivateItemObstructionProof` | Actual support ROOT completes1block; item-specific vanilla wall fails closed to zero body delta, retires contact; repeated drain/tick creates neither displacement debt nor velocity | `../s35-adversary-private/final-obstruction.log`, 64s |
| `S35PrivateItemBudgetProof` | Explicitly adapted historical S09 A11 case to an actual ItemEntity, not claimed clean-room geometry; calibration61decoys reaches256 evaluations after kernel progress; live wrapper exports zero/no contact/no suspension and observable exhaustion | `../s35-adversary-private/final-budget.log`, 66s |

Failures before those final isolated runs were fixture **UNPROVEN**, not
production RED: nonexistent `PlatformState.get` corrected to the actual
`Platforms.state(item).support`; first released tick positional fall was an
invalid oracle because vanilla stationary cadence may skip `move` while gravity
velocity and age advance (final proof checks those and fall within five ticks);
concurrent selectors interfered through their global prepared sessions, so all
final proofs ran separately. Failed logs are retained in the private evidence.
No production mutation was necessary or claimed.

`root-contact-sheet.png` was generated from actual before/after material and
item AABBs captured in `final-lifecycle.log`, with fixed relative XY projection
and92px/block. Orange before-body+ROOT reference overlaps actual blue after-body
and the item remains on the translated material face. Snapshot was viewed and
its extent corrected in the renderer. `root-snapshot.json` preserves actual
world coordinates, delta, view and source-log hash; `render_root.py` reproduces
it. This is runtime geometry evidence, not a Minecraft framebuffer/render QA.

Performance classification: S35 adds **no production path**. Existing friction
is constant pair-policy work in HOT_TICK; movement is HOT_MOVE_QUERY using the
existing bounded broadphase/32-step/256-evaluation response; retained contact
iteration scales with current bodies/supports, and vanilla merge with local
neighbors. No new item/world scan, reflection, catalog preparation, cache,
observer recomputation or lifecycle owner was added. Actual45tick merge/removal
and live item256-budget proofs complement the existing core evidence; no FPS,
TPS, general heap soak or new G9 benchmark is claimed.

The IMPLEMENTER prepared log `../s35-release-prepared.log` and ordinary build
`../s35-final-ordinary-build.log` were independently inspected:2/2 and465/465
GREEN, with hashes matching its record. These are reviewed IMPLEMENTER runs,
not mislabeled independent ADVERSARY full-suite executions. Since production,
client paths and resources are unchanged, no new client revalidation was
required by this test-only candidate.

Final source/log/oracle/snapshot manifest:
`../s35-adversary-private/final-evidence.json`, SHA-256
`9e2f8d3d425b9596e3f17d53b3e85d1af348a1f4e835b82229fc5db86e4a20eb`.
A final scope/hash/diff review produced zero production or candidate changes.
This closes the S35 **item** acceptance slice FR-007/066; later G5 categories,
rails/water boat semantics/falling blocks, legacy removal and global G9/manual
pack acceptance remain open under the canonical plan.
