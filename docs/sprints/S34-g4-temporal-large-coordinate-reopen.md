# S34 / G4 reopen — temporal response at large world coordinates

Status: S34/G4 repair closed by integrated server/client tests and an independent
ADVERSARY zero-change cross-sprint review. The logical local commit records
this closure. The final S33 review found the
existing prepared A9 suite RED even with S33 disabled; S33 cannot close under
the sprint workflow until this G4 reopening is independently repaired and
retested. No prior G4 result is rewritten.

## Scope and thesis — IMPLEMENTER

At the end of S34, the real prepared cow's interior-only ROOT interval must
produce the same safe material response at every reproducible valid-world
translation used by the acceptance fixture, under the existing 32 event / 256
evaluation budget. A genuinely exhausted query still fails closed; there is no
budget, base skin or owner increase. The numerical ulp allowance is a separate
bounded precision policy to measure and review. This reopens the numerical/work
boundary of FR-043, FR-048, FR-049 and NFR-001/002/004/007, not the S33
placement transaction or G5 special-body migration.

## Baseline and uncertainty — IMPLEMENTER

The current S33 branch's full prepared lane fails the older S09 A9 proof with
`ITERATION_LIMIT` at X≈11.43 million/Z≈8.11 million. The same failure occurred
with S33's placement fixture skipped in `../s33-prepared-skip-run-2.log`, so
the placement proof is not the trigger. In a fresh GameTest world, the runner
still placed fixtures at X≈-4.66 million/Z≈-14.57 million. This run
(`../s33-prepared-fresh-run.log`) reached A9: the chosen causal piece resolved,
but the complete cow manifold exhausted all 256 evaluations after two
contacts while the next piece resolved CLEAR in isolation. The new world
rules out stale contents; it does not isolate a single numerical cause.

At X≈11.43 million/Z≈8.11 million, the skipped-S33 run fails even on the
chosen `root/head/cube_0` alone: direct CCD at 256 evaluations is exhausted
near t=0.284156736; the same query finally reports CONTACT at 718/4096
evaluations near t=0.284156755. Its midpoint is physically penetrating, so
the test is not an endpoint-only false positive. This is a separate
pre-contact numeric/work symptom from the fresh world's post-contact
fourth-piece exhaustion below.

The fresh trace narrows the work frontier: `root/body/cube_0` clears in 82
evaluations, `root/body/cube_1` in 83, adding `root/head/cube_0` produces a
complete two-contact response in 223, and adding `root/head/cube_1` exhausts
256 at contact time 0.2842733117. That fourth piece returns CLEAR in one
evaluation when queried alone from the initial body, so its post-contact
interval and the shared scheduler must be measured together. The observed
double contact occurs at X≈-4.66 million/Z≈-14.57 million. This does not yet
prove whether projection precision, post-contact correction or ordering spends
the remaining 33 evaluations.

S09 previously fixed A9 at other translations without raising the normative
budget. Its post-close record identifies earlier causes in shared work
accounting, post-contact scheduling and an adaptive midpoint-positive first
contact probe. Those successes do not certify every world translation.
`ConvexBox` currently projects world-space vertices and AABB centers onto
SAT axes separately, then subtracts large nearby results. Relative projection
around a common nearby origin is a plausible precision improvement, but the
fresh full-manifold failure may also involve scheduler cost after contact.
Both possibilities must be measured before choosing a production change.

## Provisional implementation plan — IMPLEMENTER

- [x] I1 Reproduce A9 deterministically at both failing translations and at
  a near-origin control with the same exported cow catalog/body interval. Log
  per-piece screening, first-contact and post-contact evaluation counts without
  changing the 256 budget or solver outcome. Translate the same prepared live
  motion/body before the current first full-manifold assertion, because that
  assertion presently aborts before its existing translation controls run.
- [x] I2 Compare `ConvexBox` SAT gap/normal and `ConservativeSweep` progression
  under a common translation in double precision. If subtraction of large
  absolute projections is causal, compute box and body intervals in one nearby
  frame for separation, overlap, sweep and correction, preserving skin and
  tangency policy. Do not claim this fix until the real prepared manifold passes.
- [x] I3 If the remaining loss is work ordering/accounting, repair the single
  existing `TemporalResponse` scheduler with measured evidence. Every probe
  still charges the shared budget; no relevant piece is silently skipped and
  no ambiguous contact is converted to CLEAR.
