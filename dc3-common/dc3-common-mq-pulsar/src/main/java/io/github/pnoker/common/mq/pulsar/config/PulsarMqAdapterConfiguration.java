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

package io.github.pnoker.common.mq.pulsar.config;

import io.github.pnoker.common.mq.config.BatchConsumerProperties;
import io.github.pnoker.common.mq.pulsar.PulsarMqAdapter;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Activates the Pulsar adapter when {@code dc3.mq.type=pulsar}. Service URL comes
 * from {@code dc3.mq.pulsar.service-url}.
 *
 * @author pnoker
 * @since 2026.8.20
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "dc3.mq", name = "type", havingValue = "pulsar")
public class PulsarMqAdapterConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(PulsarClient.class)
    public PulsarClient pulsarClient(
            @Value("${dc3.mq.pulsar.service-url:pulsar://localhost:6650}") String serviceUrl)
            throws PulsarClientException {
        return PulsarClient.builder().serviceUrl(serviceUrl).build();
    }

    @Bean
    public PulsarMqAdapter pulsarMqAdapter(PulsarClient pulsarClient, BatchConsumerProperties batchProperties) {
        return new PulsarMqAdapter(pulsarClient, batchProperties);
    }
}
