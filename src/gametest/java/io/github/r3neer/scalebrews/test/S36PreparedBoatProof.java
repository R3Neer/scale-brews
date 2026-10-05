package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime;
import io.github.r3neer.scalebrews.collision.internal.MaterialIntervalRuntime;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import net.minecraft.world.phys.Vec3;

/** S36 dry canonical hull baseline; real local-client rowing has a separate mandatory lane. */
final class S36PreparedBoatProof {
    private S36PreparedBoatProof() {}

    static void run(GameTestHelper h) {
        List<EntityType<? extends AbstractBoat>> variants=List.of(EntityTypes.OAK_BOAT,
            EntityTypes.OAK_CHEST_BOAT,EntityTypes.BAMBOO_RAFT,EntityTypes.BAMBOO_CHEST_RAFT);
        for(var variant:variants)runDry(h,variant);
        runOversizeControl(h);
        runBlockedCarry(h);
    }

    private static void runDry(GameTestHelper h,EntityType<? extends AbstractBoat> variant) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        AbstractBoat boat=null;
        try {
            Platforms.tick(level);
            var frame=AnatomyMovement.queryFrame(support).orElseThrow();
            var piece=frame.snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(piece!=null,"S36 needs the certified prepared cow body");
            var bounds=piece.bounds();
            boat=variant.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            h.assertTrue(boat!=null,"S36 creates a real hull variant "+variant);
            boolean chestVariant=variant==EntityTypes.OAK_CHEST_BOAT || variant==EntityTypes.BAMBOO_CHEST_RAFT;
            h.assertTrue(boat.getType()==variant && (boat instanceof ContainerEntity)==chestVariant,
                "Vanilla hull type and chest identity survive prepared selection: "+variant);
            if(boat instanceof ContainerEntity chest)chest.setItem(0,new ItemStack(Items.STONE,3));
            boat.setPos(bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z);
            h.assertTrue(level.addFreshEntity(boat),"S36 inserts the real hull "+variant);
            h.assertTrue(AnatomyApi.ready(boat) && Platforms.eligible(boat,support),
                "Prepared policy admits hull/support pair "+variant);
            boat.move(MoverType.SELF,new Vec3(0,-1.5,0));
            var contact=AnatomyMovement.contact(boat);var surface=AnatomyMovement.surface(boat);
            h.assertTrue(contact!=null && contact.support()==support && surface!=null
                    && contact.revision()==frame.snapshot().revision() && contact.piece().equals(surface.piece())
                    && Platforms.supported(boat) && TestRetiredPlatformRuntime.absent(),
                "Real hull move acquires canonical contact without legacy owner: "+variant+" contact="+contact);
            double expectedFriction=Platforms.friction(boat,.6);
            h.assertTrue(Math.abs(boat.getGroundFriction()-expectedFriction)<1e-5,
                "Dry canonical hull reads configured material ground friction: "+variant);
            boat.tick();
            h.assertTrue(Platforms.supported(boat),"Unoccupied real hull tick retains material contact: "+variant);
            if(boat instanceof ContainerEntity chest)
                h.assertTrue(chest.getItem(0).is(Items.STONE) && chest.getItem(0).getCount()==3,
                    "Canonical material tick preserves vanilla chest contents: "+variant);

            if(variant==EntityTypes.OAK_BOAT) {
                var before=boat.position();var retained=AnatomyMovement.surface(boat);
                var materialBefore=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(retained.piece());
                var metricsBefore=MaterialPhysicsRuntime.metrics(level);
                support.move(MoverType.SELF,new Vec3(.2,.1,0));
                var materialAfter=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(retained.piece());
                var expected=materialAfter.point(retained.localPoint()).subtract(materialBefore.point(retained.localPoint()));
                var metricsAfter=MaterialPhysicsRuntime.metrics(level);
                long admittedBefore=metricsBefore.tick()==level.getGameTime()?metricsBefore.admitted():0;
                h.assertTrue(boat.position().distanceToSqr(before.add(expected))<1e-8
                        && Platforms.supported(boat) && metricsAfter.admitted()==admittedBefore+1,
                    "One certified ROOT carries a resting canonical boat once: expected="+expected
                        +" actual="+boat.position().subtract(before)+" metrics="+metricsAfter);
                var once=boat.position();MaterialPhysicsRuntime.drain(level);
                h.assertTrue(boat.position().equals(once),"Second drain cannot duplicate hull ROOT carry");
                var yawSurface=AnatomyMovement.surface(boat);
                var yawBefore=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(yawSurface.piece());
                var boatBeforeYaw=boat.position();
                var yawCapture=MaterialIntervalRuntime.captureRoot(support);
                support.yBodyRot=yawCapture.root().yaw()+10;
                MaterialIntervalRuntime.commitRoot(support,yawCapture);
                var yawAfter=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get(yawSurface.piece());
                var yawDelta=yawAfter.point(yawSurface.localPoint()).subtract(yawBefore.point(yawSurface.localPoint()));
                MaterialPhysicsRuntime.drain(level);
                h.assertTrue(boat.position().distanceToSqr(boatBeforeYaw.add(yawDelta))<1e-8
                        && Platforms.supported(boat),
                    "Retained local material point carries real hull through one ROOT yaw");
            }
        } finally {
            if(boat instanceof ContainerEntity chest)chest.setItem(0,ItemStack.EMPTY);
            if(boat!=null)boat.discard();
            support.discard();
        }
    }
    private static void runOversizeControl(GameTestHelper h) {
        var level=h.getLevel();var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(.5);support.refreshDimensions();
        AbstractBoat boat=null;
        try {
            Platforms.tick(level);
            var piece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(piece!=null,"Oversize control needs prepared geometry");
            var bounds=piece.bounds();
            boat=EntityTypes.OAK_BOAT.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            h.assertTrue(boat!=null,"Oversize control creates an ordinary boat");
            boat.setPos(bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z);
            h.assertTrue(level.addFreshEntity(boat),"Oversize control inserts hull");
            h.assertTrue(AnatomyApi.ready(boat) && !Platforms.eligible(boat,support),
                "Narrow prepared support rejects an oversize hull before material response");
            boat.move(MoverType.SELF,new Vec3(0,-1.5,0));
            h.assertTrue(AnatomyMovement.contact(boat)==null && TestRetiredPlatformRuntime.absent()
                    && !Platforms.supported(boat),
                "Rejected width ratio retains vanilla movement without a canonical or legacy material owner");
            boat.setPos(support.getX()+.4,support.getY(),support.getZ());
            boat.setDeltaMovement(Vec3.ZERO);
            h.assertTrue(AbstractBoat.canVehicleCollide(boat,support) && boat.canCollideWith(support),
                "Rejected hull/support pair retains the vanilla vehicle collision predicate");
            boat.push(support);
            h.assertTrue(boat.getDeltaMovement().horizontalDistanceSqr()>1e-8,
                "One real vanilla push still acts on a ratio-rejected boat/cow pair");
        } finally {
            if(boat!=null)boat.discard();
            support.discard();
        }
    }
    private static void runBlockedCarry(GameTestHelper h) {
        var level=h.getLevel();var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        AbstractBoat boat=null;var wall=new ArrayList<BlockPos>();
        try {
            Platforms.tick(level);
            var piece=AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(piece!=null,"Blocked hull fixture needs prepared body");
            var bounds=piece.bounds();
            boat=EntityTypes.OAK_BOAT.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            h.assertTrue(boat!=null,"Blocked hull fixture creates a real oak boat");
            boat.setPos(bounds.getCenter().x,bounds.maxY+.8,bounds.getCenter().z);
            h.assertTrue(level.addFreshEntity(boat),"Blocked hull fixture inserts boat");
            boat.move(MoverType.SELF,new Vec3(0,-1.5,0));
            h.assertTrue(Platforms.supported(boat),"Blocked hull fixture acquires canonical contact");
            int x=(int)Math.ceil(boat.getBoundingBox().maxX+.05),y=(int)Math.floor(boat.getBoundingBox().minY);
            for(int z=(int)Math.floor(boat.getBoundingBox().minZ);z<=(int)Math.floor(boat.getBoundingBox().maxZ);z++) {
                var pos=new BlockPos(x,y,z);wall.add(pos);
                h.assertTrue(level.getBlockState(pos).isAir(),"Blocked hull wall starts in air");
                level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
            }
            var before=boat.position();
            double gap=x-boat.getBoundingBox().maxX;
            double supportBeforeX=support.getX();
            // The next integer block boundary can be nearly a full block from
            // the hull. Move beyond that gap so this always tests obstruction.
            support.move(MoverType.SELF,new Vec3(1.2,0,0));
            h.assertTrue(support.getX()-supportBeforeX>gap+.05,
                "The support crosses the hull-to-wall gap in the obstruction fixture");
            h.assertTrue(boat.getBoundingBox().maxX<=x+1e-6 && boat.getX()<before.x+1.2-1e-4,
                "Material ROOT cannot carry a boat through a real block wall: before="+before
                    +" after="+boat.position()+" box="+boat.getBoundingBox()+" wallX="+x
                    +" wallY="+y+" contact="+AnatomyMovement.contact(boat));
            var stopped=boat.position();
            for(var pos:wall)level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            wall.clear();MaterialPhysicsRuntime.drain(level);
            h.assertTrue(boat.position().equals(stopped),
                "Removing obstruction cannot replay the rejected boat displacement");
            support.discard();
            h.assertTrue(AnatomyMovement.contact(boat)==null && !Platforms.supported(boat),
                "Support retirement releases the obstructed hull without legacy fallback");
        } finally {
            for(var pos:wall)level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            if(boat!=null)boat.discard();
            support.discard();
        }
    }
}
