#!/usr/bin/env python3
from pathlib import Path
import re
import sys

path=Path("src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyMoveReferencePayload.java")
if not path.is_file():
    raise SystemExit("S25_REFERENCE_PROTOCOL FAIL: missing AnatomyMoveReferencePayload.java")

text=path.read_text()
errors=[]

v1='new Type<>(ScaleBrews.id("anatomy_move_reference_v1"))'
if v1 in text:
    m=re.search(r"record\s+AnatomyMoveReferencePayload\s*\((.*?)\)\s*implements",text,re.S)
    if not m:
        errors.append("cannot parse anatomy_move_reference_v1 record schema")
    else:
        normalized=" ".join(m.group(1).split())
        expected="boolean vehicle,UUID support,long supportFrameSerial"
        if normalized!=expected:
            errors.append(
                "anatomy_move_reference_v1 schema changed without protocol version bump: "
                f"expected [{expected}] observed [{normalized}]"
            )

    # v1's semantic key is explicitly support endpoint identity. A server-issued receipt/transport
    # token belongs to a new protocol version, not a silent reinterpretation of this wire id.
    forbidden_v1=("receiptToken","receiptSequence","transportToken","serverTransportSequence","serverReceiptId")
    for field in forbidden_v1:
        if re.search(rf"\b{re.escape(field)}\b",text):
            errors.append(f"anatomy_move_reference_v1 silently gained new receipt identity field {field}")

if errors:
    print("S25_REFERENCE_PROTOCOL FAIL")
    for error in errors:
        print(" -",error)
    sys.exit(1)

print("S25_REFERENCE_PROTOCOL PASS")
if v1 in text:
    print(" - anatomy_move_reference_v1 retains its frozen support+frame identity semantics")
    print(" - any different transport identity must use an explicitly versioned wire contract")
else:
    print(" - v1 is absent; a replacement protocol must be reviewed independently")
