# TM: Alchemical Leather compatibility ownership

Status: **implementation converged; ready to merge** on branch `chatgpt/alchemical-leather-wear-compat`.

This sprint migrates Scale Brews' first-party Alchemical Leather compatibility declarations into Scale Brews itself while preserving complete standalone behavior when Alchemical Leather is absent.

## Frozen requirements

- Scale Brews owns the Alchemical Leather slot declarations for its own potion effects.
- Growth and Shrinking remain assigned to the chestplate slot, matching the previous Alchemical Leather mapping.
- Scale Brews owns the wear classification for Growth and Shrinking.
- Growth and Shrinking are persistent body-state effects and explicitly declare `wear: none`; simply remaining large or small is not a disguised durability timer.
- Compatibility resources are inert when Alchemical Leather is absent and add no hard runtime dependency.
- Scale Brews does not select armor or mutate durability directly.
- Future Scale Brews effects that need semantic wear events must use Alchemical Leather's public compatibility API through an isolated optional adapter.
- Existing scale mechanics, living-platform/anatomy behavior, movement, exhaustion, mounts and compatibility remain unchanged by this migration.

## Final implementation

Scale Brews ships four owned compatibility resources:

- `data/scalebrews/alchemical_leather/effect_slots/growth.json`
- `data/scalebrews/alchemical_leather/effect_slots/shrinking.json`
- `data/scalebrews/alchemical_leather/wear_rules/growth.json`
- `data/scalebrews/alchemical_leather/wear_rules/shrinking.json`

Both effects use the chestplate slot and both wear rules are explicit `none` policies guarded by their corresponding `requires_effect` identifier. No Scale Brews Java production class links Alchemical Leather.

## TM execution and adversarial closeout

1. **Ownership migration:** the Scale-owned slot and wear resources were added under the Scale Brews namespace.
2. **Duplicate ownership removal:** the coordinated Alchemical Leather branch removed its bundled Growth/Shrinking slot resources.
3. **Standalone gate:** Scale Brews branch CI run `34900347231` completed successfully, exercising the normal build, server GameTests and client GameTests with no Alchemical Leather dependency.
4. **Real compatibility gate:** Alchemical Leather PR #11 run `35010369543` cloned and built this branch, loaded the produced JAR into the coordinated compatibility fixture and passed the complete Alchemical Leather GameTest lane.
5. **Adversarial review:** namespace/path, missing-mod behavior and duplicate ownership were rechecked against the final resource layout. No production change was required after the final review.

The compatibility contract is deliberately data-only. Alchemical Leather owns armor selection, effect-source arbitration, work accumulation and durability; Scale Brews owns only the meaning of its own effects.

## Acceptance result

- Scale Brews launches and tests standalone: **PASS**.
- Growth/Shrinking slot declarations are owned by Scale Brews: **PASS**.
- Growth/Shrinking wear policy is explicit and non-ticking (`wear: none`): **PASS**.
- Coordinated Alchemical Leather fixture discovers the Scale Brews resources: **PASS**.
- No duplicate Scale Brews slot ownership remains in Alchemical Leather: **PASS**.
- No hard Alchemical Leather runtime dependency is introduced: **PASS**.
- Final adversarial pass required no further production change: **PASS**.
