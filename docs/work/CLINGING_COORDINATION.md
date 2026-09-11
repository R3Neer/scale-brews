# Coordination with Clinging and `chatgpt-editing`

Status: **TEMPORARY — DELETE ON SUCCESS**.

## Do not edit the other agent's branch

The active entity-collision agent owns `chatgpt-editing` and is still executing mandatory S00. This workstream must not modify that branch, close S00, begin G1 or rewrite its canonical plan.

## Information that must be handed to that agent

When convenient within their own workflow, the entity-collision agent should account for these decisions:

1. Tiny Mount arbitrary-gravity behavior is becoming native Scale behavior under the mount subsystem, not part of the collision solver.
2. Scale `main` will introduce one transversal gravity-frame provider/service usable by mounts and future collision code.
3. `collision.internal.GravityFrames` on `chatgpt-editing` must eventually delegate/migrate to that canonical service rather than survive as a second authority.
4. Preserve FR-073 single-owner semantics and FR-074: Scale reads gravity but does not own Clinging input/effects/charges.
5. The canonical provider integration with Gravity Changer must be optional and must not require Clinging.
6. Clinging G7 remains the point where moving-surface/carry/reference/reconciliation ownership moves to Scale collision; this Tiny Mount work does not advance G7 early.
7. Clinging will retain old-Scale Tiny Mount shims only when Scale's native capability marker is absent, preventing double application during release overlap.

## Merge-order expectation

The Tiny Mount gravity branch may merge to Scale `main` independently. Before `chatgpt-editing` later merges back to `main`, its agent must reconcile the gravity-frame service conflict intentionally. A blind merge that leaves both registries alive is unacceptable even if compilation succeeds.
