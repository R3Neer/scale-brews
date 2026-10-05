# S45 - automatic Minecraft model/family census

Status: closed after independent acceptance and complete zero-change reviews; own local closure commit Sprint: S45.
Active role: **IMPLEMENTER**.

## Scope and thesis - IMPLEMENTER

S44/G5 closes in its own commit858b4ff5 with a verified clean tree. A fresh
fetch still resolves independent main to b5e62535, already integrated. The next
coherent frontier is G6 tasks1-2: execute the complete Minecraft26.2 LivingEntity
census and identify original renderer/model-family/structural-variant candidates
without a hand-maintained species list. The census must expose the actual work
remaining for subsequent family implementation instead of inventing FULL rows.

Normative scope: FR-016/024/026/038/039 and FR-089..092; NFR-019/020/024/028/029/032
and preparation locality/bounds. This sprint does not implement every missing
pose family, authorize new exclusions, close UNRESOLVED=0, migrate consumers or
claim G6 complete. Current positive/negative requirements and G7-G9 remain.

## Investigation - IMPLEMENTER

CollisionCoverageDiscovery already discovers registered living types through
DefaultAttributes.hasSupplier, including player, without constructing entities
or wiring discovery into runtime. CollisionCoverageScanner owns canonical row
classification and provenance. These S22 foundations are TRUSTED for their
existing scope; they do not authenticate model/pose availability merely because
a binding exists. The current base resource catalog contains no complete
Minecraft anatomy bindings; the isolated original export contains cow and
wide/slim player geometry, not a complete target.

The client ModelPart preparation engine/extractor is reusable and bounded.
Original26.2 APIs inspected from the installed merged JAR expose Model.root(),
ModelPart.getInitialPose(), LivingEntityRenderer.getModel(), renderer-held model
fields and dispatcher-wide/slim avatar maps. Ageable renderers hold adult/baby
models; other renderers hold multiple structural models, and layers can hold
additional model candidates. A single current getModel result cannot prove
variant completeness. Original models are mutable shared rendering objects;
preparation must inspect/copy without changing their pose, visibility or selection.

Existing Java pose providers cover a limited set of source families and retain
explicit unsupported states. This census reports candidates and canonical gaps;
it must not select a static pose for an animated model, fabricate geometry from
an entity AABB or publish an unverified ordinary/variant binding.

## Implementation plan - IMPLEMENTER

- [x] I1 Finish the exact installed API/source inventory for registry discovery,
  dispatcher/renderer/model slots, structural avatar dispatch and layer models.
  Revalidate that no new runtime foundation is needed; new census/fingerprint
  ownership is UNPROVEN until independently certified within this sprint.
- [x] I2 Add an explicitly invoked client PREPARATION census in the existing
  preparation layer. Consume the single canonical discovered population and
  classify it against the actual canonical binding catalog through the existing
  scanner. Every discovered type receives one stable inventory row, including
  missing factory/renderer/unsupported model/inspection failures with reasons.
  No normal initialization/tick/render/physics/network path invokes the census.
- [x] I3 Inspect original renderer-owned model candidates generically through
  bounded, sorted fields/containers and renderer layers; identify semantic slot
  paths and structural avatar alternatives. Group candidates by geometry
  technology, original model class and actual setupAnim declaring family, with
  original entity/slot membership retained. Do not use an id-to-species switch
  or confuse field candidates with verified executable state selectors. An
  unsupported selection/composite stays an explicit future verification gap.
- [x] I4 Fingerprint original ModelPart structures using an independent copy
  of cube/child topology and initial part pose, with explicit size/depth/field/
  container limits and distinguishable exhaustion. Reuse the existing extractor
  for structure inspection, never as fallback material. Preserve all mutable
  original fields. Stable field ordering and duplicate identity handling must
  preserve distinct semantic slots and avoid cycles. The fingerprint is a
  structural inventory identity, not a verified renderer transform or physics
  catalog. Missing/invalid structures cannot turn into empty-but-FULL success.
