#!/usr/bin/env python3
from pathlib import Path
import re
import sys

movement = Path("src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyMovement.java")
root_owner = Path("src/main/java/io/github/r3neer/scalebrews/collision/runtime/RootFrameLedger.java")
endpoint_owner = Path("src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyEndpointLedger.java")

for path in (movement, root_owner, endpoint_owner):
    if not path.is_file():
        raise SystemExit(f"S24 ownership gate: missing {path}")

m = movement.read_text()
r = root_owner.read_text()
e = endpoint_owner.read_text()

errors = []

# The orchestrator may manipulate endpoint/root DTOs, but persistent ownership must stay in the ledgers.
persistent_patterns = [
    (r"(?m)^\s*private\s+static[^\n;]*Map\s*<\s*LivingEntity\s*,[^\n;]*(?:RootFrame|CausalEndpoint)[^\n;]*[;=]",
     "AnatomyMovement declares persistent per-entity RootFrame/CausalEndpoint state"),
    (r"(?m)^\s*private\s+static[^\n;]*ArrayDeque\s*<\s*RootFrame\s*>",
     "AnatomyMovement declares persistent RootFrame history"),
]
for pattern, message in persistent_patterns:
    if re.search(pattern, m):
        errors.append(message)

for legacy in ("FRAME_SERIALS", "EndpointStamp", "EndpointSerial", "RootHistory"):
    if legacy in m:
        errors.append(f"AnatomyMovement reintroduced legacy ownership token {legacy}")

# Positive ownership checks: the extracted owners must remain real state owners and the orchestrator must delegate.
if "HISTORIES" not in r or "Map<LivingEntity,History>" not in r:
    errors.append("RootFrameLedger no longer owns the bounded per-support history")
if "ENTRIES" not in e or "Map<LivingEntity,Entry>" not in e:
    errors.append("AnatomyEndpointLedger no longer owns accepted endpoint state")
if "RootFrameLedger.observe(" not in m:
    errors.append("AnatomyMovement no longer delegates root provenance observation to RootFrameLedger")
if "AnatomyEndpointLedger.get(" not in m or "AnatomyEndpointLedger.put(" not in m:
    errors.append("AnatomyMovement no longer delegates accepted endpoint state to AnatomyEndpointLedger")

if errors:
    print("S24_OWNERSHIP_GATE FAIL")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("S24_OWNERSHIP_GATE PASS root and endpoint state have single extracted owners")
