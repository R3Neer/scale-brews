from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Expected text not found in {path}: {old[:120]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


# Player guide: publish the final reach balance and the final Tiny Mount family UX.
replace_once(
    "docs/GUIDE.md",
    "Growth's entity reach increases by 50% / 100% / 150% at its pure-potion sizes: 4.5 / 6 / 7.5 blocks from a survival baseline of 3. This keeps small targets beside a giant's feet reachable from eye height. Block reach retains +20% / +40% / +60%; Shrinking's reach penalties are unchanged. Entity reach follows effective size throughout blending and mixed effects, continuing the same linear growth curve beyond Growth III for external scales (subject to vanilla attribute limits). Other reach modifiers still compose multiplicatively; explicit item attack-range components may use their own ranges.",
    "Growth's entity reach increases by 30% / 60% / 90% at its pure-potion sizes: **3.9 / 4.8 / 5.7 blocks** from a survival baseline of 3. Block reach remains +20% / +40% / +60%: **5.4 / 6.3 / 7.2 blocks** from a survival baseline of 4.5. Shrinking reduces both block and entity reach by 10% per equivalent level, giving entity ranges of **2.7 / 2.4 / 2.1** and block ranges of **4.05 / 3.60 / 3.15**. Entity reach follows effective size throughout blending and mixed effects, continuing the same linear Growth curve beyond Growth III for external scales (subject to vanilla attribute limits); Growth block reach and Shrinking reach retain their tier-III caps. Other reach modifiers still compose through Minecraft's attribute system, while explicit item attack-range components may use their own ranges."
)

replace_once(
    "docs/GUIDE.md",
    "Equip an adult chicken, bee or tamed wolf with a saddle; equipping does not require being small. Riding does require the appropriate size ratio.",
    "The three default Tiny Mounts deliberately use different interaction families. A **Chicken** (`direct`) and a **tamed Wolf** (`tameable_direct`) can carry a passive rider without a saddle, but need a vanilla saddle for manual control. A **Bee** (`item_steered`) follows Pig/Strider-style rules: it must be saddled even to mount and then needs a Flower on a Stick for manual control. Equipping a saddle does not require being small; riding still requires the appropriate size ratio. Wild wolves are the deliberate exception to the taming gate because riding is also their alternate taming route described below."
)

replace_once(
    "docs/GUIDE.md",
    "| Bee | Hold a Flower on a Stick and look where you want to fly. Remove the item to release manual control. |\n\nScale-generated bee flight",
    "| Bee | Hold a Flower on a Stick and look where you want to fly. Remove the item to release manual control. |\n\nEquipment UI follows the same families. **Chicken:** Crouch + Use from outside opens its saddle menu, and E opens that menu while riding. **Wolf:** Crouch + Use remains the mounting gesture so normal Use can keep vanilla sitting/feeding/equipment behavior; E opens its SADDLE + BODY equipment menu while mounted. **Bee:** there is no mount menu, so E opens the player's normal inventory. The saddle is Minecraft's normal shearable equipment in 26.2; shears remove it when the entity's own permissions allow. Shearable BODY equipment is removed before SADDLE by vanilla slot order, and a wolf keeps its vanilla owner-only shearing permission.\n\nScale-generated bee flight"
)

replace_once(
    "docs/GUIDE.md",
    "Bees keep their natural bobbing and rolling while ridden: the rendered rider follows the saddle, while first-person camera and collision physics stay stable.",
    "Saddles and mounted riders consume the mount model's **final rendered attachment transform**, so they follow the same finished animation instead of reconstructing a separate pose. Bees therefore keep their natural bobbing and rolling while ridden, and the same path covers chicken, wolf and vanilla horse body animation. The optional-mod proof also passed with EMF 3.3.5, ETF 7.2 and Fresh Animations 1.10.5. First-person camera and collision physics remain separate and stable."
)

replace_once(
    "docs/GUIDE.md",
    "- Tiny Mounts as a whole, individual mount types, controls and saddle visuals.",
    "- Tiny Mounts as a whole, individual mount types, interaction `family`, steering item, movement/ability, optional BODY equipment and saddle visuals."
)

replace_once(
    "docs/GUIDE.md",
    "The current GitHub Actions workflow runs server GameTests plus a real client/integrated-server GameTest under Xvfb, covering camera, beacons, resources, synchronization, tiny-mount input, animated bee riders and unmounted steering-item attraction.",
    "The current GitHub Actions workflow runs server GameTests plus a real client/integrated-server GameTest under Xvfb, covering camera, beacons, resources, synchronization, family-driven Tiny Mount mounting/inventory rules, saddle shearing and dispenser equipment, animated saddle/rider transforms, wolf controls and unmounted steering-item attraction. The beta.7 prerelease lane passed **154 required server GameTests** plus the full client suite; the pinned EMF/Fresh Animations proof is a separate optional-mod lane rather than a hard dependency."
)

# Configuration reference is already family-driven; make the default trio explicit and generalize rendering wording.
replace_once(
    "docs/CONFIGURATION.md",
    '"mounts": { "minecraft:chicken": true, "minecraft:bee": true },',
    '"mounts": { "minecraft:chicken": true, "minecraft:bee": true, "minecraft:wolf": true },'
)
replace_once(
    "docs/CONFIGURATION.md",
    "Enabled tiny-mount bees retain natural body bobbing and rolling while carrying a player. The rendered rider follows the animated saddle, including its tilt, without moving the physical entity, collision box or first-person camera. This does not require a new texture or JSON field and remains active without the steering item. Custom renderers that replace the bee model need separate compatibility testing.",
    "Supported living Tiny Mounts share the mount model's final rendered attachment transform with both the saddle and passenger, after vanilla `setupAnim` and optional model animation have run. This keeps the saddle and rider on one animation source instead of reconstructing species-specific poses. Bees retain natural bobbing and rolling while ridden; the same path covers chicken, wolf and vanilla horse body animation. The pinned optional-mod proof passed with EMF 3.3.5, ETF 7.2 and Fresh Animations 1.10.5. Custom renderers that replace the expected model/root structure still need separate compatibility testing."
)

# Keep the copyable example pack aligned with all three bundled Tiny Mounts.
replace_once(
    "examples/world-config/data/scalebrews/scalebrews/rules/default.json",
    '"mounts": { "minecraft:chicken": true, "minecraft:bee": true }',
    '"mounts": { "minecraft:chicken": true, "minecraft:bee": true, "minecraft:wolf": true }'
)

# Validation: make beta.7 the current contract and explicitly label older evidence historical.
replace_once(
    "docs/VALIDATION.md",
    "# Validation record\n\n## 0.1.0-beta.6 candidate — 2026-09-11",
    "# Validation record\n\n## Current prerelease: 0.1.0-beta.7 — 2026-09-14\n\n- The guarded prerelease lane passes **154 required server GameTests**, the complete real-client/integrated-server GameTest suite, packaged-version/test-class checks, tag verification and a post-upload SHA-256 comparison of the regular JAR.\n- Tiny Mount interaction is validated by `family`, including data-only Cow adversaries proving `direct` / `item_steered` behavior without species branches, dynamic equipment menus, Bee player-inventory UX, vanilla saddle shearing/drop behavior, Wolf owner protection and BODY-before-SADDLE shearing, plus the real dispenser equipment path.\n- Animated saddle/rider attachment uses the final rendered mount transform. A separate reproducible optional-mod lane passed with the VanillaPlus-pinned **EMF 3.3.5 + ETF 7.2 + Fresh Animations 1.10.5** stack and verified an active EMF bee model plus a finite, non-identity rider attachment transform.\n- Size presentation/reach coverage includes scale-dependent step pitch and gait timing plus the final reach contract: Growth block +20% per equivalent level, Growth entity +30%, and Shrinking block/entity -10%.\n- Normal gameplay still uses the released upper-surface living-platform system. All-direction anatomical collisions and shared Clinging Reoriented physics remain unfinished and disabled in ordinary worlds. Full-pack human acceptance and broad multiplayer/mod-combination QA remain bounded separately.\n\n## Historical validation records\n\nThe dated sections below are retained as evidence of earlier milestones. They may describe behavior or balances that were later superseded; current gameplay contracts live in the player/configuration/mechanics references and the beta.7 section above.\n\n## 0.1.0-beta.6 candidate — 2026-09-11"
)

# TODO is active work, so put beta.7 first and remove the obsolete intermediate reach target.
replace_once(
    "TODO.md",
    "# Scale Brews — active work\n\n## Beta.6 prerelease delivery — 2026-09-11",
    "# Scale Brews — active work\n\n## Beta.7 Tiny Mount families and documentation audit — 2026-09-14\n\n- [x] Merge the validated mount-polish work into `main`: data-driven `direct` / `tameable_direct` / `item_steered` families, generic SADDLE/BODY inventory, vanilla shearing/dispensers, wolf gamefeel and final animated mount attachment transforms.\n- [x] Publish `0.1.0-beta.7` through the guarded prerelease lane with 154 required server GameTests, the full real-client suite, package checks and published-JAR SHA verification.\n- [x] Audit the live GUIDE/CONFIGURATION/VALIDATION/TODO/example datapack after beta.7, remove stale reach and Tiny Mount wording, and label older validation entries as historical rather than current contracts.\n- [x] Refresh the existing beta.7 prerelease after this documentation audit so its tag/assets point at the documentation-complete validated snapshot.\n- [ ] Full-pack human acceptance and broad multiplayer/mod-combination QA remain separate from automated release validation.\n\n## Beta.6 prerelease delivery — 2026-09-11"
)
replace_once(
    "TODO.md",
    "- [x] Rebalance entity reach to 4.5 / 6 / 7.5 blocks at Growth I/II/III survival baseline, following effective scale and preserving block reach and Shrinking balance.\n- [x] Validate foot-level melee regression, mixed/external sizes, build and client runtime: 80 base server tests and the real-client suite pass, including Growth III eye-ray targeting beside the feet.",
    "- [x] Finalize Growth entity reach at **3.9 / 4.8 / 5.7 blocks** at Growth I/II/III (+30% per equivalent level from the 3.0 survival baseline), superseding the earlier 4.5 / 6 / 7.5 intermediate target. Block reach remains **5.4 / 6.3 / 7.2**, and Shrinking block/entity reach remains -10% per equivalent level.\n- [x] Validate the final reach curve across pure tiers, mixed/external sizes and client runtime in the current regression suite; beta.7 release validation runs all 154 required server GameTests plus the full real-client suite."
)

# One-shot release-preparation hook: the prerelease workflow commits these edits,
# then this helper deletes itself so future beta refreshes are not mutated again.
Path(__file__).unlink()