- [x] I5 Emit immutable deterministic inventory/family/coverage data and an
  identity incorporating the supplied pinned target manifest and tooling
  version. The actual acceptance harness records game/Fabric/Scale versions,
  source-JAR/tool hashes and selector inputs. Repeat generation with identical
  inputs must produce identical data/digest; changed structural/input identity
  must invalidate it. No manual FULL/SAFE_PARTIAL/EXCLUDED row override or
  bypass of canonical scanner provenance is added.
- [x] I6 Execute an original Minecraft26.2 client census plus separate server
  population cross-check. Preserve all ordinary current regression suites.
  Implementation-facing tests cover complete population, grouping, immutable
  originals, repeat determinism, bounds and expected unresolved acceptance.
  ADVERSARY independently verifies membership, non-material/variant gaps,
  omission/cost/reproducibility, holdouts and targeted sensitivity. Record a
  machine-readable actual census, no synthetic model replacement for original
  evidence. Snapshot only where a spatial property is exercised.
- [x] I7 Complete both full zero-change reviews, update executed VALIDATION and
  only the G6 tasks justified by evidence, create S45's own local closure commit
  and verify HEAD/tree. The complete target remains unresolved until later
  family proofs/catalog generation close it; no sprint green substitutes for
  the whole epic or for owner-only G9 acceptance.

Path classification: new work is PREPARATION/OFFLINE_TOOLING, not HOT runtime.
Scaling variables are registry types T, renderers R, fields F, candidate slots V,
parts P/cubes C and source bytes I. Reflection/model traversal is isolated to one
explicit bounded preparation pass; no per-query model evaluation, physics cache,
runtime world scan, server renderer dependency or client geometry upload is added.

The complete initial IMPLEMENTER plan review found no extra requirement or scope
change; independent plan convergence is recorded below. Any model gap or
new foundation found during that review returns here before source changes.

## Preimplementation model and plan review - ADVERSARY

Independent G6 clean-room model was prepared from requirements/architecture and
protocol before consulting historical tests or this plan, on S44 HEAD858b4ff5.
The concrete holdouts remain private. Active role for this section: ADVERSARY.

Independent obligations within the proposed census boundary:

- The actual registered LivingEntity population is the denominator, independently
  cross-checked against the discovery result. Renderer absence, factory failure,
  inspection exhaustion and unsupported composites cannot remove a type from the
  report. Reusing the same discovery on client/server is a consistency check,
  not by itself an independent completeness oracle.
- One canonical coverage row per type is separate from any number of original
  renderer/model slots. Structural avatar/age/alternate slots are inventory
  candidates; a field name or class family is not a verified state selector.
  Shared object identity must not erase different semantic slot membership.
- Canonical classification/provenance remains authoritative. Ordinary FULL needs
  executable geometry AND authoritative poses for required variants/states.
  Extraction success, static/rest pose, a renderer class and a plausible family
  name cannot promote unresolved/partial coverage. This sprint inventories gaps;
  it cannot approve technical exclusions or close the full G6 coverage gate.
- Preparation observes the real fixed-version models without changing pose,
  visibility, selection or other mutable originals. Fingerprints include actual
  initial hierarchy/cubes/transforms and expose malformed/empty/exhausted cases;
  they are not assertions about renderer world transforms or causal colliders.
- Registry/model/manifest/tool identity is deterministic under the same exact
  inputs. Changed structural/source/tool inputs must change the acceptance
  identity. Supplied provenance is checked against the actual acceptance harness
  sources, not treated as authentication merely because strings are present.
- The new scanner/inspection/fingerprint extension remains UNPROVEN until actual
  original-client comparisons, independent membership, bounds, nonmutation,
  deterministic repeat and omission/identity sensitivity certify it. Existing
  foundations are TRUSTED only within their previously accepted scope.

Performance model: PREPARATION/OFFLINE_TOOLING with T registry entries, V semantic
slots, F inspected fields, P parts/C cubes and bounded source/container sizes.
Audit recursion/cycles, identity deduplication, sorting, allocation and retained
renderer/world references; no normal tick/query/render/network invocation may
perform census traversal or compile model structures. A tooling wall-clock is
not a physics runtime percentile. Mutation/fault checks must detect omitted
rows, forged status/provenance and altered structure; concrete cases stay private.
Spatial snapshots supplement assertions if the candidate exercises spatial
structure. No new pose correctness or consumer/full-pack proof is inferred.

