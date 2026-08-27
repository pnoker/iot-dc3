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
import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies the datasource wiring contract of {@link TsdbDataSourceConfig}: qualifier-less
 * by-type consumers (MyBatis-Plus, Quartz, health checks) must keep resolving the single
 * routing datasource, while the TSDB adapter resolves the {@code history} entry through
 * its explicit qualifier.
 *
 * @author pnoker
 * @since 2026.8.27
 */
class TsdbDataSourceConfigTest {

    @Configuration(proxyBeanMethods = false)
    static class RoutingConfig {

        @Bean
        public DataSource dataSource() {
            DynamicDataSourceProvider provider = () -> Map.of(
                    "master", mock(DataSource.class),
                    "history", mock(DataSource.class));
            DynamicRoutingDataSource routing = new DynamicRoutingDataSource(List.of(provider));
            routing.setPrimary("master");
            routing.afterPropertiesSet();
            return routing;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class SingleDatasourceConfig {

        @Bean
        public DataSource dataSource() {
            return mock(DataSource.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ConsumerConfig {

        @Bean
        public ByTypeConsumer byTypeConsumer(DataSource dataSource) {
            return new ByTypeConsumer(dataSource);
        }

        @Bean
        public QualifiedConsumer qualifiedConsumer(@Qualifier("tsdbDataSource") DataSource tsdbDataSource) {
            return new QualifiedConsumer(tsdbDataSource);
        }
    }

    record ByTypeConsumer(DataSource resolved) {
    }

    record QualifiedConsumer(DataSource resolved) {
    }

    @Test
    void byTypeInjectionResolvesTheRoutingDatasourceOnly() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                RoutingConfig.class, ConsumerConfig.class, TsdbDataSourceConfig.class)) {
            DataSource routing = context.getBean("dataSource", DataSource.class);
            assertThat(context.getBean(ByTypeConsumer.class).resolved()).isSameAs(routing);
        }
    }

    @Test
    void qualifiedInjectionBindsTheHistoryEntry() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                RoutingConfig.class, ConsumerConfig.class, TsdbDataSourceConfig.class)) {
            DynamicRoutingDataSource routing = (DynamicRoutingDataSource) context.getBean("dataSource");
            assertThat(context.getBean(QualifiedConsumer.class).resolved())
                    .isSameAs(routing.getDataSource("history"))
                    .isNotSameAs(routing);
        }
    }

    @Test
    void transactionManagerWrapsTheRoutingDatasource() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                RoutingConfig.class, TransactionConfig.class)) {
            DataSource routing = context.getBean("dataSource", DataSource.class);
            PlatformTransactionManager manager = context.getBean(PlatformTransactionManager.class);
            assertThat(manager).isNotNull();
            assertThat(((DataSourceTransactionManager) manager).getDataSource()).isSameAs(routing);
        }
    }

    @Test
    void singleDatasourceContextReusesItForTsdb() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                SingleDatasourceConfig.class, ConsumerConfig.class, TsdbDataSourceConfig.class)) {
            DataSource single = context.getBean("dataSource", DataSource.class);
            assertThat(context.getBean(ByTypeConsumer.class).resolved()).isSameAs(single);
            assertThat(context.getBean(QualifiedConsumer.class).resolved()).isSameAs(single);
        }
    }
}
