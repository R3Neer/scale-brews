package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.Objects;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;

/** One explicit gravity authority, installed by compat; the core never writes gravity or camera state. */
public final class GravityFrames {
    private GravityFrames() {}
    private record Adapter(String owner,Function<Entity,GravityFrame> resolver) {}
    private static volatile Adapter adapter;
    public static synchronized void install(String owner,Function<Entity,GravityFrame> resolver) {
        if(owner==null || owner.isBlank())throw new IllegalArgumentException("Missing gravity adapter owner");
        Objects.requireNonNull(resolver);
        if(adapter!=null)throw new IllegalStateException("Gravity adapter already installed by "+adapter.owner());
        adapter=new Adapter(owner,resolver);
    }
    public static GravityFrame get(Entity entity) {
        var current=adapter;
        return current==null?GravityFrame.VANILLA:Objects.requireNonNull(current.resolver().apply(entity),"Gravity adapter returned no frame");
    }
}
