package io.github.r3neer.scalebrews.collision.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionCodecs;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.TreeSet;
import net.minecraft.resources.Identifier;

/** Complete canonical binding content identity. Construction is PREPARATION only. */
public final class CollisionBindingIdentity {
    public static final int FORMAT_VERSION = 1;
    private static final byte[] DOMAIN = ("scalebrews/collision-binding/v" + FORMAT_VERSION + "\n").getBytes(StandardCharsets.US_ASCII);
    private static final String TOKEN_PREFIX = "binding/v" + FORMAT_VERSION + "/";
    public static final int TOKEN_LENGTH = "scalebrews".length() + 1 + TOKEN_PREFIX.length() + 64;
    private CollisionBindingIdentity() {}

    /** Bounded lexical wire validation only; never hashes, serializes or certifies catalog membership. */
    public static boolean validToken(Identifier token) {
        if (token == null || !token.getNamespace().equals("scalebrews")) return false;
        var path = token.getPath();
        if (path.length() != TOKEN_PREFIX.length() + 64 || !path.startsWith(TOKEN_PREFIX)) return false;
        for (int i = TOKEN_PREFIX.length(); i < path.length(); i++) {
            char c = path.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'f')) return false;
        }
        return true;
    }

    /** Content identity only: epoch, revision, resource bytes and lifecycle ownership remain separate. */
    public static Identifier of(CollisionBinding binding) {
        Objects.requireNonNull(binding, "binding");
        var encoded = CollisionCodecs.BINDING.encodeStart(JsonOps.INSTANCE, binding).getOrThrow();
        var text = normalized(encoded).toString();
        if (text.length() > AnatomyCatalogPayload.MAX_BYTES)
            throw new IllegalArgumentException("Collision binding identity representation exceeds catalog limit");
        try {
            var bytes = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(text));
            if (bytes.remaining() > AnatomyCatalogPayload.MAX_BYTES)
                throw new IllegalArgumentException("Collision binding identity representation exceeds catalog limit");
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(DOMAIN);
            digest.update(bytes);
            return Identifier.fromNamespaceAndPath("scalebrews", TOKEN_PREFIX + HexFormat.of().formatHex(digest.digest()));
        } catch (CharacterCodingException invalid) {
            throw new IllegalArgumentException("Malformed Unicode in collision binding identity", invalid);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static JsonElement normalized(JsonElement source) {
        if (source.isJsonObject()) {
            var result = new JsonObject();
            for (var key : new TreeSet<>(source.getAsJsonObject().keySet())) {
                validUnicode(key);
                result.add(key, normalized(source.getAsJsonObject().get(key)));
            }
            return result;
        }
        if (source.isJsonArray()) {
            var result = new JsonArray();
            // Arrays retain codec order; only the canonical codecs own set ordering.
            for (var value : source.getAsJsonArray()) result.add(normalized(value));
            return result;
        }
        if (source.isJsonPrimitive() && source.getAsJsonPrimitive().isString()) validUnicode(source.getAsString());
        return source.deepCopy();
    }

    private static void validUnicode(String text) {
        for (int i = 0; i < text.length(); i++) {
            char value = text.charAt(i);
            if (Character.isHighSurrogate(value)) {
                if (++i >= text.length() || !Character.isLowSurrogate(text.charAt(i)))
                    throw new IllegalArgumentException("Malformed Unicode in collision binding identity");
            } else if (Character.isLowSurrogate(value)) {
                throw new IllegalArgumentException("Malformed Unicode in collision binding identity");
            }
        }
    }
}
