"""Fresh JDK25 checks of actual S59 PREPARATION guards, optional source shadows.
Requires the public s59StackClasspath client-linking classpath; no Minecraft run.
Seeded passes exercise metadata/budget boundaries, not physical model authority.
"""
import argparse, os, subprocess, json, hashlib
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('classpath',type=Path);p.add_argument('output',type=Path);p.add_argument('--mutants',nargs='+',choices=['work-charge','backward-target','cursor-sharing','coverage','boolean-type']);a=p.parse_args()
r=Path(__file__).absolute().parent.parent;o=a.output.absolute();o.mkdir(parents=True,exist_ok=True);cp=a.classpath.absolute().read_text(encoding='utf-8');jdk=Path(os.environ['JAVA_HOME'])/'bin';proof=r/'tools/s59-adversarial-common/S59FiniteBoundaryProof.java';owner=r/'src/client/java/io/github/r3neer/scalebrews/client/collision/preparation/OriginalLinearPosePreparation.java'
def run(name,shadow=None):
 d=o/name;d.mkdir(exist_ok=True);classes=d/'classes';classes.mkdir(exist_ok=True)
 with (d/'compile.log').open('w',encoding='utf-8') as f:code=subprocess.run([str(jdk/'javac.exe'),'-cp',cp,'-d',str(classes),str(proof),*([str(shadow)] if shadow else [])],cwd=d,stdout=f,stderr=subprocess.STDOUT).returncode
 if code:raise RuntimeError('compile failed '+name)
 with (d/'result.log').open('w',encoding='utf-8') as f:code=subprocess.run([str(jdk/'java.exe'),'-cp',str(classes)+os.pathsep+cp,'io.github.r3neer.scalebrews.client.collision.preparation.S59FiniteBoundaryProof'],cwd=d,stdout=f,stderr=subprocess.STDOUT).returncode
 return code,(d/'result.log').read_text(encoding='utf-8')
code,text=run('baseline');assert code==0 and 'S59_FINITE_BOUNDARIES_PASS' in text,text;print(text,flush=True)
edits={'work-charge':('work += count;','work += 0;'),'backward-target':('target <= i || target >= template.instructions.size()','target >= template.instructions.size()'),'cursor-sharing':('return template.fork();','return template;'),'coverage':('if (coverage.get(key).nextClearBit(0) < code.instructions.size()) throw absent("finite_uncovered_program");','if (false) throw absent("finite_uncovered_program");'),'boolean-type':('if (!(value instanceof BooleanValue bool)) throw absent("boolean_type"); return bool;','return value instanceof BooleanValue bool ? bool : new BooleanValue(false);')}
results=[]
for name in a.mutants or []:
 anchor,repl=edits[name];source=owner.read_text(encoding='utf-8');assert source.count(anchor)==1,name;d=o/name;d.mkdir(exist_ok=True);shadow=d/owner.name;shadow.write_text(source.replace(anchor,repl),encoding='utf-8');code,text=run(name,shadow);kill=code==1 and 'AssertionError: S59 ADV' in text;results.append(dict(name=name,killed=kill,exit=code,sourceSha256=hashlib.sha256(owner.read_bytes()).hexdigest(),shadowSha256=hashlib.sha256(shadow.read_bytes()).hexdigest()));print(name,kill,flush=True)
(o/'mutation-results.json').write_text(json.dumps(results,indent=2)+'\n',encoding='utf-8');assert all(x['killed'] for x in results),'survivor or infrastructure failure'
