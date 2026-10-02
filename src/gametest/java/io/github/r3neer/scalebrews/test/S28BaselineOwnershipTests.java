package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.platform.PlatformConnection;
import io.github.r3neer.scalebrews.test.mixin.TestVehicleMoveListenerAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** G4.4 owner oracle: one confirmed carry advances each vanilla movement baseline exactly once. */
public final class S28BaselineOwnershipTests {
    @GameTest
    public void confirmedTransportAdvancesPlayerAndVehicleBaselinesExactlyOnce(GameTestHelper h) {
        var player=(ServerPlayer)h.makeMockServerPlayerInLevel();
        var access=(TestVehicleMoveListenerAccess)player.connection;
        var bridge=(PlatformConnection)player.connection;

        Vec3 playerFirst=playerFirst(access),playerLast=playerLast(access);
        Vec3 playerDelta=new Vec3(.125,.25,-.5);
        bridge.scalebrews$transportBaseline(player,playerDelta);
        exact(playerFirst(access).subtract(playerFirst),playerDelta,
            "Player firstGood baseline must advance exactly once by confirmed transport");
        exact(playerLast(access).subtract(playerLast),playerDelta,
            "Player lastGood baseline must advance exactly once by confirmed transport");

        var boat=h.spawn(EntityTypes.OAK_BOAT,3,2,2);
        access.test$setLastVehicle(boat);
        Vec3 vehicleFirst=vehicleFirst(access),vehicleLast=vehicleLast(access);
        Vec3 vehicleDelta=new Vec3(.0625,-.125,.25);
        bridge.scalebrews$transportBaseline(boat,vehicleDelta);
        exact(vehicleFirst(access).subtract(vehicleFirst),vehicleDelta,
            "Vehicle firstGood baseline must advance exactly once by confirmed transport");
        exact(vehicleLast(access).subtract(vehicleLast),vehicleDelta,
            "Vehicle lastGood baseline must advance exactly once by confirmed transport");

        boat.discard();player.discard();h.succeed();
    }

    private static Vec3 playerFirst(TestVehicleMoveListenerAccess a) {
        return new Vec3(a.test$firstGoodX(),a.test$firstGoodY(),a.test$firstGoodZ());
    }
    private static Vec3 playerLast(TestVehicleMoveListenerAccess a) {
        return new Vec3(a.test$lastGoodX(),a.test$lastGoodY(),a.test$lastGoodZ());
    }
    private static Vec3 vehicleFirst(TestVehicleMoveListenerAccess a) {
        return new Vec3(a.test$vehicleFirstGoodX(),a.test$vehicleFirstGoodY(),a.test$vehicleFirstGoodZ());
    }
    private static Vec3 vehicleLast(TestVehicleMoveListenerAccess a) {
        return new Vec3(a.test$vehicleLastGoodX(),a.test$vehicleLastGoodY(),a.test$vehicleLastGoodZ());
    }
    private static void exact(Vec3 observed,Vec3 expected,String message) {
        if(observed.distanceToSqr(expected)>1.0E-18)
            throw new AssertionError(message+": observed="+observed+" expected="+expected);
    }
}
