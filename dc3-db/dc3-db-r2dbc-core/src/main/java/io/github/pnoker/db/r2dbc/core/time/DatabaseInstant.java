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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** UTC microsecond instant normalization shared by persistence layers. */
public final class DatabaseInstant {

    private DatabaseInstant() {}

    /** Normalize the instant to UTC microseconds. */
    public static Instant normalize(Instant instant) {
        return Objects.requireNonNull(instant, "instant must not be null").truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * Decode a driver timestamp column into a UTC {@link LocalDateTime}.
     * Null stays null; any non-null value of an unexpected type fails fast instead of
     * degrading to null, so a broken decode is observable at the call site.
     */
    public static LocalDateTime toLocalDateTimeUtc(Object raw) {
        if (raw == null) return null;
        if (raw instanceof LocalDateTime value) return value;
        if (raw instanceof OffsetDateTime value)
            return value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (raw instanceof Instant value) return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
        throw new IllegalStateException("unsupported timestamp type: " + raw.getClass().getName());
    }
}