Plan review I1-I7: funciona, continúa. The scope is dependency-coherent for G6
population/family inventory, preserves the real original reference and separates
candidate model slots from verified executable variants. I6 permits the required
independent completeness and negative sensitivity checks; I5 retains canonical
classification rather than granting manual FULL overrides. No operational plan
RED remains after the full independent review. This is planning convergence,
not runtime acceptance; all implementation checkboxes and G6 remain open. No
production or IMPLEMENTER section was edited. G7-G9 and human needs remain later.

## Implementation and verification - IMPLEMENTER

I1-I5 implementation exists in two explicitly invoked client preparation owners:
ModelFamilyCensus and ModelPartStructure. Reflection is bounded per row (512
fields, 256 semantic model slots, 512 entries per container, 4096 total entries,
12 container levels); ModelPart copies are capped at512parts/4096cubes/64levels/
65536vertices. The pass caches each original model by identity, preserving all
semantic slots, and retains at most4096 model inspections. No production runtime
caller exists. Sorted fields/maps and length-framed initial hierarchy/cube/pose
identity yield immutable candidate rows/families; unknown/empty/invalid/exhausted
inspection remains diagnostic rather than fallback material. Census coverage is
issued only by canonical discover/scan against the supplied actual catalog.

Exact26.2 investigation additionally found record-wrapped AdultAndBabyModelPair
in CowRenderer.models; record containers are generically inspected under the
same bounds. Avatar player/mannequin dispatcher maps mirror actual original
26.2 dispatch. Registry dispatcher slots require no entity factory, so the census
never creates entities or depends on a missing player factory. The independent
server population check constructs the actual factories separately and includes
the actual connected player; getBaseClass() was verified to return Entity.class
for all types and is therefore not used as a living-population oracle.

The reusable opt-in scalebrewsS45CensusOnly test selector invokes the original
client/server S45MinecraftCensusProof. Its full exact loaded-mod source identity
includes versions, JAR/directory digests, loaded tooling bytecode and selected
pack id manifest; machine reports retain candidate scope and actual unresolved
canonical gate. Initial run i1 reached the real server but failed in the test
manifest because the selected pack-id list exceeded Target's512-character value
bound. The harness now hashes that complete list instead of weakening the bound.
No production acceptance is inferred from this development failure.

Initial i2 reached actual structure inspection but the implementation test used
IdentityHashMap.equals on newly allocated State values, which compares value
identity rather than the record state. This was a test-oracle defect; it now
compares equal fields for each original identity and equal membership/count.
No production source was changed to satisfy that failed oracle.

Full IMPLEMENTER review of both production owners, callers, bounds, structure
copy ownership, deterministic encoding and canonical provenance produced zero
further production changes after the bounded record-container/entry-budget
review. No runtime integration, solver or resource binding was added.

A later complete identity review reopened production for one correction: enum
map slots now use declaringClass+name, because Enum.toString can be overridden.
The next complete review also corrected variable map-key delimiters by framing
the key length and rejected malformed Unicode during UTF-8 hashing (replacement
encoding could alias different malformed names). The adversary independently
observed a flat/nested slot collision in its already-compiled prior candidate;
that RED is classified as an implementation identity defect and its unchanged
oracle is rerun against the already-corrected candidate. No oracle is rewritten
to satisfy production. The complete review restarted against all three identity
corrections and produced zero further changes. Original i3/build results below
predate these corrections; final census/build acceptance is in progress.

Actual original i3 PASS in1m58s:93types/95dispatcher alternatives/561semantic
slots/467identity-cached model inspections/120candidate groups,4499inspected
parts/3217cubes,4798shared mutable parts compared before/after. First preparation
was157310000ns (preparation only, no runtime percentile claim). Repeat identity,
changed manifest, initial-structure invalidation, empty/cycle/depth/nonfinite
checks and independent server factories+connected player PASS. Only factory
null is minecraft:player. The actual GameTest catalog carries the existing
external armor_stand fixture variant:92UNRESOLVED+1SAFE_PARTIAL,0FULL, and
requireResolved correctly fails. The report includes original Minecraft models
plus current installed Scale layers (e.g. TinySaddleLayer); those cosmetic slots
are candidates, never authorized physical anatomy.9slots have no material volume.

