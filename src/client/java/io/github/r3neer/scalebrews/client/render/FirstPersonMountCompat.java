package io.github.r3neer.scalebrews.client.render;

import io.github.r3neer.scalebrews.ScaleBrews;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional FirstPerson 2.7 API bridge, sampled during extraction, never during submit. */
public final class FirstPersonMountCompat {
    private static Method rendering, logic, offset;
    private static Field instance;
    private static boolean initialized;
    private FirstPersonMountCompat() {}

    /** Null means ordinary world/GUI rendering; ZERO is a valid first-person offset. */
    public static Vec3 extractedOffset() {
        if (!initialized) {
            initialized = true;
            if (!FabricLoader.getInstance().isModLoaded("firstperson")) return null;
            try {
                rendering = Class.forName("dev.tr7zw.firstperson.api.FirstPersonAPI").getMethod("isRenderingPlayer");
                var core = Class.forName("dev.tr7zw.firstperson.FirstPersonModelCore");
                instance = core.getField("instance");
                logic = core.getMethod("getLogicHandler");
                offset = Class.forName("dev.tr7zw.firstperson.LogicHandler").getMethod("getOffset");
            } catch (ReflectiveOperationException | LinkageError error) { disable(error); }
        }
        if (rendering == null) return null;
        try {
            if (!Boolean.TRUE.equals(rendering.invoke(null))) return null;
            Object core = instance.get(null);
            if (core == null) return null;
            return (Vec3) offset.invoke(logic.invoke(core));
        } catch (ReflectiveOperationException | LinkageError | ClassCastException error) { disable(error); return null; }
    }

    private static void disable(Throwable error) {
        rendering = null;
        ScaleBrews.LOGGER.warn("FirstPerson mount alignment unavailable for this API version", error);
    }
}
