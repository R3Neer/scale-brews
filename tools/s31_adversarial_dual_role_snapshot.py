"""Deterministic X-axis comparison of captured native renderer submit and carry."""
import csv
import hashlib
import json
import sys
from pathlib import Path
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[2]
source = root / "s29-adversary-run" / (sys.argv[2] if len(sys.argv) > 2 else "s31-adversarial-dual-role-pig-snapshot.csv")
row = next(csv.DictReader(source.open()))
im = Image.new("RGB", (1000, 500), "#111923")
d = ImageDraw.Draw(im)
d.text((35, 25), "S31 native original-model composition: controlled Pig on Cow", fill="white")
d.text((35, 55), f"ROOT carry +0.2 X | partial 0.5 | native authority and contact confirmed | residual {row['residual_x']}", fill="#cdd7e2")
d.text((35, 85), "Geometry comparison of actual captured consumers; not a Minecraft pixel screenshot", fill="#cdd7e2")
axis = lambda x: 160 + (x - 1.95) * 2500
rows = [("accepted_x", "Own accepted model origin", "#8a98a8"),
        ("vanilla_x", "Vanilla interpolated body", "#ffca70"),
        ("queued_x", "Actual original model submit", "#ff7777"),
        ("physical_x", "Physical body / expected carry draw", "#7ae3ae")]
for i, (key, label, color) in enumerate(rows):
    y = 155 + i * 65
    x = float(row[key])
    d.text((35, y - 22), f"{label}: {x:.7f}", fill=color)
    d.line((160, y, 890, y), fill="#39485b", width=2)
    d.ellipse((axis(x)-7, y-7, axis(x)+7, y+7), fill=color)
error = float(row["physical_x"]) - float(row["queued_x"])
d.text((35, 435), f"Actual submit retains {error:.7f} blocks of X slip; residual = {float(row['residual_x']):.7f}", fill="#ff7777")
label = sys.argv[1] if len(sys.argv) > 1 else "snapshot"
if not label.replace("-", "").isalnum():
    raise ValueError("Snapshot label must use letters, digits or hyphens")
output = root / f"s31-adversarial-dual-role-pig-{label}.png"
im.save(output)
output.with_suffix(".json").write_text(json.dumps({"source":str(source),"sha256":hashlib.sha256(source.read_bytes()).hexdigest(),"row":row,"scope":"Actual renderer submit X versus physical carried body; fixed partial 0.5, ROOT +0.2 X; not game pixels"}, indent=2))
print(output)
