#!/usr/bin/env python3
from pathlib import Path
import re
import sys

root=Path("src/main/java/io/github/r3neer/scalebrews")
collision=root/"collision"
platform_networking=root/"platform"/"PlatformNetworking.java"
platform_reference=root/"platform"/"PlatformMovementReference.java"

errors=[]

# G4 anatomy must not borrow the legacy PlatformMovePayload/reference authority.
for path in collision.rglob("*.java"):
    text=path.read_text()
    if "PlatformMovePayload" in text or "PlatformMovementReference" in text:
        errors.append(f"{path}: collision/anatomy code references legacy movement-reference authority")

pn=platform_networking.read_text()
pr=platform_reference.read_text()
if "AnatomyApi.ownsSharedPhysics(body))return;" not in pn:
    errors.append("PlatformNetworking no longer fails closed before accepting legacy PlatformMovePayload under anatomy ownership")
if "AnatomyApi.ownsSharedPhysics(body)" not in pr or "pendingReference=null" not in pr:
    errors.append("PlatformMovementReference no longer clears/ignores legacy references under anatomy ownership")

# Any new collision/anatomy C2S receiver must remain metadata-only.
# These types represent physical authority or renderer/model data and are forbidden in a client reference surface.
forbidden=(
    "ModelGeometry",
    "ConvexBox",
    "SurfaceContact",
    "PoseEngine.Inputs",
    "RootTransformProvider.RootTransform",
    "Matrix3f",
    "Matrix4f",
    "Quaternionf",
    "HierarchyMotion",
)
receiver_files=[]
for path in collision.rglob("*.java"):
    text=path.read_text()
    if "ServerPlayNetworking.registerGlobalReceiver" not in text:
        continue
    receiver_files.append(path)
    for token in forbidden:
        if token in text:
            errors.append(f"{path}: anatomy C2S receiver references forbidden physical authority type {token}")

if errors:
    print("S25_REFERENCE_AUTHORITY_GATE FAIL")
    for error in errors:
        print(" -",error)
    sys.exit(1)

print("S25_REFERENCE_AUTHORITY_GATE PASS")
print(" - legacy PlatformMovePayload remains excluded from anatomy ownership")
if receiver_files:
    print(" - anatomy/collision C2S receivers are metadata-only at the source boundary:")
    for path in receiver_files:
        print("   ",path)
else:
    print(" - no anatomy/collision C2S receiver exists yet; presence gate remains independently RED")
