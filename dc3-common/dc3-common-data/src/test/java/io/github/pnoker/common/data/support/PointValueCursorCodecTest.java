package io.github.pnoker.common.data.support;

import io.github.pnoker.common.tsdb.model.TsdbModel.Cursor;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointValueCursorCodecTest {
    private final PointValueCursorCodec codec = new PointValueCursorCodec("test-secret", Clock.systemUTC());

    @Test
    void roundTripPreservesScopeAndMicrosecondPosition() {
        Cursor original = new Cursor(Instant.parse("2026-08-30T12:34:56.123456Z"), "message-42",
                new SeriesKey(7L, 8L, 9L));
        String token = codec.encodeCursor(7L, 8L, 9L, original);
        assertThat(codec.decodeCursor(token, 7L, 8L, 9L)).isEqualTo(original);
    }

    @Test
    void rejectsWrongScopeAndTampering() {
        String token = codec.encodeCursor(7L, 8L, 9L,
                new Cursor(Instant.parse("2026-08-30T12:34:56Z"), "message-42",
                        new SeriesKey(7L, 8L, 9L)));
        assertThatThrownBy(() -> codec.decodeCursor(token, 7L, 8L, 10L))
                .hasMessage("Invalid history cursor");
        int pivot = token.length() / 2;
        char replacement = token.charAt(pivot) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, pivot) + replacement + token.substring(pivot + 1);
        assertThatThrownBy(() -> codec.decodeCursor(tampered, 7L, 8L, 9L))
                .hasMessage("Invalid history cursor");
    }

    @Test
    void rejectsQueryMismatchAndExpiry() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC);
        PointValueCursorCodec codec = new PointValueCursorCodec("cursor-secret", clock);
        String token = codec.encode(7L, "tenant=7;series=8:9;sort=create_time.desc,tenant_id.desc,device_id.desc,point_id.desc,message_id.desc",
                new Cursor(Instant.parse("2026-08-30T12:34:56Z"), "message-42",
                        new SeriesKey(7L, 8L, 9L)));
        assertThatThrownBy(() -> codec.decode(token, 7L, "tenant=7;series=8:10;sort=create_time.desc,tenant_id.desc,device_id.desc,point_id.desc,message_id.desc"))
                .hasMessage("Invalid history cursor");
        assertThat(codec.decode(token, 7L, "tenant=7;series=8:9;sort=create_time.desc,tenant_id.desc,device_id.desc,point_id.desc,message_id.desc").messageId())
                .isEqualTo("message-42");
        PointValueCursorCodec expired = new PointValueCursorCodec("cursor-secret",
                Clock.offset(clock, Duration.ofMinutes(16)));
        assertThatThrownBy(() -> expired.decode(token, 7L, "tenant=7;series=8:9;sort=create_time.desc,tenant_id.desc,device_id.desc,point_id.desc,message_id.desc"))
                .hasMessage("Invalid history cursor");
    }

    @Test
    void roundTripPreservesSeriesTieBreakAndSnapshotWindow() {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-30T00:00:00Z");
        Cursor original = new Cursor(Instant.parse("2026-08-20T12:34:56.123456Z"), "message-42",
                new SeriesKey(7L, 8L, 9L), from, to);
        String fingerprint = "tenant=7;series=8:9;rangeKey=24h;rangeHours=0;from=;sort=create_time.desc,tenant_id.desc,device_id.desc,point_id.desc,message_id.desc";

        String token = codec.encode(7L, fingerprint, original);

        assertThat(codec.decode(token, 7L, fingerprint)).isEqualTo(original);
    }

    @Test
    void rejectsInvalidSnapshotWindow() {
        assertThatThrownBy(() -> new Cursor(Instant.parse("2026-08-20T12:34:56Z"), "message-42",
                new SeriesKey(7L, 8L, 9L), Instant.parse("2026-08-30T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z")))
                .hasMessage("cursor window must be a valid half-open range");
    }
}
