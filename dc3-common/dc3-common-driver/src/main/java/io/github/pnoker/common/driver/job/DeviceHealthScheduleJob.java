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

package io.github.pnoker.common.driver.job;

import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bean.DeviceHealthState;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Quartz job that lets protocol drivers renew per-device state leases.
 *
 * <p>The cadence is configured by each driver through
 * {@code dc3.driver.health.device.cron}. Driver implementations provide the
 * protocol-specific health decision and may return a per-device lease timeout.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class DeviceHealthScheduleJob extends QuartzJobBean {

    private final DriverProperties driverProperties;

    private final DriverMetadata driverMetadata;

    private final DeviceMetadata deviceMetadata;

    private final DriverCustomService driverCustomService;

    private final DriverSenderService driverSenderService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        Set<Long> deviceIds = driverMetadata.getDeviceIds();
        if (CollectionUtils.isEmpty(deviceIds)) {
            return;
        }

        for (Long deviceId : deviceIds) {
            reportDeviceHealth(deviceId);
        }
    }

    private void reportDeviceHealth(Long deviceId) {
        DeviceBO device = deviceMetadata.getCache(deviceId);
        if (Objects.isNull(device) || !EnableFlagEnum.ENABLE.equals(device.getEnableFlag())) {
            return;
        }

        Map<String, AttributeBO> driverConfig = deviceMetadata.getDriverConfig(deviceId);
        if (MapUtils.isNotEmpty(driverMetadata.getDriverAttributeIdMap()) && MapUtils.isEmpty(driverConfig)) {
            log.warn("Skip device health report: driver config is incomplete, deviceId={}", deviceId);
            return;
        }

        DeviceHealthState healthState = DeviceHealthState.offline();
        try {
            healthState = driverCustomService.health(driverConfig, device);
        } catch (Exception e) {
            log.warn("Device health check failed, deviceId={}", deviceId, e);
        }
        reportDeviceState(deviceId, healthState);
    }

    private void reportDeviceState(Long deviceId, DeviceHealthState healthState) {
        DeviceStatusEnum status = DeviceStatusEnum.OFFLINE;
        int timeout = driverProperties.getHealth().getDevice().getTimeout();
        TimeUnit timeoutUnit = driverProperties.getHealth().getDevice().getTimeoutUnit();
        if (Objects.nonNull(healthState)) {
            if (Objects.nonNull(healthState.getStatus())) {
                status = healthState.getStatus();
            }
            if (Objects.nonNull(healthState.getTimeout()) && healthState.getTimeout() > 0) {
                timeout = healthState.getTimeout();
            }
            if (Objects.nonNull(healthState.getTimeoutUnit())) {
                timeoutUnit = healthState.getTimeoutUnit();
            }
        }
        driverSenderService.deviceStatusSender(deviceId, status, timeout, timeoutUnit);
    }

}
