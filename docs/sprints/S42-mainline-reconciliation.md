# S42 — reconcile current mainline with the canonical collision branch

Status: closure verification complete; the associated local merge commit carries `Sprint: S42`.
Active role for this section: **IMPLEMENTER**. S41 closed at `fe6691ab`;
the explicit per-sprint local-commit policy is saved at `c49cf145`.

## Scope and thesis — IMPLEMENTER

At the end of S42, `chatgpt-editing` contains current `main@b5e62535` gameplay
and its regression tests while retaining the canonical collision API, runtime
and already certified ownership boundaries. A merge must not restore the old
anatomy implementation or create another effective-gravity authority. This
integration boundary is necessary before G5 removes legacy code: cleanup must
operate around the current product rather than an obsolete mount baseline.

FR-050/053 and NFR-025 constrain shared gravity and ownership; FR-001..004,
FR-009/010/055..058/093 and NFR-007..014/027 constrain neighboring collision
behavior and cost. The mainline mount, input, inventory, render, reach, gait
and optional Alchemical Leather behavior must survive independently of
collision READY. This sprint does not close the remaining G5 equivalence or
legacy-removal tasks, advance G6-G9, publish, install or replace human QA.
Human-only acceptance remains deferred to the end of the epic in canonical G9.
The lifecycle defect revealed during verification also invokes FR-002,
FR-082 and NFR-017: a retired connection cannot invalidate a newer live session.

## Initial state and investigation — IMPLEMENTER

The branch is clean at `c49cf145`, ahead 14 of its tracking branch. A fresh
fetch still resolves main to `b5e62535`; the merge base is
`5a8bf915d22d52cf4e79a68e2506d199bb6e848e`. A non-working-tree `merge-tree`
preview reports 28 conflicted paths, including documentation, client wiring,
mount rendering, shared gravity, mount schema and test manifests. Main has
independently evolved tiny-mount families/body equipment/menus/dispenser,
native wolf gestures and pounce feedback, renderer world-state membership and
bounded rotation history, camera/first-person seat transforms, scale gait and
step sounds, reach behavior and optional Alchemical Leather data. These are
current product behavior, not collision debt.

S11 already reconciled the shared provider in `integration.gravity.GravityFrames`
and explicitly retained `collision.api.GravityFrame` as the only physical
frame representation. Main's later integration frame has the same six cardinal
basis transforms but adds vector overloads and local-vertical conveniences;
its gameplay consumers must migrate to the canonical representation without
changing their math or reinterpreting existing world momentum. Keep the current
provider installation, optional initialization-time MethodHandle linkage,
development overrides and observation generations. No second frame DTO or
second provider should survive this integration.

Main changes to old `platform.anatomy` tests/client imports predate the canonical
package migration. They must be reconciled semantically rather than restored
as production or treated as permission to remove the current collision tests.
Main's `ImpactSurfaces.isLoaded` API adjustment must coexist with S39's material
impact guard. `PlatformCamera` must retain the causal body residual and combine
it with the current mainline tiny-mount camera contribution exactly once.

## Implementation plan — IMPLEMENTER

- [x] I1 Merge the pinned main commit without committing, retaining all unrelated
  mainline features/resources and the branch's role/workflow/commit policy.
  Resolve each conflict by subsystem ownership. Preserve canonical collision
  wiring, tests and all build proof selectors while adding mainline test and
  mixin entrypoints exactly once. Inspect automatically merged paths as well.
  Keep main's deprecation CI gate: replace deprecated connected-player calls
  with the current helper and its test-only connection completion, preserving
  each fixture's game mode, player membership and packet paths.
  The gate must still reject deprecated Minecraft/external APIs and all
  production warnings. Existing test-only collision compatibility wrappers
  may be reported through a finite exact diagnostic allowlist, without hiding
  diagnostics or permitting unreported warnings at the compiler output cap.
- [x] I2 Reconcile gravity through the existing single provider and canonical
  `collision.api.GravityFrame`: migrate new mount/test imports and direction
  accessors; add equivalent coordinate overloads and vertical operations there.
  Keep one six-cardinal transform implementation. Adapt tests to the shared
  development seam only if provider-owner ordering makes the new main test
  provider incompatible; do not weaken the owner exclusivity assertions.
  All six-frame gameplay cases must execute even if S11 already installed a
  different test owner; a conditional early-success cannot certify them.
