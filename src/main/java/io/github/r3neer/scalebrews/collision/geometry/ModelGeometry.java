package io.github.r3neer.scalebrews.collision.geometry;

import java.util.*;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/** Server-safe export DTO: no client model or renderer references. */
public record ModelGeometry(int format,String source,String version,List<Part> parts,List<Piece> pieces,List<Float> modelTransform) {
    public ModelGeometry(int format,String source,String version,List<Part> parts,List<Piece> pieces) {
        this(format,source,version,parts,pieces,values(new Matrix4f().scaling(-1,-1,1).translate(0,-1.501f,0)));
    }
    public ModelGeometry withModelTransform(Matrix4f transform) {
        return new ModelGeometry(2,source,version,parts,pieces,values(transform));
    }
    public record Part(String id,String parent,List<Float> transform) {
        public Part {transform=List.copyOf(transform);}
    }
    public record Piece(String id,String part,List<Double> min,List<Double> max,String excluded) {
        public Piece {min=List.copyOf(min);max=List.copyOf(max);}
        public AABB box(){return new AABB(min.get(0),min.get(1),min.get(2),max.get(0),max.get(1),max.get(2));}
    }
    public ModelGeometry {
        parts=List.copyOf(parts);pieces=List.copyOf(pieces);
        if((format!=1 && format!=2) || source==null || version==null || parts.isEmpty() || parts.size()>512 || pieces.size()>4096)throw new IllegalArgumentException("Unsupported/oversized model");
        if(modelTransform==null && format==1)modelTransform=values(new Matrix4f().scaling(-1,-1,1).translate(0,-1.501f,0));
        if(modelTransform==null || modelTransform.size()!=16 || modelTransform.stream().anyMatch(v->v==null || !Float.isFinite(v)))throw new IllegalArgumentException("Invalid model transform");
        modelTransform=List.copyOf(modelTransform);
        var root=matrix(modelTransform);
        if(!Float.isFinite(root.determinant()) || Math.abs(root.determinant())<1e-12 || Math.abs(root.m03())>1e-6 || Math.abs(root.m13())>1e-6 || Math.abs(root.m23())>1e-6 || Math.abs(root.m33()-1)>1e-6)throw new IllegalArgumentException("Non-affine model transform");
        Set<String> ids=new HashSet<>();
        for(Part p:parts) {
            if(p.id==null || p.id.isBlank() || p.id.length()>256 || !ids.add(p.id))throw new IllegalArgumentException("Duplicate/invalid part");
            if(p.parent!=null && (!ids.contains(p.parent) || p.parent.equals(p.id)))throw new IllegalArgumentException("Parent must precede child");
            if(p.transform.size()!=16 || p.transform.stream().anyMatch(f->f==null || !Float.isFinite(f)))throw new IllegalArgumentException("Invalid matrix");
            Matrix4f m=matrix(p.transform);
            if(Math.abs(m.m03())>1e-6 || Math.abs(m.m13())>1e-6 || Math.abs(m.m23())>1e-6 || Math.abs(m.m33()-1)>1e-6 || !Float.isFinite(m.determinant()) || Math.abs(m.determinant())<1e-12)
                throw new IllegalArgumentException("Non-affine/degenerate matrix");
        }
        Set<String> pieceIds=new HashSet<>();
        for(Piece p:pieces) {
            if(p.id==null || p.id.isBlank() || p.id.length()>256 || !pieceIds.add(p.id) || !ids.contains(p.part) || p.min.size()!=3 || p.max.size()!=3)throw new IllegalArgumentException("Invalid piece");
            for(int i=0;i<3;i++)if(!Double.isFinite(p.min.get(i)) || !Double.isFinite(p.max.get(i)) || Math.abs(p.min.get(i))>1024 || Math.abs(p.max.get(i))>1024 || p.min.get(i)>=p.max.get(i))throw new IllegalArgumentException("Invalid bounds");
            double volume=(p.max.get(0)-p.min.get(0))*(p.max.get(1)-p.min.get(1))*(p.max.get(2)-p.min.get(2));
            if(!Double.isFinite(volume) || volume<=0)throw new IllegalArgumentException("Degenerate piece volume");
        }
    }
    public static List<Float> values(Matrix4f matrix) {
        float[] f=matrix.get(new float[16]);List<Float> out=new ArrayList<>();for(float v:f)out.add(v);return List.copyOf(out);
    }
    public static Matrix4f matrix(List<Float> values) {
        float[] f=new float[16];for(int i=0;i<16;i++)f[i]=values.get(i);return new Matrix4f().set(f);
    }
    private static Matrix4f validTransform(Matrix4f matrix) {
        if(matrix==null)throw new IllegalArgumentException("Missing model transform");
        for(float value:matrix.get(new float[16]))if(!Float.isFinite(value))throw new IllegalArgumentException("Non-finite model transform");
        if(Math.abs(matrix.m03())>1e-6 || Math.abs(matrix.m13())>1e-6 || Math.abs(matrix.m23())>1e-6 || Math.abs(matrix.m33()-1)>1e-6
                || !Float.isFinite(matrix.determinant()) || Math.abs(matrix.determinant())<1e-12)
            throw new IllegalArgumentException("Non-affine/degenerate model transform");
        return matrix;
    }
    public Map<String,Matrix4f> transforms(Map<String,Matrix4f> replacements) {
        if(replacements==null)throw new IllegalArgumentException("Missing joint replacements");
        Set<String> ids=new HashSet<>();for(Part part:parts)ids.add(part.id);
        if(!ids.containsAll(replacements.keySet()))throw new IllegalArgumentException("Unknown model joint");
        replacements.values().forEach(ModelGeometry::validTransform);
        Map<String,Matrix4f> world=new LinkedHashMap<>();
        for(Part p:parts) {
            Matrix4f local=replacements.containsKey(p.id)?new Matrix4f(replacements.get(p.id)):matrix(p.transform);
            world.put(p.id,validTransform(p.parent==null?local:new Matrix4f(world.get(p.parent)).mul(local)));
        }
        return world;
    }
    /** Every piece receives a reproducible local-space decision, including excluded decoration. */
    public Map<String,String> filterReport(AnatomyFilter filter) {
        var rest=transforms(Map.of());AABB envelope=null;
        for(Piece p:pieces)if(p.excluded==null && p.box().getXsize()*p.box().getYsize()*p.box().getZsize()>0) {
            AABB b=ConvexBox.of(p.box(),rest.get(p.part)).bounds();envelope=envelope==null?b:envelope.minmax(b);
        }
        double volume=envelope==null?0:envelope.getXsize()*envelope.getYsize()*envelope.getZsize();
        Map<String,String> report=new LinkedHashMap<>();
        for(Piece p:pieces) {
            String reason=filter.rejection(p,volume);
            report.put(p.id,reason==null?"retained":reason);
        }
        return Collections.unmodifiableMap(report);
    }
    public Map<String,ConvexBox> evaluate(Matrix4f root,Map<String,Matrix4f> replacements,AnatomyFilter filter) {
        validTransform(root);
        var world=transforms(replacements);var decisions=filterReport(filter);
        Map<String,ConvexBox> result=new LinkedHashMap<>();
        for(Piece p:pieces)if(decisions.get(p.id).equals("retained"))
            result.put(p.id,ConvexBox.of(p.box(),new Matrix4f(root).mul(world.get(p.part))));
        return Collections.unmodifiableMap(result);
    }
}
