"""Replay fresh finite resource capture in a common-only classloader with client veto."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess

p=argparse.ArgumentParser()
p.add_argument("classpath",type=Path)
p.add_argument("capture",type=Path)
p.add_argument("output",type=Path)
a=p.parse_args()
a.classpath=a.classpath.absolute(); a.capture=a.capture.absolute(); a.output=a.output.absolute()
a.output.mkdir(parents=True,exist_ok=True)
source=Path(__file__).absolute().parent/"s60-implementer-common"
classes=a.output/"classes";classes.mkdir(exist_ok=True)
def binary(name):
    home=os.environ.get("JAVA_HOME")
    candidate=Path(home)/"bin"/(name+".exe") if home else None
    result=candidate if candidate and candidate.exists() else shutil.which(name)
    if not result: raise RuntimeError("JDK25 unavailable: "+name)
    return str(result)
commands=[
    [binary("javac"),"-cp",a.classpath.read_text(encoding="utf-8"),"-d",str(classes),str(source/"S60FiniteConsumer.java"),str(source/"S60FiniteBootstrap.java")],
    [binary("java"),"-cp",str(classes),"S60FiniteBootstrap",str(a.classpath),str(classes),str(a.capture)],
]
for name,command in zip(("compile.log","result.log"),commands):
    with (a.output/name).open("w",encoding="utf-8") as log: result=subprocess.run(command,cwd=a.output,stdout=log,stderr=subprocess.STDOUT)
    if result.returncode:raise RuntimeError(f"Common replay failed: {name}, exit {result.returncode}")
print((a.output/"result.log").read_text(encoding="utf-8"))
