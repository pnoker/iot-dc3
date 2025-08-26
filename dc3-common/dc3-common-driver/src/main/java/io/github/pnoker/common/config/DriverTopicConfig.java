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

import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * DriverTopicConfig is a configuration class that provides queue and binding
 * configurations for metadata, driver command, and device command messages
 * within a message broker. It is conditional on the presence of the ExchangeConfig class.
 * <p>
 * The queues and bindings are used to facilitate communication between
 * components in the driver service by leveraging message exchanges.
 */
@Slf4j
@Configuration
@ConditionalOnClass(ExchangeConfig.class)
public class DriverTopicConfig {

    private final DriverProperties driverProperties;
    private final TopicExchange metadataExchange;
    private final TopicExchange commandExchange;

    public DriverTopicConfig(DriverProperties driverProperties, TopicExchange metadataExchange, TopicExchange commandExchange) {
        this.driverProperties = driverProperties;
        this.metadataExchange = metadataExchange;
        this.commandExchange = commandExchange;
    }

    /**
     * 元数据队列配置
     * 用于处理驱动元数据信息
     *
     * @return Queue 元数据队列
     */
    @Bean
    Queue metadataQueue() {
        Map<String, Object> arguments = new HashMap<>();
        // 30秒: 30 * 1000 = 30000L
        arguments.put(RabbitConstant.MESSAGE_TTL, 30000L);
        return new Queue(RabbitConstant.QUEUE_DRIVER_METADATA_PREFIX + driverProperties.getClient(), false, false, false, arguments);
    }

    /**
     * 元数据绑定配置
     * 将元数据队列绑定到元数据交换机
     *
     * @param metadataQueue 元数据队列
     * @return Binding 绑定关系
     */
    @Bean
    Binding metadataBinding(Queue metadataQueue) {
        Binding binding = BindingBuilder
                .bind(metadataQueue)
                .to(metadataExchange)
                .with(RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + driverProperties.getService());
        binding.addArgument(RabbitConstant.AUTO_DELETE, false);
        return binding;
    }

    /**
     * 驱动命令队列配置
     * 用于处理发送给驱动的命令
     *
     * @return Queue 驱动命令队列
     */
    @Bean
    Queue driverCommandQueue() {
        Map<String, Object> arguments = new HashMap<>();
        // 30秒: 30 * 1000 = 30000L
        arguments.put(RabbitConstant.MESSAGE_TTL, 30000L);
        return new Queue(RabbitConstant.QUEUE_DRIVER_COMMAND_PREFIX + driverProperties.getService(), false, false, false, arguments);
    }

    /**
     * 驱动命令绑定配置
     * 将驱动命令队列绑定到命令交换机
     *
     * @param driverCommandQueue 驱动命令队列
     * @return Binding 绑定关系
     */
    @Bean
    Binding driverCommandBinding(Queue driverCommandQueue) {
        Binding binding = BindingBuilder
                .bind(driverCommandQueue)
                .to(commandExchange)
                .with(RabbitConstant.ROUTING_DRIVER_COMMAND_PREFIX + driverProperties.getService());
        binding.addArgument(RabbitConstant.AUTO_DELETE, false);
        return binding;
    }

    /**
     * 设备命令队列配置
     * 用于处理发送给设备的命令
     *
     * @return Queue 设备命令队列
     */
    @Bean
    Queue deviceCommandQueue() {
        Map<String, Object> arguments = new HashMap<>();
        // 30秒: 30 * 1000 = 30000L
        arguments.put(RabbitConstant.MESSAGE_TTL, 30000L);
        return new Queue(RabbitConstant.QUEUE_DEVICE_COMMAND_PREFIX + driverProperties.getService(), false, false, false, arguments);
    }

    /**
     * 设备命令绑定配置
     * 将设备命令队列绑定到命令交换机
     *
     * @param deviceCommandQueue 设备命令队列
     * @return Binding 绑定关系
     */
    @Bean
    Binding deviceCommandBinding(Queue deviceCommandQueue) {
        Binding binding = BindingBuilder
                .bind(deviceCommandQueue)
                .to(commandExchange)
                .with(RabbitConstant.ROUTING_DEVICE_COMMAND_PREFIX + driverProperties.getService());
        binding.addArgument(RabbitConstant.AUTO_DELETE, false);
        return binding;
    }

}
