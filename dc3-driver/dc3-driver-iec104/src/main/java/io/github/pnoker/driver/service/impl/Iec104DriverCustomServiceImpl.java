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
import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Custom driver service implementation for the IEC 60870-5-104 Driver.
 * <p>
 * Validates device/point configuration and builds outbound control command payloads for
 * substation automation and telecontrol equipment.
 * </p>
 *
 * <p>
 * <b>WORK IN PROGRESS:</b> protocol-level I/O over the j60870 client (IOA read and control
 * command write) is not implemented yet. {@link #read} and {@link #write} therefore fail fast by
 * throwing instead of echoing a locally cached value or reporting a fabricated success, so the SDK
 * records the failure and applies connection backoff.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Slf4j
@Service
public class Iec104DriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
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
        // Protocol read is not implemented yet (see class WORK IN PROGRESS note). Fail fast so the
        // SDK records a read failure and applies backoff instead of echoing a fabricated/cached value.
        throw new ReadPointException("IEC 104 read not implemented: protocol I/O is pending, protocol={}, deviceId={}",
                driverCode, device.getId());
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                         PointBO point, WritePointValue writePointValue) {
        // Protocol write is not implemented yet (see class WORK IN PROGRESS note). Fail fast instead
        // of caching the value locally and reporting a fabricated write success.
        throw new WritePointException("IEC 104 write not implemented: protocol I/O is pending, protocol={}, deviceId={}",
                driverCode, device.getId());
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
     * Get string config value with default.
     */
    private String getConfigValue(Map<String, AttributeBO> config, String code, String defaultValue) {
        if (Objects.isNull(config) || Objects.isNull(config.get(code))) {
            return defaultValue;
        }
        String value = config.get(code).getValue();
        return StringUtils.defaultIfBlank(value, defaultValue);
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, "host", issues);
        checkRequired(driverConfig, "port", issues);
        checkRequired(driverConfig, "asduAddress", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "ioa", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

}
