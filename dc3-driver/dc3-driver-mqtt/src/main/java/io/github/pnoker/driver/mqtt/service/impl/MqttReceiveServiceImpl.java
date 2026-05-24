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

package io.github.pnoker.driver.mqtt.service.impl;

import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.EventReportDTO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.facade.api.EventFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeEventBO;
import io.github.pnoker.common.mqtt.entity.MessageHeader;
import io.github.pnoker.common.mqtt.entity.MqttMessage;
import io.github.pnoker.common.mqtt.service.MqttReceiveService;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.LocalDateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * MQTT message receive service implementation.
 * <p>
 * This service handles incoming MQTT messages by converting them to PointValue objects
 * and forwarding them to the DC3 platform. It supports both single message and batch
 * message processing.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttReceiveServiceImpl implements MqttReceiveService {

    private static final String SOURCE_TOPIC = "sourceTopic";
    private static final String EVENT_CODE_PATH = "eventCodePath";
    private static final String PAYLOAD_PATH = "payloadPath";
    private static final int SCHEMA_VERSION = 1;

    private final DriverSenderService driverSenderService;
    private final DriverMetadata driverMetadata;
    private final DeviceMetadata deviceMetadata;
    private final EventFacade eventFacade;

    /**
     * Processes a single MQTT message received from the broker.
     * <p>
     * This method parses the MQTT message payload into a PointValue object, sets the
     * creation time, and forwards it to the platform.
     * </p>
     *
     * @param mqttMessage the MQTT message containing topic, payload, and metadata
     */
    @Override
    public void receiveValue(MqttMessage mqttMessage) {
        // do something to process your mqtt messages
        log.debug("MQTT message received, topic={}, qos={}, payloadLength={}", topicOf(mqttMessage), qosOf(mqttMessage),
                payloadLengthOf(mqttMessage));
        if (reportConfiguredEvents(mqttMessage) > 0) {
            return;
        }

        PointValue pointValue = toPointValue(mqttMessage);
        if (Objects.isNull(pointValue)) {
            return;
        }
        driverSenderService.pointValueSender(pointValue);
        log.debug("MQTT point value forwarded, topic={}, deviceId={}, pointId={}", topicOf(mqttMessage),
                pointValue.getDeviceId(), pointValue.getPointId());
    }

    /**
     * Processes a batch of MQTT messages received from the broker.
     * <p>
     * This method parses multiple MQTT messages into PointValue objects, sets their
     * creation times, and forwards them as a batch to the platform.
     * </p>
     *
     * @param mqttMessageList list of MQTT messages to process
     */
    @Override
    public void receiveValues(List<MqttMessage> mqttMessageList) {
        // do something to process your mqtt messages
        log.debug("MQTT message batch received, count={}", mqttMessageList.size());
        List<PointValue> pointValues = mqttMessageList.stream()
                .filter(mqttMessage -> reportConfiguredEvents(mqttMessage) == 0)
                .map(this::toPointValue)
                .filter(Objects::nonNull)
                .toList();
        if (!pointValues.isEmpty()) {
            driverSenderService.pointValueSender(pointValues);
        }
        log.debug("MQTT point value batch forwarded, count={}", pointValues.size());
    }

    private int reportConfiguredEvents(MqttMessage mqttMessage) {
        if (driverMetadata.getEventAttributeIdMap().isEmpty()) {
            return 0;
        }

        Object payloadRoot = parsePayload(mqttMessage.getPayload());
        if (Objects.isNull(payloadRoot)) {
            return 0;
        }

        String topic = topicOf(mqttMessage);
        int reported = 0;
        for (Long deviceId : driverMetadata.getDeviceIds()) {
            DeviceBO device = deviceMetadata.getCache(deviceId);
            if (Objects.isNull(device) || Objects.isNull(device.getEventAttributeConfigIdMap())
                    || device.getEventAttributeConfigIdMap().isEmpty()) {
                continue;
            }

            Map<Long, Map<String, AttributeBO>> eventConfigMap = deviceMetadata.getEventConfig(deviceId);
            for (Map.Entry<Long, Map<String, AttributeBO>> entry : eventConfigMap.entrySet()) {
                Long eventId = entry.getKey();
                Map<String, AttributeBO> eventConfig = entry.getValue();
                String sourceTopic = getConfigValue(eventConfig, SOURCE_TOPIC);
                if (!topicMatches(sourceTopic, topic)) {
                    continue;
                }

                FacadeEventBO event = eventFacade.getById(device.getTenantId(), eventId);
                EventReportDTO report = buildEventReport(device, event, eventConfig, payloadRoot, topic);
                if (Objects.isNull(report)) {
                    continue;
                }

                driverSenderService.eventReportSender(report);
                reported++;
                log.info("MQTT event reported, topic={}, deviceId={}, eventId={}, eventCode={}, paramValues={}",
                        topic, device.getId(), eventId, report.eventCode(), JsonUtil.toJsonString(report.paramValues()));
            }
        }
        return reported;
    }

    private EventReportDTO buildEventReport(DeviceBO device, FacadeEventBO event, Map<String, AttributeBO> eventConfig,
                                            Object payloadRoot, String topic) {
        if (Objects.isNull(event) || !EnableFlagEnum.ENABLE.equals(event.getEnableFlag())) {
            return null;
        }

        String eventCode = valueToString(resolvePath(payloadRoot,
                StringUtils.defaultIfBlank(getConfigValue(eventConfig, EVENT_CODE_PATH), "$.eventCode")));
        if (StringUtils.isNotBlank(eventCode) && !Objects.equals(eventCode, event.getEventCode())) {
            return null;
        }

        Object payloadValue = resolvePath(payloadRoot,
                StringUtils.defaultIfBlank(getConfigValue(eventConfig, PAYLOAD_PATH), "$.payload"));
        return EventReportDTO.builder()
                .recordId(UUID.randomUUID().toString())
                .tenantId(device.getTenantId())
                .deviceId(device.getId())
                .eventId(event.getId())
                .eventCode(StringUtils.defaultIfBlank(eventCode, event.getEventCode()))
                .eventTypeFlag(event.getEventTypeFlag().getIndex())
                .eventLevelFlag(event.getEventLevelFlag().getIndex())
                .paramValues(toParamValues(payloadValue))
                .configSnapshot(buildConfigSnapshot(eventConfig))
                .message("MQTT event " + event.getEventCode() + " from topic " + StringUtils.defaultString(topic))
                .occurTime(Instant.now())
                .schemaVersion(SCHEMA_VERSION)
                .build();
    }

    private PointValue toPointValue(MqttMessage mqttMessage) {
        try {
            PointValue pointValue = JsonUtil.parseObject(mqttMessage.getPayload(), PointValue.class);
            if (Objects.nonNull(pointValue)) {
                pointValue.setCreateTime(LocalDateTimeUtil.now());
            }
            return pointValue;
        } catch (Exception e) {
            log.warn("MQTT point value parse failed, topic={}, payloadLength={}", topicOf(mqttMessage),
                    payloadLengthOf(mqttMessage), e);
            return null;
        }
    }

    private Object parsePayload(String payload) {
        if (StringUtils.isBlank(payload)) {
            return null;
        }
        if (!JsonUtil.isJson(payload)) {
            return payload;
        }
        return JsonUtil.parseObject(payload, Object.class);
    }

    private Object resolvePath(Object root, String path) {
        if (Objects.isNull(root) || StringUtils.isBlank(path)) {
            return null;
        }
        if ("$".equals(path)) {
            return root;
        }

        String normalized = path.startsWith("$.") ? path.substring(2) : path;
        Object current = root;
        for (String segment : normalized.split("\\.")) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(segment);
            } else {
                return null;
            }
        }
        return current;
    }

    private Map<String, String> toParamValues(Object payloadValue) {
        Map<String, String> paramValues = new LinkedHashMap<>();
        if (payloadValue instanceof Map<?, ?> payloadMap) {
            payloadMap.forEach((key, value) -> paramValues.put(String.valueOf(key), valueToString(value)));
        } else if (Objects.nonNull(payloadValue)) {
            paramValues.put("value", valueToString(payloadValue));
        }
        return paramValues;
    }

    private String buildConfigSnapshot(Map<String, AttributeBO> eventConfig) {
        if (Objects.isNull(eventConfig) || eventConfig.isEmpty()) {
            return null;
        }

        Map<String, Map<String, String>> snapshot = new LinkedHashMap<>();
        eventConfig.forEach((attributeCode, attribute) -> {
            Map<String, String> item = new LinkedHashMap<>();
            if (Objects.nonNull(attribute)) {
                item.put("type", Objects.nonNull(attribute.getType()) ? attribute.getType().getCode() : null);
                item.put("configValue", attribute.getValue());
            }
            snapshot.put(attributeCode, item);
        });
        return JsonUtil.toJsonString(snapshot);
    }

    private String getConfigValue(Map<String, AttributeBO> config, String code) {
        if (Objects.isNull(config) || Objects.isNull(config.get(code))) {
            return null;
        }
        return config.get(code).getValue();
    }

    private String valueToString(Object value) {
        if (Objects.isNull(value)) {
            return "";
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return JsonUtil.toJsonString(value);
    }

    private boolean topicMatches(String filter, String topic) {
        if (StringUtils.isBlank(filter) || StringUtils.isBlank(topic)) {
            return false;
        }
        if (Objects.equals(filter, topic)) {
            return true;
        }

        String[] filterSegments = filter.split("/");
        String[] topicSegments = topic.split("/");
        for (int i = 0; i < filterSegments.length; i++) {
            String filterSegment = filterSegments[i];
            if ("#".equals(filterSegment)) {
                return i == filterSegments.length - 1;
            }
            if (i >= topicSegments.length) {
                return false;
            }
            if (!"+".equals(filterSegment) && !Objects.equals(filterSegment, topicSegments[i])) {
                return false;
            }
        }
        return filterSegments.length == topicSegments.length;
    }

    private String topicOf(MqttMessage mqttMessage) {
        MessageHeader header = mqttMessage.getHeader();
        return Objects.isNull(header) ? null : header.getMqttReceivedTopic();
    }

    private Integer qosOf(MqttMessage mqttMessage) {
        MessageHeader header = mqttMessage.getHeader();
        return Objects.isNull(header) ? null : header.getMqttReceivedQos();
    }

    private int payloadLengthOf(MqttMessage mqttMessage) {
        String payload = mqttMessage.getPayload();
        return Objects.isNull(payload) ? 0 : payload.length();
    }

}
