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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Tunables for the alarm-pipeline caches: rule lookup and notify policy/template
 * configurations.
 *
 * <p>Both caches use Caffeine with a hard size cap and a write-time TTL fallback.
 * Invalidation is event-driven (CRUD on the underlying entity), the TTL is purely
 * a safety net for missed invalidations during a node restart.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "dc3.alarm.cache")
public class AlarmCacheProperties {

    @Valid
    private CacheTuning rule = new CacheTuning(10_000L, 60L);

    @Valid
    private CacheTuning notify = new CacheTuning(5_000L, 60L);

    @Getter
    @Setter
    public static class CacheTuning {

        @Min(value = 1, message = "Cache max-size must be at least 1")
        private long maxSize;

        @Min(value = 1, message = "Cache ttl-seconds must be at least 1")
        private long ttlSeconds;

        public CacheTuning() {
            // Spring property binding requires a no-arg constructor.
        }

        public CacheTuning(long maxSize, long ttlSeconds) {
            this.maxSize = maxSize;
            this.ttlSeconds = ttlSeconds;
        }

    }

}
