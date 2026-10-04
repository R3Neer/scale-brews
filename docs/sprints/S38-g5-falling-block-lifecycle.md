# S38 / G5.5 — falling-block material lifecycle and anvil impact

Status: closed locally after independent runtime holdouts and complete-tree
zero-change review. Whole G5 remains open.
S37/G5.4 ordinary minecart slice closed locally at `0601fe15`. The
preimplementation model converged before S38 implementation.

## Scope and thesis — IMPLEMENTER

At the end of S38, real sand, concrete-powder and anvil `FallingBlockEntity`
bodies can rest as entities on a prepared canonical material support and follow
one retained ROOT interval. A supported block does not prematurely place,
expire or repeat an impact. On release, Minecraft owns water interaction,
hardening, placement, timeout and fall damage, each once under its native
conditions. Ineligible support/block pairs keep their native physical path.

This is the falling-block slice of FR-007/067/068 within G5, with
FR-009/010/053/055-058/093 and NFR-001/004/007/008/033 neighboring. Items,
boats and minecarts are separate prior slices. Legacy-engine removal, wider
Minecraft coverage and global G9 benchmark/pack QA remain outside this sprint.
Human-only gameplay checks are deferred until the end of the epic as requested.

## State reconstructed — IMPLEMENTER

The current local `main` is `aeca64e1`; `chatgpt-editing` preserves its
unrelated gameplay line. `BodyClassification.category` maps a real
`FallingBlockEntity` to `falling_blocks`, and the shared prepared contact/core
is available. `PlatformFallingBlockMixin` subtracts one from positive `time`
at tick HEAD while `Platforms.supported`, suppresses only the `onGround()`
placement branch during support, and adds the support to anvil fall-damage
targets when appropriate. These hooks also run for READY canonical bodies,
but their current prepared lifecycle effect has not been proved.

Existing `PlatformTests.fallingBlockRemainsEntity` and `anvilImpactOnce` use
a legacy ghast surface/`PlatformState`; they cannot certify the prepared path.
Installed Minecraft 26.2 `FallingBlockEntity.tick` bytecode increments `time`,
applies gravity and real `move`, then checks concrete powder/water before the
grounded placement branch. The water check may independently enter hardening
even when grounded placement is deferred. Its later timeout/placement/drop
branches and `causeFallDamage` require direct native-path evidence, not only
field assertions. No duplicate Scale lifecycle implementation is planned.

## Provisional implementation plan — IMPLEMENTER

- [x] I1 Build an isolated prepared-server proof with real sand, concrete
  powder and anvil entities. Use actual `fall`/`move`/`tick`, exact canonical
  support/piece/revision identity and no `PlatformState` owner. Prove one ROOT
  translation/rotation carry, obstruction without replay and a ratio/policy
  rejection with native interaction.
- [x] I2 Keep sand as an entity during prolonged supported native ticks,
  without advancing finite timeout. Release support and prove vanilla fall,
  single placement/drop decision and timeout on a controlled alternative.
  Separate fixture World/block cleanup from production lifecycle outcomes.
- [x] I3 Exercise concrete powder with real source/flowing water and release;
  distinguish supported hold, water entry, hardening and final block state.
  Exercise anvil's actual impact once and passive ROOT afterwards without
  duplicate damage; release and establish one native later impact if physics
  permits. Repair only a classified missing boundary.
- [x] I4 Run targeted prepared, ordinary and affected client suites, then
  independent ADVERSARY holdouts, locality/performance review and a final
  zero-change pass. Record executed evidence and exact hashes in
  `VALIDATION.md`/G5 and make one separate logical local commit. No push or
  release.

## ADVERSARY preimplementation model

Role: **ADVERSARY**. Independent plan review on `0601fe15`, local main
`aeca64e1`: **funciona, continúa**. No S38 production or runtime acceptance.
I1–I4 converges with FR-007/067/068 and the canonical ownership boundary.

Model: a supported falling block remains the same entity/material state while
one canonical contact transports it. Holding only suspends necessary native
terminal decisions; it must not create another Scale placement, timeout,
water-hardening or damage state machine. Losing the eligible relation returns
those decisions to the actual vanilla entity lifecycle exactly once. Checks
must observe entity survival, world/block mutations, spawned drops and real
damage outcomes, not merely `supported`, `time` or a manually invoked impact.

