# S39 / G5.6 — passive movement accounting and landing boundary

Status: closed locally after independent zero-change review.
Active role for this section: **IMPLEMENTER**. S38 closed locally at `93eb75b1`.

## Scope and thesis — IMPLEMENTER

At the end of S39, a real player carried by one certified prepared material
interval keeps its own movement, food and fall accounts unchanged, while a
subsequent voluntary move or free fall still uses Minecraft's normal accounting.
Landing on a living support does not trigger Growth terrain impact, whereas a
later real terrain landing can. The proof must distinguish these causal paths
using real entities, a prepared support and actual movement/ticks, not only
inspect transport receipts or manually reset counters.

This is FR-061/062 and the accounting part of G5 task 5. FR-009/010/053,
FR-055–060/063 and NFR-001/004/007/008/033 are neighboring invariants.
Sneak-edge and jump-release mechanics, experimental minecart movement, legacy
retirement, G9 benchmarks and human pack QA remain separate work. Human-only
checks are deferred until the end of the epic as requested.

## Current state and mainline check — IMPLEMENTER

`chatgpt-editing` contains the certified shared material transport and
`PlatformPhysics` legacy path. `PlatformTests.offRailCartAndPassiveAccounting`
checks only legacy ghast carry, saturation and fall distance. The ordinary
`ScalePhysicsTests.exhaustionOnlyChangesPhysicalActivity` checks direct calls
to jump/movement statistics under Growth/Shrinking, but not passive material
transport. `GrowthImpactMixin` intercepts `LivingEntity.checkFallDamage` and
`GrowthImpact.land` returns while `Platforms.supported`, yet a real prepared
landing and later terrain landing have not been distinguished in this path.

Fresh `origin/main` at `b5e62535` was compared before planning. It adds
`ScaleGait` and an `updateWalkAnimation` modifier in `LivingEntityMixin`, plus
other mount/gameplay work; it does not change `GrowthImpactMixin` or
`ScaleMovementExhaustionMixin`. The branch is not merged with this mainline
yet. S39 must not alter or discard the new gait, mounts or unrelated
mainline mechanics when integration occurs.

## Provisional implementation plan — IMPLEMENTER

- [x] I1 Inspect the installed 26.2 `ServerPlayer` movement-stat/exhaustion and
  `LivingEntity.checkFallDamage` call paths and the exact prepared transport
  application. Define paired control accounts before changing code.
- [x] I2 Add a prepared player proof with a real client/server movement lane:
  the connection handler must process accepted player packets before and after
  horizontal, rising and descending certified ROOT intervals. Assert current
  contact/receipt, actual displacement and unchanged passive walking,
  swimming, sprint distance, exhaustion and self fall accounts after the next
  native packet and tick boundary. A voluntary positive control must travel
  through that same handler and increase its applicable accounts. Compare
  collision-clipped, no-op and replay cases; keep per-scenario budgets intact.
- [x] I3 Exercise a real fall onto prepared living material and a later real
  fall onto terrain. Both must preserve applicable vanilla self fall damage;
  only terrain may produce Growth impact. Make the living-landing Growth
  negative non-vacuous: enabled rule, eligible tier/distance, target and a
  non-air native ground argument that does not itself block the material
  landing. Pair it with a positive terrain event under equivalent conditions,
  and classify invulnerability, flight/water/passenger state and cleanup.
  Direct calls to `GrowthImpact.land` cannot replace the real landing route.
  Repair only a reproducible missing boundary, preserving Minecraft ownership.
- [x] I4 Run focal prepared, real client movement, full prepared and ordinary
  build lanes. Obtain independent holdouts, locality
  review and a complete zero-change pass; record exact evidence in the
  canonical plan and `VALIDATION.md`, then make one logical local commit.

## IMPLEMENTER runtime candidate and classified fixture REDs

