import json, math, argparse
from pathlib import Path
from PIL import Image, ImageDraw
p=argparse.ArgumentParser();p.add_argument('snapshots',type=Path);args=p.parse_args()
a=json.loads((args.snapshots/'s48-live-adult-actual-root.json').read_text(encoding='utf-8'))
b=json.loads((args.snapshots/'s48-live-baby-actual-root.json').read_text(encoding='utf-8'))
removed=set(a['local'])-set(b['local'])
assert len(removed)==1 and not set(b['local'])-set(a['local'])
assert a['token']!=b['token'] and b['generation']>a['generation']
max_error=0.0
for key,vertices in b['local'].items():
    for x,y in zip(a['local'][key],vertices):
        max_error=max(max_error,max(abs(u-v) for u,v in zip(x,y)))
assert max_error<1e-6, max_error
img=Image.new('RGB',(1200,650),'white');d=ImageDraw.Draw(img)
for panel,frame in enumerate((a,b)):
    d.text((20+panel*600,20),f"S48 actual live {'adult' if panel==0 else 'baby'} / binding {frame['generation']} / root scale {frame['rootScale']}",fill='black')
    for key,vertices in frame['local'].items():
        color=(190,45,30) if key in removed else (20,90,150)
        def project(v):return (300+panel*600+(v[0]-.5*v[2])*160,570-(v[1]-.25*v[2])*180)
        for i in range(8):
            for bit in range(3):
                j=i^(1<<bit)
                if i<j:d.line((project(vertices[i]),project(vertices[j])),fill=color,width=2)
    d.text((20+panel*600,610),f"Original Cow 26.2, static pose, {len(frame['local'])} declared pieces; actual root normalized",fill='black')
img.save(args.snapshots/'s48-live-comparison.png')
result={'originalPieces':len(a['local']),'replacementPieces':len(b['local']),'removed':sorted(removed),'maxRetainedLocalVertexError':max_error,'rootScale':[a['rootScale'],b['rootScale']],'generation':[a['generation'],b['generation']]}
(args.snapshots/'comparison.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(json.dumps(result))
