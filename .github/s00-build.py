"""Temporary S00 transport. Builds a complete, predeclared Git tree; never writes refs."""
import base64, hashlib, json, os, pathlib, re, subprocess, urllib.request

assert os.environ['GITHUB_REF'] == 'refs/heads/chatgpt-editing'
spec = json.loads(pathlib.Path('.github/s00-candidate.json').read_text())
patches = [
    ('reviewed.patch', pathlib.Path('.github/s00-candidate.patch').read_bytes(), spec['patch_sha256']),
    ('extra.patch', pathlib.Path('.github/s00-extra.patch').read_bytes(), spec.get('extra_patch_sha256', hashlib.sha256(b'').hexdigest())),
    ('repair.patch', pathlib.Path('.github/s00-repair.patch').read_bytes(), spec.get('repair_patch_sha256', hashlib.sha256(b'').hexdigest())),
    ('tail.patch', pathlib.Path('.github/s00-tail.patch').read_bytes(), spec.get('tail_patch_sha256', hashlib.sha256(b'').hexdigest())),
    ('lifecycle-repair.patch', pathlib.Path('.github/s00-lifecycle-repair.patch').read_bytes(), spec.get('lifecycle_repair_patch_sha256', hashlib.sha256(b'').hexdigest())),
    ('holdout2.patch', pathlib.Path('.github/s00-holdout2.patch').read_bytes(), spec.get('holdout2_patch_sha256', hashlib.sha256(b'').hexdigest())),
]
for _, data, expected in patches:
    assert hashlib.sha256(data).hexdigest() == expected
assert all(re.fullmatch('[0-9a-f]{40}', spec[key]) for key in ('base', 'target'))
assert pathlib.Path('.github/s00-candidate-tree').read_text().strip() == spec['base']

def git(*args, **kwargs):
    return subprocess.check_output(['git', *args], **kwargs)

def get(suffix):
    request = urllib.request.Request(
        'https://api.github.com/repos/' + os.environ['GITHUB_REPOSITORY'] + '/git/' + suffix,
        headers={'Authorization': 'Bearer ' + os.environ['GH_TOKEN'], 'Accept': 'application/vnd.github+json'})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)

base = spec['base']
if subprocess.run(['git', 'cat-file', '-e', base + '^{tree}'], capture_output=True).returncode:
    tree = get('trees/' + base + '?recursive=1')
    assert not tree.get('truncated') and tree['sha'] == base
    entries = []
    for entry in tree['tree']:
        if entry['type'] == 'tree':
            continue
        path = pathlib.PurePosixPath(entry['path'])
        assert not path.is_absolute() and not any(part in ('..', '.git') for part in path.parts)
        assert '\n' not in entry['path'] and '\t' not in entry['path']
        assert entry['type'] == 'blob' and entry['mode'] in ('100644', '100755')
        sha = entry['sha']
        assert re.fullmatch('[0-9a-f]{40}', sha)
        if subprocess.run(['git', 'cat-file', '-e', sha], capture_output=True).returncode:
            blob = get('blobs/' + sha)
            assert blob['encoding'] == 'base64'
            data = base64.b64decode(blob['content'])
            assert hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest() == sha
            assert git('hash-object', '-w', '--stdin', input=data).decode().strip() == sha
        entries.append(f"{entry['mode']} blob {sha}\t{entry['path']}\n")
    env = dict(os.environ, GIT_INDEX_FILE=os.path.join(os.environ['RUNNER_TEMP'], 's00-candidate.index'))
    git('read-tree', '--empty', env=env)
    git('update-index', '--index-info', input=''.join(entries).encode(), env=env)
    assert git('write-tree', env=env).decode().strip() == base

git('read-tree', '--reset', '-u', base)
assert git('write-tree').decode().strip() == base
evidence = pathlib.Path(os.environ['RUNNER_TEMP']) / 's00-evidence'
evidence.mkdir()
for name, data, _ in patches:
    (evidence / name).write_bytes(data)
    if data:
        staged = pathlib.Path(os.environ['RUNNER_TEMP']) / ('s00-' + name)
        staged.write_bytes(data)
        git('apply', '--check', '--index', str(staged))
        git('apply', '--index', str(staged))
actual = git('write-tree').decode().strip()
assert actual == spec['target'], (actual, spec['target'])
changed = set(git('diff', '--cached', '--name-only', base).decode().splitlines())
assert changed == set(spec['paths']), (changed, spec['paths'])
(evidence / 'identity.txt').write_text(
    'source_tree=' + actual + '\ntransport_commit=' + os.environ['GITHUB_SHA']
    + '\nbase_tree=' + base + '\npatch_sha256=' + spec['patch_sha256']
    + '\nextra_patch_sha256=' + spec.get('extra_patch_sha256','')
    + '\nrepair_patch_sha256=' + spec.get('repair_patch_sha256','')
    + '\ntail_patch_sha256=' + spec.get('tail_patch_sha256','')
    + '\nlifecycle_repair_patch_sha256=' + spec.get('lifecycle_repair_patch_sha256','')
    + '\nholdout2_patch_sha256=' + spec.get('holdout2_patch_sha256','') + '\n')
(evidence / 'transport-spec.json').write_text(json.dumps(spec, indent=2))
git('archive', '--format=tar', '--output=' + str(evidence / 'source.tar'), actual)
print('S00_SOURCE_TREE=' + actual)
print('S00_TRANSPORT_COMMIT=' + os.environ['GITHUB_SHA'])