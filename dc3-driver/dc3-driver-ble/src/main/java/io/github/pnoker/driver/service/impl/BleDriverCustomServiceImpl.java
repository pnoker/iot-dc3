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
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bluetooth LE driver service implementation.
 * <p>
 * Uses the Sputnikdev Bluetooth Manager with TinyB transport to connect
 * to BLE devices, read GATT characteristic values, and write values.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Slf4j
@Service
public class BleDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    @Value("${dc3.driver.code}")
    private String driverCode;

    private BluetoothManager bluetoothManager;
    private Map<Long, DeviceGovernor> deviceGovernorMap;

    public BleDriverCustomServiceImpl(DriverMetadata driverMetadata, DriverSenderService driverSenderService) {
        this.driverMetadata = driverMetadata;
        this.driverSenderService = driverSenderService;
    }

    private static int readInt16(byte[] data, String byteOrder) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        if ("LITTLE".equalsIgnoreCase(byteOrder)) bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getShort();
    }

    private static int readUint16(byte[] data, String byteOrder) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        if ("LITTLE".equalsIgnoreCase(byteOrder)) bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getShort() & 0xFFFF;
    }

    private static float readFloat(byte[] data, String byteOrder) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        if ("LITTLE".equalsIgnoreCase(byteOrder)) bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getFloat();
    }

    private static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Override
    public void initial() {
        deviceGovernorMap = new ConcurrentHashMap<>(16);
        bluetoothManager = new BluetoothManagerBuilder()
                .withTinyBTransport(true)
                .withIgnoreTransportInitErrors(true)
                .withStarted(true)
                .withDiscovering(false)
                .build();
        log.info("BLE driver initialized, protocol={}", driverCode);
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
        DeviceGovernor governor = deviceGovernorMap.get(device.getId());
        if (Objects.isNull(governor)) {
            return DeviceHealthState.offline();
        }
        try {
            return governor.isOnline() && governor.isConnected()
                    ? DeviceHealthState.online()
                    : DeviceHealthState.offline();
        } catch (Exception e) {
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
                DeviceGovernor removed = deviceGovernorMap.remove(metadataEvent.getId());
                if (Objects.nonNull(removed)) {
                    removed.setConnectionControl(false);
                    log.info("Driver device control released, protocol={}, deviceId={}, operateType={}",
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
        try {
            String serviceUuid = getRequiredConfig(pointConfig, "serviceUuid");
            String characteristicUuid = getRequiredConfig(pointConfig, "characteristicUuid");
            String readFormat = getConfigValue(pointConfig, "readFormat", "UTF8");
            String byteOrder = getConfigValue(pointConfig, "byteOrder", "BIG");

            CharacteristicGovernor gov = getCharacteristicGovernor(device.getId(), driverConfig,
                    serviceUuid, characteristicUuid);

            byte[] data = gov.read();
            if (Objects.isNull(data) || data.length == 0) {
                return null;
            }

            String value = parseBytes(data, readFormat, byteOrder);
            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            throw e;
        } catch (Exception e) {
            throw new ReadPointException("BLE read failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        try {
            String serviceUuid = getRequiredConfig(pointConfig, "serviceUuid");
            String characteristicUuid = getRequiredConfig(pointConfig, "characteristicUuid");

            CharacteristicGovernor gov = getCharacteristicGovernor(device.getId(), driverConfig,
                    serviceUuid, characteristicUuid);

            byte[] data = writePointValue.getValue(String.class).getBytes(StandardCharsets.UTF_8);
            return gov.write(data);
        } catch (Exception e) {
            throw new WritePointException("BLE write failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    private CharacteristicGovernor getCharacteristicGovernor(Long deviceId, Map<String, AttributeBO> driverConfig,
                                                             String serviceUuid, String characteristicUuid) {
        String deviceAddress = getRequiredConfig(driverConfig, "deviceAddress");
        String adapterName = getConfigValue(driverConfig, "adapterName", "hci0");

        deviceGovernorMap.computeIfAbsent(deviceId, id -> {
            URL deviceUrl = new URL(adapterName, deviceAddress);
            DeviceGovernor governor = bluetoothManager.getDeviceGovernor(deviceUrl, true);
            governor.setConnectionControl(true);
            log.info("BLE device governor created, protocol={}, deviceId={}", driverCode, deviceId);
            return governor;
        });

        URL charUrl = new URL(adapterName, deviceAddress, serviceUuid, characteristicUuid);
        return bluetoothManager.getCharacteristicGovernor(charUrl, true);
    }

    private String parseBytes(byte[] data, String format, String byteOrder) {
        return switch (format.toUpperCase()) {
            case "HEX" -> bytesToHex(data);
            case "INT16" -> String.valueOf(readInt16(data, byteOrder));
            case "UINT16" -> String.valueOf(readUint16(data, byteOrder));
            case "FLOAT" -> String.valueOf(readFloat(data, byteOrder));
            default -> new String(data, StandardCharsets.UTF_8);
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
}