Installed 26.2 bytecode independently confirms the ordering: tick increments
age, applies gravity, moves, processes block effects/portal, then performs
server water/placement/timeout decisions. Concrete powder checks water at its
current block and has an additional high-speed source-water ray path. The
placement branch is selected by `onGround OR water`, so changing the grounded
expression alone cannot demonstrate supported hold in water. I3 explicitly
covers real source/flowing water and the distinction between holding, release,
hardening and the final block. Native age also has separate outside-build-height
and absolute timeout branches. I2's alternative must exercise the actual
terminal branch and its drop policy, rather than discard a fixture itself.

Anvil damage depends on hurtEntities/fall distance, native eligible target
filtering and the anvil degradation state. The independent model distinguishes
one landing, passive material transport and a later genuine free fall;
unchanged target health during native invulnerability alone cannot prove that
no duplicate damage attempt occurred. Any later-impact claim requires a
surviving anvil and physically valid release. Random degradation must not be
mistaken for a transport or placement defect.

Independent campaign boundaries: canonical-only contact/piece/revision,
retained ROOT translation/rotation and obstructed no-replay, native rejection,
prolonged entity hold, material retirement and each native terminal outcome,
water interaction and impact attribution. Deterministic spatial/temporal
snapshots supplement semantic assertions. Exact holdout inputs, seeds,
orderings and fault checks remain private until a classified reproducible
failure is returned. Fixture cleanup must follow recorded outcomes and keep
native drops or leftover blocks from contaminating the next case.

Cost model: existing contact/age/placement boundaries are HOT_TICK and
HOT_MOVE_QUERY; damage target selection is native impact-time work. Variables
are local candidate count, material pieces, held bodies, support changes and
actual impacts. Audit bounded local capture/solver work, reuse of prepared
geometry, allocations and absence of per-body global scans, hot reflection,
preparation or extra unbounded lifecycle histories. Dynamic work counters
supplement timings; integrated G9 and human pack acceptance remain separate.
Changed shared mixins require the real affected client runtime, as I4 states.

Evidence: `../s38-adversary-falling-bytecode.txt`, SHA-256
`c19f76f6eaa03043aaf7b6bc7a77dc5dc8fa8fd74d92fb1dd93b2d9365adb38d`;
current PlatformFallingBlockMixin, category and canonical FR-067/068/G5 were
inspected independently. `git diff HEAD -- src/main src/client` is empty.
No GameTests or production changes were made in this planning phase.

## IMPLEMENTER candidate evidence and integrated-lane triage

The prepared S38 selector `-PscalebrewsAnatomyPreparedSuite=true
-PscalebrewsS38Only=true` passed **2/2** in `../s38-root-first.log` after
real sand, concrete powder and anvil entities acquired canonical-only
piece/revision contact. The public proof covers one translated and one yawed
ROOT, a real wall without displacement replay, ratio rejection, 650 held sand
ticks, post-release placement and 600→601 native timeout with one drop,
source/flowing-water hold and post-release hardening, and real initial/later
anvil impacts without passive-repeat damage. Source SHA-256:
`2b0d2f92ea97439631078ffac92872a3046a78fb79d90edc29045d03eb6f6803`.
The targeted log SHA-256 is
`b0f84cad4c3a1222ccdb4111115128792de80907c8751869136652ac22c2bce1`.

The first supported concrete source-water test failed before production
repair: the native current-cell water decision bypassed the existing grounded
placement guard and hardened a still-supported powder entity. The only
production change suspends that native water predicate while current material
contact exists. Once the support retires, the same native path resumes and
hardens. The mixin SHA-256 is
`04f372826979c01d0e83ff29f6177634463e7b3bb6a219d2c7fc5abe6a186b0c`.
The installed 26.2 bytecode was used to identify the two water paths and the
native item cadence described below; no alternate placement/drop owner was
added.

Ordinary `build` passed **465/465** in `../s38-build-first.log`, SHA-256
`709a225852a684bcb403151d882cd0467194ef48a5ad073d5979aba7be2b2e63`.
The complete `runClientGameTest` passed `BUILD SUCCESSFUL` in
`../s38-client-first.log`, SHA-256
`612dd06bbb55a649d871e1ecf68d30180cbc33aad7f0498e39b96e4d4fb731b2`.
Those initial results preceded the final prepared-harness/test-fixture edits.
On the final candidate tree, ordinary `build` again passed **465/465** in
`../s38-build-final.log`, SHA-256
`b432dd538c07b3215d48a0e81d77670846bcd66284ac8c3cb6d08ce0597e0346`;
the complete client suite again passed in `../s38-client-final.log`, SHA-256
`0d0dec9b67db802c89f18c5fc2e39967d88f1bc64e2c03e2de8f4b39aaef686e`.

