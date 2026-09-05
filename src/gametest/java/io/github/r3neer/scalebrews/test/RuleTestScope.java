package io.github.r3neer.scalebrews.test;
import io.github.r3neer.scalebrews.config.ScaleRules;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
/** Test-only server-thread scope: exercise real hooks under decoded rules without mutating a frozen registry. */
public final class RuleTestScope implements AutoCloseable {
    public static final ThreadLocal<ScaleRules> CURRENT = new ThreadLocal<>();
    private final ScaleRules previous = CURRENT.get();
    public RuleTestScope(String json) { CURRENT.set(ScaleRules.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow()); }
    public void close() { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
}
