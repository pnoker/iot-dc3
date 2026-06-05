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

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.npdu.ip.IpNetwork;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.RequestUtils;
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
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BACnet IP driver service implementation.
 * <p>
 * Uses BACnet4J to communicate with BACnet/IP devices over UDP.
 * Supports reading and writing BACnet object properties by device instance number,
 * object type, object instance, and property identifier.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Slf4j
@Service
public class BacnetIpDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    @Value("${dc3.driver.code}")
    private String driverCode;

    private Map<Long, LocalDevice> connectMap;

    public BacnetIpDriverCustomServiceImpl(DriverMetadata driverMetadata, DriverSenderService driverSenderService) {
        this.driverMetadata = driverMetadata;
        this.driverSenderService = driverSenderService;
    }

    @Override
    public void initial() {
        connectMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        // No custom schedule needed.
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        try {
            LocalDevice localDevice = connectMap.get(device.getId());
            if (Objects.isNull(localDevice)) {
                return DeviceHealthState.offline();
            }
            return localDevice.isInitialized()
                    ? DeviceHealthState.online()
                    : DeviceHealthState.offline();
        } catch (Exception e) {
            log.warn("BACnet health check failed, protocol={}, deviceId={}", driverCode, device.getId(), e);
            return DeviceHealthState.offline();
        }
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
                LocalDevice removed = connectMap.remove(metadataEvent.getId());
                if (Objects.nonNull(removed)) {
                    removed.terminate();
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
        LocalDevice localDevice = getConnector(device.getId(), driverConfig);
        try {
            int remoteDeviceId = getConfigIntValue(pointConfig, "remoteDeviceId", 0);
            String objectTypeStr = getRequiredConfig(pointConfig, "objectType");
            int objectInstance = getConfigIntValue(pointConfig, "objectInstance", 0);
            String propertyIdStr = getConfigValue(pointConfig, "propertyId", "presentValue");

            RemoteDevice remoteDevice = localDevice.getRemoteDeviceBlocking(remoteDeviceId);
            ObjectType objectType = resolveObjectType(objectTypeStr);
            PropertyIdentifier propertyId = resolvePropertyIdentifier(propertyIdStr);

            ObjectIdentifier oid = new ObjectIdentifier(objectType, objectInstance);
            Encodable value = RequestUtils.readProperty(localDevice, remoteDevice, oid,
                    propertyId, null);

            return new ReadPointValue(device, point,
                    value != null ? value.toString() : null);
        } catch (ReadPointException e) {
            throw e;
        } catch (Exception e) {
            throw new ReadPointException("BACnet read failed, protocol={}, message={}",
                    driverCode, e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        LocalDevice localDevice = getConnector(device.getId(), driverConfig);
        try {
            int remoteDeviceId = getConfigIntValue(pointConfig, "remoteDeviceId", 0);
            String objectTypeStr = getRequiredConfig(pointConfig, "objectType");
            int objectInstance = getConfigIntValue(pointConfig, "objectInstance", 0);
            String propertyIdStr = getConfigValue(pointConfig, "propertyId", "presentValue");
            String value = writePointValue.getValue(String.class);

            RemoteDevice remoteDevice = localDevice.getRemoteDeviceBlocking(remoteDeviceId);
            ObjectType objectType = resolveObjectType(objectTypeStr);
            PropertyIdentifier propertyId = resolvePropertyIdentifier(propertyIdStr);

            ObjectIdentifier oid = new ObjectIdentifier(objectType, objectInstance);
            Encodable encodable = createEncodable(objectType, value);
            RequestUtils.writeProperty(localDevice, remoteDevice, oid, propertyId, encodable);

            return true;
        } catch (Exception e) {
            throw new WritePointException("BACnet write failed, protocol={}, message={}",
                    driverCode, e.getMessage(), e);
        }
    }

    /**
     * Create the correct Encodable value for the given BACnet object type.
     * <p>
     * Analog types → Real (float), Binary types → BinaryPV (active/inactive),
     * Multi-state types → UnsignedInteger, Device → UnsignedInteger, default → Real.
     */
    private Encodable createEncodable(ObjectType objectType, String value) {
        String upperName = objectType.toString().toUpperCase();
        if (upperName.startsWith("BINARY_")) {
            boolean active = "true".equalsIgnoreCase(value)
                    || "1".equals(value)
                    || "active".equalsIgnoreCase(value);
            return active ? BinaryPV.active : BinaryPV.inactive;
        }
        if (upperName.startsWith("MULTI_STATE_") || upperName.startsWith("DEVICE")) {
            try {
                return new UnsignedInteger(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                log.warn("BACnet write: multi-state/device value '{}' is not an integer, falling back to Real", value);
                return new Real(Float.parseFloat(value));
            }
        }
        // Analog types and fallback
        try {
            return new Real(Float.parseFloat(value));
        } catch (NumberFormatException e) {
            // For string values on non-analog fallback objects
            return new CharacterString(value);
        }
    }

    /**
     * Get or create a BACnet LocalDevice for the given device ID.
     */
    private LocalDevice getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return connectMap.computeIfAbsent(deviceId, id -> {
            int localDeviceId = getConfigIntValue(driverConfig, "localDeviceId", 1001);
            String bindAddress = getConfigValue(driverConfig, "bindAddress", "0.0.0.0");
            int port = getConfigIntValue(driverConfig, "port", 47808);
            String broadcastAddress = getConfigValue(driverConfig, "broadcastAddress", "255.255.255.255");
            int timeout = getConfigIntValue(driverConfig, "timeout", 6000);

            log.debug("BACnet connection creating, protocol={}, deviceId={}, localDeviceId={}, bind={}:{}",
                    driverCode, deviceId, localDeviceId, bindAddress, port);

            IpNetwork network = new IpNetworkBuilder()
                    .withLocalBindAddress(bindAddress)
                    .withBroadcast(broadcastAddress, 24)
                    .withPort(port)
                    .withReuseAddress(true)
                    .build();
            DefaultTransport transport = new DefaultTransport(network);
            transport.setTimeout(timeout);
            LocalDevice localDevice = new LocalDevice(localDeviceId, transport);
            try {
                localDevice.initialize();
                log.info("BACnet connection established, protocol={}, deviceId={}, localDeviceId={}",
                        driverCode, deviceId, localDeviceId);
            } catch (Exception e) {
                try {
                    localDevice.terminate();
                } catch (Exception destroyException) {
                    log.warn("BACnet connection destroy failed after init error, protocol={}, deviceId={}",
                            driverCode, deviceId, destroyException);
                }
                throw new ConnectorException("BACnet connection failed, protocol={}, deviceId={}, message={}",
                        driverCode, deviceId, e.getMessage(), e);
            }
            return localDevice;
        });
    }

    /**
     * Resolve an ObjectType from its string name using the static constants in bacnet4j.
     */
    private ObjectType resolveObjectType(String name) {
        Map<String, ObjectType> map = Map.ofEntries(
                Map.entry("ANALOG_INPUT", ObjectType.analogInput),
                Map.entry("ANALOG_OUTPUT", ObjectType.analogOutput),
                Map.entry("ANALOG_VALUE", ObjectType.analogValue),
                Map.entry("BINARY_INPUT", ObjectType.binaryInput),
                Map.entry("BINARY_OUTPUT", ObjectType.binaryOutput),
                Map.entry("BINARY_VALUE", ObjectType.binaryValue),
                Map.entry("MULTI_STATE_INPUT", ObjectType.multiStateInput),
                Map.entry("MULTI_STATE_OUTPUT", ObjectType.multiStateOutput),
                Map.entry("MULTI_STATE_VALUE", ObjectType.multiStateValue),
                Map.entry("DEVICE", ObjectType.device)
        );
        ObjectType result = map.get(name.toUpperCase());
        if (Objects.isNull(result)) {
            return ObjectType.analogInput;
        }
        return result;
    }

    /**
     * Resolve a PropertyIdentifier from its string name.
     */
    private PropertyIdentifier resolvePropertyIdentifier(String name) {
        return switch (name.toUpperCase()) {
            case "PRESENT_VALUE" -> PropertyIdentifier.presentValue;
            case "DESCRIPTION" -> PropertyIdentifier.description;
            case "STATUS_FLAGS" -> PropertyIdentifier.statusFlags;
            case "EVENT_STATE" -> PropertyIdentifier.eventState;
            case "RELIABILITY" -> PropertyIdentifier.reliability;
            case "UNITS" -> PropertyIdentifier.units;
            case "OUT_OF_SERVICE" -> PropertyIdentifier.outOfService;
            default -> PropertyIdentifier.presentValue;
        };
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

    private int getConfigIntValue(Map<String, AttributeBO> config, String code, int defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue())) {
            return defaultValue;
        }
        return attr.getValue(Integer.class);
    }
}
