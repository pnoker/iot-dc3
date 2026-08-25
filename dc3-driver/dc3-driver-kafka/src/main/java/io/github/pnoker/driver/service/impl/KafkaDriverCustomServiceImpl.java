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
package io.github.pnoker.driver.service.impl;

import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom driver service implementation for the Apache Kafka streaming data source driver.
 * <p>
 * Kafka is a publish-subscribe stream, so inbound values are received asynchronously through
 * {@link KafkaListener} and cached by message key; {@link #read} returns the latest cached value
 * while {@link #write} produces a message to the configured topic.
 * </p>
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Map<String, String> latestByKey = new ConcurrentHashMap<>();
    @Value("${dc3.driver.code}")
    private String driverCode;

    private static void checkRequired(Map<String, AttributeBO> config, String code,
                                      List<ValidationReport.AttributeIssue> issues) {
        AttributeBO attr = config.get(code);
        if (attr == null || attr.getValue() == null) {
            issues.add(ValidationReport.AttributeIssue.builder()
                    .attributeCode(code).level(ValidationReport.IssueLevel.ERROR)
                    .message("Missing required attribute: " + code).build());
        }
    }

    @Override
    public void initial() {
        // Kafka client lifecycle is managed by Spring Kafka auto-configuration.
    }

    @Override
    public void schedule() {
        // No custom scheduled task; messages are consumed asynchronously.
    }

    @Override
    public void event(io.github.pnoker.common.entity.dto.MetadataEventDTO metadataEvent) {
        // No per-device connection state to release.
    }

    /**
     * Consume inbound messages and cache the latest value per key (or per topic when
     * the message has no key).
     *
     * @param record inbound consumer record
     */
    @KafkaListener(topics = "${dc3.driver.kafka.topic:dc3-driver-kafka}",
            groupId = "${dc3.driver.kafka.group-id:dc3-driver-kafka-group}")
    public void onMessage(ConsumerRecord<String, String> record) {
        String cacheKey = Objects.isNull(record.key()) || record.key().isEmpty() ? record.topic() : record.key();
        latestByKey.put(cacheKey, record.value());
        log.debug("Kafka message consumed, protocol={}, topic={}, key={}", driverCode, record.topic(), record.key());
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        String cacheKey = resolveCacheKey(driverConfig, pointConfig);
        String value = latestByKey.get(cacheKey);
        if (Objects.isNull(value)) {
            throw new ReadPointException("No Kafka message consumed yet, protocol={}, key={}", driverCode, cacheKey);
        }
        return new ReadPointValue(device, point, value);
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        try {
            String topic = getConfigValue(pointConfig, "topic", getConfigValue(driverConfig, "topic", "dc3-driver-kafka"));
            String key = getConfigValue(pointConfig, "key", "");
            String value = writePointValue.getValue(String.class);
            if (key.isEmpty()) {
                kafkaTemplate.send(topic, value);
            } else {
                kafkaTemplate.send(topic, key, value);
            }
            return true;
        } catch (Exception e) {
            throw new WritePointException("Kafka write failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    private String resolveCacheKey(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig) {
        String key = getConfigValue(pointConfig, "key", "");
        if (!key.isEmpty()) {
            return key;
        }
        return getConfigValue(driverConfig, "topic", "dc3-driver-kafka");
    }

    private String getRequiredConfig(Map<String, AttributeBO> config, String code) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
            throw new ConnectorException("Required attribute '{}' is missing", code);
        }
        return attr.getValue(String.class);
    }

    private String getConfigValue(Map<String, AttributeBO> config, String code, String defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
            return defaultValue;
        }
        return attr.getValue(String.class);
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

}
