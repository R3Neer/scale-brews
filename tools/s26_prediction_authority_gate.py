#!/usr/bin/env python3
from pathlib import Path
import re
import sys

movement=Path("src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyMovement.java")
client=Path("src/client/java/io/github/r3neer/scalebrews/client/collision/network/AnatomyClientNetworking.java")
bridge=Path("src/main/java/io/github/r3neer/scalebrews/platform/PlatformPhysics.java")

for path in (movement,client,bridge):
    if not path.is_file():
        raise SystemExit(f"S26_PREDICTION_AUTHORITY FAIL missing {path}")

m=movement.read_text()
c=client.read_text()
b=bridge.read_text()
errors=[]

if not re.search(r"public static boolean simulates\(Entity body\)\s*\{\s*return !body\.level\(\)\.isClientSide\(\) \|\| body\.isLocalInstanceAuthoritative\(\);\s*\}",m):
    errors.append("AnatomyMovement.simulates no longer fences client physics by local instance authority")

if not re.search(r"public static boolean predictsBody\(Entity body\)\s*\{\s*return simulates\(body\) && \(!body\.level\(\)\.isClientSide\(\) \|\| body\.getRootVehicle\(\)==body\);\s*\}",m):
    errors.append("AnatomyMovement.predictsBody no longer reduces full client prediction to the unique root actor")

if "if(!predictsBody(body))return;" not in m:
    errors.append("AnatomyMovement.carry no longer fences full prediction through predictsBody")

if "AnatomyMovement.simulates(" in b:
    errors.append("PlatformPhysics shared-anatomy bridge still admits local replicas without unique-root prediction ownership")
if b.count("AnatomyMovement.predictsBody(")<4:
    errors.append("PlatformPhysics does not route all anatomy suppress/collide/afterMove/carry decisions through predictsBody")

loop_pattern=r"for\(var entity:poseLevel\.entitiesForRendering\(\)\)\s*\n\s*if\(entity\.isLocalInstanceAuthoritative\(\) && AnatomyMovement\.contact\(entity\)!=null\)AnatomyMovement\.carry\(entity\);"
if not re.search(loop_pattern,c):
    errors.append("client END_CLIENT_TICK carry loop no longer requires local instance authority")

send_start=c.find("public static void sendMovementReference")
if send_start<0:
    errors.append("missing sendMovementReference authority boundary")
else:
    send_tail=c[send_start:c.find("\n    }",send_start)+6]
    if "!body.isLocalInstanceAuthoritative()" not in send_tail:
        errors.append("sendMovementReference no longer requires local instance authority")
    if "body.getControllingPassenger()!=player" not in send_tail:
        errors.append("vehicle reference sender no longer requires the local player to control the vehicle")

if errors:
    print("S26_PREDICTION_AUTHORITY FAIL")
    for error in errors:
        print(" -",error)
    sys.exit(1)

print("S26_PREDICTION_AUTHORITY PASS")
print(" - core client simulation is local-authority gated")
print(" - ordinary client carry loop is local-authority gated")
print(" - outgoing prediction reference is local/control gated")
