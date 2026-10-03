# S29 — independent adversarial recertification, 2026-10-02

Active role: **ADVERSARY**. This is an execution record for the current repair
batch, not a replacement for the canonical requirements, architecture or roadmap.
No production code was changed by this role. Final ADVERSARY verdict: G4.5 is ready for canonical closure on the verified candidate; G4.6 was not started. Earlier RED/open entries below are chronological evidence, superseded by the final verdict.

## Scope and independent model

The first open gate at baseline `a54031b9` was G4.5. The existing S29 model was
read after requirements/architecture/workflow. The branch already contained the
first certified-interval candidate despite the historical capability-RED record.

The independent invariants for this batch were: exact server-issued material
identity; no inference from endpoint adjacency; complete ROOT/JOINT/DERIVED_CARRY
publication; order-independent certificate placement around accepted endpoints;
non-resurrection across expiration and material barriers; bounded state and local
lookup; and reuse of authority joints across physical/current/historical queries.

Attack families considered include boundaries, duplicate/replay, missing identity,
independent lifecycle axes, available/unavailable recovery, causal contributions
inside the same tick, certificate/endpoint permutations, pathological capacity,
metamorphic root interpolation and directed cost mutations. Concrete holdouts were
not communicated before their execution produced a defect or ready evidence.

## Reproducible defects and classification

| Finding | Observable | Classification | Independent revalidation |
| --- | --- | --- | --- |
| Expired certificate replay | Receive at client tick 0, expire at 101, replay: `accepted=true, authoritative=true` | Implementation, NFR-011 | Repaired: unchanged probe prints both false |
| Intermediate live root endpoints omitted | Two genuine `Cow.move` calls in one server action produced certificates 2→3 / 3→4 that could not materialize | Implementation, FR-086 | Repaired: both certificates materialize in the unchanged client proof |
| Material revival after unavailable/recovery | Old certificate alone revived its pre-barrier interval after fresh current recovery | Implementation, FR-086 / lifecycle | Repaired: unchanged client proof prints authoritative=false |
| Missing derived-carry certificate | Parent carried a prepared support exactly .2X; child endpoint advanced once but its derived material interval had no certificate | Implementation, FR-086 / causal completeness | Repaired: unchanged real client proof passed, derived child certificate materialized |
| Repeated historical/current query churn | 32 alternations over A/B/C caused 96 extra joint evaluations after warm-up | Implementation, NFR-009 | First repair removed repeat churn; initial historical reuse remained RED |
| First historical query duplicates physical evaluation | Physical samples A/B/C were already evaluated; first presentation A→B evaluated two of those authority samples again | Implementation, NFR-009 | Repaired: final independent server lane passed both holdouts with zero duplicate evaluations |
| Historical endpoint fixture stopped its tracking owner | `AnatomyRuntime.stop` preceded implicit-generation `sendPose`, so no packet was sent | Test fixture, not production | ADVERSARY captured generation before stop and used explicit-generation sends; client proof passed |

The original failing logs were retained separately under the chat's `work/`:
`s29-unavailable-red.log`, `s29-derived-red.log`, `s29-presentation-work-red.log`
and `s29-first-historical-work-red.log`. Test failures were classified before the
implementer changed production. The adversary never repaired production.

## Executed independent evidence

- `s29_adversarial_inbox_probe.py`: baseline RED; unchanged after repair PASS.
- `s29_adversarial_inbox_campaign.py`: PASS for duplicate/TTL inclusive boundary,
  expired replay, independent binding/tracking rollback, epoch/dimension mismatch,
  exact lookup, per-support overflow and recovery, global support cap and locality.
  100 exact queries with 4096 unrelated supports prune exactly 100 support states.
- `S29AdversarialLiveEndpointProof`: repaired client PASS, Gradle exit 0 in 55s;
  certificates material serials 2/3 and endpoint pairs 2→3/3→4 both materialized.
- `AnatomyPresentationFrameTests`: repaired fixture client PASS, exit 0 in 45s.
- `S29AdversarialUnavailableReplayProof`: repaired client PASS, exit 0 in 45s,
  old-certificate replay remained non-authoritative after recovery.
- `S29AdversarialCertificateIdentityProof`: client PASS, exit 0 in 51s. Certificate
  before/between/after endpoints converged; no certificate meant no interval;
  fraction 0/.25/.5/.75/1 reproduced the endpoint root trajectory, invalid fractions
  failed closed, identical queries reused their frame, and epoch/revision/dimension/
  network-ID/support UUID/binding/tracking mismatches did not materialize.
- `S29AdversarialDerivedPresentationProof`: unchanged repaired client PASS, exit 0
  in 36s; exact child delta and serial advance passed; the child certificate existed
  and its interval materialized (`s29-derived-green.log`).
- `S29AdversarialPresentationWorkTests`: the first repaired run passed repeated
  alternation but first-historical reuse remained RED with duplicateEvaluations=2.
  The final shared-memo repair passed the independent lane: **3/3 required tests**
  including Fabric sentinel, exit 0 in 39s; additionalJointEvaluations=0 and
  duplicateEvaluations=0 (`s29-adversarial-work-final-green.log`).
- Final affected client battery, after the shared-memo repair: all **five** proof
  classes above ran together in one real client process and fresh worlds; exit 0,
  **BUILD SUCCESSFUL in 49s**. No source modification was needed. Evidence:
  `work/s29-adversarial-final-five-client-green.log`. The final kernel campaign
  and all eight mutants were repeated: `work/s29-inbox-final-campaign.log`.
  Exact hashes of the six affected production files were captured before that
  battery in `work/s29-adversarial-final-source-hashes.json` and checked unchanged
  after it. This pins the evidence to the tested working tree, not merely baseline HEAD.
  Log SHA-256: client `9333c53453d3610fb215794956e6346c6c4bf9c5ba31d28049c6d22a0c12aa9c`,
  work lane `b148a011aa7f84acc687f934eb3010e5245a324b648c162f27264cf418a58e13`,
  inbox campaign `9936fb91deaf1fbb74bc293d249f4f6b6fc0d6bbb11024899ba1d08ffc293558`.

Directed scratch-source mutation campaign: **eight compilable mutants killed**:
expiry fence, per-support bound, global-query scan, binding rollback fence,
tracking rollback fence, epoch fence, dimension fence and nearest/any-serial lookup.
The mutated production inbox was compiled in a temporary directory; the repository's
production source was never modified. The identity-only payload fixture removes
Fabric transport dependencies; this campaign is kernel evidence, not wire evidence.
The real-client identity proof supplies the corresponding receiver evidence.

## Performance second read

Production surfaces are HOT_NETWORK, HOT_TICK and HOT_PRESENTATION. Relevant
variables are S supports, O recipients, H retained endpoints/certificates/joint
samples, P model parts and query count/fraction changes.