- [x] I3 Reconcile renderer/client initialization and data/schema consumers,
  preserving mainline world-frame membership, native menus, equipment and
  pounce presentation as well as canonical geometry preparation and causal
  presentation. Audit the complete main-relative and branch-relative diff,
  including docs/resources/tests, and classify any failures before repairs.
  Preserve the optional FirstPerson extraction bridge while moving its one-time
  API linkage to explicit client initialization. Compile public accessors to
  typed MethodHandles without initializing the external classes early; sample
  rendering/core/logic/offset once during extraction with constant work and no
  metadata discovery or Method.invoke/Field.get in the render hot path. Keep
  absent-mod, null-core, ZERO-offset and incompatible-API fail-closed behavior.
- [x] I4 Run build and the complete ordinary server/client suites with the new
  main tests enabled, prepared original-model composition, and affected real
  vehicle/control proofs. Audit hot-path scaling, allocations, caches and
  retention. Then obtain independent holdouts, mutation/differential snapshots
  where material, and a complete zero-change review. Record only executed
  evidence, update the canonical plan and close with one local merge commit
  identifying `Sprint: S42`; verify HEAD and working tree before advancing.
- [x] I5 Repair the independently reproduced retired-connection lifecycle
  defect before final I4 acceptance. Marshal JOIN/DISCONNECT state transitions
  to the Minecraft client executor, where the session/material maps are owned.
  Remember the actual joined ClientPacketListener by object identity. At
  execution, reject a JOIN whose handler is no longer the current client
  connection and reject a DISCONNECT whose handler no longer owns the session.
  A genuine current disconnect must still clear its catalog and material; a
  normal join must still reset the prior session. Clear the owner on genuine
  disconnect without retaining an unbounded retired-handler collection.
  Preserve existing level/dimension/reconnect replay fences and packet formats.
  Verify the real delayed callback holdout, positive current-disconnect and
  same-authority reconnect/dimension controls, a targeted ownership mutation,
  complete client regression and the final build. No fixture oracle may be
  weakened to treat catalog rollback as acceptable.

## Acceptance and plan review — IMPLEMENTER

Compilation alone cannot prove seat/camera composition, native mount inventory
or server/client ownership. Acceptance requires the new mainline regression
entrypoints and the current canonical suites to execute, not merely coexist in
a manifest. Any discovered conflict that changes the thesis or a public contract
returns to planning before implementation continues. Reflection stays at
initialization; the optional FirstPerson API is sampled during extraction through
prepared accessors, outside collision physics and without reflective metadata
discovery. No new registry scan or history growth is permitted in a render
or movement hot loop. A merge conflict is an integration issue, not evidence
that a main feature may be deleted.

The IMPLEMENTER reviewed the plan against canonical requirements, S11's single
frame contract, current main diff/consumers, the merge preview and regression
surfaces. The final complete plan pass produced zero changes. The final update
explicitly preserved main's deprecation gate and required non-vacuous six-frame
gameplay execution. Independent ADVERSARY preimplementation convergence is
recorded below.

## Preimplementation model — ADVERSARY

Active role: **ADVERSARY**. Independently reviewed the updated plan against
the canonical requirements, architecture, gate status, workflow and performance
protocol, and the actual mainline sources at `b5e62535` relative to merge base
`5a8bf915d22d52cf4e79a68e2506d199bb6e848e`. The collision branch remains at
`c49cf145`; this review changes only this section, not production or fixtures.

The independent model treats reconciliation as preservation of two sets of
contracts, including automatically merged files. A compile-green merge can
still erase a mainline behavior, silently skip its tests, resurrect a retired
anatomy backend, or invalidate a certified causal relationship. Acceptance
therefore compares both parent snapshots and the resulting manifest/resource
and runtime behavior, rather than treating either parent as universally correct.

- Gravity has one effective provider and one physical frame representation.
  Main's six-cardinal basis agrees with the canonical basis; coordinate and
  vertical conveniences must retain that equivalence, owner exclusivity,
  initialization-only linkage, explicit DOWN overrides and monotonic observed
  generations. Changing gravity affects subsequently generated local movement;
  it does not reinterpret pre-existing world momentum. Provider installation
  order must not turn bee flight or wolf pounce cases into early-success tests.
