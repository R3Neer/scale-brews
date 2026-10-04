# S40 / G5.7 — sneak edge and jump release on prepared material

Status: S40/G5.7 closed locally after independent ADVERSARY holdouts and a final zero-change pass.
Active role for this section: **IMPLEMENTER**. S39 closed locally at `3b3f8a74`.

## Scope and thesis — IMPLEMENTER

At the end of S40, a player actually standing on prepared living material
cannot walk off its gravity-relative edge while sneaking, but can leave it by
an ordinary jump. The jump must promptly retire the old material relation and
must have the same own impulse as the equivalent vanilla jump from a static
floor, without a later passive transport or extra impulse from the old support.
Real native movement, player input and tick order must prove this; direct
`AnatomyMovement.edge`/`collide` kernel calls alone are insufficient.

This covers FR-054, FR-063 and G5 task 3. FR-044/045/047/053/058/061 and
NFR-007/008/033 are neighboring invariants. Minecart experimental movement,
legacy engine retirement, G9 and human pack QA remain separate work; human-only
checks are deferred to epic completion as requested.

## Current state and mainline check — IMPLEMENTER

Fresh `origin/main` was fetched at `b5e62535` before planning. Its
`PlatformPhysics` is an older legacy/anatomy implementation and its
`TinyMountMovementMixin` adds wolf mount speed without changing its controlled
mount jump cancellation. Neither file is to be overwritten from mainline;
S40 must preserve new wolf/tiny-mount behavior in the eventual integration.

Current `PlatformPlayerMixin` intercepts installed Minecraft 26.2
`Player.maybeBackOffFromEdge` only while `PlatformPhysics.touching` and for
`SELF`/`PLAYER`. `PlatformPhysics.edge` routes READY shared physics to
`AnatomyMovement.edge`; that helper clips tangent movement when sneaking and
returns upward movement unmodified. `AnatomyMovement.collide` clears a contact
on positive gravity-relative vertical movement, but this happens at movement,
after `LivingEntity.jumpFromGround` sets velocity. The existing six-gravity
GameTest directly calls `edge` and `collide`, so it does not prove that the
native edge hook or actual jump/tick transition invokes them in time.
Installed 26.2 bytecode confirms vanilla `maybeBackOffFromEdge` gates on
ground/shift, `SELF`/`PLAYER`, nonpositive Y movement and ability state; its
own clipping iterates in 0.05 steps. `jumpFromGround` sets upward delta
movement and optional sprint tangent impulse without clearing a material
relation itself. S40 must compare actual behavior with these native controls.

## Provisional implementation plan — IMPLEMENTER

- [x] I1 Reconstruct actual native call order for player edge/backoff,
  jump impulse, `Entity.move`, support carry and contact publication on server
  and locally controlled client. Confirm all six gravity directions and
  eligibility/ownership gates against the installed 26.2 classes.
- [x] I2 Add a prepared-runtime player edge proof through `Player.move` and
  the `maybeBackOffFromEdge` hook, with shift positive, shift-off negative,
  cardinal/diagonal edge movement and multiple gravity frames. Measure the
  final footprint against the actual convex face, not merely a reduced delta;
  preserve ordinary world-block edge behavior for noneligible pairs. Also run
  a real integrated or dedicated client/server lane with local native sneak
  input on prepared material, native player packets and subsequent tick/ROOT
  updates; compare client/server contact, footprint, position and transport.
- [x] I3 Add a real jump/tick proof: native `jumpFromGround`, contact and
  transport sequence before/after support ROOT motion interleaved with the
  jump, and equivalent static-floor control under the same scale, gravity,
  sprint and effect conditions. Assert no sticky contact, no passive debt,
  no added own impulse and normal reacquisition only after a later real landing.
  Include controlled-mount and ineligible controls so the player route does
  not take ownership of unrelated Tiny Mount movement. The client/server lane
  must also press the real local jump input, then observe both peers' relation,
  position, own impulse and transport across the next ROOT/tick/packet boundary.
  Its static-floor control must use the same native input, not a fabricated
  upward delta or a direct server-only method call.
- [x] I4 Repair only classified product gaps. Audit HOT_MOVE_QUERY/HOT_TICK
  complexity and avoid unbounded edge retries, geometry preparation or global
  scans per input. Substantiate a finite retry bound against requested tangent
  length, scale and local piece count, and distinguish existing allocations
  from any new cost. Run focal, integrated prepared, ordinary build and the
  real local-input client/server lane unconditionally, even if production is
  unchanged; obtain independent holdouts, mutation/fault checks and a complete
  zero-change pass before canonical evidence and a logical commit.

