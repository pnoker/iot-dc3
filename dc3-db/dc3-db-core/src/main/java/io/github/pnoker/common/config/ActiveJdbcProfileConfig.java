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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Active JDBC Profile Configuration
 * <p>
 * Environment post processor that activates the dialect-neutral {@code jdbc}
 * profile carrying the shared MyBatis-Plus / datasource plumbing; the dialect
 * adapters (dc3-db-postgres, later dc3-db-mysql) layer their own profile on
 * top with driver and type-handler deltas.
 *
 * @author pnoker
 * @since 2026.8.24
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ActiveJdbcProfileConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (Boolean.FALSE.equals(environment.getProperty(EnvironmentConstant.JDBC_AUTO_PROFILE, Boolean.class,
                Boolean.TRUE))) {
            log.debug("Skipping jdbc profile activation, {}=false", EnvironmentConstant.JDBC_AUTO_PROFILE);
            return;
        }
        environment.addActiveProfile(EnvironmentConstant.JDBC_PROFILE);
    }

}
