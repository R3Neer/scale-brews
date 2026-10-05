from pathlib import Path
import subprocess,json,hashlib,os,shutil,argparse
repo=Path(__file__).resolve().parents[1]
args=argparse.ArgumentParser();args.add_argument('--output',type=Path,default=repo.parent/'s51-adversarial-parser-output');args=args.parse_args();private=args.output.resolve();private.mkdir(parents=True,exist_ok=True)
source=repo/'src/client/java/io/github/r3neer/scalebrews/client/collision/preparation/OriginalModelPrograms.java'
probe=repo/'src/gametest/java/io/github/r3neer/scalebrews/client/collision/preparation/S51AdversarialParserProof.java'
java=Path(os.environ['JAVA_HOME'])/'bin' if os.environ.get('JAVA_HOME') else Path(shutil.which('javac') or 'javac').resolve().parent
base=source.read_text(encoding='utf8')
mutants={
'instruction-cap':('MAX_INSTRUCTIONS = 512','MAX_INSTRUCTIONS = 513'),
'class-cap':('MAX_CLASSES = 64','MAX_CLASSES = 65'),
'method-cap':('MAX_METHODS = 1024','MAX_METHODS = 1025'),
'whole-program':('if (next != instructions.size()) throw absent("extra_program");','if (false) throw absent("extra_program");'),
'resource-digest':('java.util.Arrays.stream(approvedDigests).noneMatch(actual::equals)','false'),
'local-reference-type':(' || load.typeKind() != java.lang.classfile.TypeKind.REFERENCE',''),
'local-float-type':(' || load.typeKind() != java.lang.classfile.TypeKind.FLOAT',''),
'identity-cache':('if (cached != null) return cached;','if (false) return cached;'),
}
results=[]
baseline=private/'baseline';baseline.mkdir(parents=True,exist_ok=True)
compiled=subprocess.run([str(java/'javac.exe'),'-d',str(baseline),str(source),str(probe)],capture_output=True,text=True)
assert compiled.returncode==0,compiled.stderr
run=subprocess.run([str(java/'java.exe'),'-cp',str(baseline),'io.github.r3neer.scalebrews.client.collision.preparation.S51AdversarialParserProof'],capture_output=True,text=True)
(private/'baseline.log').write_text(run.stdout+run.stderr,encoding='utf8')
assert run.returncode==0,run.stdout+run.stderr
print(run.stdout,end='')
for name,(old,new) in mutants.items():
 assert old in base,name
 folder=private/'mutations'/name;folder.mkdir(parents=True,exist_ok=True)
 shadow=folder/'OriginalModelPrograms.java';shadow.write_text(base.replace(old,new,1),encoding='utf8')
 out=folder/'classes';cmd=[str(java/'javac.exe'),'-d',str(out),str(shadow),str(probe)]
 compiled=subprocess.run(cmd,capture_output=True,text=True)
 assert compiled.returncode==0,compiled.stderr
 ran=subprocess.run([str(java/'java.exe'),'-cp',str(out),'io.github.r3neer.scalebrews.client.collision.preparation.S51AdversarialParserProof'],capture_output=True,text=True)
 (folder/'result.log').write_text(ran.stdout+ran.stderr,encoding='utf8')
 results.append(dict(mutant=name,exit=ran.returncode,killed=ran.returncode!=0,reason=(ran.stdout+ran.stderr).splitlines()[0]))
(private/'parser-mutations.json').write_text(json.dumps({'sourceSha256':hashlib.sha256(source.read_bytes()).hexdigest(),'results':results},indent=2),encoding='utf8')
print(json.dumps(results,indent=2));assert all(r['killed'] for r in results)