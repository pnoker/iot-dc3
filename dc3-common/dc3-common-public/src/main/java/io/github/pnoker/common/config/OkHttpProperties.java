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

package io.github.pnoker.common.config;

import io.github.pnoker.common.constant.common.EnvironmentConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Shared OkHttp client properties.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = EnvironmentConstant.HTTP_CLIENT_PREFIX)
public class OkHttpProperties {

    /**
     * Whether OkHttp should retry recoverable connection failures.
     */
    private boolean retryOnConnectionFailure = true;

    /**
     * Maximum idle connections retained by the shared connection pool.
     */
    private int maxIdleConnections = 16;

    /**
     * How long idle connections stay in the pool.
     */
    private Duration keepAliveDuration = Duration.ofSeconds(5);

    /**
     * End-to-end call timeout.
     */
    private Duration callTimeout = Duration.ofSeconds(15);

    /**
     * TCP connection timeout.
     */
    private Duration connectTimeout = Duration.ofSeconds(15);

    /**
     * Socket read timeout.
     */
    private Duration readTimeout = Duration.ofSeconds(15);

    /**
     * Socket write timeout.
     */
    private Duration writeTimeout = Duration.ofSeconds(15);

}
