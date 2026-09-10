package io.github.r3neer.scalebrews.collision.pose;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.util.Mth;

/**
 * Species-pose channel semantics shared by history and server-safe family
 * formulas. Unknown channels are intentionally discrete until a family claims
 * a continuous meaning.
 */
public final class PoseChannels {
    private static final Set<String> CONTINUOUS=Set.of(
        "flap","flap_speed","roll","eat","stand","mouth","attack","flower","grazing");
    private PoseChannels() {}

    public static boolean continuous(String name) { return CONTINUOUS.contains(name); }
    /** Immutable interpolation preserving the union of authoritative channel names. */
    public static Map<String,Float> interpolate(Map<String,Float> from,Map<String,Float> to,float fraction) {
        var result=new TreeMap<String,Float>();
        var names=new java.util.TreeSet<String>();names.addAll(from.keySet());names.addAll(to.keySet());
        for(var name:names) {
            Float a=from.get(name),b=to.get(name);
            if(a==null)result.put(name,b);
            else if(b==null)result.put(name,a);
            else result.put(name,continuous(name)?Mth.lerp(fraction,a,b):(fraction<.5f?a:b));
        }
        return Map.copyOf(result);
    }
}
