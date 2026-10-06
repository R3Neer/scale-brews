"""S62 fixed tick locomotion comparison; scalar clock evolution, not renderer/material."""
import argparse,json,hashlib,html
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
p=argparse.ArgumentParser();p.add_argument('capture',type=Path);p.add_argument('output',type=Path);a=p.parse_args();a.output=a.output.absolute();a.output.mkdir(parents=True,exist_ok=True)
rows=json.loads(a.capture.absolute().read_text(encoding='utf-8'));groups={}
for row in rows:groups.setdefault((row['actor'],row['baby']),[]).append(row)
assert len(groups)==4
W,H=1180,1190;image=Image.new('RGB',(W,H),'#f7f9fc');draw=ImageDraw.Draw(image);font=ImageFont.load_default(size=16)
svg=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}"><rect width="100%" height="100%" fill="#f7f9fc"/>']
def line(points,color,width=2):
 draw.line(points,fill=color,width=width);svg.append('<polyline fill="none" stroke="'+color+'" stroke-width="'+str(width)+'" points="'+' '.join(f'{x:.3f},{y:.3f}' for x,y in points)+'"/>')
def text(x,y,value,color='#20324c'):
 draw.text((x,y),value,font=font,fill=color);svg.append(f'<text x="{x}" y="{y+16}" font-family="sans-serif" font-size="16" fill="{color}">{html.escape(value)}</text>')
text(24,12,'S62 original transformed method (blue) / private clock (magenta markers)')
text(24,40,'Fixed input sequence / scale .5 / DOWN / partial 1; no renderer/root/material claim')
maxdelta=0
for panel,((actor,baby),items) in enumerate(sorted(groups.items())):
 items.sort(key=lambda x:x['step']);assert [x['step'] for x in items]==list(range(12));top=90+panel*265
 text(24,top,f'{actor} / '+('baby' if baby else 'adult'))
 for column,(native,candidate,label) in enumerate([('nativePhase','candidatePhase','phase'),('nativeSpeed','candidateSpeed','speed')]):
  left=55+column*575;bottom=top+218;maximum=max([x[native] for x in items]+[x[candidate] for x in items]+[.001])*1.1
  text(left,top+25,f'{label}; y=0..{maximum:.3g} / ticks 0..11');line([(left,top+52),(left,bottom),(left+500,bottom)],'#8996ab',1)
  for key,color in [(native,'#126bc5'),(candidate,'#c72895')]:
   points=[(left+row['step']*500/11,bottom-row[key]*158/maximum) for row in items];line(points,color,2)
   if key==candidate:
    for x,y in points:line([(x-3,y-3),(x+3,y+3)],color,2);line([(x-3,y+3),(x+3,y-3)],color,2)
  for row in items:maxdelta=max(maxdelta,abs(row[native]-row[candidate]))
text(24,1155,'Temporal scalar evidence only; identical curves supplement semantic original-method assertions.')
svg.append('</svg>');(a.output/'clock-sequence.svg').write_text('\n'.join(svg)+'\n',encoding='utf-8');image.save(a.output/'clock-sequence.png')
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();meta={'captureSha256':h(a.capture.absolute()),'samples':len(rows),'groups':4,'maxDelta':maxdelta,'tickSequence':list(range(12)),'reference':'actual calculateEntityAnimation original transformed method','candidate':'LivingLocomotionClock','scale':.5,'gravity':'DOWN','partial':1,'limits':'Scalar temporal curves only; no GPU/material/root/FULL or server/client lockstep','svgSha256':h(a.output/'clock-sequence.svg'),'pngSha256':h(a.output/'clock-sequence.png')};(a.output/'snapshot-metadata.json').write_text(json.dumps(meta,indent=2)+'\n',encoding='utf-8');print(json.dumps(meta))
