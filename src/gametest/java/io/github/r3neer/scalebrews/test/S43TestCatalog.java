package io.github.r3neer.scalebrews.test;

import com.mojang.serialization.JsonOps;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.AnatomyCodecs;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/** Fixture resources through the real transactional reload; no live binding/provider substitution. */
final class S43TestCatalog {
    private S43TestCatalog() {}

    static void reload(MinecraftServer server, Map<String,ModelGeometry> models,
                       Map<String,PlatformDefinition> profiles) {
        var installed=server.getResourceManager();
        var pack=installed.listPacks().findFirst().orElseThrow();
        var geometry=new TreeMap<Identifier,Resource>();
        for(var entry:new TreeMap<>(models).entrySet()) {
            var id=Identifier.parse(entry.getKey());
            var name=Identifier.fromNamespaceAndPath(id.getNamespace(),"scalebrews/entity_geometry/"+id.getPath()+".json");
            byte[] data=AnatomyCodecs.GEOMETRY.encodeStart(JsonOps.INSTANCE,entry.getValue()).getOrThrow()
                .toString().getBytes(StandardCharsets.UTF_8);
            geometry.put(name,new Resource(pack,()->new java.io.ByteArrayInputStream(data)));
        }
        var definitions=new TreeMap<Identifier,Resource>();
        int index=0;
        for(var profile:new TreeMap<>(profiles).values()) {
            byte[] data=PlatformDefinition.CODEC.encodeStart(JsonOps.INSTANCE,profile).getOrThrow()
                .toString().getBytes(StandardCharsets.UTF_8);
            definitions.put(Identifier.parse("scalebrews_test:scalebrews/entity_platform/s43_"+(index++)+".json"),
                new Resource(pack,()->new java.io.ByteArrayInputStream(data)));
        }
        var resources=(ResourceManager)java.lang.reflect.Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),
            new Class<?>[]{ResourceManager.class},(proxy,method,args)->{
                if(method.getName().equals("listPacks"))return installed.listPacks();
                if(method.getName().equals("listResources"))return switch((String)args[0]) {
                    case "scalebrews/entity_geometry"->geometry;
                    case "scalebrews/entity_platform"->definitions;
                    default->Map.of();
                };
                throw new UnsupportedOperationException(method.toString());
            });
        try {
            var reload=AnatomyRuntime.class.getDeclaredMethod("reload",MinecraftServer.class,ResourceManager.class);
            reload.setAccessible(true);reload.invoke(null,server,resources);
        } catch(java.lang.reflect.InvocationTargetException failure) {
            if(failure.getCause() instanceof RuntimeException cause)throw cause;
            if(failure.getCause() instanceof Error cause)throw cause;
            throw new AssertionError(failure.getCause());
        } catch(ReflectiveOperationException failure) {throw new AssertionError(failure);}
    }
}