Installed 26.2 `ServerGamePacketListenerImpl` invokes
`ServerPlayer.checkMovementStatistics` in the native player and vehicle
handlers. `MaterialPhysicsRuntime.apply` transports the body by `setPos` and
records one certified transport; the real dedicated client fixture must still
prove that subsequent movement packets do not turn that carry into own motion.
The dedicated N2 fixture now includes an opt-in S39 phase after its existing
player RTT runs. It keeps the real client idle through horizontal, rising and
descending cow ROOT intervals, waits for native movement packets and a server
tick/fence, then observes unchanged walk/sprint/swim/fall stats, exhaustion and
fall distance with an advanced transport sequence. A client input pulse through
the same handler raises the walking stat from 624 to 725. Targeted
`../s39-client-n2-first.log` passed `BUILD SUCCESSFUL`, SHA-256
`af915a24a9b2a14710049743d297d1cc34180821c1fbda9a33623177ba07c2cf`.
The earlier general client run `../s39-client-accounting-first.log` was green
but did not select N2, so it is not attributed to this new oracle.

The prepared S39 proof makes a Growth III pig fall in two actual movements,
acquiring a canonical material contact only on the final landing. A
non-colliding `STRUCTURE_VOID` at the native ground position makes the living
landing's `BlockState` non-AIR. A test-only mixin observes the real
`checkFallDamage` argument and current material contact. Vanilla emits one
`HIT_GROUND`, reduces the actor's health and resets fall distance; no Growth
gust or target damage occurs. After support retirement, a separate real fall
onto stone reduces actor and target health and emits one Growth gust with
terrain particles. `../s39-landing-fourth.log` passed **2/2**, SHA-256
`73b5b888a646d1b21065f59e832faba986a544a594b03a37f6aa5cee992b891f`.

The first terrain proof was RED because one large `Entity.move` reached ground
before Growth's HEAD hook could observe previously accumulated fall distance;
two real fall movements fixed the fixture. The next living proof was RED because
it incorrectly required zero block particles; vanilla itself sends one on
native landing. Neither RED prompted a production change or weaker Growth
event/target assertions. The full prepared sequence with S39 passed **2/2**
and 22 scenarios in `../s39-prepared-full-first.log`, SHA-256
`76972d7b7bc2e3008978c8669a69751a64fe0c02e2055f1c2aaeeb36091df03f`.
Ordinary `build` passed **465/465** in `../s39-build-first.log`, SHA-256
`f06357c6c9377021117000663bc0b88e7bf588ec9e6ef2fc590853d3ef272622`.
The full client suite on this candidate passed in `../s39-client-full-first.log`,
SHA-256 `52d78326c074c84a0320c496ec74864a35444109f6595ca73c4bfa58553b3f84`.
At that point only test, harness and documentation files had changed. The
independent review then exposed an obstructed-prediction defect, addressed in
the second candidate below.

## IMPLEMENTER second candidate — obstructed prediction

The ADVERSARY reported and reproduced a synchronized-wall RED: the server's
anchored planner rejected a +1.2 X ROOT path and applied zero, while local
prediction applied its nonzero partial block clip. The following native player
packet counted the resulting 43 cm as `FLY_ONE_CM` without voluntary input.
This is a production defect at the local carry decision, not an accounting
fixture failure. `AnatomyMovement.carry` now checks the same clip mismatch
threshold before `setPos`, passenger placement, ledger/receipt recording or
observer publication. A rejected partial result retires the contact without
moving the local body. The server's certified path already has that all-or-
nothing rule. The changed path is HOT_MOVE_QUERY, executed for a locally
predicted contacted root: it adds one constant-time vector comparison already
computed before the patch, with no additional geometry query, allocation,
history, scan or lock. Production source SHA-256:
`d2467b72accf48002cb2e9a273de8e544b0319035e7b2db78719ca9379790928`.

