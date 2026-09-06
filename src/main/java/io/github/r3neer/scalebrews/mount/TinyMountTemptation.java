package io.github.r3neer.scalebrews.mount;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.EnumSet;

/** Steering items extend native food attraction. Creatures without a TemptGoal get a fallback goal. */
public final class TinyMountTemptation {
    private TinyMountTemptation() {}

    public static boolean matches(TinyMountDefinition definition, ItemStack stack) {
        return definition != null && definition.control() == TinyMountDefinition.Control.ITEM_STEERED
                && !stack.isEmpty() && definition.steeringItem().flatMap(BuiltInRegistries.ITEM::getOptional)
                .map(stack::is).orElse(false);
    }

    public static boolean follows(Mob mob, LivingEntity player) {
        if (!mob.isAlive() || mob.isNoAi() || mob.isVehicle() || mob.isPassenger() || mob.getTarget() != null) return false;
        var definition = TinyMounts.definition(mob);
        return matches(definition, player.getMainHandItem()) || matches(definition, player.getOffhandItem());
    }

    public static void install(Mob mob, GoalSelector goals) {
        var definition = TinyMounts.definition(mob);
        if (definition == null || definition.control() != TinyMountDefinition.Control.ITEM_STEERED) return;
        if (goals.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof TemptGoal)) return;
        // Priority 3 leaves the usual emergency/combat goals above attraction.
        goals.addGoal(3, new SteeringGoal(mob));
    }

    /** Same nearest-player, two-hand, stop-distance and cooldown rules as TemptGoal.
     * Also handles modded mobs without TEMPT_RANGE or PathfinderMob navigation. */
    public static final class SteeringGoal extends Goal {
        private final Mob mob;
        private final TargetingConditions targeting;
        private Player player;
        private int cooldown;

        public SteeringGoal(Mob mob) {
            this.mob = mob;
            targeting = TargetingConditions.forNonCombat().ignoreLineOfSight()
                    .selector((entity, level) -> follows(mob, entity));
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (cooldown > 0) { cooldown--; return false; }
            double range = mob.getAttributes().hasAttribute(Attributes.TEMPT_RANGE)
                    ? mob.getAttributeValue(Attributes.TEMPT_RANGE) : 10;
            player = ((ServerLevel)mob.level()).getNearestPlayer(targeting.range(range), mob);
            return player != null;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public void stop() {
            player = null;
            stopMoving();
            cooldown = reducedTickDelay(100);
        }

        @Override public void tick() {
            if (player == null || !follows(mob, player)) return;
            mob.getLookControl().setLookAt(player, mob.getMaxHeadYRot() + 20, mob.getMaxHeadXRot());
            if (mob.distanceToSqr(player) < 2.5 * 2.5) {
                stopMoving();
            } else if (mob instanceof PathfinderMob) {
                mob.getNavigation().moveTo(player, 1.25);
            } else {
                var target = player.getEyePosition();
                mob.getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.25);
            }
        }

        private void stopMoving() {
            if (mob instanceof PathfinderMob) mob.getNavigation().stop();
            else mob.getMoveControl().setWait();
        }
    }
}
