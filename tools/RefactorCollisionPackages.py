from pathlib import Path
import re

root=Path(__file__).resolve().parents[1]
src=root/'src'
base='io.github.r3neer.scalebrews'
old=f'{base}.platform.anatomy'

packages={
    # Public DTO/API-adjacent types.
    'AnatomyMode': f'{base}.collision.api',
    'GravityFrame': f'{base}.collision.api',
    'SurfaceContact': f'{base}.collision.api',
    # Pure geometry/data primitives. No live entity lifecycle here.
    'ConvexBox': f'{base}.collision.geometry',
    'ModelGeometry': f'{base}.collision.geometry',
    'AnatomyFilter': f'{base}.collision.geometry',
    # Reusable pose primitives/engines. Species-specific proof code stays in GameTest fixtures.
    'PoseProvider': f'{base}.collision.pose',
    'PoseChannels': f'{base}.collision.pose',
    'PlayerWalkingPose': f'{base}.collision.pose',
    'QuadrupedPose': f'{base}.collision.pose',
    'VanillaFamilyPose': f'{base}.collision.pose',
    'PoseProviders': f'{base}.collision.pose',
    # Pure physics/kernel types.
    'ConservativeSweep': f'{base}.collision.physics',
    'AnatomySeparation': f'{base}.collision.physics',
    'TemporalResponse': f'{base}.collision.physics',
    'BodyPath': f'{base}.collision.physics',
    'SupportTransport': f'{base}.collision.physics',
}

old_dir=root/'src/main/java/io/github/r3neer/scalebrews/platform/anatomy'
all_old=[p for p in old_dir.glob('*.java')]
for p in all_old:
    c=p.stem
    if c=='AnatomyApi':
        # The real API already lives in collision.api. Historical shim is removed now that all repo callers migrate atomically.
        p.unlink()
        continue
    if c=='GrizzlyPose':
        # Species-specific H1 formula is evidence, not a production engine. Keep it only as a GameTest fixture.
        fixture=root/'src/gametest/java/io/github/r3neer/scalebrews/test/GrizzlyPose.java'
        fixture.write_text('''package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** H1 acceptance fixture for Alex's Mobs Continued 2.1.9. Not a production pose engine. */
public final class GrizzlyPose implements PoseProvider {
    public Optional<Map<String,Matrix4f>> evaluate(ModelGeometry geometry,Inputs in) {
        if(!in.ordinary() || !geometry.source().equals("alexsmobs:grizzly_bear") || !geometry.version().equals("2.1.9"))return Optional.empty();
        float phase=in.walkPhase()*.7f,amount=in.walkAmount(),walk=Mth.cos(phase)*.7f*amount;
        float bob=(float)(Math.sin(phase)*amount*.7f-amount*.7f);
        Map<String,Matrix4f> result=new LinkedHashMap<>();
        for(var p:geometry.parts()) {
            String name=p.id().substring(p.id().lastIndexOf('/')+1);
            float x=0,y=0,z=0,dy=0;
            switch(name) {
                case "left_leg" -> {x=walk;dy=2*bob/16;}
                case "right_leg","left_arm" -> x=-walk;
                case "right_arm" -> x=walk;
                case "midbody" -> z=Mth.cos(phase+1)*(.7f*.2f)*amount;
                case "body" -> {z=Mth.cos(phase+2)*(.7f*.2f)*amount;dy=(float)-Math.abs(Math.sin(phase)*amount*.7f)/16;}
                case "head" -> {x=in.headPitch()*((float)Math.PI/180);y=in.headYaw()*((float)Math.PI/180);z=Mth.cos(phase+2)*(.7f*-.1f)*amount;}
                default -> {continue;}
            }
            var rest=ModelGeometry.matrix(p.transform());
            var pos=rest.getTranslation(new Vector3f());pos.y+=dy;
            result.put(p.id(),new Matrix4f().translation(pos).rotateZYX(z,y,x).scale(rest.getScale(new Vector3f())));
        }
        return result.size()==7?Optional.of(Collections.unmodifiableMap(result)):Optional.empty();
    }

    /** Server-safe guard retained only to validate the old 2.1.9 vertical slice. */
    public static Predicate<LivingEntity> guard() {
        return entity->{
            if(!entity.getClass().getName().equals("com.github.alexthe666.alexsmobs.entity.EntityGrizzlyBear")
                || !net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("alexsmobs")
                    .map(mod->mod.getMetadata().getVersion().getFriendlyString().equals("2.1.9")).orElse(false))return false;
            try {
                var type=entity.getClass();
                if((boolean)type.getMethod("isSitting").invoke(entity) || (boolean)type.getMethod("isStanding").invoke(entity)
                    || (boolean)type.getMethod("isEating").invoke(entity) || (boolean)type.getMethod("isEatingHeldItem").invoke(entity)
                    || (boolean)type.getMethod("isFreddy").invoke(entity))return false;
                Object current=type.getMethod("getAnimation").invoke(entity),none=type.getField("NO_ANIMATION").get(null);
                if(current!=null && current!=none)return false;
                for(String name:List.of("standProgress","sitProgress","prevStandProgress","prevSitProgress"))
                    if(type.getField(name).getFloat(entity)!=0)return false;
                return true;
            }catch(ReflectiveOperationException invalid){return false;}
        };
    }
}
''')
        p.unlink()
        continue
    packages.setdefault(c, f'{base}.collision.internal')

