# S61 independent focal verification

Use JDK 25 (JAVA_HOME) and Python 3 with Pillow. Run from the repository root.
Choose your own sibling build/cache/run/output directories. No full client/server
suite or Tiny Mounts lane is required for this boundary.

```powershell
.\gradlew.bat --project-cache-dir ../s61-public-cache '-PscalebrewsBuildDir=../s61-public-build' '-Ps61RunDir=../s61-public-run' '-Ps61ClasspathFile=../s61-public-common-cp.txt' '-Ps61ClientClasspathFile=../s61-public-client-cp.txt' -I tools/s61_adversarial.init.gradle runClientGameTest s61CommonClasspath s61ClientClasspath
python tools/s61_adversarial_common.py ../s61-public-common-cp.txt ../s61-public-run/s61-adversarial-snapshots/pose-cases.json ../s61-public-common
python tools/s61_adversarial_cold.py ../s61-public-common-cp.txt ../s61-public-client-cp.txt ../s61-public-cold
python tools/s61_adversarial_cold.py ../s61-public-common-cp.txt ../s61-public-client-cp.txt ../s61-public-cold-mutant --mutant
python tools/s61_adversarial_mutations.py ../s61-public-common-cp.txt ../s61-public-run/s61-adversarial-snapshots/pose-cases.json ../s61-public-mutations
python tools/s61_adversarial_snapshots.py ../s61-public-common/pose-comparison.json ../s61-public-snapshots
.\gradlew.bat --project-cache-dir ../s61-public-cache '-PscalebrewsBuildDir=../s61-public-build' processGametestResources --rerun-tasks
```

The last command restores generated resources without running a suite. The init
changes only generated entrypoints; source fabric.mod.json remains intact.
The mutation driver accepts --names to replay one named mutation. A kill requires
successful compilation followed by its semantic assertion; bootstrap/compile
failures are infrastructure failures and do not count. Shadow sources never
replace production files. Java argfiles avoid Windows command length limits.

Native proof: exact scalar comparison against original extractRenderState at
partial=1 on three original actors, plus Cow/Pig native matrices. Common proof:
replay of captured scalars/matrices with client classes vetoed; its counter actor
is synthetic and does not establish original extraction or a server clock.
Cold proof bypasses the public thread gate only to audit original class-resource
admission; live native proof covers the actual public preparation entrypoint.
Snapshots are offline model-local envelopes/joint origins, not GPU, root, layers,
material or FULL. Caller confinement, clock evolution and authoritative runtime
integration remain unproven. O(C) sample staging/boxing/copies are declared; the
counter check establishes alias memoization, not zero allocation or TPS.
