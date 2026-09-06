package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.platform.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public class PlatformTests {
    @GameTest public void occupiedBoatOnGiantPlayer(GameTestHelper h) {
        var support=h.makeMockPlayer(GameType.SURVIVAL);
        support.getAttribute(Attributes.SCALE).setBaseValue(5);support.refreshDimensions();
        support.setPos(h.absoluteVec(new Vec3(2,20,2)));support.setNoGravity(true);
        h.getLevel().addFreshEntity(support);
        Platforms.noteSupport(support);
        var boat=h.spawn(EntityTypes.OAK_BOAT,2,31,2);
        var rider=h.makeMockPlayer(GameType.SURVIVAL);rider.startRiding(boat,true,true);
        boat.move(MoverType.SELF,new Vec3(0,-3,0));
        h.assertTrue(Platforms.state(boat).support==support,"Occupied boat lands on giant player's head");
        var before=boat.position();
        support.setPos(support.position().add(.3,.1,.2));PlatformPhysics.carry(boat);
        near(h,boat.position().distanceTo(before.add(.3,.1,.2)),0,"Giant carries occupied boat");
        h.assertTrue(rider.getVehicle()==boat && !Platforms.supported(rider),"Native passenger remains separate from platform");
        double standingY=boat.getY();
        support.setPose(Pose.CROUCHING);support.refreshDimensions();PlatformPhysics.carry(boat);
        h.assertTrue(boat.getY()<standingY-.5 && Platforms.supported(boat),"Crouching giant lowers the physical head surface");
        h.succeed();
    }
    @GameTest public void decodedSwitches(GameTestHelper h) {
        var support=h.spawn(EntityTypes.GHAST,2,20,2);
        var profile=Platforms.definition(support);
        for(String json:java.util.List.of("{\"enabled\":false}","{\"bodies\":{\"boats\":false}}",
                "{\"supports\":{\"minecraft:ghast\":false}}","{\"max_width_ratio\":0.1}")) {
            var policy=PlatformPolicy.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString(json)).getOrThrow();
            h.assertFalse(PlatformEligibility.allows(policy,profile,"boats",.3),"Decoded switch affects live eligibility predicate: "+json);
        }
        h.assertTrue(PlatformEligibility.allows(PlatformPolicy.DEFAULT,profile,"boats",.85),"Defaults include equality at limit");
        h.assertFalse(PlatformEligibility.allows(PlatformPolicy.DEFAULT,null,"boats",.3),"No profile means no support");
        var happy=h.spawn(EntityTypes.HAPPY_GHAST,2,30,2);
        h.assertTrue(Platforms.definition(happy)==null,"Happy Ghast remains outside custom platform registry");
        h.succeed();
    }
    @GameTest public void sneakAndKnockback(GameTestHelper h) {
        var support=h.spawn(EntityTypes.GHAST,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockPlayer(GameType.SURVIVAL);
        body.setPos(support.position().add(0,5,0));body.move(MoverType.SELF,new Vec3(0,-2,0));
        body.setShiftKeyDown(true);
        h.assertTrue(PlatformPhysics.edge(body,new Vec3(8,0,0)).x<3,"Sneaking protects exposed edge");
        body.knockback(.4,1,0,body.damageSources().generic(),1);
        body.move(MoverType.SELF,body.getDeltaMovement());
        h.assertFalse(Platforms.supported(body),"Knockback releases a sneaking player");
        h.succeed();
    }
    @GameTest public void offRailCartAndPassiveAccounting(GameTestHelper h) {
        var support=h.spawn(EntityTypes.GHAST,2,20,2);
        support.setNoAi(true); support.setNoGravity(true);
        var cart=h.spawn(EntityTypes.MINECART,2,25,2);
        cart.setOnRails(false);cart.move(MoverType.SELF,new Vec3(0,-2,0));
        h.assertTrue(Platforms.supported(cart),"Off-rail cart lands");
        cart.setOnRails(true);PlatformPhysics.carry(cart);
        h.assertFalse(Platforms.supported(cart),"Rails regain control");cart.discard();
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(support.position().add(0,5,0));player.move(MoverType.SELF,new Vec3(0,-2,0));
        float saturation=player.getFoodData().getSaturationLevel();
        for(int i=0;i<30;i++) {
            support.setPos(support.position().add(.01,-.02,.01));PlatformPhysics.carry(player);
        }
        near(h,player.fallDistance,0,"Passive descent accumulates no fall distance");
        near(h,player.getFoodData().getSaturationLevel(),saturation,"Passive transport does not consume saturation");
        h.succeed();
    }
    @GameTest public void packetReferencesAreScoped(GameTestHelper h) {
        var support=h.spawn(EntityTypes.GHAST,2,20,2);
        support.setNoAi(true); support.setNoGravity(true);
        var body=h.spawn(EntityTypes.PIG,2,25,2);body.setNoAi(true);
        body.move(MoverType.SELF,new Vec3(0,-2,0));
        var state=Platforms.state(body);var absolute=body.position();var local=state.contact;
        support.setPos(support.position().add(.2,0,0));PlatformPhysics.carry(body);
        state.pendingReference=new PlatformMovePayload(body.getId(),support.getId(),absolute,local);
        near(h,PlatformMovementReference.resolve(body,absolute).distanceTo(body.position()),0,"Confirmed support rebases delayed packet");
        h.assertTrue(state.pendingReference==null,"Reference consumed once");
        state.pendingReference=new PlatformMovePayload(body.getId(),support.getId()+1,absolute,local);
        h.assertTrue(PlatformMovementReference.resolve(body,absolute).equals(absolute),"Wrong support cannot rebase");
        state.pendingReference=new PlatformMovePayload(body.getId(),support.getId(),absolute,new Vec3(100,100,100));
        h.assertTrue(PlatformMovementReference.resolve(body,absolute).equals(absolute),"Large spoofed reference rejected");
        h.succeed();
    }
    @GameTest public void closerSizeContactAndTransport(GameTestHelper h) {
        var support=h.spawn(EntityTypes.GHAST,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockPlayer(GameType.SURVIVAL);
        body.getAttribute(Attributes.SCALE).setBaseValue(.8*support.getBbWidth()/body.getBbWidth());
        body.refreshDimensions();
        body.setPos(support.position().add(0,5,0));
        body.move(MoverType.SELF,new Vec3(0,-2,0));
        h.assertTrue(Platforms.state(body).support==support,"80% width body lands on larger support");
        body.move(MoverType.SELF,new Vec3(.1,0,0));
        h.assertTrue(Platforms.supported(body),"Closer-size body can walk on surface");
        var before=body.position();
        support.setPos(support.position().add(.2,0,.1));PlatformPhysics.carry(body);
        near(h,body.position().distanceTo(before.add(.2,0,.1)),0,"Closer-size body travels with support");
        body.getAttribute(Attributes.SCALE).setBaseValue(body.getScale()/ .8);
        body.refreshDimensions();
        h.assertFalse(Platforms.eligible(body,support),"Equal-width bodies remain ineligible");
        h.succeed();
    }
    @GameTest public void widthBoundaryAndForeignScale(GameTestHelper h) {
        var support=h.spawn(EntityTypes.PIG,2,20,2); support.setNoAi(true);
        var body=h.makeMockPlayer(GameType.SURVIVAL);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        double limit=.85*support.getBbWidth()/body.getBbWidth();
        body.getAttribute(Attributes.SCALE).setBaseValue(limit*.9999);body.refreshDimensions();
        h.assertTrue(Platforms.eligible(body,support),"External scale just below ratio accepted");
        body.getAttribute(Attributes.SCALE).setBaseValue(limit*1.0001);body.refreshDimensions();
        h.assertFalse(Platforms.eligible(body,support),"External scale above ratio rejected");
        h.succeed();
    }
    @GameTest public void anvilImpactOnce(GameTestHelper h) {
        var support=h.spawn(EntityTypes.GHAST,2,20,2);
        support.setNoAi(true); support.setNoGravity(true);
        support.setHealth(10);
        var block=net.minecraft.world.entity.item.FallingBlockEntity.fall(h.getLevel(),support.blockPosition().above(5),
            net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState());
        block.setHurtsEntities(1,3);
        block.setPos(support.position().add(0,5,0));
        block.fallDistance=4;
        block.move(MoverType.SELF,new Vec3(0,-2,0));
        h.assertTrue(support.getHealth()<10,"Anvil damages contacted support, not just intersecting bodies");
        float health=support.getHealth();
        for(int i=0;i<30;i++) block.move(MoverType.SELF,new Vec3(0,-.04,0));
        near(h,support.getHealth(),health,"Resting anvil does not repeat damage");
        h.succeed();
    }
    @GameTest public void risingSurfaceAndWall(GameTestHelper h) {
        var support=h.spawn(EntityTypes.GHAST,2,20,2);
        support.setNoAi(true); support.setNoGravity(true);
        var body=h.spawn(EntityTypes.PIG,2,25,2); body.setNoAi(true);
        body.setPos(support.position().add(0,4.15,0));
        support.move(MoverType.SELF,new Vec3(0,.3,0));
        h.assertTrue(Platforms.supported(body),"Rising surface catches resting body");
        var wall=body.blockPosition().east();
        for(int y=0;y<3;y++) h.getLevel().setBlockAndUpdate(wall.above(y),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        support.setPos(support.position().add(2,0,0));
        PlatformPhysics.carry(body);
        h.assertTrue(body.getBoundingBox().maxX<=wall.getX()+.001,"Passive transport respects walls");
        h.assertFalse(Platforms.supported(body),"Obstructed transport releases support");
        h.succeed();
    }
    @GameTest public void physicalOnlyAndLifecycle(GameTestHelper h) {
        var support=h.spawn(EntityTypes.GHAST,2,20,2);
        support.setNoAi(true); support.setNoGravity(true);
        var body=h.spawn(EntityTypes.PIG,2,25,2);
        body.setNoAi(true);
        h.assertTrue(h.getLevel().noCollision(body,body.getBoundingBox().move(0,-1.1,0)),
            "Pathfinding/general noCollision sees no new platform");
        body.move(MoverType.SELF,new Vec3(0,-2,0));
        body.move(MoverType.SELF,new Vec3(.5,0,0));
        var local=Platforms.state(body).contact;
        support.yBodyRot=90;
        PlatformPhysics.carry(body);
        near(h,body.position().distanceTo(PlatformGeometry.frame(support).world(local)),0,"Rotation carries local contact");
        support.setPos(support.position().add(5,0,0));
        PlatformPhysics.carry(body);
        h.assertFalse(Platforms.supported(body),"Teleport releases rather than dragging");
        body.setPos(support.position().add(0,3.8,0));
        body.move(MoverType.SELF,new Vec3(0,.5,0));
        h.assertFalse(Platforms.supported(body),"Jump from below passes through");
        var arrow=h.spawn(EntityTypes.ARROW,2,25,2);
        h.assertFalse(Platforms.eligible(arrow,support),"Projectiles excluded");
        var baby=h.spawn(EntityTypes.PIG,2,25,2);
        baby.setBaby(true);
        h.assertFalse(Platforms.eligible(body,baby),"Baby supports excluded");
        h.succeed();
    }
    @GameTest public void stationaryItemsAndChains(GameTestHelper h) {
        var ghast=h.spawn(EntityTypes.GHAST,2,20,2);
        ghast.setNoAi(true); ghast.setNoGravity(true);
        var pig=h.spawn(EntityTypes.PIG,2,25,2); pig.setNoAi(true);
        pig.move(MoverType.SELF,new Vec3(0,-2,0));
        var p=pig.position().add(0,1.3,0);
        var item=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),p.x,p.y,p.z,
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
        h.getLevel().addFreshEntity(item);
        item.move(MoverType.SELF,new Vec3(0,-.6,0));
        h.assertTrue(Platforms.state(item).support==pig,"Item rests on pig above ghast");
        var before=item.position();
        ghast.setPos(ghast.position().add(.2,.25,.1));
        PlatformPhysics.carry(item);
        near(h,item.position().distanceTo(before.add(.2,.25,.1)),0,"Chain processed from base");
        PlatformPhysics.carry(pig); PlatformPhysics.carry(item);
        near(h,item.position().distanceTo(before.add(.2,.25,.1)),0,"Chain not carried twice");
        pig.discard(); PlatformPhysics.carry(item);
        h.assertFalse(Platforms.supported(item),"Removed support releases item");
        h.succeed();
    }
    @GameTest(maxTicks=850) public void fallingBlockRemainsEntity(GameTestHelper h) {
        var support=h.spawn(EntityTypes.GHAST,2,20,2);
        support.setNoAi(true); support.setNoGravity(true);
        var pos=support.blockPosition().above(5);
        var block=net.minecraft.world.entity.item.FallingBlockEntity.fall(h.getLevel(),pos,
            net.minecraft.world.level.block.Blocks.SAND.defaultBlockState());
        block.setPos(support.position().add(0,5,0));
        block.move(MoverType.SELF,new Vec3(0,-2,0));
        h.assertTrue(Platforms.supported(block),"Sand lands on entity");
        h.runAtTickTime(700,()->{
            h.assertFalse(block.isRemoved(),"Sand remains an entity after vanilla 600-tick limit");
            h.assertTrue(Platforms.supported(block),"Sand remains supported");
            h.assertTrue(block.time<10,"Falling expiration pauses while supported");
            support.discard();
            PlatformPhysics.carry(block);
            h.assertFalse(Platforms.supported(block),"Sand resumes falling after support removal");
            h.succeed();
        });
    }
    @GameTest public void profileValidation(GameTestHelper h) {
        var ops=com.mojang.serialization.JsonOps.INSTANCE;
        for(String invalid:java.util.List.of(
            "{\"entity\":\"minecraft:pig\",\"surfaces\":[]}",
            "{\"entity\":\"minecraft:pig\",\"surfaces\":[{\"id\":\"x\",\"y\":1,\"width\":0,\"depth\":1}]}")) {
            h.assertTrue(PlatformDefinition.CODEC.parse(ops,com.google.gson.JsonParser.parseString(invalid)).error().isPresent(),
                "Invalid geometry rejected");
        }
        h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(Platforms.DEFINITIONS).size()==16,"Complete initial profile set");
        h.succeed();
    }
    @GameTest public void movingSupportProof(GameTestHelper h) {
        var support=h.spawn(EntityTypes.GHAST,2,20,2);
        support.setNoAi(true); support.setNoGravity(true);
        var body=h.spawn(EntityTypes.PIG,2,25,2);
        body.setNoAi(true);
        h.assertTrue(Platforms.definition(support)!=null,"Fixture profile loaded");
        h.assertTrue(Platforms.eligible(body,support),"Fixture body eligible");
        h.assertTrue(PlatformPhysics.contact(body,new Vec3(3,-2,0))!=null,"Swept contact is retained even if the movement ends beyond the edge");
        h.assertTrue(PlatformPhysics.contact(body,new Vec3(0,-2,0))!=null,"Fixture contact: "+body.position()+" support "+support.position()+" bbox "+support.getBoundingBox());
        body.move(MoverType.SELF,new Vec3(0,-2,0));
        h.assertTrue(Platforms.supported(body),"Mob lands: y="+body.getY()+" expected="+(support.getY()+4)+" state="+Platforms.state(body).surface+" grounded="+body.onGround());
        near(h,body.getY(),support.getY()+4,"Landing height");
        h.assertTrue(body.onGround(),"Vanilla grounded flag");
        h.assertTrue(body.getVehicle()==null,"Support never creates a passenger");
        Vec3 before=body.position();
        support.setPos(support.position().add(.25,.3,.4));
        PlatformPhysics.carry(body);
        near(h,body.position().distanceTo(before.add(.25,.3,.4)),0,"Three-axis carry");
        PlatformPhysics.carry(body);
        near(h,body.position().distanceTo(before.add(.25,.3,.4)),0,"No double carry");
        body.move(MoverType.SELF,new Vec3(0,.4,0));
        h.assertFalse(Platforms.supported(body),"Jump releases support");
        h.succeed();
    }
    @GameTest public void boatAndPlayerProof(GameTestHelper h) {
        var support=h.spawn(EntityTypes.GHAST,2,20,2);
        support.setNoAi(true); support.setNoGravity(true);
        var boat=h.spawn(EntityTypes.OAK_BOAT,2,25,2);
        var rider=h.makeMockPlayer(GameType.SURVIVAL);
        rider.startRiding(boat,true,true);
        boat.move(MoverType.SELF,new Vec3(0,-2,0));
        h.assertTrue(Platforms.supported(boat),"Occupied boat lands");
        near(h,boat.getGroundFriction(),.6,"Boat has surface friction");
        h.assertFalse(Platforms.supported(rider),"Passenger has no independent support");
        var before=boat.position();
        support.setPos(support.position().add(.25,.2,0));
        PlatformPhysics.carry(boat);
        near(h,boat.position().distanceTo(before.add(.25,.2,0)),0,"Boat transported");
        h.assertTrue(rider.getVehicle()==boat,"Native passenger retained");
        rider.stopRiding();
        boat.discard();
        rider.setPos(support.position().add(0,5,0));
        rider.move(MoverType.SELF,new Vec3(0,-2,0));
        h.assertTrue(Platforms.supported(rider),"Player lands");
        rider.getAttribute(Attributes.SCALE).setBaseValue(10);
        rider.refreshDimensions();
        PlatformPhysics.carry(rider);
        h.assertFalse(Platforms.supported(rider),"Size invalidation releases");
        h.succeed();
    }
    @GameTest public void orientedFootprint(GameTestHelper h) {
        var surface=new PlatformDefinition.Surface("test",0,0,0,4,.2,java.util.Optional.empty());
        var frame=new PlatformGeometry.Frame(Vec3.ZERO,Math.PI/4,1);
        h.assertFalse(PlatformGeometry.overlaps(frame,surface,new net.minecraft.world.phys.AABB(1,0,-1,1.1,1,-.9)),
            "Envelope corners do not create invisible floor");
        h.assertTrue(PlatformGeometry.overlaps(frame,surface,new net.minecraft.world.phys.AABB(.9,0,.9,1,1,1)),
            "Rotated strip provides support");
        h.succeed();
    }
    private static void near(GameTestHelper h,double actual,double expected,String message) {
        h.assertTrue(Math.abs(actual-expected)<1e-4,message+": "+actual+" vs "+expected);
    }
}
