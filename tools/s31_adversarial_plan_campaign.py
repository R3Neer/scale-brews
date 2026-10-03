"""Independent reproducible transition algebra checks for the current S31 plan model."""
from dataclasses import astuple
import json
import math
import random
from s31_implementer_plan_model import Relation

rng = random.Random(0x531)
fractions = (0., .25, .5, 1.)


def sub(a, b):
    return tuple(x - y for x, y in zip(a, b))


def affine():
    angle = rng.uniform(-math.pi, math.pi)
    c, s = math.cos(angle), math.sin(angle)
    sx, sy, sz = [rng.uniform(.5, 2.) for _ in range(3)]
    dx, dy, dz = [rng.uniform(-.3, .3) for _ in range(3)]
    return lambda p: (c*sx*p[0]-s*sz*p[2]+dx, sy*p[1]+dy, s*sx*p[0]+c*sz*p[2]+dz)


def close(a, b):
    assert all(abs(x-y) < 1e-10 for x, y in zip(a, b)), (a, b)


checks = 0
for case in range(512):
    a = tuple(rng.uniform(-.25, .25) for _ in range(3))
    b = tuple(rng.uniform(-.25, .25) for _ in range(3))
    initial = lambda p: p
    middle, latest = affine(), affine()
    applied = sub(latest(b), initial(b))
    voluntary = sub(b, a)
    outcomes = []
    for contact_first in (False, True):
        for burst in (False, True):
            relation = Relation(a)
            if contact_first:
                relation.contact(b)
            if burst:
                relation.endpoint(2, initial, middle, before_serial=1)
                relation.endpoint(3, middle, latest, before_serial=2)
            else:
                relation.endpoint(3, initial, latest, before_serial=1)
            relation.carry(3, 1, b, applied, tick=7, old=(0., 0., 0.), before_serial=1)
            if not contact_first:
                relation.contact(b)
            stamp = astuple(relation)
            relation.carry(3, 1, b, applied, tick=7, old=(0., 0., 0.), before_serial=1)
            assert stamp == astuple(relation)
            offsets = []
            for partial in reversed(fractions):
                offset = relation.draw(partial, 7, (0., 0., 0.))
                vanilla = tuple(partial*(walk+carry) for walk, carry in zip(voluntary, applied))
                expected = tuple(carry+partial*walk for walk, carry in zip(voluntary, applied))
                close(tuple(x+y for x,y in zip(vanilla, offset)), expected)
                assert stamp == astuple(relation)
                offsets.append(offset)
                checks += 1
            assert relation.draw(.5, 8, applied) == (0., 0., 0.)
            assert relation.draw(.5, 7, (.01, 0., 0.)) == (0., 0., 0.)
            outcomes.append(offsets)
    assert outcomes.count(outcomes[0]) == len(outcomes)

    # Matching identity cannot rescue an edge with the wrong exact before frame.
    rejected = Relation(a)
    rejected.endpoint(2, initial, middle, before_serial=1)
    rejected.endpoint(3, middle, latest, before_serial=1)
    assert rejected.pending is None and rejected.draw(.5, 7, (0., 0., 0.)) == (0., 0., 0.)
    checks += 1

print(json.dumps({'classification': 'S31 PLAN independent transition campaign GREEN',
                  'seed': 0x531, 'cases': 512, 'fractionAssertions': 8192,
                  'totalAssertionsGrouped': checks, 'productionEdited': False,
                  'nativeGameplayAcceptance': False}, indent=2))