- Mainline mounting, input, equipment, menus, dispenser interaction, wolf
  feedback, reach and gait remain functional with collision disabled, binding
  and ready. Fixture migration must retain genuine server-player membership,
  game mode and native packet routes; connection completion belongs only to
  test infrastructure. Optional-mod data must remain valid without claiming
  an external compatibility execution that has not happened.
- Renderer composition preserves main's world-state membership, frame-scoped
  seat exchange, rotation-history lifecycle and first-person applied camera
  translation together with canonical causal geometry and residual barriers.
  Separate green mount and collision suites do not by themselves certify their
  composition: a mounted actor transported by material needs exactly one net
  contribution across partial frames, mounting changes and world/frame resets.
  Deterministic reference/candidate snapshots supplement semantic assertions;
  screenshots alone do not prove physical ownership or causal consumption.
- Union of the two parents' applicable test entrypoints, selectors and mixins
  must execute without duplicates or silently excluded cases. Legacy imports
  require semantic migration; historical green logs certify their historical
  snapshots only. Deprecated-player migration cannot substitute a disconnected
  mock for a fixture that previously exercised networking.
- Cost review covers HOT_TICK, HOT_MOVE_QUERY, HOT_NETWORK and HOT_PRESENTATION
  affected by the merge, separately from PREPARATION and OFFLINE_TOOLING.
  Scaling variables include visible render states, mounts/riders, supports,
  bodies, pieces, observers, retained history and cache invalidations. The
  existing topological ordering already uses linear list membership inside its
  loop; it is a baseline cost to compare, not a newly discovered merge defect.
  Added world-state/rotation storage must retire absent entities and old worlds,
  and shared gravity or model preparation must not duplicate hot work. Compare
  structural work and retention where relevant without increasing budgets.

Preimplementation verdict: **funciona, continúa**. The updated I1/I2 expressly
cover the native connected-player seam and non-vacuous six-frame execution;
I3/I4 cover preservation, composition and independent verification. A complete
second plan pass found no remaining contradiction requiring a plan change.
Holdouts remain independent and private. This verdict authorizes implementation
of the converged plan; it does not certify a merged candidate, external renderer
compatibility, human QA, G5 legacy removal or any later gate. No runtime suite
was run for this preimplementation review.

## Implementation and executed verification — IMPLEMENTER

The merge is applied without a commit and all 28 conflicts are resolved.
Main-owned render, mount, equipment and data surfaces are retained; canonical
collision wiring and causal camera residual remain. The initial merge's only
production change inside canonical collision packages was the shared frame's
main-equivalent
coordinate/vertical conveniences and direct cardinal inverse. The global
provider and observation ledger are unchanged. New main six-frame gameplay
fixtures use the existing development overrides and have no provider-order
early-success branch. The old connected fixture's constant CREATIVE accessor
was confirmed in installed 26.2 bytecode and is preserved through `TestPlayers`.

The first ordinary run compiled but revealed the disconnected-player fixture
conflict below. It also exposed an integration-plan omission: main's blanket
warning grep rejects intentional, already-deprecated collision fixture adapters
from S18, even though production emitted no deprecation warning. The updated I1
retains the native API guard and requires an exact test-only diagnostic allowlist,
all other diagnostics fatal and no compiler-cap omissions. This adjustment
does not change collision/gameplay contracts or authorize deleting those
fixtures. The full IMPLEMENTER plan pass after this adjustment produced no
further changes; independent confirmation is required before altering the CI
diagnostic guard.

The independent cost review found another plan contradiction: current main's
optional FirstPerson bridge lazily discovers reflective accessors during the
first render extraction and uses Method.invoke/Field.get in subsequent extractions.
Preserving the product behavior is mandatory, but the blanket initialization-only
claim was false. I3 now names that bridge's ownership and separates preparation
from constant-cost API sampling using prepared typed accessors. This is a scoped
integration repair, not removal of main behavior or proof that the absent-mod
base lane certifies a real FirstPerson installation. The full IMPLEMENTER plan
review converged after the I3 adjustment; independent reconvergence approved
that preparation/sampling boundary before the production edit.