An ordinary GameTest now starts from a real material contact, proves an
unobstructed carry applies and records transport, then places a real wall so
vanilla clipping permits only a nonzero partial displacement. The obstructed
carry must move zero, retire contact and leave the transport sequence intact;
a repeated call cannot replay it. The first ordinary run passed **466/466** in
`../s39-server-obstruction-first.log`, SHA-256
`8791dfb9df64e0c415d7975d3f5027be565514984708d908d488ba6565e2b2cb`.
The ADVERSARY's unchanged aligned real client/server holdout passed after the
fix: server/client remained at x=.27 and `FLY_ONE_CM=0`, with its other controls
green. Its log is `../s39-adversary-private/packet-obstruction-postfix-repeat.log`,
SHA-256 `a787368261244d6d6d658950e71f244059c2e8f0bfb71b658a5acc69158fd6f9`.

Two focal N2 reruns failed before reaching the opt-in S39 phase because they
started the 0.8 relative-excursion measurement immediately after the first
player contact, at relative z≈-.25; in a green run it started after native
post-teleport settling at z≈-2.12. After 25 ticks the early-start runs had
settled near the same edge, so the measured excursion included fixture setup.
The fixture now waits 40 ordinary ticks after contact and reconfirms contact
before starting its unchanged 0.8 oracle. The full N2+S39 run with the
original bound and all player/boat RTT 0/100/200 ms phases passed, including
the native accounting positive and negative controls, in
`../s39-client-accounting-settled-first.log`, SHA-256
`52fb8804c3e88d2d7248fe9a9a1016cbfc21017e2c4b90d351ba32670abc8822`.
The complete prepared lane passed **2/2**, 22 scenarios on distinct ticks, in
`../s39-prepared-full-postfix.log`, SHA-256
`7e6dcedb82baa5bafb2290ccf3e560f547c189e36ff3e047dd9c527385b27a08`.
The final ordinary build passed **466/466** in `../s39-build-postfix.log`,
SHA-256 `962f36274233d3e5290c816482730728943f60229a33adfc30a713f0cc325b89`.
The full general client suite passed in `../s39-client-full-postfix.log`,
SHA-256 `1b10d352dc73ebfee89ee4a8fdc0fa0e855679fd3db2c33ae8c1f6d51cd1480b`.
The independent final review passed the unchanged packet and landing holdouts,
plus 10,000 quiet and 10,000 retired-carry locality queries without a new
transport or geometry evaluation. It produced no production or IMPLEMENTER test
change and retained the prior Growth mutation RED. Its final inventory is
`../s39-adversary-private/review-evidence.json`, SHA-256
`1b1af34edf0d8ebbf56cdb511de0cd75f1ee66c3d1ce2743152a93662ef2ad4f`.
S39 closes FR-061/062 locally; remaining G5/G9 and human pack QA are separate.

## ADVERSARY preimplementation model

Active role: **ADVERSARY**. This is a preimplementation contract review, not
runtime acceptance. Future fixture details, permutations and seeds stay private.

The independent oracle decomposes requested own movement, permitted passive
transport and native account processing. One unchanged counter immediately after
setPos/carry is insufficient: the installed 26.2 connection handler calls
ServerPlayer.checkMovementStatistics for accepted player/vehicle movement, while
PlatformConnectionMixin adapts packet targets and transport baselines. Accounts
must therefore be observed after real native packet handling, including a
subsequent packet and actual tick boundary; a voluntary positive control must
exercise that same path. Neither direct statistics calls nor a mock player's
position/tick alone certify this integration boundary.

Landing is a separate causal boundary. A true fall onto prepared living material
must preserve applicable vanilla self fall damage while producing zero Growth
terrain outcomes. A later true terrain fall must produce its native self damage
and exactly one Growth outcome. The Growth negative needs a non-vacuous paired
positive oracle: enabled rule, eligible Growth tier/distance, live target and
real non-air terrain, with immunity/passenger/water/flight states controlled.
The current helper also short-circuits on air ground and several actor states;
zero target damage with one of those conditions does not prove material landing
suppression. No direct GrowthImpact.land call replaces the real landing route.

Accounting frontiers cover horizontal/up/down transport; own motion before and
after transport; incomplete collision-permitted carry; causal replay and no-op;
contact retirement followed by voluntary movement/free fall. Native distance,
food/exhaustion and fall accounts must be sampled without resetting counters to
manufacture a baseline. Controls use equivalent scale/gravity/rules and separate
normal food/tick effects from transport. This sprint does not close sneak/jump,
experimental minecart behavior, legacy retirement or whole G5.

