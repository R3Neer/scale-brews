package io.github.r3neer.scalebrews.platform;

import io.github.r3neer.scalebrews.mixin.PlatformBoatItemAccessor;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.AnatomyMode;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;

/** Item placement is authoritative and uses the same physical policy as falling onto a support. */
public final class PlatformPlacement {
    private PlatformPlacement() {}
    private static final ThreadLocal<Consumer<Entity>> TEST_CANDIDATE_HOOK=new ThreadLocal<>();
    /** World-thread transaction fence: callbacks during insertion must not spend the same hand twice. */
    private static final ThreadLocal<Set<Player>> ACTIVE_ANATOMICAL_PLACEMENTS=ThreadLocal.withInitial(
        ()->Collections.newSetFromMap(new IdentityHashMap<>()));

    /** Scoped development-only seam for giving the exact newborn GameTest body its external gravity frame. */
    public static void candidateHookForTests(Consumer<Entity> hook) {
        if(!FabricLoader.getInstance().isDevelopmentEnvironment())throw new IllegalStateException("GameTest only");
        if(hook==null)TEST_CANDIDATE_HOOK.remove();else TEST_CANDIDATE_HOOK.set(hook);
    }

    public static InteractionResult interact(Player player, Entity target, InteractionHand hand, Vec3 location) {
        var stack = player.getItemInHand(hand);
        if (player.isSpectator() || !(target instanceof LivingEntity support)
                || !(stack.getItem() instanceof BoatItem || stack.getItem() instanceof SpawnEggItem)
                || !Platforms.ordinary(support)) return InteractionResult.PASS;
        if (location == null || !Double.isFinite(location.lengthSqr())) return InteractionResult.FAIL;
        // The vanilla interaction packet carries a position relative to the target's feet.
        Vec3 clicked = target.position().add(location);
        // Ownership and availability are distinct. BINDING must never route this
        // interaction through the old surface/AABB implementation while the
        // accepted catalog is unavailable.
        if(AnatomyApi.mode(player)==AnatomyMode.READY) return anatomicalInteract(player,support,hand,stack,clicked);
        if(AnatomyApi.mode(player)==AnatomyMode.BINDING) return InteractionResult.FAIL;
        if(Platforms.definition(support)==null || !Platforms.policy(player.level()).enabled())return InteractionResult.PASS;
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
        Platforms.trackLegacyParticipant(body);
        body.setOnGround(true); body.resetFallDistance();
        PlatformNetworking.broadcast(body);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        stack.consume(1,player);
        level.gameEvent(player,GameEvent.ENTITY_PLACE,position);
        return InteractionResult.SUCCESS;
    }

    /** Anatomical placement is material-ray based; it never guesses a legacy surface from an entity AABB. */
    private static InteractionResult anatomicalInteract(Player player,LivingEntity target,InteractionHand hand,ItemStack stack,Vec3 clicked) {
        var active=ACTIVE_ANATOMICAL_PLACEMENTS.get();
        if(!active.add(player))return InteractionResult.FAIL;
        try {return anatomicalInteractOnce(player,target,hand,stack,clicked);}
        finally {
            active.remove(player);
            if(active.isEmpty())ACTIVE_ANATOMICAL_PLACEMENTS.remove();
        }
    }