- [x] I4 Add translation-equivariance controls for an interior-only hit, clear
  piece, tangent and multi-contact manifold at positive/negative millions,
  including coordinates near the vanilla border. Assert earliest contact,
  displacement/contact identity, fail-closed exhaustion and deterministic
  candidate order, not just a green status.
- [x] I5 Run targeted kernel, ordinary GameTest, full prepared catalog, client
  if affected, independent adversarial holdouts and zero-change final review.
  Record exact source/log hashes in `VALIDATION.md` and make one local commit.

## ADVERSARY independent model and verification

Active role: ADVERSARY. Work starts while S33 is blocked by required A9 RED,
under the explicit instruction to review/prepare S34 before production changes;
S33 need not be declared closed to investigate its blocking G4 foundation.
S33 placement oracles, assertions and evidence remain intact. No production,
IMPLEMENTER tests or IMPLEMENTER plan sections are changed by this role.

### Independent contract before candidate

The observable is continuous material response, not a preferred SAT formula.
Apply a common world translation to physical body and every point of every
material trajectory, retaining time, identities, deformation/linear velocity
bounds and correctly translated invariant planes. Neither body scale nor
motion bounds may change as an accidental fixture effect. Construct geometry
in doubles locally, then translate: float world matrices would test a different
source of error. A translated result is compared with the same unshifted input,
not a separately generated cow or newly selected piece.

For the normative A9 real prepared interval, require COMPLETE, nonzero response,
continuous interior hit, endpoint-clear body and no invented retained endpoint
anchor under the existing 32-event/256-evaluation budget. Execute original,
near-origin and each failing translation before aggregating assertions, so the
first failure cannot hide comparison controls. Preserve the full manifold;
passing a selected piece alone cannot certify the multi-contact response.
Capture exact immutable interval/catalog and body inputs for replay. A direct
kernel test cannot replace live publication/drain/authority assertions.

Translation equivalence uses documented bounded double-representation error,
contact identity/order, earliest contact time/normal and final clearance. Never
require bitwise equality between distinct translated floating-point inputs;
never widen skin or comparison tolerances after seeing a candidate failure.
Identical causal inputs and registration permutations must remain deterministic.
Compare clear, tangent, initial overlap, interior-only and multi-contact cases;
query endpoint and intermediate samples so a CLEAR/no-motion shortcut cannot
pass by omitting a genuine contact. Existing invalid/nonfinite/degenerate input
rejection remains mandatory. Signed/mixed large translations and small extents
must not collapse into coarse float geometry.

Keep raw-kernel diagnostic prefixes separate from live integration: genuine
exhaustion must export zero displacement and no partial contacts from the live
boundary. A low-budget intentional exhaustion is a negative control, never
converted to success. No local retry may restart the total budget; no candidate
may be silently skipped. Added irrelevant clear pieces and registration reversal
exercise locality/order while retaining physically relevant pieces.

### Preimplementation preparation and evidence protocol

Private test source/selector lives in ../s34-adversary-private, with own
../s34-adversary-build, cache and run directories. The first independent kernel
fixture constructs a double-precision rotating affine bar and a stationary body,
then evaluates all translations, order reversal with a clear piece, and the live
one-evaluation fail-closed boundary. It is a complementary synthetic oracle,
not evidence that the exact prepared cow A9 passed. New holdout inputs remain
private until a classified defect is delivered. Existing A9 logs retain their
required RED status; no baseline or candidate result is invented.

Mutation adequacy will challenge endpoint-only substitution, omitted relevant
piece, uncharged probe/restarted budget, arbitrary success on exhaustion,
raw-prefix export, and order-dependent scheduling. Targets are requirement
mutations rather than an assumption that relative projections are the repair.
A mutation applies only once a corresponding baseline oracle is classified.

HOT_MOVE_QUERY is the affected production surface; scaling variables are
candidate pieces P, active contacts K, interval samples M and event iterations E.
Record charged evaluations separately from actual geometry samples, first-hit
and post-contact work. Measure bounded allocations/work on equivalent inputs
and absence of preparation/reflection/whole-catalog scans in the solver loop.
Do not demand identical evaluation counts across translated floating-point
inputs, but explain significant added work and enforce the same global limits.
No normative FPS/TPS or heap-soak acceptance is inferred from kernel timing.

Deterministic visual evidence will plot local-frame material samples, stationary
body, earliest contact and resolved trajectory for reference versus translated
candidate; metadata pins seed/time/catalog and source hashes. Subtract a common
nearby origin for drawing, without changing machine assertions. A contact sheet
supplements real prepared/live and quantitative acceptance.

