"""Render fixed geometric projections from the contact oracle's captured JSON.

This debug snapshot shows authoritative geometry bounds, actual body bounds and
the captured server contact anchor. It is not a screenshot of a game renderer.
"""
import json
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parents[2] / 's29-adversary-run'
prefix = 'dim-' if '--dimension' in sys.argv else ''
states = [json.loads((root / f's29-contact-{prefix}{phase}.json').read_text())
          for phase in ('baseline', 'before-replay', 'after-replay')]
img = Image.new('RGB', (1440, 870), '#f6f8fa')
draw = ImageDraw.Draw(img)
font = ImageFont.truetype('C:/Windows/Fonts/consola.ttf', 17)
small = ImageFont.truetype('C:/Windows/Fonts/consola.ttf', 14)
draw.text((24, 18), 'S29 captured server contact across ' + ('dimension barrier' if prefix else 'connection barrier'), fill='#14263d', font=font)
draw.text((24, 45), 'Fixed projections relative to support origin. Blue: support piece; orange: body; cross: captured anchor.', fill='#34465c', font=small)

def project(v, origin, a, b, left, top):
    # Identical world ranges and pixel scale in every phase/panel.
    return (left + (v[a]-origin[a]+1.5)*88, top + 300-(v[b]-origin[b]+.8)*88)

def rect(bounds, origin, a, b, left, top, color):
    p = project(bounds[:3], origin, a, b, left, top)
    q = project(bounds[3:], origin, a, b, left, top)
    draw.rectangle((min(p[0],q[0]),min(p[1],q[1]),max(p[0],q[0]),max(p[1],q[1])), outline=color, width=3)

for n, state in enumerate(states):
    left = 22+n*477
    alert = state['drawableContact'] and not state['supported']
    draw.text((left, 88), state['phase'], fill='#b42318' if alert else '#14263d', font=font)
    for row, (a,b,label) in enumerate(((0,2,'TOP X/Z'),(2,1,'SIDE Z/Y'))):
        top = 145+row*330
        draw.text((left,top-26),label,fill='#34465c',font=small)
        draw.rectangle((left,top,left+430,top+300),outline='#bbc7d4',width=1)
        origin=state['origin']
        for tick in range(-1,4):
            x=left+(tick+1.5)*88
            draw.line((x,top,x,top+300),fill='#e0e6ed')
        rect(state['supportBounds'],origin,a,b,left,top,'#1766aa')
        rect(state['bodyBounds'],origin,a,b,left,top,'#d26916')
        x,y=project(state['capturedAnchor'],origin,a,b,left,top)
        color='#a61c1c' if alert else '#198754' if state['drawableContact'] else '#667085'
        draw.line((x-8,y,x+8,y),fill=color,width=2)
        draw.line((x,y-8,x,y+8),fill=color,width=2)
    draw.text((left,798),f"drawable={state['drawableContact']} supported={state['supported']}",fill='#b42318' if alert else '#14263d',font=small)
    draw.text((left,820),f"simulate={state['simulates']} predict={state['predicts']} transport={state['hasTransport']}",fill='#34465c',font=small)
    draw.text((left,842),f"rev={state['revision']} bodyGen={state['bodyTrackingGeneration']} {state['presentationKind']}",fill='#34465c',font=small)
dest = root.parent / ('s29-adversarial-contact-dimension-snapshots.png' if prefix else 's29-adversarial-contact-snapshots.png')
img.save(dest)
print(dest)