    private static InteractionResult anatomicalInteractOnce(Player player,LivingEntity target,InteractionHand hand,ItemStack stack,Vec3 clicked) {
        EntityType<?> type=stack.getItem() instanceof BoatItem boat
            ? ((PlatformBoatItemAccessor)boat).scalebrews$type() : SpawnEggItem.getType(stack);
        if(stack.getItem() instanceof SpawnEggItem && type==target.getType() && target instanceof AgeableMob)
            return InteractionResult.PASS;
        // A type-only ENTITY_DATA component is normal on 26.2 eggs. Arbitrary
        // entity NBT and unvetted factories remain vanilla-owned interactions.
        var data=stack.get(DataComponents.ENTITY_DATA);
        if(type==null || data!=null && (data.type()!=type || !data.copyTagWithoutId().isEmpty())
                || !BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals("minecraft"))return InteractionResult.PASS;
        if(stack.isEmpty() || !stack.isItemEnabled(player.level().enabledFeatures()) || !type.canSpawn(player.level()))
            return InteractionResult.FAIL;
        // Basic factory only: the ServerLevel overload runs finalizeSpawn, which
        // can mutate unrelated entities before a later failed placement.
        Entity body=type.create(player.level(),EntitySpawnReason.SPAWN_ITEM_USE);
        if(body==null)return InteractionResult.FAIL;
        var hook=TEST_CANDIDATE_HOOK.get();if(hook!=null)hook.accept(body);
        EntityType.appendComponentsConfig(PostSpawnProcessor.<Entity>nop(),stack).apply(body);
        // Query as the prospective body: eligibility is body-specific (boat and
        // egg entity may differ from the player who clicked).
        var hit=placementHit(body,target,player.getEyePosition(),clicked);
        if(hit==null)return player.level().isClientSide() && !AnatomyMovement.placementMaterialKnown(target)
            ? InteractionResult.SUCCESS:InteractionResult.PASS;
        if(!(player.level() instanceof ServerLevel level))return InteractionResult.SUCCESS;
        Direction face=placementFace(hit.contact().normal());
        if(face==null)return InteractionResult.FAIL;
        BlockPos block=BlockPos.containing(hit.position());
        double reach=player.entityInteractionRange();
        if(!Double.isFinite(reach) || reach<=0 || player.getEyePosition().distanceToSqr(hit.position())>reach*reach+1e-7)
            return InteractionResult.FAIL;
        if(!player.getAbilities().mayBuild || !player.level().mayInteract(player,block)
            || !player.mayUseItemAt(block,face,stack) || !stack.isItemEnabled(player.level().enabledFeatures())) {
            return InteractionResult.FAIL;
        }
        body.snapTo(hit.position().x,hit.position().y,hit.position().z,player.getYRot(),0);
        placeOutward(body,hit.position(),hit.contact().normal());
        var box=body.getBoundingBox();
        boolean clear=AnatomyMovement.placementFits(body,target,hit.contact())
            && level.getWorldBorder().isWithinBounds(box) && box.minY>=level.getMinY() && box.maxY<=level.getMaxY()
            && !level.getBlockCollisions(body,box.deflate(1e-6)).iterator().hasNext()
            && AnatomyApi.spaceClear(body,box.deflate(1e-6))
            && level.getEntities(body,box.deflate(1e-6),entity->entity!=target && !entity.isSpectator() && entity.isAlive()).isEmpty();
        var obstruction=level.clip(new ClipContext(player.getEyePosition(),hit.position(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if(!clear || obstruction.getType()!=HitResult.Type.MISS)return InteractionResult.FAIL;
        if(!AnatomyApi.attachAtContact(body,hit))return InteractionResult.FAIL;
        AABB certifiedBox=body.getBoundingBox();Vec3 certifiedPosition=body.position();
        float certifiedYaw=body.getYRot(),certifiedPitch=body.getXRot();
        var certifiedSurface=AnatomyMovement.surface(body);
        ItemStack certifiedStack=stack.copy();
        boolean inserted;
        try {inserted=level.addFreshEntity(body);}
        catch(RuntimeException | Error insertionFailure) {
            AnatomyApi.clearContact(body);
            if(!body.isRemoved())body.discard();
            throw insertionFailure;
        }
        double committedReach=player.entityInteractionRange();
        // ENTITY_LOAD is synchronous and may retire or replace this exact
        // candidate, change its SCALE/physical box, or replace the hand stack
        // before addFreshEntity returns. Commit only the certified body and
        // input, still on the original material with clear surrounding space.
        var committedBox=body.getBoundingBox();
        if(!inserted || body.isRemoved() || level.getEntity(body.getUUID())!=body
                || !body.position().equals(certifiedPosition) || !committedBox.equals(certifiedBox)
                || body.getYRot()!=certifiedYaw || body.getXRot()!=certifiedPitch
                || !java.util.Objects.equals(AnatomyMovement.surface(body),certifiedSurface)
                || AnatomyApi.support(body).orElse(null)!=target || !AnatomyApi.supported(body)
                || !AnatomyMovement.placementFits(body,target,hit.contact())
                || player.getItemInHand(hand)!=stack || !ItemStack.matches(stack,certifiedStack)
                || player.isSpectator() || !stack.isItemEnabled(level.enabledFeatures()) || !type.canSpawn(level)
                || !player.getAbilities().mayBuild || !level.mayInteract(player,block)
                || !player.mayUseItemAt(block,face,stack)
                || !Double.isFinite(committedReach) || committedReach<=0
                || player.getEyePosition().distanceToSqr(hit.position())>committedReach*committedReach+1e-7
                || !level.getWorldBorder().isWithinBounds(committedBox)
                || committedBox.minY<level.getMinY() || committedBox.maxY>level.getMaxY()
                || level.getBlockCollisions(body,committedBox.deflate(1e-6)).iterator().hasNext()
                || !AnatomyApi.spaceClear(body,committedBox.deflate(1e-6))
                || !level.getEntities(body,committedBox.deflate(1e-6),entity->entity!=target && !entity.isSpectator() && entity.isAlive()).isEmpty()
                || level.clip(new ClipContext(player.getEyePosition(),hit.position(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getType()!=HitResult.Type.MISS) {
            AnatomyApi.clearContact(body);
            if(!body.isRemoved())body.discard();
            return InteractionResult.FAIL;
        }
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));stack.consume(1,player);
        level.gameEvent(player,GameEvent.ENTITY_PLACE,body.position());
        return InteractionResult.SUCCESS;
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
    /**
     * ServerboundInteractPacket uses Vec3.LP_STREAM_CODEC. LpVec3 rounds each
     * target-relative coordinate to a 32766-step grid scaled by the largest
     * coordinate, so its decoded endpoint can fall just short of the face.
     * Search only the packet's uncertainty cell after an exact miss. Extending
     * the eye ray is insufficient at grazing angles: a tiny normal error can
     * require an arbitrarily long extension. The four opposite-corner lines
     * cross every plane intersecting the cell. A recovered face must still be
     * visible from the player's eye and unambiguous within the cell.
     */
    private static AnatomyApi.RayHit placementHit(Entity body,LivingEntity target,Vec3 eye,Vec3 clicked) {
        var exact=AnatomyApi.raycast(body,eye,clicked)
            .filter(candidate->candidate.support()==target).orElse(null);
        if(exact!=null)return exact;
        Vec3 relative=clicked.subtract(target.position());
        double max=Math.max(Math.abs(relative.x),Math.max(Math.abs(relative.y),Math.abs(relative.z)));
        double axisError=Math.min(.02,(Math.ceil(max)+1)/32766.0)+1e-6;
        if(!Double.isFinite(axisError))return null;
        AnatomyApi.RayHit best=null;
        for(int ySign:new int[]{-1,1})for(int zSign:new int[]{-1,1}) {
            Vec3 corner=clicked.add(axisError,axisError*ySign,axisError*zSign);
            Vec3 opposite=clicked.subtract(axisError,axisError*ySign,axisError*zSign);
            for(int direction=0;direction<2;direction++) {
                var candidate=AnatomyApi.raycast(body,direction==0?corner:opposite,
                    direction==0?opposite:corner).orElse(null);
                if(candidate==null || candidate.support()!=target)continue;
                Vec3 point=candidate.position();
                if(Math.abs(point.x-clicked.x)>axisError || Math.abs(point.y-clicked.y)>axisError
                        || Math.abs(point.z-clicked.z)>axisError)continue;
                var visible=AnatomyApi.raycast(body,eye,point).orElse(null);
                if(visible==null)visible=AnatomyApi.raycast(body,eye,
                    point.subtract(candidate.contact().normal().scale(1e-8))).orElse(null);
                if(visible==null || visible.support()!=target || !sameFace(visible,candidate))continue;
                if(best!=null && !sameFace(best,candidate))return null;
                if(best==null || point.distanceToSqr(clicked)<best.position().distanceToSqr(clicked))best=candidate;
            }
        }
        return best;
    }

    private static boolean sameFace(AnatomyApi.RayHit a,AnatomyApi.RayHit b) {
        return a.contact().support().equals(b.contact().support())
            && a.contact().revision()==b.contact().revision()
            && a.contact().piece().equals(b.contact().piece())
            && a.contact().face()==b.contact().face();
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
