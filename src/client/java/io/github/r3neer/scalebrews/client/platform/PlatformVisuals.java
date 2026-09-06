package io.github.r3neer.scalebrews.client.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.platform.*;
import io.github.r3neer.scalebrews.client.mixin.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import java.util.*;

/** Rendering-only translation, sampled from the real model and restored before returning. */
public final class PlatformVisuals {
    public interface Adapter {
        Vec3 offset(LivingEntity support, PlatformDefinition.Surface surface, Vec3 contact, float partialTick);
    }
    private static final Map<Identifier,Adapter> ADAPTERS=new HashMap<>();
    private static final Set<String> WARNED=new HashSet<>();
    private static final Map<Entity,Map<String,Matrix4f>> CACHE=new IdentityHashMap<>();
    private static final Set<Entity> VISITING=Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean sampling;
    private static Object level;
    private static long tick=-1;
    private static float fraction=-1;
    private PlatformVisuals() {}
    public static void registerAdapter(Identifier entity,Adapter adapter) { ADAPTERS.put(entity,adapter); }
    public static boolean sampling() { return sampling; }
    public static void reset() { CACHE.clear(); VISITING.clear(); level=null; }
    public static int missingVisualCount() { return WARNED.size(); }
    public static Vec3 offset(Entity body,float partial) {
        var client=Minecraft.getInstance();
        long now=client.level==null?0:client.level.getGameTime();
        if(level!=client.level || tick!=now || fraction!=partial) {
            CACHE.clear(); level=client.level; tick=now; fraction=partial;
        }
        if(!VISITING.add(body)) return Vec3.ZERO;
        try {
            if(body.isPassenger()) return offset(body.getVehicle(),partial);
            var s=Platforms.state(body);
            if(!Platforms.supported(body) || s.surface==null) return Vec3.ZERO;
            Vec3 parent=offset(s.support,partial);
            if(s.surface.visual().isEmpty()) return parent;
            var id=BuiltInRegistries.ENTITY_TYPE.getKey(s.support.getType());
            var adapter=ADAPTERS.get(id);
            if(adapter!=null) return parent.add(adapter.offset(s.support,s.surface,s.contact,partial));
            Vec3 relative=PlatformGeometry.frame(s.support).world(s.contact).subtract(s.support.position());
            Matrix4f delta=CACHE.computeIfAbsent(s.support,e->sample(s.support,partial))
                .getOrDefault(s.surface.id(),new Matrix4f());
            var mapped=delta.transformPosition(new Vector3f((float)relative.x,(float)relative.y,(float)relative.z));
            return parent.add(mapped.x-relative.x,mapped.y-relative.y,mapped.z-relative.z);
        } finally { VISITING.remove(body); }
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static Map<String,Matrix4f> sample(LivingEntity support,float partial) {
        Map<String,Matrix4f> result=new HashMap<>();
        var renderer=Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(support);
        if(!(renderer instanceof LivingEntityRenderer living)) {
            String key=BuiltInRegistries.ENTITY_TYPE.getKey(support.getType())+":renderer";
            if(WARNED.add(key)) ScaleBrews.LOGGER.warn("Platform visual {} requires a client adapter; physical surface retained",key);
            return result;
        }
        boolean oldSampling=sampling; sampling=true;
        List<ModelPart> parts=List.of(); List<PartPose> saved=List.of();
        List<Boolean> visible=List.of(),skip=List.of();
        try {
            LivingEntityRenderState state=(LivingEntityRenderState)living.createRenderState();
            living.extractRenderState(support,state,partial);
            EntityModel model=living instanceof AgeableModelAccess a ? a.scalebrews$adultModel() : living.getModel();
            if(living instanceof PlatformVariantModels variants) {
                Object variant=switch(state) {
                    case net.minecraft.client.renderer.entity.state.CowRenderState cow -> cow.variant.modelAndTexture().model();
                    case net.minecraft.client.renderer.entity.state.PigRenderState pig -> pig.variant.modelAndTexture().model();
                    case net.minecraft.client.renderer.entity.state.ChickenRenderState chicken -> chicken.variant.modelAndTexture().model();
                    default -> null;
                };
                if(variant!=null) model=variants.scalebrews$models().get(variant).getModel(false);
            }
            parts=model.root().getAllParts();
            saved=parts.stream().map(ModelPart::storePose).toList();
            visible=parts.stream().map(p->p.visible).toList();
            skip=parts.stream().map(p->p.skipDraw).toList();
            model.setupAnim(state);
            var stack=new PoseStack();
            stack.scale(state.scale,state.scale,state.scale);
            var access=(LivingRendererAccess)living;
            access.scalebrews$rotations(state,stack,state.bodyRot,state.scale);
            stack.scale(-1,-1,1); access.scalebrews$scale(state,stack); stack.translate(0,-1.501,0);
            Matrix4f base=new Matrix4f(stack.last().pose());
            for(var surface:Platforms.definition(support).surfaces()) {
                if(surface.visual().isEmpty()) continue;
                var visual=surface.visual().orElseThrow();
                try {
                    result.put(surface.id(),sampleSurface(support,surface,visual,model,base));
                } catch(RuntimeException ex) {
                    String key=BuiltInRegistries.ENTITY_TYPE.getKey(support.getType())+":"+visual.part();
                    if(WARNED.add(key)) ScaleBrews.LOGGER.warn("Platform visual {} unavailable; physical surface retained",key,ex);
                }
            }
            return result;
        } catch(RuntimeException ex) {
            String key=BuiltInRegistries.ENTITY_TYPE.getKey(support.getType())+":model";
            if(WARNED.add(key)) ScaleBrews.LOGGER.warn("Platform visual {} unavailable; physical surface retained",key,ex);
            return result;
        } finally {
            for(int i=0;i<parts.size() && i<saved.size();i++) {
                parts.get(i).loadPose(saved.get(i));
                parts.get(i).visible=visible.get(i); parts.get(i).skipDraw=skip.get(i);
            }
            sampling=oldSampling;
        }
    }
    private static Matrix4f sampleSurface(LivingEntity support,PlatformDefinition.Surface surface,
                                          PlatformDefinition.Visual visual,EntityModel<?> model,Matrix4f base) {
            var stack=new PoseStack();stack.last().pose().set(base);
            Matrix4f rest=new Matrix4f(base);
            ModelPart part=model.root();
            rest=pose(rest,part.getInitialPose()); part.translateAndRotate(stack);
            for(String name:visual.part().split("/")) {
                if(name.isEmpty() || name.equals("root")) continue;
                part=part.getChild(name);
                rest=pose(rest,part.getInitialPose()); part.translateAndRotate(stack);
            }
            // Geometry describes the resting surface. The point maps it to the actual model top.
            var anchor=new Vector3f((float)visual.x(),(float)visual.y(),(float)visual.z());
            var restPoint=rest.transformPosition(new Vector3f(anchor));
            var f=PlatformGeometry.frame(support);
            Vec3 center=f.world(new Vec3(surface.x(),surface.y(),surface.z())).subtract(support.position());
            Matrix4f calibration=new Matrix4f().translation(restPoint.x-(float)center.x,
                restPoint.y-(float)center.y,restPoint.z-(float)center.z);
            return new Matrix4f(stack.last().pose()).mul(rest.invert()).mul(calibration);
    }
    private static Matrix4f pose(Matrix4f m,PartPose p) {
        return m.translate(p.x()/16,p.y()/16,p.z()/16).rotateZYX(p.zRot(),p.yRot(),p.xRot()).scale(p.xScale(),p.yScale(),p.zScale());
    }
}