Exact inbox lookup now visits one support and at most 128 certificates rather
than pruning every support for every query. Tick pruning remains O(S) with bounded
amortized expired-entry removal. Endpoints retain a cap of 128 and a client-clock
TTL of 100. Certificate replay watermarks remain bounded by 4096 supports;
saturation is fail-closed. Server certificate publication reuses immutable endpoints
and existing pose DTOs; there is no model traversal/reflection/catalog rebuild in
the new hot path. Test reflection is confined to the adversarial fixture.

The final repair uses one shared memo of at most 128 exact inputs per live evaluator
entity, reused by physical and presentation paths; the physical current/previous
slots remain two references to those values. Current and interval drawable frames
have separate slots. Both observed duplicate-evaluation regressions are repaired
and independently revalidated. Lock ordering is joint-cache then retained-cache;
the retained path does not call back into the joint-cache. Entity keys are weak,
and client evaluator ownership is released at support/session barriers. No new
preparation/reflection/catalog traversal enters the loop. Allocation/GC and integrated fan-out/soak timing
were not measured by this batch. No NFR-014/G9 budget claim follows from these probes.

## Tooling and reproduction

Reusable tooling remains under `tools/` and test sources. Temporary javac fixtures
and mutants are automatically deleted after each campaign. The isolated Gradle init
is `tools/s29_adversarial.init.gradle`; selectors affect only generated test metadata.
Always provide a distinct build directory per active lane. By default its run directory
is beneath that build directory; `-Ps29AdversarialRunDir=...` can override it.

```powershell
python tools/s29_adversarial_inbox_probe.py
python tools/s29_adversarial_inbox_campaign.py
foreach ($mutation in @('expiry','bound','global-query','binding','tracking','epoch','dimension','nearest')) {
    python tools/s29_adversarial_inbox_campaign.py --mutation $mutation
}
```

Client lane (replace selector with the required proof class):

```powershell
.\gradlew.bat runClientGameTest `
  '-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S29AdversarialCertificateIdentityProof' `
  -PscalebrewsBuildDir=C:/Users/Usuario/Documents/Codex/2026-10-02/vas-x20/work/s29-adversary-build `
  -Ps29AdversarialRunDir=C:/Users/Usuario/Documents/Codex/2026-10-02/vas-x20/work/s29-adversary-run `
  --project-cache-dir C:/Users/Usuario/Documents/Codex/2026-10-02/vas-x20/work/s29-adversary-cache `
  -I tools/s29_adversarial.init.gradle --console=plain
