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

package io.github.pnoker.common.tsdb.timescale.config;

import io.github.pnoker.common.tsdb.spi.TsdbStore;
import io.github.pnoker.common.tsdb.timescale.TimescaleTsdbStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * Activates the TimescaleDB adapter when {@code dc3.tsdb.type=timescale} (the
 * default). Embedded deployments pass the primary PostgreSQL datasource; standalone
 * deployments bind a dedicated one.
 *
 * @author pnoker
 * @since 2026.8.20
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(prefix = "dc3.tsdb", name = "type", havingValue = "timescale", matchIfMissing = true)
public class TsdbTimescaleAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TsdbStore.class)
    public TsdbStore tsdbStore(DataSource dataSource) {
        TsdbStore store = new TimescaleTsdbStore(dataSource);
        log.info("TSDB port negotiated, store={}, capabilities={}", store.type(), store.capabilities());
        return store;
    }
}
