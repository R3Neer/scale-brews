#!/usr/bin/env python3
"""One directed mutation plus an exact behavioral oracle. Never changes Git refs."""
import hashlib
import json
import os
import pathlib
import subprocess
import sys
import xml.etree.ElementTree as ET

P = 'src/main/java/io/github/r3neer/scalebrews/collision/'
# A tuple is (file, exact original fragment, replacement, required failing test).
CASES = {
    'dispatcher-reentry': ('internal/MaterialEventDispatcher.java',
        'if(draining || notifyingQuarantine)return quarantine(event,Reason.GATE_VIOLATION,backend);',
        'if(draining)return quarantine(event,Reason.GATE_VIOLATION,backend);',
        's00dispatcher_tests_callback_single_reentry_outside_drain'),
    'dispatcher-abort-clear': ('internal/MaterialEventDispatcher.java',
        '} finally {queue.clear();draining=false;admittedEvents=0;}',
        '} finally {draining=false;admittedEvents=0;}',
        's00dispatcher_tests_abort_fatal_failure_does_not_leak_queue'),
    'sat-axis-cutoff': ('geometry/ConvexBox.java', 'if(largest>0) {', 'if(largest>1e-10) {',
        's00temporal_tests_thin_affine_face_plane_cannot_disappear_from_sat'),
    'sat-subnormal-reciprocal': ('geometry/ConvexBox.java',
        'new Vec3(axis.x/largest,axis.y/largest,axis.z/largest)', 'axis.scale(1/largest)',
        's00temporal_tests_accepted_subnormal_edge_components_never_produce_nonfinite_escape'),
    'bodypath-certificate': ('physics/BodyPath.java', ' || support+1e-6<bounds.minDotAntiGravity()', '',
        's00temporal_tests_body_path_rejects_an_impossible_normal_certificate'),
    'bodypath-interior-certificate': ('physics/BodyPath.java', ' || denominator+1e-6<minDot', '',
        's00temporal_tests_normal_certificate_remains_binding_inside_the_interval'),
    'plane-precopy-cap': ('physics/ConservativeSweep.java', ' || invariantPlanes.size()>6', '',
        's00temporal_tests_plane_cardinality_is_rejected_before_copying_provider_entries'),
    'tiny-rotation-bound': ('internal/HierarchyMotion.java',
        'derivative=rotationSpeed(a.rotation,b.rotation)*norm+maxAbs(new Vector3f(b.scale).sub(a.scale));',
        'derivative=2*Math.acos(Math.clamp(Math.abs(a.rotation.dot(b.rotation)),0,1))*norm+maxAbs(new Vector3f(b.scale).sub(a.scale));',
        's00geometry_tests_tiny_rotation_cannot_have_azero_motion_certificate'),
    'receipt-current-tick': ('internal/AnatomyTransportReceipts.java',
        'if(transport.tick()!=tick || transport.rootFrameSequence()!=root.sequence() || root.tick()>tick || surface.tick()>tick)return;',
        'if(transport.rootFrameSequence()!=root.sequence() || root.tick()>tick || surface.tick()>tick)return;',
        's00receipt_tests_receipt_recorder_rejects_aresult_from_another_tick'),
    'receipt-surface': ('internal/AnatomyTransportReceipts.java',
        '            new SurfaceContact(support,catalogRevision,piece,face,localPoint,normal,surfaceTick);\n', '',
        's00receipt_tests_receipt_cannot_claim_invalid_surface'),
    'frame-binding-generation': ('internal/AnatomyFrameHistory.java',
        ' || current.bindingGeneration()!=next.bindingGeneration()', '',
        's00causal_tests_frame_history_rejects_each_identity_axis_atomically'),
    'model-unknown-joint': ('geometry/ModelGeometry.java',
        '        if(!ids.containsAll(replacements.keySet()))throw new IllegalArgumentException("Unknown model joint");\n', '',
        's00model_tests_unknown_joint_keys_cannot_silently_become_rest_pose'),
    'runtime-thread-owner': ('internal/AnatomyMovement.java',
        'if(!level.isClientSide() && server!=null && !server.isSameThread())',
        'if(false && !level.isClientSide() && server!=null && !server.isSameThread())',
        's00runtime_tests_server_registration_rejects_wrong_thread_before_mutation'),
    'endpoint-same-serial': ('internal/AnatomyMovement.java',
        'if(!old.endpoint().equals(endpoint) || !Objects.equals(old.snapshot(),snapshot))return quarantineEndpoint(support);',
        'if(false)return quarantineEndpoint(support);',
        's00runtime_tests_changed_endpoint_cannot_reuse_material_serial'),
    'client-receipt-invalidate': ('internal/AnatomyTransportReceipts.java',
        'if(body==null || body.level().isClientSide())return;', 'if(body==null)return;',
        'java.lang.AssertionError: Client clear deleted authoritative receipts'),
}

def main():
    mode, mutation = sys.argv[1:3]
    if mutation not in CASES:
        raise SystemExit('Unknown mutation: ' + mutation)
    if os.environ.get('GITHUB_REF') != 'refs/heads/chatgpt-editing':
        raise SystemExit('Wrong branch')
    folder = pathlib.Path(os.environ['RUNNER_TEMP']) / 's00-mutation'
    folder.mkdir(exist_ok=True)
    relative, old, new, oracle = CASES[mutation]
    if mode == 'apply':
        path = pathlib.Path(P + relative)
        before = path.read_text(encoding='utf-8')
        if before.count(old) != 1:
            raise SystemExit(f'{mutation}: expected one target, found {before.count(old)}')
        after = before.replace(old, new)
        path.write_text(after, encoding='utf-8')
        source = subprocess.check_output(['git', 'write-tree'], text=True).strip()
        identity = {'mutation': mutation, 'source_tree': source, 'path': str(path),
            'before_sha256': hashlib.sha256(before.encode()).hexdigest(),
            'after_sha256': hashlib.sha256(after.encode()).hexdigest(), 'oracle': oracle}
        (folder/'identity.json').write_text(json.dumps(identity, indent=2))
        print('MUTATION_APPLIED ' + mutation)
    elif mode == 'verify':
        code = int(sys.argv[3])
        if code == 0:
            raise SystemExit('MUTATION_SURVIVED ' + mutation)
        identity = json.loads((folder/'identity.json').read_text())
        if identity['mutation'] != mutation or identity['oracle'] != oracle:
            raise SystemExit('Wrong mutation evidence')
        if mutation.startswith('client-'):
            log = (folder/'run.log').read_text(encoding='utf-8', errors='replace')
            if oracle not in log:
                raise SystemExit('No expected real-client assertion; not a behavioral kill')
            failed = [oracle]
        else:
            report = folder/'server.xml'
            if not report.is_file():
                raise SystemExit('Missing GameTest XML; not a behavioral kill')
            tests = ET.parse(report).findall('.//testcase')
            failed = [t.get('name') for t in tests if t.find('failure') is not None]
            if 'scalebrews-test:' + oracle not in failed:
                raise SystemExit('Expected assertion did not fail; not a behavioral kill: ' + repr(failed))
        (folder/'result.json').write_text(json.dumps({'mutation': mutation, 'status': 'KILLED',
            'exit_code': code, 'expected': oracle, 'observed_failures': failed}, indent=2))
        print('MUTATION_KILLED ' + mutation)
    else:
        raise SystemExit('Expected apply or verify')

if __name__ == '__main__':
    main()