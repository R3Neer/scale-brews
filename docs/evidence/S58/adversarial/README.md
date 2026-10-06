# S58 independent reproduction

Run from the repository root with JDK 25 (`JAVA_HOME`) and Python with Pillow.
Use independent build, project-cache and run paths; preserve prior outputs.

```powershell
.\gradlew.bat --no-daemon --project-cache-dir ../s58-public-cache -I tools/s58_adversarial.init.gradle '-PscalebrewsBuildDir=../s58-public-build' '-Ps58RunDir=../s58-public-run' runClientGameTest s58CommonClasspath s58StackClasspath
python tools/s58_adversarial_common.py ../s58-common-classpath.txt ../s58-public-run/s58-adversarial-snapshots/programs.json ../s58-public-common
python tools/s58_adversarial_stack.py ../s58-stack-classpath.txt ../s58-public-stack --mutants category2 top-identity receiver-identity stack-cap provenance
python tools/s58_adversarial_snapshots.py ../s58-public-run/s58-adversarial-snapshots/programs.json ../s58-public-common/common-matrices.json ../s58-public-snapshots
.\tools\s58_adversarial_mutations.ps1 -OutputDirectory ../s58-public-mutations -SkipBaseline
```

The default native selector runs S58 and the affected S56 oracle. The native
mutation driver uses `s58Only=true`, substitutes a source shadow, and requires a
semantic assertion failure; an invocation/compile failure is not a kill. The
five stack mutations use fresh JVMs and the client linking classpath, whereas the
numeric consumer uses only the main runtime classpath and actively vetoes client
classes. The stack proof does not claim a client veto or launch Minecraft.

`compound-comparison.svg/png` are the accepted deterministic offline views.
Each of four rows (body/fur, ordinary/aliased field) shows recentered per-piece
shapes and prints native/common projected centers. Recentring preserves shape
comparison, not whole-model spatial placement; printed centers and numeric
matrices retain displacement evidence. The original fixed-scale native SVG/PNG
are preserved under `classified/` as a cropped visualization attempt. No GPU,
root, layer rendering, live input source, material availability or FULL claim.

The unchanged 3e-6 matrix oracle is the primary numeric contract. Corner checks
retain their inherited absolute floor, then bound error using the actual matrix
difference times local corner radius plus one float ULP per output axis. Earlier
fixed absolute corner checks failed at Y=208.38 by one float ULP; attempts and
classification remain raw. No production repair or matrix tolerance relaxation.

The S58 common driver description was corrected from S56 to S58 after execution;
only its docstring changed. Timings are reported command totals, not CPU/TPS or
allocation measurements. Mutable run worlds, caches and builds are not deleted.
