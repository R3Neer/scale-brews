package io.github.r3neer.scalebrews.client.collision.preparation;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

/** Preparation-only extractor. Call with an original model from an isolated client. */
public final class GeometryExtractor {
    private final List<ModelGeometry.Part> parts=new ArrayList<>();
    private final List<ModelGeometry.Piece> pieces=new ArrayList<>();
    private final Set<Object> visited=Collections.newSetFromMap(new IdentityHashMap<>());
    private GeometryExtractor() {}
    public static ModelGeometry vanilla(String source,String version,ModelPart root,Set<String> excludedParts) {
        GeometryExtractor x=new GeometryExtractor();x.vanilla(root,"root",null,false,excludedParts);
        return new ModelGeometry(1,source,version,x.parts,x.pieces);
    }
    @SuppressWarnings("unchecked")
    private void vanilla(ModelPart p,String id,String parent,boolean hidden,Set<String> excludes) {
        if(!visited.add(p) || parts.size()>=512)throw new IllegalArgumentException("Cyclic/oversized model");
        PoseStack stack=new PoseStack();p.translateAndRotate(stack);
        parts.add(new ModelGeometry.Part(id,parent,ModelGeometry.values(new Matrix4f(stack.last().pose()))));
        boolean excluded=hidden || !p.visible || excludes.contains(id) || excludes.contains(id.substring(id.lastIndexOf('/')+1));
        List<ModelPart.Cube> cubes=(List<ModelPart.Cube>)field(p,"cubes");int index=0;
        for(var cube:cubes) {
            List<double[]> vertices=new ArrayList<>();
            for(var polygon:cube.polygons)for(var vertex:polygon.vertices())vertices.add(new double[]{vertex.worldX(),vertex.worldY(),vertex.worldZ()});
            piece(id+"/cube_"+index++,id,vertices,excluded?"hidden_or_cosmetic":p.skipDraw?"skip_draw":null);
        }
        Map<String,ModelPart> children=(Map<String,ModelPart>)field(p,"children");
        children.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->vanilla(e.getValue(),id+"/"+e.getKey(),id,excluded,excludes));
    }
    /** Citadel-style models have a different hierarchy and optional non-inherited scale. */
    public static ModelGeometry alex(String source,String version,Object model,Set<String> excludedParts) {
        try {
            GeometryExtractor x=new GeometryExtractor();Map<Object,String> names=new IdentityHashMap<>();
            for(Field f:model.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object value=f.get(model);
                if(value!=null && value.getClass().getName().endsWith("AdvancedModelBox"))names.put(value,f.getName());
            }
            Iterable<?> roots=(Iterable<?>)model.getClass().getMethod("parts").invoke(model);
            int index=0;for(Object root:roots)x.alex(root,"root_"+index++,null,false,excludedParts,names);
            return new ModelGeometry(1,source,version,x.parts,x.pieces);
        } catch(ReflectiveOperationException e){throw new IllegalArgumentException("Unsupported Alex model",e);}
    }
    private void alex(Object p,String fallback,String parent,boolean hidden,Set<String> excludes,Map<Object,String> names) {
        if(!visited.add(p) || parts.size()>=512)throw new IllegalArgumentException("Cyclic/oversized Alex model");
        String id=parent==null?fallback:parent+"/"+names.getOrDefault(p,fallback);
        PoseStack stack=new PoseStack();
        boolean advanced=p.getClass().getName().endsWith("AdvancedModelBox");
        try {p.getClass().getMethod(advanced?"translateAndRotate":"translateRotate",PoseStack.class).invoke(p,stack);}
        catch(ReflectiveOperationException e){throw new IllegalArgumentException("Missing model transform",e);}
        Matrix4f local=new Matrix4f(stack.last().pose());
        parts.add(new ModelGeometry.Part(id,parent,ModelGeometry.values(local)));
        boolean excluded=hidden || !(boolean)field(p,"showModel") || excludes.contains(id) || excludes.contains(names.getOrDefault(p,""));
        int index=0;
        for(Object cube:(Iterable<?>)field(p,"cubeList")) {
            List<double[]> vertices=new ArrayList<>();
            for(Object quad:(Object[])field(cube,"quads"))for(Object vertex:(Object[])field(quad,"vertexPositions")) {
                Vector3fc v=(Vector3fc)field(vertex,"position");vertices.add(new double[]{v.x()/16d,v.y()/16d,v.z()/16d});
            }
            piece(id+"/cube_"+index++,id,vertices,excluded?"hidden_or_cosmetic":null);
        }
        String childParent=id;
        if(advanced) {
            float x=((Number)field(p,"scaleX")).floatValue(),y=((Number)field(p,"scaleY")).floatValue(),z=((Number)field(p,"scaleZ")).floatValue();
            childParent=id+"/unscaled_children";
            Matrix4f inheritance=(boolean)field(p,"scaleChildren")?new Matrix4f():new Matrix4f().scaling(1/Math.max(x,1e-4f),1/Math.max(y,1e-4f),1/Math.max(z,1e-4f));
            parts.add(new ModelGeometry.Part(childParent,id,ModelGeometry.values(inheritance)));
        }
        int childIndex=0;for(Object child:(Iterable<?>)field(p,"childModels"))alex(child,"child_"+childIndex++,childParent,excluded,excludes,names);
    }
    private void piece(String id,String part,List<double[]> vertices,String excluded) {
        if(pieces.size()>=4096)throw new IllegalArgumentException("Too many cubes");
        if(vertices.isEmpty())return;
        double[] lo={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY},hi={-lo[0],-lo[1],-lo[2]};
        for(double[] v:vertices)for(int i=0;i<3;i++){
            if(!Double.isFinite(v[i]))throw new IllegalArgumentException("Non-finite model vertex");
            lo[i]=Math.min(lo[i],v[i]);hi[i]=Math.max(hi[i],v[i]);
        }
        // Renderer models legitimately contain zero-thickness visual quads/cubes. They are not material
        // convex pieces, so the engine omits them rather than weakening ModelGeometry's load boundary.
        for(int i=0;i<3;i++)if(!(hi[i]>lo[i]))return;
        pieces.add(new ModelGeometry.Piece(id,part,List.of(lo[0],lo[1],lo[2]),List.of(hi[0],hi[1],hi[2]),excluded));
    }
    private static Object field(Object owner,String name) {
        for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass())try {
            Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner);
        }catch(NoSuchFieldException ignored){}catch(IllegalAccessException e){throw new IllegalArgumentException(e);}
        throw new IllegalArgumentException("Missing "+owner.getClass().getName()+"."+name);
    }
}