Plan review: I1-I5 are dependency-coherent, preserve the normative budget and
fail-closed behavior, and condition the repair on measured numerical/scheduler
cause. Relative projections are a hypothesis, not a prescribed green shortcut.
No production design contradiction identified in this pass. Future closure
requires real A9 replay plus ordinary/prepared/shared-client relevant suites,
independent holdouts and a final zero-change review; S33 remains blocked until
its required prepared regression is repaired.

Preimplementation fixture classification: the first synthetic run compiled and
completed every translated trajectory within budget, but its oracle required an
identical count of raw Contact records. Large translations can cause a second
numerical retry at the same piece/time/normal; raw diagnostics are not distinct
physical manifold contacts. This run is classified UNPROVEN oracle strictness,
not a new production RED. The comparison now groups consecutive same physical
contacts within fixed time/normal bounds, retaining raw logs and independently
comparing displacement, status, first contact and order. No production changed.
A9 required RED remains unchanged. This adjustment does not discard different
pieces, separate event times or differing normals.

Preimplementation baseline execution completed: own kernel fixture plus sentinel
GREEN2/2, BUILD SUCCESSFUL1m16s, clean server shutdown in
../s34-adversary-private/preimplementation-canonical-baseline.log. All synthetic
translations COMPLETE, 55-62 charged evaluations; order controls and live
one-evaluation exhaustion assertions passed. This does not repair or replace
real prepared A9 and supplies no S34 closure claim. All five source hashes in
preimplementation-source-hashes.json still match; S33 native-cell manifest
fingerprints also remain unchanged. Operational plan verdict: funciona, continúa.

## IMPLEMENTER measured I1 localization (2026-10-04)

Before changing the solver, temporary opt-in GameTest traces replayed one
actual prepared cow motion/body at the origin and both previously failing
X/Z translations, with S33 skipped and 32 events/256 evaluations unchanged.
The logs retain their expected A9 RED classification. In
`../s34-phase-diagnostic.log` (SHA-256
`8efe9ff77bae2390929470f48e45aaed797a078ba3262395a85a6a9a5a9835ba`),
the selected head piece completed in roughly 106-110 evaluations at all
three translations, but the 10-piece manifold exhausted 256 even at the
origin. Its first failing progressive prefix varied with numerical input.

`../s34-piece-diagnostic.log` (SHA-256
`fc3327e438f43921a07bd79ddf0813420873822d14a1d3bbef8841a84c8b1f51`)
compared direct CCD per piece: eight unrelated pieces cleared in one
evaluation in that sampled motion, while the body piece needed 98 and the
causal head 81 at the origin. Full response still exhausted 256. In a
different prepared yaw, `../s34-scheduler-diagnostic.log` (SHA-256
`25df0cb4f492bec59e0c5589b173b2cf635e1698cc803182889635d4b2476888`)
measured the same causal piece at 432 direct CCD evaluations near the origin
but 87 at X≈11.43 million/Z≈8.11 million; the 10-piece query used all 256
before its first certified contact at origin. Thus absolute SAT projection
precision and shared scheduling must both be retested, rather than assuming
one translation or one causal piece is representative.

The opt-in `S34_DIAG`/`S34_PIECE`/`S34_EVENT` instrumentation is temporary and
will be removed after a durable candidate comparison. It changes no budget,
outcome or authority boundary. I2's first candidate computes material and
body intervals against one nearby origin in `ConvexBox`; candidate results
remain pending below.

### IMPLEMENTER I2 candidate and integrated retest

`ConvexBox` now subtracts the same body-center origin before projecting each
material vertex and the body center on a SAT axis. The comparison still uses
the original skin, separation/tangency inequalities, axes, 32-event/256-query
budget and temporal scheduler; `TemporalResponse` has no retained change.
The candidate applies to overlap, separation, escape-vector and exact static
sweep intervals so these consumers agree on the same local frame.

With the temporary diagnostic enabled, the real 10-piece prepared manifold
completed at origin and both failing translations in 170/171/171 evaluations;
the selected piece used 67/68/68 direct CCD evaluations
(`../s34-relative-prepared.log`, SHA-256
`d69923e000b1e69aa74b1f76eb55489e657289be586c8e1e3d79337ee7bc0ce5`,
BUILD SUCCESSFUL). The instrumentation was then removed from production and
test, leaving permanent A9 controls that execute all six translations before
aggregating errors and compare the first causal piece/time/normal, displacement
and endpoint clearance. The full prepared suite with S33 enabled passed 2/2
on this clean candidate (`../s34-full-prepared-candidate.log`, SHA-256
`6d320c4b15fc5a846986b215cff1354ecb8d5c47b3dae0e7e1a2556c7bd43917`,
BUILD SUCCESSFUL). Ordinary/client and independent tests remain pending.

