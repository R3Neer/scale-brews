# S59 scoped Code foundation audit

No production S59 or Minecraft runtime is executed. JDK25 emits and parses
classfile data; the existing production Code parser is exercised in a fresh JVM.
Compile against the frozen client runtime classpath (for example generate
`s58StackClasspath` only with its public init). Run from an independent output
folder, using absolute paths to classpath/source/classes.

```powershell
$cp = [IO.File]::ReadAllText('ABSOLUTE/client-classpath.txt')
& "$env:JAVA_HOME/bin/javac.exe" -cp $cp -d ABSOLUTE/classes ABSOLUTE/S59CodeFoundationProof.java
& "$env:JAVA_HOME/bin/java.exe" -cp ('ABSOLUTE/classes;' + $cp) io.github.r3neer.scalebrews.client.collision.preparation.S59CodeFoundationProof
```

Baseline 15 checks covers label -> instruction ordinal, forward conditional/goto,
parser exposure of backward and corrupted end targets, missing target, opcode
mismatch, full suffix/truncation, inclusive512/reject513, handler exclusion and
resource constants. Forward-only/range/type/whole-path verification belongs to
the proposed finite compiler and remains UNPROVEN until its candidate campaign.
`OriginalModelPrograms.java` is a source shadow: return target + 1. Compile it
with the same fixture into separate classes; native JVM exit1 and semantic
AssertionError are required for a kill. No classpath or compile failure counts.

Three development attempts are preserved as ADVER_FIXTURE: JDK stackmap generation
rejects a target at code-end; dropping stackmaps in that malformed construction
also removes target metadata; JDK patches unreachable NOP into a terminal opcode.
Final controls use a valid classfile and explicit map fault injection for end
range, and check unconsumed suffix irrespective of generator rewriting. This
changes no production assumption or oracle about instruction ordinals/absence.
Compiled foundations (24 classes) match the S58 JAR exactly. Prior reset/structure,
float/alias/read-after-write and neutral consumer evidence is reused only at its
unchanged scoped owner; no repeat of Tiny, catalog/server/protocol or broad suites.
