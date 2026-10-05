package io.github.r3neer.scalebrews.test.mixin;
import io.github.r3neer.scalebrews.test.S42AdversarialRetiredDisconnectProof;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(targets="net.fabricmc.fabric.impl.networking.client.ClientPlayNetworkAddon",remap=false)
public abstract class S42AdversarialRetiredDisconnectGate {
 @WrapMethod(method="invokeDisconnectEvent",remap=false)
 private void gate(Operation<Void> original) {
  boolean held=((S42AdversarialConnectionListenerAccess)(Object)this).scaleBrewsTest$listener()==S42AdversarialRetiredDisconnectProof.expectedHandler && Thread.currentThread().getName().startsWith("Netty") && S42AdversarialRetiredDisconnectProof.hold.compareAndSet(true,false);
  if(held) {
   System.out.println("S42_HELD_REAL_DISCONNECT thread="+Thread.currentThread().getName());
   S42AdversarialRetiredDisconnectProof.captured.countDown();
   try {if(!S42AdversarialRetiredDisconnectProof.release.await(45,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("Fixture disconnect gate timed out");}
   catch(InterruptedException e) {Thread.currentThread().interrupt();throw new AssertionError(e);}
  }
  try {original.call();}finally {if(held)S42AdversarialRetiredDisconnectProof.finished.countDown();}
 }
}
