package io.github.r3neer.scalebrews.test;

import com.google.gson.Gson;
import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyCatalogPayload;
import io.github.r3neer.scalebrews.collision.internal.AnatomyCatalogTransfer;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.phys.Vec3;

/**
 * Exercises two real dedicated hosts against one client, while deliberately
 * changing only a disposable client resource pack between connections.  The
 * anatomy bundle comes from the existing immutable export property; this test
 * never regenerates or writes a gameplay catalog.
 */
public final class AnatomyHostCatalogIndependenceProof implements FabricClientGameTest {
    private static final UUID SUPPORT_UUID=UUID.fromString("d7a7868b-0d2c-4d01-8d4a-2bc06af7c101");
    private static final UUID BODY_UUID=UUID.fromString("d7a7868b-0d2c-4d01-8d4a-2bc06af7c102");
    private static final String PACK_ID="scalebrews-host-independence-proof";
    private static final Identifier PACK_MARKER=Identifier.parse("scalebrews_host_proof:active");

    @Override
    public void runTest(ClientGameTestContext context) {
        try {run(context);} catch(Exception failure) {throw new AssertionError("Host/catalog independence proof failed",failure);}
    }

    private static void run(ClientGameTestContext context) throws Exception {
        var exported=requiredCatalog();
        var profile=profile(exported.cow());
        var profiles=Map.of("scalebrews:host_independence_cow",profile);
        var expectedDigest=digest(exported.models(),profiles);
        var properties=new java.util.Properties();
        properties.setProperty("allow-flight","false");
        ResourcePackScope pack=null;
        UUID firstEpoch;
        int firstNetworkId;
        Throwable primary=null;
        try {
            try(var first=context.worldBuilder().createServer(properties);var connection=first.connect()) {
                var fixture=new Fixture();
                first.runOnServer(server->boot(server,exported.models(),profiles,fixture));
                awaitPublished(context,fixture);
                firstEpoch=context.computeOnClient(client->assertAccepted(client,fixture,expectedDigest,"first host"));
                first.runOnServer(server->confirm(server,fixture));
                awaitContact(context,fixture,"first host");
                firstNetworkId=fixture.bodyId.get();
                if(AnatomyCatalogTransfer.encode(firstEpoch,1,exported.models(),profiles).getFirst().digest().equals(expectedDigest)==false)
                    throw new AssertionError("First host did not encode the immutable export bundle");
                System.out.println("ANATOMY_HOST first epoch="+firstEpoch+" bodyId="+firstNetworkId+" digest="+expectedDigest);
                first.runOnServer(AnatomyRuntime::stop);
            }

            context.waitFor(client->client.level==null,100);
            context.runOnClient(client->{
                if(AnatomyClientNetworking.catalog().announced() || AnatomyClientNetworking.catalog().revision()!=-1
                        || AnatomyClientNetworking.pose(SUPPORT_UUID)!=null || AnatomyClientNetworking.pose(BODY_UUID)!=null)
                    throw new AssertionError("Disconnect retained host-one catalog, pose, or connection state");
            });

            pack=new ResourcePackScope();
            enableAlternatePack(context,pack);
            assertExportUnchanged(exported);

            try(var second=context.worldBuilder().createServer(properties);var connection=second.connect()) {
                var fixture=new Fixture();
                // Spawn matching identities before any host-two catalog is announced.  This
                // catches stale client maps even if the dedicated harness reuses IDs.
                second.runOnServer(server->spawn(server,fixture));
                context.waitFor(client->client.level!=null && client.level.getEntity(fixture.bodyId.get())!=null,100);
                context.runOnClient(client->{
                    var body=client.level.getEntity(fixture.bodyId.get());
                    if(!(body instanceof LivingEntity living) || !body.getUUID().equals(BODY_UUID))
                        throw new AssertionError("Host two did not expose the reused body identity");
                    if(AnatomyClientNetworking.pose(BODY_UUID)!=null || AnatomyClientNetworking.presentationContact(living,client.level.getGameTime()).isPresent()
                            || AnatomyMovement.contact(living)!=null || AnatomyMovement.transport(living)!=null)
                        throw new AssertionError("Host-one pose/contact/carry leaked before host-two catalog publication");
                    if(living.getDeltaMovement().lengthSqr()>1e-12)
                        throw new AssertionError("Host-two body inherited a transport delta before publication");
                });
                second.runOnServer(server->start(server,exported.models(),profiles,fixture));
                awaitPublished(context,fixture);
                var secondEpoch=context.computeOnClient(client->assertAccepted(client,fixture,expectedDigest,"second host"));
                if(firstEpoch.equals(secondEpoch))throw new AssertionError("Reconnect reused the previous host epoch");

                // Deliver a genuine old catalog payload over the second host's real
                // connection.  The client must retain the host-two accepted epoch.
                var stale=AnatomyCatalogTransfer.encode(firstEpoch,1,exported.models(),profiles);
                second.runOnServer(server->{
                    var player=server.getPlayerList().getPlayers().getFirst();
                    for(AnatomyCatalogPayload packet:stale)ServerPlayNetworking.send(player,packet);
                });
                context.waitTicks(10);
                context.runOnClient(client->{
                    var accepted=AnatomyClientNetworking.catalog();
                    if(!secondEpoch.equals(accepted.epoch()) || accepted.revision()!=1
                            || !digest(accepted.snapshot().models(),accepted.snapshot().profiles()).equals(expectedDigest))
                        throw new AssertionError("Old first-host catalog replaced host-two accepted state");
                });
                second.runOnServer(server->confirm(server,fixture));
                awaitContact(context,fixture,"second host");
                System.out.println("ANATOMY_HOST second epoch="+secondEpoch+" bodyId="+fixture.bodyId.get()
                    +" reusedNetworkId="+(firstNetworkId==fixture.bodyId.get())+" pack="+PACK_ID);
                second.runOnServer(AnatomyRuntime::stop);
            }
        } catch(Throwable failure) {
            primary=failure;throw failure;
        } finally {
            if(pack!=null)try {pack.restore(context);} catch(Throwable cleanup) {
                if(primary!=null)primary.addSuppressed(cleanup);else if(cleanup instanceof Exception exception)throw exception;else throw (Error)cleanup;
            }
        }
    }

