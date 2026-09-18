#!/usr/bin/env python3
from pathlib import Path
import sys

root=Path("src/main/java/io/github/r3neer/scalebrews")
candidates=[]
for path in root.rglob("*.java"):
    text=path.read_text()
    # The legacy PlatformMovePayload receiver is intentionally not the G4 anatomy reference path.
    if path.as_posix().endswith("/platform/PlatformNetworking.java"):
        continue
    if "ServerPlayNetworking.registerGlobalReceiver" in text or "serverboundPlay().register" in text:
        if "Anatom" in text or "collision" in path.as_posix():
            candidates.append(path.as_posix())

if not candidates:
    print("S25_REFERENCE_PRESENCE RED")
    print(" - no non-legacy anatomy/collision C2S receiver is registered")
    print(" - current PlatformMovePayload is legacy-only and exits under ownsSharedPhysics")
    print(" - G4.1 requires a bounded metadata-only reference path backed by server receipts")
    sys.exit(1)

print("S25_REFERENCE_PRESENCE CANDIDATE")
for path in candidates:
    print(" -",path)
print("Presence only: adversarial behavior/authority gates are still required before G4.1 can close.")
