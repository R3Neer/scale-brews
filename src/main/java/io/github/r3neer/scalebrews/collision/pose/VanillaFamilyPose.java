package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;

import java.util.*;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Server-safe Minecraft 26.2 pose formulae for supported ordinary adult model families. */
public final class VanillaFamilyPose implements PoseProvider {
    public enum Family { CHICKEN,VILLAGER,IRON_GOLEM,GHAST,FELINE,EQUINE,BEE }
    private final Family family;
    public VanillaFamilyPose(Family family){this.family=Objects.requireNonNull(family);}

    public Optional<Map<String,Matrix4f>> evaluate(ModelGeometry geometry,Inputs in) {
        if(!in.ordinary())return Optional.empty();
        Map<String,Matrix4f> out=new LinkedHashMap<>();
        switch(family) {
            case CHICKEN->chicken(geometry,in,out);
            case VILLAGER->villager(geometry,in,out);
            case IRON_GOLEM->golem(geometry,in,out);
            case GHAST->ghast(geometry,in,out);
            case FELINE->feline(geometry,in,out);
            case EQUINE->equine(geometry,in,out);
            case BEE->bee(geometry,in,out);
        }
        return out.isEmpty()?Optional.empty():Optional.of(Collections.unmodifiableMap(out));
    }
    private static void chicken(ModelGeometry g,Inputs in,Map<String,Matrix4f> out) {
        float a=Mth.cos(in.walkPhase()*.6662f)*1.4f*in.walkAmount(),b=Mth.cos(in.walkPhase()*.6662f+(float)Math.PI)*1.4f*in.walkAmount();
        float flap=(Mth.sin(in.channel("flap",0))+1)*in.channel("flap_speed",0);
        for(var p:g.parts())switch(name(p)) {
            case "head"->put(out,p,0,0,0,in.headPitch()*Mth.DEG_TO_RAD,in.headYaw()*Mth.DEG_TO_RAD,0);
            case "right_leg"->put(out,p,0,0,0,a,0,0);
            case "left_leg"->put(out,p,0,0,0,b,0,0);
            case "right_wing"->put(out,p,0,0,0,0,0,flap);
            case "left_wing"->put(out,p,0,0,0,0,0,-flap);
        }
    }
    private static void villager(ModelGeometry g,Inputs in,Map<String,Matrix4f> out) {
        float a=Mth.cos(in.walkPhase()*.6662f)*.7f*in.walkAmount(),b=Mth.cos(in.walkPhase()*.6662f+(float)Math.PI)*.7f*in.walkAmount();
        for(var p:g.parts())switch(name(p)) {
            case "head"->{boolean unhappy=in.flag("unhappy");put(out,p,0,0,0,unhappy?.4f:in.headPitch()*Mth.DEG_TO_RAD,in.headYaw()*Mth.DEG_TO_RAD,unhappy?.3f*Mth.sin(.45f*in.age()):0);}
            case "right_leg"->put(out,p,0,0,0,a,0,0);
            case "left_leg"->put(out,p,0,0,0,b,0,0);
        }
    }
    private static void golem(ModelGeometry g,Inputs in,Map<String,Matrix4f> out) {
        float wave=Mth.triangleWave(in.walkPhase(),13),speed=in.walkAmount();
        float attack=in.channel("attack",0),flower=in.channel("flower",0),rightArm,leftArm;
        if(attack>0)rightArm=leftArm=-2+1.5f*Mth.triangleWave(attack,10);
        else if(flower>0){rightArm=-.8f+.025f*Mth.triangleWave(flower,70);leftArm=0;}
        else {rightArm=(-.2f+1.5f*wave)*speed;leftArm=(-.2f-1.5f*wave)*speed;}
        for(var p:g.parts())switch(name(p)) {
            case "head"->put(out,p,0,0,0,in.headPitch()*Mth.DEG_TO_RAD,in.headYaw()*Mth.DEG_TO_RAD,0);
            case "right_arm"->put(out,p,0,0,0,rightArm,0,0);
            case "left_arm"->put(out,p,0,0,0,leftArm,0,0);
            case "right_leg"->put(out,p,0,0,0,-1.5f*wave*speed,0,0);
            case "left_leg"->put(out,p,0,0,0,1.5f*wave*speed,0,0);
        }
    }
    private static void ghast(ModelGeometry g,Inputs in,Map<String,Matrix4f> out) {
        for(var p:g.parts())if(name(p).startsWith("tentacle")) {
            try {int index=Integer.parseInt(name(p).substring("tentacle".length()));put(out,p,0,0,0,.2f*Mth.sin(in.age()*.3f+index)+.4f,0,0);}
            catch(NumberFormatException ignored){}
        }
        // Body is static, but a provider must still positively recognize the model.
        if(out.isEmpty())for(var p:g.parts())if(name(p).equals("body")){putRest(out,p);break;}
    }
    private static void feline(ModelGeometry g,Inputs in,Map<String,Matrix4f> out) {
        boolean crouch=in.flag("crouching"),sprint=in.flag("sprinting");float speed=in.walkAmount(),pos=in.walkPhase();
        float lh,rh,lf,rf,tail2;
        if(sprint){lh=Mth.cos(pos*.6662f)*speed;rh=Mth.cos(pos*.6662f+.3f)*speed;lf=Mth.cos(pos*.6662f+(float)Math.PI+.3f)*speed;rf=Mth.cos(pos*.6662f+(float)Math.PI)*speed;tail2=1.7278761f+(float)Math.PI/10*Mth.cos(pos)*speed;}
        else {lh=Mth.cos(pos*.6662f)*speed;rh=Mth.cos(pos*.6662f+(float)Math.PI)*speed;lf=rh;rf=lh;tail2=1.7278761f+(crouch?.47123894f:(float)Math.PI/4)*Mth.cos(pos)*speed;}
        for(var p:g.parts())switch(name(p)) {
            case "head"->put(out,p,0,crouch?2:0,0,in.headPitch()*Mth.DEG_TO_RAD,in.headYaw()*Mth.DEG_TO_RAD,0);
            case "body"->put(out,p,0,crouch?1:0,0,(float)Math.PI/2,0,0);
            case "tail1"->put(out,p,0,crouch?1:0,0,crouch||sprint?(float)Math.PI/2:.9f,0,0);
            case "tail2"->put(out,p,0,crouch?-4:sprint?-5:0,(crouch||sprint)?2:0,tail2,0,0);
            case "left_hind_leg"->put(out,p,0,0,0,lh,0,0);
            case "right_hind_leg"->put(out,p,0,0,0,rh,0,0);
            case "left_front_leg"->put(out,p,0,0,0,lf,0,0);
            case "right_front_leg"->put(out,p,0,0,0,rf,0,0);
        }
    }
    private static void equine(ModelGeometry g,Inputs in,Map<String,Matrix4f> out) {
        float speed=in.walkAmount(),pos=in.walkPhase(),pitch=in.headPitch()*Mth.DEG_TO_RAD;
        if(speed>.2f)pitch+=Mth.cos(pos*.8f)*.15f*speed;
        float leg=Mth.cos(pos*.6662f+(float)Math.PI),swing=leg*.8f*speed;
        for(var p:g.parts())switch(name(p)) {
            case "head_parts"->put(out,p,0,0,0,(float)Math.PI/6+pitch,Mth.clamp(in.headYaw(),-20,20)*Mth.DEG_TO_RAD,0);
            case "left_hind_leg"->put(out,p,0,0,0,-leg*.5f*speed,0,0);
            case "right_hind_leg"->put(out,p,0,0,0,leg*.5f*speed,0,0);
            case "left_front_leg"->put(out,p,0,0,0,swing,0,0);
            case "right_front_leg"->put(out,p,0,0,0,-swing,0,0);
            case "tail"->put(out,p,0,speed,speed*2,(float)Math.PI/6+speed*.75f,in.flag("tail")?Mth.cos(in.age()*.7f):0,0);
        }
    }
    private static void bee(ModelGeometry g,Inputs in,Map<String,Matrix4f> out) {
        boolean ground=in.flag("on_ground"),angry=in.flag("angry");float speed=Mth.cos(in.age()*.18f),boneX=0,boneY=0;
        if(!angry&&!ground){boneX=.1f+speed*(float)Math.PI*.025f;boneY=-Mth.cos(in.age()*.18f)*.9f;}
        float roll=in.channel("roll",0);if(roll>0)boneX=boneX+roll*(float)Math.atan2(Math.sin(3.0915928f-boneX),Math.cos(3.0915928f-boneX));
        for(var p:g.parts())switch(name(p)) {
            case "bone"->put(out,p,0,boneY,0,boneX,0,0);
            case "left_antenna","right_antenna"->{if(!angry&&!ground)put(out,p,0,0,0,speed*(float)Math.PI*.03f,0,0);}
        }
    }
    private static String name(ModelGeometry.Part p){return p.id().substring(p.id().lastIndexOf('/')+1);}
    private static void putRest(Map<String,Matrix4f> out,ModelGeometry.Part p){out.put(p.id(),ModelGeometry.matrix(p.transform()));}
    /** Position deltas are vanilla model pixels; rotations replace reset-pose rotations. */
    private static void put(Map<String,Matrix4f> out,ModelGeometry.Part p,float dx,float dy,float dz,float x,float y,float z) {
        Matrix4f rest=ModelGeometry.matrix(p.transform());Vector3f translation=rest.getTranslation(new Vector3f()),scale=rest.getScale(new Vector3f());
        out.put(p.id(),new Matrix4f().translation(translation.x+dx/16,translation.y+dy/16,translation.z+dz/16).rotateZYX(z,y,x).scale(scale));
    }
}
