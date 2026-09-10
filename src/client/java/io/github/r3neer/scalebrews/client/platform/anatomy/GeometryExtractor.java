package io.github.r3neer.scalebrews.client.platform.anatomy;

import io.github.r3neer.scalebrews.platform.anatomy.ModelGeometry;
import java.util.Set;
import net.minecraft.client.model.geom.ModelPart;

/**
 * @deprecated Preparation tooling moved to
 * {@code io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor}.
 * This source-compatible shim exists only while old proof fixtures are migrated.
 */
@Deprecated(forRemoval=true)
public final class GeometryExtractor {
    private GeometryExtractor() {}
    public static ModelGeometry vanilla(String source,String version,ModelPart root,Set<String> excludedParts) {
        return io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor.vanilla(source,version,root,excludedParts);
    }
    public static ModelGeometry alex(String source,String version,Object model,Set<String> excludedParts) {
        return io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor.alex(source,version,model,excludedParts);
    }
}
