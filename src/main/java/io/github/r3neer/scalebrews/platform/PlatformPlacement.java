package io.github.r3neer.scalebrews.platform;

import io.github.r3neer.scalebrews.mixin.PlatformBoatItemAccessor;
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
