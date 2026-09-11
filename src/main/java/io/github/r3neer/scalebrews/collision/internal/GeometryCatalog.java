package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;

import java.util.*;

/** Validates a complete candidate before atomically exposing it. Resource/network wiring is separate. */
public final class GeometryCatalog {
    public record Snapshot(long revision,Map<String,ModelGeometry> models) {
        public Snapshot {models=Collections.unmodifiableMap(new TreeMap<>(models));}
    }
    private volatile Snapshot current=new Snapshot(0,Map.of());
    public Snapshot snapshot(){return current;}

    public synchronized Snapshot replace(Map<String,ModelGeometry> candidate,Collection<String> referencedModels) {
        if(candidate.size()>4096)throw new IllegalArgumentException("Too many catalog models");
        Map<String,ModelGeometry> validated=new TreeMap<>();
        long pieces=0;
        for(var e:candidate.entrySet()) {
            if(e.getKey()==null || !e.getKey().matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))throw new IllegalArgumentException("Invalid model resource identifier");
            var g=Objects.requireNonNull(e.getValue(),"Missing geometry");
            // Force hierarchy composition and convex validation, including accumulated transforms.
            g.evaluate(ModelGeometry.matrix(g.modelTransform()),Map.of(),new AnatomyFilter(0,0,0));
            pieces+=g.pieces().size();
            if(pieces>262144)throw new IllegalArgumentException("Catalog complexity limit exceeded");
            validated.put(e.getKey(),g);
        }
        for(String reference:referencedModels)if(!validated.containsKey(reference))
            throw new IllegalArgumentException("Missing referenced model: "+reference);
        Snapshot next=new Snapshot(Math.incrementExact(current.revision()),validated);
        current=next;
        return next;
    }
}
