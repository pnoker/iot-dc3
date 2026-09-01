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

package io.github.pnoker.common.mq.tck;

import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.mq.adapter.BrokerAdapter;
import io.github.pnoker.common.mq.config.BatchConsumerProperties;
import io.github.pnoker.common.mq.rabbit.RabbitMqAdapter;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Objects;

/**
 * RabbitMQ harness for the broker-neutral contract suite: disposable container,
 * publisher confirms and returns enabled, fast batch/retry tuning.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@Testcontainers(disabledWithoutDocker = true)
class RabbitMqContractIT extends AbstractMqContractTest {

    @Container
    private static final RabbitMQContainer RABBIT =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"));

    private CachingConnectionFactory connectionFactory;
    private RabbitMqAdapter rabbitAdapter;

    private static BatchConsumerProperties fastProperties() {
        BatchConsumerProperties properties = new BatchConsumerProperties();
        properties.setBatchSize(10);
        properties.setReceiveTimeoutMillis(100);
        properties.setConcurrentConsumers(2);
        properties.setMaxConcurrentConsumers(4);
        properties.setPrefetchCount(10);
        properties.setMaxRetries(2);
        properties.setRetryInitialIntervalMillis(100);
        properties.setRetryMultiplier(2);
        properties.setRetryMaxIntervalMillis(200);
        return properties;
    }

    @Override
    protected BrokerAdapter adapter() {
        if (Objects.isNull(rabbitAdapter)) {
            connectionFactory = new CachingConnectionFactory(java.net.URI.create(RABBIT.getAmqpUrl()));
            connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
            connectionFactory.setPublisherReturns(true);
            RabbitTemplate template = new RabbitTemplate(connectionFactory);
            template.setMandatory(true);
            RabbitAdmin admin = new RabbitAdmin(connectionFactory);
            rabbitAdapter = new RabbitMqAdapter(template, admin, connectionFactory, fastProperties(), 300_000);
        }
        return rabbitAdapter;
    }

    @Override
    protected void shutdownAdapter() {
        if (Objects.nonNull(rabbitAdapter)) {
            rabbitAdapter.stop();
            rabbitAdapter = null;
        }
    }

    /**
     * The driver command queue expires (x-expires) once its instance stops; after the
     * TTL the broker removes the queue entirely.
     */
    @Test
    @Override
    public void perInstanceSubscriptionExpiresAfterInstanceStops() {
        adapter();
        Assertions.assertTrue(rabbitAdapter.capabilities().subscriptionExpiry(),
                "rabbitmq declares subscriptionExpiry=true");

        String group = "tck-ttl";
        subscribeCollector(loadBalancePattern(MqTopic.POINT_COMMAND, group, "tck.*", Duration.ofMillis(600)),
                delivery -> io.github.pnoker.common.constant.mq.DeliveryDisposition.ACK);
        shutdownAdapter();

        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        Awaitility.await("queue expires after instance stop")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> Objects.isNull(admin.getQueueProperties("dc3.q.point_command." + group)));
    }
}
