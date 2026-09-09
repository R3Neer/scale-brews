package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.*;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Minecraft 26.2 ordinary quadruped walk/head channels, without client classes. */
public final class QuadrupedPose implements PoseProvider {
    public Optional<Map<String,Matrix4f>> evaluate(ModelGeometry geometry,Inputs in) {
        if(!in.ordinary())return Optional.empty();
        Map<String,Matrix4f> result=new LinkedHashMap<>();
        float phase=in.walkPhase()*.6662F,a=Mth.cos(phase)*1.4F*in.walkAmount(),b=Mth.cos(phase+(float)Math.PI)*1.4F*in.walkAmount();
        for(var p:geometry.parts()) {
            String name=p.id().substring(p.id().lastIndexOf('/')+1);
            float x,y=0;
            switch(name) {
                case "head" -> {x=in.headPitch()*(float)(Math.PI/180);y=in.headYaw()*(float)(Math.PI/180);}
                case "right_hind_leg","left_front_leg" -> x=a;
                case "left_hind_leg","right_front_leg" -> x=b;
                default -> {continue;}
            }
            Matrix4f rest=ModelGeometry.matrix(p.transform());Vector3f pos=rest.getTranslation(new Vector3f()),scale=rest.getScale(new Vector3f());
            result.put(p.id(),new Matrix4f().translation(pos).rotateZYX(0,y,x).scale(scale));
        }
        return result.size()==5?Optional.of(Collections.unmodifiableMap(result)):Optional.empty();
    }
}
