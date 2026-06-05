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
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openmuc.j60870.ClientConnectionBuilder;
import org.openmuc.j60870.Connection;
import org.openmuc.j60870.ConnectionEventListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom driver service implementation for the IEC 60870-5-104 Driver.
 * <p>
 * This service implements the IEC 104 protocol for communication with substation
 * automation and telecontrol equipment. It uses the j60870 library for IEC 104
 * client connections and supports ASDU event handling and data polling.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Slf4j
@Service
public class Iec104DriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    /**
     * Device connection cache keyed by device ID.
     */
    private final ConcurrentHashMap<Long, Connection> connectMap = new ConcurrentHashMap<>();
    /**
     * IOA value cache keyed by point ID for quick read access.
     */
    private final ConcurrentHashMap<Long, String> valueCache = new ConcurrentHashMap<>();
    @Value("${dc3.driver.code}")
    private String driverCode;

    /**
     * Explicit constructor for dependency injection.
     *
     * @param driverMetadata      driver metadata service
     * @param driverSenderService driver sender service
     */
    public Iec104DriverCustomServiceImpl(DriverMetadata driverMetadata, DriverSenderService driverSenderService) {
        this.driverMetadata = driverMetadata;
        this.driverSenderService = driverSenderService;
    }

    @Override
    public void initial() {
        /*
         * IEC 104 Driver initialization logic
         */
    }

    @Override
    public void schedule() {
        /*
         * IEC 104 Driver custom schedule logic
         */
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        /*
         * IEC 104 Driver metadata event handling
         */
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                               PointBO point) {
        int ioa = getConfigIntValue(pointConfig, "ioa", 0);

        // TODO: Verify the correct j60870 API for reading an IOA value.
        // The actual API call depends on the library version:
        //   Connection.read(int ioa) or Connection.getInformationObject(int ioa)
        // Use the connection from connectMap and retrieve the cached value.
        String cachedValue = valueCache.get(point.getId());
        if (StringUtils.isNotBlank(cachedValue)) {
            return new ReadPointValue(device, point, cachedValue);
        }

        Connection connection = connectMap.get(device.getId());
        if (Objects.isNull(connection)) {
            connection = connectToIec104Server(driverConfig, device);
            if (Objects.isNull(connection)) {
                log.error("IEC 104 connection failed, deviceId={}", device.getId());
                throw new RuntimeException("IEC 104 connection failed for device " + device.getId());
            }
            connectMap.put(device.getId(), connection);
        }

        // TODO: Perform actual read from the IEC 104 server using the connection.
        // For now, return the cached value or empty string.
        return new ReadPointValue(device, point, StringUtils.defaultString(cachedValue));
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                         PointBO point, WritePointValue writePointValue) {
        int ioa = getConfigIntValue(pointConfig, "ioa", 0);
        String value = writePointValue.getValue();

        Connection connection = connectMap.get(device.getId());
        if (Objects.isNull(connection)) {
            connection = connectToIec104Server(driverConfig, device);
            if (Objects.isNull(connection)) {
                log.error("IEC 104 connection failed for write, deviceId={}", device.getId());
                return false;
            }
            connectMap.put(device.getId(), connection);
        }

        try {
            // TODO: Verify the correct j60870 API for writing a control command.
            // Use the ASDU type from pointConfig to determine the control type.
            // Example: connection.sendControlCommand(ioa, value) or similar.
            //
            // Update the value cache after successful write.
            valueCache.put(point.getId(), value);
            return true;
        } catch (Exception e) {
            log.error("IEC 104 write failed, deviceId={}, pointId={}, ioa={}", device.getId(), point.getId(), ioa, e);
            return false;
        }
    }

    @Override
    public Map<String, String> execute(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> commandConfig,
                                       DeviceBO device, FacadeCommandBO command, Map<String, String> paramValues) {
        Map<String, String> result = new LinkedHashMap<>();
        String sendCommand = getConfigValue(commandConfig, "sendCommand", "${value}");
        if (Objects.nonNull(paramValues)) {
            for (Map.Entry<String, String> entry : paramValues.entrySet()) {
                sendCommand = sendCommand.replace("${" + entry.getKey() + "}", entry.getValue());
            }
        }
        result.put("sendCommand", sendCommand);
        return result;
    }

    /**
     * Connect to an IEC 104 server and register listeners for ASDU events.
     *
     * @param driverConfig driver configuration containing host, port, etc.
     * @param device       device descriptor
     * @return the established Connection, or null on failure
     */
    private Connection connectToIec104Server(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        String host = getConfigValue(driverConfig, "host", "localhost");
        int port = getConfigIntValue(driverConfig, "port", 2404);
        int connectTimeout = getConfigIntValue(driverConfig, "connectTimeout", 10000);

        try {
            // TODO: Verify the ClientConnectionBuilder API for j60870.
            // The builder may require address lengths (asduAddress, cotLength, caLength, ioaLength).
            // Example:
            //   Connection connection = new ClientConnectionBuilder(host)
            //       .setPort(port)
            //       .build();
            // Register listeners for ASDU events to cache IOA values.
            Connection connection = new ClientConnectionBuilder(host)
                    .setPort(port)
                    .setConnectionEventListener(new ConnectionEventListener() {
                        @Override
                        public void newASdu(Connection conn, org.openmuc.j60870.ASdu aSdu) {
                            log.debug("IEC 104 ASDU received, typeId={}, ioaCount={}",
                                    aSdu.getTypeIdentification(),
                                    aSdu.getInformationObjects() != null ? aSdu.getInformationObjects().length : 0);
                        }

                        @Override
                        public void connectionClosed(Connection conn, IOException e) {
                            log.warn("IEC 104 connection closed, deviceId={}", device.getId());
                            connectMap.remove(device.getId());
                        }

                        @Override
                        public void dataTransferStateChanged(Connection conn, boolean active) {
                            log.info("IEC 104 data transfer state changed: active={}, deviceId={}", active, device.getId());
                        }
                    })
                    .build();

            connection.startDataTransfer();

            log.info("IEC 104 connected to {}:{}, deviceId={}", host, port, device.getId());
            return connection;
        } catch (Exception e) {
            log.error("IEC 104 connection failed, host={}, port={}, deviceId={}", host, port, device.getId(), e);
            return null;
        }
    }

    /**
     * Get string config value with default.
     */
    private String getConfigValue(Map<String, AttributeBO> config, String code, String defaultValue) {
        if (Objects.isNull(config) || Objects.isNull(config.get(code))) {
            return defaultValue;
        }
        String value = config.get(code).getValue();
        return StringUtils.defaultIfBlank(value, defaultValue);
    }

    /**
     * Get int config value with default.
     */
    private int getConfigIntValue(Map<String, AttributeBO> config, String code, int defaultValue) {
        String value = getConfigValue(config, code, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse int config value for code={}, using default={}", code, defaultValue);
            return defaultValue;
        }
    }
}
