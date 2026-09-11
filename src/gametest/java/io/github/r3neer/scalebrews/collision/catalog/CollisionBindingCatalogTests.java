package io.github.r3neer.scalebrews.collision.catalog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** G1 bounded-loader tests kept in-package so the byte reader need not become public API. */
public final class CollisionBindingCatalogTests {
    @GameTest
    public void catalogBudgetCountsUtf8BytesRatherThanCharacters(GameTestHelper h) {
        byte[] utf8 = "éé".getBytes(StandardCharsets.UTF_8);
        h.assertTrue(utf8.length == 4, "Fixture requires two UTF-8 multibyte characters");
        boolean rejected = false;
        try {
            read(utf8, 3);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        h.assertTrue(rejected, "Three remaining bytes cannot admit four encoded bytes even though the text has only two characters");
        h.assertTrue(read(utf8, 4).length == 4, "Exact byte boundary remains valid");
        h.succeed();
    }

    private static byte[] read(byte[] source, long budget) {
        try {
            return CollisionBindingCatalog.readBounded(new ByteArrayInputStream(source), budget);
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
