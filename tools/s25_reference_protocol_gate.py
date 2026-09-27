#!/usr/bin/env python3
from pathlib import Path
import re
import sys

root=Path("src/main/java/io/github/r3neer/scalebrews/collision/internal")
errors=[]

def normalized_record(path,type_name):
    if not path.is_file():
        errors.append(f"missing {path.name}")
        return None
    text=path.read_text()
    match=re.search(rf"record\s+{re.escape(type_name)}\s*\((.*?)\)\s*implements",text,re.S)
    if not match:
        errors.append(f"cannot parse {type_name} record schema")
        return None
    value=re.sub(r"\s+"," ",match.group(1).strip())
    return re.sub(r"\s*,\s*",",",value)

def freeze(path,type_name,type_id,expected):
    text=path.read_text() if path.is_file() else ""
    marker=f'new Type<>(ScaleBrews.id("{type_id}"))'
    if marker not in text:
        errors.append(f"{type_name} no longer declares expected wire id {type_id}")
        return
    observed=normalized_record(path,type_name)
    if observed is not None and observed!=expected:
        errors.append(
            f"{type_id} schema changed without protocol version bump: "
            f"expected [{expected}] observed [{observed}]"
        )

v1_path=root/"AnatomyMoveReferencePayload.java"
v2_path=root/"AnatomyMoveReferenceV2Payload.java"
receipt_path=root/"AnatomyTransportReceiptPayload.java"

freeze(v1_path,"AnatomyMoveReferencePayload","anatomy_move_reference_v1",
       "boolean vehicle,UUID support,long supportFrameSerial")
freeze(v2_path,"AnatomyMoveReferenceV2Payload","anatomy_move_reference_v2",
       "boolean vehicle,long receiptSequence")
freeze(receipt_path,"AnatomyTransportReceiptPayload","anatomy_transport_receipt_v1",
       "UUID epoch,long revision,Identifier dimension,int bodyId,UUID body,long trackingGeneration,UUID support,long supportFrameSerial,long receiptSequence,long tick")

# V1's third field remains support endpoint identity. Receipt authority belongs to V2.
v1_text=v1_path.read_text() if v1_path.is_file() else ""
for field in ("receiptToken","receiptSequence","transportToken","serverTransportSequence","serverReceiptId"):
    if re.search(rf"\b{re.escape(field)}\b",v1_text):
        errors.append(f"anatomy_move_reference_v1 silently gained new receipt identity field {field}")

if errors:
    print("S25_REFERENCE_PROTOCOL FAIL")
    for error in errors:
        print(" -",error)
    sys.exit(1)

print("S25_REFERENCE_PROTOCOL PASS")
print(" - anatomy_move_reference_v1 remains support+frame identity")
print(" - anatomy_move_reference_v2 remains vehicle+server receiptSequence")
print(" - anatomy_transport_receipt_v1 retains its server-issued token schema")
print(" - semantic/schema changes require an explicitly versioned wire id")
