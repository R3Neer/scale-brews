import com.google.gson.*;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.pose.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** IMPLEMENTER: captured original numeric DTO through the new revision resource engine. */
public final class S57NumericEngineReplay {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError("S57 COMMON "+why);}
    public static void main(String[] args) throws Exception {
        var rows=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonArray();
        int matrices=0,samples=0,models=0;var id=Identifier.parse("proof:program");
        for(var row:rows){var p=NumericPoseProgram.validatedCopy(new Gson().fromJson(row.getAsJsonObject().get("program"),NumericPoseProgram.class));
            var channels=new HashSet<String>();p.nodes().forEach(n->{if(n.op()==NumericPoseProgram.Op.INPUT)channels.add(n.input());});
            var calls=new AtomicInteger();var resources=new PoseEngine.Resources(){
                public Optional<PoseProgram> program(Identifier unused){return Optional.empty();}
                public Optional<NumericPoseProgram> numericProgram(Identifier key){calls.incrementAndGet();return key.equals(id)?Optional.of(p):Optional.empty();}
            };
            var engine=new NumericPoseEngine();var bound=engine.bind(p.geometry(),Map.of("program",id.toString()),channels,resources).orElseThrow();
            check(calls.get()==1,"resource resolved more than once during engine bind");
            check(bound.evaluate(null).isEmpty() && bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,true)).isEmpty(),"missing input admitted");
            for(var sample:row.getAsJsonObject().getAsJsonArray("samples")){var s=sample.getAsJsonObject();var values=new TreeMap<String,Float>();s.getAsJsonObject("channels").entrySet().forEach(e->values.put(e.getKey(),e.getValue().getAsFloat()));
                var inputs=new PoseEngine.Inputs(0,0,0,0,0,true,values);var actual=p.geometry().transforms(bound.evaluate(inputs).orElseThrow());
                check(actual.keySet().equals(s.getAsJsonObject("matrices").keySet()),"joint membership");
                for(var e:s.getAsJsonObject("matrices").entrySet()){float[] f=new float[16];for(int i=0;i<16;i++)f[i]=e.getValue().getAsJsonArray().get(i).getAsFloat();check(new Matrix4f().set(f).equals(actual.get(e.getKey()),3e-5f),"original matrix "+e.getKey());matrices++;}samples++;
            }
            check(calls.get()==1,"resource lookup leaked into evaluate");models++;
        }
        PoseEngine.Resources oldSam=unused->Optional.empty();check(oldSam.numericProgram(id).isEmpty(),"old Resources SAM changed");
        System.out.println("S57_COMMON_ENGINE_PASS models="+models+" samples="+samples+" nativeMatrices="+matrices+" resourceLookup=bindOnly clientVeto=true oldSAM=empty");
    }
}
