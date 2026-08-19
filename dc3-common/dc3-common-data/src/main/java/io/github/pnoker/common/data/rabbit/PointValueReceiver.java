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

package io.github.pnoker.common.data.rabbit;

import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqPoisonException;
import io.github.pnoker.common.mq.listener.MqReceived;
import io.github.pnoker.common.constant.mq.ConsumptionProfile;
import io.github.pnoker.common.constant.mq.DeliveryMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Point-value consumer. RabbitMQ itself is the durable buffer; the consumer receives
 * broker batches, validates the schema-v1 envelope of every value, persists history and
 * latest projections in one transaction, and acknowledges the batch only after the
 * PostgreSQL transaction commits.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointValueReceiver {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final PointValueService pointValueService;

    /**
     * Consume and durably persist one broker batch. A value violating the schema
     * version poisons the whole batch (bounded retry, then dead-letter); the ack
     * commits every delivery in the batch after the save transaction.
     *
     * @param messages raw batch in broker delivery order
     * @param ack      batch-level acknowledgement handle
     */
    @Dc3Listener(topic = MqTopic.POINT_VALUE, profile = ConsumptionProfile.THROUGHPUT, delivery = DeliveryMode.BATCH)
    public void pointValueReceive(List<MqReceived<PointValueBO>> messages, Acknowledgment ack) {
        if (messages.isEmpty()) {
            return;
        }

        List<PointValueBO> values = new ArrayList<>(messages.size());
        for (MqReceived<PointValueBO> message : messages) {
            PointValueBO value = message.payload();
            if (!valid(value)) {
                log.error("Reject poison point-value batch, reason=invalidSchemaV1, batchSize={}", messages.size());
                throw new MqPoisonException("Point-value payload violates schema version 1");
            }
            values.add(value);
        }

        pointValueService.save(values);

        ack.ack();
        log.debug("Persisted and acknowledged point-value batch, size={}", values.size());
    }

    private boolean valid(PointValueBO value) {
        return Objects.nonNull(value)
                && Objects.equals(value.getSchemaVersion(), SUPPORTED_SCHEMA_VERSION)
                && Objects.nonNull(value.getMessageId()) && !value.getMessageId().isBlank()
                && Objects.nonNull(value.getDriverNode()) && !value.getDriverNode().isBlank()
                && positive(value.getSequence())
                && positive(value.getFencingToken())
                && positive(value.getTenantId())
                && positive(value.getDriverId())
                && positive(value.getDeviceId())
                && positive(value.getPointId())
                && Objects.nonNull(value.getRawValue())
                && Objects.nonNull(value.getCalValue())
                && Objects.nonNull(value.getCreateTime());
    }

    private boolean positive(Long value) {
        return Objects.nonNull(value) && value > 0;
    }
}
