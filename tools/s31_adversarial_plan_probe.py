"""Executed S31 relation-plan counterexample; no unimplemented production is tested."""
import json


def rotate_quarter_turn(point):
    x, y, z = point
    return (-z, y, x)


def subtract(a, b):
    return tuple(x - y for x, y in zip(a, b))


old_confirmed_point = (.25, 0., 0.)
walked_physical_point = (.5, 0., 0.)
support_delta_at_old_point = subtract(rotate_quarter_turn(old_confirmed_point), old_confirmed_point)
applied_passive_delta = subtract(rotate_quarter_turn(walked_physical_point), walked_physical_point)
# All R2 cursor checks match: live body/support, same binding/tracking,
# exact new endpoint serial and a new once-consumed local transport sequence.
# Local afterMove has advanced the physical anchor, while the server-confirmed
# contact used by presentation still names the old point. No position subtraction.
late_contact_residual = subtract(support_delta_at_old_point, applied_passive_delta)
# R2's later rebase expressly preserves that already-proven residual.
early_contact_residual = subtract(applied_passive_delta, applied_passive_delta)
evidence = {
    'kind': 'pre-implementation S31 plan counterexample',
    'classification': 'PLAN R2/R4 / passive cancellation and delayed local-point provenance',
    'geometry': 'horizontal retained face, quarter turn around gravity axis; no shear or gravity change',
    'oldConfirmedLocalPoint': old_confirmed_point,
    'physicalAnchorAfterVoluntaryWalk': walked_physical_point,
    'acceptedEndpointSerial': 2,
    'matchingLocalTransportSequence': 1,
    'supportAnchorDeltaUsingStaleConfirmedPoint': support_delta_at_old_point,
    'exactAppliedPassiveDeltaUsingPhysicalAnchor': applied_passive_delta,
    'earlyContactResidual': early_contact_residual,
    'lateContactResidualAfterPreservingRebase': late_contact_residual,
    'requiredResidualAfterFullyAppliedCarry': (0., 0., 0.),
    'productionEdited': False,
}
print(json.dumps(evidence, indent=2))
raise SystemExit(1 if late_contact_residual != early_contact_residual else 0)
