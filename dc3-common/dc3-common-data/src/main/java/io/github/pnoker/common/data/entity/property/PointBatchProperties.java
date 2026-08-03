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
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Point-value ingest buffer tuning. Replaces the legacy speed/interval threshold pair: every
 * received point value enters a bounded queue and is flushed to the repository by worker
 * threads on a size-or-time trigger.
 *
 * @author pnoker
 * @version 2026.7.8
 * @since 2026.7.8
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "dc3.data.point.batch")
public class PointBatchProperties {

    @Min(value = 1, message = "Point batch queue capacity must be greater than 0")
    private int queueCapacity = 100000;

    @Min(value = 1, message = "Point batch size must be greater than 0")
    private int batchSize = 1000;

    @Min(value = 1, message = "Point batch flush interval must be greater than 0")
    private long flushIntervalMillis = 500;

    @Min(value = 1, message = "Point batch worker count must be greater than 0")
    private int workerCount = 4;
}
