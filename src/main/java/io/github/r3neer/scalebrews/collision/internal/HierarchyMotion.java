package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;

import java.util.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Quaternionf;

/** Verified TRS interpolation between authoritative poses; static affine/sheared nodes are retained. */
public final class HierarchyMotion {
    /** Immutable affine values; {@link #matrix()} always returns a fresh mutable view. */
    public record AffineTransform(List<Float> values) {
        public AffineTransform {
            values=List.copyOf(values);
            if(values.size()!=16 || values.stream().anyMatch(v->v==null || !Float.isFinite(v)))
                throw new IllegalArgumentException("Invalid affine transform");
            var matrix=ModelGeometry.matrix(values);
            if(Math.abs(matrix.m03())>1e-6 || Math.abs(matrix.m13())>1e-6 || Math.abs(matrix.m23())>1e-6 || Math.abs(matrix.m33()-1)>1e-6
                    || !Float.isFinite(matrix.determinant()) || Math.abs(matrix.determinant())<1e-12)
                throw new IllegalArgumentException("Non-affine/degenerate transform");
        }
        public Matrix4f matrix(){return ModelGeometry.matrix(values);}
        private static AffineTransform copyOf(Matrix4f matrix){return new AffineTransform(ModelGeometry.values(matrix));}
    }
    /** One immutable interpolation sample, shared by convex physics and original-model presentation. */
    public record EvaluatedFrame(double fraction,Vec3 origin,AffineTransform rootTrs,AffineTransform modelTransform,
            Map<String,AffineTransform> localPartTransforms,Map<String,ConvexBox> pieces) {
        public EvaluatedFrame {
            if(!Double.isFinite(fraction) || fraction<0 || fraction>1 || origin==null || !Double.isFinite(origin.lengthSqr()) || rootTrs==null || modelTransform==null
                    || localPartTransforms==null || pieces==null || localPartTransforms.entrySet().stream().anyMatch(e->e.getKey()==null || e.getValue()==null)
                    || pieces.entrySet().stream().anyMatch(e->e.getKey()==null || e.getValue()==null))
                throw new IllegalArgumentException("Invalid evaluated frame");
            localPartTransforms=Collections.unmodifiableMap(new LinkedHashMap<>(localPartTransforms));
            pieces=Collections.unmodifiableMap(new LinkedHashMap<>(pieces));
        }
    }
    private record Trs(Vector3f translation,Quaternionf rotation,Vector3f scale) {}
    private static final class Node {
        final Matrix4f fixed;
        final Trs a,b;
        final double norm,derivative,translation,translationSpeed;
        Node(Matrix4f start,Matrix4f end) {
            for(var m:List.of(start,end)) {
                for(float value:m.get(new float[16]))if(!Float.isFinite(value))throw new IllegalArgumentException("Non-finite motion matrix");
                if(Math.abs(m.m03())>1e-6 || Math.abs(m.m13())>1e-6 || Math.abs(m.m23())>1e-6 || Math.abs(m.m33()-1)>1e-6
                    || !Float.isFinite(m.determinant3x3()) || Math.abs(m.determinant3x3())<1e-12)throw new IllegalArgumentException("Non-affine/degenerate motion matrix");
            }
            if(start.equals(end)) {
                fixed=new Matrix4f(start);a=b=null;
                // Frobenius norm safely bounds the operator norm, including shear.
                norm=Math.sqrt(start.m00()*start.m00()+start.m01()*start.m01()+start.m02()*start.m02()
                    +start.m10()*start.m10()+start.m11()*start.m11()+start.m12()*start.m12()
                    +start.m20()*start.m20()+start.m21()*start.m21()+start.m22()*start.m22());
                derivative=translationSpeed=0;translation=start.getTranslation(new Vector3f()).length();
            } else {
                fixed=null;a=decompose(start);b=decompose(end);
                if(a.scale.x*b.scale.x<=0 || a.scale.y*b.scale.y<=0 || a.scale.z*b.scale.z<=0)
                    throw new IllegalArgumentException("Scale crosses a degenerate transform");
                norm=Math.max(maxAbs(a.scale),maxAbs(b.scale));
                double angle=2*Math.acos(Math.clamp(Math.abs(a.rotation.dot(b.rotation)),0,1));
                derivative=angle*norm+maxAbs(new Vector3f(b.scale).sub(a.scale));
                translation=Math.max(a.translation.length(),b.translation.length());
                translationSpeed=a.translation.distance(b.translation);
            }
        }
        Matrix4f at(double t) {
            if(fixed!=null)return new Matrix4f(fixed);
            return new Matrix4f().translationRotateScale(new Vector3f(a.translation).lerp(b.translation,(float)t),
                new Quaternionf(a.rotation).slerp(b.rotation,(float)t),new Vector3f(a.scale).lerp(b.scale,(float)t));
        }
        /** Pull back a direction only if its scalar projection is constant for the full node motion. */
        Vector3f invariantPullback(Vector3f normal) {
            if(fixed!=null)return new Vector3f(
                fixed.m00()*normal.x+fixed.m01()*normal.y+fixed.m02()*normal.z,
                fixed.m10()*normal.x+fixed.m11()*normal.y+fixed.m12()*normal.z,
                fixed.m20()*normal.x+fixed.m21()*normal.y+fixed.m22()*normal.z);
            if(!a.scale.equals(b.scale))return null;
            int axis=normal.x!=0 && normal.y==0 && normal.z==0?0:normal.y!=0 && normal.x==0 && normal.z==0?1:normal.z!=0 && normal.x==0 && normal.y==0?2:-1;
            if(axis<0)return null; // No tolerance-based almost-invariant certificates.
            if(a.translation.get(axis)!=b.translation.get(axis))return null;
            var relative=new Quaternionf(b.rotation).mul(new Quaternionf(a.rotation).conjugate());
            if(axis!=0 && relative.x!=0 || axis!=1 && relative.y!=0 || axis!=2 && relative.z!=0)return null;
            return new Vector3f(normal).rotate(new Quaternionf(a.rotation).conjugate()).mul(a.scale);
        }
        private static double maxAbs(Vector3f v){return Math.max(Math.abs(v.x),Math.max(Math.abs(v.y),Math.abs(v.z)));}
        private static Trs decompose(Matrix4f m) {
            Vector3f s=m.getScale(new Vector3f());
            if(m.determinant3x3()<0)s.z=-s.z;
            if(Math.min(Math.abs(s.x),Math.min(Math.abs(s.y),Math.abs(s.z)))<1e-8)throw new IllegalArgumentException("Degenerate animated node");
            Matrix4f rotation=new Matrix4f(m).setTranslation(0,0,0).scale(1/s.x,1/s.y,1/s.z);
            Quaternionf q=rotation.getNormalizedRotation(new Quaternionf()).normalize();
            Vector3f p=m.getTranslation(new Vector3f());
            Matrix4f rebuilt=new Matrix4f().translationRotateScale(p,q,s);
            if(!rebuilt.equals(m,1e-5f))throw new IllegalArgumentException("Animated shear requires a dedicated motion provider");
            return new Trs(p,q,s);
        }
    }
    private final ModelGeometry model;
    private final Node root,staticModel;
    private final Map<String,Node> localNodes;
    private final Map<String,List<Node>> paths;
    private final Map<String,String> decisions;
    private final Vec3 originBefore,originAfter;
    private final Map<String,ConservativeSweep.Motion> pieces;
    private long frameEvaluations;
    public Map<String,ConservativeSweep.Motion> pieces(){return pieces;}
    /** Frame samples use cached joint endpoints; this count never measures pose-provider evaluation. */
    public long frameEvaluations(){return frameEvaluations;}
    /**
     * Legacy constructor: roots are already fully composed, including any static
     * model transform. New callers with root TRS must use {@link #withRootTrs}.
     */
    public HierarchyMotion(ModelGeometry model,Map<String,Matrix4f> before,Map<String,Matrix4f> after,
        Matrix4f rootBefore,Matrix4f rootAfter,Vec3 originBefore,Vec3 originAfter,AnatomyFilter filter) {
        this(model,before,after,rootBefore,rootAfter,new Matrix4f(),originBefore,originAfter,filter);
    }
    /** Dynamic root TRS plus the model's fixed affine transform (which may contain shear). */
    public static HierarchyMotion withRootTrs(ModelGeometry model,Map<String,Matrix4f> before,Map<String,Matrix4f> after,
            Matrix4f rootBefore,Matrix4f rootAfter,Vec3 originBefore,Vec3 originAfter,AnatomyFilter filter) {
        return new HierarchyMotion(model,before,after,rootBefore,rootAfter,ModelGeometry.matrix(model.modelTransform()),originBefore,originAfter,filter);
    }
    private HierarchyMotion(ModelGeometry model,Map<String,Matrix4f> before,Map<String,Matrix4f> after,
            Matrix4f rootBefore,Matrix4f rootAfter,Matrix4f modelTransform,Vec3 originBefore,Vec3 originAfter,AnatomyFilter filter) {
        this.model=Objects.requireNonNull(model);this.originBefore=Objects.requireNonNull(originBefore);this.originAfter=Objects.requireNonNull(originAfter);
        this.root=new Node(rootBefore,rootAfter);this.staticModel=new Node(modelTransform,modelTransform);
        boolean includeStaticModel=!modelTransform.equals(new Matrix4f());
        Map<String,List<Node>> builtPaths=new LinkedHashMap<>();Map<String,Node> builtLocals=new LinkedHashMap<>();
        for(var part:model.parts()) {
            List<Node> path=new ArrayList<>(part.parent()==null?(includeStaticModel?List.of(this.root,this.staticModel):List.of(this.root)):builtPaths.get(part.parent()));
            var rest=ModelGeometry.matrix(part.transform());
            var node=new Node(before.getOrDefault(part.id(),rest),after.getOrDefault(part.id(),rest));path.add(node);
            builtLocals.put(part.id(),node);builtPaths.put(part.id(),List.copyOf(path));
        }
        this.paths=Collections.unmodifiableMap(builtPaths);this.localNodes=Collections.unmodifiableMap(builtLocals);
        this.decisions=model.filterReport(filter);Map<String,ConservativeSweep.Motion> result=new LinkedHashMap<>();
        for(var piece:model.pieces())if("retained".equals(decisions.get(piece.id()))) {
            var path=this.paths.get(piece.part());var box=piece.box();
            double radius=new Vec3(Math.max(Math.abs(box.minX),Math.abs(box.maxX)),Math.max(Math.abs(box.minY),Math.abs(box.maxY)),Math.max(Math.abs(box.minZ),Math.abs(box.maxZ))).length();
            double speed=0;
            for(int i=path.size()-1;i>=0;i--) {
                var node=path.get(i);
                speed=node.translationSpeed+node.derivative*radius+node.norm*speed;
                radius=node.translation+node.norm*radius;
            }
            // Small safety margin for float matrix/quaternion evaluation.
            double bound=speed==0?0:speed*1.0001+1e-5;
            java.util.function.DoubleFunction<ConvexBox> trajectory=t->{
                if(!Double.isFinite(t) || t<0 || t>1)throw new IllegalArgumentException("Invalid motion time");
                Matrix4f transform=new Matrix4f();for(var node:path)transform.mul(node.at(t));
                return ConvexBox.of(box,transform).move(this.originBefore.lerp(this.originAfter,t));
            };
            List<ConservativeSweep.Plane> planes=new ArrayList<>();
            var initial=trajectory.apply(0).bounds();
            for(var direction:net.minecraft.core.Direction.values()) {
                Vector3f normal=new Vector3f(direction.getStepX(),direction.getStepY(),direction.getStepZ());boolean invariant=true;
                for(var node:path){normal=node.invariantPullback(normal);if(normal==null){invariant=false;break;}}
                if(invariant)planes.add(new ConservativeSweep.Plane(direction,direction.getAxisDirection()==net.minecraft.core.Direction.AxisDirection.POSITIVE?initial.max(direction.getAxis()):-initial.min(direction.getAxis())));
            }
            result.put(piece.id(),new ConservativeSweep.Motion(trajectory,bound,originAfter.subtract(originBefore),planes));
        }
        pieces=Collections.unmodifiableMap(result);
    }
    /**
     * Evaluates each local node once. This is intentionally separate from
     * {@link #pieces()}: CCD keeps its bounded per-piece path instead of calling
     * this whole-model sampler for every material query.
     */
    public EvaluatedFrame evaluate(double fraction) {
        if(!Double.isFinite(fraction) || fraction<0 || fraction>1)throw new IllegalArgumentException("Invalid motion time");
        Matrix4f rootMatrix=root.at(fraction),modelMatrix=staticModel.at(0);
        Map<String,Matrix4f> locals=new LinkedHashMap<>();Map<String,AffineTransform> exposedLocals=new LinkedHashMap<>();
        for(var part:model.parts()) {
            Matrix4f local=localNodes.get(part.id()).at(fraction);
            locals.put(part.id(),local);exposedLocals.put(part.id(),AffineTransform.copyOf(local));
        }
        Map<String,Matrix4f> world=new LinkedHashMap<>();
        for(var part:model.parts()) {
            Matrix4f parent=part.parent()==null?new Matrix4f(rootMatrix).mul(modelMatrix):new Matrix4f(world.get(part.parent()));
            world.put(part.id(),parent.mul(locals.get(part.id())));
        }
        Vec3 origin=originBefore.lerp(originAfter,fraction);Map<String,ConvexBox> evaluatedPieces=new LinkedHashMap<>();
        for(var piece:model.pieces())if("retained".equals(decisions.get(piece.id())))
            evaluatedPieces.put(piece.id(),ConvexBox.of(piece.box(),world.get(piece.part())).move(origin));
        frameEvaluations++;
        return new EvaluatedFrame(fraction,origin,AffineTransform.copyOf(rootMatrix),AffineTransform.copyOf(modelMatrix),exposedLocals,evaluatedPieces);
    }
}