# Move source files and update their package declaration.
for c,pkg in packages.items():
    src_path=old_dir/f'{c}.java'
    if not src_path.exists():
        raise SystemExit(f'Missing expected source {src_path}')
    dest=root/'src/main/java'/Path(pkg.replace('.','/'))/f'{c}.java'
    dest.parent.mkdir(parents=True,exist_ok=True)
    text=src_path.read_text()
    text=text.replace(f'package {old};',f'package {pkg};',1)
    dest.write_text(text)
    src_path.unlink()

# Client runtime networking belongs to collision, not the legacy platform client.
client_old=root/'src/client/java/io/github/r3neer/scalebrews/client/platform/anatomy/AnatomyClientNetworking.java'
client_new=root/'src/client/java/io/github/r3neer/scalebrews/client/collision/network/AnatomyClientNetworking.java'
client_new.parent.mkdir(parents=True,exist_ok=True)
t=client_old.read_text().replace(f'package {base}.client.platform.anatomy;',f'package {base}.client.collision.network;',1)
client_new.write_text(t); client_old.unlink()

# Geometry extractor's implementation was already moved in the previous pass; remove its old forwarding shim.
geo_shim=root/'src/client/java/io/github/r3neer/scalebrews/client/platform/anatomy/GeometryExtractor.java'
if geo_shim.exists(): geo_shim.unlink()

# Map every old fully-qualified class to its new package.
fq_map={f'{old}.{c}':f'{pkg}.{c}' for c,pkg in packages.items()}
fq_map[f'{old}.AnatomyApi']=f'{base}.collision.api.AnatomyApi'
fq_map[f'{base}.client.platform.anatomy.AnatomyClientNetworking']=f'{base}.client.collision.network.AnatomyClientNetworking'
fq_map[f'{base}.client.platform.anatomy.GeometryExtractor']=f'{base}.client.collision.preparation.GeometryExtractor'

collision_wildcards='\n'.join([
    f'import {base}.collision.api.*;',
    f'import {base}.collision.geometry.*;',
    f'import {base}.collision.pose.*;',
    f'import {base}.collision.physics.*;',
    f'import {base}.collision.internal.*;',
])

