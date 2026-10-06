import io.github.r3neer.scalebrews.collision.pose.LivingLocomotionClock;
import io.github.r3neer.scalebrews.collision.pose.LivingPoseInputSource;
import java.lang.reflect.Modifier;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Cold linking/absence/ownership only; no original actor executes in this JVM. */
public final class S62Common {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void main(String[] args) throws Exception {
        int bindings = 0;
        for (String name : List.of("net.minecraft.world.entity.animal.cow.Cow", "net.minecraft.world.entity.animal.pig.Pig",
                                  "net.minecraft.world.entity.animal.chicken.Chicken", "net.minecraft.world.entity.animal.nautilus.ZombieNautilus")) {
            Class actor = Class.forName(name, false, S62Common.class.getClassLoader());
            for (var source : LivingPoseInputSource.Source.values()) {
                var bound = LivingPoseInputSource.bind(actor, new LivingPoseInputSource.Program(1, name,
                    List.of(new LivingPoseInputSource.Channel("input", source))));
                check(bound.sample(null).isEmpty() && bound.sample(null, null, 0).isEmpty(), "null authority accepted"); bindings++;
            }
        }
        var clock = new LivingLocomotionClock(); check(clock.current().isEmpty(), "query creates frame");
        try { clock.advance(null, 0, io.github.r3neer.scalebrews.collision.api.GravityFrame.VANILLA); throw new AssertionError("null actor accepted"); }
        catch (IllegalArgumentException expected) {}
        check(clock.current().isEmpty(), "invalid advance creates frame");
        for (Class<?> owner : List.of(LivingLocomotionClock.class, LivingLocomotionClock.Frame.class))
            for (var field : owner.getDeclaredFields())
                check(!LivingEntity.class.isAssignableFrom(field.getType()) && !Level.class.isAssignableFrom(field.getType()), "strong actor/world field " + field);
        for (var constructor : LivingLocomotionClock.Frame.class.getDeclaredConstructors())
            check(Modifier.isPrivate(constructor.getModifiers()), "public/data frame constructor");
        var state = Class.forName("io.github.r3neer.scalebrews.collision.internal.AuthorityPoseTracker$State", false, S62Common.class.getClassLoader());
        check(java.util.Arrays.stream(state.getDeclaredFields()).noneMatch(f -> f.getType() == net.minecraft.world.entity.WalkAnimationState.class), "tracker owns another walk clock");
        System.out.println("S62_COMMON_COLD_PASS bindings=" + bindings + " clientVeto=true originalActorExecution=false singleOwner=true");
    }
}
