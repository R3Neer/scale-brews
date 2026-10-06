package io.github.r3neer.scalebrews.collision.pose;

import com.google.gson.*;
import java.nio.file.Files;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;

/** IMPLEMENTER dedicated/common replay of exact native scalar oracles, not synthetic expected formulas. */
public final class S61LivingInputTests {
    private static void check(boolean ok, String why) { if (!ok) throw new AssertionError("S61 COMMON " + why); }
    private static void bad(Runnable op) { try { op.run(); throw new AssertionError("S61 COMMON invalid data admitted"); } catch (RuntimeException expected) {} }
    @GameTest @SuppressWarnings({"rawtypes", "unchecked"}) public void originalCaptureMatchesCommonActors(GameTestHelper helper) throws Exception {
        var gson = new Gson();
        var file = FabricLoader.getInstance().getGameDir().resolve("s61-implementation/evidence.json");
        var rows = JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonArray("samples");
        int count = 0;
        for (var raw : rows) {
            var row = raw.getAsJsonObject(); var objects = S61ActorInputFixtures.create(helper.getLevel(), row.get("type").getAsString(), row.get("index").getAsInt());
            try {
                var actor = (LivingEntity)objects.getFirst();
                var program = gson.fromJson(row.get("program"), LivingPoseInputSource.Program.class);
                var bound = LivingPoseInputSource.bind((Class)actor.getClass(), program);
                var actual = bound.sample(actor).orElseThrow();
                var expected = row.getAsJsonObject("expected"); check(actual.size() == expected.size(), "scalar membership");
                for (var entry : expected.entrySet()) {
                    check(Float.floatToRawIntBits(entry.getValue().getAsFloat()) == Float.floatToRawIntBits(actual.get(entry.getKey())),
                        row.get("type") + " case" + row.get("index") + " " + entry.getKey() + " expected=" + entry.getValue() + " actual=" + actual.get(entry.getKey())); count++;
                }
                for (int i = 0; i < 20; i++) check(bound.sample(actor).orElseThrow().equals(actual), "reader changes state");
                check(bound.sample(null).isEmpty(), "null actor admitted");
            } finally { S61ActorInputFixtures.destroy(objects); }
        }
        System.out.println("S61_IMPLEMENTER_COMMON_ACTORS_PASS samples=" + rows.size() + " bitExactScalars=" + count + " repeatedReads=" + rows.size() * 20);
        helper.succeed();
    }
    @GameTest public void schemaBoundsAndImmutableBinding(GameTestHelper helper) {
        var channels = new ArrayList<LivingPoseInputSource.Channel>();
        for (int i = 0; i < 64; i++) channels.add(new LivingPoseInputSource.Channel("channel." + i, LivingPoseInputSource.Source.IS_BABY));
        var program = new LivingPoseInputSource.Program(1, Cow.class.getName(), channels);
        var bound = LivingPoseInputSource.bind(Cow.class, program); channels.clear();
        var objects = S61ActorInputFixtures.create(helper.getLevel(), "minecraft:cow", 0);
        try { check(bound.sample((LivingEntity)objects.getFirst()).orElseThrow().size() == 64, "binding aliases mutable input list"); }
        finally { S61ActorInputFixtures.destroy(objects); }
        var tooMany = new ArrayList<>(program.channels()); tooMany.add(new LivingPoseInputSource.Channel("channel.64", LivingPoseInputSource.Source.IS_BABY));
        bad(() -> new LivingPoseInputSource.Program(1, Cow.class.getName(), tooMany));
        bad(() -> new LivingPoseInputSource.Channel("bad:channel", LivingPoseInputSource.Source.IS_BABY));
        bad(() -> new LivingPoseInputSource.Channel("ok", null));
        bad(() -> new LivingPoseInputSource.Program(1, Cow.class.getName(), List.of(program.channels().getFirst(), program.channels().getFirst())));
        bad(() -> new LivingPoseInputSource.Program(2, Cow.class.getName(), program.channels()));
        bad(() -> LivingPoseInputSource.bind(Cow.class, new LivingPoseInputSource.Program(1, "wrong.Actor", program.channels())));
        bad(() -> LivingPoseInputSource.validatedCopy(null));
        helper.succeed();
    }
}
