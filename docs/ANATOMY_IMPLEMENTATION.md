# Anatomical collision implementation status

This is an **unfinished prototype** for the approved shared Scale Brews / Clinging plan. `PlatformPhysics` now delegates movement and transport to the new core only in explicitly activated proof levels. Ordinary worlds, `automatic_top`, Clinging's AABB collision and installed JARs remain unchanged. The existing 0.85 policy is untouched. Gate 1 is not complete.

## Implemented foundations

### 2026-09-09: restored checkout and client physics identity

The canonical checkout is now `D:/Minecraft/Mods/scale-brews`. Following source
reconstruction, only the reviewed anatomical changes were merged from the isolated
proof copy; installed artifacts were not replaced.

The client binds received, server-confirmed geometry to the shared physical query.
This exposed an integrated-server bug: vanilla `Entity.equals/hashCode` use the
network ID, so a client entity overwrote its server counterpart's provider in the
global weak map. Core entity state now uses weak **identity** keys. Merely waiting
longer for the unsupported-pose packet still failed before this correction.

The real-client fixture passed at 16:08:41 and exited cleanly at 16:08:42: 320
original pose comparisons, catalog/pose transfer, client occupancy queries and
removal of unsupported geometry. A dedicated same-ID regression additionally
checks independent gravity state and provider ticking. This is not yet moving
contact prediction or network reconciliation; those acceptance gates remain open.
The merged checkout's isolated build subsequently passed all 115 required server
GameTests at 17:21:44, including the same-ID regression.

At 17:24:37 the expanded isolated suite passed 116 server tests. The added
original-cow fixture executes 200 deterministic transport steps with translation,
yaw, changing scale and animated leg/head channels. It checks retained back
contact, idempotent carry and independent player movement at every step. This
exercises actual Entity movement and exported geometry, but deliberately does not
claim 200 separate server ticks: automatic tick ordering, continuous intermediate
contacts and multiplayer transport still require their own fixtures.

The real-client fixture now also places an ordinary scaled pig on the exported
cow back, scripts native `Entity.move` at the start of each level tick, and observes at least 60 actual
integrated-server ticks. It checks support throughout the observation window and
requires measurable horizontal travel and ascent. It does not manually call carry
or sample/tick the runtime provider during that window. Player prediction, occupied
boats, gravity changes and dedicated latency remain separate unproven cases.
The initial velocity-only fixture was invalid: its no-AI cow and supported body
remained stationary. A displacement assertion exposed this at 17:29:51; its earlier
contact-only pass is not counted as evidence of moving transport.
The corrected native-movement fixture passed at 17:32:01 and exited cleanly at
17:32:02, including the minimum-travel assertions, 60 server ticks, subsequent
unsupported-pose removal and the existing original-model/network checks.