### ADVERSARY candidate SAT review RED (2026-10-04)

Production candidate ConvexBox SHA256
8709f93550034dece4bc60499c9ff19830605ba0c55b73d7c016f3084735fc19.
TemporalResponse and ConservativeSweep retain preimplementation hashes; S33
production/oracles are intact. No production or IMPLEMENTER tests changed.
Own unchanged TranslationProof passed2/2 in candidate-translation.log (2m01s).
New complementary SatProof passes shear translation, hit/miss, tangent/clear,
penetration escape and measured projection work; its result is one GREEN among
the final [++X] three-test combined lane, the third failure being prepared A9.
The bounded allocation sample for20,000 separation queries reports baseline
65,590,192B vs candidate40,800,000B; times291,039,900ns vs117,576,600ns. This
one JIT-affected measurement is not normative FPS/TPS acceptance and establishes
no unexplained allocation increase in that sample.

The required real prepared suite still fails A9 on the candidate. Own combined
lane ../s34-adversary-private/candidate-sat-prepared.log at166/176 records ALL
six translated manifolds ITERATION_LIMIT256, including near-origin. At origin
contact time.28393440796885855, raw correction is approximately
(-3.1231602368e-6,0,-2.4991738906e-6) and two root/head/cube_0 contact records;
no COMPLETE response exists. Both previously failing target translations
exhaust256 before publishing a contact. The live GameTest fixture begins at
(9767565,-59,6061222); the logged translated comparisons are of that exact
prepared material input. BUILD FAILED1m37s, clean server shutdown.

Classification: production G4/FR-048..049 temporal work acceptance failure;
local SAT projections alone do not establish the required A9 green across
real prepared inputs. This is not a new placement defect and not grounds to
raise budget, suppress pieces, skip the prepared suite or relax the oracle.
Prior diagnostic170-171 results remain scoped to their captured inputs.

Exact command, with JDK25 environment and own dirs:
`./gradlew.bat runGameTest -PscalebrewsBuildDir=../s34-adversary-build -Ps34PrivateProof=io.github.r3neer.scalebrews.test.S34PrivateSatProof,io.github.r3neer.scalebrews.test.AnatomyPreparedIntegrationProof -PscalebrewsAnatomyCatalog=../s29-adversary-run/anatomy-export --project-cache-dir ../s34-adversary-cache -I ../s34-adversary-private/server.init.gradle --console=plain`
Preserve own run world for its exact fixture origin. Evidence manifest
../s34-adversary-private/candidate-red-evidence.json pins sources and logs.
Verdict: **no funciona, arréglalo**. S33 remains blocked. Visual/candidate closure
and mutation adequacy beyond these controls remain pending until the repaired
real prepared baseline is green.

## IMPLEMENTER revised candidate after the independent RED (2026-10-04)

The RED exposed two distinct costs that the first relative-SAT patch had not
covered. In a fixed second prepared yaw, the first interior head contact costs
roughly 90 evaluations; after that contact, unrelated body/head/leg pieces
consume the remaining budget if their entire residual trajectories start with
an eight-evaluation CCD probe. A bounded, charged fixed-plane cover of at most
10 samples before that probe proves those residual paths CLEAR, preserving the
exact CCD fallback whenever the cover is ambiguous. The already-known earliest
contact similarly bounds subsequent piece queries to the simultaneous-contact
window, with a charged 32-sample cover before exact CCD. A witnessed midpoint
overlap goes directly to chronological search instead of repeating a long
inconclusive probe. Every piece remains in the search and every sample charges
the same 256-evaluation response budget.

