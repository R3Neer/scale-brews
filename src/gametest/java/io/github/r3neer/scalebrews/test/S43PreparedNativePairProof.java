package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.AnatomyMode;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** IMPLEMENTER differential: actual native operations before activation versus the prepared live path. */
final class S43PreparedNativePairProof {
    enum Kind { PLAYER, MOB, BOAT, RAFT, CART, ITEM, FALLING_BLOCK }
    record Movement(Vec3 displacement,Vec3 velocity,boolean horizontal,boolean vertical,boolean ground,boolean pairCollision) {}
    record Push(Vec3 body,Vec3 support) {}
    record Sample(AABB bounds,Movement movement,Push supportCaller,Push bodyCaller) {}
    record Global(boolean clear,List<String> path,float damage,boolean hurt,boolean interaction,String held,int count) {}
    record Baseline(Map<Kind,Sample> samples,Global global) {}
    private static final Vec3 ROOT=new Vec3(.15,0,.07);
    private S43PreparedNativePairProof() {}

    static Baseline captureNative(GameTestHelper h,ModelGeometry cow) {
        var samples=new EnumMap<Kind,Sample>(Kind.class);
        for(var kind:Kind.values()) {
            var support=support(h);var body=body(h,kind);
            try {
                h.assertTrue(AnatomyApi.mode(body)==AnatomyMode.DISABLED,"S43 native reference is captured before activation");
                // Original geometry supplies fixture coordinates only; it is not registered or used as a native oracle.
                var shape=cow.evaluate(new Matrix4f().rotationY((float)Math.PI).scale(support.getScale())
                    .mul(ModelGeometry.matrix(cow.modelTransform())),Map.of(),AnatomyFilter.DEFAULT)
                    .get("root/body/cube_0").move(support.position());
                var bounds=shape.bounds();
                var movement=moveAcross(body,support,bounds);
                body.setPos(support.getX()+.25,bounds.maxY,support.getZ());
                var supportCaller=push(body,support,true);var bodyCaller=push(body,support,false);
                h.assertTrue(Platforms.state(body).support==null,"Native reference cannot acquire an automatic legacy plane");
                samples.put(kind,new Sample(bounds,movement,supportCaller,bodyCaller));
            } finally {body.discard();support.discard();}
        }
        return new Baseline(Map.copyOf(samples),global(h));
    }

