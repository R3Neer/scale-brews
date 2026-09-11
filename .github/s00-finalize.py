#!/usr/bin/env python3
"""Temporary S00 finalizer: verify exact tree, create one logical closeout commit, fast-forward only."""
import base64, hashlib, os, pathlib, subprocess, zlib
REPO_REF='refs/heads/chatgpt-editing'
CANDIDATE_TREE='68244f2acbebccfd0dec607220726b9ed7e56e8a'
FINAL_TREE='4e2a6189482d48876e7b09158c22043ba65751b5'
PATCH_SHA256='f9df7053605eb96067ed200e214f337b9ba1f838609b0c5d024047936a2a535d'
PARTS=4
assert os.environ.get('GITHUB_REF')==REPO_REF
transport=os.environ['GITHUB_SHA']
encoded=''.join(pathlib.Path(f'.github/s00-closeout-part-{i:02d}').read_text().strip() for i in range(PARTS))

def run(*args,capture=False):
    if capture:return subprocess.check_output(args,text=True).strip()
    subprocess.check_call(args)

run('python3','.github/s00-build.py')
assert run('git','write-tree',capture=True)==CANDIDATE_TREE
patch=zlib.decompress(base64.b64decode(encoded))
assert hashlib.sha256(patch).hexdigest()==PATCH_SHA256
p=pathlib.Path(os.environ['RUNNER_TEMP'])/'s00-closeout.patch';p.write_bytes(patch)
run('git','apply','--check','--index',str(p));run('git','apply','--index',str(p));run('git','diff','--cached','--check')
actual=run('git','write-tree',capture=True);assert actual==FINAL_TREE,(actual,FINAL_TREE)
remote=run('git','ls-remote','origin',REPO_REF,capture=True).split()[0];assert remote==transport,(remote,transport)
run('git','config','user.name','Samuel');run('git','config','user.email','64842010+R3Neer@users.noreply.github.com')
message='audit: close S00 foundation audit\n\nCloses the pre-G1 clean-room foundation audit on the exact reviewed tree. Includes blocking foundation repairs, adversarial/holdout tests, 25/25 directed mutation kills, 242/242 server GameTests, real client/integrated/dedicated evidence, component classifications and the non-normative S00 execution log. Removes all temporary S00 transport scaffolding. Does not start G1/S01 and does not modify main.'
commit=subprocess.check_output(['git','commit-tree',actual,'-p',transport],input=message+'\n',text=True).strip()
assert run('git','cat-file','-p',commit,capture=True).splitlines()[0]=='tree '+FINAL_TREE
run('git','push','origin',f'{commit}:refs/heads/chatgpt-editing')
print('S00_FINAL_TREE='+FINAL_TREE);print('S00_FINAL_COMMIT='+commit)
