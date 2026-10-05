package io.github.r3neer.scalebrews.test;
import io.github.r3neer.scalebrews.collision.internal.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
/** Explicit test setup; complete accepted declarations, never inferred from three nominal IDs. */
public final class TestBindingTokens {
 private TestBindingTokens() {}
 public static Identifier accepted(LivingEntity support) {
  var selection=AnatomyRuntime.catalogBinding(support).orElseThrow(()->new AssertionError("Missing accepted binding for wire fixture"));
  return CollisionBindingIdentity.of(selection);
 }
 /** Local incomplete fixtures remain local; copies of complete live packets retain their token. */
 public static AnatomyPosePayload preserve(AnatomyPosePayload copy, AnatomyPosePayload source) {
  return source.bindingIdentity()==null?copy:copy.withBindingIdentity(source.bindingIdentity());
 }
}
