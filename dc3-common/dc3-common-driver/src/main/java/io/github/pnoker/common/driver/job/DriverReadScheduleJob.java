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

/**
 * Quartz job that iterates through enabled devices and points and triggers periodic
 * reads.
 *
 * <p>The scan walks every device and every point sequentially, so a single slow
 * point can stretch one execution past the next trigger. {@link DisallowConcurrentExecution}
 * stops Quartz from launching overlapping fires when that happens — the next
 * trigger waits for the in-flight scan to finish instead of stacking up worker
 * threads on the same metadata structures.
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

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        Set<Long> deviceIds = driverMetadata.getDeviceIds();
        if (CollectionUtils.isEmpty(deviceIds)) {
            return;
        }

        for (Long deviceId : deviceIds) {
            DeviceBO entityBO = deviceMetadata.getCache(deviceId);
            if (Objects.nonNull(entityBO) && EnableFlagEnum.ENABLE.equals(entityBO.getEnableFlag())
                    && Objects.nonNull(entityBO.getProfileId())
                    && CollectionUtils.isNotEmpty(entityBO.getPointIds())
                    && MapUtils.isNotEmpty(entityBO.getDriverAttributeConfigIdMap())
                    && MapUtils.isNotEmpty(entityBO.getPointAttributeConfigIdMap())) {
                Set<Long> pointIds = entityBO.getPointIds();
                for (Long pointId : pointIds) {
                    try {
                        driverReadService.read(deviceId, pointId);
                    } catch (Exception e) {
                        log.error("Driver point read schedule failed, deviceId={}, pointId={}", deviceId, pointId, e);
                    }
                }
            }
        }
    }

}
