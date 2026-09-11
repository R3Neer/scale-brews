import os
import re
from pathlib import Path

version = os.environ["VERSION"]
sha = os.environ["RELEASE_SHA"]
server_tests = os.environ["SERVER_TESTS"]
regular_sha = os.environ["REGULAR_SHA"]

props = {}
for raw in Path("gradle.properties").read_text(encoding="utf-8").splitlines():
    if "=" in raw and not raw.lstrip().startswith("#"):
        key, value = raw.split("=", 1)
        props[key.strip()] = value.strip()

changelog = Path("CHANGELOG.md").read_text(encoding="utf-8")
header = re.compile(rf"^## \[{re.escape(version)}\](?:\s+-\s+[^\n]+)?\s*$", re.MULTILINE)
match = header.search(changelog)
if match is None:
    raise SystemExit(f"Missing changelog section for {version}")

section_start = match.end()
next_header = re.search(r"^## ", changelog[section_start:], re.MULTILINE)
section_end = section_start + next_header.start() if next_header else len(changelog)
section = changelog[section_start:section_end].strip()
if "\n### " in section:
    intro, changes = section.split("\n### ", 1)
    changes = "### " + changes.strip()
else:
    intro = section
    changes = section
intro = intro.strip()

notes = f"""# Scale Brews {version}

{intro}

## Explore first

The [README](https://github.com/R3Neer/scale-brews#readme) gives a few things to
try, with real in-game screenshots. Exact recipes, balance, mounts and world rules
live in the spoiler-rich [player guide](https://github.com/R3Neer/scale-brews/blob/main/docs/GUIDE.md).

## Changes since the previous prerelease

{changes}

## Maintenance refresh

This prerelease was rebuilt from `{sha[:7]}` after migrating the stable GameTests from
the deprecated `GameTestHelper.makeMockServerPlayerInLevel()` helper to Minecraft
26.2's explicit `makeMockServerPlayer(GameType)` API. CI now rejects any reintroduction
of the deprecated helper before building. The prerelease publisher also verifies that
the moved tag and the uploaded regular JAR both match this exact validated source
snapshot.

## Installation

Use `scalebrews-{version}.jar`, not the sources JAR. Requires Java 25,
Minecraft {props['minecraft_version']}, Fabric Loader {props['loader_version']}+ and
Fabric API {props['fabric_api_version']} or newer for Minecraft {props['minecraft_version']}.
Install on both client and server. Back up worlds before updating.

## Validation and limits

The exact source snapshot `{sha[:7]}` passed build, all **{server_tests} required server GameTests**
and the full real-client GameTest suite in this prerelease workflow on Ubuntu/Java 25.
The packaged regular JAR was also checked for the expected mod version and for leaked
GameTest classes.

Gravity Changer remains optional and is not bundled in the base release lane. Six-frame
Tiny Mount behavior is covered by the gravity-frame regression fixtures; absence of a
gravity provider retains vanilla-DOWN behavior.

**All-direction anatomical collisions and the shared Clinging physics remain unfinished
and disabled in normal gameplay.** Prototype code is included, but the existing
upper-surface living-platform system remains the released behavior.

Regular JAR SHA-256:
`{regular_sha}`
"""
Path("release-notes.md").write_text(notes, encoding="utf-8")
