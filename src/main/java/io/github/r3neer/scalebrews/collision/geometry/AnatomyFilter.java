package io.github.r3neer.scalebrews.collision.geometry;

import net.minecraft.world.phys.AABB;

/** Filtering is performed before entity scale and animation. */
public record AnatomyFilter(double minThickness,double minAspect,double minVolumeRatio,
                            java.util.Set<String> include,java.util.Set<String> exclude) {
    public AnatomyFilter(double minThickness,double minAspect,double minVolumeRatio) {
        this(minThickness,minAspect,minVolumeRatio,java.util.Set.of(),java.util.Set.of());
    }
    public static final AnatomyFilter DEFAULT=new AnatomyFilter(.5/16,.025,.00001);
    public AnatomyFilter {
        include=java.util.Set.copyOf(include);exclude=java.util.Set.copyOf(exclude);
        if(include.size()+exclude.size()>4096 || java.util.stream.Stream.concat(include.stream(),exclude.stream()).anyMatch(id->id.isBlank() || id.length()>256))
            throw new IllegalArgumentException("Invalid anatomy piece selection");
        if(include.stream().anyMatch(exclude::contains))throw new IllegalArgumentException("Piece both included and excluded");
        if(!Double.isFinite(minThickness+minAspect+minVolumeRatio) || minThickness<0 || minAspect<0 || minAspect>1 || minVolumeRatio<0 || minVolumeRatio>1)
            throw new IllegalArgumentException("Invalid anatomy filter");
    }
    public String rejection(ModelGeometry.Piece piece,double modelVolume) {
        if(piece.excluded()!=null)return piece.excluded(); // Equipment/cosmetic roles never become anatomy.
        if(exclude.contains(piece.id()) || exclude.contains(piece.part()))return "explicit_exclusion";
        var rejection=rejection(piece.box(),modelVolume);
        if(!"degenerate".equals(rejection) && (include.contains(piece.id()) || include.contains(piece.part())))return null;
        return rejection;
    }
    public String rejection(AABB b,double modelVolume) {
        double x=b.getXsize(),y=b.getYsize(),z=b.getZsize(),min=Math.min(x,Math.min(y,z)),max=Math.max(x,Math.max(y,z));
        if(!Double.isFinite(x+y+z+modelVolume) || min<=0 || modelVolume<=0)return "degenerate";
        if(min<minThickness)return "thickness";
        if(min/max<minAspect)return "aspect";
        if(x*y*z/modelVolume<minVolumeRatio)return "volume";
        return null;
    }
}