The full prepared composition initially found several test-infrastructure
issues. S35 manually called `ItemEntity.tick()` without
advancing `tickCount`; installed 26.2 bytecode defers movement for a quiet
grounded item unless `(tickCount + id) % 4 == 0`, so the old one-tick position
assertion depended on a random entity id. The fixture now advances the clock
and verifies gravity/age/contact immediately, then fall within four native
cadence ticks; the S35-only rerun passed **2/2**. S36 placed a wall at the next
integer X but requested only 0.6 blocks of travel; a 0.811-block free gap
reproduced a false failure. It now crosses the gap and the S36-only rerun
passed **2/2**. The original composite harness ran every scenario in one
GameTest tick, exhausted the intentionally bounded physics work by S37, and
quarantined conservatively. ADVERSARY independently isolated and patched its
test-only scheduler; the accepted harness gives each of 21 scenarios a
distinct real level tick while keeping one prepared catalog lifetime. A prior
tick's metrics sample also had to be normalized to zero at each new scenario
baseline. Chest boats/carts are emptied only after their inventory
assertions so native discard does not spill stone items into the next
scenario's material envelope.

The final integrated prepared sequence in
`../s38-prepared-scheduled-third.log` passed **2/2** with scenario indices
0–20 on consecutive distinct level ticks and completion on the next tick;
SHA-256 `08fbc81962a1463b30fd7d9557ef97f153bb19ec2588321efbd880301544132d`.
These earlier REDs remain preserved as fixture/integration records. No
production work budget, lifecycle owner or solver tolerance changed to make
the composition pass. The later ADVERSARY composite-scheduling section below
records the earlier private RED before the cross-tick metric baseline and
fixture repairs. The accepted scheduler and final integrated GREEN above
supersede that interim result; the historical RED remains for audit.

The independent final complete-tree review found no remaining source defect
and made zero changes. Its unchanged integrated rerun passed **2/2** with all
21 scenarios on distinct ticks in
`../s38-adversary-private/zero-review-composite.log`, SHA-256
`abf6a23558bec89fb2d3e6f94bb77063be65454657f8808aad436aa0ff5a0a35`.
Evidence manifest `../s38-adversary-private/zero-review-evidence.json`,
SHA-256 `a570f057461828862fcf602b5a335aaf6dddc1cdbb7274bb504913d79e386262`.

## ADVERSARY independent runtime review

**Funciona.** Final review of candidate mixin SHA-256
`04f372826979c01d0e83ff29f6177634463e7b3bb6a219d2c7fc5abe6a186b0c`
found no remaining reproducible S38 defect. The only production diff is the
nine-line water-decision suspension in PlatformFallingBlockMixin. The
ADVERSARY did not edit production or the IMPLEMENTER's tests, and did not read
the S38 proof source to design its holdouts. Tests/tooling remain in separate
private Java roots and build/cache/run directories.

Independent executed lanes, each **2/2 GREEN** including framework sentinel:

- `../s38-adversary-private/impact.log`, 2m33. Actual falling anvil lands on a
  prepared cow with one native damage call (10); 80 resting ticks and 16 real
  ROOT intervals introduce no extra attempt. Counter instrumentation delegates
  to vanilla hurtServer and counts attempts even when invulnerability would
  conceal repeated damage in health. Retiring that cow produces a real free
  fall to a second prepared cow, one new native damage call (16), fresh contact
  and 30 subsequent ticks without repeat. No manual causeFallDamage call.
- `water-final-snapshot.log`, 3m42. Actual current-cell water levels 0 and 5
  hold real concrete powder at age 598 through ROOT and native ticks. Changing
  support scale invalidates ratio; the same next native tick hardens red
  concrete at age 599, retires the entity and emits no item. Each fluid case
  also makes 10,000 supported queries with zero extra geometry/joint evaluations.
- `timeout.log`, 2m23. A real sand entity's initial native age becomes 1,
  remains 1 for 650 ticks, and also holds at the exact age-600 boundary.
  Actual support retirement resumes age to 601 and executes native timeout:
  dropItem=true emits exactly one sand unit; false emits none. No placed sand
  is fabricated by timeout. Cleanup follows counted native outcomes.

The full ordinary client suite was executed in independent client outputs.
The first run failed after 7m43 at
S29ImplementerAcceptedCatalogClientProof:61, a reload predicate timeout during
concurrent test JVM activity. It is preserved in `ordinary-client.log`;
no production or oracle was changed. The unchanged complete repetition,
`ordinary-client-repeat.log`, passed **BUILD SUCCESSFUL 4m41** with clean
Minecraft exit. The earlier timeout did not reproduce; load is a possible
fixture condition, not a proven cause or a silently deleted RED. The isolated
S38 mixin does not participate in that cow-only catalog transaction.

