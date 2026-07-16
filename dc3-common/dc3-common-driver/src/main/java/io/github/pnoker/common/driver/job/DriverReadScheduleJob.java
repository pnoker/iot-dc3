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

import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverReadService;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Quartz job that iterates through enabled devices and points and triggers periodic
 * reads.
 *
 * <p>The scan submits eligible devices to the shared bounded driver thread
 * pool and reads each device's points sequentially inside that task. This
 * preserves device-level protocol serialization while preventing one slow
 * device from blocking all other devices. {@link DisallowConcurrentExecution}
 * still prevents overlapping Quartz fires for the same job.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class DriverReadScheduleJob extends QuartzJobBean {

    private final DriverMetadata driverMetadata;

    private final DeviceMetadata deviceMetadata;

    private final DriverReadService driverReadService;

    private final ThreadPoolExecutor threadPoolExecutor;

    /**
     * Scheduled read job: concurrently reads every readable device's points via the
     * driver read service and waits for all read tasks to finish.
     *
     * @param jobExecutionContext Quartz job execution context
     */
    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        Set<Long> deviceIds = driverMetadata.getDeviceIds();
        if (CollectionUtils.isEmpty(deviceIds)) {
            return;
        }

        CompletableFuture<?>[] tasks = deviceIds.stream()
                .map(deviceMetadata::getCache)
                .filter(this::isReadableDevice)
                .map(device -> CompletableFuture.runAsync(() -> readDevice(device), threadPoolExecutor))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(tasks).join();
    }

    /**
     * Return whether a device is ready for scheduled reads: enabled, with a profile,
     * point ids, and non-empty driver/point attribute config maps.
     *
     * @param entityBO the device to check
     * @return true if the device can be read
     */
    private boolean isReadableDevice(DeviceBO entityBO) {
        return Objects.nonNull(entityBO) && EnableFlagEnum.ENABLE.equals(entityBO.getEnableFlag())
                && Objects.nonNull(entityBO.getProfileId())
                && CollectionUtils.isNotEmpty(entityBO.getPointIds())
                && MapUtils.isNotEmpty(entityBO.getDriverAttributeConfigIdMap())
                && MapUtils.isNotEmpty(entityBO.getPointAttributeConfigIdMap());
    }

    private void readDevice(DeviceBO device) {
        device.getPointIds().forEach(pointId -> readPoint(device.getId(), pointId));
    }

    /**
     * Read a single point on a device, logging (not rethrowing) on failure so one bad
     * point does not abort the whole schedule round.
     *
     * @param deviceId the device to read from
     * @param pointId  the point to read
     */
    private void readPoint(Long deviceId, Long pointId) {
        try {
            driverReadService.read(deviceId, pointId);
        } catch (Exception e) {
            log.error("Driver point read schedule failed, deviceId={}, pointId={}", deviceId, pointId, e);
        }
    }

}
