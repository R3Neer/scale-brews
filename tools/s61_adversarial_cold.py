"""Run S61 cold resource proof and an optional unsupported-field mutant in isolated class output.
This uses the private prepare seam: the live public API/thread checks belong to the native lane.
No Minecraft world is launched. No production source is modified.
"""
import argparse,os,shutil,subprocess,json,hashlib,time
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('commonClasspath',type=Path);p.add_argument('clientClasspath',type=Path);p.add_argument('output',type=Path);p.add_argument('--mutant',action='store_true');a=p.parse_args()
root=Path(__file__).absolute().parent.parent;a.output=a.output.absolute();a.output.mkdir(parents=True,exist_ok=True);classes=a.output/'classes';classes.mkdir(exist_ok=True)
cp=os.pathsep.join(dict.fromkeys((a.commonClasspath.absolute().read_text(encoding='utf-8')+os.pathsep+a.clientClasspath.absolute().read_text(encoding='utf-8')).split(os.pathsep)));source=root/'tools/s61-adversarial-common/S61ColdProof.java';candidate=root/'src/client/java/io/github/r3neer/scalebrews/client/collision/preparation/OriginalLivingInputPreparation.java'
files=[source]
if a.mutant:
 text=candidate.read_text(encoding='utf-8');old='default -> throw absent("unsupported_field");';assert text.count(old)==1
 shadow=a.output/'OriginalLivingInputPreparation.java';shadow.write_text(text.replace(old,'default -> LivingPoseInputSource.Source.AGE_IN_TICKS;'),encoding='utf-8');files.append(shadow)
def binary(n):
 home=os.environ.get('JAVA_HOME');p=Path(home)/'bin'/(n+'.exe') if home else None
 found=str(p) if p and p.exists() else shutil.which(n)
 if not found:raise RuntimeError('JDK25 missing '+n)
 return found
compile_args=a.output/'compile.args';compile_args.write_text('\n'.join(json.dumps(x) for x in ['-cp',cp,'-d',str(classes),*[str(x) for x in files]])+'\n',encoding='utf-8')
with (a.output/'compile.log').open('w',encoding='utf-8') as log:r=subprocess.run([binary('javac'),'@'+str(compile_args)],cwd=a.output,stdout=log,stderr=subprocess.STDOUT)
result={'mutant':a.mutant,'compileExit':r.returncode,'candidateSha256':hashlib.sha256(candidate.read_bytes()).hexdigest(),'killed':False}
if not r.returncode:
 start=time.monotonic()
 run_args=a.output/'run.args';run_args.write_text('\n'.join(json.dumps(x) for x in ['-cp',str(classes)+os.pathsep+cp,'S61ColdProof'])+'\n',encoding='utf-8')
 with (a.output/'result.log').open('w',encoding='utf-8') as log:r=subprocess.run([binary('java'),'@'+str(run_args)],cwd=a.output,stdout=log,stderr=subprocess.STDOUT)
 text=(a.output/'result.log').read_text(encoding='utf-8');result.update(runtimeExit=r.returncode,seconds=time.monotonic()-start,killed=a.mutant and r.returncode!=0 and 'AssertionError: S61 ADV unsupported field admitted' in text,passed=not a.mutant and r.returncode==0 and 'S61_COLD_PROOF_PASS' in text)
(a.output/'verification.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(json.dumps(result,indent=2));raise SystemExit(0 if result.get('killed') or result.get('passed') else 1)
