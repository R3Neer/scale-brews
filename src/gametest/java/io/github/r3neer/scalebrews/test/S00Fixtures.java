package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.geometry.*;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;

/** Real Minecraft/JOML fixtures; no dependency stubs or alternative geometry solver. */
final class S00Fixtures {
    static final AABB UNIT=new AABB(0,0,0,1,1,1);
    static final UUID EPOCH=UUID.fromString("00000000-0000-0000-0000-000000000100");
    static final UUID SUPPORT=UUID.fromString("00000000-0000-0000-0000-000000000200");
    static final Identifier MODEL=Identifier.parse("test:s00"),STATIC=Identifier.parse("scalebrews:static");
    private S00Fixtures() {}
    static ModelGeometry model() {
        return new ModelGeometry(2,"test:s00","1",List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("piece","root",List.of(0d,0d,0d),List.of(1d,1d,1d),null)),ModelGeometry.values(new Matrix4f()));
    }
    static ConvexBox box(AABB bounds){return ConvexBox.of(bounds,new Matrix4f());}
    static GeometryProvider.GeometryIdentity identity(){return new GeometryProvider.GeometryIdentity(Level.OVERWORLD,SUPPORT,1,EPOCH,1,MODEL,STATIC,1,1);}
    static GeometryProvider.QueryFrame frame(long serial,long tick,long rootSequence,double x) {
        var inputs=new PoseProvider.Inputs(0,0,0,0,0,true);
        var root=new AnatomyMovement.RootFrame(rootSequence,tick,new Vec3(x,0,0),0,1,GravityFrame.VANILLA);
        var sample=new AnatomyPoseHistory.Sample(inputs,root.origin(),root.yaw(),root.scale(),root.gravity());
        var endpoint=new GeometryProvider.CausalEndpoint(serial,tick,tick,root,sample,GeometryProvider.Availability.AVAILABLE);
        return new GeometryProvider.QueryFrame(identity(),endpoint,new GeometryProvider.Snapshot(1,Map.of("piece",box(UNIT).move(root.origin()))));
    }
    static MaterialEventDispatcher.MaterialInterval interval() {
        return new MaterialEventDispatcher.MaterialInterval(new GeometryProvider.MotionIntervalHandle(identity(),1,frame(1,0,0,0),frame(2,1,1,.2)),new AABB(0,0,0,1.2,1,1));
    }
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static Throwable thrown(Runnable action) {try{action.run();}catch(Throwable failure){return failure;}throw new AssertionError("Expected failure, but operation completed");}
    static void rejects(Runnable action){var failure=thrown(action);check(failure instanceof IllegalArgumentException,"Invalid input must be rejected explicitly: "+failure);}
    static void near(Vec3 actual,Vec3 expected,double epsilon,String message){check(actual!=null && Double.isFinite(actual.lengthSqr()) && actual.distanceTo(expected)<=epsilon,message+": "+actual+" != "+expected);}
}
