package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.*;
import java.util.concurrent.atomic.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.phys.Vec3;

/** Captured real server contact cannot survive a connection barrier under fresh support geometry. */
public final class S29AdversarialContactLifecycleProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(c->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var cowId=new AtomicInteger();var pigId=new AtomicInteger();var pigUuid=new AtomicReference<UUID>();
        var contact=new AtomicReference<AnatomyContactPayload>();var frame=new AtomicReference<GeometryProvider.PublishedFrame>();
        var generation=new AtomicLong();var freshGeneration=new AtomicLong();var chunk=new AtomicReference<net.minecraft.world.level.ChunkPos>();
        var bundle=new AtomicReference<List<AnatomyCatalogPayload>>();
        try(var server=context.worldBuilder().createServer()) {
            try {
                try(var connection=server.connect()) {
                    context.waitFor(c->c.level!=null && c.player!=null,260);
                    server.runOnServer(s->{
                        var player=s.getPlayerList().getPlayers().getFirst();var cow=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);
                        if(cow==null)throw new AssertionError("Contact fixture cow absent");
                        cow.setNoAi(true);cow.setNoGravity(true);cow.setPersistenceRequired();cow.setPos(player.getX()+2,player.getY(),player.getZ());
                        cow.setYRot(0);cow.yBodyRot=0;chunk.set(cow.chunkPosition());s.overworld().setChunkForced(chunk.get().x(),chunk.get().z(),true);
                        s.overworld().addFreshEntity(cow);cowId.set(cow.getId());
                        AnatomyRuntime.startPrepared(s,Map.of("minecraft:cow",geometry),Map.of("s29_adversary:contact",profile));
                    });
                    context.waitFor(c->c.level.getEntity(cowId.get()) instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow).isPresent()
                        && AnatomyMovement.queryFrame(cow).isPresent(),260);
                    server.runOnServer(s->{var cow=(Cow)s.overworld().getEntity(cowId.get());
                        var box=AnatomyMovement.queryFrame(cow).orElseThrow().snapshot().pieces().get("root/body/cube_0").bounds();
                        var pig=EntityTypes.PIG.create(s.overworld(),EntitySpawnReason.COMMAND);if(pig==null)throw new AssertionError("Pig absent");
                        pig.setNoAi(true);pig.setNoGravity(true);pig.setPersistenceRequired();
                        pig.getAttribute(Attributes.SCALE).addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            Identifier.parse("s29_adversary:body_scale"),-.8,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));pig.refreshDimensions();
                        pig.setPos(box.getCenter().x,box.maxY+.1,box.getCenter().z);s.overworld().addFreshEntity(pig);
                        pigId.set(pig.getId());pigUuid.set(pig.getUUID());pig.move(MoverType.SELF,new Vec3(0,-.2,0));
                        if(!AnatomyMovement.supported(pig))throw new AssertionError("Fixture did not acquire real material contact");
                    });
                    context.waitFor(c->{var pig=c.level.getEntity(pigId.get());return pig instanceof Pig && AnatomyClientNetworking.presentationContact(pig,c.level.getGameTime()).isPresent()
                        && AnatomyMovement.supported(pig);},260);
                    context.runOnClient(c->{
                        var pig=(Pig)c.level.getEntity(pigId.get());
                        @SuppressWarnings("unchecked") var accepted=(Map<UUID,AnatomyContactPayload>)field("presentationContacts");
                        contact.set(accepted.get(pigUuid.get()));assertPassive(pig,"initial confirmed contact");
                        snapshot(c,(Cow)c.level.getEntity(cowId.get()),pig,contact.get(),"baseline");
                    });
                    server.runOnServer(s->{var cow=(Cow)s.overworld().getEntity(cowId.get());
                        frame.set(AnatomyMovement.publishedFrame(cow).orElseThrow());generation.set(AnatomyRuntime.trackingGeneration(s.getPlayerList().getPlayers().getFirst(),cow));
                        bundle.set(bundle(s));s.tickRateManager().setFrozen(true);
                    });
                    if(contact.get()==null || !contact.get().present())throw new AssertionError("Captured contact absent");
                    System.out.println("S29 CONTACT BASELINE actual packet binding="+contact.get().supportBindingGeneration()+" bodyGeneration="+contact.get().trackingGeneration());
                }
                context.waitFor(c->c.level==null,260);
                server.runOnServer(s->{
                    // No fresh contact/clear packet may supersede the old one before the replay oracle.
                    AnatomyRuntime.stop(s);var pig=(Pig)s.overworld().getEntity(pigId.get());var cow=(Cow)s.overworld().getEntity(cowId.get());
                    AnatomyMovement.clear(pig);pig.setPos(cow.getX(),cow.getY()+.5,cow.getZ()+2);
                    if(AnatomyMovement.supported(pig))throw new AssertionError("Server contact survived explicit clear");
                    freshGeneration.set(AnatomyRuntime.nextTrackingGeneration(generation.get()));
                });
                try(var connection=server.connect()) {
                    context.waitFor(c->c.level!=null && c.level.getEntity(cowId.get()) instanceof Cow && c.level.getEntity(pigId.get()) instanceof Pig,260);
                    server.runOnServer(s->{var player=s.getPlayerList().getPlayers().getFirst();
                        if(s.overworld().getGameTime()-contact.get().tick()>100)throw new AssertionError("Inconclusive contact aged beyond TTL");
                        AnatomyNetworking.sendCatalog(player,bundle.get());AnatomyNetworking.sendPose(player,frame.get(),freshGeneration.get());
                    });
                    context.waitFor(c->c.level.getEntity(cowId.get()) instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow).isPresent()
                        && AnatomyMovement.queryFrame(cow).isPresent(),260);
                    context.runOnClient(c->{var pig=c.level.getEntity(pigId.get());
                        // The authority clock is frozen; refresh received scale dimensions
                        // explicitly instead of waiting for an entity tick that the fixture suppressed.
                        pig.refreshDimensions();
                        if(AnatomyClientNetworking.presentationContact(pig,c.level.getGameTime()).isPresent())throw new AssertionError("Old contact survived before replay");
                        if(!io.github.r3neer.scalebrews.platform.Platforms.eligible(pig,(Cow)c.level.getEntity(cowId.get())))
                            throw new AssertionError("Inconclusive: reconnected body is not admitted by support policy, scale="+((Pig)pig).getScale()
                                +" bodyWidth="+pig.getBbWidth()+" supportWidth="+c.level.getEntity(cowId.get()).getBbWidth()
                                +" bodyOrdinary="+io.github.r3neer.scalebrews.platform.Platforms.ordinary(pig)
                                +" supportOrdinary="+io.github.r3neer.scalebrews.platform.Platforms.ordinary(c.level.getEntity(cowId.get())));
                        snapshot(c,(Cow)c.level.getEntity(cowId.get()),(Pig)pig,contact.get(),"before-replay");
                    });
                    var captured=contact.get();
                    var replay=Boolean.getBoolean("scalebrews.s29ContactDelayedSequence")?new AnatomyContactPayload(
                        captured.epoch(),captured.revision(),captured.dimension(),captured.bodyId(),captured.body(),captured.trackingGeneration(),
                        captured.sequence()+1,captured.tick(),captured.supportId(),captured.support(),captured.supportBindingGeneration(),
                        captured.piece(),captured.face(),captured.localPoint(),captured.normal()):captured;
                    server.runOnServer(s->ServerPlayNetworking.send(s.getPlayerList().getPlayers().getFirst(),replay));
                    context.waitTicks(3);
                    context.runOnClient(c->{var pig=(Pig)c.level.getEntity(pigId.get());
                        boolean revived=AnatomyClientNetworking.presentationContact(pig,c.level.getGameTime()).isPresent();
                        snapshot(c,(Cow)c.level.getEntity(cowId.get()),pig,contact.get(),"after-replay");
                        System.out.println("S29 contact reconnect replay authoritative="+revived+" physicalSupported="+AnatomyMovement.supported(pig));
                        if(revived || AnatomyMovement.supported(pig))throw new AssertionError("NFR-017/FR-080: pre-disconnect confirmed contact revived under fresh support window");
                    });
                    var old=contact.get();
                    server.runOnServer(s->ServerPlayNetworking.send(s.getPlayerList().getPlayers().getFirst(),new AnatomyContactPayload(
                        old.epoch(),old.revision(),old.dimension(),old.bodyId(),old.body(),AnatomyRuntime.nextTrackingGeneration(old.trackingGeneration()),
                        old.sequence()+1,old.tick(),old.supportId(),old.support(),old.supportBindingGeneration(),old.piece(),old.face(),old.localPoint(),old.normal())));
                    context.waitFor(c->AnatomyClientNetworking.presentationContact(c.level.getEntity(pigId.get()),c.level.getGameTime()).isPresent(),100);
                    System.out.println("S29 CONTACT RECONNECT PASS old contact rejected, fresh contact positive control materialized");
                }
            } finally {server.runOnServer(s->{s.tickRateManager().setFrozen(false);AnatomyRuntime.stop(s);if(chunk.get()!=null)s.overworld().setChunkForced(chunk.get().x(),chunk.get().z(),false);});}
        }
    }
    static void assertPassive(Pig pig,String phase){
        if(!AnatomyMovement.supported(pig) || AnatomyMovement.simulates(pig) || AnatomyMovement.predictsBody(pig) || AnatomyMovement.transport(pig)!=null)
            throw new AssertionError("Passive confirmed observer incoherent at "+phase+": supported="+AnatomyMovement.supported(pig)
                +" simulates="+AnatomyMovement.simulates(pig)+" predicts="+AnatomyMovement.predictsBody(pig)+" transport="+AnatomyMovement.transport(pig));
    }
    static void snapshot(net.minecraft.client.Minecraft c,Cow cow,Pig pig,AnatomyContactPayload packet,String phase){
        var frame=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();
        var piece=frame.evaluated().pieces().get(packet.piece());var anchor=piece.point(packet.localPoint());
        var data=new LinkedHashMap<String,Object>();data.put("phase",phase);data.put("projection","top X/Z and side Z/Y, relative to support origin");
        data.put("origin",List.of(cow.getX(),cow.getY(),cow.getZ()));data.put("supportBounds",box(piece.bounds()));data.put("bodyBounds",box(pig.getBoundingBox()));
        data.put("capturedAnchor",List.of(anchor.x,anchor.y,anchor.z));data.put("drawableContact",AnatomyClientNetworking.presentationContact(pig,c.level.getGameTime()).isPresent());
        data.put("supported",AnatomyMovement.supported(pig));data.put("simulates",AnatomyMovement.simulates(pig));data.put("predicts",AnatomyMovement.predictsBody(pig));
        data.put("hasTransport",AnatomyMovement.transport(pig)!=null);data.put("revision",packet.revision());data.put("bodyTrackingGeneration",packet.trackingGeneration());
        data.put("supportBindingGeneration",packet.supportBindingGeneration());data.put("piece",packet.piece());data.put("contactTick",packet.tick());data.put("clientWorldTick",c.level.getGameTime());
        data.put("endpointSerial",frame.after().frameSerial());data.put("presentationKind",frame.kind().toString());
        try{java.nio.file.Files.writeString(java.nio.file.Path.of("s29-contact-"+phase+".json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(data));}
        catch(java.io.IOException e){throw new AssertionError("Snapshot metadata failed",e);}
    }
    private static List<Double> box(net.minecraft.world.phys.AABB b){return List.of(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ);}
    private static List<AnatomyCatalogPayload> bundle(net.minecraft.server.MinecraftServer s){try{
        var sf=AnatomyRuntime.class.getDeclaredField("STATES");sf.setAccessible(true);var state=((Map<?,?>)sf.get(null)).get(s);
        var cf=state.getClass().getDeclaredField("catalog");cf.setAccessible(true);var catalog=(WorldAnatomyCatalog)cf.get(state);
        var m=WorldAnatomyCatalog.class.getDeclaredMethod("preparedPackets",UUID.class);m.setAccessible(true);
        @SuppressWarnings("unchecked") var packets=(List<AnatomyCatalogPayload>)m.invoke(catalog,AnatomyNetworking.epoch(s));return packets;
    }catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    static Object field(String name){try{var f=AnatomyClientNetworking.class.getDeclaredField(name);f.setAccessible(true);return f.get(null);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
