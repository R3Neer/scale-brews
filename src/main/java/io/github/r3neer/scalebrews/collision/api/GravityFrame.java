package io.github.r3neer.scalebrews.collision.api;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Cardinal gravity contract; Gravity Changer remains responsible for orienting the physical box. */
public record GravityFrame(Direction down) {
    public GravityFrame {java.util.Objects.requireNonNull(down);}
    public static final GravityFrame VANILLA=new GravityFrame(Direction.DOWN);
    public Vec3 gravity(){return new Vec3(down.getStepX(),down.getStepY(),down.getStepZ());}
    public Vec3 up(){return gravity().scale(-1);}
    /** Cardinal basis matches Gravity Changer 1.5.2-beta.5's RotationUtil, without a dependency. */
    public Vec3 toWorld(Vec3 local) {
        return switch(down) {
            case DOWN->local;case UP->new Vec3(-local.x,-local.y,local.z);
            case NORTH->new Vec3(local.x,-local.z,local.y);case SOUTH->new Vec3(-local.x,-local.z,-local.y);
            case WEST->new Vec3(local.y,-local.z,-local.x);case EAST->new Vec3(-local.y,-local.z,local.x);
        };
    }
    public Vec3 toLocal(Vec3 world) {
        return new Vec3(world.dot(toWorld(new Vec3(1,0,0))),world.dot(up()),world.dot(toWorld(new Vec3(0,0,1))));
    }
    public org.joml.Matrix4f matrix() {
        var x=toWorld(new Vec3(1,0,0));var y=up();var z=toWorld(new Vec3(0,0,1));
        return new org.joml.Matrix4f().m00((float)x.x).m01((float)x.y).m02((float)x.z)
            .m10((float)y.x).m11((float)y.y).m12((float)y.z).m20((float)z.x).m21((float)z.y).m22((float)z.z);
    }
    public double vertical(Vec3 displacement){return displacement.dot(up());}
    public Vec3 tangent(Vec3 displacement){return displacement.subtract(up().scale(vertical(displacement)));}
    public boolean supports(Vec3 normal){return normal.dot(up())>=Math.sqrt(.5)-1e-7;}
    /** Clinging chooses cardinal gravity only; diagonal ties are deliberately ambiguous. */
    public static java.util.Optional<Direction> dominant(Vec3 normal) {
        if(!Double.isFinite(normal.lengthSqr()) || normal.lengthSqr()<1e-12)return java.util.Optional.empty();
        double x=Math.abs(normal.x),y=Math.abs(normal.y),z=Math.abs(normal.z),max=Math.max(x,Math.max(y,z));
        int ties=(max-x<1e-6?1:0)+(max-y<1e-6?1:0)+(max-z<1e-6?1:0);
        if(ties!=1)return java.util.Optional.empty();
        return java.util.Optional.of(max==x?(normal.x>0?Direction.EAST:Direction.WEST):max==y?(normal.y>0?Direction.UP:Direction.DOWN):(normal.z>0?Direction.SOUTH:Direction.NORTH));
    }
}
