"""Focused S60 export boundary and source-shadow sensitivity. No GameTests are launched.
Use current main/client classpath tasks; each execution uses its own output cwd.
"""
import argparse,os,subprocess,json,hashlib
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('main_classpath',type=Path);p.add_argument('client_classpath',type=Path);p.add_argument('capture',type=Path);p.add_argument('output',type=Path);p.add_argument('--mutants',action='store_true');a=p.parse_args();root=Path(__file__).absolute().parent.parent;out=a.output.absolute();out.mkdir(parents=True,exist_ok=True);jdk=Path(os.environ['JAVA_HOME'])/'bin';main=a.main_classpath.absolute().read_text(encoding='utf-8');client=a.client_classpath.absolute().read_text(encoding='utf-8');capture=a.capture.absolute();tools=root/'tools/s60-adversarial-common'
def run(name,mode,shadow=None):
 d=out/name;d.mkdir(exist_ok=True);cl=d/'classes';cl.mkdir(exist_ok=True);cp=client if mode=='export' else main; sources=[tools/'S60ExportBoundary.java'] if mode=='export' else [tools/'S60Consumer.java',tools/'S60Bootstrap.java']
 if shadow:sources.append(shadow)
 with (d/'compile.log').open('w',encoding='utf-8') as log:code=subprocess.run([str(jdk/'javac.exe'),'-cp',cp,'-d',str(cl),*[str(q) for q in sources]],cwd=d,stdout=log,stderr=subprocess.STDOUT).returncode
 if code:raise RuntimeError('Compilation failed '+name)
 if mode=='export':command=[str(jdk/'java.exe'),'-cp',str(cl)+os.pathsep+cp,'S60ExportBoundary',str(capture/'tables.json')]
 else:command=[str(jdk/'java.exe'),'-cp',str(cl),'S60Bootstrap',str(a.main_classpath.absolute()),str(cl),str(capture/'tables.json'),str(capture/'programs.json')]
 with (d/'result.log').open('w',encoding='utf-8') as log:code=subprocess.run(command,cwd=d,stdout=log,stderr=subprocess.STDOUT).returncode
 return code,(d/'result.log').read_text(encoding='utf-8')
code,text=run('export-baseline','export');assert code==0,text;print(text,flush=True)
engine=root/'src/main/java/io/github/r3neer/scalebrews/collision/pose/FinitePoseEngine.java';export=root/'src/client/java/io/github/r3neer/scalebrews/client/collision/preparation/OriginalFiniteResourcePreparation.java'
edits={'signed-zero':(engine,'int bits = Float.floatToIntBits(value);','int bits = Float.floatToIntBits(value == 0f ? 0f : value);','common'),'mask-order':(engine,'mask |= 1 << i','mask |= 1 << (names.length - i - 1)','common'),'resource-capture':(engine,'int mask = 0;','resources.finiteProgram(Identifier.parse(parameters.get("program"))); int mask = 0;','common'),'field-alias':(export,'channels.put(key, name);','channels.put(key, "float.0");','export'),'union-budget':(export,'fields.size() >= 64 - booleanNames.size()','fields.size() >= 65 - booleanNames.size()','export')}
results=[]
for name,(owner,anchor,replacement,mode) in (edits.items() if a.mutants else []):
 source=owner.read_text(encoding='utf-8');assert source.count(anchor)==1,name;d=out/name;d.mkdir(exist_ok=True);shadow=d/owner.name;shadow.write_text(source.replace(anchor,replacement),encoding='utf-8');code,text=run(name,mode,shadow);kill=code==1 and 'AssertionError: S60' in text;results.append({'name':name,'exit':code,'killed':kill,'sourceSha256':hashlib.sha256(owner.read_bytes()).hexdigest(),'shadowSha256':hashlib.sha256(shadow.read_bytes()).hexdigest()});print(name,kill,flush=True)
(out/'mutation-results.json').write_text(json.dumps(results,indent=2)+'\n',encoding='utf-8');assert all(q['killed'] for q in results),'Survivor or infrastructure failure'
