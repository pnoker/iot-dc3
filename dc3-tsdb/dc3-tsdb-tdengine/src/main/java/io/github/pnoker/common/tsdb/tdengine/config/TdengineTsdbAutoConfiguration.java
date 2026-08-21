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

import com.zaxxer.hikari.HikariDataSource;
import io.github.pnoker.common.tsdb.spi.TsdbStore;
import io.github.pnoker.common.tsdb.tdengine.TdengineTsdbStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Activates the TDengine adapter when {@code dc3.tsdb.type=tdengine}. The adapter
 * builds its own pooled datasource from {@code dc3.tsdb.tdengine.*} — TDengine is
 * always an external store, there is no embedded mode to reuse.
 *
 * @author pnoker
 * @since 2026.8.21
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(TdengineTsdbProperties.class)
@ConditionalOnProperty(prefix = "dc3.tsdb", name = "type", havingValue = "tdengine")
public class TdengineTsdbAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(TsdbStore.class)
    public TsdbStore tsdbStore(TdengineTsdbProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setMaximumPoolSize(properties.getMaximumPoolSize());
        dataSource.setDriverClassName("com.taosdata.jdbc.rs.RestfulDriver");
        TsdbStore store = new TdengineTsdbStore(dataSource, properties.getDatabase());
        log.info("TSDB port negotiated, store={}, capabilities={}", store.type(), store.capabilities());
        return store;
    }
}
