"""Observational constructor counter in a disposable production-source copy."""
import json
from pathlib import Path
import sys

repo=Path(__file__).resolve().parents[1]
destination=Path(sys.argv[1]).resolve()
assert destination != repo and repo not in destination.parents
relative=Path('io/github/r3neer/scalebrews/collision/internal/HierarchyMotion.java')
original=repo/'src/main/java'/relative
source=original.read_text()
anchor='public final class HierarchyMotion {'
source=source.replace(anchor,anchor+'''
    private static final ThreadLocal<long[]> s29PreparationCount=ThreadLocal.withInitial(()->new long[1]);
    public static long s29AdversarialMotionPreparations(){return s29PreparationCount.get()[0];}
''',1)
anchor='        this.model=Objects.requireNonNull(model);'
assert source.count(anchor)==1
source=source.replace(anchor,'        s29PreparationCount.get()[0]++;\n'+anchor,1)
target=destination/relative
target.parent.mkdir(parents=True,exist_ok=True)
target.write_text(source)
print(json.dumps({'original':str(original),'sourceDir':str(destination),'environment':'main'}))
