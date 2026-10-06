"""Fresh common JVM: cold input binding and eight newly captured original Cow poses."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess

p = argparse.ArgumentParser()
p.add_argument('classpath', type=Path); p.add_argument('capture', type=Path); p.add_argument('output', type=Path)
a = p.parse_args()
a.classpath = a.classpath.absolute(); a.capture = a.capture.absolute(); a.output = a.output.absolute()
a.output.mkdir(parents=True, exist_ok=True)
tools = Path(__file__).absolute().parent
classes = a.output/'classes'; classes.mkdir(exist_ok=True)
def binary(name):
    home = os.environ.get('JAVA_HOME')
    candidate = Path(home)/'bin'/(name+'.exe') if home else None
    result = candidate if candidate and candidate.exists() else shutil.which(name)
    if not result: raise RuntimeError('JDK25 unavailable: '+name)
    return str(result)
commands = [
    [binary('javac'), '-cp', a.classpath.read_text(encoding='utf-8'), '-d', str(classes),
     str(tools/'s61-implementer-common/S61InputConsumer.java'), str(tools/'s61-implementer-common/S61InputBootstrap.java'),
     str(tools/'s60-implementer-common/S60FiniteConsumer.java')],
    [binary('java'), '-cp', str(classes), 'S61InputBootstrap', str(a.classpath), str(classes), str(a.capture)],
]
for name, command in zip(('compile.log', 'result.log'), commands):
    with (a.output/name).open('w', encoding='utf-8') as log:
        result = subprocess.run(command, cwd=a.output, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode: raise RuntimeError(f'Common replay failed: {name}, exit {result.returncode}')
print((a.output/'result.log').read_text(encoding='utf-8'))
