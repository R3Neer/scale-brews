"""Render reproducible Y/Z body-bound snapshots from native S31 consumer CSV."""
from pathlib import Path
import argparse, csv, hashlib, json, math
from PIL import Image,ImageDraw,ImageFont
p=argparse.ArgumentParser();p.add_argument('csv');p.add_argument('output');a=p.parse_args()
source=Path(a.csv);rows=list(csv.DictReader(source.open()))
def xyz(r): return tuple(float(r[k]) for k in ('x','y','z'))
physical=xyz(next(r for r in rows if r['kind']=='physical'))
bmin=xyz(next(r for r in rows if r['kind']=='boundsMin'));bmax=xyz(next(r for r in rows if r['kind']=='boundsMax'))
image=Image.new('RGB',(1250,400),'#10202a');draw=ImageDraw.Draw(image)
font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',17)
small=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',14)
draw.text((20,12),'S31 native JOINT consumer / fixed input head pitch 0 -> 10 degrees / Y-Z projection',font=font,fill='white')
for i,fraction in enumerate((0,.25,.5,.75,1)):
 van=xyz(next(r for r in rows if r['kind']=='vanilla' and float(r['fraction'])==fraction));cor=xyz(next(r for r in rows if r['kind']=='corrected' and float(r['fraction'])==fraction))
 assert math.dist(cor,physical)<1e-4
 left=15+i*247;cx=left+122;bottom=310;scale=500
 def rect(pos):
  dy=pos[1]-physical[1];dz=pos[2]-physical[2]
  return (cx+(bmin[2]-physical[2]+dz)*scale,bottom-(bmax[1]-physical[1]+dy)*scale,cx+(bmax[2]-physical[2]+dz)*scale,bottom-(bmin[1]-physical[1]+dy)*scale)
 draw.line((left+8,bottom,left+235,bottom),fill='#405664',width=1)
 draw.rectangle(rect(van),outline='#ffac55',width=3)
 draw.rectangle(rect(cor),outline='#59e1c1',width=3)
 draw.text((left+20,55),f'partial = {fraction:.2f}',font=font,fill='white')
 draw.text((left+20,325),f'vanilla gap {math.dist(van,physical)*100:.3f} cm',font=small,fill='#ffac55')
 draw.text((left+20,347),f'corrected gap {math.dist(cor,physical)*100:.6f} cm',font=small,fill='#59e1c1')
draw.text((20,377),'Orange: vanilla interpolated body bounds | Green: native renderer correction; coincides with physical endpoint bounds',font=small,fill='#c1d2de')
output=Path(a.output);image.save(output)
output.with_suffix('.json').write_text(json.dumps({'source':str(source.resolve()),'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'projection':'Y/Z','pixels_per_meter':500,'scope':'native renderer position projected onto physical AABB; geometric snapshot, not Minecraft pixel capture','inputs':'head pitch 0->10; walk phase/amount/age/head yaw 0; fractions 0,.25,.5,.75,1; scale .2; actual receiver/carry callback','physical':physical,'bounds_min':bmin,'bounds_max':bmax},indent=2))
print(output.resolve())
