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
package io.github.pnoker.common.data.support;

import io.github.pnoker.common.tsdb.model.TsdbModel.Cursor;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
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
import java.util.Objects;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;

/** Signs point-value cursors and binds them to tenant and query scope. */
public final class PointValueCursorCodec {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String KEY_ID = "point-value-v1";
    private static final Duration CURSOR_TTL = Duration.ofMinutes(15);
    private final SignedCursorCodec delegate;
    private final Clock clock;

    public PointValueCursorCodec(String secret, Clock clock) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("point-value cursor signing secret is required");
        }
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.delegate = new SignedCursorCodec(
                Map.of(KEY_ID, new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM)), clock);
    }

    public String encodeCursor(long tenantId, long deviceId, long pointId, Cursor cursor) {
        if (cursor == null
                || cursor.series() == null
                || !cursor.series().equals(new SeriesKey(tenantId, deviceId, pointId))) {
            throw invalid();
        }
        return encode(tenantId, scopeFingerprint(tenantId, deviceId, pointId), cursor);
    }

    public Cursor decodeCursor(String token, long tenantId, long deviceId, long pointId) {
        Cursor cursor = decode(token, tenantId, scopeFingerprint(tenantId, deviceId, pointId));
        if (cursor.series() != null && !cursor.series().equals(new SeriesKey(tenantId, deviceId, pointId))) {
            throw invalid();
        }
        return cursor;
    }

    public String encode(long tenantId, String queryFingerprint, Cursor cursor) {
        validateScope(tenantId, queryFingerprint);
        if (cursor == null
                || cursor.deviceTime() == null
                || cursor.messageId() == null
                || cursor.messageId().isBlank()) {
            throw new IllegalArgumentException("Invalid history cursor state");
        }
        if (cursor.series() == null) {
            throw new IllegalArgumentException("History cursor series is required");
        }
        byte[] position = serializePosition(cursor);
        Instant expiresAt = clock.instant().truncatedTo(ChronoUnit.MICROS).plus(CURSOR_TTL);
        return delegate.encode(
                new CursorState(KEY_ID, tenantUuid(tenantId), digest(queryFingerprint), position, expiresAt));
    }

    public Cursor decode(String token, long tenantId, String queryFingerprint) {
        validateScope(tenantId, queryFingerprint);
        try {
            CursorState state = delegate.decode(token, tenantUuid(tenantId), digest(queryFingerprint));
            return deserializePosition(state.position());
        } catch (RuntimeException error) {
            throw invalid();
        }
    }

    private static String scopeFingerprint(long tenantId, long deviceId, long pointId) {
        if (tenantId <= 0 || deviceId <= 0 || pointId <= 0) throw invalid();
        return "tenant=" + tenantId + ";device=" + deviceId + ";point=" + pointId
                + ";sort=create_time.desc,tenant_id.desc,device_id.desc,point_id.desc,message_id.desc";
    }

    public static String normalizeFingerprint(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("cursor query fingerprint is required");
        }
        return query;
    }

    private static byte[] digest(String queryFingerprint) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(queryFingerprint.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private static byte[] serializePosition(Cursor cursor) {
        try {
            byte[] messageId = cursor.messageId().getBytes(StandardCharsets.UTF_8);
            if (messageId.length == 0 || messageId.length > 1024) {
                throw new IllegalArgumentException("History cursor message id is too long");
            }
            Instant timestamp = cursor.deviceTime().truncatedTo(ChronoUnit.MICROS);
            long epochMicros = Math.addExact(
                    Math.multiplyExact(timestamp.getEpochSecond(), 1_000_000L), timestamp.getNano() / 1_000L);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeLong(epochMicros);
                output.writeInt(messageId.length);
                output.write(messageId);
                SeriesKey series = cursor.series();
                output.writeBoolean(series != null);
                if (series != null) {
                    output.writeLong(series.tenantId());
                    output.writeLong(series.deviceId());
                    output.writeLong(series.pointId());
                }
                boolean hasWindow = cursor.windowFrom() != null || cursor.windowTo() != null;
                if (hasWindow
                        && (cursor.windowFrom() == null
                                || cursor.windowTo() == null
                                || !cursor.windowFrom().isBefore(cursor.windowTo()))) {
                    throw new IllegalArgumentException("Invalid history cursor window");
                }
                output.writeBoolean(hasWindow);
                if (hasWindow) {
                    output.writeLong(epochMicros(cursor.windowFrom()));
                    output.writeLong(epochMicros(cursor.windowTo()));
                }
            }
            return bytes.toByteArray();
        } catch (IOException | ArithmeticException error) {
            throw new IllegalArgumentException("Invalid history cursor state", error);
        }
    }

    private static Cursor deserializePosition(byte[] position) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(position))) {
            long epochMicros = input.readLong();
            int length = input.readInt();
            if (length < 1 || length > 1024) throw invalid();
            byte[] message = input.readNBytes(length);
            if (message.length != length) throw invalid();
            Instant timestamp = Instant.ofEpochSecond(
                    Math.floorDiv(epochMicros, 1_000_000L), Math.floorMod(epochMicros, 1_000_000L) * 1_000L);
            String messageId = new String(message, StandardCharsets.UTF_8);
            if (messageId.isBlank()) throw invalid();
            SeriesKey series = null;
            if (input.readBoolean()) {
                long tenantId = input.readLong();
                long deviceId = input.readLong();
                long pointId = input.readLong();
                if (tenantId <= 0 || deviceId <= 0 || pointId <= 0) throw invalid();
                series = new SeriesKey(tenantId, deviceId, pointId);
            }
            Instant windowFrom = null;
            Instant windowTo = null;
            if (input.readBoolean()) {
                windowFrom = instantOfMicros(input.readLong());
                windowTo = instantOfMicros(input.readLong());
                if (!windowFrom.isBefore(windowTo)) throw invalid();
            }
            if (input.available() != 0) throw invalid();
            return new Cursor(timestamp, messageId, series, windowFrom, windowTo);
        } catch (IOException | RuntimeException error) {
            throw invalid();
        }
    }

    private static long epochMicros(Instant timestamp) {
        Instant truncated = timestamp.truncatedTo(ChronoUnit.MICROS);
        return Math.addExact(Math.multiplyExact(truncated.getEpochSecond(), 1_000_000L), truncated.getNano() / 1_000L);
    }

    private static Instant instantOfMicros(long epochMicros) {
        return Instant.ofEpochSecond(
                Math.floorDiv(epochMicros, 1_000_000L), Math.floorMod(epochMicros, 1_000_000L) * 1_000L);
    }

    private static UUID tenantUuid(long tenantId) {
        if (tenantId <= 0) throw invalid();
        return new UUID(0, tenantId);
    }

    private static void validateScope(long tenantId, String queryFingerprint) {
        tenantUuid(tenantId);
        normalizeFingerprint(queryFingerprint);
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Invalid history cursor");
    }
}
