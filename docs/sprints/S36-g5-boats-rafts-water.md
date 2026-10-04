# S36 / G5.3 — canonical boat and raft bodies, with water priority

Status: S36/G5.3 boat/raft slice closed locally after independent ADVERSARY
holdouts and a final zero-change pass. RED2 numeric boundary is repaired and
independently revalidated. S35/G5.2 closed locally at `a7dd4533`; the wider
G5 gate remains open.

## Scope and thesis — IMPLEMENTER

At the end of S36, ordinary boats, chest boats, bamboo rafts and chest rafts
can acquire a canonical material contact on a prepared living support through
their real movement/tick path; the boat owns its passengers' carry, uses the
configured material ground friction while supported on land, and yields to
Minecraft's water navigation on entry into water. One certified support
interval may transport a resting hull once. Ineligible support/boat pairs
retain their vanilla physical interaction.

This is the boat/raft slice of FR-007/064, with FR-009/010/053/055-058/093
and NFR-001/004/007/008/033 as neighboring constraints. S33 already owns
boat-item placement; S36 must not rebuild it. Minecarts, items, falling blocks,
legacy deletion, renderer/camera prediction and the G9 pack benchmark remain
outside this sprint. Item lifecycle S35 is a separate dependency and is not
considered closed by this plan.

## State reconstructed — IMPLEMENTER

`BodyClassification.category` maps every `AbstractBoat` to `boats`, and
`Platforms.eligible` performs canonical binding/policy and effective width
ratio checks. `PlatformBoatMixin` consults `Platforms.supported` when returning
ground friction and suppresses only a moving support pair through the shared
collision boundary. `PlatformPhysics` routes READY bodies into
`AnatomyMovement`, but still carries the legacy path for non-READY consumers.
The legacy `PlatformTests.occupiedBoatOnGiantPlayer` covers boat friction and
passengers through `PlatformState`, so it cannot certify the canonical-only
route. The prepared S09 stationary-contact proof already uses a real oak boat
and a certified cow ROOT interval, but it does not exercise boat tick, chest
variants, rowing, water transition or a retained ROOT carry. S33 proves a
bamboo chest raft can be placed once on a prepared material face, not its later
navigation semantics.

Installed Minecraft 26.2 `AbstractBoat.tick` bytecode confirms that status
checks underwater/in-water before ground friction, and that native
`controlBoat` runs only in the locally authoritative client branch. A server
boat ridden by `ServerPlayer` is not locally authoritative and its tick takes
the zero-velocity path; injecting velocity on the server cannot prove native
rowing. Existing `PlatformClientProof` demonstrates real client key input but
uses a legacy ghast surface. Existing S31 controlled-vehicle client proofs
demonstrate the prepared cow session and local vehicle authority, but do not
prove native rowing across water. S36 must combine those capabilities in a
dedicated integrated client/server acceptance lane. The
test fixtures must keep support/body geometry, occupancy, fluids and world
ticks explicit; an invalid water basin or boat status is a fixture issue,
not a product RED.

## Provisional implementation plan — IMPLEMENTER

- [x] I1 Build a prepared-server matrix for ordinary boat, chest boat, raft
  and chest raft. Use actual `AbstractBoat.move`/`tick`, an identified
  `SurfaceContact` and no `PlatformState` owner. Assert an occupied hull, not
  each passenger, owns the material relation. Include policy/ratio-negative
  pairs and ordinary vanilla interaction. Treat server-only tick/velocity as
  contact and transport evidence, never as native steering evidence.
- [x] I2 Verify configured ground friction on dry material and real
  land→material→water→land transitions. Water motion and controls must retain
  Minecraft priority; entering water cannot leave a sticky material anchor or
  duplicate/friction-suppress a vanilla water impulse.
- [x] I3 Verify one certified ROOT transport for a resting and an occupied
  hull, then obstruction/release and one-pass vanilla push/no-wall behavior.
  Add an **unconditional** integrated client/server lane with a real local
  player controlling the hull via Minecraft key input, a prepared canonical
  support, dry→material→water navigation, support carry/contact retirement and
  passenger ownership. Assert actual native control effect at the client and
  accepted server movement/contact state; do not substitute injected velocity,
  server paddle flags or a legacy surface for rowing. Repair only a proven
  shared-owner boundary, without a second vehicle-specific carry loop.
