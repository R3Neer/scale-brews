# S60 IMPLEMENTER candidate evidence

Only explicit-input finite selection and revision-owned resources are tested.
This is not actor input authority, world-root/render inclusion or FULL coverage.
The numeric arithmetic evaluator remains the existing implementation.

JDK25: `C:/Users/Usuario/.jdks/ms-25.0.3`; `JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true`.
From the repository, the executed focal lanes were:

```powershell
.\gradlew.bat compileGametestJava -I tools/s60_implementer.init.gradle '-PscalebrewsBuildDir=../s60-impl-build' --offline
.\gradlew.bat runGameTest -I tools/s60_implementer.init.gradle '-PscalebrewsBuildDir=../s60-impl-build' --offline
.\gradlew.bat runClientGameTest s60CommonClasspath -I tools/s60_implementer.init.gradle '-PscalebrewsBuildDir=../s60-impl-build' --offline
C:/Python314/python.exe tools/s60_implementer_common.py ../s60-impl-classpath.txt ../s60-impl-run/s60-implementation/exports.json ../s60-impl-common
.\gradlew.bat build -x runGameTest processGametestResources --rerun-tasks '-PscalebrewsBuildDir=../s60-impl-build' --offline
```

Server selectors: S60FiniteCatalogTests, S57NumericCatalogTests, S04G1IntegrationTests.
Client selectors: S60OriginalFiniteResourceProof, S59OriginalFinitePoseProof.
The final build restored the ordinary processed manifest; its suite was not run.
No Tiny Mounts, full server/client battery or historical green mutation campaign
was executed. See raw logs for OSHI/Realms/graphics startup diagnostics and results.

`exports.json` captures original BabyTurtle, its actual shared-leg alias and Cow:
all nine assignments, four deterministic samples per assignment, original native
matrices and newly exported finite/numeric resources. Geometry excludes native
zero-thickness polygons from material cubes, while matrices cover every joint.
`common-common-matrices.json` is replayed through the new finite engine in a fresh
classloader that vetoes Minecraft and Scale client classes. Field metadata stays
unloaded JSON; the replay consumes common DTOs only. The S59 affected capture is
semantically exact to the committed S59 capture.

`finite-engine-retention.javap.txt` is an offline bytecode-signature readout, not a
new product test: the evaluation lambda captures String[] and Bound[] only.
The frozen receipt records production/source boundaries, artifacts and JAR hash.
Independent acceptance passes: six semantic kills, native/common/codec/rollback
controls and fresh snapshots. Own nine-assignment and public eight-row images
were generated and visually inspected. Public common replay passes280 matrices/
201362 assertions; public catalog passes140 matrices/272 assertions.
Post-execution driver changes affect docstrings/line endings only; executable
Python AST and Java text match the preserved executed sources. No green suite
was repeated for that cleanup.
