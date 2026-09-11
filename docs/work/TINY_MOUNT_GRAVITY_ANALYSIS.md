# Tiny Mount gravity analysis

Status: **TEMPORARY — DELETE ON SUCCESS**.

## 1. Current behavior

Scale `main` computes several Tiny Mount mechanics in vanilla/world coordinates. `TinyMounts.flightVelocity` derives a world-space vector directly from rider yaw/pitch; `TinyChickenGlideMixin` modifies world-Y damping; wolf pounce/landing logic likewise assumes ordinary DOWN semantics in places.

Clinging currently patches those Scale assumptions for non-DOWN gravity through three Scale-specific compatibility mixins. That does not mean Tiny Mounts are a distinct kind of mount. It means Scale-generated impulses are not yet gravity-frame aware.

Clinging's own mounted contract is already generic: it operates on a non-player `LivingEntity` root vehicle and asks only whether Gravity Changer gravity is supported. No Tiny Mount type or Scale API participates in that decision.

## 2. Architectural conclusion

The external contract should remain:

```text
player passenger
      ↓
LivingEntity root vehicle
      ↓
Gravity Changer effective gravity
```

A Tiny Mount participates through the exact same relation as any other eligible living mount.

Scale's responsibility begins only when Scale generates a mechanic-specific contribution such as flight velocity, glide damping or pounce impulse. Those contributions must be expressed in the root mount's effective gravity frame before they become world-space movement.

Therefore there is no justification for:

- a Tiny-Mount-specific Clinging path;
- a Tiny capability marker consumed by Clinging;
- old/new Scale shim negotiation;
- a second mount transfer protocol.

## 3. Relationship to `chatgpt-editing`

The entity-collision branch already specifies a single-owner gravity adapter (FR-073/074) and currently contains `collision.internal.GravityFrames`. The collision architecture also states that a supported body's gravity frame is independent from support-root orientation.

Scale mount mechanics need the same effective entity gravity, but remain explicitly outside the collision subsystem. Therefore the reusable frame reader/service should be transversal:

```text
optional effective-gravity integration
             ↓
   canonical Scale GravityFrame service
          ↙            ↘
future collision     mount mechanics
```

A separate mount registry would create two authorities. Making Clinging the provider owner would incorrectly make Scale mount correctness depend on Clinging.

The exact package/API shape must remain minimal and is provisional until implementation analysis checks Scale `main`, the pinned Gravity Changer API and the latest `chatgpt-editing` state. `chatgpt-editing` is not modified by this workstream while its other agent is executing S00.

## 4. Generic-mount compatibility rule

Scale should not expose a public 'native Tiny Mount gravity' capability for Clinging. Instead:

- Clinging sets/owns mount gravity generically through Gravity Changer;
- Scale reads the root mount's effective frame when generating Scale-owned mechanics;
- ordinary Minecraft passenger/vehicle semantics remain the only mount contract between the two repos;
- Scale's behavior is correct regardless of whether the frame came from Clinging, a Gravity Anchor, another compatible mod or another external source.

This is a stronger abstraction than compatibility with one Clinging release and avoids version handshakes entirely.

## 5. Release/use consequence

Scale Brews will not be treated as a required runtime dependency of the upcoming Clinging prerelease while the larger Scale work remains incomplete. Therefore there is no need to preserve released beta.5 Tiny Mount behavior inside Clinging via transitional shims.

The two workstreams validate independently:

- Clinging proves generic mounted gravity without Scale;
- Scale proves its own Tiny Mount mechanics under arbitrary effective gravity;
- a bounded cross-repo fixture may later prove the composition, but neither production side gains a Tiny-specific contract.

## 6. Transformation semantics

The mount system must distinguish:

- control intent in gravity-local coordinates;
- newly generated Scale-owned impulse/velocity;
- existing world momentum.

Only the generated contribution is transformed by the current root frame. Existing world velocity is never reinterpreted simply because gravity changes. This preserves Clinging's world-momentum contract and prevents double rotations.

## 7. Iterative convergence record

- **Pass 1:** initial idea was to copy Clinging's three Scale compatibility mixins into Scale.
- **Pass 2 change:** rejected literal port; behavior is specified independently and common transforms should move behind a reusable gravity helper.
- **Pass 3 change:** rejected placing gravity support inside `collision`; Tiny Mounts are explicitly a separate subsystem.
- **Pass 4 change:** rejected Clinging as the gravity-provider owner; Scale must read effective gravity without depending on Clinging.
- **Pass 5 change:** added one canonical transversal gravity-frame authority so future collision and mount consumers cannot disagree.
- **Pass 6 change:** initially added a native capability marker plus transitional Clinging shims.
- **Pass 7 change after owner review:** rejected that compatibility layer. Tiny Mounts are ordinary mounts at the contract boundary, unfinished Scale will not be a Clinging prerelease dependency, and Scale-specific mechanics must simply honor the root's effective gravity.
- **Pass 8:** reread Clinging `MountedGravity`/`MobGravity` and the three Scale compatibility mixins. They confirm that the special handling exists only inside Scale-generated mechanics, not in Clinging's mount-selection contract. No blocker to the generic contract was found.
