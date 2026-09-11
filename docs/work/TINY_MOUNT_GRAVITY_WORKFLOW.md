# Temporary TM workflow for Tiny Mount gravity

Status: **TEMPORARY — DELETE ON SUCCESS**.

This workstream uses a compact version of the Scale Brews iterative method. It deliberately does not modify the active `chatgpt-editing` S00 execution records.

## Cycle

```text
REQUIREMENTS
   ↓
CURRENT STATE / VERSION-SENSITIVE API ANALYSIS
   ↓
PLAN ↺
   ↓
ADVERSARIAL MODEL ↺
   ↓
IMPLEMENTATION ↺
   ↓
TARGETED + ADVERSARIAL TESTS
   ↓
FAILURE CLASSIFICATION
   ↓
FULL REVIEW ↺
   ↓
NO-CHANGE PASS
```

If a review changes requirements/architecture materially, return to the corresponding source before continuing.

## Planning constraints

- Work only on this dedicated branch until candidate completion.
- Do not edit `chatgpt-editing`; communicate required canonical changes to its agent/owner.
- Verify Gravity Changer/Fabric/Minecraft 26.2 API assumptions against pinned sources before choosing direct linkage vs reflective/compileOnly integration.
- Keep optional dependency loading safe when Gravity Changer is absent.
- Avoid creating an API that exists only to satisfy Clinging. The gravity service must make sense as a Scale capability used by mount mechanics and future collision code.
- Treat Tiny Mounts as ordinary Minecraft mounts at external boundaries. If implementation analysis discovers a genuine blocker to that contract, stop and update requirements before adding any Tiny-specific external channel.

## Adversarial minimum

For each migrated mechanic consider:

- all six cardinal frames;
- DOWN as a reference/differential case;
- rider and root with different frames;
- change of frame between ticks/actions;
- already-existing momentum vs newly generated impulse;
- accidental double transformation of a Scale-generated contribution;
- provider absent/present/reinstalled/conflicting owner;
- ordinary unmounted behavior;
- optional dependency classloading on dedicated server/client;
- a generic mounted consumer treating an actual Tiny Mount without type-specific knowledge.

Use holdout cases after implementation, but exhaustive fuzz/mutation infrastructure is optional unless a transformation helper proves error-prone.

## Finalization

1. Run full Scale build/server tests and applicable client tests.
2. Validate with Gravity Changer present and absent.
3. Validate the generic mount contract with a bounded composition fixture if practical, without creating a production Scale→Clinging dependency.
4. Update permanent Scale docs only after behavior is proven.
5. Full no-change review.
6. Delete `docs/work/*` temporary files.
7. Re-run clean candidate CI.
8. Merge normally to `main` and verify `main` CI.
9. Hand the shared gravity-service decision to the `chatgpt-editing` agent for reconciliation in its own workflow.
10. Do not publish a Scale release unless separately requested.
