from pathlib import Path
import argparse, hashlib, json
p=argparse.ArgumentParser();p.add_argument('kind',choices=['partial','duplicate','quiet','gravity','local-root']);a=p.parse_args()
root=Path(__file__).resolve().parents[1]
source=root/'src/client/java/io/github/r3neer/scalebrews/client/collision/presentation/CausalBodyResidual.java'
if a.kind=='local-root':
    source=source.with_name('CausalModelRender.java')
s=source.read_text()
changes={
'partial':('draw.appliedCarry().scale(partial)','draw.appliedCarry().scale(1)'),
'duplicate':('if(r!=null && r.body==body && cursor.localTransportSequence()==r.lastTransportSequence\n                && cursor.equals(r.lastCursor))return;','if(false)return;'),
'quiet':('r.pendingBefore=null;r.pendingAfter=null;','/* mutation: retain quiet pending edge */'),
'gravity':('r.bodyGravityObservation.generation()==body.generation()\n            && r.supportGravityObservation.generation()==support.generation()\n            && ',''),
'local-root':('AnatomyClientNetworking.presentationContact(support,support.level().getGameTime()).isPresent()', 'CausalBodyResidual.hasActiveLocalTransport(support)')}
old,new=changes[a.kind]
assert old in s,(a.kind,'source anchor changed')
mutated=s.replace(old,new,1)
out=root.parent/'s31-adversarial-mutants'/a.kind/'io/github/r3neer/scalebrews/client/collision/presentation'
out.mkdir(parents=True,exist_ok=True);(out/source.name).write_text(mutated)
print(json.dumps({'mutation':a.kind,'original_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'output':str(out/source.name)}))
