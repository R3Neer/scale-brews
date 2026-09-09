# README and prerelease plan — 2026-09-09

## Iteration 1 — before editing

Objective: describe current playable behavior, including work from other tasks;
publish all reviewed repository work as a prerelease and install its exact JAR in
the existing VanillaPlus Modrinth instance. Do not present unfinished anatomical
physics as a released feature. No production world is used for screenshots.

### Audit and content contract

- Read current source, registries, resources, tests, changelog and existing docs.
- Keep README in English, matching the repository; concise player-facing overview
  followed by brewing, size, world mechanics, mounts, configuration and installation.
- Preserve exact potion endpoints, durations, ingredients, effective-scale rules,
  configured controls, wolf trust behavior and relative villager fear.
- Add a visible material-loot section: scale^1.6, probabilistic rounding, selected
  entity materials only, no equipment/XP, Looting composition and reloadable JSON.
- Explain current living surfaces and the 0.85 physical-width ratio separately from
  riding SCALE ratios. Mention spawn-egg/boat placement and pathfinding exclusion.
- Explain that anatomical all-direction collisions / new Clinging integration are
  experimental source in the repository, not active default gameplay.
- Distinguish restart-required world registries from reloadable material-loot rules.
- Link release downloads, detailed docs and explicit compatibility/QA limitations.

### Screenshot plan

1. Near the introduction: normal/grown/shrunken subjects in the same scene, with
   readable scale contrast and no diagnostic overlay.
2. Tiny mounts: saddled bee/chicken or wolf with rider, showing useful gameplay.
3. Optional living-surface view only if it clearly shows physical support rather
   than riding; do not illustrate experimental anatomy as released gameplay.

Capture fresh real game images in an isolated client; inspect every selected PNG.
Reject obstructed, ambiguous, debug-heavy or redundant images. Keep only 2–3 useful
images with relative paths and descriptive alt text. Do not generate artwork by AI.

### Editing and review loop

1. Implement the README and capture plan after this document exists.
2. Review every requirement against source and the rendered/readable result.
3. Record deviations below. Justified changes update the plan before another review;
   unjustified deviations return to editing. Do not call the review complete while
   images, claims or links remain unverified.

### Delivery gates

- Audit all dirty paths, retain unrelated business changes, exclude caches/secrets.
- Choose the next unused beta version after checking existing releases/tags.
- Build the exact source snapshot and run server and real-client tests. Keep the
  experimental runtime off in the normal prerelease; report broader QA honestly.
- Commit and push scoped groups covering all reviewed pending work; verify remote.
- Create a GitHub prerelease with regular and sources JARs and accurate notes.
- Verify public asset/version/commit and hashes; no claim that the entire anatomy
  implementation plan has been completed.
- Back up the instance's previous Scale Brews JAR outside mods, install the new
  regular JAR only, and verify there is exactly one Scale Brews mod and matching hash.

## Review log

- Initial audit: source confirms relative villager fear, persistent probabilistic
  wolf trust, Shrinking III 0.274 scale and material loot exponent 1.6.
- GitHub authentication restored and verified as R3Neer; no existing remote tags
  or releases were returned. Select 0.1.0-beta.5 after the installed local beta.4.

## Iteration 2 — review and justified plan adjustment

- Content review found ambiguous potion-icon wording in landing impacts. Corrected
  it to actual-size thresholds 1.96 and 3.88, verified against GrowthImpact and
  ScaleSize. Explicitly exclude the prototype from the default-enabled statement.
- Rejected first screenshots: subjects were too distant/poorly framed and removal
  particles obscured mounts. Recaptured front views, removed old subjects without
  death particles, hid HUD through the current Hud API, and inspected both PNGs.
- Adjust screenshot 2 to compare all three saddle-equipped species, without a
  rider: their equipment is the intended subject and none is obscured by a rider.
  Caption explicitly says saddle equipment, not a demonstration of steering.
- Omit optional platform image: two images explain distinct features without
  conflating riding with physical support or advertising experimental anatomy.

### Repeat review against revised plan

- Source-backed brewing/durations, scale endpoints, sprint composition, wolf
  trust/controls, relative fear and reloadable scale^1.6 materials retained.
- Platform width ratio distinguished from riding scale ratio; navigation and
  placement constraints stated; anatomy development warning is prominent.
- Both real images show unobstructed subjects with no HUD. Relative image paths,
  alt text, captions, reproduction command and artwork attribution added.
- No gameplay balance changes were introduced by this documentation request.
- Build/client validation, publication and installation remain delivery gates;
  do not infer their success from this editorial review.

## Iteration 3 — user-requested discovery-first README (before editing)

The user now requests a short, curiosity-driven README and a separate wiki-like
guide, then the same approach for the other public mods if it works well.

- Move the detailed player reference into docs/GUIDE.md, preserving source-backed
  information and repairing relative links. Add a navigable contents list and
  links to existing specialized reference pages rather than duplicate their data.
- Rewrite README as a short introduction, two useful screenshots, a minimal first
  brew, a few truthful experiments, installation and visible beta/compatibility
  limits. Hide exact tiers, loot/taming formulas and surprise interactions there.
- Do not hide installation requirements, non-obvious essential controls, risks or
  experimental status for the sake of surprise. Label the guide as spoiler-rich.
- Review README and guide together for lost information, excessive spoilers,
  invented promises, navigation and drift. Log corrections and repeat review.
- After this pattern passes review, audit the public source of alchemical-leather,
  lodestone-transit and the UniversalGraves fork. Use separate editorial plans.
  Preserve upstream attribution for the fork and local concurrent code changes.
  Apply only documentation changes there; no new releases or binary installs are
  implied for these other mods. Existing Scale beta delivery remains in scope.

### Iteration 3 review

- Detailed reference moved intact into GUIDE.md with a contents list and corrected
  relative paths. Existing specialized docs remain the deeper source of detail.
- README now gives a first brew and four experiments rather than all tier tables,
  loot formulas, wolf trust rolls and environmental surprises.
- Kept the essential tameable gesture, runtime requirements and beta/prototype
  limitations visible; discovery must not conceal operational requirements.
- Reviewed the revised pair: no removed rules or invented mechanics. Images stay
  in context and the full guide is explicitly labeled spoiler-rich. This pattern
  is suitable for the other mods, with safety caveats retained on their front pages.
- Exact isolated source snapshot passed build, all 122 required server tests and
  the full real-client suite on 2026-09-09 (clean exit 17:49:26). Source hashes match
  the canonical checkout. Publication and local installation are still pending.

## Delivery verification

- Source snapshot and candidate commit 33b1f5c passed local validation and GitHub
  Actions run 34373390000. Published v0.1.0-beta.5 as a prerelease targeting that
  commit, with regular and sources JARs. Downloaded both assets and matched SHA-256.
- Installed the downloaded regular JAR in VanillaPlus-26.2 (1), with exactly one
  Scale Brews JAR. Moved beta.4 to codex-backups/scalebrews-beta5-20260909 outside
  mods; no world or configuration changed. Installed SHA-256:
  5cbee58b3da9628f53f267b35ab3a3d16d922c4d2af6dfbf5b31a4d126711fc7.
- Public README/guide updates also verified in alchemical-leather (2398463),
  lodestone-transit (46558ad) and UniversalGraves (9151fdb). Only documentation
  was committed there, from clean public snapshots; local concurrent code remains
  untouched. Each repository includes its own editorial plan and review.
- Remaining human pack/multiplayer QA and the anatomical implementation plan stay
  open. No Modrinth public release or other mod binary release was performed.
