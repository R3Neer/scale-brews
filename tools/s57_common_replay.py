"""Replay captured original numeric programs through the common revision resource engine."""
import argparse
import os
from pathlib import Path
import subprocess

p = argparse.ArgumentParser()
p.add_argument("classpath", type=Path)
p.add_argument("programs", type=Path)
p.add_argument("output", type=Path)
a = p.parse_args()
classpath, programs, output = (v.absolute() for v in (a.classpath, a.programs, a.output))
output.mkdir(parents=True, exist_ok=True)
classes = output / "classes"
classes.mkdir(exist_ok=True)
java = Path(os.environ["JAVA_HOME"]) / "bin"
source = Path(__file__).absolute().parent / "s57-common"
commands = [
    [str(java / "javac.exe"), "-cp", classpath.read_text(), "-d", str(classes),
     str(source / "S57NumericEngineReplay.java"), str(source / "S57NumericBootstrap.java")],
    [str(java / "java.exe"), "-cp", str(classes), "S57NumericBootstrap", str(classpath), str(classes), str(programs)],
]
for name, command in zip(("compile.log", "result.log"), commands):
    with (output / name).open("w", encoding="utf-8") as log:
        result = subprocess.run(command, cwd=output, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode:
        raise RuntimeError(f"S57 common replay failed in {name}: exit {result.returncode}")
print((output / "result.log").read_text())