The implemented bridge links the four public accessors to typed MethodHandles
at explicit client initialization, with `Class.forName(..., false, loader)`.
Extraction performs bounded calls through those prepared handles. Its real
optional dependency was checked against the pinned VP26 FirstPerson 2.7.2 JAR
(`firstperson-fabric-2.7.2-mc26.2.jar`, 1,600,772 bytes, SHA-512
`a1822273015aeb291bdc6ceeb2787e2734a9bde1f20abbea613b1088ad34fed84a9a37953efc842650ad597d557df868d4b5142502fcb648121a25fa83a3350b`).
`javap` confirms the actual boolean, static core, logic-handler and Vec3 APIs.
The artifact is loaded only in the isolated test checkout through `fabric.addMods`.

Executed candidate evidence so far: complete ordinary build/server suite
**483/483** (`../s42-ordinary-final-candidate.log`, 2m32s); prepared original-model
composition **2/2**, 23 scenarios (`../s42-prepared-candidate.log`, 1m55s);
the real compilation's CI audit accepts exactly **387** intentional test-only
adapter diagnostics (`../s42-ci-real-final-candidate.log`). The latter rejects
native/external, production, unparsed and compiler-cap-omitted diagnostics.
These server/prepared runs predate the subsequent FirstPerson client edit;
final affected-source verification is still required.

After that edit, TinyMountClientAcceptance with the actual FirstPerson 2.7.2
artifact passes and exits cleanly (`../s42-firstperson-client-candidate.log`,
3m55s). Actual mount-frame captures include first-person extraction, native
menus, client-to-server equipment interaction and a native horse control.
IMPLEMENTER inspected the captured chicken first-person, wolf equipment-menu
and bee mounted-front screenshots in `../s42-impl-run/screenshots/`; their
purpose is visual regression evidence, not final human pack acceptance.
The complete ordinary client suite remains RED in S29 replay/composition;
isolated green S29 runs do not close that failure. ADVERSARY is reducing the
sequence with the original assertions intact before attribution or repair.

Final affected-source focal checks now pass: prepared original-model
composition **2/2** with 23 scenarios in
`../s42-prepared-final-catalog-candidate.log` (2m08s, SHA-256
`b214769104fec7d08896cc308cc0048ad99d8700606ac0ebba67369228b91dab`),
and actual experimental minecart/native interpolation/control in
`../s42-experimental-client-final-candidate.log` (2m22s, SHA-256
`d6a2a6348741b987bed8e27e8e6c09a51700b6602692809d32deefef4ba8e71f`).
Both invocations pass `-PscalebrewsAnatomyCatalog=../s36-impl-run/anatomy-export`.
The real FirstPerson focal log's SHA-256 is
`5403fa8eb403273684b32607ce6f85a759e35d894691c46aa03aee6334767140`;
bee/chicken/wolf capture respectively 124/130/113 first-person frames.

The renewed parent/source audit (`../s42-current-review.json`, SHA-256
`bbad102c0f53afdee7cb00c45ff874f524182c7cc5fad30daf2dfeb73423da84`)
checks all 69 main-changed product/resource/tool/workflow paths and verifies
both parents' test-entrypoint subsets. There are 102 server and 25 client
entrypoints without duplicates. Of 112 canonical collision production files,
111 remain byte-equivalent after line-ending normalization; only GravityFrame
has the planned transform conveniences. The 13 reconciled main-relative paths
have explicit collision-wiring, single-gravity, causal-camera, FirstPerson
preparation or CI-diagnostic ownership; none restores retired anatomy.

IMPLEMENTER cost review classifies shared cardinal transforms as HOT_TICK /
HOT_MOVE_QUERY, optional linkage as PREPARATION, its four bounded samples and
mount/camera composition as HOT_PRESENTATION, and CI diagnostics as
OFFLINE_TOOLING. The cardinal inverse replaces multiple basis vectors/dots
with one switch and at most one vector; vanilla vector overloads reuse their
immutable input. The existing provider/observation ledger is unchanged.
FirstPerson does not perform metadata discovery or reflective invocation during
extraction and retains no per-frame history. Main's WORLD_STATES/SEATS retire
each frame and rotation history removes absent mounts and clears on world/reload;
storage scales with the current visible mounts/states. Its topological list
membership cost already exists in both parent implementations and is unchanged
by reconciliation. Camera retains one causal residual and one mount contribution
before its fixed five clip samples. No physics budget, registry scan, observer
fan-out or cache bound increases in this merge. These structural observations
do not substitute for G9's integrated benchmark.

