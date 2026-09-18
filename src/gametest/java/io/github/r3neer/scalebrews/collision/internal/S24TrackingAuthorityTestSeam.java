package io.github.r3neer.scalebrews.collision.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * GameTest-only seam for receipt/kernel fixtures that need an already-authorized tracking window
 * without exercising networking lifecycle. Production acquisition remains private to AnatomyRuntime.
 */
public final class S24TrackingAuthorityTestSeam implements AutoCloseable {
    private final MinecraftServer server;
    private final boolean installedState;

    private S24TrackingAuthorityTestSeam(MinecraftServer server, boolean installedState) {
        this.server=server;
        this.installedState=installedState;
    }

    @SuppressWarnings("unchecked")
    public static S24TrackingAuthorityTestSeam acquire(ServerPlayer recipient, Entity body) {
        if(recipient==null || body==null)throw new IllegalArgumentException("Missing tracking fixture participant");
        try {
            var server=recipient.level().getServer();
            if(server==null)throw new IllegalStateException("Tracking fixture requires a server");

            Field statesField=AnatomyRuntime.class.getDeclaredField("STATES");
            statesField.setAccessible(true);
            var states=(Map<MinecraftServer,Object>)statesField.get(null);
            Object state=states.get(server);
            boolean installed=false;

            if(state==null) {
                var stateClass=Arrays.stream(AnatomyRuntime.class.getDeclaredClasses())
                    .filter(type->type.getSimpleName().equals("State"))
                    .findFirst().orElseThrow();
                Constructor<?> ctor=stateClass.getDeclaredConstructor(MinecraftServer.class);
                ctor.setAccessible(true);
                state=ctor.newInstance(server);
                states.put(server,state);
                installed=true;
            }

            Method acquire=Arrays.stream(AnatomyRuntime.class.getDeclaredMethods())
                .filter(method->(method.getName().equals("generation") || method.getName().equals("acquireGeneration"))
                    && method.getParameterCount()==3)
                .findFirst().orElseThrow();
            acquire.setAccessible(true);
            long generation=((Long)acquire.invoke(null,state,recipient,body.getUUID())).longValue();
            if(generation<1)throw new AssertionError("GameTest fixture could not acquire tracking authority");

            long observed=AnatomyRuntime.trackingGeneration(recipient,body);
            if(observed!=generation)
                throw new AssertionError("Tracking fixture acquisition is not observable: acquired="+generation+" observed="+observed);
            return new S24TrackingAuthorityTestSeam(server,installed);
        } catch(ReflectiveOperationException failure) {
            throw new AssertionError("Could not install explicit GameTest tracking authority",failure);
        }
    }

    @Override public void close() {
        if(installedState)AnatomyRuntime.stop(server);
    }
}