If production changes are necessary, classify accounting/landing seams as
HOT_MOVE_QUERY and/or HOT_NETWORK/HOT_TICK. Scaling variables are bodies,
accepted packets, genuine intervals and local contact candidates; reject global
entity/support scans, per-consumer preparation, unbounded histories and extra
geometry evaluation solely to suppress accounting. Inspect allocations and
locking, and use work counters where they add signal. If only fixtures change,
state that no production cost change is claimed. A deterministic before/contact/
release/terrain snapshot should corroborate spatial landing assertions, without
replacing the packet and damage/event oracles.

**Initial preimplementation RED1 (plan/integration, resolved by revised I2):** I2 does not require native
handleMovePlayer/handleMoveVehicle processing or a real client/server movement
lane. A carry-only survival ServerPlayer fixture can pass unchanged accounts
while never exercising the call that records distance/exhaustion. Require the
relevant real handler lane and its same-path positive voluntary control.

**Initial preimplementation RED2 (plan/oracle, resolved by revised I3):** I3 does not explicitly require
applicable vanilla self fall damage at the living-support landing, nor prove
that the Growth negative is otherwise eligible and its paired terrain control
positive. An airborne support over AIR can give zero Growth even if the material
suppression is removed. Require both landing damage oracles and the non-vacuous
Growth positive/negative comparison before claiming FR-062.

Bytecode inspection evidence: ../s39-adversary-connection-bytecode.txt,
ServerGamePacketListenerImpl invokes checkMovementStatistics in both native
movement handlers. Production inspected: GrowthImpactMixin, GrowthImpact,
ScaleMovementExhaustionMixin, PlatformConnectionMixin and AnatomyMovement carry.
No production or runtime fixtures changed or executed in this planning review.

### ADVERSARY convergence review

The revised IMPLEMENTER plan passes the independent complete reread. I2 now
requires real client/server accepted movement packets, the next native packet
and tick boundary, same-handler voluntary positive control and clipped/no-op/
replay cases. I3 now requires applicable self fall damage on both genuine
landings, otherwise-eligible Growth suppression with non-air ground that does
not create the support landing, and the equivalent positive terrain event.
I4 makes the real client movement lane unconditional. Both initial plan REDs
are resolved; no further plan correction is required by this pass.

Operational verdict: **funciona, continúa** for preimplementation convergence
only. No runtime result or FR-061/062 closure is claimed; production and
fixtures remain untouched. The non-vacuous landing geometry, actual account
route and performance invariants still require implementation-time evidence.

### ADVERSARY independent runtime review — obstruction RED

Independent fixtures are external under ../s39-adversary-private and do not
modify production or IMPLEMENTER tests. Production had no main/client source
diff in the initial candidate. Public baseline evidence was checked separately;
it does not substitute for these independent lanes.

FR-062: S39PrivateLandingProof runs a real falling Cow through native travel/
move and checks each fall damage attempt and actual health loss inside
hurtServer, rather than relying on end-tick health which scale updates can
change. A non-colliding non-AIR grass argument accompanies the material landing.
Initial material landing applies 6 native self damage once and emits zero
Growth; descending ROOT and rest add neither damage nor Growth. After actual
support retirement, a true terrain landing applies another 6 self damage and
one Growth, with no repeated event during rest. The final snapshot lane passed
2/2 in landing-final-snapshot.log (27s). An external source-overlay mutation
removing only GrowthImpact's material suppression makes the unchanged oracle
RED with gusts=1 at the first material landing (landing-mutation.log). This
proves that its Growth negative is non-vacuous. Production source was never
swapped. Initial fixture logs are preserved: NoAI alone did not initiate travel;
then end-tick health was replaced by in-event damage accounts to separate native
scale health updates. No fall-distance counters were fabricated or reset.

