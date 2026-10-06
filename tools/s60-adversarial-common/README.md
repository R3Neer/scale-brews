# S60 independent focused replay

ADVERSARY production freeze: protocol8/schema1 finite numeric families only.
Requires project dependencies and JDK25 through JAVA_HOME. All output paths below
are separate from the source tree; neither the tools nor the test alter production.

From repository root (PowerShell):

```powershell
.\gradlew.bat --no-daemon --project-cache-dir ../s60-public-cache '-PscalebrewsBuildDir=../s60-public-build' -I tools/s60_adversarial.init.gradle '-Ps60RunDir=../s60-public-run' '-Ps60ClasspathFile=../s60-public-classpath.txt' '-Ps60ClientClasspathFile=../s60-public-client-classpath.txt' compileGameTestJava s60CommonClasspath s60ClientClasspath runClientGameTest
python tools/s60_adversarial_common.py ../s60-public-classpath.txt ../s60-public-run/s60-adversarial-snapshots/tables.json ../s60-public-run/s60-adversarial-snapshots/programs.json ../s60-public-common
python tools/s60_adversarial_catalog.py ../s60-public-classpath.txt ../s60-public-run/s60-adversarial-snapshots/tables.json ../s60-public-run/s60-adversarial-snapshots/programs.json ../s60-public-catalog
python tools/s60_adversarial_boundaries.py ../s60-public-classpath.txt ../s60-public-client-classpath.txt ../s60-public-run/s60-adversarial-snapshots ../s60-public-boundaries
python tools/s60_adversarial_snapshots.py ../s60-public-run/s60-adversarial-snapshots/programs.json ../s60-public-common/common-matrices.json ../s60-public-snapshots
```

The common and catalog JVMs forbid all Minecraft/Scale client classes. The export
boundary JVM intentionally links client preparation, launches no Minecraft, and
claims no client veto. Its constructed metadata-only fixtures test field remapping
and union64/65; original model authority is provided separately by the native test.

Optional focal sensitivity uses `--mutants` on the boundary driver; `--names`
selects only changed cases. Already-green sensitivity does not require repetition
for documentation changes. Six current source shadows: signed-zero, mask-order,
resource-capture, field-alias, union-budget, inactive-eager. Compilation failures
are infrastructure failures, not semantic kills; success requires AssertionError.

Native reference: BabyTurtle original, all four typed assignments, two actual part
aliases, all nine initial/current fields and draw flags preserved; 40 samples,
280 joint matrices and 80 cubes. Common selects the new exported finite family;
maximum 32 masks and inactive undefined arithmetic are neutral runtime controls.
Catalog uses actual RegistryFriendlyByteBuf codec and accepted snapshot/Bound
identity under malformed replacements. The temporary init changes only generated
GameTest resources in its build directory, never the normal source manifest.

Snapshots fix sample3 and eight predicate/alias rows. Shape panels recenter each
cube and retain projected native/common centers. Joint axes expose zero-thickness
animated parts without claiming they are material boxes. Both PNGs are inspected;
these are local-envelope/joint evidence, not GPU/render inclusion/root/material/FULL.

No Tiny Mounts or unrelated full suites. Cost evidence is structural: cold unique
resource lookups, no hot lookups, detached closures, bounded schemas/rows/resources,
200k family evaluations and six semantic shadows; no TPS/zero-allocation claim.
