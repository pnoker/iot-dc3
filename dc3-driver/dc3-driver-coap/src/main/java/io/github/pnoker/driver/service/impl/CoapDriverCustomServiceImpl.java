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

import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
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
import io.github.pnoker.driver.coap.client.CoapClientManager;
import io.github.pnoker.driver.coap.entity.CoapResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CoAP Driver Custom Service Implementation
 * <p>
 * Unlike MQTT's pub/sub model, CoAP supports active request-response communication.
 * The read method performs CoAP GET requests and the write method performs CoAP PUT requests.
 *
 * @author pnoker
 * @version 2026.5.0
 * @since 2026.5.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoapDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    private final CoapClientManager coapClientManager;
    private final Map<Long, String> deviceUriMap = new ConcurrentHashMap<>();
    @Value("${dc3.driver.code}")
    private String driverCode;

    @Override
    public void initial() {
        log.info("CoAP driver initialized");
    }

    @Override
    public void schedule() {
        // Device state lease renewal is owned by the SDK device health job.
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, deviceId={}", driverCode, 
                    metadataType, operateType, metadataEvent.getId());
            if (MetadataOperateTypeEnum.DELETE.equals(operateType)
                    || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                releaseDeviceClient(metadataEvent.getId());
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}", driverCode, 
                    metadataType, operateType, metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        String deviceHost = getConfigValue(driverConfig, "deviceHost", "localhost");
        int devicePort = getConfigIntValue(driverConfig, "devicePort", 5683);
        String readPath = getConfigValue(pointConfig, "readPath", "/sensors");

        String uri = buildUri(deviceHost, devicePort);
        rememberDeviceUri(device.getId(), uri);
        log.debug("CoAP read: uri={}, path={}, deviceId={}, pointId={}", uri, readPath, device.getId(), point.getId());

        CoapResult response = coapClientManager.get(uri, readPath);
        if (response == null || !response.isSuccess()) {
            log.warn("CoAP read failed, uri={}, path={}, statusCode={}", uri, readPath,
                    response != null ? response.getStatusCode() : "timeout");
            return null;
        }

        return new ReadPointValue(device, point, response.getPayload());
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue values) {
        String deviceHost = getConfigValue(driverConfig, "deviceHost", "localhost");
        int devicePort = getConfigIntValue(driverConfig, "devicePort", 5683);
        String writePath = getConfigValue(pointConfig, "writePath", "/actuators");
        String value = values.getValue();

        String uri = buildUri(deviceHost, devicePort);
        rememberDeviceUri(device.getId(), uri);
        log.debug("CoAP write: uri={}, path={}, deviceId={}, pointId={}, valueLength={}",
                uri, writePath, device.getId(), point.getId(), value != null ? value.length() : 0);

        CoapResult response = coapClientManager.put(uri, writePath, value);
        if (response == null || !response.isSuccess()) {
            log.warn("CoAP write failed, uri={}, path={}, statusCode={}", uri, writePath,
                    response != null ? response.getStatusCode() : "timeout");
            return false;
        }

        return true;
    }

    private String buildUri(String host, int port) {
        return "coap:" + SymbolConstant.DOUBLE_SLASH + host + SymbolConstant.COLON + port;
    }

    private String getConfigValue(Map<String, AttributeBO> config, String key, String defaultValue) {
        AttributeBO attribute = config.get(key);
        if (attribute == null || StringUtils.isBlank(attribute.getValue(String.class))) {
            return defaultValue;
        }
        return attribute.getValue(String.class);
    }

    private int getConfigIntValue(Map<String, AttributeBO> config, String key, int defaultValue) {
        AttributeBO attribute = config.get(key);
        if (attribute == null) {
            return defaultValue;
        }
        try {
            return attribute.getValue(Integer.class);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void rememberDeviceUri(Long deviceId, String uri) {
        if (Objects.isNull(deviceId) || StringUtils.isBlank(uri)) {
            return;
        }
        String previousUri = deviceUriMap.put(deviceId, uri);
        if (StringUtils.isNotBlank(previousUri) && !Objects.equals(previousUri, uri)) {
            coapClientManager.releaseClient(previousUri);
        }
    }

    private void releaseDeviceClient(Long deviceId) {
        String uri = deviceUriMap.remove(deviceId);
        if (StringUtils.isNotBlank(uri)) {
            coapClientManager.releaseClient(uri);
        }
    }

}
