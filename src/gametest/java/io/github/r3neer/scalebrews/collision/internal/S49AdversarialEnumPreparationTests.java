package io.github.r3neer.scalebrews.collision.internal;

import com.mojang.serialization.Codec;
import io.github.r3neer.scalebrews.collision.api.CollisionVariantSources;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;

public final class S49AdversarialEnumPreparationTests {
    static final AtomicInteger names=new AtomicInteger();
    enum Original implements StringRepresentable {
        FIRST { public String getSerializedName(){names.incrementAndGet();return "serial-first";} },
        SECOND { public String getSerializedName(){names.incrementAndGet();return "serial-second";} }
    }
    enum Foreign implements StringRepresentable { FIRST;public String getSerializedName(){return "serial-first";} }
    enum Duplicate implements StringRepresentable { FIRST,SECOND;public String getSerializedName(){return "duplicate";} }
    enum Empty implements StringRepresentable { ;public String getSerializedName(){return "unused";} }
    static String declared="valid";
    enum Text implements StringRepresentable { FIRST;public String getSerializedName(){return declared;} }
    static final class ReaderCow extends Cow {
        DataComponentType<?> requested;Object observed;
        ReaderCow(Level level){super(EntityTypes.COW,level);}
        @Override @SuppressWarnings("unchecked") public <T>T get(DataComponentType<? extends T> component){return requested==component?(T)observed:super.get(component);}
    }
    static void check(boolean value,String message){if(!value)throw new AssertionError("S49 ADV "+message);}
    static void rejected(Runnable operation,String message){try{operation.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("S49 ADV missing rejection: "+message);}
    static <T extends Enum<T>&StringRepresentable> DataComponentType<T> component(Class<T> type){return DataComponentType.<T>builder().persistent(Codec.STRING.xmap(s->type.getEnumConstants()[0],StringRepresentable::getSerializedName)).build();}
    static String expectedKey(Identifier id){return "component."+segment(id.getNamespace())+"."+segment(id.getPath());}
    static String segment(String input){var result=new StringBuilder();for(char c:input.toCharArray())switch(c){case '_'->result.append("_5f");case '.'->result.append("_2e");case '/'->result.append("_2f");default->result.append(c);}return result.toString();}

    @GameTest @SuppressWarnings({"unchecked","rawtypes"}) public void domainsOriginalDiscoveryAndStableReader(GameTestHelper h){
        var cow=new ReaderCow(h.getLevel());var type=component(Original.class);names.set(0);var source=CollisionVariantSources.enumComponent(type,Original.class);
        check(names.get()==2&&source.values().equals(Set.of("serial-first","serial-second")),"prepared original serialized names once");
        cow.requested=type;cow.observed=Original.FIRST;
        String prepared=source.reader().value(cow);check("serial-first".equals(prepared),"constant-specific class valid");
        for(int i=0;i<10000;i++)check(source.reader().value(cow)==prepared,"stable prepared name identity");
        var allocation=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
        if(allocation.isThreadAllocatedMemorySupported()){allocation.setThreadAllocatedMemoryEnabled(true);long t=Thread.currentThread().threadId(),before=allocation.getThreadAllocatedBytes(t);for(int i=0;i<100000;i++)check(source.reader().value(cow)==prepared,"hot component reader remains prepared");long bytes=allocation.getThreadAllocatedBytes(t)-before;check(bytes<=4096,"reader100k allocation "+bytes);System.out.println("S49 ADV ALLOCATION100000 bytes="+bytes);}
        check(names.get()==2,"runtime never invokes serializer");cow.observed=Original.SECOND;check(source.reader().value(cow).equals("serial-second"),"same owner next ordinal");
        cow.observed=Foreign.FIRST;check(source.reader().value(cow)==null,"foreign same ordinal rejected");cow.observed="serial-first";check(source.reader().value(cow)==null,"non-enum rejected");cow.observed=null;check(source.reader().value(cow)==null,"missing component remains unknown");
        rejected(()->CollisionVariantSources.enumComponent(component(Duplicate.class),Duplicate.class),"duplicate name");rejected(()->CollisionVariantSources.enumComponent(component(Empty.class),Empty.class),"empty enum");
        for(String invalid:new String[]{"\uD800","\uDC00","a".repeat(257)}){declared=invalid;rejected(()->CollisionVariantSources.enumComponent(component(Text.class),Text.class),"invalid declared text");}
        check(CollisionVariantSources.enumComponent(component(S49AdversarialEnumDomains.Domain256.class),S49AdversarialEnumDomains.Domain256.class).values().size()==256,"exact enum256 bound");rejected(()->CollisionVariantSources.enumComponent(component(S49AdversarialEnumDomains.Domain257.class),S49AdversarialEnumDomains.Domain257.class),"enum257 bound");declared="a".repeat(256);var boundary=CollisionVariantSources.enumComponent(component(Text.class),Text.class);check(boundary.values().contains(declared),"exact name text bound");declared="changed";check(!boundary.values().contains(declared),"domain captured immutable");
        var ids=new HashSet<Identifier>();var keys=new HashSet<String>();String[] words={"a","a_b","a.b","a_2fb","a_5fb","a_2eb"};for(String n:words)for(String p:words){for(String suffix:new String[]{"","/b",".b","_b"})ids.add(Identifier.parse(n+":"+p+suffix));}
        for(var id:ids){String key=CollisionVariantSources.componentKey(id);check(key.equals(expectedKey(id)),"independent source-ID encoding");check(key.matches("[a-z0-9_.-]{1,64}"),"bounded source vocabulary");check(keys.add(key),"separator/escape alias rejected");}
        var shortId=Identifier.parse("x:"+"a".repeat(52));check(CollisionVariantSources.componentKey(shortId).length()==64,"exact key64 boundary");rejected(()->CollisionVariantSources.componentKey(Identifier.parse("x:"+"a".repeat(53))),"key65 no truncation");
        BuiltInVariantSources.initialize();var registry=CollisionVariantSources.snapshot();int enums=0,holder=0;
        for(var field:DataComponents.class.getFields())if(Modifier.isStatic(field.getModifiers())&&field.getType()==DataComponentType.class&&field.getGenericType() instanceof ParameterizedType parameter){
            var generic=parameter.getActualTypeArguments()[0];var nativeType=(DataComponentType<?>)get(field);var id=BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(nativeType);
            if(generic instanceof Class<?> klass&&klass.isEnum()&&StringRepresentable.class.isAssignableFrom(klass)){
                enums++;var actual=registry.get(expectedKey(id));check(actual!=null,"original enum source admitted "+id);var domain=new TreeSet<String>();for(Object value:klass.getEnumConstants())domain.add(((StringRepresentable)value).getSerializedName());check(actual.values().equals(domain),"complete original domain "+id);
            }else if(generic instanceof ParameterizedType nested&&nested.getRawType()==net.minecraft.core.Holder.class){holder++;check(!registry.containsKey(expectedKey(id)),"Holder not reinterpreted as enum");}
        }
        check(enums>=4&&holder>0,"real family population positive");BuiltInVariantSources.initialize();var repeated=CollisionVariantSources.snapshot();registry.forEach((key,value)->check(repeated.get(key)==value,"initialize preserves source/reader identity "+key));
        var fox=EntityTypes.FOX.create(h.getLevel(),EntitySpawnReason.COMMAND);var salmon=EntityTypes.SALMON.create(h.getLevel(),EntitySpawnReason.COMMAND);check(fox!=null&&salmon!=null,"real factories");
        nativeValues(fox,DataComponents.FOX_VARIANT,net.minecraft.world.entity.animal.fox.Fox.Variant.class,registry);nativeValues(salmon,DataComponents.SALMON_SIZE,net.minecraft.world.entity.animal.fish.Salmon.Variant.class,registry);
        check(registry.get(expectedKey(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(DataComponents.FOX_VARIANT))).reader().value(cow)==null,"third real type absent");cow.discard();fox.discard();salmon.discard();
        System.out.println("S49 ADV PURE PASS originalEnums="+enums+" holderExcluded="+holder+" keys="+keys.size());h.succeed();
    }
        @GameTest @SuppressWarnings("unchecked") public void initializationRejectsForeignOwnership(GameTestHelper h){
        BuiltInVariantSources.initialize();String key=expectedKey(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(DataComponents.RARITY));var old=CollisionVariantSources.snapshot().get(key);check(old!=null,"real ownership source present");
        Map<String,CollisionVariantSources.Source> registry;
        try{var field=CollisionVariantSources.class.getDeclaredField("SOURCES");field.setAccessible(true);registry=(Map<String,CollisionVariantSources.Source>)field.get(null);}catch(ReflectiveOperationException e){throw new AssertionError(e);}
        var foreign=new CollisionVariantSources.Source(old.values(),e->old.values().iterator().next());synchronized(CollisionVariantSources.class){registry.put(key,foreign);}
        boolean rejected=false;try{try{BuiltInVariantSources.initialize();}catch(IllegalStateException expected){rejected=true;}check(rejected,"initialize rejects another reader owner even with identical domain");check(CollisionVariantSources.snapshot().get(key)==foreign,"initialize never overwrites foreign ownership");}finally{synchronized(CollisionVariantSources.class){registry.put(key,old);}}
        BuiltInVariantSources.initialize();check(CollisionVariantSources.snapshot().get(key)==old,"ownership control restored");System.out.println("S49 ADV OWNERSHIP PASS rejected-same-domain restored");h.succeed();
    }
    static Object get(java.lang.reflect.Field field){try{return field.get(null);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    static <T extends Enum<T>&StringRepresentable> void nativeValues(net.minecraft.world.entity.LivingEntity entity,DataComponentType<T> type,Class<T> klass,Map<String,CollisionVariantSources.Source> registry){var source=registry.get(expectedKey(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type)));for(T value:klass.getEnumConstants()){entity.setComponent(type,value);check(entity.get(type)==value,"native component mutation accepted");check(source.reader().value(entity).equals(value.getSerializedName()),"native value observes original serialization");}}
}