## IMPLEMENTER runtime result (candidate)

The installed 26.2 `Entity.move` route invokes `Player.maybeBackOffFromEdge`
before collision, while `LivingEntity.jumpFromGround` writes the own impulse
before the next `Entity.move`. The original prepared `Player.move` proof was
RED: after `jumpFromGround`, contact remained and a support ROOT interval could
carry the player before its first own move. `LivingEntityMixin` now delegates
to `AnatomyMovement.releaseOnJump` at the native jump return. It retires only
an active, predicted body with a real contact and positive effective-gravity
velocity; it does not replace the vanilla impulse. The same prepared proof now
passes its jump/ROOT interleaving and diagonal sneak-on/off movement.

The edge helper snapshots the accepted support once per move and makes at most
128 coarse plus eight refinement sweeps over its local pieces. For P pieces,
its upper query work is 136P and it performs no geometry preparation inside
that loop. The step grows with request length, and exhaustion fails closed at
zero tangent displacement. This replaces an unbounded length/scale retry loop.

The dedicated local-input proof runs in two isolated invocations of the
existing original-catalog client test (`SCALEBREWS_S40_NATIVE_INPUT=sneak` and
`jump`). The sneak lane approaches the measured convex face under native
movement, presses shift, reaches face edge margin zero without losing client
or server support, then releases shift and exits with real movement packets.
Both peers lose contact and converge in position and transport. The jump lane
presses native jump on that same prepared contact. A test-only method observer
records the exact native impulse; a subsequent state read observes contact
retired on the local player. After the server receives the jump movement and
retires contact, an interleaved ROOT cannot move either peer or increase the
transport sequence. The equivalent stone-floor native-input jump produces
exactly the same observed impulse `(0, 0.4482024610042572, 0)`. A ROOT sent
*before* the jump packet is causally known to the server can still be applied
provisionally there; S40's post-packet assertion is the observable retirement
boundary and makes no claim of server knowledge of future input.

Current IMPLEMENTER executions: ordinary build **466/466** in
`../s40-build-candidate-final.log` (SHA-256
`e83d82da73bd232c2f5b8a7810e50e2f7141d558e9759eb6b007163fe46ed6e1`);
full prepared composition **2/2** with 23 scenarios, including the native
`down`, `up` and `north` edge frames, in
`../s40-build-and-prepared-candidate.log` (SHA-256
`b5fc4f00475d92d0389323555c5ac42a959ac88500e6bf5b7b349bfccbfdcda5`).
Native sneak and exit passed in `../s40-client-sneak-close.log` (SHA-256
`8ef01ea60dca00c22b9bab2de3020680c5c49bb968788e7e7da65628c386ee17`);
native jump and exact same-input static-floor impulse passed in
`../s40-client-jump-final.log` (SHA-256
`240612306851705714d559ac176fd831644bba34f1360c5bc9789a834bef457d`);
the general client suite passed in `../s40-client-full-candidate-final.log`
(SHA-256 `1d5dd3b46eb7273addee637a8f30420548209277c8b4a7c380c11daa2192467f`).
These are candidate results. The `up`/`north` frames use prepared exported
cow pieces and the local gravity test seam; installed Gravity Changer gameplay
still belongs to final pack QA. Controlled-mount and ineligible ownership
controls, independent review and a complete zero-change pass remain before
sprint closure.

## ADVERSARY preimplementation model

Active role: **ADVERSARY**. Preimplementation review on S39 HEAD 3b3f8a74;
origin/main currently b5e62535. No production or IMPLEMENTER test changes.

Model: a canonical accepted contact owns edge protection only for its eligible
player/body pair. Native sneak tangent motion must leave a real supported
footprint in the body's gravity frame; releasing sneak must allow genuine exit.
A native jump must retire the old relation before any later support interval can
move that body, preserve its own vanilla impulse under an equivalent control,
and never revive contact or accumulate transport until a genuine later landing.
Read-only presentation/receipt equality is insufficient to prove those physical
transitions. Unrelated world-block edges, third parties and controlled mounts
retain their native ownership. Six frame reconstruction and several native edge
frames are distinct promises; controls compare effective scale, gravity, sprint,
effects, initial motion and authoritative position baselines.

