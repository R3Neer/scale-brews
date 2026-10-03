"""Compile the unchanged production inbox with an identity-only payload fixture.

The fixture removes Fabric wire dependencies only. All lifecycle/TTL/selection logic
under test is the real production AnatomyCertifiedIntervalInbox.java.
Run: python tools/s29_adversarial_inbox_probe.py
"""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
INBOX = ROOT / "src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyCertifiedIntervalInbox.java"
PACKAGE = "io.github.r3neer.scalebrews.collision.internal"
PAYLOAD = '''package io.github.r3neer.scalebrews.collision.internal;
import java.util.UUID;
public record AnatomyMaterialIntervalPayload(UUID epoch,long revision,String dimension,
 int supportId,UUID support,long bindingGeneration,long trackingGeneration,long materialSerial,
 long beforeFrameSerial,long afterFrameSerial,long beforeAuthorityTick,long afterAuthorityTick,
 long beforeJointSampleTick,long afterJointSampleTick) {}
'''
PROBE = '''package io.github.r3neer.scalebrews.collision.internal;
import java.util.UUID;
public final class S29AdversarialInboxProbe {
 static final UUID EPOCH=new UUID(0,1), SUPPORT=new UUID(0,2);
 static AnatomyMaterialIntervalPayload p(long serial,long binding,long tracking) {
  return new AnatomyMaterialIntervalPayload(EPOCH,1,"minecraft:overworld",3,SUPPORT,
   binding,tracking,serial,2*serial,2*serial+1,10,11,10,11);
 }
 public static void main(String[] args) {
  var inbox=new AnatomyCertifiedIntervalInbox();var certificate=p(1,1,1);
  if(!inbox.accept(certificate,0)||inbox.exact(SUPPORT,1,0).isEmpty())
   throw new AssertionError("fixture did not establish a valid certificate");
  if(inbox.exact(SUPPORT,1,AnatomyCertifiedIntervalInbox.TTL_CLIENT_TICKS+1).isPresent())
   throw new AssertionError("certificate did not expire at the documented TTL");
  boolean accepted=inbox.accept(certificate,AnatomyCertifiedIntervalInbox.TTL_CLIENT_TICKS+1);
  boolean revived=inbox.exact(SUPPORT,1,AnatomyCertifiedIntervalInbox.TTL_CLIENT_TICKS+1).isPresent();
  System.out.println("S29 expired-replay: accepted="+accepted+", authoritative="+revived);
  if(revived)throw new AssertionError("NFR-011: expired exact material certificate became authoritative again after replay");
  System.out.println("S29 expired-replay PASS");
 }
}
'''

with tempfile.TemporaryDirectory(prefix="s29-adversary-") as temporary:
    scratch = Path(temporary)
    payload = scratch / "AnatomyMaterialIntervalPayload.java"
    probe = scratch / "S29AdversarialInboxProbe.java"
    payload.write_text(PAYLOAD, encoding="utf-8")
    probe.write_text(PROBE, encoding="utf-8")
    subprocess.run(["javac", "-d", str(scratch), str(payload), str(INBOX), str(probe)], check=True)
    subprocess.run(["java", "-cp", str(scratch), f"{PACKAGE}.S29AdversarialInboxProbe"], check=True)
