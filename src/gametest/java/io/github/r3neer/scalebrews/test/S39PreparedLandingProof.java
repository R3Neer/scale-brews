package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.effect.ScaleEffects;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** FR-062: real material and terrain landings keep native damage but split Growth impact. */
final class S39PreparedLandingProof {
    private S39PreparedLandingProof() {}

    static void run(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(6);support.refreshDimensions();
        var actor=h.spawn(EntityTypes.PIG,2,29,2);
        actor.setNoAi(true);actor.setNoGravity(true);
        actor.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);actor.setHealth(200);
        actor.addEffect(new MobEffectInstance(ScaleEffects.GROWTH,1200,2));TestScale.settle(actor);
        var target=h.spawn(EntityTypes.PIG,4,20,2);
        target.setNoAi(true);target.setNoGravity(true);
        var terrain=new ArrayList<BlockPos>();BlockPos decoy=null;
        try {
            Platforms.tick(level);
            var piece=AnatomyMovement.queryFrame(support).orElseThrow()
                .snapshot().pieces().get("root/body/cube_0");
            h.assertTrue(piece!=null && Platforms.eligible(actor,support),
                "Growth III landing actor must be eligible for prepared living material");
            var bounds=piece.bounds();
            actor.setPos(bounds.getCenter().x,bounds.maxY+8,bounds.getCenter().z);
            target.setPos(bounds.getCenter().x+2,bounds.maxY,bounds.getCenter().z);
            // Non-colliding, non-AIR block at the native on-position makes
            // Growth's supported negative observable rather than vacuous.
            decoy=BlockPos.containing(actor.getX(),bounds.maxY-.2,actor.getZ());
            h.assertTrue(level.getBlockState(decoy).isAir(),"Landing decoy starts clear");
            level.setBlockAndUpdate(decoy,Blocks.STRUCTURE_VOID.defaultBlockState());
            float ownBefore=actor.getHealth(),targetBefore=target.getHealth();
            try(var observed=new LandingObservation(actor)) {
                actor.move(MoverType.SELF,new Vec3(0,-4,0));
                h.assertTrue(actor.fallDistance>=3 && !Platforms.supported(actor),
                    "Free fall accumulates eligible distance before the living landing");
                actor.move(MoverType.SELF,new Vec3(0,-6,0));
                h.assertTrue(Platforms.supported(actor) && AnatomyMovement.contact(actor)!=null,
                    "True fall onto prepared living material acquires contact");
                h.assertTrue(observed.groundedChecks==1 && observed.nonAirGroundedChecks==1
                        && observed.materialAtGroundedCheck,
                    "Native landing sees a non-AIR ground argument and current material contact: "
                        +"checks="+observed.groundedChecks+" nonAir="+observed.nonAirGroundedChecks
                        +" supported="+observed.materialAtGroundedCheck+" feet="+actor.getY()
                        +" decoy="+decoy+" ground="+level.getBlockState(actor.getOnPosLegacy()));
                h.assertTrue(observed.landings==1 && observed.gusts==0
                        && actor.getHealth()<ownBefore && actor.fallDistance==0
                        && target.getHealth()==targetBefore && target.getDeltaMovement().lengthSqr()<1e-12,
                    "Living landing keeps native self fall damage without Growth terrain impact: "
                        +" ownBefore="+ownBefore+" own="+actor.getHealth()
                        +" fallDistance="+actor.fallDistance+" targetBefore="+targetBefore
                        +" target="+target.getHealth()+" targetVelocity="+target.getDeltaMovement()
                        +" events="+observed.landings+" gusts="+observed.gusts
                        +" terrain="+observed.terrain);
            }

            support.discard();AnatomyMovement.clear(actor);
            level.setBlockAndUpdate(decoy,Blocks.AIR.defaultBlockState());decoy=null;
            var floorY=(int)Math.floor(bounds.maxY)-6;
            var centerX=(int)Math.floor(bounds.getCenter().x);
            var centerZ=(int)Math.floor(bounds.getCenter().z);
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++) {
                var pos=new BlockPos(centerX+dx,floorY,centerZ+dz);
                h.assertTrue(level.getBlockState(pos).isAir(),"Terrain control starts clear");
                level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());terrain.add(pos);
            }
            actor.setPos(bounds.getCenter().x,floorY+11,bounds.getCenter().z);
            target.setPos(bounds.getCenter().x+2,floorY+1,bounds.getCenter().z);
            target.setDeltaMovement(Vec3.ZERO);
            float ownTerrainBefore=actor.getHealth(),targetTerrainBefore=target.getHealth();
            try(var observed=new LandingObservation(actor)) {
                actor.move(MoverType.SELF,new Vec3(0,-5,0));
                h.assertTrue(actor.fallDistance>=3 && !Platforms.supported(actor),
                    "Terrain control accumulates the same eligible free-fall distance");
                actor.move(MoverType.SELF,new Vec3(0,-7,0));
                h.assertTrue(observed.groundedChecks==1 && observed.nonAirGroundedChecks==1
                        && !observed.materialAtGroundedCheck && observed.landings==1,
                    "Native terrain fall reaches the same eligible landing route without material contact");
                h.assertTrue(actor.getHealth()<ownTerrainBefore && actor.fallDistance==0
                        && observed.gusts==1 && observed.terrain>=36
                        && target.getHealth()<targetTerrainBefore,
                    "Terrain fall keeps native self damage and one Growth III impact: "
                        +" own="+actor.getHealth()+" target="+target.getHealth()
                        +" gusts="+observed.gusts+" terrain="+observed.terrain);
            }
        } finally {
            if(decoy!=null)level.setBlockAndUpdate(decoy,Blocks.AIR.defaultBlockState());
            for(var pos:terrain)level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            target.discard();actor.discard();support.discard();
        }
    }
}
