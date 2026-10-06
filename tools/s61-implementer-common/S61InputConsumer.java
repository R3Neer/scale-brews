import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import io.github.r3neer.scalebrews.collision.pose.LivingPoseInputSource;
import net.minecraft.world.entity.LivingEntity;

/** Cold input binding under client veto; actor execution is separately proved by native/server lanes. */
public final class S61InputConsumer {
    public static void main(String[] args) throws Exception {
        var gson = new Gson();
        var capture = JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
        int bindings = 0;
        var cowSamples = new JsonArray();
        for (var item : capture.getAsJsonArray("samples")) {
            var sample = item.getAsJsonObject();
            var program = LivingPoseInputSource.validatedCopy(gson.fromJson(sample.get("program"), LivingPoseInputSource.Program.class));
            var actor = Class.forName(program.actor(), false, S61InputConsumer.class.getClassLoader()).asSubclass(LivingEntity.class);
            var bound = LivingPoseInputSource.bind(actor, program);
            if (bound.sample(null).isPresent()) throw new AssertionError("Null actor accepted");
            for (var field : bound.getClass().getDeclaredFields()) {
                var type = field.getType();
                if (type != Class.class && type != String[].class && type != LivingPoseInputSource.Source[].class)
                    throw new AssertionError("Unexpected retained state: " + field);
            }
            bindings++;
            if (sample.get("type").getAsString().equals("minecraft:cow")) {
                var pose = new JsonObject();
                pose.add("channels", sample.get("expected")); pose.add("matrices", sample.get("matrices"));
                cowSamples.add(pose);
            }
        }
        if (bindings != 32 || cowSamples.size() != 8) throw new AssertionError("Incomplete capture");
        var family = new JsonObject(); family.add("exported", capture.get("model"));
        family.addProperty("alias", 0); family.add("samples", cowSamples);
        var poses = new JsonArray(); poses.add(family);
        var posePath = Path.of("pose-export.json");
        Files.writeString(posePath, new GsonBuilder().setPrettyPrinting().create().toJson(poses));
        S60FiniteConsumer.main(new String[]{posePath.toString()});
        System.out.println("S61_IMPLEMENTER_COLD_INPUT_PASS bindings=" + bindings + " clientVeto=true actorExecution=false");
    }
}
