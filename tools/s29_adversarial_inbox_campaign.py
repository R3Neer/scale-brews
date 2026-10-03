"""S29 identity, replay, bounds and locality campaign over the production inbox.

Only the payload's wire dependencies are replaced. A test-only work counter is
injected into a scratch copy of production source to observe prune locality.
Mutation mode changes only that scratch copy; it must be killed by the oracle.
"""
from pathlib import Path
import argparse
import subprocess
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument('--mutation', choices=['expiry', 'bound', 'global-query', 'binding', 'tracking', 'epoch', 'dimension', 'nearest'])
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
source = (root / 'src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyCertifiedIntervalInbox.java').read_text(encoding='utf-8')
source = source.replace('public final class AnatomyCertifiedIntervalInbox {', 'public final class AnatomyCertifiedIntervalInbox { public static long testPrunedStates;')
source = source.replace('private static void prune(State state,long clientTick) {', 'private static void prune(State state,long clientTick) { testPrunedStates++;')
if args.mutation == 'expiry':
    needle = 'if(payload.materialSerial()<=state.expiredThrough)return false;'
    assert needle in source
    source = source.replace(needle, '')
elif args.mutation == 'bound':
    needle = 'if(state.entries.size()>=MAX_PER_SUPPORT)'
    assert needle in source
    source = source.replace(needle, 'if(false)')
elif args.mutation == 'global-query':
    needle = 'public Optional<AnatomyMaterialIntervalPayload> exact(UUID support,long materialSerial,long clientTick) {'
    assert needle in source
    source = source.replace(needle, needle + ' prune(clientTick);')
elif args.mutation in ('binding', 'tracking'):
    field=args.mutation+'Generation'
    needle=f'next.{field}()<current.{field}()'
    assert needle in source
    source=source.replace(needle,'false')
elif args.mutation == 'epoch':
    source=source.replace('!current.epoch().equals(next.epoch())','false')
    source=source.replace('a.epoch().equals(b.epoch())','true')
elif args.mutation == 'dimension':
    source=source.replace('!current.dimension().equals(next.dimension())','false')
    source=source.replace('a.dimension().equals(b.dimension())','true')
elif args.mutation == 'nearest':
    needle='if(entry.payload().materialSerial()==materialSerial)'
    assert needle in source
    source=source.replace(needle,'if(true)')

