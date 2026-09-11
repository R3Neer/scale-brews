from pathlib import Path


def read(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    Path(path).write_text(text, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one match, found {count}: {old[:80]!r}")
    write(path, text.replace(old, new, 1))


def insert_after_once(path: str, marker: str, addition: str, guard: str) -> None:
    text = read(path)
    if guard in text:
        return
    count = text.count(marker)
    if count != 1:
        raise SystemExit(f"{path}: expected one insertion marker, found {count}: {marker!r}")
    write(path, text.replace(marker, marker + addition, 1))


replace_once(
    "gradle.properties",
    "version=0.1.0-beta.5",
    "version=0.1.0-beta.6",
)

replace_once(
    "README.md",
    "Tiny Mounts remain ordinary Minecraft mounts at the integration boundary. When a compatible gravity provider is present, Scale-generated bee flight, chicken glide and wolf pounce/landing use the root mount's effective gravity frame instead of assuming world-down; Scale still works normally with no gravity mod installed. See [effective-gravity integration](docs/GRAVITY_INTEGRATION.md).",
    "Tiny Mounts remain ordinary Minecraft mounts at the integration boundary. When a compatible gravity provider is present, Scale-generated bee flight, chicken glide and wolf pounce/landing use the root mount's effective gravity frame instead of assuming world-down. **Gravity Changer is the provider Scale Brews currently recognizes automatically**; with no provider installed, the frame falls back to vanilla `DOWN` and Tiny Mount behavior is unchanged. See [effective-gravity integration](docs/GRAVITY_INTEGRATION.md).",
)

insert_after_once(
    "docs/GRAVITY_INTEGRATION.md",
    "Scale Brews treats Tiny Mounts as ordinary Minecraft vehicle/passenger relationships. External mods do not need a Tiny-Mount-specific API to rotate or otherwise manage a mount: they operate on the root `LivingEntity`, and Scale-owned mount mechanics read that root's effective gravity when generating their own movement.\n",
    "\nThis integration is released for Tiny Mount movement starting with **0.1.0-beta.6**. It does not make the in-development anatomical collision system a released feature.\n",
    "starting with **0.1.0-beta.6**",
)

insert_after_once(
    "docs/GUIDE.md",
    "| Bee | Hold a Flower on a Stick and look where you want to fly. Remove the item to release manual control. |\n",
    "\nScale-generated bee flight, chicken glide and wolf pounce/landing use the **root mount's effective gravity frame** when a compatible provider is available. Gravity Changer is recognized automatically; no gravity mod is required, and without one the frame is vanilla `DOWN`. See [Effective-gravity integration](GRAVITY_INTEGRATION.md) for the ownership boundary and validation scope.\n",
    "Scale-generated bee flight, chicken glide and wolf pounce/landing use the **root mount's effective gravity frame**",
)

replace_once(
    "docs/GUIDE.md",
    "The current suite includes server GameTests plus a real client/integrated-server test covering camera, beacons, resources, synchronization, tiny-mount input, animated bee riders and unmounted steering-item attraction.",
    "The current GitHub Actions workflow runs server GameTests plus a real client/integrated-server GameTest under Xvfb, covering camera, beacons, resources, synchronization, tiny-mount input, animated bee riders and unmounted steering-item attraction.",
)

insert_after_once(
    "docs/GUIDE.md",
    "Both run with the base Fabric setup and were also tested with **Combatify 1.4.0-26.2** and **Alex's Mobs Continued 2.1.9**, including their required dependencies. Targeted checks cover weapon-dependent reach, attack knockback, tendon brewing, modded-mob landings, small-player corner collision and elytra eligibility.\n",
    "\n**Gravity Changer** is the concrete optional gravity provider currently recognized automatically. Six-frame Tiny Mount semantics are covered by provider-fixture regressions, while ordinary base CI verifies that the integration remains optional and vanilla-DOWN behavior still works without a gravity mod. See [Effective-gravity integration](GRAVITY_INTEGRATION.md).\n",
    "**Gravity Changer** is the concrete optional gravity provider currently recognized automatically.",
)

replace_once(
    "docs/VALIDATION.md",
    "GitHub Actions runs the build/server suite on Ubuntu with Java 25. The client visual check is local, not part of headless CI. Packaging checks ensure production JARs include sprites/languages/tags/mixins but exclude GameTest classes and test-only mixins. `git diff --check` checks patch whitespace.",
    "GitHub Actions runs both the build/server suite and the real client GameTest under Xvfb on Ubuntu with Java 25 for every push and pull request. Screenshot interpretation remains a human/local task, but client assertions now run in CI and CI uploads logs/screenshots for inspection. Packaging checks ensure production JARs include sprites/languages/tags/mixins but exclude GameTest classes and test-only mixins. `git diff --check` checks patch whitespace.",
)

insert_after_once(
    "docs/VALIDATION.md",
    "# Validation record\n",
    "\n## 0.1.0-beta.6 candidate — 2026-09-11\n\n- Candidate scope adds optional effective-gravity support for Scale-generated Tiny Mount movement: bee flight, chicken glide and wolf pounce/landing use the root mount's effective gravity frame rather than assuming world-down.\n- Gravity Changer is the concrete provider recognized automatically. It is not a hard dependency; without a provider, the shared frame service resolves to vanilla `DOWN` and the existing Tiny Mount behavior is retained.\n- Normal gameplay still uses the released upper-surface living-platform system. All-direction anatomical collisions and the shared Clinging Reoriented physics remain unfinished and disabled in ordinary worlds.\n- Prerelease publication is gated by `.github/workflows/prerelease.yml`: the exact release tree must pass `git diff --check`, build, every required server GameTest, the real client GameTest suite, and packaged-version checks before the workflow may create a tag or GitHub prerelease.\n- Full-pack human acceptance, broad optional-mod combinations and multiplayer latency/host-change coverage remain bounded separately; a passing prerelease lane does not certify every modpack.\n",
    "## 0.1.0-beta.6 candidate — 2026-09-11",
)

insert_after_once(
    "CHANGELOG.md",
    "# Changelog\n",
    "\n## [0.1.0-beta.6] - 2026-09-11\n\nThis prerelease adds optional effective-gravity support to Scale-owned Tiny Mount movement and moves the real-client GameTest into ordinary GitHub Actions validation. The all-direction anatomical collision system remains unfinished and disabled in normal gameplay.\n\n### Tiny Mount effective gravity\n\n- Bee flight, controlled chicken glide and wolf pounce/landing now generate their Scale-owned movement in the root mount's effective gravity frame instead of assuming world-down. Existing world-space momentum is not reinterpreted when the frame changes.\n- Centralize effective gravity behind one Scale-side service with vanilla `DOWN` fallback. Gravity Changer is discovered reflectively when present and remains an optional dependency.\n- Cover all six cardinal frames, root-versus-rider ownership, frame changes between actions and preservation of pre-existing momentum with dedicated regressions.\n\n### Validation and release engineering\n\n- Run both server and real-client GameTests in ordinary GitHub Actions CI. Client assertions execute headlessly under Xvfb; screenshots/logs are retained as workflow artifacts for inspection.\n- Add a guarded prerelease workflow that validates the exact release tree, checks packaged metadata and test-class exclusion, calculates the regular-JAR SHA-256 and creates the GitHub prerelease only after all gates pass.\n\n### Development prototype - anatomical geometry (not active gameplay)\n\n- Retain and extend the internal anatomical/contact synchronization and gravity-frame preparation without enabling it in normal worlds. The existing upper-surface living-platform system remains the released behavior.\n",
    "## [0.1.0-beta.6] - 2026-09-11",
)

insert_after_once(
    "TODO.md",
    "# Scale Brews — active work\n",
    "\n## Beta.6 prerelease delivery — 2026-09-11\n\n- [x] Audit `main` against beta.5 and current README claims; confirm the latest pre-release code snapshot has green server/client CI before changing publication metadata.\n- [x] Align README, player guide, gravity integration, validation record and changelog around released Tiny Mount effective-gravity scope versus the still-disabled anatomical prototype.\n- [x] Bump the project candidate to `0.1.0-beta.6` and define a guarded GitHub Actions prerelease lane that publishes only after exact-tree server/client validation and package checks.\n- [ ] Human acceptance of the resulting beta.6 JAR in the full pack/multiplayer remains separate from automated prerelease publication and does not complete the anatomical migration.\n",
    "## Beta.6 prerelease delivery — 2026-09-11",
)

# This file is deliberately one-shot. The reusable workflow remains in the tree.
Path(__file__).unlink()
