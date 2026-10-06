"""Deterministic local envelope comparison of S61 original and common matrices.
This visual does not show renderer root/layers/GPU/material; machine assertions remain authoritative.
"""
import argparse,hashlib,json,math,itertools,html
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
p=argparse.ArgumentParser();p.add_argument('comparison',type=Path);p.add_argument('output',type=Path);a=p.parse_args();a.output=a.output.absolute();a.output.mkdir(parents=True,exist_ok=True)
rows=json.loads(a.comparison.absolute().read_text(encoding='utf-8'));selected=[x for x in rows if x['sample'] in (0,1,7,10)]
assert len(selected)==8
W,H=1320,130+len(selected)*270
image=Image.new('RGB',(W,H),'#f7f9fc');draw=ImageDraw.Draw(image);font=ImageFont.load_default(size=17)
svg=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}"><rect width="100%" height="100%" fill="#f7f9fc"/>']
def text(x,y,value,color='#182338'):
 draw.text((x,y),value,fill=color,font=font);svg.append(f'<text x="{x}" y="{y+17}" fill="{color}" font-family="monospace" font-size="17">{html.escape(value)}</text>')
def line(points,color,width=1):
 draw.line(points,fill=color,width=width);svg.append('<polyline points="'+' '.join(f'{x:.3f},{y:.3f}' for x,y in points)+f'" fill="none" stroke="{color}" stroke-width="{width}"/>')
def point(m,v):return tuple(sum(m[k*4+j]*v[k] for k in range(3))+m[12+j] for j in range(3))
def project(v):x,y,z=v;return (x*.866-z*.5,y*.83+z*.27+x*.18)
text(30,22,'S61 partial1: original input source -> model-local pose')
text(30,48,'Native original / common numeric replay. Fixed sampled state; no renderer root or material claim.')
text(215,87,'NATIVE ORIGINAL', '#1a65a5');text(867,87,'COMMON CANDIDATE','#a32878')
max_delta=0.0
for r,row in enumerate(selected):
 top=130+r*270;geometry=row['program']['geometry'];pieces=geometry['pieces'];mats=[row['nativeMatrices'],row['candidateMatrices']]
 polygons=[]
 for matrices in mats:
  polygons.append([[project(point(matrices[piece['part']],v)) for v in itertools.product(*zip(piece['min'],piece['max']))] for piece in pieces])
 pts=[p for group in polygons for cube in group for p in cube];xs=[p[0] for p in pts];ys=[p[1] for p in pts]
 xmin,xmax=min(xs),max(xs);ymin,ymax=min(ys),max(ys);scale=min(540/max(xmax-xmin,1e-6),188/max(ymax-ymin,1e-6));cx=(xmin+xmax)/2;cy=(ymin+ymax)/2
 delta=max(abs(x-y) for part in mats[0] for x,y in zip(mats[0][part],mats[1][part]));max_delta=max(max_delta,delta)
 text(30,top+4,row['actor'].removeprefix('entity.minecraft.')+f' sample {row["sample"]}  | max matrix delta {delta:.3g}')
 for lane,group in enumerate(polygons):
  color=('#1a65a5','#a32878')[lane];origin=(330+lane*660,top+155)
  for cube in group:
   coords=[(origin[0]+(x-cx)*scale,origin[1]+(y-cy)*scale) for x,y in cube]
   for i in range(8):
    for bit in (1,2,4):
     j=i^bit
     if i<j:line([coords[i],coords[j]],color,2)
  for part,m in sorted(mats[lane].items()):
   v=project(point(m,(0,0,0)));loc=(origin[0]+(v[0]-cx)*scale,origin[1]+(v[1]-cy)*scale)
   line([(loc[0]-3,loc[1]),(loc[0]+3,loc[1])],color,1);line([(loc[0],loc[1]-3),(loc[0],loc[1]+3)],color,1)
  text(origin[0]-265,top+244,'local envelopes + joint origins; common scale and framing',color)
 line([(20,top+266),(1300,top+266)],'#c9d3e1',1)
svg.append('</svg>');(a.output/'living-input-pose.svg').write_text('\n'.join(svg)+'\n',encoding='utf-8');image.save(a.output/'living-input-pose.png')
metadata={'inputSha256':hashlib.sha256(a.comparison.absolute().read_bytes()).hexdigest(),'rows':[{'actor':x['actor'],'sample':x['sample']} for x in selected],'maxMatrixDelta':max_delta,'projection':'x*.866-z*.5,y*.83+z*.27+x*.18; shared bounds per N/C row; 30px side margin','limits':'Model-local envelopes and joint origins only. Not GPU/root/layers/material/clock evolution or FULL.','svgSha256':hashlib.sha256((a.output/'living-input-pose.svg').read_bytes()).hexdigest(),'pngSha256':hashlib.sha256((a.output/'living-input-pose.png').read_bytes()).hexdigest()}
(a.output/'snapshot-metadata.json').write_text(json.dumps(metadata,indent=2)+'\n',encoding='utf-8');print(json.dumps(metadata,indent=2))
