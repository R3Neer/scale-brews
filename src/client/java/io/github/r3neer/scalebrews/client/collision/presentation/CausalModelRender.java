package io.github.r3neer.scalebrews.client.collision.presentation;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking.PresentationFrame;
import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking.PresentationIdentity;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.BuiltInGeometryEngines;
import io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/** Original-model presentation consumer. Accepted wire frames remain the only pose authority. */
public final class CausalModelRender {
    public record Token(WeakReference<LivingEntity> support,PresentationIdentity identity,long trackingGeneration,long frameSerial) {
        public Token {
            if(support==null || identity==null || trackingGeneration<1 || frameSerial<1)throw new IllegalArgumentException("Invalid causal render token");
        }
    }
    private record Key(UUID epoch,long revision,Identifier model,AnatomyFilter filter,long resourceGeneration) {}
    private record CacheEntry(Key key,Optional<CausalModelPartAdapter.Prepared> prepared) {}
    private record Selection(ModelGeometry model,AnatomyFilter filter) {}
    private static final int MAX_PREPARED_MODELS=512;
    private static final Map<EntityModel<?>,CacheEntry> PREPARED=new IdentityHashMap<>();
    private static final ThreadLocal<ArrayDeque<SubmitScope>> SUBMITS=ThreadLocal.withInitial(ArrayDeque::new);
    private static long resourceGeneration;
    private CausalModelRender() {}

    public static void capture(LivingEntity support,LivingEntityRenderState state) {
        var stored=(CausalPresentationState)state;
        stored.scalebrews$causalToken(null);
        stored.scalebrews$causalSubmitted(false);
        if(support==null || support.isPassenger())return;
        var frame=AnatomyClientNetworking.presentationFrame(support).orElse(null);
        if(frame!=null)stored.scalebrews$causalToken(new Token(new WeakReference<>(support),frame.identity(),frame.trackingGeneration(),frame.after().frameSerial()));
    }
    /** Resource reload re-bakes renderer models even if the server catalog remains the same. */
    public static void resourceReload(){resourceGeneration++;PREPARED.clear();}

