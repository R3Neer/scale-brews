package io.github.r3neer.scalebrews.collision.geometry;

import java.util.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/** Affine image of a box. Rotation and shear retain the actual convex volume. */
public record ConvexBox(List<Vec3> vertices) {
    private static final double EPS=1e-9;
    public ConvexBox {
        if(vertices==null || vertices.size()!=8 || vertices.stream().anyMatch(v->v==null || !Double.isFinite(v.lengthSqr())))
            throw new IllegalArgumentException("Eight finite vertices required");
        vertices=List.copyOf(vertices);
        Vec3 x=vertices.get(1).subtract(vertices.get(0)),y=vertices.get(2).subtract(vertices.get(0)),z=vertices.get(4).subtract(vertices.get(0));
        double determinant=x.dot(y.cross(z));
        if(!Double.isFinite(determinant) || Math.abs(determinant)<1e-15)
            throw new IllegalArgumentException("Degenerate or numerically unbounded box");
        for(Vec3 normal:List.of(x.cross(y),y.cross(z),z.cross(x))) {
            double square=normal.lengthSqr();
            if(!Double.isFinite(square) || square<Double.MIN_NORMAL)
                throw new IllegalArgumentException("Numerically invalid face plane");
        }
        for(int i=0;i<8;i++) if(vertices.get(0).add(x.scale(i&1)).add(y.scale((i>>1)&1)).add(z.scale((i>>2)&1)).distanceToSqr(vertices.get(i))>1e-10)
            throw new IllegalArgumentException("Non-affine vertices");
    }
    /** Reject invalid physical bounds before SAT or any temporal early exit. */
    public static void requireBounds(AABB b) {
        if(b==null || !Double.isFinite(b.minX) || !Double.isFinite(b.minY) || !Double.isFinite(b.minZ)
                || !Double.isFinite(b.maxX) || !Double.isFinite(b.maxY) || !Double.isFinite(b.maxZ)
                || !(b.maxX>b.minX) || !(b.maxY>b.minY) || !(b.maxZ>b.minZ)
                || !Double.isFinite(b.getXsize()+b.getYsize()+b.getZsize())
                || !Double.isFinite(b.getCenter().lengthSqr()))throw new IllegalArgumentException("Invalid physical bounds");
    }
    private static Vec3 requireVector(Vec3 v) {
        if(v==null || !Double.isFinite(v.lengthSqr()))throw new IllegalArgumentException("Invalid geometry vector");
        return v;
    }
    public static ConvexBox of(AABB b,Matrix4fc m) {
        requireBounds(b);
        if(m==null || Math.abs(m.m03())>1e-6 || Math.abs(m.m13())>1e-6 || Math.abs(m.m23())>1e-6 || Math.abs(m.m33()-1)>1e-6)
            throw new IllegalArgumentException("Non-affine box transform");
        for(float value:m.get(new float[16]))if(!Float.isFinite(value))throw new IllegalArgumentException("Invalid box transform");
        List<Vec3> points=new ArrayList<>();
        for(int i=0;i<8;i++) {
            Vector3f v=m.transformPosition((float)((i&1)==0?b.minX:b.maxX),(float)((i&2)==0?b.minY:b.maxY),(float)((i&4)==0?b.minZ:b.maxZ),new Vector3f());
            points.add(new Vec3(v.x,v.y,v.z));
        }
        return new ConvexBox(points);
    }
    public AABB bounds() {
        double x=Double.POSITIVE_INFINITY,y=x,z=x,X=-x,Y=-x,Z=-x;
        for(Vec3 p:vertices) {x=Math.min(x,p.x);y=Math.min(y,p.y);z=Math.min(z,p.z);X=Math.max(X,p.x);Y=Math.max(Y,p.y);Z=Math.max(Z,p.z);}
        return new AABB(x,y,z,X,Y,Z);
    }
    public ConvexBox move(Vec3 delta) {requireVector(delta);return new ConvexBox(vertices.stream().map(v->v.add(delta)).toList());}
    public Vec3 point(Vec3 local) {
        requireVector(local);
        Vec3 origin=vertices.getFirst();
        return requireVector(origin.add(vertices.get(1).subtract(origin).scale(local.x)).add(vertices.get(2).subtract(origin).scale(local.y)).add(vertices.get(4).subtract(origin).scale(local.z)));
    }
    public Vec3 coordinates(Vec3 world) {
        requireVector(world);
        Vec3 origin=vertices.getFirst(),x=vertices.get(1).subtract(origin),y=vertices.get(2).subtract(origin),z=vertices.get(4).subtract(origin),v=world.subtract(origin);
        double determinant=x.dot(y.cross(z));
        return requireVector(new Vec3(v.dot(y.cross(z))/determinant,v.dot(z.cross(x))/determinant,v.dot(x.cross(y))/determinant));
    }
    /** Faces: low/high X, low/high Y, low/high Z in immutable model-local coordinates. */
    public Vec3 faceNormal(int face) {
        if(face<0 || face>5)throw new IllegalArgumentException("Invalid face");
        Vec3 o=vertices.getFirst();var edges=List.of(vertices.get(1).subtract(o),vertices.get(2).subtract(o),vertices.get(4).subtract(o));
        int axis=face/2;Vec3 n=edges.get((axis+1)%3).cross(edges.get((axis+2)%3));
        if(n.dot(edges.get(axis))<0)n=n.scale(-1);
        return n.scale((face%2==0?-1:1)/Math.sqrt(n.lengthSqr()));
    }
    public int closestFace(Vec3 normal) {
        requireVector(normal);
        if(normal.lengthSqr()==0)throw new IllegalArgumentException("Missing normal direction");
        int best=0;double score=Double.NEGATIVE_INFINITY;
        for(int face=0;face<6;face++){double dot=faceNormal(face).dot(normal);if(dot>score){score=dot;best=face;}}
        return best;
    }
    public Vec3 facePoint(int face,Vec3 world) {
        if(face<0 || face>5)throw new IllegalArgumentException("Invalid face");
        Vec3 p=coordinates(world);double[] c={Math.clamp(p.x,0,1),Math.clamp(p.y,0,1),Math.clamp(p.z,0,1)};
        c[face/2]=face%2;return new Vec3(c[0],c[1],c[2]);
    }
    public record RayHit(double fraction,int face,Vec3 localPoint,Vec3 normal) {}
    public RayHit raycast(Vec3 start,Vec3 end) {
        Vec3 a=coordinates(start),d=coordinates(end).subtract(a);
        double[] origin={a.x,a.y,a.z},delta={d.x,d.y,d.z};double enter=Double.NEGATIVE_INFINITY,exit=1;int face=-1;
        for(int axis=0;axis<3;axis++) {
            if(Math.abs(delta[axis])<1e-12){if(origin[axis]<0 || origin[axis]>1)return null;continue;}
            double low=-origin[axis]/delta[axis],high=(1-origin[axis])/delta[axis];
            double near=Math.min(low,high),far=Math.max(low,high);
            if(near>enter){enter=near;face=axis*2+(delta[axis]>0?0:1);}exit=Math.min(exit,far);
            if(enter>exit)return null;
        }
        if(face<0 || enter<0 || enter>1)return null;
        return new RayHit(enter,face,a.add(d.scale(enter)),faceNormal(face));
    }
    private List<Vec3> axes() {
        Vec3 x=vertices.get(1).subtract(vertices.get(0)),y=vertices.get(2).subtract(vertices.get(0)),z=vertices.get(4).subtract(vertices.get(0));
        List<Vec3> world=List.of(new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1));
        List<Vec3> result=new ArrayList<>(world);result.addAll(List.of(x.cross(y),y.cross(z),z.cross(x)));
        for(Vec3 edge:List.of(x,y,z))for(Vec3 axis:world)result.add(edge.cross(axis));
        // Direction is independent of magnitude. Scale by the largest component
        // before normalization: neither squared underflow nor a subnormal reciprocal
        // may discard a separating plane or create NaN escape candidates.
        List<Vec3> normalized=new ArrayList<>();
        for(Vec3 axis:result) {
            double largest=Math.max(Math.abs(axis.x),Math.max(Math.abs(axis.y),Math.abs(axis.z)));
            if(!Double.isFinite(largest))throw new IllegalArgumentException("Unbounded SAT axis");
            if(largest>0) {
                var scaled=new Vec3(axis.x/largest,axis.y/largest,axis.z/largest);
                normalized.add(scaled.scale(1/Math.sqrt(scaled.lengthSqr())));
            }
        }
        return List.copyOf(normalized);
    }
    private double[] interval(Vec3 axis) {
        double lo=Double.POSITIVE_INFINITY,hi=-lo;
        for(Vec3 p:vertices){double d=p.dot(axis);lo=Math.min(lo,d);hi=Math.max(hi,d);}return new double[]{lo,hi};
    }
    private static double[] interval(AABB b,Vec3 a) {
        double c=b.getCenter().dot(a),r=(b.getXsize()*Math.abs(a.x)+b.getYsize()*Math.abs(a.y)+b.getZsize()*Math.abs(a.z))*.5;
        return new double[]{c-r,c+r};
    }
    public boolean overlaps(AABB body) {
        requireBounds(body);
        for(Vec3 axis:axes()){double[] a=interval(axis),b=interval(body,axis);if(b[1]<=a[0]+EPS || b[0]>=a[1]-EPS)return false;}return true;
    }
    public record Separation(double gap,Vec3 normal) {}
    /** Candidate minimum translations along SAT axes; includes both exits for blocked-side recovery. */
    public List<Vec3> escapeVectors(AABB body) {
        if(!overlaps(body))return List.of();
        List<Vec3> result=new ArrayList<>();
        for(Vec3 axis:axes()) {
            double[] a=interval(axis),b=interval(body,axis);
            result.add(axis.scale(a[1]-b[0]+1e-6));result.add(axis.scale(a[0]-b[1]-1e-6));
        }
        result.sort(Comparator.comparingDouble(Vec3::lengthSqr));return List.copyOf(result);
    }
    /** Maximum separating-plane gap; a conservative lower bound on Euclidean distance. */
    public Separation separation(AABB body) {
        requireBounds(body);
        double gap=Double.NEGATIVE_INFINITY;Vec3 normal=Vec3.ZERO;
        for(Vec3 axis:axes()) {
            double[] a=interval(axis),b=interval(body,axis);
            double positive=b[0]-a[1],negative=a[0]-b[1];
            if(positive>gap){gap=positive;normal=axis;}
            if(negative>gap){gap=negative;normal=axis.scale(-1);}
        }
        return new Separation(gap,normal);
    }
    public record Hit(double fraction,Vec3 normal,boolean penetrating) {}
    /** Exact translating-AABB sweep. Temporal rotation/deformation is NOT implemented here. */
    public Hit sweep(AABB body,Vec3 movement) {
        requireVector(movement);
        if(overlaps(body))return new Hit(0,Vec3.ZERO,true);
        double enter=Double.NEGATIVE_INFINITY,exit=Double.POSITIVE_INFINITY;Vec3 normal=Vec3.ZERO;
        for(Vec3 axis:axes()) {
            double[] a=interval(axis),b=interval(body,axis);double speed=movement.dot(axis);
            if(Math.abs(speed)<EPS){if(b[1]<=a[0]+EPS || b[0]>=a[1]-EPS)return null;continue;}
            double t1=(a[0]-b[1])/speed,t2=(a[1]-b[0])/speed,near=Math.min(t1,t2),far=Math.max(t1,t2);
            if(near>enter){enter=near;normal=axis.scale(speed>0?-1:1);}exit=Math.min(exit,far);
            if(enter>exit+EPS)return null;
        }
        if(enter < -EPS || enter>1+EPS || exit<0 || normal.lengthSqr()<EPS)return null;
        return new Hit(Math.clamp(enter,0,1),normal,false);
    }
}
