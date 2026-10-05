package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Explicit policy for hand-authored core convex fixtures, never a prepared/READY session. */
public final class TestCoreBindings {
    private TestCoreBindings() {}
    private static CollisionBinding binding(LivingEntity support,GeometryProvider.GeometryIdentityDescriptor descriptor){
        var existing=io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime.catalogBinding(support)
            .orElse(AnatomyMovement.canonicalBinding(support));
        if(existing!=null)return existing;
        var model=descriptor==null?Identifier.parse("test:explicit_core"):descriptor.model();
        var pose=descriptor==null?Identifier.parse("test:explicit_static"):descriptor.poseProvider();
        var root=descriptor==null?Identifier.parse("scalebrews:entity_root"):descriptor.rootProvider();
        return new CollisionBinding(1,BuiltInRegistries.ENTITY_TYPE.getKey(support.getType()),Map.of(),
            new CollisionBinding.Geometry(Identifier.parse("test:explicit_convex"),model,Map.of(),AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(pose,Map.of(),Set.of()),root,
            new CollisionPolicy.Patch(Optional.empty(),Optional.empty(),Optional.of(.6)),Set.of());
    }
    public static void register(LivingEntity support,GeometryProvider provider){
        AnatomyMovement.registerCore(support,provider,binding(support,null));
    }
    public static void register(LivingEntity support,GeometryProvider provider,GeometryProvider.GeometryIdentityDescriptor descriptor){
        AnatomyMovement.register(support,provider,descriptor,binding(support,descriptor));
    }
    public static void register(LivingEntity support,GeometryProvider provider,GeometryProvider.GeometryIdentityDescriptor descriptor,CollisionBinding binding){
        AnatomyMovement.register(support,provider,descriptor,binding);
    }
}
