package io.github.pnoker.db.r2dbc.core.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseInstantTest {

    @Test
    void truncatesToTheCrossDialectMicrosecondPrecision() {
        assertEquals(
                Instant.parse("2026-08-28T01:02:03.123456Z"),
                DatabaseInstant.normalize(Instant.parse("2026-08-28T01:02:03.123456789Z")));
    }
}