Actual initial build PASS in2m7s with466/466 ordinary server GameTests. Full
default client regression PASS:32entrypoints in5m4s, with no census selector;
this lane loaded the preidentity-correction tooling, which none of its32proofs
invokes. Final corrected census PASS1m59s, same93/95/561/467/120counts and unchanged
original state; first preparation354938400ns while other lanes were active.
No physics timing/isolated throughput claim derives from these startup samples.
Final corrected build PASS2m35s with466/466 required server GameTests. Actual
census identity is3b7762e12c4f3e99467df8e992ad2d6cd3da733464bd12cc90ee333d4a570048;
coverage remains92UNRESOLVED+1fixtureSAFE_PARTIAL+0FULL. Final JAR SHA256
3418a682f7b3c3f19edb0881d63ef35c0a84ca9e66f93c78c5857c2d3580af4a,
1174236bytes/732entries, contains the two preparation owners and no GameTest or
retired platform classes.360production source/resource hashes, executed log
hashes and census preimage/report hashes are pinned in implementation-verification.
Default processed manifest restored in11s and verified32client entries/noS45
entrypoint; exact source/processed hashes and list in default-manifest.json.
All eight existing ownership/protocol gates PASS (presence gates remain presence
only), with exact tool hashes/output in ownership-gates.json. Complete final
IMPLEMENTER source/evidence review produced zero further changes. Independent
final ownership/cost/holdout sensitivity and zero-change review PASS below. Actual census and
canonical preimage are recorded under docs/evidence/S45; generated worlds and
cache output remain outside the repository.

## Independent verification - ADVERSARY

Final candidate owners: ModelFamilyCensus SHA256
`a5ff6c1fa843e5c2d5bc3b97ee5acc43dde7d86905671fc66df726e4639c6511`,
ModelPartStructure SHA256
`2de374c107be79d58c03aab155ecf515bf19a836dc34e98a37f8fb50fd1de098`.
All executed log hashes/classifications and the bounded work/lifetime model are
in `docs/evidence/S45/adversarial-verification.json`.

- Independent real factories plus the actual connected player discover93 living
  types, no null factory remains, and match the complete canonical census. This
  is independent of DefaultAttributes discovery. The initial getBaseClass oracle
  was invalid (it returns generic base metadata here): first run infrastructure
  RED, repaired to actual factories before acceptance, without production edits.
- Final real-client observation reads523 dispatcher/layer models and4798 shared
  parts before/after inspection; position/rotation/scale, visibility, skipDraw
  and initial pose remain identical. The broader observer includes other
  dispatcher candidates beyond the467 models inspected by the census. Repeated
  inventory text/digest matches; changed supplied metadata changes identity.
  Real catalog produces no FULL; candidates never materialize a collider.
- The intact independent flat/nested slot oracle reproduced a production alias
  on the pre-fix owner (`../s45-adversary-slot-first.log`,54s). A nested map key
  `a` then `b` and flat key `a][java.lang.String:b` gave the same path. This is a
  recorded production RED, not an invocation problem. IMPLEMENTER independently
  repaired framing; reexecution yields distinct paths and both slots survive
  shared Model identity. No additional fix was requested after that repair.
- Enum keys with constant-specific overridden/mutable labels remain distinct and
  stable by declared name. Malformed high/low surrogates and malformed child
  names are rejected; valid supplementary Unicode does not collapse to `?`.
- Structural fingerprint ignores current renderer pose/visibility while changes
  to initial pose invalidate it. Nonfinite initial pose and cycles fail. Exact
 512parts passes/513 fails; container513 does not traverse; nested entries stop
  at4096 and slots at256 with explicit gaps; container depth exhausts explicitly.
  Structural depth64 passes/65 fails, cubes4096 passes/4097 fails, and the vertex
  cap's valid predecessor/overflow are exercised. A worker thread is rejected
  before census work. The first depth fixture used long `child` path segments
  exceeding the preexisting256-character part-id bound; shorter valid segments
  preserve the64/65 depth oracle. This was setup RED, not a loosened depth limit.
