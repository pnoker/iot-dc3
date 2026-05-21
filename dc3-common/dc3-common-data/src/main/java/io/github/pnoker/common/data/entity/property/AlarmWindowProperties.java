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

package io.github.pnoker.common.data.entity.property;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Tunables for the in-memory window sample buffer used by short-window alarm
 * evaluation. Long-window evaluation (durations larger than {@link #localCutoff})
 * routes to the time-series repository instead.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "dc3.alarm.window")
public class AlarmWindowProperties {

    /**
     * Boundary between local-buffer and repository window evaluation. Rules
     * with {@code duration <= localCutoff} read samples from memory; longer
     * windows go to PostgreSQL.
     */
    @NotNull
    private Duration localCutoff = Duration.ofMinutes(5);

    /**
     * Maximum number of distinct {@code (tenantId, targetType, entityId)}
     * keys the buffer holds. Caffeine evicts least-recently-accessed keys
     * when the cap is reached.
     */
    @Min(1)
    private long maxBufferKeys = 10_000L;

    /**
     * Maximum samples retained per buffer key. The buffer is also bounded
     * by retention time, but on a high-frequency point this size cap fires
     * first and trims the oldest sample.
     */
    @Min(1)
    private int maxSamplesPerBuffer = 1_000;

    /**
     * Drop a buffer that has not been accessed for this long. Defaults to
     * 30 minutes — enough to absorb temporary stalls without leaking memory
     * for entities that no longer ingest.
     */
    @NotNull
    private Duration bufferIdleExpiry = Duration.ofMinutes(30);

}
