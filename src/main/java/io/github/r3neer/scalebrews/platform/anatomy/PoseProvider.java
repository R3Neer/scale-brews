package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.Map;
import java.util.Optional;
import org.joml.Matrix4f;

/** Deterministic pose evaluation. Inputs must eventually be sampled/synchronized by the server. */
public interface PoseProvider {
    record Inputs(float walkPhase,float walkAmount,float age,float headYaw,float headPitch,boolean ordinary,Map<String,Float> channels) {
        public Inputs(float walkPhase,float walkAmount,float age,float headYaw,float headPitch,boolean ordinary) {
            this(walkPhase,walkAmount,age,headYaw,headPitch,ordinary,Map.of());
        }
        public Inputs {
            if(!Float.isFinite(walkPhase+walkAmount+age+headYaw+headPitch) || walkAmount<0)
                throw new IllegalArgumentException("Invalid pose inputs");
            if(channels==null || channels.size()>64)throw new IllegalArgumentException("Invalid pose channels");
            var copy=new java.util.TreeMap<String,Float>();
            channels.forEach((name,value)->{
                if(name==null || !name.matches("[a-z0-9_.-]{1,64}") || value==null || !Float.isFinite(value))
                    throw new IllegalArgumentException("Invalid pose channel");
                copy.put(name,value);
            });
            channels=java.util.Collections.unmodifiableMap(copy);
        }
        public float channel(String name,float fallback){return channels.getOrDefault(name,fallback);}
        public boolean flag(String name){return channel(name,0)>0.5f;}
    }
    /** Empty means unsupported, never silently return a frozen anatomy pose. */
    Optional<Map<String,Matrix4f>> evaluate(ModelGeometry geometry,Inputs inputs);
}
