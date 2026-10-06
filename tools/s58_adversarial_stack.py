"""JDK25 fresh JVM audit of S58 symbolic stack, optionally source-shadow mutations.
Classpath must include compiled client production and Minecraft clientOnly libraries.
"""
import argparse, os, subprocess, json, hashlib
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument("classpath",type=Path);p.add_argument("output",type=Path);p.add_argument("--mutants",nargs="+",choices=["category2","top-identity","receiver-identity","stack-cap","provenance"]);a=p.parse_args()
r=Path(__file__).absolute().parent.parent;o=a.output.absolute();o.mkdir(parents=True,exist_ok=True);cp=a.classpath.absolute().read_text(encoding="utf-8");java=Path(os.environ["JAVA_HOME"])/"bin";proof=r/"tools/s58-adversarial-common/S58StackProof.java";source=r/"src/client/java/io/github/r3neer/scalebrews/client/collision/preparation/OriginalLinearPosePreparation.java"
def run(name,shadow=None):
 d=o/name;d.mkdir(exist_ok=True);classes=d/"classes";classes.mkdir(exist_ok=True);sources=[proof]+([shadow] if shadow else [])
 with (d/"compile.log").open("w",encoding="utf-8") as log:c=subprocess.run([str(java/"javac.exe"),"-cp",cp,"-d",str(classes),*[str(x) for x in sources]],cwd=d,stdout=log,stderr=subprocess.STDOUT)
 if c.returncode:raise RuntimeError("compile "+name)
 with (d/"result.log").open("w",encoding="utf-8") as log:c=subprocess.run([str(java/"java.exe"),"-cp",str(classes)+os.pathsep+cp,"io.github.r3neer.scalebrews.client.collision.preparation.S58StackProof"],cwd=d,stdout=log,stderr=subprocess.STDOUT)
 return c.returncode,(d/"result.log").read_text(encoding="utf-8")
code,text=run("baseline")
if code or "S58_STACK_PASS" not in text:raise RuntimeError("baseline failed")
print("baseline PASS",flush=True)
edits={"category2":("|| number.promoted()))", "|| false))"),"top-identity":("var value = stack.getLast();","var value = stack.getFirst();"),"receiver-identity":("push(stack, value);\n        }\n        static Object pop", "push(stack, value instanceof Ref ref ? new Ref(ref.part(), ref.state()) : value);\n        }\n        static Object pop"),"stack-cap":("if (stack.size() >= MAX_STACK)","if (stack.size() > MAX_STACK)"),"provenance":("original-linear-pose-v2","original-linear-pose-v1")}
results=[]
for name in a.mutants or []:
 anchor,repl=edits[name];s=source.read_text(encoding="utf-8");assert s.count(anchor)==1,name;d=o/name;d.mkdir(exist_ok=True);shadow=d/source.name;shadow.write_text(s.replace(anchor,repl),encoding="utf-8");code,text=run(name,shadow);kill=code!=0 and "AssertionError: S58 ADV" in text;results.append(dict(name=name,killed=kill,exit=code,sourceSha256=hashlib.sha256(source.read_bytes()).hexdigest(),shadowSha256=hashlib.sha256(shadow.read_bytes()).hexdigest()));print(name,kill,flush=True)
(o/"mutation-results.json").write_text(json.dumps(results,indent=2)+"\n",encoding="utf-8")
if not all(x["killed"] for x in results):raise RuntimeError("survivor or infrastructure failure")
