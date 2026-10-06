# S61 IMPLEMENTER evidence

The candidate projects requested original Living input fields at `partialTicks=1`.
It does not install a runtime binding or prove server animation-clock evolution.
`verification.json` records exact sources, artifacts, counts and remaining limits.

From the repository, with Java 25 in `JAVA_HOME`:

```powershell
.\gradlew.bat runClientGameTest s61CommonClasspath --offline -I tools/s61_implementer.init.gradle '-PscalebrewsBuildDir=../s61-impl-build'
.\gradlew.bat runGameTest --offline -I tools/s61_implementer.init.gradle '-PscalebrewsBuildDir=../s61-impl-build' '-PscalebrewsGameTestReport=../s61-impl-server.xml'
C:/Python314/python.exe tools/s61_implementer_common.py ../s61-impl-classpath.txt ../s61-impl-run/s61-implementation/evidence.json ../s61-impl-common
C:/Python314/python.exe tools/s61_implementer_snapshots.py ../s61-impl-common/pose-export.json ../s61-impl-common/common-matrices.json ../s61-impl-snapshots
```

Native capture creates four original actors and renderers, eight fixed cases each.
The fixture owns supplied actor inputs; expected scalars come from complete original
`extractRenderState`. The original Cow model supplies the representative local matrix
oracle, without certifying renderer model-slot selection, root or material inclusion.
Cases cover walking, upside-down baby, large historical angles, dead actor with stale
invalid walk data, two Living vehicle orientations, nonliving vehicle and combined
frozen/water/spin flags. Native scalar comparison preserves raw float bits.

The server lane constructs common actors independently from the same fixed input
fixture and compares every requested scalar with the captured native output. The
fresh JVM separately vetoes all client classes, proves cold source binding without
executing actors, and replays the new Cow matrix capture with the unchanged S60
finite consumer. These are distinct evidence levels.

Both failed native fixture attempts and their exact sources are retained. The
original setter rejects NaN; the original partial1 getter ignores historical pitch.
The final fault injection writes the actual current field and checks complete
absence. Production was unchanged through those repairs.

The two visual sheets compare all eight original/common states at fixed projection,
with local outlines, original projected centers and joint bases. They supplement
machine matrix comparisons and do not certify GPU rendering or world alignment.

Only new source/proof boundaries were executed. The ordinary generated GameTest
manifest must be restored after focused lanes; previous green suites and unrelated
Tiny Mounts tests were not repeated.