- [x] I4 Run targeted prepared, ordinary and the integrated local-controller
  client/server lane **even if production is unchanged**, followed by
  independent adversarial scenarios, final zero-change review and
  locality/performance checks. Record exact hashes/evidence, update G5 and
  create a separate logical local commit. No push or release.

## IMPLEMENTER response to ADVERSARY RED1

The plan now treats local-client control as mandatory evidence and names the
real key-input and prepared-session harnesses already present in the repo.
Server fixtures remain useful for contact, variant and material-interval
properties but cannot close I3 rowing. The client/server lane must establish
the authority and prepared-contact preconditions before evaluating movement,
water transition and passenger owner; otherwise its result is UNPROVEN. This
revision changes the plan only. A full preimplementation review restarts here.

## IMPLEMENTER runtime result

The prepared proof exercises actual oak boat, oak chest boat, bamboo raft and
bamboo chest raft bodies through `Entity.move` and tick. All four acquire a
named canonical material contact, use its dry friction, retain native type and
chest contents, and never use `PlatformState`. A resting oak boat follows a
ROOT translation and later nonidentity yaw exactly once. A too-wide ordinary
boat rejects material contact while preserving native pair collision and one
vanilla push. Another boat is clipped by a real block wall during ROOT carry;
removing the wall cannot replay the blocked displacement, and support removal
retires its contact.

The required integrated client/server GameTest mounts a connected local
player in a real oak boat over a prepared cow. Real key input rows the boat on
the body, a ROOT carries the occupied hull once, and the passenger remains a
passenger rather than another material owner. Continued native rowing enters
a source-water basin, retires the material contact on both sides, and exits
onto dry stone with native `ON_LAND` status. The basin is one block below the
material face and the bank is lower still so buoyancy and gravity, rather than
fixture collision, determine the transition.

That lane exposed a server vehicle-packet boundary. Minecraft collision-probes
a controlled boat, then accepts the client destination with `absSnapTo`.
The small native gravity step can place the server hull slightly inside its
accepted material face; the following material drain then retires contact.
`acceptedBoatPacketPosition` now projects only a shallow inward destination
onto the same accepted face for an occupied server boat. Fluid, changed face,
ineligible support, remote movement and out-of-world coordinates keep the
unmodified destination for native handling. Its fluid inspection is capped at
64 local block cells and follows a current cached material snapshot. The
first version of that inspection had an integer-loop overflow on a finite
out-of-world coordinate; ADVERSARY RED2 caught it, and the revised version
checks world bounds before narrowing to integers. The unchanged RED2 oracle
then passed 2/2 in the independent final run.

IMPLEMENTER same-source evidence after the RED2 repair: `../s36-prepared-push.log`
passed **465/465** required GameTests with the expanded server proof
(SHA-256 `919375c726a496679df489664e96fb9d34a7b9b1b171f5611f0e084e9eaec1c2`);
`../s36-client-final-candidate.log` passed the local rowing/water/bank lane
(SHA-256 `04e6ca71348f393a22c61237dc8c019c77285de5f49a6fc6c8a12658da8afb23`);
`../s36-final-ordinary-build.log` passed **465/465** (SHA-256
`6d9fdf12c077571fd20fff28e9d3233e842e333dd41c39c01ac2de0c0bd5f43e`);
and `../s36-final-full-client.log` passed the complete client suite (SHA-256
`0d12e0f70c81945c0eaf67340a5908f239ecda62321e68593d495bbe2d6e8eab`).
The corrected production source hashes are `ea50e54ffdb8ea0226dc82e3fb4ba246c81ba4a2b3fabc8724fa4449a1f1e433`
for `AnatomyMovement.java` and `12adf0c1661ebb0288ce323d4347018623d13a7bd59282c5c2dd8a024c441f6c`
for `PlatformConnectionMixin.java`. The independent final pass and its exact
evidence appear in the ADVERSARY section below. This closes only the S36
boat/raft slice, not the wider G5 gate or G9 acceptance.

## ADVERSARY preimplementation model

Active role: ADVERSARY. Independent reconstruction follows S35 commit
`a7dd4533`; local main is `aeca64e1`. No production was changed; S35 oracles
remain intact. This review does not certify S36 runtime behavior.

### Requirement-derived model

FR-007/064 require real boat, chest boat, bamboo raft and chest raft bodies
in the same material core. Establish prepared canonical selection, physical
hull dimensions and current exact SurfaceContact before interpreting outcomes.
Legacy state must not own the hull. Ratio/policy/state rejection preserves
native interaction and push, without an anatomy wall. Compare net push exactly
once rather than just a collision predicate. Passengers are not additional
material owners; native seat updates must preserve relative hull/passenger
motion without double carry. Chest contents and variant identity stay vanilla.

