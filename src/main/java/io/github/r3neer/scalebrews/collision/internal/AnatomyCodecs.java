package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;

import com.google.gson.Gson;
import com.mojang.serialization.*;

/** The export JSON is also the registry representation; all constructor validation is retained. */
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
    private static final Codec<Double> FINITE=Codec.DOUBLE.validate(value->Double.isFinite(value)
        ?DataResult.success(value):DataResult.error(()->"Expected finite filter value"));
    private static final Codec<java.util.Set<String>> IDS=Codec.STRING.validate(id->!id.isBlank() && id.length()<=256
        ?DataResult.success(id):DataResult.error(()->"Invalid piece identifier"))
        .listOf(0,4096).xmap(java.util.Set::copyOf,java.util.ArrayList::new);
    private record FilterData(double thickness,double aspect,double volume,java.util.Set<String> include,java.util.Set<String> exclude) {}
    public static final Codec<AnatomyFilter> FILTER=com.mojang.serialization.codecs.RecordCodecBuilder.<FilterData>create(i->i.group(
        FINITE.optionalFieldOf("min_thickness",.5/16).forGetter(FilterData::thickness),
        FINITE.optionalFieldOf("min_aspect",.025).forGetter(FilterData::aspect),
        FINITE.optionalFieldOf("min_volume_ratio",.00001).forGetter(FilterData::volume),
        IDS.optionalFieldOf("include",java.util.Set.of()).forGetter(FilterData::include),
        IDS.optionalFieldOf("exclude",java.util.Set.of()).forGetter(FilterData::exclude)
    ).apply(i,FilterData::new)).comapFlatMap(data->{
        try{return DataResult.success(new AnatomyFilter(data.thickness,data.aspect,data.volume,data.include,data.exclude));}
        catch(IllegalArgumentException invalid){return DataResult.error(invalid::getMessage);}
    },filter->new FilterData(filter.minThickness(),filter.minAspect(),filter.minVolumeRatio(),filter.include(),filter.exclude()));
}
