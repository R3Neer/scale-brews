package io.github.r3neer.scalebrews.client.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.WeakHashMap;

/** Translation-only camera follower; visual samples are short-lived and keyed by the vehicle object. */
public final class TinyMountCamera {
    private record Sample(Vec3 seatWorld, Vec3 up, long capturedAt) {}
    private static final WeakHashMap<Entity, Sample> SAMPLES = new WeakHashMap<>();
    private static Entity previousVehicle;
    private static Object previousWorld;
    private static Vec3 previousPhysical = Vec3.ZERO;
    private static Vec3 smoothed = Vec3.ZERO;
    private static long nanos;
    private TinyMountCamera() {}

    public static void capture(int entityId, SeatFrame frame) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        Entity vehicle = client.level.getEntity(entityId);
        if (vehicle == null) return;
        Vec3 camera = client.gameRenderer.mainCamera().position();
        synchronized (SAMPLES) {
            SAMPLES.put(vehicle, new Sample(camera.add(frame.cameraPosition().x, frame.cameraPosition().y, frame.cameraPosition().z),
                    new Vec3(frame.up().x, frame.up().y, frame.up().z),
                    System.nanoTime()));
        }
    }

    public static Vec3 offset(Camera camera, float partialTick) {
        Entity rider = camera.entity();
        Entity vehicle = rider == null ? null : rider.getVehicle();
        if (vehicle == null || io.github.r3neer.scalebrews.mount.TinyMounts.definition(vehicle) == null) {
            reset();
            return Vec3.ZERO;
        }
        Sample sample;
        synchronized (SAMPLES) { sample = SAMPLES.get(vehicle); }
        long now = System.nanoTime();
        if (sample == null || now - sample.capturedAt > 250_000_000L) {
            reset();
            return Vec3.ZERO;
        }
        Vec3 physical = rider.getPosition(partialTick);
        boolean reset = previousVehicle != vehicle || previousWorld != rider.level()
                || physical.distanceToSqr(previousPhysical) > 16;
        double dt = nanos == 0 ? .05 : Math.clamp((now - nanos) / 1E9, 0, .1);
        Vec3 desired = sample.seatWorld.subtract(physical)
                .subtract(sample.up.scale(rider.getBbHeight() * .4));
        if (reset) smoothed = desired;
        else smoothed = smoothed.lerp(desired, 1 - Math.exp(-dt / .1));
        previousVehicle = vehicle;
        previousWorld = rider.level();
        previousPhysical = physical;
        nanos = now;
        return smoothed;
    }

    public static void reset() {
        previousVehicle = null; previousWorld = null; previousPhysical = Vec3.ZERO; smoothed = Vec3.ZERO; nanos = 0;
    }

    public static void clear() {
        synchronized (SAMPLES) { SAMPLES.clear(); }
        reset();
    }
}