The translated near-origin replay had a different failure: its prepared boxes
were originally sampled at million-block coordinates and then moved near zero.
Using only their destination-coordinate ulp to set CCD's numeric band discarded
the precision already lost at sampling. `ConvexBox.move` now retains the
largest coordinate ulp in the box's arithmetic history; CCD and temporal
certificates use the larger of that provenance and the current body/box ulp.
The base `SKIN=1e-6` and the existing maximum extra band of `SKIN/4` remain
unchanged. The measured allowance factor changed from 8 to 13 coordinate ulps;
at the vanilla border that term is below `5e-8` blocks. This is a numerical
contact-policy change, not merely a work optimization, and requires explicit
adversarial scrutiny of tangent/clear controls. A six-level chronological leaf
search is needed on the near-origin prepared replay: restoring the historical
depth eight made the same fixture RED at 256 evaluations in
`../s34-final-prepared.log` (SHA-256
`e980289767e7657e5568fc2fb745a38623fd76857b71d6805255e08fc9f1bcdc`).

With depth six and temporary traces removed, the real prepared server suite
passed **2/2**, including both fixed interior-only cow yaw cases and six
translations of each complete manifold, in `../s34-final-prepared-depth6.log`
(SHA-256 `133dc6ddab5c2ff8af8f4afd9858fcc89830287dacd98c045b61a6c3efa41198`).
The ordinary `build` passed **463/463** GameTests on the same source in
`../s34-final-build-depth6.log` (SHA-256
`50ab58e75f43eebb76ef24df33a4d06b96a2230e4e9651c45b3d654f5d7ea153`).
Source SHA-256: `ConvexBox.java` `591caca1f655d8d3fb785d9721758f880bf12491b4b8a461c94ffc6d82c05beb`,
`ConservativeSweep.java` `86cf8d9e148032aa7888f6870761bd35bac0a492efe4daa4cb4646c701ab20ca`,
`TemporalResponse.java` `b93da6b1404aeeb27b6877e893e1ab87686c303fa249bfe252ac9cb438cdc12e`,
and `S09PreparedIntermediateContactProof.java`
`cc079b472d0aacb7fff6f3c2526f7e2039a7b28398a90ff46306c812317b7db0`.
The ordinary client GameTest completed `BUILD SUCCESSFUL` in
`../s34-final-client.log` (SHA-256
`562ca9b6f3b781265aebc1a8fb9595072a4609b292c808ce64c2ee754e003bbb`).
This is an IMPLEMENTER candidate, not an independent closure. Adversarial
holdouts and zero-change review remain pending.

### ADVERSARY repaired scheduler / numeric policy GREEN (2026-10-04)

Reviewed updated candidate as a material plan delta: numerical precision
allowance changes from8 to13 coordinate ulps and retains source/translation ULP
history. Base SKIN, hard quarter-SKIN numeric cap, event32 and shared evaluation
256 remain unchanged. The new ULP provenance is part of the material's numeric
input; moving a sampled large-world box near zero does not recreate precision.
This review does not silently redefine the earlier no-tolerance-change plan.
Own additional oracle checks an exaggerated inherited ULP still cannot admit a
gap above SKIN + quarter SKIN or suppress a deep initial penetration.

Pinned candidate SHA256:
- ConvexBox:591caca1f655d8d3fb785d9721758f880bf12491b4b8a461c94ffc6d82c05beb
- ConservativeSweep:86cf8d9e148032aa7888f6870761bd35bac0a492efe4daa4cb4646c701ab20ca
- TemporalResponse:b93da6b1404aeeb27b6877e893e1ab87686c303fa249bfe252ac9cb438cdc12e

Own candidate2-full.log GREEN4/4, BUILD SUCCESSFUL3m53s: unchanged independent
TranslationProof, existing SatProof and REQUIRED full prepared suite, including
S33 and A9's six translations, plus sentinel. The former exact lane RED is now
green on a new real GameTest origin; no A9 assertion was suppressed. This
proves the required real manifold controls rather than only diagnostic replay.
Candidate2-windows.log GREEN2/2,1m40s: a certified non-linear trajectory is clear
at0/.5/1 yet intersects before midpoint. Direct CCD witnesses early contact;
raw/live response detects it, order reversal is identical, and numeric cap /
deep-penetration checks pass. The intentionally loose velocity bound gives a
conservative response; this synthetic case does not claim a minimum-motion
solution. The fixed existing live one-evaluation failure oracle also passed in
TranslationProof, exporting no partial displacement/contact on exhaustion.

Endpoint-only mutation is KILLED: test-only replacement freezes every interval
at its first endpoint, in separate mutant build/cache/run directories.
endpoint-mutant.log:3838 shows COMPLETE/zero displacement/no contacts;3859
fails "No missed hidden contact exported as COMPLETE", BUILD FAILED1m39s and
clean shutdown. Oracle unchanged between candidate GREEN and mutation RED.
Earlier endpoint-mutant-fixture-compile.log is classified own fixture compile
error (reassigned lambda capture), never counted as mutation kill or production
RED. Its test-only capture was repaired before the actual runtime kill.

