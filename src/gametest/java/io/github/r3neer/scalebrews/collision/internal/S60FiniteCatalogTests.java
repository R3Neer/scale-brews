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

/** IMPLEMENTER: finite resource binding/selection and atomic revision transfer only. */
public final class S60FiniteCatalogTests {
    private static final Identifier ENGINE=Identifier.parse("scalebrews:finite_numeric_program");
    private static final Identifier TABLE=Identifier.parse("proof:s60_table"),MODEL=Identifier.parse("proof:s60_model"),ENTITY=Identifier.parse("proof:s60_entity");
    private static final String ZERO="proof:zero",ONE="proof:one";
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError("S60 IMPL "+why);}
    private static void bad(Runnable op){try{op.run();throw new AssertionError("S60 IMPL invalid candidate admitted");}catch(RuntimeException expected){}}
    private static ModelGeometry geometry(){var p=new ModelGeometry.SourcePose(0,0,0,0,0,0,1,1,1);return new ModelGeometry(2,"proof:s60","1",
        List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(p.matrix()),p)),
        List.of(new ModelGeometry.Piece("cube","root",List.of(0d,0d,0d),List.of(1d,1d,1d),null)),ModelGeometry.values(new Matrix4f()));}
    private static NumericPoseProgram numeric(float add,String input){return new NumericPoseProgram(1,"proof:s60","1",geometry(),List.of(
        new NumericPoseProgram.Node(NumericPoseProgram.Op.INPUT,-1,-1,0,input,null,-1),
        new NumericPoseProgram.Node(NumericPoseProgram.Op.CONSTANT,-1,-1,add,null,null,-1),
        new NumericPoseProgram.Node(NumericPoseProgram.Op.ADD,0,1,0,null,null,-1)),List.of(new NumericPoseProgram.Write("root",0,2)));}
    private static FinitePoseProgram table(){return new FinitePoseProgram(1,"proof:s60","1",List.of("boolean.0","boolean.1"),List.of(
        new FinitePoseProgram.Row(3,ONE),new FinitePoseProgram.Row(0,ZERO),new FinitePoseProgram.Row(2,ZERO),new FinitePoseProgram.Row(1,ONE)));}
    private static Map<String,NumericPoseProgram> numericMap(float shift){return Map.of(ZERO,numeric(shift,"float.0"),ONE,numeric(shift+16,"float.1"));}
    private static PoseEngine.Inputs inputs(int mask){return new PoseEngine.Inputs(0,0,0,0,0,true,Map.of("boolean.0",(mask&1)==0?0f:1f,"boolean.1",(mask&2)==0?0f:1f,"float.0",2f,"float.1",2f));}
    private static Set<String> schema(){return inputs(0).channels().keySet();}
    private static CollisionBinding binding(){return new CollisionBinding(1,ENTITY,Map.of(),
        new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART,MODEL,Map.of(),AnatomyFilter.DEFAULT),
        new CollisionBinding.Pose(ENGINE,Map.of("program",TABLE.toString()),schema()),LegacyCollisionData.ENTITY_ROOT,CollisionPolicy.Patch.EMPTY,Set.of());}

    @GameTest public void exactPredicatesAndDetachedResourceSelection(GameTestHelper h){
        var engine=CollisionEngines.pose(ENGINE).orElseThrow();BuiltInPoseEngines.initialize();check(CollisionEngines.pose(ENGINE).orElseThrow()==engine,"reserved owner changed");
        PoseEngine.Resources sam=id->Optional.empty();check(sam.finiteProgram(TABLE).isEmpty(),"SAM default gained resource");
        int[] lookups={0,0};var resources=new PoseEngine.Resources(){
            @Override public Optional<PoseProgram> program(Identifier id){return Optional.empty();}
            @Override public Optional<FinitePoseProgram> finiteProgram(Identifier id){lookups[0]++;return id.equals(TABLE)?Optional.of(table()):Optional.empty();}
            @Override public Optional<NumericPoseProgram> numericProgram(Identifier id){lookups[1]++;return Optional.ofNullable(numericMap(0).get(id.toString()));}};
        var bound=engine.bind(geometry(),Map.of("program",TABLE.toString()),schema(),resources).orElseThrow();check(lookups[0]==1&&lookups[1]==2,"lookup count not 1+U");
        for(int i=0;i<100;i++)for(int mask=0;mask<4;mask++)check(bound.evaluate(inputs(mask)).orElseThrow().get("root").m30()==((mask&1)==0?.125f:1.125f),"selected wrong row");
        check(lookups[0]==1&&lookups[1]==2,"runtime resource lookup");
        for(float value:new float[]{-0f,.5f,-1f,2f,Math.nextDown(1f)}){var channels=new TreeMap<>(inputs(0).channels());channels.put("boolean.0",value);check(bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,true,channels)).isEmpty(),"threshold/noncanonical boolean admitted "+value);}
        for(String name:schema()){var channels=new TreeMap<>(inputs(0).channels());channels.remove(name);check(bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,true,channels)).isEmpty(),"missing union input admitted "+name);}
        check(bound.evaluate(null).isEmpty()&&bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,false,inputs(0).channels())).isEmpty(),"nonordinary/null admitted");
        check(engine.evaluate(geometry(),inputs(0),Map.of("program",TABLE.toString())).isEmpty(),"direct evaluation has revision");
        check(engine.bind(geometry(),Map.of("program",TABLE.toString()),Set.of("boolean.0","boolean.1","float.0"),resources).isEmpty(),"unselected row schema ignored");
        check(engine.bind(geometry(),Map.of("program",TABLE.toString(),"extra","x"),schema(),resources).isEmpty(),"unknown parameter");
        System.out.println("S60_IMPL_SELECT_PASS rows=4 evaluations=400 bindLookups=3 hotLookups=0");h.succeed();
    }

    @GameTest public void allRowsValidatedBeforeAtomicCatalogSwap(GameTestHelper h){
        var catalog=new WorldAnatomyCatalog();var tables=Map.of(TABLE.toString(),table());var first=catalog.replace(Map.of(MODEL.toString(),geometry()),Map.of(),Map.of(),numericMap(0),tables,List.of(binding()));
        var bound=first.bindings().get(ENTITY).poses();var epoch=UUID.randomUUID();var packets=catalog.preparedPackets(epoch);
        for(int i=0;i<100;i++)check(catalog.preparedPackets(epoch)==packets,"packet cache lost");
        bad(()->catalog.replace(Map.of(MODEL.toString(),geometry()),Map.of(),Map.of(),Map.of(ZERO,numeric(0,"float.0")),tables,List.of(binding())));
        var foreign=new NumericPoseProgram(1,"foreign","1",new ModelGeometry(2,"foreign","1",geometry().parts(),geometry().pieces(),geometry().modelTransform()),numeric(16,"float.1").nodes(),numeric(16,"float.1").writes());
        bad(()->catalog.replace(Map.of(MODEL.toString(),geometry()),Map.of(),Map.of(),Map.of(ZERO,numeric(0,"float.0"),ONE,foreign),tables,List.of(binding())));
        bad(()->catalog.replace(Map.of(),Map.of(),Map.of(),numericMap(0),tables,List.of(binding())));
        bad(()->catalog.replace(Map.of(),Map.of(),Map.of(),numericMap(0),Map.of("alias",table(),"minecraft:alias",table()),List.of()));
        bad(()->catalog.replace(Map.of(),Map.of(),Map.of(),Map.of(ZERO,numeric(0,"boolean.0"),ONE,numeric(16,"float.1")),tables,List.of()));
        check(catalog.snapshot()==first&&catalog.preparedPackets(epoch)==packets,"invalid family changed snapshot/cache");
        var second=catalog.replace(Map.of(MODEL.toString(),geometry()),Map.of(),Map.of(),numericMap(32),tables,List.of(binding()));
        check(second.revision()==first.revision()+1,"revision did not advance");
        for(int m=0;m<4;m++){check(bound.evaluate(inputs(m)).orElseThrow().get("root").m30()==((m&1)==0?.125f:1.125f),"old Bound retargeted");check(second.bindings().get(ENTITY).poses().evaluate(inputs(m)).orElseThrow().get("root").m30()==((m&1)==0?2.125f:3.125f),"new revision unchanged");}
        try{first.finitePosePrograms().clear();throw new AssertionError("mutable finite map");}catch(UnsupportedOperationException expected){}
        h.succeed();
    }

    @GameTest public void completeCanonicalTablesAndCaps(GameTestHelper h){
        check(FinitePoseProgram.validatedCopy(table()).rows().getFirst().mask()==0,"rows not canonical");
        bad(()->new FinitePoseProgram(1,"s","v",List.of("b","a"),table().rows()));
        bad(()->new FinitePoseProgram(1,"s","v",List.of("a"),List.of(new FinitePoseProgram.Row(0,ZERO),new FinitePoseProgram.Row(0,ONE))));
        bad(()->new FinitePoseProgram(1,"s","v",List.of("a"),List.of(new FinitePoseProgram.Row(0,ZERO))));
        bad(()->new FinitePoseProgram(1,"s","v",List.of("a"),List.of(new FinitePoseProgram.Row(0,ZERO),new FinitePoseProgram.Row(2,ONE))));
        bad(()->new FinitePoseProgram.Row(0,"proof:"+"a".repeat(251)));
        var zero=new FinitePoseProgram(1,"s","v",List.of(),List.of(new FinitePoseProgram.Row(0,ZERO)));check(zero.rows().size()==1,"B0 absent");
        var huge=new HashMap<String,FinitePoseProgram>();for(int i=0;i<4097;i++)huge.put("proof:p"+i,zero);
        var catalog=new WorldAnatomyCatalog();var before=catalog.snapshot();bad(()->catalog.replace(Map.of(),Map.of(),Map.of(),numericMap(0),huge,List.of()));check(catalog.snapshot()==before,"oversized map swapped");
        var tree=new Gson().toJsonTree(table()).getAsJsonObject();tree.getAsJsonArray("rows").get(0).getAsJsonObject().addProperty("mask",31);
        bad(()->FinitePoseProgram.validatedCopy(new Gson().fromJson(tree,FinitePoseProgram.class)));h.succeed();
    }

    @GameTest public void finiteTransferProtocolAndRollback(GameTestHelper h)throws Exception{
        long cap=AnatomyApi.mask(AnatomyApi.Capability.FINITE_POSE_PROGRAMS);check(AnatomyApi.PROTOCOL_VERSION==8&&AnatomyApi.DATA_SCHEMA_VERSION==1&&AnatomyApi.compatible(8,cap)&&!AnatomyApi.compatible(7,cap)&&!AnatomyApi.compatible(8,1L<<60),"protocol/capability mismatch");
        var tables=Map.of(TABLE.toString(),table());var models=Map.of(MODEL.toString(),geometry());var programs=numericMap(0);var binding=List.of(binding());
        byte[] bytes=AnatomyCatalogTransfer.serializedBundle(models,Map.of(),Map.of(),programs,tables,binding);
        var reverse=new TreeMap<String,NumericPoseProgram>(Comparator.reverseOrder());reverse.putAll(programs);check(Arrays.equals(bytes,AnatomyCatalogTransfer.serializedBundle(models,Map.of(),Map.of(),reverse,tables,binding)),"noncanonical bundle");
        var epoch=UUID.randomUUID();var packets=AnatomyCatalogTransfer.encode(epoch,51,models,Map.of(),Map.of(),programs,tables,binding);var receiver=new AnatomyCatalogTransfer();
        for(var packet:packets){var buf=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());try{AnatomyCatalogPayload.CODEC.encode(buf,packet);receiver.accept(AnatomyCatalogPayload.CODEC.decode(buf));}finally{buf.release();}}
        check(receiver.ready()&&receiver.revision()==51&&receiver.snapshot().finitePosePrograms().equals(tables),"finite transfer failed");
        for(int m=0;m<4;m++)check(receiver.snapshot().bindings().get(ENTITY).poses().evaluate(inputs(m)).orElseThrow().get("root").m30()==((m&1)==0?.125f:1.125f),"transferred selection changed");
        for(String removed:List.of("finite_pose_programs","numeric_pose_programs")){
            var json=JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();json.remove(removed);byte[] invalid=json.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(invalid));var accepted=receiver.snapshot();
            bad(()->receiver.accept(new AnatomyCatalogPayload(epoch,8,AnatomyApi.capabilities(),52,0,1,invalid.length,hash,invalid)));
            check(receiver.snapshot()==accepted&&receiver.revision()==51,"invalid transfer swapped");receiver.rejectPending();check(receiver.ready(),"rollback lost readiness");
        }
        for(var packet:packets)check(!receiver.accept(packet),"stale accepted");
        check(JsonParser.parseString(new String(AnatomyCatalogTransfer.serializedBundle(Map.of(),List.of()),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("finite_pose_programs").size()==0,"old overload lost empty family");
        System.out.println("S60_IMPL_TRANSFER_PASS rows=4 revision=51 fragments="+packets.size()+" bytes="+bytes.length);h.succeed();
    }
}
