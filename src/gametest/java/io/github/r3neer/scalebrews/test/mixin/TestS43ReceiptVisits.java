package io.github.r3neer.scalebrews.test.mixin;
import io.github.r3neer.scalebrews.collision.internal.S43RetirementVisits;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import java.util.*;
@Mixin(targets="io.github.r3neer.scalebrews.collision.internal.AnatomyTransportReceipts",remap=false)
public abstract class TestS43ReceiptVisits {
 @WrapOperation(method="invalidate",at=@At(value="INVOKE",target="Ljava/util/Map;entrySet()Ljava/util/Set;"),require=0)
 private static Set<?> entries(Map<?,?> map,Operation<Set<?>> original){var values=original.call(map);if(!S43RetirementVisits.active)return values;
  return new AbstractSet<Object>(){public int size(){return values.size();}public Iterator<Object> iterator(){var it=values.iterator();return new Iterator<>(){public boolean hasNext(){return it.hasNext();}public Object next(){S43RetirementVisits.foreign.incrementAndGet();return it.next();}public void remove(){it.remove();}};}};
 }
}
