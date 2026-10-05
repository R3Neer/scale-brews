package io.github.r3neer.scalebrews.collision.internal;

import com.mojang.serialization.Codec;
import io.github.r3neer.scalebrews.collision.api.CollisionVariantSources;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.variant.ModelAndTexture;
import net.minecraft.world.level.Level;

public final class S50AdversarialHolderPreparationTests {
    static final AtomicInteger serialized=new AtomicInteger(),accessed=new AtomicInteger();
    public enum Own implements StringRepresentable { A {public String getSerializedName(){serialized.incrementAndGet();return "declared-a";}}, B {public String getSerializedName(){serialized.incrementAndGet();return "declared-b";}} }
    public enum Alien implements StringRepresentable { A;public String getSerializedName(){return "declared-a";} }
    public record Good(ModelAndTexture<Own> selected,boolean fail) {
        @Override public ModelAndTexture<Own> selected(){accessed.incrementAndGet();if(fail)throw new IllegalStateException("fixture-accessor");return selected;}
    }
    public record Other(ModelAndTexture<Own> selected) {}
    public record Missing(String asset) {}
    public record Ambiguous(ModelAndTexture<Own> one,ModelAndTexture<Own> two) {}
    @SuppressWarnings("rawtypes") public record Raw(ModelAndTexture selected) {}
    public record NotEnum(ModelAndTexture<String> selected) {}
    public record Generic<T>(ModelAndTexture<T> selected) {}
    private record Private(ModelAndTexture<Own> selected) {}
    static final class Reader extends Cow {
        DataComponentType<?> requested;Object observed;int reads;
        Reader(Level level){super(EntityTypes.COW,level);}
        @Override @SuppressWarnings("unchecked") public <T>T get(DataComponentType<? extends T> type){if(type==requested){reads++;return(T)observed;}return super.get(type);}
    }
    static void check(boolean v,String m){if(!v)throw new AssertionError("S50 ADV "+m);}
    static void rejected(Runnable r,String m){try{r.run();}catch(IllegalArgumentException e){return;}throw new AssertionError("S50 ADV missing schema rejection "+m);}
    static <V>DataComponentType<Holder<V>> component(){return DataComponentType.<Holder<V>>builder().persistent(Codec.STRING.xmap(s->(Holder<V>)null,h->"fixture")).build();}
    static Identifier asset(String name){return Identifier.parse("s50_adversary:"+name);}
    @GameTest @SuppressWarnings({"rawtypes","unchecked"}) public void schemaTypingAndBoundedReader(GameTestHelper h){
        var component=S50AdversarialHolderPreparationTests.<Good>component();serialized.set(0);accessed.set(0);var source=CollisionVariantSources.holderModelComponent(component,Good.class);check(source.values().equals(Set.of("declared-a","declared-b"))&&serialized.get()==2,"prepared serialized names once");
        var body=new Reader(h.getLevel());body.requested=component;body.observed=Holder.direct(new Good(new ModelAndTexture<>(Own.A,asset("first")),false));
        String prepared=source.reader().value(body);check("declared-a".equals(prepared),"constant-specific enum owner and selected accessor");
        for(int i=0;i<100000;i++)check(source.reader().value(body)==prepared,"stable reader name");
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);long tid=Thread.currentThread().threadId(),before=bean.getThreadAllocatedBytes(tid);int readBefore=body.reads,accessBefore=accessed.get();for(int i=0;i<100000;i++)check(source.reader().value(body)==prepared,"prepared holder read");long bytes=bean.getThreadAllocatedBytes(tid)-before;check(bytes<=4096,"reader allocation100k="+bytes);check(body.reads-readBefore==100000&&accessed.get()-accessBefore==100000,"exactly one local component and accessor per read");check(serialized.get()==2,"no runtime enum serialization");System.out.println("S50 ADV HOLDER ALLOC100k="+bytes);
        body.observed=Holder.direct(new Good(new ModelAndTexture<>(Own.A,asset("cosmetic-change")),false));check(source.reader().value(body)==prepared,"same model different texture remains same prepared name");body.observed=Holder.direct(new Good(new ModelAndTexture<>(Own.B,asset("first")),false));check("declared-b".equals(source.reader().value(body)),"next model changes selected name");
        body.observed=Holder.direct(new Other(new ModelAndTexture<>(Own.A,asset("first"))));check(source.reader().value(body)==null,"another record owner rejected");body.observed=Holder.direct(new Good(new ModelAndTexture(Alien.A,asset("first")),false));check(source.reader().value(body)==null,"foreign enum same ordinal rejected");
        body.observed=Holder.direct(new Good(new ModelAndTexture(null,asset("first")),false));check(source.reader().value(body)==null,"null model unknown");body.observed=Holder.direct(new Good(null,false));check(source.reader().value(body)==null,"null projection unknown");body.observed=null;check(source.reader().value(body)==null,"missing component unknown");body.observed="not-holder";check(source.reader().value(body)==null,"wrong component unknown");
        body.observed=Holder.Reference.createStandAlone(new net.minecraft.core.HolderOwner<Good>(){},net.minecraft.resources.ResourceKey.create(net.minecraft.resources.ResourceKey.createRegistryKey(asset("registry")),asset("unbound")));check(source.reader().value(body)==null,"unbound holder checked before value");
        body.observed=Holder.direct(new Good(new ModelAndTexture<>(Own.A,asset("first")),true));boolean throwsOriginal=false;try{source.reader().value(body);}catch(IllegalStateException e){throwsOriginal=e.getMessage().equals("fixture-accessor");}check(throwsOriginal,"accessor exception delivered to fail-closed cursor");
        for(Class bad:new Class[]{Missing.class,Ambiguous.class,Raw.class,NotEnum.class,Generic.class,Private.class,String.class})rejected(()->CollisionVariantSources.holderModelComponent((DataComponentType)component,bad),bad.getName());
        for(String id:new String[]{"a:b","a:b.model","a:b_2emodel","a_b:c/d","a:b/model"}){var identifier=Identifier.parse(id);String expected=S49AdversarialEnumPreparationTests.expectedKey(identifier)+".model";check(CollisionVariantSources.modelComponentKey(identifier).equals(expected),"projection key independent encoding");}
        check(CollisionVariantSources.modelComponentKey(Identifier.parse("x:"+"a".repeat(46))).length()==64,"model key64 admitted");rejected(()->CollisionVariantSources.modelComponentKey(Identifier.parse("x:"+"a".repeat(47))),"model key65 rejected");
        body.discard();System.out.println("S50 ADV SCHEMA PASS typing/schema/unknown/key/accessor/locality");h.succeed();
    }
    @GameTest public void accessorFailureRetiresCursorAndRecovers(GameTestHelper h){
        var component=S50AdversarialHolderPreparationTests.<Good>component();var source=CollisionVariantSources.holderModelComponent(component,Good.class);String key="s50.adversary.throwing.model";
        CollisionVariantSources.register(key,source.values(),source.reader());
        var row=new io.github.r3neer.scalebrews.collision.data.CollisionBinding(1,Identifier.parse("minecraft:cow"),Map.of(key,"declared-a"),new io.github.r3neer.scalebrews.collision.data.CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART,S48AdversarialVariantFixtures.MODEL,Map.of(),io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter.DEFAULT),new io.github.r3neer.scalebrews.collision.data.CollisionBinding.Pose(S48AdversarialVariantFixtures.POSE,Map.of(),Set.of()),S48AdversarialVariantFixtures.ROOT,io.github.r3neer.scalebrews.collision.data.CollisionPolicy.Patch.EMPTY,Set.of());
        var catalog=new WorldAnatomyCatalog();var snapshot=catalog.reload(S48AdversarialVariantFixtures.resources(h.getLevel().getServer().getResourceManager(),List.of(row)));var cursor=new PreparedVariantBindings(snapshot).cursor(Identifier.parse("minecraft:cow"));
        var body=new Reader(h.getLevel());body.requested=component;body.observed=Holder.direct(new Good(new ModelAndTexture<>(Own.A,asset("first")),false));check(cursor.read(body)!=null&&cursor.status()==PreparedVariantBindings.Status.AVAILABLE,"cursor positive row before failure");
        body.observed=Holder.direct(new Good(new ModelAndTexture<>(Own.A,asset("first")),true));check(cursor.read(body)==null&&cursor.selected()==null&&cursor.status()==PreparedVariantBindings.Status.SOURCE_UNAVAILABLE,"accessor RuntimeException closes cursor without stale selected row");
        body.observed=Holder.direct(new Good(null,false));check(cursor.read(body)==null&&cursor.selected()==null,"unknown does not take default or old row");body.observed=Holder.direct(new Good(new ModelAndTexture<>(Own.A,asset("next-texture")),false));check(cursor.read(body)!=null&&cursor.status()==PreparedVariantBindings.Status.AVAILABLE,"cursor recovers exact known projection");body.discard();System.out.println("S50 ADV CURSOR PASS throw/unknown/recovery");h.succeed();
    }
    static Object get(Field f){try{return f.get(null);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    @GameTest @SuppressWarnings({"rawtypes","unchecked"})public void originalPopulationAndNativeProjection(GameTestHelper h){
        BuiltInVariantSources.initialize();var all=CollisionVariantSources.snapshot();int projected=0,remaining=0;
        for(var field:DataComponents.class.getFields())if(Modifier.isStatic(field.getModifiers())&&field.getType()==DataComponentType.class&&field.getGenericType()instanceof ParameterizedType p&&p.getActualTypeArguments()[0]instanceof ParameterizedType nested&&nested.getRawType()==Holder.class){
            var component=(DataComponentType)get(field);var id=BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component);String key=S49AdversarialEnumPreparationTests.expectedKey(id)+".model";var argument=nested.getActualTypeArguments()[0];Class<?> type=argument instanceof Class<?> c?c:null;Class<?> model=null;int found=0;
            if(type!=null&&type.isRecord())for(var rc:type.getRecordComponents())if(rc.getType()==ModelAndTexture.class){found++;if(rc.getGenericType()instanceof ParameterizedType generic&&generic.getActualTypeArguments()[0]instanceof Class<?> m)model=m;}
            if(found==1&&model!=null&&model.isEnum()&&StringRepresentable.class.isAssignableFrom(model)){projected++;var source=all.get(key);check(source!=null,"original model source admitted "+id);var values=new TreeSet<String>();for(Object value:model.getEnumConstants())values.add(((StringRepresentable)value).getSerializedName());check(source.values().equals(values),"original complete enum domain "+id);}else{remaining++;check(!all.containsKey(key),"unsupported Holder stays absent "+id);}
        }
        check(projected==4&&remaining==13,"original Holder population projected="+projected+" remaining="+remaining);
        nativeAll(h,EntityTypes.COW,DataComponents.COW_VARIANT);nativeAll(h,EntityTypes.PIG,DataComponents.PIG_VARIANT);nativeAll(h,EntityTypes.CHICKEN,DataComponents.CHICKEN_VARIANT);nativeAll(h,EntityTypes.ZOMBIE_NAUTILUS,DataComponents.ZOMBIE_NAUTILUS_VARIANT);
        BuiltInVariantSources.initialize();var next=CollisionVariantSources.snapshot();all.forEach((k,v)->check(next.get(k)==v,"stable source ownership "+k));System.out.println("S50 ADV NATIVE PASS projected="+projected+" unsupported="+remaining+" direct/reference/original-enums");h.succeed();
    }
    @SuppressWarnings({"rawtypes","unchecked"})static void nativeAll(GameTestHelper h,EntityType<?> factory,DataComponentType component){
        var entity=(LivingEntity)factory.create(h.getLevel(),EntitySpawnReason.COMMAND);check(entity!=null,"native factory");var original=(Holder)entity.get(component);check(original!=null&&original.isBound()&&original.kind()==Holder.Kind.REFERENCE,"original bound registry reference");Object record=original.value();var source=CollisionVariantSources.snapshot().get(S49AdversarialEnumPreparationTests.expectedKey(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component))+".model");
        ModelAndTexture<?> mt=null;try{for(var rc:record.getClass().getRecordComponents())if(rc.getType()==ModelAndTexture.class)mt=(ModelAndTexture<?>)rc.getAccessor().invoke(record);}catch(ReflectiveOperationException e){throw new AssertionError(e);}check(mt!=null,"original record projection");check(source.reader().value(entity).equals(((StringRepresentable)mt.model()).getSerializedName()),"reference value reflects original");
        for(Object model:((Enum<?>)mt.model()).getDeclaringClass().getEnumConstants())for(String texture:new String[]{"one","two"}){entity.setComponent(component,Holder.direct(cloneModel(record,model,asset(texture))));check(source.reader().value(entity).equals(((StringRepresentable)model).getSerializedName()),"native direct holder model/texture projection");}entity.discard();
    }
    @SuppressWarnings({"rawtypes","unchecked"})static Object cloneModel(Object record,Object model,Identifier asset){try{var fields=record.getClass().getRecordComponents();Object[] values=new Object[fields.length];Class<?>[] types=new Class[fields.length];for(int i=0;i<fields.length;i++){types[i]=fields[i].getType();values[i]=types[i]==ModelAndTexture.class?new ModelAndTexture(model,asset):fields[i].getAccessor().invoke(record);}return record.getClass().getConstructor(types).newInstance(values);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
