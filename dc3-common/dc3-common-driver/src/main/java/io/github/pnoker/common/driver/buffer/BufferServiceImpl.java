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

import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * SQLite-backed {@link BufferService}. Persists point values that failed to reach RabbitMQ
 * (synchronous {@link AmqpException} or asynchronous publisher NACK) and republishes them
 * from a Quartz job with exponential backoff. When the buffer file exceeds the configured
 * size cap the oldest records are evicted to keep the newest readings.
 *
 * <p>Republish is optimistic: a record that leaves the channel without throwing is deleted,
 * and a later NACK re-queues it through the confirm callback using the same correlation id.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BufferServiceImpl implements BufferService {

    private final DriverProperties driverProperties;
    private final RabbitTemplate rabbitTemplate;

    private PointValueBuffer buffer;

    @Override
    public void initialize() {
        DriverProperties.BufferProperties config = driverProperties.getBuffer();
        if (Objects.isNull(config) || !Boolean.TRUE.equals(config.getEnabled())) {
            log.info("Point value buffer disabled, skip initialization");
            return;
        }
        this.buffer = new PointValueBuffer(config.getDbPath());
        this.buffer.initialize();
    }

    @Override
    public boolean isEnabled() {
        DriverProperties.BufferProperties config = driverProperties.getBuffer();
        return Objects.nonNull(config) && Boolean.TRUE.equals(config.getEnabled()) && Objects.nonNull(buffer);
    }

    @Override
    public long pendingCount() {
        return Objects.nonNull(buffer) ? buffer.count() : 0;
    }

    @Override
    public void offer(PointValue pointValue, String routingKey, String correlationId, int attempt) {
        if (!isEnabled()) {
            return;
        }
        DriverProperties.BufferProperties config = driverProperties.getBuffer();
        long now = epochSecond();
        BufferedPointValue record = new BufferedPointValue(
                correlationId,
                pointValue.getDeviceId(),
                pointValue.getPointId(),
                pointValue.getDriverId(),
                pointValue.getTenantId(),
                JsonUtil.toJsonString(pointValue),
                routingKey,
                attempt,
                now + backoffSeconds(attempt, config),
                now
        );
        buffer.upsert(record);
        if (log.isDebugEnabled()) {
            log.debug("Buffered point value, id={}, deviceId={}, pointId={}, attempt={}, queueSize={}",
                    correlationId, pointValue.getDeviceId(), pointValue.getPointId(), attempt, buffer.count());
        }
        enforceCapacity(config);
    }

    @Override
    public void republishBatch() {
        if (!isEnabled()) {
            return;
        }
        DriverProperties.BufferProperties config = driverProperties.getBuffer();
        List<BufferedPointValue> records = buffer.selectPending(config.getBatchSize(), epochSecond());
        if (records.isEmpty()) {
            return;
        }
        log.debug("Republishing {} buffered point values", records.size());
        for (BufferedPointValue record : records) {
            republishOne(record, config);
        }
        enforceCapacity(config);
    }

    /**
     * Republish a single buffered record. Records that have exhausted {@code maxRetry} are
     * dropped as poison with an ERROR log; the rest are re-sent with an incremented attempt
     * counter carried in the correlation so a NACK re-queue stores the right ordinal.
     */
    private void republishOne(BufferedPointValue record, DriverProperties.BufferProperties config) {
        if (record.attempt() >= config.getMaxRetry()) {
            log.error("Buffer record exceeded max retry ({}), dropping poison, id={}, deviceId={}, pointId={}",
                    config.getMaxRetry(), record.id(), record.deviceId(), record.pointId());
            buffer.delete(record.id());
            return;
        }
        PointValue pointValue;
        try {
            pointValue = JsonUtil.parseObject(record.payloadJson(), PointValue.class);
        } catch (Exception e) {
            log.error("Buffer record payload corrupted, dropping, id={}, deviceId={}, pointId={}",
                    record.id(), record.deviceId(), record.pointId(), e);
            buffer.delete(record.id());
            return;
        }
        int nextAttempt = record.attempt() + 1;
        PointValueCorrelation correlation = new PointValueCorrelation(
                record.id(), record.deviceId(), record.pointId(), nextAttempt,
                record.payloadJson(), record.routingKey());
        try {
            rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_VALUE, record.routingKey(), pointValue, correlation);
            // Optimistic delete: the message left the channel. A later NACK re-queues it
            // via the ConfirmCallback using the same correlation id.
            buffer.delete(record.id());
        } catch (AmqpException e) {
            long backoff = backoffSeconds(nextAttempt, config);
            log.warn("Buffer republish rejected, id={}, attempt={}, retrying in {}s",
                    record.id(), nextAttempt, backoff);
            buffer.markRetry(record.id(), nextAttempt, epochSecond() + backoff);
        }
    }

    /**
     * When the SQLite file exceeds the configured size cap, evict the oldest batch. SQLite
     * reuses freed pages, so the file does not shrink without a VACUUM — that is acceptable
     * for a bounded buffer that cycles through records.
     */
    private void enforceCapacity(DriverProperties.BufferProperties config) {
        long maxBytes = config.getMaxSizeMb() * 1024L * 1024L;
        if (buffer.fileSize() <= maxBytes) {
            return;
        }
        int evicted = buffer.deleteOldest(config.getBatchSize());
        log.warn("Buffer capacity exceeded ({}B > {}B), evicted {} oldest records",
                buffer.fileSize(), maxBytes, evicted);
    }

    /**
     * Exponential backoff in seconds: {@code backoffSeconds * 2^(attempt-1)}, capped at
     * {@code maxBackoffSeconds}.
     */
    private long backoffSeconds(int attempt, DriverProperties.BufferProperties config) {
        long delay = (long) (config.getBackoffSeconds() * Math.pow(2, attempt - 1));
        return Math.min(delay, config.getMaxBackoffSeconds());
    }

    private static long epochSecond() {
        return System.currentTimeMillis() / 1000;
    }

    @PreDestroy
    void destroy() {
        if (Objects.nonNull(buffer)) {
            buffer.close();
        }
    }
}
