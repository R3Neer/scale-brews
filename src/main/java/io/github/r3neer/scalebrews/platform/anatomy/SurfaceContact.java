package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;

/** Wire-independent contact identity; selected Clinging reference is separate from live support. */
public record SurfaceContact(UUID support,long revision,String piece,int face,Vec3 localPoint,Vec3 normal,long tick) {
    public SurfaceContact {
        if(support==null || piece==null || piece.isBlank() || piece.length()>256 || face<0 || face>5
            || !Double.isFinite(localPoint.lengthSqr()+normal.lengthSqr()) || Math.abs(normal.lengthSqr()-1)>1e-5)
            throw new IllegalArgumentException("Invalid anatomical contact");
        if(localPoint.x< -1e-6 || localPoint.y< -1e-6 || localPoint.z< -1e-6 || localPoint.x>1+1e-6 || localPoint.y>1+1e-6 || localPoint.z>1+1e-6)
            throw new IllegalArgumentException("Contact lies outside its material piece");
        double coordinate=switch(face/2){case 0->localPoint.x;case 1->localPoint.y;default->localPoint.z;};
        if(Math.abs(coordinate-face%2)>1e-6)throw new IllegalArgumentException("Point is not on the declared face");
    }
}
