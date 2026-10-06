# S57 common replay

Generate the main classpath with `s57CommonClasspath` from
`tools/s57_implementer.init.gradle`. With JDK25 in `JAVA_HOME`, run:

```
python tools/s57_common_replay.py <classpath-file> <captured-programs.json> <output>
```

Uses a fresh classloader that forbids both client namespaces. Original S56
captures are consumed as neutral data through `NumericPoseEngine`; matrices,
declared inputs, the old Resources SAM and bind-only resource lookup are checked.
This does not establish actor inputs, renderer roots, materials or FULL coverage.
