# S37 / G5.4 — off-rail minecart material contact and rail handoff

Status: S37/G5.4 ordinary minecart behavior closed locally after independent
ADVERSARY holdouts and a zero-change review. S36/G5.3 closed at `9511a716`.
Experimental `MINECART_IMPROVEMENTS` behavior, the wider G5 gate and G9 remain
open.

## Scope and thesis — IMPLEMENTER

At the end of S37, a real off-rail minecart can acquire and follow one prepared
canonical living material contact. Entering a real rail retires that contact and
returns movement to Minecraft rail physics; leaving the rail permits a fresh
material contact. A cart that is ineligible for a particular support keeps
its native physical interaction. The proof must exercise real cart movement and
ticks, not a manually toggled `setOnRails` flag alone.

This is the minecart slice of FR-007/065 under G5. FR-009/010/053/055-058/093
and NFR-001/004/007/008/033 remain neighboring constraints. S35 covers items
and S36 covers boats/rafts. Falling blocks, legacy-engine deletion, rendering,
and global G9 pack acceptance are separate work. Any human gameplay review is
recorded for the end of the epic, as requested by the owner.

## State reconstructed — IMPLEMENTER

The current local `main` is `aeca64e1`; the working branch is
`chatgpt-editing`. No mainline minecart-specific change was found in the
inspected paths. `BodyClassification.category` already returns `minecarts`
for an `AbstractMinecart` only when `isOnRails()` is false. `Platforms.eligible`
uses that category with canonical binding, policy and effective width ratio.
`AnatomyMovement.afterMove` and `supported` recheck eligibility, so rail entry
may already retire a previous contact. This is a hypothesis, not acceptance.

The existing `PlatformTests.offRailCartAndPassiveAccounting` uses a ghast
legacy surface and calls `setOnRails(true)` before `PlatformPhysics.carry`.
It proves neither prepared canonical contact nor real rail reacquisition and
native movement. The S35 prepared server harness and S36 real-vehicle lane
offer reusable fixture patterns, but the cart's Minecraft 26.2 rail state,
tick/behavior hierarchy and real transition ordering were checked in the
installed 26.2 bytecode before implementation. `AbstractMinecart.tick` delegates
to `MinecartBehavior`; `FeatureFlags.MINECART_IMPROVEMENTS` selects the new
behavior. The ordinary old behavior sets `onRails` from a real block check
before track/off-track motion. The new behavior has a different track iteration
and client interpolation, so a fixture's feature set must be identified before
attributing a result. No cart-specific carry loop or parallel rail state should
be introduced.

## Provisional implementation plan — IMPLEMENTER

- [x] I1 Verify installed 26.2 cart and rail APIs/authority, then establish a
  real off-rail cart on a prepared canonical support. Exercise `move` and
  `tick`, assert exact contact identity/serial, `PlatformState` absence and
  unique hull/passenger ownership. Include ordinary, chest and powered/cart
  variants only to the extent their native lifecycle changes the shared
  boundary; do not replace their Minecraft behavior.
- [x] I2 Prove one resting cart ROOT translation and nonidentity rotation
  carry, obstruction/release without delayed debt, and a policy/ratio-negative
  pair with vanilla collision/push. Use actual physical displacement and
  structural work bounds rather than `Platforms.supported` alone.
- [x] I3 Build a real rail transition: off-rail material → rail entry → rail
  travel → rail exit → fresh eligible material contact. Check native rail state,
  position/velocity, contact retirement and no duplicate material carry while
  on rail. Include a nearby nonparticipant control and an unfavorable ordering
  of support movement and cart tick.
- [x] I4 Run targeted prepared, ordinary and any affected client/dedicated
  lanes, then independent ADVERSARY holdouts, hot-path locality review and a
  final zero-change pass. Record exact sources/logs in `VALIDATION.md` and G5,
  and create a separate logical local commit. No push, tag or release.

## IMPLEMENTER runtime candidate

No production file changed. `S37PreparedMinecartProof` runs in the actual
isolated prepared server session and uses `AbstractMinecart.move`/`tick`.
Ordinary, chest and furnace carts acquire a named canonical cow piece/revision
off rail without `PlatformState` ownership; their real off-track ticks retain
the relation, and a chest inventory survives. The occupied ordinary cart alone
follows one ROOT translation and nonidentity yaw while its pig passenger has
no second material contact. The ratio-rejected cart preserves native entity
collision and one actual push. A real block wall clips a 1.2-block ROOT
interval, and removing that wall cannot replay the blocked part. Support
retirement clears the contact.

