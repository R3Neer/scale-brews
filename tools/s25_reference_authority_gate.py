#!/usr/bin/env python3
from pathlib import Path
import re
import sys

root=Path("src/main/java/io/github/r3neer/scalebrews")
collision=root/"collision"
client_collision=Path("src/client/java/io/github/r3neer/scalebrews/client/collision")
platform_networking=root/"platform"/"PlatformNetworking.java"
platform_reference=root/"platform"/"PlatformMovementReference.java"
platform_outgoing=Path("src/client/java/io/github/r3neer/scalebrews/client/mixin/PlatformOutgoingMoveMixin.java")

errors=[]

# G4 anatomy must not borrow the legacy PlatformMovePayload/reference authority.
for source_root in (collision,client_collision):
    for path in source_root.rglob("*.java"):
        text=path.read_text()
        if "PlatformMovePayload" in text or "PlatformMovementReference" in text:
            errors.append(f"{path}: collision/anatomy code references legacy movement-reference authority")

pn=platform_networking.read_text()
pr=platform_reference.read_text()
po=platform_outgoing.read_text()
if "AnatomyApi.ownsSharedPhysics(body))return;" not in pn:
    errors.append("PlatformNetworking no longer fails closed before accepting legacy PlatformMovePayload under anatomy ownership")
if "AnatomyApi.ownsSharedPhysics(body)" not in pr or "pendingReference=null" not in pr:
    errors.append("PlatformMovementReference no longer clears/ignores legacy references under anatomy ownership")
if "AnatomyApi.ownsSharedPhysics(body)) return;" not in po:
    errors.append("PlatformOutgoingMoveMixin no longer suppresses legacy PlatformMovePayload under anatomy ownership")

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
print(" - legacy PlatformMovePayload remains excluded from anatomy ownership")
if registered_serverbound:
    print(" - registered collision C2S payload schemas are metadata-only:")
    for type_name in sorted(registered_serverbound):
        print("   ",type_name)
else:
    print(" - no collision/anatomy serverbound payload exists yet; presence gate remains independently RED")
