"""Pre-implementation event model for S31 local carry-point provenance.

This models one confirmed relation and a pair of accepted support endpoints.
It deliberately does not import or exercise production code.
"""
from dataclasses import dataclass


def sub(a, b):
    return tuple(x - y for x, y in zip(a, b))


def add(a, b):
    return tuple(x + y for x, y in zip(a, b))


def scale(a, factor):
    return tuple(x * factor for x in a)


def rotate_quarter_turn(point):
    x, y, z = point
    return (-z, y, x)


@dataclass
class Relation:
    confirmed_point: tuple
    target: tuple = (0.0, 0.0, 0.0)
    pending: tuple | None = None
    last_sequence: int = 0
    draw_support: tuple = (0.0, 0.0, 0.0)
    draw_applied: tuple = (0.0, 0.0, 0.0)
    draw_tick: int | None = None
    draw_old: tuple | None = None
    current_serial: int = 1

    def contact(self, point):
        # A later server contact must not retroactively reinterpret a carry.
        self.confirmed_point = point

    def endpoint(self, serial, before, after, before_serial=None):
        # In this pure model, callable identity stands in for an exact accepted
        # frame edge. Production must compare causal frame serials instead.
        expected_before = self.pending[0] if self.pending is not None else self.current_serial
        if before_serial is None:
            before_serial = expected_before
        if serial < 1 or before_serial != expected_before or serial <= before_serial or (
            self.pending is not None and before is not self.pending[3]
        ):
            self.target = (0.0, 0.0, 0.0)
            self.pending = None
            self.draw_tick = None
            return
        if self.pending is None:
            self.pending = (serial, before_serial, before, after)
        else:
            # An accepted burst coalesces across its exact shared endpoint.
            self.pending = (serial, self.pending[1], self.pending[2], after)
        # No old confirmed point is used while the physical carry point is unknown.
        self.target = (0.0, 0.0, 0.0)

    def carry(self, serial, sequence, physical_point, applied, tick=7, old=(0.0, 0.0, 0.0), before_serial=None):
        if sequence <= self.last_sequence:
            return
        self.last_sequence = sequence
        if before_serial is None:
            before_serial = self.current_serial
        if self.pending is None or self.pending[0] != serial or self.pending[1] != before_serial:
            self.target = (0.0, 0.0, 0.0)
            self.pending = None
            self.draw_tick = None
            return
        _, _, before, after = self.pending
        induced = sub(after(physical_point), before(physical_point))
        if self.draw_tick != tick or self.draw_old != old:
            self.draw_support = (0.0, 0.0, 0.0)
            self.draw_applied = (0.0, 0.0, 0.0)
        self.draw_support = add(self.draw_support, induced)
        self.draw_applied = add(self.draw_applied, applied)
        self.draw_tick = tick
        self.draw_old = old
        self.target = sub(self.draw_support, self.draw_applied)
        self.current_serial = serial
        self.pending = None

    def draw(self, partial, tick, old):
        if self.pending is not None or self.draw_tick != tick or self.draw_old != old:
            return (0.0, 0.0, 0.0)
        return sub(self.draw_support, scale(self.draw_applied, partial))


def run(contact_first):
    a = (.25, 0.0, 0.0)
    b = (.5, 0.0, 0.0)
    relation = Relation(a)
    if contact_first:
        relation.contact(b)
    relation.endpoint(2, lambda point: point, rotate_quarter_turn)
    assert relation.target == (0.0, 0.0, 0.0)
    applied = sub(rotate_quarter_turn(b), b)
    relation.carry(2, 1, b, applied)
    if not contact_first:
        relation.contact(b)
    assert relation.target == (0.0, 0.0, 0.0)
    relation.carry(2, 1, b, applied)  # duplicate is idempotent
    assert relation.target == (0.0, 0.0, 0.0)
    relation.contact(b)  # heartbeat is idempotent
    assert relation.target == (0.0, 0.0, 0.0)
    return relation.target


if __name__ == "__main__":
    assert run(False) == run(True)
    stable = Relation((.25, 0.0, 0.0))
    stable.contact((.5, 0.0, 0.0))  # voluntary walk, no support endpoint
    assert stable.target == (0.0, 0.0, 0.0)
    unknown = Relation((.25, 0.0, 0.0))
    unknown.endpoint(2, lambda point: point, rotate_quarter_turn)
    unknown.carry(3, 1, (.5, 0.0, 0.0), (0.0, 0.0, 0.0))
    assert unknown.target == (0.0, 0.0, 0.0) and unknown.pending is None
    root = Relation((0.0, 0.0, 0.0))
    root.endpoint(2, lambda point: point, lambda point: add(point, (.2, 0.0, 0.0)))
    root.carry(2, 1, (0.0, 0.0, 0.0), (.2, 0.0, 0.0))
    for partial in (0.0, .25, .5, 1.0):
        vanilla = partial * .2
        corrected = vanilla + root.draw(partial, 7, (0.0, 0.0, 0.0))[0]
        assert abs(corrected - .2) < 1e-12
    assert root.draw(.5, 8, (.2, 0.0, 0.0)) == (0.0, 0.0, 0.0)
    # A voluntary +.1 move remains linearly interpolated on top of support +.2.
    assert abs(.5 * (.1 + .2) + root.draw(.5, 7, (0.0, 0.0, 0.0))[0] - .25) < 1e-12
    root.endpoint(3, lambda point: point, lambda point: add(point, (.1, 0.0, 0.0)))
    root.carry(3, 2, (0.0, 0.0, 0.0), (.1, 0.0, 0.0))
    assert abs(root.draw(.5, 7, (0.0, 0.0, 0.0))[0] - .15) < 1e-12
    initial = lambda point: point
    middle = lambda point: add(point, (.1, 0.0, 0.0))
    last = lambda point: add(point, (.2, 0.0, 0.0))
    burst = Relation((0.0, 0.0, 0.0))
    burst.endpoint(2, initial, middle)
    burst.endpoint(3, middle, last)
    burst.carry(3, 1, (0.0, 0.0, 0.0), (.2, 0.0, 0.0))
    assert abs(burst.draw(.5, 7, (0.0, 0.0, 0.0))[0] - .1) < 1e-12
    broken = Relation((0.0, 0.0, 0.0))
    broken.endpoint(2, initial, middle)
    broken.endpoint(3, initial, last)
    assert broken.pending is None and broken.draw(.5, 7, (0.0, 0.0, 0.0)) == (0.0, 0.0, 0.0)
    print("S31 IMPLEMENTER PLAN MODEL PASS: order, fractions, tick, multiple carries and endpoint bursts")