The independent real-event holdout subsequently made the earlier semantic
failure class deterministic: one old DISCONNECT callback on a Netty thread
resets a new connection's accepted revision 2; a revision-1 replay on that same
current connection is then accepted. This is a production NFR-017 violation,
not a fixture expectation failure. It does not prove the exact chronology of
the earlier uninstrumented RED. The former candidate greens and source ledger
therefore predate the required I5 repair and do not certify its final state.
I5 changes only connection-transition authority/thread confinement, retaining
the canonical session, material cleanup and replay semantics. The revised
IMPLEMENTER plan was reviewed completely against actual Fabric callback types,
current client/server lifecycle code and the canonical causal-barrier contract;
that plan pass produced no further changes. Independent reconvergence is
recorded by ADVERSARY and preceded the production edit.

The I5 repair confines JOIN/DISCONNECT cleanup to `Minecraft.execute` and
records the joined listener by object identity. The JOIN task checks the
current client listener; the DISCONNECT task checks the session owner at
execution and clears that owner only for the genuine current disconnect.
No packet shape, replay-fence policy or public collision API changes. The
installed Fabric callback signatures and 26.2 executor bytecode were verified;
the executor runs inline on its owning thread and schedules foreign-thread
events. The revised source ledger is `../s42-i5-current-review.json` (SHA-256
`d7eaedc258db106e93a29bcbbc57bfab955f5fcbc5f0b80be2f19d92244d6aba`):
110/112 canonical files remain equivalent to S41, with only GravityFrame and
this connection transition changed. It does not overwrite the earlier ledger.

The extra lifecycle state is one live handler reference, cleared on the active
disconnect; it has no retired-handler collection. Each callback schedules at
most one constant-size task and performs constant-time identity checks before
the existing bounded cleanup. It adds no per-tick/per-packet scan, lock or
metadata lookup. Verification of the repaired final snapshot remains pending.

The repaired public candidate now passes ordinary `build runGameTest`
**483/483** with clean exit (`../s42-i5-ordinary-full.log`, 3m39s, SHA-256
`fe8b7975a49e143f401a3e65e5cea1dadd40665cdcefaa2b2c12b0481584c2db`)
and all 25 client entrypoints with clean exit (`../s42-i5-client-full.log`,
6m04s, SHA-256
`022bf554a41546c1e1d6b561c16ccb4268d51618a649133316c5998c4e81787d`).
The original accepted-reload, active A-B-A and early replay assertions all pass.
The produced beta.9 JAR's SHA-256 is
`b2dafaa5e2a1f9785a6959e60506c4ef91707d9bf833ee5bce3b02bfd0ad8b26`;
metadata version is correct and none of the 354 GameTest source classes or
their nested classes leaks into production, across any package. Independent
positive lifecycle controls, mutation and final durable regression remain
pending before I4 can close.

Independent I5 plan reconvergence — ADVERSARY: **PASS, implementation may
continue; no closure claim**. Reviewed the new transition contract against
FR-002/082 and NFR-017, the unchanged real-event RED, actual callback threads
and current session ownership. Executor confinement and identity checked when
the transition executes address the reproduced defect rather than its timing.
The model requires both directions: obsolete JOIN/DISCONNECT must leave a
new owner untouched; genuine current disconnect must retire all material and
a subsequent normal join must establish a fresh session. Queued callbacks,
level replacement and same-authority reconnect must preserve their distinct
barriers without accumulating retired identities. The delayed-event oracle
and its catalog monotonicity assertion remain unchanged. The planned bounded
owner slot adds constant work per connection event, not per render/physics
query. The complete independent plan pass found no further contradiction;
production and regression evidence remain pending I5 implementation.
## Adversarial verification — ADVERSARY

Independent verification is in progress; **S42 remains open**. Own build/cache
and run directories are outside the source checkout under `../s42-adversary-*`.
Production was not edited by ADVERSARY.

Executed green evidence:

- `../s42-adversary-ordinary-first.log`: 483/483 ordinary server GameTests,
  BUILD SUCCESSFUL 2m14. The disconnected S25 receipt-publication oracle was
  correctly red after the general mock helper gained a network login. Its
  test-only repair constructs an unregistered vanilla ServerPlayer, preserves
  `connection == null`, and retains the original authoritative receipt assertion.
