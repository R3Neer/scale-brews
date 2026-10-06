import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.*;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** IMPLEMENTER fresh common replay: captured table resources, no client metadata class loading. */
public final class S60FiniteConsumer {
    public static void main(String[] args)throws Exception{
        var gson=new Gson();var capture=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonArray();
        var output=new ArrayList<Object>();int matrices=0,assignments=0;double maxDelta=0;
        for(var item:capture){var family=item.getAsJsonObject();var exported=family.getAsJsonObject("exported");
            var table=FinitePoseProgram.validatedCopy(gson.fromJson(exported.get("table"),FinitePoseProgram.class));
            var programs=new TreeMap<String,NumericPoseProgram>();exported.getAsJsonObject("numericPrograms").entrySet().forEach(e->programs.put(e.getKey(),NumericPoseProgram.validatedCopy(gson.fromJson(e.getValue(),NumericPoseProgram.class))));
            var geometry=programs.firstEntry().getValue().geometry();var schema=new TreeSet<>(table.booleans());
            programs.values().forEach(p->p.nodes().forEach(n->{if(n.op()==NumericPoseProgram.Op.INPUT)schema.add(n.input());}));
            var id=Identifier.parse(exported.get("tableId").getAsString());int[] lookups={0,0};
            var resources=new PoseEngine.Resources(){
                @Override public Optional<PoseProgram> program(Identifier ignored){return Optional.empty();}
                @Override public Optional<FinitePoseProgram> finiteProgram(Identifier requested){lookups[0]++;return requested.equals(id)?Optional.of(table):Optional.empty();}
                @Override public Optional<NumericPoseProgram> numericProgram(Identifier requested){lookups[1]++;return Optional.ofNullable(programs.get(requested.toString()));}
            };
            var bound=new FinitePoseEngine().bind(geometry,Map.of("program",id.toString()),schema,resources).orElseThrow();
            if(lookups[0]!=1||lookups[1]!=programs.size())throw new AssertionError("S60 bind lookup count");
            var seen=new TreeSet<Integer>();var samples=new ArrayList<Object>();
            for(var sample:family.getAsJsonArray("samples")){var source=sample.getAsJsonObject();var channels=new TreeMap<String,Float>();source.getAsJsonObject("channels").entrySet().forEach(e->channels.put(e.getKey(),e.getValue().getAsFloat()));
                int mask=0;for(int i=0;i<table.booleans().size();i++)if(channels.get(table.booleans().get(i))==1f)mask|=1<<i;seen.add(mask);
                var actual=geometry.transforms(bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,true,channels)).orElseThrow());var nativeMatrices=source.getAsJsonObject("matrices");
                if(!actual.keySet().equals(nativeMatrices.keySet()))throw new AssertionError("S60 native membership");
                var recorded=new TreeMap<String,List<Float>>();
                for(var key:actual.keySet()){float[] expected=gson.fromJson(nativeMatrices.get(key),float[].class),got=new float[16];actual.get(key).get(got);
                    if(!new Matrix4f().set(expected).equals(actual.get(key),3e-6f))throw new AssertionError("S60 native matrix "+key+" mask="+mask);
                    for(int i=0;i<16;i++)maxDelta=Math.max(maxDelta,Math.abs((double)expected[i]-got[i]));
                    recorded.put(key,ModelGeometry.values(actual.get(key)));matrices++;
                }
                for(String name:table.booleans()){var invalid=new TreeMap<>(channels);invalid.put(name,-0f);if(bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,true,invalid)).isPresent())throw new AssertionError("S60 negative zero accepted");}
                samples.add(recorded);
            }
            if(seen.size()!=table.rows().size()||lookups[0]!=1||lookups[1]!=programs.size())throw new AssertionError("S60 incomplete assignments/runtime lookup");assignments+=seen.size();output.add(samples);
        }
        Files.writeString(Path.of("common-matrices.json"),new GsonBuilder().setPrettyPrinting().create().toJson(output));
        System.out.println("S60_IMPLEMENTER_COMMON_PASS families="+capture.size()+" assignments="+assignments+" matrices="+matrices+" maxDelta="+maxDelta+" clientVeto=true hotLookups=0");
    }
}
