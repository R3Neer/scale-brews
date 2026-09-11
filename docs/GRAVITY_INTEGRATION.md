# Effective-gravity integration

Scale Brews treats Tiny Mounts as ordinary Minecraft vehicle/passenger relationships. External mods do not need a Tiny-Mount-specific API to rotate or otherwise manage a mount: they operate on the root `LivingEntity`, and Scale-owned mount mechanics read that root's effective gravity when generating their own movement.

This integration is released for Tiny Mount movement starting with **0.1.0-beta.6**. It does not make the in-development anatomical collision system a released feature.

## Ownership boundary

Scale Brews owns the vectors that Scale Brews creates. Bee flight, controlled chicken glide and wolf pounce/landing therefore interpret local up/down and control vectors in the root mount's effective gravity frame. A frame change affects subsequent generated contributions; it does not reinterpret or rotate world-space momentum that was already present.

Scale Brews does **not** own gravity policy, effects, input charges or the decision to change an entity's gravity. Without a compatible gravity provider, the effective frame is vanilla `DOWN` and Tiny Mount behavior is unchanged.

## Shared gravity-frame service

`integration.gravity.GravityFrames` is the single Scale-side authority for reading effective entity gravity. It defaults to `DOWN`, permits one provider owner, treats reinstalling the same owner as idempotent, and rejects a competing owner rather than allowing two Scale subsystems to disagree.

When Gravity Changer is installed, Scale links its public `GravityDirectionUtil.getGravityDirection(Entity)` API reflectively at initialization. Gravity Changer remains optional: its classes are not a hard Scale dependency and a Scale-only runtime does not load them.

`GravityFrame` contains the six cardinal local/world vector transforms used by mount mechanics. Mount code consumes this service instead of carrying per-species coordinate conventions.

## Tiny Mount behavior

- **Bee flight:** rider look intent is calculated in mount-local coordinates and the generated velocity is transformed once into the root mount's current frame.
- **Chicken glide:** falling/damping is evaluated along local vertical. The vanilla world-Y fall damping is not allowed to act on a tangential axis under rotated gravity.
- **Wolf pounce:** the generated launch, landing cleanup and body-height sampling use the root wolf's local frame. Existing world momentum is not double-transformed.

The rider's own frame is not used merely because the rider supplies controls. The physical root mount determines the frame for mount-generated mechanics.

## Relationship to Entity Collisions

The in-development all-direction anatomical collision system is a separate subsystem. Tiny Mount gravity support does not require that collision system to be READY and does not advance its G7 Clinging migration.

That collision work also needs an effective body gravity frame. When its development branch is reconciled with `main`, its current collision-local gravity reader must migrate/delegate to `integration.gravity.GravityFrames` rather than survive as a second authority. The shared rule is simple: Scale may read gravity through one service; it does not acquire ownership of another mod's gravity policy.

## Validation scope

The gravity-frame suite covers all six cardinal frames, DOWN equivalence, inverse local/world transforms, rider/root frame separation, frame changes between actions, pre-existing momentum preservation, bee flight, chicken glide and wolf pounce/landing. The normal CI runs both server and real client GameTests so these regressions remain part of ordinary repository validation.

On 2026-09-11, GitHub Actions run `34586367431` completed successfully on Ubuntu/Java 25. The server lane ran all **130 required GameTests** and the real client GameTest lane also completed successfully. Gravity Changer was not present in that run, so the same evidence verifies that the optional integration does not become a hard runtime dependency and that the vanilla-DOWN fallback remains valid. The six-frame provider behavior itself is exercised through the test provider without creating a production Clinging dependency.

This is bounded integration evidence, not a claim that every gravity mod is compatible. Gravity Changer is the concrete optional provider currently recognized by Scale Brews.
