# S46 - prepared variant selections and complete binding identity

Status: closed after independent acceptance and complete zero-change reviews; own local closure commit Sprint: S46.
Active role: **IMPLEMENTER**.

## Scope and thesis - IMPLEMENTER

S45 closed in its own local commit cbdb28cb with a clean tree. Independent main
is b5e62535, already integrated. G6 task2 needs executable structural variant
selection before ordinary model families can be certified. The first dependency
is the accepted catalog: it currently prepares all canonical bindings, then
discards every nondefault executable selection.

At the end of S46, every executable canonical variant will remain available in
the accepted immutable snapshot through the existing selector semantics and a
complete content identity, demonstrable by independent selection/transfer and
atomic rejection probes, without assuming runtime entity-state selection.

Normative scope: FR-004/024/026/033/035/036/037 and NFR-007..014/016/019/026..028.
This is a PREPARATION and immutable lookup boundary. It does not add a variant
state adapter, change server/client runtime selection, modify pose/contact wire
payloads, implement missing families or promote coverage to FULL. Those are the
next dependency-coherent frontier; G6 task2 and G7-G9 remain open. Owner-only
needs stay in the canonical G9 matrix until the whole epic ends.

## Investigation - IMPLEMENTER

CollisionBindingCatalog is the sole canonical declarative selector. It validates
all registered engines, retains deterministic entity/variant order and resolves
most-specific subsets, with equally-specific ambiguity returning absence.
WorldAnatomyCatalog prepares every selection before acceptance, but publishes
only catalog.resolve(entity, emptyMap) in Snapshot.bindings. The latter is an
existing default-only compatibility projection used by runtime consumers.
AnatomyCatalogTransfer already carries every canonical variant and atomically
reconstructs WorldAnatomyCatalog. No new catalog wire schema is needed here.

The current pose wire identifies model/pose/root and generation; those three
IDs cannot distinguish bindings differing in parameters, filters, channels,
policy or excluded states. A complete prepared selection identity is required
before the later lifecycle sprint can publish exact variants. Identity is
content identification, never authentication or a replacement for epoch,
revision, entity ownership, binding generation or tracking generation.

Existing S00/S16/S20 catalog validation, selector and transaction foundations
remain TRUSTED within their verified scope. The new executable index and
complete content identity are UNPROVEN until independently tested here. No
new geometry, pose evaluator, renderer dependency or solver foundation is used.

## Implementation plan - IMPLEMENTER

- [x] I1 Retain an immutable revision-local executable index for ALL prepared
  canonical selections alongside the existing default-only projection. Validate
  index membership and exact binding selection against the canonical catalog;
  preserve old Snapshot constructor source compatibility. Compatibility callers
  may supply their existing default bindings without inventing other prepared
  variants or lazily compiling them.
- [x] I2 Expose an immutable snapshot lookup by entity and supplied variant map
  which delegates selection exclusively to CollisionBindingCatalog.resolve,
  then retrieves that exact prepared binding. Ambiguous, absent, catalog-only
  and unavailable selections return absence, never a less-specific executable
  fallback when the canonical selected row is not executable.
- [x] I3 Prepare a versioned, domain-separated SHA-256 identity from the complete
  CollisionCodecs.BINDING encoding, with recursively deterministic JSON object
  ordering and canonical collection ordering. Include every canonical field.
  Reject malformed Unicode rather than replacement encoding; bound the identity
  representation under the existing catalog byte ceiling. Build identity tables
  once in preparation; no hash, codec, compile, reflection or registry locking
  in lookup. Canonical selector results retrieve prepared entries by owned object
  identity, avoiding repeated record hashing of large parameter/filter maps;
  querying a prepared binding's ID likewise uses its owned object identity.
  Exact entity+identity lookup must reject wrong entity/unknown ID.
  Detect conflicting identity entries before atomic acceptance.
- [x] I4 Construct executable tables and the existing prepared wire bundle before
  publishing their single accepted snapshot/bundle pair. Invalid nondefault
  candidates retain the exact prior pair. Transfer reconstructs the same
  complete executable index/identities from canonical bindings without sending
  an independent redundant identity table. Current default runtime behavior and
  default client manifest remain unchanged.
- [x] I5 Add implementation-facing probes for default plus variants, variant-only
  catalogs, subset/ambiguity/absence, identity field sensitivity and permutation
  determinism, wire reconstruction, atomic rejection and repeated prepared
  lookups without recompilation. Use actual shared catalog engines and small
  valid model fixtures; do not manufacture ordinary Minecraft FULL evidence.