```

For the work-counter lane use `runGameTest` and
`'-Ps29AdversarialServerProof=io.github.r3neer.scalebrews.test.S29AdversarialPresentationWorkTests'`.

## Global S29 closure gaps

The initial repair batch did not close G4.5 or supply frontend mutation adequacy.
The extension below now supplies the minimum frontend mutation-family evidence;
the initial kernel mutations alone never stood in for it. The complete lifecycle
matrix and contact acceptance remain bounded by the exercised paths. Dedicated/late-tracking/reconnect,
wire revision barriers, longer memory/GC/fan-out evidence and final zero-change
review of the complete S29 milestone remain distinct acceptance work.

Snapshots were considered: this batch's demonstrated defects concern protocol
identity, missing certificates, lifecycle replay and CPU work, each directly observed
by deterministic machine assertions. Root interpolation is checked at fixed fractions.
No renderer/residual/camera integration is added in this batch; visual alignment and
animated/contact snapshot acceptance remains part of the full S29/G4.6 progression.
These protocol assertions are not a claim that visual acceptance has been executed.

## Final pass of this repair batch

After the shared-memo repair, the adversary re-read all seven affected production
surfaces: inbox, exact frame retention, server interval publication, derived event
publication, client receiver/lifecycle handling, drawable cache ownership and joint
memoization. Rechecked complete identity comparisons, expiration/caps, material
barriers, source of root/pose authority, scheduling neutrality, no extra carry,
protocol compatibility, map ownership, lock order, locality and unrelated scope.
`git diff --check` passed. The final inbox campaign remained green. No further
production change was requested by this pass, and no observed defect in this batch
remains unresolved. This is a zero-change review of the **repair batch**, not a
declaration that the outstanding full S29 acceptance/mutation matrix is complete.

## Extension: lifecycle ordering and compiled frontend sensitivity

The next coherent frontier exercises receiver lifecycle changes while a certificate
is already staged before its first endpoint. `S29AdversarialLifecycleOrderingProof`
passed in a real integrated client (BUILD SUCCESSFUL, 3m25s). The unchanged lifecycle,
binding-only restart, tracking-only restart and simultaneous restart all preserve
their matching early certificate once both exact endpoints arrive. Before either
endpoint exists no interval materializes. Replaying the prior generation's poses
and certificate neither revives retired material nor removes the accepted interval.
A missing before endpoint, followed by its late/stale arrival, remains fail-closed.
Evidence: `../s29-lifecycle-ordering-green.log`.

The material-barrier oracle now has a separate `-Ps29GravityRoundTrip=true` mode:
DOWN -> WEST -> DOWN, with binding/tracking unchanged. Its real-client baseline
passed (BUILD SUCCESSFUL, 1m34s), reporting old-certificate replay authoritative=false.
The original UNAVAILABLE case remains the default; this option adds a distinct
barrier instead of replacing the previously executed case.
Evidence: `../s29-gravity-roundtrip-green.log`.

`tools/s29_adversarial_frontend_mutation.py` prepares copied sources strictly outside
the repository. The test-only Gradle init excludes only the original compilation
unit and includes the copied unit in its corresponding main/client source set.
Production files are never rewritten. A compiled client binding mutant was killed
by `Mismatched certificate axis 5 became authoritative` (1m21s), proving the original
identity oracle detects the removed comparison. Compile/configuration errors are
not counted as kills. `tools/s29_adversarial_frontend_campaign.ps1` checks nonzero
runtime results against specific oracle assertions and preserves complete logs.
The first campaign invocation stopped because Windows PowerShell treated normal
javac stderr notes as ErrorRecords; the harness was corrected and rerun. That
interrupted invocation is not evidence of a killed mutant.

### Independent replay-fence locality check

After IMPLEMENTER replaced global tombstone iteration with per-UUID scopes,
`python tools/s29_adversarial_replay_fence_probe.py` passed. With 4096 scopes
retained, the queried UUID has three dimensions and lookup visits exactly those
three tombstones; an unknown UUID adds zero visits. The same probe verifies per-
dimension removal, reclaimed capacity, exact/older replay rejection, forward
same-window progress, conservative saturation and explicit reset. Instrumentation
is one observational counter in a disposable source copy. Identifier/payload value
objects are stubbed, so this proves the fence kernel rather than wire integration.
Evidence: `../s29-replay-fence-locality-green.log`.

### Compiled frontend mutants executed

The following copied compilation units compiled and reached the real-client oracle.
Every row is a runtime assertion, not a compile/configuration failure:

| Mutant | Observed assertion | Evidence |
| --- | --- | --- |
| Infer a certificate from adjacent retained serials | Missing serial silently selected another interval | `../s29-front-inference.log` |
| Ignore certificate binding | Mismatched certificate axis 5 became authoritative | `../s29-front-binding.log` |
| Ignore certificate tracking | Mismatched certificate axis 6 became authoritative | `../s29-front-tracking.log` |
| Remove epoch fences from copied client unit | Mismatched certificate axis 0 became authoritative | `../s29-front-epoch.log` |
| Remove dimension fences from copied client unit | Mismatched certificate axis 2 became authoritative | `../s29-front-dimension.log` |
| Exact inbox lookup chooses its first retained certificate | Certified interval changed material endpoints or root trajectory at 0.0 | `../s29-front-nearest.log` |
| Retain endpoints through material barriers | Gravity roundtrip old-certificate replay authoritative=true; pre-UNAVAILABLE material interval revived assertion | `../s29-front-material-barrier.log` |
| Remove the per-support certificate cap | Overflow inbox retained authoritative interval 0 | `../s29-front-bound.log` |
| Missing target certificate falls back to another live support's certified interval | Another support's certified interval became authoritative for target | `../s29-front-cross-support.log` |

The nearest mutant failed before reaching the missing-serial assertion: the oracle
already detected wrong endpoints/trajectory for an explicitly named serial. The
campaign's expected-message list initially omitted that earlier valid assertion;
the adversary inspected the runtime failure and extended the recognized pattern.
No timeout or unrelated exception was counted as a kill. Epoch/dimension variants
remove both certificate matching and receiver rejection comparisons in the copied
client unit; identical comparisons elsewhere in that unit are also affected, but
the failing inputs are isolated interval certificates with all other axes unchanged.

The extension executed nine compiled frontend mutants covering all eight minimum
S29 mutation families (epoch and dimension have separate variants). This completes
the minimum mutation-family sensitivity evidence, not the full G4.5 acceptance
matrix. The current `presentationContact` implementation calls `presentationFrame`
(CURRENT_ENDPOINT), so this extension does not claim it consumes or coherently
selects a certified interval. Full lifecycle/dedicated/reconnect and memory/GC/
fan-out acceptance remain distinct from this coherent receiver frontier.

### Positive wire bounds and joint baseline of this extension

`S29AdversarialWireBoundsProof` exercises the production receiver over real packets:
all 128 exact certificates at the declared per-support boundary materialize; the
129th clears/saturates the scope and none of the 129 intervals remains drawable.
A same-window replay cannot reopen it. A higher tracking generation, with its
certificate arriving before both poses, recovers the new interval without reviving
the old saturated material. The fixture deliberately constructs trusted-server
payloads to stress the receiver; this is not a claim the server normally emits 128
different material serials for one pair of endpoints.

The combined real-client baseline selected LifecycleOrderingProof, WireBoundsProof,
CertificateIdentityProof and UnavailableReplayProof in gravity-roundtrip mode.
All four passed on original production sources (BUILD SUCCESSFUL, 3m10s), with
clean client/server shutdown. Evidence: `../s29-lifecycle-frontend-baseline-green.log`.
Earlier default UNAVAILABLE evidence is preserved separately. Original source
hashes for client networking, inbox, endpoint history and replay fence were checked
unchanged during this extension; snapshot: `../s29-frontend-source-hashes.json`.

The compiled cap mutant reached the wire oracle, passed the within-bound positive
assertion, then died at `Overflow inbox retained authoritative interval 0` (2m42s).
This adds frontend evidence to the earlier isolated kernel bound kill. It does not
measure heap/GC under long-duration or multiplayer fan-out workloads.

### Two-support authority and serial partitioning

`S29AdversarialCrossSupportProof` bootstraps two actual tracked supports, publishes
exact pose endpoints for both, and first sends a valid certificate only for the
neighbor. That neighbor materializes, while the target's query stays empty. After
the target receives its own certificate with the **same** material serial, both
queries preserve distinct support UUIDs and root geometry. The original-source
client baseline passed (BUILD SUCCESSFUL, 2m38s), with clean shutdown.
Evidence: `../s29-cross-support-green.log`.

The compiled cross-support variant falls back to a neighboring live support's
valid exact certificate when the target has none. It died at `Another support's
certified interval became authoritative for target` (2m54s). This exercises actual
valid foreign authority, rather than only an invented UUID rejected at receive.
Evidence: `../s29-front-cross-support.log`.

The runnable frontend campaign is:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/s29_adversarial_frontend_campaign.ps1
```

It uses the adversary's own build/cache/run directories beneath the repository's
parent directory. Do not run it concurrently with another task using those same
directories. A single row can be replayed with `-Mutations binding`, `bound`, etc.
Run the positive original-source client selectors separately before counting a
mutant failure. The two-support oracle's selector is
`io.github.r3neer.scalebrews.test.S29AdversarialCrossSupportProof`.

### Zero-change review and handoff of this receiver frontier

Re-read certificate receiver/session gates, exact support selection, per-endpoint
matching, RESTART staged-state preservation, retired replay handling, material
barriers and bounded lookup after the last run. The extension found no new
production defect requiring a repair. Source hashes remained unchanged and
`git diff --check` passed. The final copied mutant was removed from compilation
by omitting mutation properties; original main/client/gametest compilation then
passed (BUILD SUCCESSFUL, 20s). No production file was edited by ADVERSARY.

Evidence-file SHA-256 inventory is `../s29-frontend-evidence-hashes.json`.
This closes the tested **receiver identity/bounds/mutation frontier**, with no
observed production failure left unresolved in it. It does not close G4.5:
full dedicated/reconnect/catalog/dimension lifecycle transitions, longer heap/GC/
fan-out evidence, applicable visual/contact snapshots and the final zero-change
review of the entire S29 acceptance matrix remain open. G4.6 was not advanced.

## Extension: persistent dedicated server reconnect

The next coherent frontier is a real connection barrier while the server/support
remain alive. `S29AdversarialDedicatedReconnectProof` uses Fabric's dedicated server
context (`Starting Minecraft server on *:25565`), closes and reopens the client
connection, and force-loads the support's chunk so entity loss is not confused with
recipient lifecycle. Its first certificate is captured from a real `Cow.move` ROOT
event and positively materialized, not invented by the test.

The normal reconnect path passed (BUILD SUCCESSFUL, 6m54s): disconnect drops the
announced catalog, pose history and certificate inbox; reconnect keeps the running
server epoch/revision and physical support but acquires a strictly higher recipient
tracking generation. Replaying the first connection's exact endpoints and certificate
does not materialize that old interval. A new real ROOT move then materializes a
certificate for the new generation. Evidence: `../s29-dedicated-reconnect-green.log`.
This is a dedicated implementation and actual socket connection within the test
environment; it does not claim a remote-machine fan-out/GC benchmark.