**Plan RED1 — mandatory local-player integration missing.** I2 can be satisfied
by server Player.move and I3 by a direct jumpFromGround call. I4 asks for client
lanes only “where touched”; therefore a no-production-diff candidate can close
with those prepared calls and the existing general client lane, without ever
pressing sneak/jump on a locally controlled player over prepared material.
Counterexample: direct server edge/jump controls pass while LocalPlayer native
input/tick order retains the client relation and a following ROOT carries it,
or the real player packet path incorporates that displacement incorrectly.
Neither I2/I3 nor a general unchanged client suite observes that transition.
Require a real integrated or dedicated client/server S40 lane, even with no
production delta: actual native sneak and jump input, observed client and server
relation/position/transport through subsequent ROOT/tick/packet handling, plus
the equivalent static-floor native-input control. Do not fabricate the jump by
setting upward delta or merely invoking a server method in that lane. This is a
plan/evidence gap for FR-054/063, not an executed production defect. Plan verdict:
**no funciona, arréglalo** until that mandatory lane is explicit.

Performance model: HOT_MOVE_QUERY for native edge clipping and HOT_TICK for jump,
carry and retirement; HOT_NETWORK only if actual integration changes packets.
Variables: body scale, tangent request length, decrement step, candidate pieces,
local contacts and support intervals in a tick. Current edge code decrements by
max(.005,.05*scale) and sweeps each snapshot piece on each retry: approximately
O(ceil(tangentLength/step)*P), with no explicit retry cap. I4 must substantiate its
bounded-retry promise, including finite inputs, preparation cache reuse, and no
repeated geometry/joint work per retry. No observers/world-wide support scan may
multiply this local query. Audit allocations and retirement retention separately;
no timing/FPS claim is made in this preimplementation review.

Independent holdouts and mutation details remain private. Spatial/temporal proof
will use deterministic gravity-frame footprint and jump/contact snapshots beside
machine assertions; snapshots cannot replace actual native input/network proof.
No GameTests were executed for this plan review and no G5 closure is claimed.


### ADVERSARY convergence pass after plan RED1 repair

**funciona, continúa** for the revised preimplementation plan. I2 now mandates
native local sneak input and actual packets/ticks/ROOT with observations on both
peers. I3 mandates native local jump input and the equivalent static-floor input
control. I4 runs that lane unconditionally even without production changes and
requires a finite retry-bound/cost explanation. The historical RED1 above is
resolved at plan level; no new concrete plan contradiction found in this pass.
This is not runtime certification. Holdouts stay private; no production changed.


### ADVERSARY independent runtime review of S40 candidate

Operational verdict: **funciona** for the agreed S40 boundary. Production was
not edited by ADVERSARY. Stable production SHA-256 throughout this review:
AnatomyMovement 1fe729433ba1cef385077283e5fef188cc47c7c8a6ba71c28fb6b7440365ff26;
LivingEntityMixin cf47ca67900f71ed1bce388f715b2e07a3cc3b9ab888869fc763284da576574c.
All tooling/holdouts below live outside production in ../s40-adversary-private.

Independent real-input evidence: native-input-snapshot.log GREEN1m19 and
native-reacquire-grounded.log GREEN1m33, both with clean client shutdown. Actual
local shift/forward and jump keys traverse native integrated client/server
movement and packets over a prepared Cow. Jump retires both peers' relationship
after native packet arrival; a genuine ROOT cannot move the released player or
create transport. Native post-tick own impulse is .33319999363422365 on material
and on the equivalent static floor. The extended lane waits for a genuine native
landing, asserts a new server contact sequence (2 to3), consumes genuine new ROOT
(.07,0,.04), and observes the resulting client position. Contact is not manually
confirmed and position is not teleported during the flight/reacquisition phase.
The later static-floor fixture is separately placed and settled before its input.