FR-061: S39PrivatePacketProof uses an actual integrated client/server connection,
accepted player and sprint-command packets, canonical prepared Cow material,
native client prediction and real tick boundaries. Horizontal/up/down ROOT
passes invariant checks for WALK, SPRINT, CROUCH, SWIM, water walk, underwater
walk, FALL, FLY and CLIMB distances, food, saturation, exhaustion and fall
distance. A same-handler voluntary sprint positive control counts 25 cm and
.024999999 exhaustion. No-op and repeated drain preserve accounts. Positive
base lane passed in packet-native-prediction-current.log (40s); complete lane
with a fully rejected obstruction passed in packet-obstruction-repeat.log
(39s). These do not close the partial-prediction frontier below.

**Runtime RED1 — obstructed prediction/accounting:** deterministic actor/support
orientation and wall distance expose a no-input obstruction divergence. Before
the interval player x=.27, FLY_ONE_CM=0; the support publishes genuine ROOT
+1.2 X against wall x=1. Immediate physical server result is unchanged and
contactAfter=null, but client native prediction subsequently moves to
x=.699999988079071. The native server packet handler records FLY_ONE_CM=43.
Input was already stopped; the fixture does not set the client position or
fabricate movement packets at the obstruction boundary. This is a prediction/
blocked-carry and accounting frontier, not a claim that an applied server carry
was charged: server carry was zero. Log: packet-obstruction-aligned.log (RED36s),
with S39_PRIVATE_CLIPPED_PHYSICAL showing ROOT and transport/retirement state.
Reproduce using runClientGameTest -PscalebrewsBuildDir=../s39-adversary-client-build
--project-cache-dir ../s39-adversary-client-cache
-I ../s39-adversary-private/client.init.gradle with Java25. Returned to
IMPLEMENTER; S39 remains open until this frontier is classified/repaired and
independently revalidated.

Fixture refinement logs remain preserved: packet-first lacked exact initial
teleport agreement; packet-synchronized also had unrelated support AABB push and
forced client position incorporation. The corrected lane acquires high material
outside that AABB and lets native prediction consume ROOT. The initial strict
positive-partial obstruction oracle was too strong for a valid conservative
zero result; the final oracle allows rejection, requires initial canonical
contact plus genuine full ROOT, and asserts no penetration/replay. Its accounting
invariance was not weakened. Later holdouts remain private.

The measured landing-contact-sheet.png and landing-snapshot.json supplement
semantic events: fixed XY scale, native start/material/held/terrain AABBs and
material reference error 3.6e-15. They are not framebuffer/manual gameplay
acceptance. No production performance change is claimed for the initial S39
candidate: its changes are test infrastructure/OFFLINE_TOOLING. Any repair must
repeat hot-path locality/cost review, mutation sensitivity and unaffected lanes.

Aligned RED was reproduced without changing the oracle in
packet-obstruction-aligned-repeat.log (39s): the same x=.27 to .699999988
predicted displacement, zero immediate server carry and FLY_ONE_CM +43.
All main/client files stayed byte-identical across that repeat. Durable hashes
and sources/logs/snapshot evidence are in ../s39-adversary-private/final-evidence.json.

### ADVERSARY unchanged aligned holdout after RED1 repair

The exact aligned S39PrivatePacketProof oracle (SHA-256
4e5a5ec2fc045d707909f2b88c7dbf70eda4dcbcf52e45084052776e6275ea49)
passes against AnatomyMovement SHA-256
d2467b72accf48002cb2e9a273de8e544b0319035e7b2db78719ca9379790928.
packet-obstruction-postfix-repeat.log: BUILD SUCCESSFUL 1m23s, clean client
Stopping. Blocked ROOT still applies zero server carry and retires contact;
client/server player stays at x=.27 and FLY_ONE_CM stays0. All preceding
horizontal/up/down, food/fall, sprint-positive (.025 exhaustion /25cm), no-op
and repeated-drain checks pass unchanged. RED1 is resolved for this holdout.

