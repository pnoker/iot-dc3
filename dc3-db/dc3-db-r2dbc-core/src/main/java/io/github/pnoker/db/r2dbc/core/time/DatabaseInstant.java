package io.github.pnoker.db.r2dbc.core.time;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class DatabaseInstant {

    private DatabaseInstant() {
    }

    public static Instant normalize(Instant instant) {
        return Objects.requireNonNull(instant, "instant must not be null")
                .truncatedTo(ChronoUnit.MICROS);
    }
}
