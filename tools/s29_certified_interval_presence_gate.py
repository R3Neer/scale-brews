#!/usr/bin/env python3
from pathlib import Path
import re
import sys

client=Path("src/client/java/io/github/r3neer/scalebrews/client/collision/network/AnatomyClientNetworking.java")
internal=Path("src/main/java/io/github/r3neer/scalebrews/collision/internal")

if not client.is_file():
    raise SystemExit(f"S29_CERTIFIED_INTERVAL FAIL missing {client}")

text=client.read_text()
errors=[]

# The enum reservation is not a capability. There must be an executable production path that
# actually constructs/returns CERTIFIED_INTERVAL rather than only declaring the enum constant.
qualified_uses=[
    line.strip() for line in text.splitlines()
    if "PresentationKind.CERTIFIED_INTERVAL" in line
]
if not qualified_uses:
    errors.append("no production path constructs PresentationKind.CERTIFIED_INTERVAL")

# Q1 currently explicitly rejects interval semantics. Keep this diagnostic separate so the RED
# says why a pair of endpoints cannot yet become a certified presentation frame.
if "kind!=PresentationKind.CURRENT_ENDPOINT" in text:
    errors.append("PresentationFrame constructor still rejects every non-CURRENT_ENDPOINT frame")

# Diagnostic inventory only: a certified Q2 interval should have an S2C/server-issued wire surface.
# Do not prescribe class names; look for payload schemas carrying interval/material identity.
interval_payloads=[]
for path in internal.glob("*Payload.java"):
    source=path.read_text()
    if "CustomPacketPayload" not in source:
        continue
    lowered=source.lower()
    if ("materialserial" in lowered or "intervalserial" in lowered or "intervalsequence" in lowered) and (
        ("before" in lowered and "after" in lowered) or "interval" in lowered
    ):
        interval_payloads.append(path.name)

if not interval_payloads:
    errors.append("no server-issued material-interval payload/schema is present")

if errors:
    print("S29_CERTIFIED_INTERVAL RED capability absent")
    for error in errors:
        print(" -",error)
    sys.exit(1)

print("S29_CERTIFIED_INTERVAL PRESENT")
print(" - product has a CERTIFIED_INTERVAL presentation path")
print(" - candidate interval wire surfaces:",", ".join(interval_payloads))
print("NOTE: presence is not acceptance; behavioral/lifecycle/mutation gates are still required")