The first compilation attempt exposed a fixture-only access error: calling package-
private `WorldAnatomyCatalog.preparedPackets` from package `test`. The fixture now
captures that immutable bundle by test-only reflection, without widening production
access or constructing replacement catalog authority. The corrected fixture compiled
and reached the complete normal reconnect assertions above.

### Early replay before a fresh recipient window: production failure

The optional `-Ps29EarlyReconnectReplay=true` lane freezes the dedicated server
clock after capturing and positively displaying the first connection's real ROOT
interval. It disconnects, stops anatomical publication in the gap, reconnects to
the same persistent support, and delivers the captured catalog bundle, exact poses
and certificate before any new anatomical recipient window. The fixture checks
that the captured authority clock remains within the 100-tick retention limit;
it does not rewrite payload clocks or fabricate a replacement certificate.

On the catalog-rollback-fixed candidate this reached
`S29 dedicated early reconnect old material authoritative=true` and failed at
`NFR-017: pre-disconnect material revived before new tracking window`
(BUILD FAILED, 6m31s). Evidence: `../s29-dedicated-early-reconnect-red.log`.
Classified as a production connection-lifecycle replay defect (NFR-017/FR-080),
reported to IMPLEMENTER. Its repair remains pending independent revalidation.
The normal reconnect green above does not cover this ordering boundary.

Two earlier dedicated launches did not reach an oracle: the installed Fabric
startup helper's 10-second timeout expired, then a test-only timeout hook conflicted
with the existing timeout hook. Evidence is kept as
`../s29-dedicated-reconnect-startup-inconclusive.log` and
`../s29-dedicated-startup-mixin-inconclusive.log`; neither counts as a production
failure. The adversarial init now selects one 60-second test-only startup hook
only for this dedicated selector, in generated resources. Production and the
ordinary source mixin configuration are untouched.

### Independent catalog rollback and repaired normal reconnect

`S29AdversarialCatalogRollbackProof` first obtains a real ROOT certificate and
checks physical authority. A two-fragment candidate for the next revision has an
honest SHA-256 digest but invalid JSON content, reaching the decode/rejection
phase independently of IMPLEMENTER's invalid-digest oracle. During BINDING both
the accepted interval and physical query must be absent. After rejection, the old
epoch/revision and exact certificate endpoints must return together with physical
authority. These assertions passed on the repaired candidate.

The initial run reached all green assertions but blocked during Fabric cleanup.
Its render thread waited in `IntegratedServer.halt` while the server waited in
`ThreadingImpl.enterPhase`; evidence is
`../s29-adversarial-catalog-rollback-client.log` and
`../s29-adversarial-catalog-cleanup-threads.txt`. ADVERSARY terminated only that
identified client process and added two fixture cleanup ticks before `world.close`.
This run is assertion evidence, not a successful completed Gradle lane.

The subsequent combined rollback + normal dedicated reconnect lane passed with
clean shutdown (BUILD SUCCESSFUL, 2m32s), evidence
`../s29-adversarial-catalog-normal-reconnect.log`. Reconnect preserved the running
catalog/support, rejected old material after fresh tracking, and displayed the
new generation's real ROOT interval. No production source was changed by ADVERSARY.

The isolated replay-fence oracle was expanded after the repair and passed:
quiet retirement permits same-window progress before a barrier; explicit closure
rejects it even after a binding-only change; fresh tracking is permitted; quiet
retirement cannot erase a closed window; accepting one dimension preserves another
dimension's barrier; catalog clear releases them. Existing locality, budget and
saturation checks also remain green. Evidence:
`../s29-adversarial-replay-closed-window-green.log`.
This isolated oracle uses production fence logic with DTO/Identifier stubs and
an observational lookup counter; it is distinct from the actual reconnect lane.

### NFR-017 repair independently revalidated

The original early-reconnect assertions were rerun unchanged on the repaired
candidate and passed: `S29 dedicated early reconnect old material authoritative=false`,
BUILD SUCCESSFUL, with two actual connections to the persistent dedicated server
and clean shutdown. Evidence: `../s29-adversarial-early-reconnect-green.log`.
The log spans a large host-clock discontinuity between startup messages; its
reported 5h4m46s wall duration is not a performance measurement. The normal lane
above separately proves forward progress with fresh tracking and a new ROOT event.
NFR-017 is therefore no longer an unresolved defect in this tested ordering scope.

Reproduce either client selector with the adversarial init and separate directories:

```powershell
.\gradlew.bat runClientGameTest '-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S29AdversarialDedicatedReconnectProof' -Ps29EarlyReconnectReplay=true -PscalebrewsBuildDir=C:/Users/Usuario/Documents/Codex/2026-10-02/vas-x20/work/s29-adversary-build -Ps29AdversarialRunDir=C:/Users/Usuario/Documents/Codex/2026-10-02/vas-x20/work/s29-adversary-run --project-cache-dir C:/Users/Usuario/Documents/Codex/2026-10-02/vas-x20/work/s29-adversary-cache -I tools/s29_adversarial.init.gradle --console=plain
```

Omit `-Ps29EarlyReconnectReplay=true` for the normal path. The combined green lane
selects `S29AdversarialCatalogRollbackProof,S29AdversarialDedicatedReconnectProof`
with their full package names, without early-replay mode. Do not overlap jobs in
these directories.

Final zero-change review covered physical provider restoration after BINDING,
catalog-scoped fence retention/reset, explicit level/connection retirement, quiet
TTL retirement, exact pose/certificate replay gates, fresh-window recovery, and
bounded fence locality. Client networking and fence source hashes remained
unchanged through the final replay run; inventory:
`../s29-adversarial-catalog-reconnect-source-hashes.json`. `git diff --check` passed.
ADVERSARY changed only its own fixtures, probe, generated-resource init and record.

This closes the tested catalog rejection and persistent dedicated reconnect
frontier. It does not close G4.5: the full accepted-catalog reload/rollback matrix,
actual dimension round trips, broader connection/catalog permutations, applicable
visual/contact evidence, sustained heap/GC/fan-out measurements and the final
entire-sprint zero-change review still require their own evidence. G4.6 is untouched.

## Extension: accepted catalog and dimension boundaries

Independent model: accepted catalog replacement must remove old revision material
without losing forward physical/presentation authority. A connection-scoped catalog
must survive an actual A-B-A dimension trip while level-scoped endpoints/certificates
cannot survive it. Replay rejection must hold before a fresh recipient pose, not
only after a newer pose supersedes the old tracking generation. Positive ROOT
certification before and after barriers is required to avoid vacuous fail-closed
success. G4.6 remains excluded.

