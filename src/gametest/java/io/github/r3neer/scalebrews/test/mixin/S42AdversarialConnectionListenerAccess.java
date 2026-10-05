package io.github.r3neer.scalebrews.test.mixin;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test-only identity access; Fabric declares listener on the common addon superclass. */
@Mixin(targets="net.fabricmc.fabric.impl.networking.client.ClientCommonNetworkAddon",remap=false)
public interface S42AdversarialConnectionListenerAccess {
 @Accessor(value="listener",remap=false)
 ClientCommonPacketListenerImpl scaleBrewsTest$listener();
}
