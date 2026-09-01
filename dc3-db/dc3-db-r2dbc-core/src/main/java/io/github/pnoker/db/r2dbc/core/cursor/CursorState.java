package io.github.pnoker.db.r2dbc.core.cursor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public final class CursorState {

    public static final int QUERY_DIGEST_LENGTH = 32;

    private final String keyId;
    private final UUID tenantId;
    private final byte[] queryDigest;
    private final byte[] position;
    private final Instant expiresAt;

    public CursorState(String keyId, UUID tenantId, byte[] queryDigest, byte[] position, Instant expiresAt) {
        if (keyId == null || !keyId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("keyId must be a base64url-safe identifier");
        }
        this.keyId = keyId;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.queryDigest = copyDigest(queryDigest);
        this.position = Arrays.copyOf(Objects.requireNonNull(position, "position must not be null"), position.length);
        if (position.length == 0 || position.length > SignedCursorCodec.MAX_POSITION_BYTES) {
            throw new IllegalArgumentException("position length is invalid");
        }
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null")
                .truncatedTo(ChronoUnit.MICROS);
    }

    public String keyId() {
        return keyId;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public byte[] queryDigest() {
        return Arrays.copyOf(queryDigest, queryDigest.length);
    }

    public byte[] position() {
        return Arrays.copyOf(position, position.length);
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    static byte[] copyDigest(byte[] digest) {
        Objects.requireNonNull(digest, "queryDigest must not be null");
        if (digest.length != QUERY_DIGEST_LENGTH) {
            throw new IllegalArgumentException("queryDigest must be a SHA-256 digest");
        }
        return Arrays.copyOf(digest, digest.length);
    }
}
