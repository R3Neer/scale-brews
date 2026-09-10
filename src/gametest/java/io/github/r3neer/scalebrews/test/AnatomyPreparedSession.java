package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

/**
 * Explicit GameTest-only runtime scope. It proves the catalog/session pipeline
 * with real exported definitions and never makes a manually registered core
 * fixture look like a prepared production session.
 */
public final class AnatomyPreparedSession implements AutoCloseable {
    private final MinecraftServer server;
    private boolean closed;
    private AnatomyPreparedSession(MinecraftServer server){this.server=server;}

    public static AnatomyPreparedSession start(MinecraftServer server,Map<String,ModelGeometry> models,Map<String,PlatformDefinition> profiles) {
        if(server==null || models==null || profiles==null || models.isEmpty() || profiles.isEmpty())
            throw new IllegalArgumentException("Prepared anatomy GameTest requires non-empty exported models and profiles");
        for(var profile:profiles.values())profile.anatomy().ifPresent(definition->{
            if(!models.containsKey(definition.model().toString()))
                throw new IllegalArgumentException("Prepared profile references missing exported model: "+definition.model());
        });
        AnatomyRuntime.startPrepared(server,Map.copyOf(models),Map.copyOf(profiles));
        return new AnatomyPreparedSession(server);
    }
    @Override public void close() {
        if(!closed){closed=true;AnatomyRuntime.stop(server);}
    }
}
