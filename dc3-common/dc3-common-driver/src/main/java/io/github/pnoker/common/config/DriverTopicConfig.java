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
 * RabbitMQ configuration that declares the driver-specific metadata and command queues
 * together with their exchange bindings.
 */
@Slf4j
@Configuration
@ConditionalOnClass(ExchangeConfig.class)
public class DriverTopicConfig {

	private final DriverProperties driverProperties;

	private final TopicExchange metadataExchange;

	private final TopicExchange commandExchange;

	public DriverTopicConfig(DriverProperties driverProperties, TopicExchange metadataExchange,
			TopicExchange commandExchange) {
		this.driverProperties = driverProperties;
		this.metadataExchange = metadataExchange;
		this.commandExchange = commandExchange;
	}

	/**
	 * Creates the metadata queue used to receive driver metadata synchronization events.
	 * @return metadata queue
	 */
	@Bean
	Queue metadataQueue() {
		Map<String, Object> arguments = new HashMap<>();
		// 30 seconds: 30 * 1000 = 30000L
		arguments.put(RabbitConstant.MESSAGE_TTL, 30000L);
		return new Queue(RabbitConstant.QUEUE_DRIVER_METADATA_PREFIX + driverProperties.getClient(), true, false, true,
				arguments);
	}

	/**
	 * Binds the metadata queue to the metadata exchange.
	 * @param metadataQueue metadata queue
	 * @return queue binding
	 */
	@Bean
	Binding metadataBinding(Queue metadataQueue) {
		Binding binding = BindingBuilder.bind(metadataQueue)
			.to(metadataExchange)
			.with(RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + driverProperties.getService());
		binding.addArgument(RabbitConstant.AUTO_DELETE, false);
		return binding;
	}

	/**
	 * Creates the driver command queue used to receive driver-level commands.
	 * @return driver command queue
	 */
	@Bean
	Queue driverCommandQueue() {
		Map<String, Object> arguments = new HashMap<>();
		// 30 seconds: 30 * 1000 = 30000L
		arguments.put(RabbitConstant.MESSAGE_TTL, 30000L);
		return new Queue(RabbitConstant.QUEUE_DRIVER_COMMAND_PREFIX + driverProperties.getService(), true, false, false,
				arguments);
	}

	/**
	 * Binds the driver command queue to the command exchange.
	 * @param driverCommandQueue driver command queue
	 * @return queue binding
	 */
	@Bean
	Binding driverCommandBinding(Queue driverCommandQueue) {
		Binding binding = BindingBuilder.bind(driverCommandQueue)
			.to(commandExchange)
			.with(RabbitConstant.ROUTING_DRIVER_COMMAND_PREFIX + driverProperties.getService());
		binding.addArgument(RabbitConstant.AUTO_DELETE, false);
		return binding;
	}

	/**
	 * Creates the device command queue used to receive device read and write commands.
	 * @return device command queue
	 */
	@Bean
	Queue deviceCommandQueue() {
		Map<String, Object> arguments = new HashMap<>();
		// 30 seconds: 30 * 1000 = 30000L
		arguments.put(RabbitConstant.MESSAGE_TTL, 30000L);
		return new Queue(RabbitConstant.QUEUE_DEVICE_COMMAND_PREFIX + driverProperties.getService(), true, false, false,
				arguments);
	}

	/**
	 * Binds the device command queue to the command exchange.
	 * @param deviceCommandQueue device command queue
	 * @return queue binding
	 */
	@Bean
	Binding deviceCommandBinding(Queue deviceCommandQueue) {
		Binding binding = BindingBuilder.bind(deviceCommandQueue)
			.to(commandExchange)
			.with(RabbitConstant.ROUTING_DEVICE_COMMAND_PREFIX + driverProperties.getService());
		binding.addArgument(RabbitConstant.AUTO_DELETE, false);
		return binding;
	}

}
