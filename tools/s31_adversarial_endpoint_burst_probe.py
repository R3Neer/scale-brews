"""S31 plan holdout: two accepted support frames precede one physical body carry."""
import json
from s31_implementer_plan_model import Relation

point = (0., 0., 0.)
identity = lambda p: p
first = lambda p: (p[0] + .1, p[1], p[2])
latest = lambda p: (p[0] + .2, p[1], p[2])
body_old = (0., 0., 0.)
applied = (.2, 0., 0.)

direct = Relation(point)
direct.endpoint(3, identity, latest)
direct.carry(3, 1, point, applied, tick=7, old=body_old)
direct_offset = direct.draw(.5, 7, body_old)[0]
assert abs(direct_offset - .1) < 1e-12  # Positive control for the previous repair.

burst = Relation(point)
burst.endpoint(2, identity, first)
burst.endpoint(3, first, latest)
# Local physics still carries the original frame-1 anchor to current frame 3.
# One physical outcome, one exact new sequence, no duplicate or identity barrier.
burst.carry(3, 1, point, applied, tick=7, old=body_old)
burst_offset = burst.draw(.5, 7, body_old)[0]
print(json.dumps({'classification': 'PLAN R2/R3 / accepted endpoint burst before one carry',
                  'acceptedEndpointSerials': [2, 3], 'physicalAnchorBeforeFrame': 1,
                  'physicalCarryAfterFrame': 3, 'localTransportSequence': 1,
                  'appliedPassiveDeltaX': .2, 'partial': .5,
                  'directOffsetX': direct_offset, 'burstOffsetX': burst_offset,
                  'vanillaBodyX': .1, 'acceptedSupportX': .2,
                  'burstDrawnBodyX': .1 + burst_offset,
                  'requiredOffsetX': .1, 'productionEdited': False}, indent=2))
raise SystemExit(1 if abs(burst_offset - direct_offset) > 1e-12 else 0)
