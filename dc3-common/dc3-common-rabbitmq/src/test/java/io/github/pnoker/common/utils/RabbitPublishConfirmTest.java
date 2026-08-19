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

package io.github.pnoker.common.utils;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RabbitPublishConfirmTest {

    @Test
    void acceptsOnlyAckedAndRoutedPublish() {
        CorrelationData correlation = new CorrelationData("ok");
        correlation.getFuture().complete(new CorrelationData.Confirm(true, null));

        assertThatCode(() -> RabbitPublishConfirm.awaitRouted(correlation, Duration.ofSeconds(1)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPublisherNack() {
        CorrelationData correlation = new CorrelationData("nack");
        correlation.getFuture().complete(new CorrelationData.Confirm(false, "broker rejected"));

        assertThatThrownBy(() -> RabbitPublishConfirm.awaitRouted(correlation, Duration.ofSeconds(1)))
                .isInstanceOf(AmqpException.class)
                .hasMessageContaining("NACK");
    }

    @Test
    void rejectsUnroutableAck() {
        CorrelationData correlation = new CorrelationData("returned");
        correlation.setReturned(new ReturnedMessage(new Message(new byte[0], new MessageProperties()),
                312, "NO_ROUTE", "exchange", "routing"));
        correlation.getFuture().complete(new CorrelationData.Confirm(true, null));

        assertThatThrownBy(() -> RabbitPublishConfirm.awaitRouted(correlation, Duration.ofSeconds(1)))
                .isInstanceOf(AmqpException.class)
                .hasMessageContaining("unroutable");
    }
}
