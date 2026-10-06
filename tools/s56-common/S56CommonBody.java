import com.google.gson.*;
import io.github.r3neer.scalebrews.collision.pose.NumericPoseProgram;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import java.nio.file.*;
import java.util.*;

/** IMPLEMENTER replay of native matrix captures in the common runtime classpath. */
public final class S56CommonBody {
    public static void main(String[] args) throws Exception {
        var gson=new Gson(); var rows=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonArray();
        int matrices=0,samples=0;
        for(var row:rows) {
            var object=row.getAsJsonObject();
            var program=gson.fromJson(object.get("program"),NumericPoseProgram.class);
            var bound=program.bind(program.geometry()).orElseThrow();
            for(var sample:object.getAsJsonArray("samples")) {
                var channels=new TreeMap<String,Float>();
                sample.getAsJsonObject().getAsJsonObject("channels").entrySet().forEach(e->channels.put(e.getKey(),e.getValue().getAsFloat()));
                var actual=program.geometry().transforms(bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,true,channels)).orElseThrow());
                var expected=sample.getAsJsonObject().getAsJsonObject("matrices");
                if(actual.size()!=expected.size())throw new AssertionError("S56 COMMON membership");
                for(var entry:expected.entrySet()) {
                    var values=new ArrayList<Float>(); for(var element:entry.getValue().getAsJsonArray())values.add(element.getAsFloat());
                    if(!ModelGeometry.matrix(values).equals(actual.get(entry.getKey()),3e-5f))throw new AssertionError("S56 COMMON native matrix "+entry.getKey());
                    matrices++;
                }
                samples++;
            }
            if(bound.evaluate(null).isPresent() || bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,true)).isPresent())
                throw new AssertionError("S56 COMMON missing inputs admitted");
        }
        System.out.println("S56_COMMON_PASS models="+rows.size()+" samples="+samples+" nativeMatrices="+matrices+" clientClassloading=forbidden");
    }
}
