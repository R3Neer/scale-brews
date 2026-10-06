"""Replay S61 captured original matrices and run synthetic source-locality counters in a client-veto JVM.
The synthetic counter actor is not an original actor extraction oracle; use the native lane for that claim.
Generate common classpath with tools/s61_adversarial.init.gradle s61CommonClasspath.
"""
import argparse, os, shutil, subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('classpath',type=Path);p.add_argument('capture',type=Path);p.add_argument('output',type=Path);a=p.parse_args()
a.classpath=a.classpath.absolute();a.capture=a.capture.absolute();a.output=a.output.absolute();a.output.mkdir(parents=True,exist_ok=True)
source=Path(__file__).absolute().parent/'s61-adversarial-common';classes=a.output/'classes';classes.mkdir(exist_ok=True)
def binary(n):
 home=os.environ.get('JAVA_HOME');candidate=Path(home)/'bin'/(n+'.exe') if home else None
 found=str(candidate) if candidate and candidate.exists() else shutil.which(n)
 if not found:raise RuntimeError('JDK25 missing '+n)
 return found
cp=a.classpath.read_text(encoding='utf-8')
commands=[('compile.log',[binary('javac'),'-cp',cp,'-d',str(classes),str(source/'S61Bootstrap.java'),str(source/'S61Consumer.java')]),('result.log',[binary('java'),'-cp',str(classes),'S61Bootstrap',str(a.classpath),str(classes),str(a.capture),str(a.output)])]
for name,command in commands:
 with (a.output/name).open('w',encoding='utf-8') as log:r=subprocess.run(command,cwd=a.output,stdout=log,stderr=subprocess.STDOUT)
 if r.returncode:raise RuntimeError(name+' exit '+str(r.returncode))
print((a.output/'result.log').read_text(encoding='utf-8'))
