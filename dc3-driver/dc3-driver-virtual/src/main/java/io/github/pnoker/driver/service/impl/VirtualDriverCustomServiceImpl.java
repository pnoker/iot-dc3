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
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.dto.EventReportDTO;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.facade.api.EventFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.facade.entity.bo.FacadeEventBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeEventQuery;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom driver service implementation for the Virtual Driver.
 * <p>
 * This service provides virtual device simulation capabilities for testing and
 * development. It generates random data for different point types and simulates device
 * behavior without requiring physical hardware connections.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualDriverCustomServiceImpl implements DriverCustomService {

    private static final String PAYLOAD_TEMPLATE = "payloadTemplate";
    private static final String RESPONSE_TEMPLATE = "responseTemplate";
    private static final String EVENT_CODE_PATH = "eventCodePath";
    private static final String PAYLOAD_PATH = "payloadPath";
    private static final long EVENT_REPORT_INTERVAL_MILLIS = 30_000L;
    private static final int SCHEMA_VERSION = 1;

    private final DriverMetadata driverMetadata;
    private final DeviceMetadata deviceMetadata;
    private final DriverSenderService driverSenderService;
    private final EventFacade eventFacade;
    private final AtomicLong lastEventReportMillis = new AtomicLong();
    @Value("${dc3.driver.code}")
    private String driverCode;

    /**
     * Initializes the virtual driver.
     * <p>
     * This method is called when the driver starts. Override this method to implement
     * custom initialization logic specific to your virtual device simulation.
     * </p>
     */
    @Override
    public void initial() {
        /*
         * Driver initialization logic
         *
         * Hint: The logic here is for reference only; please modify it according to the
         * actual application scenario. This method is automatically executed when the
         * driver starts, and you can perform specific initialization operations here.
         *
         */
    }

    @Override
    public void schedule() {
        if (!shouldReportEvent()) {
            return;
        }

        driverMetadata.getDeviceIds().forEach(this::reportDeviceEvents);
    }

    /**
     * Handles metadata change events for drivers, devices, and points.
     * <p>
     * This method is called when metadata is created, updated, or deleted. Override this
     * method to implement custom event handling logic.
     * </p>
     *
     * @param metadataEvent the metadata event containing type, operation, and ID
     *                      information
     */
    @Override
    public void event(MetadataEventDTO metadataEvent) {
        /*
         * Receive metadata events for driver, device, and point creation, update, and
         * deletion.
         *
         * Metadata type: {@link MetadataTypeEnum} (DRIVER, DEVICE, POINT) Metadata
         * operation type: {@link MetadataOperateTypeEnum} (ADD, DELETE, UPDATE)
         *
         * Hint: The logic here is for reference only; please modify it according to the
         * actual application scenario.
         */
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            // to do something for device event
            log.info("Driver metadata event received, protocol=" + driverCode + ", metadataType={}, operateType={}, deviceId={}",
                    metadataType, operateType, metadataEvent.getId());
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            // to do something for point event
            log.info("Driver metadata event received, protocol=" + driverCode + ", metadataType={}, operateType={}, pointId={}",
                    metadataType, operateType, metadataEvent.getId());
        }
    }

    /**
     * Reads data from a virtual device point.
     * <p>
     * This method generates simulated data based on the point type:
     * <ul>
     * <li>STRING type: returns "abcd1234"</li>
     * <li>BOOLEAN type: returns a random boolean value</li>
     * <li>Other types: returns a random float between 0 and 100</li>
     * </ul>
     * Override this method to implement custom data generation logic.
     * </p>
     *
     * @param driverConfig driver configuration attributes
     * @param pointConfig  point configuration attributes
     * @param device       the device to read from
     * @param point        the point to read
     * @return the read value wrapped in a ReadPointValue object
     */
    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                               PointBO point) {
        /*
         * Read device point data logic
         *
         * Hint: The logic here is for reference only; please modify it according to the
         * actual application scenario. Generate random data based on point type: - If the
         * point type is STRING, generate a random string of length 8; - If the point type
         * is BOOLEAN, generate a random boolean value; - Otherwise, generate a random
         * float between 0 and 100.
         */
        if (PointTypeFlagEnum.STRING.equals(point.getPointTypeFlag())) {
            return new ReadPointValue(device, point, "abcd1234");
        }
        if (PointTypeFlagEnum.BOOLEAN.equals(point.getPointTypeFlag())) {
            return new ReadPointValue(device, point, String.valueOf(ThreadLocalRandom.current().nextBoolean()));
        }

        double value = ThreadLocalRandom.current().nextDouble() * 100;
        return new ReadPointValue(device, point, String.valueOf(value));
    }

    /**
     * Writes data to a virtual device point.
     * <p>
     * By default, this method returns false indicating the write operation is not
     * supported. Override this method to implement custom write logic for virtual
     * devices.
     * </p>
     *
     * @param driverConfig    driver configuration attributes
     * @param pointConfig     point configuration attributes
     * @param device          the device to write to
     * @param point           the point to write
     * @param writePointValue the value to write
     * @return true if the write operation succeeded, false otherwise
     */
    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                         PointBO point, WritePointValue writePointValue) {
        /*
         * Write device point data logic
         *
         * Hint: The logic here is for reference only; please modify it according to the
         * actual application scenario. You can implement the point data write logic based
         * on specific business requirements. By default, this method returns false,
         * indicating that the write operation was not executed or failed.
         */
        return false;
    }

    @Override
    public Map<String, String> execute(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> commandConfig,
                                       DeviceBO device, FacadeCommandBO command, Map<String, String> paramValues) {
        Map<String, String> context = new LinkedHashMap<>();
        if (Objects.nonNull(paramValues)) {
            context.putAll(paramValues);
        }
        context.put("deviceId", String.valueOf(device.getId()));
        context.put("deviceCode", device.getDeviceCode());
        context.put("deviceName", device.getDeviceName());
        context.put("commandId", String.valueOf(command.getId()));
        context.put("commandCode", command.getCommandCode());
        context.put("commandName", command.getCommandName());

        String payloadTemplate = getConfigValue(commandConfig, PAYLOAD_TEMPLATE);
        String payload = render(StringUtils.defaultString(payloadTemplate), context);
        context.put("payload", payload);

        String responseTemplate = getConfigValue(commandConfig, RESPONSE_TEMPLATE);
        String response = render(StringUtils.defaultIfBlank(responseTemplate, "{}"), context);

        Map<String, String> result = parseResponse(response);
        result.putIfAbsent("payload", payload);
        result.putIfAbsent("response", response);
        log.info("Virtual command executed, deviceId={}, commandId={}, payload={}, response={}",
                device.getId(), command.getId(), payload, response);
        return result;
    }

    private String getConfigValue(Map<String, AttributeBO> config, String code) {
        if (Objects.isNull(config) || Objects.isNull(config.get(code))) {
            return null;
        }
        return config.get(code).getValue();
    }

    private String render(String template, Map<String, String> context) {
        String rendered = template;
        for (Map.Entry<String, String> entry : context.entrySet()) {
            rendered = rendered.replace("${" + entry.getKey() + "}", StringUtils.defaultString(entry.getValue()));
        }
        return rendered;
    }

    private Map<String, String> parseResponse(String response) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!JsonUtil.isJson(response)) {
            result.put("response", response);
            return result;
        }

        Object parsed = JsonUtil.parseObject(response, Object.class);
        if (parsed instanceof Map<?, ?> parsedMap) {
            parsedMap.forEach((key, value) -> result.put(String.valueOf(key), valueToString(value)));
        } else {
            result.put("response", valueToString(parsed));
        }
        return result;
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

    private boolean shouldReportEvent() {
        long now = System.currentTimeMillis();
        long previous = lastEventReportMillis.get();
        return now - previous >= EVENT_REPORT_INTERVAL_MILLIS
                && lastEventReportMillis.compareAndSet(previous, now);
    }

    private void reportDeviceEvents(Long deviceId) {
        DeviceBO device = deviceMetadata.getCache(deviceId);
        if (Objects.isNull(device)) {
            return;
        }

        List<FacadeEventBO> events = listDeviceEvents(device);
        for (FacadeEventBO event : events) {
            Map<String, AttributeBO> eventConfig = deviceMetadata.getEventConfig(device.getId(), event.getId());
            if (eventConfig.isEmpty()) {
                continue;
            }

            EventReportDTO report = buildEventReport(device, event, eventConfig);
            driverSenderService.eventReportSender(report);
            log.info("Virtual event reported, deviceId={}, eventId={}, eventCode={}, paramValues={}",
                    device.getId(), event.getId(), report.eventCode(), JsonUtil.toJsonString(report.paramValues()));
        }
    }

    private List<FacadeEventBO> listDeviceEvents(DeviceBO device) {
        Pages page = new Pages();
        page.setSize(100);
        FacadeEventQuery query = FacadeEventQuery.builder()
                .page(page)
                .tenantId(device.getTenantId())
                .deviceId(device.getId())
                .enableFlag(EnableFlagEnum.ENABLE)
                .build();
        FacadePage<FacadeEventBO> eventPage = eventFacade.listByPage(query);
        if (Objects.isNull(eventPage) || Objects.isNull(eventPage.getRecords())) {
            return List.of();
        }
        return eventPage.getRecords();
    }

    private EventReportDTO buildEventReport(DeviceBO device, FacadeEventBO event, Map<String, AttributeBO> eventConfig) {
        Map<String, Object> rawEvent = new LinkedHashMap<>();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("value", String.valueOf(ThreadLocalRandom.current().nextInt(0, 100)));
        payload.put("deviceCode", device.getDeviceCode());
        payload.put("source", "virtual");
        rawEvent.put("eventCode", event.getEventCode());
        rawEvent.put("payload", payload);

        String eventCode = valueToString(resolvePath(rawEvent,
                StringUtils.defaultIfBlank(getConfigValue(eventConfig, EVENT_CODE_PATH), "$.eventCode")));
        Object payloadValue = resolvePath(rawEvent,
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
                .message("Virtual event " + event.getEventCode() + " from " + device.getDeviceCode())
                .occurTime(Instant.now())
                .schemaVersion(SCHEMA_VERSION)
                .build();
    }

    private Object resolvePath(Object root, String path) {
        if (Objects.isNull(root) || StringUtils.isBlank(path)) {
            return null;
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
        } else {
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

}
