"""Fixed sample3 local shapes/bases from fresh native/common finite captures."""
import argparse
import hashlib
import itertools
import json
from pathlib import Path
from PIL import Image, ImageDraw

p=argparse.ArgumentParser()
p.add_argument('native',type=Path);p.add_argument('common',type=Path);p.add_argument('output',type=Path)
a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
families=json.loads(a.native.read_text(encoding='utf-8'));common=json.loads(a.common.read_text(encoding='utf-8'))
rows=[]
for fi,family in enumerate(families):
    exported=family['exported'];geometry=next(iter(exported['numericPrograms'].values()))['geometry']
    for row in exported['table']['rows']:
        sample=row['mask']*4+3
        assert sample<len(family['samples'])
        channels=family['samples'][sample]['channels']
        mask=sum((1<<i) for i,key in enumerate(exported['table']['booleans']) if channels[key]==1)
        assert mask==row['mask']
        rows.append((fi,row['mask'],family['alias'],geometry,family['samples'][sample]['matrices'],common[fi][sample]))

def project(matrix,point):
    v=[sum(matrix[col*4+axis]*point[col] for col in range(3))+matrix[12+axis] for axis in range(3)]
    return v[0]+.35*v[2],-v[1]+.15*v[2]

def hull(points):
    points=sorted(set(points))
    def cross(o,a,b):return (a[0]-o[0])*(b[1]-o[1])-(a[1]-o[1])*(b[0]-o[0])
    halves=[]
    for sequence in (points,reversed(points)):
        half=[]
        for point in sequence:
            while len(half)>1 and cross(half[-2],half[-1],point)<=0:half.pop()
            half.append(point)
        halves.append(half[:-1])
    return halves[0]+halves[1]

width=max(max(len(row[3]['pieces'])*175,len(row[3]['parts'])*150) for row in rows)+25
height=len(rows)*235+40
envelopes=Image.new('RGB',(width,height),'white');ed=ImageDraw.Draw(envelopes)
bases=Image.new('RGB',(width,height),'white');bd=ImageDraw.Draw(bases)
ed.text((10,5),'Native blue | common green | overlay; local envelopes and projected N/C centers',fill='black')
bd.text((10,5),'Joint bases only: native solid / common dashed; zero-thickness joints carry no material claim',fill='black')
for ri,(fi,mask,alias,geometry,native,candidate) in enumerate(rows):
    y=ri*235+30;title=f'family={fi} mask={mask} alias={alias} sample=3; same fixed projection'
    ed.text((10,y),title,fill='black');bd.text((10,y),title,fill='black')
    for ci,piece in enumerate(geometry['pieces']):
        x=ci*175+10;ed.text((x,y+18),piece['id'].split('/')[-1]+' '+piece['part'].split('/')[-1],fill='black')
        polygons=[];centers=[]
        for matrices in (native,candidate):
            vertices=[project(matrices[piece['part']],v) for v in itertools.product(*zip(piece['min'],piece['max']))]
            center=[sum(v[i] for v in vertices)/8 for i in range(2)];centers.append(center)
            polygons.append(hull([(v[0]-center[0],v[1]-center[1]) for v in vertices]))
        allpoints=[v for polygon in polygons for v in polygon]
        lo=[min(v[i] for v in allpoints) for i in range(2)];hi=[max(v[i] for v in allpoints) for i in range(2)]
        scale=min(45/max(hi[0]-lo[0],1e-9),110/max(hi[1]-lo[1],1e-9))
        for panel in range(3):
            for source in ([panel] if panel<2 else [0,1]):
                pts=[(x+panel*54+(v[0]-lo[0])*scale,y+45+(v[1]-lo[1])*scale) for v in polygons[source]]
                ed.line(pts+[pts[0]],fill=('#1f4b99' if source==0 else '#15956c'),width=1)
        for source,center in enumerate(centers):ed.text((x,y+172+source*14),f'{"N" if source==0 else "C"}({center[0]:.4f},{center[1]:.4f})',fill='black')
    for ci,part in enumerate(geometry['parts']):
        x=ci*150+10;cx=x+60;cy=y+96;bd.text((x,y+18),part['id'].split('/')[-1],fill='black')
        for source,matrices in enumerate((native,candidate)):
            matrix=matrices[part['id']];center=project(matrix,(0,0,0))
            for axis,color in enumerate(('#dc2626','#16a34a','#2563eb')):
                point=[0,0,0];point[axis]=1;end=project(matrix,point)
                ex=cx+(end[0]-center[0])*28;ey=cy+(end[1]-center[1])*28
                if source==0:bd.line((cx,cy,ex,ey),fill=color,width=2)
                else:
                    for segment in range(0,10,2):bd.line((cx+(ex-cx)*segment/10,cy+(ey-cy)*segment/10,cx+(ex-cx)*(segment+1)/10,cy+(ey-cy)*(segment+1)/10),fill=color,width=1)
            bd.text((x,y+172+source*14),f'{"N" if source==0 else "C"}({center[0]:.4f},{center[1]:.4f})',fill='black')
envelopes.save(a.output/'local-envelopes.png');bases.save(a.output/'joint-bases.png')
(a.output/'metadata.json').write_text(json.dumps({'families':len(families),'assignments':len(rows),'sample':3,'projection':[.35,.15],'pillowVersion':Image.__version__,'nativeSha256':hashlib.sha256(a.native.read_bytes()).hexdigest(),'commonSha256':hashlib.sha256(a.common.read_bytes()).hexdigest(),'shapePanels':['native','common','overlay'],'bases':'native solid/common dashed','scope':'recentered local shapes + original projected centers; bases include zero-thickness joints; no GPU/world-root/material-inclusion authority'},indent=2)+'\n',encoding='utf-8')
print(f'S60_IMPLEMENTER_SNAPSHOT_PASS families={len(families)} assignments={len(rows)} fixedSample=3 width={width} height={height}')
