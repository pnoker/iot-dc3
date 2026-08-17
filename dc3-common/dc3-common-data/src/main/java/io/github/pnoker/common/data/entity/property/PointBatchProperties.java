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
 * RabbitMQ-to-PostgreSQL point-value batch ingestion tuning.
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

    @Min(value = 1, message = "Point batch size must be greater than 0")
    private int batchSize = 500;

    @Min(value = 1, message = "Point batch receive timeout must be greater than 0")
    private long receiveTimeoutMillis = 100;

    @Min(value = 1, message = "Point consumer count must be greater than 0")
    private int concurrentConsumers = 4;

    @Min(value = 1, message = "Point maximum consumer count must be greater than 0")
    private int maxConcurrentConsumers = 16;

    @Min(value = 1, message = "Point prefetch count must be greater than 0")
    private int prefetchCount = 1000;

    @Min(value = 0, message = "Point retry count can't be negative")
    private int maxRetries = 3;

    @Min(value = 1, message = "Point retry initial interval must be greater than 0")
    private long retryInitialIntervalMillis = 1000;

    @Min(value = 1, message = "Point retry multiplier must be at least 1")
    private int retryMultiplier = 2;

    @Min(value = 1, message = "Point retry maximum interval must be greater than 0")
    private long retryMaxIntervalMillis = 10000;
}
