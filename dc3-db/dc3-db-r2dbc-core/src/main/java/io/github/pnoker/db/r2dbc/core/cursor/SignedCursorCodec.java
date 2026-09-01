package io.github.pnoker.db.r2dbc.core.cursor;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class SignedCursorCodec {

    static final int MAX_POSITION_BYTES = 4_096;
    private static final int MAX_PAYLOAD_BYTES = 8_192;
    private static final int MAGIC = 0x44433343;
    /** Version 2 stores expiry at the platform's UTC-microsecond precision. */
    private static final int VERSION = 2;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Duration MAX_CURSOR_LIFETIME = Duration.ofDays(365);
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final Map<String, SecretKey> keys;
    private final Clock clock;

    public SignedCursorCodec(Map<String, SecretKey> keys, Clock clock) {
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("at least one cursor signing key is required");
        }
        keys.forEach((keyId, key) -> {
            if (keyId == null || !keyId.matches("[A-Za-z0-9_-]{1,64}")) {
                throw new IllegalArgumentException("cursor signing key id is invalid");
            }
            Objects.requireNonNull(key, "cursor signing key must not be null");
        });
        this.keys = Map.copyOf(keys);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public String encode(CursorState state) {
        Objects.requireNonNull(state, "state must not be null");
        SecretKey key = keys.get(state.keyId());
        if (key == null) {
            throw new IllegalArgumentException("cursor signing key does not exist");
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        if (!state.expiresAt().isAfter(now)
                || state.expiresAt().isAfter(now.plus(MAX_CURSOR_LIFETIME))) {
            throw new IllegalArgumentException("cursor expiration must be in the future");
        }
        byte[] payload = serialize(state);
        byte[] signature = sign(key, payload);
        return ENCODER.encodeToString(payload) + "." + ENCODER.encodeToString(signature);
    }

    public CursorState decode(String token, UUID expectedTenantId, byte[] expectedQueryDigest) {
        Objects.requireNonNull(expectedTenantId, "expectedTenantId must not be null");
        byte[] expectedDigest = CursorState.copyDigest(expectedQueryDigest);
        if (token == null || token.isBlank() || token.length() > 16_384) {
            throw invalidCursor();
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2) {
            throw invalidCursor();
        }

        try {
            byte[] payload = DECODER.decode(parts[0]);
            byte[] signature = DECODER.decode(parts[1]);
            if (!parts[0].equals(ENCODER.encodeToString(payload))
                    || !parts[1].equals(ENCODER.encodeToString(signature))) {
                throw invalidCursor();
            }
            if (payload.length == 0 || payload.length > MAX_PAYLOAD_BYTES) {
                throw invalidCursor();
            }
            String keyId = readKeyId(payload);
            SecretKey key = keys.get(keyId);
            if (key == null || !MessageDigest.isEqual(sign(key, payload), signature)) {
                throw invalidCursor();
            }

            CursorState state = deserialize(payload);
            Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
            if (!state.tenantId().equals(expectedTenantId)
                    || !MessageDigest.isEqual(state.queryDigest(), expectedDigest)
                    || !state.expiresAt().isAfter(now)
                    || state.expiresAt().isAfter(now.plus(MAX_CURSOR_LIFETIME))) {
                throw invalidCursor();
            }
            return state;
        } catch (RuntimeException exception) {
            if ("Invalid cursor".equals(exception.getMessage())) {
                throw exception;
            }
            throw invalidCursor();
        }
    }

    private static byte[] serialize(CursorState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeByte(VERSION);
                output.writeUTF(state.keyId());
                output.writeLong(state.tenantId().getMostSignificantBits());
                output.writeLong(state.tenantId().getLeastSignificantBits());
                long expiryMicros = Math.addExact(
                        Math.multiplyExact(state.expiresAt().getEpochSecond(), 1_000_000L),
                        state.expiresAt().getNano() / 1_000L);
                output.writeLong(expiryMicros);
                output.write(state.queryDigest());
                byte[] position = state.position();
                output.writeInt(position.length);
                output.write(position);
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode cursor", exception);
        }
    }

    private static String readKeyId(byte[] payload) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            requireHeader(input);
            String keyId = input.readUTF();
            if (!keyId.matches("[A-Za-z0-9_-]{1,64}")) {
                throw invalidCursor();
            }
            return keyId;
        } catch (IOException exception) {
            throw invalidCursor();
        }
    }

    private static CursorState deserialize(byte[] payload) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            requireHeader(input);
            String keyId = input.readUTF();
            UUID tenantId = new UUID(input.readLong(), input.readLong());
            long expiryMicros = input.readLong();
            Instant expiresAt = Instant.ofEpochSecond(
                    Math.floorDiv(expiryMicros, 1_000_000L),
                    Math.floorMod(expiryMicros, 1_000_000L) * 1_000L);
            byte[] digest = input.readNBytes(CursorState.QUERY_DIGEST_LENGTH);
            int positionLength = input.readInt();
            if (digest.length != CursorState.QUERY_DIGEST_LENGTH
                    || positionLength < 1 || positionLength > MAX_POSITION_BYTES) {
                throw invalidCursor();
            }
            byte[] position = input.readNBytes(positionLength);
            if (position.length != positionLength || input.available() != 0) {
                throw invalidCursor();
            }
            return new CursorState(keyId, tenantId, digest, position, expiresAt);
        } catch (IOException exception) {
            throw invalidCursor();
        }
    }

    private static void requireHeader(DataInputStream input) throws IOException {
        if (input.readInt() != MAGIC || input.readUnsignedByte() != VERSION) {
            throw invalidCursor();
        }
    }

    private static byte[] sign(SecretKey key, byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(key);
            return mac.doFinal(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Cursor signing is unavailable", exception);
        }
    }

    private static IllegalArgumentException invalidCursor() {
        return new IllegalArgumentException("Invalid cursor");
    }
}