A resting hull follows its retained local material point through a certified
ROOT once, including nonidentity orientation. Carry respects blocks/entities;
obstruction or budget exhaustion cannot leave teleport/debt for the next tick.
Voluntary rowing composes with carry; boarding/controller changes cannot replay
an old interval. No vehicle-specific carry loop or placement rewrite is allowed.

Water priority requires real fluid depth/status, not forced private flags.
Compare a matched native hull with the same type, occupancy, input, velocity
and transition timing. First dry→water tick, stable water and exit/reacquisition
are separate boundaries: native drag/buoyancy/control continue, old material
contact retires, and return to dry material acquires a fresh accepted relation.
Surface water, underwater and flowing-water branches are not interchangeable.
An old anchor may not erase or duplicate native water impulses. Third entities
remain vanilla.

### Installed26.2 authority and status observations

Read-only local `javap -c -p` evidence:
`../s36-adversary-boat-bytecode.txt`,
`../s36-adversary-entity-bytecode.txt`,
`../s36-adversary-player-bytecode.txt`.
AbstractBoat capture SHA-256:
`4df115c358042667ca03b81b4990435695532367be3d757346dfb13ed49dac1d`.

AbstractBoat.tick derives status and interpolates, then gates floatBoat on
isLocalInstanceAuthoritative. Only its client-side branch invokes controlBoat
and sends ServerboundPaddleBoatPacket; the nonauthoritative branch writes
velocity ZERO. Entity's server authority is !isClientAuthoritative. Vehicles
delegate client authority to their controller; Player returns true. Therefore
an occupied ServerPlayer boat tick cannot certify native rowing. Injected
velocity/manual steering is not a replacement for actual local-client control.

getStatus checks underwater/checkInWater before getGroundFriction. Only dry
status consults ground friction. floatBoat has separate water/buoyancy branches
and special Player landFriction adjustment. Preserve exact native ordering and
matched outcomes rather than impose guessed constants. A friction-return test
alone does not prove navigation or water priority.

### Independent oracles and cost

Holdouts remain private until a completed pass or classified defect. Assertions
will separate real authority/input, accepted hull/contact, passenger seat motion,
material serial/local point, fluid velocity/state/anchor retirement and fresh
reacquisition. Fixed-input hull/material/passenger/fluid snapshots complement
numbers. Wrong authority/registration/fluid placement is UNPROVEN fixture
failure, not production RED. Runtime geometry snapshots are not framebuffer QA.

Audit HOT_TICK status/friction/control/contact retirement, HOT_MOVE_QUERY hull
sweep/carry, and HOT_NETWORK if replication changes. Scaling variables are
hulls B, supports S, pieces P, nearby collision neighbors C and passengers V.
Require local/bounded relations and no new world scan, repeated catalog/pose
preparation or reflection in the hot loop. Shared-owner changes need affected
server/client regressions and a final zero-change pass. Later G5 categories,
legacy removal and global G9/pack acceptance remain open.

### Plan review RED1 — missing mandatory native-controller lane

**No funciona, arréglalo (plan/evidence gap, not a production runtime bug).**
I1 specifies a prepared-server matrix; I3 asks for voluntary controls; I4 makes
client testing conditional on changed shared motion/presentation. Together
these allow no-production-change closure using server ticks/injected velocity,
although native controlBoat is client-only and an occupied ServerPlayer hull
is nonauthoritative. Counterexample: mount ServerPlayer, set input/paddles and
call the server boat tick: the authority predicate is false, controlBoat is not
called and velocity becomes ZERO. Empty-hull server water tests miss that path.
Require an unconditional real local-controller client/server acceptance lane
for native rowing across land→material→water, with carry/contact retirement and
passenger ownership checks, even if production is reused unchanged. Excluding
new renderer/camera implementation can remain.

The IMPLEMENTER status still calls S35 review pending although a7dd4533 already
closed it. This stale status is not a second technical RED. No other concrete
plan contradiction was found in this pass.

### ADVERSARY second full plan pass — convergence

Verdict: **funciona, continúa** (plan only). RED1 is resolved by revised I3/I4:
real local-player key input and integrated client/server native rowing are
unconditional, including when production is reused with zero diff. The plan
now requires prepared canonical contact/authority, client native control effect,
accepted server movement/contact state, water retirement and hull/passenger
ownership; server ticks or injected velocity cannot substitute for that lane.