    private static void boot(net.minecraft.server.MinecraftServer server,Map<String,ModelGeometry> models,
            Map<String,PlatformDefinition> profiles,Fixture fixture) {
        spawn(server,fixture);
        start(server,models,profiles,fixture);
    }

    private static void spawn(net.minecraft.server.MinecraftServer server,Fixture fixture) {
        var level=server.overworld();
        var cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
        var pig=EntityTypes.PIG.create(level,EntitySpawnReason.COMMAND);
        if(cow==null || pig==null)throw new IllegalStateException("Dedicated fixture could not create cow/pig");
        cow.setUUID(SUPPORT_UUID);cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(3,2,3);level.addFreshEntity(cow);
        pig.setUUID(BODY_UUID);pig.setNoAi(true);pig.setNoGravity(true);pig.setPos(3,4,3);level.addFreshEntity(pig);
        pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);pig.refreshDimensions();
        fixture.cow.set(cow);fixture.pig.set(pig);fixture.bodyId.set(pig.getId());
    }

    private static void start(net.minecraft.server.MinecraftServer server,Map<String,ModelGeometry> models,
            Map<String,PlatformDefinition> profiles,Fixture fixture) {
        AnatomyRuntime.startPrepared(server,models,profiles);
    }

    private static void confirm(net.minecraft.server.MinecraftServer server,Fixture fixture) {
        Cow cow=fixture.cow.get();Pig pig=fixture.pig.get();
        var frame=AnatomyMovement.queryFrame(cow).orElseThrow(()->new IllegalStateException("Prepared runtime did not publish a current cow QueryFrame"));
        var snapshot=frame.snapshot();
        var before=pig.getBoundingBox();var center=before.getCenter();
        FaceCandidate candidate=null;
        outer: for(var entry:snapshot.pieces().entrySet())for(int face=0;face<6;face++) {
            var piece=entry.getValue();var normal=piece.faceNormal(face);if(normal.y<=.99)continue;
            var local=piece.facePoint(face,piece.bounds().getCenter());var point=piece.point(local);
            double extent=(before.getXsize()*Math.abs(normal.x)+before.getYsize()*Math.abs(normal.y)+before.getZsize()*Math.abs(normal.z))*.5;
            var body=before.move(point.add(normal.scale(extent+.001)).subtract(center));
            if(server.overworld().noCollision(pig,body) && snapshot.pieces().values().stream().noneMatch(other->other.overlaps(body))) {
                candidate=new FaceCandidate(entry.getKey(),face,local,normal,body);break outer;
            }
        }
        if(candidate==null)throw new AssertionError("Exported cow has no exposed upper face with a clear pig AABB");
        final FaceCandidate selected=candidate;
        pig.setPos(pig.position().add(selected.body().getCenter().subtract(center)));
        if(!pig.getBoundingBox().equals(selected.body()) || snapshot.pieces().values().stream().anyMatch(piece->piece.overlaps(selected.body()))
                || !server.overworld().noCollision(pig,selected.body()))
            throw new AssertionError("Prepared host contact lacks a clear real AABB above every exported cow piece");
        var surface=new SurfaceContact(cow.getUUID(),frame.identity().revision(),selected.piece(),selected.face(),selected.local(),selected.normal(),server.overworld().getGameTime());
        if(!AnatomyMovement.confirm(pig,cow,surface))throw new AssertionError("Server could not confirm prepared original-cow material contact");
        if(!AnatomyMovement.supported(pig))throw new AssertionError("Confirmed host contact was not physically supported by its selected cow face");
        var beforeMove=pig.position();cow.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(.125,0,0));AnatomyMovement.carry(pig);
        var carry=AnatomyMovement.transport(pig);
        if(carry==null || carry.appliedDelta().lengthSqr()<1e-8 || pig.position().distanceToSqr(beforeMove)<1e-8)
            throw new AssertionError("Host-one support movement did not produce an actual material carry before disconnect");
    }

    private static void awaitPublished(ClientGameTestContext context,Fixture fixture) {
        context.waitFor(client->{
            net.minecraft.world.entity.Entity support=client.level==null?null:client.level.getEntity(fixture.cow.get().getId());
            return support instanceof LivingEntity living && living.getUUID().equals(SUPPORT_UUID)
                && AnatomyClientNetworking.catalog().ready() && AnatomyClientNetworking.pose(SUPPORT_UUID)!=null
                && AnatomyClientNetworking.geometry(living,AnatomyClientNetworking.pose(SUPPORT_UUID).current().tick()).isPresent();
        },200);
    }

    private static void awaitContact(ClientGameTestContext context,Fixture fixture,String label) {
        context.waitFor(client->{
            var body=client.level==null?null:client.level.getEntity(fixture.bodyId.get());
            return body instanceof LivingEntity living && AnatomyClientNetworking.presentationContact(living,client.level.getGameTime()).isPresent();
        },200);
        context.runOnClient(client->{
            var body=(LivingEntity)client.level.getEntity(fixture.bodyId.get());
            var contact=AnatomyClientNetworking.presentationContact(body,client.level.getGameTime()).orElseThrow();
            if(!contact.support().getUUID().equals(SUPPORT_UUID) || AnatomyMovement.transport(body)!=null)
                throw new AssertionError(label+" did not retain exactly the new confirmed contact without client carry");
        });
    }

    private static UUID assertAccepted(net.minecraft.client.Minecraft client,Fixture fixture,String expectedDigest,String label) {
        var transfer=AnatomyClientNetworking.catalog();
        var support=client.level.getEntity(fixture.cow.get().getId());
        var pose=AnatomyClientNetworking.pose(SUPPORT_UUID);
        if(!transfer.ready() || transfer.epoch()==null || transfer.revision()!=1 || !(support instanceof LivingEntity living)
                || pose==null || !pose.current().inputs().ordinary() || AnatomyClientNetworking.geometry(living,pose.current().tick()).isEmpty())
            throw new AssertionError(label+" did not admit the original cow catalog/pose");
        if(!digest(transfer.snapshot().models(),transfer.snapshot().profiles()).equals(expectedDigest))
            throw new AssertionError(label+" geometry/profile digest differs from the immutable export bundle");
        return transfer.epoch();
    }

    private static PlatformDefinition profile(ModelGeometry cow) {
        return new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse(cow.source()),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
    }

    private record ExportedCatalog(ModelGeometry cow,Map<String,ModelGeometry> models,Path root,String sourceHash) {}

    private static ExportedCatalog requiredCatalog() throws IOException {
        var property=System.getProperty("scalebrews.anatomyCatalog");
        if(property==null || property.isBlank())throw new IllegalStateException("Host proof requires the existing isolated anatomy export");
        var root=Path.of(property);var gson=new Gson();
        var cow=gson.fromJson(Files.readString(root.resolve("minecraft_cow.json")),ModelGeometry.class);
        var player=gson.fromJson(Files.readString(root.resolve("minecraft_player_wide.json")),ModelGeometry.class);
        if(cow==null || player==null || cow.format()!=2 || player.format()!=2 || !"minecraft:cow".equals(cow.source()) || !"minecraft:player_wide".equals(player.source()))
            throw new IllegalArgumentException("Host proof needs the existing original cow/player format-2 export");
        return new ExportedCatalog(cow,Map.of(cow.source(),cow,player.source(),player),root,exportHash(root));
    }

    private static void assertExportUnchanged(ExportedCatalog catalog) throws IOException {
        if(!catalog.sourceHash().equals(exportHash(catalog.root())))
            throw new AssertionError("Host proof export changed between servers instead of reusing the immutable input");
    }

    private static String exportHash(Path root) throws IOException {
        return hash((hash(Files.readAllBytes(root.resolve("minecraft_cow.json")))+":"
            +hash(Files.readAllBytes(root.resolve("minecraft_player_wide.json")))).getBytes(StandardCharsets.UTF_8));
    }

    private static String digest(Map<String,ModelGeometry> models,Map<String,PlatformDefinition> profiles) {
        return AnatomyCatalogTransfer.encode(UUID.fromString("00000000-0000-0000-0000-000000000001"),1,models,profiles).getFirst().digest();
    }

    private static String hash(byte[] bytes) {
        try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }

    private static void enableAlternatePack(ClientGameTestContext context,ResourcePackScope scope) throws Exception {
        context.runOnClient(client->{
            scope.previousPacks=new ArrayList<>(client.options.resourcePacks);
            scope.previousIncompatible=new ArrayList<>(client.options.incompatibleResourcePacks);
            scope.root=client.getResourcePackDirectory().resolve(PACK_ID);
            var format=SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES);
            Files.createDirectories(scope.root.resolve("assets/scalebrews_host_proof"));
            Files.writeString(scope.root.resolve("pack.mcmeta"),"{\"pack\":{\"description\":\"Disposable host-independence proof\",\"min_format\":["+format.major()+","+format.minor()+"],\"max_format\":["+format.major()+","+format.minor()+"]}}",StandardCharsets.UTF_8);
            Files.writeString(scope.root.resolve("assets/scalebrews_host_proof/active"),"This marker proves the isolated alternate pack was reloaded.",StandardCharsets.UTF_8);
            var repository=client.getResourcePackRepository();repository.reload();
            scope.previousSelected=new ArrayList<>(repository.getSelectedIds());
            scope.selectedId=repository.getAvailablePacks().stream().filter(pack->pack.location().id().endsWith(PACK_ID))
                .map(net.minecraft.server.packs.repository.Pack::getId).findFirst()
                .orElseThrow(()->new IllegalStateException("Disposable host proof pack was not discovered by the real repository"));
            var selected=new ArrayList<>(scope.previousSelected);selected.add(scope.selectedId);repository.setSelected(selected);
            client.options.updateResourcePacks(repository);
            scope.reload.set(client.reloadResourcePacks());
        });
        waitReload(context,scope.reload,"alternate resource pack");
        context.runOnClient(client->{
            if(!client.getResourcePackRepository().getSelectedIds().contains(scope.selectedId) || client.getResourceManager().getResource(PACK_MARKER).isEmpty())
                throw new AssertionError("Alternate client resource pack was selected but not active after reload");
        });
    }

    private static void waitReload(ClientGameTestContext context,AtomicReference<CompletableFuture<Void>> reload,String label) {
        context.waitFor(client->reload.get()!=null && reload.get().isDone(),200);
        context.runOnClient(client->{try{reload.get().join();}catch(RuntimeException failure){throw new AssertionError("Failed to reload "+label,failure);}});
    }

    private static final class ResourcePackScope {
        private Path root;
        private String selectedId;
        private List<String> previousPacks;
        private List<String> previousIncompatible;
        private List<String> previousSelected;
        private final AtomicReference<CompletableFuture<Void>> reload=new AtomicReference<>();

        void restore(ClientGameTestContext context) throws Exception {
            if(previousPacks==null || previousSelected==null)return;
            context.runOnClient(client->{
                var repository=client.getResourcePackRepository();
                repository.setSelected(previousSelected);client.options.updateResourcePacks(repository);reload.set(client.reloadResourcePacks());
            });
            waitReload(context,reload,"original client resource-pack selection");
            context.runOnClient(client->{
                var selected=client.getResourcePackRepository().getSelectedIds();boolean marker=client.getResourceManager().getResource(PACK_MARKER).isPresent();
                if(!client.options.resourcePacks.equals(previousPacks) || !selected.equals(previousSelected) || marker)
                    throw new AssertionError("Host proof did not restore the isolated client pack selection: expectedOptions="+previousPacks
                        +", actualOptions="+client.options.resourcePacks+", selected="+selected+", proofId="+selectedId+", marker="+marker);
            });
            if(root==null)return;
            try(var paths=Files.walk(root)) {
                for(var path:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(path);
            }
        }
    }

    private static final class Fixture {
        final AtomicReference<Cow> cow=new AtomicReference<>();
        final AtomicReference<Pig> pig=new AtomicReference<>();
        final AtomicInteger bodyId=new AtomicInteger(-1);
    }
    private record FaceCandidate(String piece,int face,Vec3 local,Vec3 normal,net.minecraft.world.phys.AABB body) {}
}
