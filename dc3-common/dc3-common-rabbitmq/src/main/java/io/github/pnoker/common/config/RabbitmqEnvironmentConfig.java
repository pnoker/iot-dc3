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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Environment Config
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Order
@Configuration
public class RabbitmqEnvironmentConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 此处配置用于开发环境下多人开发, 根据 env 和 group 标识区分不同对 exchange, queue, topic
        String env = environment.getProperty(EnvironmentConstant.SPRING_ENV, String.class);
        String group = environment.getProperty(EnvironmentConstant.SPRING_GROUP, String.class);

        String tag = EnvironmentUtil.getTag(env, group);

        // Sync
        RabbitConstant.TOPIC_EXCHANGE_REGISTER = tag + RabbitConstant.TOPIC_EXCHANGE_REGISTER;
        RabbitConstant.QUEUE_REGISTER_UP = tag + RabbitConstant.QUEUE_REGISTER_UP;
        RabbitConstant.QUEUE_REGISTER_DOWN_PREFIX = tag + RabbitConstant.QUEUE_REGISTER_DOWN_PREFIX;

        // Event
        RabbitConstant.TOPIC_EXCHANGE_EVENT = tag + RabbitConstant.TOPIC_EXCHANGE_EVENT;
        RabbitConstant.QUEUE_DRIVER_EVENT = tag + RabbitConstant.QUEUE_DRIVER_EVENT;
        RabbitConstant.QUEUE_DEVICE_EVENT = tag + RabbitConstant.QUEUE_DEVICE_EVENT;

        // Metadata
        RabbitConstant.TOPIC_EXCHANGE_METADATA = tag + RabbitConstant.TOPIC_EXCHANGE_METADATA;
        RabbitConstant.QUEUE_DRIVER_METADATA_PREFIX = tag + RabbitConstant.QUEUE_DRIVER_METADATA_PREFIX;

        // Command
        RabbitConstant.TOPIC_EXCHANGE_COMMAND = tag + RabbitConstant.TOPIC_EXCHANGE_COMMAND;
        RabbitConstant.QUEUE_DRIVER_COMMAND_PREFIX = tag + RabbitConstant.QUEUE_DRIVER_COMMAND_PREFIX;
        RabbitConstant.QUEUE_DEVICE_COMMAND_PREFIX = tag + RabbitConstant.QUEUE_DEVICE_COMMAND_PREFIX;

        // Point Value
        RabbitConstant.TOPIC_EXCHANGE_VALUE = tag + RabbitConstant.TOPIC_EXCHANGE_VALUE;
        RabbitConstant.QUEUE_POINT_VALUE = tag + RabbitConstant.QUEUE_POINT_VALUE;
    }

}