payload = '''package io.github.r3neer.scalebrews.collision.internal;
import java.util.UUID;
public record AnatomyMaterialIntervalPayload(UUID epoch,long revision,String dimension,
 int supportId,UUID support,long bindingGeneration,long trackingGeneration,long materialSerial,
 long beforeFrameSerial,long afterFrameSerial,long beforeAuthorityTick,long afterAuthorityTick,
 long beforeJointSampleTick,long afterJointSampleTick) {}
'''
probe = '''package io.github.r3neer.scalebrews.collision.internal;
import java.util.UUID;
public final class S29InboxCampaign {
 static AnatomyMaterialIntervalPayload p(long serial,long binding,long tracking,int id,UUID support) {
  return new AnatomyMaterialIntervalPayload(new UUID(0,1),1,"minecraft:overworld",id,support,binding,tracking,serial,2*serial,2*serial+1,10,11,10,11);
 }
 static void expect(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] ignored) {
  UUID support=new UUID(0,2);var p=p(1,1,1,3,support);var inbox=new AnatomyCertifiedIntervalInbox();
  expect(inbox.accept(p,0),"valid token");expect(inbox.accept(p,99),"idempotent duplicate");
  expect(inbox.exact(support,1,100).isPresent(),"TTL inclusive boundary");
  expect(inbox.exact(support,1,101).isEmpty(),"duplicate must not refresh TTL");
  expect(!inbox.accept(p,101),"expired replay must fail");
  expect(inbox.exact(support,1,101).isEmpty(),"no resurrection");System.out.println("PASS expiry/duplicate boundary");

  inbox=new AnatomyCertifiedIntervalInbox();expect(inbox.accept(p(1,2,4,3,support),0),"initial lifecycle");
  expect(!inbox.accept(p(2,1,5,3,support),0),"binding rollback");
  expect(!inbox.accept(p(2,3,3,3,support),0),"tracking rollback");
  expect(!inbox.accept(p(2,2,5,9,support),0),"network ID replacement under pure retrack");
  expect(inbox.accept(p(2,2,5,3,support),0),"legitimate retrack");
  expect(inbox.exact(support,1,0).isEmpty(),"retrack removes old material");
  expect(!inbox.accept(p(1,2,4,3,support),0),"old tracking replay");
  expect(inbox.exact(support,999,0).isEmpty(),"exact lookup must not choose nearest");
  expect(inbox.accept(p(3,3,5,9,support),0),"binding advance permits network replacement");System.out.println("PASS lifecycle/exact identity");
  var canonical=p(4,3,5,9,support);
  var wrongEpoch=new AnatomyMaterialIntervalPayload(new UUID(90,91),1,"minecraft:overworld",9,support,3,5,4,8,9,10,11,10,11);
  var wrongDimension=new AnatomyMaterialIntervalPayload(new UUID(0,1),1,"minecraft:the_nether",9,support,3,5,4,8,9,10,11,10,11);
  expect(!inbox.accept(wrongEpoch,0),"epoch mismatch");expect(!inbox.accept(wrongDimension,0),"dimension mismatch");
  expect(inbox.accept(canonical,0),"canonical identity after rejected alternatives");System.out.println("PASS epoch/dimension barriers");

  inbox=new AnatomyCertifiedIntervalInbox();
  for(int i=1;i<=AnatomyCertifiedIntervalInbox.MAX_PER_SUPPORT;i++)expect(inbox.accept(p(i,1,1,3,support),0),"capacity inclusive");
  expect(!inbox.accept(p(129,1,1,3,support),0),"overflow must reject");
  expect(inbox.exact(support,1,0).isEmpty(),"overflow must release material");
  expect(inbox.entries(support)==0,"overflow releases bounded entries");
  expect(!inbox.accept(p(130,1,1,3,support),101),"saturation remains fenced");
  expect(inbox.accept(p(131,1,2,3,support),101),"legitimate new tracking recovers");System.out.println("PASS per-support bound/outcome");

  inbox=new AnatomyCertifiedIntervalInbox();
  for(int i=0;i<4096;i++)expect(inbox.accept(p(1,1,1,i,new UUID(0,100+i)),0),"support capacity");
  AnatomyCertifiedIntervalInbox.testPrunedStates=0;
  for(int i=0;i<100;i++)expect(inbox.exact(new UUID(0,100),1,0).isPresent(),"local certificate");
  long work=AnatomyCertifiedIntervalInbox.testPrunedStates;
  expect(work<=100,"exact presentation query traversed unrelated supports: prunedStates="+work);
  expect(!inbox.accept(p(1,1,1,5000,new UUID(0,99999)),0),"support overflow reject");
  expect(inbox.saturated()&&inbox.supports()==0,"support overflow fail-closed releases state");
  System.out.println("PASS locality structural work="+work+" and global bound");
 }
}
'''
with tempfile.TemporaryDirectory(prefix='s29-inbox-campaign-') as directory:
    scratch=Path(directory)
    files=[]
    for name,content in [('AnatomyCertifiedIntervalInbox.java',source),('AnatomyMaterialIntervalPayload.java',payload),('S29InboxCampaign.java',probe)]:
        path=scratch/name
        path.write_text(content,encoding='utf-8')
        files.append(str(path))
    subprocess.run(['javac','-d',str(scratch),*files],check=True)
    result=subprocess.run(['java','-cp',str(scratch),'io.github.r3neer.scalebrews.collision.internal.S29InboxCampaign'])
    if args.mutation:
        if result.returncode==0:raise SystemExit('SURVIVING MUTANT '+args.mutation)
        print('KILLED MUTANT '+args.mutation)
    else:
        raise SystemExit(result.returncode)
