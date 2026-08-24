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

package io.github.pnoker.common.mq.kafka.config;

import io.github.pnoker.common.mq.config.BatchConsumerProperties;
import io.github.pnoker.common.mq.kafka.KafkaMqAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

/**
 * Activates the Kafka adapter when {@code dc3.mq.type=kafka}. Bootstrap servers come
 * from {@code dc3.mq.kafka.bootstrap-servers} / {@code DC3_MQ_KAFKA_BOOTSTRAP},
 * falling back to the standard {@code spring.kafka.bootstrap-servers}.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "dc3.mq", name = "type", havingValue = "kafka")
public class KafkaMqAdapterConfiguration {

    /** Producer template on the adapter bootstrap servers; overridable by a user bean. */
    @Bean
    @ConditionalOnMissingBean(KafkaTemplate.class)
    public KafkaTemplate<String, byte[]> kafkaMqTemplate(
            @Value("${dc3.mq.kafka.bootstrap-servers:${DC3_MQ_KAFKA_BOOTSTRAP:${spring.kafka.bootstrap-servers:localhost:9092}}}") String bootstrapServers) {
        return KafkaMqAdapter.template(bootstrapServers);
    }

    /** The port adapter bound to the Kafka template. */
    @Bean
    public KafkaMqAdapter kafkaMqAdapter(KafkaTemplate<String, byte[]> kafkaTemplate,
                                         @Value("${dc3.mq.kafka.bootstrap-servers:${DC3_MQ_KAFKA_BOOTSTRAP:${spring.kafka.bootstrap-servers:localhost:9092}}}")
                                         String bootstrapServers, BatchConsumerProperties batchProperties) {
        Map<String, Object> consumerConfig = KafkaMqAdapter.consumerConfig(bootstrapServers);
        return new KafkaMqAdapter(kafkaTemplate, consumerConfig, batchProperties);
    }
}
