import hashlib,json,pathlib,shutil,subprocess,sys,tempfile
base=pathlib.Path(__file__).resolve().parent
repo=base.parent; gate=repo/'tools/s25_reference_authority_gate.py'
emitter='src/client/java/io/github/r3neer/scalebrews/client/mixin/AnatomyOutgoingMoveMixin.java'
payload='src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyMoveReferenceV2Payload.java'
network='src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyNetworking.java'
config='src/client/resources/scalebrews.client.mixins.json'
cases=[('baseline',None,None,None),
 ('ownership',emitter,'AnatomyApi.ownsSharedPhysics(body)','AnatomyApi.ready(body)'),
 ('thread',emitter,' || !client.isSameThread()',''),
 ('positional',emitter,' && p.hasPosition()',''),
 ('field',payload,'long receiptSequence)','long receiptSequence, double localPoint)'),
 ('physical-type',payload,'long receiptSequence)','long receiptSequence, ConvexBox supplied)'),
 ('receiver',network,'PayloadTypeRegistry.serverboundPlay().register(AnatomyMoveReferenceV2Payload.TYPE,AnatomyMoveReferenceV2Payload.CODEC);',''),
 ('mixin',config,'"AnatomyOutgoingMoveMixin",','')]
results=[]
for name,path,before,after in cases+[('legacy',None,None,None),('second-emitter',None,None,None),('unqualified-emitter',None,None,None),('missing-emitter',None,None,None)]:
 with tempfile.TemporaryDirectory(prefix='s25-shadow-') as td:
  work=pathlib.Path(td)
  for source in ['src/main/java','src/client/java','src/client/resources']:
   shutil.copytree(repo/source,work/source)
  if path:
   p=work/path;s=p.read_text();assert before in s,(name,before);p.write_text(s.replace(before,after,1))
  if name=='legacy':
   p=work/'src/main/java/io/github/r3neer/scalebrews/platform/PlatformMovePayload.java';p.parent.mkdir(exist_ok=True);p.write_text('class PlatformMovePayload {}')
  if name in ('second-emitter','unqualified-emitter'):
   p=work/'src/client/java/io/github/r3neer/scalebrews/client/mixin/ExtraEmitter.java';p.write_text('class ExtraEmitter { void send() { '+('AnatomyClientNetworking.' if name=='second-emitter' else '')+'sendMovementReference(body); } }')
  if name=='missing-emitter': (work/emitter).unlink()
  r=subprocess.run([sys.executable,str(gate)],cwd=work,text=True,capture_output=True)
  assert r.returncode==(0 if name=='baseline' else 1),(name,r.stdout,r.stderr)
  assert not r.stderr,(name,r.stderr)
  results.append({'case':name,'exit':r.returncode,'output':r.stdout})
out={'gateSha256':hashlib.sha256(gate.read_bytes()).hexdigest(),'results':results}
print(json.dumps(out,indent=2))
print('S25_SHADOW_SENSITIVITY_PASS',len(results))
