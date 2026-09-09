package io.github.r3neer.scalebrews.platform.anatomy;

import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import io.github.r3neer.scalebrews.platform.Platforms;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Publish geometry and species bindings together, only after every reference has validated. */
public final class WorldAnatomyCatalog {
    public record Binding(PlatformDefinition policy,ModelGeometry model,PoseProvider poses) {}
    public record Snapshot(long revision,Map<String,ModelGeometry> models,Map<Identifier,Binding> bindings,Map<String,PlatformDefinition> profiles) {
        public Snapshot {models=Map.copyOf(models);bindings=Map.copyOf(bindings);profiles=Map.copyOf(profiles);}
    }
    private volatile Snapshot current=new Snapshot(0,Map.of(),Map.of(),Map.of());
    public WorldAnatomyCatalog() {}
    public WorldAnatomyCatalog(long previousRevision) {
        if(previousRevision<0)throw new IllegalArgumentException("Negative catalog revision");
        current=new Snapshot(previousRevision,Map.of(),Map.of(),Map.of());
    }
    public Snapshot snapshot(){return current;}
    public Snapshot reload(net.minecraft.server.packs.resources.ResourceManager resources) {
        var models=read(resources,"scalebrews/entity_geometry",AnatomyCodecs.GEOMETRY);
        var profiles=read(resources,"scalebrews/entity_platform",PlatformDefinition.CODEC);
        return replace(models,profiles);
    }
    private static <T> Map<String,T> read(net.minecraft.server.packs.resources.ResourceManager resources,String directory,com.mojang.serialization.Codec<T> codec) {
        var files=resources.listResources(directory,id->id.getPath().endsWith(".json"));
        if(files.size()>4096)throw new IllegalArgumentException("Too many resources under "+directory);
        Map<String,T> result=new TreeMap<>();long total=0;
        for(var entry:files.entrySet()) {
            try(var reader=entry.getValue().openAsReader()) {
                var text=new StringBuilder();char[] buffer=new char[8192];int count;
                while((count=reader.read(buffer))!=-1) {
                    total+=count;if(total>AnatomyCatalogPayload.MAX_BYTES)throw new IllegalArgumentException("Catalog resource limit exceeded");
                    text.append(buffer,0,count);
                }
                var file=entry.getKey();var path=file.getPath();
                String id=Identifier.fromNamespaceAndPath(file.getNamespace(),path.substring(directory.length()+1,path.length()-5)).toString();
                result.put(id,codec.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(text.toString())).getOrThrow());
            }catch(java.io.IOException | RuntimeException invalid){throw new IllegalArgumentException("Invalid anatomical resource "+entry.getKey(),invalid);}
        }
        return result;
    }
    public Snapshot reload(RegistryAccess registries) {
        Map<String,ModelGeometry> models=new TreeMap<>();Map<String,PlatformDefinition> profiles=new TreeMap<>();
        registries.lookup(Platforms.GEOMETRIES).ifPresent(registry->registry.listElements().forEach(h->models.put(h.key().identifier().toString(),h.value())));
        registries.lookup(Platforms.DEFINITIONS).ifPresent(registry->registry.listElements().forEach(h->profiles.put(h.key().identifier().toString(),h.value())));
        return replace(models,profiles);
    }
    public synchronized Snapshot replace(Map<String,ModelGeometry> models,Map<String,PlatformDefinition> profiles) {
        if(profiles.size()>4096)throw new IllegalArgumentException("Too many anatomical profiles");
        List<String> references=new ArrayList<>();Map<Identifier,Binding> bindings=new HashMap<>();Set<Identifier> species=new HashSet<>();
        for(var entry:new TreeMap<>(profiles).entrySet()) {
            if(!entry.getKey().matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))throw new IllegalArgumentException("Invalid anatomical profile identifier");
            var profile=entry.getValue();
            if(!species.add(profile.entity()))throw new IllegalArgumentException("Duplicate platform species "+profile.entity());
            if(profile.anatomy().isEmpty())continue; // Legacy planes are not convex anatomy.
            var anatomy=profile.anatomy().orElseThrow();String id=anatomy.model().toString();references.add(id);
            var model=models.get(id);
            if(model==null)throw new IllegalArgumentException("Missing geometry "+id+" in "+entry.getKey());
            var provider=PoseProviders.find(anatomy.poses()).orElseThrow(()->new IllegalArgumentException("Missing pose provider "+anatomy.poses()));
            if(provider.evaluate(model,new PoseProvider.Inputs(0,0,0,0,0,true)).isEmpty())
                throw new IllegalArgumentException("Pose provider does not support model/version: "+entry.getKey());
            Set<String> ids=new HashSet<>();model.parts().forEach(p->ids.add(p.id()));model.pieces().forEach(p->ids.add(p.id()));
            for(String selected:java.util.stream.Stream.concat(anatomy.filter().include().stream(),anatomy.filter().exclude().stream()).toList())
                if(!ids.contains(selected))throw new IllegalArgumentException("Missing selected piece/part "+selected+" in "+entry.getKey());
            bindings.put(profile.entity(),new Binding(profile,model,provider));
        }
        var validated=new GeometryCatalog().replace(models,references);
        AnatomyCatalogTransfer.serializedBundle(validated.models(),profiles); // Reject unsendable bundles before committing server state.
        Snapshot next=new Snapshot(Math.incrementExact(current.revision()),validated.models(),bindings,profiles);
        current=next;
        return next;
    }
}
