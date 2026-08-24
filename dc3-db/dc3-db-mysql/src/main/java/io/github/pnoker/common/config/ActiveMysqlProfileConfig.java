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
 * Active MySQL Profile Configuration
 * <p>
 * Activates the {@code mysql} profile (driver + connection conventions) only
 * when {@code dc3.db.type=mysql} — the PostgreSQL profile stays the default
 * for unset values, so existing deployments are untouched.
 *
 * @author pnoker
 * @since 2026.8.24
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ActiveMysqlProfileConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbType = environment.getProperty(EnvironmentConstant.DB_TYPE);
        if (!"mysql".equals(dbType)) {
            return;
        }
        environment.addActiveProfile(EnvironmentConstant.MYSQL_PROFILE);
        log.info("Relational dialect profile activated: mysql");
    }

}
