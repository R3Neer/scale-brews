package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.platform.Platforms;
import io.github.r3neer.scalebrews.test.mixin.TestMountAccess;
import java.util.Comparator;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Native Player.move edge routing and jump-before-next-move retirement on a prepared cow. */
final class S40PreparedSneakJumpProof {
    private S40PreparedSneakJumpProof() {}

    static void run(GameTestHelper h) {
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(6);support.refreshDimensions();
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var impulseControl=h.makeMockPlayer(GameType.SURVIVAL);
        player.getAttribute(Attributes.SCALE).setBaseValue(.35);player.refreshDimensions();
        impulseControl.getAttribute(Attributes.SCALE).setBaseValue(.35);impulseControl.refreshDimensions();
        try {
            Platforms.tick(h.getLevel());
            var frame=AnatomyMovement.queryFrame(support).orElseThrow();
            var face=frame.snapshot().pieces().entrySet().stream()
                .flatMap(entry->java.util.stream.IntStream.range(0,6).mapToObj(index->new Face(entry.getKey(),entry.getValue(),index)))
                .filter(candidate->candidate.piece().faceNormal(candidate.face()).y>.99)
                .filter(candidate->{
                    var target=targetBox(player,candidate);
                    return h.getLevel().noCollision(player,target)
                        && frame.snapshot().pieces().values().stream().noneMatch(piece->piece.overlaps(target));
                })
                .max(Comparator.comparingDouble(candidate->candidate.piece().bounds().getXsize()*candidate.piece().bounds().getZsize()))
                .orElseThrow(()->new AssertionError("S40 prepared cow has no clear top face for real Player.move"));
            var start=targetBox(player,face).getCenter();
            placeAndConfirm(player,support,frame.identity().revision(),face,start);
            h.assertTrue(AnatomyMovement.supported(player),"S40 starts with accepted prepared contact");
            var edgeRequest=new Vec3(face.piece().bounds().getXsize()*2,0,face.piece().bounds().getZsize());
            player.setShiftKeyDown(true);
            player.move(MoverType.SELF,edgeRequest);
            var sneaking=player.position();
            h.assertTrue(AnatomyMovement.supported(player)
                    && sneaking.distanceToSqr(start)<edgeRequest.lengthSqr()*.8,
                "Native Player.move while sneaking must stop at the actual prepared face edge: start="
                    +start+" end="+sneaking+" request="+edgeRequest);

            player.setShiftKeyDown(false);
            placeAndConfirm(player,support,frame.identity().revision(),face,start);
            player.move(MoverType.SELF,edgeRequest);
            h.assertTrue(!AnatomyMovement.supported(player)
                    && player.position().distanceToSqr(start)>sneaking.distanceToSqr(start)+.1,
                "Releasing sneak must permit actual native edge departure");

            int alternateFrames=0;
            for(var down:new Direction[]{Direction.UP,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}) {
                var gravity=new GravityFrame(down);
                AnatomyMovement.gravity(player,gravity);
                var alternate=frame.snapshot().pieces().entrySet().stream()
                    .flatMap(entry->java.util.stream.IntStream.range(0,6)
                        .mapToObj(index->new Face(entry.getKey(),entry.getValue(),index)))
                    .filter(candidate->candidate.piece().faceNormal(candidate.face()).dot(gravity.up())>.99)
                    .filter(candidate->{
                        var target=targetBox(player,candidate);
                        return h.getLevel().noCollision(player,target)
                            && frame.snapshot().pieces().values().stream().noneMatch(piece->piece.overlaps(target));
                    })
                    .max(Comparator.comparingDouble(candidate->candidate.piece().bounds().getSize()));
                if(alternate.isEmpty())continue;
                var alternateFace=alternate.get();var alternateStart=targetBox(player,alternateFace).getCenter();
                placeAndConfirm(player,support,frame.identity().revision(),alternateFace,alternateStart);
                Vec3 alternateRequest=gravity.toWorld(new Vec3(5,0,2));
                player.setShiftKeyDown(true);
                player.move(MoverType.SELF,alternateRequest);
                double guardedDistance=player.position().distanceTo(alternateStart);
                h.assertTrue(AnatomyMovement.supported(player)
                        && guardedDistance<alternateRequest.length()*.8,
                    "Native Player.move must guard prepared material in gravity frame "+down
                        +": start="+alternateStart+" end="+player.position()+" requested="+alternateRequest);
                player.setShiftKeyDown(false);
                placeAndConfirm(player,support,frame.identity().revision(),alternateFace,alternateStart);
                player.move(MoverType.SELF,alternateRequest);
                h.assertTrue(player.position().distanceTo(alternateStart)>guardedDistance+.1,
                    "Shift-off native Player.move must retain more freedom in gravity frame "+down);
                System.out.println("S40_PREPARED_NATIVE_FRAME PASS down="+down
                    +" guardedDistance="+guardedDistance+" freeDistance="+player.position().distanceTo(alternateStart));
                alternateFrames++;
                if(alternateFrames==2)break;
            }
            h.assertTrue(alternateFrames==2,"S40 must exercise two non-DOWN prepared native edge frames");
            AnatomyMovement.gravity(player,GravityFrame.VANILLA);

            placeAndConfirm(player,support,frame.identity().revision(),face,start);
            impulseControl.setPos(start.add(20,0,0));
            impulseControl.setOnGround(true);impulseControl.setDeltaMovement(Vec3.ZERO);
            player.setOnGround(true);player.setDeltaMovement(Vec3.ZERO);
            player.setYRot(0);impulseControl.setYRot(0);
            player.setSprinting(false);impulseControl.setSprinting(false);
            long transportBefore=transportSequence(player);
            Vec3 positionBefore=player.position();
            ((TestMountAccess)impulseControl).test$jump();
            ((TestMountAccess)player).test$jump();
            h.assertTrue(player.getDeltaMovement().distanceToSqr(impulseControl.getDeltaMovement())<1e-12,
                "Prepared support jump must preserve the same native impulse as a no-material control");
            h.assertTrue(AnatomyMovement.contact(player)==null,
                "Native jump must retire material contact before a later ROOT interval");
            support.move(MoverType.SELF,new Vec3(.04,0,.02));
            AnatomyMovement.carry(player);
            h.assertTrue(player.position().distanceToSqr(positionBefore)<1e-12
                    && transportSequence(player)==transportBefore,
                "A ROOT interval between jump impulse and player move must not carry or account the released player");
            player.move(MoverType.SELF,player.getDeltaMovement());
            h.assertTrue(AnatomyMovement.contact(player)==null,
                "First own move after jump must not resurrect the old material relation");
        } finally {player.discard();impulseControl.discard();support.discard();}
    }