- Ownership sensitivity uses an isolated ModelPartStructure copy that resets
  `original.loadPose(pose)`. The unchanged original-state oracle fails with
  `Original model mutated by census` in
  `../s45-adversary-mutant-ownership-first.log` (2m29 expected RED). It never
  modified production. The natural alias RED also demonstrates slot-oracle
  sensitivity against the faulty candidate.
- Promoted reusable tests are S45AdversarialCensusProof, SlotIdentityProof,
  IdentityProof and BoundsProof. All four pass with clean shutdown in
  `../s45-adversary-public-expanded-second.log` (57s). Command:
  `./gradlew -I tools/s45_adversarial.init.gradle -PscalebrewsBuildDir=../s45-adversary-build --project-cache-dir ../s45-adversary-cache runClientGameTest --console=plain`.
  Optional `s45AdversarialRunDir` overrides only this lane's output location.
  Component slot/budget probes deliberately reflect the private walker seam;
  the separate census proof exercises the actual public client entrypoint.

Independent cost/ownership review: both owners are explicit PREPARATION with
zero production callers. Variables T,V,F,A,P,C and input/revision bytes govern
traversal/reflection/sorting. Structure inspection caches once per Model identity
within a pass, not once per slot/observer; actual work is95renderers,1986fields,
370container entries,561slots,467unique structures,4499parts/3217cubes. Bounded
fault probes above establish exhaustion, not global runtime percentiles. Pass
maps hold originals only for the call; returned Inventory holds immutable scalar/
string fingerprints and canonical coverage DTOs, with no new static model/world
cache or lifecycle listener. No reflection/preparation is added to tick, move,
network or presentation. Initial cubes are read-only and copied-tree extraction
never resets the shared original. ModelFamilyCensus/ModelPartStructure extensions
are TRUSTED within original Minecraft population/candidate-slot/fingerprint
inventory; this does not certify selector/pose/root correctness or physics FULL.

Final zero-change review: all360 productive file hashes match IMPLEMENTER's
frozen record. Compared with S44, zero existing productive files change and
exactly two preparation Java owners are added; API/solver/gravity/contact/network/
mainline consumers and resources remain unchanged. Executed IMPLEMENTER log
hashes were checked: final original census1m59, build466/4662m35, and default32
client5m4 before isolated tooling identity fixes, with no runtime callers added.
The real pinned census/provenance/canonical artifacts are attributed to that
execution. ADVERSARY's lightweight fixture-target digest probes identity changes;
it is not substituted for the pinned acceptance report. No new spatial pose or
world-transform assertion is made by this inventory sprint, so no diagram or
screenshot is used to imply verified physics/animation. Deterministic spatial
comparisons remain required when later family/pose work exercises those properties.

Dictamen — ADVERSARY: funciona, continúa. Complete independent I1-I7 review
requires no further production change, no unresolved S45 RED and no pending
promotion. G6 task1 can close for the actual complete census. Task2 and complete
FULL/variants/families/catalog coverage remain open;93 inventory rows do not prove
93 ordinary FULL entities. G7/G8, G9 runtime benchmark/soak/full-pack/human needs
remain separate. IMPLEMENTER owns canonical status updates and the S45 commit.

## Final review and closure

S45 closes after both complete review passes require zero further changes.
New inventory foundations are TRUSTED only in the bounded candidate census scope.
G6 task1 closes for actual complete original Minecraft discovery/scanner execution;
task2 remains open because candidates are not verified executable engine/variant
selectors. Full physical family/catalog coverage and G7-G9 remain. No new human
question interrupts autonomous work; owner-only needs stay in the G9 matrix.
The closure commit identifies Sprint: S45 and contains implementation, reusable
independent tests, actual evidence and necessary canonical status. HEAD/tree are
verified after that commit before advancing to the next sprint.