    static void run(GameTestHelper h,Baseline reference,Map<String,ModelGeometry> models,
                    Map<String,PlatformDefinition> original) {
        int comparisons=0,nonzero=0;
        try {
            for(var kind:Kind.values()) {
                var support=support(h);var body=body(h,kind);
                try {
                    double ratio=body.getBbWidth()/(double)support.getBbWidth();
                    for(int boundary=-1;boundary<=2;boundary++) {
                        boolean enabled=boundary!=2;
                        double limit=boundary==-1?Math.nextDown(ratio):boundary==1?Math.nextUp(ratio):ratio;
                        var profiles=new TreeMap<>(original);
                        profiles.replaceAll((key,value)->value.entity().equals(Identifier.parse("minecraft:cow"))
                            ?new PlatformDefinition(value.entity(),enabled,value.friction(),Optional.of(limit),List.of(),value.anatomy()):value);
                        S43TestCatalog.reload(h.getLevel().getServer(),models,profiles);
                        Platforms.tick(h.getLevel());
                        boolean eligible=enabled && boundary!=-1;
                        h.assertTrue(AnatomyApi.ready(body) && Platforms.eligible(body,support)==eligible,
                            "S43 measured boundary kind="+kind+" ratio="+ratio+" limit="+limit+" enabled="+enabled
                                +" eligible="+Platforms.eligible(body,support)+" mode="+AnatomyApi.mode(body)
                                +" bodyWidth="+body.getBbWidth()+" supportWidth="+support.getBbWidth()
                                +" category="+Platforms.category(body)+" ordinary="+Platforms.ordinary(body)
                                +" supportOrdinary="+Platforms.ordinary(support)+" body="+body);
                        var nativeSample=reference.samples().get(kind);
                        var actual=moveAcross(body,support,nativeSample.bounds());
                        if(!eligible) {
                            h.assertTrue(equal(actual,nativeSample.movement()),"Rejected pair must match native move/flags/velocity: "
                                +kind+" boundary="+boundary+" native="+nativeSample.movement()+" actual="+actual);
                            h.assertTrue(AnatomyMovement.contact(body)==null && Platforms.state(body).support==null,
                                "Rejected pair cannot acquire either contact owner");
                            var center=nativeSample.bounds().getCenter();
                            h.assertTrue(AnatomyApi.spaceClear(body,body.getBoundingBox().move(center.subtract(body.position())))
                                && AnatomyApi.raycast(body,center.add(-5,0,0),center.add(5,0,0)).isEmpty(),
                                "Rejected pair has no material clearance wall or ray target");
                            body.setPos(support.getX()+.25,nativeSample.bounds().maxY,support.getZ());
                            comparePush(h,kind,body,support,nativeSample);
                            var rejectedBefore=body.position();var rejectedRoot=MaterialIntervalRuntime.captureRoot(support);
                            support.setPos(support.position().add(ROOT));
                            MaterialIntervalRuntime.commitRoot(support,rejectedRoot);MaterialPhysicsRuntime.drain(h.getLevel());
                            h.assertTrue(body.position().equals(rejectedBefore) && AnatomyMovement.contact(body)==null,
                                "Rejected pair preserves native push but receives no material carry: "+kind);
                            support.setPos(support.position().subtract(ROOT));
                        } else {
                            h.assertTrue(actual.displacement().x<request(body,nativeSample.bounds()).x-.05,
                                "Eligible positive must hit original prepared body through real Entity.move: "+kind+" "+actual);
                            land(h,body,support);
                            comparePush(h,kind,body,support,nativeSample);
                            var before=body.position();var capture=MaterialIntervalRuntime.captureRoot(support);
                            support.setPos(support.position().add(ROOT));
                            MaterialIntervalRuntime.commitRoot(support,capture);MaterialPhysicsRuntime.drain(h.getLevel());
                            h.assertTrue(body.position().distanceToSqr(before.add(ROOT))<1e-8 && AnatomyMovement.supported(body),
                                "Actual eligible ROOT carry applies once before the second push comparison: "+kind
                                    +" before="+before+" actual="+body.position()+" contact="+AnatomyMovement.contact(body)
                                    +" metrics="+MaterialPhysicsRuntime.metrics(h.getLevel()));
                            var once=body.position();MaterialPhysicsRuntime.drain(h.getLevel());
                            h.assertTrue(body.position().equals(once),"Repeated drain cannot duplicate ROOT");
                            comparePush(h,kind,body,support,nativeSample);
                            if(nativeSample.supportCaller().body().lengthSqr()+nativeSample.supportCaller().support().lengthSqr()>1e-12)nonzero++;
                            support.setPos(support.position().subtract(ROOT));
                        }
                        comparisons++;
                    }
                } finally {body.discard();support.discard();}
            }
        } finally {S43TestCatalog.reload(h.getLevel().getServer(),models,original);}
        h.assertTrue(nonzero>0,"S43 cannot certify push with only native zero-response categories");
        h.assertTrue(reference.global().equals(global(h)),"Nonparticipant global collision/pathfinding/combat/interaction differs: "
            +reference.global());
        scaleAndStateRelease(h);
        System.out.println("S43_PREPARED_NATIVE_PAIR_PASS comparisons="+comparisons+" nonzeroPushControls="+nonzero);
    }

    private static Cow support(GameTestHelper h) {
        var cow=h.spawn(EntityTypes.COW,2,20,2);cow.setNoAi(true);cow.setNoGravity(true);cow.yBodyRot=0;
        cow.getAttribute(Attributes.SCALE).setBaseValue(6);cow.refreshDimensions();return cow;
    }

