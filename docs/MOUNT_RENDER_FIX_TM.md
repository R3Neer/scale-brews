# Mount render and interaction corrections — iterative TM

## Analysis

Baseline: beta.8 (`7a9247d`). Work takes place in an isolated local clone; the
installed profile and other working trees are not changed.

Confirmed source defects: surface selection uses the first render's world
orientation and animated pose; the cached reference is reset abruptly above 60
degrees; camera samples accept inventory submissions; rider correction discards
the render origin offset supplied by other renderers. Legacy anchor hints are
ignored. Wolf secondary use currently mounts instead of opening inventory.

Not yet established by reproduction: the exact source of the black paperdoll,
all EMF-specific flicker, and the reported hearts. WolfTaming already returns
early for tame wolves, so hearts must not be attributed to repeated taming
without evidence. FirstPerson 2.7.2 offsets the extracted entity temporarily.

## Architectural plan R

Keep attachment geometry in canonical model space, independent of camera/world
orientation. Resolve group anchors through their descendants. Use immutable rest
reference transforms and preserve animated transforms at submission. Exchange
world seats only for render states belonging to the current world frame; GUI
renders must remain independent. Preserve first-person extraction displacement
when moving a rider to a seat. Test before introducing any optional-mod adapter.

For tamed wolves only: secondary use opens inventory; ordinary empty-hand use
mounts a saddled wolf, and remains vanilla sit/stand without a saddle. Preserve
feeding, repair, dyeing and other item interactions. Wild ride-taming and other
configured tameables retain their previous semantics.

Architecture review 1: checked client/server ownership and backwards-compatible
wild-wolf behaviour; no changes. Review 2: checked GUI isolation, non-player
riders, resource reload and optional-mod absence; no changes.

## Implementation plan R

- [x] Stable model-space selection, legacy/group anchors, remove abrupt rebase.
- [x] Strict world-frame membership and camera capture isolation.
- [x] Preserve extracted rider displacement; investigate first-person alignment.
- [x] Wolf gestures and regression tests, including no item consumption on mount.
- [x] Integrate the approved hand-drawn wolf armor slot with reproducible source.
- [x] Expand synthetic and live bee/chicken/wolf tests; build and run suites.
- [x] Investigate GUI/EMF failures with installed versions; retain non-reproduction limits below.
- [x] Implementation R: two unchanged reviews after fixes.
- [x] Tests R-prime: validate failing tests before returning to implementation.
- [x] Adversarial R-prime and final explicit automated/manual QA boundary.

Plan review 1: requirements mapped to implementation and verification; no changes.
Plan review 2: tests distinguish synthetic math, server interactions and rendered
pixels; no changes. Publication, installation and release are not part of this plan.

## Iteration record

Exploratory tests and review returned to the architecture: FirstPerson must use
the camera's actually applied (smoothed and collision-clipped) displacement,
not the unbounded third-person seat target. Its temporary extraction displacement
must also be scaled to the player's model. This is an optional 2.7 API bridge;
ordinary rendering and native horses do not enter that branch. The body remains
upright with the translation-only camera. Camera roll/gravity integrations outside
this compatibility stack are not newly guaranteed.

Revised architecture review 1: checked extraction versus submission timing and
camera/body coordinates; no changes. Review 2: checked GUI and absent/incompatible
optional-mod behaviour; no changes. Revised plan reviews 1 and 2: retain the same
steps, add actual first-person frame observation to client acceptance; no further
changes in either review.

Implementation review found and corrected missing reset of interpolation on
resource reload and lack of finally cleanup on failed world submission. The
optional reflection bridge now tolerates missing API classes/methods and an
uninitialized core. Follow-up implementation review 1: canonical selection,
immutable calibration, GUI membership, world/reload lifetimes and API fallback
checked; no changes. Review 2: server-only mutations, owner-protected BODY slots,
wild taming, native mounts and documentation checked; no changes.

Exploratory test issue: the new server fixture called protected Player.closeContainer;
the fixture was corrected before judging production behaviour (R-prime).
Old Shift-to-mount wolf assertions were updated to the approved new contract;
the configured-cat and wild-wolf contracts were retained.

## Evidence so far

- Build and all 154 server GameTests passed.
- Base client TinyMountClientAcceptance passed; mounted bee paperdoll is visible.
- Compatibility acceptance passed with FirstPerson 2.7.2, EMF 3.3.5, ETF 7.2,
  Fresh Animations 1.10.5, FA Details 2.3 and FA Player 1.1.
- Actual submission probe: bee 200 frames / 126 first-person frames,
  chicken 205 / 130, wolf 195 / 125. Stable anchors, finite transforms,
  no greater-than-60-degree adjacent-frame saddle jumps in the sampled sequence.
- Actual bee anchor: root/bone/body/EMF_torso. Chicken:
  root/body/EMF_body/EMF_rotation. Wolf:
  root/body/EMF_body/EMF_body_rotation/EMF_mane2/EMF_mane_shake.
- Forward/down first-person screenshots show a centred body on all three mounts.
  Looking backwards on the unsteered bee retains native mounted torso yaw limits;
  it is not used as evidence that all viewpoints should have centred limbs.
- Armor icon is generated with `java tools/GenerateArt.java wolf-slot`; this writes
  only the approved 16x16, 34-pixel, opaque #7c7c7c slot design.

