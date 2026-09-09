package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.platform.anatomy.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;

public class AnatomyGeometryTests {
    @GameTest public void exportedCowRepeatedTransportAndIndependentMovement(GameTestHelper h) throws java.io.IOException {
        String directory=System.getProperty("scalebrews.anatomyCatalog");if(directory==null){h.succeed();return;}
        var model=new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(java.nio.file.Path.of(directory,"minecraft_cow.json")),ModelGeometry.class);
        var support=h.spawn(net.minecraft.world.entity.EntityTypes.COW,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        support.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(2);support.refreshDimensions();
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);player.refreshDimensions();
        var provider=new ModelGeometryProvider(model,new QuadrupedPose(),AnatomyFilter.DEFAULT,1);
        provider.pose(support,new PoseProvider.Inputs(0,0,0,0,0,true));
        AnatomyMovement.activate(h.getLevel());AnatomyMovement.register(support,provider);
        try {
            var back=provider.sample(support).orElseThrow().pieces().get("root/body/cube_0").bounds();
            player.setPos(back.getCenter().x,back.maxY+.2,back.getCenter().z);
            player.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-.4,0));
            h.assertTrue(AnatomyMovement.supported(player),"Actual exported cow back acquires support");
            var origin=support.position();
            for(int step=1;step<=200;step++) {
                support.setPos(origin.add(.2*Math.sin(step*.07),.1*Math.sin(step*.05),.2*Math.cos(step*.07)));
                support.yBodyRot=step*2;
                support.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(2+.1*Math.sin(step*.03));support.refreshDimensions();
                provider.pose(support,new PoseProvider.Inputs(step*.37f,.5f,step,10,5,true));
                AnatomyMovement.carry(player);var carried=player.position();
                AnatomyMovement.carry(player);
                h.assertTrue(player.position().equals(carried),"Repeated carry is idempotent at step "+step);
                h.assertTrue(AnatomyMovement.supported(player),"Contact follows original anatomy through rotation/scale at step "+step);
                var own=new Vec3(step%2==0?.003:-.003,-.03,0);
                player.move(net.minecraft.world.entity.MoverType.SELF,own);
                h.assertTrue(Math.abs(player.getX()-carried.x-own.x)<1e-5 && AnatomyMovement.supported(player),"Independent movement survives transport at step "+step);
            }
        }finally{AnatomyMovement.deactivate(h.getLevel());support.discard();player.discard();}
        h.succeed();
    }
    @GameTest public void sameNetworkIdDoesNotAliasPhysicalState(GameTestHelper h) {
        var first=net.minecraft.world.entity.EntityTypes.COW.create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        var second=net.minecraft.world.entity.EntityTypes.COW.create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        second.setId(first.getId());
        h.assertTrue(first!=second && first.equals(second),"Fixture reproduces vanilla network-ID equality");
        var ticks=new int[2];AnatomyMovement.activate(h.getLevel());
        try {
            AnatomyMovement.gravity(first,new GravityFrame(net.minecraft.core.Direction.UP));
            AnatomyMovement.gravity(second,new GravityFrame(net.minecraft.core.Direction.EAST));
            h.assertTrue(AnatomyMovement.gravity(first).down()==net.minecraft.core.Direction.UP && AnatomyMovement.gravity(second).down()==net.minecraft.core.Direction.EAST,"Same network ID cannot overwrite another instance's gravity");
            for(int i=0;i<2;i++) {
                final int index=i;
                AnatomyMovement.register(i==0?first:second,new GeometryProvider(){
                    public java.util.Optional<Snapshot> sample(net.minecraft.world.entity.LivingEntity e){return java.util.Optional.empty();}
                    public void tick(net.minecraft.world.entity.LivingEntity e,long tick){ticks[index]++;}
                });
            }
            AnatomyMovement.tick(h.getLevel());
            h.assertTrue(ticks[0]==1 && ticks[1]==1,"Distinct instances retain both physical providers");
        }finally{AnatomyMovement.deactivate(h.getLevel());first.discard();second.discard();}
        h.succeed();
    }
    @GameTest public void authoritativeWalkingUsesGravityTangent(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        try {
            for(var direction:net.minecraft.core.Direction.values()) {
                var frame=new GravityFrame(direction);AnatomyMovement.gravity(player,frame);
                var tracker=new AuthorityPoseTracker();player.setPos(0,20,0);tracker.tick(player,0,true);
                player.setPos(player.position().add(frame.gravity().scale(.2)));
                h.assertTrue(tracker.tick(player,1,true).walkAmount()==0,"Falling does not animate walking under "+direction);
                player.setPos(player.position().add(frame.toWorld(new Vec3(.1,0,0))));
                h.assertTrue(Math.abs(tracker.tick(player,2,true).walkAmount()-.16)<1e-6,"Tangent distance drives the same walking speed under "+direction);
                AnatomyMovement.gravity(player,new GravityFrame(direction.getOpposite()));
                h.assertTrue(tracker.tick(player,3,true).walkAmount()==0,"Gravity discontinuity clears prior walking state");
            }
        }finally{AnatomyMovement.gravity(player,GravityFrame.VANILLA);player.discard();}
        h.succeed();
    }
    @GameTest public void rotatingFloorPreservesTangentialMovement(GameTestHelper h) {
        var model=new ModelGeometry(2,"test:floor","1",java.util.List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            java.util.List.of(new ModelGeometry.Piece("floor","root",java.util.List.of(-2d,-1d,-2d),java.util.List.of(2d,0d,2d),null)),ModelGeometry.values(new Matrix4f()));
        var motion=new HierarchyMotion(model,java.util.Map.of(),java.util.Map.of(),new Matrix4f(),new Matrix4f().rotateY(.4f),Vec3.ZERO,new Vec3(0,.5,0),AnatomyFilter.DEFAULT).pieces().get("floor");
        h.assertTrue(motion.invariantPlanes().stream().anyMatch(p->p.outward()==net.minecraft.core.Direction.UP),"Yaw certifies the horizontal plane for the entire trajectory");
        var body=new AABB(-.1,0,-.1,.1,.2,.1);
        var result=TemporalResponse.resolve(body,new Vec3(.3,.5,0),java.util.Map.of("floor",motion),16,256);
        h.assertTrue(result.status()==TemporalResponse.Status.COMPLETE && result.displacement().distanceTo(new Vec3(.3,.5,0))<1e-8,"Yaw and ascent do not repeatedly block tangent movement");
        var interval=motion.interval(.5,1);
        h.assertTrue(ConservativeSweep.query(body.move(0,.25,0),new Vec3(.2,.25,0),interval,256).status()==ConservativeSweep.Status.CLEAR,"Subinterval translates its invariant plane to its actual start");
        var landing=TemporalResponse.resolve(body.move(0,.2,0),new Vec3(.3,-.1,0),java.util.Map.of("floor",motion),32,256);
        h.assertTrue(landing.status()==TemporalResponse.Status.COMPLETE && Math.abs(landing.displacement().y-.3)<2e-6,"Landing on a yawing ascending floor completes after contact");
        var pitched=new HierarchyMotion(model,java.util.Map.of(),java.util.Map.of(),new Matrix4f(),new Matrix4f().rotateX(.4f),Vec3.ZERO,Vec3.ZERO,AnatomyFilter.DEFAULT).pieces().get("floor");
        h.assertFalse(pitched.invariantPlanes().stream().anyMatch(p->p.outward()==net.minecraft.core.Direction.UP),"Pitch never certifies an invariant horizontal plane");
        var scaled=new HierarchyMotion(model,java.util.Map.of(),java.util.Map.of(),new Matrix4f(),new Matrix4f().scale(1.2f),Vec3.ZERO,Vec3.ZERO,AnatomyFilter.DEFAULT).pieces().get("floor");
        h.assertTrue(scaled.invariantPlanes().isEmpty(),"Changing scale does not receive unproven plane certificates");
        h.succeed();
    }
    @GameTest public void resourceReloadAndNetworkBundleRemainAtomic(GameTestHelper h) {
        var model=new ModelGeometry(2,"test:body","1",java.util.List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            java.util.List.of(new ModelGeometry.Piece("body","root",java.util.List.of(0d,0d,0d),java.util.List.of(1d,1d,1d),null)),ModelGeometry.values(new Matrix4f()));
        var profile=new java.util.concurrent.atomic.AtomicReference<>("{\"entity\":\"minecraft:cow\",\"friction\":0.4,\"max_width_ratio\":0.85,\"anatomy\":{\"model\":\"test:body\",\"pose_provider\":\"scalebrews:static\",\"filter\":{\"exclude\":[\"body\"]}}}");
        var pack=h.getLevel().getServer().getResourceManager().listPacks().findFirst().orElseThrow();
        var modelJson=AnatomyCodecs.GEOMETRY.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,model).getOrThrow().toString();
        var resourceType=net.minecraft.server.packs.resources.ResourceManager.class;
        var resources=(net.minecraft.server.packs.resources.ResourceManager)java.lang.reflect.Proxy.newProxyInstance(resourceType.getClassLoader(),new Class<?>[]{resourceType},(proxy,method,args)->{
            if(!method.getName().equals("listResources"))throw new UnsupportedOperationException(method.toString());
            String directory=(String)args[0];String json=directory.endsWith("entity_geometry")?modelJson:profile.get();
            return java.util.Map.of(net.minecraft.resources.Identifier.parse("test:"+directory+"/body.json"),new net.minecraft.server.packs.resources.Resource(pack,()->new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        });
        var world=new WorldAnatomyCatalog();var accepted=world.reload(resources);
        var receiver=new AnatomyCatalogTransfer();
        for(var packet:AnatomyCatalogTransfer.encode(java.util.UUID.randomUUID(),accepted.revision(),accepted.models(),accepted.profiles()))receiver.accept(packet);
        h.assertTrue(receiver.snapshot().models().equals(accepted.models()) && receiver.snapshot().profiles().equals(accepted.profiles()),"Network commits geometry and filtered policies together");
        h.assertTrue(receiver.snapshot().bindings().get(net.minecraft.resources.Identifier.parse("minecraft:cow")).policy().friction()==.4,"Reloaded policy survives independent client validation");
        profile.set("{\"entity\":\"minecraft:cow\",\"anatomy\":{\"model\":\"test:missing\",\"pose_provider\":\"scalebrews:static\"}}");
        boolean rejected=false;try{world.reload(resources);}catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected && world.snapshot()==accepted,"Invalid resource reload retains prior geometry and policies atomically");
        h.succeed();
    }
    @GameTest public void grizzlyRuntimeGuardRejectsTransitionalPoses(GameTestHelper h) throws Exception {
        if(!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("alexsmobs")){h.succeed();return;}
        var type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.parse("alexsmobs:grizzly_bear"));
        var bear=(net.minecraft.world.entity.LivingEntity)type.create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        bear.setPos(h.absoluteVec(new Vec3(2,20,2)));h.getLevel().addFreshEntity(bear);
        var provider=net.minecraft.resources.Identifier.parse("scalebrews:grizzly");
        try {
            h.assertTrue(AnatomyPoseEligibility.supported(provider,bear),"Actual adult ordinary grizzly admitted without renderer classes");
            for(String name:java.util.List.of("standProgress","sitProgress","prevStandProgress","prevSitProgress")) {
                var field=bear.getClass().getField(name);field.setFloat(bear,1);
                h.assertFalse(AnatomyPoseEligibility.supported(provider,bear),"Transition disables ordinary pose: "+name);
                field.setFloat(bear,0);
            }
            h.assertTrue(AnatomyPoseEligibility.supported(provider,bear),"Ordinary pose returns after transition ends");
        } finally {bear.discard();}
        h.succeed();
    }
    @GameTest public void registryJsonAndWorldBindingsAreAtomic(GameTestHelper h) {
        var model=new ModelGeometry(2,"test:body","1",java.util.List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            java.util.List.of(new ModelGeometry.Piece("body","root",java.util.List.of(-1d,0d,-1d),java.util.List.of(1d,2d,1d),null)),ModelGeometry.values(new Matrix4f()));
        var json=AnatomyCodecs.GEOMETRY.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,model).getOrThrow();
        h.assertTrue(AnatomyCodecs.GEOMETRY.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow().equals(model),"Export schema round-trips through registry codec");
        var invalid=json.deepCopy();invalid.getAsJsonObject().getAsJsonArray("parts").get(0).getAsJsonObject().addProperty("parent","root");
        h.assertTrue(AnatomyCodecs.GEOMETRY.parse(com.mojang.serialization.JsonOps.INSTANCE,invalid).error().isPresent(),"Invalid hierarchy produces codec error");
        var profileJson=com.google.gson.JsonParser.parseString("{\"entity\":\"minecraft:cow\",\"max_width_ratio\":0.85,\"anatomy\":{\"model\":\"test:alternate\",\"pose_provider\":\"scalebrews:static\"}}");
        var profile=io.github.r3neer.scalebrews.platform.PlatformDefinition.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,profileJson).getOrThrow();
        h.assertTrue(profile.surfaces().isEmpty() && profile.maxRatio().orElseThrow()==.85,"Anatomical profile preserves policy without fabricated top plane");
        var world=new WorldAnatomyCatalog();var accepted=world.replace(java.util.Map.of("test:alternate",model),java.util.Map.of("test:cow",profile));
        h.assertTrue(accepted.bindings().get(net.minecraft.resources.Identifier.parse("minecraft:cow")).model().equals(model),"World explicitly selects alternative geometry");
        boolean missing=false;try{world.replace(java.util.Map.of(),java.util.Map.of("test:cow",profile));}catch(IllegalArgumentException expected){missing=true;}
        h.assertTrue(missing && world.snapshot()==accepted,"Missing references retain entire previous snapshot");
        boolean duplicate=false;try{world.replace(accepted.models(),java.util.Map.of("test:a",profile,"test:b",profile));}catch(IllegalArgumentException expected){duplicate=true;}
        h.assertTrue(duplicate && world.snapshot()==accepted,"Duplicate species cannot partially replace catalog");
        profileJson.getAsJsonObject().getAsJsonObject("anatomy").addProperty("pose_provider","test:missing");
        var unknownProfile=io.github.r3neer.scalebrews.platform.PlatformDefinition.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,profileJson).getOrThrow();
        boolean unknown=false;try{world.replace(accepted.models(),java.util.Map.of("test:cow",unknownProfile));}catch(IllegalArgumentException expected){unknown=true;}
        h.assertTrue(unknown && world.snapshot()==accepted,"Unknown pose provider is not static fallback");
        h.assertTrue(h.getLevel().registryAccess().lookup(io.github.r3neer.scalebrews.platform.Platforms.GEOMETRIES).isPresent(),"Geometry registry exists on dedicated server");
        h.succeed();
    }
    @GameTest public void explicitPieceFiltersRetainOnlyValidAnatomy(GameTestHelper h) {
        var thin=new ModelGeometry.Piece("thin","root",java.util.List.of(0d,0d,0d),java.util.List.of(1d,.001,1d),null);
        var flat=new ModelGeometry.Piece("flat","root",java.util.List.of(0d,0d,0d),java.util.List.of(1d,0d,1d),null);
        var cosmetic=new ModelGeometry.Piece("coat","root",java.util.List.of(0d,0d,0d),java.util.List.of(1d,1d,1d),"equipment");
        var explicit=new AnatomyFilter(.5/16,.025,.00001,java.util.Set.of("thin","flat","coat"),java.util.Set.of());
        h.assertTrue(AnatomyFilter.DEFAULT.rejection(thin,1).equals("thickness") && explicit.rejection(thin,1)==null,"Explicit inclusion bypasses numeric filter");
        h.assertTrue(explicit.rejection(flat,1).equals("degenerate") && explicit.rejection(cosmetic,1).equals("equipment"),"Degenerate and equipment remain nonphysical");
        var excluded=new AnatomyFilter(0,0,0,java.util.Set.of(),java.util.Set.of("root"));
        h.assertTrue(excluded.rejection(thin,1).equals("explicit_exclusion"),"Part IDs select associated cubes");
        var conflicting=com.google.gson.JsonParser.parseString("{\"include\":[\"root\"],\"exclude\":[\"root\"]}");
        h.assertTrue(AnatomyCodecs.FILTER.parse(com.mojang.serialization.JsonOps.INSTANCE,conflicting).error().isPresent(),"Conflicting filter decisions rejected");
        h.succeed();
    }
    @GameTest public void gravityOrientsGeometryAndResetsPoseHistory(GameTestHelper h) {
        var model=new ModelGeometry(1,"test:head","1",java.util.List.of(new ModelGeometry.Part("head",null,ModelGeometry.values(new Matrix4f()))),
            java.util.List.of(new ModelGeometry.Piece("head","head",java.util.List.of(-.2d,1.2d,-.2d),java.util.List.of(.2d,1.6d,.2d),null))).withModelTransform(new Matrix4f());
        var inputs=new PoseProvider.Inputs(0,0,0,0,0,true);var entity=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var evaluator=new ModelGeometryProvider(model,(g,p)->java.util.Optional.of(java.util.Map.of()),AnatomyFilter.DEFAULT,1);
        var base=evaluator.sampleAt(entity,new AnatomyPoseHistory.Sample(inputs,Vec3.ZERO,180,1)).orElseThrow().pieces().get("head");
        try {
            for(var direction:net.minecraft.core.Direction.values()) {
                var frame=new GravityFrame(direction);
                var rotated=evaluator.sampleAt(entity,new AnatomyPoseHistory.Sample(inputs,Vec3.ZERO,180,1,frame)).orElseThrow().pieces().get("head");
                for(int i=0;i<8;i++)h.assertTrue(rotated.vertices().get(i).distanceToSqr(frame.toWorld(base.vertices().get(i)))<1e-12,"Model pieces rotate in the actual gravity frame: "+direction);
            }
            var id=net.minecraft.resources.Identifier.parse("test:head");var dim=net.minecraft.resources.Identifier.parse("minecraft:overworld");var epoch=java.util.UUID.randomUUID();
            var a=new AnatomyPosePayload(epoch,1,dim,entity.getId(),entity.getUUID(),id,id,1,inputs,Vec3.ZERO,180,1,net.minecraft.core.Direction.DOWN);
            var b=new AnatomyPosePayload(epoch,1,dim,entity.getId(),entity.getUUID(),id,id,2,inputs,Vec3.ZERO,180,1,net.minecraft.core.Direction.UP);
            var history=new AnatomyPoseHistory();history.accept(a);history.accept(b);var segment=history.segment(1.5);
            h.assertTrue(segment.before().equals(segment.after()) && segment.after().gravity().down()==net.minecraft.core.Direction.UP,"Cardinal change resets physical interpolation without intermediate gravity");
        }finally{entity.discard();}h.succeed();
    }
    @GameTest public void cardinalGravityBasisAndInstalledAdapter(GameTestHelper h) {
        var probe=new Vec3(.3,-.7,1.2);
        for(var direction:net.minecraft.core.Direction.values()) {
            var frame=new GravityFrame(direction);
            h.assertTrue(frame.toLocal(frame.toWorld(probe)).distanceToSqr(probe)<1e-20,"Cardinal coordinate round trip: "+direction);
            h.assertTrue(frame.toWorld(new Vec3(0,-1,0)).distanceToSqr(frame.gravity())<1e-20,"Local gravity maps to world down direction");
        }
        if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("gravity_changer"))try {
            var utility=Class.forName("com.moigferdsrte.gravitychanger.util.GravityDirectionUtil");
            var get=utility.getMethod("getGravityDirection",net.minecraft.world.entity.Entity.class);
            var set=utility.getMethod("setGravityDirection",net.minecraft.world.entity.LivingEntity.class,net.minecraft.core.Direction.class);
            var rotation=Class.forName("com.moigferdsrte.gravitychanger.util.RotationUtil").getMethod("vecPlayerToWorld",Vec3.class,net.minecraft.core.Direction.class);
            GravityFrames.install("gravity_changer_test",entity->{try{return new GravityFrame((net.minecraft.core.Direction)get.invoke(null,entity));}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}});
            var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            try {
                for(var direction:net.minecraft.core.Direction.values()) {
                    set.invoke(null,player,direction);var frame=AnatomyMovement.gravity(player);
                    h.assertTrue(frame.down()==direction,"Core reads actual Gravity Changer state");
                    h.assertTrue(frame.toWorld(probe).distanceToSqr((Vec3)rotation.invoke(null,probe,direction))<1e-20,"Basis matches actual upstream implementation");
                }
            }finally{set.invoke(null,player,net.minecraft.core.Direction.DOWN);player.discard();}
            System.out.println("ANATOMY_GRAVITY actual Gravity Changer adapter and all six coordinate bases passed");
        }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        h.succeed();
    }
    @GameTest public void separationRespectsObstructionsAndDistanceBudget(GameTestHelper h) {
        var cube=ConvexBox.of(new AABB(-1,-1,-1,1,1,1),new Matrix4f());var body=new AABB(.9,-.1,-.1,1.1,.1,.1);
        var free=AnatomySeparation.resolve(body,java.util.List.of(cube),4,128,(box,delta)->delta);
        h.assertTrue(free.separated() && Math.abs(free.displacement().x-.100001)<1e-6 && !cube.overlaps(body.move(free.displacement())),"Shortest free SAT exit is selected");
        var alternate=AnatomySeparation.resolve(body,java.util.List.of(cube),4,128,(box,delta)->delta.x>0?Vec3.ZERO:delta);
        h.assertTrue(alternate.separated() && alternate.displacement().x<=0,"Blocked nearest exit tries another validated exit");
        var trapped=AnatomySeparation.resolve(body,java.util.List.of(cube),4,128,(box,delta)->Vec3.ZERO);
        h.assertTrue(!trapped.separated() && trapped.displacement().equals(Vec3.ZERO),"No unchecked escape through surrounding blocks");
        var distant=AnatomySeparation.resolve(body,java.util.List.of(cube),.01,128,(box,delta)->delta);
        h.assertTrue(!distant.separated(),"No large repositioning beyond the explicit recovery bound");h.succeed();
    }
    @GameTest public void trappedPairSuspendsAndReacquiresIndependently(GameTestHelper h) {
        var support=h.spawn(net.minecraft.world.entity.EntityTypes.COW,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var other=h.spawn(net.minecraft.world.entity.EntityTypes.COW,3,20,2);other.setNoAi(true);other.setNoGravity(true);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);player.refreshDimensions();
        player.setPos(support.position());var enclosing=ConvexBox.of(new AABB(-10,-10,-10,10,10,10),new Matrix4f()).move(support.position());
        AnatomyMovement.activate(h.getLevel());AnatomyMovement.register(support,e->java.util.Optional.of(new GeometryProvider.Snapshot(1,java.util.Map.of("body",enclosing))));
        try {
            var position=player.position();player.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(.01,0,0));
            h.assertTrue(AnatomyMovement.suspended(player,support) && !AnatomyMovement.suspended(player,other),"Only unresolvable pair is suspended");
            h.assertTrue(player.position().distanceTo(position)<.1,"Recovery did not teleport out of enclosing anatomy");
            AnatomyMovement.register(support,e->java.util.Optional.of(new GeometryProvider.Snapshot(2,java.util.Map.of("body",enclosing.move(new Vec3(30,0,0))))));
            h.assertTrue(!AnatomyMovement.suspended(player,support),"Pair re-enables after actual overlap ends");
        }finally{AnatomyMovement.deactivate(h.getLevel());support.discard();other.discard();player.discard();}h.succeed();
    }
    @GameTest public void actualEntityMovesWithAscendingAnatomy(GameTestHelper h) {
        var support=h.spawn(net.minecraft.world.entity.EntityTypes.COW,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);player.refreshDimensions();
        var center=support.position();var floor=ConvexBox.of(new AABB(-1,-1,-1,1,0,1),new Matrix4f()).move(center);
        var motion=new ConservativeSweep.Motion(t->floor.move(new Vec3(0,t,0)),0,new Vec3(0,1,0));
        player.setPos(center.x,center.y+.5,center.z);player.setOnGround(false);
        AnatomyMovement.activate(h.getLevel());AnatomyMovement.register(support,new GeometryProvider(){
            public java.util.Optional<Snapshot> sample(net.minecraft.world.entity.LivingEntity e){return java.util.Optional.of(new Snapshot(1,java.util.Map.of("back",motion.at().apply(1))));}
            public java.util.Optional<MotionSnapshot> motion(net.minecraft.world.entity.LivingEntity e){return java.util.Optional.of(new MotionSnapshot(1,e.level().getGameTime(),java.util.Map.of("back",motion)));}
        });
        try {
            player.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(.2,0,0));
            h.assertTrue(Math.abs(player.getY()-center.y-1)<1e-6 && Math.abs(player.getX()-center.x-.2)<1e-6,"Real entity follows rising surface and preserves its own movement");
            h.assertTrue(player.onGround() && AnatomyMovement.surface(player)!=null,"Real moving contact acquires a material face anchor");
            h.assertTrue(!motion.at().apply(1).overlaps(player.getBoundingBox()),"No overlap after actual movement");
        }finally{AnatomyMovement.deactivate(h.getLevel());support.discard();player.discard();}
        h.succeed();
    }
    @GameTest public void movingContactResponsePreservesRelativeMotion(GameTestHelper h) {
        var floor=ConvexBox.of(new AABB(-2,-1,-2,2,0,2),new Matrix4f());
        var rising=new ConservativeSweep.Motion(t->floor.move(new Vec3(0,t,0)),0,new Vec3(0,1,0));
        var body=new AABB(-.1,.5,-.1,.1,.7,.1);
        var result=TemporalResponse.resolve(body,new Vec3(.4,0,0),java.util.Map.of("floor",rising),16,128);
        h.assertTrue(result.status()==TemporalResponse.Status.COMPLETE && Math.abs(result.displacement().x-.4)<1e-7 && Math.abs(result.displacement().y-.5)<1e-7,"Rising support pushes body while preserving independent tangent movement");
        h.assertTrue(!rising.at().apply(1).overlaps(body.move(result.displacement())),"Response finishes outside moving surface");
        var following=ConservativeSweep.query(new AABB(-.1,0,-.1,.1,.2,.1),new Vec3(.3,1,0),rising,1);
        h.assertTrue(following.status()==ConservativeSweep.Status.CLEAR,"Shared translation cancels before sweep and permits tangent movement at contact");
        var falling=new ConservativeSweep.Motion(t->floor.move(new Vec3(0,-t,0)),0,new Vec3(0,-1,0));
        var downward=TemporalResponse.resolve(body,new Vec3(0,-2,0),java.util.Map.of("floor",falling),16,128);
        h.assertTrue(downward.status()==TemporalResponse.Status.COMPLETE && Math.abs(downward.displacement().y+1.5)<1e-7,"Descending support preserves relative landing time");
        var overlap=TemporalResponse.resolve(new AABB(-.1,-.2,-.1,.1,.2,.1),Vec3.ZERO,java.util.Map.of("floor",rising),16,128);
        h.assertTrue(overlap.status()==TemporalResponse.Status.INITIAL_OVERLAP && overlap.displacement().equals(Vec3.ZERO),"Overlap requires separation policy, never blind movement");
        h.succeed();
    }
    @GameTest public void spatialTemporalQueryFindsIntermediateLimb(GameTestHelper h) {
        var support=h.spawn(net.minecraft.world.entity.EntityTypes.COW,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);player.refreshDimensions();
        var center=support.position().add(0,.6,0);player.setPos(center.x,center.y-player.getBbHeight()/2,center.z+.8);
        var motion=new ConservativeSweep.Motion(t->ConvexBox.of(new AABB(-1,-.1,-.1,1,.1,.1),new Matrix4f().rotateY((float)(Math.PI*t))).move(center),3.3);
        AnatomyMovement.activate(h.getLevel());
        AnatomyMovement.register(support,new GeometryProvider(){
            public java.util.Optional<Snapshot> sample(net.minecraft.world.entity.LivingEntity e){return java.util.Optional.of(new Snapshot(1,java.util.Map.of("limb",motion.at().apply(1))));}
            public java.util.Optional<MotionSnapshot> motion(net.minecraft.world.entity.LivingEntity e){return java.util.Optional.of(new MotionSnapshot(1,e.level().getGameTime(),java.util.Map.of("limb",motion)));}
        });
        try {
            h.assertTrue(!motion.at().apply(0).overlaps(player.getBoundingBox()) && !motion.at().apply(1).overlaps(player.getBoundingBox()),"Endpoint anatomy is clear");
            var hit=AnatomyMovement.sweep(player,Vec3.ZERO,4096);
            h.assertTrue(hit!=null && hit.support()==support && hit.piece().equals("limb") && hit.result().status()==ConservativeSweep.Status.CONTACT,"Spatial movement query finds the intermediate anatomical limb");
            var limited=AnatomyMovement.sweep(player,Vec3.ZERO,1);
            h.assertTrue(limited!=null && limited.result().status()==ConservativeSweep.Status.ITERATION_LIMIT,"Spatial query propagates failure instead of reporting clear");
            var metrics=AnatomyMovement.sweepMetrics(h.getLevel());
            h.assertTrue(metrics.queries()>=2 && metrics.pieces()>=2 && metrics.evaluations()>2 && metrics.exhausted()>=1,"Temporal query records work and exhausted budgets");
        }finally{AnatomyMovement.deactivate(h.getLevel());support.discard();player.discard();}
        h.succeed();
    }
    @GameTest public void hierarchyMotionBoundsRotationAndScale(GameTestHelper h) {
        var model=new ModelGeometry(1,"test:rotor","1",java.util.List.of(
            new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f())),
            new ModelGeometry.Part("tip","root",ModelGeometry.values(new Matrix4f().translation(2,0,0)))),
            java.util.List.of(new ModelGeometry.Piece("tip","tip",java.util.List.of(-.1d,-.1d,-.1d),java.util.List.of(.1d,.1d,.1d),null)));
        var before=java.util.Map.of("root",new Matrix4f().rotateZ(-1.4f));
        var after=java.util.Map.of("root",new Matrix4f().rotateZ(1.4f).scale(1.2f));
        var motion=new HierarchyMotion(model,before,after,new Matrix4f(),new Matrix4f(),Vec3.ZERO,Vec3.ZERO,AnatomyFilter.DEFAULT).pieces().get("tip");
        var target=new AABB(2.05,-.15,-.15,2.35,.15,.15);
        h.assertTrue(!motion.at().apply(0).overlaps(target) && !motion.at().apply(1).overlaps(target),"Both endpoint poses miss the target");
        var hit=ConservativeSweep.query(target,Vec3.ZERO,motion,4096);
        h.assertTrue(hit.status()==ConservativeSweep.Status.CONTACT && hit.safeFraction()>0 && hit.safeFraction()<1,"Hierarchy sweep detects intermediate rotation");
        var previous=motion.at().apply(0);
        for(int step=1;step<=1000;step++) {
            var next=motion.at().apply(step/1000d);
            for(int vertex=0;vertex<8;vertex++)h.assertTrue(next.vertices().get(vertex).distanceTo(previous.vertices().get(vertex))*1000<=motion.maxPointSpeed()+.001,"Material vertex stays within analytic hierarchy speed bound");
            previous=next;
        }
        boolean rejected=false;
        try{new HierarchyMotion(model,before,java.util.Map.of("root",new Matrix4f().m10(.5f)),new Matrix4f(),new Matrix4f(),Vec3.ZERO,Vec3.ZERO,AnatomyFilter.DEFAULT);}
        catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected,"Unsupported animated shear is not silently approximated");h.succeed();
    }
    @GameTest public void poseHistoryRejectsReplayAndDiscontinuity(GameTestHelper h) {
        var epoch=java.util.UUID.randomUUID();var entity=java.util.UUID.randomUUID();var dimension=net.minecraft.resources.Identifier.parse("minecraft:overworld");
        var model=net.minecraft.resources.Identifier.parse("minecraft:player_wide");var provider=net.minecraft.resources.Identifier.parse("scalebrews:player_walking");
        var a=new AnatomyPosePayload(epoch,1,dimension,1,entity,model,provider,10,new PoseProvider.Inputs(0,0,10,179,0,true),Vec3.ZERO,179,1);
        var b=new AnatomyPosePayload(epoch,1,dimension,1,entity,model,provider,11,new PoseProvider.Inputs(1,1,11,-179,10,true),new Vec3(1,0,0),-179,2);
        var history=new AnatomyPoseHistory();history.accept(a);history.accept(b);
        var mid=history.sample(10.5);
        h.assertTrue(Math.abs(mid.origin().x-.5)<1e-6 && Math.abs(mid.scale()-1.5)<1e-6 && Math.abs(Math.abs(mid.yaw())-180)<1e-6,"Interpolate channels with shortest-angle rotation");
        h.assertTrue(!history.accept(a) && history.current()==b,"Stale frame cannot rewind state");
        for(int i=0;i<100;i++)h.assertTrue(history.sample(10.5).equals(mid),"Sampling is independent of FPS/query count");
        var jump=new AnatomyPosePayload(epoch,1,dimension,1,entity,model,provider,12,b.inputs(),new Vec3(10,0,0),0,1);history.accept(jump);
        h.assertTrue(history.sample(11.5).origin().x==10,"Teleport-sized discontinuity is not smoothed");
        boolean rejected=false;
        try{history.accept(new AnatomyPosePayload(epoch,2,dimension,1,entity,model,provider,13,b.inputs(),Vec3.ZERO,0,1));}catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected,"Catalog revision change requires explicit state reset");h.succeed();
    }
    @GameTest public void catalogFragmentsAreAtomic(GameTestHelper h) {
        var geometry=new ModelGeometry(1,"test:box","1",java.util.List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            java.util.List.of(new ModelGeometry.Piece("body","root",java.util.List.of(0d,0d,0d),java.util.List.of(1d,1d,1d),null)));
        var models=new java.util.TreeMap<String,ModelGeometry>();for(int i=0;i<100;i++)models.put("test:model_"+i,geometry);
        var epoch=java.util.UUID.randomUUID();var packets=AnatomyCatalogTransfer.encode(epoch,1,models);
        h.assertTrue(packets.size()>1,"Fixture exercises fragmented transfer");
        var receiver=new AnatomyCatalogTransfer();
        for(int index=packets.size()-1;index>0;index--) {
            h.assertTrue(!receiver.accept(packets.get(index)),"Incomplete catalog is not published");
            h.assertTrue(!receiver.accept(packets.get(index)),"Identical duplicate is idempotent");
        }
        h.assertTrue(receiver.snapshot().models().isEmpty(),"No partial catalog leaks");
        h.assertTrue(receiver.accept(packets.getFirst()) && receiver.snapshot().models().equals(models),"Complete out-of-order catalog is applied atomically");
        h.assertTrue(!receiver.accept(packets.getFirst()),"Old revision cannot replay");
        var previous=receiver.snapshot();var corrupt=AnatomyCatalogTransfer.encode(epoch,2,models);
        boolean rejected=false;
        try {
            for(int i=0;i<corrupt.size();i++) {
                var p=corrupt.get(i);byte[] data=p.fragment();if(i==0)data[0]^=1;
                receiver.accept(new AnatomyCatalogPayload(p.epoch(),p.revision(),p.index(),p.count(),p.totalBytes(),p.digest(),data));
            }
        }catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected && receiver.snapshot()==previous && receiver.revision()==1,"Corrupt transfer preserves accepted catalog");
        for(var p:AnatomyCatalogTransfer.encode(epoch,3,models))receiver.accept(p);
        h.assertTrue(receiver.revision()==3,"Later valid revision can recover after rejection");
        h.succeed();
    }
    @GameTest public void materialFaceIdentity(GameTestHelper h) {
        var box=ConvexBox.of(new AABB(-1,-1,-1,1,1,1),new Matrix4f().rotateY(.4f).m10(.3f));
        for(int face=0;face<6;face++) {
            Vec3 local=box.facePoint(face,box.bounds().getCenter()),point=box.point(local),normal=box.faceNormal(face);
            var hit=box.raycast(point.add(normal.scale(3)),point.subtract(normal.scale(.01)));
            h.assertTrue(hit!=null && hit.face()==face && hit.normal().dot(normal)>.999999,"Ray identifies true affine face "+face);
            var contact=new SurfaceContact(java.util.UUID.randomUUID(),1,"body",face,hit.localPoint(),hit.normal(),0);
            h.assertTrue(box.point(contact.localPoint()).distanceToSqr(point)<1e-9,"Material contact survives local/world conversion");
        }
        h.assertTrue(GravityFrame.dominant(new Vec3(1,1,0)).isEmpty(),"Ambiguous diagonal cannot silently choose gravity");
        h.assertTrue(GravityFrame.dominant(new Vec3(.1,-.9,0)).orElseThrow()==net.minecraft.core.Direction.DOWN,"Dominant normal keeps cardinal gravity");
        h.succeed();
    }
    @GameTest public void exportedPlayerHeadCarriesBoat(GameTestHelper h) {
        String directory=System.getProperty("scalebrews.anatomyCatalog");
        if(directory==null){h.succeed();return;}
        ModelGeometry geometry;
        try{geometry=new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(java.nio.file.Path.of(directory,"minecraft_player_wide.json")),ModelGeometry.class);}
        catch(java.io.IOException e){throw new RuntimeException(e);}
        h.assertTrue(geometry.format()==2 && Math.abs(ModelGeometry.matrix(geometry.modelTransform()).m00()+.9375)<1e-7,"Player renderer root scale is catalog data");
        var giant=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        giant.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(3);giant.refreshDimensions();
        giant.setPos(h.absoluteVec(new Vec3(2,20,2)));giant.yBodyRot=0;giant.setNoGravity(true);h.getLevel().addFreshEntity(giant);
        var provider=new ModelGeometryProvider(geometry,new PlayerWalkingPose(),AnatomyFilter.DEFAULT,1);
        provider.pose(giant,new PoseProvider.Inputs(0,0,0,0,0,true));
        var head=provider.sample(giant).orElseThrow().pieces().get("root/head/cube_0");
        var boat=h.spawn(net.minecraft.world.entity.EntityTypes.OAK_BOAT,2,27,2);
        var rider=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);rider.startRiding(boat,true,true);
        AnatomyMovement.activate(h.getLevel());AnatomyMovement.register(giant,provider);
        try {
            var center=head.bounds().getCenter();boat.setPos(center.x,head.bounds().maxY+1,center.z);
            boat.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-2,0));
            h.assertTrue(AnatomyMovement.contact(boat)!=null && AnatomyMovement.contact(boat).piece().equals("root/head/cube_0"),"Occupied boat lands on exported player head, not bounding-box top");
            h.assertTrue(Math.abs(boat.getY()-head.bounds().maxY)<1e-5,"Contact height matches original player renderer");
            var before=boat.position();giant.setPos(giant.position().add(.2,.1,.3));AnatomyMovement.carry(boat);
            h.assertTrue(boat.position().distanceToSqr(before.add(.2,.1,.3))<1e-9,"Original head contact transports occupied boat");
        } finally {AnatomyMovement.deactivate(h.getLevel());boat.discard();rider.discard();giant.discard();}
        h.succeed();
    }
    @GameTest public void authorityClockDoesNotDependOnQueries(GameTestHelper h) {
        var cow=h.spawn(net.minecraft.world.entity.EntityTypes.COW,2,20,2);cow.setNoAi(true);cow.setNoGravity(true);
        var tracker=new AuthorityPoseTracker();var reference=new net.minecraft.world.entity.WalkAnimationState();
        tracker.tick(cow,0,true);
        for(int tick=1;tick<=20;tick++) {
            var before=cow.position();cow.setPos(before.add(.1,0,0));
            float distance=(float)cow.position().distanceTo(before);
            reference.update(Math.min(distance*4,1),.4f,1);
            var pose=tracker.tick(cow,tick,true);
            h.assertTrue(Math.abs(pose.walkPhase()-reference.position())<1e-6 && Math.abs(pose.walkAmount()-reference.speed())<1e-6,"Authority reproduces ordinary vanilla walk clock");
            for(int observer=0;observer<50;observer++)h.assertTrue(tracker.tick(cow,tick,true)==pose,"Queries cannot advance authoritative phase");
        }
        cow.setPos(cow.position().add(10,0,0));
        h.assertTrue(tracker.tick(cow,21,true).walkAmount()==0,"Discontinuities reset locomotion instead of replaying accumulated motion");
        h.assertTrue(!tracker.tick(cow,22,false).ordinary(),"Unsupported pose remains explicitly unavailable");
        cow.discard();h.succeed();
    }
    @GameTest public void exportedCowActualMovement(GameTestHelper h) {
        String directory=System.getProperty("scalebrews.anatomyCatalog");
        if(directory==null){h.succeed();return;}
        ModelGeometry geometry;
        try{geometry=new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(java.nio.file.Path.of(directory,"minecraft_cow.json")),ModelGeometry.class);}
        catch(java.io.IOException e){throw new RuntimeException(e);}
        var support=h.spawn(net.minecraft.world.entity.EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);support.yBodyRot=0;
        var provider=new ModelGeometryProvider(geometry,new QuadrupedPose(),AnatomyFilter.DEFAULT,1);
        provider.pose(support,new PoseProvider.Inputs(0,0,0,0,0,true));
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.1);player.refreshDimensions();
        AnatomyMovement.activate(h.getLevel());AnatomyMovement.register(support,provider);
        try {
            var pieces=provider.sample(support).orElseThrow().pieces();
            var torso=pieces.entrySet().stream().filter(e->e.getKey().contains("/body/")).findFirst().orElseThrow().getValue();
            var center=torso.bounds().getCenter();
            for(var outward:net.minecraft.core.Direction.values()) {
                var n=new Vec3(outward.getStepX(),outward.getStepY(),outward.getStepZ());
                AnatomyMovement.gravity(player,new GravityFrame(outward.getOpposite()));
                Vec3 start=center.add(n.scale(5));
                player.setPos(start.x,start.y-player.getBbHeight()/2,start.z);
                AnatomyMovement.clear(player);player.setOnGround(false);player.setDeltaMovement(Vec3.ZERO);
                var beforeBox=player.getBoundingBox();
                player.move(net.minecraft.world.entity.MoverType.SELF,n.scale(-10));
                if(AnatomyMovement.contact(player)==null)System.out.println("ANATOMY_COW_MISS "+outward+" start="+beforeBox+" end="+player.getBoundingBox()+" hits="+pieces.entrySet().stream().map(e->e.getKey()+":"+e.getValue().sweep(beforeBox,n.scale(-10))+" gap="+e.getValue().separation(player.getBoundingBox())).toList());
                h.assertTrue(AnatomyMovement.contact(player)!=null,"Original cow geometry catches the moving entity: "+outward);
                h.assertTrue(pieces.values().stream().noneMatch(p->p.overlaps(player.getBoundingBox())),"No penetration of original model: "+outward);
            }
            AnatomyMovement.clear(player);AnatomyMovement.gravity(player,new GravityFrame(net.minecraft.core.Direction.UP));
            player.setPos(support.position().add(0,-.3,0));
            player.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,.6,0));
            h.assertTrue(AnatomyMovement.contact(player)==null,"Original cow limb gap remains open below belly");
            long evaluations=provider.evaluations();
            for(int i=0;i<50;i++)provider.sample(support);
            h.assertTrue(provider.evaluations()==evaluations,"Repeated observers/queries share one geometry evaluation");
            provider.pose(support,new PoseProvider.Inputs(0,0,0,0,0,false));
            h.assertTrue(provider.sample(support).isEmpty(),"Unsupported pose removes anatomy instead of freezing it");
        } finally {AnatomyMovement.deactivate(h.getLevel());support.discard();player.discard();}
        h.succeed();
    }
    @GameTest public void gravityChangesInvalidateMaterialTransport(GameTestHelper h) {
        var support=h.spawn(net.minecraft.world.entity.EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);player.refreshDimensions();
        GeometryProvider provider=e->java.util.Optional.of(new GeometryProvider.Snapshot(1,java.util.Map.of("body",
            ConvexBox.of(new AABB(-1,0,-1,1,1,1),new Matrix4f()).move(e.position()))));
        AnatomyMovement.activate(h.getLevel());AnatomyMovement.register(support,provider);
        try {
            for(boolean changeSupport:new boolean[]{true,false}) {
                AnatomyMovement.gravity(support,GravityFrame.VANILLA);AnatomyMovement.gravity(player,GravityFrame.VANILLA);
                player.setPos(support.position().add(0,2,0));player.setDeltaMovement(Vec3.ZERO);
                player.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-2,0));
                h.assertTrue(AnatomyMovement.supported(player),"Fixture acquires material contact");
                var before=player.position();
                AnatomyMovement.gravity(changeSupport?support:player,new GravityFrame(net.minecraft.core.Direction.EAST));
                support.setPos(support.position().add(.3,0,0));
                AnatomyMovement.carry(player);
                h.assertTrue(AnatomyMovement.contact(player)==null,"Cardinal discontinuity releases old material anchor");
                h.assertTrue(player.position().equals(before),"Gravity change cannot replay previous-frame transport");
            }
        } finally {AnatomyMovement.deactivate(h.getLevel());support.discard();player.discard();}
        h.succeed();
    }
    @GameTest public void anatomicalRootTransportOnce(GameTestHelper h) {
        var support=h.spawn(net.minecraft.world.entity.EntityTypes.COW,2,20,2);
        support.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        support.setNoAi(true);support.setNoGravity(true);
        GeometryProvider provider=e->java.util.Optional.of(new GeometryProvider.Snapshot(1,java.util.Map.of("body",
            ConvexBox.of(new AABB(-2,0,-2,2,2,2),new Matrix4f().rotateY((float)Math.toRadians(e.yBodyRot))).move(e.position()))));
        AnatomyMovement.activate(h.getLevel());AnatomyMovement.register(support,provider);
        var boat=h.spawn(net.minecraft.world.entity.EntityTypes.OAK_BOAT,2,23,2);
        var rider=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        rider.startRiding(boat,true,true);
        try {
            boat.setPos(support.position().add(.7,3,.4));
            boat.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-2,0));
            h.assertTrue(AnatomyMovement.contact(boat)!=null,"Occupied boat lands on convex geometry through Entity.move");
            h.assertTrue(io.github.r3neer.scalebrews.platform.PlatformPhysics.touching(boat),"Network grounding query sees anatomical contact without legacy surface state");
            boat.positionRider(rider);var riderBefore=rider.position();
            var before=boat.position();var oldBox=provider.sample(support).orElseThrow().pieces().get("body");
            Vec3 local=oldBox.coordinates(before);
            support.setPos(support.position().add(.2,.1,.3));support.yBodyRot+=20;
            var expected=provider.sample(support).orElseThrow().pieces().get("body").point(local);
            AnatomyMovement.carry(boat);
            h.assertTrue(boat.position().distanceToSqr(expected)<1e-9,"Material contact follows translation and rotation");
            h.assertTrue(rider.position().subtract(riderBefore).distanceToSqr(expected.subtract(before))<1e-9,"Native passenger seat follows the transported root immediately");
            var riderAfter=rider.position();
            AnatomyMovement.carry(boat);
            h.assertTrue(boat.position().distanceToSqr(expected)<1e-9,"Repeated carry cannot duplicate transport");
            h.assertTrue(rider.position().equals(riderAfter) && AnatomyMovement.transport(rider)==null,"Passenger placement neither duplicates displacement nor creates independent transport");
            h.assertTrue(AnatomyMovement.transport(boat).displacement().distanceToSqr(expected.subtract(before))<1e-9,"Passive displacement is accounted separately");
            h.assertTrue(rider.getVehicle()==boat && AnatomyMovement.contact(rider)==null,"Vehicle root carries without independent passenger support");
        } finally {AnatomyMovement.deactivate(h.getLevel());boat.discard();rider.discard();support.discard();}
        h.succeed();
    }
    @GameTest public void actualMovementSixDirections(GameTestHelper h) {
        var support=h.spawn(net.minecraft.world.entity.EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        var center=support.position().add(0,.5,0);
        var box=ConvexBox.of(new AABB(-1,-1,-1,1,1,1),new Matrix4f()).move(center);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);
        player.refreshDimensions();
        AnatomyMovement.activate(h.getLevel());
        AnatomyMovement.register(support,e->java.util.Optional.of(new GeometryProvider.Snapshot(1,java.util.Map.of("body",box))));
        try {
            for(var outward:net.minecraft.core.Direction.values()) {
                var n=new Vec3(outward.getStepX(),outward.getStepY(),outward.getStepZ());
                AnatomyMovement.gravity(player,new GravityFrame(outward.getOpposite()));
                Vec3 start=center.add(n.scale(3));
                player.setPos(start.x,start.y-player.getBbHeight()/2,start.z);
                player.setOnGround(false);player.setDeltaMovement(Vec3.ZERO);
                player.move(net.minecraft.world.entity.MoverType.SELF,n.scale(-4));
                h.assertTrue(!box.overlaps(player.getBoundingBox()),"Entity.move must not enter anatomy: "+outward);
                h.assertTrue(AnatomyMovement.contact(player)!=null && player.onGround(),"Actual entity acquires gravity-relative support: "+outward);
                h.assertTrue(box.separation(player.getBoundingBox()).gap()<1e-4,"Entity stops at anatomy, not its whole bounding box: "+outward);
            }
            h.assertTrue(h.getLevel().noCollision(player,new AABB(center.x-.02,center.y-.02,center.z-.02,center.x+.02,center.y+.02,center.z+.02)),"Global noCollision retains vanilla behavior inside an anatomical box");
        } finally {AnatomyMovement.deactivate(h.getLevel());support.discard();player.discard();}
        h.succeed();
    }
    @GameTest public void temporalRotationAndFailClosed(GameTestHelper h) {
        AABB limb=new AABB(-1,-.1,-.1,1,.1,.1);
        AABB body=new AABB(-.05,-.05,.75,.05,.05,.85);
        // Rotating bar crosses the body even though both endpoint poses are clear.
        var motion=new ConservativeSweep.Motion(t->ConvexBox.of(limb,new Matrix4f().rotateY((float)(Math.PI*t))),3.3);
        var hit=ConservativeSweep.query(body,Vec3.ZERO,motion,128);
        h.assertTrue(hit.status()==ConservativeSweep.Status.CONTACT && hit.safeFraction()>0 && hit.safeFraction()<.5,"Detect rotation between clear endpoints");
        var limited=ConservativeSweep.query(body,Vec3.ZERO,motion,1);
        h.assertTrue(limited.status()==ConservativeSweep.Status.ITERATION_LIMIT && limited.safeFraction()<hit.safeFraction(),"Exhausted iteration budget is fail-closed, not clear");
        var translated=new ConservativeSweep.Motion(t->ConvexBox.of(new AABB(-.2,-.2,-.2,.2,.2,.2),new Matrix4f().translation(0,(float)(2*t),0)),2);
        var above=new AABB(-.1,1,-.1,.1,1.2,.1);
        var ascent=ConservativeSweep.query(above,Vec3.ZERO,translated,64);
        h.assertTrue(ascent.status()==ConservativeSweep.Status.CONTACT && Math.abs(ascent.safeFraction()-.4)<1e-5,"Ascending support catches stationary body");
        h.succeed();
    }
    @GameTest public void atomicCatalog(GameTestHelper h) {
        var geometry=new ModelGeometry(1,"test:box","1",java.util.List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            java.util.List.of(new ModelGeometry.Piece("body","root",java.util.List.of(0d,0d,0d),java.util.List.of(1d,1d,1d),null)));
        var catalog=new GeometryCatalog();
        var accepted=catalog.replace(java.util.Map.of("test:box",geometry),java.util.List.of("test:box"));
        boolean rejected=false;
        try{catalog.replace(java.util.Map.of(),java.util.List.of("test:missing"));}catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected && catalog.snapshot()==accepted,"Invalid reload leaves the entire previous snapshot intact");
        h.succeed();
    }
    @GameTest public void exportedCatalogOnDedicatedServer(GameTestHelper h) {
        String directory=System.getProperty("scalebrews.anatomyCatalog");
        if(directory==null){h.succeed();return;} // Optional exported-artifact proof, never a bundled client dependency.
        var models=new java.util.TreeMap<String,ModelGeometry>();
        try(var files=java.nio.file.Files.list(java.nio.file.Path.of(directory))) {
            for(var file:files.filter(p->p.toString().endsWith(".json")).toList()) {
                var model=new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(file),ModelGeometry.class);
                models.put(model.source(),model);
                var boxes=model.evaluate(new Matrix4f(),java.util.Map.of(),AnatomyFilter.DEFAULT);
                h.assertTrue(!boxes.isEmpty(),"Exported model has non-decorative server geometry: "+model.source());
                PoseProvider poseProvider=switch(model.source()) {case "minecraft:cow"->new QuadrupedPose();case "alexsmobs:grizzly_bear"->new GrizzlyPose();default->new PlayerWalkingPose();};
                var evaluator=new ModelGeometryProvider(model,poseProvider,AnatomyFilter.DEFAULT,1);
                var from=new AnatomyPoseHistory.Sample(new PoseProvider.Inputs(1,.5f,10,-10,5,true),new Vec3(12000000,20,3000000),179,2);
                var to=new AnatomyPoseHistory.Sample(new PoseProvider.Inputs(1.37f,.6f,11,10,-5,true),from.origin().add(.2,.1,0),-179,2.1f);
                var motions=evaluator.motionBetween(from,to).orElseThrow().pieces();
                h.assertTrue(motions.keySet().equals(boxes.keySet()),"Motion retains anatomical pieces");
                for(var motion:motions.values()) {
                    var previous=motion.at().apply(0);
                    for(int sample=1;sample<=100;sample++) {
                        var next=motion.at().apply(sample/100d);
                        for(int vertex=0;vertex<8;vertex++)h.assertTrue(next.vertices().get(vertex).distanceTo(previous.vertices().get(vertex))*100<=motion.maxPointSpeed()+.001,"Exported animated hierarchy respects analytic speed bound: "+model.source());
                        previous=next;
                    }
                }
                if(model.source().equals("minecraft:cow"))for(int tick=0;tick<80;tick++)
                    h.assertTrue(!model.evaluate(new Matrix4f(),new QuadrupedPose().evaluate(model,new PoseProvider.Inputs(tick*.37f,.5f,tick,10,5,true)).orElseThrow(),AnatomyFilter.DEFAULT).isEmpty(),"Cow server pose");
                if(model.source().equals("alexsmobs:grizzly_bear"))for(int tick=0;tick<80;tick++)
                    h.assertTrue(!model.evaluate(new Matrix4f(),new GrizzlyPose().evaluate(model,new PoseProvider.Inputs(tick*.37f,.5f,tick,10,5,true)).orElseThrow(),AnatomyFilter.DEFAULT).isEmpty(),"Grizzly server pose without Alex client classes");
                if(model.source().startsWith("minecraft:player_"))for(int tick=0;tick<80;tick++)
                    h.assertTrue(!model.evaluate(new Matrix4f(),new PlayerWalkingPose().evaluate(model,new PoseProvider.Inputs(tick*.37f,.5f,tick,10,5,true)).orElseThrow(),AnatomyFilter.DEFAULT).isEmpty(),"Player server pose");
            }
        }catch(java.io.IOException e){throw new RuntimeException(e);}
        h.assertTrue(models.size()==4,"Expected cow, both player variants, and grizzly");
        new GeometryCatalog().replace(models,models.keySet());
        System.out.println("ANATOMY_DEDICATED exported original models and server poses passed: "+models.keySet());
        h.succeed();
    }
    @GameTest public void sixFacesAndGap(GameTestHelper h) {
        var far=ConvexBox.of(new AABB(-1,-1,-1,1,1,1),new Matrix4f().rotateY((float)Math.PI)).move(new Vec3(12000000,20,3000000));
        h.assertTrue(far.sweep(new AABB(11999999.9,16,2999999.9,12000000.1,16.2,3000000.1),new Vec3(0,8,0))!=null,"Near-parallel SAT axes must not normalize to zero at distant world coordinates");
        ConvexBox box=ConvexBox.of(new AABB(-1,-1,-1,1,1,1),new Matrix4f());
        for(Vec3 n:java.util.List.of(new Vec3(1,0,0),new Vec3(-1,0,0),new Vec3(0,1,0),new Vec3(0,-1,0),new Vec3(0,0,1),new Vec3(0,0,-1))) {
            var body=new AABB(-.1,-.1,-.1,.1,.1,.1).move(n.scale(2));
            var hit=box.sweep(body,n.scale(-2));
            h.assertTrue(hit!=null && !hit.penetrating() && Math.abs(hit.fraction()-.45)<1e-7 && hit.normal().dot(n)>.999,"All six faces stop an approaching body");
            h.assertTrue(box.sweep(body,n)==null,"Moving away is not a collision");
        }
        var thin=ConvexBox.of(new AABB(-1,-.1,-.1,1,.1,.1),new Matrix4f().rotateY((float)Math.PI/4));
        AABB emptyCorner=new AABB(.6,-.05,.6,.7,.05,.7);
        h.assertTrue(thin.bounds().intersects(emptyCorner) && !thin.overlaps(emptyCorner),"Do not fill rotated box corners");
        var gap=new AABB(-.05,-2,-.05,.05,-1.5,.05);
        var left=ConvexBox.of(new AABB(-1,-1,-1,-.3,1,1),new Matrix4f());
        var right=ConvexBox.of(new AABB(.3,-1,-1,1,1,1),new Matrix4f());
        h.assertTrue(left.sweep(gap,new Vec3(0,4,0))==null && right.sweep(gap,new Vec3(0,4,0))==null,"Gaps remain open from below");
        h.succeed();
    }
    @GameTest public void filtersAndShear(GameTestHelper h) {
        h.assertTrue("degenerate".equals(AnatomyFilter.DEFAULT.rejection(new AABB(0,0,0,1,0,1),1)),"Planes have no anatomy volume");
        h.assertTrue("thickness".equals(AnatomyFilter.DEFAULT.rejection(new AABB(0,0,0,1,.01,1),1)),"Thin ornaments rejected before scaling");
        h.assertTrue(AnatomyFilter.DEFAULT.rejection(new AABB(0,0,0,.2,1,.2),1)==null,"Substantial limbs retained");
        var shear=ConvexBox.of(new AABB(0,0,0,1,1,1),new Matrix4f().m10(.7f));
        h.assertTrue(shear.overlaps(new AABB(.8,.4,.4,.9,.5,.5)),"Affine sheared anatomy remains supported");
        h.assertTrue(!shear.overlaps(new AABB(.01,.9,.2,.05,.95,.3)),"Shear does not fill envelope");
        h.succeed();
    }
    @GameTest public void confirmedContactAndTeleportLifecycle(GameTestHelper h) {
        var support=h.spawn(net.minecraft.world.entity.EntityTypes.COW,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        body.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        var box=ConvexBox.of(new AABB(-1,-1,-1,1,1,1),new Matrix4f()).move(support.position());
        AnatomyMovement.activate(h.getLevel());AnatomyMovement.register(support,e->java.util.Optional.of(new GeometryProvider.Snapshot(7,java.util.Map.of("body",box))));
        try {
            var surface=new SurfaceContact(support.getUUID(),7,"body",3,new Vec3(.5,1,.5),new Vec3(0,1,0),h.getLevel().getGameTime());
            body.setPos(support.position().add(10,0,0));
            h.assertTrue(AnatomyMovement.confirm(body,support,surface) && AnatomyMovement.supported(body)==false,
                "Network confirmation restores identity but does not invent physical proximity");
            body.setPos(box.bounds().getCenter().x,box.bounds().maxY,box.bounds().getCenter().z);
            h.assertTrue(AnatomyMovement.confirm(body,support,surface) && AnatomyMovement.supported(body),"Confirmed material face becomes grounded at its real location");
            body.setPos(body.position().add(5,0,0));AnatomyMovement.carry(body);
            h.assertTrue(AnatomyMovement.contact(body)==null,"External teleport over four blocks releases temporary support");
            var epoch=java.util.UUID.randomUUID();
            h.assertTrue(!AnatomyContactPayload.clear(epoch,7,body.getId(),body.getUUID(),2,h.getLevel().getGameTime()).present(),"Explicit contact clear has no surface payload");
        } finally {AnatomyMovement.deactivate(h.getLevel());support.discard();body.discard();}
        h.succeed();
    }
}