HOT_MOVE_QUERY review: chronology/horizon/post-contact screening still charges
one shared Budget; max sample caps fall back to exact CCD when ambiguous,
while shared exhaustion remains distinct. SCREEN_DEPTH6 bounds chronology;
covered-window queues are bounded by charged samples and do not reset budgets.
No catalog preparation, reflection, global registry scan or second solver added.
ULP provenance adds one scalar field and one eight-vertex scan per constructed
box, with constant bounds. Local SAT uses temporary relative vectors per axis;
20,000 warmed separation queries sample baseline80,584,752B vs candidate
81,930,816B (+1.67%),184,550,800ns vs173,616,300ns. Earlier allocation samples
vary with JIT optimization; this bounded sample is explained, not promoted to
normative FPS/TPS, complete movement benchmark or heap-soak acceptance.

Deterministic translation-contact-sheet.png was rendered and visually inspected:
fixed local XY view, five material samples, identical body/reference/candidate
inputs. Difference metadata retains world offsets and numeric response/contact
deltas; it is synthetic geometry visualization, not a Minecraft framebuffer.
translation-visual-metadata.json and render_translation.py reproduce the view.

Zero production changes proposed in this completed affected-surface review.
S33 production/oracle fingerprints remain unchanged. Manifest
../s34-adversary-private/candidate2-green-evidence.json pins candidate, tests,
logs, test-only mutant and visual artifacts. Operational verdict: **funciona**.
Ordinary build/server/shared-client regressions and final integration/commit
status must still be reconciled by IMPLEMENTER before declaring S34/S33 closed;
this result claims the affected independent oracles and full prepared lane,
not unexecuted suites or global G5/G9 acceptance.

## IMPLEMENTER post-review yaw regression and root-speed revision (2026-10-04)

The preceding adversarial GREEN is valid for its pinned source, but a subsequent
restoration of A9's original live-spawn yaw found another real RED at a fixed
root yaw of 0.5 degrees near X=29,000,000/Z=-29,000,000. The full ten-piece
manifold exhausted 256 evaluations even though the selected head contact was
physical. This input is retained as a permanent fixed regression, alongside
the two previously fixed yaws and the original variable live yaw. No prior
GREEN is silently extended to this new input.

The root-only yaw interval had an unnecessarily large generic point-speed
bound: it used the piece's full distance from the root origin, including a
vertical offset that does not move under yaw. A tighter bound now uses the
rotation-axis perpendicular radius over all eight downstream fixed-piece
vertices. It also includes root translation and scale-change speed, and falls
back to the original generic bound when any descendant is animated. The
existing safety margin remains. At the border, the 0.5-degree causal head
piece's direct CCD cost fell from 159 to 22 evaluations and the complete
response used 118, without changing the 32-event/256-evaluation limits,
skin, ulp cap, contact order or fail-closed rule. The permanent root-speed
test samples every vertex across 2048 steps with root yaw, translation and
scale drift while requiring the bound to exclude a large fixed vertical
offset.

The cleaned prepared suite with all four yaw cases passed 2/2 in
`../s34-four-yaw-prepared.log`; ordinary `build` passed 464/464 GameTests in
`../s34-final-rootbound-build.log`. Exact hashes, client rerun and the new
independent adversarial verdict are recorded in `docs/VALIDATION.md` once
those runs finish. S34 and S33 remain open until final zero-change review.

### ADVERSARY root-speed holdout RED and IMPLEMENTER correction

The adversary's independent nonidentity-root holdout found that the first
tighter bound was unsafe. A root initially rotated around Z and then rotated
around world X reported a point-speed bound of only 0.117179 while a vertex
moved 5.657297 blocks from t=0 to t=1. Direct CCD returned CLEAR despite a
verified interior overlap (`../s34-adversary-private/rigid-root-bound.log`,
`rigid-root-red-evidence.json`). This is a genuine FR-049 tunneling defect,
not a mere work-budget failure. The old generic bound was not the cause.

The relative quaternion's rotation axis is in world orientation, whereas
the piece vertices used to calculate the perpendicular radius are root-local.
The revised bound pulls the axis back through the initial root rotation before
measuring those local vertices. A permanent ordinary GameTest asserts both
the full-interval vertex secant and the interior-only CCD hit for a nonidentity
initial root orientation. This revised candidate requires a fresh adversarial
rerun; all previous GREEN results remain pinned to their earlier source.