    private static Entity body(GameTestHelper h,Kind kind) {
        var level=h.getLevel();
        Entity entity=switch(kind) {
            case PLAYER->{var player=h.makeMockPlayer(GameType.SURVIVAL);
                h.assertTrue(level.addFreshEntity(player),"Native Player fixture must enter the real world query");yield player;}
            case MOB->h.spawn(EntityTypes.PIG,0,20,0);
            case BOAT->h.spawn(EntityTypes.OAK_BOAT,0,20,0);
            case RAFT->h.spawn(EntityTypes.BAMBOO_RAFT,0,20,0);
            case CART->h.spawn(EntityTypes.MINECART,0,20,0);
            case ITEM->{var item=new ItemEntity(level,0,20,0,new ItemStack(Items.STONE));level.addFreshEntity(item);yield item;}
            case FALLING_BLOCK->FallingBlockEntity.fall(level,h.absolutePos(new BlockPos(0,30,0)),Blocks.SAND.defaultBlockState());
        };
        entity.setNoGravity(true);
        if(entity instanceof Mob mob)mob.setNoAi(true);
        if(entity instanceof LivingEntity living){living.getAttribute(Attributes.SCALE).setBaseValue(.25);living.refreshDimensions();}
        return entity;
    }

    private static Vec3 request(Entity body,AABB bounds){return new Vec3(bounds.getXsize()+body.getBbWidth()+1.2,0,0);}
    private static Movement moveAcross(Entity body,Cow support,AABB bounds) {
        AnatomyApi.clearContact(body);Platforms.state(body).clear();
        body.setPos(bounds.minX-body.getBbWidth()/2-.6,bounds.getCenter().y,support.getZ());
        body.setDeltaMovement(Vec3.ZERO);body.setOnGround(false);
        var before=body.position();body.move(MoverType.SELF,request(body,bounds));
        return new Movement(body.position().subtract(before),body.getDeltaMovement(),body.horizontalCollision,
            body.verticalCollision,body.onGround(),body.canCollideWith(support));
    }

    private static boolean equal(Movement a,Movement b) {
        return a.displacement().distanceToSqr(b.displacement())<1e-12 && a.velocity().distanceToSqr(b.velocity())<1e-12
            && a.horizontal()==b.horizontal() && a.vertical()==b.vertical() && a.ground()==b.ground()
            && a.pairCollision()==b.pairCollision();
    }

    private static Push push(Entity body,Cow support,boolean supportCaller) {
        body.setDeltaMovement(Vec3.ZERO);support.setDeltaMovement(Vec3.ZERO);
        if(supportCaller)support.push(body);else body.push(support);
        return new Push(body.getDeltaMovement(),support.getDeltaMovement());
    }
    private static void comparePush(GameTestHelper h,Kind kind,Entity body,Cow support,Sample reference) {
        var a=push(body,support,true);var b=push(body,support,false);
        h.assertTrue(a.body().distanceToSqr(reference.supportCaller().body())<1e-12
            && a.support().distanceToSqr(reference.supportCaller().support())<1e-12
            && b.body().distanceToSqr(reference.bodyCaller().body())<1e-12
            && b.support().distanceToSqr(reference.bodyCaller().support())<1e-12,
            "Native push must match both callers and both entities once during actual material contact/carry: "+kind
                +" native="+reference.supportCaller()+"/"+reference.bodyCaller()+" actual="+a+"/"+b);
    }
    private static void land(GameTestHelper h,Entity body,Cow support) {
        AnatomyApi.clearContact(body);body.setDeltaMovement(Vec3.ZERO);support.setDeltaMovement(Vec3.ZERO);
        var bounds=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get("root/body/cube_0").bounds();
        body.setPos(support.getX()+.25,bounds.maxY+.8,support.getZ());
        body.move(MoverType.SELF,new Vec3(0,-1.2,0));
        h.assertTrue(AnatomyMovement.supported(body) && AnatomyMovement.contact(body).support()==support
            && Platforms.state(body).support==null,"S43 actual original-model landing must acquire only material ownership");
    }

