# Scale Brews — active work

`TODO.md` is only an index of **currently open work**. Completed history belongs in [CHANGELOG](CHANGELOG.md) and executed evidence in [VALIDATION](docs/VALIDATION.md). The entity-collision subsystem has its own canonical [requirements](docs/ENTITY_COLLISIONS_REQUIREMENTS.md), [architecture](docs/ENTITY_COLLISIONS.md) and [plan](docs/ENTITY_COLLISIONS_PLAN.md); its gates are intentionally not duplicated here.

## Entity collisions

- [ ] Continue the canonical [entity-collision plan](docs/ENTITY_COLLISIONS_PLAN.md) from its first open gate. Requirement changes happen in [ENTITY_COLLISIONS_REQUIREMENTS](docs/ENTITY_COLLISIONS_REQUIREMENTS.md), never by adding a second checklist here.

## Beta acceptance and publication

- [ ] Human acceptance of the exact beta.5 JAR in the full VanillaPlus pack and a real multiplayer/dedicated session.
- [ ] Complete a clean exit of the full VP26 client test suite. Gameplay assertions reached the end in the recorded run, but Fabric's integrated-world teardown deadlocked; [VALIDATION](docs/VALIDATION.md) owns the evidence and exact limitation.
- [ ] After human approval, publish the already verified beta.5 artifact to Modrinth as a beta and verify metadata/download digest against the accepted JAR. Do not create a different artifact for publication.

## Manual compatibility QA

- [ ] Two-player/latency acceptance of borrowed-wolf combat and the requested shared-wolf gameplay cases.
- [ ] Manual modded-renderer/player-model acceptance for the released beta behavior, including giant-player head animation with external model/resource packs.
- [ ] Broader manual stress of the released platform path until it is replaced: lead-dragged occupied boats, water/rail transitions, falling-block hardening/placement after release, crowded support chains, wall/camera corners and late-joining observers.

When the new entity-collision system reaches its migration gate, equivalent cases move to that plan's acceptance matrix rather than being copied here.
