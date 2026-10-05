package io.github.r3neer.scalebrews.test;

/** Classpath/mixin absence replaces assertions about the deleted secondary state, never physical assertions. */
public final class TestRetiredPlatformRuntime {
    private static final boolean ABSENT = inspect();
    private TestRetiredPlatformRuntime() {}
    public static boolean absent() { return ABSENT; }
    private static boolean inspect() {
        var loader=TestRetiredPlatformRuntime.class.getClassLoader();
        for(String type:java.util.List.of("PlatformBody","PlatformState","PlatformPhysics","PlatformGeometry",
                "PlatformPayload","PlatformMovePayload","PlatformNetworking","PlatformMovementReference")) {
            try {Class.forName("io.github.r3neer.scalebrews.platform."+type,false,loader);return false;}
            catch(ClassNotFoundException expected) { /* Required physical owner is absent from the actual runtime. */ }
        }
        return java.util.Arrays.stream(net.minecraft.world.entity.Entity.class.getDeclaredMethods())
            .noneMatch(method->method.getName().equals("scalebrews$platform"));
    }
}
