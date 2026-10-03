"""Executed pre-implementation counterexamples, not a test of an unimplemented S30 candidate."""
import json
import math

failures=[]
# Existing PlatformCamera: same entity/world, short teleport does not trigger >16 squared-distance reset.
old_camera=.2
physical_jump=1.
dt=.05
owner_after_barrier=0.
smoothed=0. if physical_jump**2>16 else old_camera
camera_after_barrier=smoothed+(owner_after_barrier-smoothed)*(1-math.exp(-dt/.1))
if camera_after_barrier != 0:
    failures.append({'classification':'PLAN FR-088 / I3-I4 reset propagation',
                     'scenario':'one-block same-level teleport, owner immediately zero, unobstructed camera',
                     'oldCameraResidual':old_camera,'newOwnerResidual':owner_after_barrier,
                     'cameraResidualAfterUpdate':camera_after_barrier,
                     'requiredCameraResidual':0.})
# I2's direct calibrated material-point versus physical-body comparison, absent an explicit walking discriminator.
anchor_at_acceptance=0.
body_at_acceptance=0.
calibration=body_at_acceptance-anchor_at_acceptance
unchanged_support_anchor=0.
body_after_voluntary_walk=.2
naive_residual=unchanged_support_anchor+calibration-body_after_voluntary_walk
if naive_residual != 0:
    failures.append({'classification':'PLAN FR-086 / I2 voluntary motion and delayed contact',
                     'scenario':'static support, .2-block voluntary local walk, server local-point update delayed',
                     'naiveResidual':naive_residual,'requiredSupportResidual':0.,
                     'renderedBodyPosition':body_after_voluntary_walk+naive_residual,
                     'requiredRenderedBodyPosition':body_after_voluntary_walk})
print(json.dumps({'kind':'pre-implementation plan counterexamples','failures':failures},indent=2))
raise SystemExit(1 if failures else 0)
