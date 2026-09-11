# Tiny Mount arbitrary-gravity implementation plan

Status: **TEMPORARY — DELETE ON SUCCESS**.

Branch target: dedicated Scale branch from `main`; do not use or modify `chatgpt-editing`.

## T0 — baseline and coordination

- [x] Read current Tiny Mount code and `chatgpt-editing` entity-collision requirements/architecture/plan.
- [x] Establish generic-mount-vs-collision ownership and future single gravity-frame authority.
- [x] Converge temporary requirements/analysis/workflow/plan.
- [ ] Record exact Scale `main` baseline and CI before implementation.
- [ ] Re-check active `chatgpt-editing` state immediately before implementation in case S00/G1 has changed the coordination contract.

## T1 — canonical effective-gravity reader/service

**Requirements:** TGR-004, TGR-012..015, TGR-018..019.

- [ ] Inspect pinned Gravity Changer API and choose an optional integration mechanism that cannot break Scale-only loading.
- [ ] Introduce one canonical Scale effective-gravity/frame service outside collision-specific internals.
- [ ] Default to vanilla/DOWN with no provider.
- [ ] Preserve single-owner/idempotent-owner semantics.
- [ ] Do not expose a Tiny-Mount-specific public capability or Clinging handshake.
- [ ] Keep the service minimal enough for future `chatgpt-editing` collision adoption.

## T2 — bee/general Tiny Mount flight

**Requirements:** TGR-001..007, TGR-010..011.

- [ ] Express rider look/control intent in the root mount's gravity frame.
- [ ] Reuse one shared transform helper rather than Clinging-specific mixins.
- [ ] Preserve current speed, pitch and vertical limits.
- [ ] Prove DOWN differential equivalence and six-frame behavior.

## T3 — chicken glide

**Requirements:** TGR-001..006, TGR-008, TGR-010..011.

- [ ] Replace world-Y assumptions with local gravity vertical for Scale's controlled Tiny Mount glide behavior only.
- [ ] Preserve jump-held/released semantics and ordinary unmounted chicken behavior.
- [ ] Add six-frame and frame-change adversarial coverage.

## T4 — wolf pounce/landing

**Requirements:** TGR-001..006, TGR-009..011.

- [ ] Transform newly generated pounce impulse using the root frame.
- [ ] Make landing cleanup local-frame aware without rotating unrelated velocity components incorrectly.
- [ ] Preserve hit attribution, cooldown and attack-animation behavior.
- [ ] Add six-frame, mid-action frame change and DOWN regression cases.

## T5 — generic mount-contract verification

**Requirements:** TGR-001, TGR-016..020, TNFR-004, TNFR-007.

- [ ] Verify production Scale contains no Clinging dependency and no Tiny-Mount-specific external compatibility channel.
- [ ] Verify the Tiny Mount remains an ordinary Minecraft vehicle/root and that gravity-aware behavior is obtained solely because Scale-generated mechanics read the root's effective frame.
- [ ] Use the old Clinging Scale mixins only as regression references for outcomes, never as runtime shims.
- [ ] Add a bounded integration fixture, if practical, demonstrating that a generic external mounted-gravity consumer can rotate an actual Tiny Mount without type-specific knowledge.
- [ ] Verify existing world momentum is not double-transformed when the frame changes.

## T6 — permanent docs and merge

- [ ] Run Scale without Gravity Changer.
- [ ] Run Scale with the pinned optional Gravity Changer integration.
- [ ] Run applicable real client tests.
- [ ] Run the bounded generic-mount composition fixture if implemented; do not create a permanent Scale→Clinging dependency.
- [ ] Update permanent Scale docs according to existing scope (MECHANICS/GUIDE/CONFIGURATION/VALIDATION and any Tiny Mount-specific doc actually affected).
- [ ] Hand the gravity-service decision to the `chatgpt-editing` agent for reconciliation in their own canonical workflow; do not edit their active branch from this workstream.
- [ ] Full review produces zero changes.
- [ ] Delete `docs/work/*`.
- [ ] Re-run candidate CI.
- [ ] Merge normally to `main` and verify `main` CI.

## Dependency graph

```text
T0 → T1 → T2
       ├── T3
       └── T4
T2+T3+T4 → T5 → T6
```

T1 is the architectural coordination point. T2/T3/T4 may be separate commits after that service stabilizes. There is deliberately no old/new Clinging compatibility gate: the contract is generic mounting, not Tiny-Mount version negotiation.