The repeated full scope review found no remaining concrete contradiction:
I1 retains the four actual hull variants and policy/ratio vanilla controls;
I2 keeps real dry friction and land/material/water transitions; I3 keeps one
material ROOT owner, obstruction/release, one-pass native push and mandatory
rowing; I4 keeps independent scenarios, cost/locality review and a zero-change
pass before a separate local commit. S33 placement and later G5 categories,
legacy removal, new renderer/camera implementation and G9/pack acceptance stay
outside the proposed production slice. S35 dependency status is now current.

Current HEAD remains `a7dd4533f5fb69dc3af2aa6f33a19f551dddb4dd`, local main
`aeca64e1`, and no production diff exists. Installed26.2 authority/status evidence
and the independent model above remain applicable. Future holdouts remain
private. No S36 runtime acceptance, new test execution or gate closure is
claimed by this preimplementation convergence result.

### ADVERSARY candidate RED2 — unbounded fluid-cell walk before native packet validation

**No funciona, arréglalo.** Classification: production HOT_NETWORK boundedness,
NFR-007/012. The new packetIntersectsFluid casts floor(box bounds) to int, then
uses inclusive `for(int x=minX;x<=maxX;x++)` (also y/z). A finite target at
X=3,000,000,000 makes minX=maxX=Integer.MAX_VALUE; the volume guard counts only
one X cell and admits four total cells for a normal hull AABB. After the first
X iteration, x++ wraps to Integer.MIN_VALUE, still satisfies x<=MAX_VALUE, and
the scan never terminates. acceptedBoatPacketPosition calls this scan before
piece separation, while PlatformConnectionMixin calls that correction before
the original vanilla packet handler performs its coordinate checks/clamp.
Thus a READY dry controlled hull with accepted contact can stall its server
thread on a finite vehicle target; the later native guard cannot protect it.

Own `S36PrivatePacketBoundsProof` calls the actual production helper through a
read-only test reflection seam with a real boat/Level. Both AABBs are above
build height so every real fluid query is EMPTY and no remote chunk loads.
Ordinary matched scan completed in2,209,400ns. Extreme finite AABB reported
castMinX=castMaxX=2147483647, maxCellCount=4, completed=false after250ms, no
exception. A daemon test thread keeps the oracle/server shutdown bounded; the
server reported the assertion RED and shut down cleanly. This helper-level
runtime proof does not masquerade as an actual remote packet integration test;
the reachable native hook ordering and loop overflow establish the defect.

Reproduction (same own fixture, no production edits):
`gradlew.bat runGameTest -PscalebrewsBuildDir=../s36-adversary-build
 --project-cache-dir ../s36-adversary-cache
 -I ../s36-adversary-private/server.init.gradle --console=plain`
with installed JDK25 configured. Entry point defaults to the public failing
private oracle; fixture source is
`../s36-adversary-private/java/io/github/r3neer/scalebrews/test/S36PrivatePacketBoundsProof.java`.
Log `../s36-adversary-private/packet-bounds.log`:1required failure,
BUILD FAILED48s. Candidate hashes:
AnatomyMovement `a45795785591a6716d190653c2c0578f4ff58c256fb6dc39a7532a7169907961`;
PlatformConnectionMixin `12adf0c1661ebb0288ce323d4347018623d13a7bd59282c5c2dd8a024c441f6c`.
No S36/G5 closure. Other holdouts stay private until a repaired candidate.

### ADVERSARY final S36 functional/numeric pass

Verdict: **funciona**. RED2 is closed by the original oracle, unchanged:
`S36PrivatePacketBoundsProof` final2/2 GREEN reports bounded-control1.149ms
and finite extreme-cell query completed=true without an exception. The
production helper checks all six finite/world bounds before int conversion;
its long extents are each<=64 before multiplication, and actual admitted
volume<=64. Inclusive loop counters now stay inside finite world limits.
The public correction leaves destinations beyond1block to native handling
before geometry/fluid lookup; separation also precedes the fluid scan.

Final production hashes, verified again after the runs:
AnatomyMovement
`ea50e54ffdb8ea0226dc82e3fb4ba246c81ba4a2b3fabc8724fa4449a1f1e433`;
PlatformConnectionMixin
`12adf0c1661ebb0288ce323d4347018623d13a7bd59282c5c2dd8a024c441f6c`.
ADVERSARY changed no production or IMPLEMENTER tests. Current HEAD remains
S35 `a7dd4533`; only the IMPLEMENTER's declared S36 delta plus this owned
record is pending. S35/S33/S34 oracles are intact.

