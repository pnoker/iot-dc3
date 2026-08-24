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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * Activates the TimescaleDB adapter when {@code dc3.tsdb.type=timescale} (the
 * default). The adapter binds the application-provided {@code tsdbDataSource}
 * bean — the data center exposes the {@code history} entry of its dynamic
 * datasource there; standalone deployments supply a dedicated datasource under
 * the same name. The plain {@code DataSource} autowire candidate is never used
 * because a routing/primary datasource may carry a different search_path than
 * the schema the hypertable lives in.
 *
 * @author pnoker
 * @since 2026.8.20
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(DataSource.class)
@ConditionalOnBean(name = "tsdbDataSource")
@ConditionalOnProperty(prefix = "dc3.tsdb", name = "type", havingValue = "timescale", matchIfMissing = true)
public class TsdbTimescaleAutoConfiguration {

    /** The TimescaleDB adapter on the application-provided tsdbDataSource. */
    @Bean
    @ConditionalOnMissingBean(TsdbStore.class)
    public TsdbStore tsdbStore(@Qualifier("tsdbDataSource") DataSource tsdbDataSource,
                               @Value("${dc3.tsdb.timescale.rollup.minute-keep-days:365}") int minuteTierKeepDays) {
        TsdbStore store = new TimescaleTsdbStore(tsdbDataSource, minuteTierKeepDays);
        log.info("TSDB port negotiated, store={}, capabilities={}", store.type(), store.capabilities());
        return store;
    }
}
