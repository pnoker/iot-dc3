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

package io.github.pnoker.common.driver.buffer;

import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.mq.message.MqMessage;
import io.github.pnoker.common.mq.sender.MessageSender;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * SQLite-backed durable point-value outbox. Values are persisted before the first
 * publish and deleted only after a positive publisher confirmation. Every failure path
 * retains the same message identity for idempotent downstream retry. The outbox itself
 * is broker-neutral: only the final publish goes through the messaging port.
 *
 * @author pnoker
 * @since 2026.6.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BufferServiceImpl implements BufferService {

    /**
     * Physical routing-key prefix persisted by pre-port versions; stripped on republish
     * so pending outbox rows survive the port migration.
     */
    private static final String LEGACY_ROUTING_PREFIX = "dc3.r.value.point.";

    private final DriverProperties driverProperties;
    private final MessageSender messageSender;

    private PointValueBuffer buffer;

    private static long epochSecond() {
        return System.currentTimeMillis() / 1000;
    }

    @Override
    public void initialize() {
        DriverProperties.BufferProperties config = driverProperties.getBuffer();
        if (Objects.isNull(config)) {
            throw new IllegalStateException("Driver point-value outbox configuration is required");
        }
        this.buffer = new PointValueBuffer(config.getDbPath());
        this.buffer.initialize();
    }

    private PointValueBuffer requireBuffer() {
        if (Objects.isNull(buffer)) {
            throw new IllegalStateException("Driver point-value outbox is not initialized");
        }
        return buffer;
    }

    @Override
    public long pendingCount() {
        return requireBuffer().count();
    }

    @Override
    public void publish(PointValue pointValue, String partitionKey) {
        publishBatch(List.of(pointValue), partitionKey);
    }

    @Override
    public void publishBatch(List<PointValue> pointValues, String partitionKey) {
        if (pointValues == null || pointValues.isEmpty()) {
            return;
        }
        long now = epochSecond();
        List<BufferedPointValue> records = pointValues.stream()
                .map(pointValue -> toRecord(pointValue, partitionKey, pointValue.getMessageId(), 0, now))
                .toList();
        requireBuffer().upsertBatch(records);
        pointValues.forEach(pointValue -> publishPersisted(
                pointValue, partitionKey, pointValue.getMessageId(), 1));
    }

    private BufferedPointValue toRecord(PointValue pointValue, String partitionKey, String correlationId,
                                        int attempt, long now) {
        DriverProperties.BufferProperties config = driverProperties.getBuffer();
        return new BufferedPointValue(
                correlationId,
                pointValue.getDeviceId(),
                pointValue.getPointId(),
                pointValue.getDriverId(),
                pointValue.getTenantId(),
                JsonUtil.toJsonString(pointValue),
                partitionKey,
                attempt,
                now + backoffSeconds(attempt, config),
                now);
    }

    @Override
    public void republishBatch() {
        DriverProperties.BufferProperties config = driverProperties.getBuffer();
        List<BufferedPointValue> records = requireBuffer().selectPending(config.getBatchSize(), epochSecond());
        if (records.isEmpty()) {
            return;
        }
        log.debug("Republishing {} point values from outbox", records.size());
        records.forEach(record -> republishOne(record, config));
    }

    private void republishOne(BufferedPointValue record, DriverProperties.BufferProperties config) {
        PointValue pointValue;
        try {
            pointValue = JsonUtil.parseObject(record.payloadJson(), PointValue.class);
        } catch (Exception e) {
            buffer.markRetry(record.id(), record.attempt(), epochSecond() + config.getMaxBackoffSeconds());
            log.error("Outbox payload corrupted and retained, id={}, deviceId={}, pointId={}",
                    record.id(), record.deviceId(), record.pointId(), e);
            return;
        }

        int nextAttempt = record.attempt() == Integer.MAX_VALUE ? Integer.MAX_VALUE : record.attempt() + 1;
        long backoff = backoffSeconds(nextAttempt, config);
        // Claim the row before publishing so overlapping scheduler runs do not resend it.
        buffer.markRetry(record.id(), nextAttempt, epochSecond() + backoff);
        publishPersisted(pointValue, partitionKeyOf(record.routingKey()), record.id(), nextAttempt);
    }

    /**
     * Rows written before the messaging port carry the full physical routing key; strip
     * the prefix so the port receives the semantic partition key again.
     */
    private String partitionKeyOf(String storedKey) {
        if (Objects.nonNull(storedKey) && storedKey.startsWith(LEGACY_ROUTING_PREFIX)) {
            return storedKey.substring(LEGACY_ROUTING_PREFIX.length());
        }
        return storedKey;
    }

    private void publishPersisted(PointValue pointValue, String partitionKey, String correlationId, int attempt) {
        try {
            messageSender.sendAsync(MqMessage.of(MqTopic.POINT_VALUE, partitionKey, pointValue),
                    (message, confirmed, cause) -> {
                        if (confirmed) {
                            buffer.delete(correlationId);
                            return;
                        }
                        markPublishFailure(correlationId, attempt, cause);
                    });
        } catch (Exception e) {
            markPublishFailure(correlationId, attempt, e);
        }
    }

    private void markPublishFailure(String correlationId, int attempt, Throwable failure) {
        DriverProperties.BufferProperties config = driverProperties.getBuffer();
        long backoff = backoffSeconds(attempt, config);
        buffer.markRetry(correlationId, attempt, epochSecond() + backoff);
        log.warn("Point value publish unconfirmed, id={}, attempt={}, retryInSeconds={}",
                correlationId, attempt, backoff, failure);
    }

    private long backoffSeconds(int attempt, DriverProperties.BufferProperties config) {
        int exponent = Math.max(0, Math.min(attempt - 1, 30));
        long multiplier = 1L << exponent;
        long initial = config.getBackoffSeconds();
        long delay = initial > Long.MAX_VALUE / multiplier ? Long.MAX_VALUE : initial * multiplier;
        return Math.min(delay, config.getMaxBackoffSeconds());
    }

    @PreDestroy
    void destroy() {
        if (Objects.nonNull(buffer)) {
            buffer.close();
        }
    }
}