# Java files anywhere in src can reference the previous package explicitly or implicitly through wildcard imports.
for p in src.rglob('*.java'):
    text=p.read_text()
    # Old wildcard imports must now cover the responsibility packages plus transitional orchestration package.
    text=text.replace(f'import {old}.*;', collision_wildcards)
    # Exact imports/FQNs.
    for a,b in sorted(fq_map.items(), key=lambda kv:-len(kv[0])):
        text=text.replace(a,b)
    # Retire obsolete implementation-phase labels from source comments.
    text=text.replace('Shared movement integration under explicit proof activation until Gate 1 passes.',
                      'Shared movement integration for the replacement entity-collision runtime.')
    text=text.replace('Legacy fixture overload. Runtime must use the causal-descriptor overload once T2 publishes it.',
                      'Fixture-only overload. Production runtime uses the causal-descriptor overload.')
    text=text.replace('T2-only client receiver proof.', 'Client receiver proof.')
    # The Alex 2.1.9 species-specific vertical slice is now test-only; production keeps only reusable engines/adapters.
    if p.name=='PoseProviders.java':
        text=text.replace('        register(Identifier.parse("scalebrews:grizzly"),new GrizzlyPose());\n','')
    if p.name=='AnatomyPoseEligibility.java':
        text=re.sub(r'\n    private static final ClassValue<java\.util\.function\.Predicate<LivingEntity>> GRIZZLY=new ClassValue<>\(\) \{.*?^    \};\n', '\n', text, flags=re.S|re.M)
        text=re.sub(r'\n            case "scalebrews:grizzly"->entity\.getClass\(\)\.getName\(\)\.equals\("com\.github\.alexthe666\.alexsmobs\.entity\.EntityGrizzlyBear"\)\n                && net\.fabricmc\.loader\.api\.FabricLoader\.getInstance\(\)\.getModContainer\("alexsmobs"\)\.map\(mod->mod\.getMetadata\(\)\.getVersion\(\)\.getFriendlyString\(\)\.equals\("2\.1\.9"\)\)\.orElse\(false\)\n                && GRIZZLY\.get\(entity\.getClass\(\)\)\.test\(entity\);', '', text)
    if p.name=='AnatomyGeometryTests.java':
        text=text.replace('        var provider=net.minecraft.resources.Identifier.parse("scalebrews:grizzly");\n        try {',
                          '        var provider=net.minecraft.resources.Identifier.parse("scalebrews:grizzly");\n        try{AnatomyPoseEligibility.register(provider,GrizzlyPose.guard());}catch(IllegalArgumentException alreadyRegistered){}\n        try {')
    # Files formerly in one flat package referenced siblings without imports. Add only the
    # responsibility-package imports they actually need, so package boundaries remain inspectable.
    package_match=re.search(r'^package\s+([^;]+);',text,re.M)
    if package_match and package_match.group(1).startswith(f'{base}.collision.'):
        current_package=package_match.group(1)
        class_packages=dict(packages)
        class_packages['AnatomyApi']=f'{base}.collision.api'
        required=[]
        for class_name,target_package in class_packages.items():
            if target_package==current_package or not re.search(r'\b'+re.escape(class_name)+r'\b',text):
                continue
            fq=f'{target_package}.{class_name}'
            if f'import {fq};' not in text and fq not in text:
                required.append(f'import {fq};')
        if required:
            end=package_match.end()
            text=text[:end]+'\n\n'+'\n'.join(sorted(set(required)))+text[end:]
    p.write_text(text)

# Non-Java source/tool files occasionally contain literal class/package names.
for folder in [src, root/'tools']:
    if not folder.exists(): continue
    for p in folder.rglob('*'):
        if not p.is_file() or p.suffix.lower() not in {'.json','.ps1','.gradle','.properties','.md','.txt'}: continue
        try: text=p.read_text()
        except UnicodeDecodeError: continue
        before=text
        for a,b in sorted(fq_map.items(), key=lambda kv:-len(kv[0])):
            text=text.replace(a,b)
        if text!=before: p.write_text(text)

# Remove now-empty directories.
for d in [old_dir, root/'src/client/java/io/github/r3neer/scalebrews/client/platform/anatomy']:
    if d.exists() and not any(d.iterdir()): d.rmdir()

# Guardrails: no source/tool reference may retain the retired anatomy package/client paths.
needles=[old, f'{base}.client.platform.anatomy']
for needle in needles:
    hits=[]
    for folder in [src, root/'tools']:
        for p in folder.rglob('*'):
            if not p.is_file(): continue
            try: text=p.read_text()
            except UnicodeDecodeError: continue
            if needle in text: hits.append(str(p.relative_to(root)))
    if hits: raise SystemExit(f'Retired package reference {needle}: {hits}')
