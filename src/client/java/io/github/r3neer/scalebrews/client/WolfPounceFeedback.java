package io.github.r3neer.scalebrews.client;

import io.github.r3neer.scalebrews.client.render.WolfPounceVisualState;
import io.github.r3neer.scalebrews.mount.WolfMount;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.joml.Matrix4f;

/** Rider-local anticipation, launch stretch, FOV kick and short impact shake for wolf pounces. */
public final class WolfPounceFeedback {
    private static int wolfId = -1;
    private static int chargeTicks;
    private static int pounceTicks;
    private static long clientTicks;
    private static boolean previousJump;
    private static boolean airborne;
    private static boolean pouncing;
    private static boolean previousSwing;
    private static float fovKick;
    private static float shake;

    private WolfPounceFeedback() {}

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(WolfPounceFeedback::tick);
    }

    private static void tick(Minecraft client) {
        clientTicks++;
        var player = client.player;
        if (player == null || !(player.getVehicle() instanceof Wolf wolf) || !WolfMount.enabled(wolf)) {
            resetMount();
            fovKick *= .68F;
            shake *= .56F;
            return;
        }
        if (wolf.getId() != wolfId) {
            resetMount();
            wolfId = wolf.getId();
        }

        boolean jump = player.input.keyPresses.jump();
        if (jump && wolf.onGround() && !pouncing) chargeTicks++;
        if (!jump && previousJump) {
            int releasedCharge = chargeTicks;
            chargeTicks = 0;
            if (releasedCharge > 3 && wolf.onGround()) {
                pouncing = true;
                airborne = false;
                pounceTicks = 0;
            }
        }

        if (pouncing) {
            pounceTicks++;
            if (!wolf.onGround()) airborne = true;
            if (wolf.swinging && !previousSwing) shake = Math.max(shake, 1.0F);
            if ((airborne && wolf.onGround()) || pounceTicks > 38) {
                if (airborne && wolf.onGround()) shake = Math.max(shake, .72F);
                pouncing = false;
                airborne = false;
                pounceTicks = 0;
            }
        }

        float targetFov = pouncing ? 1 : 0;
        fovKick += (targetFov - fovKick) * .38F;
        shake *= .58F;
        previousJump = jump;
        previousSwing = wolf.swinging;
    }

    private static void resetMount() {
        wolfId = -1;
        chargeTicks = 0;
        pounceTicks = 0;
        previousJump = false;
        previousSwing = false;
        airborne = false;
        pouncing = false;
    }

    public static void extract(Wolf wolf, LivingEntityRenderState state) {
        var visual = (WolfPounceVisualState) state;
        if (wolf.getId() != wolfId) {
            visual.scalebrews$wolfScale(1, 1, 1);
            return;
        }
        float charge = Math.clamp((chargeTicks - 3) / 7F, 0F, 1F);
        if (pouncing) {
            visual.scalebrews$wolfScale(.965F, .955F, 1.075F);
        } else if (charge > 0) {
            visual.scalebrews$wolfScale(1 + .035F * charge, 1 - .065F * charge, 1 + .035F * charge);
        } else {
            visual.scalebrews$wolfScale(1, 1, 1);
        }
    }

    public static float modifyFov(float original) {
        Minecraft client = Minecraft.getInstance();
        float effectScale = client.options == null ? 1 : client.options.fovEffectScale().get().floatValue();
        return original + 8F * fovKick * effectScale;
    }

    public static Matrix4f applyViewShake(Matrix4f matrix) {
        if (shake < .01F) return matrix;
        float phase = clientTicks * 2.47F;
        float x = (float)Math.sin(phase) * shake * .018F;
        float z = (float)Math.cos(phase * 1.37F) * shake * .013F;
        return matrix.rotateX(x).rotateZ(z);
    }
}