`S29AdversarialCatalogDimensionProof` exercises the transactional production reload
entrypoint with a test ResourceManager supplying a valid replacement model/binding,
then real player teleports Overworld-Nether-Overworld. A second trip freezes only
the authority clock and stops anatomical publication to isolate early replay;
captured payload clocks/generations remain unchanged. Assertions and executed
results for this extension will be recorded after its lane completes.

Fixture classification before counting results: the first replacement model used
an invented source ID. The nominal quadruped pose engine only evaluates its known
vanilla source identities; production correctly published UNAVAILABLE for it.
The reload catalog itself reached revision 2/READY, while the published endpoint
had UNAVAILABLE and no client pose/frame. Evidence:
`../s29-adversarial-catalog-dimension-diagnostic.log`. This timeout is not a
production defect. The fixture now uses an admitted `minecraft:pig` source for
its controlled quadruped geometry/binding replacement. A transient diagnostic
compile error from a package-private ownership accessor was also removed entirely;
no production visibility was changed.

### Executed accepted-catalog / A-B-A result

Final lane: `../s29-adversarial-catalog-dimension-scoped.log`, BUILD SUCCESSFUL
1m6s, with clean integrated-client/server shutdown. Its three PASS markers prove:

- Transactional accepted reload advances revision within the same server epoch,
  replaces the executable model binding, restores physical registration, rejects
  the captured old catalog bundle and old certified material, and displays a new
  real ROOT interval under the new revision.
- Real active Overworld-Nether-Overworld transition preserves the catalog, removes
  the original level's pose/certificate material, rejects its replay after return,
  advances recipient tracking, and displays a new real ROOT interval.
- A second real round trip, with anatomical publication stopped and the authority
  clock frozen, rejects exact pre-barrier endpoints/certificate before a fresh
  tracking pose can hide a replay defect. The original clock remains inside TTL.

Further fixture classifications, not production defects: the first ready check
returned between pose receipt and the end-tick physical registration; it now waits
for both presentation and physical queries. Evidence of that premature check:
`../s29-adversarial-catalog-dimension-candidate.log`. The ordinary server catalog
publisher correctly refuses to publish an old revision after reload, so delayed
replay is delivered by raw wire from the authentic previously captured bundle;
publisher-guard evidence: `../s29-adversarial-catalog-dimension-physical-ready.log`.
Finally, material serials can be reused between catalog revisions. The oracle now
checks the old revision in the returned presentation identity and in the inbox,
rather than treating any newly valid interval with the same numeric serial as
resurrection. The incomplete earlier serial-only assertion is preserved in
`../s29-adversarial-catalog-dimension-wire-replay.log`, not counted as a defect.

Run the final fixture with the same init/directory properties documented above,
using `-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S29AdversarialCatalogDimensionProof`.
No early-reconnect property is needed. Test-only ResourceManager/reflection enters
the same transactional reload method; this is not a pack-UI installation test.
The replacement keeps the controlled quadruped geometry while changing its admitted
source identity, avoiding renderer-derived physical authority during reload.

Zero-change review after green: accepted-revision reset, model/binding replacement,
rejected older bundle, revision-scoped certificate identity, physical re-registration,
connection-scoped catalog versus level material, explicit dimension fence, and
fresh tracking progress. Client networking, runtime and replay-fence hashes stayed
unchanged throughout the lane; inventory:
`../s29-adversarial-catalog-dimension-source-hashes.json`. `git diff --check` passed.
ADVERSARY made no production changes. No unresolved production defect was observed
in this tested frontier.

G4.5 remains open for the complete acceptance matrix: broader accepted-catalog/
connection permutations, support authority in multiple destination dimensions,
applicable deterministic visual/contact snapshots, sustained heap/GC/fan-out
evidence and an entire-sprint zero-change pass. This lane covers the described
accepted replacement and real A-B-A boundaries, not those separate claims.
G4.6 remains untouched.

## Extension: confirmed contact across a connection barrier

`presentationContact` currently consumes CURRENT_ENDPOINT. This extension therefore
tests contact lifecycle authority under fresh support geometry; it does not claim
contact selection from CERTIFIED_INTERVAL. `S29AdversarialContactLifecycleProof`
creates a real small pig/cow support relation through the production solver and
captures the contact actually accepted by the client's presentation map. Its
positive baseline requires physically supported, read-only observer state with no
local simulation, prediction or transport. It closes a real dedicated connection,
stops anatomical publication during the gap, explicitly clears the server relation
and moves the body away, then reconnects to the persistent entities/catalog.
The support is reacquired with a new server-issued fixture tracking generation
before the unchanged old contact is replayed. The authority clock is held within
TTL, preventing age rejection from concealing a lifecycle defect.

RED production result: `../s29-adversarial-contact-reconnect-dimensions.log`,
BUILD FAILED 1m29s. The positive baseline records support binding 1/body generation
3; after replay `authoritative=true physicalSupported=false`, failing
`NFR-017/FR-080: pre-disconnect confirmed contact revived under fresh support window`.
The body was eligible under the support policy at injection. Reported to IMPLEMENTER;
production repair is pending independent verification. This defect prevents closure
of the contact frontier. Dimension/contact coverage remains pending separately.

Visual evidence: the fixture exports geometric bounds and the captured surface
anchor in fixed support-relative X/Z and Z/Y projections. Reusable renderer:
`python tools/s29_adversarial_contact_snapshots.py`. Inspected PNG:
`../s29-adversarial-contact-snapshots-red.png`; durable input metadata is
`../s29-contact-baseline.json`, `../s29-contact-before-replay.json`,
`../s29-contact-after-replay.json`. The first panel shows a correctly supported
small body and accepted anchor. Before/after replay have identical separated body
and support geometry, but the latter declares the old contact drawable again.
These are deterministic debug geometry/contact snapshots, not Minecraft renderer
screenshots or gameplay visual acceptance. They supplement the machine oracle.

Fixture classification: the first passive-state assertion ran before vanilla body
position converged; the final baseline waits for supported state. A subsequent
apparent green was vacuous: body scale/dimensions on reconnect were not eligible.
The final fixture uses a permanent scale modifier, verifies eligibility before
injection, and explicitly refreshes received dimensions because its frozen entity
clock suppressed that update. No contact authority is changed by this refresh.
Transient diagnostic typing/access failures were fixed only in the fixture.
Earlier logs `../s29-adversarial-contact-reconnect.log`,
`../s29-adversarial-contact-reconnect-diagnostic.log`,
`../s29-adversarial-contact-reconnect-settled.log` and
`../s29-adversarial-contact-reconnect-admitted.log` are not production-red or
accepted-green evidence. The final oracle also defines a fresh-contact positive
control after rejection; the RED correctly stops before that control.