- `../s42-adversary-composition-first.log`: six-frame gravity projection,
  vertical scaling and matrix equivalence; conjugated world-space seat target;
  rejection of a same-ID GUI/non-world render state; end-frame retirement; and
  rotation retention across 80 batches of 48 replaced mounts. The unchanged
  private S41 oracle also passed four ROOT phases for two actual experimental
  carts, local occupancy, native interpolation and exactly applied server carry.
  BUILD SUCCESSFUL 2m18 with clean exit.
- `../s42-adversary-catalog-first.log`: unchanged S29 catalog/dimension semantic
  assertions passed in isolation. An independent tiny-mount Chicken variant
  then exercised wire-confirmed local carry through the existing S31 observer:
  physical ROOT .2, renderer/camera residual .1 at half interpolation, and no
  duplicate passenger correction. BUILD SUCCESSFUL 1m32 with clean exit.
- `../s42-adversary-sequence-first.log`: JOINT, rejected catalog, accepted catalog
  and S29 catalog/dimension all passed sequentially, BUILD SUCCESSFUL 3m34.
  `../s42-adversary-prefix-second.log` also passed the complete public prefix
  through S29, BUILD SUCCESSFUL 4m02. These passes do **not** invalidate the
  semantic replay red observed in the earlier full client suite.
- `../s42-adversary-firstperson-first.log`: local instrumented API shim confirms
  deferred external class initialization, inactive-render/null-core behavior,
  valid ZERO offset, exactly one accessor sample per active extraction over
  1000 calls, and permanent fail-closed behavior after an incompatible state.
  The repaired production bridge and unmutated seat/retention oracle both pass,
  BUILD SUCCESSFUL 3m07. This is an API-contract fixture, not an execution of the
  external FirstPerson mod. Its public signatures were compared with the local
  real 2.7.2 javap record.

Private mutation evidence: removing only `riderTransform`'s world-state
membership guard in an external source overlay produces the expected semantic
red, `same id outside world frame must not inherit attachment`, in
`../s42-adversary-membership-mutation-second.log`. The first mutation invocation
had incomplete API-shim fixture dependencies and failed compilation; that was
infrastructure, not a killed semantic mutant. No repository production source
was swapped for either run.

Deterministic spatial evidence is in `../s42-adversary-private/seat-snapshot.json`
and `seat-contact-sheet.png`, generated by `render_seat_snapshot.py` and visually
inspected. Frame 1, seed 0, yaw 1.1 and nonuniform scale (.5,.3,.7) compare the
world reference (5,7.2,9) with actual transformed client coordinates
(5.000001,7.2000003,9), approximately 1e-6 error. This numeric capture supplements
the spatial oracle; it is not a Minecraft framebuffer or external-renderer
human acceptance.

CI probes in `../s42-adversary-private/ci-probes.json` independently accept only
the enumerated fixture diagnostic and reject production, a later native API
warning, a compiler-cap omission, a malformed diagnostic and an unlisted
adapter, with Windows path normalization exercised. Applying the first guard
to the real uncapped compiler log exposed missing fixture entries and the
native S39 diagnostic getter. Those were returned to IMPLEMENTER, not hidden
by a blanket warning exemption. Private fixtures outside `src/gametest/java`
are intentionally outside this production CI allowlist.

Remaining classification: the first public prefix independently reproduced a
timeout at S29's initial await (`../s42-adversary-prefix-first.log`, 3m43).
IMPLEMENTER's full client run separately produced a semantic stale-replay red.
S29's observable assertions remain unchanged; additional failure messages now
print catalog revision/epoch/certificates and initial pose/presentation/physics
state. The twelve-session unchanged S29 oracle completed successfully in
`../s42-adversary-catalog-soak-first.log` (3m23). A traced seventeen-entrypoint
prefix also passed (`../s42-adversary-prefix-trace-first.log`, 5m51), while
showing actual catalog acceptance on the Render thread and actual DISCONNECT
reset callbacks on Netty threads. Neither pass waives the semantic red.

A deterministic lifecycle holdout now reproduces the semantic failure class:
`../s42-adversary-retired-first.log`, BUILD FAILED 1m29, NFR-017. The fixture
retains exactly one real old connection's Fabric DISCONNECT callback on its
original Netty thread. A new real connection accepts revision 1, then reloads
and accepts revision 2. Releasing the retired callback resets that live
catalog to revision -1; replaying revision 1 over the unchanged current
connection accepts it again. The final assertion reports `accepted=2 actual=1`.
No disconnect event is fabricated or repeated, and current connection identity
is asserted unchanged. The trace records accepted revision 2 at line 321,
retired callback reset at lines 322–328, empty live state at line 329 and
acceptance of stale revision 1 at lines 330–331. This proves a production
lifecycle defect, independently reproducing the earlier semantic failure class;
it does not reconstruct the uninstrumented historical run's exact chronology.

