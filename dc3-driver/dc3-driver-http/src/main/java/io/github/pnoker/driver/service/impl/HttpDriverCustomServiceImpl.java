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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pnoker.common.driver.entity.bean.DeviceHealthState;
import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP REST client driver service implementation.
 * <p>
 * Provides HTTP-based device communication using Spring WebClient.
 * Supports configurable HTTP methods (GET, POST, PUT, DELETE), JSON response
 * path extraction, and request body templates with ${value} placeholders.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HttpDriverCustomServiceImpl implements DriverCustomService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ATTR_BASE_URL = "baseUrl";
    private static final String ATTR_BODY_TEMPLATE = "bodyTemplate";
    private static final String ATTR_METHOD = "method";
    private static final String ATTR_PATH = "path";
    private static final String ATTR_RESPONSE_PATH = "responsePath";
    private static final String ATTR_TIMEOUT = "timeout";
    private static final String DEFAULT_HTTP_METHOD = "GET";
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;

    @Value("${dc3.driver.code}")
    private String driverCode;

    private Map<Long, WebClient> clientMap;

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
        clientMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        // HTTP drivers do not need custom scheduled tasks.
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        return clientMap.containsKey(device.getId())
                ? DeviceHealthState.online()
                : DeviceHealthState.offline();
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, deviceId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());

            if (MetadataOperateTypeEnum.DELETE.equals(operateType)
                    || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                WebClient removed = clientMap.remove(metadataEvent.getId());
                if (Objects.nonNull(removed)) {
                    log.info("Driver connection destroyed, protocol={}, deviceId={}, operateType={}",
                            driverCode, metadataEvent.getId(), operateType);
                }
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        WebClient client = getConnector(device.getId(), driverConfig);
        try {
            String path = getConfigValue(pointConfig, ATTR_PATH, "");
            String method = getConfigValue(pointConfig, ATTR_METHOD, DEFAULT_HTTP_METHOD);
            String responsePath = getConfigValue(pointConfig, ATTR_RESPONSE_PATH, "");

            HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
            String responseBody = client.method(httpMethod)
                    .uri(path)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (Objects.isNull(responseBody)) {
                throw new ReadPointException("Empty HTTP response, protocol={}", driverCode);
            }

            String value = extractValue(responseBody, responsePath);
            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            throw e;
        } catch (Exception e) {
            clientMap.remove(device.getId());
            throw new ReadPointException("HTTP read failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        WebClient client = getConnector(device.getId(), driverConfig);
        try {
            String path = getConfigValue(pointConfig, ATTR_PATH, "");
            String method = getConfigValue(pointConfig, ATTR_METHOD, DEFAULT_HTTP_METHOD);
            String bodyTemplate = getConfigValue(pointConfig, ATTR_BODY_TEMPLATE, "");

            HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
            String body = bodyTemplate.replace("${value}", writePointValue.getValue(String.class));

            WebClient.RequestBodySpec request = client.method(httpMethod).uri(path);
            if (!body.isEmpty()) {
                request.bodyValue(body);
            }

            request.retrieve().bodyToMono(String.class).block();
            return true;
        } catch (Exception e) {
            clientMap.remove(device.getId());
            throw new WritePointException("HTTP write failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    /**
     * Get or create a WebClient for the given device.
     *
     * @param deviceId     unique device identifier
     * @param driverConfig driver configuration containing connection parameters
     * @return cached or newly created WebClient
     */
    private WebClient getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return clientMap.computeIfAbsent(deviceId, id -> {
            String baseUrl = getConfigValue(driverConfig, ATTR_BASE_URL, "");
            int timeout = getConfigIntValue(driverConfig, ATTR_TIMEOUT, DEFAULT_TIMEOUT_MS);

            log.debug("Driver connection creating, protocol={}, deviceId={}, baseUrl={}",
                    driverCode, deviceId, baseUrl);

            HttpClient httpClient = HttpClient.create()
                    .responseTimeout(Duration.ofMillis(timeout));

            WebClient client = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
                    .build();

            log.info("Driver connection established, protocol={}, deviceId={}, baseUrl={}",
                    driverCode, deviceId, baseUrl);
            return client;
        });
    }

    /**
     * Extract a value from a JSON response using a simple path notation.
     *
     * @param json the JSON response body
     * @param path JSON path (e.g. $.data.temperature)
     * @return extracted value as string, or the raw JSON if path is empty
     */
    private String extractValue(String json, String path) {
        if (Objects.isNull(path) || path.isEmpty()) {
            return json;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            String[] parts = path.replace("$.", "").split("\\.");
            for (String part : parts) {
                if (node == null) {
                    return json;
                }
                node = node.get(part);
            }
            return node != null ? node.asText() : json;
        } catch (Exception e) {
            log.warn("JSON path extraction failed, path={}", path, e);
            return json;
        }
    }

    private String getConfigValue(Map<String, AttributeBO> config, String code, String defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
            return defaultValue;
        }
        return attr.getValue(String.class);
    }

    private int getConfigIntValue(Map<String, AttributeBO> config, String code, int defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue())) {
            return defaultValue;
        }
        return attr.getValue(Integer.class);
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, ATTR_BASE_URL, issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, ATTR_PATH, issues);
        checkRequired(pointConfig, ATTR_METHOD, issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

}