IMPLEMENTER logs were reviewed as separate executed evidence: targeted
`../s38-root-first.log` **2/2**, ordinary `../s38-build-first.log` **465/465**,
and `../s38-client-first.log` client GREEN. They are not misattributed to
adversarial execution; no claim is made that private tests alone replace the
prepared wall/rejection/placement scenarios recorded there.

Mutation/fault evidence: a test-only source overlay outside the repository
replaces the new mixin with its prior implementation, compiled into its own
mutation build/run. Class signatures confirm the candidate water hook is
absent only there. The unchanged held-water oracle fails at `Current water
cannot harden or advance held timeout`, **BUILD FAILED 1m57** in
`water-mutation-current.log`. This is the expected killed mutation, not a RED
of the current candidate. No production source was swapped. Earlier private
fixture compilation used removed Blocks.RED_* constants; replacing them with
actual 26.2 registry IDs repaired infrastructure only. The initial mutation
init needed a newline between Groovy blocks; that evaluation failure also
remains a fixture record.

Cost review: changed runtime is HOT_TICK with at most two additional current
support queries at the two native water-tag checks; false-water short circuits
without query. These checks are server-branch decisions, not client geometry
preparation. Existing bounded/local contact and solver ownership remains;
there is no new world scan, reflection, lifecycle owner, cache/history or
observer fanout in production. Independent structural reuse counts cover
20,000 held-water queries. Counters and runtime bounds provide signal; noisy
multi-JVM timings are not a G9/FPS/TPS acceptance benchmark.

The deterministic `root-water-contact-sheet.png` was rendered and visually
inspected against actual entity AABBs and the translated material reference in
`root-water-snapshots.json`: both measured AABB/reference errors are zero.
It uses a fixed XY scale and explicitly labels the current water-cell boundary
rather than inventing a fluid surface. Semantic survival/placement/drop
assertions accompany it; it is not framebuffer or human pack acceptance.

Reproduce private server lanes from repository root with Java 25:
`./gradlew.bat runGameTest -PscalebrewsBuildDir=../s38-adversary-build
--project-cache-dir ../s38-adversary-cache
-I ../s38-adversary-private/server.init.gradle` defaults to impact. Select
`-Ps38PrivateProof=io.github.r3neer.scalebrews.test.S38PrivateWaterRetirementProof`
or `S38PrivateTimeoutProof` (same package) for other lanes. The full client
uses runClientGameTest with `../s38-adversary-client-build`, separate cache and
`../s38-adversary-private/client.init.gradle`. Production hash remained
unchanged throughout final tests; diff-check is green. Future holdouts remain
private. This verdict concerns the S38 falling-block slice only; whole G5,
experimental minecarts, legacy retirement, G9 and human acceptance remain open.

Manifest: `../s38-adversary-private/final-evidence.json`, SHA-256
410256f9ac076df7aebea8439b443551b41aaec620a6fd168557777429a46413.

### ADVERSARY composite scheduling repair (not yet accepted)

The parent requested a test-only repair after the synchronous prepared wrapper
exhausted/quarantined S37 in the composite lane. Private overlay
`../s38-adversary-private/composite-java/.../AnatomyPreparedIntegrationProof.java`
preserves one prepared session and schedules every scenario on a later real
level tick, asserting monotonic gameTime. Session closes at final success and
on callback failure; Only selector precedence remains unchanged. No production
limits, physics or active S35/S36/S37/S38 source was edited.

`composite-scheduling.patch` applies cleanly with git apply --check. Compile
passes after adapting the callback interface for the existing IOException
helper. The complete selector remains RED in
`../s38-adversary-private/composite-scheduling-current.log`: scenario indices
0/1/2 run at gameTime 76403/76404/76405; S36 line81 then fails even though ROOT
displacement exactly equals (.2,.1,0) and final metrics are admitted1,
quarantined0, exhausted0. Its admitted-count comparison reads the prior tick's
metrics before the first current-tick event; runtime record resets counters on
that first event. Classified as fixture cross-tick baseline error, returned to
the owner of the active fixture. This is not a production movement failure and
not a green composite verdict. Later scenarios remain blocked by that assertion.

Reproduce: runGameTest -PscalebrewsAnatomyPreparedSuite=true
-PscalebrewsBuildDir=../s38-adversary-composite-build
--project-cache-dir ../s38-adversary-composite-cache
-I ../s38-adversary-private/composite.init.gradle. Current production falling
block SHA-256 remains 04f372826979c01d0e83ff29f6177634463e7b3bb6a219d2c7fc5abe6a186b0c.
