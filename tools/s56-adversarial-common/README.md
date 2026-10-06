S56 scoped regression, JDK25. Run the native baseline before mutation drivers.
All outputs, classpaths, builds and runs should be absolute paths outside the repo.

Native: gradlew -I tools/s56_adversarial.init.gradle -PscalebrewsBuildDir=<build>
--project-cache-dir <cache> -Ps56RunDir=<run> runClientGameTest s56CommonClasspath
-Ps56ClasspathFile=<classpath.txt>. Only S56AdversarialLinearProof is selected.

Common: python tools/s56_adversarial_common.py <classpath.txt>
<run>/s56-adversarial-snapshots/programs.json <output>.

Common mutations: python tools/s56_adversarial_common_mutations.py <classpath.txt>
<programs.json> <output>. Six semantic mutations compile into output/classes;
production source is never edited. The baseline must already pass.

Client mutations: tools/s56_adversarial_mutations.ps1 -OutputDirectory <output>.
Optional -SkipBaseline is valid only after a green identical public oracle.
Two mutations operate in client source shadows. The read-after-write/NOP cases
are a declared ClassModel fault seam, not original-resource admission evidence.
Semantic assertion kills are required; compilation or infrastructure errors do
not count. Expected negative JVM/Gradle exits are normalized to aggregate exit0.

Common launcher vetoes all client package loading and uses output as JVM cwd.
Snapshot SVG is an aggregate local projection envelope, not renderer/FULL proof.
