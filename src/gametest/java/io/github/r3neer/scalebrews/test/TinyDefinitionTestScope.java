package io.github.r3neer.scalebrews.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.r3neer.scalebrews.mount.TinyMountDefinition;

/** Overrides one decoded definition only on the synchronous test thread. */
public final class TinyDefinitionTestScope implements AutoCloseable {
    public static final ThreadLocal<TinyMountDefinition> CURRENT = new ThreadLocal<>();
    private final TinyMountDefinition previous = CURRENT.get();
    public TinyDefinitionTestScope(String json) {
        CURRENT.set(TinyMountDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow());
    }
    @Override public void close() {
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
    }
}
