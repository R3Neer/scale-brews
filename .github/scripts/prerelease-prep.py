from pathlib import Path
import re

OLD_VERSION = "0.1.0-beta.6"
VERSION = "0.1.0-beta.7"
DATE = "2026-09-14"

props_path = Path("gradle.properties")
props = props_path.read_text(encoding="utf-8")
match = re.search(r"(?m)^version=(.+)$", props)
if match is None:
    raise SystemExit("Missing version in gradle.properties")
if match.group(1) not in {OLD_VERSION, VERSION}:
    raise SystemExit(f"Unexpected current version: {match.group(1)}")
props = re.sub(r"(?m)^version=.+$", f"version={VERSION}", props, count=1)
props_path.write_text(props, encoding="utf-8")

entry = f"""## [{VERSION}] - {DATE}

This prerelease makes Tiny Mount interaction and equipment data-driven through three vanilla-style behavior families, synchronizes saddles and riders with final animated mount models, and polishes wolf riding. It also adds size-scaled gait/footstep presentation and updates interaction-reach balance. The experimental all-direction anatomical collision system remains unfinished and disabled in normal gameplay.

### Tiny Mount behavior families

- Replace independent `saddle` / `control` JSON switches with `family`: `direct`, `tameable_direct`, or `item_steered`. The codec rejects contradictory combinations while movement and abilities remain independent.
- Chicken uses `direct`: it can carry a passenger unsaddled, needs a saddle for direct control, and exposes its mount equipment menu. Bee uses `item_steered`: it needs a saddle even to mount, needs Flower on a Stick to control, has no mount menu, and E opens the player's normal inventory. Wolf uses `tameable_direct`: native taming gates equipment/control and its menu exposes SADDLE plus configured BODY equipment.
- Remove species-hardcoded inventory routing. Inventory-bearing families open their menus dynamically; `direct` mounts also support Crouch + Use from outside, while `tameable_direct` keeps Crouch + Use as the wolf mounting gesture and uses E for equipment while mounted.
- Preserve vanilla equipment semantics: SADDLE/BODY are real equipment slots, shearability comes from each item's `Equippable` component, vanilla BODY-before-SADDLE shearing order remains intact, wolf owner-only shearing remains authoritative, and vanilla dispensers can saddle configured Tiny Mount species.
- Custom Tiny Mount datapacks using the old `saddle` and `control` fields must migrate to `family`; `item_steered` definitions still require `steering_item`.

### Wolf mount polish

- Add a horse-style equipment menu backed by the wolf's native SADDLE and BODY slots, including configured Wolf Armor and owner protection for BODY management.
- Set controlled wolf travel speed to 0.20 and make pounce charging progressively slow ground movement to 50% at full charge, preserving the established pounce travel envelope instead of turning the wolf into a better long-distance horse.
- Add squash/stretch, a short FOV kick, damped camera impact feedback, gravity-aware landing dust and concise audiovisual landing feedback without changing server pounce physics.

### Animated saddles and riders

- Capture the mount model's final rendered attachment transform after vanilla `setupAnim` and optional model animation, then share that same transform with saddle and rider rendering. This removes separate pose reconstruction and covers Tiny Mounts plus native horse body animation.
- Validate the path against the exact VanillaPlus stack of EMF 3.3.5, ETF 7.2 and Fresh Animations 1.10.5: Fresh Animations produces an active EMF bee model and the mounted rider receives a finite, non-identity final attachment transform. These remain optional dependencies.

### Size-scaled gait and interaction reach

- Scale ordinary footstep pitch with `clamp(1 / sqrt(scale), 0.5, 2.0)` and walk-animation time with `clamp(1 / scale, 0.25, 4.0)`, following effective physical scale while leaving vanilla cadence/amplitude ownership intact.
- Growth block reach remains +20% per equivalent level (5.4 / 6.3 / 7.2 blocks), while entity reach is +30% per equivalent level (3.9 / 4.8 / 5.7) and continues the same curve beyond Growth III. Shrinking block/entity reach both decrease by 10% per equivalent level.

### Validation

- Add adversarial family tests using a data-only Cow to prove mounting, control, inventory and dispenser behavior come from decoded family data rather than species branches.
- Exercise real client/server inventory routing, external `direct` menu interaction, vanilla saddle shearing/drop behavior, wolf BODY-before-SADDLE shearing, owner protection, horse rider animation and the pinned EMF/Fresh Animations runtime proof.
"""

changelog_path = Path("CHANGELOG.md")
changelog = changelog_path.read_text(encoding="utf-8")
section_header = f"## [{VERSION}]"
if section_header not in changelog:
    prefix = "# Changelog\n\n"
    if not changelog.startswith(prefix):
        raise SystemExit("Unexpected CHANGELOG.md header")
    changelog = prefix + entry + "\n" + changelog[len(prefix):]
    changelog_path.write_text(changelog, encoding="utf-8")

notes_path = Path(".github/scripts/compose-prerelease-notes.py")
notes = notes_path.read_text(encoding="utf-8")
old_heading = "## Maintenance refresh\n\n"
install_heading = "\n## Installation\n"
if old_heading in notes:
    start = notes.index(old_heading)
    end = notes.index(install_heading, start)
    replacement = """## Release integrity

The prerelease publisher tags the exact validated source snapshot `{sha[:7]}` and
verifies that the uploaded regular JAR matches the locally built SHA-256 before
publication completes.
"""
    notes = notes[:start] + replacement + notes[end:]
    notes_path.write_text(notes, encoding="utf-8")
elif "## Release integrity" not in notes:
    raise SystemExit("Could not locate prerelease-notes integrity section")

# One-shot release preparation: the workflow commits the resulting tree and removes
# this helper so ordinary development is not left with release-only automation.
Path(__file__).unlink()
