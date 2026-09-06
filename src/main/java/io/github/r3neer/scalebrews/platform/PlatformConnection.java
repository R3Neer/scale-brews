package io.github.r3neer.scalebrews.platform;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
/** Keeps movement baselines in the same coordinate frame after server-applied transport. */
public interface PlatformConnection { void scalebrews$transportBaseline(Entity body,Vec3 delta); }
