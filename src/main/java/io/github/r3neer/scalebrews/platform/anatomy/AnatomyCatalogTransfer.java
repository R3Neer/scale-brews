package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/** One connection's ordered revisions; incomplete/invalid candidates never replace accepted geometry. */
public final class AnatomyCatalogTransfer {
    private UUID epoch;
    private long acceptedRevision=-1,pendingRevision=-1;
    private String digest;
    private byte[][] chunks;
    private int totalBytes,received;
    private final WorldAnatomyCatalog catalog=new WorldAnatomyCatalog();
    public WorldAnatomyCatalog.Snapshot snapshot(){return catalog.snapshot();}
    public long revision(){return acceptedRevision;}
    public UUID epoch(){return epoch;}
    private static String hash(byte[] bytes) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public static List<AnatomyCatalogPayload> encode(UUID epoch,long revision,Map<String,ModelGeometry> models) {
        return encode(epoch,revision,models,Map.of());
    }
    public static List<AnatomyCatalogPayload> encode(UUID epoch,long revision,Map<String,ModelGeometry> models,Map<String,io.github.r3neer.scalebrews.platform.PlatformDefinition> profiles) {
        new WorldAnatomyCatalog().replace(models,profiles);
        byte[] bytes=serializedBundle(models,profiles);
        int count=(bytes.length+AnatomyCatalogPayload.CHUNK-1)/AnatomyCatalogPayload.CHUNK;String digest=hash(bytes);
        List<AnatomyCatalogPayload> result=new ArrayList<>();
        for(int index=0;index<count;index++)result.add(new AnatomyCatalogPayload(epoch,revision,index,count,bytes.length,digest,
            Arrays.copyOfRange(bytes,index*AnatomyCatalogPayload.CHUNK,Math.min(bytes.length,(index+1)*AnatomyCatalogPayload.CHUNK))));
        return List.copyOf(result);
    }
    static byte[] serializedBundle(Map<String,ModelGeometry> models,Map<String,io.github.r3neer.scalebrews.platform.PlatformDefinition> profiles) {
        var bundle=new com.google.gson.JsonObject();bundle.add("models",new Gson().toJsonTree(new TreeMap<>(models)));
        var definitions=new com.google.gson.JsonObject();
        new TreeMap<>(profiles).forEach((id,profile)->definitions.add(id,io.github.r3neer.scalebrews.platform.PlatformDefinition.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,profile).getOrThrow()));
        bundle.add("profiles",definitions);
        byte[] bytes=bundle.toString().getBytes(StandardCharsets.UTF_8);
        if(bytes.length>AnatomyCatalogPayload.MAX_BYTES)throw new IllegalArgumentException("Catalog transfer exceeds size limit");
        return bytes;
    }
    public boolean accept(AnatomyCatalogPayload packet) {
        if(epoch==null)epoch=packet.epoch();
        if(!epoch.equals(packet.epoch()))throw new IllegalArgumentException("Catalog belongs to a different connection epoch");
        if(packet.revision()<=acceptedRevision || packet.revision()<pendingRevision)return false;
        if(packet.revision()!=pendingRevision) {
            pendingRevision=packet.revision();digest=packet.digest();totalBytes=packet.totalBytes();
            chunks=new byte[packet.count()][];received=0;
        }
        if(!digest.equals(packet.digest()) || totalBytes!=packet.totalBytes() || chunks.length!=packet.count())throw new IllegalArgumentException("Conflicting catalog fragments");
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
        Map<String,ModelGeometry> models=new Gson().fromJson(bundle.get("models"),new TypeToken<Map<String,ModelGeometry>>(){}.getType());
        if(models==null)throw new IllegalArgumentException("Missing catalog object");
        Map<String,io.github.r3neer.scalebrews.platform.PlatformDefinition> profiles=new TreeMap<>();
        for(var entry:bundle.getAsJsonObject("profiles").entrySet())profiles.put(entry.getKey(),io.github.r3neer.scalebrews.platform.PlatformDefinition.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,entry.getValue()).getOrThrow());
        catalog.replace(models,profiles);acceptedRevision=pendingRevision;chunks=null;
        return true;
    }
}
