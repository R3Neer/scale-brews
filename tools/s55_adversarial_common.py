"""Consume a captured S55 DTO in a fresh classloader forbidding all client classes.
Generate the main runtime classpath with the public init's s55CommonClasspath task.
Requires JDK25 via JAVA_HOME or javac/java on PATH; no Minecraft client is launched.
"""
import argparse
import os
from pathlib import Path
import shutil
import subprocess

p = argparse.ArgumentParser()
p.add_argument("classpath", type=Path)
p.add_argument("geometry", type=Path)
p.add_argument("output", type=Path)
a = p.parse_args()
a.classpath = a.classpath.absolute()
a.geometry = a.geometry.absolute()
a.output = a.output.absolute()
a.output.mkdir(parents=True, exist_ok=True)
root = Path(__file__).absolute().parent
java_home = os.environ.get("JAVA_HOME")
def binary(name):
    candidate = Path(java_home) / "bin" / (name + ".exe") if java_home else None
    found = candidate if candidate and candidate.exists() else shutil.which(name)
    if not found:
        raise RuntimeError("JDK25 " + name + " unavailable")
    return str(found)
classes = a.output / "classes"
classes.mkdir(exist_ok=True)
cp = a.classpath.read_text(encoding="utf-8")
commands = [
    [binary("javac"), "-cp", cp, "-d", str(classes), str(root / "s55-common/S55CommonBody.java"), str(root / "s55-common/S55CommonBootstrap.java")],
    [binary("java"), "-cp", str(classes), "S55CommonBootstrap", str(a.classpath), str(classes), str(a.geometry)],
]
for name, command in zip(("compile.log", "result.log"), commands):
    with (a.output / name).open("w", encoding="utf-8") as log:
        result = subprocess.run(command, cwd=a.output, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode:
        raise RuntimeError(f"Common replay failed: {name}, exit {result.returncode}")
print((a.output / "result.log").read_text(encoding="utf-8"))
