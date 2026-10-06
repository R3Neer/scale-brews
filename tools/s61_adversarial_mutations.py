"""Compile isolated S61 source mutants and run the unchanged common consumer.
Only successful compile + a S61 assertion is a kill; preserve full execution logs.
No repository production file is edited and no Minecraft client/Tiny suite is launched.
"""
import argparse, hashlib, json, os, subprocess, shutil, time
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('classpath',type=Path);p.add_argument('capture',type=Path);p.add_argument('output',type=Path);p.add_argument('--names',nargs='*');a=p.parse_args()
a.classpath=a.classpath.absolute();a.capture=a.capture.absolute();a.output=a.output.absolute();a.output.mkdir(parents=True,exist_ok=True)
root=Path(__file__).absolute().parent.parent
original=root/'src/main/java/io/github/r3neer/scalebrews/collision/pose/LivingPoseInputSource.java'
text=original.read_text(encoding='utf-8');cp=a.classpath.read_text(encoding='utf-8')
mutations={
 'alias-memo':('if ((computed & bit) != 0) return values[index];','if (false) return values[index];'),
 'eager-source':('var scratch = new Scratch(entity);','var scratch = new Scratch(entity); for (Source s : Source.values()) if (!Float.isFinite(scratch.value(s))) return Optional.empty();'),
 'partial-exception':('} catch (RuntimeException failure) {\n                return Optional.empty();','} catch (RuntimeException failure) {\n                return Optional.of(Map.of());'),
 'schema-65':('FORMAT = 1, MAX_CHANNELS = 64;','FORMAT = 1, MAX_CHANNELS = 65;'),
 'signed-zero':('value ? 1f : 0f','value ? 1f : -0f'),
 'nonfinite-prefix':('if (!Float.isFinite(value)) return Optional.empty();','if (false) return Optional.empty();'),
}
home=os.environ.get('JAVA_HOME')
def binary(name):
 candidate=Path(home)/'bin'/(name+'.exe') if home else None
 found=str(candidate) if candidate and candidate.exists() else shutil.which(name)
 if not found:raise RuntimeError('JDK25 missing '+name)
 return found
consumer=root/'tools/s61-adversarial-common'
results=[]
for name in a.names or mutations:
 old,new=mutations[name];assert text.count(old)==1,name
 target=a.output/name;target.mkdir(exist_ok=True);classes=target/'classes';classes.mkdir(exist_ok=True)
 source=target/'LivingPoseInputSource.java';source.write_text(text.replace(old,new),encoding='utf-8')
 command=[binary('javac'),'-cp',cp,'-d',str(classes),str(source),str(consumer/'S61Consumer.java'),str(consumer/'S61Bootstrap.java')]
 with (target/'compile.log').open('w',encoding='utf-8') as log:r=subprocess.run(command,cwd=target,stdout=log,stderr=subprocess.STDOUT)
 entry={'name':name,'compileExit':r.returncode,'sourceSha256':hashlib.sha256(source.read_bytes()).hexdigest(),'consumerSha256':hashlib.sha256((consumer/'S61Consumer.java').read_bytes()).hexdigest(),'killed':False}
 if not r.returncode:
  start=time.monotonic()
  command=[binary('java'),'-cp',str(classes),'S61Bootstrap',str(a.classpath),str(classes),str(a.capture),str(target)]
  with (target/'result.log').open('w',encoding='utf-8') as log:r=subprocess.run(command,cwd=target,stdout=log,stderr=subprocess.STDOUT)
  output=(target/'result.log').read_text(encoding='utf-8');entry.update(runtimeExit=r.returncode,seconds=time.monotonic()-start,killed=r.returncode!=0 and 'AssertionError: S61 ADV ' in output)
 results.append(entry)
(a.output/'mutation-results.json').write_text(json.dumps({'candidateSha256':hashlib.sha256(original.read_bytes()).hexdigest(),'results':results},indent=2)+'\n',encoding='utf-8')
print(json.dumps(results,indent=2));raise SystemExit(0 if all(x['killed'] for x in results) else 1)
