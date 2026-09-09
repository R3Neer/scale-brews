package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.*;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Standing, empty-handed 26.2 player walk/head/breathing. Other states are explicitly unsupported. */
public final class PlayerWalkingPose implements PoseProvider {
    public Optional<Map<String,Matrix4f>> evaluate(ModelGeometry geometry,Inputs in) {
        if(!in.ordinary() || !geometry.version().equals("26.2") || !Set.of("minecraft:player_wide","minecraft:player_slim").contains(geometry.source()))return Optional.empty();
        float phase=in.walkPhase()*.6662f;
        float a=Mth.cos(phase),b=Mth.cos(phase+(float)Math.PI);
        float bobX=Mth.sin(in.age()*.067f)*.05f,bobZ=Mth.cos(in.age()*.09f)*.05f+.05f;
        Map<String,Matrix4f> result=new LinkedHashMap<>();
        for(var p:geometry.parts()) {
            String name=p.id().substring(p.id().lastIndexOf('/')+1);
            float x,y=0,z=0;
            switch(name) {
                case "head" -> {x=in.headPitch()*(float)(Math.PI/180);y=in.headYaw()*(float)(Math.PI/180);}
                case "right_arm" -> {x=b*2*in.walkAmount()*.5f+bobX;z=bobZ;}
                case "left_arm" -> {x=a*2*in.walkAmount()*.5f-bobX;z=-bobZ;}
                case "right_leg" -> {x=a*1.4f*in.walkAmount();y=z=.005f;}
                case "left_leg" -> {x=b*1.4f*in.walkAmount();y=z=-.005f;}
                default -> {continue;}
            }
            var rest=ModelGeometry.matrix(p.transform());
            result.put(p.id(),new Matrix4f().translation(rest.getTranslation(new Vector3f())).rotateZYX(z,y,x).scale(rest.getScale(new Vector3f())));
        }
        return result.size()==5?Optional.of(Collections.unmodifiableMap(result)):Optional.empty();
    }
}
