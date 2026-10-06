package io.github.r3neer.scalebrews.collision.internal;

import com.google.gson.*;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData;
import io.github.r3neer.scalebrews.collision.pose.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** IMPLEMENTER: revision-owned numeric resources, not proof of actor/root/material authority. */
public final class S57NumericCatalogTests {
    private static final Identifier ENGINE = Identifier.parse("scalebrews:numeric_program");
    private static final Identifier ENTITY = Identifier.parse("proof:s57_entity");
    private static final Identifier MODEL = Identifier.parse("proof:s57_model");
    private static final Identifier PROGRAM = Identifier.parse("proof:s57_program");
    private static void check(boolean ok, String why) { if (!ok) throw new AssertionError("S57 IMPL " + why); }
    private static void bad(Runnable operation) {
        try { operation.run(); throw new AssertionError("S57 IMPL malformed candidate admitted"); }
        catch (RuntimeException expected) {}
    }
    private static ModelGeometry geometry() {
        var rest = new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1);
        return new ModelGeometry(2, "proof:s57", "1",
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(rest.matrix()), rest)),
            List.of(new ModelGeometry.Piece("cube", "root", List.of(0d,0d,0d), List.of(1d,1d,1d), null)),
            ModelGeometry.values(new Matrix4f()));
    }
    private static NumericPoseProgram program(float add) {
        return new NumericPoseProgram(1, "proof:s57", "1", geometry(), List.of(
            new NumericPoseProgram.Node(NumericPoseProgram.Op.INPUT, -1,-1,0,"phase",null,-1),
            new NumericPoseProgram.Node(NumericPoseProgram.Op.CONSTANT,-1,-1,add,null,null,-1),
            new NumericPoseProgram.Node(NumericPoseProgram.Op.ADD,0,1,0,null,null,-1)),
            List.of(new NumericPoseProgram.Write("root",0,2)));
    }
    private static CollisionBinding binding(Identifier entity, Identifier model, Identifier program, Set<String> channels) {
        return new CollisionBinding(1,entity,Map.of(),
            new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART,model,Map.of(),AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(ENGINE,Map.of("program",program.toString()),channels),
            LegacyCollisionData.ENTITY_ROOT,CollisionPolicy.Patch.EMPTY,Set.of());
    }
    private static PoseEngine.Inputs inputs() { return new PoseEngine.Inputs(0,0,0,0,0,true,Map.of("phase",2f)); }

    @GameTest
    public void registeredEngineKeepsResourceSamAndExplicitChannelContract(GameTestHelper h) {
        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        BuiltInPoseEngines.initialize(); check(CollisionEngines.pose(ENGINE).orElseThrow()==engine,"reserved owner changed");
        PoseEngine.Resources sam = ignored -> Optional.empty();
        check(sam.numericProgram(PROGRAM).isEmpty(),"old Resources SAM gained implicit resource");
        var p = program(0);
        var resources = new PoseEngine.Resources() {
            @Override public Optional<PoseProgram> program(Identifier id) { return Optional.empty(); }
            @Override public Optional<NumericPoseProgram> numericProgram(Identifier id) { return id.equals(PROGRAM)?Optional.of(p):Optional.empty(); }
        };
        check(engine.evaluate(p.geometry(),inputs(),Map.of("program",PROGRAM.toString())).isEmpty(),"direct evaluation has no revision");
        check(engine.bind(p.geometry(),Map.of("program",PROGRAM.toString()),Set.of("phase"),sam).isEmpty(),"missing resource admitted");
        check(engine.bind(p.geometry(),Map.of("program",PROGRAM.toString()),Set.of(),resources).isEmpty(),"undeclared input admitted");
        check(engine.bind(p.geometry(),Map.of("program",PROGRAM.toString(),"unknown","1"),Set.of("phase"),resources).isEmpty(),"unknown parameter admitted");
        var bound=engine.bind(p.geometry(),Map.of("program",PROGRAM.toString()),Set.of("phase","extra"),resources).orElseThrow();
        check(bound.evaluate(inputs()).isEmpty(),"declared extra input ignored");
        check(bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,true,Map.of("phase",2f,"extra",0f))).orElseThrow().get("root").m30()==.125f,"prepared result differs");
        h.succeed();
    }

    @GameTest
    public void replacementPreservesOldBoundAndRejectsMissingForeignOrAliasedResources(GameTestHelper h) {
        var catalog=new WorldAnatomyCatalog();var p=program(0);var select=binding(ENTITY,MODEL,PROGRAM,Set.of("phase"));
        var first=catalog.replace(Map.of(MODEL.toString(),p.geometry()),Map.of(),Map.of(),Map.of(PROGRAM.toString(),p),List.of(select));
        var old=first.bindings().get(ENTITY).poses();var epoch=UUID.randomUUID();var packets=catalog.preparedPackets(epoch);
        for(int i=0;i<100;i++)check(catalog.preparedPackets(epoch)==packets,"packet cache identity lost");
        bad(()->catalog.replace(Map.of(MODEL.toString(),p.geometry()),Map.of(),Map.of(),Map.of(),List.of(select)));
        check(catalog.snapshot()==first && catalog.preparedPackets(epoch)==packets,"failed revision changed snapshot/bundle");
        var foreign=new ModelGeometry(2,"foreign","1",p.geometry().parts(),p.geometry().pieces(),p.geometry().modelTransform());
        bad(()->catalog.replace(Map.of(MODEL.toString(),foreign),Map.of(),Map.of(),Map.of(PROGRAM.toString(),p),List.of(select)));
        bad(()->catalog.replace(Map.of(),Map.of(),Map.of(),Map.of(PROGRAM.toString(),p),List.of(select)));
        bad(()->catalog.replace(Map.of(),Map.of(),Map.of(),Map.of("alias",p,"minecraft:alias",p),List.of()));
        check(catalog.snapshot()==first,"rejection mutated accepted revision");
        var second=catalog.replace(Map.of(MODEL.toString(),p.geometry()),Map.of(),Map.of(),Map.of(PROGRAM.toString(),program(16)),List.of(select));
        check(second.revision()==first.revision()+1 && second.bindings().get(ENTITY).poses().evaluate(inputs()).orElseThrow().get("root").m30()==1.125f,"new resource not revision-owned");
        check(old.evaluate(inputs()).orElseThrow().get("root").m30()==.125f,"old Bound retargeted by reload");
        try { first.numericPosePrograms().clear(); throw new AssertionError("mutable resource map"); } catch(UnsupportedOperationException expected){}
        h.succeed();
    }

    @GameTest
    public void nestedInvalidProgramsAndResourceMapLimitRejectBeforeSwap(GameTestHelper h) {
        var p=program(0);var tree=new Gson().toJsonTree(p).getAsJsonObject();
        tree.getAsJsonArray("writes").get(0).getAsJsonObject().addProperty("node",999);
        bad(()->NumericPoseProgram.validatedCopy(new Gson().fromJson(tree,NumericPoseProgram.class)));
        var corrupt=new Gson().toJsonTree(p).getAsJsonObject();
        corrupt.getAsJsonObject("geometry").getAsJsonArray("parts").get(0).getAsJsonObject().getAsJsonObject("sourcePose").addProperty("xScale",0);
        bad(()->NumericPoseProgram.validatedCopy(new Gson().fromJson(corrupt,NumericPoseProgram.class)));
        var values=new HashMap<String,NumericPoseProgram>();for(int i=0;i<4097;i++)values.put("proof:p"+i,p);
        var catalog=new WorldAnatomyCatalog();var before=catalog.snapshot();
        bad(()->catalog.replace(Map.of(),Map.of(),Map.of(),values,List.of()));check(catalog.snapshot()==before,"oversized map swapped");
        check(NumericPoseProgram.validatedCopy(p).equals(p),"validated copy changed data");
        h.succeed();
    }

    @GameTest
    public void transferCarriesOriginalCapturedProgramsAndPreparedPoses(GameTestHelper h) throws Exception {
        JsonArray rows;
        try (var stream=S57NumericCatalogTests.class.getResourceAsStream("/fixtures/s57-original-programs.json")) {
            check(stream!=null,"native capture fixture missing");
            rows=JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
        }
        Map<String,ModelGeometry> models=new LinkedHashMap<>();Map<String,NumericPoseProgram> programs=new LinkedHashMap<>();
        var bindings=new ArrayList<CollisionBinding>();int index=0;
        for(var row:rows){var p=new Gson().fromJson(row.getAsJsonObject().get("program"),NumericPoseProgram.class);
            var id=Identifier.parse("proof:native_"+index++);models.put(id.toString(),p.geometry());programs.put(id.toString(),p);
            var channels=new HashSet<String>();p.nodes().forEach(n->{if(n.op()==NumericPoseProgram.Op.INPUT)channels.add(n.input());});bindings.add(binding(id,id,id,channels));}
        byte[] a=AnatomyCatalogTransfer.serializedBundle(models,Map.of(),Map.of(),programs,bindings);
        var reverse=new LinkedHashMap<String,NumericPoseProgram>();programs.entrySet().stream().sorted(Map.Entry.<String,NumericPoseProgram>comparingByKey().reversed()).forEach(e->reverse.put(e.getKey(),e.getValue()));
        check(Arrays.equals(a,AnatomyCatalogTransfer.serializedBundle(models,Map.of(),Map.of(),reverse,bindings)),"bundle depends on program map order");
        var epoch=UUID.randomUUID();var encoded=AnatomyCatalogTransfer.encode(epoch,37,models,Map.of(),Map.of(),programs,bindings);var receiver=new AnatomyCatalogTransfer();
        for(var packet:encoded){var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
            try{AnatomyCatalogPayload.CODEC.encode(buffer,packet);var decoded=AnatomyCatalogPayload.CODEC.decode(buffer);receiver.accept(decoded);}finally{buffer.release();}}
        check(receiver.ready() && receiver.revision()==37 && receiver.snapshot().numericPosePrograms().equals(programs),"program catalog did not transfer atomically");
        int matrices=0,samples=0;index=0;
        for(var row:rows){var id=Identifier.parse("proof:native_"+index++);var bound=receiver.snapshot().bindings().get(id).poses();var g=receiver.snapshot().models().get(id.toString());
            check(bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,true)).isEmpty(),"native capture inputs omitted");
            for(var sample:row.getAsJsonObject().getAsJsonArray("samples")){var s=sample.getAsJsonObject();var channels=new TreeMap<String,Float>();s.getAsJsonObject("channels").entrySet().forEach(e->channels.put(e.getKey(),e.getValue().getAsFloat()));
                var actual=g.transforms(bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,true,channels)).orElseThrow());
                check(actual.keySet().equals(s.getAsJsonObject("matrices").keySet()),"native matrix membership changed");
                for(var e:s.getAsJsonObject("matrices").entrySet()){float[] f=new float[16];for(int i=0;i<16;i++)f[i]=e.getValue().getAsJsonArray().get(i).getAsFloat();check(new Matrix4f().set(f).equals(actual.get(e.getKey()),3e-5f),"original captured matrix changed "+e.getKey());matrices++;}samples++;}}
        for(var packet:encoded)check(!receiver.accept(packet),"stale revision accepted");
        System.out.println("S57_IMPL_CATALOG_REPLAY_PASS programs="+programs.size()+" samples="+samples+" nativeMatrices="+matrices+" fragments="+encoded.size());
        h.succeed();
    }

    @GameTest
    public void wireVersionAndMandatoryNumericMapFailClosed(GameTestHelper h) throws Exception {
        long required=AnatomyApi.mask(AnatomyApi.Capability.NUMERIC_POSE_PROGRAMS);
        check(AnatomyApi.PROTOCOL_VERSION==7 && AnatomyApi.compatible(7,required) && !AnatomyApi.compatible(6,0)
            && !AnatomyApi.compatible(7,1L<<60),"wire/capability negotiation changed");
        var json=JsonParser.parseString(new String(AnatomyCatalogTransfer.serializedBundle(Map.of(),List.of()),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        check(json.get("numeric_pose_programs").isJsonObject(),"new map not present");json.remove("numeric_pose_programs");
        byte[] bytes=json.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        var receiver=new AnatomyCatalogTransfer();var before=receiver.snapshot();
        bad(()->receiver.accept(new AnatomyCatalogPayload(UUID.randomUUID(),7,required,1,0,1,bytes.length,hash,bytes)));
        check(receiver.snapshot()==before && receiver.revision()==-1,"invalid schema swapped accepted snapshot");
        receiver.rejectPending();check(!receiver.announced(),"failed initial candidate retained authority");h.succeed();
    }
}
