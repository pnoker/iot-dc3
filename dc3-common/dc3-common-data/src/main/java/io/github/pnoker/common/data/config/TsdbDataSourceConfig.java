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

package io.github.pnoker.common.data.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Binds the connection the TSDB port adapter runs on. The hypertable lives in
 * the {@code dc3_history} schema, reached through the dynamic datasource's
 * {@code history} entry — handing the adapter the routing (primary) datasource
 * instead would resolve its unqualified table names against the wrong
 * search_path and every statement would fail.
 *
 * @author pnoker
 * @since 2026.8.20
 */
@Slf4j
@Configuration
public class TsdbDataSourceConfig {

    /**
     * Resolve the datasource the TSDB port adapter runs on.
     *
     * <p>With dynamic routing, returns the {@code history} entry so unqualified table names resolve against the
     * {@code dc3_history} schema; otherwise returns the single application datasource unchanged.
     *
     * @param dataSource the primary application datasource
     * @return the history-scoped datasource when routing is active, the primary datasource otherwise
     */
    @Bean
    public DataSource tsdbDataSource(DataSource dataSource) {
        if (dataSource instanceof DynamicRoutingDataSource routing) {
            DataSource history = routing.getDataSource("history");
            log.info("TSDB datasource bound to the dynamic 'history' entry ({})", history.getClass().getSimpleName());
            return history;
        }
        log.info("TSDB datasource bound to the single application datasource");
        return dataSource;
    }
}
