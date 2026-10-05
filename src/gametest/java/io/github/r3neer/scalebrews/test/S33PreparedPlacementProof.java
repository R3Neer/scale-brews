package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.integration.gravity.GravityFrames;
import io.github.r3neer.scalebrews.platform.PlatformPlacement;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S33 real interactOn proof inside the single prepared server session. */
final class S33PreparedPlacementProof {
    private S33PreparedPlacementProof() {}

    static void run(GameTestHelper h) {
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        Vec3 center=support.position().add(0,2,0);
        var piece=ConvexBox.of(new AABB(-3,-3,-3,3,3,3),new Matrix4f()).move(center);
        io.github.r3neer.scalebrews.test.TestCoreBindings.register(support,entity->Optional.of(new GeometryProvider.Snapshot(1,Map.of("body",piece))));
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        try {
            h.assertTrue(AnatomyApi.ready(player),"Prepared session must own placement");
            for(Direction outward:Direction.values()) {
                Vec3 normal=new Vec3(outward.getStepX(),outward.getStepY(),outward.getStepZ());
                Vec3 eye=center.add(normal.scale(4));
                player.setPos(eye.subtract(0,player.getEyeHeight(),0));
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.OAK_BOAT,2));
                final Entity[] created={null};
                PlatformPlacement.candidateHookForTests(body->{
                    created[0]=body;
                    GravityFrames.overrideForTests(body,new GravityFrame(outward.getOpposite()));
                });
                try {
                    // The point on a side face lies outside the cow's vanilla
                    // AABB; the accepted material piece alone authorizes it.
                    Vec3 click=center.add(normal.scale(2.5)).subtract(support.position());
                    var result=player.interactOn(support,InteractionHand.MAIN_HAND,click);
                    h.assertTrue(result==InteractionResult.SUCCESS,"Real anatomical placement on "+outward+": "+result);
                    h.assertTrue(created[0]!=null && !created[0].isRemoved(),"One body inserted on "+outward);
                    h.assertTrue(AnatomyApi.support(created[0]).orElse(null)==support,"Scale anchor on "+outward);
                    h.assertTrue(GravityFrames.frame(created[0]).down()==outward.getOpposite(),
                        "The exact placed body uses the owner-provided gravity frame on "+outward);
                    assertActualBoxOnFace(h,created[0].getBoundingBox(),center,outward);
                    h.assertTrue(player.getMainHandItem().getCount()==1,"Survival consumes exactly one on "+outward);
                } finally {
                    PlatformPlacement.candidateHookForTests(null);
                    if(created[0]!=null){GravityFrames.clearOverrideForTests(created[0]);created[0].discard();}
                }
            }
            Vec3 topEye=center.add(0,4,0),topClick=center.add(0,2.5,0).subtract(support.position());
            player.setPos(topEye.subtract(0,player.getEyeHeight(),0));
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.PIG_SPAWN_EGG,2));
            h.assertTrue(player.interactOn(support,InteractionHand.MAIN_HAND,topClick)==InteractionResult.SUCCESS,
                "Type-only vanilla spawn egg places on anatomical material");
            var eggBodies=placedBodies(h,support,piece.bounds());
            h.assertTrue(eggBodies.size()==1 && eggBodies.getFirst().getType()==EntityTypes.PIG,
                "Egg creates exactly one intended pig with a Scale anchor");
            h.assertTrue(player.getMainHandItem().getCount()==1,"Egg consumes once in survival");
            eggBodies.forEach(Entity::discard);

            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.PIG_SPAWN_EGG,2));
            Vec3 roundedOutside=center.add(0,3.00012,0).subtract(support.position());
            h.assertTrue(player.interactOn(support,InteractionHand.MAIN_HAND,roundedOutside)==InteractionResult.SUCCESS,
                "LpVec3-sized outward endpoint error still selects the material face");
            var roundedBodies=placedBodies(h,support,piece.bounds());
            h.assertTrue(roundedBodies.size()==1 && roundedBodies.getFirst().getType()==EntityTypes.PIG
                    && player.getMainHandItem().getCount()==1,
                "Bounded packet precision recovery inserts and consumes exactly once");
            roundedBodies.forEach(Entity::discard);
            Vec3 grazingEye=center.add(2.05,3.01,0);
            player.setPos(grazingEye.subtract(0,player.getEyeHeight(),0));
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.PIG_SPAWN_EGG,2));
            h.assertTrue(player.interactOn(support,InteractionHand.MAIN_HAND,roundedOutside)==InteractionResult.SUCCESS,
                "Grazing ray still selects a face inside the packet precision cell");
            var grazingBodies=placedBodies(h,support,piece.bounds());
            h.assertTrue(grazingBodies.size()==1 && grazingBodies.getFirst().getType()==EntityTypes.PIG
                    && player.getMainHandItem().getCount()==1,
                "Grazing packet recovery inserts one body and consumes one egg");
            grazingBodies.forEach(Entity::discard);
            player.setPos(topEye.subtract(0,player.getEyeHeight(),0));
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.PIG_SPAWN_EGG,2));
            Vec3 beyondCodec=center.add(0,3.001,0).subtract(support.position());
            h.assertTrue(player.interactOn(support,InteractionHand.MAIN_HAND,beyondCodec)==InteractionResult.PASS,
                "A miss beyond LpVec3 precision remains vanilla-owned");
            h.assertTrue(player.getMainHandItem().getCount()==2 && placedBodies(h,support,piece.bounds()).isEmpty(),
                "Out-of-budget endpoint cannot insert or consume a body");

            GameType.CREATIVE.updatePlayerAbilities(player.getAbilities());
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BAMBOO_CHEST_RAFT));
            h.assertTrue(player.interactOn(support,InteractionHand.MAIN_HAND,topClick)==InteractionResult.SUCCESS,
                "Creative raft places on anatomical material");
            h.assertTrue(player.getMainHandItem().getCount()==1,"Creative placement consumes nothing");
            var creativeBodies=placedBodies(h,support,piece.bounds());
            h.assertTrue(creativeBodies.size()==1 && creativeBodies.getFirst().getType()==EntityTypes.BAMBOO_CHEST_RAFT,
                "Creative placement creates exactly one raft and anchor");
            creativeBodies.forEach(Entity::discard);

            GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COW_SPAWN_EGG,2));
            h.assertTrue(PlatformPlacement.interact(player,support,InteractionHand.MAIN_HAND,topClick)==InteractionResult.PASS,
                "Same-species adult egg remains vanilla-owned even over accepted anatomical material");
            h.assertTrue(player.getMainHandItem().getCount()==2 && placedBodies(h,support,piece.bounds()).isEmpty(),
                "Scale does not consume or attach a same-species egg");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.OAK_BOAT,2));
            player.getAbilities().mayBuild=false;
            h.assertTrue(player.interactOn(support,InteractionHand.MAIN_HAND,topClick)==InteractionResult.FAIL,
                "Denied build permission rejects Scale placement");
            h.assertTrue(player.getMainHandItem().getCount()==2 && placedBodies(h,support,piece.bounds()).isEmpty(),
                "Denied placement leaves world and inventory untouched");
            var existingChicken=h.spawn(EntityTypes.CHICKEN,3,25,2);
            try {
                var around=piece.bounds().inflate(8);
                int chickensBefore=h.getLevel().getEntities(support,around,e->e.getType()==EntityTypes.CHICKEN).size();
                int zombiesBefore=h.getLevel().getEntities(support,around,e->e.getType()==EntityTypes.ZOMBIE).size();
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.ZOMBIE_SPAWN_EGG,2));
                final int[] zombieCandidates={0};
                PlatformPlacement.candidateHookForTests(entity->{if(entity.getType()==EntityTypes.ZOMBIE)zombieCandidates[0]++;});
                try {
                    h.assertTrue(player.interactOn(support,InteractionHand.MAIN_HAND,topClick)==InteractionResult.FAIL,
                        "Rejected zombie egg fails without finalizeSpawn side effects");
                } finally {PlatformPlacement.candidateHookForTests(null);}
                h.assertTrue(player.getMainHandItem().getCount()==2
                        && zombieCandidates[0]==1 && existingChicken.getVehicle()==null
                        && h.getLevel().getEntities(support,around,e->e.getType()==EntityTypes.CHICKEN).size()==chickensBefore
                        && h.getLevel().getEntities(support,around,e->e.getType()==EntityTypes.ZOMBIE).size()==zombiesBefore,
                    "Rejected egg keeps existing chicken state and creates no zombie or chicken");
            } finally {existingChicken.discard();}
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.OAK_BOAT,2));
            player.getAbilities().mayBuild=true;
            player.setPos(topEye.add(0,8,0).subtract(0,player.getEyeHeight(),0));
            h.assertTrue(player.interactOn(support,InteractionHand.MAIN_HAND,topClick)==InteractionResult.FAIL,
                "Actual face point outside entity reach rejects placement");
            h.assertTrue(player.getMainHandItem().getCount()==2 && placedBodies(h,support,piece.bounds()).isEmpty(),
                "Out-of-reach placement leaves world and inventory untouched");

            player.setPos(topEye.subtract(0,player.getEyeHeight(),0));
            BlockPos occupiedBlock=BlockPos.containing(center.add(0,3.25,0));
            h.getLevel().setBlockAndUpdate(occupiedBlock,Blocks.STONE.defaultBlockState());
            try {
                h.assertTrue(player.interactOn(support,InteractionHand.MAIN_HAND,topClick)==InteractionResult.FAIL,
                    "A block occupying the intended body volume rejects material placement");
                h.assertTrue(player.getMainHandItem().getCount()==2 && placedBodies(h,support,piece.bounds()).isEmpty(),
                    "Blocked placement leaves entity world and inventory untouched");
            } finally {h.getLevel().setBlockAndUpdate(occupiedBlock,Blocks.AIR.defaultBlockState());}
            var occupyingSheep=h.spawn(EntityTypes.SHEEP,2,25,2);
            occupyingSheep.setNoAi(true);occupyingSheep.setNoGravity(true);
            try {
                h.assertTrue(player.interactOn(support,InteractionHand.MAIN_HAND,topClick)==InteractionResult.FAIL,
                    "Another live entity occupying the body volume rejects material placement");
                h.assertTrue(player.getMainHandItem().getCount()==2 && placedBodies(h,support,piece.bounds()).isEmpty(),
                    "Occupied placement leaves entity world and inventory untouched");
            } finally {occupyingSheep.discard();}

            var narrow=ConvexBox.of(new AABB(-.25,-3,-.25,.25,3,.25),new Matrix4f()).move(center);
            io.github.r3neer.scalebrews.test.TestCoreBindings.register(support,entity->Optional.of(new GeometryProvider.Snapshot(2,Map.of("narrow",narrow))));
            player.setPos(topEye.subtract(0,player.getEyeHeight(),0));
            h.assertTrue(player.interactOn(support,InteractionHand.MAIN_HAND,topClick)==InteractionResult.FAIL,
                "Boat footprint cannot overhang a narrow material face");
            h.assertTrue(player.getMainHandItem().getCount()==2 && placedBodies(h,support,piece.bounds()).isEmpty(),
                "Footprint rejection leaves world and inventory untouched");
        } finally {PlatformPlacement.candidateHookForTests(null);player.discard();support.discard();}
    }

    private static java.util.List<Entity> placedBodies(GameTestHelper h,Entity support,AABB around) {
        return h.getLevel().getEntities(support,around.inflate(8),
            body->AnatomyApi.support(body).orElse(null)==support);
    }

    private static void assertActualBoxOnFace(GameTestHelper h,AABB box,Vec3 center,Direction outward) {
        double face=switch(outward.getAxis()) {
            case X -> center.x+3*outward.getStepX();
            case Y -> center.y+3*outward.getStepY();
            case Z -> center.z+3*outward.getStepZ();
        };
        double facing=switch(outward) {
            case EAST -> box.minX;
            case WEST -> box.maxX;
            case UP -> box.minY;
            case DOWN -> box.maxY;
            case SOUTH -> box.minZ;
            case NORTH -> box.maxZ;
        };
        h.assertTrue(Math.abs(facing-face)<.001,"Actual placed AABB touches selected material plane on "+outward);
        h.assertTrue(outward.getAxis()==Direction.Axis.X || box.minX>=center.x-3 && box.maxX<=center.x+3,
            "Actual X footprint stays on selected face on "+outward);
        h.assertTrue(outward.getAxis()==Direction.Axis.Y || box.minY>=center.y-3 && box.maxY<=center.y+3,
            "Actual Y footprint stays on selected face on "+outward);
        h.assertTrue(outward.getAxis()==Direction.Axis.Z || box.minZ>=center.z-3 && box.maxZ<=center.z+3,
            "Actual Z footprint stays on selected face on "+outward);
    }
}
