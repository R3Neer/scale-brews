package io.github.r3neer.scalebrews.test;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.collision.geometry.*;
import io.github.r3neer.scalebrews.platform.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
public final class S42AdversarialRetiredDisconnectProof implements FabricClientGameTest {
 public static final AtomicBoolean hold=new AtomicBoolean();
 public static volatile Object expectedHandler;
 public static volatile CountDownLatch captured=new CountDownLatch(1),release=new CountDownLatch(1),finished=new CountDownLatch(1);
 public static void arm(){expectedHandler=null;hold.set(false);captured=new CountDownLatch(1);release=new CountDownLatch(1);finished=new CountDownLatch(1);}
 public void runTest(ClientGameTestContext c) {
  arm();
  var model=c.computeOnClient(mc->io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor.vanilla("minecraft:cow","26.2",net.minecraft.client.model.animal.cow.CowModel.createBodyLayer().bakeRoot(),Set.of()));
  var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),Optional.of(new AnatomyDefinition(Identifier.parse(model.source()),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
  var old=c.worldBuilder().create();
  try {
   old.getServer().runOnServer(s->AnatomyRuntime.startPrepared(s,Map.of(model.source(),model),Map.of("test:s42",profile)));
   c.waitFor(mc->AnatomyClientNetworking.catalog().ready(),160);
   expectedHandler=c.computeOnClient(mc->mc.getConnection());hold.set(true);old.getServer().runOnServer(AnatomyRuntime::stop);
  }finally {old.close();}
  c.waitFor(mc->captured.getCount()==0,160);
  var current=c.worldBuilder().create();
  try {
   current.getServer().runOnServer(s->AnatomyRuntime.startPrepared(s,Map.of(model.source(),model),Map.of("test:s42",profile)));
   c.waitFor(mc->AnatomyClientNetworking.catalog().ready(),160);
   var packets=current.getServer().computeOnServer(s->bundle(s));
   long before=c.computeOnClient(mc->AnatomyClientNetworking.catalog().revision());
   var connection=c.computeOnClient(mc->mc.getConnection());
   current.getServer().runOnServer(s->S29ImplementerCatalogIntervalTests.reloadPrepared(s,Map.of(model.source(),model),Map.of("test:s42",profile)));
   c.waitFor(mc->AnatomyClientNetworking.catalog().ready() && AnatomyClientNetworking.catalog().revision()>before,160);
   long accepted=c.computeOnClient(mc->AnatomyClientNetworking.catalog().revision());
   System.out.println("S42_LIVE_RELOADED_CATALOG accepted="+accepted+" heldOldDisconnect="+(captured.getCount()==0));
   release.countDown();c.waitFor(mc->finished.getCount()==0,160);
   c.runOnClient(mc->{if(mc.getConnection()!=connection)throw new AssertionError("Fixture changed current connection");System.out.println("S42_AFTER_RETIRED_DISCONNECT ready="+AnatomyClientNetworking.catalog().ready()+" revision="+AnatomyClientNetworking.catalog().revision());});
   current.getServer().runOnServer(s->{for(var p:packets)ServerPlayNetworking.send(s.getPlayerList().getPlayers().getFirst(),p);});
   c.waitTicks(3);
   c.runOnClient(mc->{long actual=AnatomyClientNetworking.catalog().revision();
    if(actual<accepted)throw new AssertionError("NFR-017: retired connection disconnect rolled live accepted catalog backward after stale replay: accepted="+accepted+" actual="+actual);
   });
  }finally {release.countDown();hold.set(false);current.getServer().runOnServer(AnatomyRuntime::stop);current.close();}
 }
 static java.util.List<AnatomyCatalogPayload> bundle(net.minecraft.server.MinecraftServer s) {try {
  var f=AnatomyRuntime.class.getDeclaredField("STATES");f.setAccessible(true);var state=((Map<?,?>)f.get(null)).get(s);
  var cf=state.getClass().getDeclaredField("catalog");cf.setAccessible(true);var catalog=(WorldAnatomyCatalog)cf.get(state);
  var m=WorldAnatomyCatalog.class.getDeclaredMethod("preparedPackets",UUID.class);m.setAccessible(true);
  @SuppressWarnings("unchecked") var packets=(java.util.List<AnatomyCatalogPayload>)m.invoke(catalog,AnatomyNetworking.epoch(s));return packets;
 }catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
