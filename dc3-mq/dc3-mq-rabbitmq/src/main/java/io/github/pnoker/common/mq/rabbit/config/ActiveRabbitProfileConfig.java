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

package io.github.pnoker.common.mq.rabbit.config;

import io.github.pnoker.common.constant.common.EnvironmentConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Adds the {@code rabbitmq} active profile (unless disabled), preserving the pre-port
 * profile behavior for deployments that carry {@code application-rabbitmq.yml} files.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ActiveRabbitProfileConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (Boolean.FALSE.equals(environment.getProperty(EnvironmentConstant.RABBITMQ_AUTO_PROFILE, Boolean.class,
                Boolean.TRUE))) {
            log.debug("Skipping rabbitmq profile activation, {}=false", EnvironmentConstant.RABBITMQ_AUTO_PROFILE);
            return;
        }
        environment.addActiveProfile(EnvironmentConstant.RABBITMQ_PROFILE);
    }
}