The rail fixture places five physical rail blocks with a stone bed at the
prepared body's edge. The ordinary cart's actual old-behavior tick detects
rail entry, releases canonical contact, travels natively along the rail, and
ignores a subsequent ROOT move of its former support. It then leaves the end
of the track and acquires a fresh material contact on a second prepared cow,
which carries it once. The second cow is inserted after physical rail exit:
placing it at the adjacent track end before departure made the native cart
stop against the cow rather than leave the track. This is a valid dynamic
support-arrival/reacquisition case; a pre-existing adjacent support handoff is
not claimed by this fixture. The fixture reports `OldMinecartBehavior`; the
experimental `MINECART_IMPROVEMENTS` behavior is not claimed by this run.

Test-setup failures were classified before candidate acceptance: ordinary
465/465 runs initially omitted the prepared selector; the first prepared run
omitted its required isolated client-export catalog; a nine-block unpowered
track exceeded the empty cart's coast distance; and a .6-block ROOT failed to
reach the wall because the cart is narrower than a boat. These are not product
REDs and did not weaken the final physical assertions.

The correct targeted `runGameTest` selected
`scalebrewsAnatomyPreparedSuite=true`, `scalebrewsS37Only=true` and the
isolated S36 client-export catalog; `../s37-prepared-candidate.log` passed
**2/2**, SHA-256
`36596c421453e66c0c6d8e484fddcb7832c7e6fad85da211d728cc106bbed53c`.
`../s37-ordinary-candidate.log` passed **465/465** and `BUILD SUCCESSFUL`,
SHA-256 `b7925714aeb6f88901762d86fb41f4be1c6e3736e945cf807090b0c9c56f56cb`.
Proof source SHA-256 is
`f47f22af7abe8ec98d7f06fcda085d2921938685833a2487c2498ee3637ef377`.
No shared/client mixin or runtime resource changed, so a client rerun is not
attributed to this candidate. The independent holdouts, performance audit and
final zero-change review are recorded below. This closes only the ordinary
minecart behavior exercised here; the experimental feature-flag path is not
silently certified by the standard fixture.

## ADVERSARY preimplementation model

Role: **ADVERSARY**. Independent plan review on `9511a716`, local main
`aeca64e1`: **funciona, continúa**. No S37 production or runtime verdict.
Canonical G5/VALIDATION confirms S36 closure; the provisional status line above
predates that closure. I1–I4 converges for the FR-007/065 slice.

Model: the cart is the transported root; Minecraft owns passenger seats,
inventory, powered behavior and rail physics. One current material identity can
transport that root only off rail. Actual displacement, retirement and fresh
reacquisition must be compared against native nonparticipants; `supported`
alone cannot certify them. Independent tests will cover retained ROOT
translation/rotation, occupied-root ownership, native rejection/push,
obstruction without debt, and real rail entry/travel/exit. Deterministic
geometry/seat snapshots supplement semantic assertions. Precise holdout
inputs/orderings remain private. Variant selection follows native lifecycle
boundaries, not a redundant type enumeration.

Installed 26.2 bytecode confirms `AbstractMinecart.tick` delegates to behavior;
`FeatureFlags.MINECART_IMPROVEMENTS` selects Old/NewMinecartBehavior. Old server
tick sets rail state before track/off-track motion; new behavior updates it
inside track iteration and uses distinct client interpolation. Experimental
`AbstractMinecart.move` may perform a second native move after push/pickup.
I1 must identify the actual fixture behavior/feature set; evidence for one
must not be silently attributed to the other. Server ticks execute real cart
rail physics, unlike the local-client-only boat rowing branch. Any changed
shared/client integration still requires its actual client lane.

Cost model: HOT_TICK/HOT_MOVE_QUERY, with network/presentation classified if
changed. Variables are local candidates, pieces, transported roots, passengers
and native track iterations. Review locality/bounds, shared geometry reuse,
allocations and absence of global scans, hot reflection/preparation or an
alternate rail/history owner. Structural counters/native controls supplement
timing; global G9 remains open.

Evidence: `../s37-adversary-cart-bytecode.txt`, SHA-256
`3820d3d93eab0984a8b9b17d270ff681a0d2f1fe588f7fd5f69df1256eb798c5`.
Current category and supported/afterMove paths were inspected against canonical
requirements/architecture/G5. `git diff HEAD -- src/main src/client` is empty.
No S37 runtime tests were executed in this planning review.

