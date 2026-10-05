package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.AnatomyMode;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Live native controls for the retired runtime; selected independently from prepared physical positives. */
public final class S44RuntimeRetirementTests {
    @GameTest public void disabledMovementAndInteractionStayNative(GameTestHelper h) {
        var support=h.spawn(EntityTypes.GHAST,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var body=h.spawn(EntityTypes.PIG,2,25,2);body.setNoAi(true);body.setNoGravity(true);
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        try {
            h.assertTrue(AnatomyApi.mode(body)==AnatomyMode.DISABLED,"The native control has no accepted anatomy session");
            Vec3 before=body.position(),delta=new Vec3(0,-2,0);
            body.move(MoverType.SELF,delta);
            h.assertTrue(body.position().distanceToSqr(before.add(delta))<1e-12
                    && AnatomyMovement.contact(body)==null && !Platforms.supported(body),
                "Actual native movement crosses the removed ghast height plane without acquiring material support");
            player.setPos(support.position().add(-4,3,0));
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.OAK_BOAT,2));
            h.assertTrue(player.interactOn(support,InteractionHand.MAIN_HAND,new Vec3(0,4,0))==InteractionResult.PASS
                    && player.getMainHandItem().getCount()==2,
                "DISABLED actual interactOn delegates natively without legacy placement or inventory consumption");
        } finally {support.discard();body.discard();player.discard();}
        h.succeed();
    }

    @GameTest public void warnedPlaneDataDoesNotBecomeAnatomicalPhysics(GameTestHelper h) {
        var server=h.getLevel().getServer();
        var plane=new PlatformDefinition(Identifier.parse("minecraft:ghast"),true,.6,Optional.empty(),List.of(
            new PlatformDefinition.Surface("released_top",0,4,0,4,4,Optional.empty())));
        var support=h.spawn(EntityTypes.GHAST,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var body=h.spawn(EntityTypes.PIG,2,25,2);body.setNoAi(true);body.setNoGravity(true);
        Vec3 nativeStart=body.position(),delta=new Vec3(0,-2,0);
        body.move(MoverType.SELF,delta);var nativeEnd=body.position();
        body.setPos(nativeStart);body.setDeltaMovement(Vec3.ZERO);
        AnatomyRuntime.startPrepared(server,Map.of(),Map.of("fixture:released_plane",plane));
        try {
            h.assertTrue(AnatomyApi.ready(body) && AnatomyMovement.canonicalBinding(support)==null,
                "Accepted warned plane data has one explicit data meaning and no executable anatomy binding");
            body.move(MoverType.SELF,delta);
            h.assertTrue(body.position().distanceToSqr(nativeEnd)<1e-12 && AnatomyMovement.contact(body)==null
                    && !Platforms.eligible(body,support)
                    && AnatomyApi.raycast(body,nativeStart,nativeStart.add(0,-4,0)).isEmpty(),
                "READY missing geometry preserves the native move and cannot create a height plane, side or underside");
        } finally {AnatomyRuntime.stop(server);support.discard();body.discard();}
        h.succeed();
    }
}
