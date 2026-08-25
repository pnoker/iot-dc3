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

package io.github.pnoker.common.tsdb.tdengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings of the TDengine adapter — an external store, so unlike the
 * embedded timescale adapter it owns its datasource.
 *
 * @author pnoker
 * @since 2026.8.21
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "dc3.tsdb.tdengine")
public class TdengineTsdbProperties {

    /**
     * REST/WS JDBC url without a database segment, e.g. {@code jdbc:TAOS-RS://dc3-tdengine:6041/}.
     */
    private String url = "jdbc:TAOS-RS://localhost:6041/";

    private String username = "root";

    private String password = "taosdata";

    /**
     * Database the adapter creates and owns (PRECISION 'us').
     */
    private String database = "dc3";

    private int maximumPoolSize = 8;
}
