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
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom driver service implementation for the LoRaWAN driver.
 * <p>
 * Subscribes to ChirpStack MQTT uplink topics ({@code application/+/device/+/event/up}),
 * decodes the JSON payload, and caches the latest FRMPayload (base64) and Cayenne LPP
 * object fields per DevEUI. Reads resolve a point against the cached DevEUI; writes publish
 * a downlink command to the ChirpStack command/down topic.
 * </p>
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LorawanDriverCustomServiceImpl implements DriverCustomService, MqttCallback {

    private static final String DEFAULT_TOPIC = "application/+/device/+/event/up";

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${dc3.driver.code}")
    private String driverCode;

    private final Map<String, String> dataByDevEui = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> objectByDevEui = new ConcurrentHashMap<>();

    private MqttClient mqttClient;
    private volatile boolean connected;

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
        // Connection is established lazily on first read/write to keep startup tolerant
        // of a temporarily unreachable broker.
    }

    @Override
    public void schedule() {
        // No custom scheduled task; uplinks arrive asynchronously.
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        return connected ? DeviceHealthState.online() : DeviceHealthState.offline();
    }

    @Override
    public void event(io.github.pnoker.common.entity.dto.MetadataEventDTO metadataEvent) {
        // No per-device connection state to release.
    }

    @Override
    public void connectionLost(Throwable cause) {
        connected = false;
        log.warn("MQTT connection lost, protocol={}", driverCode, cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(payload);
            String devEui = root.path("deviceInfo").path("devEui").asText();
            if (devEui.isEmpty()) {
                log.debug("LoRaWAN uplink without devEui ignored, protocol={}, topic={}", driverCode, topic);
                return;
            }
            String data = root.path("data").asText(null);
            if (Objects.nonNull(data)) {
                dataByDevEui.put(devEui, data);
            }
            JsonNode objectNode = root.path("object");
            if (objectNode.isObject()) {
                Map<String, String> fields = new HashMap<>();
                Iterator<Map.Entry<String, JsonNode>> it = objectNode.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    fields.put(entry.getKey(), entry.getValue().asText());
                }
                objectByDevEui.put(devEui, fields);
            }
            log.debug("LoRaWAN uplink cached, protocol={}, devEui={}", driverCode, devEui);
        } catch (Exception e) {
            log.warn("Failed to parse LoRaWAN uplink, protocol={}, topic={}", driverCode, topic, e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Downlink delivery acknowledgement; nothing to do.
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        String devEui = getRequiredConfig(pointConfig, "devEui");
        String field = getConfigValue(pointConfig, "field", "");
        String value;
        if (field.isEmpty()) {
            value = dataByDevEui.get(devEui);
        } else {
            Map<String, String> fields = objectByDevEui.get(devEui);
            value = Objects.isNull(fields) ? null : fields.get(field);
        }
        if (Objects.isNull(value)) {
            throw new ReadPointException("No LoRaWAN uplink cached, protocol={}, devEui={}, field={}",
                    driverCode, devEui, field);
        }
        return new ReadPointValue(device, point, value);
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        try {
            String applicationId = getRequiredConfig(driverConfig, "applicationId");
            String devEui = getRequiredConfig(pointConfig, "devEui");
            String value = writePointValue.getValue(String.class);
            String data = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
            String downTopic = "application/" + applicationId + "/device/" + devEui + "/command/down";
            String json = "{\"confirmed\":false,\"fPort\":1,\"data\":\"" + data + "\"}";
            getClient(driverConfig).publish(downTopic, new MqttMessage(json.getBytes(StandardCharsets.UTF_8)));
            return true;
        } catch (WritePointException e) {
            throw e;
        } catch (Exception e) {
            throw new WritePointException("LoRaWAN write failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    private synchronized MqttClient getClient(Map<String, AttributeBO> driverConfig) throws Exception {
        if (Objects.nonNull(mqttClient) && mqttClient.isConnected()) {
            return mqttClient;
        }
        String brokerUri = getConfigValue(driverConfig, "brokerUri", "tcp://dc3-mqtt:1883");
        String clientId = "dc3-driver-lorawan-" + UUID.randomUUID();
        mqttClient = new MqttClient(brokerUri, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        String username = getConfigValue(driverConfig, "username", "");
        String password = getConfigValue(driverConfig, "password", "");
        if (!username.isEmpty()) {
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        mqttClient.setCallback(this);
        mqttClient.connect(options);
        String topic = getConfigValue(driverConfig, "topic", DEFAULT_TOPIC);
        mqttClient.subscribe(topic, 1);
        connected = true;
        log.info("MQTT connected and subscribed, protocol={}, broker={}, topic={}", driverCode, brokerUri, topic);
        return mqttClient;
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
        checkRequired(driverConfig, "applicationId", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "devEui", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

}