## ADVERSARY candidate review and operational verdict

**Funciona.** Independent final review of the S37 candidate on `9511a716`
found no production change or remaining concrete defect in the ordinary
minecart slice. No IMPLEMENTER section or production/test source was edited
by the adversary. G5 beyond minecarts and global G9 remain open.

Two independent private server oracles completed **2/2 each** in separate
adversarial build/cache/run directories. `boundary-final.log` passed in 17 s:
ordinary, chest and furnace carts at fixed initial yaw acquire canonical-only
contact, consume one 17-degree ROOT rotation at an offset material point,
retain native contents and refresh the occupied cart seat. Repeated drain
cannot replay carry; a nearby nonparticipant stays unchanged and each yaw
capture visits precisely one transported root. Physical rails are installed,
the support moves before native cart detection, and a stationary actual tick
retires contact. Later ROOT cannot move the on-rail cart. Removing the real
rail permits fresh acquisition and consumption of only the next interval.
The fixture reports OldMinecartBehavior explicitly.

`native-rejection.log` passed in 18 s. Both ratio and disabled-policy rejection
preserve READY body classification without canonical/legacy contact. Actual
cart displacement equals the independently computed native block/entity clip
`(0,-0.3999999999999986,0)` and native push remains effective.

The unchanged public prepared S37 selector was independently repeated in those
own directories: `public-repeat.log`, **2/2**, BUILD SUCCESSFUL in 28 s. This
covers the candidate's real rail travel/dynamic support arrival, wall/no-debt
and support-retirement cases. IMPLEMENTER's ordinary **465/465** log was
reviewed, not attributed to a new adversarial execution. Production is empty
under `git diff HEAD -- src/main src/client`; no shared/client production or
resource changed, so no fresh client runtime is claimed for this test-only
candidate.

Cost review: no production path changed. Existing eligibility/contact queries
are HOT_MOVE_QUERY/HOT_TICK; they use current canonical identities, bounded
local material capture (128 bodies, 64 envelope span) and bounded solver work,
not an added cart-specific state owner, reflection or global support scan.
Each of the three variants ran 10,000 current-support queries with **zero new
geometry and joint evaluations**. Final measured total query times were
146,858,600 / 102,894,200 / 91,285,600 ns (ordinary/chest/furnace), including
assertion overhead, without a normative FPS/TPS or G9 budget claim. Native
track loops were not altered. No cache/history/packet fanout was added.

Deterministic point/seat comparison `root-contact-sheet.png` was rendered and
inspected against material-reference positions from `root-snapshots.json`.
Measured ROOT/reference and native relative-seat error are zero for the final
capture. The fixed XZ projection supplements machine assertions; it is not a
Minecraft framebuffer, experimental-client interpolation proof or human pack
QA. Its initial label overlap was repaired in the private renderer only.

Fixture classification: the first compound lane produced quarantine at the
third yaw. The chest cart's native discard spills inventory into the next
cart's spawn area (`AbstractMinecartContainer.remove` calls dropContents when
the removal reason destroys). This invalidated the oracle's free single-body
assumption. The isolated furnace and yaw-only reduction passed; the final
fixture clears the already-verified container only during cleanup, pins yaw,
and keeps unmounted controls outside the envelope. The full compound lane
then passed with zero quarantines. Earlier logs remain preserved as fixture
failures, not product REDs. No production fix or oracle tolerance reduction
was performed; no production mutation campaign is claimed.

Reproduction from repository root with the installed Java 25 runtime:
`./gradlew.bat runGameTest -PscalebrewsBuildDir=../s37-adversary-build
--project-cache-dir ../s37-adversary-cache
-I ../s37-adversary-private/server.init.gradle` selects the private boundary.
Add `-Ps37PrivateProof=io.github.r3neer.scalebrews.test.S37PrivateNativeRejectionProof`
for the native-negative oracle. For the unmodified public repetition use
`-Ps37PrivateProof=io.github.r3neer.scalebrews.test.AnatomyPreparedIntegrationProof
-PscalebrewsS37Only=true -PscalebrewsAnatomyPreparedSuite=true`.
These private tooling paths preserve remaining holdouts outside production.

Manifest: `../s37-adversary-private/final-evidence.json`; pins current
production/resource files, public proof and executed private artifacts.
SHA-256: ad37cf7f8802377d68d2411eea6361e9af51b4843275e71136fa1ec81071a158.