### Exact replay repair and virtual retention repetition

The same final admitted fixture was rerun without changing its assertions:
`../s29-adversarial-contact-reconnect-repaired.log`, BUILD SUCCESSFUL 1m36s and
clean shutdown. Exact old replay returned `authoritative=false physicalSupported=false`;
the fresh body-generation contact control subsequently materialized through the
same support/geometry/policy path. Snapshot of the rejection state:
`../s29-adversarial-contact-snapshots-exact-green.png`. This independently validates
the exact-replay repair; other retired-window ordering and dimension contact
boundaries remain separate acceptance cases.

Reviewed and repeated IMPLEMENTER's `S29ImplementerIntervalSoak` unchanged in the
adversary's own server lane: `../s29-adversarial-interval-soak-repetition.log`,
BUILD SUCCESSFUL 53s, 2/2 selected tests (soak + sentinel). It accepted 3,073,024
independently allocated payloads across 16 inboxes/64 support identities for
12,000 virtual ticks. Half/full explicit-GC heap snapshots were both 238,651,144
bytes, delta zero, six GC cycles; cap/identity checks passed. This confirms the
probe's bounded-retention claim. It does not establish integrated wire allocation,
real-tick GC, body/contact memory, or NFR-014 p95/p99/TPS: its synchronous virtual
clock and explicit GC are outside those workloads. No performance production
change was needed by this repetition.

### Retired-window sequence and dimension contact continuation

The exact-contact repair was challenged with an unseen later sequence from the
same retired body generation (`-Ps29ContactDelayedSequence=true`). This preserves
all captured contact identity, surface, binding and clock fields and advances only
the sequence. It produced a second production RED:
`../s29-adversarial-contact-delayed-window.log`, BUILD FAILED 1m27s,
`authoritative=true physicalSupported=false`. The admitted real baseline and fresh
contact control remain the same. Reported NFR-017/FR-080; IMPLEMENTER subsequently
repaired strict contact-window retirement and monotonic runtime-wide recipient
generation allocation.

Unchanged replay oracles on the current repair, in own directories:
`../s29-adversarial-contact-final-delayed.log`, BUILD SUCCESSFUL 1m25s, clean exit.
Both actual dedicated disconnect/reconnect and actual integrated Overworld ->
Nether -> Overworld reject the delayed old-generation sequence, with
`authoritative=false`, and then admit the fresh-generation contact control. A
separate dimension run also passed:
`../s29-adversarial-contact-dimension-delayed.log`, BUILD SUCCESSFUL 1m36s.
These freshness controls are explicitly injected server fixture packets; genuine
runtime/solver generation progression is a separate oracle.

`S29AdversarialContactDimensionProof` clears the server relation, moves the body
off the support and keeps the old contact within TTL while crossing both real
level transitions. After return the support has fresh authority and the tiny body
is eligible before injection. The unchanged old generation cannot materialize
contact. Renderer `python tools/s29_adversarial_contact_snapshots.py --dimension`
exports `../s29-adversarial-contact-dimension-snapshots.png`. Inspected projection:
initial contact is drawable and supported, while identical separated geometry
before/after replay remains neither drawable nor supported, with no simulation,
prediction or transport in any phase. This is deterministic debug evidence,
not Minecraft visual-renderer acceptance. The delayed reconnect RED snapshot is
preserved at `../s29-adversarial-contact-snapshots-delayed-red.png`.

### Genuine contact reacquisition on the final candidate

Added `S29AdversarialContactLiveReconnectProof`: runtime remains active across
an actual dedicated socket disconnect/reconnect with persistent cow/pig UUIDs.
There is no pose, certificate, contact or generation injection. The first and
second relations come from real solver movement; accepted client contact must
match the actual server recipient generation, preserve epoch/revision and advance
past the pre-disconnect generation. Both observations require supported passive
state with no simulation, prediction or transport.

Final exact-replay + dimension + genuine reconnect lane:
`../s29-adversarial-contact-final-exact-live-port.log`, BUILD SUCCESSFUL 1m28s,
clean shutdown. Exact replays are not authoritative in either lifecycle transition;
fresh fixture controls pass. Genuine runtime contact advances server/client body
generation 3 -> 6 with the same accepted catalog and materializes correctly.
This independently verifies that strict replay retirement does not suppress fresh
real tracking/contact progress.

Infrastructure classification: preceding
`../s29-adversarial-contact-final-exact-live.log` failed to bind25565 before its
first oracle. Changing the root runDir server.properties to25575 did not affect
Fabric's generated dedicated world; the successful repeat used25565 after it was
free. This startup collision is neither a production defect nor oracle evidence.
Dedicated lanes still require scheduling to avoid shared-port overlap.

Scope of this frontier: contact is confirmed from CURRENT_ENDPOINT. No claim of
contact consumption of CERTIFIED_INTERVAL is made. The supplied real and injected
controls, dimension snapshots and virtual soak do not replace the remaining full
S29 visual/performance workloads, all matrix evidence and global zero-change
review. G4.5 remains open; no G4.6 work or production edits by ADVERSARY.

### Full final-candidate adversarial repetition

All thirteen executed ADVERSARY client proof classes were selected together on
this candidate with `-Ps29ContactDelayedSequence=true`:
`../s29-adversarial-final-candidate-client.log`, BUILD SUCCESSFUL 2m25s, clean exit.
Coverage includes accepted catalog/model replacement, invalid-JSON rollback,
seven certificate identity axes, ROOT intermediate endpoints, DERIVED_CARRY,
UNAVAILABLE retention, pending lifecycle ordering, two independent supports,
128-entry wire bounds, normal dedicated reconnect, dimension barriers, delayed
contact replay and genuine contact-generation advancement. The exact-contact
variant separately passed in the preceding three-class lane. These repetitions
reuse previously executed oracles; no production was changed by ADVERSARY.

Current-source probes pass: expired replay is not accepted or authoritative;
replay-fence locality remains3/4096 visits with closed-window, removal, cap/reuse
and saturation checks; the inbox campaign passes expiry/duplicates, exact identity,
epoch/dimension, per-support outcomes and global bounds. All eight scratch-source
inbox mutants were killed again (`expiry`, `bound`, `global-query`, `binding`,
`tracking`, `epoch`, `dimension`, `nearest`), recorded in
`../s29-adversarial-final-inbox-mutants.log`. The earlier executed frontend mutation
campaign remains separate evidence; no claim of rerunning it here.

Snapshot `../s29-adversarial-contact-final-source-hashes.json` records HEAD and all
nine modified productive paths; they were checked unchanged after the full client
lane. `git diff --check` passed. This is candidate integrity evidence, not the
still-required final global zero-change/release review. No open reproducible
production defect from this contact frontier remains in the completed client
oracles. G4.5 remains open for its full matrix, especially integrated sustained
NFR-014 timing/GC workloads and applicable actual-renderer visual acceptance.