Own tests ran separately in real Minecraft with private build/cache/run dirs:
`../s36-adversary-build`, `../s36-adversary-cache`, `../s36-adversary-run`;
selector `../s36-adversary-private/server.init.gradle`, property
`-Ps36PrivateProof=io.github.r3neer.scalebrews.test.<class>`.

| Independent own oracle | Executed result |
|---|---|
| `S36PrivatePacketBoundsProof` | Original RED2 oracle unchanged;2/2 GREEN final, completed finite query; log `final-S36PrivatePacketBoundsProof.log` |
| `S36PrivateHullMatrixProof` | Four actual hull types, dry canonical owner, ROOT(.18,.09,.07) once, native seats2normal/1chest, chest golden-apple payload3 intact, passenger queries/dismount do not create anchors or repeat carry; ratio/policy rejection matches exact native block/entity clip `(0,-.4,0)`;2/2 GREEN22s |
| `S36PrivateOverlappingWaterProof` | First actual water tick over the same material face is native IN_WATER/isInWater; old owner retires; later support ROOT(.25,0,0) moves hull zero and leaves no contact;2/2 GREEN31s |
| `S36PrivateProjectionProof` | Public controlled-hull positive repairs shallow -.04 inward step to current face; outward/deep/nonlocal/nonfinite controls stay native; target fluid returns native before water flags update;10000 repeated queries deterministic/pure with no body/contact sequence change;2/2 GREEN33s |

Logs are in `../s36-adversary-private`. Fixture failures were classified
UNPROVEN and retained: missing AnatomyFilter import, an initial incorrect
assumption of two chest-hull seats, and snapshot metadata read after dismount.
Final capacity assertions match installed vanilla, and the snapshot stores
passenger positions at the same pre-dismount ROOT phase as the unchanged
semantic assertions. These were not production REDs or weakened product checks.

The mandatory real local-controller IMPLEMENTER oracle was also independently
executed unchanged in own client build/cache/run dirs, twice; final
`../s36-adversary-private/client-final.log` is BUILD SUCCESSFUL67s with clean
exit. It exercises real key input, accepted server hull state, occupied ROOT
and material→water→dry-bank navigation. This is an independently executed
public oracle, not falsely labeled an ADVERSARY-authored client holdout.
Reviewed IMPLEMENTER final ordinary465/465 and full-client logs are GREEN;
those broad suites remain attributed to their executing role.

Snapshot `hull-contact-sheet.png` was viewed after generation/layout correction.
Fixed relative YZ,58px/block, uses actual runtime AABBs and native passenger
root positions for all four types. Orange before+ROOT reference overlaps blue
actual after for hull and seats. `render_hulls.py` asserts all reference/actual
coordinates within1e-6 and `hull-snapshots.json` preserves actual coordinates,
delta, view and source-log hash. This geometry/contact projection is not
rendered hull mesh, framebuffer, modded renderer or human pack QA.

Performance: the added route is HOT_NETWORK with constant one-piece SAT,
maximum64local fluid cells and constant-distance cutoff; existing HOT_TICK and
HOT_MOVE_QUERY semantics are reused. It introduces no world entity scan,
reflection, catalog compilation, new history or extra lifecycle owner.
A fixed-input public-projection diagnostic10000calls took198,969,200ns and
allocated126,687,296bytes (about19.9us/12.7KB per call including test checks).
Provider geometry/joint evaluations both increased by0; current frame/pose
reuse is proven. Cost is explicitly recorded rather than claimed free. This
one-piece synchronous diagnostic is not FPS/TPS, a normal-session heap soak
or global NFR-014/G9 acceptance; local/bounded work and no preparation leak
were independently reviewed. Native fluid/push/variant paths remain shared.

Final source/log/oracle/snapshot manifest:
`../s36-adversary-private/final-evidence.json`, SHA-256
`92de7d2e4c551e1500056f817d35657eb9241b747323793f09ce29f3b61f97f8`.
Final scope/code/hash review found no further concrete defect and made zero
candidate changes. S36 boat/raft FR-007/064 slice is ready for closure; later
G5 minecart/falling-block/legacy removal and global G9/manual pack remain open.
