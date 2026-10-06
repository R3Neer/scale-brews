"""Reproducible fresh common cold linking/absence/owner checks, not native actor execution."""
import argparse
import os
import subprocess
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('classpath', type=Path)
p.add_argument('output', type=Path)
a = p.parse_args()
a.output = a.output.absolute()
a.output.mkdir(parents=True, exist_ok=True)
tools = Path(__file__).absolute().parent
java = Path(os.environ['JAVA_HOME']) / 'bin'
classes = a.output / 'classes'
classes.mkdir(exist_ok=True)
commands = [[str(java / 'javac.exe'), '-cp', a.classpath.read_text().strip(), '-d', str(classes),
             str(tools / 's62-implementer-common/S62Bootstrap.java'), str(tools / 's62-implementer-common/S62Common.java')],
            [str(java / 'java.exe'), '-cp', str(classes), 'S62Bootstrap', str(a.classpath.absolute()), str(classes)]]
for index, cmd in enumerate(commands):
    result = subprocess.run(cmd, capture_output=True, text=True)
    (a.output / ('compile.log' if index == 0 else 'cold.log')).write_text(result.stdout + result.stderr, encoding='utf-8')
    print(result.stdout + result.stderr, end='')
    if result.returncode:
        raise SystemExit(result.returncode)