### ADVERSARY newly found ROOT certificate RED (2026-10-04)

Final review revalidated prior solver hashes and IMPLEMENTER prepared/build/
client log hashes, then found a production delta not pinned in the preceding
solver-only review: HierarchyMotion.rigidRootRotationSpeed. The three SAT/CCD/
scheduler hashes remain unchanged, but that new tightening must independently
certify every material vertex under nonidentity root orientation. No earlier
GREEN result is relabelled as covering this new certificate frontier.

Own S34PrivateRigidRootBoundProof constructs one admitted fixed-descendant
piece, local x=[3,4], y/z=[-.05,.05]. Root before is Rz(pi/2), root after is
Rx(pi/2)*before. Both are valid root TRS, no animated descendant, shear or scale
change. This also represents yaw around a side-gravity world axis; it is not
restricted to an identity initial root. It derives a stationary physical body
from the actual midpoint material, proves both endpoints clear and midpoint
penetrating, and checks a necessary bound independent of the solver: maximum
vertex endpoint secant on a unit interval cannot exceed max-point velocity.

Candidate HierarchyMotion SHA256
84addf1e15d8a635d1e05d0f5eb969beb778df9e7161fe0bc1b2eae24c724efe.
rigid-root-bound.log:243 records declared deformationSpeed0.11717900500008777
vs actual same-vertex endpoint secant5.657296515344627. ConservativeSweep
returns CLEAR at evaluation1 despite the certified trajectory's interior
penetration. At264 the necessary speed-bound assertion fails, required RED,
BUILD FAILED1m42s, clean server shutdown. This is NOT budget exhaustion or
fixture precision noise: the certificate underbounds by more than48x.

Classification: production FR-049 continuous-collision soundness failure in
the new ROOT-only velocity certificate; downstream false CLEAR/tunneling.
The point velocity calculation and trajectory must use compatible coordinate
frames; the holdout does not prescribe a repair. Safe budget/skin policies and
all earlier oracles stay intact. No production/IMPLEMENTER test edits made.

Reproduction with JDK25 and own dirs:
`./gradlew.bat runGameTest -PscalebrewsBuildDir=../s34-adversary-build -Ps34PrivateProof=io.github.r3neer.scalebrews.test.S34PrivateRigidRootBoundProof --project-cache-dir ../s34-adversary-cache -I ../s34-adversary-private/server.init.gradle --console=plain`
Manifest ../s34-adversary-private/rigid-root-red-evidence.json pins code, fixture,
selector and actual run. Verdict: **no funciona, arréglalo**. S34 and S33 closure
remain blocked despite the valid prior ordinary/prepared/client successes.

### ADVERSARY ROOT frame-certificate repair GREEN (2026-10-04)

Retested S34PrivateRigidRootBoundProof without changing its source or assertions.
HierarchyMotion now pulls the relative quaternion axis into the same root-local
frame as fixed downstream vertices. Candidate SHA256
b26d21335a35c298fbfdfb2f491fddde9af8e6bd65bb1b1e563c8dffc38cc454.
rigid-root-fixed-campaign.log:155 records declared6.628607538188697 covering
actual vertex endpoint secant5.657296515344627; CCD now CONTACT at
.48318903610580166 in12 evaluations instead of false CLEAR. The original
endpoint-clear/midpoint-penetration prerequisites and necessary speed-bound
assertion are intact. Combined affected lane GREEN6/6,1m28s: this oracle,
unchanged TranslationProof, WindowSafetyProof, SatProof, full prepared suite
with S33/A9 and sentinel. No budget/skin/oracle relaxation.

An additional independent deterministic campaign constructs48 nonidentity root
TRS intervals with varying rotation axes, translation, nonuniform scale,
negative-z scale on16 cases and fixed affine/sheared descendants. Across32
subintervals and8 vertices,12,288 observed vertex secants after subtracting the
declared external linear translation remain within deformationSpeed. Maximum
observed/declared ratio.9110772847717333. Large world offsets are retained.
rigid-root-expanded.log GREEN2/2,1m37s, clean server shutdown. This sampling
supplements rather than proves the full derivative bound mathematically. Its
initial wildcard-import ambiguity is classified own fixture compile failure
in rigid-root-expanded-fixture-compile.log, not production RED; only imports
changed before its first executed run.

