/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.manager.config;

import io.github.pnoker.common.config.ExchangeConfig;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(ExchangeConfig.class)
public class ManagerTopicConfig {

    @Resource
    private TopicExchange syncExchange;

    /**
     * 该 Queue 用于接收来自驱动端的上行数据同步
     *
     * @return Queue
     */
    @Bean
    Queue syncUpQueue() {
        Map<String, Object> arguments = new HashMap<>();
        // 30秒：30 * 1000 = 30000L
        arguments.put(RabbitConstant.MESSAGE_TTL, 30000L);
        return new Queue(RabbitConstant.QUEUE_SYNC_UP, false, false, false, arguments);
    }

    /**
     * 使用 * 匹配全部 Routing Key
     *
     * @param syncUpQueue Queue
     * @return Binding
     */
    @Bean
    Binding driverRegisterBinding(Queue syncUpQueue) {
        Binding binding = BindingBuilder
                .bind(syncUpQueue)
                .to(syncExchange)
                .with(RabbitConstant.ROUTING_SYNC_UP_PREFIX + SymbolConstant.ASTERISK);
        binding.addArgument(RabbitConstant.AUTO_DELETE, true);
        return binding;
    }

}
