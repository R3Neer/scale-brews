"""Semantic mutation replay of the common S56 numeric evaluator; no client launch."""
import argparse,os,subprocess,json,hashlib
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('classpath',type=Path);p.add_argument('snapshots',type=Path);p.add_argument('output',type=Path);a=p.parse_args()
r=Path(__file__).absolute().parent.parent;o=a.output.absolute();o.mkdir(parents=True,exist_ok=True);cp=a.classpath.absolute().read_text();src=r/'src/main/java/io/github/r3neer/scalebrews/collision/pose/NumericPoseProgram.java';text=src.read_text();java=Path(os.environ['JAVA_HOME'])/'bin';results=[]
edits={
'arithmetic':('value = values[n.left()] * values[n.right()];','value = values[n.left()] + values[n.right()];'),
'finite-before-lut':('if (!Float.isFinite(value)) return Optional.empty();',''),
'exact-bind':('if (!geometry.equals(candidate)) return Optional.empty();',''),
'initial-field':('initials.get(node.part())[node.field()]','initials.get(node.part())[0]'),
'ordinary':('if (inputs == null || !inputs.ordinary())','if (inputs == null)'),
'negative-zero':('case NEGATE -> value = -values[n.left()];','case NEGATE -> value = values[n.left()];')}
for name,(anchor,replacement) in edits.items():
 if text.count(anchor)!=1:raise RuntimeError('anchor '+name)
 d=o/name;d.mkdir(exist_ok=True);shadow=d/'NumericPoseProgram.java';shadow.write_text(text.replace(anchor,replacement));classes=d/'classes';classes.mkdir(exist_ok=True)
 sources=[shadow,r/'tools/s56-adversarial-common/S56IndependentNumericProof.java',r/'tools/s56-adversarial-common/S56NumericBootstrap.java']
 with (d/'compile.log').open('w') as log:c=subprocess.run([str(java/'javac.exe'),'-cp',cp,'-d',str(classes),*[str(s) for s in sources]],cwd=d,stdout=log,stderr=subprocess.STDOUT)
 if c.returncode:raise RuntimeError('compile '+name)
 classpath=d/'classpath.txt';classpath.write_text(cp)
 with (d/'result.log').open('w') as log:c=subprocess.run([str(java/'java.exe'),'-cp',str(classes),'S56NumericBootstrap',str(classpath),str(classes),str(a.snapshots.absolute())],cwd=d,stdout=log,stderr=subprocess.STDOUT)
 log=(d/'result.log').read_text();kill=c.returncode!=0 and 'AssertionError: S56 ' in log
 results.append(dict(name=name,killed=kill,exit=c.returncode,sourceSha256=hashlib.sha256(src.read_bytes()).hexdigest(),shadowSha256=hashlib.sha256(shadow.read_bytes()).hexdigest()))
 print(name,kill)
(o/'mutation-results.json').write_text(json.dumps(results,indent=2))
if not all(x['killed'] for x in results):raise RuntimeError('survivor/infrastructure failure')
