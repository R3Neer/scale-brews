package io.github.r3neer.scalebrews.client.collision.preparation;

import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.model.geom.ModelPart;

/** Preparation-only legacy entrypoints. Family engines own their concrete extraction algorithms. */
public final class GeometryExtractor {
    private GeometryExtractor() {}

    /** @deprecated S17 production/tooling should prepare ModelPart geometry through {@link ModelPartGeometryEngine}. */
    @Deprecated
    public static ModelGeometry vanilla(String source,String version,ModelPart root,Set<String> excludedParts) {
        return ModelPartGeometryExtractor.extract(source,version,root,excludedParts);
    }

    /**
     * @deprecated S19 owns AdvancedModelBox extraction through {@link AdvancedModelBoxGeometryEngine}.
     * This compatibility entrypoint delegates to that exact extractor and only translates the old
     * explicit-exclusion argument into Piece metadata; it no longer contains a second traversal or
     * reflection algorithm.
     */
    @Deprecated
    public static ModelGeometry alex(String source,String version,Object model,Set<String> excludedParts) {
        if(excludedParts==null)throw new IllegalArgumentException("Missing legacy AdvancedModelBox exclusions");
        var base=AdvancedModelBoxGeometryExtractor.extract(source,version,
            AdvancedModelBoxGeometryEngine.ALEX_2_1_9_DIALECT,model).geometry();
        if(excludedParts.isEmpty())return base;
        var pieces=new ArrayList<ModelGeometry.Piece>(base.pieces().size());
        for(var piece:base.pieces()) {
            String localPart=piece.part().substring(piece.part().lastIndexOf('/')+1);
            boolean excluded=excludedParts.contains(piece.id()) || excludedParts.contains(piece.part()) || excludedParts.contains(localPart);
            pieces.add(excluded && piece.excluded()==null
                ? new ModelGeometry.Piece(piece.id(),piece.part(),piece.min(),piece.max(),"legacy_explicit_exclusion")
                : piece);
        }
        return new ModelGeometry(base.format(),base.source(),base.version(),base.parts(),List.copyOf(pieces),base.modelTransform());
    }
}
