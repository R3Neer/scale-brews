# TM: Alchemical Leather compatibility ownership

Status: **temporary implementation specification** for branch `chatgpt/alchemical-leather-wear-compat`.

This sprint migrates Scale Brews' first-party Alchemical Leather compatibility declarations into Scale Brews itself while preserving complete standalone behavior when Alchemical Leather is absent.

## Frozen requirements

- Scale Brews owns the Alchemical Leather slot declarations for its own potion effects.
- Growth and Shrinking remain explicitly assigned to their intended humanoid armor slots; the existing Alchemical Leather mapping is the migration source of truth unless adversarial review finds a design bug.
- Scale Brews owns the wear classification for Growth and Shrinking.
- Growth and Shrinking are persistent body-state effects and initially declare `wear: none`; simply remaining large/small must never become a disguised durability timer.
- Compatibility resources must be inert when Alchemical Leather is absent and must not add a hard runtime dependency.
- Scale Brews does not select armor or mutate durability directly.
- If future Scale Brews effects need semantic wear events, they use Alchemical Leather's public compatibility API through an isolated optional adapter.
- Existing scale mechanics, living-platform/anatomy behavior, movement, exhaustion, mounts and compatibility must remain unchanged by this migration.

## Iterative TM phases

1. Add owned slot/wear resources under Scale Brews' namespace.
2. Remove duplicate ownership from Alchemical Leather only after the new resources are validated in the real compatibility fixture.
3. Add tests proving resources are discoverable with Alchemical Leather and Scale Brews still launches standalone.
4. Adversarial review for resource namespace/path, missing-mod behavior and duplicate rule precedence.
5. Update README/architecture/compatibility/validation docs after convergence.
