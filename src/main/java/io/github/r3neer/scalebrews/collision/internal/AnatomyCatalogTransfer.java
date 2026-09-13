package io.github.r3neer.scalebrews.collision.internal;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.AnatomyMode;
import io.github.r3neer.scalebrews.collision.catalog.CollisionBindingCatalog;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionCodecs;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.migration.LegacyAnatomyCatalogMigration;
import io.github.r3neer.scalebrews.collision.pose.PoseProgram;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/** One connection's ordered revisions; incomplete/invalid candidates never replace accepted geometry/programs. */
public final class AnatomyCatalogTransfer {
    private UUID epoch;
    private int protocolVersion;
    private long requiredCapabilities;
    private long acceptedRevision=-1,pendingRevision=-1;
    private String digest;
    private byte[][] chunks;
    private int totalBytes,received;
    private final WorldAnatomyCatalog catalog=new WorldAnatomyCatalog();
    public WorldAnatomyCatalog.Snapshot snapshot(){return catalog.snapshot();}
    public long revision(){return acceptedRevision;}
    public UUID epoch(){return epoch;}
    /** No server announcement means the client must leave shared anatomy disabled. */
    public boolean announced(){return epoch!=null;}
    /** A compatible replacement is transferring; the previous accepted snapshot is retained. */
    public boolean binding(){return announced() && pendingRevision>acceptedRevision;}
    /** The last complete catalog remains usable unless a replacement is actively binding. */
    public boolean ready(){return acceptedRevision>=0 && !binding();}
    /** Connection-level client mode; callers still reject entities from another level. */
    public AnatomyMode mode(){return !announced()?AnatomyMode.DISABLED:binding()?AnatomyMode.BINDING:ready()?AnatomyMode.READY:AnatomyMode.DISABLED;}
    /** Reject only the candidate; never discard an atomically accepted prior revision. */
    public void rejectPending() {
        if(pendingRevision<=acceptedRevision)return;
        pendingRevision=acceptedRevision;chunks=null;received=0;totalBytes=0;digest=null;
        if(acceptedRevision<0){epoch=null;protocolVersion=0;requiredCapabilities=0;}
    }
    private static String hash(byte[] bytes) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }

    /** Immutable serialization/hash/fragmentation result for one accepted catalog revision. */
    static final class PreparedBundle {
        private final int totalBytes;
        private final String digest;
        private final List<byte[]> fragments;
        private UUID packetEpoch;
        private long packetRevision=Long.MIN_VALUE;
        private List<AnatomyCatalogPayload> packets;

        private PreparedBundle(byte[] bytes) {
            Objects.requireNonNull(bytes,"bytes");
            if(bytes.length<1 || bytes.length>AnatomyCatalogPayload.MAX_BYTES)throw new IllegalArgumentException("Invalid prepared catalog size");
            totalBytes=bytes.length;digest=hash(bytes);
            int count=(bytes.length+AnatomyCatalogPayload.CHUNK-1)/AnatomyCatalogPayload.CHUNK;
            var split=new ArrayList<byte[]>(count);
            for(int index=0;index<count;index++)split.add(Arrays.copyOfRange(bytes,index*AnatomyCatalogPayload.CHUNK,
                Math.min(bytes.length,(index+1)*AnatomyCatalogPayload.CHUNK)));
            fragments=List.copyOf(split);
        }

        synchronized List<AnatomyCatalogPayload> packets(UUID epoch,long revision) {
            Objects.requireNonNull(epoch,"epoch");
            if(revision<0)throw new IllegalArgumentException("Negative catalog revision");
            if(packets!=null && epoch.equals(packetEpoch) && revision==packetRevision)return packets;
            var built=new ArrayList<AnatomyCatalogPayload>(fragments.size());
            for(int index=0;index<fragments.size();index++)built.add(new AnatomyCatalogPayload(epoch,AnatomyApi.PROTOCOL_VERSION,
                AnatomyApi.capabilities(),revision,index,fragments.size(),totalBytes,digest,fragments.get(index)));
            packetEpoch=epoch;packetRevision=revision;packets=List.copyOf(built);return packets;
        }
    }

    static PreparedBundle prepareBundle(Map<String,ModelGeometry> models,Collection<CollisionBinding> bindings) {
        return prepareBundle(models,Map.of(),bindings);
    }

    static PreparedBundle prepareBundle(Map<String,ModelGeometry> models,Map<String,PoseProgram> programs,
                                        Collection<CollisionBinding> bindings) {
        return new PreparedBundle(serializedBundle(models,programs,bindings));
    }

    public static List<AnatomyCatalogPayload> encode(UUID epoch,long revision,Map<String,ModelGeometry> models) {
        return encode(epoch,revision,models,Map.of(),List.of());
    }

    /** Canonical fixture/convenience encoder. Production publication uses WorldAnatomyCatalog's prepared bundle. */
    public static List<AnatomyCatalogPayload> encode(UUID epoch,long revision,Map<String,ModelGeometry> models,
                                                      Collection<CollisionBinding> bindings) {
        return encode(epoch,revision,models,Map.of(),bindings);
    }

    public static List<AnatomyCatalogPayload> encode(UUID epoch,long revision,Map<String,ModelGeometry> models,
                                                      Map<String,PoseProgram> programs,Collection<CollisionBinding> bindings) {
        var validation=new WorldAnatomyCatalog();
        validation.replaceAtRevision(revision,models,programs,bindings);
        return validation.preparedPackets(epoch);
    }

    /** Legacy fixture seam: migrate before crossing the authoritative catalog boundary. */
    public static List<AnatomyCatalogPayload> encode(UUID epoch,long revision,Map<String,ModelGeometry> models,
            Map<String,io.github.r3neer.scalebrews.platform.PlatformDefinition> profiles) {
        return encode(epoch,revision,models,Map.of(),LegacyAnatomyCatalogMigration.bindings(profiles));
    }

    static byte[] serializedBundle(Map<String,ModelGeometry> models,Collection<CollisionBinding> bindings) {
        return serializedBundle(models,Map.of(),bindings);
    }

    static byte[] serializedBundle(Map<String,ModelGeometry> models,Map<String,PoseProgram> programs,
                                   Collection<CollisionBinding> bindings) {
        Objects.requireNonNull(models,"models");Objects.requireNonNull(programs,"programs");
        var canonical=new CollisionBindingCatalog(bindings);
        var validatedPrograms=new TreeMap<String,PoseProgram>();
        programs.forEach((id,program)->validatedPrograms.put(id,PoseProgram.validatedCopy(program)));
        var bundle=new com.google.gson.JsonObject();
        bundle.add("models",new Gson().toJsonTree(new TreeMap<>(models)));
        bundle.add("pose_programs",new Gson().toJsonTree(validatedPrograms));
        var encoded=new com.google.gson.JsonArray();
        for(var binding:canonical.bindings())encoded.add(CollisionCodecs.BINDING.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,binding).getOrThrow());
        bundle.add("bindings",encoded);
        byte[] bytes=bundle.toString().getBytes(StandardCharsets.UTF_8);
        if(bytes.length>AnatomyCatalogPayload.MAX_BYTES)throw new IllegalArgumentException("Catalog transfer exceeds size limit");
        return bytes;
    }

    public boolean accept(AnatomyCatalogPayload packet) {
        if(!AnatomyApi.compatible(packet.protocolVersion(),packet.requiredCapabilities()))throw new IllegalArgumentException("Incompatible anatomy protocol/capabilities");
        if(epoch==null)epoch=packet.epoch();
        if(!epoch.equals(packet.epoch()))throw new IllegalArgumentException("Catalog belongs to a different connection epoch");
        if(packet.revision()<=acceptedRevision || packet.revision()<pendingRevision)return false;
        if(packet.revision()!=pendingRevision) {
            pendingRevision=packet.revision();protocolVersion=packet.protocolVersion();requiredCapabilities=packet.requiredCapabilities();digest=packet.digest();totalBytes=packet.totalBytes();
            chunks=new byte[packet.count()][];received=0;
        }
        if(protocolVersion!=packet.protocolVersion() || requiredCapabilities!=packet.requiredCapabilities() || !digest.equals(packet.digest()) || totalBytes!=packet.totalBytes() || chunks.length!=packet.count())throw new IllegalArgumentException("Conflicting catalog fragments");
        byte[] fragment=packet.fragment();
        if(chunks[packet.index()]!=null) {
            if(!Arrays.equals(chunks[packet.index()],fragment))throw new IllegalArgumentException("Conflicting repeated fragment");
            return false;
        }
        chunks[packet.index()]=fragment;received++;
        if(received!=chunks.length)return false;
        byte[] complete=new byte[totalBytes];
        for(int index=0;index<chunks.length;index++)System.arraycopy(chunks[index],0,complete,index*AnatomyCatalogPayload.CHUNK,chunks[index].length);
        if(!hash(complete).equals(digest))throw new IllegalArgumentException("Catalog integrity check failed");
        var bundle=com.google.gson.JsonParser.parseString(new String(complete,StandardCharsets.UTF_8)).getAsJsonObject();
        if(bundle.has("profiles") || !bundle.has("models") || !bundle.has("pose_programs") || !bundle.has("bindings"))
            throw new IllegalArgumentException("Invalid protocol-v5 catalog object");
        var gson=new Gson();
        Map<String,ModelGeometry> models=gson.fromJson(bundle.get("models"),new TypeToken<Map<String,ModelGeometry>>(){}.getType());
        if(models==null)throw new IllegalArgumentException("Missing catalog models");
        Map<String,PoseProgram> rawPrograms=gson.fromJson(bundle.get("pose_programs"),new TypeToken<Map<String,PoseProgram>>(){}.getType());
        if(rawPrograms==null)throw new IllegalArgumentException("Missing catalog pose programs");
        Map<String,PoseProgram> programs=new TreeMap<>();
        rawPrograms.forEach((id,program)->programs.put(id,PoseProgram.validatedCopy(program)));
        var bindingJson=bundle.get("bindings");
        if(!bindingJson.isJsonArray())throw new IllegalArgumentException("Missing canonical binding array");
        List<CollisionBinding> bindings=new ArrayList<>();
        for(var element:bindingJson.getAsJsonArray())bindings.add(CollisionCodecs.BINDING.parse(com.mojang.serialization.JsonOps.INSTANCE,element).getOrThrow());
        catalog.replaceAtRevision(pendingRevision,models,programs,bindings);acceptedRevision=pendingRevision;chunks=null;
        return true;
    }
}
