# S20 — Citadel runtime and authoring guide

Status: **adversarial documentation / I10 PASS**. This document describes the intended reusable S20 contract and the limits implemented by the current common-side program engine. It does **not** declare S20 closed by itself. The canonical executable-binding path remains gated by the independent S20 I9 holdout.

## 1. Ownership and runtime boundary

S20 splits one Citadel/Alex-style model into revision-owned neutral data:

- `ModelGeometry` owns extracted hierarchy, convex pieces, model transform and exact `SourcePose` values;
- `CitadelPoseProgram` owns procedural helper calls, conditions and `ModelAnimator` clips;
- `CollisionBinding.Pose` selects the shared `scalebrews:citadel_program` engine, names the program through the `program` parameter and explicitly declares every custom channel the program may read;
- `CollisionBinding.Geometry` selects the geometry family/model independently;
- `root_transform` remains separate root authority;
- `WorldAnatomyCatalog` owns accepted geometry/program/binding snapshots and transfers them atomically by revision.

After bind, HOT_TICK evaluation must use only the compiled `PoseEngine.Bound`, authoritative scalar inputs and prepared `SourcePose` data. JSON parsing, resource lookup, renderer traversal, Citadel/Alex classes and reflective method discovery are preparation concerns, not pose-evaluation work.

The ordinary Scale Brews JAR has no required Alex's Mobs or Citadel dependency. Optional external state is sampled through a neutral `PoseChannelAdapter`; the built-in Alex adapter is discovered only when the locked external mod/version is present.

## 2. Fail-closed rules

A Citadel binding is unavailable rather than approximated when any indispensable input cannot be proven. In particular:

- missing/unknown program;
- missing target bone or exact `SourcePose`;
- undeclared program-implied channel;
- missing authoritative channel at evaluation time;
- non-finite channel output;
- unknown animation token;
- unsupported primitive or invalid program data;
- condition structure beyond the program-wide node budget.

External channel publication is transactional: an adapter that emits a valid prefix and later produces an invalid value, duplicate ownership, channel conflict, exception or budget overflow must publish none of its partial output.

## 3. Program limits

`CitadelPoseProgram` schema 1 is deliberately not a general scripting language. Current hard bounds are:

- at most **128 clips**;
- at most **4,096 keyframes** across the program;
- at most **8,192 keyframe deltas** across the program;
- at most **2,048 procedural operations**;
- compound-condition depth at most **16** and at most **32 direct terms** per `ALL`/`ANY` node;
- at most **4,096 condition nodes in total across the whole program**, summed across every operation;
- at most **64 required channels**;
- at most **64 bones** in one `FACE_TARGET` operation;
- at most **512 deltas** in one keyframe;
- at most **1,024 keyframes** in one clip;
- individual keyframe duration and total clip duration are bounded to **1,000,000 ticks**;
- scalar/operation constants are finite and bounded; keyframe rotation/position deltas are finite and bounded.

The global condition budget was added by `b3e2c47` after the adversarial CPU probe demonstrated that the previous depth/fan-out limits still admitted a 33,825-node tree costing roughly **0.296 ms/evaluation** for condition traversal alone. The behavior-only workflow `s20-adversarial-condition-budget`, run **35070996485**, passes without inspecting implementation details: it discovers the accepted boundary, proves `boundary + 1` fails closed, and proves the budget is **program-wide** by aggregating nodes across multiple individually valid operations.

The evaluator compiles clip/keyframe data during bind and uses binary search for keyframe selection. The adversarial allocation probe found approximately linear allocation growth, not a combinatorial blow-up: roughly 3.90–4.18 KiB/sample for the pinned Gazelle program and 5.17–5.45 KiB/sample for the pinned Grizzly program on the measured CI/JVM lane. The CPU scaling probe likewise measured approximately linear operation cost through the schema maximum of 2,048 operations. Allocation remains material and worth monitoring.

## 4. Adding another Citadel/Alex-style species

For a species already expressible by the S20 dialect, adding support should be data/preparation work, not a new Java pose class.

1. **Pin the external input.** Record exact mod/library versions and immutable artifact hashes in the compatibility evidence used for acceptance.
2. **Extract geometry in client/tooling.** Use the reusable `AdvancedModelBox` preparation path to export hierarchy, pieces, model transform and exact local `SourcePose` values. Do not make the dedicated/server runtime instantiate the renderer/model.
3. **Author one neutral pose program.** Store the bounded program under the model owner's namespace in `scalebrews/citadel_pose_programs/<model>.json`. Use only S20 primitives. If the model needs a helper outside the frozen dialect, extend/review the reusable dialect instead of smuggling species code into JSON.
4. **Declare authoritative channels.** Every custom scalar read by an operation/condition must be declared by the canonical binding. If state comes from the external entity implementation, provide/reuse a neutral `PoseChannelAdapter` whose discovery happens outside HOT_TICK and whose per-sample call path is already compiled/bounded.
5. **Create the canonical binding.** Select the prepared geometry model, `scalebrews:citadel_program`, `parameters.program=<program id>`, the complete declared channel set, the appropriate independent root provider and policy. Geometry/pose/root choices must remain independently replaceable.
6. **Prove real-model parity.** Acceptance requires samples against the original pinned model/render implementation, including ordinary procedural state and every clip/condition class materially used by the exported program. Comparing only against copied formulas is insufficient.
7. **Prove catalog/runtime integration.** The accepted revision must preserve the program and binding through the catalog wire path and materialize an executable runtime binding without requiring the external classes on the common/dedicated classpath. The prepared binding must preserve the selected model, pose parameters, declared channels and root-provider identity, not merely place a placeholder entry in the executable map.
8. **Re-run fail-closed and performance gates.** Unknown/missing channels/programs/bones must remain unavailable, `jointEvaluations` must remain at most once per causal sample, condition-node budget overflow must fail closed, and any materially larger program should be measured rather than assumed cheap.

## 5. Pinned S20 reference pair

S20 uses Alex's Mobs Continued `alexsmobs` 2.1.9 as its pinned family proof, with Grizzly Bear and Gazelle as the minimum two-model pair. The independent real-model oracle exercises four samples for each model, including ordinary procedural states and `ModelAnimator` clips, through the same `scalebrews:citadel_program` engine.

Those oracle results prove evaluator parity for the sampled model states. They do not, by themselves, prove that a canonical `CollisionBinding` is materialized into the executable runtime catalog. That distinct I9 property is intentionally protected by `S20AdversarialCanonicalBindingTests` and its dedicated workflow; S20 must remain open while that holdout is red. I10 boundedness is independently green via run `35070996485`.
