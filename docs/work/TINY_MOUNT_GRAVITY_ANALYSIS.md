# Tiny Mount gravity analysis

Status: **TEMPORARY — DELETE ON SUCCESS**.

## 1. Current behavior

Scale `main` computes Tiny Mount movement in vanilla/world coordinates. `TinyMounts.flightVelocity` derives a world-space vector directly from rider yaw/pitch; `TinyChickenGlideMixin` modifies world-Y damping. Clinging currently patches these assumptions for non-DOWN gravity through Scale-specific compatibility mixins, including wolf pounce/landing behavior.

This is the wrong long-term ownership: the behavior describes Tiny Mount physics under an entity gravity frame and should work regardless of which mod/source produced that gravity.

## 2. Relationship to `chatgpt-editing`

The entity-collision branch already specifies a single-owner gravity adapter (FR-073/074) and currently contains `collision.internal.GravityFrames`. The collision architecture also states that the supported body's gravity frame is independent from support-root orientation.

Tiny Mounts needs the same effective gravity frame but is explicitly outside the collision subsystem. Therefore the long-term service must be transversal:

```text
optional gravity provider integration
             ↓
   canonical Scale GravityFrame service
          ↙            ↘
      collision       mount
```

A separate mount registry would violate single authority. Making Clinging install the provider would preserve an unnecessary Scale→Clinging dependency.

## 3. Proposed ownership

Recommended eventual package shape (names are provisional until implementation analysis confirms repository conventions):

```text
io.github.r3neer.scalebrews.integration.gravity
    GravityFrame / frame transforms
    GravityProvider registry/service
    optional Gravity Changer adapter

io.github.r3neer.scalebrews.mount
    TinyMounts consumes the service
    wolf/chicken/bee mechanics consume shared transforms
```

The entity-collision branch, when later synchronized with `main`, should move/delegate its `GravityFrames` logic to the same service. That synchronization belongs to the other agent's canonical requirements/architecture process, not this branch editing their active S00 work.

## 4. Backward compatibility with Clinging prerelease

Clinging's next prerelease still needs to work with released Scale beta.5, which has no native capability. Therefore Clinging should retain its compatibility shims conditionally:

- old Scale/native capability absent → Clinging shims apply;
- native Scale capability present → Clinging shims are disabled.

This overlap is temporary and avoids requiring an unreleased Scale artifact for the Clinging prerelease.

## 5. Transformation semantics

The mount system should distinguish:

- control intent in mount-local/gravity-local coordinates;
- newly generated impulse/velocity;
- existing world momentum.

Only the generated contribution is transformed by the current root frame. Existing world velocity is never reinterpreted simply because gravity changes. This is necessary to preserve Clinging's world-momentum contract and prevent double rotations.

## 6. Iterative convergence record

- **Pass 1:** initial idea was to copy Clinging's three Scale compatibility mixins into Scale.
- **Pass 2 change:** rejected literal port; behavior is specified independently and common transforms should move behind a reusable mount/gravity helper.
- **Pass 3 change:** rejected placing gravity support inside `collision`; Tiny Mounts is explicitly a separate subsystem.
- **Pass 4 change:** rejected Clinging as the gravity-provider owner; Scale must integrate optionally with Gravity Changer so Tiny Mounts works without Clinging.
- **Pass 5 change:** added one canonical transversal gravity provider to reconcile future collision and mount consumers and preserve FR-073.
- **Pass 6 change:** added a native capability marker plus transitional Clinging shims so the next Clinging prerelease remains compatible with Scale beta.5.
- **Pass 7:** full ownership/compatibility/main-vs-chatgpt-editing review produced no further changes.
