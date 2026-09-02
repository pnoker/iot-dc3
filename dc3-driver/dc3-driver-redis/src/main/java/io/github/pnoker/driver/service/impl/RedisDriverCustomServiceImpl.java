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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Custom driver service implementation for the Redis data source driver.
 * <p>
 * Reads and writes string (GET/SET) and hash (HGET/HSET) keys through the Spring
 * Boot auto-configured {@link StringRedisTemplate}. A device-level key prefix keeps
 * multiple logical devices isolated within one Redis instance.
 * </p>
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${dc3.driver.code}")
    private String driverCode;

    private static void checkRequired(
            Map<String, AttributeBO> config, String code, List<ValidationReport.AttributeIssue> issues) {
        AttributeBO attr = config.get(code);
        if (attr == null || attr.getValue() == null) {
            issues.add(ValidationReport.AttributeIssue.builder()
                    .attributeCode(code)
                    .level(ValidationReport.IssueLevel.ERROR)
                    .message("Missing required attribute: " + code)
                    .build());
        }
    }

    @Override
    public void initial() {
        // Redis connection lifecycle is managed by Spring Boot auto-configuration.
    }

    @Override
    public void schedule() {
        // No custom scheduled task; keys are read on the SDK read schedule.
    }

    @Override
    public void event(io.github.pnoker.common.entity.dto.MetadataEventDTO metadataEvent) {
        // No per-device connection state to release.
    }

    @Override
    public ReadPointValue read(
            Map<String, AttributeBO> driverConfig,
            Map<String, AttributeBO> pointConfig,
            DeviceBO device,
            PointBO point) {
        try {
            String key = resolveKey(driverConfig, pointConfig);
            String dataType = getConfigValue(pointConfig, "dataType", "STRING");
            String value;
            if ("HASH".equalsIgnoreCase(dataType)) {
                String field = getRequiredConfig(pointConfig, "field");
                value = stringRedisTemplate.opsForHash().get(key, field) == null
                        ? null
                        : String.valueOf(stringRedisTemplate.opsForHash().get(key, field));
            } else {
                value = stringRedisTemplate.opsForValue().get(key);
            }
            if (Objects.isNull(value)) {
                throw new ReadPointException("Redis key has no value, protocol={}, key={}", driverCode, key);
            }
            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            throw e;
        } catch (Exception e) {
            throw new ReadPointException("Redis read failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(
            Map<String, AttributeBO> driverConfig,
            Map<String, AttributeBO> pointConfig,
            DeviceBO device,
            PointBO point,
            WritePointValue writePointValue) {
        try {
            String key = resolveKey(driverConfig, pointConfig);
            String value = writePointValue.getValue(String.class);
            String dataType = getConfigValue(pointConfig, "dataType", "STRING");
            if ("HASH".equalsIgnoreCase(dataType)) {
                String field = getRequiredConfig(pointConfig, "field");
                stringRedisTemplate.opsForHash().put(key, field, value);
            } else {
                stringRedisTemplate.opsForValue().set(key, value);
            }
            return true;
        } catch (Exception e) {
            throw new WritePointException("Redis write failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    private String resolveKey(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig) {
        String keyPrefix = getConfigValue(driverConfig, "keyPrefix", "");
        String key = getRequiredConfig(pointConfig, "key");
        return keyPrefix + key;
    }

    private String getRequiredConfig(Map<String, AttributeBO> config, String code) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr)
                || Objects.isNull(attr.getValue())
                || attr.getValue().isEmpty()) {
            throw new ConnectorException("Required attribute '{}' is missing", code);
        }
        return attr.getValue(String.class);
    }

    private String getConfigValue(Map<String, AttributeBO> config, String code, String defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr)
                || Objects.isNull(attr.getValue())
                || attr.getValue().isEmpty()) {
            return defaultValue;
        }
        return attr.getValue(String.class);
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "key", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }
}