The reported original black paperdoll and heart particles were not reproduced
as distinct runtime failures here; do not claim their exact original cause was
proven. Empty-hand riding is tested not to enter breeding or wild taming. Feeding
can still legitimately produce vanilla hearts. Whole-pack human playtesting,
long sessions, arbitrary resource packs and multiplayer remain separate QA.

## Final R-prime closure

Adversarial review found that replacement parts with the same child names and
cube counts could reuse an obsolete cached chain. The reviewer was checked
against the fingerprint and a same-shape replacement fixture. Returned to
implementation: include part/cube identity, never animated poses, in the variant
fingerprint. The regression replaces the actual child and checks the new rest
position. Follow-up implementation reviews 1 and 2 found no further changes:
the cache now distinguishes replacement identity while ordinary animation does
not invalidate selection; GUI lifetime, world reset, optional API and server
interaction semantics remained consistent.

Final command (2026-09-16):

```powershell
.\gradlew.bat build runGameTest runClientGameTest --offline --console=plain '-PscalebrewsCompatMods=build/compat-mods' '-PscalebrewsGameTestReport=build/final-tests.xml'
```

Result: BUILD SUCCESSFUL, 154 server cases, zero failures/errors, all five client
entrypoints completed (AnatomyExportProof, WolfClientProof, PlatformClientProof,
ScaleBrewsClientTests, TinyMountClientAcceptance). Final real-frame sample:
bee 198/122, chicken 201/127, wolf 201/124 (total/FirstPerson). Tests include the
scaled FirstPerson origin under a rotated stack and a same-shape model replacement.
Packaged JAR contains the optional bridge and updated icon, and no test classes.
Pixel comparison with the approved icon: zero changed pixels, 34 opaque pixels.

Tests review 1: checked actual manifest, reports, enabled resource packs and real
frame observation rather than synthetic-only claims; no changes. Tests review 2:
checked gestures through real networked input and native horse isolation; no
changes. Repeated adversarial reviews 1 and 2: no further findings in the scoped
changes. This closes automated TM delivery, not whole-pack human acceptance.

The initial TM closure used a local development JAR with beta.8 metadata. A
separate, explicitly authorized publication step subsequently promotes the same
validated hotfix as beta.9; profile installation remains outside this work.

## Post-closure visual regression R-prime

The release was stopped before push, tag or publication when human review found
the player mounted incorrectly in third person. The compatibility screenshots
confirmed two production failures which the previous finite/stability probe did
not reject: Chicken inherited a 171.1-degree generated-model basis rotation, and
Wolf selected accessory geometry (`mane_shake`, later `head2`) below its explicit
`body` group. Vanilla captures remained correct, localizing the regression to the
EMF/Fresh Animations hierarchy rather than camera placement or rider scale.

R-prime first validated the reviewer: a real-frame test failed on the Chicken
tilt and a semantic anchor assertion failed on the Wolf accessory path. Returning
to architecture established that threshold correction and first-frame calibration
were invalid: both introduced state-dependent boundaries as EMF interpolated its
coordinate wrapper. The revised architecture instead chooses the nearest
non-accessory torso geometry and treats a terminal `EMF_*rotation` hierarchy as
a coordinate conversion. Its complete authored rest chain is baked into the seat;
internal EMF animation is isolated while entity position, yaw and external gravity
remain in the outer render transform. Bee `EMF_torso` animation is unaffected.

Architecture reviews 1 and 2 checked vanilla fallback, custom group anchors,
resource reloads and the intentional tradeoff of stable Chicken/Wolf seats over
Fresh Animations body bobbing; no changes. Implementation review found and removed
an obsolete calculated-basis helper. Follow-up implementation reviews 1 and 2
checked cache identity, root/rest composition, saddle/rider shared frames and the
Bee exclusion; no further changes.

Tests R-prime now require a stationary rider tilt below 45 degrees, reject
Wolf/Chicken accessory paths, and include oversized same-depth and deeper
accessories in the synthetic group-anchor fixture. The directed compatibility
acceptance passed with Chicken 200 frames at 0 degrees on
`root/body/EMF_body/EMF_rotation`, Wolf 203 frames at 0 degrees on
`root/body/EMF_body/EMF_body_rotation`, and Bee retaining its animated torso.
Manual rear, front and top capture review found both Chicken and Wolf upright,
centred and seated over the saddle. Directed vanilla acceptance also passed with
the ordinary `root/body` anchors unchanged.

Final post-regression gate (2026-09-16): `build`, 154/154 server GameTests with
zero failures/errors/skips, and all five client entrypoints passed with the full
compatibility stack. Final probes: Bee 204/132 frames at 6.37 degrees on
`EMF_torso`; Chicken 209/132 at 0 degrees on `EMF_rotation`; Wolf 197/127 at
0 degrees on `EMF_body_rotation` (total/FirstPerson). The packaged beta.9 JAR
contains no test classes and has local SHA-256
`2AB4362CBA70582FA463D9DB7EC2F7EA1235A7C217AE0CC5F128C96A27FAF181`.

Adversarial review 1 rechecked that the new assertions fail the captured old
frames and that the implementation, rather than a weakened test, makes them
pass; no changes. Review 2 checked the executed manifest, JAR boundary, version,
changelog and publication scope; no changes. This closes the reopened automated
TM cycle. Visual approval in arbitrary gameplay remains human QA.
