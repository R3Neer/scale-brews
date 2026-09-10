#!/usr/bin/env python3
import pathlib, sys

mutation=sys.argv[1]
root=pathlib.Path('.')
cases={
 'dispatcher-reentry': ('src/main/java/io/github/r3neer/scalebrews/collision/internal/MaterialEventDispatcher.java',
   'if(draining || notifyingQuarantine)return quarantine(event,Reason.GATE_VIOLATION,backend);',
   'if(draining)return quarantine(event,Reason.GATE_VIOLATION,backend);'),
 'dispatcher-abort-clear': ('src/main/java/io/github/r3neer/scalebrews/collision/internal/MaterialEventDispatcher.java',
   '} finally {queue.clear();draining=false;admittedEvents=0;}',
   '} finally {draining=false;admittedEvents=0;}'),
 'sat-axis-cutoff': ('src/main/java/io/github/r3neer/scalebrews/collision/geometry/ConvexBox.java',
   'if(length>0)normalized.add(axis.scale(1/length));',
   'if(length>1e-10)normalized.add(axis.scale(1/length));'),
 'bodypath-certificate': ('src/main/java/io/github/r3neer/scalebrews/collision/physics/BodyPath.java',
   ' || support+1e-9<bounds.minDotAntiGravity()', ''),
 'receipt-current-tick': ('src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyTransportReceipts.java',
   'if(transport.tick()!=tick || transport.rootFrameSequence()!=root.sequence() || root.tick()>tick || surface.tick()>tick)return;',
   'if(transport.rootFrameSequence()!=root.sequence() || root.tick()>tick || surface.tick()>tick)return;'),
 'receipt-surface': ('src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyTransportReceipts.java',
   '            new SurfaceContact(support,catalogRevision,piece,face,localPoint,normal,surfaceTick);\n', ''),
 'frame-binding-generation': ('src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyFrameHistory.java',
   ' || current.bindingGeneration()!=next.bindingGeneration()', ''),
 'model-unknown-joint': ('src/main/java/io/github/r3neer/scalebrews/collision/geometry/ModelGeometry.java',
   '        if(!ids.containsAll(replacements.keySet()))throw new IllegalArgumentException("Unknown model joint");\n', ''),
 'runtime-thread-owner': ('src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyMovement.java',
   'if(!level.isClientSide() && server!=null && !server.isSameThread())',
   'if(false && !level.isClientSide() && server!=null && !server.isSameThread())'),
 'endpoint-same-serial': ('src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyMovement.java',
   'if(!old.endpoint().equals(endpoint) || !Objects.equals(old.snapshot(),snapshot))return quarantineEndpoint(support);',
   'if(false)return quarantineEndpoint(support);'),
 'client-receipt-invalidate': ('src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyTransportReceipts.java',
   'if(body==null || body.level().isClientSide())return;',
   'if(body==null)return;'),
}
if mutation not in cases:
    raise SystemExit('unknown mutation '+mutation)
path,old,new=cases[mutation]
p=root/path
text=p.read_text()
count=text.count(old)
if count!=1:
    raise SystemExit(f'{mutation}: expected exactly one target, found {count}')
p.write_text(text.replace(old,new))
print(f'MUTATION_APPLIED {mutation} {path}')
