/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.pnoker.db.r2dbc.core.cursor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class SignedCursorCodecTest {

    private static final Instant NOW = Instant.parse("2026-08-28T00:00:00Z");
    private static final UUID TENANT_ID = UUID.fromString("0198f1d4-3400-7000-8000-000000000001");
    private static final byte[] QUERY_DIGEST = sha256("point-history:device=1");

    private final SignedCursorCodec codec = new SignedCursorCodec(
            Map.of("k1", new SecretKeySpec(new byte[32], "HmacSHA256")), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void roundTripsAnAuthenticatedTenantAndQueryBoundCursor() {
        byte[] position = "2026-08-28T00:00:00Z|0198f1d4".getBytes(StandardCharsets.UTF_8);
        String token = codec.encode(new CursorState("k1", TENANT_ID, QUERY_DIGEST, position, NOW.plusSeconds(300)));

        CursorState decoded = codec.decode(token, TENANT_ID, QUERY_DIGEST);

        assertArrayEquals(position, decoded.position());
    }

    @Test
    void preservesTheCanonicalMicrosecondExpiryPrecision() {
        Instant expiry = NOW.plusSeconds(300).plusNanos(123_456_789);
        String token = codec.encode(new CursorState("k1", TENANT_ID, QUERY_DIGEST, new byte[] {1}, expiry));

        CursorState decoded = codec.decode(token, TENANT_ID, QUERY_DIGEST);

        org.junit.jupiter.api.Assertions.assertEquals(
                expiry.truncatedTo(java.time.temporal.ChronoUnit.MICROS), decoded.expiresAt());
    }

    @Test
    void rejectsTamperingCrossTenantReuseQueryReuseAndExpiry() {
        String token = codec.encode(new CursorState("k1", TENANT_ID, QUERY_DIGEST, new byte[] {1}, NOW.plusSeconds(1)));
        String tampered = (token.charAt(0) == 'A' ? "B" : "A") + token.substring(1);

        assertThrows(IllegalArgumentException.class, () -> codec.decode(tampered, TENANT_ID, QUERY_DIGEST));
        assertThrows(IllegalArgumentException.class, () -> codec.decode(token, UUID.randomUUID(), QUERY_DIGEST));
        assertThrows(IllegalArgumentException.class, () -> codec.decode(token, TENANT_ID, sha256("another query")));

        SignedCursorCodec expiredCodec = new SignedCursorCodec(
                Map.of("k1", new SecretKeySpec(new byte[32], "HmacSHA256")),
                Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC));
        assertThrows(IllegalArgumentException.class, () -> expiredCodec.decode(token, TENANT_ID, QUERY_DIGEST));
    }

    @Test
    void mapsMalformedExpiryPayloadsToInvalidCursor() {
        String token =
                codec.encode(new CursorState("k1", TENANT_ID, QUERY_DIGEST, new byte[] {1}, NOW.plusSeconds(300)));
        String[] parts = token.split("\\.", -1);
        byte[] payload = java.util.Base64.getUrlDecoder().decode(parts[0]);
        // The expiry field starts after the fixed header, UTF key and UUID.
        int expiryOffset = 4 + 1 + 2 + 2 + 16;
        java.nio.ByteBuffer.wrap(payload).putLong(expiryOffset, Long.MAX_VALUE);
        javax.crypto.spec.SecretKeySpec key = new javax.crypto.spec.SecretKeySpec(new byte[32], "HmacSHA256");
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(key);
            String malformed = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload) + "."
                    + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload));
            assertThrows(IllegalArgumentException.class, () -> codec.decode(malformed, TENANT_ID, QUERY_DIGEST));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    @Test
    void rejectsNonCanonicalBase64Representations() {
        String token =
                codec.encode(new CursorState("k1", TENANT_ID, QUERY_DIGEST, new byte[] {1}, NOW.plusSeconds(300)));
        String[] parts = token.split("\\.", -1);
        String padded = parts[0] + "=";

        assertThrows(
                IllegalArgumentException.class, () -> codec.decode(padded + "." + parts[1], TENANT_ID, QUERY_DIGEST));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