Reproduction (JDK 25): `./gradlew runClientGameTest --init-script
../s42-adversary-private/retired.init.gradle
'-PscalebrewsBuildDir=../s42-adversary-build' --project-cache-dir
../s42-adversary-cache --console=plain`. The test and scheduling gate are
external adversarial fixtures; repository production remains untouched.
Neither red is waived by the green isolated or composed repetitions.

I5 repaired-candidate verification — ADVERSARY:

- Candidate `AnatomyClientNetworking` SHA256
  `d043bf47f6fa66e1e012f2a293dcf7395a8ef17d9ba3c5bea998d74c9cec2eb2`.
  The unchanged original real-event oracle passes in
  `../s42-adversary-retired-postfix.log`, BUILD SUCCESSFUL 2m16, clean exit;
  the retired callback leaves the live catalog ready at revision 2.
- Omitting only the identity guard in an external source overlay kills that
  candidate with the same semantic assertion, `accepted=2 actual=1`, in
  `../s42-adversary-owner-mutant-second.log` (expected BUILD FAILED 2m26).
  Compilation succeeded; no repository production source was replaced.
- Reusable repository regressions `S42AdversarialRetiredDisconnectProof` and
  `S42AdversarialCurrentDisconnectProof`, plus two client-only GameTest mixins,
  execute in `../s42-adversary-public-lifecycle-final-third.log`: BUILD SUCCESSFUL
  1m08, clean exit. The gate matches the actual Fabric listener object, retains
  one real callback on its original Netty thread, and invokes it exactly once.
  The positive current callback retires its owner and catalog. The unchanged
  S29 accepted-catalog/dimension oracle subsequently passes, including reload,
  stale replay and both dimension round trips. No external catalog path is
  required by the two new regressions; the default client manifest includes
  them. They are absent from production resources and production mixin lists.
- Reset traces in the repaired private run and the successful first positive
  disconnect show Render-thread execution; the source confines both transitions
  to the client executor and checks identity there. Ownership is one slot,
  cleared at genuine disconnect, with no retired-identity history or additional
  render/physics work. Packet formats and level/material barriers are unchanged.

Infrastructure classifications retained: the first new positive fixture had
an absent import (compile-only failure); a `world.close()` loop did not observe
its second callback within the fixture bound; an explicit native disconnect
left Fabric's world context before its mandatory close. The final positive
control uses the same actual-callback gate and releases it while that session
still owns its state. The first exact-listener gate incorrectly shadowed an
inherited Fabric field; an accessor now targets its declaring superclass,
verified against installed 26.2/Fabric bytecode. These are fixture faults,
not production passes or newly attributed production defects. Both resource
EOF issues were corrected and scoped `git diff --check` passes.

The complete independent I5 source/infrastructure review now produces zero
further changes. The deterministic lifecycle defect is repaired, with a
positive control and a killed ownership mutant. Earlier uninstrumented REDs
remain historical observations, not exact-chronology claims; their semantic
failure class is covered by the retained deterministic regression. Final
repository-wide regression and the S42 closure commit remain IMPLEMENTER's
required closure steps.
Cost review distinguishes preparation from hot extraction/presentation. The
mainline list-membership topological ordering remains a pre-existing baseline
cost; current-state identity storage and rotation retirement are exercised
above. Canonical causal runtime, inboxes, physical caches and shared gravity
provider/observation code remain unchanged by this merge. The optional bridge's
metadata discovery was a genuine preparation boundary violation and returned
to planning before repair. No whole-pack heap/TPS/FPS claim, G5 removal closure,
external renderer equivalence or human QA follows from these measurements.

## Failures, final review and closure

- Environment/command failure: the unquoted PowerShell Gradle property arrived
  empty and failed build evaluation before compilation. Quoting property
  arguments fixed that invocation; no source change addressed this failure.
- Environment/command failure: the first final isolated prepared and S41 focal
  repetitions omitted their mandatory original-catalog property. Both fixtures
  rejected the invocation before the material scenarios. The corrected
  commands above pass on unchanged production and expectations.
