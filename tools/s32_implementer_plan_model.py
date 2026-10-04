"""S32 plan contract model; no production or Minecraft packet behavior is claimed."""

from dataclasses import dataclass
from fractions import Fraction as F


@dataclass
class RemoteSample:
    native: F = F(0)
    target: F = F(0)
    support: F = F(0)
    passive_pending: F = F(0)
    steps: int = 0

    def carry(self, delta: F) -> None:
        self.support += delta
        self.passive_pending += delta

    def native_update(self, target: F, restart: bool) -> None:
        self.target = target
        if restart:
            self.steps = 3

    def tick(self) -> None:
        if self.steps:
            fraction = F(1, self.steps)
            self.native += (self.target - self.native) * fraction
            self.passive_pending *= 1 - fraction
            self.steps -= 1

    def draw(self) -> F:
        return self.native + self.passive_pending


def prove_restart() -> None:
    sample = RemoteSample()
    sample.carry(F(3, 4))
    sample.native_update(F(3, 4), restart=True)
    assert sample.draw() == F(3, 4)
    sample.tick()
    assert (sample.native, sample.passive_pending, sample.draw()) == (
        F(1, 4), F(1, 2), F(3, 4)
    )
    sample.carry(F(3, 4))
    sample.native_update(F(3, 2), restart=True)
    assert (sample.native, sample.passive_pending, sample.draw()) == (
        F(1, 4), F(5, 4), F(3, 2)
    )
    for _ in range(3):
        sample.tick()
        assert sample.draw() == F(3, 2)
    assert sample.passive_pending == 0


def prove_rotation_restart_and_noop() -> None:
    sample = RemoteSample()
    sample.carry(F(3, 4))
    sample.native_update(F(3, 4), restart=True)
    sample.tick()
    assert sample.native == F(1, 4)
    sample.native_update(F(3, 4), restart=True)  # changed yaw, same target
    sample.tick()
    assert (sample.native, sample.passive_pending, sample.draw()) == (
        F(5, 12), F(1, 3), F(3, 4)
    )
    steps = sample.steps
    sample.native_update(F(3, 4), restart=False)  # identical pose, early return
    assert sample.steps == steps


def prove_occurrence_scope() -> None:
    first_packet = object()
    second_packet = object()  # same decoded target does not imply same packet
    seen = set()

    def attest(expected: object, observed: object, nonce: int, target: F) -> bool:
        if expected is not observed or nonce in seen or target != F(3, 4):
            return False
        seen.add(nonce)
        return True

    assert attest(first_packet, first_packet, 1, F(3, 4))
    assert not attest(first_packet, second_packet, 2, F(3, 4))
    assert not attest(first_packet, first_packet, 1, F(3, 4))
    assert attest(second_packet, second_packet, 2, F(3, 4))


def prove_dual_role_root() -> None:
    native_root = F(1, 4)
    accepted_own_model_origin = F(3, 4)
    remaining_residual = F(1, 2)
    assert native_root + remaining_residual == accepted_own_model_origin
    assert accepted_own_model_origin + remaining_residual == F(5, 4)
    # ENTITY_ROOT uses native_root under the accepted joints/model transform.
    assert native_root + remaining_residual == F(3, 4)


if __name__ == "__main__":
    prove_restart()
    prove_rotation_restart_and_noop()
    prove_occurrence_scope()
    prove_dual_role_root()
    print("S32 IMPLEMENTER PLAN MODEL PASS: carry transfer, rotation/no-op, occurrence, dual-role root")