    private static PresentationFrame current(LivingEntityRenderState state) {
        if(!(state instanceof CausalPresentationState stored))return null;
        var token=stored.scalebrews$causalToken();
        var support=token==null?null:token.support().get();
        if(support==null || support.isRemoved() || support.level()!=Minecraft.getInstance().level
                || !AnatomyClientNetworking.ready(support))return null;
        var frame=AnatomyClientNetworking.presentationFrame(support).orElse(null);
        return frame!=null && frame.identity().equals(token.identity())
            && frame.trackingGeneration()==token.trackingGeneration()
            && frame.after().frameSerial()==token.frameSerial()?frame:null;
    }
    private static Selection selection(PresentationFrame frame,LivingEntity support) {
        var transfer=AnatomyClientNetworking.catalog();
        if(!transfer.ready() || !frame.identity().epoch().equals(transfer.epoch())
                || frame.identity().revision()!=transfer.revision())return null;
        var snapshot=transfer.snapshot();
        var binding=snapshot.bindings().get(BuiltInRegistries.ENTITY_TYPE.getKey(support.getType()));
        if(binding==null || !binding.selection().geometry().model().equals(frame.identity().model()))return null;
        var engine=binding.selection().geometry().engine();
        if(!engine.equals(BuiltInGeometryEngines.MODEL_PART) && !engine.equals(LegacyCollisionData.PRECOMPUTED_GEOMETRY))return null;
        var model=snapshot.models().get(frame.identity().model().toString());
        if(model==null || !frame.evaluated().modelTransform().values().equals(model.modelTransform()))return null;
        return new Selection(model,binding.selection().geometry().filter());
    }
    private static CausalModelPartAdapter.Prepared prepared(EntityModel<?> rendererModel,PresentationFrame frame,LivingEntity support) {
        var chosen=selection(frame,support);
        if(chosen==null)return null;
        var key=new Key(frame.identity().epoch(),frame.identity().revision(),frame.identity().model(),chosen.filter(),resourceGeneration);
        var cached=PREPARED.get(rendererModel);
        if(cached==null || !cached.key().equals(key) || cached.prepared().isPresent() && !cached.prepared().orElseThrow().owns(rendererModel)) {
            if(PREPARED.size()>=MAX_PREPARED_MODELS && !PREPARED.containsKey(rendererModel))PREPARED.clear();
            cached=new CacheEntry(key,CausalModelPartAdapter.prepare(rendererModel,key.model(),chosen.model(),chosen.filter()));
            PREPARED.put(rendererModel,cached);
        }
        return cached.prepared().orElse(null);
    }
    public static final class SubmitScope implements AutoCloseable {
        private final LivingEntityRenderState state;
        private final Matrix4f base;
        private CausalModelPartAdapter.Applied layerPose;
        private boolean closed;
        private SubmitScope(LivingEntityRenderState state,PoseStack poses) {
            this.state=state;this.base=new Matrix4f(poses.last().pose());
        }
        @Override public void close(){
            if(closed)return;closed=true;
            try {if(layerPose!=null)layerPose.close();}
            finally {
                var stack=SUBMITS.get();
                if(stack.peek()==this)stack.pop();else stack.remove(this);
                if(stack.isEmpty())SUBMITS.remove();
            }
        }
    }
    public static SubmitScope enterSubmit(LivingEntityRenderState state,PoseStack poses) {
        var scope=new SubmitScope(state,poses);
        SUBMITS.get().push(scope);
        ((CausalPresentationState)state).scalebrews$causalSubmitted(false);
        return scope;
    }
    private static SubmitScope scope(LivingEntityRenderState state) {
        var stack=SUBMITS.get();var current=stack.peek();return current!=null && current.state==state?current:null;
    }
    /** Called after vanilla has built its root stack and before it queues the base model/layers. */
    public static void applyRoot(LivingEntityRenderState state,EntityModel<?> rendererModel,PoseStack poses) {
        var scope=scope(state);var frame=current(state);
        if(scope==null || frame==null || rendererModel==null)return;
        var support=((CausalPresentationState)state).scalebrews$causalToken().support().get();
        if(support==null)return;
        var adapter=prepared(rendererModel,frame,support);
        if(adapter==null || !adapter.representable(frame.evaluated()))return;
        try {
            var origin=frame.evaluated().origin();
            var desired=new Matrix4f(scope.base).translate((float)(origin.x-state.x),(float)(origin.y-state.y),(float)(origin.z-state.z))
                .mul(frame.evaluated().rootTrs().matrix()).mul(frame.evaluated().modelTransform().matrix());
            var current=new Matrix4f(poses.last().pose());
            if(!Float.isFinite(current.determinant()) || Math.abs(current.determinant())<1e-12
                    || !Float.isFinite(desired.determinant()))return;
            var correction=current.invert().mul(desired);
            poses.mulPose(correction);
            ((CausalPresentationState)state).scalebrews$causalSubmitted(true);
        } catch(RuntimeException invalid) {
            ((CausalPresentationState)state).scalebrews$causalSubmitted(false);
        }
    }
    /** Immediate render layers sample the shared model during submit, after vanilla setupAnim. */
    public static void applyLayers(LivingEntityRenderState state,EntityModel<?> rendererModel) {
        var scope=scope(state);
        if(scope==null || !((CausalPresentationState)state).scalebrews$causalSubmitted())return;
        var frame=current(state);
        var support=((CausalPresentationState)state).scalebrews$causalToken().support().get();
        if(frame==null || support==null)return;
        var adapter=prepared(rendererModel,frame,support);
        if(adapter==null)return;
        if(scope.layerPose!=null)scope.layerPose.close();
        scope.layerPose=adapter.apply(frame.evaluated()).orElse(null);
    }
    /** Delayed base model draw revalidates the token after vanilla setupAnim has run again. */
    public static void draw(Object renderState,Object model,Runnable original) {
        if(!(renderState instanceof LivingEntityRenderState state)
                || !(model instanceof EntityModel<?> rendererModel)
                || !(state instanceof CausalPresentationState stored)
                || !stored.scalebrews$causalSubmitted()) {original.run();return;}
        var frame=current(state);
        var token=stored.scalebrews$causalToken();
        var support=token==null?null:token.support().get();
        // A queued submit cannot retain authority across a lifecycle barrier.
        if(frame==null || support==null)return;
        var adapter=prepared(rendererModel,frame,support);
        if(adapter==null)return;
        var applied=adapter.apply(frame.evaluated()).orElse(null);
        if(applied==null)return;
        try {original.run();} finally {applied.close();}
    }
}