- Test-run configuration failure: requesting server and client together with
  IMPLEMENTER's temporary init script makes both use the same run-directory
  resources. Gradle rejects the undeclared shared `downloadAssets` output after
  the build and 483 server tests have passed. Server and client are therefore
  invoked separately; this does not change repository build wiring or fixtures.
- Test infrastructure conflict: the newly merged connected-player mixin also
  connects the intentionally disconnected S25 receipt-publication fixture.
  The oracle requires no listener and remains valid. Returned to ADVERSARY
  fixture/infrastructure review before changing its observable expectation.
- Integration-plan/CI conflict: expected test-only collision adapter diagnostics
  are rejected by the new blanket mainline warning grep. Returned to planning
  with the narrow I1 adjustment above; no production warning is exempted.
- Integration-plan/cost conflict: main's optional FirstPerson reflection path
  contradicts the initialization-only acceptance claim. Returned to planning
  with I3's explicit preparation/sampling boundary before modifying the bridge.
- Production lifecycle defect (FR-002/082, NFR-017): a retired real DISCONNECT
  callback runs on a Netty thread and clears the newer client session. Returned
  to planning with I5 before repair; earlier green repetitions do not waive it.

Final closure verification — IMPLEMENTER (2026-10-05):

- `../s42-final-server.log`: build plus **483/483** ordinary server tests,
  clean exit in 2m31s, SHA-256
  `9a5a79b5926060a70e7f7bffe593e1f6359d0ca16fadf0ac480696b8c0b8ced3`.
  The uncapped incremental compilation reports 147 intentional test-only
  adapter diagnostics and passes the exact CI guard; it supplements the
  earlier full 387-diagnostic compilation audit rather than replacing it.
- `../s42-final-client.log`: complete default **27-entrypoint** client suite,
  clean exit in 5m53s, SHA-256
  `c896863a68cdfb58a2b27d217c1cfbd47ae75d5fd0c72bc71a3d20273642e231`.
  The processed manifest exactly equals the source manifest without duplicates.
  Actual retained-event output keeps the new connection ready at revision 2;
  the positive current-disconnect regression then passes with owner/catalog
  cleanup. Original S29 reload, replay and dimension assertions remain intact.
- `../s42-final-experimental-client.log`: actual `NewMinecartBehavior`,
  material ROOT, rail release/reacquisition, native interpolation and ratio/
  policy/obstruction controls pass on the final networking source, with clean
  exit in 1m08s, SHA-256
  `f5ed37afd333fc7f7bb32c52a2d25c3475c981800e39215a8ed613dd4c0b1c54`.
- `../s42-final-source-review.json`, SHA-256
  `a211ed8be38146e27d7bba162caaf17dd79dbd220f0b3b1166fad16f28c2b8e0`,
  retains all 69 main-relative paths and both parents' entrypoint subsets.
  Exactly 110/112 canonical production files are unchanged after line-ending
  normalization; the two planned changes are GravityFrame and the I5 listener
  barrier. A fresh fetch still resolves main to the pinned merge parent.
- `../s42-final-evidence.json`, SHA-256
  `c57a03fd862125c504dc387ea9ed9b6cdf11a702bd3b12599d0ad5f4c5ee1c7d`,
  records the final suite manifests/log hashes and unchanged beta.9 JAR digest
  `b2dafaa5e2a1f9785a6959e60506c4ef91707d9bf833ee5bce3b02bfd0ad8b26`.
  Correct version metadata and all **358** GameTest source-class paths and their
  nested classes were checked across all packages: zero production leaks.

IMPLEMENTER inspected the revealed seat contact sheet and metadata; its world
reference/candidate overlap agrees with the numeric oracle. The final complete
source, parent preservation, API/ownership, cost, tests, resources and evidence
pass required no further production or fixture changes. ADVERSARY's independent
zero-change pass and killed ownership/membership mutants are recorded above.
The two durable lifecycle tests and their client-only mixins are retained as
reusable regressions; one-off runners, overlays, captures and logs remain outside
the repository, and generated Python cache files were removed from the tree.
Canonical architecture, plan and VALIDATION are updated. `git diff --check`
passes. The local merge commit includes both roles' distinct changes and
`Sprint: S42`; verify its two parents and clean working tree before advancing.
Broad G5 equivalence/removal and G6-G9 remain open; human QA stays deferred
to the end of the epic, with no publication or installation performed.
