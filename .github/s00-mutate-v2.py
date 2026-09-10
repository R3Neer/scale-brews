#!/usr/bin/env python3
"""S00 mutation evidence v2: seals exact mutated Git trees and adds post-repair mutations."""
import hashlib, json, os, pathlib, subprocess, sys, xml.etree.ElementTree as ET

OLD = {
 'dispatcher-reentry','dispatcher-abort-clear','sat-axis-cutoff','sat-subnormal-reciprocal',
 'bodypath-certificate','bodypath-interior-certificate','plane-precopy-cap','tiny-rotation-bound',
 'receipt-current-tick','receipt-surface','frame-binding-generation','model-unknown-joint',
 'runtime-thread-owner','endpoint-same-serial','first-capture-quarantine','registration-lifecycle-watermark',
 'client-receipt-invalidate','client-carry-authority'
}
# path, exact original, replacement, behavioral oracle, client?
NEW = {
 'dispatcher-joint-orphan': (
  'src/main/java/io/github/r3neer/scalebrews/collision/internal/MaterialEventDispatcher.java',
  'if(resolution.derived().stream().anyMatch(carry->!parents.containsKey(carry.parent()))) {',
  'if(false) {',
  's00dispatcher_tests_joint_batch_invalid_parent_must_mark_returned_outcomes', False),
 'catalog-authority-revision': (
  'src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyCatalogTransfer.java',
  'catalog.replaceAtRevision(pendingRevision,models,profiles);acceptedRevision=pendingRevision;chunks=null;',
  'catalog.replace(models,profiles);acceptedRevision=pendingRevision;chunks=null;',
  's00causal_tests_catalog_transfer_snapshot_uses_server_authority_revision', False),
 'model-degenerate-piece': (
  'src/main/java/io/github/r3neer/scalebrews/collision/geometry/ModelGeometry.java',
  '            for(int i=0;i<3;i++)if(!Double.isFinite(p.min.get(i)) || !Double.isFinite(p.max.get(i)) || Math.abs(p.min.get(i))>1024 || Math.abs(p.max.get(i))>1024 || p.min.get(i)>=p.max.get(i))throw new IllegalArgumentException("Invalid bounds");\n            double volume=(p.max.get(0)-p.min.get(0))*(p.max.get(1)-p.min.get(1))*(p.max.get(2)-p.min.get(2));\n            if(!Double.isFinite(volume) || volume<=0)throw new IllegalArgumentException("Degenerate piece volume");',
  '            for(int i=0;i<3;i++)if(!Double.isFinite(p.min.get(i)) || !Double.isFinite(p.max.get(i)) || Math.abs(p.min.get(i))>1024 || Math.abs(p.max.get(i))>1024 || p.min.get(i)>p.max.get(i))throw new IllegalArgumentException("Invalid bounds");',
  's00model_tests_degenerate_pieces_cannot_enter_the_model', False),
 'client-extractor-zero-thickness': (
  'src/client/java/io/github/r3neer/scalebrews/client/collision/preparation/GeometryExtractor.java',
  '        for(int i=0;i<3;i++)if(!(hi[i]>lo[i]))return;',
  '        for(int i=0;i<3;i++)if(false)return;',
  'java.lang.IllegalArgumentException: Invalid bounds', True),
}

def git(*args, **kwargs): return subprocess.check_output(['git',*args], text=True, **kwargs).strip()
def folder():
 p=pathlib.Path(os.environ['RUNNER_TEMP'])/'s00-mutation'; p.mkdir(exist_ok=True); return p

def seal_existing_identity():
 p=folder()/'identity.json'; identity=json.loads(p.read_text())
 candidate=identity['source_tree']; path=pathlib.Path(identity['path'])
 subprocess.check_call(['git','add','--',str(path)])
 mutation=git('write-tree')
 if mutation==candidate: raise SystemExit('Mutation tree did not change')
 identity['candidate_tree']=candidate; identity['source_tree']=mutation; identity['evidence_schema']='s00-mutation-v2'
 p.write_text(json.dumps(identity,indent=2))
 print('MUTATION_TREE='+mutation)

