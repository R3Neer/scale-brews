#!/usr/bin/env python3
from pathlib import Path
import re
import sys

movement=Path("src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyMovement.java")
client=Path("src/client/java/io/github/r3neer/scalebrews/client/collision/network/AnatomyClientNetworking.java")
bridge=Path("src/main/java/io/github/r3neer/scalebrews/collision/integration/MovingBodyContext.java")
hooks=Path("src/main/java/io/github/r3neer/scalebrews/mixin/PlatformEntityMixin.java")

for path in (movement,client,bridge,hooks):
    if not path.is_file():
        raise SystemExit(f"S26_PREDICTION_AUTHORITY FAIL missing {path}")

m=movement.read_text()
c=client.read_text()
b=bridge.read_text()
h=hooks.read_text()
errors=[]

if not re.search(r"public static boolean simulates\(Entity body\)\s*\{\s*return !body\.level\(\)\.isClientSide\(\) \|\| body\.isLocalInstanceAuthoritative\(\);\s*\}",m):
    errors.append("AnatomyMovement.simulates no longer fences client physics by local instance authority")

if not re.search(r"public static boolean predictsBody\(Entity body\)\s*\{\s*return simulates\(body\) && \(!body\.level\(\)\.isClientSide\(\) \|\| body\.getRootVehicle\(\)==body\);\s*\}",m):
    errors.append("AnatomyMovement.predictsBody no longer reduces full client prediction to the unique root actor")

if "if(!predictsBody(body))return;" not in m:
    errors.append("AnatomyMovement.carry no longer fences full prediction through predictsBody")

if "AnatomyMovement.simulates(" in b or "AnatomyMovement.simulates(" in h:
    errors.append("native movement integration admits local replicas without unique-root prediction ownership")
if not re.search(r"return MOVING\.get\(\) == body && AnatomyApi\.ready\(body\) && AnatomyMovement\.predictsBody\(body\)\s*&& AnatomyMovement\.replacesPair\(body, other\);",b):
    errors.append("MovingBodyContext pair suppression lost exact scope/READY/unique-root ownership")
# The removed bridge's four decisions now have two canonical owners. Check each
# live decision, rather than weakening the obligation to a total token count.
for decision in ("carry","afterMove"):
    if not re.search(r"if\(AnatomyApi\.ready\(self\) && AnatomyMovement\.predictsBody\(self\)\)AnatomyMovement\."+decision+r"\(self\);",h):
        errors.append(f"Entity.move {decision} hook lost READY/unique-root authority fence")
if not re.search(r"if\(AnatomyApi\.ready\(self\) && AnatomyMovement\.predictsBody\(self\)\)\s*cir\.setReturnValue\(AnatomyMovement\.collide\(self,cir\.getReturnValue\(\)\)\);",h):
    errors.append("Entity.collide hook lost READY/unique-root authority fence")

loop_pattern=r"for\(var entity:poseLevel\.entitiesForRendering\(\)\)\s*\n\s*if\((?:entity!=predictionBody && )?entity\.isLocalInstanceAuthoritative\(\)\s*&& AnatomyMovement\.contact\(entity\)!=null\)AnatomyMovement\.carry\(entity\);"
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
