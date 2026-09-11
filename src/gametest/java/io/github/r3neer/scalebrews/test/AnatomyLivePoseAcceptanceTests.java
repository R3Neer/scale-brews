package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import io.github.r3neer.scalebrews.collision.pose.QuadrupedPose;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Live server-provider -> packet -> original-model equivalence at one causal
 * sample. The independently interpolated vanilla replica is diagnostic only:
 * this fixture does not certify the future G4 renderer integration.
 * No test-side tracker is constructed and no renderer input defines authority.
 */
public final class AnatomyLivePoseAcceptanceTests {
    /** Four END-level authority frames bound accepted client interpolation delay. */
    private static final int MAX_AUTHORITY_DELAY_TICKS=4;
    private AnatomyLivePoseAcceptanceTests() {}

    public static void verifyCow(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla(
            "minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var entityId=new AtomicInteger(-1);
        var world=context.worldBuilder().create();
        var recording=new AtomicBoolean();
        var serverRef=new AtomicReference<MinecraftServer>();
        Deque<AnatomyRuntime.AuthoritativePose> samples=new ConcurrentLinkedDeque<>();
        // The provider ticks and publishes in the level END hook. Capturing in
        // a later registered END hook gives the test the same immutable frame
        // that `send` used, rather than a test-thread reconstruction.
        ServerTickEvents.END_LEVEL_TICK.register(level->{
            if(!recording.get() || level.getServer()!=serverRef.get())return;
            var entity=level.getEntity(entityId.get());
            if(entity instanceof Cow cow)AnatomyRuntime.authoritativePose(cow).ifPresent(sample->{
                var previous=samples.peekLast();
                if(previous!=null && previous.serverTick()==sample.serverTick())return;
                samples.addLast(sample);
                while(samples.size()>MAX_AUTHORITY_DELAY_TICKS)samples.removeFirst();
            });
        });
        try {
            world.getServer().runOnServer(server->{
                serverRef.set(server);
                var level=server.overworld();
                Cow cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("Could not create live cow");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(3,2,3);
                level.addFreshEntity(cow);entityId.set(cow.getId());
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:live_cow",profile));
            });
            recording.set(true);
            // Binding/catalogue delivery is real and bounded. An empty runtime
            // sample here is a test failure, never an excuse to use render state.
            context.waitTicks(12);
            for(int frame=0;frame<3;frame++) {
                world.getServer().runOnServer(server->{
                    var cow=(Cow)server.overworld().getEntity(entityId.get());
                    if(cow==null)throw new AssertionError("Live cow disappeared before authority capture");
                    var before=cow.position();
                    // NoAI disables Mob.isEffectiveAi and therefore vanilla travel.
                    // Exercise actual self movement, not a velocity never consumed.
                    cow.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(.12,0,0));
                    if(cow.position().distanceToSqr(before)<.01)
                        throw new AssertionError("Fixture did not actually move its NoAI cow");
                });
                context.waitTicks(1);
            }
            context.runOnClient(client->{
                var entity=client.level==null?null:client.level.getEntity(entityId.get());
                if(!(entity instanceof Cow cow))throw new AssertionError("Client did not resolve live cow");
                var history=AnatomyClientNetworking.pose(cow.getUUID());
                if(history==null || history.current()==null)throw new AssertionError("Client did not receive a live authoritative cow pose");
                var packet=history.current();
                var oldest=samples.peekFirst();var newest=samples.peekLast();
                if(oldest==null || newest==null)throw new AssertionError("Runtime did not publish an END-level authority frame");
                if(packet.tick()<oldest.serverTick() || packet.tick()>newest.serverTick())throw new AssertionError(
                    "Client pose tick "+packet.tick()+" exceeds bounded authority window "
                        +oldest.serverTick()+".."+newest.serverTick());
                var authority=samples.stream().filter(sample->sample.entity().equals(cow.getUUID())
                    && sample.serverTick()==packet.tick()).findFirst().orElseThrow();
                if(!packet.inputs().equals(authority.inputs()))throw new AssertionError(
                    "Client pose payload differs from exact runtime frame "+authority.serverTick());
                var segment=history.segment(packet.tick());
                if(!segment.after().inputs().equals(packet.inputs()))throw new AssertionError(
                    "Client history current endpoint differs from packet "+packet.tick());
                var render0=renderInputs(cow,0);var renderHalf=renderInputs(cow,.5f);var render1=renderInputs(cow,1);
                System.out.println("ANATOMY_LIVE_CLOCK minecraft:cow authorityFrames="+samples.stream()
                    .map(frame->frame.serverTick()+":"+frame.inputs()).toList()+" packet="+packet.tick()+":"+packet.inputs()
                    +" historyFraction="+segment.fraction()+" historyBefore="+segment.before().inputs()+" historyAfter="+segment.after().inputs()
                    +" entityTick="+cow.tickCount+" head="+cow.yHeadRotO+"->"+cow.yHeadRot+" body="+cow.yBodyRotO+"->"+cow.yBodyRot
                    +" renderer0="+render0+" rendererHalf="+renderHalf+" renderer1="+render1);
                // A vanilla replica has its own quantized head/body and animation
                // clocks. Partial=1 does not make it the packet's authority tick.
                // Keep those observations above; compare the real original model
                // at exactly the received inputs rather than searching for a pass.
                compareOriginalModel(cow,packet.inputs());
                System.out.println("ANATOMY_LIVE_TRACKER minecraft:cow authority="+authority.serverTick()
                    +" clientLevelTick="+client.level.getGameTime()+" walk="+packet.inputs().walkAmount());
            });
            var latest=samples.peekLast();
            if(latest==null || latest.inputs().walkAmount()<=0)
                throw new AssertionError("Actual cow movement did not advance the runtime provider walk state");
        } finally {
            recording.set(false);
            world.getServer().runOnServer(AnatomyRuntime::stop);
            serverRef.set(null);
            world.close();
        }
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private record RendererInputs(float partial,float walkPhase,float walkAmount,float headYaw,float headPitch) {}

    @SuppressWarnings({"rawtypes","unchecked"})
    private static RendererInputs renderInputs(Cow cow,float partial) {
        var dispatcher=net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher();
        var raw=dispatcher.getRenderer(cow);
        if(!(raw instanceof LivingEntityRenderer renderer))throw new AssertionError("Cow renderer is not a LivingEntityRenderer");
        LivingEntityRenderState state=(LivingEntityRenderState)renderer.createRenderState();
        renderer.extractRenderState(cow,state,partial);
        return new RendererInputs(partial,state.walkAnimationPos,state.walkAnimationSpeed,state.yRot,state.xRot);
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private static void compareOriginalModel(Cow cow,PoseProvider.Inputs inputs) {
        var renderer=net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(cow);
        if(!(renderer instanceof LivingEntityRenderer living))throw new AssertionError("No living cow renderer");
        LivingEntityRenderState state=(LivingEntityRenderState)living.createRenderState();
        // The original model consumes these channels, not renderer interpolation.
        state.walkAnimationPos=inputs.walkPhase();state.walkAnimationSpeed=inputs.walkAmount();
        state.xRot=inputs.headPitch();state.yRot=inputs.headYaw();state.ageInTicks=inputs.age();
        ModelGeometry baseline=GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of());
        var predicted=baseline.evaluate(new Matrix4f(),new QuadrupedPose().evaluate(baseline,inputs).orElseThrow(),AnatomyFilter.DEFAULT);
        EntityModel model=new CowModel(CowModel.createBodyLayer().bakeRoot());
        model.setupAnim(state);
        var original=GeometryExtractor.vanilla("minecraft:cow","26.2",model.root(),Set.of())
            .evaluate(new Matrix4f(),Map.of(),AnatomyFilter.DEFAULT);
        compare(predicted,original);
    }

    private static void compare(Map<String,ConvexBox> expected,Map<String,ConvexBox> actual) {
        if(!expected.keySet().equals(actual.keySet()))throw new AssertionError("Original model piece set differs at authoritative sample");
        for(String id:actual.keySet())for(int vertex=0;vertex<8;vertex++)
            if(expected.get(id).vertices().get(vertex).distanceToSqr(actual.get(id).vertices().get(vertex))>1e-8)
                throw new AssertionError("Original model vertex differs at authoritative sample: "+id+" vertex "+vertex);
    }

}
