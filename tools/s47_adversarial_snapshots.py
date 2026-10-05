"""Compare and draw deterministic S47 original-Cow variant snapshots (requires Pillow)."""
import argparse
import json
from pathlib import Path
from PIL import Image, ImageDraw

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("snapshots", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    first = json.loads((args.snapshots / "variant-a.json").read_text(encoding="utf-8"))
    second = json.loads((args.snapshots / "variant-b.json").read_text(encoding="utf-8"))
    a, b = first["pieces"], second["pieces"]
    removed = {key: value for key, value in a.items() if key not in b}
    if len(removed) != 1 or any(key not in a or a[key] != value for key, value in b.items()):
        raise AssertionError("Exactly one declared piece must retire; all retained vertices must match")
    if first["origin"] != second["origin"] or first["rootQuaternion"] != second["rootQuaternion"] or first["pose"] != second["pose"]:
        raise AssertionError("Snapshots have different root or pose inputs")
    image = Image.new("RGB", (1350, 650), "white")
    draw = ImageDraw.Draw(image)
    draw.text((30, 20), "S47: fixed neutral pose, identity root, common projection, original Cow 26.2", fill="black")
    edges = [(i, i ^ (1 << axis)) for i in range(8) for axis in range(3) if i < (i ^ (1 << axis))]
    def project(vertex, panel):
        return panel * 450 + 80 + (vertex[0] - .5 * vertex[2] - .4) * 180, 580 - (vertex[1] - .25 * vertex[2] - 2.2) * 210
    def boxes(panel, pieces, color):
        for vertices in pieces.values():
            for i, j in edges:
                draw.line([project(vertices[i], panel), project(vertices[j], panel)], fill=color, width=2)
    titles = ["A:10 pieces - original reference", "B:9 pieces - exact variant token", "Overlay: red = declared exclusion"]
    for panel, title in enumerate(titles):
        draw.text((panel * 450 + 35, 70), title, fill="black")
        draw.rectangle((panel * 450 + 25, 110, panel * 450 + 425, 610), outline="#dddddd")
    boxes(0, a, "#2166ac")
    boxes(1, b, "#1b9e77")
    boxes(2, b, "#1b9e77")
    boxes(2, removed, "#d73027")
    target = args.output or args.snapshots / "variant-comparison.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target)
    print(json.dumps({"firstPieces": len(a), "secondPieces": len(b), "retired": list(removed), "retainedVerticesExact": True, "image": str(target)}))

if __name__ == "__main__":
    main()
