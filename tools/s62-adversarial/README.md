# S62 focal adversarial reproduction

Role: ADVERSARY. Run from the repository root with JDK 25 and Minecraft 26.2 dependencies already prepared. Use a separate build, project cache and run directory. Coordinate Gradle access with the other lane; these commands select only S62 tests and the harness.

```powershell
$env:JAVA_HOME = 'C:/path/to/jdk-25'
./gradlew.bat --project-cache-dir ../s62-replay-cache '-PscalebrewsBuildDir=../s62-replay-build' '-Ps62RunDir=../s62-replay-run' '-Ps62ClasspathFile=../s62-replay-classpath.txt' -I tools/s62_adversarial.init.gradle runGameTest s62CommonClasspath
```

Default selection: four S62 classes, six tests plus the always-pass harness. `-Ps62Classes=fully.qualified.Class` selects a smaller affected boundary. The selector changes generated resources only. To restore ordinary generated resources without executing gameplay:

```powershell
./gradlew.bat --project-cache-dir ../s62-replay-cache '-PscalebrewsBuildDir=../s62-replay-build' processGametestResources --rerun-tasks
```

After the positive baseline, Python with Pillow can render the temporal capture:

```powershell
python tools/s62_adversarial_snapshots.py ../s62-replay-run/s62-adversarial/clock-sequence.json ../s62-replay-snapshots
python tools/s62_adversarial_mutations.py ../s62-replay-classpath.txt ../s62-replay-mutants --build ../s62-replay-build --cache ../s62-replay-cache
```

Mutation tooling compiles new clock shadows, substitutes classes only in the selected build output, and restores backed-up classes in finally. `--names omit-gait` selects one mutant. A kill requires the named semantic assertion after successful compilation; infrastructure failures are not kills. Production sources are never changed.

The differential calls the original transformed Cow/Pig walk method on projected own movement and compares clock phase/speed bit exactly. Ledger transport is applied manually. The snapshots show temporal input curves, not rendered models, world roots, contact physics or synchronized server/client clocks. No Tiny Mounts or broad unrelated suites are selected. Closure ends S62; no subsequent sprint is authorized.
