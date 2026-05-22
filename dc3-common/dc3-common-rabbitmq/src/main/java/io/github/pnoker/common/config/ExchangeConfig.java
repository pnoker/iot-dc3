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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * RabbitMQ Exchange Configuration Class
 * <p>
 * Configuration class for defining RabbitMQ topic exchanges in IoT DC3 platform.
 * Configures different exchanges for events, metadata, commands, data, and MQTT. Each
 * exchange serves different messaging purposes in the system architecture.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Getter
@Setter
@AutoConfiguration
public class ExchangeConfig {

    /**
     * State exchange for platform-side load balancing
     *
     * @return TopicExchange bean for state messages
     */
    @Bean
    TopicExchange stateExchange() {
        return new TopicExchange(RabbitConstant.TOPIC_EXCHANGE_STATE, true, false);
    }

    /**
     * Alarm exchange for platform-side load balancing
     *
     * @return TopicExchange bean for alarm messages
     */
    @Bean
    TopicExchange alarmExchange() {
        return new TopicExchange(RabbitConstant.TOPIC_EXCHANGE_ALARM, true, false);
    }

    /**
     * Metadata exchange for platform broadcasting and driver subscription
     *
     * @return TopicExchange bean for metadata messages
     */
    @Bean
    TopicExchange metadataExchange() {
        return new TopicExchange(RabbitConstant.TOPIC_EXCHANGE_METADATA, true, false);
    }

    /**
     * Point command exchange for driver-side load balancing
     *
     * @return TopicExchange bean for point command messages
     */
    @Bean
    TopicExchange pointCommandExchange() {
        return new TopicExchange(RabbitConstant.TOPIC_EXCHANGE_POINT_COMMAND, true, false);
    }

    /**
     * Data exchange for platform-side load balancing
     *
     * @return TopicExchange bean for data messages
     */
    @Bean
    TopicExchange valueExchange() {
        return new TopicExchange(RabbitConstant.TOPIC_EXCHANGE_VALUE, true, false);
    }

    /**
     * MQTT exchange for platform-side load balancing
     *
     * @return TopicExchange bean for MQTT messages
     */
    @Bean
    TopicExchange mqttExchange() {
        return new TopicExchange(RabbitConstant.TOPIC_EXCHANGE_MQTT, true, false);
    }

    /**
     * State timeout delay exchange for TTL + DLX based lease checks.
     *
     * @return TopicExchange bean for state timeout delay messages
     */
    @Bean
    TopicExchange stateTimeoutDelayExchange() {
        return new TopicExchange(RabbitConstant.TOPIC_EXCHANGE_STATE_TIMEOUT_DELAY, true, false);
    }

    /**
     * State timeout check exchange receiving dead-lettered timeout messages.
     *
     * @return TopicExchange bean for state timeout check messages
     */
    @Bean
    TopicExchange stateTimeoutCheckExchange() {
        return new TopicExchange(RabbitConstant.TOPIC_EXCHANGE_STATE_TIMEOUT_CHECK, true, false);
    }

}
