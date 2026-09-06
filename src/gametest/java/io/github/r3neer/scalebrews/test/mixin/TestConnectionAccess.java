package io.github.r3neer.scalebrews.test.mixin;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Connection.class)
public interface TestConnectionAccess {
    @Accessor("channel") Channel test$channel();
}
