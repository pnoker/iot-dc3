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
package io.github.pnoker.db.r2dbc.core.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class DatabaseInstantTest {

    @Test
    void truncatesToTheCrossDialectMicrosecondPrecision() {
        assertEquals(
                Instant.parse("2026-08-28T01:02:03.123456Z"),
                DatabaseInstant.normalize(Instant.parse("2026-08-28T01:02:03.123456789Z")));
    }

    @Test
    void decodesLocalDateTimeAsIs() {
        LocalDateTime value = LocalDateTime.of(2026, 9, 19, 12, 0);
        assertEquals(value, DatabaseInstant.toLocalDateTimeUtc(value));
    }

    @Test
    void decodesOffsetDateTimeToUtc() {
        OffsetDateTime value = OffsetDateTime.of(2026, 9, 19, 14, 0, 0, 0, ZoneOffset.ofHours(2));
        assertEquals(LocalDateTime.of(2026, 9, 19, 12, 0), DatabaseInstant.toLocalDateTimeUtc(value));
    }

    @Test
    void decodesInstantToUtc() {
        assertEquals(
                LocalDateTime.of(2026, 9, 19, 12, 0),
                DatabaseInstant.toLocalDateTimeUtc(Instant.parse("2026-09-19T12:00:00Z")));
    }

    @Test
    void keepsNullAsNull() {
        assertNull(DatabaseInstant.toLocalDateTimeUtc(null));
    }

    @Test
    void failsFastOnUnsupportedTimestampType() {
        IllegalStateException error =
                assertThrows(IllegalStateException.class, () -> DatabaseInstant.toLocalDateTimeUtc("2026-09-19"));
        assertEquals("unsupported timestamp type: java.lang.String", error.getMessage());
    }
}
