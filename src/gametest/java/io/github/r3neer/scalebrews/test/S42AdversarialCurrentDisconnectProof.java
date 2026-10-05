package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.collision.geometry.*;
import io.github.r3neer.scalebrews.platform.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Positive control: the actual current Netty disconnect still retires its session. */
public final class S42AdversarialCurrentDisconnectProof implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c) {
  var model=c.computeOnClient(mc->io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor.vanilla("minecraft:cow","26.2",net.minecraft.client.model.animal.cow.CowModel.createBodyLayer().bakeRoot(),Set.of()));
  var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),Optional.of(new AnatomyDefinition(Identifier.parse(model.source()),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
  S42AdversarialRetiredDisconnectProof.arm();
  var world=c.worldBuilder().create();
  try {
   world.getServer().runOnServer(s->AnatomyRuntime.startPrepared(s,Map.of(model.source(),model),Map.of("test:s42",profile)));
   c.waitFor(mc->AnatomyClientNetworking.catalog().ready(),160);
   c.runOnClient(mc->{if(owner()!=mc.getConnection())throw new AssertionError("Joined session owner differs from live handler");});
   S42AdversarialRetiredDisconnectProof.expectedHandler=c.computeOnClient(mc->mc.getConnection());
   S42AdversarialRetiredDisconnectProof.hold.set(true);
  }finally {world.close();}
  try {
   c.waitFor(mc->S42AdversarialRetiredDisconnectProof.captured.getCount()==0,160);
   S42AdversarialRetiredDisconnectProof.release.countDown();
   c.waitFor(mc->S42AdversarialRetiredDisconnectProof.finished.getCount()==0,160);
   c.waitFor(mc->owner()==null && !AnatomyClientNetworking.catalog().ready() && AnatomyClientNetworking.catalog().revision()==-1,160);
   System.out.println("S42_CURRENT_REAL_DISCONNECT_PASS");
  }finally {S42AdversarialRetiredDisconnectProof.release.countDown();S42AdversarialRetiredDisconnectProof.hold.set(false);}
 }
 static Object owner() {try {var f=AnatomyClientNetworking.class.getDeclaredField("connectionOwner");f.setAccessible(true);return f.get(null);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
