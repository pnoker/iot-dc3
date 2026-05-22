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
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.utils.EnvironmentUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * RabbitMQ Environment Configuration
 * <p>
 * Environment post processor for configuring RabbitMQ constants based on development
 * environment and group tags. Used for multi-developer scenarios where different
 * exchanges, queues, and topics need to be isolated by environment and group identifiers.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Order
public class RabbitmqEnvironmentConfig implements EnvironmentPostProcessor {

    /**
     * Post-process environment to configure RabbitMQ constants
     *
     * @param environment ConfigurableEnvironment to modify
     * @param application SpringApplication instance
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // This configuration is used for multi-developer scenarios,
        // distinguishing different exchanges, queues, and topics based on env and group
        // identifiers
        String env = environment.getProperty(EnvironmentConstant.SPRING_ENV, String.class);
        String group = environment.getProperty(EnvironmentConstant.SPRING_GROUP, String.class);

        String tag = EnvironmentUtil.getTag(env, group);

        // Sync registration related constants
        RabbitConstant.TOPIC_EXCHANGE_REGISTER = tag + RabbitConstant.TOPIC_EXCHANGE_REGISTER;
        RabbitConstant.QUEUE_REGISTER_UP = tag + RabbitConstant.QUEUE_REGISTER_UP;
        RabbitConstant.QUEUE_REGISTER_DOWN_PREFIX = tag + RabbitConstant.QUEUE_REGISTER_DOWN_PREFIX;

        // State related constants
        RabbitConstant.TOPIC_EXCHANGE_STATE = tag + RabbitConstant.TOPIC_EXCHANGE_STATE;
        RabbitConstant.QUEUE_DRIVER_STATE = tag + RabbitConstant.QUEUE_DRIVER_STATE;
        RabbitConstant.QUEUE_DEVICE_STATE = tag + RabbitConstant.QUEUE_DEVICE_STATE;

        // Alarm related constants
        RabbitConstant.TOPIC_EXCHANGE_ALARM = tag + RabbitConstant.TOPIC_EXCHANGE_ALARM;
        RabbitConstant.QUEUE_DRIVER_ALARM = tag + RabbitConstant.QUEUE_DRIVER_ALARM;
        RabbitConstant.QUEUE_DEVICE_ALARM = tag + RabbitConstant.QUEUE_DEVICE_ALARM;

        // Metadata related constants
        RabbitConstant.TOPIC_EXCHANGE_METADATA = tag + RabbitConstant.TOPIC_EXCHANGE_METADATA;
        RabbitConstant.QUEUE_DRIVER_METADATA_PREFIX = tag + RabbitConstant.QUEUE_DRIVER_METADATA_PREFIX;

        // Point command related constants
        RabbitConstant.TOPIC_EXCHANGE_POINT_COMMAND = tag + RabbitConstant.TOPIC_EXCHANGE_POINT_COMMAND;
        RabbitConstant.QUEUE_POINT_COMMAND_PREFIX = tag + RabbitConstant.QUEUE_POINT_COMMAND_PREFIX;

        // Point Value related constants
        RabbitConstant.TOPIC_EXCHANGE_VALUE = tag + RabbitConstant.TOPIC_EXCHANGE_VALUE;
        RabbitConstant.QUEUE_POINT_VALUE = tag + RabbitConstant.QUEUE_POINT_VALUE;
    }

}