First post-fix attempt is preserved in packet-obstruction-postfix.log: it
failed initial material acquisition before any tested carry/account boundary.
The unchanged repeat acquired correctly and completed; this is a non-reproduced
initial fixture acquisition failure, not evidence that the blocked-carry repair
failed. No assertion or fixture source was changed between attempts.

Static repair review: the same scalar clip/delta comparison and existing clear
operation now precede setPos and transport publication; no additional loops,
pose preparation, persistent history or reflection appear. Server authoritative
prepared intervals still use their dispatcher path. This is a frontier green,
not independent final whole-S39 closure or a replacement for required server/
prepared/client regression lanes on the repaired candidate.


### ADVERSARY final S39 repaired-candidate review

Operational verdict: **funciona** for the S39 FR-061/062 boundary. No production
or IMPLEMENTER test edits were made by ADVERSARY. The original packet and landing
oracles remain byte-identical to their pinned versions above. AnatomyMovement
remains d2467b72accf48002cb2e9a273de8e544b0319035e7b2db78719ca9379790928;
the only production delta is the early rejection of a clipped predicted carry.

Independent repaired-candidate evidence: unchanged aligned packet oracle GREEN
in packet-obstruction-postfix-repeat.log (1m23s); unchanged landing oracle GREEN
in landing-postfix.log (1m19s, 2/2), preserving native applied own fall damage on
both material and terrain while Growth fires only on terrain. The previously
executed Growth guard-removal mutation remains RED against the same landing
oracle. Native contact-sheet evidence remains measured geometry, not framebuffer
or manual acceptance.

A separate auxiliary S39PrivateReleaseProof passed in
release-locality-separated.log (49s, clean shutdown). It repeats native ROOT
horizontal/up/down and obstruction accounts, then measures 10,000 quiet client
carry queries: zero additional geometry/joint evaluations and no position or
receipt change. After blocked ROOT retires contact, a later genuine ROOT has no
movement/stat debt; 10,000 retired carry queries cannot recreate a receipt or
movement. A subsequent native voluntary air packet counts FLY_ONE_CM +31.
This is bounded functional/work evidence, not an FPS/TPS or G9 benchmark.

Auxiliary fixture failures are preserved: initial acquisition occurred in the
same tick as support alignment, and the first instrumentation cast confused the
client provider wrapper with its model evaluator. The auxiliary setup now waits
two real ticks after alignment and reads the actual current frame; test-only
reflection resolves the evaluator before the measured loop. No original oracle
assertion, production behavior or work bound was changed to obtain the result.
The initial acquisition failure recurred in auxiliary setup; its earlier label
as non-reproduced applies only to the original unchanged repeat.

Cost/locality review: HOT_MOVE_QUERY/HOT_TICK, one body/contact/support and its
local collision candidates. The existing scalar comparison and clear are moved
before setPos/passenger/baseline/receipt callbacks; no new loop, catalog lookup,
pose preparation, lock, history, network fanout or persistent retention appears.
Existing carry calls allocate their traversal set; zero allocation is not
claimed. Prepared authoritative server transport remains dispatcher-owned and
already releases clipped transport with zero displacement.

Final IMPLEMENTER regression logs were inspected, not executed by ADVERSARY:
s39-build-postfix.log (466/466, 1m1s), s39-prepared-full-postfix.log
(22 sequential scenarios, 2/2, 35s), s39-client-accounting-settled-first.log
(native accounting plus N2 0/100/200 ms with its original limit active, 1m52s), and s39-client-full-postfix.log
(4m7s, clean shutdown). Earlier RED N2 attempts are preserved and are superseded
by the final settled accounting lane with the original N2 limit active; the
postfix-correct log used S25 reference mode and does not prove that limit. The
settled log SHA-256 is
52fb8804c3e88d2d7248fe9a9a1016cbfc21017e2c4b90d351ba32670abc8822.
The final test places the gated accounting before its measured N2 phases. git diff --check
passes. No new concrete S39 defect remains in this review. Whole G5, neighboring
mechanics, G9 and final human pack acceptance are not closed by this verdict.
Durable hashes and evidence inventory: ../s39-adversary-private/review-evidence.json.
