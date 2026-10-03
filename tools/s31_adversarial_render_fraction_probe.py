"""Exercise revised S31 pure relation against the pinned vanilla draw-position lerp."""
import json
from s31_implementer_plan_model import Relation

point = (0., 0., 0.)
delta = (.2, 0., 0.)
relation = Relation(point)
relation.endpoint(2, lambda p: p, lambda p: tuple(v + d for v, d in zip(p, delta)))
relation.carry(2, 1, point, delta)
# Actual carry writes Entity.setPos(current + applied) without setting xOld.
# EntityRenderer.extractRenderState in installed Minecraft 26.2 uses
# Mth.lerp(partial, body.xOld, body.getX()). S30 draws CURRENT_ENDPOINT by default.
old_x, current_x, accepted_material_x = 0., .2, .2
samples = []
for partial in (0., .25, .5, 1.):
    vanilla_body_x = old_x + partial * (current_x - old_x)
    drawn_body_x = vanilla_body_x + relation.target[0]
    samples.append({'partial': partial, 'vanillaBodyX': vanilla_body_x,
                    'publishedResidualX': relation.target[0], 'drawnBodyX': drawn_body_x,
                    'acceptedMaterialX': accepted_material_x,
                    'visibleSlipX': drawn_body_x - accepted_material_x})
print(json.dumps({'classification': 'PLAN R2/R3/R4 / local passive carry draw incorporation',
                  'supportKind': 'CURRENT_ENDPOINT', 'voluntaryMotion': 0.,
                  'appliedCarryX': .2, 'samples': samples,
                  'productionEdited': False}, indent=2))
raise SystemExit(1 if any(abs(s['visibleSlipX']) > 1e-12 for s in samples) else 0)