- Original vanilla `ModelPart` and Alex's Mobs Continued 2.1.9 `AdvancedModelBox` extraction, preserving hierarchy, separate boxes, inflation, rotations, and inherited/non-inherited scale. No whole-model solid envelope.
- Stable piece paths, including synthetic scale-inheritance nodes whose IDs remain stable when animation changes inheritance.
- Player outer skin layers and the grizzly's cosmetic hat/microphone excluded in the proof. Per-piece local thickness/aspect/relative-volume decisions are exported for inspection.
- Server-safe immutable geometry DTOs and transactional catalog snapshots. Invalid references reject the entire candidate. This is not yet the synced Fabric registry or datapack reload integration.
- Versioned server-to-client catalog transfer: bounded 16 KiB fragments, 16 MiB total limit, per-server epoch, monotonic revision, SHA-256 verification and atomic acceptance. Receivers reset on disconnect/join. No serverbound geometry receiver exists. Automatic registry/reload publication and contact synchronization remain pending.
- Server-safe ordinary walking/head pose formulas for cow, adult grizzly, and empty-handed standing wide/slim players. Other poses are unsupported. An authoritative 20 Hz locomotion tracker and pose payload now exist. Two-sample interpolation rejects replay and resets at discontinuities; client histories clear on world-instance/catalog/connection changes and expire after 100 client ticks without a fresh sample. Automatic provider registration and live movement/presentation binding remain pending.
- Static affine convex SAT, exact translation sweeps, and a conservative temporal sweep accepting a verified material-point speed bound. Budget exhaustion is an explicit fail-closed result. Motion bounds for real entity hierarchies and gameplay resolution are still pending.
- `HierarchyMotion` now builds conservative material-point speed bounds recursively from joint translation, shortest-arc quaternion rotation and scale interpolation. Static affine/sheared nodes remain convex; animated shear is explicitly rejected pending a compatible evaluator. `ModelGeometryProvider.motionBetween` exposes this trajectory between supported authoritative endpoint poses. `sampleInterpolated` and the shared client geometry query now use that same trajectory, cached by support, endpoint pair and sample time. Rendering hooks and gameplay CCD still need to consume it.
- Explicit proof-level `AnatomyMovement`: real Entity.move collision/sliding, gravity-relative support, block rechecks, material-point transport and passive contribution accounting. Simple six-face and occupied-boat transport fixtures exercise the actual movement hook. This still lacks live animated swept resolution, network reconciliation and the full lifecycle/category migration.
- Geometry providers now expose tick-stamped motion snapshots. Model providers capture consecutive authoritative frames once per tick. `AnatomyMovement.sweep` queries local eligible supports, then uses conservative swept-piece bounds and temporal convex queries, preserving explicit iteration-limit results. Per-level/tick counters record queries, pieces, evaluations and exhausted budgets.
- Proof-level `collide` now uses event-based `TemporalResponse`, rechecking block/entity clipping after each changed trajectory. Verified constant translation is separated from deformation speed, allowing exact relative sweeps without redundant shared motion. Rising/falling contact response preserves independent tangent movement. Curved/simultaneous contact continuation and overlap separation still need refinement; budget exhaustion returns only the processed displacement and is recorded, not treated as completion.
- Initial/final anatomical overlap now invokes a bounded shortest-candidate SAT separation search (128 candidates, maximum 4 blocks), checking clipping and preventing entry into previously separate pieces. Unresolvable overlaps release contact and suspend only the affected body/support pair; the pair re-enables once actual overlap ends. This is a bounded candidate search, not a proof of globally minimal separation for arbitrary unions. Sustained animated squeezing and lifecycle/network recovery still require broader fixtures.
- Authoritative pose protocol v2 includes cardinal gravity. Model evaluation applies that gravity basis before body yaw/model transforms; caches distinguish gravity frames. A cardinal change resets physical interpolation rather than sweeping through arbitrary intermediate directions. Gravity Changer retains its visual/camera rotation. Full contact invalidation/reconciliation on support gravity changes remains pending.

## Reproduce the isolated proof

Java 25 and the project's Gradle dependencies are required. Run from PowerShell:

```powershell
.\tools\PrepareAnatomyProof.ps1 `
  -PackMods 'C:\path\to\instance\mods' `
  -OutputDirectory 'C:\new\anatomy-proof-directory' `
  -JavaHome 'C:\path\to\jdk-25'
