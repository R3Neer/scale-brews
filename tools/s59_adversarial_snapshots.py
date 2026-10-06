"""Deterministic adaptive local-envelope comparison, never GPU/root/material evidence."""
import argparse,json,itertools,html
from pathlib import Path
from PIL import Image,ImageDraw
p=argparse.ArgumentParser();p.add_argument("native",type=Path);p.add_argument("common",type=Path);p.add_argument("output",type=Path);a=p.parse_args();a.output=a.output.absolute();a.output.mkdir(parents=True,exist_ok=True);rows=json.loads(a.native.read_text());common=json.loads(a.common.read_text());svg=['<svg xmlns="http://www.w3.org/2000/svg" width="1080" height="2160"><rect width="1080" height="2160" fill="white"/>'];image=Image.new("RGB",(1080,2160),"white");draw=ImageDraw.Draw(image)
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
 title=f"finite predicate={row['predicate']} alias={row['alias']} sample={tick}; local shape and N/C centers"
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
svg.append('</svg>');(a.output/'compound-comparison.svg').write_text(''.join(svg),encoding='utf-8');image.save(a.output/'compound-comparison.png');print('S59 snapshots8rows sample3; recentered per-cube shapes+original centers; no global root/render claim')

# Joint-basis projections expose animated zero-thickness parts without claiming
# those parts are material convex pieces. Native solid, common dashed axes.
bases=Image.new("RGB",(1080,2160),"white");bd=ImageDraw.Draw(bases);bs=['<svg xmlns="http://www.w3.org/2000/svg" width="1080" height="2160"><rect width="1080" height="2160" fill="white"/>']
for rowIndex,row in enumerate(rows):
 tick=3; yy=rowIndex*265+15;title=f"joint bases only; predicate={row['predicate']} alias={row['alias']}; sample3"
 bd.text((10,yy),title,fill="black");bs.append(f'<text x="10" y="{yy+12}" font-size="12">{html.escape(title)}</text>')
 for col,part in enumerate(row["program"]["geometry"]["parts"]):
  partId=part["id"];x=col*150+10;cy=yy+105;bd.text((x,yy+22),partId.split('/')[-1],fill="black");bs.append(f'<text x="{x}" y="{yy+34}" font-size="10">{html.escape(partId.split("/")[-1])}</text>')
  for source,matrices in enumerate((row["samples"][tick]["matrices"],common[rowIndex][tick])):
   m=matrices[partId];center=project(m,(0,0,0));cx=x+65
   for axis,color in enumerate(("#dc2626","#16a34a","#2563eb")):
    v=[0,0,0];v[axis]=1;pnt=project(m,v);ex=cx+(pnt[0]-center[0])*30;ey=cy+(pnt[1]-center[1])*30
    if source==0:bd.line((cx,cy,ex,ey),fill=color,width=2)
    else:
     for segment in range(0,10,2):bd.line((cx+(ex-cx)*segment/10,cy+(ey-cy)*segment/10,cx+(ex-cx)*(segment+1)/10,cy+(ey-cy)*(segment+1)/10),fill=color,width=1)
    dash=' stroke-dasharray="3 3"' if source else '';bs.append(f'<line x1="{cx}" y1="{cy}" x2="{ex}" y2="{ey}" stroke="{color}" stroke-width="{1 if source else 2}"{dash}/>')
   label=f"{'N' if source==0 else 'C'}({center[0]:.4f},{center[1]:.4f})";bd.text((x,yy+190+source*15),label,fill="black");bs.append(f'<text x="{x}" y="{yy+202+source*15}" font-size="10">{label}</text>')
bs.append('</svg>');(a.output/"joint-bases.svg").write_text(''.join(bs),encoding="utf-8");bases.save(a.output/"joint-bases.png")
