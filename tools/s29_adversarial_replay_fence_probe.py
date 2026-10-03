"""Independent bounded/locality oracle for production TrackingReplayFence.

Only Minecraft identifier and payload value objects are stubbed; the fence source
is copied unchanged except an observational loop counter into disposable scratch.
"""
from pathlib import Path
import subprocess
import tempfile

repo = Path(__file__).resolve().parents[1]
source = (repo / 'src/main/java/io/github/r3neer/scalebrews/collision/internal/TrackingReplayFence.java').read_text(encoding='utf-8')
source = source.replace('public final class TrackingReplayFence {', 'public final class TrackingReplayFence { public static long visited;')
anchor = 'for(var entry:scopes.values())result=Math.max(result,entry.trackingGeneration());'
assert anchor in source
source = source.replace(anchor, 'for(var entry:scopes.values()){visited++;result=Math.max(result,entry.trackingGeneration());}')
payload = '''package io.github.r3neer.scalebrews.collision.internal;
import java.util.UUID;import net.minecraft.resources.Identifier;
public record AnatomyPosePayload(UUID epoch,long revision,Identifier dimension,int entityId,UUID entity,
 Identifier model,Identifier provider,Identifier rootProvider,long bindingGeneration,long trackingGeneration,
 long frameSerial,long authorityTick,long jointSampleTick) {}
'''
probe = '''package io.github.r3neer.scalebrews.collision.internal;
import java.util.UUID;import net.minecraft.resources.Identifier;
public class FenceProbe {
 static final UUID E=new UUID(1,2), TARGET=new UUID(3,4);
 static AnatomyPosePayload p(UUID u,int d,long b,long t,long serial) {
   var i=new Identifier("model");return new AnatomyPosePayload(E,1,new Identifier("dim"+d),8,u,i,i,i,b,t,serial,serial,serial);
 }
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] ignored){
   var f=new TrackingReplayFence();
   check(f.retire(p(TARGET,0,1,3,10)),"initial retire");
   check(f.retire(p(TARGET,1,1,9,20)),"second dimension");
   check(f.retire(p(TARGET,2,1,5,30)),"third dimension");
   for(int n=0;n<4093;n++)check(f.retire(p(new UUID(9,n),0,1,100,1)),"fill "+n);
   check(f.entries()==4096,"exact global cap");
   TrackingReplayFence.visited=0;
   check(f.retiredGeneration(TARGET)==9,"max only target dimensions");
   check(TrackingReplayFence.visited==3,"visited unrelated supports");
   check(f.retiredGeneration(new UUID(7,7))==0,"unknown target");
   check(TrackingReplayFence.visited==3,"unknown target scanned");
   check(f.rejects(p(TARGET,0,1,3,10)),"exact replay");
   check(f.rejects(p(TARGET,0,1,2,100)),"tracking rollback");
   check(!f.rejects(p(TARGET,0,1,3,11)),"same-window forward serial");
   f.accepted(p(TARGET,1,1,10,21));
   check(f.entries()==4095 && f.retiredGeneration(TARGET)==5,"remove dimension only");
   check(f.retire(p(TARGET,1,1,7,21)),"budget reuse");
   check(f.entries()==4096 && f.retiredGeneration(TARGET)==7,"dimension restore");
   check(!f.retire(p(new UUID(8,8),0,1,1,1)),"overflow accepted");
   check(f.saturated() && f.rejects(p(new UUID(8,8),0,1,100,100)),"saturation not fail closed");
   f.accepted(p(TARGET,0,1,4,11));check(f.entries()==4096,"saturation released budget");
   f.clear();check(f.entries()==0 && !f.saturated() && f.retiredGeneration(TARGET)==0,"clear");
   check(f.retire(p(TARGET,0,1,1,1)),"reuse after clear");
   f.accepted(p(TARGET,0,1,2,2));check(f.entries()==0 && f.retiredGeneration(TARGET)==0,"last scope retained");
   check(f.retire(p(TARGET,0,1,4,10)),"quiet history retire");
   check(!f.rejects(p(TARGET,0,1,4,11)),"quiet history cannot progress before barrier");
   f.closeWindows();
   check(f.rejects(p(TARGET,0,1,4,11)),"closed quiet window progressed");
   check(f.rejects(p(TARGET,0,2,4,12)),"binding change reopened closed tracking window");
   check(!f.rejects(p(TARGET,0,1,5,11)),"fresh tracking window rejected");
   check(f.retire(p(TARGET,0,1,4,12)),"closed window refresh");
   check(f.rejects(p(TARGET,0,1,4,13)),"retire erased closed-window barrier");
   check(f.retireWindow(p(TARGET,1,1,6,20)),"explicit dimension close");
   check(f.rejects(p(TARGET,1,1,6,21)),"explicit window progressed");
   f.accepted(p(TARGET,0,1,5,11));
   check(f.entries()==1 && f.rejects(p(TARGET,1,1,6,21)),"accept erased another dimension barrier");
   f.clear();check(!f.rejects(p(TARGET,1,1,6,21)),"catalog reset retained closed barrier");
   System.out.println("PASS replay fence: local 3/4096 visits, dimension removal, budget reuse, replay, saturation and closed-window barriers");
 }
}
'''
java = Path.home() / '.jdks/ms-25.0.3/bin'
with tempfile.TemporaryDirectory(prefix='s29-fence-') as temp:
    root = Path(temp)
    pkg = root / 'io/github/r3neer/scalebrews/collision/internal'
    pkg.mkdir(parents=True)
    for name, text in [('TrackingReplayFence.java', source), ('AnatomyPosePayload.java', payload), ('FenceProbe.java', probe)]:
        (pkg / name).write_text(text, encoding='utf-8')
    ids = root / 'net/minecraft/resources'; ids.mkdir(parents=True)
    (ids / 'Identifier.java').write_text('package net.minecraft.resources; public record Identifier(String value) {}', encoding='utf-8')
    subprocess.run([str(java / 'javac.exe'), '-d', str(root), *map(str, root.rglob('*.java'))], check=True)
    subprocess.run([str(java / 'java.exe'), '-cp', str(root), 'io.github.r3neer.scalebrews.collision.internal.FenceProbe'], check=True)
