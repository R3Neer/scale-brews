package io.github.r3neer.scalebrews.client.render;

import io.github.r3neer.scalebrews.ScaleBrews;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.phys.Vec3;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/** Optional FirstPerson 2.7 API bridge, sampled during extraction, never during submit. */
public final class FirstPersonMountCompat {
    private record Access(MethodHandle rendering, MethodHandle instance, MethodHandle logic, MethodHandle offset) {}
    private static Access access;
    private static boolean initialized;
    private FirstPersonMountCompat() {}

    /** Prepare the optional API once; external class initialization stays at its normal first use. */
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        if (!FabricLoader.getInstance().isModLoaded("firstperson")) return;
        try {
            var loader = FirstPersonMountCompat.class.getClassLoader();
            var api = Class.forName("dev.tr7zw.firstperson.api.FirstPersonAPI", false, loader);
            var core = Class.forName("dev.tr7zw.firstperson.FirstPersonModelCore", false, loader);
            var handler = Class.forName("dev.tr7zw.firstperson.LogicHandler", false, loader);
            var lookup = MethodHandles.publicLookup();
            access = new Access(
                lookup.unreflect(api.getMethod("isRenderingPlayer")).asType(MethodType.methodType(boolean.class)),
                lookup.unreflectGetter(core.getField("instance")).asType(MethodType.methodType(Object.class)),
                lookup.unreflect(core.getMethod("getLogicHandler")).asType(MethodType.methodType(Object.class, Object.class)),
                lookup.unreflect(handler.getMethod("getOffset")).asType(MethodType.methodType(Vec3.class, Object.class)));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) { disable(error); }
    }

    /** Null means ordinary world/GUI rendering; ZERO is a valid first-person offset. */
    public static Vec3 extractedOffset() {
        Access current = access;
        if (current == null) return null;
        try {
            if (!(boolean)current.rendering().invokeExact()) return null;
            Object core = (Object)current.instance().invokeExact();
            if (core == null) return null;
            Object handler = (Object)current.logic().invokeExact(core);
            return (Vec3)current.offset().invokeExact(handler);
        } catch (Throwable error) { disable(error); return null; }
    }

    private static void disable(Throwable error) {
        access = null;
        ScaleBrews.LOGGER.warn("FirstPerson mount alignment unavailable for this API version", error);
    }
}