```

Optional `-GradleUserHome` selects an existing dependency cache; `-Offline` prevents Gradle network dependency resolution. Minecraft may still perform its ordinary service requests.

The output directory must not exist. The script copies source into a fresh checkout and reads only the actual Alex's Mobs, CodxLib and Cloth Config JARs into a preparation environment. It does not import worlds, settings, resource packs, EMF, NEA or First Person. `inputs.json` records versions and JAR hashes; `source-hashes.json` identifies the source snapshot.

The client fixture exports cow, both player variants and grizzly, compares original transformed vertices, and compares 80 ordinary animated poses per variant. A separate dedicated GameTest process loads those JSON files **without Alex's Mobs or any client model classes** and evaluates the common pose providers.

Only after both commands succeed are results copied to `verified-proof-catalog` and `filter-report`. These are experimental JSON DTOs, **not an installable gameplay datapack**. No world/catalog installation occurs. This command intentionally does not claim full VP26 species coverage.

## Evidence (2026-09-06)

- Isolated original-model reference proof: cow 240 vertices, wide/slim players 144 vertices each, grizzly 360 vertices before cosmetic filtering.
- 320 animated pose comparisons passed against the actual client models: 80 each for cow, wide player, slim player and grizzly.
- The complete preparation command passed against the installed VP26 JARs, including independent dedicated catalog evaluation and 90 required server tests on its immutable source snapshot.
- A subsequent build and all 91 dedicated GameTests passed after adding continuous rotation/ascent queries and fail-closed iteration-budget tests. This still tests the query kernel, not live entity transport.
- The next build and all 93 dedicated GameTests passed with real six-direction Entity.move contact and an occupied boat following translation/rotation exactly once. The six-direction fixture supplies a gravity frame directly; it does not yet prove Gravity Changer or Clinging integration. Global noCollision remains unchanged in that fixture.
- Build and all 95 dedicated GameTests subsequently passed with six-sided Entity.move against the exported original cow, its open limb gap, unsupported-pose removal, shared-query caching and a server locomotion clock independent of query count. The real model exposed and fixed a SAT normalization defect for nearly parallel axes at distant coordinates.
- Catalog format 2 now carries the renderer's model transform. The isolated client invokes the original renderer scale hooks: cow/grizzly retain scale 1, both player models supply 0.9375. The 320 pose comparisons still pass. Build and all 96 dedicated GameTests passed, including an occupied boat landing on and moving with the original exported player head at its actual model height.
- Build and all 97 dedicated GameTests passed after introducing validated SurfaceContact identity, true affine face normals and ray queries, cardinal-normal ambiguity rejection, and material-face anchors for transport. Shared maps are protected for client/server coexistence and valid current supports win equal-time contact ties. These queries are ready for the Clinging adapter, which is not wired yet.
- The isolated real-client proof sent all four exported models through Minecraft's actual play connection and confirmed exact server/client catalog equality. Existing 320 pose comparisons still pass. This validates transfer, not automatic host migration or synchronized live support.
- Build and 98 dedicated GameTests passed, including out-of-order fragments, duplicate delivery, corruption rejection without replacing accepted data, stale-revision rejection and recovery with a later valid catalog.
- Build and 99 dedicated GameTests passed with authoritative pose-history replay, identity, discontinuity and non-mutating interpolation checks. A real play-connection round trip also preserved pose channels, entity identity, world transform and the catalog revision.
- The subsequent real-client proof reconstructed world-space convex pieces directly from the received pose and compared every vertex against the authoritative transform. One hundred repeated queries reused one evaluation; the existing 320 original-model animated comparisons still passed. This verifies the explicit frame evaluator, not automatic live collision/render binding.
- All 100 dedicated GameTests passed after adding hierarchical motion. The rotating/scaling articulated fixture detects an intermediate collision missed by both endpoints. Finite-difference checks cover every vertex on the four exported models under simultaneous walking, head movement, root rotation, translation and growth; these supplement the analytic bound, not replace it. Gameplay motion still uses the earlier static query until temporal resolution is connected.
- The subsequent isolated client proof passed shared-query reconstruction and verified that interpolated presentation geometry matches the physical joint trajectory vertex-for-vertex, reusing the evaluation across 100 observer queries. This is API evidence, not an assertion that vanilla renderers or Clinging already consume it.
- Build and 101 dedicated GameTests passed with the local spatial temporal-query fixture: an eligible real player/cow pair detects a moving limb whose endpoint boxes both miss, propagates iteration exhaustion, and records query work. This validates candidate lookup and temporal detection, not the still-pending moving-contact response.
- Build and 103 dedicated GameTests passed after connecting temporal response to real Entity.move in proof levels. Tests cover rising support with independent lateral movement, descending relative landing, coincident shared translation, explicit initial overlap and actual player movement ending grounded on the material face. Existing six-direction, cow anatomy and occupied-boat regressions passed. This is not yet proof of sustained articulated contact under Gravity Changer or network latency.
- 2026-09-07: build and 105 dedicated GameTests passed. Separation fixtures cover nearest exit, a blocked nearest exit with another valid route, fully obstructed recovery and distance limits. A real entity fixture verifies pair-specific suspension, no large recovery repositioning and re-enabling after overlap ends. Earlier movement/geometry regressions remain green.
- 2026-09-07 00:10:12: all 106 dedicated GameTests and build passed with the actual Gravity Changer 1.5.2-beta.5 and Cloth Config JARs in a separate proof environment. The explicit test adapter reads real gravity attributes in all six directions and compares coordinate conversion against upstream RotationUtil. The first run exposed a test-only signed-zero equality assertion, corrected to geometric distance. Production Clinging registration and gravity-oriented support geometry remain pending.
- 2026-09-07: all 107 dedicated GameTests passed with Gravity Changer after adding gravity-oriented model transforms and resetting interpolation across cardinal changes. The real-client proof then passed pose-v2 transfer with EAST gravity and reconstructed the oriented player geometry from server data; all 320 original-model pose comparisons still passed. Clinging production registration, gravity-change contact lifecycle and sustained multiplayer contact remain unverified.
- Earlier broad Alex client-suite execution failed in the unrelated wolf input proof; the anatomical fixture passed. Focused anatomical evidence does not replace the full final client/pack suite.
- No installed artifact changed. No new gameplay compatibility is claimed from these results.

## Remaining gate and migration work

### Registry/runtime progress — 2026-09-07

`scalebrews:entity_geometry` is a synchronized Fabric dynamic registry using the
validated format-2 export JSON. Put geometry at
`data/<namespace>/scalebrews/entity_geometry/<name>.json`. The existing species
profile path accepts `anatomy` instead of `surfaces`, for example:

```json
{
  "entity": "minecraft:cow",
  "enabled": true,
  "friction": 0.6,
  "max_width_ratio": 0.85,
  "anatomy": {
    "model": "mypack:alternative_cow",
    "pose_provider": "scalebrews:static",
    "filter": {
      "min_thickness": 0.03125,
      "min_aspect": 0.025,
      "min_volume_ratio": 0.00001,
      "include": [],
      "exclude": []
    }
  }
}
```

`static` is an explicit world-author choice, never a missing-animation fallback.
Selecting both anatomy and legacy planes is rejected. Piece/part selections are
validated against the model; inclusion bypasses numeric filters but not degeneracy
or equipment/cosmetic roles. Conflicting include/exclude IDs fail validation.

`WorldAnatomyCatalog` validates geometry and species bindings together before
publishing a snapshot. Invalid references/providers, incompatible model versions,
unknown selected pieces or duplicate species retain the accepted snapshot. Runtime
reload now reads the server ResourceManager directly, not the startup RegistryAccess.
Catalog protocol v2 transfers the geometry AND profiles as one bounded, hashed bundle;
the client validates both before replacing its accepted revision and policy index.
Bundles exceeding the network-size limit are rejected before server publication.
The dedicated resource-reader/transfer tests prove atomic rejection and consistency;
full live `/reload` while entities are supported remains an acceptance test to run.

`AnatomyRuntime` owns automatic server bindings, pose ticking, tracked-player
publication and lifecycle cleanup. Activation remains preparation-only, through
`scalebrews.anatomyRuntime` or the server-side preparation harness. Common pose
providers have separate state guards. The Alex 2.1.9 grizzly guard rejects sit/stand
transitions, eating, special animations and Freddy without renderer dependencies.

Evidence: 110 dedicated tests/build passed at 00:48:50 for codecs, registry presence,
alternative bindings and atomic rejection. The real-client proof passed at 00:57:00:
automatic tracked-cow publication and disappearance of its geometry on becoming a
baby, plus the existing 320 reference-pose comparisons and actual network round
trips; client exited cleanly. At 01:01:23 build and 111 dedicated tests passed with
actual Alex 2.1.9, including transitional pose guards. The first guard run exposed
Alex's null initial animation state; this was corrected from actual source and the
reference-model behavior before rerunning.

At 01:11:20 build and all 112 dedicated tests passed with actual Alex 2.1.9. The new
fixture reads real Resource wrappers through ResourceManager, loads geometry and
filtered policies together, transfers them through protocol v2, and rejects a later
missing-model reference without changing either accepted component. This is not yet
the complete live command/transport lifecycle test.

At 01:14:07 the real-client fixture passed protocol-v2 replacement of a prior
geometry-only catalog by a higher revision containing the actual species policy.
The client policy index, model binding, automatic pose publication and unsupported
state removal agreed; all 320 original-model pose comparisons remained green and
the client exited cleanly. This does not yet validate replacement while carrying a
player or reconnecting under artificial latency.

Still unproven: live client collision prediction, sustained articulated contact
under the runtime tick order, complete hot reload, host replacement and the full
acceptance matrix. Installed profiles and JARs have not been replaced.

### Tangential response and gravity-local locomotion follow-up

Hierarchy motion now certifies invariant cardinal separating planes only when
every node preserves the corresponding scalar projection throughout interpolation.
These certificates permit tangential motion on yawing supports without repeatedly
resolving the same zero-time contact. Subintervals translate the certificate by
the declared root movement. Pitch, changing scale and unproven projections do
not receive the optimization; unresolved curved contacts still fail closed at the
explicit iteration budget. This is not a general solution to articulated sliding.

The isolated build with Alex's Mobs 2.1.9 passed all 113 dedicated tests at
01:26:09 on 2026-09-07, including yaw/ascent, relative landing, subinterval plane
translation and rejection of pitch/scale certificates. This numerical response
fixture does not replace sustained entity/vehicle transport acceptance.

Authoritative walking now measures displacement in the gravity-tangent plane,
not world X/Z, and resets its clock on cardinal gravity changes. Passive transport
subtraction remains in place. A dedicated regression exercises all six frames.
The subsequent isolated build passed all 114 required dedicated tests at 01:28:01,
including this six-frame regression. Client and sustained articulated transport
validation remain pending for these changes.

### Vehicle seats and server movement integration

Root transport now refreshes the vanilla passenger seats in parent-first order,
including nested passenger trees. It does not create passenger contacts, invoke
passenger movement physics or add separate passenger transport. The occupied-boat
fixture now checks actual rider displacement and an idempotent second carry,
rather than merely checking that the mounting relationship survives. Its isolated
build passed on 2026-09-07 at 01:31:41.

The anatomical path also forwards confirmed root displacement to the existing
player/controlled-vehicle movement-baseline hook and routes physical-touch queries
to anatomical contact. These connections are necessary but are not evidence of
completed packet reconciliation, anti-flight latency acceptance or prediction.

Live Clinging audit on 2026-09-07 subsequently found concurrent alpha.5 development. Its `EXPANSION_ALPHA5_ES.md` supersedes the older Crouch input: preserve jump-key input, limited airborne Clinging use, Reorientation, mounted-root controls, landing recharge and First Person correction. Do not revert these unrelated changes to the alpha.4 snapshot.

The optional Clinging bridge now registers the real Gravity Changer adapter and delegates anatomical clearance, contact and grounding to Scale. In proof-active worlds its legacy AABB injection, carry, outgoing relative reference, reconciliation and inherited jump impulse are bypassed. Mobs' oriented-space preflight also queries the shared geometry. Older Scale artifacts still take the legacy path. This is not final protocol negotiation or a completed migration of the installed artifacts.

At 00:35:22 the isolated coordinated dedicated fixture passed managed-Clinging contact on all six cardinal faces, upstream gravity lookup, anatomical-hole clearance, shared support identity, absence of second carry and jump release without stale legacy impulse. The complete Clinging suite was **not green**: its separate alpha.5 effect-refresh/forged-ground assertion failed. This evidence uses a synthetic convex body to isolate the bridge, not the full original-model/animation acceptance matrix. At 00:37:52 Scale build and all 108 dedicated tests passed, including release of material anchors when either body's or support's cardinal gravity changes without replaying old transport. No installed JAR changed.

The subsequent isolated Clinging build and all 29 dedicated tests passed at 00:38:54 after adding mob anatomical-space preflight and improving the effect-refresh assertion's diagnostic. The earlier failure is retained above; this successful rerun alone does not establish its root cause or prove repeatability. This Clinging run used the previous coordinated Scale proof JAR, before the separately validated gravity-anchor invalidation change.

1. Authoritative pose sampling, interpolation, root/model/world transforms and conservative motion bounds, including gravity orientation and external scale.
   - Common pose tick tracking, cached model evaluation and explicit evaluation at received/interpolated world frames now exist. Ordinary cow/player/grizzly root scale hooks are verified and exported as data; other renderer families and exceptional root rotations remain unsupported until verified. Pose packets and interpolation exist, but automatic publication, animated sweep integration and binding to live presentation/physics remain pending.
2. Real six-sided contact/resolution/transport with player, mob and occupied boat on a dedicated server. Verify animation vs contact at feet, belly, head, sides and limb gaps, not just numerical pose equality.
3. Prove resource-pack and dynamic-host independence in runtime; preparation isolation alone is not that full proof.
4. Synced geometry registry, versioned catalog/profile codecs, JSON substitutions, legacy one-sided reader, reload lifecycle, late tracking and protocol negotiation.
5. Replace horizontal physics and calibrations while retaining categories, friction, edge protection, jump, chains, vehicle/item/falling-block handling, placement, camera and transport accounting.
6. Migrate Clinging selection, oriented-box preflight and gravity adapter; remove its duplicate AABB collider/carry/reconciliation only when the shared replacements pass regression tests.
7. Broaden extractors/providers and report rejected/unsupported species and poses across VP26; verify all acceptance scenarios, latency/soak tests and coordinated builds before installation.

The prototype has not loaded any renderer in a server, enabled a new bounding-box fallback, altered pathfinding, or changed ordinary-world player movement. Activation remains restricted to proof fixtures until the remaining Gate 1 evidence exists.
