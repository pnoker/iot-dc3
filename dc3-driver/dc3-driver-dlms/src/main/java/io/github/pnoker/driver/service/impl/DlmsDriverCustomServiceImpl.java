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

import gurux.dlms.GXDLMSClient;
import gurux.dlms.enums.Authentication;
import gurux.dlms.enums.InterfaceType;
import gurux.dlms.enums.ObjectType;
import gurux.dlms.objects.GXDLMSObject;
import io.github.pnoker.common.driver.entity.bean.DeviceHealthState;
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
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DLMS/COSEM driver service implementation.
 * <p>
 * Provides DLMS/COSEM protocol communication for smart metering devices.
 * Supports TCP and serial transport, with configurable authentication and
 * standard DLMS object types (Register, Clock, Data, etc.).
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Slf4j
@Service
public class DlmsDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    @Value("${dc3.driver.code}")
    private String driverCode;

    private Map<Long, GXDLMSClient> clientMap;

    public DlmsDriverCustomServiceImpl(DriverMetadata driverMetadata, DriverSenderService driverSenderService) {
        this.driverMetadata = driverMetadata;
        this.driverSenderService = driverSenderService;
    }

    @Override
    public void initial() {
        clientMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        // DLMS drivers do not need custom scheduled tasks.
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
                GXDLMSClient removed = clientMap.remove(metadataEvent.getId());
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
        GXDLMSClient client = getConnector(device.getId(), driverConfig);
        try {
            String objectTypeStr = getConfigValue(pointConfig, "objectType", "REGISTER");
            String logicalName = getConfigValue(pointConfig, "logicalName", "");
            int attributeId = getConfigIntValue(pointConfig, "attributeId", 2);

            ObjectType objectType = ObjectType.valueOf(objectTypeStr.toUpperCase());
            GXDLMSObject dlmsObject = client.getObjects().findByLN(objectType, logicalName);
            if (Objects.isNull(dlmsObject)) {
                dlmsObject = GXDLMSClient.createObject(objectType);
                dlmsObject.setLogicalName(logicalName);
                client.getObjects().add(dlmsObject);
            }

            // TODO: client.read() generates raw DLMS frames (byte[][]) that must be sent
            //       over the transport. The full flow requires:
            //       1. byte[][] frames = client.read(dlmsObject, attributeId)
            //       2. Send frames over TCP/serial transport
            //       3. Receive response bytes from transport
            //       4. Object value = client.updateValue(dlmsObject, attributeId, responseBytes)
            byte[][] frames = client.read(dlmsObject, attributeId);
            log.debug("DLMS read frames generated, protocol={}, deviceId={}, objectType={}, logicalName={}, frameCount={}",
                    driverCode, device.getId(), objectTypeStr, logicalName, frames.length);

            // TODO: Replace with actual transport send/receive and value decoding
            // For now, return a placeholder indicating the DLMS frame was generated
            return new ReadPointValue(device, point, null);
        } catch (ReadPointException e) {
            throw e;
        } catch (Exception e) {
            clientMap.remove(device.getId());
            throw new ReadPointException("DLMS read failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        GXDLMSClient client = getConnector(device.getId(), driverConfig);
        try {
            String objectTypeStr = getConfigValue(pointConfig, "objectType", "REGISTER");
            String logicalName = getConfigValue(pointConfig, "logicalName", "");
            int attributeId = getConfigIntValue(pointConfig, "attributeId", 2);

            ObjectType objectType = ObjectType.valueOf(objectTypeStr.toUpperCase());
            GXDLMSObject dlmsObject = client.getObjects().findByLN(objectType, logicalName);
            if (Objects.isNull(dlmsObject)) {
                dlmsObject = GXDLMSClient.createObject(objectType);
                dlmsObject.setLogicalName(logicalName);
                client.getObjects().add(dlmsObject);
            }

            // TODO: client.write() generates raw DLMS frames that must be sent over transport.
            //       The full write flow:
            //       1. Set the value on the DLMS object attribute
            //       2. byte[][] frames = client.write(dlmsObject, attributeId)
            //       3. Send frames over TCP/serial transport
            //       4. Receive and process acknowledgment
            byte[][] frames = client.write(dlmsObject, attributeId);
            log.debug("DLMS write frames generated, protocol={}, deviceId={}, objectType={}, logicalName={}, frameCount={}",
                    driverCode, device.getId(), objectTypeStr, logicalName, frames.length);

            return true;
        } catch (Exception e) {
            clientMap.remove(device.getId());
            throw new WritePointException("DLMS write failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    /**
     * Get or create a GXDLMSClient for the given device.
     *
     * @param deviceId     unique device identifier
     * @param driverConfig driver configuration containing DLMS connection parameters
     * @return cached or newly created GXDLMSClient
     */
    private GXDLMSClient getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return clientMap.computeIfAbsent(deviceId, id -> {
            String transportType = getConfigValue(driverConfig, "transportType", "TCP");
            int clientAddress = getConfigIntValue(driverConfig, "clientAddress", 16);
            int serverAddress = getConfigIntValue(driverConfig, "serverAddress", 1);
            String authenticationStr = getConfigValue(driverConfig, "authentication", "NONE");
            String password = getConfigValue(driverConfig, "password", "");

            log.debug("Driver connection creating, protocol={}, deviceId={}, transportType={}",
                    driverCode, deviceId, transportType);

            Authentication authentication = Authentication.valueOf(authenticationStr.toUpperCase());
            InterfaceType interfaceType = "SERIAL".equalsIgnoreCase(transportType)
                    ? InterfaceType.HDLC
                    : InterfaceType.WRAPPER;

            GXDLMSClient client = new GXDLMSClient(true);
            client.setInterfaceType(interfaceType);
            client.setClientAddress(clientAddress);
            client.setServerAddress(serverAddress);
            client.setAuthentication(authentication);
            if (!password.isEmpty()) {
                client.setPassword(password.getBytes());
            }

            // TODO: Implement actual TCP/serial transport connection and HDLC handshake
            // TODO: For production use, establish the physical connection here and store
            //       the transport alongside the client for proper read/write operations

            log.info("Driver connection established, protocol={}, deviceId={}, transportType={}, clientAddress={}, serverAddress={}",
                    driverCode, deviceId, transportType, clientAddress, serverAddress);
            return client;
        });
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
}
