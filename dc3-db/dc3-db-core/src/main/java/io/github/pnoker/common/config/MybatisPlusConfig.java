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

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import io.github.pnoker.common.constant.common.EnvironmentConstant;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.beans.factory.ObjectProvider;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

/**
 * MyBatis-Plus Configuration Class
 * <p>
 * Configuration class for MyBatis-Plus framework integration. Configures pagination
 * interceptors and enables transaction management for PostgreSQL database operations in
 * IoT DC3 platform.
 * </p>
 *
 * @author pnoker
 * @since 2016.10.1
 */
@AutoConfiguration
@EnableTransactionManagement
public class MybatisPlusConfig {

    /**
     * Tenant-line handler bean: fail-closed tenant id resolution.
     * <p>
     * Registered here instead of via {@code @Component} because center applications do not
     * component-scan the {@code io.github.pnoker.common.config} package, so only this
     * auto-configuration reliably loads it.
     *
     * @return a {@link TenantLineHandler} backed by the thread-local tenant context
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantLineHandler tenantLineHandler() {
        return new TenantLineHandlerImpl();
    }

    /**
     * Vendor databaseId provider — enables {@code databaseId="mysql"} statement
     * forks in mapper XML; PostgreSQL deployments resolve to {@code postgres}.
     *
     * @return vendor-mapped DatabaseIdProvider
     */
    @Bean
    @ConditionalOnMissingBean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
        java.util.Properties aliases = new java.util.Properties();
        aliases.setProperty("PostgreSQL", "postgres");
        aliases.setProperty("MySQL", "mysql");
        aliases.setProperty("MariaDB", "mariadb");
        provider.setProperties(aliases);
        return provider;
    }

    /**
     * Tenant-line before pagination; the pagination dialect comes from the selected
     * db adapter — every {@code dc3-db-*} auto-configuration contributes its
     * {@link DbType} bean. Falls back to PostgreSQL when no adapter participates
     * (default deployment) and fails fast when a non-default {@code dc3.db.type}
     * is requested without its adapter jar on the classpath.
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(TenantLineHandler tenantLineHandler,
                                                         ObjectProvider<DbType> paginationDbType,
                                                         Environment environment) {
        String selected = environment.getProperty(EnvironmentConstant.DB_TYPE);
        if (StringUtils.hasText(selected) && !"postgres".equals(selected)
                && paginationDbType.getIfAvailable() == null) {
            throw new IllegalStateException(String.format(
                    "%s=%s but no matching dc3-db adapter is on the classpath: add the dc3-db-%s "
                            + "dependency or unset the DC3_DB_TYPE variable",
                    EnvironmentConstant.DB_TYPE, selected, selected));
        }
        DbType dialect = paginationDbType.getIfUnique(() -> DbType.POSTGRE_SQL);
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // Order matters: tenant-line MUST be added before pagination.
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dialect));
        return interceptor;
    }

}
