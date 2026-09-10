package io.github.r3neer.scalebrews.platform;

import io.github.r3neer.scalebrews.mixin.PlatformBoatItemAccessor;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.AnatomyMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;

/** Item placement is authoritative and uses the same physical policy as falling onto a support. */
public final class PlatformPlacement {
    private PlatformPlacement() {}

    public static InteractionResult interact(Player player, Entity target, InteractionHand hand, Vec3 location) {
        var stack = player.getItemInHand(hand);
        if (player.isSpectator() || !(target instanceof LivingEntity support)
                || !(stack.getItem() instanceof BoatItem || stack.getItem() instanceof SpawnEggItem)
                || Platforms.definition(support) == null || !Platforms.ordinary(support)) return InteractionResult.PASS;
        if (!Platforms.policy(player.level()).enabled()) return InteractionResult.PASS;
        if (location == null || !Double.isFinite(location.lengthSqr())) return InteractionResult.FAIL;
        // The vanilla interaction packet carries a position relative to the target's feet.
        Vec3 clicked = target.position().add(location);
        if (!target.getBoundingBox().inflate(.25).contains(clicked)) return InteractionResult.FAIL;
        // Ownership and availability are distinct. BINDING must never route this
        // interaction through the old surface/AABB implementation while the
        // accepted catalog is unavailable.
        if(AnatomyApi.mode(player)==AnatomyMode.READY) return anatomicalInteract(player,support,hand,stack,clicked);
        if(AnatomyApi.mode(player)==AnatomyMode.BINDING) return InteractionResult.FAIL;
        var definition = Platforms.definition(support);
        var frame = PlatformGeometry.frame(support);
        var surface = definition.surfaces().stream().min(java.util.Comparator
                .comparingDouble((PlatformDefinition.Surface s) -> frame.world(new Vec3(s.x(),s.y(),s.z())).distanceToSqr(clicked))
                .thenComparing(PlatformDefinition.Surface::id)).orElse(null);
        if (surface == null) return InteractionResult.FAIL;
        Vec3 position = frame.world(new Vec3(surface.x(),surface.y(),surface.z()));
        BlockPos block = BlockPos.containing(position);
        if (!player.getAbilities().mayBuild || !player.level().mayInteract(player,block)
                || !player.mayUseItemAt(block,Direction.UP,stack)
                || !stack.isItemEnabled(player.level().enabledFeatures())) return InteractionResult.FAIL;
        if (!(player.level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        EntityType<?> type = stack.getItem() instanceof BoatItem boat
                ? ((PlatformBoatItemAccessor)boat).scalebrews$type() : SpawnEggItem.getType(stack);
        if (type == null || !type.canSpawn(level)) return InteractionResult.FAIL;
        Entity body = create(type, level, stack, player, block);
        if (body == null) return InteractionResult.FAIL;
        body.snapTo(position.x,position.y,position.z,player.getYRot(),0);
        var box = body.getBoundingBox();
        if (!Platforms.eligible(body,support) || !fits(frame,surface,box)) {
            body.discard();
            // Matching eggs on ordinary adults must retain vanilla's baby-spawning interaction.
            return stack.getItem() instanceof SpawnEggItem && type == support.getType()
                    && support instanceof AgeableMob ? InteractionResult.PASS : InteractionResult.FAIL;
        }
        boolean clear = level.getWorldBorder().isWithinBounds(box) && box.minY >= level.getMinY() && box.maxY <= level.getMaxY()
                && !level.getBlockCollisions(body,box.deflate(1e-6)).iterator().hasNext()
                && level.getEntities(body,box.deflate(1e-6), e -> e != support && !e.isSpectator() && e.isAlive()).isEmpty();
        // Do not place through an intervening block, even when interacting with a large hitbox.
        var obstruction = level.clip(new ClipContext(player.getEyePosition(),position.add(0,.01,0),
                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if (!clear || obstruction.getType() != HitResult.Type.MISS) { body.discard(); return InteractionResult.FAIL; }
        if (!level.addFreshEntity(body)) { body.discard(); return InteractionResult.FAIL; }
        var state = Platforms.state(body);
        state.support=support; state.surface=surface; state.frame=frame; state.contact=frame.local(position);
        body.setOnGround(true); body.resetFallDistance();
        PlatformNetworking.broadcast(body);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        stack.consume(1,player);
        level.gameEvent(player,GameEvent.ENTITY_PLACE,position);
        return InteractionResult.SUCCESS;
    }

    /** Anatomical placement is material-ray based; it never guesses a legacy surface from an entity AABB. */
    private static InteractionResult anatomicalInteract(Player player,LivingEntity target,InteractionHand hand,ItemStack stack,Vec3 clicked) {
        if(!(player.level() instanceof ServerLevel level))return InteractionResult.SUCCESS;
        EntityType<?> type=stack.getItem() instanceof BoatItem boat
            ? ((PlatformBoatItemAccessor)boat).scalebrews$type() : SpawnEggItem.getType(stack);
        if(type==null || !type.canSpawn(level))return InteractionResult.FAIL;
        Entity body=create(type,level,stack,player,BlockPos.containing(clicked));
        if(body==null)return InteractionResult.FAIL;
        // Query as the prospective body: eligibility is body-specific (boat and
        // egg entity may differ from the player who clicked).
        var hit=AnatomyApi.raycast(body,player.getEyePosition(),clicked)
            .filter(candidate->candidate.support()==target).orElse(null);
        if(hit==null){body.discard();return babyFallback(stack,type,target);}
        Direction face=placementFace(hit.contact().normal());
        if(face==null){body.discard();return InteractionResult.FAIL;}
        BlockPos block=BlockPos.containing(hit.position());
        if(!player.getAbilities().mayBuild || !player.level().mayInteract(player,block)
            || !player.mayUseItemAt(block,face,stack) || !stack.isItemEnabled(player.level().enabledFeatures())) {
            body.discard();return InteractionResult.FAIL;
        }
        body.snapTo(hit.position().x,hit.position().y,hit.position().z,player.getYRot(),0);
        placeOutward(body,hit.position(),hit.contact().normal());
        var box=body.getBoundingBox();
        boolean clear=level.getWorldBorder().isWithinBounds(box) && box.minY>=level.getMinY() && box.maxY<=level.getMaxY()
            && !level.getBlockCollisions(body,box.deflate(1e-6)).iterator().hasNext()
            && AnatomyApi.spaceClear(body,box.deflate(1e-6))
            && level.getEntities(body,box.deflate(1e-6),entity->entity!=target && !entity.isSpectator() && entity.isAlive()).isEmpty();
        var obstruction=level.clip(new ClipContext(player.getEyePosition(),hit.position(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if(!clear || obstruction.getType()!=HitResult.Type.MISS || !level.addFreshEntity(body) || !AnatomyApi.attachAtContact(body,hit)) {
            body.discard();return InteractionResult.FAIL;
        }
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));stack.consume(1,player);
        level.gameEvent(player,GameEvent.ENTITY_PLACE,body.position());
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult babyFallback(ItemStack stack,EntityType<?> type,LivingEntity support) {
        return stack.getItem() instanceof SpawnEggItem && type==support.getType() && support instanceof AgeableMob
            ? InteractionResult.PASS:InteractionResult.FAIL;
    }
    /**
     * Permissions require a cardinal face, while material support remains
     * arbitrary convex geometry. Keep the floating-point normal until this
     * deliberate approximate cardinal projection; it does not filter physics.
     */
    private static Direction placementFace(Vec3 normal) {
        if(!Double.isFinite(normal.lengthSqr()) || normal.lengthSqr()<=1e-18)return null;
        return Direction.getApproximateNearest(normal.x,normal.y,normal.z);
    }
    /** Move the newly-created AABB until its outward-most support-facing extent touches the material plane. */
    private static void placeOutward(Entity body,Vec3 point,Vec3 normal) {
        AABB box=body.getBoundingBox();double min=Double.POSITIVE_INFINITY;
        for(double x:new double[]{box.minX,box.maxX})for(double y:new double[]{box.minY,box.maxY})for(double z:new double[]{box.minZ,box.maxZ})
            min=Math.min(min,normal.x*x+normal.y*y+normal.z*z);
        double plane=normal.dot(point),distance=plane-min+1e-5;
        body.setPos(body.position().add(normal.scale(distance)));
    }

    private static <T extends Entity> T create(EntityType<T> type, ServerLevel level, ItemStack stack, Player player, BlockPos pos) {
        return type.create(level,EntityType.createDefaultStackConfig(level,stack,player),pos,
                EntitySpawnReason.SPAWN_ITEM_USE,false,false);
    }

    /** Every footprint corner must be on the oriented surface; overlap alone is not sufficient for placement. */
    public static boolean fits(PlatformGeometry.Frame frame, PlatformDefinition.Surface surface, AABB box) {
        for(double x:new double[]{box.minX,box.maxX}) for(double z:new double[]{box.minZ,box.maxZ}) {
            Vec3 point=frame.local(new Vec3(x,box.minY,z));
            if(Math.abs(point.x-surface.x())>surface.width()/2+1e-7
                    || Math.abs(point.z-surface.z())>surface.depth()/2+1e-7) return false;
        }
        return true;
    }
}
