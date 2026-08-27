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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * Restores transaction management over the dynamic routing datasource.
 *
 * <p>{@link TsdbDataSourceConfig} publishes a second {@code DataSource} bean, so
 * the context no longer satisfies Spring Boot's
 * {@code @ConditionalOnSingleCandidate(DataSource.class)} and
 * {@code DataSourceTransactionManagerAutoConfiguration} backs off silently:
 * no {@code PlatformTransactionManager}, hence no auto-configured
 * {@code TransactionTemplate} for beans that need programmatic transactions.
 * Declaring the manager explicitly closes that gap.
 *
 * <p>The manager wraps the routing datasource itself. A transaction binds the
 * connection of the route current when it starts and keeps it for its whole
 * scope, which is the local-transaction semantics dynamic-datasource documents:
 * datasource switching inside an open transaction does not take effect.
 *
 * @author pnoker
 * @since 2026.8.27
 */
@Configuration
public class TransactionConfig {

    /**
     * Transaction manager bound to the primary dynamic routing datasource.
     *
     * @param dataSource the routing {@code dataSource} bean (master route at transaction start)
     * @return a datasource-scoped transaction manager
     */
    @Bean
    public DataSourceTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
