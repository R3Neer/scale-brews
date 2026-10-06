# S59 independent focal reproduction

Use JDK25 (`JAVA_HOME`) and Python/Pillow. Run from the repository root with
independent build/project-cache/run directories and retain prior outputs.

```powershell
.\gradlew.bat --no-daemon --project-cache-dir ../s59-public-cache -I tools/s59_adversarial.init.gradle '-PscalebrewsBuildDir=../s59-public-build' '-Ps59RunDir=../s59-public-run' runClientGameTest s59CommonClasspath s59StackClasspath
python tools/s59_adversarial_common.py ../s59-common-classpath.txt ../s59-public-run/s59-adversarial-snapshots/programs.json ../s59-public-common
python tools/s59_adversarial_boundaries.py ../s59-stack-classpath.txt ../s59-public-boundaries --mutants work-charge backward-target cursor-sharing coverage boolean-type
python tools/s59_adversarial_snapshots.py ../s59-public-run/s59-adversarial-snapshots/programs.json ../s59-public-common/common-matrices.json ../s59-public-snapshots
.\tools\s59_adversarial_mutations.ps1 -OutputDirectory ../s59-public-mutations -SkipBaseline
```

The default native selector includes only S59 and S58's affected compiler seam;
the mutation driver selects only S59. Four complete original predicate rows,
two field-alias configurations and five explicit float inputs yield40 samples,
280 original matrices and80 material cubes. The original Adult write-visible
program is rejected, and strict linear/reset interfaces retain their vetoes.
No actual actor-state source, baby eligibility, renderer/root/layer or FULL claim.
The Baby witness's visual zero-thickness legs are not material convex pieces.

Fresh standalone boundary JVM uses client classes for linking only, not a
Minecraft run or client veto:15 assertions cover budget admission/charge,
forward/backward targets, cursor independence, coverage and typed bool guards.
Seeded internal pass objects deliberately exercise negative metadata boundaries;
they are not substitutes for the original-model differential oracle. Its five
source shadows require semantic AssertionError after valid compilation. Native
branch-direction shadow is the sixth semantic kill, reported56s/nativeexit1;
PowerShell driver aggregateexit0. Invocation/compile errors never count as kills.

The common consumer receives the newly captured eight neutral programs and
actively vetoes all client namespaces while reproducing280 matrices. Matrix
oracle remains3e-6; corners retain S58's measured matrix forward-error plus ULP
contract. Four sets of projected material outlines show local shape/centers;
`joint-bases.svg/png` separately compare all seven articulated bases across eight
rows, exposing branch/alias differences even for excluded planar legs. Native
solid/common dashed, axes red/green/blue; sample3 and printed N/C centers fixed.
These are offline recentered projections, not GPU or whole-model root pictures.

RED1 production/PREPARATION budget order is preserved in adversarial-red1; the
unchanged revealed oracle now returns work1048576/cache0/coverage0. One native
ADVER_FIXTURE failure attempted to construct NaN Inputs before evaluation; its
constructor correctly rejects them. The classified source/log is retained and
the fixture now asserts rejection at that actual contract boundary. No numeric
or original-model oracle weakened. All times are command totals, not CPU/TPS.
No unrelated Tiny/full-client/server/catalog/protocol suites were run.
