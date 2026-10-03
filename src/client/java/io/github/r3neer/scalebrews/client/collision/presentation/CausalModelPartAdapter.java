package io.github.r3neer.scalebrews.client.collision.presentation;

import io.github.r3neer.scalebrews.client.collision.preparation.ModelPartGeometryEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.HierarchyMotion;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Preparation-time visible geometry check and exact, reversible ModelPart pose application. */
public final class CausalModelPartAdapter {
    private static final float MATRIX_EPS=3e-5f;
    private static final double PIECE_EPS=1e-6;

    private record Saved(ModelPart part,PartPose pose,boolean visible,boolean skipDraw) {
        static Saved of(ModelPart part) {
            return new Saved(part,new PartPose(part.x,part.y,part.z,part.xRot,part.yRot,part.zRot,
                part.xScale,part.yScale,part.zScale),part.visible,part.skipDraw);
        }
        void restore(){part.loadPose(pose);part.visible=visible;part.skipDraw=skipDraw;}
    }
    public static final class Prepared {
        private final ModelPart root;
        private final Map<String,ModelPart> parts;
        private final List<ModelPart> materialParts;
        private Prepared(ModelPart root,Map<String,ModelPart> parts,List<ModelPart> materialParts) {
            this.root=root;this.parts=Map.copyOf(parts);this.materialParts=List.copyOf(materialParts);
        }
        public boolean owns(EntityModel<?> model){return model!=null && model.root()==root;}
        public boolean materialVisible(){for(var part:materialParts)if(!part.visible || part.skipDraw)return false;return true;}
        public boolean representable(HierarchyMotion.EvaluatedFrame frame){return poses(frame).isPresent();}
        private Optional<Map<ModelPart,PartPose>> poses(HierarchyMotion.EvaluatedFrame frame){
            if(frame==null || !materialVisible() || frame.localPartTransforms().size()!=parts.size())return Optional.empty();
            var poses=new LinkedHashMap<ModelPart,PartPose>();
            for(var entry:parts.entrySet()) {
                var transform=frame.localPartTransforms().get(entry.getKey());
                if(transform==null)return Optional.empty();
                var pose=pose(transform.matrix(),entry.getValue().getInitialPose()).orElse(null);
                if(pose==null)return Optional.empty();
                poses.put(entry.getValue(),pose);
            }
            return Optional.of(poses);
        }
        public Optional<Applied> apply(HierarchyMotion.EvaluatedFrame frame) {
            var poses=poses(frame).orElse(null);
            if(poses==null)return Optional.empty();
            var saved=new ArrayList<Saved>(poses.size());
            try {
                for(var entry:poses.entrySet()){saved.add(Saved.of(entry.getKey()));entry.getKey().loadPose(entry.getValue());}
                return Optional.of(new Applied(saved));
            } catch(RuntimeException invalid) {
                for(int i=saved.size()-1;i>=0;i--)saved.get(i).restore();
                return Optional.empty();
            }
        }
    }
    public static final class Applied implements AutoCloseable {
        private final List<Saved> saved;
        private boolean closed;
        private Applied(List<Saved> saved){this.saved=saved;}
        @Override public void close(){if(closed)return;closed=true;for(int i=saved.size()-1;i>=0;i--)saved.get(i).restore();}
    }
    private CausalModelPartAdapter() {}

    /** Must be invoked only for a new renderer/model/catalog/resource generation. */
    public static Optional<Prepared> prepare(EntityModel<?> model,Identifier modelId,ModelGeometry accepted,
            AnatomyFilter filter) {
        if(model==null || modelId==null || accepted==null || filter==null || accepted.format()!=2)return Optional.empty();
        ModelPart root=model.root();
        List<ModelPart> all=root.getAllParts();
        var saved=new ArrayList<Saved>(all.size());
        try {
            for(var part:all){
                saved.add(Saved.of(part));
                part.resetPose();
                // Visibility is animation state and ModelPart has no baked-visibility reset.
                // Inspect geometry itself; verify retained visibility at each actual draw.
                part.visible=true;
                part.skipDraw=false;
            }
            // The accepted family transform is compared as an immutable catalog value at the caller;
            // this inspection proves that the actual renderer's ModelPart tree has the same material.
            var visible=ModelPartGeometryEngine.inspectVisible(modelId,root,ModelGeometry.matrix(accepted.modelTransform()));
            if(!compatible(accepted,visible))return Optional.empty();
            var parts=new LinkedHashMap<String,ModelPart>();
            for(var part:accepted.parts()) {
                var current=part.parent()==null?root:parts.get(part.parent()).getChild(part.id().substring(part.id().lastIndexOf('/')+1));
                parts.put(part.id(),current);
            }
            var material=new ArrayList<ModelPart>();
            var parents=new java.util.HashMap<String,String>();
            for(var part:accepted.parts())parents.put(part.id(),part.parent());
            var decisions=accepted.filterReport(filter);
            for(var piece:accepted.pieces())if("retained".equals(decisions.get(piece.id()))) {
                var path=piece.part();
                while(path!=null){
                    var part=parts.get(path);if(part==null)return Optional.empty();
                    if(!material.contains(part))material.add(part);
                    path=parents.get(path);
                }
            }
            return Optional.of(new Prepared(root,parts,material));
        } catch(RuntimeException invalid) {
            return Optional.empty();
        } finally {
            for(int i=saved.size()-1;i>=0;i--)saved.get(i).restore();
        }
    }

