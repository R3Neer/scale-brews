# Scale Brews anatomical API

This is the narrow, public boundary consumed by optional integrations. It is not a
catalog, renderer, pose source, movement-packet protocol, or an invitation to
duplicate collision/transport in another mod.

## Compatibility

`AnatomyApi.PROTOCOL_VERSION` is incremented only for an incompatible API change.
An integration must require the version it was built for and every capability it
will use. An absent/incompatible Scale artifact is diagnosed or rejected by the
consumer's declared minimum version; it does **not** authorize a second entity
AABB/carry/reference/reconciliation engine. A standalone consumer may retain its
own gravity/input/effect behavior but has no substitute living-entity platform
physics. Legacy third-party JSON is migrated inside this core as an explicit,
one-sided plane, never as inferred anatomy.

Version 1 capabilities:

| Capability | Contract |
| --- | --- |
| `CONTACTS` | Read and clear Scale's server-owned material contact. A contact is transient and may disappear at any time. |
| `CLEARANCE` | Test only configured anatomical convex pieces. The caller must separately test blocks and its upstream oriented destination box. |
| `RAYCAST` | Query a material face on eligible configured anatomy for a scoped interaction such as placement. It does not install a global selection hook. |
| `GRAVITY_FRAME` | Register/read the active cardinal frame through the single adapter owned by gravity integration. Scale never writes it. |
| `ROOT_TRANSPORT` | Scale owns anatomical transport/baselines for the root and vanilla passenger-seat refresh. Consumers must not move or rebase it again. |
| `SERVER_AUTHORITY` | Geometry, poses and contacts are server-originated; no client geometry upload is supported. |

`compatible(peerVersion, required)` only proves the binary contract. The mode is
level/session/policy state, never a test of whether the transported body itself
has exported geometry:

| Mode | Ownership and permitted operations |
| --- | --- |
| `DISABLED` | Scale does not own entity platform physics. Vanilla/Gravity Changer standalone behavior applies; Scale must not revive its former platform engine. |
| `BINDING` | Scale owns the shared route while catalog/session data is unavailable. It fails closed: no legacy AABB, carry, placement, clearance, gravity claim or material contact. |
| `READY` | Catalog/session is accepted. Material queries are allowed, but an unexported species or unsupported pose returns empty geometry/contact rather than a fallback shape. |

Use `mode(entity)`, `ownsSharedPhysics(entity)` and `ready(entity)` explicitly.
`active(entity)` and `usesSharedPhysics(entity)` are deprecated compatibility
aliases; `active` means ownership (including BINDING) and the latter means
`READY`, not ownership. A consumer that sees BINDING
must clear only its transient reference/presentation state and must not route to
legacy AABB/carry behavior.

## Operations

`mode(Entity)`, `ownsSharedPhysics(Entity)`, `ready(Entity)`, `supported(Entity)`, `support(Entity)`, `clearContact(Entity)`,
`spaceClear(Entity, AABB)`, `raycast(Entity, start, end)`, `attachAtContact(body, hit)` and `gravity(Entity)` are the complete V1 physical
surface. `support` exposes identity only; material piece, local anchor, movement
baseline and resolution remain Scale internals. `spaceClear` and `raycast` consider
only READY, configured and eligible convex anatomy. They do not implement general
`noCollision`, pathfinding, block selection, placement, or an AABB fallback.
`RayHit` identifies the actual support/material face. A caller first performs its
own permission, block-clearance, inventory and gravity rules, positions the new
body, then calls `attachAtContact`. That method validates the current material
face/eligibility and creates only Scale's transient anchor; it never moves the
body or creates a general collision shape.

The gravity owner installs its `Function<Entity, Direction>` only through
`installGravityAdapter(owner, resolver)`. Repeating the same owner is idempotent;
a different owner fails explicitly. Consumers must not reach `GravityFrames` or
write a `GravityFrame` directly.

For Gravity Changer integrations: use `gravity` for physical queries, preserve the
owner's camera/input/effect rules, test the owner-oriented target AABB against
blocks and `spaceClear` before gravity mutation, and leave gravity/position
unchanged on a voluntary failure. A physical support accepts any normal whose dot
against anti-gravity is at least cos(45°); cardinal dominance/ambiguous ties are
only an input-direction selection rule.

## Lifecycle and transport

On DISABLED/BINDING, unsupported pose, catalog/epoch/revision invalidation,
tracking loss, teleport, dimension change, gravity change or discontinuity, an
integration must discard only its presentation/reference state. It must not create
a replacement contact or carry. `clearContact` releases Scale's contact and anchor;
it does not alter gravity selection or a mod's independent effect state.

Player/controlled-vehicle references, authoritative baseline and reconciliation
are owned by Scale while `ROOT_TRANSPORT` is active. Each confirmed material/root
frame transition is applied exactly once; multiple genuine transitions may be
aggregated and measured within one tick, but the same transition is never applied
twice. Existing passenger trees retain vanilla seats and never receive independent
contacts or transport.

## Evolution

Additive capabilities may be introduced without changing the version. A changed
method signature, semantic weakening, new client-to-server geometry, or exposing
an internal pose/catalog implementation requires a new protocol version and a
coordinated Scale/consumer artifact validation.