    private static AABB targetBox(net.minecraft.world.entity.player.Player player,Face face) {
        double[] local={.5,.5,.5};local[face.face()/2]=face.face()%2;
        var point=face.piece().point(new Vec3(local[0],local[1],local[2]));
        var normal=face.piece().faceNormal(face.face());
        var box=player.getBoundingBox();
        double extent=(box.getXsize()*Math.abs(normal.x)+box.getYsize()*Math.abs(normal.y)+box.getZsize()*Math.abs(normal.z))*.5;
        return box.move(point.add(normal.scale(extent+.002)).subtract(box.getCenter()));
    }

    private static void placeAndConfirm(net.minecraft.world.entity.player.Player player,
            net.minecraft.world.entity.animal.cow.Cow support,long revision,Face face,Vec3 center) {
        AnatomyMovement.clear(player);
        player.setPos(player.position().add(center.subtract(player.getBoundingBox().getCenter())));
        player.setDeltaMovement(Vec3.ZERO);player.setOnGround(true);
        double[] local={.5,.5,.5};local[face.face()/2]=face.face()%2;
        var surface=new SurfaceContact(support.getUUID(),revision,face.pieceId(),face.face(),
            new Vec3(local[0],local[1],local[2]),face.piece().faceNormal(face.face()),support.level().getGameTime());
        if(!AnatomyMovement.confirm(player,support,surface) || !AnatomyMovement.supported(player))
            throw new AssertionError("S40 could not confirm prepared cow contact at native player setup");
    }

    private static long transportSequence(net.minecraft.world.entity.Entity body) {
        var transport=AnatomyMovement.transport(body);
        return transport==null?0:transport.sequence();
    }

    private record Face(String pieceId,ConvexBox piece,int face) {}
}
