"""Render the deterministic S54 rectangle SVG; this is a local-transform sketch."""
import argparse
import re
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image, ImageDraw

parser = argparse.ArgumentParser()
parser.add_argument("svg", type=Path)
parser.add_argument("png", type=Path)
args = parser.parse_args()
root = ET.fromstring(args.svg.read_text(encoding="utf-8"))
image = Image.new("RGB", (int(root.attrib["width"]), int(root.attrib["height"])), "white")
draw = ImageDraw.Draw(image)
for group in root:
    if not group.tag.endswith("g"):
        continue
    tx, ty = map(float, re.findall(r"[-\d.]+", group.attrib["transform"]))
    for element in group:
        a = element.attrib
        if element.tag.endswith("text"):
            draw.text((tx + float(a["x"]), ty + float(a["y"])), element.text, fill="black")
        elif element.tag.endswith("rect"):
            x, y = tx + float(a["x"]), ty + float(a["y"])
            draw.rectangle((x, y, x + float(a["width"]), y + float(a["height"])),
                           fill="#d2e5f3", outline=a["stroke"])
args.png.parent.mkdir(parents=True, exist_ok=True)
image.save(args.png)