- [x] I6 Execute the complete server build/GameTests and default client suite.
  ADVERSARY independently owns the model, holdouts, mutation/fault evidence,
  immutable ownership and preparation/lookup performance review. Spatial model
  transforms are not changed by this sprint; snapshots become necessary if a
  spatial property or render regression is discovered, never as a substitute
  for the canonical identity and transaction oracles.
- [x] I7 Complete both zero-change reviews, record exact executed evidence and
  remaining variant lifecycle/family work in the canonical documents, create
  S46's own local closure commit and verify HEAD/tree before S47.

Scaling: B accepted bindings (existing maximum4096), bounded normalized binding
bytes and selector entries. Preparation encodes/hashes each executable binding
once and builds immutable tables; selector lookup retains the existing O(B_type)
bounded matching, identity lookup is O(1) expected. No world enumeration,
renderer traversal, geometry preparation or payload reconstruction is introduced
in steady lookup, tick, render or physics paths.

## Plan review - IMPLEMENTER

Initial investigation considered migrating live entity selectors and pose wire
in this sprint. Those need the complete accepted index first and are deferred
to the next sprint; keeping them here would join preparation and live lifecycle
frontiers. A full review of requirements, ownership, consumers, transaction
failure, compatibility, simplicity and verifiability after this split produces
zero changes. A further cost review identified record-key hashing as repeated
work over potentially large parameter/filter maps; I3 now specifies owned object
identity lookup. The full review restarted after this change and produces zero
additional changes. The plan is ready for independent adversarial convergence.

## Preimplementation model and plan review - ADVERSARY

Active role: **ADVERSARY**. Independent conceptual model was constructed from
canonical requirements/architecture before reading this implementation plan,
and revalidated at S45 commit cbdb28cb. No concrete battery has been designed
and no production has changed during this review.

**Prepared-state model.** An accepted revision owns the canonical declarations
and exactly the executable bindings prepared from them. Selection is the
existing declarative subset/ambiguity rule, not a new runtime variant authority.
Retrieving a selected row must preserve that row's geometry, pose, root, policy,
channels, filters, parameters and exclusions as one binding. A selected row
without an executable representation returns absence; the default projection
cannot substitute a less-specific executable row. Compatibility constructors
may preserve their supplied default bindings but cannot claim preparation of
missing selections. Their entries must correspond to their canonical selection.

**Identity model.** The new ID names complete canonical binding content in a
versioned domain. Equivalent declaration/collection ordering must preserve it;
any semantic field change must be represented. Canonicalization respects field
semantics rather than sorting arbitrary order-sensitive data. Malformed text,
conflicting entries or representation overflow reject preparation before
publication. Owned object identity is an internal lookup key, never a serialized
identity or a content-equivalence claim. Content IDs do not certify executable
model bytes or renderer agreement by themselves, and do not replace revision,
resource provenance, tracking/binding generations or entity ownership.

**Transaction and transfer model.** All nondefault declarations receive the same
validation as defaults before acceptance. Snapshot and prepared bundle publish
as one pair; any preparation/index/identity failure leaves the exact prior pair.
Wire reconstruction uses the canonical declarations and referenced resources to
recover the same selection and identities, without a second identity authority.
No default runtime consumer changes its selection behavior in this frontier.
Prepared maps and identity indexes remain revision-local and immutable, without
retaining caller-owned mutable tables or lazily preparing absent entries.

**Cost and foundation model.** New prepared indexes and identity construction
remain UNPROVEN extensions until independently certified. Preparation work is
bounded by accepted bindings and encoded bytes. Selection keeps its declared
per-type matching cost; exact prepared lookup must not hash nested binding
records or repeat codec/hash/compile/reflection/registry work. Identity maps
retain only accepted revision-owned entries, with no global entity/history scan
or persistent cross-revision cache introduced. The later runtime selector will
need its own hot-path locality and lifecycle recertification; this review does
not approve a future per-tick scan merely because preparation lookup is bounded.

**Convergence: funciona, continua.** A full independent pass against FR-004/024/
026/033/035-037 and the applicable performance/ownership protocol finds no
blocking contradiction in I1-I7, including I3's owned-identity lookup. The
PREPARATION split is a coherent dependency and preserves the clean-room model's
live selection, structural transitions, replay fences and original-model
agreement obligations for the immediate next frontier. G6 task2 and later gates
remain open; S46 cannot claim FULL or runtime variant support. Source inspection
confirms the existing canonical selector and default-only snapshot projection
underlying the plan. This is planning conformity, not executed candidate
acceptance or recertification. Holdouts will be designed independently after
convergence and remain private until exercised.

