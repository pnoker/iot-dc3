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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Getter
@Setter
@Configuration
public class ExchangeConfig {

    /**
     * 事件相关, 平台端可负载
     *
     * @return TopicExchange
     */
    @Bean
    TopicExchange eventExchange() {
        return new TopicExchange(RabbitConstant.TOPIC_EXCHANGE_EVENT, true, false);
    }

    /**
     * 元数据相关, 平台端广播, 驱动端订阅
     *
     * @return FanoutExchange
     */
    @Bean
    TopicExchange metadataExchange() {
        return new TopicExchange(RabbitConstant.TOPIC_EXCHANGE_METADATA, true, false);
    }

    /**
     * 指令相关, 驱动端可负载
     *
     * @return TopicExchange
     */
    @Bean
    TopicExchange commandExchange() {
        return new TopicExchange(RabbitConstant.TOPIC_EXCHANGE_COMMAND, true, false);
    }

    /**
     * 数据相关, 平台端可负载
     *
     * @return TopicExchange
     */
    @Bean
    TopicExchange valueExchange() {
        return new TopicExchange(RabbitConstant.TOPIC_EXCHANGE_VALUE, true, false);
    }

    /**
     * MQTT 相关, 平台端可负载
     *
     * @return TopicExchange
     */
    @Bean
    TopicExchange mqttExchange() {
        return new TopicExchange(RabbitConstant.TOPIC_EXCHANGE_MQTT, true, false);
    }

}
