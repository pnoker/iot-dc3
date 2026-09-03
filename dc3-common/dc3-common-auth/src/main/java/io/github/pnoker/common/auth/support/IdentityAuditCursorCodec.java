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
package io.github.pnoker.common.auth.support;

import io.github.pnoker.db.r2dbc.core.cursor.CursorState;
import io.github.pnoker.db.r2dbc.core.cursor.SignedCursorCodec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;

/** Signs identity-audit cursors and binds them to tenant and exact filters. */
public final class IdentityAuditCursorCodec {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String KEY_ID = "identity-audit-v1";
    private static final Duration TTL = Duration.ofMinutes(15);

    private final SignedCursorCodec delegate;
    private final Clock clock;

    public IdentityAuditCursorCodec(String secret, Clock clock) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("identity-audit cursor signing secret is required");
        }
        this.clock = clock;
        this.delegate = new SignedCursorCodec(
                Map.of(KEY_ID, new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM)), clock);
    }

    /** Encode a signed cursor for the tenant and query fingerprint. */
    public String encode(long tenantId, String queryFingerprint, Instant time, long id) {
        validateTenant(tenantId);
        if (time == null || id <= 0) throw invalid();
        Instant expiry = clock.instant().truncatedTo(ChronoUnit.MICROS).plus(TTL);
        return delegate.encode(
                new CursorState(KEY_ID, tenantUuid(tenantId), digest(queryFingerprint), position(time, id), expiry));
    }

    /** Decode and verify a cursor token for the tenant and query fingerprint. */
    public Position decode(String token, long tenantId, String queryFingerprint) {
        validateTenant(tenantId);
        try {
            CursorState state = delegate.decode(token, tenantUuid(tenantId), digest(queryFingerprint));
            return readPosition(state.position());
        } catch (RuntimeException error) {
            throw invalid();
        }
    }

    private static byte[] position(Instant time, long id) {
        try {
            Instant instant = time.truncatedTo(ChronoUnit.MICROS);
            long micros =
                    Math.addExact(Math.multiplyExact(instant.getEpochSecond(), 1_000_000L), instant.getNano() / 1_000L);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(16);
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeLong(micros);
                output.writeLong(id);
            }
            return bytes.toByteArray();
        } catch (IOException | ArithmeticException error) {
            throw invalid();
        }
    }

    private static Position readPosition(byte[] value) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(value))) {
            long micros = input.readLong();
            long id = input.readLong();
            if (id <= 0 || input.available() != 0) throw invalid();
            Instant instant = Instant.ofEpochSecond(
                    Math.floorDiv(micros, 1_000_000L), Math.floorMod(micros, 1_000_000L) * 1_000L);
            return new Position(instant, id);
        } catch (IOException | RuntimeException error) {
            throw invalid();
        }
    }

    private static byte[] digest(String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()) throw invalid();
        try {
            return MessageDigest.getInstance("SHA-256").digest(fingerprint.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private static UUID tenantUuid(long tenantId) {
        return new UUID(0, tenantId);
    }

    private static void validateTenant(long tenantId) {
        if (tenantId <= 0) throw invalid();
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Invalid identity audit cursor");
    }

    public record Position(Instant time, long id) {}
}