## Candidate implementation and iterative review - IMPLEMENTER

I1-I5 are implemented. The accepted Snapshot retains PreparedBindingIndex;
default-only accessors and both earlier constructors remain available. The index
owns immutable tables keyed by canonical selection/Binding object identity and
complete content IDs. Its selector delegates to CollisionBindingCatalog rather
than reproducing precedence. The only modified existing production source is
WorldAnatomyCatalog; no live runtime consumer, payload or manifest client list
has changed. Two new common owners contain no renderer/world/entity references.

CollisionBindingIdentity uses the complete canonical codec, recursively orders
JSON objects and preserves codec array ordering, validates raw string Unicode
before serialization, encodes UTF-8 strictly and applies the versioned SHA-256
domain. Identity validation covers all canonical declarations, including
catalog-only rows, so lack of geometry cannot hide malformed text. Only
executable identities are retained. Model/resource bytes and lifecycle identity
remain separate obligations, as required by the independent model.

The first production review tightened Snapshot's default projection from record
equality to exact prepared Binding instances and removed duplicate version
constants from the identity domain/prefix. It then restarted across requirements,
ownership, callers, fail-closed/transactions, compatibility, dead code, simplicity
and preparation/lookup cost; the complete pass produces zero further production
changes. New owners remain UNPROVEN until independent acceptance finishes.

Nine implementation-facing probes cover all retained variants, canonical subset
and ambiguity selection, unknown/wrong ownership, unavailable-specific absence,
variant-only wire reconstruction, every canonical decision's identity, equivalent
collection ordering, nondefault atomic rejection including malformed catalog-only
text, caller mutation/foreign ownership, old constructor behavior and repeated
lookup without pose rebinding. They do not assert a live state selector or FULL.

Initial ordinary build passes475/475 in2m24s; the unchanged default32 client
suite passes in4m54s. These runs started before the final constructor/version
review adjustments, so final-source verification and independent evidence are
still required before closure. No historic spatial proof is recaptured or used
to promote this preparation-only boundary into a physics/pose claim.

## Candidate verification and final review - ADVERSARY

**Funciona (PREPARATION only).** Independent public lane
`tools/s46_adversarial.init.gradle` executes five reusable oracles in
`S46AdversarialPreparedVariantTests`; final run
`../s46-adversary-public-complete.log` passed **6/6 including the sentinel**,
BUILD SUCCESSFUL **1m20s**, with clean server exit. Exact source/log/test hashes
and reproducible commands are in
`docs/evidence/S46/adversarial-verification.json`; eight retained gates passed
with their complete outputs in `adversarial-ownership-gates.json`.

The oracles preserve all executable variants including a variant-only catalog;
verify the exact canonical selection, ambiguity and absent selected material
without default fallback; reconstruct small and real multipart canonical
bundles (incomplete publication remains unavailable, reordered completion
recovers identical binding IDs); exercise every independently variable identity
field including each filter field, collection permutations and malformed text;
reject foreign equal Binding ownership and incorrect default projection; reject
wrong-entity identity lookup; and preserve the exact accepted snapshot/bundle
following invalid executable or catalog-only nondefault candidates. Defensive
collection/map ownership and the4096/4097 index boundary pass. These are prepared
catalog/component assertions, not ordinary Minecraft family/pose acceptance.

**Sensitivity and classification.** Isolated shadow sources, with no production
edits, independently omitted pose channels from identity, introduced default
fallback after failed selection, and skipped identity/text validation of
catalog-only declarations. Each produced the expected semantic RED. Recipes and
source hashes are durable in the evidence JSON; temporary shadow sources/init
files were removed after capture. The first cost fixture incorrectly assumed
one bind per selection: the unchanged pre-S46 path validates and then prepares
poses. A second fixture placed the hot-window baseline before a legitimate wire
reconstruction. Both are INFRA_RED; only setup/window changed, and the oracle
requiring zero additional binding/evaluation/root work during lookup remained
intact. Their initial red logs are retained and not counted as product defects.