    private static boolean compatible(ModelGeometry accepted,ModelGeometry visible) {
        if(accepted.parts().size()!=visible.parts().size())return false;
        for(int i=0;i<accepted.parts().size();i++) {
            var a=accepted.parts().get(i);var b=visible.parts().get(i);
            if(!a.id().equals(b.id()) || !java.util.Objects.equals(a.parent(),b.parent())
                    || a.sourcePose()==null || b.sourcePose()==null
                    || !near(a.transform(),b.transform()) || !near(a.sourcePose(),b.sourcePose()))return false;
        }
        if(accepted.pieces().size()!=visible.pieces().size())return false;
        var visiblePieces=new LinkedHashMap<String,ModelGeometry.Piece>();
        for(var piece:visible.pieces())visiblePieces.put(piece.id(),piece);
        for(var piece:accepted.pieces()) {
            var other=visiblePieces.get(piece.id());
            if(other==null || !piece.part().equals(other.part()) || !near(piece.min(),other.min())
                    || !near(piece.max(),other.max()))return false;
        }
        return true;
    }
    private static boolean near(List<? extends Number> a,List<? extends Number> b) {
        if(a.size()!=b.size())return false;
        for(int i=0;i<a.size();i++)if(Math.abs(a.get(i).doubleValue()-b.get(i).doubleValue())>PIECE_EPS)return false;
        return true;
    }
    private static boolean near(ModelGeometry.SourcePose a,ModelGeometry.SourcePose b) {
        return Math.abs(a.x()-b.x())<=MATRIX_EPS && Math.abs(a.y()-b.y())<=MATRIX_EPS
            && Math.abs(a.z()-b.z())<=MATRIX_EPS && Math.abs(a.xRot()-b.xRot())<=MATRIX_EPS
            && Math.abs(a.yRot()-b.yRot())<=MATRIX_EPS && Math.abs(a.zRot()-b.zRot())<=MATRIX_EPS
            && Math.abs(a.xScale()-b.xScale())<=MATRIX_EPS && Math.abs(a.yScale()-b.yScale())<=MATRIX_EPS
            && Math.abs(a.zScale()-b.zScale())<=MATRIX_EPS;
    }
    private static Optional<PartPose> pose(Matrix4f matrix,PartPose neutral) {
        float determinant=matrix.determinant3x3();
        if(!Float.isFinite(determinant) || Math.abs(determinant)<1e-12)return Optional.empty();
        Vector3f translation=matrix.getTranslation(new Vector3f()),magnitudes=matrix.getScale(new Vector3f());
        // An animated ModelPart may reflect one or more local axes even when
        // its baked neutral pose does not. Try the nearest sign assignment
        // first; only assignments with the affine determinant's parity can
        // yield a proper rotation, and each must recompose exactly.
        for(int changes=0;changes<=3;changes++)for(int mask=0;mask<8;mask++) {
            if(Integer.bitCount(mask)!=changes)continue;
            float sx=Math.copySign(magnitudes.x,(mask&1)==0?neutral.xScale():-neutral.xScale());
            float sy=Math.copySign(magnitudes.y,(mask&2)==0?neutral.yScale():-neutral.yScale());
            float sz=Math.copySign(magnitudes.z,(mask&4)==0?neutral.zScale():-neutral.zScale());
            if(Math.abs(sx*sy*sz)<1e-12 || Math.signum(sx*sy*sz)!=Math.signum(determinant))continue;
            var candidate=poseForScale(matrix,translation,sx,sy,sz);
            if(candidate.isPresent())return candidate;
        }
        return Optional.empty();
    }
    private static Optional<PartPose> poseForScale(Matrix4f matrix,Vector3f translation,float sx,float sy,float sz) {
        Matrix4f rotation=new Matrix4f(matrix).setTranslation(0,0,0).scale(1/sx,1/sy,1/sz);
        // Quaternion-to-Euler extraction loses the sign of cos(y) near the ZYX
        // gimbal. Read the rotation columns directly, then verify through the
        // exact transform used by ModelPart before mutating any shared state.
        float y=(float)Math.atan2(-rotation.m02(),Math.hypot(rotation.m00(),rotation.m01()));
        float x=(float)Math.atan2(rotation.m12(),rotation.m22());
        float z=(float)Math.atan2(rotation.m01(),rotation.m00());
        var direct=matchingPose(matrix,translation,sx,sy,sz,x,y,z);
        if(direct.isPresent())return direct;
        // At an exact gimbal lock x and z are not individually determined.
        if(Math.hypot(rotation.m00(),rotation.m01())<2e-4) {
            float lockedY=rotation.m02()<0?(float)(Math.PI/2):(float)(-Math.PI/2);
            float lockedZ=(float)Math.atan2(-rotation.m10(),rotation.m11());
            var locked=matchingPose(matrix,translation,sx,sy,sz,0,lockedY,lockedZ);
            if(locked.isPresent())return locked;
        }
        Vector3f angles=rotation.getNormalizedRotation(new Quaternionf()).normalize().getEulerAnglesZYX(new Vector3f());
        return matchingPose(matrix,translation,sx,sy,sz,angles.x,angles.y,angles.z);
    }
    private static Optional<PartPose> matchingPose(Matrix4f matrix,Vector3f translation,
            float sx,float sy,float sz,float x,float y,float z) {
        var result=new PartPose(translation.x*16,translation.y*16,translation.z*16,x,y,z,sx,sy,sz);
        var rebuilt=new Matrix4f().translationRotateScale(new Vector3f(result.x()/16,result.y()/16,result.z()/16),
            new Quaternionf().rotationZYX(result.zRot(),result.yRot(),result.xRot()),
            new Vector3f(result.xScale(),result.yScale(),result.zScale()));
        return rebuilt.equals(matrix,2e-4f)?Optional.of(result):Optional.empty();
    }
}