    private static void scaleAndStateRelease(GameTestHelper h) {
        var support=support(h);var body=body(h,Kind.PLAYER);
        try {
            Platforms.tick(h.getLevel());land(h,body,support);
            ((LivingEntity)body).getAttribute(Attributes.SCALE).setBaseValue(16);body.refreshDimensions();
            var before=body.position();var capture=MaterialIntervalRuntime.captureRoot(support);
            support.setPos(support.position().add(ROOT));MaterialIntervalRuntime.commitRoot(support,capture);
            MaterialPhysicsRuntime.drain(h.getLevel());
            h.assertTrue(!Platforms.eligible(body,support) && AnatomyMovement.contact(body)==null && body.position().equals(before),
                "Effective scale rejection retires contact before ROOT without carry debt: eligible="+Platforms.eligible(body,support)
                    +" before="+before+" actual="+body.position()+" contact="+AnatomyMovement.contact(body)
                    +" ratio="+(body.getBbWidth()/(double)support.getBbWidth())
                    +" metrics="+MaterialPhysicsRuntime.metrics(h.getLevel()));
            ((LivingEntity)body).getAttribute(Attributes.SCALE).setBaseValue(.25);body.refreshDimensions();
            land(h,body,support);support.setBaby(true);
            AnatomyMovement.carry(body);
            h.assertTrue(!Platforms.eligible(body,support) && AnatomyMovement.contact(body)==null,
                "A real support lifecycle exclusion returns the pair to native ownership");
        } finally {body.discard();support.discard();}
    }

    private static Global global(GameTestHelper h) {
        var origin=h.absolutePos(new BlockPos(14,4,0));
        for(int x=-1;x<=5;x++)for(int z=-2;z<=2;z++)
            h.getLevel().setBlockAndUpdate(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState());
        var mob=h.spawn(EntityTypes.PIG,14,4,0);mob.setNoAi(true);mob.setNoGravity(true);
        var cow=h.spawn(EntityTypes.COW,14,4,2);cow.setNoAi(true);cow.setNoGravity(true);
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.getAttribute(Attributes.SCALE).setBaseValue(2);player.refreshDimensions();
        player.setPos(cow.getX()+1,cow.getY(),cow.getZ()+.4);
        try {
            boolean clear=h.getLevel().noCollision(player,cow.getBoundingBox().deflate(.1));
            h.assertTrue(h.getLevel().getBlockState(mob.blockPosition().below()).is(Blocks.STONE),
                "Native path fixture must stand on its real stone floor");
            // The synchronous fixture has not ticked to establish ground state yet.
            mob.setOnGround(true);
            var path=mob.getNavigation().createPath(origin.offset(4,0,0),0);
            h.assertTrue(path!=null && path.getNodeCount()>1,"Native path control must find a real multi-node route");
            var nodes=new ArrayList<String>();
            for(int i=0;i<path.getNodeCount();i++){var node=path.getNode(i);nodes.add(node.x+","+node.y+","+node.z);}
            float health=cow.getHealth();boolean hurt=cow.hurtServer(h.getLevel(),h.getLevel().damageSources().playerAttack(player),1);
            float damage=health-cow.getHealth();
            h.assertTrue(hurt && damage>0,"Native combat control must cause actual damage");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
            boolean interaction=cow.interact(player,InteractionHand.MAIN_HAND,Vec3.ZERO).consumesAction();
            var held=player.getItemInHand(InteractionHand.MAIN_HAND);
            h.assertTrue(interaction && held.is(Items.MILK_BUCKET),"Native interaction control must actually milk the cow");
            h.assertTrue(AnatomyMovement.contact(player)==null && AnatomyMovement.contact(mob)==null,
                "Global controls remain nonparticipants");
            return new Global(clear,List.copyOf(nodes),damage,hurt,interaction,BuiltInRegistries.ITEM.getKey(held.getItem()).toString(),held.getCount());
        } finally {player.discard();mob.discard();cow.discard();}
    }
}
