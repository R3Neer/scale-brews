#!/usr/bin/env python3
from pathlib import Path
import re
import sys

root=Path("src/main/java/io/github/r3neer/scalebrews")
collision=root/"collision"
client_collision=Path("src/client/java/io/github/r3neer/scalebrews/client/collision")
outgoing=Path("src/client/java/io/github/r3neer/scalebrews/client/mixin/AnatomyOutgoingMoveMixin.java")
mixins=Path("src/client/resources/scalebrews.client.mixins.json")

errors=[]

# S44 retires the old authority instead of retaining a dormant legacy receiver.
legacy_names=("PlatformNetworking", "PlatformMovementReference", "PlatformOutgoingMoveMixin", "PlatformMovePayload")
production_roots=(root, Path("src/client/java/io/github/r3neer/scalebrews"))
callers=[]
for source_root in production_roots:
    for path in source_root.rglob("*.java"):
        text=path.read_text()
        for name in legacy_names:
            if path.stem==name or re.search(rf"\b{re.escape(name)}\b",text):
                errors.append(f"{path}: retired legacy movement-reference authority {name} remains")
        # Qualified and same-owner calls are audited; the method declaration is not a call.
        call_text=re.sub(r"public\s+static\s+void\s+sendMovementReference\s*\(", "", text)
        callers.extend(path for _ in re.finditer(r"\bsendMovementReference\s*\(",call_text))
if callers != [outgoing]:
    errors.append(f"movement references require exactly one canonical emitter; found {callers}")
if not outgoing.is_file():
    errors.append(f"missing canonical movement-reference emitter {outgoing}")
else:
    po=outgoing.read_text()
    if not re.search(r"if\(client\.player==null \|\| !client\.isSameThread\(\)\) return;",po):
        errors.append("canonical emitter lost current local-player/client-thread fence")
    if not re.search(r"ServerboundMovePlayerPacket p && p\.hasPosition\(\)",po) or "body=client.player.getRootVehicle();" not in po:
        errors.append("canonical emitter lost native positional player/vehicle selection")
    if not re.search(r"if\([^\n]*AnatomyApi\.ownsSharedPhysics\(body\)\)\s*[^;]*AnatomyClientNetworking\.sendMovementReference\(body\);",po):
        errors.append("canonical emitter lost shared-physics ownership fence")
try:
    import json
    config=json.loads(mixins.read_text())
    configured=config.get("client",[])+config.get("mixins",[])
    if configured.count("AnatomyOutgoingMoveMixin") != 1 or any(name in configured for name in legacy_names):
        errors.append("client mixins require exactly one canonical emitter and no legacy emitter")
except (OSError,ValueError) as error:
    errors.append(f"cannot audit canonical emitter mixin configuration: {error}")

# A G4 reference names server-issued state. It may carry scalar/identifier metadata, but never
# upload geometry, pose channels, contact DTOs, matrices or material-frame authority.
forbidden_tokens=(
    "AnatomyContactPayload",
    "AnatomyPosePayload",
    "ModelGeometry",
    "ConvexBox",
    "SurfaceContact",
    "GeometryProvider.CausalEndpoint",
    "GeometryProvider.Snapshot",
    "PoseEngine.Inputs",
    "RootTransformProvider.RootTransform",
    "RootFrame",
    "Matrix3f",
    "Matrix4f",
    "Quaternionf",
    "HierarchyMotion",
)
forbidden_field_names=(
    "localPoint",
    "normal",
    "rootTransform",
    "poseInputs",
    "geometry",
    "convex",
    "matrix",
    "quaternion",
    # A client-local carry cursor may stage emission, but is not server-issued receipt authority.
    "localTransportSequence",
)

java_files=list(collision.rglob("*.java"))
source_by_type={}
for path in java_files:
    text=path.read_text()
    for match in re.finditer(r"\b(?:record|class)\s+(\w+)",text):
        source_by_type.setdefault(match.group(1),(path,text))

registered_serverbound=set()
receiver_types=set()
receiver_files=[]

for path in java_files:
    text=path.read_text()
    for match in re.finditer(
        r"PayloadTypeRegistry\.serverboundPlay\(\)\.register\(\s*(\w+)\.TYPE\s*,\s*\1\.CODEC\s*\)",
        text,
    ):
        registered_serverbound.add(match.group(1))
    for match in re.finditer(r"ServerPlayNetworking\.registerGlobalReceiver\(\s*(\w+)\.TYPE",text):
        receiver_types.add(match.group(1))
        receiver_files.append(path)

# Every collision serverbound payload schema is audited independently of receiver implementation.
for type_name in sorted(registered_serverbound):
    item=source_by_type.get(type_name)
    if item is None:
        errors.append(f"cannot locate source for registered collision serverbound payload {type_name}")
        continue
    path,text=item
    for token in forbidden_tokens:
        if token in text and token != type_name:
            errors.append(f"{path}: serverbound payload {type_name} embeds forbidden physical authority type {token}")
    header_match=re.search(rf"\brecord\s+{re.escape(type_name)}\s*\((.*?)\)\s*implements",text,re.S)
    if header_match:
        header=header_match.group(1)
        for field in forbidden_field_names:
            if re.search(rf"\b{re.escape(field)}\b",header):
                errors.append(f"{path}: serverbound payload {type_name} exposes forbidden authority field {field}")

# A receiver without matching serverbound registration is not a valid anatomy C2S surface.
for type_name in sorted(receiver_types-registered_serverbound):
    errors.append(f"collision C2S receiver {type_name} lacks matching serverbound payload registration")

if errors:
    print("S25_REFERENCE_AUTHORITY_GATE FAIL")
    for error in errors:
        print(" -",error)
    sys.exit(1)

print("S25_REFERENCE_AUTHORITY_GATE PASS")
print(" - retired legacy authority is absent; one canonical native-packet emitter is ownership-fenced")
if registered_serverbound:
    print(" - registered collision C2S payload schemas are metadata-only:")
    for type_name in sorted(registered_serverbound):
        print("   ",type_name)
else:
    print(" - no collision/anatomy serverbound payload exists yet; presence gate remains independently RED")
