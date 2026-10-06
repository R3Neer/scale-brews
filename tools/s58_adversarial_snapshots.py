"""Deterministic adaptive local-envelope comparison, never GPU/root/material evidence."""
import argparse,json,itertools,html
from pathlib import Path
from PIL import Image,ImageDraw
p=argparse.ArgumentParser();p.add_argument("native",type=Path);p.add_argument("common",type=Path);p.add_argument("output",type=Path);a=p.parse_args();a.output=a.output.absolute();a.output.mkdir(parents=True,exist_ok=True);rows=json.loads(a.native.read_text());common=json.loads(a.common.read_text());svg=['<svg xmlns="http://www.w3.org/2000/svg" width="1080" height="1080"><rect width="1080" height="1080" fill="white"/>'];image=Image.new("RGB",(1080,1080),"white");draw=ImageDraw.Draw(image)
def hull(points):
 points=sorted(set(points))
 def cross(o,a,b):return (a[0]-o[0])*(b[1]-o[1])-(a[1]-o[1])*(b[0]-o[0])
 lower=[]
 for v in points:
  while len(lower)>1 and cross(lower[-2],lower[-1],v)<=0:lower.pop()
  lower.append(v)
 upper=[]
 for v in reversed(points):
  while len(upper)>1 and cross(upper[-2],upper[-1],v)<=0:upper.pop()
  upper.append(v)
 return lower[:-1]+upper[:-1]
def project(m,v):
 w=[sum(m[col*4+axis]*v[col] for col in range(3))+m[12+axis] for axis in range(3)]
 return w[0]+.35*w[2],-w[1]+.15*w[2]
for index,row in enumerate(rows):
 tick=3;geometry=row["program"]["geometry"];native=row["samples"][tick]["matrices"];candidate=common[index][tick]
 title=f"{'body' if index<2 else 'fur'} alias={row['alias']} sample={tick}; recentered per-cube shapes; N/C centers retain displacement"
 y=index*265+15;draw.text((10,y),title,fill="black");svg.append(f'<text x="10" y="{y+12}" font-size="12">{html.escape(title)}</text>')
 for partIndex,piece in enumerate(geometry["pieces"]):
  sets=[];centers=[]
  for matrices in (native,candidate):
   vertices=[project(matrices[piece["part"]],v) for v in itertools.product(*zip(piece["min"],piece["max"]))];center=[sum(v[i] for v in vertices)/len(vertices) for i in range(2)];centers.append(center);sets.append(hull([(v[0]-center[0],v[1]-center[1]) for v in vertices]))
  allpoints=[v for polygon in sets for v in polygon];lo=[min(v[i] for v in allpoints) for i in range(2)];hi=[max(v[i] for v in allpoints) for i in range(2)];scale=min(42/max(hi[0]-lo[0],1e-9),115/max(hi[1]-lo[1],1e-9));x=partIndex*175+10
  label=piece["part"].split('/')[-1];draw.text((x,y+22),label,fill="black");svg.append(f'<text x="{x}" y="{y+34}" font-size="10">{html.escape(label)}</text>')
  for panel in range(3):
   for choice in ([panel] if panel<2 else [0,1]):
    color="#1f4b99" if choice==0 else "#15956c";pts=[(x+panel*53+2+(v[0]-lo[0])*scale,y+52+(v[1]-lo[1])*scale) for v in sets[choice]];draw.line(pts+[pts[0]],fill=color,width=1);svg.append('<polygon points="'+' '.join(f'{px:.4f},{py:.4f}' for px,py in pts)+f'" fill="none" stroke="{color}" stroke-width="1"/>')
  for line,center in enumerate(centers):
   caption=f"{'N' if line==0 else 'C'} center=({center[0]:.5f},{center[1]:.5f})";draw.text((x,y+185+line*16),caption,fill="black");svg.append(f'<text x="{x}" y="{y+197+line*16}" font-size="9">{html.escape(caption)}</text>')
svg.append('</svg>');(a.output/'compound-comparison.svg').write_text(''.join(svg),encoding='utf-8');image.save(a.output/'compound-comparison.png');print('S58 snapshots4rows sample3; recentered per-cube shapes+original centers; no global root/render claim')
