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

The JAR is a local development build with the unchanged beta.8 metadata, not a
new published version. No profile installation, tag, release or push was performed.
