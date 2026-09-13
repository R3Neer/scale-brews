package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.ModelPartGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Structural client/tooling boundary for S18. Semantic equivalence is added once the compiler API exists. */
public final class S18AnimationDefinitionCompilerClientTests implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            try {
                Path clientRoot = Path.of(ModelPartGeometryEngine.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                Path preparation = clientRoot.resolve("io/github/r3neer/scalebrews/client/collision/preparation");
                check(Files.isDirectory(preparation), "Could not inspect client preparation classes for S18");
                boolean compilerFound;
                try (var classes = Files.walk(preparation)) {
                    compilerFound = classes.filter(path -> path.toString().endsWith(".class"))
                        .anyMatch(path -> contains(path, "net/minecraft/client/animation/")
                            || contains(path, "net/minecraft/client/render/entity/animation/"));
                }
                check(compilerFound,
                    "S18 requires client/tooling preparation code that actually references Mojang's client animation API before emitting a neutral server-safe program");

                Path commonRoot = Path.of(PoseEngine.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                Path collision = commonRoot.resolve("io/github/r3neer/scalebrews/collision");
                check(Files.isDirectory(collision), "Could not inspect common collision classes for S18");
                try (var classes = Files.walk(collision)) {
                    var leaking = classes.filter(path -> path.toString().endsWith(".class"))
                        .filter(path -> contains(path, "net/minecraft/client/animation/")
                            || contains(path, "net/minecraft/client/render/entity/animation/"))
                        .findFirst();
                    check(leaking.isEmpty(),
                        "Client-animation type references must remain outside common collision runtime: " + leaking.orElse(null));
                }

                System.out.println("S18_ANIMATION_COMPILER_BOUNDARY PASS client compiler present, common runtime client-free");
            } catch (AssertionError failure) {
                throw failure;
            } catch (Exception failure) {
                throw new AssertionError("Could not inspect S18 compiler boundary", failure);
            }
        });
    }

    private static boolean contains(Path path, String needle) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.ISO_8859_1).contains(needle);
        } catch (Exception unreadable) {
            throw new IllegalStateException("Could not inspect class " + path, unreadable);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
