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

package io.github.pnoker.common.tsdb.iotdb.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings of the IoTDB adapter — session API against an external
 * node running with {@code timestamp_precision=us}.
 *
 * @author pnoker
 * @since 2026.8.21
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "dc3.tsdb.iotdb")
public class IotdbTsdbProperties {

    private String host = "localhost";

    private int port = 6667;

    private String username = "root";

    private String password = "root";

    /**
     * Raw retention in days applied to the whole {@code root.dc3} subtree via
     * {@code SET TTL TO}.
     */
    private int ttlDays = 180;
}