Independent review of the bound: fixed descendant maps are composed once; each
root-local vertex's scaled position is affine over the interval. Perpendicular
radius norm is convex and bounded by its endpoint maximum. Angular speed uses
the existing conservative slerp/nlerp chord bound; root translation and scale
velocity bounds add conservatively. A non-fixed descendant, static root or
unusable rotation axis retains the generic bound. The source/root-local axis
must remain compatible, as the original RED demonstrates. Known unsafe-frame
mutation is killed by the unchanged oracle, and the repaired candidate passes;
previous endpoint-only mutation/runtime kill evidence remains valid.

Cost: this HOT_MOVE_QUERY interval-construction tightening adds O(P*H+8P)
fixed-path work, constant corner loops and local temporary values. It stores
one bound per piece and does not recompute it in Motion.at/CCD samples. Static
presentation root paths take the guard without the eight-corner loop. No
catalog compilation, reflection, new global cache/scan or second solver.
The combined SAT sample reports identical40,800,000B for20,000 baseline and
candidate separation calls;85,954,700ns vs79,461,600ns, with the existing JIT /
non-normative performance limits. No unexplained hot-loop work was found.

All production hashes were checked after execution and match the tested
candidate; S33 source and all native-cell manifest fingerprints are intact.
No production or IMPLEMENTER test/section edits by ADVERSARY. Evidence manifest
../s34-adversary-private/rigid-root-fixed-evidence.json includes the immutable
original oracle hash, expanded fixture, candidate code, selectors and logs.
Affected-surface zero-change verdict: **funciona**. Final ordinary/shared-client
runs must refer to this repaired HierarchyMotion hash before unconditional
integration closure; prior suites on the unsafe root candidate are not silently
credited to this repair. G5-wide and normative G9 acceptance remain unclaimed.

### ADVERSARY final cross-sprint zero-change review GREEN (2026-10-04)

Final independent closure pass compares all tested production and oracle hashes,
executed evidence and canonical scope after the ROOT certificate repair. No
production changes or new functional evidence gaps found; no production or
IMPLEMENTER tests/sections changed by this role. The source hashes in
rigid-root-fixed-evidence.json all still match. S33 native-cell production,
fixture, selector and log fingerprints all match its prior independent GREEN.
Relevant shared-core deltas were reviewed against placement routing, atomic
commit, real contact authority, fail-closed limits and continuous motion;
full own prepared lane includes S33 and the repaired A9 controls.

Verified final IMPLEMENTER log SHA256 and actual results on repaired source:
- s34-axis-prepared.log:43ad1a00aaf68617220210ff990a3c2fde08b6521b65616c25b87b000f5dd01a,2/2 GREEN;
- s34-axis-build.log:c82b2888ba7bfecdaa224be347885756283f5593e3d4b968a8b1d6afc3088318,465/465 GREEN;
- s34-axis-client.log:0cca144cba9bb0624a8c009e6199c1a6ecfb64823c3a6759a4194ba5ef87639f,BUILD SUCCESSFUL/clean integrated-world shutdown.

Permanent tests retain their physical assertions. A9 now runs both historical
fixed yaws, small-yaw regression and original live spawn yaw; it aggregates six
translated complete-manifold controls without skipping its live material drain,
physical response, endpoint clearance or anchor checks. The added permanent
nonidentity-root and vertical-offset speed tests complement the independent
original RED-to-GREEN and48-case campaign. Previously executed REDs stay
explicitly classified/historical; none is miscounted as GREEN. Earlier own
window/ULP/order/exhaustion controls, endpoint-only mutation kill and deterministic
visual artifacts remain valid on the unchanged solver hashes.

Performance/code/API review is retained from the completed component passes:
no preparation/reflection leak, second solver, unbounded work/cache or unexplained
cost regression found. The new numeric provenance and ulp factor are explicit
reviewed policy, capped below quarter SKIN; no budget increase or skipped
candidate is hidden by this final verdict. Mainline gameplay/config/input/render/
mount/gravity/inventory/reach surfaces outside the reviewed subsystem are not
removed or restored. git diff --check is clean.

Functional/testing/final-review criteria for S34 repair and S33 placement now
converge: **funciona**. Update canonical administrative status and create separate
logical S34/S33 commits under IMPLEMENTER ownership. This record does not claim
those commits already exist and does not advance later G5/G9 or external
Gravity Changer rotated-box compatibility. Final manifest:
../s34-adversary-private/final-cross-sprint-evidence.json.
