package io.github.pnoker.common.auth.support;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityAuditCursorCodecTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-31T00:00:00Z"), ZoneOffset.UTC);
    private final IdentityAuditCursorCodec codec = new IdentityAuditCursorCodec("audit-secret", clock);

    @Test
    void roundTripsPositionForSameTenantAndFilters() {
        String fingerprint = "tenant=7;status=SUCCESS;sort=create_time.desc,id.desc";
        String token = codec.encode(7L, fingerprint, Instant.parse("2026-08-30T01:02:03.123456Z"), 11L);

        IdentityAuditCursorCodec.Position position = codec.decode(token, 7L, fingerprint);

        assertThat(position.time()).isEqualTo(Instant.parse("2026-08-30T01:02:03.123456Z"));
        assertThat(position.id()).isEqualTo(11L);
    }

    @Test
    void rejectsTenantOrFilterReuse() {
        String fingerprint = "tenant=7;status=SUCCESS;sort=create_time.desc,id.desc";
        String token = codec.encode(7L, fingerprint, Instant.parse("2026-08-30T01:02:00Z"), 11L);

        assertThatThrownBy(() -> codec.decode(token, 8L, fingerprint)).hasMessage("Invalid identity audit cursor");
        assertThatThrownBy(() -> codec.decode(token, 7L, fingerprint + ";changed")).hasMessage("Invalid identity audit cursor");
    }
}
