# Coordination with Clinging and `chatgpt-editing`

Status: **TEMPORARY — DELETE ON SUCCESS**.

## Do not edit the other agent's branch

The active entity-collision agent owns `chatgpt-editing` and is still executing mandatory S00. This workstream must not modify that branch, close S00, begin G1 or rewrite its canonical plan.

## Contract agreed with Clinging

Clinging already treats mounted gravity generically: an eligible non-player `LivingEntity` root vehicle is the mount. Tiny Mounts must remain inside that same contract.

Therefore:

- Clinging will not add a Tiny-Mount-specific mount channel, capability marker or Scale-version negotiation.
- Clinging's current `ScaleFlightGravityMixin`, `ScaleWolfGravityMixin` and `ScaleChickenGravityMixin` are considered temporary patches for Scale-generated mechanics and are targeted for removal from Clinging.
- Scale is responsible for making its own flight/glide/pounce/landing contributions respect the effective gravity already present on the mount root.
- A Tiny Mount remains an ordinary Minecraft vehicle/passenger relation from an external consumer's point of view.

## Information that must be handed to the entity-collision agent

When convenient within their own workflow, the `chatgpt-editing` agent should account for these decisions:

1. Scale `main` will gain gravity-aware Tiny Mount mechanics under the mount subsystem, not inside the collision solver.
2. The supporting effective-gravity/frame reader should be transversal within Scale so both mount mechanics and future collision code can consume one authority.
3. `collision.internal.GravityFrames` on `chatgpt-editing` must eventually delegate/migrate to that canonical service rather than survive as a second authority.
4. Preserve FR-073 single-owner semantics and FR-074: Scale reads gravity but does not own Clinging input/effects/charges.
5. Gravity Changer integration remains optional and must not require Clinging.
6. G7 remains the point where moving-surface/carry/reference/reconciliation ownership moves to Scale collision. Generic mounted gravity is a separate concern and does not wait for G7.
7. Do not introduce a Tiny-Mount-specific consumer capability into the collision API. Tiny Mount status is irrelevant to Clinging's mounted-gravity contract.

## Merge-order expectation

The Tiny Mount gravity branch may merge to Scale `main` independently. Before `chatgpt-editing` later merges back to `main`, its agent must reconcile the gravity-frame service intentionally. A blind merge that leaves both registries/readers alive is unacceptable even if compilation succeeds.

No change is required to the other agent's current S00 execution merely because this workstream exists.
