"""Render captured certified geometry; independent ROOT translation and JOINT isolation oracles."""
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parents[1]
data = json.loads((root.parent / 's29-adversary-run/s29-presentation-batch.json').read_text())
frames = data['geometryFrames']
groups = {kind: sorted((f for f in frames if f['kind'] == kind), key=lambda f: f['fraction']) for kind in ('ROOT', 'JOINT')}
assert all(len(group) == 5 for group in groups.values())
reference = groups['ROOT'][0]
delta = [b-a for a,b in zip(reference['beforeRoot'], reference['afterRoot'])]
assert abs(delta[0]-.5) < 1e-6 and abs(delta[1])+abs(delta[2]) < 1e-6
for frame in groups['ROOT']:
    for name, vertices in frame['pieces'].items():
        for vertex, before in zip(vertices, reference['pieces'][name]):
            assert max(abs(v-(a+d*frame['fraction'])) for v,a,d in zip(vertex,before,delta)) < 2e-5, (frame['fraction'],name)
joint = groups['JOINT']
for frame in joint:
    assert frame['beforeRoot'] == frame['afterRoot']
    assert frame['pieces']['root/body/cube_0'] == joint[0]['pieces']['root/body/cube_0']
assert any(joint[0]['pieces'][name] != joint[-1]['pieces'][name] for name in joint[0]['pieces'] if '/head/' in name)
print('PASS ROOT exact linear displacement of every vertex; JOINT fixed root/body with moving head')

image = Image.new('RGB',(1700,1060),'#f5f7fb')
draw = ImageDraw.Draw(image)
font = ImageFont.truetype('C:/Windows/Fonts/consola.ttf',16)
small = ImageFont.truetype('C:/Windows/Fonts/consola.ttf',13)
draw.text((22,15),'S29 real-wire certified geometry | fixed inputs/state, support-relative projections',font=font,fill='#12263d')
draw.text((22,40),'Grey: fraction=0 reference. Blue: body/legs. Orange: head. No renderer animation input.',font=small,fill='#334b66')
edges = [(i,i^(1<<axis)) for i in range(8) for axis in range(3) if not i&(1<<axis)]
for row,(kind,axes,label) in enumerate([('ROOT',(0,2),'TOP X/Z'),('ROOT',(2,1),'SIDE Z/Y'),('JOINT',(0,2),'TOP X/Z'),('JOINT',(2,1),'SIDE Z/Y')]):
    group=groups[kind]
    points=[v for frame in frames for vertices in frame['pieces'].values() for v in vertices]
    low_a,high_a=min(v[axes[0]] for v in points),max(v[axes[0]] for v in points)
    low_b,high_b=min(v[axes[1]] for v in points),max(v[axes[1]] for v in points)
    scale=min(292/(high_a-low_a),166/(high_b-low_b))
    for col,frame in enumerate(group):
        x,y=20+col*337,80+row*240
        draw.text((x,y),f'{kind} f={frame["fraction"]:.2f} {label}',font=small,fill='#12263d')
        draw.rectangle((x,y+24,x+320,y+218),outline='#b8c8db')
        def project(vertex):
            a,b=vertex[axes[0]],vertex[axes[1]]
            return x+160+(a-(low_a+high_a)/2)*scale, y+121+((low_b+high_b)/2-b if axes[1]==1 else b-(low_b+high_b)/2)*scale
        for reference_layer in (True,False):
            pieces=group[0]['pieces'] if reference_layer else frame['pieces']
            for name,vertices in pieces.items():
                colour='#c5cdd7' if reference_layer else '#dc6b12' if '/head/' in name else '#1b68b0'
                for a,b in edges:draw.line([project(vertices[a]),project(vertices[b])],fill=colour,width=1 if reference_layer else 2)
        draw.text((x,y+220),f'material={frame["materialSerial"]} frames={frame["beforeSerial"]}->{frame["afterSerial"]}',font=small,fill='#334b66')
destination=root.parent/'s29-adversarial-certified-geometry.png'
image.save(destination)
print(destination)
