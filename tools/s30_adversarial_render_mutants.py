"""Prepare test-only source replacements; production files are never modified."""
from pathlib import Path
import argparse
import json

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = Path("src/client/java/io/github/r3neer/scalebrews/client/collision/presentation")
MUTANTS = {
    "tracking-alias": ("CausalModelRender.java", "&& frame.trackingGeneration()==token.trackingGeneration()", "&& true"),
    "signed-parity": ("CausalModelPartAdapter.java", "if(Integer.bitCount(mask)!=changes)continue;", "if(mask!=0 || Integer.bitCount(mask)!=changes)continue;"),
    "geometry-mismatch": ("CausalModelPartAdapter.java", "if(accepted.parts().size()!=visible.parts().size())return false;", "if(accepted != null)return true;\n        if(accepted.parts().size()!=visible.parts().size())return false;"),
    "restore-missing": ("CausalModelPartAdapter.java", "closed=true;for(int i=saved.size()-1;i>=0;i--)saved.get(i).restore();", "closed=true;"),
    "delayed-local-missing": ("CausalModelRender.java", "try {original.run();} finally {applied.close();}", "applied.close();original.run();"),
    "retired-draw": ("CausalModelRender.java", "if(frame==null || support==null)return;\n        var adapter=prepared(rendererModel,frame,support);\n        if(adapter==null)return;\n        var applied=adapter.apply", "if(frame==null || support==null){original.run();return;}\n        var adapter=prepared(rendererModel,frame,support);\n        if(adapter==null)return;\n        var applied=adapter.apply"),
    "root-double": ("CausalModelRender.java", "poses.mulPose(correction);", "poses.mulPose(correction);poses.mulPose(correction);"),
}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mutant", choices=MUTANTS)
    parser.add_argument("--output", type=Path, default=ROOT.parent / "s30-adversarial-mutants")
    args = parser.parse_args()
    name, old, new = MUTANTS[args.mutant]
    original = ROOT / PACKAGE / name
    source = original.read_text(encoding="utf-8")
    if source.count(old) != 1:
        raise SystemExit(f"Ambiguous/stale mutation anchor: {args.mutant}")
    directory = args.output.resolve() / args.mutant
    directory.mkdir(parents=True, exist_ok=True)
    (directory / name).write_text(source.replace(old, new), encoding="utf-8")
    print(json.dumps({"mutant": args.mutant, "original": str(original), "directory": str(directory)}))

if __name__ == "__main__":
    main()
