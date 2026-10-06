# S57 public independent replay

Role ADVERSARY. Scope: prepared numeric resource/engine, catalog transaction and protocol7 transport. No new actor/root/material/FULL claim.

Use JDK25 (`JAVA_HOME`) and Python. From the repository, use your own sibling output paths:

```powershell
.\gradlew.bat --no-daemon --project-cache-dir ..\s57-public-cache -I tools\s57_adversarial.init.gradle '-PscalebrewsBuildDir=../s57-public-build' '-Ps57RunDir=../s57-public-run' '-Ps57ClasspathFile=../s57-public-classpath.txt' runGameTest s57CommonClasspath
python tools/s57_adversarial_common.py ../s57-public-classpath.txt docs/evidence/S56/adversarial/programs.json ../s57-public-common
python tools/s57_adversarial_mutations.py ../s57-public-classpath.txt docs/evidence/S56/adversarial/programs.json ../s57-public-mutations
```

Server selector runs only the three affected S18 classes, 16 tests; it does not run Tiny Mounts or a client suite. Common proof uses a fresh classloader forbidding client classes, a real RegistryFriendlyByteBuf codec, revision-local catalog and the immutable original S56 capture. No Minecraft client launch is required for unchanged captured original matrices.

The mutation driver compiles source shadows in independent outputs and exits nonzero unless every selected mutation is killed by an S57 assertion. Optional `--mutants protocol` repeats only the peer-admission guard. Run the green baseline first. The initial constant-replacement protocol experiment was invalid because already compiled callers retained the inlined constant; its source/log/driver remain in classified-protocol-inline and are not counted as a semantic kill. Final eight kills combine seven valid initial experiments with the corrected peer guard alone. Existing production and original capture are never edited.

Reference visual: ../S56/adversarial/linear-pose.svg and .png. Inspected again for S57. It is a deterministic local envelope projection; complete 140 matrix comparisons and exact DTO/wire equality carry the new transport claim. It is not renderer/root/material evidence. Runtime/resource/cube/actor coverage outside this scope remains open.