Independent immediate-span evidence: immediate-native-first.log GREEN2/2 1m36.
A real prepared contact, native jumpFromGround call and genuine ROOT between its
return and Entity.move demonstrate immediate release and no debt. Own impulse
matches the native no-material control. Isolated mutation removing only the
production jump-return hook fails the exact immediate oracle in
jump-hook-mutation-current.log (expected RED1m47, 'Jump must retire contact at
return before Entity.move or another interval'). Production files were never
swapped; the mutation used an external source overlay/build/run directory.

Independent frame evidence: six-prepared-current.log GREEN2/2 39s. Player.move
through its native edge hook on a prepared static cube clips shift-on requests
and permits shift-off departure in DOWN/UP/NORTH/SOUTH/WEST/EAST. This uses the
canonical test gravity-frame seam and the actual physical AABB. It does not
claim that an external Gravity Changer installation or six-frame local input
jump controls were executed. The prepared export and native real-input lanes
supply separate integration evidence.

Independent cost evidence: work-final.log GREEN2m5. Test-only ThreadLocal sweep
instrumentation measures 100 long finite edge queries after one warmup:
128700 sweeps <=137000 (137*10 local pieces*100); zero additional model geometry
or joint evaluations, no body movement and no receipt mutation. Reflection to
locate the existing evaluator occurs outside the measured loop. HOT_MOVE_QUERY
now fetches one support snapshot per query; at most1 endpoint probe+128 coarse
probes+8 refinements each visit local pieces. Complexity is O(137*P), independent
of tangent magnitude; no global support/world/observer scan, reflection,
persistent history, new lock or preparation is introduced. Temporary vectors,
AABBs and sweep results still allocate: zero allocation/FPS/TPS is not claimed.
The HOT_TICK jump hook uses existing ownership/contact checks and clear paths;
nonparticipants short-circuit, with no new scheduling or retained state.

Spatial evidence: footprint-snapshot.json and footprint-contact-sheet.png were
rendered from actual native-input-snapshot.log bounds and visually inspected.
Fixed top-down projection at240 pixels/block shows a supported footprint overlap
.00704817977872052 x.5 blocks and normal gap0. This depicts measured contact;
it is not a framebuffer/human gameplay proof or alone evidence that input stopped
at an edge. Native edge clipping and shift-off controls above provide that proof.

Fixture failures remain durable and are not hidden as product reds. The first
client jump check ran before the server received its movement packet. The first
work probe set an entity shift flag instead of LocalPlayer input, then lacked an
initial physical-cache warmup. Auxiliary compile errors used wrong presentation
accessor/import/scheduler names. The first six-frame fixture registered the raw
kernel while its public mode remained DISABLED, so it exercised vanilla; READY
prepared ownership fixed its scope. An extended input attempt pressed jump while
the preceding native material transition/cooldown was not settled. The final
lane waits12 neutral ticks and asserts actual grounded contact before pressing
jump; native jump impulse and release assertions are retained.

The final IMPLEMENTER ordinary build (466/466), prepared multi-frame and complete
client logs were inspected as corroboration, not attributed as ADVERSARY runs.
The ordinary suite includes controlled-chicken native jump cancellation and
other mount/jump controls; production TinyMount ownership code remains untouched.
No new concrete product RED remains. git diff --check passes. No whole-G5/G9,
third-party pack integration or deferred human acceptance is claimed. Final
source/log/snapshot hashes are in ../s40-adversary-private/evidence.json.

## IMPLEMENTER final review and closure

The independent ADVERSARY pass found no remaining S40 product RED. Its actual
local-input holdout reacquired a new material contact only after native landing,
and a later ROOT carried that new relation. Its isolated removal of the jump
hook made the immediate post-jump oracle RED, while the unmodified candidate
passed. Six prepared gravity frames and the bounded-work probe passed without
new geometry preparation or state mutation. The adversary left production and
IMPLEMENTER tests unchanged; its evidence and limitations are recorded above.

The final unchanged tree passed `build runGameTest` with **466/466** ordinary
server tests (`../s40-zero-ordinary-final.log`, SHA-256
`57fd67101417be230e35a6c45661f86a1f33e167cfd2678f72818b4d83a30c7b`),
and the prepared composition with **2/2**, including 23 scenarios
(`../s40-zero-build-prepared-final.log`, SHA-256
`633aafbd6c785cc27236ed4c34c72b07071baafb56c446a3049ee4e5cab0ae5c`).
The final dedicated local-input sneak and jump runs passed with explicit peer
position convergence in `../s40-client-sneak-assert-final.log` (SHA-256
`9e68f1456d13225c554f86754f73bcd7a6a9b3bb3d4710133779e7773c436d86`)
and `../s40-client-jump-assert-final.log` (SHA-256
`f34a751e71f7c86f0703a36a8e6018c04899e374cfdc952858f584f2267898db`).
The full client rerun passed in `../s40-zero-client-rerun.log` (SHA-256
`cbb7245c0595f4382c102a47d11947f6d69bb46f3861c5e08fe83368e5a11f20`).

One preceding full-client execution, `../s40-zero-client-final.log`, timed
out at an S29 catalog-certificate wait. The same suite had passed earlier on
the candidate and passed on the unchanged tree in the final rerun. No S40
source or oracle was changed to obtain the rerun. This is classified as an
unreproduced test-execution timeout; its precise environmental cause is not
established. A separate prepared attempt used the wrong Gradle selector and
ran the ordinary entrypoint against the prepared catalog; the corrected
selector passed. Neither failed invocation is counted as acceptance evidence.

Final IMPLEMENTER diff review found no second physical owner, unrelated
mainline edit, dead production branch or new retained state. `git diff --check`
passed. FR-054/063 and G5 task 3 close for this sprint. Whole G5, experimental
minecarts, legacy retirement, G9 benchmarks and final human pack QA remain open;
human-only checks stay deferred to epic completion as requested.