Final server holdouts also pass on the unchanged productive hashes:
`../s29-adversarial-final-presentation-work.log`, BUILD SUCCESSFUL38s, 3/3
(two holdouts plus sentinel), clean shutdown. First historical presentation reuses
physical joint samples with `duplicateEvaluations=0`; alternating current and
historical endpoints yields `additionalJointEvaluations=0`. This validates bounded
joint-work reuse, not integrated wall-clock/render-frame performance. Final
`git diff --check` and productive SHA256 comparisons remain green.

### Global S29 review: applicable scope and new performance RED

The final scope reading corrects an overly broad earlier pending-evidence label:
NFR-014's integrated 64-support/64-body/16-real-player 12,000-real-tick benchmark
is explicitly G9 in the canonical performance protocol. Renderer/camera residual
consumption is G4.6. Neither is invented as an additional S29 implementation task.
S29 still must close its own HOT_NETWORK/HOT_PRESENTATION costs, bounds, reuse,
allocation/memory evidence and applicable primitive geometry/contact snapshots.
The contact projections are applicable S29 spatial evidence, not an untested claim
of later renderer alignment. All nine productive hashes remain unchanged.

Added reusable `S29AdversarialPresentationBatchProof`: 64 actually tracked cows
receive real Q2 ROOT certificates via the ordinary client wire. Server authority
is held while the real client queries exact material serials. It measures paired
baseline CURRENT_ENDPOINT queries against CURRENT_ENDPOINT + certified interval,
with alternating measurement order, 200 warm-up batches and 1,000 measured batches.
Scaling cases S=1/8/64 and C=1/16 represent query consumers in one client, not 16
real network players. ThreadMXBean reports client-thread allocated bytes, GC counts
are process-wide observations, and p95/p99 describe these query batches. They are
not tick/TPS or normative NFR-014 measurements. The harness deliberately blocks
client rendering while holding the server clock; resulting catch-up messages are
harness timing, not an observed gameplay TPS regression.

Native runs: `../s29-adversarial-presentation-batch.log`, BUILD SUCCESSFUL 1m6s;
`../s29-adversarial-presentation-batch-repeat.log`, BUILD SUCCESSFUL 1m13s, both
clean. Stable-fraction repetition at64 supports/16 consumers has p95/p99
1.124/1.533ms in the repeat (first run1.539/1.928ms), allocation876,544 bytes/batch,
no joint reevaluations. Varying the requested fraction between .25/.5/.75 once per
batch, while retaining identical certified endpoints, costs p95/p99 5.839/8.298ms
at64 supports/1 consumer and allocates6,774,816 bytes/batch; fixed fraction costs
p95/p99 .235/.318ms and59,983 bytes/batch in that run. At16 consumers, the changing
case costs6.181/7.197ms and7,597,056 bytes/batch, showing reuse within a batch but
material cost when the render fraction changes. Small timing rows vary with JIT;
no percentile subtraction or universal threshold claim is made.

To distinguish required vertex interpolation from repeated invariant preparation,
`tools/s29_adversarial_motion_preparation_probe.py` creates a disposable copied
HierarchyMotion with only a thread-local constructor counter/accessor. It does
not replace semantics, remove caches or change original production. The same live
query fixture reads the observational counter by reflection outside each batch.
Fixed fraction performs zero new preparations after warm-up. Changing fraction
performs exactly64,000 preparations in1,000 batches on the SAME64 certified pairs,
with zero joint evaluations. Each new preparation rebuilds hierarchy paths/nodes,
filter decisions and CCD plane/speed material although no endpoint identity changes.

**Production performance RED:**
`../s29-adversarial-presentation-preparation-counter-retry.log`, BUILD FAILED1m27s,
assertion `HOT_PRESENTATION: identical certified endpoints recompile invariant
motion preparation per fraction: 64000`. Observational run again reports6,368,288
bytes/batch and p95/p99 5.550/7.423ms for64 supports/1 consumer. Classification:
repeated invariant preparation and allocation churn in HOT_PRESENTATION, violating
the sprint performance protocol's reuse/no-preparation-leak review. It is NOT a
claimed NFR-014 integrated-budget failure. Returned to IMPLEMENTER; ADVERSARY
has not repaired production. This concrete defect blocks G4.5 closure until the
unchanged varying-fraction oracle and the relevant behavior matrix pass.

Reproduce by regenerating the counter copy from current source (never reuse a
stale counter copy after a repair):
`python tools/s29_adversarial_motion_preparation_probe.py ../s29-adversarial-motion-counter-source`;
then select `io.github.r3neer.scalebrews.test.S29AdversarialPresentationBatchProof`
in the own-directory client lane with init tools/s29_adversarial.init.gradle,
quoted property `-Ps29MutationOriginal=src/main/java/io/github/r3neer/scalebrews/collision/internal/HierarchyMotion.java`,
`-Ps29MutationDir=.../work/s29-adversarial-motion-counter-source`,
`-Ps29MutationEnvironment=main`. First counter invocation failed at task selection
because PowerShell split an unquoted .java property; classified fixture command
quoting, not a production RED or mutation kill. Only the retry reached the oracle.

Primitive visual evidence is now complete for the tested ROOT/JOINT cases:
`python tools/s29_adversarial_presentation_snapshots.py` renders the live wire
metadata at fixed fractions0/.25/.5/.75/1 into
`../s29-adversarial-certified-geometry.png`. Inspected TOP X/Z and SIDE Z/Y sheets
use fixed common projection scales, each overlaid on its fraction0 baseline.
Independent vertex oracle requires every ROOT vertex to move by exactly the
fractional .5-block translation (tolerance2e-5); JOINT requires unchanged root/body
and changed head geometry. Both pass. These are original model convex edges,
not AABB approximations or renderer animation. The JOINT sample also contains
server-issued limb inputs, visible in the leg transforms, with fixed root/body.
Durable metadata: `../s29-adversarial-presentation-batch-first.json` and
`../s29-adversarial-presentation-batch-repeat.json`. Reusable capture/renderer tools
stay isolated under gametest/tools. No new functional or spatial defect was found;
closure is now blocked by the concrete preparation-cost RED above.

### Independent verification of prepared-motion repair

Regenerated the observational HierarchyMotion copy from the current candidate and
reran the unchanged holdout: `../s29-adversarial-presentation-preparation-repaired.log`,
BUILD SUCCESSFUL 1m45s, clean exit. All measured stable/changing-fraction cases
report `compiledMotionPreparations=0` and `duplicateJointEvaluations=0` after
warm-up, including64 supports with16 query consumers. The former64,000 repeated
preparations are eliminated without weakening the assertion.

