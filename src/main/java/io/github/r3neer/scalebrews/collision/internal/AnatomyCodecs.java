package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.data.CollisionCodecs;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;

import com.google.gson.Gson;
import com.mojang.serialization.*;

/** Transitional geometry codec holder; canonical filter/data codecs live in collision.data. */
public final class AnatomyCodecs {
    private AnatomyCodecs() {}
    private static final Gson JSON=new Gson();
    public static final Codec<ModelGeometry> GEOMETRY=Codec.PASSTHROUGH.comapFlatMap(value->{
        try {
            var json=value.convert(JsonOps.INSTANCE).getValue();
            var geometry=JSON.fromJson(json,ModelGeometry.class);
            if(geometry==null)return DataResult.error(()->"Missing model geometry");
            geometry.evaluate(new org.joml.Matrix4f(),java.util.Map.of(),new AnatomyFilter(0,0,0));
            return DataResult.success(geometry);
        } catch(RuntimeException invalid) {
            return DataResult.error(()->"Invalid anatomical geometry: "+invalid.getMessage());
        }
    },geometry->new Dynamic<>(JsonOps.INSTANCE,JSON.toJsonTree(geometry)));
    public static final Codec<AnatomyFilter> FILTER=CollisionCodecs.FILTER;
}
