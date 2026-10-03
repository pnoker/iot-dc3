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
import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * CAN bus driver service implementation.
 * <p>
 * Communicates with CAN bus devices on Linux via {@code can-utils} command-line
 * tools ({@code candump}, {@code cansend}). A {@link ProcessBuilder} is used to
 * execute shell commands for both read and write operations. Point attributes
 * {@code dataOffset}/{@code dataLength} select the payload byte range returned by
 * the read path (0-based slice of the frame payload, reported as a contiguous hex
 * string); the write path substitutes {@code ${value}} into the configured frame
 * template.
 * </p>
 * <p>
 * The per-call process startup cost of {@code can-utils} is an accepted trade-off:
 * a native SocketCAN JNI bridge was evaluated and deliberately not adopted, because
 * the latency win does not justify dragging cross-compilation into this driver's
 * build for its low-frequency industrial sampling profile.
 * </p>
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Slf4j
@Service
public class CanDriverCustomServiceImpl implements DriverCustomService {

    private static final java.util.regex.Pattern CAN_COMMAND_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9 _#,.=%/\\-]+$");
    private static final java.util.regex.Pattern INTERFACE_NAME_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9_]+$");

    private final DriverMetadata driverMetadata;
    private final DriverProperties driverProperties;

    private Map<Long, Boolean> deviceMap = new ConcurrentHashMap<>(16);

    /** can driver custom service impl. */
    public CanDriverCustomServiceImpl(DriverMetadata driverMetadata, DriverProperties driverProperties) {
        this.driverProperties = driverProperties;
        this.driverMetadata = driverMetadata;
    }

    private static void checkRequired(
            Map<String, AttributeBO> config, String code, List<ValidationReport.AttributeIssue> issues) {
        AttributeBO attr = config.get(code);
        if (attr == null || attr.getValue() == null) {
            issues.add(ValidationReport.AttributeIssue.builder()
                    .attributeCode(code)
                    .level(ValidationReport.IssueLevel.ERROR)
                    .message("Missing required attribute: " + code)
                    .build());
        }
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
        if (!INTERFACE_NAME_PATTERN.matcher(interfaceName).matches()) {
            // A health probe must answer with a state, never throw: an invalid
            // interface name is one more way for the device to be unreachable.
            log.debug("CAN interface name invalid, reporting offline, interface={}", interfaceName);
            return DeviceHealthState.offline();
        }
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
            log.info(
                    "Driver metadata event received, protocol={}, metadataType={}, operateType={}, deviceId={}",
                    driverProperties.getCode(),
                    metadataType,
                    operateType,
                    metadataEvent.getId());

            if (MetadataOperateTypeEnum.DELETE.equals(operateType)
                    || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                if (Objects.nonNull(metadataEvent.getId())) {
                    deviceMap.remove(metadataEvent.getId());
                }
                log.info(
                        "Driver connection destroyed, protocol={}, deviceId={}, operateType={}",
                        driverProperties.getCode(),
                        metadataEvent.getId(),
                        operateType);
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info(
                    "Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}",
                    driverProperties.getCode(),
                    metadataType,
                    operateType,
                    metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(
            Map<String, AttributeBO> driverConfig,
            Map<String, AttributeBO> pointConfig,
            DeviceBO device,
            PointBO point) {
        String interfaceName = getConfigValue(driverConfig, "interfaceName", "can0");
        if (!INTERFACE_NAME_PATTERN.matcher(interfaceName).matches()) {
            throw new ReadPointException("Invalid CAN interface name, interface={}", interfaceName);
        }
        String canId = getConfigValue(pointConfig, "canId", "");
        String requestCanId = getConfigValue(pointConfig, "requestCanId", "");
        String requestData = getConfigValue(pointConfig, "requestData", "");
        int dataOffset = parseByteIndex(pointConfig, "dataOffset", 0);
        int dataLength = parseByteIndex(pointConfig, "dataLength", 1);

        try {
            // If a request CAN ID is configured, send a request frame first
            if (!requestCanId.isEmpty() && !requestData.isEmpty()) {
                String requestCmd = String.format("cansend %s %s#%s", interfaceName, requestCanId, requestData);
                executeCommand(requestCmd);
            }

            // Listen for CAN frames matching the expected CAN ID
            String value = readCanFrame(interfaceName, canId, dataOffset, dataLength);
            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            throw e;
        } catch (Exception e) {
            throw new ReadPointException(
                    "CAN read failed, protocol={}, interface={}, canId={}, message={}",
                    driverProperties.getCode(),
                    interfaceName,
                    canId,
                    e.getMessage(),
                    e);
        }
    }

    @Override
    public Boolean write(
            Map<String, AttributeBO> driverConfig,
            Map<String, AttributeBO> pointConfig,
            DeviceBO device,
            PointBO point,
            WritePointValue writePointValue) {
        String interfaceName = getConfigValue(driverConfig, "interfaceName", "can0");
        if (!INTERFACE_NAME_PATTERN.matcher(interfaceName).matches()) {
            throw new ReadPointException("Invalid CAN interface name, interface={}", interfaceName);
        }
        String canId = getConfigValue(pointConfig, "canId", "");
        String data = getConfigValue(pointConfig, "data", "");

        try {
            String value = writePointValue.getValue(String.class);
            String frameData = data.replace("${value}", Objects.toString(value, ""));
            String command = String.format("cansend %s %s#%s", interfaceName, canId, frameData);
            executeCommand(command);
            return true;
        } catch (Exception e) {
            throw new WritePointException(
                    "CAN write failed, protocol={}, interface={}, canId={}, message={}",
                    driverProperties.getCode(),
                    interfaceName,
                    canId,
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Execute a shell command and return the output.
     *
     * @param command the shell command to execute
     * @return the trimmed stdout output
     */
    private String executeCommand(String command) throws Exception {
        // Reject shell metacharacters: command is built from driver config (interfaceName)
        // and request data (canId, frame data) and run via `sh -c`, so any ;|&`$()<> would
        // be command injection. CAN commands (cansend/candump/timeout) only use these chars.
        if (!CAN_COMMAND_PATTERN.matcher(command).matches()) {
            throw new ReadPointException("Unsafe CAN command rejected, command={}", command);
        }
        log.debug("CAN command execution started, commandLength={}", command.length());
        Process process = new ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
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
     * Uses {@code candump} to capture a single frame, then slices the payload by
     * the point's {@code dataOffset}/{@code dataLength} attributes.
     *
     * @param interfaceName the CAN interface name (e.g. can0)
     * @param canId         the expected CAN ID (hex)
     * @param dataOffset    0-based byte offset into the frame payload
     * @param dataLength    number of payload bytes to report
     * @return the sliced payload bytes as a contiguous hex string
     */
    private String readCanFrame(String interfaceName, String canId, int dataOffset, int dataLength) throws Exception {
        String command = String.format("timeout 3 candump -n 1 %s,%s", interfaceName, canId);
        String output = executeCommand(command);

        if (output.isEmpty()) {
            throw new ReadPointException("No CAN frame received, interface={}, canId={}", interfaceName, canId);
        }

        String payload = sliceCanPayload(output, dataOffset, dataLength);
        if (payload.isEmpty()) {
            throw new ReadPointException(
                    "CAN frame slice is empty, interface={}, canId={}, dataOffset={}, dataLength={}",
                    interfaceName,
                    canId,
                    dataOffset,
                    dataLength);
        }
        return payload;
    }

    /**
     * Extract a byte-range slice from candump output as a contiguous hex string.
     * candump lines look like {@code can0  123   [8]  DE AD BE EF 00 11 22 33};
     * the payload starts after the {@code [dlc]} marker, {@code dataOffset} is
     * 0-based from the first payload byte, and {@code dataLength} counts bytes.
     * Output without a DLC marker is treated as payload-only.
     */
    static String sliceCanPayload(String candumpOutput, int dataOffset, int dataLength) {
        String trimmed = candumpOutput.trim();
        int marker = trimmed.indexOf('[');
        String payloadPart = marker >= 0 ? trimmed.substring(trimmed.indexOf(']', marker) + 1) : trimmed;
        String[] bytes = payloadPart.trim().split("\\s+");
        int from = Math.max(0, dataOffset);
        int to = Math.min(bytes.length, from + Math.max(0, dataLength));
        StringBuilder hex = new StringBuilder();
        for (int i = from; i < to; i++) {
            hex.append(bytes[i]);
        }
        return hex.toString();
    }

    private int parseByteIndex(Map<String, AttributeBO> config, String code, int defaultValue) {
        String raw = getConfigValue(config, code, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new ReadPointException("CAN point attribute '{}' must be an integer, value={}", code, raw);
        }
    }

    private String getConfigValue(Map<String, AttributeBO> config, String code, String defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr)
                || Objects.isNull(attr.getValue())
                || attr.getValue().isEmpty()) {
            return defaultValue;
        }
        return attr.getValue(String.class);
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, "interfaceName", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "canId", issues);
        checkRequired(pointConfig, "dataOffset", issues);
        checkRequired(pointConfig, "dataLength", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }
}