def apply_new(name):
 path_s,old,new,oracle,client=NEW[name]; path=pathlib.Path(path_s); before=path.read_text(encoding='utf-8')
 if before.count(old)!=1: raise SystemExit(f'{name}: expected one target, found {before.count(old)}')
 candidate=git('write-tree'); after=before.replace(old,new); path.write_text(after,encoding='utf-8')
 subprocess.check_call(['git','add','--',str(path)]); mutation=git('write-tree')
 if mutation==candidate: raise SystemExit('Mutation tree did not change')
 identity={'mutation':name,'candidate_tree':candidate,'source_tree':mutation,'path':path_s,
  'before_sha256':hashlib.sha256(before.encode()).hexdigest(),'after_sha256':hashlib.sha256(after.encode()).hexdigest(),
  'oracle':oracle,'client':client,'evidence_schema':'s00-mutation-v2'}
 (folder()/'identity.json').write_text(json.dumps(identity,indent=2)); print('MUTATION_APPLIED '+name+' tree='+mutation)

def verify_new(name,code):
 identity=json.loads((folder()/'identity.json').read_text()); oracle=identity['oracle']
 if identity['mutation']!=name or git('write-tree')!=identity['source_tree']: raise SystemExit('Mutation identity drift')
 if code==0: raise SystemExit('MUTATION_SURVIVED '+name)
 if identity['client']:
  log=(folder()/'run.log').read_text(encoding='utf-8',errors='replace')
  if oracle not in log: raise SystemExit('No expected client oracle; not a behavioral kill')
  failed=[oracle]
 else:
  report=folder()/'server.xml'
  if not report.is_file(): raise SystemExit('Missing GameTest XML; not a behavioral kill')
  failed=[t.get('name') for t in ET.parse(report).findall('.//testcase') if t.find('failure') is not None]
  if 'scalebrews-test:'+oracle not in failed: raise SystemExit('Expected assertion did not fail: '+repr(failed))
 result={'mutation':name,'status':'KILLED','exit_code':code,'expected':oracle,'observed_failures':failed,
  'candidate_tree':identity['candidate_tree'],'mutation_tree':identity['source_tree'],'identity_verified':True}
 (folder()/'result.json').write_text(json.dumps(result,indent=2)); print('MUTATION_KILLED '+name)

def main():
 if os.environ.get('GITHUB_REF')!='refs/heads/chatgpt-editing': raise SystemExit('Wrong branch')
 mode,name=sys.argv[1:3]
 if name in OLD:
  old=os.environ.get('S00_OLD_MUTATOR');
  if not old: raise SystemExit('Missing S00_OLD_MUTATOR')
  if mode=='apply':
   subprocess.check_call([sys.executable,old,'apply',name]); seal_existing_identity()
  elif mode=='verify':
   code=int(sys.argv[3]); subprocess.check_call([sys.executable,old,'verify',name,str(code)])
   identity=json.loads((folder()/'identity.json').read_text());
   if git('write-tree')!=identity['source_tree']: raise SystemExit('Mutation identity drift after test')
   result=json.loads((folder()/'result.json').read_text()); result.update(candidate_tree=identity['candidate_tree'],mutation_tree=identity['source_tree'],identity_verified=True,evidence_schema='s00-mutation-v2')
   (folder()/'result.json').write_text(json.dumps(result,indent=2)); print('MUTATION_IDENTITY_VERIFIED '+name)
  else: raise SystemExit('Expected apply or verify')
 elif name in NEW:
  if mode=='apply': apply_new(name)
  elif mode=='verify': verify_new(name,int(sys.argv[3]))
  else: raise SystemExit('Expected apply or verify')
 else: raise SystemExit('Unknown mutation: '+name)
if __name__=='__main__': main()
