from pathlib import Path
import json,hashlib
root=Path(__file__).resolve().parents[1]
import argparse
parser=argparse.ArgumentParser(description='Create isolated S49 shadow sources; never edit production. Pass s49ShadowDir/s49ShadowOriginal to public init.')
parser.add_argument('output',type=Path)
out=parser.parse_args().output
api='src/main/java/io/github/r3neer/scalebrews/collision/api/CollisionVariantSources.java'
builtin='src/main/java/io/github/r3neer/scalebrews/collision/internal/BuiltInVariantSources.java'
def mutate(path,old,new):
    source=(root/path).read_text(encoding='utf-8')
    assert source.count(old)==1,(path,old)
    return source.replace(old,new)
recipes={
 'foreign-enum':(api,mutate(api,'value.getDeclaringClass() != enumType','false')),
 'constant-subclass':(api,mutate(api,'value.getDeclaringClass() != enumType','value.getClass() != enumType')),
 'runtime-serialize':(api,mutate(api,'return names[value.ordinal()];','return ((StringRepresentable)value).getSerializedName();')),
 'key-escape':(api,mutate(api,'value.replace("_", "_5f").replace(".", "_2e").replace("/", "_2f")','value.replace(".", "_2e").replace("/", "_2f")')),
 'discovery-omitted':(builtin,mutate(builtin,'DataComponents.class.getFields()','new java.lang.reflect.Field[0]')),
 'ownership-blind':(builtin,mutate(builtin,'existing.reader() != source.reader()','false')),
 'reader-registry-copy':(api,mutate(api,'return names[value.ordinal()];','CollisionVariantSources.snapshot();return names[value.ordinal()];')),
}
receipt={}
for name,(original,source) in recipes.items():
    path=out/name/Path(original).relative_to('src/main/java')
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(source,encoding='utf-8',newline='\n')
    receipt[name]={'original':original,'productionSha256':hashlib.sha256((root/original).read_bytes()).hexdigest(),'shadowSha256':hashlib.sha256(path.read_bytes()).hexdigest()}
(out/'mutations.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8')
print('generated',len(receipt),'isolated shadow sources')
