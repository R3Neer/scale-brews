package io.github.r3neer.scalebrews.collision.internal;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Proxy;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import io.github.r3neer.scalebrews.collision.api.*;
import io.github.r3neer.scalebrews.collision.api.spi.*;
import io.github.r3neer.scalebrews.collision.data.*;
import io.github.r3neer.scalebrews.collision.geometry.*;
import static io.github.r3neer.scalebrews.collision.internal.S48AdversarialVariantFixtures.*;
public final class S48AdversarialVariantPreparationTests {
 @GameTest public void preparedSourceBoundsAndStableReads(GameTestHelper h){
 registry();var level=h.getLevel();var server=level.getServer();var world=new WorldAnatomyCatalog();var defaultRow=row(Map.of());var specific=row(Map.of("adv_signal","y"));var snap=world.replace(Map.of(MODEL.toString(),model()),List.of(defaultRow,specific));var cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);check(cow!=null,"original cow factory");cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2,3,2)));
  try {signal.set("x");var cursor=new PreparedVariantBindings(snap).cursor(TYPE);var expected=snap.resolve(TYPE,Map.of("adv_signal","x")).orElseThrow();check(cursor.read(cow)==expected,"known input default resolver");reads.set(0);long resolutions=resolveCount();if(Boolean.getBoolean("s48.requireCounter"))check(resolutions>0,"preparation counter loaded");for(int i=0;i<10000;i++)check(cursor.read(cow)==expected,"unchanged owned row");check(reads.get()==10000,"exactly one local source read per cursor read");check(resolveCount()==resolutions,"stable inputs never repeat canonical resolution");var allocationBean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();if(allocationBean.isThreadAllocatedMemorySupported()){allocationBean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().threadId();long beforeBytes=allocationBean.getThreadAllocatedBytes(thread);for(int i=0;i<100000;i++)check(cursor.read(cow)==expected,"stable allocation input");long bytes=allocationBean.getThreadAllocatedBytes(thread)-beforeBytes;check(bytes<=4096,"stable cursor100k allocation bound: "+bytes);System.out.println("S48 ADV STABLE_ALLOCATION iterations100000 bytes="+bytes);}signal.set(null);check(cursor.read(cow)==null,"unknown cannot default");check(cursor.selected()==null,"unknown also clears policy observation");signal.set("invalid");check(cursor.read(cow)==null,"out of domain cannot default");check(cursor.selected()==null,"domain failure also clears policy observation");signal.set("y");check(cursor.read(cow)==snap.resolve(TYPE,Map.of("adv_signal","y")).orElseThrow(),"source recovers specific row");
  var missingWorld=new WorldAnatomyCatalog();var missingSnapshot=missingWorld.replace(Map.of(MODEL.toString(),model()),List.of(defaultRow,row(Map.of("adv_late","x"))));var missingCursor=new PreparedVariantBindings(missingSnapshot).cursor(TYPE);check(missingCursor.read(cow)==null&&missingCursor.status()==PreparedVariantBindings.Status.PLAN_UNAVAILABLE,"unknown axis cannot expose default");CollisionVariantSources.register("adv_late",Set.of("x"),e->"x");check(missingCursor.read(cow)==null,"late registry cannot mutate captured plan");check(new PreparedVariantBindings(missingSnapshot).cursor(TYPE).read(cow)!=null,"new explicit preparation sees reader");var axes=new TreeMap<String,String>();var axisReads=new AtomicInteger();for(int axis=0;axis<33;axis++){String key="adv_axis"+axis;CollisionVariantSources.register(key,Set.of("x"),e->{axisReads.incrementAndGet();return "x";});if(axis<32)axes.put(key,"x");}var cappedWorld=new WorldAnatomyCatalog();var cappedSnapshot=cappedWorld.replace(Map.of(MODEL.toString(),model()),List.of(row(axes)));check(new PreparedVariantBindings(cappedSnapshot).cursor(TYPE).read(cow)!=null&&axisReads.get()==32,"exact32 axes executable/local");var exceededWorld=new WorldAnatomyCatalog();var exceededSnapshot=exceededWorld.replace(Map.of(MODEL.toString(),model()),List.of(row(axes),row(Map.of("adv_axis32","x"))));axisReads.set(0);var exceededCursor=new PreparedVariantBindings(exceededSnapshot).cursor(TYPE);check(exceededCursor.read(cow)==null&&exceededCursor.status()==PreparedVariantBindings.Status.PLAN_UNAVAILABLE&&axisReads.get()==0,"union33 rejected before runtime reads");System.out.println("S48 ADV PREPARATION PASS stable100k axes32/33 captured-registry unavailable-policy");h.succeed();} finally {cow.discard();}
 }
}