Changing fraction at64 supports/1 consumer now allocates2,408,852 bytes/batch,
p95/p99 2.409/3.363ms, versus the native RED measurements6,774,816 bytes/batch and
5.839/8.298ms. At16 consumers the repair measures3,230,208 bytes/batch and
4.259/6.774ms. Stable64-support/16-consumer queries allocate877,056 bytes/batch,
p95/p99 1.170/1.385ms. These are the same held-clock meso workload, not NFR-014.
The remaining geometry-frame interpolation allocation is explicitly observed;
the structural prepared-motion duplication is no longer present. Process GC counts
are noisy/global; allocation reduction and constructor counts are stronger signals.

Static repair review: the bound provider stores one prepared pair/motion per weak
entity key, not an unbounded material-serial history. Both complete endpoint values
must match; changed serial/clocks/root/joints/availability cause replacement.
Stored endpoints, geometry, node paths and motion closures do not hold the entity
key. Receiver certificate/identity validation precedes access, and provider/evaluator
owners are removed at support lifecycle barriers and catalog/level/socket changes.
The memo is an evaluator cache and cannot independently authorize an interval.
No new preparation/reflection path or endpoint-inference route was introduced.
Candidate integrity snapshot:
`../s29-adversarial-preparation-repair-source-hashes.json`.

The repaired capture again passes independent ROOT all-vertex interpolation and
JOINT fixed-root/body/moving-head assertions. Metadata retained separately at
`../s29-adversarial-presentation-preparation-repaired.json`; the projection generator
again exports `../s29-adversarial-certified-geometry.png` with the same fixed views.
The native full-client repetition and remaining affected server holdouts are
being completed before the final whole-sprint verdict.

Native repaired full-client lane also passed without the instrumented source:
`../s29-adversarial-preparation-repair-full-client.log`, BUILD SUCCESSFUL2m40s,
14 selected ADVERSARY proof classes and clean exit. The counter accessor is absent
(`compiledMotionPreparations=-1`) in this native run; it is not claimed as a native
counter measurement. Every identity, lifecycle, cap, exact endpoint, contact,
ROOT/DERIVED_CARRY and real reconnect oracle from the prior matrix still passes.
Native changing-fraction64-support/1-consumer p95/p99 are2.931/3.661ms with
2,386,667 bytes/batch;16-consumer results3.760/4.311ms with3,207,168 bytes/batch.
No duplicated joints are observed. These corroborate the reduced cost without
using the observational counter copy for timing. Native metadata preserved at
`../s29-adversarial-preparation-repair-native.json`. The fresh native ROOT/JOINT
sheet was inspected again and both independent geometric assertions passed.
The candidate productive hashes stayed identical through this complete lane.

## Final whole-sprint ADVERSARY verdict

**GREEN — the reviewed S29/G4.5 candidate is ready for canonical closure.**
The production preparation defect is independently repaired. The complete final
review produced zero further production/test-design changes or unresolved S29
performance findings. ADVERSARY does not modify the canonical roadmap, IMPLEMENTER
record or VALIDATION as part of this handoff; the owner integrates the evidence
and logical sprint commit. No G4.6 implementation was begun.

Final additional affected lanes on identical productive hashes:
`../s29-adversarial-preparation-repair-barriers.log`, BUILD SUCCESSFUL1m32s,
clean exit: DOWN -> WEST -> DOWN old-certificate replay authoritative=false and
real dedicated early reconnect old material authoritative=false. These supplement
the default UNAVAILABLE/normal reconnect cases in the fourteen-class native lane.
`../s29-adversarial-preparation-repair-server-work.log`, BUILD SUCCESSFUL39s,
3/3 with clean exit: first historical duplicateEvaluations=0 and alternating
physical/current/historical additionalJointEvaluations=0. Final productive SHA256
comparison against preparation-repair-source-hashes and git diff --check pass.

| Canonical S29 acceptance boundary | Executed evidence / verdict |
| --- | --- |
| Real certified publication/materialization | ROOT two same-tick intervals, live JOINT without root displacement, real DERIVED_CARRY child contact/carry, native full-client GREEN |
| No inference / exact serial | Missing certificate/consecutive serial and explicit missing/gapped serial remain fail closed; exact fraction0/.25/.5/.75/1 endpoints preserve geometry |
| Ordering | Certificate before, between and after poses; matching early binding/tracking RESTART survives, mismatches reject; real wire GREEN |
| Identity/lifecycle | Seven independent axes, support/network identity, binding/tracking, epoch/revision/dimension, UNAVAILABLE, gravity roundtrip, expiry, accepted/rejected catalog, dedicated/reconnect, early replay GREEN |
| Contact coherence | Real server baseline and solver reacquisition, passive observer3 ->6, exact/delayed retired contacts reject across socket and A-B-A dimension; CURRENT_ENDPOINT ownership, no claim of conditional CERTIFIED_INTERVAL contact consumer |
| Bounds/locality/memory | Real128/129 receiver boundary and saturation recovery; per-support/global caps,100-tick TTL and expired replay; local3/4096 replay-fence lookup; independent12,000-virtual-tick64-support/16-inbox settled-heap soak GREEN |
| Required mutation sensitivity | Nine compiled frontend variants kill all eight canonical families, plus eight inbox mutants; compile/configuration errors not counted |
| Applicable visual primitive | Inspected exact convex-edge ROOT/JOINT fixed-fraction contact sheet with independent vertex/root/body/head assertions; dimension/reconnect contact projections compare equal geometry before/after rejection |
| Applicable performance | HOT_TICK/NETWORK/PRESENTATION scaling, local bounds, owner/reuse, preparation/reflection/allocation/weak-key review; native paired meso costs measured; thread-local motion preparation and joint duplicate counts now zero |
| Final independent pass | All14 client proof classes, additional gravity/early reconnect,3 server holdouts; unchanged candidate hashes; no additional productive defect/change |

The generic earlier references to pending integrated NFR-014 and actual renderer
acceptance are resolved by scope, not by falsely claiming those workloads were run.
Canonical performance protocol section7 explicitly keeps the normative64-support/
64-body/16-real-player12,000-real-tick p95/p99/20TPS benchmark at G9. Its failure
can still reopen appropriate architecture in that later workload. S29's meso
measurements, virtual soak and zero-preparation counter do not replace it.
Actual renderer/residual/camera integration belongs to G4.6; original-model debug
geometry/contact projections are the applicable S29 visual evidence permitted by
AGENTS and the workflow. These are explicit future gates, not unfulfilled tests of
the reviewed certified primitive. All currently revealed S29 holdouts and minimum
mutation families have executed results; no private unexecuted closure holdout is
being concealed.

Reusable oracles, source-copy instrumentation generator, selector/init and snapshot
renderers remain isolated under gametest/tools. Disposable source copies and all
logs/captures are outside production; counter copies must be regenerated before
reuse. No temporary CI workflow, data pack or live fixture server is left running.
