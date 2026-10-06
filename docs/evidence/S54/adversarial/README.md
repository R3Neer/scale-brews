# S54 independent verification

Run from the repository with its required Java 25 environment. Use a separate
build, project cache and run directory. The init changes only processed GameTest
entrypoints, not the normal source manifest or production files.

```powershell
.\gradlew.bat -I tools/s54_adversarial.init.gradle '-PscalebrewsBuildDir=../s54-my-build' --project-cache-dir ../s54-my-cache '-Ps54RunDir=../s54-my-run' runClientGameTest
```

Expected: exit 0, clean Minecraft exit and S54_INDEPENDENT_RESET_PASS. Snapshots
are written to s54-adversarial-snapshots under the run directory. Native reset
compares nine actual ModelPart fields with immutable initial rows. The drawing
is a deterministic local-transform sketch, not a Minecraft renderer screenshot.

```powershell
python tools/s54_adversarial_snapshots.py ../s54-my-run/s54-adversarial-snapshots/reset-sequence.svg ../s54-my-run/s54-adversarial-snapshots/reset-sequence.png
& ./tools/s54_adversarial_mutations.ps1 -OutputDirectory ../s54-my-mutations
```

The mutation driver runs a positive baseline and six separate one-owner shadow
variants. Every killed mutant requires a native semantic S54 assertion; a compile
error or startup crash does not count. SkipBaseline is available only when the
unchanged fixture/candidate already has a recorded positive lane.

The path-frame mutant tests the private framed-row helper directly. Ambiguous
slash paths are already rejected by the inherited S45 extractor before complete
preparation; this helper result does not claim wider structure admission.

Scope: original Model.setupAnim/resetPose numeric local program, initial structure
and exact cache membership. No visibility/skipDraw, renderer/world root, layers,
material, transformed bytecode, actor association, executable engine/binding or
FULL/G6 closure. No Tiny or unrelated full regression was repeated.
