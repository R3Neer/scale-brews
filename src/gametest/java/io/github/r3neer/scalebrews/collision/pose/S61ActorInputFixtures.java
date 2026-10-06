package io.github.r3neer.scalebrews.collision.pose;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** IMPLEMENTER fixture inputs only. Expected scalars come from the native renderer capture. */
public final class S61ActorInputFixtures {
    private S61ActorInputFixtures() {}
    public static List<Entity> create(Level level, String type, int index) {
        var actor = (LivingEntity)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(type)).create(level, EntitySpawnReason.COMMAND);
        var objects = new ArrayList<Entity>(); objects.add(actor);
        actor.setPos(5, 8, 5); actor.setId(61000 + index); actor.tickCount = index == 2 ? Integer.MAX_VALUE : 97 + index;
        actor.yHeadRotO = -121; actor.yHeadRot = 17;
        actor.yBodyRotO = 210; actor.yBodyRot = 3;
        actor.xRotO = -31; actor.setXRot(29);
        if (actor instanceof AgeableMob ageable) ageable.setAge(index == 1 ? -24000 : 0);
        actor.walkAnimation.stop();
        for (int i = 0; i < 5; i++) actor.walkAnimation.update(1.3f, .4f, index == 1 ? 3f : 1f);
        if (index == 1) { actor.setCustomName(Component.literal("Dinnerbone")); actor.xRotO = 0; actor.setXRot(0); }
        if (index == 2) { actor.yHeadRotO = 1e8f; actor.yHeadRot = 1; actor.xRotO = 1e8f; actor.setXRot(1); }
        if (index == 3) { actor.setHealth(0); actor.walkAnimation.setSpeed(Float.NaN); }
        if (index == 4 || index == 5 || index == 6) {
            Entity vehicle = (index == 6 ? EntityTypes.ITEM : EntityTypes.COW).create(level, EntitySpawnReason.COMMAND);
            vehicle.setPos(5, 8, 5); vehicle.setId(61900 + index); objects.add(vehicle);
            if (vehicle instanceof LivingEntity living) { living.yBodyRotO = -400; living.yBodyRot = 72; }
            actor.yHeadRotO = index == 5 ? 157 : -10; actor.yHeadRot = index == 5 ? 157 : 139;
            if (!actor.startRiding(vehicle, true, false)) throw new AssertionError("S61 fixture mount rejected");
        }
        if (index == 7) {
            actor.setTicksFrozen(200); actor.setCustomName(Component.literal("Grumm"));
            try {
                var water = Entity.class.getDeclaredField("wasTouchingWater"); water.setAccessible(true); water.setBoolean(actor, true);
                var spin = LivingEntity.class.getDeclaredMethod("setLivingEntityFlag", int.class, boolean.class);
                spin.setAccessible(true); spin.invoke(actor, 4, true);
            } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        }
        return objects;
    }
    public static void destroy(List<Entity> entities) {
        entities.getFirst().stopRiding(); entities.forEach(Entity::discard);
    }
}
