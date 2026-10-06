"""Fixed temporal contact sheet: native transformed reference vs authority clock.
Only scalar clock evolution is shown; no renderer, world-root or GPU claim.
"""
import argparse
import hashlib
import json
from collections import defaultdict
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

p = argparse.ArgumentParser()
p.add_argument('capture', type=Path)
p.add_argument('output', type=Path)
a = p.parse_args()
a.output.mkdir(parents=True, exist_ok=True)
raw = a.capture.read_bytes()
capture = json.loads(raw)
groups = defaultdict(list)
for row in capture['samples']:
    groups[(row['actor'].rsplit('.', 1)[-1], row['baby'], row['scale'])].append(row)
groups = sorted(groups.items())
assert len(groups) == 20 and all(len(rows) == 32 for _, rows in groups)
assert all(r['actual'] == r['expected'] for _, rows in groups for r in rows)
phase_max = max(r['expected']['walk_position'] for _, rows in groups for r in rows) * 1.1
W, H = 2000, 1550
im = Image.new('RGB', (W, H), '#10151c')
d = ImageDraw.Draw(im)
font_path = Path('C:/Windows/Fonts/consola.ttf')
font = ImageFont.truetype(str(font_path), 18) if font_path.exists() else ImageFont.load_default()
title = ImageFont.truetype(str(font_path), 23) if font_path.exists() else font
d.text((25, 14), 'S62 | native transformed Living helper / authority clock | 640 samples', fill='white', font=title)
d.text((25, 46), 'Blue: native reference. Amber dashes: candidate. Shared axes: phase %.3f / speed 1.0.' % phase_max, fill='#c9d4e2', font=font)
d.text((25, 73), 'Grey ticks: pause. Green ticks: passive carry (deducted). Fixed steps 0..31; 3840 scalar raw-bit matches.', fill='#c9d4e2', font=font)
for index, (key, rows) in enumerate(groups):
    rows.sort(key=lambda r: r['step'])
    x0, y0 = 20 + index % 4 * 495, 110 + index // 4 * 280
    d.rectangle((x0, y0, x0 + 477, y0 + 266), outline='#3d4c60')
    name, baby, scale = key
    d.text((x0 + 12, y0 + 8), '%s %s scale=%g' % (name, 'baby' if baby else 'adult', scale), fill='white', font=font)
    x, width = x0 + 42, 418
    for field, top, height, maximum, label in [('walk_position', y0 + 48, 116, phase_max, 'phase'), ('walk_speed', y0 + 187, 54, 1, 'speed')]:
        d.text((x0 + 5, top - 18), label, fill='#a2b2c9', font=font)
        d.line((x, top + height, x + width, top + height), fill='#657588')
        for row in rows:
            xx = x + row['step'] / 31 * width
            if row['step'] % 7 == 0:
                d.line((xx, top, xx, top + height), fill='#2a3442')
            if any(row['carry']):
                d.line((xx, top + height - 6, xx, top + height), fill='#54bb8e', width=2)
        for source, color in [('expected', '#57b7ee'), ('actual', '#ffbf52')]:
            points = [(x + r['step'] / 31 * width, top + height * (1 - r[source][field] / maximum)) for r in rows]
            if source == 'expected':
                d.line(points, fill=color, width=3)
            else:
                for step in range(0, 31, 2):
                    d.line(points[step:step + 2], fill=color, width=2)
    d.text((x0 + 10, y0 + 246), '0                             31', fill='#a2b2c9', font=font)
output = a.output / 'clock-evolution.png'
im.save(output)
metadata = {'role': 'IMPLEMENTER', 'sprint': 'S62', 'captureSha256': hashlib.sha256(raw).hexdigest(),
            'dimensions': [W, H], 'steps': [0, 31], 'sharedPhaseMaximum': phase_max,
            'sharedSpeedMaximum': 1, 'groups': len(groups), 'samples': len(capture['samples']),
            'projection': 'scalar temporal curves; fixed axes for all reference/candidate panels',
            'limits': 'Not geometry/render/root/material/TPS proof',
            'pngSha256': hashlib.sha256(output.read_bytes()).hexdigest()}
(a.output / 'metadata.json').write_text(json.dumps(metadata, indent=2) + '\n', encoding='utf-8')
print(json.dumps(metadata))