**Continuous cost/ownership recertification.** Classification is PREPARATION
plus immutable lookup. The final loop made300000 lookups with128 parameters of
1024 characters and recorded additionalBinds=0, evaluations=0, rootSamples=0.
The12 initial binds are two preparation passes (server and reconstruction), each
using the existing validation+binding phases for three rows. Elapsed time is a
diagnostic, not a throughput claim: parallel host load caused a scheduling
warning in the final server run. Source review confirms owned IdentityHashMap
keys avoid nested record hashing during lookup; the unchanged canonical selector
keeps its declared per-type matching cost, exact ID lookup has expected constant
map cost, and no codec/hash/reflection/compile/registry access occurs in lookup.
Tables are immutable and revision-owned, with no new cross-revision/world cache,
world enumeration or runtime caller. Maximum population testing concerns the
index, not4096 physical actors. Global stress/heap/GC and human acceptance remain
in G9. No model transform or renderer path changed, so this preparation frontier
does not require a new spatial snapshot.

**Final zero-change pass.** Production remains exactly the frozen three-owner
scope: new CollisionBindingIdentity/PreparedBindingIndex and the prepared
Snapshot extension in WorldAnatomyCatalog. Existing selection/default runtime,
pose/contact protocol, engines and renderer/kernel remain untouched. The new
owners are TRUSTED only for the tested version1 prepared identity/index and
transaction boundaries; previous foundations retain their scoped certification.
The implementer logs were independently inspected: source-final475/475 server
and default32 client passed before public test promotion; final integrated
fixture/artifact accounting remains the IMPLEMENTER closure responsibility.
This complete independent review requires no production or oracle change.
Live entity selectors, structural transitions, causal generations/replay and
original model agreement remain obligations of the immediate next frontier.
G6 task2 and tasks3-6, G7-G9 and FULL remain open. No ADVERSARY commit/push.

The final independent receipt check additionally verified all362 production/
resource hashes against the IMPLEMENTER receipt with zero mismatches. The real
JAR hash is a0651b897583e75097489cda112032c80cdde87e389930566d4cf06ea64aff65
(734 entries,1181602 bytes), includes both new prepared-owner classes and no
adversarial/implementer test classes or test manifest leakage. JSON records the
check; this supplements, rather than replaces, the executed component oracles.

## Integrated acceptance and closure - IMPLEMENTER

The final source build passes475/475 (1m7s), default client32 passes (4m15s),
and an independent IMPLEMENTER reproduction of the unchanged public adversarial
recipe passes6/6 including the sentinel (1m4s). The latter records300000 lookups
with additionalBinds=0/evaluations=0/rootSamples=0. After promotion of the public
test/runner, a normal integrated build again passes475/475 with clean exit;
the GameTest phase took23.96s. That invocation reports1h31m41s total, with a
9:51:39-11:19:08 gap during original server startup before recipes/test execution.
The cause of this host/startup gap was not established; it is an uncontrolled
wall-clock observation, not a candidate throughput or runtime performance result.
Earlier final-source and independent runs passed at their recorded normal times.

Default processing was explicitly rerun after the opt-in lane and succeeded in2s.
Both processed manifests match the normal source:104 server entrypoints and
32 unchanged client entrypoints. The independent five-oracle/sentinel lane stays
explicitly opt-in through the reusable runner. Production/resource accounting
is362 files, with only WorldAnatomyCatalog changed from S45, two new owners and
zero removals. All362 hashes and the734-entry/1181602-byte real JAR are recorded
in implementation-verification.json; artifact SHA-256 is
a0651b897583e75097489cda112032c80cdde87e389930566d4cf06ea64aff65.
Source and public test/tool hashes match independent acceptance; no test classes,
test manifest or retired platform physics leaked into the product artifact.

The complete final IMPLEMENTER review rechecks normative scope, canonical
selection ownership, prepared object/content identity, immutable tables,
transaction order/failure, compatibility constructors/default callers,
preparation/lookup cost, tests/fixtures/runner and durable documentation. It
produces zero production, test or evidence changes. ADVERSARY's independent
zero-change pass is above; the new owners are TRUSTED only for these version1
prepared selection/identity/index boundaries. The closure has its own local
Sprint:S46 commit, followed by HEAD/tree verification; no push/tag/release.

G6 task2 remains open: the prepared index does not select the variant of a live
entity. The next frontier must connect exact server state to binding selection,
retire material/contacts/history on variant changes, carry complete accepted
selection identity to client consumers, and preserve original model agreement.
Families/FULL, G7-G9 and owner-only acceptance remain open for the whole epic.
