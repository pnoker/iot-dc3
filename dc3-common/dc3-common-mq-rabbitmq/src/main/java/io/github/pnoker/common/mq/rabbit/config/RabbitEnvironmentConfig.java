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
import io.github.pnoker.common.utils.EnvironmentUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Stores the environment/group tag as the {@code dc3.rabbit.tag} system property so
 * {@code RabbitNames} can prefix physical names during class initialization. Moved
 * unchanged from the pre-port rabbitmq module; runs before any application context
 * creation.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@Slf4j
@Order
public class RabbitEnvironmentConfig implements EnvironmentPostProcessor {

    /**
     * System property key carrying the environment/group tag.
     */
    public static final String DC3_RABBIT_TAG = "dc3.rabbit.tag";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String env = environment.getProperty(EnvironmentConstant.SPRING_ENV, String.class);
        String group = environment.getProperty(EnvironmentConstant.SPRING_GROUP, String.class);
        String tag = EnvironmentUtil.getTag(env, group);
        System.setProperty(DC3_RABBIT_TAG, tag);
        log.info("RabbitMQ environment configured, tag={}", tag);
    }
}
