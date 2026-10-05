package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.platform.*;
import io.github.r3neer.scalebrews.collision.integration.CollisionRules;
import io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Released data/policy assertions retained after S44 retires the upper-plane engine. */
public class PlatformTests {
    @GameTest public void decodedSwitches(GameTestHelper h) {
        var profile=new PlatformDefinition(Identifier.parse("minecraft:ghast"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("fixture:ghast"),Identifier.parse("scalebrews:static"),AnatomyFilter.DEFAULT)));
        for(String json:List.of("{\"enabled\":false}","{\"bodies\":{\"boats\":false}}",
                "{\"supports\":{\"minecraft:ghast\":false}}","{\"max_width_ratio\":0.1}")) {
            var policy=PlatformPolicy.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString(json)).getOrThrow();
            h.assertFalse(CollisionRules.allows(LegacyCollisionData.policy(policy),LegacyCollisionData.profilePolicy(profile),
                "boats",profile.entity(),.3),"Decoded switch affects the sole policy predicate: "+json);
        }
        h.assertTrue(CollisionRules.allows(LegacyCollisionData.policy(PlatformPolicy.DEFAULT),LegacyCollisionData.profilePolicy(profile),
            "boats",profile.entity(),.85),"Defaults include equality at limit");
        var happy=h.spawn(EntityTypes.HAPPY_GHAST,2,30,2);
        var body=h.makeMockPlayer(GameType.SURVIVAL);
        h.assertFalse(Platforms.eligible(body,happy),"Happy Ghast retains its native support owner without a material binding");
        happy.discard();body.discard();h.succeed();
    }
    @GameTest public void widthBoundaryAndForeignScale(GameTestHelper h) {
        var support=h.spawn(EntityTypes.PIG,2,20,2);support.setNoAi(true);
        var body=h.makeMockPlayer(GameType.SURVIVAL);
        var profile=new PlatformDefinition(Identifier.parse("minecraft:pig"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("fixture:pig"),Identifier.parse("scalebrews:static"),AnatomyFilter.DEFAULT)));
        var binding=LegacyCollisionData.decode(profile).binding().orElseThrow();
        AnatomyMovement.registerCore(support,ignored->Optional.empty(),binding);
        try {
            support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
            double limit=.85*support.getBbWidth()/body.getBbWidth();
            body.getAttribute(Attributes.SCALE).setBaseValue(limit*.9999);body.refreshDimensions();
            h.assertTrue(Platforms.eligible(body,support),"External scale just below ratio accepted");
            body.getAttribute(Attributes.SCALE).setBaseValue(limit*1.0001);body.refreshDimensions();
            h.assertFalse(Platforms.eligible(body,support),"External scale above ratio rejected");
            h.assertTrue(AnatomyMovement.queryFrame(support).isEmpty(),"Policy registration never invents available geometry");
        } finally {support.discard();body.discard();}
        h.succeed();
    }
    @GameTest public void profileValidation(GameTestHelper h) {
        var ops=com.mojang.serialization.JsonOps.INSTANCE;
        for(String invalid:List.of(
            "{\"entity\":\"minecraft:pig\",\"surfaces\":[]}",
            "{\"entity\":\"minecraft:pig\",\"surfaces\":[{\"id\":\"x\",\"y\":1,\"width\":0,\"depth\":1}]}"))
            h.assertTrue(PlatformDefinition.CODEC.parse(ops,com.google.gson.JsonParser.parseString(invalid)).error().isPresent(),
                "Invalid geometry rejected");
        h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(Platforms.DEFINITIONS).size()==0,
            "Cancelled built-in plane profiles no longer ship as executable support definitions");
        h.succeed();
    }
}
