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
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * CAN bus driver service implementation.
 * <p>
 * Communicates with CAN bus devices on Linux via {@code can-utils} command-line
 * tools ({@code candump}, {@code cansend}). A {@link ProcessBuilder} is used to
 * execute shell commands for both read and write operations.
 * </p>
 * <p>
 * TODO: Native SocketCAN JNI integration for lower-latency frame-level access.
 * The current {@code ProcessBuilder} approach works but incurs per-call
 * process startup overhead.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Slf4j
@Service
public class CanDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;

    @Value("${dc3.driver.code}")
    private String driverCode;

    private Map<Long, Boolean> deviceMap;

    public CanDriverCustomServiceImpl(DriverMetadata driverMetadata,
                                      DriverSenderService driverSenderService) {
        this.driverMetadata = driverMetadata;
        this.driverSenderService = driverSenderService;
    }

    @Override
    public void initial() {
        deviceMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        // CAN bus drivers do not need custom scheduled tasks.
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        String interfaceName = getConfigValue(driverConfig, "interfaceName", "can0");
        try {
            Process process = new ProcessBuilder("ip", "link", "show", interfaceName)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            int exitCode = finished ? process.exitValue() : -1;
            return exitCode == 0 ? DeviceHealthState.online() : DeviceHealthState.offline();
        } catch (Exception e) {
            log.warn("CAN interface health check failed, interface={}", interfaceName, e);
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
                deviceMap.remove(metadataEvent.getId());
                log.info("Driver connection destroyed, protocol={}, deviceId={}, operateType={}",
                        driverCode, metadataEvent.getId(), operateType);
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        String interfaceName = getConfigValue(driverConfig, "interfaceName", "can0");
        String canId = getConfigValue(pointConfig, "canId", "");
        String requestCanId = getConfigValue(pointConfig, "requestCanId", "");
        String requestData = getConfigValue(pointConfig, "requestData", "");

        try {
            // If a request CAN ID is configured, send a request frame first
            if (!requestCanId.isEmpty() && !requestData.isEmpty()) {
                String requestCmd = String.format("cansend %s %s#%s", interfaceName, requestCanId, requestData);
                executeCommand(requestCmd);
            }

            // Listen for CAN frames matching the expected CAN ID
            String value = readCanFrame(interfaceName, canId);
            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            throw e;
        } catch (Exception e) {
            throw new ReadPointException("CAN read failed, protocol={}, interface={}, canId={}, message={}",
                    driverCode, interfaceName, canId, e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        String interfaceName = getConfigValue(driverConfig, "interfaceName", "can0");
        String canId = getConfigValue(pointConfig, "canId", "");
        String data = getConfigValue(pointConfig, "data", "");

        try {
            String value = writePointValue.getValue(String.class);
            String frameData = data.replace("${value}", Objects.toString(value, ""));
            String command = String.format("cansend %s %s#%s", interfaceName, canId, frameData);
            executeCommand(command);
            return true;
        } catch (Exception e) {
            throw new WritePointException("CAN write failed, protocol={}, interface={}, canId={}, message={}",
                    driverCode, interfaceName, canId, e.getMessage(), e);
        }
    }

    /**
     * Execute a shell command and return the output.
     *
     * @param command the shell command to execute
     * @return the trimmed stdout output
     */
    private String executeCommand(String command) throws Exception {
        log.debug("Executing CAN command: {}", command);
        Process process = new ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        boolean finished = process.waitFor(5, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new ReadPointException("CAN command timed out, command={}", command);
        }
        return output.toString().trim();
    }

    /**
     * Read a CAN frame matching the given CAN ID from the specified interface.
     * Uses {@code candump} to capture a single frame.
     *
     * @param interfaceName the CAN interface name (e.g. can0)
     * @param canId         the expected CAN ID (hex)
     * @return the data payload of the captured frame
     */
    private String readCanFrame(String interfaceName, String canId) throws Exception {
        String command = String.format("timeout 3 candump -n 1 %s,%s", interfaceName, canId);
        String output = executeCommand(command);

        if (output.isEmpty()) {
            throw new ReadPointException("No CAN frame received, interface={}, canId={}", interfaceName, canId);
        }

        // candump output format: "can0  123   [1]  FF"
        // Extract the data portion (last field)
        String[] parts = output.trim().split("\\s+");
        if (parts.length >= 3) {
            return parts[parts.length - 1];
        }
        return output.trim();
    }

    private String getConfigValue(Map<String, AttributeBO> config, String code, String defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
            return defaultValue;
        }
        return attr.getValue(String.class);
    }
}
