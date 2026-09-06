package io.github.r3neer.scalebrews.test;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;

/** Validate against loaded datapack registries, not a vanilla-only datagen snapshot before mod initialization. */
public final class TestWorldgenValidation {
    private static final ThreadLocal<HolderLookup.Provider> CONTEXT = new ThreadLocal<>();
    private static boolean scheduled;
    private static volatile int completed;
    private TestWorldgenValidation() {}
    public static HolderLookup.Provider context() { return CONTEXT.get(); }
    public static boolean defer() {
        if(!FabricLoader.getInstance().isModLoaded("wilderwild") || context()!=null) return false;
        if(!scheduled) {
            scheduled=true;
            ServerLifecycleEvents.SERVER_STARTED.register(server->{
                CONTEXT.set(server.registryAccess());
                try {
                    Commands.validate();
                    completed++;
                    io.github.r3neer.scalebrews.ScaleBrews.LOGGER.info("Completed deferred command validation against loaded VP26 registries");
                } finally { CONTEXT.remove(); }
            });
        }
        return true;
    }
    public static void assertCompleted() {
        if(FabricLoader.getInstance().isModLoaded("wilderwild") && completed==0)
            throw new AssertionError("Deferred worldgen/command validation did not run");
    }
}
