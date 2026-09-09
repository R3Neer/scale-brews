package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.*;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Alex's Mobs Continued 2.1.9 ordinary adult walking only. No render dependencies. */
public final class GrizzlyPose implements PoseProvider {
    public Optional<Map<String,Matrix4f>> evaluate(ModelGeometry geometry,Inputs in) {
        if(!in.ordinary() || !geometry.source().equals("alexsmobs:grizzly_bear") || !geometry.version().equals("2.1.9"))return Optional.empty();
        float phase=in.walkPhase()*.7f,amount=in.walkAmount(),walk=Mth.cos(phase)*.7f*amount;
        float bob=(float)(Math.sin(phase)*amount*.7f-amount*.7f);
        Map<String,Matrix4f> result=new LinkedHashMap<>();
        for(var p:geometry.parts()) {
            String name=p.id().substring(p.id().lastIndexOf('/')+1);
            float x=0,y=0,z=0,dy=0;
            switch(name) {
                case "left_leg" -> {x=walk;dy=2*bob/16;}
                case "right_leg","left_arm" -> x=-walk;
                case "right_arm" -> x=walk;
                case "midbody" -> z=Mth.cos(phase+1)*(.7f*.2f)*amount;
                case "body" -> {z=Mth.cos(phase+2)*(.7f*.2f)*amount;dy=(float)-Math.abs(Math.sin(phase)*amount*.7f)/16;}
                case "head" -> {x=in.headPitch()*((float)Math.PI/180);y=in.headYaw()*((float)Math.PI/180);z=Mth.cos(phase+2)*(.7f*-.1f)*amount;}
                default -> {continue;}
            }
            var rest=ModelGeometry.matrix(p.transform());
            var pos=rest.getTranslation(new Vector3f());pos.y+=dy;
            result.put(p.id(),new Matrix4f().translation(pos).rotateZYX(z,y,x).scale(rest.getScale(new Vector3f())));
        }
        return result.size()==7?Optional.of(Collections.unmodifiableMap(result)):Optional.empty();
    }
}
